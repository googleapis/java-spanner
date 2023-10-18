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

import com.google.common.collect.ImmutableMap;
import io.opencensus.contrib.grpc.util.StatusConverter;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.EndSpanOptions;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.StatusCode;
import java.util.HashMap;
import java.util.Map;

class DualSpan implements ISpan {

  static final EndSpanOptions END_SPAN_OPTIONS =
      EndSpanOptions.builder().setSampleToLocalSpanStore(true).build();
  private final Span openCensusSpan;
  private final io.opentelemetry.api.trace.Span openTelemetrySpan;

  public DualSpan(Span openCensusSpan, io.opentelemetry.api.trace.Span openTelemetrySpan) {
    this.openCensusSpan = openCensusSpan;
    this.openTelemetrySpan = openTelemetrySpan;
  }

  Span getOpenCensusSpan() {
    return openCensusSpan;
  }

  io.opentelemetry.api.trace.Span getOpenTelemetrySpan() {
    return openTelemetrySpan;
  }

  @Override
  public void addAnnotation(String message, Map<String, Object> attributes) {
    AttributesBuilder otAttributesBuilder = Attributes.builder();
    Map<String, AttributeValue> ocAttributeValues = new HashMap<>();
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      String key = entry.getKey();
      Object val = entry.getValue();
      if (val == null || val instanceof String) {
        String strVal = (String) val;
        otAttributesBuilder.put(key, strVal);
        ocAttributeValues.put(key, AttributeValue.stringAttributeValue(strVal));
      } else if (val instanceof Long) {
        long longVal = (Long) val;
        otAttributesBuilder.put(key, longVal);
        ocAttributeValues.put(key, AttributeValue.longAttributeValue(longVal));
      }
    }
    openTelemetrySpan.addEvent(message, otAttributesBuilder.build());
    openCensusSpan.addAnnotation(message, ocAttributeValues);
  }

  @Override
  public void addAnnotation(String message) {
    openTelemetrySpan.addEvent(message);
    openCensusSpan.addAnnotation(message);
  }

  @Override
  public void addAnnotation(String message, String key, String value) {
    openTelemetrySpan.addEvent(message, Attributes.builder().put(key, value).build());
    openCensusSpan.addAnnotation(
        message, ImmutableMap.of(key, AttributeValue.stringAttributeValue(value)));
  }

  @Override
  public void addAnnotation(String message, String key, long value) {
    openTelemetrySpan.addEvent(message, Attributes.builder().put(key, value).build());
    openCensusSpan.addAnnotation(
        message, ImmutableMap.of(key, AttributeValue.longAttributeValue(value)));
  }

  @Override
  public void addAnnotation(String message, Throwable e) {
    openCensusSpan.addAnnotation(message, this.getOpenCensusExceptionAnnotations(e));
    openTelemetrySpan.addEvent(message, this.getOpenTelemetryExceptionAnnotations(e));
  }

  @Override
  public void setStatus(Throwable e) {
    openTelemetrySpan.setStatus(StatusCode.ERROR);
    openTelemetrySpan.recordException(e);
    if (e instanceof SpannerException) {
      openCensusSpan.setStatus(
          StatusConverter.fromGrpcStatus(((SpannerException) e).getErrorCode().getGrpcStatus())
              .withDescription(e.getMessage()));
    } else {
      openCensusSpan.setStatus(Status.INTERNAL.withDescription(e.getMessage()));
    }
  }

  @Override
  public void setStatus(Status status) {
    openTelemetrySpan.setStatus(StatusCode.ERROR);
    openTelemetrySpan.recordException(new Throwable(status.getDescription()));
    openCensusSpan.setStatus(status);
  }

  @Override
  public void end() {
    openCensusSpan.end(END_SPAN_OPTIONS);
    openTelemetrySpan.end();
  }

  private ImmutableMap<String, AttributeValue> getOpenCensusExceptionAnnotations(Throwable e) {
    if (e instanceof SpannerException) {
      return ImmutableMap.of(
          "Status",
          AttributeValue.stringAttributeValue(((SpannerException) e).getErrorCode().toString()));
    }
    return ImmutableMap.of();
  }

  private Attributes getOpenTelemetryExceptionAnnotations(Throwable e) {
    AttributesBuilder attributesBuilder = Attributes.builder();
    if (e instanceof SpannerException) {
      attributesBuilder.put("Status", ((SpannerException) e).getErrorCode().toString());
    }
    return attributesBuilder.build();
  }
}
