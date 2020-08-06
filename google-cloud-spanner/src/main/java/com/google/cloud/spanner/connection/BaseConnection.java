package com.google.cloud.spanner.connection;

import com.google.api.core.InternalApi;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbortedDueToConcurrentModificationException;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerBatchUpdateException;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.Options.QueryOption;
import java.util.Iterator;

public interface BaseConnection extends AutoCloseable {
  /** Closes this connection. This is a no-op if the {@link Connection} has alread been closed. */
  @Override
  void close();

  /** @return <code>true</code> if this connection has been closed. */
  boolean isClosed();

  /**
   * Sets autocommit on/off for this {@link Connection}. Connections in autocommit mode will apply
   * any changes to the database directly without waiting for an explicit commit. DDL- and DML
   * statements as well as {@link Mutation}s are sent directly to Spanner, and committed
   * automatically unless the statement caused an error. The statement is retried in case of an
   * {@link AbortedException}. All other errors will cause the underlying transaction to be rolled
   * back.
   *
   * <p>A {@link Connection} that is in autocommit and read/write mode will allow all types of
   * statements: Queries, DML, DDL, and Mutations (writes). If the connection is in read-only mode,
   * only queries will be allowed.
   *
   * <p>{@link Connection}s in autocommit mode may also accept partitioned DML statements. See
   * {@link Connection#setAutocommitDmlMode(AutocommitDmlMode)} for more information.
   *
   * @param autocommit true/false to turn autocommit on/off
   */
  void setAutocommit(boolean autocommit);

  /** @return <code>true</code> if this connection is in autocommit mode */
  boolean isAutocommit();

  /**
   * Sets this connection to read-only or read-write. This method may only be called when no
   * transaction is active. A connection that is in read-only mode, will never allow any kind of
   * changes to the database to be submitted.
   *
   * @param readOnly true/false to turn read-only mode on/off
   */
  void setReadOnly(boolean readOnly);

  /** @return <code>true</code> if this connection is in read-only mode */
  boolean isReadOnly();

  /**
   * Sets the transaction mode to use for current transaction. This method may only be called when
   * in a transaction, and before the transaction is actually started, i.e. before any statements
   * have been executed in the transaction.
   *
   * @param transactionMode The transaction mode to use for the current transaction.
   *     <ul>
   *       <li>{@link TransactionMode#READ_ONLY_TRANSACTION} will create a read-only transaction and
   *           prevent any changes to written to the database through this transaction. The read
   *           timestamp to be used will be determined based on the current readOnlyStaleness
   *           setting of this connection. It is recommended to use {@link
   *           TransactionMode#READ_ONLY_TRANSACTION} instead of {@link
   *           TransactionMode#READ_WRITE_TRANSACTION} when possible, as read-only transactions do
   *           not acquire locks on Cloud Spanner, and read-only transactions never abort.
   *       <li>{@link TransactionMode#READ_WRITE_TRANSACTION} this value is only allowed when the
   *           connection is not in read-only mode and will create a read-write transaction. If
   *           {@link Connection#isRetryAbortsInternally()} is <code>true</code>, each read/write
   *           transaction will keep track of a running SHA256 checksum for each {@link ResultSet}
   *           that is returned in order to be able to retry the transaction in case the transaction
   *           is aborted by Spanner.
   *     </ul>
   */
  void setTransactionMode(TransactionMode transactionMode);

  /**
   * @return the transaction mode of the current transaction. This method may only be called when
   *     the connection is in a transaction.
   */
  TransactionMode getTransactionMode();

  /**
   * @return <code>true</code> if this connection will automatically retry read/write transactions
   *     that abort. This method may only be called when the connection is in read/write
   *     transactional mode and no transaction has been started yet.
   */
  boolean isRetryAbortsInternally();

  /**
   * Sets whether this connection will internally retry read/write transactions that abort. The
   * default is <code>true</code>. When internal retry is enabled, the {@link Connection} will keep
   * track of a running SHA256 checksum of all {@link ResultSet}s that have been returned from Cloud
   * Spanner. If the checksum that is calculated during an internal retry differs from the original
   * checksum, the transaction will abort with an {@link
   * AbortedDueToConcurrentModificationException}.
   *
   * <p>Note that retries of a read/write transaction that calls a non-deterministic function on
   * Cloud Spanner, such as CURRENT_TIMESTAMP(), will never be successful, as the data returned
   * during the retry will always be different from the original transaction.
   *
   * <p>It is also highly recommended that all queries in a read/write transaction have an ORDER BY
   * clause that guarantees that the data is returned in the same order as in the original
   * transaction if the transaction is internally retried. The most efficient way to achieve this is
   * to always include the primary key columns at the end of the ORDER BY clause.
   *
   * <p>This method may only be called when the connection is in read/write transactional mode and
   * no transaction has been started yet.
   *
   * @param retryAbortsInternally Set to <code>true</code> to internally retry transactions that are
   *     aborted by Spanner. When set to <code>false</code>, any database call on a transaction that
   *     has been aborted by Cloud Spanner will throw an {@link AbortedException} instead of being
   *     retried. Set this to false if your application already uses retry loops to handle {@link
   *     AbortedException}s.
   */
  void setRetryAbortsInternally(boolean retryAbortsInternally);

  /**
   * Add a {@link TransactionRetryListener} to this {@link Connection} for testing and logging
   * purposes. The method {@link TransactionRetryListener#retryStarting(Timestamp, long, int)} will
   * be called before an automatic retry is started for a read/write transaction on this connection.
   * The method {@link TransactionRetryListener#retryFinished(Timestamp, long, int,
   * TransactionRetryListener.RetryResult)} will be called after the retry has finished.
   *
   * @param listener The listener to add to this connection.
   */
  void addTransactionRetryListener(TransactionRetryListener listener);

  /**
   * Removes one existing {@link TransactionRetryListener} from this {@link Connection}, if it is
   * present (optional operation).
   *
   * @param listener The listener to remove from the connection.
   * @return <code>true</code> if a listener was removed from the connection.
   */
  boolean removeTransactionRetryListener(TransactionRetryListener listener);

  /**
   * @return an unmodifiable iterator of the {@link TransactionRetryListener}s registered for this
   *     connection.
   */
  Iterator<TransactionRetryListener> getTransactionRetryListeners();

  /**
   * Sets the mode for executing DML statements in autocommit mode for this connection. This setting
   * is only used when the connection is in autocommit mode, and may only be set while the
   * transaction is in autocommit mode and not in a temporary transaction. The autocommit
   * transaction mode is reset to its default value of {@link AutocommitDmlMode#TRANSACTIONAL} when
   * autocommit mode is changed on the connection.
   *
   * @param mode The DML autocommit mode to use
   *     <ul>
   *       <li>{@link AutocommitDmlMode#TRANSACTIONAL} DML statements are executed as single
   *           read-write transaction. After successful execution, the DML statement is guaranteed
   *           to have been applied exactly once to the database
   *       <li>{@link AutocommitDmlMode#PARTITIONED_NON_ATOMIC} DML statements are executed as
   *           partitioned DML transactions. If an error occurs during the execution of the DML
   *           statement, it is possible that the statement has been applied to some but not all of
   *           the rows specified in the statement.
   *     </ul>
   */
  void setAutocommitDmlMode(AutocommitDmlMode mode);

  /**
   * @return the current {@link AutocommitDmlMode} setting for this connection. This method may only
   *     be called on a connection that is in autocommit mode and not while in a temporary
   *     transaction.
   */
  AutocommitDmlMode getAutocommitDmlMode();

  /**
   * Sets the staleness to use for the current read-only transaction. This method may only be called
   * when the transaction mode of the current transaction is {@link
   * TransactionMode#READ_ONLY_TRANSACTION} and there is no transaction that has started, or when
   * the connection is in read-only and autocommit mode.
   *
   * @param staleness The staleness to use for the current but not yet started read-only transaction
   */
  void setReadOnlyStaleness(TimestampBound staleness);

  /**
   * @return the read-only staleness setting for the current read-only transaction. This method may
   *     only be called when the current transaction is a read-only transaction, or when the
   *     connection is in read-only and autocommit mode.
   */
  TimestampBound getReadOnlyStaleness();

  /**
   * Sets the query optimizer version to use for this connection.
   *
   * @param optimizerVersion The query optimizer version to use. Must be a valid optimizer version
   *     number, the string <code>LATEST</code> or an empty string. The empty string will instruct
   *     the connection to use the optimizer version that is defined in the environment variable
   *     <code>SPANNER_OPTIMIZER_VERSION</code>. If no value is specified in the environment
   *     variable, the default query optimizer of Cloud Spanner is used.
   */
  void setOptimizerVersion(String optimizerVersion);

  /**
   * Gets the current query optimizer version of this connection.
   *
   * @return The query optimizer version that is currently used by this connection.
   */
  String getOptimizerVersion();

  /**
   * @return <code>true</code> if this connection has a transaction (that has not necessarily
   *     started). This method will only return false when the {@link Connection} is in autocommit
   *     mode and no explicit transaction has been started by calling {@link
   *     Connection#beginTransaction()}. If the {@link Connection} is not in autocommit mode, there
   *     will always be a transaction.
   */
  boolean isInTransaction();

  /**
   * @return <code>true</code> if this connection has a transaction that has started. A transaction
   *     is automatically started by the first statement that is executed in the transaction.
   */
  boolean isTransactionStarted();

  /**
   * Returns the read timestamp of the current/last {@link TransactionMode#READ_ONLY_TRANSACTION}
   * transaction, or the read timestamp of the last query in autocommit mode.
   *
   * <ul>
   *   <li>When in autocommit mode: The method will return the read timestamp of the last statement
   *       if the last statement was a query.
   *   <li>When in a {@link TransactionMode#READ_ONLY_TRANSACTION} transaction that has started (a
   *       query has been executed), or that has just committed: The read timestamp of the
   *       transaction. If the read-only transaction was committed without ever executing a query,
   *       calling this method after the commit will also throw a {@link SpannerException}
   *   <li>In all other cases the method will throw a {@link SpannerException}.
   * </ul>
   *
   * @return the read timestamp of the current/last read-only transaction.
   */
  Timestamp getReadTimestamp();

  /**
   * @return the commit timestamp of the last {@link TransactionMode#READ_WRITE_TRANSACTION}
   *     transaction. This method will throw a {@link SpannerException} if there is no last {@link
   *     TransactionMode#READ_WRITE_TRANSACTION} transaction (i.e. the last transaction was a {@link
   *     TransactionMode#READ_ONLY_TRANSACTION}), or if the last {@link
   *     TransactionMode#READ_WRITE_TRANSACTION} transaction rolled back. It will also throw a
   *     {@link SpannerException} if the last {@link TransactionMode#READ_WRITE_TRANSACTION}
   *     transaction was empty when committed.
   */
  Timestamp getCommitTimestamp();

  /**
   * Starts a new DDL batch on this connection. A DDL batch allows several DDL statements to be
   * grouped into a batch that can be executed as a group. DDL statements that are issued during the
   * batch are buffered locally and will return immediately with an OK. It is not guaranteed that a
   * DDL statement that has been issued during a batch will eventually succeed when running the
   * batch. Aborting a DDL batch will clear the DDL buffer and will have made no changes to the
   * database. Running a DDL batch will send all buffered DDL statements to Spanner, and Spanner
   * will try to execute these. The result will be OK if all the statements executed successfully.
   * If a statement cannot be executed, Spanner will stop execution at that point and return an
   * error message for the statement that could not be executed. Preceding statements of the batch
   * may have been executed.
   *
   * <p>This method may only be called when the connection is in read/write mode, autocommit mode is
   * enabled or no read/write transaction has been started, and there is not already another batch
   * active. The connection will only accept DDL statements while a DDL batch is active.
   */
  void startBatchDdl();

  /**
   * Starts a new DML batch on this connection. A DML batch allows several DML statements to be
   * grouped into a batch that can be executed as a group. DML statements that are issued during the
   * batch are buffered locally and will return immediately with an OK. It is not guaranteed that a
   * DML statement that has been issued during a batch will eventually succeed when running the
   * batch. Aborting a DML batch will clear the DML buffer and will have made no changes to the
   * database. Running a DML batch will send all buffered DML statements to Spanner, and Spanner
   * will try to execute these. The result will be OK if all the statements executed successfully.
   * If a statement cannot be executed, Spanner will stop execution at that point and return {@link
   * SpannerBatchUpdateException} for the statement that could not be executed. Preceding statements
   * of the batch will have been executed, and the update counts of those statements can be
   * retrieved through {@link SpannerBatchUpdateException#getUpdateCounts()}.
   *
   * <p>This method may only be called when the connection is in read/write mode, autocommit mode is
   * enabled or no read/write transaction has been started, and there is not already another batch
   * active. The connection will only accept DML statements while a DML batch is active.
   */
  void startBatchDml();

  /**
   * Clears all buffered statements in the current batch and ends the batch.
   *
   * <p>This method may only be called when a (possibly empty) batch is active.
   */
  void abortBatch();

  /** @return <code>true</code> if a DDL batch is active on this connection. */
  boolean isDdlBatchActive();

  /** @return <code>true</code> if a DML batch is active on this connection. */
  boolean isDmlBatchActive();

  /**
   * Buffers the given mutation locally on the current transaction of this {@link Connection}. The
   * mutation will be written to the database at the next call to {@link Connection#commit()}. The
   * value will not be readable on this {@link Connection} before the transaction is committed.
   *
   * <p>Calling this method is only allowed when not in autocommit mode. See {@link
   * Connection#write(Mutation)} for writing mutations in autocommit mode.
   *
   * @param mutation the {@link Mutation} to buffer for writing to the database on the next commit
   * @throws SpannerException if the {@link Connection} is in autocommit mode
   */
  void bufferedWrite(Mutation mutation);

  /**
   * Buffers the given mutations locally on the current transaction of this {@link Connection}. The
   * mutations will be written to the database at the next call to {@link Connection#commit()}. The
   * values will not be readable on this {@link Connection} before the transaction is committed.
   *
   * <p>Calling this method is only allowed when not in autocommit mode. See {@link
   * Connection#write(Iterable)} for writing mutations in autocommit mode.
   *
   * @param mutations the {@link Mutation}s to buffer for writing to the database on the next commit
   * @throws SpannerException if the {@link Connection} is in autocommit mode
   */
  void bufferedWrite(Iterable<Mutation> mutations);

  /**
   * This query option is used internally to indicate that a query is executed by the library itself
   * to fetch metadata. These queries are specifically allowed to be executed even when a DDL batch
   * is active.
   *
   * <p>NOT INTENDED FOR EXTERNAL USE!
   */
  @InternalApi
  public static final class InternalMetadataQuery implements QueryOption {
    @InternalApi public static final InternalMetadataQuery INSTANCE = new InternalMetadataQuery();

    private InternalMetadataQuery() {}
  }
}
