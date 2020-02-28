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

package com.google.cloud.spanner;

import static com.google.cloud.spanner.MockSpannerTestUtil.*;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.AsyncRunner.AsyncWork;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AsyncRunnerTest {

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private static ExecutorService executor;

  private Spanner spanner;
  private Spanner spannerWithEmptySessionPool;

  @BeforeClass
  public static void setup() throws Exception {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D);
    mockSpanner.putStatementResult(
        StatementResult.query(READ_ONE_EMPTY_KEY_VALUE_STATEMENT, EMPTY_KEY_VALUE_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.query(READ_ONE_KEY_VALUE_STATEMENT, READ_ONE_KEY_VALUE_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.query(
            READ_MULTIPLE_KEY_VALUE_STATEMENT, READ_MULTIPLE_KEY_VALUE_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));
    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
    executor = Executors.newSingleThreadExecutor();
  }

  @AfterClass
  public static void teardown() throws Exception {
    executor.shutdown();
    server.shutdown();
    server.awaitTermination();
  }

  @Before
  public void before() {
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setFailOnSessionLeak().build())
            .build()
            .getService();
    spannerWithEmptySessionPool =
        spanner
            .getOptions()
            .toBuilder()
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setFailOnSessionLeak().setMinSessions(0).build())
            .build()
            .getService();
  }

  private DatabaseClient client() {
    return spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
  }

  private DatabaseClient clientWithEmptySessionPool() {
    return spannerWithEmptySessionPool.getDatabaseClient(
        DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
  }

  @After
  public void after() {
    spanner.close();
    spannerWithEmptySessionPool.close();
    mockSpanner.removeAllExecutionTimes();
    mockSpanner.reset();
  }

  @Test
  public void asyncRunnerUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<Long> updateCount =
        runner.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                return txn.executeUpdateAsync(UPDATE_STATEMENT);
              }
            },
            executor);
    assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
  }

  @Test
  public void asyncRunnerIsNonBlocking() throws Exception {
    mockSpanner.freeze();
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> res =
        runner.runAsync(
            new AsyncWork<Void>() {
              @Override
              public ApiFuture<Void> doWorkAsync(TransactionContext txn) {
                txn.executeUpdateAsync(UPDATE_STATEMENT);
                return ApiFutures.immediateFuture(null);
              }
            },
            executor);
    ApiFuture<Timestamp> ts = runner.getCommitTimestamp();
    mockSpanner.unfreeze();
    assertThat(res.get()).isNull();
    assertThat(ts.get()).isNotNull();
  }

  @Test
  public void asyncRunnerInvalidUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<Long> updateCount =
        runner.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                return txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
              }
            },
            executor);
    try {
      updateCount.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(se.getMessage()).contains("invalid statement");
    }
  }

  @Test
  public void asyncRunnerFireAndForgetInvalidUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<Long> res =
        runner.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
                return txn.executeUpdateAsync(UPDATE_STATEMENT);
              }
            },
            executor);
    assertThat(res.get()).isEqualTo(UPDATE_COUNT);
  }

  @Test
  public void asyncRunnerUpdateAborted() throws Exception {
    try {
      // Temporarily set the result of the update to 2 rows.
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT + 1L));
      final AtomicInteger attempt = new AtomicInteger();
      AsyncRunner runner = client().runAsync();
      ApiFuture<Long> updateCount =
          runner.runAsync(
              new AsyncWork<Long>() {
                @Override
                public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                  if (attempt.incrementAndGet() == 1) {
                    mockSpanner.abortTransaction(txn);
                  } else {
                    // Set the result of the update statement back to 1 row.
                    mockSpanner.putStatementResult(
                        StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                  }
                  return txn.executeUpdateAsync(UPDATE_STATEMENT);
                }
              },
              executor);
      assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
      assertThat(attempt.get()).isEqualTo(2);
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    }
  }

  @Test
  public void asyncRunnerCommitAborted() throws Exception {
    try {
      // Temporarily set the result of the update to 2 rows.
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT + 1L));
      final AtomicInteger attempt = new AtomicInteger();
      AsyncRunner runner = client().runAsync();
      ApiFuture<Long> updateCount =
          runner.runAsync(
              new AsyncWork<Long>() {
                @Override
                public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                  ApiFuture<Long> updateCount = txn.executeUpdateAsync(UPDATE_STATEMENT);
                  if (attempt.incrementAndGet() == 1) {
                    mockSpanner.abortTransaction(txn);
                  } else {
                    // Set the result of the update statement back to 1 row.
                    mockSpanner.putStatementResult(
                        StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                  }
                  return updateCount;
                }
              },
              executor);
      assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
      assertThat(attempt.get()).isEqualTo(2);
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    }
  }

  @Test
  public void asyncRunnerUpdateAbortedWithoutGettingResult() throws Exception {
    final AtomicInteger attempt = new AtomicInteger();
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> result =
        runner.runAsync(
            new AsyncWork<Void>() {
              @Override
              public ApiFuture<Void> doWorkAsync(TransactionContext txn) {
                if (attempt.incrementAndGet() == 1) {
                  mockSpanner.abortTransaction(txn);
                }
                // This update statement will be aborted, but the error will not propagated to the
                // transaction runner and cause the transaction to retry. Instead, the commit call
                // will do that.
                txn.executeUpdateAsync(UPDATE_STATEMENT);
                // Resolving this future will not resolve the result of the entire transaction. The
                // transaction result will be resolved when the commit has actually finished
                // successfully.
                return ApiFutures.immediateFuture(null);
              }
            },
            executor);
    assertThat(result.get()).isNull();
    assertThat(attempt.get()).isEqualTo(2);
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            BeginTransactionRequest.class,
            ExecuteSqlRequest.class,
            CommitRequest.class,
            BeginTransactionRequest.class,
            ExecuteSqlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void asyncRunnerCommitFails() throws Exception {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.RESOURCE_EXHAUSTED
                .withDescription("mutation limit exceeded")
                .asRuntimeException()));
    AsyncRunner runner = client().runAsync();
    ApiFuture<Long> updateCount =
        runner.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                // This statement will succeed, but the commit will fail. The error from the commit
                // will bubble up to the future that is returned by the transaction, and the update
                // count returned here will never reach the user application.
                return txn.executeUpdateAsync(UPDATE_STATEMENT);
              }
            },
            executor);
    try {
      updateCount.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
      assertThat(se.getMessage()).contains("mutation limit exceeded");
    }
  }

  @Test
  public void asyncRunnerWaitsUntilAsyncUpdateHasFinished() throws Exception {
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> res =
        runner.runAsync(
            new AsyncWork<Void>() {
              @Override
              public ApiFuture<Void> doWorkAsync(TransactionContext txn) {
                txn.executeUpdateAsync(UPDATE_STATEMENT);
                return ApiFutures.immediateFuture(null);
              }
            },
            executor);
    res.get();
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            BeginTransactionRequest.class,
            ExecuteSqlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void closeTransactionBeforeEndOfAsyncQuery() throws Exception {
    final BlockingQueue<String> results = new SynchronousQueue<>();
    final SettableApiFuture<Boolean> finished = SettableApiFuture.create();
    DatabaseClientImpl clientImpl = (DatabaseClientImpl) client();

    // There should currently not be any sessions checked out of the pool.
    assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(0);

    AsyncRunner runner = clientImpl.runAsync();
    final CountDownLatch dataReceived = new CountDownLatch(1);
    ApiFuture<Void> res =
        runner.runAsync(
            new AsyncWork<Void>() {
              @Override
              public ApiFuture<Void> doWorkAsync(TransactionContext txn) {
                try (AsyncResultSet rs =
                    txn.readAsync(
                        READ_TABLE_NAME, KeySet.all(), READ_COLUMN_NAMES, Options.bufferRows(1))) {
                  rs.setCallback(
                      Executors.newSingleThreadExecutor(),
                      new ReadyCallback() {
                        @Override
                        public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                          try {
                            while (true) {
                              switch (resultSet.tryNext()) {
                                case DONE:
                                  finished.set(true);
                                  return CallbackResponse.DONE;
                                case NOT_READY:
                                  return CallbackResponse.CONTINUE;
                                case OK:
                                  dataReceived.countDown();
                                  results.put(resultSet.getString(0));
                              }
                            }
                          } catch (Throwable t) {
                            finished.setException(t);
                            dataReceived.countDown();
                            return CallbackResponse.DONE;
                          }
                        }
                      });
                }
                return ApiFutures.immediateFuture(null);
              }
            },
            executor);
    // Wait until at least one row has been fetched. At that moment there should be one session
    // checked out.
    dataReceived.await();
    assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(1);
    assertThat(res.isDone()).isFalse();
    // Get the data from the transaction.
    List<String> resultList = new ArrayList<>();
    do {
      results.drainTo(resultList);
    } while (!finished.isDone() || results.size() > 0);
    assertThat(finished.get()).isTrue();
    assertThat(resultList).containsExactly("k1", "k2", "k3");
    assertThat(res.get()).isNull();
    assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(0);
  }

  @Test
  public void asyncRunnerReadRow() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<String> val =
        runner.runAsync(
            new AsyncWork<String>() {
              @Override
              public ApiFuture<String> doWorkAsync(TransactionContext txn) {
                return ApiFutures.transform(
                    txn.readRowAsync(READ_TABLE_NAME, Key.of(1L), READ_COLUMN_NAMES),
                    new ApiFunction<Struct, String>() {
                      @Override
                      public String apply(Struct input) {
                        return input.getString("Value");
                      }
                    },
                    MoreExecutors.directExecutor());
              }
            },
            executor);
    assertThat(val.get()).isEqualTo("v1");
  }

  @Test
  public void asyncRunnerRead() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<ImmutableList<String>> val =
        runner.runAsync(
            new AsyncWork<ImmutableList<String>>() {
              @Override
              public ApiFuture<ImmutableList<String>> doWorkAsync(TransactionContext txn) {
                return txn.readAsync(READ_TABLE_NAME, KeySet.all(), READ_COLUMN_NAMES)
                    .toListAsync(
                        new Function<StructReader, String>() {
                          @Override
                          public String apply(StructReader input) {
                            return input.getString("Value");
                          }
                        },
                        MoreExecutors.directExecutor());
              }
            },
            executor);
    assertThat(val.get()).containsExactly("v1", "v2", "v3");
  }
}
