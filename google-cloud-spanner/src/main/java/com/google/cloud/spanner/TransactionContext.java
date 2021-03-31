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
import com.google.cloud.spanner.Options.UpdateOption;

/**
 * Context for a single attempt of a locking read-write transaction. This type of transaction is the
 * only way to write data into Cloud Spanner; {@link Session#write(Iterable)} and {@link
 * Session#writeAtLeastOnce(Iterable)} use transactions internally. These transactions rely on
 * pessimistic locking and, if necessary, two-phase commit. Locking read-write transactions may
 * abort, requiring the application to retry. However, the interface exposed by {@link
 * TransactionRunner} eliminates the need for applications to write retry loops explicitly.
 *
 * <p>Locking transactions may be used to atomically read-modify-write data anywhere in a database.
 * This type of transaction is externally consistent.
 *
 * <p>Clients should attempt to minimize the amount of time a transaction is active. Faster
 * transactions commit with higher probability and cause less contention. Cloud Spanner attempts to
 * keep read locks active as long as the transaction continues to do reads, and the transaction has
 * not been terminated by returning from a {@link TransactionRunner.TransactionCallable}. Long
 * periods of inactivity at the client may cause Cloud Spanner to release a transaction's locks and
 * abort it.
 *
 * <p>Reads performed within a transaction acquire locks on the data being read. Writes can only be
 * done at commit time, after all reads have been completed.
 *
 * <p>Conceptually, a read-write transaction consists of zero or more reads or SQL queries followed
 * by a commit.
 *
 * <h3>Semantics</h3>
 *
 * <p>Cloud Spanner can commit the transaction if all read locks it acquired are still valid at
 * commit time, and it is able to acquire write locks for all writes. Cloud Spanner can abort the
 * transaction for any reason. If a commit attempt returns {@code ABORTED}, Cloud Spanner guarantees
 * that the transaction has not modified any user data in Cloud Spanner.
 *
 * <p>Unless the transaction commits, Cloud Spanner makes no guarantees about how long the
 * transaction's locks were held for. It is an error to use Cloud Spanner locks for any sort of
 * mutual exclusion other than between Cloud Spanner transactions themselves.
 *
 * <h3>Retrying Aborted Transactions</h3>
 *
 * <p>When a transaction aborts, the application can choose to retry the whole transaction again. To
 * maximize the chances of successfully committing the retry, the client should execute the retry in
 * the same session as the original attempt. The original session's lock priority increases with
 * each consecutive abort, meaning that each attempt has a slightly better chance of success than
 * the previous.
 *
 * <p>Under some circumstances (e.g., many transactions attempting to modify the same row(s)), a
 * transaction can abort many times in a short period before successfully committing. Thus, it is
 * not a good idea to cap the number of retries a transaction can attempt; instead, it is better to
 * limit the total amount of wall time spent retrying.
 *
 * <p>Application code does not need to retry explicitly; {@link TransactionRunner} will
 * automatically retry a transaction if an attempt results in an abort.
 *
 * <h3>Idle Transactions</h3>
 *
 * <p>A transaction is considered idle if it has no outstanding reads or SQL queries and has not
 * started a read or SQL query within the last 10 seconds. Idle transactions can be aborted by Cloud
 * Spanner so that they don't hold on to locks indefinitely. In that case, the commit will fail with
 * error {@code ABORTED}.
 *
 * <p>If this behavior is undesirable, periodically executing a simple SQL query in the transaction
 * (e.g., {@code SELECT 1}) prevents the transaction from becoming idle.
 *
 * @see Session#readWriteTransaction()
 * @see TransactionRunner
 */
public interface TransactionContext extends ReadContext {
  /**
   * Buffers a single mutation to be applied if the transaction commits successfully. The effects of
   * this mutation will not be visible to subsequent operations in the transaction. All buffered
   * mutations will be applied atomically.
   */
  void buffer(Mutation mutation);

  /**
   * Buffers mutations to be applied if the transaction commits successfully. The effects of the
   * mutations will not be visible to subsequent operations in the transaction. All buffered
   * mutations will be applied atomically.
   */
  void buffer(Iterable<Mutation> mutations);

  /**
   * Executes the DML statement(s) and returns the number of rows modified. For non-DML statements,
   * it will result in an {@code IllegalArgumentException}. The effects of the DML statement will be
   * visible to subsequent operations in the transaction.
   */
  long executeUpdate(Statement statement, UpdateOption... options);

  /**
   * Same as {@link #executeUpdate(Statement)}, but is guaranteed to be non-blocking. If multiple
   * asynchronous update statements are submitted to the same read/write transaction, the statements
   * are guaranteed to be submitted to Cloud Spanner in the order that they were submitted in the
   * client. This does however not guarantee that an asynchronous update statement will see the
   * results of all previously submitted statements, as the execution of the statements can be
   * parallel. If you rely on the results of a previous statement, you should block until the result
   * of that statement is known and has been returned to the client.
   */
  ApiFuture<Long> executeUpdateAsync(Statement statement, UpdateOption... options);

  /**
   * Executes a list of DML statements in a single request. The statements will be executed in order
   * and the semantics is the same as if each statement is executed by {@code executeUpdate} in a
   * loop. This method returns an array of long integers, each representing the number of rows
   * modified by each statement.
   *
   * <p>If an individual statement fails, execution stops and a {@code SpannerBatchUpdateException}
   * is returned, which includes the error and the number of rows affected by the statements that
   * are run prior to the error.
   *
   * <p>For example, if statements contains 3 statements, and the 2nd one is not a valid DML. This
   * method throws a {@code SpannerBatchUpdateException} that contains the error message from the
   * 2nd statement, and an array of length 1 that contains the number of rows modified by the 1st
   * statement. The 3rd statement will not run.
   */
  long[] batchUpdate(Iterable<Statement> statements, UpdateOption... options);

  /**
   * Same as {@link #batchUpdate(Iterable)}, but is guaranteed to be non-blocking. If multiple
   * asynchronous update statements are submitted to the same read/write transaction, the statements
   * are guaranteed to be submitted to Cloud Spanner in the order that they were submitted in the
   * client. This does however not guarantee that an asynchronous update statement will see the
   * results of all previously submitted statements, as the execution of the statements can be
   * parallel. If you rely on the results of a previous statement, you should block until the result
   * of that statement is known and has been returned to the client.
   */
  ApiFuture<long[]> batchUpdateAsync(Iterable<Statement> statements, UpdateOption... options);
}
