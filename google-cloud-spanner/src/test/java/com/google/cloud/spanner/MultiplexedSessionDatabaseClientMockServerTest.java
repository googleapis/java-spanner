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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.Session;
import io.grpc.Status;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
