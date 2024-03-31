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
package com.google.cloud.spanner.it;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TimestampBound;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for multiplexed sessions.
 *
 * <p>See also {@link ITWriteTest}, which provides coverage of writing and reading back all Cloud
 * Spanner types.
 */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITMultiplexedSessionsTest {
  private static final String INSTANCE_ID = "arpanmishra-dev-span";
  private static final String SERVER_URL = "https://staging-wrenchworks.sandbox.googleapis.com";
  private static DatabaseAdminClient databaseAdminClient;

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static final AtomicInteger dbSeq = new AtomicInteger();
  private static final int dbPrefix = new Random().nextInt(Integer.MAX_VALUE);
  private static final String TABLE_NAME = "TestTable";
  private static final List<String> ALL_COLUMNS = Arrays.asList("Key", "StringValue");
  private static DatabaseClient client;
  private static Spanner spanner;
  private static List<String> databaseIds = new ArrayList<>();

  @BeforeClass
  public static void setUpDatabase() throws Exception {
    final String databaseId = getUniqueDatabaseId();
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setUseMultiplexedSession(true).build())
            .setHost(SERVER_URL)
            .build();
    spanner = options.getService();
    databaseAdminClient = spanner.getDatabaseAdminClient();
    final Database database =
        databaseAdminClient
            .createDatabase(
                INSTANCE_ID,
                databaseId,
                ImmutableList.of(
                    "CREATE TABLE TestTable ("
                        + "  key                STRING(MAX) NOT NULL,"
                        + "  stringvalue        STRING(MAX),"
                        + ") PRIMARY KEY (key)",
                    "CREATE INDEX TestTableByValue ON TestTable(stringvalue)",
                    "CREATE INDEX TestTableByValueDesc ON TestTable(stringvalue DESC)"))
            .get(Duration.ofMinutes(2).toNanos(), TimeUnit.NANOSECONDS);

    databaseIds.add(databaseId);

    client = spanner.getDatabaseClient(database.getId());

    // Includes k0..k14.  Note that strings k{10,14} sort between k1 and k2.
    List<Mutation> mutations = new ArrayList<>();
    for (int i = 0; i < 15; ++i) {
      mutations.add(
          Mutation.newInsertOrUpdateBuilder(TABLE_NAME)
              .set("key")
              .to("k" + i)
              .set("stringvalue")
              .to("v" + i)
              .build());
    }
    client.write(mutations);
  }

  @AfterClass
  public static void teardown() {
    cleanUp();
    spanner.close();
  }

  @Test
  public void pointReadRow() {
    Struct row = executeReadRow();
    assertThat(row).isNotNull();
    assertThat(row.getString(0)).isEqualTo("k1");
    assertThat(row.getString(1)).isEqualTo("v1");
    // Ensure that the Struct implementation supports equality properly.
    assertThat(row)
        .isEqualTo(Struct.newBuilder().set("key").to("k1").set("stringvalue").to("v1").build());
  }

  @Test
  public void pointRead() {
    executeRead();
  }

  @Test
  public void pointReadAsync() throws Exception {
    ListeningScheduledExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(10));
    List<ListenableFuture<Struct>> listenableFutures = new ArrayList();
    for (int i = 0; i < 10; i++) {
      listenableFutures.add(service.submit(() -> executeReadRow()));
    }
    for (ListenableFuture<Struct> listenableFuture : listenableFutures) {
      Struct row = listenableFuture.get();
      assertThat(row).isNotNull();
      assertThat(row.getString(0)).isEqualTo("k1");
      assertThat(row.getString(1)).isEqualTo("v1");
      // Ensure that the Struct implementation supports equality properly.
      assertThat(row)
          .isEqualTo(Struct.newBuilder().set("key").to("k1").set("stringvalue").to("v1").build());
    }
  }

  private void executeRead() {
    ResultSet resultSet =
        client.singleUse(TimestampBound.strong()).read(TABLE_NAME, KeySet.singleKey(Key.of("k1")), ALL_COLUMNS);
    while (resultSet.next()) {
      assertThat(resultSet).isNotNull();
      assertThat(resultSet.getString(0)).isEqualTo("k1");
      assertThat(resultSet.getString(1)).isEqualTo("v1");
    }
  }

  private Struct executeReadRow() {
    Struct row =
        client.singleUse(TimestampBound.strong()).readRow(TABLE_NAME, Key.of("k1"), ALL_COLUMNS);
    return row;
  }

  public static String getUniqueDatabaseId() {
    return String.format("testdb_%d_%04d", dbPrefix, dbSeq.incrementAndGet());
  }

  public static void cleanUp() {
    // Drop all the databases we created explicitly.
    int numDropped = 0;
    for (String databaseId : databaseIds) {
      try {
        spanner.getDatabaseAdminClient().dropDatabase(INSTANCE_ID, databaseId);
        ++numDropped;
      } catch (Throwable e) {
      }
    }
  }
}
