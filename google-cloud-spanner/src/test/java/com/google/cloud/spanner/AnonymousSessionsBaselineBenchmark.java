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
 * mvn clean test -DskipTests -Pbenchmark -Dbenchmark.name=AnonymousSessionsBaselineBenchmark -Dbenchmark.database=arpanmishra-dev-span -Dbenchmark.instance=anonymous-sessions -Dbenchmark.serverUrl=https://staging-wrenchworks.sandbox.googleapis.com
 * </code>
 *
 * Test Table Schema :
 *
 * CREATE TABLE FOO (
 *   id INT64 NOT NULL,
 *   BAZ INT64,
 *   BAR INT64,
 * ) PRIMARY KEY(id);
 *
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 1)
@Measurement(batchSize = 1, iterations = 1, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
public class AnonymousSessionsBaselineBenchmark extends AbstractLatencyBenchmark {
  static final Statement SELECT_QUERY = Statement.of("SELECT id,BAZ,BAR FROM FOO WHERE ID = 1");
  static final Statement UPDATE_QUERY = Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");

  @State(Scope.Thread)
  @AuxCounters(org.openjdk.jmh.annotations.AuxCounters.Type.EVENTS)
  public static class BenchmarkState {

    private final String instance = System.getProperty("instance");
    private final String database = System.getProperty("database");
    private final String serverUrl = System.getProperty("serverUrl", "https://staging-wrenchworks.sandbox.googleapis.com");

    private Spanner spanner;
    private DatabaseClientImpl client;

    @Param({"100"})
    int minSessions;

    @Param({"400"})
    int maxSessions;

    // We are adding this configuration to see if having single session has any noticeable differences
    // as compared to having multiple sessions.
    @Param({"50", "100", "400"})
    int numSessions;

    @Setup(Level.Invocation)
    public void setup() throws Exception {
      SpannerOptions options =
          SpannerOptions.newBuilder()
              .setSessionPoolOption(
                  SessionPoolOptions.newBuilder()
                      .setMinSessions(numSessions)
                      .setMaxSessions(numSessions)
                      .setWaitForMinSessions(org.threeten.bp.Duration.ofSeconds(20)).build())
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
   * Measures the time needed to execute a burst of read requests.
   *
   * <p>Some read requests will be long-running and will cause session leaks. Such sessions will be
   * removed by the session maintenance background task if SessionPool Option
   * ActionOnInactiveTransaction is set as WARN_AND_CLOSE.
   */
  @Benchmark
  public void burstRead(final BenchmarkState server) throws Exception {
    int totalQueries = server.maxSessions * 8;
    int parallelThreads = server.maxSessions * 2;
    final DatabaseClientImpl client = server.client;
    SessionPool pool = client.pool;
    assertThat(pool.totalSessions()).isEqualTo(
        server.spanner.getOptions().getSessionPoolOptions().getMinSessions());

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(parallelThreads));
    List<ListenableFuture<Duration>> futures = new ArrayList<>(totalQueries);
    for (int i = 0; i < totalQueries; i++) {
      futures.add(
          service.submit(
              () -> runBenchmarkForReads(server)));
    }

    final List<java.time.Duration> results =
        collectResults(service, futures, totalQueries);

    System.out.printf("Num Sessions: %d\n", server.numSessions);

    printResults(results);
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
   */
  @Benchmark
  public void burstReadAndWrite(final BenchmarkState server) throws Exception {
    int totalWrites = server.maxSessions * 4;
    int totalReads = server.maxSessions * 4;
    int parallelThreads = server.maxSessions * 2;
    final DatabaseClientImpl client = server.client;
    SessionPool pool = client.pool;
    assertThat(pool.totalSessions()).isEqualTo(
        server.spanner.getOptions().getSessionPoolOptions().getMinSessions());

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(parallelThreads));
    List<ListenableFuture<java.time.Duration>> futures =
        new ArrayList<>(totalReads + totalWrites);
    for (int i = 0; i < totalReads; i++) {
      futures.add(
          service.submit(
              () -> runBenchmarkForReads(server)));
    }
    for (int i = 0; i < totalWrites; i++) {
      futures.add(
          service.submit(
              () -> runBenchmarkForWrites(server)));
    }

    final List<java.time.Duration> results =
        collectResults(service, futures, totalReads + totalWrites);

    System.out.printf("Num Sessions: %d\n", server.numSessions);

    printResults(results);
  }

  private java.time.Duration runBenchmarkForReads(final BenchmarkState server) {
    Stopwatch watch = Stopwatch.createStarted();

    try (ResultSet rs =
        server.client.singleUse().executeQuery(SELECT_QUERY)) {
      while (rs.next()) {}
    }

    return watch.elapsed();
  }
  private java.time.Duration runBenchmarkForWrites(final BenchmarkState server) {
    Stopwatch watch = Stopwatch.createStarted();

    TransactionRunner runner = server.client.readWriteTransaction();
    runner.run(transaction -> transaction.executeUpdate(UPDATE_QUERY));

    return watch.elapsed();
  }
}
