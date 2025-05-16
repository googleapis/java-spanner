/*
 * Copyright 2020 Google LLC
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SessionPool.PooledSession;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.Position;
import com.google.cloud.spanner.SessionPool.SessionConsumerImpl;
import com.google.common.base.Preconditions;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class SessionPoolMaintainerTest extends BaseSessionPoolTest {
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private @Mock SpannerImpl client;
  private @Mock SessionClient sessionClient;
  private @Mock SpannerOptions spannerOptions;
  private DatabaseId db = DatabaseId.of("projects/p/instances/i/databases/unused");
  private SessionPoolOptions options;
  private FakeClock clock = new FakeClock();
  private List<PooledSession> idledSessions = new ArrayList<>();
  private Map<String, Integer> pingedSessions = new HashMap<>();

  @Before
  public void setUp() {
    initMocks(this);
    when(client.getOptions()).thenReturn(spannerOptions);
    when(client.getSessionClient(db)).thenReturn(sessionClient);
    when(sessionClient.getSpanner()).thenReturn(client);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    when(spannerOptions.getDatabaseRole()).thenReturn("role");
    setupMockSessionCreation();
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxIdleSessions(1)
            .setMaxSessions(5)
            .setIncStep(1)
            .setKeepAliveIntervalMinutes(2)
            .setPoolMaintainerClock(clock)
            .build();
    when(spannerOptions.getSessionPoolOptions()).thenReturn(options);
    idledSessions.clear();
    pingedSessions.clear();
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
                      ReadContext mockContext = mock(ReadContext.class);
                      consumer.onSessionReady(
                          setupMockSession(buildMockSession(client, mockContext), mockContext));
                    }
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(
            Mockito.anyInt(), Mockito.anyBoolean(), any(SessionConsumer.class));
  }

  private SessionImpl setupMockSession(final SessionImpl session, final ReadContext mockContext) {
    final ResultSet mockResult = mock(ResultSet.class);
    when(mockContext.executeQuery(any(Statement.class)))
        .thenAnswer(
            invocation -> {
              Integer currentValue = pingedSessions.get(session.getName());
              if (currentValue == null) {
                currentValue = 0;
              }
              pingedSessions.put(session.getName(), ++currentValue);
              return mockResult;
            });
    when(mockResult.next()).thenReturn(true);
    return session;
  }

  private SessionPool createPool() throws Exception {
    return createPool(this.options);
  }

  private SessionPool createPool(SessionPoolOptions options) throws Exception {
    // Allow sessions to be added to the head of the pool in all cases in this test, as it is
    // otherwise impossible to know which session exactly is getting pinged at what point in time.
    SessionPool pool =
        SessionPool.createPool(
            options,
            new TestExecutorFactory(),
            client.getSessionClient(db),
            clock,
            Position.FIRST,
            new TraceWrapper(Tracing.getTracer(), OpenTelemetry.noop().getTracer(""), false),
            OpenTelemetry.noop());
    pool.idleSessionRemovedListener =
        input -> {
          idledSessions.add(input);
          return null;
        };
    // Wait until pool has initialized.
    while (pool.totalSessions() < options.getMinSessions()) {
      Thread.sleep(1L);
    }
    return pool;
  }

  @Test
  public void testKeepAlive() throws Exception {
    SessionPool pool = createPool();
    assertThat(pingedSessions).isEmpty();
    // Run one maintenance loop. No sessions should get a keep-alive ping.
    runMaintenanceLoop(clock, pool, 1);
    assertThat(pingedSessions).isEmpty();

    // Checkout two sessions and do a maintenance loop. Still no sessions should be getting any
    // pings.
    Session session1 = pool.getSession();
    Session session2 = pool.getSession();
    runMaintenanceLoop(clock, pool, 1);
    assertThat(pingedSessions).isEmpty();

    // Check the sessions back into the pool and do a maintenance loop.
    session2.close();
    session1.close();
    runMaintenanceLoop(clock, pool, 1);
    assertThat(pingedSessions).isEmpty();

    // Now advance the time enough for both sessions in the pool to be idled. Then do one
    // maintenance loop. This should cause the last session to have been checked back into the pool
    // to get a ping, but not the second session.
    clock.currentTimeMillis.addAndGet(
        TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1);
    runMaintenanceLoop(clock, pool, 1);
    assertThat(pingedSessions).containsExactly(session1.getName(), 1);
    // Do another maintenance loop. This should cause the other session to also get a ping.
    runMaintenanceLoop(clock, pool, 1);
    assertThat(pingedSessions).containsExactly(session1.getName(), 1, session2.getName(), 1);

    // Now check out three sessions so the pool will create an additional session. The pool will
    // only keep 2 sessions alive, as that is the setting for MinSessions.
    Session session3 = pool.getSession();
    Session session4 = pool.getSession();
    Session session5 = pool.getSession();
    // Pinging a session will put it at the back of the pool. A session that needed a ping to be
    // kept alive is not one that should be preferred for use. This means that session2 is the last
    // session in the pool, and session1 the second-to-last.
    assertEquals(session1.getName(), session3.getName());
    assertEquals(session2.getName(), session4.getName());
    session5.close();
    session4.close();
    session3.close();
    // Advance the clock to force pings for the sessions in the pool and do three maintenance loops.
    // This should ping the sessions in the following order:
    // 1. session3 (=session1)
    // 2. session4 (=session2)
    // The pinged sessions already contains: {session1: 1, session2: 1}
    // Note that the pool only pings up to MinSessions sessions.
    clock.currentTimeMillis.addAndGet(
        TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1);
    runMaintenanceLoop(clock, pool, 3);
    assertThat(pingedSessions).containsExactly(session1.getName(), 2, session2.getName(), 2);

    // Advance the clock to idle all sessions in the pool again and then check out one session. This
    // should cause only one session to get a ping.
    clock.currentTimeMillis.addAndGet(
        TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1);
    // This will be session1, as all sessions were pinged in the previous 3 maintenance loops, and
    // this will have brought session1 back to the front of the pool.
    Session session6 = pool.getSession();
    // The session that was first in the pool now is equal to the initial first session as each full
    // round of pings will swap the order of the first MinSessions sessions in the pool.
    assertThat(session6.getName()).isEqualTo(session1.getName());
    runMaintenanceLoop(clock, pool, 3);
    // Running 3 cycles will only ping the 2 sessions in the pool once.
    assertThat(pool.totalSessions()).isEqualTo(3);
    assertThat(pingedSessions).containsExactly(session1.getName(), 2, session2.getName(), 3);
    // Update the last use date and release the session to the pool and do another maintenance
    // cycle. This should not ping any sessions.
    ((PooledSessionFuture) session6).get().markUsed();
    session6.close();
    runMaintenanceLoop(clock, pool, 3);
    assertThat(pingedSessions).containsExactly(session1.getName(), 2, session2.getName(), 3);

    // Now check out 3 sessions again and make sure the 'extra' session is checked in last. That
    // will make it eligible for pings.
    Session session7 = pool.getSession();
    Session session8 = pool.getSession();
    Session session9 = pool.getSession();

    assertThat(session7.getName()).isEqualTo(session1.getName());
    assertThat(session8.getName()).isEqualTo(session2.getName());
    assertThat(session9.getName()).isEqualTo(session5.getName());

    session7.close();
    session8.close();
    session9.close();

    clock.currentTimeMillis.addAndGet(
        TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1);
    runMaintenanceLoop(clock, pool, 3);
    // session1 will not get a ping this time, as it was checked in first and is now the last
    // session in the pool.
    assertThat(pingedSessions)
        .containsExactly(session1.getName(), 2, session2.getName(), 4, session5.getName(), 1);
  }

  @Test
  public void testIdleSessions() throws Exception {
    SessionPool pool = createPool();
    long loopsToIdleSessions =
        Double.valueOf(
                    Math.ceil(
                        (double) options.getRemoveInactiveSessionAfter().toMillis()
                            / pool.poolMaintainer.loopFrequency))
                .longValue()
            + 2L;
    assertThat(idledSessions).isEmpty();
    // Run one maintenance loop. No sessions should be removed from the pool.
    runMaintenanceLoop(clock, pool, 1);
    assertThat(idledSessions).isEmpty();

    // Checkout two sessions and do a maintenance loop. Still no sessions should be removed.
    Session session1 = pool.getSession();
    Session session2 = pool.getSession();
    runMaintenanceLoop(clock, pool, 1);
    assertThat(idledSessions).isEmpty();

    // Check the sessions back into the pool and do a maintenance loop.
    session2.close();
    session1.close();
    runMaintenanceLoop(clock, pool, 1);
    assertThat(idledSessions).isEmpty();

    // Now advance the time enough for both sessions in the pool to be idled. Both sessions should
    // be kept alive by the maintainer and remain in the pool.
    runMaintenanceLoop(clock, pool, loopsToIdleSessions);
    assertThat(idledSessions).isEmpty();

    // Now check out three sessions so the pool will create an additional session. The pool will
    // only keep 2 sessions alive, as that is the setting for MinSessions.
    Session session3 = pool.getSession().get();
    Session session4 = pool.getSession().get();
    Session session5 = pool.getSession().get();
    // Note that pinging sessions does not change the order of the pool. This means that session2
    // is still the last session in the pool.
    assertThat(session3.getName()).isEqualTo(session1.getName());
    assertThat(session4.getName()).isEqualTo(session2.getName());
    session5.close();
    session4.close();
    session3.close();
    // Advance the clock to idle sessions. The pool will keep session4 and session3 alive, session5
    // will be idled and removed.
    runMaintenanceLoop(clock, pool, loopsToIdleSessions);
    assertThat(idledSessions).containsExactly(session5);
    assertThat(pool.totalSessions()).isEqualTo(2);

    // Check out three sessions again and keep one session checked out.
    Session session6 = pool.getSession().get();
    Session session7 = pool.getSession().get();
    Session session8 = pool.getSession().get();
    session8.close();
    session7.close();
    // Now advance the clock to idle sessions. This should remove session8 from the pool.
    runMaintenanceLoop(clock, pool, loopsToIdleSessions);
    assertThat(idledSessions).containsExactly(session5, session8);
    assertThat(pool.totalSessions()).isEqualTo(2);
    ((PooledSession) session6).markUsed();
    session6.close();

    // Check out three sessions and keep them all checked out. No sessions should be removed from
    // the pool.
    Session session9 = pool.getSession().get();
    Session session10 = pool.getSession().get();
    Session session11 = pool.getSession().get();
    runMaintenanceLoop(clock, pool, loopsToIdleSessions);
    assertThat(idledSessions).containsExactly(session5, session8);
    assertThat(pool.totalSessions()).isEqualTo(3);
    // Return the sessions to the pool. As they have not been used, they are all into idle time.
    // Running the maintainer will now remove all the sessions from the pool and then start the
    // replenish method.
    session9.close();
    session10.close();
    session11.close();
    runMaintenanceLoop(clock, pool, 1);
    assertThat(idledSessions).containsExactly(session5, session8, session9, session10, session11);
    // Check that the pool is replenished.
    while (pool.totalSessions() < options.getMinSessions()) {
      Thread.sleep(1L);
    }
    assertThat(pool.totalSessions()).isEqualTo(options.getMinSessions());
  }

  @Test
  public void testRandomizeThreshold() throws Exception {
    SessionPool pool =
        createPool(
            this.options.toBuilder()
                .setMaxSessions(400)
                .setLoopFrequency(1000L)
                .setRandomizePositionQPSThreshold(4)
                .build());
    List<Session> sessions;

    // Run a maintenance loop. No sessions have been checked out so far, so the TPS should be 0.
    runMaintenanceLoop(clock, pool, 1);
    assertFalse(pool.shouldRandomize());

    // Get and return one session. This means TPS == 1.
    returnSessions(1, useSessions(1, pool));
    runMaintenanceLoop(clock, pool, 1);
    assertFalse(pool.shouldRandomize());

    // Get and return four sessions. This means TPS == 4, and that no sessions are checked out.
    returnSessions(4, useSessions(4, pool));
    runMaintenanceLoop(clock, pool, 1);
    assertFalse(pool.shouldRandomize());

    // Get four sessions without returning them.
    // This means TPS == 4 and that they are all still checked out.
    sessions = useSessions(4, pool);
    runMaintenanceLoop(clock, pool, 1);
    assertTrue(pool.shouldRandomize());
    // Returning one of the sessions reduces the number of checked out sessions enough to stop the
    // randomizing.
    returnSessions(1, sessions);
    runMaintenanceLoop(clock, pool, 1);
    assertFalse(pool.shouldRandomize());

    // Get three more session and run the maintenance loop.
    // The TPS is then 3, as we've only gotten 3 sessions since the last maintenance run.
    // That means that we should not randomize.
    sessions.addAll(useSessions(3, pool));
    runMaintenanceLoop(clock, pool, 1);
    assertFalse(pool.shouldRandomize());

    returnSessions(sessions.size(), sessions);
  }

  private List<Session> useSessions(int numSessions, SessionPool pool) {
    List<Session> sessions = new ArrayList<>(numSessions);
    for (int i = 0; i < numSessions; i++) {
      sessions.add(pool.getSession());
      sessions.get(sessions.size() - 1).singleUse().executeQuery(Statement.of("SELECT 1")).next();
    }
    return sessions;
  }

  private void returnSessions(int numSessions, List<Session> sessions) {
    Preconditions.checkArgument(numSessions <= sessions.size());
    for (int i = 0; i < numSessions; i++) {
      sessions.remove(0).close();
    }
  }
}
