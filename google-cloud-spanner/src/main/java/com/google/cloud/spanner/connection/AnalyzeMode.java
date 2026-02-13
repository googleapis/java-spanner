/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.connection;

import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;

/**
 * {@link AnalyzeMode} controls the execution and returned information for a query:
 *
 * <ul>
 *   <li>{@code NONE}: The default mode. Only the statement results are returned.
 *   <li>{@code PLAN}: Returns only the query plan, without any results or execution statistics
 *       information.
 *   <li>{@code PROFILE}: Returns the query plan, overall execution statistics, operator-level
 *       execution statistics along with the results. This mode has a performance overhead and is
 *       not recommended for production traffic.
 *   <li>{@code WITH_STATS}: Returns the overall (but not operator-level) execution statistics along
 *       with the results.
 *   <li>{@code WITH_PLAN_AND_STATS}: Returns the query plan, overall (but not operator-level)
 *       execution statistics along with the results.
 * </ul>
 */
enum AnalyzeMode {
  NONE(null),
  PLAN(QueryAnalyzeMode.PLAN),
  PROFILE(QueryAnalyzeMode.PROFILE),
  WITH_STATS(QueryAnalyzeMode.WITH_STATS),
  WITH_PLAN_AND_STATS(QueryAnalyzeMode.WITH_PLAN_AND_STATS);

  private final QueryAnalyzeMode mode;

  AnalyzeMode(QueryAnalyzeMode mode) {
    this.mode = mode;
  }

  QueryAnalyzeMode getQueryAnalyzeMode() {
    return mode;
  }

  /** Translates from the Spanner client library QueryAnalyzeMode to {@link AnalyzeMode}. */
  static AnalyzeMode of(QueryAnalyzeMode mode) {
    switch (mode) {
      case PLAN:
        return AnalyzeMode.PLAN;
      case PROFILE:
        return AnalyzeMode.PROFILE;
      case WITH_STATS:
        return AnalyzeMode.WITH_STATS;
      case WITH_PLAN_AND_STATS:
        return AnalyzeMode.WITH_PLAN_AND_STATS;
      default:
        throw new IllegalArgumentException(mode + " is unknown");
    }
  }
}
