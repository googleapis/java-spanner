/*
 * Copyright 2019 Google LLC
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

import static com.google.cloud.spanner.SpannerApiFutures.get;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionStep;
import com.google.cloud.spanner.AsyncTransactionManager.CommitTimestampFuture;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.SessionPoolOptions.Builder;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerClient.ListSessionsPagedResponse;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.common.base.Function;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.threeten.bp.Duration;

@RunWith(Parameterized.class)
public class RetryOnInvalidatedSessionTest {
  private static final class ToLongTransformer implements Function<StructReader, Long> {
    @Override
    public Long apply(StructReader input) {
      return input.getLong(0);
    }
  }

  private static final ToLongTransformer TO_LONG = new ToLongTransformer();

  @Parameter(0)
  public boolean failOnInvalidatedSession;

  @Parameters(name = "fail on invalidated session = {0}")
  public static Collection<Object[]> data() {
    List<Object[]> params = new ArrayList<>();
    params.add(new Object[] {false});
    params.add(new Object[] {true});
    return params;
  }

  private static final ResultSetMetadata READ_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("BAR")
                          .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet READ_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(Value.newBuilder().setStringValue("1").build())
                  .build())
          .addRows(
              ListValue.newBuilder()
                  .addValues(Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(READ_METADATA)
          .build();
  private static final com.google.spanner.v1.ResultSet READ_ROW_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(Value.newBuilder().setStringValue("1").build())
                  .build())
          .setMetadata(READ_METADATA)
          .build();
  private static final Statement SELECT1AND2 =
      Statement.of("SELECT 1 AS COL1 UNION ALL SELECT 2 AS COL1");
  private static final ResultSetMetadata SELECT1AND2_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("COL1")
                          .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(Value.newBuilder().setStringValue("1").build())
                  .build())
          .addRows(
              ListValue.newBuilder()
                  .addValues(Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(SELECT1AND2_METADATA)
          .build();
  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final long UPDATE_COUNT = 1L;
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private static SpannerClient spannerClient;
  private static Spanner spanner;
  private static DatabaseClient client;
  private static ExecutorService executor;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(
        StatementResult.read(
            "FOO", KeySet.all(), Collections.singletonList("BAR"), READ_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.read(
            "FOO",
            KeySet.singleKey(Key.of()),
            Collections.singletonList("BAR"),
            READ_ROW_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.query(SELECT1AND2, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));

    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .directExecutor()
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);

    SpannerSettings settings =
        SpannerSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    spannerClient = SpannerClient.create(settings);
    executor = Executors.newSingleThreadExecutor();
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    spannerClient.close();
    server.shutdown();
    server.awaitTermination();
    executor.shutdown();
  }

  @Before
  public void setUp() throws InterruptedException {
    mockSpanner.reset();
    if (spanner == null
        || spanner.getOptions().getSessionPoolOptions().isFailIfPoolExhausted()
            != failOnInvalidatedSession) {
      if (spanner != null) {
        spanner.close();
      }
      Builder builder =
          SessionPoolOptions.newBuilder()
              .setFailOnSessionLeak()
              .setFailIfNumSessionsInUseIsNegative()
              .setWaitForMinSessions(Duration.ofSeconds(10L));
      if (failOnInvalidatedSession) {
        builder.setFailIfSessionNotFound();
      }
      // This prevents repeated retries for a large number of sessions in the pool.
      builder.setMinSessions(1);
      spanner =
          SpannerOptions.newBuilder()
              .setProjectId("[PROJECT]")
              .setChannelProvider(channelProvider)
              .setSessionPoolOption(builder.build())
              .setCredentials(NoCredentials.getInstance())
              .build()
              .getService();
      client = spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      invalidateSessionPool();
    }
  }

  private static void invalidateSessionPool() {
    // Delete all sessions on the mock server.
    ListSessionsPagedResponse response =
        spannerClient.listSessions("projects/[PROJECT]/instances/[INSTANCE]/databases/[DATABASE]");
    for (Session session : response.iterateAll()) {
      spannerClient.deleteSession(session.getName());
    }
  }

  private <T> T assertThrowsSessionNotFoundIfShouldFail(Supplier<T> supplier) {
    if (failOnInvalidatedSession) {
      assertThrows(SessionNotFoundException.class, supplier::get);
      return null;
    } else {
      return supplier.get();
    }
  }

  private int numExpectedRequests() {
    return failOnInvalidatedSession ? 1 : 2;
  }

  @Test
  public void singleUseSelect() {
    // This call will receive an invalidated session that will be replaced on the first call to
    // rs.next().
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void singleUseSelectAsync() {
    ApiFuture<List<Long>> list;
    try (AsyncResultSet rs = client.singleUse().executeQueryAsync(SELECT1AND2)) {
      list = rs.toListAsync(TO_LONG, executor);
      assertThrowsSessionNotFoundIfShouldFail(() -> get(list));
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void singleUseRead() {
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void singleUseReadUsingIndex() {
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void singleUseReadRow() {
    try (ReadContext context = client.singleUse()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void singleUseReadRowUsingIndex() {
    try (ReadContext context = client.singleUse()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void singleUseReadOnlyTransactionSelect() {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void singleUseReadOnlyTransactionRead() {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void singleUseReadOnlyTransactionReadUsingIndex() {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void singleUseReadOnlyTransactionReadRow() {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void singleUseReadOnlyTransactionReadRowUsingIndex() {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readOnlyTransactionSelect() {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionRead() {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionReadUsingIndex() {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
    }
    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionReadRow() {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionReadRowUsingIndex() {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionSelectNonRecoverable() {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
      // Invalidate the session pool while in a transaction. This is not recoverable.
      invalidateSessionPool();
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrows(SessionNotFoundException.class, rs::next);
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 4 : 2,
        mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionReadNonRecoverable() {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
      invalidateSessionPool();
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrows(SessionNotFoundException.class, rs::next);
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 4 : 2,
        mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionReadUsingIndexNonRecoverable() {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(rs::next);
      }
      invalidateSessionPool();
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrows(SessionNotFoundException.class, rs::next);
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 4 : 2,
        mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionReadRowNonRecoverable() {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
      invalidateSessionPool();
      assertThrows(
          SessionNotFoundException.class,
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readOnlyTransactionReadRowUsingIndexNonRecoverable() {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
      invalidateSessionPool();
      assertThrows(
          SessionNotFoundException.class,
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readWriteTransactionSelect() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  return null;
                }));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void readWriteTransactionRead() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  try (ResultSet rs =
                      transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  return null;
                }));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionReadWithOptimisticLock() {
    TransactionRunner runner = client.readWriteTransaction(Options.optimisticLock());
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  try (ResultSet rs =
                      transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  return null;
                }));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionReadUsingIndex() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  try (ResultSet rs =
                      transaction.readUsingIndex(
                          "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  return null;
                }));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionReadRow() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction ->
                    transaction.readRow("FOO", Key.of(), Collections.singletonList("BAR"))));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionReadRowUsingIndex() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction ->
                    transaction.readRowUsingIndex(
                        "FOO", "IDX", Key.of(), Collections.singletonList("BAR"))));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionUpdate() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () -> runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT)));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void readWriteTransactionBatchUpdate() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction ->
                    transaction.batchUpdate(Collections.singletonList(UPDATE_STATEMENT))));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
  }

  @Test
  public void readWriteTransactionBuffer() {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  transaction.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
                  return null;
                }));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void readWriteTransactionSelectInvalidatedDuringTransaction() {
    TransactionRunner runner = client.readWriteTransaction();
    final AtomicInteger attempt = new AtomicInteger();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  attempt.incrementAndGet();
                  try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  if (attempt.get() == 1) {
                    invalidateSessionPool();
                  }
                  try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void readWriteTransactionReadInvalidatedDuringTransaction() {
    TransactionRunner runner = client.readWriteTransaction();
    final AtomicInteger attempt = new AtomicInteger();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  attempt.incrementAndGet();
                  try (ResultSet rs =
                      transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  if (attempt.get() == 1) {
                    invalidateSessionPool();
                  }
                  try (ResultSet rs =
                      transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionReadUsingIndexInvalidatedDuringTransaction() {
    TransactionRunner runner = client.readWriteTransaction();
    final AtomicInteger attempt = new AtomicInteger();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  attempt.incrementAndGet();
                  try (ResultSet rs =
                      transaction.readUsingIndex(
                          "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  if (attempt.get() == 1) {
                    invalidateSessionPool();
                  }
                  try (ResultSet rs =
                      transaction.readUsingIndex(
                          "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
                    //noinspection StatementWithEmptyBody
                    while (rs.next()) {}
                  }
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionReadRowInvalidatedDuringTransaction() {
    TransactionRunner runner = client.readWriteTransaction();
    final AtomicInteger attempt = new AtomicInteger();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  attempt.incrementAndGet();
                  Struct row =
                      transaction.readRow("FOO", Key.of(), Collections.singletonList("BAR"));
                  assertNotNull(row);
                  assertEquals(1L, row.getLong(0));
                  if (attempt.get() == 1) {
                    invalidateSessionPool();
                  }
                  transaction.readRow("FOO", Key.of(), Collections.singletonList("BAR"));
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void readWriteTransactionReadRowUsingIndexInvalidatedDuringTransaction() {
    TransactionRunner runner = client.readWriteTransaction();
    final AtomicInteger attempt = new AtomicInteger();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  attempt.incrementAndGet();
                  Struct row =
                      transaction.readRowUsingIndex(
                          "FOO", "IDX", Key.of(), Collections.singletonList("BAR"));
                  assertNotNull(row);
                  assertEquals(1L, row.getLong(0));
                  if (attempt.get() == 1) {
                    invalidateSessionPool();
                  }
                  transaction.readRowUsingIndex(
                      "FOO", "IDX", Key.of(), Collections.singletonList("BAR"));
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerSelect() {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            assertThrowsSessionNotFoundIfShouldFail(rs::next);
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerRead() {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs =
              transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
            assertThrowsSessionNotFoundIfShouldFail(rs::next);
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadUsingIndex() {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs =
              transaction.readUsingIndex(
                  "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
            assertThrowsSessionNotFoundIfShouldFail(rs::next);
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRow() {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          TransactionContext context = transaction;
          assertThrowsSessionNotFoundIfShouldFail(
              () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRowUsingIndex() {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          TransactionContext context = transaction;
          assertThrowsSessionNotFoundIfShouldFail(
              () ->
                  context.readRowUsingIndex(
                      "FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerUpdate() {
    try (TransactionManager manager = client.transactionManager(Options.commitStats())) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          TransactionContext context = transaction;
          assertThrowsSessionNotFoundIfShouldFail(() -> context.executeUpdate(UPDATE_STATEMENT));
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerAborted_thenSessionNotFoundOnBeginTransaction() {
    int attempt = 0;
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          attempt++;
          if (attempt == 1) {
            mockSpanner.abortNextStatement();
          }
          if (attempt == 2) {
            invalidateSessionPool();
          }
          TransactionContext context = transaction;
          assertThrowsSessionNotFoundIfShouldFail(() -> context.executeUpdate(UPDATE_STATEMENT));
          manager.commit();
          // The actual number of attempts depends on when the transaction manager will actually get
          // a valid session, as we invalidate the entire session pool.
          assertThat(attempt).isAtLeast(3);
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(1, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    assertEquals(
        failOnInvalidatedSession ? 1 : 4, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerBatchUpdate() {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          TransactionContext context = transaction;
          assertThrowsSessionNotFoundIfShouldFail(
              () -> context.batchUpdate(Collections.singletonList(UPDATE_STATEMENT)));
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
    }
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerBuffer() {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        transaction.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
        try {
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
          if (transaction == null) {
            break;
          }
        }
      }
      assertThat(manager.getCommitTimestamp()).isNotNull();
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void transactionManagerSelectInvalidatedDuringTransaction() {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        try {
          assertNotNull(transaction);
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            if (assertThrowsSessionNotFoundIfShouldFail(rs::next) == null) {
              break;
            }
          }
          if (attempts == 1) {
            invalidateSessionPool();
          }
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            if (assertThrowsSessionNotFoundIfShouldFail(rs::next) == null) {
              break;
            }
          }
          manager.commit();
          assertThat(attempts).isGreaterThan(1);
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(manager::resetForRetry);
        }
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void transactionManagerReadInvalidatedDuringTransaction() {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        try {
          try (ResultSet rs =
              transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
            if (assertThrowsSessionNotFoundIfShouldFail(rs::next) == null) {
              break;
            }
          }
          if (attempts == 1) {
            invalidateSessionPool();
          }
          try (ResultSet rs =
              transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
            if (assertThrowsSessionNotFoundIfShouldFail(rs::next) == null) {
              break;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void transactionManagerReadUsingIndexInvalidatedDuringTransaction() {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        try {
          try (ResultSet rs =
              transaction.readUsingIndex(
                  "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
            if (assertThrowsSessionNotFoundIfShouldFail(rs::next) == null) {
              break;
            }
          }
          if (attempts == 1) {
            invalidateSessionPool();
          }
          try (ResultSet rs =
              transaction.readUsingIndex(
                  "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
            if (assertThrowsSessionNotFoundIfShouldFail(rs::next) == null) {
              break;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void transactionManagerReadRowInvalidatedDuringTransaction() {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        try {
          TransactionContext context = transaction;
          if (assertThrowsSessionNotFoundIfShouldFail(
                  () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")))
              == null) {
            break;
          }
          if (attempts == 1) {
            invalidateSessionPool();
          }
          if (assertThrowsSessionNotFoundIfShouldFail(
                  () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")))
              == null) {
            break;
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void transactionManagerReadRowUsingIndexInvalidatedDuringTransaction() {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        try {
          TransactionContext context = transaction;
          if (assertThrowsSessionNotFoundIfShouldFail(
                  () ->
                      context.readRowUsingIndex(
                          "FOO", "IDX", Key.of(), Collections.singletonList("BAR")))
              == null) {
            break;
          }
          if (attempts == 1) {
            invalidateSessionPool();
          }
          if (assertThrowsSessionNotFoundIfShouldFail(
                  () ->
                      context.readRowUsingIndex(
                          "FOO", "IDX", Key.of(), Collections.singletonList("BAR")))
              == null) {
            break;
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetry();
        }
      }
    }
    assertEquals(
        failOnInvalidatedSession ? 1 : 3, mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void partitionedDml() {
    assertThrowsSessionNotFoundIfShouldFail(
        () -> client.executePartitionedUpdate(UPDATE_STATEMENT));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void write() throws InterruptedException {
    assertThrowsSessionNotFoundIfShouldFail(
        () -> client.write(Collections.singletonList(Mutation.delete("FOO", KeySet.all()))));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void writeAtLeastOnce() throws InterruptedException {
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            client.writeAtLeastOnce(
                Collections.singletonList(Mutation.delete("FOO", KeySet.all()))));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void asyncRunnerSelect() {
    asyncRunner_withReadFunction(input -> input.executeQueryAsync(SELECT1AND2));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void asyncRunnerRead() {
    asyncRunner_withReadFunction(
        input -> input.readAsync("FOO", KeySet.all(), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncRunnerReadUsingIndex() {
    asyncRunner_withReadFunction(
        input ->
            input.readUsingIndexAsync(
                "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  private void asyncRunner_withReadFunction(
      final java.util.function.Function<TransactionContext, AsyncResultSet> readFunction) {
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try {
      AsyncRunner runner = client.runAsync();
      final AtomicLong counter = new AtomicLong();
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              get(
                  runner.runAsync(
                      txn -> {
                        AsyncResultSet rs = readFunction.apply(txn);
                        ApiFuture<Void> fut =
                            rs.setCallback(
                                queryExecutor,
                                resultSet -> {
                                  while (true) {
                                    switch (resultSet.tryNext()) {
                                      case OK:
                                        counter.incrementAndGet();
                                        break;
                                      case DONE:
                                        return CallbackResponse.DONE;
                                      case NOT_READY:
                                        return CallbackResponse.CONTINUE;
                                    }
                                  }
                                });
                        return ApiFutures.transform(
                            fut, input -> counter.get(), MoreExecutors.directExecutor());
                      },
                      executor)));
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncRunnerReadRow() {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            get(
                runner.runAsync(
                    txn -> txn.readRowAsync("FOO", Key.of(), Collections.singletonList("BAR")),
                    executor)));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncRunnerReadRowUsingIndex() {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            get(
                runner.runAsync(
                    txn ->
                        txn.readRowUsingIndexAsync(
                            "FOO", "IDX", Key.of(), Collections.singletonList("BAR")),
                    executor)));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncRunnerUpdate() {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () -> get(runner.runAsync(txn -> txn.executeUpdateAsync(UPDATE_STATEMENT), executor)));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void asyncRunnerBatchUpdate() {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            get(
                runner.runAsync(
                    txn -> txn.batchUpdateAsync(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)),
                    executor)));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
  }

  @Test
  public void asyncRunnerBuffer() {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            get(
                runner.runAsync(
                    txn -> {
                      txn.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
                      return ApiFutures.immediateFuture(null);
                    },
                    executor)));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }

  @Test
  public void asyncTransactionManagerAsyncSelect() {
    asyncTransactionManager_readAsync(input -> input.executeQueryAsync(SELECT1AND2));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void asyncTransactionManagerAsyncRead() {
    asyncTransactionManager_readAsync(
        input -> input.readAsync("FOO", KeySet.all(), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncTransactionManagerAsyncReadUsingIndex() {
    asyncTransactionManager_readAsync(
        input ->
            input.readUsingIndexAsync(
                "FOO", "idx", KeySet.all(), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  private void asyncTransactionManager_readAsync(
      final java.util.function.Function<TransactionContext, AsyncResultSet> fn) {
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture context = manager.beginAsync();
      while (true) {
        try {
          final AtomicLong counter = new AtomicLong();
          AsyncTransactionStep<Void, Long> count =
              context.then(
                  (transaction, ignored) -> {
                    AsyncResultSet rs = fn.apply(transaction);
                    ApiFuture<Void> fut =
                        rs.setCallback(
                            queryExecutor,
                            resultSet -> {
                              while (true) {
                                switch (resultSet.tryNext()) {
                                  case OK:
                                    counter.incrementAndGet();
                                    break;
                                  case DONE:
                                    return CallbackResponse.DONE;
                                  case NOT_READY:
                                    return CallbackResponse.CONTINUE;
                                }
                              }
                            });
                    return ApiFutures.transform(
                        fut, input -> counter.get(), MoreExecutors.directExecutor());
                  },
                  executor);
          CommitTimestampFuture ts = count.commitAsync();
          assertThrowsSessionNotFoundIfShouldFail(() -> get(ts));
          break;
        } catch (AbortedException e) {
          context = manager.resetForRetryAsync();
        }
      }
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncTransactionManagerSelect() {
    asyncTransactionManager_readSync(input -> input.executeQuery(SELECT1AND2));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void asyncTransactionManagerRead() {
    asyncTransactionManager_readSync(
        input -> input.read("FOO", KeySet.all(), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncTransactionManagerReadUsingIndex() {
    asyncTransactionManager_readSync(
        input ->
            input.readUsingIndex("FOO", "idx", KeySet.all(), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  private void asyncTransactionManager_readSync(
      final java.util.function.Function<TransactionContext, ResultSet> fn) {
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture context = manager.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Void, Long> count =
              context.then(
                  (transaction, ignored) -> {
                    long counter = 0L;
                    try (ResultSet rs = fn.apply(transaction)) {
                      while (rs.next()) {
                        counter++;
                      }
                    }
                    return ApiFutures.immediateFuture(counter);
                  },
                  executor);
          CommitTimestampFuture ts = count.commitAsync();
          assertThrowsSessionNotFoundIfShouldFail(() -> get(ts));
          break;
        } catch (AbortedException e) {
          context = manager.resetForRetryAsync();
        }
      }
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncTransactionManagerReadRow() {
    asyncTransactionManager_readRowFunction(
        input ->
            ApiFutures.immediateFuture(
                input.readRow("FOO", Key.of("foo"), Collections.singletonList("BAR"))));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncTransactionManagerReadRowUsingIndex() {
    asyncTransactionManager_readRowFunction(
        input ->
            ApiFutures.immediateFuture(
                input.readRowUsingIndex(
                    "FOO", "idx", Key.of("foo"), Collections.singletonList("BAR"))));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncTransactionManagerReadRowAsync() {
    asyncTransactionManager_readRowFunction(
        input -> input.readRowAsync("FOO", Key.of("foo"), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  @Test
  public void asyncTransactionManagerReadRowUsingIndexAsync() {
    asyncTransactionManager_readRowFunction(
        input ->
            input.readRowUsingIndexAsync(
                "FOO", "idx", Key.of("foo"), Collections.singletonList("BAR")));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ReadRequest.class));
  }

  private void asyncTransactionManager_readRowFunction(
      final java.util.function.Function<TransactionContext, ApiFuture<Struct>> fn) {
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture context = manager.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Void, Struct> row =
              context.then((transaction, ignored) -> fn.apply(transaction), executor);
          CommitTimestampFuture ts = row.commitAsync();
          assertThrowsSessionNotFoundIfShouldFail(() -> get(ts));
          break;
        } catch (AbortedException e) {
          context = manager.resetForRetryAsync();
        }
      }
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncTransactionManagerUpdateAsync() {
    asyncTransactionManager_updateFunction(input -> input.executeUpdateAsync(UPDATE_STATEMENT));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void asyncTransactionManagerUpdate() {
    asyncTransactionManager_updateFunction(
        input -> ApiFutures.immediateFuture(input.executeUpdate(UPDATE_STATEMENT)));
    assertEquals(numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }

  @Test
  public void asyncTransactionManagerBatchUpdateAsync() {
    asyncTransactionManager_updateFunction(
        input -> input.batchUpdateAsync(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
  }

  @Test
  public void asyncTransactionManagerBatchUpdate() {
    asyncTransactionManager_updateFunction(
        input ->
            ApiFutures.immediateFuture(
                input.batchUpdate(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT))));
    assertEquals(
        numExpectedRequests(), mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
  }

  private <T> void asyncTransactionManager_updateFunction(
      final java.util.function.Function<TransactionContext, ApiFuture<T>> fn) {
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture transaction = manager.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Void, T> res =
              transaction.then((txn, input) -> fn.apply(txn), executor);
          CommitTimestampFuture ts = res.commitAsync();
          assertThrowsSessionNotFoundIfShouldFail(() -> get(ts));
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetryAsync();
        }
      }
    }
  }
}
