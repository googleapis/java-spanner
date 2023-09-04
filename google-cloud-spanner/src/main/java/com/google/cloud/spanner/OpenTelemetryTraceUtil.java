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

import com.google.cloud.Timestamp;
import com.google.spanner.v1.Transaction;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/** Utility methods for tracing. */
class OpenTelemetryTraceUtil {

  // static final EndSpanOptions END_SPAN_OPTIONS =
  //     EndSpanOptions.builder().setSampleToLocalSpanStore(true).build();

  static Attributes getTransactionAnnotations(Transaction t) {
    return Attributes.builder()
        .put("Id", t.getId().toStringUtf8())
        .put("Timestamp", Timestamp.fromProto(t.getReadTimestamp()).toString())
        .build();
  }

  static Attributes getExceptionAnnotations(Throwable e) {
    AttributesBuilder attributesBuilder = Attributes.builder();
    if (e instanceof SpannerException) {
      attributesBuilder.put("Status", ((SpannerException) e).getErrorCode().toString());
    }
    return attributesBuilder.build();
  }

  static Attributes getExceptionAnnotations(SpannerException e) {
    return Attributes.builder()
        .put("Status", ((SpannerException) e).getErrorCode().toString())
        .build();
  }

  static void addEvent(Span span, String eventName, Attributes attributes) {
    span.addEvent(eventName, attributes);
  }

  static void addEventWithException(Span span, String eventName, Throwable e) {
    span.addEvent(eventName, getExceptionAnnotations(e));
    span.recordException(e);
  }

  static void addEventWithExceptionAndSetFailure(Span span, String eventName, Throwable e) {
    span.addEvent(eventName, getExceptionAnnotations(e));
    span.setStatus(StatusCode.ERROR);
    span.recordException(e);
  }

  static void setWithFailure(Span span, Throwable e) {
    span.setStatus(StatusCode.ERROR);
    span.recordException(e);
  }

  static void endSpanWithFailure(Span span, Throwable e) {
    span.setStatus(StatusCode.ERROR);
    span.recordException(e);
    span.end();
  }

  static Span spanBuilder(Tracer tracer, String spanName) {
    return tracer.spanBuilder(spanName).startSpan();
  }

  static Span spanBuilderWithExplicitParent(Tracer tracer, String spanName, Span parentSpan) {
    if (parentSpan != null)
      return tracer.spanBuilder(spanName).setParent(Context.root().with(parentSpan)).startSpan();
    else return tracer.spanBuilder(spanName).startSpan();
  }

  static void endSpan(Span span) {
    span.end();
  }
}
