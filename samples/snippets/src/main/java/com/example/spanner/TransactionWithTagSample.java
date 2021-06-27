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

  // [START spanner_set_transaction_and_request_tags]
  static void taggedTransaction(DatabaseClient databaseClient) {
    // The request below would have transaction_tag set as "app=cart,env=dev".
    databaseClient
        .readWriteTransaction(Options.tag("app=cart,env=dev"))
        .run(
            new TransactionCallable<Void>() {
              @Override
              public Void run(TransactionContext transaction) throws Exception {
                transaction.executeQuery(Statement.of("SELECT 1"),
                    Options.tag("app=cart,env=dev,action=list"));
                transaction.executeUpdate(
                    Statement.of("UPDATE foo SET bar='baz' WHERE TRUE"),
                    Options.tag("app=cart,env=dev,action=update"));
                return null;
              }
            });
  }
  // [END spanner_set_transaction_and_request_tags]

  // [START spanner_query_tags]
  static void queryStats(DatabaseClient databaseClient) {
    // Execute query on Query statistics
    // see: https://cloud.google.com/spanner/docs/introspection/query-statistics, for more details.
    String sql =
        "SELECT t.REQUEST_TAG, t.AVG_LATENCY_SECONDS, t.AVG_CPU_SECONDS "
            + "FROM SPANNER_SYS.QUERY_STATS_TOP_MINUTE as t";
    try (ResultSet resultSet = databaseClient.singleUse().executeQuery(Statement.of(sql))) {
      while (resultSet.next()) {
        System.out.printf(
            "%s %d %d\n", resultSet.getString(0), resultSet.getLong(1), resultSet.getLong(2));
      }
    }
  }
  // [END spanner_query_tags]
}
