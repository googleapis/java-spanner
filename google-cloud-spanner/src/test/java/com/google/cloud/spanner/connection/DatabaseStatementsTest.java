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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.DatabaseNotFoundException;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerExceptionFactoryTest;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.StatementResult.ResultType;
import com.google.longrunning.Operation;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DropDatabaseRequest;
import com.google.spanner.admin.database.v1.GetDatabaseRequest;
import com.google.spanner.admin.database.v1.ListDatabasesRequest;
import com.google.spanner.admin.database.v1.ListDatabasesResponse;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DatabaseStatementsTest extends AbstractMockServerTest {

  @Test
  public void testShowDatabases() {
    mockDatabaseAdmin.addResponse(
        ListDatabasesResponse.newBuilder()
            .addDatabases(
                Database.newBuilder().setName("projects/proj/instances/inst/databases/db1").build())
            .addDatabases(
                Database.newBuilder().setName("projects/proj/instances/inst/databases/db2").build())
            .build());
    try (Connection connection = createConnectionWithoutDb()) {
      StatementResult result = connection.execute(Statement.of("SHOW VARIABLE DATABASES"));
      assertEquals(ResultType.RESULT_SET, result.getResultType());
      try (ResultSet resultSet = result.getResultSet()) {
        assertTrue(resultSet.next());
        assertEquals("db1", resultSet.getString("NAME"));
        assertTrue(resultSet.next());
        assertEquals("db2", resultSet.getString("NAME"));
        assertFalse(resultSet.next());
      }
    }
    List<AbstractMessage> requests = mockDatabaseAdmin.getRequests();
    assertEquals(1, requests.size());
    assertTrue(requests.get(0) instanceof ListDatabasesRequest);
  }

  @Test
  public void testUseDatabase() {
    mockDatabaseAdmin.addResponse(
        Database.newBuilder()
            .setName("projects/proj/instances/inst/databases/my-database")
            .build());
    try (Connection connection = createConnectionWithoutDb()) {
      connection.execute(Statement.of("USE DATABASE my-database"));
    }
    List<AbstractMessage> requests = mockDatabaseAdmin.getRequests();
    assertEquals(1, requests.size());
    assertTrue(requests.get(0) instanceof GetDatabaseRequest);
    GetDatabaseRequest request = (GetDatabaseRequest) requests.get(0);
    assertEquals("projects/proj/instances/inst/databases/my-database", request.getName());
  }

  @Test
  public void testUseDatabase_WithInvalidDatabase() {
    mockDatabaseAdmin.addException(
        SpannerExceptionFactoryTest.newStatusDatabaseNotFoundException(
            "projects/proj/instances/inst/databases/non-existing-database"));
    try (Connection connection = createConnectionWithoutDb()) {
      DatabaseNotFoundException e =
          assertThrows(
              DatabaseNotFoundException.class,
              () -> connection.execute(Statement.of("USE DATABASE non-existing-database")));
      assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
    }
  }

  @Test
  public void testCreateDatabase() {
    mockDatabaseAdmin.addResponse(
        Operation.newBuilder()
            .setDone(true)
            .setResponse(
                Any.pack(
                    Database.newBuilder()
                        .setName("projects/p/instances/i/databases/my-database")
                        .build()))
            .setMetadata(Any.pack(CreateDatabaseMetadata.getDefaultInstance()))
            .build());
    try (Connection connection = createConnectionWithoutDb()) {
      connection.execute(Statement.of("CREATE DATABASE my-database"));
    }
    List<AbstractMessage> requests = mockDatabaseAdmin.getRequests();
    assertEquals(1, requests.size());
    assertTrue(requests.get(0) instanceof CreateDatabaseRequest);
    CreateDatabaseRequest request = (CreateDatabaseRequest) requests.get(0);
    assertEquals("CREATE DATABASE `my-database`", request.getCreateStatement());
  }

  @Test
  public void testAlterDatabase() {
    mockDatabaseAdmin.addResponse(
        Operation.newBuilder()
            .setDone(true)
            .setResponse(Any.pack(Empty.getDefaultInstance()))
            .setMetadata(Any.pack(UpdateDatabaseDdlMetadata.getDefaultInstance()))
            .build());
    try (Connection connection = createConnectionWithoutDb()) {
      connection.execute(
          Statement.of("ALTER DATABASE `my-database` SET OPTIONS ( optimizer_version = 1 )"));
    }
    List<AbstractMessage> requests = mockDatabaseAdmin.getRequests();
    assertEquals(1, requests.size());
    assertTrue(requests.get(0) instanceof UpdateDatabaseDdlRequest);
    UpdateDatabaseDdlRequest request = (UpdateDatabaseDdlRequest) requests.get(0);
    assertEquals(1, request.getStatementsCount());
    assertEquals(
        "ALTER DATABASE `my-database` SET OPTIONS ( optimizer_version = 1 )",
        request.getStatements(0));
    assertEquals("projects/proj/instances/inst/databases/my-database", request.getDatabase());
  }

  @Test
  public void testDropDatabase() {
    mockDatabaseAdmin.addResponse(Empty.getDefaultInstance());
    try (Connection connection = createConnectionWithoutDb()) {
      connection.execute(Statement.of("DROP DATABASE `my-database`"));
    }
    List<AbstractMessage> requests = mockDatabaseAdmin.getRequests();
    assertEquals(1, requests.size());
    assertTrue(requests.get(0) instanceof DropDatabaseRequest);
    DropDatabaseRequest request = (DropDatabaseRequest) requests.get(0);
    assertEquals("projects/proj/instances/inst/databases/my-database", request.getDatabase());
  }
}
