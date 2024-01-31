/*
 * Copyright 2024 Google LLC
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

import static junit.framework.TestCase.assertEquals;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.DirectedReadOptions.ExcludeReplicas;
import com.google.spanner.v1.DirectedReadOptions.IncludeReplicas;
import com.google.spanner.v1.DirectedReadOptions.ReplicaSelection;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DirectedReadTest extends AbstractMockServerTest {
  private static final Statement STATEMENT = Statement.of("SELECT 1 AS C");

  @Parameters(name = "dialect = {0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (Dialect dialect : Dialect.values()) {
      builder.add(new Object[] {dialect});
    }
    return builder.build();
  }

  @Parameter public Dialect dialect;

  private Dialect currentDialect;

  @BeforeClass
  public static void setupQueryResult() {
    mockSpanner.putStatementResult(
        MockSpannerServiceImpl.StatementResult.query(
            STATEMENT,
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("C")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(Value.newBuilder().setStringValue("1").build())
                        .build())
                .build()));
  }

  @Before
  public void setupDialect() {
    if (currentDialect != dialect) {
      // Reset the dialect result.
      SpannerPool.closeSpannerPool();
      mockSpanner.putStatementResult(
          MockSpannerServiceImpl.StatementResult.detectDialectResult(dialect));
      currentDialect = dialect;
    }
  }

  private String getVariablePrefix() {
    return dialect == Dialect.POSTGRESQL ? "spanner." : "";
  }

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
  }

  @Test
  public void testNoDirectedReadByDefault() {
    try (Connection connection = createConnection()) {
      for (boolean readOnly : new boolean[] {true, false}) {
        for (boolean autocommit : new boolean[] {true, false}) {
          connection.setAutocommit(autocommit);
          connection.setReadOnly(readOnly);

          executeQuery(connection);
          assertDirectedReadOptions(DirectedReadOptions.getDefaultInstance());

          if (!autocommit) {
            connection.commit();
          }
          mockSpanner.clearRequests();
        }
      }
    }
  }

  @Test
  public void testSetDirectedRead() {
    for (DirectedReadOptions expected :
        new DirectedReadOptions[] {
          DirectedReadOptions.newBuilder()
              .setIncludeReplicas(
                  IncludeReplicas.newBuilder()
                      .addReplicaSelections(
                          ReplicaSelection.newBuilder()
                              .setLocation("eu-west1")
                              .setType(ReplicaSelection.Type.READ_ONLY)
                              .build())
                      .build())
              .build(),
          DirectedReadOptions.newBuilder()
              .setExcludeReplicas(
                  ExcludeReplicas.newBuilder()
                      .addReplicaSelections(
                          ReplicaSelection.newBuilder()
                              .setLocation("eu-west1")
                              .setType(ReplicaSelection.Type.READ_ONLY)
                              .build())
                      .build())
              .build(),
          DirectedReadOptions.newBuilder().build()
        }) {
      try (Connection connection = createConnection()) {
        connection.setAutocommit(true);
        connection.execute(
            Statement.of(
                String.format(
                    "set %sdirected_read='%s'",
                    getVariablePrefix(), DirectedReadOptionsUtil.toString(expected))));
        executeQuery(connection);
        assertDirectedReadOptions(expected);

        mockSpanner.clearRequests();

        // Reset to default.
        connection.execute(
            Statement.of(String.format("set %sdirected_read=''", getVariablePrefix())));
        executeQuery(connection);
        assertDirectedReadOptions(DirectedReadOptions.getDefaultInstance());

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  public void testDirectedReadIsIgnoredInReadWriteTransaction() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setReadOnly(false);

      connection.execute(
          Statement.of(
              String.format(
                  "set %sdirected_read='%s'",
                  getVariablePrefix(),
                  DirectedReadOptionsUtil.toString(
                      DirectedReadOptions.newBuilder()
                          .setIncludeReplicas(
                              IncludeReplicas.newBuilder()
                                  .addReplicaSelections(
                                      ReplicaSelection.newBuilder()
                                          .setType(ReplicaSelection.Type.READ_WRITE)
                                          .setLocation("us-west1")
                                          .build())
                                  .build())
                          .build()))));
      // This uses a read/write transaction, which will ignore any DirectedReadOptions.
      executeQuery(connection);
      assertDirectedReadOptions(DirectedReadOptions.getDefaultInstance());
    }
  }

  @Test
  public void testDirectedReadIsUsedInReadOnlyTransaction() {
    DirectedReadOptions expected =
        DirectedReadOptions.newBuilder()
            .setIncludeReplicas(
                IncludeReplicas.newBuilder()
                    .addReplicaSelections(
                        ReplicaSelection.newBuilder()
                            .setType(ReplicaSelection.Type.READ_WRITE)
                            .setLocation("us-west1")
                            .build())
                    .build())
            .build();

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setReadOnly(true);

      connection.execute(
          Statement.of(
              String.format(
                  "set %sdirected_read='%s'",
                  getVariablePrefix(), DirectedReadOptionsUtil.toString(expected))));
      // This uses a read/write transaction, which will ignore any DirectedReadOptions.
      executeQuery(connection);
      assertDirectedReadOptions(expected);
    }
  }

  private void executeQuery(Connection connection) {
    try (ResultSet resultSet = connection.executeQuery(STATEMENT)) {
      //noinspection StatementWithEmptyBody
      while (resultSet.next()) {
        // ignore
      }
    }
  }

  private void assertDirectedReadOptions(DirectedReadOptions expected) {
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertEquals(1, requests.size());
    ExecuteSqlRequest request = requests.get(0);
    assertEquals(expected, request.getDirectedReadOptions());
  }
}
