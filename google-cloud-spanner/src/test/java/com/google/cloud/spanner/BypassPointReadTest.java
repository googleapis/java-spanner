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
  private static final String INSTANCE_ID = "default";
  private static final String DATABASE_ID = "db";
  private static final String TABLE_NAME = "T";
  private static final String KEY_COLUMN = "Key";

  public static void main(String[] args) {
    String host = System.getenv("BYPASS_HOST");
    if (host == null || host.isEmpty()) {
      host = DEFAULT_HOST;
    }

    System.out.println("=== Bypass Point Read Test ===");
    System.out.println("Connecting to bypass server: " + host);

    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setExperimentalHost(host)
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .setCredentials(NoCredentials.getInstance())
            .build();

    try (Spanner spanner = options.getService()) {
      DatabaseClient dbClient =
          spanner.getDatabaseClient(DatabaseId.of("default", INSTANCE_ID, DATABASE_ID));

      System.out.println("\n--- Test 1: Point Read ---");
      testPointRead(dbClient);

      System.out.println("\n--- Test 2: Multiple Point Reads (cache warm-up) ---");
      testMultiplePointReads(dbClient);

      System.out.println("\n=== All tests completed successfully! ===");

    } catch (Exception e) {
      System.err.println("Test failed with exception:");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void testPointRead(DatabaseClient dbClient) {
      long testKey = 1L;
      List<String> columns = Arrays.asList(KEY_COLUMN, "Value");

    System.out.println("Reading key: " + testKey);
    System.out.println("Columns: " + columns);

    try (ResultSet resultSet =
        dbClient.singleUse().read(TABLE_NAME, KeySet.singleKey(Key.of(testKey)), columns)) {

      int rowCount = 0;
      while (resultSet.next()) {
        rowCount++;
        System.out.println("  Row " + rowCount + ": Key=" + resultSet.getLong(KEY_COLUMN));
        if (columns.size() > 1) {
          try {
              System.out.println("           Value=" + resultSet.getString("Value"));
          } catch (Exception e) {
            // Column might not exist
          }
        }
      }
      System.out.println("Total rows returned: " + rowCount);

      if (rowCount == 0) {
        System.out.println("WARNING: No rows returned. Make sure the table has data.");
      }
    }
  }

  private static void testMultiplePointReads(DatabaseClient dbClient) {
    List<String> columns = Arrays.asList(KEY_COLUMN);

    // Perform multiple reads to test cache warm-up
    // First read: cache miss, server returns CacheUpdate with recipe
    // Second read: client computes ssformat key, server returns tablet info
    // Third+ reads: should hit fast path

    for (int i = 1; i <= 5; i++) {
        long testKey = i;
      long startTime = System.nanoTime();

      try (ResultSet resultSet =
          dbClient.singleUse().read(TABLE_NAME, KeySet.singleKey(Key.of(testKey)), columns)) {
        int rowCount = 0;
        while (resultSet.next()) {
          rowCount++;
        }
        long elapsedUs = (System.nanoTime() - startTime) / 1000;
        System.out.println(
            "Read #" + i + " (key=" + testKey + "): " + rowCount + " row(s), " + elapsedUs + " us");
      }
    }

    System.out.println("\nNote: Subsequent reads should be faster as the location cache warms up.");
  }
}
