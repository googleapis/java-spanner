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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.opencensus.trace.Tracing;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
@Category(TracerTest.class)
public class SpanTest {
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
  private DatabaseClient client;
  private Spanner spannerWithTimeout;
  private DatabaseClient clientWithTimeout;
  private static FailOnOverkillTraceComponentImpl failOnOverkillTraceComponent =
      new FailOnOverkillTraceComponentImpl();

  private static final SimulatedExecutionTime ONE_SECOND =
      SimulatedExecutionTime.ofMinimumAndRandomTime(1000, 0);
  private static final StatusRuntimeException FAILED_PRECONDITION =
      io.grpc.Status.FAILED_PRECONDITION
          .withDescription("Non-retryable test exception.")
          .asRuntimeException();

  @BeforeClass
  public static void startStaticServer() throws Exception {
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

    // Use a little bit reflection to set the test tracer.
    java.lang.reflect.Field field = Tracing.class.getDeclaredField("traceComponent");
    field.setAccessible(true);
    java.lang.reflect.Field modifiersField =
        java.lang.reflect.Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    // Remove the final modifier from the 'traceComponent' field.
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, failOnOverkillTraceComponent);
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
  }

  @Before
  public void setUp() throws Exception {
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build());

    spanner = builder.build().getService();

    client = spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));

    final RetrySettings retrySettings =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(1L))
            .setMaxRetryDelay(Duration.ofMillis(1L))
            .setInitialRpcTimeout(Duration.ofMillis(75L))
            .setMaxRpcTimeout(Duration.ofMillis(75L))
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
            DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));

    failOnOverkillTraceComponent.clearSpans();
  }

  @After
  public void tearDown() {
    spanner.close();
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
  }

  @Test
  public void singleUseNonRetryableErrorOnNext() {
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
      mockSpanner.addException(FAILED_PRECONDITION);
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
    }
  }

  @Test
  public void singleUseExecuteStreamingSqlTimeout() {
    try (ResultSet rs = clientWithTimeout.singleUse().executeQuery(SELECT1)) {
      mockSpanner.setExecuteStreamingSqlExecutionTime(ONE_SECOND);
      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, e.getErrorCode());
    }
  }

  @Test
  public void singleUse() {
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
      while (rs.next()) {
        // Just consume the result set.
      }
    }
    Map<String, Boolean> spans = failOnOverkillTraceComponent.getSpans();
    assertThat(spans).containsEntry("CloudSpanner.ReadOnlyTransaction", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessions", true);
    assertThat(spans).containsEntry("SessionPool.WaitForSession", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessionsRequest", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.ExecuteStreamingQuery", true);
  }

  @Test
  public void multiUse() {
    try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
      try (ResultSet rs = tx.executeQuery(SELECT1)) {
        while (rs.next()) {
          // Just consume the result set.
        }
      }
    }

    Map<String, Boolean> spans = failOnOverkillTraceComponent.getSpans();
    assertThat(spans).containsEntry("CloudSpanner.ReadOnlyTransaction", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessions", true);
    assertThat(spans).containsEntry("SessionPool.WaitForSession", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessionsRequest", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.ExecuteStreamingQuery", true);
  }

  @Test
  public void transactionRunner() {
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
    Map<String, Boolean> spans = failOnOverkillTraceComponent.getSpans();
    assertThat(spans).containsEntry("CloudSpanner.ReadWriteTransaction", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessions", true);
    assertThat(spans).containsEntry("SessionPool.WaitForSession", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessionsRequest", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.Commit", true);
  }

  @Test
  public void transactionRunnerWithError() {
    TransactionRunner runner = client.readWriteTransaction();
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> runner.run(transaction -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT)));
    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());

    Map<String, Boolean> spans = failOnOverkillTraceComponent.getSpans();
    assertThat(spans.size()).isEqualTo(4);
    assertThat(spans).containsEntry("CloudSpanner.ReadWriteTransaction", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessions", true);
    assertThat(spans).containsEntry("SessionPool.WaitForSession", true);
    assertThat(spans).containsEntry("CloudSpannerOperation.BatchCreateSessionsRequest", true);
  }
}
