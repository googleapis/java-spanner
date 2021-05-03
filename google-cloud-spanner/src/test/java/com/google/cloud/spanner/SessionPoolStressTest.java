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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.SessionConsumerImpl;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
  SessionPoolOptions options;
  ExecutorService createExecutor = Executors.newSingleThreadExecutor();
  Object lock = new Object();
  Random random = new Random();
  FakeClock clock = new FakeClock();

  Map<String, Boolean> sessions = new HashMap<>();
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
    mockSpanner = mock(SpannerImpl.class);
    spannerOptions = mock(SpannerOptions.class);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    SessionClient sessionClient = mock(SessionClient.class);
    when(mockSpanner.getSessionClient(db)).thenReturn(sessionClient);
    when(mockSpanner.getOptions()).thenReturn(spannerOptions);
    doAnswer(
            invocation -> {
              createExecutor.submit(
                  () -> {
                    int sessionCount = invocation.getArgumentAt(0, Integer.class);
                    for (int s = 0; s < sessionCount; s++) {
                      SessionImpl session;
                      synchronized (lock) {
                        session = mockSession();
                        setupSession(session);
                        sessions.put(session.getName(), false);
                        if (sessions.size() > maxAliveSessions) {
                          maxAliveSessions = sessions.size();
                        }
                      }
                      SessionConsumerImpl consumer =
                          invocation.getArgumentAt(2, SessionConsumerImpl.class);
                      consumer.onSessionReady(session);
                    }
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(
            Mockito.anyInt(), Mockito.anyBoolean(), Mockito.any(SessionConsumer.class));
  }

  private void setupSession(final SessionImpl session) {
    ReadContext mockContext = mock(ReadContext.class);
    final ResultSet mockResult = mock(ResultSet.class);
    when(session.singleUse(any(TimestampBound.class))).thenReturn(mockContext);
    when(mockContext.executeQuery(any(Statement.class)))
        .thenAnswer(
            invocation -> {
              resetTransaction(session);
              return mockResult;
            });
    when(mockResult.next()).thenReturn(true);
    doAnswer(
            invocation -> {
              synchronized (lock) {
                if (expiredSessions.contains(session.getName())) {
                  return ApiFutures.immediateFailedFuture(
                      SpannerExceptionFactoryTest.newSessionNotFoundException(session.getName()));
                }
                if (sessions.remove(session.getName()) == null) {
                  setFailed(closedSessions.get(session.getName()));
                }
                closedSessions.put(session.getName(), new Exception("Session closed at:"));
                if (sessions.size() < minSessionsWhenSessionClosed) {
                  minSessionsWhenSessionClosed = sessions.size();
                }
              }
              return ApiFutures.immediateFuture(Empty.getDefaultInstance());
            })
        .when(session)
        .asyncClose();

    doAnswer(
            invocation -> {
              if (random.nextInt(100) < 10) {
                expireSession(session);
                throw SpannerExceptionFactoryTest.newSessionNotFoundException(session.getName());
              }
              String name = session.getName();
              synchronized (lock) {
                if (sessions.put(name, true)) {
                  setFailed();
                }
                session.readyTransactionId = ByteString.copyFromUtf8("foo");
              }
              return null;
            })
        .when(session)
        .prepareReadWriteTransaction();
    when(session.hasReadyTransaction()).thenCallRealMethod();
  }

  private void expireSession(Session session) {
    String name = session.getName();
    synchronized (lock) {
      sessions.remove(name);
      expiredSessions.add(name);
    }
  }

  private void resetTransaction(SessionImpl session) {
    String name = session.getName();
    synchronized (lock) {
      session.readyTransactionId = null;
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
        SessionPoolOptions.newBuilder().setMinSessions(minSessions).setMaxSessions(maxSessions);
    if (shouldBlock) {
      builder.setBlockIfPoolExhausted();
    } else {
      builder.setFailIfPoolExhausted();
    }
    pool =
        SessionPool.createPool(
            builder.build(), new TestExecutorFactory(), mockSpanner.getSessionClient(db), clock);
    pool.idleSessionRemovedListener =
        pooled -> {
          String name = pooled.getName();
          synchronized (lock) {
            sessions.remove(name);
            return null;
          }
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
              }
            })
        .start();
    releaseThreads.countDown();
    threadsDone.await();
    synchronized (lock) {
      assertThat(maxAliveSessions).isAtMost(maxSessions);
    }
    stopMaintenance.set(true);
    pool.closeAsync(new SpannerImpl.ClosedException()).get();
    Exception e = getFailedError();
    if (e != null) {
      throw e;
    }
  }
}
