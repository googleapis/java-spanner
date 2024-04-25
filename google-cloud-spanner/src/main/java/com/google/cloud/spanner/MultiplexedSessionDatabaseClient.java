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

import com.google.api.core.SettableApiFuture;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * {@link DatabaseClient} implementation that uses a single multiplexed session to execute
 * transactions.
 */
class MultiplexedSessionDatabaseClient extends AbstractMultiplexedSessionDatabaseClient {

  /**
   * Represents a single transaction on a multiplexed session. This can be both a single-use or
   * multi-use transaction, and both read/write or read-only transaction. This can be compared to a
   * 'checked out session' of a pool, except as multiplexed sessions support multiple parallel
   * transactions, we do not need to actually check out and exclusively reserve a single session for
   * a transaction. This class therefore only contains context information about the current
   * transaction, such as the current span, and a reference to the multiplexed session that is used
   * for the transaction.
   */
  static class MultiplexedSessionTransaction extends SessionImpl {
    MultiplexedSessionTransaction(
        SpannerImpl spanner, ISpan span, SessionReference sessionReference) {
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
