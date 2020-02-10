/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.spanner;

import com.google.common.collect.ImmutableList;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;

/** A helper class that holds OpenCensus's related constants. */
class MetricRegistryConstants {

  // The label keys are used to uniquely identify timeseries.
  private static final LabelKey DATABASE = LabelKey.create("database", "Target database");
  private static final LabelKey INSTANCE_ID =
      LabelKey.create("instance_id", "Name of the instance");
  private static final LabelKey LIBRARY_VERSION =
      LabelKey.create("library_version", "Library version");

  /** The label value is used to represent missing value. */
  private static final LabelValue UNSET_LABEL = LabelValue.create(null);

  static final ImmutableList<LabelKey> SPANNER_LABEL_KEYS =
      ImmutableList.of(DATABASE, INSTANCE_ID, LIBRARY_VERSION);

  static final ImmutableList<LabelValue> SPANNER_DEFAULT_LABEL_VALUES =
      ImmutableList.of(UNSET_LABEL, UNSET_LABEL, UNSET_LABEL);

  /** Unit to represent counts. */
  static final String COUNT = "1";

  // The Metric name and description
  static final String MAX_IN_USE_SESSIONS = "cloud.google.com/java/spanner/max_in_use_sessions";
  static final String MAX_ALLOWED_SESSIONS = "cloud.google.com/java/spanner/max_allowed_sessions";
  static final String IN_USE_SESSIONS = "cloud.google.com/java/spanner/in_use_sessions";
  static final String GET_SESSION_TIMEOUTS = "cloud.google.com/java/spanner/get_sessions_timeouts";

  static final String MAX_IN_USE_SESSIONS_DESCRIPTION =
      "The maximum number of sessions in use during the last 10 minute interval.";
  static final String MAX_ALLOWED_SESSIONS_DESCRIPTION =
      "The maximum number of sessions allowed. Configurable by the user.";
  static final String IN_USE_SESSIONS_DESCRIPTION = "The number of sessions currently in use.";
  static final String SESSIONS_TIMEOUTS_DESCRIPTION =
      "The number of get sessions timeouts due to pool exhaustion";
}
