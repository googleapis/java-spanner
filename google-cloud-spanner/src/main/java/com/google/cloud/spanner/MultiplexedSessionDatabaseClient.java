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

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SpannerException.ResourceNotFoundException;
import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link DatabaseClient} implementation that uses a single multiplexed session to execute
 * transactions.
 */
final class MultiplexedSessionDatabaseClient extends AbstractMultiplexedSessionDatabaseClient {

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
    private final MultiplexedSessionDatabaseClient client;

    private final boolean singleUse;

    MultiplexedSessionTransaction(
        MultiplexedSessionDatabaseClient client,
        ISpan span,
        SessionReference sessionReference,
        boolean singleUse) {
      super(client.sessionClient.getSpanner(), sessionReference);
      this.client = client;
      this.singleUse = singleUse;
      setCurrentSpan(span);
    }

    @Override
    void onError(SpannerException spannerException) {
      if (this.client.resourceNotFoundException.get() == null
          && (spannerException instanceof DatabaseNotFoundException
              || spannerException instanceof InstanceNotFoundException
              || spannerException instanceof SessionNotFoundException)) {
        // This could in theory set this field more than once, but we don't want to bother with
        // synchronizing, as it does not really matter exactly which error is set.
        this.client.resourceNotFoundException.set((ResourceNotFoundException) spannerException);
        System.out.println("Registered resource not found exception: " + spannerException);
      }
    }

    @Override
    void onReadDone() {
      // This method is called whenever a ResultSet that was returned by this transaction is closed.
      // Close the active transaction if this is a single-use transaction. This ensures that the
      // active span is ended.
      if (this.singleUse && getActiveTransaction() != null) {
        getActiveTransaction().close();
        setActive(null);
      }
    }

    @Override
    public void close() {
      // no-op, we don't want to delete the multiplexed session.
    }
  }

  // Replace the session after 7 days, even though they can live for 28 days.
  private static final Duration SESSION_EXPIRATION_TIME = Duration.ofDays(7);

  private boolean isClosed;

  private final SessionClient sessionClient;

  private final TraceWrapper tracer;

  private final AtomicReference<ApiFuture<SessionReference>> multiplexedSessionReference;

  // Initialize the expiration date to the current time + 7 days to avoid unnecessary null checks.
  // The time difference with the actual creation is small enough that it does not matter.
  private final AtomicReference<Instant> expirationDate =
      new AtomicReference<>(Instant.now().plus(SESSION_EXPIRATION_TIME));

  private final MultiplexedSessionMaintainer maintainer = new MultiplexedSessionMaintainer();

  private final AtomicReference<ResourceNotFoundException> resourceNotFoundException =
      new AtomicReference<>();

  MultiplexedSessionDatabaseClient(SessionClient sessionClient) {
    this.sessionClient = sessionClient;
    this.tracer = sessionClient.getSpanner().getTracer();
    final SettableApiFuture<SessionReference> initialSessionReferenceFuture =
        SettableApiFuture.create();
    this.multiplexedSessionReference = new AtomicReference<>(initialSessionReferenceFuture);
    this.sessionClient.asyncCreateMultiplexedSession(
        new SessionConsumer() {
          @Override
          public void onSessionReady(SessionImpl session) {
            initialSessionReferenceFuture.set(session.getSessionReference());
            // only start the maintainer if we actually managed to create a session in the first
            // place.
            maintainer.start();
          }

          @Override
          public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
            initialSessionReferenceFuture.setException(t);
          }
        });
    maybeWaitForSessionCreation(
        sessionClient.getSpanner().getOptions().getSessionPoolOptions(),
        initialSessionReferenceFuture);
  }

  private static void maybeWaitForSessionCreation(
      SessionPoolOptions sessionPoolOptions, ApiFuture<SessionReference> future) {
    org.threeten.bp.Duration waitDuration = sessionPoolOptions.getWaitForMinSessions();
    if (waitDuration != null && !waitDuration.isZero()) {
      long timeoutMillis = waitDuration.toMillis();
      try {
        future.get(timeoutMillis, TimeUnit.MILLISECONDS);
      } catch (ExecutionException executionException) {
        throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
      } catch (InterruptedException interruptedException) {
        throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
      } catch (TimeoutException timeoutException) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.DEADLINE_EXCEEDED,
            "Timed out after waiting " + timeoutMillis + "ms for multiplexed session creation");
      }
    }
  }

  boolean isValid() {
    return resourceNotFoundException.get() == null;
  }

  void close() {
    synchronized (this) {
      if (!this.isClosed) {
        this.isClosed = true;
        this.maintainer.stop();
      }
    }
  }

  /**
   * Returns true if the multiplexed session has been created. This client can be used before the
   * session has been created, and will in that case use a delayed transaction that contains a
   * future reference to the multiplexed session. The delayed transaction will block at the first
   * actual statement that is being executed (e.g. the first query that is sent to Spanner).
   */
  private boolean isMultiplexedSessionCreated() {
    return multiplexedSessionReference.get().isDone();
  }

  private DatabaseClient createMultiplexedSessionTransaction(boolean singleUse) {
    Preconditions.checkState(!isClosed, "This client has been closed");
    return isMultiplexedSessionCreated()
        ? createDirectMultiplexedSessionTransaction(singleUse)
        : createDelayedMultiplexSessionTransaction();
  }

  private MultiplexedSessionTransaction createDirectMultiplexedSessionTransaction(
      boolean singleUse) {
    try {
      return new MultiplexedSessionTransaction(
          this,
          tracer.getCurrentSpan(),
          // Getting the result of the SettableApiFuture that contains the multiplexed session will
          // also automatically propagate any error that happened during the creation of the
          // session, such as for example a DatabaseNotFound exception. We therefore do not need
          // any special handling of such errors.
          multiplexedSessionReference.get().get(),
          singleUse);
    } catch (ExecutionException executionException) {
      throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
    } catch (InterruptedException interruptedException) {
      throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
    }
  }

  private DelayedMultiplexedSessionTransaction createDelayedMultiplexSessionTransaction() {
    return new DelayedMultiplexedSessionTransaction(
        this, tracer.getCurrentSpan(), multiplexedSessionReference.get());
  }

  @Override
  public ReadContext singleUse() {
    return createMultiplexedSessionTransaction(true).singleUse();
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    return createMultiplexedSessionTransaction(true).singleUse(bound);
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    return createMultiplexedSessionTransaction(true).singleUseReadOnlyTransaction();
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    return createMultiplexedSessionTransaction(true).singleUseReadOnlyTransaction(bound);
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    return createMultiplexedSessionTransaction(false).readOnlyTransaction();
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    return createMultiplexedSessionTransaction(false).readOnlyTransaction(bound);
  }

  /**
   * It is enough with one executor to maintain the multiplexed sessions in all the clients, as they
   * do not need to be updated often, and the maintenance task is light.
   */
  private static final ScheduledExecutorService MAINTAINER_SERVICE =
      Executors.newScheduledThreadPool(
          0,
          ThreadFactoryUtil.createVirtualOrPlatformDaemonThreadFactory(
              "multiplexed-session-maintainer", /* tryVirtual = */ false));

  final class MultiplexedSessionMaintainer {
    // Schedule the maintainer to run once every ten minutes.
    private ScheduledFuture<?> scheduledFuture;

    void start() {
      this.scheduledFuture =
          MAINTAINER_SERVICE.scheduleAtFixedRate(this::maintain, 10, 10, TimeUnit.MINUTES);
    }

    void stop() {
      if (this.scheduledFuture != null) {
        this.scheduledFuture.cancel(false);
      }
    }

    void maintain() {
      if (Instant.now().isAfter(expirationDate.get())) {
        sessionClient.asyncCreateMultiplexedSession(
            new SessionConsumer() {
              @Override
              public void onSessionReady(SessionImpl session) {
                multiplexedSessionReference.set(
                    ApiFutures.immediateFuture(session.getSessionReference()));
                expirationDate.set(Instant.now().plus(Duration.ofDays(7)));
              }

              @Override
              public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
                // ignore any errors during re-creation of the multiplexed session. This means that
                // we continue to use the session that has passed its expiration date for now, and
                // that a new attempt at creating a new session will be done in 10 minutes from now.
              }
            });
      }
    }
  }
}
