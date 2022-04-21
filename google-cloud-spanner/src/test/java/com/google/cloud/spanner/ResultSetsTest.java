/*
 * Copyright 2017 Google LLC
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

package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.ExecutorProvider;
import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.MoreExecutors;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ResultSets} */
@RunWith(JUnit4.class)
public class ResultSetsTest {

  @Test
  public void resultSetIteration() {
    double doubleVal = 1.2;
    BigDecimal bigDecimalVal = BigDecimal.valueOf(123, 2);
    String stringVal = "stringVal";
    String jsonVal = "{\"color\":\"red\",\"value\":\"#f00\"}";
    String byteVal = "101";
    long usecs = 32343;
    int year = 2018;
    int month = 5;
    int day = 26;
    boolean[] boolArray = {true, false, true, true, false};
    long[] longArray = {Long.MAX_VALUE, Long.MIN_VALUE, 0, 1, -1};
    double[] doubleArray = {Double.MIN_VALUE, Double.MAX_VALUE, 0, 1, -1, 1.2341};
    BigDecimal[] bigDecimalArray = {
      BigDecimal.valueOf(1, Integer.MAX_VALUE),
      BigDecimal.valueOf(1, Integer.MIN_VALUE),
      BigDecimal.ZERO,
      BigDecimal.TEN,
      BigDecimal.valueOf(3141592, 6)
    };
    ByteArray[] byteArray = {
      ByteArray.copyFrom("123"), ByteArray.copyFrom("456"), ByteArray.copyFrom("789")
    };
    Timestamp[] timestampArray = {
      Timestamp.ofTimeMicroseconds(101),
      Timestamp.ofTimeMicroseconds(202),
      Timestamp.ofTimeMicroseconds(303)
    };
    Date[] dateArray = {
      Date.fromYearMonthDay(1, 2, 3), Date.fromYearMonthDay(4, 5, 6), Date.fromYearMonthDay(7, 8, 9)
    };
    String[] stringArray = {"abc", "def", "ghi"};
    String[] jsonArray = {"{}", "{\"color\":\"red\",\"value\":\"#f00\"}", "[]"};

    Type type =
        Type.struct(
            Type.StructField.of("f1", Type.string()),
            Type.StructField.of("f2", Type.int64()),
            Type.StructField.of("f3", Type.bool()),
            Type.StructField.of("doubleVal", Type.float64()),
            Type.StructField.of("bigDecimalVal", Type.numeric()),
            Type.StructField.of("stringVal", Type.string()),
            Type.StructField.of("jsonVal", Type.json()),
            Type.StructField.of("byteVal", Type.bytes()),
            Type.StructField.of("timestamp", Type.timestamp()),
            Type.StructField.of("date", Type.date()),
            Type.StructField.of("boolArray", Type.array(Type.bool())),
            Type.StructField.of("longArray", Type.array(Type.int64())),
            Type.StructField.of("doubleArray", Type.array(Type.float64())),
            Type.StructField.of("bigDecimalArray", Type.array(Type.numeric())),
            Type.StructField.of("byteArray", Type.array(Type.bytes())),
            Type.StructField.of("timestampArray", Type.array(Type.timestamp())),
            Type.StructField.of("dateArray", Type.array(Type.date())),
            Type.StructField.of("stringArray", Type.array(Type.string())),
            Type.StructField.of("jsonArray", Type.array(Type.json())));
    Struct struct1 =
        Struct.newBuilder()
            .set("f1")
            .to("x")
            .set("f2")
            .to(2)
            .set("f3")
            .to(Value.bool(true))
            .set("doubleVal")
            .to(Value.float64(doubleVal))
            .set("bigDecimalVal")
            .to(Value.numeric(bigDecimalVal))
            .set("stringVal")
            .to(stringVal)
            .set("jsonVal")
            .to(Value.json(jsonVal))
            .set("byteVal")
            .to(Value.bytes(ByteArray.copyFrom(byteVal)))
            .set("timestamp")
            .to(Timestamp.ofTimeMicroseconds(usecs))
            .set("date")
            .to(Date.fromYearMonthDay(year, month, day))
            .set("boolArray")
            .to(Value.boolArray(boolArray))
            .set("longArray")
            .to(Value.int64Array(longArray))
            .set("doubleArray")
            .to(Value.float64Array(doubleArray))
            .set("bigDecimalArray")
            .to(Value.numericArray(Arrays.asList(bigDecimalArray)))
            .set("byteArray")
            .to(Value.bytesArray(Arrays.asList(byteArray)))
            .set("timestampArray")
            .to(Value.timestampArray(Arrays.asList(timestampArray)))
            .set("dateArray")
            .to(Value.dateArray(Arrays.asList(dateArray)))
            .set("stringArray")
            .to(Value.stringArray(Arrays.asList(stringArray)))
            .set("jsonArray")
            .to(Value.jsonArray(Arrays.asList(jsonArray)))
            .build();
    Struct struct2 =
        Struct.newBuilder()
            .set("f1")
            .to("y")
            .set("f2")
            .to(3)
            .set("f3")
            .to(Value.bool(null))
            .set("doubleVal")
            .to(Value.float64(doubleVal))
            .set("bigDecimalVal")
            .to(Value.numeric(bigDecimalVal))
            .set("stringVal")
            .to(stringVal)
            .set("jsonVal")
            .to(Value.json(jsonVal))
            .set("byteVal")
            .to(Value.bytes(ByteArray.copyFrom(byteVal)))
            .set("timestamp")
            .to(Timestamp.ofTimeMicroseconds(usecs))
            .set("date")
            .to(Date.fromYearMonthDay(year, month, day))
            .set("boolArray")
            .to(Value.boolArray(boolArray))
            .set("longArray")
            .to(Value.int64Array(longArray))
            .set("doubleArray")
            .to(Value.float64Array(doubleArray))
            .set("bigDecimalArray")
            .to(Value.numericArray(Arrays.asList(bigDecimalArray)))
            .set("byteArray")
            .to(Value.bytesArray(Arrays.asList(byteArray)))
            .set("timestampArray")
            .to(Value.timestampArray(Arrays.asList(timestampArray)))
            .set("dateArray")
            .to(Value.dateArray(Arrays.asList(dateArray)))
            .set("stringArray")
            .to(Value.stringArray(Arrays.asList(stringArray)))
            .set("jsonArray")
            .to(Value.jsonArray(Arrays.asList(jsonArray)))
            .build();
    ResultSet rs = ResultSets.forRows(type, Arrays.asList(struct1, struct2));

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> rs.getType());
    assertThat(e.getMessage()).contains("Must be preceded by a next() call");

    int columnIndex = 0;
    assertThat(rs.next()).isTrue();
    assertThat(rs.getType()).isEqualTo(type);
    assertThat(rs.getColumnCount()).isEqualTo(type.getStructFields().size());
    assertThat(rs.getColumnIndex("f1")).isEqualTo(0);
    assertThat(rs.getColumnType("nonexistent")).isNull();
    assertThat(rs.getColumnType("f1")).isEqualTo(Type.string());
    assertThat(rs.getColumnType(0)).isEqualTo(Type.string());
    assertThat(rs.getColumnIndex("f2")).isEqualTo(1);
    assertThat(rs.getColumnType("f2")).isEqualTo(Type.int64());
    assertThat(rs.getColumnType(1)).isEqualTo(Type.int64());
    assertThat(rs.getColumnIndex("f3")).isEqualTo(2);
    assertThat(rs.getColumnType("f3")).isEqualTo(Type.bool());
    assertThat(rs.getColumnType(2)).isEqualTo(Type.bool());
    assertThat(rs.getCurrentRowAsStruct()).isEqualTo(struct1);
    assertThat(rs.getString(columnIndex)).isEqualTo("x");
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.string("x"));
    assertThat(rs.getLong(columnIndex)).isEqualTo(2L);
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.int64(2L));
    assertThat(rs.getBoolean(columnIndex)).isTrue();
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.bool(true));
    assertThat(rs.getBoolean("f3")).isTrue();
    assertThat(rs.getValue("f3")).isEqualTo(Value.bool(true));
    assertThat(rs.getDouble("doubleVal")).isWithin(0.0).of(doubleVal);
    assertThat(rs.getValue("doubleVal").getFloat64()).isWithin(0.0).of(doubleVal);
    assertThat(rs.getDouble(columnIndex)).isWithin(0.0).of(doubleVal);
    assertThat(rs.getValue(columnIndex++).getFloat64()).isWithin(0.0).of(doubleVal);
    assertThat(rs.getBigDecimal("bigDecimalVal")).isEqualTo(new BigDecimal("1.23"));
    assertThat(rs.getValue("bigDecimalVal")).isEqualTo(Value.numeric(new BigDecimal("1.23")));
    assertThat(rs.getBigDecimal(columnIndex)).isEqualTo(new BigDecimal("1.23"));
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.numeric(new BigDecimal("1.23")));
    assertThat(rs.getString(columnIndex)).isEqualTo(stringVal);
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.string(stringVal));
    assertThat(rs.getString("stringVal")).isEqualTo(stringVal);
    assertThat(rs.getValue("stringVal")).isEqualTo(Value.string(stringVal));
    assertThat(rs.getJson(columnIndex)).isEqualTo(jsonVal);
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.json(jsonVal));
    assertThat(rs.getJson("jsonVal")).isEqualTo(jsonVal);
    assertThat(rs.getValue("jsonVal")).isEqualTo(Value.json(jsonVal));
    assertThat(rs.getBytes(columnIndex)).isEqualTo(ByteArray.copyFrom(byteVal));
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.bytes(ByteArray.copyFrom(byteVal)));
    assertThat(rs.getBytes("byteVal")).isEqualTo(ByteArray.copyFrom(byteVal));
    assertThat(rs.getValue("byteVal")).isEqualTo(Value.bytes(ByteArray.copyFrom(byteVal)));
    assertThat(rs.getTimestamp(columnIndex)).isEqualTo(Timestamp.ofTimeMicroseconds(usecs));
    assertThat(rs.getValue(columnIndex++))
        .isEqualTo(Value.timestamp(Timestamp.ofTimeMicroseconds(usecs)));
    assertThat(rs.getTimestamp("timestamp")).isEqualTo(Timestamp.ofTimeMicroseconds(usecs));
    assertThat(rs.getValue("timestamp"))
        .isEqualTo(Value.timestamp(Timestamp.ofTimeMicroseconds(usecs)));
    assertThat(rs.getDate(columnIndex)).isEqualTo(Date.fromYearMonthDay(year, month, day));
    assertThat(rs.getValue(columnIndex++))
        .isEqualTo(Value.date(Date.fromYearMonthDay(year, month, day)));
    assertThat(rs.getDate("date")).isEqualTo(Date.fromYearMonthDay(year, month, day));
    assertThat(rs.getValue("date")).isEqualTo(Value.date(Date.fromYearMonthDay(year, month, day)));
    assertThat(rs.getBooleanArray(columnIndex)).isEqualTo(boolArray);
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.boolArray(boolArray));
    assertThat(rs.getBooleanArray("boolArray")).isEqualTo(boolArray);
    assertThat(rs.getValue("boolArray")).isEqualTo(Value.boolArray(boolArray));
    assertThat(rs.getLongArray(columnIndex)).isEqualTo(longArray);
    assertThat(rs.getValue(columnIndex)).isEqualTo(Value.int64Array(longArray));
    assertThat(rs.getLongArray("longArray")).isEqualTo(longArray);
    assertThat(rs.getValue("longArray")).isEqualTo(Value.int64Array(longArray));
    assertThat(rs.getLongList(columnIndex++)).isEqualTo(Longs.asList(longArray));
    assertThat(rs.getLongList("longArray")).isEqualTo(Longs.asList(longArray));
    assertThat(rs.getDoubleArray(columnIndex)).usingTolerance(0.0).containsAtLeast(doubleArray);
    assertThat(rs.getValue(columnIndex)).isEqualTo(Value.float64Array(doubleArray));
    assertThat(rs.getDoubleArray("doubleArray"))
        .usingTolerance(0.0)
        .containsExactly(doubleArray)
        .inOrder();
    assertThat(rs.getValue("doubleArray")).isEqualTo(Value.float64Array(doubleArray));
    assertThat(rs.getDoubleList(columnIndex++)).isEqualTo(Doubles.asList(doubleArray));
    assertThat(rs.getDoubleList("doubleArray")).isEqualTo(Doubles.asList(doubleArray));
    assertThat(rs.getBigDecimalList(columnIndex)).isEqualTo(Arrays.asList(bigDecimalArray));
    assertThat(rs.getValue(columnIndex++))
        .isEqualTo(Value.numericArray(Arrays.asList(bigDecimalArray)));
    assertThat(rs.getBigDecimalList("bigDecimalArray")).isEqualTo(Arrays.asList(bigDecimalArray));
    assertThat(rs.getValue("bigDecimalArray"))
        .isEqualTo(Value.numericArray(Arrays.asList(bigDecimalArray)));
    assertThat(rs.getBytesList(columnIndex)).isEqualTo(Arrays.asList(byteArray));
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.bytesArray(Arrays.asList(byteArray)));
    assertThat(rs.getBytesList("byteArray")).isEqualTo(Arrays.asList(byteArray));
    assertThat(rs.getValue("byteArray")).isEqualTo(Value.bytesArray(Arrays.asList(byteArray)));
    assertThat(rs.getTimestampList(columnIndex)).isEqualTo(Arrays.asList(timestampArray));
    assertThat(rs.getValue(columnIndex++))
        .isEqualTo(Value.timestampArray(Arrays.asList(timestampArray)));
    assertThat(rs.getTimestampList("timestampArray")).isEqualTo(Arrays.asList(timestampArray));
    assertThat(rs.getValue("timestampArray"))
        .isEqualTo(Value.timestampArray(Arrays.asList(timestampArray)));
    assertThat(rs.getDateList(columnIndex)).isEqualTo(Arrays.asList(dateArray));
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.dateArray(Arrays.asList(dateArray)));
    assertThat(rs.getDateList("dateArray")).isEqualTo(Arrays.asList(dateArray));
    assertThat(rs.getValue("dateArray")).isEqualTo(Value.dateArray(Arrays.asList(dateArray)));
    assertThat(rs.getStringList(columnIndex)).isEqualTo(Arrays.asList(stringArray));
    assertThat(rs.getValue(columnIndex++)).isEqualTo(Value.stringArray(Arrays.asList(stringArray)));
    assertThat(rs.getStringList("stringArray")).isEqualTo(Arrays.asList(stringArray));
    assertThat(rs.getValue("stringArray")).isEqualTo(Value.stringArray(Arrays.asList(stringArray)));
    assertThat(rs.getJsonList(columnIndex)).isEqualTo(Arrays.asList(jsonArray));
    assertThat(rs.getJsonList("jsonArray")).isEqualTo(Arrays.asList(jsonArray));

    assertThat(rs.next()).isTrue();
    assertThat(rs.getCurrentRowAsStruct()).isEqualTo(struct2);
    assertThat(rs.getString(0)).isEqualTo("y");
    assertThat(rs.getLong(1)).isEqualTo(3L);
    assertThat(rs.isNull(2)).isTrue();
    assertThat(rs.next()).isFalse();

    UnsupportedOperationException unsupported =
        assertThrows(UnsupportedOperationException.class, () -> rs.getStats());
    assertThat(unsupported.getMessage())
        .contains("ResultSetStats are available only for results returned from analyzeQuery");
  }

  @Test
  public void resultSetIterationWithStructColumns() {
    Type nestedStructType = Type.struct(Type.StructField.of("g1", Type.string()));
    Type type =
        Type.struct(
            Type.StructField.of("f1", nestedStructType), Type.StructField.of("f2", Type.int64()));

    Struct value1 = Struct.newBuilder().set("g1").to("abc").build();

    Struct struct1 = Struct.newBuilder().set("f1").to(value1).set("f2").to((Long) null).build();
    UnsupportedOperationException e =
        assertThrows(
            UnsupportedOperationException.class,
            () -> ResultSets.forRows(type, Collections.singletonList(struct1)));
    assertThat(e.getMessage())
        .contains("STRUCT-typed columns are not supported inside ResultSets.");
  }

  @Test
  public void resultSetIterationWithArrayStructColumns() {
    Type nestedStructType = Type.struct(Type.StructField.of("g1", Type.string()));
    Type type =
        Type.struct(
            Type.StructField.of("f1", Type.array(nestedStructType)),
            Type.StructField.of("f2", Type.int64()));

    Struct value1 = Struct.newBuilder().set("g1").to("abc").build();

    List<Struct> arrayValue = Arrays.asList(value1, null);

    Struct struct1 =
        Struct.newBuilder()
            .set("f1")
            .toStructArray(nestedStructType, arrayValue)
            .set("f2")
            .to((Long) null)
            .build();

    Struct struct2 =
        Struct.newBuilder()
            .set("f1")
            .toStructArray(nestedStructType, null)
            .set("f2")
            .to(20)
            .build();

    ResultSet rs = ResultSets.forRows(type, Arrays.asList(struct1, struct2));

    assertThat(rs.next()).isTrue();
    assertThat(rs.getType()).isEqualTo(type);
    assertThat(rs.getColumnCount()).isEqualTo(2);

    assertThat(rs.getColumnIndex("f1")).isEqualTo(0);
    assertThat(rs.getColumnType("f1")).isEqualTo(Type.array(nestedStructType));
    assertThat(rs.getColumnType(0)).isEqualTo(Type.array(nestedStructType));

    assertThat(rs.getColumnIndex("f2")).isEqualTo(1);
    assertThat(rs.getColumnType("f2")).isEqualTo(Type.int64());
    assertThat(rs.getColumnType(1)).isEqualTo(Type.int64());

    assertThat(rs.getCurrentRowAsStruct()).isEqualTo(struct1);

    assertThat(rs.getStructList(0)).isEqualTo(arrayValue);
    assertThat(rs.getValue(0)).isEqualTo(Value.structArray(nestedStructType, arrayValue));
    assertThat(rs.getStructList("f1")).isEqualTo(arrayValue);
    assertThat(rs.getValue("f1")).isEqualTo(Value.structArray(nestedStructType, arrayValue));
    assertThat(rs.isNull(1)).isTrue();

    assertThat(rs.next()).isTrue();
    assertThat(rs.getCurrentRowAsStruct()).isEqualTo(struct2);

    assertThat(rs.isNull(0)).isTrue();
    assertThat(rs.isNull("f1")).isTrue();
    assertThat(rs.getLong(1)).isEqualTo(20);
    assertThat(rs.getValue(1)).isEqualTo(Value.int64(20));
    assertThat(rs.getLong("f2")).isEqualTo(20);
    assertThat(rs.getValue("f2")).isEqualTo(Value.int64(20));

    assertThat(rs.next()).isFalse();
  }

  @Test
  public void closeResultSet() {
    ResultSet rs =
        ResultSets.forRows(
            Type.struct(Type.StructField.of("f1", Type.string())),
            Collections.singletonList(Struct.newBuilder().set("f1").to("x").build()));
    rs.close();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> rs.getCurrentRowAsStruct());
    assertNotNull(e.getMessage());
  }

  @Test
  public void exceptionIfNextIsNotCalled() {
    ResultSet rs =
        ResultSets.forRows(
            Type.struct(Type.StructField.of("f1", Type.string())),
            Collections.singletonList(Struct.newBuilder().set("f1").to("x").build()));
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> rs.getCurrentRowAsStruct());
    assertNotNull(e.getMessage());
  }

  @Test
  public void testToAsyncResultSet() {
    ResultSet delegate =
        ResultSets.forRows(
            Type.struct(Type.StructField.of("f1", Type.string())),
            Collections.singletonList(Struct.newBuilder().set("f1").to("x").build()));

    final AtomicInteger count = new AtomicInteger();
    AsyncResultSet rs = ResultSets.toAsyncResultSet(delegate);
    ApiFuture<Void> fut =
        rs.setCallback(
            MoreExecutors.directExecutor(),
            resultSet -> {
              while (true) {
                switch (resultSet.tryNext()) {
                  case DONE:
                    return CallbackResponse.DONE;
                  case NOT_READY:
                    return CallbackResponse.CONTINUE;
                  case OK:
                    count.incrementAndGet();
                    assertThat(resultSet.getString("f1")).isEqualTo("x");
                }
              }
            });
    SpannerApiFutures.get(fut);
    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  public void testToAsyncResultSetWithExecProvider() {
    ResultSet delegate =
        ResultSets.forRows(
            Type.struct(Type.StructField.of("f1", Type.string())),
            Collections.singletonList(Struct.newBuilder().set("f1").to("x").build()));

    ExecutorProvider provider =
        new ExecutorProvider() {
          final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

          @Override
          public boolean shouldAutoClose() {
            return true;
          }

          @Override
          public ScheduledExecutorService getExecutor() {
            return executor;
          }
        };
    final AtomicInteger count = new AtomicInteger();
    AsyncResultSet rs = ResultSets.toAsyncResultSet(delegate, provider);
    ApiFuture<Void> fut =
        rs.setCallback(
            MoreExecutors.directExecutor(),
            resultSet -> {
              while (true) {
                switch (resultSet.tryNext()) {
                  case DONE:
                    return CallbackResponse.DONE;
                  case NOT_READY:
                    return CallbackResponse.CONTINUE;
                  case OK:
                    count.incrementAndGet();
                    assertThat(resultSet.getString("f1")).isEqualTo("x");
                }
              }
            });
    SpannerApiFutures.get(fut);
    assertThat(count.get()).isEqualTo(1);
    assertThat(provider.getExecutor().isShutdown()).isTrue();
  }

  @Test
  public void testToAsyncResultSetWithFuture() {
    ApiFuture<ResultSet> delegateFuture =
        ApiFutures.immediateFuture(
            ResultSets.forRows(
                Type.struct(Type.StructField.of("f1", Type.string())),
                Collections.singletonList(Struct.newBuilder().set("f1").to("x").build())));

    ExecutorProvider provider =
        new ExecutorProvider() {
          final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

          @Override
          public boolean shouldAutoClose() {
            return false;
          }

          @Override
          public ScheduledExecutorService getExecutor() {
            return executor;
          }
        };
    final AtomicInteger count = new AtomicInteger();
    AsyncResultSet rs = ResultSets.toAsyncResultSet(delegateFuture, provider);
    ApiFuture<Void> fut =
        rs.setCallback(
            MoreExecutors.directExecutor(),
            resultSet -> {
              while (true) {
                switch (resultSet.tryNext()) {
                  case DONE:
                    return CallbackResponse.DONE;
                  case NOT_READY:
                    return CallbackResponse.CONTINUE;
                  case OK:
                    count.incrementAndGet();
                    assertThat(resultSet.getString("f1")).isEqualTo("x");
                }
              }
            });
    SpannerApiFutures.get(fut);
    assertThat(count.get()).isEqualTo(1);
    assertThat(provider.getExecutor().isShutdown()).isFalse();
    provider.getExecutor().shutdown();
  }
}
