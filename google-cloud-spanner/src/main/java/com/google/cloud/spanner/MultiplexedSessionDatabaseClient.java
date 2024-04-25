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

package com.google.cloud.spanner;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * {@link DatabaseClient} implementation that uses a single multiplexed session to execute
 * transactions.
 */
class MultiplexedSessionDatabaseClient extends AbstractMultiplexedSessionDatabaseClient {
  /**
   * Represents a {@link ReadContext} using a multiplexed session that is not yet ready. The
   * execution will be delayed until the multiplexed session has been created and is ready.
   */
  static class DelayedReadOnlyTransaction extends DelayedReadContext<ReadOnlyTransaction>
      implements ReadOnlyTransaction {
    DelayedReadOnlyTransaction(ApiFuture<ReadOnlyTransaction> readContextFuture) {
      super(readContextFuture);
    }

    @Override
    public Timestamp getReadTimestamp() {
      return getReadContext().getReadTimestamp();
    }
  }

  /**
   * Represents a {@link ReadContext} using a multiplexed session that is not yet ready. The
   * execution will be delayed until the multiplexed session has been created and is ready.
   */
  static class DelayedReadContext<T extends ReadContext> implements ReadContext {
    private final ApiFuture<T> readContextFuture;

    DelayedReadContext(ApiFuture<T> readContextFuture) {
      this.readContextFuture = readContextFuture;
    }

    T getReadContext() {
      try {
        return this.readContextFuture.get();
      } catch (ExecutionException executionException) {
        throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
      } catch (InterruptedException interruptedException) {
        throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
      }
    }

    @Override
    public ResultSet read(
        String table, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new ForwardingResultSet(
          Suppliers.memoize(() -> getReadContext().read(table, keys, columns, options)));
    }

    @Override
    public AsyncResultSet readAsync(
        String table, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new ForwardingAsyncResultSet(
          Suppliers.memoize(() -> getReadContext().readAsync(table, keys, columns, options)));
    }

    @Override
    public ResultSet readUsingIndex(
        String table, String index, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new ForwardingResultSet(
          Suppliers.memoize(
              () -> getReadContext().readUsingIndex(table, index, keys, columns, options)));
    }

    @Override
    public AsyncResultSet readUsingIndexAsync(
        String table, String index, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return new ForwardingAsyncResultSet(
          Suppliers.memoize(
              () -> getReadContext().readUsingIndexAsync(table, index, keys, columns, options)));
    }

    @Nullable
    @Override
    public Struct readRow(String table, Key key, Iterable<String> columns) {
      // This is allowed to be blocking.
      return getReadContext().readRow(table, key, columns);
    }

    @Override
    public ApiFuture<Struct> readRowAsync(String table, Key key, Iterable<String> columns) {
      return ApiFutures.transformAsync(
          this.readContextFuture,
          readContext -> readContext.readRowAsync(table, key, columns),
          MoreExecutors.directExecutor());
    }

    @Nullable
    @Override
    public Struct readRowUsingIndex(String table, String index, Key key, Iterable<String> columns) {
      // This is allowed to be blocking.
      return getReadContext().readRowUsingIndex(table, index, key, columns);
    }

    @Override
    public ApiFuture<Struct> readRowUsingIndexAsync(
        String table, String index, Key key, Iterable<String> columns) {
      return ApiFutures.transformAsync(
          this.readContextFuture,
          readContext -> readContext.readRowUsingIndexAsync(table, index, key, columns),
          MoreExecutors.directExecutor());
    }

    @Override
    public ResultSet executeQuery(Statement statement, QueryOption... options) {
      return new ForwardingResultSet(
          Suppliers.memoize(() -> getReadContext().executeQuery(statement, options)));
    }

    @Override
    public AsyncResultSet executeQueryAsync(Statement statement, QueryOption... options) {
      return new ForwardingAsyncResultSet(
          Suppliers.memoize(() -> getReadContext().executeQueryAsync(statement, options)));
    }

    @Override
    public ResultSet analyzeQuery(Statement statement, QueryAnalyzeMode queryMode) {
      return new ForwardingResultSet(
          Suppliers.memoize(() -> getReadContext().analyzeQuery(statement, queryMode)));
    }

    @Override
    public void close() {}
  }

  /**
   * Represents a delayed execution of a transaction on a multiplexed session. The execution is
   * delayed because the multiplexed session is not yet ready.
   */
  static class DelayedMultiplexedSessionTransaction
      extends AbstractMultiplexedSessionDatabaseClient {
    private final SpannerImpl spanner;

    private final ISpan span;

    private final ApiFuture<SessionReference> sessionFuture;

    DelayedMultiplexedSessionTransaction(
        SpannerImpl spanner, ISpan span, ApiFuture<SessionReference> sessionFuture) {
      this.spanner = spanner;
      this.span = span;
      this.sessionFuture = sessionFuture;
    }

    @Override
    public ReadContext singleUse() {
      return new DelayedReadContext<>(
          ApiFutures.transform(
              this.sessionFuture,
              sessionReference ->
                  new MultiplexedSessionTransaction(spanner, span, sessionReference).singleUse(),
              MoreExecutors.directExecutor()));
    }

    @Override
    public ReadContext singleUse(TimestampBound bound) {
      return new DelayedReadContext<>(
          ApiFutures.transform(
              this.sessionFuture,
              sessionReference ->
                  new MultiplexedSessionTransaction(spanner, span, sessionReference)
                      .singleUse(bound),
              MoreExecutors.directExecutor()));
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction() {
      return new DelayedReadOnlyTransaction(
          ApiFutures.transform(
              this.sessionFuture,
              sessionReference ->
                  new MultiplexedSessionTransaction(spanner, span, sessionReference)
                      .singleUseReadOnlyTransaction(),
              MoreExecutors.directExecutor()));
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
      return new DelayedReadOnlyTransaction(
          ApiFutures.transform(
              this.sessionFuture,
              sessionReference ->
                  new MultiplexedSessionTransaction(spanner, span, sessionReference)
                      .singleUseReadOnlyTransaction(bound),
              MoreExecutors.directExecutor()));
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction() {
      return new DelayedReadOnlyTransaction(
          ApiFutures.transform(
              this.sessionFuture,
              sessionReference ->
                  new MultiplexedSessionTransaction(spanner, span, sessionReference)
                      .readOnlyTransaction(),
              MoreExecutors.directExecutor()));
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
      return new DelayedReadOnlyTransaction(
          ApiFutures.transform(
              this.sessionFuture,
              sessionReference ->
                  new MultiplexedSessionTransaction(spanner, span, sessionReference)
                      .readOnlyTransaction(bound),
              MoreExecutors.directExecutor()));
    }
  }

  /**
   * Represents a single transaction on a multiplexed session. This can be both a single-use or
   * multi-use transaction, and both read/write or read-only transaction.
   */
  static class MultiplexedSessionTransaction extends SessionImpl {
    MultiplexedSessionTransaction(
        SpannerImpl spanner, ISpan span, SessionReference sessionReference) {
      super(spanner, sessionReference);
      setCurrentSpan(span);
    }

    @Override
    <T extends SessionTransaction> T setActive(@Nullable T ctx) {
      // TODO: Enable check again
      // throwIfTransactionsPending();
      return ctx;
    }

    @Override
    public void close() {
      // no-op
    }
  }

  private final SessionClient sessionClient;

  private final TraceWrapper tracer;

  private final AtomicReference<SettableApiFuture<SessionReference>> multiplexedSessionReference =
      new AtomicReference<>(SettableApiFuture.create());

  MultiplexedSessionDatabaseClient(SessionClient sessionClient) {
    this.sessionClient = sessionClient;
    this.tracer = sessionClient.getSpanner().getTracer();
    this.sessionClient.asyncCreateMultiplexedSession(
        new SessionConsumer() {
          @Override
          public void onSessionReady(SessionImpl session) {
            multiplexedSessionReference.get().set(session.getSessionReference());
          }

          @Override
          public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
            multiplexedSessionReference.get().setException(t);
          }
        });
    // TODO: Remove, this is just for testing.
    try {
      this.multiplexedSessionReference.get().get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isReady() {
    return multiplexedSessionReference.get().isDone();
  }

  private DatabaseClient createMultiplexedSessionTransaction() {
    return isReady()
        ? createDirectMultiplexedSessionTransaction()
        : createDelayedMultiplexSessionTransaction();
  }

  private MultiplexedSessionTransaction createDirectMultiplexedSessionTransaction() {
    try {
      return new MultiplexedSessionTransaction(
          sessionClient.getSpanner(),
          tracer.getCurrentSpan(),
          multiplexedSessionReference.get().get());
    } catch (ExecutionException executionException) {
      throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
    } catch (InterruptedException interruptedException) {
      throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
    }
  }

  private DelayedMultiplexedSessionTransaction createDelayedMultiplexSessionTransaction() {
    return new DelayedMultiplexedSessionTransaction(
        sessionClient.getSpanner(), tracer.getCurrentSpan(), multiplexedSessionReference.get());
  }

  @Override
  public ReadContext singleUse() {
    return createMultiplexedSessionTransaction().singleUse();
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    return createMultiplexedSessionTransaction().singleUse(bound);
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    return createMultiplexedSessionTransaction().singleUseReadOnlyTransaction();
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    return createMultiplexedSessionTransaction().singleUseReadOnlyTransaction(bound);
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    return createMultiplexedSessionTransaction().readOnlyTransaction();
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    return createDelayedMultiplexSessionTransaction().readOnlyTransaction(bound);
  }
}
