/*
 * Copyright 2025 Google LLC
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

import com.google.cloud.NoCredentials;
import io.grpc.ManagedChannelBuilder;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Simple test to verify bypass point read functionality against a bypass-enabled server.
 *
 * <p>Usage: Set the BYPASS_HOST environment variable or modify the DEFAULT_HOST constant, then run
 * this test.
 *
 * <p>Prerequisites:
 *
 * <ul>
 *   <li>A bypass-enabled Spanner server running (e.g., box::TestEnv)
 *   <li>A database with table T created and populated
 * </ul>
 */
public class BypassPointReadTest {

  // Configure these based on your bypass server setup
  private static final String DEFAULT_HOST = "http://localhost:8080";
  private static final String DEFAULT_PROJECT_ID = "default";
  private static final String DEFAULT_INSTANCE_ID = "default";
  private static final String DEFAULT_DATABASE_ID = "db";
  private static final String DEFAULT_TABLE_NAME = "T";
  private static final String DEFAULT_KEY_COLUMN = "Key";
  private static final String DEFAULT_VALUE_COLUMN = "Value";
  private static final String DEFAULT_INDEX_NAME = "";

  public static void main(String[] args) {
    String host = envOrDefault("BYPASS_HOST", DEFAULT_HOST);
    String projectId = envOrDefault("BYPASS_PROJECT_ID", DEFAULT_PROJECT_ID);
    String instanceId = envOrDefault("BYPASS_INSTANCE_ID", DEFAULT_INSTANCE_ID);
    String databaseId = envOrDefault("BYPASS_DATABASE_ID", DEFAULT_DATABASE_ID);
    String tableName = envOrDefault("BYPASS_TABLE", DEFAULT_TABLE_NAME);
    String keyColumn = envOrDefault("BYPASS_KEY_COLUMN", DEFAULT_KEY_COLUMN);
    String valueColumn = envOrDefault("BYPASS_VALUE_COLUMN", DEFAULT_VALUE_COLUMN);
    String indexName = envOrDefault("BYPASS_INDEX_NAME", DEFAULT_INDEX_NAME);
    String querySql = envOrDefault("BYPASS_QUERY", "");
    String dmlSql = envOrDefault("BYPASS_DML_STATEMENT", "");
    String mutationValueRaw = envOrDefault("BYPASS_MUTATION_VALUE", "");
    int repeatOperation = envInt("BYPASS_REPEAT", 2);

    KeyValue keyValue = parseKeyValue(envOrDefault("BYPASS_KEY_VALUE", "1"));
    KeyValue mutationValue = parseOptionalValue(mutationValueRaw);

    System.out.println("=== SpanFE Bypass Smoke Test ===");
    System.out.println("Connecting to bypass server: " + host);
    System.out.println(
        "Project/Instance/Database: " + projectId + "/" + instanceId + "/" + databaseId);
    System.out.println("Table: " + tableName + ", Key column: " + keyColumn);

    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setExperimentalHost(host)
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .setCredentials(NoCredentials.getInstance())
            .build();

    try (Spanner spanner = options.getService()) {
      DatabaseClient dbClient =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

      System.out.println("\n--- Test 1: Point Read (StreamingRead) ---");
      testPointRead(dbClient, tableName, keyColumn, valueColumn, keyValue);

      System.out.println("\n--- Test 2: Multiple Point Reads (cache warm-up) ---");
      testMultiplePointReads(dbClient, tableName, keyColumn, keyValue);

      System.out.println("\n--- Test 3: Read Using Index (StreamingRead) ---");
      testReadUsingIndex(dbClient, tableName, indexName, keyColumn, keyValue, repeatOperation);

      System.out.println("\n--- Test 4: Execute Query (ExecuteStreamingSql) ---");
      testExecuteQuery(
          dbClient, tableName, keyColumn, valueColumn, keyValue, querySql, repeatOperation);

      System.out.println("\n--- Test 5: Single-Use Read-Only Transaction (Streaming) ---");
      testSingleUseReadOnlyTransaction(
          dbClient, tableName, keyColumn, valueColumn, keyValue, repeatOperation);

      System.out.println("\n--- Test 6: Read-Only Transaction (BeginTransaction + Streaming) ---");
      testReadOnlyTransaction(
          dbClient, tableName, keyColumn, valueColumn, keyValue, repeatOperation);

      System.out.println(
          "\n--- Test 7: Read-Write Transaction (BeginTransaction + ExecuteSql + Commit) ---");
      testReadWriteTransactionDml(
          dbClient, tableName, keyColumn, valueColumn, keyValue, dmlSql, repeatOperation);

      System.out.println("\n--- Test 8: Explicit Rollback (BeginTransaction + Rollback) ---");
      testExplicitRollback(dbClient, repeatOperation);

      System.out.println("\n--- Test 9: Mutation Write (BeginTransaction + Commit) ---");
      testMutationWrite(
          dbClient, tableName, keyColumn, valueColumn, keyValue, mutationValue, repeatOperation);

      System.out.println("\n=== All tests completed successfully! ===");

    } catch (Exception e) {
      System.err.println("Test failed with exception:");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void testPointRead(
      DatabaseClient dbClient,
      String tableName,
      String keyColumn,
      String valueColumn,
      KeyValue keyValue) {
    List<String> columns =
        valueColumn.isEmpty() ? Arrays.asList(keyColumn) : Arrays.asList(keyColumn, valueColumn);

    System.out.println("Reading key: " + keyValue.display);
    System.out.println("Columns: " + columns);

    try (ResultSet resultSet =
        dbClient.singleUse().read(tableName, KeySet.singleKey(keyValue.asKey()), columns)) {

      int rowCount = 0;
      while (resultSet.next()) {
        rowCount++;
        System.out.println(
            "  Row "
                + rowCount
                + ": "
                + keyColumn
                + "="
                + keyValue.readValue(resultSet, keyColumn));
        if (columns.size() > 1) {
          try {
            System.out.println(
                "           " + valueColumn + "=" + resultSet.getString(valueColumn));
          } catch (Exception e) {
            // Column might not exist or be non-string
          }
        }
      }
      System.out.println("Total rows returned: " + rowCount);

      if (rowCount == 0) {
        System.out.println("WARNING: No rows returned. Make sure the table has data.");
      }
    }
  }

  private static void testMultiplePointReads(
      DatabaseClient dbClient, String tableName, String keyColumn, KeyValue keyValue) {
    if (keyValue.longValue == null) {
      System.out.println(
          "Skipping: BYPASS_KEY_VALUE is not numeric, cache warm-up test uses numeric keys.");
      return;
    }

    List<String> columns = Arrays.asList(keyColumn);

    // Perform multiple reads to test cache warm-up
    // First read: cache miss, server returns CacheUpdate with recipe
    // Second read: client computes ssformat key, server returns tablet info
    // Third+ reads: should hit fast path

    for (int i = 0; i < 5; i++) {
      long testKey = keyValue.longValue + i;
      long startTime = System.nanoTime();

      try (ResultSet resultSet =
          dbClient.singleUse().read(tableName, KeySet.singleKey(Key.of(testKey)), columns)) {
        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
        }
        long elapsedUs = (System.nanoTime() - startTime) / 1000;
        System.out.println(
            "Read #"
                + (i + 1)
                + " (key="
                + testKey
                + "): "
                + rowCount
                + " row(s), "
                + elapsedUs
                + " us");
      }
    }

    System.out.println("Note: Subsequent reads should be faster as the location cache warms up.");
  }

  private static void testReadUsingIndex(
      DatabaseClient dbClient,
      String tableName,
      String indexName,
      String keyColumn,
      KeyValue keyValue,
      int repeat) {

    if (indexName.isEmpty()) {
      System.out.println("Skipping: set BYPASS_INDEX_NAME to run readUsingIndex.");
      return;
    }
    for (int i = 1; i <= Math.max(1, repeat); i++) {
      try (ResultSet resultSet =
          dbClient
              .singleUse()
              .readUsingIndex(
                  tableName,
                  indexName,
                  KeySet.singleKey(keyValue.asKey()),
                  Arrays.asList(keyColumn))) {
        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
        }
        System.out.println("Index read rows returned: " + rowCount);
      }
    }
  }

  private static void testExecuteQuery(
      DatabaseClient dbClient,
      String tableName,
      String keyColumn,
      String valueColumn,
      KeyValue keyValue,
      String querySql,
      int repeat) {
    Statement statement =
        buildSelectStatement(tableName, keyColumn, valueColumn, keyValue, querySql);

    for (int i = 1; i <= Math.max(1, repeat); i++) {
      try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
        }
        System.out.println("Query run #" + i + " rows returned: " + rowCount);
      }
    }
  }

  private static void testSingleUseReadOnlyTransaction(
      DatabaseClient dbClient,
      String tableName,
      String keyColumn,
      String valueColumn,
      KeyValue keyValue,
      int repeat) {
    for (int i = 1; i <= Math.max(1, repeat); i++) {
      try (ReadOnlyTransaction transaction = dbClient.singleUseReadOnlyTransaction()) {
        Statement statement = buildSelectStatement(tableName, keyColumn, valueColumn, keyValue, "");
        try (ResultSet resultSet = transaction.executeQuery(statement)) {
          int rowCount = 0;
          while (resultSet.next()) {
            rowCount++;
          }
          System.out.println("Single-use RO run #" + i + " rows returned: " + rowCount);
        }
      }
    }
  }

  private static void testReadOnlyTransaction(
      DatabaseClient dbClient,
      String tableName,
      String keyColumn,
      String valueColumn,
      KeyValue keyValue,
      int repeat) {
    for (int i = 1; i <= Math.max(1, repeat); i++) {
      try (ReadOnlyTransaction transaction = dbClient.readOnlyTransaction()) {
        Statement statement = buildSelectStatement(tableName, keyColumn, valueColumn, keyValue, "");
        try (ResultSet resultSet = transaction.executeQuery(statement)) {
          int rowCount = 0;
          while (resultSet.next()) {
            rowCount++;
          }
          System.out.println("Read-only txn run #" + i + " rows returned: " + rowCount);
        }
      }
    }
  }

  private static void testReadWriteTransactionDml(
      DatabaseClient dbClient,
      String tableName,
      String keyColumn,
      String valueColumn,
      KeyValue keyValue,
      String dmlSql,
      int repeat) {
    Statement dmlStatement = buildDmlStatement(tableName, keyColumn, valueColumn, keyValue, dmlSql);
    if (dmlStatement == null) {
      System.out.println("Skipping: set BYPASS_VALUE_COLUMN or BYPASS_DML_STATEMENT to run DML.");
      return;
    }

    for (int i = 1; i <= Math.max(1, repeat); i++) {
      TransactionRunner runner = dbClient.readWriteTransaction();
      long updateCount = runner.run(transaction -> transaction.executeUpdate(dmlStatement));
      System.out.println("DML run #" + i + " update count: " + updateCount);
    }
  }

  private static void testExplicitRollback(DatabaseClient dbClient, int repeat) {
    for (int i = 1; i <= Math.max(1, repeat); i++) {
      try (TransactionManager manager = dbClient.transactionManager()) {
        manager.begin();
        manager.rollback();
        System.out.println("Rollback run #" + i + " completed.");
      }
    }
  }

  private static void testMutationWrite(
      DatabaseClient dbClient,
      String tableName,
      String keyColumn,
      String valueColumn,
      KeyValue keyValue,
      @Nullable KeyValue mutationValue,
      int repeat) {
    if (mutationValue == null) {
      System.out.println("Skipping: set BYPASS_MUTATION_VALUE to run mutation write.");
      return;
    }
    if (valueColumn == null || valueColumn.isEmpty()) {
      System.out.println("Skipping: set BYPASS_VALUE_COLUMN to run mutation write.");
      return;
    }

    for (int i = 1; i <= Math.max(1, repeat); i++) {
      Mutation.WriteBuilder builder = Mutation.newUpdateBuilder(tableName);
      keyValue.setMutationValue(builder, keyColumn);
      mutationValue.setMutationValue(builder, valueColumn);
      dbClient.write(Arrays.asList(builder.build()));
      System.out.println("Mutation write run #" + i + " committed.");
    }
  }

  private static Statement buildSelectStatement(
      String tableName, String keyColumn, String valueColumn, KeyValue keyValue, String querySql) {
    if (querySql != null && !querySql.isEmpty()) {
      return Statement.of(querySql);
    }

    String columns = valueColumn.isEmpty() ? keyColumn : keyColumn + ", " + valueColumn;
    String sql = "SELECT " + columns + " FROM " + tableName + " WHERE " + keyColumn + " = @key";
    Statement.Builder builder = Statement.newBuilder(sql);
    keyValue.bind(builder, "key");
    return builder.build();
  }

  @Nullable
  private static Statement buildDmlStatement(
      String tableName, String keyColumn, String valueColumn, KeyValue keyValue, String dmlSql) {
    if (dmlSql != null && !dmlSql.isEmpty()) {
      return Statement.of(dmlSql);
    }
    if (valueColumn == null || valueColumn.isEmpty()) {
      return null;
    }

    String sql =
        "UPDATE "
            + tableName
            + " SET "
            + valueColumn
            + " = "
            + valueColumn
            + " WHERE "
            + keyColumn
            + " = @key";
    Statement.Builder builder = Statement.newBuilder(sql);
    keyValue.bind(builder, "key");
    return builder.build();
  }

  private static String envOrDefault(String name, String defaultValue) {
    String value = System.getenv(name);
    return (value == null || value.isEmpty()) ? defaultValue : value;
  }

  private static int envInt(String name, int defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static KeyValue parseKeyValue(String raw) {
    if (raw == null) {
      return new KeyValue(null, "1");
    }
    try {
      return new KeyValue(Long.parseLong(raw), raw);
    } catch (NumberFormatException e) {
      return new KeyValue(null, raw);
    }
  }

  @Nullable
  private static KeyValue parseOptionalValue(String raw) {
    if (raw == null || raw.isEmpty()) {
      return null;
    }
    try {
      return new KeyValue(Long.parseLong(raw), raw);
    } catch (NumberFormatException e) {
      return new KeyValue(null, raw);
    }
  }

  private static final class KeyValue {
    @Nullable private final Long longValue;
    private final String display;

    private KeyValue(@Nullable Long longValue, String display) {
      this.longValue = longValue;
      this.display = display;
    }

    private Key asKey() {
      if (longValue != null) {
        return Key.of(longValue);
      }
      return Key.of(display);
    }

    private void bind(Statement.Builder builder, String paramName) {
      if (longValue != null) {
        builder.bind(paramName).to(longValue);
      } else {
        builder.bind(paramName).to(display);
      }
    }

    private void setMutationValue(Mutation.WriteBuilder builder, String columnName) {
      if (longValue != null) {
        builder.set(columnName).to(longValue);
      } else {
        builder.set(columnName).to(display);
      }
    }

    private String readValue(ResultSet resultSet, String columnName) {
      if (longValue != null) {
        return String.valueOf(resultSet.getLong(columnName));
      }
      return resultSet.getString(columnName);
    }
  }
}
