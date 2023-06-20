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

package com.example.spanner;

// [START spanner_batch_write_atleast_once]

import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.collect.ImmutableList;
import com.google.rpc.Code;
import com.google.spanner.v1.BatchWriteResponse;
import java.util.List;

public class BatchWriteAtleastOnceSample {

  private static final List<Mutation> MUTATIONS =
      ImmutableList.of(
          Mutation.newInsertOrUpdateBuilder("Singers")
              .set("SingerId")
              .to(16)
              .set("FirstName")
              .to("Scarlet")
              .set("LastName")
              .to("Terry")
              .build(),
          Mutation.newInsertOrUpdateBuilder("Singers")
              .set("SingerId")
              .to(17)
              .set("FirstName")
              .to("Luciano")
              .build());

  static void batchWriteAtleastOnce() {
    // TODO(developer): Replace these variables before running the sample.
    final String projectId = "my-project";
    final String instanceId = "my-instance";
    final String databaseId = "my-database";
    batchWriteAtleastOnce(projectId, instanceId, databaseId);
  }

  static void batchWriteAtleastOnce(String projectId, String instanceId, String databaseId) {
    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseId dbId = DatabaseId.of(projectId, instanceId, databaseId);
      final DatabaseClient dbClient = spanner.getDatabaseClient(dbId);

      // Creates and issues a BatchWrite RPC request that will apply the mutations non-atomically
      // and respond back with a stream of BatchWriteResponse.
      ServerStream<BatchWriteResponse> responses =
          dbClient.batchWriteAtleastOnceWithOptions(MUTATIONS, Options.tag("batch-write-tag"));

      // Iterates through the results in the stream response and prints the mutation indexes,
      // commit timestamp and status.
      for (BatchWriteResponse response : responses) {
        if (response.getStatus().getCode() == Code.OK_VALUE) {
          System.out.printf(
              "Mutation indexes %s have been applied with commit timestamp %s",
              response.getIndexesList(), response.getCommitTimestamp());
        } else {
          System.out.printf(
              "Mutation indexes %s could not be applied with error code %s",
              response.getIndexesList(), Code.forNumber(response.getStatus().getCode()));
        }
      }
    }
  }
}

// [END spanner_batch_write_atleast_once]
