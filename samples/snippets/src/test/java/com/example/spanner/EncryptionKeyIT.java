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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for: {@link CreateDatabaseWithEncryptionKey}, {@link
 * CreateBackupWithEncryptionKey} and {@link RestoreBackupWithEncryptionKey}
 */
@RunWith(JUnit4.class)
public class EncryptionKeyIT {

  private static String projectId;
  private static final String instanceId = System.getProperty("spanner.test.instance");
  private static final List<String> databasesToDrop = new ArrayList<>();
  private static final List<String> backupsToDrop = new ArrayList<>();
  private static DatabaseAdminClient databaseAdminClient;
  private static Spanner spanner;
  private static String key;

  @BeforeClass
  public static void setUp() {
    final SpannerOptions options = SpannerOptions
        .newBuilder()
        .setAutoThrottleAdministrativeRequests()
        .build();
    projectId = options.getProjectId();
    spanner = options.getService();
    databaseAdminClient = spanner.getDatabaseAdminClient();

    String keyLocation = System.getProperty("spanner.test.key.location");
    String keyRing = System.getProperty("spanner.test.key.ring");
    String keyName = System.getProperty("spanner.test.key.name");
    key = "projects/" + projectId + "/locations/" + keyLocation + "/keyRings/" + keyRing
        + "/cryptoKeys/" + keyName;
  }

  @AfterClass
  public static void tearDown() {
    for (String databaseId : databasesToDrop) {
      try {
        databaseAdminClient.dropDatabase(instanceId, databaseId);
      } catch (Exception e) {
        System.out.println("Failed to drop database " + databaseId + ", skipping...");
      }
    }
    for (String backupId : backupsToDrop) {
      try {
        databaseAdminClient.deleteBackup(instanceId, backupId);
      } catch (Exception e) {
        System.out.println("Failed to drop backup " + backupId + ", skipping...");
      }
    }
    spanner.close();
  }

  @Test
  public void testEncryptedDatabaseAndBackupAndRestore() throws Exception {
    final String databaseId = DatabaseIdGenerator.generateDatabaseId();
    final String backupId = DatabaseIdGenerator.generateDatabaseId();
    final String restoreId = DatabaseIdGenerator.generateDatabaseId();

    databasesToDrop.add(databaseId);
    backupsToDrop.add(backupId);
    databasesToDrop.add(restoreId);

    String out = SampleRunner.runSample(() ->
        CreateDatabaseWithEncryptionKey.createDatabaseWithEncryptionKey(
            databaseAdminClient,
            projectId,
            instanceId,
            databaseId,
            key
        ));
    assertThat(out).contains(
        "Database projects/" + projectId + "/instances/" + instanceId + "/databases/" + databaseId
            + " created with encryption key " + key);

    out = SampleRunner.runSample(() ->
        CreateBackupWithEncryptionKey.createBackupWithEncryptionKey(
            databaseAdminClient,
            projectId,
            instanceId,
            databaseId,
            backupId,
            key
        ));
    assertThat(out).containsMatch(
        "Backup projects/" + projectId + "/instances/" + instanceId + "/backups/" + backupId
            + " of size \\d+ bytes was created at (.*) using encryption key " + key);

    out = SampleRunner.runSample(() ->
        RestoreBackupWithEncryptionKey.restoreBackupWithEncryptionKey(
            databaseAdminClient,
            projectId,
            instanceId,
            backupId,
            restoreId,
            key
        ));
    assertThat(out).contains(
        "Database projects/" + projectId + "/instances/" + instanceId + "/databases/" + databaseId
            + " restored to projects/" + projectId + "/instances/" + instanceId + "/databases/"
            + restoreId + " from backup projects/" + projectId + "/instances/" + instanceId
            + "/backups/" + backupId + " using encryption key " + key);
  }
}
