/*
 * Copyright 2019 Google LLC
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions.Builder;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.AbstractConnectionImplTest.ConnectionConsumer;
import com.google.cloud.spanner.connection.ConnectionOptions.SpannerOptionsConfigurator;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.ITConnection;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.longrunning.Operation;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.Status;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class StatementTimeoutTest extends AbstractMockServerTest {

  private static final String SLOW_SELECT = "SELECT foo FROM bar";
  private static final String INVALID_SELECT = "SELECT FROM bar"; // missing columns / *
  private static final String SLOW_DDL = "CREATE TABLE foo";
  private static final String FAST_DDL = "CREATE TABLE fast_table";
  private static final String SLOW_UPDATE = "UPDATE foo SET col1=1 WHERE id=2";

  /** Execution time for statements that have been defined as slow. */
  private static final int EXECUTION_TIME_SLOW_STATEMENT = 10_000;
  /**
   * This timeout should be high enough that it will never be exceeded, even on a slow build
   * environment, but still significantly lower than the expected execution time of the slow
   * statements.
   */
  private static final long TIMEOUT_FOR_FAST_STATEMENTS = 1000L;

  /**
   * This timeout should be low enough that it will not make the test case unnecessarily slow, but
   * still high enough that it would normally not be exceeded for a statement that is executed
   * directly.
   */
  private static final int TIMEOUT_FOR_SLOW_STATEMENTS = 50;

  ITConnection createConnection() {
    StringBuilder url = new StringBuilder(getBaseUrl());
    ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(url.toString())
            .setConfigurator(
                new SpannerOptionsConfigurator() {
                  @Override
                  public void configure(Builder options) {
                    options
                        .getDatabaseAdminStubSettingsBuilder()
                        .updateDatabaseDdlOperationSettings()
                        .setPollingAlgorithm(
                            OperationTimedPollAlgorithm.create(
                                RetrySettings.newBuilder()
                                    .setInitialRetryDelay(Duration.ofMillis(1L))
                                    .setMaxRetryDelay(Duration.ofMillis(1L))
                                    .setRetryDelayMultiplier(1.0)
                                    .setTotalTimeout(Duration.ofMinutes(10L))
                                    .build()));
                  }
                })
            .build();
    return createITConnection(options);
  }

  @After
  public void clearExecutionTimes() {
    mockSpanner.removeAllExecutionTimes();
  }

  @Test
  public void testTimeoutExceptionReadOnlyAutocommit() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setReadOnly(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadOnlyAutocommitMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setReadOnly(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      // assert that multiple statements after each other also time out
      for (int i = 0; i < 2; i++) {
        try {
          connection.executeQuery(SELECT_RANDOM_STATEMENT);
          fail("missing expected exception");
        } catch (SpannerException e) {
          assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
        }
      }
      // try to do a new query that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadOnlyTransactional() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setReadOnly(true);
      connection.setAutocommit(false);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadOnlyTransactionMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setReadOnly(true);
      connection.setAutocommit(false);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      // assert that multiple statements after each other also time out
      for (int i = 0; i < 2; i++) {
        try {
          connection.executeQuery(SELECT_RANDOM_STATEMENT);
          fail("missing expected exception");
        } catch (SpannerException e) {
          assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
        }
      }
      // do a rollback without any chance of a timeout
      connection.clearStatementTimeout();
      connection.rollback();
      // try to do a new query that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteAutocommit() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteAutocommitMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      // assert that multiple statements after each other also time out
      for (int i = 0; i < 2; i++) {
        try {
          connection.executeQuery(SELECT_RANDOM_STATEMENT);
          fail("missing expected exception");
        } catch (SpannerException e) {
          assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
        }
      }
      // try to do a new query that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteAutocommitSlowUpdate() {
    mockSpanner.setExecuteSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.execute(INSERT_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteAutocommitSlowUpdateMultipleStatements() {
    mockSpanner.setExecuteSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);

      // assert that multiple statements after each other also time out
      for (int i = 0; i < 2; i++) {
        try {
          connection.execute(Statement.of(SLOW_UPDATE));
          fail("missing expected exception");
        } catch (SpannerException e) {
          assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
        }
      }
      // try to do a new update that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      assertThat(connection.execute(INSERT_STATEMENT).getUpdateCount(), is(equalTo(UPDATE_COUNT)));
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteAutocommitSlowCommit() {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      // First verify that the fast update does not timeout when in transactional mode (as it is the
      // commit that is slow).
      connection.setAutocommit(false);
      connection.execute(INSERT_STATEMENT);
      connection.rollback();

      // Then verify that the update does timeout when executed in autocommit mode, as the commit
      // gRPC call will be slow.
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      connection.setAutocommit(true);
      try {
        connection.execute(INSERT_STATEMENT);
        fail("missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteAutocommitSlowCommitMultipleStatements() {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      // assert that multiple statements after each other also time out
      for (int i = 0; i < 2; i++) {
        try {
          connection.execute(INSERT_STATEMENT);
          fail("Missing expected exception");
        } catch (SpannerException e) {
          assertThat(e.getErrorCode(), is(equalTo(ErrorCode.DEADLINE_EXCEEDED)));
        }
      }
      // try to do a query in autocommit mode. This will use a single-use read-only transaction that
      // does not need to commit, i.e. it should succeed.
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteAutocommitPartitioned() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setAutocommitDmlMode(AutocommitDmlMode.PARTITIONED_NON_ATOMIC);
      // First verify that the statement will not timeout by default.
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      connection.execute(INSERT_STATEMENT);

      // Now slow down the execution and verify that it times out. PDML uses the ExecuteStreamingSql
      // RPC.
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.execute(INSERT_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteTransactional() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteTransactionMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      // Assert that multiple statements after each other will timeout the first time, and then
      // throw a SpannerException with code FAILED_PRECONDITION.
      for (int i = 0; i < 2; i++) {
        try {
          connection.executeQuery(SELECT_RANDOM_STATEMENT);
          fail("Missing expected exception");
        } catch (SpannerException e) {
          if (i == 0) {
            assertThat(e.getErrorCode(), is(equalTo(ErrorCode.DEADLINE_EXCEEDED)));
          } else {
            assertThat(e.getErrorCode(), is(equalTo(ErrorCode.FAILED_PRECONDITION)));
          }
        }
      }
      // do a rollback without any chance of a timeout
      connection.clearStatementTimeout();
      connection.rollback();
      // try to do a new query that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteTransactionalSlowCommit() {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);

      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }

      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.commit();
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testTimeoutExceptionReadWriteTransactionalSlowRollback() {
    mockSpanner.setRollbackExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);

      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      // Rollback timeouts are not propagated as exceptions, as all errors during a Rollback RPC are
      // ignored by the client library.
      connection.rollback();
    }
  }

  private static final class ConnectionReadOnlyAutocommit implements ConnectionConsumer {
    @Override
    public void accept(Connection t) {
      t.setAutocommit(true);
      t.setReadOnly(true);
    }
  }

  @Test
  public void testInterruptedExceptionReadOnlyAutocommit()
      throws InterruptedException, ExecutionException {
    testInterruptedException(new ConnectionReadOnlyAutocommit());
  }

  private static final class ConnectionReadOnlyTransactional implements ConnectionConsumer {
    @Override
    public void accept(Connection t) {
      t.setReadOnly(true);
      t.setAutocommit(false);
    }
  }

  @Test
  public void testInterruptedExceptionReadOnlyTransactional()
      throws InterruptedException, ExecutionException {
    testInterruptedException(new ConnectionReadOnlyTransactional());
  }

  private static final class ConnectionReadWriteAutocommit implements ConnectionConsumer {
    @Override
    public void accept(Connection t) {
      t.setAutocommit(true);
      t.setReadOnly(false);
    }
  }

  @Test
  public void testInterruptedExceptionReadWriteAutocommit()
      throws InterruptedException, ExecutionException {
    testInterruptedException(new ConnectionReadWriteAutocommit());
  }

  private static final class ConnectionReadWriteTransactional implements ConnectionConsumer {
    @Override
    public void accept(Connection t) {
      t.setAutocommit(false);
      t.setReadOnly(false);
    }
  }

  @Test
  public void testInterruptedExceptionReadWriteTransactional()
      throws InterruptedException, ExecutionException {
    testInterruptedException(new ConnectionReadWriteTransactional());
  }

  private void testInterruptedException(final ConnectionConsumer consumer)
      throws InterruptedException, ExecutionException {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    final CountDownLatch latch = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Boolean> future =
        executor.submit(
            new Callable<Boolean>() {
              @Override
              public Boolean call() {
                try (Connection connection = createConnection()) {
                  consumer.accept(connection);
                  connection.setStatementTimeout(10000L, TimeUnit.MILLISECONDS);

                  latch.countDown();
                  try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {}
                  return false;
                } catch (SpannerException e) {
                  return e.getErrorCode() == ErrorCode.CANCELLED;
                }
              }
            });
    latch.await(10L, TimeUnit.SECONDS);
    executor.shutdownNow();
    assertThat(future.get(), is(true));
  }

  @Test
  public void testInvalidQueryReadOnlyAutocommit() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(Status.INVALID_ARGUMENT.asRuntimeException()));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setReadOnly(true);
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(Statement.of(INVALID_SELECT));
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.INVALID_ARGUMENT, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testInvalidQueryReadOnlyTransactional() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(Status.INVALID_ARGUMENT.asRuntimeException()));

    try (Connection connection = createConnection()) {
      connection.setReadOnly(true);
      connection.setAutocommit(false);
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(Statement.of(INVALID_SELECT));
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.INVALID_ARGUMENT, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testInvalidQueryReadWriteAutocommit() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(Status.INVALID_ARGUMENT.asRuntimeException()));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(Statement.of(INVALID_SELECT));
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.INVALID_ARGUMENT, ex.getErrorCode());
      }
    }
  }

  @Test
  public void testInvalidQueryReadWriteTransactional() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(Status.INVALID_ARGUMENT.asRuntimeException()));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try {
        connection.executeQuery(Statement.of(INVALID_SELECT));
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.INVALID_ARGUMENT, ex.getErrorCode());
      }
    }
  }

  static void waitForRequestsToContain(Class<? extends AbstractMessage> request) {
    try {
      mockSpanner.waitForRequestsToContain(request, EXECUTION_TIME_SLOW_STATEMENT);
    } catch (InterruptedException e) {
      throw SpannerExceptionFactory.propagateInterrupt(e);
    } catch (TimeoutException e) {
      throw SpannerExceptionFactory.propagateTimeout(e);
    }
  }

  @Test
  public void testCancelReadOnlyAutocommit() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setReadOnly(true);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadOnlyAutocommitMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setReadOnly(true);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });

      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        fail("Missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode(), is(equalTo(ErrorCode.CANCELLED)));
      }

      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadOnlyTransactional() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setReadOnly(true);
      connection.setAutocommit(false);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadOnlyTransactionalMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setReadOnly(true);
      connection.setAutocommit(false);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      try {
        connection.executeQuery(Statement.of(SLOW_SELECT));
        fail("Missing expected exception");
      } catch (SpannerException e) {
        assertEquals(ErrorCode.CANCELLED, e.getErrorCode());
      }

      // try to do a new query that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
      // rollback and do another fast query
      connection.rollback();
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadWriteAutocommit() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadWriteAutocommitMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
      }

      // try to do a new query that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadWriteAutocommitSlowUpdate() {
    mockSpanner.setExecuteSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      try {
        connection.execute(INSERT_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException ex) {
        assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadWriteAutocommitSlowCommit() {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(CommitRequest.class);
              connection.cancel();
            }
          });
      connection.execute(INSERT_STATEMENT);
      fail("Missing expected exception");
    } catch (SpannerException ex) {
      assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadWriteTransactional() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      connection.executeQuery(SELECT_RANDOM_STATEMENT);
      fail("Missing expected exception");
    } catch (SpannerException ex) {
      assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelReadWriteTransactionalMultipleStatements() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              waitForRequestsToContain(ExecuteSqlRequest.class);
              connection.cancel();
            }
          });
      try {
        connection.executeQuery(SELECT_RANDOM_STATEMENT);
        fail("Missing expected exception");
      } catch (SpannerException e) {
        assertEquals(ErrorCode.CANCELLED, e.getErrorCode());
      }
      // Rollback the transaction as it is no longer usable.
      connection.rollback();

      // Try to do a new query that is fast.
      mockSpanner.removeAllExecutionTimes();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      try (ResultSet rs = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        assertThat(rs, is(notNullValue()));
      }
    } finally {
      executor.shutdown();
    }
  }

  static void addSlowMockDdlOperation() {
    addSlowMockDdlOperations(1);
  }

  static void addSlowMockDdlOperations(int count) {
    addMockDdlOperations(count, false);
  }

  static void addFastMockDdlOperation() {
    addFastMockDdlOperations(1);
  }

  static void addFastMockDdlOperations(int count) {
    addMockDdlOperations(count, true);
  }

  static void addMockDdlOperations(int count, boolean done) {
    for (int i = 0; i < count; i++) {
      mockDatabaseAdmin.addResponse(
          Operation.newBuilder()
              .setMetadata(
                  Any.pack(
                      UpdateDatabaseDdlMetadata.newBuilder()
                          .addStatements(SLOW_DDL)
                          .setDatabase("projects/proj/instances/inst/databases/db")
                          .build()))
              .setName("projects/proj/instances/inst/databases/db/operations/1")
              .setDone(done)
              .setResponse(Any.pack(Empty.getDefaultInstance()))
              .build());
    }
  }

  @Test
  public void testCancelDdlBatch() {
    addSlowMockDdlOperation();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.startBatchDdl();
      connection.execute(Statement.of(SLOW_DDL));
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
              connection.cancel();
            }
          });
      connection.runBatch();
      fail("Missing expected exception");
    } catch (SpannerException ex) {
      assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testCancelDdlAutocommit() {
    addSlowMockDdlOperation();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
              connection.cancel();
            }
          });
      connection.execute(Statement.of(SLOW_DDL));
      fail("Missing expected exception");
    } catch (SpannerException ex) {
      assertEquals(ErrorCode.CANCELLED, ex.getErrorCode());
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testTimeoutExceptionDdlAutocommit() {
    addSlowMockDdlOperations(10);

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);
      connection.execute(Statement.of(SLOW_DDL));
      fail("Missing expected exception");
    } catch (SpannerException ex) {
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
    }
  }

  @Test
  public void testTimeoutExceptionDdlAutocommitMultipleStatements() {
    addSlowMockDdlOperations(20);

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);

      // assert that multiple statements after each other also time out
      for (int i = 0; i < 2; i++) {
        try {
          connection.execute(Statement.of(SLOW_DDL));
          fail("Missing expected exception");
        } catch (SpannerException e) {
          assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
        }
      }
      // try to do a new DDL statement that is fast.
      mockDatabaseAdmin.reset();
      addFastMockDdlOperation();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      assertThat(connection.execute(Statement.of(FAST_DDL)), is(notNullValue()));
    }
  }

  @Test
  public void testTimeoutExceptionDdlBatch() {
    addSlowMockDdlOperations(10);

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.startBatchDdl();
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);

      // the following statement will NOT timeout as the statement is only buffered locally
      connection.execute(Statement.of(SLOW_DDL));
      // the runBatch() statement sends the statement to the server and should timeout
      connection.runBatch();
      fail("Missing expected exception");
    } catch (SpannerException ex) {
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, ex.getErrorCode());
    }
  }

  @Test
  public void testTimeoutExceptionDdlBatchMultipleStatements() {
    addSlowMockDdlOperations(20);

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setStatementTimeout(TIMEOUT_FOR_SLOW_STATEMENTS, TimeUnit.MILLISECONDS);

      // assert that multiple statements after each other also time out
      for (int i = 0; i < 2; i++) {

        connection.startBatchDdl();
        connection.execute(Statement.of(SLOW_DDL));
        try {
          connection.runBatch();
          fail("Missing expected exception");
        } catch (SpannerException e) {
          assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
        }
      }
      // try to do a new DDL statement that is fast.
      mockDatabaseAdmin.reset();
      addFastMockDdlOperation();
      connection.setStatementTimeout(TIMEOUT_FOR_FAST_STATEMENTS, TimeUnit.MILLISECONDS);
      connection.startBatchDdl();
      assertThat(connection.execute(Statement.of(FAST_DDL)), is(notNullValue()));
      connection.runBatch();
    }
  }

  @Test
  public void testTimeoutDifferentTimeUnits() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(EXECUTION_TIME_SLOW_STATEMENT, 0));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      for (TimeUnit unit : ReadOnlyStalenessUtil.SUPPORTED_UNITS) {
        connection.setStatementTimeout(1L, unit);
        try {
          connection.execute(SELECT_RANDOM_STATEMENT);
          fail("Missing expected exception");
        } catch (SpannerException e) {
          assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
        }
      }
    }
  }
}
