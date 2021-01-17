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

package com.google.cloud.spanner;

import static com.google.cloud.spanner.MockSpannerTestUtil.SELECT1;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AbstractResultSet.GrpcStreamIterator;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.AsyncRunner.AsyncWork;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SpannerOptions.SpannerCallContextTimeoutConfigurator;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.AbstractMessage;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import io.grpc.Context;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class DatabaseClientImplTest {
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private static final String INSTANCE_NAME =
      String.format("projects/%s/instances/%s", TEST_PROJECT, TEST_INSTANCE);
  private static final String DATABASE_NAME =
      String.format(
          "projects/%s/instances/%s/databases/%s", TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE);
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final Statement INVALID_UPDATE_STATEMENT =
      Statement.of("UPDATE NON_EXISTENT_TABLE SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;
  private Spanner spanner;
  private Spanner spannerWithEmptySessionPool;
  private static ExecutorService executor;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT1, MockSpannerTestUtil.SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));

    executor = Executors.newSingleThreadExecutor();
    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            // We need to use a real executor for timeouts to occur.
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
    executor.shutdown();
  }

  @Before
  public void setUp() {
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
                SessionPoolOptions.newBuilder().setMinSessions(0).setFailOnSessionLeak().build())
            .build()
            .getService();
  }

  @After
  public void tearDown() {
    mockSpanner.unfreeze();
    spanner.close();
    spannerWithEmptySessionPool.close();
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
  }

  @Test
  public void write() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.write(
        Arrays.asList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
  }

  @Test
  public void writeAtLeastOnce() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.writeAtLeastOnce(
        Arrays.asList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
  }

  @Test
  public void singleUse() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void singleUseIsNonBlocking() {
    mockSpanner.freeze();
    // Use a Spanner instance with no initial sessions in the pool to show that getting a session
    // from the pool and then preparing a query is non-blocking (i.e. does not wait on a reply from
    // the server).
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
      mockSpanner.unfreeze();
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void singleUseAsync() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    final AtomicInteger rowCount = new AtomicInteger();
    ApiFuture<Void> res;
    try (AsyncResultSet rs = client.singleUse().executeQueryAsync(SELECT1)) {
      res =
          rs.setCallback(
              executor,
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
    }
    res.get();
    assertThat(rowCount.get()).isEqualTo(1);
  }

  @Test
  public void singleUseAsyncWithoutCallback() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    int rowCount = 0;
    try (AsyncResultSet rs = client.singleUse().executeQueryAsync(SELECT1)) {
      while (rs.next()) {
        rowCount++;
      }
    }
    assertThat(rowCount).isEqualTo(1);
  }

  @Test
  public void singleUseBound() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs =
        client
            .singleUse(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))
            .executeQuery(SELECT1)) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void singleUseBoundIsNonBlocking() {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs =
        client
            .singleUse(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))
            .executeQuery(SELECT1)) {
      mockSpanner.unfreeze();
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void singleUseBoundAsync() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    final AtomicInteger rowCount = new AtomicInteger();
    ApiFuture<Void> res;
    try (AsyncResultSet rs =
        client
            .singleUse(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))
            .executeQueryAsync(SELECT1)) {
      res =
          rs.setCallback(
              executor,
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
    }
    res.get();
    assertThat(rowCount.get()).isEqualTo(1);
  }

  @Test
  public void singleUseTransaction() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs = client.singleUseReadOnlyTransaction().executeQuery(SELECT1)) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void singleUseTransactionIsNonBlocking() {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs = client.singleUseReadOnlyTransaction().executeQuery(SELECT1)) {
      mockSpanner.unfreeze();
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void singleUseTransactionBound() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs =
        client
            .singleUseReadOnlyTransaction(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))
            .executeQuery(SELECT1)) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void singleUseTransactionBoundIsNonBlocking() {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs =
        client
            .singleUseReadOnlyTransaction(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))
            .executeQuery(SELECT1)) {
      mockSpanner.unfreeze();
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void readOnlyTransaction() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
      try (ResultSet rs = tx.executeQuery(SELECT1)) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(0)).isEqualTo(1L);
        assertThat(rs.next()).isFalse();
      }
    }
  }

  @Test
  public void readOnlyTransactionIsNonBlocking() {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
      try (ResultSet rs = tx.executeQuery(SELECT1)) {
        mockSpanner.unfreeze();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(0)).isEqualTo(1L);
        assertThat(rs.next()).isFalse();
      }
    }
  }

  @Test
  public void readOnlyTransactionBound() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ReadOnlyTransaction tx =
        client.readOnlyTransaction(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))) {
      try (ResultSet rs = tx.executeQuery(SELECT1)) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(0)).isEqualTo(1L);
        assertThat(rs.next()).isFalse();
      }
    }
  }

  @Test
  public void readOnlyTransactionBoundIsNonBlocking() {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ReadOnlyTransaction tx =
        client.readOnlyTransaction(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))) {
      try (ResultSet rs = tx.executeQuery(SELECT1)) {
        mockSpanner.unfreeze();
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(0)).isEqualTo(1L);
        assertThat(rs.next()).isFalse();
      }
    }
  }

  @Test
  public void readWriteTransaction() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        new TransactionCallable<Void>() {
          @Override
          public Void run(TransactionContext transaction) throws Exception {
            transaction.executeUpdate(UPDATE_STATEMENT);
            return null;
          }
        });
  }

  @Test
  public void readWriteTransactionIsNonBlocking() {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    // The runner.run(...) method cannot be made non-blocking, as it returns the result of the
    // transaction.
    mockSpanner.unfreeze();
    runner.run(
        new TransactionCallable<Void>() {
          @Override
          public Void run(TransactionContext transaction) throws Exception {
            transaction.executeUpdate(UPDATE_STATEMENT);
            return null;
          }
        });
  }

  @Test
  public void runAsync() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AsyncRunner runner = client.runAsync();
    ApiFuture<Long> fut =
        runner.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                return ApiFutures.immediateFuture(txn.executeUpdate(UPDATE_STATEMENT));
              }
            },
            executor);
    assertThat(fut.get()).isEqualTo(UPDATE_COUNT);
    executor.shutdown();
  }

  @Test
  public void runAsyncIsNonBlocking() throws Exception {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AsyncRunner runner = client.runAsync();
    ApiFuture<Long> fut =
        runner.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                return ApiFutures.immediateFuture(txn.executeUpdate(UPDATE_STATEMENT));
              }
            },
            executor);
    mockSpanner.unfreeze();
    assertThat(fut.get()).isEqualTo(UPDATE_COUNT);
    executor.shutdown();
  }

  @Test
  public void runAsyncWithException() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AsyncRunner runner = client.runAsync();
    ApiFuture<Long> fut =
        runner.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                return ApiFutures.immediateFuture(txn.executeUpdate(INVALID_UPDATE_STATEMENT));
              }
            },
            executor);
    try {
      fut.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
    executor.shutdown();
  }

  @Test
  public void transactionManager() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (TransactionManager txManager = client.transactionManager()) {
      while (true) {
        TransactionContext tx = txManager.begin();
        try {
          tx.executeUpdate(UPDATE_STATEMENT);
          txManager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis() / 1000);
          tx = txManager.resetForRetry();
        }
      }
    }
  }

  @Test
  public void transactionManagerIsNonBlocking() throws Exception {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (TransactionManager txManager = client.transactionManager()) {
      while (true) {
        mockSpanner.unfreeze();
        TransactionContext tx = txManager.begin();
        try {
          tx.executeUpdate(UPDATE_STATEMENT);
          txManager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis() / 1000);
          tx = txManager.resetForRetry();
        }
      }
    }
  }

  @Test
  public void transactionManagerExecuteQueryAsync() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    final AtomicInteger rowCount = new AtomicInteger();
    try (TransactionManager txManager = client.transactionManager()) {
      while (true) {
        TransactionContext tx = txManager.begin();
        try {
          try (AsyncResultSet rs = tx.executeQueryAsync(SELECT1)) {
            rs.setCallback(
                executor,
                new ReadyCallback() {
                  @Override
                  public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                    try {
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
                    } catch (Throwable t) {
                      return CallbackResponse.DONE;
                    }
                  }
                });
          }
          txManager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis() / 1000);
          tx = txManager.resetForRetry();
        }
      }
    }
    assertThat(rowCount.get()).isEqualTo(1);
  }

  /**
   * Test that the update statement can be executed as a partitioned transaction that returns a
   * lower bound update count.
   */
  @Test
  public void testExecutePartitionedDml() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);
    assertThat(updateCount).isEqualTo(UPDATE_COUNT);
  }

  /** {@link AbortedException} should automatically be retried. */
  @Test
  public void testExecutePartitionedDmlAborted() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    mockSpanner.abortNextTransaction();
    long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);
    assertThat(updateCount).isEqualTo(UPDATE_COUNT);
  }

  /**
   * A valid query that returns a {@link ResultSet} should not be accepted by a partitioned dml
   * transaction.
   */
  @Test(expected = SpannerException.class)
  public void testExecutePartitionedDmlWithQuery() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.executePartitionedUpdate(SELECT1);
  }

  /** Server side exceptions that are not {@link AbortedException}s should propagate to the user. */
  @Test(expected = SpannerException.class)
  public void testExecutePartitionedDmlWithException() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.executePartitionedUpdate(INVALID_UPDATE_STATEMENT);
  }

  @Test
  public void testPartitionedDmlDoesNotTimeout() {
    mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(10, 0));
    final RetrySettings retrySettings =
        RetrySettings.newBuilder()
            .setInitialRpcTimeout(Duration.ofMillis(1L))
            .setMaxRpcTimeout(Duration.ofMillis(1L))
            .setMaxAttempts(1)
            .setTotalTimeout(Duration.ofMillis(1L))
            .build();
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    // Set normal DML timeout value.
    builder.getSpannerStubSettingsBuilder().executeSqlSettings().setRetrySettings(retrySettings);
    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));

      assertThat(spanner.getOptions().getPartitionedDmlTimeout()).isEqualTo(Duration.ofHours(2L));

      // PDML should not timeout with these settings.
      long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);

      // Normal DML should timeout.
      try {
        client
            .readWriteTransaction()
            .run(
                new TransactionCallable<Void>() {
                  @Override
                  public Void run(TransactionContext transaction) {
                    transaction.executeUpdate(UPDATE_STATEMENT);
                    return null;
                  }
                });
        fail("expected DEADLINE_EXCEEDED");
      } catch (SpannerException e) {
        if (e.getErrorCode() != ErrorCode.DEADLINE_EXCEEDED) {
          fail("expected DEADLINE_EXCEEDED");
        }
      }
    }
  }

  @Test
  public void testPartitionedDmlWithLowerTimeout() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(1000, 0));
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    // Set PDML timeout value.
    builder.setPartitionedDmlTimeout(Duration.ofMillis(10L));
    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      assertThat(spanner.getOptions().getPartitionedDmlTimeout()).isEqualTo(Duration.ofMillis(10L));
      // PDML should timeout with these settings.
      mockSpanner.setExecuteSqlExecutionTime(
          SimulatedExecutionTime.ofMinimumAndRandomTime(1000, 0));
      try {
        client.executePartitionedUpdate(UPDATE_STATEMENT);
        fail("expected DEADLINE_EXCEEDED");
      } catch (SpannerException e) {
        if (e.getErrorCode() != ErrorCode.DEADLINE_EXCEEDED) {
          fail("expected DEADLINE_EXCEEDED");
        }
      }

      // Normal DML should not timeout.
      mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(10, 0));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  new TransactionCallable<Long>() {
                    @Override
                    public Long run(TransactionContext transaction) {
                      return transaction.executeUpdate(UPDATE_STATEMENT);
                    }
                  });
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
    }
  }

  @Test
  public void testPartitionedDmlWithHigherTimeout() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(100, 0));
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    // Set PDML timeout value to a value that should allow the statement to be executed.
    builder.setPartitionedDmlTimeout(Duration.ofMillis(5000L));
    // Set the ExecuteSql RPC timeout value to a value lower than the time needed to execute the
    // statement. The higher timeout value that is set above should be respected, and the value for
    // the ExecuteSQL RPC should be ignored specifically for Partitioned DML.
    builder
        .getSpannerStubSettingsBuilder()
        .executeSqlSettings()
        .setRetrySettings(
            builder
                .getSpannerStubSettingsBuilder()
                .executeSqlSettings()
                .getRetrySettings()
                .toBuilder()
                .setInitialRpcTimeout(Duration.ofMillis(10L))
                .setMaxRpcTimeout(Duration.ofMillis(10L))
                .setInitialRetryDelay(Duration.ofMillis(1L))
                .setMaxRetryDelay(Duration.ofMillis(1L))
                .build());
    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      // PDML should not timeout with these settings.
      long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);

      // Normal DML should timeout as it should use the ExecuteSQL RPC settings.
      mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(100, 0));
      try {
        client
            .readWriteTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) {
                    return transaction.executeUpdate(UPDATE_STATEMENT);
                  }
                });
        fail("missing expected DEADLINE_EXCEEDED exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
      }
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
    }
  }

  @Test
  public void testPartitionedDmlRetriesOnUnavailable() {
    mockSpanner.setExecuteSqlExecutionTime(
        SimulatedExecutionTime.ofException(Status.UNAVAILABLE.asRuntimeException()));
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
    }
  }

  @Test
  public void testDatabaseOrInstanceDoesNotExistOnInitialization() throws Exception {
    StatusRuntimeException[] exceptions =
        new StatusRuntimeException[] {
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Database", SpannerExceptionFactory.DATABASE_RESOURCE_TYPE, DATABASE_NAME),
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Instance", SpannerExceptionFactory.INSTANCE_RESOURCE_TYPE, INSTANCE_NAME)
        };
    for (StatusRuntimeException exception : exceptions) {
      try (Spanner spanner =
          SpannerOptions.newBuilder()
              .setProjectId(TEST_PROJECT)
              .setChannelProvider(channelProvider)
              .setCredentials(NoCredentials.getInstance())
              .build()
              .getService()) {
        mockSpanner.setBatchCreateSessionsExecutionTime(
            SimulatedExecutionTime.ofStickyException(exception));
        DatabaseClientImpl dbClient =
            (DatabaseClientImpl)
                spanner.getDatabaseClient(
                    DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
        // Wait until session creation has finished.
        Stopwatch watch = Stopwatch.createStarted();
        while (watch.elapsed(TimeUnit.SECONDS) < 5
            && dbClient.pool.getNumberOfSessionsBeingCreated() > 0) {
          Thread.sleep(1L);
        }
        // All session creation should fail and stop trying.
        assertThat(dbClient.pool.getNumberOfSessionsInPool()).isEqualTo(0);
        assertThat(dbClient.pool.getNumberOfSessionsBeingCreated()).isEqualTo(0);
        mockSpanner.reset();
        mockSpanner.removeAllExecutionTimes();
      }
    }
  }

  @Test
  public void testDatabaseOrInstanceDoesNotExistOnCreate() {
    StatusRuntimeException[] exceptions =
        new StatusRuntimeException[] {
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Database", SpannerExceptionFactory.DATABASE_RESOURCE_TYPE, DATABASE_NAME),
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Instance", SpannerExceptionFactory.INSTANCE_RESOURCE_TYPE, INSTANCE_NAME)
        };
    for (StatusRuntimeException exception : exceptions) {
      mockSpanner.setBatchCreateSessionsExecutionTime(
          SimulatedExecutionTime.ofStickyException(exception));
      // Ensure there are no sessions in the pool by default.
      try (Spanner spanner =
          SpannerOptions.newBuilder()
              .setProjectId(TEST_PROJECT)
              .setChannelProvider(channelProvider)
              .setCredentials(NoCredentials.getInstance())
              .setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build())
              .build()
              .getService()) {
        DatabaseClientImpl dbClient =
            (DatabaseClientImpl)
                spanner.getDatabaseClient(
                    DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
        // The create session failure should propagate to the client and not retry.
        try (ResultSet rs = dbClient.singleUse().executeQuery(SELECT1)) {
          rs.next();
          fail("missing expected exception");
        } catch (DatabaseNotFoundException | InstanceNotFoundException e) {
          // The server should only receive one BatchCreateSessions request.
          assertThat(mockSpanner.getRequests()).hasSize(1);
        }
        try {
          dbClient.readWriteTransaction();
          fail("missing expected exception");
        } catch (DatabaseNotFoundException | InstanceNotFoundException e) {
          // No additional requests should have been sent by the client.
          assertThat(mockSpanner.getRequests()).hasSize(1);
        }
      }
      mockSpanner.reset();
      mockSpanner.removeAllExecutionTimes();
    }
  }

  @Test
  public void testDatabaseOrInstanceDoesNotExistOnReplenish() throws Exception {
    StatusRuntimeException[] exceptions =
        new StatusRuntimeException[] {
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Database", SpannerExceptionFactory.DATABASE_RESOURCE_TYPE, DATABASE_NAME),
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Instance", SpannerExceptionFactory.INSTANCE_RESOURCE_TYPE, INSTANCE_NAME)
        };
    for (StatusRuntimeException exception : exceptions) {
      try (Spanner spanner =
          SpannerOptions.newBuilder()
              .setProjectId(TEST_PROJECT)
              .setChannelProvider(channelProvider)
              .setCredentials(NoCredentials.getInstance())
              .build()
              .getService()) {
        mockSpanner.setBatchCreateSessionsExecutionTime(
            SimulatedExecutionTime.ofStickyException(exception));
        DatabaseClientImpl dbClient =
            (DatabaseClientImpl)
                spanner.getDatabaseClient(
                    DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
        // Wait until session creation has finished.
        Stopwatch watch = Stopwatch.createStarted();
        while (watch.elapsed(TimeUnit.SECONDS) < 5
            && dbClient.pool.getNumberOfSessionsBeingCreated() > 0) {
          Thread.sleep(1L);
        }
        // All session creation should fail and stop trying.
        assertThat(dbClient.pool.getNumberOfSessionsInPool()).isEqualTo(0);
        assertThat(dbClient.pool.getNumberOfSessionsBeingCreated()).isEqualTo(0);
        // Force a maintainer run. This should schedule new session creation.
        dbClient.pool.poolMaintainer.maintainPool();
        // Wait until the replenish has finished.
        watch = watch.reset().start();
        while (watch.elapsed(TimeUnit.SECONDS) < 5
            && dbClient.pool.getNumberOfSessionsBeingCreated() > 0) {
          Thread.sleep(1L);
        }
        // All session creation from replenishPool should fail and stop trying.
        assertThat(dbClient.pool.getNumberOfSessionsInPool()).isEqualTo(0);
        assertThat(dbClient.pool.getNumberOfSessionsBeingCreated()).isEqualTo(0);
      }
      mockSpanner.reset();
      mockSpanner.removeAllExecutionTimes();
    }
  }

  /**
   * Test showing that when a database is deleted while it is in use by a database client and then
   * re-created with the same name, will continue to return {@link DatabaseNotFoundException}s until
   * a new {@link DatabaseClient} is created.
   */
  @Test
  public void testDatabaseOrInstanceIsDeletedAndThenRecreated() throws Exception {
    StatusRuntimeException[] exceptions =
        new StatusRuntimeException[] {
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Database", SpannerExceptionFactory.DATABASE_RESOURCE_TYPE, DATABASE_NAME),
          SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
              "Instance", SpannerExceptionFactory.INSTANCE_RESOURCE_TYPE, INSTANCE_NAME)
        };
    for (StatusRuntimeException exception : exceptions) {
      try (Spanner spanner =
          SpannerOptions.newBuilder()
              .setProjectId(TEST_PROJECT)
              .setChannelProvider(channelProvider)
              .setCredentials(NoCredentials.getInstance())
              .build()
              .getService()) {
        DatabaseClientImpl dbClient =
            (DatabaseClientImpl)
                spanner.getDatabaseClient(
                    DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
        // Wait until all sessions have been created and prepared.
        Stopwatch watch = Stopwatch.createStarted();
        while (watch.elapsed(TimeUnit.SECONDS) < 5
            && (dbClient.pool.getNumberOfSessionsBeingCreated() > 0)) {
          Thread.sleep(1L);
        }
        // Simulate that the database or instance has been deleted.
        mockSpanner.setStickyGlobalExceptions(true);
        mockSpanner.addException(exception);

        // All subsequent calls should fail with a DatabaseNotFoundException.
        try (ResultSet rs = dbClient.singleUse().executeQuery(SELECT1)) {
          while (rs.next()) {}
          fail("missing expected exception");
        } catch (DatabaseNotFoundException | InstanceNotFoundException e) {
        }
        try {
          dbClient
              .readWriteTransaction()
              .run(
                  new TransactionCallable<Void>() {
                    @Override
                    public Void run(TransactionContext transaction) {
                      return null;
                    }
                  });
          fail("missing expected exception");
        } catch (DatabaseNotFoundException | InstanceNotFoundException e) {
        }

        // Now simulate that the database has been re-created. The database client should still
        // throw
        // DatabaseNotFoundExceptions, as it is not the same database. The server should not receive
        // any new requests.
        mockSpanner.reset();
        // All subsequent calls should fail with a DatabaseNotFoundException.
        try (ResultSet rs = dbClient.singleUse().executeQuery(SELECT1)) {
          while (rs.next()) {}
          fail("missing expected exception");
        } catch (DatabaseNotFoundException | InstanceNotFoundException e) {
        }
        try {
          dbClient
              .readWriteTransaction()
              .run(
                  new TransactionCallable<Void>() {
                    @Override
                    public Void run(TransactionContext transaction) {
                      return null;
                    }
                  });
          fail("missing expected exception");
        } catch (DatabaseNotFoundException | InstanceNotFoundException e) {
        }
        assertThat(mockSpanner.getRequests()).isEmpty();
        // Now get a new database client. Normally multiple calls to Spanner#getDatabaseClient will
        // return the same instance, but not when the instance has been invalidated by a
        // DatabaseNotFoundException.
        DatabaseClientImpl newClient =
            (DatabaseClientImpl)
                spanner.getDatabaseClient(
                    DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
        assertThat(newClient).isNotSameInstanceAs(dbClient);
        // Executing a query should now work without problems.
        try (ResultSet rs = newClient.singleUse().executeQuery(SELECT1)) {
          while (rs.next()) {}
        }
        assertThat(mockSpanner.getRequests()).isNotEmpty();
      }
      mockSpanner.reset();
      mockSpanner.removeAllExecutionTimes();
    }
  }

  @Test
  public void testAllowNestedTransactions() throws InterruptedException {
    final DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    // Wait until all sessions have been created.
    final int minSessions = spanner.getOptions().getSessionPoolOptions().getMinSessions();
    Stopwatch watch = Stopwatch.createStarted();
    while (watch.elapsed(TimeUnit.SECONDS) < 5
        && client.pool.getNumberOfSessionsInPool() < minSessions) {
      Thread.sleep(1L);
    }
    assertThat(client.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
    Long res =
        client
            .readWriteTransaction()
            .allowNestedTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) {
                    assertThat(client.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions - 1);
                    return transaction.executeUpdate(UPDATE_STATEMENT);
                  }
                });
    assertThat(res).isEqualTo(UPDATE_COUNT);
    assertThat(client.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
  }

  @Test
  public void testNestedTransactionsUsingTwoDatabases() throws InterruptedException {
    final DatabaseClientImpl client1 =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, "my-database-1"));
    final DatabaseClientImpl client2 =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, "my-database-2"));
    // Wait until all sessions have been created so we can actually check the number of sessions
    // checked out of the pools.
    final int minSessions = spanner.getOptions().getSessionPoolOptions().getMinSessions();
    Stopwatch watch = Stopwatch.createStarted();
    while (watch.elapsed(TimeUnit.SECONDS) < 5
        && (client1.pool.getNumberOfSessionsInPool() < minSessions
            || client2.pool.getNumberOfSessionsInPool() < minSessions)) {
      Thread.sleep(1L);
    }
    assertThat(client1.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
    assertThat(client2.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
    Long res =
        client1
            .readWriteTransaction()
            .allowNestedTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) {
                    // Client1 should have 1 session checked out.
                    // Client2 should have 0 sessions checked out.
                    assertThat(client1.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions - 1);
                    assertThat(client2.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
                    Long add =
                        client2
                            .readWriteTransaction()
                            .run(
                                new TransactionCallable<Long>() {
                                  @Override
                                  public Long run(TransactionContext transaction) {
                                    // Both clients should now have 1 session checked out.
                                    assertThat(client1.pool.getNumberOfSessionsInPool())
                                        .isEqualTo(minSessions - 1);
                                    assertThat(client2.pool.getNumberOfSessionsInPool())
                                        .isEqualTo(minSessions - 1);
                                    try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                                      if (rs.next()) {
                                        return rs.getLong(0);
                                      }
                                      return 0L;
                                    }
                                  }
                                });
                    try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                      if (rs.next()) {
                        return add + rs.getLong(0);
                      }
                      return add + 0L;
                    }
                  }
                });
    assertThat(res).isEqualTo(2L);
    // All sessions should now be checked back in to the pools.
    assertThat(client1.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
    assertThat(client2.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
  }

  @Test
  public void testBackendQueryOptions() {
    // Use a Spanner instance with MinSession=0 and WriteFraction=0.0 to prevent background requests
    // from the session pool interfering with the test case.
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setMinSessions(0)
                    .setWriteSessionsFraction(0.0f)
                    .build())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE"));
      try (ResultSet rs =
          client
              .singleUse()
              .executeQuery(
                  Statement.newBuilder(SELECT1.getSql())
                      .withQueryOptions(QueryOptions.newBuilder().setOptimizerVersion("1").build())
                      .build())) {
        // Just iterate over the results to execute the query.
        while (rs.next()) {}
      }
      // Check that the last query was executed using a custom optimizer version.
      List<AbstractMessage> requests = mockSpanner.getRequests();
      assertThat(requests).isNotEmpty();
      assertThat(requests.get(requests.size() - 1)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest request = (ExecuteSqlRequest) requests.get(requests.size() - 1);
      assertThat(request.getQueryOptions()).isNotNull();
      assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("1");
    }
  }

  @Test
  public void testBackendQueryOptionsWithAnalyzeQuery() {
    // Use a Spanner instance with MinSession=0 and WriteFraction=0.0 to prevent background requests
    // from the session pool interfering with the test case.
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setMinSessions(0)
                    .setWriteSessionsFraction(0.0f)
                    .build())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE"));
      try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
        try (ResultSet rs =
            tx.analyzeQuery(
                Statement.newBuilder(SELECT1.getSql())
                    .withQueryOptions(QueryOptions.newBuilder().setOptimizerVersion("1").build())
                    .build(),
                QueryAnalyzeMode.PROFILE)) {
          // Just iterate over the results to execute the query.
          while (rs.next()) {}
        }
      }
      // Check that the last query was executed using a custom optimizer version.
      List<AbstractMessage> requests = mockSpanner.getRequests();
      assertThat(requests).isNotEmpty();
      assertThat(requests.get(requests.size() - 1)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest request = (ExecuteSqlRequest) requests.get(requests.size() - 1);
      assertThat(request.getQueryOptions()).isNotNull();
      assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("1");
      assertThat(request.getQueryMode()).isEqualTo(QueryMode.PROFILE);
    }
  }

  @Test
  public void testBackendPartitionQueryOptions() {
    // Use a Spanner instance with MinSession=0 and WriteFraction=0.0 to prevent background requests
    // from the session pool interfering with the test case.
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setMinSessions(0)
                    .setWriteSessionsFraction(0.0f)
                    .build())
            .build()
            .getService()) {
      BatchClient client =
          spanner.getBatchClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE"));
      BatchReadOnlyTransaction transaction =
          client.batchReadOnlyTransaction(TimestampBound.strong());
      List<Partition> partitions =
          transaction.partitionQuery(
              PartitionOptions.newBuilder().setMaxPartitions(10L).build(),
              Statement.newBuilder(SELECT1.getSql())
                  .withQueryOptions(QueryOptions.newBuilder().setOptimizerVersion("1").build())
                  .build());
      try (ResultSet rs = transaction.execute(partitions.get(0))) {
        // Just iterate over the results to execute the query.
        while (rs.next()) {}
      }
      // Check that the last query was executed using a custom optimizer version.
      List<AbstractMessage> requests = mockSpanner.getRequests();
      assertThat(requests).isNotEmpty();
      assertThat(requests.get(requests.size() - 1)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest request = (ExecuteSqlRequest) requests.get(requests.size() - 1);
      assertThat(request.getQueryOptions()).isNotNull();
      assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("1");
    }
  }

  @Test
  public void testAsyncQuery() throws Exception {
    final int EXPECTED_ROW_COUNT = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(EXPECTED_ROW_COUNT);
    com.google.spanner.v1.ResultSet resultSet = generator.generate();
    mockSpanner.putStatementResult(
        StatementResult.query(Statement.of("SELECT * FROM RANDOM"), resultSet));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    ApiFuture<Void> resultSetClosed;
    final SettableFuture<Boolean> finished = SettableFuture.create();
    final List<Struct> receivedResults = new ArrayList<>();
    try (AsyncResultSet rs =
        client.singleUse().executeQueryAsync(Statement.of("SELECT * FROM RANDOM"))) {
      resultSetClosed =
          rs.setCallback(
              executor,
              new ReadyCallback() {
                @Override
                public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                  try {
                    while (true) {
                      switch (rs.tryNext()) {
                        case DONE:
                          finished.set(true);
                          return CallbackResponse.DONE;
                        case NOT_READY:
                          return CallbackResponse.CONTINUE;
                        case OK:
                          receivedResults.add(resultSet.getCurrentRowAsStruct());
                          break;
                        default:
                          throw new IllegalStateException("Unknown cursor state");
                      }
                    }
                  } catch (Throwable t) {
                    finished.setException(t);
                    return CallbackResponse.DONE;
                  }
                }
              });
    }
    assertThat(finished.get()).isTrue();
    assertThat(receivedResults.size()).isEqualTo(EXPECTED_ROW_COUNT);
    resultSetClosed.get();
  }

  @Test
  public void testClientIdReusedOnDatabaseNotFound() {
    mockSpanner.setBatchCreateSessionsExecutionTime(
        SimulatedExecutionTime.ofStickyException(
            SpannerExceptionFactoryTest.newStatusResourceNotFoundException(
                "my-database",
                SpannerExceptionFactory.DATABASE_RESOURCE_TYPE,
                "project/my-project/instances/my-instance/databases/my-database")));
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("my-project")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseId databaseId = DatabaseId.of("my-project", "my-instance", "my-database");
      String prevClientId = null;
      for (int i = 0; i < 100; i++) {
        try {
          DatabaseClientImpl client = (DatabaseClientImpl) spanner.getDatabaseClient(databaseId);
          if (prevClientId != null) {
            assertThat(client.clientId).isEqualTo(prevClientId);
          }
          prevClientId = client.clientId;
          client.singleUse().readRow("MyTable", Key.of(0), Arrays.asList("MyColumn"));
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

  @Test
  public void testBatchCreateSessionsPermissionDenied() {
    mockSpanner.setBatchCreateSessionsExecutionTime(
        SimulatedExecutionTime.ofStickyException(
            Status.PERMISSION_DENIED.withDescription("Not permitted").asRuntimeException()));
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("my-project")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseId databaseId = DatabaseId.of("my-project", "my-instance", "my-database");
      DatabaseClient client = spanner.getDatabaseClient(databaseId);
      // The following call is non-blocking and will not generate an exception.
      ResultSet rs = client.singleUse().executeQuery(SELECT1);
      try {
        // Actually trying to get any results will cause an exception.
        rs.next();
        fail("missing PERMISSION_DENIED exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
      }
    }
  }

  @Test
  public void testExceptionIncludesStatement() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.INVALID_ARGUMENT.withDescription("Invalid query").asRuntimeException()));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs =
        client
            .singleUse()
            .executeQuery(
                Statement.newBuilder("SELECT * FROM FOO WHERE ID=@id").bind("id").to(1L).build())) {
      rs.next();
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(e.getMessage()).contains("Statement: 'SELECT * FROM FOO WHERE ID=@id'");
      // The error message should normally not include the parameter values to prevent sensitive
      // information from accidentally being logged.
      assertThat(e.getMessage()).doesNotContain("id: 1");
    }

    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.INVALID_ARGUMENT.withDescription("Invalid query").asRuntimeException()));
    Logger logger = Logger.getLogger(GrpcStreamIterator.class.getName());
    Level currentLevel = logger.getLevel();
    try (ResultSet rs =
        client
            .singleUse()
            .executeQuery(
                Statement.newBuilder("SELECT * FROM FOO WHERE ID=@id").bind("id").to(1L).build())) {
      logger.setLevel(Level.FINEST);
      rs.next();
      fail("missing expected exception");
    } catch (SpannerException e) {
      // With log level set to FINEST the error should also include the parameter values.
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(e.getMessage()).contains("Statement: 'SELECT * FROM FOO WHERE ID=@id {id: 1}'");
    } finally {
      logger.setLevel(currentLevel);
    }
  }

  @Test
  public void testReadDoesNotIncludeStatement() {
    mockSpanner.setStreamingReadExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.INVALID_ARGUMENT.withDescription("Invalid read").asRuntimeException()));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet rs =
        client.singleUse().read("FOO", KeySet.singleKey(Key.of(1L)), ImmutableList.of("BAR"))) {
      rs.next();
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(e.getMessage()).doesNotContain("Statement:");
    }
  }

  @Test
  public void testSpecificTimeout() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(10000, 0));
    final DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Context.current()
        .withValue(
            SpannerOptions.CALL_CONTEXT_CONFIGURATOR_KEY,
            SpannerCallContextTimeoutConfigurator.create()
                .withExecuteQueryTimeout(Duration.ofNanos(1L)))
        .run(
            new Runnable() {
              @Override
              public void run() {
                // Query should fail with a timeout.
                try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
                  rs.next();
                  fail("missing expected DEADLINE_EXCEEDED exception");
                } catch (SpannerException e) {
                  assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
                }
                // Update should succeed.
                client
                    .readWriteTransaction()
                    .run(
                        new TransactionCallable<Long>() {
                          @Override
                          public Long run(TransactionContext transaction) throws Exception {
                            return transaction.executeUpdate(UPDATE_STATEMENT);
                          }
                        });
              }
            });
  }

  @Test
  public void testBatchCreateSessionsFailure_shouldNotPropagateToCloseMethod() {
    try {
      // Simulate session creation failures on the backend.
      mockSpanner.setBatchCreateSessionsExecutionTime(
          SimulatedExecutionTime.ofStickyException(Status.RESOURCE_EXHAUSTED.asRuntimeException()));
      DatabaseClient client =
          spannerWithEmptySessionPool.getDatabaseClient(
              DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      // This will not cause any failure as getting a session from the pool is guaranteed to be
      // non-blocking, and any exceptions will be delayed until actual query execution.
      ResultSet rs = client.singleUse().executeQuery(SELECT1);
      try {
        while (rs.next()) {
          fail("Missing expected exception");
        }
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
      } finally {
        // This should not cause any failures.
        rs.close();
      }
    } finally {
      mockSpanner.setBatchCreateSessionsExecutionTime(SimulatedExecutionTime.none());
    }
  }

  @Test
  public void testReadWriteTransaction_usesOptions() {
    SessionPool pool = mock(SessionPool.class);
    PooledSessionFuture session = mock(PooledSessionFuture.class);
    when(pool.getSession()).thenReturn(session);
    TransactionOption option = mock(TransactionOption.class);

    DatabaseClientImpl client = new DatabaseClientImpl(pool);
    client.readWriteTransaction(option);

    verify(session).readWriteTransaction(option);
  }

  @Test
  public void testTransactionManager_usesOptions() {
    SessionPool pool = mock(SessionPool.class);
    PooledSessionFuture session = mock(PooledSessionFuture.class);
    when(pool.getSession()).thenReturn(session);
    TransactionOption option = mock(TransactionOption.class);

    DatabaseClientImpl client = new DatabaseClientImpl(pool);
    client.transactionManager(option);

    verify(session).transactionManager(option);
  }

  @Test
  public void testRunAsync_usesOptions() {
    SessionPool pool = mock(SessionPool.class);
    PooledSessionFuture session = mock(PooledSessionFuture.class);
    when(pool.getSession()).thenReturn(session);
    TransactionOption option = mock(TransactionOption.class);

    DatabaseClientImpl client = new DatabaseClientImpl(pool);
    client.runAsync(option);

    verify(session).runAsync(option);
  }

  @Test
  public void testTransactionManagerAsync_usesOptions() {
    SessionPool pool = mock(SessionPool.class);
    PooledSessionFuture session = mock(PooledSessionFuture.class);
    when(pool.getSession()).thenReturn(session);
    TransactionOption option = mock(TransactionOption.class);

    DatabaseClientImpl client = new DatabaseClientImpl(pool);
    client.transactionManagerAsync(option);

    verify(session).transactionManagerAsync(option);
  }
}
