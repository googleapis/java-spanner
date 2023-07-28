/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.connection;

import static com.google.common.base.Preconditions.checkState;

import com.google.cloud.spanner.ForwardingStructReader;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Struct;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link MergedResultSet} is a {@link ResultSet} implementation that combines the results from
 * multiple queries. Each query uses its own {@link RowProducer} that feeds rows into the {@link
 * MergedResultSet}. The order of the records in the {@link MergedResultSet} is not guaranteed.
 */
class MergedResultSet extends ForwardingStructReader implements PartitionedQueryResultSet {
  static class PartitionExecutor implements Runnable {
    private final Connection connection;
    private final String partitionId;
    private final LinkedBlockingDeque<PartitionExecutorResult> queue;
    private final AtomicBoolean shouldStop = new AtomicBoolean();

    PartitionExecutor(
        Connection connection,
        String partitionId,
        LinkedBlockingDeque<PartitionExecutorResult> queue) {
      this.connection = Preconditions.checkNotNull(connection);
      this.partitionId = Preconditions.checkNotNull(partitionId);
      this.queue = queue;
    }

    @Override
    public void run() {
      try (ResultSet resultSet = connection.runPartition(partitionId)) {
        boolean first = true;
        while (resultSet.next()) {
          if (first) {
            queue.put(
                PartitionExecutorResult.dataAndMetadata(
                    resultSet.getCurrentRowAsStruct(), resultSet.getMetadata()));
            first = false;
          } else {
            queue.put(PartitionExecutorResult.data(resultSet.getCurrentRowAsStruct()));
          }
          if (shouldStop.get()) {
            break;
          }
        }
      } catch (Throwable exception) {
        putWithoutInterruptPropagation(PartitionExecutorResult.exception(exception));
      } finally {
        // Emit a special 'finished' result to ensure that the row producer is not blocked on a
        // queue that never receives any more results. This ensures that we can safely block on
        // queue.take(), as we know that we will always receive at least one result from each
        // worker.
        putWithoutInterruptPropagation(PartitionExecutorResult.finished());
      }
    }

    private void putWithoutInterruptPropagation(PartitionExecutorResult result) {
      try {
        queue.put(result);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }

  static class PartitionExecutorResult {
    private final Struct data;
    private final Throwable exception;
    private final ResultSetMetadata metadata;

    static PartitionExecutorResult data(Struct data) {
      return new PartitionExecutorResult(data, null, null);
    }

    static PartitionExecutorResult dataAndMetadata(Struct data, ResultSetMetadata metadata) {
      return new PartitionExecutorResult(data, metadata, null);
    }

    static PartitionExecutorResult exception(Throwable exception) {
      return new PartitionExecutorResult(null, null, exception);
    }

    static PartitionExecutorResult finished() {
      return new PartitionExecutorResult(null, null, null);
    }

    private PartitionExecutorResult(Struct data, ResultSetMetadata metadata, Throwable exception) {
      this.data = data;
      this.metadata = metadata;
      this.exception = exception;
    }

    boolean isFinished() {
      return this.data == null && this.metadata == null && this.exception == null;
    }
  }

  static class RowProducer implements Supplier<Struct> {
    /** The maximum number of rows that we will cache per thread that is fetching rows. */
    private static final int QUEUE_SIZE_PER_WORKER = 32;

    private final ExecutorService executor;
    private final int parallelism;
    private final List<PartitionExecutor> partitionExecutors;
    private final AtomicInteger finishedCounter;
    private final LinkedBlockingDeque<PartitionExecutorResult> queue;
    private ResultSetMetadata metadata;
    private Struct currentRow;

    RowProducer(Connection connection, List<String> partitions, int maxParallelism) {
      Preconditions.checkArgument(maxParallelism >= 0, "maxParallelism must be >= 0");
      if (maxParallelism == 0) {
        // Dynamically determine parallelism.
        this.parallelism = Math.min(partitions.size(), Runtime.getRuntime().availableProcessors());
      } else {
        this.parallelism = Math.min(partitions.size(), maxParallelism);
      }
      this.executor =
          Executors.newFixedThreadPool(
              this.parallelism,
              runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("partitioned-query-row-producer");
                thread.setDaemon(true);
                return thread;
              });
      this.queue = new LinkedBlockingDeque<>(QUEUE_SIZE_PER_WORKER * this.parallelism);
      this.partitionExecutors = new ArrayList<>(partitions.size());
      this.finishedCounter = new AtomicInteger(partitions.size());
      for (String partition : partitions) {
        PartitionExecutor partitionExecutor =
            new PartitionExecutor(connection, partition, this.queue);
        this.partitionExecutors.add(partitionExecutor);
        this.executor.submit(partitionExecutor);
      }
      // Pre-emptively shutdown the executor. This does not terminate any running tasks, but it
      // stops the executor from accepting any new tasks and guarantees that the executor will
      // always be shutdown, regardless whether the user calls ResultSet#close().
      this.executor.shutdown();
    }

    void close() {
      this.partitionExecutors.forEach(partitionExecutor -> partitionExecutor.shouldStop.set(true));
      // shutdownNow will interrupt any running tasks and then shut down directly.
      // This will also cancel any queries that might be running.
      this.executor.shutdownNow();
    }

    boolean nextRow() throws Throwable {
      while (true) {
        PartitionExecutorResult next;
        if ((next = queue.peek()) != null && !next.isFinished()) {
          // There's a valid result available. Return this quickly.
          setNextRow(queue.remove());
          return true;
        }
        // Block until the next row is available.
        next = queue.take();
        if (next.isFinished()) {
          finishedCounter.decrementAndGet();
          if (finishedCounter.get() == 0) {
            return false;
          }
        } else {
          setNextRow(next);
          return true;
        }
      }
    }

    void setNextRow(PartitionExecutorResult next) throws Throwable {
      if (next.exception != null) {
        throw next.exception;
      }
      currentRow = next.data;
      if (this.metadata == null && next.metadata != null) {
        this.metadata = next.metadata;
      }
    }

    @Override
    public Struct get() {
      checkState(currentRow != null, "next() call required");
      return currentRow;
    }

    public ResultSetMetadata getMetadata() {
      checkState(metadata != null, "next() call required");
      return metadata;
    }
  }

  private final RowProducer rowProducer;

  private boolean closed;

  MergedResultSet(Connection connection, List<String> partitions, int maxParallelism) {
    this(new RowProducer(connection, partitions, maxParallelism));
  }

  MergedResultSet(RowProducer rowProducer) {
    super(rowProducer);
    this.rowProducer = rowProducer;
  }

  @Override
  protected void checkValidState() {
    Preconditions.checkState(!closed, "This result set has been closed");
  }

  @Override
  public boolean next() throws SpannerException {
    checkValidState();
    try {
      return rowProducer.nextRow();
    } catch (InterruptedException interruptedException) {
      throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
    } catch (Throwable throwable) {
      throw SpannerExceptionFactory.asSpannerException(throwable);
    }
  }

  @Override
  public Struct getCurrentRowAsStruct() {
    checkValidState();
    return rowProducer.get();
  }

  @Override
  public void close() {
    this.closed = true;
    rowProducer.close();
  }

  @Override
  public ResultSetStats getStats() {
    throw new UnsupportedOperationException(
        "ResultSetStats are available only for results returned from analyzeQuery() calls");
  }

  @Override
  public ResultSetMetadata getMetadata() {
    checkValidState();
    return rowProducer.getMetadata();
  }

  @Override
  public int getNumPartitions() {
    return rowProducer.partitionExecutors.size();
  }

  @Override
  public int getParallelism() {
    return rowProducer.parallelism;
  }
}
