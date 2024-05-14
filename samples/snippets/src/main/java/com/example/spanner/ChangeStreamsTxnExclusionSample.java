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

import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.spanner.CommitResponse;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.MutationGroup;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;
import com.google.common.collect.ImmutableList;
import com.google.rpc.Code;
import com.google.spanner.v1.BatchWriteResponse;
import java.util.Collections;

/**
 * Sample showing how to set exclude transaction from change streams in different write requests.
 */
public class ChangeStreamsTxnExclusionSample {

  static void setExcludeTxnFromChangeStreams() {
    // TODO(developer): Replace these variables before running the sample.
    final String projectId = "span-cloud-testing";
    final String instanceId = "weideng-test";
    final String databaseId = "my-database";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      final DatabaseClient databaseClient =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      rwTxnExcludedFromChangeStreams(databaseClient);
    }
  }

  // [START spanner_set_exclude_txn_from_change_streams]
  static void rwTxnExcludedFromChangeStreams(DatabaseClient client) {
    // Exclude the transaction from allowed tracking change streams with alloww_txn_exclusion=true.
    // This exclusion will be applied to all the individual operations inside this transaction.
    client
        .readWriteTransaction(Options.excludeTxnFromChangeStreams())
        .run(
            transaction -> {
              transaction.executeUpdate(
                  Statement.of(
                      "INSERT Singers (SingerId, FirstName, LastName)\n"
                          + "VALUES (1341, 'Virginia', 'Watson')"));
              System.out.println("New singer inserted.");

              transaction.executeUpdate(
                  Statement.of("UPDATE Singers SET FirstName = 'Hi' WHERE SingerId = 111"));
              System.out.println("Singer first name updated.");

              return null;
            });
  }

  static void writeExcludedFromChangeStreams(DatabaseClient client) {
    CommitResponse response =
        client.writeWithOptions(
            Collections.singletonList(
                Mutation.newInsertOrUpdateBuilder("Singers")
                    .set("SingerId")
                    .to(4520)
                    .set("FirstName")
                    .to("Lauren")
                    .set("LastName")
                    .to("Lee")
                    .build()),
            Options.excludeTxnFromChangeStreams());
    System.out.println("New singer inserted.");
  }

  static void writeAtLeastOnceExcludedFromChangeStreams(DatabaseClient client) {
    CommitResponse response =
        client.writeAtLeastOnceWithOptions(
            Collections.singletonList(
                Mutation.newInsertOrUpdateBuilder("Singers")
                    .set("SingerId")
                    .to(45201)
                    .set("FirstName")
                    .to("Laura")
                    .set("LastName")
                    .to("Johnson")
                    .build()),
            Options.excludeTxnFromChangeStreams());
    System.out.println("New singer inserted.");
  }

  static void batchWriteAtLeastOnceExcludedFromChangeStreams(DatabaseClient client) {
    ServerStream<BatchWriteResponse> responses =
        client.batchWriteAtLeastOnce(
            ImmutableList.of(
                MutationGroup.of(
                    Mutation.newInsertOrUpdateBuilder("Singers")
                        .set("SingerId")
                        .to(116)
                        .set("FirstName")
                        .to("Scarlet")
                        .set("LastName")
                        .to("Terry")
                        .build())),
            Options.excludeTxnFromChangeStreams());
    for (BatchWriteResponse response : responses) {
      if (response.getStatus().getCode() == Code.OK_VALUE) {
        System.out.printf(
            "Mutation group have been applied with commit timestamp %s",
            response.getIndexesList(), response.getCommitTimestamp());
      } else {
        System.out.printf(
            "Mutation group could not be applied with error code %s and " + "error message %s",
            response.getIndexesList(),
            Code.forNumber(response.getStatus().getCode()),
            response.getStatus().getMessage());
      }
    }
  }

  static void pdmlExcludedFromChangeStreams(DatabaseClient client) {
    client.executePartitionedUpdate(
        Statement.of("DELETE FROM Singers WHERE TRUE"), Options.excludeTxnFromChangeStreams());
    System.out.println("Singers deleted.");
  }

  static void txnManagerExcludedFromChangeStreams(DatabaseClient client) {
    try (TransactionManager manager =
        client.transactionManager(Options.excludeTxnFromChangeStreams())) {
      TransactionContext transaction = manager.begin();
      transaction.buffer(
          Mutation.newInsertOrUpdateBuilder("Singers")
              .set("SingerId")
              .to(888)
              .set("FirstName")
              .to("Johnson")
              .set("LastName")
              .to("Doug")
              .build());
      manager.commit();
      System.out.println("New singer inserted.");
    }
  }

  // [END spanner_set_exclude_txn_from_change_streams]

}
