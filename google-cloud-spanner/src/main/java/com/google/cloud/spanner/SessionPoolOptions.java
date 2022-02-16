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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
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
  private final long initialWaitForSessionTimeoutMillis;
  private final boolean autoDetectDialect;

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
    this.initialWaitForSessionTimeoutMillis = builder.initialWaitForSessionTimeoutMillis;
    this.loopFrequency = builder.loopFrequency;
    this.keepAliveIntervalMinutes = builder.keepAliveIntervalMinutes;
    this.removeInactiveSessionAfter = builder.removeInactiveSessionAfter;
    this.autoDetectDialect = builder.autoDetectDialect;
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
            this.initialWaitForSessionTimeoutMillis, other.initialWaitForSessionTimeoutMillis)
        && Objects.equals(this.loopFrequency, other.loopFrequency)
        && Objects.equals(this.keepAliveIntervalMinutes, other.keepAliveIntervalMinutes)
        && Objects.equals(this.removeInactiveSessionAfter, other.removeInactiveSessionAfter)
        && Objects.equals(this.autoDetectDialect, other.autoDetectDialect);
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
        this.initialWaitForSessionTimeoutMillis,
        this.loopFrequency,
        this.keepAliveIntervalMinutes,
        this.removeInactiveSessionAfter,
        this.autoDetectDialect);
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
    private long loopFrequency = 10 * 1000L;
    private int keepAliveIntervalMinutes = 30;
    private Duration removeInactiveSessionAfter = Duration.ofMinutes(55L);
    private boolean autoDetectDialect = false;

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
      this.loopFrequency = options.loopFrequency;
      this.keepAliveIntervalMinutes = options.keepAliveIntervalMinutes;
      this.removeInactiveSessionAfter = options.removeInactiveSessionAfter;
      this.autoDetectDialect = options.autoDetectDialect;
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
     */
    public Builder setBlockIfPoolExhausted() {
      this.actionOnExhaustion = ActionOnExhaustion.BLOCK;
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
