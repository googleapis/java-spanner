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

// [START spanner_restore_backup_with_multiple_encryption_key]

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.BackupId;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Restore;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.encryption.EncryptionConfigs;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import java.util.concurrent.ExecutionException;

public class RestoreBackupWithMultipleEncryptionKey {

  static void restoreBackupWithMultipleEncryptionKey() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";
    String backupId = "my-backup";
    String kmsKeyName1 =
        "projects/" + projectId + "/locations/<location>/keyRings/<keyRing>/cryptoKeys/<keyId>";
    String kmsKeyName2 =
        "projects/" + projectId + "/locations/<location>/keyRings/<keyRing>/cryptoKeys/<keyId>";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseAdminClient adminClient = spanner.getDatabaseAdminClient();
      restoreBackupWithMultipleEncryptionKey(
          adminClient,
          projectId,
          instanceId,
          backupId,
          databaseId,
          kmsKeyName);
    }
  }

  static void restoreBackupWithMultipleEncryptionKey(DatabaseAdminClient adminClient,
      String projectId, String instanceId, String backupId, String restoreId, String kmsKeyName1, String kmsKeyName2) {
    final Restore restore = adminClient
        .newRestoreBuilder(
            BackupId.of(projectId, instanceId, backupId),
            DatabaseId.of(projectId, instanceId, restoreId))
        .setEncryptionConfig(EncryptionConfigs.customerManagedEncryption(kmsKeyName1, kmsKeyName2))
        .build();
    final OperationFuture<Database, RestoreDatabaseMetadata> operation = adminClient
        .restoreDatabase(restore);

    Database database;
    try {
      System.out.println("Waiting for operation to complete...");
      database = operation.get();
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw SpannerExceptionFactory.asSpannerException(e.getCause());
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }

    System.out.printf(
        "Database %s restored to %s from backup %s using encryption key %s%n",
        database.getRestoreInfo().getSourceDatabase(),
        database.getId(),
        database.getRestoreInfo().getBackup(),
        database.getEncryptionConfig().getKmsKeyNames()
    );
  }
}
// [END spanner_restore_backup_with_multiple_encryption_key]
