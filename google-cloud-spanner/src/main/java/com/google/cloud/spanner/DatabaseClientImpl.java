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

import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.cloud.spanner.Statement.StatementFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.spanner.v1.BatchWriteResponse;
import io.opentelemetry.api.common.Attributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

class DatabaseClientImpl implements DatabaseClient {
  private static final String READ_WRITE_TRANSACTION = "CloudSpanner.ReadWriteTransaction";
  private static final String READ_ONLY_TRANSACTION = "CloudSpanner.ReadOnlyTransaction";
  private static final String PARTITION_DML_TRANSACTION = "CloudSpanner.PartitionDMLTransaction";
  private final TraceWrapper tracer;
  private Attributes commonAttributes;
  @VisibleForTesting final String clientId;
  @VisibleForTesting final SessionPool pool;
  @VisibleForTesting final MultiplexedSessionDatabaseClient multiplexedSessionDatabaseClient;
  @VisibleForTesting final boolean useMultiplexedSessionPartitionedOps;
  @VisibleForTesting final boolean useMultiplexedSessionForRW;
  @VisibleForTesting final int dbId;
  private final AtomicInteger nthRequest;
  private final Map<String, Integer> clientIdToOrdinalMap;

  final boolean useMultiplexedSessionBlindWrite;

  @VisibleForTesting
  DatabaseClientImpl(SessionPool pool, TraceWrapper tracer) {
    this(
        "",
        pool,
        /* useMultiplexedSessionBlindWrite= */ false,
        /* multiplexedSessionDatabaseClient= */ null,
        /* useMultiplexedSessionPartitionedOps= */ false,
        tracer,
        /* useMultiplexedSessionForRW= */ false,
        Attributes.empty());
  }

  @VisibleForTesting
  DatabaseClientImpl(String clientId, SessionPool pool, TraceWrapper tracer) {
    this(
        clientId,
        pool,
        /* useMultiplexedSessionBlindWrite= */ false,
        /* multiplexedSessionDatabaseClient= */ null,
        /* useMultiplexedSessionPartitionedOps= */ false,
        tracer,
        /* useMultiplexedSessionForRW= */ false,
        Attributes.empty());
  }

  DatabaseClientImpl(
      String clientId,
      SessionPool pool,
      boolean useMultiplexedSessionBlindWrite,
      @Nullable MultiplexedSessionDatabaseClient multiplexedSessionDatabaseClient,
      boolean useMultiplexedSessionPartitionedOps,
      TraceWrapper tracer,
      boolean useMultiplexedSessionForRW,
      Attributes commonAttributes) {
    this.clientId = clientId;
    this.pool = pool;
    this.useMultiplexedSessionBlindWrite = useMultiplexedSessionBlindWrite;
    this.multiplexedSessionDatabaseClient = multiplexedSessionDatabaseClient;
    this.useMultiplexedSessionPartitionedOps = useMultiplexedSessionPartitionedOps;
    this.tracer = tracer;
    this.useMultiplexedSessionForRW = useMultiplexedSessionForRW;
    this.commonAttributes = commonAttributes;

    this.clientIdToOrdinalMap = new HashMap<String, Integer>();
    this.dbId = this.dbIdFromClientId(this.clientId);
    this.nthRequest = new AtomicInteger(0);
  }

  @VisibleForTesting
  synchronized int dbIdFromClientId(String clientId) {
    Integer id = this.clientIdToOrdinalMap.get(clientId);
    if (id == null) {
      id = this.clientIdToOrdinalMap.size() + 1;
      this.clientIdToOrdinalMap.put(clientId, id);
    }
    return id;
  }

  @VisibleForTesting
  PooledSessionFuture getSession() {
    return pool.getSession();
  }

  @VisibleForTesting
  DatabaseClient getMultiplexedSession() {
    if (canUseMultiplexedSessions()) {
      return this.multiplexedSessionDatabaseClient;
    }
    return pool.getMultiplexedSessionWithFallback();
  }

  @VisibleForTesting
  DatabaseClient getMultiplexedSessionForRW() {
    if (canUseMultiplexedSessionsForRW()) {
      return getMultiplexedSession();
    }
    return getSession();
  }

  private MultiplexedSessionDatabaseClient getMultiplexedSessionDatabaseClient() {
    return canUseMultiplexedSessions() ? this.multiplexedSessionDatabaseClient : null;
  }

  private boolean canUseMultiplexedSessions() {
    return this.multiplexedSessionDatabaseClient != null
        && this.multiplexedSessionDatabaseClient.isMultiplexedSessionsSupported();
  }

  private boolean canUseMultiplexedSessionsForRW() {
    return this.useMultiplexedSessionForRW
        && this.multiplexedSessionDatabaseClient != null
        && this.multiplexedSessionDatabaseClient.isMultiplexedSessionsForRWSupported();
  }

  private boolean canUseMultiplexedSessionsForPartitionedOps() {
    return this.useMultiplexedSessionPartitionedOps
        && this.multiplexedSessionDatabaseClient != null
        && this.multiplexedSessionDatabaseClient.isMultiplexedSessionsForPartitionedOpsSupported();
  }

  @Override
  public Dialect getDialect() {
    MultiplexedSessionDatabaseClient client = getMultiplexedSessionDatabaseClient();
    if (client != null) {
      return client.getDialect();
    }
    return pool.getDialect();
  }

  private final AbstractLazyInitializer<StatementFactory> statementFactorySupplier =
      new AbstractLazyInitializer<StatementFactory>() {
        @Override
        protected StatementFactory initialize() {
          try {
            Dialect dialect = getDialectAsync().get(30, TimeUnit.SECONDS);
            return new StatementFactory(dialect);
          } catch (ExecutionException | TimeoutException e) {
            throw SpannerExceptionFactory.asSpannerException(e);
          } catch (InterruptedException e) {
            throw SpannerExceptionFactory.propagateInterrupt(e);
          }
        }
      };

  @Override
  public StatementFactory getStatementFactory() {
    try {
      return statementFactorySupplier.get();
    } catch (Exception exception) {
      throw SpannerExceptionFactory.asSpannerException(exception);
    }
  }

  @Override
  @Nullable
  public String getDatabaseRole() {
    return pool.getDatabaseRole();
  }

  @Override
  public Timestamp write(final Iterable<Mutation> mutations) throws SpannerException {
    return writeWithOptions(mutations).getCommitTimestamp();
  }

  @Override
  public CommitResponse writeWithOptions(
      final Iterable<Mutation> mutations, final TransactionOption... options)
      throws SpannerException {
    ISpan span = tracer.spanBuilder(READ_WRITE_TRANSACTION, commonAttributes, options);
    try (IScope s = tracer.withSpan(span)) {
      if (canUseMultiplexedSessionsForRW() && getMultiplexedSessionDatabaseClient() != null) {
        return getMultiplexedSessionDatabaseClient().writeWithOptions(mutations, options);
      }

      return runWithSessionRetry(
          (session, reqId) -> {
            return session.writeWithOptions(mutations, withReqId(reqId, options));
          });
    } catch (RuntimeException e) {
      span.setStatus(e);
      throw e;
    } finally {
      span.end();
    }
  }

  @Override
  public Timestamp writeAtLeastOnce(final Iterable<Mutation> mutations) throws SpannerException {
    return writeAtLeastOnceWithOptions(mutations).getCommitTimestamp();
  }

  @Override
  public CommitResponse writeAtLeastOnceWithOptions(
      final Iterable<Mutation> mutations, final TransactionOption... options)
      throws SpannerException {
    ISpan span = tracer.spanBuilder(READ_WRITE_TRANSACTION, commonAttributes, options);
    try (IScope s = tracer.withSpan(span)) {
      if (useMultiplexedSessionBlindWrite && getMultiplexedSessionDatabaseClient() != null) {
        return getMultiplexedSessionDatabaseClient()
            .writeAtLeastOnceWithOptions(mutations, options);
      }
      return runWithSessionRetry(
          (session, reqId) ->
              session.writeAtLeastOnceWithOptions(mutations, withReqId(reqId, options)));
    } catch (RuntimeException e) {
      span.setStatus(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private int nextNthRequest() {
    return this.nthRequest.incrementAndGet();
  }

  @VisibleForTesting
  int getNthRequest() {
    return this.nthRequest.get();
  }

  @Override
  public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
      final Iterable<MutationGroup> mutationGroups, final TransactionOption... options)
      throws SpannerException {
    ISpan span = tracer.spanBuilder(READ_WRITE_TRANSACTION, commonAttributes, options);
    try (IScope s = tracer.withSpan(span)) {
      if (canUseMultiplexedSessionsForRW() && getMultiplexedSessionDatabaseClient() != null) {
        return getMultiplexedSessionDatabaseClient().batchWriteAtLeastOnce(mutationGroups, options);
      }
      return runWithSessionRetry(
          (session, reqId) ->
              session.batchWriteAtLeastOnce(mutationGroups, withReqId(reqId, options)));
    } catch (RuntimeException e) {
      span.setStatus(e);
      throw e;
    } finally {
      span.end();
    }
  }

  @Override
  public ReadContext singleUse() {
    ISpan span = tracer.spanBuilder(READ_ONLY_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSession().singleUse();
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    ISpan span = tracer.spanBuilder(READ_ONLY_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSession().singleUse(bound);
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    ISpan span = tracer.spanBuilder(READ_ONLY_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSession().singleUseReadOnlyTransaction();
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    ISpan span = tracer.spanBuilder(READ_ONLY_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSession().singleUseReadOnlyTransaction(bound);
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    ISpan span = tracer.spanBuilder(READ_ONLY_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSession().readOnlyTransaction();
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    ISpan span = tracer.spanBuilder(READ_ONLY_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSession().readOnlyTransaction(bound);
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public TransactionRunner readWriteTransaction(TransactionOption... options) {
    ISpan span = tracer.spanBuilder(READ_WRITE_TRANSACTION, commonAttributes, options);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSessionForRW().readWriteTransaction(options);
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public TransactionManager transactionManager(TransactionOption... options) {
    ISpan span = tracer.spanBuilder(READ_WRITE_TRANSACTION, commonAttributes, options);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSessionForRW().transactionManager(options);
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public AsyncRunner runAsync(TransactionOption... options) {
    ISpan span = tracer.spanBuilder(READ_WRITE_TRANSACTION, commonAttributes, options);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSessionForRW().runAsync(options);
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
    ISpan span = tracer.spanBuilder(READ_WRITE_TRANSACTION, commonAttributes, options);
    try (IScope s = tracer.withSpan(span)) {
      return getMultiplexedSessionForRW().transactionManagerAsync(options);
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @Override
  public long executePartitionedUpdate(final Statement stmt, final UpdateOption... options) {

    if (canUseMultiplexedSessionsForPartitionedOps()) {
      try {
        return getMultiplexedSession().executePartitionedUpdate(stmt, options);
      } catch (SpannerException e) {
        if (!multiplexedSessionDatabaseClient.maybeMarkUnimplementedForPartitionedOps(e)) {
          throw e;
        }
      }
    }
    return executePartitionedUpdateWithPooledSession(stmt, options);
  }

  private Future<Dialect> getDialectAsync() {
    MultiplexedSessionDatabaseClient client = getMultiplexedSessionDatabaseClient();
    if (client != null) {
      return client.getDialectAsync();
    }
    return pool.getDialectAsync();
  }

  private UpdateOption[] withReqId(
      final XGoogSpannerRequestId reqId, final UpdateOption... options) {
    if (reqId == null) {
      return options;
    }
    if (options == null || options.length == 0) {
      return new UpdateOption[] {new Options.RequestIdOption(reqId)};
    }
    UpdateOption[] allOptions = new UpdateOption[options.length + 1];
    System.arraycopy(options, 0, allOptions, 0, options.length);
    allOptions[options.length] = new Options.RequestIdOption(reqId);
    return allOptions;
  }

  private TransactionOption[] withReqId(
      final XGoogSpannerRequestId reqId, final TransactionOption... options) {
    if (reqId == null) {
      return options;
    }
    if (options == null || options.length == 0) {
      return new TransactionOption[] {new Options.RequestIdOption(reqId)};
    }
    TransactionOption[] allOptions = new TransactionOption[options.length + 1];
    System.arraycopy(options, 0, allOptions, 0, options.length);
    allOptions[options.length] = new Options.RequestIdOption(reqId);
    return allOptions;
  }

  private long executePartitionedUpdateWithPooledSession(
      final Statement stmt, final UpdateOption... options) {
    ISpan span = tracer.spanBuilder(PARTITION_DML_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return runWithSessionRetry(
          (session, reqId) -> {
            return session.executePartitionedUpdate(stmt, withReqId(reqId, options));
          });
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  @VisibleForTesting
  <T> T runWithSessionRetry(BiFunction<Session, XGoogSpannerRequestId, T> callable) {
    PooledSessionFuture session = getSession();
    XGoogSpannerRequestId reqId =
        XGoogSpannerRequestId.of(
            this.dbId, Long.valueOf(session.getChannel()), this.nextNthRequest(), 1);
    while (true) {
      try {
        return callable.apply(session, reqId);
      } catch (SessionNotFoundException e) {
        session =
            (PooledSessionFuture)
                pool.getPooledSessionReplacementHandler().replaceSession(e, session);
        reqId =
            XGoogSpannerRequestId.of(
                this.dbId, Long.valueOf(session.getChannel()), this.nextNthRequest(), 1);
      }
    }
  }

  boolean isValid() {
    return pool.isValid()
        && (multiplexedSessionDatabaseClient == null
            || multiplexedSessionDatabaseClient.isValid()
            || !multiplexedSessionDatabaseClient.isMultiplexedSessionsSupported());
  }

  ListenableFuture<Void> closeAsync(ClosedException closedException) {
    if (this.multiplexedSessionDatabaseClient != null) {
      // This method is non-blocking.
      this.multiplexedSessionDatabaseClient.close();
    }
    return pool.closeAsync(closedException);
  }
}
