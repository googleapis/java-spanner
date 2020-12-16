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

import static com.google.cloud.spanner.MetricRegistryConstants.NUM_IN_USE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_READ_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_SESSIONS_BEING_PREPARED;
import static com.google.cloud.spanner.MetricRegistryConstants.NUM_WRITE_SESSIONS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS;
import static com.google.cloud.spanner.MetricRegistryConstants.SPANNER_LABEL_KEYS_WITH_TYPE;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
import com.google.cloud.spanner.SessionPool.Clock;
import com.google.cloud.spanner.SessionPool.PooledSession;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.SessionConsumerImpl;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.spi.v1.SpannerRpc.ResultStreamConsumer;
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
import io.opencensus.trace.Span;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Tests for SessionPool that mock out the underlying stub. */
@RunWith(Parameterized.class)
public class SessionPoolTest extends BaseSessionPoolTest {

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  @Parameter public int minSessions;

  @Mock SpannerImpl client;
  @Mock SessionClient sessionClient;
  @Mock SpannerOptions spannerOptions;
  DatabaseId db = DatabaseId.of("projects/p/instances/i/databases/unused");
  SessionPool pool;
  SessionPoolOptions options;
  private String sessionName = String.format("%s/sessions/s", db.getName());

  @Parameters(name = "min sessions = {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{0}, {1}});
  }

  private SessionPool createPool() {
    return SessionPool.createPool(options, new TestExecutorFactory(), client.getSessionClient(db));
  }

  private SessionPool createPool(Clock clock) {
    return SessionPool.createPool(
        options, new TestExecutorFactory(), client.getSessionClient(db), clock);
  }

  private SessionPool createPool(
      Clock clock, MetricRegistry metricRegistry, List<LabelValue> labelValues) {
    return SessionPool.createPool(
        options,
        new TestExecutorFactory(),
        client.getSessionClient(db),
        clock,
        metricRegistry,
        labelValues);
  }

  @Before
  public void setUp() {
    initMocks(this);
    when(client.getOptions()).thenReturn(spannerOptions);
    when(client.getSessionClient(db)).thenReturn(sessionClient);
    when(spannerOptions.getNumChannels()).thenReturn(4);
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        int sessionCount = invocation.getArgumentAt(0, Integer.class);
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        for (int i = 0; i < sessionCount; i++) {
                          consumer.onSessionReady(mockSession());
                        }
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(
            Mockito.anyInt(), Mockito.anyBoolean(), any(SessionConsumer.class));
  }

  @Test
  public void testClosedPoolIncludesClosedException() {
    pool = createPool();
    assertThat(pool.isValid()).isTrue();
    closePoolWithStacktrace();
    try {
      pool.getSession();
      fail("missing expected exception");
    } catch (IllegalStateException e) {
      assertThat(e.getCause()).isInstanceOf(ClosedException.class);
      StringWriter sw = new StringWriter();
      e.getCause().printStackTrace(new PrintWriter(sw));
      assertThat(sw.toString()).contains("closePoolWithStacktrace");
    }
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
    pool = createPool();
    Session session1 = pool.getSession().get();
    Session session2 = pool.getSession().get();
    assertThat(session1).isNotEqualTo(session2);

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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(sessions.pop());
                      }
                    });
                return null;
              }
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
            new Runnable() {

              @Override
              public void run() {
                // Run in a tight loop.
                while (!stop.get()) {
                  runMaintainanceLoop(clock, pool, 1);
                }
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(session1);
                      }
                    });
                return null;
              }
            })
        .doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Callable<Void>() {
                      @Override
                      public Void call() throws Exception {
                        insideCreation.countDown();
                        releaseCreation.await();
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(session2);
                        return null;
                      }
                    });
                return null;
              }
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(session1);
                      }
                    });
                return null;
              }
            })
        .doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Callable<Void>() {
                      @Override
                      public Void call() throws Exception {
                        insideCreation.countDown();
                        releaseCreation.await();
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(session2);
                        return null;
                      }
                    });
                return null;
              }
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Callable<Void>() {
                      @Override
                      public Void call() throws Exception {
                        insideCreation.countDown();
                        releaseCreation.await();
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionCreateFailure(
                            SpannerExceptionFactory.newSpannerException(new RuntimeException()), 1);
                        return null;
                      }
                    });
                return null;
              }
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(session);
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    PooledSessionFuture leakedSession = pool.getSession();
    leakedSession.get();
    // Suppress expected leakedSession warning.
    leakedSession.clearLeakedException();
    pool.closeAsync(new SpannerImpl.ClosedException());
    try {
      pool.getSession();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Callable<Void>() {
                      @Override
                      public Void call() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionCreateFailure(
                            SpannerExceptionFactory.newSpannerException(ErrorCode.INTERNAL, ""), 1);
                        return null;
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    try {
      pool.getSession().get();
      fail("Expected exception");
    } catch (SpannerException ex) {
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL);
    }
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(mockSession());
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    pool = createPool();
    Session session1 = pool.getSession();
    try {
      pool.getSession();
      fail("Expected exception");
    } catch (SpannerException ex) {
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
    }
    session1.close();
    session1 = pool.getSession();
    assertThat(session1).isNotNull();
    session1.close();
  }

  @Test
  public void idleSessionCleanup() throws Exception {
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(3)
            .setIncStep(1)
            .setMaxIdleSessions(0)
            .build();
    SessionImpl session1 = mockSession();
    SessionImpl session2 = mockSession();
    SessionImpl session3 = mockSession();
    final LinkedList<SessionImpl> sessions =
        new LinkedList<>(Arrays.asList(session1, session2, session3));
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(sessions.pop());
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    for (SessionImpl session : sessions) {
      mockKeepAlive(session);
    }
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
    pool = createPool(clock);
    // Make sure pool has been initialized
    pool.getSession().close();
    runMaintainanceLoop(clock, pool, pool.poolMaintainer.numClosureCycles);
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
    runMaintainanceLoop(clock, pool, pool.poolMaintainer.numClosureCycles);
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
    runMaintainanceLoop(clock, pool, cycles);
    // We will still close 2 sessions since at any point in time only 1 session was in use.
    assertThat(pool.numIdleSessionsRemoved()).isEqualTo(2L);
    pool.closeAsync(new SpannerImpl.ClosedException()).get(5L, TimeUnit.SECONDS);
  }

  @Test
  public void keepAlive() throws Exception {
    options = SessionPoolOptions.newBuilder().setMinSessions(2).setMaxSessions(3).build();
    final SessionImpl session = mockSession();
    mockKeepAlive(session);
    // This is cheating as we are returning the same session each but it makes the verification
    // easier.
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        int sessionCount = invocation.getArgumentAt(0, Integer.class);
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        for (int i = 0; i < sessionCount; i++) {
                          consumer.onSessionReady(session);
                        }
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(anyInt(), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
    pool = createPool(clock);
    PooledSessionFuture session1 = pool.getSession();
    PooledSessionFuture session2 = pool.getSession();
    session1.get();
    session2.get();
    session1.close();
    session2.close();
    runMaintainanceLoop(clock, pool, pool.poolMaintainer.numKeepAliveCycles);
    verify(session, never()).singleUse(any(TimestampBound.class));
    runMaintainanceLoop(clock, pool, pool.poolMaintainer.numKeepAliveCycles);
    verify(session, times(2)).singleUse(any(TimestampBound.class));
    clock.currentTimeMillis +=
        clock.currentTimeMillis + (options.getKeepAliveIntervalMinutes() + 5) * 60 * 1000;
    session1 = pool.getSession();
    session1.writeAtLeastOnce(new ArrayList<Mutation>());
    session1.close();
    runMaintainanceLoop(clock, pool, pool.poolMaintainer.numKeepAliveCycles);
    // The session pool only keeps MinSessions + MaxIdleSessions alive.
    verify(session, times(options.getMinSessions() + options.getMaxIdleSessions()))
        .singleUse(any(TimestampBound.class));
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
            new Callable<Void>() {
              @Override
              public Void call() {
                latch.countDown();
                PooledSessionFuture session = pool.getSession();
                session.close();
                return null;
              }
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(closedSession);
                      }
                    });
                return null;
              }
            })
        .doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(openSession);
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(closedSession);
                      }
                    });
                return null;
              }
            })
        .doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(openSession);
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
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
    EXCEPTION;
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
              any(ExecuteSqlRequest.class), any(ResultStreamConsumer.class), any(Map.class)))
          .thenReturn(closedStreamingCall);
      when(rpc.executeQuery(any(ExecuteSqlRequest.class), any(Map.class)))
          .thenThrow(sessionNotFound);
      when(rpc.executeBatchDml(any(ExecuteBatchDmlRequest.class), any(Map.class)))
          .thenThrow(sessionNotFound);
      when(rpc.commitAsync(any(CommitRequest.class), any(Map.class)))
          .thenReturn(ApiFutures.<CommitResponse>immediateFailedFuture(sessionNotFound));
      when(rpc.rollbackAsync(any(RollbackRequest.class), any(Map.class)))
          .thenReturn(ApiFutures.<Empty>immediateFailedFuture(sessionNotFound));
      final SessionImpl closedSession = mock(SessionImpl.class);
      when(closedSession.getName())
          .thenReturn("projects/dummy/instances/dummy/database/dummy/sessions/session-closed");
      final TransactionContextImpl closedTransactionContext =
          TransactionContextImpl.newBuilder()
              .setSession(closedSession)
              .setOptions(Options.fromTransactionOptions())
              .setRpc(rpc)
              .build();
      when(closedSession.asyncClose())
          .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
      when(closedSession.newTransaction(Options.fromTransactionOptions()))
          .thenReturn(closedTransactionContext);
      when(closedSession.beginTransactionAsync()).thenThrow(sessionNotFound);
      TransactionRunnerImpl closedTransactionRunner =
          new TransactionRunnerImpl(closedSession, rpc, 10);
      closedTransactionRunner.setSpan(mock(Span.class));
      when(closedSession.readWriteTransaction()).thenReturn(closedTransactionRunner);

      final SessionImpl openSession = mock(SessionImpl.class);
      when(openSession.asyncClose())
          .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
      when(openSession.getName())
          .thenReturn("projects/dummy/instances/dummy/database/dummy/sessions/session-open");
      final TransactionContextImpl openTransactionContext = mock(TransactionContextImpl.class);
      when(openSession.newTransaction(Options.fromTransactionOptions()))
          .thenReturn(openTransactionContext);
      when(openSession.beginTransactionAsync())
          .thenReturn(ApiFutures.immediateFuture(ByteString.copyFromUtf8("open-txn")));
      TransactionRunnerImpl openTransactionRunner =
          new TransactionRunnerImpl(openSession, mock(SpannerRpc.class), 10);
      openTransactionRunner.setSpan(mock(Span.class));
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

      doAnswer(
              new Answer<Void>() {
                @Override
                public Void answer(final InvocationOnMock invocation) {
                  executor.submit(
                      new Runnable() {
                        @Override
                        public void run() {
                          SessionConsumerImpl consumer =
                              invocation.getArgumentAt(2, SessionConsumerImpl.class);
                          consumer.onSessionReady(closedSession);
                        }
                      });
                  return null;
                }
              })
          .doAnswer(
              new Answer<Void>() {
                @Override
                public Void answer(final InvocationOnMock invocation) {
                  executor.submit(
                      new Runnable() {
                        @Override
                        public void run() {
                          SessionConsumerImpl consumer =
                              invocation.getArgumentAt(2, SessionConsumerImpl.class);
                          consumer.onSessionReady(openSession);
                        }
                      });
                  return null;
                }
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
      when(spanner.getOptions()).thenReturn(spannerOptions);
      SessionPool pool =
          SessionPool.createPool(options, new TestExecutorFactory(), spanner.getSessionClient(db));
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
    List<Mutation> mutations = Arrays.asList(Mutation.newInsertBuilder("FOO").build());
    final SessionImpl closedSession = mockSession();
    when(closedSession.write(mutations)).thenThrow(sessionNotFound);

    final SessionImpl openSession = mockSession();
    when(openSession.write(mutations)).thenReturn(Timestamp.now());
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(closedSession);
                      }
                    });
                return null;
              }
            })
        .doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(openSession);
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));

    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
    pool = createPool(clock);
    DatabaseClientImpl impl = new DatabaseClientImpl(pool);
    assertThat(impl.write(mutations)).isNotNull();
  }

  @Test
  public void testSessionNotFoundWriteAtLeastOnce() {
    SpannerException sessionNotFound =
        SpannerExceptionFactoryTest.newSessionNotFoundException(sessionName);
    List<Mutation> mutations = Arrays.asList(Mutation.newInsertBuilder("FOO").build());
    final SessionImpl closedSession = mockSession();
    when(closedSession.writeAtLeastOnce(mutations)).thenThrow(sessionNotFound);

    final SessionImpl openSession = mockSession();
    when(openSession.writeAtLeastOnce(mutations)).thenReturn(Timestamp.now());
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(closedSession);
                      }
                    });
                return null;
              }
            })
        .doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(openSession);
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
    pool = createPool(clock);
    DatabaseClientImpl impl = new DatabaseClientImpl(pool);
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
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(closedSession);
                      }
                    });
                return null;
              }
            })
        .doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(final InvocationOnMock invocation) {
                executor.submit(
                    new Runnable() {
                      @Override
                      public void run() {
                        SessionConsumerImpl consumer =
                            invocation.getArgumentAt(2, SessionConsumerImpl.class);
                        consumer.onSessionReady(openSession);
                      }
                    });
                return null;
              }
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(Mockito.eq(1), Mockito.anyBoolean(), any(SessionConsumer.class));
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
    pool = createPool(clock);
    DatabaseClientImpl impl = new DatabaseClientImpl(pool);
    assertThat(impl.executePartitionedUpdate(statement)).isEqualTo(1L);
  }

  @Test
  public void testSessionMetrics() throws Exception {
    // Create a session pool with max 2 session and a low timeout for waiting for a session.
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxSessions(2)
            .setMaxIdleSessions(0)
            .setInitialWaitForSessionTimeoutMillis(50L)
            .build();
    FakeClock clock = new FakeClock();
    clock.currentTimeMillis = System.currentTimeMillis();
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
        record.getMetrics().get(MetricRegistryConstants.MAX_IN_USE_SESSIONS);
    assertThat(maxInUseSessions.size()).isEqualTo(1);
    assertThat(maxInUseSessions.get(0).value()).isEqualTo(2L);
    assertThat(maxInUseSessions.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(maxInUseSessions.get(0).values()).isEqualTo(labelValues);

    List<PointWithFunction> getSessionsTimeouts =
        record.getMetrics().get(MetricRegistryConstants.GET_SESSION_TIMEOUTS);
    assertThat(getSessionsTimeouts.size()).isEqualTo(1);
    assertThat(getSessionsTimeouts.get(0).value()).isAtMost(1L);
    assertThat(getSessionsTimeouts.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(getSessionsTimeouts.get(0).values()).isEqualTo(labelValues);

    List<PointWithFunction> numAcquiredSessions =
        record.getMetrics().get(MetricRegistryConstants.NUM_ACQUIRED_SESSIONS);
    assertThat(numAcquiredSessions.size()).isEqualTo(1);
    assertThat(numAcquiredSessions.get(0).value()).isEqualTo(2L);
    assertThat(numAcquiredSessions.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(numAcquiredSessions.get(0).values()).isEqualTo(labelValues);

    List<PointWithFunction> numReleasedSessions =
        record.getMetrics().get(MetricRegistryConstants.NUM_RELEASED_SESSIONS);
    assertThat(numReleasedSessions.size()).isEqualTo(1);
    assertThat(numReleasedSessions.get(0).value()).isEqualTo(0);
    assertThat(numReleasedSessions.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(numReleasedSessions.get(0).values()).isEqualTo(labelValues);

    List<PointWithFunction> maxAllowedSessions =
        record.getMetrics().get(MetricRegistryConstants.MAX_ALLOWED_SESSIONS);
    assertThat(maxAllowedSessions.size()).isEqualTo(1);
    assertThat(maxAllowedSessions.get(0).value()).isEqualTo(options.getMaxSessions());
    assertThat(maxAllowedSessions.get(0).keys()).isEqualTo(SPANNER_LABEL_KEYS);
    assertThat(maxAllowedSessions.get(0).values()).isEqualTo(labelValues);

    List<PointWithFunction> numSessionsInPool =
        record.getMetrics().get(MetricRegistryConstants.NUM_SESSIONS_IN_POOL);
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
            new Callable<Void>() {
              @Override
              public Void call() {
                latch.countDown();
                Session session = pool.getSession();
                session.close();
                return null;
              }
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

    session1.close();
    numAcquiredSessions = record.getMetrics().get(MetricRegistryConstants.NUM_ACQUIRED_SESSIONS);
    assertThat(numAcquiredSessions.size()).isEqualTo(1);
    assertThat(numAcquiredSessions.get(0).value()).isEqualTo(3L);

    numReleasedSessions = record.getMetrics().get(MetricRegistryConstants.NUM_RELEASED_SESSIONS);
    assertThat(numReleasedSessions.size()).isEqualTo(1);
    assertThat(numReleasedSessions.get(0).value()).isEqualTo(3L);

    maxInUseSessions = record.getMetrics().get(MetricRegistryConstants.MAX_IN_USE_SESSIONS);
    assertThat(maxInUseSessions.size()).isEqualTo(1);
    assertThat(maxInUseSessions.get(0).value()).isEqualTo(2L);

    numSessionsInPool = record.getMetrics().get(MetricRegistryConstants.NUM_SESSIONS_IN_POOL);
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

  private void mockKeepAlive(Session session) {
    ReadContext context = mock(ReadContext.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, false);
    when(session.singleUse(any(TimestampBound.class))).thenReturn(context);
    when(context.executeQuery(any(Statement.class))).thenReturn(resultSet);
  }

  private void getSessionAsync(final CountDownLatch latch, final AtomicBoolean failed) {
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                try (PooledSessionFuture future = pool.getSession()) {
                  PooledSession session = future.get();
                  failed.compareAndSet(false, session == null);
                  Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                } catch (Throwable e) {
                  failed.compareAndSet(false, true);
                } finally {
                  latch.countDown();
                }
              }
            })
        .start();
  }
}
