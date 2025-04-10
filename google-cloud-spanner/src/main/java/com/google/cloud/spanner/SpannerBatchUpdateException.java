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

package com.google.cloud.spanner;

public class SpannerBatchUpdateException extends SpannerException {
  private final long[] updateCounts;

  /** Private constructor. Use {@link SpannerExceptionFactory} to create instances. */
  SpannerBatchUpdateException(
      DoNotConstructDirectly token,
      ErrorCode code,
      String message,
      long[] counts,
      Throwable cause,
      XGoogSpannerRequestId reqId) {
    super(token, code, false, message, cause, null, reqId);
    updateCounts = counts;
  }

  /** Returns the number of rows affected by each statement that is successfully run. */
  public long[] getUpdateCounts() {
    return updateCounts;
  }
}
