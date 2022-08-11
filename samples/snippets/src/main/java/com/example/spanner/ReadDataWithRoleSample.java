/*
 * Copyright 2022 Google Inc.
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

// [START spanner_read_data_with_database_role]

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class ReadDataWithRoleSample {
  static void readDataWithRole() throws InterruptedException, ExecutionException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";
    String role = "my-role";
    readDataWithRole(projectId, instanceId, databaseId, role);
  }

  static void readDataWithRole(String projectId, String instanceId, String databaseId, String role)
      throws InterruptedException, ExecutionException {
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId(projectId)
            .setDatabaseRole(role)
            .build()
            .getService()) {
      final DatabaseClient dbClient =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      try (ResultSet resultSet =
          dbClient
              .singleUse()
              .read(
                  "Albums",
                  KeySet.all(), // Read all rows in a table.
                  Arrays.asList("SingerId", "AlbumId", "AlbumTitle"))) {
        while (resultSet.next()) {
          System.out.printf(
              "%d %d %s\n", resultSet.getLong(0), resultSet.getLong(1), resultSet.getString(2));
        }
      }
    }
  }
}
// [END spanner_read_data_with_database_role]
