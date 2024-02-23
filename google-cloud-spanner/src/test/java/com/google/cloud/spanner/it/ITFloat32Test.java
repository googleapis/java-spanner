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

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.connection.ConnectionOptions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Category(ParallelIntegrationTest.class)
@RunWith(Parameterized.class)
public class ITFloat32Test {

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  @Parameterized.Parameters(name = "Dialect = {0}")
  public static List<DialectTestParameter> data() {
    return Arrays.asList(
        new DialectTestParameter(Dialect.GOOGLE_STANDARD_SQL),
        new DialectTestParameter(Dialect.POSTGRESQL));
  }

  @Parameterized.Parameter() public DialectTestParameter dialect;

  private static DatabaseClient googleStandardSQLClient;
  private static DatabaseClient postgreSQLClient;

  private static final String[] GOOGLE_STANDARD_SQL_SCHEMA =
      new String[] {
        "CREATE TABLE T ("
            + "  Key                 STRING(MAX) NOT NULL,"
            + "  Float32Value        FLOAT32,"
            + "  Float32ArrayValue   ARRAY<FLOAT32>,"
            + ") PRIMARY KEY (Key)"
      };

  private static final String[] POSTGRESQL_SCHEMA =
      new String[] {
        "CREATE TABLE T ("
            + "  Key                 VARCHAR PRIMARY KEY,"
            + "  Float32Value        REAL,"
            + "  Float32ArrayValue   REAL[]"
            + ")"
      };

  private static DatabaseClient client;

  private static boolean isUsingCloudDevel() {
    String jobType = System.getenv("JOB_TYPE");

    // Assumes that the jobType contains the string "cloud-devel" to signal that
    // the environment is cloud-devel.
    return !isNullOrEmpty(jobType) && jobType.contains("cloud-devel");
  }

  @BeforeClass
  public static void setUpDatabase()
      throws ExecutionException, InterruptedException, TimeoutException {
    if (!isUsingCloudDevel()) {
      return;
    }

    Database googleStandardSQLDatabase =
        env.getTestHelper().createTestDatabase(GOOGLE_STANDARD_SQL_SCHEMA);

    googleStandardSQLClient = env.getTestHelper().getDatabaseClient(googleStandardSQLDatabase);

    Database postgreSQLDatabase =
        env.getTestHelper()
            .createTestDatabase(Dialect.POSTGRESQL, Arrays.asList(POSTGRESQL_SCHEMA));
    postgreSQLClient = env.getTestHelper().getDatabaseClient(postgreSQLDatabase);
  }

  @Before
  public void before() {
    assumeFalse("Emulator does not support FLOAT32 yet", isUsingEmulator());

    client =
        dialect.dialect == Dialect.GOOGLE_STANDARD_SQL ? googleStandardSQLClient : postgreSQLClient;
  }

  @AfterClass
  public static void tearDown() throws Exception {
    ConnectionOptions.closeSpanner();
  }

  /** Sequence used to generate unique keys. */
  private static int seq;

  private static String uniqueString() {
    return String.format("k%04d", seq++);
  }

  private String lastKey;

  private Timestamp write(Mutation m) {
    return client.write(Collections.singletonList(m));
  }

  private Mutation.WriteBuilder baseInsert() {
    return Mutation.newInsertOrUpdateBuilder("T").set("Key").to(lastKey = uniqueString());
  }

  private Struct readRow(String table, String key, String... columns) {
    return client
        .singleUse(TimestampBound.strong())
        .readRow(table, Key.of(key), Arrays.asList(columns));
  }

  private Struct readLastRow(String... columns) {
    return readRow("T", lastKey, columns);
  }

  @Test
  public void writeFloat32() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    write(baseInsert().set("Float32Value").to(2.0f).build());
    Struct row = readLastRow("Float32Value");
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getFloat(0)).isWithin(0.0f).of(2.0f);
  }

  @Ignore("Needs a fix in the backend.")
  @Test
  public void writeFloat32NonNumbers() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    write(baseInsert().set("Float32Value").to(Float.NEGATIVE_INFINITY).build());
    Struct row = readLastRow("Float32Value");
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getFloat(0)).isNegativeInfinity();

    write(baseInsert().set("Float32Value").to(Float.POSITIVE_INFINITY).build());
    row = readLastRow("Float32Value");
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getFloat(0)).isPositiveInfinity();

    write(baseInsert().set("Float32Value").to(Float.NaN).build());
    row = readLastRow("Float32Value");
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getFloat(0)).isNaN();
  }

  @Test
  public void writeFloat32Null() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    write(baseInsert().set("Float32Value").to((Float) null).build());
    Struct row = readLastRow("Float32Value");
    assertThat(row.isNull(0)).isTrue();
  }

  @Test
  public void writeFloat32ArrayNull() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    write(baseInsert().set("Float32ArrayValue").toFloat32Array((float[]) null).build());
    Struct row = readLastRow("Float32ArrayValue");
    assertThat(row.isNull(0)).isTrue();
  }

  @Test
  public void writeFloat32ArrayEmpty() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    write(baseInsert().set("Float32ArrayValue").toFloat32Array(new float[] {}).build());
    Struct row = readLastRow("Float32ArrayValue");
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getFloatList(0)).containsExactly();
  }

  @Test
  public void writeFloat32Array() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    write(
        baseInsert()
            .set("Float32ArrayValue")
            .toFloat32Array(Arrays.asList(null, 1.0f, 2.0f))
            .build());
    Struct row = readLastRow("Float32ArrayValue");
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getFloatList(0)).containsExactly(null, 1.0f, 2.0f).inOrder();
    try {
      row.getFloatArray(0);
      fail("Expected exception");
    } catch (NullPointerException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void writeFloat32ArrayNoNulls() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    write(baseInsert().set("Float32ArrayValue").toFloat32Array(Arrays.asList(1.0f, 2.0f)).build());
    Struct row = readLastRow("Float32ArrayValue");
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getFloatArray(0).length).isEqualTo(2);
    assertThat(row.getFloatArray(0)[0]).isWithin(0.0f).of(1.0f);
    assertThat(row.getFloatArray(0)[1]).isWithin(0.0f).of(2.0f);
  }

  private String getInsertStatementWithLiterals() {
    String statement = "INSERT INTO T (Key, Float32Value, Float32ArrayValue) VALUES ";

    if (dialect.dialect == Dialect.POSTGRESQL) {
      statement +=
          "('dml1', 3.14::float8, array[1.1]::float4[]), "
              + "('dml2', '3.14'::float4, array[3.14::float4, 3.14::float8]::float4[]), "
              + "('dml3', 'nan'::real, array['inf'::real, (3.14::float8)::float4, 1.2, '-inf']::float4[]), "
              + "('dml4', 1.175494e-38::real, array[1.175494e-38, 3.4028234e38, -3.4028234e38]::real[]), "
              + "('dml5', null, null)";
    } else {
      statement +=
          "('dml1', 3.14, [CAST(1.1 AS FLOAT32)]), "
              + "('dml2', CAST('3.14' AS FLOAT32), array[CAST(3.14 AS FLOAT32), 3.14]), "
              + "('dml3', CAST('nan' AS FLOAT32), array[CAST('inf' AS FLOAT32), CAST(CAST(3.14 AS FLOAT64) AS FLOAT32), 1.2, CAST('-inf' AS FLOAT32)]), "
              + "('dml4', 1.175494e-38, [CAST(1.175494e-38 AS FLOAT32), 3.4028234e38, -3.4028234e38]), "
              + "('dml5', null, null)";
    }
    return statement;
  }

  @Test
  public void float32Literals() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(Statement.of(getInsertStatementWithLiterals()));
              return null;
            });

    verifyContents("dml");
  }

  private String getInsertStatementWithParameters() {
    String pgStatement =
        "INSERT INTO T (Key, Float32Value, Float32ArrayValue) VALUES "
            + "('param1', $1, $2), "
            + "('param2', $3, $4), "
            + "('param3', $5, $6), "
            + "('param4', $7, $8), "
            + "('param5', $9, $10)";

    return (dialect.dialect == Dialect.POSTGRESQL) ? pgStatement : pgStatement.replace("$", "@p");
  }

  @Ignore("Needs a fix in the backend.")
  @Test
  public void float32Parameter() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(
                  Statement.newBuilder(getInsertStatementWithParameters())
                      .bind("p1")
                      .to(Value.float32(3.14f))
                      .bind("p2")
                      .to(Value.float32Array(Arrays.asList(1.1f)))
                      .bind("p3")
                      .to(Value.float32(3.14f))
                      .bind("p4")
                      .to(Value.float32Array(new float[] {3.14f, 3.14f}))
                      .bind("p5")
                      .to(Value.float32(Float.NaN))
                      .bind("p6")
                      .to(
                          Value.float32Array(
                              Arrays.asList(
                                  Float.POSITIVE_INFINITY, 3.14f, 1.2f, Float.POSITIVE_INFINITY)))
                      .bind("p7")
                      .to(Value.float32(Float.MIN_NORMAL))
                      .bind("p8")
                      .to(
                          Value.float32Array(
                              Arrays.asList(
                                  Float.MIN_NORMAL, Float.MAX_VALUE, -1 * Float.MAX_VALUE)))
                      .bind("p9")
                      .to(Value.float32(null))
                      .bind("p10")
                      .to(Value.float32Array((float[]) null))
                      .build());
              return null;
            });

    verifyContents("param");
  }

  @Test
  public void float32UntypedParameter() {
    assumeTrue("FLOAT32 is currently only supported in cloud-devel", isUsingCloudDevel());

    // TODO: Verify before submitting.
    assumeTrue(
        "This test is currently only supported in GoogleSQL",
        dialect.dialect == Dialect.GOOGLE_STANDARD_SQL);

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(
                  Statement.newBuilder(
                          "INSERT INTO T (Key, Float32Value, Float32ArrayValue) VALUES "
                              + "('untyped1', CAST(@p1 AS FLOAT32), CAST(@p2 AS ARRAY<FLOAT32>))")
                      .bind("p1")
                      .to(
                          Value.untyped(
                              com.google.protobuf.Value.newBuilder()
                                  .setNumberValue((double) 3.14f)
                                  .build()))
                      .bind("p2")
                      .to(
                          Value.untyped(
                              com.google.protobuf.Value.newBuilder()
                                  .setListValue(
                                      com.google.protobuf.ListValue.newBuilder()
                                          .addValues(
                                              com.google.protobuf.Value.newBuilder()
                                                  .setNumberValue((double) Float.MIN_NORMAL)))
                                  .build()))
                      .build());
              return null;
            });

    Struct row = readRow("T", "untyped1", "Float32Value", "Float32ArrayValue");
    assertThat(row.isNull("Float32Value")).isFalse();
    assertThat(row.getFloat("Float32Value")).isWithin(0.001f).of(3.14f);
    assertThat(row.isNull("Float32ArrayValue")).isFalse();
    assertThat(row.getFloatList("Float32ArrayValue")).hasSize(1);
    assertThat(row.getFloatList("Float32ArrayValue").get(0)).isWithin(0.001f).of(Float.MIN_NORMAL);
  }

  private void verifyContents(String keyPrefix) {
    try (ResultSet resultSet =
        client
            .singleUse()
            .executeQuery(
                Statement.of(
                    "SELECT Key AS key, Float32Value AS float32value, Float32ArrayValue AS float32arrayvalue FROM T WHERE Key LIKE '{keyPrefix}%' ORDER BY key"
                        .replace("{keyPrefix}", keyPrefix)))) {

      assertTrue(resultSet.next());

      assertThat(resultSet.getFloat("float32value")).isWithin(0.001f).of(3.14f);
      assertThat(resultSet.getValue("float32value")).isEqualTo(Value.float32(3.14f));
      ;
      assertThat(resultSet.getFloatArray("float32arrayvalue")).isEqualTo(new float[] {1.1f});

      assertTrue(resultSet.next());

      assertThat(resultSet.getFloat("float32value")).isWithin(0.001f).of(3.14f);
      assertThat(resultSet.getFloatList("float32arrayvalue")).containsExactly(3.14f, 3.14f);
      assertThat(resultSet.getValue("float32arrayvalue"))
          .isEqualTo(Value.float32Array(new float[] {3.14f, 3.14f}));

      assertTrue(resultSet.next());
      assertThat(resultSet.getFloat("float32value")).isNaN();
      assertThat(resultSet.getValue("float32value").getFloat32()).isNaN();
      assertThat(resultSet.getFloatList("float32arrayvalue"))
          .containsExactly(Float.POSITIVE_INFINITY, 3.14f, 1.2f, Float.NEGATIVE_INFINITY);
      assertThat(resultSet.getValue("float32arrayvalue"))
          .isEqualTo(
              Value.float32Array(
                  Arrays.asList(Float.POSITIVE_INFINITY, 3.14f, 1.2f, Float.NEGATIVE_INFINITY)));

      assertTrue(resultSet.next());
      assertThat(resultSet.getFloat("float32value")).isWithin(0.1f).of(Float.MIN_NORMAL);
      assertThat(resultSet.getValue("float32value").getFloat32())
          .isWithin(0.1f)
          .of(Float.MIN_NORMAL);
      assertThat(resultSet.getFloatList("float32arrayvalue")).hasSize(3);
      assertThat(resultSet.getFloatList("float32arrayvalue").get(0))
          .isWithin(0.1f)
          .of(Float.MIN_NORMAL);
      assertThat(resultSet.getFloatList("float32arrayvalue").get(1)).isEqualTo(Float.MAX_VALUE);
      assertThat(resultSet.getFloatList("float32arrayvalue").get(2))
          .isEqualTo(-1 * Float.MAX_VALUE);
      assertThat(resultSet.getValue("float32arrayvalue").getFloat32Array()).hasSize(3);

      assertTrue(resultSet.next());
      assertTrue(resultSet.isNull("float32value"));
      assertTrue(resultSet.isNull("float32arrayvalue"));

      assertFalse(resultSet.next());
    }
  }
}
