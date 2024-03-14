/*
 * Copyright 2024 Google LLC
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractLatencyBenchmark {

  /** Utility to print latency numbers. It computes metrics such as Average, P50, P95 and P99. */
  public void printResults(List<Duration> results) {
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

  private double percentile(int percentile, List<Duration> orderedResults) {
    int index = percentile * orderedResults.size() / 100;
    Duration value = orderedResults.get(index);
    Double convertedValue = convertDurationToFractionInSeconds(value);
    return convertedValue;
  }

  /** Returns the average duration in seconds from a list of duration values. */
  private double avg(List<Duration> results) {
    return results.stream()
        .collect(Collectors.averagingDouble(this::convertDurationToFractionInSeconds));
  }

  private double convertDurationToFractionInSeconds(Duration duration) {
    long seconds = duration.getSeconds();
    long nanos = duration.getNano();
    double fraction = (double) nanos / 1_000_000_000;
    double value = seconds + fraction;
    return value;
  }
}
