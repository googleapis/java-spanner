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

import static com.google.cloud.spanner.SpannerOptionsTest.runWithSystemProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.NoCredentials;
import com.google.cloud.Tuple;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.AbstractMessage;
import com.google.spanner.v1.BeginTransactionRequest;
import io.grpc.Context;
import io.grpc.Context.CancellableContext;
import io.grpc.Deadline;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BeginTransactionRetryTest {
  private static final String ENABLE_RETRY_PROPERTY =
      "com.google.cloud.spanner.retry_begin_transaction_on_new_channel";
  private static final String MAX_ATTEMPTS_PROPERTY =
      "com.google.cloud.spanner.max_attempts_begin_transaction_on_new_channel";
  private static final String RETRY_DELAY_PROPERTY =
      "com.google.cloud.spanner.retry_delay_begin_transaction_on_new_channel";
  private static final String INITIAL_RPC_TIMEOUT_PROPERTY =
      "com.google.cloud.spanner.initial_rpc_timeout_begin_transaction_on_new_channel";
  private static final String MAX_RPC_TIMEOUT_PROPERTY =
      "com.google.cloud.spanner.max_rpc_timeout_begin_transaction_on_new_channel";
  private static final String MAX_RPC_ATTEMPTS_PROPERTY =
      "com.google.cloud.spanner.max_rpc_attempts_begin_transaction_on_new_channel";

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;

  private static final Map<String, String> ORIGINAL_VALUES = new HashMap<>();

  private static void registerOriginalValue(String propertyName) {
    ORIGINAL_VALUES.put(propertyName, System.getProperty(propertyName));
  }

  @BeforeClass
  public static void setSystemProperties() {
    registerOriginalValue(ENABLE_RETRY_PROPERTY);
    registerOriginalValue(MAX_ATTEMPTS_PROPERTY);
    registerOriginalValue(RETRY_DELAY_PROPERTY);
    registerOriginalValue(INITIAL_RPC_TIMEOUT_PROPERTY);
    registerOriginalValue(MAX_RPC_TIMEOUT_PROPERTY);
    registerOriginalValue(MAX_RPC_ATTEMPTS_PROPERTY);

    System.setProperty(ENABLE_RETRY_PROPERTY, "true");
    System.setProperty(MAX_ATTEMPTS_PROPERTY, "4");
    System.setProperty(RETRY_DELAY_PROPERTY, "2");
    System.setProperty(INITIAL_RPC_TIMEOUT_PROPERTY, "10");
    System.setProperty(MAX_RPC_TIMEOUT_PROPERTY, "10");
    System.setProperty(MAX_RPC_ATTEMPTS_PROPERTY, "3");
  }

  @AfterClass
  public static void resetSystemProperties() {
    for (Entry<String, String> entry : ORIGINAL_VALUES.entrySet()) {
      if (entry.getValue() == null) {
        System.clearProperty(entry.getKey());
      } else {
        System.setProperty(entry.getKey(), entry.getValue());
      }
    }
  }

  @BeforeClass
  public static void startServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    // Use a TCP connection and a NettyServer instead of an InProcessServer, so we can verify the
    // use of different gRPC channels.
    InetSocketAddress address = new InetSocketAddress("localhost", 0);
    server = NettyServerBuilder.forAddress(address).addService(mockSpanner).build().start();
  }

  @AfterClass
  public static void stopServer() {
    server.shutdown();
  }

  @Before
  public void clearRequests() {
    mockSpanner.clearRequests();
  }

  @Test
  public void testSuccessfulRetryOnDifferentChannel() {
    // Force the BeginTransaction RPC to return an UNAVAILABLE error twice, and then to take 200ms
    // to execute successfully. We only simulate this behavior on the first remote address that we
    // see. A specific remote address is equal to a specific gRPC channel.
    mockSpanner.setBeginTransactionExecutionTime(
        new SimulatedExecutionTime(
            /* minimumExecutionTime = */ 200,
            /* randomExecutionTime = */ 0,
            /* exceptions = */ ImmutableList.of(
                Status.UNAVAILABLE.asRuntimeException(), Status.UNAVAILABLE.asRuntimeException()),
            /* stickyExceptions = */ false,
            /* streamIndices = */ Collections.emptySet()) {
          private SocketAddress firstSeenRemoteAddress;

          @Override
          protected void simulateExecutionTime(
              Queue<Exception> globalExceptions,
              boolean stickyGlobalExceptions,
              CountDownLatch freezeLock,
              @Nullable SocketAddress remoteAddress) {
            // Only simulate that the RPC is slow and unreliable on the first channel that we have
            // seen.
            Objects.requireNonNull(remoteAddress);
            if (firstSeenRemoteAddress == null) {
              firstSeenRemoteAddress = remoteAddress;
            }
            if (firstSeenRemoteAddress.equals(remoteAddress)) {
              super.simulateExecutionTime(
                  globalExceptions, stickyGlobalExceptions, freezeLock, remoteAddress);
            }
          }
        });

    // Run the test transaction with a 5 seconds gRPC context timeout. That timeout should not be
    // triggered and the transaction should succeed.
    runTestTransaction(5000);

    // We should normally see 4 attempts at BeginTransaction:
    // 1. BeginTransaction on the channel affiliated with the session -> UNAVAILABLE.
    // 2. BeginTransaction on the channel affiliated with the session -> UNAVAILABLE.
    // 3. BeginTransaction on the channel affiliated with the session -> DEADLINE_EXCEEDED.
    // 4. BeginTransaction on a new channel -> SUCCESS.
    // 'Normally 4': It can also be less than four BeginTransaction requests. This happens if the
    // 10ms timeout that we have set for the BeginTransaction RPC is reached before the mock server
    // has returned an UNAVAILABLE error in the two first cases. This can for example happen if the
    // host where we are running the test is slow. Increasing the tight deadline for
    // BeginTransaction would reduce the probability of that happening, but would also slow down the
    // test accordingly.
    List<Tuple<AbstractMessage, SocketAddress>> requests =
        mockSpanner.getRequestTuples().stream()
            .filter(requestTuple -> requestTuple.x() instanceof BeginTransactionRequest)
            .collect(Collectors.toList());
    assertTrue(
        "The server should receive at between 2 and 4 BeginTransaction requests",
        requests.size() >= 2 && requests.size() <= 4);
    // The first 1-3 requests should use the gRPC channel that is affiliated with the session.
    // These should all fail.
    // The last request should use a different gRPC channel and succeed.
    SocketAddress firstAddress = requests.get(0).y();
    assertNotNull(firstAddress);
    // The first requests should use the same channel.
    for (int index = 1; index < requests.size() - 1; index++) {
      assertEquals(firstAddress, requests.get(index).y());
    }
    // The last (successful) request should use a different channel.
    assertNotEquals(firstAddress, requests.get(requests.size() - 1).y());
  }

  @Test
  public void testContextDeadlineIsRespected() {
    // Force the BeginTransaction RPC to return an UNAVAILABLE error twice, and then to take 200ms
    // to execute successfully on all channels. This will cause the RPC to fail with a
    // DEADLINE_EXCEEDED every time after the UNAVAILABLE errors have finished being returned, as
    // the RPC timeout is 10ms.
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTimeAndExceptions(
            200,
            0,
            ImmutableList.of(
                Status.UNAVAILABLE.asRuntimeException(), Status.UNAVAILABLE.asRuntimeException())));

    // Run the test transaction with a 50 millisecond gRPC context timeout. This context timeout
    // should cause the transaction to fail.
    SpannerException exception = assertThrows(SpannerException.class, () -> runTestTransaction(50));
    assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());
    // The error message should be the specific 'context timed out' error, and not an RPC timeout
    // error.
    assertTrue(exception.getMessage(), exception.getMessage().contains("context timed out"));
    // We don't know exactly how far the test transaction came, meaning that we also don't know
    // exactly which requests we received, so we don't verify anything for that.
  }

  @Test
  public void testRetryGivesUp() {
    // Let the BeginTransaction RPC be slow all the time. This should cause a DEADLINE_EXCEEDED
    // error to be thrown from the retries on new channels.
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(200, 0));

    SpannerException exception =
        assertThrows(SpannerException.class, () -> runTestTransaction(5000));
    assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());
    // The message should be the message that we get when an RPC times out.
    assertTrue(exception.getMessage(), exception.getMessage().contains("deadline exceeded after"));
    assertFalse(exception.getMessage(), exception.getMessage().contains("context timed out"));
    // Verify that the errors bubbled up to the custom retry handling, and that we therefore
    // received BeginTransaction RPC calls on as many channels as there are attempts (4).
    List<Tuple<AbstractMessage, SocketAddress>> requests =
        mockSpanner.getRequestTuples().stream()
            .filter(requestTuple -> requestTuple.x() instanceof BeginTransactionRequest)
            .collect(Collectors.toList());
    assertFalse("The server should receive BeginTransaction requests", requests.isEmpty());
    Set<SocketAddress> remoteAddresses =
        requests.stream().map(Tuple::y).collect(Collectors.toSet());
    assertEquals(4, remoteAddresses.size());
  }

  @Category(SlowTest.class)
  @Test
  public void testLongContextDeadlineIsRespected() {
    // Force the BeginTransaction RPC to return an UNAVAILABLE error twice, and then to take 200ms
    // to execute successfully on all channels. This will cause the RPC to fail with a
    // DEADLINE_EXCEEDED every time after the UNAVAILABLE errors have finished being returned, as
    // the RPC timeout is 10ms.
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTimeAndExceptions(
            200,
            0,
            ImmutableList.of(
                Status.UNAVAILABLE.asRuntimeException(), Status.UNAVAILABLE.asRuntimeException())));

    // Run the test transaction with a 5 seconds gRPC context timeout. This context timeout
    // should cause the transaction to fail. We increase the max_attempts for the retries of
    // BeginTransaction on new channels to ensure that the transaction gets 'stuck' there until the
    // context timeout occurs.
    SpannerException exception =
        assertThrows(
            SpannerException.class,
            () ->
                runWithSystemProperty(
                    MAX_ATTEMPTS_PROPERTY, "1000", () -> runTestTransaction(5000)));
    assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());
    // The error message should be the specific 'context timed out' error, and not an RPC timeout
    // error.
    assertTrue(exception.getMessage(), exception.getMessage().contains("context timed out"));
    // Verify that the errors bubbled up to the custom retry handling, and that we therefore
    // received BeginTransaction RPC calls on all channels. As we allow up to 1,000 retries, we
    // should have an attempt on each channel.
    List<Tuple<AbstractMessage, SocketAddress>> requests =
        mockSpanner.getRequestTuples().stream()
            .filter(requestTuple -> requestTuple.x() instanceof BeginTransactionRequest)
            .collect(Collectors.toList());
    assertFalse("The server should receive BeginTransaction requests", requests.isEmpty());
    Set<SocketAddress> remoteAddresses =
        requests.stream().map(Tuple::y).collect(Collectors.toSet());
    assertEquals(SpannerOptions.DEFAULT_CHANNELS, remoteAddresses.size());
  }

  private void runTestTransaction(long contextTimeoutMillis) {
    RandomResultSetGenerator generator = new RandomResultSetGenerator(10);
    mockSpanner.putStatementResult(
        StatementResult.query(Statement.of("select * from random"), generator.generate()));

    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setHost("http://localhost:" + server.getPort())
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext);
    SpannerStubSettings.Builder settingsBuilder = builder.getSpannerStubSettingsBuilder();
    // Set a tight retry loop for BeginTransaction that will only attempt three times.
    // Only retry UNAVAILABLE errors. Other errors should bubble up.
    // The timeout for a single RPC is 10ms. The execution time of BeginTransaction is 20 ms.
    // This means that a DEADLINE_EXCEEDED will bubble up once Gax is done with
    // retrying UNAVAILABLE errors.
    //    settingsBuilder
    //        .beginTransactionSettings()
    //        .setRetryableCodes(StatusCode.Code.UNAVAILABLE)
    //        .setRetrySettings(
    //            RetrySettings.newBuilder()
    //                .setInitialRetryDelay(Duration.ofMillis(2))
    //                .setMaxRetryDelay(Duration.ofMillis(2))
    //                .setRetryDelayMultiplier(1.0)
    //                .setInitialRpcTimeout(Duration.ofMillis(10))
    //                .setMaxRpcTimeout(Duration.ofMillis(10))
    //                .setRpcTimeoutMultiplier(1.0)
    //                .setMaxAttempts(3)
    //                .setTotalTimeout(Duration.ofMillis(30000))
    //                .build());
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(
              DatabaseId.of("test-project", "test-instance", "test-database"));
      // Create a gRPC context with a deadline and run everything in that context.
      try (CancellableContext context =
          Context.current()
              .withDeadline(
                  Deadline.after(contextTimeoutMillis, TimeUnit.MILLISECONDS), executor)) {
        context.run(
            () -> {
              // Execute a query outside any transactions.
              try (ResultSet resultSet =
                  client.singleUse().executeQuery(Statement.of("select * from random"))) {
                while (resultSet.next()) {
                  // ignore
                }
              }
              // Execute a transaction that only commits a mutation. This transaction will then use
              // a
              // BeginTransaction RPC.
              try (TransactionManager transactionManager = client.transactionManager()) {
                try (TransactionContext transaction = transactionManager.begin()) {
                  transaction.buffer(
                      Mutation.newInsertBuilder("my-table").set("id").to(1L).build());
                  transactionManager.commit();
                }
              }
            });
      }
    } finally {
      executor.shutdown();
    }
  }
}
