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
import static org.junit.Assert.fail;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.NoCredentials;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
  public void setUp() {
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
  }

  @After
  public void tearDown() {
    spanner.close();
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
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
      assertThat(spanner.getOptions().getPartitionedDmlTimeout())
          .isEqualTo(Duration.ofMillis(100L));
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
                    public Long run(TransactionContext transaction) {
                      return transaction.executeUpdate(UPDATE_STATEMENT);
                    }
                  });
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
    }
  }

  @Test
  public void testPartitionedDmlWithHigherTimeout() {
    mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(100, 0));
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
        assertThat(dbClient.pool.getNumberOfSessionsBeingPrepared()).isEqualTo(0);
        assertThat(dbClient.pool.getNumberOfAvailableWritePreparedSessions()).isEqualTo(0);
        int currentNumRequest = mockSpanner.getRequests().size();
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

  @Test
  public void testPermissionDeniedOnPrepareSession() throws Exception {
    testExceptionOnPrepareSession(
        Status.PERMISSION_DENIED
            .withDescription(
                "Caller is missing IAM permission spanner.databases.beginOrRollbackReadWriteTransaction on resource")
            .asRuntimeException());
  }

  @Test
  public void testFailedPreconditionOnPrepareSession() throws Exception {
    testExceptionOnPrepareSession(
        Status.FAILED_PRECONDITION
            .withDescription("FAILED_PRECONDITION: Database is in read-only mode")
            .asRuntimeException());
  }

  private void testExceptionOnPrepareSession(StatusRuntimeException exception)
      throws InterruptedException {
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofStickyException(exception));
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
    assertThat(dbClient.pool.getNumberOfSessionsBeingPrepared()).isEqualTo(0);
    assertThat(dbClient.pool.getNumberOfAvailableWritePreparedSessions()).isEqualTo(0);
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
      fail(String.format("missing expected %s exception", exception.getStatus().getCode().name()));
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.fromGrpcStatus(exception.getStatus()));
    }
    // Remove the semi-permanent error condition. Getting a read/write transaction should now
    // succeed, and the automatic preparing of sessions should be restarted.
    mockSpanner.setBeginTransactionExecutionTime(SimulatedExecutionTime.none());
    dbClient
        .readWriteTransaction()
        .run(
            new TransactionCallable<Void>() {
              @Override
              public Void run(TransactionContext transaction) {
                return null;
              }
            });
    for (int i = 0; i < spanner.getOptions().getSessionPoolOptions().getMinSessions(); i++) {
      dbClient.pool.getReadSession().close();
    }
    int expectedPreparedSessions =
        (int)
            Math.ceil(
                dbClient.pool.getNumberOfSessionsInPool()
                    * spanner.getOptions().getSessionPoolOptions().getWriteSessionsFraction());
    watch = watch.reset().start();
    while (watch.elapsed(TimeUnit.SECONDS) < 5
        && dbClient.pool.getNumberOfAvailableWritePreparedSessions() < expectedPreparedSessions) {
      Thread.sleep(1L);
    }
    assertThat(dbClient.pool.getNumberOfSessionsBeingPrepared()).isEqualTo(0);
    assertThat(dbClient.pool.getNumberOfAvailableWritePreparedSessions())
        .isEqualTo(expectedPreparedSessions);
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
}
