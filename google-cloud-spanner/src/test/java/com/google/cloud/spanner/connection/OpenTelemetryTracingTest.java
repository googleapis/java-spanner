/*
 * Copyright 2024 Google LLC
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

package com.google.cloud.spanner.connection;

import static com.google.cloud.spanner.connection.Repeat.twice;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.SpannerOptionsTestHelper;
import com.google.cloud.spanner.Statement;
import com.google.common.collect.ImmutableList;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OpenTelemetryTracingTest extends AbstractMockServerTest {
  private static InMemorySpanExporter spanExporter;

  private static OpenTelemetrySdk openTelemetry;

  @BeforeClass
  public static void setupOpenTelemetry() {
    SpannerOptionsTestHelper.resetActiveTracingFramework();
    SpannerOptions.enableOpenTelemetryTraces();
    GlobalOpenTelemetry.resetForTest();

    spanExporter = InMemorySpanExporter.create();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    openTelemetry =
        OpenTelemetrySdk.builder()
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();
  }

  @AfterClass
  public static void closeOpenTelemetry() {
    SpannerPool.closeSpannerPool();
    if (openTelemetry != null) {
      openTelemetry.close();
    }
  }

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
    spanExporter.reset();
  }

  public Connection createTestConnection() {
    return ConnectionOptions.newBuilder()
        .setTracingPrefix("CloudSpannerJdbc")
        .setUri(getBaseUrl())
        .build()
        .getConnection();
  }

  @Test
  public void testSingleUseQuery() {
    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(true);
      try (ResultSet resultSet = connection.executeQuery(SELECT1_STATEMENT)) {
        assertTrue(resultSet.next());
        assertFalse(resultSet.next());
      }
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains("CloudSpannerJdbc.SingleUseTransaction", spans);
    assertContains("CloudSpanner.ReadOnlyTransaction", spans);
    assertContains(
        "CloudSpannerOperation.ExecuteStreamingQuery",
        Attributes.of(AttributeKey.stringKey("db.statement"), SELECT1_STATEMENT.getSql()),
        spans);
    assertParent(
        "CloudSpannerJdbc.SingleUseTransaction", "CloudSpanner.ReadOnlyTransaction", spans);
    assertParent(
        "CloudSpanner.ReadOnlyTransaction",
        "CloudSpannerOperation.ExecuteStreamingQuery",
        Attributes.of(AttributeKey.stringKey("db.statement"), SELECT1_STATEMENT.getSql()),
        spans);
  }

  @Test
  public void testSingleUseUpdate() {
    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(true);
      connection.executeUpdate(INSERT_STATEMENT);
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains("CloudSpannerJdbc.SingleUseTransaction", spans);
    assertContains("CloudSpanner.ReadWriteTransaction", spans);
    assertContains(
        "CloudSpannerOperation.ExecuteUpdate",
        Attributes.of(AttributeKey.stringKey("db.statement"), INSERT_STATEMENT.getSql()),
        spans);
    assertContains("CloudSpannerOperation.Commit", spans);

    assertParent(
        "CloudSpannerJdbc.SingleUseTransaction", "CloudSpanner.ReadWriteTransaction", spans);
    assertParent(
        "CloudSpanner.ReadWriteTransaction",
        "CloudSpannerOperation.ExecuteUpdate",
        Attributes.of(AttributeKey.stringKey("db.statement"), INSERT_STATEMENT.getSql()),
        spans);
    assertParent("CloudSpanner.ReadWriteTransaction", "CloudSpannerOperation.Commit", spans);
  }

  @Test
  public void testSingleUseBatchUpdate() {
    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(true);
      connection.startBatchDml();
      connection.executeUpdate(INSERT_STATEMENT);
      connection.executeUpdate(INSERT_STATEMENT);
      connection.runBatch();
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains("CloudSpannerJdbc.SingleUseTransaction", spans);
    assertContains("CloudSpanner.ReadWriteTransaction", spans);
    assertContains(
        "CloudSpannerOperation.BatchUpdate",
        Attributes.of(
            AttributeKey.stringArrayKey("db.statement"),
            ImmutableList.of(INSERT_STATEMENT.getSql(), INSERT_STATEMENT.getSql())),
        spans);
    assertContains("CloudSpannerOperation.Commit", spans);

    assertParent(
        "CloudSpannerJdbc.SingleUseTransaction", "CloudSpanner.ReadWriteTransaction", spans);
    assertParent(
        "CloudSpanner.ReadWriteTransaction",
        "CloudSpannerOperation.BatchUpdate",
        Attributes.of(
            AttributeKey.stringArrayKey("db.statement"),
            ImmutableList.of(INSERT_STATEMENT.getSql(), INSERT_STATEMENT.getSql())),
        spans);
    assertParent("CloudSpanner.ReadWriteTransaction", "CloudSpannerOperation.Commit", spans);
  }

  @Test
  public void testSingleUseDdl() {
    String ddl = "CREATE TABLE foo (id int64) PRIMARY KEY (id)";
    addUpdateDdlResponse();

    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(true);
      connection.execute(Statement.of(ddl));
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains(
        "CloudSpannerJdbc.DdlStatement",
        Attributes.of(AttributeKey.stringKey("db.statement"), ddl),
        spans);
  }

  @Test
  public void testSingleUseDdlBatch() {
    String ddl1 = "CREATE TABLE foo (id int64, value string(max)) PRIMARY KEY (id)";
    String ddl2 = "CREATE INDEX idx_foo ON foo (value)";
    addUpdateDdlResponse();

    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(true);
      connection.startBatchDdl();
      connection.execute(Statement.of(ddl1));
      connection.execute(Statement.of(ddl2));
      connection.runBatch();
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains(
        "CloudSpannerJdbc.DdlBatch",
        Attributes.of(AttributeKey.stringArrayKey("db.statement"), ImmutableList.of(ddl1, ddl2)),
        spans);
  }

  @Test
  public void testMultiUseReadOnlyQueries() {
    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(false);
      connection.setReadOnly(true);
      twice(
          () -> {
            try (ResultSet resultSet = connection.executeQuery(SELECT1_STATEMENT)) {
              assertTrue(resultSet.next());
              assertFalse(resultSet.next());
            }
          });
      connection.commit();
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains("CloudSpannerJdbc.ReadOnlyTransaction", spans);
    assertContains("CloudSpanner.ReadOnlyTransaction", spans);
    assertContains(
        "CloudSpannerOperation.ExecuteStreamingQuery",
        2,
        Attributes.of(AttributeKey.stringKey("db.statement"), SELECT1_STATEMENT.getSql()),
        spans);
    assertParent("CloudSpannerJdbc.ReadOnlyTransaction", "CloudSpanner.ReadOnlyTransaction", spans);
    assertParent(
        "CloudSpanner.ReadOnlyTransaction",
        "CloudSpannerOperation.ExecuteStreamingQuery",
        Attributes.of(AttributeKey.stringKey("db.statement"), SELECT1_STATEMENT.getSql()),
        spans);
  }

  @Test
  public void testMultiUseReadWriteQueries() {
    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(false);
      connection.setReadOnly(false);
      twice(
          () -> {
            try (ResultSet resultSet = connection.executeQuery(SELECT1_STATEMENT)) {
              assertTrue(resultSet.next());
              assertFalse(resultSet.next());
            }
          });
      connection.commit();
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains("CloudSpannerJdbc.ReadWriteTransaction", spans);
    assertContains("CloudSpanner.ReadWriteTransaction", spans);
    assertContains(
        "CloudSpannerOperation.ExecuteStreamingQuery",
        2,
        Attributes.of(AttributeKey.stringKey("db.statement"), SELECT1_STATEMENT.getSql()),
        spans);
    assertContains("CloudSpannerOperation.Commit", spans);
    assertParent(
        "CloudSpannerJdbc.ReadWriteTransaction", "CloudSpanner.ReadWriteTransaction", spans);
    assertParent(
        "CloudSpanner.ReadWriteTransaction",
        "CloudSpannerOperation.ExecuteStreamingQuery",
        Attributes.of(AttributeKey.stringKey("db.statement"), SELECT1_STATEMENT.getSql()),
        spans);
    assertParent("CloudSpanner.ReadWriteTransaction", "CloudSpannerOperation.Commit", spans);
  }

  @Test
  public void testMultiUseReadWriteUpdates() {
    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(false);
      connection.setReadOnly(false);
      assertEquals(1L, connection.executeUpdate(INSERT_STATEMENT));
      assertEquals(1L, connection.executeUpdate(INSERT_STATEMENT));
      connection.commit();
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains("CloudSpannerJdbc.ReadWriteTransaction", spans);
    assertContains("CloudSpanner.ReadWriteTransaction", spans);
    assertContains(
        "CloudSpannerOperation.ExecuteUpdate",
        2,
        Attributes.of(AttributeKey.stringKey("db.statement"), INSERT_STATEMENT.getSql()),
        spans);
    assertContains("CloudSpannerOperation.Commit", spans);
    assertParent(
        "CloudSpannerJdbc.ReadWriteTransaction", "CloudSpanner.ReadWriteTransaction", spans);
    assertParent(
        "CloudSpanner.ReadWriteTransaction",
        "CloudSpannerOperation.ExecuteUpdate",
        Attributes.of(AttributeKey.stringKey("db.statement"), INSERT_STATEMENT.getSql()),
        spans);
    assertParent("CloudSpanner.ReadWriteTransaction", "CloudSpannerOperation.Commit", spans);
  }

  @Test
  public void testMultiUseReadWriteBatchUpdates() {
    try (Connection connection = createTestConnection()) {
      connection.setAutocommit(false);
      connection.setReadOnly(false);

      twice(
          () -> {
            connection.startBatchDml();
            connection.executeUpdate(INSERT_STATEMENT);
            connection.executeUpdate(INSERT_STATEMENT);
            connection.runBatch();
          });

      connection.commit();
    }
    assertEquals(CompletableResultCode.ofSuccess(), spanExporter.flush());
    List<SpanData> spans = spanExporter.getFinishedSpanItems();
    assertContains("CloudSpannerJdbc.ReadWriteTransaction", spans);
    assertContains("CloudSpanner.ReadWriteTransaction", spans);
    assertContains(
        "CloudSpannerOperation.BatchUpdate",
        2,
        Attributes.of(
            AttributeKey.stringArrayKey("db.statement"),
            ImmutableList.of(INSERT_STATEMENT.getSql(), INSERT_STATEMENT.getSql())),
        spans);
    assertContains("CloudSpannerOperation.Commit", spans);
    assertParent(
        "CloudSpannerJdbc.ReadWriteTransaction", "CloudSpanner.ReadWriteTransaction", spans);
    assertParent(
        "CloudSpanner.ReadWriteTransaction",
        "CloudSpannerOperation.BatchUpdate",
        Attributes.of(
            AttributeKey.stringArrayKey("db.statement"),
            ImmutableList.of(INSERT_STATEMENT.getSql(), INSERT_STATEMENT.getSql())),
        spans);
    assertParent("CloudSpanner.ReadWriteTransaction", "CloudSpannerOperation.Commit", spans);
  }

  void assertContains(String expected, List<SpanData> spans) {
    assertTrue(
        "Expected " + spansToString(spans) + " to contain " + expected,
        spans.stream().anyMatch(span -> span.getName().equals(expected)));
  }

  void assertContains(String expected, Attributes attributes, List<SpanData> spans) {
    assertContains(expected, 1, attributes, spans);
  }

  void assertContains(String expected, int count, Attributes attributes, List<SpanData> spans) {
    assertEquals(
        "Expected " + spansToString(spans) + " to contain " + expected,
        count,
        spans.stream().filter(span -> equalsSpan(span, expected, attributes)).count());
  }

  boolean equalsSpan(SpanData span, String name, Attributes attributes) {
    if (!span.getName().equals(name)) {
      return false;
    }
    for (Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
      if (!span.getAttributes().asMap().containsKey(entry.getKey())) {
        return false;
      }
      if (!Objects.equals(entry.getValue(), span.getAttributes().get(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  void assertParent(String expectedParent, String child, List<SpanData> spans) {
    SpanData parentSpan = getSpan(expectedParent, spans);
    SpanData childSpan = getSpan(child, spans);
    assertEquals(parentSpan.getSpanId(), childSpan.getParentSpanId());
  }

  void assertParent(
      String expectedParent, String child, Attributes attributes, List<SpanData> spans) {
    SpanData parentSpan = getSpan(expectedParent, spans);
    SpanData childSpan = getSpan(child, attributes, spans);
    assertEquals(parentSpan.getSpanId(), childSpan.getParentSpanId());
  }

  SpanData getSpan(String name, List<SpanData> spans) {
    return spans.stream()
        .filter(span -> span.getName().equals(name))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException("Span " + name + " not found"));
  }

  SpanData getSpan(String name, Attributes attributes, List<SpanData> spans) {
    return spans.stream()
        .filter(span -> equalsSpan(span, name, attributes))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException("Span " + name + " not found"));
  }

  private String spansToString(List<SpanData> spans) {
    return spans.stream().map(SpanData::getName).collect(Collectors.joining("\n", "\n", "\n"));
  }

  private void addUpdateDdlResponse() {
    mockDatabaseAdmin.addResponse(
        Operation.newBuilder()
            .setMetadata(
                Any.pack(
                    UpdateDatabaseDdlMetadata.newBuilder()
                        .setDatabase("projects/proj/instances/inst/databases/db")
                        .build()))
            .setName("projects/proj/instances/inst/databases/db/operations/1")
            .setDone(true)
            .setResponse(Any.pack(Empty.getDefaultInstance()))
            .build());
  }
}
