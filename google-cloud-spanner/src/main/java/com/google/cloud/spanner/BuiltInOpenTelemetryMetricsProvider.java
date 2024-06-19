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
import static com.google.cloud.spanner.BuiltInMetricsConstant.LOCATION_ID_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.PROJECT_ID_KEY;

import com.google.api.gax.core.GaxProperties;
import com.google.auth.Credentials;
import com.google.cloud.opentelemetry.detection.DetectedPlatform;
import com.google.cloud.opentelemetry.detection.GCPPlatformDetector;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

class BuiltInOpenTelemetryMetricsProvider {

  private static final Logger logger =
      Logger.getLogger(BuiltInOpenTelemetryMetricsProvider.class.getName());

  private OpenTelemetry openTelemetry;

  OpenTelemetry getOpenTelemetry(String projectId, @Nullable Credentials credentials) {
    try {
      return getOpenTelemetry(SpannerCloudMonitoringExporter.create(projectId, credentials));
    } catch (IOException ex) {
      logger.log(
          Level.WARNING,
          "Unable to get OpenTelemetry object for client side metrics, will skip exporting client side metrics",
          ex);
      return null;
    }
  }

  @VisibleForTesting
  OpenTelemetry getOpenTelemetry(MetricExporter metricExporter) {
    if (this.openTelemetry == null) {
      SdkMeterProviderBuilder sdkMeterProviderBuilder = SdkMeterProvider.builder();
      BuiltInOpenTelemetryMetricsView.registerBuiltinMetrics(
          metricExporter, sdkMeterProviderBuilder);
      this.openTelemetry =
          OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProviderBuilder.build()).build();
    }
    return this.openTelemetry;
  }

  Map<String, String> getClientAttributes(String projectId, boolean canUseDirectPath) {
    Map<String, String> clientAttributes = new HashMap<>();
    clientAttributes.put(LOCATION_ID_KEY.getKey(), detectClientLocation());
    clientAttributes.put(PROJECT_ID_KEY.getKey(), projectId);
    clientAttributes.put(DIRECT_PATH_ENABLED_KEY.getKey(), String.valueOf(canUseDirectPath));
    clientAttributes.put(
        CLIENT_NAME_KEY.getKey(),
        "spanner-java/"
            + GaxProperties.getLibraryVersion(SpannerCloudMonitoringExporterUtils.class));
    clientAttributes.put(CLIENT_UID_KEY.getKey(), getDefaultTaskValue());
    return clientAttributes;
  }

  private String detectClientLocation() {
    GCPPlatformDetector detector = GCPPlatformDetector.DEFAULT_INSTANCE;
    DetectedPlatform detectedPlatform = detector.detectPlatform();
    String region = detectedPlatform.getAttributes().get("cloud.region");
    return region == null ? "global" : region;
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
