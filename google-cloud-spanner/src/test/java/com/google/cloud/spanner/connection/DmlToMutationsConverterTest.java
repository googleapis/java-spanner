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

import static com.google.cloud.spanner.connection.DmlToMutationsConverter.convert;
import static com.google.cloud.spanner.connection.DmlToMutationsConverter.parseValue;
import static org.junit.Assert.assertEquals;

import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.NullValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DmlToMutationsConverterTest {

  @Test
  public void testParseValue() {
    assertEquals(Value.bool(true), parseValue("true"));
    assertEquals(Value.bool(false), parseValue("false"));
    assertEquals(
        Value.untyped(
            com.google.protobuf.Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build()),
        parseValue("null"));
  }

  @Test
  public void testConvertInsertStatementWithParameters() {
    assertEquals(
        ImmutableList.of(Mutation.newInsertBuilder("my_table").set("col1").to("value1").build()),
        convert(
            Statement.newBuilder("insert into my_table (col1) values (@param1)")
                .bind("param1")
                .to("value1")
                .build()));
    assertEquals(
        ImmutableList.of(
            Mutation.newInsertBuilder("my_table")
                .set("col1")
                .to("value1")
                .set("col2")
                .to(100L)
                .set("col3")
                .to(Date.parseDate("2022-11-27"))
                .set("col4")
                .to(Timestamp.parseTimestamp("2022-11-27T14:47:00Z"))
                .set("col5")
                .to(Value.json("{\"key\": \"value\"}"))
                .build()),
        convert(
            Statement.newBuilder(
                    "insert into my_table (col1, col2, col3, col4, col5) "
                        + "values (@param1, @param2, @param3, @param4, @param5)")
                .bind("param1")
                .to("value1")
                .bind("param2")
                .to(100L)
                .bind("param3")
                .to(Date.parseDate("2022-11-27"))
                .bind("param4")
                .to(Timestamp.parseTimestamp("2022-11-27T14:47:00Z"))
                .bind("param5")
                .to(Value.json("{\"key\": \"value\"}"))
                .build()));
    assertEquals(
        ImmutableList.of(
            Mutation.newInsertBuilder("my_table").set("col1").to("value1").build(),
            Mutation.newInsertBuilder("my_table").set("col1").to("value2").build(),
            Mutation.newInsertBuilder("my_table").set("col1").to("value3").build()),
        convert(
            Statement.newBuilder(
                    "insert into my_table (col1) values (@param1), (@param2), (@param3)")
                .bind("param1")
                .to("value1")
                .bind("param2")
                .to("value2")
                .bind("param3")
                .to("value3")
                .build()));
  }

  @Test
  public void testConvertInsertStatementWithLiterals() {
    assertEquals(
        ImmutableList.of(
            Mutation.newInsertBuilder("my_table").set("col1").to(untyped("value1")).build()),
        convert(Statement.of("insert into my_table (col1) values ('value1')")));
    assertEquals(
        ImmutableList.of(
            Mutation.newInsertBuilder("my_table")
                .set("col1")
                .to(untyped("value1"))
                .set("col2")
                .to(
                    Value.untyped(
                        com.google.protobuf.Value.newBuilder()
                            .setStringValue("100")
                            .setNumberValue(100d)
                            .build()))
                .set("col3")
                .to(Date.parseDate("2022-11-27"))
                .set("col4")
                .to(Timestamp.parseTimestamp("2022-11-27T14:47:00Z"))
                .set("col5")
                .to(Value.json("{\"key\": \"value\"}"))
                .build()),
        convert(
            Statement.of(
                "insert into my_table (col1, col2, col3, col4, col5) "
                    + "values ('value1', 100, date '2022-11-27', timestamp '2022-11-27T14:47:00Z', json '{\"key\": \"value\"}')")));
    assertEquals(
        ImmutableList.of(
            Mutation.newInsertBuilder("my_table").set("col1").to(untyped("value1")).build(),
            Mutation.newInsertBuilder("my_table").set("col1").to(untyped("value2")).build(),
            Mutation.newInsertBuilder("my_table").set("col1").to(untyped("value3")).build()),
        convert(
            Statement.of("insert into my_table (col1) values ('value1'), ('value2'), ('value3')")));
  }

  @Test
  public void testConvertUpdateStatementWithParameters() {
    assertEquals(
        ImmutableList.of(
            Mutation.newUpdateBuilder("my_table")
                .set("id")
                .to(1L)
                .set("col1")
                .to("value1")
                .build()),
        convert(
            Statement.newBuilder("update my_table set col1=@param1 where id=@id")
                .bind("param1")
                .to("value1")
                .bind("id")
                .to(1L)
                .build()));
    assertEquals(
        ImmutableList.of(
            Mutation.newUpdateBuilder("my_table")
                .set("id1")
                .to("parent-key")
                .set("id2")
                .to(1L)
                .set("col1")
                .to("value1")
                .set("col2")
                .to(100L)
                .set("col3")
                .to(Date.parseDate("2022-11-27"))
                .set("col4")
                .to(Timestamp.parseTimestamp("2022-11-27T14:47:00Z"))
                .set("col5")
                .to(Value.json("{\"key\": \"value\"}"))
                .build()),
        convert(
            Statement.newBuilder(
                    "update my_table set col1=@param1, col2=@param2, col3=@param3, col4=@param4, col5=@param5 "
                        + "where id1=@id1 and id2=@id2")
                .bind("id1")
                .to("parent-key")
                .bind("id2")
                .to(1L)
                .bind("param1")
                .to("value1")
                .bind("param2")
                .to(100L)
                .bind("param3")
                .to(Date.parseDate("2022-11-27"))
                .bind("param4")
                .to(Timestamp.parseTimestamp("2022-11-27T14:47:00Z"))
                .bind("param5")
                .to(Value.json("{\"key\": \"value\"}"))
                .build()));
  }

  @Test
  public void testConvertUpdateStatementWithLiterals() {
    assertEquals(
        ImmutableList.of(
            Mutation.newUpdateBuilder("my_table")
                .set("id")
                .to(
                    Value.untyped(
                        com.google.protobuf.Value.newBuilder()
                            .setStringValue("1")
                            .setNumberValue(1d)
                            .build()))
                .set("col1")
                .to(untyped("value1"))
                .build()),
        convert(Statement.of("update my_table set col1 = 'value1' where id=1")));
    assertEquals(
        ImmutableList.of(
            Mutation.newUpdateBuilder("my_table")
                .set("id1")
                .to(
                    Value.untyped(
                        com.google.protobuf.Value.newBuilder()
                            .setStringValue("1")
                            .setNumberValue(1d)
                            .build()))
                .set("id2")
                .to(
                    Value.untyped(
                        com.google.protobuf.Value.newBuilder()
                            .setStringValue("2.2")
                            .setNumberValue(2.2d)
                            .build()))
                .set("col1")
                .to(untyped("value1"))
                .set("col2")
                .to(
                    Value.untyped(
                        com.google.protobuf.Value.newBuilder()
                            .setStringValue("100")
                            .setNumberValue(100d)
                            .build()))
                .set("col3")
                .to(Date.parseDate("2022-11-27"))
                .set("col4")
                .to(Timestamp.parseTimestamp("2022-11-27T14:47:00Z"))
                .set("col5")
                .to(Value.json("{\"key\": \"value\"}"))
                .build()),
        convert(
            Statement.of(
                "update my_table set col1='value1', col2=100, col3=date '2022-11-27', col4=timestamp '2022-11-27T14:47:00Z', col5=json '{\"key\": \"value\"}' "
                    + "where id1=1 and id2 = 2.2")));
  }

  static Value untyped(String value) {
    return Value.untyped(com.google.protobuf.Value.newBuilder().setStringValue(value).build());
  }
}
