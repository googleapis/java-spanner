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

import static com.google.cloud.spanner.MockSpannerTestUtil.READ_COLUMN_NAMES;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_ONE_KEY_VALUE_RESULTSET;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_ONE_KEY_VALUE_STATEMENT;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_TABLE_NAME;
import static com.google.cloud.spanner.MockSpannerTestUtil.SELECT1;
import static com.google.cloud.spanner.MockSpannerTestUtil.SELECT1_RESULTSET;
import static com.google.cloud.spanner.SpannerApiFutures.get;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.ByteArray;
import com.google.cloud.NoCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbstractResultSet.GrpcStreamIterator;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPoolOptions.ActionOnInactiveTransaction;
import com.google.cloud.spanner.SessionPoolOptions.InactiveTransactionRemovalOptions;
import com.google.cloud.spanner.SpannerException.ResourceNotFoundException;
import com.google.cloud.spanner.SpannerOptions.CallContextConfigurator;
import com.google.cloud.spanner.SpannerOptions.SpannerCallContextTimeoutConfigurator;
import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.rpc.RetryInfo;
import com.google.spanner.v1.BatchWriteRequest;
import com.google.spanner.v1.BatchWriteResponse;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.DeleteSessionRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.RequestOptions.Priority;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeAnnotationCode;
import com.google.spanner.v1.TypeCode;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

@RunWith(JUnit4.class)
public class DatabaseClientImplTest {
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private static final String TEST_DATABASE_ROLE = "my-role";
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
  private static final com.google.rpc.Status STATUS_OK =
      com.google.rpc.Status.newBuilder().setCode(com.google.rpc.Code.OK_VALUE).build();
  private static final Iterable<MutationGroup> MUTATION_GROUPS =
      ImmutableList.of(
          MutationGroup.of(
              Mutation.newInsertBuilder("FOO1").set("ID").to(1L).set("NAME").to("Bar1").build(),
              Mutation.newInsertBuilder("FOO2").set("ID").to(2L).set("NAME").to("Bar2").build()),
          MutationGroup.of(
              Mutation.newInsertBuilder("FOO3").set("ID").to(3L).set("NAME").to("Bar3").build(),
              Mutation.newInsertBuilder("FOO4").set("ID").to(4L).set("NAME").to("Bar4").build()),
          MutationGroup.of(
              Mutation.newInsertBuilder("FOO4").set("ID").to(4L).set("NAME").to("Bar4").build(),
              Mutation.newInsertBuilder("FOO5").set("ID").to(5L).set("NAME").to("Bar5").build()),
          MutationGroup.of(
              Mutation.newInsertBuilder("FOO6").set("ID").to(6L).set("NAME").to("Bar6").build()));
  private static final Iterable<BatchWriteResponse> BATCH_WRITE_RESPONSES =
      ImmutableList.of(
          BatchWriteResponse.newBuilder()
              .setStatus(STATUS_OK)
              .addAllIndexes(ImmutableList.of(0, 1))
              .build(),
          BatchWriteResponse.newBuilder()
              .setStatus(STATUS_OK)
              .addAllIndexes(ImmutableList.of(2, 3))
              .build());
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
        StatementResult.query(READ_ONE_KEY_VALUE_STATEMENT, READ_ONE_KEY_VALUE_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));
    mockSpanner.setBatchWriteResult(BATCH_WRITE_RESPONSES);

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
            .setDatabaseRole(TEST_DATABASE_ROLE)
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
  public void
      testPoolMaintainer_whenInactiveTransactionAndSessionIsNotFoundOnBackend_removeSessionsFromPool() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    2L)) // any session not used for more than 2s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      mockSpanner.setCommitExecutionTime(
          SimulatedExecutionTime.ofException(
              mockSpanner.createSessionNotFoundException("TEST_SESSION_NAME")));
      while (true) {
        try {
          transaction.executeUpdate(UPDATE_STATEMENT);

          // Simulate a delay of 3 minutes to ensure that the below transaction is a long-running
          // one.
          // As per this test, anything which takes more than 2s is long-running
          poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();
          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
        mockSpanner.setCommitExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(0, 0));
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    // first session executed update, session found to be long-running and cleaned up.
    // During commit, SessionNotFound exception from backend caused replacement of session and
    // transaction needs to be retried.
    // On retry, session again found to be long-running and cleaned up.
    // During commit, there was no exception from backend.

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(2, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenInactiveTransactionAndSessionExistsOnBackend_removeSessionsFromPool() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    2L)) // any session not used for more than 2s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.executeUpdate(UPDATE_STATEMENT);

          // Simulate a delay of 3 minutes to ensure that the below transaction is a long-running
          // one.
          // As per this test, anything which takes more than 2s is long-running
          poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();
          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    // first session executed update, session found to be long-running and cleaned up.
    // During commit, SessionNotFound exception from backend caused replacement of session and
    // transaction needs to be retried.
    // On retry, session again found to be long-running and cleaned up.
    // During commit, there was no exception from backend.
    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(1, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void testPoolMaintainer_whenLongRunningPartitionedUpdateRequest_takeNoAction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    2L)) // any session not used for more than 2s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();

    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;
    poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();

    client.executePartitionedUpdate(UPDATE_STATEMENT);

    // Simulate a delay of 3 minutes to ensure that the below transaction is a long-running one.
    // As per this test, anything which takes more than 2s is long-running
    poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();

    // force trigger pool maintainer to check for long-running sessions
    client.pool.poolMaintainer.maintainPool();

    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  /**
   * PDML transaction is expected to be long-running. This is indicated through session flag
   * eligibleForLongRunning = true . For all other transactions which are not expected to be
   * long-running eligibleForLongRunning = false.
   *
   * <p>Below tests uses a session for PDML transaction. Post that, the same session is used for
   * executeUpdate(). Both transactions are long-running. The test verifies that
   * eligibleForLongRunning = false for the second transaction, and it's identified as a
   * long-running transaction.
   */
  @Test
  public void testPoolMaintainer_whenPDMLFollowedByInactiveTransaction_removeSessionsFromPool() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    2L)) // any session not used for more than 2s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();

    client.executePartitionedUpdate(UPDATE_STATEMENT);

    // Simulate a delay of 3 minutes to ensure that the below transaction is a long-running one.
    // As per this test, anything which takes more than 2s is long-running
    poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();

    // force trigger pool maintainer to check for long-running sessions
    client.pool.poolMaintainer.maintainPool();

    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());

    poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.executeUpdate(UPDATE_STATEMENT);

          // Simulate a delay of 3 minutes to ensure that the below transaction is a long-running
          // one.
          // As per this test, anything which takes more than 2s is long-running
          poolMaintainerClock.currentTimeMillis += Duration.ofMinutes(3).toMillis();
          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    // first session executed update, session found to be long-running and cleaned up.
    // During commit, SessionNotFound exception from backend caused replacement of session and
    // transaction needs to be retried.
    // On retry, session again found to be long-running and cleaned up.
    // During commit, there was no exception from backend.
    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(1, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningReadsUsingTransactionRunner_retainSessionForTransaction()
          throws Exception {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction -> {
          try (ResultSet resultSet =
              transaction.read(
                  READ_TABLE_NAME,
                  KeySet.singleKey(Key.of(1L)),
                  READ_COLUMN_NAMES,
                  Options.priority(RpcPriority.HIGH))) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          try (ResultSet resultSet =
              transaction.read(
                  READ_TABLE_NAME,
                  KeySet.singleKey(Key.of(1L)),
                  READ_COLUMN_NAMES,
                  Options.priority(RpcPriority.HIGH))) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          return null;
        });

    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningQueriesUsingTransactionRunner_retainSessionForTransaction()
          throws Exception {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction -> {
          try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          return null;
        });

    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningUpdatesUsingTransactionManager_retainSessionForTransaction()
          throws Exception {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.executeUpdate(UPDATE_STATEMENT);
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          transaction.executeUpdate(UPDATE_STATEMENT);
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningReadsUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet resultSet =
              transaction.read(
                  READ_TABLE_NAME,
                  KeySet.singleKey(Key.of(1L)),
                  READ_COLUMN_NAMES,
                  Options.priority(RpcPriority.HIGH))) {

            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          try (ResultSet resultSet =
              transaction.read(
                  READ_TABLE_NAME,
                  KeySet.singleKey(Key.of(1L)),
                  READ_COLUMN_NAMES,
                  Options.priority(RpcPriority.HIGH))) {

            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningReadRowUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.readRow(READ_TABLE_NAME, Key.of(1L), READ_COLUMN_NAMES);

          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          transaction.readRow(READ_TABLE_NAME, Key.of(1L), READ_COLUMN_NAMES);

          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningAnalyzeUpdateStatementUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet resultSet =
              transaction.analyzeUpdateStatement(UPDATE_STATEMENT, QueryAnalyzeMode.PROFILE); ) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          try (ResultSet resultSet =
              transaction.analyzeUpdateStatement(UPDATE_STATEMENT, QueryAnalyzeMode.PROFILE); ) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningBatchUpdatesUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.batchUpdate(Lists.newArrayList(UPDATE_STATEMENT));

          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          transaction.batchUpdate(Lists.newArrayList(UPDATE_STATEMENT));

          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningBatchUpdatesAsyncUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.batchUpdateAsync(Lists.newArrayList(UPDATE_STATEMENT));

          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          transaction.batchUpdateAsync(Lists.newArrayList(UPDATE_STATEMENT));

          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningExecuteQueryUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningExecuteQueryAsyncUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet resultSet = transaction.executeQueryAsync(SELECT1)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          try (ResultSet resultSet = transaction.executeQueryAsync(SELECT1)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void
      testPoolMaintainer_whenLongRunningAnalyzeQueryUsingTransactionManager_retainSessionForTransaction() {
    FakeClock poolMaintainerClock = new FakeClock();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(
                Duration.ofSeconds(
                    3L)) // any session not used for more than 3s will be long-running
            .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
            .setExecutionFrequency(Duration.ofSeconds(1)) // execute thread every 1s
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1) // to ensure there is 1 session and pool is 100% utilized
            .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
            .setLoopFrequency(1000L) // main thread runs every 1s
            .setPoolMaintainerClock(poolMaintainerClock)
            .build();
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setDatabaseRole(TEST_DATABASE_ROLE)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(sessionPoolOptions)
            .build()
            .getService();
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Instant initialExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet resultSet = transaction.analyzeQuery(SELECT1, QueryAnalyzeMode.PROFILE)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(1050).toMillis();

          try (ResultSet resultSet = transaction.analyzeQuery(SELECT1, QueryAnalyzeMode.PROFILE)) {
            while (resultSet.next()) {}
          }
          poolMaintainerClock.currentTimeMillis += Duration.ofMillis(2050).toMillis();

          // force trigger pool maintainer to check for long-running sessions
          client.pool.poolMaintainer.maintainPool();

          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    Instant endExecutionTime = client.pool.poolMaintainer.lastExecutionTime;

    assertNotEquals(
        endExecutionTime,
        initialExecutionTime); // if session clean up task runs then these timings won't match
    assertEquals(0, client.pool.numLeakedSessionsRemoved());
    assertTrue(client.pool.getNumberOfSessionsInPool() <= client.pool.totalSessions());
  }

  @Test
  public void testWrite() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Timestamp timestamp =
        client.write(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getRequestOptions());
    assertEquals(Priority.PRIORITY_UNSPECIFIED, commit.getRequestOptions().getPriority());
  }

  @Test
  public void testWriteAborted() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    Timestamp timestamp =
        client.write(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(2, commitRequests.size());
  }

  @Test
  public void testWriteAtLeastOnceAborted() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    Timestamp timestamp =
        client.writeAtLeastOnce(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(2, commitRequests.size());
  }

  @Test
  public void testWriteWithOptions() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.writeWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.priority(RpcPriority.HIGH));

    List<CommitRequest> commits = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commits).hasSize(1);
    CommitRequest commit = commits.get(0);
    assertNotNull(commit.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, commit.getRequestOptions().getPriority());
  }

  @Test
  public void testWriteWithCommitStats() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    CommitResponse response =
        client.writeWithOptions(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
            Options.commitStats());
    assertNotNull(response);
    assertNotNull(response.getCommitTimestamp());
    assertNotNull(response.getCommitStats());
  }

  @Test
  public void testWriteAtLeastOnce() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Timestamp timestamp =
        client.writeAtLeastOnce(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);
  }

  @Test
  public void testWriteAtLeastOnceWithCommitStats() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    CommitResponse response =
        client.writeAtLeastOnceWithOptions(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
            Options.commitStats());
    assertNotNull(response);
    assertNotNull(response.getCommitTimestamp());
    assertNotNull(response.getCommitStats());

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getSingleUseTransaction());
    assertTrue(commit.getSingleUseTransaction().hasReadWrite());
    assertNotNull(commit.getRequestOptions());
    assertEquals(Priority.PRIORITY_UNSPECIFIED, commit.getRequestOptions().getPriority());
  }

  @Test
  public void testWriteAtLeastOnceWithOptions() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.writeAtLeastOnceWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.priority(RpcPriority.LOW));

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getSingleUseTransaction());
    assertTrue(commit.getSingleUseTransaction().hasReadWrite());
    assertNotNull(commit.getRequestOptions());
    assertEquals(Priority.PRIORITY_LOW, commit.getRequestOptions().getPriority());
  }

  @Test
  public void testWriteAtLeastOnceWithTagOptions() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.writeAtLeastOnceWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.tag("app=spanner,env=test"));

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getSingleUseTransaction());
    assertTrue(commit.getSingleUseTransaction().hasReadWrite());
    assertNotNull(commit.getRequestOptions());
    assertThat(commit.getRequestOptions().getTransactionTag()).isEqualTo("app=spanner,env=test");
    assertThat(commit.getRequestOptions().getRequestTag()).isEmpty();
  }

  @Test
  public void testBatchWriteAtLeastOnceWithoutOptions() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));

    ServerStream<BatchWriteResponse> responseStream = client.batchWriteAtLeastOnce(MUTATION_GROUPS);
    int idx = 0;
    for (BatchWriteResponse response : responseStream) {
      assertEquals(
          response.getStatus(),
          com.google.rpc.Status.newBuilder().setCode(com.google.rpc.Code.OK_VALUE).build());
      assertEquals(response.getIndexesList(), ImmutableList.of(idx, idx + 1));
      idx += 2;
    }

    assertNotNull(responseStream);
    List<BatchWriteRequest> requests = mockSpanner.getRequestsOfType(BatchWriteRequest.class);
    assertEquals(requests.size(), 1);
    BatchWriteRequest request = requests.get(0);
    assertEquals(request.getMutationGroupsCount(), 4);
    assertEquals(request.getRequestOptions().getPriority(), Priority.PRIORITY_UNSPECIFIED);
  }

  @Test
  public void testBatchWriteAtLeastOnceWithOptions() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ServerStream<BatchWriteResponse> responseStream =
        client.batchWriteAtLeastOnce(MUTATION_GROUPS, Options.priority(RpcPriority.LOW));
    for (BatchWriteResponse response : responseStream) {}

    assertNotNull(responseStream);
    List<BatchWriteRequest> requests = mockSpanner.getRequestsOfType(BatchWriteRequest.class);
    assertEquals(requests.size(), 1);
    BatchWriteRequest request = requests.get(0);
    assertEquals(request.getMutationGroupsCount(), 4);
    assertEquals(request.getRequestOptions().getPriority(), Priority.PRIORITY_LOW);
  }

  @Test
  public void testBatchWriteAtLeastOnceWithTagOptions() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ServerStream<BatchWriteResponse> responseStream =
        client.batchWriteAtLeastOnce(MUTATION_GROUPS, Options.tag("app=spanner,env=test"));
    for (BatchWriteResponse response : responseStream) {}

    assertNotNull(responseStream);
    List<BatchWriteRequest> requests = mockSpanner.getRequestsOfType(BatchWriteRequest.class);
    assertEquals(requests.size(), 1);
    BatchWriteRequest request = requests.get(0);
    assertEquals(request.getMutationGroupsCount(), 4);
    assertEquals(request.getRequestOptions().getTransactionTag(), "app=spanner,env=test");
    assertThat(request.getRequestOptions().getRequestTag()).isEmpty();
  }

  @Test
  public void testExecuteQueryWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet =
        client
            .singleUse()
            .executeQuery(SELECT1, Options.tag("app=spanner,env=test,action=query"))) {
      while (resultSet.next()) {}
    }

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag())
        .isEqualTo("app=spanner,env=test,action=query");
    assertThat(request.getRequestOptions().getTransactionTag()).isEmpty();
  }

  @Test
  public void testExecuteReadWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet =
        client
            .singleUse()
            .read(
                READ_TABLE_NAME,
                KeySet.singleKey(Key.of(1L)),
                READ_COLUMN_NAMES,
                Options.tag("app=spanner,env=test,action=read"))) {
      while (resultSet.next()) {}
    }

    List<ReadRequest> requests = mockSpanner.getRequestsOfType(ReadRequest.class);
    assertThat(requests).hasSize(1);
    ReadRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag())
        .isEqualTo("app=spanner,env=test,action=read");
    assertThat(request.getRequestOptions().getTransactionTag()).isEmpty();
  }

  @Test
  public void testReadWriteExecuteQueryWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner =
        client.readWriteTransaction(Options.tag("app=spanner,env=test,action=txn"));
    runner.run(
        transaction -> {
          try (ResultSet resultSet =
              transaction.executeQuery(SELECT1, Options.tag("app=spanner,env=test,action=query"))) {
            while (resultSet.next()) {}
          }
          return null;
        });

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag())
        .isEqualTo("app=spanner,env=test,action=query");
    assertThat(request.getRequestOptions().getTransactionTag())
        .isEqualTo("app=spanner,env=test,action=txn");
  }

  @Test
  public void testReadWriteExecuteReadWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner =
        client.readWriteTransaction(Options.tag("app=spanner,env=test,action=txn"));
    runner.run(
        transaction -> {
          try (ResultSet resultSet =
              transaction.read(
                  READ_TABLE_NAME,
                  KeySet.singleKey(Key.of(1L)),
                  READ_COLUMN_NAMES,
                  Options.tag("app=spanner,env=test,action=read"))) {
            while (resultSet.next()) {}
          }
          return null;
        });

    List<ReadRequest> requests = mockSpanner.getRequestsOfType(ReadRequest.class);
    assertThat(requests).hasSize(1);
    ReadRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag())
        .isEqualTo("app=spanner,env=test,action=read");
    assertThat(request.getRequestOptions().getTransactionTag())
        .isEqualTo("app=spanner,env=test,action=txn");
  }

  @Test
  public void testExecuteUpdateWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction ->
            transaction.executeUpdate(
                UPDATE_STATEMENT, Options.tag("app=spanner,env=test,action=update")));

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag())
        .isEqualTo("app=spanner,env=test,action=update");
    assertThat(request.getRequestOptions().getTransactionTag()).isEmpty();
  }

  @Test
  public void testBatchUpdateWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner =
        client.readWriteTransaction(Options.tag("app=spanner,env=test,action=txn"));
    runner.run(
        transaction ->
            transaction.batchUpdate(
                Collections.singletonList(UPDATE_STATEMENT),
                Options.tag("app=spanner,env=test,action=batch")));

    List<ExecuteBatchDmlRequest> requests =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteBatchDmlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag())
        .isEqualTo("app=spanner,env=test,action=batch");
    assertThat(request.getRequestOptions().getTransactionTag())
        .isEqualTo("app=spanner,env=test,action=txn");
  }

  @Test
  public void testPartitionedDMLWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.executePartitionedUpdate(
        UPDATE_STATEMENT, Options.tag("app=spanner,env=test,action=dml"));

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag())
        .isEqualTo("app=spanner,env=test,action=dml");
    assertThat(request.getRequestOptions().getTransactionTag()).isEmpty();
  }

  @Test
  public void testCommitWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner =
        client.readWriteTransaction(Options.tag("app=spanner,env=test,action=commit"));
    runner.run(
        transaction -> {
          transaction.buffer(Mutation.delete("TEST", KeySet.all()));
          return null;
        });

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag()).isEmpty();
    assertThat(request.getRequestOptions().getTransactionTag())
        .isEqualTo("app=spanner,env=test,action=commit");
  }

  @Test
  public void testTransactionManagerCommitWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionManager manager =
        client.transactionManager(Options.tag("app=spanner,env=test,action=manager"));
    TransactionContext transaction = manager.begin();
    transaction.buffer(Mutation.delete("TEST", KeySet.all()));
    manager.commit();

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag()).isEmpty();
    assertThat(request.getRequestOptions().getTransactionTag())
        .isEqualTo("app=spanner,env=test,action=manager");
  }

  @Test
  public void testAsyncRunnerCommitWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    AsyncRunner runner = client.runAsync(Options.tag("app=spanner,env=test,action=runner"));
    get(
        runner.runAsync(
            txn -> {
              txn.buffer(Mutation.delete("TEST", KeySet.all()));
              return ApiFutures.immediateFuture(null);
            },
            executor));

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag()).isEmpty();
    assertThat(request.getRequestOptions().getTransactionTag())
        .isEqualTo("app=spanner,env=test,action=runner");
  }

  @Test
  public void testAsyncTransactionManagerCommitWithTag() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (AsyncTransactionManager manager =
        client.transactionManagerAsync(Options.tag("app=spanner,env=test,action=manager"))) {
      TransactionContextFuture transaction = manager.beginAsync();
      get(
          transaction
              .then(
                  (txn, input) -> {
                    txn.buffer(Mutation.delete("TEST", KeySet.all()));
                    return ApiFutures.immediateFuture(null);
                  },
                  executor)
              .commitAsync());
    }

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertThat(request.getRequestOptions().getRequestTag()).isEmpty();
    assertThat(request.getRequestOptions().getTransactionTag())
        .isEqualTo("app=spanner,env=test,action=manager");
  }

  @Test
  public void singleUse() {
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Collection<PooledSessionFuture> checkedOut = client.pool.checkedOutSessions;
    assertThat(checkedOut).isEmpty();
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
      assertThat(rs.next()).isTrue();
      assertThat(checkedOut).hasSize(1);
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
    assertThat(checkedOut).isEmpty();
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
              resultSet -> {
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
              });
    }
    res.get();
    assertThat(rowCount.get()).isEqualTo(1);
  }

  @Test
  public void singleUseAsyncWithoutCallback() {
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
              resultSet -> {
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
  public void testReadWriteTransaction() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction -> {
          transaction.executeUpdate(UPDATE_STATEMENT);
          return null;
        });
    assertNotNull(runner.getCommitTimestamp());
  }

  @Test
  public void testReadWriteTransaction_returnsCommitStats() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction(Options.commitStats());
    runner.run(
        transaction -> {
          transaction.buffer(Mutation.delete("FOO", Key.of("foo")));
          return null;
        });
    assertNotNull(runner.getCommitResponse());
    assertNotNull(runner.getCommitResponse().getCommitStats());
    assertEquals(1L, runner.getCommitResponse().getCommitStats().getMutationCount());
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
        transaction -> {
          transaction.executeUpdate(UPDATE_STATEMENT);
          return null;
        });
  }

  @Test
  public void testRunAsync() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AsyncRunner runner = client.runAsync();
    ApiFuture<Long> result =
        runner.runAsync(
            txn -> ApiFutures.immediateFuture(txn.executeUpdate(UPDATE_STATEMENT)), executor);
    assertEquals(UPDATE_COUNT, result.get().longValue());
    assertNotNull(runner.getCommitTimestamp().get());
    executor.shutdown();
  }

  @Test
  public void testRunAsync_returnsCommitStats() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AsyncRunner runner = client.runAsync(Options.commitStats());
    ApiFuture<Void> result =
        runner.runAsync(
            txn -> {
              txn.buffer(Mutation.delete("FOO", Key.of("foo")));
              return ApiFutures.immediateFuture(null);
            },
            executor);
    assertNull(get(result));
    assertNotNull(get(runner.getCommitResponse()));
    assertNotNull(get(runner.getCommitResponse()).getCommitStats());
    assertEquals(1L, get(runner.getCommitResponse()).getCommitStats().getMutationCount());
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
            txn -> ApiFutures.immediateFuture(txn.executeUpdate(UPDATE_STATEMENT)), executor);
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
            txn -> ApiFutures.immediateFuture(txn.executeUpdate(INVALID_UPDATE_STATEMENT)),
            executor);

    ExecutionException e = assertThrows(ExecutionException.class, () -> fut.get());
    assertThat(e.getCause()).isInstanceOf(SpannerException.class);
    SpannerException se = (SpannerException) e.getCause();
    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);

    executor.shutdown();
  }

  @SuppressWarnings("resource")
  @Test
  public void testTransactionManager() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.executeUpdate(UPDATE_STATEMENT);
          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void testTransactionManager_returnsCommitStats() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (TransactionManager manager = client.transactionManager(Options.commitStats())) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          transaction.buffer(Mutation.delete("FOO", Key.of("foo")));
          manager.commit();
          assertNotNull(manager.getCommitResponse());
          assertNotNull(manager.getCommitResponse().getCommitStats());
          assertEquals(1L, manager.getCommitResponse().getCommitStats().getMutationCount());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerIsNonBlocking() throws Exception {
    mockSpanner.freeze();
    DatabaseClient client =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (TransactionManager txManager = client.transactionManager()) {
      mockSpanner.unfreeze();
      TransactionContext transaction = txManager.begin();
      while (true) {
        try {
          transaction.executeUpdate(UPDATE_STATEMENT);
          txManager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = txManager.resetForRetry();
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerExecuteQueryAsync() throws Exception {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    final AtomicInteger rowCount = new AtomicInteger();
    try (TransactionManager txManager = client.transactionManager()) {
      TransactionContext transaction = txManager.begin();
      while (true) {
        try {
          try (AsyncResultSet rs = transaction.executeQueryAsync(SELECT1)) {
            rs.setCallback(
                executor,
                resultSet -> {
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
                });
          }
          txManager.commit();
          break;
        } catch (AbortedException e) {
          transaction = txManager.resetForRetry();
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
    mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(20, 0));
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
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction -> {
                            transaction.executeUpdate(UPDATE_STATEMENT);
                            return null;
                          }));
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
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
      SpannerException e =
          assertThrows(
              SpannerException.class, () -> client.executePartitionedUpdate(UPDATE_STATEMENT));
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());

      // Normal DML should not timeout.
      mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(10, 0));
      long updateCount =
          client
              .readWriteTransaction()
              .run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
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
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT)));
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
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
          assertThrows(ResourceNotFoundException.class, () -> rs.next());
          // The server should only receive one BatchCreateSessions request.
          assertThat(mockSpanner.getRequests()).hasSize(1);
        }
        assertThrows(ResourceNotFoundException.class, () -> dbClient.readWriteTransaction());
        // No additional requests should have been sent by the client.
        assertThat(mockSpanner.getRequests()).hasSize(1);
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
          assertThrows(ResourceNotFoundException.class, () -> rs.next());
        }
        assertThrows(
            ResourceNotFoundException.class,
            () -> dbClient.readWriteTransaction().run(transaction -> null));

        // Now simulate that the database has been re-created. The database client should still
        // throw DatabaseNotFoundExceptions, as it is not the same database. The server should not
        // receive any new requests.
        mockSpanner.reset();
        // All subsequent calls should fail with a DatabaseNotFoundException.
        assertThrows(
            ResourceNotFoundException.class, () -> dbClient.singleUse().executeQuery(SELECT1));
        assertThrows(
            ResourceNotFoundException.class,
            () -> dbClient.readWriteTransaction().run(transaction -> null));
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
  public void testGetInvalidatedClientMultipleTimes() {
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
      try (Spanner spanner =
          SpannerOptions.newBuilder()
              .setProjectId(TEST_PROJECT)
              .setChannelProvider(channelProvider)
              .setCredentials(NoCredentials.getInstance())
              .setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build())
              .build()
              .getService()) {
        for (int run = 0; run < 2; run++) {
          DatabaseClientImpl dbClient =
              (DatabaseClientImpl)
                  spanner.getDatabaseClient(
                      DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
          for (int useClient = 0; useClient < 2; useClient++) {
            // Using the same client multiple times should continue to return the same
            // ResourceNotFoundException, even though the session pool has been invalidated.
            assertThrows(
                ResourceNotFoundException.class,
                () -> dbClient.singleUse().executeQuery(SELECT1).next());
            // The server should only receive one BatchCreateSessions request for each run as we
            // have set MinSessions=0.
            assertThat(mockSpanner.getRequests()).hasSize(run + 1);
            assertThat(dbClient.pool.isValid()).isFalse();
          }
        }
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
                transaction -> {
                  assertThat(client.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions - 1);
                  return transaction.executeUpdate(UPDATE_STATEMENT);
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
                transaction -> {
                  // Client1 should have 1 session checked out.
                  // Client2 should have 0 sessions checked out.
                  assertThat(client1.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions - 1);
                  assertThat(client2.pool.getNumberOfSessionsInPool()).isEqualTo(minSessions);
                  Long add =
                      client2
                          .readWriteTransaction()
                          .run(
                              transaction1 -> {
                                // Both clients should now have 1 session checked out.
                                assertThat(client1.pool.getNumberOfSessionsInPool())
                                    .isEqualTo(minSessions - 1);
                                assertThat(client2.pool.getNumberOfSessionsInPool())
                                    .isEqualTo(minSessions - 1);
                                try (ResultSet rs = transaction1.executeQuery(SELECT1)) {
                                  if (rs.next()) {
                                    return rs.getLong(0);
                                  }
                                  return 0L;
                                }
                              });
                  try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                    if (rs.next()) {
                      return add + rs.getLong(0);
                    }
                    return add;
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
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE"));
      try (ResultSet rs =
          client
              .singleUse()
              .executeQuery(
                  Statement.newBuilder(SELECT1.getSql())
                      .withQueryOptions(
                          QueryOptions.newBuilder()
                              .setOptimizerVersion("1")
                              .setOptimizerStatisticsPackage("custom-package")
                              .build())
                      .build())) {
        // Just iterate over the results to execute the query.
        while (rs.next()) {}
      }
      // Check that the last query was executed using a custom optimizer version and statistics
      // package.
      List<AbstractMessage> requests = mockSpanner.getRequests();
      assertThat(requests).isNotEmpty();
      assertThat(requests.get(requests.size() - 1)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest request = (ExecuteSqlRequest) requests.get(requests.size() - 1);
      assertThat(request.getQueryOptions()).isNotNull();
      assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("1");
      assertThat(request.getQueryOptions().getOptimizerStatisticsPackage())
          .isEqualTo("custom-package");
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
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE"));
      try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
        try (ResultSet rs =
            tx.analyzeQuery(
                Statement.newBuilder(SELECT1.getSql())
                    .withQueryOptions(
                        QueryOptions.newBuilder()
                            .setOptimizerVersion("1")
                            .setOptimizerStatisticsPackage("custom-package")
                            .build())
                    .build(),
                QueryAnalyzeMode.PROFILE)) {
          // Just iterate over the results to execute the query.
          while (rs.next()) {}
        }
      }
      // Check that the last query was executed using a custom optimizer version and statistics
      // package.
      List<AbstractMessage> requests = mockSpanner.getRequests();
      assertThat(requests).isNotEmpty();
      assertThat(requests.get(requests.size() - 1)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest request = (ExecuteSqlRequest) requests.get(requests.size() - 1);
      assertThat(request.getQueryOptions()).isNotNull();
      assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("1");
      assertThat(request.getQueryOptions().getOptimizerStatisticsPackage())
          .isEqualTo("custom-package");
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
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build())
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
                  .withQueryOptions(
                      QueryOptions.newBuilder()
                          .setOptimizerVersion("1")
                          .setOptimizerStatisticsPackage("custom-package")
                          .build())
                  .build());
      try (ResultSet rs = transaction.execute(partitions.get(0))) {
        // Just iterate over the results to execute the query.
        while (rs.next()) {}
      } finally {
        transaction.cleanup();
      }
      // Check if the last query executed is a DeleteSessionRequest and the second last query
      // executed is a ExecuteSqlRequest and was executed using a custom optimizer version and
      // statistics package.
      List<AbstractMessage> requests = mockSpanner.getRequests();
      assert requests.size() >= 2 : "required to have at least 2 requests";
      assertThat(requests.get(requests.size() - 1)).isInstanceOf(DeleteSessionRequest.class);
      assertThat(requests.get(requests.size() - 2)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest executeSqlRequest = (ExecuteSqlRequest) requests.get(requests.size() - 2);
      assertThat(executeSqlRequest.getQueryOptions()).isNotNull();
      assertThat(executeSqlRequest.getQueryOptions().getOptimizerVersion()).isEqualTo("1");
      assertThat(executeSqlRequest.getQueryOptions().getOptimizerStatisticsPackage())
          .isEqualTo("custom-package");
    }
  }

  @Test
  public void testAsyncQuery() throws Exception {
    final int EXPECTED_ROW_COUNT = 10;
    com.google.cloud.spanner.connection.RandomResultSetGenerator generator =
        new RandomResultSetGenerator(EXPECTED_ROW_COUNT);
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
              asyncResultSet -> {
                try {
                  while (true) {
                    switch (rs.tryNext()) {
                      case DONE:
                        finished.set(true);
                        return CallbackResponse.DONE;
                      case NOT_READY:
                        return CallbackResponse.CONTINUE;
                      case OK:
                        receivedResults.add(asyncResultSet.getCurrentRowAsStruct());
                        break;
                      default:
                        throw new IllegalStateException("Unknown cursor state");
                    }
                  }
                } catch (Throwable t) {
                  finished.setException(t);
                  return CallbackResponse.DONE;
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
          client.singleUse().readRow("MyTable", Key.of(0), Collections.singletonList("MyColumn"));
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
      // Actually trying to get any results will cause an exception.
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.PERMISSION_DENIED, e.getErrorCode());
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
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
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
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
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
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
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
            () -> {
              // Query should fail with a timeout.
              try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
                SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
              }
              // Update should succeed.
              client
                  .readWriteTransaction()
                  .run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
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
      try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
        SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
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

  @Test
  public void testExecuteQueryWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet =
        client.singleUse().executeQuery(SELECT1, Options.priority(RpcPriority.HIGH))) {
      while (resultSet.next()) {}
    }

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testExecuteReadWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet =
        client
            .singleUse()
            .read(
                READ_TABLE_NAME,
                KeySet.singleKey(Key.of(1L)),
                READ_COLUMN_NAMES,
                Options.priority(RpcPriority.HIGH))) {
      while (resultSet.next()) {}
    }

    List<ReadRequest> requests = mockSpanner.getRequestsOfType(ReadRequest.class);
    assertThat(requests).hasSize(1);
    ReadRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testReadWriteExecuteQueryWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction -> {
          try (ResultSet resultSet =
              transaction.executeQuery(SELECT1, Options.priority(RpcPriority.HIGH))) {
            while (resultSet.next()) {}
          }
          return null;
        });

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testReadWriteExecuteReadWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction -> {
          try (ResultSet resultSet =
              transaction.read(
                  READ_TABLE_NAME,
                  KeySet.singleKey(Key.of(1L)),
                  READ_COLUMN_NAMES,
                  Options.priority(RpcPriority.HIGH))) {
            while (resultSet.next()) {}
          }
          return null;
        });

    List<ReadRequest> requests = mockSpanner.getRequestsOfType(ReadRequest.class);
    assertThat(requests).hasSize(1);
    ReadRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testExecuteUpdateWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction ->
            transaction.executeUpdate(UPDATE_STATEMENT, Options.priority(RpcPriority.HIGH)));

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testBatchUpdateWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction ->
            transaction.batchUpdate(
                Collections.singletonList(UPDATE_STATEMENT), Options.priority(RpcPriority.HIGH)));

    List<ExecuteBatchDmlRequest> requests =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteBatchDmlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testPartitionedDMLWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client.executePartitionedUpdate(UPDATE_STATEMENT, Options.priority(RpcPriority.HIGH));

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests).hasSize(1);
    ExecuteSqlRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testCommitWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionRunner runner = client.readWriteTransaction(Options.priority(RpcPriority.HIGH));
    runner.run(
        transaction -> {
          transaction.buffer(Mutation.delete("TEST", KeySet.all()));
          return null;
        });

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testTransactionManagerCommitWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    TransactionManager manager = client.transactionManager(Options.priority(RpcPriority.HIGH));
    TransactionContext transaction = manager.begin();
    transaction.buffer(Mutation.delete("TEST", KeySet.all()));
    manager.commit();

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testAsyncRunnerCommitWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    AsyncRunner runner = client.runAsync(Options.priority(RpcPriority.HIGH));
    get(
        runner.runAsync(
            txn -> {
              txn.buffer(Mutation.delete("TEST", KeySet.all()));
              return ApiFutures.immediateFuture(null);
            },
            executor));

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void testAsyncTransactionManagerCommitWithPriority() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (AsyncTransactionManager manager =
        client.transactionManagerAsync(Options.priority(RpcPriority.HIGH))) {
      TransactionContextFuture transaction = manager.beginAsync();
      get(
          transaction
              .then(
                  (txn, input) -> {
                    txn.buffer(Mutation.delete("TEST", KeySet.all()));
                    return ApiFutures.immediateFuture(null);
                  },
                  executor)
              .commitAsync());
    }

    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getRequestOptions());
    assertEquals(Priority.PRIORITY_HIGH, request.getRequestOptions().getPriority());
  }

  @Test
  public void singleUseNoAction_ClearsCheckedOutSession() {
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Collection<PooledSessionFuture> checkedOut = client.pool.checkedOutSessions;
    assertThat(checkedOut).isEmpty();

    // Getting a single use read-only transaction and not using it should not cause any sessions
    // to be stuck in the map of checked out sessions.
    client.singleUse().close();

    assertThat(checkedOut).isEmpty();
  }

  @Test
  public void singleUseReadOnlyTransactionNoAction_ClearsCheckedOutSession() {
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Collection<PooledSessionFuture> checkedOut = client.pool.checkedOutSessions;
    assertThat(checkedOut).isEmpty();

    client.singleUseReadOnlyTransaction().close();

    assertThat(checkedOut).isEmpty();
  }

  @Test
  public void readWriteTransactionNoAction_ClearsCheckedOutSession() {
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Collection<PooledSessionFuture> checkedOut = client.pool.checkedOutSessions;
    assertThat(checkedOut).isEmpty();

    client.readWriteTransaction();

    assertThat(checkedOut).isEmpty();
  }

  @Test
  public void readOnlyTransactionNoAction_ClearsCheckedOutSession() {
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Collection<PooledSessionFuture> checkedOut = client.pool.checkedOutSessions;
    assertThat(checkedOut).isEmpty();

    client.readOnlyTransaction().close();

    assertThat(checkedOut).isEmpty();
  }

  @Test
  public void transactionManagerNoAction_ClearsCheckedOutSession() {
    DatabaseClientImpl client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Collection<PooledSessionFuture> checkedOut = client.pool.checkedOutSessions;
    assertThat(checkedOut).isEmpty();

    client.transactionManager().close();

    assertThat(checkedOut).isEmpty();
  }

  @Test
  public void transactionContextFailsIfUsedMultipleTimes() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));

    Function<TransactionContext, Long> function =
        new Function<TransactionContext, Long>() {
          TransactionContext ctx;

          @Override
          public Long apply(TransactionContext transactionContext) {
            if (ctx == null) {
              ctx = transactionContext;
            }
            try (ResultSet rs = ctx.executeQuery(SELECT1)) {
              while (rs.next()) {}
            }
            return 1L;
          }
        };
    assertEquals(Long.valueOf(1L), client.readWriteTransaction().run(tx -> function.apply(tx)));
    SpannerException exception =
        assertThrows(
            SpannerException.class,
            () -> client.readWriteTransaction().run(tx -> function.apply(tx)));
    assertTrue(exception.getMessage().contains("Context has been closed"));
  }

  @Test
  public void testGetDialectDefault() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    assertEquals(Dialect.GOOGLE_STANDARD_SQL, client.getDialect());
  }

  @Test
  public void testGetDialectDefaultPreloaded() {
    try (Spanner spanner =
        this.spanner
            .getOptions()
            .toBuilder()
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setAutoDetectDialect(true).build())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      assertEquals(Dialect.GOOGLE_STANDARD_SQL, client.getDialect());
    }
  }

  @Test
  public void testGetDialectPostgreSQL() {
    mockSpanner.putStatementResult(StatementResult.detectDialectResult(Dialect.POSTGRESQL));
    try {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      assertEquals(Dialect.POSTGRESQL, client.getDialect());
    } finally {
      mockSpanner.putStatementResult(
          StatementResult.detectDialectResult(Dialect.GOOGLE_STANDARD_SQL));
    }
  }

  @Test
  public void testGetDialectPostgreSQLPreloaded() {
    mockSpanner.putStatementResult(StatementResult.detectDialectResult(Dialect.POSTGRESQL));
    try (Spanner spanner =
        this.spanner
            .getOptions()
            .toBuilder()
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setAutoDetectDialect(true).build())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      assertEquals(Dialect.POSTGRESQL, client.getDialect());
    } finally {
      mockSpanner.putStatementResult(
          StatementResult.detectDialectResult(Dialect.GOOGLE_STANDARD_SQL));
    }
  }

  @Test
  public void testGetDialect_FailsDirectlyIfDatabaseNotFound() {
    mockSpanner.setBatchCreateSessionsExecutionTime(
        SimulatedExecutionTime.stickyDatabaseNotFoundException("invalid-database"));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));

    SpannerException exception = assertThrows(SpannerException.class, client::getDialect);
    assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    assertTrue(
        exception
            .getMessage()
            .contains(
                "NOT_FOUND: Database not found: Database with id invalid-database not found"));
  }

  @Test
  public void testGetDialectDefaultPreloaded_FailsDirectlyIfDatabaseNotFound() {
    mockSpanner.setBatchCreateSessionsExecutionTime(
        SimulatedExecutionTime.stickyDatabaseNotFoundException("invalid-database"));
    try (Spanner spanner =
        this.spanner
            .getOptions()
            .toBuilder()
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setAutoDetectDialect(true).build())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      SpannerException exception = assertThrows(SpannerException.class, client::getDialect);
      assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
      assertTrue(
          exception
              .getMessage()
              .contains(
                  "NOT_FOUND: Database not found: Database with id invalid-database not found"));
    }
  }

  @Test
  public void testUntypedNullParameters() {
    Statement statement =
        Statement.newBuilder("INSERT INTO FOO (BAR) VALUES (@p)")
            .bind("p")
            .to((Value) null)
            .build();
    mockSpanner.putStatementResult(StatementResult.update(statement, 1L));

    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    Long updateCount =
        client.readWriteTransaction().run(transaction -> transaction.executeUpdate(statement));

    assertNotNull(updateCount);
    assertEquals(1L, updateCount.longValue());
  }

  @Test
  public void testGetDatabaseRole() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    assertEquals(TEST_DATABASE_ROLE, client.getDatabaseRole());
  }

  @Test
  public void testAnalyzeUpdateStatement() {
    String sql = "update foo set bar=1 where baz=@param";
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of(sql),
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setUndeclaredParameters(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("param")
                                        .setType(Type.newBuilder().setCode(TypeCode.STRING).build())
                                        .build())
                                .build())
                        .build())
                .setStats(ResultSetStats.newBuilder().setRowCountExact(0L).build())
                .build()));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              try (ResultSet resultSet =
                  transaction.analyzeUpdateStatement(Statement.of(sql), QueryAnalyzeMode.PLAN)) {
                assertFalse(resultSet.next());
                assertNotNull(resultSet.getStats());
                assertEquals(0L, resultSet.getStats().getRowCountExact());
                assertNotNull(resultSet.getMetadata());
                assertEquals(1, resultSet.getMetadata().getUndeclaredParameters().getFieldsCount());
                assertEquals(
                    "param",
                    resultSet.getMetadata().getUndeclaredParameters().getFields(0).getName());
                assertEquals(
                    Type.newBuilder().setCode(TypeCode.STRING).build(),
                    resultSet.getMetadata().getUndeclaredParameters().getFields(0).getType());
              }
              return null;
            });
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertEquals(QueryMode.PLAN, request.getQueryMode());
  }

  @Test
  public void testByteArray() {
    Random random = new Random();
    byte[] bytes = new byte[random.nextInt(200)];
    int numRows = 5;
    List<ListValue> rows = new ArrayList<>(numRows);
    for (int i = 0; i < numRows; i++) {
      random.nextBytes(bytes);
      rows.add(
          ListValue.newBuilder()
              .addValues(
                  com.google.protobuf.Value.newBuilder()
                      .setStringValue(
                          // Use both the Guava and the JDK encoder to encode the values to ensure
                          // that encoding/decoding using both of them works.
                          i % 2 == 0
                              ? Base64.getEncoder().encodeToString(bytes)
                              : BaseEncoding.base64().encode(bytes))
                      .build())
              .build());
    }
    Statement statement = Statement.of("select * from foo");
    mockSpanner.putStatementResult(
        StatementResult.query(
            statement,
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setType(Type.newBuilder().setCode(TypeCode.BYTES).build())
                                        .setName("f1")
                                        .build())
                                .build())
                        .build())
                .addAllRows(rows)
                .build()));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        String base64String = resultSet.getValue(0).getAsString();
        ByteArray byteArray = resultSet.getBytes(0);
        // Use the 'old' ByteArray.fromBase64(..) method that uses the Guava encoder to ensure that
        // the two encoders (JDK and Guava) return the same values.
        assertEquals(ByteArray.fromBase64(base64String), byteArray);
      }
    }
  }

  @Test
  public void testGetAllTypesAsString() {
    for (Dialect dialect : Dialect.values()) {
      Statement statement = Statement.of("select * from all_types");
      mockSpanner.putStatementResult(
          StatementResult.query(
              statement,
              com.google.spanner.v1.ResultSet.newBuilder()
                  .setMetadata(
                      RandomResultSetGenerator.generateAllTypesMetadata(
                          RandomResultSetGenerator.generateAllTypes(dialect)))
                  .addRows(
                      ListValue.newBuilder()
                          .addValues(
                              com.google.protobuf.Value.newBuilder().setBoolValue(true).build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder().setStringValue("100").build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder().setNumberValue(3.14d).build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue("6.626")
                                  .build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue("test-string")
                                  .build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue("{\"key1\": \"value1\"}")
                                  .build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue(
                                      Base64.getEncoder()
                                          .encodeToString(
                                              "test-bytes".getBytes(StandardCharsets.UTF_8)))
                                  .build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue("2023-01-11")
                                  .build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue("2023-01-11T11:55:18.123456789Z")
                                  .build())
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setBoolValue(true)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setBoolValue(false)
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue(String.valueOf(Long.MAX_VALUE))
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue(String.valueOf(Long.MIN_VALUE))
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNumberValue(-12345.6789d)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNumberValue(3.14d)
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("6.626")
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("-8.9123")
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("test-string1")
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("test-string2")
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("{\"key\": \"value1\"}")
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("{\"key\": \"value2\"}")
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue(
                                                      Base64.getEncoder()
                                                          .encodeToString(
                                                              "test-bytes1"
                                                                  .getBytes(
                                                                      StandardCharsets.UTF_8)))
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue(
                                                      Base64.getEncoder()
                                                          .encodeToString(
                                                              "test-bytes2"
                                                                  .getBytes(
                                                                      StandardCharsets.UTF_8)))
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("2000-02-29")
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("2000-01-01")
                                                  .build())
                                          .build()))
                          .addValues(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("2023-01-11T11:55:18.123456789Z")
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNullValue(NullValue.NULL_VALUE)
                                                  .build())
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setStringValue("2023-01-12T11:55:18Z")
                                                  .build())
                                          .build()))
                          .build())
                  .build()));

      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
        assertTrue(resultSet.next());
        int col = 0;
        assertAsString("true", resultSet, col++);
        assertAsString("100", resultSet, col++);
        assertAsString("3.14", resultSet, col++);
        assertAsString("6.626", resultSet, col++);
        assertAsString("test-string", resultSet, col++);
        assertAsString("{\"key1\": \"value1\"}", resultSet, col++);
        assertAsString(
            Base64.getEncoder().encodeToString("test-bytes".getBytes(StandardCharsets.UTF_8)),
            resultSet,
            col++);
        assertAsString("2023-01-11", resultSet, col++);
        assertAsString("2023-01-11T11:55:18.123456789Z", resultSet, col++);

        assertAsString(ImmutableList.of("true", "NULL", "false"), resultSet, col++);
        assertAsString(
            ImmutableList.of(
                String.format("%d", Long.MAX_VALUE), String.format("%d", Long.MIN_VALUE), "NULL"),
            resultSet,
            col++);
        assertAsString(ImmutableList.of("NULL", "-12345.6789", "3.14"), resultSet, col++);
        assertAsString(ImmutableList.of("6.626", "NULL", "-8.9123"), resultSet, col++);
        assertAsString(ImmutableList.of("test-string1", "NULL", "test-string2"), resultSet, col++);
        assertAsString(
            ImmutableList.of("{\"key\": \"value1\"}", "{\"key\": \"value2\"}", "NULL"),
            resultSet,
            col++);
        assertAsString(
            ImmutableList.of(
                String.format(
                    "%s",
                    Base64.getEncoder()
                        .encodeToString("test-bytes1".getBytes(StandardCharsets.UTF_8))),
                String.format(
                    "%s",
                    Base64.getEncoder()
                        .encodeToString("test-bytes2".getBytes(StandardCharsets.UTF_8))),
                "NULL"),
            resultSet,
            col++);
        assertAsString(ImmutableList.of("2000-02-29", "NULL", "2000-01-01"), resultSet, col++);
        assertAsString(
            ImmutableList.of("2023-01-11T11:55:18.123456789Z", "NULL", "2023-01-12T11:55:18Z"),
            resultSet,
            col++);

        assertFalse(resultSet.next());
      }
    }
  }

  @Test
  public void testSelectUnknownType() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("SELECT * FROM foo"),
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c")
                                        .setType(
                                            Type.newBuilder()
                                                .setCodeValue(Integer.MAX_VALUE)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            com.google.protobuf.Value.newBuilder().setStringValue("bar").build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            com.google.protobuf.Value.newBuilder().setBoolValue(true).build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            com.google.protobuf.Value.newBuilder().setNumberValue(3.14d).build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            com.google.protobuf.Value.newBuilder()
                                .setNullValue(NullValue.NULL_VALUE)
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(
                            com.google.protobuf.Value.newBuilder()
                                .setListValue(
                                    ListValue.newBuilder()
                                        .addValues(
                                            com.google.protobuf.Value.newBuilder()
                                                .setStringValue("baz")
                                                .build())
                                        .addValues(
                                            com.google.protobuf.Value.newBuilder()
                                                .setBoolValue(false)
                                                .build())
                                        .addValues(
                                            com.google.protobuf.Value.newBuilder()
                                                .setNumberValue(6.626)
                                                .build())
                                        .addValues(
                                            com.google.protobuf.Value.newBuilder()
                                                .setNullValue(NullValue.NULL_VALUE)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build()));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet = client.singleUse().executeQuery(Statement.of("SELECT * FROM foo"))) {
      assertTrue(resultSet.next());
      assertAsString("bar", resultSet, 0);

      assertTrue(resultSet.next());
      assertAsString("true", resultSet, 0);

      assertTrue(resultSet.next());
      assertAsString("3.14", resultSet, 0);

      assertTrue(resultSet.next());
      assertAsString("NULL", resultSet, 0);

      assertTrue(resultSet.next());
      assertAsString(ImmutableList.of("baz", "false", "6.626", "NULL"), resultSet, 0);

      assertFalse(resultSet.next());
    }
  }

  @Test
  public void testMetadataUnknownTypes() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("SELECT * FROM foo"),
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c1")
                                        .setType(
                                            Type.newBuilder()
                                                .setCodeValue(Integer.MAX_VALUE)
                                                .build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c2")
                                        .setType(
                                            Type.newBuilder()
                                                .setCode(TypeCode.STRING)
                                                .setTypeAnnotationValue(Integer.MAX_VALUE)
                                                .build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c3")
                                        .setType(
                                            Type.newBuilder()
                                                .setCodeValue(Integer.MAX_VALUE)
                                                .setTypeAnnotation(TypeAnnotationCode.PG_NUMERIC)
                                                .build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c4")
                                        .setType(
                                            Type.newBuilder()
                                                .setCode(TypeCode.ARRAY)
                                                .setArrayElementType(
                                                    Type.newBuilder()
                                                        .setCodeValue(Integer.MAX_VALUE)
                                                        .build())
                                                .build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c5")
                                        .setType(
                                            Type.newBuilder()
                                                .setCode(TypeCode.ARRAY)
                                                .setArrayElementType(
                                                    Type.newBuilder()
                                                        .setCode(TypeCode.STRING)
                                                        .setTypeAnnotationValue(Integer.MAX_VALUE)
                                                        .build())
                                                .build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c6")
                                        .setType(
                                            Type.newBuilder()
                                                // Set an unrecognized type with an array element
                                                // type. The client should recognize this as an
                                                // array.
                                                .setCodeValue(Integer.MAX_VALUE)
                                                .setArrayElementType(
                                                    Type.newBuilder()
                                                        .setCodeValue(Integer.MAX_VALUE)
                                                        .build())
                                                .build())
                                        .build())
                                .addFields(
                                    Field.newBuilder()
                                        .setName("c7")
                                        .setType(
                                            Type.newBuilder()
                                                .setCode(TypeCode.ARRAY)
                                                .setArrayElementType(
                                                    Type.newBuilder()
                                                        .setCodeValue(Integer.MAX_VALUE)
                                                        .setTypeAnnotation(
                                                            TypeAnnotationCode.PG_NUMERIC)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build()));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet = client.singleUse().executeQuery(Statement.of("SELECT * FROM foo"))) {
      // There are no rows, but we need to call resultSet.next() before we can get the metadata.
      assertFalse(resultSet.next());
      assertEquals(
          "STRUCT<c1 UNRECOGNIZED, c2 STRING<UNRECOGNIZED>, c3 UNRECOGNIZED<PG_NUMERIC>, c4 ARRAY<UNRECOGNIZED>, c5 ARRAY<STRING<UNRECOGNIZED>>, c6 UNRECOGNIZED<UNRECOGNIZED>, c7 ARRAY<UNRECOGNIZED<PG_NUMERIC>>>",
          resultSet.getType().toString());
      assertEquals(
          "UNRECOGNIZED", resultSet.getType().getStructFields().get(0).getType().toString());
      assertEquals(
          "STRING<UNRECOGNIZED>",
          resultSet.getType().getStructFields().get(1).getType().toString());
      assertEquals(
          "UNRECOGNIZED<PG_NUMERIC>",
          resultSet.getType().getStructFields().get(2).getType().toString());
      assertEquals(
          "ARRAY<UNRECOGNIZED>", resultSet.getType().getStructFields().get(3).getType().toString());
      assertEquals(Code.ARRAY, resultSet.getType().getStructFields().get(3).getType().getCode());
      assertEquals(
          Code.UNRECOGNIZED,
          resultSet.getType().getStructFields().get(3).getType().getArrayElementType().getCode());
      assertEquals(
          "ARRAY<STRING<UNRECOGNIZED>>",
          resultSet.getType().getStructFields().get(4).getType().toString());
      assertEquals(Code.ARRAY, resultSet.getType().getStructFields().get(4).getType().getCode());
      assertEquals(
          Code.UNRECOGNIZED,
          resultSet.getType().getStructFields().get(4).getType().getArrayElementType().getCode());
      assertEquals(
          "UNRECOGNIZED<UNRECOGNIZED>",
          resultSet.getType().getStructFields().get(5).getType().toString());
      assertEquals(
          Code.UNRECOGNIZED, resultSet.getType().getStructFields().get(5).getType().getCode());
      assertEquals(
          Code.UNRECOGNIZED,
          resultSet.getType().getStructFields().get(5).getType().getArrayElementType().getCode());
      assertEquals(
          "ARRAY<UNRECOGNIZED<PG_NUMERIC>>",
          resultSet.getType().getStructFields().get(6).getType().toString());
      assertEquals(Code.ARRAY, resultSet.getType().getStructFields().get(6).getType().getCode());
      assertEquals(
          Code.UNRECOGNIZED,
          resultSet.getType().getStructFields().get(6).getType().getArrayElementType().getCode());
    }
  }

  @Test
  public void testStatementWithBytesArrayParameter() {
    Statement statement =
        Statement.newBuilder("select id from test where b=@p1")
            .bind("p1")
            .toBytesArray(
                Arrays.asList(ByteArray.copyFrom("test1"), null, ByteArray.copyFrom("test2")))
            .build();
    mockSpanner.putStatementResult(StatementResult.query(statement, SELECT1_RESULTSET));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
      assertTrue(resultSet.next());
      assertEquals(1L, resultSet.getLong(0));
      assertFalse(resultSet.next());
    }
  }

  @Test
  public void testStreamWaitTimeout() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    // Add a wait time to the mock server. Note that the test won't actually wait 100ms, as it uses
    // a 1ns time out.
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(100, 0));
    // Create a custom call configuration that uses a 1 nanosecond stream timeout value. This will
    // always time out, as a call to the mock server will always take more than 1 nanosecond.
    CallContextConfigurator configurator =
        new CallContextConfigurator() {
          @Override
          public <ReqT, RespT> ApiCallContext configure(
              ApiCallContext context, ReqT request, MethodDescriptor<ReqT, RespT> method) {
            return context.withStreamWaitTimeout(Duration.ofNanos(1L));
          }
        };
    Context context =
        Context.current().withValue(SpannerOptions.CALL_CONTEXT_CONFIGURATOR_KEY, configurator);
    context.run(
        () -> {
          try (ResultSet resultSet = client.singleUse().executeQuery(SELECT1)) {
            SpannerException exception = assertThrows(SpannerException.class, resultSet::next);
            assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());
            assertTrue(
                exception.getMessage(), exception.getMessage().contains("stream wait timeout"));
          }
        });
  }

  @Test
  public void testZeroStreamWaitTimeout() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    // Create a custom call configuration that sets the stream timeout to zero.
    // This should disable the timeout.
    CallContextConfigurator configurator =
        new CallContextConfigurator() {
          @Override
          public <ReqT, RespT> ApiCallContext configure(
              ApiCallContext context, ReqT request, MethodDescriptor<ReqT, RespT> method) {
            return context.withStreamWaitTimeout(Duration.ZERO);
          }
        };
    Context context =
        Context.current().withValue(SpannerOptions.CALL_CONTEXT_CONFIGURATOR_KEY, configurator);
    context.run(
        () -> {
          try (ResultSet resultSet = client.singleUse().executeQuery(SELECT1)) {
            // A zero timeout should not cause a timeout, and instead be ignored.
            assertTrue(resultSet.next());
            assertFalse(resultSet.next());
          }
        });
  }

  @Test
  public void testRetryOnResourceExhausted() {
    final RetrySettings retrySettings =
        RetrySettings.newBuilder()
            .setInitialRpcTimeout(Duration.ofSeconds(60L))
            .setMaxRpcTimeout(Duration.ofSeconds(60L))
            .setTotalTimeout(Duration.ofSeconds(60L))
            .setRpcTimeoutMultiplier(1.0d)
            .setInitialRetryDelay(Duration.ZERO)
            .setMaxRetryDelay(Duration.ZERO)
            .setMaxAttempts(100)
            .build();
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    RetryInfo retryInfo =
        RetryInfo.newBuilder()
            .setRetryDelay(
                com.google.protobuf.Duration.newBuilder()
                    .setNanos((int) Duration.ofMillis(1).toNanos())
                    .build())
            .build();
    Metadata.Key<RetryInfo> key =
        Metadata.Key.of(
            retryInfo.getDescriptorForType().getFullName() + Metadata.BINARY_HEADER_SUFFIX,
            ProtoLiteUtils.metadataMarshaller(retryInfo));
    Metadata trailers = new Metadata();
    trailers.put(key, retryInfo);
    builder
        .getSpannerStubSettingsBuilder()
        .executeStreamingSqlSettings()
        .setRetryableCodes(StatusCode.Code.UNAVAILABLE, StatusCode.Code.RESOURCE_EXHAUSTED)
        .setRetrySettings(retrySettings);

    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      final int expectedRowCount = 5;
      RandomResultSetGenerator generator = new RandomResultSetGenerator(expectedRowCount);
      Statement statement = Statement.of("select * from random_table");
      mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));

      for (int errorIndex = 0; errorIndex < expectedRowCount - 1; errorIndex++) {
        for (boolean withRetryInfo : new boolean[] {false, true}) {
          // RESOURCE_EXHAUSTED errors with and without retry-info should be retried.
          StatusRuntimeException exception =
              Status.RESOURCE_EXHAUSTED.asRuntimeException(withRetryInfo ? trailers : null);
          mockSpanner.setExecuteStreamingSqlExecutionTime(
              SimulatedExecutionTime.ofStreamException(exception, errorIndex));
          try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
            //noinspection StatementWithEmptyBody
            while (resultSet.next()) {}
          }
          assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
          if (errorIndex == 0) {
            // We should only have two requests without a resume token, as the error occurred before
            // any resume token could be returned.
            assertEquals(
                2,
                mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
                    .filter(request -> request.getResumeToken().isEmpty())
                    .count());
          } else {
            final int expectedResumeToken = errorIndex;
            // Check that we have one request with a resume token that corresponds with the place in
            // the stream where the error happened.
            assertEquals(
                1,
                mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
                    .filter(
                        request ->
                            request
                                .getResumeToken()
                                .equals(
                                    ByteString.copyFromUtf8(
                                        String.format("%09d", expectedResumeToken))))
                    .count());
          }
          mockSpanner.clearRequests();
        }
      }
    }
  }

  static void assertAsString(String expected, ResultSet resultSet, int col) {
    assertEquals(expected, resultSet.getValue(col).getAsString());
    assertEquals(ImmutableList.of(expected), resultSet.getValue(col).getAsStringList());
  }

  static void assertAsString(ImmutableList<String> expected, ResultSet resultSet, int col) {
    assertEquals(expected, resultSet.getValue(col).getAsStringList());
    assertEquals(
        expected.stream().collect(Collectors.joining(",", "[", "]")),
        resultSet.getValue(col).getAsString());
  }
}
