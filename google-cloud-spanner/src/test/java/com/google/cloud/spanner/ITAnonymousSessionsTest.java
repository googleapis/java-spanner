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

import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPoolOptions.ActionForAnonymousSessionsChannelHints;
import com.google.cloud.spanner.SessionPoolOptions.ActionForNumberOfAnonymousSessions;
import com.google.cloud.spanner.SessionPoolOptions.AnonymousSessionOptions;
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
public class ITAnonymousSessionsTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static final String TABLE_NAME = "TestTable";

  private static Database db;
  private SessionPool pool;

  @BeforeClass
  public static void setUpDatabase() {
    db =
        env.getTestHelper()
            .createTestDatabase(
                "CREATE TABLE TestTable ("
                    + "  Key                STRING(MAX) NOT NULL,"
                    + "  StringValue        STRING(MAX),"
                    + ") PRIMARY KEY (Key)",
                "CREATE INDEX TestTableByValue ON TestTable(StringValue)");
  }

  @Before
  public void setUp() {
    AnonymousSessionOptions anonymousSessionOptions =
        AnonymousSessionOptions.newBuilder()
            .setActionForAnonymousSessionsChannelHints(
                ActionForAnonymousSessionsChannelHints.MULTI_CHANNEL)
            .setActionForNumberOfAnonymousSessions(
                ActionForNumberOfAnonymousSessions.SHARED_SESSION).build();
    SessionPoolOptions options =
        SessionPoolOptions.newBuilder().setMinSessions(1)
            .setAnonymousSessionOptions(anonymousSessionOptions).setMaxSessions(2).build();
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
            ((SpannerImpl) env.getTestHelper().getClient()).getSessionClient(db.getId()));
  }
  @Test
  public void testAnonymousSessions() {
    AnonymousSessionOptions anonymousSessionOptions =
        AnonymousSessionOptions.newBuilder()
            .setActionForAnonymousSessionsChannelHints(
                ActionForAnonymousSessionsChannelHints.MULTI_CHANNEL)
            .setActionForNumberOfAnonymousSessions(
                ActionForNumberOfAnonymousSessions.SHARED_SESSION).build();

    Session session1 = pool.getSession().get();
    Session session2 = pool.getSession().get();

    SpannerOptions options = env.getTestHelper().getOptions()
        .toBuilder()
        .setSessionPoolOption(SessionPoolOptions.newBuilder()
            .setAnonymousSessionOptions(anonymousSessionOptions).build()).build();

    try (Spanner spanner = options.getService()) {
      DatabaseClientImpl client = (DatabaseClientImpl) spanner.getDatabaseClient(db.getId());
      try (ResultSet rs =
          client.singleUseWithSharedSession()
              .executeQuery(Statement.of("SELECT 1 AS COL1"))) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(0)).isEqualTo(1L);
        assertThat(rs.next()).isFalse();
      }
    }

    assertThat(pool.getNumberOfSessionsInPool()).isEqualTo(2);
    assertThat(pool.getNumberOfSessionsInUse()).isEqualTo(0);
  }
}
