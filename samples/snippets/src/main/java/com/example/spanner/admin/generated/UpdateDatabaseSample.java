/*
 * Copyright 2023 Google LLC
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

package com.example.spanner.admin.generated;

// [START spanner_update_database]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.collect.Lists;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.UpdateDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseRequest;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UpdateDatabaseSample {

  static void updateDatabase() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    final String projectId = "my-project";
    final String instanceId = "my-instance";
    final String databaseId = "my-database";

    updateDatabase(projectId, instanceId, databaseId);
  }

  static void updateDatabase(
      String projectId, String instanceId, String databaseId) throws IOException {
    DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create();
    try {
      final Database database =
          Database.newBuilder()
              .setName(DatabaseName.of(projectId, instanceId, databaseId).toString())
              .setEnableDropProtection(true).build();
      final UpdateDatabaseRequest updateDatabaseRequest =
          UpdateDatabaseRequest.newBuilder()
              .setDatabase(database)
              .setUpdateMask(
                  FieldMask.newBuilder().addAllPaths(
                      Lists.newArrayList("enable_drop_protection")).build())
              .build();
      OperationFuture<Database, UpdateDatabaseMetadata> operation =
          databaseAdminClient.updateDatabaseAsync(updateDatabaseRequest);
      System.out.printf("Waiting for update operation for %s to complete...\n", databaseId);
      Database updatedDb = operation.get(5, TimeUnit.MINUTES);
      System.out.printf("Updated database %s.\n", updatedDb.getName());
    } catch (ExecutionException | TimeoutException e) {
      // If the operation failed during execution, expose the cause.
      throw SpannerExceptionFactory.asSpannerException(e.getCause());
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
}
// [END spanner_update_database]
