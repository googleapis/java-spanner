package com.google.cloud.spanner;

import java.util.List;
import java.util.Map;

public interface SimpleClient {

  /**
   * Execute SQL Query. Query would run using a strong read.
   *
   * @param query
   * @return
   */
  ResultSet executeSqlQuery(String query);

  /**
   * Execute Snapshot SQL Query. Query would run using a snapshot with staleness of 5 seconds.
   *
   * @param query
   * @return
   */
  ResultSet executeSnapshotSqlQuery(String query);

  /**
   * Execute SQL Query using TimestampBound.
   *
   * @param query
   * @param timestampBound
   * @return
   */
  ResultSet executeSqlQuery(String query, TimestampBound timestampBound);

  /**
   * Execute SQL Query using TimestampBound. Accept Map<String, Value> as named parameters.
   *
   * @param query
   * @param args
   * @param timestampBound
   * @return
   */
  ResultSet executeSqlQuery(String query, Map<String, Value> args, TimestampBound timestampBound);

  /**
   * Execute SQL Query. Query would run using a strong read. Accept Map<String, Value> as named
   * parameters.
   *
   * @param query
   * @param args
   * @return
   */
  ResultSet executeSqlQuery(String query, Map<String, Value> args);

  /**
   * Execute Snapshot SQL Query. Query would run using a snapshot with staleness of 5 sec. Accept
   * Map<String, Value> as named parameters.
   *
   * @param query
   * @param args
   * @return
   */
  ResultSet executeSnapshotSqlQuery(String query, Map<String, Value> args);

  /**
   * Execute DML. Using Map<String Value> as named parameters.
   *
   * @param sql
   * @param args
   * @return
   */
  long executeSqlWrite(String sql, Map<String, Value> args);

  /**
   * Run a sequence of Statements in a single Read/Write Query.
   *
   * @param statements
   * @return success of transaction.
   */
  boolean runTransaction(List<Statement> statements);

  /**
   * Run a query in a read/write transaction, with the handler invoked as the readStatement
   * completes..
   *
   * @param readStatement
   * @param handler handler contains List<Statement> to apply.
   * @return success of transaction.
   */
  void runTransaction(Statement readStatement, OnReadHandler handler);
}
