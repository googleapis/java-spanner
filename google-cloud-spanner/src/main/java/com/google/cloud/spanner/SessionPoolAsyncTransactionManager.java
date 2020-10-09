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

import com.google.api.core.ApiAsyncFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.TransactionContextFutureImpl.CommittableAsyncTransactionManager;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import javax.annotation.concurrent.GuardedBy;

class SessionPoolAsyncTransactionManager implements CommittableAsyncTransactionManager {
  private final Object lock = new Object();

  @GuardedBy("lock")
  private TransactionState txnState;

  private volatile PooledSessionFuture session;
  private final SettableApiFuture<AsyncTransactionManagerImpl> delegate =
      SettableApiFuture.create();

  SessionPoolAsyncTransactionManager(PooledSessionFuture session) {
    this.session = session;
    this.session.addListener(
        new Runnable() {
          @Override
          public void run() {
            try {
              delegate.set(
                  SessionPoolAsyncTransactionManager.this.session.get().transactionManagerAsync());
            } catch (Throwable t) {
              delegate.setException(t);
            }
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public void close() {
    ApiFutures.addCallback(
        delegate,
        new ApiFutureCallback<AsyncTransactionManagerImpl>() {
          @Override
          public void onFailure(Throwable t) {
            session.close();
          }

          @Override
          public void onSuccess(AsyncTransactionManagerImpl result) {
            result.close();
            session.close();
          }
        },
        MoreExecutors.directExecutor());
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
                    delegateTxnFuture.set(result);
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
      }
    }
  }

  @Override
  public ApiFuture<Timestamp> commitAsync() {
    synchronized (lock) {
      Preconditions.checkState(
          txnState == TransactionState.STARTED,
          "commit can only be invoked if the transaction is in progress. Current state: "
              + txnState);
      txnState = TransactionState.COMMITTED;
    }
    return ApiFutures.transformAsync(
        delegate,
        new ApiAsyncFunction<AsyncTransactionManagerImpl, Timestamp>() {
          @Override
          public ApiFuture<Timestamp> apply(AsyncTransactionManagerImpl input) throws Exception {
            final SettableApiFuture<Timestamp> res = SettableApiFuture.create();
            ApiFutures.addCallback(
                input.commitAsync(),
                new ApiFutureCallback<Timestamp>() {
                  @Override
                  public void onFailure(Throwable t) {
                    synchronized (lock) {
                      if (t instanceof AbortedException) {
                        txnState = TransactionState.ABORTED;
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
          }
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
        new ApiAsyncFunction<AsyncTransactionManagerImpl, Void>() {
          @Override
          public ApiFuture<Void> apply(AsyncTransactionManagerImpl input) throws Exception {
            ApiFuture<Void> res = input.rollbackAsync();
            res.addListener(
                new Runnable() {
                  @Override
                  public void run() {
                    session.close();
                  }
                },
                MoreExecutors.directExecutor());
            return res;
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public TransactionContextFuture resetForRetryAsync() {
    synchronized (lock) {
      Preconditions.checkState(
          txnState == TransactionState.ABORTED,
          "resetForRetry can only be called after the transaction aborted.");
      txnState = TransactionState.STARTED;
    }
    return new TransactionContextFutureImpl(
        this,
        ApiFutures.transformAsync(
            delegate,
            new ApiAsyncFunction<AsyncTransactionManagerImpl, TransactionContext>() {
              @Override
              public ApiFuture<TransactionContext> apply(AsyncTransactionManagerImpl input)
                  throws Exception {
                return input.resetForRetryAsync();
              }
            },
            MoreExecutors.directExecutor()));
  }

  @Override
  public TransactionState getState() {
    synchronized (lock) {
      return txnState;
    }
  }
}
