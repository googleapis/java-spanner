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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFuture;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.DatabaseClient.AsyncWork;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AsyncRunnerTest {
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";

  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final Statement INVALID_UPDATE_STATEMENT =
      Statement.of("UPDATE NON_EXISTENT_TABLE SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private static ExecutorService executor;

  private Spanner spanner;
  private Spanner spannerWithEmptySessionPool;
  private DatabaseClient client;
  private DatabaseClient clientWithEmptySessionPool;

  @BeforeClass
  public static void setup() throws Exception {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D);
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));
    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
    executor = Executors.newSingleThreadExecutor();
  }

  @AfterClass
  public static void teardown() throws Exception {
    executor.shutdown();
    server.shutdown();
    server.awaitTermination();
  }

  @Before
  public void before() {
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setFailOnSessionLeak().build())
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    spannerWithEmptySessionPool =
        spanner
            .getOptions()
            .toBuilder()
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setFailOnSessionLeak().setMinSessions(0).build())
            .build()
            .getService();
    clientWithEmptySessionPool =
        spannerWithEmptySessionPool.getDatabaseClient(
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
  }

  @After
  public void after() {
    spanner.close();
    spannerWithEmptySessionPool.close();
    mockSpanner.removeAllExecutionTimes();
  }

  @Test
  public void asyncRunnerUpdate() throws Exception {
    ApiFuture<Long> updateCount =
        client.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                return txn.executeUpdateAsync(UPDATE_STATEMENT);
              }
            },
            executor);
    assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
  }

  @Test
  public void asyncRunnerIsNonBlocking() throws Exception {
    mockSpanner.freeze();
    final SettableApiFuture<Void> finished = SettableApiFuture.create();
    ApiFuture<Void> res =
        clientWithEmptySessionPool.runAsync(
            new AsyncWork<Void>() {
              @Override
              public ApiFuture<Void> doWorkAsync(TransactionContext txn) {
                finished.set(null);
                return finished;
              }
            },
            executor);
    mockSpanner.unfreeze();
    assertThat(res.get()).isNull();
  }

  @Test
  public void asyncRunnerInvalidUpdate() throws Exception {
    ApiFuture<Long> updateCount =
        client.runAsync(
            new AsyncWork<Long>() {
              @Override
              public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                return txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
              }
            },
            executor);
    try {
      updateCount.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(se.getMessage()).contains("invalid statement");
    }
  }

  @Test
  public void asyncRunnerUpdateAborted() throws Exception {
    try {
      // Temporarily set the result of the update to 2 rows.
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT + 1L));
      final AtomicInteger attempt = new AtomicInteger();
      ApiFuture<Long> updateCount =
          client.runAsync(
              new AsyncWork<Long>() {
                @Override
                public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                  if (attempt.incrementAndGet() == 1) {
                    mockSpanner.abortTransaction(txn);
                  } else {
                    // Set the result of the update statement back to 1 row.
                    mockSpanner.putStatementResult(
                        StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                  }
                  return txn.executeUpdateAsync(UPDATE_STATEMENT);
                }
              },
              executor);
      assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
      assertThat(attempt.get()).isEqualTo(2);
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    }
  }

  @Test
  public void asyncRunnerCommitAborted() throws Exception {
    try {
      // Temporarily set the result of the update to 2 rows.
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT + 1L));
      final AtomicInteger attempt = new AtomicInteger();
      ApiFuture<Long> updateCount =
          client.runAsync(
              new AsyncWork<Long>() {
                @Override
                public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                  ApiFuture<Long> updateCount = txn.executeUpdateAsync(UPDATE_STATEMENT);
                  if (attempt.incrementAndGet() == 1) {
                    mockSpanner.abortTransaction(txn);
                  } else {
                    // Set the result of the update statement back to 1 row.
                    mockSpanner.putStatementResult(
                        StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                  }
                  return updateCount;
                }
              },
              executor);
      assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
      assertThat(attempt.get()).isEqualTo(2);
    } finally {
      mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    }
  }

  @Test
  public void asyncRunnerUpdateAbortedWithoutGettingResult() throws Exception {
    final SettableApiFuture<Void> finished = SettableApiFuture.create();
    final AtomicInteger attempt = new AtomicInteger();
    ApiFuture<Void> result =
        client.runAsync(
            new AsyncWork<Void>() {
              @Override
              public ApiFuture<Void> doWorkAsync(TransactionContext txn) {
                if (attempt.incrementAndGet() == 1) {
                  mockSpanner.abortTransaction(txn);
                }
                txn.executeUpdateAsync(UPDATE_STATEMENT);
                finished.set(null);
                return finished;
              }
            },
            executor);
    assertThat(result.get()).isNull();
    assertThat(attempt.get()).isEqualTo(2);
  }
}
