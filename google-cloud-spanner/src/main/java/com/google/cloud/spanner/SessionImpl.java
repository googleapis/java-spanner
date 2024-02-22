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
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbstractReadContext.MultiUseReadOnlyTransaction;
import com.google.cloud.spanner.AbstractReadContext.SingleReadContext;
import com.google.cloud.spanner.AbstractReadContext.SingleUseReadOnlyTransaction;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionClient.SessionId;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.common.base.Ticker;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.spanner.v1.BatchWriteRequest;
import com.google.spanner.v1.BatchWriteResponse;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.RequestOptions;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.threeten.bp.Instant;

/**
 * Implementation of {@link Session}. Sessions are managed internally by the client library, and
 * users need not be aware of the actual session management, pooling and handling.
 */
class SessionImpl implements Session {
  private final TraceWrapper tracer;

  /** Keep track of running transactions on this session per thread. */
  static final ThreadLocal<Boolean> hasPendingTransaction = ThreadLocal.withInitial(() -> false);

  static void throwIfTransactionsPending() {
    if (hasPendingTransaction.get() == Boolean.TRUE) {
      throw newSpannerException(ErrorCode.INTERNAL, "Nested transactions are not supported");
    }
  }

  static TransactionOptions createReadWriteTransactionOptions(Options options) {
    TransactionOptions.ReadWrite.Builder readWrite = TransactionOptions.ReadWrite.newBuilder();
    if (options.withOptimisticLock() == Boolean.TRUE) {
      readWrite.setReadLockMode(TransactionOptions.ReadWrite.ReadLockMode.OPTIMISTIC);
    }
    return TransactionOptions.newBuilder().setReadWrite(readWrite).build();
  }

  /**
   * Represents a transaction within a session. "Transaction" here is used in the general sense,
   * which covers standalone reads, standalone writes, single-use and multi-use read-only
   * transactions, and read-write transactions. The defining characteristic is that a session may
   * only have one such transaction active at a time.
   */
  interface SessionTransaction {

    /** Invalidates the transaction, generally because a new one has been started on the session. */
    void invalidate();

    /** Registers the current span on the transaction. */
    void setSpan(ISpan span);
  }

  private final SpannerImpl spanner;
  private final String name;
  private final DatabaseId databaseId;
  private SessionTransaction activeTransaction;
  ByteString readyTransactionId;
  private final Map<SpannerRpc.Option, ?> options;
  private volatile Instant lastUseTime;
  private ISpan currentSpan;

  SessionImpl(SpannerImpl spanner, String name, Map<SpannerRpc.Option, ?> options) {
    this.spanner = spanner;
    this.tracer = spanner.getTracer();
    this.options = options;
    this.name = checkNotNull(name);
    this.databaseId = SessionId.of(name).getDatabaseId();
    this.lastUseTime = Instant.now();
  }

  @Override
  public String getName() {
    return name;
  }

  Map<SpannerRpc.Option, ?> getOptions() {
    return options;
  }

  void setCurrentSpan(ISpan span) {
    currentSpan = span;
  }

  ISpan getCurrentSpan() {
    return currentSpan;
  }

  Instant getLastUseTime() {
    return lastUseTime;
  }

  void markUsed(Instant instant) {
    lastUseTime = instant;
  }

  @Override
  public long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
    setActive(null);
    PartitionedDmlTransaction txn =
        new PartitionedDmlTransaction(this, spanner.getRpc(), Ticker.systemTicker());
    return txn.executeStreamingPartitionedUpdate(
        stmt, spanner.getOptions().getPartitionedDmlTimeout(), options);
  }

  @Override
  public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
    return writeWithOptions(mutations).getCommitTimestamp();
  }

  @Override
  public CommitResponse writeWithOptions(Iterable<Mutation> mutations, TransactionOption... options)
      throws SpannerException {
    TransactionRunner runner = readWriteTransaction(options);
    final Collection<Mutation> finalMutations =
        mutations instanceof java.util.Collection<?>
            ? (Collection<Mutation>) mutations
            : Lists.newArrayList(mutations);
    runner.run(
        ctx -> {
          ctx.buffer(finalMutations);
          return null;
        });
    return runner.getCommitResponse();
  }

  @Override
  public Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
    return writeAtLeastOnceWithOptions(mutations).getCommitTimestamp();
  }

  @Override
  public CommitResponse writeAtLeastOnceWithOptions(
      Iterable<Mutation> mutations, TransactionOption... transactionOptions)
      throws SpannerException {
    setActive(null);
    List<com.google.spanner.v1.Mutation> mutationsProto = new ArrayList<>();
    Mutation.toProto(mutations, mutationsProto);
    Options options = Options.fromTransactionOptions(transactionOptions);
    final CommitRequest.Builder requestBuilder =
        CommitRequest.newBuilder()
            .setSession(name)
            .setReturnCommitStats(options.withCommitStats())
            .addAllMutations(mutationsProto)
            .setSingleUseTransaction(
                TransactionOptions.newBuilder()
                    .setReadWrite(TransactionOptions.ReadWrite.getDefaultInstance()));
    if (options.hasMaxCommitDelay()) {
      requestBuilder.setMaxCommitDelay(
          Duration.newBuilder()
              .setSeconds(options.maxCommitDelay().getSeconds())
              .setNanos(options.maxCommitDelay().getNano())
              .build());
    }
    RequestOptions commitRequestOptions = getRequestOptions(transactionOptions);

    if (commitRequestOptions != null) {
      requestBuilder.setRequestOptions(commitRequestOptions);
    }
    CommitRequest request = requestBuilder.build();
    ISpan span = tracer.spanBuilder(SpannerImpl.COMMIT);
    try (IScope s = tracer.withSpan(span)) {
      return SpannerRetryHelper.runTxWithRetriesOnAborted(
          () -> new CommitResponse(spanner.getRpc().commit(request, this.options)));
    } catch (RuntimeException e) {
      span.setStatus(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private RequestOptions getRequestOptions(TransactionOption... transactionOptions) {
    Options requestOptions = Options.fromTransactionOptions(transactionOptions);
    if (requestOptions.hasPriority() || requestOptions.hasTag()) {
      RequestOptions.Builder requestOptionsBuilder = RequestOptions.newBuilder();
      if (requestOptions.hasPriority()) {
        requestOptionsBuilder.setPriority(requestOptions.priority());
      }
      if (requestOptions.hasTag()) {
        requestOptionsBuilder.setTransactionTag(requestOptions.tag());
      }
      return requestOptionsBuilder.build();
    }
    return null;
  }

  @Override
  public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
      Iterable<MutationGroup> mutationGroups, TransactionOption... transactionOptions)
      throws SpannerException {
    setActive(null);
    List<BatchWriteRequest.MutationGroup> mutationGroupsProto =
        MutationGroup.toListProto(mutationGroups);
    final BatchWriteRequest.Builder requestBuilder =
        BatchWriteRequest.newBuilder().setSession(name).addAllMutationGroups(mutationGroupsProto);
    RequestOptions batchWriteRequestOptions = getRequestOptions(transactionOptions);
    if (batchWriteRequestOptions != null) {
      requestBuilder.setRequestOptions(batchWriteRequestOptions);
    }
    ISpan span = tracer.spanBuilder(SpannerImpl.BATCH_WRITE);
    try (IScope s = tracer.withSpan(span)) {
      return spanner.getRpc().batchWriteAtLeastOnce(requestBuilder.build(), this.options);
    } catch (Throwable e) {
      span.setStatus(e);
      throw SpannerExceptionFactory.newSpannerException(e);
    } finally {
      span.end();
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
            .setDefaultDecodeMode(spanner.getDefaultDecodeMode())
            .setDefaultDirectedReadOptions(spanner.getOptions().getDirectedReadOptions())
            .setSpan(currentSpan)
            .setTracer(tracer)
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
            .setDefaultDecodeMode(spanner.getDefaultDecodeMode())
            .setDefaultDirectedReadOptions(spanner.getOptions().getDirectedReadOptions())
            .setSpan(currentSpan)
            .setTracer(tracer)
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
            .setDefaultDecodeMode(spanner.getDefaultDecodeMode())
            .setDefaultDirectedReadOptions(spanner.getOptions().getDirectedReadOptions())
            .setSpan(currentSpan)
            .setTracer(tracer)
            .setExecutorProvider(spanner.getAsyncExecutorProvider())
            .build());
  }

  @Override
  public TransactionRunner readWriteTransaction(TransactionOption... options) {
    return setActive(new TransactionRunnerImpl(this, options));
  }

  @Override
  public AsyncRunner runAsync(TransactionOption... options) {
    return new AsyncRunnerImpl(setActive(new TransactionRunnerImpl(this, options)));
  }

  @Override
  public TransactionManager transactionManager(TransactionOption... options) {
    return new TransactionManagerImpl(this, currentSpan, tracer, options);
  }

  @Override
  public AsyncTransactionManagerImpl transactionManagerAsync(TransactionOption... options) {
    return new AsyncTransactionManagerImpl(this, currentSpan, options);
  }

  @Override
  public void prepareReadWriteTransaction() {
    setActive(null);
    readyTransactionId = beginTransaction(true);
  }

  @Override
  public ApiFuture<Empty> asyncClose() {
    return spanner.getRpc().asyncDeleteSession(name, options);
  }

  @Override
  public void close() {
    ISpan span = tracer.spanBuilder(SpannerImpl.DELETE_SESSION);
    try (IScope s = tracer.withSpan(span)) {
      spanner.getRpc().deleteSession(name, options);
    } catch (RuntimeException e) {
      span.setStatus(e);
      throw e;
    } finally {
      span.end();
    }
  }

  ByteString beginTransaction(boolean routeToLeader) {
    try {
      return beginTransactionAsync(routeToLeader).get();
    } catch (ExecutionException e) {
      throw SpannerExceptionFactory.newSpannerException(e.getCause() == null ? e : e.getCause());
    } catch (InterruptedException e) {
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }

  ApiFuture<ByteString> beginTransactionAsync(boolean routeToLeader) {
    return beginTransactionAsync(Options.fromTransactionOptions(), routeToLeader);
  }

  ApiFuture<ByteString> beginTransactionAsync(Options transactionOptions, boolean routeToLeader) {
    final SettableApiFuture<ByteString> res = SettableApiFuture.create();
    final ISpan span = tracer.spanBuilder(SpannerImpl.BEGIN_TRANSACTION);
    final BeginTransactionRequest request =
        BeginTransactionRequest.newBuilder()
            .setSession(name)
            .setOptions(createReadWriteTransactionOptions(transactionOptions))
            .build();
    final ApiFuture<Transaction> requestFuture =
        spanner.getRpc().beginTransactionAsync(request, options, routeToLeader);
    requestFuture.addListener(
        () -> {
          try (IScope s = tracer.withSpan(span)) {
            Transaction txn = requestFuture.get();
            if (txn.getId().isEmpty()) {
              throw newSpannerException(
                  ErrorCode.INTERNAL, "Missing id in transaction\n" + getName());
            }
            span.end();
            res.set(txn.getId());
          } catch (ExecutionException e) {
            span.setStatus(e);
            span.end();
            res.setException(
                SpannerExceptionFactory.newSpannerException(
                    e.getCause() == null ? e : e.getCause()));
          } catch (InterruptedException e) {
            span.setStatus(e);
            span.end();
            res.setException(SpannerExceptionFactory.propagateInterrupt(e));
          } catch (Exception e) {
            span.setStatus(e);
            span.end();
            res.setException(e);
          }
        },
        MoreExecutors.directExecutor());
    return res;
  }

  TransactionContextImpl newTransaction(Options options) {
    // A clock instance is passed in {@code SessionPoolOptions} in order to allow mocking via tests.
    final Clock poolMaintainerClock =
        spanner.getOptions().getSessionPoolOptions().getPoolMaintainerClock();
    return TransactionContextImpl.newBuilder()
        .setSession(this)
        .setOptions(options)
        .setTransactionId(readyTransactionId)
        .setOptions(options)
        .setTrackTransactionStarter(spanner.getOptions().isTrackTransactionStarter())
        .setRpc(spanner.getRpc())
        .setDefaultQueryOptions(spanner.getDefaultQueryOptions(databaseId))
        .setDefaultPrefetchChunks(spanner.getDefaultPrefetchChunks())
        .setDefaultDecodeMode(spanner.getDefaultDecodeMode())
        .setSpan(currentSpan)
        .setTracer(tracer)
        .setExecutorProvider(spanner.getAsyncExecutorProvider())
        .setClock(poolMaintainerClock == null ? new Clock() : poolMaintainerClock)
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

  TraceWrapper getTracer() {
    return tracer;
  }
}
