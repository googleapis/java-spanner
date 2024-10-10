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

import static com.google.cloud.spanner.connection.AbstractStatementParser.COMMIT_STATEMENT;
import static com.google.cloud.spanner.connection.AbstractStatementParser.RUN_BATCH_STATEMENT;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.Tuple;
import com.google.cloud.spanner.BatchClient;
import com.google.cloud.spanner.BatchReadOnlyTransaction;
import com.google.cloud.spanner.CommitResponse;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.PartitionOptions;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerApiFutures;
import com.google.cloud.spanner.SpannerBatchUpdateException;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionRunner;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.AbstractStatementParser.StatementType;
import com.google.cloud.spanner.connection.ReadWriteTransaction.Builder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.admin.database.v1.DatabaseAdminGrpc;
import com.google.spanner.v1.SpannerGrpc;
import io.opentelemetry.context.Scope;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;

/**
 * Transaction that is used when a {@link Connection} is in autocommit mode. Each method on this
 * transaction actually starts a new transaction on Spanner. The type of transaction that is started
 * depends on the type of statement that is being executed. A {@link SingleUseTransaction} will
 * always try to choose the most efficient type of one-time transaction that is available for the
 * statement.
 *
 * <p>A {@link SingleUseTransaction} can be used to execute any type of statement on Cloud Spanner:
 *
 * <ul>
 *   <li>Client side statements, e.g. SHOW VARIABLE AUTOCOMMIT
 *   <li>Queries, e.g. SELECT * FROM FOO
 *   <li>DML statements, e.g. UPDATE FOO SET BAR=1
 *   <li>DDL statements, e.g. CREATE TABLE FOO (...)
 * </ul>
 */
class SingleUseTransaction extends AbstractBaseUnitOfWork {
  private final boolean readOnly;
  private final DdlClient ddlClient;
  private final DatabaseClient dbClient;
  private final BatchClient batchClient;
  private final TimestampBound readOnlyStaleness;
  private final AutocommitDmlMode autocommitDmlMode;
  private final boolean returnCommitStats;
  private final Duration maxCommitDelay;
  private final boolean internalMetdataQuery;
  private final byte[] protoDescriptors;
  private volatile SettableApiFuture<Timestamp> readTimestamp = null;
  private volatile TransactionRunner writeTransaction;
  private boolean used = false;
  private volatile UnitOfWorkState state = UnitOfWorkState.STARTED;

  static class Builder extends AbstractBaseUnitOfWork.Builder<Builder, SingleUseTransaction> {
    private DdlClient ddlClient;
    private DatabaseClient dbClient;
    private BatchClient batchClient;
    private boolean readOnly;
    private TimestampBound readOnlyStaleness;
    private AutocommitDmlMode autocommitDmlMode;
    private boolean returnCommitStats;
    private Duration maxCommitDelay;
    private boolean internalMetadataQuery;
    private byte[] protoDescriptors;

    private Builder() {}

    Builder setDdlClient(DdlClient ddlClient) {
      Preconditions.checkNotNull(ddlClient);
      this.ddlClient = ddlClient;
      return this;
    }

    Builder setDatabaseClient(DatabaseClient client) {
      Preconditions.checkNotNull(client);
      this.dbClient = client;
      return this;
    }

    Builder setBatchClient(BatchClient batchClient) {
      this.batchClient = Preconditions.checkNotNull(batchClient);
      return this;
    }

    Builder setReadOnly(boolean readOnly) {
      this.readOnly = readOnly;
      return this;
    }

    Builder setReadOnlyStaleness(TimestampBound staleness) {
      Preconditions.checkNotNull(staleness);
      this.readOnlyStaleness = staleness;
      return this;
    }

    Builder setAutocommitDmlMode(AutocommitDmlMode dmlMode) {
      Preconditions.checkNotNull(dmlMode);
      this.autocommitDmlMode = dmlMode;
      return this;
    }

    Builder setReturnCommitStats(boolean returnCommitStats) {
      this.returnCommitStats = returnCommitStats;
      return this;
    }

    Builder setMaxCommitDelay(Duration maxCommitDelay) {
      this.maxCommitDelay = maxCommitDelay;
      return this;
    }

    Builder setInternalMetadataQuery(boolean internalMetadataQuery) {
      this.internalMetadataQuery = internalMetadataQuery;
      return this;
    }

    Builder setProtoDescriptors(byte[] protoDescriptors) {
      this.protoDescriptors = protoDescriptors;
      return this;
    }

    @Override
    SingleUseTransaction build() {
      Preconditions.checkState(ddlClient != null, "No DDL client specified");
      Preconditions.checkState(dbClient != null, "No DatabaseClient client specified");
      Preconditions.checkState(batchClient != null, "No BatchClient client specified");
      Preconditions.checkState(readOnlyStaleness != null, "No read-only staleness specified");
      Preconditions.checkState(autocommitDmlMode != null, "No autocommit dml mode specified");
      return new SingleUseTransaction(this);
    }
  }

  static Builder newBuilder() {
    return new Builder();
  }

  private SingleUseTransaction(Builder builder) {
    super(builder);
    this.ddlClient = builder.ddlClient;
    this.dbClient = builder.dbClient;
    this.batchClient = builder.batchClient;
    this.readOnly = builder.readOnly;
    this.readOnlyStaleness = builder.readOnlyStaleness;
    this.autocommitDmlMode = builder.autocommitDmlMode;
    this.returnCommitStats = builder.returnCommitStats;
    this.maxCommitDelay = builder.maxCommitDelay;
    this.internalMetdataQuery = builder.internalMetadataQuery;
    this.protoDescriptors = builder.protoDescriptors;
  }

  @Override
  public boolean isSingleUse() {
    return true;
  }

  @Override
  public Type getType() {
    return Type.TRANSACTION;
  }

  @Override
  public UnitOfWorkState getState() {
    return state;
  }

  @Override
  public boolean isActive() {
    // Single-use transactions are never active as they can be used only once.
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public boolean supportsDirectedReads(ParsedStatement parsedStatement) {
    return parsedStatement.isQuery();
  }

  private void checkAndMarkUsed() {
    Preconditions.checkState(!used, "This single-use transaction has already been used");
    used = true;
  }

  @Override
  public ApiFuture<ResultSet> executeQueryAsync(
      final CallType callType,
      final ParsedStatement statement,
      final AnalyzeMode analyzeMode,
      final QueryOption... options) {
    Preconditions.checkNotNull(statement);
    Preconditions.checkArgument(
        statement.isQuery()
            || (statement.isUpdate()
                && (analyzeMode != AnalyzeMode.NONE || statement.hasReturningClause())),
        "The statement must be a query, or the statement must be DML and AnalyzeMode must be PLAN or PROFILE");
    try (Scope ignore = span.makeCurrent()) {
      checkAndMarkUsed();

      if (statement.isUpdate()) {
        if (analyzeMode != AnalyzeMode.NONE) {
          return analyzeTransactionalUpdateAsync(callType, statement, analyzeMode);
        }
        // DML with returning clause.
        return executeDmlReturningAsync(callType, statement, options);
      }

      // Do not use a read-only staleness for internal metadata queries.
      final ReadOnlyTransaction currentTransaction =
          internalMetdataQuery
              ? dbClient.singleUseReadOnlyTransaction()
              : dbClient.singleUseReadOnlyTransaction(readOnlyStaleness);
      Callable<ResultSet> callable =
          () -> {
            try {
              ResultSet rs;
              if (analyzeMode == AnalyzeMode.NONE) {
                rs = currentTransaction.executeQuery(statement.getStatement(), options);
              } else {
                rs =
                    currentTransaction.analyzeQuery(
                        statement.getStatement(), analyzeMode.getQueryAnalyzeMode());
              }
              // Return a DirectExecuteResultSet, which will directly do a next() call in order to
              // ensure that the query is actually sent to Spanner.
              ResultSet directRs = DirectExecuteResultSet.ofResultSet(rs);
              state = UnitOfWorkState.COMMITTED;
              readTimestamp.set(currentTransaction.getReadTimestamp());
              return directRs;
            } catch (Throwable t) {
              state = UnitOfWorkState.COMMIT_FAILED;
              readTimestamp.set(null);
              currentTransaction.close();
              throw t;
            }
          };
      readTimestamp = SettableApiFuture.create();
      return executeStatementAsync(
          callType, statement, callable, SpannerGrpc.getExecuteStreamingSqlMethod());
    }
  }

  private ApiFuture<ResultSet> executeDmlReturningAsync(
      CallType callType, final ParsedStatement update, QueryOption... options) {
    Callable<ResultSet> callable =
        () -> {
          try {
            writeTransaction = createWriteTransaction();
            ResultSet resultSet =
                writeTransaction.run(
                    transaction ->
                        DirectExecuteResultSet.ofResultSet(
                            transaction.executeQuery(update.getStatement(), options)));
            state = UnitOfWorkState.COMMITTED;
            return resultSet;
          } catch (Throwable t) {
            state = UnitOfWorkState.COMMIT_FAILED;
            throw t;
          }
        };
    return executeStatementAsync(
        callType,
        update,
        callable,
        ImmutableList.of(SpannerGrpc.getExecuteSqlMethod(), SpannerGrpc.getCommitMethod()));
  }

  @Override
  public ApiFuture<ResultSet> partitionQueryAsync(
      CallType callType,
      ParsedStatement query,
      PartitionOptions partitionOptions,
      QueryOption... options) {
    try (Scope ignore = span.makeCurrent()) {
      Callable<ResultSet> callable =
          () -> {
            try (BatchReadOnlyTransaction transaction =
                batchClient.batchReadOnlyTransaction(readOnlyStaleness)) {
              ResultSet resultSet = partitionQuery(transaction, partitionOptions, query, options);
              readTimestamp.set(transaction.getReadTimestamp());
              state = UnitOfWorkState.COMMITTED;
              return resultSet;
            } catch (Throwable throwable) {
              state = UnitOfWorkState.COMMIT_FAILED;
              readTimestamp.set(null);
              throw throwable;
            }
          };
      readTimestamp = SettableApiFuture.create();
      return executeStatementAsync(
          callType,
          query,
          callable,
          ImmutableList.of(SpannerGrpc.getExecuteSqlMethod(), SpannerGrpc.getCommitMethod()));
    }
  }

  @Override
  public Timestamp getReadTimestamp() {
    ConnectionPreconditions.checkState(
        SpannerApiFutures.getOrNull(readTimestamp) != null,
        "There is no read timestamp available for this transaction.");
    return SpannerApiFutures.get(readTimestamp);
  }

  @Override
  public Timestamp getReadTimestampOrNull() {
    return SpannerApiFutures.getOrNull(readTimestamp);
  }

  private boolean hasCommitResponse() {
    return state == UnitOfWorkState.COMMITTED && writeTransaction != null;
  }

  @Override
  public Timestamp getCommitTimestamp() {
    ConnectionPreconditions.checkState(
        hasCommitResponse(), "There is no commit timestamp available for this transaction.");
    return getCommitResponse().getCommitTimestamp();
  }

  @Override
  public Timestamp getCommitTimestampOrNull() {
    CommitResponse response = getCommitResponseOrNull();
    return response == null ? null : response.getCommitTimestamp();
  }

  @Override
  public CommitResponse getCommitResponse() {
    ConnectionPreconditions.checkState(
        hasCommitResponse(), "There is no commit response available for this transaction.");
    return writeTransaction.getCommitResponse();
  }

  @Override
  public CommitResponse getCommitResponseOrNull() {
    if (hasCommitResponse()) {
      try {
        return writeTransaction.getCommitResponse();
      } catch (SpannerException e) {
        // ignore
      }
    }
    return null;
  }

  @Override
  public ApiFuture<Void> executeDdlAsync(CallType callType, final ParsedStatement ddl) {
    Preconditions.checkNotNull(ddl);
    Preconditions.checkArgument(
        ddl.getType() == StatementType.DDL, "Statement is not a ddl statement");
    ConnectionPreconditions.checkState(
        !isReadOnly(), "DDL statements are not allowed in read-only mode");
    try (Scope ignore = span.makeCurrent()) {
      checkAndMarkUsed();
      span.setAttribute(DB_STATEMENT_KEY, ddl.getStatement().getSql());

      Callable<Void> callable =
          () -> {
            try {
              OperationFuture<?, ?> operation;
              if (DdlClient.isCreateDatabaseStatement(ddl.getSqlWithoutComments())) {
                operation =
                    ddlClient.executeCreateDatabase(
                        ddl.getSqlWithoutComments(), dbClient.getDialect());
              } else {
                operation = ddlClient.executeDdl(ddl.getSqlWithoutComments(), protoDescriptors);
              }
              getWithStatementTimeout(operation, ddl);
              state = UnitOfWorkState.COMMITTED;
              return null;
            } catch (Throwable t) {
              state = UnitOfWorkState.COMMIT_FAILED;
              throw t;
            }
          };
      return executeStatementAsync(
          callType, ddl, callable, DatabaseAdminGrpc.getUpdateDatabaseDdlMethod());
    }
  }

  @Override
  public ApiFuture<Long> executeUpdateAsync(
      CallType callType, ParsedStatement update, UpdateOption... options) {
    Preconditions.checkNotNull(update);
    Preconditions.checkArgument(update.isUpdate(), "Statement is not an update statement");
    ConnectionPreconditions.checkState(
        !isReadOnly(), "Update statements are not allowed in read-only mode");
    try (Scope ignore = span.makeCurrent()) {
      checkAndMarkUsed();

      ApiFuture<Long> res;
      switch (autocommitDmlMode) {
        case TRANSACTIONAL:
          res =
              ApiFutures.transform(
                  executeTransactionalUpdateAsync(callType, update, AnalyzeMode.NONE, options),
                  Tuple::x,
                  MoreExecutors.directExecutor());
          break;
        case PARTITIONED_NON_ATOMIC:
          res = executePartitionedUpdateAsync(callType, update, options);
          break;
        default:
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.FAILED_PRECONDITION, "Unknown dml mode: " + autocommitDmlMode);
      }
      return res;
    }
  }

  @Override
  public ApiFuture<ResultSet> analyzeUpdateAsync(
      CallType callType, ParsedStatement update, AnalyzeMode analyzeMode, UpdateOption... options) {
    Preconditions.checkNotNull(update);
    Preconditions.checkArgument(update.isUpdate(), "Statement is not an update statement");
    ConnectionPreconditions.checkState(
        !isReadOnly(), "Update statements are not allowed in read-only mode");
    ConnectionPreconditions.checkState(
        autocommitDmlMode != AutocommitDmlMode.PARTITIONED_NON_ATOMIC,
        "Analyzing update statements is not supported for Partitioned DML");
    try (Scope ignore = span.makeCurrent()) {
      checkAndMarkUsed();

      return ApiFutures.transform(
          executeTransactionalUpdateAsync(callType, update, analyzeMode, options),
          Tuple::y,
          MoreExecutors.directExecutor());
    }
  }

  @Override
  public ApiFuture<long[]> executeBatchUpdateAsync(
      CallType callType, Iterable<ParsedStatement> updates, UpdateOption... options) {
    Preconditions.checkNotNull(updates);
    for (ParsedStatement update : updates) {
      Preconditions.checkArgument(
          update.isUpdate(),
          "Statement is not an update statement: " + update.getSqlWithoutComments());
    }
    ConnectionPreconditions.checkState(
        !isReadOnly(), "Batch update statements are not allowed in read-only mode");
    try (Scope ignore = span.makeCurrent()) {
      checkAndMarkUsed();

      switch (autocommitDmlMode) {
        case TRANSACTIONAL:
          return executeTransactionalBatchUpdateAsync(callType, updates, options);
        case PARTITIONED_NON_ATOMIC:
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.FAILED_PRECONDITION,
              "Batch updates are not allowed in " + autocommitDmlMode);
        default:
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.FAILED_PRECONDITION, "Unknown dml mode: " + autocommitDmlMode);
      }
    }
  }

  private TransactionRunner createWriteTransaction() {
    int numOptions = 0;
    if (this.rpcPriority != null) {
      numOptions++;
    }
    if (returnCommitStats) {
      numOptions++;
    }
    if (excludeTxnFromChangeStreams) {
      numOptions++;
    }
    if (maxCommitDelay != null) {
      numOptions++;
    }
    if (numOptions == 0) {
      return dbClient.readWriteTransaction();
    }
    Options.TransactionOption[] options = new Options.TransactionOption[numOptions];
    int index = 0;
    if (this.rpcPriority != null) {
      options[index++] = Options.priority(this.rpcPriority);
    }
    if (returnCommitStats) {
      options[index++] = Options.commitStats();
    }
    if (excludeTxnFromChangeStreams) {
      options[index++] = Options.excludeTxnFromChangeStreams();
    }
    if (maxCommitDelay != null) {
      options[index++] = Options.maxCommitDelay(maxCommitDelay);
    }
    return dbClient.readWriteTransaction(options);
  }

  private ApiFuture<Tuple<Long, ResultSet>> executeTransactionalUpdateAsync(
      CallType callType,
      final ParsedStatement update,
      AnalyzeMode analyzeMode,
      final UpdateOption... options) {
    Callable<Tuple<Long, ResultSet>> callable =
        () -> {
          try {
            writeTransaction = createWriteTransaction();
            Tuple<Long, ResultSet> res =
                writeTransaction.run(
                    transaction -> {
                      if (analyzeMode == AnalyzeMode.NONE) {
                        return Tuple.of(
                            transaction.executeUpdate(update.getStatement(), options), null);
                      }
                      ResultSet resultSet =
                          transaction.analyzeUpdateStatement(
                              update.getStatement(), analyzeMode.getQueryAnalyzeMode(), options);
                      return Tuple.of(null, resultSet);
                    });
            state = UnitOfWorkState.COMMITTED;
            return res;
          } catch (Throwable t) {
            state = UnitOfWorkState.COMMIT_FAILED;
            throw t;
          }
        };
    return executeStatementAsync(
        callType,
        update,
        callable,
        ImmutableList.of(SpannerGrpc.getExecuteSqlMethod(), SpannerGrpc.getCommitMethod()));
  }

  private ApiFuture<ResultSet> analyzeTransactionalUpdateAsync(
      CallType callType, final ParsedStatement update, AnalyzeMode analyzeMode) {
    Callable<ResultSet> callable =
        () -> {
          try {
            writeTransaction = createWriteTransaction();
            ResultSet resultSet =
                writeTransaction.run(
                    transaction ->
                        DirectExecuteResultSet.ofResultSet(
                            transaction.analyzeQuery(
                                update.getStatement(), analyzeMode.getQueryAnalyzeMode())));
            state = UnitOfWorkState.COMMITTED;
            return resultSet;
          } catch (Throwable t) {
            state = UnitOfWorkState.COMMIT_FAILED;
            throw t;
          }
        };
    return executeStatementAsync(
        callType,
        update,
        callable,
        ImmutableList.of(SpannerGrpc.getExecuteSqlMethod(), SpannerGrpc.getCommitMethod()));
  }

  private ApiFuture<Long> executePartitionedUpdateAsync(
      CallType callType, final ParsedStatement update, final UpdateOption... options) {
    final UpdateOption[] effectiveOptions;
    if (excludeTxnFromChangeStreams) {
      if (options.length == 0) {
        effectiveOptions = new UpdateOption[] {Options.excludeTxnFromChangeStreams()};
      } else {
        effectiveOptions = Arrays.copyOf(options, options.length + 1);
        effectiveOptions[effectiveOptions.length - 1] = Options.excludeTxnFromChangeStreams();
      }
    } else {
      effectiveOptions = options;
    }
    Callable<Long> callable =
        () -> {
          try {
            Long res = dbClient.executePartitionedUpdate(update.getStatement(), effectiveOptions);
            state = UnitOfWorkState.COMMITTED;
            return res;
          } catch (Throwable t) {
            state = UnitOfWorkState.COMMIT_FAILED;
            throw t;
          }
        };
    return executeStatementAsync(
        callType, update, callable, SpannerGrpc.getExecuteStreamingSqlMethod());
  }

  private ApiFuture<long[]> executeTransactionalBatchUpdateAsync(
      final CallType callType,
      final Iterable<ParsedStatement> updates,
      final UpdateOption... options) {
    Callable<long[]> callable =
        () -> {
          writeTransaction = createWriteTransaction();
          return writeTransaction.run(
              transaction -> {
                try {
                  long[] res =
                      transaction.batchUpdate(
                          Iterables.transform(updates, ParsedStatement::getStatement), options);
                  state = UnitOfWorkState.COMMITTED;
                  return res;
                } catch (Throwable t) {
                  if (t instanceof SpannerBatchUpdateException) {
                    // Batch update exceptions does not cause a rollback.
                    state = UnitOfWorkState.COMMITTED;
                  } else {
                    state = UnitOfWorkState.COMMIT_FAILED;
                  }
                  throw t;
                }
              });
        };
    return executeStatementAsync(
        callType, RUN_BATCH_STATEMENT, callable, SpannerGrpc.getExecuteBatchDmlMethod());
  }

  @Override
  public ApiFuture<Void> writeAsync(CallType callType, final Iterable<Mutation> mutations) {
    Preconditions.checkNotNull(mutations);
    ConnectionPreconditions.checkState(
        !isReadOnly(), "Update statements are not allowed in read-only mode");
    try (Scope ignore = span.makeCurrent()) {
      checkAndMarkUsed();

      Callable<Void> callable =
          () -> {
            try {
              writeTransaction = createWriteTransaction();
              Void res =
                  writeTransaction.run(
                      transaction -> {
                        transaction.buffer(mutations);
                        return null;
                      });
              state = UnitOfWorkState.COMMITTED;
              return res;
            } catch (Throwable t) {
              state = UnitOfWorkState.COMMIT_FAILED;
              throw t;
            }
          };
      return executeStatementAsync(
          callType, COMMIT_STATEMENT, callable, SpannerGrpc.getCommitMethod());
    }
  }

  @Override
  public ApiFuture<Void> commitAsync(
      @Nonnull CallType callType, @Nonnull EndTransactionCallback callback) {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Commit is not supported for single-use transactions");
  }

  @Override
  public ApiFuture<Void> rollbackAsync(
      @Nonnull CallType callType, @Nonnull EndTransactionCallback callback) {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Rollback is not supported for single-use transactions");
  }

  @Override
  String getUnitOfWorkName() {
    return "single-use transaction";
  }

  @Override
  public ApiFuture<long[]> runBatchAsync(CallType callType) {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Run batch is not supported for single-use transactions");
  }

  @Override
  public void abortBatch() {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Run batch is not supported for single-use transactions");
  }
}
