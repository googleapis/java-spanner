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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.spanner.v1.BatchWriteResponse;
import io.opentelemetry.api.common.Attributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
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
  private final int dbId;
  private final AtomicInteger nthRequest;

  final boolean useMultiplexedSessionBlindWrite;

  @VisibleForTesting
  DatabaseClientImpl(SessionPool pool, TraceWrapper tracer) {
    this(
        "",
        pool,
        /* useMultiplexedSessionBlindWrite = */ false,
        /* multiplexedSessionDatabaseClient = */ null,
        /* useMultiplexedSessionPartitionedOps= */ false,
        tracer,
        /* useMultiplexedSessionForRW = */ false,
        Attributes.empty());
  }

  @VisibleForTesting
  DatabaseClientImpl(String clientId, SessionPool pool, TraceWrapper tracer) {
    this(
        clientId,
        pool,
        /* useMultiplexedSessionBlindWrite = */ false,
        /* multiplexedSessionDatabaseClient = */ null,
        /* useMultiplexedSessionPartitionedOps= */ false,
        tracer,
        /* useMultiplexedSessionForRW = */ false,
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

    this.dbId = this.dbIdFromClientId(this.clientId);
    this.nthRequest = new AtomicInteger(0);
  }

  private int dbIdFromClientId(String clientId) {
    int i = clientId.indexOf("-");
    String strWithValue = clientId.substring(i + 1);
    return Integer.parseInt(strWithValue);
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
    return pool.getDialect();
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
      return runWithSessionRetry(session -> session.writeWithOptions(mutations, options));
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

      int nthRequest = this.nextNthRequest();
      int channelId = 1; /* TODO: infer the channelId from the gRPC channel of the session */
      XGoogSpannerRequestId reqId = XGoogSpannerRequestId.of(this.dbId, channelId, nthRequest, 0);

      return runWithSessionRetry(
          (session) -> {
            reqId.incrementAttempt();
            // TODO: Update the channelId depending on the session that is inferred.
            ArrayList<TransactionOption> allOptions = new ArrayList<>();
            allOptions.add(new Options.RequestIdOption(reqId));
            allOptions.addAll(Arrays.asList(options));

            return session.writeAtLeastOnceWithOptions(
                mutations, allOptions.toArray(new TransactionOption[0]));
          });
    } catch (RuntimeException e) {
      span.setStatus(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private int nextNthRequest() {
    return this.nthRequest.addAndGet(1);
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
      return runWithSessionRetry(session -> session.batchWriteAtLeastOnce(mutationGroups, options));
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

  private long executePartitionedUpdateWithPooledSession(
      final Statement stmt, final UpdateOption... options) {
    ISpan span = tracer.spanBuilder(PARTITION_DML_TRANSACTION, commonAttributes);
    try (IScope s = tracer.withSpan(span)) {
      return runWithSessionRetry(session -> session.executePartitionedUpdate(stmt, options));
    } catch (RuntimeException e) {
      span.setStatus(e);
      span.end();
      throw e;
    }
  }

  private <T> T runWithSessionRetry(Function<Session, T> callable) {
    PooledSessionFuture session = getSession();
    while (true) {
      try {
        return callable.apply(session);
      } catch (SessionNotFoundException e) {
        session =
            (PooledSessionFuture)
                pool.getPooledSessionReplacementHandler().replaceSession(e, session);
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
