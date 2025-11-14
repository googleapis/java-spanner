/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.spanner.spi.v1;

import static com.google.api.gax.grpc.GrpcCallContext.TRACER_KEY;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.DATABASE_ID;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.INSTANCE_ID;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.METHOD;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.PROJECT_ID;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.SPANNER_GFE_HEADER_MISSING_COUNT;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.SPANNER_GFE_LATENCY;

import com.google.api.gax.tracing.ApiTracer;
import com.google.cloud.spanner.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.spanner.admin.database.v1.DatabaseName;
import io.grpc.*;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.alts.AltsContextUtil;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts all gRPC calls to extract server-timing header. Captures GFE Latency and GFE Header
 * Missing count metrics.
 */
class HeaderInterceptor implements ClientInterceptor {
  private static final DatabaseName UNDEFINED_DATABASE_NAME =
      DatabaseName.of("undefined-project", "undefined-instance", "undefined-database");
  private static final Metadata.Key<String> SERVER_TIMING_HEADER_KEY =
      Metadata.Key.of("server-timing", Metadata.ASCII_STRING_MARSHALLER);
  private static final String GFE_TIMING_HEADER = "gfet4t7";
  private static final String AFE_TIMING_HEADER = "afe";
  private static final Metadata.Key<String> GOOGLE_CLOUD_RESOURCE_PREFIX_KEY =
      Metadata.Key.of("google-cloud-resource-prefix", Metadata.ASCII_STRING_MARSHALLER);
  private static final Pattern SERVER_TIMING_PATTERN =
      Pattern.compile("(?<metricName>[a-zA-Z0-9_-]+);\\s*dur=(?<duration>\\d+(\\.\\d+)?)");
  private static final Pattern GOOGLE_CLOUD_RESOURCE_PREFIX_PATTERN =
      Pattern.compile(
          ".*projects/(?<project>\\p{ASCII}[^/]*)(/instances/(?<instance>\\p{ASCII}[^/]*))?(/databases/(?<database>\\p{ASCII}[^/]*))?");
  private final Cache<String, DatabaseName> databaseNameCache =
      CacheBuilder.newBuilder().maximumSize(100).build();
  private final Cache<String, TagContext> tagsCache =
      CacheBuilder.newBuilder().maximumSize(1000).build();
  private final Cache<String, Attributes> attributesCache =
      CacheBuilder.newBuilder().maximumSize(1000).build();
  private final Cache<String, Map<String, String>> builtInAttributesCache =
      CacheBuilder.newBuilder().maximumSize(1000).build();
  private final Cache<DatabaseName, Cache<String, String>> keyCache =
      CacheBuilder.newBuilder().maximumSize(1000).build();

  // Get the global singleton Tagger object.
  private static final Tagger TAGGER = Tags.getTagger();
  private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();

  private static final Logger LOGGER = Logger.getLogger(HeaderInterceptor.class.getName());
  private static final Level LEVEL = Level.INFO;
  private final SpannerRpcMetrics spannerRpcMetrics;

  HeaderInterceptor(SpannerRpcMetrics spannerRpcMetrics) {
    this.spannerRpcMetrics = spannerRpcMetrics;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    ApiTracer tracer = callOptions.getOption(TRACER_KEY);
    CompositeTracer compositeTracer =
        tracer instanceof CompositeTracer ? (CompositeTracer) tracer : null;
    final AtomicBoolean headersReceived = new AtomicBoolean(false);
    SpannerGrpcStreamTracer streamTracer = new SpannerGrpcStreamTracer();
    CallOptions newOptions =
        callOptions.withStreamTracerFactory(new SpannerGrpcStreamTracer.Factory(streamTracer));
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, newOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        try {
          Span span = Span.current();
          DatabaseName databaseName = extractDatabaseName(headers);
          String key = extractKey(databaseName, method.getFullMethodName());
          String requestId = extractRequestId(headers);
          TagContext tagContext = getTagContext(key, method.getFullMethodName(), databaseName);
          Attributes attributes =
              getMetricAttributes(key, method.getFullMethodName(), databaseName);
          Map<String, String> builtInMetricsAttributes =
              getBuiltInMetricAttributes(key, databaseName);
          builtInMetricsAttributes.put(BuiltInMetricsConstant.REQUEST_ID_KEY.getKey(), requestId);
          addBuiltInMetricAttributes(compositeTracer, builtInMetricsAttributes);
          if (span != null) {
            span.setAttribute(XGoogSpannerRequestId.REQUEST_ID, requestId);
          }
          super.start(
              new SimpleForwardingClientCallListener<RespT>(responseListener) {
                @Override
                public void onHeaders(Metadata metadata) {
                  headersReceived.set(true);
                  // Check if the call uses DirectPath by inspecting the ALTS context.
                  boolean isDirectPathUsed = AltsContextUtil.check(getAttributes());
                  addDirectPathUsedAttribute(compositeTracer, isDirectPathUsed);
                  processHeader(
                      metadata, tagContext, attributes, span, compositeTracer, isDirectPathUsed);
                  super.onHeaders(metadata);
                }

                @Override
                public void onClose(Status status, Metadata trailers) {
                  // Check if RPC was sent from gRPC client, but no response headers were received.
                  // This can happen in
                  // case of a timeout, for example.
                  if (streamTracer.isOutBoundMessageSent() && !headersReceived.get()) {
                    if (compositeTracer != null) {
                      compositeTracer.recordGfeHeaderMissingCount(1L);
                      // Disable afe_connectivity_error_count metric as AFE header is disabled in
                      // backend
                      // currently.
                      // if (GapicSpannerRpc.isEnableAFEServerTiming()) {
                      //   compositeTracer.recordAfeHeaderMissingCount(1L);
                      // }
                    }
                  }
                  super.onClose(status, trailers);
                }
              },
              headers);
        } catch (ExecutionException executionException) {
          // This should never happen,
          throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
        }
      }
    };
  }

  private void processHeader(
      Metadata metadata,
      TagContext tagContext,
      Attributes attributes,
      Span span,
      CompositeTracer compositeTracer,
      boolean isDirectPathUsed) {
    MeasureMap measureMap = STATS_RECORDER.newMeasureMap();
    String serverTiming = metadata.get(SERVER_TIMING_HEADER_KEY);
    try {
      // Previous implementation parsed the GFE latency directly using:
      // long latency = Long.parseLong(serverTiming.substring("gfet4t7; dur=".length()));
      // This approach assumed the serverTiming header contained exactly one metric "gfet4t7".
      // If additional metrics were introduced in the header, older versions of the library
      // would fail to parse it correctly. To make the parsing more robust, the logic has been
      // updated to handle multiple metrics gracefully.

      Map<String, Float> serverTimingMetrics = parseServerTimingHeader(serverTiming);
      if (serverTimingMetrics.containsKey(GFE_TIMING_HEADER)) {
        float gfeLatency = serverTimingMetrics.get(GFE_TIMING_HEADER);

        measureMap.put(SPANNER_GFE_LATENCY, (long) gfeLatency);
        measureMap.put(SPANNER_GFE_HEADER_MISSING_COUNT, 0L);
        measureMap.record(tagContext);

        spannerRpcMetrics.recordGfeLatency((long) gfeLatency, attributes);
        spannerRpcMetrics.recordGfeHeaderMissingCount(0L, attributes);
        if (compositeTracer != null && !isDirectPathUsed) {
          compositeTracer.recordGFELatency(gfeLatency);
        }
        if (span != null) {
          span.setAttribute("gfe_latency", String.valueOf(gfeLatency));
        }
      } else {
        measureMap.put(SPANNER_GFE_HEADER_MISSING_COUNT, 1L).record(tagContext);
        spannerRpcMetrics.recordGfeHeaderMissingCount(1L, attributes);
        if (compositeTracer != null && !isDirectPathUsed) {
          compositeTracer.recordGfeHeaderMissingCount(1L);
        }
      }

      // Record AFE metrics
      if (compositeTracer != null && GapicSpannerRpc.isEnableAFEServerTiming()) {
        if (serverTimingMetrics.containsKey(AFE_TIMING_HEADER)) {
          float afeLatency = serverTimingMetrics.get(AFE_TIMING_HEADER);
          compositeTracer.recordAFELatency(afeLatency);
        } else {
          // Disable afe_connectivity_error_count metric as AFE header is disabled in backend
          // currently.
          // compositeTracer.recordAfeHeaderMissingCount(1L);
        }
      }
    } catch (NumberFormatException e) {
      LOGGER.log(LEVEL, "Invalid server-timing object in header: {}", serverTiming);
    }
  }

  private Map<String, Float> parseServerTimingHeader(String serverTiming) {
    Map<String, Float> serverTimingMetrics = new HashMap<>();
    if (serverTiming != null) {
      Matcher matcher = SERVER_TIMING_PATTERN.matcher(serverTiming);
      while (matcher.find()) {
        String metricName = matcher.group("metricName");
        String durationStr = matcher.group("duration");

        if (metricName != null && durationStr != null) {
          serverTimingMetrics.put(metricName, Float.valueOf(durationStr));
        }
      }
    }
    return serverTimingMetrics;
  }

  private String extractKey(DatabaseName databaseName, String methodName)
      throws ExecutionException {
    Cache<String, String> keys =
        keyCache.get(databaseName, () -> CacheBuilder.newBuilder().maximumSize(1000).build());
    return keys.get(methodName, () -> databaseName + methodName);
  }

  private DatabaseName extractDatabaseName(Metadata headers) throws ExecutionException {
    String googleResourcePrefix = headers.get(GOOGLE_CLOUD_RESOURCE_PREFIX_KEY);
    if (googleResourcePrefix != null) {
      return databaseNameCache.get(
          googleResourcePrefix,
          () -> {
            String projectId = "undefined-project";
            String instanceId = "undefined-database";
            String databaseId = "undefined-database";
            Matcher matcher = GOOGLE_CLOUD_RESOURCE_PREFIX_PATTERN.matcher(googleResourcePrefix);
            if (matcher.find()) {
              projectId = matcher.group("project");
              if (matcher.group("instance") != null) {
                instanceId = matcher.group("instance");
              }
              if (matcher.group("database") != null) {
                databaseId = matcher.group("database");
              }
            } else {
              LOGGER.log(
                  LEVEL, "Error parsing google cloud resource header: " + googleResourcePrefix);
            }
            return DatabaseName.of(projectId, instanceId, databaseId);
          });
    }
    return UNDEFINED_DATABASE_NAME;
  }

  private String extractRequestId(Metadata headers) throws ExecutionException {
    return headers.get(XGoogSpannerRequestId.REQUEST_HEADER_KEY);
  }

  private TagContext getTagContext(String key, String method, DatabaseName databaseName)
      throws ExecutionException {
    return tagsCache.get(
        key,
        () ->
            TAGGER
                .currentBuilder()
                .putLocal(PROJECT_ID, TagValue.create(databaseName.getProject()))
                .putLocal(INSTANCE_ID, TagValue.create(databaseName.getInstance()))
                .putLocal(DATABASE_ID, TagValue.create(databaseName.getDatabase()))
                .putLocal(METHOD, TagValue.create(method))
                .build());
  }

  private Attributes getMetricAttributes(String key, String method, DatabaseName databaseName)
      throws ExecutionException {
    return attributesCache.get(
        key,
        () -> {
          AttributesBuilder attributesBuilder = Attributes.builder();
          attributesBuilder.put("database", databaseName.getDatabase());
          attributesBuilder.put("instance_id", databaseName.getInstance());
          attributesBuilder.put("project_id", databaseName.getProject());
          attributesBuilder.put("method", method);

          return attributesBuilder.build();
        });
  }

  private Map<String, String> getBuiltInMetricAttributes(String key, DatabaseName databaseName)
      throws ExecutionException {
    return builtInAttributesCache.get(
        key,
        () -> {
          Map<String, String> attributes = new HashMap<>();
          attributes.put(BuiltInMetricsConstant.DATABASE_KEY.getKey(), databaseName.getDatabase());
          attributes.put(
              BuiltInMetricsConstant.INSTANCE_ID_KEY.getKey(), databaseName.getInstance());
          attributes.put(
              BuiltInMetricsConstant.DIRECT_PATH_ENABLED_KEY.getKey(),
              String.valueOf(GapicSpannerRpc.DIRECTPATH_CHANNEL_CREATED));
          return attributes;
        });
  }

  private void addBuiltInMetricAttributes(
      CompositeTracer compositeTracer, Map<String, String> builtInMetricsAttributes) {
    if (compositeTracer != null) {
      compositeTracer.addAttributes(builtInMetricsAttributes);
    }
  }

  private void addDirectPathUsedAttribute(
      CompositeTracer compositeTracer, Boolean isDirectPathUsed) {
    if (compositeTracer != null) {
      compositeTracer.addAttributes(
          BuiltInMetricsConstant.DIRECT_PATH_USED_KEY.getKey(), Boolean.toString(isDirectPathUsed));
    }
  }
}
