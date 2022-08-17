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

package com.google.cloud.spanner.connection.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.spanner.AsyncResultSet;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.AsyncStatementResult;
import com.google.cloud.spanner.connection.Connection;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest;
import com.google.cloud.spanner.connection.StatementResult;
import com.google.cloud.spanner.connection.StatementResult.ResultType;
import com.google.cloud.spanner.connection.TransactionMode;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Execute DML Returning statements using the generic connection API. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITDmlReturningTest extends ITAbstractSpannerTest {
  private final String UPDATE =
      "UPDATE Singers SET LastName = \"XYZ\" WHERE FirstName = \"ABC\" THEN RETURN *";
  private static boolean initialized = false;

  @Before
  public void setupTable() {
    if (!initialized) {
      try (Connection connection = createConnection()) {
        if (!tableExists(connection, "Singers")) {
          connection.execute(
              Statement.of(
                  "CREATE TABLE Singers ("
                      + "  SingerId INT64 NOT NULL,"
                      + "  FirstName STRING(1024),"
                      + "  LastName STRING(1024)"
                      + ")  PRIMARY KEY(SingerId)"));
          connection.execute(
              Statement.of(
                  "INSERT INTO SINGERS "
                      + "(SINGERID, FIRSTNAME, LASTNAME) VALUES "
                      + "(1, \"ABC\", \"XYZ\"), "
                      + "(2, \"ABC\", \"DEF\"), "
                      + "(3, \"DEF\", \"XYZ\"), "
                      + "(4, \"PQR\", \"ABC\"), "
                      + "(5, \"ABC\", \"GHI\")"));
        }
      }
      initialized = true;
    }
  }

  @Test
  public void testDmlReturningExecuteQuery() {
    try (Connection connection = createConnection()) {
      ResultSet rs = connection.executeQuery(Statement.of(UPDATE));
      assertEquals(rs.getColumnCount(), 3);
      assertTrue(rs.next());
      assertEquals(rs.getString(1), "ABC");
      assertTrue(rs.next());
      assertEquals(rs.getString(1), "ABC");
      assertTrue(rs.next());
      assertEquals(rs.getString(1), "ABC");
      assertFalse(rs.next());
      assertNotNull(rs.getStats());
      assertEquals(rs.getStats().getRowCountExact(), 3);
    }
  }

  @Test
  public void testDmlReturningExecuteQueryAsync() {
    try (Connection connection = createConnection()) {
      AsyncResultSet rs = connection.executeQueryAsync(Statement.of(UPDATE));
      rs.setCallback(
          Executors.newSingleThreadExecutor(),
          resultSet -> {
            try {
              while (true) {
                switch (resultSet.tryNext()) {
                  case OK:
                    assertEquals(resultSet.getColumnCount(), 3);
                    assertEquals(resultSet.getString(1), "ABC");
                    break;
                  case DONE:
                    assertNotNull(resultSet.getStats());
                    assertEquals(resultSet.getStats().getRowCountExact(), 3);
                    return CallbackResponse.DONE;
                  case NOT_READY:
                    return CallbackResponse.CONTINUE;
                  default:
                    throw new IllegalStateException();
                }
              }
            } catch (SpannerException e) {
              System.out.printf("Error in callback: %s%n", e.getMessage());
              return CallbackResponse.DONE;
            }
          });
    }
  }

  @Test
  public void testDmlReturningExecuteUpdate() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      try {
        connection.executeUpdate(Statement.of(UPDATE));
        fail("missing exception");
      } catch (SpannerException e) {
        assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      }
    }
  }

  @Test
  public void testDmlReturningExecuteUpdateAsync() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      try {
        connection.executeUpdateAsync(Statement.of(UPDATE));
        fail("missing exception");
      } catch (SpannerException e) {
        assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      }
    }
  }

  @Test
  public void testDmlReturningExecute() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      StatementResult res = connection.execute(Statement.of(UPDATE));
      assertEquals(res.getResultType(), ResultType.RESULT_SET);
      ResultSet rs = res.getResultSet();
      assertEquals(rs.getColumnCount(), 3);
      assertTrue(rs.next());
      assertEquals(rs.getString(1), "ABC");
      assertTrue(rs.next());
      assertEquals(rs.getString(1), "ABC");
      assertTrue(rs.next());
      assertEquals(rs.getString(1), "ABC");
      assertFalse(rs.next());
      assertNotNull(rs.getStats());
      assertEquals(rs.getStats().getRowCountExact(), 3);
    }
  }

  @Test
  public void testDmlReturningExecuteAsync() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      AsyncStatementResult res = connection.executeAsync(Statement.of(UPDATE));
      assertEquals(res.getResultType(), ResultType.RESULT_SET);
      AsyncResultSet rs = res.getResultSetAsync();
      rs.setCallback(
          Executors.newSingleThreadExecutor(),
          resultSet -> {
            try {
              while (true) {
                switch (resultSet.tryNext()) {
                  case OK:
                    assertEquals(resultSet.getColumnCount(), 3);
                    assertEquals(resultSet.getString(1), "ABC");
                    break;
                  case DONE:
                    assertNotNull(resultSet.getStats());
                    assertEquals(resultSet.getStats().getRowCountExact(), 3);
                    return CallbackResponse.DONE;
                  case NOT_READY:
                    return CallbackResponse.CONTINUE;
                  default:
                    throw new IllegalStateException();
                }
              }
            } catch (SpannerException e) {
              System.out.printf("Error in callback: %s%n", e.getMessage());
              return CallbackResponse.DONE;
            }
          });
    }
  }

  @Test
  public void testDmlReturningExecuteQueryReadOnlyMode() {
    try (Connection connection = createConnection()) {
      connection.setReadOnly(true);
      try {
        connection.executeQuery(Statement.of(UPDATE));
        fail("missing exception");
      } catch (SpannerException e) {
        assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      }
    }
  }

  @Test
  public void testDmlReturningExecuteQueryReadOnlyTransaction() {
    try (Connection connection = createConnection()) {
      connection.setReadOnly(false);
      connection.setAutocommit(false);
      connection.setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
      try {
        connection.executeQuery(Statement.of(UPDATE));
        fail("missing exception");
      } catch (SpannerException e) {
        assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      }
    }
  }

  @Test
  public void testDmlReturningExecuteQueryAsyncReadOnlyMode() {
    try (Connection connection = createConnection()) {
      connection.setReadOnly(true);
      try {
        connection.executeQueryAsync(Statement.of(UPDATE));
        fail("missing exception");
      } catch (SpannerException e) {
        assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      }
    }
  }

  @Test
  public void testDmlReturningExecuteQueryAsyncReadOnlyTransaction() {
    try (Connection connection = createConnection()) {
      connection.setReadOnly(false);
      connection.setAutocommit(false);
      connection.setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
      try {
        connection.executeQueryAsync(Statement.of(UPDATE));
        fail("missing exception");
      } catch (SpannerException e) {
        assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      }
    }
  }
}
