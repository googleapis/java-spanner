/*
 * Copyright 2020 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.common.collect.ImmutableList;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.RequestOptions;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ConnectionTest {
  public static class EnvironmentConnectionOptionsTest extends AbstractMockServerTest {
    @Test
    public void testUseOptimizerVersionAndStatisticsPackageFromEnvironment() {
      try {
        SpannerOptions.useEnvironment(
            new SpannerOptions.SpannerEnvironment() {
              @Nonnull
              @Override
              public String getOptimizerVersion() {
                return "20";
              }

              @Nonnull
              @Override
              public String getOptimizerStatisticsPackage() {
                return "env-package";
              }
            });
        try (Connection connection = createConnection()) {
          // Do a query and verify that the version from the environment is used.
          try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(COUNT_BEFORE_INSERT);
            assertThat(rs.next()).isFalse();
            // Verify query options from the environment.
            ExecuteSqlRequest request = getLastExecuteSqlRequest();
            assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("20");
            assertThat(request.getQueryOptions().getOptimizerStatisticsPackage())
                .isEqualTo("env-package");
          }
          // Now set one of the query options on the connection. That option should be used in
          // combination with the other option from the environment.
          connection.execute(Statement.of("SET OPTIMIZER_VERSION='30'"));
          connection.execute(Statement.of("SET OPTIMIZER_STATISTICS_PACKAGE='custom-package'"));
          try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(COUNT_BEFORE_INSERT);
            assertThat(rs.next()).isFalse();

            ExecuteSqlRequest request = getLastExecuteSqlRequest();
            // Optimizer version should come from the connection.
            assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("30");
            // Optimizer statistics package should come from the connection.
            assertThat(request.getQueryOptions().getOptimizerStatisticsPackage())
                .isEqualTo("custom-package");
          }
          // Now specify options directly for the query. These should override both the environment
          // and what is set on the connection.
          try (ResultSet rs =
              connection.executeQuery(
                  Statement.newBuilder(SELECT_COUNT_STATEMENT.getSql())
                      .withQueryOptions(
                          QueryOptions.newBuilder()
                              .setOptimizerVersion("user-defined-version")
                              .setOptimizerStatisticsPackage("user-defined-statistics-package")
                              .build())
                      .build())) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(COUNT_BEFORE_INSERT);
            assertThat(rs.next()).isFalse();

            ExecuteSqlRequest request = getLastExecuteSqlRequest();
            // Optimizer version should come from the query.
            assertThat(request.getQueryOptions().getOptimizerVersion())
                .isEqualTo("user-defined-version");
            // Optimizer statistics package should come from the query.
            assertThat(request.getQueryOptions().getOptimizerStatisticsPackage())
                .isEqualTo("user-defined-statistics-package");
          }
        }
      } finally {
        SpannerOptions.useDefaultEnvironment();
      }
    }
  }

  public static class DefaultConnectionOptionsTest extends AbstractMockServerTest {
    @Test
    public void testDefaultOptimizerVersion() {
      try (Connection connection = createConnection()) {
        try (ResultSet rs =
            connection.executeQuery(Statement.of("SHOW VARIABLE OPTIMIZER_VERSION"))) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getString("OPTIMIZER_VERSION")).isEqualTo("");
          assertThat(rs.next()).isFalse();
        }
      }
    }

    @Test
    public void testDefaultOptimizerStatisticsPackage() {
      try (Connection connection = createConnection()) {
        try (ResultSet rs =
            connection.executeQuery(Statement.of("SHOW VARIABLE OPTIMIZER_STATISTICS_PACKAGE"))) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getString("OPTIMIZER_STATISTICS_PACKAGE")).isEqualTo("");
          assertThat(rs.next()).isFalse();
        }
      }
    }

    @Test
    public void testExecuteInvalidBatchUpdate() {
      try (Connection connection = createConnection()) {
        try {
          connection.executeBatchUpdate(
              ImmutableList.of(INSERT_STATEMENT, SELECT_RANDOM_STATEMENT));
          fail("Missing expected exception");
        } catch (SpannerException e) {
          assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
        }
      }
    }

    @Test
    public void testQueryAborted() {
      try (Connection connection = createConnection()) {
        connection.setRetryAbortsInternally(false);
        for (boolean abort : new Boolean[] {true, false}) {
          try {
            if (abort) {
              mockSpanner.abortNextStatement();
            }
            connection.executeQuery(SELECT_RANDOM_STATEMENT);
            assertThat(abort).isFalse();
            connection.commit();
          } catch (AbortedException e) {
            assertThat(abort).isTrue();
            connection.rollback();
          }
        }
      }
    }

    @Test
    public void testUpdateAborted() {
      try (Connection connection = createConnection()) {
        connection.setRetryAbortsInternally(false);
        for (boolean abort : new Boolean[] {true, false}) {
          try {
            if (abort) {
              mockSpanner.abortNextStatement();
            }
            connection.executeUpdate(INSERT_STATEMENT);
            assertThat(abort).isFalse();
            connection.commit();
          } catch (AbortedException e) {
            assertThat(abort).isTrue();
            connection.rollback();
          }
        }
      }
    }

    @Test
    public void testBatchUpdateAborted() {
      try (Connection connection = createConnection()) {
        connection.setRetryAbortsInternally(false);
        for (boolean abort : new Boolean[] {true, false}) {
          try {
            if (abort) {
              mockSpanner.abortNextStatement();
            }
            connection.executeBatchUpdate(ImmutableList.of(INSERT_STATEMENT, INSERT_STATEMENT));
            assertThat(abort).isFalse();
            connection.commit();
          } catch (AbortedException e) {
            assertThat(abort).isTrue();
            connection.rollback();
          }
        }
      }
    }
  }

  public static class ConnectionMinSessionsTest extends AbstractMockServerTest {

    @AfterClass
    public static void reset() {
      mockSpanner.reset();
    }

    protected String getBaseUrl() {
      return super.getBaseUrl() + ";minSessions=1";
    }

    @Test
    public void testMinSessions() throws InterruptedException, TimeoutException {
      try (Connection connection = createConnection()) {
        mockSpanner.waitForRequestsToContain(
            input ->
                input instanceof BatchCreateSessionsRequest
                    && ((BatchCreateSessionsRequest) input).getSessionCount() == 1,
            5000L);
      }
    }
  }

  public static class ConnectionMaxSessionsTest extends AbstractMockServerTest {

    @AfterClass
    public static void reset() {
      mockSpanner.reset();
    }

    protected String getBaseUrl() {
      return super.getBaseUrl() + ";maxSessions=1";
    }

    @Test
    public void testMaxSessions()
        throws InterruptedException, TimeoutException, ExecutionException {
      try (Connection connection1 = createConnection();
          Connection connection2 = createConnection()) {
        connection1.beginTransactionAsync();
        connection2.beginTransactionAsync();

        ApiFuture<Long> count1 = connection1.executeUpdateAsync(INSERT_STATEMENT);
        ApiFuture<Long> count2 = connection2.executeUpdateAsync(INSERT_STATEMENT);

        // Commit the transactions. Both should be able to finish, but both used the same session.
        ApiFuture<Void> commit1 = connection1.commitAsync();
        ApiFuture<Void> commit2 = connection2.commitAsync();

        // At least one transaction must wait until the other has finished before it can get a
        // session.
        assertThat(count1.isDone() && count2.isDone()).isFalse();
        assertThat(commit1.isDone() && commit2.isDone()).isFalse();

        // Wait until both finishes.
        ApiFutures.allAsList(Arrays.asList(commit1, commit2)).get(5L, TimeUnit.SECONDS);

        assertThat(count1.isDone()).isTrue();
        assertThat(count2.isDone()).isTrue();
      }
      assertThat(mockSpanner.numSessionsCreated()).isEqualTo(1);
    }
  }

  public static class ConnectionRPCPriorityTest extends AbstractMockServerTest {

    @AfterClass
    public static void reset() {
      mockSpanner.reset();
    }

    protected String getBaseUrl() {
      return super.getBaseUrl() + ";rpcPriority=MEDIUM";
    }

    @Test
    public void testQuery_RPCPriority() {
      try (Connection connection = createConnection()) {
        for (boolean autocommit : new boolean[] {true, false}) {
          connection.setAutocommit(autocommit);
          try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {}

          assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
          assertEquals(
              RequestOptions.Priority.PRIORITY_MEDIUM,
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getPriority());
          mockSpanner.clearRequests();
        }
      }
    }

    @Test
    public void testUpdate_RPCPriority() {
      try (Connection connection = createConnection()) {
        for (boolean autocommit : new boolean[] {true, false}) {
          connection.setAutocommit(autocommit);
          connection.executeUpdate(INSERT_STATEMENT);

          assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
          assertEquals(
              RequestOptions.Priority.PRIORITY_MEDIUM,
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getPriority());
          mockSpanner.clearRequests();
        }
      }
    }

    @Test
    public void testPartitionedUpdate_RPCPriority() {
      try (Connection connection = createConnection()) {
        connection.setAutocommit(true);
        connection.setAutocommitDmlMode(AutocommitDmlMode.PARTITIONED_NON_ATOMIC);
        connection.executeUpdate(INSERT_STATEMENT);

        assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
        assertEquals(
            RequestOptions.Priority.PRIORITY_MEDIUM,
            mockSpanner
                .getRequestsOfType(ExecuteSqlRequest.class)
                .get(0)
                .getRequestOptions()
                .getPriority());
        mockSpanner.clearRequests();
      }
    }

    @Test
    public void testBatchUpdate_RPCPriority() {
      try (Connection connection = createConnection()) {
        connection.executeBatchUpdate(Collections.singleton(INSERT_STATEMENT));
        connection.commit();

        assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
        assertEquals(
            RequestOptions.Priority.PRIORITY_MEDIUM,
            mockSpanner
                .getRequestsOfType(ExecuteBatchDmlRequest.class)
                .get(0)
                .getRequestOptions()
                .getPriority());
        assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
        assertEquals(
            RequestOptions.Priority.PRIORITY_MEDIUM,
            mockSpanner
                .getRequestsOfType(CommitRequest.class)
                .get(0)
                .getRequestOptions()
                .getPriority());
        mockSpanner.clearRequests();
      }
    }

    @Test
    public void testDmlBatch_RPCPriority() {
      try (Connection connection = createConnection()) {
        for (boolean autocommit : new boolean[] {true, false}) {
          connection.setAutocommit(autocommit);

          connection.startBatchDml();
          connection.execute(INSERT_STATEMENT);
          connection.execute(INSERT_STATEMENT);
          connection.runBatch();

          assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
          assertEquals(
              RequestOptions.Priority.PRIORITY_MEDIUM,
              mockSpanner
                  .getRequestsOfType(ExecuteBatchDmlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getPriority());
          mockSpanner.clearRequests();
        }
      }
    }

    @Test
    public void testRunBatch_RPCPriority() {
      try (Connection connection = createConnection()) {
        connection.startBatchDml();
        connection.execute(INSERT_STATEMENT);
        connection.execute(INSERT_STATEMENT);
        connection.runBatch();
        connection.commit();

        assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
        assertEquals(
            RequestOptions.Priority.PRIORITY_MEDIUM,
            mockSpanner
                .getRequestsOfType(ExecuteBatchDmlRequest.class)
                .get(0)
                .getRequestOptions()
                .getPriority());
        assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
        assertEquals(
            RequestOptions.Priority.PRIORITY_MEDIUM,
            mockSpanner
                .getRequestsOfType(CommitRequest.class)
                .get(0)
                .getRequestOptions()
                .getPriority());
        mockSpanner.clearRequests();
      }
    }

    @Test
    public void testShowSetRPCPriority() {
      try (Connection connection = createConnection()) {
        connection.setRPCPriority(null);
        try (ResultSet rs =
            connection.execute(Statement.of("SHOW VARIABLE RPC_PRIORITY")).getResultSet()) {
          assertTrue(rs.next());
          assertEquals("PRIORITY_UNSPECIFIED", rs.getString("RPC_PRIORITY"));
          assertFalse(rs.next());
        }
        connection.execute(Statement.of("SET RPC_PRIORITY='LOW'"));
        try (ResultSet rs =
            connection.execute(Statement.of("SHOW VARIABLE RPC_PRIORITY")).getResultSet()) {
          assertTrue(rs.next());
          assertEquals("LOW", rs.getString("RPC_PRIORITY"));
          assertFalse(rs.next());
        }
        connection.execute(Statement.of("SET RPC_PRIORITY='HIGH'"));
        try (ResultSet rs =
            connection.execute(Statement.of("SHOW VARIABLE RPC_PRIORITY")).getResultSet()) {
          assertTrue(rs.next());
          assertEquals("HIGH", rs.getString("RPC_PRIORITY"));
          assertFalse(rs.next());
        }
      }
    }
  }

  public static class DialectDetectionTest extends AbstractMockServerTest {
    protected String getBaseUrl() {
      return super.getBaseUrl() + ";minSessions=1";
    }

    @After
    public void reset() {
      // Reset dialect to default.
      mockSpanner.putStatementResult(
          MockSpannerServiceImpl.StatementResult.detectDialectResult(Dialect.GOOGLE_STANDARD_SQL));
      mockSpanner.reset();
      mockSpanner.removeAllExecutionTimes();
      // Close all open Spanner instances to ensure that each test run gets a fresh instance.
      SpannerPool.closeSpannerPool();
    }

    @Test
    public void testDefaultGetDialect() {
      try (Connection connection = createConnection()) {
        assertEquals(Dialect.GOOGLE_STANDARD_SQL, connection.getDialect());
      }
    }

    @Test
    public void testPostgreSQLGetDialect() {
      mockSpanner.putStatementResult(
          MockSpannerServiceImpl.StatementResult.detectDialectResult(Dialect.POSTGRESQL));
      try (Connection connection = createConnection()) {
        assertEquals(Dialect.POSTGRESQL, connection.getDialect());
      }
    }

    @Test
    public void testGetDialect_DatabaseNotFound() throws Exception {
      mockSpanner.setBatchCreateSessionsExecutionTime(
          SimulatedExecutionTime.stickyDatabaseNotFoundException("invalid-database"));
      try (Connection connection = createConnection()) {
        SpannerException exception = assertThrows(SpannerException.class, connection::getDialect);
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Database with id invalid-database not found"));
      }
    }
  }
}
