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

import com.google.cloud.spanner.SessionPoolOptions.ActionForAnonymousSessionsChannelHints;
import com.google.cloud.spanner.SessionPoolOptions.ActionForNumberOfAnonymousSessions;
import com.google.cloud.spanner.SessionPoolOptions.AnonymousSessionOptions;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
 * profile `benchmark` and can be executed like this:
 *
 * <code>
 * mvn clean test -DskipTests -Pbenchmark -Dbenchmark.name=AnonymousSessionsWithSharedSessionsMultiChannelBenchmark -Dbenchmark.database=arpanmishra-dev-span -Dbenchmark.instance=anonymous-sessions -Dbenchmark.serverUrl=https://staging-wrenchworks.sandbox.googleapis.com
 * </code>
 *
 * Test Table Schema :
 *
 * CREATE TABLE FOO (
 *   id INT64 NOT NULL,
 *   BAZ INT64,
 *   BAR INT64,
 * ) PRIMARY KEY(id);
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Measurement(batchSize = 1, iterations = 1, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1)
public class AnonymousSessionsWithSharedSessionsMultiChannelBenchmark extends AbstractAnonymousSessionsBenchmark {
  @State(Scope.Thread)
  @AuxCounters(org.openjdk.jmh.annotations.AuxCounters.Type.EVENTS)
  public static class BenchmarkState {

    private final String instance = System.getProperty("benchmark.instance", "arpanmishra-dev-span");
    private final String database = System.getProperty("benchmark.database", "anonymous-sessions");
    private final String serverUrl = System.getProperty("benchmark.serverUrl",
        "https://staging-wrenchworks.sandbox.googleapis.com");

    private Spanner spanner;
    private DatabaseClientImpl client;

    @Param({"100"})
    int minSessions;

    @Param({"400"})
    int maxSessions;

    @Setup(Level.Invocation)
    public void setup() throws Exception {
      AnonymousSessionOptions anonymousSessionOptions =
          AnonymousSessionOptions.newBuilder()
              .setActionForAnonymousSessionsChannelHints(
                  ActionForAnonymousSessionsChannelHints.MULTI_CHANNEL)
              .setActionForNumberOfAnonymousSessions(
                  ActionForNumberOfAnonymousSessions.SHARED_SESSION).build();
      SpannerOptions options =
          SpannerOptions.newBuilder()
              .setSessionPoolOption(
                  SessionPoolOptions.newBuilder()
                      .setMinSessions(minSessions)
                      .setMaxSessions(maxSessions)
                      .setAnonymousSessionOptions(anonymousSessionOptions)
                      .setWaitForMinSessions(
                          org.threeten.bp.Duration.ofSeconds(20)).build())
              .setHost(serverUrl)
              .build();
      spanner = options.getService();
      System.out.println("running benchmark with **REAL** server");
      System.out.println("instance: " + instance);
      System.out.println("database: " + database);
      client =
          (DatabaseClientImpl)
              spanner.getDatabaseClient(DatabaseId.of(options.getProjectId(), instance, database));
    }

    @TearDown(Level.Invocation)
    public void teardown() throws Exception {
      spanner.close();
    }
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
  public void burstReadAndWrite_withSharedROSessions(final BenchmarkState server) throws Exception {
    Stopwatch watch = Stopwatch.createStarted();

    final DatabaseClientImpl client = server.client;
    SessionPool pool = client.pool;
    assertThat(pool.totalSessions()).isEqualTo(
        server.spanner.getOptions().getSessionPoolOptions().getMinSessions());
    assertThat(pool.totalSharedSessions()).isEqualTo(
        server.spanner.getOptions().getSessionPoolOptions().getSharedSessionCount());
    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(PARALLEL_THREADS));
    List<ListenableFuture<List<Duration>>> results = new ArrayList<>(PARALLEL_THREADS);
    for (int i = 0; i < PARALLEL_THREADS; i++) {
      results.add(service.submit(() -> runBenchmarksForReads(server, TOTAL_READS_PER_THREAD)));
    }
    for (int i = 0; i < PARALLEL_THREADS; i++) {
      results.add(
          service.submit(() -> runBenchmarkForWrites(server, TOTAL_WRITES_PER_THREAD)));
    }

    collectResultsAndPrint(service, results);
    Duration elapsedTime = watch.elapsed();
    System.out.printf("Total Execution Time: %.2fs\n", convertDurationToFractionInSeconds(elapsedTime));
  }

  private void collectResultsAndPrint(ListeningScheduledExecutorService service,
      List<ListenableFuture<List<Duration>>> results) throws Exception {
    final List<java.time.Duration> collectResults =
        collectResults(service, results, TOTAL_READS_PER_THREAD * PARALLEL_THREADS);
    printResults(collectResults);
  }

  private List<java.time.Duration> runBenchmarksForReads(
      final BenchmarkState server, int numberOfOperations) {
    List<Duration> results = new ArrayList<>(numberOfOperations);
    // Execute one query to make sure everything has been warmed up.
    executeQuery(server);

    for (int i = 0; i < numberOfOperations; i++) {
      results.add(executeQuery(server));
    }
    return results;
  }
  private Duration executeQuery(final BenchmarkState server) {
    Stopwatch watch = Stopwatch.createStarted();

    try (ResultSet rs =
        server.client.singleUseWithSharedSession().executeQuery(getRandomisedReadStatement())) {
      while (rs.next()) {}
    }

    return watch.elapsed();
  }

  private List<java.time.Duration> runBenchmarkForWrites(
      final BenchmarkState server, int numberOfOperations) {
    List<Duration> results = new ArrayList<>(numberOfOperations);
    // Execute one update to make sure everything has been warmed up.
    executeUpdate(server);

    for (int i = 0; i < numberOfOperations; i++) {
      results.add(executeUpdate(server));
    }
    return results;
  }
  private Duration executeUpdate(final BenchmarkState server) {
    Stopwatch watch = Stopwatch.createStarted();

    TransactionRunner runner = server.client.readWriteTransaction();
    runner.run(transaction -> transaction.executeUpdate(getRandomisedUpdateStatement()));

    return watch.elapsed();
  }
}

