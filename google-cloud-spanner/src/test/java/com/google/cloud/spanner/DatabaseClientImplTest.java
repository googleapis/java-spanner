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
import static com.google.cloud.spanner.SpannerApiFutures.get;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
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
import com.google.cloud.spanner.SpannerException.ResourceNotFoundException;
import com.google.cloud.spanner.SpannerOptions.SpannerCallContextTimeoutConfigurator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.AbstractMessage;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.RequestOptions.Priority;
import io.grpc.Context;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
        StatementResult.query(READ_ONE_KEY_VALUE_STATEMENT, READ_ONE_KEY_VALUE_RESULTSET));
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
  public void writeAtLeastOnceWithTagOptions() {
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
}
