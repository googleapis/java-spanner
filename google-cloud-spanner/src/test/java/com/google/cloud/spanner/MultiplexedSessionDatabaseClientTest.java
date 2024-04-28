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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.SessionClient.SessionConsumer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;

@RunWith(JUnit4.class)
public class MultiplexedSessionDatabaseClientTest {

  @Ignore("Fails on native builds due to ues of reflection")
  @Test
  public void testMaintainer() {
    Instant now = Instant.now();
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(now);
    SessionClient sessionClient = mock(SessionClient.class);
    SpannerImpl spanner = mock(SpannerImpl.class);
    SpannerOptions spannerOptions = mock(SpannerOptions.class);
    SessionPoolOptions sessionPoolOptions = mock(SessionPoolOptions.class);
    when(sessionClient.getSpanner()).thenReturn(spanner);
    when(spanner.getOptions()).thenReturn(spannerOptions);
    when(spannerOptions.getSessionPoolOptions()).thenReturn(sessionPoolOptions);
    when(sessionPoolOptions.getMultiplexedSessionMaintenanceDuration())
        .thenReturn(org.threeten.bp.Duration.ofDays(7));
    when(sessionPoolOptions.getMultiplexedSessionMaintenanceLoopFrequency())
        .thenReturn(Duration.ofMinutes(10));

    SessionImpl session1 = mock(SessionImpl.class);
    SessionReference sessionReference1 = mock(SessionReference.class);
    when(session1.getSessionReference()).thenReturn(sessionReference1);

    SessionImpl session2 = mock(SessionImpl.class);
    SessionReference sessionReference2 = mock(SessionReference.class);
    when(session2.getSessionReference()).thenReturn(sessionReference2);

    doAnswer(
            (Answer<?>)
                invocationOnMock -> {
                  SessionConsumer consumer = invocationOnMock.getArgument(0);
                  // Return session1 the first time it is called.
                  consumer.onSessionReady(session1);
                  return null;
                })
        .doAnswer(
            (Answer<?>)
                invocationOnMock -> {
                  SessionConsumer consumer = invocationOnMock.getArgument(0);
                  // Return session2 the second time that it is called.
                  consumer.onSessionReady(session2);
                  return null;
                })
        .when(sessionClient)
        .asyncCreateMultiplexedSession(any(SessionConsumer.class));

    // Create a client. This should get session1.
    MultiplexedSessionDatabaseClient client =
        new MultiplexedSessionDatabaseClient(sessionClient, clock);

    // Make sure that the client uses the initial session that is created.
    assertEquals(client.getCurrentSessionReference(), session1.getSessionReference());

    // Run the maintainer without advancing the clock. We should still get the same session.
    client.getMaintainer().maintain();
    assertEquals(client.getCurrentSessionReference(), session1.getSessionReference());

    // Advance the clock by 1 day. We should still get the same session.
    when(clock.instant()).thenReturn(now.plus(Duration.ofDays(1)));
    client.getMaintainer().maintain();
    assertEquals(client.getCurrentSessionReference(), session1.getSessionReference());

    // Advance the clock by 8 days. We should now get a new session.
    when(clock.instant()).thenReturn(now.plus(Duration.ofDays(8)));
    client.getMaintainer().maintain();
    assertEquals(client.getCurrentSessionReference(), session2.getSessionReference());
  }
}
