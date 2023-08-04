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
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.StatementResult.ClientSideStatementType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal class for the Spanner Connection API.
 *
 * <p>Parses {@link ClientSideStatement}s and normal SQL statements. The parser is able to recognize
 * the type of statement, allowing the connection API to know which method on Spanner should be
 * called. The parser does not validate the validity of statements, except for {@link
 * ClientSideStatement}s. This means that an invalid DML statement could be accepted by the {@link
 * AbstractStatementParser} and sent to Spanner, and Spanner will then reject it with some error
 * message.
 */
@InternalApi
public abstract class AbstractStatementParser {

  private static final Object lock = new Object();
  private static final Map<Dialect, AbstractStatementParser> INSTANCES = new HashMap<>();
  static final ImmutableMap<Dialect, Class<? extends AbstractStatementParser>>
      KNOWN_PARSER_CLASSES =
          ImmutableMap.of(
              Dialect.GOOGLE_STANDARD_SQL,
              SpannerStatementParser.class,
              Dialect.POSTGRESQL,
              PostgreSQLStatementParser.class);

  /**
   * Get an instance of {@link AbstractStatementParser} for the specified dialect.
   *
   * @param dialect
   * @return
   */
  public static AbstractStatementParser getInstance(Dialect dialect) {
    synchronized (lock) {
      if (!INSTANCES.containsKey(dialect)) {
        try {
          Class<? extends AbstractStatementParser> clazz = KNOWN_PARSER_CLASSES.get(dialect);
          if (clazz == null) {
            throw SpannerExceptionFactory.newSpannerException(
                ErrorCode.INTERNAL, "There is no known statement parser for dialect " + dialect);
          }
          INSTANCES.put(dialect, clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.INTERNAL,
              "Could not instantiate statement parser for dialect " + dialect.name(),
              e);
        }
      }
      return INSTANCES.get(dialect);
    }
  }

  static final ParsedStatement BEGIN_STATEMENT;
  static final ParsedStatement COMMIT_STATEMENT;
  static final ParsedStatement ROLLBACK_STATEMENT;
  static final ParsedStatement RUN_BATCH_STATEMENT;

  static {
    try {
      BEGIN_STATEMENT = getInstance(Dialect.GOOGLE_STANDARD_SQL).parse(Statement.of("BEGIN"));
      COMMIT_STATEMENT = getInstance(Dialect.GOOGLE_STANDARD_SQL).parse(Statement.of("COMMIT"));
      ROLLBACK_STATEMENT = getInstance(Dialect.GOOGLE_STANDARD_SQL).parse(Statement.of("ROLLBACK"));
      RUN_BATCH_STATEMENT =
          getInstance(Dialect.GOOGLE_STANDARD_SQL).parse(Statement.of("RUN BATCH"));

    } catch (Throwable ex) {
      Logger logger = Logger.getLogger(AbstractStatementParser.class.getName());
      logger.log(Level.SEVERE, "Static initialization failure.", ex);
      throw ex;
    }
  }

  /** The type of statement that has been recognized by the parser. */
  @InternalApi
  public enum StatementType {
    CLIENT_SIDE,
    DDL,
    QUERY,
    UPDATE,
    UNKNOWN
  }

  /** A statement that has been parsed */
  @InternalApi
  public static class ParsedStatement {

    private final StatementType type;
    private final ClientSideStatementImpl clientSideStatement;
    private final Statement statement;
    private final String sqlWithoutComments;
    private final boolean returningClause;

    private static ParsedStatement clientSideStatement(
        ClientSideStatementImpl clientSideStatement,
        Statement statement,
        String sqlWithoutComments) {
      return new ParsedStatement(clientSideStatement, statement, sqlWithoutComments);
    }

    private static ParsedStatement ddl(Statement statement, String sqlWithoutComments) {
      return new ParsedStatement(StatementType.DDL, statement, sqlWithoutComments);
    }

    private static ParsedStatement query(
        Statement statement, String sqlWithoutComments, QueryOptions defaultQueryOptions) {
      return new ParsedStatement(
          StatementType.QUERY, statement, sqlWithoutComments, defaultQueryOptions, false);
    }

    private static ParsedStatement update(
        Statement statement, String sqlWithoutComments, boolean returningClause) {
      return new ParsedStatement(
          StatementType.UPDATE, statement, sqlWithoutComments, returningClause);
    }

    private static ParsedStatement unknown(Statement statement, String sqlWithoutComments) {
      return new ParsedStatement(StatementType.UNKNOWN, statement, sqlWithoutComments);
    }

    private ParsedStatement(
        ClientSideStatementImpl clientSideStatement,
        Statement statement,
        String sqlWithoutComments) {
      Preconditions.checkNotNull(clientSideStatement);
      Preconditions.checkNotNull(statement);
      this.type = StatementType.CLIENT_SIDE;
      this.clientSideStatement = clientSideStatement;
      this.statement = statement;
      this.sqlWithoutComments = sqlWithoutComments;
      this.returningClause = false;
    }

    private ParsedStatement(
        StatementType type,
        Statement statement,
        String sqlWithoutComments,
        boolean returningClause) {
      this(type, statement, sqlWithoutComments, null, returningClause);
    }

    private ParsedStatement(StatementType type, Statement statement, String sqlWithoutComments) {
      this(type, statement, sqlWithoutComments, null, false);
    }

    private ParsedStatement(
        StatementType type,
        Statement statement,
        String sqlWithoutComments,
        QueryOptions defaultQueryOptions,
        boolean returningClause) {
      Preconditions.checkNotNull(type);
      Preconditions.checkNotNull(statement);
      this.type = type;
      this.clientSideStatement = null;
      this.statement = mergeQueryOptions(statement, defaultQueryOptions);
      this.sqlWithoutComments = sqlWithoutComments;
      this.returningClause = returningClause;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          this.type, this.clientSideStatement, this.statement, this.sqlWithoutComments);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ParsedStatement)) {
        return false;
      }
      ParsedStatement o = (ParsedStatement) other;
      return Objects.equals(this.type, o.type)
          && Objects.equals(this.clientSideStatement, o.clientSideStatement)
          && Objects.equals(this.statement, o.statement)
          && Objects.equals(this.sqlWithoutComments, o.sqlWithoutComments);
    }

    /** @return the type of statement that was recognized by the parser. */
    @InternalApi
    public StatementType getType() {
      return type;
    }

    /** @return whether the statement has a returning clause or not. */
    @InternalApi
    public boolean hasReturningClause() {
      return this.returningClause;
    }

    /**
     * @return true if the statement is a query that will return a {@link
     *     com.google.cloud.spanner.ResultSet}.
     */
    @InternalApi
    public boolean isQuery() {
      switch (type) {
        case CLIENT_SIDE:
          return getClientSideStatement().isQuery();
        case QUERY:
          return true;
        case UPDATE:
        case DDL:
        case UNKNOWN:
        default:
      }
      return false;
    }

    /**
     * @return true if the statement is a DML statement or a client side statement that will return
     *     an update count.
     */
    @InternalApi
    public boolean isUpdate() {
      switch (type) {
        case CLIENT_SIDE:
          return getClientSideStatement().isUpdate();
        case UPDATE:
          return true;
        case QUERY:
        case DDL:
        case UNKNOWN:
        default:
      }
      return false;
    }

    /** @return true if the statement is a DDL statement. */
    @InternalApi
    public boolean isDdl() {
      switch (type) {
        case DDL:
          return true;
        case CLIENT_SIDE:
        case UPDATE:
        case QUERY:
        case UNKNOWN:
        default:
      }
      return false;
    }

    /**
     * @return the {@link ClientSideStatementType} of this statement. This method may only be called
     *     on statements of type {@link StatementType#CLIENT_SIDE}.
     */
    @InternalApi
    public ClientSideStatementType getClientSideStatementType() {
      Preconditions.checkState(type == StatementType.CLIENT_SIDE);
      return clientSideStatement.getStatementType();
    }

    Statement getStatement() {
      return statement;
    }

    /**
     * Merges the {@link QueryOptions} of the {@link Statement} with the current {@link
     * QueryOptions} of this connection. The {@link QueryOptions} that are already present on the
     * statement take precedence above the connection {@link QueryOptions}.
     */
    Statement mergeQueryOptions(Statement statement, QueryOptions defaultQueryOptions) {
      if (defaultQueryOptions == null
          || defaultQueryOptions.equals(QueryOptions.getDefaultInstance())) {
        return statement;
      }
      if (statement.getQueryOptions() == null) {
        return statement.toBuilder().withQueryOptions(defaultQueryOptions).build();
      }
      return statement
          .toBuilder()
          .withQueryOptions(
              defaultQueryOptions.toBuilder().mergeFrom(statement.getQueryOptions()).build())
          .build();
    }

    /** @return the SQL statement with all comments removed from the SQL string. */
    @InternalApi
    public String getSqlWithoutComments() {
      return sqlWithoutComments;
    }

    ClientSideStatement getClientSideStatement() {
      Preconditions.checkState(
          clientSideStatement != null,
          "This ParsedStatement does not contain a ClientSideStatement");
      return clientSideStatement;
    }
  }

  static final Set<String> ddlStatements =
      ImmutableSet.of("CREATE", "DROP", "ALTER", "ANALYZE", "GRANT", "REVOKE");
  static final Set<String> selectStatements = ImmutableSet.of("SELECT", "WITH", "SHOW");
  static final Set<String> dmlStatements = ImmutableSet.of("INSERT", "UPDATE", "DELETE");
  private final Set<ClientSideStatementImpl> statements;

  AbstractStatementParser(Set<ClientSideStatementImpl> statements) {
    this.statements = Collections.unmodifiableSet(statements);
  }

  @VisibleForTesting
  Set<ClientSideStatementImpl> getClientSideStatements() {
    return statements;
  }

  /**
   * Parses the given statement and categorizes it as one of the possible {@link StatementType}s.
   * The validity of the statement is not checked, unless it is a client-side statement.
   *
   * @param statement The statement to parse.
   * @return the parsed and categorized statement.
   */
  @InternalApi
  public ParsedStatement parse(Statement statement) {
    return parse(statement, null);
  }

  ParsedStatement parse(Statement statement, QueryOptions defaultQueryOptions) {
    String sql = removeCommentsAndTrim(statement.getSql());
    ClientSideStatementImpl client = parseClientSideStatement(sql);
    if (client != null) {
      return ParsedStatement.clientSideStatement(client, statement, sql);
    } else if (isQuery(sql)) {
      return ParsedStatement.query(statement, sql, defaultQueryOptions);
    } else if (isUpdateStatement(sql)) {
      return ParsedStatement.update(statement, sql, checkReturningClause(sql));
    } else if (isDdlStatement(sql)) {
      return ParsedStatement.ddl(statement, sql);
    }
    return ParsedStatement.unknown(statement, sql);
  }

  /**
   * Parses the given statement as a client-side statement. Client-side statements are statements
   * that are never sent to Cloud Spanner, but that are interpreted by the Connection API and then
   * translated into some action, such as for example starting a transaction or getting the last
   * commit timestamp.
   *
   * @param sql The statement to try to parse as a client-side statement (without any comments).
   * @return a valid {@link ClientSideStatement} or null if the statement is not a client-side
   *     statement.
   */
  @VisibleForTesting
  ClientSideStatementImpl parseClientSideStatement(String sql) {
    for (ClientSideStatementImpl css : statements) {
      if (css.matches(sql)) {
        return css;
      }
    }
    return null;
  }

  /**
   * Checks whether the given statement is (probably) a DDL statement. The method does not check the
   * validity of the statement, only if it is a DDL statement based on the first word in the
   * statement.
   *
   * @param sql The statement to check (without any comments).
   * @return <code>true</code> if the statement is a DDL statement (i.e. starts with 'CREATE',
   *     'ALTER' or 'DROP').
   */
  @InternalApi
  public boolean isDdlStatement(String sql) {
    return statementStartsWith(sql, ddlStatements);
  }

  /**
   * Checks whether the given statement is (probably) a SELECT query. The method does not check the
   * validity of the statement, only if it is a SELECT statement based on the first word in the
   * statement.
   *
   * @param sql The statement to check (without any comments).
   * @return <code>true</code> if the statement is a SELECT statement (i.e. starts with 'SELECT').
   */
  @InternalApi
  public boolean isQuery(String sql) {
    // Skip any query hints at the beginning of the query.
    // We only do this if we actually know that it starts with a hint to prevent unnecessary
    // re-assigning the exact same sql string.
    if (sql.startsWith("@")) {
      sql = removeStatementHint(sql);
    }
    return statementStartsWith(sql, selectStatements);
  }

  /**
   * Checks whether the given statement is (probably) an update statement. The method does not check
   * the validity of the statement, only if it is an update statement based on the first word in the
   * statement.
   *
   * @param sql The statement to check (without any comments).
   * @return <code>true</code> if the statement is a DML update statement (i.e. starts with
   *     'INSERT', 'UPDATE' or 'DELETE').
   */
  @InternalApi
  public boolean isUpdateStatement(String sql) {
    // Skip any query hints at the beginning of the query.
    if (sql.startsWith("@")) {
      sql = removeStatementHint(sql);
    }
    return statementStartsWith(sql, dmlStatements);
  }

  protected abstract boolean supportsExplain();

  private boolean statementStartsWith(String sql, Iterable<String> checkStatements) {
    Preconditions.checkNotNull(sql);
    String[] tokens = sql.split("\\s+", 2);
    int checkIndex = 0;
    if (supportsExplain() && tokens[0].equalsIgnoreCase("EXPLAIN")) {
      checkIndex = 1;
    }
    if (tokens.length > checkIndex) {
      for (String check : checkStatements) {
        if (tokens[checkIndex].equalsIgnoreCase(check)) {
          return true;
        }
      }
    }
    return false;
  }

  static final char SINGLE_QUOTE = '\'';
  static final char DOUBLE_QUOTE = '"';
  static final char BACKTICK_QUOTE = '`';
  static final char HYPHEN = '-';
  static final char DASH = '#';
  static final char SLASH = '/';
  static final char ASTERISK = '*';
  static final char DOLLAR = '$';
  static final char SPACE = ' ';
  static final char CLOSE_PARENTHESIS = ')';
  static final char COMMA = ',';
  static final char UNDERSCORE = '_';

  /**
   * Removes comments from and trims the given sql statement using the dialect of this parser.
   *
   * @param sql The sql statement to remove comments from and to trim.
   * @return the sql statement without the comments and leading and trailing spaces.
   */
  @InternalApi
  abstract String removeCommentsAndTrimInternal(String sql);

  /**
   * Removes comments from and trims the given sql statement using the dialect of this parser.
   *
   * @param sql The sql statement to remove comments from and to trim.
   * @return the sql statement without the comments and leading and trailing spaces.
   */
  @InternalApi
  public String removeCommentsAndTrim(String sql) {
    return removeCommentsAndTrimInternal(sql);
  }

  /** Removes any statement hints at the beginning of the statement. */
  abstract String removeStatementHint(String sql);

  /** Parameter information with positional parameters translated to named parameters. */
  @InternalApi
  public static class ParametersInfo {

    public final int numberOfParameters;
    public final String sqlWithNamedParameters;

    ParametersInfo(int numberOfParameters, String sqlWithNamedParameters) {
      this.numberOfParameters = numberOfParameters;
      this.sqlWithNamedParameters = sqlWithNamedParameters;
    }
  }

  /**
   * Converts all positional parameters (?) in the given sql string into named parameters. The
   * parameters are named @p1, @p2, etc. This method is used when converting a JDBC statement that
   * uses positional parameters to a Cloud Spanner {@link Statement} instance that requires named
   * parameters. The input SQL string may not contain any comments. There is an exception case if
   * the statement starts with a GSQL comment which forces it to be interpreted as a GoogleSql
   * statement.
   *
   * @param sql The sql string without comments that should be converted
   * @return A {@link ParametersInfo} object containing a string with named parameters instead of
   *     positional parameters and the number of parameters.
   * @throws SpannerException If the input sql string contains an unclosed string/byte literal.
   */
  @InternalApi
  abstract ParametersInfo convertPositionalParametersToNamedParametersInternal(
      char paramChar, String sql);

  /**
   * Converts all positional parameters (?) in the given sql string into named parameters. The
   * parameters are named @p1, @p2, etc. This method is used when converting a JDBC statement that
   * uses positional parameters to a Cloud Spanner {@link Statement} instance that requires named
   * parameters. The input SQL string may not contain any comments. There is an exception case if
   * the statement starts with a GSQL comment which forces it to be interpreted as a GoogleSql
   * statement.
   *
   * @param sql The sql string without comments that should be converted
   * @return A {@link ParametersInfo} object containing a string with named parameters instead of
   *     positional parameters and the number of parameters.
   * @throws SpannerException If the input sql string contains an unclosed string/byte literal.
   */
  @InternalApi
  public ParametersInfo convertPositionalParametersToNamedParameters(char paramChar, String sql) {
    return convertPositionalParametersToNamedParametersInternal(paramChar, sql);
  }

  /** Convenience method that is used to estimate the number of parameters in a SQL statement. */
  static int countOccurrencesOf(char c, String string) {
    int res = 0;
    for (int i = 0; i < string.length(); i++) {
      if (string.charAt(i) == c) {
        res++;
      }
    }
    return res;
  }

  /**
   * Checks if the given SQL string contains a Returning clause. This method is used only in case of
   * a DML statement.
   *
   * @param sql The sql string without comments that has to be evaluated.
   * @return A boolean indicating whether the sql string has a Returning clause or not.
   */
  @InternalApi
  protected abstract boolean checkReturningClauseInternal(String sql);

  /**
   * Checks if the given SQL string contains a Returning clause. This method is used only in case of
   * a DML statement.
   *
   * @param sql The sql string without comments that has to be evaluated.
   * @return A boolean indicating whether the sql string has a Returning clause or not.
   */
  @InternalApi
  public boolean checkReturningClause(String sql) {
    return checkReturningClauseInternal(sql);
  }
}
