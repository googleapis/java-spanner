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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.cloud.spanner.SessionPool.MultiplexedSession;
import com.google.cloud.spanner.SessionPool.MultiplexedSessionConsumer;
import com.google.cloud.spanner.SessionPool.MultiplexedSessionFuture;
import com.google.cloud.spanner.SessionPool.SessionFuture;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.threeten.bp.Duration;

/**
 * Tests for {@link com.google.cloud.spanner.SessionPool.MultiplexedSession} component within the
 * {@link SessionPool} class.
 */
public class MultiplexedSessionPoolTest extends BaseSessionPoolTest {

  @Mock SpannerImpl client;
  @Mock SessionClient sessionClient;
  @Mock SpannerOptions spannerOptions;
  private final DatabaseId db = DatabaseId.of("projects/p/instances/i/databases/unused");
  private final TraceWrapper tracer =
      new TraceWrapper(Tracing.getTracer(), OpenTelemetry.noop().getTracer(""));
  SessionPoolOptions options;
  SessionPool pool;

  private SessionPool createPool() {
    return SessionPool.createPool(
        options,
        new TestExecutorFactory(),
        client.getSessionClient(db),
        tracer,
        OpenTelemetry.noop());
  }

  @Before
  public void setUp() {
    initMocks(this);
    SpannerOptions.resetActiveTracingFramework();
    SpannerOptions.enableOpenTelemetryTraces();
    when(client.getOptions()).thenReturn(spannerOptions);
    when(client.getSessionClient(db)).thenReturn(sessionClient);
    when(sessionClient.getSpanner()).thenReturn(client);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    when(spannerOptions.getDatabaseRole()).thenReturn("role");
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(2)
            .setMaxSessions(2)
            .setUseMultiplexedSession(true)
            .build();
  }

  @Test
  public void testGetMultiplexedSession_whenClosedPool_assertSessionReturned() {
    pool = createPool();
    assertTrue(pool.isValid());
    closePoolWithStacktrace();

    // checking out a multiplexed session does not throw error even if pool is closed
    MultiplexedSessionFuture multiplexedSessionFuture =
        (MultiplexedSessionFuture) pool.getMultiplexedSessionWithFallback();
    assertNotNull(multiplexedSessionFuture);

    // checking out a regular session throws error.
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> pool.getSession());
    assertThat(e.getCause()).isInstanceOf(ClosedException.class);
    StringWriter sw = new StringWriter();
    e.getCause().printStackTrace(new PrintWriter(sw));
    assertThat(sw.toString()).contains("closePoolWithStacktrace");
  }

  private void closePoolWithStacktrace() {
    pool.closeAsync(new SpannerImpl.ClosedException());
  }

  @Test
  public void testSynchronousSessionInitialization_hasAtMostOneMultiplexedSession() {
    setupMockMultiplexedSessionCreation();

    pool = createPool();
    pool.waitOnMultiplexedSession();

    assertEquals(1, pool.totalMultiplexedSessions());
    Session session1 = pool.getMultiplexedSessionWithFallback().get();
    Session session2 = pool.getMultiplexedSessionWithFallback().get();
    assertEquals(session1, session2);

    session2.close();
    session1.close();
    verify(sessionClient, times(1)).createMultiplexedSession(any(MultiplexedSessionConsumer.class));
  }

  @Test
  public void testGetMultiplexedSession_whenSessionCreationFailed_assertErrorForWaiters() {
    doAnswer(
            invocation -> {
              MultiplexedSessionConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionConsumer.class);
              consumer.onSessionCreateFailure(
                  SpannerExceptionFactory.newSpannerException(ErrorCode.INTERNAL, ""), 1);
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionConsumer.class));
    options =
        options
            .toBuilder()
            .setMinSessions(2)
            .setUseMultiplexedSession(true)
            .setAcquireSessionTimeout(
                Duration.ofMillis(50)) // block for a max of 50 ms for session to be available
            .build();
    pool = createPool();

    // create 5 requests which require a session
    for (int i = 0; i < 5; i++) {
      SessionFuture future = pool.getMultiplexedSessionWithFallback();
      SpannerException e = assertThrows(SpannerException.class, () -> future.get());
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
    }
    // assert that all 5 requests failed with exception
    assertEquals(5, pool.getNumMultiplexedSessionWaiterTimeouts());
    assertEquals(0, pool.getNumWaiterTimeouts());
    assertEquals(0, pool.getNumberOfSessionsInPool());
  }

  @Test
  public void
      testGetMultiplexedSession_whenSessionCreationSucceededOnSecondAttempt_assertWaitersAreUnblocked() {
    doAnswer(
            invocation -> {
              MultiplexedSessionConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionConsumer.class);
              consumer.onSessionCreateFailure(
                  SpannerExceptionFactory.newSpannerException(ErrorCode.INTERNAL, ""), 1);
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionConsumer.class));
    options =
        options
            .toBuilder()
            .setMinSessions(2)
            .setUseMultiplexedSession(true)
            .setAcquireSessionTimeout(
                Duration.ofHours(
                    1)) // use a high value since the waiters will be eventually unblocked
            .build();
    pool = createPool();
    assertEquals(0, pool.totalMultiplexedSessions());
    assertEquals(0, pool.totalSessions());

    // create 5 requests which require a session
    List<SessionFuture> futures = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      // below request will be added to a waiter queue since no session is available
      futures.add(pool.getMultiplexedSessionWithFallback());
    }
    // assert that all 5 requests are present in waiter queue
    assertEquals(5, pool.getNumMultiplexedSessionWaiters());
    assertEquals(0, pool.getNumMultiplexedSessionWaiterTimeouts());

    // now mock a successful session creation
    // run a background pool maintainer which should now create a multiplexed session
    // this should unblock the waiters
    setupMockMultiplexedSessionCreation();
    pool.poolMaintainer.maintainPool();
    assertEquals(1, pool.totalMultiplexedSessions());

    for (SessionFuture future : futures) {
      MultiplexedSession multiplexedSession = (MultiplexedSession) future.get();
      assertNotNull(multiplexedSession);
    }
    assertEquals(1, pool.totalMultiplexedSessions());
    assertEquals(0, pool.getNumMultiplexedSessionWaiterTimeouts());
    assertEquals(0, pool.getNumWaiterTimeouts());
    assertEquals(0, pool.getNumberOfSessionsInPool());
  }

  private void setupMockMultiplexedSessionCreation() {
    doAnswer(
            invocation -> {
              MultiplexedSessionConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionConsumer.class);
              consumer.onSessionReady(mockSession());
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionConsumer.class));
  }
}
