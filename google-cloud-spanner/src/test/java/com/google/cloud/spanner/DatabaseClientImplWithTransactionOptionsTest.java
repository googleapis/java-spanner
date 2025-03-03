package com.google.cloud.spanner;

import static com.google.cloud.spanner.SpannerApiFutures.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.api.core.ApiFutures;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncTransactionManager.TransactionContextFuture;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.SpannerOptions.Builder.TransactionOptions.TransactionOptionsBuilder;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TransactionOptions.IsolationLevel;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DatabaseClientImplWithTransactionOptionsTest {

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static ExecutorService executor;
  private static LocalChannelProvider channelProvider;
  private static final Statement UPDATE_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
  private static final Statement INVALID_UPDATE_STATEMENT =
      Statement.of("UPDATE NON_EXISTENT_TABLE SET BAR=1 WHERE BAZ=2");
  private static final Statement INVALID_SELECT_STATEMENT =
      Statement.of("SELECT * FROM NON_EXISTENT_TABLE");
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
  private Spanner spannerWithRepeatableReadOption;
  private Spanner spannerWithSerializableOption;
  private DatabaseClient client;
  private DatabaseClient clientWithRepeatableReadOption;
  private DatabaseClient clientWithSerializableOption;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_STATEMENT, UPDATE_COUNT));
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_UPDATE_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));
    mockSpanner.putStatementResult(
        StatementResult.exception(
            INVALID_SELECT_STATEMENT,
            Status.INVALID_ARGUMENT.withDescription("invalid statement").asRuntimeException()));
    mockSpanner.putStatementResult(
        StatementResult.read(
            "FOO", KeySet.all(), Collections.singletonList("ID"), SELECT1_RESULTSET));

    String uniqueName = InProcessServerBuilder.generateName();
    executor = Executors.newSingleThreadExecutor();
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
    SpannerOptions.Builder spannerOptionsBuilder =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    spanner = spannerOptionsBuilder.build().getService();
    spannerWithRepeatableReadOption =
        spannerOptionsBuilder
            .setDefaultTransactionOptions(
                TransactionOptionsBuilder.newBuilder()
                    .setIsolationLevel(Options.repeatableReadIsolationLevel())
                    .build())
            .build()
            .getService();
    spannerWithSerializableOption =
        spannerOptionsBuilder
            .setDefaultTransactionOptions(
                TransactionOptionsBuilder.newBuilder()
                    .setIsolationLevel(Options.serializableIsolationLevel())
                    .build())
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    clientWithRepeatableReadOption =
        spannerWithRepeatableReadOption.getDatabaseClient(
            DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    clientWithSerializableOption =
        spannerWithSerializableOption.getDatabaseClient(
            DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
  }

  @After
  public void tearDown() {
    spanner.close();
    spannerWithRepeatableReadOption.close();
    spannerWithSerializableOption.close();
  }

  @Test
  public void testWrite_WithNoIsolationLevel() {
    Timestamp timestamp =
        client.write(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);
    validateIsolationLevel(IsolationLevel.ISOLATION_LEVEL_UNSPECIFIED);
  }

  @Test
  public void testWrite_WithRRSpannerOptions() {
    Timestamp timestamp =
        clientWithRepeatableReadOption.write(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void testWriteWithOptions_WithRRSpannerOptions() {
    clientWithRepeatableReadOption.writeWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.priority(RpcPriority.HIGH));
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void testWriteWithOptions_WithSerializableTxnOption() {
    clientWithRepeatableReadOption.writeWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.serializableIsolationLevel());
    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void testWriteAtLeastOnce_WithSerializableSpannerOptions() {
    Timestamp timestamp =
        clientWithSerializableOption.writeAtLeastOnce(
            Collections.singletonList(
                Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()));
    assertNotNull(timestamp);
    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void testWriteAtLeastOnceWithOptions_WithRRTxnOption() {
    clientWithSerializableOption.writeAtLeastOnceWithOptions(
        Collections.singletonList(
            Mutation.newInsertBuilder("FOO").set("ID").to(1L).set("NAME").to("Bar").build()),
        Options.repeatableReadIsolationLevel());
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void testReadWriteTxn_WithRRSpannerOption_batchUpdate() {
    TransactionRunner runner = clientWithRepeatableReadOption.readWriteTransaction();
    runner.run(transaction -> transaction.batchUpdate(Collections.singletonList(UPDATE_STATEMENT)));
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void testReadWriteTxn_WithSerializableTxnOption_batchUpdate() {
    TransactionRunner runner =
        clientWithRepeatableReadOption.readWriteTransaction(Options.serializableIsolationLevel());
    runner.run(transaction -> transaction.batchUpdate(Collections.singletonList(UPDATE_STATEMENT)));
    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void testPartitionedDML_WithRRSpannerOption() {
    clientWithRepeatableReadOption.executePartitionedUpdate(UPDATE_STATEMENT);
    validateIsolationLevel(IsolationLevel.ISOLATION_LEVEL_UNSPECIFIED);
  }

  @Test
  public void testCommit_WithSerializableTxnOption() {
    TransactionRunner runner = client.readWriteTransaction(Options.serializableIsolationLevel());
    runner.run(
        transaction -> {
          transaction.buffer(Mutation.delete("TEST", KeySet.all()));
          return null;
        });

    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void testTransactionManagerCommit_WithRRTxnOption() {
    try (TransactionManager manager =
        clientWithSerializableOption.transactionManager(Options.repeatableReadIsolationLevel())) {
      TransactionContext transaction = manager.begin();
      transaction.buffer(Mutation.delete("TEST", KeySet.all()));
      manager.commit();
    }

    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void testAsyncRunnerCommit_WithRRSpannerOption() {
    AsyncRunner runner = clientWithRepeatableReadOption.runAsync();
    get(
        runner.runAsync(
            txn -> {
              txn.buffer(Mutation.delete("TEST", KeySet.all()));
              return ApiFutures.immediateFuture(null);
            },
            executor));

    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void testAsyncTransactionManagerCommit_WithSerializableTxnOption() {
    try (AsyncTransactionManager manager =
        clientWithRepeatableReadOption.transactionManagerAsync(
            Options.serializableIsolationLevel())) {
      TransactionContextFuture transaction = manager.beginAsync();
      get(
          transaction
              .then(
                  (txn, input) -> {
                    txn.buffer(Mutation.delete("TEST", KeySet.all()));
                    return ApiFutures.immediateFuture(null);
                  },
                  executor)
              .commitAsync());
    }

    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void testReadWriteTxn_WithNoOptions() {
    client
        .readWriteTransaction()
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.ISOLATION_LEVEL_UNSPECIFIED);
  }

  @Test
  public void executeSqlWithRWTransactionOptions_RepeatableRead() {
    client
        .readWriteTransaction(Options.repeatableReadIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void
      executeSqlWithDefaultSpannerOptions_SerializableAndRWTransactionOptions_RepeatableRead() {
    clientWithSerializableOption
        .readWriteTransaction(Options.repeatableReadIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void
      executeSqlWithDefaultSpannerOptions_RepeatableReadAndRWTransactionOptions_Serializable() {
    clientWithRepeatableReadOption
        .readWriteTransaction(Options.serializableIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void executeSqlWithDefaultSpannerOptions_RepeatableReadAndNoRWTransactionOptions() {
    clientWithRepeatableReadOption
        .readWriteTransaction()
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void executeSqlWithRWTransactionOptions_Serializable() {
    client
        .readWriteTransaction(Options.serializableIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(SELECT1)) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void readWithRWTransactionOptions_RepeatableRead() {
    client
        .readWriteTransaction(Options.repeatableReadIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs =
                  transaction.read("FOO", KeySet.all(), Collections.singletonList("ID"))) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void readWithRWTransactionOptions_Serializable() {
    client
        .readWriteTransaction(Options.serializableIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs =
                  transaction.read("FOO", KeySet.all(), Collections.singletonList("ID"))) {
                while (rs.next()) {
                  assertEquals(rs.getLong(0), 1);
                }
              }
              return null;
            });
    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  @Test
  public void beginTransactionWithRWTransactionOptions_RepeatableRead() {
    client
        .readWriteTransaction(Options.repeatableReadIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(INVALID_SELECT_STATEMENT)) {
                SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
                assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
              }
              return transaction.executeUpdate(UPDATE_STATEMENT);
            });
    validateIsolationLevel(IsolationLevel.REPEATABLE_READ);
  }

  @Test
  public void beginTransactionWithRWTransactionOptions_Serializable() {
    client
        .readWriteTransaction(Options.serializableIsolationLevel())
        .run(
            transaction -> {
              try (ResultSet rs = transaction.executeQuery(INVALID_SELECT_STATEMENT)) {
                SpannerException e = assertThrows(SpannerException.class, () -> rs.next());
                assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
              }
              return transaction.executeUpdate(UPDATE_STATEMENT);
            });
    validateIsolationLevel(IsolationLevel.SERIALIZABLE);
  }

  private void validateIsolationLevel(IsolationLevel isolationLevel) {
    boolean foundMatchingRequest = false;
    for (AbstractMessage request : mockSpanner.getRequests()) {
      if (request instanceof ExecuteSqlRequest) {
        foundMatchingRequest = true;
        assertEquals(
            ((ExecuteSqlRequest) request).getTransaction().getBegin().getIsolationLevel(),
            isolationLevel);
      } else if (request instanceof BeginTransactionRequest) {
        foundMatchingRequest = true;
        assertEquals(
            ((BeginTransactionRequest) request).getOptions().getIsolationLevel(), isolationLevel);
      } else if (request instanceof ReadRequest) {
        foundMatchingRequest = true;
        assertEquals(
            ((ReadRequest) request).getTransaction().getBegin().getIsolationLevel(),
            isolationLevel);
      } else if (request instanceof CommitRequest) {
        foundMatchingRequest = true;
        assertEquals(
            ((CommitRequest) request).getSingleUseTransaction().getIsolationLevel(),
            isolationLevel);
      } else if (request instanceof ExecuteBatchDmlRequest) {
        foundMatchingRequest = true;
        assertEquals(
            ((ExecuteBatchDmlRequest) request).getTransaction().getBegin().getIsolationLevel(),
            isolationLevel);
      }
      if (foundMatchingRequest) {
        break;
      }
    }
  }
}
