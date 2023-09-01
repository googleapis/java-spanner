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

import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opentelemetry.context.Context;

class TraceWrapper {

  private Tracer openCensusTracer;

  public TraceWrapper(Tracer openCensusTracer) {
    this.openCensusTracer = openCensusTracer;
  }

  public ISpan spanBuilder(String spanName) {
    return new DualSpan(
        openCensusTracer.spanBuilder(spanName).startSpan(),
        SpannerOptions.getOpenTelemetry()
            .getTracer(MetricRegistryConstants.Scope)
            .spanBuilder(spanName)
            .startSpan());
  }

  public ISpan spanBuilderWithExplicitParent(String spanName, ISpan parentSpan) {
    DualSpan dualParentSpan;
    if (!(parentSpan instanceof DualSpan)) {
      dualParentSpan = new DualSpan(null, null);
    } else {
      dualParentSpan = (DualSpan) parentSpan;
    }
    Span ocSpan =
        openCensusTracer
            .spanBuilderWithExplicitParent(spanName, dualParentSpan.getOpenCensusSpan())
            .startSpan();

    io.opentelemetry.api.trace.Span otSpan;

    if (dualParentSpan.getOpenTelemetrySpan() != null)
      otSpan =
          SpannerOptions.getOpenTelemetry()
              .getTracer(MetricRegistryConstants.Scope)
              .spanBuilder(spanName)
              .setParent(Context.current().with(dualParentSpan.getOpenTelemetrySpan()))
              .startSpan();
    else
      otSpan =
          SpannerOptions.getOpenTelemetry()
              .getTracer(MetricRegistryConstants.Scope)
              .spanBuilder(spanName)
              .startSpan();

    return new DualSpan(ocSpan, otSpan);
  }

  public ISpan getCurrentSpan() {
    return new DualSpan(
        openCensusTracer.getCurrentSpan(),
        io.opentelemetry.api.trace.Span.fromContext(io.opentelemetry.context.Context.current()));
  }

  public IScope withSpan(ISpan span) {
    DualSpan dualSpan;
    if (!(span instanceof DualSpan)) {
      dualSpan = new DualSpan(null, null);
    } else {
      dualSpan = (DualSpan) span;
    }
    return new DualScope(
        openCensusTracer.withSpan(dualSpan.getOpenCensusSpan()),
        dualSpan.getOpenTelemetrySpan().makeCurrent());
  }
}
