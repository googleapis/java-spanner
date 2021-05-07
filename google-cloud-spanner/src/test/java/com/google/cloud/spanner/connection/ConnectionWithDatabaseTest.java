/*
 * Copyright 2021 Google LLC
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

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConnectionWithDatabaseTest extends AbstractMockServerTest {

  @Test
  public void testExecuteDdlWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  connection.execute(Statement.of("CREATE TABLE Foo (Id INT64) PRIMARY KEY (Id)")));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testExecuteDmlWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () -> connection.execute(Statement.of("UPDATE Foo SET Bar=1 WHERE TRUE")));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testExecuteQueryWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e =
          assertThrows(
              SpannerException.class, () -> connection.execute(Statement.of("SELECT * FROM Foo")));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testExecuteStartBatchDmlWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e =
          assertThrows(
              SpannerException.class, () -> connection.execute(Statement.of("START BATCH DML")));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testExecuteStartBatchDdlWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e =
          assertThrows(
              SpannerException.class, () -> connection.execute(Statement.of("START BATCH DDL")));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testBufferWriteWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () -> connection.bufferedWrite(Mutation.delete("Foo", KeySet.all())));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testBeginTransactionWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      connection.setAutocommit(true);
      SpannerException e =
          assertThrows(SpannerException.class, () -> connection.beginTransaction());
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testCommitWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e = assertThrows(SpannerException.class, () -> connection.commit());
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }

  @Test
  public void testRollbackWithoutDatabase() {
    try (Connection connection = createConnectionWithoutDb()) {
      SpannerException e = assertThrows(SpannerException.class, () -> connection.rollback());
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertTrue(e.getMessage().contains("This connection is not connected to a database."));
    }
  }
}
