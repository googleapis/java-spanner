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

package com.google.cloud.spanner.it;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeyRange;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionRunner;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import java.util.Collections;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for DML. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public final class ITDMLTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static Database db;
  private static DatabaseClient client;
  /** Sequence for assigning unique keys to test cases. */
  private static int seq;

  /** Id prefix per test case. */
  private static int id;

  private static final String INSERT_DML =
      "INSERT INTO T (k, v) VALUES ('%d-boo1', 1), ('%d-boo2', 2), ('%d-boo3', 3), ('%d-boo4', 4);";
  private static final String UPDATE_DML = "UPDATE T SET T.V = 100 WHERE T.K LIKE '%d-boo%%';";
  private static final String DELETE_DML = "DELETE FROM T WHERE T.K like '%d-boo%%';";
  private static final long DML_COUNT = 4;

  private static boolean throwAbortOnce = false;

  @BeforeClass
  public static void setUpDatabase() {
    db =
        env.getTestHelper()
            .createTestDatabase(
                "CREATE TABLE T ("
                    + "  K    STRING(MAX) NOT NULL,"
                    + "  V    INT64,"
                    + ") PRIMARY KEY (K)");
    client = env.getTestHelper().getDatabaseClient(db);
  }

  @Before
  public void increaseTestIdAndDeleteTestData() {
    client.writeAtLeastOnce(Collections.singletonList(Mutation.delete("T", KeySet.all())));
    id++;
  }

  private static String uniqueKey() {
    return "k" + seq++;
  }

  private String insertDml() {
    return String.format(INSERT_DML, id, id, id, id);
  }

  private String updateDml() {
    return String.format(UPDATE_DML, id);
  }

  private String deleteDml() {
    return String.format(DELETE_DML, id);
  }

  private void executeUpdate(long expectedCount, final String... stmts) {
    final TransactionCallable<Long> callable =
        transaction -> {
          long rowCount = 0;
          for (String stmt : stmts) {
            if (throwAbortOnce) {
              throwAbortOnce = false;
              throw SpannerExceptionFactory.newSpannerException(ErrorCode.ABORTED, "Abort in test");
            }

            rowCount += transaction.executeUpdate(Statement.of(stmt));
          }
          return rowCount;
        };
    TransactionRunner runner = client.readWriteTransaction();
    Long rowCount = runner.run(callable);
    assertThat(rowCount).isEqualTo(expectedCount);
  }

  @Test
  public void abortOnceShouldSucceedAfterRetry() {
    try {
      throwAbortOnce = true;
      executeUpdate(DML_COUNT, insertDml());
      assertThat(throwAbortOnce).isFalse();
    } catch (AbortedException e) {
      fail("Abort Exception not caught and retried");
    }
  }

  @Test
  public void partitionedDML() {
    executeUpdate(DML_COUNT, insertDml());
    assertThat(
            client
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V"))
                .getLong(0))
        .isEqualTo(1);

    long rowCount = client.executePartitionedUpdate(Statement.of(updateDml()));
    // Note: With PDML there is a possibility of network replay or partial update to occur, causing
    // this assert to fail. We should remove this assert if it is a recurring failure in IT tests.
    assertThat(rowCount).isEqualTo(DML_COUNT);
    assertThat(
            client
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V"))
                .getLong(0))
        .isEqualTo(100);

    rowCount = client.executePartitionedUpdate(Statement.of(deleteDml()));
    assertThat(rowCount).isEqualTo(DML_COUNT);
    assertThat(
            client
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V")))
        .isNull();
  }

  @Test
  public void standardDML() {
    executeUpdate(DML_COUNT, insertDml());
    assertThat(
            client
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V"))
                .getLong(0))
        .isEqualTo(1);
    executeUpdate(DML_COUNT, updateDml());
    assertThat(
            client
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V"))
                .getLong(0))
        .isEqualTo(100);
    executeUpdate(DML_COUNT, deleteDml());
    assertThat(
            client
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V")))
        .isNull();
  }

  @Test
  public void standardDMLWithError() {
    try {
      executeUpdate(0, "SELECT * FROM T;");
      fail("Expected illegal argument exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNKNOWN);
      assertThat(e.getMessage())
          .contains("DML response missing stats possibly due to non-DML statement as input");
      assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void standardDMLWithDuplicates() {
    executeUpdate(DML_COUNT, insertDml());

    executeUpdate(
        4,
        String.format("UPDATE T SET v = 200 WHERE k = '%d-boo1';", id),
        String.format("UPDATE T SET v = 300 WHERE k = '%d-boo1';", id),
        String.format("UPDATE T SET v = 400 WHERE k = '%d-boo1';", id),
        String.format("UPDATE T SET v = 500 WHERE k = '%d-boo1';", id));
    assertThat(
            client
                .singleUse(TimestampBound.strong())
                .readRow("T", Key.of(String.format("%d-boo1", id)), Collections.singletonList("V"))
                .getLong(0))
        .isEqualTo(500);

    executeUpdate(DML_COUNT, deleteDml(), deleteDml());
  }

  @Test
  public void standardDMLReadYourWrites() {
    executeUpdate(DML_COUNT, insertDml());

    final TransactionCallable<Void> callable =
        transaction -> {
          long rowCount =
              transaction.executeUpdate(
                  Statement.of(String.format("UPDATE T SET v = v * 2 WHERE k = '%d-boo2';", id)));
          assertThat(rowCount).isEqualTo(1);
          assertThat(
                  transaction
                      .readRow(
                          "T", Key.of(String.format("%d-boo2", id)), Collections.singletonList("v"))
                      .getLong(0))
              .isEqualTo(2 * 2);
          return null;
        };
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(callable);

    executeUpdate(DML_COUNT, deleteDml());
  }

  @Test
  public void standardDMLRollback() {
    class UserException extends Exception {
      UserException(String message) {
        super(message);
      }
    }
    final TransactionCallable<Void> callable =
        transaction -> {
          long rowCount = transaction.executeUpdate(Statement.of(insertDml()));
          assertThat(rowCount).isEqualTo(DML_COUNT);
          throw new UserException("failing to commit");
        };

    try {
      TransactionRunner runner = client.readWriteTransaction();
      runner.run(callable);
      fail("Expected user exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNKNOWN);
      assertThat(e.getMessage()).contains("failing to commit");
      assertThat(e.getCause()).isInstanceOf(UserException.class);
    }

    ResultSet resultSet =
        client
            .singleUse(TimestampBound.strong())
            .read(
                "T",
                KeySet.range(KeyRange.prefix(Key.of(String.format("%d-boo", id)))),
                Collections.singletonList("K"));
    assertThat(resultSet.next()).isFalse();
  }

  @Test
  public void standardDMLAndMutations() {
    final String key1 = uniqueKey();
    final String key2 = uniqueKey();
    final TransactionCallable<Void> callable =
        transaction -> {
          // DML
          long rowCount =
              transaction.executeUpdate(
                  Statement.of("INSERT INTO T (k, v) VALUES ('" + key1 + "', 1)"));
          assertThat(rowCount).isEqualTo(1);

          // Mutations
          transaction.buffer(
              Mutation.newInsertOrUpdateBuilder("T").set("K").to(key2).set("V").to(2).build());
          return null;
        };
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(callable);

    KeySet.Builder keys = KeySet.newBuilder();
    keys.addKey(Key.of(key1)).addKey(Key.of(key2));
    ResultSet resultSet =
        client
            .singleUse(TimestampBound.strong())
            .read("T", keys.build(), Collections.singletonList("K"));
    int rowCount = 0;
    while (resultSet.next()) {
      rowCount++;
    }
    assertThat(rowCount).isEqualTo(2);
  }

  private void executeQuery(long expectedCount, final String... stmts) {
    final TransactionCallable<Long> callable =
        transaction -> {
          long rowCount = 0;
          for (final String stmt : stmts) {
            ResultSet resultSet = transaction.executeQuery(Statement.of(stmt));
            assertThat(resultSet.next()).isFalse();
            assertThat(resultSet.getStats()).isNotNull();
            rowCount += resultSet.getStats().getRowCountExact();
          }
          return rowCount;
        };
    TransactionRunner runner = client.readWriteTransaction();
    Long rowCount = runner.run(callable);
    assertThat(rowCount).isEqualTo(expectedCount);
  }

  @Test
  public void standardDMLWithExecuteSQL() {
    executeQuery(DML_COUNT, insertDml());
    // checks for multi-stmts within a txn, therefore also verifying seqNo.
    executeQuery(DML_COUNT * 2, updateDml(), deleteDml());
  }
}
