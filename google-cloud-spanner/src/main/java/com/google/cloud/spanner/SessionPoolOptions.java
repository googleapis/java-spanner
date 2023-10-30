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

import com.google.cloud.spanner.SessionPool.Clock;
import com.google.cloud.spanner.SessionPool.Position;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Locale;
import java.util.Objects;
import org.threeten.bp.Duration;

/** Options for the session pool used by {@code DatabaseClient}. */
public class SessionPoolOptions {
  // Default number of channels * 100.
  private static final int DEFAULT_MAX_SESSIONS = 400;
  private static final int DEFAULT_MIN_SESSIONS = 100;
  private static final int DEFAULT_INC_STEP = 25;
  private static final ActionOnExhaustion DEFAULT_ACTION = ActionOnExhaustion.BLOCK;
  private final int minSessions;
  private final int maxSessions;
  private final int incStep;
  /**
   * Use {@link #minSessions} instead to set the minimum number of sessions in the pool to maintain.
   * Creating a larger number of sessions during startup is relatively cheap as it is executed with
   * the BatchCreateSessions RPC.
   */
  @Deprecated private final int maxIdleSessions;
  /**
   * The session pool no longer prepares a fraction of the sessions with a read/write transaction.
   * This setting therefore does not have any meaning anymore, and may be removed in the future.
   */
  @Deprecated private final float writeSessionsFraction;

  private final ActionOnExhaustion actionOnExhaustion;
  private final long loopFrequency;
  private final int keepAliveIntervalMinutes;
  private final Duration removeInactiveSessionAfter;
  private final ActionOnSessionNotFound actionOnSessionNotFound;
  private final ActionOnSessionLeak actionOnSessionLeak;
  private final boolean trackStackTraceOfSessionCheckout;
  private final InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions;

  /**
   * Use {@link #acquireSessionTimeout} instead to specify the total duration to wait while
   * acquiring session for a transaction.
   */
  @Deprecated private final long initialWaitForSessionTimeoutMillis;

  private final boolean autoDetectDialect;
  private final Duration waitForMinSessions;
  private final Duration acquireSessionTimeout;
  private final Position releaseToPosition;

  /** Property for allowing mocking of session maintenance clock. */
  private final Clock poolMaintainerClock;

  private SessionPoolOptions(Builder builder) {
    // minSessions > maxSessions is only possible if the user has only set a value for maxSessions.
    // We allow that to prevent code that only sets a value for maxSessions to break if the
    // maxSessions value is less than the default for minSessions.
    this.minSessions = Math.min(builder.minSessions, builder.maxSessions);
    this.maxSessions = builder.maxSessions;
    this.incStep = builder.incStep;
    this.maxIdleSessions = builder.maxIdleSessions;
    this.writeSessionsFraction = builder.writeSessionsFraction;
    this.actionOnExhaustion = builder.actionOnExhaustion;
    this.actionOnSessionNotFound = builder.actionOnSessionNotFound;
    this.actionOnSessionLeak = builder.actionOnSessionLeak;
    this.trackStackTraceOfSessionCheckout = builder.trackStackTraceOfSessionCheckout;
    this.initialWaitForSessionTimeoutMillis = builder.initialWaitForSessionTimeoutMillis;
    this.loopFrequency = builder.loopFrequency;
    this.keepAliveIntervalMinutes = builder.keepAliveIntervalMinutes;
    this.removeInactiveSessionAfter = builder.removeInactiveSessionAfter;
    this.autoDetectDialect = builder.autoDetectDialect;
    this.waitForMinSessions = builder.waitForMinSessions;
    this.acquireSessionTimeout = builder.acquireSessionTimeout;
    this.releaseToPosition = builder.releaseToPosition;
    this.inactiveTransactionRemovalOptions = builder.inactiveTransactionRemovalOptions;
    this.poolMaintainerClock = builder.poolMaintainerClock;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SessionPoolOptions)) {
      return false;
    }
    SessionPoolOptions other = (SessionPoolOptions) o;
    return Objects.equals(this.minSessions, other.minSessions)
        && Objects.equals(this.maxSessions, other.maxSessions)
        && Objects.equals(this.incStep, other.incStep)
        && Objects.equals(this.maxIdleSessions, other.maxIdleSessions)
        && Objects.equals(this.writeSessionsFraction, other.writeSessionsFraction)
        && Objects.equals(this.actionOnExhaustion, other.actionOnExhaustion)
        && Objects.equals(this.actionOnSessionNotFound, other.actionOnSessionNotFound)
        && Objects.equals(this.actionOnSessionLeak, other.actionOnSessionLeak)
        && Objects.equals(
            this.trackStackTraceOfSessionCheckout, other.trackStackTraceOfSessionCheckout)
        && Objects.equals(
            this.initialWaitForSessionTimeoutMillis, other.initialWaitForSessionTimeoutMillis)
        && Objects.equals(this.loopFrequency, other.loopFrequency)
        && Objects.equals(this.keepAliveIntervalMinutes, other.keepAliveIntervalMinutes)
        && Objects.equals(this.removeInactiveSessionAfter, other.removeInactiveSessionAfter)
        && Objects.equals(this.autoDetectDialect, other.autoDetectDialect)
        && Objects.equals(this.waitForMinSessions, other.waitForMinSessions)
        && Objects.equals(this.acquireSessionTimeout, other.acquireSessionTimeout)
        && Objects.equals(this.releaseToPosition, other.releaseToPosition)
        && Objects.equals(
            this.inactiveTransactionRemovalOptions, other.inactiveTransactionRemovalOptions)
        && Objects.equals(this.poolMaintainerClock, other.poolMaintainerClock);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.minSessions,
        this.maxSessions,
        this.incStep,
        this.maxIdleSessions,
        this.writeSessionsFraction,
        this.actionOnExhaustion,
        this.actionOnSessionNotFound,
        this.actionOnSessionLeak,
        this.trackStackTraceOfSessionCheckout,
        this.initialWaitForSessionTimeoutMillis,
        this.loopFrequency,
        this.keepAliveIntervalMinutes,
        this.removeInactiveSessionAfter,
        this.autoDetectDialect,
        this.waitForMinSessions,
        this.acquireSessionTimeout,
        this.releaseToPosition,
        this.inactiveTransactionRemovalOptions,
        this.poolMaintainerClock);
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public int getMinSessions() {
    return minSessions;
  }

  public int getMaxSessions() {
    return maxSessions;
  }

  int getIncStep() {
    return incStep;
  }

  /**
   * @deprecated Use a higher value for {@link SessionPoolOptions.Builder#setMinSessions(int)}
   *     instead of setting this option.
   */
  @Deprecated
  public int getMaxIdleSessions() {
    return maxIdleSessions;
  }

  /**
   * @deprecated This value is no longer used. The session pool does not prepare any sessions for
   *     read/write transactions. Instead, a transaction will be started by including a
   *     BeginTransaction option with the first statement of a transaction. This method may be
   *     removed in a future release.
   */
  @Deprecated
  public float getWriteSessionsFraction() {
    return writeSessionsFraction;
  }

  long getLoopFrequency() {
    return loopFrequency;
  }

  public int getKeepAliveIntervalMinutes() {
    return keepAliveIntervalMinutes;
  }

  public Duration getRemoveInactiveSessionAfter() {
    return removeInactiveSessionAfter;
  }

  public boolean isFailIfPoolExhausted() {
    return actionOnExhaustion == ActionOnExhaustion.FAIL;
  }

  public boolean isBlockIfPoolExhausted() {
    return actionOnExhaustion == ActionOnExhaustion.BLOCK;
  }

  public boolean isAutoDetectDialect() {
    return autoDetectDialect;
  }

  InactiveTransactionRemovalOptions getInactiveTransactionRemovalOptions() {
    return inactiveTransactionRemovalOptions;
  }

  boolean closeInactiveTransactions() {
    return inactiveTransactionRemovalOptions.actionOnInactiveTransaction
        == ActionOnInactiveTransaction.CLOSE;
  }

  boolean warnAndCloseInactiveTransactions() {
    return inactiveTransactionRemovalOptions.actionOnInactiveTransaction
        == ActionOnInactiveTransaction.WARN_AND_CLOSE;
  }

  boolean warnInactiveTransactions() {
    return inactiveTransactionRemovalOptions.actionOnInactiveTransaction
        == ActionOnInactiveTransaction.WARN;
  }

  @VisibleForTesting
  long getInitialWaitForSessionTimeoutMillis() {
    return initialWaitForSessionTimeoutMillis;
  }

  @VisibleForTesting
  boolean isFailIfSessionNotFound() {
    return actionOnSessionNotFound == ActionOnSessionNotFound.FAIL;
  }

  @VisibleForTesting
  boolean isFailOnSessionLeak() {
    return actionOnSessionLeak == ActionOnSessionLeak.FAIL;
  }

  @VisibleForTesting
  Clock getPoolMaintainerClock() {
    return poolMaintainerClock;
  }

  public boolean isTrackStackTraceOfSessionCheckout() {
    return trackStackTraceOfSessionCheckout;
  }

  Duration getWaitForMinSessions() {
    return waitForMinSessions;
  }

  @VisibleForTesting
  Duration getAcquireSessionTimeout() {
    return acquireSessionTimeout;
  }

  Position getReleaseToPosition() {
    return releaseToPosition;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  private enum ActionOnExhaustion {
    BLOCK,
    FAIL,
  }

  private enum ActionOnSessionNotFound {
    RETRY,
    FAIL
  }

  private enum ActionOnSessionLeak {
    WARN,
    FAIL
  }

  @VisibleForTesting
  enum ActionOnInactiveTransaction {
    WARN,
    WARN_AND_CLOSE,
    CLOSE
  }

  /** Configuration options for task to clean up inactive transactions. */
  static class InactiveTransactionRemovalOptions {

    /** Option to set the behaviour when there are inactive transactions. */
    private ActionOnInactiveTransaction actionOnInactiveTransaction;

    /**
     * Frequency for closing inactive transactions. Between two consecutive task executions, it's
     * ensured that the duration is greater or equal to this duration.
     */
    private Duration executionFrequency;

    /**
     * Long-running transactions will be cleaned up if utilisation is greater than the below value.
     */
    private double usedSessionsRatioThreshold;

    /**
     * A transaction is considered to be idle if it has not been used for a duration greater than
     * the below value.
     */
    private Duration idleTimeThreshold;

    InactiveTransactionRemovalOptions(final Builder builder) {
      this.actionOnInactiveTransaction = builder.actionOnInactiveTransaction;
      this.idleTimeThreshold = builder.idleTimeThreshold;
      this.executionFrequency = builder.executionFrequency;
      this.usedSessionsRatioThreshold = builder.usedSessionsRatioThreshold;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof InactiveTransactionRemovalOptions)) {
        return false;
      }
      InactiveTransactionRemovalOptions other = (InactiveTransactionRemovalOptions) o;
      return Objects.equals(this.actionOnInactiveTransaction, other.actionOnInactiveTransaction)
          && Objects.equals(this.idleTimeThreshold, other.idleTimeThreshold)
          && Objects.equals(this.executionFrequency, other.executionFrequency)
          && Objects.equals(this.usedSessionsRatioThreshold, other.usedSessionsRatioThreshold);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          this.actionOnInactiveTransaction,
          this.idleTimeThreshold,
          this.executionFrequency,
          this.usedSessionsRatioThreshold);
    }

    Duration getExecutionFrequency() {
      return executionFrequency;
    }

    double getUsedSessionsRatioThreshold() {
      return usedSessionsRatioThreshold;
    }

    Duration getIdleTimeThreshold() {
      return idleTimeThreshold;
    }

    static InactiveTransactionRemovalOptions.Builder newBuilder() {
      return new Builder();
    }

    static class Builder {
      private ActionOnInactiveTransaction actionOnInactiveTransaction;
      private Duration executionFrequency = Duration.ofMinutes(2);
      private double usedSessionsRatioThreshold = 0.95;
      private Duration idleTimeThreshold = Duration.ofMinutes(60L);

      public Builder() {}

      InactiveTransactionRemovalOptions build() {
        validate();
        return new InactiveTransactionRemovalOptions(this);
      }

      private void validate() {
        Preconditions.checkArgument(
            executionFrequency.toMillis() > 0,
            "Execution frequency %s should be positive",
            executionFrequency.toMillis());
        Preconditions.checkArgument(
            idleTimeThreshold.toMillis() > 0,
            "Idle Time Threshold duration %s should be positive",
            idleTimeThreshold.toMillis());
      }

      @VisibleForTesting
      InactiveTransactionRemovalOptions.Builder setActionOnInactiveTransaction(
          final ActionOnInactiveTransaction actionOnInactiveTransaction) {
        this.actionOnInactiveTransaction = actionOnInactiveTransaction;
        return this;
      }

      @VisibleForTesting
      InactiveTransactionRemovalOptions.Builder setExecutionFrequency(
          final Duration executionFrequency) {
        this.executionFrequency = executionFrequency;
        return this;
      }

      @VisibleForTesting
      InactiveTransactionRemovalOptions.Builder setUsedSessionsRatioThreshold(
          final double usedSessionsRatioThreshold) {
        this.usedSessionsRatioThreshold = usedSessionsRatioThreshold;
        return this;
      }

      @VisibleForTesting
      InactiveTransactionRemovalOptions.Builder setIdleTimeThreshold(
          final Duration idleTimeThreshold) {
        this.idleTimeThreshold = idleTimeThreshold;
        return this;
      }
    }
  }

  /** Builder for creating SessionPoolOptions. */
  public static class Builder {
    private boolean minSessionsSet = false;
    private int minSessions = DEFAULT_MIN_SESSIONS;
    private int maxSessions = DEFAULT_MAX_SESSIONS;
    private int incStep = DEFAULT_INC_STEP;

    /** Set a higher value for {@link #minSessions} instead of using this field. */
    @Deprecated private int maxIdleSessions;

    /**
     * The session pool no longer prepares a fraction of the sessions with a read/write transaction.
     * This setting therefore does not have any meaning anymore, and may be removed in the future.
     */
    @Deprecated private float writeSessionsFraction = 0.2f;

    private ActionOnExhaustion actionOnExhaustion = DEFAULT_ACTION;
    private long initialWaitForSessionTimeoutMillis = 30_000L;
    private ActionOnSessionNotFound actionOnSessionNotFound = ActionOnSessionNotFound.RETRY;
    private ActionOnSessionLeak actionOnSessionLeak = ActionOnSessionLeak.WARN;
    /**
     * Capture the call stack of the thread that checked out a session of the pool. This will
     * pre-create a {@link com.google.cloud.spanner.SessionPool.LeakedSessionException} already when
     * a session is checked out. This can be disabled by users, for example if their monitoring
     * systems log the pre-created exception. If disabled, the {@link
     * com.google.cloud.spanner.SessionPool.LeakedSessionException} will only be created when an
     * actual session leak is detected. The stack trace of the exception will in that case not
     * contain the call stack of when the session was checked out.
     */
    private boolean trackStackTraceOfSessionCheckout = true;

    private InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder().build();
    private long loopFrequency = 10 * 1000L;
    private int keepAliveIntervalMinutes = 30;
    private Duration removeInactiveSessionAfter = Duration.ofMinutes(55L);
    private boolean autoDetectDialect = false;
    private Duration waitForMinSessions = Duration.ZERO;
    private Duration acquireSessionTimeout = Duration.ofSeconds(60);
    private Position releaseToPosition = getReleaseToPositionFromSystemProperty();

    private Clock poolMaintainerClock;

    private static Position getReleaseToPositionFromSystemProperty() {
      // NOTE: This System property is a beta feature. Support for it can be removed in the future.
      String key = "com.google.cloud.spanner.session_pool_release_to_position";
      if (System.getProperties().containsKey(key)) {
        try {
          return Position.valueOf(System.getProperty(key).toUpperCase(Locale.ENGLISH));
        } catch (Throwable ignore) {
          // fallthrough and return the default.
        }
      }
      return Position.FIRST;
    }

    public Builder() {}

    private Builder(SessionPoolOptions options) {
      this.minSessionsSet = true;
      this.minSessions = options.minSessions;
      this.maxSessions = options.maxSessions;
      this.incStep = options.incStep;
      this.maxIdleSessions = options.maxIdleSessions;
      this.writeSessionsFraction = options.writeSessionsFraction;
      this.actionOnExhaustion = options.actionOnExhaustion;
      this.initialWaitForSessionTimeoutMillis = options.initialWaitForSessionTimeoutMillis;
      this.actionOnSessionNotFound = options.actionOnSessionNotFound;
      this.actionOnSessionLeak = options.actionOnSessionLeak;
      this.trackStackTraceOfSessionCheckout = options.trackStackTraceOfSessionCheckout;
      this.loopFrequency = options.loopFrequency;
      this.keepAliveIntervalMinutes = options.keepAliveIntervalMinutes;
      this.removeInactiveSessionAfter = options.removeInactiveSessionAfter;
      this.autoDetectDialect = options.autoDetectDialect;
      this.waitForMinSessions = options.waitForMinSessions;
      this.acquireSessionTimeout = options.acquireSessionTimeout;
      this.inactiveTransactionRemovalOptions = options.inactiveTransactionRemovalOptions;
      this.poolMaintainerClock = options.poolMaintainerClock;
    }

    /**
     * Minimum number of sessions that this pool will always maintain. These will be created eagerly
     * in parallel. Defaults to 100.
     */
    public Builder setMinSessions(int minSessions) {
      Preconditions.checkArgument(minSessions >= 0, "minSessions must be >= 0");
      this.minSessionsSet = true;
      this.minSessions = minSessions;
      return this;
    }

    /**
     * Maximum number of sessions that this pool will have. If current numbers of sessions in the
     * pool is less than this and they are all busy, then a new session will be created for any new
     * operation. If current number of in use sessions is same as this and a new request comes, pool
     * can either block or fail. Defaults to 400.
     */
    public Builder setMaxSessions(int maxSessions) {
      Preconditions.checkArgument(maxSessions > 0, "maxSessions must be > 0");
      this.maxSessions = maxSessions;
      return this;
    }

    /**
     * Number of sessions to batch create when the pool needs at least one more session. Defaults to
     * 25.
     */
    Builder setIncStep(int incStep) {
      Preconditions.checkArgument(incStep > 0, "incStep must be > 0");
      this.incStep = incStep;
      return this;
    }

    /**
     * Maximum number of idle sessions that this pool will maintain. Pool will close any sessions
     * beyond this but making sure to always have at least as many sessions as specified by {@link
     * #setMinSessions}. To determine how many sessions are idle we look at maximum number of
     * sessions used concurrently over a window of time. Any sessions beyond that are idle. Defaults
     * to 0.
     *
     * @deprecated set a higher value for {@link #setMinSessions(int)} instead of using this
     *     configuration option. This option will be removed in a future release.
     */
    @Deprecated
    public Builder setMaxIdleSessions(int maxIdleSessions) {
      this.maxIdleSessions = maxIdleSessions;
      return this;
    }

    Builder setLoopFrequency(long loopFrequency) {
      this.loopFrequency = loopFrequency;
      return this;
    }

    Builder setInactiveTransactionRemovalOptions(
        InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions) {
      this.inactiveTransactionRemovalOptions = inactiveTransactionRemovalOptions;
      return this;
    }

    public Builder setRemoveInactiveSessionAfter(Duration duration) {
      this.removeInactiveSessionAfter = duration;
      return this;
    }

    /**
     * How frequently to keep alive idle sessions. This should be less than 60 since an idle session
     * is automatically closed after 60 minutes. Sessions will be kept alive by sending a dummy
     * query "Select 1". Default value is 30 minutes.
     */
    public Builder setKeepAliveIntervalMinutes(int intervalMinutes) {
      this.keepAliveIntervalMinutes = intervalMinutes;
      return this;
    }

    /**
     * If all sessions are in use and and {@code maxSessions} has been reached, fail the request by
     * throwing a {@link SpannerException} with the error code {@code RESOURCE_EXHAUSTED}. Default
     * behavior is to block the request.
     */
    public Builder setFailIfPoolExhausted() {
      this.actionOnExhaustion = ActionOnExhaustion.FAIL;
      return this;
    }

    /**
     * If all sessions are in use and there is no more room for creating new sessions, block for a
     * session to become available. Default behavior is same.
     *
     * <p>By default the requests are blocked for 60s and will fail with a `SpannerException` with
     * error code `ResourceExhausted` if this timeout is exceeded. If you wish to block for a
     * different period use the option {@link Builder#setAcquireSessionTimeout(Duration)} ()}
     */
    public Builder setBlockIfPoolExhausted() {
      this.actionOnExhaustion = ActionOnExhaustion.BLOCK;
      return this;
    }

    /**
     * If there are inactive transactions, log warning messages with the origin of such transactions
     * to aid debugging. A transaction is classified as inactive if it executes for more than a
     * system defined duration.
     *
     * <p>This option won't change the state of the transactions. It only generates warning logs
     * that can be used for debugging.
     *
     * @return this builder for chaining
     */
    Builder setWarnIfInactiveTransactions() {
      this.inactiveTransactionRemovalOptions =
          InactiveTransactionRemovalOptions.newBuilder()
              .setActionOnInactiveTransaction(ActionOnInactiveTransaction.WARN)
              .build();
      return this;
    }

    /**
     * If there are inactive transactions, release the resources consumed by such transactions. A
     * transaction is classified as inactive if it executes for more than a system defined duration.
     * The option would also produce necessary warning logs through which it can be debugged as to
     * what resources were released due to this option.
     *
     * <p>Use the option {@link Builder#setWarnIfInactiveTransactions()} if you only want to log
     * warnings about long-running transactions.
     *
     * @return this builder for chaining
     */
    Builder setWarnAndCloseIfInactiveTransactions() {
      this.inactiveTransactionRemovalOptions =
          InactiveTransactionRemovalOptions.newBuilder()
              .setActionOnInactiveTransaction(ActionOnInactiveTransaction.WARN_AND_CLOSE)
              .build();
      return this;
    }

    /**
     * If there are inactive transactions, release the resources consumed by such transactions. A
     * transaction is classified as inactive if it executes for more than a system defined duration.
     *
     * <p>Use the option {@link Builder#setWarnIfInactiveTransactions()} if you only want to log
     * warnings about long-running sessions.
     *
     * <p>Use the option {@link Builder#setWarnAndCloseIfInactiveTransactions()} if you want to log
     * warnings along with closing the long-running transactions.
     *
     * @return this builder for chaining
     */
    @VisibleForTesting
    Builder setCloseIfInactiveTransactions() {
      this.inactiveTransactionRemovalOptions =
          InactiveTransactionRemovalOptions.newBuilder()
              .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
              .build();
      return this;
    }

    @VisibleForTesting
    Builder setPoolMaintainerClock(Clock poolMaintainerClock) {
      this.poolMaintainerClock = poolMaintainerClock;
      return this;
    }

    /**
     * Sets whether the client should automatically execute a background query to detect the dialect
     * that is used by the database or not. Set this option to true if you do not know what the
     * dialect of the database will be.
     *
     * <p>Note that you can always call {@link DatabaseClient#getDialect()} to get the dialect of a
     * database regardless of this setting, but by setting this to true, the value will be
     * pre-populated and cached in the client.
     *
     * @param autoDetectDialect Whether the client should automatically execute a background query
     *     to detect the dialect of the underlying database
     * @return this builder for chaining
     */
    public Builder setAutoDetectDialect(boolean autoDetectDialect) {
      this.autoDetectDialect = autoDetectDialect;
      return this;
    }

    /**
     * The initial number of milliseconds to wait for a session to become available when one is
     * requested. The session pool will keep retrying to get a session, and the timeout will be
     * doubled for each new attempt. The default is 30 seconds.
     */
    @VisibleForTesting
    Builder setInitialWaitForSessionTimeoutMillis(long timeout) {
      this.initialWaitForSessionTimeoutMillis = timeout;
      return this;
    }

    /**
     * If a session has been invalidated by the server, the {@link SessionPool} will by default
     * retry the session. Set this option to throw an exception instead of retrying.
     */
    @VisibleForTesting
    Builder setFailIfSessionNotFound() {
      this.actionOnSessionNotFound = ActionOnSessionNotFound.FAIL;
      return this;
    }

    @VisibleForTesting
    Builder setFailOnSessionLeak() {
      this.actionOnSessionLeak = ActionOnSessionLeak.FAIL;
      return this;
    }

    /**
     * Sets whether the session pool should capture the call stack trace when a session is checked
     * out of the pool. This will internally prepare a {@link
     * com.google.cloud.spanner.SessionPool.LeakedSessionException} that will only be thrown if the
     * session is actually leaked. This makes it easier to debug session leaks, as the stack trace
     * of the thread that checked out the session will be available in the exception.
     *
     * <p>Some monitoring tools might log these exceptions even though they are not thrown. This
     * option can be used to suppress the creation and logging of these exceptions.
     */
    public Builder setTrackStackTraceOfSessionCheckout(boolean trackStackTraceOfSessionCheckout) {
      this.trackStackTraceOfSessionCheckout = trackStackTraceOfSessionCheckout;
      return this;
    }

    /**
     * @deprecated This configuration value is no longer in use. The session pool does not prepare
     *     any sessions for read/write transactions. Instead, a transaction will automatically be
     *     started by the first statement that is executed by a transaction by including a
     *     BeginTransaction option with that statement.
     *     <p>This method may be removed in a future release.
     */
    public Builder setWriteSessionsFraction(float writeSessionsFraction) {
      this.writeSessionsFraction = writeSessionsFraction;
      return this;
    }

    /**
     * If greater than zero, waits for the session pool to have at least {@link
     * SessionPoolOptions#minSessions} before returning the database client to the caller. Note that
     * this check is only done during the session pool creation. This is usually done asynchronously
     * in order to provide the client back to the caller as soon as possible. We don't recommend
     * using this option unless you are executing benchmarks and want to guarantee the session pool
     * has min sessions in the pool before continuing.
     *
     * <p>Defaults to zero (initialization is done asynchronously).
     */
    public Builder setWaitForMinSessions(Duration waitForMinSessions) {
      this.waitForMinSessions = waitForMinSessions;
      return this;
    }

    /**
     * If greater than zero, we wait for said duration when no sessions are available in the {@link
     * SessionPool}. The default is a 60s timeout. Set the value to null to disable the timeout.
     */
    public Builder setAcquireSessionTimeout(Duration acquireSessionTimeout) {
      try {
        if (acquireSessionTimeout != null) {
          Preconditions.checkArgument(
              acquireSessionTimeout.toMillis() > 0,
              "acquireSessionTimeout should be greater than 0 ns");
        }
      } catch (ArithmeticException ex) {
        throw new IllegalArgumentException(
            "acquireSessionTimeout in millis should be lesser than Long.MAX_VALUE");
      }
      this.acquireSessionTimeout = acquireSessionTimeout;
      return this;
    }

    Builder setReleaseToPosition(Position releaseToPosition) {
      this.releaseToPosition = Preconditions.checkNotNull(releaseToPosition);
      return this;
    }

    /** Build a SessionPoolOption object */
    public SessionPoolOptions build() {
      validate();
      return new SessionPoolOptions(this);
    }

    private void validate() {
      if (minSessionsSet) {
        Preconditions.checkArgument(
            maxSessions >= minSessions,
            "Min sessions(%s) must be <= max sessions(%s)",
            minSessions,
            maxSessions);
      }
      Preconditions.checkArgument(
          keepAliveIntervalMinutes < 60, "Keep alive interval should be less than" + "60 minutes");
    }
  }
}
