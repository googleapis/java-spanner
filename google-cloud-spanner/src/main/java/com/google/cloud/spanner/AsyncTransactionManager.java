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
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An interface for managing the life cycle of a read write transaction including all its retries.
 * See {@link TransactionContext} for a description of transaction semantics.
 *
 * <p>At any point in time there can be at most one active transaction in this manager. When that
 * transaction is committed, if it fails with an {@code ABORTED} error, calling {@link
 * #resetForRetryAsync()} would create a new {@link TransactionContextFuture}. The newly created
 * transaction would use the same session thus increasing its lock priority. If the transaction is
 * committed successfully, or is rolled back or commit fails with any error other than {@code
 * ABORTED}, the manager is considered complete and no further transactions are allowed to be
 * created in it.
 *
 * <p>Every {@code AsyncTransactionManager} should either be committed or rolled back. Failure to do
 * so can cause resources to be leaked and deadlocks. Easiest way to guarantee this is by calling
 * {@link #close()} in a finally block.
 *
 * @see DatabaseClient#transactionManagerAsync()
 */
public interface AsyncTransactionManager extends AutoCloseable {
  /**
   * {@link ApiFuture} that returns a {@link TransactionContext} and that supports chaining of
   * multiple {@link TransactionContextFuture}s to form a transaction.
   */
  public interface TransactionContextFuture extends ApiFuture<TransactionContext> {
    /**
     * Sets the first step to execute as part of this transaction after the transaction has started
     * using the specified executor. {@link MoreExecutors#directExecutor()} can be be used for
     * lightweight functions, but should be avoided for heavy or blocking operations. See also
     * {@link ListenableFuture#addListener(Runnable, Executor)} for further information.
     */
    <O> AsyncTransactionStep<Void, O> then(
        AsyncTransactionFunction<Void, O> function, Executor executor);
  }

  /**
   * {@link ApiFuture} that returns the commit {@link Timestamp} of a Cloud Spanner transaction that
   * is executed using an {@link AsyncTransactionManager}. This future is returned by the call to
   * {@link AsyncTransactionStep#commitAsync()} of the last step in the transaction.
   */
  public interface CommitTimestampFuture extends ApiFuture<Timestamp> {
    /**
     * Returns the commit timestamp of the transaction. Getting this value should always be done in
     * order to ensure that the transaction succeeded. If any of the steps in the transaction fails
     * with an uncaught exception, this method will automatically stop the transaction at that point
     * and the exception will be returned as the cause of the {@link ExecutionException} that is
     * thrown by this method.
     *
     * @throws AbortedException if the transaction was aborted by Cloud Spanner and needs to be
     *     retried.
     */
    @Override
    Timestamp get() throws AbortedException, InterruptedException, ExecutionException;

    /**
     * Same as {@link #get()}, but will throw a {@link TimeoutException} if the transaction does not
     * finish within the timeout.
     */
    @Override
    Timestamp get(long timeout, TimeUnit unit)
        throws AbortedException, InterruptedException, ExecutionException, TimeoutException;
  }

  /**
   * {@link AsyncTransactionStep} is returned by {@link
   * TransactionContextFuture#then(AsyncTransactionFunction)} and {@link
   * AsyncTransactionStep#then(AsyncTransactionFunction)} and allows transaction steps that should
   * be executed serially to be chained together. Each step can contain one or more statements that
   * may execute in parallel.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * TransactionContextFuture txnFuture = manager.beginAsync();
   * final String column = "FirstName";
   * txnFuture.then(
   *         new AsyncTransactionFunction<Void, Struct>() {
   *           @Override
   *           public ApiFuture<Struct> apply(TransactionContext txn, Void input)
   *               throws Exception {
   *             return txn.readRowAsync(
   *                 "Singers", Key.of(singerId), Collections.singleton(column));
   *           }
   *         })
   *     .then(
   *         new AsyncTransactionFunction<Struct, Void>() {
   *           @Override
   *           public ApiFuture<Void> apply(TransactionContext txn, Struct input)
   *               throws Exception {
   *             String name = input.getString(column);
   *             txn.buffer(
   *                 Mutation.newUpdateBuilder("Singers")
   *                     .set(column)
   *                     .to(name.toUpperCase())
   *                     .build());
   *             return ApiFutures.immediateFuture(null);
   *           }
   *         })
   * }</pre>
   */
  public interface AsyncTransactionStep<I, O> extends ApiFuture<O> {
    /**
     * Adds a step to the transaction chain that should be executed using the specified executor.
     * This step is guaranteed to be executed only after the previous step executed successfully.
     * {@link MoreExecutors#directExecutor()} can be be used for lightweight functions, but should
     * be avoided for heavy or blocking operations. See also {@link
     * ListenableFuture#addListener(Runnable, Executor)} for further information.
     */
    <RES> AsyncTransactionStep<O, RES> then(
        AsyncTransactionFunction<O, RES> next, Executor executor);

    /**
     * Commits the transaction and returns a {@link CommitTimestampFuture} that will return the
     * commit timestamp of the transaction, or throw the first uncaught exception in the transaction
     * chain as an {@link ExecutionException}.
     */
    CommitTimestampFuture commitAsync();
  }

  /**
   * Each step in a transaction chain is defined by an {@link AsyncTransactionFunction}. It receives
   * a {@link TransactionContext} and the output value of the previous transaction step as its input
   * parameters. The method should return an {@link ApiFuture} that will return the result of this
   * step.
   */
  public interface AsyncTransactionFunction<I, O> {
    /**
     * {@link #apply(TransactionContext, Object)} is called when this transaction step is executed.
     * The input value is the result of the previous step, and this method will only be called if
     * the previous step executed successfully.
     *
     * @param txn the {@link TransactionContext} that can be used to execute statements.
     * @param input the result of the previous transaction step.
     * @return an {@link ApiFuture} that will return the result of this step, and that will be the
     *     input of the next transaction step. This method should never return <code>null</code>.
     *     Instead, if the method does not have a return value, the method should return {@link
     *     ApiFutures#immediateFuture(null)}.
     */
    ApiFuture<O> apply(TransactionContext txn, I input) throws Exception;
  }

  /**
   * Creates a new read write transaction. This must be called before doing any other operation and
   * can only be called once. To create a new transaction for subsequent retries, see {@link
   * #resetForRetry()}.
   */
  TransactionContextFuture beginAsync();

  /**
   * Rolls back the currently active transaction. In most cases there should be no need to call this
   * explicitly since {@link #close()} would automatically roll back any active transaction.
   */
  ApiFuture<Void> rollbackAsync();

  /**
   * Creates a new transaction for retry. This should only be called if the previous transaction
   * failed with {@code ABORTED}. In all other cases, this will throw an {@link
   * IllegalStateException}. Users should backoff before calling this method. Backoff delay is
   * specified by {@link SpannerException#getRetryDelayInMillis()} on the {@code SpannerException}
   * throw by the previous commit call.
   */
  TransactionContextFuture resetForRetryAsync();

  /** Returns the state of the transaction. */
  TransactionState getState();

  /** Returns the {@link CommitResponse} of this transaction. */
  ApiFuture<CommitResponse> getCommitResponse();

  /**
   * Closes the manager. If there is an active transaction, it will be rolled back. Underlying
   * session will be released back to the session pool.
   */
  @Override
  void close();

  /**
   * Closes the transaction manager. If there is an active transaction, it will be rolled back. The
   * underlying session will be released back to the session pool. The returned {@link ApiFuture} is
   * done when the transaction (if any) has been rolled back.
   */
  ApiFuture<Void> closeAsync();
}
