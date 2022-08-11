/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FGACSampleIT extends SampleTestBase {

  private static DatabaseId databaseId;

  private static final List<PgAsyncExamplesIT.Singer> TEST_SINGERS =
      Arrays.asList(
          new PgAsyncExamplesIT.Singer(1, "Marc", "Richards"),
          new PgAsyncExamplesIT.Singer(2, "Catalina", "Smith"),
          new PgAsyncExamplesIT.Singer(3, "Alice", "Trentor"),
          new PgAsyncExamplesIT.Singer(4, "Lea", "Martin"),
          new PgAsyncExamplesIT.Singer(5, "David", "Lomond"));
  private static final List<PgAsyncExamplesIT.Album> ALBUMS =
      Arrays.asList(
          new PgAsyncExamplesIT.Album(1, 1, "Total Junk", 300_000L),
          new PgAsyncExamplesIT.Album(1, 2, "Go, Go, Go", 400_000L),
          new PgAsyncExamplesIT.Album(2, 1, "Green", 150_000L),
          new PgAsyncExamplesIT.Album(2, 2, "Forever Hold Your Peace", 350_000L),
          new PgAsyncExamplesIT.Album(2, 3, "Terrified", null));

  private void assertAlbumsOutput(String out) {
    assertThat(out).contains("1 1 Total Junk");
    assertThat(out).contains("1 2 Go, Go, Go");
    assertThat(out).contains("2 1 Green");
    assertThat(out).contains("2 2 Forever Hold Your Peace");
    assertThat(out).contains("2 3 Terrified");
  }

  @BeforeClass
  public static void createTestDatabase() throws Exception {
    final String database = idGenerator.generateDatabaseId();
    databaseId = DatabaseId.of(projectId, instanceId, database);
    databaseAdminClient
        .createDatabase(
            databaseAdminClient.newDatabaseBuilder(databaseId).build(), Collections.emptyList())
        .get();
    databaseAdminClient
        .updateDatabaseDdl(
            instanceId,
            database,
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
                    + "  AlbumTitle      STRING(MAX)"
                    + ") PRIMARY KEY (SingerId, AlbumId),"
                    + "  INTERLEAVE IN PARENT Singers ON DELETE CASCADE"),
            null)
        .get();
  }

  @Before
  public void insertTestData() {
    DatabaseClient client = spanner.getDatabaseClient(databaseId);
    ImmutableList.Builder<Mutation> mutations =
        ImmutableList.builderWithExpectedSize(TEST_SINGERS.size());
    for (PgAsyncExamplesIT.Singer singer : TEST_SINGERS) {
      mutations.add(
          Mutation.newInsertBuilder("Singers")
              .set("SingerId")
              .to(singer.singerId)
              .set("FirstName")
              .to(singer.firstName)
              .set("LastName")
              .to(singer.lastName)
              .build());
    }
    for (PgAsyncExamplesIT.Album album : ALBUMS) {
      mutations.add(
          Mutation.newInsertBuilder("Albums")
              .set("SingerId")
              .to(album.singerId)
              .set("AlbumId")
              .to(album.albumId)
              .set("AlbumTitle")
              .to(album.albumTitle)
              .build());
    }
    client.write(mutations.build());
  }

  @After
  public void removeTestData() {
    DatabaseClient client = spanner.getDatabaseClient(databaseId);
    client.write(Arrays.asList(Mutation.delete("Singers", KeySet.all())));
  }

  @Test
  public void testFGAC() throws Exception {

    // add new role
    final String out =
        SampleRunner.runSample(
            () -> AddNewRoleSample.addNewRole(projectId, instanceId, databaseId.getDatabase()));
    assertTrue(
        "Expected to create parent and child role." + " Output received was " + out,
        out.contains("Successfully added parent and child roles"));
    assertTrue(
        "Expected to delete child role." + " Output received was " + out,
        out.contains("Successfully deleted chile role"));

    // read data with "parent" role
    final String readOut =
        SampleRunner.runSample(
            () ->
                ReadDataWithRoleSample.readDataWithRole(
                    projectId, instanceId, databaseId.getDatabase(), "parent"));
    assertAlbumsOutput(readOut);

    // list database roles
    final String listRolesOut =
        SampleRunner.runSample(
            () -> ListRolesSample.listRoles(projectId, instanceId, databaseId.getDatabase()));
    assertThat(listRolesOut).contains("parent");
    assertThat(listRolesOut).contains("public");
    assertThat(listRolesOut).contains("spanner_info_reader");
    assertThat(listRolesOut).contains("spanner_sys_reader");
  }
}
