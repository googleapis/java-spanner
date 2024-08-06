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

import com.google.common.collect.ImmutableSet;
import io.opentelemetry.api.common.AttributeKey;
import java.util.Set;
import java.util.stream.Collectors;

public class BuiltInMetricsConstant {

  public static final String METER_NAME = "spanner.googleapis.com/internal/client";

  public static final String GAX_METER_NAME = "gax-java";

  static final String OPERATION_LATENCIES_NAME = "operation_latencies";
  static final String ATTEMPT_LATENCIES_NAME = "attempt_latencies";
  static final String OPERATION_LATENCY_NAME = "operation_latency";
  static final String ATTEMPT_LATENCY_NAME = "attempt_latency";
  static final String OPERATION_COUNT_NAME = "operation_count";
  static final String ATTEMPT_COUNT_NAME = "attempt_count";

  public static final Set<String> SPANNER_METRICS =
      ImmutableSet.of(
              OPERATION_LATENCIES_NAME,
              ATTEMPT_LATENCIES_NAME,
              OPERATION_COUNT_NAME,
              ATTEMPT_COUNT_NAME)
          .stream()
          .map(m -> METER_NAME + '/' + m)
          .collect(Collectors.toSet());

  public static final String SPANNER_RESOURCE_TYPE = "spanner_instance_client";

  public static final AttributeKey<String> PROJECT_ID_KEY = AttributeKey.stringKey("project_id");
  public static final AttributeKey<String> INSTANCE_ID_KEY = AttributeKey.stringKey("instance_id");
  public static final AttributeKey<String> LOCATION_ID_KEY = AttributeKey.stringKey("location");
  public static final AttributeKey<String> INSTANCE_CONFIG_ID_KEY =
      AttributeKey.stringKey("instance_config");

  // These metric labels will be promoted to the spanner monitored resource fields
  public static final Set<AttributeKey<String>> SPANNER_PROMOTED_RESOURCE_LABELS =
      ImmutableSet.of(PROJECT_ID_KEY, INSTANCE_ID_KEY, INSTANCE_CONFIG_ID_KEY, LOCATION_ID_KEY);

  public static final AttributeKey<String> DATABASE_KEY = AttributeKey.stringKey("database");
  public static final AttributeKey<String> CLIENT_UID_KEY = AttributeKey.stringKey("client_uid");
  public static final AttributeKey<String> CLIENT_NAME_KEY = AttributeKey.stringKey("client_name");
  public static final AttributeKey<String> METHOD_KEY = AttributeKey.stringKey("method");
  public static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");
  public static final AttributeKey<String> DIRECT_PATH_ENABLED_KEY =
      AttributeKey.stringKey("directpath_enabled");
  public static final AttributeKey<String> DIRECT_PATH_USED_KEY =
      AttributeKey.stringKey("directpath_used");

  public static final Set<AttributeKey> COMMON_ATTRIBUTES =
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
}
