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

import com.google.api.gax.tracing.MethodName;
import com.google.api.gax.tracing.MetricsTracer;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements built-in metrics tracer.
 *
 * <p>This class extends the {@link MetricsTracer} which computes generic metrics that can be
 * observed in the lifecycle of an RPC operation.
 */
class BuiltInMetricsTracer extends MetricsTracer {

  private final BuiltInMetricsRecorder builtInOpenTelemetryMetricsRecorder;
  // These are RPC specific attributes and pertain to a specific API Trace
  private final Map<String, String> attributes = new HashMap<>();

  BuiltInMetricsTracer(
      MethodName methodName, BuiltInMetricsRecorder builtInOpenTelemetryMetricsRecorder) {
    super(methodName, builtInOpenTelemetryMetricsRecorder);
    this.builtInOpenTelemetryMetricsRecorder = builtInOpenTelemetryMetricsRecorder;
    this.attributes.put(METHOD_ATTRIBUTE, methodName.toString());
  }

  void recordGFELatency(double gfeLatency) {
    this.builtInOpenTelemetryMetricsRecorder.recordGFELatency(gfeLatency, this.attributes);
  }

  @Override
  public void addAttributes(Map<String, String> attributes) {
    super.addAttributes(attributes);
    this.attributes.putAll(attributes);
  };

  @Override
  public void addAttributes(String key, String value) {
    super.addAttributes(key, value);
    this.attributes.put(key, value);
  }
}
