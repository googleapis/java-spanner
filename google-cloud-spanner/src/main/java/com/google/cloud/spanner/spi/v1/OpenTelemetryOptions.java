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

import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.ArrayList;
import java.util.List;

public final class OpenTelemetryOptions {

  private static final List<RPCView> registeredViews = new ArrayList<>();

  private static MetricExporter metricExporter = null;

  public static void registerViews(List<RPCView> views) {
    registeredViews.addAll(views);
  }

  public static List<RPCView> getRegisteredViews() {
    return registeredViews;
  }

  public static void registerMetrics() {
  }

  public static void registerTraces() {
  }

  //
  // public static void registerTracesExporter(SpanExporter exporter) {
  //   sdkTracerProviderBuilder.addSpanProcessor(BatchSpanProcessor.builder(exporter).build());
  // }

  public static void registerMetricsExporter(MetricExporter metricExporter) {
    OpenTelemetryOptions.metricExporter = metricExporter;
  }

  public static MetricExporter getMetricsExporter() {
    return metricExporter;
  }
}
