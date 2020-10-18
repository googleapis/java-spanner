/*
 * Copyright 2017 Google LLC
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
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.SessionImpl.SessionTransaction;
import com.google.cloud.spanner.TransactionContextFutureImpl.CommittableAsyncTransactionManager;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;

/** Implementation of {@link AsyncTransactionManager}. */
final class AsyncTransactionManagerImpl
    implements CommittableAsyncTransactionManager, SessionTransaction {
  private static final Tracer tracer = Tracing.getTracer();

  private final SessionImpl session;
  private Span span;
  private boolean returnCommitStats;

  private TransactionRunnerImpl.TransactionContextImpl txn;
  private TransactionState txnState;
  private final SettableApiFuture<Timestamp> commitTimestamp = SettableApiFuture.create();
  private SettableApiFuture<CommitStats> commitStats;

  AsyncTransactionManagerImpl(SessionImpl session, Span span) {
    this.session = session;
    this.span = span;
  }

  @Override
  public void setSpan(Span span) {
    this.span = span;
  }

  @Override
  public AsyncTransactionManager withCommitStats() {
    this.returnCommitStats = true;
    return this;
  }

  @Override
  public ApiFuture<CommitStats> getCommitStats() {
    Preconditions.checkState(
        txnState == TransactionState.COMMITTED,
        "getCommitStats can only be invoked if the transaction committed successfully");
    Preconditions.checkState(
        returnCommitStats,
        "getCommitStats can only be invoked if withCommitStats() was invoked before committing the transaction");
    return commitStats;
  }

  @Override
  public void close() {
    closeAsync();
  }

  @Override
  public ApiFuture<Void> closeAsync() {
    ApiFuture<Void> res = null;
    if (txnState == TransactionState.STARTED) {
      res = rollbackAsync();
    }
    txn.close();
    return MoreObjects.firstNonNull(res, ApiFutures.<Void>immediateFuture(null));
  }

  @Override
  public TransactionContextFutureImpl beginAsync() {
    Preconditions.checkState(txn == null, "begin can only be called once");
    TransactionContextFutureImpl begin =
        new TransactionContextFutureImpl(this, internalBeginAsync(true));
    return begin;
  }

  private ApiFuture<TransactionContext> internalBeginAsync(boolean setActive) {
    txnState = TransactionState.STARTED;
    txn = session.newTransaction();
    if (setActive) {
      session.setActive(this);
    }
    final SettableApiFuture<TransactionContext> res = SettableApiFuture.create();
    final ApiFuture<Void> fut = txn.ensureTxnAsync();
    ApiFutures.addCallback(
        fut,
        new ApiFutureCallback<Void>() {
          @Override
          public void onFailure(Throwable t) {
            res.setException(SpannerExceptionFactory.newSpannerException(t));
          }

          @Override
          public void onSuccess(Void result) {
            res.set(txn);
          }
        },
        MoreExecutors.directExecutor());
    return res;
  }

  @Override
  public void onError(Throwable t) {
    if (t instanceof AbortedException) {
      txnState = TransactionState.ABORTED;
    }
  }

  @Override
  public ApiFuture<Timestamp> commitAsync() {
    Preconditions.checkState(
        txnState == TransactionState.STARTED,
        "commit can only be invoked if the transaction is in progress. Current state: " + txnState);
    if (txn.isAborted()) {
      txnState = TransactionState.ABORTED;
      return ApiFutures.immediateFailedFuture(
          SpannerExceptionFactory.newSpannerException(
              ErrorCode.ABORTED, "Transaction already aborted"));
    }
    ApiFuture<Timestamp> res = txn.commitAsync(returnCommitStats);
    if (returnCommitStats) {
      commitStats = SettableApiFuture.create();
    }
    txnState = TransactionState.COMMITTED;
    ApiFutures.addCallback(
        res,
        new ApiFutureCallback<Timestamp>() {
          @Override
          public void onFailure(Throwable t) {
            if (t instanceof AbortedException) {
              txnState = TransactionState.ABORTED;
            } else {
              txnState = TransactionState.COMMIT_FAILED;
              commitTimestamp.setException(t);
              commitStats.setException(t);
            }
          }

          @Override
          public void onSuccess(Timestamp result) {
            commitTimestamp.set(result);
            commitStats.set(txn.commitStats());
          }
        },
        MoreExecutors.directExecutor());
    return res;
  }

  @Override
  public ApiFuture<Void> rollbackAsync() {
    Preconditions.checkState(
        txnState == TransactionState.STARTED,
        "rollback can only be called if the transaction is in progress");
    try {
      return txn.rollbackAsync();
    } finally {
      txnState = TransactionState.ROLLED_BACK;
    }
  }

  @Override
  public TransactionContextFuture resetForRetryAsync() {
    if (txn == null || !txn.isAborted() && txnState != TransactionState.ABORTED) {
      throw new IllegalStateException(
          "resetForRetry can only be called if the previous attempt aborted");
    }
    return new TransactionContextFutureImpl(this, internalBeginAsync(false));
  }

  @Override
  public TransactionState getState() {
    return txnState;
  }

  @Override
  public void invalidate() {
    if (txnState == TransactionState.STARTED || txnState == null) {
      txnState = TransactionState.ROLLED_BACK;
    }
  }
}
