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

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base class for sample integration tests.
 */
public class SampleTestBase {

  private static final String BASE_DATABASE_ID = System.getProperty(
      "spanner.sample.database",
      "sampledb"
  );
  private static final String BASE_BACKUP_ID = "samplebk";

  protected static Spanner spanner;
  protected static DatabaseAdminClient databaseAdminClient;
  protected static String projectId;
  protected static final String instanceId = System.getProperty("spanner.test.instance");
  protected static SampleIdGenerator idGenerator;

  @BeforeClass
  public static void setUp() {
    final SpannerOptions options = SpannerOptions
        .newBuilder()
        .setAutoThrottleAdministrativeRequests()
        .build();
    projectId = options.getProjectId();
    spanner = options.getService();
    databaseAdminClient = spanner.getDatabaseAdminClient();
    idGenerator = new SampleIdGenerator(BASE_DATABASE_ID, BASE_BACKUP_ID);
  }

  @AfterClass
  public static void tearDown() {
    for (String databaseId : idGenerator.getDatabaseIds()) {
      try {
        databaseAdminClient.dropDatabase(instanceId, databaseId);
      } catch (Exception e) {
        System.out.println("Failed to drop database " + databaseId + ", skipping...");
      }
    }
    for (String backupId : idGenerator.getBackupIds()) {
      try {
        databaseAdminClient.deleteBackup(instanceId, backupId);
      } catch (Exception e) {
        System.out.println("Failed to delete backup " + backupId + ", skipping...");
      }
    }
    spanner.close();
  }
}
