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
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
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
  private static List<Backup> backupsToDrop;

  @BeforeClass
  public static void doNotRunOnEmulator() {
    assumeFalse("PITR features are not supported by the emulator", isUsingEmulator());
  }

  @BeforeClass
  public static void setUp() throws Exception {
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
    testDatabase = createTestDatabase();
    backupsToDrop = new ArrayList<>();
  }

  @AfterClass
  public static void tearDown() {
    int droppedBackups = 0;
    for (Backup backup : backupsToDrop) {
      try {
        backup.delete();
        droppedBackups++;
      } catch (SpannerException e) {
        logger.log(Level.SEVERE, "Failed to drop test backup " + backup.getId(), e);
      }
    }
    logger.log(Level.INFO, "Dropped {0} test backup(s)", droppedBackups);
  }

  @Test
  public void backupCreationWithVersionTimeWithinVersionRetentionPeriodSucceeds() throws Exception {
    final DatabaseId databaseId = testDatabase.getId();
    final InstanceId instanceId = databaseId.getInstanceId();
    final String backupId = testHelper.getUniqueBackupId();
    final Timestamp expireTime = afterDays(7);
    final Timestamp versionTime = daysAgo(3);
    final Backup backupToCreate =
        dbAdminClient
            .newBackupBuilder(BackupId.of(instanceId, backupId))
            .setDatabase(databaseId)
            .setExpireTime(expireTime)
            .setVersionTime(versionTime)
            .build();

    final Backup createdBackup = createBackup(backupToCreate);

    assertThat(createdBackup.getVersionTime()).isEqualTo(backupToCreate.getVersionTime());
  }

  @Test(expected = SpannerException.class)
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

  private Backup createBackup(Backup backup)
      throws InterruptedException, ExecutionException, TimeoutException {
    backupsToDrop.add(backup);
    return dbAdminClient.createBackup(backup).get(OP_TIMEOUT, OP_TIMEOUT_UNIT);
  }

  private static Database createTestDatabase()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Database database = testHelper.createTestDatabase();
    final String instanceId = database.getId().getInstanceId().getInstance();
    final String databaseId = database.getId().getDatabase();
    final String statement =
        "ALTER DATABASE " + databaseId + " SET OPTIONS (version_retention_period = '7d')";
    final OperationFuture<Void, UpdateDatabaseDdlMetadata> op =
        dbAdminClient.updateDatabaseDdl(
            instanceId, databaseId, Collections.singletonList(statement), "op_" + databaseId);
    op.get(OP_TIMEOUT, OP_TIMEOUT_UNIT);
    return database;
  }
}
