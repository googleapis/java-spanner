/*
 * Copyright 2022 Google LLC
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.AbortedDueToConcurrentModificationException;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.RollbackRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IsolationLevelTest extends AbstractMockServerTest {

  @BeforeClass
  public static void setupMockResults() {
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT_COUNT_STATEMENT, SELECT_COUNT_RESULTSET_AFTER_INSERT));
  }

  @Test
  public void testQueryInDefaultIsolationLevel() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testQueryInReadCommitted() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(request.hasTransaction());
    assertEquals(0, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testSingleDml() {
    // The behavior of a single DML statement should be equal in both isolation levels.
    for (IsolationLevel isolationLevel :
        new IsolationLevel[] {IsolationLevel.SERIALIZABLE, IsolationLevel.READ_COMMITTED}) {
      try (Connection connection = createConnection()) {
        assertFalse(connection.isAutocommit());
        connection.setIsolationLevel(isolationLevel);
        assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
        connection.commit();
      }
      assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
      ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
      assertTrue(request.getTransaction().hasBegin());
      assertTrue(request.getTransaction().getBegin().hasReadWrite());
      assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testQueryAndDmlInDefaultIsolationLevel() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
      connection.commit();
    }
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest selectRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(selectRequest.getTransaction().hasBegin());
    assertTrue(selectRequest.getTransaction().getBegin().hasReadWrite());
    ExecuteSqlRequest insertRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
    assertTrue(insertRequest.getTransaction().hasId());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testQueryAndDmlInReadCommitted() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
      connection.commit();
    }
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest selectRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(selectRequest.hasTransaction());
    ExecuteSqlRequest insertRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testDmlAndQuery() {
    // The behavior of a DML statement followed by a query should be equal in both isolation levels.
    for (IsolationLevel isolationLevel :
        new IsolationLevel[] {IsolationLevel.SERIALIZABLE, IsolationLevel.READ_COMMITTED}) {
      try (Connection connection = createConnection()) {
        assertFalse(connection.isAutocommit());
        connection.setIsolationLevel(isolationLevel);
        assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
        try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
          assertTrue(resultSet.next());
          assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
          assertFalse(resultSet.next());
        }
        connection.commit();
      }
      assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
      ExecuteSqlRequest insertRequest =
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
      assertTrue(insertRequest.getTransaction().hasBegin());
      assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());
      ExecuteSqlRequest selectRequest =
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
      assertTrue(selectRequest.getTransaction().hasId());
      assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testRollbackQueryInReadCommitted() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      connection.rollback();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(request.hasTransaction());
    // The rollback is a no-op as there was no transaction started.
    assertEquals(0, mockSpanner.countRequestsOfType(RollbackRequest.class));
  }

  @Test
  public void testQueryAndMutationsInReadCommitted() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      connection.bufferedWrite(Mutation.newInsertBuilder("foo").set("col1").to(1L).build());
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(request.hasTransaction());
    // The mutations should be committed using a transaction that uses an explicit BeginTransaction.
    assertEquals(1, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commitRequest = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commitRequest.getMutationsCount());
    assertEquals("foo", commitRequest.getMutations(0).getInsert().getTable());
  }

  @Test
  public void testQueryDmlAndMutationsInReadCommitted() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      assertEquals(1L, connection.execute(INSERT_STATEMENT).getUpdateCount().longValue());
      connection.bufferedWrite(Mutation.newInsertBuilder("foo").set("col1").to(1L).build());
      connection.commit();
    }
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest selectRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(selectRequest.hasTransaction());
    ExecuteSqlRequest insertRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());
    // The mutations should be committed using the transaction that was started by the insert
    // statement.
    assertEquals(0, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commitRequest = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commitRequest.getMutationsCount());
    assertEquals("foo", commitRequest.getMutations(0).getInsert().getTable());
  }

  @Test
  public void testQueryAndDmlInReadCommitted_DmlAborted() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      mockSpanner.abortNextStatement();
      assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
      connection.commit();
    }
    // We should get 3 execute requests, as the DML statement is retried.
    assertEquals(3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest selectRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(selectRequest.hasTransaction());
    ExecuteSqlRequest insertRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());

    // The transaction retry uses an explicit BeginTransaction request, as it was the first
    // statement that failed.
    assertEquals(1, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    ExecuteSqlRequest retriedInsertRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(2);
    assertTrue(retriedInsertRequest.getTransaction().hasId());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testQueryAndDmlInReadCommitted_CommitAborted() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
      mockSpanner.abortNextStatement();
      connection.commit();
    }
    // We should get 3 execute requests, as the DML statement is retried.
    assertEquals(3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest selectRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(selectRequest.hasTransaction());
    ExecuteSqlRequest insertRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());

    assertEquals(0, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    ExecuteSqlRequest retriedInsertRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(2);
    assertTrue(retriedInsertRequest.getTransaction().hasBegin());
    assertTrue(retriedInsertRequest.getTransaction().getBegin().hasReadWrite());
    assertEquals(2, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testDmlAndQuery_CommitAborted() {
    // The behavior of a DML statement followed by a query should be equal in both isolation levels.
    for (IsolationLevel isolationLevel :
        new IsolationLevel[] {IsolationLevel.SERIALIZABLE, IsolationLevel.READ_COMMITTED}) {
      try (Connection connection = createConnection()) {
        assertFalse(connection.isAutocommit());
        connection.setIsolationLevel(isolationLevel);
        assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
        try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
          assertTrue(resultSet.next());
          assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
          assertFalse(resultSet.next());
        }
        mockSpanner.abortNextStatement();
        connection.commit();
      }
      // Both the DML and query should be retried.
      assertEquals(4, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
      ExecuteSqlRequest insertRequest =
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
      assertTrue(insertRequest.getTransaction().hasBegin());
      assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());
      ExecuteSqlRequest selectRequest =
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
      assertTrue(selectRequest.getTransaction().hasId());

      ExecuteSqlRequest retriedInsertRequest =
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(2);
      assertEquals(INSERT_STATEMENT.getSql(), retriedInsertRequest.getSql());
      assertTrue(retriedInsertRequest.getTransaction().hasBegin());
      assertTrue(retriedInsertRequest.getTransaction().getBegin().hasReadWrite());
      ExecuteSqlRequest retriedSelectRequest =
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(3);
      assertTrue(retriedSelectRequest.getTransaction().hasId());
      assertEquals(SELECT_COUNT_STATEMENT.getSql(), retriedSelectRequest.getSql());
      assertEquals(2, mockSpanner.countRequestsOfType(CommitRequest.class));

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testQueryAndDmlInReadCommitted_DmlAborted_QueryResultsChanged() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      // Changing this result will not cause the transaction to fail.
      mockSpanner.putStatementResult(
          StatementResult.query(SELECT_COUNT_STATEMENT, SELECT_COUNT_RESULTSET_BEFORE_INSERT));
      mockSpanner.abortNextStatement();
      assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
      connection.commit();
    } finally {
      mockSpanner.putStatementResult(
          StatementResult.query(SELECT_COUNT_STATEMENT, SELECT_COUNT_RESULTSET_AFTER_INSERT));
    }
    // We should get 3 execute requests, as the DML statement is retried.
    assertEquals(3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest selectRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(selectRequest.hasTransaction());
    ExecuteSqlRequest insertRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());

    // The transaction retry uses an explicit BeginTransaction request, as it was the first
    // statement that failed.
    assertEquals(1, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    ExecuteSqlRequest retriedInsertRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(2);
    assertTrue(retriedInsertRequest.getTransaction().hasId());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testQueryAndDmlInReadCommitted_CommitAborted_DmlResultChanged() {
    try (Connection connection = createConnection()) {
      assertFalse(connection.isAutocommit());
      connection.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      try (ResultSet resultSet = connection.execute(SELECT_COUNT_STATEMENT).getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals(COUNT_AFTER_INSERT, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      assertEquals(1L, (long) connection.execute(INSERT_STATEMENT).getUpdateCount());
      mockSpanner.putStatementResult(StatementResult.update(INSERT_STATEMENT, 0L));
      mockSpanner.abortNextStatement();
      assertThrows(AbortedDueToConcurrentModificationException.class, connection::commit);
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(INSERT_STATEMENT, 1L));
    }
    // We should get 3 execute requests, as the DML statement is retried.
    assertEquals(3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest selectRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertFalse(selectRequest.hasTransaction());
    ExecuteSqlRequest insertRequest = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(1);
    assertTrue(insertRequest.getTransaction().hasBegin());
    assertTrue(insertRequest.getTransaction().getBegin().hasReadWrite());

    assertEquals(0, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    ExecuteSqlRequest retriedInsertRequest =
        mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(2);
    assertTrue(retriedInsertRequest.getTransaction().hasBegin());
    assertTrue(retriedInsertRequest.getTransaction().getBegin().hasReadWrite());
    // We only get one commit request, as the retry fails before trying to commit a second time.
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }
}
