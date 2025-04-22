/*
 * Copyright 2025 Google LLC
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

import com.google.api.gax.core.GaxProperties;
import com.google.api.gax.tracing.OpenTelemetryMetricsRecorder;
import com.google.common.base.Preconditions;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import java.util.Map;

/**
 * Implementation for recording built in metrics.
 *
 * <p>This class extends the {@link OpenTelemetryMetricsRecorder} which implements the *
 * measurements related to the lifecyle of an RPC.
 */
class BuiltInMetricsRecorder extends OpenTelemetryMetricsRecorder {

  private final DoubleHistogram gfeLatencyRecorder;

  /**
   * Creates the following instruments for the following metrics:
   *
   * <ul>
   *   <li>GFE Latency: Histogram
   * </ul>
   *
   * @param openTelemetry OpenTelemetry instance
   * @param serviceName Service Name
   */
  BuiltInMetricsRecorder(OpenTelemetry openTelemetry, String serviceName) {
    super(openTelemetry, serviceName);
    Meter meter =
        openTelemetry
            .meterBuilder(BuiltInMetricsConstant.SPANNER_METER_NAME)
            .setInstrumentationVersion(GaxProperties.getLibraryVersion(getClass()))
            .build();
    this.gfeLatencyRecorder =
        meter
            .histogramBuilder(serviceName + '/' + BuiltInMetricsConstant.GFE_LATENCIES_NAME)
            .setDescription(
                "Latency between Google's network receiving an RPC and reading back the first byte"
                    + " of the response")
            .setUnit("ms")
            .build();
  }

  /**
   * Record the latency between Google's network receiving an RPC and reading back the first byte of
   * the response. Data is stored in a Histogram.
   *
   * @param gfeLatency Attempt Latency in ms
   * @param attributes Map of the attributes to store
   */
  void recordGFELatency(double gfeLatency, Map<String, String> attributes) {
    gfeLatencyRecorder.record(gfeLatency, toOtelAttributes(attributes));
  }

  Attributes toOtelAttributes(Map<String, String> attributes) {
    Preconditions.checkNotNull(attributes, "Attributes map cannot be null");
    AttributesBuilder attributesBuilder = Attributes.builder();
    attributes.forEach(attributesBuilder::put);
    return attributesBuilder.build();
  }
}
