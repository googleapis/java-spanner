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

package com.google.cloud.spanner.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.testing.EmulatorSpannerHelper;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.NullValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITPgJsonbTest {

  private static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(5);

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static RemoteSpannerHelper testHelper;
  private static DatabaseAdminClient databaseAdminClient;
  private static List<DatabaseId> databasesToDrop;
  private static String projectId;
  private static String instanceId;
  private static String databaseId;
  private DatabaseClient databaseClient;
  private String tableName;

  @BeforeClass
  public static void beforeClass() throws Exception {
    assumeFalse(
        "PgJsonb is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());
    testHelper = env.getTestHelper();
    databaseAdminClient = testHelper.getClient().getDatabaseAdminClient();
    databasesToDrop = new ArrayList<>();
    projectId = testHelper.getInstanceId().getProject();
    instanceId = testHelper.getInstanceId().getInstance();
    databaseId = testHelper.getUniqueDatabaseId();
    final Database database =
        databaseAdminClient
            .newDatabaseBuilder(DatabaseId.of(projectId, instanceId, databaseId))
            .setDialect(Dialect.POSTGRESQL)
            .build();
    databaseAdminClient
        .createDatabase(database, Collections.emptyList())
        .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    databasesToDrop.add(database.getId());
  }

  @AfterClass
  public static void afterClass() throws Exception {
    if (databasesToDrop != null) {
      for (DatabaseId id : databasesToDrop) {
        try {
          databaseAdminClient.dropDatabase(id.getInstanceId().getInstance(), id.getDatabase());
        } catch (Exception e) {
          System.err.println("Failed to drop database " + id + ", skipping...: " + e.getMessage());
        }
      }
    }
  }

  @Before
  public void setUp() throws Exception {
    databaseClient =
        testHelper.getClient().getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
    tableName = testHelper.getUniqueDatabaseId();
    databaseAdminClient
        .updateDatabaseDdl(
            instanceId,
            databaseId,
            Collections.singletonList(
                "CREATE TABLE \"" + tableName + "\" (id BIGINT PRIMARY KEY, col1 JSONB)"),
            null)
        .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Test
  public void testPgJsonbAsPrimaryKey() {
    // JSONB is not allowed as a primary key.
    ExecutionException executionException =
        assertThrows(
            ExecutionException.class,
            () ->
                databaseAdminClient
                    .updateDatabaseDdl(
                        instanceId,
                        databaseId,
                        Collections.singletonList(
                            "CREATE TABLE with_jsonb_pk (id jsonb primary key)"),
                        null)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    SpannerException spannerException =
        SpannerExceptionFactory.asSpannerException(executionException.getCause());
    assertEquals(ErrorCode.INVALID_ARGUMENT, spannerException.getErrorCode());
    assertTrue(
        spannerException.getMessage(),
        spannerException
            .getMessage()
            .contains(
                "Column with_jsonb_pk.id has type PG.JSONB, but is part of the primary key."));
  }

  @Test
  public void testPgJsonbInSecondaryIndex() {
    // JSONB is not allowed as a key in a secondary index.
    ExecutionException executionException =
        assertThrows(
            ExecutionException.class,
            () ->
                databaseAdminClient
                    .updateDatabaseDdl(
                        instanceId,
                        databaseId,
                        Collections.singletonList(
                            "CREATE INDEX idx_jsonb on \"" + tableName + "\" (col1)"),
                        null)
                    .get(OPERATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    SpannerException spannerException =
        SpannerExceptionFactory.asSpannerException(executionException.getCause());
    assertEquals(ErrorCode.FAILED_PRECONDITION, spannerException.getErrorCode());
    assertTrue(
        spannerException.getMessage(),
        spannerException
            .getMessage()
            .contains("Index idx_jsonb is defined on a column of unsupported type PG.JSONB."));
  }

  private static final String JSON_VALUE_1 = "{\"color\":\"red\",\"value\":\"#f00\"}";
  private static final String JSON_VALUE_2 =
      "["
          + "   {\"color\":\"red\",\"value\":\"#f00\"},"
          + "   {\"color\":\"green\",\"value\":\"#0f0\"},"
          + "   {\"color\":\"blue\",\"value\":\"#00f\"}"
          + "]";

  @Test
  public void testLiteralPgJsonb() {
    assumeFalse(
        "PgJsonb is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(
                  Statement.of(
                      "INSERT INTO "
                          + tableName
                          + " (id, col1) VALUES"
                          + " (1, '"
                          + JSON_VALUE_1
                          + "')"
                          + ", (2, '"
                          + JSON_VALUE_2
                          + "')"
                          + ", (3, '{}')"
                          + ", (4, '[]')"
                          + ", (5, null)"));
              return null;
            });

    verifyContents();
  }

  @Test
  public void testPgJsonbParameter() {
    assumeFalse(
        "PgJsonb is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(
                  Statement.newBuilder(
                          "INSERT INTO "
                              + tableName
                              + " (id, col1) VALUES"
                              + " (1, $1)"
                              + ", (2, $2)"
                              + ", (3, $3)"
                              + ", (4, $4)"
                              + ", (5, $5)")
                      .bind("p1")
                      .to(Value.pgJsonb(JSON_VALUE_1))
                      .bind("p2")
                      .to(Value.pgJsonb(JSON_VALUE_2))
                      .bind("p3")
                      .to(Value.pgJsonb("{}"))
                      .bind("p4")
                      .to(Value.pgJsonb("[]"))
                      .bind("p5")
                      .to(Value.pgJsonb(null))
                      .build());
              return null;
            });

    verifyContents();
  }

  @Ignore("Untyped jsonb parameters are not yet supported")
  @Test
  public void testPgJsonbUntypedParameter() {
    assumeFalse(
        "PgJsonb is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());

    // Verify that we can use Jsonb as an untyped parameter. This is especially important for
    // PGAdapter and the JDBC driver, as these will often use untyped parameters.
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(
                  Statement.newBuilder(
                          "INSERT INTO "
                              + tableName
                              + " (id, col1) VALUES"
                              + " (1, $1)"
                              + ", (2, $2)"
                              + ", (3, $3)"
                              + ", (4, $4)"
                              + ", (5, $5)")
                      .bind("p1")
                      .to(
                          Value.untyped(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue(JSON_VALUE_1)
                                  .build()))
                      .bind("p2")
                      .to(
                          Value.untyped(
                              com.google.protobuf.Value.newBuilder()
                                  .setStringValue(JSON_VALUE_2)
                                  .build()))
                      .bind("p3")
                      .to(
                          Value.untyped(
                              com.google.protobuf.Value.newBuilder().setStringValue("{}").build()))
                      .bind("p4")
                      .to(
                          Value.untyped(
                              com.google.protobuf.Value.newBuilder().setStringValue("[]").build()))
                      .bind("p5")
                      .to(
                          Value.untyped(
                              com.google.protobuf.Value.newBuilder()
                                  .setNullValue(NullValue.NULL_VALUE)
                                  .build()))
                      .build());
              return null;
            });

    verifyContents();
  }

  @Test
  public void testMutationsWithPgJsonbAsString() {
    assumeFalse(
        "PgJsonb is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.buffer(
                  ImmutableList.of(
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(1)
                          .set("col1")
                          .to(JSON_VALUE_1)
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(2)
                          .set("col1")
                          .to(JSON_VALUE_2)
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(3)
                          .set("col1")
                          .to("{}")
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(4)
                          .set("col1")
                          .to("[]")
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(5)
                          .set("col1")
                          .to((String) null)
                          .build()));
              return null;
            });

    verifyContents();
  }

  @Test
  public void testMutationsWithPgJsonbAsValue() {
    assumeFalse(
        "PgJsonb is not supported in the emulator", EmulatorSpannerHelper.isUsingEmulator());
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.buffer(
                  ImmutableList.of(
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(1)
                          .set("col1")
                          .to(Value.pgJsonb(JSON_VALUE_1))
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(2)
                          .set("col1")
                          .to(Value.pgJsonb(JSON_VALUE_2))
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(3)
                          .set("col1")
                          .to(Value.pgJsonb("{}"))
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(4)
                          .set("col1")
                          .to(Value.pgJsonb("[]"))
                          .build(),
                      Mutation.newInsertBuilder(tableName)
                          .set("id")
                          .to(5)
                          .set("col1")
                          .to(Value.pgJsonb(null))
                          .build()));
              return null;
            });

    verifyContents();
  }

  private void verifyContents() {
    try (ResultSet resultSet =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of("SELECT * FROM " + tableName + " ORDER BY id"))) {

      assertTrue(resultSet.next());
      // Note: We do not use the JSON_VALUE_1 constant here, because the backend prettifies the
      // value a little, which means that there is a small difference between what we insert and
      // what we get back.
      assertEquals("{\"color\": \"red\", \"value\": \"#f00\"}", resultSet.getPgJsonb("col1"));
      assertEquals(
          Value.pgJsonb("{\"color\": \"red\", \"value\": \"#f00\"}"), resultSet.getValue("col1"));

      assertTrue(resultSet.next());
      assertEquals(
          "["
              + "{\"color\": \"red\", \"value\": \"#f00\"}, "
              + "{\"color\": \"green\", \"value\": \"#0f0\"}, "
              + "{\"color\": \"blue\", \"value\": \"#00f\"}"
              + "]",
          resultSet.getPgJsonb("col1"));
      assertEquals(
          Value.pgJsonb(
              "["
                  + "{\"color\": \"red\", \"value\": \"#f00\"}, "
                  + "{\"color\": \"green\", \"value\": \"#0f0\"}, "
                  + "{\"color\": \"blue\", \"value\": \"#00f\"}"
                  + "]"),
          resultSet.getValue("col1"));

      assertTrue(resultSet.next());
      assertEquals("{}", resultSet.getPgJsonb("col1"));
      assertEquals(Value.pgJsonb("{}"), resultSet.getValue("col1"));

      assertTrue(resultSet.next());
      assertEquals("[]", resultSet.getPgJsonb("col1"));
      assertEquals(Value.pgJsonb("[]"), resultSet.getValue("col1"));

      assertTrue(resultSet.next());
      assertTrue(resultSet.isNull("col1"));

      assertFalse(resultSet.next());
    }
  }
}
