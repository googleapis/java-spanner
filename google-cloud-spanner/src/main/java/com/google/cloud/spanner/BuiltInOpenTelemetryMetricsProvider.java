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

import static com.google.cloud.opentelemetry.detection.GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE;
import static com.google.cloud.spanner.BuiltInMetricsConstant.CLIENT_HASH_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.CLIENT_NAME_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.CLIENT_UID_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.INSTANCE_CONFIG_ID_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.LOCATION_ID_KEY;
import static com.google.cloud.spanner.BuiltInMetricsConstant.PROJECT_ID_KEY;

import com.google.auth.Credentials;
import com.google.cloud.opentelemetry.detection.AttributeKeys;
import com.google.cloud.opentelemetry.detection.DetectedPlatform;
import com.google.cloud.opentelemetry.detection.GCPPlatformDetector;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

final class BuiltInOpenTelemetryMetricsProvider {

  static BuiltInOpenTelemetryMetricsProvider INSTANCE = new BuiltInOpenTelemetryMetricsProvider();

  private static final Logger logger =
      Logger.getLogger(BuiltInOpenTelemetryMetricsProvider.class.getName());

  private static String taskId;

  private OpenTelemetry openTelemetry;

  private BuiltInOpenTelemetryMetricsProvider() {}

  OpenTelemetry getOrCreateOpenTelemetry(String projectId, @Nullable Credentials credentials) {
    try {
      if (this.openTelemetry == null) {
        SdkMeterProviderBuilder sdkMeterProviderBuilder = SdkMeterProvider.builder();
        BuiltInOpenTelemetryMetricsView.registerBuiltinMetrics(
            SpannerCloudMonitoringExporter.create(projectId, credentials), sdkMeterProviderBuilder);
        this.openTelemetry =
            OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProviderBuilder.build()).build();
      }
      return this.openTelemetry;
    } catch (IOException ex) {
      logger.log(
          Level.WARNING,
          "Unable to get OpenTelemetry object for client side metrics, will skip exporting client side metrics",
          ex);
      return null;
    }
  }

  Map<String, String> createClientAttributes(String projectId, String client_name) {
    Map<String, String> clientAttributes = new HashMap<>();
    clientAttributes.put(LOCATION_ID_KEY.getKey(), detectClientLocation());
    clientAttributes.put(PROJECT_ID_KEY.getKey(), projectId);
    // TODO: Replace this with real value.
    clientAttributes.put(INSTANCE_CONFIG_ID_KEY.getKey(), "unknown");
    clientAttributes.put(CLIENT_NAME_KEY.getKey(), client_name);
    String clientUid = getDefaultTaskValue();
    clientAttributes.put(CLIENT_UID_KEY.getKey(), clientUid);
    clientAttributes.put(CLIENT_HASH_KEY.getKey(), generateClientHash(clientUid));
    return clientAttributes;
  }

  /**
   * Generates a 6-digit zero-padded all lower case hexadecimal representation of hash of the
   * accounting group. The hash utilizes the 10 most significant bits of the value returned by
   * `Hashing.goodFastHash(64).hashBytes()`, so effectively the returned values are uniformly
   * distributed in the range [000000, 0003ff].
   *
   * <p>The primary purpose of this function is to generate a hash value for the `client_hash`
   * resource label using `client_uid` metric field. The range of values is chosen to be small
   * enough to keep the cardinality of the Resource targets under control. Note: If at later time
   * the range needs to be increased, it can be done by increasing the value of `kPrefixLength` to
   * up to 24 bits without changing the format of the returned value.
   *
   * @return Returns a 6-digit zero-padded all lower case hexadecimal representation of hash of the
   *     accounting group.
   */
  static String generateClientHash(String clientUid) {
    if (clientUid == null) {
      return "000000";
    }

    HashFunction hashFunction = Hashing.goodFastHash(64);
    Long hash = hashFunction.hashBytes(clientUid.getBytes()).asLong();
    // Don't change this value without reading above comment
    int kPrefixLength = 10;
    long shiftedValue = hash >>> (64 - kPrefixLength);
    return String.format("%06x", shiftedValue);
  }

  static String detectClientLocation() {
    GCPPlatformDetector detector = GCPPlatformDetector.DEFAULT_INSTANCE;
    DetectedPlatform detectedPlatform = detector.detectPlatform();
    // All platform except GKE uses "cloud_region" for region attribute.
    String region = detectedPlatform.getAttributes().get("cloud_region");
    if (detectedPlatform.getSupportedPlatform() == GOOGLE_KUBERNETES_ENGINE) {
      region = detectedPlatform.getAttributes().get(AttributeKeys.GKE_CLUSTER_LOCATION);
    }
    return region == null ? "global" : region;
  }

  /**
   * Generates a unique identifier for the Client_uid metric field. The identifier is composed of a
   * UUID, the process ID (PID), and the hostname.
   *
   * <p>For Java 9 and later, the PID is obtained using the ProcessHandle API. For Java 8, the PID
   * is extracted from ManagementFactory.getRuntimeMXBean().getName().
   *
   * @return A unique identifier string in the format UUID@PID@hostname
   */
  private static String getDefaultTaskValue() {
    if (taskId == null) {
      String identifier = UUID.randomUUID().toString();
      String pid = getProcessId();

      try {
        String hostname = InetAddress.getLocalHost().getHostName();
        taskId = identifier + "@" + pid + "@" + hostname;
      } catch (UnknownHostException e) {
        logger.log(Level.INFO, "Unable to get the hostname.", e);
        taskId = identifier + "@" + pid + "@localhost";
      }
    }
    return taskId;
  }

  private static String getProcessId() {
    try {
      // Check if Java 9+ and ProcessHandle class is available
      Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
      Method currentMethod = processHandleClass.getMethod("current");
      Object processHandleInstance = currentMethod.invoke(null);
      Method pidMethod = processHandleClass.getMethod("pid");
      long pid = (long) pidMethod.invoke(processHandleInstance);
      return Long.toString(pid);
    } catch (Exception e) {
      // Fallback to Java 8 method
      final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
      if (jvmName != null && jvmName.contains("@")) {
        return jvmName.split("@")[0];
      } else {
        return "unknown";
      }
    }
  }
}
