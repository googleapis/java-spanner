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
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.SessionNotFoundHandler;
import com.google.cloud.spanner.TransactionContextFutureImpl.CommittableAsyncTransactionManager;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import javax.annotation.concurrent.GuardedBy;

class SessionPoolAsyncTransactionManager
    implements CommittableAsyncTransactionManager, SessionNotFoundHandler {
  private final Object lock = new Object();

  @GuardedBy("lock")
  private TransactionState txnState;

  @GuardedBy("lock")
  private AbortedException abortedException;

  private final SessionPool pool;
  private final TransactionOption[] options;
  private volatile PooledSessionFuture session;
  private volatile SettableApiFuture<AsyncTransactionManagerImpl> delegate;
  private boolean restartedAfterSessionNotFound;

  SessionPoolAsyncTransactionManager(
      SessionPool pool, PooledSessionFuture session, TransactionOption... options) {
    this.pool = Preconditions.checkNotNull(pool);
    this.options = options;
    createTransaction(session);
  }

  private void createTransaction(PooledSessionFuture session) {
    this.session = session;
    this.delegate = SettableApiFuture.create();
    this.session.addListener(
        () -> {
          try {
            delegate.set(
                SessionPoolAsyncTransactionManager.this
                    .session
                    .get()
                    .transactionManagerAsync(options));
          } catch (Throwable t) {
            delegate.setException(t);
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public SpannerException handleSessionNotFound(SessionNotFoundException notFound) {
    // Restart the entire transaction with a new session and throw an AbortedException to force the
    // client application to retry.
    createTransaction(pool.replaceSession(notFound, session));
    restartedAfterSessionNotFound = true;
    return SpannerExceptionFactory.newSpannerException(
        ErrorCode.ABORTED, notFound.getMessage(), notFound);
  }

  @Override
  public void close() {
    SpannerApiFutures.get(closeAsync());
  }

  @Override
  public ApiFuture<Void> closeAsync() {
    final SettableApiFuture<Void> res = SettableApiFuture.create();
    ApiFutures.addCallback(
        delegate,
        new ApiFutureCallback<AsyncTransactionManagerImpl>() {
          @Override
          public void onFailure(Throwable t) {
            session.close();
          }

          @Override
          public void onSuccess(AsyncTransactionManagerImpl result) {
            ApiFutures.addCallback(
                result.closeAsync(),
                new ApiFutureCallback<Void>() {
                  @Override
                  public void onFailure(Throwable t) {
                    res.setException(t);
                  }

                  @Override
                  public void onSuccess(Void result) {
                    session.close();
                    res.set(result);
                  }
                },
                MoreExecutors.directExecutor());
          }
        },
        MoreExecutors.directExecutor());
    return res;
  }

  @Override
  public TransactionContextFuture beginAsync() {
    synchronized (lock) {
      Preconditions.checkState(txnState == null, "begin can only be called once");
      txnState = TransactionState.STARTED;
    }
    final SettableApiFuture<TransactionContext> delegateTxnFuture = SettableApiFuture.create();
    ApiFutures.addCallback(
        delegate,
        new ApiFutureCallback<AsyncTransactionManagerImpl>() {
          @Override
          public void onFailure(Throwable t) {
            delegateTxnFuture.setException(t);
          }

          @Override
          public void onSuccess(AsyncTransactionManagerImpl result) {
            ApiFutures.addCallback(
                result.beginAsync(),
                new ApiFutureCallback<TransactionContext>() {
                  @Override
                  public void onFailure(Throwable t) {
                    delegateTxnFuture.setException(t);
                  }

                  @Override
                  public void onSuccess(TransactionContext result) {
                    delegateTxnFuture.set(
                        new SessionPool.SessionPoolTransactionContext(
                            SessionPoolAsyncTransactionManager.this, result));
                  }
                },
                MoreExecutors.directExecutor());
          }
        },
        MoreExecutors.directExecutor());
    return new TransactionContextFutureImpl(this, delegateTxnFuture);
  }

  @Override
  public void onError(Throwable t) {
    if (t instanceof AbortedException) {
      synchronized (lock) {
        txnState = TransactionState.ABORTED;
        abortedException = (AbortedException) t;
      }
    }
  }

  @Override
  public ApiFuture<Timestamp> commitAsync() {
    synchronized (lock) {
      Preconditions.checkState(
          txnState == TransactionState.STARTED || txnState == TransactionState.ABORTED,
          "commit can only be invoked if the transaction is in progress. Current state: "
              + txnState);
      if (txnState == TransactionState.ABORTED) {
        return ApiFutures.immediateFailedFuture(abortedException);
      }
      txnState = TransactionState.COMMITTED;
    }
    return ApiFutures.transformAsync(
        delegate,
        input -> {
          final SettableApiFuture<Timestamp> res = SettableApiFuture.create();
          ApiFutures.addCallback(
              input.commitAsync(),
              new ApiFutureCallback<Timestamp>() {
                @Override
                public void onFailure(Throwable t) {
                  synchronized (lock) {
                    if (t instanceof AbortedException) {
                      txnState = TransactionState.ABORTED;
                      abortedException = (AbortedException) t;
                    } else {
                      txnState = TransactionState.COMMIT_FAILED;
                    }
                  }
                  res.setException(t);
                }

                @Override
                public void onSuccess(Timestamp result) {
                  res.set(result);
                }
              },
              MoreExecutors.directExecutor());
          return res;
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public ApiFuture<Void> rollbackAsync() {
    synchronized (lock) {
      Preconditions.checkState(
          txnState == TransactionState.STARTED,
          "rollback can only be called if the transaction is in progress");
      txnState = TransactionState.ROLLED_BACK;
    }
    return ApiFutures.transformAsync(
        delegate,
        input -> {
          ApiFuture<Void> res = input.rollbackAsync();
          res.addListener(() -> session.close(), MoreExecutors.directExecutor());
          return res;
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public TransactionContextFuture resetForRetryAsync() {
    synchronized (lock) {
      Preconditions.checkState(
          txnState == TransactionState.ABORTED || restartedAfterSessionNotFound,
          "resetForRetry can only be called after the transaction aborted.");
      txnState = TransactionState.STARTED;
    }
    return new TransactionContextFutureImpl(
        this,
        ApiFutures.transform(
            ApiFutures.transformAsync(
                delegate,
                input -> {
                  if (restartedAfterSessionNotFound) {
                    restartedAfterSessionNotFound = false;
                    return input.beginAsync();
                  }
                  return input.resetForRetryAsync();
                },
                MoreExecutors.directExecutor()),
            input ->
                new SessionPool.SessionPoolTransactionContext(
                    SessionPoolAsyncTransactionManager.this, input),
            MoreExecutors.directExecutor()));
  }

  @Override
  public TransactionState getState() {
    synchronized (lock) {
      return txnState;
    }
  }

  public ApiFuture<CommitResponse> getCommitResponse() {
    synchronized (lock) {
      Preconditions.checkState(
          txnState == TransactionState.COMMITTED,
          "commit can only be invoked if the transaction was successfully committed");
    }
    return ApiFutures.transformAsync(
        delegate, AsyncTransactionManagerImpl::getCommitResponse, MoreExecutors.directExecutor());
  }
}
