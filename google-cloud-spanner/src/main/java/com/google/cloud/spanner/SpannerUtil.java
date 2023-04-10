package com.google.cloud.spanner;

import com.google.spanner.v1.DirectedReadOptions;

/** Utility methods for Spanner. */
class SpannerUtil {
  static final int MAX_REPLICA_SELECTIONS_COUNT = 10;

  static void verifyDirectedReadOptions(DirectedReadOptions directedReadOptions) {
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
}
