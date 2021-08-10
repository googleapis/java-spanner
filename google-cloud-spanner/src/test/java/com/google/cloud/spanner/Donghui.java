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

package com.google.cloud.spanner;

import com.google.cloud.spanner.connection.Connection;
import com.google.cloud.spanner.connection.ConnectionOptions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

// This program tests the autocommit feature using a pre-created Cloud-Devel database.
//
// Prerequisite:
// - The mentioned Spanner database includes a pre-created table foo(key: INT64, value: INT64).
//
// For a quick run:
// - One thing that prevents a run from finishing in seconds is that, if the table does not already
//   include all keys in [0..NUM_PRE_INSERTED-1], these keys will be inserted. But this is a
//   one-time thing.
// - Another things that may cause the program to take a while to run is that
//   testPerformance() uses multiples ways each updating NUM_TRANSACTIONS_IN_PERF_TEST records. For
//   a quick run, it is recommended to temporarily reduce NUM_TRANSACTIONS_IN_PERF_TEST.
//
public class Donghui {
  //
  // constants
  //

  // Pointer to a pre-created staging database with a table foo(key: INT64, value: INT64).
  private static final String HOST = "staging-wrenchworks.sandbox.googleapis.com";
  private static final String PROJECT = "span-cloud-testing";
  private static final String INSTANCE = "donghuiz-instance"; // test-instance or donghuiz-instance
  private static final String DATABASE = "donghuiz-db";

  // SQL related.
  private static final String INSERT = "INSERT INTO foo (key, value) VALUES (@key, @key)";
  private static final String UPDATE = "UPDATE foo SET value=value+1 WHERE key=@key";
  private static final String SELECT = "SELECT value FROM foo WHERE key=@key";
  private static final String SUM =
      "SELECT SUM(value) FROM foo WHERE key>=0 AND key<@numPreInserted";
  private static final String COUNT =
      "SELECT COUNT(value) FROM foo WHERE key>=0 AND key<@numPreInserted";
  private static final String ALL_KEYS = "SELECT key FROM foo WHERE key>=0 AND key<@numPreInserted";
  private static final String PARAM_KEY = "key";
  private static final String PARAM_NUM_PRE_INSERTED = "numPreInserted";

  // Number of keys to pre-insert.
  private static final long NUM_PRE_INSERTED = 1000;

  // Number of transactions to run during the performance test.
  private static final int NUM_TRANSACTIONS_IN_PERF_TEST = 1000;

  private static final Random random = new Random();
  private static final boolean debugLogging = true;

  //
  // private methods
  //

  // A random key in [0..NUM_PRE_INSERTED-1].
  private static long randomKey() {
    long key = random.nextLong() % NUM_PRE_INSERTED;
    return key >= 0 ? key : -key;
  }

  private static void check(boolean condition, String message) throws SpannerException {
    if (!condition) {
      throw new RuntimeException("[CHECK FAILURE:] " + message);
    }
  }

  // Returns the value of a record given an existing key, or throws NOT_FOUND.
  private static long select(Connection connection, long key) throws SpannerException {
    check(!connection.isTransactionStarted(), "before selectOne, connection isTransactionStarted");
    ResultSet resultSet =
        connection.executeQuery(Statement.newBuilder(SELECT).bind(PARAM_KEY).to(key).build());
    check(resultSet.next(), "The record with key " + Long.toString(key) + " does not exist.");
    final long value = resultSet.getLong(0);
    resultSet.close();
    if (!connection.isAutocommit()) {
      connection.commit();
    }
    check(!connection.isTransactionStarted(), "after selectOne, connection isTransactionStarted");
    return value;
  }

  // Returns the SUM of values.
  private static long sum(Connection connection) throws SpannerException {
    return sumCountImpl(connection, true);
  }

  // Returns the COUNT of values.
  public static long count(Connection connection) throws SpannerException {
    return sumCountImpl(connection, false);
  }

  private static long sumCountImpl(Connection connection, boolean isSum) throws SpannerException {
    check(!connection.isTransactionStarted(), "before sum, connection isTransactionStarted");

    ResultSet resultSet =
        connection.executeQuery(
            Statement.newBuilder(isSum ? SUM : COUNT)
                .bind(PARAM_NUM_PRE_INSERTED)
                .to(NUM_PRE_INSERTED)
                .build());
    check(resultSet.next(), "Fail to get result.");
    final long ret = resultSet.getLong(0);
    resultSet.close();
    if (!connection.isAutocommit()) {
      connection.commit();
    }
    check(!connection.isTransactionStarted(), "after selectOne, connection isTransactionStarted");
    return ret;
  }

  // Increments the value of a record with a given key.
  private static void updateOne(Connection connection, long key) {
    check(!connection.isTransactionStarted(), "isTransactionStarted should not be true");
    long row_count =
        connection.executeUpdate(Statement.newBuilder(UPDATE).bind(PARAM_KEY).to(key).build());
    check(row_count == 1, "Wrong updated row_count.");
    if (!connection.isAutocommit()) {
      connection.commit();
    }
    check(!connection.isTransactionStarted(), "isTransactionStarted should not be true");
  }

  // Increments the value of numUpdates pre-inserted records with randomly-selected keys.
  // Every update uses a separate transaction. Ensures exactly 1 row is modified.
  private static void update(Connection connection, boolean autocommit, int numUpdates)
      throws SpannerException {
    connection.setAutocommit(autocommit);
    for (int i = 0; i < numUpdates; ++i) {
      updateOne(connection, randomKey());
    }
  }

  private static void updateOne(DatabaseClient client, boolean inlineCommit, long key)
      throws NullPointerException, SpannerException {
    Statement statement = Statement.newBuilder(UPDATE).bind(PARAM_KEY).to(key).build();
    if (inlineCommit) {
      long row_count = client.singleDmlTransaction().executeUpdate(statement);
      check(row_count == 1, "Wrong updated row_count.");
    } else {
      client
          .readWriteTransaction()
          .run(
              transaction -> {
                long row_count = transaction.executeUpdate(statement);
                check(row_count == 1, "Wrong updated row_count.");
                return null;
              });
    }
  }

  private static void update(DatabaseClient client, boolean inlineCommit, int numUpdates)
      throws SpannerException {
    for (int i = 0; i < numUpdates; ++i) {
      updateOne(client, inlineCommit, randomKey());
    }
  }

  // Percentage of speedup from slow to fast.
  private static double speedupPct(long slow, long fast) {
    if (fast == 0) {
      return 0;
    }
    return (((double) slow / fast) - 1) * 100;
  }

  //
  // public methods
  //

  public static Connection newConnection() {
    final ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(
                String.format(
                    "cloudspanner://%s/projects/%s/instances/%s/databases/%s",
                    HOST, PROJECT, INSTANCE, DATABASE))
            .build();
    return options.getConnection();
  }

  public static DatabaseClient newDatabaseClient() {
    final SpannerOptions options =
        SpannerOptions.newBuilder().setProjectId(PROJECT).setHost("https://" + HOST).build();
    final Spanner spanner = options.getService();
    final DatabaseId id = DatabaseId.of(PROJECT, INSTANCE, DATABASE);
    return spanner.getDatabaseClient(id);
  }

  // Ensures all keys in [0..NUM_PRE_INSERTED-1] exist.
  public static void preInsert(DatabaseClient client) {
    if (debugLogging) {
      System.out.println(
          String.format("Begin preInsert to insert %d records...", NUM_PRE_INSERTED));
    }
    HashSet<Long> keys = new HashSet<>();
    ResultSet resultSet =
        client
            .readOnlyTransaction()
            .executeQuery(
                Statement.newBuilder(ALL_KEYS)
                    .bind(PARAM_NUM_PRE_INSERTED)
                    .to(NUM_PRE_INSERTED)
                    .build());
    while (resultSet.next()) {
      long key = resultSet.getLong(0);
      keys.add(key);
    }

    // Inserts all missing keys.
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              for (long i = 0; i < NUM_PRE_INSERTED; ++i) {
                if (!keys.contains(i)) {
                  transaction.executeUpdate(
                      Statement.newBuilder(INSERT).bind(PARAM_KEY).to(i).build());
                }
              }
              return null;
            });

    // Verifies all keys should exist.
    resultSet =
        client
            .readOnlyTransaction()
            .executeQuery(
                Statement.newBuilder(COUNT)
                    .bind(PARAM_NUM_PRE_INSERTED)
                    .to(NUM_PRE_INSERTED)
                    .build());
    check(
        resultSet.next() && resultSet.getLong(0) == NUM_PRE_INSERTED,
        "The number of pre-inserted keys is wrong.");
    if (debugLogging) {
      System.out.println("end preInsert.");
    }
  }

  public static void testUpdateOneShouldPersist(Connection connection, DatabaseClient client)
      throws SpannerException {
    if (debugLogging) {
      System.out.println("Begin testUpdateOneShouldPersist...");
    }
    final long key = randomKey();
    final long value = select(connection, key);
    int numIncrements = 0;

    // With or without autocommit, updateOne() should increment the value.
    while (numIncrements < 4) {
      ++numIncrements;
      connection.setAutocommit(numIncrements % 2 == 0);
      updateOne(connection, key);
      check(select(connection, key) == value + numIncrements, "wrong value");
    }

    // Same with DatabaseClient about updateOne().
    while (numIncrements < 8) {
      ++numIncrements;
      updateOne(client, numIncrements % 2 == 0, key);
      check(select(connection, key) == value + numIncrements, "wrong value");
    }

    if (debugLogging) {
      System.out.println("end testUpdateOneShouldPersist.");
    }
  }

  public static void testUpdateShouldPersist(Connection connection, DatabaseClient client)
      throws SpannerException {
    if (debugLogging) {
      System.out.println("Begin testUpdateShouldPersist...");
    }
    final int numUpdates = 5;
    final long sum = sum(connection);
    long numIncrements = 0;
    long newSum = 0;

    numIncrements += numUpdates;
    update(connection, false, numUpdates);
    newSum = sum(connection);
    check(newSum == sum + numIncrements, "wrong value");

    numIncrements += numUpdates;
    update(connection, true, numUpdates);
    newSum = sum(connection);
    check(newSum == sum + numIncrements, "wrong value");

    numIncrements += numUpdates;
    update(client, false, numUpdates);
    newSum = sum(connection);
    check(newSum == sum + numIncrements, "wrong value");

    numIncrements += numUpdates;
    update(client, true, numUpdates);
    newSum = sum(connection);
    check(newSum == sum + numIncrements, "wrong value");

    if (debugLogging) {
      System.out.println("end testUpdateShouldPersist.");
    }
  }

  // Sample output when testing against donghuiz-instance @ us-central1 (cloud-devel):
  //   Connection w/o autocommit: 169.77s; with autocommit: 137.31s; speedup: 23.63%
  //   DatabaseClient w/o autocommit: 165.30s; with autocommit: 132.79s; speedup: 24.48%
  // Sample output when testing against test-instance @ US Central 1 (staging):
  //   Connection w/o autocommit: 77.70s; with autocommit: 76.15s; speedup: 2.04%
  //   DatabaseClient w/o autocommit: 72.80s; with autocommit: 71.26s; speedup: 2.17%
  // Both cases were run from Donghuiz's desktop (located in CAM).
  public static void testPerformance(Connection connection, DatabaseClient client) {
    for (int maxTransactionsPerIteration : Arrays.asList(10)) {
      if (debugLogging) {
        System.out.println(
            String.format(
                "Begin testPerformance (using the total time of %d transactions)...",
                NUM_TRANSACTIONS_IN_PERF_TEST));
      }
      // Record the previous sum of all values.
      final long prev_sum = sum(connection);

      // Run 4 * NUM_TRANSACTIONS_IN_PERF_TEST transactions.
      long connNoAuto = 0, connAuto = 0, clientNoAuto = 0, clientAuto = 0;
      int numFinished = 0;
      while (numFinished < NUM_TRANSACTIONS_IN_PERF_TEST) {
        final int numUpdates =
            Math.min(maxTransactionsPerIteration, NUM_TRANSACTIONS_IN_PERF_TEST - numFinished);
        final long t1 = System.currentTimeMillis();
        update(connection, false, numUpdates);
        final long t2 = System.currentTimeMillis();
        update(connection, true, numUpdates);
        final long t3 = System.currentTimeMillis();
        update(client, false, numUpdates);
        final long t4 = System.currentTimeMillis();
        update(client, true, numUpdates);
        final long t5 = System.currentTimeMillis();

        connNoAuto += t2 - t1;
        connAuto += t3 - t2;
        clientNoAuto += t4 - t3;
        clientAuto += t5 - t4;
        numFinished += numUpdates;
      }
      System.out.println(
          String.format(
              "end testPerformance (using the total time of %d transactions):\n"
                  + "  Connection w/o autocommit: %.2fs; with autocommit: %.2fs; speedup: %.2f%%\n"
                  + "  DatabaseClient w/o autocommit: %.2fs; with autocommit: %.2fs; speedup: %.2f%%\n",
              NUM_TRANSACTIONS_IN_PERF_TEST,
              connNoAuto / 1000.0,
              connAuto / 1000.0,
              speedupPct(connNoAuto, connAuto),
              clientNoAuto / 1000.0,
              clientAuto / 1000.0,
              speedupPct(clientNoAuto, clientAuto)));

      // Ensure the overall increase of the SUM is expected.
      check(sum(connection) == prev_sum + NUM_TRANSACTIONS_IN_PERF_TEST * 4, "unexpected sum");
    }
  }

  public static void main(String[] args) throws SpannerException {
    Connection connection = newConnection();
    DatabaseClient client = newDatabaseClient();

    preInsert(client);
    testUpdateOneShouldPersist(connection, client);
    testUpdateShouldPersist(connection, client);
    testPerformance(connection, client);

    connection.close();
  }
}
