/*
 * Copyright 2023 Google LLC
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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Hosts a bunch of utility methods/scripts that can be used while performing benchmarks to load
 * data, report latency metrics, etc.
 */
@Category(SlowTest.class)
@RunWith(JUnit4.class)
public class BenchmarkingDataLoaderUtility {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static final String PROJECT_ID = "";
  private static final String INSTANCE_ID = "";
  private static final String DATABASE_ID = "";
  private static final String SERVER_URL = "https://staging-wrenchworks.sandbox.googleapis.com";
  private static DatabaseClient client;
  private static Spanner spanner;

  @BeforeClass
  public static void beforeClass() {
    final SpannerOptions.Builder optionsBuilder =
        SpannerOptions.newBuilder()
            .setProjectId(PROJECT_ID)
            .setAutoThrottleAdministrativeRequests();
    if (!SERVER_URL.isEmpty()) {
      optionsBuilder.setHost(SERVER_URL);
    }
    final SpannerOptions options = optionsBuilder.build();
    spanner = options.getService();
    client = spanner.getDatabaseClient(DatabaseId.of(PROJECT_ID, INSTANCE_ID, DATABASE_ID));
  }

  @AfterClass
  public static void afterClass() {
    spanner.close();
  }

  /**
   * A utility which bulk inserts 10^5 records into the database in batches. The method assumes that
   * the instance/database/table is already created. It does not perform any admin operations.
   *
   * <p>Table schema used here: CREATE TABLE FOO ( id INT64 NOT NULL, BAZ INT64, BAR INT64, )
   * PRIMARY KEY(id);
   */
  @Test
  public void bulkInsertTestData() {
    String insertQuery = "INSERT INTO FOO(id, BAZ, BAR) VALUES(@key, @val1, @val2)";
    int key = 0;
    for (int batch = 0; batch < 1000; batch++) {
      List<Statement> stmts = new LinkedList<>();
      for (int i = 0; i < 100; i++) {
        stmts.add(
            Statement.newBuilder(insertQuery)
                .bind("key")
                .to(key)
                .bind("val1")
                .to(1)
                .bind("val2")
                .to(2)
                .build());
        key++;
      }
      final TransactionCallable<long[]> callable = transaction -> transaction.batchUpdate(stmts);
      TransactionRunner runner = client.readWriteTransaction();
      long[] actualRowCounts = runner.run(callable);
      assertThat(actualRowCounts.length).isEqualTo(100);
      System.out.println("Completed batch => " + batch);
    }
  }

  /**
   * Collects all results from a collection of future objects.
   *
   * @param service
   * @param results
   * @param numOperations
   * @return
   * @throws Exception
   */
  public static List<Duration> collectResults(
      final ListeningScheduledExecutorService service,
      final List<ListenableFuture<List<Duration>>> results,
      final int numOperations)
      throws Exception {
    service.shutdown();
    if (!service.awaitTermination(60L, TimeUnit.MINUTES)) {
      throw new TimeoutException();
    }
    List<Duration> allResults = new ArrayList<>(numOperations);
    for (Future<List<Duration>> result : results) {
      allResults.addAll(result.get());
    }
    return allResults;
  }

  /**
   * Utility to print latency numbers. It computes metrics such as Average, P50, P95 and P99.
   *
   * @param results
   */
  public static void printResults(List<Duration> results) {
    if (results == null) {
      return;
    }
    List<Duration> orderedResults = new ArrayList<>(results);
    Collections.sort(orderedResults);
    System.out.println();
    System.out.printf("Total number of queries: %d\n", orderedResults.size());
    System.out.printf("Avg: %fs\n", avg(results));
    System.out.printf("P50: %fs\n", percentile(50, orderedResults));
    System.out.printf("P95: %fs\n", percentile(95, orderedResults));
    System.out.printf("P99: %fs\n", percentile(99, orderedResults));
  }

  public static double percentile(int percentile, List<Duration> orderedResults) {
    int index = percentile * orderedResults.size() / 100;
    Duration value = orderedResults.get(index);
    Double convertedValue = convertDurationToFractionInSeconds(value);
    return convertedValue;
  }

  public static double avg(List<Duration> results) {
    return results.stream()
        .collect(
            Collectors.averagingDouble(
                BenchmarkingDataLoaderUtility::convertDurationToFractionInSeconds));
  }

  public static double convertDurationToFractionInSeconds(Duration duration) {
    long seconds = duration.getSeconds();
    long nanos = duration.getNano();
    double fraction = (double) nanos / 1_000_000_000;
    double value = seconds + fraction;
    return value;
  }
}
