/*
 * Copyright 2021 Google LLC
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

import static org.junit.Assert.assertEquals;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that the Spanner client opens multiple channels, and that each session is associated with
 * one specific channel.
 */
@RunWith(Parameterized.class)
public class ChannelUsageTest {

  @Parameter(0)
  public int numChannels;

  @Parameter(1)
  public boolean enableGcpPool;

  @Parameters(name = "num channels = {0}, enable GCP pool = {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {{1, true}, {1, false}, {2, true}, {2, false}, {4, true}, {4, false}});
  }

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
  private static final Set<InetSocketAddress> batchCreateSessionLocalIps =
      ConcurrentHashMap.newKeySet();
  private static final Set<InetSocketAddress> executeSqlLocalIps = ConcurrentHashMap.newKeySet();

  @BeforeClass
  public static void startServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.query(SELECT1, SELECT1_RESULTSET));

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
                                .filter(k -> k.toString().equals("remote-addr"))
                                .findFirst()
                                .orElse(null);
                    if (key != null) {
                      if (call.getMethodDescriptor()
                          .equals(SpannerGrpc.getBatchCreateSessionsMethod())) {
                        batchCreateSessionLocalIps.add(attributes.get(key));
                      }
                      if (call.getMethodDescriptor()
                          .equals(SpannerGrpc.getExecuteStreamingSqlMethod())) {
                        executeSqlLocalIps.add(attributes.get(key));
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

  @After
  public void reset() {
    mockSpanner.reset();
    batchCreateSessionLocalIps.clear();
    executeSqlLocalIps.clear();
  }

  private SpannerOptions createSpannerOptions() {
    String endpoint = address.getHostString() + ":" + server.getPort();
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelConfigurator(
                input -> {
                  input.usePlaintext();
                  return input;
                })
            .setNumChannels(numChannels)
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setMinSessions(numChannels * 2)
                    .setMaxSessions(numChannels * 2)
                    .build())
            .setHost("http://" + endpoint)
            .setCredentials(NoCredentials.getInstance());
    if (enableGcpPool) {
      builder.enableGrpcGcpExtension();
    }

    return builder.build();
  }

  @Test
  public void testCreatesNumChannels() {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (ResultSet resultSet = client.singleUse().executeQuery(SELECT1)) {
        while (resultSet.next()) {}
      }
    }
    assertEquals(numChannels, batchCreateSessionLocalIps.size());
  }

  @Test
  public void testUsesAllChannels() throws InterruptedException, ExecutionException {
    try (Spanner spanner = createSpannerOptions().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      ListeningExecutorService executor =
          MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numChannels * 2));
      CountDownLatch latch = new CountDownLatch(numChannels * 2);
      List<ListenableFuture<Boolean>> futures = new ArrayList<>(numChannels * 2);
      for (int run = 0; run < numChannels * 2; run++) {
        futures.add(
            executor.submit(
                () -> {
                  try (ReadOnlyTransaction transaction = client.readOnlyTransaction()) {
                    try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
                      while (resultSet.next()) {}
                      latch.countDown();
                      try {
                        return latch.await(10L, TimeUnit.SECONDS);
                      } catch (InterruptedException e) {
                        throw SpannerExceptionFactory.asSpannerException(e);
                      }
                    }
                  }
                }));
      }
      assertEquals(numChannels * 2, Futures.allAsList(futures).get().size());
    }
    assertEquals(numChannels, executeSqlLocalIps.size());
  }
}
