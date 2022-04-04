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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.AbstractStatementParser.StatementType;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BeginPgTransactionTest {
  private final AbstractStatementParser parser =
      AbstractStatementParser.getInstance(Dialect.POSTGRESQL);

  @Test
  public void testBeginWithNoOption() {
    ConnectionImpl connection = mock(ConnectionImpl.class);
    ConnectionStatementExecutorImpl executor = new ConnectionStatementExecutorImpl(connection);

    int index = 1;
    for (String sql :
        ImmutableList.of(
            "begin",
            "begin transaction",
            "begin work",
            "start",
            "start transaction",
            "start work",
            "begin isolation level default",
            "begin transaction isolation level default",
            "begin work isolation level default",
            "start isolation level default",
            "start transaction isolation level default",
            "start work isolation level default",
            "begin isolation level serializable",
            "begin transaction isolation level serializable",
            "begin work isolation level serializable",
            "start isolation level serializable",
            "start transaction isolation level serializable",
            "start work isolation level serializable")) {
      ParsedStatement statement = parser.parse(Statement.of(sql));
      assertEquals(sql, StatementType.CLIENT_SIDE, statement.getType());
      statement.getClientSideStatement().execute(executor, sql);

      verify(connection, times(index)).beginTransaction();
      verify(connection, never()).setTransactionMode(any());
      index++;
    }
  }

  @Test
  public void testBeginReadOnly() {
    ConnectionImpl connection = mock(ConnectionImpl.class);
    ConnectionStatementExecutorImpl executor = new ConnectionStatementExecutorImpl(connection);

    int index = 1;
    for (String sql :
        ImmutableList.of(
            "begin read only",
            "begin transaction read only",
            "begin work read only",
            "start read only",
            "start transaction read only",
            "start work read only")) {
      ParsedStatement statement = parser.parse(Statement.of(sql));
      assertEquals(sql, StatementType.CLIENT_SIDE, statement.getType());
      statement.getClientSideStatement().execute(executor, sql);

      verify(connection, times(index)).beginTransaction();
      verify(connection, times(index)).setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
      verify(connection, never()).setTransactionMode(TransactionMode.READ_WRITE_TRANSACTION);
      index++;
    }
  }

  @Test
  public void testBeginReadWrite() {
    ConnectionImpl connection = mock(ConnectionImpl.class);
    ConnectionStatementExecutorImpl executor = new ConnectionStatementExecutorImpl(connection);

    int index = 1;
    for (String sql :
        ImmutableList.of(
            "begin read write",
            "begin transaction read write",
            "begin work read write",
            "start read write",
            "start transaction read write",
            "start work read write")) {
      ParsedStatement statement = parser.parse(Statement.of(sql));
      assertEquals(sql, StatementType.CLIENT_SIDE, statement.getType());
      statement.getClientSideStatement().execute(executor, sql);

      verify(connection, times(index)).beginTransaction();
      verify(connection, times(index)).setTransactionMode(TransactionMode.READ_WRITE_TRANSACTION);
      verify(connection, never()).setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
      index++;
    }
  }
}
