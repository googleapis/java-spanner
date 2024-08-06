/*
 * Copyright 2017 Google LLC
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

package com.google.cloud.spanner;

import static com.google.cloud.spanner.MetricRegistryConstants.COUNT;
import static com.google.cloud.spanner.MetricRegistryConstants.GET_SESSION_TIMEOUTS;
import static com.google.cloud.spanner.MetricRegistryConstants.IS_MULTIPLEXED;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_ALLOWED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_ALLOWED_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_IN_USE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_IN_USE_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.METRIC_PREFIX;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_ACQUIRED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_ACQUIRED_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_IN_USE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_READ_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_RELEASED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_RELEASED_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_AVAILABLE;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_BEING_PREPARED;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_IN_POOL;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_IN_POOL_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_IN_USE;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_WRITE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.SESSIONS_TIMEOUTS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.SESSIONS_TYPE;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_DEFAULT_LABEL_VALUES;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS_WITH_TYPE;
import static com.google.cloud.spanner.SpannerExceptionFactory.asSpannerException;
import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerException;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.Timestamp;
import com.google.cloud.Tuple;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SessionPoolOptions.InactiveTransactionRemovalOptions;
import com.google.cloud.spanner.SpannerException.ResourceNotFoundException;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Empty;
import com.google.spanner.v1.BatchWriteResponse;
import com.google.spanner.v1.ResultSetStats;
import io.opencensus.metrics.DerivedLongCumulative;
import io.opencensus.metrics.DerivedLongGauge;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.MetricOptions;
import io.opencensus.metrics.MetricRegistry;
import io.opencensus.metrics.Metrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.Meter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

/**
 * Maintains a pool of sessions. This class itself is thread safe and is meant to be used
 * concurrently across multiple threads.
 */
class SessionPool {

  private static final Logger logger = Logger.getLogger(SessionPool.class.getName());
  private final TraceWrapper tracer;
  static final String WAIT_FOR_SESSION = "SessionPool.WaitForSession";

  /**
   * If the {@link SessionPoolOptions#getWaitForMinSessions()} duration is greater than zero, waits
   * for the creation of at least {@link SessionPoolOptions#getMinSessions()} in the pool using the
   * given duration. If the waiting times out, a {@link SpannerException} with the {@link
   * ErrorCode#DEADLINE_EXCEEDED} is thrown.
   */
  void maybeWaitOnMinSessions() {
    final long timeoutNanos = options.getWaitForMinSessions().toNanos();
    if (timeoutNanos <= 0) {
      return;
    }

    try {
      if (!waitOnMinSessionsLatch.await(timeoutNanos, TimeUnit.NANOSECONDS)) {
        final long timeoutMillis = options.getWaitForMinSessions().toMillis();
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.DEADLINE_EXCEEDED,
            "Timed out after waiting " + timeoutMillis + "ms for session pool creation");
      }
    } catch (InterruptedException e) {
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }

  private abstract static class CachedResultSetSupplier
      implements com.google.common.base.Supplier<ResultSet> {

    private ResultSet cached;

    abstract ResultSet load();

    ResultSet reload() {
      return cached = load();
    }

    @Override
    public ResultSet get() {
      if (cached == null) {
        cached = load();
      }
      return cached;
    }
  }

  /**
   * Wrapper around {@code ReadContext} that releases the session to the pool once the call is
   * finished, if it is a single use context.
   */
  private static class AutoClosingReadContext<I extends SessionFuture, T extends ReadContext>
      implements ReadContext {
    /**
     * {@link AsyncResultSet} implementation that keeps track of the async operations that are still
     * running for this {@link ReadContext} and that should finish before the {@link ReadContext}
     * releases its session back into the pool.
     */
    private class AutoClosingReadContextAsyncResultSetImpl extends AsyncResultSetImpl {
      private AutoClosingReadContextAsyncResultSetImpl(
          ExecutorProvider executorProvider, ResultSet delegate, int bufferRows) {
        super(executorProvider, delegate, bufferRows);
      }

      @Override
      public ApiFuture<Void> setCallback(Executor exec, ReadyCallback cb) {
        Runnable listener =
            () -> {
              synchronized (lock) {
                if (asyncOperationsCount.decrementAndGet() == 0 && closed) {
                  // All async operations for this read context have finished.
                  AutoClosingReadContext.this.close();
                }
              }
            };
        try {
          asyncOperationsCount.incrementAndGet();
          addListener(listener);
          return super.setCallback(exec, cb);
        } catch (Throwable t) {
          removeListener(listener);
          asyncOperationsCount.decrementAndGet();
          throw t;
        }
      }
    }

    private final Function<I, T> readContextDelegateSupplier;
    private T readContextDelegate;
    private final SessionPool sessionPool;
    private final SessionReplacementHandler<I> sessionReplacementHandler;
    private final boolean isSingleUse;
    private final AtomicInteger asyncOperationsCount = new AtomicInteger();

    private final Object lock = new Object();

    @GuardedBy("lock")
    private boolean sessionUsedForQuery = false;

    @GuardedBy("lock")
    private I session;

    @GuardedBy("lock")
    private boolean closed;

    @GuardedBy("lock")
    private boolean delegateClosed;

    private AutoClosingReadContext(
        Function<I, T> delegateSupplier,
        SessionPool sessionPool,
        SessionReplacementHandler<I> sessionReplacementHandler,
        I session,
        boolean isSingleUse) {
      this.readContextDelegateSupplier = delegateSupplier;
      this.sessionPool = sessionPool;
      this.sessionReplacementHandler = sessionReplacementHandler;
      this.session = session;
      this.isSingleUse = isSingleUse;
    }

    T getReadContextDelegate() {
      synchronized (lock) {
        if (readContextDelegate == null) {
          while (true) {
            try {
              this.readContextDelegate = readContextDelegateSupplier.apply(this.session);
              break;
            } catch (SessionNotFoundException e) {
              replaceSessionIfPossible(e);
            }
          }
        }
      }
      return readContextDelegate;
    }

    private ResultSet wrap(final CachedResultSetSupplier resultSetSupplier) {
      return new ForwardingResultSet(resultSetSupplier) {
        private boolean beforeFirst = true;

        @Override
        public boolean next() throws SpannerException {
          while (true) {
            try {
              return internalNext();
            } catch (SessionNotFoundException e) {
              while (true) {
                // Keep the replace-if-possible outside the try-block to let the exception bubble up
                // if it's too late to replace the session.
                replaceSessionIfPossible(e);
                try {
                  replaceDelegate(resultSetSupplier.reload());
                  break;
                } catch (SessionNotFoundException snfe) {
                  e = snfe;
                  // retry on yet another session.
                }
              }
            }
          }
        }

        private boolean internalNext() {
          try {
            boolean ret = super.next();
            if (beforeFirst) {
              synchronized (lock) {
                session.get().markUsed();
                beforeFirst = false;
                sessionUsedForQuery = true;
              }
            }
            if (!ret && isSingleUse) {
              close();
            }
            return ret;
          } catch (SessionNotFoundException e) {
            throw e;
          } catch (SpannerException e) {
            synchronized (lock) {
              if (!closed && isSingleUse) {
                session.get().setLastException(e);
                AutoClosingReadContext.this.close();
              }
            }
            throw e;
          }
        }

        @Override
        public void close() {
          try {
            super.close();
          } finally {
            if (isSingleUse) {
              AutoClosingReadContext.this.close();
            }
          }
        }
      };
    }

    private void replaceSessionIfPossible(SessionNotFoundException notFound) {
      synchronized (lock) {
        if (isSingleUse || !sessionUsedForQuery) {
          // This class is only used by read-only transactions, so we know that we only need a
          // read-only session.
          session = sessionReplacementHandler.replaceSession(notFound, session);
          readContextDelegate = readContextDelegateSupplier.apply(session);
        } else {
          throw notFound;
        }
      }
    }

    @Override
    public ResultSet read(
        final String table,
        final KeySet keys,
        final Iterable<String> columns,
        final ReadOption... options) {
      return wrap(
          new CachedResultSetSupplier() {
            @Override
            ResultSet load() {
              return getReadContextDelegate().read(table, keys, columns, options);
            }
          });
    }

    @Override
    public AsyncResultSet readAsync(
        final String table,
        final KeySet keys,
        final Iterable<String> columns,
        final ReadOption... options) {
      Options readOptions = Options.fromReadOptions(options);
      final int bufferRows =
          readOptions.hasBufferRows()
              ? readOptions.bufferRows()
              : AsyncResultSetImpl.DEFAULT_BUFFER_SIZE;
      return new AutoClosingReadContextAsyncResultSetImpl(
          sessionPool.sessionClient.getSpanner().getAsyncExecutorProvider(),
          wrap(
              new CachedResultSetSupplier() {
                @Override
                ResultSet load() {
                  return getReadContextDelegate().read(table, keys, columns, options);
                }
              }),
          bufferRows);
    }

    @Override
    public ResultSet readUsingIndex(
        final String table,
        final String index,
        final KeySet keys,
        final Iterable<String> columns,
        final ReadOption... options) {
      return wrap(
          new CachedResultSetSupplier() {
            @Override
            ResultSet load() {
              return getReadContextDelegate().readUsingIndex(table, index, keys, columns, options);
            }
          });
    }

    @Override
    public AsyncResultSet readUsingIndexAsync(
        final String table,
        final String index,
        final KeySet keys,
        final Iterable<String> columns,
        final ReadOption... options) {
      Options readOptions = Options.fromReadOptions(options);
      final int bufferRows =
          readOptions.hasBufferRows()
              ? readOptions.bufferRows()
              : AsyncResultSetImpl.DEFAULT_BUFFER_SIZE;
      return new AutoClosingReadContextAsyncResultSetImpl(
          sessionPool.sessionClient.getSpanner().getAsyncExecutorProvider(),
          wrap(
              new CachedResultSetSupplier() {
                @Override
                ResultSet load() {
                  return getReadContextDelegate()
                      .readUsingIndex(table, index, keys, columns, options);
                }
              }),
          bufferRows);
    }

    @Override
    @Nullable
    public Struct readRow(String table, Key key, Iterable<String> columns) {
      try {
        while (true) {
          try {
            synchronized (lock) {
              session.get().markUsed();
            }
            return getReadContextDelegate().readRow(table, key, columns);
          } catch (SessionNotFoundException e) {
            replaceSessionIfPossible(e);
          }
        }
      } finally {
        synchronized (lock) {
          sessionUsedForQuery = true;
        }
        if (isSingleUse) {
          close();
        }
      }
    }

    @Override
    public ApiFuture<Struct> readRowAsync(String table, Key key, Iterable<String> columns) {
      try (AsyncResultSet rs = readAsync(table, KeySet.singleKey(key), columns)) {
        return AbstractReadContext.consumeSingleRowAsync(rs);
      }
    }

    @Override
    @Nullable
    public Struct readRowUsingIndex(String table, String index, Key key, Iterable<String> columns) {
      try {
        while (true) {
          try {
            synchronized (lock) {
              session.get().markUsed();
            }
            return getReadContextDelegate().readRowUsingIndex(table, index, key, columns);
          } catch (SessionNotFoundException e) {
            replaceSessionIfPossible(e);
          }
        }
      } finally {
        synchronized (lock) {
          sessionUsedForQuery = true;
        }
        if (isSingleUse) {
          close();
        }
      }
    }

    @Override
    public ApiFuture<Struct> readRowUsingIndexAsync(
        String table, String index, Key key, Iterable<String> columns) {
      try (AsyncResultSet rs = readUsingIndexAsync(table, index, KeySet.singleKey(key), columns)) {
        return AbstractReadContext.consumeSingleRowAsync(rs);
      }
    }

    @Override
    public ResultSet executeQuery(final Statement statement, final QueryOption... options) {
      return wrap(
          new CachedResultSetSupplier() {
            @Override
            ResultSet load() {
              return getReadContextDelegate().executeQuery(statement, options);
            }
          });
    }

    @Override
    public AsyncResultSet executeQueryAsync(
        final Statement statement, final QueryOption... options) {
      Options queryOptions = Options.fromQueryOptions(options);
      final int bufferRows =
          queryOptions.hasBufferRows()
              ? queryOptions.bufferRows()
              : AsyncResultSetImpl.DEFAULT_BUFFER_SIZE;
      return new AutoClosingReadContextAsyncResultSetImpl(
          sessionPool.sessionClient.getSpanner().getAsyncExecutorProvider(),
          wrap(
              new CachedResultSetSupplier() {
                @Override
                ResultSet load() {
                  return getReadContextDelegate().executeQuery(statement, options);
                }
              }),
          bufferRows);
    }

    @Override
    public ResultSet analyzeQuery(final Statement statement, final QueryAnalyzeMode queryMode) {
      return wrap(
          new CachedResultSetSupplier() {
            @Override
            ResultSet load() {
              return getReadContextDelegate().analyzeQuery(statement, queryMode);
            }
          });
    }

    @Override
    public void close() {
      synchronized (lock) {
        if (closed && delegateClosed) {
          return;
        }
        closed = true;
        if (asyncOperationsCount.get() == 0) {
          if (readContextDelegate != null) {
            readContextDelegate.close();
          }
          session.close();
          delegateClosed = true;
        }
      }
    }
  }

  private static class AutoClosingReadTransaction<I extends SessionFuture>
      extends AutoClosingReadContext<I, ReadOnlyTransaction> implements ReadOnlyTransaction {

    AutoClosingReadTransaction(
        Function<I, ReadOnlyTransaction> txnSupplier,
        SessionPool sessionPool,
        SessionReplacementHandler<I> sessionReplacementHandler,
        I session,
        boolean isSingleUse) {
      super(txnSupplier, sessionPool, sessionReplacementHandler, session, isSingleUse);
    }

    @Override
    public Timestamp getReadTimestamp() {
      return getReadContextDelegate().getReadTimestamp();
    }
  }

  interface SessionReplacementHandler<T extends SessionFuture> {
    T replaceSession(SessionNotFoundException notFound, T sessionFuture);
  }

  class PooledSessionReplacementHandler implements SessionReplacementHandler<PooledSessionFuture> {
    @Override
    public PooledSessionFuture replaceSession(
        SessionNotFoundException e, PooledSessionFuture session) {
      if (!options.isFailIfSessionNotFound() && session.get().isAllowReplacing()) {
        synchronized (lock) {
          numSessionsInUse--;
          numSessionsReleased++;
          checkedOutSessions.remove(session);
          markedCheckedOutSessions.remove(session);
        }
        session.leakedException = null;
        invalidateSession(session.get());
        return getSession();
      } else {
        throw e;
      }
    }
  }

  interface SessionNotFoundHandler {
    /**
     * Handles the given {@link SessionNotFoundException} by possibly converting it to a different
     * exception that should be thrown.
     */
    SpannerException handleSessionNotFound(SessionNotFoundException notFound);
  }

  static class SessionPoolResultSet extends ForwardingResultSet {
    private final SessionNotFoundHandler handler;

    private SessionPoolResultSet(SessionNotFoundHandler handler, ResultSet delegate) {
      super(delegate);
      this.handler = Preconditions.checkNotNull(handler);
    }

    @Override
    public boolean next() {
      try {
        return super.next();
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }
  }

  static class AsyncSessionPoolResultSet extends ForwardingAsyncResultSet {
    private final SessionNotFoundHandler handler;

    private AsyncSessionPoolResultSet(SessionNotFoundHandler handler, AsyncResultSet delegate) {
      super(delegate);
      this.handler = Preconditions.checkNotNull(handler);
    }

    @Override
    public ApiFuture<Void> setCallback(Executor executor, final ReadyCallback callback) {
      return super.setCallback(
          executor,
          resultSet -> {
            try {
              return callback.cursorReady(resultSet);
            } catch (SessionNotFoundException e) {
              throw handler.handleSessionNotFound(e);
            }
          });
    }

    @Override
    public boolean next() {
      try {
        return super.next();
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }

    @Override
    public CursorState tryNext() {
      try {
        return super.tryNext();
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }
  }

  /**
   * {@link TransactionContext} that is used in combination with an {@link
   * AutoClosingTransactionManager}. This {@link TransactionContext} handles {@link
   * SessionNotFoundException}s by replacing the underlying session with a fresh one, and then
   * throws an {@link AbortedException} to trigger the retry-loop that has been created by the
   * caller.
   */
  static class SessionPoolTransactionContext implements TransactionContext {
    private final SessionNotFoundHandler handler;
    final TransactionContext delegate;

    SessionPoolTransactionContext(SessionNotFoundHandler handler, TransactionContext delegate) {
      this.handler = Preconditions.checkNotNull(handler);
      this.delegate = delegate;
    }

    @Override
    public ResultSet read(
        String table, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new SessionPoolResultSet(handler, delegate.read(table, keys, columns, options));
    }

    @Override
    public AsyncResultSet readAsync(
        String table, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new AsyncSessionPoolResultSet(
          handler, delegate.readAsync(table, keys, columns, options));
    }

    @Override
    public ResultSet readUsingIndex(
        String table, String index, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new SessionPoolResultSet(
          handler, delegate.readUsingIndex(table, index, keys, columns, options));
    }

    @Override
    public AsyncResultSet readUsingIndexAsync(
        String table, String index, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new AsyncSessionPoolResultSet(
          handler, delegate.readUsingIndexAsync(table, index, keys, columns, options));
    }

    @Override
    public Struct readRow(String table, Key key, Iterable<String> columns) {
      try {
        return delegate.readRow(table, key, columns);
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }

    @Override
    public ApiFuture<Struct> readRowAsync(String table, Key key, Iterable<String> columns) {
      try (AsyncResultSet rs = readAsync(table, KeySet.singleKey(key), columns)) {
        return ApiFutures.catching(
            AbstractReadContext.consumeSingleRowAsync(rs),
            SessionNotFoundException.class,
            input -> {
              throw handler.handleSessionNotFound(input);
            },
            MoreExecutors.directExecutor());
      }
    }

    @Override
    public void buffer(Mutation mutation) {
      delegate.buffer(mutation);
    }

    @Override
    public ApiFuture<Void> bufferAsync(Mutation mutation) {
      return delegate.bufferAsync(mutation);
    }

    @Override
    public Struct readRowUsingIndex(String table, String index, Key key, Iterable<String> columns) {
      try {
        return delegate.readRowUsingIndex(table, index, key, columns);
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }

    @Override
    public ApiFuture<Struct> readRowUsingIndexAsync(
        String table, String index, Key key, Iterable<String> columns) {
      try (AsyncResultSet rs = readUsingIndexAsync(table, index, KeySet.singleKey(key), columns)) {
        return ApiFutures.catching(
            AbstractReadContext.consumeSingleRowAsync(rs),
            SessionNotFoundException.class,
            input -> {
              throw handler.handleSessionNotFound(input);
            },
            MoreExecutors.directExecutor());
      }
    }

    @Override
    public void buffer(Iterable<Mutation> mutations) {
      delegate.buffer(mutations);
    }

    @Override
    public ApiFuture<Void> bufferAsync(Iterable<Mutation> mutations) {
      return delegate.bufferAsync(mutations);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResultSetStats analyzeUpdate(
        Statement statement, QueryAnalyzeMode analyzeMode, UpdateOption... options) {
      try (ResultSet resultSet = analyzeUpdateStatement(statement, analyzeMode, options)) {
        return resultSet.getStats();
      }
    }

    @Override
    public ResultSet analyzeUpdateStatement(
        Statement statement, QueryAnalyzeMode analyzeMode, UpdateOption... options) {
      try {
        return delegate.analyzeUpdateStatement(statement, analyzeMode, options);
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }

    @Override
    public long executeUpdate(Statement statement, UpdateOption... options) {
      try {
        return delegate.executeUpdate(statement, options);
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }

    @Override
    public ApiFuture<Long> executeUpdateAsync(Statement statement, UpdateOption... options) {
      return ApiFutures.catching(
          delegate.executeUpdateAsync(statement, options),
          SessionNotFoundException.class,
          input -> {
            throw handler.handleSessionNotFound(input);
          },
          MoreExecutors.directExecutor());
    }

    @Override
    public long[] batchUpdate(Iterable<Statement> statements, UpdateOption... options) {
      try {
        return delegate.batchUpdate(statements, options);
      } catch (SessionNotFoundException e) {
        throw handler.handleSessionNotFound(e);
      }
    }

    @Override
    public ApiFuture<long[]> batchUpdateAsync(
        Iterable<Statement> statements, UpdateOption... options) {
      return ApiFutures.catching(
          delegate.batchUpdateAsync(statements, options),
          SessionNotFoundException.class,
          input -> {
            throw handler.handleSessionNotFound(input);
          },
          MoreExecutors.directExecutor());
    }

    @Override
    public ResultSet executeQuery(Statement statement, QueryOption... options) {
      return new SessionPoolResultSet(handler, delegate.executeQuery(statement, options));
    }

    @Override
    public AsyncResultSet executeQueryAsync(Statement statement, QueryOption... options) {
      return new AsyncSessionPoolResultSet(handler, delegate.executeQueryAsync(statement, options));
    }

    @Override
    public ResultSet analyzeQuery(Statement statement, QueryAnalyzeMode queryMode) {
      return new SessionPoolResultSet(handler, delegate.analyzeQuery(statement, queryMode));
    }

    @Override
    public void close() {
      delegate.close();
    }
  }

  private static class AutoClosingTransactionManager<T extends SessionFuture>
      implements TransactionManager, SessionNotFoundHandler {
    private TransactionManager delegate;
    private T session;
    private final SessionReplacementHandler<T> sessionReplacementHandler;
    private final TransactionOption[] options;
    private boolean closed;
    private boolean restartedAfterSessionNotFound;

    AutoClosingTransactionManager(
        T session,
        SessionReplacementHandler<T> sessionReplacementHandler,
        TransactionOption... options) {
      this.session = session;
      this.options = options;
      this.sessionReplacementHandler = sessionReplacementHandler;
    }

    @Override
    public TransactionContext begin() {
      this.delegate = session.get().transactionManager(options);
      // This cannot throw a SessionNotFoundException, as it does not call the BeginTransaction RPC.
      // Instead, the BeginTransaction will be included with the first statement of the transaction.
      return internalBegin();
    }

    private TransactionContext internalBegin() {
      TransactionContext res = new SessionPoolTransactionContext(this, delegate.begin());
      session.get().markUsed();
      return res;
    }

    @Override
    public SpannerException handleSessionNotFound(SessionNotFoundException notFoundException) {
      session = sessionReplacementHandler.replaceSession(notFoundException, session);
      CachedSession cachedSession = session.get();
      delegate = cachedSession.getDelegate().transactionManager(options);
      restartedAfterSessionNotFound = true;
      return createAbortedExceptionWithMinimalRetryDelay(notFoundException);
    }

    private static SpannerException createAbortedExceptionWithMinimalRetryDelay(
        SessionNotFoundException notFoundException) {
      return SpannerExceptionFactory.newSpannerException(
          ErrorCode.ABORTED,
          notFoundException.getMessage(),
          SpannerExceptionFactory.createAbortedExceptionWithRetryDelay(
              notFoundException.getMessage(), notFoundException, 0, 1));
    }

    @Override
    public void commit() {
      try {
        delegate.commit();
      } catch (SessionNotFoundException e) {
        throw handleSessionNotFound(e);
      } finally {
        if (getState() != TransactionState.ABORTED) {
          close();
        }
      }
    }

    @Override
    public void rollback() {
      try {
        delegate.rollback();
      } finally {
        close();
      }
    }

    @Override
    public TransactionContext resetForRetry() {
      while (true) {
        try {
          if (restartedAfterSessionNotFound) {
            TransactionContext res = new SessionPoolTransactionContext(this, delegate.begin());
            restartedAfterSessionNotFound = false;
            return res;
          } else {
            return new SessionPoolTransactionContext(this, delegate.resetForRetry());
          }
        } catch (SessionNotFoundException e) {
          session = sessionReplacementHandler.replaceSession(e, session);
          CachedSession cachedSession = session.get();
          delegate = cachedSession.getDelegate().transactionManager(options);
          restartedAfterSessionNotFound = true;
        }
      }
    }

    @Override
    public Timestamp getCommitTimestamp() {
      return delegate.getCommitTimestamp();
    }

    @Override
    public CommitResponse getCommitResponse() {
      return delegate.getCommitResponse();
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      try {
        if (delegate != null) {
          delegate.close();
        }
      } finally {
        session.close();
      }
    }

    @Override
    public TransactionState getState() {
      if (restartedAfterSessionNotFound) {
        return TransactionState.ABORTED;
      } else {
        return delegate == null ? null : delegate.getState();
      }
    }
  }

  /**
   * {@link TransactionRunner} that automatically handles {@link SessionNotFoundException}s by
   * replacing the underlying session and then restarts the transaction.
   */
  private static final class SessionPoolTransactionRunner<I extends SessionFuture>
      implements TransactionRunner {

    private I session;
    private final SessionReplacementHandler<I> sessionReplacementHandler;
    private final TransactionOption[] options;
    private TransactionRunner runner;

    private SessionPoolTransactionRunner(
        I session,
        SessionReplacementHandler<I> sessionReplacementHandler,
        TransactionOption... options) {
      this.session = session;
      this.options = options;
      this.sessionReplacementHandler = sessionReplacementHandler;
    }

    private TransactionRunner getRunner() {
      if (this.runner == null) {
        this.runner = session.get().readWriteTransaction(options);
      }
      return runner;
    }

    @Override
    @Nullable
    public <T> T run(TransactionCallable<T> callable) {
      try {
        T result;
        while (true) {
          try {
            result = getRunner().run(callable);
            break;
          } catch (SessionNotFoundException e) {
            session = sessionReplacementHandler.replaceSession(e, session);
            CachedSession cachedSession = session.get();
            runner = cachedSession.getDelegate().readWriteTransaction();
          }
        }
        session.get().markUsed();
        return result;
      } catch (SpannerException e) {
        //noinspection ThrowableNotThrown
        session.get().setLastException(e);
        throw e;
      } finally {
        session.close();
      }
    }

    @Override
    public Timestamp getCommitTimestamp() {
      return getRunner().getCommitTimestamp();
    }

    @Override
    public CommitResponse getCommitResponse() {
      return getRunner().getCommitResponse();
    }

    @Override
    public TransactionRunner allowNestedTransaction() {
      getRunner().allowNestedTransaction();
      return this;
    }
  }

  private static class SessionPoolAsyncRunner<I extends SessionFuture> implements AsyncRunner {
    private volatile I session;
    private final SessionReplacementHandler<I> sessionReplacementHandler;
    private final TransactionOption[] options;
    private SettableApiFuture<CommitResponse> commitResponse;

    private SessionPoolAsyncRunner(
        I session,
        SessionReplacementHandler<I> sessionReplacementHandler,
        TransactionOption... options) {
      this.session = session;
      this.options = options;
      this.sessionReplacementHandler = sessionReplacementHandler;
    }

    @Override
    public <R> ApiFuture<R> runAsync(final AsyncWork<R> work, Executor executor) {
      commitResponse = SettableApiFuture.create();
      final SettableApiFuture<R> res = SettableApiFuture.create();
      executor.execute(
          () -> {
            SpannerException exception = null;
            R r = null;
            AsyncRunner runner = null;
            while (true) {
              SpannerException se = null;
              try {
                runner = session.get().runAsync(options);
                r = runner.runAsync(work, MoreExecutors.directExecutor()).get();
                break;
              } catch (ExecutionException e) {
                se = asSpannerException(e.getCause());
              } catch (InterruptedException e) {
                se = SpannerExceptionFactory.propagateInterrupt(e);
              } catch (Throwable t) {
                se = SpannerExceptionFactory.newSpannerException(t);
              } finally {
                if (se instanceof SessionNotFoundException) {
                  try {
                    // The replaceSession method will re-throw the SessionNotFoundException if the
                    // session cannot be replaced with a new one.
                    session =
                        sessionReplacementHandler.replaceSession(
                            (SessionNotFoundException) se, session);
                  } catch (SessionNotFoundException e) {
                    exception = e;
                    break;
                  }
                } else {
                  exception = se;
                  break;
                }
              }
            }
            session.get().markUsed();
            session.close();
            setCommitResponse(runner);
            if (exception != null) {
              res.setException(exception);
            } else {
              res.set(r);
            }
          });
      return res;
    }

    private void setCommitResponse(AsyncRunner delegate) {
      try {
        commitResponse.set(delegate.getCommitResponse().get());
      } catch (Throwable t) {
        commitResponse.setException(t);
      }
    }

    @Override
    public ApiFuture<Timestamp> getCommitTimestamp() {
      checkState(commitResponse != null, "runAsync() has not yet been called");
      return ApiFutures.transform(
          commitResponse, CommitResponse::getCommitTimestamp, MoreExecutors.directExecutor());
    }

    @Override
    public ApiFuture<CommitResponse> getCommitResponse() {
      checkState(commitResponse != null, "runAsync() has not yet been called");
      return commitResponse;
    }
  }

  // Exception class used just to track the stack trace at the point when a session was handed out
  // from the pool.
  final class LeakedSessionException extends RuntimeException {
    private static final long serialVersionUID = 1451131180314064914L;

    private LeakedSessionException() {
      super("Session was checked out from the pool at " + clock.instant());
    }

    private LeakedSessionException(String message) {
      super(message);
    }
  }

  private enum SessionState {
    AVAILABLE,
    BUSY,
    CLOSING,
  }

  private PooledSessionFuture createPooledSessionFuture(
      ListenableFuture<PooledSession> future, ISpan span) {
    return new PooledSessionFuture(future, span);
  }

  /** Wrapper class for the {@link SessionFuture} implementations. */
  interface SessionFutureWrapper<T extends SessionFuture> extends DatabaseClient {

    /** Method to resolve {@link SessionFuture} implementation for different use-cases. */
    T get();

    default Dialect getDialect() {
      return get().getDialect();
    }

    default String getDatabaseRole() {
      return get().getDatabaseRole();
    }

    default Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
      return get().write(mutations);
    }

    default CommitResponse writeWithOptions(
        Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
      return get().writeWithOptions(mutations, options);
    }

    default Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
      return get().writeAtLeastOnce(mutations);
    }

    default CommitResponse writeAtLeastOnceWithOptions(
        Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
      return get().writeAtLeastOnceWithOptions(mutations, options);
    }

    default ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
        Iterable<MutationGroup> mutationGroups, TransactionOption... options)
        throws SpannerException {
      return get().batchWriteAtLeastOnce(mutationGroups, options);
    }

    default ReadContext singleUse() {
      return get().singleUse();
    }

    default ReadContext singleUse(TimestampBound bound) {
      return get().singleUse(bound);
    }

    default ReadOnlyTransaction singleUseReadOnlyTransaction() {
      return get().singleUseReadOnlyTransaction();
    }

    default ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
      return get().singleUseReadOnlyTransaction(bound);
    }

    default ReadOnlyTransaction readOnlyTransaction() {
      return get().readOnlyTransaction();
    }

    default ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
      return get().readOnlyTransaction(bound);
    }

    default TransactionRunner readWriteTransaction(TransactionOption... options) {
      return get().readWriteTransaction(options);
    }

    default TransactionManager transactionManager(TransactionOption... options) {
      return get().transactionManager(options);
    }

    default AsyncRunner runAsync(TransactionOption... options) {
      return get().runAsync(options);
    }

    default AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
      return get().transactionManagerAsync(options);
    }

    default long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
      return get().executePartitionedUpdate(stmt, options);
    }
  }

  class PooledSessionFutureWrapper implements SessionFutureWrapper<PooledSessionFuture> {
    PooledSessionFuture pooledSessionFuture;

    public PooledSessionFutureWrapper(PooledSessionFuture pooledSessionFuture) {
      this.pooledSessionFuture = pooledSessionFuture;
    }

    @Override
    public PooledSessionFuture get() {
      return this.pooledSessionFuture;
    }
  }

  interface SessionFuture extends Session {

    /**
     * We need to do this because every implementation of {@link SessionFuture} today extends {@link
     * SimpleForwardingListenableFuture}. The get() method in parent {@link
     * java.util.concurrent.Future} classes specifies checked exceptions in method signature.
     *
     * <p>This method is a workaround we don't have to handle checked exceptions specified by other
     * interfaces.
     */
    CachedSession get();

    default void addListener(Runnable listener, Executor exec) {}
  }

  class PooledSessionFuture extends SimpleForwardingListenableFuture<PooledSession>
      implements SessionFuture {

    private volatile LeakedSessionException leakedException;
    private final AtomicBoolean inUse = new AtomicBoolean();
    private final CountDownLatch initialized = new CountDownLatch(1);
    private final ISpan span;

    @VisibleForTesting
    PooledSessionFuture(ListenableFuture<PooledSession> delegate, ISpan span) {
      super(delegate);
      this.span = span;
    }

    @VisibleForTesting
    void clearLeakedException() {
      this.leakedException = null;
    }

    private void markCheckedOut() {
      if (options.isTrackStackTraceOfSessionCheckout()) {
        this.leakedException = new LeakedSessionException();
        synchronized (SessionPool.this.lock) {
          SessionPool.this.markedCheckedOutSessions.add(this);
        }
      }
    }

    @Override
    public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
      return writeWithOptions(mutations).getCommitTimestamp();
    }

    @Override
    public CommitResponse writeWithOptions(
        Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
      try {
        return get().writeWithOptions(mutations, options);
      } finally {
        close();
      }
    }

    @Override
    public Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
      return writeAtLeastOnceWithOptions(mutations).getCommitTimestamp();
    }

    @Override
    public CommitResponse writeAtLeastOnceWithOptions(
        Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
      try {
        return get().writeAtLeastOnceWithOptions(mutations, options);
      } finally {
        close();
      }
    }

    @Override
    public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
        Iterable<MutationGroup> mutationGroups, TransactionOption... options)
        throws SpannerException {
      try {
        return get().batchWriteAtLeastOnce(mutationGroups, options);
      } finally {
        close();
      }
    }

    @Override
    public ReadContext singleUse() {
      try {
        return new AutoClosingReadContext<>(
            session -> {
              PooledSession ps = session.get();
              return ps.delegate.singleUse();
            },
            SessionPool.this,
            pooledSessionReplacementHandler,
            this,
            true);
      } catch (Exception e) {
        close();
        throw e;
      }
    }

    @Override
    public ReadContext singleUse(final TimestampBound bound) {
      try {
        return new AutoClosingReadContext<>(
            session -> {
              PooledSession ps = session.get();
              return ps.delegate.singleUse(bound);
            },
            SessionPool.this,
            pooledSessionReplacementHandler,
            this,
            true);
      } catch (Exception e) {
        close();
        throw e;
      }
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction() {
      return internalReadOnlyTransaction(
          session -> {
            PooledSession ps = session.get();
            return ps.delegate.singleUseReadOnlyTransaction();
          },
          true);
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction(final TimestampBound bound) {
      return internalReadOnlyTransaction(
          session -> {
            PooledSession ps = session.get();
            return ps.delegate.singleUseReadOnlyTransaction(bound);
          },
          true);
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction() {
      return internalReadOnlyTransaction(
          session -> {
            PooledSession ps = session.get();
            return ps.delegate.readOnlyTransaction();
          },
          false);
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction(final TimestampBound bound) {
      return internalReadOnlyTransaction(
          session -> {
            PooledSession ps = session.get();
            return ps.delegate.readOnlyTransaction(bound);
          },
          false);
    }

    private ReadOnlyTransaction internalReadOnlyTransaction(
        Function<PooledSessionFuture, ReadOnlyTransaction> transactionSupplier,
        boolean isSingleUse) {
      try {
        return new AutoClosingReadTransaction<>(
            transactionSupplier,
            SessionPool.this,
            pooledSessionReplacementHandler,
            this,
            isSingleUse);
      } catch (Exception e) {
        close();
        throw e;
      }
    }

    @Override
    public TransactionRunner readWriteTransaction(TransactionOption... options) {
      return new SessionPoolTransactionRunner<>(this, pooledSessionReplacementHandler, options);
    }

    @Override
    public TransactionManager transactionManager(TransactionOption... options) {
      return new AutoClosingTransactionManager<>(this, pooledSessionReplacementHandler, options);
    }

    @Override
    public AsyncRunner runAsync(TransactionOption... options) {
      return new SessionPoolAsyncRunner<>(this, pooledSessionReplacementHandler, options);
    }

    @Override
    public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
      return new SessionPoolAsyncTransactionManager<>(
          pooledSessionReplacementHandler, this, options);
    }

    @Override
    public long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
      try {
        return get(true).executePartitionedUpdate(stmt, options);
      } finally {
        close();
      }
    }

    @Override
    public String getName() {
      return get().getName();
    }

    @Override
    public void close() {
      try {
        asyncClose().get();
      } catch (InterruptedException e) {
        throw SpannerExceptionFactory.propagateInterrupt(e);
      } catch (ExecutionException e) {
        throw asSpannerException(e.getCause());
      }
    }

    @Override
    public ApiFuture<Empty> asyncClose() {
      try {
        PooledSession delegate = getOrNull();
        if (delegate != null) {
          return delegate.asyncClose();
        }
      } finally {
        synchronized (lock) {
          leakedException = null;
          checkedOutSessions.remove(this);
          markedCheckedOutSessions.remove(this);
        }
      }
      return ApiFutures.immediateFuture(Empty.getDefaultInstance());
    }

    private PooledSession getOrNull() {
      try {
        return get();
      } catch (Throwable t) {
        return null;
      }
    }

    @Override
    public PooledSession get() {
      return get(false);
    }

    PooledSession get(final boolean eligibleForLongRunning) {
      if (inUse.compareAndSet(false, true)) {
        PooledSession res = null;
        try {
          res = super.get();
        } catch (Throwable e) {
          // ignore the exception as it will be handled by the call to super.get() below.
        }
        if (res != null) {
          res.markBusy(span);
          span.addAnnotation("Using Session", "sessionId", res.getName());
          synchronized (lock) {
            incrementNumSessionsInUse();
            checkedOutSessions.add(this);
          }
          res.eligibleForLongRunning = eligibleForLongRunning;
        }
        initialized.countDown();
      }
      try {
        initialized.await();
        return super.get();
      } catch (ExecutionException e) {
        throw SpannerExceptionFactory.newSpannerException(e.getCause());
      } catch (InterruptedException e) {
        throw SpannerExceptionFactory.propagateInterrupt(e);
      }
    }
  }

  interface CachedSession extends Session {

    SessionImpl getDelegate();

    void markBusy(ISpan span);

    void markUsed();

    SpannerException setLastException(SpannerException exception);

    AsyncTransactionManagerImpl transactionManagerAsync(TransactionOption... options);

    void setAllowReplacing(boolean b);
  }

  class PooledSession implements CachedSession {

    @VisibleForTesting final SessionImpl delegate;
    private volatile SpannerException lastException;
    private volatile boolean allowReplacing = true;

    /**
     * This ensures that the session is added at a random position in the pool the first time it is
     * actually added to the pool.
     */
    @GuardedBy("lock")
    private Position releaseToPosition = initialReleasePosition;

    /**
     * Property to mark if the session is eligible to be long-running. This can only be true if the
     * session is executing certain types of transactions (for ex - Partitioned DML) which can be
     * long-running. By default, most transaction types are not expected to be long-running and
     * hence this value is false.
     */
    private volatile boolean eligibleForLongRunning = false;

    /**
     * Property to mark if the session is no longer part of the session pool. For ex - A session
     * which is long-running gets cleaned up and removed from the pool.
     */
    private volatile boolean isRemovedFromPool = false;

    /**
     * Property to mark if a leaked session exception is already logged. Given a session maintainer
     * thread runs repeatedly at a defined interval, this property allows us to ensure that an
     * exception is logged only once per leaked session. This is to avoid noisy repeated logs around
     * session leaks for long-running sessions.
     */
    private volatile boolean isLeakedExceptionLogged = false;

    @GuardedBy("lock")
    private SessionState state;

    private PooledSession(SessionImpl delegate) {
      this.delegate = Preconditions.checkNotNull(delegate);
      this.state = SessionState.AVAILABLE;

      // initialise the lastUseTime field for each session.
      this.markUsed();
    }

    int getChannel() {
      Long channelHint = (Long) delegate.getOptions().get(SpannerRpc.Option.CHANNEL_HINT);
      return channelHint == null
          ? 0
          : (int) (channelHint % sessionClient.getSpanner().getOptions().getNumChannels());
    }

    @Override
    public String toString() {
      return getName();
    }

    @VisibleForTesting
    @Override
    public void setAllowReplacing(boolean allowReplacing) {
      this.allowReplacing = allowReplacing;
    }

    @VisibleForTesting
    void setEligibleForLongRunning(boolean eligibleForLongRunning) {
      this.eligibleForLongRunning = eligibleForLongRunning;
    }

    @Override
    public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
      return writeWithOptions(mutations).getCommitTimestamp();
    }

    @Override
    public CommitResponse writeWithOptions(
        Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
      try {
        markUsed();
        return delegate.writeWithOptions(mutations, options);
      } catch (SpannerException e) {
        throw lastException = e;
      }
    }

    @Override
    public Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
      return writeAtLeastOnceWithOptions(mutations).getCommitTimestamp();
    }

    @Override
    public CommitResponse writeAtLeastOnceWithOptions(
        Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
      try {
        markUsed();
        return delegate.writeAtLeastOnceWithOptions(mutations, options);
      } catch (SpannerException e) {
        throw lastException = e;
      }
    }

    @Override
    public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
        Iterable<MutationGroup> mutationGroups, TransactionOption... options)
        throws SpannerException {
      try {
        markUsed();
        return delegate.batchWriteAtLeastOnce(mutationGroups, options);
      } catch (SpannerException e) {
        throw lastException = e;
      }
    }

    @Override
    public long executePartitionedUpdate(Statement stmt, UpdateOption... options)
        throws SpannerException {
      try {
        markUsed();
        return delegate.executePartitionedUpdate(stmt, options);
      } catch (SpannerException e) {
        throw lastException = e;
      }
    }

    @Override
    public ReadContext singleUse() {
      return delegate.singleUse();
    }

    @Override
    public ReadContext singleUse(TimestampBound bound) {
      return delegate.singleUse(bound);
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction() {
      return delegate.singleUseReadOnlyTransaction();
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
      return delegate.singleUseReadOnlyTransaction(bound);
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction() {
      return delegate.readOnlyTransaction();
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
      return delegate.readOnlyTransaction(bound);
    }

    @Override
    public TransactionRunner readWriteTransaction(TransactionOption... options) {
      return delegate.readWriteTransaction(options);
    }

    @Override
    public AsyncRunner runAsync(TransactionOption... options) {
      return delegate.runAsync(options);
    }

    @Override
    public AsyncTransactionManagerImpl transactionManagerAsync(TransactionOption... options) {
      return delegate.transactionManagerAsync(options);
    }

    @Override
    public ApiFuture<Empty> asyncClose() {
      close();
      return ApiFutures.immediateFuture(Empty.getDefaultInstance());
    }

    @Override
    public void close() {
      synchronized (lock) {
        numSessionsInUse--;
        numSessionsReleased++;
      }
      if ((lastException != null && isSessionNotFound(lastException)) || isRemovedFromPool) {
        invalidateSession(this);
      } else {
        if (isDatabaseOrInstanceNotFound(lastException)) {
          // Mark this session pool as no longer valid and then release the session into the pool as
          // there is nothing we can do with it anyways.
          synchronized (lock) {
            SessionPool.this.resourceNotFoundException =
                MoreObjects.firstNonNull(
                    SessionPool.this.resourceNotFoundException,
                    (ResourceNotFoundException) lastException);
          }
        }
        lastException = null;
        isRemovedFromPool = false;
        if (state != SessionState.CLOSING) {
          state = SessionState.AVAILABLE;
        }
        releaseSession(this, false);
      }
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    private void keepAlive() {
      markUsed();
      final ISpan previousSpan = delegate.getCurrentSpan();
      delegate.setCurrentSpan(tracer.getBlankSpan());
      try (ResultSet resultSet =
          delegate
              .singleUse(TimestampBound.ofMaxStaleness(60, TimeUnit.SECONDS))
              .executeQuery(Statement.newBuilder("SELECT 1").build())) {
        resultSet.next();
      } finally {
        delegate.setCurrentSpan(previousSpan);
      }
    }

    private void determineDialectAsync(final SettableFuture<Dialect> dialect) {
      Preconditions.checkNotNull(dialect);
      executor.submit(
          () -> {
            try {
              dialect.set(determineDialect());
            } catch (Throwable t) {
              // Catch-all as we want to propagate all exceptions to anyone who might be interested
              // in the database dialect, and there's nothing sensible that we can do with it here.
              dialect.setException(t);
            } finally {
              releaseSession(this, false);
            }
          });
    }

    private Dialect determineDialect() {
      try (ResultSet dialectResultSet =
          delegate.singleUse().executeQuery(DETERMINE_DIALECT_STATEMENT)) {
        if (dialectResultSet.next()) {
          return Dialect.fromName(dialectResultSet.getString(0));
        } else {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.NOT_FOUND, "No dialect found for database");
        }
      }
    }

    @Override
    public SessionImpl getDelegate() {
      return this.delegate;
    }

    @Override
    public void markBusy(ISpan span) {
      this.delegate.setCurrentSpan(span);
      this.state = SessionState.BUSY;
    }

    private void markClosing() {
      this.state = SessionState.CLOSING;
    }

    @Override
    public void markUsed() {
      delegate.markUsed(clock.instant());
    }

    @Override
    public SpannerException setLastException(SpannerException exception) {
      this.lastException = exception;
      return exception;
    }

    boolean isAllowReplacing() {
      return this.allowReplacing;
    }

    @Override
    public TransactionManager transactionManager(TransactionOption... options) {
      return delegate.transactionManager(options);
    }
  }

  private final class WaiterFuture extends ForwardingListenableFuture<PooledSession> {
    private static final long MAX_SESSION_WAIT_TIMEOUT = 240_000L;
    private final SettableFuture<PooledSession> waiter = SettableFuture.create();

    @Override
    @Nonnull
    protected ListenableFuture<? extends PooledSession> delegate() {
      return waiter;
    }

    private void put(PooledSession session) {
      waiter.set(session);
    }

    private void put(SpannerException e) {
      waiter.setException(e);
    }

    @Override
    public PooledSession get() {
      long currentTimeout = options.getInitialWaitForSessionTimeoutMillis();
      while (true) {
        ISpan span = tracer.spanBuilder(WAIT_FOR_SESSION);
        try (IScope ignore = tracer.withSpan(span)) {
          PooledSession s =
              pollUninterruptiblyWithTimeout(currentTimeout, options.getAcquireSessionTimeout());
          if (s == null) {
            // Set the status to DEADLINE_EXCEEDED and retry.
            numWaiterTimeouts.incrementAndGet();
            tracer.getCurrentSpan().setStatus(ErrorCode.DEADLINE_EXCEEDED);
            currentTimeout = Math.min(currentTimeout * 2, MAX_SESSION_WAIT_TIMEOUT);
          } else {
            return s;
          }
        } catch (Exception e) {
          if (e instanceof SpannerException
              && ErrorCode.RESOURCE_EXHAUSTED.equals(((SpannerException) e).getErrorCode())) {
            numWaiterTimeouts.incrementAndGet();
            tracer.getCurrentSpan().setStatus(ErrorCode.RESOURCE_EXHAUSTED);
          }
          span.setStatus(e);
          throw e;
        } finally {
          span.end();
        }
      }
    }

    private PooledSession pollUninterruptiblyWithTimeout(
        long timeoutMillis, Duration acquireSessionTimeout) {
      boolean interrupted = false;
      try {
        while (true) {
          try {
            return acquireSessionTimeout == null
                ? waiter.get(timeoutMillis, TimeUnit.MILLISECONDS)
                : waiter.get(acquireSessionTimeout.toMillis(), TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            interrupted = true;
          } catch (TimeoutException e) {
            if (acquireSessionTimeout != null) {
              SpannerException exception =
                  SpannerExceptionFactory.newSpannerException(
                      ErrorCode.RESOURCE_EXHAUSTED,
                      "Timed out after waiting "
                          + acquireSessionTimeout.toMillis()
                          + "ms for acquiring session. To mitigate error SessionPoolOptions#setAcquireSessionTimeout(Duration) to set a higher timeout"
                          + " or increase the number of sessions in the session pool.\n"
                          + createCheckedOutSessionsStackTraces());
              if (waiter.setException(exception)) {
                // Only throw the exception if setting it on the waiter was successful. The
                // waiter.setException(..) method returns false if some other thread in the meantime
                // called waiter.set(..), which means that a session became available between the
                // time that the TimeoutException was thrown and now.
                throw exception;
              }
            }
            return null;
          } catch (ExecutionException e) {
            throw SpannerExceptionFactory.newSpannerException(e.getCause());
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Background task to maintain the pool. Tasks:
   *
   * <ul>
   *   <li>Removes idle sessions from the pool. Sessions that go above MinSessions that have not
   *       been used for the last 55 minutes will be removed from the pool. These will automatically
   *       be garbage collected by the backend.
   *   <li>Keeps alive sessions that have not been used for a user configured time in order to keep
   *       MinSessions sessions alive in the pool at any time. The keep-alive traffic is smeared out
   *       over a window of 10 minutes to avoid bursty traffic.
   *   <li>Removes unexpected long running transactions from the pool. Only certain transaction
   *       types (for ex - Partitioned DML / Batch Reads) can be long running. This tasks checks the
   *       sessions which have been inactive for a longer than usual duration (for ex - 60 minutes)
   *       and removes such sessions from the pool.
   * </ul>
   */
  final class PoolMaintainer {

    // Length of the window in millis over which we keep track of maximum number of concurrent
    // sessions in use.
    private final Duration windowLength = Duration.ofMillis(TimeUnit.MINUTES.toMillis(10));
    // Frequency of the timer loop.
    @VisibleForTesting final long loopFrequency = options.getLoopFrequency();
    // Number of loop iterations in which we need to close all the sessions waiting for closure.
    @VisibleForTesting final long numClosureCycles = windowLength.toMillis() / loopFrequency;
    private final Duration keepAliveMillis =
        Duration.ofMillis(TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()));
    // Number of loop iterations in which we need to keep alive all the sessions
    @VisibleForTesting final long numKeepAliveCycles = keepAliveMillis.toMillis() / loopFrequency;

    /**
     * Variable maintaining the last execution time of the long-running transaction cleanup task.
     *
     * <p>The long-running transaction cleanup needs to be performed every X minutes. The X minutes
     * recurs multiple times within the invocation of the pool maintainer thread. For ex - If the
     * main thread runs every 10s and the long-running transaction clean-up needs to be performed
     * every 2 minutes, then we need to keep a track of when was the last time that this task
     * executed and makes sure we only execute it every 2 minutes and not every 10 seconds.
     */
    @VisibleForTesting Instant lastExecutionTime;

    /**
     * The previous numSessionsAcquired seen by the maintainer. This is used to calculate the
     * transactions per second, which again is used to determine whether to randomize the order of
     * the session pool.
     */
    private long prevNumSessionsAcquired;

    boolean closed = false;

    @GuardedBy("lock")
    ScheduledFuture<?> scheduledFuture;

    @GuardedBy("lock")
    boolean running;

    void init() {
      lastExecutionTime = clock.instant();

      // Scheduled pool maintenance worker.
      synchronized (lock) {
        scheduledFuture =
            executor.scheduleAtFixedRate(
                this::maintainPool, loopFrequency, loopFrequency, TimeUnit.MILLISECONDS);
      }
    }

    void close() {
      synchronized (lock) {
        if (!closed) {
          closed = true;
          scheduledFuture.cancel(false);
          if (!running) {
            decrementPendingClosures(1);
          }
        }
      }
    }

    boolean isClosed() {
      synchronized (lock) {
        return closed;
      }
    }

    // Does various pool maintenance activities.
    void maintainPool() {
      synchronized (lock) {
        if (SessionPool.this.isClosed()) {
          return;
        }
        running = true;
        if (loopFrequency >= 1000L) {
          SessionPool.this.transactionsPerSecond =
              (SessionPool.this.numSessionsAcquired - prevNumSessionsAcquired)
                  / (loopFrequency / 1000L);
        }
        this.prevNumSessionsAcquired = SessionPool.this.numSessionsAcquired;
      }
      Instant currTime = clock.instant();
      removeIdleSessions(currTime);
      // Now go over all the remaining sessions and see if they need to be kept alive explicitly.
      keepAliveSessions(currTime);
      replenishPool();
      synchronized (lock) {
        running = false;
        if (SessionPool.this.isClosed()) {
          decrementPendingClosures(1);
        }
      }
      removeLongRunningSessions(currTime);
    }

    private void removeIdleSessions(Instant currTime) {
      synchronized (lock) {
        // Determine the minimum last use time for a session to be deemed to still be alive. Remove
        // all sessions that have a lastUseTime before that time, unless it would cause us to go
        // below MinSessions.
        Instant minLastUseTime = currTime.minus(options.getRemoveInactiveSessionAfter());
        Iterator<PooledSession> iterator = sessions.descendingIterator();
        while (iterator.hasNext()) {
          PooledSession session = iterator.next();
          if (session.delegate.getLastUseTime().isBefore(minLastUseTime)) {
            if (session.state != SessionState.CLOSING) {
              boolean isRemoved = removeFromPool(session);
              if (isRemoved) {
                numIdleSessionsRemoved++;
                if (idleSessionRemovedListener != null) {
                  idleSessionRemovedListener.apply(session);
                }
              }
              iterator.remove();
            }
          }
        }
      }
    }

    private void keepAliveSessions(Instant currTime) {
      long numSessionsToKeepAlive = 0;
      synchronized (lock) {
        if (numSessionsInUse >= (options.getMinSessions() + options.getMaxIdleSessions())) {
          // At least MinSessions are in use, so we don't have to ping any sessions.
          return;
        }
        // In each cycle only keep alive a subset of sessions to prevent burst of traffic.
        numSessionsToKeepAlive =
            (long)
                Math.ceil(
                    (double)
                            ((options.getMinSessions() + options.getMaxIdleSessions())
                                - numSessionsInUse)
                        / numKeepAliveCycles);
      }
      // Now go over all the remaining sessions and see if they need to be kept alive explicitly.
      Instant keepAliveThreshold = currTime.minus(keepAliveMillis);

      // Keep chugging till there is no session that needs to be kept alive.
      while (numSessionsToKeepAlive > 0) {
        Tuple<PooledSession, Integer> sessionToKeepAlive;
        synchronized (lock) {
          sessionToKeepAlive = findSessionToKeepAlive(sessions, keepAliveThreshold, 0);
        }
        if (sessionToKeepAlive == null) {
          break;
        }
        try {
          logger.log(Level.FINE, "Keeping alive session " + sessionToKeepAlive.x().getName());
          numSessionsToKeepAlive--;
          sessionToKeepAlive.x().keepAlive();
          releaseSession(sessionToKeepAlive);
        } catch (SpannerException e) {
          handleException(e, sessionToKeepAlive);
        }
      }
    }

    private void replenishPool() {
      synchronized (lock) {
        // If we have gone below min pool size, create that many sessions.
        int sessionCount = options.getMinSessions() - (totalSessions() + numSessionsBeingCreated);
        if (sessionCount > 0) {
          createSessions(getAllowedCreateSessions(sessionCount), false);
        }
      }
    }

    // cleans up sessions which are unexpectedly long-running.
    void removeLongRunningSessions(Instant currentTime) {
      try {
        if (SessionPool.this.isClosed()) {
          return;
        }
        final InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
            options.getInactiveTransactionRemovalOptions();
        final Instant minExecutionTime =
            lastExecutionTime.plus(inactiveTransactionRemovalOptions.getExecutionFrequency());
        if (currentTime.isBefore(minExecutionTime)) {
          return;
        }
        lastExecutionTime = currentTime; // update this only after we have decided to execute task
        if (options.closeInactiveTransactions()
            || options.warnInactiveTransactions()
            || options.warnAndCloseInactiveTransactions()) {
          removeLongRunningSessions(currentTime, inactiveTransactionRemovalOptions);
        }
      } catch (final Throwable t) {
        logger.log(Level.WARNING, "Failed removing long running transactions", t);
      }
    }

    private void removeLongRunningSessions(
        final Instant currentTime,
        final InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions) {
      synchronized (lock) {
        final double usedSessionsRatio = getRatioOfSessionsInUse();
        if (usedSessionsRatio > inactiveTransactionRemovalOptions.getUsedSessionsRatioThreshold()) {
          Iterator<PooledSessionFuture> iterator = checkedOutSessions.iterator();
          while (iterator.hasNext()) {
            final PooledSessionFuture sessionFuture = iterator.next();
            // the below get() call on future object is non-blocking since checkedOutSessions
            // collection is populated only when the get() method in {@code PooledSessionFuture} is
            // called.
            final PooledSession session = (PooledSession) sessionFuture.get();
            final Duration durationFromLastUse =
                Duration.between(session.getDelegate().getLastUseTime(), currentTime);
            if (!session.eligibleForLongRunning
                && durationFromLastUse.compareTo(
                        inactiveTransactionRemovalOptions.getIdleTimeThreshold())
                    > 0) {
              if ((options.warnInactiveTransactions() || options.warnAndCloseInactiveTransactions())
                  && !session.isLeakedExceptionLogged) {
                if (options.warnAndCloseInactiveTransactions()) {
                  logger.log(
                      Level.WARNING,
                      String.format("Removing long-running session => %s", session.getName()),
                      sessionFuture.leakedException);
                  session.isLeakedExceptionLogged = true;
                } else if (options.warnInactiveTransactions()) {
                  logger.log(
                      Level.WARNING,
                      String.format(
                          "Detected long-running session => %s. To automatically remove "
                              + "long-running sessions, set SessionOption ActionOnInactiveTransaction "
                              + "to WARN_AND_CLOSE by invoking setWarnAndCloseIfInactiveTransactions() method.",
                          session.getName()),
                      sessionFuture.leakedException);
                  session.isLeakedExceptionLogged = true;
                }
              }
              if ((options.closeInactiveTransactions()
                      || options.warnAndCloseInactiveTransactions())
                  && session.state != SessionState.CLOSING) {
                final boolean isRemoved = removeFromPool(session);
                if (isRemoved) {
                  session.isRemovedFromPool = true;
                  numLeakedSessionsRemoved++;
                  if (longRunningSessionRemovedListener != null) {
                    longRunningSessionRemovedListener.apply(session);
                  }
                }
                iterator.remove();
              }
            }
          }
        }
      }
    }
  }

  enum Position {
    FIRST,
    LAST,
    RANDOM
  }

  /**
   * This statement is (currently) used to determine the dialect of the database that is used by the
   * session pool. This statement is subject to change when the INFORMATION_SCHEMA contains a table
   * where the dialect of the database can be read directly, and any tests that want to detect the
   * specific 'determine dialect statement' should rely on this constant instead of the actual
   * value.
   */
  @VisibleForTesting
  static final Statement DETERMINE_DIALECT_STATEMENT =
      Statement.newBuilder(
              "SELECT 'POSTGRESQL' AS DIALECT\n"
                  + "FROM INFORMATION_SCHEMA.SCHEMATA\n"
                  + "WHERE SCHEMA_NAME='information_schema'\n"
                  + "UNION ALL\n"
                  + "SELECT 'GOOGLE_STANDARD_SQL' AS DIALECT\n"
                  + "FROM INFORMATION_SCHEMA.SCHEMATA\n"
                  + "WHERE SCHEMA_NAME='INFORMATION_SCHEMA' AND CATALOG_NAME=''")
          .build();

  private final SessionPoolOptions options;
  private final SettableFuture<Dialect> dialect = SettableFuture.create();
  private final String databaseRole;
  private final SessionClient sessionClient;
  private final int numChannels;
  private final ScheduledExecutorService executor;
  private final ExecutorFactory<ScheduledExecutorService> executorFactory;

  final PoolMaintainer poolMaintainer;
  private final Clock clock;
  /**
   * initialReleasePosition determines where in the pool sessions are added when they are released
   * into the pool the first time. This is always RANDOM in production, but some tests use FIRST to
   * be able to verify the order of sessions in the pool. Using RANDOM ensures that we do not get an
   * unbalanced session pool where all sessions belonging to one gRPC channel are added to the same
   * region in the pool.
   */
  private final Position initialReleasePosition;

  private final Object lock = new Object();
  private final Random random = new Random();

  @GuardedBy("lock")
  private boolean detectDialectStarted;

  @GuardedBy("lock")
  private int pendingClosure;

  @GuardedBy("lock")
  private SettableFuture<Void> closureFuture;

  @GuardedBy("lock")
  private ClosedException closedException;

  @GuardedBy("lock")
  private ResourceNotFoundException resourceNotFoundException;

  @GuardedBy("lock")
  private final LinkedList<PooledSession> sessions = new LinkedList<>();

  @GuardedBy("lock")
  private final Queue<WaiterFuture> waiters = new LinkedList<>();

  @GuardedBy("lock")
  private int numSessionsBeingCreated = 0;

  @GuardedBy("lock")
  private int numSessionsInUse = 0;

  @GuardedBy("lock")
  private int maxSessionsInUse = 0;

  @GuardedBy("lock")
  private long numSessionsAcquired = 0;

  @GuardedBy("lock")
  private long numSessionsReleased = 0;

  @GuardedBy("lock")
  private long numIdleSessionsRemoved = 0;

  @GuardedBy("lock")
  private long transactionsPerSecond = 0L;

  @GuardedBy("lock")
  private long numLeakedSessionsRemoved = 0;

  private final AtomicLong numWaiterTimeouts = new AtomicLong();

  @GuardedBy("lock")
  private final Set<PooledSession> allSessions = new HashSet<>();

  @GuardedBy("lock")
  @VisibleForTesting
  final Set<PooledSessionFuture> checkedOutSessions = new HashSet<>();

  @GuardedBy("lock")
  private final Set<PooledSessionFuture> markedCheckedOutSessions = new HashSet<>();

  private final SessionConsumer sessionConsumer = new SessionConsumerImpl();

  @VisibleForTesting Function<PooledSession, Void> idleSessionRemovedListener;

  @VisibleForTesting Function<PooledSession, Void> longRunningSessionRemovedListener;
  private final CountDownLatch waitOnMinSessionsLatch;
  private final PooledSessionReplacementHandler pooledSessionReplacementHandler =
      new PooledSessionReplacementHandler();

  /**
   * Create a session pool with the given options and for the given database. It will also start
   * eagerly creating sessions if {@link SessionPoolOptions#getMinSessions()} is greater than 0.
   * Return pool is immediately ready for use, though getting a session might block for sessions to
   * be created.
   */
  static SessionPool createPool(
      SpannerOptions spannerOptions,
      SessionClient sessionClient,
      TraceWrapper tracer,
      List<LabelValue> labelValues,
      Attributes attributes,
      AtomicLong numMultiplexedSessionsAcquired,
      AtomicLong numMultiplexedSessionsReleased) {
    final SessionPoolOptions sessionPoolOptions = spannerOptions.getSessionPoolOptions();

    // A clock instance is passed in {@code SessionPoolOptions} in order to allow mocking via tests.
    final Clock poolMaintainerClock = sessionPoolOptions.getPoolMaintainerClock();
    return createPool(
        sessionPoolOptions,
        spannerOptions.getDatabaseRole(),
        ((GrpcTransportOptions) spannerOptions.getTransportOptions()).getExecutorFactory(),
        sessionClient,
        poolMaintainerClock == null ? new Clock() : poolMaintainerClock,
        Position.RANDOM,
        Metrics.getMetricRegistry(),
        tracer,
        labelValues,
        spannerOptions.getOpenTelemetry(),
        attributes,
        numMultiplexedSessionsAcquired,
        numMultiplexedSessionsReleased);
  }

  static SessionPool createPool(
      SessionPoolOptions poolOptions,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      SessionClient sessionClient,
      TraceWrapper tracer,
      OpenTelemetry openTelemetry) {
    return createPool(
        poolOptions,
        executorFactory,
        sessionClient,
        new Clock(),
        Position.RANDOM,
        tracer,
        openTelemetry);
  }

  static SessionPool createPool(
      SessionPoolOptions poolOptions,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      SessionClient sessionClient,
      Clock clock,
      Position initialReleasePosition,
      TraceWrapper tracer,
      OpenTelemetry openTelemetry) {
    return createPool(
        poolOptions,
        null,
        executorFactory,
        sessionClient,
        clock,
        initialReleasePosition,
        Metrics.getMetricRegistry(),
        tracer,
        SPANNER_DEFAULT_LABEL_VALUES,
        openTelemetry,
        null,
        new AtomicLong(),
        new AtomicLong());
  }

  static SessionPool createPool(
      SessionPoolOptions poolOptions,
      String databaseRole,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      SessionClient sessionClient,
      Clock clock,
      Position initialReleasePosition,
      MetricRegistry metricRegistry,
      TraceWrapper tracer,
      List<LabelValue> labelValues,
      OpenTelemetry openTelemetry,
      Attributes attributes,
      AtomicLong numMultiplexedSessionsAcquired,
      AtomicLong numMultiplexedSessionsReleased) {
    SessionPool pool =
        new SessionPool(
            poolOptions,
            databaseRole,
            executorFactory,
            executorFactory.get(),
            sessionClient,
            clock,
            initialReleasePosition,
            metricRegistry,
            tracer,
            labelValues,
            openTelemetry,
            attributes,
            numMultiplexedSessionsAcquired,
            numMultiplexedSessionsReleased);
    pool.initPool();
    return pool;
  }

  private SessionPool(
      SessionPoolOptions options,
      String databaseRole,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      ScheduledExecutorService executor,
      SessionClient sessionClient,
      Clock clock,
      Position initialReleasePosition,
      MetricRegistry metricRegistry,
      TraceWrapper tracer,
      List<LabelValue> labelValues,
      OpenTelemetry openTelemetry,
      Attributes attributes,
      AtomicLong numMultiplexedSessionsAcquired,
      AtomicLong numMultiplexedSessionsReleased) {
    this.options = options;
    this.databaseRole = databaseRole;
    this.executorFactory = executorFactory;
    this.executor = executor;
    this.sessionClient = sessionClient;
    this.numChannels = sessionClient.getSpanner().getOptions().getNumChannels();
    this.clock = clock;
    this.initialReleasePosition = initialReleasePosition;
    this.poolMaintainer = new PoolMaintainer();
    this.tracer = tracer;
    this.initOpenCensusMetricsCollection(
        metricRegistry,
        labelValues,
        numMultiplexedSessionsAcquired,
        numMultiplexedSessionsReleased);
    this.initOpenTelemetryMetricsCollection(
        openTelemetry, attributes, numMultiplexedSessionsAcquired, numMultiplexedSessionsReleased);
    this.waitOnMinSessionsLatch =
        options.getMinSessions() > 0 ? new CountDownLatch(1) : new CountDownLatch(0);
  }

  /**
   * @return the {@link Dialect} of the underlying database. This method will block until the
   *     dialect is available. It will potentially execute one or two RPCs to get the dialect if
   *     necessary: One to create a session if there are no sessions in the pool (yet), and one to
   *     query the database for the dialect that is used. It is recommended that clients that always
   *     need to know the dialect set {@link
   *     SessionPoolOptions.Builder#setAutoDetectDialect(boolean)} to true. This will ensure that
   *     the dialect is fetched automatically in a background task when a session pool is created.
   */
  Dialect getDialect() {
    boolean mustDetectDialect = false;
    synchronized (lock) {
      if (!detectDialectStarted) {
        mustDetectDialect = true;
        detectDialectStarted = true;
      }
    }
    if (mustDetectDialect) {
      try (PooledSessionFuture session = getSession()) {
        dialect.set(((PooledSession) session.get()).determineDialect());
      }
    }
    try {
      return dialect.get(60L, TimeUnit.SECONDS);
    } catch (ExecutionException executionException) {
      throw asSpannerException(executionException);
    } catch (InterruptedException interruptedException) {
      throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
    } catch (TimeoutException timeoutException) {
      throw SpannerExceptionFactory.propagateTimeout(timeoutException);
    }
  }

  PooledSessionReplacementHandler getPooledSessionReplacementHandler() {
    return pooledSessionReplacementHandler;
  }

  @Nullable
  public String getDatabaseRole() {
    return databaseRole;
  }

  @VisibleForTesting
  int getNumberOfSessionsInUse() {
    synchronized (lock) {
      return numSessionsInUse;
    }
  }

  @VisibleForTesting
  int getMaxSessionsInUse() {
    synchronized (lock) {
      return maxSessionsInUse;
    }
  }

  @VisibleForTesting
  double getRatioOfSessionsInUse() {
    synchronized (lock) {
      final int maxSessions = options.getMaxSessions();
      if (maxSessions == 0) {
        return 0;
      }
      return (double) numSessionsInUse / maxSessions;
    }
  }

  boolean removeFromPool(PooledSession session) {
    synchronized (lock) {
      if (isClosed()) {
        decrementPendingClosures(1);
        return false;
      }
      session.markClosing();
      allSessions.remove(session);
      return true;
    }
  }

  long numIdleSessionsRemoved() {
    synchronized (lock) {
      return numIdleSessionsRemoved;
    }
  }

  @VisibleForTesting
  long numLeakedSessionsRemoved() {
    synchronized (lock) {
      return numLeakedSessionsRemoved;
    }
  }

  @VisibleForTesting
  int getNumberOfSessionsInPool() {
    synchronized (lock) {
      return sessions.size();
    }
  }

  @VisibleForTesting
  int getNumberOfSessionsBeingCreated() {
    synchronized (lock) {
      return numSessionsBeingCreated;
    }
  }

  @VisibleForTesting
  int getTotalSessionsPlusNumSessionsBeingCreated() {
    synchronized (lock) {
      return numSessionsBeingCreated + allSessions.size();
    }
  }

  @VisibleForTesting
  long getNumWaiterTimeouts() {
    return numWaiterTimeouts.get();
  }

  private void initPool() {
    synchronized (lock) {
      poolMaintainer.init();
      if (options.getMinSessions() > 0) {
        createSessions(options.getMinSessions(), true);
      }
    }
  }

  private boolean isClosed() {
    synchronized (lock) {
      return closureFuture != null;
    }
  }

  private void handleException(SpannerException e, Tuple<PooledSession, Integer> session) {
    if (isSessionNotFound(e)) {
      invalidateSession(session.x());
    } else {
      releaseSession(session);
    }
  }

  private boolean isSessionNotFound(SpannerException e) {
    return e.getErrorCode() == ErrorCode.NOT_FOUND && e.getMessage().contains("Session not found");
  }

  private boolean isDatabaseOrInstanceNotFound(SpannerException e) {
    return e instanceof DatabaseNotFoundException || e instanceof InstanceNotFoundException;
  }

  private void invalidateSession(PooledSession session) {
    synchronized (lock) {
      if (isClosed()) {
        decrementPendingClosures(1);
        return;
      }
      allSessions.remove(session);
      // replenish the pool.
      createSessions(getAllowedCreateSessions(1), false);
    }
  }

  private Tuple<PooledSession, Integer> findSessionToKeepAlive(
      Queue<PooledSession> queue, Instant keepAliveThreshold, int numAlreadyChecked) {
    int numChecked = 0;
    Iterator<PooledSession> iterator = queue.iterator();
    while (iterator.hasNext()
        && (numChecked + numAlreadyChecked)
            < (options.getMinSessions() + options.getMaxIdleSessions() - numSessionsInUse)) {
      PooledSession session = iterator.next();
      if (session.delegate.getLastUseTime().isBefore(keepAliveThreshold)) {
        iterator.remove();
        return Tuple.of(session, numChecked);
      }
      numChecked++;
    }
    return null;
  }

  /** @return true if this {@link SessionPool} is still valid. */
  boolean isValid() {
    synchronized (lock) {
      return closureFuture == null && resourceNotFoundException == null;
    }
  }

  /**
   * Returns a multiplexed session. The method fallbacks to a regular session if {@link
   * SessionPoolOptions#getUseMultiplexedSession} is not set.
   */
  PooledSessionFutureWrapper getMultiplexedSessionWithFallback() throws SpannerException {
    return new PooledSessionFutureWrapper(getSession());
  }

  /**
   * Returns a session to be used for requests to spanner. This method is always non-blocking and
   * returns a {@link PooledSessionFuture}. In case the pool is exhausted and {@link
   * SessionPoolOptions#isFailIfPoolExhausted()} has been set, it will throw an exception. Returned
   * session must be closed by calling {@link Session#close()}.
   *
   * <p>Implementation strategy:
   *
   * <ol>
   *   <li>If a read session is available, return that.
   *   <li>Otherwise if a session can be created, fire a creation request.
   *   <li>Wait for a session to become available. Note that this can be unblocked either by a
   *       session being returned to the pool or a new session being created.
   * </ol>
   */
  PooledSessionFuture getSession() throws SpannerException {
    ISpan span = tracer.getCurrentSpan();
    span.addAnnotation("Acquiring session");
    WaiterFuture waiter = null;
    PooledSession sess = null;
    synchronized (lock) {
      if (closureFuture != null) {
        span.addAnnotation("Pool has been closed");
        throw new IllegalStateException("Pool has been closed", closedException);
      }
      if (resourceNotFoundException != null) {
        span.addAnnotation("Database has been deleted");
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.NOT_FOUND,
            String.format(
                "The session pool has been invalidated because a previous RPC returned 'Database not found': %s",
                resourceNotFoundException.getMessage()),
            resourceNotFoundException);
      }
      sess = sessions.poll();
      if (sess == null) {
        span.addAnnotation("No session available");
        maybeCreateSession();
        waiter = new WaiterFuture();
        waiters.add(waiter);
      } else {
        span.addAnnotation("Acquired session");
      }
      return checkoutSession(span, sess, waiter);
    }
  }

  private PooledSessionFuture checkoutSession(
      final ISpan span, final PooledSession readySession, WaiterFuture waiter) {
    ListenableFuture<PooledSession> sessionFuture;
    if (waiter != null) {
      logger.log(
          Level.FINE,
          "No session available in the pool. Blocking for one to become available/created");
      span.addAnnotation("Waiting for a session to come available");
      sessionFuture = waiter;
    } else {
      SettableFuture<PooledSession> fut = SettableFuture.create();
      fut.set(readySession);
      sessionFuture = fut;
    }
    PooledSessionFuture res = createPooledSessionFuture(sessionFuture, span);
    res.markCheckedOut();
    return res;
  }

  private void incrementNumSessionsInUse() {
    synchronized (lock) {
      if (maxSessionsInUse < ++numSessionsInUse) {
        maxSessionsInUse = numSessionsInUse;
      }
      numSessionsAcquired++;
    }
  }

  private void maybeCreateSession() {
    ISpan span = tracer.getCurrentSpan();
    boolean throwResourceExhaustedException = false;
    synchronized (lock) {
      if (numWaiters() >= numSessionsBeingCreated) {
        if (canCreateSession()) {
          span.addAnnotation("Creating sessions");
          createSessions(getAllowedCreateSessions(options.getIncStep()), false);
        } else if (options.isFailIfPoolExhausted()) {
          throwResourceExhaustedException = true;
        }
      }
    }
    if (!throwResourceExhaustedException) {
      return;
    }
    span.addAnnotation("Pool exhausted. Failing");

    String message =
        "No session available in the pool. Maximum number of sessions in the pool can be"
            + " overridden by invoking SessionPoolOptions#Builder#setMaxSessions. Client can be made to block"
            + " rather than fail by setting SessionPoolOptions#Builder#setBlockIfPoolExhausted.\n"
            + createCheckedOutSessionsStackTraces();
    throw newSpannerException(ErrorCode.RESOURCE_EXHAUSTED, message);
  }

  private StringBuilder createCheckedOutSessionsStackTraces() {
    List<PooledSessionFuture> currentlyCheckedOutSessions;
    synchronized (lock) {
      currentlyCheckedOutSessions = new ArrayList<>(this.markedCheckedOutSessions);
    }

    // Create the error message without holding the lock, as we are potentially looping through a
    // large set, and analyzing a large number of stack traces.
    StringBuilder stackTraces =
        new StringBuilder(
            "There are currently "
                + currentlyCheckedOutSessions.size()
                + " sessions checked out:\n\n");
    if (options.isTrackStackTraceOfSessionCheckout()) {
      for (PooledSessionFuture session : currentlyCheckedOutSessions) {
        if (session.leakedException != null) {
          StringWriter writer = new StringWriter();
          PrintWriter printWriter = new PrintWriter(writer);
          session.leakedException.printStackTrace(printWriter);
          stackTraces.append(writer).append("\n\n");
        }
      }
    }
    return stackTraces;
  }

  private void releaseSession(Tuple<PooledSession, Integer> sessionWithPosition) {
    releaseSession(sessionWithPosition.x(), false, sessionWithPosition.y());
  }

  private void releaseSession(PooledSession session, boolean isNewSession) {
    releaseSession(session, isNewSession, null);
  }

  /** Releases a session back to the pool. This might cause one of the waiters to be unblocked. */
  private void releaseSession(
      PooledSession session, boolean isNewSession, @Nullable Integer position) {
    Preconditions.checkNotNull(session);
    synchronized (lock) {
      if (closureFuture != null) {
        return;
      }
      if (waiters.isEmpty()) {
        // There are no pending waiters.
        // Add to a random position if the transactions per second is high or the head of the
        // session pool already contains many sessions with the same channel as this one.
        if (session.releaseToPosition != Position.RANDOM && shouldRandomize()) {
          session.releaseToPosition = Position.RANDOM;
        } else if (session.releaseToPosition == Position.FIRST && isUnbalanced(session)) {
          session.releaseToPosition = Position.RANDOM;
        } else if (session.releaseToPosition == Position.RANDOM
            && !isNewSession
            && checkedOutSessions.size() <= 2) {
          // Do not randomize if there are few other sessions checked out and this session has been
          // used. This ensures that this session will be re-used for the next transaction, which is
          // more efficient.
          session.releaseToPosition = options.getReleaseToPosition();
        }
        if (position != null) {
          // Make sure we use a valid position, as the number of sessions could have changed in the
          // meantime.
          int actualPosition = Math.min(position, sessions.size());
          sessions.add(actualPosition, session);
        } else if (session.releaseToPosition == Position.RANDOM && !sessions.isEmpty()) {
          // A session should only be added at a random position the first time it is added to
          // the pool or if the pool was deemed unbalanced. All following releases into the pool
          // should normally happen at the default release position (unless the pool is again deemed
          // to be unbalanced and the insertion would happen at the front of the pool).
          session.releaseToPosition = options.getReleaseToPosition();
          int pos = random.nextInt(sessions.size() + 1);
          sessions.add(pos, session);
        } else if (session.releaseToPosition == Position.LAST) {
          sessions.addLast(session);
        } else {
          sessions.addFirst(session);
        }
        session.releaseToPosition = options.getReleaseToPosition();
      } else {
        waiters.poll().put(session);
      }
    }
  }

  /**
   * Returns true if the position where we return the session should be random if:
   *
   * <ol>
   *   <li>The current TPS is higher than the configured threshold.
   *   <li>AND the number of sessions checked out is larger than the number of channels.
   * </ol>
   *
   * The second check prevents the session pool from being randomized when the application is
   * running many small, quick queries using a small number of parallel threads. This can cause a
   * high TPS, without actually having a high degree of parallelism.
   */
  @VisibleForTesting
  boolean shouldRandomize() {
    return this.options.getRandomizePositionQPSThreshold() > 0
        && this.transactionsPerSecond >= this.options.getRandomizePositionQPSThreshold()
        && this.numSessionsInUse >= this.numChannels;
  }

  private boolean isUnbalanced(PooledSession session) {
    int channel = session.getChannel();
    int numChannels = sessionClient.getSpanner().getOptions().getNumChannels();
    return isUnbalanced(channel, this.sessions, this.checkedOutSessions, numChannels);
  }

  /**
   * Returns true if the given list of sessions is considered unbalanced when compared to the
   * sessionChannel that is about to be added to the pool.
   *
   * <p>The method returns true if all the following is true:
   *
   * <ol>
   *   <li>The list of sessions is not empty.
   *   <li>The number of checked out sessions is > 2.
   *   <li>The number of channels being used by the pool is > 1.
   *   <li>And at least one of the following is true:
   *       <ol>
   *         <li>The first numChannels sessions in the list of sessions contains more than 2
   *             sessions that use the same channel as the one being added.
   *         <li>The list of currently checked out sessions contains more than 2 times the the
   *             number of sessions with the same channel as the one being added than it should in
   *             order for it to be perfectly balanced. Perfectly balanced in this case means that
   *             the list should preferably contain size/numChannels sessions of each channel.
   *       </ol>
   * </ol>
   *
   * @param channelOfSessionBeingAdded the channel number being used by the session that is about to
   *     be released into the pool
   * @param sessions the list of all sessions in the pool
   * @param checkedOutSessions the currently checked out sessions of the pool
   * @param numChannels the number of channels in use
   * @return true if the pool is considered unbalanced, and false otherwise
   */
  @VisibleForTesting
  static boolean isUnbalanced(
      int channelOfSessionBeingAdded,
      List<PooledSession> sessions,
      Set<PooledSessionFuture> checkedOutSessions,
      int numChannels) {
    // Do not re-balance the pool if the number of checked out sessions is low, as it is
    // better to re-use sessions as much as possible in a low-QPS scenario.
    if (sessions.isEmpty() || checkedOutSessions.size() <= 2) {
      return false;
    }
    if (numChannels == 1) {
      return false;
    }

    // Ideally, the first numChannels sessions in the pool should contain exactly one session for
    // each channel.
    // Check if the first numChannels sessions at the head of the pool already contain more than 2
    // sessions that use the same channel as this one. If so, we re-balance.
    // We also re-balance the pool in the specific case that the pool uses 2 channels and the first
    // two sessions use those two channels.
    int maxSessionsAtHeadOfPool = Math.min(numChannels, 3);
    int count = 0;
    for (int i = 0; i < Math.min(numChannels, sessions.size()); i++) {
      PooledSession otherSession = sessions.get(i);
      if (channelOfSessionBeingAdded == otherSession.getChannel()) {
        count++;
        if (count >= maxSessionsAtHeadOfPool) {
          return true;
        }
      }
    }
    // Ideally, the use of a channel in the checked out sessions is exactly
    // numCheckedOut / numChannels
    // We check whether we are more than a factor two away from that perfect distribution.
    // If we are, then we re-balance.
    count = 0;
    int checkedOutThreshold = Math.max(2, 2 * checkedOutSessions.size() / numChannels);
    for (PooledSessionFuture otherSession : checkedOutSessions) {
      if (otherSession.isDone() && channelOfSessionBeingAdded == otherSession.get().getChannel()) {
        count++;
        if (count > checkedOutThreshold) {
          return true;
        }
      }
    }
    return false;
  }

  private void handleCreateSessionsFailure(SpannerException e, int count) {
    synchronized (lock) {
      for (int i = 0; i < count; i++) {
        if (!waiters.isEmpty()) {
          waiters.poll().put(e);
        } else {
          break;
        }
      }
      if (!dialect.isDone()) {
        dialect.setException(e);
      }
      if (isDatabaseOrInstanceNotFound(e)) {
        setResourceNotFoundException((ResourceNotFoundException) e);
        poolMaintainer.close();
      }
    }
  }

  void setResourceNotFoundException(ResourceNotFoundException e) {
    this.resourceNotFoundException = MoreObjects.firstNonNull(this.resourceNotFoundException, e);
  }

  private void decrementPendingClosures(int count) {
    pendingClosure -= count;
    if (pendingClosure == 0) {
      closureFuture.set(null);
    }
  }

  /**
   * Close all the sessions. Once this method is invoked {@link #getSession()} will start throwing
   * {@code IllegalStateException}. The returned future blocks till all the sessions created in this
   * pool have been closed.
   */
  ListenableFuture<Void> closeAsync(ClosedException closedException) {
    ListenableFuture<Void> retFuture = null;
    synchronized (lock) {
      if (closureFuture != null) {
        throw new IllegalStateException("Close has already been invoked", this.closedException);
      }
      this.closedException = closedException;
      // Fail all pending waiters.
      WaiterFuture waiter = waiters.poll();
      while (waiter != null) {
        waiter.put(newSpannerException(ErrorCode.INTERNAL, "Client has been closed"));
        waiter = waiters.poll();
      }
      closureFuture = SettableFuture.create();
      retFuture = closureFuture;

      pendingClosure = totalSessions() + numSessionsBeingCreated;

      if (!poolMaintainer.isClosed()) {
        pendingClosure += 1; // For pool maintenance thread
        poolMaintainer.close();
      }

      sessions.clear();
      for (PooledSessionFuture session : checkedOutSessions) {
        if (session.leakedException != null) {
          if (options.isFailOnSessionLeak()) {
            throw session.leakedException;
          } else {
            logger.log(Level.WARNING, "Leaked session", session.leakedException);
          }
        } else {
          String message =
              "Leaked session. "
                  + "Call SessionOptions.Builder#setTrackStackTraceOfSessionCheckout(true) to start "
                  + "tracking the call stack trace of the thread that checked out the session.";
          if (options.isFailOnSessionLeak()) {
            throw new LeakedSessionException(message);
          } else {
            logger.log(Level.WARNING, message);
          }
        }
      }
      for (final PooledSession session : ImmutableList.copyOf(allSessions)) {
        if (session.state != SessionState.CLOSING) {
          closeSessionAsync(session);
        }
      }

      // Nothing to be closed, mark as complete
      if (pendingClosure == 0) {
        closureFuture.set(null);
      }
    }

    retFuture.addListener(() -> executorFactory.release(executor), MoreExecutors.directExecutor());
    return retFuture;
  }

  private int numWaiters() {
    synchronized (lock) {
      return waiters.size();
    }
  }

  @VisibleForTesting
  int totalSessions() {
    synchronized (lock) {
      return allSessions.size();
    }
  }

  private ApiFuture<Empty> closeSessionAsync(final PooledSession sess) {
    ApiFuture<Empty> res = sess.delegate.asyncClose();
    res.addListener(
        () -> {
          synchronized (lock) {
            allSessions.remove(sess);
            if (isClosed()) {
              decrementPendingClosures(1);
              return;
            }
            // Create a new session if needed to unblock some waiter.
            if (numWaiters() > numSessionsBeingCreated) {
              createSessions(
                  getAllowedCreateSessions(numWaiters() - numSessionsBeingCreated), false);
            }
          }
        },
        MoreExecutors.directExecutor());
    return res;
  }

  /**
   * Returns the minimum of the wanted number of sessions that the caller wants to create and the
   * actual max number that may be created at this moment.
   */
  private int getAllowedCreateSessions(int wantedSessions) {
    synchronized (lock) {
      return Math.min(
          wantedSessions, options.getMaxSessions() - (totalSessions() + numSessionsBeingCreated));
    }
  }

  private boolean canCreateSession() {
    synchronized (lock) {
      return totalSessions() + numSessionsBeingCreated < options.getMaxSessions();
    }
  }

  private void createSessions(final int sessionCount, boolean distributeOverChannels) {
    logger.log(Level.FINE, String.format("Creating %d sessions", sessionCount));
    synchronized (lock) {
      numSessionsBeingCreated += sessionCount;
      try {
        // Create a batch of sessions. The actual session creation can be split into multiple gRPC
        // calls and the session consumer consumes the returned sessions as they become available.
        // The batchCreateSessions method automatically spreads the sessions evenly over all
        // available channels.
        sessionClient.asyncBatchCreateSessions(
            sessionCount, distributeOverChannels, sessionConsumer);
      } catch (Throwable t) {
        // Expose this to customer via a metric.
        numSessionsBeingCreated -= sessionCount;
        if (isClosed()) {
          decrementPendingClosures(sessionCount);
        }
        handleCreateSessionsFailure(newSpannerException(t), sessionCount);
      }
    }
  }

  /**
   * {@link SessionConsumer} that receives the created sessions from a {@link SessionClient} and
   * releases these into the pool. The session pool only needs one instance of this, as all sessions
   * should be returned to the same pool regardless of what triggered the creation of the sessions.
   */
  class SessionConsumerImpl implements SessionConsumer {
    /** Release a new session to the pool. */
    @Override
    public void onSessionReady(SessionImpl session) {
      PooledSession pooledSession = null;
      boolean closeSession = false;
      synchronized (lock) {
        int minSessions = options.getMinSessions();
        pooledSession = new PooledSession(session);
        numSessionsBeingCreated--;
        if (closureFuture != null) {
          closeSession = true;
        } else {
          Preconditions.checkState(totalSessions() <= options.getMaxSessions() - 1);
          allSessions.add(pooledSession);
          if (allSessions.size() >= minSessions) {
            waitOnMinSessionsLatch.countDown();
          }
          if (options.isAutoDetectDialect() && !detectDialectStarted) {
            // Get the dialect of the underlying database if that has not yet been done. Note that
            // this method will release the session into the pool once it is done.
            detectDialectStarted = true;
            pooledSession.determineDialectAsync(SessionPool.this.dialect);
          } else {
            // Release the session to a random position in the pool to prevent the case that a batch
            // of sessions that are affiliated with the same channel are all placed sequentially in
            // the pool.
            releaseSession(pooledSession, true);
          }
        }
      }
      if (closeSession) {
        closeSessionAsync(pooledSession);
      }
    }

    /**
     * Informs waiters for a session that session creation failed. The exception will propagate to
     * the waiters as a {@link SpannerException}.
     */
    @Override
    public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
      synchronized (lock) {
        numSessionsBeingCreated -= createFailureForSessionCount;
        if (numSessionsBeingCreated == 0) {
          // Don't continue to block if no more sessions are being created.
          waitOnMinSessionsLatch.countDown();
        }
        if (isClosed()) {
          decrementPendingClosures(createFailureForSessionCount);
        }
        handleCreateSessionsFailure(newSpannerException(t), createFailureForSessionCount);
      }
    }
  }

  /**
   * Initializes and creates Spanner session relevant metrics using OpenCensus. When coupled with an
   * exporter, it allows users to monitor client behavior.
   */
  private void initOpenCensusMetricsCollection(
      MetricRegistry metricRegistry,
      List<LabelValue> labelValues,
      AtomicLong numMultiplexedSessionsAcquired,
      AtomicLong numMultiplexedSessionsReleased) {
    if (!SpannerOptions.isEnabledOpenCensusMetrics()) {
      return;
    }
    DerivedLongGauge maxInUseSessionsMetric =
        metricRegistry.addDerivedLongGauge(
            METRIC_PREFIX + MAX_IN_USE_SESSIONS,
            MetricOptions.builder()
                .setDescription(MAX_IN_USE_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongGauge maxAllowedSessionsMetric =
        metricRegistry.addDerivedLongGauge(
            METRIC_PREFIX + MAX_ALLOWED_SESSIONS,
            MetricOptions.builder()
                .setDescription(MAX_ALLOWED_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongCumulative sessionsTimeouts =
        metricRegistry.addDerivedLongCumulative(
            METRIC_PREFIX + GET_SESSION_TIMEOUTS,
            MetricOptions.builder()
                .setDescription(SESSIONS_TIMEOUTS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongCumulative numAcquiredSessionsMetric =
        metricRegistry.addDerivedLongCumulative(
            METRIC_PREFIX + NUM_ACQUIRED_SESSIONS,
            MetricOptions.builder()
                .setDescription(NUM_ACQUIRED_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS)
                .build());

    DerivedLongCumulative numReleasedSessionsMetric =
        metricRegistry.addDerivedLongCumulative(
            METRIC_PREFIX + NUM_RELEASED_SESSIONS,
            MetricOptions.builder()
                .setDescription(NUM_RELEASED_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS)
                .build());

    DerivedLongGauge numSessionsInPoolMetric =
        metricRegistry.addDerivedLongGauge(
            METRIC_PREFIX + NUM_SESSIONS_IN_POOL,
            MetricOptions.builder()
                .setDescription(NUM_SESSIONS_IN_POOL_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS_WITH_TYPE)
                .build());

    // The value of a maxSessionsInUse is observed from a callback function. This function is
    // invoked whenever metrics are collected.
    maxInUseSessionsMetric.removeTimeSeries(labelValues);
    maxInUseSessionsMetric.createTimeSeries(
        labelValues, this, sessionPool -> sessionPool.maxSessionsInUse);

    // The value of a maxSessions is observed from a callback function. This function is invoked
    // whenever metrics are collected.
    maxAllowedSessionsMetric.removeTimeSeries(labelValues);
    maxAllowedSessionsMetric.createTimeSeries(
        labelValues, options, SessionPoolOptions::getMaxSessions);

    // The value of a numWaiterTimeouts is observed from a callback function. This function is
    // invoked whenever metrics are collected.
    sessionsTimeouts.removeTimeSeries(labelValues);
    sessionsTimeouts.createTimeSeries(labelValues, this, SessionPool::getNumWaiterTimeouts);

    List<LabelValue> labelValuesWithRegularSessions = new ArrayList<>(labelValues);
    List<LabelValue> labelValuesWithMultiplexedSessions = new ArrayList<>(labelValues);
    labelValuesWithMultiplexedSessions.add(LabelValue.create("true"));
    labelValuesWithRegularSessions.add(LabelValue.create("false"));

    numAcquiredSessionsMetric.removeTimeSeries(labelValuesWithRegularSessions);
    numAcquiredSessionsMetric.createTimeSeries(
        labelValuesWithRegularSessions, this, sessionPool -> sessionPool.numSessionsAcquired);
    numAcquiredSessionsMetric.removeTimeSeries(labelValuesWithMultiplexedSessions);
    numAcquiredSessionsMetric.createTimeSeries(
        labelValuesWithMultiplexedSessions, this, unused -> numMultiplexedSessionsAcquired.get());

    numReleasedSessionsMetric.removeTimeSeries(labelValuesWithRegularSessions);
    numReleasedSessionsMetric.createTimeSeries(
        labelValuesWithRegularSessions, this, sessionPool -> sessionPool.numSessionsReleased);
    numReleasedSessionsMetric.removeTimeSeries(labelValuesWithMultiplexedSessions);
    numReleasedSessionsMetric.createTimeSeries(
        labelValuesWithMultiplexedSessions, this, unused -> numMultiplexedSessionsReleased.get());

    List<LabelValue> labelValuesWithBeingPreparedType = new ArrayList<>(labelValues);
    labelValuesWithBeingPreparedType.add(NUM_SESSIONS_BEING_PREPARED);
    numSessionsInPoolMetric.removeTimeSeries(labelValuesWithBeingPreparedType);
    numSessionsInPoolMetric.createTimeSeries(
        labelValuesWithBeingPreparedType,
        this,
        // TODO: Remove metric.
        ignored -> 0L);

    List<LabelValue> labelValuesWithInUseType = new ArrayList<>(labelValues);
    labelValuesWithInUseType.add(NUM_IN_USE_SESSIONS);
    numSessionsInPoolMetric.removeTimeSeries(labelValuesWithInUseType);
    numSessionsInPoolMetric.createTimeSeries(
        labelValuesWithInUseType, this, sessionPool -> sessionPool.numSessionsInUse);

    List<LabelValue> labelValuesWithReadType = new ArrayList<>(labelValues);
    labelValuesWithReadType.add(NUM_READ_SESSIONS);
    numSessionsInPoolMetric.removeTimeSeries(labelValuesWithReadType);
    numSessionsInPoolMetric.createTimeSeries(
        labelValuesWithReadType, this, sessionPool -> sessionPool.sessions.size());

    List<LabelValue> labelValuesWithWriteType = new ArrayList<>(labelValues);
    labelValuesWithWriteType.add(NUM_WRITE_SESSIONS);
    numSessionsInPoolMetric.removeTimeSeries(labelValuesWithWriteType);
    numSessionsInPoolMetric.createTimeSeries(
        labelValuesWithWriteType,
        this,
        // TODO: Remove metric.
        ignored -> 0L);
  }

  /**
   * Initializes and creates Spanner session relevant metrics using OpenTelemetry. When coupled with
   * an exporter, it allows users to monitor client behavior.
   */
  private void initOpenTelemetryMetricsCollection(
      OpenTelemetry openTelemetry,
      Attributes attributes,
      AtomicLong numMultiplexedSessionsAcquired,
      AtomicLong numMultiplexedSessionsReleased) {
    if (openTelemetry == null || !SpannerOptions.isEnabledOpenTelemetryMetrics()) {
      return;
    }

    Meter meter = openTelemetry.getMeter(MetricRegistryConstants.INSTRUMENTATION_SCOPE);
    meter
        .gaugeBuilder(MAX_ALLOWED_SESSIONS)
        .setDescription(MAX_ALLOWED_SESSIONS_DESCRIPTION)
        .setUnit(COUNT)
        .buildWithCallback(
            measurement -> {
              // Although Max sessions is a constant value, OpenTelemetry requires to define this as
              // a callback.
              measurement.record(options.getMaxSessions(), attributes);
            });

    meter
        .gaugeBuilder(MAX_IN_USE_SESSIONS)
        .setDescription(MAX_IN_USE_SESSIONS_DESCRIPTION)
        .setUnit(COUNT)
        .buildWithCallback(
            measurement -> {
              measurement.record(this.maxSessionsInUse, attributes);
            });

    AttributesBuilder attributesBuilder;
    if (attributes != null) {
      attributesBuilder = attributes.toBuilder();
    } else {
      attributesBuilder = Attributes.builder();
    }
    Attributes attributesInUseSessions =
        attributesBuilder.put(SESSIONS_TYPE, NUM_SESSIONS_IN_USE).build();
    Attributes attributesAvailableSessions =
        attributesBuilder.put(SESSIONS_TYPE, NUM_SESSIONS_AVAILABLE).build();
    meter
        .upDownCounterBuilder(NUM_SESSIONS_IN_POOL)
        .setDescription(NUM_SESSIONS_IN_POOL_DESCRIPTION)
        .setUnit(COUNT)
        .buildWithCallback(
            measurement -> {
              measurement.record(this.numSessionsInUse, attributesInUseSessions);
              measurement.record(this.sessions.size(), attributesAvailableSessions);
            });

    AttributesBuilder attributesBuilderIsMultiplexed;
    if (attributes != null) {
      attributesBuilderIsMultiplexed = attributes.toBuilder();
    } else {
      attributesBuilderIsMultiplexed = Attributes.builder();
    }
    Attributes attributesRegularSession =
        attributesBuilderIsMultiplexed.put(IS_MULTIPLEXED, false).build();
    Attributes attributesMultiplexedSession =
        attributesBuilderIsMultiplexed.put(IS_MULTIPLEXED, true).build();
    meter
        .counterBuilder(GET_SESSION_TIMEOUTS)
        .setDescription(SESSIONS_TIMEOUTS_DESCRIPTION)
        .setUnit(COUNT)
        .buildWithCallback(
            measurement -> {
              measurement.record(this.getNumWaiterTimeouts(), attributes);
            });

    meter
        .counterBuilder(NUM_ACQUIRED_SESSIONS)
        .setDescription(NUM_ACQUIRED_SESSIONS_DESCRIPTION)
        .setUnit(COUNT)
        .buildWithCallback(
            measurement -> {
              measurement.record(this.numSessionsAcquired, attributesRegularSession);
              measurement.record(
                  numMultiplexedSessionsAcquired.get(), attributesMultiplexedSession);
            });

    meter
        .counterBuilder(NUM_RELEASED_SESSIONS)
        .setDescription(NUM_RELEASED_SESSIONS_DESCRIPTION)
        .setUnit(COUNT)
        .buildWithCallback(
            measurement -> {
              measurement.record(this.numSessionsReleased, attributesRegularSession);
              measurement.record(
                  numMultiplexedSessionsReleased.get(), attributesMultiplexedSession);
            });
  }
}
