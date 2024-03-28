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

import com.google.cloud.spanner.SessionPool.MultiplexedSessionConsumer;
import com.google.cloud.spanner.SessionPool.MultiplexedSessionFuture;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
  public void sessionCreation() {
    setupMockMultiplexedSessionCreation();
    pool = createPool();
    try (MultiplexedSessionFuture sessionFuture =
        (MultiplexedSessionFuture) pool.getMultiplexedSessionWithFallback()) {
      assertNotNull(sessionFuture);
    }
  }

  @Test
  public void testSynchronousPoolInit_hasAtMostOneMultiplexedSession() {
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
                Duration.ofMillis(50)) // block for a max of 100 ms for session to be available
            .build();
    pool = createPool();
    SpannerException e =
        assertThrows(SpannerException.class, () -> pool.getMultiplexedSessionWithFallback().get());
    assertEquals(ErrorCode.RESOURCE_EXHAUSTED, e.getErrorCode());
  }

  private void setupMockMultiplexedSessionCreation() {
    doAnswer(
            invocation -> {
              MultiplexedSessionConsumer consumer =
                  invocation.getArgument(0, MultiplexedSessionConsumer.class);
              consumer.onSessionReady(mockMultiplexedSession());
              return null;
            })
        .when(sessionClient)
        .createMultiplexedSession(any(MultiplexedSessionConsumer.class));
  }
}
