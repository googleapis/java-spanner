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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts all gRPC calls to extract server-timing header. Captures GFE Latency and GFE Header
 * Missing count metrics.
 */
class HeaderInterceptor implements ClientInterceptor {

  private static final Metadata.Key<String> SERVER_TIMING_HEADER_KEY =
      Metadata.Key.of("server-timing", Metadata.ASCII_STRING_MARSHALLER);
  private static final Pattern SERVER_TIMING_HEADER_PATTERN = Pattern.compile(".*dur=(?<dur>\\d+)");
  private static final Metadata.Key<String> GOOGLE_CLOUD_RESOURCE_PREFIX_KEY =
      Metadata.Key.of("google-cloud-resource-prefix", Metadata.ASCII_STRING_MARSHALLER);
  private static final Pattern GOOGLE_CLOUD_RESOURCE_PREFIX_PATTERN =
      Pattern.compile(
          ".*projects/(?<project>\\p{ASCII}[^/]*)(/instances/(?<instance>\\p{ASCII}[^/]*))?(/databases/(?<database>\\p{ASCII}[^/]*))?");

  // Get the global singleton Tagger object.
  private static final Tagger TAGGER = Tags.getTagger();
  private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();

  private static final Logger LOGGER = Logger.getLogger(HeaderInterceptor.class.getName());
  private static final Level LEVEL = Level.INFO;

  HeaderInterceptor() {}

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        TagContext tagContext = getTagContext(headers, method.getFullMethodName());
        super.start(
            new SimpleForwardingClientCallListener<RespT>(responseListener) {
              @Override
              public void onHeaders(Metadata metadata) {

                processHeader(metadata, tagContext);
                super.onHeaders(metadata);
              }
            },
            headers);
      }
    };
  }

  private void processHeader(Metadata metadata, TagContext tagContext) {
    MeasureMap measureMap = STATS_RECORDER.newMeasureMap();
    if (metadata.get(SERVER_TIMING_HEADER_KEY) != null) {
      String serverTiming = metadata.get(SERVER_TIMING_HEADER_KEY);
      Matcher matcher = SERVER_TIMING_HEADER_PATTERN.matcher(serverTiming);
      if (matcher.find()) {
        try {
          long latency = Long.parseLong(matcher.group("dur"));
          measureMap.put(SPANNER_GFE_LATENCY, latency);
          measureMap.put(SPANNER_GFE_HEADER_MISSING_COUNT, 0L);
          measureMap.record(tagContext);
        } catch (NumberFormatException e) {
          LOGGER.log(LEVEL, "Invalid server-timing object in header", matcher.group("dur"));
        }
      }
    } else {
      measureMap.put(SPANNER_GFE_HEADER_MISSING_COUNT, 1L).record(tagContext);
    }
  }

  private TagContext getTagContext(
      String method, String projectId, String instanceId, String databaseId) {
    return TAGGER
        .currentBuilder()
        .putLocal(PROJECT_ID, TagValue.create(projectId))
        .putLocal(INSTANCE_ID, TagValue.create(instanceId))
        .putLocal(DATABASE_ID, TagValue.create(databaseId))
        .putLocal(METHOD, TagValue.create(method))
        .build();
  }

  private TagContext getTagContext(Metadata headers, String method) {
    String projectId = "undefined-project";
    String instanceId = "undefined-database";
    String databaseId = "undefined-database";
    if (headers.get(GOOGLE_CLOUD_RESOURCE_PREFIX_KEY) != null) {
      String googleResourcePrefix = headers.get(GOOGLE_CLOUD_RESOURCE_PREFIX_KEY);
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
        LOGGER.log(LEVEL, "Error parsing google cloud resource header: " + googleResourcePrefix);
      }
    }
    return getTagContext(method, projectId, instanceId, databaseId);
  }
}
