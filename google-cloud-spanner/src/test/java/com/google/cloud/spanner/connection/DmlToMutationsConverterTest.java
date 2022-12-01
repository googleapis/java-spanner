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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.SpannerException;
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
                .to(Value.int64(100L))
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
        ImmutableList.of(Mutation.newUpdateBuilder("customerid").set("next_val").to(5L).build()),
        convert(
            Statement.newBuilder("update customerId set next_val= @p1 where next_val=@p2")
                .bind("p1")
                .to(5L)
                .bind("p2")
                .to(1L)
                .build()));
    assertEquals(
        ImmutableList.of(
            Mutation.newUpdateBuilder("my_table")
                .set("col1")
                .to("value1")
                .set("id")
                .to(1L)
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
                .set("id1")
                .to("parent-key")
                .set("id2")
                .to(1L)
                .build()),
        convert(
            Statement.newBuilder(
                    "update my_table set col1=@param1, col2=@param2, col3=@param3, col4=@param4, col5=@param5 "
                        + "where id1=@id1 and id2=@id2")
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
                .bind("id1")
                .to("parent-key")
                .bind("id2")
                .to(1L)
                .build()));
  }

  @Test
  public void testConvertUpdateStatementWithLiterals() {
    assertEquals(
        ImmutableList.of(
            Mutation.newUpdateBuilder("my_table")
                .set("col1")
                .to(untyped("value1"))
                .set("id")
                .to(Value.int64(1L))
                .build()),
        convert(Statement.of("update my_table set col1 = 'value1' where id=1")));
    assertEquals(
        ImmutableList.of(
            Mutation.newUpdateBuilder("my_table")
                .set("col1")
                .to(untyped("value1"))
                .set("col2")
                .to(Value.int64(100L))
                .set("col3")
                .to(Date.parseDate("2022-11-27"))
                .set("col4")
                .to(Timestamp.parseTimestamp("2022-11-27T14:47:00Z"))
                .set("col5")
                .to(Value.json("{\"key\": \"value\"}"))
                .set("id1")
                .to(Value.int64(1L))
                .set("id2")
                .to(Value.float64(2.2d))
                .build()),
        convert(
            Statement.of(
                "update my_table set col1='value1', col2=100, col3=date '2022-11-27', col4=timestamp '2022-11-27T14:47:00Z', col5=json '{\"key\": \"value\"}' "
                    + "where id1=1 and id2 = 2.2")));
  }

  @Test
  public void testConvertDeleteStatementWithParameters() {
    assertEquals(
        ImmutableList.of(Mutation.delete("my_table", Key.of(Value.int64(1L)))),
        convert(Statement.newBuilder("delete my_table where id=@id").bind("id").to(1L).build()));
    assertEquals(
        ImmutableList.of(
            Mutation.delete("my_table", Key.of(Value.string("parent-key"), Value.int64(1L)))),
        convert(
            Statement.newBuilder("delete from my_table " + "where id1=@id1 and id2=@id2")
                .bind("id1")
                .to("parent-key")
                .bind("id2")
                .to(1L)
                .build()));
  }

  @Test
  public void testConvertDeleteStatementWithLiterals() {
    assertEquals(
        ImmutableList.of(Mutation.delete("my_table", Key.of(Value.int64(1L)))),
        convert(Statement.of("delete from my_table where id=1")));
    assertEquals(
        ImmutableList.of(Mutation.delete("my_table", Key.of(Value.int64(1L), Value.float64(2.2d)))),
        convert(Statement.of("delete my_table " + "where id1=1 and id2 = 2.2")));
  }

  @Test
  public void testThenReturn() {
    assertIsThenReturnUnsupportedException(
        assertThrows(
            SpannerException.class,
            () ->
                convert(
                    Statement.of(
                        "insert into my_table (id, value) values (1, 'one') then return id"))));
    assertIsThenReturnUnsupportedException(
        assertThrows(
            SpannerException.class,
            () ->
                convert(
                    Statement.of("update my_table set value='two' where id=2 then return value"))));
    assertIsThenReturnUnsupportedException(
        assertThrows(
            SpannerException.class,
            () -> convert(Statement.of("delete my_table where id=3 then return value"))));
  }

  void assertIsThenReturnUnsupportedException(SpannerException exception) {
    assertTrue(
        exception.getMessage(),
        exception
            .getMessage()
            .contains(
                "'THEN RETURN' clauses are not supported for DML statements that should be converted to mutations"));
  }

  static Value untyped(String value) {
    return Value.untyped(com.google.protobuf.Value.newBuilder().setStringValue(value).build());
  }
}
