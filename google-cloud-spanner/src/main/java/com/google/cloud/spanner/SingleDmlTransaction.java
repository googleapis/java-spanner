/*
 * Copyright 2021 Google LLC
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

import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.connection.Connection;
import io.opencensus.common.Scope;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.util.Arrays;

/**
 * Transaction that is used to execute a single DML statement. A successful execution will also
 * commit the transaction.
 *
 * <p>There are two intended usages of {@link SingleDmlTransaction}:
 *
 * <ul>
 *   <li>When {@link Connection} is in autocommit mode, an execute() or executeUpdate() of a DML
 *       statement will use {@link SingleDmlTransaction} under the hood.
 *   <li>Application code may call {@link DatabaseClient#singleDmlTransaction} to get a {@link
 *       SingleDmlTransaction} object, and call its executeUpdate() to execute a DML statement and
 *       inline-commit the transaction.
 * </ul>
 */
public class SingleDmlTransaction {
  private static final String READ_WRITE_TRANSACTION = "CloudSpanner.ReadWriteTransaction";
  private static final Tracer tracer = Tracing.getTracer();
  private final TransactionRunner runner;
  private boolean used = false;

  SingleDmlTransaction(PooledSessionFuture session, TransactionOption... options) {
    Span span = tracer.spanBuilder(READ_WRITE_TRANSACTION).startSpan();
    try (Scope s = tracer.withSpan(span)) {
      TransactionOption[] newOptions = Arrays.copyOf(options, options.length + 1);
      newOptions[options.length] = Options.INLINE_COMMIT_OPTION;
      runner = session.readWriteTransaction(newOptions);
    } catch (RuntimeException e) {
      TraceUtil.endSpanWithFailure(span, e);
      throw e;
    } finally {
      span.end(TraceUtil.END_SPAN_OPTIONS);
    }
  }

  SingleDmlTransaction(TransactionRunner runner) {
    this.runner = runner;
  }

  private synchronized void ensureNotUsed() {
    if (used) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.FAILED_PRECONDITION, "A SingleDmlTransaction can only be used once.");
    }
    used = true;
  }

  /**
   * Executes the DML statement(s) and returns the number of rows modified. For non-DML statements,
   * it will result in an {@code IllegalArgumentException}. Upon a successful execution, the
   * transaction will be committed, and the {@link CommitResponse} can be retrieved via a subsequent
   * call of getCommitResponse().
   */
  public long executeUpdate(Statement statement, UpdateOption... options) {
    ensureNotUsed();
    UpdateOption[] newOptions = Arrays.copyOf(options, options.length + 1);
    newOptions[options.length] = Options.INLINE_COMMIT_OPTION;
    return runner.run(transaction -> transaction.executeUpdate(statement, newOptions));
  }

  /**
   * Returns the {@link CommitResponse} of this transaction. It may only be called after a
   * successfully call of executeUpdate.
   */
  public CommitResponse getCommitResponse() {
    return runner.getCommitResponse();
  }
}
