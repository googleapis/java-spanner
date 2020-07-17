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

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;

class DatabaseClientImpl implements DatabaseClient {
  private static final String READ_WRITE_TRANSACTION = "CloudSpanner.ReadWriteTransaction";
  private static final String READ_WRITE_TRANSACTION_WITH_INLINE_BEGIN =
      "CloudSpanner.ReadWriteTransactionWithInlineBegin";
  private static final String READ_ONLY_TRANSACTION = "CloudSpanner.ReadOnlyTransaction";
  private static final String PARTITION_DML_TRANSACTION = "CloudSpanner.PartitionDMLTransaction";
  private static final Tracer tracer = Tracing.getTracer();

  private enum SessionMode {
    READ,
    READ_WRITE
  }

  @VisibleForTesting final String clientId;
  @VisibleForTesting final SessionPool pool;
  private final boolean inlineBeginReadWriteTransactions;

  @VisibleForTesting
  DatabaseClientImpl(SessionPool pool, boolean inlineBeginReadWriteTransactions) {
    this("", pool, inlineBeginReadWriteTransactions);
  }

  DatabaseClientImpl(String clientId, SessionPool pool, boolean inlineBeginReadWriteTransactions) {
    this.clientId = clientId;
    this.pool = pool;
    this.inlineBeginReadWriteTransactions = inlineBeginReadWriteTransactions;
  }

  @VisibleForTesting
  PooledSessionFuture getReadSession() {
    return pool.getReadSession();
  }

  @VisibleForTesting
  PooledSessionFuture getReadWriteSession() {
    return pool.getReadWriteSession();
  }

  @Override
  public Timestamp write(final Iterable<Mutation> mutations) throws SpannerException {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return runWithSessionRetry(
          SessionMode.READ_WRITE,
          new Function<Session, Timestamp>() {
            @Override
            public Timestamp apply(Session session) {
              return session.write(mutations);
            }
          });
    } catch (RuntimeException e) {
      TraceUtil.setWithFailure(span, e);
      throw e;
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  @Override
  public Timestamp writeAtLeastOnce(final Iterable<Mutation> mutations) throws SpannerException {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return runWithSessionRetry(
          SessionMode.READ_WRITE,
          new Function<Session, Timestamp>() {
            @Override
            public Timestamp apply(Session session) {
              return session.writeAtLeastOnce(mutations);
            }
          });
    } catch (RuntimeException e) {
      TraceUtil.setWithFailure(span, e);
      throw e;
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  @Override
  public ReadContext singleUse() {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().singleUse();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().singleUse(bound);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().singleUseReadOnlyTransaction();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().singleUseReadOnlyTransaction(bound);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().readOnlyTransaction();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().readOnlyTransaction(bound);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public TransactionRunner readWriteTransaction() {
    return inlineBeginReadWriteTransactions
        ? inlinedReadWriteTransaction()
        : preparedReadWriteTransaction();
  }

  private TransactionRunner preparedReadWriteTransaction() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadWriteSession().readWriteTransaction();
    } catch (RuntimeException e) {
      TraceUtil.setWithFailure(span, e);
      throw e;
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  private TransactionRunner inlinedReadWriteTransaction() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION_WITH_INLINE_BEGIN).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      // An inlined read/write transaction does not need a write-prepared session.
      return getReadSession().readWriteTransaction();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public TransactionManager transactionManager() {
    return inlineBeginReadWriteTransactions
        ? inlinedTransactionManager()
        : preparedTransactionManager();
  }

  private TransactionManager preparedTransactionManager() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadWriteSession().transactionManager();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  private TransactionManager inlinedTransactionManager() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION_WITH_INLINE_BEGIN).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      // An inlined read/write transaction does not need a write-prepared session.
      return getReadSession().transactionManager();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public AsyncRunner runAsync() {
    return inlineBeginReadWriteTransactions ? inlinedRunAsync() : preparedRunAsync();
  }

  private AsyncRunner preparedRunAsync() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadWriteSession().runAsync();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  private AsyncRunner inlinedRunAsync() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION_WITH_INLINE_BEGIN).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().runAsync();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public AsyncTransactionManager transactionManagerAsync() {
    return inlineBeginReadWriteTransactions
        ? inlinedTransactionManagerAsync()
        : preparedTransactionManagerAsync();
  }

  private AsyncTransactionManager preparedTransactionManagerAsync() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadWriteSession().transactionManagerAsync();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  private AsyncTransactionManager inlinedTransactionManagerAsync() {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION_WITH_INLINE_BEGIN).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getReadSession().transactionManagerAsync();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public long executePartitionedUpdate(final Statement stmt) {
    Span span = tracer.spanBuilder(PARTITION_DML_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      // A partitioned update transaction does not need a prepared write session, as the transaction
      // object will start a new transaction with specific options anyway.
      return runWithSessionRetry(
          SessionMode.READ,
          new Function<Session, Long>() {
            @Override
            public Long apply(Session session) {
              return session.executePartitionedUpdate(stmt);
            }
          });
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  private <T> T runWithSessionRetry(SessionMode mode, Function<Session, T> callable) {
    PooledSessionFuture session =
        mode == SessionMode.READ_WRITE ? getReadWriteSession() : getReadSession();
    while (true) {
      try {
        return callable.apply(session);
      } catch (SessionNotFoundException e) {
        session =
            mode == SessionMode.READ_WRITE
                ? pool.replaceReadWriteSession(e, session)
                : pool.replaceReadSession(e, session);
      }
    }
  }

  ListenableFuture<Void> closeAsync(ClosedException closedException) {
    return pool.closeAsync(closedException);
  }
}
