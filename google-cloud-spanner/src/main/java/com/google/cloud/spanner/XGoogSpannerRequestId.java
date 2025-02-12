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

import java.security.SecureRandom;

public class XGoogSpannerRequestId {
  // 1. Generate the random process Id singleton.
  public static String RAND_PROCESS_ID = XGoogSpannerRequestId.generateRandProcessId();
  public static long VERSION = 1; // The version of the specification being implemented.
  private long nthClientId;
  private long nthChannelId;
  private long nthRequest;
  private long attempt;

  public XGoogSpannerRequestId(long nthClientId, long nthChannelId, long nthRequest, long attempt) {
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
    byte[] rBytes = new byte[8];
    SecureRandom srng = new SecureRandom();
    srng.nextBytes(rBytes);
    int result =
        Byte.toUnsignedInt(rBytes[0])
            | Byte.toUnsignedInt(rBytes[1]) << 8
            | Byte.toUnsignedInt(rBytes[2]) << 16
            | Byte.toUnsignedInt(rBytes[3]) << 24;

    return Integer.toHexString(result);
  }

  public String toString() {
    return String.format(
        "%d.%s.%d.%d.%d.%d",
        this.VERSION,
        this.RAND_PROCESS_ID,
        this.nthClientId,
        this.nthChannelId,
        this.nthRequest,
        this.attempt);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (!(other instanceof XGoogSpannerRequestId)) {
      return false;
    }

    XGoogSpannerRequestId otherReqId = (XGoogSpannerRequestId) (other);
    return otherReqId.toString().equals(this.toString());
  }
}
