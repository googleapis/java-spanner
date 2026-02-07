/*
 * Copyright 2026 Google LLC
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

import static org.junit.Assert.assertEquals;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpanFEBypassTest extends AbstractMockServerTest {
  private static final Statement SELECT_RANDOM_STATEMENT = Statement.of("select * from random");
  private static final int RANDOM_RESULT_ROW_COUNT = 20;
  private static Spanner spanner;
  private static DatabaseClient client;

  @BeforeClass
  public static void enableLocationApiAndSetupClient() {
    SpannerOptions.useEnvironment(
        new SpannerOptions.SpannerEnvironment() {
          @Override
          public boolean isEnableLocationApi() {
            return true;
          }
        });
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("my-project")
            .setHost(String.format("http://localhost:%d", getPort()))
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of("my-project", "my-instance", "my-database"));

    RandomResultSetGenerator generator = new RandomResultSetGenerator(RANDOM_RESULT_ROW_COUNT);
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT_RANDOM_STATEMENT, generator.generate()));
  }

  @AfterClass
  public static void cleanup() {
    SpannerOptions.useDefaultEnvironment();
    if (spanner != null) {
      spanner.close();
    }
  }

  @Test
  public void testSingleQuery() {
    int rowCount = 0;
    try (ResultSet resultSet = client.singleUse().executeQuery(SELECT_RANDOM_STATEMENT)) {
      while (resultSet.next()) {
        rowCount++;
      }
    }
    assertEquals(RANDOM_RESULT_ROW_COUNT, rowCount);
  }

  @Test
  public void testParallelQueries() throws Exception {
    int numThreads = 10;
    ListeningExecutorService executor =
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads));
    List<ListenableFuture<Void>> results = new ArrayList<>();
    for (int i = 0; i < numThreads; i++) {
      results.add(
          executor.submit(
              () -> {
                try (ResultSet resultSet =
                    client.singleUse().executeQuery(SELECT_RANDOM_STATEMENT)) {
                  while (resultSet.next()) {
                    // Randomly stop consuming results somewhere halfway the results (sometimes).
                    if (ThreadLocalRandom.current().nextInt(RANDOM_RESULT_ROW_COUNT * 2) == 5) {
                      break;
                    }
                  }
                }
                return null;
              }));
    }
    executor.shutdown();
    Futures.allAsList(results).get();
  }

  @Test
  public void testSingleReadWriteTransaction() {
    client.readWriteTransaction().run(transaction -> transaction.executeUpdate(INSERT_STATEMENT));
  }

  @Test
  public void testParallelReadWriteTransactions() throws Exception {
    int numThreads = 10;
    ListeningExecutorService executor =
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads));
    List<ListenableFuture<Void>> results = new ArrayList<>();
    for (int i = 0; i < numThreads; i++) {
      results.add(
          executor.submit(
              () -> {
                client
                    .readWriteTransaction()
                    .run(transaction -> transaction.executeUpdate(INSERT_STATEMENT));
                return null;
              }));
    }
    executor.shutdown();
    Futures.allAsList(results).get();
  }
}
