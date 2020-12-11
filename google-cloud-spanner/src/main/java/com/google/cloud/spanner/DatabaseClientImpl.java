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
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
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
  private static final String READ_ONLY_TRANSACTION = "CloudSpanner.ReadOnlyTransaction";
  private static final String PARTITION_DML_TRANSACTION = "CloudSpanner.PartitionDMLTransaction";
  private static final Tracer tracer = Tracing.getTracer();

  @VisibleForTesting final String clientId;
  @VisibleForTesting final SessionPool pool;

  @VisibleForTesting
  DatabaseClientImpl(SessionPool pool) {
    this("", pool);
  }

  DatabaseClientImpl(String clientId, SessionPool pool) {
    this.clientId = clientId;
    this.pool = pool;
  }

  @VisibleForTesting
  PooledSessionFuture getSession() {
    return pool.getSession();
  }

  @Override
  public Timestamp write(final Iterable<Mutation> mutations) throws SpannerException {
    return writeWithOptions(mutations).getCommitTimestamp();
  }

  @Override
  public CommitResponse writeWithOptions(
      final Iterable<Mutation> mutations, final TransactionOption... options)
      throws SpannerException {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return runWithSessionRetry(
          new Function<Session, CommitResponse>() {
            @Override
            public CommitResponse apply(Session session) {
              return session.writeWithOptions(mutations, options);
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
    return writeAtLeastOnceWithOptions(mutations).getCommitTimestamp();
  }

  @Override
  public CommitResponse writeAtLeastOnceWithOptions(
      final Iterable<Mutation> mutations, final TransactionOption... options)
      throws SpannerException {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return runWithSessionRetry(
          new Function<Session, CommitResponse>() {
            @Override
            public CommitResponse apply(Session session) {
              return session.writeAtLeastOnceWithOptions(mutations, options);
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
      return getSession().singleUse();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().singleUse(bound);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().singleUseReadOnlyTransaction();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().singleUseReadOnlyTransaction(bound);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().readOnlyTransaction();
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    Span span = tracer.spanBuilder(READ_ONLY_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().readOnlyTransaction(bound);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public TransactionRunner readWriteTransaction(TransactionOption... options) {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().readWriteTransaction(options);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  @Override
  public TransactionManager transactionManager(TransactionOption... options) {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().transactionManager(options);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public AsyncRunner runAsync(TransactionOption... options) {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().runAsync(options);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return getSession().transactionManagerAsync(options);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  @Override
  public long executePartitionedUpdate(final Statement stmt, final UpdateOption... options) {
    Span span = tracer.spanBuilder(PARTITION_DML_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      return runWithSessionRetry(
          new Function<Session, Long>() {
            @Override
            public Long apply(Session session) {
              return session.executePartitionedUpdate(stmt, options);
            }
          });
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    }
  }

  private <T> T runWithSessionRetry(Function<Session, T> callable) {
    PooledSessionFuture session = getSession();
    while (true) {
      try {
        return callable.apply(session);
      } catch (SessionNotFoundException e) {
        session = pool.replaceSession(e, session);
      }
    }
  }

  ListenableFuture<Void> closeAsync(ClosedException closedException) {
    return pool.closeAsync(closedException);
  }
}
