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

import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerBatchUpdateException;
import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerException;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionImpl.SessionTransaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.rpc.Code;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteBatchDmlResponse;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import com.google.spanner.v1.TransactionSelector;
import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** Default implementation of {@link TransactionRunner}. */
class TransactionRunnerImpl implements SessionTransaction, TransactionRunner {
  private static final Tracer tracer = Tracing.getTracer();
  private static final Logger txnLogger = Logger.getLogger(TransactionRunner.class.getName());

  @VisibleForTesting
  static class TransactionContextImpl extends AbstractReadContext implements TransactionContext {
    static class Builder extends AbstractReadContext.Builder<Builder, TransactionContextImpl> {
      private ByteString transactionId;
      private Options options;
      private boolean trackTransactionStarter;

      private Builder() {}

      Builder setTransactionId(ByteString transactionId) {
        this.transactionId = transactionId;
        return self();
      }

      Builder setOptions(Options options) {
        this.options = Preconditions.checkNotNull(options);
        return self();
      }

      Builder setTrackTransactionStarter(boolean trackTransactionStarter) {
        this.trackTransactionStarter = trackTransactionStarter;
        return self();
      }

      @Override
      TransactionContextImpl build() {
        Preconditions.checkState(this.options != null, "Options must be set");
        return new TransactionContextImpl(this);
      }
    }

    static Builder newBuilder() {
      return new Builder();
    }

    /**
     * {@link AsyncResultSet} implementation that keeps track of the async operations that are still
     * running for this {@link TransactionContext} and that should finish before the {@link
     * TransactionContext} can commit and release its session back into the pool.
     */
    private class TransactionContextAsyncResultSetImpl extends ForwardingAsyncResultSet
        implements ListenableAsyncResultSet {
      private TransactionContextAsyncResultSetImpl(ListenableAsyncResultSet delegate) {
        super(delegate);
      }

      @Override
      public ApiFuture<Void> setCallback(Executor exec, ReadyCallback cb) {
        Runnable listener =
            new Runnable() {
              @Override
              public void run() {
                decreaseAsyncOperations();
              }
            };
        try {
          increaseAsynOperations();
          addListener(listener);
          return super.setCallback(exec, cb);
        } catch (Throwable t) {
          removeListener(listener);
          decreaseAsyncOperations();
          throw t;
        }
      }

      @Override
      public void addListener(Runnable listener) {
        ((ListenableAsyncResultSet) this.delegate).addListener(listener);
      }

      @Override
      public void removeListener(Runnable listener) {
        ((ListenableAsyncResultSet) this.delegate).removeListener(listener);
      }
    }

    @GuardedBy("lock")
    private volatile boolean committing;

    @GuardedBy("lock")
    private volatile SettableApiFuture<Void> finishedAsyncOperations = SettableApiFuture.create();

    @GuardedBy("lock")
    private volatile int runningAsyncOperations;

    @GuardedBy("lock")
    private List<Mutation> mutations = new ArrayList<>();

    @GuardedBy("lock")
    private boolean aborted;

    private final Options options;

    /** Default to -1 to indicate not available. */
    @GuardedBy("lock")
    private long retryDelayInMillis = -1L;

    /**
     * transactionIdFuture will return the transaction id returned by the first statement in the
     * transaction if the BeginTransaction option is included with the first statement of the
     * transaction.
     */
    private volatile SettableApiFuture<ByteString> transactionIdFuture = null;

    @VisibleForTesting long waitForTransactionTimeoutMillis = 60_000L;
    private final boolean trackTransactionStarter;
    private Exception transactionStarter;

    volatile ByteString transactionId;

    private CommitResponse commitResponse;

    private TransactionContextImpl(Builder builder) {
      super(builder);
      this.transactionId = builder.transactionId;
      this.trackTransactionStarter = builder.trackTransactionStarter;
      this.options = builder.options;
      this.finishedAsyncOperations.set(null);
    }

    private void increaseAsynOperations() {
      synchronized (lock) {
        if (runningAsyncOperations == 0) {
          finishedAsyncOperations = SettableApiFuture.create();
        }
        runningAsyncOperations++;
      }
    }

    private void decreaseAsyncOperations() {
      synchronized (lock) {
        runningAsyncOperations--;
        if (runningAsyncOperations == 0) {
          finishedAsyncOperations.set(null);
        }
      }
    }

    void ensureTxn() {
      try {
        ensureTxnAsync().get();
      } catch (ExecutionException e) {
        throw SpannerExceptionFactory.newSpannerException(e.getCause() == null ? e : e.getCause());
      } catch (InterruptedException e) {
        throw SpannerExceptionFactory.propagateInterrupt(e);
      }
    }

    ApiFuture<Void> ensureTxnAsync() {
      final SettableApiFuture<Void> res = SettableApiFuture.create();
      if (transactionId == null || isAborted()) {
        createTxnAsync(res);
      } else {
        span.addAnnotation(
            "Transaction Initialized",
            ImmutableMap.of(
                "Id", AttributeValue.stringAttributeValue(transactionId.toStringUtf8())));
        txnLogger.log(
            Level.FINER,
            "Using prepared transaction {0}",
            txnLogger.isLoggable(Level.FINER) ? transactionId.asReadOnlyByteBuffer() : null);
        res.set(null);
      }
      return res;
    }

    private void createTxnAsync(final SettableApiFuture<Void> res) {
      span.addAnnotation("Creating Transaction");
      final ApiFuture<ByteString> fut = session.beginTransactionAsync();
      fut.addListener(
          new Runnable() {
            @Override
            public void run() {
              try {
                transactionId = fut.get();
                span.addAnnotation(
                    "Transaction Creation Done",
                    ImmutableMap.of(
                        "Id", AttributeValue.stringAttributeValue(transactionId.toStringUtf8())));
                txnLogger.log(
                    Level.FINER,
                    "Started transaction {0}",
                    txnLogger.isLoggable(Level.FINER)
                        ? transactionId.asReadOnlyByteBuffer()
                        : null);
                res.set(null);
              } catch (ExecutionException e) {
                span.addAnnotation(
                    "Transaction Creation Failed",
                    TraceUtil.getExceptionAnnotations(e.getCause() == null ? e : e.getCause()));
                res.setException(e.getCause() == null ? e : e.getCause());
              } catch (InterruptedException e) {
                res.setException(SpannerExceptionFactory.propagateInterrupt(e));
              }
            }
          },
          MoreExecutors.directExecutor());
    }

    void commit() {
      try {
        commitResponse = commitAsync().get();
      } catch (InterruptedException e) {
        if (commitFuture != null) {
          commitFuture.cancel(true);
        }
        throw SpannerExceptionFactory.propagateInterrupt(e);
      } catch (ExecutionException e) {
        throw SpannerExceptionFactory.newSpannerException(e.getCause() == null ? e : e.getCause());
      }
    }

    volatile ApiFuture<CommitResponse> commitFuture;

    ApiFuture<CommitResponse> commitAsync() {
      final SettableApiFuture<CommitResponse> res = SettableApiFuture.create();
      final SettableApiFuture<Void> finishOps;
      CommitRequest.Builder builder =
          CommitRequest.newBuilder()
              .setSession(session.getName())
              .setReturnCommitStats(options.withCommitStats());
      synchronized (lock) {
        if (transactionIdFuture == null && transactionId == null && runningAsyncOperations == 0) {
          finishOps = SettableApiFuture.create();
          createTxnAsync(finishOps);
        } else {
          finishOps = finishedAsyncOperations;
        }
        if (!mutations.isEmpty()) {
          List<com.google.spanner.v1.Mutation> mutationsProto = new ArrayList<>();
          Mutation.toProto(mutations, mutationsProto);
          builder.addAllMutations(mutationsProto);
        }
        // Ensure that no call to buffer mutations that would be lost can succeed.
        mutations = null;
      }
      finishOps.addListener(
          new CommitRunnable(res, finishOps, builder), MoreExecutors.directExecutor());
      return res;
    }

    private final class CommitRunnable implements Runnable {
      private final SettableApiFuture<CommitResponse> res;
      private final ApiFuture<Void> prev;
      private final CommitRequest.Builder requestBuilder;

      CommitRunnable(
          SettableApiFuture<CommitResponse> res,
          ApiFuture<Void> prev,
          CommitRequest.Builder requestBuilder) {
        this.res = res;
        this.prev = prev;
        this.requestBuilder = requestBuilder;
      }

      @Override
      public void run() {
        try {
          prev.get();
          if (transactionId == null && transactionIdFuture == null) {
            requestBuilder.setSingleUseTransaction(
                TransactionOptions.newBuilder()
                    .setReadWrite(TransactionOptions.ReadWrite.getDefaultInstance()));
          } else {
            requestBuilder.setTransactionId(
                transactionId == null ? transactionIdFuture.get() : transactionId);
          }
          final CommitRequest commitRequest = requestBuilder.build();
          span.addAnnotation("Starting Commit");
          final Span opSpan =
              tracer.spanBuilderWithExplicitParent(SpannerImpl.COMMIT, span).startSpan();
          final ApiFuture<com.google.spanner.v1.CommitResponse> commitFuture =
              rpc.commitAsync(commitRequest, session.getOptions());
          commitFuture.addListener(
              tracer.withSpan(
                  opSpan,
                  new Runnable() {
                    @Override
                    public void run() {
                      try {
                        com.google.spanner.v1.CommitResponse proto = commitFuture.get();
                        if (!proto.hasCommitTimestamp()) {
                          throw newSpannerException(
                              ErrorCode.INTERNAL, "Missing commitTimestamp:\n" + session.getName());
                        }
                        span.addAnnotation("Commit Done");
                        opSpan.end(TraceUtil.END_SPAN_OPTIONS);
                        res.set(new CommitResponse(proto));
                      } catch (Throwable e) {
                        if (e instanceof ExecutionException) {
                          e =
                              SpannerExceptionFactory.newSpannerException(
                                  e.getCause() == null ? e : e.getCause());
                        } else if (e instanceof InterruptedException) {
                          e = SpannerExceptionFactory.propagateInterrupt((InterruptedException) e);
                        } else {
                          e = SpannerExceptionFactory.newSpannerException(e);
                        }
                        span.addAnnotation("Commit Failed", TraceUtil.getExceptionAnnotations(e));
                        TraceUtil.endSpanWithFailure(opSpan, e);
                        onError((SpannerException) e, false);
                        res.setException(e);
                      }
                    }
                  }),
              MoreExecutors.directExecutor());
        } catch (InterruptedException e) {
          res.setException(SpannerExceptionFactory.propagateInterrupt(e));
        } catch (ExecutionException e) {
          res.setException(
              SpannerExceptionFactory.newSpannerException(e.getCause() == null ? e : e.getCause()));
        }
      }
    }

    CommitResponse getCommitResponse() {
      checkState(commitResponse != null, "run() has not yet returned normally");
      return commitResponse;
    }

    boolean isAborted() {
      synchronized (lock) {
        return aborted;
      }
    }

    void rollback() {
      try {
        rollbackAsync().get();
      } catch (ExecutionException e) {
        txnLogger.log(Level.FINE, "Exception during rollback", e);
        span.addAnnotation("Rollback Failed", TraceUtil.getExceptionAnnotations(e));
      } catch (InterruptedException e) {
        throw SpannerExceptionFactory.propagateInterrupt(e);
      }
    }

    ApiFuture<Empty> rollbackAsync() {
      // It could be that there is no transaction if the transaction has been marked
      // withInlineBegin, and there has not been any query/update statement that has been executed.
      // In that case, we do not need to do anything, as there is no transaction.
      //
      // We do not take the transactionLock before trying to rollback to prevent a rollback call
      // from blocking if an async query or update statement that is trying to begin the transaction
      // is still in flight. That transaction will then automatically be terminated by the server.
      if (transactionId != null) {
        span.addAnnotation("Starting Rollback");
        return rpc.rollbackAsync(
            RollbackRequest.newBuilder()
                .setSession(session.getName())
                .setTransactionId(transactionId)
                .build(),
            session.getOptions());
      } else {
        return ApiFutures.immediateFuture(Empty.getDefaultInstance());
      }
    }

    @Nullable
    @Override
    TransactionSelector getTransactionSelector() {
      // Check if there is already a transactionId available. That is the case if this transaction
      // has already been prepared by the session pool, or if this transaction has been marked
      // withInlineBegin and an earlier statement has already started a transaction.
      if (transactionId == null) {
        try {
          ApiFuture<ByteString> tx = null;
          synchronized (lock) {
            // The first statement of a transaction that gets here will be the one that includes
            // BeginTransaction with the statement. The others will be waiting on the
            // transactionIdFuture until an actual transactionId is available.
            if (transactionIdFuture == null) {
              transactionIdFuture = SettableApiFuture.create();
              if (trackTransactionStarter) {
                transactionStarter = new Exception("Requesting new transaction");
              }
            } else {
              tx = transactionIdFuture;
            }
          }
          if (tx == null) {
            return TransactionSelector.newBuilder()
                .setBegin(
                    TransactionOptions.newBuilder()
                        .setReadWrite(TransactionOptions.ReadWrite.getDefaultInstance()))
                .build();
          } else {
            // Wait for the transaction to come available. The tx.get() call will fail with an
            // Aborted error if the call that included the BeginTransaction option fails. The
            // Aborted error will cause the entire transaction to be retried, and the retry will use
            // a separate BeginTransaction RPC.
            // If tx.get() returns successfully, this.transactionId will also have been set to a
            // valid value as the latter is always set when a transaction id is returned by a
            // statement.
            return TransactionSelector.newBuilder()
                .setId(tx.get(waitForTransactionTimeoutMillis, TimeUnit.MILLISECONDS))
                .build();
          }
        } catch (ExecutionException e) {
          if (e.getCause() instanceof AbortedException) {
            synchronized (lock) {
              aborted = true;
            }
          }
          throw SpannerExceptionFactory.newSpannerException(e.getCause());
        } catch (TimeoutException e) {
          // Throw an ABORTED exception to force a retry of the transaction if no transaction
          // has been returned by the first statement.
          SpannerException se =
              SpannerExceptionFactory.newSpannerException(
                  ErrorCode.ABORTED,
                  "Timeout while waiting for a transaction to be returned by another statement."
                      + (trackTransactionStarter
                          ? " See the suppressed exception for the stacktrace of the caller that should return a transaction"
                          : ""),
                  e);
          if (transactionStarter != null) {
            se.addSuppressed(transactionStarter);
          }
          throw se;
        } catch (InterruptedException e) {
          throw SpannerExceptionFactory.newSpannerExceptionForCancellation(null, e);
        }
      }
      // There is already a transactionId available. Include that id as the transaction to use.
      return TransactionSelector.newBuilder().setId(transactionId).build();
    }

    @Override
    public void onTransactionMetadata(Transaction transaction, boolean shouldIncludeId) {
      Preconditions.checkNotNull(transaction);
      if (transaction.getId() != ByteString.EMPTY) {
        // A transaction has been returned by a statement that was executed. Set the id of the
        // transaction on this instance and release the lock to allow other statements to proceed.
        if ((transactionIdFuture == null || !this.transactionIdFuture.isDone())
            && this.transactionId == null) {
          this.transactionId = transaction.getId();
          this.transactionIdFuture.set(transaction.getId());
        }
      } else if (shouldIncludeId) {
        // The statement should have returned a transaction.
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.FAILED_PRECONDITION, AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
      }
    }

    @Override
    public void onError(SpannerException e, boolean withBeginTransaction) {
      // If the statement that caused an error was the statement that included a BeginTransaction
      // option, we simulate an aborted transaction to force a retry of the entire transaction. This
      // will cause the retry to execute an explicit BeginTransaction RPC and then the actual
      // statements of the transaction. This is needed as the first statement of the transaction
      // must be included with the transaction to ensure that any locks that are taken by the
      // statement are included in the transaction, even if the statement again causes an error
      // during the retry.
      if (withBeginTransaction) {
        // Simulate an aborted transaction to force a retry with a new transaction.
        this.transactionIdFuture.setException(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.ABORTED, "Aborted due to failed initial statement", e));
      }

      if (e.getErrorCode() == ErrorCode.ABORTED) {
        long delay = -1L;
        if (e instanceof AbortedException) {
          delay = ((AbortedException) e).getRetryDelayInMillis();
        }
        if (delay == -1L) {
          txnLogger.log(Level.FINE, "Retry duration is missing from the exception.", e);
        }

        synchronized (lock) {
          retryDelayInMillis = delay;
          aborted = true;
        }
      }
    }

    @Override
    public void onDone(boolean withBeginTransaction) {
      if (withBeginTransaction
          && transactionIdFuture != null
          && !this.transactionIdFuture.isDone()) {
        // Context was done (closed) before a transaction id was returned.
        this.transactionIdFuture.setException(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.FAILED_PRECONDITION,
                "ResultSet was closed before a transaction id was returned"));
      }
    }

    @Override
    public void buffer(Mutation mutation) {
      synchronized (lock) {
        checkNotNull(mutations, "Context is closed");
        mutations.add(checkNotNull(mutation));
      }
    }

    @Override
    public void buffer(Iterable<Mutation> mutations) {
      synchronized (lock) {
        checkNotNull(this.mutations, "Context is closed");
        for (Mutation mutation : mutations) {
          this.mutations.add(checkNotNull(mutation));
        }
      }
    }

    @Override
    public long executeUpdate(Statement statement, UpdateOption... options) {
      beforeReadOrQuery();
      final ExecuteSqlRequest.Builder builder =
          getExecuteSqlRequestBuilder(
              statement,
              QueryMode.NORMAL,
              Options.fromUpdateOptions(options),
              /* withTransactionSelector = */ true);
      try {
        com.google.spanner.v1.ResultSet resultSet =
            rpc.executeQuery(builder.build(), session.getOptions());
        if (resultSet.getMetadata().hasTransaction()) {
          onTransactionMetadata(
              resultSet.getMetadata().getTransaction(), builder.getTransaction().hasBegin());
        }
        if (!resultSet.hasStats()) {
          throw new IllegalArgumentException(
              "DML response missing stats possibly due to non-DML statement as input");
        }
        // For standard DML, using the exact row count.
        return resultSet.getStats().getRowCountExact();
      } catch (Throwable t) {
        onError(SpannerExceptionFactory.asSpannerException(t), builder.getTransaction().hasBegin());
        throw t;
      }
    }

    @Override
    public ApiFuture<Long> executeUpdateAsync(Statement statement, UpdateOption... options) {
      beforeReadOrQuery();
      final ExecuteSqlRequest.Builder builder =
          getExecuteSqlRequestBuilder(
              statement,
              QueryMode.NORMAL,
              Options.fromUpdateOptions(options),
              /* withTransactionSelector = */ true);
      final ApiFuture<com.google.spanner.v1.ResultSet> resultSet;
      try {
        // Register the update as an async operation that must finish before the transaction may
        // commit.
        increaseAsynOperations();
        resultSet = rpc.executeQueryAsync(builder.build(), session.getOptions());
      } catch (Throwable t) {
        decreaseAsyncOperations();
        throw t;
      }
      ApiFuture<Long> updateCount =
          ApiFutures.transform(
              resultSet,
              new ApiFunction<com.google.spanner.v1.ResultSet, Long>() {
                @Override
                public Long apply(ResultSet input) {
                  if (!input.hasStats()) {
                    throw SpannerExceptionFactory.newSpannerException(
                        ErrorCode.INVALID_ARGUMENT,
                        "DML response missing stats possibly due to non-DML statement as input");
                  }
                  if (builder.getTransaction().hasBegin()
                      && !(input.getMetadata().hasTransaction()
                          && input.getMetadata().getTransaction().getId() != ByteString.EMPTY)) {
                    throw SpannerExceptionFactory.newSpannerException(
                        ErrorCode.FAILED_PRECONDITION, NO_TRANSACTION_RETURNED_MSG);
                  }
                  // For standard DML, using the exact row count.
                  return input.getStats().getRowCountExact();
                }
              },
              MoreExecutors.directExecutor());
      updateCount =
          ApiFutures.catching(
              updateCount,
              Throwable.class,
              new ApiFunction<Throwable, Long>() {
                @Override
                public Long apply(Throwable input) {
                  SpannerException e = SpannerExceptionFactory.asSpannerException(input);
                  onError(e, builder.getTransaction().hasBegin());
                  throw e;
                }
              },
              MoreExecutors.directExecutor());
      updateCount.addListener(
          new Runnable() {
            @Override
            public void run() {
              try {
                if (resultSet.get().getMetadata().hasTransaction()) {
                  onTransactionMetadata(
                      resultSet.get().getMetadata().getTransaction(),
                      builder.getTransaction().hasBegin());
                }
              } catch (Throwable e) {
                // Ignore this error here as it is handled by the future that is returned by the
                // executeUpdateAsync method.
              }
              decreaseAsyncOperations();
            }
          },
          MoreExecutors.directExecutor());
      return updateCount;
    }

    @Override
    public long[] batchUpdate(Iterable<Statement> statements, UpdateOption... options) {
      beforeReadOrQuery();
      final ExecuteBatchDmlRequest.Builder builder =
          getExecuteBatchDmlRequestBuilder(statements, Options.fromUpdateOptions(options));
      try {
        com.google.spanner.v1.ExecuteBatchDmlResponse response =
            rpc.executeBatchDml(builder.build(), session.getOptions());
        long[] results = new long[response.getResultSetsCount()];
        for (int i = 0; i < response.getResultSetsCount(); ++i) {
          results[i] = response.getResultSets(i).getStats().getRowCountExact();
          if (response.getResultSets(i).getMetadata().hasTransaction()) {
            onTransactionMetadata(
                response.getResultSets(i).getMetadata().getTransaction(),
                builder.getTransaction().hasBegin());
          }
        }

        // If one of the DML statements was aborted, we should throw an aborted exception.
        // In all other cases, we should throw a BatchUpdateException.
        if (response.getStatus().getCode() == Code.ABORTED_VALUE) {
          throw newSpannerException(
              ErrorCode.fromRpcStatus(response.getStatus()), response.getStatus().getMessage());
        } else if (response.getStatus().getCode() != 0) {
          throw newSpannerBatchUpdateException(
              ErrorCode.fromRpcStatus(response.getStatus()),
              response.getStatus().getMessage(),
              results);
        }
        return results;
      } catch (Throwable e) {
        onError(SpannerExceptionFactory.asSpannerException(e), builder.getTransaction().hasBegin());
        throw e;
      }
    }

    @Override
    public ApiFuture<long[]> batchUpdateAsync(
        Iterable<Statement> statements, UpdateOption... options) {
      beforeReadOrQuery();
      final ExecuteBatchDmlRequest.Builder builder =
          getExecuteBatchDmlRequestBuilder(statements, Options.fromUpdateOptions(options));
      ApiFuture<com.google.spanner.v1.ExecuteBatchDmlResponse> response;
      try {
        // Register the update as an async operation that must finish before the transaction may
        // commit.
        increaseAsynOperations();
        response = rpc.executeBatchDmlAsync(builder.build(), session.getOptions());
      } catch (Throwable t) {
        decreaseAsyncOperations();
        throw t;
      }
      ApiFuture<long[]> updateCounts =
          ApiFutures.transform(
              response,
              new ApiFunction<ExecuteBatchDmlResponse, long[]>() {
                @Override
                public long[] apply(ExecuteBatchDmlResponse input) {
                  long[] results = new long[input.getResultSetsCount()];
                  for (int i = 0; i < input.getResultSetsCount(); ++i) {
                    results[i] = input.getResultSets(i).getStats().getRowCountExact();
                    if (input.getResultSets(i).getMetadata().hasTransaction()) {
                      onTransactionMetadata(
                          input.getResultSets(i).getMetadata().getTransaction(),
                          builder.getTransaction().hasBegin());
                    }
                  }
                  // If one of the DML statements was aborted, we should throw an aborted exception.
                  // In all other cases, we should throw a BatchUpdateException.
                  if (input.getStatus().getCode() == Code.ABORTED_VALUE) {
                    throw newSpannerException(
                        ErrorCode.fromRpcStatus(input.getStatus()), input.getStatus().getMessage());
                  } else if (input.getStatus().getCode() != 0) {
                    throw newSpannerBatchUpdateException(
                        ErrorCode.fromRpcStatus(input.getStatus()),
                        input.getStatus().getMessage(),
                        results);
                  }
                  return results;
                }
              },
              MoreExecutors.directExecutor());
      updateCounts =
          ApiFutures.catching(
              updateCounts,
              Throwable.class,
              new ApiFunction<Throwable, long[]>() {
                @Override
                public long[] apply(Throwable input) {
                  SpannerException e = SpannerExceptionFactory.asSpannerException(input);
                  onError(e, builder.getTransaction().hasBegin());
                  throw e;
                }
              },
              MoreExecutors.directExecutor());
      updateCounts.addListener(
          new Runnable() {
            @Override
            public void run() {
              decreaseAsyncOperations();
            }
          },
          MoreExecutors.directExecutor());
      return updateCounts;
    }

    private ListenableAsyncResultSet wrap(ListenableAsyncResultSet delegate) {
      return new TransactionContextAsyncResultSetImpl(delegate);
    }

    @Override
    public ListenableAsyncResultSet readAsync(
        String table, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return wrap(super.readAsync(table, keys, columns, options));
    }

    @Override
    public ListenableAsyncResultSet readUsingIndexAsync(
        String table, String index, KeySet keys, Iterable<String> columns, ReadOption... options) {
      return wrap(super.readUsingIndexAsync(table, index, keys, columns, options));
    }

    @Override
    public ListenableAsyncResultSet executeQueryAsync(
        final Statement statement, final QueryOption... options) {
      return wrap(super.executeQueryAsync(statement, options));
    }
  }

  private boolean blockNestedTxn = true;
  private final SessionImpl session;
  private final Options options;
  private Span span;
  private TransactionContextImpl txn;
  private volatile boolean isValid = true;

  @Override
  public TransactionRunner allowNestedTransaction() {
    blockNestedTxn = false;
    return this;
  }

  TransactionRunnerImpl(SessionImpl session, TransactionOption... options) {
    this.session = session;
    this.options = Options.fromTransactionOptions(options);
    this.txn = session.newTransaction(this.options);
  }

  @Override
  public void setSpan(Span span) {
    this.span = span;
  }

  @Nullable
  @Override
  public <T> T run(TransactionCallable<T> callable) {
    try (Scope s = tracer.withSpan(span)) {
      if (blockNestedTxn) {
        SessionImpl.hasPendingTransaction.set(Boolean.TRUE);
      }
      return runInternal(callable);
    } catch (RuntimeException e) {
      TraceUtil.setWithFailure(span, e);
      throw e;
    } finally {
      // Remove threadLocal rather than set to FALSE to avoid a possible memory leak.
      // We also do this unconditionally in case a user has modified the flag when the transaction
      // was running.
      SessionImpl.hasPendingTransaction.remove();
    }
  }

  private <T> T runInternal(final TransactionCallable<T> txCallable) {
    final AtomicInteger attempt = new AtomicInteger();
    Callable<T> retryCallable =
        new Callable<T>() {
          @Override
          public T call() {
            boolean useInlinedBegin = true;
            if (attempt.get() > 0) {
              // Do not inline the BeginTransaction during a retry if the initial attempt did not
              // actually start a transaction.
              useInlinedBegin = txn.transactionId != null;
              txn = session.newTransaction(options);
            }
            checkState(
                isValid,
                "TransactionRunner has been invalidated by a new operation on the session");
            attempt.incrementAndGet();
            span.addAnnotation(
                "Starting Transaction Attempt",
                ImmutableMap.of("Attempt", AttributeValue.longAttributeValue(attempt.longValue())));
            // Only ensure that there is a transaction if we should not inline the beginTransaction
            // with the first statement.
            if (!useInlinedBegin) {
              txn.ensureTxn();
            }

            T result;
            boolean shouldRollback = true;
            try {
              result = txCallable.run(txn);
              shouldRollback = false;
            } catch (Exception e) {
              txnLogger.log(Level.FINE, "User-provided TransactionCallable raised exception", e);
              if (txn.isAborted() || (e instanceof AbortedException)) {
                span.addAnnotation(
                    "Transaction Attempt Aborted in user operation. Retrying",
                    ImmutableMap.of(
                        "Attempt", AttributeValue.longAttributeValue(attempt.longValue())));
                shouldRollback = false;
                if (e instanceof AbortedException) {
                  throw (AbortedException) e;
                }
                throw SpannerExceptionFactory.newSpannerException(
                    ErrorCode.ABORTED, e.getMessage(), e);
              }
              SpannerException toThrow;
              if (e instanceof SpannerException) {
                toThrow = (SpannerException) e;
              } else {
                toThrow = newSpannerException(ErrorCode.UNKNOWN, e.getMessage(), e);
              }
              span.addAnnotation(
                  "Transaction Attempt Failed in user operation",
                  ImmutableMap.<String, AttributeValue>builder()
                      .putAll(TraceUtil.getExceptionAnnotations(toThrow))
                      .put("Attempt", AttributeValue.longAttributeValue(attempt.longValue()))
                      .build());
              throw toThrow;
            } finally {
              if (shouldRollback) {
                txn.rollback();
              }
            }

            try {
              txn.commit();
              span.addAnnotation(
                  "Transaction Attempt Succeeded",
                  ImmutableMap.of(
                      "Attempt", AttributeValue.longAttributeValue(attempt.longValue())));
              return result;
            } catch (AbortedException e) {
              txnLogger.log(Level.FINE, "Commit aborted", e);
              span.addAnnotation(
                  "Transaction Attempt Aborted in Commit. Retrying",
                  ImmutableMap.of(
                      "Attempt", AttributeValue.longAttributeValue(attempt.longValue())));
              throw e;
            } catch (SpannerException e) {
              span.addAnnotation(
                  "Transaction Attempt Failed in Commit",
                  ImmutableMap.<String, AttributeValue>builder()
                      .putAll(TraceUtil.getExceptionAnnotations(e))
                      .put("Attempt", AttributeValue.longAttributeValue(attempt.longValue()))
                      .build());
              throw e;
            }
          }
        };
    return SpannerRetryHelper.runTxWithRetriesOnAborted(retryCallable);
  }

  @Override
  public Timestamp getCommitTimestamp() {
    checkState(txn != null, "run() has not yet returned normally");
    return txn.getCommitResponse().getCommitTimestamp();
  }

  public CommitResponse getCommitResponse() {
    checkState(txn != null, "run() has not yet returned normally");
    return txn.getCommitResponse();
  }

  @Override
  public void invalidate() {
    isValid = false;
  }
}
