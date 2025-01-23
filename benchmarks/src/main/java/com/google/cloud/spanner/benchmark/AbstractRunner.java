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

package com.google.cloud.spanner.benchmark;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

abstract class AbstractRunner implements BenchmarkRunner {
  static final int TOTAL_RECORDS = 100000;
  static final int READ_RANGE = 99;
  static final String TABLE_NAME = "Employees";
  static final String SELECT_QUERY = "SELECT ID FROM Employees WHERE ID=@id";
  static final String UPDATE_QUERY = "UPDATE Employees SET NAME=SAKTHI WHERE ID = @id";
  static final String ID_COLUMN_NAME = "id";
  static final String SERVER_URL = "https://spanner.googleapis.com";

  private final AtomicReference<Instant> operationEndTime = new AtomicReference<>();

  private final AtomicBoolean operationStarted = new AtomicBoolean();

  private final AtomicReference<Instant> warmUpEndTime = new AtomicReference<>();

  protected void setOperationEndTime(Instant instant) {
    operationEndTime.set(instant);
  }

  protected void operationStarted(boolean started) {
    operationStarted.set(started);
  }

  protected void setWarmUpEndTime(Instant instant) {
    warmUpEndTime.set(instant);
  }

  protected List<Integer> collectResults(
      ExecutorService service,
      List<Future<List<Duration>>> results,
      int numClients,
      int numOperations)
      throws Exception {
    service.shutdown();
    while (!service.isTerminated()) {
      Thread.sleep(1000L);
      if (operationStarted.get()) {
        //noinspection BusyWait
        System.out.printf("\rOperation Ends in %s", showTime(operationEndTime.get()));
      } else if (warmUpEndTime.get() != null) {
        System.out.printf("\rWarm up ends in %s", showTime(warmUpEndTime.get()));
      }
    }
    System.out.print("\r\r");
    System.out.println();
    if (!service.awaitTermination(60L, TimeUnit.MINUTES)) {
      throw new TimeoutException();
    }
    List<Duration> allResults = new ArrayList<>(numClients * numOperations);
    for (Future<List<Duration>> result : results) {
      allResults.addAll(result.get());
    }
    return allResults.stream().map(Duration::getNano).collect(Collectors.toList());
  }

  private String showTime(Instant endTime) {
    long totalSeconds = ChronoUnit.SECONDS.between(Instant.now(), endTime);
    long totalMinutes = ChronoUnit.MINUTES.between(Instant.now(), endTime);

    return String.format("%s-%s", totalMinutes, totalSeconds - (totalMinutes * 60));
  }

  protected void randomWait(int waitMillis) throws InterruptedException {
    if (waitMillis <= 0) {
      return;
    }
    int randomMillis = ThreadLocalRandom.current().nextInt(waitMillis * 2);
    Thread.sleep(randomMillis);
  }
}
