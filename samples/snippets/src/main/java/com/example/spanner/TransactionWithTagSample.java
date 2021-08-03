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

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;

/**
 * Sample showing how to add and query tags to Cloud Spanner operations.
 */
public class TransactionWithTagSample {

  // [START spanner_set_transaction_tag]
  static void setTransactionTag(DatabaseClient databaseClient) {
    // Sets the transaction tag to "app=concert,env=dev".
    // This transaction tag will be applied to all the individual operations inside this
    // transaction.
    databaseClient
        .readWriteTransaction(Options.tag("app=concert,env=dev"))
        .run(
            new TransactionCallable<Void>() {
              @Override
              public Void run(TransactionContext transaction) throws Exception {
                Statement selectStatement = Statement.newBuilder(
                    "SELECT VenueId, VenueName, Capacity FROM Venues "
                        + "WHERE OutdoorVenue = @outdoorVenue")
                    .bind("outdoorVenue")
                    .to(false)
                    .build();

                // Sets the request tag to "app=concert,env=dev,action=select".
                // This request tag will only be set on this request.
                ResultSet resultSet = transaction.executeQuery(
                    selectStatement, Options.tag("app=concert,env=dev,action=select"));

                while (resultSet.next()) {
                  long newCapacity = resultSet.getLong(2) / 4;
                  Statement updateStatement = Statement.newBuilder(
                      "UPDATE Venues SET Capacity = @capacity WHERE VenueId = @venueId")
                      .bind("capacity")
                      .to(newCapacity)
                      .bind("venueId")
                      .to(resultSet.getLong(0))
                      .build();

                  // Sets the request tag to "app=concert,env=dev,action=update".
                  // This request tag will only be set on this request.
                  transaction.executeUpdate(
                      updateStatement, Options.tag("app=concert,env=dev,action=update"));
                  System.out.printf(
                      "Capacity of %s updated to %d\n", resultSet.getString(1), newCapacity);
                }

                return null;
              }
            });
  }
  // [END spanner_set_transaction_tag]

  // [START spanner_query_tags]
  static void setRequestTag(DatabaseClient databaseClient) {
    // Sets the request tag to "app=concert,env=dev,action=select".
    // This request tag will only be set on this request.
    try (ResultSet resultSet = dbClient.singleUse().executeQuery(
        Statement.of("SELECT SingerId, AlbumId, AlbumTitle FROM Albums"),
        Options.tag("app=concert,env=dev,action=select"))) {
      while (resultSet.next()) {
        System.out.printf(
            "%d %d %s\n", resultSet.getString(0), resultSet.getLong(1), resultSet.getLong(2));
      }
    }
  }
  // [END spanner_query_tags]
}
