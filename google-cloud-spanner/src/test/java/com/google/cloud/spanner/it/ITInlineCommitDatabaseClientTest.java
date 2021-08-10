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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SingleDmlTransaction;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for InlineCommit used in DatabaseClient. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public final class ITInlineCommitDatabaseClientTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static DatabaseClient client;
  private static long prevKey;
  private static final String CREATE = "CREATE TABLE T (key INT64, value INT64) PRIMARY KEY (key)";
  private static final String INSERT = "INSERT INTO T (key, value) VALUES (@key, @key)";
  private static final String UPDATE = "UPDATE T SET value=value+1 WHERE key=@key";
  private static final String SELECT = "SELECT value FROM T where key=@key";

  private long preInsertedKey;

  @BeforeClass
  public static void setUpDatabase() {
    Database db = env.getTestHelper().createTestDatabase(CREATE);
    client = env.getTestHelper().getDatabaseClient(db);
  }

  private static synchronized long nextKey() {
    return ++prevKey;
  }

  private Statement getStatement(String sql) {
    return Statement.newBuilder(sql).bind("key").to(preInsertedKey).build();
  }

  @Before
  public void preInsert() {
    preInsertedKey = nextKey();
    try {
      client.singleDmlTransaction().executeUpdate(getStatement(INSERT));
    } catch (SpannerException e) {
      if (e.getErrorCode() != ErrorCode.ALREADY_EXISTS) {
        throw e;
      }
    }
  }

  private long readValue() {
    ResultSet resultSet = client.singleUse().executeQuery(getStatement(SELECT));
    assertTrue(resultSet.next());
    long value = resultSet.getLong(0);
    assertFalse(resultSet.next());
    return value;
  }

  @Test
  public void dmlWithInlineCommit() {
    long prevValue = readValue();
    long rowCount = client.singleDmlTransaction().executeUpdate(getStatement(UPDATE));
    assertEquals(1, rowCount);
    assertTrue(prevValue < readValue());
  }

  @Test
  public void queryWithInlineCommit() {
    SpannerException exception =
        assertThrows(
            SpannerException.class,
            () -> client.singleDmlTransaction().executeUpdate(getStatement(SELECT)));
    assertEquals(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Autocommit may only be used with a DML statement"));
  }

  @Test
  public void singleDmlTransactionCanOnlyBeUsedOnce() {
    SingleDmlTransaction singleDmlTransaction = client.singleDmlTransaction();
    long rowCount = singleDmlTransaction.executeUpdate(getStatement(UPDATE));
    assertEquals(1, rowCount);

    SpannerException exception =
        assertThrows(
            SpannerException.class, () -> singleDmlTransaction.executeUpdate(getStatement(UPDATE)));
    assertEquals(ErrorCode.FAILED_PRECONDITION, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("A SingleDmlTransaction can only be used once"));
  }

  @Test
  public void onlyOnceEvenIfTheFirstUsageWasInvalid() {
    SingleDmlTransaction singleDmlTransaction = client.singleDmlTransaction();
    assertThrows(
        SpannerException.class, () -> singleDmlTransaction.executeUpdate(getStatement(SELECT)));

    SpannerException exception =
        assertThrows(
            SpannerException.class, () -> singleDmlTransaction.executeUpdate(getStatement(UPDATE)));
    assertEquals(ErrorCode.FAILED_PRECONDITION, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("A SingleDmlTransaction can only be used once"));
  }

  @Test
  public void useOptions() {
    long prevValue = readValue();
    long rowCount =
        client
            .singleDmlTransaction(Options.priority(RpcPriority.MEDIUM), Options.commitStats())
            .executeUpdate(
                getStatement(UPDATE), Options.priority(RpcPriority.LOW), Options.tag("dumm tag"));
    assertEquals(1, rowCount);
    assertTrue(prevValue < readValue());
  }
}
