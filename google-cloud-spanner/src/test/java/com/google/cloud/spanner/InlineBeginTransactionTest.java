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

import static com.google.cloud.spanner.MockSpannerTestUtil.SELECT1;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.core.ApiAsyncFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.AsyncRunner.AsyncWork;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionFunction;
import com.google.cloud.spanner.AsyncTransactionManager.AsyncTransactionStep;
import com.google.cloud.spanner.AsyncTransactionManager.CommitTimestampFuture;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class InlineBeginTransactionTest {
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

  private Spanner spanner;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT1_UNION_ALL_SELECT2, SELECT1_UNION_ALL_SELECT2_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.query(READ_STATEMENT, SELECT1_RESULTSET));
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
  public void setUp() throws IOException {
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    // Create a Spanner instance that will inline BeginTransaction calls. It also has no prepared
    // sessions in the pool to prevent session preparing from interfering with test cases.
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
  }

  @After
  public void tearDown() throws Exception {
    spanner.close();
    mockSpanner.reset();
  }

  @Test
  public void testInlinedBeginTx() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    long updateCount =
        client
            .readWriteTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    return transaction.executeUpdate(UPDATE_STATEMENT);
                  }
                });
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
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    long res = transaction.executeUpdate(UPDATE_STATEMENT);
                    if (firstAttempt.getAndSet(false)) {
                      mockSpanner.abortTransaction(transaction);
                    }
                    return res;
                  }
                });
    assertThat(updateCount).isEqualTo(UPDATE_COUNT);
    assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
    assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
    // We have started 2 transactions, because the first transaction aborted during the commit.
    assertThat(countRequests(CommitRequest.class)).isEqualTo(2);
    assertThat(countTransactionsStarted()).isEqualTo(2);
  }

  @Test
  public void testInlinedBeginTxWithQuery() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    long updateCount =
        client
            .readWriteTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                      while (rs.next()) {
                        return rs.getLong(0);
                      }
                    }
                    return 0L;
                  }
                });
    assertThat(updateCount).isEqualTo(1L);
    assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
    assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
    assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
    assertThat(countTransactionsStarted()).isEqualTo(1);
  }

  @Test
  public void testInlinedBeginTxWithRead() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    long updateCount =
        client
            .readWriteTransaction()
            .run(
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    try (ResultSet rs =
                        transaction.read("FOO", KeySet.all(), Arrays.asList("ID"))) {
                      while (rs.next()) {
                        return rs.getLong(0);
                      }
                    }
                    return 0L;
                  }
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
                new TransactionCallable<long[]>() {
                  @Override
                  public long[] run(TransactionContext transaction) throws Exception {
                    return transaction.batchUpdate(
                        Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT));
                  }
                });
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
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    try {
                      transaction.executeUpdate(INVALID_UPDATE_STATEMENT);
                      fail("missing expected exception");
                    } catch (SpannerException e) {
                      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                    }
                    return transaction.executeUpdate(UPDATE_STATEMENT);
                  }
                });
    assertThat(updateCount).isEqualTo(UPDATE_COUNT);
    // The transaction will be retried because the first statement that also tried to include the
    // BeginTransaction statement failed and did not return a transaction. That forces a retry of
    // the entire transaction with an explicit BeginTransaction RPC.
    assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(1);
    // The update statement will be executed 3 times:
    // 1. The invalid update statement will be executed during the first attempt and fail. The
    // second update statement will not be executed, as the transaction runner sees that the initial
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
    try {
      client
          .readWriteTransaction()
          .run(
              new TransactionCallable<Void>() {
                @Override
                public Void run(TransactionContext transaction) throws Exception {
                  try {
                    transaction.executeUpdate(INVALID_UPDATE_STATEMENT);
                    fail("missing expected exception");
                  } catch (SpannerException e) {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                  }
                  return null;
                }
              });
      fail("Missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTERNAL);
      assertThat(e.getMessage()).contains("Begin transaction failed due to an internal error");
    }
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
    try {
      client
          .readWriteTransaction()
          .run(
              new TransactionCallable<Long>() {
                @Override
                public Long run(TransactionContext transaction) throws Exception {
                  return transaction.executeUpdate(INVALID_UPDATE_STATEMENT);
                }
              });
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
    // The first update will start a transaction, but then fail the update statement. This will
    // start a transaction on the mock server, but that transaction will never be returned to the
    // client.
    assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
    assertThat(countRequests(CommitRequest.class)).isEqualTo(0);
    assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(1);
    // No rollback request will be initiated because the client does not receive any transaction id.
    assertThat(countRequests(RollbackRequest.class)).isEqualTo(0);
    assertThat(countTransactionsStarted()).isEqualTo(1);
  }

  @Test
  public void testInlinedBeginTxWithUncaughtErrorAfterSuccessfulBegin() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    try {
      client
          .readWriteTransaction()
          .run(
              new TransactionCallable<Long>() {
                @Override
                public Long run(TransactionContext transaction) throws Exception {
                  // This statement will start a transaction.
                  transaction.executeUpdate(UPDATE_STATEMENT);
                  // This statement will fail and cause a rollback as the exception is not caught.
                  return transaction.executeUpdate(INVALID_UPDATE_STATEMENT);
                }
              });
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
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
                new TransactionCallable<Void>() {
                  @Override
                  public Void run(TransactionContext transaction) throws Exception {
                    try {
                      transaction.batchUpdate(
                          ImmutableList.of(INVALID_UPDATE_STATEMENT, UPDATE_STATEMENT));
                      fail("missing expected exception");
                    } catch (SpannerBatchUpdateException e) {
                      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                      assertThat(e.getUpdateCounts()).hasLength(0);
                    }
                    return null;
                  }
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
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(TransactionContext transaction) throws Exception {
                    try {
                      transaction.batchUpdate(
                          ImmutableList.of(UPDATE_STATEMENT, INVALID_UPDATE_STATEMENT));
                      fail("missing expected exception");
                      // The following line is needed as the compiler does not know that this is
                      // unreachable.
                      return -1L;
                    } catch (SpannerBatchUpdateException e) {
                      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                      assertThat(e.getUpdateCounts()).hasLength(1);
                      return e.getUpdateCounts()[0];
                    }
                  }
                });
    assertThat(updateCount).isEqualTo(UPDATE_COUNT);
    // Although the batch DML returned an error, that error was for the second statement. That means
    // that the transaction was started by the first statement.
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
                new TransactionCallable<Void>() {
                  @Override
                  public Void run(TransactionContext transaction) throws Exception {
                    try (ResultSet rs = transaction.executeQuery(INVALID_SELECT)) {
                      while (rs.next()) {}
                      fail("missing expected exception");
                    } catch (SpannerException e) {
                      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
                    }
                    return null;
                  }
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
    // The first PartialResultSet will be returned successfully, and then a DATA_LOSS exception will
    // be returned.
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofStreamException(Status.DATA_LOSS.asRuntimeException(), 1));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    Void res =
        client
            .readWriteTransaction()
            .run(
                new TransactionCallable<Void>() {
                  @Override
                  public Void run(TransactionContext transaction) throws Exception {
                    try (ResultSet rs = transaction.executeQuery(statement)) {
                      while (rs.next()) {}
                      fail("missing expected exception");
                    } catch (SpannerException e) {
                      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DATA_LOSS);
                    }
                    return null;
                  }
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
                new TransactionCallable<Long>() {
                  @Override
                  public Long run(final TransactionContext transaction) throws Exception {
                    List<Future<Long>> futures = new ArrayList<>(numQueries);
                    for (int i = 0; i < numQueries; i++) {
                      futures.add(
                          executor.submit(
                              new Callable<Long>() {
                                @Override
                                public Long call() throws Exception {
                                  try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                                    while (rs.next()) {
                                      return rs.getLong(0);
                                    }
                                  }
                                  return 0L;
                                }
                              }));
                    }
                    Long res = 0L;
                    for (Future<Long> f : futures) {
                      res += f.get();
                    }
                    return res;
                  }
                });
    assertThat(updateCount).isEqualTo(1L * numQueries);
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
            new TransactionCallable<Void>() {
              @Override
              public Void run(TransactionContext transaction) throws Exception {
                transaction.buffer(
                    Arrays.asList(
                        Mutation.newInsertBuilder("FOO").set("ID").to(1L).build(),
                        Mutation.delete("FOO", Key.of(1L))));
                return null;
              }
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

  @SuppressWarnings("resource")
  @Test
  public void testTransactionManagerInlinedBeginTxWithError() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    try (TransactionManager txMgr = client.transactionManager()) {
      TransactionContext txn = txMgr.begin();
      while (true) {
        try {
          try {
            txn.executeUpdate(INVALID_UPDATE_STATEMENT);
            fail("missing expected exception");
          } catch (SpannerException e) {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
          }
          assertThat(txn.executeUpdate(UPDATE_STATEMENT)).isEqualTo(UPDATE_COUNT);
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
    // The first statement will start a transaction, but it will never be returned to the client as
    // the update statement fails.
    assertThat(countTransactionsStarted()).isEqualTo(2);
  }

  @SuppressWarnings("resource")
  @Test
  public void testTransactionManagerInlinedBeginTxWithUncaughtError() {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    try (TransactionManager txMgr = client.transactionManager()) {
      TransactionContext txn = txMgr.begin();
      while (true) {
        try {
          txn.executeUpdate(INVALID_UPDATE_STATEMENT);
          fail("missing expected exception");
        } catch (AbortedException e) {
          txn = txMgr.resetForRetry();
        }
      }
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
    assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
    assertThat(countTransactionsStarted()).isEqualTo(1);
  }

  @Test
  public void testInlinedBeginAsyncTx() throws InterruptedException, ExecutionException {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    ApiFuture<Long> updateCount =
        client
            .runAsync()
            .runAsync(
                new AsyncWork<Long>() {
                  @Override
                  public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                    return txn.executeUpdateAsync(UPDATE_STATEMENT);
                  }
                },
                executor);
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
                new AsyncWork<Long>() {
                  @Override
                  public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                    ApiFuture<Long> res = txn.executeUpdateAsync(UPDATE_STATEMENT);
                    if (firstAttempt.getAndSet(false)) {
                      mockSpanner.abortTransaction(txn);
                    }
                    return res;
                  }
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
                new AsyncWork<Long>() {
                  @Override
                  public ApiFuture<Long> doWorkAsync(TransactionContext txn) {
                    final SettableApiFuture<Long> res = SettableApiFuture.create();
                    try (AsyncResultSet rs = txn.executeQueryAsync(SELECT1)) {
                      rs.setCallback(
                          executor,
                          new ReadyCallback() {
                            @Override
                            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
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
                            }
                          });
                    }
                    return res;
                  }
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
                new AsyncWork<long[]>() {
                  @Override
                  public ApiFuture<long[]> doWorkAsync(TransactionContext transaction) {
                    return transaction.batchUpdateAsync(
                        Arrays.asList(UPDATE_STATEMENT, UPDATE_STATEMENT));
                  }
                },
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
                new AsyncWork<Long>() {
                  @Override
                  public ApiFuture<Long> doWorkAsync(TransactionContext transaction) {
                    transaction.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
                    return transaction.executeUpdateAsync(UPDATE_STATEMENT);
                  }
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
                new AsyncWork<Long>() {
                  @Override
                  public ApiFuture<Long> doWorkAsync(final TransactionContext txn) {
                    List<ApiFuture<Long>> futures = new ArrayList<>(numQueries);
                    for (int i = 0; i < numQueries; i++) {
                      final SettableApiFuture<Long> res = SettableApiFuture.create();
                      try (AsyncResultSet rs = txn.executeQueryAsync(SELECT1)) {
                        rs.setCallback(
                            executor,
                            new ReadyCallback() {
                              @Override
                              public CallbackResponse cursorReady(AsyncResultSet resultSet) {
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
                              }
                            });
                      }
                      futures.add(res);
                    }
                    return ApiFutures.transformAsync(
                        ApiFutures.allAsList(futures),
                        new ApiAsyncFunction<List<Long>, Long>() {
                          @Override
                          public ApiFuture<Long> apply(List<Long> input) throws Exception {
                            long sum = 0L;
                            for (Long l : input) {
                              sum += l;
                            }
                            return ApiFutures.immediateFuture(sum);
                          }
                        },
                        MoreExecutors.directExecutor());
                  }
                },
                executor);
    assertThat(updateCount.get()).isEqualTo(1L * numQueries);
    assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
    assertThat(countTransactionsStarted()).isEqualTo(1);
  }

  @Test
  public void testInlinedBeginAsyncTxWithOnlyMutations()
      throws InterruptedException, ExecutionException {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    client
        .runAsync()
        .runAsync(
            new AsyncWork<Void>() {
              @Override
              public ApiFuture<Void> doWorkAsync(TransactionContext transaction) {
                transaction.buffer(Mutation.newInsertBuilder("FOO").set("ID").to(1L).build());
                return ApiFutures.immediateFuture(null);
              }
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
                new AsyncTransactionFunction<Void, Long>() {
                  @Override
                  public ApiFuture<Long> apply(TransactionContext txn, Void input)
                      throws Exception {
                    return txn.executeUpdateAsync(UPDATE_STATEMENT);
                  }
                },
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
                  new AsyncTransactionFunction<Void, Long>() {
                    @Override
                    public ApiFuture<Long> apply(TransactionContext txn, Void input)
                        throws Exception {
                      return txn.executeUpdateAsync(UPDATE_STATEMENT);
                    }
                  },
                  executor);
          if (first) {
            // Abort the transaction after the statement has been executed to ensure that the
            // transaction has actually been started before the test tries to abort it.
            updateCount.then(
                new AsyncTransactionFunction<Long, Void>() {
                  @Override
                  public ApiFuture<Void> apply(TransactionContext txn, Long input)
                      throws Exception {
                    mockSpanner.abortAllTransactions();
                    return ApiFutures.immediateFuture(null);
                  }
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
                  new AsyncTransactionFunction<Void, Void>() {
                    @Override
                    public ApiFuture<Void> apply(TransactionContext txn, Void input)
                        throws Exception {
                      txn.buffer(Mutation.newInsertBuilder("FOO").set("ID").to(1L).build());
                      return ApiFutures.immediateFuture(null);
                    }
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
  public void testAsyncTransactionManagerInlinedBeginTxWithError()
      throws InterruptedException, ExecutionException {
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    try (AsyncTransactionManager txMgr = client.transactionManagerAsync()) {
      TransactionContextFuture txn = txMgr.beginAsync();
      while (true) {
        try {
          AsyncTransactionStep<Long, Long> updateCount =
              txn.then(
                      new AsyncTransactionFunction<Void, Long>() {
                        @Override
                        public ApiFuture<Long> apply(TransactionContext txn, Void input)
                            throws Exception {
                          return txn.executeUpdateAsync(INVALID_UPDATE_STATEMENT);
                        }
                      },
                      executor)
                  .then(
                      new AsyncTransactionFunction<Long, Long>() {
                        @Override
                        public ApiFuture<Long> apply(TransactionContext txn, Long input)
                            throws Exception {
                          return txn.executeUpdateAsync(UPDATE_STATEMENT);
                        }
                      },
                      executor);
          try {
            updateCount.commitAsync().get();
            fail("missing expected exception");
          } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(SpannerException.class);
            SpannerException se = (SpannerException) e.getCause();
            assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
          }
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
  public void queryWithoutNext() {
    DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    assertThat(
            client
                .readWriteTransaction()
                .run(
                    new TransactionCallable<Long>() {
                      @Override
                      public Long run(TransactionContext transaction) throws Exception {
                        // This will not actually send an RPC, so it will also not request a
                        // transaction.
                        transaction.executeQuery(SELECT1);
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }
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
                .run(
                    new TransactionCallable<Long>() {
                      @Override
                      public Long run(TransactionContext transaction) throws Exception {
                        transaction.executeQueryAsync(SELECT1);
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }
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
                .run(
                    new TransactionCallable<Long>() {
                      @Override
                      public Long run(TransactionContext transaction) throws Exception {
                        transaction.read("FOO", KeySet.all(), Arrays.asList("ID"));
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }
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
                .run(
                    new TransactionCallable<Long>() {
                      @Override
                      public Long run(TransactionContext transaction) throws Exception {
                        transaction.readAsync("FOO", KeySet.all(), Arrays.asList("ID"));
                        return transaction.executeUpdate(UPDATE_STATEMENT);
                      }
                    }))
        .isEqualTo(UPDATE_COUNT);
    assertThat(mockSpanner.countRequestsOfType(BeginTransactionRequest.class)).isEqualTo(0L);
    assertThat(mockSpanner.countRequestsOfType(ReadRequest.class)).isEqualTo(0L);
    assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1L);
    assertThat(countTransactionsStarted()).isEqualTo(1);
  }

  @Test
  public void query_ThenUpdate_ThenConsumeResultSet()
      throws InterruptedException, TimeoutException {
    DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    assertThat(
            client
                .readWriteTransaction()
                .run(
                    new TransactionCallable<Long>() {
                      @Override
                      public Long run(TransactionContext transaction) throws Exception {
                        ResultSet rs = transaction.executeQuery(SELECT1);
                        long updateCount = transaction.executeUpdate(UPDATE_STATEMENT);
                        // Consume the result set.
                        while (rs.next()) {}
                        return updateCount;
                      }
                    }))
        .isEqualTo(UPDATE_COUNT);
    // The update statement should start the transaction, and the query should use the transaction
    // id returned by the update.
    assertThat(mockSpanner.countRequestsOfType(BeginTransactionRequest.class)).isEqualTo(0L);
    assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(2L);
    assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1L);
    assertThat(countTransactionsStarted()).isEqualTo(1);
    List<AbstractMessage> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests.get(0)).isInstanceOf(ExecuteSqlRequest.class);
    assertThat(((ExecuteSqlRequest) requests.get(0)).getSql()).isEqualTo(UPDATE_STATEMENT.getSql());
    assertThat(requests.get(1)).isInstanceOf(ExecuteSqlRequest.class);
    assertThat(((ExecuteSqlRequest) requests.get(1)).getSql()).isEqualTo(SELECT1.getSql());
  }

  @Test
  public void testInlinedBeginTxWithStreamRetry() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofStreamException(Status.UNAVAILABLE.asRuntimeException(), 1));

    DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    client
        .readWriteTransaction()
        .run(
            new TransactionCallable<Void>() {
              @Override
              public Void run(TransactionContext transaction) throws Exception {
                try (ResultSet rs = transaction.executeQuery(SELECT1_UNION_ALL_SELECT2)) {
                  while (rs.next()) {}
                }
                return null;
              }
            });
    assertThat(countRequests(BeginTransactionRequest.class)).isEqualTo(0);
    assertThat(countRequests(ExecuteSqlRequest.class)).isEqualTo(2);
    assertThat(countRequests(CommitRequest.class)).isEqualTo(1);
    assertThat(countTransactionsStarted()).isEqualTo(1);

    List<AbstractMessage> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    assertThat(requests.get(0)).isInstanceOf(ExecuteSqlRequest.class);
    ExecuteSqlRequest request1 = (ExecuteSqlRequest) requests.get(0);
    assertThat(request1.getSql()).isEqualTo(SELECT1_UNION_ALL_SELECT2.getSql());
    assertThat(request1.getTransaction().getBegin().hasReadWrite()).isTrue();
    assertThat(request1.getTransaction().getId()).isEqualTo(ByteString.EMPTY);
    assertThat(request1.getResumeToken()).isEqualTo(ByteString.EMPTY);

    assertThat(requests.get(1)).isInstanceOf(ExecuteSqlRequest.class);
    ExecuteSqlRequest request2 = (ExecuteSqlRequest) requests.get(1);
    assertThat(request2.getSql()).isEqualTo(SELECT1_UNION_ALL_SELECT2.getSql());
    assertThat(request2.getTransaction().hasBegin()).isFalse();
    assertThat(request2.getTransaction().getId()).isNotEqualTo(ByteString.EMPTY);
    assertThat(request2.getResumeToken()).isNotEqualTo(ByteString.EMPTY);
  }

  private int countRequests(Class<? extends AbstractMessage> requestType) {
    int count = 0;
    for (AbstractMessage msg : mockSpanner.getRequests()) {
      if (msg.getClass().equals(requestType)) {
        count++;
      }
    }
    return count;
  }

  private int countTransactionsStarted() {
    return mockSpanner.getTransactionsStarted().size();
  }
}
