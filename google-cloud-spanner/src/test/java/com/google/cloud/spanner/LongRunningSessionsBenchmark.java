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

import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.SessionPoolOptions.ActionOnInactiveTransaction;
import com.google.cloud.spanner.SessionPoolOptions.InactiveTransactionRemovalOptions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks for long-running sessions scenarios. The simulated execution times are based on
 * reasonable estimates and are primarily intended to keep the benchmarks comparable with each other
 * before and after changes have been made to the pool. The benchmarks are bound to the Maven
 * profile `benchmark` and can be executed like this: <code>
 * mvn clean test -DskipTests -Pbenchmark -Dbenchmark.name=LongRunningSessionsBenchmark
 * </code>
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Measurement(batchSize = 1, iterations = 1, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(batchSize = 0, iterations = 0)
@OutputTimeUnit(TimeUnit.SECONDS)
public class LongRunningSessionsBenchmark {
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private static final int HOLD_SESSION_TIME = 100;
  private static final int LONG_HOLD_SESSION_TIME = 10000; // 10 seconds
  private static final int RND_WAIT_TIME_BETWEEN_REQUESTS = 100;
  private static final Random RND = new Random();

  @State(Scope.Thread)
  @AuxCounters(org.openjdk.jmh.annotations.AuxCounters.Type.EVENTS)
  public static class BenchmarkState {
    private StandardBenchmarkMockServer mockServer;
    private Spanner spanner;
    private DatabaseClientImpl client;
    private AtomicInteger longRunningSessions;

    @Param({"100"})
    int minSessions;

    @Param({"400"})
    int maxSessions;

    @Param({"4"})
    int numChannels;

    /** AuxCounter for number of RPCs. */
    public int numBatchCreateSessionsRpcs() {
      return mockServer.countRequests(BatchCreateSessionsRequest.class);
    }

    /** AuxCounter for number of sessions created. */
    public int sessionsCreated() {
      return mockServer.getMockSpanner().numSessionsCreated();
    }

    @Setup(Level.Invocation)
    public void setup() throws Exception {
      mockServer = new StandardBenchmarkMockServer();
      longRunningSessions = new AtomicInteger();
      TransportChannelProvider channelProvider = mockServer.start();

      /**
       * This ensures that the background thread responsible for cleaning long-running sessions
       * executes every 10s. Any transaction for which session has not been used for more than 2s
       * will be treated as long-running.
       */
      InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
          InactiveTransactionRemovalOptions.newBuilder()
              .setActionOnInactiveTransaction(ActionOnInactiveTransaction.WARN_AND_CLOSE)
              .setExecutionFrequency(Duration.ofSeconds(10))
              .setIdleTimeThreshold(Duration.ofSeconds(2))
              .build();
      SpannerOptions options =
          SpannerOptions.newBuilder()
              .setProjectId(TEST_PROJECT)
              .setChannelProvider(channelProvider)
              .setNumChannels(numChannels)
              .setCredentials(NoCredentials.getInstance())
              .setSessionPoolOption(
                  SessionPoolOptions.newBuilder()
                      .setMinSessions(minSessions)
                      .setMaxSessions(maxSessions)
                      .setWaitForMinSessionsDuration(Duration.ofSeconds(5))
                      .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions)
                      .build())
              .build();

      spanner = options.getService();
      client =
          (DatabaseClientImpl)
              spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    }

    @TearDown(Level.Invocation)
    public void teardown() throws Exception {
      spanner.close();
      mockServer.shutdown();
    }
  }

  /**
   * Measures the time needed to execute a burst of read requests.
   *
   * <p>Some read requests will be long-running and will cause session leaks. Such sessions will be
   * removed by the session maintenance background task if SessionPool Option
   * ActionOnInactiveTransaction is set as WARN_AND_CLOSE.
   *
   * @param server
   * @throws Exception
   */
  @Benchmark
  public void burstRead(final BenchmarkState server) throws Exception {
    int totalQueries = server.maxSessions * 8;
    int parallelThreads = server.maxSessions * 2;
    final DatabaseClient client =
        server.spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    SessionPool pool = ((DatabaseClientImpl) client).pool;
    assertThat(pool.totalSessions()).isEqualTo(server.minSessions);

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(parallelThreads));
    List<ListenableFuture<?>> futures = new ArrayList<>(totalQueries);
    for (int i = 0; i < totalQueries; i++) {
      futures.add(
          service.submit(
              () -> {
                Thread.sleep(RND.nextInt(RND_WAIT_TIME_BETWEEN_REQUESTS));
                try (ResultSet rs =
                    client.singleUse().executeQuery(StandardBenchmarkMockServer.SELECT1)) {
                  while (rs.next()) {
                    // introduce random sleep times to have long-running sessions
                    randomWait(server);
                  }
                  return null;
                }
              }));
    }
    // explicitly run the maintenance cycle to clean up any dangling long-running sessions.
    pool.poolMaintainer.maintainPool();

    Futures.allAsList(futures).get();
    service.shutdown();
    assertNumLeakedSessionsRemoved(server, pool);
  }

  /**
   * Measures the time needed to execute a burst of write requests (PDML).
   *
   * <p>Some write requests will be long-running. The test asserts that no sessions are removed by
   * the session maintenance background task with SessionPool Option ActionOnInactiveTransaction set
   * as WARN_AND_CLOSE. This is because PDML writes are expected to be long-running.
   *
   * @param server
   * @throws Exception
   */
  @Benchmark
  public void burstWrite(final BenchmarkState server) throws Exception {
    int totalWrites = server.maxSessions * 8;
    int parallelThreads = server.maxSessions * 2;
    final DatabaseClient client =
        server.spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    SessionPool pool = ((DatabaseClientImpl) client).pool;
    assertThat(pool.totalSessions()).isEqualTo(server.minSessions);

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(parallelThreads));
    List<ListenableFuture<?>> futures = new ArrayList<>(totalWrites);
    for (int i = 0; i < totalWrites; i++) {
      futures.add(
          service.submit(
              () -> {
                // introduce random sleep times so that some sessions are long-running sessions
                randomWaitForMockServer(server);
                client.executePartitionedUpdate(StandardBenchmarkMockServer.UPDATE_STATEMENT);
              }));
    }
    // explicitly run the maintenance cycle to clean up any dangling long-running sessions.
    pool.poolMaintainer.maintainPool();

    Futures.allAsList(futures).get();
    service.shutdown();
    assertThat(pool.numLeakedSessionsRemoved())
        .isEqualTo(0); // no sessions should be cleaned up in case of partitioned updates.
  }

  /**
   * Measures the time needed to execute a burst of read and write requests.
   *
   * <p>Some read requests will be long-running and will cause session leaks. Such sessions will be
   * removed by the session maintenance background task if SessionPool Option
   * ActionOnInactiveTransaction is set as WARN_AND_CLOSE.
   *
   * <p>Some write requests will be long-running. The test asserts that no sessions are removed by
   * the session maintenance background task with SessionPool Option ActionOnInactiveTransaction set
   * as WARN_AND_CLOSE. This is because PDML writes are expected to be long-running.
   *
   * @param server
   * @throws Exception
   */
  @Benchmark
  public void burstReadAndWrite(final BenchmarkState server) throws Exception {
    int totalWrites = server.maxSessions * 4;
    int totalReads = server.maxSessions * 4;
    int parallelThreads = server.maxSessions * 2;
    final DatabaseClient client =
        server.spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    SessionPool pool = ((DatabaseClientImpl) client).pool;
    assertThat(pool.totalSessions()).isEqualTo(server.minSessions);

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(parallelThreads));
    List<ListenableFuture<?>> futures = new ArrayList<>(totalReads + totalWrites);
    for (int i = 0; i < totalWrites; i++) {
      futures.add(
          service.submit(
              () -> {
                // introduce random sleep times so that some sessions are long-running sessions
                randomWaitForMockServer(server);
                client.executePartitionedUpdate(StandardBenchmarkMockServer.UPDATE_STATEMENT);
              }));
    }
    for (int i = 0; i < totalReads; i++) {
      futures.add(
          service.submit(
              () -> {
                Thread.sleep(RND.nextInt(RND_WAIT_TIME_BETWEEN_REQUESTS));
                try (ResultSet rs =
                    client.singleUse().executeQuery(StandardBenchmarkMockServer.SELECT1)) {
                  while (rs.next()) {
                    // introduce random sleep times to have long-running sessions
                    randomWait(server);
                  }
                  return null;
                }
              }));
    }
    // explicitly run the maintenance cycle to clean up any dangling long-running sessions.
    pool.poolMaintainer.maintainPool();

    Futures.allAsList(futures).get();
    service.shutdown();
    assertNumLeakedSessionsRemoved(server, pool);
  }

  private void randomWait(final BenchmarkState server) throws InterruptedException {
    if (RND.nextBoolean()) {
      server.longRunningSessions.incrementAndGet();
      Thread.sleep(LONG_HOLD_SESSION_TIME);
    } else {
      Thread.sleep(HOLD_SESSION_TIME);
    }
  }

  private void randomWaitForMockServer(final BenchmarkState server) {
    if (RND.nextBoolean()) {
      server.longRunningSessions.incrementAndGet();
      server
          .mockServer
          .getMockSpanner()
          .setExecuteStreamingSqlExecutionTime(
              SimulatedExecutionTime.ofMinimumAndRandomTime(LONG_HOLD_SESSION_TIME, 0));
    } else {
      server
          .mockServer
          .getMockSpanner()
          .setExecuteStreamingSqlExecutionTime(
              SimulatedExecutionTime.ofMinimumAndRandomTime(HOLD_SESSION_TIME, 0));
    }
  }

  private void assertNumLeakedSessionsRemoved(final BenchmarkState server, final SessionPool pool) {
    final SessionPoolOptions sessionPoolOptions =
        server.spanner.getOptions().getSessionPoolOptions();
    assertThat(server.longRunningSessions.get()).isNotEqualTo(0);
    if (sessionPoolOptions.warnAndCloseInactiveTransactions()
        || sessionPoolOptions.closeInactiveTransactions()) {
      assertThat(pool.numLeakedSessionsRemoved()).isGreaterThan(0);
    } else if (sessionPoolOptions.warnInactiveTransactions()) {
      assertThat(pool.numLeakedSessionsRemoved()).isEqualTo(0);
    }
  }
}
