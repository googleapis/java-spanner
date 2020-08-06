package com.google.cloud.spanner.connection;

import com.google.cloud.spanner.connection.ConnectionImpl.LeakedConnectionException;
import org.threeten.bp.Instant;

abstract class AbstractBaseConnection implements BaseConnection {
  private static final String CLOSED_ERROR_MSG = "This connection is closed";
  private static final String ONLY_ALLOWED_IN_AUTOCOMMIT =
      "This method may only be called while in autocommit mode";
  private static final String NOT_ALLOWED_IN_AUTOCOMMIT =
      "This method may not be called while in autocommit mode";

  /**
   * Exception that is used to register the stacktrace of the code that opened a {@link Connection}.
   * This exception is logged if the application closes without first closing the connection.
   */
  static class LeakedConnectionException extends RuntimeException {
    private static final long serialVersionUID = 7119433786832158700L;

    private LeakedConnectionException() {
      super("Connection was opened at " + Instant.now());
    }
  }

  private volatile LeakedConnectionException leakedException = new LeakedConnectionException();
  private final SpannerPool spannerPool;
  private final StatementParser parser = StatementParser.INSTANCE;
  /**
   * The {@link ConnectionStatementExecutor} is responsible for translating parsed {@link
   * ClientSideStatement}s into actual method calls on this {@link ConnectionImpl}. I.e. the {@link
   * ClientSideStatement} 'SET AUTOCOMMIT ON' will be translated into the method call {@link
   * ConnectionImpl#setAutocommit(boolean)} with value <code>true</code>.
   */
  private final ConnectionStatementExecutor connectionStatementExecutor =
      new ConnectionStatementExecutorImpl(this);

}
