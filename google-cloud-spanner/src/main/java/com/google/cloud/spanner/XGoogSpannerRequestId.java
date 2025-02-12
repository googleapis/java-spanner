/*
 * Copyright 2025 Google LLC
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

import com.google.api.core.InternalApi;
import com.google.common.annotations.VisibleForTesting;
import java.security.SecureRandom;
import java.util.Objects;

@InternalApi
public class XGoogSpannerRequestId {
  // 1. Generate the random process Id singleton.
  @VisibleForTesting static String RAND_PROCESS_ID = XGoogSpannerRequestId.generateRandProcessId();

  @VisibleForTesting
  static final long VERSION = 1; // The version of the specification being implemented.

  private final long nthClientId;
  private final long nthChannelId;
  private final long nthRequest;
  private long attempt;

  XGoogSpannerRequestId(long nthClientId, long nthChannelId, long nthRequest, long attempt) {
    this.nthClientId = nthClientId;
    this.nthChannelId = nthChannelId;
    this.nthRequest = nthRequest;
    this.attempt = attempt;
  }

  public static XGoogSpannerRequestId of(
      long nthClientId, long nthChannelId, long nthRequest, long attempt) {
    return new XGoogSpannerRequestId(nthClientId, nthChannelId, nthRequest, attempt);
  }

  private static String generateRandProcessId() {
    return String.format("%08x", new SecureRandom().nextInt());
  }

  @Override
  public String toString() {
    return String.format(
        "%d.%s.%d.%d.%d.%d",
        XGoogSpannerRequestId.VERSION,
        XGoogSpannerRequestId.RAND_PROCESS_ID,
        this.nthClientId,
        this.nthChannelId,
        this.nthRequest,
        this.attempt);
  }

  @Override
  public boolean equals(Object other) {
    // instanceof for a null object returns false.
    if (!(other instanceof XGoogSpannerRequestId)) {
      return false;
    }

    XGoogSpannerRequestId otherReqId = (XGoogSpannerRequestId) (other);

    return Objects.equals(this.nthClientId, otherReqId.nthClientId)
        && Objects.equals(this.nthChannelId, otherReqId.nthChannelId)
        && Objects.equals(this.nthRequest, otherReqId.nthRequest)
        && Objects.equals(this.attempt, otherReqId.attempt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.nthClientId, this.nthChannelId, this.nthRequest, this.attempt);
  }
}
