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

import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;

/**
 * A {@link ClientSideStatementExecutor} is used to compile {@link ClientSideStatement}s from the
 * json source file, and to execute these against a {@link Connection} (through a {@link
 * ConnectionStatementExecutor}).
 */
interface ClientSideStatementExecutor {

  /**
   * Executes the {@link ClientSideStatementImpl} that has been compiled and registered with this
   * executor on the specified connection.
   *
   * @param connectionExecutor The {@link ConnectionStatementExecutor} to use to execute the
   *     statement on a {@link Connection}.
   * @param statement The statement that is executed. This can be used to parse any additional
   *     arguments that might be needed for the execution of the {@link ClientSideStatementImpl}.
   * @return the result of the execution.
   * @throws Exception If an error occurs while executing the statement, for example if an invalid
   *     argument has been specified in the sql statement, or if the statement is invalid for the
   *     current state of the {@link Connection}.
   */
  StatementResult execute(ConnectionStatementExecutor connectionExecutor, ParsedStatement statement)
      throws Exception;
}
