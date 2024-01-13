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

// [START spanner_postgresql_drop_sequence]

import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.DatabaseName;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PgDropSequenceSample {

  static void pgDropSequence() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    final String projectId = "my-project";
    final String instanceId = "my-instance";
    final String databaseId = "my-database";
    pgDropSequence(projectId, instanceId, databaseId);
  }

  static void pgDropSequence(String projectId, String instanceId, String databaseId)
      throws IOException {
    DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create();

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      databaseAdminClient
          .updateDatabaseDdlAsync(
              DatabaseName.of(projectId, instanceId, databaseId),
              ImmutableList.of(
                  "ALTER TABLE Customers ALTER COLUMN CustomerId DROP DEFAULT",
                  "DROP SEQUENCE Seq"))
          .get(5, TimeUnit.MINUTES);
      System.out.println(
          "Altered Customers table to drop DEFAULT from "
              + "CustomerId column and dropped the Seq sequence");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw SpannerExceptionFactory.asSpannerException(e.getCause());
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    } catch (TimeoutException e) {
      // If the operation timed out propagate the timeout
      throw SpannerExceptionFactory.propagateTimeout(e);
    }
  }
}
// [END spanner_postgresql_drop_sequence]
