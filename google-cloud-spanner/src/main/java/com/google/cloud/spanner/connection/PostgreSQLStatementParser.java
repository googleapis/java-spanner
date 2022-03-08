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

package com.google.cloud.spanner.connection;

import com.google.api.core.InternalApi;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.common.base.Preconditions;
import java.util.Collections;

@InternalApi
public class PostgreSQLStatementParser extends AbstractStatementParser {
  PostgreSQLStatementParser() throws CompileException {
    super(Dialect.POSTGRESQL, Collections.unmodifiableSet(ClientSideStatements.getInstance(Dialect.POSTGRESQL).getCompiledStatements()));
  }

  /**
   * Indicates whether the parser supports the {@code EXPLAIN} clause. The PostgreSQL parser does
   * not support it.
   */
  @Override
  protected boolean supportsExplain() {
    return false;
  }

  /**
   * Removes comments from and trims the given sql statement. PostgreSQL supports two types of
   * comments:
   *
   * <ul>
   *   <li>Single line comments starting with '--'
   *   <li>Multi line comments between '/&#42;' and '&#42;/'. Nested comments are supported and all
   *       comments, including the nested comments, must be terminated.
   * </ul>
   *
   * Reference: https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-COMMENTS
   *
   * @param sql The sql statement to remove comments from and to trim.
   * @return the sql statement without the comments and leading and trailing spaces.
   */
  @InternalApi
  @Override
  String removeCommentsAndTrimInternal(String sql) {
    Preconditions.checkNotNull(sql);
    String currentTag = null;
    boolean isInQuoted = false;
    boolean isInSingleLineComment = false;
    int multiLineCommentLevel = 0;
    char startQuote = 0;
    boolean lastCharWasEscapeChar = false;
    StringBuilder res = new StringBuilder(sql.length());
    int index = 0;
    while (index < sql.length()) {
      char c = sql.charAt(index);
      if (isInQuoted) {
        if ((c == '\n' || c == '\r') && startQuote != DOLLAR) {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.INVALID_ARGUMENT, "SQL statement contains an unclosed literal: " + sql);
        } else if (c == startQuote) {
          if (c == DOLLAR) {
            // Check if this is the end of the current dollar quoted string.
            String tag = parseDollarQuotedString(sql, index + 1);
            if (tag != null && tag.equals(currentTag)) {
              index += tag.length() + 1;
              res.append(c);
              res.append(tag);
              isInQuoted = false;
              startQuote = 0;
            }
          } else if (lastCharWasEscapeChar) {
            lastCharWasEscapeChar = false;
          } else if (sql.length() > index + 1 && sql.charAt(index + 1) == startQuote) {
            // This is an escaped quote (e.g. 'foo''bar')
            res.append(c);
            index++;
          } else {
            isInQuoted = false;
            startQuote = 0;
          }
        } else if (c == '\\') {
          lastCharWasEscapeChar = true;
        } else {
          lastCharWasEscapeChar = false;
        }
        res.append(c);
      } else {
        // We are not in a quoted string.
        if (isInSingleLineComment) {
          if (c == '\n') {
            isInSingleLineComment = false;
            // Include the line feed in the result.
            res.append(c);
          }
        } else if (multiLineCommentLevel > 0) {
          if (sql.length() > index + 1 && c == ASTERIKS && sql.charAt(index + 1) == SLASH) {
            multiLineCommentLevel--;
            index++;
          } else if (sql.length() > index + 1 && c == SLASH && sql.charAt(index + 1) == ASTERIKS) {
            multiLineCommentLevel++;
            index++;
          }
        } else {
          // Check for -- which indicates the start of a single-line comment.
          if (sql.length() > index + 1 && c == HYPHEN && sql.charAt(index + 1) == HYPHEN) {
            // This is a single line comment.
            isInSingleLineComment = true;
          } else if (sql.length() > index + 1 && c == SLASH && sql.charAt(index + 1) == ASTERIKS) {
            multiLineCommentLevel++;
            index++;
          } else {
            if (c == SINGLE_QUOTE || c == DOUBLE_QUOTE) {
              isInQuoted = true;
              startQuote = c;
            } else if (c == DOLLAR) {
              currentTag = parseDollarQuotedString(sql, index + 1);
              if (currentTag != null) {
                isInQuoted = true;
                startQuote = DOLLAR;
                index += currentTag.length() + 1;
                res.append(c);
                res.append(currentTag);
              }
            }
            res.append(c);
          }
        }
      }
      index++;
    }
    if (isInQuoted) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, "SQL statement contains an unclosed literal: " + sql);
    }
    if (multiLineCommentLevel > 0) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          "SQL statement contains an unterminated block comment: " + sql);
    }
    if (res.length() > 0 && res.charAt(res.length() - 1) == ';') {
      res.deleteCharAt(res.length() - 1);
    }
    return res.toString().trim();
  }

  String parseDollarQuotedString(String sql, int index) {
    // Look ahead to the next dollar sign (if any). Everything in between is the quote tag.
    StringBuilder tag = new StringBuilder();
    while (index < sql.length()) {
      char c = sql.charAt(index);
      if (c == DOLLAR) {
        return tag.toString();
      }
      if (!Character.isJavaIdentifierPart(c)) {
        break;
      }
      tag.append(c);
      index++;
    }
    return null;
  }

  /** PostgreSQL does not support statement hints. */
  @Override
  String removeStatementHint(String sql) {
    return sql;
  }

  @InternalApi
  @Override
  ParametersInfo convertPositionalParametersToNamedParametersInternal(char paramChar, String sql) {
    Preconditions.checkNotNull(sql);
    final String namedParamPrefix = "$";
    String currentTag = null;
    boolean isInQuoted = false;
    char startQuote = 0;
    boolean lastCharWasEscapeChar = false;
    StringBuilder named = new StringBuilder(sql.length() + countOccurrencesOf(paramChar, sql));
    int index = 0;
    int paramIndex = 1;
    while (index < sql.length()) {
      char c = sql.charAt(index);
      if (isInQuoted) {
        if ((c == '\n' || c == '\r') && startQuote != DOLLAR) {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.INVALID_ARGUMENT, "SQL statement contains an unclosed literal: " + sql);
        } else if (c == startQuote) {
          if (c == DOLLAR) {
            // Check if this is the end of the current dollar quoted string.
            String tag = parseDollarQuotedString(sql, index + 1);
            if (tag != null && tag.equals(currentTag)) {
              index += tag.length() + 1;
              named.append(c);
              named.append(tag);
              isInQuoted = false;
              startQuote = 0;
            }
          } else if (lastCharWasEscapeChar) {
            lastCharWasEscapeChar = false;
          } else if (sql.length() > index + 1 && sql.charAt(index + 1) == startQuote) {
            // This is an escaped quote (e.g. 'foo''bar')
            named.append(c);
            index++;
          } else {
            isInQuoted = false;
            startQuote = 0;
          }
        } else if (c == '\\') {
          lastCharWasEscapeChar = true;
        } else {
          lastCharWasEscapeChar = false;
        }
        named.append(c);
      } else {
        if (c == paramChar) {
          named.append(namedParamPrefix + paramIndex);
          paramIndex++;
        } else {
          if (c == SINGLE_QUOTE || c == DOUBLE_QUOTE) {
            isInQuoted = true;
            startQuote = c;
          } else if (c == DOLLAR) {
            currentTag = parseDollarQuotedString(sql, index + 1);
            if (currentTag != null) {
              isInQuoted = true;
              startQuote = DOLLAR;
              index += currentTag.length() + 1;
              named.append(c);
              named.append(currentTag);
            }
          }
          named.append(c);
        }
      }
      index++;
    }
    if (isInQuoted) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, "SQL statement contains an unclosed literal: " + sql);
    }
    return new ParametersInfo(paramIndex - 1, named.toString());
  }
}
