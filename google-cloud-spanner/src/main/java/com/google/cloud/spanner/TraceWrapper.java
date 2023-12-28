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

import io.opencensus.trace.BlankSpan;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opentelemetry.context.Context;

class TraceWrapper {

  private final Tracer openCensusTracer;
  private final io.opentelemetry.api.trace.Tracer openTelemetryTracer;

  TraceWrapper(Tracer openCensusTracer, io.opentelemetry.api.trace.Tracer openTelemetryTracer) {
    this.openTelemetryTracer = openTelemetryTracer;
    this.openCensusTracer = openCensusTracer;
  }

  ISpan spanBuilder(String spanName) {
    if (SpannerOptions.isEnabledOpenTelemetryTraces()) {
      return new OpenTelemetrySpan(openTelemetryTracer.spanBuilder(spanName).startSpan());
    } else {
      return new OpenCensusSpan(openCensusTracer.spanBuilder(spanName).startSpan());
    }
  }

  ISpan spanBuilderWithExplicitParent(String spanName, ISpan parentSpan) {
    if (SpannerOptions.isEnabledOpenTelemetryTraces()) {
      OpenTelemetrySpan otParentSpan;
      if (!(parentSpan instanceof OpenTelemetrySpan)) {
        otParentSpan = new OpenTelemetrySpan(null);
      } else {
        otParentSpan = (OpenTelemetrySpan) parentSpan;
      }

      io.opentelemetry.api.trace.Span otSpan;

      if (otParentSpan.getOpenTelemetrySpan() != null)
        otSpan =
            openTelemetryTracer
                .spanBuilder(spanName)
                .setParent(Context.current().with(otParentSpan.getOpenTelemetrySpan()))
                .startSpan();
      else otSpan = openTelemetryTracer.spanBuilder(spanName).startSpan();

      return new OpenTelemetrySpan(otSpan);

    } else {
      OpenCensusSpan parentOcSpan;
      if (!(parentSpan instanceof OpenCensusSpan)) {
        parentOcSpan = new OpenCensusSpan(null);
      } else {
        parentOcSpan = (OpenCensusSpan) parentSpan;
      }
      Span ocSpan =
          openCensusTracer
              .spanBuilderWithExplicitParent(spanName, parentOcSpan.getOpenCensusSpan())
              .startSpan();

      return new OpenCensusSpan(ocSpan);
    }
  }

  ISpan getCurrentSpan() {
    if (SpannerOptions.isEnabledOpenTelemetryTraces()) {
      return new OpenTelemetrySpan(
          io.opentelemetry.api.trace.Span.fromContext(io.opentelemetry.context.Context.current()));
    } else {
      return new OpenCensusSpan(openCensusTracer.getCurrentSpan());
    }
  }

  ISpan getBlankSpan() {
    if (SpannerOptions.isEnabledOpenTelemetryTraces()) {
      return new OpenTelemetrySpan(io.opentelemetry.api.trace.Span.getInvalid());
    } else {
      return new OpenCensusSpan(BlankSpan.INSTANCE);
    }
  }

  IScope withSpan(ISpan span) {
    if (SpannerOptions.isEnabledOpenTelemetryTraces()) {
      OpenTelemetrySpan openTelemetrySpan;
      if (!(span instanceof OpenTelemetrySpan)) {
        openTelemetrySpan = new OpenTelemetrySpan(null);
      } else {
        openTelemetrySpan = (OpenTelemetrySpan) span;
      }
      return new OpenTelemetryScope(openTelemetrySpan.getOpenTelemetrySpan().makeCurrent());
    } else {
      OpenCensusSpan openCensusSpan;
      if (!(span instanceof OpenCensusSpan)) {
        openCensusSpan = new OpenCensusSpan(null);
      } else {
        openCensusSpan = (OpenCensusSpan) span;
      }
      return new OpenCensusScope(openCensusTracer.withSpan(openCensusSpan.getOpenCensusSpan()));
    }
  }
}
