/*
 * Copyright 2025 Google LLC
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

import static com.google.cloud.spanner.connection.ConnectionProperties.DEFAULT_ISOLATION_LEVEL;
import static com.google.cloud.spanner.connection.ConnectionProperties.READ_LOCK_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.ITConnection;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.TransactionOptions.IsolationLevel;
import com.google.spanner.v1.TransactionOptions.ReadWrite.ReadLockMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TransactionMockServerTest extends AbstractMockServerTest {

  @Parameter(0)
  public IsolationLevel isolationLevel;

  @Parameter(1)
  public ReadLockMode readLockMode;

  @Parameters(name = "isolationLevel = {0}, readLockMode = {1}")
  public static Collection<Object[]> data() {
    List<Object[]> result = new ArrayList<>();
    for (IsolationLevel isolationLevel : DEFAULT_ISOLATION_LEVEL.getValidValues()) {
      for (ReadLockMode readLockMode : READ_LOCK_MODE.getValidValues()) {
        result.add(new Object[] {isolationLevel, readLockMode});
      }
    }
    return result;
  }

  @Override
  protected ITConnection createConnection() {
    return createConnection(
        Collections.emptyList(),
        Collections.emptyList(),
        String.format(
            ";default_isolation_level=%s;read_lock_mode=%s", isolationLevel, readLockMode));
  }

  @Test
  public void testQuery() {
    try (Connection connection = createConnection()) {
      //noinspection EmptyTryBlock
      try (ResultSet ignore = connection.executeQuery(SELECT1_STATEMENT)) {}
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertEquals(
        readLockMode, request.getTransaction().getBegin().getReadWrite().getReadLockMode());
    assertFalse(request.getLastStatement());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testDml() {
    try (Connection connection = createConnection()) {
      connection.executeUpdate(INSERT_STATEMENT);
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertEquals(
        readLockMode, request.getTransaction().getBegin().getReadWrite().getReadLockMode());
    assertFalse(request.getLastStatement());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testDmlReturning() {
    try (Connection connection = createConnection()) {
      //noinspection EmptyTryBlock
      try (ResultSet ignore = connection.executeQuery(INSERT_RETURNING_STATEMENT)) {}
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertEquals(
        readLockMode, request.getTransaction().getBegin().getReadWrite().getReadLockMode());
    assertFalse(request.getLastStatement());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testBatchDml() {
    try (Connection connection = createConnection()) {
      connection.startBatchDml();
      connection.executeUpdate(INSERT_STATEMENT);
      connection.executeUpdate(INSERT_STATEMENT);
      connection.runBatch();
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
    ExecuteBatchDmlRequest request =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertEquals(
        readLockMode, request.getTransaction().getBegin().getReadWrite().getReadLockMode());
    assertFalse(request.getLastStatements());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testBeginTransactionIsolationLevel() {
    SpannerPool.closeSpannerPool();
    for (Dialect dialect : new Dialect[] {Dialect.POSTGRESQL, Dialect.GOOGLE_STANDARD_SQL}) {
      mockSpanner.putStatementResult(
          MockSpannerServiceImpl.StatementResult.detectDialectResult(dialect));

      try (Connection connection = super.createConnection()) {
        for (IsolationLevel isolationLevel :
            new IsolationLevel[] {IsolationLevel.REPEATABLE_READ, IsolationLevel.SERIALIZABLE}) {
          for (ReadLockMode readLockMode :
              new ReadLockMode[] {ReadLockMode.PESSIMISTIC, ReadLockMode.OPTIMISTIC}) {
            for (boolean useSql : new boolean[] {true, false}) {
              if (useSql) {
                connection.execute(
                    Statement.of(
                        "begin transaction isolation level "
                            + isolationLevel.name().replace("_", " ")));
              } else {
                connection.beginTransaction(isolationLevel);
              }
              if (dialect == Dialect.POSTGRESQL) {
                connection.execute(
                    Statement.of("set spanner.read_lock_mode = '" + readLockMode.name() + "'"));
              } else {
                connection.execute(
                    Statement.of("set read_lock_mode = '" + readLockMode.name() + "'"));
              }
              connection.executeUpdate(INSERT_STATEMENT);
              connection.commit();

              assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
              ExecuteSqlRequest request =
                  mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
              assertTrue(request.getTransaction().hasBegin());
              assertTrue(request.getTransaction().getBegin().hasReadWrite());
              assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
              assertEquals(
                  readLockMode,
                  request.getTransaction().getBegin().getReadWrite().getReadLockMode());
              assertFalse(request.getLastStatement());
              assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));

              mockSpanner.clearRequests();
            }
          }
        }
      }
      SpannerPool.closeSpannerPool();
    }
  }

  @Test
  public void testSetTransactionIsolationLevel() {
    SpannerPool.closeSpannerPool();
    mockSpanner.putStatementResult(
        MockSpannerServiceImpl.StatementResult.detectDialectResult(Dialect.POSTGRESQL));

    try (Connection connection = super.createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);

        for (IsolationLevel isolationLevel :
            new IsolationLevel[] {IsolationLevel.REPEATABLE_READ, IsolationLevel.SERIALIZABLE}) {
          for (ReadLockMode readLockMode :
              new ReadLockMode[] {ReadLockMode.OPTIMISTIC, ReadLockMode.PESSIMISTIC}) {
            // Manually start a transaction if autocommit is enabled.
            if (autocommit) {
              connection.execute(Statement.of("begin"));
            }
            connection.execute(
                Statement.of(
                    "set transaction isolation level " + isolationLevel.name().replace("_", " ")));
            connection.execute(
                Statement.of("set spanner.read_lock_mode = '" + readLockMode.name() + "'"));
            connection.executeUpdate(INSERT_STATEMENT);
            connection.commit();

            assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
            ExecuteSqlRequest request =
                mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
            assertTrue(request.getTransaction().hasBegin());
            assertTrue(request.getTransaction().getBegin().hasReadWrite());
            assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
            assertEquals(
                readLockMode, request.getTransaction().getBegin().getReadWrite().getReadLockMode());
            assertFalse(request.getLastStatement());
            assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));

            mockSpanner.clearRequests();
          }
        }
      }
    }
    SpannerPool.closeSpannerPool();
  }
}
