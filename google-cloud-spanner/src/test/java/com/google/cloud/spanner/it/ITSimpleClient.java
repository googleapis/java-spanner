/*
 * Copyright 2020 Google LLC
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
import static org.junit.Assert.assertTrue;

import com.google.cloud.ByteArray;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.OnReadHandler;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SimpleClient;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.Value;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for SimpleClient. */
@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class ITSimpleClient {

  private static final int WRITE_BATCH_SIZE = 1 << 20;
  private static final String TABLE_NAME = "TestTable";

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  private static Database db;
  private static HashFunction hasher;

  private static DatabaseClient client;
  private static SimpleClient simpleClient;

  // Generate row sizes
  private static List<Integer> rowSizes() {
    List<Integer> rowSizes = new ArrayList<>();
    rowSizes.addAll(Collections.nCopies(1000, 4096));
    Collections.shuffle(rowSizes);
    return rowSizes;
  }

  @BeforeClass
  public static void setUpDatabase() {
    db =
        env.getTestHelper()
            .createTestDatabase(
                "CREATE TABLE TestTable ("
                    + "  Key           INT64 NOT NULL,"
                    + "  Data          BYTES(MAX),"
                    + "  Fingerprint   INT64,"
                    + "  Size          INT64,"
                    + ") PRIMARY KEY (Key)");
    hasher = Hashing.goodFastHash(64);
    client = env.getTestHelper().getDatabaseClient(db);
    simpleClient = env.getTestHelper().getSimpleClient(db);

    List<Mutation> mutations = new ArrayList<>();
    Random rnd = new Random();
    int totalSize = 0;
    int i = 0;
    for (int rowSize : rowSizes()) {
      byte[] data = new byte[rowSize];
      rnd.nextBytes(data);
      mutations.add(
          Mutation.newInsertOrUpdateBuilder(TABLE_NAME)
              .set("Key")
              .to(i)
              .set("Data")
              .to(ByteArray.copyFrom(data))
              .set("Fingerprint")
              .to(hasher.hashBytes(data).asLong())
              .set("Size")
              .to(rowSize)
              .build());
      totalSize += rowSize;
      i++;
      if (totalSize >= WRITE_BATCH_SIZE) {
        client.write(mutations);
        mutations.clear();
        totalSize = 0;
      }
    }
    client.write(mutations);
  }

  @Test
  public void testReads() {
    int count = 0;
    try (ResultSet res =
        simpleClient.executeSqlQuery(
            "SELECT Key, Data, Fingerprint, Size from  TestTable LIMIT 10")) {
      while (res.next()) {
        count++;
      }
    }
    assertEquals(10, count);

    count = 0;
    Map<String, Value> args = Maps.newHashMap();
    args.put("COL_ID", Value.int64(20));
    try (ResultSet res =
        simpleClient.executeSqlQuery(
            "SELECT Key, Data, Fingerprint, Size from TestTable WHERE Key > @COL_ID LIMIT 30 ",
            args)) {
      while (res.next()) {
        count++;
        assertTrue(res.getLong("Key") > 20);
      }
    }
    assertEquals(30, count);

    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(15L));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    count = 0;
    try (ResultSet res =
        simpleClient.executeSnapshotSqlQuery(
            "SELECT Key, Data, Fingerprint, Size from TestTable LIMIT 20")) {
      while (res.next()) {
        count++;
      }
    }
    assertEquals(20, count);

    count = 0;
    args.put("COL_ID", Value.int64(60));
    try (ResultSet res =
        simpleClient.executeSnapshotSqlQuery(
            "SELECT Key, Data, Fingerprint, Size from TestTable WHERE Key > @COL_ID LIMIT 30 ",
            args)) {
      while (res.next()) {
        count++;
        assertTrue(res.getLong("Key") > 60);
      }
    }
    assertEquals(30, count);
  }

  @Test
  public void testTransactions() {
    Statement.Builder builder =
        Statement.newBuilder(
            "INSERT INTO TestTable (Key, Data, Fingerprint, Size) VALUES (@KEY_V, @DATA_V, @FINGERPRINT_V, @SIZE_V)");
    byte data[] = new byte[100];
    builder.bind("DATA_V").to(ByteArray.copyFrom(data));
    builder.bind("FINGERPRINT_V").to(hasher.hashBytes(data).asLong());
    builder.bind("SIZE_V").to(data.length);
    List<Statement> statements =
        Lists.newArrayList(
            builder.bind("KEY_V").to(5000).build(),
            builder.bind("KEY_V").to(5001).build(),
            builder.bind("KEY_V").to(5002).build());
    simpleClient.runTransaction(statements);

    final long originalSize = data.length;

    String verifySql = "SELECT Key FROM TestTable where Key in (5000, 5001, 5002)";
    Set<Long> keys = Sets.newHashSet();
    try (ResultSet res = simpleClient.executeSqlQuery(verifySql)) {
      while (res.next()) {
        keys.add(res.getLong("Key"));
      }
    }
    assertEquals(3, keys.size());
    for (long i = 5000; i <= 5002; i++) {
      assertTrue(keys.contains(i));
    }

    String sql = "SELECT Key, Size FROM TestTable WHERE Key = 5000 LIMIT 1";
    simpleClient.runTransaction(
        Statement.of(sql),
        new OnReadHandler() {
          @Override
          public void handle(ResultSet resultSet, TransactionContext transaction) {
            if (resultSet.next()) {
              transaction.executeUpdate(
                  Statement.newBuilder("UPDATE TestTable set Size = @SIZE WHERE Key = 5000")
                      .bind("SIZE")
                      .to(resultSet.getLong("Size") * 2)
                      .build());
            }
          }
        });

    try (ResultSet resultSet = simpleClient.executeSqlQuery(sql)) {
      if (resultSet.next()) {
        assertEquals(2 * originalSize, resultSet.getLong("Size"));
      }
    }
  }
}
