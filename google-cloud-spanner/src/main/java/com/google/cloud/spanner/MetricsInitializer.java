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

import com.google.cloud.spanner.spi.v1.OpenTelemetryOptions;
import com.google.cloud.spanner.spi.v1.RPCView;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.util.List;
import java.util.Optional;

public class MetricsInitializer {

  private static Meter meter;

  private static OpenTelemetry openTelemetry;

  // Metrics
  private static LongHistogram gfeLatencies = null;

  private static LongCounter gfeHeaderMissingCount = null;

  private MetricsInitializer() {}

  public static void initializeOpenTelemetry() {

    if (openTelemetry == null) {

      SdkMeterProviderBuilder sdkMeterProviderBuilder = SdkMeterProvider.builder();
      List<RPCView> registeredViews = OpenTelemetryOptions.getRegisteredViews();

      registeredViews.forEach(
          view -> {
            Optional<RPCViewImpl> viewImpl = RPCViewImpl.getRPCViewByName(view.name());
            viewImpl.ifPresent(
                rpcView -> {
                  sdkMeterProviderBuilder.registerView(
                      rpcView.getInstrumentSelector(), rpcView.getView());
                });
          });

      MetricExporter metricExporter = OpenTelemetryOptions.getMetricsExporter();

      if (metricExporter != null) {
        sdkMeterProviderBuilder.registerMetricReader(
            PeriodicMetricReader.builder(OpenTelemetryOptions.getMetricsExporter())
                .setInterval(java.time.Duration.ofSeconds(5))
                .build());
      }

      openTelemetry =
          OpenTelemetrySdk.builder()
              .setMeterProvider(sdkMeterProviderBuilder.build())
              // .setTracerProvider(OpenTelemetryOptions.getSdkTracerProviderBuilder().build())
              .build();

      meter =
          openTelemetry
              .meterBuilder(MetricRegistryConstants.Scope)
              .setInstrumentationVersion("1.0.0")
              .build();

      registeredViews.forEach(view -> initializeRPCViewMetrics(view, meter));
    }
  }

  private static void initializeRPCViewMetrics(RPCView rpcView, Meter meter) {
    switch (rpcView) {
      case SPANNER_GFE_LATENCY_VIEW:
        gfeLatencies =
            meter
                .histogramBuilder(RPCViewAttributes.SPANNER_GFE_LATENCY_NAME)
                .ofLongs()
                .setDescription(RPCViewAttributes.SPANNER_GFE_LATENCY_DESCRIPTION)
                .setUnit(RPCViewAttributes.MILLISECOND)
                .build();
        break;
      case SPANNER_GFE_HEADER_MISSING_COUNT_VIEW:
        gfeHeaderMissingCount =
            meter
                .counterBuilder(RPCViewAttributes.SPANNER_GFE_HEADER_MISSING_COUNT_NAME)
                .setDescription(RPCViewAttributes.SPANNER_GFE_HEADER_MISSING_COUNT_DESCRIPTION)
                .setUnit(RPCViewAttributes.COUNT)
                .build();
        break;
    }
  }

  public static OpenTelemetry getOpenTelemetryObject() {
    return openTelemetry;
  }

  public static void gfeLatencyRecorder(long value, Attributes attributes) {
    if (gfeLatencies != null) gfeLatencies.record(value, attributes);
  }

  public static void gfeHeaderMissingCountRecorder(long value, Attributes attributes) {
    if (gfeHeaderMissingCount != null) gfeHeaderMissingCount.add(value, attributes);
  }
}
