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
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;

class SessionPoolAsyncTransactionManager implements AsyncTransactionManager {
  private TransactionState txnState;
  private volatile PooledSessionFuture session;
  private final SettableApiFuture<AsyncTransactionManager> delegate = SettableApiFuture.create();

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
    delegate.addListener(
        new Runnable() {
          @Override
          public void run() {
            session.close();
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public TransactionContextFuture beginAsync() {
    Preconditions.checkState(txnState == null, "begin can only be called once");
    txnState = TransactionState.STARTED;
    final SettableApiFuture<TransactionContext> delegateTxnFuture = SettableApiFuture.create();
    ApiFutures.addCallback(
        delegate,
        new ApiFutureCallback<AsyncTransactionManager>() {
          @Override
          public void onFailure(Throwable t) {
            delegateTxnFuture.setException(t);
          }

          @Override
          public void onSuccess(AsyncTransactionManager result) {
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

    //    return new TransactionContextFutureImpl(
    //        this,
    //        ApiFutures.transformAsync(
    //            delegate,
    //            new ApiAsyncFunction<AsyncTransactionManager, TransactionContext>() {
    //              @Override
    //              public ApiFuture<TransactionContext> apply(AsyncTransactionManager input) {
    //                return input.beginAsync();
    //              }
    //            },
    //            MoreExecutors.directExecutor()));
  }

  @Override
  public ApiFuture<Timestamp> commitAsync() {
    Preconditions.checkState(
        txnState == TransactionState.STARTED,
        "commit can only be invoked if the transaction is in progress. Current state: " + txnState);
    txnState = TransactionState.COMMITTED;
    return ApiFutures.transformAsync(
        delegate,
        new ApiAsyncFunction<AsyncTransactionManager, Timestamp>() {
          @Override
          public ApiFuture<Timestamp> apply(AsyncTransactionManager input) throws Exception {
            ApiFuture<Timestamp> res = input.commitAsync();
            //            res.addListener(
            //                new Runnable() {
            //                  @Override
            //                  public void run() {
            //                    session.close();
            //                  }
            //                },
            //                MoreExecutors.directExecutor());
            return res;
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public ApiFuture<Void> rollbackAsync() {
    Preconditions.checkState(
        txnState == TransactionState.STARTED,
        "rollback can only be called if the transaction is in progress");
    txnState = TransactionState.ROLLED_BACK;
    return ApiFutures.transformAsync(
        delegate,
        new ApiAsyncFunction<AsyncTransactionManager, Void>() {
          @Override
          public ApiFuture<Void> apply(AsyncTransactionManager input) throws Exception {
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
    Preconditions.checkState(
        txnState != null, "resetForRetry can only be called after the transaction has started.");
    txnState = TransactionState.STARTED;
    return new TransactionContextFutureImpl(
        this,
        ApiFutures.transformAsync(
            delegate,
            new ApiAsyncFunction<AsyncTransactionManager, TransactionContext>() {
              @Override
              public ApiFuture<TransactionContext> apply(AsyncTransactionManager input)
                  throws Exception {
                return input.resetForRetryAsync();
              }
            },
            MoreExecutors.directExecutor()));
  }

  @Override
  public TransactionState getState() {
    try {
      return delegate.get().getState();
    } catch (InterruptedException e) {
      throw SpannerExceptionFactory.propagateInterrupt(e);
    } catch (ExecutionException e) {
      throw SpannerExceptionFactory.newSpannerException(e.getCause() == null ? e : e.getCause());
    }
  }
}
