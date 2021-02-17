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

package com.google.cloud.spanner.it;

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static com.google.cloud.spanner.testing.TimestampHelper.afterDays;
import static com.google.cloud.spanner.testing.TimestampHelper.daysAgo;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeFalse;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Backup;
import com.google.cloud.spanner.BackupId;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITPitrBackupAndRestore {
  private static final Logger logger = Logger.getLogger(ITPitrBackupAndRestore.class.getName());

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static final long OP_TIMEOUT = 10;
  private static final TimeUnit OP_TIMEOUT_UNIT = TimeUnit.MINUTES;
  private static RemoteSpannerHelper testHelper;
  private static DatabaseAdminClient dbAdminClient;
  private static Database testDatabase;
  private static final List<Backup> backupsToDrop = new ArrayList<>();
  private static final List<Database> databasesToDrop = new ArrayList<>();

  @BeforeClass
  public static void doNotRunOnEmulator() {
    assumeFalse("PITR features are not supported by the emulator", isUsingEmulator());
  }

  @BeforeClass
  public static void setUp() throws Exception {
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
    testDatabase = createTestDatabase();
  }

  @AfterClass
  public static void tearDown() {
    int numDropped = 0;
    for (Database database : databasesToDrop) {
      try {
        database.drop();
        numDropped++;
      } catch (SpannerException e) {
        logger.log(Level.SEVERE, "Failed to drop test database " + database.getId(), e);
      }
    }
    logger.log(Level.INFO, "Dropped {0} test databases(s)", numDropped);

    numDropped = 0;
    for (Backup backup : backupsToDrop) {
      try {
        backup.delete();
        numDropped++;
      } catch (SpannerException e) {
        logger.log(Level.SEVERE, "Failed to drop test backup " + backup.getId(), e);
      }
    }
    logger.log(Level.INFO, "Dropped {0} test backup(s)", numDropped);
  }

  @Test
  @Ignore("backup and restore for pitr is not released yet")
  public void backupCreationWithVersionTimeWithinVersionRetentionPeriodSucceeds() throws Exception {
    final DatabaseId backupDatabaseId = testDatabase.getId();
    final String restoreDatabaseId = testHelper.getUniqueDatabaseId();
    final String projectId = backupDatabaseId.getInstanceId().getProject();
    final String instanceId = backupDatabaseId.getInstanceId().getInstance();
    final String backupId = testHelper.getUniqueBackupId();
    final Timestamp expireTime = afterDays(7);
    final Timestamp versionTime = testDatabase.getEarliestVersionTime();
    final Backup backupToCreate =
        dbAdminClient
            .newBackupBuilder(BackupId.of(projectId, instanceId, backupId))
            .setDatabase(backupDatabaseId)
            .setExpireTime(expireTime)
            .setVersionTime(versionTime)
            .build();

    final Backup createdBackup = createBackup(backupToCreate);
    assertThat(createdBackup.getVersionTime()).isEqualTo(versionTime);

    final RestoreDatabaseMetadata restoreDatabaseMetadata =
        restoreDatabase(instanceId, backupId, restoreDatabaseId);
    assertThat(Timestamp.fromProto(restoreDatabaseMetadata.getBackupInfo().getVersionTime()))
        .isEqualTo(versionTime);

    final Database retrievedDatabase = dbAdminClient.getDatabase(instanceId, restoreDatabaseId);
    assertThat(retrievedDatabase).isNotNull();
    assertThat(
            Timestamp.fromProto(
                retrievedDatabase.getRestoreInfo().getProto().getBackupInfo().getVersionTime()))
        .isEqualTo(versionTime);

    final Database listedDatabase = listDatabase(instanceId, restoreDatabaseId);
    assertThat(listedDatabase).isNotNull();
    assertThat(
            Timestamp.fromProto(
                listedDatabase.getRestoreInfo().getProto().getBackupInfo().getVersionTime()))
        .isEqualTo(versionTime);
  }

  @Test(expected = SpannerException.class)
  @Ignore("backup and restore for pitr is not released yet")
  public void backupCreationWithVersionTimeTooFarInThePastFails() throws Exception {
    final DatabaseId databaseId = testDatabase.getId();
    final InstanceId instanceId = databaseId.getInstanceId();
    final String backupId = testHelper.getUniqueBackupId();
    final Timestamp expireTime = afterDays(7);
    final Timestamp versionTime = daysAgo(30);
    final Backup backupToCreate =
        dbAdminClient
            .newBackupBuilder(BackupId.of(instanceId, backupId))
            .setDatabase(databaseId)
            .setExpireTime(expireTime)
            .setVersionTime(versionTime)
            .build();

    createBackup(backupToCreate);
  }

  @Test(expected = SpannerException.class)
  @Ignore("backup and restore for pitr is not released yet")
  public void backupCreationWithVersionTimeInTheFutureFails() throws Exception {
    final DatabaseId databaseId = testDatabase.getId();
    final InstanceId instanceId = databaseId.getInstanceId();
    final String backupId = testHelper.getUniqueBackupId();
    final Timestamp expireTime = afterDays(7);
    final Timestamp versionTime = afterDays(1);
    final Backup backupToCreate =
        dbAdminClient
            .newBackupBuilder(BackupId.of(instanceId, backupId))
            .setDatabase(databaseId)
            .setExpireTime(expireTime)
            .setVersionTime(versionTime)
            .build();

    createBackup(backupToCreate);
  }

  private Backup createBackup(Backup backupToCreate)
      throws InterruptedException, ExecutionException, TimeoutException {
    final Backup createdBackup = getOrThrow(dbAdminClient.createBackup(backupToCreate));
    backupsToDrop.add(createdBackup);
    return createdBackup;
  }

  private RestoreDatabaseMetadata restoreDatabase(
      String instanceId, String backupId, String databaseId)
      throws InterruptedException, ExecutionException, TimeoutException {
    final OperationFuture<Database, RestoreDatabaseMetadata> op =
        dbAdminClient.restoreDatabase(instanceId, backupId, instanceId, databaseId);
    final Database database = getOrThrow(op);
    databasesToDrop.add(database);
    return op.getMetadata().get(OP_TIMEOUT, OP_TIMEOUT_UNIT);
  }

  private Database listDatabase(String instanceId, String databaseId) {
    Page<Database> page = dbAdminClient.listDatabases(instanceId);
    while (page != null) {
      for (Database database : page.getValues()) {
        if (database.getId().getDatabase().equals(databaseId)) {
          return database;
        }
      }
      page = page.getNextPage();
    }
    return null;
  }

  private static Database createTestDatabase()
      throws InterruptedException, ExecutionException, TimeoutException {
    final String instanceId = testHelper.getInstanceId().getInstance();
    final String databaseId = testHelper.getUniqueDatabaseId();
    final OperationFuture<Database, CreateDatabaseMetadata> op =
        dbAdminClient.createDatabase(
            instanceId,
            databaseId,
            Collections.singletonList(
                "ALTER DATABASE " + databaseId + " SET OPTIONS (version_retention_period = '7d')"));
    final Database database = getOrThrow(op);
    databasesToDrop.add(database);
    return database;
  }

  private static <T> T getOrThrow(OperationFuture<T, ?> op)
      throws TimeoutException, InterruptedException, ExecutionException {
    try {
      return op.get(OP_TIMEOUT, OP_TIMEOUT_UNIT);
    } catch (ExecutionException e) {
      if (e.getCause() != null && e.getCause() instanceof SpannerException) {
        throw (SpannerException) e.getCause();
      } else {
        throw e;
      }
    }
  }
}
