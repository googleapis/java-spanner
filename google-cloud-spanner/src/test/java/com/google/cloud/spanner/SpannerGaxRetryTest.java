/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class SpannerGaxRetryTest {
  private static final Statement SELECT1AND2 =
      Statement.of("SELECT 1 AS COL1 UNION ALL SELECT 2 AS COL1");
  private static final ResultSetMetadata SELECT1AND2_METADATA =
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
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(SELECT1AND2_METADATA)
          .build();
  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;
  private static final SimulatedExecutionTime ONE_SECOND =
      SimulatedExecutionTime.ofMinimumAndRandomTime(1000, 0);
  private static final StatusRuntimeException UNAVAILABLE =
      io.grpc.Status.UNAVAILABLE.withDescription("Retryable test exception.").asRuntimeException();
  private static final StatusRuntimeException FAILED_PRECONDITION =
      io.grpc.Status.FAILED_PRECONDITION
          .withDescription("Non-retryable test exception.")
          .asRuntimeException();
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private Spanner spanner;
  private DatabaseClient client;
  private Spanner spannerWithTimeout;
  private DatabaseClient clientWithTimeout;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.query(SELECT1AND2, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));

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
    server.shutdown();
    server.awaitTermination();
  }

  @Before
  public void setUp() throws Exception {
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    // Make sure the session pool is empty by default.
    builder.setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build());
    // Create one client with default timeout values and one with short timeout values specifically
    // for the test cases that expect a DEADLINE_EXCEEDED.
    spanner = builder.build().getService();
    client = spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));

    final RetrySettings retrySettings =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(1L))
            .setMaxRetryDelay(Duration.ofMillis(1L))
            .setInitialRpcTimeout(Duration.ofMillis(175L))
            .setMaxRpcTimeout(Duration.ofMillis(175L))
            .setMaxAttempts(3)
            .setTotalTimeout(Duration.ofMillis(200L))
            .build();
    RetrySettings commitRetrySettings =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(1L))
            .setMaxRetryDelay(Duration.ofMillis(1L))
            .setInitialRpcTimeout(Duration.ofMillis(5000L))
            .setMaxRpcTimeout(Duration.ofMillis(10000L))
            .setMaxAttempts(1)
            .setTotalTimeout(Duration.ofMillis(20000L))
            .build();
    builder
        .getSpannerStubSettingsBuilder()
        .applyToAllUnaryMethods(
            input -> {
              input.setRetrySettings(retrySettings);
              return null;
            });
    builder
        .getSpannerStubSettingsBuilder()
        .executeStreamingSqlSettings()
        .setRetrySettings(retrySettings);
    builder.getSpannerStubSettingsBuilder().commitSettings().setRetrySettings(commitRetrySettings);
    builder
        .getSpannerStubSettingsBuilder()
        .executeStreamingSqlSettings()
        .setRetrySettings(retrySettings);
    builder.getSpannerStubSettingsBuilder().streamingReadSettings().setRetrySettings(retrySettings);
    spannerWithTimeout = builder.build().getService();
    clientWithTimeout =
        spannerWithTimeout.getDatabaseClient(
            DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
  }

  @After
  public void tearDown() {
    spannerWithTimeout.close();
    spanner.close();
  }

  private void warmUpSessionPool(DatabaseClient client) {
    for (int i = 0; i < 10; i++) {
      int retryCount = 0;
      while (true) {
        try {
          TransactionRunner runner = client.readWriteTransaction();
          long updateCount = runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
          assertThat(updateCount, is(equalTo(UPDATE_COUNT)));
          break;
        } catch (SpannerException e) {
          // On slow systems there is a chance of DEADLINE_EXCEEDED errors.
          // These should be retried.
          retryCount++;
          if (e.getErrorCode() != ErrorCode.DEADLINE_EXCEEDED || retryCount > 10) {
            throw e;
          }
        }
      }
    }
  }

  @Test
  public void singleUseTimeout() {
    mockSpanner.setBatchCreateSessionsExecutionTime(ONE_SECOND);
    try (ResultSet rs = clientWithTimeout.singleUse().executeQuery(SELECT1AND2)) {
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
    }
  }

  @Test
  public void singleUseUnavailable() {
    mockSpanner.addException(UNAVAILABLE);
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1AND2)) {
      while (rs.next()) {}
    }
  }

  @Test
  public void singleUseNonRetryableError() {
    mockSpanner.addException(FAILED_PRECONDITION);
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1AND2)) {
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
    }
  }

  @Test
  public void singleUseNonRetryableErrorOnNext() {
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1AND2)) {
      mockSpanner.addException(FAILED_PRECONDITION);
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
    }
  }

  @Test
  public void singleUseInternal() {
    mockSpanner.addException(new IllegalArgumentException());
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1AND2)) {
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
    }
  }

  @Test
  public void singleUseReadOnlyTransactionTimeout() {
    mockSpanner.setBatchCreateSessionsExecutionTime(ONE_SECOND);
    try (ResultSet rs =
        clientWithTimeout.singleUseReadOnlyTransaction().executeQuery(SELECT1AND2)) {
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
    }
  }

  @Test
  public void singleUseReadOnlyTransactionUnavailable() {
    mockSpanner.addException(UNAVAILABLE);
    try (ResultSet rs = client.singleUseReadOnlyTransaction().executeQuery(SELECT1AND2)) {
      while (rs.next()) {}
    }
  }

  @Test
  public void singleUseExecuteStreamingSqlTimeout() {
    try (ResultSet rs = clientWithTimeout.singleUse().executeQuery(SELECT1AND2)) {
      mockSpanner.setExecuteStreamingSqlExecutionTime(ONE_SECOND);
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
    }
  }

  @Test
  public void singleUseExecuteStreamingSqlUnavailable() {
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1AND2)) {
      mockSpanner.addException(UNAVAILABLE);
      while (rs.next()) {}
    }
  }

  @Test
  public void readWriteTransactionTimeout() {
    mockSpanner.setBeginTransactionExecutionTime(ONE_SECOND);
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> clientWithTimeout.readWriteTransaction().run(transaction -> null));
    assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
  }

  @Test
  public void readWriteTransactionUnavailable() {
    warmUpSessionPool(client);
    mockSpanner.addException(UNAVAILABLE);
    TransactionRunner runner = client.readWriteTransaction();
    long updateCount = runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
    assertThat(updateCount, is(equalTo(UPDATE_COUNT)));
  }

  @Test
  public void readWriteTransactionStatementAborted() {
    TransactionRunner runner = client.readWriteTransaction();
    final AtomicInteger attempts = new AtomicInteger();
    long updateCount =
        runner.run(
            transaction -> {
              if (attempts.getAndIncrement() == 0) {
                mockSpanner.abortNextStatement();
              }
              return transaction.executeUpdate(UPDATE_STATEMENT);
            });
    assertThat(updateCount, is(equalTo(UPDATE_COUNT)));
    assertThat(attempts.get(), is(equalTo(2)));
  }

  @Test
  public void readWriteTransactionCommitAborted() {
    TransactionRunner runner = client.readWriteTransaction();
    final AtomicInteger attempts = new AtomicInteger();
    long updateCount =
        runner.run(
            transaction -> {
              long res = transaction.executeUpdate(UPDATE_STATEMENT);
              if (attempts.getAndIncrement() == 0) {
                mockSpanner.abortTransaction(transaction);
              }
              return res;
            });
    assertThat(updateCount, is(equalTo(UPDATE_COUNT)));
    assertThat(attempts.get(), is(equalTo(2)));
  }

  @Test(expected = Exception.class)
  public void readWriteTransactionCheckedException() {
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction -> {
          transaction.executeUpdate(UPDATE_STATEMENT);
          throw new Exception("test");
        });
  }

  @Test(expected = SpannerException.class)
  public void readWriteTransactionUncheckedException() {
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(
        transaction -> {
          transaction.executeUpdate(UPDATE_STATEMENT);
          throw SpannerExceptionFactory.newSpannerException(ErrorCode.INVALID_ARGUMENT, "test");
        });
  }

  @Test
  public void transactionManagerTimeout() {
    mockSpanner.setExecuteSqlExecutionTime(ONE_SECOND);
    try (TransactionManager txManager = clientWithTimeout.transactionManager()) {
      TransactionContext tx = txManager.begin();
      SpannerException e =
          assertThrows(SpannerException.class, () -> tx.executeUpdate(UPDATE_STATEMENT));
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerUnavailable() {
    warmUpSessionPool(client);
    mockSpanner.addException(UNAVAILABLE);
    try (TransactionManager txManager = client.transactionManager()) {
      TransactionContext tx = txManager.begin();
      while (true) {
        try {
          assertThat(tx.executeUpdate(UPDATE_STATEMENT), is(equalTo(UPDATE_COUNT)));
          txManager.commit();
          break;
        } catch (AbortedException e) {
          tx = txManager.resetForRetry();
        }
      }
    }
  }
}
