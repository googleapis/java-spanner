/*
 * Copyright 2020 Google Inc.
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

package main.java.com.example.spanner;

// [START spanner_query_with_json_parameter]
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;

class QueryWithJsonParameterSample {

  static void queryWithJsonParameter() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      queryWithJsonParameter(client);
    }
  }

  static void queryWithJsonParameter(DatabaseClient client) {
    String exampleJson = "{rating: 9}";
    Statement statement =
        Statement.newBuilder(
                "SELECT VenueId, VenueDetails\n"
                    + "FROM Venues\n"
                    + "WHERE JSON_VALUE(VenueDetails, '$.rating') = "
                    + "JSON_VALUE(@details, '$.rating')")
            .bind("details")
            .to(Value.json(exampleJson))
            .build();
    try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        System.out.printf(
            "VenueId: %s, VenueDetails: %s%n",
            resultSet.getLong("VenueId"), resultSet.getString("VenueDetails"));
      }
    }
  }
}
// [END spanner_query_with_json_parameter]
