/*
 * Copyright 2020 Google LLC
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

import com.google.common.base.Preconditions;
import org.threeten.bp.Duration;
import org.threeten.bp.temporal.ChronoUnit;

/**
 * Commit statistics are returned by a read/write transaction if specifically requested.
 *
 * <p>Usage: {@link TransactionRunner#withCommitStats()}
 */
public class CommitStats {
  private final long mutationCount;
  private final Duration overloadDelay;

  private CommitStats(long mutationCount, Duration overloadDelay) {
    this.mutationCount = mutationCount;
    this.overloadDelay = overloadDelay;
  }

  static CommitStats fromProto(com.google.spanner.v1.CommitResponse.CommitStats proto) {
    Preconditions.checkNotNull(proto);
    return new CommitStats(
        proto.getMutationCount(),
        Duration.of(proto.getOverloadDelay().getSeconds(), ChronoUnit.SECONDS)
            .plusNanos(proto.getOverloadDelay().getNanos()));
  }

  public long getMutationCount() {
    return mutationCount;
  }

  public Duration getOverloadDelay() {
    return overloadDelay;
  }
}
