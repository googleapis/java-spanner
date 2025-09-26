/*
 * Copyright 2025 Google LLC
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

package com.google.cloud.spanner.benchmarking;

import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.rpc.TransportChannel;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.WarmupMode;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(5)
@Fork(1)
public class ReadBenchmark {

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    Spanner spanner;
    DatabaseClient databaseClient;
    MockSpannerServiceImpl mockSpanner;
    Server gRPCServer;
    ExecutorService clientExecutors;
    ExecutorService serverExecutors;

    @Setup(Level.Trial)
    public void setup() throws IOException {
      mockSpanner = new MockSpannerServiceImpl();
      mockSpanner.setAbortProbability(0.0D);

      serverExecutors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
      clientExecutors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

      String serverName = InProcessServerBuilder.generateName();
      gRPCServer = InProcessServerBuilder.forName(serverName)
          .addService(mockSpanner)
          .executor(serverExecutors)
          .build()
          .start();

      registerAllMocks(mockSpanner);

      ManagedChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName).usePlaintext()
          .executor(clientExecutors);
      GrpcTransportChannel grpcTransportChannel = GrpcTransportChannel.newBuilder()
          .setManagedChannel(channelBuilder.build())
          .build();

      spanner = SpannerOptions.newBuilder()
          .setProjectId("span-cloud-testing")
          .setChannelProvider(new TransportChannelProvider() {
            @Override
            public boolean shouldAutoClose() {
              return false;
            }

            @Override
            public boolean needsExecutor() {
              return false;
            }

            @Override
            public TransportChannelProvider withExecutor(Executor executor) {
              return null;
            }

            @Override
            public TransportChannelProvider withExecutor(
                ScheduledExecutorService scheduledExecutorService) {
              return null;
            }

            @Override
            public boolean needsHeaders() {
              return false;
            }

            @Override
            public TransportChannelProvider withHeaders(Map<String, String> map) {
              return null;
            }

            @Override
            public boolean needsEndpoint() {
              return false;
            }

            @Override
            public TransportChannelProvider withEndpoint(String s) {
              return null;
            }

            @Override
            public boolean acceptsPoolSize() {
              return false;
            }

            @Override
            public TransportChannelProvider withPoolSize(int i) {
              return null;
            }

            @Override
            public boolean needsCredentials() {
              return false;
            }

            @Override
            public TransportChannelProvider withCredentials(Credentials credentials) {
              return null;
            }

            @Override
            public TransportChannel getTransportChannel() throws IOException {
              return grpcTransportChannel;
            }

            @Override
            public String getTransportName() {
              return "";
            }
          })
          .build().getService();
      databaseClient = spanner.getDatabaseClient(
          DatabaseId.of("span-cloud-testing", "sakthi-spanner-testing", "benchmarking"));
    }

    private void registerAllMocks(MockSpannerServiceImpl mockSpanner) {
      List<String> columns = new ArrayList<>();
      columns.add("id");
      columns.add("name");
      ResultSetMetadata SELECT1_METADATA =
          ResultSetMetadata.newBuilder()
              .setRowType(
                  StructType.newBuilder()
                      .addFields(
                          Field.newBuilder()
                              .setName("id")
                              .setType(
                                  com.google.spanner.v1.Type.newBuilder()
                                      .setCode(TypeCode.INT64)
                                      .build())
                              .build())
                      .addFields(Field.newBuilder()
                          .setName("name")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.INT64)
                                  .build())
                          .build())
                      .build())
              .build();
      com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
          com.google.spanner.v1.ResultSet.newBuilder()
              .addRows(
                  ListValue.newBuilder()
                      .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                      .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1000").build())
                      .build())
              .setMetadata(SELECT1_METADATA)
              .build();
      mockSpanner.putStatementResult(StatementResult.read("Employees", KeySet.singleKey(Key.of()), columns, SELECT1_RESULTSET));
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      spanner.close();
      gRPCServer.shutdown();
      clientExecutors.shutdown();
      serverExecutors.shutdown();
    }
  }

  @Benchmark
  @Warmup(time = 1, timeUnit = TimeUnit.MINUTES, iterations = 1)
  @Measurement(time = 2, timeUnit = TimeUnit.MINUTES, iterations = 1)
  public long staleReadBenchmark(BenchmarkState benchmarkState, Blackhole blackhole) {
    List<String> columns = new ArrayList<>();
    columns.add("id");
    columns.add("name");
    long id = -1;
    try (ReadContext readContext = benchmarkState.databaseClient.singleUse()) {
      try (ResultSet resultSet = readContext.read("Employees",
          KeySet.singleKey(Key.of("2")), columns)) {
        while (resultSet.next()) {
          blackhole.consume(resultSet.getLong("id"));
        }
      }
    }
    return id;
  }

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(ReadBenchmark.class.getSimpleName())
        .result("my_benchmark_results.json")
        .resultFormat(ResultFormatType.JSON)
        .build();
    new Runner(opt).run();
  }
}
