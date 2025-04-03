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

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.tracing.ApiTracer;
import com.google.api.gax.tracing.MethodName;
import com.google.api.gax.tracing.MetricsTracer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;

/**
 * Implements built-in metrics tracer.
 *
 * <p>This class extends the {@link MetricsTracer} which computes generic metrics that can be
 * observed in the lifecycle of an RPC operation.
 */
class BuiltInMetricsTracer extends MetricsTracer implements ApiTracer {

  private final BuiltInMetricsRecorder builtInOpenTelemetryMetricsRecorder;
  // These are RPC specific attributes and pertain to a specific API Trace
  private final Map<String, String> attributes = new HashMap<>();

  private Long gfeLatency = null;

  private Long afeLatency = null;

  private long gfeHeaderMissingCount = 0;

  private long afeHeaderMissingCount = 0;

  BuiltInMetricsTracer(
      MethodName methodName, BuiltInMetricsRecorder builtInOpenTelemetryMetricsRecorder) {
    super(methodName, builtInOpenTelemetryMetricsRecorder);
    this.builtInOpenTelemetryMetricsRecorder = builtInOpenTelemetryMetricsRecorder;
    this.attributes.put(METHOD_ATTRIBUTE, methodName.toString());
  }

  /**
   * Adds an annotation that the attempt succeeded. Successful attempt add "OK" value to the status
   * attribute key.
   */
  @Override
  public void attemptSucceeded() {
    super.attemptSucceeded();
    if (gfeLatency != null) {
      attributes.put(STATUS_ATTRIBUTE, StatusCode.Code.OK.toString());
      builtInOpenTelemetryMetricsRecorder.recordServerTimingHeaderMetrics(
          gfeLatency, afeLatency, gfeHeaderMissingCount, afeHeaderMissingCount, attributes);
    }
  }

  /**
   * Add an annotation that the attempt was cancelled by the user. Cancelled attempt add "CANCELLED"
   * to the status attribute key.
   */
  @Override
  public void attemptCancelled() {
    super.attemptCancelled();
    if (gfeLatency != null) {
      attributes.put(STATUS_ATTRIBUTE, StatusCode.Code.CANCELLED.toString());
      builtInOpenTelemetryMetricsRecorder.recordServerTimingHeaderMetrics(
          gfeLatency, afeLatency, gfeHeaderMissingCount, afeHeaderMissingCount, attributes);
    }
  }

  /**
   * Adds an annotation that the attempt failed, but another attempt will be made after the delay.
   *
   * @param error the error that caused the attempt to fail.
   * @param delay the amount of time to wait before the next attempt will start.
   *     <p>Failed attempt extracts the error from the throwable and adds it to the status attribute
   *     key.
   */
  @Override
  public void attemptFailedDuration(Throwable error, java.time.Duration delay) {
    super.attemptFailedDuration(error, delay);
    if (gfeLatency != null) {
      attributes.put(STATUS_ATTRIBUTE, extractStatus(error));
      builtInOpenTelemetryMetricsRecorder.recordServerTimingHeaderMetrics(
          gfeLatency, afeLatency, gfeHeaderMissingCount, afeHeaderMissingCount, attributes);
    }
  }

  /**
   * Adds an annotation that the attempt failed and that no further attempts will be made because
   * retry limits have been reached. This extracts the error from the throwable and adds it to the
   * status attribute key.
   *
   * @param error the last error received before retries were exhausted.
   */
  @Override
  public void attemptFailedRetriesExhausted(Throwable error) {
    super.attemptFailedRetriesExhausted(error);
    if (gfeLatency != null) {
      attributes.put(STATUS_ATTRIBUTE, extractStatus(error));
      builtInOpenTelemetryMetricsRecorder.recordServerTimingHeaderMetrics(
          gfeLatency, afeLatency, gfeHeaderMissingCount, afeHeaderMissingCount, attributes);
    }
  }

  /**
   * Adds an annotation that the attempt failed and that no further attempts will be made because
   * the last error was not retryable. This extracts the error from the throwable and adds it to the
   * status attribute key.
   *
   * @param error the error that caused the final attempt to fail.
   */
  @Override
  public void attemptPermanentFailure(Throwable error) {
    super.attemptPermanentFailure(error);
    if (gfeLatency != null) {
      attributes.put(STATUS_ATTRIBUTE, extractStatus(error));
      builtInOpenTelemetryMetricsRecorder.recordServerTimingHeaderMetrics(
          gfeLatency, afeLatency, gfeHeaderMissingCount, afeHeaderMissingCount, attributes);
    }
  }

  void recordGFELatency(Long gfeLatency) {
    this.gfeLatency = gfeLatency;
  }

  void recordAFELatency(Long afeLatency) {
    this.afeLatency = afeLatency;
  }

  void recordGfeHeaderMissingCount(Long value) {
    this.gfeHeaderMissingCount = value;
  }

  void recordAfeHeaderMissingCount(Long value) {
    this.afeHeaderMissingCount = value;
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

  private static String extractStatus(@Nullable Throwable error) {
    final String statusString;

    if (error == null) {
      return StatusCode.Code.OK.toString();
    } else if (error instanceof CancellationException) {
      statusString = StatusCode.Code.CANCELLED.toString();
    } else if (error instanceof ApiException) {
      statusString = ((ApiException) error).getStatusCode().getCode().toString();
    } else {
      statusString = StatusCode.Code.UNKNOWN.toString();
    }

    return statusString;
  }
}
