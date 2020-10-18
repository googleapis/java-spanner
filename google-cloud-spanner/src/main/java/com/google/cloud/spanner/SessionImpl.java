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

package com.google.cloud.spanner;

import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerException;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.core.ApiFuture;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbstractReadContext.MultiUseReadOnlyTransaction;
import com.google.cloud.spanner.AbstractReadContext.SingleReadContext;
import com.google.cloud.spanner.AbstractReadContext.SingleUseReadOnlyTransaction;
import com.google.cloud.spanner.SessionClient.SessionId;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.common.base.Ticker;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CommitResponse;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;

/**
 * Implementation of {@link Session}. Sessions are managed internally by the client library, and
 * users need not be aware of the actual session management, pooling and handling.
 */
class SessionImpl implements Session {
  private static final Tracer tracer = Tracing.getTracer();

  /** Keep track of running transactions on this session per thread. */
  static final ThreadLocal<Boolean> hasPendingTransaction =
      new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
          return false;
        }
      };

  static void throwIfTransactionsPending() {
    if (hasPendingTransaction.get() == Boolean.TRUE) {
      throw newSpannerException(ErrorCode.INTERNAL, "Nested transactions are not supported");
    }
  }

  /**
   * Represents a transaction within a session. "Transaction" here is used in the general sense,
   * which covers standalone reads, standalone writes, single-use and multi-use read-only
   * transactions, and read-write transactions. The defining characteristic is that a session may
   * only have one such transaction active at a time.
   */
  static interface SessionTransaction {
    /** Invalidates the transaction, generally because a new one has been started on the session. */
    void invalidate();
    /** Registers the current span on the transaction. */
    void setSpan(Span span);
  }

  private static class WriteResponseImpl implements WriteResponse {
    private final Timestamp commitTimestamp;
    private final CommitStats commitStats;

    private WriteResponseImpl(TransactionRunner runner) {
      this.commitTimestamp = runner.getCommitTimestamp();
      this.commitStats = runner.getCommitStats();
    }

    private WriteResponseImpl(CommitResponse response) {
      this.commitTimestamp = Timestamp.fromProto(response.getCommitTimestamp());
      this.commitStats = CommitStats.fromProto(response.getCommitStats());
    }

    @Override
    public Timestamp getCommitTimestamp() {
      return this.commitTimestamp;
    }

    @Override
    public CommitStats getCommitStats() {
      return this.commitStats;
    }
  }

  private final SpannerImpl spanner;
  private final String name;
  private final DatabaseId databaseId;
  private SessionTransaction activeTransaction;
  ByteString readyTransactionId;
  private final Map<SpannerRpc.Option, ?> options;
  private Span currentSpan;

  SessionImpl(SpannerImpl spanner, String name, Map<SpannerRpc.Option, ?> options) {
    this.spanner = spanner;
    this.options = options;
    this.name = checkNotNull(name);
    this.databaseId = SessionId.of(name).getDatabaseId();
  }

  @Override
  public String getName() {
    return name;
  }

  Map<SpannerRpc.Option, ?> getOptions() {
    return options;
  }

  void setCurrentSpan(Span span) {
    currentSpan = span;
  }

  @Override
  public long executePartitionedUpdate(Statement stmt) {
    setActive(null);
    PartitionedDmlTransaction txn =
        new PartitionedDmlTransaction(this, spanner.getRpc(), Ticker.systemTicker());
    return txn.executeStreamingPartitionedUpdate(
        stmt, spanner.getOptions().getPartitionedDmlTimeout());
  }

  @Override
  public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
    return write(mutations, false).getCommitTimestamp();
  }

  @Override
  public WriteResponse writeWithCommitStats(Iterable<Mutation> mutations) throws SpannerException {
    return new WriteResponseImpl(write(mutations, true));
  }

  private TransactionRunner write(Iterable<Mutation> mutations, boolean withCommitStats) {
    TransactionRunner runner = readWriteTransaction();
    if (withCommitStats) {
      runner = runner.withCommitStats();
    }
    final Collection<Mutation> finalMutations =
        mutations instanceof java.util.Collection<?>
            ? (Collection<Mutation>) mutations
            : Lists.newArrayList(mutations);
    runner.run(
        new TransactionRunner.TransactionCallable<Void>() {
          @Override
          public Void run(TransactionContext ctx) {
            ctx.buffer(finalMutations);
            return null;
          }
        });
    return runner;
  }

  @Override
  public Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
    return Timestamp.fromProto(writeAtLeastOnce(mutations, false).getCommitTimestamp());
  }

  @Override
  public WriteResponse writeAtLeastOnceWithCommitStats(Iterable<Mutation> mutations)
      throws SpannerException {
    return new WriteResponseImpl(writeAtLeastOnce(mutations, true));
  }

  private CommitResponse writeAtLeastOnce(Iterable<Mutation> mutations, boolean withCommitStats) {
    setActive(null);
    List<com.google.spanner.v1.Mutation> mutationsProto = new ArrayList<>();
    Mutation.toProto(mutations, mutationsProto);
    final CommitRequest request =
        CommitRequest.newBuilder()
            .setSession(name)
            .setReturnCommitStats(withCommitStats)
            .addAllMutations(mutationsProto)
            .setSingleUseTransaction(
                TransactionOptions.newBuilder()
                    .setReadWrite(TransactionOptions.ReadWrite.getDefaultInstance()))
            .build();
    Span span = tracer.spanBuilder(SpannerImpl.COMMIT).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return spanner.getRpc().commit(request, options);
    } catch (IllegalArgumentException e) {
      TraceUtil.setWithFailure(span, e);
      throw newSpannerException(ErrorCode.INTERNAL, "Could not parse commit timestamp", e);
    } catch (RuntimeException e) {
      TraceUtil.setWithFailure(span, e);
      throw e;
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  @Override
  public ReadContext singleUse() {
    return singleUse(TimestampBound.strong());
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    return setActive(
        SingleReadContext.newBuilder()
            .setSession(this)
            .setTimestampBound(bound)
            .setRpc(spanner.getRpc())
            .setDefaultQueryOptions(spanner.getDefaultQueryOptions(databaseId))
            .setDefaultPrefetchChunks(spanner.getDefaultPrefetchChunks())
            .setSpan(currentSpan)
            .setExecutorProvider(spanner.getAsyncExecutorProvider())
            .build());
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    return singleUseReadOnlyTransaction(TimestampBound.strong());
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    return setActive(
        SingleUseReadOnlyTransaction.newBuilder()
            .setSession(this)
            .setTimestampBound(bound)
            .setRpc(spanner.getRpc())
            .setDefaultQueryOptions(spanner.getDefaultQueryOptions(databaseId))
            .setDefaultPrefetchChunks(spanner.getDefaultPrefetchChunks())
            .setSpan(currentSpan)
            .setExecutorProvider(spanner.getAsyncExecutorProvider())
            .buildSingleUseReadOnlyTransaction());
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    return readOnlyTransaction(TimestampBound.strong());
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    return setActive(
        MultiUseReadOnlyTransaction.newBuilder()
            .setSession(this)
            .setTimestampBound(bound)
            .setRpc(spanner.getRpc())
            .setDefaultQueryOptions(spanner.getDefaultQueryOptions(databaseId))
            .setDefaultPrefetchChunks(spanner.getDefaultPrefetchChunks())
            .setSpan(currentSpan)
            .setExecutorProvider(spanner.getAsyncExecutorProvider())
            .build());
  }

  @Override
  public TransactionRunner readWriteTransaction() {
    return setActive(
        new TransactionRunnerImpl(this, spanner.getRpc(), spanner.getDefaultPrefetchChunks()));
  }

  @Override
  public AsyncRunner runAsync() {
    return new AsyncRunnerImpl(
        setActive(
            new TransactionRunnerImpl(this, spanner.getRpc(), spanner.getDefaultPrefetchChunks())));
  }

  @Override
  public TransactionManager transactionManager() {
    return new TransactionManagerImpl(this, currentSpan);
  }

  @Override
  public AsyncTransactionManagerImpl transactionManagerAsync() {
    return new AsyncTransactionManagerImpl(this, currentSpan);
  }

  @Override
  public void prepareReadWriteTransaction() {
    setActive(null);
    readyTransactionId = beginTransaction();
  }

  @Override
  public ApiFuture<Empty> asyncClose() {
    return spanner.getRpc().asyncDeleteSession(name, options);
  }

  @Override
  public void close() {
    Span span = tracer.spanBuilder(SpannerImpl.DELETE_SESSION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      spanner.getRpc().deleteSession(name, options);
    } catch (RuntimeException e) {
      TraceUtil.setWithFailure(span, e);
      throw e;
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  ByteString beginTransaction() {
    try {
      return beginTransactionAsync().get();
    } catch (ExecutionException e) {
      throw SpannerExceptionFactory.newSpannerException(e.getCause() == null ? e : e.getCause());
    } catch (InterruptedException e) {
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }

  ApiFuture<ByteString> beginTransactionAsync() {
    final SettableApiFuture<ByteString> res = SettableApiFuture.create();
    final Span span = tracer.spanBuilder(SpannerImpl.BEGIN_TRANSACTION).startSpan();
    final BeginTransactionRequest request =
        BeginTransactionRequest.newBuilder()
            .setSession(name)
            .setOptions(
                TransactionOptions.newBuilder()
                    .setReadWrite(TransactionOptions.ReadWrite.getDefaultInstance()))
            .build();
    final ApiFuture<Transaction> requestFuture =
        spanner.getRpc().beginTransactionAsync(request, options);
    requestFuture.addListener(
        tracer.withSpan(
            span,
            new Runnable() {
              @Override
              public void run() {
                try {
                  Transaction txn = requestFuture.get();
                  if (txn.getId().isEmpty()) {
                    throw newSpannerException(
                        ErrorCode.INTERNAL, "Missing id in transaction\n" + getName());
                  }
                  span.end(TraceUtil.END_SPAN_OPTIONS);
                  res.set(txn.getId());
                } catch (ExecutionException e) {
                  TraceUtil.endSpanWithFailure(span, e);
                  res.setException(
                      SpannerExceptionFactory.newSpannerException(
                          e.getCause() == null ? e : e.getCause()));
                } catch (InterruptedException e) {
                  TraceUtil.endSpanWithFailure(span, e);
                  res.setException(SpannerExceptionFactory.propagateInterrupt(e));
                } catch (Exception e) {
                  TraceUtil.endSpanWithFailure(span, e);
                  res.setException(e);
                }
              }
            }),
        MoreExecutors.directExecutor());
    return res;
  }

  TransactionContextImpl newTransaction() {
    return TransactionContextImpl.newBuilder()
        .setSession(this)
        .setTransactionId(readyTransactionId)
        .setRpc(spanner.getRpc())
        .setDefaultQueryOptions(spanner.getDefaultQueryOptions(databaseId))
        .setDefaultPrefetchChunks(spanner.getDefaultPrefetchChunks())
        .setSpan(currentSpan)
        .setExecutorProvider(spanner.getAsyncExecutorProvider())
        .build();
  }

  <T extends SessionTransaction> T setActive(@Nullable T ctx) {
    throwIfTransactionsPending();

    if (activeTransaction != null) {
      activeTransaction.invalidate();
    }
    activeTransaction = ctx;
    readyTransactionId = null;
    if (activeTransaction != null) {
      activeTransaction.setSpan(currentSpan);
    }
    return ctx;
  }

  boolean hasReadyTransaction() {
    return readyTransactionId != null;
  }
}
