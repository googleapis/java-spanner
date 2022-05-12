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

package com.google.cloud.spanner.connection;

import com.google.api.core.ApiFuture;
import com.google.api.core.ListenableFutureToApiFuture;
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.ReadOnlyStalenessUtil.DurationValueGetter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.threeten.bp.temporal.ChronoUnit;

/**
 * {@link StatementExecutor} is responsible for executing statements on a {@link Connection}.
 * Statements are executed using a separate executor to allow timeouts and cancellation of
 * statements.
 */
class StatementExecutor {

  /** Simple holder class for statement timeout that allows us to pass the value by reference. */
  static class StatementTimeout {
    /**
     * Only {@link TimeUnit#NANOSECONDS}, {@link TimeUnit#MICROSECONDS}, {@link
     * TimeUnit#MILLISECONDS} and {@link TimeUnit#SECONDS} may be used to specify a statement
     * timeout.
     */
    static boolean isValidTimeoutUnit(TimeUnit unit) {
      return unit == TimeUnit.NANOSECONDS
          || unit == TimeUnit.MICROSECONDS
          || unit == TimeUnit.MILLISECONDS
          || unit == TimeUnit.SECONDS;
    }

    /** The statement timeout. */
    private volatile Duration duration = null;

    /**
     * Does this {@link StatementTimeout} have an actual timeout (i.e. it will eventually timeout).
     */
    boolean hasTimeout() {
      return duration != null;
    }

    void clearTimeoutValue() {
      this.duration = null;
    }

    void setTimeoutValue(long timeout, TimeUnit unit) {
      Preconditions.checkArgument(timeout > 0L);
      Preconditions.checkArgument(isValidTimeoutUnit(unit));
      this.duration = ReadOnlyStalenessUtil.createDuration(timeout, unit);
    }

    long getTimeoutValue(TimeUnit unit) {
      Preconditions.checkArgument(isValidTimeoutUnit(unit));
      return duration == null ? 0L : ReadOnlyStalenessUtil.durationToUnits(duration, unit);
    }

    /**
     * Returns the {@link TimeUnit} with the least precision that could be used to represent this
     * {@link StatementTimeout} without loss of precision.
     */
    TimeUnit getAppropriateTimeUnit() {
      ConnectionPreconditions.checkState(
          duration != null, "This StatementTimeout has no timeout value");
      return ReadOnlyStalenessUtil.getAppropriateTimeUnit(
          new DurationValueGetter() {
            @Override
            public long getDuration(TimeUnit unit) {
              return StatementTimeout.this.getTimeoutValue(unit);
            }

            @Override
            public boolean hasDuration() {
              return StatementTimeout.this.hasTimeout();
            }
          });
    }

    org.threeten.bp.Duration asDuration() {
      if (!hasTimeout()) {
        return org.threeten.bp.Duration.ZERO;
      }
      TimeUnit unit = getAppropriateTimeUnit();
      switch (unit) {
        case DAYS:
          return org.threeten.bp.Duration.ofDays(getTimeoutValue(unit));
        case HOURS:
          return org.threeten.bp.Duration.ofHours(getTimeoutValue(unit));
        case MICROSECONDS:
          return org.threeten.bp.Duration.of(getTimeoutValue(unit), ChronoUnit.MICROS);
        case MILLISECONDS:
          return org.threeten.bp.Duration.ofMillis(getTimeoutValue(unit));
        case MINUTES:
          return org.threeten.bp.Duration.ofMinutes(getTimeoutValue(unit));
        case NANOSECONDS:
          return org.threeten.bp.Duration.ofNanos(getTimeoutValue(unit));
        case SECONDS:
          return org.threeten.bp.Duration.ofSeconds(getTimeoutValue(unit));
        default:
          throw new IllegalStateException("invalid time unit: " + unit);
      }
    }
  }

  /**
   * Use a {@link ThreadFactory} that produces daemon threads and sets recognizable name on the
   * threads.
   */
  private static final ThreadFactory THREAD_FACTORY =
      new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("connection-executor-%d")
          .setThreadFactory(MoreExecutors.platformThreadFactory())
          .build();

  /** Creates an {@link ExecutorService} for a {@link StatementExecutor}. */
  private static ListeningExecutorService createExecutorService() {
    return MoreExecutors.listeningDecorator(
        new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), THREAD_FACTORY));
  }

  private ListeningExecutorService executor = createExecutorService();

  /**
   * Interceptors that should be invoked before or after a statement is executed can be registered
   * for a connection. This are added to this list. The interceptors are intended for test usage.
   */
  private final List<StatementExecutionInterceptor> interceptors;

  @VisibleForTesting
  StatementExecutor() {
    this.interceptors = Collections.emptyList();
  }

  StatementExecutor(List<StatementExecutionInterceptor> interceptors) {
    this.interceptors = Collections.unmodifiableList(interceptors);
  }

  void shutdown() {
    executor.shutdown();
  }

  void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    executor.awaitTermination(timeout, unit);
  }

  /**
   * Shutdown this executor now and do not wait for any statement that is being executed to finish.
   */
  List<Runnable> shutdownNow() {
    return executor.shutdownNow();
  }

  /** Execute a statement on this {@link StatementExecutor}. */
  <T> ApiFuture<T> submit(Callable<T> callable) {
    return new ListenableFutureToApiFuture<>(executor.submit(callable));
  }

  /**
   * Invoke the interceptors that have been registered for this {@link StatementExecutor} for the
   * given step.
   */
  void invokeInterceptors(
      ParsedStatement statement, StatementExecutionStep step, UnitOfWork transaction) {
    for (StatementExecutionInterceptor interceptor : interceptors) {
      interceptor.intercept(statement, step, transaction);
    }
  }
}
