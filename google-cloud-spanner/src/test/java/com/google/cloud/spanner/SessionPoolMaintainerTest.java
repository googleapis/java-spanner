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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.cloud.spanner.SessionClient.SessionConsumer;
import com.google.cloud.spanner.SessionPool.PooledSession;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.SessionConsumerImpl;
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
    when(spannerOptions.getNumChannels()).thenReturn(4);
    setupMockSessionCreation();
    options =
        SessionPoolOptions.newBuilder()
            .setMinSessions(1)
            .setMaxIdleSessions(1)
            .setMaxSessions(5)
            .setIncStep(1)
            .setKeepAliveIntervalMinutes(2)
            .build();
    idledSessions.clear();
    pingedSessions.clear();
  }

  private void setupMockSessionCreation() {
    doAnswer(
            invocation -> {
              executor.submit(
                  () -> {
                    int sessionCount = invocation.getArgumentAt(0, Integer.class);
                    SessionConsumerImpl consumer =
                        invocation.getArgumentAt(2, SessionConsumerImpl.class);
                    for (int i = 0; i < sessionCount; i++) {
                      consumer.onSessionReady(setupMockSession(mockSession()));
                    }
                  });
              return null;
            })
        .when(sessionClient)
        .asyncBatchCreateSessions(
            Mockito.anyInt(), Mockito.anyBoolean(), any(SessionConsumer.class));
  }

  private SessionImpl setupMockSession(final SessionImpl session) {
    ReadContext mockContext = mock(ReadContext.class);
    final ResultSet mockResult = mock(ResultSet.class);
    when(session.singleUse(any(TimestampBound.class))).thenReturn(mockContext);
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
    SessionPool pool =
        SessionPool.createPool(
            options, new TestExecutorFactory(), client.getSessionClient(db), clock);
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
    clock.currentTimeMillis += TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1;
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
    // Note that session2 was now the first session in the pool as it was the last to receive a
    // ping.
    assertThat(session3.getName()).isEqualTo(session2.getName());
    assertThat(session4.getName()).isEqualTo(session1.getName());
    session5.close();
    session4.close();
    session3.close();
    // Advance the clock to force pings for the sessions in the pool and do three maintenance loops.
    clock.currentTimeMillis += TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1;
    runMaintenanceLoop(clock, pool, 3);
    assertThat(pingedSessions).containsExactly(session1.getName(), 2, session2.getName(), 2);

    // Advance the clock to idle all sessions in the pool again and then check out one session. This
    // should cause only one session to get a ping.
    clock.currentTimeMillis += TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1;
    // We are now checking out session2 because
    Session session6 = pool.getSession();
    // The session that was first in the pool now is equal to the initial first session as each full
    // round of pings will swap the order of the first MinSessions sessions in the pool.
    assertThat(session6.getName()).isEqualTo(session1.getName());
    runMaintenanceLoop(clock, pool, 3);
    assertThat(pool.totalSessions()).isEqualTo(3);
    assertThat(pingedSessions).containsExactly(session1.getName(), 2, session2.getName(), 3);
    // Update the last use date and release the session to the pool and do another maintenance
    // cycle.
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

    clock.currentTimeMillis += TimeUnit.MINUTES.toMillis(options.getKeepAliveIntervalMinutes()) + 1;
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
    // Note that session2 was now the first session in the pool as it was the last to receive a
    // ping.
    assertThat(session3.getName()).isEqualTo(session2.getName());
    assertThat(session4.getName()).isEqualTo(session1.getName());
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
}
