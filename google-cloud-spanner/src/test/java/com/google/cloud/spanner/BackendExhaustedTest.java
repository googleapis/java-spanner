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

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that a degraded backend that can no longer create any new sessions will not cause an
 * application that already has a healthy session pool to stop functioning.
 */
@RunWith(JUnit4.class)
public class BackendExhaustedTest {
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private MockSpannerServiceImpl mockSpanner;
  private Server server;
  private LocalChannelProvider channelProvider;
  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final Statement INVALID_UPDATE_STATEMENT =
      Statement.of("UPDATE NON_EXISTENT_TABLE SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;
  private static final Statement SELECT1 = Statement.of("SELECT 1 AS COL1");
  private static final ResultSetMetadata SELECT1_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("COL1")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.INT64)
                                  .build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                  .build())
          .setMetadata(SELECT1_METADATA)
          .build();
  private Spanner blockingSpanner;
  private DatabaseClientImpl blockingClient;
  private Spanner failingSpanner;
  private DatabaseClientImpl failingClient;

  @Before
  public void startStaticServer() throws IOException, InterruptedException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));

    String uniqueName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(uniqueName).addService(mockSpanner).build().start();
    channelProvider = LocalChannelProvider.create(uniqueName);
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build();
    ExecutorFactory<ScheduledExecutorService> executorFactory =
        ((GrpcTransportOptions) options.getTransportOptions()).getExecutorFactory();
    ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) executorFactory.get();
    SessionPoolOptions.Builder poolOptionsBuilder =
        SessionPoolOptions.newBuilder()
            .setMinSessions(executor.getCorePoolSize())
            .setMaxSessions(executor.getCorePoolSize() * 3)
            .setWriteSessionsFraction(0.0f);
    options = options.toBuilder().setSessionPoolOption(poolOptionsBuilder.build()).build();
    executorFactory.release(executor);

    blockingSpanner = options.getService();
    failingSpanner =
        options
            .toBuilder()
            .setSessionPoolOption(
                poolOptionsBuilder
                    .setFailIfPoolExhausted()
                    .setMinSessions(100)
                    .setMaxSessions(100)
                    .build())
            .build()
            .getService();
    blockingClient =
        (DatabaseClientImpl)
            blockingSpanner.getDatabaseClient(
                DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    failingClient =
        (DatabaseClientImpl)
            failingSpanner.getDatabaseClient(
                DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));

    // Wait until the session pool has initialized.
    while (blockingClient.pool.getNumberOfSessionsInPool()
        < blockingSpanner.getOptions().getSessionPoolOptions().getMinSessions()) {
      Thread.sleep(1L);
    }
    while (failingClient.pool.getNumberOfSessionsInPool()
        < failingSpanner.getOptions().getSessionPoolOptions().getMinSessions()) {
      Thread.sleep(1L);
    }
  }

  @After
  public void stopServer() throws InterruptedException {
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    // This test case force-closes the Spanner instance as it would otherwise wait
    // forever on the BatchCreateSessions requests that are 'stuck'.
    try {
      ((SpannerImpl) blockingSpanner).close(100L, TimeUnit.MILLISECONDS);
    } catch (SpannerException e) {
      // ignore any errors during close as they are expected.
    }
    try {
      ((SpannerImpl) failingSpanner).close(100L, TimeUnit.MILLISECONDS);
    } catch (SpannerException e) {
      // ignore any errors during close as they are expected.
    }

    // Force a shutdown as there are still requests stuck in the server.
    server.shutdownNow();
  }

  @Test
  public void testBatchCreateSessionsBlocked() throws Exception {
    // Simulate very heavy load on the server by effectively stopping session creation.
    mockSpanner.setBatchCreateSessionsExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(Integer.MAX_VALUE, 0));
    // Create an executor that can handle twice as many requests as the minimum number of sessions
    // in the pool and then start that many read requests. That will initiate the creation of
    // additional sessions.
    ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(
            blockingSpanner.getOptions().getSessionPoolOptions().getMinSessions() * 2);
    // Also temporarily freeze the server to ensure that the requests that can be served will
    // continue to be in-flight and keep the sessions in the pool checked out.
    mockSpanner.freeze();
    for (int i = 0;
        i < blockingSpanner.getOptions().getSessionPoolOptions().getMinSessions() * 2;
        i++) {
      executor.submit(new ReadRunnable(blockingClient));
    }
    // Now schedule as many write requests as there can be sessions in the pool.
    for (int i = 0;
        i < blockingSpanner.getOptions().getSessionPoolOptions().getMaxSessions();
        i++) {
      executor.submit(new WriteRunnable(blockingClient));
    }
    // Now unfreeze the server and verify that all requests can be served using the sessions that
    // were already present in the pool.
    mockSpanner.unfreeze();
    executor.shutdown();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testBeginTransactionBlocked() throws InterruptedException {
    // Simulate very heavy load on the server by effectively stopping the creation of sessions and
    // transactions.
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(Integer.MAX_VALUE, 0));
    // Create an executor that can handle as many requests as there can be sessions in the pool.
    // This executor will request read/write sessions that cannot be served, as BeginTransaction is
    // blocked.
    ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(
            failingSpanner.getOptions().getSessionPoolOptions().getMaxSessions());
    // Now schedule as many write requests as there can be sessions in the pool.
    for (int i = 0; i < failingSpanner.getOptions().getSessionPoolOptions().getMaxSessions(); i++) {
      executor.submit(new WriteRunnable(failingClient));
    }
    while (failingClient.pool.getNumberOfSessionsBeingPrepared()
            + failingClient.pool.getNumberOfSessionsInProcessPrepared()
        < failingSpanner.getOptions().getSessionPoolOptions().getMinSessions()) {
      Thread.sleep(1L);
    }
    // Now try to execute a read. This will fail as the session pool has been exhausted.
    try (ResultSet rs = failingClient.singleUse().executeQuery(SELECT1)) {
      while (rs.next()) {}
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_EXHAUSTED);
      // There will be no sessions in use, as they are still blocked on BeginTransaction.
      assertThat(failingClient.pool.getNumberOfSessionsInUse()).isEqualTo(0);
    }
    executor.shutdownNow();
  }

  private final class ReadRunnable implements Runnable {
    private final DatabaseClient client;

    ReadRunnable(DatabaseClient client) {
      this.client = client;
    }

    @Override
    public void run() {
      try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
        while (rs.next()) {}
      }
    }
  }

  private final class WriteRunnable implements Runnable {
    private final DatabaseClient client;

    WriteRunnable(DatabaseClient client) {
      this.client = client;
    }

    @Override
    public void run() {
      TransactionRunner runner = client.readWriteTransaction();
      runner.run(
          new TransactionCallable<Long>() {
            @Override
            public Long run(TransactionContext transaction) {
              return transaction.executeUpdate(UPDATE_STATEMENT);
            }
          });
    }
  }
}
