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

import static com.google.cloud.spanner.DisableDefaultMtlsProvider.disableDefaultMtlsProvider;
import static io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
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
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  private static final Set<InetSocketAddress> executeSqlLocalIps = ConcurrentHashMap.newKeySet();

  private static Level originalLogLevel;

  @BeforeClass
  public static void startServer() throws Exception {
    disableDefaultMtlsProvider();
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
                    // Verify that the compressor name header is set.
                    assertEquals(
                        "gzip",
                        headers.get(
                            Metadata.Key.of(
                                "x-response-encoding", Metadata.ASCII_STRING_MARSHALLER)));
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
            .setCompressorName("gzip")
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
  public void testUsesAllChannels() throws InterruptedException {
    final int multiplier = 10;
    try (Spanner spanner = createSpannerOptions().getService()) {
      assumeFalse("GRPC-GCP is currently not supported with multiplexed sessions", enableGcpPool);
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      ListeningExecutorService executor =
          MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numChannels * multiplier));
      CountDownLatch latch = new CountDownLatch(numChannels * multiplier);
      for (int run = 0; run < numChannels * multiplier; run++) {
        executor.submit(
            () -> {
              // Use a multi-use read-only transaction to make sure we keep a session in use for
              // a longer period of time.
              try (ReadOnlyTransaction transaction = client.readOnlyTransaction()) {
                try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
                  while (resultSet.next()) {}
                }
                latch.countDown();
                // Wait here until we now that all threads have reached this point and have a
                // session in use.
                latch.await();
                try (ResultSet resultSet = transaction.executeQuery(SELECT1)) {
                  while (resultSet.next()) {}
                }
              }
              return true;
            });
      }
      executor.shutdown();
      assertTrue(executor.awaitTermination(Duration.ofSeconds(10L)));
    }
    assertEquals(numChannels, executeSqlLocalIps.size());
  }
}
