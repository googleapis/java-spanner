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

package com.google.cloud.spanner;

import com.google.cloud.spanner.connection.Connection;
import com.google.cloud.spanner.connection.ConnectionOptions;

// This program tests the prototype of autocommit v2.
//
// Prerequisite:
// - The mentioned Spanner database includes a pre-created table foo(key: INT64, value: INT64).
// - Records with key in [1..4] should have been pre-inserted.
//
// The output (that proves nextTxnId is indeed use in the next write transaction):
//
// -- 1st update should call createTxnAsync --
// In ensureTxnAsync(): call createTxnAsync() to get a new txnId.
// In commit(), store nextTxnId received from CommitResponse.
//
// -- 2nd update should use the nextTxnId returned from committing 1st --
// SessionPool.getSession() polled from writeSessions.
// In ensureTxnAsync(), use the stored nextTxnId.
// In commit(), store nextTxnId received from CommitResponse.
//
// -- A query should use a session without nextTxnId --
// SessionPool.getSession() polled from sessions (w/o nextRwTxnId).
//
// -- 3rd update should use the nextTxnId returned from committing 2nd--
// SessionPool.getSession() polled from writeSessions.
// In ensureTxnAsync(), use the stored nextTxnId.
// In commit(), store nextTxnId received from CommitResponse.
//
public class Donghui {
  // Pointer to a pre-created staging database with a table foo(key: INT64, value: INT64).
  private static final String HOST = "staging-wrenchworks.sandbox.googleapis.com";
  private static final String PROJECT = "span-cloud-testing";
  private static final String INSTANCE = "donghuiz-instance"; // test-instance or donghuiz-instance
  private static final String DATABASE = "donghuiz-db";

  // SQL related.
  private static final String UPDATE = "UPDATE foo SET value=value+1 WHERE key=@key";
  private static final String SELECT = "SELECT value FROM foo WHERE key=@key";
  private static final String PARAM_KEY = "key";

  private static void check(boolean condition, String message) throws SpannerException {
    if (!condition) {
      throw new RuntimeException("[CHECK FAILURE:] " + message);
    }
  }

  // Returns the value of a record given an existing key, or throws NOT_FOUND.
  private static long select(Connection connection, long key) throws SpannerException {
    check(!connection.isTransactionStarted(), "before selectOne, connection isTransactionStarted");
    ResultSet resultSet =
        connection.executeQuery(Statement.newBuilder(SELECT).bind(PARAM_KEY).to(key).build());
    check(resultSet.next(), "The record with key " + Long.toString(key) + " does not exist.");
    final long value = resultSet.getLong(0);
    resultSet.close();
    if (!connection.isAutocommit()) {
      connection.commit();
    }
    check(!connection.isTransactionStarted(), "after selectOne, connection isTransactionStarted");
    return value;
  }

  // Increments the value of a record with a given key.
  private static void updateOne(Connection connection, long key) {
    long row_count =
        connection.executeUpdate(Statement.newBuilder(UPDATE).bind(PARAM_KEY).to(key).build());
    check(row_count == 1, "Wrong updated row_count.");
    if (!connection.isAutocommit()) {
      connection.commit();
    }
  }

  public static Connection newConnection() {
    final ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(
                String.format(
                    "cloudspanner://%s/projects/%s/instances/%s/databases/%s",
                    HOST, PROJECT, INSTANCE, DATABASE))
            .build();
    return options.getConnection();
  }

  public static void main(String[] args) throws SpannerException {
    Connection connection = newConnection();
    System.out.println("-- 1st update should call createTxnAsync --");
    updateOne(connection, 1);

    System.out.println("\n-- 2nd update should use the nextTxnId returned from committing 1st --");
    updateOne(connection, 2);

    System.out.println("\n-- A query should use a session without nextTxnId --");
    select(connection, 3);

    System.out.println("\n-- 3rd update should use the nextTxnId returned from committing 2nd--");
    updateOne(connection, 4);
  }
}
