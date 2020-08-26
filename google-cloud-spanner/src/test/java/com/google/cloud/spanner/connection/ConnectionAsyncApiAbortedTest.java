/*
 * Copyright 2020 Google LLC
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

import static com.google.cloud.spanner.SpannerApiFutures.get;
import static com.google.common.truth.Truth.assertThat;

import com.google.api.core.ApiFuture;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncResultSet;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.ITConnection;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.Status;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Tests retry handling of read/write transactions using the Async Connection API. */
public class ConnectionAsyncApiAbortedTest extends AbstractMockServerTest {
  private static final class QueryResult {
    final ApiFuture<Void> finished;
    final AtomicInteger rowCount;

    QueryResult(ApiFuture<Void> finished, AtomicInteger rowCount) {
      this.finished = finished;
      this.rowCount = rowCount;
    }
  }

  private static final class RetryCounter implements TransactionRetryListener {
    final CountDownLatch latch;
    int retryCount = 0;

    RetryCounter() {
      this(0);
    }

    RetryCounter(int countDown) {
      latch = new CountDownLatch(countDown);
    }

    @Override
    public void retryStarting(Timestamp transactionStarted, long transactionId, int retryAttempt) {
      retryCount++;
      latch.countDown();
    }

    @Override
    public void retryFinished(
        Timestamp transactionStarted, long transactionId, int retryAttempt, RetryResult result) {}
  }

  private static final ExecutorService singleThreadedExecutor = Executors.newSingleThreadExecutor();
  public static final int RANDOM_RESULT_SET_ROW_COUNT_2 = 50;
  public static final Statement SELECT_RANDOM_STATEMENT_2 = Statement.of("SELECT * FROM RANDOM2");
  public static final com.google.spanner.v1.ResultSet RANDOM_RESULT_SET_2 =
      new RandomResultSetGenerator(RANDOM_RESULT_SET_ROW_COUNT_2).generate();

  @BeforeClass
  public static void setupAdditionalResults() {
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT_RANDOM_STATEMENT_2, RANDOM_RESULT_SET_2));
  }

  @AfterClass
  public static void stopExecutor() {
    singleThreadedExecutor.shutdown();
  }

  @After
  public void reset() {
    mockSpanner.removeAllExecutionTimes();
  }

  ITConnection createConnection(TransactionRetryListener listener) {
    ITConnection connection =
        super.createConnection(
            ImmutableList.<StatementExecutionInterceptor>of(), ImmutableList.of(listener));
    connection.setAutocommit(false);
    return connection;
  }

  @Test
  public void testSingleQueryAborted() {
    RetryCounter counter = new RetryCounter();
    try (Connection connection = createConnection(counter)) {
      assertThat(counter.retryCount).isEqualTo(0);
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofException(Status.ABORTED.asRuntimeException()));
      QueryResult res = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT);

      assertThat(get(res.finished)).isNull();
      assertThat(res.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT);
      assertThat(counter.retryCount).isEqualTo(1);
    }
  }

  @Test
  public void testTwoQueriesSecondAborted() {
    RetryCounter counter = new RetryCounter();
    try (Connection connection = createConnection(counter)) {
      assertThat(counter.retryCount).isEqualTo(0);
      QueryResult res1 = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT);
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofException(Status.ABORTED.asRuntimeException()));
      QueryResult res2 = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT_2);

      assertThat(get(res1.finished)).isNull();
      assertThat(res1.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT);
      assertThat(get(res2.finished)).isNull();
      assertThat(res2.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT_2);
      assertThat(counter.retryCount).isEqualTo(1);
    }
  }

  @Test
  public void testTwoQueriesBothAborted() throws InterruptedException {
    RetryCounter counter = new RetryCounter(1);
    try (Connection connection = createConnection(counter)) {
      assertThat(counter.retryCount).isEqualTo(0);
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofException(Status.ABORTED.asRuntimeException()));
      QueryResult res1 = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT);
      // Wait until the first query aborted.
      assertThat(counter.latch.await(10L, TimeUnit.SECONDS)).isTrue();
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofException(Status.ABORTED.asRuntimeException()));
      QueryResult res2 = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT_2);

      assertThat(get(res1.finished)).isNull();
      assertThat(res1.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT);
      assertThat(get(res2.finished)).isNull();
      assertThat(res2.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT_2);
      assertThat(counter.retryCount).isEqualTo(2);
    }
  }

  @Test
  public void testSingleQueryAbortedMidway() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofStreamException(
            Status.ABORTED.asRuntimeException(), RANDOM_RESULT_SET_ROW_COUNT / 2));
    RetryCounter counter = new RetryCounter();
    try (Connection connection = createConnection(counter)) {
      assertThat(counter.retryCount).isEqualTo(0);
      QueryResult res = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT);

      assertThat(get(res.finished)).isNull();
      assertThat(res.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT);
      assertThat(counter.retryCount).isEqualTo(1);
    }
  }

  @Test
  public void testTwoQueriesSecondAbortedMidway() {
    RetryCounter counter = new RetryCounter();
    try (Connection connection = createConnection(counter)) {
      assertThat(counter.retryCount).isEqualTo(0);
      QueryResult res1 = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT);
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofStreamException(
              Status.ABORTED.asRuntimeException(), RANDOM_RESULT_SET_ROW_COUNT_2 / 2));
      QueryResult res2 = executeQueryAsync(connection, SELECT_RANDOM_STATEMENT_2);

      assertThat(get(res1.finished)).isNull();
      assertThat(res1.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT);
      assertThat(get(res2.finished)).isNull();
      assertThat(res2.rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT_2);
      assertThat(counter.retryCount).isEqualTo(1);
    }
  }

  @Test
  public void testUpdateAndQueryAbortedMidway() throws InterruptedException {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofStreamException(
            Status.ABORTED.asRuntimeException(), RANDOM_RESULT_SET_ROW_COUNT / 2));
    final RetryCounter counter = new RetryCounter();
    try (Connection connection = createConnection(counter)) {
      assertThat(counter.retryCount).isEqualTo(0);
      final SettableApiFuture<Long> rowCount = SettableApiFuture.create();
      final CountDownLatch updateLatch = new CountDownLatch(1);
      final CountDownLatch queryLatch = new CountDownLatch(1);
      ApiFuture<Void> finished;
      try (AsyncResultSet rs =
          connection.executeQueryAsync(
              SELECT_RANDOM_STATEMENT, Options.bufferRows(RANDOM_RESULT_SET_ROW_COUNT / 2 - 1))) {
        finished =
            rs.setCallback(
                singleThreadedExecutor,
                new ReadyCallback() {
                  long count;

                  @Override
                  public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                    // Indicate that the query has been executed.
                    queryLatch.countDown();
                    try {
                      // Wait until the update is on its way.
                      updateLatch.await(10L, TimeUnit.SECONDS);
                      while (true) {
                        switch (resultSet.tryNext()) {
                          case OK:
                            count++;
                            break;
                          case DONE:
                            rowCount.set(count);
                            return CallbackResponse.DONE;
                          case NOT_READY:
                            return CallbackResponse.CONTINUE;
                        }
                      }
                    } catch (InterruptedException e) {
                      throw SpannerExceptionFactory.propagateInterrupt(e);
                    }
                  }
                });
      }
      // Wait until the query has actually executed.
      queryLatch.await(10L, TimeUnit.SECONDS);
      ApiFuture<Long> updateCount = connection.executeUpdateAsync(INSERT_STATEMENT);
      updateCount.addListener(
          new Runnable() {
            @Override
            public void run() {
              updateLatch.countDown();
            }
          },
          MoreExecutors.directExecutor());

      // We should not commit before the AsyncResultSet has finished.
      assertThat(get(finished)).isNull();
      ApiFuture<Void> commit = connection.commitAsync();

      assertThat(get(rowCount)).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT);
      assertThat(get(updateCount)).isEqualTo(UPDATE_COUNT);
      assertThat(get(commit)).isNull();
      assertThat(counter.retryCount).isEqualTo(1);

      // Verify the order of the statements on the server.
      List<? extends AbstractMessage> requests =
          Lists.newArrayList(
              Collections2.filter(
                  mockSpanner.getRequests(),
                  new Predicate<AbstractMessage>() {
                    @Override
                    public boolean apply(AbstractMessage input) {
                      return input instanceof ExecuteSqlRequest;
                    }
                  }));
      assertThat(requests).hasSize(4);
      assertThat(((ExecuteSqlRequest) requests.get(0)).getSeqno()).isEqualTo(1L);
      assertThat(((ExecuteSqlRequest) requests.get(0)).getSql())
          .isEqualTo(SELECT_RANDOM_STATEMENT.getSql());
      assertThat(((ExecuteSqlRequest) requests.get(1)).getSeqno()).isEqualTo(2L);
      assertThat(((ExecuteSqlRequest) requests.get(1)).getSql())
          .isEqualTo(INSERT_STATEMENT.getSql());
      assertThat(((ExecuteSqlRequest) requests.get(2)).getSeqno()).isEqualTo(1L);
      assertThat(((ExecuteSqlRequest) requests.get(2)).getSql())
          .isEqualTo(SELECT_RANDOM_STATEMENT.getSql());
      assertThat(((ExecuteSqlRequest) requests.get(3)).getSeqno()).isEqualTo(2L);
      assertThat(((ExecuteSqlRequest) requests.get(3)).getSql())
          .isEqualTo(INSERT_STATEMENT.getSql());
    }
  }

  private QueryResult executeQueryAsync(Connection connection, Statement statement) {
    ApiFuture<Void> res;
    final AtomicInteger rowCount = new AtomicInteger();
    try (AsyncResultSet rs = connection.executeQueryAsync(statement)) {
      res =
          rs.setCallback(
              singleThreadedExecutor,
              new ReadyCallback() {
                @Override
                public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                  while (true) {
                    switch (resultSet.tryNext()) {
                      case OK:
                        rowCount.incrementAndGet();
                        break;
                      case DONE:
                        return CallbackResponse.DONE;
                      case NOT_READY:
                        return CallbackResponse.CONTINUE;
                    }
                  }
                }
              });
      return new QueryResult(res, rowCount);
    }
  }
}
