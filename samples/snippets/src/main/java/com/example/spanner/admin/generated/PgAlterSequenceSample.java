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

// [START spanner_postgresql_alter_sequence]

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.DatabaseName;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PgAlterSequenceSample {

  static void pgAlterSequence() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    final String projectId = "my-project";
    final String instanceId = "my-instance";
    final String databaseId = "my-database";
    pgAlterSequence(projectId, instanceId, databaseId);
  }

  static void pgAlterSequence(String projectId, String instanceId, String databaseId)
      throws IOException {
    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      final DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create();
      databaseAdminClient
          .updateDatabaseDdlAsync(
              DatabaseName.of(projectId, instanceId, databaseId),
              ImmutableList.of("ALTER SEQUENCE Seq SKIP RANGE 1000 5000000"))
          .get(5, TimeUnit.MINUTES);
      System.out.println(
          "Altered Seq sequence to skip an inclusive range between 1000 and 5000000");
      final DatabaseClient dbClient =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      Long insertCount =
          dbClient
              .readWriteTransaction()
              .run(
                  transaction -> {
                    try (ResultSet rs =
                        transaction.executeQuery(
                            Statement.of(
                                "INSERT INTO Customers (CustomerName) VALUES "
                                    + "('Lea'), ('Catalina'), ('Smith') RETURNING CustomerId"))) {
                      while (rs.next()) {
                        System.out.printf(
                            "Inserted customer record with CustomerId: %d\n", rs.getLong(0));
                      }
                      return Objects.requireNonNull(rs.getStats()).getRowCountExact();
                    }
                  });
      System.out.printf("Number of customer records inserted is: %d\n", insertCount);
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
// [END spanner_postgresql_alter_sequence]
