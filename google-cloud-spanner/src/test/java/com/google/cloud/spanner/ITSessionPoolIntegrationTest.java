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

import static com.google.cloud.spanner.testing.ExperimentalHostHelper.isExperimentalHost;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.OpenTelemetry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for read and query.
 *
 * <p>See also {@code it/WriteIntegrationTest}, which provides coverage of writing and reading back
 * all Cloud Spanner types.
 */
@Category(SerialIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITSessionPoolIntegrationTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static final String TABLE_NAME = "TestTable";

  private static Database db;
  private SessionPool pool;

  @BeforeClass
  public static void setUpDatabase() {
    assumeFalse("Only Multiplexed Sessions are supported on this host", isExperimentalHost());
    db =
        env.getTestHelper()
            .createTestDatabase(
                "CREATE TABLE TestTable ("
                    + "  Key                STRING(MAX) NOT NULL,"
                    + "  StringValue        STRING(MAX),"
                    + ") PRIMARY KEY (Key)",
                "CREATE INDEX TestTableByValue ON TestTable(StringValue)");

    // Includes k0..k14.  Note that strings k{10,14} sort between k1 and k2.
    List<Mutation> mutations = new ArrayList<>();
    for (int i = 0; i < 15; ++i) {
      mutations.add(
          Mutation.newInsertOrUpdateBuilder(TABLE_NAME)
              .set("Key")
              .to("k" + i)
              .set("StringValue")
              .to("v" + i)
              .build());
    }
    env.getTestHelper().getDatabaseClient(db).write(mutations);
  }

  @Before
  public void setUp() {
    SessionPoolOptions options =
        SessionPoolOptions.newBuilder().setMinSessions(1).setMaxSessions(2).build();
    pool =
        SessionPool.createPool(
            options,
            new ExecutorFactory<ScheduledExecutorService>() {

              @Override
              public void release(ScheduledExecutorService executor) {
                executor.shutdown();
              }

              @Override
              public ScheduledExecutorService get() {
                return new ScheduledThreadPoolExecutor(2);
              }
            },
            ((SpannerImpl) env.getTestHelper().getClient()).getSessionClient(db.getId()),
            new TraceWrapper(Tracing.getTracer(), OpenTelemetry.noop().getTracer(""), false),
            OpenTelemetry.noop());
  }

  @Test
  public void sessionCreation() {
    try (PooledSessionFuture session = pool.getSession()) {
      assertThat(session.get()).isNotNull();
    }

    try (PooledSessionFuture session = pool.getSession();
        PooledSessionFuture session2 = pool.getSession()) {
      assertThat(session.get()).isNotNull();
      assertThat(session2.get()).isNotNull();
    }
  }

  @Test
  public void poolExhaustion() throws Exception {
    Session session1 = pool.getSession().get();
    Session session2 = pool.getSession().get();
    final CountDownLatch latch = new CountDownLatch(1);
    new Thread(
            () -> {
              try (Session session3 = pool.getSession().get()) {
                latch.countDown();
              }
            })
        .start();
    assertThat(latch.await(5, TimeUnit.SECONDS)).isFalse();
    session1.close();
    session2.close();
    latch.await();
  }

  @Test
  public void multipleWaiters() throws Exception {
    Session session1 = pool.getSession().get();
    Session session2 = pool.getSession().get();
    int numSessions = 5;
    final CountDownLatch latch = new CountDownLatch(numSessions);
    for (int i = 0; i < numSessions; i++) {
      new Thread(
              () -> {
                try (Session session = pool.getSession().get()) {
                  latch.countDown();
                }
              })
          .start();
    }
    session1.close();
    session2.close();
    // Everyone should get session pretty quickly.
    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void closeQuicklyDoesNotBlockIndefinitely() throws Exception {
    pool.closeAsync(new SpannerImpl.ClosedException()).get();
  }

  @Test
  public void closeAfterInitialCreateDoesNotBlockIndefinitely() throws Exception {
    pool.getSession().close();
    pool.closeAsync(new SpannerImpl.ClosedException()).get();
  }

  @Test
  public void closeWhenSessionsActiveFinishes() throws Exception {
    pool.getSession().get();
    // This will log a warning that a session has been leaked, as the session that we retrieved in
    // the previous statement was never returned to the pool.
    pool.closeAsync(new SpannerImpl.ClosedException()).get();
  }
}
