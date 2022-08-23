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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

@InternalApi
public class PostgreSQLStatementParser extends AbstractStatementParser {
  private static final Pattern RETURNING_PATTERN = Pattern.compile("[ ')\"]returning[ '(\"]");
  private static final Pattern AS_RETURNING_PATTERN = Pattern.compile("[ ')\"]as returning[ '(\"]");

  PostgreSQLStatementParser() throws CompileException {
    super(
        Collections.unmodifiableSet(
            ClientSideStatements.getInstance(Dialect.POSTGRESQL).getCompiledStatements()));
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
    boolean isInSingleLineComment = false;
    int multiLineCommentLevel = 0;
    boolean whitespaceBeforeOrAfterMultiLineComment = false;
    int multiLineCommentStartIdx = -1;
    StringBuilder res = new StringBuilder(sql.length());
    int index = 0;
    while (index < sql.length()) {
      char c = sql.charAt(index);
      if (isInSingleLineComment) {
        if (c == '\n') {
          isInSingleLineComment = false;
          // Include the line feed in the result.
          res.append(c);
        }
      } else if (multiLineCommentLevel > 0) {
        if (sql.length() > index + 1 && c == ASTERISK && sql.charAt(index + 1) == SLASH) {
          multiLineCommentLevel--;
          if (multiLineCommentLevel == 0) {
            if (!whitespaceBeforeOrAfterMultiLineComment && (sql.length() > index + 2)) {
              whitespaceBeforeOrAfterMultiLineComment =
                  Character.isWhitespace(sql.charAt(index + 2));
            }
            // If the multiline comment does not have any whitespace before or after it, and it is
            // neither at the start nor at the end of SQL string, append an extra space.
            if (!whitespaceBeforeOrAfterMultiLineComment
                && (multiLineCommentStartIdx != 0)
                && (index != sql.length() - 2)) {
              res.append(' ');
            }
          }
          index++;
        } else if (sql.length() > index + 1 && c == SLASH && sql.charAt(index + 1) == ASTERISK) {
          multiLineCommentLevel++;
          index++;
        }
      } else {
        // Check for -- which indicates the start of a single-line comment.
        if (sql.length() > index + 1 && c == HYPHEN && sql.charAt(index + 1) == HYPHEN) {
          // This is a single line comment.
          isInSingleLineComment = true;
          index += 2;
          continue;
        } else if (sql.length() > index + 1 && c == SLASH && sql.charAt(index + 1) == ASTERISK) {
          multiLineCommentLevel++;
          if (index >= 1) {
            whitespaceBeforeOrAfterMultiLineComment = Character.isWhitespace(sql.charAt(index - 1));
          }
          multiLineCommentStartIdx = index;
          index += 2;
          continue;
        } else {
          index = skip(sql, index, res);
          continue;
        }
      }
      index++;
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
    StringBuilder named = new StringBuilder(sql.length() + countOccurrencesOf(paramChar, sql));
    int index = 0;
    int paramIndex = 1;
    while (index < sql.length()) {
      char c = sql.charAt(index);
      if (c == paramChar) {
        named.append(namedParamPrefix).append(paramIndex);
        paramIndex++;
        index++;
      } else {
        index = skip(sql, index, named);
      }
    }
    return new ParametersInfo(paramIndex - 1, named.toString());
  }

  /**
   * Note: This is an internal API and breaking changes can be made without prior notice.
   *
   * <p>Returns the PostgreSQL-style query parameters ($1, $2, ...) in the given SQL string. The
   * SQL-string is assumed to not contain any comments. Use {@link #removeCommentsAndTrim(String)}
   * to remove all comments before calling this method. Occurrences of query-parameter like strings
   * inside quoted identifiers or string literals are ignored.
   *
   * <p>The following example will return a set containing ("$1", "$2"). <code>
   * select col1, col2, "col$4"
   * from some_table
   * where col1=$1 and col2=$2
   * and not col3=$1 and col4='$3'
   * </code>
   *
   * @param sql the SQL-string to check for parameters. Must not contain comments.
   * @return A set containing all the parameters in the SQL-string.
   */
  @InternalApi
  public Set<String> getQueryParameters(String sql) {
    Preconditions.checkNotNull(sql);
    int maxCount = countOccurrencesOf('$', sql);
    Set<String> parameters = new HashSet<>(maxCount);
    int currentIndex = 0;
    while (currentIndex < sql.length() - 1) {
      char c = sql.charAt(currentIndex);
      if (c == '$' && Character.isDigit(sql.charAt(currentIndex + 1))) {
        // Look ahead for the first non-digit. That is the end of the query parameter.
        int endIndex = currentIndex + 2;
        while (endIndex < sql.length() && Character.isDigit(sql.charAt(endIndex))) {
          endIndex++;
        }
        parameters.add(sql.substring(currentIndex, endIndex));
        currentIndex = endIndex;
      } else {
        currentIndex = skip(sql, currentIndex, null);
      }
    }
    return parameters;
  }

  private int skip(String sql, int currentIndex, @Nullable StringBuilder result) {
    char currentChar = sql.charAt(currentIndex);
    if (currentChar == SINGLE_QUOTE || currentChar == DOUBLE_QUOTE) {
      appendIfNotNull(result, currentChar);
      return skipQuoted(sql, currentIndex, currentChar, result);
    } else if (currentChar == DOLLAR) {
      String dollarTag = parseDollarQuotedString(sql, currentIndex + 1);
      if (dollarTag != null) {
        appendIfNotNull(result, currentChar, dollarTag, currentChar);
        return skipQuoted(
            sql, currentIndex + dollarTag.length() + 1, currentChar, dollarTag, result);
      }
    }

    appendIfNotNull(result, currentChar);
    return currentIndex + 1;
  }

  private int skipQuoted(
      String sql, int startIndex, char startQuote, @Nullable StringBuilder result) {
    return skipQuoted(sql, startIndex, startQuote, null, result);
  }

  private int skipQuoted(
      String sql,
      int startIndex,
      char startQuote,
      String dollarTag,
      @Nullable StringBuilder result) {
    int currentIndex = startIndex + 1;
    while (currentIndex < sql.length()) {
      char currentChar = sql.charAt(currentIndex);
      if (currentChar == startQuote) {
        if (currentChar == DOLLAR) {
          // Check if this is the end of the current dollar quoted string.
          String tag = parseDollarQuotedString(sql, currentIndex + 1);
          if (tag != null && tag.equals(dollarTag)) {
            appendIfNotNull(result, currentChar, dollarTag, currentChar);
            return currentIndex + tag.length() + 2;
          }
        } else if (sql.length() > currentIndex + 1 && sql.charAt(currentIndex + 1) == startQuote) {
          // This is an escaped quote (e.g. 'foo''bar')
          appendIfNotNull(result, currentChar);
          appendIfNotNull(result, currentChar);
          currentIndex += 2;
          continue;
        } else {
          appendIfNotNull(result, currentChar);
          return currentIndex + 1;
        }
      }
      currentIndex++;
      appendIfNotNull(result, currentChar);
    }
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.INVALID_ARGUMENT, "SQL statement contains an unclosed literal: " + sql);
  }

  private void appendIfNotNull(@Nullable StringBuilder result, char currentChar) {
    if (result != null) {
      result.append(currentChar);
    }
  }

  private void appendIfNotNull(
      @Nullable StringBuilder result, char prefix, String tag, char suffix) {
    if (result != null) {
      result.append(prefix).append(tag).append(suffix);
    }
  }

  private boolean isReturning(String sql, int index) {
    // RETURNING is a reserved keyword in PG, but requires a
    // leading AS to be used as column label, to avoid ambiguity.
    // We thus check for cases which do not have a leading AS.
    // (https://www.postgresql.org/docs/current/sql-keywords-appendix.html)
    return (index >= 1)
        && (index + 10 <= sql.length())
        && RETURNING_PATTERN.matcher(sql.substring(index - 1, index + 10)).matches()
        && !((index >= 4)
            && AS_RETURNING_PATTERN.matcher(sql.substring(index - 4, index + 10)).matches());
  }

  @InternalApi
  @Override
  boolean checkReturningClauseInternal(String rawSql) {
    Preconditions.checkNotNull(rawSql);
    int index = 0;
    String sql = rawSql.replaceAll("\\s+", " ").toLowerCase();
    while (index < sql.length()) {
      if (isReturning(sql, index)) {
        return true;
      } else {
        index = skip(sql, index, null);
      }
    }
    return false;
  }
}
