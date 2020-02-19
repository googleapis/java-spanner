/*
 * Copyright 2020 Google LLC
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleClientImpl implements SimpleClient {

  private final DatabaseClient client;
  private static final Logger logger = Logger.getLogger(SimpleClientImpl.class.getName());

  SimpleClientImpl(DatabaseClient client) {
    this.client = client;
  }

  @Override
  public ResultSet executeSqlQuery(String query) {
    return executeSqlQuery(query, TimestampBound.strong());
  }

  @Override
  public ResultSet executeSnapshotSqlQuery(String query) {
    return executeSqlQuery(query, TimestampBound.ofExactStaleness(5L, TimeUnit.SECONDS));
  }

  @Override
  public ResultSet executeSqlQuery(String query, TimestampBound timestampBound) {
    return executeSqlQuery(query, null, timestampBound);
  }

  @Override
  public ResultSet executeSqlQuery(
      String query, Map<String, Value> args, TimestampBound timestampBound) {
    try (ReadOnlyTransaction tx = client.singleUseReadOnlyTransaction(timestampBound)) {
      Statement statement;
      if (args != null && !args.isEmpty()) {
        Statement.Builder builder = Statement.newBuilder(query);
        for (Map.Entry<String, Value> entry : args.entrySet()) {
          builder.bind(entry.getKey()).to(entry.getValue());
        }
        statement = builder.build();
      } else {
        statement = Statement.of(query);
      }
      return tx.executeQuery(statement);
    }
  }

  @Override
  public ResultSet executeSqlQuery(String query, Map<String, Value> args) {
    return executeSqlQuery(query, args, TimestampBound.strong());
  }

  @Override
  public ResultSet executeSnapshotSqlQuery(String query, Map<String, Value> args) {
    return executeSqlQuery(query, args, TimestampBound.ofExactStaleness(5L, TimeUnit.SECONDS));
  }

  @Override
  public long executeSqlWrite(String sql, Map<String, Value> args) {
    Statement.Builder builder = Statement.newBuilder(sql);
    if (args != null && !args.isEmpty()) {
      for (Map.Entry<String, Value> entry : args.entrySet()) {
        builder.bind(entry.getKey()).to(entry.getValue());
      }
    }
    return client.executePartitionedUpdate(builder.build());
  }

  @Override
  public boolean runTransaction(final List<Statement> statements) {
    try {
      boolean status =
          client
              .readWriteTransaction()
              .run(
                  new TransactionRunner.TransactionCallable<Boolean>() {
                    @Override
                    public Boolean run(TransactionContext transaction) {
                      try {
                        transaction.batchUpdate(statements);
                        return true;
                      } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to complete transaction", e);
                      }
                      return false;
                    }
                  });
    } catch (Exception e) {
      throw SpannerExceptionFactory.newSpannerException(e);
    }
    return true;
  }

  @Override
  public void runTransaction(final Statement readStatement, final OnReadHandler handler) {
    client
        .readWriteTransaction()
        .run(
            new TransactionRunner.TransactionCallable<Boolean>() {
              @Override
              public Boolean run(TransactionContext transaction) throws Exception {
                try {
                  ResultSet resultSet = transaction.executeQuery(readStatement);
                  handler.handle(resultSet, transaction);
                  return true;
                } catch (Exception e) {
                  logger.log(Level.WARNING, "Failed to complete transaction", e);
                }
                return false;
              }
            });
  }
}
