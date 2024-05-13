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

import com.google.cloud.spanner.Options.TagOption;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.SpannerOptions.TracingFramework;
import io.opencensus.trace.BlankSpan;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class TraceWrapper {
  private static final AttributeKey<String> TRANSACTION_TAG_KEY =
      AttributeKey.stringKey("transaction.tag");
  private static final AttributeKey<String> STATEMENT_TAG_KEY =
      AttributeKey.stringKey("statement.tag");
  private static final AttributeKey<String> DB_STATEMENT_KEY =
      AttributeKey.stringKey("db.statement");
  private static final AttributeKey<List<String>> DB_STATEMENT_ARRAY_KEY =
      AttributeKey.stringArrayKey("db.statement");

  private final Tracer openCensusTracer;
  private final io.opentelemetry.api.trace.Tracer openTelemetryTracer;
  private final boolean enableExtendedTracing;

  TraceWrapper(
      Tracer openCensusTracer,
      io.opentelemetry.api.trace.Tracer openTelemetryTracer,
      boolean enableExtendedTracing) {
    this.openTelemetryTracer = openTelemetryTracer;
    this.openCensusTracer = openCensusTracer;
    this.enableExtendedTracing = enableExtendedTracing;
  }

  ISpan spanBuilder(String spanName) {
    return spanBuilder(spanName, Attributes.empty());
  }

  ISpan spanBuilder(String spanName, TransactionOption... options) {
    return spanBuilder(spanName, createTransactionAttributes(options));
  }

  ISpan spanBuilder(String spanName, Attributes attributes) {
    if (SpannerOptions.getActiveTracingFramework().equals(TracingFramework.OPEN_TELEMETRY)) {
      return new OpenTelemetrySpan(
          openTelemetryTracer.spanBuilder(spanName).setAllAttributes(attributes).startSpan());
    } else {
      return new OpenCensusSpan(openCensusTracer.spanBuilder(spanName).startSpan());
    }
  }

  ISpan spanBuilderWithExplicitParent(String spanName, ISpan parentSpan) {
    return spanBuilderWithExplicitParent(spanName, parentSpan, Attributes.empty());
  }

  ISpan spanBuilderWithExplicitParent(String spanName, ISpan parentSpan, Attributes attributes) {
    if (SpannerOptions.getActiveTracingFramework().equals(TracingFramework.OPEN_TELEMETRY)) {
      OpenTelemetrySpan otParentSpan = (OpenTelemetrySpan) parentSpan;

      io.opentelemetry.api.trace.SpanBuilder otSpan =
          openTelemetryTracer.spanBuilder(spanName).setAllAttributes(attributes);
      if (otParentSpan != null && otParentSpan.getOpenTelemetrySpan() != null) {
        otSpan = otSpan.setParent(Context.current().with(otParentSpan.getOpenTelemetrySpan()));
      }
      return new OpenTelemetrySpan(otSpan.startSpan());
    } else {
      OpenCensusSpan parentOcSpan = (OpenCensusSpan) parentSpan;
      Span ocSpan =
          openCensusTracer
              .spanBuilderWithExplicitParent(
                  spanName, parentOcSpan != null ? parentOcSpan.getOpenCensusSpan() : null)
              .startSpan();

      return new OpenCensusSpan(ocSpan);
    }
  }

  ISpan getCurrentSpan() {
    if (SpannerOptions.getActiveTracingFramework().equals(TracingFramework.OPEN_TELEMETRY)) {
      return new OpenTelemetrySpan(
          io.opentelemetry.api.trace.Span.fromContext(io.opentelemetry.context.Context.current()));
    } else {
      return new OpenCensusSpan(openCensusTracer.getCurrentSpan());
    }
  }

  ISpan getBlankSpan() {
    if (SpannerOptions.getActiveTracingFramework().equals(TracingFramework.OPEN_TELEMETRY)) {
      return new OpenTelemetrySpan(io.opentelemetry.api.trace.Span.getInvalid());
    } else {
      return new OpenCensusSpan(BlankSpan.INSTANCE);
    }
  }

  IScope withSpan(ISpan span) {
    if (SpannerOptions.getActiveTracingFramework().equals(TracingFramework.OPEN_TELEMETRY)) {
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

  Attributes createTransactionAttributes(TransactionOption... options) {
    if (options != null && options.length > 0) {
      Optional<TagOption> tagOption =
          Arrays.stream(options)
              .filter(option -> option instanceof TagOption)
              .map(option -> (TagOption) option)
              .findAny();
      if (tagOption.isPresent()) {
        return Attributes.of(TRANSACTION_TAG_KEY, tagOption.get().getTag());
      }
    }
    return Attributes.empty();
  }

  Attributes createStatementAttributes(Statement statement, Options options) {
    if (this.enableExtendedTracing || (options != null && options.hasTag())) {
      AttributesBuilder builder = Attributes.builder();
      if (this.enableExtendedTracing) {
        builder.put(DB_STATEMENT_KEY, statement.getSql());
      }
      if (options != null && options.hasTag()) {
        builder.put(STATEMENT_TAG_KEY, options.tag());
      }
      return builder.build();
    }
    return Attributes.empty();
  }

  Attributes createStatementBatchAttributes(Iterable<Statement> statements, Options options) {
    if (this.enableExtendedTracing || (options != null && options.hasTag())) {
      AttributesBuilder builder = Attributes.builder();
      if (this.enableExtendedTracing) {
        builder.put(
            DB_STATEMENT_ARRAY_KEY,
            StreamSupport.stream(statements.spliterator(), false)
                .map(Statement::getSql)
                .collect(Collectors.toList()));
      }
      if (options != null && options.hasTag()) {
        builder.put(STATEMENT_TAG_KEY, options.tag());
      }
      return builder.build();
    }
    return Attributes.empty();
  }
}
