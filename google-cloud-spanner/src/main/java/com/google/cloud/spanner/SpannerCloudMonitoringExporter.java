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

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.core.InternalApi;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.auth.Credentials;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.Empty;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;
import static com.google.cloud.spanner.SpannerMetricsConstant.SPANNER_METRICS;

/**
 * Spanner Cloud Monitoring OpenTelemetry Exporter.
 *
 * <p>The exporter will look for all spanner owned metrics under spanner.googleapis.com
 * instrumentation scope and upload it via the Google Cloud Monitoring API.
 */
@InternalApi
public final class SpannerCloudMonitoringExporter implements MetricExporter {

  private static final Logger logger =
      Logger.getLogger(SpannerCloudMonitoringExporter.class.getName());

  // This system property can be used to override the monitoring endpoint
  // to a different environment. It's meant for internal testing only.
  private static final String  MONITORING_ENDPOINT =
      MoreObjects.firstNonNull(
          System.getProperty("test"),
          MetricServiceSettings.getDefaultEndpoint());

  // This the quota limit from Cloud Monitoring. More details in
  // https://cloud.google.com/monitoring/quotas#custom_metrics_quotas.
  private static final int EXPORT_BATCH_SIZE_LIMIT = 200;
  private final AtomicBoolean spannerExportFailureLogged = new AtomicBoolean(false);
  private CompletableResultCode lastExportCode;

  private final MetricServiceClient client;
  private final String spannerProjectId;
  private final String taskId;
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  public static SpannerCloudMonitoringExporter create(
      String projectId, @Nullable Credentials credentials) throws IOException {
    MetricServiceSettings.Builder settingsBuilder = MetricServiceSettings.newBuilder();
    CredentialsProvider credentialsProvider;
    if (credentials == null) {
      credentialsProvider = NoCredentialsProvider.create();
    } else {
      credentialsProvider = FixedCredentialsProvider.create(credentials);
    }
    settingsBuilder.setCredentialsProvider(credentialsProvider);
    settingsBuilder.setEndpoint(MONITORING_ENDPOINT);

    org.threeten.bp.Duration timeout = Duration.ofMinutes(1);
    // TODO: createServiceTimeSeries needs special handling if the request failed. Leaving
    // it as not retried for now.
    settingsBuilder.createServiceTimeSeriesSettings().setSimpleTimeoutNoRetries(timeout);

    return new SpannerCloudMonitoringExporter(
        projectId,
        MetricServiceClient.create(settingsBuilder.build()),
        SpannerCloudMonitoringExporterUtils.getDefaultTaskValue());
  }

  @VisibleForTesting
  SpannerCloudMonitoringExporter(
      String projectId,
      MetricServiceClient client,
      String taskId) {
    this.client = client;
    this.taskId = taskId;
    this.spannerProjectId = projectId;
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> collection) {
    if (isShutdown.get()) {
      logger.log(Level.WARNING, "Exporter is shutting down");
      return CompletableResultCode.ofFailure();
    }

    lastExportCode = exportSpannerResourceMetrics(collection);
    return lastExportCode;
  }

  /** Export metrics associated with a Spanner resource. */
  private CompletableResultCode exportSpannerResourceMetrics(Collection<MetricData> collection) {
    // Filter spanner metrics
    List<MetricData> spannerMetricData =
        collection.stream()
            .filter(md -> SPANNER_METRICS.contains(md.getName()))
            .collect(Collectors.toList());

    // Skips exporting if there's none
    if (spannerMetricData.isEmpty()) {
      return CompletableResultCode.ofSuccess();
    }

    // Verifies metrics project id are the same as the spanner project id set on this client
    if (!spannerMetricData.stream()
        .flatMap(metricData -> metricData.getData().getPoints().stream())
        .allMatch(pd -> spannerProjectId.equals(SpannerCloudMonitoringExporterUtils.getProjectId(pd)))) {
      logger.log(Level.WARNING, "Metric data has different a projectId. Skip exporting.");
      return CompletableResultCode.ofFailure();
    }

    List<TimeSeries> spannerTimeSeries;
    try {
      spannerTimeSeries =
          SpannerCloudMonitoringExporterUtils.convertToSpannerTimeSeries(spannerMetricData, taskId);
    } catch (Throwable e) {
      logger.log(
          Level.WARNING,
          "Failed to convert spanner table metric data to cloud monitoring timeseries.",
          e);
      return CompletableResultCode.ofFailure();
    }

    ProjectName projectName = ProjectName.of(spannerProjectId);

    ApiFuture<List<Empty>> futureList = exportTimeSeriesInBatch(projectName, spannerTimeSeries);

    CompletableResultCode spannerExportCode = new CompletableResultCode();
    ApiFutures.addCallback(
        futureList,
        new ApiFutureCallback<List<Empty>>() {
          @Override
          public void onFailure(Throwable throwable) {
            if (spannerExportFailureLogged.compareAndSet(false, true)) {
              String msg = "createServiceTimeSeries request failed for spanner metrics.";
              if (throwable instanceof PermissionDeniedException) {
                msg +=
                    String.format(
                        " Need monitoring metric writer permission on project=%s. Follow TODO to set up permissions.",
                        projectName.getProject());
              }
              logger.log(Level.WARNING, msg, throwable);
            }
            spannerExportCode.fail();
          }

          @Override
          public void onSuccess(List<Empty> empty) {
            spannerExportFailureLogged.set(false);
            spannerExportCode.succeed();
          }
        },
        MoreExecutors.directExecutor());

    return spannerExportCode;
  }

  private ApiFuture<List<Empty>> exportTimeSeriesInBatch(
      ProjectName projectName, List<TimeSeries> timeSeries) {
    List<ApiFuture<Empty>> batchResults = new ArrayList<>();

    for (List<TimeSeries> batch : Iterables.partition(timeSeries, EXPORT_BATCH_SIZE_LIMIT)) {
      CreateTimeSeriesRequest req =
          CreateTimeSeriesRequest.newBuilder()
              .setName(projectName.toString())
              .addAllTimeSeries(batch)
              .build();
      ApiFuture<Empty> f = this.client.createServiceTimeSeriesCallable().futureCall(req);
      batchResults.add(f);
    }

    return ApiFutures.allAsList(batchResults);
  }

  @Override
  public CompletableResultCode flush() {
    if (lastExportCode != null) {
      return lastExportCode;
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      logger.log(Level.WARNING, "shutdown is called multiple times");
      return CompletableResultCode.ofSuccess();
    }
    CompletableResultCode flushResult = flush();
    CompletableResultCode shutdownResult = new CompletableResultCode();
    flushResult.whenComplete(
        () -> {
          Throwable throwable = null;
          try {
            client.shutdown();
          } catch (Throwable e) {
            logger.log(Level.WARNING, "failed to shutdown the monitoring client", e);
            throwable = e;
          }
          if (throwable != null) {
            shutdownResult.fail();
          } else {
            shutdownResult.succeed();
          }
        });
    return CompletableResultCode.ofAll(Arrays.asList(flushResult, shutdownResult));
  }

  /**
   * For Google Cloud Monitoring always return CUMULATIVE to keep track of the cumulative value of a
   * metric over time.
   */
  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return AggregationTemporality.CUMULATIVE;
  }
}
