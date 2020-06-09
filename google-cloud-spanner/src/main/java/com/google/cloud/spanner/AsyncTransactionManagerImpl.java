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

import java.util.concurrent.ExecutionException;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.SessionImpl.SessionTransaction;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;

/** Implementation of {@link AsyncTransactionManager}. */
final class AsyncTransactionManagerImpl implements AsyncTransactionManager, SessionTransaction {
  private static final Tracer tracer = Tracing.getTracer();

  private final SessionImpl session;
  private Span span;

  private TransactionRunnerImpl.TransactionContextImpl txn;
  private TransactionState txnState;

  AsyncTransactionManagerImpl(SessionImpl session, Span span) {
    this.session = session;
    this.span = span;
  }

  @Override
  public void setSpan(Span span) {
    this.span = span;
  }

  @Override
  public ApiFuture<? extends TransactionContext> beginAsync() {
    Preconditions.checkState(txn == null, "begin can only be called once");
    txnState = TransactionState.STARTED;
    txn = session.newTransaction();
    session.setActive(this);
    final SettableApiFuture<TransactionContext> res = SettableApiFuture.create();
    final ApiFuture<Void> fut = txn.ensureTxnAsync();
    fut.addListener(tracer.withSpan(span, new Runnable(){
      @Override
      public void run() {
        try {
          fut.get();
          res.set(txn);
        } catch (ExecutionException e) {
          res.setException(e.getCause() == null ? e : e.getCause());
        } catch (InterruptedException e) {
          res.setException(SpannerExceptionFactory.propagateInterrupt(e));
        }
      }
    }), MoreExecutors.directExecutor());
    return res;
  }

  @Override
  public ApiFuture<Void> commitAsync() {
    Preconditions.checkState(
        txnState == TransactionState.STARTED,
        "commit can only be invoked if the transaction is in progress");
    SettableApiFuture<Void> res = SettableApiFuture.create();
    if (txn.isAborted()) {
      txnState = TransactionState.ABORTED;
      res.setException(SpannerExceptionFactory.newSpannerException(
          ErrorCode.ABORTED, "Transaction already aborted"));
    }
    try {
      txn.commit();
      txnState = TransactionState.COMMITTED;
      return ApiFutures.immediateFuture(null);
    } catch (AbortedException e1) {
      txnState = TransactionState.ABORTED;
      return ApiFutures.immediateFailedFuture(e1);
    } catch (SpannerException e2) {
      txnState = TransactionState.COMMIT_FAILED;
      return ApiFutures.immediateFailedFuture(e2);
    }
  }

  @Override
  public ApiFuture<Void> rollbackAsync() {
    Preconditions.checkState(
        txnState == TransactionState.STARTED,
        "rollback can only be called if the transaction is in progress");
    try {
      txn.rollback();
    } finally {
      txnState = TransactionState.ROLLED_BACK;
    }
    return ApiFutures.immediateFuture(null);
  }

  @Override
  public ApiFuture<? extends TransactionContext> resetForRetryAsync() {
    if (txn == null || !txn.isAborted() && txnState != TransactionState.ABORTED) {
      throw new IllegalStateException(
          "resetForRetry can only be called if the previous attempt" + " aborted");
    }
    try (Scope s = tracer.withSpan(span)) {
      txn = session.newTransaction();
      txn.ensureTxn();
      txnState = TransactionState.STARTED;
      return ApiFutures.immediateFuture(txn);
    }
  }

  @Override
  public ApiFuture<Timestamp> getCommitTimestampAsync() {
    Preconditions.checkState(
        txnState == TransactionState.COMMITTED,
        "getCommitTimestamp can only be invoked if the transaction committed successfully");
    return ApiFutures.immediateFuture(txn.commitTimestamp());
  }

  @Override
  public void close() {
    try {
      if (txnState == TransactionState.STARTED && !txn.isAborted()) {
        txn.rollback();
        txnState = TransactionState.ROLLED_BACK;
      }
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  @Override
  public TransactionState getState() {
    return txnState;
  }

  @Override
  public void invalidate() {
    close();
  }
}
