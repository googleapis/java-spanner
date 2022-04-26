/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

/** Sample Spanner application compiled with Native Image. */
public class NativeImageSpannerSample {

  private static final String TEST_INSTANCE_ID = "test-instance";
  private static final String TEST_DATABASE_ID = "test-database";

  /**
   * Runs the Spanner sample application.
   *
   * <p>This application should be run with the Spanner emulator for testing purposes.
   */
  public static void main(String[] args) throws Exception {
    System.out.println("Running the Spanner Sample.");

    SpannerOptions options = SpannerOptions.newBuilder().build();
    Spanner spanner = options.getService();

    // Setup the Spanner environment.
    InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
    DatabaseAdminClient databaseAdminClient = spanner.getDatabaseAdminClient();

    InstanceOperations.createTestInstance(
        instanceAdminClient, options.getProjectId(), TEST_INSTANCE_ID);
    DatabaseOperations.createDatabase(databaseAdminClient, TEST_INSTANCE_ID, TEST_DATABASE_ID);

    // Insert data
    DatabaseClient dbClient =
        spanner.getDatabaseClient(
            DatabaseId.of(options.getProjectId(), TEST_INSTANCE_ID, TEST_DATABASE_ID));
    DatabaseOperations.insertUsingDml(dbClient);
    DatabaseOperations.insertUsingMutation(dbClient);

    // Run some queries.
    ResultSet resultSet = DatabaseOperations.performRead(dbClient);
    System.out.println("Singers Registered in Spanner:");
    while (resultSet.next()) {
      System.out.println(resultSet.getString("FirstName") + " " + resultSet.getString("LastName"));
    }
    if (DatabaseOperations.databaseExists(
        databaseAdminClient, TEST_INSTANCE_ID, TEST_DATABASE_ID)) {
      DatabaseOperations.deleteDatabase(dbClient);
    }
  }
}
