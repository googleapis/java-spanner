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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

abstract class AbstractLatencyBenchmark {

  /**
   * Collects all results from a collection of future objects.
   *
   * @param service
   * @param results
   * @param numOperations
   * @return
   * @throws Exception
   */
  protected List<Duration> collectResults(
      final ListeningScheduledExecutorService service,
      final List<ListenableFuture<Duration>> results,
      final int numOperations)
      throws Exception {
    service.shutdown();
    if (!service.awaitTermination(60L, TimeUnit.MINUTES)) {
      throw new TimeoutException();
    }
    List<Duration> allResults = new ArrayList<>(numOperations);
    for (Future<Duration> result : results) {
      allResults.add(result.get());
    }
    return allResults;
  }

  protected void printResults(List<Duration> results) {
    if (results == null) {
      return;
    }
    List<Duration> orderedResults = new ArrayList<>(results);
    Collections.sort(orderedResults);
    System.out.println();
    System.out.printf("Total number of queries: %d\n", orderedResults.size());
    System.out.printf("Avg: %.2fms\n", avg(results));
    System.out.printf("P50: %.2fms\n", percentile(50, orderedResults));
    System.out.printf("P95: %.2fms\n", percentile(95, orderedResults));
    System.out.printf("P99: %.2fms\n", percentile(99, orderedResults));
  }
  private double percentile(int percentile, List<Duration> orderedResults) {
    return orderedResults.get(percentile * orderedResults.size() / 100).get(ChronoUnit.NANOS)
        / 1_000_000.0f;
  }

  private double avg(List<Duration> results) {
    return results.stream()
        .collect(Collectors.averagingDouble(result -> result.get(ChronoUnit.NANOS) / 1_000_000.0f));
  }
}
