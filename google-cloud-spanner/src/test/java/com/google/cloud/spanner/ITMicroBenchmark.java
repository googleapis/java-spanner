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

import com.google.common.base.Stopwatch;
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
            .setEmulatorHost("localhost:9010")
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setWaitForMinSessionsDuration(Duration.ofSeconds(5L))
                    .setFailOnSessionLeak()
                    .build())
            .setEnableApiTracing(true)
            .build()
            .getService();
    String instanceName = System.getenv("SPANNER_DATABASE") == null ? "sakthi-spanner-eu-west10" : System.getenv("SPANNER_DATABASE");
    DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("span-cloud-testing", instanceName, "benchmarking"));

    final String SELECT_QUERY = "SELECT ID FROM Employees WHERE id = 1";

    int waitTimeMilli = 5;
    Instant perfEndTime = Instant.now().plus(5, ChronoUnit.MINUTES);

    System.out.println("Running benchmarking for 5 minutes, Started at " + currentTimeInIST());
    while (perfEndTime.isAfter(Instant.now())) {
      try (ReadContext readContext = client.singleUse()) {
        try (ResultSet resultSet = readContext.executeQuery(Statement.of(SELECT_QUERY))) {
          while (resultSet.next()) {}
          assertFalse(resultSet.next());
        }
        randomWait(waitTimeMilli);
      }
    }
    spanner.close();
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
}