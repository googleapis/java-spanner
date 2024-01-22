/*
 * Copyright 2023 Google LLC
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

package com.google.cloud.spanner.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AbortedDueToConcurrentModificationException;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.AbstractMessage;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.Session;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SavepointMockServerTest extends AbstractMockServerTest {

  @Parameters(name = "dialect = {0}")
  public static Object[] data() {
    return Dialect.values();
  }

  @Parameter public Dialect dialect;

  @Before
  public void setupDialect() {
    mockSpanner.putStatementResult(StatementResult.detectDialectResult(dialect));
  }

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
    SpannerPool.closeSpannerPool();
  }

  @Ignore
  @Test
  public void testGapicLatency() throws Exception {
    try (SpannerClient client =
        SpannerClient.create(
            SpannerSettings.newBuilder()
                .setTransportChannelProvider(
                    InstantiatingGrpcChannelProvider.newBuilder()
                        .setEndpoint("localhost:" + getPort())
                        .setCredentials(NoCredentials.getInstance())
                        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
                        .build())
                .setCredentialsProvider(NoCredentialsProvider.create())
                .build())) {
      mockSpanner.putStatementResult(
          StatementResult.query(
              Statement.of("select * from random"), SELECT_COUNT_RESULTSET_BEFORE_INSERT));

      Session session = client.createSession("projects/p/instances/i/databases/d");
      for (int n = 0; n < 100; n++) {
        Thread.sleep(500L);
        Stopwatch watch = Stopwatch.createStarted();
        for (PartialResultSet ignore :
            client
                .executeStreamingSqlCallable()
                .call(
                    ExecuteSqlRequest.newBuilder()
                        .setSession(session.getName())
                        .setSql("select * from random")
                        .build())) {
          // ignore
        }
        System.out.println("Total: " + watch.elapsed());
      }
    }
  }

  @Test
  public void testLatency() throws Exception {
    Statement statement = Statement.of("select * from random");
    //    RandomResultSetGenerator generator = new RandomResultSetGenerator(10);
    //    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    mockSpanner.putStatementResult(
        StatementResult.query(statement, SELECT_COUNT_RESULTSET_BEFORE_INSERT));

    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setCredentials(NoCredentials.getInstance())
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .setHost("http://localhost:" + getPort())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setTrackStackTraceOfSessionCheckout(false).build())
            .build()
            .getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

      try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
        while (resultSet.next()) {
          // ignore
        }
      }

      for (int n = 0; n < 100; n++) {
        Thread.sleep(500L);
        Stopwatch watch = Stopwatch.createStarted();
        try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
          while (resultSet.next()) {
            // ignore
          }
        }
        System.out.println("Total: " + watch.elapsed());
        //        System.out.println("GLOBAL: " + GLOBAL_STOP_WATCH.elapsed());
      }
    }
  }

  @Test
  public void testCreateSavepoint() {
    try (Connection connection = createConnection()) {
      connection.savepoint("s1");

      if (dialect == Dialect.POSTGRESQL) {
        // PostgreSQL allows multiple savepoints with the same name.
        connection.savepoint("s1");
      } else {
        assertThrows(SpannerException.class, () -> connection.savepoint("s1"));
      }

      // Test invalid identifiers.
      assertThrows(SpannerException.class, () -> connection.savepoint(null));
      assertThrows(SpannerException.class, () -> connection.savepoint(""));
      assertThrows(SpannerException.class, () -> connection.savepoint("1"));
      assertThrows(SpannerException.class, () -> connection.savepoint("-foo"));
      assertThrows(SpannerException.class, () -> connection.savepoint(Strings.repeat("t", 129)));
    }
  }

  @Test
  public void testCreateSavepointWhenDisabled() {
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.DISABLED);
      assertThrows(SpannerException.class, () -> connection.savepoint("s1"));
    }
  }

  @Test
  public void testReleaseSavepoint() {
    try (Connection connection = createConnection()) {
      connection.savepoint("s1");
      connection.releaseSavepoint("s1");
      assertThrows(SpannerException.class, () -> connection.releaseSavepoint("s1"));

      connection.savepoint("s1");
      connection.savepoint("s2");
      connection.releaseSavepoint("s1");
      // Releasing a savepoint also removes all savepoints after it.
      assertThrows(SpannerException.class, () -> connection.releaseSavepoint("s2"));

      if (dialect == Dialect.POSTGRESQL) {
        // PostgreSQL allows multiple savepoints with the same name.
        connection.savepoint("s1");
        connection.savepoint("s2");
        connection.savepoint("s1");
        connection.releaseSavepoint("s1");
        connection.releaseSavepoint("s2");
        connection.releaseSavepoint("s1");
        assertThrows(SpannerException.class, () -> connection.releaseSavepoint("s1"));
      }
    }
  }

  @Test
  public void testRollbackToSavepoint() {
    for (SavepointSupport savepointSupport :
        new SavepointSupport[] {SavepointSupport.ENABLED, SavepointSupport.FAIL_AFTER_ROLLBACK}) {
      try (Connection connection = createConnection()) {
        connection.setSavepointSupport(savepointSupport);

        connection.savepoint("s1");
        connection.rollbackToSavepoint("s1");
        // Rolling back to a savepoint does not remove it, so we can roll back multiple times to the
        // same savepoint.
        connection.rollbackToSavepoint("s1");

        connection.savepoint("s2");
        connection.rollbackToSavepoint("s1");
        // Rolling back to a savepoint removes all savepoints after it.
        assertThrows(SpannerException.class, () -> connection.rollbackToSavepoint("s2"));

        if (dialect == Dialect.POSTGRESQL) {
          // PostgreSQL allows multiple savepoints with the same name.
          connection.savepoint("s2");
          connection.savepoint("s1");
          connection.rollbackToSavepoint("s1");
          connection.rollbackToSavepoint("s2");
          connection.rollbackToSavepoint("s1");
          connection.rollbackToSavepoint("s1");
          connection.releaseSavepoint("s1");
          assertThrows(SpannerException.class, () -> connection.rollbackToSavepoint("s1"));
        }
      }
    }
  }

  @Test
  public void testSavepointInAutoCommit() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      assertThrows(SpannerException.class, () -> connection.savepoint("s1"));

      // Starting a 'manual' transaction in autocommit mode should enable savepoints.
      connection.beginTransaction();
      connection.savepoint("s1");
      connection.releaseSavepoint("s1");
    }
  }

  @Test
  public void testRollbackToSavepointInReadOnlyTransaction() {
    for (SavepointSupport savepointSupport :
        new SavepointSupport[] {SavepointSupport.ENABLED, SavepointSupport.FAIL_AFTER_ROLLBACK}) {
      try (Connection connection = createConnection()) {
        connection.setSavepointSupport(savepointSupport);
        connection.setReadOnly(true);

        // Read-only transactions also support savepoints, but they do not do anything. This feature
        // is here purely for compatibility.
        connection.savepoint("s1");
        try (ResultSet resultSet = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
          int count = 0;
          while (resultSet.next()) {
            count++;
          }
          assertEquals(RANDOM_RESULT_SET_ROW_COUNT, count);
        }

        connection.rollbackToSavepoint("s1");
        try (ResultSet resultSet = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
          int count = 0;
          while (resultSet.next()) {
            count++;
          }
          assertEquals(RANDOM_RESULT_SET_ROW_COUNT, count);
        }
        // Committing a read-only transaction is necessary to mark the end of the transaction.
        // It is a no-op on Cloud Spanner.
        connection.commit();

        assertEquals(1, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
        BeginTransactionRequest beginRequest =
            mockSpanner.getRequestsOfType(BeginTransactionRequest.class).get(0);
        assertTrue(beginRequest.getOptions().hasReadOnly());
        assertEquals(0, mockSpanner.countRequestsOfType(CommitRequest.class));
      }
      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testRollbackToSavepointInReadWriteTransaction() {
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.ENABLED);

      connection.savepoint("s1");
      try (ResultSet resultSet = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(RANDOM_RESULT_SET_ROW_COUNT, count);
      }

      connection.rollbackToSavepoint("s1");
      try (ResultSet resultSet = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(RANDOM_RESULT_SET_ROW_COUNT, count);
      }
      connection.commit();

      // Read/write transactions are started with inlined Begin transaction options.
      assertEquals(0, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
      assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));
      assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
      assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));

      List<AbstractMessage> requests =
          mockSpanner.getRequests().stream()
              .filter(
                  request ->
                      request instanceof ExecuteSqlRequest
                          || request instanceof RollbackRequest
                          || request instanceof CommitRequest)
              .collect(Collectors.toList());
      assertEquals(4, requests.size());
      int index = 0;
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(RollbackRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(CommitRequest.class, requests.get(index++).getClass());
    }
  }

  @Test
  public void testRollbackToSavepointWithDmlStatements() {
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.ENABLED);

      // First do a query that is included in the transaction.
      try (ResultSet resultSet = connection.executeQuery(SELECT_RANDOM_STATEMENT)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(RANDOM_RESULT_SET_ROW_COUNT, count);
      }
      // Set a savepoint and execute a couple of DML statements.
      connection.savepoint("s1");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.savepoint("s2");
      connection.executeUpdate(INSERT_STATEMENT);
      // Rollback the last DML statement and commit.
      connection.rollbackToSavepoint("s2");

      connection.commit();

      // Read/write transactions are started with inlined Begin transaction options.
      assertEquals(0, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
      assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));
      assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
      assertEquals(5, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));

      List<AbstractMessage> requests =
          mockSpanner.getRequests().stream()
              .filter(
                  request ->
                      request instanceof ExecuteSqlRequest
                          || request instanceof RollbackRequest
                          || request instanceof CommitRequest)
              .collect(Collectors.toList());
      assertEquals(7, requests.size());
      int index = 0;
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(RollbackRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(CommitRequest.class, requests.get(index++).getClass());
    }
  }

  @Test
  public void testRollbackToSavepointFails() {
    Statement statement = Statement.of("select * from foo where bar=true");
    int numRows = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.ENABLED);

      try (ResultSet resultSet = connection.executeQuery(statement)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(numRows, count);
      }
      // Set a savepoint and execute a couple of DML statements.
      connection.savepoint("s1");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.executeUpdate(INSERT_STATEMENT);
      // Change the result of the initial query.
      mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
      // Rollback to before the DML statements.
      // This will succeed as long as we don't execute any further statements.
      connection.rollbackToSavepoint("s1");

      // Trying to commit the transaction or execute any other statements on the transaction will
      // fail.
      assertThrows(AbortedDueToConcurrentModificationException.class, connection::commit);

      // Read/write transactions are started with inlined Begin transaction options.
      assertEquals(0, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
      assertEquals(2, mockSpanner.countRequestsOfType(RollbackRequest.class));
      assertEquals(0, mockSpanner.countRequestsOfType(CommitRequest.class));
      assertEquals(4, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));

      List<AbstractMessage> requests =
          mockSpanner.getRequests().stream()
              .filter(
                  request ->
                      request instanceof ExecuteSqlRequest
                          || request instanceof RollbackRequest
                          || request instanceof CommitRequest)
              .collect(Collectors.toList());
      assertEquals(6, requests.size());
      int index = 0;
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(RollbackRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(RollbackRequest.class, requests.get(index++).getClass());
    }
  }

  @Test
  public void testRollbackToSavepointWithFailAfterRollback() {
    Statement statement = Statement.of("select * from foo where bar=true");
    int numRows = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.FAIL_AFTER_ROLLBACK);

      try (ResultSet resultSet = connection.executeQuery(statement)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(numRows, count);
      }
      // Set a savepoint and execute a couple of DML statements.
      connection.savepoint("s1");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.executeUpdate(INSERT_STATEMENT);
      // Rollback to before the DML statements.
      // This will succeed as long as we don't execute any further statements.
      connection.rollbackToSavepoint("s1");

      // Trying to commit the transaction or execute any other statements on the transaction will
      // fail with an FAILED_PRECONDITION error, as using a transaction after a rollback to
      // savepoint has been disabled.
      SpannerException exception = assertThrows(SpannerException.class, connection::commit);
      assertEquals(ErrorCode.FAILED_PRECONDITION, exception.getErrorCode());
      assertEquals(
          "FAILED_PRECONDITION: Using a read/write transaction after rolling back to a "
              + "savepoint is not supported with SavepointSupport=FAIL_AFTER_ROLLBACK",
          exception.getMessage());
    }
  }

  @Test
  public void testRollbackToSavepointSucceedsWithRollback() {
    for (SavepointSupport savepointSupport :
        new SavepointSupport[] {SavepointSupport.ENABLED, SavepointSupport.FAIL_AFTER_ROLLBACK}) {
      Statement statement = Statement.of("select * from foo where bar=true");
      int numRows = 10;
      RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
      mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
      try (Connection connection = createConnection()) {
        connection.setSavepointSupport(savepointSupport);

        try (ResultSet resultSet = connection.executeQuery(statement)) {
          int count = 0;
          while (resultSet.next()) {
            count++;
          }
          assertEquals(numRows, count);
        }
        // Change the result of the initial query and set a savepoint.
        connection.savepoint("s1");
        mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
        // This will succeed as long as we don't execute any further statements.
        connection.rollbackToSavepoint("s1");

        // Rolling back the transaction should now be a no-op, as it has already been rolled back.
        connection.rollback();

        // Read/write transactions are started with inlined Begin transaction options.
        assertEquals(0, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
        assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));
        assertEquals(0, mockSpanner.countRequestsOfType(CommitRequest.class));
        assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
      }
      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testMultipleRollbacksWithChangedResults() {
    Statement statement = Statement.of("select * from foo where bar=true");
    int numRows = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    try (Connection connection = createConnection()) {
      try (ResultSet resultSet = connection.executeQuery(statement)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(numRows, count);
      }
      connection.savepoint("s1");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.savepoint("s2");
      connection.executeUpdate(INSERT_STATEMENT);

      // Change the result of the initial query to make sure that any retry will fail.
      mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
      // This will succeed as long as we don't execute any further statements.
      connection.rollbackToSavepoint("s2");
      // Rolling back one further should also work.
      connection.rollbackToSavepoint("s1");

      // Rolling back the transaction should now be a no-op, as it has already been rolled back.
      connection.rollback();

      assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));
      assertEquals(0, mockSpanner.countRequestsOfType(CommitRequest.class));
      assertEquals(3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    }
  }

  @Test
  public void testMultipleRollbacks() {
    Statement statement = Statement.of("select * from foo where bar=true");
    int numRows = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.ENABLED);

      try (ResultSet resultSet = connection.executeQuery(statement)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(numRows, count);
      }
      connection.savepoint("s1");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.savepoint("s2");
      connection.executeUpdate(INSERT_STATEMENT);

      // First roll back one step and then one more.
      connection.rollbackToSavepoint("s2");
      connection.rollbackToSavepoint("s1");

      // This will only commit the SELECT query.
      connection.commit();

      assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));
      assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
      assertEquals(4, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));

      List<AbstractMessage> requests =
          mockSpanner.getRequests().stream()
              .filter(
                  request ->
                      request instanceof ExecuteSqlRequest
                          || request instanceof RollbackRequest
                          || request instanceof CommitRequest)
              .collect(Collectors.toList());
      assertEquals(6, requests.size());
      int index = 0;
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(RollbackRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(CommitRequest.class, requests.get(index++).getClass());
    }
  }

  @Test
  public void testRollbackMutations() {
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.ENABLED);

      connection.bufferedWrite(Mutation.newInsertBuilder("foo1").build());
      connection.savepoint("s1");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.bufferedWrite(Mutation.newInsertBuilder("foo2").build());
      connection.savepoint("s2");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.bufferedWrite(Mutation.newInsertBuilder("foo3").build());

      connection.rollbackToSavepoint("s1");

      // This will only commit the first mutation.
      connection.commit();

      assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));
      assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
      assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
      CommitRequest commitRequest = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
      assertEquals(1, commitRequest.getMutationsCount());
      assertEquals("foo1", commitRequest.getMutations(0).getInsert().getTable());
    }
  }

  @Test
  public void testRollbackBatchDml() {
    try (Connection connection = createConnection()) {
      connection.setSavepointSupport(SavepointSupport.ENABLED);

      connection.executeUpdate(INSERT_STATEMENT);
      connection.savepoint("s1");
      connection.executeBatchUpdate(ImmutableList.of(INSERT_STATEMENT, INSERT_STATEMENT));
      connection.savepoint("s2");

      connection.executeUpdate(INSERT_STATEMENT);
      connection.savepoint("s3");
      connection.executeBatchUpdate(ImmutableList.of(INSERT_STATEMENT, INSERT_STATEMENT));
      connection.savepoint("s4");

      connection.rollbackToSavepoint("s2");

      connection.commit();

      assertEquals(1, mockSpanner.countRequestsOfType(RollbackRequest.class));
      assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
      assertEquals(3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
      assertEquals(3, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));

      List<AbstractMessage> requests =
          mockSpanner.getRequests().stream()
              .filter(
                  request ->
                      request instanceof ExecuteSqlRequest
                          || request instanceof RollbackRequest
                          || request instanceof CommitRequest
                          || request instanceof ExecuteBatchDmlRequest)
              .collect(Collectors.toList());
      assertEquals(8, requests.size());
      int index = 0;
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteBatchDmlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteBatchDmlRequest.class, requests.get(index++).getClass());
      assertEquals(RollbackRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteSqlRequest.class, requests.get(index++).getClass());
      assertEquals(ExecuteBatchDmlRequest.class, requests.get(index++).getClass());
      assertEquals(CommitRequest.class, requests.get(index++).getClass());
    }
  }

  @Test
  public void testRollbackToSavepointWithoutInternalRetries() {
    Statement statement = Statement.of("select * from foo where bar=true");
    int numRows = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    try (Connection connection = createConnection()) {
      connection.setRetryAbortsInternally(false);

      connection.savepoint("s1");
      try (ResultSet resultSet = connection.executeQuery(statement)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(numRows, count);
      }
      // This should work.
      connection.rollbackToSavepoint("s1");
      // Resuming after a rollback is not supported without internal retries enabled.
      assertThrows(SpannerException.class, () -> connection.executeUpdate(INSERT_STATEMENT));
    }
  }

  @Test
  public void testRollbackToSavepointWithoutInternalRetriesInReadOnlyTransaction() {
    Statement statement = Statement.of("select * from foo where bar=true");
    int numRows = 10;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    try (Connection connection = createConnection()) {
      connection.setRetryAbortsInternally(false);
      connection.setReadOnly(true);

      connection.savepoint("s1");
      try (ResultSet resultSet = connection.executeQuery(statement)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(numRows, count);
      }

      // Both rolling back and resuming after a rollback are supported in a read-only transaction,
      // even if internal retries have been disabled.
      connection.rollbackToSavepoint("s1");
      try (ResultSet resultSet = connection.executeQuery(statement)) {
        int count = 0;
        while (resultSet.next()) {
          count++;
        }
        assertEquals(numRows, count);
      }
    }
  }
}
