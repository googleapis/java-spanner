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
import static org.junit.Assert.assertTrue;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
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

  @BeforeClass
  public static void startStaticServer() throws IOException {
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
            .buildAndRegisterGlobal();

    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
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
    List<String> spans =
        spanExporter.getFinishedSpanItems().stream()
            .map(SpanData::getName)
            .distinct()
            .collect(Collectors.toList());

    boolean hasSpanData =
        spans.stream()
            .allMatch(
                name ->
                    name.equals("CloudSpannerOperation.BatchCreateSessionsRequest")
                        || name.equals("CloudSpannerOperation.ExecuteStreamingQuery")
                        || name.equals("CloudSpannerOperation.BatchCreateSessions")
                        || name.equals("CloudSpanner.ReadOnlyTransaction")
                        || name.equals("SessionPool.WaitForSession"));
    assertTrue(hasSpanData);
    assertEquals(5, spans.size());
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
    List<String> spans =
        spanExporter.getFinishedSpanItems().stream()
            .map(SpanData::getName)
            .distinct()
            .collect(Collectors.toList());
    boolean hasSpanData =
        spans.stream()
            .allMatch(
                name ->
                    name.equals("CloudSpannerOperation.BatchCreateSessionsRequest")
                        || name.equals("CloudSpannerOperation.ExecuteStreamingQuery")
                        || name.equals("CloudSpannerOperation.BatchCreateSessions")
                        || name.equals("CloudSpanner.ReadOnlyTransaction")
                        || name.equals("SessionPool.WaitForSession"));
    assertTrue(hasSpanData);
    assertEquals(5, spans.size());
  }

  @Test
  public void transactionRunner() {
    TransactionRunner runner = client.readWriteTransaction();
    runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
    List<String> spans =
        spanExporter.getFinishedSpanItems().stream()
            .map(SpanData::getName)
            .distinct()
            .collect(Collectors.toList());
    boolean hasSpanData =
        spans.stream()
            .allMatch(
                name ->
                    name.equals("CloudSpannerOperation.BatchCreateSessionsRequest")
                        || name.equals("CloudSpannerOperation.Commit")
                        || name.equals("CloudSpannerOperation.BatchCreateSessions")
                        || name.equals("CloudSpanner.ReadWriteTransaction")
                        || name.equals("SessionPool.WaitForSession"));
    assertTrue(hasSpanData);
    assertEquals(5, spans.size());
  }

  @Test
  public void transactionRunnerWithError() {
    TransactionRunner runner = client.readWriteTransaction();
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> runner.run(transaction -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT)));
    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());

    List<String> spans =
        spanExporter.getFinishedSpanItems().stream()
            .map(SpanData::getName)
            .distinct()
            .collect(Collectors.toList());
    boolean hasSpanData =
        spans.stream()
            .allMatch(
                name ->
                    name.equals("CloudSpannerOperation.BatchCreateSessionsRequest")
                        || name.equals("CloudSpannerOperation.BatchCreateSessions")
                        || name.equals("CloudSpanner.ReadWriteTransaction")
                        || name.equals("SessionPool.WaitForSession"));
    assertTrue(hasSpanData);
    assertEquals(4, spans.size());
  }
}
