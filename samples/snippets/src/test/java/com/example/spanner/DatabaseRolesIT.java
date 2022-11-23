/*
 * Copyright 2022 Google LLC
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

import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DatabaseRolesIT extends SampleTestBase {
  private static DatabaseId databaseId;

  @BeforeClass
  public static void createTestDatabase() throws Exception {
    final String database = idGenerator.generateDatabaseId();
    databaseAdminClient
        .createDatabase(
            instanceId,
            database,
            ImmutableList.of(
                "CREATE TABLE Singers ("
                    + "  SingerId   INT64 NOT NULL,"
                    + "  FirstName  STRING(1024),"
                    + "  LastName   STRING(1024),"
                    + "  SingerInfo BYTES(MAX),"
                    + "  FullName STRING(2048) AS "
                    + "  (ARRAY_TO_STRING([FirstName, LastName], \" \")) STORED"
                    + ") PRIMARY KEY (SingerId)",
                "CREATE TABLE Albums ("
                    + "  SingerId     INT64 NOT NULL,"
                    + "  AlbumId      INT64 NOT NULL,"
                    + "  AlbumTitle   STRING(MAX),"
                    + "  MarketingBudget INT64"
                    + ") PRIMARY KEY (SingerId, AlbumId),"
                    + "  INTERLEAVE IN PARENT Singers ON DELETE CASCADE"))
        .get(10, TimeUnit.MINUTES);
    databaseId = DatabaseId.of(projectId, instanceId, database);
  }

  @Before
  public void insertTestData() {
    final DatabaseClient client = spanner.getDatabaseClient(databaseId);
    client.write(
        Arrays.asList(
            Mutation.newInsertOrUpdateBuilder("Singers")
                .set("SingerId")
                .to(1L)
                .set("FirstName")
                .to("Melissa")
                .set("LastName")
                .to("Garcia")
                .build(),
            Mutation.newInsertOrUpdateBuilder("Albums")
                .set("SingerId")
                .to(1L)
                .set("AlbumId")
                .to(1L)
                .set("AlbumTitle")
                .to("title 1")
                .set("MarketingBudget")
                .to(20000L)
                .build()));
  }

  @After
  public void removeTestData() {
    final DatabaseClient client = spanner.getDatabaseClient(databaseId);
    client.write(Collections.singletonList(Mutation.delete("Singers", KeySet.all())));
  }

  @Test
  public void testAddAndDropDatabaseRole() throws Exception {
    final String out =
        SampleRunner.runSample(
            () ->
                AddAndDropDatabaseRole.addAndDropDatabaseRole(
                    databaseAdminClient,
                    instanceId,
                    databaseId.getDatabase(),
                    "new-parent",
                    "new-child"));
    assertTrue(out.contains("Created roles new_parent and new_child and granted privileges"));
    assertTrue(out.contains("Revoked privileges and dropped role new_child"));
  }

  @Test
  public void testListDatabaseRoles() throws Exception {
    final String out =
        SampleRunner.runSample(
            () ->
                ListDatabaseRoles.listDatabaseRoles(
                    databaseAdminClient, projectId, instanceId, databaseId.getDatabase()));
    assertTrue(out.contains("new_parent"));
  }

  @Test
  public void testReadDataWithDatabaseRole() throws Exception {
    final String out =
        SampleRunner.runSample(
            () ->
                ReadDataWithDatabaseRole.readDataWithDatabaseRole(
                    projectId, instanceId, databaseId.getDatabase(), "new_parent"));
    assertTrue(out.contains("SingerId: 1"));
    assertTrue(out.contains("FirstName: Melissa"));
    assertTrue(out.contains("LastName: Garcia"));
  }
}
