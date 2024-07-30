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

import static com.google.cloud.spanner.MetricRegistryConstants.GET_SESSION_TIMEOUTS;
import static com.google.cloud.spanner.MetricRegistryConstants.IS_MULTIPLEXED_KEY;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_ALLOWED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.MAX_IN_USE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.METRIC_PREFIX;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_ACQUIRED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_IN_USE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_READ_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_RELEASED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_AVAILABLE;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_BEING_PREPARED;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_IN_POOL;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_IN_USE;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_WRITE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_DEFAULT_LABEL_VALUES;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS_WITH_TYPE;
import static com.google.cloud.spanner.SpannerOptionsTest.runWithSystemProperty;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.MetricRegistryTestUtils.FakeMetricRegistry;
import com.google.cloud.spanner.MetricRegistryTestUtils.MetricsRecord;
import com.google.cloud.spanner.MetricRegistryTestUtils.PointWithFunction;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SessionPool.PooledSession;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.Position;
import com.google.cloud.spanner.SessionPool.SessionConsumerImpl;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.spi.v1.SpannerRpc.ResultStreamConsumer;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CommitResponse;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.RollbackRequest;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.MetricRegistry;
import io.opencensus.metrics.Metrics;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoUnit;

/** Tests for SessionPool that mock out the underlying stub. */
@RunWith(Parameterized.class)
public class SessionPoolTest extends BaseSessionPoolTest {
  private static Level originalLogLevel;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  @Parameter public int minSessions;

  @Mock SpannerImpl client;
  @Mock SessionClient sessionClient;
  @Mock SpannerOptions spannerOptions;
  DatabaseId db = DatabaseId.of("projects/p/instances/i/databases/unused");
  SessionPool pool;
  SessionPoolOptions options;
  private String sessionName = String.format("%s/sessions/s", db.getName());
  private String TEST_DATABASE_ROLE = "my-role";

  private final TraceWrapper tracer =
      new TraceWrapper(Tracing.getTracer(), OpenTelemetry.noop().getTracer(""), false);

  @Parameters(name = "min sessions = {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{0}, {1}});
  }

  private SessionPool createPool() {
    return SessionPool.createPool(
        options,
        new TestExecutorFactory(),
        client.getSessionClient(db),
        tracer,
        OpenTelemetry.noop());
  }

  private SessionPool createPool(Clock clock) {
    return SessionPool.createPool(
        options,
        new TestExecutorFactory(),
        client.getSessionClient(db),
        clock,
        Position.RANDOM,
        tracer,
        OpenTelemetry.noop());
  }

  private SessionPool createPool(
      Clock clock, MetricRegistry metricRegistry, List<LabelValue> labelValues) {
    return SessionPool.createPool(
        options,
        TEST_DATABASE_ROLE,
        new TestExecutorFactory(),
        client.getSessionClient(db),
        clock,
        Position.RANDOM,
        metricRegistry,
        tracer,
        labelValues,
        OpenTelemetry.noop(),
        null,
        new AtomicLong(),
        new AtomicLong());
  }

  private SessionPool createPool(
      Clock clock,
      MetricRegistry metricRegistry,
      List<LabelValue> labelValues,
      OpenTelemetry openTelemetry,
      Attributes attributes) {
    return SessionPool.createPool(
        options,
        TEST_DATABASE_ROLE,
        new TestExecutorFactory(),
        client.getSessionClient(db),
        clock,
        Position.RANDOM,
        metricRegistry,
        tracer,
        labelValues,
        openTelemetry,
        attributes,
        new AtomicLong(),
        new AtomicLong());
  }

  @BeforeClass
  public static void disableLogging() {
    Logger logger = Logger.getLogger("");
    originalLogLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
  }

  @AfterClass
  public static void resetLogging() {
    Logger logger = Logger.getLogger("");
    logger.setLevel(originalLogLevel);
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
            .setMinSessions(minSessions)
            .setMaxSessions(2)
            .setIncStep(1)
            .setBlockIfPoolExhausted()
            .build();
  }

  private void setupMockSessionCreation() {
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    int sessionCount = invocation.getArgument(0, Integer.class);
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    for (int i = 0; i < sessionCount; i++) {
                      consumer.onSessionReady(mockSession());
                    }
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(
            Mockito.anyInt(), Mockito.anyBoolean(), any(SessionConsumer.class));
    doAnswer(
            invocation ->
                executor.submit(
                    () -> {
                      SessionConsumer consumer = invocation.getArgument(0, SessionConsumer.class);
                      consumer.onSessionReady(mockMultiplexedSession());
                    }))
        .when(sessionClient)
        .asyncCreateMultiplexedSession(any(SessionConsumer.class));
  }

  @Test
  public void testClosedPoolIncludesClosedException() {
    pool = createPool();
    assertTrue(pool.isValid());
    closePoolWithStacktrace();
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
    setupMockSessionCreation();
    pool = createPool();
    try (Session session = pool.getSession()) {
      assertThat(session).isNotNull();
    }
  }

  @Test
  public void poolLifo() {
    setupMockSessionCreation();
    options =
        options
            .toBuilder()
            .setMinSessions(2)
            .setWaitForMinSessions(Duration.ofSeconds(10L))
            .build();
    pool = createPool();
    pool.maybeWaitOnMinSessions();
    Session session1 = pool.getSession().get();
    Session session2 = pool.getSession().get();
    assertThat(session1).isNotEqualTo(session2);

    session2.close();
    session1.close();

    // Check the session out and back in once more to finalize their positions.
    session1 = pool.getSession().get();
    session2 = pool.getSession().get();
    session2.close();
    session1.close();

    Session session3 = pool.getSession().get();
    Session session4 = pool.getSession().get();
    assertThat(session3).isEqualTo(session1);
    assertThat(session4).isEqualTo(session2);
    session3.close();
    session4.close();
  }

  @Test
  public void poolFifo() throws Exception {
    setupMockSessionCreation();
    runWithSystemProperty(
        "com.google.cloud.spanner.session_pool_release_to_position",
        "LAST",
        () -> {
          options =
              options
                  .toBuilder()
                  .setMinSessions(2)
                  .setWaitForMinSessions(Duration.ofSeconds(10L))
                  .build();
          pool = createPool();
          pool.maybeWaitOnMinSessions();
          Session session1 = pool.getSession().get();
          Session session2 = pool.getSession().get();
          assertNotEquals(session1, session2);

          session2.close();
          session1.close();

          // Check the session out and back in once more to finalize their positions.
          session1 = pool.getSession().get();
          session2 = pool.getSession().get();
          session2.close();
          session1.close();

          // Verify that we get the sessions in FIFO order, so in this order:
          // 1. session2
          // 2. session1
          Session session3 = pool.getSession().get();
          Session session4 = pool.getSession().get();
          assertEquals(session2, session3);
          assertEquals(session1, session4);
          session3.close();
          session4.close();

          return null;
        });
  }

  @Test
  public void poolAllPositions() throws Exception {
    int maxAttempts = 100;
    setupMockSessionCreation();
    for (Position position : Position.values()) {
      runWithSystemProperty(
          "com.google.cloud.spanner.session_pool_release_to_position",
          position.name(),
          () -> {
            int attempt = 0;
            while (attempt < maxAttempts) {
              int numSessions = 5;
              options =
                  options
                      .toBuilder()
                      .setMinSessions(numSessions)
                      .setMaxSessions(numSessions)
                      .setWaitForMinSessions(Duration.ofSeconds(10L))
                      .build();
              pool = createPool();
              pool.maybeWaitOnMinSessions();
              // First check out and release the sessions twice to the pool, so we know that we have
              // finalized the position of them.
              for (int n = 0; n < 2; n++) {
                checkoutAndReleaseAllSessions();
              }

              // Now verify that if we get all sessions twice, they will be in random order.
              List<List<PooledSessionFuture>> allSessions = new ArrayList<>(2);
              for (int n = 0; n < 2; n++) {
                allSessions.add(checkoutAndReleaseAllSessions());
              }
              List<Session> firstTime =
                  allSessions.get(0).stream()
                      .map(PooledSessionFuture::get)
                      .collect(Collectors.toList());
              List<Session> secondTime =
                  allSessions.get(1).stream()
                      .map(PooledSessionFuture::get)
                      .collect(Collectors.toList());
              switch (position) {
                case FIRST:
                  // LIFO:
                  // First check out all sessions, so we have 1, 2, 3, 4, ..., N
                  // Then release them all back into the pool in the same order (1, 2, 3, 4, ..., N)
                  // That will give us the list N, ..., 4, 3, 2, 1 because each session is added at
                  // the front of the pool.
                  assertEquals(firstTime, Lists.reverse(secondTime));
                  break;
                case LAST:
                  // FIFO:
                  // First check out all sessions, so we have 1, 2, 3, 4, ..., N
                  // Then release them all back into the pool in the same order (1, 2, 3, 4, ..., N)
                  // That will give us the list 1, 2, 3, 4, ..., N because each session is added at
                  // the end of the pool.
                  assertEquals(firstTime, secondTime);
                  break;
                case RANDOM:
                  // Random means that we should not get the same order twice (unless the randomizer
                  // got lucky, and then we retry).
                  if (attempt < (maxAttempts - 1)) {
                    if (Objects.equals(firstTime, secondTime)) {
                      attempt++;
                      continue;
                    }
                  }
                  assertNotEquals(firstTime, secondTime);
              }
              break;
            }
            return null;
          });
    }
  }

  private List<PooledSessionFuture> checkoutAndReleaseAllSessions() {
    List<PooledSessionFuture> sessions = new ArrayList<>(pool.totalSessions());
    for (int i = 0; i < pool.totalSessions(); i++) {
      sessions.add(pool.getSession());
    }
    for (Session session : sessions) {
      session.close();
    }
    return sessions;
  }

  @Test
  public void poolClosure() throws Exception {
    setupMockSessionCreation();
    pool = createPool();
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void poolClosureClosesLeakedSessions() throws Exception {
    SessionImpl mockSession1 = mockSession();
    SessionImpl mockSession2 = mockSession();
    final LinkedList<SessionImpl> sessions =
        new LinkedList<>(Arrays.asList(mockSession1, mockSession2));
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(sessions.pop());
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    Session session1 = pool.getSession();
    // Leaked sessions
    PooledSessionFuture leakedSession = pool.getSession();
    // Clear the leaked exception to suppress logging of expected exceptions.
    leakedSession.clearLeakedException();
    session1.close();
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
    verify(mockSession1).asyncClose();
    verify(mockSession2).asyncClose();
  }

  @Test
  public void poolClosesWhenMaintenanceLoopIsRunning() throws Exception {
    setupMockSessionCreation();
    final FakeClock clock = new FakeClock();
    pool = createPool(clock);
    final AtomicBoolean stop = new AtomicBoolean(false);
    new Thread(
            () -> {
              // Run in a tight loop.
              while (!stop.get()) {
                runMaintenanceLoop(clock, pool, 1);
              }
            })
        .start();
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
    stop.set(true);
  }

  @Test
  public void poolClosureFailsPendingReadWaiters() throws Exception {
    final CountDownLatch insideCreation = new CountDownLatch(1);
    final CountDownLatch releaseCreation = new CountDownLatch(1);
    final SessionImpl session1 = mockSession();
    final SessionImpl session2 = mockSession();
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(session1);
                  });
              return null;
            })
        .doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    insideCreation.countDown();
                    releaseCreation.await();
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(session2);
                    return null;
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    pool = createPool();
    PooledSessionFuture leakedSession = pool.getSession();
    // Suppress expected leakedSession warning.
    leakedSession.clearLeakedException();
    AtomicBoolean failed = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(1);
    getSessionAsync(latch, failed);
    insideCreation.await();
    pool.closeAsync(new SpannerImpl.ClosedException());
    releaseCreation.countDown();
    latch.await(5L, TimeUnit.SECONDS);
    assertThat(failed.get()).isTrue();
  }

  @Test
  public void poolClosureFailsPendingWriteWaiters() throws Exception {
    final CountDownLatch insideCreation = new CountDownLatch(1);
    final CountDownLatch releaseCreation = new CountDownLatch(1);
    final SessionImpl session1 = mockSession();
    final SessionImpl session2 = mockSession();
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(session1);
                  });
              return null;
            })
        .doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    insideCreation.countDown();
                    releaseCreation.await();
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(session2);
                    return null;
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    pool = createPool();
    PooledSessionFuture leakedSession = pool.getSession();
    // Suppress expected leakedSession warning.
    leakedSession.clearLeakedException();
    AtomicBoolean failed = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(1);
    getSessionAsync(latch, failed);
    insideCreation.await();
    pool.closeAsync(new SpannerImpl.ClosedException());
    releaseCreation.countDown();
    latch.await();
    assertThat(failed.get()).isTrue();
  }

  @Test
  public void poolClosesEvenIfCreationFails() throws Exception {
    final CountDownLatch insideCreation = new CountDownLatch(1);
    final CountDownLatch releaseCreation = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    insideCreation.countDown();
                    releaseCreation.await();
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionCreateFailure(
                        SpannerExceptionFactory.newSpannerException(new RuntimeException()), 1);
                    return null;
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    AtomicBoolean failed = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(1);
    getSessionAsync(latch, failed);
    insideCreation.await();
    ListenableFuture<Void> f = pool.closeAsync(new SpannerImpl.ClosedException());
    releaseCreation.countDown();
    f.get();
    assertThat(f.isDone()).isTrue();
  }

  @Test
  public void poolClosureFailsNewRequests() {
    final SessionImpl session = mockSession();
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(session);
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    PooledSessionFuture leakedSession = pool.getSession();
    leakedSession.get();
    // Suppress expected leakedSession warning.
    leakedSession.clearLeakedException();
    pool.closeAsync(new SpannerImpl.ClosedException());
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> pool.getSession());
    assertNotNull(e.getMessage());
  }

  @Test
  public void atMostMaxSessionsCreated() {
    setupMockSessionCreation();
    AtomicBoolean failed = new AtomicBoolean(false);
    pool = createPool();
    int numSessions = 10;
    final CountDownLatch latch = new CountDownLatch(numSessions);
    for (int i = 0; i < numSessions; i++) {
      getSessionAsync(latch, failed);
    }
    Uninterruptibles.awaitUninterruptibly(latch);
    verify(sessionClient, atMost(options.getMaxSessions()))
        .asyncBatchCreateSessions(eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    assertThat(failed.get()).isFalse();
  }

  @Test
  public void creationExceptionPropagatesToReadSession() {
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionCreateFailure(
                        SpannerExceptionFactory.newSpannerException(ErrorCode.INTERNAL, ""), 1);
                    return null;
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    SpannerException e = assertThrows(SpannerException.class, () -> pool.getSession().get());
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
  }

  @Test
  public void failOnPoolExhaustion() {
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(1)
            .setFailIfPoolExhausted()
            .build();
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(mockSession());
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    Session session1 = pool.getSession();
    SpannerException e = assertThrows(SpannerException.class, () -> pool.getSession());
    assertEquals(ErrorCode.RESOURCE_EXHAUSTED, e.getErrorCode());
    session1.close();
    session1 = pool.getSession();
    assertThat(session1).isNotNull();
    session1.close();
  }

  @Test
  public void idleSessionCleanup() throws Exception {
    ReadContext context = mock(ReadContext.class);

    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .setPoolMaintainerClock(clock)
            .build();
    SpannerImpl spanner = mock(SpannerImpl.class);
    SpannerOptions spannerOptions = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(spannerOptions);
    when(spannerOptions.getSessionPoolOptions()).thenReturn(options);
    SessionImpl session1 = buildMockSession(spanner, context);
    SessionImpl session2 = buildMockSession(spanner, context);
    SessionImpl session3 = buildMockSession(spanner, context);
    final LinkedList<SessionImpl> sessions =
        new LinkedList<>(Arrays.asList(session1, session2, session3));
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(sessions.pop());
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    mockKeepAlive(context);

    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();
    runMaintenanceLoop(clock, pool, pool.poolMaintainer.numClosureCycles);
    assertThat(pool.numIdleSessionsRemoved()).isEqualTo(0L);
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();
    PooledSessionFuture readSession3 = pool.getSession();
    // Wait until the sessions have actually been gotten in order to make sure they are in use in
    // parallel.
    readSession1.get();
    readSession2.get();
    readSession3.get();
    readSession1.close();
    readSession2.close();
    readSession3.close();
    // Now there are 3 sessions in the pool but since none of them has timed out, they will all be
    // kept in the pool.
    runMaintenanceLoop(clock, pool, pool.poolMaintainer.numClosureCycles);
    assertThat(pool.numIdleSessionsRemoved()).isEqualTo(0L);
    // Counters have now been reset
    // Use all 3 sessions sequentially
    pool.getSession().close();
    pool.getSession().close();
    pool.getSession().close();
    // Advance the time by running the maintainer. This should cause
    // one session to be kept alive and two sessions to be removed.
    long cycles =
        options.getRemoveInactiveSessionAfter().toMillis() / pool.poolMaintainer.loopFrequency;
    runMaintenanceLoop(clock, pool, cycles);
    // We will still close 2 sessions since at any point in time only 1 session was in use.
    assertThat(pool.numIdleSessionsRemoved()).isEqualTo(2L);
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void longRunningTransactionsCleanup_whenActionSetToClose_verifyInactiveSessionsClosed()
      throws Exception {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .setPoolMaintainerClock(clock)
            .setCloseIfInactiveTransactions() // set option to close inactive transactions
            .build();
    setupForLongRunningTransactionsCleanup(options);

    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();

    // All 3 sessions used. 100% of pool utilised.
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();
    PooledSessionFuture readSession3 = pool.getSession();

    // complete the async tasks
    readSession1.get().setEligibleForLongRunning(false);
    readSession2.get().setEligibleForLongRunning(false);
    readSession3.get().setEligibleForLongRunning(true);

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());

    // ensure that the sessions are in use for > 60 minutes
    pool.poolMaintainer.lastExecutionTime = Instant.now();
    when(clock.instant()).thenReturn(Instant.now().plus(61, ChronoUnit.MINUTES));

    pool.poolMaintainer.maintainPool();

    // the two session that were un-expectedly long-running were removed from the pool.
    // verify that only 1 session that is unexpected to be long-running remains in the pool.
    assertEquals(1, pool.totalSessions());
    assertEquals(2, pool.numLeakedSessionsRemoved());
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void longRunningTransactionsCleanup_whenActionSetToWarn_verifyInactiveSessionsOpen()
      throws Exception {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setPoolMaintainerClock(clock)
            .setWarnIfInactiveTransactions() // set option to warn (via logs) inactive transactions
            .build();
    setupForLongRunningTransactionsCleanup(options);

    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();

    // All 3 sessions used. 100% of pool utilised.
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();
    PooledSessionFuture readSession3 = pool.getSession();

    // complete the async tasks
    readSession1.get().setEligibleForLongRunning(false);
    readSession2.get().setEligibleForLongRunning(false);
    readSession3.get().setEligibleForLongRunning(true);

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());

    // ensure that the sessions are in use for > 60 minutes
    pool.poolMaintainer.lastExecutionTime = Instant.now();
    when(clock.instant()).thenReturn(Instant.now().plus(61, ChronoUnit.MINUTES));

    pool.poolMaintainer.maintainPool();

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());
    assertEquals(0, pool.numLeakedSessionsRemoved());

    readSession1.close();
    readSession2.close();
    readSession3.close();
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void
      longRunningTransactionsCleanup_whenUtilisationBelowThreshold_verifyInactiveSessionsOpen()
          throws Exception {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .setPoolMaintainerClock(clock)
            .setCloseIfInactiveTransactions() // set option to close inactive transactions
            .build();
    setupForLongRunningTransactionsCleanup(options);

    pool = createPool(clock);
    pool.getSession().close();

    // 2/3 sessions are used. Hence utilisation < 95%
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();

    // complete the async tasks and mark sessions as checked out
    readSession1.get().setEligibleForLongRunning(false);
    readSession2.get().setEligibleForLongRunning(false);

    assertEquals(2, pool.totalSessions());
    assertEquals(2, pool.checkedOutSessions.size());

    // ensure that the sessions are in use for > 60 minutes
    pool.poolMaintainer.lastExecutionTime = Instant.now();
    when(clock.instant()).thenReturn(Instant.now().plus(61, ChronoUnit.MINUTES));

    pool.poolMaintainer.maintainPool();

    assertEquals(2, pool.totalSessions());
    assertEquals(2, pool.checkedOutSessions.size());
    assertEquals(0, pool.numLeakedSessionsRemoved());
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void
      longRunningTransactionsCleanup_whenAllAreExpectedlyLongRunning_verifyInactiveSessionsOpen()
          throws Exception {
    SessionImpl session1 = mockSession();
    SessionImpl session2 = mockSession();
    SessionImpl session3 = mockSession();

    final LinkedList<SessionImpl> sessions =
        new LinkedList<>(Arrays.asList(session1, session2, session3));
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(sessions.pop());
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    for (SessionImpl session : sessions) {
      mockKeepAlive(session);
    }
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .setCloseIfInactiveTransactions() // set option to close inactive transactions
            .build();
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now());

    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();

    // All 3 sessions used. 100% of pool utilised.
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();
    PooledSessionFuture readSession3 = pool.getSession();

    // complete the async tasks
    readSession1.get().setEligibleForLongRunning(true);
    readSession2.get().setEligibleForLongRunning(true);
    readSession3.get().setEligibleForLongRunning(true);

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());

    // ensure that the sessions are in use for > 60 minutes
    pool.poolMaintainer.lastExecutionTime = Instant.now();
    when(clock.instant()).thenReturn(Instant.now().plus(61, ChronoUnit.MINUTES));

    pool.poolMaintainer.maintainPool();

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());
    assertEquals(0, pool.numLeakedSessionsRemoved());
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void longRunningTransactionsCleanup_whenBelowDurationThreshold_verifyInactiveSessionsOpen()
      throws Exception {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .setPoolMaintainerClock(clock)
            .setCloseIfInactiveTransactions() // set option to close inactive transactions
            .build();
    setupForLongRunningTransactionsCleanup(options);

    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();

    // All 3 sessions used. 100% of pool utilised.
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();
    PooledSessionFuture readSession3 = pool.getSession();

    // complete the async tasks
    readSession1.get().setEligibleForLongRunning(false);
    readSession2.get().setEligibleForLongRunning(false);
    readSession3.get().setEligibleForLongRunning(true);

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());

    // ensure that the sessions are in use for < 60 minutes
    pool.poolMaintainer.lastExecutionTime = Instant.now();
    when(clock.instant()).thenReturn(Instant.now().plus(50, ChronoUnit.MINUTES));

    pool.poolMaintainer.maintainPool();

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());
    assertEquals(0, pool.numLeakedSessionsRemoved());
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void longRunningTransactionsCleanup_whenException_doNothing() throws Exception {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .setPoolMaintainerClock(clock)
            .setCloseIfInactiveTransactions() // set option to close inactive transactions
            .build();
    setupForLongRunningTransactionsCleanup(options);

    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();

    // All 3 sessions used. 100% of pool utilised.
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();
    PooledSessionFuture readSession3 = pool.getSession();

    // complete the async tasks
    readSession1.get().setEligibleForLongRunning(false);
    readSession2.get().setEligibleForLongRunning(false);
    readSession3.get().setEligibleForLongRunning(true);

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());

    when(clock.instant()).thenReturn(Instant.now().plus(50, ChronoUnit.MINUTES));

    pool.poolMaintainer.lastExecutionTime = null; // setting null to throw exception
    pool.poolMaintainer.maintainPool();

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());
    assertEquals(0, pool.numLeakedSessionsRemoved());
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void
      longRunningTransactionsCleanup_whenTaskRecurrenceBelowThreshold_verifyInactiveSessionsOpen()
          throws Exception {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.now());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .setPoolMaintainerClock(clock)
            .setCloseIfInactiveTransactions() // set option to close inactive transactions
            .build();
    setupForLongRunningTransactionsCleanup(options);

    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();

    // All 3 sessions used. 100% of pool utilised.
    PooledSessionFuture readSession1 = pool.getSession();
    PooledSessionFuture readSession2 = pool.getSession();
    PooledSessionFuture readSession3 = pool.getSession();

    // complete the async tasks
    readSession1.get();
    readSession2.get();
    readSession3.get();

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());

    pool.poolMaintainer.lastExecutionTime = Instant.now();
    when(clock.instant()).thenReturn(Instant.now().plus(10, ChronoUnit.SECONDS));

    pool.poolMaintainer.maintainPool();

    assertEquals(3, pool.totalSessions());
    assertEquals(3, pool.checkedOutSessions.size());
    assertEquals(0, pool.numLeakedSessionsRemoved());

    readSession1.close();
    readSession2.close();
    readSession3.close();
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  private void setupForLongRunningTransactionsCleanup(SessionPoolOptions sessionPoolOptions) {
    ReadContext context = mock(ReadContext.class);
    SpannerImpl spanner = mock(SpannerImpl.class);
    SpannerOptions options = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(options);
    when(options.getSessionPoolOptions()).thenReturn(sessionPoolOptions);
    SessionImpl session1 = buildMockSession(spanner, context);
    SessionImpl session2 = buildMockSession(spanner, context);
    SessionImpl session3 = buildMockSession(spanner, context);

    final LinkedList<SessionImpl> sessions =
        new LinkedList<>(Arrays.asList(session1, session2, session3));
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(sessions.pop());
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    mockKeepAlive(context);
  }

  @Test
  public void keepAlive() throws Exception {
    ReadContext context = mock(ReadContext.class);
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(2)
            .setMaxSessions(3)
            .setPoolMaintainerClock(clock)
            .build();
    SpannerImpl spanner = mock(SpannerImpl.class);
    SpannerOptions spannerOptions = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(spannerOptions);
    when(spannerOptions.getSessionPoolOptions()).thenReturn(options);
    final SessionImpl mockSession1 = buildMockSession(spanner, context);
    final SessionImpl mockSession2 = buildMockSession(spanner, context);
    final SessionImpl mockSession3 = buildMockSession(spanner, context);
    final LinkedList<SessionImpl> sessions =
        new LinkedList<>(Arrays.asList(mockSession1, mockSession2, mockSession3));

    mockKeepAlive(context);
    // This is cheating as we are returning the same session each but it makes the verification
    // easier.
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    int sessionCount = invocation.getArgument(0, Integer.class);
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    for (int i = 0; i < sessionCount; i++) {
                      consumer.onSessionReady(sessions.pop());
                    }
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(anyInt(), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool(clock);
    PooledSessionFuture session1 = pool.getSession();
    PooledSessionFuture session2 = pool.getSession();
    session1.get();
    session2.get();
    session1.close();
    session2.close();
    runMaintenanceLoop(clock, pool, pool.poolMaintainer.numKeepAliveCycles);
    verify(context, never()).executeQuery(any(Statement.class));
    runMaintenanceLoop(clock, pool, pool.poolMaintainer.numKeepAliveCycles);
    verify(context, times(2)).executeQuery(Statement.newBuilder("SELECT 1").build());
    clock.currentTimeMillis.addAndGet(
        clock.currentTimeMillis.get() + (options.getKeepAliveIntervalMinutes() + 5L) * 60L * 1000L);
    session1 = pool.getSession();
    session1.writeAtLeastOnceWithOptions(new ArrayList<>());
    session1.close();
    runMaintenanceLoop(clock, pool, pool.poolMaintainer.numKeepAliveCycles);
    // The session pool only keeps MinSessions + MaxIdleSessions alive.
    verify(context, times(options.getMinSessions() + options.getMaxIdleSessions()))
        .executeQuery(Statement.newBuilder("SELECT 1").build());
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void blockAndTimeoutOnPoolExhaustion() throws Exception {
    // Create a session pool with max 1 session and a low timeout for waiting for a session.
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(minSessions)
            .setMaxSessions(1)
            .setInitialWaitForSessionTimeoutMillis(20L)
            .setAcquireSessionTimeout(null)
            .build();
    setupMockSessionCreation();
    pool = createPool();
    // Take the only session that can be in the pool.
    PooledSessionFuture checkedOutSession = pool.getSession();
    checkedOutSession.get();
    ExecutorService executor = Executors.newFixedThreadPool(1);
    final CountDownLatch latch = new CountDownLatch(1);
    // Then try asynchronously to take another session. This attempt should time out.
    Future<Void> fut =
        executor.submit(
            () -> {
              latch.countDown();
              PooledSessionFuture session = pool.getSession();
              session.close();
              return null;
            });
    // Wait until the background thread is actually waiting for a session.
    latch.await();
    // Wait until the request has timed out.
    int waitCount = 0;
    while (pool.getNumWaiterTimeouts() == 0L && waitCount < 5000) {
      Thread.sleep(1L);
      waitCount++;
    }
    // Return the checked out session to the pool so the async request will get a session and
    // finish.
    checkedOutSession.close();
    // Verify that the async request also succeeds.
    fut.get(10L, TimeUnit.SECONDS);
    executor.shutdown();

    // Verify that the session was returned to the pool and that we can get it again.
    Session session = pool.getSession();
    assertThat(session).isNotNull();
    session.close();
    assertThat(pool.getNumWaiterTimeouts()).isAtLeast(1L);
  }

  @Test
  public void blockAndTimeoutOnPoolExhaustion_withAcquireSessionTimeout() throws Exception {
    // Create a session pool with max 1 session and a low timeout for waiting for a session.
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(minSessions)
            .setMaxSessions(1)
            .setInitialWaitForSessionTimeoutMillis(20L)
            .setAcquireSessionTimeout(Duration.ofMillis(20L))
            .build();
    setupMockSessionCreation();
    pool = createPool();
    // Take the only session that can be in the pool.
    PooledSessionFuture checkedOutSession = pool.getSession();
    checkedOutSession.get();
    ExecutorService executor = Executors.newFixedThreadPool(1);
    final CountDownLatch latch = new CountDownLatch(1);
    // Then try asynchronously to take another session. This attempt should time out.
    Future<Void> fut =
        executor.submit(
            () -> {
              latch.countDown();
              PooledSessionFuture session = pool.getSession();
              session.close();
              return null;
            });
    // Wait until the background thread is actually waiting for a session.
    latch.await();
    // Wait until the request has timed out.
    int waitCount = 0;
    while (pool.getNumWaiterTimeouts() == 0L && waitCount < 5000) {
      Thread.sleep(1L);
      waitCount++;
    }
    // Return the checked out session to the pool so the async request will get a session and
    // finish.
    checkedOutSession.close();
    // Verify that the async request also succeeds.
    fut.get(10L, TimeUnit.SECONDS);
    executor.shutdown();

    // Verify that the session was returned to the pool and that we can get it again.
    Session session = pool.getSession();
    assertThat(session).isNotNull();
    session.close();
    assertThat(pool.getNumWaiterTimeouts()).isAtLeast(1L);
  }

  @Test
  public void testSessionNotFoundSingleUse() {
    Statement statement = Statement.of("SELECT 1");
    final SessionImpl closedSession = mockSession();
    ReadContext closedContext = mock(ReadContext.class);
    ResultSet closedResultSet = mock(ResultSet.class);
    when(closedResultSet.next())
        .thenThrow(SpannerExceptionFactoryTest.newSessionNotFoundException(sessionName));
    when(closedContext.executeQuery(statement)).thenReturn(closedResultSet);
    when(closedSession.singleUse()).thenReturn(closedContext);

    final SessionImpl openSession = mockSession();
    ReadContext openContext = mock(ReadContext.class);
    ResultSet openResultSet = mock(ResultSet.class);
    when(openResultSet.next()).thenReturn(true, false);
    when(openContext.executeQuery(statement)).thenReturn(openResultSet);
    when(openSession.singleUse()).thenReturn(openContext);

    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(closedSession);
                  });
              return null;
            })
        .doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(openSession);
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    pool = createPool(clock);
    ReadContext context = pool.getSession().singleUse();
    ResultSet resultSet = context.executeQuery(statement);
    assertThat(resultSet.next()).isTrue();
  }

  @Test
  public void testSessionNotFoundReadOnlyTransaction() {
    Statement statement = Statement.of("SELECT 1");
    final SessionImpl closedSession = mockSession();
    when(closedSession.readOnlyTransaction())
        .thenThrow(SpannerExceptionFactoryTest.newSessionNotFoundException(sessionName));

    final SessionImpl openSession = mockSession();
    ReadOnlyTransaction openTransaction = mock(ReadOnlyTransaction.class);
    ResultSet openResultSet = mock(ResultSet.class);
    when(openResultSet.next()).thenReturn(true, false);
    when(openTransaction.executeQuery(statement)).thenReturn(openResultSet);
    when(openSession.readOnlyTransaction()).thenReturn(openTransaction);

    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(closedSession);
                  });
              return null;
            })
        .doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(openSession);
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    pool = createPool(clock);
    ReadOnlyTransaction transaction = pool.getSession().readOnlyTransaction();
    ResultSet resultSet = transaction.executeQuery(statement);
    assertThat(resultSet.next()).isTrue();
  }

  private enum ReadWriteTransactionTestStatementType {
    QUERY,
    ANALYZE,
    UPDATE,
    BATCH_UPDATE,
    WRITE,
    EXCEPTION
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSessionNotFoundReadWriteTransaction() {
    final Statement queryStatement = Statement.of("SELECT 1");
    final Statement updateStatement = Statement.of("UPDATE FOO SET BAR=1 WHERE ID=2");
    final SpannerException sessionNotFound =
        SpannerExceptionFactoryTest.newSessionNotFoundException(sessionName);
    for (ReadWriteTransactionTestStatementType statementType :
        ReadWriteTransactionTestStatementType.values()) {
      final ReadWriteTransactionTestStatementType executeStatementType = statementType;
      SpannerRpc.StreamingCall closedStreamingCall = mock(SpannerRpc.StreamingCall.class);
      doThrow(sessionNotFound).when(closedStreamingCall).request(Mockito.anyInt());
      SpannerRpc rpc = mock(SpannerRpc.class);
      when(rpc.asyncDeleteSession(Mockito.anyString(), Mockito.anyMap()))
          .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
      when(rpc.executeQuery(
              any(ExecuteSqlRequest.class),
              any(ResultStreamConsumer.class),
              any(Map.class),
              eq(true)))
          .thenReturn(closedStreamingCall);
      when(rpc.executeQuery(any(ExecuteSqlRequest.class), any(Map.class), eq(true)))
          .thenThrow(sessionNotFound);
      when(rpc.executeBatchDml(any(ExecuteBatchDmlRequest.class), any(Map.class)))
          .thenThrow(sessionNotFound);
      when(rpc.commitAsync(any(CommitRequest.class), any(Map.class)))
          .thenReturn(ApiFutures.<CommitResponse>immediateFailedFuture(sessionNotFound));
      when(rpc.rollbackAsync(any(RollbackRequest.class), any(Map.class)))
          .thenReturn(ApiFutures.<Empty>immediateFailedFuture(sessionNotFound));
      when(rpc.getReadRetrySettings())
          .thenReturn(SpannerStubSettings.newBuilder().streamingReadSettings().getRetrySettings());
      when(rpc.getReadRetryableCodes())
          .thenReturn(SpannerStubSettings.newBuilder().streamingReadSettings().getRetryableCodes());
      when(rpc.getExecuteQueryRetrySettings())
          .thenReturn(
              SpannerStubSettings.newBuilder().executeStreamingSqlSettings().getRetrySettings());
      when(rpc.getExecuteQueryRetryableCodes())
          .thenReturn(
              SpannerStubSettings.newBuilder().executeStreamingSqlSettings().getRetryableCodes());
      final SessionImpl closedSession = mock(SessionImpl.class);
      when(closedSession.getName())
          .thenReturn("projects/dummy/instances/dummy/database/dummy/sessions/session-closed");

      Span oTspan = mock(Span.class);
      ISpan span = new OpenTelemetrySpan(oTspan);
      when(oTspan.makeCurrent()).thenReturn(mock(Scope.class));

      final TransactionContextImpl closedTransactionContext =
          TransactionContextImpl.newBuilder()
              .setSession(closedSession)
              .setOptions(Options.fromTransactionOptions())
              .setRpc(rpc)
              .setTracer(tracer)
              .setSpan(span)
              .build();
      when(closedSession.asyncClose())
          .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
      when(closedSession.newTransaction(Options.fromTransactionOptions()))
          .thenReturn(closedTransactionContext);
      when(closedSession.beginTransactionAsync(any(), eq(true))).thenThrow(sessionNotFound);
      when(closedSession.getTracer()).thenReturn(tracer);
      TransactionRunnerImpl closedTransactionRunner = new TransactionRunnerImpl(closedSession);
      closedTransactionRunner.setSpan(span);
      when(closedSession.readWriteTransaction()).thenReturn(closedTransactionRunner);

      final SessionImpl openSession = mock(SessionImpl.class);
      when(openSession.asyncClose())
          .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
      when(openSession.getName())
          .thenReturn("projects/dummy/instances/dummy/database/dummy/sessions/session-open");
      final TransactionContextImpl openTransactionContext = mock(TransactionContextImpl.class);
      when(openSession.newTransaction(Options.fromTransactionOptions()))
          .thenReturn(openTransactionContext);
      when(openSession.beginTransactionAsync(any(), eq(true)))
          .thenReturn(ApiFutures.immediateFuture(ByteString.copyFromUtf8("open-txn")));
      when(openSession.getTracer()).thenReturn(tracer);
      TransactionRunnerImpl openTransactionRunner = new TransactionRunnerImpl(openSession);
      openTransactionRunner.setSpan(span);
      when(openSession.readWriteTransaction()).thenReturn(openTransactionRunner);

      ResultSet openResultSet = mock(ResultSet.class);
      when(openResultSet.next()).thenReturn(true, false);
      ResultSet planResultSet = mock(ResultSet.class);
      when(planResultSet.getStats()).thenReturn(ResultSetStats.getDefaultInstance());
      when(openTransactionContext.executeQuery(queryStatement)).thenReturn(openResultSet);
      when(openTransactionContext.analyzeQuery(queryStatement, QueryAnalyzeMode.PLAN))
          .thenReturn(planResultSet);
      when(openTransactionContext.executeUpdate(updateStatement)).thenReturn(1L);
      when(openTransactionContext.batchUpdate(Arrays.asList(updateStatement, updateStatement)))
          .thenReturn(new long[] {1L, 1L});
      SpannerImpl spanner = mock(SpannerImpl.class);
      SessionClient sessionClient = mock(SessionClient.class);
      when(spanner.getSessionClient(db)).thenReturn(sessionClient);
      when(sessionClient.getSpanner()).thenReturn(spanner);

      doAnswer(
              invocation -> {
                executor.submit(
                    () -> {
                      SessionConsumerImpl consumer =
                          invocation.getArgument(2, SessionConsumerImpl.class);
                      consumer.onSessionReady(closedSession);
                    });
                return null;
              })
          .doAnswer(
              invocation -> {
                executor.submit(
                    () -> {
                      SessionConsumerImpl consumer =
                          invocation.getArgument(2, SessionConsumerImpl.class);
                      consumer.onSessionReady(openSession);
                    });
                return null;
              })
          .when(sessionClient)
          .asyncBatchCreateSessions(
              Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
      SessionPoolOptions options =
          SessionPoolOptions.newBuilder()
              .setMinSessions(0) // The pool should not auto-create any sessions
              .setMaxSessions(2)
              .setIncStep(1)
              .setBlockIfPoolExhausted()
              .build();
      SpannerOptions spannerOptions = mock(SpannerOptions.class);
      when(spannerOptions.getSessionPoolOptions()).thenReturn(options);
      when(spannerOptions.getNumChannels()).thenReturn(4);
      when(spannerOptions.getDatabaseRole()).thenReturn("role");
      when(spanner.getOptions()).thenReturn(spannerOptions);
      SessionPool pool =
          SessionPool.createPool(
              options,
              new TestExecutorFactory(),
              spanner.getSessionClient(db),
              tracer,
              OpenTelemetry.noop());
      try (PooledSessionFuture readWriteSession = pool.getSession()) {
        TransactionRunner runner = readWriteSession.readWriteTransaction();
        try {
          runner.run(
              new TransactionCallable<Integer>() {
                private int callNumber = 0;

                @Override
                public Integer run(TransactionContext transaction) {
                  callNumber++;
                  if (callNumber == 1) {
                    assertThat(transaction).isEqualTo(closedTransactionContext);
                  } else {
                    assertThat(transaction).isEqualTo(openTransactionContext);
                  }
                  switch (executeStatementType) {
                    case QUERY:
                      ResultSet resultSet = transaction.executeQuery(queryStatement);
                      assertThat(resultSet.next()).isTrue();
                      break;
                    case ANALYZE:
                      ResultSet planResultSet =
                          transaction.analyzeQuery(queryStatement, QueryAnalyzeMode.PLAN);
                      assertThat(planResultSet.next()).isFalse();
                      assertThat(planResultSet.getStats()).isNotNull();
                      break;
                    case UPDATE:
                      long updateCount = transaction.executeUpdate(updateStatement);
                      assertThat(updateCount).isEqualTo(1L);
                      break;
                    case BATCH_UPDATE:
                      long[] updateCounts =
                          transaction.batchUpdate(Arrays.asList(updateStatement, updateStatement));
                      assertThat(updateCounts).isEqualTo(new long[] {1L, 1L});
                      break;
                    case WRITE:
                      transaction.buffer(Mutation.delete("FOO", Key.of(1L)));
                      break;
                    case EXCEPTION:
                      throw new RuntimeException("rollback at call " + callNumber);
                    default:
                      fail("Unknown statement type: " + executeStatementType);
                  }
                  return callNumber;
                }
              });
        } catch (Exception e) {
          // The rollback will also cause a SessionNotFoundException, but this is caught, logged
          // and further ignored by the library, meaning that the session will not be re-created
          // for retry. Hence rollback at call 1.
          assertThat(executeStatementType)
              .isEqualTo(ReadWriteTransactionTestStatementType.EXCEPTION);
          assertThat(e.getMessage()).contains("rollback at call 1");
        }
      }
      pool.closeAsync(new SpannerImpl.ClosedException());
    }
  }

  @Test
  public void testSessionNotFoundWrite() {
    SpannerException sessionNotFound =
        SpannerExceptionFactoryTest.newSessionNotFoundException(sessionName);
    List<Mutation> mutations = Collections.singletonList(Mutation.newInsertBuilder("FOO").build());
    final SessionImpl closedSession = mockSession();
    when(closedSession.writeWithOptions(mutations)).thenThrow(sessionNotFound);

    final SessionImpl openSession = mockSession();
    com.google.cloud.spanner.CommitResponse response =
        mock(com.google.cloud.spanner.CommitResponse.class);
    when(response.getCommitTimestamp()).thenReturn(Timestamp.now());
    when(openSession.writeWithOptions(mutations)).thenReturn(response);
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(closedSession);
                  });
              return null;
            })
        .doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(openSession);
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    pool = createPool(clock);
    DatabaseClientImpl impl = new DatabaseClientImpl(pool, tracer);
    assertThat(impl.write(mutations)).isNotNull();
  }

  @Test
  public void testSessionNotFoundWriteAtLeastOnce() {
    SpannerException sessionNotFound =
        SpannerExceptionFactoryTest.newSessionNotFoundException(sessionName);
    List<Mutation> mutations = Collections.singletonList(Mutation.newInsertBuilder("FOO").build());
    final SessionImpl closedSession = mockSession();
    when(closedSession.writeAtLeastOnceWithOptions(mutations)).thenThrow(sessionNotFound);

    final SessionImpl openSession = mockSession();
    com.google.cloud.spanner.CommitResponse response =
        mock(com.google.cloud.spanner.CommitResponse.class);
    when(response.getCommitTimestamp()).thenReturn(Timestamp.now());
    when(openSession.writeAtLeastOnceWithOptions(mutations)).thenReturn(response);
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(closedSession);
                  });
              return null;
            })
        .doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(openSession);
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    pool = createPool(clock);
    DatabaseClientImpl impl = new DatabaseClientImpl(pool, tracer);
    assertThat(impl.writeAtLeastOnce(mutations)).isNotNull();
  }

  @Test
  public void testSessionNotFoundPartitionedUpdate() {
    SpannerException sessionNotFound =
        SpannerExceptionFactoryTest.newSessionNotFoundException(sessionName);
    Statement statement = Statement.of("UPDATE FOO SET BAR=1 WHERE 1=1");
    final SessionImpl closedSession = mockSession();
    when(closedSession.executePartitionedUpdate(statement)).thenThrow(sessionNotFound);

    final SessionImpl openSession = mockSession();
    when(openSession.executePartitionedUpdate(statement)).thenReturn(1L);
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(closedSession);
                  });
              return null;
            })
        .doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    SessionConsumerImpl consumer =
                        invocation.getArgument(2, SessionConsumerImpl.class);
                    consumer.onSessionReady(openSession);
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    pool = createPool(clock);
    DatabaseClientImpl impl = new DatabaseClientImpl(pool, mock(TraceWrapper.class));
    assertThat(impl.executePartitionedUpdate(statement)).isEqualTo(1L);
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testOpenCensusSessionMetrics() throws Exception {
    // Create a session pool with max 2 session and a low timeout for waiting for a session.
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(2)
            .setInitialWaitForSessionTimeoutMillis(50L)
            .setAcquireSessionTimeout(null)
            .build();
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    FakeMetricRegistry metricRegistry = new FakeMetricRegistry();
    List<LabelValue> labelValues =
        Arrays.asList(
            LabelValue.create("client1"),
            LabelValue.create("database1"),
            LabelValue.create("instance1"),
            LabelValue.create("1.0.0"));

    setupMockSessionCreation();
    pool = createPool(clock, metricRegistry, labelValues);
    PooledSessionFuture session1 = pool.getSession();
    PooledSessionFuture session2 = pool.getSession();
    session1.get();
    session2.get();

    MetricsRecord record = metricRegistry.pollRecord();
    assertThat(record.getMetrics().size()).isEqualTo(6);

    List<PointWithFunction> maxInUseSessions =
        record.getMetrics().get(METRIC_PREFIX + MAX_IN_USE_SESSIONS);
    assertThat(maxInUseSessions.size()).isEqualTo(1);
    assertThat(maxInUseSessions.get(0).value()).isEqualTo(2L);
    assertThat(maxInUseSessions.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(maxInUseSessions.get(0).values()).isEqualTo(labelValues);

    List<PointWithFunction> getSessionsTimeouts =
        record.getMetrics().get(METRIC_PREFIX + GET_SESSION_TIMEOUTS);
    assertThat(getSessionsTimeouts.size()).isEqualTo(1);
    assertThat(getSessionsTimeouts.get(0).value()).isAtMost(1L);
    assertThat(getSessionsTimeouts.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(getSessionsTimeouts.get(0).values()).isEqualTo(labelValues);

    List<LabelValue> labelValuesWithRegularSessions = new ArrayList<>(labelValues);
    labelValuesWithRegularSessions.add(LabelValue.create("false"));
    List<LabelValue> labelValuesWithMultiplexedSessions = new ArrayList<>(labelValues);
    labelValuesWithMultiplexedSessions.add(LabelValue.create("true"));
    List<PointWithFunction> numAcquiredSessions =
        record.getMetrics().get(METRIC_PREFIX + NUM_ACQUIRED_SESSIONS);
    assertThat(numAcquiredSessions.size()).isEqualTo(2);
    PointWithFunction regularSessionMetric =
        numAcquiredSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("false")))
            .findFirst()
            .get();
    PointWithFunction multiplexedSessionMetric =
        numAcquiredSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("true")))
            .findFirst()
            .get();
    // verify metrics for regular sessions
    assertThat(regularSessionMetric.value()).isEqualTo(2L);
    assertThat(regularSessionMetric.keys()).isEqualTo(SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS);
    assertThat(regularSessionMetric.values()).isEqualTo(labelValuesWithRegularSessions);

    // verify metrics for multiplexed sessions
    assertThat(multiplexedSessionMetric.value()).isEqualTo(0L);
    assertThat(multiplexedSessionMetric.keys())
        .isEqualTo(SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS);
    assertThat(multiplexedSessionMetric.values()).isEqualTo(labelValuesWithMultiplexedSessions);

    List<PointWithFunction> numReleasedSessions =
        record.getMetrics().get(METRIC_PREFIX + NUM_RELEASED_SESSIONS);
    assertThat(numReleasedSessions.size()).isEqualTo(2);

    regularSessionMetric =
        numReleasedSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("false")))
            .findFirst()
            .get();
    multiplexedSessionMetric =
        numReleasedSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("true")))
            .findFirst()
            .get();
    // verify metrics for regular sessions
    assertThat(regularSessionMetric.value()).isEqualTo(0L);
    assertThat(regularSessionMetric.keys()).isEqualTo(SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS);
    assertThat(regularSessionMetric.values()).isEqualTo(labelValuesWithRegularSessions);

    // verify metrics for multiplexed sessions
    assertThat(multiplexedSessionMetric.value()).isEqualTo(0L);
    assertThat(multiplexedSessionMetric.keys())
        .isEqualTo(SPANNER_LABEL_KEYS_WITH_MULTIPLEXED_SESSIONS);
    assertThat(multiplexedSessionMetric.values()).isEqualTo(labelValuesWithMultiplexedSessions);

    List<PointWithFunction> maxAllowedSessions =
        record.getMetrics().get(METRIC_PREFIX + MAX_ALLOWED_SESSIONS);
    assertThat(maxAllowedSessions.size()).isEqualTo(1);
    assertThat(maxAllowedSessions.get(0).value()).isEqualTo(options.getMaxSessions());
    assertThat(maxAllowedSessions.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(maxAllowedSessions.get(0).values()).isEqualTo(labelValues);

    List<PointWithFunction> numSessionsInPool =
        record.getMetrics().get(METRIC_PREFIX + NUM_SESSIONS_IN_POOL);
    assertThat(numSessionsInPool.size()).isEqualTo(4);
    PointWithFunction beingPrepared = numSessionsInPool.get(0);
    List<LabelValue> labelValuesWithBeingPreparedType = new ArrayList<>(labelValues);
    labelValuesWithBeingPreparedType.add(NUM_SESSIONS_BEING_PREPARED);
    assertThat(beingPrepared.value()).isEqualTo(0L);
    assertThat(beingPrepared.keys()).isEqualTo(SPANNER_LABEL_KEYS_WITH_TYPE);
    assertThat(beingPrepared.values()).isEqualTo(labelValuesWithBeingPreparedType);
    PointWithFunction numSessionsInUse = numSessionsInPool.get(1);
    List<LabelValue> labelValuesWithInUseType = new ArrayList<>(labelValues);
    labelValuesWithInUseType.add(NUM_IN_USE_SESSIONS);
    assertThat(numSessionsInUse.value()).isEqualTo(2L);
    assertThat(numSessionsInUse.keys()).isEqualTo(SPANNER_LABEL_KEYS_WITH_TYPE);
    assertThat(numSessionsInUse.values()).isEqualTo(labelValuesWithInUseType);
    PointWithFunction readSessions = numSessionsInPool.get(2);
    List<LabelValue> labelValuesWithReadType = new ArrayList<>(labelValues);
    labelValuesWithReadType.add(NUM_READ_SESSIONS);
    assertThat(readSessions.value()).isEqualTo(0L);
    assertThat(readSessions.keys()).isEqualTo(SPANNER_LABEL_KEYS_WITH_TYPE);
    assertThat(readSessions.values()).isEqualTo(labelValuesWithReadType);
    PointWithFunction writePreparedSessions = numSessionsInPool.get(3);
    List<LabelValue> labelValuesWithWriteType = new ArrayList<>(labelValues);
    labelValuesWithWriteType.add(NUM_WRITE_SESSIONS);
    assertThat(writePreparedSessions.value()).isEqualTo(0L);
    assertThat(writePreparedSessions.keys()).isEqualTo(SPANNER_LABEL_KEYS_WITH_TYPE);
    assertThat(writePreparedSessions.values()).isEqualTo(labelValuesWithWriteType);

    final CountDownLatch latch = new CountDownLatch(1);
    // Try asynchronously to take another session. This attempt should time out.
    Future<Void> fut =
        executor.submit(
            () -> {
              latch.countDown();
              Session session = pool.getSession();
              session.close();
              return null;
            });
    // Wait until the background thread is actually waiting for a session.
    latch.await();
    // Wait until the request has timed out.
    int waitCount = 0;
    while (pool.getNumWaiterTimeouts() == 0L && waitCount < 5000) {
      //noinspection BusyWait
      Thread.sleep(1L);
      waitCount++;
    }
    assertTrue(pool.getNumWaiterTimeouts() > 0L);
    // Return the checked out session to the pool so the async request will get a session and
    // finish.
    session2.close();
    // Verify that the async request also succeeds.
    fut.get(10L, TimeUnit.SECONDS);
    executor.shutdown();

    session1.close();
    numAcquiredSessions = record.getMetrics().get(METRIC_PREFIX + NUM_ACQUIRED_SESSIONS);
    assertThat(numAcquiredSessions.size()).isEqualTo(2);
    regularSessionMetric =
        numAcquiredSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("false")))
            .findFirst()
            .get();
    multiplexedSessionMetric =
        numAcquiredSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("true")))
            .findFirst()
            .get();
    assertThat(regularSessionMetric.value()).isEqualTo(3L);
    assertThat(multiplexedSessionMetric.value()).isEqualTo(0L);

    numReleasedSessions = record.getMetrics().get(METRIC_PREFIX + NUM_RELEASED_SESSIONS);
    assertThat(numReleasedSessions.size()).isEqualTo(2);
    regularSessionMetric =
        numReleasedSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("false")))
            .findFirst()
            .get();
    multiplexedSessionMetric =
        numReleasedSessions.stream()
            .filter(
                x ->
                    x.keys().contains(IS_MULTIPLEXED_KEY)
                        && x.values().contains(LabelValue.create("true")))
            .findFirst()
            .get();
    assertThat(regularSessionMetric.value()).isEqualTo(3L);
    assertThat(multiplexedSessionMetric.value()).isEqualTo(0L);

    maxInUseSessions = record.getMetrics().get(METRIC_PREFIX + MAX_IN_USE_SESSIONS);
    assertThat(maxInUseSessions.size()).isEqualTo(1);
    assertThat(maxInUseSessions.get(0).value()).isEqualTo(2L);

    numSessionsInPool = record.getMetrics().get(METRIC_PREFIX + NUM_SESSIONS_IN_POOL);
    assertThat(numSessionsInPool.size()).isEqualTo(4);
    beingPrepared = numSessionsInPool.get(0);
    assertThat(beingPrepared.value()).isEqualTo(0L);
    numSessionsInUse = numSessionsInPool.get(1);
    assertThat(numSessionsInUse.value()).isEqualTo(0L);
    readSessions = numSessionsInPool.get(2);
    assertThat(readSessions.value()).isEqualTo(2L);
    writePreparedSessions = numSessionsInPool.get(3);
    assertThat(writePreparedSessions.value()).isEqualTo(0L);
  }

  @Test
  public void testOpenCensusMetricsDisable() {
    SpannerOptions.disableOpenCensusMetrics();
    // Create a session pool with max 2 session and a low timeout for waiting for a session.
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(2)
            .setMaxIdleSessions(0)
            .setInitialWaitForSessionTimeoutMillis(50L)
            .build();
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis.set(System.currentTimeMillis());
    FakeMetricRegistry metricRegistry = new FakeMetricRegistry();
    List<LabelValue> labelValues =
        Arrays.asList(
            LabelValue.create("client1"),
            LabelValue.create("database1"),
            LabelValue.create("instance1"),
            LabelValue.create("1.0.0"));

    setupMockSessionCreation();
    pool = createPool(clock, metricRegistry, labelValues);
    PooledSessionFuture session1 = pool.getSession();
    PooledSessionFuture session2 = pool.getSession();
    session1.get();
    session2.get();

    MetricsRecord record = metricRegistry.pollRecord();
    assertThat(record.getMetrics().size()).isEqualTo(0);
    SpannerOptions.enableOpenCensusMetrics();
  }

  @Test
  public void testOpenTelemetrySessionMetrics() throws Exception {
    SpannerOptions.resetActiveTracingFramework();
    SpannerOptions.enableOpenTelemetryMetrics();
    // Create a session pool with max 2 session and a low timeout for waiting for a session.
    if (minSessions == 1) {
      options =
          SessionPoolOptions.newBuilder()
              .setMinSessions(1)
              .setMaxSessions(3)
              .setMaxIdleSessions(0)
              .setInitialWaitForSessionTimeoutMillis(50L)
              .build();
      FakeClock clock = new FakeClock();
      clock.currentTimeMillis.set(System.currentTimeMillis());

      InMemoryMetricReader inMemoryMetricReader = InMemoryMetricReader.create();
      SdkMeterProvider sdkMeterProvider =
          SdkMeterProvider.builder().registerMetricReader(inMemoryMetricReader).build();
      OpenTelemetry openTelemetry =
          OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();

      setupMockSessionCreation();

      AttributesBuilder attributesBuilder = Attributes.builder();
      attributesBuilder.put("client_id", "testClient");
      attributesBuilder.put("database", "testDb");
      attributesBuilder.put("instance_id", "test_instance");
      attributesBuilder.put("library_version", "test_version");

      pool =
          createPool(
              clock,
              Metrics.getMetricRegistry(),
              SPANNER_DEFAULT_LABEL_VALUES,
              openTelemetry,
              attributesBuilder.build());
      PooledSessionFuture session1 = pool.getSession();
      PooledSessionFuture session2 = pool.getSession();
      session1.get();
      session2.get();

      Collection<MetricData> metricDataCollection = inMemoryMetricReader.collectAllMetrics();
      // Acquired sessions are 2.
      verifyMetricData(metricDataCollection, NUM_ACQUIRED_SESSIONS, 1, 2L);
      // Max in use session are 2.
      verifyMetricData(metricDataCollection, MAX_IN_USE_SESSIONS, 1, 2D);
      // Max Allowed sessions should be 3
      verifyMetricData(metricDataCollection, MAX_ALLOWED_SESSIONS, 1, 3D);
      // Released sessions should be 0
      verifyMetricData(metricDataCollection, NUM_RELEASED_SESSIONS, 1, 0L);
      // Num sessions in pool
      verifyMetricData(metricDataCollection, NUM_SESSIONS_IN_POOL, 1, NUM_SESSIONS_IN_USE, 2);

      PooledSessionFuture session3 = pool.getSession();
      session3.get();

      final CountDownLatch latch = new CountDownLatch(1);
      // Try asynchronously to take another session. This attempt should time out.
      Future<Void> fut =
          executor.submit(
              () -> {
                latch.countDown();
                Session session = pool.getSession();
                session.close();
                return null;
              });
      // Wait until the background thread is actually waiting for a session.
      latch.await();
      // Wait until the request has timed out.
      int waitCount = 0;
      while (pool.getNumWaiterTimeouts() == 0L && waitCount < 1000) {
        Thread.sleep(5L);
        waitCount++;
      }
      // Return the checked out session to the pool so the async request will get a session and
      // finish.
      session2.close();
      // Verify that the async request also succeeds.
      fut.get(10L, TimeUnit.SECONDS);
      executor.shutdown();

      metricDataCollection = inMemoryMetricReader.collectAllMetrics();

      // Max Allowed sessions should be 3
      verifyMetricData(metricDataCollection, MAX_ALLOWED_SESSIONS, 1, 3D);
      // Session timeouts 1
      // verifyMetricData(metricDataCollection, GET_SESSION_TIMEOUTS, 1, 1L);
      // Max in use session are 2.
      verifyMetricData(metricDataCollection, MAX_IN_USE_SESSIONS, 1, 3D);
      // Session released 2
      verifyMetricData(metricDataCollection, NUM_RELEASED_SESSIONS, 1, 2L);
      // Acquired sessions are 4.
      verifyMetricData(metricDataCollection, NUM_ACQUIRED_SESSIONS, 1, 4L);
      // Num sessions in pool
      verifyMetricData(metricDataCollection, NUM_SESSIONS_IN_POOL, 1, NUM_SESSIONS_IN_USE, 2);
      verifyMetricData(metricDataCollection, NUM_SESSIONS_IN_POOL, 1, NUM_SESSIONS_AVAILABLE, 1);
    }
  }

  private static void verifyMetricData(
      Collection<MetricData> metricDataCollection, String metricName, int size, long value) {
    Collection<MetricData> metricDataFiltered =
        metricDataCollection.stream()
            .filter(x -> x.getName().equals(metricName))
            .collect(Collectors.toList());

    assertEquals(metricDataFiltered.size(), size);
    MetricData metricData = metricDataFiltered.stream().findFirst().get();
    LongPointData regularSessionMetric =
        metricData.getLongSumData().getPoints().stream()
            .filter(
                x ->
                    Boolean.FALSE.equals(
                        x.getAttributes().get(AttributeKey.booleanKey("is_multiplexed"))))
            .findFirst()
            .get();
    LongPointData multiplexedSessionMetric =
        metricData.getLongSumData().getPoints().stream()
            .filter(
                x ->
                    Boolean.TRUE.equals(
                        x.getAttributes().get(AttributeKey.booleanKey("is_multiplexed"))))
            .findFirst()
            .get();
    assertEquals(value, regularSessionMetric.getValue());
    assertEquals(0, multiplexedSessionMetric.getValue());
  }

  private static void verifyMetricData(
      Collection<MetricData> metricDataCollection, String metricName, int size, double value) {
    Collection<MetricData> metricDataFiltered =
        metricDataCollection.stream()
            .filter(x -> x.getName().equals(metricName))
            .collect(Collectors.toList());

    assertEquals(metricDataFiltered.size(), size);
    MetricData metricData = metricDataFiltered.stream().findFirst().get();
    assertEquals(
        metricData.getDoubleGaugeData().getPoints().stream().findFirst().get().getValue(),
        value,
        0.0);
  }

  private static void verifyMetricData(
      Collection<MetricData> metricDataCollection,
      String metricName,
      int size,
      String labelName,
      long value) {
    Collection<MetricData> metricDataFiltered =
        metricDataCollection.stream()
            .filter(x -> x.getName().equals(metricName))
            .collect(Collectors.toList());

    assertEquals(metricDataFiltered.size(), size);

    MetricData metricData = metricDataFiltered.stream().findFirst().get();

    assertEquals(
        metricData.getLongSumData().getPoints().stream()
            .filter(x -> x.getAttributes().asMap().containsValue(labelName))
            .findFirst()
            .get()
            .getValue(),
        value);
  }

  @Test
  public void testGetDatabaseRole() throws Exception {
    setupMockSessionCreation();
    pool = createPool(new FakeClock(), new FakeMetricRegistry(), SPANNER_DEFAULT_LABEL_VALUES);
    assertEquals(TEST_DATABASE_ROLE, pool.getDatabaseRole());
  }

  @Test
  public void testWaitOnMinSessionsWhenSessionsAreCreatedBeforeTimeout() {
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(minSessions)
            .setMaxSessions(minSessions + 1)
            .setWaitForMinSessions(Duration.ofSeconds(5))
            .build();
    doAnswer(
            invocation ->
                executor.submit(
                    () -> {
                      SessionConsumerImpl consumer =
                          invocation.getArgument(2, SessionConsumerImpl.class);
                      consumer.onSessionReady(mockSession());
                    }))
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    pool = createPool(new FakeClock(), new FakeMetricRegistry(), SPANNER_DEFAULT_LABEL_VALUES);
    pool.maybeWaitOnMinSessions();
    assertTrue(pool.getNumberOfSessionsInPool() >= minSessions);
  }

  @Test(expected = SpannerException.class)
  public void testWaitOnMinSessionsThrowsExceptionWhenTimeoutIsReached() {
    // Does not call onSessionReady, so session pool is never populated
    doAnswer(invocation -> null)
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(minSessions + 1)
            .setMaxSessions(minSessions + 1)
            .setWaitForMinSessions(Duration.ofMillis(100))
            .build();
    pool = createPool(new FakeClock(), new FakeMetricRegistry(), SPANNER_DEFAULT_LABEL_VALUES);
    pool.maybeWaitOnMinSessions();
  }

  private void mockKeepAlive(ReadContext context) {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, false);
    when(context.executeQuery(any(Statement.class))).thenReturn(resultSet);
  }

  private void mockKeepAlive(Session session) {
    ReadContext context = mock(ReadContext.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, false);
    when(session.singleUse(any(TimestampBound.class))).thenReturn(context);
    when(context.executeQuery(any(Statement.class))).thenReturn(resultSet);
  }

  private void getSessionAsync(final CountDownLatch latch, final AtomicBoolean failed) {
    new Thread(
            () -> {
              try (PooledSessionFuture future = pool.getSession()) {
                PooledSession session = future.get();
                failed.compareAndSet(false, session == null);
                Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
              } catch (Throwable e) {
                failed.compareAndSet(false, true);
              } finally {
                latch.countDown();
              }
            })
        .start();
  }
}
