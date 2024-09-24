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

package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.tracing.ApiTracerFactory;
import com.google.api.gax.tracing.MetricsTracerFactory;
import com.google.api.gax.tracing.OpenTelemetryMetricsRecorder;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.grpc.Status;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class OpenTelemetryBuiltInMetricsTracerTest extends AbstractMockServerTest {

  private static final Statement SELECT_RANDOM = Statement.of("SELECT * FROM random");

  private static final Statement UPDATE_RANDOM = Statement.of("UPDATE random SET foo=1 WHERE id=1");
  private static InMemoryMetricReader metricReader;

  private static OpenTelemetry openTelemetry;

  private static Map<String, String> attributes;

  private static Attributes expectedBaseAttributes;

  private static final long MIN_LATENCY = 0;

  private DatabaseClient client;

  @BeforeClass
  public static void setup() {
    metricReader = InMemoryMetricReader.create();

    BuiltInOpenTelemetryMetricsProvider provider = BuiltInOpenTelemetryMetricsProvider.INSTANCE;

    SdkMeterProviderBuilder meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReader);

    BuiltInMetricsConstant.getAllViews().forEach(meterProvider::registerView);

    String client_name = "spanner-java/";
    openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(meterProvider.build()).build();
    attributes = provider.createClientAttributes("test-project", client_name);

    expectedBaseAttributes =
        Attributes.builder()
            .put(BuiltInMetricsConstant.PROJECT_ID_KEY, "test-project")
            .put(BuiltInMetricsConstant.INSTANCE_CONFIG_ID_KEY, "unknown")
            .put(
                BuiltInMetricsConstant.LOCATION_ID_KEY,
                BuiltInOpenTelemetryMetricsProvider.detectClientLocation())
            .put(BuiltInMetricsConstant.CLIENT_NAME_KEY, client_name)
            .put(BuiltInMetricsConstant.CLIENT_UID_KEY, attributes.get("client_uid"))
            .put(BuiltInMetricsConstant.CLIENT_HASH_KEY, "cloud_spanner_client_raw_metrics")
            .build();
  }

  @BeforeClass
  public static void setupResults() {
    RandomResultSetGenerator generator = new RandomResultSetGenerator(1);
    mockSpanner.putStatementResult(StatementResult.query(SELECT_RANDOM, generator.generate()));
    mockSpanner.putStatementResults(StatementResult.update(UPDATE_RANDOM, 1L));
  }

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
  }

  @Override
  public void createSpannerInstance() {
    SpannerOptions.Builder builder = SpannerOptions.newBuilder();

    ApiTracerFactory metricsTracerFactory =
        new MetricsTracerFactory(
            new OpenTelemetryMetricsRecorder(openTelemetry, BuiltInMetricsConstant.METER_NAME),
            attributes);
    // Set a quick polling algorithm to prevent this from slowing down the test unnecessarily.
    builder
        .getDatabaseAdminStubSettingsBuilder()
        .updateDatabaseDdlOperationSettings()
        .setPollingAlgorithm(
            OperationTimedPollAlgorithm.create(
                RetrySettings.newBuilder()
                    .setInitialRetryDelay(Duration.ofNanos(1L))
                    .setMaxRetryDelay(Duration.ofNanos(1L))
                    .setRetryDelayMultiplier(1.0)
                    .setTotalTimeout(Duration.ofMinutes(10L))
                    .build()));
    spanner =
        builder
            .setProjectId("test-project")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setWaitForMinSessions(Duration.ofSeconds(5L))
                    .setFailOnSessionLeak()
                    .build())
            // Setting this to false so that Spanner Options does not register Metrics Tracer
            // factory again.
            .setEnableBuiltInMetrics(false)
            .setApiTracerFactory(metricsTracerFactory)
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of("test-project", "i", "d"));
  }

  @Test
  public void testMetricsSingleUseQuery() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try (ResultSet resultSet = client.singleUse().executeQuery(SELECT_RANDOM)) {
      assertTrue(resultSet.next());
      assertFalse(resultSet.next());
    }

    long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    Attributes expectedAttributes =
        expectedBaseAttributes
            .toBuilder()
            .put(BuiltInMetricsConstant.STATUS_KEY, "OK")
            .put(BuiltInMetricsConstant.METHOD_KEY, "Spanner.ExecuteStreamingSql")
            .build();

    MetricData operationLatencyMetricData =
        getMetricData(metricReader, BuiltInMetricsConstant.OPERATION_LATENCIES_NAME);
    long operationLatencyValue = getAggregatedValue(operationLatencyMetricData, expectedAttributes);
    assertThat(operationLatencyValue).isIn(Range.closed(MIN_LATENCY, elapsed));

    MetricData attemptLatencyMetricData =
        getMetricData(metricReader, BuiltInMetricsConstant.ATTEMPT_LATENCIES_NAME);
    long attemptLatencyValue = getAggregatedValue(attemptLatencyMetricData, expectedAttributes);
    assertThat(attemptLatencyValue).isIn(Range.closed(MIN_LATENCY, elapsed));

    MetricData operationCountMetricData =
        getMetricData(metricReader, BuiltInMetricsConstant.OPERATION_COUNT_NAME);
    assertThat(getAggregatedValue(operationCountMetricData, expectedAttributes)).isEqualTo(1);

    MetricData attemptCountMetricData =
        getMetricData(metricReader, BuiltInMetricsConstant.ATTEMPT_COUNT_NAME);
    assertThat(getAggregatedValue(attemptCountMetricData, expectedAttributes)).isEqualTo(1);
  }

  @Test
  public void testMetricsWithGaxRetryUnaryRpc() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofException(Status.UNAVAILABLE.asRuntimeException()));

    // Execute a simple read/write transaction using only mutations. This will use the
    // BeginTransaction RPC to start the transaction. That RPC will first return UNAVAILABLE, then
    // be retried by Gax, and succeed. The retry should show up in the tracing.
    client.write(ImmutableList.of(Mutation.newInsertBuilder("foo").set("bar").to(1L).build()));

    stopwatch.elapsed(TimeUnit.MILLISECONDS);

    Attributes expectedAttributesBeginTransactionOK =
        expectedBaseAttributes
            .toBuilder()
            .put(BuiltInMetricsConstant.STATUS_KEY, "OK")
            .put(BuiltInMetricsConstant.METHOD_KEY, "Spanner.BeginTransaction")
            .build();

    Attributes expectedAttributesBeginTransactionFailed =
        expectedBaseAttributes
            .toBuilder()
            .put(BuiltInMetricsConstant.STATUS_KEY, "UNAVAILABLE")
            .put(BuiltInMetricsConstant.METHOD_KEY, "Spanner.BeginTransaction")
            .build();

    MetricData attemptCountMetricData =
        getMetricData(metricReader, BuiltInMetricsConstant.ATTEMPT_COUNT_NAME);
    assertThat(getAggregatedValue(attemptCountMetricData, expectedAttributesBeginTransactionOK))
        .isEqualTo(1);
    // Attempt count should have a failed metric point for Begin Transaction.
    assertThat(getAggregatedValue(attemptCountMetricData, expectedAttributesBeginTransactionFailed))
        .isEqualTo(1);

    MetricData operationCountMetricData =
        getMetricData(metricReader, BuiltInMetricsConstant.OPERATION_COUNT_NAME);
    assertThat(getAggregatedValue(operationCountMetricData, expectedAttributesBeginTransactionOK))
        .isEqualTo(1);
    // Operation count should not have a failed metric point for Begin Transaction as overall
    // operation is success..
    assertThat(
            getAggregatedValue(operationCountMetricData, expectedAttributesBeginTransactionFailed))
        .isEqualTo(0);
  }

  private MetricData getMetricData(InMemoryMetricReader reader, String metricName) {
    String fullMetricName = BuiltInMetricsConstant.METER_NAME + "/" + metricName;
    Collection<MetricData> allMetricData = Collections.emptyList();

    // Fetch the MetricData with retries
    for (int attemptsLeft = 1000; attemptsLeft > 0; attemptsLeft--) {
      allMetricData = reader.collectAllMetrics();
      List<MetricData> matchingMetadata =
          allMetricData.stream()
              .filter(md -> md.getName().equals(fullMetricName))
              .collect(Collectors.toList());
      assertWithMessage(
              "Found multiple MetricData with the same name: %s, in: %s",
              fullMetricName, matchingMetadata)
          .that(matchingMetadata.size())
          .isAtMost(1);

      if (!matchingMetadata.isEmpty()) {
        return matchingMetadata.get(0);
      }

      try {
        Thread.sleep(1);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(interruptedException);
      }
    }

    assertTrue(String.format("MetricData is missing for metric {0}", fullMetricName), false);
    return null;
  }

  private long getAggregatedValue(MetricData metricData, Attributes attributes) {
    switch (metricData.getType()) {
      case HISTOGRAM:
        Optional<HistogramPointData> hd =
            metricData.getHistogramData().getPoints().stream()
                .filter(pd -> pd.getAttributes().equals(attributes))
                .collect(Collectors.toList())
                .stream()
                .findFirst();
        return hd.isPresent() ? (long) hd.get().getSum() / hd.get().getCount() : 0;
      case LONG_SUM:
        Optional<LongPointData> ld =
            metricData.getLongSumData().getPoints().stream()
                .filter(pd -> pd.getAttributes().equals(attributes))
                .collect(Collectors.toList())
                .stream()
                .findFirst();
        return ld.isPresent() ? ld.get().getValue() : 0;
      default:
        return 0;
    }
  }
}
