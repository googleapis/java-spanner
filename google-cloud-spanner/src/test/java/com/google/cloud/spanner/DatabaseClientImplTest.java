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

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFuture;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.DatabaseClient.AsyncWork;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.common.base.Stopwatch;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
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
  private static final Statement SELECT1 = Statement.of("SELECT 1 AS COL1");
  private static final ResultSetMetadata SELECT1_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("COL1")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.INT64)
                                  .build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                  .build())
          .setMetadata(SELECT1_METADATA)
          .build();
  private Spanner spanner;
  private Spanner spannerWithEmptySessionPool;

  @Rule public Timeout globalTimeout = new Timeout(5L, TimeUnit.SECONDS);

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));

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
  }

  @Before
  public void setUp() throws IOException {
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
  public void tearDown() throws Exception {
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
    ApiFuture<Long> fut =
        client.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                SettableApiFuture<Long> res = SettableApiFuture.create();
                res.set(txn.executeUpdate(UPDATE_STATEMENT));
                return res;
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
    ApiFuture<Long> fut =
        client.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                SettableApiFuture<Long> res = SettableApiFuture.create();
                res.set(txn.executeUpdate(UPDATE_STATEMENT));
                return res;
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
    ApiFuture<Long> fut =
        client.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                SettableApiFuture<Long> res = SettableApiFuture.create();
                res.set(txn.executeUpdate(INVALID_UPDATE_STATEMENT));
                return res;
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

  /**
   * Test that the update statement can be executed as a partitioned transaction that returns a
   * lower bound update count.
   */
  @Test
  public void testExecutePartitionedDml() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);
    assertThat(updateCount, is(equalTo(UPDATE_COUNT)));
  }

  /** {@link AbortedException} should automatically be retried. */
  @Test
  public void testExecutePartitionedDmlAborted() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    mockSpanner.abortNextTransaction();
    long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);
    assertThat(updateCount, is(equalTo(UPDATE_COUNT)));
  }

  /**
   * A valid query that returns a {@link ResultSet} should not be accepted by a partitioned dml
   * transaction.
   */
  @Test(expected = IllegalArgumentException.class)
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
  public void testPartitionedDmlDoesNotTimeout() throws Exception {
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

      assertThat(
          spanner.getOptions().getPartitionedDmlTimeout(), is(equalTo(Duration.ofHours(2L))));

      // PDML should not timeout with these settings.
      long updateCount = client.executePartitionedUpdate(UPDATE_STATEMENT);
      assertThat(updateCount, is(equalTo(UPDATE_COUNT)));

      // Normal DML should timeout.
      try {
        client
            .readWriteTransaction()
            .run(
                new TransactionCallable<Void>() {
                  @Override
                  public Void run(TransactionContext transaction) throws Exception {
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
  public void testPartitionedDmlWithTimeout() throws Exception {
    mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(1000, 0));
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    // Set PDML timeout value.
    builder.setPartitionedDmlTimeout(Duration.ofMillis(100L));
    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      assertThat(
          spanner.getOptions().getPartitionedDmlTimeout(), is(equalTo(Duration.ofMillis(100L))));
      // PDML should timeout with these settings.
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
                    public Long run(TransactionContext transaction) throws Exception {
                      return transaction.executeUpdate(UPDATE_STATEMENT);
                    }
                  });
      assertThat(updateCount, is(equalTo(UPDATE_COUNT)));
    }
  }

  @Test
  public void testDatabaseOrInstanceDoesNotExistOnPrepareSession() throws Exception {
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
        mockSpanner.setBeginTransactionExecutionTime(
            SimulatedExecutionTime.ofStickyException(exception));
        DatabaseClientImpl dbClient =
            (DatabaseClientImpl)
                spanner.getDatabaseClient(
                    DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
        // Wait until all sessions have been created.
        Stopwatch watch = Stopwatch.createStarted();
        while (watch.elapsed(TimeUnit.SECONDS) < 5
            && dbClient.pool.getNumberOfSessionsBeingCreated() > 0) {
          Thread.sleep(1L);
        }
        // Ensure that no sessions could be prepared and that the session pool gives up trying to
        // prepare sessions.
        watch = watch.reset().start();
        while (watch.elapsed(TimeUnit.SECONDS) < 5
            && dbClient.pool.getNumberOfSessionsBeingPrepared() > 0) {
          Thread.sleep(1L);
        }
        assertThat(dbClient.pool.getNumberOfSessionsBeingPrepared(), is(equalTo(0)));
        assertThat(dbClient.pool.getNumberOfAvailableWritePreparedSessions(), is(equalTo(0)));
        int currentNumRequest = mockSpanner.getRequests().size();
        try {
          dbClient
              .readWriteTransaction()
              .run(
                  new TransactionCallable<Void>() {
                    @Override
                    public Void run(TransactionContext transaction) throws Exception {
                      return null;
                    }
                  });
          fail("missing expected exception");
        } catch (DatabaseNotFoundException | InstanceNotFoundException e) {
        }
        assertThat(mockSpanner.getRequests()).hasSize(currentNumRequest);
        mockSpanner.reset();
        mockSpanner.removeAllExecutionTimes();
      }
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
        assertThat(dbClient.pool.getNumberOfSessionsInPool(), is(equalTo(0)));
        assertThat(dbClient.pool.getNumberOfSessionsBeingCreated(), is(equalTo(0)));
        mockSpanner.reset();
        mockSpanner.removeAllExecutionTimes();
      }
    }
  }

  @Test
  public void testDatabaseOrInstanceDoesNotExistOnCreate() throws Exception {
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
        assertThat(dbClient.pool.getNumberOfSessionsInPool(), is(equalTo(0)));
        assertThat(dbClient.pool.getNumberOfSessionsBeingCreated(), is(equalTo(0)));
        // Force a maintainer run. This should schedule new session creation.
        dbClient.pool.poolMaintainer.maintainPool();
        // Wait until the replenish has finished.
        watch = watch.reset().start();
        while (watch.elapsed(TimeUnit.SECONDS) < 5
            && dbClient.pool.getNumberOfSessionsBeingCreated() > 0) {
          Thread.sleep(1L);
        }
        // All session creation from replenishPool should fail and stop trying.
        assertThat(dbClient.pool.getNumberOfSessionsInPool(), is(equalTo(0)));
        assertThat(dbClient.pool.getNumberOfSessionsBeingCreated(), is(equalTo(0)));
      }
      mockSpanner.reset();
      mockSpanner.removeAllExecutionTimes();
    }
  }

  @Test
  public void testPermissionDeniedOnPrepareSession() throws Exception {
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofStickyException(
            Status.PERMISSION_DENIED
                .withDescription(
                    "Caller is missing IAM permission spanner.databases.beginOrRollbackReadWriteTransaction on resource")
                .asRuntimeException()));
    DatabaseClientImpl dbClient =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    // Wait until all sessions have been created.
    Stopwatch watch = Stopwatch.createStarted();
    while (watch.elapsed(TimeUnit.SECONDS) < 5
        && dbClient.pool.getNumberOfSessionsBeingCreated() > 0) {
      Thread.sleep(1L);
    }
    // Ensure that no sessions could be prepared and that the session pool gives up trying to
    // prepare sessions.
    watch = watch.reset().start();
    while (watch.elapsed(TimeUnit.SECONDS) < 5
        && dbClient.pool.getNumberOfSessionsBeingPrepared() > 0) {
      Thread.sleep(1L);
    }
    assertThat(dbClient.pool.getNumberOfSessionsBeingPrepared(), is(equalTo(0)));
    assertThat(dbClient.pool.getNumberOfAvailableWritePreparedSessions(), is(equalTo(0)));
    try {
      dbClient
          .readWriteTransaction()
          .run(
              new TransactionCallable<Void>() {
                @Override
                public Void run(TransactionContext transaction) throws Exception {
                  return null;
                }
              });
      fail("missing expected PERMISSION_DENIED exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode(), is(equalTo(ErrorCode.PERMISSION_DENIED)));
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
            && (dbClient.pool.getNumberOfSessionsBeingCreated() > 0
                || dbClient.pool.getNumberOfSessionsBeingPrepared() > 0)) {
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
                    public Void run(TransactionContext transaction) throws Exception {
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
                    public Void run(TransactionContext transaction) throws Exception {
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
    assertThat(client.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions)));
    Long res =
        client
            .readWriteTransaction()
            .allowNestedTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    assertThat(
                        client.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions - 1)));
                    return transaction.executeUpdate(UPDATE_STATEMENT);
                  }
                });
    assertThat(res, is(equalTo(UPDATE_COUNT)));
    assertThat(client.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions)));
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
    assertThat(client1.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions)));
    assertThat(client2.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions)));
    Long res =
        client1
            .readWriteTransaction()
            .allowNestedTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    // Client1 should have 1 session checked out.
                    // Client2 should have 0 sessions checked out.
                    assertThat(
                        client1.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions - 1)));
                    assertThat(client2.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions)));
                    Long add =
                        client2
                            .readWriteTransaction()
                            .run(
                                new TransactionCallable<Long>() {
                                  @Override
                                  public Long run(TransactionContext transaction) throws Exception {
                                    // Both clients should now have 1 session checked out.
                                    assertThat(
                                        client1.pool.getNumberOfSessionsInPool(),
                                        is(equalTo(minSessions - 1)));
                                    assertThat(
                                        client2.pool.getNumberOfSessionsInPool(),
                                        is(equalTo(minSessions - 1)));
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
    assertThat(res, is(equalTo(2L)));
    // All sessions should now be checked back in to the pools.
    assertThat(client1.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions)));
    assertThat(client2.pool.getNumberOfSessionsInPool(), is(equalTo(minSessions)));
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

  public void testAsyncQuery() throws InterruptedException {
    final int EXPECTED_ROW_COUNT = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(EXPECTED_ROW_COUNT);
    com.google.spanner.v1.ResultSet resultSet = generator.generate();
    mockSpanner.putStatementResult(
        StatementResult.query(Statement.of("SELECT * FROM RANDOM"), resultSet));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    final CountDownLatch finished = new CountDownLatch(1);
    final List<Struct> receivedResults = new ArrayList<>();
    try (AsyncResultSet rs =
        client.singleUse().executeQueryAsync(Statement.of("SELECT * FROM RANDOM"))) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              while (true) {
                switch (rs.tryNext()) {
                  case DONE:
                    finished.countDown();
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
            }
          });
    }
    finished.await();
    assertThat(receivedResults.size()).isEqualTo(EXPECTED_ROW_COUNT);
  }
}
