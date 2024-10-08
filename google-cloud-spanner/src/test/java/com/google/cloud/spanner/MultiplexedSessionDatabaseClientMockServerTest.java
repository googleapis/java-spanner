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

import static com.google.cloud.spanner.MockSpannerTestUtil.UPDATE_COUNT;
import static com.google.cloud.spanner.MockSpannerTestUtil.UPDATE_STATEMENT;
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
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.RequestOptions.Priority;
import com.google.spanner.v1.Session;
import io.grpc.Status;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
    assertThat(commitRequests).hasSize(1);
    CommitRequest commit = commitRequests.get(0);
    assertNotNull(commit.getRequestOptions());
    assertThat(commit.getRequestOptions().getTransactionTag()).isEqualTo("app=spanner,env=test");
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
          assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
          assertThat(commitTimestamp.get()).isNotNull();
          assertThat(attempt.get()).isEqualTo(2);
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
    assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
    assertThat(attempt.get()).isEqualTo(2);

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
