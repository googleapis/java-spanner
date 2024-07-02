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

import static com.google.cloud.spanner.BuiltInMetricsConstant.CLIENT_NAME_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.CLIENT_UID_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.DIRECT_PATH_ENABLED_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.INSTANCE_CONFIG_ID_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.LOCATION_ID_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.PROJECT_ID_KEY;

import com.google.api.gax.core.GaxProperties;
import com.google.auth.Credentials;
import com.google.cloud.opentelemetry.detection.DetectedPlatform;
import com.google.cloud.opentelemetry.detection.GCPPlatformDetector;
import com.google.common.collect.ImmutableSet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

class BuiltInOpenTelemetryMetricsProvider {

  private static final Logger logger =
      Logger.getLogger(BuiltInOpenTelemetryMetricsProvider.class.getName());

  private OpenTelemetry openTelemetry;

  public OpenTelemetry getOpenTelemetry(String projectId, @Nullable Credentials credentials) {
    if (this.openTelemetry == null) {

      // Use custom exporter
      MetricExporter metricExporter = null;
      try {
        metricExporter = SpannerCloudMonitoringExporter.create(projectId, credentials);

        SdkMeterProviderBuilder sdkMeterProviderBuilder = SdkMeterProvider.builder();
        registerView(
            sdkMeterProviderBuilder,
            BuiltInMetricsConstant.OPERATION_LATENCY_NAME,
            BuiltInMetricsConstant.OPERATION_LATENCIES_NAME,
            BuiltInMetricsConstant.AGGREGATION_WITH_MILLIS_HISTOGRAM,
            InstrumentType.HISTOGRAM,
            "ms");
        registerView(
            sdkMeterProviderBuilder,
            BuiltInMetricsConstant.ATTEMPT_LATENCY_NAME,
            BuiltInMetricsConstant.ATTEMPT_LATENCIES_NAME,
            BuiltInMetricsConstant.AGGREGATION_WITH_MILLIS_HISTOGRAM,
            InstrumentType.HISTOGRAM,
            "ms");
        registerView(
            sdkMeterProviderBuilder,
            BuiltInMetricsConstant.OPERATION_COUNT_NAME,
            BuiltInMetricsConstant.OPERATION_COUNT_NAME,
            Aggregation.sum(),
            InstrumentType.COUNTER,
            "1");
        registerView(
            sdkMeterProviderBuilder,
            BuiltInMetricsConstant.ATTEMPT_COUNT_NAME,
            BuiltInMetricsConstant.ATTEMPT_COUNT_NAME,
            Aggregation.sum(),
            InstrumentType.COUNTER,
            "1");

        SdkMeterProvider sdkMeterProvider =
            sdkMeterProviderBuilder
                .registerMetricReader(PeriodicMetricReader.create(metricExporter))
                .build();

        this.openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
      } catch (IOException e) {
        logger.log(
            Level.WARNING,
            "Unable to get OpenTelemetry object for client side metrics, will skip exporting client side metrics",
            e);
      }
    }
    return this.openTelemetry;
  }

  public Map<String, String> getClientAttributes(String projectId) {
    Map<String, String> clientAttributes = new HashMap<>();
    clientAttributes.put(LOCATION_ID_KEY.getKey(), detectClientLocation());
    clientAttributes.put(PROJECT_ID_KEY.getKey(), projectId);
    clientAttributes.put(INSTANCE_CONFIG_ID_KEY.getKey(), "us-central1");
    clientAttributes.put(DIRECT_PATH_ENABLED_KEY.getKey(), "true");
    clientAttributes.put(
        CLIENT_NAME_KEY.getKey(),
        "spanner-java/"
            + GaxProperties.getLibraryVersion(SpannerCloudMonitoringExporterUtils.class));
    clientAttributes.put(CLIENT_UID_KEY.getKey(), getDefaultTaskValue());
    return clientAttributes;
  }

  private void registerView(
      SdkMeterProviderBuilder sdkMeterProviderBuilder, String metricName, String metricViewName,
      Aggregation aggregation,
      InstrumentType type,
      String unit) {
    InstrumentSelector selector =
        InstrumentSelector.builder()
            .setName(BuiltInMetricsConstant.METER_NAME + '/' + metricName)
            .setMeterName(BuiltInMetricsConstant.GAX_METER_NAME)
            .setType(type)
            .setUnit(unit)
            .build();
    Set<String> attributesFilter =
        ImmutableSet.<String>builder()
            .addAll(
                BuiltInMetricsConstant.COMMON_ATTRIBUTES.stream()
                    .map(AttributeKey::getKey)
                    .collect(Collectors.toSet()))
            .build();
    View view =
        View.builder()
            .setName(BuiltInMetricsConstant.METER_NAME + '/' + metricViewName)
            .setAggregation(aggregation)
            .setAttributeFilter(attributesFilter)
            .build();
    sdkMeterProviderBuilder.registerView(selector, view);
  }

  private String detectClientLocation() {
    GCPPlatformDetector detector = GCPPlatformDetector.DEFAULT_INSTANCE;
    DetectedPlatform detectedPlatform = detector.detectPlatform();
    String region = detectedPlatform.getAttributes().get("cloud.region");
    return region;
  }

  /**
   * In most cases this should look like ${UUID}@${hostname}. The hostname will be retrieved from
   * the jvm name and fallback to the local hostname.
   */
  private String getDefaultTaskValue() {
    // Something like '<pid>@<hostname>'
    final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
    // If jvm doesn't have the expected format, fallback to the local hostname
    if (jvmName.indexOf('@') < 1) {
      String hostname = "localhost";
      try {
        hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        logger.log(Level.INFO, "Unable to get the hostname.", e);
      }
      // Generate a random number and use the same format "random_number@hostname".
      return UUID.randomUUID() + "@" + hostname;
    }
    return UUID.randomUUID() + jvmName;
  }
}
