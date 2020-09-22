/*
 * Copyright 2020 Google LLC
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

import com.google.api.gax.paging.Page;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITPitrUpdateDdlTest {

  private static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(1);
  private static final String VERSION_RETENTION_PERIOD = "7d";

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static DatabaseAdminClient dbAdminClient;
  private static DatabaseClient dbClient;
  private static String projectId;
  private static String instanceId;
  private static String databaseId;

  @BeforeClass
  public static void setUp() throws Exception {
    final RemoteSpannerHelper testHelper = env.getTestHelper();
    projectId = testHelper.getOptions().getProjectId();
    instanceId = testHelper.getInstanceId().getInstance();
    databaseId = testHelper.getUniqueDatabaseId();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();

    createDatabase(dbAdminClient, instanceId, databaseId);
    updateVersionRetentionPeriod(dbAdminClient, instanceId, databaseId, VERSION_RETENTION_PERIOD);

    dbClient =
        testHelper.getClient().getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
  }

  @AfterClass
  public static void tearDown() {
    dbAdminClient.dropDatabase(instanceId, databaseId);
  }

  @Test
  public void returnsTheVersionRetentionPeriodSetThroughGetDatabase() {
    final Database database = dbAdminClient.getDatabase(instanceId, databaseId);

    assertThat(database.getVersionRetentionPeriod()).isEqualTo(VERSION_RETENTION_PERIOD);
    assertThat(database.getEarliestVersionTime()).isNotNull();
  }

  @Test
  public void returnsTheVersionRetentionPeriodSetThroughListDatabases() {
    final Page<Database> page = dbAdminClient.listDatabases(instanceId);

    for (Database database : page.iterateAll()) {
      if (!database.getId().getDatabase().equals(databaseId)) {
        continue;
      }
      assertThat(database.getVersionRetentionPeriod()).isEqualTo(VERSION_RETENTION_PERIOD);
      assertThat(database.getEarliestVersionTime()).isNotNull();
    }
  }

  @Test
  public void returnsTheVersionRetentionPeriodSetThroughGetDatabaseDdl() {
    final List<String> ddls = dbAdminClient.getDatabaseDdl(instanceId, databaseId);

    boolean hasVersionRetentionPeriodStatement = false;
    for (String ddl : ddls) {
      hasVersionRetentionPeriodStatement =
          ddl.contains("version_retention_period = '" + VERSION_RETENTION_PERIOD + "'");
      if (hasVersionRetentionPeriodStatement) {
        break;
      }
    }
    assertThat(hasVersionRetentionPeriodStatement).isTrue();
  }

  @Test
  public void returnsTheVersionRetentionPeriodSetThroughInformationSchema() {
    final ResultSet rs =
        dbClient
            .singleUse()
            .executeQuery(
                Statement.of(
                    "SELECT OPTION_VALUE AS version_retention_period "
                        + "FROM INFORMATION_SCHEMA.DATABASE_OPTIONS "
                        + "WHERE SCHEMA_NAME = '' AND OPTION_NAME = 'version_retention_period'"));

    String versionRetentionPeriod = null;
    while (rs.next()) {
      versionRetentionPeriod = rs.getString("version_retention_period");
    }

    assertThat(versionRetentionPeriod).isEqualTo(VERSION_RETENTION_PERIOD);
  }

  private static void createDatabase(
      final DatabaseAdminClient dbAdminClient, final String instanceId, final String databaseId)
      throws Exception {
    dbAdminClient
        .createDatabase(instanceId, databaseId, Collections.<String>emptyList())
        .get(OPERATION_TIMEOUT.toNanos(), TimeUnit.NANOSECONDS);
  }

  private static void updateVersionRetentionPeriod(
      final DatabaseAdminClient dbAdminClient,
      final String instanceId,
      final String databaseId,
      final String versionRetentionPeriod)
      throws Exception {
    dbAdminClient
        .updateDatabaseDdl(
            instanceId,
            databaseId,
            Collections.singletonList(
                "ALTER DATABASE "
                    + databaseId
                    + " SET OPTIONS ( version_retention_period = '"
                    + versionRetentionPeriod
                    + "' )"),
            "updateddl_version_retention_period")
        .get(OPERATION_TIMEOUT.toNanos(), TimeUnit.NANOSECONDS);
  }
}
