/*
 * Copyright 2017 Google LLC
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

import static com.google.cloud.spanner.AbstractLatencyBenchmark.convertDurationToFraction;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for read and query.
 *
 * <p>See also {@code it/WriteIntegrationTest}, which provides coverage of writing and reading back
 * all Cloud Spanner types.
 */
@Category(SlowTest.class)
@RunWith(JUnit4.class)
public class ITAnonymousSessionsTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static Database db;
  private static DatabaseClient client;
  private static Spanner spanner;
  private static DatabaseAdminClient databaseAdminClient;
  private static String projectId;

  @BeforeClass
  public static void beforeClass() {
    final String serverUrl = "https://staging-wrenchworks.sandbox.googleapis.com";
    final SpannerOptions.Builder optionsBuilder = SpannerOptions
        .newBuilder()
        .setProjectId("span-cloud-testing")
        .setAutoThrottleAdministrativeRequests();
    if (!serverUrl.isEmpty()) {
      optionsBuilder.setHost(serverUrl);
    }
    final SpannerOptions options = optionsBuilder.build();
    projectId = options.getProjectId();
    spanner = options.getService();
    databaseAdminClient = spanner.getDatabaseAdminClient();
    client = spanner.getDatabaseClient(DatabaseId.of("span-cloud-testing",
        "arpanmishra-dev-span", "anonymous-sessions"));
  }

  @AfterClass
  public static void afterClass() {
    spanner.close();
  }
  @Test
  public void bulkInsertTestData() {
    String insertQuery = "INSERT INTO FOO(id, BAZ, BAR) VALUES(@key, @val1, @val2)";
    int key = 0;
    for(int batch = 0; batch < 1000; batch++) {
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

  @Test
  public void testConvertDurationToFraction() {
    System.out.println((convertDurationToFraction(Duration.ofNanos(10000000))));
    System.out.println((convertDurationToFraction(Duration.ofMillis(20000000))));

    Duration d = Duration.ofMillis(10);
    System.out.println(d.getNano());
    System.out.println(d.getSeconds());
  }
}
