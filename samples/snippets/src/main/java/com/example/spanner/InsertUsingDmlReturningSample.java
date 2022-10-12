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

// [START spanner_insert_dml_returning]

import com.google.api.gax.paging.Page;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;

public class InsertUsingDmlReturningSample {

  static void insertUsingDmlReturning() {
    // TODO(developer): Replace these variables before running the sample.
    final String projectId = "my-project";
    final String instanceId = "my-instance";
    final String databaseId = "my-database";
    insertUsingDmlReturning(projectId, instanceId, databaseId);
  }

  static void insertUsingDmlReturning(String projectId, String instanceId, String databaseId) {
    try (Spanner spanner = SpannerOptions
        .newBuilder()
        .setProjectId(projectId)
        .build()
        .getService()) {
      final DatabaseClient dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      // Insert records into the SINGERS table and returns the
      // generated column FullName of the inserted records using
      // ‘THEN RETURN FullName’.
      // It is also possible to return all columns of all the
      // inserted records by using ‘THEN RETURN *’.
      dbClient
          .readWriteTransaction()
          .run(transaction -> {
            String sql =
                "INSERT INTO Singers (SingerId, FirstName, LastName) VALUES "
                    + "(12, 'Melissa', 'Garcia'), "
                    + "(13, 'Russell', 'Morales'), "
                    + "(14, 'Jacqueline', 'Long'), "
                    + "(15, 'Dylan', 'Shaw') THEN RETURN FullName";
            try (ResultSet resultSet = transaction.executeQuery(Statement.of(sql))) {
              while (resultSet.next()) {
                System.out.println(resultSet.getString(0));
              }
              System.out.printf(
                  "Inserted row(s) count: %d\n", resultSet.getStats().getRowCountExact());
            }
            return null;
          });
    }
  }
}
// [END spanner_insert_dml_returning]
