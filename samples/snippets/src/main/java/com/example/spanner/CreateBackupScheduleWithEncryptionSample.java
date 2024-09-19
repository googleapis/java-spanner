/*
 * Copyright 2024 Google LLC
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

// [START spanner_create_backup_schedule_with_encryption_sample]

import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.protobuf.Duration;
import com.google.spanner.admin.database.v1.BackupSchedule;
import com.google.spanner.admin.database.v1.BackupScheduleSpec;
import com.google.spanner.admin.database.v1.CreateBackupEncryptionConfig;
import com.google.spanner.admin.database.v1.CreateBackupScheduleRequest;
import com.google.spanner.admin.database.v1.CrontabSpec;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.FullBackupSpec;
import java.io.IOException;

class CreateBackupScheduleWithEncryptionSample {

  static void createBackupScheduleWithEncryption() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";
    String backupScheduleId = "my-backup-schedule";
    String kmsKeyName =
        "projects/" + projectId + "/locations/<location>/keyRings/<keyRing>/cryptoKeys/<keyId>";
    createBackupScheduleWithEncryption(
        projectId, instanceId, databaseId, backupScheduleId, kmsKeyName);
  }

  static void createBackupScheduleWithEncryption(
      String projectId,
      String instanceId,
      String databaseId,
      String backupScheduleId,
      String kmsKeyName)
      throws IOException {
    final CreateBackupEncryptionConfig encryptionConfig =
        CreateBackupEncryptionConfig.newBuilder()
            .setEncryptionType(
                CreateBackupEncryptionConfig.EncryptionType.CUSTOMER_MANAGED_ENCRYPTION)
            .setKmsKeyName(kmsKeyName)
            .build();
    final BackupSchedule backupSchedule =
        BackupSchedule.newBuilder()
            .setFullBackupSpec(FullBackupSpec.newBuilder().build())
            .setRetentionDuration(Duration.newBuilder().setSeconds(3600 * 24 * 7))
            .setSpec(
                BackupScheduleSpec.newBuilder()
                    .setCronSpec(CrontabSpec.newBuilder().setText("0 0 * * *").build())
                    .build())
            .setEncryptionConfig(encryptionConfig)
            .build();

    try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
      DatabaseName databaseName = DatabaseName.of(projectId, instanceId, databaseId);
      final BackupSchedule createdBackupSchedule =
          databaseAdminClient.createBackupSchedule(
              CreateBackupScheduleRequest.newBuilder()
                  .setParent(databaseName.toString())
                  .setBackupScheduleId(backupScheduleId)
                  .setBackupSchedule(backupSchedule)
                  .build());
      System.out.println(
          String.format(
              "Created backup schedule with encryption: %s", createdBackupSchedule.getName()));
    }
  }
}
// [END spanner_create_backup_schedule_with_encryption_sample]
