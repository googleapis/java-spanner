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

package com.google.cloud.spanner.spi.v1;

import com.google.cloud.spanner.RPCView;
import com.google.cloud.spanner.RPCViewImpl;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.List;
import java.util.Optional;

public final class OpenTelemetryOptions {

  private static SdkMeterProviderBuilder sdkMeterProviderBuilder = SdkMeterProvider.builder();

  private static SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder();

  public static void registerViews(List<RPCView> views) {
    views.forEach(
        view -> {
          Optional<RPCViewImpl> viewImpl = RPCViewImpl.getRPCViewByName(view.name());
          viewImpl.ifPresent(
              rpcView ->
                  sdkMeterProviderBuilder.registerView(
                      rpcView.getInstrumentSelector(), rpcView.getView()));
        });
  }

  public static final SdkMeterProviderBuilder getSdkMeterProviderBuilder() {
    return sdkMeterProviderBuilder;
  }

  public static final SdkTracerProviderBuilder getSdkTracerProviderBuilder() {
    return sdkTracerProviderBuilder;
  }

  public static void registerMetrics() {}

  public static void registerTraces() {}

  public static void registerTracesExporter(SpanExporter exporter) {
    sdkTracerProviderBuilder.addSpanProcessor(BatchSpanProcessor.builder(exporter).build());
  }

  public static void registerMetricsExporter(MetricExporter metricExporter) {
    sdkMeterProviderBuilder.registerMetricReader(
        PeriodicMetricReader.builder(metricExporter).setInterval(java.time.Duration.ofSeconds(5)).build());
  }
}
