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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AsyncRunnerTest extends AbstractAsyncTransactionTest {

  @Test
  public void testAsyncRunner_doesNotReturnCommitTimestampBeforeCommit() {
    AsyncRunner runner = client().runAsync();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> runner.getCommitTimestamp());
    assertTrue(e.getMessage().contains("runAsync() has not yet been called"));
  }

  @Test
  public void testAsyncRunner_doesNotReturnCommitResponseBeforeCommit() {
    AsyncRunner runner = client().runAsync();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> runner.getCommitResponse());
    assertTrue(e.getMessage().contains("runAsync() has not yet been called"));
  }

  @Test
  public void asyncRunnerUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<Long> updateCount =
        runner.runAsync(txn -> txn.executeUpdateAsync(UPDATE_STATEMENT), executor);
    assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
  }

  @Test
  public void asyncRunnerIsNonBlocking() throws Exception {
    mockSpanner.freeze();
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> res =
        runner.runAsync(
            txn -> {
              txn.executeUpdateAsync(UPDATE_STATEMENT);
              return ApiFutures.immediateFuture(null);
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
        runner.runAsync(txn -> txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT), executor);
    ExecutionException e = assertThrows(ExecutionException.class, () -> updateCount.get());
    assertThat(e.getCause()).isInstanceOf(SpannerException.class);
    SpannerException se = (SpannerException) e.getCause();
    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    assertThat(se.getMessage()).contains("invalid statement");
  }

  @Test
  public void asyncRunnerFireAndForgetInvalidUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<Long> res =
        runner.runAsync(
            txn -> {
              txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
              return txn.executeUpdateAsync(UPDATE_STATEMENT);
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
              txn -> {
                if (attempt.incrementAndGet() == 1) {
                  mockSpanner.abortNextStatement();
                } else {
                  // Set the result of the update statement back to 1 row.
                  mockSpanner.putStatementResult(
                      StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                }
                return txn.executeUpdateAsync(UPDATE_STATEMENT);
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
              txn -> {
                if (attempt.get() > 0) {
                  // Set the result of the update statement back to 1 row.
                  mockSpanner.putStatementResult(
                      StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                }
                ApiFuture<Long> updateCount1 = txn.executeUpdateAsync(UPDATE_STATEMENT);
                if (attempt.incrementAndGet() == 1) {
                  mockSpanner.abortTransaction(txn);
                }
                return updateCount1;
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
            txn -> {
              if (attempt.incrementAndGet() == 1) {
                mockSpanner.abortNextStatement();
              }
              // This update statement will be aborted, but the error will not propagated to the
              // transaction runner and cause the transaction to retry. Instead, the commit call
              // will do that.
              txn.executeUpdateAsync(UPDATE_STATEMENT);
              // Resolving this future will not resolve the result of the entire transaction. The
              // transaction result will be resolved when the commit has actually finished
              // successfully.
              return ApiFutures.immediateFuture(null);
            },
            executor);
    assertThat(result.get()).isNull();
    assertThat(attempt.get()).isEqualTo(2);
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            ExecuteSqlRequest.class,
            // The retry will use an explicit BeginTransaction RPC because the first statement of
            // the transaction did not return a transaction id during the initial attempt.
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
            txn -> {
              // This statement will succeed, but the commit will fail. The error from the commit
              // will bubble up to the future that is returned by the transaction, and the update
              // count returned here will never reach the user application.
              return txn.executeUpdateAsync(UPDATE_STATEMENT);
            },
            executor);
    ExecutionException e = assertThrows(ExecutionException.class, () -> updateCount.get());
    assertThat(e.getCause()).isInstanceOf(SpannerException.class);
    SpannerException se = (SpannerException) e.getCause();
    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
    assertThat(se.getMessage()).contains("mutation limit exceeded");
  }

  @Test
  public void asyncRunnerWaitsUntilAsyncUpdateHasFinished() throws Exception {
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> res =
        runner.runAsync(
            txn -> {
              txn.executeUpdateAsync(UPDATE_STATEMENT);
              return ApiFutures.immediateFuture(null);
            },
            executor);
    res.get();
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class, ExecuteSqlRequest.class, CommitRequest.class);
  }

  @Test
  public void asyncRunnerBatchUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<long[]> updateCount =
        runner.runAsync(
            txn -> txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT)),
            executor);
    assertThat(updateCount.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
  }

  @Test
  public void asyncRunnerIsNonBlockingWithBatchUpdate() throws Exception {
    mockSpanner.freeze();
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> res =
        runner.runAsync(
            txn -> {
              txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT));
              return ApiFutures.immediateFuture(null);
            },
            executor);
    ApiFuture<Timestamp> ts = runner.getCommitTimestamp();
    mockSpanner.unfreeze();
    assertThat(res.get()).isNull();
    assertThat(ts.get()).isNotNull();
  }

  @Test
  public void asyncRunnerInvalidBatchUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<long[]> updateCount =
        runner.runAsync(
            txn ->
                txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, INVALID_UPDATE_STATEMENT)),
            executor);
    ExecutionException e = assertThrows(ExecutionException.class, () -> updateCount.get());
    assertThat(e.getCause()).isInstanceOf(SpannerException.class);
    SpannerException se = (SpannerException) e.getCause();
    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    assertThat(se.getMessage()).contains("invalid statement");
  }

  @Test
  public void asyncRunnerFireAndForgetInvalidBatchUpdate() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<long[]> res =
        runner.runAsync(
            txn -> {
              txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, INVALID_UPDATE_STATEMENT));
              return txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
            },
            executor);
    assertThat(res.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
  }

  @Test
  public void asyncRunnerBatchUpdateAborted() throws Exception {
    final AtomicInteger attempt = new AtomicInteger();
    AsyncRunner runner = client().runAsync();
    ApiFuture<long[]> updateCount =
        runner.runAsync(
            txn -> {
              if (attempt.incrementAndGet() == 1) {
                return txn.batchUpdateAsync(
                    ImmutableList.of(UPDATE_STATEMENT, UPDATE_ABORTED_STATEMENT));
              } else {
                return txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
              }
            },
            executor);
    assertThat(updateCount.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
    assertThat(attempt.get()).isEqualTo(2);
  }

  @Test
  public void asyncRunnerWithBatchUpdateCommitAborted() throws Exception {
    try {
      // Temporarily set the result of the update to 2 rows.
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT + 1L));
      final AtomicInteger attempt = new AtomicInteger();
      AsyncRunner runner = client().runAsync();
      ApiFuture<long[]> updateCount =
          runner.runAsync(
              txn -> {
                if (attempt.get() > 0) {
                  // Set the result of the update statement back to 1 row.
                  mockSpanner.putStatementResult(
                      StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                }
                ApiFuture<long[]> updateCount1 =
                    txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
                if (attempt.incrementAndGet() == 1) {
                  mockSpanner.abortTransaction(txn);
                }
                return updateCount1;
              },
              executor);
      assertThat(updateCount.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
      assertThat(attempt.get()).isEqualTo(2);
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    }
  }

  @Test
  public void asyncRunnerBatchUpdateAbortedWithoutGettingResult() throws Exception {
    final AtomicInteger attempt = new AtomicInteger();
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> result =
        runner.runAsync(
            txn -> {
              if (attempt.incrementAndGet() == 1) {
                mockSpanner.abortNextTransaction();
              }
              // This statement will succeed and return a transaction id. The transaction will be
              // marked as aborted on the mock server.
              txn.executeUpdate(UPDATE_STATEMENT);

              // This batch update statement will be aborted, but the error will not propagated to
              // the
              // transaction runner and cause the transaction to retry. Instead, the commit call
              // will do that.
              txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
              // Resolving this future will not resolve the result of the entire transaction. The
              // transaction result will be resolved when the commit has actually finished
              // successfully.
              return ApiFutures.immediateFuture(null);
            },
            executor);
    assertThat(result.get()).isNull();
    assertThat(attempt.get()).isEqualTo(2);
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class,
            ExecuteSqlRequest.class,
            ExecuteBatchDmlRequest.class,
            CommitRequest.class,
            ExecuteSqlRequest.class,
            ExecuteBatchDmlRequest.class,
            CommitRequest.class);
  }

  @Test
  public void asyncRunnerWithBatchUpdateCommitFails() throws Exception {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.RESOURCE_EXHAUSTED
                .withDescription("mutation limit exceeded")
                .asRuntimeException()));
    AsyncRunner runner = client().runAsync();
    ApiFuture<long[]> updateCount =
        runner.runAsync(
            txn -> {
              // This statement will succeed, but the commit will fail. The error from the commit
              // will bubble up to the future that is returned by the transaction, and the update
              // count returned here will never reach the user application.
              return txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT, UPDATE_STATEMENT));
            },
            executor);
    ExecutionException e = assertThrows(ExecutionException.class, () -> updateCount.get());
    assertThat(e.getCause()).isInstanceOf(SpannerException.class);
    SpannerException se = (SpannerException) e.getCause();
    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
    assertThat(se.getMessage()).contains("mutation limit exceeded");
  }

  @Test
  public void asyncRunnerWaitsUntilAsyncBatchUpdateHasFinished() throws Exception {
    AsyncRunner runner = clientWithEmptySessionPool().runAsync();
    ApiFuture<Void> res =
        runner.runAsync(
            txn -> {
              txn.batchUpdateAsync(ImmutableList.of(UPDATE_STATEMENT));
              return ApiFutures.immediateFuture(null);
            },
            executor);
    res.get();
    assertThat(mockSpanner.getRequestTypes())
        .containsExactly(
            BatchCreateSessionsRequest.class, ExecuteBatchDmlRequest.class, CommitRequest.class);
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
    final CountDownLatch dataChecked = new CountDownLatch(1);
    ApiFuture<Void> res =
        runner.runAsync(
            txn -> {
              try (AsyncResultSet rs =
                  txn.readAsync(
                      READ_TABLE_NAME, KeySet.all(), READ_COLUMN_NAMES, Options.bufferRows(1))) {
                rs.setCallback(
                    Executors.newSingleThreadExecutor(),
                    resultSet -> {
                      dataReceived.countDown();
                      try {
                        while (true) {
                          switch (resultSet.tryNext()) {
                            case DONE:
                              finished.set(true);
                              return CallbackResponse.DONE;
                            case NOT_READY:
                              return CallbackResponse.CONTINUE;
                            case OK:
                              dataChecked.await();
                              results.put(resultSet.getString(0));
                          }
                        }
                      } catch (Throwable t) {
                        finished.setException(t);
                        return CallbackResponse.DONE;
                      }
                    });
              }
              try {
                dataReceived.await();
                return ApiFutures.immediateFuture(null);
              } catch (InterruptedException e) {
                return ApiFutures.immediateFailedFuture(
                    SpannerExceptionFactory.propagateInterrupt(e));
              }
            },
            executor);
    // Wait until at least one row has been fetched. At that moment there should be one session
    // checked out.
    dataReceived.await();
    assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(1);
    assertThat(res.isDone()).isFalse();
    dataChecked.countDown();
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
            txn ->
                ApiFutures.transform(
                    txn.readRowAsync(READ_TABLE_NAME, Key.of(1L), READ_COLUMN_NAMES),
                    input -> input.getString("Value"),
                    MoreExecutors.directExecutor()),
            executor);
    assertThat(val.get()).isEqualTo("v1");
  }

  @Test
  public void asyncRunnerRead() throws Exception {
    AsyncRunner runner = client().runAsync();
    ApiFuture<List<String>> val =
        runner.runAsync(
            txn ->
                txn.readAsync(READ_TABLE_NAME, KeySet.all(), READ_COLUMN_NAMES)
                    .toListAsync(input -> input.getString("Value"), MoreExecutors.directExecutor()),
            executor);
    assertThat(val.get()).containsExactly("v1", "v2", "v3");
  }
}
