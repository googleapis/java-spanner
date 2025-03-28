/*
 * Copyright 2025 Google LLC
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

package com.example.spanner;

//[START spanner_database_add_split_points]

import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ListValue;
import com.google.spanner.admin.database.v1.AddSplitPointsRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.SplitPoints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DatabaseAddSplitPointsSample {

    /***
     * Assume DDL for the underlying database:
     * <pre>{@code
     * CREATE TABLE Singers (
     * SingerId INT64 NOT NULL,
     * FirstName STRING(1024),
     * LastName STRING(1024),
     *  SingerInfo BYTES(MAX),
     * ) PRIMARY KEY(SingerId);
     * 
     * 
     * CREATE INDEX SingersByFirstLastName ON Singers(FirstName, LastName);
     * }</pre>
     */

  static void addSplitPoints() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";
    addSplitPoints(projectId,instanceId, databaseId);
  }

  static void addSplitPoints(String projectId, String instanceId,
      String databaseId) throws IOException {
    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService();
        DatabaseAdminClient databaseAdminClient = spanner.createDatabaseAdminClient())  {
        final String database =
          "projects/"
              + projectId
              + "/instances/"
              + instanceId
              + "/databases/"
              + databaseId;
            AddSplitPointsRequest splitPointRequest = new AddSplitPointsRequest();
            List<com.google.spanner.admin.database.v1.SplitPoints> splitPoints = new ArrayList<>();
            com.google.spanner.admin.database.v1.SplitPoints splitPointForTable = new SplitPoints();
            splitPointForTable.setTable("Singers");
            com.google.spanner.admin.database.v1.SplitPoints.Key tableKey = new com.google.spanner.admin.database.v1.SplitPoints.Key();
            tableKey.setKeyParts = new ArrayList<>();
            splitPointForTable.getKeyParts().add(new ListValue("42"));
            splitPointForTable.setKeys(tableKey);

            // index key without table key part
            com.google.spanner.admin.database.v1.SplitPoints splitPointForIndex = new SplitPoints();
            splitPointForIndex.setIndex("SingersByFirstLastName");
            com.google.spanner.admin.database.v1.SplitPoints.Key indexKey = new com.google.spanner.admin.database.v1.SplitPoints.Key();
            indexKey.setKeyParts = new ArrayList<>();
            splitPointForIndex.getKeyParts().add(new ListValue("John","Doe"));
            splitPointForIndex.setKeys(indexKey);

            // index key with table key part
            com.google.spanner.admin.database.v1.SplitPoints splitPointForIndexWitTableKey = new SplitPoints();
            splitPointForIndexWitTableKey.setIndex("SingersByFirstLastName");
            com.google.spanner.admin.database.v1.SplitPoints.Key tableKey2 = new com.google.spanner.admin.database.v1.SplitPoints.Key();
            tableKey2.setKeyParts = new ArrayList<>();
            splitPointForIndexWitTableKey.getKeyParts().add(new ListValue("38"));
            splitPointForIndexWitTableKey.setKeys(tableKey2);
            com.google.spanner.admin.database.v1.SplitPoints.Key indexKey2 = new com.google.spanner.admin.database.v1.SplitPoints.Key();
            indexKey2.setKeyParts = new ArrayList<>();
            splitPointForIndexWitTableKey.getKeyParts().add(new ListValue("Jane","Doe"));
            splitPointForIndexWitTableKey.setKeys(indexKey2);

            splitPoints.add(splitPointForTable);
            splitPoints.add(splitPointForIndex);
            splitPoints.add(splitPointForIndexWitTableKey);
            databaseAdminClient.addSplitPoints(database,splitPoints);
      
    } catch (Exception e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    }
  }
}
//[END spanner_create_database_with_default_leader]