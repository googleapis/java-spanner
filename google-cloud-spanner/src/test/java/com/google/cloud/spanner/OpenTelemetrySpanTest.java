/*
 * Copyright 2023 Google LLC
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
import static org.junit.Assert.assertThrows;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OpenTelemetrySpanTest {

  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private static LocalChannelProvider channelProvider;
  private static MockSpannerServiceImpl mockSpanner;
  private Spanner spanner;
  private DatabaseClient client;
  private static Server server;
  private static InMemorySpanExporter spanExporter;

  private static FailOnOverkillTraceComponentImpl failOnOverkillTraceComponent =
      new FailOnOverkillTraceComponentImpl();

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
  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;
  private static final Statement INVALID_UPDATE_STATEMENT =
      Statement.of("UPDATE NON_EXISTENT_TABLE SET BAR=1 WHERE BAZ=2");

  private List<String> expectedBatchCreateSessionsRequestEvents =
      ImmutableList.of("Requesting 25 sessions", "Request for 25 sessions returned 25 sessions");

  private int expectedBatchCreateSessionsRequestEventsCount = 2;

  private List<String> expectedBatchCreateSessionsEvents = ImmutableList.of("Creating 25 sessions");

  private int expectedBatchCreateSessionsEventsCount = 1;

  private List<String> expectedExecuteStreamingQueryEvents =
      ImmutableList.of("Starting/Resuming stream");

  private int expectedExecuteStreamingQueryEventsCount = 1;

  private List<String> expectedReadOnlyTransactionSingleUseEvents =
      ImmutableList.of(
          "Acquiring session",
          "No session available",
          "Creating sessions",
          "Waiting for a session to come available",
          "Using Session");

  private int expectedReadOnlyTransactionSingleUseEventsCount = 5;

  private List<String> expectedReadOnlyTransactionMultiUseEvents =
      ImmutableList.of(
          "Acquiring session",
          "No session available",
          "Creating sessions",
          "Waiting for a session to come available",
          "Using Session",
          "Creating Transaction",
          "Transaction Creation Done");

  private int expectedReadOnlyTransactionMultiUseEventsCount = 7;

  private List<String> expectedReadWriteTransactionErrorEvents =
      ImmutableList.of(
          "Acquiring session",
          "No session available",
          "Creating sessions",
          "Waiting for a session to come available",
          "Using Session",
          "Starting Transaction Attempt",
          "Transaction Attempt Failed in user operation",
          "exception");

  private int expectedReadWriteTransactionErrorEventsCount = 8;
  private List<String> expectedReadWriteTransactionEvents =
      ImmutableList.of(
          "Acquiring session",
          "No session available",
          "Creating sessions",
          "Waiting for a session to come available",
          "Using Session",
          "Starting Transaction Attempt",
          "Starting Commit",
          "Commit Done",
          "Transaction Attempt Succeeded");

  private int expectedReadWriteTransactionCount = 9;
  private List<String> expectedReadWriteTransactionErrorWithBeginTransactionEvents =
      ImmutableList.of(
          "Acquiring session",
          "No session available",
          "Creating sessions",
          "Waiting for a session to come available",
          "Using Session",
          "Starting Transaction Attempt",
          "Transaction Attempt Aborted in user operation. Retrying",
          "Creating Transaction",
          "Transaction Creation Done",
          "Starting Commit",
          "Commit Done",
          "Transaction Attempt Succeeded");

  private int expectedReadWriteTransactionErrorWithBeginTransactionEventsCount = 13;
  private List<String> expectedReadOnlyTransactionSpans =
      ImmutableList.of(
          "CloudSpannerOperation.BatchCreateSessionsRequest",
          "CloudSpannerOperation.ExecuteStreamingQuery",
          "CloudSpannerOperation.BatchCreateSessions",
          "CloudSpanner.ReadOnlyTransaction",
          "SessionPool.WaitForSession");

  private List<String> expectedReadWriteTransactionWithCommitSpans =
      ImmutableList.of(
          "CloudSpannerOperation.BatchCreateSessionsRequest",
          "CloudSpannerOperation.Commit",
          "CloudSpannerOperation.BatchCreateSessions",
          "CloudSpanner.ReadWriteTransaction",
          "SessionPool.WaitForSession");

  private List<String> expectedReadWriteTransactionSpans =
      ImmutableList.of(
          "CloudSpannerOperation.BatchCreateSessionsRequest",
          "CloudSpannerOperation.BatchCreateSessions",
          "CloudSpanner.ReadWriteTransaction",
          "SessionPool.WaitForSession");

  private List<String> expectedReadWriteTransactionWithCommitAndBeginTransactionSpans =
      ImmutableList.of(
          "CloudSpannerOperation.BeginTransaction",
          "CloudSpannerOperation.BatchCreateSessionsRequest",
          "CloudSpannerOperation.Commit",
          "CloudSpannerOperation.BatchCreateSessions",
          "CloudSpanner.ReadWriteTransaction",
          "SessionPool.WaitForSession");

  @BeforeClass
  public static void startStaticServer() throws Exception {

    // Incorporating OpenCensus tracer to ensure that OpenTraces traces are utilized if enabled,
    // regardless of the presence of OpenCensus tracer.
    java.lang.reflect.Field field = Tracing.class.getDeclaredField("traceComponent");
    field.setAccessible(true);
    java.lang.reflect.Field modifiersField = null;
    try {
      modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
    } catch (NoSuchFieldException e) {
      // Halt the test and ignore it.
      Assume.assumeTrue(
          "Skipping test as reflection is not allowed on reflection class in this JDK build",
          false);
    }
    modifiersField.setAccessible(true);
    // Remove the final modifier from the 'traceComponent' field.
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, failOnOverkillTraceComponent);

    // OpenTelemetry Configuration

    SpannerOptions.enableOpenTelemetryTraces();
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));
    String uniqueName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(uniqueName).addService(mockSpanner).build().start();

    channelProvider = LocalChannelProvider.create(uniqueName);
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    if (server != null) {
      server.shutdown();
      server.awaitTermination();
    }
  }

  @Before
  public void setUp() throws Exception {
    spanExporter = InMemorySpanExporter.create();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    GlobalOpenTelemetry.resetForTest();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder()
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .setTracerProvider(tracerProvider)
            .build();

    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setOpenTelemetry(openTelemetry)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setMinSessions(0).build());

    spanner = builder.build().getService();

    client = spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
  }

  @After
  public void tearDown() {
    spanner.close();
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    spanExporter.reset();
  }

  @Test
  public void singleUse() {
    try (ResultSet rs = client.singleUse().executeQuery(SELECT1)) {
      while (rs.next()) {
        // Just consume the result set.
      }
    }

    // OpenCensus spans should be 0 as OpenTelemetry is enabled.
    assertEquals(failOnOverkillTraceComponent.getSpans().size(), 0);

    List<String> actualSpanItems = new ArrayList<>();
    spanExporter
        .getFinishedSpanItems()
        .forEach(
            spanItem -> {
              actualSpanItems.add(spanItem.getName());
              switch (spanItem.getName()) {
                case "CloudSpannerOperation.BatchCreateSessionsRequest":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsRequestEvents,
                      expectedBatchCreateSessionsRequestEventsCount);
                  break;
                case "CloudSpannerOperation.BatchCreateSessions":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsEvents,
                      expectedBatchCreateSessionsEventsCount);
                  break;
                case "CloudSpannerOperation.ExecuteStreamingQuery":
                  verifyRequestEvents(
                      spanItem,
                      expectedExecuteStreamingQueryEvents,
                      expectedExecuteStreamingQueryEventsCount);
                  break;
                case "CloudSpanner.ReadOnlyTransaction":
                  verifyRequestEvents(
                      spanItem,
                      expectedReadOnlyTransactionSingleUseEvents,
                      expectedReadOnlyTransactionSingleUseEventsCount);
                  break;
                case "SessionPool.WaitForSession":
                  assertEquals(0, spanItem.getEvents().size());
                  break;
                default:
                  assert false;
              }
            });

    verifySpans(actualSpanItems, expectedReadOnlyTransactionSpans);
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

    List<String> actualSpanItems = new ArrayList<>();
    spanExporter
        .getFinishedSpanItems()
        .forEach(
            spanItem -> {
              actualSpanItems.add(spanItem.getName());
              switch (spanItem.getName()) {
                case "CloudSpannerOperation.BatchCreateSessionsRequest":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsRequestEvents,
                      expectedBatchCreateSessionsRequestEventsCount);
                  break;
                case "CloudSpannerOperation.BatchCreateSessions":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsEvents,
                      expectedBatchCreateSessionsEventsCount);
                  break;
                case "CloudSpannerOperation.ExecuteStreamingQuery":
                  verifyRequestEvents(
                      spanItem,
                      expectedExecuteStreamingQueryEvents,
                      expectedExecuteStreamingQueryEventsCount);
                  break;
                case "SessionPool.WaitForSession":
                  assertEquals(0, spanItem.getEvents().size());
                  break;
                case "CloudSpanner.ReadOnlyTransaction":
                  verifyRequestEvents(
                      spanItem,
                      expectedReadOnlyTransactionMultiUseEvents,
                      expectedReadOnlyTransactionMultiUseEventsCount);
                  break;
                default:
                  assert false;
              }
            });

    verifySpans(actualSpanItems, expectedReadOnlyTransactionSpans);
  }

  @Test
  public void transactionRunner() {
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));

    List<String> actualSpanItems = new ArrayList<>();
    spanExporter
        .getFinishedSpanItems()
        .forEach(
            spanItem -> {
              actualSpanItems.add(spanItem.getName());
              switch (spanItem.getName()) {
                case "CloudSpannerOperation.BatchCreateSessionsRequest":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsRequestEvents,
                      expectedBatchCreateSessionsRequestEventsCount);
                  break;
                case "CloudSpannerOperation.BatchCreateSessions":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsEvents,
                      expectedBatchCreateSessionsEventsCount);
                  break;
                case "SessionPool.WaitForSession":
                case "CloudSpannerOperation.Commit":
                  assertEquals(0, spanItem.getEvents().size());
                  break;
                case "CloudSpanner.ReadWriteTransaction":
                  verifyRequestEvents(
                      spanItem,
                      expectedReadWriteTransactionEvents,
                      expectedReadWriteTransactionCount);
                  break;
                default:
                  assert false;
              }
            });

    verifySpans(actualSpanItems, expectedReadWriteTransactionWithCommitSpans);
  }

  @Test
  public void transactionRunnerWithError() {
    TransactionRunner runner = client.readWriteTransaction();
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> runner.run(transaction -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT)));
    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());

    List<String> actualSpanItems = new ArrayList<>();
    spanExporter
        .getFinishedSpanItems()
        .forEach(
            spanItem -> {
              actualSpanItems.add(spanItem.getName());
              switch (spanItem.getName()) {
                case "CloudSpannerOperation.BatchCreateSessionsRequest":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsRequestEvents,
                      expectedBatchCreateSessionsRequestEventsCount);
                  break;
                case "CloudSpannerOperation.BatchCreateSessions":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsEvents,
                      expectedBatchCreateSessionsEventsCount);
                  break;
                case "SessionPool.WaitForSession":
                  assertEquals(0, spanItem.getEvents().size());
                  break;
                case "CloudSpanner.ReadWriteTransaction":
                  verifyRequestEvents(
                      spanItem,
                      expectedReadWriteTransactionErrorEvents,
                      expectedReadWriteTransactionErrorEventsCount);
                  break;
                default:
                  assert false;
              }
            });

    verifySpans(actualSpanItems, expectedReadWriteTransactionSpans);
  }

  @Test
  public void transactionRunnerWithFailedAndBeginTransaction() {
    Long updateCount =
        client
            .readWriteTransaction()
            .run(
                transaction -> {
                  // This update statement carries the BeginTransaction, but fails. This will
                  // cause the entire transaction to be retried with an explicit
                  // BeginTransaction RPC to ensure all statements in the transaction are
                  // actually executed against the same transaction.
                  SpannerException e =
                      assertThrows(
                          SpannerException.class,
                          () -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT));
                  assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
                  return transaction.executeUpdate(UPDATE_STATEMENT);
                });

    List<String> actualSpanItems = new ArrayList<>();
    spanExporter
        .getFinishedSpanItems()
        .forEach(
            spanItem -> {
              actualSpanItems.add(spanItem.getName());
              switch (spanItem.getName()) {
                case "CloudSpannerOperation.BatchCreateSessionsRequest":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsRequestEvents,
                      expectedBatchCreateSessionsRequestEventsCount);
                  break;
                case "CloudSpannerOperation.BatchCreateSessions":
                  verifyRequestEvents(
                      spanItem,
                      expectedBatchCreateSessionsEvents,
                      expectedBatchCreateSessionsEventsCount);
                  break;
                case "SessionPool.WaitForSession":
                case "CloudSpannerOperation.Commit":
                case "CloudSpannerOperation.BeginTransaction":
                  assertEquals(0, spanItem.getEvents().size());
                  break;
                case "CloudSpanner.ReadWriteTransaction":
                  verifyRequestEvents(
                      spanItem,
                      expectedReadWriteTransactionErrorWithBeginTransactionEvents,
                      expectedReadWriteTransactionErrorWithBeginTransactionEventsCount);
                  break;
                default:
                  assert false;
              }
            });

    verifySpans(actualSpanItems, expectedReadWriteTransactionWithCommitAndBeginTransactionSpans);
  }

  private void verifyRequestEvents(SpanData spanItem, List<String> expectedEvents, int eventCount) {
    List<String> eventNames =
        spanItem.getEvents().stream().map(EventData::getName).collect(Collectors.toList());
    assertEquals(eventCount, spanItem.getEvents().size());
    assertEquals(
        eventNames.stream().distinct().sorted().collect(Collectors.toList()),
        expectedEvents.stream().sorted().collect(Collectors.toList()));
  }

  private static void verifySpans(List<String> actualSpanItems, List<String> expectedSpansItems) {
    assertEquals(
        actualSpanItems.stream().distinct().sorted().collect(Collectors.toList()),
        expectedSpansItems.stream().sorted().collect(Collectors.toList()));
  }
}
