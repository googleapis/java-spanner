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

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Backup;
import com.google.cloud.spanner.BackupId;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.Restore;
import com.google.cloud.spanner.encryption.EncryptionConfigs;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.base.Preconditions;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITCmek {

  private static final String KMS_KEY_NAME_PROPERTY = "spanner.testenv.kms_key.name";
  private static final String BACKUP_ID_PREFIX = "spanner-test-backup";
  private static final List<DatabaseId> dbs = new ArrayList<>();
  private static final List<BackupId> backups = new ArrayList<>();

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static DatabaseAdminClient dbAdminClient;

  private static RemoteSpannerHelper testHelper;
  private static Random random;
  private static String keyName;

  @BeforeClass
  public static void beforeClass() {
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
    random = new Random();
    keyName = System.getProperty(KMS_KEY_NAME_PROPERTY);
    Preconditions.checkNotNull(
        keyName,
        "Key name is null, please set a key to be used for this test. The necessary permissions should be grant to the spanner service account according to the CMEK user guide.");
  }

  @AfterClass
  public static void afterClass() {
    for (DatabaseId db : dbs) {
      dbAdminClient.dropDatabase(db.getInstanceId().getInstance(), db.getDatabase());
    }
    for (BackupId backup : backups) {
      dbAdminClient.deleteBackup(backup.getInstanceId().getInstance(), backup.getBackup());
    }
  }

  @Test
  public void createsEncryptedDatabaseBackupAndRestore()
      throws ExecutionException, InterruptedException {
    final InstanceId instanceId = testHelper.getInstanceId();
    final String sourceDatabaseId = testHelper.getUniqueDatabaseId();
    final String destinationDatabaseId = testHelper.getUniqueDatabaseId();
    final String backupId = randomBackupId();

    final Database sourceDatabase =
        dbAdminClient
            .newDatabaseBuilder(DatabaseId.of(instanceId, sourceDatabaseId))
            .setEncryptionConfig(EncryptionConfigs.customerManagedEncryption(keyName))
            .build();
    final Backup backup =
        dbAdminClient
            .newBackupBuilder(BackupId.of(testHelper.getInstanceId(), backupId))
            .setDatabase(DatabaseId.of(instanceId, sourceDatabaseId))
            .setEncryptionConfig(EncryptionConfigs.customerManagedEncryption(keyName))
            .setExpireTime(
                com.google.cloud.Timestamp.ofTimeSecondsAndNanos(after7DaysInSeconds(), 0))
            .build();
    final Restore restore =
        dbAdminClient
            .newRestoreBuilder(
                BackupId.of(testHelper.getInstanceId(), backupId),
                DatabaseId.of(testHelper.getInstanceId(), destinationDatabaseId))
            .setEncryptionConfig(EncryptionConfigs.customerManagedEncryption(keyName))
            .build();

    final Database createdDatabase = createDatabase(sourceDatabase);
    final Backup createdBackup = createBackup(backup);
    final Database restoredDatabase = restoreDatabase(restore);

    assertThat(createdDatabase.getEncryptionConfig()).isNotNull();
    assertThat(createdDatabase.getEncryptionConfig().getKmsKeyName()).isEqualTo(keyName);
    assertThat(createdBackup.getEncryptionInfo().getKmsKeyVersion()).isNotNull();
    assertThat(restoredDatabase.getEncryptionConfig()).isNotNull();
    assertThat(restoredDatabase.getEncryptionConfig().getKmsKeyName()).isEqualTo(keyName);
  }

  private String randomBackupId() {
    return BACKUP_ID_PREFIX + random.nextInt();
  }

  private long after7DaysInSeconds() {
    return TimeUnit.SECONDS.convert(
        System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(7L, TimeUnit.DAYS),
        TimeUnit.MILLISECONDS);
  }

  private Database createDatabase(final Database database)
      throws ExecutionException, InterruptedException {
    final OperationFuture<Database, CreateDatabaseMetadata> op =
        dbAdminClient.createDatabase(database, Collections.<String>emptyList());
    final Database createdDatabase = op.get();
    dbs.add(createdDatabase.getId());

    return createdDatabase;
  }

  private Backup createBackup(final Backup backup) throws ExecutionException, InterruptedException {
    final OperationFuture<Backup, CreateBackupMetadata> op = dbAdminClient.createBackup(backup);
    final Backup createdBackup = op.get();
    dbs.add(createdBackup.getDatabase());
    backups.add(backup.getId());

    return createdBackup;
  }

  private Database restoreDatabase(final Restore restore)
      throws ExecutionException, InterruptedException {
    final OperationFuture<Database, RestoreDatabaseMetadata> op =
        dbAdminClient.restoreDatabase(restore);
    final Database restoredDatabase = op.get();
    dbs.add(restoredDatabase.getId());

    return dbAdminClient
        .getDatabase(
            restoredDatabase.getId().getInstanceId().getInstance(),
            restoredDatabase.getId().getDatabase()
        );
  }
}
