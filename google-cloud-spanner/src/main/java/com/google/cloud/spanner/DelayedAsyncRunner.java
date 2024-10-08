/*
 * Copyright 2024 Google LLC
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

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Represents a {@link AsyncRunner} using a multiplexed session that is not yet ready. The execution
 * will be delayed until the multiplexed session has been created and is ready. This class is only
 * used during the startup of the client and the multiplexed session has not yet been created.
 */
public class DelayedAsyncRunner implements AsyncRunner {

  private final ApiFuture<AsyncRunner> asyncRunnerFuture;

  public DelayedAsyncRunner(ApiFuture<AsyncRunner> asyncRunnerFuture) {
    this.asyncRunnerFuture = asyncRunnerFuture;
  }

  AsyncRunner getAsyncRunner() {
    try {
      return this.asyncRunnerFuture.get();
    } catch (ExecutionException executionException) {
      // Propagate the underlying exception as a RuntimeException (SpannerException is also a
      // RuntimeException).
      if (executionException.getCause() instanceof RuntimeException) {
        throw (RuntimeException) executionException.getCause();
      }
      throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
    } catch (InterruptedException interruptedException) {
      throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
    }
  }

  @Override
  public <R> ApiFuture<R> runAsync(AsyncWork<R> work, Executor executor) {
    return getAsyncRunner().runAsync(work, executor);
  }

  @Override
  public ApiFuture<Timestamp> getCommitTimestamp() {
    return getAsyncRunner().getCommitTimestamp();
  }

  @Override
  public ApiFuture<CommitResponse> getCommitResponse() {
    return getAsyncRunner().getCommitResponse();
  }
}
