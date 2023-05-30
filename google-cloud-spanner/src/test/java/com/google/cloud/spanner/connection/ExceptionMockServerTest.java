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

import static com.google.cloud.spanner.SpannerApiFutures.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import io.grpc.Status;
import java.util.Arrays;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExceptionMockServerTest extends AbstractMockServerTest {

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
  }

  @Test
  public void testUpdateAsyncException() {
    Statement update = Statement.of("update foo set bar=1 where baz=1");
    mockSpanner.putStatementResult(
        StatementResult.exception(
            update,
            Status.INVALID_ARGUMENT.withDescription("Table 'foo' not found").asRuntimeException()));

    try (Connection connection = createConnection()) {
      SpannerException exception =
          assertThrows(SpannerException.class, () -> get(connection.executeUpdateAsync(update)));
      assertNotNull(exception.getSuppressed());
      assertEquals(1, exception.getSuppressed().length);
      Throwable suppressed = exception.getSuppressed()[0];
      String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
      assertTrue(
          Arrays.stream(suppressed.getStackTrace())
              .anyMatch(
                  element ->
                      element.getClassName().equals(ExceptionMockServerTest.class.getName())
                          && element.getMethodName().equals(methodName)));
    }
  }

  @Test
  public void testUpdateException() {
    Statement update = Statement.of("update foo set bar=1 where baz=1");
    mockSpanner.putStatementResult(
        StatementResult.exception(
            update,
            Status.INVALID_ARGUMENT.withDescription("Table 'foo' not found").asRuntimeException()));

    try (Connection connection = createConnection()) {
      SpannerException exception =
          assertThrows(SpannerException.class, () -> connection.executeUpdate(update));
      assertNotNull(exception.getSuppressed());
      assertEquals(0, exception.getSuppressed().length);
    }
  }

  @Test
  public void testQueryAsyncException() {
    Statement update = Statement.of("select * from foo");
    mockSpanner.putStatementResult(
        StatementResult.exception(
            update,
            Status.INVALID_ARGUMENT.withDescription("Table 'foo' not found").asRuntimeException()));

    try (Connection connection = createConnection()) {
      SpannerException exception =
          assertThrows(
              SpannerException.class,
              () -> connection.executeQueryAsync(update).toList(row -> row));
      assertNotNull(exception.getSuppressed());
      assertEquals(1, exception.getSuppressed().length);
      Throwable suppressed = exception.getSuppressed()[0];
      String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
      assertTrue(
          Arrays.stream(suppressed.getStackTrace())
              .anyMatch(
                  element ->
                      element.getClassName().equals(ExceptionMockServerTest.class.getName())
                          && element.getMethodName().equals(methodName)));
    }
  }

  @Test
  public void testQueryException() {
    Statement update = Statement.of("select * from foo");
    mockSpanner.putStatementResult(
        StatementResult.exception(
            update,
            Status.INVALID_ARGUMENT.withDescription("Table 'foo' not found").asRuntimeException()));

    try (Connection connection = createConnection()) {
      SpannerException exception =
          assertThrows(SpannerException.class, () -> connection.executeQuery(update).next());
      assertNotNull(exception.getSuppressed());
      assertEquals(0, exception.getSuppressed().length);
    }
  }

  @Test
  public void testCommitAsyncException() {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.INVALID_ARGUMENT.withDescription("Table 'foo' not found").asRuntimeException()));

    try (Connection connection = createConnection()) {
      connection.bufferedWrite(Mutation.newInsertBuilder("foo").set("id").to(1L).build());
      SpannerException exception =
          assertThrows(SpannerException.class, () -> get(connection.commitAsync()));
      assertNotNull(exception.getSuppressed());
      assertEquals(1, exception.getSuppressed().length);
      Throwable suppressed = exception.getSuppressed()[0];
      String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
      assertTrue(
          Arrays.stream(suppressed.getStackTrace())
              .anyMatch(
                  element ->
                      element.getClassName().equals(ExceptionMockServerTest.class.getName())
                          && element.getMethodName().equals(methodName)));
    }
  }

  @Test
  public void testCommitException() {
    mockSpanner.setCommitExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.INVALID_ARGUMENT.withDescription("Table 'foo' not found").asRuntimeException()));

    try (Connection connection = createConnection()) {
      connection.bufferedWrite(Mutation.newInsertBuilder("foo").set("id").to(1L).build());
      SpannerException exception = assertThrows(SpannerException.class, connection::commit);
      assertNotNull(exception.getSuppressed());
      assertEquals(0, exception.getSuppressed().length);
    }
  }
}
