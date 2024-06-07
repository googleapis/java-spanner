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

import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.DATABASE_ID;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.INSTANCE_ID;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.METHOD;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.PROJECT_ID;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.SPANNER_GFE_HEADER_MISSING_COUNT;
import static com.google.cloud.spanner.spi.v1.SpannerRpcViews.SPANNER_GFE_LATENCY;

import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerRpcMetrics;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.spanner.admin.database.v1.DatabaseName;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
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
import java.util.concurrent.ExecutionException;
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
  private static final String SERVER_TIMING_HEADER_PREFIX = "gfet4t7; dur=";
  private static final Metadata.Key<String> GOOGLE_CLOUD_RESOURCE_PREFIX_KEY =
      Metadata.Key.of("google-cloud-resource-prefix", Metadata.ASCII_STRING_MARSHALLER);
  private static final Pattern GOOGLE_CLOUD_RESOURCE_PREFIX_PATTERN =
      Pattern.compile(
          ".*projects/(?<project>\\p{ASCII}[^/]*)(/instances/(?<instance>\\p{ASCII}[^/]*))?(/databases/(?<database>\\p{ASCII}[^/]*))?");
  private final Cache<String, DatabaseName> databaseNameCache =
      CacheBuilder.newBuilder().maximumSize(100).build();
  private final Cache<String, TagContext> tagsCache =
      CacheBuilder.newBuilder().maximumSize(1000).build();
  private final Cache<String, Attributes> attributesCache =
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
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        try {
          Span span = Span.current();
          DatabaseName databaseName = extractDatabaseName(headers);
          String key = databaseName + method.getFullMethodName();
          TagContext tagContext = getTagContext(key, method.getFullMethodName(), databaseName);
          Attributes attributes =
              getMetricAttributes(key, method.getFullMethodName(), databaseName);
          super.start(
              new SimpleForwardingClientCallListener<RespT>(responseListener) {
                @Override
                public void onHeaders(Metadata metadata) {
                  processHeader(metadata, tagContext, attributes, span);
                  super.onHeaders(metadata);
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
      Metadata metadata, TagContext tagContext, Attributes attributes, Span span) {
    MeasureMap measureMap = STATS_RECORDER.newMeasureMap();
    String serverTiming = metadata.get(SERVER_TIMING_HEADER_KEY);
    if (serverTiming != null && serverTiming.startsWith(SERVER_TIMING_HEADER_PREFIX)) {
      try {
        long latency = Long.parseLong(serverTiming.substring(SERVER_TIMING_HEADER_PREFIX.length()));
        measureMap.put(SPANNER_GFE_LATENCY, latency);
        measureMap.put(SPANNER_GFE_HEADER_MISSING_COUNT, 0L);
        measureMap.record(tagContext);

        spannerRpcMetrics.recordGfeLatency(latency, attributes);
        spannerRpcMetrics.recordGfeHeaderMissingCount(0L, attributes);

        if (span != null) {
          span.setAttribute("gfe_latency", String.valueOf(latency));
        }
      } catch (NumberFormatException e) {
        LOGGER.log(LEVEL, "Invalid server-timing object in header: {}", serverTiming);
      }
    } else {
      spannerRpcMetrics.recordGfeHeaderMissingCount(1L, attributes);
      measureMap.put(SPANNER_GFE_HEADER_MISSING_COUNT, 1L).record(tagContext);
    }
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
}
