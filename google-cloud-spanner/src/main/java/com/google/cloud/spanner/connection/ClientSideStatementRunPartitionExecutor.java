/*
 * Copyright 2022 Google LLC
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

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.common.base.Strings;
import java.lang.reflect.Method;
import java.util.regex.Matcher;

/** Executor for <code>RUN PARTITION &lt;partition_id&gt;</code> statements. */
class ClientSideStatementRunPartitionExecutor implements ClientSideStatementExecutor {
  private final ClientSideStatementImpl statement;
  private final Method method;

  ClientSideStatementRunPartitionExecutor(ClientSideStatementImpl statement)
      throws CompileException {
    try {
      this.statement = statement;
      this.method =
          ConnectionStatementExecutor.class.getDeclaredMethod(
              statement.getMethodName(), String.class);
    } catch (Exception e) {
      throw new CompileException(e, statement);
    }
  }

  @Override
  public StatementResult execute(
      ConnectionStatementExecutor connection, ParsedStatement parsedStatement) throws Exception {
    String partitionId = getParameterValue(parsedStatement);
    if (partitionId == null) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          "No valid partition id found in statement: " + parsedStatement.getStatement().getSql());
    }
    return (StatementResult) method.invoke(connection, partitionId);
  }

  String getParameterValue(ParsedStatement parsedStatement) {
    Matcher matcher = statement.getPattern().matcher(parsedStatement.getSqlWithoutComments());
    if (matcher.find() && matcher.groupCount() >= 1) {
      String value = matcher.group(1);
      if (!Strings.isNullOrEmpty(value)) {
        return value;
      }
    }
    if (parsedStatement.getStatement().getParameters().size() == 1) {
      Value value = parsedStatement.getStatement().getParameters().values().iterator().next();
      return value.getAsString();
    }
    return null;
  }
}
