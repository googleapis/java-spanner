/*
 * Copyright 2021 Google LLC
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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.spanner.SpannerOptions.FixedCloseableExecutorProvider;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;

public class LatencyTest {

  public static void main(String[] args) throws Exception {
    ThreadFactory threadFactory =
        ThreadFactoryUtil.tryCreateVirtualThreadFactory("spanner-async-worker");
    if (threadFactory == null) {
      return;
    }
    ScheduledExecutorService service = Executors.newScheduledThreadPool(0, threadFactory);
    Spanner spanner =
        SpannerOptions.newBuilder()
            .setCredentials(
                GoogleCredentials.fromStream(
                    Files.newInputStream(
                        Paths.get("/Users/loite/Downloads/appdev-soda-spanner-staging.json"))))
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setWaitForMinSessionsDuration(Duration.ofSeconds(5L))
                    // .setUseMultiplexedSession(true)
                    .build())
            .setUseVirtualThreads(true)
            .setAsyncExecutorProvider(FixedCloseableExecutorProvider.create(service))
            .build()
            .getService();
    DatabaseClient client =
        spanner.getDatabaseClient(
            DatabaseId.of("appdev-soda-spanner-staging", "knut-test-ycsb", "latencytest"));
    for (int i = 0; i < 1000000; i++) {
      try (AsyncResultSet resultSet =
          client
              .singleUse()
              .executeQueryAsync(
                  Statement.newBuilder("select col_varchar from latency_test where col_bigint=$1")
                      .bind("p1")
                      .to(ThreadLocalRandom.current().nextLong(100000L))
                      .build())) {
        while (resultSet.next()) {
          for (int col = 0; col < resultSet.getColumnCount(); col++) {
            if (resultSet.getValue(col) == null) {
              throw new IllegalStateException();
            }
          }
        }
      }
    }
  }
}
