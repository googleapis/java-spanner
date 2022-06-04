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
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionRunner;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.connection.ConnectionOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration tests for DML Returning. */
@Category(ParallelIntegrationTest.class)
@RunWith(Parameterized.class)
public final class ITDmlReturningTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static DatabaseClient googleStandardSQLClient;
  private static DatabaseClient postgreSQLClient;

  /** Id prefix per test case. */
  private static int id;

  private static final String INSERT_DML_RETURNING =
      "INSERT INTO T (k, v) VALUES ('%d-boo1', 1), ('%d-boo2', 2), ('%d-boo3', 3), ('%d-boo4', 4) THEN RETURN *;";
  private static final String UPDATE_DML_RETURNING =
      "UPDATE T SET V = 100 WHERE K LIKE '%d-boo%%' THEN RETURN *;";
  private static final String DELETE_DML_RETURNING =
      "DELETE FROM T WHERE K like '%d-boo%%' THEN RETURN *;";
  private static final String DELETE_DML = "DELETE FROM T WHERE K like '%d-boo%%';";

  private static final long DML_COUNT = 4;

  @BeforeClass
  public static void setUpDatabase() {
    Database googleStandardSQLDatabase =
        env.getTestHelper()
            .createTestDatabase(
                "CREATE TABLE T ("
                    + "  K    STRING(MAX) NOT NULL,"
                    + "  V    INT64,"
                    + ") PRIMARY KEY (K)");
    googleStandardSQLClient = env.getTestHelper().getDatabaseClient(googleStandardSQLDatabase);
    if (!isUsingEmulator()) {
      Database postgreSQLDatabase =
          env.getTestHelper()
              .createTestDatabase(
                  Dialect.POSTGRESQL,
                  Collections.singletonList(
                      "CREATE TABLE T (" + "  K    VARCHAR PRIMARY KEY," + "  V    BIGINT" + ")"));
      postgreSQLClient = env.getTestHelper().getDatabaseClient(postgreSQLDatabase);
    }
  }

  @AfterClass
  public static void teardown() {
    ConnectionOptions.closeSpanner();
  }

  @Before
  public void increaseTestIdAndDeleteTestData() {
    if (dialect.dialect == Dialect.GOOGLE_STANDARD_SQL) {
      googleStandardSQLClient.writeAtLeastOnce(
          Collections.singletonList(Mutation.delete("T", KeySet.all())));
    } else {
      postgreSQLClient.writeAtLeastOnce(
          Collections.singletonList(Mutation.delete("T", KeySet.all())));
    }
    id++;
  }

  @Parameterized.Parameters(name = "Dialect = {0}")
  public static List<DialectTestParameter> data() {
    // DML Returning does not support PG dialect yet.
    // So we run tests only for Google Standard SQL for now.
    List<DialectTestParameter> params = new ArrayList<>();
    params.add(new DialectTestParameter(Dialect.GOOGLE_STANDARD_SQL));
    return params;
  }

  @Parameterized.Parameter(0)
  public DialectTestParameter dialect;

  private String insertDmlReturning() {
    return String.format(INSERT_DML_RETURNING, id, id, id, id);
  }

  private String updateDmlReturning() {
    return String.format(UPDATE_DML_RETURNING, id);
  }

  private String deleteDmlReturning() {
    return String.format(DELETE_DML_RETURNING, id);
  }

  private String deleteDml() {
    return String.format(DELETE_DML, id);
  }

  private DatabaseClient getClient(Dialect dialect) {
    if (dialect == Dialect.POSTGRESQL) {
      return postgreSQLClient;
    }
    return googleStandardSQLClient;
  }

  @Test
  public void dmlReturningWithExecuteUpdate() {
    executeUpdate(DML_COUNT, insertDmlReturning());
    // checks for multi-stmts within a txn, therefore also verifying seqNo.
    executeUpdate(DML_COUNT * 2, updateDmlReturning(), deleteDmlReturning());
  }

  private void executeUpdate(long expectedCount, final String... stmts) {
    final TransactionCallable<Long> callable =
        transaction -> {
          long rowCount = 0;
          for (String stmt : stmts) {
            rowCount += transaction.executeUpdate(Statement.of(stmt));
          }
          return rowCount;
        };
    TransactionRunner runner = getClient(dialect.dialect).readWriteTransaction();
    Long rowCount = runner.run(callable);
    assertThat(rowCount).isEqualTo(expectedCount);
  }

  @Test
  public void dmlReturningWithExecutePartitionedUpdate() {
    Throwable exception =
        assertThrows(
            SpannerException.class,
            () ->
                getClient(dialect.dialect)
                    .executePartitionedUpdate(Statement.of(updateDmlReturning())));
    assertThat(exception.getMessage()).ignoringCase().contains("not supported");
  }

  @Test
  public void dmlReturningWithExecuteQuery() {
    List<Struct> rows = executeQuery(DML_COUNT, insertDmlReturning());
    assertThat(
            getClient(dialect.dialect)
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V"))
                .getLong(0))
        .isEqualTo(1);

    // Check if keys(K) and V have expected values.
    for (int idx = 0; idx < rows.size(); idx++) {
      assertThat(rows.get(idx).getLong("V")).isEqualTo(idx + 1);
      assertThat(rows.get(idx).getString("K")).isEqualTo(String.format("%d-boo%d", id, idx + 1));
    }
    rows = executeQuery(DML_COUNT, updateDmlReturning());
    assertThat(
            getClient(dialect.dialect)
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V"))
                .getLong(0))
        .isEqualTo(100);

    // Check if keys(K) and V have expected values.
    for (int idx = 0; idx < rows.size(); idx++) {
      assertThat(rows.get(idx).getLong("V")).isEqualTo(100);
      assertThat(rows.get(idx).getString("K")).isEqualTo(String.format("%d-boo%d", id, idx + 1));
    }
    rows = executeQuery(DML_COUNT, deleteDmlReturning());
    assertThat(
            getClient(dialect.dialect)
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V")))
        .isNull();

    // Check if keys(K) and V have expected values.
    for (int idx = 0; idx < rows.size(); idx++) {
      assertThat(rows.get(idx).getLong("V")).isEqualTo(100);
      assertThat(rows.get(idx).getString("K")).isEqualTo(String.format("%d-boo%d", id, idx + 1));
    }
  }

  private List<Struct> executeQuery(long expectedCount, String stmt) {
    List<Struct> rows = new ArrayList<>();
    final TransactionCallable<Void> callable =
        transaction -> {
          ResultSet resultSet = transaction.executeQuery(Statement.of(stmt));
          // resultSet.next() returns false, when no more row exists.
          // So, number of times resultSet.next() returns true, is the number of rows
          // returned by the DML Returning statement.
          while (resultSet.next()) {
            rows.add(resultSet.getCurrentRowAsStruct());
          }
          assertThat(resultSet.next()).isFalse();
          assertThat(resultSet.getStats()).isNotNull();
          assertThat(expectedCount).isEqualTo(resultSet.getStats().getRowCountExact());
          return null;
        };
    TransactionRunner runner = getClient(dialect.dialect).readWriteTransaction();
    runner.run(callable);
    rows.sort(Comparator.comparing(a -> a.getString("K")));
    return rows;
  }

  @Test
  public void dmlReturningWithBatchUpdate() {
    // We pass in a mix of DML and DML Returning statements, to batchUpdate.
    long[] rowCounts = batchUpdate(insertDmlReturning(), updateDmlReturning(), deleteDml());
    assertThat(rowCounts.length).isEqualTo(3);
    assertThat(rowCounts[0]).isEqualTo(DML_COUNT);
    assertThat(rowCounts[1]).isEqualTo(DML_COUNT);
    assertThat(rowCounts[2]).isEqualTo(DML_COUNT);
  }

  private long[] batchUpdate(final String... stmts) {
    final TransactionCallable<long[]> callable =
        transaction ->
            transaction.batchUpdate(
                Arrays.stream(stmts).map(Statement::of).collect(Collectors.toList()));
    TransactionRunner runner = getClient(dialect.dialect).readWriteTransaction();
    return runner.run(callable);
  }
}
