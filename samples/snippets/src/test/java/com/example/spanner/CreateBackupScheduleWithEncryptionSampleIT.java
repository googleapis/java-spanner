/*
 * Copyright 2024 Google LLC
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Preconditions;
import com.google.spanner.admin.database.v1.BackupScheduleName;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CreateBackupScheduleWithEncryptionSampleIT extends SampleTestBaseV2 {
  // Default instance, given db and kms key should exist for tests to pass.
  private static String databaseId = System.getProperty("spanner.sample.database", "mysample");
  private static String key;

  @BeforeClass
  public static void setUp() {
    String keyLocation =
        Preconditions.checkNotNull(System.getProperty("spanner.test.key.location"));
    String keyRing = Preconditions.checkNotNull(System.getProperty("spanner.test.key.ring"));
    String keyName = Preconditions.checkNotNull(System.getProperty("spanner.test.key.name"));
    key =
        "projects/"
            + projectId
            + "/locations/"
            + keyLocation
            + "/keyRings/"
            + keyRing
            + "/cryptoKeys/"
            + keyName;
  }

  @Test
  public void testCreateBackupScheduleWithEncryptionSample() throws Exception {
    String backupScheduleId = String.format("schedule-%s", UUID.randomUUID());
    BackupScheduleName backupScheduleName =
        BackupScheduleName.of(projectId, instanceId, databaseId, backupScheduleId);
    String out =
        SampleRunner.runSample(
            () -> {
              try {
                CreateBackupScheduleWithEncryptionSample.createBackupScheduleWithEncryption(
                    projectId, instanceId, databaseId, backupScheduleId, key);
              } finally {
                DeleteBackupScheduleSample.deleteBackupSchedule(
                    projectId, instanceId, databaseId, backupScheduleId);
              }
            });
    assertThat(out)
        .contains(String.format("Created backup schedule with encryption: %s", backupScheduleName));
  }
}
