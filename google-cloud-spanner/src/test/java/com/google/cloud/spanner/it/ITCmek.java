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
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.kms.v1.CryptoKey;
import com.google.cloud.kms.v1.CryptoKey.CryptoKeyPurpose;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyRing;
import com.google.cloud.kms.v1.KeyRingName;
import com.google.cloud.kms.v1.LocationName;
import com.google.cloud.spanner.Backup;
import com.google.cloud.spanner.BackupId;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.EncryptionConfigInfo;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.Restore;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.protobuf.Timestamp;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import java.io.IOException;
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

  private static final String BACKUP_ID_PREFIX = "spanner-test-backup";
  private static final String KMS_KEY_LOCATION = "eur5";
  private static final String KMS_KEY_RING_ID = "spanner-test-keyring";
  private static final String KMS_KEY_ID_PREFIX = "spanner-test-key";
  private static final List<CryptoKey> keys = new ArrayList<>();
  private static final List<DatabaseId> dbs = new ArrayList<>();
  private static final List<BackupId> backups = new ArrayList<>();
  public static final String SPANNER_PRODUCTION_ACCOUNT = "serviceAccount:service-353504090643@gcp-sa-spanner.iam.gserviceaccount.com";
  public static final String KMS_KEY_ENCRYPTER_DECRYPTER = "roles/cloudkms.cryptoKeyEncrypterDecrypter";

  @ClassRule
  public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static KeyManagementServiceClient kmsClient;
  private static DatabaseAdminClient dbAdminClient;

  private static RemoteSpannerHelper testHelper;
  private static Random random;

  @BeforeClass
  public static void beforeClass() throws IOException {
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
    kmsClient = KeyManagementServiceClient.create();
    random = new Random();
  }

  @AfterClass
  public static void afterClass() {
    // for (CryptoKey key : keys) {
    //   for (CryptoKeyVersion keyVersion : kmsClient.listCryptoKeyVersions(key.getName())
    //       .iterateAll()) {
    //     kmsClient.destroyCryptoKeyVersion(keyVersion.getName());
    //   }
    // }
    for (DatabaseId db : dbs) {
      dbAdminClient
          .dropDatabase(db.getInstanceId().getInstance(), db.getDatabase());
    }
    for (BackupId backup : backups) {
      dbAdminClient.deleteBackup(backup.getInstanceId().getInstance(), backup.getBackup());
    }
    kmsClient.close();
  }

  @Test
  public void createsEncryptedDatabaseBackupAndRestore() throws ExecutionException, InterruptedException {
    final InstanceId instanceId = testHelper.getInstanceId();
    final String sourceDatabaseId = testHelper.getUniqueDatabaseId();
    final String destinationDatabaseId = testHelper.getUniqueDatabaseId();
    final String backupId = randomBackupId();

    final CryptoKey key = createKey(randomKeyId());
    final Database sourceDatabase = dbAdminClient
        .newDatabaseBuilder(DatabaseId.of(instanceId, sourceDatabaseId))
        .setEncryptionConfigInfo(EncryptionConfigInfo.ofKey(key.getName()))
        .build();
    final Backup backup = dbAdminClient
        .newBackupBuilder(BackupId.of(
            testHelper.getInstanceId(),
            backupId
        ))
        .setDatabase(DatabaseId.of(instanceId, sourceDatabaseId))
        .setEncryptionConfigInfo(EncryptionConfigInfo.ofKey(key.getName()))
        .setExpireTime(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(after7DaysInSeconds(), 0))
        .build();
    final Restore restore = dbAdminClient
        .newRestoreBuilder(
            BackupId.of(testHelper.getInstanceId(), backupId),
            DatabaseId.of(testHelper.getInstanceId(), destinationDatabaseId)
        )
        .setEncryptionConfigInfo(EncryptionConfigInfo.ofKey(key.getName()))
        .build();

    final Database createdDatabase = createDatabase(sourceDatabase);
    final Backup createdBackup = createBackup(backup);
    final Database restoredDatabase = restoreDatabase(restore);

    assertThat(createdDatabase.getEncryptionConfigInfo()).isNotNull();
    assertThat(createdDatabase.getEncryptionConfigInfo().getKmsKeyName()).isEqualTo(key.getName());
    assertThat(createdBackup.getEncryptionInfo().getKmsKeyVersion()).isNotNull();
    assertThat(restoredDatabase.getEncryptionConfigInfo()).isNotNull();
    assertThat(restoredDatabase.getEncryptionConfigInfo().getKmsKeyName()).isEqualTo(key.getName());
  }

  private String randomKeyId() {
    return KMS_KEY_ID_PREFIX + random.nextInt();
  }

  private String randomBackupId() {
    return BACKUP_ID_PREFIX + random.nextInt();
  }

  private CryptoKey createKey(final String keyId) {
    final LocationName locationName = LocationName.of(
        testHelper.getOptions().getProjectId(),
        KMS_KEY_LOCATION
    );
    final KeyRing keyRing = createOrRetrieveKeyRing(locationName);
    final Timestamp.Builder rotationTime = Timestamp
        .newBuilder()
        .setSeconds(after7DaysInSeconds());

    final CryptoKey cryptoKeyInput = CryptoKey.newBuilder()
        .setPurpose(CryptoKeyPurpose.ENCRYPT_DECRYPT)
        .setNextRotationTime(rotationTime)
        .build();
    final CryptoKey cryptoKey = kmsClient
        .createCryptoKey(KeyRingName.parse(keyRing.getName()), keyId, cryptoKeyInput);

    final Policy policy = kmsClient.getIamPolicy(cryptoKey.getName());
    final Binding binding = Binding
        .newBuilder()
        .addMembers(SPANNER_PRODUCTION_ACCOUNT)
        .setRole(KMS_KEY_ENCRYPTER_DECRYPTER)
        .build();
    final Policy newPolicy = policy.toBuilder().addBindings(binding).build();
    kmsClient.setIamPolicy(cryptoKey.getName(), newPolicy);

    keys.add(cryptoKey);
    return cryptoKey;
  }

  private long after7DaysInSeconds() {
    return TimeUnit.SECONDS.convert(
        System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(7L, TimeUnit.DAYS),
        TimeUnit.MILLISECONDS
    );
  }

  private KeyRing createOrRetrieveKeyRing(final LocationName locationName) {
    try {
      return kmsClient.getKeyRing(
          KeyRingName.of(locationName.getProject(), locationName.getLocation(), KMS_KEY_RING_ID)
      );
    } catch (NotFoundException e) {
      return kmsClient.createKeyRing(locationName, KMS_KEY_RING_ID, KeyRing.getDefaultInstance());
    }
  }

  private Database createDatabase(final Database database)
      throws ExecutionException, InterruptedException {
    final OperationFuture<Database, CreateDatabaseMetadata> op = dbAdminClient
        .createDatabase(database, Collections.<String>emptyList());
    final Database createdDatabase = op.get();
    dbs.add(createdDatabase.getId());

    return createdDatabase;
  }

  private Backup createBackup(final Backup backup) throws ExecutionException, InterruptedException {
    final OperationFuture<Backup, CreateBackupMetadata> op = dbAdminClient
        .createBackup(backup);
    final Backup createdBackup = op.get();
    dbs.add(createdBackup.getDatabase());
    backups.add(backup.getId());

    return createdBackup;
  }

  private Database restoreDatabase(final Restore restore)
      throws ExecutionException, InterruptedException {
    final OperationFuture<Database, RestoreDatabaseMetadata> op = dbAdminClient
        .restoreDatabase(restore);
    final Database database = op.get();
    dbs.add(database.getId());

    return database;
  }
}
