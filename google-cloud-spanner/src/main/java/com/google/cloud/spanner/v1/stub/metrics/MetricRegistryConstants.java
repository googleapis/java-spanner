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
package com.google.cloud.spanner.v1.stub.metrics;

import com.google.common.collect.ImmutableList;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;

/** A helper class that holds OpenCensus's related constants. */
public class MetricRegistryConstants {

  // The label keys are used to uniquely identify timeseries.
  private static final LabelKey DATABASE = LabelKey.create("database", "Target database");
  private static final LabelKey INSTANCE_ID =
      LabelKey.create("instance_id", "Name of the instance");
  private static final LabelKey LIBRARY_VERSION =
      LabelKey.create("library_version", "Library version");

  /** The label value is used to represent missing value. */
  private static final LabelValue UNSET_LABEL = LabelValue.create(null);

  public static final ImmutableList<LabelKey> SPANNER_LABEL_KEYS =
      ImmutableList.of(DATABASE, INSTANCE_ID, LIBRARY_VERSION);

  public static final ImmutableList<LabelValue> SPANNER_DEFAULT_LABEL_VALUES =
      ImmutableList.of(UNSET_LABEL, UNSET_LABEL, UNSET_LABEL);

  /** Unit to represent counts. */
  public static final String COUNT = "1";

  // The Metric name and description
  public static final String ACTIVE_SESSIONS = "cloud.google.com/java/spanner/active_sessions";
  public static final String MAX_SESSIONS = "cloud.google.com/java/spanner/max_sessions";
  public static final String SESSIONS_IN_USE = "cloud.google.com/java/spanner/sessions_in_use";
  public static final String ACTIVE_SESSIONS_DESCRIPTION =
      "Max number of sessions in use during the last 10 minutes";
  public static final String MAX_SESSIONS_DESCRIPTION = "The number of max sessions configured";
  public static final String SESSIONS_IN_USE_DESCRIPTION =
      "The number of sessions checked out from the pool";
}
