/*
 * Copyright 2020 Google Inc.
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

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for Cloud Spanner cloud client examples. */
@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class SpannerStandaloneExamplesIT {
  // The instance needs to exist for tests to pass.
  private static String instanceId = System.getProperty("spanner.test.instance");
  private static String baseDatabaseId = System.getProperty("spanner.sample.database", "mysample");
  private static String databaseId = SpannerSampleIT.formatForTest(baseDatabaseId);
  private static DatabaseId dbId;
  private static DatabaseAdminClient dbClient;
  private static Spanner spanner;

  private String runExample(Runnable example) {
    PrintStream stdOut = System.out;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    System.setOut(out);
    example.run();
    System.setOut(stdOut);
    return bout.toString();
  }

  @BeforeClass
  public static void createTestDatabase() throws Exception {
    SpannerOptions options =
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
                "CREATE TABLE Venues ("
                    + "VenueId INT64 NOT NULL,"
                    + "Revenue NUMERIC"
                    + ") PRIMARY KEY (VenueId)"))
        .get();
  }

  @AfterClass
  public static void dropTestDatabase() throws Exception {
    dbClient.dropDatabase(dbId.getInstanceId().getInstance(), dbId.getDatabase());
    spanner.close();
  }

  @Before
  public void deleteTestData() {
    String projectId = spanner.getOptions().getProjectId();
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
    client.write(Collections.singleton(Mutation.delete("Singers", KeySet.all())));
    client.write(Collections.singleton(Mutation.delete("Venues", KeySet.all())));
  }

  @Test
  public void executeSqlWithCustomTimeoutAndRetrySettings_shouldWriteData() {
    String projectId = spanner.getOptions().getProjectId();
    String out =
        runExample(
            () ->
                CustomTimeoutAndRetrySettingsExample.executeSqlWithCustomTimeoutAndRetrySettings(
                    projectId, instanceId, databaseId));
    assertThat(out).contains("1 record inserted.");
  }

  @Test
  public void executeSqlWithTimeout_shouldWriteData() {
    String projectId = spanner.getOptions().getProjectId();
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
    String out = runExample(() -> StatementTimeoutExample.executeSqlWithTimeout(client));
    assertThat(out).contains("1 record inserted.");
  }

  @Test
  public void addNumericColumn_shouldSuccessfullyAddColumn()
      throws InterruptedException, ExecutionException {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> operation =
        spanner
            .getDatabaseAdminClient()
            .updateDatabaseDdl(
                instanceId,
                databaseId,
                ImmutableList.of("ALTER TABLE Venues DROP COLUMN Revenue"),
                null);
    operation.get();
    String out =
        runExample(
            () -> {
              try {
                AddNumericColumnSample.addNumericColumn(
                    spanner.getDatabaseAdminClient(), instanceId, databaseId);
              } catch (ExecutionException e) {
                System.out.printf(
                    "Adding column `Revenue` failed: %s%n", e.getCause().getMessage());
              } catch (InterruptedException e) {
                System.out.printf("Adding column `Revenue` was interrupted%n");
              }
            });
    assertThat(out).contains("Successfully added column `Revenue`");
  }

  @Test
  public void updateNumericData_shouldWriteData() {
    String projectId = spanner.getOptions().getProjectId();
    String out =
        runExample(
            () ->
                UpdateNumericDataSample.updateNumericData(
                    spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId))));
    assertThat(out).contains("Venues successfully updated");
  }

  @Test
  public void queryWithNumericParameter_shouldReturnResults() {
    String projectId = spanner.getOptions().getProjectId();
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
    client.write(
        ImmutableList.of(
            Mutation.newInsertOrUpdateBuilder("Venues")
                .set("VenueId")
                .to(4L)
                .set("Revenue")
                .to(new BigDecimal("35000"))
                .build(),
            Mutation.newInsertOrUpdateBuilder("Venues")
                .set("VenueId")
                .to(19L)
                .set("Revenue")
                .to(new BigDecimal("104500"))
                .build(),
            Mutation.newInsertOrUpdateBuilder("Venues")
                .set("VenueId")
                .to(42L)
                .set("Revenue")
                .to(new BigDecimal("99999999999999999999999999999.99"))
                .build()));
    String out =
        runExample(() -> QueryWithNumericParameterSample.queryWithNumericParameter(client));
    assertThat(out).contains("4 35000");
  }
}