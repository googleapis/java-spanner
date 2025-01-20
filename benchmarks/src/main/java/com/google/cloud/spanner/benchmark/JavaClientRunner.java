/*
 * Copyright 2024 Google LLC
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

package com.google.cloud.spanner.benchmark;

import com.google.api.gax.grpc.GrpcCallContext;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.SessionPoolOptionsHelper;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.SpannerOptions.CallContextConfigurator;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.spi.v1.SpannerInterceptorProvider;
import com.google.common.base.Stopwatch;
import io.grpc.CallOptions;
import io.grpc.CallOptions.Key;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

class JavaClientRunner extends AbstractRunner {
  private final DatabaseId databaseId;
  private final ConcurrentHashMap<String, Long> concurrentHashMap;
  private final Key<String> trackingKey = Key.create("tracking-uuid");
  private static final Metadata.Key<String> SERVER_TIMING_HEADER_KEY =
      Metadata.Key.of("server-timing", Metadata.ASCII_STRING_MARSHALLER);
  private static final String SERVER_TIMING_HEADER_PREFIX = "gfet4t7; dur=";
  private long numNullValues;
  private long numNonNullValues;

  JavaClientRunner(DatabaseId databaseId) {
    this.databaseId = databaseId;
    this.concurrentHashMap = new ConcurrentHashMap<>();
  }

  @Override
  public void execute(
      TransactionType transactionType,
      int numClients,
      int numOperations,
      int waitMillis,
      boolean useMultiplexedSession,
      int warmUpMinutes,
      int staleReadMinutes) {
    // setup open telemetry metrics and traces
    // setup open telemetry metrics and traces
    SpanExporter traceExporter = TraceExporter.createWithDefaultConfiguration();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
            .setResource(
                Resource.create(
                    Attributes.of(
                        AttributeKey.stringKey("service.name"),
                        "Java-MultiplexedSession-Benchmark")))
            .setSampler(Sampler.alwaysOn())
            .build();
    MetricExporter cloudMonitoringExporter =
        GoogleCloudMetricExporter.createWithDefaultConfiguration();
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder()
            .registerMetricReader(PeriodicMetricReader.create(cloudMonitoringExporter))
            .build();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .setTracerProvider(tracerProvider)
            .build();
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptionsHelper.setUseMultiplexedSession(
                SessionPoolOptions.newBuilder(), useMultiplexedSession)
            .build();
    SpannerOptions.enableOpenTelemetryMetrics();
    SpannerOptions.enableOpenTelemetryTraces();

    ClientInterceptor clientInterceptor =
        new ClientInterceptor() {
          @Override
          public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
              MethodDescriptor<ReqT, RespT> methodDescriptor,
              CallOptions callOptions,
              Channel channel) {
            return new SimpleForwardingClientCall<ReqT, RespT>(
                channel.newCall(methodDescriptor, callOptions)) {
              @Override
              public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(
                    new SimpleForwardingClientCallListener<RespT>(responseListener) {
                      @Override
                      public void onHeaders(Metadata headers) {
                        if ("google.spanner.v1.Spanner/ExecuteStreamingSql"
                                .equalsIgnoreCase(methodDescriptor.getFullMethodName())
                            || "google.spanner.v1.Spanner/StreamingRead"
                                .equalsIgnoreCase(methodDescriptor.getFullMethodName())) {
                          String serverTiming = headers.get(SERVER_TIMING_HEADER_KEY);
                          if (serverTiming != null
                              && serverTiming.startsWith(SERVER_TIMING_HEADER_PREFIX)) {
                            long latency =
                                Long.parseLong(
                                    serverTiming.substring(SERVER_TIMING_HEADER_PREFIX.length()));
                            String trackingUuid = callOptions.getOption(trackingKey);
                            Optional.ofNullable(trackingUuid)
                                .ifPresent(id -> concurrentHashMap.put(trackingUuid, latency));
                          }
                        }
                        //                        concurrentHashMap.put(uuid, "uuid");
                        super.onHeaders(headers);
                      }
                    },
                    headers);
              }
            };
          }
        };

    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setOpenTelemetry(openTelemetry)
            .setProjectId(databaseId.getInstanceId().getProject())
            .setSessionPoolOption(sessionPoolOptions)
            .setInterceptorProvider(
                SpannerInterceptorProvider.createDefault(openTelemetry).with(clientInterceptor))
            .setHost(SERVER_URL)
            .build();
    // Register query stats metric.
    // This should be done once before start recording the data.
    Meter meter = openTelemetry.getMeter("cloud.google.com/java");
    DoubleHistogram endToEndLatencies =
        meter
            .histogramBuilder("spanner/end_end_elapsed")
            .setDescription("The execution of end to end latency")
            .setUnit("ms")
            .build();
    try (Spanner spanner = options.getService()) {
      DatabaseClient databaseClient = spanner.getDatabaseClient(databaseId);

      executeBenchmarkAndPrintResults(
          numClients,
          databaseClient,
          transactionType,
          numOperations,
          waitMillis,
          warmUpMinutes,
          staleReadMinutes,
          endToEndLatencies,
          true);
      executeBenchmarkAndPrintResults(
          numClients,
          databaseClient,
          transactionType,
          numOperations,
          waitMillis,
          warmUpMinutes,
          staleReadMinutes,
          endToEndLatencies,
          false);
    } catch (Throwable t) {
      throw SpannerExceptionFactory.asSpannerException(t);
    }

    sdkMeterProvider.close();
  }

  private void executeBenchmarkAndPrintResults(
      int numClients,
      DatabaseClient databaseClient,
      TransactionType transactionType,
      int numOperations,
      int waitMillis,
      int warmUpMinutes,
      int staleReadMinutes,
      DoubleHistogram endToEndLatencies,
      boolean skipTrailers)
      throws Exception {
    List<Future<List<Duration>>> results = new ArrayList<>(numClients);
    ExecutorService service = Executors.newFixedThreadPool(numClients);
    resetOperations();
    for (int client = 0; client < numClients; client++) {
      results.add(
          service.submit(
              () ->
                  runBenchmark(
                      databaseClient,
                      transactionType,
                      numOperations,
                      waitMillis,
                      warmUpMinutes,
                      staleReadMinutes,
                      endToEndLatencies,
                      skipTrailers)));
    }
    LatencyBenchmark.printResults(
        "Performance Results", collectResults(service, results, numClients, numOperations));
  }

  private List<Duration> runBenchmark(
      DatabaseClient databaseClient,
      TransactionType transactionType,
      int numOperations,
      int waitMillis,
      int warmUpMinutes,
      int staleReadMinutes,
      DoubleHistogram endToEndLatencies,
      boolean skipTrailers) {
    List<Duration> results = new ArrayList<>(numOperations);
    // Execute one query to make sure everything has been warmed up.
    Instant endTime = Instant.now().plus(warmUpMinutes, ChronoUnit.MINUTES);
    while (Instant.now().isBefore(endTime)) {
      executeTransaction(
          databaseClient,
          skipTrailers,
          staleReadMinutes,
          transactionType,
          endToEndLatencies,
          false);
    }

    for (int i = 0; i < numOperations; i++) {
      try {
        randomWait(waitMillis);
        results.add(
            executeTransaction(
                databaseClient,
                skipTrailers,
                staleReadMinutes,
                transactionType,
                endToEndLatencies,
                true));
        incOperations();
      } catch (InterruptedException interruptedException) {
        throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
      }
    }
    return results;
  }

  private Duration executeTransaction(
      DatabaseClient client,
      boolean skipTrailers,
      int staleReadMinutes,
      TransactionType transactionType,
      DoubleHistogram endToEndLatencies,
      boolean recordLatency) {
    String uuid = UUID.randomUUID().toString();
    CallContextConfigurator configurator =
        new CallContextConfigurator() {
          @Override
          public <ReqT, RespT> ApiCallContext configure(
              ApiCallContext context, ReqT request, MethodDescriptor<ReqT, RespT> method) {
            GrpcCallContext grpcCallContext = (GrpcCallContext) context;
            return grpcCallContext.withCallOptions(
                grpcCallContext.getCallOptions().withOption(trackingKey, uuid));
          }
        };
    Context context =
        Context.current().withValue(SpannerOptions.CALL_CONTEXT_CONFIGURATOR_KEY, configurator);
    Stopwatch watch = Stopwatch.createStarted();
    context.run(
        () -> {
          switch (transactionType) {
            case READ_ONLY_STALE_READ:
              executeSingleUseReadOnlyStaleReadTransaction(client, staleReadMinutes, skipTrailers);
            case READ_ONLY_SINGLE_USE:
              executeSingleUseReadOnlyTransaction(client, skipTrailers);
              break;
            case READ_ONLY_MULTI_USE:
              executeMultiUseReadOnlyTransaction(client);
              break;
            case READ_WRITE:
              executeReadWriteTransaction(client);
              break;
          }
        });
    Duration elapsedTime = watch.elapsed();
    long gfeLatency = concurrentHashMap.remove(uuid);
    if (recordLatency) {
      endToEndLatencies.record(elapsedTime.toMillis() - gfeLatency);
    }
    return elapsedTime.minus(gfeLatency, ChronoUnit.MILLIS);
  }

  private void executeSingleUseReadOnlyStaleReadTransaction(
      DatabaseClient client, int staleReadMinutes, boolean skipTrailers) {
    List<String> columns = new ArrayList<>();
    columns.add(ID_COLUMN_NAME);
    try (ResultSet resultSet =
        client
            .singleUse(TimestampBound.ofExactStaleness(staleReadMinutes, TimeUnit.MINUTES))
            .read(
                TABLE_NAME,
                KeySet.singleKey(com.google.cloud.spanner.Key.of(getRandomReadKey())),
                columns,
                Options.skippingTrailerOption(skipTrailers))) {
      while (resultSet.next()) {
        for (int i = 0; i < resultSet.getColumnCount(); i++) {
          if (resultSet.isNull(i)) {
            numNullValues++;
          } else {
            numNonNullValues++;
          }
        }
      }
    }
  }

  private void executeSingleUseReadOnlyTransaction(DatabaseClient client, boolean skipTrailers) {
    try (ResultSet resultSet =
        client
            .singleUse()
            .executeQuery(
                getRandomisedReadStatement(),
                Options.tag("uuid"),
                Options.skippingTrailerOption(skipTrailers))) {
      while (resultSet.next()) {
        for (int i = 0; i < resultSet.getColumnCount(); i++) {
          if (resultSet.isNull(i)) {
            numNullValues++;
          } else {
            numNonNullValues++;
          }
        }
      }
    }
  }

  private void executeMultiUseReadOnlyTransaction(DatabaseClient client) {
    try (ReadOnlyTransaction transaction = client.readOnlyTransaction()) {
      ResultSet resultSet = transaction.executeQuery(getRandomisedReadStatement());
      iterateResultSet(resultSet);

      ResultSet resultSet1 = transaction.executeQuery(getRandomisedReadStatement());
      iterateResultSet(resultSet1);

      ResultSet resultSet2 = transaction.executeQuery(getRandomisedReadStatement());
      iterateResultSet(resultSet2);

      ResultSet resultSet3 = transaction.executeQuery(getRandomisedReadStatement());
      iterateResultSet(resultSet3);
    }
  }

  private void iterateResultSet(ResultSet resultSet) {
    while (resultSet.next()) {
      for (int i = 0; i < resultSet.getColumnCount(); i++) {
        if (resultSet.isNull(i)) {
          numNullValues++;
        } else {
          numNonNullValues++;
        }
      }
    }
  }

  private void executeReadWriteTransaction(DatabaseClient client) {
    client
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(getRandomisedUpdateStatement()));
  }

  static int getRandomReadKey() {
    return ThreadLocalRandom.current().nextInt(READ_RANGE);
  }

  static Statement getRandomisedReadStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(TOTAL_RECORDS);
    return Statement.newBuilder(SELECT_QUERY).bind(ID_COLUMN_NAME).to(randomKey).build();
  }

  static Statement getRandomisedUpdateStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(TOTAL_RECORDS);
    return Statement.newBuilder(UPDATE_QUERY).bind(ID_COLUMN_NAME).to(randomKey).build();
  }
}
