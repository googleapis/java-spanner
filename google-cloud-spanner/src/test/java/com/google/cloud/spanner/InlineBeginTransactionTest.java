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

import static com.google.cloud.spanner.SpannerApiFutures.get;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionStep;
import com.google.cloud.spanner.AsyncTransactionManager.CommitTimestampFuture;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class InlineBeginTransactionTest {
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
  private static final Statement SELECT1_UNION_ALL_SELECT2 =
      Statement.of("SELECT 1 AS COL1 UNION ALL SELECT 2 AS COL1");
  private static final com.google.spanner.v1.ResultSet SELECT1_UNION_ALL_SELECT2_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                  .build())
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(SELECT1_METADATA)
          .build();
  private static final Statement INVALID_SELECT = Statement.of("SELECT * FROM NON_EXISTING_TABLE");
  private static final Statement READ_STATEMENT = Statement.of("SELECT ID FROM FOO WHERE 1=1");
  private static final Statement READ_ROW_STATEMENT =
      Statement.of("SELECT BAR FROM FOO WHERE ID=1");

  protected Spanner spanner;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT1_UNION_ALL_SELECT2, SELECT1_UNION_ALL_SELECT2_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.query(READ_STATEMENT, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.query(READ_ROW_STATEMENT, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT
                .withDescription("invalid update statement")
                .asRuntimeException()));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_SELECT,
            Status.INVALID_ARGUMENT
                .withDescription("invalid select statement")
                .asRuntimeException()));

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
  public void setUp() {
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    // Create a Spanner instance that will inline BeginTransaction calls. It also has no prepared
    // sessions in the pool to prevent session preparing from interfering with test cases.
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setTrackTransactionStarter()
            .build()
            .getService();
  }

  @After
  public void tearDown() {
    spanner.close();
    mockSpanner.reset();
    mockSpanner.clearRequests();
  }

  @RunWith(Parameterized.class)
  public static class InlineBeginTransactionWithExecutorTest extends InlineBeginTransactionTest {
    @Parameter public Executor executor;

    @Parameters(name = "executor = {0}")
    public static Collection<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {MoreExecutors.directExecutor()},
            {Executors.newSingleThreadExecutor()},
            {Executors.newFixedThreadPool(4)}
          });
    }

    @Test
    public void testInlinedBeginAsyncTx() throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      ApiFuture<Long> updateCount =
          client.runAsync().runAsync(txn -> txn.executeUpdateAsync(UPDATE_STATEMENT), executor);
      assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginAsyncTxAborted() throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      final AtomicBoolean firstAttempt = new AtomicBoolean(true);
      ApiFuture<Long> updateCount =
          client
              .runAsync()
              .runAsync(
                  txn -> {
                    ApiFuture<Long> res = txn.executeUpdateAsync(UPDATE_STATEMENT);
                    if (firstAttempt.getAndSet(false)) {
                      mockSpanner.abortTransaction(txn);
                    }
                    return res;
                  },
                  executor);
      assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      // We have started 2 transactions, because the first transaction aborted.
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testInlinedBeginAsyncTxWithQuery() throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
      ApiFuture<Long> updateCount =
          client
              .runAsync()
              .runAsync(
                  txn -> {
                    final SettableApiFuture<Long> res = SettableApiFuture.create();
                    try (AsyncResultSet rs = txn.executeQueryAsync(SELECT1)) {
                      rs.setCallback(
                          executor,
                          resultSet -> {
                            switch (resultSet.tryNext()) {
                              case DONE:
                                return CallbackResponse.DONE;
                              case NOT_READY:
                                return CallbackResponse.CONTINUE;
                              case OK:
                                res.set(resultSet.getLong(0));
                              default:
                                throw new IllegalStateException();
                            }
                          });
                    }
                    return res;
                  },
                  queryExecutor);
      assertThat(updateCount.get()).isEqualTo(1L);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
      queryExecutor.shutdown();
    }

    @Test
    public void testInlinedBeginAsyncTxWithBatchDml()
        throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      ApiFuture<long[]> updateCounts =
          client
              .runAsync()
              .runAsync(
                  transaction ->
                      transaction.batchUpdateAsync(
                          Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)),
                  executor);
      assertThat(updateCounts.get()).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginAsyncTxWithError() throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      ApiFuture<Long> updateCount =
          client
              .runAsync()
              .runAsync(
                  transaction -> {
                    transaction.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
                    return transaction.executeUpdateAsync(UPDATE_STATEMENT);
                  },
                  executor);
      assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
      // The first statement will fail and not return a transaction id. This will trigger a retry of
      // the entire transaction, and the retry will do an explicit BeginTransaction RPC.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      // The first update will start a transaction, but then fail the update statement. This will
      // start a transaction on the mock server, but that transaction will never be returned to the
      // client.
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testInlinedBeginAsyncTxWithOnlyMutations()
        throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      client
          .runAsync()
          .runAsync(
              transaction -> {
                transaction.buffer(Mutation.newInsertBuilder("FOO").set("ID").to(1L).build());
                return ApiFutures.immediateFuture(null);
              },
              executor)
          .get();
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testAsyncTransactionManagerInlinedBeginTx()
        throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (AsyncTransactionManager txMgr = client.transactionManagerAsync()) {
        TransactionContextFuture txn = txMgr.beginAsync();
        while (true) {
          AsyncTransactionStep<Void, Long> updateCount =
              txn.then(
                  (transaction, ignored) -> transaction.executeUpdateAsync(UPDATE_STATEMENT),
                  executor);
          CommitTimestampFuture commitTimestamp = updateCount.commitAsync();
          try {
            assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
            assertThat(commitTimestamp.get()).isNotNull();
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetryAsync();
          }
        }
      }
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testAsyncTransactionManagerInlinedBeginTxAborted()
        throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (AsyncTransactionManager txMgr = client.transactionManagerAsync()) {
        TransactionContextFuture txn = txMgr.beginAsync();
        boolean first = true;
        while (true) {
          try {
            AsyncTransactionStep<Void, Long> updateCount =
                txn.then(
                    (transaction, ignored) -> transaction.executeUpdateAsync(UPDATE_STATEMENT),
                    executor);
            if (first) {
              // Abort the transaction after the statement has been executed to ensure that the
              // transaction has actually been started before the test tries to abort it.
              updateCount.then(
                  (ignored1, ignored2) -> {
                    mockSpanner.abortAllTransactions();
                    return ApiFutures.immediateFuture(null);
                  },
                  MoreExecutors.directExecutor());
              first = false;
            }
            assertThat(updateCount.commitAsync().get()).isNotNull();
            assertThat(updateCount.get()).isEqualTo(UPDATE_COUNT);
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetryAsync();
          }
        }
      }
      // The retry will use a BeginTransaction RPC.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testAsyncTransactionManagerInlinedBeginTxWithOnlyMutations()
        throws InterruptedException, ExecutionException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (AsyncTransactionManager txMgr = client.transactionManagerAsync()) {
        TransactionContextFuture txn = txMgr.beginAsync();
        while (true) {
          try {
            txn.then(
                    (transaction, ignored) -> {
                      transaction.buffer(Mutation.newInsertBuilder("FOO").set("ID").to(1L).build());
                      return ApiFutures.immediateFuture(null);
                    },
                    executor)
                .commitAsync()
                .get();
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetryAsync();
          }
        }
      }
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testAsyncTransactionManagerInlinedBeginTxWithError() throws InterruptedException {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (AsyncTransactionManager txMgr = client.transactionManagerAsync()) {
        TransactionContextFuture txn = txMgr.beginAsync();
        while (true) {
          try {
            AsyncTransactionStep<Long, Long> updateCount =
                txn.then(
                        (transaction, ignored) ->
                            transaction.executeUpdateAsync(INVALID_UPDATE_STATEMENT),
                        executor)
                    .then(
                        (transaction, ignored) -> transaction.executeUpdateAsync(UPDATE_STATEMENT),
                        executor);
            SpannerException e =
                assertThrows(SpannerException.class, () -> get(updateCount.commitAsync()));
            assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetryAsync();
          }
        }
      }
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }
  }

  @RunWith(JUnit4.class)
  public static class InlineBeginTransactionWithoutExecutorTest extends InlineBeginTransactionTest {
    @Test
    public void testInlinedBeginTx() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT));
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxAborted() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      final AtomicBoolean firstAttempt = new AtomicBoolean(true);
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    long res = transaction.executeUpdate(UPDATE_STATEMENT);
                    if (firstAttempt.getAndSet(false)) {
                      mockSpanner.abortTransaction(transaction);
                    }
                    return res;
                  });
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
      // We have started 2 transactions, because the first transaction aborted during the commit.
      assertThat(countRequests(CommitRequest.class)).isEqualTo(2);
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testInlinedBeginFirstUpdateAborts() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  new TransactionCallable<Long>() {
                    boolean firstAttempt = true;

                    @Override
                    public Long run(TransactionContext transaction) {
                      if (firstAttempt) {
                        firstAttempt = false;
                        mockSpanner.putStatementResult(
                            StatementResult.exception(
                                UPDATE_STATEMENT,
                                mockSpanner.createAbortedException(
                                    ByteString.copyFromUtf8("some-tx"))));
                      } else {
                        mockSpanner.putStatementResult(
                            StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
                      }
                      return transaction.executeUpdate(UPDATE_STATEMENT);
                    }
                  });
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginFirstQueryAborts() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  new TransactionCallable<Long>() {
                    boolean firstAttempt = true;

                    @Override
                    public Long run(TransactionContext transaction) {
                      if (firstAttempt) {
                        firstAttempt = false;
                        mockSpanner.putStatementResult(
                            StatementResult.exception(
                                SELECT1,
                                mockSpanner.createAbortedException(
                                    ByteString.copyFromUtf8("some-tx"))));
                      } else {
                        mockSpanner.putStatementResult(
                            StatementResult.query(SELECT1, SELECT1_RESULTSET));
                      }
                      try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                        while (rs.next()) {
                          return rs.getLong(0);
                        }
                      }
                      return 0L;
                    }
                  });
      assertThat(updateCount).isEqualTo(1L);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginFirstQueryReturnsUnavailable() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofStreamException(Status.UNAVAILABLE.asRuntimeException(), 0));
      long value =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    // The first attempt will return UNAVAILABLE and retry internally.
                    try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                      while (rs.next()) {
                        return rs.getLong(0);
                      }
                    }
                    return 0L;
                  });
      assertThat(value).isEqualTo(1L);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginFirstReadReturnsUnavailable() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      mockSpanner.setStreamingReadExecutionTime(
          SimulatedExecutionTime.ofStreamException(Status.UNAVAILABLE.asRuntimeException(), 0));
      long value =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    // The first attempt will return UNAVAILABLE and retry internally.
                    try (ResultSet rs =
                        transaction.read("FOO", KeySet.all(), Collections.singletonList("ID"))) {
                      while (rs.next()) {
                        return rs.getLong(0);
                      }
                    }
                    return 0L;
                  });
      assertThat(value).isEqualTo(1L);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ReadRequest.class)).isEqualTo(2);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithQuery() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                      while (rs.next()) {
                        return rs.getLong(0);
                      }
                    }
                    return 0L;
                  });
      assertThat(updateCount).isEqualTo(1L);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithRead() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    try (ResultSet rs =
                        transaction.read("FOO", KeySet.all(), Collections.singletonList("ID"))) {
                      while (rs.next()) {
                        return rs.getLong(0);
                      }
                    }
                    return 0L;
                  });
      assertThat(updateCount).isEqualTo(1L);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ReadRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithBatchDml() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      long[] updateCounts =
          client
              .readWriteTransaction()
              .run(
                  transaction ->
                      transaction.batchUpdate(Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT)));
      assertThat(updateCounts).asList().containsExactly(UPDATE_COUNT, UPDATE_COUNT);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteBatchDmlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithError() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    SpannerException e =
                        assertThrows(
                            SpannerException.class,
                            () -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT));
                    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
                    return transaction.executeUpdate(UPDATE_STATEMENT);
                  });
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
      // The transaction will be retried because the first statement that also tried to include the
      // BeginTransaction statement failed and did not return a transaction. That forces a retry of
      // the entire transaction with an explicit BeginTransaction RPC.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      // The update statement will be executed 3 times:
      // 1. The invalid update statement will be executed during the first attempt and fail. The
      // second update statement will not be executed, as the transaction runner sees that the
      // initial
      // statement failed and did not return a valid transaction id.
      // 2. The invalid update statement is executed again during the retry.
      // 3. The valid update statement is only executed after the first statement succeeded.
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(3);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      // The first update will start a transaction, but then fail the update statement. This will
      // start a transaction on the mock server, but that transaction will never be returned to the
      // client.
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testInlinedBeginTxWithErrorOnFirstStatement_andThenErrorOnBeginTransaction() {
      mockSpanner.setBeginTransactionExecutionTime(
          SimulatedExecutionTime.ofException(
              Status.INTERNAL
                  .withDescription("Begin transaction failed due to an internal error")
                  .asRuntimeException()));
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      SpannerException outerException =
          assertThrows(
              SpannerException.class,
              () -> {
                client
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          SpannerException innerException =
                              assertThrows(
                                  SpannerException.class,
                                  () -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT));
                          assertEquals(ErrorCode.INVALID_ARGUMENT, innerException.getErrorCode());
                          return null;
                        });
              });
      assertEquals(ErrorCode.INTERNAL, outerException.getErrorCode());
      assertThat(outerException.getMessage())
          .contains("Begin transaction failed due to an internal error");
      // The transaction will be retried because the first statement that also tried to include the
      // BeginTransaction statement failed and did not return a transaction. That forces a retry of
      // the entire transaction with an explicit BeginTransaction RPC.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
      // The explicit BeginTransaction RPC failed, so only one transaction was started.
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithUncaughtError() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(transaction -> transaction.executeUpdate(INVALID_UPDATE_STATEMENT)));
      assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
      // The first update will start a transaction, but then fail the update statement. This will
      // start a transaction on the mock server, but that transaction will never be returned to the
      // client.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      // No rollback request will be initiated because the client does not receive any transaction
      // id.
      assertThat(countRequests(RollbackRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithUncaughtErrorAfterSuccessfulBegin() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction -> {
                            // This statement will start a transaction.
                            transaction.executeUpdate(UPDATE_STATEMENT);
                            // This statement will fail and cause a rollback as the exception is not
                            // caught.
                            return transaction.executeUpdate(INVALID_UPDATE_STATEMENT);
                          }));
      assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
      assertThat(countRequests(RollbackRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxBatchDmlWithErrorOnFirstStatement() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      Void res =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    SpannerBatchUpdateException e =
                        assertThrows(
                            SpannerBatchUpdateException.class,
                            () ->
                                transaction.batchUpdate(
                                    ImmutableList.of(INVALID_UPDATE_STATEMENT, UPDATE_STATEMENT)));
                    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
                    assertEquals(0, e.getUpdateCounts().length);
                    return null;
                  });
      assertThat(res).isNull();
      // The first statement failed and could not return a transaction. The entire transaction is
      // therefore retried with an explicit BeginTransaction RPC.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(ExecuteBatchDmlRequest.class)).isEqualTo(2);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testInlinedBeginTxBatchDmlWithErrorOnSecondStatement() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    SpannerBatchUpdateException e =
                        assertThrows(
                            SpannerBatchUpdateException.class,
                            () ->
                                transaction.batchUpdate(
                                    ImmutableList.of(UPDATE_STATEMENT, INVALID_UPDATE_STATEMENT)));
                    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
                    assertEquals(1, e.getUpdateCounts().length);
                    return e.getUpdateCounts()[0];
                  });
      assertThat(updateCount).isEqualTo(UPDATE_COUNT);
      // Although the batch DML returned an error, that error was for the second statement. That
      // means that the transaction was started by the first statement.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteBatchDmlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithErrorOnStreamingSql() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      Void res =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    try (ResultSet rs = transaction.executeQuery(INVALID_SELECT)) {
                      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
                      assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
                    }
                    return null;
                  });
      assertThat(res).isNull();
      // The transaction will be retried because the first statement that also tried to include the
      // BeginTransaction statement failed and did not return a transaction. That forces a retry of
      // the entire transaction with an explicit BeginTransaction RPC.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      // The first update will start a transaction, but then fail the update statement. This will
      // start a transaction on the mock server, but that transaction will never be returned to the
      // client.
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testInlinedBeginTxWithErrorOnSecondPartialResultSet() {
      final Statement statement = Statement.of("SELECT * FROM BROKEN_TABLE");
      RandomResultSetGenerator generator = new RandomResultSetGenerator(2);
      mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
      // The first PartialResultSet will be returned successfully, and then a DATA_LOSS exception
      // will be returned.
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofStreamException(Status.DATA_LOSS.asRuntimeException(), 1));
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      Void res =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    try (ResultSet rs = transaction.executeQuery(statement)) {
                      assertTrue(rs.next());
                      SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
                      assertEquals(ErrorCode.DATA_LOSS, e.getErrorCode());
                    }
                    return null;
                  });
      assertThat(res).isNull();
      // The transaction will not be retried, as the first PartialResultSet returns the transaction
      // ID, and the second fails with an error code.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithParallelQueries() {
      final int numQueries = 100;
      final ScheduledExecutorService executor = Executors.newScheduledThreadPool(16);
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  transaction -> {
                    List<Future<Long>> futures = new ArrayList<>(numQueries);
                    for (int i = 0; i < numQueries; i++) {
                      futures.add(
                          executor.submit(
                              () -> {
                                try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                                  while (rs.next()) {
                                    return rs.getLong(0);
                                  }
                                }
                                return 0L;
                              }));
                    }
                    Long res = 0L;
                    for (Future<Long> f : futures) {
                      res += f.get();
                    }
                    return res;
                  });
      assertThat(updateCount).isEqualTo((long) numQueries);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginTxWithOnlyMutations() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      client
          .readWriteTransaction()
          .run(
              transaction -> {
                transaction.buffer(
                    Arrays.asList(
                        Mutation.newInsertBuilder("FOO").set("ID").to(1L).build(),
                        Mutation.delete("FOO", Key.of(1L))));
                return null;
              });
      // There should be 1 call to BeginTransaction because there is no statement that we can use to
      // inline the BeginTransaction call with.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @SuppressWarnings("resource")
    @Test
    public void testTransactionManagerInlinedBeginTx() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager txMgr = client.transactionManager()) {
        TransactionContext txn = txMgr.begin();
        while (true) {
          try {
            assertThat(txn.executeUpdate(UPDATE_STATEMENT)).isEqualTo(UPDATE_COUNT);
            txMgr.commit();
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetry();
          }
        }
      }
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @SuppressWarnings("resource")
    @Test
    public void testTransactionManagerInlinedBeginTxAborted() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager txMgr = client.transactionManager()) {
        TransactionContext txn = txMgr.begin();
        boolean first = true;
        while (true) {
          try {
            assertThat(txn.executeUpdate(UPDATE_STATEMENT)).isEqualTo(UPDATE_COUNT);
            if (first) {
              mockSpanner.abortAllTransactions();
              first = false;
            }
            txMgr.commit();
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetry();
          }
        }
      }
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @SuppressWarnings("resource")
    @Test
    public void testTransactionManagerInlinedBeginTxWithOnlyMutations() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager txMgr = client.transactionManager()) {
        TransactionContext txn = txMgr.begin();
        while (true) {
          try {
            txn.buffer(Mutation.newInsertBuilder("FOO").set("ID").to(1L).build());
            txMgr.commit();
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetry();
          }
        }
      }
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testTransactionManagerInlinedBeginTxWithError() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager txMgr = client.transactionManager()) {
        TransactionContext txn = txMgr.begin();
        while (true) {
          final TransactionContext txnToUse = txn;
          try {
            SpannerException e =
                assertThrows(
                    SpannerException.class, () -> txnToUse.executeUpdate(INVALID_UPDATE_STATEMENT));
            assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
            assertEquals(UPDATE_COUNT, txnToUse.executeUpdate(UPDATE_STATEMENT));
            txMgr.commit();
            break;
          } catch (AbortedException e) {
            txn = txMgr.resetForRetry();
          }
        }
      }
      // The first statement will fail and not return a transaction id. This will trigger a retry of
      // the entire transaction, and the retry will do an explicit BeginTransaction RPC.
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
      // The first statement will start a transaction, but it will never be returned to the client
      // as the update statement fails.
      assertThat(countTransactionsStarted()).isEqualTo(2);
    }

    @Test
    public void testTransactionManagerInlinedBeginTxWithUncaughtError() {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      try (TransactionManager txMgr = client.transactionManager()) {
        TransactionContext txn = txMgr.begin();
        SpannerException e =
            assertThrows(SpannerException.class, () -> txn.executeUpdate(INVALID_UPDATE_STATEMENT));
        assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
      }
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void testInlinedBeginAsyncTxWithParallelQueries()
        throws InterruptedException, ExecutionException {
      final int numQueries = 100;
      final ScheduledExecutorService executor = Executors.newScheduledThreadPool(16);
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      ApiFuture<Long> updateCount =
          client
              .runAsync()
              .runAsync(
                  txn -> {
                    List<ApiFuture<Long>> futures = new ArrayList<>(numQueries);
                    for (int i = 0; i < numQueries; i++) {
                      final SettableApiFuture<Long> res = SettableApiFuture.create();
                      try (AsyncResultSet rs = txn.executeQueryAsync(SELECT1)) {
                        rs.setCallback(
                            executor,
                            resultSet -> {
                              switch (resultSet.tryNext()) {
                                case DONE:
                                  return CallbackResponse.DONE;
                                case NOT_READY:
                                  return CallbackResponse.CONTINUE;
                                case OK:
                                  res.set(resultSet.getLong(0));
                                default:
                                  throw new IllegalStateException();
                              }
                            });
                      }
                      futures.add(res);
                    }
                    return ApiFutures.transformAsync(
                        ApiFutures.allAsList(futures),
                        input -> {
                          long sum = 0L;
                          for (Long l : input) {
                            sum += l;
                          }
                          return ApiFutures.immediateFuture(sum);
                        },
                        MoreExecutors.directExecutor());
                  },
                  executor);
      assertThat(updateCount.get()).isEqualTo((long) numQueries);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void queryWithoutNext() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      assertThat(
              client
                  .readWriteTransaction()
                  .<Long>run(
                      transaction -> {
                        // This will not actually send an RPC, so it will also not request a
                        // transaction.
                        transaction.executeQuery(SELECT1);
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }))
          .isEqualTo(UPDATE_COUNT);
      assertThat(mockSpanner.countRequestsOfType(BeginTransactionRequest.class)).isEqualTo(0L);
      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1L);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void queryAsyncWithoutCallback() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      assertThat(
              client
                  .readWriteTransaction()
                  .<Long>run(
                      transaction -> {
                        transaction.executeQueryAsync(SELECT1);
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }))
          .isEqualTo(UPDATE_COUNT);
      assertThat(mockSpanner.countRequestsOfType(BeginTransactionRequest.class)).isEqualTo(0L);
      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1L);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void readWithoutNext() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      assertThat(
              client
                  .readWriteTransaction()
                  .<Long>run(
                      transaction -> {
                        transaction.read("FOO", KeySet.all(), Collections.singletonList("ID"));
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }))
          .isEqualTo(UPDATE_COUNT);
      assertThat(mockSpanner.countRequestsOfType(BeginTransactionRequest.class)).isEqualTo(0L);
      assertThat(mockSpanner.countRequestsOfType(ReadRequest.class)).isEqualTo(0L);
      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1L);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void readAsyncWithoutCallback() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      assertThat(
              client
                  .readWriteTransaction()
                  .<Long>run(
                      transaction -> {
                        transaction.readAsync("FOO", KeySet.all(), Collections.singletonList("ID"));
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }))
          .isEqualTo(UPDATE_COUNT);
      assertThat(mockSpanner.countRequestsOfType(BeginTransactionRequest.class)).isEqualTo(0L);
      assertThat(mockSpanner.countRequestsOfType(ReadRequest.class)).isEqualTo(0L);
      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1L);
      assertThat(countTransactionsStarted()).isEqualTo(1);
    }

    @Test
    public void query_ThenUpdate_ThenConsumeResultSet() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      assertThat(
              client
                  .readWriteTransaction()
                  .<Long>run(
                      transaction -> {
                        ResultSet rs = transaction.executeQuery(SELECT1);
                        long updateCount = transaction.executeUpdate(UPDATE_STATEMENT);
                        // Consume the result set.
                        while (rs.next()) {}
                        return updateCount;
                      }))
          .isEqualTo(UPDATE_COUNT);
      // The update statement should start the transaction, and the query should use the transaction
      // id returned by the update.
      assertThat(mockSpanner.countRequestsOfType(BeginTransactionRequest.class)).isEqualTo(0L);
      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(2L);
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1L);
      assertThat(countTransactionsStarted()).isEqualTo(1);
      List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
      assertThat(requests.get(0)).isInstanceOf(ExecuteSqlRequest.class);
      assertThat(requests.get(0).getSql()).isEqualTo(UPDATE_STATEMENT.getSql());
      assertThat(requests.get(1)).isInstanceOf(ExecuteSqlRequest.class);
      assertThat(requests.get(1).getSql()).isEqualTo(SELECT1.getSql());
    }

    @Test
    public void testInlinedBeginTxWithStreamRetry() {
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofStreamException(Status.UNAVAILABLE.asRuntimeException(), 1));

      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      client
          .readWriteTransaction()
          .run(
              transaction -> {
                try (ResultSet rs = transaction.executeQuery(SELECT1_UNION_ALL_SELECT2)) {
                  while (rs.next()) {}
                }
                return null;
              });
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
      assertThat(countTransactionsStarted()).isEqualTo(1);

      List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
      assertThat(requests.get(0)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest request1 = requests.get(0);
      assertThat(request1.getSql()).isEqualTo(SELECT1_UNION_ALL_SELECT2.getSql());
      assertThat(request1.getTransaction().getBegin().hasReadWrite()).isTrue();
      assertThat(request1.getTransaction().getId()).isEqualTo(ByteString.EMPTY);
      assertThat(request1.getResumeToken()).isEqualTo(ByteString.EMPTY);

      assertThat(requests.get(1)).isInstanceOf(ExecuteSqlRequest.class);
      ExecuteSqlRequest request2 = requests.get(1);
      assertThat(request2.getSql()).isEqualTo(SELECT1_UNION_ALL_SELECT2.getSql());
      assertThat(request2.getTransaction().hasBegin()).isFalse();
      assertThat(request2.getTransaction().getId()).isNotEqualTo(ByteString.EMPTY);
      assertThat(request2.getResumeToken()).isNotEqualTo(ByteString.EMPTY);
    }

    @Test
    public void testWaitForTransactionTimeout() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      client
          .readWriteTransaction()
          .run(
              new TransactionCallable<Void>() {
                int attempt = 0;

                @Override
                public Void run(TransactionContext transaction) {
                  attempt++;
                  TransactionContextImpl impl = (TransactionContextImpl) transaction;
                  if (attempt == 1) {
                    impl.waitForTransactionTimeoutMillis = 1L;
                    // Freeze the mock server to prevent the first (async) statement from returning
                    // a transaction.
                    mockSpanner.freeze();
                  } else {
                    impl.waitForTransactionTimeoutMillis = 60_000L;
                  }
                  ApiFuture<Long> updateCount = transaction.executeUpdateAsync(UPDATE_STATEMENT);

                  // Try to execute a query. This will timeout during the first attempt while
                  // waiting
                  // for the first statement to return a transaction, and then force a retry of the
                  // transaction.
                  try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                    while (rs.next()) {}
                  } catch (Throwable t) {
                    mockSpanner.unfreeze();
                    // Wait until the update actually finishes so it has returned a transaction.
                    // This ensures that the retry does not issue a BeginTransaction RPC.
                    SpannerApiFutures.get(updateCount);
                    throw t;
                  }
                  return null;
                }
              });
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(3);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
    }

    @Test
    public void testQueryWithInlineBeginDidNotReturnTransaction() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      // This will cause the first statement that requests a transaction to not return a transaction
      // id.
      mockSpanner.ignoreNextInlineBeginRequest();
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction -> {
                            try (ResultSet rs =
                                transaction.executeQuery(SELECT1_UNION_ALL_SELECT2)) {
                              while (rs.next()) {}
                            }
                            return null;
                          }));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertThat(e.getMessage()).contains(AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    }

    @Test
    public void testReadWithInlineBeginDidNotReturnTransaction() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      // This will cause the first statement that requests a transaction to not return a transaction
      // id.
      mockSpanner.ignoreNextInlineBeginRequest();
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction ->
                              transaction.readRow(
                                  "FOO", Key.of(1L), Collections.singletonList("BAR"))));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertThat(e.getMessage()).contains(AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ReadRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    }

    @Test
    public void testUpdateWithInlineBeginDidNotReturnTransaction() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      // This will cause the first statement that requests a transaction to not return a transaction
      // id.
      mockSpanner.ignoreNextInlineBeginRequest();
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(transaction -> transaction.executeUpdate(UPDATE_STATEMENT)));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertThat(e.getMessage()).contains(AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    }

    @Test
    public void testBatchUpdateWithInlineBeginDidNotReturnTransaction() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      // This will cause the first statement that requests a transaction to not return a transaction
      // id.
      mockSpanner.ignoreNextInlineBeginRequest();
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction -> {
                            transaction.batchUpdate(Collections.singletonList(UPDATE_STATEMENT));
                            return null;
                          }));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertThat(e.getMessage()).contains(AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteBatchDmlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    }

    @Test
    public void testQueryAsyncWithInlineBeginDidNotReturnTransaction() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      // This will cause the first statement that requests a transaction to not return a transaction
      // id.
      mockSpanner.ignoreNextInlineBeginRequest();
      SpannerException outerException =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction -> {
                            try (AsyncResultSet rs =
                                transaction.executeQueryAsync(SELECT1_UNION_ALL_SELECT2)) {
                              return SpannerApiFutures.get(
                                  rs.setCallback(
                                      executor,
                                      resultSet -> {
                                        try {
                                          while (true) {
                                            switch (resultSet.tryNext()) {
                                              case OK:
                                                break;
                                              case DONE:
                                                return CallbackResponse.DONE;
                                              case NOT_READY:
                                                return CallbackResponse.CONTINUE;
                                            }
                                          }
                                        } catch (SpannerException e) {
                                          return CallbackResponse.DONE;
                                        }
                                      }));
                            }
                          }));
      assertEquals(ErrorCode.FAILED_PRECONDITION, outerException.getErrorCode());
      assertThat(outerException.getMessage())
          .contains(AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);

      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    }

    @Test
    public void testUpdateAsyncWithInlineBeginDidNotReturnTransaction() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      // This will cause the first statement that requests a transaction to not return a transaction
      // id.
      mockSpanner.ignoreNextInlineBeginRequest();
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction ->
                              SpannerApiFutures.get(
                                  transaction.executeUpdateAsync(UPDATE_STATEMENT))));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertThat(e.getMessage()).contains(AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    }

    @Test
    public void testBatchUpdateAsyncWithInlineBeginDidNotReturnTransaction() {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      // This will cause the first statement that requests a transaction to not return a transaction
      // id.
      mockSpanner.ignoreNextInlineBeginRequest();
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction ->
                              SpannerApiFutures.get(
                                  transaction.batchUpdateAsync(
                                      Collections.singletonList(UPDATE_STATEMENT)))));
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
      assertThat(e.getMessage()).contains(AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
      assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
      assertThat(countRequests(ExecuteBatchDmlRequest.class)).isEqualTo(1);
      assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    }

    @Test
    public void testInlinedBeginTx_withCancelledOnFirstStatement() {
      final Statement statement = Statement.of("INSERT INTO FOO (Id) VALUES (1)");
      mockSpanner.putStatementResult(
          StatementResult.exception(
              statement,
              Status.CANCELLED
                  .withDescription(
                      "Read/query was cancelled due to the enclosing transaction being invalidated by a later transaction in the same session.")
                  .asRuntimeException()));

      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      long updateCount =
          client
              .readWriteTransaction()
              .run(
                  new TransactionCallable<Long>() {
                    int attempt = 0;

                    @Override
                    public Long run(TransactionContext transaction) {
                      if (attempt > 0) {
                        mockSpanner.putStatementResult(StatementResult.update(statement, 1L));
                      }
                      attempt++;
                      return transaction.executeUpdate(statement);
                    }
                  });
      assertEquals(1L, updateCount);
      // The transaction will be retried because the first statement that also tried to include the
      // BeginTransaction statement failed with the specific CANCELLED error and did not return a
      // transaction. That forces a retry of the entire transaction with an explicit
      // BeginTransaction RPC.
      assertEquals(1, countRequests(BeginTransactionRequest.class));
      // The update statement will be executed 2 times:
      assertEquals(2, countRequests(ExecuteSqlRequest.class));
      // The transaction will attempt to commit once.
      assertEquals(1, countRequests(CommitRequest.class));
      // The first update will start a transaction, but then fail the update statement. This will
      // start a transaction on the mock server, but that transaction will never be returned to the
      // client.
      assertEquals(2, countTransactionsStarted());
    }

    @Test
    public void testInlinedBeginTx_withStickyCancelledOnFirstStatement() {
      final Statement statement = Statement.of("INSERT INTO FOO (Id) VALUES (1)");
      mockSpanner.putStatementResult(
          StatementResult.exception(
              statement,
              Status.CANCELLED
                  .withDescription(
                      "Read/query was cancelled due to the enclosing transaction being invalidated by a later transaction in the same session.")
                  .asRuntimeException()));

      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      // The CANCELLED error is thrown both on the first and second attempt. The second attempt will
      // not be retried, as it did not include a BeginTransaction option.
      SpannerException e =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(transaction -> transaction.executeUpdate(statement)));
      assertEquals(ErrorCode.CANCELLED, e.getErrorCode());
      assertEquals(1, countRequests(BeginTransactionRequest.class));
      // The update statement will be executed 2 times:
      assertEquals(2, countRequests(ExecuteSqlRequest.class));
      // The transaction will never attempt to commit.
      assertEquals(0, countRequests(CommitRequest.class));
      assertEquals(2, countTransactionsStarted());
    }
  }

  private static int countRequests(Class<? extends AbstractMessage> requestType) {
    int count = 0;
    for (AbstractMessage msg : mockSpanner.getRequests()) {
      if (msg.getClass().equals(requestType)) {
        count++;
      }
    }
    return count;
  }

  private static int countTransactionsStarted() {
    return mockSpanner.getTransactionsStarted().size();
  }
}
