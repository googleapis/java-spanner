/*
 * Copyright 2021 Google LLC
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

package com.example.spanner;

import static com.example.spanner.SampleRunner.runSample;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for {@link TransactionWithTagSample} */
@RunWith(JUnit4.class)
public class TransactionWithTagSampleIT extends SampleTestBase {

  private static DatabaseId databaseId;

  @BeforeClass
  public static void createTestDatabase() throws Exception {
    final String database = idGenerator.generateDatabaseId();
    databaseAdminClient
        .createDatabase(
            instanceId,
            database,
            ImmutableList.of(
                "CREATE TABLE Albums ("
                    + "  SingerId   INT64 NOT NULL,"
                    + "  AlbumId  INT64,"
                    + "  AlbumTitle   STRING(1024)"
                    + ") PRIMARY KEY (SingerId)",
                "CREATE TABLE Venues ("
                    + "  VenueId        INT64 NOT NULL,"
                    + "  VenueName      STRING(MAX),"
                    + "  Capacity       INT64,"
                    + "  OutdoorVenue   BOOL"
                    + ") PRIMARY KEY (VenueId)"))
        .get();
    databaseId = DatabaseId.of(projectId, instanceId, database);
  }

  @Before
  public void insertTestData() {
    final DatabaseClient client = spanner.getDatabaseClient(databaseId);
    client.write(
        Arrays.asList(
            Mutation.newInsertOrUpdateBuilder("Albums")
                .set("SingerId")
                .to(1L)
                .set("AlbumId")
                .to(1L)
                .set("AlbumTitle")
                .to("title 1")
                .build(),
            Mutation.newInsertOrUpdateBuilder("Venues")
                .set("VenueId")
                .to(4L)
                .set("VenueName")
                .to("name")
                .set("Capacity")
                .to(4000000)
                .set("OutdoorVenue")
                .to(false)
                .build()));
  }

  @After
  public void removeTestData() {
    final DatabaseClient client = spanner.getDatabaseClient(databaseId);
    client.write(Collections.singletonList(Mutation.delete("Albums", KeySet.all())));
    client.write(Collections.singleton(Mutation.delete("Venues", KeySet.all())));
  }

  @Test
  public void testSetRequestTag() throws Exception {
    final DatabaseClient client = spanner.getDatabaseClient(databaseId);

    // Execute query multiple times to give a better chance of being part of the TopN tables.
    for (int i = 0; i < 50; i++) {
      final String out = runSample(() -> TransactionWithTagSample.setRequestTag(client));
      assertThat(out).contains("1 1 title 1");
    }

    boolean queryStatsFound = false;
    // Query statistics will be collected within 1 minute intervals.
    for (int i = 0; i < 50; i++) {
      try (ResultSet resultSet =
          client
              .singleUse()
              .executeQuery(
                  Statement.of("SELECT text FROM SPANNER_SYS.QUERY_STATS_TOP_MINUTE "
                      + "Where request_tag=\"app=concert,env=dev,action=select\" limit 1"))) {
        while (resultSet.next()) {
          assertThat(resultSet.getString(0))
              .isEqualTo("SELECT SingerId, AlbumId, AlbumTitle FROM Albums");
          queryStatsFound = true;
        }
        if (queryStatsFound) {
          break;
        }
        Thread.sleep(5000);
      }
    }
    assertTrue(queryStatsFound);
  }

  @Test
  public void testSetTransactionTag() throws Exception {
    final DatabaseClient client = spanner.getDatabaseClient(databaseId);
    // Execute transaction multiple times to give a better chance of being part of the TopN tables.
    for (int i = 0; i < 50; i++) {
      final String out = runSample(() -> TransactionWithTagSample.setTransactionTag(client));
      assertThat(out).contains("Capacity of name updated to");
    }

    boolean txnStatsFound = false;
    // Transaction statistics will be collected within 1 minute intervals.
    for (int i = 0; i < 50; i++) {
      try (ResultSet resultSet =
          client
              .singleUse()
              .executeQuery(
                  Statement.of(
                      "SELECT READ_COLUMNS FROM SPANNER_SYS.TXN_STATS_TOP_MINUTE where "
                          + "transaction_tag=\"app=concert,env=dev\"limit 1"))) {
        while (resultSet.next()) {
          assertThat(resultSet.getStringList(0)).contains("Venues.Capacity");
          assertThat(resultSet.getStringList(0)).contains("Venues.VenueName");
          txnStatsFound = true;
        }
        if (txnStatsFound) {
          break;
        }
        Thread.sleep(5000);
      }
    }
    assertTrue(txnStatsFound);
  }
}
