/*
 * Copyright 2023 Google LLC
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

import com.google.common.collect.ImmutableList;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.View;

public class OpenTelemetryMetricsAttributes {
  static final String SCOPE = "cloud.google.com";
  private static final Aggregation AGGREGATION_WITH_MILLIS_HISTOGRAM =
      Aggregation.explicitBucketHistogram(
          ImmutableList.of(
              0.0, 0.01, 0.05, 0.1, 0.3, 0.6, 0.8, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 13.0,
              16.0, 20.0, 25.0, 30.0, 40.0, 50.0, 65.0, 80.0, 100.0, 130.0, 160.0, 200.0, 250.0,
              300.0, 400.0, 500.0, 650.0, 800.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0,
              100000.0));
  static final InstrumentSelector SPANNER_GFE_LATENCY_SELECTOR =
      InstrumentSelector.builder()
          .setName("gfe_latency_ot")
          .setMeterName(SCOPE)
          .setType(InstrumentType.HISTOGRAM)
          .setUnit("ms")
          .build();

  static final View SPANNER_GFE_LATENCY_VIEW =
      View.builder()
          .setName("custom.googleapis.com/java/spanner/gfe_latency_ot")
          .setAggregation(AGGREGATION_WITH_MILLIS_HISTOGRAM)
          .build();
}
