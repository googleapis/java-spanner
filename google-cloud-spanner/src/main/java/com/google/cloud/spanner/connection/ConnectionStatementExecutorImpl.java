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

import static com.google.cloud.spanner.connection.DialectNamespaceMapper.getNamespace;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.ABORT_BATCH;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.BEGIN;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.COMMIT;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.ROLLBACK;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.RUN_BATCH;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_AUTOCOMMIT;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_AUTOCOMMIT_DML_MODE;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_OPTIMIZER_STATISTICS_PACKAGE;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_OPTIMIZER_VERSION;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_READONLY;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_READ_ONLY_STALENESS;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_RETRY_ABORTS_INTERNALLY;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_RETURN_COMMIT_STATS;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_RPC_PRIORITY;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_STATEMENT_TAG;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_STATEMENT_TIMEOUT;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_TRANSACTION_MODE;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SET_TRANSACTION_TAG;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_AUTOCOMMIT;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_AUTOCOMMIT_DML_MODE;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_COMMIT_RESPONSE;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_COMMIT_TIMESTAMP;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_OPTIMIZER_STATISTICS_PACKAGE;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_OPTIMIZER_VERSION;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_READONLY;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_READ_ONLY_STALENESS;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_READ_TIMESTAMP;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_RETRY_ABORTS_INTERNALLY;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_RETURN_COMMIT_STATS;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_RPC_PRIORITY;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_STATEMENT_TAG;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_STATEMENT_TIMEOUT;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_TRANSACTION_ISOLATION_LEVEL;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.SHOW_TRANSACTION_TAG;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.START_BATCH_DDL;
import static com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType.START_BATCH_DML;
import static com.google.cloud.spanner.connection.StatementResultImpl.noResult;
import static com.google.cloud.spanner.connection.StatementResultImpl.resultSet;

import com.google.cloud.spanner.CommitResponse;
import com.google.cloud.spanner.CommitStats;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.ResultSets;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.StructField;
import com.google.cloud.spanner.connection.ReadOnlyStalenessUtil.DurationValueGetter;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Duration;
import com.google.spanner.v1.PlanNode;
import com.google.spanner.v1.QueryPlan;
import com.google.spanner.v1.RequestOptions;
import com.google.spanner.v1.RequestOptions.Priority;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * The methods in this class are called by the different {@link ClientSideStatement}s. These method
 * calls are then forwarded into a {@link Connection}.
 */
class ConnectionStatementExecutorImpl implements ConnectionStatementExecutor {

  static final class StatementTimeoutGetter implements DurationValueGetter {
    private final Connection connection;

    public StatementTimeoutGetter(Connection connection) {
      this.connection = connection;
    }

    @Override
    public long getDuration(TimeUnit unit) {
      return connection.getStatementTimeout(unit);
    }

    @Override
    public boolean hasDuration() {
      return connection.hasStatementTimeout();
    }
  }

  private static final Map<Priority, RpcPriority> validRPCPriorityValues;

  static {
    ImmutableMap.Builder<Priority, RpcPriority> builder = ImmutableMap.builder();
    builder.put(Priority.PRIORITY_HIGH, RpcPriority.HIGH);
    builder.put(Priority.PRIORITY_MEDIUM, RpcPriority.MEDIUM);
    builder.put(Priority.PRIORITY_LOW, RpcPriority.LOW);
    validRPCPriorityValues = builder.build();
  }

  /** The connection to execute the statements on. */
  private final ConnectionImpl connection;

  ConnectionStatementExecutorImpl(ConnectionImpl connection) {
    this.connection = connection;
  }

  ConnectionImpl getConnection() {
    return connection;
  }

  @Override
  public StatementResult statementSetAutocommit(Boolean autocommit) {
    Preconditions.checkNotNull(autocommit);
    getConnection().setAutocommit(autocommit);
    return noResult(SET_AUTOCOMMIT);
  }

  @Override
  public StatementResult statementShowAutocommit() {
    return resultSet("AUTOCOMMIT", getConnection().isAutocommit(), SHOW_AUTOCOMMIT);
  }

  @Override
  public StatementResult statementSetReadOnly(Boolean readOnly) {
    Preconditions.checkNotNull(readOnly);
    getConnection().setReadOnly(readOnly);
    return noResult(SET_READONLY);
  }

  @Override
  public StatementResult statementShowReadOnly() {
    return StatementResultImpl.resultSet(
        String.format("%sREADONLY", getNamespace(connection.getDialect())),
        getConnection().isReadOnly(),
        SHOW_READONLY);
  }

  @Override
  public StatementResult statementSetRetryAbortsInternally(Boolean retryAbortsInternally) {
    Preconditions.checkNotNull(retryAbortsInternally);
    getConnection().setRetryAbortsInternally(retryAbortsInternally);
    return noResult(SET_RETRY_ABORTS_INTERNALLY);
  }

  @Override
  public StatementResult statementShowRetryAbortsInternally() {
    return StatementResultImpl.resultSet(
        String.format("%sRETRY_ABORTS_INTERNALLY", getNamespace(connection.getDialect())),
        getConnection().isRetryAbortsInternally(),
        SHOW_RETRY_ABORTS_INTERNALLY);
  }

  @Override
  public StatementResult statementSetAutocommitDmlMode(AutocommitDmlMode mode) {
    getConnection().setAutocommitDmlMode(mode);
    return noResult(SET_AUTOCOMMIT_DML_MODE);
  }

  @Override
  public StatementResult statementShowAutocommitDmlMode() {
    return resultSet(
        String.format("%sAUTOCOMMIT_DML_MODE", getNamespace(connection.getDialect())),
        getConnection().getAutocommitDmlMode(),
        SHOW_AUTOCOMMIT_DML_MODE);
  }

  @Override
  public StatementResult statementSetStatementTimeout(Duration duration) {
    if (duration.getSeconds() == 0L && duration.getNanos() == 0) {
      getConnection().clearStatementTimeout();
    } else {
      TimeUnit unit =
          ReadOnlyStalenessUtil.getAppropriateTimeUnit(
              new ReadOnlyStalenessUtil.DurationGetter(duration));
      getConnection()
          .setStatementTimeout(ReadOnlyStalenessUtil.durationToUnits(duration, unit), unit);
    }
    return noResult(SET_STATEMENT_TIMEOUT);
  }

  @Override
  public StatementResult statementShowStatementTimeout() {
    return resultSet(
        "STATEMENT_TIMEOUT",
        getConnection().hasStatementTimeout()
            ? ReadOnlyStalenessUtil.durationToString(new StatementTimeoutGetter(getConnection()))
            : connection.getDialect() == Dialect.POSTGRESQL ? "0" : null,
        SHOW_STATEMENT_TIMEOUT);
  }

  @Override
  public StatementResult statementShowReadTimestamp() {
    return resultSet(
        String.format("%sREAD_TIMESTAMP", getNamespace(connection.getDialect())),
        getConnection().getReadTimestampOrNull(),
        SHOW_READ_TIMESTAMP);
  }

  @Override
  public StatementResult statementShowCommitTimestamp() {
    return resultSet(
        String.format("%sCOMMIT_TIMESTAMP", getNamespace(connection.getDialect())),
        getConnection().getCommitTimestampOrNull(),
        SHOW_COMMIT_TIMESTAMP);
  }

  @Override
  public StatementResult statementShowCommitResponse() {
    CommitResponse response = getConnection().getCommitResponseOrNull();
    CommitStats stats = null;
    if (response != null && response.hasCommitStats()) {
      stats = response.getCommitStats();
    }
    ResultSet resultSet =
        ResultSets.forRows(
            Type.struct(
                StructField.of(
                    String.format("%sCOMMIT_TIMESTAMP", getNamespace(connection.getDialect())),
                    Type.timestamp()),
                StructField.of(
                    String.format("%sMUTATION_COUNT", getNamespace(connection.getDialect())),
                    Type.int64())),
            Collections.singletonList(
                Struct.newBuilder()
                    .set(String.format("%sCOMMIT_TIMESTAMP", getNamespace(connection.getDialect())))
                    .to(response == null ? null : response.getCommitTimestamp())
                    .set(String.format("%sMUTATION_COUNT", getNamespace(connection.getDialect())))
                    .to(stats == null ? null : stats.getMutationCount())
                    .build()));
    return StatementResultImpl.of(resultSet, SHOW_COMMIT_RESPONSE);
  }

  @Override
  public StatementResult statementSetReadOnlyStaleness(TimestampBound staleness) {
    getConnection().setReadOnlyStaleness(staleness);
    return noResult(SET_READ_ONLY_STALENESS);
  }

  @Override
  public StatementResult statementShowReadOnlyStaleness() {
    TimestampBound staleness = getConnection().getReadOnlyStaleness();
    return resultSet(
        String.format("%sREAD_ONLY_STALENESS", getNamespace(connection.getDialect())),
        ReadOnlyStalenessUtil.timestampBoundToString(staleness),
        SHOW_READ_ONLY_STALENESS);
  }

  @Override
  public StatementResult statementSetOptimizerVersion(String optimizerVersion) {
    getConnection().setOptimizerVersion(optimizerVersion);
    return noResult(SET_OPTIMIZER_VERSION);
  }

  @Override
  public StatementResult statementShowOptimizerVersion() {
    return resultSet(
        String.format("%sOPTIMIZER_VERSION", getNamespace(connection.getDialect())),
        getConnection().getOptimizerVersion(),
        SHOW_OPTIMIZER_VERSION);
  }

  @Override
  public StatementResult statementSetOptimizerStatisticsPackage(String optimizerStatisticsPackage) {
    getConnection().setOptimizerStatisticsPackage(optimizerStatisticsPackage);
    return noResult(SET_OPTIMIZER_STATISTICS_PACKAGE);
  }

  @Override
  public StatementResult statementShowOptimizerStatisticsPackage() {
    return resultSet(
        String.format("%sOPTIMIZER_STATISTICS_PACKAGE", getNamespace(connection.getDialect())),
        getConnection().getOptimizerStatisticsPackage(),
        SHOW_OPTIMIZER_STATISTICS_PACKAGE);
  }

  @Override
  public StatementResult statementSetReturnCommitStats(Boolean returnCommitStats) {
    getConnection().setReturnCommitStats(returnCommitStats);
    return noResult(SET_RETURN_COMMIT_STATS);
  }

  @Override
  public StatementResult statementShowReturnCommitStats() {
    return resultSet(
        String.format("%sRETURN_COMMIT_STATS", getNamespace(connection.getDialect())),
        getConnection().isReturnCommitStats(),
        SHOW_RETURN_COMMIT_STATS);
  }

  @Override
  public StatementResult statementSetStatementTag(String tag) {
    getConnection().setStatementTag("".equals(tag) ? null : tag);
    return noResult(SET_STATEMENT_TAG);
  }

  @Override
  public StatementResult statementShowStatementTag() {
    return resultSet(
        String.format("%sSTATEMENT_TAG", getNamespace(connection.getDialect())),
        MoreObjects.firstNonNull(getConnection().getStatementTag(), ""),
        SHOW_STATEMENT_TAG);
  }

  @Override
  public StatementResult statementSetTransactionTag(String tag) {
    getConnection().setTransactionTag("".equals(tag) ? null : tag);
    return noResult(SET_TRANSACTION_TAG);
  }

  @Override
  public StatementResult statementShowTransactionTag() {
    return resultSet(
        String.format("%sTRANSACTION_TAG", getNamespace(connection.getDialect())),
        MoreObjects.firstNonNull(getConnection().getTransactionTag(), ""),
        SHOW_TRANSACTION_TAG);
  }

  @Override
  public StatementResult statementBeginTransaction() {
    getConnection().beginTransaction();
    return noResult(BEGIN);
  }

  @Override
  public StatementResult statementBeginPgTransaction(@Nullable PgTransactionMode transactionMode) {
    getConnection().beginTransaction();
    if (transactionMode != null) {
      statementSetPgTransactionMode(transactionMode);
    }
    return noResult(BEGIN);
  }

  @Override
  public StatementResult statementCommit() {
    getConnection().commit();
    return noResult(COMMIT);
  }

  @Override
  public StatementResult statementRollback() {
    getConnection().rollback();
    return noResult(ROLLBACK);
  }

  @Override
  public StatementResult statementSetTransactionMode(TransactionMode mode) {
    getConnection().setTransactionMode(mode);
    return noResult(SET_TRANSACTION_MODE);
  }

  @Override
  public StatementResult statementSetPgTransactionMode(PgTransactionMode transactionMode) {
    switch (transactionMode) {
      case READ_ONLY_TRANSACTION:
        getConnection().setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
        break;
      case READ_WRITE_TRANSACTION:
        getConnection().setTransactionMode(TransactionMode.READ_WRITE_TRANSACTION);
        break;
      case ISOLATION_LEVEL_DEFAULT:
      case ISOLATION_LEVEL_SERIALIZABLE:
      default:
        // no-op
    }
    return noResult(SET_TRANSACTION_MODE);
  }

  @Override
  public StatementResult statementSetPgSessionCharacteristicsTransactionMode(
      PgTransactionMode transactionMode) {
    switch (transactionMode) {
      case READ_ONLY_TRANSACTION:
        getConnection().setReadOnly(true);
        break;
      case READ_WRITE_TRANSACTION:
        getConnection().setReadOnly(false);
        break;
      case ISOLATION_LEVEL_DEFAULT:
      case ISOLATION_LEVEL_SERIALIZABLE:
      default:
        // no-op
    }
    return noResult(SET_TRANSACTION_MODE);
  }

  @Override
  public StatementResult statementStartBatchDdl() {
    getConnection().startBatchDdl();
    return noResult(START_BATCH_DDL);
  }

  @Override
  public StatementResult statementStartBatchDml() {
    getConnection().startBatchDml();
    return noResult(START_BATCH_DML);
  }

  @Override
  public StatementResult statementRunBatch() {
    long[] updateCounts = getConnection().runBatch();
    return resultSet("UPDATE_COUNTS", updateCounts, RUN_BATCH);
  }

  @Override
  public StatementResult statementAbortBatch() {
    getConnection().abortBatch();
    return noResult(ABORT_BATCH);
  }

  @Override
  public StatementResult statementSetRPCPriority(Priority priority) {
    RpcPriority value = validRPCPriorityValues.get(priority);
    getConnection().setRPCPriority(value);
    return noResult(SET_RPC_PRIORITY);
  }

  @Override
  public StatementResult statementShowRPCPriority() {
    return resultSet(
        String.format("%sRPC_PRIORITY", getNamespace(connection.getDialect())),
        getConnection().getRPCPriority() == null
            ? RequestOptions.Priority.PRIORITY_UNSPECIFIED
            : getConnection().getRPCPriority(),
        SHOW_RPC_PRIORITY);
  }

  @Override
  public StatementResult statementShowTransactionIsolationLevel() {
    return resultSet("transaction_isolation", "serializable", SHOW_TRANSACTION_ISOLATION_LEVEL);
  }

  private String processQueryPlan(PlanNode planNode){
    String planNodeDescription = " : { ";
    com.google.protobuf.Struct metadata = planNode.getMetadata();

    for(String key : metadata.getFieldsMap().keySet()){
      planNodeDescription += key + " : " + metadata.getFieldsMap().get(key).getStringValue() + " , ";
    }
    planNodeDescription = planNodeDescription.substring(0, planNodeDescription.length()-3);
    planNodeDescription += " }";

    return planNodeDescription;
  }

  private String processExecutionStats(PlanNode planNode){
    String executionStats = "";
    for(String key : planNode.getExecutionStats().getFieldsMap().keySet()){
      executionStats += key + " : { ";

      com.google.protobuf.Struct value = planNode.getExecutionStats().getFieldsMap().get(key).getStructValue();
      for(String newKey : value.getFieldsMap().keySet()){
        String newValue = value.getFieldsMap().get(newKey).getStringValue();
        executionStats += newKey + " : " + newValue + " , ";
      }
      executionStats = executionStats.substring(0, executionStats.length()-3);
      executionStats += " } , ";
    }
    executionStats = executionStats.substring(0, executionStats.length()-3);

    return executionStats;
  }

  private StatementResult getStatementResultFromQueryPlan(QueryPlan queryPlan, boolean isAnalyse){
    ArrayList<Struct> list = new ArrayList<>();
    for(PlanNode planNode : queryPlan.getPlanNodesList()){
      String planNodeDescription = planNode.getDisplayName();
      String executionStats = "";

      if(!planNode.getMetadata().toString().equalsIgnoreCase("")){
        planNodeDescription += processQueryPlan(planNode);
      }

      if(!planNode.getShortRepresentation().toString().equalsIgnoreCase("")){
        planNodeDescription += " : " + planNode.getShortRepresentation().getDescription();
      }

      if(isAnalyse && !planNode.getExecutionStats().toString().equals("")){
        executionStats = processExecutionStats(planNode);
      }

      if(isAnalyse){
        list.add(Struct.newBuilder().set("QUERY PLAN").to(planNodeDescription).set("EXECUTION STATS").to(executionStats).build());
      }
      else{
        list.add(Struct.newBuilder().set("QUERY PLAN").to(planNodeDescription).build());
      }
    }

    ResultSet rs;
    if(isAnalyse) {
      rs = ResultSets.forRows(
          Type.struct(
              StructField.of("QUERY PLAN", Type.string()),
              StructField.of("EXECUTION STATS", Type.string())),
          list);
    }
    else{
      rs = ResultSets.forRows(
          Type.struct(StructField.of("QUERY PLAN", Type.string())),
          list);
    }
    return StatementResultImpl.of(rs);
  }

  private StatementResult executeStatement(String sql, QueryAnalyzeMode qam){
    Statement statement = Statement.newBuilder(sql).build();
    ResultSet rs = getConnection().analyzeQuery(statement, qam);
    while(rs.next()){
      continue;
    }

    if(rs.getStats() != null && rs.getStats().getQueryPlan() != null){
      return getStatementResultFromQueryPlan(rs.getStats().getQueryPlan(), qam.equals(QueryAnalyzeMode.PROFILE));
    }
    throw SpannerExceptionFactory.newSpannerException(ErrorCode.FAILED_PRECONDITION, String.format("Couldn't fetch stats for %s",sql));
  }

  /*
  * This method executes the given SQL string in either PLAN or PROFILE mode and returns
  * the query plan as a ResultSet containing a single String column with the query plan nodes.
  *
  * The only additional option that is supported is ANALYZE. The method will throw a SpannerException
  * if it is invoked with a statement that includes any other options.
  */
  @Override
  public StatementResult statementExplain(String sql) {
    if(sql == null){
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, String.format("Invalid String with Explain"));
    }

    if(sql.charAt(0) == '('){
      int index = sql.indexOf(')');

      String options[] = sql.substring(1, index).split("\\s*,\\s*");
      boolean isAnalyse = false, startAfterIndex = false;
      for(String option : options){
        String optionExpression[] = option.split("\\s+");
        if(optionExpression.length >= 3){
          isAnalyse = false;
          break;
        }
        else if(ClientSideStatementExplainExecutor.EXPLAIN_OPTIONS.contains(optionExpression[0].toLowerCase())){
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.UNIMPLEMENTED, String.format("%s is not implemented yet", optionExpression[0]));
        }
        else if(optionExpression[0].equalsIgnoreCase("analyse") || optionExpression[0].equalsIgnoreCase("analyze")){
          isAnalyse = true;
        }
        else{
          isAnalyse = false;
          break;
        }

        if(optionExpression.length == 2){
          if(optionExpression[1].equalsIgnoreCase("false") || optionExpression[1].equalsIgnoreCase("0") || optionExpression[1].equalsIgnoreCase("off")){
            isAnalyse = false;
            startAfterIndex = true;
          }
          else if(!(optionExpression[1].equalsIgnoreCase("true") || optionExpression[1].equalsIgnoreCase("1") || optionExpression[1].equalsIgnoreCase("on"))){
            isAnalyse = false;
            break;
          }
        }
      }
      if(isAnalyse){
        return executeStatement(sql.substring(index+1), QueryAnalyzeMode.PROFILE);
      }
      else if(startAfterIndex){
        return executeStatement(sql.substring(index+1), QueryAnalyzeMode.PLAN);
      }
      else{
        return executeStatement(sql, QueryAnalyzeMode.PLAN);
      }
    }
    else {
      String[] arr = sql.split("\\s+", 2);
      if (arr.length >= 2) {
        String option = arr[0].toLowerCase();
        String statementToBeExplained = arr[1];

        if (ClientSideStatementExplainExecutor.EXPLAIN_OPTIONS.contains(option)) {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.UNIMPLEMENTED, String.format("%s is not implemented yet", option));
        } else if (option.equals("analyze") || option.equals("analyse")) {
          return executeStatement(statementToBeExplained, QueryAnalyzeMode.PROFILE);
        }
      }
      return executeStatement(sql, QueryAnalyzeMode.PLAN);
    }
  }
}
