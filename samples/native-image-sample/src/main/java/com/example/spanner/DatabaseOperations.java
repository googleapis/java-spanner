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

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;

/** Helper methods to manage Spanner Databases. */
public class DatabaseOperations {

  private static final List<String> DDL_STATEMENTS =
      ImmutableList.of(
          "CREATE TABLE Singers (SingerId INT64 NOT NULL, FirstName "
              + "STRING(1024), LastName STRING(1024)) PRIMARY KEY (SingerId)");

  static void createDatabase(
      DatabaseAdminClient databaseAdminClient, String instanceId, String databaseId) {

    if (databaseExists(databaseAdminClient, instanceId, databaseId)) {
      databaseAdminClient.dropDatabase(instanceId, databaseId);
    }
    databaseAdminClient.createDatabase(instanceId, databaseId, DDL_STATEMENTS);
  }

  static boolean databaseExists(
      DatabaseAdminClient databaseAdminClient, String instanceId, String databaseId) {

    for (Database database : databaseAdminClient.listDatabases(instanceId).iterateAll()) {
      if (databaseId.equals(database.getId().getDatabase())) {
        return true;
      }
    }
    return false;
  }

  static void insertUsingDml(DatabaseClient dbClient) {
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              String sql =
                  "INSERT INTO Singers (SingerId, FirstName, LastName) "
                      + " VALUES (10, 'Virginia', 'Watson')";
              transaction.executeUpdate(Statement.of(sql));
              return null;
            });
  }

  static void insertUsingMutation(DatabaseClient dbClient) {
    Mutation mutation =
        Mutation.newInsertBuilder("Singers")
            .set("SingerId")
            .to(12)
            .set("FirstName")
            .to("Bob")
            .set("LastName")
            .to("Loblaw")
            .build();
    dbClient.write(Collections.singletonList(mutation));
  }

  static ResultSet performRead(DatabaseClient dbClient) {
    return dbClient.singleUse().executeQuery(Statement.of("SELECT * FROM Singers"));
  }

  static void deleteDatabase(DatabaseClient dbClient) {
    dbClient.write(Collections.singletonList(Mutation.delete("Singers", KeySet.all())));
    System.out.println("Records deleted.");
  }
}
