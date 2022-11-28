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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.Mutation;
import com.google.spanner.v1.Mutation.OperationCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConvertDmlToMutationsTest extends AbstractMockServerTest {

  @Test
  public void testSelect() {
    try (Connection connection = createConnection()) {
      connection.setConvertDmlToMutations(true);
      try (ResultSet resultSet = connection.executeQuery(SELECT_COUNT_STATEMENT)) {
        assertTrue(resultSet.next());
        assertEquals(0L, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(0, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testInsertSingleWithParameters() {
    String sql = "insert into my_table (id, value) values (@id, @value)";
    Statement statement =
        Statement.newBuilder(sql).bind("id").to(1L).bind("value").to("value1").build();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.INSERT, mutation.getOperationCase());
    assertEquals(1, mutation.getInsert().getValuesCount());
    assertEquals("my_table", mutation.getInsert().getTable());
    assertEquals(2, mutation.getInsert().getColumnsCount());
    assertEquals("id", mutation.getInsert().getColumns(0));
    assertEquals("value", mutation.getInsert().getColumns(1));
    assertEquals("1", mutation.getInsert().getValues(0).getValues(0).getStringValue());
    assertEquals("value1", mutation.getInsert().getValues(0).getValues(1).getStringValue());
  }

  @Test
  public void testInsertMultipleWithParameters() {
    String sql =
        "insert into my_table (id, value) values (@id1, @value1), (@id2, @value2),\n(@id3, @value3)";
    Statement statement =
        Statement.newBuilder(sql)
            .bind("id1")
            .to(1L)
            .bind("value1")
            .to("value1")
            .bind("id2")
            .to(2L)
            .bind("value2")
            .to("value2")
            .bind("id3")
            .to(3L)
            .bind("value3")
            .to("value3")
            .build();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(3L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(3, mutation.getInsert().getValuesCount());
    assertEquals(OperationCase.INSERT, mutation.getOperationCase());
    assertEquals("my_table", mutation.getInsert().getTable());
    assertEquals(2, mutation.getInsert().getColumnsCount());
    assertEquals("id", mutation.getInsert().getColumns(0));
    assertEquals("value", mutation.getInsert().getColumns(1));
    int rowNum = 1;
    for (ListValue listValue : mutation.getInsert().getValuesList()) {
      assertEquals(String.valueOf(rowNum), listValue.getValues(0).getStringValue());
      assertEquals("value" + rowNum, listValue.getValues(1).getStringValue());
      rowNum++;
    }
  }

  @Test
  public void testInsertSingleWithLiterals() {
    String sql = "insert into my_table (id, value) values (1, 'value1')";
    Statement statement = Statement.of(sql);
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.INSERT, mutation.getOperationCase());
    assertEquals(1, mutation.getInsert().getValuesCount());
    assertEquals("my_table", mutation.getInsert().getTable());
    assertEquals(2, mutation.getInsert().getColumnsCount());
    assertEquals("id", mutation.getInsert().getColumns(0));
    assertEquals("value", mutation.getInsert().getColumns(1));
    assertEquals("1", mutation.getInsert().getValues(0).getValues(0).getStringValue());
    assertEquals("value1", mutation.getInsert().getValues(0).getValues(1).getStringValue());
  }

  @Test
  public void testInsertMultipleWithLiterals() {
    String sql =
        "insert into my_table (id, value) values (@id1, @value1), (@id2, @value2),\n(@id3, @value3)";
    Statement statement =
        Statement.newBuilder(sql)
            .bind("id1")
            .to(1L)
            .bind("value1")
            .to("value1")
            .bind("id2")
            .to(2L)
            .bind("value2")
            .to("value2")
            .bind("id3")
            .to(3L)
            .bind("value3")
            .to("value3")
            .build();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(3L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(3, mutation.getInsert().getValuesCount());
    assertEquals(OperationCase.INSERT, mutation.getOperationCase());
    assertEquals("my_table", mutation.getInsert().getTable());
    assertEquals(2, mutation.getInsert().getColumnsCount());
    assertEquals("id", mutation.getInsert().getColumns(0));
    assertEquals("value", mutation.getInsert().getColumns(1));
    int rowNum = 1;
    for (ListValue listValue : mutation.getInsert().getValuesList()) {
      assertEquals(String.valueOf(rowNum), listValue.getValues(0).getStringValue());
      assertEquals("value" + rowNum, listValue.getValues(1).getStringValue());
      rowNum++;
    }
  }

  @Test
  public void testUpdateWithParameters() {
    String sql = "update my_table set value=@value where id=@id";
    Statement statement =
        Statement.newBuilder(sql).bind("id").to(1L).bind("value").to("value1").build();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.UPDATE, mutation.getOperationCase());
    assertEquals(1, mutation.getUpdate().getValuesCount());
    assertEquals("my_table", mutation.getUpdate().getTable());
    assertEquals(2, mutation.getUpdate().getColumnsCount());
    assertEquals("id", mutation.getUpdate().getColumns(0));
    assertEquals("value", mutation.getUpdate().getColumns(1));
    assertEquals("1", mutation.getUpdate().getValues(0).getValues(0).getStringValue());
    assertEquals("value1", mutation.getUpdate().getValues(0).getValues(1).getStringValue());
  }

  @Test
  public void testUpdateWithLiterals() {
    String sql = "update my_table set value='value1' where id=1";
    Statement statement = Statement.of(sql);
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.UPDATE, mutation.getOperationCase());
    assertEquals(1, mutation.getUpdate().getValuesCount());
    assertEquals("my_table", mutation.getUpdate().getTable());
    assertEquals(2, mutation.getUpdate().getColumnsCount());
    assertEquals("id", mutation.getUpdate().getColumns(0));
    assertEquals("value", mutation.getUpdate().getColumns(1));
    assertEquals("1", mutation.getUpdate().getValues(0).getValues(0).getStringValue());
    assertEquals("value1", mutation.getUpdate().getValues(0).getValues(1).getStringValue());
  }

  @Test
  public void testDeleteWithParameters() {
    String sql = "delete my_table where id=@id";
    Statement statement = Statement.newBuilder(sql).bind("id").to(1L).build();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.DELETE, mutation.getOperationCase());
    assertEquals(1, mutation.getDelete().getKeySet().getKeysCount());
    assertEquals("my_table", mutation.getDelete().getTable());
    assertEquals(1, mutation.getDelete().getKeySet().getKeys(0).getValuesCount());
    assertEquals("1", mutation.getDelete().getKeySet().getKeys(0).getValues(0).getStringValue());
  }

  @Test
  public void testDeleteWithMultipleParameters() {
    String sql = "delete my_table where id_parent=@parent and id_child=@child";
    Statement statement =
        Statement.newBuilder(sql).bind("parent").to(1L).bind("child").to(2.2d).build();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.DELETE, mutation.getOperationCase());
    assertEquals(1, mutation.getDelete().getKeySet().getKeysCount());
    assertEquals("my_table", mutation.getDelete().getTable());
    assertEquals(2, mutation.getDelete().getKeySet().getKeys(0).getValuesCount());
    assertEquals("1", mutation.getDelete().getKeySet().getKeys(0).getValues(0).getStringValue());
    assertEquals(
        2.2d, mutation.getDelete().getKeySet().getKeys(0).getValues(1).getNumberValue(), 0.0d);
  }

  @Test
  public void testDeleteWithLiterals() {
    String sql = "delete from my_table where id=1";
    Statement statement = Statement.of(sql);
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.DELETE, mutation.getOperationCase());
    assertEquals(1, mutation.getDelete().getKeySet().getKeysCount());
    assertEquals("my_table", mutation.getDelete().getTable());
    assertEquals(1, mutation.getDelete().getKeySet().getKeys(0).getValuesCount());
    assertEquals("1", mutation.getDelete().getKeySet().getKeys(0).getValues(0).getStringValue());
  }

  @Test
  public void testDeleteWithMultipleLiterals() {
    String sql = "delete from my_table where parent=1 and child=2.2";
    Statement statement = Statement.of(sql);
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertEquals(1L, connection.executeUpdate(statement));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(OperationCase.DELETE, mutation.getOperationCase());
    assertEquals(1, mutation.getDelete().getKeySet().getKeysCount());
    assertEquals("my_table", mutation.getDelete().getTable());
    assertEquals(2, mutation.getDelete().getKeySet().getKeys(0).getValuesCount());
    assertEquals("1", mutation.getDelete().getKeySet().getKeys(0).getValues(0).getStringValue());
    assertEquals(
        2.2d, mutation.getDelete().getKeySet().getKeys(0).getValues(1).getNumberValue(), 0.0d);
  }

  @Test
  public void testBatchWithParameters() {
    String sql = "insert into my_table (id, value) values (@id, @value)";
    Statement statement =
        Statement.newBuilder(sql).bind("id").to(1L).bind("value").to("value1").build();
    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      connection.setConvertDmlToMutations(true);
      assertArrayEquals(
          new long[] {1L, 1L, 1L},
          connection.executeBatchUpdate(
              ImmutableList.of(
                  Statement.newBuilder(sql).bind("id").to(1L).bind("value").to("value1").build(),
                  Statement.newBuilder(sql).bind("id").to(2L).bind("value").to("value2").build(),
                  Statement.newBuilder(sql).bind("id").to(3L).bind("value").to("value3").build())));
      connection.commit();
    }
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
    CommitRequest commit = mockSpanner.getRequestsOfType(CommitRequest.class).get(0);
    assertEquals(1, commit.getMutationsCount());
    Mutation mutation = commit.getMutations(0);
    assertEquals(3, mutation.getInsert().getValuesCount());
    assertEquals(OperationCase.INSERT, mutation.getOperationCase());
    assertEquals("my_table", mutation.getInsert().getTable());
    assertEquals(2, mutation.getInsert().getColumnsCount());
    assertEquals("id", mutation.getInsert().getColumns(0));
    assertEquals("value", mutation.getInsert().getColumns(1));
    int rowNum = 1;
    for (ListValue listValue : mutation.getInsert().getValuesList()) {
      assertEquals(String.valueOf(rowNum), listValue.getValues(0).getStringValue());
      assertEquals("value" + rowNum, listValue.getValues(1).getStringValue());
      rowNum++;
    }
  }
}
