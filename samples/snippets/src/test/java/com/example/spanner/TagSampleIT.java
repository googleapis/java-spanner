package com.example.spanner;

import static com.example.spanner.SampleRunner.runSample;
import static org.junit.Assert.assertEquals;
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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for {@link TagSample}
 */
@RunWith(JUnit4.class)
public class TagSampleIT extends SampleTestBase {

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
                    + "  SingerId    INT64 NOT NULL,"
                    + "  AlbumId     INT64,"
                    + "  AlbumTitle  STRING(1024)"
                    + ") PRIMARY KEY (SingerId)",
                "CREATE TABLE Venues ("
                    + "  VenueId      INT64 NOT NULL,"
                    + "  VenueName    STRING(MAX),"
                    + "  Capacity     INT64,"
                    + "  OutdoorVenue BOOL"
                    + ") PRIMARY KEY  (VenueId)"))
        .get(10, TimeUnit.MINUTES);
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

    final String out = runSample(() -> TagSample.setRequestTag(client));
    assertEquals("SingerId: 1, AlbumId: 1, AlbumTitle: title 1", out);
  }

  @Test
  public void testSetTransactionTag() throws Exception {
    final DatabaseClient client = spanner.getDatabaseClient(databaseId);

    final String out = runSample(() -> TagSample.setTransactionTag(client));
    assertTrue(out.contains("Venue capacities updated."));
    assertTrue(out.contains("New venue inserted."));
  }
}
