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
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_ALLOWED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_ALLOWED_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_IN_USE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_IN_USE_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_ACQUIRED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_ACQUIRED_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_IN_USE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_READ_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_RELEASED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_RELEASED_SESSIONS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_BEING_PREPARED;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_IN_POOL;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_IN_POOL_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_WRITE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.SESSIONS_TIMEOUTS_DESCRIPTION;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_DEFAULT_LABEL_VALUES;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS_WITH_TYPE;
import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerException;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.core.ExecutorProvider;
import com.google.cloud.Timestamp;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SpannerException.ResourceNotFoundException;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Empty;
import io.opencensus.common.Scope;
import io.opencensus.metrics.DerivedLongCumulative;
import io.opencensus.metrics.DerivedLongGauge;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.MetricOptions;
import io.opencensus.metrics.MetricRegistry;
import io.opencensus.metrics.Metrics;
import io.opencensus.trace.Annotation;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.BlankSpan;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
  private static final Tracer tracer = Tracing.getTracer();
  static final String WAIT_FOR_SESSION = "SessionPool.WaitForSession";
  static final ImmutableSet<ErrorCode> SHOULD_STOP_PREPARE_SESSIONS_ERROR_CODES =
      ImmutableSet.of(
          ErrorCode.UNKNOWN,
          ErrorCode.INVALID_ARGUMENT,
          ErrorCode.PERMISSION_DENIED,
          ErrorCode.UNAUTHENTICATED,
          ErrorCode.RESOURCE_EXHAUSTED,
          ErrorCode.FAILED_PRECONDITION,
          ErrorCode.OUT_OF_RANGE,
          ErrorCode.UNIMPLEMENTED,
          ErrorCode.INTERNAL);

  /**
   * Wrapper around current time so that we can fake it in tests. TODO(user): Replace with Java 8
   * Clock.
   */
  static class Clock {
    Instant instant() {
      return Instant.now();
    }
  }

  private abstract static class CachedResultSetSupplier implements Supplier<ResultSet> {
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
  private static class AutoClosingReadContext<T extends ReadContext> implements ReadContext {
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

    private final Function<PooledSessionFuture, T> readContextDelegateSupplier;
    private T readContextDelegate;
    private final SessionPool sessionPool;
    private final boolean isSingleUse;
    private final AtomicInteger asyncOperationsCount = new AtomicInteger();

    private final Object lock = new Object();

    @GuardedBy("lock")
    private boolean sessionUsedForQuery = false;

    @GuardedBy("lock")
    private PooledSessionFuture session;

    @GuardedBy("lock")
    private boolean closed;

    @GuardedBy("lock")
    private boolean delegateClosed;

    private AutoClosingReadContext(
        Function<PooledSessionFuture, T> delegateSupplier,
        SessionPool sessionPool,
        PooledSessionFuture session,
        boolean isSingleUse) {
      this.readContextDelegateSupplier = delegateSupplier;
      this.sessionPool = sessionPool;
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
                session.get().lastException = e;
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
          session = sessionPool.replaceSession(notFound, session);
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

  private static class AutoClosingReadTransaction
      extends AutoClosingReadContext<ReadOnlyTransaction> implements ReadOnlyTransaction {

    AutoClosingReadTransaction(
        Function<PooledSessionFuture, ReadOnlyTransaction> txnSupplier,
        SessionPool sessionPool,
        PooledSessionFuture session,
        boolean isSingleUse) {
      super(txnSupplier, sessionPool, session, isSingleUse);
    }

    @Override
    public Timestamp getReadTimestamp() {
      return getReadContextDelegate().getReadTimestamp();
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

  private static class AutoClosingTransactionManager
      implements TransactionManager, SessionNotFoundHandler {
    private TransactionManager delegate;
    private final SessionPool sessionPool;
    private PooledSessionFuture session;
    private final TransactionOption[] options;
    private boolean closed;
    private boolean restartedAfterSessionNotFound;

    AutoClosingTransactionManager(
        SessionPool sessionPool, PooledSessionFuture session, TransactionOption... options) {
      this.sessionPool = sessionPool;
      this.session = session;
      this.options = options;
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
      session = sessionPool.replaceSession(notFoundException, session);
      PooledSession pooledSession = session.get();
      delegate = pooledSession.delegate.transactionManager(options);
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
          session = sessionPool.replaceSession(e, session);
          PooledSession pooledSession = session.get();
          delegate = pooledSession.delegate.transactionManager(options);
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
  private static final class SessionPoolTransactionRunner implements TransactionRunner {
    private final SessionPool sessionPool;
    private PooledSessionFuture session;
    private final TransactionOption[] options;
    private TransactionRunner runner;

    private SessionPoolTransactionRunner(
        SessionPool sessionPool, PooledSessionFuture session, TransactionOption... options) {
      this.sessionPool = sessionPool;
      this.session = session;
      this.options = options;
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
            session = sessionPool.replaceSession(e, session);
            PooledSession ps = session.get();
            runner = ps.delegate.readWriteTransaction();
          }
        }
        session.get().markUsed();
        return result;
      } catch (SpannerException e) {
        throw session.get().lastException = e;
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

  private static class SessionPoolAsyncRunner implements AsyncRunner {
    private final SessionPool sessionPool;
    private volatile PooledSessionFuture session;
    private final TransactionOption[] options;
    private SettableApiFuture<CommitResponse> commitResponse;

    private SessionPoolAsyncRunner(
        SessionPool sessionPool, PooledSessionFuture session, TransactionOption... options) {
      this.sessionPool = sessionPool;
      this.session = session;
      this.options = options;
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
                se = SpannerExceptionFactory.asSpannerException(e.getCause());
              } catch (InterruptedException e) {
                se = SpannerExceptionFactory.propagateInterrupt(e);
              } catch (Throwable t) {
                se = SpannerExceptionFactory.newSpannerException(t);
              } finally {
                if (se instanceof SessionNotFoundException) {
                  try {
                    // The replaceSession method will re-throw the SessionNotFoundException if the
                    // session cannot be replaced with a new one.
                    session = sessionPool.replaceSession((SessionNotFoundException) se, session);
                    se = null;
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
  }

  private enum SessionState {
    AVAILABLE,
    BUSY,
    CLOSING,
  }

  private PooledSessionFuture createPooledSessionFuture(
      ListenableFuture<PooledSession> future, Span span) {
    return new PooledSessionFuture(future, span);
  }

  class PooledSessionFuture extends SimpleForwardingListenableFuture<PooledSession>
      implements Session {
    private volatile LeakedSessionException leakedException;
    private volatile AtomicBoolean inUse = new AtomicBoolean();
    private volatile CountDownLatch initialized = new CountDownLatch(1);
    private final Span span;

    @VisibleForTesting
    PooledSessionFuture(ListenableFuture<PooledSession> delegate, Span span) {
      super(delegate);
      this.span = span;
    }

    @VisibleForTesting
    void clearLeakedException() {
      this.leakedException = null;
    }

    private void markCheckedOut() {
      this.leakedException = new LeakedSessionException();
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
    public ReadContext singleUse() {
      try {
        return new AutoClosingReadContext<>(
            session -> {
              PooledSession ps = session.get();
              return ps.delegate.singleUse();
            },
            SessionPool.this,
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
        return new AutoClosingReadTransaction(
            transactionSupplier, SessionPool.this, this, isSingleUse);
      } catch (Exception e) {
        close();
        throw e;
      }
    }

    @Override
    public TransactionRunner readWriteTransaction(TransactionOption... options) {
      return new SessionPoolTransactionRunner(SessionPool.this, this, options);
    }

    @Override
    public TransactionManager transactionManager(TransactionOption... options) {
      return new AutoClosingTransactionManager(SessionPool.this, this, options);
    }

    @Override
    public AsyncRunner runAsync(TransactionOption... options) {
      return new SessionPoolAsyncRunner(SessionPool.this, this, options);
    }

    @Override
    public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
      return new SessionPoolAsyncTransactionManager(SessionPool.this, this, options);
    }

    @Override
    public long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
      try {
        return get().executePartitionedUpdate(stmt, options);
      } finally {
        close();
      }
    }

    @Override
    public String getName() {
      return get().getName();
    }

    @Override
    public void prepareReadWriteTransaction() {
      get().prepareReadWriteTransaction();
    }

    @Override
    public void close() {
      synchronized (lock) {
        leakedException = null;
        checkedOutSessions.remove(this);
      }
      PooledSession delegate = getOrNull();
      if (delegate != null) {
        delegate.close();
      }
    }

    @Override
    public ApiFuture<Empty> asyncClose() {
      synchronized (lock) {
        leakedException = null;
        checkedOutSessions.remove(this);
      }
      PooledSession delegate = getOrNull();
      if (delegate != null) {
        return delegate.asyncClose();
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
      if (inUse.compareAndSet(false, true)) {
        PooledSession res = null;
        try {
          res = super.get();
        } catch (Throwable e) {
          // ignore the exception as it will be handled by the call to super.get() below.
        }
        if (res != null) {
          res.markBusy(span);
          span.addAnnotation(sessionAnnotation(res));
          synchronized (lock) {
            incrementNumSessionsInUse();
            checkedOutSessions.add(this);
          }
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

  final class PooledSession implements Session {
    @VisibleForTesting SessionImpl delegate;
    private volatile Instant lastUseTime;
    private volatile SpannerException lastException;
    private volatile boolean allowReplacing = true;

    @GuardedBy("lock")
    private SessionState state;

    private PooledSession(SessionImpl delegate) {
      this.delegate = delegate;
      this.state = SessionState.AVAILABLE;
      this.lastUseTime = clock.instant();
    }

    @Override
    public String toString() {
      return getName();
    }

    @VisibleForTesting
    void setAllowReplacing(boolean allowReplacing) {
      this.allowReplacing = allowReplacing;
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
      if (lastException != null && isSessionNotFound(lastException)) {
        invalidateSession(this);
      } else {
        if (lastException != null && isDatabaseOrInstanceNotFound(lastException)) {
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
        if (state != SessionState.CLOSING) {
          state = SessionState.AVAILABLE;
        }
        releaseSession(this, Position.FIRST);
      }
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public void prepareReadWriteTransaction() {
      markUsed();
      delegate.prepareReadWriteTransaction();
    }

    private void keepAlive() {
      markUsed();
      final Span previousSpan = delegate.getCurrentSpan();
      delegate.setCurrentSpan(BlankSpan.INSTANCE);
      try (ResultSet resultSet =
          delegate
              .singleUse(TimestampBound.ofMaxStaleness(60, TimeUnit.SECONDS))
              .executeQuery(Statement.newBuilder("SELECT 1").build())) {
        resultSet.next();
      } finally {
        delegate.setCurrentSpan(previousSpan);
      }
    }

    private void markBusy(Span span) {
      this.delegate.setCurrentSpan(span);
      this.state = SessionState.BUSY;
    }

    private void markClosing() {
      this.state = SessionState.CLOSING;
    }

    void markUsed() {
      lastUseTime = clock.instant();
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
        Span span = tracer.spanBuilder(WAIT_FOR_SESSION).startSpan();
        try (Scope waitScope = tracer.withSpan(span)) {
          PooledSession s = pollUninterruptiblyWithTimeout(currentTimeout);
          if (s == null) {
            // Set the status to DEADLINE_EXCEEDED and retry.
            numWaiterTimeouts.incrementAndGet();
            tracer.getCurrentSpan().setStatus(Status.DEADLINE_EXCEEDED);
            currentTimeout = Math.min(currentTimeout * 2, MAX_SESSION_WAIT_TIMEOUT);
          } else {
            return s;
          }
        } catch (Exception e) {
          TraceUtil.setWithFailure(span, e);
          throw e;
        } finally {
          span.end(TraceUtil.END_SPAN_OPTIONS);
        }
      }
    }

    private PooledSession pollUninterruptiblyWithTimeout(long timeoutMillis) {
      boolean interrupted = false;
      try {
        while (true) {
          try {
            return waiter.get(timeoutMillis, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            interrupted = true;
          } catch (TimeoutException e) {
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
   * </ul>
   */
  final class PoolMaintainer {
    // Length of the window in millis over which we keep track of maximum number of concurrent
    // sessions in use.
    private final Duration windowLength = Duration.ofMillis(TimeUnit.MINUTES.toMillis(10));
    // Frequency of the timer loop.
    @VisibleForTesting final long loopFrequency = options.getLoopFrequency();
    // Number of loop iterations in which we need to to close all the sessions waiting for closure.
    @VisibleForTesting final long numClosureCycles = windowLength.toMillis() / loopFrequency;
    private final Duration keepAliveMillis =
        Duration.ofMillis(TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()));
    // Number of loop iterations in which we need to keep alive all the sessions
    @VisibleForTesting final long numKeepAliveCycles = keepAliveMillis.toMillis() / loopFrequency;

    Instant lastResetTime = Instant.ofEpochMilli(0);
    int numSessionsToClose = 0;
    int sessionsToClosePerLoop = 0;
    boolean closed = false;

    @GuardedBy("lock")
    ScheduledFuture<?> scheduledFuture;

    @GuardedBy("lock")
    boolean running;

    void init() {
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
          if (session.lastUseTime.isBefore(minLastUseTime)) {
            if (session.state != SessionState.CLOSING) {
              removeFromPool(session);
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
        PooledSession sessionToKeepAlive = null;
        synchronized (lock) {
          sessionToKeepAlive = findSessionToKeepAlive(sessions, keepAliveThreshold, 0);
        }
        if (sessionToKeepAlive == null) {
          break;
        }
        try {
          logger.log(Level.FINE, "Keeping alive session " + sessionToKeepAlive.getName());
          numSessionsToKeepAlive--;
          sessionToKeepAlive.keepAlive();
          releaseSession(sessionToKeepAlive, Position.FIRST);
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
  }

  private enum Position {
    FIRST,
    RANDOM
  }

  private final SessionPoolOptions options;
  private final SessionClient sessionClient;
  private final ScheduledExecutorService executor;
  private final ExecutorFactory<ScheduledExecutorService> executorFactory;

  final PoolMaintainer poolMaintainer;
  private final Clock clock;
  private final Object lock = new Object();
  private final Random random = new Random();

  @GuardedBy("lock")
  private int pendingClosure;

  @GuardedBy("lock")
  private SettableFuture<Void> closureFuture;

  @GuardedBy("lock")
  private ClosedException closedException;

  @GuardedBy("lock")
  private ResourceNotFoundException resourceNotFoundException;

  @GuardedBy("lock")
  private boolean stopAutomaticPrepare;

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

  private AtomicLong numWaiterTimeouts = new AtomicLong();

  @GuardedBy("lock")
  private final Set<PooledSession> allSessions = new HashSet<>();

  @GuardedBy("lock")
  private final Set<PooledSessionFuture> checkedOutSessions = new HashSet<>();

  private final SessionConsumer sessionConsumer = new SessionConsumerImpl();

  @VisibleForTesting Function<PooledSession, Void> idleSessionRemovedListener;

  /**
   * Create a session pool with the given options and for the given database. It will also start
   * eagerly creating sessions if {@link SessionPoolOptions#getMinSessions()} is greater than 0.
   * Return pool is immediately ready for use, though getting a session might block for sessions to
   * be created.
   */
  static SessionPool createPool(
      SpannerOptions spannerOptions, SessionClient sessionClient, List<LabelValue> labelValues) {
    return createPool(
        spannerOptions.getSessionPoolOptions(),
        ((GrpcTransportOptions) spannerOptions.getTransportOptions()).getExecutorFactory(),
        sessionClient,
        new Clock(),
        Metrics.getMetricRegistry(),
        labelValues);
  }

  static SessionPool createPool(
      SessionPoolOptions poolOptions,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      SessionClient sessionClient) {
    return createPool(poolOptions, executorFactory, sessionClient, new Clock());
  }

  static SessionPool createPool(
      SessionPoolOptions poolOptions,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      SessionClient sessionClient,
      Clock clock) {
    return createPool(
        poolOptions,
        executorFactory,
        sessionClient,
        clock,
        Metrics.getMetricRegistry(),
        SPANNER_DEFAULT_LABEL_VALUES);
  }

  static SessionPool createPool(
      SessionPoolOptions poolOptions,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      SessionClient sessionClient,
      Clock clock,
      MetricRegistry metricRegistry,
      List<LabelValue> labelValues) {
    SessionPool pool =
        new SessionPool(
            poolOptions,
            executorFactory,
            executorFactory.get(),
            sessionClient,
            clock,
            metricRegistry,
            labelValues);
    pool.initPool();
    return pool;
  }

  private SessionPool(
      SessionPoolOptions options,
      ExecutorFactory<ScheduledExecutorService> executorFactory,
      ScheduledExecutorService executor,
      SessionClient sessionClient,
      Clock clock,
      MetricRegistry metricRegistry,
      List<LabelValue> labelValues) {
    this.options = options;
    this.executorFactory = executorFactory;
    this.executor = executor;
    this.sessionClient = sessionClient;
    this.clock = clock;
    this.poolMaintainer = new PoolMaintainer();
    this.initMetricsCollection(metricRegistry, labelValues);
  }

  @VisibleForTesting
  int getNumberOfSessionsInUse() {
    synchronized (lock) {
      return numSessionsInUse;
    }
  }

  void removeFromPool(PooledSession session) {
    synchronized (lock) {
      if (isClosed()) {
        decrementPendingClosures(1);
        return;
      }
      session.markClosing();
      allSessions.remove(session);
      numIdleSessionsRemoved++;
    }
    if (idleSessionRemovedListener != null) {
      idleSessionRemovedListener.apply(session);
    }
  }

  long numIdleSessionsRemoved() {
    synchronized (lock) {
      return numIdleSessionsRemoved;
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

  private void handleException(SpannerException e, PooledSession session) {
    if (isSessionNotFound(e)) {
      invalidateSession(session);
    } else {
      releaseSession(session, Position.FIRST);
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

  private PooledSession findSessionToKeepAlive(
      Queue<PooledSession> queue, Instant keepAliveThreshold, int numAlreadyChecked) {
    int numChecked = 0;
    Iterator<PooledSession> iterator = queue.iterator();
    while (iterator.hasNext()
        && (numChecked + numAlreadyChecked)
            < (options.getMinSessions() + options.getMaxIdleSessions() - numSessionsInUse)) {
      PooledSession session = iterator.next();
      if (session.lastUseTime.isBefore(keepAliveThreshold)) {
        iterator.remove();
        return session;
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
    Span span = Tracing.getTracer().getCurrentSpan();
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
      final Span span, final PooledSession readySession, WaiterFuture waiter) {
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

  PooledSessionFuture replaceSession(SessionNotFoundException e, PooledSessionFuture session) {
    if (!options.isFailIfSessionNotFound() && session.get().allowReplacing) {
      synchronized (lock) {
        numSessionsInUse--;
        numSessionsReleased++;
        checkedOutSessions.remove(session);
      }
      session.leakedException = null;
      invalidateSession(session.get());
      return getSession();
    } else {
      throw e;
    }
  }

  private Annotation sessionAnnotation(Session session) {
    AttributeValue sessionId = AttributeValue.stringAttributeValue(session.getName());
    return Annotation.fromDescriptionAndAttributes(
        "Using Session", ImmutableMap.of("sessionId", sessionId));
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
    Span span = Tracing.getTracer().getCurrentSpan();
    synchronized (lock) {
      if (numWaiters() >= numSessionsBeingCreated) {
        if (canCreateSession()) {
          span.addAnnotation("Creating sessions");
          createSessions(getAllowedCreateSessions(options.getIncStep()), false);
        } else if (options.isFailIfPoolExhausted()) {
          span.addAnnotation("Pool exhausted. Failing");
          // throw specific exception
          throw newSpannerException(
              ErrorCode.RESOURCE_EXHAUSTED,
              "No session available in the pool. Maximum number of sessions in the pool can be"
                  + " overridden by invoking SessionPoolOptions#Builder#setMaxSessions. Client can be made to block"
                  + " rather than fail by setting SessionPoolOptions#Builder#setBlockIfPoolExhausted.");
        }
      }
    }
  }
  /** Releases a session back to the pool. This might cause one of the waiters to be unblocked. */
  private void releaseSession(PooledSession session, Position position) {
    Preconditions.checkNotNull(session);
    synchronized (lock) {
      if (closureFuture != null) {
        return;
      }
      if (waiters.size() == 0) {
        // No pending waiters
        switch (position) {
          case RANDOM:
            if (!sessions.isEmpty()) {
              int pos = random.nextInt(sessions.size() + 1);
              sessions.add(pos, session);
              break;
            }
            // fallthrough
          case FIRST:
          default:
            sessions.addFirst(session);
        }
      } else {
        waiters.poll().put(session);
      }
    }
  }

  private void handleCreateSessionsFailure(SpannerException e, int count) {
    synchronized (lock) {
      for (int i = 0; i < count; i++) {
        if (waiters.size() > 0) {
          waiters.poll().put(e);
        } else {
          break;
        }
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
        pooledSession = new PooledSession(session);
        numSessionsBeingCreated--;
        if (closureFuture != null) {
          closeSession = true;
        } else {
          Preconditions.checkState(totalSessions() <= options.getMaxSessions() - 1);
          allSessions.add(pooledSession);
          // Release the session to a random position in the pool to prevent the case that a batch
          // of sessions that are affiliated with the same channel are all placed sequentially in
          // the pool.
          releaseSession(pooledSession, Position.RANDOM);
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
        if (isClosed()) {
          decrementPendingClosures(createFailureForSessionCount);
        }
        handleCreateSessionsFailure(newSpannerException(t), createFailureForSessionCount);
      }
    }
  }

  /**
   * Initializes and creates Spanner session relevant metrics. When coupled with an exporter, it
   * allows users to monitor client behavior.
   */
  private void initMetricsCollection(MetricRegistry metricRegistry, List<LabelValue> labelValues) {
    DerivedLongGauge maxInUseSessionsMetric =
        metricRegistry.addDerivedLongGauge(
            MAX_IN_USE_SESSIONS,
            MetricOptions.builder()
                .setDescription(MAX_IN_USE_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongGauge maxAllowedSessionsMetric =
        metricRegistry.addDerivedLongGauge(
            MAX_ALLOWED_SESSIONS,
            MetricOptions.builder()
                .setDescription(MAX_ALLOWED_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongCumulative sessionsTimeouts =
        metricRegistry.addDerivedLongCumulative(
            GET_SESSION_TIMEOUTS,
            MetricOptions.builder()
                .setDescription(SESSIONS_TIMEOUTS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongCumulative numAcquiredSessionsMetric =
        metricRegistry.addDerivedLongCumulative(
            NUM_ACQUIRED_SESSIONS,
            MetricOptions.builder()
                .setDescription(NUM_ACQUIRED_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongCumulative numReleasedSessionsMetric =
        metricRegistry.addDerivedLongCumulative(
            NUM_RELEASED_SESSIONS,
            MetricOptions.builder()
                .setDescription(NUM_RELEASED_SESSIONS_DESCRIPTION)
                .setUnit(COUNT)
                .setLabelKeys(SPANNER_LABEL_KEYS)
                .build());

    DerivedLongGauge numSessionsInPoolMetric =
        metricRegistry.addDerivedLongGauge(
            NUM_SESSIONS_IN_POOL,
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

    numAcquiredSessionsMetric.removeTimeSeries(labelValues);
    numAcquiredSessionsMetric.createTimeSeries(
        labelValues, this, sessionPool -> sessionPool.numSessionsAcquired);

    numReleasedSessionsMetric.removeTimeSeries(labelValues);
    numReleasedSessionsMetric.createTimeSeries(
        labelValues, this, sessionPool -> sessionPool.numSessionsReleased);

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
}
