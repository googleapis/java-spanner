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
import static org.junit.Assert.assertThrows;

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.StatementResult.ResultType;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import com.google.spanner.v1.CommitRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DdlTest extends AbstractMockServerTest {

  @After
  public void reset() {
    mockDatabaseAdmin.reset();
  }

  @Override
  protected String getBaseUrl() {
    return String.format(
        "cloudspanner://localhost:%d/projects/proj/instances/inst/databases/db?usePlainText=true",
        getPort());
  }

  private void addUpdateDdlResponse() {
    mockDatabaseAdmin.addResponse(
        Operation.newBuilder()
            .setMetadata(
                Any.pack(
                    UpdateDatabaseDdlMetadata.newBuilder()
                        .setDatabase("projects/proj/instances/inst/databases/db")
                        .build()))
            .setName("projects/proj/instances/inst/databases/db/operations/1")
            .setDone(true)
            .setResponse(Any.pack(Empty.getDefaultInstance()))
            .build());
  }

  @Test
  public void testSingleAnalyzeStatement() {
    addUpdateDdlResponse();

    try (Connection connection = createConnection()) {
      StatementResult result = connection.execute(Statement.of("analyze"));
      assertEquals(ResultType.NO_RESULT, result.getResultType());
    }

    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(1, requests.size());
    assertEquals(1, requests.get(0).getStatementsCount());
    assertEquals("analyze", requests.get(0).getStatements(0));
  }

  @Test
  public void testBatchedAnalyzeStatement() {
    addUpdateDdlResponse();

    try (Connection connection = createConnection()) {
      connection.startBatchDdl();
      assertEquals(
          ResultType.NO_RESULT,
          connection
              .execute(Statement.of("create table foo (id int64) primary key (id)"))
              .getResultType());
      assertEquals(
          ResultType.NO_RESULT, connection.execute(Statement.of("analyze")).getResultType());
      connection.runBatch();
    }

    List<UpdateDatabaseDdlRequest> requests =
        mockDatabaseAdmin.getRequests().stream()
            .filter(request -> request instanceof UpdateDatabaseDdlRequest)
            .map(request -> (UpdateDatabaseDdlRequest) request)
            .collect(Collectors.toList());
    assertEquals(1, requests.size());
    assertEquals(2, requests.get(0).getStatementsCount());
    assertEquals("create table foo (id int64) primary key (id)", requests.get(0).getStatements(0));
    assertEquals("analyze", requests.get(0).getStatements(1));
  }

  @Test
  public void testDdlAtStartOfTransaction() {
    Statement statement = Statement.of("create table foo (id int64) primary key (id)");
    for (DdlInTransactionMode mode : DdlInTransactionMode.values()) {
      mockDatabaseAdmin.getRequests().clear();
      if (mode != DdlInTransactionMode.FAIL) {
        addUpdateDdlResponse();
      }

      try (Connection connection = createConnection()) {
        connection.setAutocommit(false);
        connection.setDdlInTransactionMode(mode);

        if (mode == DdlInTransactionMode.FAIL) {
          SpannerException exception =
              assertThrows(SpannerException.class, () -> connection.execute(statement));
          assertEquals(ErrorCode.FAILED_PRECONDITION, exception.getErrorCode());
        } else {
          assertEquals(ResultType.NO_RESULT, connection.execute(statement).getResultType());
          assertEquals(1, mockDatabaseAdmin.getRequests().size());
        }
      }
    }
  }

  @Test
  public void testDdlBatchAtStartOfTransaction() {
    for (DdlInTransactionMode mode : DdlInTransactionMode.values()) {
      mockDatabaseAdmin.getRequests().clear();
      if (mode != DdlInTransactionMode.FAIL) {
        addUpdateDdlResponse();
      }

      try (Connection connection = createConnection()) {
        connection.setAutocommit(false);
        connection.setDdlInTransactionMode(mode);

        if (mode == DdlInTransactionMode.FAIL) {
          SpannerException exception =
              assertThrows(
                  SpannerException.class,
                  () -> connection.execute(Statement.of("start batch ddl")));
          assertEquals(ErrorCode.FAILED_PRECONDITION, exception.getErrorCode());
        } else {
          connection.execute(Statement.of("start batch ddl"));
          connection.execute(Statement.of("create table foo"));
          connection.execute(Statement.of("alter table bar"));
          connection.execute(Statement.of("run batch"));
          assertEquals(1, mockDatabaseAdmin.getRequests().size());
          UpdateDatabaseDdlRequest request =
              (UpdateDatabaseDdlRequest) mockDatabaseAdmin.getRequests().get(0);
          assertEquals(2, request.getStatementsCount());
        }
      }
    }
  }

  @Test
  public void testDdlInTransaction() {
    Statement statement = Statement.of("create table foo (id int64) primary key (id)");
    for (DdlInTransactionMode mode : DdlInTransactionMode.values()) {
      mockDatabaseAdmin.getRequests().clear();
      if (mode == DdlInTransactionMode.AUTO_COMMIT_TRANSACTION) {
        addUpdateDdlResponse();
      }

      try (Connection connection = createConnection()) {
        connection.setAutocommit(false);
        connection.setDdlInTransactionMode(mode);

        connection.execute(INSERT_STATEMENT);

        if (mode != DdlInTransactionMode.AUTO_COMMIT_TRANSACTION) {
          SpannerException exception =
              assertThrows(SpannerException.class, () -> connection.execute(statement));
          assertEquals(ErrorCode.FAILED_PRECONDITION, exception.getErrorCode());
        } else {
          assertEquals(ResultType.NO_RESULT, connection.execute(statement).getResultType());
          assertEquals(1, mockDatabaseAdmin.getRequests().size());
          assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
        }
      }
    }
  }

  @Test
  public void testDdlBatchInTransaction() {
    for (DdlInTransactionMode mode : DdlInTransactionMode.values()) {
      mockDatabaseAdmin.getRequests().clear();
      if (mode == DdlInTransactionMode.AUTO_COMMIT_TRANSACTION) {
        addUpdateDdlResponse();
      }

      try (Connection connection = createConnection()) {
        connection.setAutocommit(false);
        connection.setDdlInTransactionMode(mode);

        connection.execute(INSERT_STATEMENT);

        if (mode != DdlInTransactionMode.AUTO_COMMIT_TRANSACTION) {
          SpannerException exception =
              assertThrows(
                  SpannerException.class,
                  () -> connection.execute(Statement.of("start batch ddl")));
          assertEquals(ErrorCode.FAILED_PRECONDITION, exception.getErrorCode());
        } else {
          connection.execute(Statement.of("start batch ddl"));
          connection.execute(Statement.of("create table foo"));
          connection.execute(Statement.of("alter table bar"));
          connection.execute(Statement.of("run batch"));
          assertEquals(1, mockDatabaseAdmin.getRequests().size());
          UpdateDatabaseDdlRequest request =
              (UpdateDatabaseDdlRequest) mockDatabaseAdmin.getRequests().get(0);
          assertEquals(2, request.getStatementsCount());
          assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
        }
      }
    }
  }
}
