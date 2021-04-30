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

import static com.google.cloud.spanner.MockSpannerTestUtil.INVALID_UPDATE_STATEMENT;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_COLUMN_NAMES;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_TABLE_NAME;
import static com.google.cloud.spanner.MockSpannerTestUtil.UPDATE_ABORTED_STATEMENT;
import static com.google.cloud.spanner.MockSpannerTestUtil.UPDATE_COUNT;
import static com.google.cloud.spanner.MockSpannerTestUtil.UPDATE_STATEMENT;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionFunction;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionStep;
import com.google.cloud.spanner.AsyncTransactionManager.CommitTimestampFuture;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.SessionPool.SessionPoolTransactionContext;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.TransactionSelector;
import io.grpc.Status;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncTransactionManagerTest extends AbstractAsyncTransactionTest {

  @Parameter public Executor executor;

  @Parameters(name = "executor = {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {MoreExecutors.directExecutor()},
          {Executors.newSingleThreadExecutor()},
          {Executors.newFixedThreadPool(4)}
        });
  }

  /**
   * Static helper methods that simplifies creating {@link AsyncTransactionFunction}s for Java7.
   * Java8 and higher can use lambda expressions.
   */
  public static class AsyncTransactionManagerHelper {

    public static <I> AsyncTransactionFunction<I, AsyncResultSet> readAsync(
        final String table,
        final KeySet keys,
        final Iterable<String> columns,
        final ReadOption... options) {
      return new AsyncTransactionFunction<I, AsyncResultSet>() {
        @Override
        public ApiFuture<AsyncResultSet> apply(TransactionContext txn, I input) {
          return ApiFutures.immediateFuture(txn.readAsync(table, keys, columns, options));
        }
      };
    }

    public static <I> AsyncTransactionFunction<I, Struct> readRowAsync(
        final String table, final Key key, final Iterable<String> columns) {
      return new AsyncTransactionFunction<I, Struct>() {
        @Override
        public ApiFuture<Struct> apply(TransactionContext txn, I input) {
          return txn.readRowAsync(table, key, columns);
        }
      };
    }

    public static <I> AsyncTransactionFunction<I, Void> buffer(Mutation mutation) {
      return buffer(ImmutableList.of(mutation));
    }

    public static <I> AsyncTransactionFunction<I, Void> buffer(final Iterable<Mutation> mutations) {
      return new AsyncTransactionFunction<I, Void>() {
        @Override
        public ApiFuture<Void> apply(TransactionContext txn, I input) {
          txn.buffer(mutations);
          return ApiFutures.immediateFuture(null);
        }
      };
    }

    public static <I> AsyncTransactionFunction<I, Long> executeUpdateAsync(Statement statement) {
      return executeUpdateAsync(SettableApiFuture.<Long>create(), statement);
    }

    public static <I> AsyncTransactionFunction<I, Long> executeUpdateAsync(
        final SettableApiFuture<Long> result, final Statement statement) {
      return new AsyncTransactionFunction<I, Long>() {
        @Override
        public ApiFuture<Long> apply(TransactionContext txn, I input) {
          ApiFuture<Long> updateCount = txn.executeUpdateAsync(statement);
          ApiFutures.addCallback(
              updateCount,
              new ApiFutureCallback<Long>() {
                @Override
                public void onFailure(Throwable t) {
                  result.setException(t);
                }

                @Override
                public void onSuccess(Long input) {
                  result.set(input);
                }
              },
              MoreExecutors.directExecutor());
          return updateCount;
        }
      };
    }

    public static <I> AsyncTransactionFunction<I, long[]> batchUpdateAsync(
        final Statement... statements) {
      return batchUpdateAsync(SettableApiFuture.<long[]>create(), statements);
    }

    public static <I> AsyncTransactionFunction<I, long[]> batchUpdateAsync(
        final SettableApiFuture<long[]> result, final Statement... statements) {
      return new AsyncTransactionFunction<I, long[]>() {
        @Override
        public ApiFuture<long[]> apply(TransactionContext txn, I input) {
          ApiFuture<long[]> updateCounts = txn.batchUpdateAsync(Arrays.asList(statements));
          ApiFutures.addCallback(
              updateCounts,
              new ApiFutureCallback<long[]>() {
                @Override
                public void onFailure(Throwable t) {
                  result.setException(t);
                }

                @Override
                public void onSuccess(long[] input) {
                  result.set(input);
                }
              },
              MoreExecutors.directExecutor());
          return updateCounts;
        }
      };
    }
  }

  @Test
  public void asyncTransactionManager_shouldRollbackOnCloseAsync() throws Exception {
    AsyncTransactionManager manager = client().transactionManagerAsync();
    TransactionContext txn = manager.beginAsync().get();
    txn.executeUpdateAsync(UPDATE_STATEMENT).get();
    final TransactionSelector selector =
        ((TransactionContextImpl) ((SessionPoolTransactionContext) txn).delegate)
            .getTransactionSelector();

    SpannerApiFutures.get(manager.closeAsync());
    // The mock server should already have the Rollback request, as we are waiting for the returned
    // ApiFuture to be done.
    mockSpanner.waitForRequestsToContain(
        new Predicate<AbstractMessage>() {
          @Override
          public boolean apply(AbstractMessage input) {
            if (input instanceof RollbackRequest) {
              RollbackRequest request = (RollbackRequest) input;
              return request.getTransactionId().equals(selector.getId());
            }
            return false;
          }
        },
        0L);
  }

  @Test
  public void testAsyncTransactionManager_returnsCommitStats() throws Exception {
    try (AsyncTransactionManager manager =
        client().transactionManagerAsync(Options.commitStats())) {
      TransactionContextFuture transaction = manager.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture commitTimestamp =
              transaction
                  .then(
                      AsyncTransactionManagerHelper.<Void>buffer(
                          Mutation.delete("FOO", Key.of("foo"))),
                      executor)
                  .commitAsync();
          assertNotNull(commitTimestamp.get());
          assertNotNull(manager.getCommitResponse().get());
          assertNotNull(manager.getCommitResponse().get().getCommitStats());
          assertEquals(1L, manager.getCommitResponse().get().getCommitStats().getMutationCount());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerUpdate() throws Exception {
    final SettableApiFuture<Long> updateCount = SettableApiFuture.create();

    try (AsyncTransactionManager manager = client().transactionManagerAsync()) {
      TransactionContextFuture txn = manager.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture commitTimestamp =
              txn.then(
                      AsyncTransactionManagerHelper.<Void>executeUpdateAsync(
                          updateCount, UPDATE_STATEMENT),
                      executor)
                  .commitAsync();
          assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
          assertThat(commitTimestamp.get()).isNotNull();
          break;
        } catch (AbortedException e) {
          txn = manager.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerIsNonBlocking() throws Exception {
    SettableApiFuture<Long> updateCount = SettableApiFuture.create();

    mockSpanner.freeze();
    try (AsyncTransactionManager manager = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = manager.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture commitTimestamp =
              txn.then(
                      AsyncTransactionManagerHelper.<Void>executeUpdateAsync(
                          updateCount, UPDATE_STATEMENT),
                      executor)
                  .commitAsync();
          mockSpanner.unfreeze();
          assertThat(updateCount.get(10L, TimeUnit.SECONDS)).isEqualTo(UPDATE_COUNT);
          assertThat(commitTimestamp.get(10L, TimeUnit.SECONDS)).isNotNull();
          break;
        } catch (AbortedException e) {
          txn = manager.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerInvalidUpdate() throws Exception {
    try (AsyncTransactionManager manager = client().transactionManagerAsync()) {
      TransactionContextFuture txn = manager.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture commitTimestamp =
              txn.then(
                      AsyncTransactionManagerHelper.<Void>executeUpdateAsync(
                          INVALID_UPDATE_STATEMENT),
                      executor)
                  .commitAsync();
          commitTimestamp.get();
          fail("missing expected exception");
        } catch (AbortedException e) {
          txn = manager.resetForRetryAsync();
        } catch (ExecutionException e) {
          manager.rollbackAsync();
          assertThat(e.getCause()).isInstanceOf(SpannerException.class);
          SpannerException se = (SpannerException) e.getCause();
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
          assertThat(se.getMessage()).contains("invalid statement");
          break;
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerCommitAborted() throws Exception {
    SettableApiFuture<Long> updateCount = SettableApiFuture.create();
    final AtomicInteger attempt = new AtomicInteger();
    try (AsyncTransactionManager manager = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = manager.beginAsync();
      while (true) {
        try {
          attempt.incrementAndGet();
          CommitTimestampFuture commitTimestamp =
              txn.then(
                      AsyncTransactionManagerHelper.<Void>executeUpdateAsync(
                          updateCount, UPDATE_STATEMENT),
                      executor)
                  .then(
                      new AsyncTransactionFunction<Long, Void>() {
                        @Override
                        public ApiFuture<Void> apply(TransactionContext txn, Long input) {
                          if (attempt.get() == 1) {
                            mockSpanner.abortTransaction(txn);
                          }
                          return ApiFutures.immediateFuture(null);
                        }
                      },
                      executor)
                  .commitAsync();
          assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
          assertThat(commitTimestamp.get()).isNotNull();
          assertThat(attempt.get()).isEqualTo(2);
          break;
        } catch (AbortedException e) {
          txn = manager.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerFireAndForgetInvalidUpdate() throws Exception {
    final SettableApiFuture<Long> updateCount = SettableApiFuture.create();

    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture ts =
              txn.then(
                      new AsyncTransactionFunction<Void, Long>() {
                        @Override
                        public ApiFuture<Long> apply(TransactionContext txn, Void input) {
                          // This fire-and-forget update statement should not fail the transaction.
                          // The exception will however cause the transaction to be retried, as the
                          // statement will not return a transaction id.
                          txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
                          ApiFutures.addCallback(
                              txn.executeUpdateAsync(UPDATE_STATEMENT),
                              new ApiFutureCallback<Long>() {
                                @Override
                                public void onFailure(Throwable t) {
                                  updateCount.setException(t);
                                }

                                @Override
                                public void onSuccess(Long result) {
                                  updateCount.set(result);
                                }
                              },
                              MoreExecutors.directExecutor());
                          return updateCount;
                        }
                      },
                      executor)
                  .commitAsync();
          assertThat(ts.get()).isNotNull();
          assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            // The first update that fails. This will cause a transaction retry.
            ExecuteSqlRequest.class,
            // The retry will use an explicit BeginTransaction call.
            BeginTransactionRequest.class,
            // The first update will again fail, but now there is a transaction id, so the
            // transaction can continue.
            ExecuteSqlRequest.class,
            ExecuteSqlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void asyncTransactionManagerChain() throws Exception {
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture ts =
              txn.then(
                      AsyncTransactionManagerHelper.<Void>executeUpdateAsync(UPDATE_STATEMENT),
                      executor)
                  .then(
                      AsyncTransactionManagerHelper.<Long>readRowAsync(
                          READ_TABLE_NAME, Key.of(1L), READ_COLUMN_NAMES),
                      executor)
                  .then(
                      new AsyncTransactionFunction<Struct, String>() {
                        @Override
                        public ApiFuture<String> apply(TransactionContext txn, Struct input) {
                          return ApiFutures.immediateFuture(input.getString("Value"));
                        }
                      },
                      executor)
                  .then(
                      new AsyncTransactionFunction<String, Void>() {
                        @Override
                        public ApiFuture<Void> apply(TransactionContext txn, String input) {
                          assertThat(input).isEqualTo("v1");
                          return ApiFutures.immediateFuture(null);
                        }
                      },
                      executor)
                  .commitAsync();
          assertThat(ts.get()).isNotNull();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerChainWithErrorInTheMiddle() throws Exception {
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture ts =
              txn.then(
                      AsyncTransactionManagerHelper.<Void>executeUpdateAsync(
                          INVALID_UPDATE_STATEMENT),
                      executor)
                  .then(
                      new AsyncTransactionFunction<Long, Void>() {
                        @Override
                        public ApiFuture<Void> apply(TransactionContext txn, Long input) {
                          throw new IllegalStateException("this should not be executed");
                        }
                      },
                      executor)
                  .commitAsync();
          ts.get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        } catch (ExecutionException e) {
          mgr.rollbackAsync();
          assertThat(e.getCause()).isInstanceOf(SpannerException.class);
          SpannerException se = (SpannerException) e.getCause();
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
          break;
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerUpdateAborted() throws Exception {
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      // Temporarily set the result of the update to 2 rows.
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT + 1L));
      final AtomicInteger attempt = new AtomicInteger();

      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture ts =
              txn.then(
                      new AsyncTransactionFunction<Void, Void>() {
                        @Override
                        public ApiFuture<Void> apply(TransactionContext txn, Void input) {
                          if (attempt.incrementAndGet() == 1) {
                            // Abort the first attempt.
                            mockSpanner.abortNextStatement();
                          } else {
                            // Set the result of the update statement back to 1 row.
                            mockSpanner.putStatementResult(
                                StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                          }
                          return ApiFutures.immediateFuture(null);
                        }
                      },
                      executor)
                  .then(
                      AsyncTransactionManagerHelper.<Void>executeUpdateAsync(UPDATE_STATEMENT),
                      executor)
                  .commitAsync();
          assertThat(ts.get()).isNotNull();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
      assertThat(attempt.get()).isEqualTo(2);
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    }
  }

  @Test
  public void asyncTransactionManagerUpdateAbortedWithoutGettingResult() throws Exception {
    final AtomicInteger attempt = new AtomicInteger();
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture ts =
              txn.then(
                      new AsyncTransactionFunction<Void, Void>() {
                        @Override
                        public ApiFuture<Void> apply(TransactionContext txn, Void input) {
                          if (attempt.incrementAndGet() == 1) {
                            mockSpanner.abortNextStatement();
                          }
                          // This update statement will be aborted, but the error will not
                          // propagated to the transaction runner and cause the transaction to
                          // retry. Instead, the commit call will do that.
                          txn.executeUpdateAsync(UPDATE_STATEMENT);
                          // Resolving this future will not resolve the result of the entire
                          // transaction. The transaction result will be resolved when the commit
                          // has actually finished successfully.
                          return ApiFutures.immediateFuture(null);
                        }
                      },
                      executor)
                  .commitAsync();
          assertThat(ts.get()).isNotNull();
          assertThat(attempt.get()).isEqualTo(2);
          // The server may receive 1 or 2 commit requests depending on whether the call to
          // commitAsync() already knows that the transaction has aborted. If it does, it will not
          // attempt to call the Commit RPC and instead directly propagate the Aborted error.
          assertThat(mockSpanner.getRequestTypes())
              .containsAtLeast(
                  BatchCreateSessionsRequest.class,
                  ExecuteSqlRequest.class,
                  // The retry will use a BeginTransaction RPC.
                  BeginTransactionRequest.class,
                  ExecuteSqlRequest.class,
                  CommitRequest.class);
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerCommitFails() throws Exception {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.RESOURCE_EXHAUSTED
                .withDescription("mutation limit exceeded")
                .asRuntimeException()));
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  AsyncTransactionManagerHelper.<Void>executeUpdateAsync(UPDATE_STATEMENT),
                  executor)
              .commitAsync()
              .get();
          fail("missing expected exception");
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        } catch (ExecutionException e) {
          assertThat(e.getCause()).isInstanceOf(SpannerException.class);
          SpannerException se = (SpannerException) e.getCause();
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
          assertThat(se.getMessage()).contains("mutation limit exceeded");
          break;
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerWaitsUntilAsyncUpdateHasFinished() throws Exception {
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  new AsyncTransactionFunction<Void, Void>() {
                    @Override
                    public ApiFuture<Void> apply(TransactionContext txn, Void input) {
                      // Shoot-and-forget update. The commit will still wait for this request to
                      // finish.
                      txn.executeUpdateAsync(UPDATE_STATEMENT);
                      return ApiFutures.immediateFuture(null);
                    }
                  },
                  executor)
              .commitAsync()
              .get();
          assertThat(mockSpanner.getRequestTypes())
              .containsExactly(
                  BatchCreateSessionsRequest.class, ExecuteSqlRequest.class, CommitRequest.class);
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerBatchUpdate() throws Exception {
    final SettableApiFuture<long[]> result = SettableApiFuture.create();
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  AsyncTransactionManagerHelper.<Void>batchUpdateAsync(
                      result, UPDATE_STATEMENT, UPDATE_STATEMENT),
                  executor)
              .commitAsync()
              .get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(result.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
  }

  @Test
  public void asyncTransactionManagerIsNonBlockingWithBatchUpdate() throws Exception {
    SettableApiFuture<long[]> res = SettableApiFuture.create();
    mockSpanner.freeze();
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          CommitTimestampFuture ts =
              txn.then(
                      AsyncTransactionManagerHelper.<Void>batchUpdateAsync(res, UPDATE_STATEMENT),
                      executor)
                  .commitAsync();
          mockSpanner.unfreeze();
          assertThat(ts.get()).isNotNull();
          assertThat(res.get()).asList().containsExactly(UPDATE_COUNT);
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerInvalidBatchUpdate() throws Exception {
    SettableApiFuture<long[]> result = SettableApiFuture.create();
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  AsyncTransactionManagerHelper.<Void>batchUpdateAsync(
                      result, UPDATE_STATEMENT, INVALID_UPDATE_STATEMENT),
                  executor)
              .commitAsync()
              .get();
          fail("missing expected exception");
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        } catch (ExecutionException e) {
          assertThat(e.getCause()).isInstanceOf(SpannerException.class);
          SpannerException se = (SpannerException) e.getCause();
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
          assertThat(se.getMessage()).contains("invalid statement");
          break;
        }
      }
    }
  }

  @Test
  public void asyncTransactionManagerFireAndForgetInvalidBatchUpdate() throws Exception {
    SettableApiFuture<long[]> result = SettableApiFuture.create();
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  new AsyncTransactionFunction<Void, Void>() {
                    @Override
                    public ApiFuture<Void> apply(TransactionContext txn, Void input) {
                      txn.batchUpdateAsync(
                          ImmutableList.of(UPDATE_STATEMENT, INVALID_UPDATE_STATEMENT));
                      return ApiFutures.immediateFuture(null);
                    }
                  },
                  executor)
              .then(
                  AsyncTransactionManagerHelper.<Void>batchUpdateAsync(
                      result, UPDATE_STATEMENT, UPDATE_STATEMENT),
                  executor)
              .commitAsync()
              .get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(result.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            ExecuteBatchDmlRequest.class,
            ExecuteBatchDmlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void asyncTransactionManagerBatchUpdateAborted() throws Exception {
    final AtomicInteger attempt = new AtomicInteger();
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  new AsyncTransactionFunction<Void, long[]>() {
                    @Override
                    public ApiFuture<long[]> apply(TransactionContext txn, Void input) {
                      if (attempt.incrementAndGet() == 1) {
                        return txn.batchUpdateAsync(
                            ImmutableList.of(UPDATE_STATEMENT, UPDATE_ABORTED_STATEMENT));
                      } else {
                        return txn.batchUpdateAsync(
                            ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
                      }
                    }
                  },
                  executor)
              .commitAsync()
              .get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(attempt.get()).isEqualTo(2);
    // There should only be 1 CommitRequest, as the first attempt should abort already after the
    // ExecuteBatchDmlRequest.
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            ExecuteBatchDmlRequest.class,
            BeginTransactionRequest.class,
            ExecuteBatchDmlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void asyncTransactionManagerBatchUpdateAbortedBeforeFirstStatement() throws Exception {
    final AtomicInteger attempt = new AtomicInteger();
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  new AsyncTransactionFunction<Void, long[]>() {
                    @Override
                    public ApiFuture<long[]> apply(TransactionContext txn, Void input) {
                      if (attempt.incrementAndGet() == 1) {
                        mockSpanner.abortNextStatement();
                      }
                      return txn.batchUpdateAsync(
                          ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
                    }
                  },
                  executor)
              .commitAsync()
              .get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(attempt.get()).isEqualTo(2);
    // There should only be 1 CommitRequest, as the first attempt should abort already after the
    // ExecuteBatchDmlRequest.
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            ExecuteBatchDmlRequest.class,
            BeginTransactionRequest.class,
            ExecuteBatchDmlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void asyncTransactionManagerWithBatchUpdateCommitAborted() throws Exception {
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      // Temporarily set the result of the update to 2 rows.
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT + 1L));
      final AtomicInteger attempt = new AtomicInteger();
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        final SettableApiFuture<long[]> result = SettableApiFuture.create();
        try {
          txn.then(
                  new AsyncTransactionFunction<Void, Void>() {
                    @Override
                    public ApiFuture<Void> apply(TransactionContext txn, Void input) {
                      if (attempt.get() > 0) {
                        // Set the result of the update statement back to 1 row.
                        mockSpanner.putStatementResult(
                            StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                      }
                      return ApiFutures.immediateFuture(null);
                    }
                  },
                  executor)
              .then(
                  AsyncTransactionManagerHelper.<Void>batchUpdateAsync(
                      result, UPDATE_STATEMENT, UPDATE_STATEMENT),
                  executor)
              .then(
                  new AsyncTransactionFunction<long[], Void>() {
                    @Override
                    public ApiFuture<Void> apply(TransactionContext txn, long[] input) {
                      if (attempt.incrementAndGet() == 1) {
                        mockSpanner.abortTransaction(txn);
                      }
                      return ApiFutures.immediateFuture(null);
                    }
                  },
                  executor)
              .commitAsync()
              .get();
          assertThat(result.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
          assertThat(attempt.get()).isEqualTo(2);
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    }
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            ExecuteBatchDmlRequest.class,
            CommitRequest.class,
            BeginTransactionRequest.class,
            ExecuteBatchDmlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void asyncTransactionManagerBatchUpdateAbortedWithoutGettingResult() throws Exception {
    final AtomicInteger attempt = new AtomicInteger();
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  new AsyncTransactionFunction<Void, Void>() {
                    @Override
                    public ApiFuture<Void> apply(TransactionContext txn, Void input) {
                      if (attempt.incrementAndGet() == 1) {
                        mockSpanner.abortNextStatement();
                      }
                      // This update statement will be aborted, but the error will not propagated to
                      // the transaction manager and cause the transaction to retry. Instead, the
                      // commit call will do that. Depending on the timing, that will happen
                      // directly in the transaction manager if the ABORTED error has already been
                      // returned by the batch update call before the commit call starts. Otherwise,
                      // the backend will return an ABORTED error for the commit call.
                      txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
                      return ApiFutures.immediateFuture(null);
                    }
                  },
                  executor)
              .commitAsync()
              .get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(attempt.get()).isEqualTo(2);
    Iterable<Class<? extends AbstractMessage>> requests = mockSpanner.getRequestTypes();
    int size = Iterables.size(requests);
    assertThat(size).isIn(Range.closed(5, 6));
    if (size == 5) {
      assertThat(requests)
          .containsExactly(
              BatchCreateSessionsRequest.class,
              ExecuteBatchDmlRequest.class,
              BeginTransactionRequest.class,
              ExecuteBatchDmlRequest.class,
              CommitRequest.class);
    } else {
      assertThat(requests)
          .containsExactly(
              BatchCreateSessionsRequest.class,
              ExecuteBatchDmlRequest.class,
              CommitRequest.class,
              BeginTransactionRequest.class,
              ExecuteBatchDmlRequest.class,
              CommitRequest.class);
    }
  }

  @Test
  public void asyncTransactionManagerWithBatchUpdateCommitFails() throws Exception {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.RESOURCE_EXHAUSTED
                .withDescription("mutation limit exceeded")
                .asRuntimeException()));
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  AsyncTransactionManagerHelper.<Void>batchUpdateAsync(
                      UPDATE_STATEMENT, UPDATE_STATEMENT),
                  executor)
              .commitAsync()
              .get();
          fail("missing expected exception");
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        } catch (ExecutionException e) {
          assertThat(e.getCause()).isInstanceOf(SpannerException.class);
          SpannerException se = (SpannerException) e.getCause();
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
          assertThat(se.getMessage()).contains("mutation limit exceeded");
          break;
        }
      }
    }
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class, ExecuteBatchDmlRequest.class, CommitRequest.class);
  }

  @Test
  public void asyncTransactionManagerWaitsUntilAsyncBatchUpdateHasFinished() throws Exception {
    try (AsyncTransactionManager mgr = clientWithEmptySessionPool().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          txn.then(
                  new AsyncTransactionFunction<Void, Void>() {
                    @Override
                    public ApiFuture<Void> apply(TransactionContext txn, Void input) {
                      txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT));
                      return ApiFutures.immediateFuture(null);
                    }
                  },
                  executor)
              .commitAsync()
              .get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class, ExecuteBatchDmlRequest.class, CommitRequest.class);
  }

  @Test
  public void asyncTransactionManagerReadRow() throws Exception {
    ApiFuture<String> val;
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Struct, String> step;
          val =
              step =
                  txn.then(
                          AsyncTransactionManagerHelper.<Void>readRowAsync(
                              READ_TABLE_NAME, Key.of(1L), READ_COLUMN_NAMES),
                          executor)
                      .then(
                          new AsyncTransactionFunction<Struct, String>() {
                            @Override
                            public ApiFuture<String> apply(TransactionContext txn, Struct input) {
                              return ApiFutures.immediateFuture(input.getString("Value"));
                            }
                          },
                          executor);
          step.commitAsync().get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(val.get()).isEqualTo("v1");
  }

  @Test
  public void asyncTransactionManagerRead() throws Exception {
    AsyncTransactionStep<Void, List<String>> res;
    try (AsyncTransactionManager mgr = client().transactionManagerAsync()) {
      TransactionContextFuture txn = mgr.beginAsync();
      while (true) {
        try {
          res =
              txn.then(
                  new AsyncTransactionFunction<Void, List<String>>() {
                    @Override
                    public ApiFuture<List<String>> apply(TransactionContext txn, Void input) {
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
          // Commit the transaction.
          res.commitAsync().get();
          break;
        } catch (AbortedException e) {
          txn = mgr.resetForRetryAsync();
        }
      }
    }
    assertThat(res.get()).containsExactly("v1", "v2", "v3");
  }

  @Test
  public void asyncTransactionManagerQuery() throws Exception {
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("SELECT FirstName FROM Singers WHERE ID=1"),
            MockSpannerTestUtil.READ_FIRST_NAME_SINGERS_RESULTSET));
    final long singerId = 1L;
    try (AsyncTransactionManager manager = client().transactionManagerAsync()) {
      TransactionContextFuture txn = manager.beginAsync();
      while (true) {
        final String column = "FirstName";
        CommitTimestampFuture commitTimestamp =
            txn.then(
                    new AsyncTransactionFunction<Void, Struct>() {
                      @Override
                      public ApiFuture<Struct> apply(TransactionContext txn, Void input) {
                        return txn.readRowAsync(
                            "Singers", Key.of(singerId), Collections.singleton(column));
                      }
                    },
                    executor)
                .then(
                    new AsyncTransactionFunction<Struct, Void>() {
                      @Override
                      public ApiFuture<Void> apply(TransactionContext txn, Struct input) {
                        String name = input.getString(column);
                        txn.buffer(
                            Mutation.newUpdateBuilder("Singers")
                                .set(column)
                                .to(name.toUpperCase())
                                .build());
                        return ApiFutures.immediateFuture(null);
                      }
                    },
                    executor)
                .commitAsync();
        try {
          commitTimestamp.get();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          txn = manager.resetForRetryAsync();
        }
      }
    }
  }

  @Test
  public void asyncTransactionManager_shouldPropagateStatementFailure()
      throws ExecutionException, InterruptedException, TimeoutException {
    DatabaseClient dbClient = client();
    try (AsyncTransactionManager transactionManager = dbClient.transactionManagerAsync()) {
      TransactionContextFuture txnContextFuture = transactionManager.beginAsync();
      AsyncTransactionStep<Void, Long> updateFuture =
          txnContextFuture.then(
              new AsyncTransactionFunction<Void, Long>() {
                @Override
                public ApiFuture<Long> apply(TransactionContext txn, Void input) {
                  return txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
                }
              },
              executor);
      final SettableApiFuture<Void> res = SettableApiFuture.create();
      ApiFutures.addCallback(
          updateFuture,
          new ApiFutureCallback<Long>() {
            @Override
            public void onFailure(Throwable throwable) {
              // Check that we got the expected failure.
              try {
                assertThat(throwable).isInstanceOf(SpannerException.class);
                SpannerException e = (SpannerException) throwable;
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                assertThat(e.getMessage()).contains("invalid statement");
                res.set(null);
              } catch (Throwable t) {
                res.setException(t);
              }
            }

            @Override
            public void onSuccess(Long aLong) {
              res.setException(new AssertionError("Statement should not succeed."));
            }
          },
          executor);

      assertThat(res.get(10L, TimeUnit.SECONDS)).isNull();
    }
  }
}
