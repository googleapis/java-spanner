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

import static com.google.cloud.spanner.BenchmarkingDataLoaderUtility.collectResults;
import static com.google.cloud.spanner.BenchmarkingDataLoaderUtility.printResults;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks for measuring existing latencies of various APIs using the Java Client. The benchmarks
 * are bound to the Maven profile `benchmark` and can be executed like this: <code>
 * mvn clean test -DskipTests -Pbenchmark -Dbenchmark.name=DefaultBenchmark
 * </code> Test Table Schema :
 *
 * <p>CREATE TABLE FOO ( id INT64 NOT NULL, BAZ INT64, BAR INT64, ) PRIMARY KEY(id);
 *
 * <p>Below are a few considerations here: 1. We use all default options for this test because that
 * is what most customers would be using. 2. The test schema uses a numeric primary key. To ensure
 * that the reads/updates are distributed across a large query space, we insert 10^5 records.
 * Utility at {@link BenchmarkingDataLoaderUtility} can be used for loading data. 3. For queries, we
 * make sure that the query is sampled randomly across a large query space. This ensure we don't
 * cause hot-spots. 4. For avoid cold start issues, we execute 1 query/update and ignore its latency
 * from the final reported metrics.
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Measurement(batchSize = 1, iterations = 1, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1)
public class DefaultBenchmark {
  private static final String SELECT_QUERY = "SELECT ID FROM FOO WHERE ID = @id";
  private static final String UPDATE_QUERY = "UPDATE FOO SET BAR=1 WHERE ID = @id";
  private static final String ID_COLUMN_NAME = "id";
  private static final int PARALLEL_THREADS = 1;
  private static final int TOTAL_READS_PER_THREAD = 12000;
  private static final int TOTAL_WRITES_PER_THREAD = 4000;
  private static final int WARMUP_TRANSACTIONS = 1;
  private static final int RANDOM_SEARCH_SPACE = 99999;

  @State(Scope.Thread)
  @AuxCounters(org.openjdk.jmh.annotations.AuxCounters.Type.EVENTS)
  public static class BenchmarkState {

    private static final String INSTANCE_ID = "";
    private static final String DATABASE_ID = "";
    private static final String SERVER_URL = "";
    private Spanner spanner;
    private DatabaseClientImpl client;

    @Setup(Level.Invocation)
    public void setup() throws Exception {
      SpannerOptions options =
          SpannerOptions.newBuilder()
              .setSessionPoolOption(
                  SessionPoolOptions.newBuilder()
                      .setWaitForMinSessions(org.threeten.bp.Duration.ofSeconds(20))
                      .build())
              .setHost(SERVER_URL)
              .build();
      spanner = options.getService();
      client =
          (DatabaseClientImpl)
              spanner.getDatabaseClient(
                  DatabaseId.of(options.getProjectId(), INSTANCE_ID, DATABASE_ID));
    }

    @TearDown(Level.Invocation)
    public void teardown() throws Exception {
      spanner.close();
    }
  }

  /** Measures the time needed to execute a burst of queries. */
  @Benchmark
  public void burstQueries(final BenchmarkState server) throws Exception {
    final DatabaseClientImpl client = server.client;
    SessionPool pool = client.pool;
    assertThat(pool.totalSessions())
        .isEqualTo(server.spanner.getOptions().getSessionPoolOptions().getMinSessions());

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(PARALLEL_THREADS));
    List<ListenableFuture<List<Duration>>> results = new ArrayList<>(PARALLEL_THREADS);
    for (int i = 0; i < PARALLEL_THREADS; i++) {
      results.add(service.submit(() -> runBenchmarksForQueries(server, TOTAL_READS_PER_THREAD)));
    }
    collectResultsAndPrint(service, results, TOTAL_READS_PER_THREAD);
  }

  /** Measures the time needed to execute a burst of read and write requests. */
  @Benchmark
  public void burstQueriesAndWrites(final BenchmarkState server) throws Exception {
    final DatabaseClientImpl client = server.client;
    SessionPool pool = client.pool;
    assertThat(pool.totalSessions())
        .isEqualTo(server.spanner.getOptions().getSessionPoolOptions().getMinSessions());

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(PARALLEL_THREADS));
    List<ListenableFuture<List<Duration>>> results = new ArrayList<>(PARALLEL_THREADS);
    for (int i = 0; i < PARALLEL_THREADS; i++) {
      results.add(service.submit(() -> runBenchmarksForQueries(server, TOTAL_READS_PER_THREAD)));
    }
    for (int i = 0; i < PARALLEL_THREADS; i++) {
      results.add(service.submit(() -> runBenchmarkForUpdates(server, TOTAL_WRITES_PER_THREAD)));
    }

    collectResultsAndPrint(service, results, TOTAL_READS_PER_THREAD + TOTAL_WRITES_PER_THREAD);
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
  public void burstUpdates(final BenchmarkState server) throws Exception {
    final DatabaseClientImpl client = server.client;
    SessionPool pool = client.pool;
    assertThat(pool.totalSessions())
        .isEqualTo(server.spanner.getOptions().getSessionPoolOptions().getMinSessions());

    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(PARALLEL_THREADS));
    List<ListenableFuture<List<Duration>>> results = new ArrayList<>(PARALLEL_THREADS);
    for (int i = 0; i < PARALLEL_THREADS; i++) {
      results.add(service.submit(() -> runBenchmarkForUpdates(server, TOTAL_WRITES_PER_THREAD)));
    }

    collectResultsAndPrint(service, results, TOTAL_WRITES_PER_THREAD);
  }

  private List<java.time.Duration> runBenchmarksForQueries(
      final BenchmarkState server, int numberOfOperations) {
    List<Duration> results = new ArrayList<>(numberOfOperations);
    // Execute one query to make sure everything has been warmed up.
    executeWarmup(server);

    for (int i = 0; i < numberOfOperations; i++) {
      results.add(executeQuery(server));
    }
    return results;
  }

  private void executeWarmup(final BenchmarkState server) {
    for (int i = 0; i < WARMUP_TRANSACTIONS; i++) {
      executeQuery(server);
    }
  }

  private java.time.Duration executeQuery(final BenchmarkState server) {
    Stopwatch watch = Stopwatch.createStarted();

    try (ResultSet rs = server.client.singleUse().executeQuery(getRandomisedReadStatement())) {
      while (rs.next()) {
        int count = rs.getColumnCount();
      }
    }
    return watch.elapsed();
  }

  private List<java.time.Duration> runBenchmarkForUpdates(
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

  static Statement getRandomisedReadStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(RANDOM_SEARCH_SPACE);
    return Statement.newBuilder(SELECT_QUERY).bind(ID_COLUMN_NAME).to(randomKey).build();
  }

  static Statement getRandomisedUpdateStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(RANDOM_SEARCH_SPACE);
    return Statement.newBuilder(UPDATE_QUERY).bind(ID_COLUMN_NAME).to(randomKey).build();
  }

  static void collectResultsAndPrint(
      ListeningScheduledExecutorService service,
      List<ListenableFuture<List<Duration>>> results,
      int numOperationsPerThread)
      throws Exception {
    final List<java.time.Duration> collectResults =
        collectResults(service, results, numOperationsPerThread * PARALLEL_THREADS);
    printResults(collectResults);
  }
}
