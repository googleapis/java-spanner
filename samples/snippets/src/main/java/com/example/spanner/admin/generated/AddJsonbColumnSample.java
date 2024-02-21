/*
 * Copyright 2022 Google Inc.
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

package com.example.spanner.admin.generated;

// [START spanner_postgresql_jsonb_add_column]
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.DatabaseName;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

class AddJsonbColumnSample {

  static void addJsonbColumn() throws InterruptedException, ExecutionException, IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";

    addJsonbColumn(projectId, instanceId, databaseId);
  }

  static void addJsonbColumn(String projectId, String instanceId, String databaseId)
      throws InterruptedException, ExecutionException, IOException {
    final DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create();

    // JSONB datatype is only supported with PostgreSQL-dialect databases.
    // Wait for the operation to finish.
    // This will throw an ExecutionException if the operation fails.
    databaseAdminClient.updateDatabaseDdlAsync(
        DatabaseName.of(projectId, instanceId, databaseId),
        ImmutableList.of("ALTER TABLE Venues ADD COLUMN VenueDetails JSONB")).get();
    System.out.printf("Successfully added column `VenueDetails`%n");
  }
}
// [END spanner_postgresql_jsonb_add_column]
