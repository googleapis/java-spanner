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

package com.google.cloud.spanner.spi.v1;

import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import org.threeten.bp.Duration;

/** Configuration for RPCs that use a custom retry strategy of retrying on a new gRPC channel. */
class RetryOnNewChannelConfiguration {
  private final Boolean retryBeginTransactionOnNewChannel =
      Boolean.parseBoolean(
          System.getProperty(
              "com.google.cloud.spanner.retry_begin_transaction_on_new_channel", "false"));
  private final int retryDelayBeginTransactionOnNewChannel =
      getIntProperty("com.google.cloud.spanner.retry_delay_begin_transaction_on_new_channel", 5);
  // This is the maximum number of times that the custom retry logic will attempt to execute the
  // RPC.
  private final int maxAttemptsBeginTransactionOnNewChannel =
      getIntProperty("com.google.cloud.spanner.max_attempts_begin_transaction_on_new_channel", 4);

  // This is the number of times that Gax will retry the RPC on a given channel when retrying on a
  // new channel is enabled.
  private final int maxRpcAttemptsBeginTransactionOnNewChannel =
      getIntProperty(
          "com.google.cloud.spanner.max_rpc_attempts_begin_transaction_on_new_channel", 2);
  // This is the initial RPC timeout that is used by Gax when retrying on a new channel is enabled.
  private final int initialRpcTimeoutBeginTransactionOnNewChannel =
      getIntProperty(
          "com.google.cloud.spanner.initial_rpc_timeout_begin_transaction_on_new_channel", 500);
  // This is the max RPC timeout that is used by Gax when retrying on a new channel is enabled.
  private final int maxRpcTimeoutBeginTransactionOnNewChannel =
      getIntProperty(
          "com.google.cloud.spanner.max_rpc_timeout_begin_transaction_on_new_channel", 500);

  RetryOnNewChannelConfiguration(SpannerStubSettings.Builder spannerStubSettingsBuilder) {
    if (isRetryBeginTransactionOnNewChannel()) {
      setBeginTransactionOnNewChannelRetrySettings(spannerStubSettingsBuilder);
    }
  }

  boolean isRetryBeginTransactionOnNewChannel() {
    return retryBeginTransactionOnNewChannel;
  }

  int getMaxAttemptsBeginTransactionOnNewChannel() {
    return maxAttemptsBeginTransactionOnNewChannel;
  }

  int getMaxRpcAttemptsBeginTransactionOnNewChannel() {
    return maxRpcAttemptsBeginTransactionOnNewChannel;
  }

  int getInitialRpcTimeoutBeginTransactionOnNewChannel() {
    return initialRpcTimeoutBeginTransactionOnNewChannel;
  }

  int getMaxRpcTimeoutBeginTransactionOnNewChannel() {
    return maxRpcTimeoutBeginTransactionOnNewChannel;
  }

  /**
   * Sets the retry settings for the BeginTransaction RPC when {@link
   * #isRetryBeginTransactionOnNewChannel()} is true.
   */
  private void setBeginTransactionOnNewChannelRetrySettings(
      SpannerStubSettings.Builder spannerStubSettingsBuilder) {
    // These are the retry settings that are used for the BeginTransaction RPC by Gax.
    spannerStubSettingsBuilder
        .beginTransactionSettings()
        .setRetryableCodes(StatusCode.Code.UNAVAILABLE)
        .setRetrySettings(
            RetrySettings.newBuilder()
                .setInitialRetryDelay(
                    Duration.ofMillis(getRetryDelayBeginTransactionOnNewChannel()))
                .setMaxRetryDelay(Duration.ofMillis(getRetryDelayBeginTransactionOnNewChannel()))
                .setRetryDelayMultiplier(1.0)
                .setInitialRpcTimeout(
                    Duration.ofMillis(getInitialRpcTimeoutBeginTransactionOnNewChannel()))
                .setMaxRpcTimeout(Duration.ofMillis(getMaxRpcTimeoutBeginTransactionOnNewChannel()))
                .setRpcTimeoutMultiplier(1.0)
                .setMaxAttempts(getMaxRpcAttemptsBeginTransactionOnNewChannel())
                .setTotalTimeout(Duration.ofSeconds(30))
                .build());
  }

  int getRetryDelayBeginTransactionOnNewChannel() {
    return retryDelayBeginTransactionOnNewChannel;
  }

  private static int getIntProperty(String name, int defaultValue) {
    try {
      return Integer.parseInt(System.getProperty(name, String.valueOf(defaultValue)));
    } catch (Throwable ignore) {
      return defaultValue;
    }
  }
}
