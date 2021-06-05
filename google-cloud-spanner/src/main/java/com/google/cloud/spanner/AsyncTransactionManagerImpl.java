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
import com.google.cloud.spanner.Options.TransactionOption;
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
  private final Options options;

  private TransactionRunnerImpl.TransactionContextImpl txn;
  private TransactionState txnState;
  private final SettableApiFuture<CommitResponse> commitResponse = SettableApiFuture.create();

  AsyncTransactionManagerImpl(SessionImpl session, Span span, TransactionOption... options) {
    this.session = session;
    this.span = span;
    this.options = Options.fromTransactionOptions(options);
  }

  @Override
  public void setSpan(Span span) {
    this.span = span;
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
    if (txn != null) {
      txn.close();
    }
    return MoreObjects.firstNonNull(res, ApiFutures.immediateFuture(null));
  }

  @Override
  public TransactionContextFutureImpl beginAsync() {
    Preconditions.checkState(txn == null, "begin can only be called once");
    return new TransactionContextFutureImpl(this, internalBeginAsync(true));
  }

  private ApiFuture<TransactionContext> internalBeginAsync(boolean firstAttempt) {
    txnState = TransactionState.STARTED;
    txn = session.newTransaction(options);
    if (firstAttempt) {
      session.setActive(this);
    }
    final SettableApiFuture<TransactionContext> res = SettableApiFuture.create();
    final ApiFuture<Void> fut;
    if (firstAttempt) {
      fut = ApiFutures.immediateFuture(null);
    } else {
      fut = txn.ensureTxnAsync();
    }
    ApiFutures.addCallback(
        fut,
        new ApiFutureCallback<Void>() {
          @Override
          public void onFailure(Throwable t) {
            onError(t);
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
    ApiFuture<CommitResponse> commitResponseFuture = txn.commitAsync();
    txnState = TransactionState.COMMITTED;

    ApiFutures.addCallback(
        commitResponseFuture,
        new ApiFutureCallback<CommitResponse>() {
          @Override
          public void onFailure(Throwable t) {
            if (t instanceof AbortedException) {
              txnState = TransactionState.ABORTED;
            } else {
              txnState = TransactionState.COMMIT_FAILED;
              commitResponse.setException(t);
            }
          }

          @Override
          public void onSuccess(CommitResponse result) {
            commitResponse.set(result);
          }
        },
        MoreExecutors.directExecutor());
    return ApiFutures.transform(
        commitResponseFuture, CommitResponse::getCommitTimestamp, MoreExecutors.directExecutor());
  }

  @Override
  public ApiFuture<Void> rollbackAsync() {
    Preconditions.checkState(
        txnState == TransactionState.STARTED,
        "rollback can only be called if the transaction is in progress");
    try {
      return ApiFutures.transformAsync(
          txn.rollbackAsync(),
          ignored -> ApiFutures.immediateFuture(null),
          MoreExecutors.directExecutor());
    } finally {
      txnState = TransactionState.ROLLED_BACK;
    }
  }

  @Override
  public TransactionContextFuture resetForRetryAsync() {
    return new TransactionContextFutureImpl(this, internalBeginAsync(false));
  }

  @Override
  public TransactionState getState() {
    return txnState;
  }

  @Override
  public ApiFuture<CommitResponse> getCommitResponse() {
    return commitResponse;
  }

  @Override
  public void invalidate() {
    if (txnState == TransactionState.STARTED || txnState == null) {
      txnState = TransactionState.ROLLED_BACK;
    }
  }
}
