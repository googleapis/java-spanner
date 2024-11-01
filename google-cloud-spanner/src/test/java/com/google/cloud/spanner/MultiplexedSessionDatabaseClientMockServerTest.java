/*
 * Copyright 2024 Google LLC
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
import static com.google.cloud.spanner.MockSpannerTestUtil.UPDATE_COUNT;
import static com.google.cloud.spanner.MockSpannerTestUtil.UPDATE_STATEMENT;
import static com.google.cloud.spanner.SpannerApiFutures.get;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.NoCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionStep;
import com.google.cloud.spanner.AsyncTransactionManager.CommitTimestampFuture;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.RequestOptions.Priority;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.Transaction;
import io.grpc.Status;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MultiplexedSessionDatabaseClientMockServerTest extends AbstractMockServerTest {
  private static final Statement STATEMENT = Statement.of("select * from random");

  @BeforeClass
  public static void setupResults() {
    mockSpanner.putStatementResults(
        StatementResult.query(STATEMENT, new RandomResultSetGenerator(1).generate()));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));
  }

  @Before
  public void createSpannerInstance() {
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setUseMultiplexedSession(true)
                    .setUseMultiplexedSessionBlindWrite(true)
                    .setUseMultiplexedSessionForRW(true)
                    // Set the maintainer to loop once every 1ms
                    .setMultiplexedSessionMaintenanceLoopFrequency(Duration.ofMillis(1L))
                    // Set multiplexed sessions to be replaced once every 1ms
                    .setMultiplexedSessionMaintenanceDuration(org.threeten.bp.Duration.ofMillis(1L))
                    .setFailOnSessionLeak()
                    .build())
            .build()
            .getService();
  }

  @Test
  public void testMultiUseReadOnlyTransactionUsesSameSession() {
    // Execute two queries using the same transaction. Both queries should use the same
    // session, also when the maintainer has executed in the meantime.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    try (ReadOnlyTransaction transaction = client.readOnlyTransaction()) {
      try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
        //noinspection StatementWithEmptyBody
        while (resultSet.next()) {
          // ignore
        }
      }

      // Wait until the maintainer has replaced the current session.
      waitForSessionToBeReplaced(client);

      try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
        //noinspection StatementWithEmptyBody
        while (resultSet.next()) {
          // ignore
        }
      }
    }
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(2, requests.size());
    assertEquals(requests.get(0).getSession(), requests.get(1).getSession());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testNewTransactionUsesNewSession() {
    // Execute a single-use read-only transactions, then wait for the maintainer to replace the
    // current session, and then run another single-use read-only transaction. The two transactions
    // should use two different sessions.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      //noinspection StatementWithEmptyBody
      while (resultSet.next()) {
        // ignore
      }
    }

    // Wait until the maintainer has replaced the current session.
    waitForSessionToBeReplaced(client);

    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      //noinspection StatementWithEmptyBody
      while (resultSet.next()) {
        // ignore
      }
    }

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(2, requests.size());
    assertNotEquals(requests.get(0).getSession(), requests.get(1).getSession());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testMaintainerMaintainsMultipleClients() {
    // Verify that the single-threaded shared executor that is used by the multiplexed client
    // maintains and replaces sessions from multiple clients.
    DatabaseClientImpl client1 =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of("p", "i", "d" + UUID.randomUUID()));
    DatabaseClientImpl client2 =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of("p", "i", "d" + UUID.randomUUID()));

    for (DatabaseClientImpl client : ImmutableList.of(client1, client2)) {
      try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
        //noinspection StatementWithEmptyBody
        while (resultSet.next()) {
          // ignore
        }
      }
      // Wait until the maintainer has replaced the current session.
      waitForSessionToBeReplaced(client);
      try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
        //noinspection StatementWithEmptyBody
        while (resultSet.next()) {
          // ignore
        }
      }
    }

    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(4, requests.size());
    // Put all session IDs in a Set to verify that they were all different.
    Set<String> sessionIds =
        requests.stream().map(ExecuteSqlRequest::getSession).collect(Collectors.toSet());
    assertEquals(4, sessionIds.size());

    for (DatabaseClientImpl client : ImmutableList.of(client1, client2)) {
      assertNotNull(client.multiplexedSessionDatabaseClient);
      assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
      assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
    }
  }

  @Test
  public void testUnimplementedErrorOnCreation_fallsBackToRegularSessions() {
    mockSpanner.setCreateSessionExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription("Multiplexed sessions are not implemented")
                .asRuntimeException()));
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Get the current session reference. This will block until the CreateSession RPC has failed.
    assertNotNull(client.multiplexedSessionDatabaseClient);
    SpannerException spannerException =
        assertThrows(
            SpannerException.class,
            client.multiplexedSessionDatabaseClient::getCurrentSessionReference);
    assertEquals(ErrorCode.UNIMPLEMENTED, spannerException.getErrorCode());
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      //noinspection StatementWithEmptyBody
      while (resultSet.next()) {
        // ignore
      }
    }
    // Verify that we received one ExecuteSqlRequest, and that it used a regular session.
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);

    Session session = mockSpanner.getSession(requests.get(0).getSession());
    assertNotNull(session);
    assertFalse(session.getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(0L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(0L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void
      testUnimplementedErrorOnCreation_firstReceivesError_secondFallsBackToRegularSessions() {
    mockSpanner.setCreateSessionExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription("Multiplexed sessions are not implemented")
                .asRuntimeException()));
    // Freeze the mock server to ensure that the CreateSession RPC does not return an error or any
    // other result just yet.
    mockSpanner.freeze();
    // Get a database client using multiplexed sessions. The CreateSession RPC will be blocked as
    // long as the mock server is frozen.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Try to execute a query. This is all non-blocking until the call to ResultSet#next().
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      // Unfreeze the mock server to get the error from the backend. This query will then fail.
      mockSpanner.unfreeze();
      SpannerException spannerException = assertThrows(SpannerException.class, resultSet::next);
      assertEquals(ErrorCode.UNIMPLEMENTED, spannerException.getErrorCode());
    }
    // The next query will fall back to regular sessions and succeed.
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      //noinspection StatementWithEmptyBody
      while (resultSet.next()) {
        // ignore
      }
    }
    // Verify that we received one ExecuteSqlRequest, and that it used a regular session.
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);

    Session session = mockSpanner.getSession(requests.get(0).getSession());
    assertNotNull(session);
    assertFalse(session.getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(0L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(0L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testMaintainerInvalidatesMultiplexedSessionClientIfUnimplemented() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // The first query should succeed.
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      //noinspection StatementWithEmptyBody
      while (resultSet.next()) {
        // ignore
      }
    }
    // Now ensure that CreateSession returns UNIMPLEMENTED. This error should be recognized by the
    // maintainer and invalidate the MultiplexedSessionDatabaseClient. New queries will fall back to
    // regular sessions.
    mockSpanner.setCreateSessionExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription("Multiplexed sessions are not implemented")
                .asRuntimeException()));
    // Wait until the client sees that MultiplexedSessions are not supported.
    assertNotNull(client.multiplexedSessionDatabaseClient);
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (client.multiplexedSessionDatabaseClient.isMultiplexedSessionsSupported()
        && stopwatch.elapsed().compareTo(Duration.ofSeconds(5)) < 0) {
      Thread.yield();
    }
    // Queries should fall back to regular sessions.
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      //noinspection StatementWithEmptyBody
      while (resultSet.next()) {
        // ignore
      }
    }
    // Verify that we received two ExecuteSqlRequests, and that the first one used a multiplexed
    // session, and that the second used a regular session.
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);

    Session session1 = mockSpanner.getSession(requests.get(0).getSession());
    assertNotNull(session1);
    assertTrue(session1.getMultiplexed());

    Session session2 = mockSpanner.getSession(requests.get(1).getSession());
    assertNotNull(session2);
    assertFalse(session2.getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testWriteAtLeastOnceAborted() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
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
    for (CommitRequest request : commitRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testWriteAtLeastOnce() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    Timestamp timestamp =
        client.writeAtLeastOnce(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getSingleUseTransaction());
    assertTrue(commit.getSingleUseTransaction().hasReadWrite());
    assertFalse(commit.getSingleUseTransaction().getExcludeTxnFromChangeStreams());
    assertNotNull(commit.getRequestOptions());
    assertEquals(Priority.PRIORITY_UNSPECIFIED, commit.getRequestOptions().getPriority());
    assertTrue(mockSpanner.getSession(commit.getSession()).getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testWriteAtLeastOnceWithCommitStats() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
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
    assertFalse(commit.getSingleUseTransaction().getExcludeTxnFromChangeStreams());
    assertNotNull(commit.getRequestOptions());
    assertEquals(Priority.PRIORITY_UNSPECIFIED, commit.getRequestOptions().getPriority());
    assertTrue(mockSpanner.getSession(commit.getSession()).getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testWriteAtLeastOnceWithOptions() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    client.writeAtLeastOnceWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.priority(RpcPriority.LOW));

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getSingleUseTransaction());
    assertTrue(commit.getSingleUseTransaction().hasReadWrite());
    assertFalse(commit.getSingleUseTransaction().getExcludeTxnFromChangeStreams());
    assertNotNull(commit.getRequestOptions());
    assertEquals(Priority.PRIORITY_LOW, commit.getRequestOptions().getPriority());
    assertTrue(mockSpanner.getSession(commit.getSession()).getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testWriteAtLeastOnceWithTagOptions() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    client.writeAtLeastOnceWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.tag("app=spanner,env=test"));

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getSingleUseTransaction());
    assertTrue(commit.getSingleUseTransaction().hasReadWrite());
    assertFalse(commit.getSingleUseTransaction().getExcludeTxnFromChangeStreams());
    assertNotNull(commit.getRequestOptions());
    assertThat(commit.getRequestOptions().getTransactionTag()).isEqualTo("app=spanner,env=test");
    assertThat(commit.getRequestOptions().getRequestTag()).isEmpty();
    assertTrue(mockSpanner.getSession(commit.getSession()).getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testWriteAtLeastOnceWithExcludeTxnFromChangeStreams() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    client.writeAtLeastOnceWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.excludeTxnFromChangeStreams());

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getSingleUseTransaction());
    assertTrue(commit.getSingleUseTransaction().hasReadWrite());
    assertTrue(commit.getSingleUseTransaction().getExcludeTxnFromChangeStreams());
    assertTrue(mockSpanner.getSession(commit.getSession()).getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testReadWriteTransactionUsingTransactionRunner() {
    // Queries executed within a R/W transaction via TransactionRunner should use a multiplexed
    // session.
    // During a retry (due to an ABORTED error), the transaction should use the same multiplexed
    // session as before, assuming the maintainer hasn't run in the meantime.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
                //noinspection StatementWithEmptyBody
                while (resultSet.next()) {
                  // ignore
                }
              }
              return null;
            });

    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(2, executeSqlRequests.size());
    assertEquals(executeSqlRequests.get(0).getSession(), executeSqlRequests.get(1).getSession());

    // Verify the requests are executed using multiplexed sessions
    for (ExecuteSqlRequest request : executeSqlRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testReadWriteTransactionUsingTransactionManager() {
    // Queries executed within a R/W transaction via TransactionManager should use a multiplexed
    // session.
    // During a retry (due to an ABORTED error), the transaction should use the same multiplexed
    // session as before, assuming the maintainer hasn't run in the meantime.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
            //noinspection StatementWithEmptyBody
            while (resultSet.next()) {
              // ignore
            }
          }
          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }

    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(2, executeSqlRequests.size());
    assertEquals(executeSqlRequests.get(0).getSession(), executeSqlRequests.get(1).getSession());

    // Verify the requests are executed using multiplexed sessions
    for (ExecuteSqlRequest request : executeSqlRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testMutationUsingWrite() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
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

    List<BeginTransactionRequest> beginTransactionRequests =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertEquals(2, beginTransactionRequests.size());
    for (BeginTransactionRequest request : beginTransactionRequests) {
      // Verify that mutation key is set for mutations-only case in read-write transaction.
      assertTrue(request.hasMutationKey());
    }

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(2, commitRequests.size());
    for (CommitRequest request : commitRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
      // Verify that the precommit token is set in CommitRequest
      assertTrue(request.hasPrecommitToken());
      assertEquals(
          ByteString.copyFromUtf8("TransactionPrecommitToken"),
          request.getPrecommitToken().getPrecommitToken());
    }

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testMutationUsingWriteWithOptions() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    CommitResponse response =
        client.writeWithOptions(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
            Options.tag("app=spanner,env=test"));
    assertNotNull(response);
    assertNotNull(response.getCommitTimestamp());

    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(1L, commitRequests.size());
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getRequestOptions());
    assertEquals("app=spanner,env=test", commit.getRequestOptions().getTransactionTag());
    assertTrue(mockSpanner.getSession(commit.getSession()).getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testReadWriteTransactionUsingAsyncTransactionManager() throws Exception {
    // Updates executed within a R/W transaction via AsyncTransactionManager should use a
    // multiplexed session.
    // During a retry (due to an ABORTED error), the transaction should use the same multiplexed
    // session as before, assuming the maintainer hasn't run in the meantime.
    final AtomicInteger attempt = new AtomicInteger();
    CountDownLatch abortedLatch = new CountDownLatch(1);
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture transactionContextFuture = manager.beginAsync();
      while (true) {
        try {
          attempt.incrementAndGet();
          AsyncTransactionStep<Void, Long> updateCount =
              transactionContextFuture.then(
                  (transaction, ignored) -> transaction.executeUpdateAsync(UPDATE_STATEMENT),
                  MoreExecutors.directExecutor());
          updateCount.then(
              (transaction, ignored) -> {
                if (attempt.get() == 1) {
                  mockSpanner.abortTransaction(transaction);
                  abortedLatch.countDown();
                }
                return ApiFutures.immediateFuture(null);
              },
              MoreExecutors.directExecutor());
          abortedLatch.await(10L, TimeUnit.SECONDS);
          CommitTimestampFuture commitTimestamp = updateCount.commitAsync();
          assertEquals(UPDATE_COUNT, updateCount.get().longValue());
          assertNotNull(commitTimestamp.get());
          assertEquals(2L, attempt.get());
          break;
        } catch (AbortedException e) {
          transactionContextFuture = manager.resetForRetryAsync();
        }
      }
    }

    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(2, executeSqlRequests.size());
    assertEquals(executeSqlRequests.get(0).getSession(), executeSqlRequests.get(1).getSession());

    // Verify the requests are executed using multiplexed sessions
    for (ExecuteSqlRequest request : executeSqlRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testReadWriteTransactionUsingAsyncRunner() throws Exception {
    // Updates executed within a R/W transaction via AsyncRunner should use a multiplexed
    // session.
    // During a retry (due to an ABORTED error), the transaction should use the same multiplexed
    // session as before, assuming the maintainer hasn't run in the meantime.
    final AtomicInteger attempt = new AtomicInteger();
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    AsyncRunner runner = client.runAsync();
    ApiFuture<Long> updateCount =
        runner.runAsync(
            txn -> {
              ApiFuture<Long> updateCount1 = txn.executeUpdateAsync(UPDATE_STATEMENT);
              if (attempt.incrementAndGet() == 1) {
                mockSpanner.abortTransaction(txn);
              }
              return updateCount1;
            },
            MoreExecutors.directExecutor());
    assertEquals(UPDATE_COUNT, updateCount.get().longValue());
    assertEquals(2L, attempt.get());

    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(2L, executeSqlRequests.size());
    assertEquals(executeSqlRequests.get(0).getSession(), executeSqlRequests.get(1).getSession());

    // Verify the requests are executed using multiplexed sessions
    for (ExecuteSqlRequest request : executeSqlRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testAsyncRunnerIsNonBlockingWithMultiplexedSession() throws Exception {
    mockSpanner.freeze();
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    AsyncRunner runner = client.runAsync();
    ApiFuture<Void> res =
        runner.runAsync(
            txn -> {
              txn.executeUpdateAsync(UPDATE_STATEMENT);
              return ApiFutures.immediateFuture(null);
            },
            MoreExecutors.directExecutor());
    ApiFuture<Timestamp> ts = runner.getCommitTimestamp();
    mockSpanner.unfreeze();
    assertThat(res.get()).isNull();
    assertThat(ts.get()).isNotNull();

    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(1L, executeSqlRequests.size());

    // Verify the requests are executed using multiplexed sessions
    for (ExecuteSqlRequest request : executeSqlRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testAbortedReadWriteTxnUsesPreviousTxnIdOnRetryWithInlineBegin() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    TransactionRunner runner = client.readWriteTransaction();
    AtomicReference<ByteString> validTransactionId = new AtomicReference<>();
    runner.run(
        transaction -> {
          try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
            while (resultSet.next()) {}
          }

          TransactionContextImpl impl = (TransactionContextImpl) transaction;
          if (validTransactionId.get() == null) {
            // Track the first not-null transactionId. This transaction gets ABORTED during commit
            // operation and gets retried.
            validTransactionId.set(impl.transactionId);
          }
          return null;
        });

    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(2, executeSqlRequests.size());

    // Verify the requests are executed using multiplexed sessions
    for (ExecuteSqlRequest request : executeSqlRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    // Verify that the first request uses inline begin, and the previous transaction ID is set to
    // ByteString.EMPTY
    assertTrue(executeSqlRequests.get(0).hasTransaction());
    assertTrue(executeSqlRequests.get(0).getTransaction().hasBegin());
    assertTrue(executeSqlRequests.get(0).getTransaction().getBegin().hasReadWrite());
    assertNotNull(
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertEquals(
        ByteString.EMPTY,
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());

    // Verify that the second request uses inline begin, and the previous transaction ID is set
    // appropriately
    assertTrue(executeSqlRequests.get(1).hasTransaction());
    assertTrue(executeSqlRequests.get(1).getTransaction().hasBegin());
    assertTrue(executeSqlRequests.get(1).getTransaction().getBegin().hasReadWrite());
    assertNotNull(
        executeSqlRequests
            .get(1)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertNotEquals(
        ByteString.EMPTY,
        executeSqlRequests
            .get(1)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertEquals(
        validTransactionId.get(),
        executeSqlRequests
            .get(1)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
  }

  @Test
  public void testAbortedReadWriteTxnUsesPreviousTxnIdOnRetryWithExplicitBegin() {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    TransactionRunner runner = client.readWriteTransaction();
    AtomicReference<ByteString> validTransactionId = new AtomicReference<>();
    Long updateCount =
        runner.run(
            transaction -> {
              // This update statement carries the BeginTransaction, but fails. This will
              // cause the entire transaction to be retried with an explicit
              // BeginTransaction RPC to ensure all statements in the transaction are
              // actually executed against the same transaction.
              TransactionContextImpl impl = (TransactionContextImpl) transaction;
              if (validTransactionId.get() == null) {
                // Track the first not-null transactionId. This transaction gets ABORTED during
                // commit operation and gets retried.
                validTransactionId.set(impl.transactionId);
              }
              SpannerException e =
                  assertThrows(
                      SpannerException.class,
                      () -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT));
              assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
              return transaction.executeUpdate(UPDATE_STATEMENT);
            });

    assertThat(updateCount).isEqualTo(1L);
    List<BeginTransactionRequest> beginTransactionRequests =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertEquals(2, beginTransactionRequests.size());

    // Verify the requests are executed using multiplexed sessions
    for (BeginTransactionRequest request : beginTransactionRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
    }

    // Verify that explicit begin transaction is called during retry, and the previous transaction
    // ID is set to ByteString.EMPTY
    assertTrue(beginTransactionRequests.get(0).hasOptions());
    assertTrue(beginTransactionRequests.get(0).getOptions().hasReadWrite());
    assertNotNull(
        beginTransactionRequests
            .get(0)
            .getOptions()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertEquals(
        ByteString.EMPTY,
        beginTransactionRequests
            .get(0)
            .getOptions()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());

    // The previous transaction with id (txn1) fails during commit operation with ABORTED error.
    // Verify that explicit begin transaction is called during retry, and the previous transaction
    // ID is not ByteString.EMPTY (should be set to txn1)
    assertTrue(beginTransactionRequests.get(1).hasOptions());
    assertTrue(beginTransactionRequests.get(1).getOptions().hasReadWrite());
    assertNotNull(
        beginTransactionRequests
            .get(1)
            .getOptions()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertNotEquals(
        ByteString.EMPTY,
        beginTransactionRequests
            .get(1)
            .getOptions()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertEquals(
        validTransactionId.get(),
        beginTransactionRequests
            .get(1)
            .getOptions()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
  }

  @Test
  public void testPrecommitTokenForResultSet() {
    // This test verifies that the precommit token received from the ResultSet is properly tracked
    // and set in the CommitRequest.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    Long count =
        client
            .readWriteTransaction()
            .run(
                transaction -> {
                  long res = transaction.executeUpdate(UPDATE_STATEMENT);

                  // Verify that the latest precommit token is tracked in the transaction context.
                  TransactionContextImpl impl = (TransactionContextImpl) transaction;
                  assertNotNull(impl.getLatestPrecommitToken());
                  assertEquals(
                      ByteString.copyFromUtf8("ResultSetPrecommitToken"),
                      impl.getLatestPrecommitToken().getPrecommitToken());
                  return res;
                });

    assertNotNull(count);
    assertEquals(1, count.longValue());

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(1, commitRequests.size());
    assertTrue(mockSpanner.getSession(commitRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(commitRequests.get(0).getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("ResultSetPrecommitToken"),
        commitRequests.get(0).getPrecommitToken().getPrecommitToken());
  }

  @Test
  public void testPrecommitTokenForExecuteBatchDmlResponse() {
    // This test verifies that the precommit token received from the ExecuteBatchDmlResponse is
    // properly tracked and set in the CommitRequest.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    long[] count =
        client
            .readWriteTransaction()
            .run(
                transaction -> {
                  long[] res = transaction.batchUpdate(Lists.newArrayList(UPDATE_STATEMENT));

                  // Verify that the latest precommit token is tracked in the transaction context.
                  TransactionContextImpl impl = (TransactionContextImpl) transaction;
                  assertNotNull(impl.getLatestPrecommitToken());
                  assertEquals(
                      ByteString.copyFromUtf8("ExecuteBatchDmlResponsePrecommitToken"),
                      impl.getLatestPrecommitToken().getPrecommitToken());
                  return res;
                });

    assertNotNull(count);
    assertEquals(1, count.length);

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(1, commitRequests.size());
    assertTrue(mockSpanner.getSession(commitRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(commitRequests.get(0).getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("ExecuteBatchDmlResponsePrecommitToken"),
        commitRequests.get(0).getPrecommitToken().getPrecommitToken());
  }

  @Test
  public void testPrecommitTokenForPartialResultSet() {
    // This test verifies that the precommit token received from the PartialResultSet is properly
    // tracked and set in the CommitRequest.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              ResultSet resultSet = transaction.executeQuery(STATEMENT);
              //noinspection StatementWithEmptyBody
              while (resultSet.next()) {
                // ignore
              }

              // Verify that the latest precommit token is tracked in the transaction context.
              TransactionContextImpl impl = (TransactionContextImpl) transaction;
              assertNotNull(impl.getLatestPrecommitToken());
              assertEquals(
                  ByteString.copyFromUtf8("PartialResultSetPrecommitToken"),
                  impl.getLatestPrecommitToken().getPrecommitToken());
              return null;
            });

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(1, commitRequests.size());
    assertTrue(mockSpanner.getSession(commitRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(commitRequests.get(0).getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("PartialResultSetPrecommitToken"),
        commitRequests.get(0).getPrecommitToken().getPrecommitToken());
  }

  @Test
  public void testTxnTracksPrecommitTokenWithLatestSeqNo() {
    // This test ensures that the read-write transaction tracks the precommit token with the
    // highest sequence number and sets it in the CommitRequest.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              // Returns a ResultSet containing the precommit token (ResultSetPrecommitToken)
              transaction.executeUpdate(UPDATE_STATEMENT);

              // Returns a PartialResultSet containing the precommit token
              // (PartialResultSetPrecommitToken)
              ResultSet resultSet = transaction.executeQuery(STATEMENT);
              //noinspection StatementWithEmptyBody
              while (resultSet.next()) {
                // ignore
              }

              // Returns an ExecuteBatchDmlResponse containing the precommit token
              // (ExecuteBatchDmlResponsePrecommitToken).
              // Since this is the last request received by the mock Spanner, it should be the most
              // recent precommit token tracked by the transaction context.
              transaction.batchUpdate(Lists.newArrayList(UPDATE_STATEMENT));

              // Verify that the latest precommit token with highest sequence number is tracked in
              // the transaction context.
              TransactionContextImpl impl = (TransactionContextImpl) transaction;
              assertNotNull(impl.getLatestPrecommitToken());
              assertEquals(
                  ByteString.copyFromUtf8("ExecuteBatchDmlResponsePrecommitToken"),
                  impl.getLatestPrecommitToken().getPrecommitToken());
              return null;
            });

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(1, commitRequests.size());
    assertTrue(mockSpanner.getSession(commitRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(commitRequests.get(0).getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("ExecuteBatchDmlResponsePrecommitToken"),
        commitRequests.get(0).getPrecommitToken().getPrecommitToken());
  }

  @Test
  public void testPrecommitTokenForTransactionResponse() {
    // This test verifies that
    // 1. A random mutation from the list is set in BeginTransactionRequest.
    // 2. The precommit token from the Transaction response is correctly tracked
    // and applied in the CommitRequest. The Transaction response includes a precommit token
    // only when the read-write transaction consists solely of mutations.

    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              Mutation mutation =
                  Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build();
              transaction.buffer(mutation);
              return null;
            });

    // Verify that for mutation only case, a mutation key is set in BeginTransactionRequest.
    List<BeginTransactionRequest> beginTxnRequest =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertEquals(1, beginTxnRequest.size());
    assertTrue(mockSpanner.getSession(beginTxnRequest.get(0).getSession()).getMultiplexed());
    assertTrue(beginTxnRequest.get(0).hasMutationKey());
    assertTrue(beginTxnRequest.get(0).getMutationKey().hasInsert());

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(1L, commitRequests.size());
    assertTrue(mockSpanner.getSession(commitRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(commitRequests.get(0).getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("TransactionPrecommitToken"),
        commitRequests.get(0).getPrecommitToken().getPrecommitToken());
  }

  @Test
  public void testMutationOnlyCaseAborted() {
    // This test verifies that in the case of mutations-only, when a transaction is retried after an
    // ABORT, the mutation key is correctly set in the BeginTransaction request.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              Mutation mutation =
                  Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build();
              transaction.buffer(mutation);
              return null;
            });

    // Verify that for mutation only case, a mutation key is set in BeginTransactionRequest.
    List<BeginTransactionRequest> beginTransactionRequests =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertEquals(2, beginTransactionRequests.size());
    // Verify the requests are executed using multiplexed sessions
    for (BeginTransactionRequest request : beginTransactionRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
      assertTrue(request.hasMutationKey());
      assertTrue(request.getMutationKey().hasInsert());
    }

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertEquals(2L, commitRequests.size());
    for (CommitRequest request : commitRequests) {
      assertTrue(mockSpanner.getSession(request.getSession()).getMultiplexed());
      assertNotNull(request.getPrecommitToken());
      assertEquals(
          ByteString.copyFromUtf8("TransactionPrecommitToken"),
          request.getPrecommitToken().getPrecommitToken());
    }
  }

  @Test
  public void testMutationOnlyUsingTransactionManager() {
    // Test verifies mutation-only case within a R/W transaction via TransactionManager.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          Mutation mutation =
              Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build();
          transaction.buffer(mutation);
          manager.commit();
          assertNotNull(manager.getCommitTimestamp());
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }

    // Verify that for mutation only case, a mutation key is set in BeginTransactionRequest.
    List<BeginTransactionRequest> beginTransactionRequests =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertThat(beginTransactionRequests).hasSize(1);
    BeginTransactionRequest beginTransaction = beginTransactionRequests.get(0);
    assertTrue(mockSpanner.getSession(beginTransaction.getSession()).getMultiplexed());
    assertTrue(beginTransaction.hasMutationKey());
    assertTrue(beginTransaction.getMutationKey().hasInsert());

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commitRequest = commitRequests.get(0);
    assertNotNull(commitRequest.getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("TransactionPrecommitToken"),
        commitRequest.getPrecommitToken().getPrecommitToken());
  }

  @Test
  public void testMutationOnlyUsingAsyncRunner() {
    // Test verifies mutation-only case within a R/W transaction via AsyncRunner.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    AsyncRunner runner = client.runAsync();
    get(
        runner.runAsync(
            txn -> {
              txn.buffer(Mutation.delete("TEST", KeySet.all()));
              return ApiFutures.immediateFuture(null);
            },
            MoreExecutors.directExecutor()));

    // Verify that the mutation key is set in BeginTransactionRequest
    List<BeginTransactionRequest> beginTransactions =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertThat(beginTransactions).hasSize(1);
    BeginTransactionRequest beginTransaction = beginTransactions.get(0);
    assertTrue(beginTransaction.hasMutationKey());
    assertTrue(beginTransaction.getMutationKey().hasDelete());

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> commitRequests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(commitRequests).hasSize(1);
    CommitRequest commitRequest = commitRequests.get(0);
    assertNotNull(commitRequest.getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("TransactionPrecommitToken"),
        commitRequest.getPrecommitToken().getPrecommitToken());
  }

  @Test
  public void testMutationOnlyUsingAsyncTransactionManager() {
    // Test verifies mutation-only case within a R/W transaction via AsyncTransactionManager.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture transaction = manager.beginAsync();
      get(
          transaction
              .then(
                  (txn, input) -> {
                    txn.buffer(Mutation.delete("TEST", KeySet.all()));
                    return ApiFutures.immediateFuture(null);
                  },
                  MoreExecutors.directExecutor())
              .commitAsync());
    }

    // Verify that the mutation key is set in BeginTransactionRequest
    List<BeginTransactionRequest> beginTransactions =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertThat(beginTransactions).hasSize(1);
    BeginTransactionRequest beginTransaction = beginTransactions.get(0);
    assertTrue(beginTransaction.hasMutationKey());
    assertTrue(beginTransaction.getMutationKey().hasDelete());

    // Verify that the latest precommit token is set in the CommitRequest
    List<CommitRequest> requests = mockSpanner.getRequestsOfType(CommitRequest.class);
    assertThat(requests).hasSize(1);
    CommitRequest request = requests.get(0);
    assertNotNull(request.getPrecommitToken());
    assertEquals(
        ByteString.copyFromUtf8("TransactionPrecommitToken"),
        request.getPrecommitToken().getPrecommitToken());
  }

  // Tests the behavior of the server-side kill switch for read-write multiplexed sessions..
  @Test
  public void testInitialBeginTransactionWithRW_receivesUnimplemented_fallsBackToRegularSession() {
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription(
                    "Transaction type read_write not supported with multiplexed sessions")
                .asRuntimeException()));
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    assertNotNull(client.multiplexedSessionDatabaseClient);

    // Wait until the client sees that MultiplexedSessions are not supported for read-write.
    // Get the begin transaction reference. This will block until the BeginTransaction RPC with
    // read-write has failed.
    SpannerException spannerException =
        assertThrows(
            SpannerException.class,
            client.multiplexedSessionDatabaseClient::getReadWriteBeginTransactionReference);
    assertEquals(ErrorCode.UNIMPLEMENTED, spannerException.getErrorCode());
    assertTrue(client.multiplexedSessionDatabaseClient.unimplementedForRW.get());

    // read-write transaction should fallback to regular sessions
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
                //noinspection StatementWithEmptyBody
                while (resultSet.next()) {
                  // ignore
                }
              }
              return null;
            });

    // Verify that we received one ExecuteSqlRequest, and it uses a regular session due to fallback.
    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(1, executeSqlRequests.size());
    // Verify the requests are not executed using multiplexed sessions
    Session session2 = mockSpanner.getSession(executeSqlRequests.get(0).getSession());
    assertNotNull(session2);
    assertFalse(session2.getMultiplexed());
  }

  @Test
  public void
      testReadWriteUnimplementedErrorDuringInitialBeginTransactionRPC_firstReceivesError_secondFallsBackToRegularSessions() {
    // This test simulates the following scenario,
    // 1. The server-side flag for RW multiplexed sessions is disabled.
    // 2. Application starts. The initial BeginTransaction RPC during client initialization will
    // fail with UNIMPLEMENTED error.
    // 3. Read-write transaction initialized before the BeginTransaction RPC response will fail with
    // UNIMPLEMENTED error.
    // 4. Read-write transaction initialized after the BeginTransaction RPC response will fallback
    // to regular sessions.
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription(
                    "Transaction type read_write not supported with multiplexed sessions")
                .asRuntimeException()));
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription(
                    "Transaction type read_write not supported with multiplexed sessions")
                .asRuntimeException()));
    // Freeze the mock server to ensure that the BeginTransaction with read-write on multiplexed
    // session RPC does not return an error or any
    // other result just yet.
    mockSpanner.freeze();
    // Get a database client using multiplexed sessions. The BeginTransaction RPC to validation
    // read-write on multiplexed session will be blocked as
    // long as the mock server is frozen.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    // Get the runner so that the read-write transaction is executed via multiplexed session.
    TransactionRunner runner = client.readWriteTransaction();

    // Unfreeze the mock server to get the error from the backend. The above read-write transaction
    // will then fail.
    mockSpanner.unfreeze();

    SpannerException e =
        assertThrows(
            SpannerException.class,
            () ->
                runner.run(
                    transaction -> {
                      ResultSet resultSet = transaction.executeQuery(STATEMENT);
                      //noinspection StatementWithEmptyBody
                      while (resultSet.next()) {
                        // ignore
                      }
                      return null;
                    }));
    assertEquals(ErrorCode.UNIMPLEMENTED, e.getErrorCode());

    // Wait until the client sees that MultiplexedSessions are not supported for read-write.
    assertNotNull(client.multiplexedSessionDatabaseClient);
    SpannerException spannerException =
        assertThrows(
            SpannerException.class,
            client.multiplexedSessionDatabaseClient::getReadWriteBeginTransactionReference);
    assertEquals(ErrorCode.UNIMPLEMENTED, spannerException.getErrorCode());
    assertTrue(client.multiplexedSessionDatabaseClient.unimplementedForRW.get());

    // The next read-write transaction will fall back to regular sessions and succeed.
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
                //noinspection StatementWithEmptyBody
                while (resultSet.next()) {
                  // ignore
                }
              }
              return null;
            });

    // Verify that two ExecuteSqlRequests were received: the first using a multiplexed session and
    // the second using a regular session.
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);

    Session session1 = mockSpanner.getSession(requests.get(0).getSession());
    assertNotNull(session1);
    assertTrue(session1.getMultiplexed());

    Session session2 = mockSpanner.getSession(requests.get(1).getSession());
    assertNotNull(session2);
    assertFalse(session2.getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testReadWriteUnimplemented_firstReceivesError_secondFallsBackToRegularSessions() {
    // This test simulates the following scenario,
    // 1. The server side flag for read-write multiplexed session is not disabled. When an
    // application starts, the initial BeginTransaction RPC with read-write will succeed.
    // 2. After time t, the server side flag for read-write multiplexed session is disabled. After
    // this a read-write transaction executed with multiplexed sessions should fail with
    // UNIMPLEMENTED error.
    // 3. All read-write transactions in the application after the initial failure should fallback
    // to using regular sessions.
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription(
                    "Transaction type read_write not supported with multiplexed sessions")
                .asRuntimeException()));

    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    // Wait until the initial BeginTransaction RPC with read-write is complete.
    assertNotNull(client.multiplexedSessionDatabaseClient);
    Transaction txn =
        client.multiplexedSessionDatabaseClient.getReadWriteBeginTransactionReference();
    assertNotNull(txn);
    assertNotNull(txn.getId());
    assertFalse(client.multiplexedSessionDatabaseClient.unimplementedForRW.get());

    SpannerException e =
        assertThrows(
            SpannerException.class,
            () ->
                client
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          ResultSet resultSet = transaction.executeQuery(STATEMENT);
                          //noinspection StatementWithEmptyBody
                          while (resultSet.next()) {
                            // ignore
                          }
                          return null;
                        }));
    assertEquals(ErrorCode.UNIMPLEMENTED, e.getErrorCode());

    // Verify that the previous failed transaction has marked multiplexed session client to be
    // unimplemented for read-write.
    assertTrue(client.multiplexedSessionDatabaseClient.unimplementedForRW.get());

    // The next read-write transaction will fall back to regular sessions and succeed.
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
                //noinspection StatementWithEmptyBody
                while (resultSet.next()) {
                  // ignore
                }
              }
              return null;
            });

    // Verify that two ExecuteSqlRequests were received: the first using a multiplexed session and
    // the second using a regular session.
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);

    Session session1 = mockSpanner.getSession(requests.get(0).getSession());
    assertNotNull(session1);
    assertTrue(session1.getMultiplexed());

    Session session2 = mockSpanner.getSession(requests.get(1).getSession());
    assertNotNull(session2);
    assertFalse(session2.getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(1L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void testOtherUnimplementedError_ReadWriteTransactionStillUsesMultiplexedSession() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNIMPLEMENTED
                .withDescription("Multiplexed sessions are not supported.")
                .asRuntimeException()));

    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    // Wait until the initial BeginTransaction RPC with read-write is complete.
    assertNotNull(client.multiplexedSessionDatabaseClient);
    Transaction txn =
        client.multiplexedSessionDatabaseClient.getReadWriteBeginTransactionReference();
    assertNotNull(txn);
    assertNotNull(txn.getId());
    assertFalse(client.multiplexedSessionDatabaseClient.unimplementedForRW.get());

    // Try to execute a query using single use transaction.
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      SpannerException spannerException = assertThrows(SpannerException.class, resultSet::next);
      assertEquals(ErrorCode.UNIMPLEMENTED, spannerException.getErrorCode());
    }
    // Verify other UNIMPLEMENTED errors does not turn off read-write transactions to use
    // multiplexed sessions.
    assertFalse(client.multiplexedSessionDatabaseClient.unimplementedForRW.get());

    // The read-write transaction should use multiplexed sessions and succeed.
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              // Returns a ResultSet containing the precommit token (ResultSetPrecommitToken)
              transaction.executeUpdate(UPDATE_STATEMENT);

              // Verify that a precommit token is received. This guarantees that the read-write
              // transaction was executed on a multiplexed session.
              TransactionContextImpl impl = (TransactionContextImpl) transaction;
              assertNotNull(impl.getLatestPrecommitToken());
              assertEquals(
                  ByteString.copyFromUtf8("ResultSetPrecommitToken"),
                  impl.getLatestPrecommitToken().getPrecommitToken());
              return null;
            });

    // Verify that two ExecuteSqlRequests were received and second one uses a multiplexed session.
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);

    Session session2 = mockSpanner.getSession(requests.get(1).getSession());
    assertNotNull(session2);
    assertTrue(session2.getMultiplexed());

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  private void waitForSessionToBeReplaced(DatabaseClientImpl client) {
    assertNotNull(client.multiplexedSessionDatabaseClient);
    SessionReference sessionReference =
        client.multiplexedSessionDatabaseClient.getCurrentSessionReference();
    while (sessionReference
        == client.multiplexedSessionDatabaseClient.getCurrentSessionReference()) {
      Thread.yield();
    }
  }
}
