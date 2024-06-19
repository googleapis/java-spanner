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

import com.google.api.gax.tracing.OpenTelemetryMetricsRecorder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.View;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BuiltInMetricsConstant {

  static final String METER_NAME = "spanner.googleapis.com/internal/client";

  static final String GAX_METER_NAME = OpenTelemetryMetricsRecorder.GAX_METER_NAME;

  static final String OPERATION_LATENCIES_NAME = "operation_latencies";
  static final String ATTEMPT_LATENCIES_NAME = "attempt_latencies";
  static final String OPERATION_LATENCY_NAME = "operation_latency";
  static final String ATTEMPT_LATENCY_NAME = "attempt_latency";
  static final String OPERATION_COUNT_NAME = "operation_count";
  static final String ATTEMPT_COUNT_NAME = "attempt_count";

  static final Set<String> SPANNER_METRICS =
      ImmutableSet.of(
              OPERATION_LATENCIES_NAME,
              ATTEMPT_LATENCIES_NAME,
              OPERATION_COUNT_NAME,
              ATTEMPT_COUNT_NAME)
          .stream()
          .map(m -> METER_NAME + '/' + m)
          .collect(Collectors.toSet());

  static final String SPANNER_RESOURCE_TYPE = "spanner_instance_client";

  static final AttributeKey<String> PROJECT_ID_KEY = AttributeKey.stringKey("project_id");
  public static final AttributeKey<String> INSTANCE_ID_KEY = AttributeKey.stringKey("instance_id");
  static final AttributeKey<String> LOCATION_ID_KEY = AttributeKey.stringKey("location");
  static final AttributeKey<String> INSTANCE_CONFIG_ID_KEY =
      AttributeKey.stringKey("instance_config");

  // These metric labels will be promoted to the spanner monitored resource fields
  static final Set<AttributeKey<String>> SPANNER_PROMOTED_RESOURCE_LABELS =
      ImmutableSet.of(PROJECT_ID_KEY, INSTANCE_ID_KEY, INSTANCE_CONFIG_ID_KEY, LOCATION_ID_KEY);

  public static final AttributeKey<String> DATABASE_KEY = AttributeKey.stringKey("database");
  static final AttributeKey<String> CLIENT_UID_KEY = AttributeKey.stringKey("client_uid");
  static final AttributeKey<String> CLIENT_NAME_KEY = AttributeKey.stringKey("client_name");
  static final AttributeKey<String> METHOD_KEY = AttributeKey.stringKey("method");
  static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");
  static final AttributeKey<String> DIRECT_PATH_ENABLED_KEY =
      AttributeKey.stringKey("directpath_enabled");
  static final AttributeKey<String> DIRECT_PATH_USED_KEY =
      AttributeKey.stringKey("directpath_used");

  static final Set<AttributeKey> COMMON_ATTRIBUTES =
      ImmutableSet.of(
          PROJECT_ID_KEY,
          INSTANCE_ID_KEY,
          LOCATION_ID_KEY,
          INSTANCE_CONFIG_ID_KEY,
          CLIENT_UID_KEY,
          METHOD_KEY,
          STATUS_KEY,
          DATABASE_KEY,
          CLIENT_NAME_KEY,
          DIRECT_PATH_ENABLED_KEY,
          DIRECT_PATH_USED_KEY);

  static Aggregation AGGREGATION_WITH_MILLIS_HISTOGRAM =
      Aggregation.explicitBucketHistogram(
          ImmutableList.of(
              0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 13.0, 16.0, 20.0, 25.0, 30.0, 40.0,
              50.0, 65.0, 80.0, 100.0, 130.0, 160.0, 200.0, 250.0, 300.0, 400.0, 500.0, 650.0,
              800.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0, 100000.0, 200000.0,
              400000.0, 800000.0, 1600000.0, 3200000.0));

  static Map<InstrumentSelector, View> getAllViews() {
    ImmutableMap.Builder<InstrumentSelector, View> views = ImmutableMap.builder();
    defineView(
        views,
        BuiltInMetricsConstant.OPERATION_LATENCY_NAME,
        BuiltInMetricsConstant.OPERATION_LATENCIES_NAME,
        BuiltInMetricsConstant.AGGREGATION_WITH_MILLIS_HISTOGRAM,
        InstrumentType.HISTOGRAM,
        "ms");
    defineView(
        views,
        BuiltInMetricsConstant.ATTEMPT_LATENCY_NAME,
        BuiltInMetricsConstant.ATTEMPT_LATENCIES_NAME,
        BuiltInMetricsConstant.AGGREGATION_WITH_MILLIS_HISTOGRAM,
        InstrumentType.HISTOGRAM,
        "ms");
    defineView(
        views,
        BuiltInMetricsConstant.OPERATION_COUNT_NAME,
        BuiltInMetricsConstant.OPERATION_COUNT_NAME,
        Aggregation.sum(),
        InstrumentType.COUNTER,
        "1");
    defineView(
        views,
        BuiltInMetricsConstant.ATTEMPT_COUNT_NAME,
        BuiltInMetricsConstant.ATTEMPT_COUNT_NAME,
        Aggregation.sum(),
        InstrumentType.COUNTER,
        "1");
    return views.build();
  }

  private static void defineView(
      ImmutableMap.Builder<InstrumentSelector, View> viewMap,
      String metricName,
      String metricViewName,
      Aggregation aggregation,
      InstrumentType type,
      String unit) {
    InstrumentSelector selector =
        InstrumentSelector.builder()
            .setName(BuiltInMetricsConstant.METER_NAME + '/' + metricName)
            .setMeterName(BuiltInMetricsConstant.GAX_METER_NAME)
            .setType(type)
            .setUnit(unit)
            .build();
    Set<String> attributesFilter =
        BuiltInMetricsConstant.COMMON_ATTRIBUTES.stream()
            .map(AttributeKey::getKey)
            .collect(Collectors.toSet());
    View view =
        View.builder()
            .setName(BuiltInMetricsConstant.METER_NAME + '/' + metricViewName)
            .setAggregation(aggregation)
            .setAttributeFilter(attributesFilter)
            .build();
    viewMap.put(selector, view);
  }
}
