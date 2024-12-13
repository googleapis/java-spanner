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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.common.base.Stopwatch;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SessionPoolMaintainerMockServerTest extends AbstractMockServerTest {
  private final FakeClock clock = new FakeClock();

  @BeforeClass
  public static void setupResults() {
    mockSpanner.putStatementResult(
        StatementResult.query(
            Statement.of("SELECT 1"),
            com.google.spanner.v1.ResultSet.newBuilder()
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setRowType(
                            StructType.newBuilder()
                                .addFields(
                                    Field.newBuilder()
                                        .setName("C")
                                        .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                                        .build())
                                .build())
                        .build())
                .addRows(
                    ListValue.newBuilder()
                        .addValues(Value.newBuilder().setStringValue("1").build())
                        .build())
                .build()));
  }

  @Before
  public void createSpannerInstance() {
    clock.currentTimeMillis.set(System.currentTimeMillis());
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("p")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setPoolMaintainerClock(clock)
                    .setWaitForMinSessionsDuration(Duration.ofSeconds(10L))
                    .setFailOnSessionLeak()
                    .build())
            .build()
            .getService();
  }

  @Test
  public void testMaintain() {
    int minSessions = spanner.getOptions().getSessionPoolOptions().getMinSessions();
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    assertEquals(minSessions, mockSpanner.getSessions().size());
    assertEquals(0, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    clock.currentTimeMillis.addAndGet(Duration.ofMinutes(35).toMillis());
    client.pool.poolMaintainer.maintainPool();
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    client.pool.poolMaintainer.maintainPool();
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    clock.currentTimeMillis.addAndGet(Duration.ofMinutes(21).toMillis());

    // Most sessions are considered idle and are removed. Freeze the mock Spanner server to prevent
    // the replenish action to fill the pool again before we check the number of sessions in the
    // pool.
    mockSpanner.freeze();
    client.pool.poolMaintainer.maintainPool();
    assertEquals(2, client.pool.totalSessions());
    mockSpanner.unfreeze();

    // The pool should be replenished.
    client.pool.poolMaintainer.maintainPool();
    assertEquals(minSessions, client.pool.getTotalSessionsPlusNumSessionsBeingCreated());
    Stopwatch watch = Stopwatch.createStarted();
    //noinspection StatementWithEmptyBody
    while (client.pool.totalSessions() < minSessions
        && watch.elapsed(TimeUnit.MILLISECONDS)
            < spanner.getOptions().getSessionPoolOptions().getWaitForMinSessions().toMillis()) {
      // wait for the pool to be replenished.
    }
    assertEquals(minSessions, client.pool.totalSessions());
  }

  @Test
  public void testSessionNotFoundIsRetried() {
    assumeFalse(
        "Session not found errors are not relevant for multiplexed sessions",
        spanner.getOptions().getSessionPoolOptions().getUseMultiplexedSession());

    int minSessions = spanner.getOptions().getSessionPoolOptions().getMinSessions();
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    assertEquals(minSessions, mockSpanner.getSessions().size());

    // Remove all sessions from the backend.
    mockSpanner.getSessions().clear();

    // Sessions have been removed from the backend, but this will still succeed, as Session not
    // found errors are retried by the client.
    try (ResultSet resultSet = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
      assertTrue(resultSet.next());
      assertEquals(1L, resultSet.getLong(0));
      assertFalse(resultSet.next());
    }

    int numRequests = mockSpanner.countRequestsOfType(ExecuteSqlRequest.class);
    assertTrue(
        String.format("Number of requests should be larger than 1, but was %d", numRequests),
        numRequests > 1);
  }

  @Test
  public void testMaintainerReplenishesPoolIfAllAreInvalid() {
    int minSessions = spanner.getOptions().getSessionPoolOptions().getMinSessions();
    DatabaseClientImpl client =
        (DatabaseClientImpl) spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    assertEquals(minSessions, mockSpanner.getSessions().size());

    // Remove all sessions from the backend.
    mockSpanner.getSessions().clear();
    // Advance the clock of the maintainer to mark all sessions are eligible for maintenance.
    clock.currentTimeMillis.addAndGet(Duration.ofMinutes(35).toMillis());
    // Run the maintainer. This will ping one session, which again will cause it to be replaced.
    client.pool.poolMaintainer.maintainPool();
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));

    // The session will be replaced using a single BatchCreateSessions call.
    Stopwatch watch = Stopwatch.createStarted();
    //noinspection StatementWithEmptyBody
    while (client.pool.totalSessions() < minSessions
        && watch.elapsed(TimeUnit.MILLISECONDS)
            < spanner.getOptions().getSessionPoolOptions().getWaitForMinSessions().toMillis()) {
      // wait for the pool to be replenished.
    }
    assertEquals(minSessions, client.pool.totalSessions());
    assertEquals(
        spanner.getOptions().getNumChannels() + 1,
        mockSpanner.countRequestsOfType(BatchCreateSessionsRequest.class));
  }
}
