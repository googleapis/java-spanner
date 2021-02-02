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
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for {@link CreateDatabaseWithVersionRetentionPeriodSample}
 */
@RunWith(JUnit4.class)
public class CreateDatabaseWithVersionRetentionPeriodSampleIT {
  private static final String instanceId = System.getProperty("spanner.test.instance");
  private static final String baseDatabaseId = System.getProperty("spanner.sample.database", "pitrsample");
  private static DatabaseAdminClient databaseAdminClient;

  private String runSample(
      String databaseId,
      String versionRetentionPeriod
  ) {
    final PrintStream stdOut = System.out;
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(bout);
    System.setOut(out);
    CreateDatabaseWithVersionRetentionPeriodSample.createDatabaseWithVersionRetentionPeriod(
        databaseAdminClient,
        instanceId,
        databaseId,
        versionRetentionPeriod
    );
    System.setOut(stdOut);
    return bout.toString();
  }

  @BeforeClass
  public static void setUp() {
    final SpannerOptions options = SpannerOptions
        .newBuilder()
        .setAutoThrottleAdministrativeRequests()
        .build();
    final Spanner spanner = options.getService();
    databaseAdminClient = spanner.getDatabaseAdminClient();
  }

  @Test
  public void createsDatabaseWithVersionRetentionPeriod() {
    final String databaseId = formatForTest(baseDatabaseId);
    final String versionRetentionPeriod = "7d";

    final String out = runSample(databaseId, versionRetentionPeriod);

    assertThat(out).contains("Created database [" + databaseId + "]");
    assertThat(out).contains("Version retention period: " + versionRetentionPeriod);
  }

  static String formatForTest(String name) {
    return (name + "-" + UUID.randomUUID().toString()).substring(0, 30);
  }
}
