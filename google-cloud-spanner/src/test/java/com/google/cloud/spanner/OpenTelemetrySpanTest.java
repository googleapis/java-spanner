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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import io.opencensus.trace.Tracing;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@Category(TracerTest.class)
@RunWith(JUnit4.class)
public class OpenTelemetrySpanTest {

  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";
  private static LocalChannelProvider channelProvider;
  private static MockSpannerServiceImpl mockSpanner;
  private Spanner spanner;
  private Spanner spannerWithApiTracing;
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

  private List<String> expectedCreateMultiplexedSessionsRequestEvents =
      ImmutableList.of("Request for 1 multiplexed session returned 1 session");

  private int expectedCreateMultiplexedSessionsRequestEventsCount = 1;
  private List<String> expectedBatchCreateSessionsRequestEvents =
      ImmutableList.of("Requesting 2 sessions", "Request for 2 sessions returned 2 sessions");

  private int expectedBatchCreateSessionsRequestEventsCount = 2;

  private List<String> expectedBatchCreateSessionsEvents = ImmutableList.of("Creating 2 sessions");

  private int expectedBatchCreateSessionsEventsCount = 1;

  private List<String> expectedExecuteStreamingQueryEvents =
      ImmutableList.of("Starting/Resuming stream");

  private int expectedExecuteStreamingQueryEventsCount = 1;

  private List<String> expectedReadWriteTransactionErrorEvents =
      ImmutableList.of(
          "Acquiring session",
          "Acquired session",
          "Using Session",
          "Starting Transaction Attempt",
          "Transaction Attempt Failed in user operation",
          "exception");

  private int expectedReadWriteTransactionErrorEventsCount = 6;
  private List<String> expectedReadWriteTransactionEvents =
      ImmutableList.of(
          "Acquiring session",
          "Acquired session",
          "Using Session",
          "Starting Transaction Attempt",
          "Starting Commit",
          "Commit Done",
          "Transaction Attempt Succeeded");

  private int expectedReadWriteTransactionCount = 7;
  private List<String> expectedReadWriteTransactionErrorWithBeginTransactionEvents =
      ImmutableList.of(
          "Acquiring session",
          "Acquired session",
          "Using Session",
          "Starting Transaction Attempt",
          "Transaction Attempt Aborted in user operation. Retrying",
          "Creating Transaction",
          "Transaction Creation Done",
          "Starting Commit",
          "Commit Done",
          "Transaction Attempt Succeeded");

  private int expectedReadWriteTransactionErrorWithBeginTransactionEventsCount = 11;

  @BeforeClass
  public static void setupOpenTelemetry() {
    SpannerOptions.resetActiveTracingFramework();
    SpannerOptions.enableOpenTelemetryTraces();
  }

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
    failOnOverkillTraceComponent.clearSpans();
    failOnOverkillTraceComponent.clearAnnotations();
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
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setMinSessions(2)
                    .setWaitForMinSessions(Duration.ofSeconds(10))
                    .build());

    spanner = builder.build().getService();
    spannerWithApiTracing = builder.setEnableApiTracing(true).build().getService();
  }

  DatabaseClient getClient() {
    return spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
  }

  DatabaseClient getClientWithApiTracing() {
    return spannerWithApiTracing.getDatabaseClient(
        DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
  }

  @After
  public void tearDown() {
    spanner.close();
    spannerWithApiTracing.close();
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    spanExporter.reset();
  }

  @Test
  public void singleUse() {
    List<String> expectedReadOnlyTransactionSingleUseEvents =
        getExpectedReadOnlyTransactionSingleUseEvents();
    List<String> expectedReadOnlyTransactionSpans =
        isMultiplexedSessionsEnabled()
            ? ImmutableList.of(
                "CloudSpannerOperation.CreateMultiplexedSession",
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteStreamingQuery",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadOnlyTransaction")
            : ImmutableList.of(
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteStreamingQuery",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadOnlyTransaction");
    int expectedReadOnlyTransactionSingleUseEventsCount =
        expectedReadOnlyTransactionSingleUseEvents.size();

    DatabaseClient client = getClient();
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
                case "CloudSpannerOperation.CreateMultiplexedSession":
                  verifyRequestEvents(
                      spanItem,
                      expectedCreateMultiplexedSessionsRequestEvents,
                      expectedCreateMultiplexedSessionsRequestEventsCount);
                  break;
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
                default:
                  assert false;
              }
            });

    verifySpans(actualSpanItems, expectedReadOnlyTransactionSpans);
  }

  private List<String> getExpectedReadOnlyTransactionSingleUseEvents() {
    List<String> expectedReadOnlyTransactionSingleUseEvents;
    if (isMultiplexedSessionsEnabled()) {
      expectedReadOnlyTransactionSingleUseEvents = ImmutableList.of();
    } else {
      expectedReadOnlyTransactionSingleUseEvents =
          ImmutableList.of("Acquiring session", "Acquired session", "Using Session");
    }
    return expectedReadOnlyTransactionSingleUseEvents;
  }

  @Test
  public void multiUse() {
    List<String> expectedReadOnlyTransactionSpans =
        isMultiplexedSessionsEnabled()
            ? ImmutableList.of(
                "CloudSpannerOperation.CreateMultiplexedSession",
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteStreamingQuery",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadOnlyTransaction")
            : ImmutableList.of(
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteStreamingQuery",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadOnlyTransaction");
    List<String> expectedReadOnlyTransactionMultiUseEvents;
    if (isMultiplexedSessionsEnabled()) {
      expectedReadOnlyTransactionMultiUseEvents =
          ImmutableList.of("Creating Transaction", "Transaction Creation Done");
    } else {
      expectedReadOnlyTransactionMultiUseEvents =
          ImmutableList.of(
              "Acquiring session",
              "Acquired session",
              "Using Session",
              "Creating Transaction",
              "Transaction Creation Done");
    }
    int expectedReadOnlyTransactionMultiUseEventsCount =
        expectedReadOnlyTransactionMultiUseEvents.size();

    DatabaseClient client = getClient();
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
                case "CloudSpannerOperation.CreateMultiplexedSession":
                  verifyRequestEvents(
                      spanItem,
                      expectedCreateMultiplexedSessionsRequestEvents,
                      expectedCreateMultiplexedSessionsRequestEventsCount);
                  break;
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
    List<String> expectedReadWriteTransactionWithCommitSpans =
        isMultiplexedSessionsEnabled()
            ? ImmutableList.of(
                "CloudSpannerOperation.CreateMultiplexedSession",
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteUpdate",
                "CloudSpannerOperation.Commit",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadWriteTransaction")
            : ImmutableList.of(
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteUpdate",
                "CloudSpannerOperation.Commit",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadWriteTransaction");
    DatabaseClient client = getClient();
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
    // Wait until the list of spans contains "CloudSpannerOperation.BatchCreateSessions", as this is
    // an async operation.
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (spanExporter.getFinishedSpanItems().stream()
            .noneMatch(span -> span.getName().equals("CloudSpannerOperation.BatchCreateSessions"))
        && stopwatch.elapsed(TimeUnit.MILLISECONDS) < 100) {
      Thread.yield();
    }
    List<String> actualSpanItems = new ArrayList<>();
    spanExporter
        .getFinishedSpanItems()
        .forEach(
            spanItem -> {
              actualSpanItems.add(spanItem.getName());
              switch (spanItem.getName()) {
                case "CloudSpannerOperation.CreateMultiplexedSession":
                  verifyRequestEvents(
                      spanItem,
                      expectedCreateMultiplexedSessionsRequestEvents,
                      expectedCreateMultiplexedSessionsRequestEventsCount);
                  break;
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
                case "CloudSpannerOperation.Commit":
                case "CloudSpannerOperation.ExecuteUpdate":
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
    List<String> expectedReadWriteTransactionSpans =
        isMultiplexedSessionsEnabled()
            ? ImmutableList.of(
                "CloudSpannerOperation.CreateMultiplexedSession",
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpannerOperation.ExecuteUpdate",
                "CloudSpanner.ReadWriteTransaction")
            : ImmutableList.of(
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpannerOperation.ExecuteUpdate",
                "CloudSpanner.ReadWriteTransaction");
    DatabaseClient client = getClient();
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
                case "CloudSpannerOperation.CreateMultiplexedSession":
                  verifyRequestEvents(
                      spanItem,
                      expectedCreateMultiplexedSessionsRequestEvents,
                      expectedCreateMultiplexedSessionsRequestEventsCount);
                  break;
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
                case "CloudSpanner.ReadWriteTransaction":
                  verifyRequestEvents(
                      spanItem,
                      expectedReadWriteTransactionErrorEvents,
                      expectedReadWriteTransactionErrorEventsCount);
                  break;
                case "CloudSpannerOperation.ExecuteUpdate":
                  assertEquals(0, spanItem.getEvents().size());
                  break;
                default:
                  assert false;
              }
            });

    verifySpans(actualSpanItems, expectedReadWriteTransactionSpans);
  }

  @Test
  public void transactionRunnerWithFailedAndBeginTransaction() {
    List<String> expectedReadWriteTransactionWithCommitAndBeginTransactionSpans =
        isMultiplexedSessionsEnabled()
            ? ImmutableList.of(
                "CloudSpannerOperation.CreateMultiplexedSession",
                "CloudSpannerOperation.BeginTransaction",
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteUpdate",
                "CloudSpannerOperation.Commit",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadWriteTransaction")
            : ImmutableList.of(
                "CloudSpannerOperation.BeginTransaction",
                "CloudSpannerOperation.BatchCreateSessionsRequest",
                "CloudSpannerOperation.ExecuteUpdate",
                "CloudSpannerOperation.Commit",
                "CloudSpannerOperation.BatchCreateSessions",
                "CloudSpanner.ReadWriteTransaction");
    DatabaseClient client = getClient();
    assertEquals(
        Long.valueOf(1L),
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
                }));
    // Wait for all spans to finish. Failing to do so can cause the test to miss the
    // BatchCreateSessions span, as that span is executed asynchronously in the SessionClient, and
    // the SessionClient returns the session to the pool before the span has finished fully.
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (spanExporter.getFinishedSpanItems().size()
            < expectedReadWriteTransactionWithCommitAndBeginTransactionSpans.size()
        && stopwatch.elapsed().compareTo(java.time.Duration.ofMillis(1000)) < 0) {
      Thread.yield();
    }

    List<String> actualSpanItems = new ArrayList<>();
    spanExporter
        .getFinishedSpanItems()
        .forEach(
            spanItem -> {
              actualSpanItems.add(spanItem.getName());
              switch (spanItem.getName()) {
                case "CloudSpannerOperation.CreateMultiplexedSession":
                  verifyRequestEvents(
                      spanItem,
                      expectedCreateMultiplexedSessionsRequestEvents,
                      expectedCreateMultiplexedSessionsRequestEventsCount);
                  break;
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
                case "CloudSpannerOperation.Commit":
                case "CloudSpannerOperation.BeginTransaction":
                case "CloudSpannerOperation.ExecuteUpdate":
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

  @Test
  public void testTransactionRunnerWithRetryOnBeginTransaction() {
    // First get the client to ensure that the BatchCreateSessions request has been executed.
    DatabaseClient clientWithApiTracing = getClientWithApiTracing();

    // Register an UNAVAILABLE error on the server. This error will be returned the first time the
    // BeginTransaction RPC is called. This RPC is then retried, and the transaction succeeds.
    // The retry should be added as an event to the span.
    mockSpanner.addException(Status.UNAVAILABLE.asRuntimeException());

    clientWithApiTracing
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.buffer(Mutation.newInsertBuilder("foo").set("id").to(1L).build());
              return null;
            });

    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    int numExpectedSpans = isMultiplexedSessionsEnabled() ? 10 : 8;
    waitForFinishedSpans(numExpectedSpans);
    List<SpanData> finishedSpans = spanExporter.getFinishedSpanItems();
    List<String> finishedSpanNames =
        finishedSpans.stream().map(SpanData::getName).collect(Collectors.toList());
    String actualSpanNames =
        finishedSpans.stream().map(SpanData::getName).collect(Collectors.joining("\n", "\n", "\n"));
    assertEquals(actualSpanNames, numExpectedSpans, finishedSpans.size());

    assertTrue(actualSpanNames, finishedSpanNames.contains("CloudSpanner.ReadWriteTransaction"));
    assertTrue(
        actualSpanNames, finishedSpanNames.contains("CloudSpannerOperation.BeginTransaction"));
    assertTrue(actualSpanNames, finishedSpanNames.contains("CloudSpannerOperation.Commit"));
    assertTrue(
        actualSpanNames, finishedSpanNames.contains("CloudSpannerOperation.BatchCreateSessions"));
    assertTrue(
        actualSpanNames,
        finishedSpanNames.contains("CloudSpannerOperation.BatchCreateSessionsRequest"));

    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.BatchCreateSessions"));
    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.BeginTransaction"));
    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.Commit"));

    SpanData beginTransactionSpan =
        finishedSpans.stream()
            .filter(span -> span.getName().equals("Spanner.BeginTransaction"))
            .findAny()
            .orElseThrow(IllegalStateException::new);
    assertTrue(
        beginTransactionSpan.toString(),
        beginTransactionSpan.getEvents().stream()
            .anyMatch(event -> event.getName().equals("Starting RPC retry 1")));
  }

  @Test
  public void testSingleUseRetryOnExecuteStreamingSql() {
    // First get the client to ensure that the BatchCreateSessions request has been executed.
    DatabaseClient clientWithApiTracing = getClientWithApiTracing();

    // Register an UNAVAILABLE error on the server. This error will be returned the first time the
    // BeginTransaction RPC is called. This RPC is then retried, and the transaction succeeds.
    // The retry should be added as an event to the span.
    mockSpanner.addException(Status.UNAVAILABLE.asRuntimeException());

    try (ResultSet resultSet = clientWithApiTracing.singleUse().executeQuery(SELECT1)) {
      assertTrue(resultSet.next());
      assertFalse(resultSet.next());
    }

    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    int numExpectedSpans = isMultiplexedSessionsEnabled() ? 9 : 7;
    waitForFinishedSpans(numExpectedSpans);
    List<SpanData> finishedSpans = spanExporter.getFinishedSpanItems();
    List<String> finishedSpanNames =
        finishedSpans.stream().map(SpanData::getName).collect(Collectors.toList());
    String actualSpanNames =
        finishedSpans.stream().map(SpanData::getName).collect(Collectors.joining("\n", "\n", "\n"));
    assertEquals(actualSpanNames, numExpectedSpans, finishedSpans.size());

    assertTrue(actualSpanNames, finishedSpanNames.contains("CloudSpanner.ReadOnlyTransaction"));
    assertTrue(
        actualSpanNames, finishedSpanNames.contains("CloudSpannerOperation.ExecuteStreamingQuery"));
    assertTrue(
        actualSpanNames, finishedSpanNames.contains("CloudSpannerOperation.BatchCreateSessions"));
    assertTrue(
        actualSpanNames,
        finishedSpanNames.contains("CloudSpannerOperation.BatchCreateSessionsRequest"));

    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.BatchCreateSessions"));
    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.ExecuteStreamingSql"));

    // UNAVAILABLE errors on ExecuteStreamingSql are handled manually in the client library, which
    // means that the retry event is on this span.
    SpanData executeStreamingQuery =
        finishedSpans.stream()
            .filter(span -> span.getName().equals("CloudSpannerOperation.ExecuteStreamingQuery"))
            .findAny()
            .orElseThrow(IllegalStateException::new);
    assertTrue(
        executeStreamingQuery.toString(),
        executeStreamingQuery.getEvents().stream()
            .anyMatch(event -> event.getName().contains("Stream broken. Safe to retry")));
  }

  @Test
  public void testRetryOnExecuteSql() {
    // First get the client to ensure that the BatchCreateSessions request has been executed.
    DatabaseClient clientWithApiTracing = getClientWithApiTracing();

    // Register an UNAVAILABLE error on the server. This error will be returned the first time the
    // ExecuteSql RPC is called. This RPC is then retried, and the statement succeeds.
    // The retry should be added as an event to the span.
    mockSpanner.addException(Status.UNAVAILABLE.asRuntimeException());

    clientWithApiTracing
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));

    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    int numExpectedSpans = isMultiplexedSessionsEnabled() ? 10 : 8;
    waitForFinishedSpans(numExpectedSpans);
    List<SpanData> finishedSpans = spanExporter.getFinishedSpanItems();
    List<String> finishedSpanNames =
        finishedSpans.stream().map(SpanData::getName).collect(Collectors.toList());
    String actualSpanNames =
        finishedSpans.stream().map(SpanData::getName).collect(Collectors.joining("\n", "\n", "\n"));
    assertEquals(actualSpanNames, numExpectedSpans, finishedSpans.size());

    assertTrue(actualSpanNames, finishedSpanNames.contains("CloudSpanner.ReadWriteTransaction"));
    assertTrue(actualSpanNames, finishedSpanNames.contains("CloudSpannerOperation.Commit"));
    assertTrue(
        actualSpanNames, finishedSpanNames.contains("CloudSpannerOperation.BatchCreateSessions"));
    assertTrue(
        actualSpanNames,
        finishedSpanNames.contains("CloudSpannerOperation.BatchCreateSessionsRequest"));

    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.BatchCreateSessions"));
    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.ExecuteSql"));
    assertTrue(actualSpanNames, finishedSpanNames.contains("Spanner.Commit"));

    SpanData executeSqlSpan =
        finishedSpans.stream()
            .filter(span -> span.getName().equals("Spanner.ExecuteSql"))
            .findAny()
            .orElseThrow(IllegalStateException::new);
    assertTrue(
        executeSqlSpan.toString(),
        executeSqlSpan.getEvents().stream()
            .anyMatch(event -> event.getName().equals("Starting RPC retry 1")));
  }

  private void waitForFinishedSpans(int numExpectedSpans) {
    // Wait for all spans to finish. Failing to do so can cause the test to miss the
    // BatchCreateSessions span, as that span is executed asynchronously in the SessionClient, and
    // the SessionClient returns the session to the pool before the span has finished fully.
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (spanExporter.getFinishedSpanItems().size() < numExpectedSpans
        && stopwatch.elapsed().compareTo(java.time.Duration.ofMillis(1000)) < 0) {
      Thread.yield();
    }
  }

  private void verifyRequestEvents(SpanData spanItem, List<String> expectedEvents, int eventCount) {
    List<String> eventNames =
        spanItem.getEvents().stream().map(EventData::getName).collect(Collectors.toList());
    assertEquals(eventCount, spanItem.getEvents().size());
    assertEquals(
        expectedEvents.stream().sorted().collect(Collectors.toList()),
        eventNames.stream().distinct().sorted().collect(Collectors.toList()));
  }

  private static void verifySpans(List<String> actualSpanItems, List<String> expectedSpansItems) {
    assertEquals(
        expectedSpansItems.stream().sorted().collect(Collectors.toList()),
        actualSpanItems.stream().distinct().sorted().collect(Collectors.toList()));
  }

  private boolean isMultiplexedSessionsEnabled() {
    if (spanner.getOptions() == null || spanner.getOptions().getSessionPoolOptions() == null) {
      return false;
    }
    return spanner.getOptions().getSessionPoolOptions().getUseMultiplexedSession();
  }
}
