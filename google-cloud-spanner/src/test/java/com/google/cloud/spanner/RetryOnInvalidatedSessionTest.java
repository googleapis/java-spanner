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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionFunction;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionStep;
import com.google.cloud.spanner.AsyncTransactionManager.CommitTimestampFuture;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
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
        StatementResult.read("FOO", KeySet.all(), Arrays.asList("BAR"), READ_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.read(
            "FOO", KeySet.singleKey(Key.of()), Arrays.asList("BAR"), READ_ROW_RESULTSET));
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
  public void setUp() {
    mockSpanner.reset();
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder().setFailOnSessionLeak();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
  }

  @After
  public void tearDown() {
    spanner.close();
  }

  private static void invalidateSessionPool() throws InterruptedException {
    invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
  }

  private static void invalidateSessionPool(DatabaseClient client, int minSessions)
      throws InterruptedException {
    // Wait for all sessions to have been created, and then delete them.
    Stopwatch watch = Stopwatch.createStarted();
    while (((DatabaseClientImpl) client).pool.totalSessions() < minSessions) {
      if (watch.elapsed(TimeUnit.SECONDS) > 5L) {
        fail(String.format("Failed to create MinSessions=%d", minSessions));
      }
      Thread.sleep(5L);
    }

    ListSessionsPagedResponse response =
        spannerClient.listSessions("projects/[PROJECT]/instances/[INSTANCE]/databases/[DATABASE]");
    for (com.google.spanner.v1.Session session : response.iterateAll()) {
      spannerClient.deleteSession(session.getName());
    }
  }

  @Test
  public void singleUseSelect() throws InterruptedException {
    invalidateSessionPool();
    try {
      // This call will receive an invalidated session that will be replaced on the first call to
      // rs.next().
      int count = 0;
      try (ReadContext context = client.singleUse()) {
        try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
          while (rs.next()) {
            count++;
          }
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseSelectAsync() throws Exception {
    invalidateSessionPool();
    ApiFuture<List<Long>> list;
    try (AsyncResultSet rs = client.singleUse().executeQueryAsync(SELECT1AND2)) {
      list = rs.toListAsync(TO_LONG, executor);
      assertThat(list.get()).containsExactly(1L, 2L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SessionNotFoundException.class);
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseRead() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseReadUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.singleUse()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseReadRow() throws InterruptedException {
    invalidateSessionPool();
    try (ReadContext context = client.singleUse()) {
      Struct row = context.readRow("FOO", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseReadRowUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try (ReadContext context = client.singleUse()) {
      Struct row = context.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseReadOnlyTransactionSelect() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseReadOnlyTransactionRead() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singlUseReadOnlyTransactionReadUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseReadOnlyTransactionReadRow() throws InterruptedException {
    invalidateSessionPool();
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      Struct row = context.readRow("FOO", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void singleUseReadOnlyTransactionReadRowUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try (ReadContext context = client.singleUseReadOnlyTransaction()) {
      Struct row = context.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readOnlyTransactionSelect() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readOnlyTransactionRead() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readOnlyTransactionReadUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    int count = 0;
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readOnlyTransactionReadRow() throws InterruptedException {
    invalidateSessionPool();
    try (ReadContext context = client.readOnlyTransaction()) {
      Struct row = context.readRow("FOO", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readOnlyTransactionReadRowUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try (ReadContext context = client.readOnlyTransaction()) {
      Struct row = context.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test(expected = SessionNotFoundException.class)
  public void readOnlyTransactionSelectNonRecoverable() throws InterruptedException {
    int count = 0;
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      // Invalidate the session pool while in a transaction. This is not recoverable.
      invalidateSessionPool();
      try (ResultSet rs = context.executeQuery(SELECT1AND2)) {
        while (rs.next()) {
          count++;
        }
      }
    }
  }

  @Test(expected = SessionNotFoundException.class)
  public void readOnlyTransactionReadNonRecoverable() throws InterruptedException {
    int count = 0;
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs = context.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      invalidateSessionPool();
      try (ResultSet rs = context.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
    }
  }

  @Test(expected = SessionNotFoundException.class)
  public void readOnlyTransactionReadUsingIndexNonRecoverable() throws InterruptedException {
    int count = 0;
    try (ReadContext context = client.readOnlyTransaction()) {
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
      assertThat(count).isEqualTo(2);
      invalidateSessionPool();
      try (ResultSet rs =
          context.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
        while (rs.next()) {
          count++;
        }
      }
    }
  }

  @Test(expected = SessionNotFoundException.class)
  public void readOnlyTransactionReadRowNonRecoverable() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      Struct row = context.readRow("FOO", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      invalidateSessionPool();
      row = context.readRow("FOO", Key.of(), Arrays.asList("BAR"));
    }
  }

  @Test(expected = SessionNotFoundException.class)
  public void readOnlyTransactionReadRowUsingIndexNonRecoverable() throws InterruptedException {
    try (ReadContext context = client.readOnlyTransaction()) {
      Struct row = context.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
      assertThat(row.getLong(0)).isEqualTo(1L);
      invalidateSessionPool();
      row = context.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
    }
  }

  @Test
  public void readWriteTransactionReadOnlySessionInPool() throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int count =
          runner.run(
              transaction -> {
                int count1 = 0;
                try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                  while (rs.next()) {
                    count1++;
                  }
                }
                return count1;
              });
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionSelect() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int count =
          runner.run(
              transaction -> {
                int count1 = 0;
                try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                  while (rs.next()) {
                    count1++;
                  }
                }
                return count1;
              });
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionRead() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int count =
          runner.run(
              transaction -> {
                int count1 = 0;
                try (ResultSet rs = transaction.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
                  while (rs.next()) {
                    count1++;
                  }
                }
                return count1;
              });
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionReadUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int count =
          runner.run(
              transaction -> {
                int count1 = 0;
                try (ResultSet rs =
                    transaction.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
                  while (rs.next()) {
                    count1++;
                  }
                }
                return count1;
              });
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionReadRow() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      Struct row =
          runner.run(transaction -> transaction.readRow("FOO", Key.of(), Arrays.asList("BAR")));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionReadRowUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      Struct row =
          runner.run(
              transaction ->
                  transaction.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR")));
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionUpdate() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      long count = runner.run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
      assertThat(count).isEqualTo(UPDATE_COUNT);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionBatchUpdate() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      long[] count =
          runner.run(transaction -> transaction.batchUpdate(Arrays.asList(UPDATE_STATEMENT)));
      assertThat(count.length).isEqualTo(1);
      assertThat(count[0]).isEqualTo(UPDATE_COUNT);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionBuffer() throws InterruptedException {
    invalidateSessionPool();
    try {
      TransactionRunner runner = client.readWriteTransaction();
      runner.run(
          transaction -> {
            transaction.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
            return null;
          });
      assertThat(runner.getCommitTimestamp()).isNotNull();
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionSelectInvalidatedDuringTransaction() {
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int attempts =
          runner.run(
              new TransactionCallable<Integer>() {
                private int attempt = 0;

                @Override
                public Integer run(TransactionContext transaction) throws Exception {
                  attempt++;
                  int count = 0;
                  try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                    while (rs.next()) {
                      count++;
                    }
                  }
                  assertThat(count).isEqualTo(2);
                  if (attempt == 1) {
                    invalidateSessionPool();
                  }
                  try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
                    while (rs.next()) {
                      count++;
                    }
                  }
                  return attempt;
                }
              });
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionReadInvalidatedDuringTransaction() {
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int attempts =
          runner.run(
              new TransactionCallable<Integer>() {
                private int attempt = 0;

                @Override
                public Integer run(TransactionContext transaction) throws Exception {
                  attempt++;
                  int count = 0;
                  try (ResultSet rs = transaction.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
                    while (rs.next()) {
                      count++;
                    }
                  }
                  assertThat(count).isEqualTo(2);
                  if (attempt == 1) {
                    invalidateSessionPool();
                  }
                  try (ResultSet rs = transaction.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
                    while (rs.next()) {
                      count++;
                    }
                  }
                  return attempt;
                }
              });
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionReadUsingIndexInvalidatedDuringTransaction() {
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int attempts =
          runner.run(
              new TransactionCallable<Integer>() {
                private int attempt = 0;

                @Override
                public Integer run(TransactionContext transaction) throws Exception {
                  attempt++;
                  int count = 0;
                  try (ResultSet rs =
                      transaction.readUsingIndex(
                          "FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
                    while (rs.next()) {
                      count++;
                    }
                  }
                  assertThat(count).isEqualTo(2);
                  if (attempt == 1) {
                    invalidateSessionPool();
                  }
                  try (ResultSet rs =
                      transaction.readUsingIndex(
                          "FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
                    while (rs.next()) {
                      count++;
                    }
                  }
                  return attempt;
                }
              });
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionReadRowInvalidatedDuringTransaction() {
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int attempts =
          runner.run(
              new TransactionCallable<Integer>() {
                private int attempt = 0;

                @Override
                public Integer run(TransactionContext transaction) throws Exception {
                  attempt++;
                  Struct row = transaction.readRow("FOO", Key.of(), Arrays.asList("BAR"));
                  assertThat(row.getLong(0)).isEqualTo(1L);
                  if (attempt == 1) {
                    invalidateSessionPool();
                  }
                  row = transaction.readRow("FOO", Key.of(), Arrays.asList("BAR"));
                  return attempt;
                }
              });
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void readWriteTransactionReadRowUsingIndexInvalidatedDuringTransaction() {
    try {
      TransactionRunner runner = client.readWriteTransaction();
      int attempts =
          runner.run(
              new TransactionCallable<Integer>() {
                private int attempt = 0;

                @Override
                public Integer run(TransactionContext transaction) throws Exception {
                  attempt++;
                  Struct row =
                      transaction.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
                  assertThat(row.getLong(0)).isEqualTo(1L);
                  if (attempt == 1) {
                    invalidateSessionPool();
                  }
                  row = transaction.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
                  return attempt;
                }
              });
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  /**
   * Test with one read-only session in the pool that is invalidated. The session pool will try to
   * prepare this session for read/write, which will fail with a {@link SessionNotFoundException}.
   * That again will trigger the creation of a new session. This will always succeed.
   */
  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadOnlySessionInPool() throws InterruptedException {
    SessionPoolOptions.Builder builder = SessionPoolOptions.newBuilder();
    if (failOnInvalidatedSession) {
      builder.setFailIfSessionNotFound();
    }
    Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setSessionPoolOption(builder.build())
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    invalidateSessionPool(client, spanner.getOptions().getSessionPoolOptions().getMinSessions());
    int count = 0;
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            while (rs.next()) {
              count++;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerSelect() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager()) {
      int count = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            while (rs.next()) {
              count++;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerRead() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager()) {
      int count = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs = transaction.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
            while (rs.next()) {
              count++;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager()) {
      int count = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          try (ResultSet rs =
              transaction.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
            while (rs.next()) {
              count++;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(count).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRow() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager()) {
      Struct row;
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          row = transaction.readRow("FOO", Key.of(), Arrays.asList("BAR"));
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRowUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager()) {
      Struct row;
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          row = transaction.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(row.getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerUpdate() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager(Options.commitStats())) {
      long count;
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          count = transaction.executeUpdate(UPDATE_STATEMENT);
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertEquals(UPDATE_COUNT, count);
      assertNotNull(manager.getCommitResponse().getCommitStats());
      assertFalse(failOnInvalidatedSession);
    } catch (SessionNotFoundException e) {
      assertTrue(failOnInvalidatedSession);
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerAborted_thenSessionNotFoundOnBeginTransaction()
      throws InterruptedException {
    int attempt = 0;
    try (TransactionManager manager = client.transactionManager()) {
      long count;
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
          count = transaction.executeUpdate(UPDATE_STATEMENT);
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(count).isEqualTo(UPDATE_COUNT);
      assertThat(failOnInvalidatedSession).isFalse();
      // The actual number of attempts depends on when the transaction manager will actually get a
      // valid session, as we invalidate the entire session pool.
      assertThat(attempt).isAtLeast(3);
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerBatchUpdate() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager()) {
      long[] count;
      TransactionContext transaction = manager.begin();
      while (true) {
        try {
          count = transaction.batchUpdate(Arrays.asList(UPDATE_STATEMENT));
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(count.length).isEqualTo(1);
      assertThat(count[0]).isEqualTo(UPDATE_COUNT);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerBuffer() throws InterruptedException {
    invalidateSessionPool();
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext transaction = manager.begin();
      while (true) {
        transaction.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
        try {
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
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
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        int count = 0;
        try {
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            while (rs.next()) {
              count++;
            }
          }
          assertThat(count).isEqualTo(2);
          if (attempts == 1) {
            invalidateSessionPool();
          }
          try (ResultSet rs = transaction.executeQuery(SELECT1AND2)) {
            while (rs.next()) {
              count++;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadInvalidatedDuringTransaction() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        int count = 0;
        try {
          try (ResultSet rs = transaction.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
            while (rs.next()) {
              count++;
            }
          }
          assertThat(count).isEqualTo(2);
          if (attempts == 1) {
            invalidateSessionPool();
          }
          try (ResultSet rs = transaction.read("FOO", KeySet.all(), Arrays.asList("BAR"))) {
            while (rs.next()) {
              count++;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadUsingIndexInvalidatedDuringTransaction()
      throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        int count = 0;
        try {
          try (ResultSet rs =
              transaction.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
            while (rs.next()) {
              count++;
            }
          }
          assertThat(count).isEqualTo(2);
          if (attempts == 1) {
            invalidateSessionPool();
          }
          try (ResultSet rs =
              transaction.readUsingIndex("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"))) {
            while (rs.next()) {
              count++;
            }
          }
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRowInvalidatedDuringTransaction() throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        try {
          Struct row = transaction.readRow("FOO", Key.of(), Arrays.asList("BAR"));
          assertThat(row.getLong(0)).isEqualTo(1L);
          if (attempts == 1) {
            invalidateSessionPool();
          }
          row = transaction.readRow("FOO", Key.of(), Arrays.asList("BAR"));
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @SuppressWarnings("resource")
  @Test
  public void transactionManagerReadRowUsingIndexInvalidatedDuringTransaction()
      throws InterruptedException {
    try (TransactionManager manager = client.transactionManager()) {
      int attempts = 0;
      TransactionContext transaction = manager.begin();
      while (true) {
        attempts++;
        try {
          Struct row = transaction.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
          assertThat(row.getLong(0)).isEqualTo(1L);
          if (attempts == 1) {
            invalidateSessionPool();
          }
          row = transaction.readRowUsingIndex("FOO", "IDX", Key.of(), Arrays.asList("BAR"));
          manager.commit();
          break;
        } catch (AbortedException e) {
          Thread.sleep(e.getRetryDelayInMillis());
          transaction = manager.resetForRetry();
        }
      }
      assertThat(attempts).isGreaterThan(1);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void partitionedDml() throws InterruptedException {
    invalidateSessionPool();
    try {
      assertThat(client.executePartitionedUpdate(UPDATE_STATEMENT)).isEqualTo(UPDATE_COUNT);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void write() throws InterruptedException {
    invalidateSessionPool();
    try {
      Timestamp timestamp = client.write(Arrays.asList(Mutation.delete("FOO", KeySet.all())));
      assertThat(timestamp).isNotNull();
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void writeAtLeastOnce() throws InterruptedException {
    invalidateSessionPool();
    try {
      Timestamp timestamp =
          client.writeAtLeastOnce(Arrays.asList(Mutation.delete("FOO", KeySet.all())));
      assertThat(timestamp).isNotNull();
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void asyncRunnerSelect() throws InterruptedException {
    asyncRunner_withReadFunction(
        new Function<TransactionContext, AsyncResultSet>() {
          @Override
          public AsyncResultSet apply(TransactionContext input) {
            return input.executeQueryAsync(SELECT1AND2);
          }
        });
  }

  @Test
  public void asyncRunnerRead() throws InterruptedException {
    asyncRunner_withReadFunction(
        new Function<TransactionContext, AsyncResultSet>() {
          @Override
          public AsyncResultSet apply(TransactionContext input) {
            return input.readAsync("FOO", KeySet.all(), Arrays.asList("BAR"));
          }
        });
  }

  @Test
  public void asyncRunnerReadUsingIndex() throws InterruptedException {
    asyncRunner_withReadFunction(
        new Function<TransactionContext, AsyncResultSet>() {
          @Override
          public AsyncResultSet apply(TransactionContext input) {
            return input.readUsingIndexAsync("FOO", "IDX", KeySet.all(), Arrays.asList("BAR"));
          }
        });
  }

  private void asyncRunner_withReadFunction(
      final Function<TransactionContext, AsyncResultSet> readFunction) throws InterruptedException {
    invalidateSessionPool();
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try {
      AsyncRunner runner = client.runAsync();
      final AtomicLong counter = new AtomicLong();
      ApiFuture<Long> count =
          runner.runAsync(
              txn -> {
                AsyncResultSet rs = readFunction.apply(txn);
                ApiFuture<Void> fut =
                    rs.setCallback(
                        queryExecutor,
                        new ReadyCallback() {
                          @Override
                          public CallbackResponse cursorReady(AsyncResultSet resultSet) {
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
                          }
                        });
                return ApiFutures.transform(
                    fut,
                    new ApiFunction<Void, Long>() {
                      @Override
                      public Long apply(Void input) {
                        return counter.get();
                      }
                    },
                    MoreExecutors.directExecutor());
              },
              executor);
      assertThat(get(count)).isEqualTo(2);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncRunnerReadRow() throws InterruptedException {
    invalidateSessionPool();
    try {
      AsyncRunner runner = client.runAsync();
      ApiFuture<Struct> row =
          runner.runAsync(txn -> txn.readRowAsync("FOO", Key.of(), Arrays.asList("BAR")), executor);
      assertThat(get(row).getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void asyncRunnerReadRowUsingIndex() throws InterruptedException {
    invalidateSessionPool();
    try {
      AsyncRunner runner = client.runAsync();
      ApiFuture<Struct> row =
          runner.runAsync(
              txn -> txn.readRowUsingIndexAsync("FOO", "IDX", Key.of(), Arrays.asList("BAR")),
              executor);
      assertThat(get(row).getLong(0)).isEqualTo(1L);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void asyncRunnerUpdate() throws InterruptedException {
    invalidateSessionPool();
    try {
      AsyncRunner runner = client.runAsync();
      ApiFuture<Long> count =
          runner.runAsync(txn -> txn.executeUpdateAsync(UPDATE_STATEMENT), executor);
      assertThat(get(count)).isEqualTo(UPDATE_COUNT);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void asyncRunnerBatchUpdate() throws InterruptedException {
    invalidateSessionPool();
    try {
      AsyncRunner runner = client.runAsync();
      ApiFuture<long[]> count =
          runner.runAsync(
              txn -> txn.batchUpdateAsync(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)),
              executor);
      assertThat(get(count)).hasLength(2);
      assertThat(get(count)).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void asyncRunnerBuffer() throws InterruptedException {
    invalidateSessionPool();
    try {
      AsyncRunner runner = client.runAsync();
      ApiFuture<Void> res =
          runner.runAsync(
              txn -> {
                txn.buffer(Mutation.newInsertBuilder("FOO").set("BAR").to(1L).build());
                return ApiFutures.immediateFuture(null);
              },
              executor);
      assertThat(get(res)).isNull();
      assertThat(get(runner.getCommitTimestamp())).isNotNull();
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }

  @Test
  public void asyncTransactionManagerAsyncSelect() throws InterruptedException {
    asyncTransactionManager_readAsync(
        new Function<TransactionContext, AsyncResultSet>() {
          @Override
          public AsyncResultSet apply(TransactionContext input) {
            return input.executeQueryAsync(SELECT1AND2);
          }
        });
  }

  @Test
  public void asyncTransactionManagerAsyncRead() throws InterruptedException {
    asyncTransactionManager_readAsync(
        new Function<TransactionContext, AsyncResultSet>() {
          @Override
          public AsyncResultSet apply(TransactionContext input) {
            return input.readAsync("FOO", KeySet.all(), Arrays.asList("BAR"));
          }
        });
  }

  @Test
  public void asyncTransactionManagerAsyncReadUsingIndex() throws InterruptedException {
    asyncTransactionManager_readAsync(
        new Function<TransactionContext, AsyncResultSet>() {
          @Override
          public AsyncResultSet apply(TransactionContext input) {
            return input.readUsingIndexAsync("FOO", "idx", KeySet.all(), Arrays.asList("BAR"));
          }
        });
  }

  private void asyncTransactionManager_readAsync(
      final Function<TransactionContext, AsyncResultSet> fn) throws InterruptedException {
    invalidateSessionPool();
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture context = manager.beginAsync();
      while (true) {
        try {
          final AtomicLong counter = new AtomicLong();
          AsyncTransactionStep<Void, Long> count =
              context.then(
                  new AsyncTransactionFunction<Void, Long>() {
                    @Override
                    public ApiFuture<Long> apply(TransactionContext txn, Void input)
                        throws Exception {
                      AsyncResultSet rs = fn.apply(txn);
                      ApiFuture<Void> fut =
                          rs.setCallback(
                              queryExecutor,
                              new ReadyCallback() {
                                @Override
                                public CallbackResponse cursorReady(AsyncResultSet resultSet) {
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
                                }
                              });
                      return ApiFutures.transform(
                          fut,
                          new ApiFunction<Void, Long>() {
                            @Override
                            public Long apply(Void input) {
                              return counter.get();
                            }
                          },
                          MoreExecutors.directExecutor());
                    }
                  },
                  executor);
          CommitTimestampFuture ts = count.commitAsync();
          assertThat(get(ts)).isNotNull();
          assertThat(get(count)).isEqualTo(2);
          assertThat(failOnInvalidatedSession).isFalse();
          break;
        } catch (AbortedException e) {
          context = manager.resetForRetryAsync();
        }
      }
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncTransactionManagerSelect() throws InterruptedException {
    asyncTransactionManager_readSync(
        new Function<TransactionContext, ResultSet>() {
          @Override
          public ResultSet apply(TransactionContext input) {
            return input.executeQuery(SELECT1AND2);
          }
        });
  }

  @Test
  public void asyncTransactionManagerRead() throws InterruptedException {
    asyncTransactionManager_readSync(
        new Function<TransactionContext, ResultSet>() {
          @Override
          public ResultSet apply(TransactionContext input) {
            return input.read("FOO", KeySet.all(), Arrays.asList("BAR"));
          }
        });
  }

  @Test
  public void asyncTransactionManagerReadUsingIndex() throws InterruptedException {
    asyncTransactionManager_readSync(
        new Function<TransactionContext, ResultSet>() {
          @Override
          public ResultSet apply(TransactionContext input) {
            return input.readUsingIndex("FOO", "idx", KeySet.all(), Arrays.asList("BAR"));
          }
        });
  }

  private void asyncTransactionManager_readSync(final Function<TransactionContext, ResultSet> fn)
      throws InterruptedException {
    invalidateSessionPool();
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture context = manager.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Void, Long> count =
              context.then(
                  new AsyncTransactionFunction<Void, Long>() {
                    @Override
                    public ApiFuture<Long> apply(TransactionContext txn, Void input)
                        throws Exception {
                      long counter = 0L;
                      try (ResultSet rs = fn.apply(txn)) {
                        while (rs.next()) {
                          counter++;
                        }
                      }
                      return ApiFutures.immediateFuture(counter);
                    }
                  },
                  executor);
          CommitTimestampFuture ts = count.commitAsync();
          assertThat(get(ts)).isNotNull();
          assertThat(get(count)).isEqualTo(2);
          assertThat(failOnInvalidatedSession).isFalse();
          break;
        } catch (AbortedException e) {
          context = manager.resetForRetryAsync();
        }
      }
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncTransactionManagerReadRow() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        new Function<TransactionContext, ApiFuture<Struct>>() {
          @Override
          public ApiFuture<Struct> apply(TransactionContext input) {
            return ApiFutures.immediateFuture(
                input.readRow("FOO", Key.of("foo"), Arrays.asList("BAR")));
          }
        });
  }

  @Test
  public void asyncTransactionManagerReadRowUsingIndex() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        new Function<TransactionContext, ApiFuture<Struct>>() {
          @Override
          public ApiFuture<Struct> apply(TransactionContext input) {
            return ApiFutures.immediateFuture(
                input.readRowUsingIndex("FOO", "idx", Key.of("foo"), Arrays.asList("BAR")));
          }
        });
  }

  @Test
  public void asyncTransactionManagerReadRowAsync() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        new Function<TransactionContext, ApiFuture<Struct>>() {
          @Override
          public ApiFuture<Struct> apply(TransactionContext input) {
            return input.readRowAsync("FOO", Key.of("foo"), Arrays.asList("BAR"));
          }
        });
  }

  @Test
  public void asyncTransactionManagerReadRowUsingIndexAsync() throws InterruptedException {
    asyncTransactionManager_readRowFunction(
        new Function<TransactionContext, ApiFuture<Struct>>() {
          @Override
          public ApiFuture<Struct> apply(TransactionContext input) {
            return input.readRowUsingIndexAsync("FOO", "idx", Key.of("foo"), Arrays.asList("BAR"));
          }
        });
  }

  private void asyncTransactionManager_readRowFunction(
      final Function<TransactionContext, ApiFuture<Struct>> fn) throws InterruptedException {
    invalidateSessionPool();
    final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture context = manager.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Void, Struct> row =
              context.then(
                  new AsyncTransactionFunction<Void, Struct>() {
                    @Override
                    public ApiFuture<Struct> apply(TransactionContext txn, Void input)
                        throws Exception {
                      return fn.apply(txn);
                    }
                  },
                  executor);
          CommitTimestampFuture ts = row.commitAsync();
          assertThat(get(ts)).isNotNull();
          assertThat(get(row)).isEqualTo(Struct.newBuilder().set("BAR").to(1L).build());
          assertThat(failOnInvalidatedSession).isFalse();
          break;
        } catch (AbortedException e) {
          context = manager.resetForRetryAsync();
        }
      }
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    } finally {
      queryExecutor.shutdown();
    }
  }

  @Test
  public void asyncTransactionManagerUpdateAsync() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        new Function<TransactionContext, ApiFuture<Long>>() {
          @Override
          public ApiFuture<Long> apply(TransactionContext input) {
            return input.executeUpdateAsync(UPDATE_STATEMENT);
          }
        },
        UPDATE_COUNT);
  }

  @Test
  public void asyncTransactionManagerUpdate() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        new Function<TransactionContext, ApiFuture<Long>>() {
          @Override
          public ApiFuture<Long> apply(TransactionContext input) {
            return ApiFutures.immediateFuture(input.executeUpdate(UPDATE_STATEMENT));
          }
        },
        UPDATE_COUNT);
  }

  @Test
  public void asyncTransactionManagerBatchUpdateAsync() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        new Function<TransactionContext, ApiFuture<long[]>>() {
          @Override
          public ApiFuture<long[]> apply(TransactionContext input) {
            return input.batchUpdateAsync(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT));
          }
        },
        new long[] {UPDATE_COUNT, UPDATE_COUNT});
  }

  @Test
  public void asyncTransactionManagerBatchUpdate() throws InterruptedException {
    asyncTransactionManager_updateFunction(
        new Function<TransactionContext, ApiFuture<long[]>>() {
          @Override
          public ApiFuture<long[]> apply(TransactionContext input) {
            return ApiFutures.immediateFuture(
                input.batchUpdate(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)));
          }
        },
        new long[] {UPDATE_COUNT, UPDATE_COUNT});
  }

  private <T> void asyncTransactionManager_updateFunction(
      final Function<TransactionContext, ApiFuture<T>> fn, T expected) throws InterruptedException {
    invalidateSessionPool();
    try (AsyncTransactionManager manager = client.transactionManagerAsync()) {
      TransactionContextFuture transaction = manager.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Void, T> res =
              transaction.then(
                  new AsyncTransactionFunction<Void, T>() {
                    @Override
                    public ApiFuture<T> apply(TransactionContext txn, Void input) throws Exception {
                      return fn.apply(txn);
                    }
                  },
                  executor);
          CommitTimestampFuture ts = res.commitAsync();
          assertThat(get(res)).isEqualTo(expected);
          assertThat(get(ts)).isNotNull();
          break;
        } catch (AbortedException e) {
          transaction = manager.resetForRetryAsync();
        }
      }
      assertThat(failOnInvalidatedSession).isFalse();
    } catch (SessionNotFoundException e) {
      assertThat(failOnInvalidatedSession).isTrue();
    }
  }
}
