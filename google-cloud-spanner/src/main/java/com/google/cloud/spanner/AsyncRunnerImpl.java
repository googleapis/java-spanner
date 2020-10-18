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

import com.google.api.core.ApiFuture;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

class AsyncRunnerImpl implements AsyncRunner {
  private final TransactionRunnerImpl delegate;
  private final SettableApiFuture<Timestamp> commitTimestamp = SettableApiFuture.create();
  private final SettableApiFuture<CommitStats> commitStats = SettableApiFuture.create();
  private boolean returnCommitStats;

  AsyncRunnerImpl(TransactionRunnerImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public <R> ApiFuture<R> runAsync(final AsyncWork<R> work, Executor executor) {
    final SettableApiFuture<R> res = SettableApiFuture.create();
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            try {
              res.set(runTransaction(work));
            } catch (Throwable t) {
              res.setException(t);
            } finally {
              setCommitTimestampAndStats();
            }
          }
        });
    return res;
  }

  private <R> R runTransaction(final AsyncWork<R> work) {
    return delegate.run(
        new TransactionCallable<R>() {
          @Override
          public R run(TransactionContext transaction) throws Exception {
            try {
              return work.doWorkAsync(transaction).get();
            } catch (ExecutionException e) {
              throw SpannerExceptionFactory.newSpannerException(e.getCause());
            } catch (InterruptedException e) {
              throw SpannerExceptionFactory.propagateInterrupt(e);
            }
          }
        });
  }

  private void setCommitTimestampAndStats() {
    try {
      commitTimestamp.set(delegate.getCommitTimestamp());
    } catch (Throwable t) {
      commitTimestamp.setException(t);
    }
    if (returnCommitStats) {
      try {
        commitStats.set(delegate.getCommitStats());
      } catch (Throwable t) {
        commitStats.setException(t);
      }
    }
  }

  @Override
  public ApiFuture<Timestamp> getCommitTimestamp() {
    return commitTimestamp;
  }

  @Override
  public AsyncRunner withCommitStats() {
    delegate.withCommitStats();
    returnCommitStats = true;
    return this;
  }

  @Override
  public ApiFuture<CommitStats> getCommitStats() {
    Preconditions.checkState(
        returnCommitStats,
        "getCommitStats may only be invoked if withCommitStats has been invoked before executing the transaction");
    return commitStats;
  }
}
