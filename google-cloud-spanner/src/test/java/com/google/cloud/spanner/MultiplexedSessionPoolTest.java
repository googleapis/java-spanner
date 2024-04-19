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

import com.google.cloud.spanner.SessionPool.MultiplexedSessionFuture;
import com.google.cloud.spanner.SessionPool.MultiplexedSessionInitializationConsumer;
import com.google.cloud.spanner.SessionPool.SessionFutureWrapper;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Assume;
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
    when(spannerOptions.getSessionPoolOptions()).thenReturn(options);
    Assume.assumeTrue(options.getUseMultiplexedSession());
  }

  @Test
  public void testGetMultiplexedSession_whenSessionInitializationSucceeded_assertSessionReturned() {
    setupMockMultiplexedSessionCreation();

    pool = createPool();
    assertTrue(pool.isValid());

    // create 5 requests which require a session
    for (int i = 0; i < 5; i++) {
      // checking out a multiplexed session
      SessionFutureWrapper multiplexedSessionFuture = pool.getMultiplexedSessionWithFallback();
      assertNotNull(multiplexedSessionFuture.get());
    }
    verify(sessionClient, times(1))
        .asyncCreateMultiplexedSession(any(MultiplexedSessionInitializationConsumer.class));
  }

  @Test
  public void testGetMultiplexedSession_whenClosedPool_assertSessionReturned() {
    setupMockMultiplexedSessionCreation();

    pool = createPool();
    assertTrue(pool.isValid());
    closePoolWithStacktrace();

    // checking out a multiplexed session does not throw error even if pool is closed
    MultiplexedSessionFuture multiplexedSessionFuture =
        (MultiplexedSessionFuture) pool.getMultiplexedSessionWithFallback().get();
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
  public void testGetMultiplexedSession_whenSessionCreationFailed_assertErrorForWaiters() {
    doAnswer(
            invocation -> {
              MultiplexedSessionInitializationConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionInitializationConsumer.class);
              consumer.onSessionCreateFailure(
                  SpannerExceptionFactory.newSpannerException(ErrorCode.DEADLINE_EXCEEDED, ""), 1);
              return null;
            })
        .when(sessionClient)
        .asyncCreateMultiplexedSession(any(MultiplexedSessionInitializationConsumer.class));
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
      SpannerException e =
          assertThrows(
              SpannerException.class, () -> pool.getMultiplexedSessionWithFallback().get().get());
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
    }
    // assert that all 5 requests failed with exception
    assertEquals(0, pool.getNumWaiterTimeouts());
    assertEquals(0, pool.getNumberOfSessionsInPool());
  }

  private void setupMockMultiplexedSessionCreation() {
    doAnswer(
            invocation -> {
              MultiplexedSessionInitializationConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionInitializationConsumer.class);
              consumer.onSessionReady(mockSession());
              return null;
            })
        .when(sessionClient)
        .asyncCreateMultiplexedSession(any(MultiplexedSessionInitializationConsumer.class));
  }
}
