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

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.auth.mtls.DefaultMtlsProviderFactory;
import com.google.cloud.NoCredentials;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
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
  private Spanner spanner;
  private DatabaseClientImpl client;

  private static boolean originalSkipMtls;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    originalSkipMtls = DefaultMtlsProviderFactory.SKIP_MTLS.get();
    DefaultMtlsProviderFactory.SKIP_MTLS.set(true);
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));

    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            // We need to use a real executor for timeouts to occur.
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    // Force a shutdown as there are still requests stuck in the server.
    server.shutdownNow();
    server.awaitTermination();
    DefaultMtlsProviderFactory.SKIP_MTLS.set(originalSkipMtls);
  }

  @Before
  public void setUp() throws Exception {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build();
    ExecutorFactory<ScheduledExecutorService> executorFactory =
        ((GrpcTransportOptions) options.getTransportOptions()).getExecutorFactory();
    ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) executorFactory.get();
    options =
        options.toBuilder()
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setMinSessions(executor.getCorePoolSize())
                    .setMaxSessions(executor.getCorePoolSize() * 3)
                    .build())
            .build();
    executorFactory.release(executor);

    spanner = options.getService();
    client =
        (DatabaseClientImpl)
            spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
    // Wait until the session pool has initialized.
    while (client.pool.getNumberOfSessionsInPool()
        < spanner.getOptions().getSessionPoolOptions().getMinSessions()) {
      Thread.sleep(1L);
    }
  }

  @After
  public void tearDown() {
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    // This test case force-closes the Spanner instance as it would otherwise wait
    // forever on the BatchCreateSessions requests that are 'stuck'.
    try {
      ((SpannerImpl) spanner).close(10L, TimeUnit.MILLISECONDS);
    } catch (SpannerException e) {
      // ignore any errors during close as they are expected.
    }
  }

  @Test
  public void test() throws Exception {
    // Simulate very heavy load on the server by effectively stopping session creation.
    mockSpanner.setBatchCreateSessionsExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(Integer.MAX_VALUE, 0));
    // Create an executor that can handle twice as many requests as the minimum number of sessions
    // in the pool and then start that many read requests. That will initiate the creation of
    // additional sessions.
    ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(
            spanner.getOptions().getSessionPoolOptions().getMinSessions() * 2);
    // Also temporarily freeze the server to ensure that the requests that can be served will
    // continue to be in-flight and keep the sessions in the pool checked out.
    mockSpanner.freeze();
    for (int i = 0; i < spanner.getOptions().getSessionPoolOptions().getMinSessions() * 2; i++) {
      executor.submit(new ReadRunnable());
    }
    // Now schedule as many write requests as there can be sessions in the pool.
    for (int i = 0; i < spanner.getOptions().getSessionPoolOptions().getMaxSessions(); i++) {
      executor.submit(new WriteRunnable());
    }
    // Now unfreeze the server and verify that all requests can be served using the sessions that
    // were already present in the pool.
    mockSpanner.unfreeze();
    executor.shutdown();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
  }

  private final class ReadRunnable implements Runnable {
    @Override
    public void run() {
      try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
        while (rs.next()) {}
      }
    }
  }

  private final class WriteRunnable implements Runnable {
    @Override
    public void run() {
      TransactionRunner runner = client.readWriteTransaction();
      runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
    }
  }
}
