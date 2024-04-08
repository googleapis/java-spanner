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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.SessionPool.MultiplexedSession;
import com.google.cloud.spanner.SessionPool.MultiplexedSessionInitializationConsumer;
import com.google.cloud.spanner.SessionPool.MultiplexedSessionMaintainerConsumer;
import com.google.cloud.spanner.SessionPool.Position;
import com.google.cloud.spanner.SessionPool.SessionFutureWrapper;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

@RunWith(JUnit4.class)
public class MultiplexedSessionMaintainerTest extends BaseSessionPoolTest {

  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private @Mock SpannerImpl client;
  private @Mock SessionClient sessionClient;
  private @Mock SpannerOptions spannerOptions;
  private DatabaseId db = DatabaseId.of("projects/p/instances/i/databases/unused");
  private SessionPoolOptions options;
  private FakeClock clock = new FakeClock();
  private List<MultiplexedSession> multiplexedSessionsRemoved = new ArrayList<>();

  @Before
  public void setUp() {
    initMocks(this);
    when(client.getOptions()).thenReturn(spannerOptions);
    when(client.getSessionClient(db)).thenReturn(sessionClient);
    when(sessionClient.getSpanner()).thenReturn(client);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    when(spannerOptions.getDatabaseRole()).thenReturn("role");
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxIdleSessions(1)
            .setMaxSessions(5)
            .setIncStep(1)
            .setKeepAliveIntervalMinutes(2)
            .setUseMultiplexedSession(true)
            .build();
    multiplexedSessionsRemoved.clear();
  }

  @Test
  public void testMaintainMultiplexedSession_whenNewSessionCreated_assertThatStaleSessionIsRemoved()
      throws InterruptedException {
    doAnswer(
            invocation -> {
              MultiplexedSessionInitializationConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionInitializationConsumer.class);
              ReadContext mockContext = mock(ReadContext.class);
              Timestamp timestamp =
                  Timestamp.ofTimeSecondsAndNanos(
                      Instant.ofEpochMilli(clock.currentTimeMillis.get()).getEpochSecond(), 0);
              consumer.onSessionReady(
                  setupMockSession(
                      buildMockMultiplexedSession(mockContext, timestamp.toProto()), mockContext));
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionInitializationConsumer.class));
    doAnswer(
            invocation -> {
              MultiplexedSessionMaintainerConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionMaintainerConsumer.class);
              ReadContext mockContext = mock(ReadContext.class);
              Timestamp timestamp =
                  Timestamp.ofTimeSecondsAndNanos(
                      Instant.ofEpochMilli(clock.currentTimeMillis.get()).getEpochSecond(), 0);
              consumer.onSessionReady(
                  setupMockSession(
                      buildMockMultiplexedSession(mockContext, timestamp.toProto()), mockContext));
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionMaintainerConsumer.class));

    SessionPool pool = createPool();

    // Run one maintenance loop.
    SessionFutureWrapper session1 = pool.getMultiplexedSessionWithFallback();
    runMaintenanceLoop(clock, pool, 1);
    assertTrue(multiplexedSessionsRemoved.isEmpty());

    // Advance clock by 8 days
    clock.currentTimeMillis.addAndGet(Duration.ofDays(8).toMillis());

    // Run second maintenance loop. the first session would now be stale since it has now existed
    // for more than 7 days.
    runMaintenanceLoop(clock, pool, 1);
    SessionFutureWrapper session2 = pool.getMultiplexedSessionWithFallback();
    assertNotEquals(session1.get().getName(), session2.get().getName());
    assertEquals(1, multiplexedSessionsRemoved.size());
    assertTrue(multiplexedSessionsRemoved.contains(session1.get().get()));

    // Advance clock by 8 days
    clock.currentTimeMillis.addAndGet(Duration.ofDays(8).toMillis());

    // Run third maintenance loop. the second session would now be stale since it has now existed
    // for more than 7 days
    runMaintenanceLoop(clock, pool, 1);

    SessionFutureWrapper session3 = pool.getMultiplexedSessionWithFallback();
    assertNotEquals(session2.get().getName(), session3.get().getName());
    assertEquals(2, multiplexedSessionsRemoved.size());
    assertTrue(multiplexedSessionsRemoved.contains(session2.get().get()));
  }

  @Test
  public void
      testMaintainMultiplexedSession_whenMultiplexedSessionNotStale_assertThatSessionIsNotRemoved() {
    doAnswer(
            invocation -> {
              MultiplexedSessionInitializationConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionInitializationConsumer.class);
              ReadContext mockContext = mock(ReadContext.class);
              Timestamp timestamp =
                  Timestamp.ofTimeSecondsAndNanos(
                      Instant.ofEpochMilli(clock.currentTimeMillis.get()).getEpochSecond(), 0);
              consumer.onSessionReady(
                  setupMockSession(
                      buildMockMultiplexedSession(mockContext, timestamp.toProto()), mockContext));
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionInitializationConsumer.class));
    SessionPool pool = createPool();

    // Run one maintenance loop.
    SessionFutureWrapper session1 = pool.getMultiplexedSessionWithFallback();
    runMaintenanceLoop(clock, pool, 1);
    assertTrue(multiplexedSessionsRemoved.isEmpty());

    // Advance clock by 4 days
    clock.currentTimeMillis.addAndGet(Duration.ofDays(4).toMillis());
    // Run one maintenance loop. the first session would not be stale yet since it has now existed
    // for less than 7 days.
    runMaintenanceLoop(clock, pool, 1);
    SessionFutureWrapper session2 = pool.getMultiplexedSessionWithFallback();
    assertTrue(multiplexedSessionsRemoved.isEmpty());
    assertEquals(session1.get().getName(), session2.get().getName());
  }

  @Test
  public void
      testMaintainMultiplexedSession_whenMultiplexedSessionCreationFailed_testRetryAfterDelay() {
    doAnswer(
            invocation -> {
              MultiplexedSessionInitializationConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionInitializationConsumer.class);
              ReadContext mockContext = mock(ReadContext.class);
              Timestamp timestamp =
                  Timestamp.ofTimeSecondsAndNanos(
                      Instant.ofEpochMilli(clock.currentTimeMillis.get()).getEpochSecond(), 0);
              consumer.onSessionReady(
                  setupMockSession(
                      buildMockMultiplexedSession(mockContext, timestamp.toProto()), mockContext));
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionInitializationConsumer.class));
    doAnswer(
            invocation -> {
              MultiplexedSessionMaintainerConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionMaintainerConsumer.class);
              consumer.onSessionCreateFailure(
                  SpannerExceptionFactory.newSpannerException(ErrorCode.DEADLINE_EXCEEDED, ""), 1);
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionMaintainerConsumer.class));
    SessionPool pool = createPool();

    // Advance clock by 8 days
    clock.currentTimeMillis.addAndGet(Duration.ofDays(8).toMillis());

    // Run one maintenance loop. Attempt replacing stale session should fail.
    SessionFutureWrapper session1 = pool.getMultiplexedSessionWithFallback();
    runMaintenanceLoop(clock, pool, 1);
    assertTrue(multiplexedSessionsRemoved.isEmpty());
    verify(sessionClient, times(1))
        .createMultiplexedSession(any(MultiplexedSessionMaintainerConsumer.class));

    // Advance clock by 10s and now mock session creation to be successful.
    clock.currentTimeMillis.addAndGet(Duration.ofSeconds(10).toMillis());
    doAnswer(
            invocation -> {
              MultiplexedSessionMaintainerConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionMaintainerConsumer.class);
              ReadContext mockContext = mock(ReadContext.class);
              Timestamp timestamp =
                  Timestamp.ofTimeSecondsAndNanos(
                      Instant.ofEpochMilli(clock.currentTimeMillis.get()).getEpochSecond(), 0);
              consumer.onSessionReady(
                  setupMockSession(
                      buildMockMultiplexedSession(mockContext, timestamp.toProto()), mockContext));
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionMaintainerConsumer.class));
    // Run one maintenance loop. Attempt should be ignored as it has not been 10 minutes since last
    // attempt.
    runMaintenanceLoop(clock, pool, 1);
    SessionFutureWrapper session2 = pool.getMultiplexedSessionWithFallback();
    assertTrue(multiplexedSessionsRemoved.isEmpty());
    assertEquals(session1.get().getName(), session2.get().getName());
    verify(sessionClient, times(1))
        .createMultiplexedSession(any(MultiplexedSessionMaintainerConsumer.class));

    // Advance clock by 15 minutes
    clock.currentTimeMillis.addAndGet(Duration.ofMinutes(15).toMillis());
    // Run one maintenance loop. Attempt should succeed since its already more than 10 minutes since
    // the last attempt.
    runMaintenanceLoop(clock, pool, 1);
    SessionFutureWrapper session3 = pool.getMultiplexedSessionWithFallback();
    assertTrue(multiplexedSessionsRemoved.contains(session1.get().get()));
    assertNotEquals(session1.get().getName(), session3.get().getName());
    verify(sessionClient, times(2))
        .createMultiplexedSession(any(MultiplexedSessionMaintainerConsumer.class));
  }

  private SessionImpl setupMockSession(final SessionImpl session, final ReadContext mockContext) {
    final ResultSet mockResult = mock(ResultSet.class);
    when(mockContext.executeQuery(any(Statement.class))).thenAnswer(invocation -> mockResult);
    when(mockResult.next()).thenReturn(true);
    return session;
  }

  private SessionPool createPool() {
    // Allow sessions to be added to the head of the pool in all cases in this test, as it is
    // otherwise impossible to know which session exactly is getting pinged at what point in time.
    SessionPool pool =
        SessionPool.createPool(
            options,
            new TestExecutorFactory(),
            client.getSessionClient(db),
            clock,
            Position.FIRST,
            new TraceWrapper(Tracing.getTracer(), OpenTelemetry.noop().getTracer("")),
            OpenTelemetry.noop());
    pool.multiplexedSessionRemovedListener =
        input -> {
          multiplexedSessionsRemoved.add(input);
          return null;
        };
    return pool;
  }
}
