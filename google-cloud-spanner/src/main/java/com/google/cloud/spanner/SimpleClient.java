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
   * Execute Snapshot SQL Query. Query would run using a snapshot with staleness of 15 seconds.
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
   * Execute Snapshot SQL Query. Query would run using a snapshot with staleness of 15 sec.
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
   */
  void runTransaction(List<Statement> statements);

  /**
   * Run a query in a read/write transaction, with the handler invoked as the readStatement
   * completes..
   *
   * @param readStatement
   * @param handler handler contains List<Statement> to apply.
   */
  void runTransaction(Statement readStatement, OnReadHandler handler);
}
