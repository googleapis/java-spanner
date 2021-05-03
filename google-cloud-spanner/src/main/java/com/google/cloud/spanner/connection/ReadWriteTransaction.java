/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.connection;

import static com.google.cloud.spanner.SpannerApiFutures.get;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbortedDueToConcurrentModificationException;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.CommitResponse;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;
import com.google.cloud.spanner.connection.StatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.TransactionRetryListener.RetryResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.SpannerGrpc;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction that is used when a {@link Connection} is normal read/write mode (i.e. not autocommit
 * and not read-only). These transactions can be automatically retried if an {@link
 * AbortedException} is thrown. The transaction will keep track of a running checksum of all {@link
 * ResultSet}s that have been returned, and the update counts returned by any DML statement executed
 * during the transaction. As long as these checksums and update counts are equal for both the
 * original transaction and the retried transaction, the retry can safely be assumed to have the
 * exact same results as the original transaction.
 */
class ReadWriteTransaction extends AbstractMultiUseTransaction {
  private static final Logger logger = Logger.getLogger(ReadWriteTransaction.class.getName());
  private static final AtomicLong ID_GENERATOR = new AtomicLong();
  private static final String MAX_INTERNAL_RETRIES_EXCEEDED =
      "Internal transaction retry maximum exceeded";
  private static final int MAX_INTERNAL_RETRIES = 50;
  private final long transactionId;
  private final DatabaseClient dbClient;
  private TransactionManager txManager;
  private final boolean retryAbortsInternally;
  private int transactionRetryAttempts;
  private int successfulRetries;
  private final List<TransactionRetryListener> transactionRetryListeners;
  private volatile ApiFuture<TransactionContext> txContextFuture;
  private volatile SettableApiFuture<CommitResponse> commitResponseFuture;
  private volatile UnitOfWorkState state = UnitOfWorkState.STARTED;
  private volatile AbortedException abortedException;
  private boolean timedOutOrCancelled = false;
  private final List<RetriableStatement> statements = new ArrayList<>();
  private final List<Mutation> mutations = new ArrayList<>();
  private Timestamp transactionStarted;
  final Object abortedLock = new Object();

  static class Builder extends AbstractMultiUseTransaction.Builder<Builder, ReadWriteTransaction> {
    private DatabaseClient dbClient;
    private Boolean retryAbortsInternally;
    private boolean returnCommitStats;
    private List<TransactionRetryListener> transactionRetryListeners;

    private Builder() {}

    Builder setDatabaseClient(DatabaseClient client) {
      Preconditions.checkNotNull(client);
      this.dbClient = client;
      return this;
    }

    Builder setRetryAbortsInternally(boolean retryAbortsInternally) {
      this.retryAbortsInternally = retryAbortsInternally;
      return this;
    }

    Builder setReturnCommitStats(boolean returnCommitStats) {
      this.returnCommitStats = returnCommitStats;
      return this;
    }

    Builder setTransactionRetryListeners(List<TransactionRetryListener> listeners) {
      Preconditions.checkNotNull(listeners);
      this.transactionRetryListeners = listeners;
      return this;
    }

    @Override
    ReadWriteTransaction build() {
      Preconditions.checkState(dbClient != null, "No DatabaseClient client specified");
      Preconditions.checkState(
          retryAbortsInternally != null, "RetryAbortsInternally is not specified");
      Preconditions.checkState(
          transactionRetryListeners != null, "TransactionRetryListeners are not specified");
      return new ReadWriteTransaction(this);
    }
  }

  static Builder newBuilder() {
    return new Builder();
  }

  private ReadWriteTransaction(Builder builder) {
    super(builder);
    this.transactionId = ID_GENERATOR.incrementAndGet();
    this.dbClient = builder.dbClient;
    this.retryAbortsInternally = builder.retryAbortsInternally;
    this.transactionRetryListeners = builder.transactionRetryListeners;
    this.txManager =
        builder.returnCommitStats
            ? dbClient.transactionManager(Options.commitStats())
            : dbClient.transactionManager();
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("ReadWriteTransaction - ID: ")
        .append(transactionId)
        .append("; Status: ")
        .append(internalGetStateName())
        .append("; Started: ")
        .append(internalGetTimeStarted())
        .append("; Retry attempts: ")
        .append(transactionRetryAttempts)
        .append("; Successful retries: ")
        .append(successfulRetries)
        .toString();
  }

  private String internalGetStateName() {
    return transactionStarted == null ? "Not yet started" : getState().toString();
  }

  private String internalGetTimeStarted() {
    return transactionStarted == null ? "Not yet started" : transactionStarted.toString();
  }

  @Override
  public UnitOfWorkState getState() {
    return this.state;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  private static final ParsedStatement BEGIN_STATEMENT =
      StatementParser.INSTANCE.parse(Statement.of("BEGIN"));

  @Override
  void checkValidTransaction() {
    checkValidState();
    if (txContextFuture == null) {
      transactionStarted = Timestamp.now();
      txContextFuture =
          executeStatementAsync(
              BEGIN_STATEMENT, () -> txManager.begin(), SpannerGrpc.getBeginTransactionMethod());
    }
  }

  private void checkValidState() {
    ConnectionPreconditions.checkState(
        this.state == UnitOfWorkState.STARTED || this.state == UnitOfWorkState.ABORTED,
        "This transaction has status "
            + this.state.name()
            + ", only "
            + UnitOfWorkState.STARTED
            + "or "
            + UnitOfWorkState.ABORTED
            + " is allowed.");
    checkTimedOut();
  }

  private void checkTimedOut() {
    ConnectionPreconditions.checkState(
        !timedOutOrCancelled,
        "The last statement of this transaction timed out or was cancelled. "
            + "The transaction is no longer usable. "
            + "Rollback the transaction and start a new one.");
  }

  @Override
  public boolean isActive() {
    // Consider ABORTED an active state, as it is something that is automatically set if the
    // transaction is aborted by the backend. That means that we should not automatically create a
    // new transaction for the following statement after a transaction has aborted, and instead we
    // should wait until the application has rolled back the current transaction.
    //
    // Otherwise the following list of statements could show unexpected behavior:

    // connection.executeUpdateAsync("UPDATE FOO SET BAR=1 ...");
    // connection.executeUpdateAsync("UPDATE BAR SET FOO=2 ...");
    // connection.commitAsync();
    //
    // If the first update statement fails with an aborted exception, the second update statement
    // should not be executed in a new transaction, but should also abort.
    return getState().isActive() || state == UnitOfWorkState.ABORTED;
  }

  void checkAborted() {
    if (this.state == UnitOfWorkState.ABORTED && this.abortedException != null) {
      if (this.abortedException instanceof AbortedDueToConcurrentModificationException) {
        throw SpannerExceptionFactory.newAbortedDueToConcurrentModificationException(
            (AbortedDueToConcurrentModificationException) this.abortedException);
      } else {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.ABORTED,
            "This transaction has already been aborted. Rollback this transaction to start a new one.",
            this.abortedException);
      }
    }
  }

  @Override
  TransactionContext getReadContext() {
    ConnectionPreconditions.checkState(txContextFuture != null, "Missing transaction context");
    return get(txContextFuture);
  }

  @Override
  public Timestamp getReadTimestamp() {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION,
        "There is no read timestamp available for read/write transactions.");
  }

  @Override
  public Timestamp getReadTimestampOrNull() {
    return null;
  }

  private boolean hasCommitResponse() {
    return commitResponseFuture != null;
  }

  @Override
  public Timestamp getCommitTimestamp() {
    ConnectionPreconditions.checkState(
        hasCommitResponse(), "This transaction has not been committed.");
    return get(commitResponseFuture).getCommitTimestamp();
  }

  @Override
  public Timestamp getCommitTimestampOrNull() {
    return hasCommitResponse() ? get(commitResponseFuture).getCommitTimestamp() : null;
  }

  @Override
  public CommitResponse getCommitResponse() {
    ConnectionPreconditions.checkState(
        hasCommitResponse(), "This transaction has not been committed.");
    return get(commitResponseFuture);
  }

  @Override
  public CommitResponse getCommitResponseOrNull() {
    return hasCommitResponse() ? get(commitResponseFuture) : null;
  }

  @Override
  public ApiFuture<Void> executeDdlAsync(ParsedStatement ddl) {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION,
        "DDL-statements are not allowed inside a read/write transaction.");
  }

  private void handlePossibleInvalidatingException(SpannerException e) {
    if (e.getErrorCode() == ErrorCode.DEADLINE_EXCEEDED
        || e.getErrorCode() == ErrorCode.CANCELLED) {
      this.timedOutOrCancelled = true;
    }
  }

  @Override
  public ApiFuture<ResultSet> executeQueryAsync(
      final ParsedStatement statement,
      final AnalyzeMode analyzeMode,
      final QueryOption... options) {
    Preconditions.checkArgument(statement.isQuery(), "Statement is not a query");
    checkValidTransaction();

    ApiFuture<ResultSet> res;
    if (retryAbortsInternally) {
      res =
          executeStatementAsync(
              statement,
              () -> {
                checkTimedOut();
                return runWithRetry(
                    () -> {
                      try {
                        getStatementExecutor()
                            .invokeInterceptors(
                                statement,
                                StatementExecutionStep.EXECUTE_STATEMENT,
                                ReadWriteTransaction.this);
                        ResultSet delegate =
                            DirectExecuteResultSet.ofResultSet(
                                internalExecuteQuery(statement, analyzeMode, options));
                        return createAndAddRetryResultSet(
                            delegate, statement, analyzeMode, options);
                      } catch (AbortedException e) {
                        throw e;
                      } catch (SpannerException e) {
                        createAndAddFailedQuery(e, statement, analyzeMode, options);
                        throw e;
                      }
                    });
              },
              // ignore interceptors here as they are invoked in the Callable.
              InterceptorsUsage.IGNORE_INTERCEPTORS,
              ImmutableList.of(SpannerGrpc.getExecuteStreamingSqlMethod()));
    } else {
      res = super.executeQueryAsync(statement, analyzeMode, options);
    }
    ApiFutures.addCallback(
        res,
        new ApiFutureCallback<ResultSet>() {
          @Override
          public void onFailure(Throwable t) {
            if (t instanceof SpannerException) {
              handlePossibleInvalidatingException((SpannerException) t);
            }
          }

          @Override
          public void onSuccess(ResultSet result) {}
        },
        MoreExecutors.directExecutor());
    return res;
  }

  @Override
  public ApiFuture<Long> executeUpdateAsync(final ParsedStatement update) {
    Preconditions.checkNotNull(update);
    Preconditions.checkArgument(update.isUpdate(), "The statement is not an update statement");
    checkValidTransaction();
    ApiFuture<Long> res;
    if (retryAbortsInternally) {
      res =
          executeStatementAsync(
              update,
              () -> {
                checkTimedOut();
                return runWithRetry(
                    () -> {
                      try {
                        getStatementExecutor()
                            .invokeInterceptors(
                                update,
                                StatementExecutionStep.EXECUTE_STATEMENT,
                                ReadWriteTransaction.this);
                        long updateCount =
                            get(txContextFuture).executeUpdate(update.getStatement());
                        createAndAddRetriableUpdate(update, updateCount);
                        return updateCount;
                      } catch (AbortedException e) {
                        throw e;
                      } catch (SpannerException e) {
                        createAndAddFailedUpdate(e, update);
                        throw e;
                      }
                    });
              },
              // ignore interceptors here as they are invoked in the Callable.
              InterceptorsUsage.IGNORE_INTERCEPTORS,
              ImmutableList.of(SpannerGrpc.getExecuteSqlMethod()));
    } else {
      res =
          executeStatementAsync(
              update,
              () -> {
                checkTimedOut();
                checkAborted();
                return get(txContextFuture).executeUpdate(update.getStatement());
              },
              SpannerGrpc.getExecuteSqlMethod());
    }
    ApiFutures.addCallback(
        res,
        new ApiFutureCallback<Long>() {
          @Override
          public void onFailure(Throwable t) {
            if (t instanceof SpannerException) {
              handlePossibleInvalidatingException((SpannerException) t);
            }
          }

          @Override
          public void onSuccess(Long result) {}
        },
        MoreExecutors.directExecutor());
    return res;
  }

  /**
   * Create a RUN BATCH statement to use with the {@link #executeBatchUpdate(Iterable)} method to
   * allow it to be cancelled, time out or retried.
   *
   * <p>{@link ReadWriteTransaction} uses the generic methods {@link #executeAsync(ParsedStatement,
   * Callable)} and {@link #runWithRetry(Callable)} to allow statements to be cancelled, to timeout
   * and to be retried. These methods require a {@link ParsedStatement} as input. When the {@link
   * #executeBatchUpdate(Iterable)} method is called, we do not have one {@link ParsedStatement},
   * and the method uses this statement instead in order to use the same logic as the other
   * statements.
   */
  static final ParsedStatement EXECUTE_BATCH_UPDATE_STATEMENT =
      StatementParser.INSTANCE.parse(Statement.of("RUN BATCH"));

  @Override
  public ApiFuture<long[]> executeBatchUpdateAsync(Iterable<ParsedStatement> updates) {
    Preconditions.checkNotNull(updates);
    final List<Statement> updateStatements = new LinkedList<>();
    for (ParsedStatement update : updates) {
      Preconditions.checkArgument(
          update.isUpdate(),
          "Statement is not an update statement: " + update.getSqlWithoutComments());
      updateStatements.add(update.getStatement());
    }
    checkValidTransaction();

    ApiFuture<long[]> res;
    if (retryAbortsInternally) {
      res =
          executeStatementAsync(
              EXECUTE_BATCH_UPDATE_STATEMENT,
              () -> {
                checkTimedOut();
                return runWithRetry(
                    () -> {
                      try {
                        getStatementExecutor()
                            .invokeInterceptors(
                                EXECUTE_BATCH_UPDATE_STATEMENT,
                                StatementExecutionStep.EXECUTE_STATEMENT,
                                ReadWriteTransaction.this);
                        long[] updateCounts = get(txContextFuture).batchUpdate(updateStatements);
                        createAndAddRetriableBatchUpdate(updateStatements, updateCounts);
                        return updateCounts;
                      } catch (AbortedException e) {
                        throw e;
                      } catch (SpannerException e) {
                        createAndAddFailedBatchUpdate(e, updateStatements);
                        throw e;
                      }
                    });
              },
              // ignore interceptors here as they are invoked in the Callable.
              InterceptorsUsage.IGNORE_INTERCEPTORS,
              ImmutableList.of(SpannerGrpc.getExecuteBatchDmlMethod()));
    } else {
      res =
          executeStatementAsync(
              EXECUTE_BATCH_UPDATE_STATEMENT,
              () -> {
                checkTimedOut();
                checkAborted();
                return get(txContextFuture).batchUpdate(updateStatements);
              },
              SpannerGrpc.getExecuteBatchDmlMethod());
    }
    ApiFutures.addCallback(
        res,
        new ApiFutureCallback<long[]>() {
          @Override
          public void onFailure(Throwable t) {
            if (t instanceof SpannerException) {
              handlePossibleInvalidatingException((SpannerException) t);
            }
          }

          @Override
          public void onSuccess(long[] result) {}
        },
        MoreExecutors.directExecutor());
    return res;
  }

  @Override
  public ApiFuture<Void> writeAsync(Iterable<Mutation> mutations) {
    Preconditions.checkNotNull(mutations);
    checkValidTransaction();
    for (Mutation mutation : mutations) {
      this.mutations.add(checkNotNull(mutation));
    }
    return ApiFutures.immediateFuture(null);
  }

  /**
   * Create a COMMIT statement to use with the {@link #commit()} method to allow it to be cancelled,
   * time out or retried.
   *
   * <p>{@link ReadWriteTransaction} uses the generic methods {@link #executeAsync(ParsedStatement,
   * Callable)} and {@link #runWithRetry(Callable)} to allow statements to be cancelled, to timeout
   * and to be retried. These methods require a {@link ParsedStatement} as input. When the {@link
   * #commit()} method is called directly, we do not have a {@link ParsedStatement}, and the method
   * uses this statement instead in order to use the same logic as the other statements.
   */
  private static final ParsedStatement COMMIT_STATEMENT =
      StatementParser.INSTANCE.parse(Statement.of("COMMIT"));

  private final Callable<Void> commitCallable =
      new Callable<Void>() {
        @Override
        public Void call() {
          checkAborted();
          get(txContextFuture).buffer(mutations);
          txManager.commit();
          commitResponseFuture.set(txManager.getCommitResponse());
          state = UnitOfWorkState.COMMITTED;
          return null;
        }
      };

  @Override
  public ApiFuture<Void> commitAsync() {
    checkValidTransaction();
    state = UnitOfWorkState.COMMITTING;
    commitResponseFuture = SettableApiFuture.create();
    ApiFuture<Void> res;
    if (retryAbortsInternally) {
      res =
          executeStatementAsync(
              COMMIT_STATEMENT,
              () -> {
                checkTimedOut();
                try {
                  return runWithRetry(
                      () -> {
                        getStatementExecutor()
                            .invokeInterceptors(
                                COMMIT_STATEMENT,
                                StatementExecutionStep.EXECUTE_STATEMENT,
                                ReadWriteTransaction.this);
                        return commitCallable.call();
                      });
                } catch (Throwable t) {
                  commitResponseFuture.setException(t);
                  state = UnitOfWorkState.COMMIT_FAILED;
                  try {
                    txManager.close();
                  } catch (Throwable t2) {
                    // Ignore.
                  }
                  throw t;
                }
              },
              InterceptorsUsage.IGNORE_INTERCEPTORS,
              ImmutableList.of(SpannerGrpc.getCommitMethod()));
    } else {
      res =
          executeStatementAsync(
              COMMIT_STATEMENT,
              () -> {
                checkTimedOut();
                try {
                  return commitCallable.call();
                } catch (Throwable t) {
                  commitResponseFuture.setException(t);
                  state = UnitOfWorkState.COMMIT_FAILED;
                  try {
                    txManager.close();
                  } catch (Throwable t2) {
                    // Ignore.
                  }
                  throw t;
                }
              },
              SpannerGrpc.getCommitMethod());
    }
    return res;
  }

  /**
   * Executes a database call that could throw an {@link AbortedException}. If an {@link
   * AbortedException} is thrown, the transaction will automatically be retried and the checksums of
   * all {@link ResultSet}s and update counts of DML statements will be checked against the original
   * values of the original transaction. If the checksums and/or update counts do not match, the
   * method will throw an {@link AbortedException} that cannot be retried, as the underlying data
   * have actually changed.
   *
   * <p>If {@link ReadWriteTransaction#retryAbortsInternally} has been set to <code>false</code>,
   * this method will throw an exception instead of retrying the transaction if the transaction was
   * aborted.
   *
   * @param callable The actual database calls.
   * @return the results of the database calls.
   * @throws SpannerException if the database calls threw an exception, an {@link
   *     AbortedDueToConcurrentModificationException} if a retry of the transaction yielded
   *     different results than the original transaction, or an {@link AbortedException} if the
   *     maximum number of retries has been exceeded.
   */
  <T> T runWithRetry(Callable<T> callable) throws SpannerException {
    while (true) {
      synchronized (abortedLock) {
        checkAborted();
        try {
          return callable.call();
        } catch (final AbortedException aborted) {
          handleAborted(aborted);
        } catch (SpannerException e) {
          throw e;
        } catch (Exception e) {
          throw SpannerExceptionFactory.asSpannerException(e);
        }
      }
    }
  }

  /**
   * Registers a {@link ResultSet} on this transaction that must be checked during a retry, and
   * returns a retryable {@link ResultSet}.
   */
  private ResultSet createAndAddRetryResultSet(
      ResultSet resultSet,
      ParsedStatement statement,
      AnalyzeMode analyzeMode,
      QueryOption... options) {
    if (retryAbortsInternally) {
      ChecksumResultSet checksumResultSet =
          createChecksumResultSet(resultSet, statement, analyzeMode, options);
      addRetryStatement(checksumResultSet);
      return checksumResultSet;
    }
    return resultSet;
  }

  /** Registers the statement as a query that should return an error during a retry. */
  private void createAndAddFailedQuery(
      SpannerException e,
      ParsedStatement statement,
      AnalyzeMode analyzeMode,
      QueryOption... options) {
    if (retryAbortsInternally) {
      addRetryStatement(new FailedQuery(this, e, statement, analyzeMode, options));
    }
  }

  private void createAndAddRetriableUpdate(ParsedStatement update, long updateCount) {
    if (retryAbortsInternally) {
      addRetryStatement(new RetriableUpdate(this, update, updateCount));
    }
  }

  private void createAndAddRetriableBatchUpdate(Iterable<Statement> updates, long[] updateCounts) {
    if (retryAbortsInternally) {
      addRetryStatement(new RetriableBatchUpdate(this, updates, updateCounts));
    }
  }

  /** Registers the statement as an update that should return an error during a retry. */
  private void createAndAddFailedUpdate(SpannerException e, ParsedStatement update) {
    if (retryAbortsInternally) {
      addRetryStatement(new FailedUpdate(this, e, update));
    }
  }

  /** Registers the statements as a batch of updates that should return an error during a retry. */
  private void createAndAddFailedBatchUpdate(SpannerException e, Iterable<Statement> updates) {
    if (retryAbortsInternally) {
      addRetryStatement(new FailedBatchUpdate(this, e, updates));
    }
  }

  /**
   * Adds a statement to the list of statements that should be retried if this transaction aborts.
   */
  private void addRetryStatement(RetriableStatement statement) {
    Preconditions.checkState(
        retryAbortsInternally, "retryAbortsInternally is not enabled for this transaction");
    statements.add(statement);
  }

  /**
   * Handles an aborted exception by checking whether the transaction may be retried internally, and
   * if so, does the retry. If retry is not allowed, or if the retry fails, the method will throw an
   * {@link AbortedException}.
   */
  private void handleAborted(AbortedException aborted) {
    if (transactionRetryAttempts >= MAX_INTERNAL_RETRIES) {
      // If the same statement in transaction keeps aborting, then we need to abort here.
      throwAbortWithRetryAttemptsExceeded();
    } else if (retryAbortsInternally) {
      logger.fine(toString() + ": Starting internal transaction retry");
      while (true) {
        // First back off and then restart the transaction.
        long delay = aborted.getRetryDelayInMillis();
        try {
          if (delay > 0L) {
            Thread.sleep(delay);
          }
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.CANCELLED, "The statement was cancelled");
        }
        try {
          txContextFuture = ApiFutures.immediateFuture(txManager.resetForRetry());
          // Inform listeners about the transaction retry that is about to start.
          invokeTransactionRetryListenersOnStart();
          // Then retry all transaction statements.
          transactionRetryAttempts++;
          for (RetriableStatement statement : statements) {
            statement.retry(aborted);
          }
          successfulRetries++;
          invokeTransactionRetryListenersOnFinish(RetryResult.RETRY_SUCCESSFUL);
          logger.fine(
              toString()
                  + ": Internal transaction retry succeeded. Starting retry of original statement.");
          // Retry succeeded, return and continue the original transaction.
          break;
        } catch (AbortedDueToConcurrentModificationException e) {
          // Retry failed because of a concurrent modification, we have to abort.
          invokeTransactionRetryListenersOnFinish(
              RetryResult.RETRY_ABORTED_DUE_TO_CONCURRENT_MODIFICATION);
          logger.fine(
              toString() + ": Internal transaction retry aborted due to a concurrent modification");
          // Do a shoot and forget rollback.
          try {
            txManager.rollback();
          } catch (Throwable t) {
            // ignore
          }
          this.state = UnitOfWorkState.ABORTED;
          this.abortedException = e;
          throw e;
        } catch (AbortedException e) {
          // Retry aborted, do another retry of the transaction.
          if (transactionRetryAttempts >= MAX_INTERNAL_RETRIES) {
            throwAbortWithRetryAttemptsExceeded();
          }
          invokeTransactionRetryListenersOnFinish(RetryResult.RETRY_ABORTED_AND_RESTARTING);
          logger.fine(toString() + ": Internal transaction retry aborted, trying again");
        } catch (SpannerException e) {
          // unexpected exception
          logger.log(
              Level.FINE,
              toString() + ": Internal transaction retry failed due to an unexpected exception",
              e);
          // Do a shoot and forget rollback.
          try {
            txManager.rollback();
          } catch (Throwable t) {
            // ignore
          }
          // Set transaction state to aborted as the retry failed.
          this.state = UnitOfWorkState.ABORTED;
          this.abortedException = aborted;
          // Re-throw underlying exception.
          throw e;
        }
      }
    } else {
      try {
        txManager.close();
      } catch (Throwable t) {
        // ignore
      }
      // Internal retry is not enabled.
      this.state = UnitOfWorkState.ABORTED;
      this.abortedException = aborted;
      throw aborted;
    }
  }

  private void throwAbortWithRetryAttemptsExceeded() throws SpannerException {
    invokeTransactionRetryListenersOnFinish(RetryResult.RETRY_ABORTED_AND_MAX_ATTEMPTS_EXCEEDED);
    logger.fine(
        toString()
            + ": Internal transaction retry aborted and max number of retry attempts has been exceeded");
    // Try to rollback the transaction and ignore any exceptions.
    // Normally it should not be necessary to do this, but in order to be sure we never leak
    // any sessions it is better to do so.
    try {
      txManager.rollback();
    } catch (Throwable t) {
      // ignore
    }
    this.state = UnitOfWorkState.ABORTED;
    this.abortedException =
        (AbortedException)
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.ABORTED, MAX_INTERNAL_RETRIES_EXCEEDED);
    throw this.abortedException;
  }

  private void invokeTransactionRetryListenersOnStart() {
    for (TransactionRetryListener listener : transactionRetryListeners) {
      listener.retryStarting(transactionStarted, transactionId, transactionRetryAttempts);
    }
  }

  private void invokeTransactionRetryListenersOnFinish(RetryResult result) {
    for (TransactionRetryListener listener : transactionRetryListeners) {
      listener.retryFinished(transactionStarted, transactionId, transactionRetryAttempts, result);
    }
  }

  /** The {@link Statement} and {@link Callable} for rollbacks */
  private final ParsedStatement rollbackStatement =
      StatementParser.INSTANCE.parse(Statement.of("ROLLBACK"));

  private final Callable<Void> rollbackCallable =
      new Callable<Void>() {
        @Override
        public Void call() {
          try {
            if (state != UnitOfWorkState.ABORTED) {
              // Make sure the transaction has actually started before we try to rollback.
              get(txContextFuture);
              txManager.rollback();
            }
            return null;
          } finally {
            txManager.close();
          }
        }
      };

  @Override
  public ApiFuture<Void> rollbackAsync() {
    ConnectionPreconditions.checkState(
        state == UnitOfWorkState.STARTED || state == UnitOfWorkState.ABORTED,
        "This transaction has status " + state.name());
    state = UnitOfWorkState.ROLLED_BACK;
    if (txContextFuture != null && state != UnitOfWorkState.ABORTED) {
      return executeStatementAsync(
          rollbackStatement, rollbackCallable, SpannerGrpc.getRollbackMethod());
    } else {
      return ApiFutures.immediateFuture(null);
    }
  }

  /**
   * A retriable statement is a query or DML statement during a read/write transaction that can be
   * retried if the original transaction aborted.
   */
  interface RetriableStatement {
    /**
     * Retry this statement in a new transaction. Throws an {@link
     * AbortedDueToConcurrentModificationException} if the retry could not successfully be executed
     * because of an actual concurrent modification of the underlying data. This {@link
     * AbortedDueToConcurrentModificationException} cannot be retried.
     */
    void retry(AbortedException aborted) throws AbortedException;
  }

  /** Creates a {@link ChecksumResultSet} for this {@link ReadWriteTransaction}. */
  @VisibleForTesting
  ChecksumResultSet createChecksumResultSet(
      ResultSet delegate,
      ParsedStatement statement,
      AnalyzeMode analyzeMode,
      QueryOption... options) {
    return new ChecksumResultSet(this, delegate, statement, analyzeMode, options);
  }
}
