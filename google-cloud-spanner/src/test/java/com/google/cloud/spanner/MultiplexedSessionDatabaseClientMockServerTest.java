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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionStep;
import com.google.cloud.spanner.AsyncTransactionManager.CommitTimestampFuture;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.*;
import io.grpc.Status;
import java.time.Duration;
import java.util.*;
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
    assumeFalse(TestHelper.isMultiplexSessionDisabled());
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
                    .setUseMultiplexedSessionForRW(true)
                    .setUseMultiplexedSessionPartitionedOps(true)
                    // Set the maintainer to loop once every 1ms
                    .setMultiplexedSessionMaintenanceLoopFrequency(Duration.ofMillis(1L))
                    // Set multiplexed sessions to be replaced once every 1ms
                    .setMultiplexedSessionMaintenanceDuration(Duration.ofMillis(1L))
                    .setFailOnSessionLeak()
                    .build())
            .build()
            .getService();
  }

  @Test
  public void
      testRWTransactionWithTransactionManager_CommitAborted_SetsTransactionId_AndUsedInNewInstance() {
    // The below test verifies the behaviour of begin(AbortedException) method which is used to
    // maintain transaction priority if resetForRetry() is not called.

    // This test performs the following steps:
    // 1. Simulates an ABORTED exception during commit and verifies that the transaction ID is
    // included in the AbortedException.
    // 2. Passes the ABORTED exception to the begin(AbortedException) method of a new
    // TransactionManager, and verifies that the transaction ID from the failed transaction is sent
    // during the inline begin of the first request.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));

    ByteString abortedTransactionID = null;
    AbortedException exception = null;
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      try {
        try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
          //noinspection StatementWithEmptyBody
          while (resultSet.next()) {
            // ignore
          }
        }
        manager.commit();
        assertNotNull(manager.getCommitTimestamp());
      } catch (AbortedException e) {
        // The transactionID of the Aborted transaction should be set in AbortedException class.
        assertNotNull(e.getTransactionID());
        abortedTransactionID = e.getTransactionID();
        exception = e;
      }
    }
    // Verify that the transactionID of the aborted transaction is set.
    assertNotNull(abortedTransactionID);
    assertNotNull(exception);
    mockSpanner.clearRequests();

    // Pass AbortedException while invoking begin on the new manager instance.
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin(exception);
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

    // Verify that the ExecuteSqlRequest with the inline begin passes the transactionID of the
    // previously aborted transaction.
    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(1, executeSqlRequests.size());
    assertTrue(mockSpanner.getSession(executeSqlRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertEquals(
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId(),
        abortedTransactionID);

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void
      testRWTransactionWithTransactionManager_ExecuteSQLAborted_SetsTransactionId_AndUsedInNewInstance() {
    // This test performs the following steps:
    // 1. Simulates an ABORTED exception during ExecuteSQL and verifies that the transaction ID is
    // included in the AbortedException.
    // 2. Passes the ABORTED exception to the begin(AbortedException) method of a new
    // TransactionManager, and verifies that the transaction ID from the failed transaction is sent
    // during the inline begin of the first request.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    ByteString abortedTransactionID = null;
    AbortedException exception = null;
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      try {
        try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
          //noinspection StatementWithEmptyBody
          while (resultSet.next()) {
            // ignore
          }
        }

        // Simulate an ABORTED in next ExecuteSQL request.
        mockSpanner.setExecuteStreamingSqlExecutionTime(
            SimulatedExecutionTime.ofException(
                mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));

        try (ResultSet resultSet = transaction.executeQuery(STATEMENT)) {
          //noinspection StatementWithEmptyBody
          while (resultSet.next()) {
            // ignore
          }
        }
        manager.commit();
        assertNotNull(manager.getCommitTimestamp());
      } catch (AbortedException e) {
        // The transactionID of the Aborted transaction should be set in AbortedException class.
        assertNotNull(e.getTransactionID());
        abortedTransactionID = e.getTransactionID();
        exception = e;
      }
    }
    // Verify that the transactionID of the aborted transaction is set.
    assertNotNull(abortedTransactionID);
    assertNotNull(exception);
    mockSpanner.clearRequests();

    // Pass AbortedException while invoking begin on the new manager instance.
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin(exception);
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

    // Verify that the ExecuteSqlRequest with inline begin includes the transaction ID from the
    // previously aborted transaction.
    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(1, executeSqlRequests.size());
    assertTrue(mockSpanner.getSession(executeSqlRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertEquals(
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId(),
        abortedTransactionID);

    assertNotNull(client.multiplexedSessionDatabaseClient);
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsAcquired().get());
    assertEquals(2L, client.multiplexedSessionDatabaseClient.getNumSessionsReleased().get());
  }

  @Test
  public void
      testRWTransactionWithAsyncTransactionManager_CommitAborted_SetsTransactionId_AndUsedInNewInstance()
          throws Exception {
    // This test performs the following steps:
    // 1. Simulates an ABORTED exception during ExecuteSQL and verifies that the transaction ID is
    // included in the AbortedException.
    // 2. Passes the ABORTED exception to the begin(AbortedException) method of a new
    // AsyncTransactionManager, and verifies that the transaction ID from the failed transaction is
    // sent
    // during the inline begin of the first request.
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    // Force the Commit RPC to return Aborted the first time it is called. The exception is cleared
    // after the first call, so the retry should succeed.
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            mockSpanner.createAbortedException(ByteString.copyFromUtf8("test"))));
    ByteString abortedTransactionID = null;
    AbortedException exception = null;
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture transactionContextFuture = manager.beginAsync();
      try {
        AsyncTransactionStep<Void, Long> updateCount =
            transactionContextFuture.then(
                (transaction, ignored) -> transaction.executeUpdateAsync(UPDATE_STATEMENT),
                MoreExecutors.directExecutor());
        CommitTimestampFuture commitTimestamp = updateCount.commitAsync();
        assertEquals(UPDATE_COUNT, updateCount.get().longValue());
        assertNotNull(commitTimestamp.get());
      } catch (AbortedException e) {
        assertNotNull(e.getTransactionID());
        exception = e;
        abortedTransactionID = e.getTransactionID();
      }
    }

    // Verify that the transactionID of the aborted transaction is set.
    assertNotNull(abortedTransactionID);
    assertNotNull(exception);
    mockSpanner.clearRequests();

    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture transactionContextFuture = manager.beginAsync(exception);
      while (true) {
        try {
          AsyncTransactionStep<Void, Long> updateCount =
              transactionContextFuture.then(
                  (transaction, ignored) -> transaction.executeUpdateAsync(UPDATE_STATEMENT),
                  MoreExecutors.directExecutor());
          CommitTimestampFuture commitTimestamp = updateCount.commitAsync();
          assertEquals(UPDATE_COUNT, updateCount.get().longValue());
          assertNotNull(commitTimestamp.get());
          break;
        } catch (AbortedException e) {
          transactionContextFuture = manager.resetForRetryAsync();
        }
      }
    }

    List<ExecuteSqlRequest> executeSqlRequests =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(1, executeSqlRequests.size());
    assertTrue(mockSpanner.getSession(executeSqlRequests.get(0).getSession()).getMultiplexed());
    assertNotNull(
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId());
    assertEquals(
        executeSqlRequests
            .get(0)
            .getTransaction()
            .getBegin()
            .getReadWrite()
            .getMultiplexedSessionPreviousTransactionId(),
        abortedTransactionID);

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
