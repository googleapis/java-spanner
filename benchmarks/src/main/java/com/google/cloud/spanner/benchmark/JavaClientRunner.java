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

import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.SessionPoolOptionsHelper;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.common.base.Stopwatch;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

class JavaClientRunner extends AbstractRunner {
  private final DatabaseId databaseId;
  private long numNullValues;
  private long numNonNullValues;

  JavaClientRunner(DatabaseId databaseId) {
    this.databaseId = databaseId;
  }

  @Override
  public List<Duration> execute(
      TransactionType transactionType,
      int numClients,
      int numOperations,
      int waitMillis,
      boolean useMultiplexedSession) {
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
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setOpenTelemetry(openTelemetry)
            .setProjectId(databaseId.getInstanceId().getProject())
            .setSessionPoolOption(sessionPoolOptions)
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

      List<Future<List<Duration>>> results = new ArrayList<>(numClients);
      ExecutorService service = Executors.newFixedThreadPool(numClients);
      for (int client = 0; client < numClients; client++) {
        results.add(
            service.submit(
                () ->
                    runBenchmark(
                        databaseClient,
                        transactionType,
                        numOperations,
                        waitMillis,
                        endToEndLatencies)));
      }
      return collectResults(service, results, numClients, numOperations);
    } catch (Throwable t) {
      throw SpannerExceptionFactory.asSpannerException(t);
    }
  }

  private List<Duration> runBenchmark(
      DatabaseClient databaseClient,
      TransactionType transactionType,
      int numOperations,
      int waitMillis,
      DoubleHistogram endToEndLatencies) {
    List<Duration> results = new ArrayList<>(numOperations);
    // Execute one query to make sure everything has been warmed up.
    executeTransaction(databaseClient, transactionType, endToEndLatencies, false);

    for (int i = 0; i < numOperations; i++) {
      try {
        randomWait(waitMillis);
        results.add(executeTransaction(databaseClient, transactionType, endToEndLatencies, true));
        incOperations();
      } catch (InterruptedException interruptedException) {
        throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
      }
    }
    return results;
  }

  private Duration executeTransaction(
      DatabaseClient client,
      TransactionType transactionType,
      DoubleHistogram endToEndLatencies,
      boolean recordLatency) {
    Stopwatch watch = Stopwatch.createStarted();
    switch (transactionType) {
      case READ_ONLY_SINGLE_USE:
        executeSingleUseReadOnlyTransaction(client);
        break;
      case READ_ONLY_MULTI_USE:
        executeMultiUseReadOnlyTransaction(client);
        break;
      case READ_WRITE:
        executeReadWriteTransaction(client);
        break;
    }
    Duration elapsedTime = watch.elapsed();
    if (recordLatency) {
      endToEndLatencies.record(elapsedTime.toMillis());
    }
    return elapsedTime;
  }

  private void executeSingleUseReadOnlyTransaction(DatabaseClient client) {
    try (ResultSet resultSet = client.singleUse().executeQuery(getRandomisedReadStatement())) {
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

  static Statement getRandomisedReadStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(TOTAL_RECORDS);
    return Statement.newBuilder(SELECT_QUERY).bind(ID_COLUMN_NAME).to(randomKey).build();
  }

  static Statement getRandomisedUpdateStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(TOTAL_RECORDS);
    return Statement.newBuilder(UPDATE_QUERY).bind(ID_COLUMN_NAME).to(randomKey).build();
  }
}
