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
import static com.google.cloud.spanner.connection.AbstractStatementParser.BEGIN_STATEMENT;
import static com.google.cloud.spanner.connection.AbstractStatementParser.COMMIT_STATEMENT;
import static com.google.cloud.spanner.connection.AbstractStatementParser.ROLLBACK_STATEMENT;
import static com.google.cloud.spanner.connection.AbstractStatementParser.RUN_BATCH_STATEMENT;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.Tuple;
import com.google.cloud.spanner.AbortedDueToConcurrentModificationException;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.CommitResponse;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.ProtobufResultSet;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.AbstractStatementParser.StatementType;
import com.google.cloud.spanner.connection.TransactionRetryListener.RetryResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.SpannerGrpc;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
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
  private final ReentrantLock abortedLock = new ReentrantLock();
  private final long transactionId;
  private final DatabaseClient dbClient;
  private final TransactionOption[] transactionOptions;
  private TransactionManager txManager;
  private final boolean retryAbortsInternally;
  private final boolean delayTransactionStartUntilFirstWrite;
  private final SavepointSupport savepointSupport;
  private int transactionRetryAttempts;
  private int successfulRetries;
  private final List<TransactionRetryListener> transactionRetryListeners;
  private volatile ApiFuture<TransactionContext> txContextFuture;
  private boolean canUseSingleUseRead;
  private volatile SettableApiFuture<CommitResponse> commitResponseFuture;
  private volatile UnitOfWorkState state = UnitOfWorkState.STARTED;
  private volatile AbortedException abortedException;
  private AbortedException rolledBackToSavepointException;
  private boolean timedOutOrCancelled = false;
  private final List<RetriableStatement> statements = new ArrayList<>();
  private final List<Mutation> mutations = new ArrayList<>();
  private Timestamp transactionStarted;

  private static final class RollbackToSavepointException extends Exception {}

  static class Builder extends AbstractMultiUseTransaction.Builder<Builder, ReadWriteTransaction> {
    private DatabaseClient dbClient;
    private Boolean retryAbortsInternally;
    private boolean delayTransactionStartUntilFirstWrite;
    private boolean returnCommitStats;
    private SavepointSupport savepointSupport;
    private List<TransactionRetryListener> transactionRetryListeners;

    private Builder() {}

    Builder setDatabaseClient(DatabaseClient client) {
      Preconditions.checkNotNull(client);
      this.dbClient = client;
      return this;
    }

    Builder setDelayTransactionStartUntilFirstWrite(boolean delayTransactionStartUntilFirstWrite) {
      this.delayTransactionStartUntilFirstWrite = delayTransactionStartUntilFirstWrite;
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

    Builder setSavepointSupport(SavepointSupport savepointSupport) {
      this.savepointSupport = savepointSupport;
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
      Preconditions.checkState(savepointSupport != null, "SavepointSupport is not specified");
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
    this.delayTransactionStartUntilFirstWrite = builder.delayTransactionStartUntilFirstWrite;
    this.retryAbortsInternally = builder.retryAbortsInternally;
    this.savepointSupport = builder.savepointSupport;
    this.transactionRetryListeners = builder.transactionRetryListeners;
    this.transactionOptions = extractOptions(builder);
  }

  private TransactionOption[] extractOptions(Builder builder) {
    int numOptions = 0;
    if (builder.returnCommitStats) {
      numOptions++;
    }
    if (this.transactionTag != null) {
      numOptions++;
    }
    if (this.rpcPriority != null) {
      numOptions++;
    }
    TransactionOption[] options = new TransactionOption[numOptions];
    int index = 0;
    if (builder.returnCommitStats) {
      options[index++] = Options.commitStats();
    }
    if (this.transactionTag != null) {
      options[index++] = Options.tag(this.transactionTag);
    }
    if (this.rpcPriority != null) {
      options[index++] = Options.priority(this.rpcPriority);
    }
    return options;
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("ReadWriteTransaction - ID: ")
        .append(transactionId)
        .append("; Delay tx start: ")
        .append(delayTransactionStartUntilFirstWrite)
        .append("; Tag: ")
        .append(Strings.nullToEmpty(transactionTag))
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

  @Override
  void checkOrCreateValidTransaction(ParsedStatement statement, CallType callType) {
    checkValidStateAndMarkStarted();
    if (txContextFuture == null
        && (!delayTransactionStartUntilFirstWrite
            || (statement != null && statement.isUpdate())
            || (statement == COMMIT_STATEMENT && !mutations.isEmpty()))) {
      txManager = dbClient.transactionManager(this.transactionOptions);
      canUseSingleUseRead = false;
      txContextFuture =
          executeStatementAsync(
              callType, BEGIN_STATEMENT, txManager::begin, SpannerGrpc.getBeginTransactionMethod());
    } else if (txContextFuture == null && delayTransactionStartUntilFirstWrite) {
      canUseSingleUseRead = true;
    }
  }

  private void checkValidStateAndMarkStarted() {
    ConnectionPreconditions.checkState(
        this.state == UnitOfWorkState.STARTED || this.state == UnitOfWorkState.ABORTED,
        "This transaction has status "
            + this.state.name()
            + ", only "
            + UnitOfWorkState.STARTED
            + "or "
            + UnitOfWorkState.ABORTED
            + " is allowed.");
    ConnectionPreconditions.checkState(
        this.retryAbortsInternally || this.rolledBackToSavepointException == null,
        "Cannot resume execution after rolling back to a savepoint if internal retries have been disabled. "
            + "Call Connection#setRetryAbortsInternally(true) or execute `SET RETRY_ABORTS_INTERNALLY=TRUE` to enable "
            + "resuming execution after rolling back to a savepoint.");
    checkTimedOut();
    if (transactionStarted == null) {
      transactionStarted = Timestamp.now();
    }
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

  void checkRolledBackToSavepoint() {
    if (this.rolledBackToSavepointException != null) {
      if (savepointSupport == SavepointSupport.FAIL_AFTER_ROLLBACK) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.FAILED_PRECONDITION,
            "Using a read/write transaction after rolling back to a savepoint is not supported "
                + "with SavepointSupport="
                + savepointSupport);
      } else {
        AbortedException exception = this.rolledBackToSavepointException;
        this.rolledBackToSavepointException = null;
        throw exception;
      }
    }
  }

  @Override
  ReadContext getReadContext() {
    if (txContextFuture == null && canUseSingleUseRead) {
      return dbClient.singleUse();
    }
    ConnectionPreconditions.checkState(txContextFuture != null, "Missing transaction context");
    return get(txContextFuture);
  }

  TransactionContext getTransactionContext() {
    ConnectionPreconditions.checkState(txContextFuture != null, "Missing transaction context");
    return (TransactionContext) getReadContext();
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
  public ApiFuture<Void> executeDdlAsync(CallType callType, ParsedStatement ddl) {
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
      final CallType callType,
      final ParsedStatement statement,
      final AnalyzeMode analyzeMode,
      final QueryOption... options) {
    Preconditions.checkArgument(
        (statement.getType() == StatementType.QUERY)
            || (statement.getType() == StatementType.UPDATE && statement.hasReturningClause()),
        "Statement must be a query or DML with returning clause");
    checkOrCreateValidTransaction(statement, callType);

    ApiFuture<ResultSet> res;
    if (retryAbortsInternally && txContextFuture != null) {
      res =
          executeStatementAsync(
              callType,
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
                        DirectExecuteResultSet delegate =
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
      res = super.executeQueryAsync(callType, statement, analyzeMode, options);
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
  public ApiFuture<ResultSet> analyzeUpdateAsync(
      CallType callType, ParsedStatement update, AnalyzeMode analyzeMode, UpdateOption... options) {
    return ApiFutures.transform(
        internalExecuteUpdateAsync(callType, update, analyzeMode, options),
        Tuple::y,
        MoreExecutors.directExecutor());
  }

  @Override
  public ApiFuture<Long> executeUpdateAsync(
      CallType callType, final ParsedStatement update, final UpdateOption... options) {
    return ApiFutures.transform(
        internalExecuteUpdateAsync(callType, update, AnalyzeMode.NONE, options),
        Tuple::x,
        MoreExecutors.directExecutor());
  }

  /**
   * Executes the given update statement using the specified query planning mode and with the given
   * options and returns the result as a {@link Tuple}. The tuple contains either a {@link
   * ResultSet} with the query plan and execution statistics, or a {@link Long} that contains the
   * update count that was returned for the update statement. Only one of the elements in the tuple
   * will be set, and the reason that we are using a {@link Tuple} here is because Java does not
   * have a standard implementation for an 'Either' class (i.e. a Tuple where only one element is
   * set). An alternative would be to always return a {@link ResultSet} with the update count
   * encoded in the execution stats of the result set, but this would mean that we would create
   * additional {@link ResultSet} instances every time an update statement is executed in normal
   * mode.
   */
  private ApiFuture<Tuple<Long, ResultSet>> internalExecuteUpdateAsync(
      CallType callType, ParsedStatement update, AnalyzeMode analyzeMode, UpdateOption... options) {
    Preconditions.checkNotNull(update);
    Preconditions.checkArgument(update.isUpdate(), "The statement is not an update statement");
    checkOrCreateValidTransaction(update, callType);
    ApiFuture<Tuple<Long, ResultSet>> res;
    if (retryAbortsInternally && txContextFuture != null) {
      res =
          executeStatementAsync(
              callType,
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

                        Tuple<Long, ResultSet> result;
                        long updateCount;
                        if (analyzeMode == AnalyzeMode.NONE) {
                          updateCount =
                              get(txContextFuture).executeUpdate(update.getStatement(), options);
                          result = Tuple.of(updateCount, null);
                        } else {
                          ResultSet resultSet =
                              get(txContextFuture)
                                  .analyzeUpdateStatement(
                                      update.getStatement(),
                                      analyzeMode.getQueryAnalyzeMode(),
                                      options);
                          updateCount =
                              Objects.requireNonNull(resultSet.getStats()).getRowCountExact();
                          result = Tuple.of(null, resultSet);
                        }
                        createAndAddRetriableUpdate(update, analyzeMode, updateCount, options);
                        return result;
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
              callType,
              update,
              () -> {
                checkTimedOut();
                checkAborted();
                if (analyzeMode == AnalyzeMode.NONE) {
                  return Tuple.of(
                      get(txContextFuture).executeUpdate(update.getStatement(), options), null);
                }
                ResultSet resultSet =
                    get(txContextFuture)
                        .analyzeUpdateStatement(
                            update.getStatement(), analyzeMode.getQueryAnalyzeMode(), options);
                return Tuple.of(null, resultSet);
              },
              SpannerGrpc.getExecuteSqlMethod());
    }
    ApiFutures.addCallback(
        res,
        new ApiFutureCallback<Tuple<Long, ResultSet>>() {
          @Override
          public void onFailure(Throwable t) {
            if (t instanceof SpannerException) {
              handlePossibleInvalidatingException((SpannerException) t);
            }
          }

          @Override
          public void onSuccess(Tuple<Long, ResultSet> result) {}
        },
        MoreExecutors.directExecutor());
    return res;
  }

  @Override
  public ApiFuture<long[]> executeBatchUpdateAsync(
      CallType callType, Iterable<ParsedStatement> updates, final UpdateOption... options) {
    Preconditions.checkNotNull(updates);
    final List<Statement> updateStatements = new LinkedList<>();
    for (ParsedStatement update : updates) {
      Preconditions.checkArgument(
          update.isUpdate(),
          "Statement is not an update statement: " + update.getSqlWithoutComments());
      updateStatements.add(update.getStatement());
    }
    checkOrCreateValidTransaction(Iterables.getFirst(updates, null), callType);

    ApiFuture<long[]> res;
    if (retryAbortsInternally) {
      res =
          executeStatementAsync(
              callType,
              RUN_BATCH_STATEMENT,
              () -> {
                checkTimedOut();
                return runWithRetry(
                    () -> {
                      try {
                        getStatementExecutor()
                            .invokeInterceptors(
                                RUN_BATCH_STATEMENT,
                                StatementExecutionStep.EXECUTE_STATEMENT,
                                ReadWriteTransaction.this);
                        long[] updateCounts =
                            get(txContextFuture).batchUpdate(updateStatements, options);
                        createAndAddRetriableBatchUpdate(updateStatements, updateCounts, options);
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
              callType,
              RUN_BATCH_STATEMENT,
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
  public ApiFuture<Void> writeAsync(CallType callType, Iterable<Mutation> mutations) {
    Preconditions.checkNotNull(mutations);
    // We actually don't need an underlying transaction yet, as mutations are buffered until commit.
    // But we do need to verify that this transaction is valid, and to mark the start of the
    // transaction.
    checkValidStateAndMarkStarted();
    for (Mutation mutation : mutations) {
      this.mutations.add(checkNotNull(mutation));
    }
    return ApiFutures.immediateFuture(null);
  }

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
  public ApiFuture<Void> commitAsync(CallType callType) {
    checkOrCreateValidTransaction(COMMIT_STATEMENT, callType);
    state = UnitOfWorkState.COMMITTING;
    commitResponseFuture = SettableApiFuture.create();
    ApiFuture<Void> res;
    // Check if this transaction actually needs to commit anything.
    if (txContextFuture == null) {
      // No actual transaction was started by this read/write transaction, which also means that we
      // don't have to commit anything.
      commitResponseFuture.set(
          new CommitResponse(
              Timestamp.fromProto(com.google.protobuf.Timestamp.getDefaultInstance())));
      state = UnitOfWorkState.COMMITTED;
      res = SettableApiFuture.create();
      ((SettableApiFuture<Void>) res).set(null);
    } else if (retryAbortsInternally) {
      res =
          executeStatementAsync(
              callType,
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
              callType,
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
      abortedLock.lock();
      try {
        checkAborted();
        try {
          checkRolledBackToSavepoint();
          return callable.call();
        } catch (final AbortedException aborted) {
          handleAborted(aborted);
        } catch (SpannerException e) {
          throw e;
        } catch (Exception e) {
          throw SpannerExceptionFactory.asSpannerException(e);
        }
      } finally {
        abortedLock.unlock();
      }
    }
  }

  /**
   * Registers a {@link ResultSet} on this transaction that must be checked during a retry, and
   * returns a retryable {@link ResultSet}.
   */
  private ResultSet createAndAddRetryResultSet(
      ProtobufResultSet resultSet,
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

  private void createAndAddRetriableUpdate(
      ParsedStatement update, AnalyzeMode analyzeMode, long updateCount, UpdateOption... options) {
    if (retryAbortsInternally) {
      addRetryStatement(new RetriableUpdate(this, update, analyzeMode, updateCount, options));
    }
  }

  private void createAndAddRetriableBatchUpdate(
      Iterable<Statement> updates, long[] updateCounts, UpdateOption... options) {
    if (retryAbortsInternally) {
      addRetryStatement(new RetriableBatchUpdate(this, updates, updateCounts, options));
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
            //noinspection BusyWait
            Thread.sleep(delay);
          }
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.CANCELLED, "The statement was cancelled");
        }
        try {
          if (aborted.getCause() instanceof RollbackToSavepointException) {
            txManager = dbClient.transactionManager(transactionOptions);
            txContextFuture = ApiFutures.immediateFuture(txManager.begin());
          } else {
            txContextFuture = ApiFutures.immediateFuture(txManager.resetForRetry());
          }
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

  private final Callable<Void> rollbackCallable =
      new Callable<Void>() {
        @Override
        public Void call() {
          try {
            if (state != UnitOfWorkState.ABORTED && rolledBackToSavepointException == null) {
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
  public ApiFuture<Void> rollbackAsync(CallType callType) {
    return rollbackAsync(callType, true);
  }

  private ApiFuture<Void> rollbackAsync(CallType callType, boolean updateStatus) {
    ConnectionPreconditions.checkState(
        state == UnitOfWorkState.STARTED || state == UnitOfWorkState.ABORTED,
        "This transaction has status " + state.name());
    if (updateStatus) {
      state = UnitOfWorkState.ROLLED_BACK;
    }
    if (txContextFuture != null && state != UnitOfWorkState.ABORTED) {
      return executeStatementAsync(
          callType, ROLLBACK_STATEMENT, rollbackCallable, SpannerGrpc.getRollbackMethod());
    } else {
      return ApiFutures.immediateFuture(null);
    }
  }

  @Override
  String getUnitOfWorkName() {
    return "read/write transaction";
  }

  static class ReadWriteSavepoint extends Savepoint {
    private final int statementPosition;
    private final int mutationPosition;

    ReadWriteSavepoint(String name, int statementPosition, int mutationPosition) {
      super(name);
      this.statementPosition = statementPosition;
      this.mutationPosition = mutationPosition;
    }

    @Override
    int getStatementPosition() {
      return this.statementPosition;
    }

    @Override
    int getMutationPosition() {
      return this.mutationPosition;
    }
  }

  @Override
  Savepoint savepoint(String name) {
    return new ReadWriteSavepoint(name, statements.size(), mutations.size());
  }

  @Override
  void rollbackToSavepoint(Savepoint savepoint) {
    get(rollbackAsync(CallType.SYNC, false));
    // Mark the state of the transaction as rolled back to a savepoint. This will ensure that the
    // transaction will retry the next time a statement is actually executed.
    this.rolledBackToSavepointException =
        (AbortedException)
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.ABORTED,
                "Transaction has been rolled back to a savepoint",
                new RollbackToSavepointException());
    // Clear all statements and mutations after the savepoint.
    this.statements.subList(savepoint.getStatementPosition(), this.statements.size()).clear();
    this.mutations.subList(savepoint.getMutationPosition(), this.mutations.size()).clear();
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
      ProtobufResultSet delegate,
      ParsedStatement statement,
      AnalyzeMode analyzeMode,
      QueryOption... options) {
    return new ChecksumResultSet(this, delegate, statement, analyzeMode, options);
  }
}
