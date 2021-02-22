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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for {@link GetCommitStatsSample}
 */
@RunWith(JUnit4.class)
public class GetCommitStatsSampleIT {

  private static String instanceId = System.getProperty("spanner.test.instance");
  private static String databaseId = formatForTest(
      System.getProperty("spanner.sample.database", "commitstatssample"));
  private static DatabaseId dbId;
  private static DatabaseAdminClient dbClient;
  private static Spanner spanner;

  @BeforeClass
  public static void createTestDatabase() throws Exception {
    final SpannerOptions options =
        SpannerOptions.newBuilder().setAutoThrottleAdministrativeRequests().build();
    spanner = options.getService();
    dbClient = spanner.getDatabaseAdminClient();
    if (instanceId == null) {
      Iterator<Instance> iterator =
          spanner.getInstanceAdminClient().listInstances().iterateAll().iterator();
      if (iterator.hasNext()) {
        instanceId = iterator.next().getId().getInstance();
      }
    }
    dbId = DatabaseId.of(options.getProjectId(), instanceId, databaseId);
    dbClient.dropDatabase(dbId.getInstanceId().getInstance(), dbId.getDatabase());
    dbClient
        .createDatabase(
            instanceId,
            databaseId,
            ImmutableList.of(
                "CREATE TABLE Singers ("
                    + "  SingerId   INT64 NOT NULL,"
                    + "  FirstName  STRING(1024),"
                    + "  LastName   STRING(1024),"
                    + "  SingerInfo BYTES(MAX)"
                    + ") PRIMARY KEY (SingerId)",
                "CREATE TABLE Albums ("
                    + "  SingerId        INT64 NOT NULL,"
                    + "  AlbumId         INT64 NOT NULL,"
                    + "  AlbumTitle      STRING(MAX),"
                    + "  MarketingBudget INT64"
                    + ") PRIMARY KEY (SingerId, AlbumId),"
                    + "  INTERLEAVE IN PARENT Singers ON DELETE CASCADE",
                "CREATE INDEX AlbumsByAlbumTitle ON Albums(AlbumTitle)"))
        .get();
  }

  @AfterClass
  public static void dropTestDatabase() {
    dbClient.dropDatabase(dbId.getInstanceId().getInstance(), dbId.getDatabase());
    spanner.close();
  }

  @Before
  public void insertTestData() {
    final DatabaseClient client = spanner.getDatabaseClient(dbId);
    client.write(Arrays.asList(
        Mutation.newInsertBuilder("Singers")
            .set("SingerId")
            .to(1L)
            .set("FirstName")
            .to("first name 1")
            .set("LastName")
            .to("last name 1")
            .build(),
        Mutation.newInsertBuilder("Singers")
            .set("SingerId")
            .to(2L)
            .set("FirstName")
            .to("first name 2")
            .set("LastName")
            .to("last name 2")
            .build()
    ));
  }

  @After
  public void removeTestData() {
    final DatabaseClient client = spanner.getDatabaseClient(dbId);
    client.write(Collections.singletonList(Mutation.delete("Singers", KeySet.all())));
  }

  @Test
  public void testGetCommitStatsSample() {
    final DatabaseClient client = spanner.getDatabaseClient(dbId);
    final String out = runExample(client);

    assertThat(out).contains("Updated data with 8 mutations.");
  }

  private String runExample(DatabaseClient client) {
    PrintStream stdOut = System.out;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    System.setOut(out);
    GetCommitStatsSample.getCommitStats(client);
    System.setOut(stdOut);
    return bout.toString();
  }

  private static String formatForTest(String name) {
    return name + "-" + UUID.randomUUID().toString().substring(0, 20);
  }
}
