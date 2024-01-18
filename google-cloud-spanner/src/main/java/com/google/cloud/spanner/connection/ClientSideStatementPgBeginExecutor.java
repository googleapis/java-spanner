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
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.PgTransactionModeConverter;
import java.lang.reflect.Method;
import java.util.regex.Matcher;

/** Specific executor for the BEGIN statement for PostgreSQL. */
class ClientSideStatementPgBeginExecutor implements ClientSideStatementExecutor {
  private final ClientSideStatementImpl statement;
  private final Method method;
  private final PgTransactionModeConverter converter;

  ClientSideStatementPgBeginExecutor(ClientSideStatementImpl statement) throws CompileException {
    try {
      this.statement = statement;
      this.converter = new PgTransactionModeConverter();
      this.method =
          ConnectionStatementExecutor.class.getDeclaredMethod(
              statement.getMethodName(), converter.getParameterClass());
    } catch (Exception e) {
      throw new CompileException(e, statement);
    }
  }

  @Override
  public StatementResult execute(ConnectionStatementExecutor connection, ParsedStatement statement)
      throws Exception {
    return (StatementResult)
        method.invoke(connection, getParameterValue(statement.getSqlWithoutComments()));
  }

  PgTransactionMode getParameterValue(String sql) {
    Matcher matcher = statement.getPattern().matcher(sql);
    if (matcher.find() && matcher.groupCount() >= 1) {
      String value = matcher.group(1);
      if (value != null) {
        PgTransactionMode res = converter.convert(value.trim());
        if (res != null) {
          return res;
        }
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT, String.format("Unknown transaction mode: %s", value));
      }
    }
    return null;
  }
}
