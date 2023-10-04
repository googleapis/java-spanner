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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.admin.database.v1.MockDatabaseAdminImpl;
import com.google.cloud.spanner.admin.instance.v1.MockInstanceAdminImpl;
import com.google.common.collect.ImmutableList;
import com.google.longrunning.GetOperationRequest;
import com.google.longrunning.Operation;
import com.google.longrunning.OperationsGrpc.OperationsImplBase;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RecreateGrpcChannelsTest {
  private static final int NUM_THREADS = 128;
  private static final int NUM_TESTS = 2048;

  private static final int NUM_RANDOM_ROWS = 250;
  private static final Statement SELECT_RANDOM_STATEMENT =
      Statement.of("SELECT * FROM random_table");
  private static final Statement SELECT1_STATEMENT = Statement.of("SELECT 1");
  private static final ResultSetMetadata SELECT1_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("C")
                          .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
                          .build())
                  .build())
          .build();
  public static final com.google.spanner.v1.ResultSet SELECT1_RESULT_SET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(Value.newBuilder().setStringValue("1").build())
                  .build())
          .setMetadata(SELECT1_METADATA)
          .build();

  private static final Statement INSERT_STATEMENT =
      Statement.of("INSERT INTO my_table (id, value) VALUES (1, 'One')");

  protected static MockSpannerServiceImpl mockSpanner;
  public static MockInstanceAdminImpl mockInstanceAdmin;
  public static MockDatabaseAdminImpl mockDatabaseAdmin;
  public static OperationsImplBase mockOperations;
  protected static Server server;

  private static Spanner spanner;
  private static DatabaseClient client1;
  private static DatabaseClient client2;

  private final AtomicInteger reconnectCount = new AtomicInteger();

  @BeforeClass
  public static void startMockServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockInstanceAdmin = new MockInstanceAdminImpl();
    mockDatabaseAdmin = new MockDatabaseAdminImpl();
    mockOperations =
        new OperationsImplBase() {
          @Override
          public void getOperation(
              GetOperationRequest request, StreamObserver<Operation> responseObserver) {
            responseObserver.onNext(
                Operation.newBuilder()
                    .setDone(false)
                    .setName(request.getName())
                    .setMetadata(Any.pack(Empty.getDefaultInstance()))
                    .build());
            responseObserver.onCompleted();
          }
        };
    InetSocketAddress address = new InetSocketAddress("localhost", 0);
    server =
        NettyServerBuilder.forAddress(address)
            .addService(mockSpanner)
            .addService(mockInstanceAdmin)
            .addService(mockDatabaseAdmin)
            .addService(mockOperations)
            .build()
            .start();

    RandomResultSetGenerator generator = new RandomResultSetGenerator(NUM_RANDOM_ROWS);
    mockSpanner.putStatementResult(StatementResult.query(SELECT1_STATEMENT, SELECT1_RESULT_SET));
    mockSpanner.putStatementResult(
        StatementResult.query(SELECT_RANDOM_STATEMENT, generator.generate()));
    mockSpanner.putStatementResult(StatementResult.update(INSERT_STATEMENT, 1L));

    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setHost(String.format("http://localhost:%d", server.getPort()))
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setFailOnSessionLeak().build())
            .setCloseChannelsDelayMillis(1)
            .build()
            .getService();
    // Create two database clients for two different databases to ensure that we also hit two
    // different session pools.
    client1 =
        spanner.getDatabaseClient(DatabaseId.of("test-project", "test-instance", "test-database1"));
    client2 =
        spanner.getDatabaseClient(DatabaseId.of("test-project", "test-instance", "test-database2"));
  }

  @AfterClass
  public static void stopMockServer() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
  }

  @After
  public void cleanup() {
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
  }

  @Test
  public void runRandomTests() throws Exception {
    ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    // Schedule a reconnect every 100 to 200 milliseconds.
    int initialDelay = ThreadLocalRandom.current().nextInt(100) + 100;
    int period = ThreadLocalRandom.current().nextInt(100) + 100;
    reconnectExecutor.scheduleAtFixedRate(
        this::reconnectSpanner, initialDelay, period, TimeUnit.MILLISECONDS);
    ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    List<Future<?>> futures = new ArrayList<>(NUM_TESTS);
    for (int n = 0; n < NUM_TESTS; n++) {
      futures.add(executor.submit(this::runRandomTest));
    }
    executor.shutdown();
    assertTrue("Test timed out", executor.awaitTermination(120L, TimeUnit.SECONDS));
    // Verify that all futures succeeded.
    for (Future<?> future : futures) {
      assertNull(future.get());
    }
    reconnectExecutor.shutdown();
    assertTrue("Spanner should have been reconnected at least once", reconnectCount.get() > 0);
  }

  void reconnectSpanner() {
    ((SpannerImpl) spanner).getRpc().createStubs(false);
    reconnectCount.incrementAndGet();
  }

  interface TestRunnable {
    void run(DatabaseClient client);
  }

  private final ImmutableList<TestRunnable> tests =
      ImmutableList.of(
          this::testSelect1,
          this::testSelectFixedRandomStatement,
          this::testSelectRandomStatement,
          this::testSimpleInsert);

  void runRandomTest() {
    DatabaseClient client = ThreadLocalRandom.current().nextBoolean() ? client1 : client2;
    TestRunnable test = tests.get(ThreadLocalRandom.current().nextInt(tests.size()));
    test.run(client);
  }

  void testSelect1(DatabaseClient client) {
    try (ResultSet resultSet = client.singleUse().executeQuery(SELECT1_STATEMENT)) {
      assertTrue(resultSet.next());
      assertEquals(1L, resultSet.getLong(0));
      assertFalse(resultSet.next());
    }
  }

  void testSelectFixedRandomStatement(DatabaseClient client) {
    int rowCount = 0;
    try (ResultSet resultSet = client.singleUse().executeQuery(SELECT_RANDOM_STATEMENT)) {
      while (resultSet.next()) {
        rowCount++;
      }
    }
    assertEquals(NUM_RANDOM_ROWS, rowCount);
  }

  void testSelectRandomStatement(DatabaseClient client) {
    int numRows = ThreadLocalRandom.current().nextInt(1000) + 1;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(numRows);
    Statement statement =
        Statement.newBuilder("select * from random_table where value=@value")
            .bind("value")
            .to(UUID.randomUUID().toString())
            .build();
    mockSpanner.putStatementResult(StatementResult.query(statement, generator.generate()));
    int rowCount = 0;
    try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        rowCount++;
      }
    }
    assertEquals(numRows, rowCount);
    mockSpanner.removeStatementResult(statement);
  }

  void testSimpleInsert(DatabaseClient client) {
    assertEquals(
        Long.valueOf(1L),
        client
            .readWriteTransaction()
            .run(transaction -> transaction.executeUpdate(INSERT_STATEMENT)));
  }
}
