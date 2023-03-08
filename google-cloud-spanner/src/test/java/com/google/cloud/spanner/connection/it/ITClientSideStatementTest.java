package com.google.cloud.spanner.connection.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.api.core.ApiFuture;
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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Execute ClientSideStatements using the generic connection API. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITClientSideStatementTest extends ITAbstractSpannerTest {
  private static final String SET_AUTOCOMMIT_FALSE = "SET AUTOCOMMIT=FALSE";
  private static final String SHOW_VARIABLE_AUTOCOMMIT = "SHOW VARIABLE AUTOCOMMIT";

  @Test
  public void testExecuteUpdateClientSideStatement() {
    try (Connection connection = createConnection()) {
      long res = connection.executeUpdate(Statement.of(SET_AUTOCOMMIT_FALSE));
      assertFalse(connection.isAutocommit());
      assertEquals(res, 0L);
      connection.commit();
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () -> connection.executeUpdate(Statement.of(SHOW_VARIABLE_AUTOCOMMIT)));
      assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      try (ResultSet rs = connection.executeQuery(Statement.of(SHOW_VARIABLE_AUTOCOMMIT))) {
        while (rs.next()) {
          assertFalse(rs.getBoolean(0));
        }
      }
    }
  }

  @Test
  public void testExecuteUpdateAsyncClientSideStatement() {
    try (Connection connection = createConnection()) {
      ApiFuture<Long> resFuture = connection.executeUpdateAsync(Statement.of(SET_AUTOCOMMIT_FALSE));
      long res = resFuture.get();
      assertFalse(connection.isAutocommit());
      assertEquals(res, 0L);
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () -> connection.executeUpdateAsync(Statement.of(SHOW_VARIABLE_AUTOCOMMIT)));
      assertEquals(e.getErrorCode(), ErrorCode.FAILED_PRECONDITION);
      try (ResultSet rs = connection.executeQuery(Statement.of(SHOW_VARIABLE_AUTOCOMMIT))) {
        while (rs.next()) {
          assertFalse(rs.getBoolean(0));
        }
      }
    } catch (ExecutionException | InterruptedException e) {
      // pass
    }
  }

  @Test
  public void testExecuteClientSideStatement() {
    try (Connection connection = createConnection()) {
      StatementResult res = connection.execute(Statement.of(SET_AUTOCOMMIT_FALSE));
      assertFalse(connection.isAutocommit());
      assertEquals(res.getResultType(), ResultType.NO_RESULT);
      try (ResultSet rs = connection.executeQuery(Statement.of(SHOW_VARIABLE_AUTOCOMMIT))) {
        while (rs.next()) {
          assertFalse(rs.getBoolean(0));
        }
      }
    }
  }

  @Test
  public void testExecuteAsyncClientSideStatement() {
    try (Connection connection = createConnection()) {
      AsyncStatementResult res = connection.executeAsync(Statement.of(SET_AUTOCOMMIT_FALSE));
      assertFalse(connection.isAutocommit());
      assertEquals(res.getResultType(), ResultType.NO_RESULT);
      try (ResultSet rs = connection.executeQuery(Statement.of(SHOW_VARIABLE_AUTOCOMMIT))) {
        while (rs.next()) {
          assertFalse(rs.getBoolean(0));
        }
      }
    }
  }

  @Test
  public void testExecuteQueryClientSideStatement() {
    try (Connection connection = createConnection()) {
      try (ResultSet rs = connection.executeQuery(Statement.of(SHOW_VARIABLE_AUTOCOMMIT))) {
        while (rs.next()) {
          assertTrue(rs.getBoolean(0));
        }
      }
    }
  }

  @Test
  public void testExecuteQueryAsyncClientSideStatement() {
    try (Connection connection = createConnection()) {
      try (AsyncResultSet rs =
          connection.executeQueryAsync(Statement.of(SHOW_VARIABLE_AUTOCOMMIT))) {
        rs.setCallback(
            Executors.newSingleThreadExecutor(),
            resultSet -> {
              try {
                while (true) {
                  switch (resultSet.tryNext()) {
                    case OK:
                      assertTrue(resultSet.getBoolean(0));
                      break;
                    case DONE:
                      assertEquals(
                          Objects.requireNonNull(resultSet.getStats()).getRowCountExact(), 1);
                      return CallbackResponse.DONE;
                    case NOT_READY:
                      return CallbackResponse.CONTINUE;
                    default:
                      throw new IllegalStateException();
                  }
                }
              } catch (SpannerException e) {
                return CallbackResponse.DONE;
              }
            });
      }
    }
  }
}
