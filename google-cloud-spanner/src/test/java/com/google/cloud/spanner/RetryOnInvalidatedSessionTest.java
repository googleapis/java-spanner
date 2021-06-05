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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

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
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerClient.ListSessionsPagedResponse;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
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
import java.util.concurrent.TimeUnit;
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
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.INT64)
                                  .build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet READ_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                  .build())
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(READ_METADATA)
          .build();
  private static final com.google.spanner.v1.ResultSet READ_ROW_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
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
      SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder().setFailOnSessionLeak();
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
      invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
    }
  }

  private static void invalidateSessionPool(DatabaseClient client, int minSessions)
      throws InterruptedException {
    // Wait for all sessions to have been created, and then delete them.
    Stopwatch watch = Stopwatch.createStarted();
    while (((DatabaseClientImpl) client).pool.totalSessions() < minSessions) {
      if (watch.elapsed(TimeUnit.SECONDS) > 5L) {
        fail(String.format("Failed to create MinSessions=%d", minSessions));
      }
      Thread.sleep(1L);
    }

    ListSessionsPagedResponse response =
        spannerClient.listSessions("projects/[PROJECT]/instances/[INSTANCE]/databases/[DATABASE]");
    for (com.google.spanner.v1.Session session : response.iterateAll()) {
      spannerClient.deleteSession(session.getName());
    }
  }

  private <T> T assertThrowsSessionNotFoundIfShouldFail(Supplier<T> supplier) {
    if (failOnInvalidatedSession) {
      assertThrows(SessionNotFoundException.class, () -> supplier.get());
      return null;
    } else {
      return supplier.get();
    }
  }

  @Test
  public void singleUseSelect() throws InterruptedException {
    // This call will receive an invalidated session that will be replaced on the first call to
    // rs.next().
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void singleUseSelectAsync() throws Exception {
    ApiFuture<List<Long>> list;
    try (AsyncResultSet rs = client.singleUse().executeQueryAsync(SELECT1AND2)) {
      list = rs.toListAsync(TO_LONG, executor);
      assertThrowsSessionNotFoundIfShouldFail(() -> get(list));
    }
  }

  @Test
  public void singleUseRead() throws InterruptedException {
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void singleUseReadUsingIndex() throws InterruptedException {
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void singleUseReadRow() throws InterruptedException {
    try (ReadContext context = client.singleUse()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void singleUseReadRowUsingIndex() throws InterruptedException {
    try (ReadContext context = client.singleUse()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void singleUseReadOnlyTransactionSelect() throws InterruptedException {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void singleUseReadOnlyTransactionRead() throws InterruptedException {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void singlUseReadOnlyTransactionReadUsingIndex() throws InterruptedException {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void singleUseReadOnlyTransactionReadRow() throws InterruptedException {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void singleUseReadOnlyTransactionReadRowUsingIndex() throws InterruptedException {
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void readOnlyTransactionSelect() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void readOnlyTransactionRead() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void readOnlyTransactionReadUsingIndex() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
    }
  }

  @Test
  public void readOnlyTransactionReadRow() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void readOnlyTransactionReadRowUsingIndex() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void readOnlyTransactionSelectNonRecoverable() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
      // Invalidate the session pool while in a transaction. This is not recoverable.
      invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        assertThrows(SessionNotFoundException.class, () -> rs.next());
      }
    }
  }

  @Test
  public void readOnlyTransactionReadNonRecoverable() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
      invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
      try (ResultSet rs = context.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrows(SessionNotFoundException.class, () -> rs.next());
      }
    }
  }

  @Test
  public void readOnlyTransactionReadUsingIndexNonRecoverable() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
      }
      invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
        assertThrows(SessionNotFoundException.class, () -> rs.next());
      }
    }
  }

  @Test
  public void readOnlyTransactionReadRowNonRecoverable() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
      invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
      assertThrows(
          SessionNotFoundException.class,
          () -> context.readRow("FOO", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void readOnlyTransactionReadRowUsingIndexNonRecoverable() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
      invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
      assertThrows(
          SessionNotFoundException.class,
          () ->
              context.readRowUsingIndex("FOO", "IDX", Key.of(), Collections.singletonList("BAR")));
    }
  }

  @Test
  public void readWriteTransactionReadOnlySessionInPool() throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
      TransactionRunner runner = client.readWriteTransaction();
      assertThrowsSessionNotFoundIfShouldFail(
          () ->
              runner.run(
                  transaction -> {
                    try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                      while (rs.next()) {}
                    }
                    return null;
                  }));
    }
  }

  @Test
  public void readWriteTransactionSelect() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                    while (rs.next()) {}
                  }
                  return null;
                }));
  }

  @Test
  public void readWriteTransactionRead() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  try (ResultSet rs =
                      transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
                    while (rs.next()) {}
                  }
                  return null;
                }));
  }

  @Test
  public void readWriteTransactionReadUsingIndex() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  try (ResultSet rs =
                      transaction.readUsingIndex(
                          "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
                    while (rs.next()) {}
                  }
                  return null;
                }));
  }

  @Test
  public void readWriteTransactionReadRow() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction ->
                    transaction.readRow("FOO", Key.of(), Collections.singletonList("BAR"))));
  }

  @Test
  public void readWriteTransactionReadRowUsingIndex() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction ->
                    transaction.readRowUsingIndex(
                        "FOO", "IDX", Key.of(), Collections.singletonList("BAR"))));
  }

  @Test
  public void readWriteTransactionUpdate() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () -> runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT)));
  }

  @Test
  public void readWriteTransactionBatchUpdate() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction ->
                    transaction.batchUpdate(Collections.singletonList(UPDATE_STATEMENT))));
  }

  @Test
  public void readWriteTransactionBuffer() throws InterruptedException {
    TransactionRunner runner = client.readWriteTransaction();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            runner.run(
                transaction -> {
                  transaction.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
                  return null;
                }));
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
                    while (rs.next()) {}
                  }
                  if (attempt.get() == 1) {
                    invalidateSessionPool(
                        client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
                  }
                  try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                    while (rs.next()) {}
                  }
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
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
                    while (rs.next()) {}
                  }
                  if (attempt.get() == 1) {
                    invalidateSessionPool(
                        client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
                  }
                  try (ResultSet rs =
                      transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
                    while (rs.next()) {}
                  }
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
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
                    while (rs.next()) {}
                  }
                  if (attempt.get() == 1) {
                    invalidateSessionPool(
                        client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
                  }
                  try (ResultSet rs =
                      transaction.readUsingIndex(
                          "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
                    while (rs.next()) {}
                  }
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
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
                  assertThat(row.getLong(0)).isEqualTo(1L);
                  if (attempt.get() == 1) {
                    invalidateSessionPool(
                        client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
                  }
                  transaction.readRow("FOO", Key.of(), Collections.singletonList("BAR"));
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
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
                  assertThat(row.getLong(0)).isEqualTo(1L);
                  if (attempt.get() == 1) {
                    invalidateSessionPool(
                        client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
                  }
                  transaction.readRowUsingIndex(
                      "FOO", "IDX", Key.of(), Collections.singletonList("BAR"));
                  assertThat(attempt.get()).isGreaterThan(1);
                  return null;
                }));
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadOnlySessionInPool() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerSelect() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerRead() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs =
              transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
            assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadUsingIndex() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs =
              transaction.readUsingIndex(
                  "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
            assertThrowsSessionNotFoundIfShouldFail(() -> rs.next());
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @Test
  public void transactionManagerReadRow() throws InterruptedException {
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
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @Test
  public void transactionManagerReadRowUsingIndex() throws InterruptedException {
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
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @Test
  public void transactionManagerUpdate() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager(Options.commitStats())) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          TransactionContext context = transaction;
          assertThrowsSessionNotFoundIfShouldFail(() -> context.executeUpdate(UPDATE_STATEMENT));
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @Test
  public void transactionManagerAborted_thenSessionNotFoundOnBeginTransaction()
      throws InterruptedException {
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
            invalidateSessionPool(
                client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
          }
          TransactionContext context = transaction;
          assertThrowsSessionNotFoundIfShouldFail(() -> context.executeUpdate(UPDATE_STATEMENT));
          manager.commit();
          // The actual number of attempts depends on when the transaction manager will actually get
          // a valid session, as we invalidate the entire session pool.
          assertThat(attempt).isAtLeast(3);
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @Test
  public void transactionManagerBatchUpdate() throws InterruptedException {
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
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          if (transaction == null) {
            break;
          }
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerBuffer() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        transaction.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
        try {
          manager.commit();
          break;
        } catch (AbortedException e) {
          transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
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
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerSelectInvalidatedDuringTransaction() throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager manager = client.transactionManager()) {
        int attempts = 0;
        TransactionContext transaction = manager.begin();
        while (true) {
          attempts++;
          try {
            try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
              while (rs.next()) {}
            }
            if (attempts == 1) {
              invalidateSessionPool(
                  client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
            }
            try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
              if (assertThrowsSessionNotFoundIfShouldFail(() -> rs.next()) == null) {
                break;
              }
            }
            manager.commit();
            assertThat(attempts).isGreaterThan(1);
            break;
          } catch (AbortedException e) {
            transaction = assertThrowsSessionNotFoundIfShouldFail(() -> manager.resetForRetry());
          }
        }
      }
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadInvalidatedDuringTransaction() throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager manager = client.transactionManager()) {
        int attempts = 0;
        TransactionContext transaction = manager.begin();
        while (true) {
          attempts++;
          try {
            try (ResultSet rs =
                transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
              while (rs.next()) {}
            }
            if (attempts == 1) {
              invalidateSessionPool(
                  client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
            }
            try (ResultSet rs =
                transaction.read("FOO", KeySet.all(), Collections.singletonList("BAR"))) {
              if (assertThrowsSessionNotFoundIfShouldFail(() -> rs.next()) == null) {
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
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadUsingIndexInvalidatedDuringTransaction()
      throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager manager = client.transactionManager()) {
        int attempts = 0;
        TransactionContext transaction = manager.begin();
        while (true) {
          attempts++;
          try {
            try (ResultSet rs =
                transaction.readUsingIndex(
                    "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
              while (rs.next()) {}
            }
            if (attempts == 1) {
              invalidateSessionPool(
                  client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
            }
            try (ResultSet rs =
                transaction.readUsingIndex(
                    "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR"))) {
              if (assertThrowsSessionNotFoundIfShouldFail(() -> rs.next()) == null) {
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
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRowInvalidatedDuringTransaction() throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager manager = client.transactionManager()) {
        int attempts = 0;
        TransactionContext transaction = manager.begin();
        while (true) {
          attempts++;
          try {
            Struct row = transaction.readRow("FOO", Key.of(), Collections.singletonList("BAR"));
            assertThat(row.getLong(0)).isEqualTo(1L);
            if (attempts == 1) {
              invalidateSessionPool(
                  client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
            }
            TransactionContext context = transaction;
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
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRowUsingIndexInvalidatedDuringTransaction()
      throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager manager = client.transactionManager()) {
        int attempts = 0;
        TransactionContext transaction = manager.begin();
        while (true) {
          attempts++;
          try {
            Struct row =
                transaction.readRowUsingIndex(
                    "FOO", "IDX", Key.of(), Collections.singletonList("BAR"));
            assertThat(row.getLong(0)).isEqualTo(1L);
            if (attempts == 1) {
              invalidateSessionPool(
                  client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
            }
            TransactionContext context = transaction;
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
    }
  }

  @Test
  public void partitionedDml() throws InterruptedException {
    assertThrowsSessionNotFoundIfShouldFail(
        () -> client.executePartitionedUpdate(UPDATE_STATEMENT));
  }

  @Test
  public void write() throws InterruptedException {
    assertThrowsSessionNotFoundIfShouldFail(
        () -> client.write(Collections.singletonList(Mutation.delete("FOO", KeySet.all()))));
  }

  @Test
  public void writeAtLeastOnce() throws InterruptedException {
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            client.writeAtLeastOnce(
                Collections.singletonList(Mutation.delete("FOO", KeySet.all()))));
  }

  @Test
  public void asyncRunnerSelect() throws InterruptedException {
    asyncRunner_withReadFunction(input -> input.executeQueryAsync(SELECT1AND2));
  }

  @Test
  public void asyncRunnerRead() throws InterruptedException {
    asyncRunner_withReadFunction(
        input -> input.readAsync("FOO", KeySet.all(), Collections.singletonList("BAR")));
  }

  @Test
  public void asyncRunnerReadUsingIndex() throws InterruptedException {
    asyncRunner_withReadFunction(
        input ->
            input.readUsingIndexAsync(
                "FOO", "IDX", KeySet.all(), Collections.singletonList("BAR")));
  }

  private void asyncRunner_withReadFunction(
      final Function<TransactionContext, AsyncResultSet> readFunction) throws InterruptedException {
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
  public void asyncRunnerReadRow() throws InterruptedException {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            get(
                runner.runAsync(
                    txn -> txn.readRowAsync("FOO", Key.of(), Collections.singletonList("BAR")),
                    executor)));
  }

  @Test
  public void asyncRunnerReadRowUsingIndex() throws InterruptedException {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            get(
                runner.runAsync(
                    txn ->
                        txn.readRowUsingIndexAsync(
                            "FOO", "IDX", Key.of(), Collections.singletonList("BAR")),
                    executor)));
  }

  @Test
  public void asyncRunnerUpdate() throws InterruptedException {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () -> get(runner.runAsync(txn -> txn.executeUpdateAsync(UPDATE_STATEMENT), executor)));
  }

  @Test
  public void asyncRunnerBatchUpdate() throws InterruptedException {
    AsyncRunner runner = client.runAsync();
    assertThrowsSessionNotFoundIfShouldFail(
        () ->
            get(
                runner.runAsync(
                    txn -> txn.batchUpdateAsync(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)),
                    executor)));
  }

  @Test
  public void asyncRunnerBuffer() throws InterruptedException {
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
  }

  @Test
  public void asyncTransactionManagerAsyncSelect() throws InterruptedException {
    asyncTransactionManager_readAsync(input -> input.executeQueryAsync(SELECT1AND2));
  }

  @Test
  public void asyncTransactionManagerAsyncRead() throws InterruptedException {
    asyncTransactionManager_readAsync(
        input -> input.readAsync("FOO", KeySet.all(), Collections.singletonList("BAR")));
  }

  @Test
  public void asyncTransactionManagerAsyncReadUsingIndex() throws InterruptedException {
    asyncTransactionManager_readAsync(
        input ->
            input.readUsingIndexAsync(
                "FOO", "idx", KeySet.all(), Collections.singletonList("BAR")));
  }

  private void asyncTransactionManager_readAsync(
      final Function<TransactionContext, AsyncResultSet> fn) throws InterruptedException {
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
  public void asyncTransactionManagerSelect() throws InterruptedException {
    asyncTransactionManager_readSync(input -> input.executeQuery(SELECT1AND2));
  }

  @Test
  public void asyncTransactionManagerRead() throws InterruptedException {
    asyncTransactionManager_readSync(
        input -> input.read("FOO", KeySet.all(), Collections.singletonList("BAR")));
  }

  @Test
  public void asyncTransactionManagerReadUsingIndex() throws InterruptedException {
    asyncTransactionManager_readSync(
        input ->
            input.readUsingIndex("FOO", "idx", KeySet.all(), Collections.singletonList("BAR")));
  }

  private void asyncTransactionManager_readSync(final Function<TransactionContext, ResultSet> fn)
      throws InterruptedException {
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
  public void asyncTransactionManagerReadRow() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        input ->
            ApiFutures.immediateFuture(
                input.readRow("FOO", Key.of("foo"), Collections.singletonList("BAR"))));
  }

  @Test
  public void asyncTransactionManagerReadRowUsingIndex() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        input ->
            ApiFutures.immediateFuture(
                input.readRowUsingIndex(
                    "FOO", "idx", Key.of("foo"), Collections.singletonList("BAR"))));
  }

  @Test
  public void asyncTransactionManagerReadRowAsync() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        input -> input.readRowAsync("FOO", Key.of("foo"), Collections.singletonList("BAR")));
  }

  @Test
  public void asyncTransactionManagerReadRowUsingIndexAsync() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        input ->
            input.readRowUsingIndexAsync(
                "FOO", "idx", Key.of("foo"), Collections.singletonList("BAR")));
  }

  private void asyncTransactionManager_readRowFunction(
      final Function<TransactionContext, ApiFuture<Struct>> fn) throws InterruptedException {
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
  public void asyncTransactionManagerUpdateAsync() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        input -> input.executeUpdateAsync(UPDATE_STATEMENT), UPDATE_COUNT);
  }

  @Test
  public void asyncTransactionManagerUpdate() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        input -> ApiFutures.immediateFuture(input.executeUpdate(UPDATE_STATEMENT)), UPDATE_COUNT);
  }

  @Test
  public void asyncTransactionManagerBatchUpdateAsync() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        input -> input.batchUpdateAsync(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)),
        new long[] {UPDATE_COUNT, UPDATE_COUNT});
  }

  @Test
  public void asyncTransactionManagerBatchUpdate() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        input ->
            ApiFutures.immediateFuture(
                input.batchUpdate(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT))),
        new long[] {UPDATE_COUNT, UPDATE_COUNT});
  }

  private <T> void asyncTransactionManager_updateFunction(
      final Function<TransactionContext, ApiFuture<T>> fn, T expected) throws InterruptedException {
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
