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

import com.google.api.core.ApiFuture;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.CommitResponse;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerApiFutures;
import com.google.cloud.spanner.SpannerBatchUpdateException;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionRunner;
import com.google.cloud.spanner.connection.StatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.StatementParser.StatementType;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.spanner.admin.database.v1.DatabaseAdminGrpc;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.v1.SpannerGrpc;
import java.util.concurrent.Callable;

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
  private final TimestampBound readOnlyStaleness;
  private final AutocommitDmlMode autocommitDmlMode;
  private final boolean returnCommitStats;
  private volatile SettableApiFuture<Timestamp> readTimestamp = null;
  private volatile TransactionRunner writeTransaction;
  private boolean used = false;
  private volatile UnitOfWorkState state = UnitOfWorkState.STARTED;

  static class Builder extends AbstractBaseUnitOfWork.Builder<Builder, SingleUseTransaction> {
    private DdlClient ddlClient;
    private DatabaseClient dbClient;
    private boolean readOnly;
    private TimestampBound readOnlyStaleness;
    private AutocommitDmlMode autocommitDmlMode;
    private boolean returnCommitStats;

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

    @Override
    SingleUseTransaction build() {
      Preconditions.checkState(ddlClient != null, "No DDL client specified");
      Preconditions.checkState(dbClient != null, "No DatabaseClient client specified");
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
    this.readOnly = builder.readOnly;
    this.readOnlyStaleness = builder.readOnlyStaleness;
    this.autocommitDmlMode = builder.autocommitDmlMode;
    this.returnCommitStats = builder.returnCommitStats;
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

  private void checkAndMarkUsed() {
    Preconditions.checkState(!used, "This single-use transaction has already been used");
    used = true;
  }

  @Override
  public ApiFuture<ResultSet> executeQueryAsync(
      final ParsedStatement statement,
      final AnalyzeMode analyzeMode,
      final QueryOption... options) {
    Preconditions.checkNotNull(statement);
    Preconditions.checkArgument(statement.isQuery(), "Statement is not a query");
    checkAndMarkUsed();

    final ReadOnlyTransaction currentTransaction =
        dbClient.singleUseReadOnlyTransaction(readOnlyStaleness);
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
    return executeStatementAsync(statement, callable, SpannerGrpc.getExecuteStreamingSqlMethod());
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
  public ApiFuture<Void> executeDdlAsync(final ParsedStatement ddl) {
    Preconditions.checkNotNull(ddl);
    Preconditions.checkArgument(
        ddl.getType() == StatementType.DDL, "Statement is not a ddl statement");
    ConnectionPreconditions.checkState(
        !isReadOnly(), "DDL statements are not allowed in read-only mode");
    checkAndMarkUsed();

    Callable<Void> callable =
        () -> {
          try {
            OperationFuture<Void, UpdateDatabaseDdlMetadata> operation =
                ddlClient.executeDdl(ddl.getSqlWithoutComments());
            Void res = getWithStatementTimeout(operation, ddl);
            state = UnitOfWorkState.COMMITTED;
            return res;
          } catch (Throwable t) {
            state = UnitOfWorkState.COMMIT_FAILED;
            throw t;
          }
        };
    return executeStatementAsync(ddl, callable, DatabaseAdminGrpc.getUpdateDatabaseDdlMethod());
  }

  @Override
  public ApiFuture<Long> executeUpdateAsync(ParsedStatement update) {
    Preconditions.checkNotNull(update);
    Preconditions.checkArgument(update.isUpdate(), "Statement is not an update statement");
    ConnectionPreconditions.checkState(
        !isReadOnly(), "Update statements are not allowed in read-only mode");
    checkAndMarkUsed();

    ApiFuture<Long> res;
    switch (autocommitDmlMode) {
      case TRANSACTIONAL:
        res = executeTransactionalUpdateAsync(update);
        break;
      case PARTITIONED_NON_ATOMIC:
        res = executePartitionedUpdateAsync(update);
        break;
      default:
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.FAILED_PRECONDITION, "Unknown dml mode: " + autocommitDmlMode);
    }
    return res;
  }

  private final ParsedStatement executeBatchUpdateStatement =
      StatementParser.INSTANCE.parse(Statement.of("RUN BATCH"));

  @Override
  public ApiFuture<long[]> executeBatchUpdateAsync(Iterable<ParsedStatement> updates) {
    Preconditions.checkNotNull(updates);
    for (ParsedStatement update : updates) {
      Preconditions.checkArgument(
          update.isUpdate(),
          "Statement is not an update statement: " + update.getSqlWithoutComments());
    }
    ConnectionPreconditions.checkState(
        !isReadOnly(), "Batch update statements are not allowed in read-only mode");
    checkAndMarkUsed();

    switch (autocommitDmlMode) {
      case TRANSACTIONAL:
        return executeTransactionalBatchUpdateAsync(updates);
      case PARTITIONED_NON_ATOMIC:
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.FAILED_PRECONDITION, "Batch updates are not allowed in " + autocommitDmlMode);
      default:
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.FAILED_PRECONDITION, "Unknown dml mode: " + autocommitDmlMode);
    }
  }

  private TransactionRunner createWriteTransaction() {
    return returnCommitStats
        ? dbClient.readWriteTransaction(Options.commitStats())
        : dbClient.readWriteTransaction();
  }

  private ApiFuture<Long> executeTransactionalUpdateAsync(final ParsedStatement update) {
    Callable<Long> callable =
        () -> {
          try {
            writeTransaction = createWriteTransaction();
            Long res =
                writeTransaction.run(
                    transaction -> transaction.executeUpdate(update.getStatement()));
            state = UnitOfWorkState.COMMITTED;
            return res;
          } catch (Throwable t) {
            state = UnitOfWorkState.COMMIT_FAILED;
            throw t;
          }
        };
    return executeStatementAsync(
        update,
        callable,
        ImmutableList.of(SpannerGrpc.getExecuteSqlMethod(), SpannerGrpc.getCommitMethod()));
  }

  private ApiFuture<Long> executePartitionedUpdateAsync(final ParsedStatement update) {
    Callable<Long> callable =
        () -> {
          try {
            Long res = dbClient.executePartitionedUpdate(update.getStatement());
            state = UnitOfWorkState.COMMITTED;
            return res;
          } catch (Throwable t) {
            state = UnitOfWorkState.COMMIT_FAILED;
            throw t;
          }
        };
    return executeStatementAsync(update, callable, SpannerGrpc.getExecuteStreamingSqlMethod());
  }

  private ApiFuture<long[]> executeTransactionalBatchUpdateAsync(
      final Iterable<ParsedStatement> updates) {
    Callable<long[]> callable =
        () -> {
          writeTransaction = createWriteTransaction();
          return writeTransaction.run(
              transaction -> {
                try {
                  long[] res =
                      transaction.batchUpdate(
                          Iterables.transform(updates, ParsedStatement::getStatement));
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
        executeBatchUpdateStatement, callable, SpannerGrpc.getExecuteBatchDmlMethod());
  }

  private final ParsedStatement commitStatement =
      StatementParser.INSTANCE.parse(Statement.of("COMMIT"));

  @Override
  public ApiFuture<Void> writeAsync(final Iterable<Mutation> mutations) {
    Preconditions.checkNotNull(mutations);
    ConnectionPreconditions.checkState(
        !isReadOnly(), "Update statements are not allowed in read-only mode");
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
    return executeStatementAsync(commitStatement, callable, SpannerGrpc.getCommitMethod());
  }

  @Override
  public ApiFuture<Void> commitAsync() {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Commit is not supported for single-use transactions");
  }

  @Override
  public ApiFuture<Void> rollbackAsync() {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Rollback is not supported for single-use transactions");
  }

  @Override
  public ApiFuture<long[]> runBatchAsync() {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Run batch is not supported for single-use transactions");
  }

  @Override
  public void abortBatch() {
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.FAILED_PRECONDITION, "Run batch is not supported for single-use transactions");
  }
}
