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

import static com.google.cloud.spanner.SessionImpl.NO_CHANNEL_HINT;
import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerException;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SessionPool.SessionPoolTransactionRunner;
import com.google.cloud.spanner.SpannerException.ResourceNotFoundException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.spanner.v1.BatchWriteResponse;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.RequestOptions;
import com.google.spanner.v1.Transaction;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link TransactionRunner} that automatically handles "UNIMPLEMENTED" errors with the message
 * "Transaction type read_write not supported with multiplexed sessions" by switching from a
 * multiplexed session to a regular session and then restarts the transaction.
 */
class MultiplexedSessionTransactionRunner implements TransactionRunner {
  private final SessionPool sessionPool;
  private final TransactionRunnerImpl transactionRunnerForMultiplexedSession;
  private SessionPoolTransactionRunner transactionRunnerForRegularSession;
  private final TransactionOption[] options;
  private boolean isUsingMultiplexedSession = true;

  public MultiplexedSessionTransactionRunner(
      SessionImpl multiplexedSession, SessionPool sessionPool, TransactionOption... options) {
    this.sessionPool = sessionPool;
    this.transactionRunnerForMultiplexedSession =
        new TransactionRunnerImpl(
            multiplexedSession, options); // Uses multiplexed session initially
    multiplexedSession.setActive(this.transactionRunnerForMultiplexedSession);
    this.options = options;
  }

  private TransactionRunner getRunner() {
    if (this.isUsingMultiplexedSession) {
      return this.transactionRunnerForMultiplexedSession;
    } else {
      if (this.transactionRunnerForRegularSession == null) {
        this.transactionRunnerForRegularSession =
            new SessionPoolTransactionRunner<>(
                sessionPool.getSession(),
                sessionPool.getPooledSessionReplacementHandler(),
                options);
      }
      return this.transactionRunnerForRegularSession;
    }
  }

  @Override
  public <T> T run(TransactionCallable<T> callable) {
    while (true) {
      try {
        return getRunner().run(callable);
      } catch (SpannerException e) {
        if (e.getErrorCode() == ErrorCode.UNIMPLEMENTED
            && verifyUnimplementedErrorMessageForRWMux(e)) {
          this.isUsingMultiplexedSession = false; // Fallback to regular session
        } else {
          throw e; // Other errors propagate
        }
      }
    }
  }

  @Override
  public Timestamp getCommitTimestamp() {
    return getRunner().getCommitTimestamp();
  }

  @Override
  public CommitResponse getCommitResponse() {
    return getRunner().getCommitResponse();
  }

  @Override
  public TransactionRunner allowNestedTransaction() {
    getRunner().allowNestedTransaction();
    return this;
  }

  private boolean verifyUnimplementedErrorMessageForRWMux(SpannerException spannerException) {
    if (spannerException.getCause() == null) {
      return false;
    }
    if (spannerException.getCause().getMessage() == null) {
      return false;
    }
    return spannerException
        .getCause()
        .getMessage()
        .contains("Transaction type read_write not supported with multiplexed sessions");
  }
}

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

    private final int singleUseChannelHint;

    private boolean done;
    private final SessionPool pool;

    MultiplexedSessionTransaction(
        MultiplexedSessionDatabaseClient client,
        ISpan span,
        SessionReference sessionReference,
        int singleUseChannelHint,
        boolean singleUse) {
      this(client, span, sessionReference, singleUseChannelHint, singleUse, null);
    }

    MultiplexedSessionTransaction(
        MultiplexedSessionDatabaseClient client,
        ISpan span,
        SessionReference sessionReference,
        int singleUseChannelHint,
        boolean singleUse,
        SessionPool pool) {
      super(client.sessionClient.getSpanner(), sessionReference, singleUseChannelHint);
      this.client = client;
      this.singleUse = singleUse;
      this.singleUseChannelHint = singleUseChannelHint;
      this.client.numSessionsAcquired.incrementAndGet();
      this.pool = pool;
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
      }
      // Mark multiplexed sessions for RW as unimplemented and fall back to regular sessions if
      // UNIMPLEMENTED with error message "Transaction type read_write not supported with
      // multiplexed sessions" is returned.
      this.client.maybeMarkUnimplementedForRW(spannerException);
      // Mark multiplexed sessions for Partitioned Ops as unimplemented and fall back to regular
      // sessions if
      // UNIMPLEMENTED with error message "Partitioned operations are not supported with multiplexed
      // sessions".
      this.client.maybeMarkUnimplementedForPartitionedOps(spannerException);
    }

    @Override
    void onReadDone() {
      // This method is called whenever a ResultSet that was returned by this transaction is closed.
      // Close the active transaction if this is a single-use transaction. This ensures that the
      // active span is ended.
      if (this.singleUse && getActiveTransaction() != null) {
        getActiveTransaction().close();
        setActive(null);
        if (this.singleUseChannelHint != NO_CHANNEL_HINT) {
          this.client.channelUsage.clear(this.singleUseChannelHint);
        }
        this.client.numCurrentSingleUseTransactions.decrementAndGet();
      }
    }

    @Override
    public CommitResponse writeAtLeastOnceWithOptions(
        Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
      CommitResponse response = super.writeAtLeastOnceWithOptions(mutations, options);
      onTransactionDone();
      return response;
    }

    @Override
    public TransactionRunner readWriteTransaction(TransactionOption... options) {
      return new MultiplexedSessionTransactionRunner(this, pool, options);
    }

    @Override
    void onTransactionDone() {
      boolean markedDone = false;
      synchronized (this) {
        if (!this.done) {
          this.done = true;
          markedDone = true;
        }
      }
      if (markedDone) {
        client.numSessionsReleased.incrementAndGet();
      }
    }

    @Override
    public void close() {
      // no-op, we don't want to delete the multiplexed session.
    }
  }

  /**
   * Keeps track of which channels have been 'given' to single-use transactions for a given Spanner
   * instance.
   */
  private static final Map<SpannerImpl, BitSet> CHANNEL_USAGE = new HashMap<>();

  private final BitSet channelUsage;

  private final int numChannels;

  /**
   * The number of single-use read-only transactions currently running on this multiplexed session.
   */
  private final AtomicInteger numCurrentSingleUseTransactions = new AtomicInteger();

  private boolean isClosed;

  /** The duration before we try to replace the multiplexed session. The default is 7 days. */
  private final Duration sessionExpirationDuration;

  private final SessionClient sessionClient;

  private final TraceWrapper tracer;

  /** The current multiplexed session that is used by this client. */
  private final AtomicReference<ApiFuture<SessionReference>> multiplexedSessionReference;

  /**
   * The Transaction response returned by the BeginTransaction request with read-write when a
   * multiplexed session is created during client initialization.
   */
  private final SettableApiFuture<Transaction> readWriteBeginTransactionReferenceFuture;

  /** The expiration date/time of the current multiplexed session. */
  private final AtomicReference<Instant> expirationDate;

  /**
   * The maintainer runs every 10 minutes to check whether the multiplexed session should be
   * refreshed.
   */
  private final MultiplexedSessionMaintainer maintainer;

  /**
   * If a {@link DatabaseNotFoundException} or {@link InstanceNotFoundException} is returned by the
   * server, then we set this field to mark the client as invalid.
   */
  private final AtomicReference<ResourceNotFoundException> resourceNotFoundException =
      new AtomicReference<>();

  private final AtomicLong numSessionsAcquired = new AtomicLong();

  private final AtomicLong numSessionsReleased = new AtomicLong();

  /**
   * This flag is set to true if the server return UNIMPLEMENTED when we try to create a multiplexed
   * session. TODO: Remove once this is guaranteed to be available.
   */
  private final AtomicBoolean unimplemented = new AtomicBoolean(false);

  /**
   * This flag is set to true if the server return UNIMPLEMENTED when a read-write transaction is
   * executed on a multiplexed session. TODO: Remove once this is guaranteed to be available.
   */
  @VisibleForTesting final AtomicBoolean unimplementedForRW = new AtomicBoolean(false);

  /**
   * This flag is set to true if the server return UNIMPLEMENTED when partitioned transaction is
   * executed on a multiplexed session. TODO: Remove once this is guaranteed to be available.
   */
  @VisibleForTesting final AtomicBoolean unimplementedForPartitionedOps = new AtomicBoolean(false);

  /**
   * Lock to synchronize session creation attempts. This ensures that only one thread attempts to
   * create a session at a time, while other threads wait for the result.
   */
  private final ReentrantLock creationLock = new ReentrantLock();

  /**
   * Flag to indicate if session creation is currently in progress. Used to allow waiting threads to
   * know if they should wait or trigger a new creation attempt.
   */
  private final AtomicBoolean creationInProgress = new AtomicBoolean(false);

  /**
   * Latch that waiting threads block on while session creation is in progress. This is replaced
   * with a new latch after each creation attempt completes.
   */
  private volatile CountDownLatch creationLatch = new CountDownLatch(1);

  /**
   * The last error that occurred during session creation. This is stored temporarily and cleared
   * when a session is successfully created. Unlike the previous implementation, this error is not
   * cached forever - subsequent requests will retry session creation.
   */
  @VisibleForTesting final AtomicReference<Throwable> lastCreationError = new AtomicReference<>();

  private SessionPool pool;

  MultiplexedSessionDatabaseClient(SessionClient sessionClient) {
    this(sessionClient, Clock.systemUTC());
  }

  @VisibleForTesting
  MultiplexedSessionDatabaseClient(SessionClient sessionClient, Clock clock) {
    this.numChannels = sessionClient.getSpanner().getOptions().getNumChannels();
    synchronized (CHANNEL_USAGE) {
      CHANNEL_USAGE.putIfAbsent(sessionClient.getSpanner(), new BitSet(numChannels));
      this.channelUsage = CHANNEL_USAGE.get(sessionClient.getSpanner());
    }
    this.sessionExpirationDuration =
        Duration.ofMillis(
            sessionClient
                .getSpanner()
                .getOptions()
                .getSessionPoolOptions()
                .getMultiplexedSessionMaintenanceDuration()
                .toMillis());
    // Initialize the expiration date to the current time + 7 days to avoid unnecessary null checks.
    // The time difference with the actual creation is small enough that it does not matter.
    this.expirationDate = new AtomicReference<>(Instant.now().plus(this.sessionExpirationDuration));
    this.sessionClient = sessionClient;
    this.maintainer = new MultiplexedSessionMaintainer(clock);
    this.tracer = sessionClient.getSpanner().getTracer();
    this.readWriteBeginTransactionReferenceFuture = SettableApiFuture.create();
    // Initialize with null - session will be set when created successfully
    this.multiplexedSessionReference = new AtomicReference<>(null);
    // Mark creation as in progress for the initial attempt
    this.creationInProgress.set(true);
    this.sessionClient.asyncCreateMultiplexedSession(
        new SessionConsumer() {
          @Override
          public void onSessionReady(SessionImpl session) {
            onSessionCreatedSuccessfully(session);

            // initiate a begin transaction request to verify if read-write transactions are
            // supported using multiplexed sessions.
            if (sessionClient
                    .getSpanner()
                    .getOptions()
                    .getSessionPoolOptions()
                    .getUseMultiplexedSessionForRW()
                && !sessionClient
                    .getSpanner()
                    .getOptions()
                    .getSessionPoolOptions()
                    .getSkipVerifyBeginTransactionForMuxRW()) {
              verifyBeginTransactionWithRWOnMultiplexedSessionAsync(session.getName());
            }
            if (sessionClient
                .getSpanner()
                .getOptions()
                .getSessionPoolOptions()
                .isAutoDetectDialect()) {
              MAINTAINER_SERVICE.submit(() -> getDialect());
            }
          }

          @Override
          public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
            onSessionCreationFailed(t);
          }
        });
    maybeWaitForInitialSessionCreation(
        sessionClient.getSpanner().getOptions().getSessionPoolOptions());
  }

  void setPool(SessionPool pool) {
    this.pool = pool;
  }

  /**
   * Called when a multiplexed session is successfully created. This method stores the session
   * reference, clears any previous error, starts the maintainer, and notifies waiting threads.
   */
  private void onSessionCreatedSuccessfully(SessionImpl session) {
    creationLock.lock();
    try {
      multiplexedSessionReference.set(ApiFutures.immediateFuture(session.getSessionReference()));
      lastCreationError.set(null);
      expirationDate.set(Instant.now().plus(sessionExpirationDuration));
      creationInProgress.set(false);
      // Start the maintainer if not already started
      if (!maintainer.isStarted()) {
        maintainer.start();
      }
      // Notify all waiting threads
      creationLatch.countDown();
      creationLatch = new CountDownLatch(1);
    } finally {
      creationLock.unlock();
    }
  }

  /**
   * Called when multiplexed session creation fails. This method stores the error temporarily,
   * notifies waiting threads, and starts the maintainer for retry (unless UNIMPLEMENTED).
   */
  private void onSessionCreationFailed(Throwable t) {
    creationLock.lock();
    try {
      // Mark multiplexed sessions as unimplemented and fall back to regular sessions if
      // UNIMPLEMENTED is returned.
      maybeMarkUnimplemented(t);
      lastCreationError.set(t);
      creationInProgress.set(false);
      // Notify all waiting threads
      creationLatch.countDown();
      creationLatch = new CountDownLatch(1);
      // Start the maintainer even on failure (except for UNIMPLEMENTED) so it can retry
      if (!unimplemented.get() && !maintainer.isStarted()) {
        maintainer.start();
      }
    } finally {
      creationLock.unlock();
    }
  }

  /**
   * Waits for the initial session creation to complete if configured to do so. This method handles
   * the case where the session creation is still in progress or has failed.
   */
  private void maybeWaitForInitialSessionCreation(SessionPoolOptions sessionPoolOptions) {
    Duration waitDuration = sessionPoolOptions.getWaitForMinSessions();
    if (waitDuration != null && !waitDuration.isZero()) {
      long timeoutMillis = waitDuration.toMillis();
      try {
        if (!creationLatch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.DEADLINE_EXCEEDED,
              "Timed out after waiting " + timeoutMillis + "ms for multiplexed session creation");
        }
        // Check if there was an error during creation
        Throwable error = lastCreationError.get();
        if (error != null && multiplexedSessionReference.get() == null) {
          throw SpannerExceptionFactory.asSpannerException(error);
        }
      } catch (InterruptedException interruptedException) {
        throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
      }
    }
  }

  /**
   * Gets or creates a multiplexed session reference. This method implements retry-on-access
   * semantics: if no session exists and creation is not in progress, it triggers a new creation
   * attempt. If creation is in progress, it waits for the result.
   *
   * @return the session reference
   * @throws SpannerException if session creation fails
   */
  @VisibleForTesting
  SessionReference getOrCreateSessionReference() {
    // Fast path: session already exists
    ApiFuture<SessionReference> sessionFuture = multiplexedSessionReference.get();
    if (sessionFuture != null) {
      try {
        return sessionFuture.get();
      } catch (ExecutionException | InterruptedException e) {
        // This shouldn't happen with immediateFuture, but handle it anyway
        throw SpannerExceptionFactory.asSpannerException(e.getCause() != null ? e.getCause() : e);
      }
    }

    // Check if UNIMPLEMENTED - don't retry in this case
    if (unimplemented.get()) {
      Throwable error = lastCreationError.get();
      if (error != null) {
        throw SpannerExceptionFactory.asSpannerException(error);
      }
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.UNIMPLEMENTED, "Multiplexed sessions are not supported");
    }

    // Try to acquire the lock for creation
    if (creationLock.tryLock()) {
      try {
        // Double-check after acquiring lock
        sessionFuture = multiplexedSessionReference.get();
        if (sessionFuture != null) {
          try {
            return sessionFuture.get();
          } catch (ExecutionException | InterruptedException e) {
            throw SpannerExceptionFactory.asSpannerException(
                e.getCause() != null ? e.getCause() : e);
          }
        }

        // Check if creation is already in progress
        if (creationInProgress.get()) {
          // Wait for the ongoing creation to complete
          creationLock.unlock();
          return waitForSessionCreation();
        }

        // Start a new creation attempt
        creationInProgress.set(true);
        CountDownLatch currentLatch = creationLatch;
        creationLock.unlock();

        // Trigger async session creation
        sessionClient.asyncCreateMultiplexedSession(
            new SessionConsumer() {
              @Override
              public void onSessionReady(SessionImpl session) {
                onSessionCreatedSuccessfully(session);
              }

              @Override
              public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
                onSessionCreationFailed(t);
              }
            });

        // Wait for creation to complete
        try {
          currentLatch.await();
        } catch (InterruptedException e) {
          throw SpannerExceptionFactory.propagateInterrupt(e);
        }

        // Check result
        sessionFuture = multiplexedSessionReference.get();
        if (sessionFuture != null) {
          try {
            return sessionFuture.get();
          } catch (ExecutionException | InterruptedException e) {
            throw SpannerExceptionFactory.asSpannerException(
                e.getCause() != null ? e.getCause() : e);
          }
        }

        // Creation failed
        Throwable error = lastCreationError.get();
        if (error != null) {
          throw SpannerExceptionFactory.asSpannerException(error);
        }
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INTERNAL, "Failed to create multiplexed session");
      } finally {
        if (creationLock.isHeldByCurrentThread()) {
          creationLock.unlock();
        }
      }
    } else {
      // Another thread is creating, wait for it
      return waitForSessionCreation();
    }
  }

  /** Waits for an ongoing session creation to complete and returns the result. */
  private SessionReference waitForSessionCreation() {
    try {
      creationLatch.await();
    } catch (InterruptedException e) {
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }

    ApiFuture<SessionReference> sessionFuture = multiplexedSessionReference.get();
    if (sessionFuture != null) {
      try {
        return sessionFuture.get();
      } catch (ExecutionException | InterruptedException e) {
        throw SpannerExceptionFactory.asSpannerException(e.getCause() != null ? e.getCause() : e);
      }
    }

    Throwable error = lastCreationError.get();
    if (error != null) {
      throw SpannerExceptionFactory.asSpannerException(error);
    }
    throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.INTERNAL, "Failed to create multiplexed session");
  }

  private void maybeMarkUnimplemented(Throwable t) {
    SpannerException spannerException = SpannerExceptionFactory.asSpannerException(t);
    if (spannerException.getErrorCode() == ErrorCode.UNIMPLEMENTED) {
      unimplemented.set(true);
    }
  }

  private void maybeMarkUnimplementedForRW(SpannerException spannerException) {
    if (spannerException.getErrorCode() == ErrorCode.UNIMPLEMENTED
        && verifyErrorMessage(
            spannerException,
            "Transaction type read_write not supported with multiplexed sessions")) {
      unimplementedForRW.set(true);
    }
  }

  boolean maybeMarkUnimplementedForPartitionedOps(SpannerException spannerException) {
    if (spannerException.getErrorCode() == ErrorCode.UNIMPLEMENTED
        && verifyErrorMessage(
            spannerException,
            "Transaction type partitioned_dml not supported with multiplexed sessions")) {
      unimplementedForPartitionedOps.set(true);
      return true;
    }
    return false;
  }

  static boolean verifyErrorMessage(SpannerException spannerException, String message) {
    if (spannerException.getCause() == null) {
      return false;
    }
    if (spannerException.getCause().getMessage() == null) {
      return false;
    }
    return spannerException.getCause().getMessage().contains(message);
  }

  private void verifyBeginTransactionWithRWOnMultiplexedSessionAsync(String sessionName) {
    // TODO: Remove once this is guaranteed to be available.
    // annotate the explict BeginTransactionRequest with a transaction tag
    // "multiplexed-rw-background-begin-txn" to avoid storing this request on mock spanner.
    // this is to safeguard other mock spanner tests whose BeginTransaction request count will
    // otherwise increase by 1. Modifying the unit tests do not seem valid since this code is
    // temporary and will be removed once the read-write on multiplexed session looks stable at
    // backend.
    BeginTransactionRequest.Builder requestBuilder =
        BeginTransactionRequest.newBuilder()
            .setSession(sessionName)
            .setOptions(
                SessionImpl.createReadWriteTransactionOptions(
                    Options.fromTransactionOptions(), /* previousTransactionId= */ null))
            .setRequestOptions(
                RequestOptions.newBuilder()
                    .setTransactionTag("multiplexed-rw-background-begin-txn")
                    .build());
    final BeginTransactionRequest request = requestBuilder.build();
    final ApiFuture<Transaction> requestFuture;
    requestFuture =
        sessionClient
            .getSpanner()
            .getRpc()
            .beginTransactionAsync(request, /* options= */ null, /* routeToLeader= */ true);
    requestFuture.addListener(
        () -> {
          try {
            Transaction txn = requestFuture.get();
            if (txn.getId().isEmpty()) {
              throw newSpannerException(
                  ErrorCode.INTERNAL, "Missing id in transaction\n" + sessionName);
            }
            readWriteBeginTransactionReferenceFuture.set(txn);
          } catch (Exception e) {
            SpannerException spannerException = SpannerExceptionFactory.asSpannerException(e);
            // Mark multiplexed sessions for RW as unimplemented and fall back to regular sessions
            // if UNIMPLEMENTED is returned.
            maybeMarkUnimplementedForRW(spannerException);
            readWriteBeginTransactionReferenceFuture.setException(e);
          }
        },
        MoreExecutors.directExecutor());
  }

  boolean isValid() {
    return resourceNotFoundException.get() == null;
  }

  AtomicLong getNumSessionsAcquired() {
    return this.numSessionsAcquired;
  }

  AtomicLong getNumSessionsReleased() {
    return this.numSessionsReleased;
  }

  boolean isMultiplexedSessionsSupported() {
    return !this.unimplemented.get();
  }

  boolean isMultiplexedSessionsForRWSupported() {
    return !this.unimplementedForRW.get();
  }

  boolean isMultiplexedSessionsForPartitionedOpsSupported() {
    return !this.unimplementedForPartitionedOps.get();
  }

  void close() {
    synchronized (this) {
      if (!this.isClosed) {
        this.isClosed = true;
        this.maintainer.stop();
      }
    }
  }

  @VisibleForTesting
  MultiplexedSessionMaintainer getMaintainer() {
    return this.maintainer;
  }

  @VisibleForTesting
  SessionReference getCurrentSessionReference() {
    return getOrCreateSessionReference();
  }

  @VisibleForTesting
  Transaction getReadWriteBeginTransactionReference() {
    try {
      return this.readWriteBeginTransactionReferenceFuture.get();
    } catch (ExecutionException executionException) {
      throw SpannerExceptionFactory.asSpannerException(executionException.getCause());
    } catch (InterruptedException interruptedException) {
      throw SpannerExceptionFactory.propagateInterrupt(interruptedException);
    }
  }

  /**
   * Returns true if the multiplexed session has been created. This client can be used before the
   * session has been created, and will in that case use a delayed transaction that contains a
   * future reference to the multiplexed session. The delayed transaction will block at the first
   * actual statement that is being executed (e.g. the first query that is sent to Spanner).
   */
  private boolean isMultiplexedSessionCreated() {
    ApiFuture<SessionReference> future = multiplexedSessionReference.get();
    return future != null && future.isDone();
  }

  private DatabaseClient createMultiplexedSessionTransaction(boolean singleUse) {
    Preconditions.checkState(!isClosed, "This client has been closed");
    return isMultiplexedSessionCreated()
        ? createDirectMultiplexedSessionTransaction(singleUse)
        : createDelayedMultiplexSessionTransaction();
  }

  private MultiplexedSessionTransaction createDirectMultiplexedSessionTransaction(
      boolean singleUse) {
    // Use getOrCreateSessionReference() which implements retry-on-access semantics.
    // This will trigger a new creation attempt if the session doesn't exist yet.
    SessionReference sessionReference = getOrCreateSessionReference();
    return new MultiplexedSessionTransaction(
        this,
        tracer.getCurrentSpan(),
        sessionReference,
        singleUse ? getSingleUseChannelHint() : NO_CHANNEL_HINT,
        singleUse,
        this.pool);
  }

  private DelayedMultiplexedSessionTransaction createDelayedMultiplexSessionTransaction() {
    // For delayed transactions, we create a future that will be resolved when
    // getOrCreateSessionReference()
    // is called. This allows the transaction to be created immediately while the session creation
    // may still be in progress or may need to be retried.
    SettableApiFuture<SessionReference> delayedFuture = SettableApiFuture.create();
    MAINTAINER_SERVICE.submit(
        () -> {
          try {
            SessionReference sessionRef = getOrCreateSessionReference();
            delayedFuture.set(sessionRef);
          } catch (Throwable t) {
            delayedFuture.setException(t);
          }
        });
    return new DelayedMultiplexedSessionTransaction(
        this, tracer.getCurrentSpan(), delayedFuture, this.pool);
  }

  private int getSingleUseChannelHint() {
    if (this.numCurrentSingleUseTransactions.incrementAndGet() > this.numChannels) {
      return NO_CHANNEL_HINT;
    }
    synchronized (this.channelUsage) {
      // Get the first unused channel.
      int channel = this.channelUsage.nextClearBit(/* fromIndex= */ 0);
      // BitSet returns an index larger than its original size if all the bits are set.
      // This then means that all channels have already been assigned to single-use transactions,
      // and that we should not use a specific channel, but rather pick a random one.
      if (channel == this.numChannels) {
        return NO_CHANNEL_HINT;
      }
      this.channelUsage.set(channel);
      return channel;
    }
  }

  private final AbstractLazyInitializer<Dialect> dialectSupplier =
      new AbstractLazyInitializer<Dialect>() {
        @Override
        protected Dialect initialize() {
          try (ResultSet dialectResultSet =
              singleUse().executeQuery(SessionPool.DETERMINE_DIALECT_STATEMENT)) {
            if (dialectResultSet.next()) {
              return Dialect.fromName(dialectResultSet.getString(0));
            }
          }
          // This should not really happen, but it is the safest fallback value.
          return Dialect.GOOGLE_STANDARD_SQL;
        }
      };

  @Override
  public Dialect getDialect() {
    try {
      return dialectSupplier.get();
    } catch (Exception exception) {
      throw SpannerExceptionFactory.asSpannerException(exception);
    }
  }

  Future<Dialect> getDialectAsync() {
    try {
      return MAINTAINER_SERVICE.submit(dialectSupplier::get);
    } catch (Exception exception) {
      throw SpannerExceptionFactory.asSpannerException(exception);
    }
  }

  @Override
  public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
    return createMultiplexedSessionTransaction(/* singleUse= */ false).write(mutations);
  }

  @Override
  public CommitResponse writeWithOptions(
      final Iterable<Mutation> mutations, final TransactionOption... options)
      throws SpannerException {
    return createMultiplexedSessionTransaction(/* singleUse= */ false)
        .writeWithOptions(mutations, options);
  }

  @Override
  public CommitResponse writeAtLeastOnceWithOptions(
      Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
    return createMultiplexedSessionTransaction(/* singleUse= */ true)
        .writeAtLeastOnceWithOptions(mutations, options);
  }

  @Override
  public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
      Iterable<MutationGroup> mutationGroups, TransactionOption... options)
      throws SpannerException {
    return createMultiplexedSessionTransaction(/* singleUse= */ true)
        .batchWriteAtLeastOnce(mutationGroups, options);
  }

  @Override
  public ReadContext singleUse() {
    return createMultiplexedSessionTransaction(/* singleUse= */ true).singleUse();
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    return createMultiplexedSessionTransaction(/* singleUse= */ true).singleUse(bound);
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    return createMultiplexedSessionTransaction(/* singleUse= */ true)
        .singleUseReadOnlyTransaction();
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    return createMultiplexedSessionTransaction(/* singleUse= */ true)
        .singleUseReadOnlyTransaction(bound);
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    return createMultiplexedSessionTransaction(/* singleUse= */ false).readOnlyTransaction();
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    return createMultiplexedSessionTransaction(/* singleUse= */ false).readOnlyTransaction(bound);
  }

  @Override
  public TransactionRunner readWriteTransaction(TransactionOption... options) {
    return createMultiplexedSessionTransaction(/* singleUse= */ false)
        .readWriteTransaction(options);
  }

  @Override
  public TransactionManager transactionManager(TransactionOption... options) {
    return createMultiplexedSessionTransaction(/* singleUse= */ false).transactionManager(options);
  }

  @Override
  public AsyncRunner runAsync(TransactionOption... options) {
    return createMultiplexedSessionTransaction(/* singleUse= */ false).runAsync(options);
  }

  @Override
  public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
    return createMultiplexedSessionTransaction(/* singleUse= */ false)
        .transactionManagerAsync(options);
  }

  @Override
  public long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
    return createMultiplexedSessionTransaction(/* singleUse= */ false)
        .executePartitionedUpdate(stmt, options);
  }

  /**
   * It is enough with one executor to maintain the multiplexed sessions in all the clients, as they
   * do not need to be updated often, and the maintenance task is light. The core pool size is set
   * to 1 to prevent continuous creating and tearing down threads, and to avoid high CPU usage when
   * running on Java 8 due to <a href="https://bugs.openjdk.org/browse/JDK-8129861">
   * https://bugs.openjdk.org/browse/JDK-8129861</a>.
   */
  private static final ScheduledExecutorService MAINTAINER_SERVICE =
      Executors.newScheduledThreadPool(
          /* corePoolSize= */ 1,
          ThreadFactoryUtil.createVirtualOrPlatformDaemonThreadFactory(
              "multiplexed-session-maintainer", /* tryVirtual= */ false));

  final class MultiplexedSessionMaintainer {
    private final Clock clock;

    private volatile ScheduledFuture<?> scheduledFuture;
    private volatile boolean started = false;

    MultiplexedSessionMaintainer(Clock clock) {
      this.clock = clock;
    }

    boolean isStarted() {
      return started;
    }

    void start() {
      if (started) {
        return;
      }
      started = true;
      // Schedule the maintainer to run once every ten minutes (by default).
      long loopFrequencyMillis =
          MultiplexedSessionDatabaseClient.this
              .sessionClient
              .getSpanner()
              .getOptions()
              .getSessionPoolOptions()
              .getMultiplexedSessionMaintenanceLoopFrequency()
              .toMillis();
      this.scheduledFuture =
          MAINTAINER_SERVICE.scheduleAtFixedRate(
              this::maintain, loopFrequencyMillis, loopFrequencyMillis, TimeUnit.MILLISECONDS);
    }

    void stop() {
      if (this.scheduledFuture != null) {
        this.scheduledFuture.cancel(false);
      }
    }

    void maintain() {
      // Check if session needs to be created (either null or expired)
      boolean sessionNeedsCreation =
          multiplexedSessionReference.get() == null
              || clock.instant().isAfter(expirationDate.get());

      if (sessionNeedsCreation && !creationInProgress.get()) {
        // Only attempt creation if not already in progress
        creationLock.lock();
        try {
          // Double-check after acquiring lock
          if (creationInProgress.get()) {
            return;
          }
          creationInProgress.set(true);
        } finally {
          creationLock.unlock();
        }

        sessionClient.asyncCreateMultiplexedSession(
            new SessionConsumer() {
              @Override
              public void onSessionReady(SessionImpl session) {
                onSessionCreatedSuccessfully(session);
              }

              @Override
              public void onSessionCreateFailure(Throwable t, int createFailureForSessionCount) {
                // Store the error but allow future retries.
                // The only exception is UNIMPLEMENTED which falls back to regular sessions.
                onSessionCreationFailed(t);
              }
            });
      }
    }
  }
}
