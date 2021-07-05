/*
 * Copyright 2020 Google LLC
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

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.common.base.Stopwatch;
import com.google.spanner.admin.database.v1.ListDatabasesResponse;
import com.google.spanner.admin.instance.v1.ListInstancesResponse;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpannerOptionsThreadTest extends AbstractMockServerTest {
  private static final int NUMBER_OF_TEST_RUNS = 2;
  private static final int DEFAULT_NUM_CHANNELS_PER_GAPIC_CLIENT = 4;
  private static final int NUM_GAPIC_CLIENTS = 4;
  private static final int NUM_THREADS =
      Math.max(
          DEFAULT_NUM_CHANNELS_PER_GAPIC_CLIENT * NUM_GAPIC_CLIENTS,
          Runtime.getRuntime().availableProcessors());
  private static final String SPANNER_THREAD_NAME = "Cloud-Spanner-TransportChannel";
  private static final String THREAD_PATTERN = "%s-[0-9]+";

  private final DatabaseId dbId = DatabaseId.of("p", "i", "d");

  private SpannerOptions createOptions() {
    return SpannerOptions.newBuilder()
        .setProjectId("p")
        // Set a custom channel configurator to allow http instead of https.
        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
        .setHost("http://localhost:" + getPort())
        .setCredentials(NoCredentials.getInstance())
        .build();
  }

  @Test
  public void testCloseAllThreadsWhenClosingSpanner() throws InterruptedException {
    int baseThreadCount = getNumberOfThreadsWithName(SPANNER_THREAD_NAME);
    for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
      waitForStartup();
      assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME)).isAtMost(baseThreadCount);
      // Create Spanner instance.
      // We make a copy of the options instance, as SpannerOptions caches any service object
      // that has been handed out.
      SpannerOptions options = createOptions();
      Spanner spanner = options.getService();
      // Get a database client and do a query. This should initiate threads for the Spanner service.
      DatabaseClient client = spanner.getDatabaseClient(dbId);
      List<ResultSet> resultSets = new ArrayList<>();
      // SpannerStub affiliates a channel with a session, so we need to use multiple sessions
      // to ensure we also hit multiple channels.
      for (int i2 = 0; i2 < options.getSessionPoolOptions().getMaxSessions(); i2++) {
        ResultSet rs = client.singleUse().executeQuery(SELECT_COUNT_STATEMENT);
        // Execute ResultSet#next() to send the query to Spanner.
        rs.next();
        // Delay closing the result set in order to force the use of multiple sessions.
        // As each session is linked to one transport channel, using multiple different
        // sessions should initialize multiple transport channels.
        resultSets.add(rs);
        // Check whether the number of expected threads has been reached.
        if (getNumberOfThreadsWithName(SPANNER_THREAD_NAME) == NUM_THREADS + baseThreadCount) {
          break;
        }
      }
      for (ResultSet rs : resultSets) {
        rs.close();
      }
      // Check the number of threads after the query. Doing a request should initialize a thread
      // pool for the underlying SpannerClient.
      assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME))
          .isEqualTo(NUM_THREADS + baseThreadCount);

      // Then do a request to the InstanceAdmin service and check the number of threads.
      // Doing a request should initialize a thread pool for the underlying InstanceAdminClient.
      for (int i2 = 0; i2 < DEFAULT_NUM_CHANNELS_PER_GAPIC_CLIENT * 2; i2++) {
        InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
        mockInstanceAdmin.addResponse(ListInstancesResponse.getDefaultInstance());
        instanceAdminClient.listInstances();
      }
      assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME))
          .isEqualTo(NUM_THREADS + baseThreadCount);

      // Then do a request to the DatabaseAdmin service and check the number of threads.
      // Doing a request should initialize a thread pool for the underlying DatabaseAdminClient.
      for (int i2 = 0; i2 < DEFAULT_NUM_CHANNELS_PER_GAPIC_CLIENT * 2; i2++) {
        DatabaseAdminClient databaseAdminClient = spanner.getDatabaseAdminClient();
        mockDatabaseAdmin.addResponse(ListDatabasesResponse.getDefaultInstance());
        databaseAdminClient.listDatabases(dbId.getInstanceId().getInstance());
      }
      assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME))
          .isEqualTo(NUM_THREADS + baseThreadCount);

      // Now close the Spanner instance and check whether the threads are shutdown or not.
      spanner.close();
      // Wait a little to allow the threads to actually shutdown.
      Stopwatch watch = Stopwatch.createStarted();
      while (getNumberOfThreadsWithName(SPANNER_THREAD_NAME) > baseThreadCount
          && watch.elapsed(TimeUnit.SECONDS) < 2) {
        Thread.sleep(50L);
      }
      assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME)).isAtMost(baseThreadCount);
    }
  }

  @Test
  public void testMultipleSpannersFromSameSpannerOptions() throws InterruptedException {
    waitForStartup();
    int baseThreadCount = getNumberOfThreadsWithName(SPANNER_THREAD_NAME);
    SpannerOptions options = createOptions();
    try (Spanner spanner1 = options.getService()) {
      // Having both in the try-with-resources block is not possible, as it is the same instance.
      // One will be closed before the other, and the closing of the second instance would fail.
      Spanner spanner2 = options.getService();
      assertThat(spanner1).isSameInstanceAs(spanner2);
      DatabaseClient client1 = spanner1.getDatabaseClient(dbId);
      DatabaseClient client2 = spanner2.getDatabaseClient(dbId);
      assertThat(client1).isSameInstanceAs(client2);
      try (ResultSet rs1 = client1.singleUse().executeQuery(SELECT_COUNT_STATEMENT);
          ResultSet rs2 = client2.singleUse().executeQuery(SELECT_COUNT_STATEMENT)) {
        while (rs1.next() && rs2.next()) {
          // Do nothing, just consume the result sets.
        }
      }
    }
    Stopwatch watch = Stopwatch.createStarted();
    while (getNumberOfThreadsWithName(SPANNER_THREAD_NAME) > baseThreadCount
        && watch.elapsed(TimeUnit.SECONDS) < 2) {
      Thread.sleep(50L);
    }
    assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME)).isAtMost(baseThreadCount);
  }

  private void waitForStartup() throws InterruptedException {
    // Wait until the IT environment has already started all base worker threads.
    int threadCount;
    Stopwatch watch = Stopwatch.createStarted();
    do {
      threadCount = getNumberOfThreadsWithName(SPANNER_THREAD_NAME);
      Thread.sleep(100L);
    } while (getNumberOfThreadsWithName(SPANNER_THREAD_NAME) > threadCount
        && watch.elapsed(TimeUnit.SECONDS) < 5);
  }

  private int getNumberOfThreadsWithName(String serviceName) {
    Pattern pattern = Pattern.compile(String.format(THREAD_PATTERN, serviceName));
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    int res = 0;
    for (Thread thread : threadSet) {
      if (pattern.matcher(thread.getName()).matches()) {
        res++;
      }
    }
    return res;
  }
}
