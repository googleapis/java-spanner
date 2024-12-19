/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static com.google.cloud.spanner.MockSpannerTestUtil.READ_COLUMN_NAMES;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_ONE_KEY_VALUE_RESULTSET;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_ONE_KEY_VALUE_STATEMENT;
import static com.google.cloud.spanner.MockSpannerTestUtil.READ_TABLE_NAME;
import static io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.SpannerGrpc;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Transaction always utilize a channel hint to ensure multiple RPCs that are part of the same
 * transaction, they go via same channel. For regular session, the hint is stored per session. For
 * multiplexed sessions this hint is stored per transaction.
 *
 * <p>The below tests assert this behavior for both kinds of sessions.
 */
@RunWith(JUnit4.class)
public class TransactionChannelHintTest {

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

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static InetSocketAddress address;
  private static final Set<InetSocketAddress> executeSqlLocalIps = ConcurrentHashMap.newKeySet();
  private static final Set<InetSocketAddress> beginTransactionLocalIps =
      ConcurrentHashMap.newKeySet();
  private static final Set<InetSocketAddress> streamingReadLocalIps = ConcurrentHashMap.newKeySet();
  private static Level originalLogLevel;

  @BeforeClass
  public static void startServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.query(READ_ONE_KEY_VALUE_STATEMENT, READ_ONE_KEY_VALUE_RESULTSET));

    address = new InetSocketAddress("localhost", 0);
    server =
        NettyServerBuilder.forAddress(address)
            .addService(mockSpanner)
            // Add a server interceptor to register the remote addresses that we are seeing. This
            // indicates how many channels are used client side to communicate with the server.
            .intercept(
                new ServerInterceptor() {
                  @Override
                  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                      ServerCall<ReqT, RespT> call,
                      Metadata headers,
                      ServerCallHandler<ReqT, RespT> next) {
                    Attributes attributes = call.getAttributes();
                    @SuppressWarnings({"unchecked", "deprecation"})
                    Attributes.Key<InetSocketAddress> key =
                        (Attributes.Key<InetSocketAddress>)
                            attributes.keys().stream()
                                .filter(k -> k.equals(TRANSPORT_ATTR_REMOTE_ADDR))
                                .findFirst()
                                .orElse(null);
                    if (key != null) {
                      if (call.getMethodDescriptor()
                          .equals(SpannerGrpc.getExecuteStreamingSqlMethod())) {
                        executeSqlLocalIps.add(attributes.get(key));
                      }
                      if (call.getMethodDescriptor().equals(SpannerGrpc.getStreamingReadMethod())) {
                        streamingReadLocalIps.add(attributes.get(key));
                      }
                      if (call.getMethodDescriptor()
                          .equals(SpannerGrpc.getBeginTransactionMethod())) {
                        beginTransactionLocalIps.add(attributes.get(key));
                      }
                    }
                    return Contexts.interceptCall(Context.current(), call, headers, next);
                  }
                })
            .build()
            .start();
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
  }

  @BeforeClass
  public static void disableLogging() {
    Logger logger = Logger.getLogger("");
    originalLogLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
  }

  @AfterClass
  public static void resetLogging() {
    Logger logger = Logger.getLogger("");
    logger.setLevel(originalLogLevel);
  }

  @After
  public void reset() {
    mockSpanner.reset();
    executeSqlLocalIps.clear();
    streamingReadLocalIps.clear();
    beginTransactionLocalIps.clear();
  }

  private SpannerOptions createSpannerOptions() {
    String endpoint = address.getHostString() + ":" + server.getPort();
    return SpannerOptions.newBuilder()
        .setProjectId("[PROJECT]")
        .setChannelConfigurator(
            input -> {
              input.usePlaintext();
              return input;
            })
        .setCompressorName("gzip")
        .setHost("http://" + endpoint)
        .setCredentials(NoCredentials.getInstance())
        .setSessionPoolOption(
            SessionPoolOptions.newBuilder().setSkipVerifyingBeginTransactionForMuxRW(true).build())
        .build();
  }

  @Test
  public void testSingleUseReadOnlyTransaction_usesSingleChannel() {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (ResultSet resultSet = client.singleUseReadOnlyTransaction().executeQuery(SELECT1)) {
        while (resultSet.next()) {}
      }
    }
    assertEquals(1, executeSqlLocalIps.size());
  }

  @Test
  public void testSingleUseReadOnlyTransaction_withTimestampBound_usesSingleChannel() {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (ResultSet resultSet =
          client
              .singleUseReadOnlyTransaction(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))
              .executeQuery(SELECT1)) {
        while (resultSet.next()) {}
      }
    }
    assertEquals(1, executeSqlLocalIps.size());
  }

  @Test
  public void testReadOnlyTransaction_usesSingleChannel() {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (ReadOnlyTransaction transaction = client.readOnlyTransaction()) {
        try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
          while (resultSet.next()) {}
        }
        try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
          while (resultSet.next()) {}
        }
      }
    }
    assertEquals(1, executeSqlLocalIps.size());
    assertEquals(1, beginTransactionLocalIps.size());
    assertEquals(executeSqlLocalIps, beginTransactionLocalIps);
  }

  @Test
  public void testReadOnlyTransaction_withTimestampBound_usesSingleChannel() {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (ReadOnlyTransaction transaction =
          client.readOnlyTransaction(TimestampBound.ofExactStaleness(15L, TimeUnit.SECONDS))) {
        try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
          while (resultSet.next()) {}
        }
        try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
          while (resultSet.next()) {}
        }
      }
    }
    assertEquals(1, executeSqlLocalIps.size());
    assertEquals(1, beginTransactionLocalIps.size());
    assertEquals(executeSqlLocalIps, beginTransactionLocalIps);
  }

  @Test
  public void testTransactionManager_usesSingleChannel() {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (TransactionManager manager = client.transactionManager()) {
        TransactionContext transaction = manager.begin();
        while (true) {
          try {
            try (ResultSet resultSet =
                transaction.analyzeQuery(SELECT1, QueryAnalyzeMode.PROFILE)) {
              while (resultSet.next()) {}
            }

            try (ResultSet resultSet =
                transaction.analyzeQuery(SELECT1, QueryAnalyzeMode.PROFILE)) {
              while (resultSet.next()) {}
            }
            manager.commit();
            assertNotNull(manager.getCommitTimestamp());
            break;
          } catch (AbortedException e) {
            transaction = manager.resetForRetry();
          }
        }
      }
    }
    assertEquals(1, executeSqlLocalIps.size());
  }

  @Test
  public void testTransactionRunner_usesSingleChannel() {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      TransactionRunner runner = client.readWriteTransaction();
      runner.run(
          transaction -> {
            try (ResultSet resultSet =
                transaction.read(
                    READ_TABLE_NAME,
                    KeySet.singleKey(Key.of(1L)),
                    READ_COLUMN_NAMES,
                    Options.priority(RpcPriority.HIGH))) {
              while (resultSet.next()) {}
            }

            try (ResultSet resultSet =
                transaction.read(
                    READ_TABLE_NAME,
                    KeySet.singleKey(Key.of(1L)),
                    READ_COLUMN_NAMES,
                    Options.priority(RpcPriority.HIGH))) {
              while (resultSet.next()) {}
            }
            return null;
          });
    }
    assertEquals(1, streamingReadLocalIps.size());
  }
}
