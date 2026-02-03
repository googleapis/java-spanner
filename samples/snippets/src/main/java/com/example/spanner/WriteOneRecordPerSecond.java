/*
 * Copyright 2024 Google LLC
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

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This sample demonstrates how to write a record to Spanner once per second.
 */
public class WriteOneRecordPerSecond {

  private static final Logger logger = Logger.getLogger(WriteOneRecordPerSecond.class.getName());

  public static void main(String[] args) throws InterruptedException {
    if (args.length != 3) {
      System.err.println("Usage: WriteOneRecordPerSecond <project_id> <instance_id> <database_id>");
      System.exit(1);
    }
    String projectId = args[0];
    String instanceId = args[1];
    String databaseId = args[2];

    SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
    try (Spanner spanner = options.getService()) {
      DatabaseClient dbClient =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

      // Start a counter for the singer ID.
      // In a real application, you would likely use a more robust way to generate unique IDs.
      long singerId = System.currentTimeMillis();

      // Loop indefinitely, writing one record per second.
      while (true) {
        writeSinger(dbClient, singerId);
        singerId++;
        TimeUnit.SECONDS.sleep(1);
      }
    }
  }

  static void writeSinger(DatabaseClient dbClient, long singerId) {
    String firstName = "FirstName" + singerId;
    String lastName = "LastName" + singerId;
    Mutation mutation =
        Mutation.newInsertOrUpdateBuilder("Singers")
            .set("SingerId")
            .to(singerId)
            .set("FirstName")
            .to(firstName)
            .set("LastName")
            .to(lastName)
            .build();
    try {
      dbClient.write(Collections.singletonList(mutation));
      logger.log(Level.INFO, "Wrote record for singer " + singerId);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to write record for singer " + singerId, e);
    }
  }
}
