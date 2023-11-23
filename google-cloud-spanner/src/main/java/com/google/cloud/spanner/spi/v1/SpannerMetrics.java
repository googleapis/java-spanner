/*
 * Copyright 2023 Google LLC
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
package com.google.cloud.spanner.spi.v1;

public class SpannerMetrics {

  private static boolean sessionMetricsEnabled = false;
  private static boolean rpcMetricsEnabled = false;

  /** Enable all metrics related to Session Management. */
  public static void enableSessionMetrics() {
    sessionMetricsEnabled = true;
  }

  /** Enable RPC metrics including gfe_latency and gfe_header_missing_count. */
  public static void enableRPCMetrics() {
    rpcMetricsEnabled = true;
  }

  /** Check if session metrics are enabled. */
  public static boolean isSessionMetricsEnabled() {
    return sessionMetricsEnabled;
  }

  /** Check if GFE metrics are enabled. */
  public static boolean isRPCMetricsEnabled() {
    return rpcMetricsEnabled;
  }
}
