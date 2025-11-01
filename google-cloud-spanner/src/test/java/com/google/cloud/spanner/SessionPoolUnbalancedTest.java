/*
 * Copyright 2023 Google LLC
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

import static com.google.cloud.spanner.SessionPool.isUnbalanced;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spanner.SessionPool.PooledSession;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SessionPoolUnbalancedTest {

  static PooledSession mockedSession(int channel) {
    PooledSession session = mock(PooledSession.class);
    when(session.getChannel()).thenReturn(channel);
    return session;
  }

  static List<PooledSession> mockedSessions(int... channels) {
    return Arrays.stream(channels)
        .mapToObj(SessionPoolUnbalancedTest::mockedSession)
        .collect(Collectors.toList());
  }

  static PooledSessionFuture mockedCheckedOutSession(int channel) {
    PooledSession session = mockedSession(channel);
    // Suppressed for initial Error Prone rollout.
    @SuppressWarnings("DoNotMock")
    PooledSessionFuture future = mock(PooledSessionFuture.class);
    when(future.get()).thenReturn(session);
    when(future.isDone()).thenReturn(true);
    return future;
  }

  static Set<PooledSessionFuture> mockedCheckedOutSessions(int... channels) {
    return Arrays.stream(channels)
        .mapToObj(SessionPoolUnbalancedTest::mockedCheckedOutSession)
        .collect(Collectors.toSet());
  }

  @Test
  public void testIsUnbalancedBasics() {
    // An empty session pool is never unbalanced.
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1), 1));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1), 2));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1), 4));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1, 1), 1));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1, 1), 2));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1, 1), 4));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1, 1, 1), 1));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1, 1, 1), 2));
    assertFalse(isUnbalanced(1, mockedSessions(), mockedCheckedOutSessions(1, 1, 1, 1, 1), 4));

    // A session pool that has 2 or fewer sessions checked out is never unbalanced.
    // This prevents low-QPS scenarios from re-balancing the pool.
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1), mockedCheckedOutSessions(), 1));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1), mockedCheckedOutSessions(), 2));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1), mockedCheckedOutSessions(), 4));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1), mockedCheckedOutSessions(1), 1));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1), mockedCheckedOutSessions(1), 2));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1), mockedCheckedOutSessions(1), 4));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1, 1), mockedCheckedOutSessions(1, 1), 1));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1, 1), mockedCheckedOutSessions(1, 1), 2));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1, 1), mockedCheckedOutSessions(1, 1), 4));

    // A session pool that uses only 1 channel is never unbalanced.
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1), mockedCheckedOutSessions(), 1));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1), mockedCheckedOutSessions(), 1));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1, 1), mockedCheckedOutSessions(), 1));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1, 1, 1, 1), mockedCheckedOutSessions(), 1));
    assertFalse(isUnbalanced(1, mockedSessions(1, 1, 1), mockedCheckedOutSessions(1, 1, 1), 1));
    assertFalse(
        isUnbalanced(1, mockedSessions(1, 1, 1, 1), mockedCheckedOutSessions(1, 1, 1, 1), 1));
    assertFalse(
        isUnbalanced(1, mockedSessions(1, 1, 1, 1, 1), mockedCheckedOutSessions(1, 1, 1, 1, 1), 1));
    assertFalse(
        isUnbalanced(
            1, mockedSessions(1, 1, 1, 1, 1, 1), mockedCheckedOutSessions(1, 1, 1, 1, 1, 1), 1));
  }

  @Test
  public void testIsUnbalanced_returnsFalseForBalancedPool() {
    assertFalse(
        isUnbalanced(1, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 2, 3, 4), 4));
    assertFalse(
        isUnbalanced(2, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 2, 3, 4), 4));
    assertFalse(
        isUnbalanced(3, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 2, 3, 4), 4));
    assertFalse(
        isUnbalanced(4, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 2, 3, 4), 4));

    assertFalse(
        isUnbalanced(1, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(4, 3, 2, 1), 4));
    assertFalse(
        isUnbalanced(2, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(4, 3, 2, 1), 4));
    assertFalse(
        isUnbalanced(3, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(4, 3, 2, 1), 4));
    assertFalse(
        isUnbalanced(4, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(4, 3, 2, 1), 4));

    assertFalse(
        isUnbalanced(
            1,
            mockedSessions(1, 2, 3, 4, 1, 2, 3, 4),
            mockedCheckedOutSessions(1, 2, 3, 4, 1, 2, 3, 4),
            4));

    // We only check the first numChannels sessions that are in the pool, so the fact that the end
    // of the pool is unbalanced is not a reason to re-balance.
    assertFalse(
        isUnbalanced(
            1, mockedSessions(1, 2, 3, 4, 1, 1, 1, 1), mockedCheckedOutSessions(1, 2, 3, 4), 4));
    assertFalse(
        isUnbalanced(1, mockedSessions(1, 2, 1, 1, 1, 1), mockedCheckedOutSessions(1, 2), 2));
    assertFalse(
        isUnbalanced(
            1,
            mockedSessions(1, 2, 3, 4, 1, 2, 3, 4, 1, 1, 1, 1),
            mockedCheckedOutSessions(1, 2, 3, 4),
            8));
    assertFalse(
        isUnbalanced(
            1,
            mockedSessions(1, 1, 2, 2, 3, 3, 4, 4, 1, 1, 1, 1),
            mockedCheckedOutSessions(1, 2, 3, 4),
            8));

    // The list of checked out sessions is allowed to contain up to twice the number of sessions
    // with a given channel than it should for a perfect distribution (perfect means
    // num_sessions_with_a_channel == num_channels).
    assertFalse(
        isUnbalanced(1, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 1, 2, 3), 4));
    assertFalse(
        isUnbalanced(
            1,
            mockedSessions(1, 2, 3, 4),
            mockedCheckedOutSessions(1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 2, 3, 4, 5, 6),
            8));
    // We're only checking the list of checked out sessions against the channel that is being added
    // to the pool.
    assertFalse(
        isUnbalanced(1, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(2, 2, 2, 2), 4));

    // We do not consider a pool unbalanced if the list of checked out sessions only contains 2 of
    // the same channel, even if that would still be 'more than twice the ideal number'. This
    // prevents that a small number of checked out sessions that happen to use the same channel
    // causes the pool to be considered unbalanced.
    assertFalse(
        isUnbalanced(
            1, mockedSessions(1, 2, 3, 4, 5, 6, 7, 8), mockedCheckedOutSessions(1, 1, 2), 8));

    // A larger number of checked out sessions means that we can also have a 'large' number of the
    // same channels in that list, as long as it does not exceed twice the number that it should be
    // for an ideal distribution.
    assertFalse(
        isUnbalanced(
            1,
            mockedSessions(1, 2, 3, 4, 5, 6, 7, 8),
            mockedCheckedOutSessions(1, 1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 2, 4, 5, 5, 3, 4, 8, 8),
            8));
  }

  @Test
  public void testIsUnbalanced_returnsTrueForUnbalancedPool() {
    // The pool is considered unbalanced if the first numChannel sessions contain 3 or more of the
    // same sessions as the one that is being added. Also; if the pool uses only 2 channels, then it
    // is also considered unbalanced if the two first sessions in the pool already use the same
    // channel as the one being added.
    assertTrue(isUnbalanced(1, mockedSessions(1, 1), mockedCheckedOutSessions(1, 2, 1, 2), 2));
    assertTrue(isUnbalanced(2, mockedSessions(2, 2), mockedCheckedOutSessions(1, 2, 1, 2), 2));

    assertTrue(
        isUnbalanced(1, mockedSessions(1, 1, 1, 4), mockedCheckedOutSessions(1, 2, 3, 4), 4));
    assertTrue(
        isUnbalanced(2, mockedSessions(2, 2, 2, 4), mockedCheckedOutSessions(1, 2, 3, 4), 4));
    assertTrue(
        isUnbalanced(3, mockedSessions(1, 3, 3, 3), mockedCheckedOutSessions(1, 2, 3, 4), 4));
    assertTrue(
        isUnbalanced(4, mockedSessions(1, 4, 4, 4), mockedCheckedOutSessions(1, 2, 3, 4), 4));

    assertTrue(
        isUnbalanced(
            1, mockedSessions(1, 2, 3, 4, 5, 6, 1, 1), mockedCheckedOutSessions(1, 2, 3, 4), 8));
    assertTrue(
        isUnbalanced(
            2, mockedSessions(1, 3, 4, 5, 6, 2, 2, 2), mockedCheckedOutSessions(1, 2, 3, 4), 8));
    assertTrue(
        isUnbalanced(
            3, mockedSessions(1, 2, 3, 3, 4, 5, 3, 6), mockedCheckedOutSessions(1, 2, 3, 4), 8));
    assertTrue(
        isUnbalanced(
            4, mockedSessions(1, 2, 3, 4, 5, 4, 5, 4), mockedCheckedOutSessions(1, 2, 3, 4), 8));

    // The pool is also considered unbalanced if the list of checked out sessions contain more than
    // 2 times as many sessions of the one being returned as it should.
    assertTrue(
        isUnbalanced(1, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 1, 2, 1), 4));
    assertTrue(
        isUnbalanced(2, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(2, 2, 2, 4), 4));
    assertTrue(
        isUnbalanced(3, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 3, 3, 3), 4));
    assertTrue(
        isUnbalanced(4, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(4, 2, 4, 4), 4));
    assertTrue(
        isUnbalanced(
            1, mockedSessions(1, 2, 3, 4), mockedCheckedOutSessions(1, 1, 2, 1, 1, 2, 3, 1), 4));

    assertTrue(
        isUnbalanced(
            1, mockedSessions(1, 2, 3, 4, 5, 6, 7, 8), mockedCheckedOutSessions(1, 1, 1, 3), 8));
    assertTrue(
        isUnbalanced(
            1,
            mockedSessions(1, 2, 3, 4, 5, 6, 7, 8),
            mockedCheckedOutSessions(1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 1, 1),
            8));
  }
}
