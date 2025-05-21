/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static org.junit.Assert.assertFalse;

import io.grpc.internal.PerformanceHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ITMicroBenchmark {

  @Test
  public void testSingleUseQuery() throws InterruptedException {
    Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("span-cloud-testing")
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setWaitForMinSessionsDuration(Duration.ofSeconds(5L))
                    .setFailOnSessionLeak()
                    .build())
            .setEnableApiTracing(true)
            .build()
            .getService();
    DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("span-cloud-testing", "sakthi-spanner-eu-west10", "benchmarking"));

    final String SELECT_QUERY = "SELECT ID FROM Employees WHERE id = 1";

    Instant warmUpEndTime = Instant.now().plus(10, ChronoUnit.SECONDS);
    int waitTimeMilli = 5;

    System.out.println("Running warmup for 5 minutes, Started at " + currentTimeInIST());
    while (warmUpEndTime.isAfter(Instant.now())) {
      try (ReadContext readContext = client.singleUse()) {
        try (ResultSet resultSet = readContext.executeQuery(Statement.of(SELECT_QUERY))) {
          while (resultSet.next()) {}
        }
      }
      randomWait(waitTimeMilli);
    }
    System.out.println("Warmup completed");

    List<Long> overallRequestLatencies = new ArrayList<>();
    List<Long> gRPCRequestLatencies = new ArrayList<>();
    List<Long> clientRequestLatencies = new ArrayList<>();

    List<Long> overallResponseLatencies = new ArrayList<>();
    List<Long> gRPCResponseLatencies = new ArrayList<>();
    List<Long> clientResponseLatencies = new ArrayList<>();

    List<Long> requestInterceptorLatencies = new ArrayList<>();
    List<Long> responseInterceptorLatencies = new ArrayList<>();

    Instant perfEndTime = Instant.now().plus(10, ChronoUnit.SECONDS);

    System.out.println("Running benchmarking for 30 minutes, Started at " + currentTimeInIST());
    while (perfEndTime.isAfter(Instant.now())) {
      PerformanceHandler.resetAll();
      PerformanceHandler.CLIENT_REQUEST_OVERHEAD.start();
      PerformanceHandler.OVERALL_REQUEST_OVERHEAD.start();
      try (ReadContext readContext = client.singleUse()) {
        try (ResultSet resultSet = readContext.executeQuery(Statement.of(SELECT_QUERY))) {
          while (resultSet.next()) {}
          PerformanceHandler.OVERALL_RESPONSE_OVERHEAD.stop();
          PerformanceHandler.CLIENT_RESPONSE_OVERHEAD.stop();

          overallRequestLatencies.add(PerformanceHandler.OVERALL_REQUEST_OVERHEAD.elapsed(TimeUnit.MICROSECONDS));
          gRPCRequestLatencies.add(PerformanceHandler.GRPC_REQUEST_OVERHEAD.elapsed(TimeUnit.MICROSECONDS));
          clientRequestLatencies.add(PerformanceHandler.CLIENT_REQUEST_OVERHEAD.elapsed(TimeUnit.MICROSECONDS));

          overallResponseLatencies.add(PerformanceHandler.OVERALL_RESPONSE_OVERHEAD.elapsed(TimeUnit.MICROSECONDS));
          gRPCResponseLatencies.add(PerformanceHandler.GRPC_RESPONSE_OVERHEAD.elapsed(TimeUnit.MICROSECONDS));
          clientResponseLatencies.add(PerformanceHandler.CLIENT_RESPONSE_OVERHEAD.elapsed(TimeUnit.MICROSECONDS));

          requestInterceptorLatencies.add(PerformanceHandler.getRequestInterceptorLatency());
          responseInterceptorLatencies.add(PerformanceHandler.getResponseInterceptorLatency());
          assertFalse(resultSet.next());
        }
      }
      randomWait(waitTimeMilli);
    }
    spanner.close();

    Collections.sort(overallRequestLatencies);
    Collections.sort(gRPCRequestLatencies);
    Collections.sort(clientRequestLatencies);

    Collections.sort(overallResponseLatencies);
    Collections.sort(gRPCResponseLatencies);
    Collections.sort(clientResponseLatencies);

    Collections.sort(requestInterceptorLatencies);
    Collections.sort(responseInterceptorLatencies);

    System.out.println("Overall Request latencies: " + percentile(overallRequestLatencies, 0.5));
    System.out.println("GRPC Request latencies: " + percentile(gRPCRequestLatencies, 0.5));
    System.out.println("Client Request latencies: " + percentile(clientRequestLatencies, 0.5));

    System.out.println("Overall Response latencies: " + percentile(overallResponseLatencies, 0.5));
    System.out.println("GRPC Response latencies: " + percentile(gRPCResponseLatencies, 0.5));
    System.out.println("Client Response latencies: " + percentile(clientResponseLatencies, 0.5));

    System.out.println("Request Interceptor latencies: " + percentile(requestInterceptorLatencies, 0.5));
    System.out.println("Response Interceptor latencies: " + percentile(responseInterceptorLatencies, 0.5));
  }

  private String currentTimeInIST() {
    return ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toString();
  }

  public void randomWait(int waitMillis) throws InterruptedException {
    if (waitMillis <= 0) {
      return;
    }
    int randomMillis = ThreadLocalRandom.current().nextInt(waitMillis * 2);
    Thread.sleep(randomMillis);
  }

  public static long percentile(List<Long> latencies, double percentile) {
    int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
    return latencies.get(index - 1);
  }
}
