/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.spanner;

import com.google.api.core.InternalApi;
import com.google.cloud.spanner.spi.v1.SpannerMetrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;

@InternalApi
public class SpannerRpcMetrics {
  private static LongHistogram gfeLatencies = null;
  private static LongCounter gfeHeaderMissingCount = null;

  static void initializeRPCMetrics(OpenTelemetry openTelemetry) {
    if (openTelemetry == null || !SpannerMetrics.isRPCMetricsEnabled()) {
      return;
    }
    Meter meter = openTelemetry.getMeter(MetricRegistryConstants.Scope);
    gfeLatencies =
        meter
            .histogramBuilder(MetricRegistryConstants.SPANNER_GFE_LATENCY)
            .ofLongs()
            .setDescription(MetricRegistryConstants.SPANNER_GFE_LATENCY_DESCRIPTION)
            .setUnit(MetricRegistryConstants.MILLISECOND)
            // .setExplicitBucketBoundariesAdvice(
            //     Collections.unmodifiableList(
            //         Arrays.asList(
            //             0.0, 0.01, 0.05, 0.1, 0.3, 0.6, 0.8, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0,
            //             10.0, 13.0, 16.0, 20.0, 25.0, 30.0, 40.0, 50.0, 65.0, 80.0, 100.0, 130.0,
            //             160.0, 200.0, 250.0, 300.0, 400.0, 500.0, 650.0, 800.0, 1000.0, 2000.0,
            //             5000.0, 10000.0, 20000.0, 50000.0, 100000.0)))
            .build();
    gfeHeaderMissingCount =
        meter
            .counterBuilder(MetricRegistryConstants.SPANNER_GFE_HEADER_MISSING_COUNT)
            .setDescription(MetricRegistryConstants.SPANNER_GFE_HEADER_MISSING_COUNT_DESCRIPTION)
            .setUnit(MetricRegistryConstants.COUNT)
            .build();
  }

  @InternalApi
  public static void recordGfeLatency(long value, Attributes attributes) {
    if (gfeLatencies != null) {
      gfeLatencies.record(value, attributes);
    }
  }

  @InternalApi
  public static void recordGfeHeaderMissingCount(long value, Attributes attributes) {
    if (gfeHeaderMissingCount != null) {
      gfeHeaderMissingCount.add(value, attributes);
    }
  }
}
