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

import com.google.spanner.v1.DirectedReadOptions;

/** Utility methods for Spanner. */
public class SpannerUtil {
  static final int MAX_REPLICA_SELECTIONS_COUNT = 10;

  public static void verifyDirectedReadOptions(DirectedReadOptions directedReadOptions) {
    if (directedReadOptions.hasIncludeReplicas() && directedReadOptions.hasExcludeReplicas()) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          "Only one of include_replicas or exclude_replicas can be set");
    }
    if ((directedReadOptions.hasIncludeReplicas()
            && directedReadOptions.getIncludeReplicas().getReplicaSelectionsCount()
                > MAX_REPLICA_SELECTIONS_COUNT)
        || (directedReadOptions.hasExcludeReplicas()
            && directedReadOptions.getExcludeReplicas().getReplicaSelectionsCount()
                > MAX_REPLICA_SELECTIONS_COUNT)) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          String.format(
              "Maximum length of replica selection allowed in IncludeReplicas/ExcludeReplicas is %d",
              MAX_REPLICA_SELECTIONS_COUNT));
    }
  }

  public static DirectedReadOptions validateAndGetPreferredDirectedReadOptions(
      DirectedReadOptions directedReadOptionsForClient,
      DirectedReadOptions directedReadOptionsForRequest,
      boolean readOnly) {
    if (!readOnly) {
      if ((directedReadOptionsForRequest != null) || (directedReadOptionsForClient != null)) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.FAILED_PRECONDITION,
            "DirectedReadOptions can't be set for Read-Write or Partitioned DML transactions");
      }
    }
    // If DirectedReadOptions is not set at request-level, the request object won't be
    // having DirectedReadOptions field set. Though, if DirectedReadOptions is set at client-level
    // (through SpannerOptions), we must modify the request object to set the DirectedReadOptions
    // proto field to this value.
    if ((directedReadOptionsForRequest == null) && (directedReadOptionsForClient != null)) {
      return directedReadOptionsForClient;
    }
    return directedReadOptionsForRequest;
  }
}
