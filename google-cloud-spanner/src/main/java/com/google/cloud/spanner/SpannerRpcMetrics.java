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
    if (openTelemetry == null) {
      return;
    }
    Meter meter = openTelemetry.getMeter(MetricRegistryConstants.Scope);
    gfeLatencies =
        meter
            .histogramBuilder(MetricRegistryConstants.SPANNER_GFE_LATENCY)
            .ofLongs()
            .setDescription(MetricRegistryConstants.SPANNER_GFE_LATENCY_DESCRIPTION)
            .setUnit(MetricRegistryConstants.MILLISECOND)
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
