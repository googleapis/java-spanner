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

import com.google.api.core.InternalApi;
import com.google.cloud.spanner.ResultSet;

/**
 * A result of the execution of a statement. Statements that are executed by the {@link
 * Connection#execute(com.google.cloud.spanner.Statement)} method could have different types of
 * return values. These are wrapped in a {@link StatementResult}.
 */
@InternalApi
public interface StatementResult {

  /**
   * Enum indicating the type of result that was returned by {@link
   * Connection#execute(com.google.cloud.spanner.Statement)}
   */
  enum ResultType {
    /**
     * A result set either returned by a query on Cloud Spanner or a local result set generated by a
     * client side statement.
     */
    RESULT_SET,
    /** An update count returned by Cloud Spanner. */
    UPDATE_COUNT,
    /**
     * DDL statements and client side statements that set the state of a connection return no
     * result.
     */
    NO_RESULT;
  }

  /** The type of client side statement that was executed. */
  enum ClientSideStatementType {
    SHOW_AUTOCOMMIT,
    SET_AUTOCOMMIT,
    SHOW_READONLY,
    SET_READONLY,
    SHOW_RETRY_ABORTS_INTERNALLY,
    SET_RETRY_ABORTS_INTERNALLY,
    SHOW_AUTOCOMMIT_DML_MODE,
    SET_AUTOCOMMIT_DML_MODE,
    SHOW_STATEMENT_TIMEOUT,
    SET_STATEMENT_TIMEOUT,
    SHOW_READ_TIMESTAMP,
    SHOW_COMMIT_TIMESTAMP,
    SHOW_COMMIT_RESPONSE,
    SHOW_READ_ONLY_STALENESS,
    SET_READ_ONLY_STALENESS,
    SHOW_OPTIMIZER_VERSION,
    SET_OPTIMIZER_VERSION,
    SHOW_RETURN_COMMIT_STATS,
    SET_RETURN_COMMIT_STATS,
    SHOW_STATEMENT_TAG,
    SET_STATEMENT_TAG,
    SHOW_TRANSACTION_TAG,
    SET_TRANSACTION_TAG,
    BEGIN,
    COMMIT,
    ROLLBACK,
    SET_TRANSACTION_MODE,
    START_BATCH_DDL,
    START_BATCH_DML,
    RUN_BATCH,
    ABORT_BATCH;
  }

  /**
   * Returns the {@link ResultType} of this result.
   *
   * @return the result type.
   */
  ResultType getResultType();

  /**
   * @return the {@link ClientSideStatementType} that was executed, or null if no such statement was
   *     executed.
   */
  ClientSideStatementType getClientSideStatementType();

  /**
   * Returns the {@link ResultSet} held by this result. May only be called if the type of this
   * result is {@link ResultType#RESULT_SET}.
   *
   * @return the {@link ResultSet} held by this result.
   */
  ResultSet getResultSet();

  /**
   * Returns the update count held by this result. May only be called if the type of this result is
   * {@link ResultType#UPDATE_COUNT}.
   *
   * @return the update count held by this result.
   */
  Long getUpdateCount();
}
