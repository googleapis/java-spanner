package com.google.cloud.spanner;

import com.google.api.core.ApiAsyncFunction;
import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.BatchWriteResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

class MultiplexedSessionPool implements DatabaseClient {
  static class DelayedReadContext implements ReadContext {
    private final ApiFuture<ReadContext> readContextFuture;

    DelayedReadContext(ApiFuture<ReadContext> readContextFuture) {
      this.readContextFuture = readContextFuture;
    }

    ReadContext getReadContext() {
      try {
        return this.readContextFuture.get();
      } catch (ExecutionException executionException) {
        throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
      } catch (InterruptedException interruptedException) {
        throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
      }
    }

    @Override
    public ResultSet read(String table, KeySet keys, Iterable<String> columns,
        ReadOption... options) {
      return null;
    }

    @Override
    public AsyncResultSet readAsync(String table, KeySet keys, Iterable<String> columns,
        ReadOption... options) {
      return null;
    }

    @Override
    public ResultSet readUsingIndex(String table, String index, KeySet keys,
        Iterable<String> columns, ReadOption... options) {
      return null;
    }

    @Override
    public AsyncResultSet readUsingIndexAsync(String table, String index, KeySet keys,
        Iterable<String> columns, ReadOption... options) {
      return null;
    }

    @Nullable
    @Override
    public Struct readRow(String table, Key key, Iterable<String> columns) {
      return null;
    }

    @Override
    public ApiFuture<Struct> readRowAsync(String table, Key key, Iterable<String> columns) {
      return null;
    }

    @Nullable
    @Override
    public Struct readRowUsingIndex(String table, String index, Key key, Iterable<String> columns) {
      return null;
    }

    @Override
    public ApiFuture<Struct> readRowUsingIndexAsync(String table, String index, Key key,
        Iterable<String> columns) {
      return null;
    }

    @Override
    public ResultSet executeQuery(Statement statement, QueryOption... options) {
      return new ForwardingResultSet(Suppliers.memoize(() -> getReadContext().executeQuery(statement, options)));
    }

    @Override
    public AsyncResultSet executeQueryAsync(Statement statement, QueryOption... options) {
      return null;
    }

    @Override
    public ResultSet analyzeQuery(Statement statement, QueryAnalyzeMode queryMode) {
      return null;
    }

    @Override
    public void close() {

    }
  }

  static class DelayedMultiplexedSessionUse implements DatabaseClient {
    private final SpannerImpl spanner;

    private final ISpan span;

    private final ApiFuture<SessionReference> sessionFuture;

    DelayedMultiplexedSessionUse(SpannerImpl spanner, ISpan span, ApiFuture<SessionReference> sessionFuture) {
      this.spanner = spanner;
      this.span = span;
      this.sessionFuture = sessionFuture;
    }

    @Override
    public Dialect getDialect() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getDatabaseRole() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
      throw new UnsupportedOperationException();
    }

    @Override
    public CommitResponse writeWithOptions(Iterable<Mutation> mutations,
        TransactionOption... options) throws SpannerException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
      throw new UnsupportedOperationException();
    }

    @Override
    public CommitResponse writeAtLeastOnceWithOptions(Iterable<Mutation> mutations,
        TransactionOption... options) throws SpannerException {
      throw new UnsupportedOperationException();
    }

    @Override
    public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
        Iterable<MutationGroup> mutationGroups, TransactionOption... options)
        throws SpannerException {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReadContext singleUse() {
      return new DelayedReadContext(ApiFutures.transform(this.sessionFuture, sessionReference -> new MultiplexedSessionUse(spanner, span, sessionReference).singleUse(), MoreExecutors.directExecutor()));
    }

    @Override
    public ReadContext singleUse(TimestampBound bound) {
      return null;
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction() {
      return null;
    }

    @Override
    public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
      return null;
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction() {
      return null;
    }

    @Override
    public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
      return null;
    }

    @Override
    public TransactionRunner readWriteTransaction(TransactionOption... options) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TransactionManager transactionManager(TransactionOption... options) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AsyncRunner runAsync(TransactionOption... options) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
      return 0;
    }
  }

  static class MultiplexedSessionUse extends SessionImpl {
    MultiplexedSessionUse(SpannerImpl spanner, ISpan span, SessionReference sessionReference) {
      super(spanner, sessionReference);
      setCurrentSpan(span);
    }

    @Override
    <T extends SessionTransaction> T setActive(@Nullable T ctx) {
      throwIfTransactionsPending();
      return ctx;
    }

    @Override
    public void close() {
      // no-op
    }
  }

  private final SessionClient sessionClient;

  private final TraceWrapper tracer;

  private final AtomicReference<SettableApiFuture<SessionReference>> multiplexedSessionReference = new AtomicReference<>(SettableApiFuture.create());

  MultiplexedSessionPool(SessionClient sessionClient) {
    this.sessionClient = sessionClient;
    this.tracer = sessionClient.getSpanner().getTracer();
    this.sessionClient.asyncCreateMultiplexedSession(new SessionConsumer() {
      @Override
      public void onSessionReady(SessionImpl session) {
        multiplexedSessionReference.get().set(session.getSessionReference());
      }

      @Override
      public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
        multiplexedSessionReference.get().setException(t);
      }
    });
  }

  boolean isReady() {
    return multiplexedSessionReference.get().isDone();
  }

  DatabaseClient createMultiplexedSessionUse() {
    return isReady()
        ? createMultiplexedSessionUse()
        : createDelayedMultiplexSessionUse();
  }

  MultiplexedSessionUse createDirectMultiplexedSessionUse() {
    try {
      return new MultiplexedSessionUse(sessionClient.getSpanner(), tracer.getCurrentSpan(),
          multiplexedSessionReference.get().get());
    } catch (ExecutionException executionException) {
      throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
    } catch (InterruptedException interruptedException) {
      throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
    }
  }

  DelayedMultiplexedSessionUse createDelayedMultiplexSessionUse() {
    return new DelayedMultiplexedSessionUse(sessionClient.getSpanner(), tracer.getCurrentSpan(), multiplexedSessionReference.get());
  }

  @Override
  public Dialect getDialect() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDatabaseRole() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommitResponse writeWithOptions(Iterable<Mutation> mutations, TransactionOption... options)
      throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommitResponse writeAtLeastOnceWithOptions(Iterable<Mutation> mutations,
      TransactionOption... options) throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
      Iterable<MutationGroup> mutationGroups, TransactionOption... options)
      throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReadContext singleUse() {
    return createMultiplexedSessionUse().singleUse();
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    return null;
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    return null;
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    return null;
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    return null;
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    return null;
  }

  @Override
  public TransactionRunner readWriteTransaction(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TransactionManager transactionManager(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AsyncRunner runAsync(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
    throw new UnsupportedOperationException();
  }
}
