/*
 * Copyright 2024 Google LLC
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

import static com.google.cloud.spanner.connection.AbstractStatementParser.ASTERISK;
import static com.google.cloud.spanner.connection.AbstractStatementParser.DASH;
import static com.google.cloud.spanner.connection.AbstractStatementParser.HYPHEN;
import static com.google.cloud.spanner.connection.AbstractStatementParser.SLASH;

import com.google.cloud.spanner.Dialect;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Objects;

/** A very simple token-based parser for extracting relevant information from SQL strings. */
class SimpleParser {
  /**
   * An immutable result from a parse action indicating whether the parse action was successful, and
   * if so, what the value was.
   */
  static class Result {
    static final Result NOT_FOUND = new Result(null);

    static Result found(String value) {
      return new Result(Preconditions.checkNotNull(value));
    }

    private final String value;

    private Result(String value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.value);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Result)) {
        return false;
      }
      return Objects.equals(this.value, ((Result) o).value);
    }

    @Override
    public String toString() {
      if (isValid()) {
        return this.value;
      }
      return "NOT FOUND";
    }

    boolean isValid() {
      return this.value != null;
    }

    String getValue() {
      return this.value;
    }
  }

  // TODO: Replace this with a direct reference to the dialect, and move the isXYZSupported methods
  //       from the AbstractStatementParser class to the Dialect class.
  private final AbstractStatementParser statementParser;

  private final String sql;

  private final boolean treatHintCommentsAsTokens;

  private int pos;

  /** Constructs a simple parser for the given SQL string and dialect. */
  SimpleParser(Dialect dialect, String sql) {
    this(dialect, sql, 0, /* treatHintCommentsAsTokens = */ false);
  }

  /**
   * Constructs a simple parser for the given SQL string and dialect. <code>
   * treatHintCommentsAsTokens</code> indicates whether comments that start with '/*@' should be
   * treated as tokens or not. This option may only be enabled if the dialect is PostgreSQL.
   */
  SimpleParser(Dialect dialect, String sql, int pos, boolean treatHintCommentsAsTokens) {
    Preconditions.checkArgument(
        !(treatHintCommentsAsTokens && dialect != Dialect.POSTGRESQL),
        "treatHintCommentsAsTokens can only be enabled for PostgreSQL");
    this.sql = sql;
    this.pos = pos;
    this.statementParser = AbstractStatementParser.getInstance(dialect);
    this.treatHintCommentsAsTokens = treatHintCommentsAsTokens;
  }

  Dialect getDialect() {
    return this.statementParser.getDialect();
  }

  String getSql() {
    return this.sql;
  }

  int getPos() {
    return this.pos;
  }

  /** Returns true if this parser has more tokens. Advances the position to the first next token. */
  boolean hasMoreTokens() {
    skipWhitespaces();
    return pos < sql.length();
  }

  /**
   * Eats and returns the identifier at the current position. This implementation does not support
   * quoted identifiers.
   */
  Result eatIdentifier() {
    // TODO: Implement support for quoted identifiers.
    // TODO: Implement support for identifiers with multiple parts (e.g. my_schema.my_table).
    if (!hasMoreTokens()) {
      return Result.NOT_FOUND;
    }
    if (!isValidIdentifierFirstChar(sql.charAt(pos))) {
      return Result.NOT_FOUND;
    }
    int startPos = pos;
    while (pos < sql.length() && isValidIdentifierChar(sql.charAt(pos))) {
      pos++;
    }
    return Result.found(sql.substring(startPos, pos));
  }

  /**
   * Eats a single-quoted string. This implementation currently does not support escape sequences.
   */
  Result eatSingleQuotedString() {
    if (!eatToken('\'')) {
      return Result.NOT_FOUND;
    }
    int startPos = pos;
    while (pos < sql.length() && sql.charAt(pos) != '\'') {
      if (sql.charAt(pos) == '\n') {
        return Result.NOT_FOUND;
      }
      pos++;
    }
    if (pos == sql.length()) {
      return Result.NOT_FOUND;
    }
    return Result.found(sql.substring(startPos, pos++));
  }

  boolean peekTokens(char... tokens) {
    return internalEatTokens(/* updatePos = */ false, tokens);
  }

  /**
   * Returns true if the next tokens in the SQL string are equal to the given tokens, and advances
   * the position of the parser to after the tokens. The position is not changed if the next tokens
   * are not equal to the list of tokens.
   */
  boolean eatTokens(char... tokens) {
    return internalEatTokens(/* updatePos = */ true, tokens);
  }

  /**
   * Returns true if the next tokens in the SQL string are equal to the given tokens, and advances
   * the position of the parser to after the tokens if updatePos is true. The position is not
   * changed if the next tokens are not equal to the list of tokens, or if updatePos is false.
   */
  private boolean internalEatTokens(boolean updatePos, char... tokens) {
    int currentPos = pos;
    for (char token : tokens) {
      if (!eatToken(token)) {
        pos = currentPos;
        return false;
      }
    }
    if (!updatePos) {
      pos = currentPos;
    }
    return true;
  }

  /**
   * Returns true if the next token is equal to the given character, but does not advance the
   * position of the parser.
   */
  boolean peekToken(char token) {
    int currentPos = pos;
    boolean res = eatToken(token);
    pos = currentPos;
    return res;
  }

  /**
   * Returns true and advances the position of the parser if the next token is equal to the given
   * character.
   */
  boolean eatToken(char token) {
    skipWhitespaces();
    if (pos < sql.length() && sql.charAt(pos) == token) {
      pos++;
      return true;
    }
    return false;
  }

  /**
   * Returns true if the given character is valid as the first character of an identifier. That
   * means that it can be used as the first character of an unquoted identifier.
   */
  static boolean isValidIdentifierFirstChar(char c) {
    return Character.isLetter(c) || c == '_';
  }

  /**
   * Returns true if the given character is a valid identifier character. That means that it can be
   * used in an unquoted identifiers.
   */
  static boolean isValidIdentifierChar(char c) {
    return isValidIdentifierFirstChar(c) || Character.isDigit(c) || c == '$';
  }

  /**
   * Skips all whitespaces, including comments, from the current position and advances the parser to
   * the next actual token.
   */
  @VisibleForTesting
  void skipWhitespaces() {
    while (pos < sql.length()) {
      if (sql.charAt(pos) == HYPHEN && sql.length() > (pos + 1) && sql.charAt(pos + 1) == HYPHEN) {
        skipSingleLineComment(/* prefixLength = */ 2);
      } else if (statementParser.supportsHashSingleLineComments() && sql.charAt(pos) == DASH) {
        skipSingleLineComment(/* prefixLength = */ 1);
      } else if (sql.charAt(pos) == SLASH
          && sql.length() > (pos + 1)
          && sql.charAt(pos + 1) == ASTERISK) {
        if (treatHintCommentsAsTokens && sql.length() > (pos + 2) && sql.charAt(pos + 2) == '@') {
          break;
        }
        skipMultiLineComment();
      } else if (Character.isWhitespace(sql.charAt(pos))) {
        pos++;
      } else {
        break;
      }
    }
  }

  /**
   * Skips through a single-line comment from the current position. The single-line comment is
   * started by a prefix with the given length (e.g. either '#' or '--').
   */
  @VisibleForTesting
  boolean skipSingleLineComment(int prefixLength) {
    int endIndex = sql.indexOf('\n', pos + prefixLength);
    if (endIndex == -1) {
      pos = sql.length();
      return true;
    }
    pos = endIndex + 1;
    return true;
  }

  /** Skips through a multi-line comment from the current position. */
  @VisibleForTesting
  boolean skipMultiLineComment() {
    int level = 1;
    pos += 2;
    while (pos < sql.length()) {
      if (statementParser.supportsNestedComments()
          && sql.charAt(pos) == SLASH
          && sql.length() > (pos + 1)
          && sql.charAt(pos + 1) == ASTERISK) {
        level++;
      }
      if (sql.charAt(pos) == ASTERISK && sql.length() > (pos + 1) && sql.charAt(pos + 1) == SLASH) {
        level--;
        if (level == 0) {
          pos += 2;
          return true;
        }
      }
      pos++;
    }
    pos = sql.length();
    return false;
  }
}
