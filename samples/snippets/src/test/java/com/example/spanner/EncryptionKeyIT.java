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
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for: {@link CreateDatabaseWithEncryptionKey}, {@link
 * CreateBackupWithEncryptionKey} and {@link RestoreBackupWithEncryptionKey}
 */
@RunWith(JUnit4.class)
@Ignore
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

    String keyLocation = Preconditions
        .checkNotNull(System.getProperty("spanner.test.key.location"));
    String keyRing = Preconditions.checkNotNull(System.getProperty("spanner.test.key.ring"));
    String keyName = Preconditions.checkNotNull(System.getProperty("spanner.test.key.name"));
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

    out = SampleRunner.runSampleWithRetry(() ->
        CreateBackupWithEncryptionKey.createBackupWithEncryptionKey(
            databaseAdminClient,
            projectId,
            instanceId,
            databaseId,
            backupId,
            key
        ), new ShouldRetryBackupOperation());
    assertThat(out).containsMatch(
        "Backup projects/" + projectId + "/instances/" + instanceId + "/backups/" + backupId
            + " of size \\d+ bytes was created at (.*) using encryption key " + key);

    out = SampleRunner.runSampleWithRetry(() ->
        RestoreBackupWithEncryptionKey.restoreBackupWithEncryptionKey(
            databaseAdminClient,
            projectId,
            instanceId,
            backupId,
            restoreId,
            key
        ), new ShouldRetryBackupOperation());
    assertThat(out).contains(
        "Database projects/" + projectId + "/instances/" + instanceId + "/databases/" + databaseId
            + " restored to projects/" + projectId + "/instances/" + instanceId + "/databases/"
            + restoreId + " from backup projects/" + projectId + "/instances/" + instanceId
            + "/backups/" + backupId + " using encryption key " + key);
  }

  static class ShouldRetryBackupOperation implements Predicate<SpannerException> {
    private static final int MAX_ATTEMPTS = 20;
    private int attempts = 0;

    @Override
    public boolean test(SpannerException e) {
      if (e.getErrorCode() == ErrorCode.FAILED_PRECONDITION
          && e.getMessage().contains("Please retry the operation once the pending")) {
        attempts++;
        if (attempts == MAX_ATTEMPTS) {
          // Throw custom exception so it is easier to locate in the log why it went wrong.
          throw SpannerExceptionFactory.newSpannerException(ErrorCode.DEADLINE_EXCEEDED,
              String.format("Operation failed %d times because of other pending operations. "
                  + "Giving up operation.\n", attempts),
              e);
        }
        // Wait one minute before retrying.
        Uninterruptibles.sleepUninterruptibly(60L, TimeUnit.SECONDS);
        return true;
      }
      return false;
    }
  }
}
