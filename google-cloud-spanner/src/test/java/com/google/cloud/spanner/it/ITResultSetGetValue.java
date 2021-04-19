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

package com.google.cloud.spanner.it;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.primitives.Doubles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITResultSetGetValue {

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  // For floats / doubles comparison
  private static final double DELTA = 1e-15;
  private static final String TABLE_NAME = "TestTable";
  private static DatabaseClient databaseClient;

  @BeforeClass
  public static void beforeClass() {
    final RemoteSpannerHelper testHelper = env.getTestHelper();
    final Database database =
        testHelper.createTestDatabase(
            "CREATE TABLE "
                + TABLE_NAME
                + "("
                + "Id INT64 NOT NULL,"
                + "bool BOOL,"
                + "int64 INT64,"
                + "float64 FLOAT64,"
                + "numeric NUMERIC,"
                + "string STRING(MAX),"
                + "bytes BYTES(MAX),"
                + "timestamp TIMESTAMP,"
                + "date DATE,"
                + "boolArray ARRAY<BOOL>,"
                + "int64Array ARRAY<INT64>,"
                + "float64Array ARRAY<FLOAT64>,"
                + "numericArray ARRAY<NUMERIC>,"
                + "stringArray ARRAY<STRING(MAX)>,"
                + "bytesArray ARRAY<BYTES(MAX)>,"
                + "timestampArray ARRAY<TIMESTAMP>,"
                + "dateArray ARRAY<DATE>"
                + ") PRIMARY KEY (Id)");

    databaseClient = testHelper.getDatabaseClient(database);
  }

  @Test
  public void testReadNonNullValues() {
    databaseClient.write(
        Collections.singletonList(
            Mutation.newInsertBuilder(TABLE_NAME)
                .set("Id")
                .to(1L)
                .set("bool")
                .to(true)
                .set("int64")
                .to(10L)
                .set("float64")
                .to(20D)
                .set("numeric")
                .to(new BigDecimal("30"))
                .set("string")
                .to("stringValue")
                .set("bytes")
                .to(ByteArray.copyFrom("bytesValue"))
                .set("timestamp")
                .to(Timestamp.ofTimeSecondsAndNanos(1, 0))
                .set("date")
                .to(Date.fromYearMonthDay(2021, 1, 2))
                .set("boolArray")
                .toBoolArray(new boolean[] {false, true})
                .set("int64Array")
                .toInt64Array(new long[] {100L, 200L})
                .set("float64Array")
                .toFloat64Array(new double[] {1000D, 2000D})
                .set("numericArray")
                .toNumericArray(Arrays.asList(new BigDecimal("10000"), new BigDecimal("20000")))
                .set("stringArray")
                .toStringArray(Arrays.asList("string1", "string2"))
                .set("bytesArray")
                .toBytesArray(
                    Arrays.asList(ByteArray.copyFrom("bytes1"), ByteArray.copyFrom("bytes2")))
                .set("timestampArray")
                .toTimestampArray(
                    Arrays.asList(
                        Timestamp.ofTimeSecondsAndNanos(10, 0),
                        Timestamp.ofTimeSecondsAndNanos(20, 0)))
                .set("dateArray")
                .toDateArray(
                    Arrays.asList(
                        Date.fromYearMonthDay(2021, 2, 3), Date.fromYearMonthDay(2021, 3, 4)))
                .build()));

    try (ResultSet resultSet =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of("SELECT * FROM " + TABLE_NAME + " WHERE Id = 1"))) {
      resultSet.next();

      assertEquals(Value.int64(1L), resultSet.getValue("Id"));
      assertEquals(Value.bool(true), resultSet.getValue("bool"));
      assertEquals(Value.int64(10L), resultSet.getValue("int64"));
      assertEquals(20D, resultSet.getValue("float64").getFloat64(), 1e-15);
      assertEquals(Value.numeric(new BigDecimal("30")), resultSet.getValue("numeric"));
      assertEquals(Value.string("stringValue"), resultSet.getValue("string"));
      assertEquals(Value.bytes(ByteArray.copyFrom("bytesValue")), resultSet.getValue("bytes"));
      assertEquals(
          Value.timestamp(Timestamp.ofTimeSecondsAndNanos(1, 0)), resultSet.getValue("timestamp"));
      assertEquals(Value.date(Date.fromYearMonthDay(2021, 1, 2)), resultSet.getValue("date"));
      assertEquals(Value.boolArray(new boolean[] {false, true}), resultSet.getValue("boolArray"));
      assertEquals(Value.int64Array(new long[] {100L, 200L}), resultSet.getValue("int64Array"));
      assertArrayEquals(
          new double[] {1000D, 2000D},
          Doubles.toArray(resultSet.getValue("float64Array").getFloat64Array()),
          1e-15);
      assertEquals(
          Value.numericArray(Arrays.asList(new BigDecimal("10000"), new BigDecimal("20000"))),
          resultSet.getValue("numericArray"));
      assertEquals(
          Value.stringArray(Arrays.asList("string1", "string2")),
          resultSet.getValue("stringArray"));
      assertEquals(
          Value.bytesArray(
              Arrays.asList(ByteArray.copyFrom("bytes1"), ByteArray.copyFrom("bytes2"))),
          resultSet.getValue("bytesArray"));
      assertEquals(
          Value.timestampArray(
              Arrays.asList(
                  Timestamp.ofTimeSecondsAndNanos(10, 0), Timestamp.ofTimeSecondsAndNanos(20, 0))),
          resultSet.getValue("timestampArray"));
      assertEquals(
          Value.dateArray(
              Arrays.asList(Date.fromYearMonthDay(2021, 2, 3), Date.fromYearMonthDay(2021, 3, 4))),
          resultSet.getValue("dateArray"));
    }
  }

  @Test
  public void testReadNullInArrays() {
    databaseClient.write(
        Collections.singletonList(
            Mutation.newInsertBuilder(TABLE_NAME)
                .set("Id")
                .to(2L)
                .set("numericArray")
                .toNumericArray(Arrays.asList(new BigDecimal("10000"), null))
                .set("stringArray")
                .toStringArray(Arrays.asList(null, "string2"))
                .set("bytesArray")
                .toBytesArray(Arrays.asList(ByteArray.copyFrom("bytes1"), null))
                .set("timestampArray")
                .toTimestampArray(Arrays.asList(null, Timestamp.ofTimeSecondsAndNanos(20, 0)))
                .set("dateArray")
                .toDateArray(Arrays.asList(Date.fromYearMonthDay(2021, 2, 3), null))
                .build()));

    try (ResultSet resultSet =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of("SELECT * FROM " + TABLE_NAME + " WHERE Id = 2"))) {
      resultSet.next();

      assertEquals(Value.int64(2L), resultSet.getValue("Id"));
      assertEquals(
          Value.numericArray(Arrays.asList(new BigDecimal("10000"), null)),
          resultSet.getValue("numericArray"));
      assertEquals(
          Value.stringArray(Arrays.asList(null, "string2")), resultSet.getValue("stringArray"));
      assertEquals(
          Value.bytesArray(Arrays.asList(ByteArray.copyFrom("bytes1"), null)),
          resultSet.getValue("bytesArray"));
      assertEquals(
          Value.timestampArray(Arrays.asList(null, Timestamp.ofTimeSecondsAndNanos(20, 0))),
          resultSet.getValue("timestampArray"));
      assertEquals(
          Value.dateArray(Arrays.asList(Date.fromYearMonthDay(2021, 2, 3), null)),
          resultSet.getValue("dateArray"));
    }
  }
}
