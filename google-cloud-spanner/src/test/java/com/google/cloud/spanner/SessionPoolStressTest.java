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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.Position;
import com.google.cloud.spanner.SessionPool.SessionConsumerImpl;
import com.google.cloud.spanner.SessionPoolOptions.ActionOnInactiveTransaction;
import com.google.cloud.spanner.SessionPoolOptions.InactiveTransactionRemovalOptions;
import com.google.cloud.spanner.spi.v1.SpannerRpc.Option;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.Empty;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

/**
 * Stress test for {@code SessionPool} which does multiple operations on the pool, making some of
 * them fail and asserts that all the invariants are maintained.
 */
@RunWith(Parameterized.class)
public class SessionPoolStressTest extends BaseSessionPoolTest {

  @Parameter(0)
  public boolean shouldBlock;

  DatabaseId db = DatabaseId.of("projects/p/instances/i/databases/unused");
  SessionPool pool;
  ExecutorService createExecutor = Executors.newSingleThreadExecutor();
  final Object lock = new Object();
  Random random = new Random();
  FakeClock clock = new FakeClock();
  final Map<String, Boolean> sessions = new ConcurrentHashMap<>();
  // Exception keeps track of where the session was closed at.
  Map<String, Exception> closedSessions = new HashMap<>();
  Set<String> expiredSessions = new HashSet<>();
  SpannerImpl mockSpanner;
  SpannerOptions spannerOptions;
  int maxAliveSessions;
  int minSessionsWhenSessionClosed = Integer.MAX_VALUE;
  Exception e;

  @Parameters(name = "should block = {0}")
  public static Collection<Object[]> data() {
    List<Object[]> params = new ArrayList<>();
    params.add(new Object[] {true});
    params.add(new Object[] {false});
    return params;
  }

  private void setupSpanner(DatabaseId db) {
    ReadContext context = mock(ReadContext.class);
    mockSpanner = mock(SpannerImpl.class);
    spannerOptions = mock(SpannerOptions.class);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    when(spannerOptions.getDatabaseRole()).thenReturn("role");
    SessionClient sessionClient = mock(SessionClient.class);
    when(sessionClient.getSpanner()).thenReturn(mockSpanner);
    when(mockSpanner.getSessionClient(db)).thenReturn(sessionClient);
    when(mockSpanner.getOptions()).thenReturn(spannerOptions);
    doAnswer(
            invocation -> {
              createExecutor.submit(
                  () -> {
                    int sessionCount = invocation.getArgument(0, Integer.class);
                    for (int s = 0; s < sessionCount; s++) {
                      SessionImpl session;
                      synchronized (lock) {
                        session = getMockedSession(mockSpanner, context);
                        setupSession(session, context);
                        sessions.put(session.getName(), false);
                        if (sessions.size() > maxAliveSessions) {
                          maxAliveSessions = sessions.size();
                        }
                      }
                      SessionConsumerImpl consumer =
                          invocation.getArgument(2, SessionConsumerImpl.class);
                      consumer.onSessionReady(session);
                    }
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(
            Mockito.anyInt(), Mockito.anyBoolean(), Mockito.any(SessionConsumer.class));
  }

  SessionImpl getMockedSession(SpannerImpl spanner, ReadContext context) {
    Map options = new HashMap<>();
    options.put(Option.CHANNEL_HINT, channelHint.getAndIncrement());
    final SessionImpl session =
        new SessionImpl(
            spanner,
            new SessionReference(
                "projects/dummy/instances/dummy/databases/dummy/sessions/session" + sessionIndex,
                options)) {
          @Override
          public ReadContext singleUse(TimestampBound bound) {
            // The below stubs are added so that we can mock keep-alive.
            return context;
          }

          @Override
          public ApiFuture<Empty> asyncClose() {
            synchronized (lock) {
              if (expiredSessions.contains(this.getName())) {
                return ApiFutures.immediateFailedFuture(
                    SpannerExceptionFactoryTest.newSessionNotFoundException(this.getName()));
              }
              if (sessions.remove(this.getName()) == null) {
                setFailed(closedSessions.get(this.getName()));
              }
              closedSessions.put(this.getName(), new Exception("Session closed at:"));
              if (sessions.size() < minSessionsWhenSessionClosed) {
                minSessionsWhenSessionClosed = sessions.size();
              }
            }
            return ApiFutures.immediateFuture(Empty.getDefaultInstance());
          }
        };
    sessionIndex++;
    return session;
  }

  private void setupSession(final SessionImpl session, final ReadContext mockContext) {
    final ResultSet mockResult = mock(ResultSet.class);
    when(mockContext.executeQuery(any(Statement.class)))
        .thenAnswer(
            invocation -> {
              resetTransaction(session);
              return mockResult;
            });
    when(mockResult.next()).thenReturn(true);
  }

  private void resetTransaction(SessionImpl session) {
    String name = session.getName();
    synchronized (lock) {
      sessions.put(name, false);
    }
  }

  private void setFailed(Exception cause) {
    e = new Exception(cause);
  }

  private void setFailed() {
    e = new Exception();
  }

  private Exception getFailedError() {
    synchronized (lock) {
      return e;
    }
  }

  @Test
  public void stressTest() throws Exception {
    int concurrentThreads = 10;
    final int numOperationsPerThread = 1000;
    final CountDownLatch releaseThreads = new CountDownLatch(1);
    final CountDownLatch threadsDone = new CountDownLatch(concurrentThreads);
    setupSpanner(db);
    int minSessions = 2;
    int maxSessions = concurrentThreads / 2;
    SessionPoolOptions.Builder builder =
        SessionPoolOptions.newBuilder()
            .setPoolMaintainerClock(clock)
            .setMinSessions(minSessions)
            .setMaxSessions(maxSessions)
            .setInactiveTransactionRemovalOptions(
                InactiveTransactionRemovalOptions.newBuilder()
                    .setActionOnInactiveTransaction(ActionOnInactiveTransaction.CLOSE)
                    .build());
    if (shouldBlock) {
      builder.setBlockIfPoolExhausted();
    } else {
      builder.setFailIfPoolExhausted();
    }
    SessionPoolOptions sessionPoolOptions = builder.build();
    when(spannerOptions.getSessionPoolOptions()).thenReturn(sessionPoolOptions);
    pool =
        SessionPool.createPool(
            sessionPoolOptions,
            mockSpanner.getSessionClient(db),
            clock,
            Position.RANDOM,
            new TraceWrapper(Tracing.getTracer(), OpenTelemetry.noop().getTracer(""), false),
            OpenTelemetry.noop());
    pool.idleSessionRemovedListener =
        pooled -> {
          String name = pooled.getName();
          // We do not take the test lock here, as we already hold the session pool lock. Taking the
          // test lock as well here can cause a deadlock.
          sessions.remove(name);
          return null;
        };
    pool.longRunningSessionRemovedListener =
        pooled -> {
          String name = pooled.getName();
          // We do not take the test lock here, as we already hold the session pool lock. Taking the
          // test lock as well here can cause a deadlock.
          sessions.remove(name);
          return null;
        };
    for (int i = 0; i < concurrentThreads; i++) {
      new Thread(
              () -> {
                Uninterruptibles.awaitUninterruptibly(releaseThreads);
                for (int j = 0; j < numOperationsPerThread; j++) {
                  try {
                    PooledSessionFuture session = pool.getSession();
                    session.get();
                    Uninterruptibles.sleepUninterruptibly(random.nextInt(2), TimeUnit.MILLISECONDS);
                    resetTransaction(session.get().delegate);
                    session.close();
                  } catch (SpannerException e) {
                    if (e.getErrorCode() != ErrorCode.RESOURCE_EXHAUSTED || shouldBlock) {
                      setFailed(e);
                    }
                  } catch (Exception e) {
                    setFailed(e);
                  }
                }
                threadsDone.countDown();
              })
          .start();
    }
    // Start maintenance threads in tight loop
    final AtomicBoolean stopMaintenance = new AtomicBoolean(false);
    new Thread(
            () -> {
              while (!stopMaintenance.get()) {
                runMaintenanceLoop(clock, pool, 1);
                // Sleep 1ms between maintenance loops to prevent the long-running session remover
                // from stealing all sessions before they can be used.
                Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.MILLISECONDS);
              }
            })
        .start();
    releaseThreads.countDown();
    threadsDone.await();
    synchronized (lock) {
      assertThat(pool.totalSessions()).isAtMost(maxSessions);
    }
    stopMaintenance.set(true);
    pool.closeAsync(new SpannerImpl.ClosedException()).get();
    Exception e = getFailedError();
    if (e != null) {
      throw e;
    }
  }
}
