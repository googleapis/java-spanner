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

import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
 * Benchmarks for common session pool scenarios. The simulated execution times are based on
 * reasonable estimates and are primarily intended to keep the benchmarks comparable with each other
 * before and after changes have been made to the pool. The benchmarks are bound to the Maven
 * profile `benchmark` and can be executed like this: <code>
 * mvn clean test -DskipTests -Pbenchmark -Dbenchmark.name=SessionPoolBenchmark
 * </code>
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Measurement(batchSize = 1, iterations = 1, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(batchSize = 0, iterations = 0)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SessionPoolBenchmark {
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private static final int HOLD_SESSION_TIME = 100;
  private static final int RND_WAIT_TIME_BETWEEN_REQUESTS = 10;
  private static final Random RND = new Random();

  @State(Scope.Thread)
  @AuxCounters(org.openjdk.jmh.annotations.AuxCounters.Type.EVENTS)
  public static class BenchmarkState {
    private StandardBenchmarkMockServer mockServer;
    private Spanner spanner;
    private DatabaseClientImpl client;

    @Param({"100"})
    int minSessions;

    @Param({"400"})
    int maxSessions;

    @Param({"1", "10", "20", "25", "30", "40", "50", "100"})
    int incStep;

    @Param({"4"})
    int numChannels;

    @Param({"0.2"})
    float writeFraction;

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
      TransportChannelProvider channelProvider = mockServer.start();

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
                      .setIncStep(incStep)
                      .setWriteSessionsFraction(writeFraction)
                      .build())
              .build();

      spanner = options.getService();
      client =
          (DatabaseClientImpl)
              spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
      // Wait until the session pool has initialized.
      while (client.pool.getNumberOfSessionsInPool()
          < spanner.getOptions().getSessionPoolOptions().getMinSessions()) {
        Thread.sleep(1L);
      }
    }

    @TearDown(Level.Invocation)
    public void teardown() throws Exception {
      spanner.close();
      mockServer.shutdown();
    }

    int expectedStepsToMax() {
      int remainder = (maxSessions - minSessions) % incStep == 0 ? 0 : 1;
      return numChannels + ((maxSessions - minSessions) / incStep) + remainder;
    }
  }

  /** Measures the time needed to execute a burst of read requests. */
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
                    Thread.sleep(RND.nextInt(HOLD_SESSION_TIME));
                  }
                  return null;
                }
              }));
    }
    Futures.allAsList(futures).get();
    service.shutdown();
  }

  /** Measures the time needed to execute a burst of write requests. */
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
                Thread.sleep(RND.nextInt(RND_WAIT_TIME_BETWEEN_REQUESTS));
                TransactionRunner runner = client.readWriteTransaction();
                return runner.run(
                    transaction ->
                        transaction.executeUpdate(StandardBenchmarkMockServer.UPDATE_STATEMENT));
              }));
    }
    Futures.allAsList(futures).get();
    service.shutdown();
  }

  /** Measures the time needed to execute a burst of read and write requests. */
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
                Thread.sleep(RND.nextInt(RND_WAIT_TIME_BETWEEN_REQUESTS));
                TransactionRunner runner = client.readWriteTransaction();
                return runner.run(
                    transaction ->
                        transaction.executeUpdate(StandardBenchmarkMockServer.UPDATE_STATEMENT));
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
                    Thread.sleep(RND.nextInt(HOLD_SESSION_TIME));
                  }
                  return null;
                }
              }));
    }
    Futures.allAsList(futures).get();
    service.shutdown();
  }

  /** Measures the time needed to acquire MaxSessions session sequentially. */
  @Benchmark
  public void steadyIncrease(BenchmarkState server) {
    final DatabaseClient client =
        server.spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    SessionPool pool = ((DatabaseClientImpl) client).pool;
    assertThat(pool.totalSessions()).isEqualTo(server.minSessions);

    // Checkout maxSessions sessions by starting maxSessions read-only transactions sequentially.
    List<ReadOnlyTransaction> transactions = new ArrayList<>(server.maxSessions);
    for (int i = 0; i < server.maxSessions; i++) {
      ReadOnlyTransaction tx = client.readOnlyTransaction();
      tx.executeQuery(StandardBenchmarkMockServer.SELECT1);
      transactions.add(tx);
    }
    for (ReadOnlyTransaction tx : transactions) {
      tx.close();
    }
  }
}
