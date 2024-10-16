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

package com.google.cloud.spanner.it;

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.*;
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Category(ParallelIntegrationTest.class)
@RunWith(Parameterized.class)
public class ITIntervalTest {

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
        "CREATE TABLE IntervalTable (\n"
            + "  key STRING(MAX),\n"
            + "  slo_days INT64,\n"
            + "  update_time TIMESTAMP,\n"
            + "  expiry_days INT64 AS (EXTRACT(DAY FROM make_interval(day => GREATEST(LEAST(slo_days, 365), 1)))),\n"
            + "  interval_array_len bigint AS (ARRAY_LENGTH(ARRAY<INTERVAL>[INTERVAL '1-2 3 4:5:6' YEAR TO SECOND]))\n"
            + ") PRIMARY KEY (key);"
      };

  private static final String[] POSTGRESQL_SCHEMA =
      new String[] {
        "CREATE TABLE IntervalTable (\n"
            + "  key text primary key,\n"
            + "  slo_days bigint,\n"
            + "  update_time timestamptz,\n"
            + "  expiry_days bigint GENERATED ALWAYS AS (EXTRACT(DAY FROM make_interval(days =>GREATEST(LEAST(slo_days, 365), 1)))) STORED,\n"
            + "  interval_array_len bigint GENERATED ALWAYS AS (ARRAY_LENGTH(ARRAY[INTERVAL '1-2 3 4:5:6'], 1)) STORED\n"
            + ");"
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
    assumeTrue("Interval is supported only in Cloud-Devel for now", isUsingCloudDevel());
    assumeFalse("Emulator does not support Interval yet", isUsingEmulator());

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
  public void writeToTableWithIntervalExpressions() {
    write(
        baseInsert()
            .set("slo_days")
            .to(5)
            .set("update_time")
            .to(Timestamp.ofTimeMicroseconds(12345678L))
            .build());
    Struct row = readLastRow("expiryDays", "interval_array_len");
    assertFalse(row.isNull(0));
    assertEquals(5, row.getLong(0));
    assertFalse(row.isNull(1));
    assertEquals(1, row.getLong(1));
  }

  @Test
  public void queryInterval() {
    try (ResultSet resultSet =
        client
            .singleUse()
            .executeQuery(Statement.of("SELECT INTERVAL '1' DAY + INTERVAL '1' MONTH AS Col1"))) {
      assertTrue(resultSet.next());
      assertTrue(resultSet.getInterval(0).equals(Interval.fromMonthsDaysMicros(1, 1, 0)));
    }
  }

  @Test
  public void queryWithUntypedIntervalParam() {
    String query;
    if (dialect.dialect == Dialect.POSTGRESQL) {
      query = "SELECT (INTERVAL '1' DAY > $1) AS Col1";
    } else {
      query = "SELECT (INTERVAL '1' DAY > @p1) AS Col1";
    }

    try (ResultSet resultSet =
        client
            .singleUse()
            .executeQuery(
                Statement.newBuilder(query)
                    .bind("p1")
                    .to(
                        Value.untyped(
                            com.google.protobuf.Value.newBuilder()
                                .setStringValue("PT1.5S")
                                .build()))
                    .build())) {
      assertTrue(resultSet.next());
      assertTrue(resultSet.getBoolean(0));
    }
  }

  @Test
  public void queryIntervalArray() {
    String query =
        "SELECT ARRAY[CAST('P1Y2M3DT4H5M6.789123S' AS INTERVAL), null, CAST('P-1Y-2M-3DT-4H-5M-6.789123S' AS INTERVAL)] AS Col1";
    try (ResultSet resultSet = client.singleUse().executeQuery(Statement.of(query))) {
      assertTrue(resultSet.next());
      assertTrue(
          Arrays.asList(
                  Interval.parseFromString("P1Y2M3DT4H5M6.789123S"),
                  null,
                  Interval.parseFromString("P-1Y-2M-3DT-4H-5M-6.789123S"))
              .equals(resultSet.getIntervalList(0)));
    }
  }
}
