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
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.DeleteSessionRequest;
import java.time.Duration;
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
 * Benchmarks for the SessionPoolMaintainer. Run these benchmarks from the command line like this:
 * <code>
 * mvn clean test -DskipTests -Pbenchmark -Dbenchmark.name=SessionPoolMaintainerBenchmark
 * </code>
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Measurement(batchSize = 1, iterations = 1, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(batchSize = 0, iterations = 0)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SessionPoolMaintainerBenchmark {
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private static final int HOLD_SESSION_TIME = 10;
  private static final int RND_WAIT_TIME_BETWEEN_REQUESTS = 100;
  private static final Random RND = new Random();

  @State(Scope.Thread)
  @AuxCounters(org.openjdk.jmh.annotations.AuxCounters.Type.EVENTS)
  public static class MockServer {
    private StandardBenchmarkMockServer mockServer;
    private Spanner spanner;
    private DatabaseClientImpl client;

    /**
     * The tests set the session idle timeout to an extremely low value to force timeouts and
     * sessions to be evicted from the pool. This is not intended to replicate a realistic scenario,
     * only to detect whether certain changes to the client library might cause the number of RPCs
     * or the execution time to change drastically.
     */
    @Param({"100"})
    long idleTimeout;

    /** AuxCounter for number of create RPCs. */
    public int numBatchCreateSessionsRpcs() {
      return mockServer.countRequests(BatchCreateSessionsRequest.class);
    }

    /** AuxCounter for number of delete RPCs. */
    public int numDeleteSessionRpcs() {
      return mockServer.countRequests(DeleteSessionRequest.class);
    }

    /** AuxCounter for number of begin tx RPCs. */
    public int numBeginTransactionRpcs() {
      return mockServer.countRequests(BeginTransactionRequest.class);
    }

    @Setup(Level.Invocation)
    public void setup() throws Exception {
      mockServer = new StandardBenchmarkMockServer();
      TransportChannelProvider channelProvider = mockServer.start();

      SpannerOptions options =
          SpannerOptions.newBuilder()
              .setProjectId(TEST_PROJECT)
              .setChannelProvider(channelProvider)
              .setCredentials(NoCredentials.getInstance())
              .setSessionPoolOption(
                  SessionPoolOptions.newBuilder()
                      // Set idle timeout and loop frequency to very low values.
                      .setRemoveInactiveSessionAfterDuration(Duration.ofMillis(idleTimeout))
                      .setLoopFrequency(idleTimeout / 10)
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
  }

  /** Measures the time and RPCs needed to execute read requests. */
  @Benchmark
  public void read(final MockServer server) throws Exception {
    int min = server.spanner.getOptions().getSessionPoolOptions().getMinSessions();
    int max = server.spanner.getOptions().getSessionPoolOptions().getMaxSessions();
    int totalQueries = max * 4;
    int parallelThreads = min;
    final DatabaseClient client =
        server.spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    SessionPool pool = ((DatabaseClientImpl) client).pool;
    assertThat(pool.totalSessions()).isEqualTo(min);

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

  /** Measures the time and RPCs needed to execute write requests. */
  @Benchmark
  public void write(final MockServer server) throws Exception {
    int min = server.spanner.getOptions().getSessionPoolOptions().getMinSessions();
    int max = server.spanner.getOptions().getSessionPoolOptions().getMaxSessions();
    int totalWrites = max * 4;
    int parallelThreads = max;
    final DatabaseClient client =
        server.spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    SessionPool pool = ((DatabaseClientImpl) client).pool;
    assertThat(pool.totalSessions()).isEqualTo(min);

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

  /** Measures the time and RPCs needed to execute read and write requests. */
  @Benchmark
  public void readAndWrite(final MockServer server) throws Exception {
    int min = server.spanner.getOptions().getSessionPoolOptions().getMinSessions();
    int max = server.spanner.getOptions().getSessionPoolOptions().getMaxSessions();
    int totalWrites = max * 2;
    int totalReads = max * 2;
    int parallelThreads = max;
    final DatabaseClient client =
        server.spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    SessionPool pool = ((DatabaseClientImpl) client).pool;
    assertThat(pool.totalSessions()).isEqualTo(min);

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
}
