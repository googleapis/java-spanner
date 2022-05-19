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

import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.StatementResult.ResultType;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
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
}
