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

import com.google.api.core.NanoClock;
import com.google.api.gax.retrying.ResultRetryAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.TimedAttemptSettings;
import com.google.cloud.RetryHelper;
import com.google.cloud.RetryHelper.RetryHelperException;
import com.google.cloud.spanner.v1.stub.SpannerStub;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.base.Throwables;
import com.google.spanner.v1.RollbackRequest;
import io.grpc.Context;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import org.threeten.bp.Duration;

/**
 * Util class for retrying aborted transactions. This class is a wrapper around {@link RetryHelper}
 * that uses specific settings to only retry on aborted transactions, without a timeout and without
 * a cap on the number of retries.
 */
class SpannerRetryHelper {

  /**
   * Use the same {@link RetrySettings} for retrying an aborted transaction as for retrying a {@link
   * RollbackRequest}. The {@link RollbackRequest} automatically uses the default retry settings
   * defined for the {@link SpannerStub}. By referencing these settings, the retry settings for
   * retrying aborted transactions will also automatically be updated if the default retry settings
   * are updated.
   *
   * <p>These default {@link RetrySettings} are only used if no retry information is returned by the
   * {@link AbortedException}.
   */
  private static final RetrySettings txRetrySettings =
      SpannerStubSettings.newBuilder().rollbackSettings().getRetrySettings();

  /** Executes the {@link Callable} and retries if it fails with an {@link AbortedException}. */
  static <T> T runTxWithRetriesOnAborted(Callable<T> callable) {
    return runTxWithRetriesOnAborted(callable, txRetrySettings);
  }

  /**
   * Executes the {@link Callable} and retries if it fails with an {@link AbortedException} using
   * the specific {@link RetrySettings}.
   */
  static <T> T runTxWithRetriesOnAborted(Callable<T> callable, RetrySettings retrySettings) {
    try {
      return RetryHelper.runWithRetries(
          callable, retrySettings, new TxRetryAlgorithm<>(), NanoClock.getDefaultClock());
    } catch (RetryHelperException e) {
      if (e.getCause() != null) {
        Throwables.throwIfUnchecked(e.getCause());
      }
      throw e;
    }
  }

  private static class TxRetryAlgorithm<T> implements ResultRetryAlgorithm<T> {
    @Override
    public TimedAttemptSettings createNextAttempt(
        Throwable prevThrowable, T prevResponse, TimedAttemptSettings prevSettings) {
      if (prevThrowable != null) {
        long retryDelay = SpannerException.extractRetryDelay(prevThrowable);
        if (retryDelay > -1L) {
          return prevSettings
              .toBuilder()
              .setRandomizedRetryDelay(Duration.ofMillis(retryDelay))
              .build();
        }
      }
      return null;
    }

    @Override
    public boolean shouldRetry(Throwable prevThrowable, T prevResponse)
        throws CancellationException {
      if (Context.current().isCancelled()) {
        throw SpannerExceptionFactory.newSpannerExceptionForCancellation(Context.current(), null);
      }
      return prevThrowable != null
          && (prevThrowable instanceof AbortedException
              || prevThrowable instanceof com.google.api.gax.rpc.AbortedException);
    }
  }
}
