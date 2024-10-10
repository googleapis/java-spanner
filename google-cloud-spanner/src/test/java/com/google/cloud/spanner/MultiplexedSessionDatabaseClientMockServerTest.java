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
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.NoCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.BeginTransactionRequest;
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
  public void testAbortedReadWriteTxnUsesPreviousTxnIdOnRetryWithInlineBegin()
      throws InterruptedException {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    Thread.sleep(10000);
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
  public void testAbortedReadWriteTxnUsesPreviousTxnIdOnRetryWithExplicitBegin()
      throws InterruptedException {
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    Thread.sleep(10000);
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
