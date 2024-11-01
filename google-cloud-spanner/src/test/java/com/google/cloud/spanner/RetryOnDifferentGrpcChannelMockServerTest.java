/*
 * Copyright 2024 Google LLC
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

import static io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.common.collect.ImmutableSet;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class RetryOnDifferentGrpcChannelMockServerTest extends AbstractMockServerTest {
  private static final Map<String, Set<InetSocketAddress>> SERVER_ADDRESSES = new HashMap<>();

  @BeforeClass
  public static void startStaticServer() throws IOException {
    System.setProperty("spanner.retry_deadline_exceeded_on_different_channel", "true");
    startStaticServer(createServerInterceptor());
  }

  @AfterClass
  public static void removeSystemProperty() {
    System.clearProperty("spanner.retry_deadline_exceeded_on_different_channel");
  }

  @After
  public void clearRequests() {
    SERVER_ADDRESSES.clear();
    mockSpanner.clearRequests();
    mockSpanner.removeAllExecutionTimes();
  }

  static ServerInterceptor createServerInterceptor() {
    return new ServerInterceptor() {
      @Override
      public <ReqT, RespT> Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> serverCall,
          Metadata metadata,
          ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Attributes attributes = serverCall.getAttributes();
        //noinspection unchecked,deprecation
        Attributes.Key<InetSocketAddress> key =
            (Attributes.Key<InetSocketAddress>)
                attributes.keys().stream()
                    .filter(k -> k.equals(TRANSPORT_ATTR_REMOTE_ADDR))
                    .findFirst()
                    .orElse(null);
        if (key != null) {
          InetSocketAddress address = attributes.get(key);
          synchronized (SERVER_ADDRESSES) {
            Set<InetSocketAddress> addresses =
                SERVER_ADDRESSES.getOrDefault(
                    serverCall.getMethodDescriptor().getFullMethodName(), new HashSet<>());
            addresses.add(address);
            SERVER_ADDRESSES.putIfAbsent(
                serverCall.getMethodDescriptor().getFullMethodName(), addresses);
          }
        }
        return serverCallHandler.startCall(serverCall, metadata);
      }
    };
  }

  SpannerOptions.Builder createSpannerOptionsBuilder() {
    return SpannerOptions.newBuilder()
        .setProjectId("my-project")
        .setHost(String.format("http://localhost:%d", getPort()))
        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
        .setCredentials(NoCredentials.getInstance());
  }

  @Test
  public void testReadWriteTransaction_retriesOnNewChannel() {
    SpannerOptions.Builder builder = createSpannerOptionsBuilder();
    builder.setSessionPoolOption(
        SessionPoolOptions.newBuilder().setWaitForMinSessions(Duration.ofSeconds(5L)).build());
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofStickyException(Status.DEADLINE_EXCEEDED.asRuntimeException()));
    AtomicInteger attempts = new AtomicInteger();

    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      client
          .readWriteTransaction()
          .run(
              transaction -> {
                if (attempts.incrementAndGet() > 1) {
                  mockSpanner.setBeginTransactionExecutionTime(
                      MockSpannerServiceImpl.NO_EXECUTION_TIME);
                }
                transaction.buffer(Mutation.newInsertBuilder("foo").set("id").to(1L).build());
                return null;
              });
    }
    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    List<BeginTransactionRequest> requests =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertNotEquals(requests.get(0).getSession(), requests.get(1).getSession());
    assertEquals(
        2,
        SERVER_ADDRESSES
            .getOrDefault("google.spanner.v1.Spanner/BeginTransaction", ImmutableSet.of())
            .size());
  }

  @Test
  public void testReadWriteTransaction_stopsRetrying() {
    SpannerOptions.Builder builder = createSpannerOptionsBuilder();
    builder.setSessionPoolOption(
        SessionPoolOptions.newBuilder().setWaitForMinSessions(Duration.ofSeconds(5L)).build());
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofStickyException(Status.DEADLINE_EXCEEDED.asRuntimeException()));

    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      SpannerException exception =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction -> {
                            transaction.buffer(
                                Mutation.newInsertBuilder("foo").set("id").to(1L).build());
                            return null;
                          }));
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());

      int numChannels = spanner.getOptions().getNumChannels();
      assertEquals(numChannels, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
      List<BeginTransactionRequest> requests =
          mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
      Set<String> sessions =
          requests.stream().map(BeginTransactionRequest::getSession).collect(Collectors.toSet());
      assertEquals(numChannels, sessions.size());
      assertEquals(
          numChannels,
          SERVER_ADDRESSES
              .getOrDefault("google.spanner.v1.Spanner/BeginTransaction", ImmutableSet.of())
              .size());
    }
  }

  @Test
  public void testDenyListedChannelIsCleared() {
    FakeClock clock = new FakeClock();
    SpannerOptions.Builder builder = createSpannerOptionsBuilder();
    builder.setSessionPoolOption(
        SessionPoolOptions.newBuilder()
            .setWaitForMinSessions(Duration.ofSeconds(5))
            .setPoolMaintainerClock(clock)
            .build());
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofStickyException(Status.DEADLINE_EXCEEDED.asRuntimeException()));

    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

      // Retry until all channels have been deny-listed.
      SpannerException exception =
          assertThrows(
              SpannerException.class,
              () ->
                  client
                      .readWriteTransaction()
                      .run(
                          transaction -> {
                            transaction.buffer(
                                Mutation.newInsertBuilder("foo").set("id").to(1L).build());
                            return null;
                          }));
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());

      // Now advance the clock by 1 minute. This should clear all deny-listed channels.
      clock.currentTimeMillis.addAndGet(TimeUnit.MILLISECONDS.convert(2L, TimeUnit.MINUTES));
      AtomicInteger attempts = new AtomicInteger();
      client
          .readWriteTransaction()
          .run(
              transaction -> {
                if (attempts.incrementAndGet() > 1) {
                  mockSpanner.setBeginTransactionExecutionTime(SimulatedExecutionTime.none());
                }
                transaction.buffer(Mutation.newInsertBuilder("foo").set("id").to(1L).build());
                return null;
              });

      int numChannels = spanner.getOptions().getNumChannels();
      // We should have numChannels BeginTransactionRequests from the first transaction, and 2 from
      // the second transaction.
      assertEquals(numChannels + 2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
      List<BeginTransactionRequest> requests =
          mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
      // The requests should all use different sessions, as deny-listing a session will bring it to
      // the back of the session pool.
      Set<String> sessions =
          requests.stream().map(BeginTransactionRequest::getSession).collect(Collectors.toSet());
      // We should have used numChannels+1==5 sessions. The reason for that is that first 3 attempts
      // of the first transaction used 3 different sessions, that were then all deny-listed. The
      // 4th attempt also failed, but as it would be the last channel to be deny-listed, it was not
      // deny-listed and instead added to the front of the pool.
      // The first attempt of the second transaction then uses the same session as the last attempt
      // of the first transaction. That fails, the session is deny-listed, the transaction is
      // retried on yet another session and succeeds.
      assertEquals(numChannels + 1, sessions.size());
      assertEquals(
          numChannels,
          SERVER_ADDRESSES
              .getOrDefault("google.spanner.v1.Spanner/BeginTransaction", ImmutableSet.of())
              .size());
      assertEquals(numChannels, mockSpanner.countRequestsOfType(BatchCreateSessionsRequest.class));
    }
  }

  @Test
  public void testSingleUseQuery_retriesOnNewChannel() {
    SpannerOptions.Builder builder = createSpannerOptionsBuilder();
    builder.setSessionPoolOption(
        SessionPoolOptions.newBuilder().setUseMultiplexedSession(true).build());
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(Status.DEADLINE_EXCEEDED.asRuntimeException()));

    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (ResultSet resultSet = client.singleUse().executeQuery(SELECT1_STATEMENT)) {
        assertTrue(resultSet.next());
        assertEquals(1L, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
    }
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
    // The requests use the same multiplexed session.
    assertEquals(requests.get(0).getSession(), requests.get(1).getSession());
    // The requests use two different gRPC channels.
    assertEquals(
        2,
        SERVER_ADDRESSES
            .getOrDefault("google.spanner.v1.Spanner/ExecuteStreamingSql", ImmutableSet.of())
            .size());
  }

  @Test
  public void testSingleUseQuery_stopsRetrying() {
    SpannerOptions.Builder builder = createSpannerOptionsBuilder();
    builder.setSessionPoolOption(
        SessionPoolOptions.newBuilder().setUseMultiplexedSession(true).build());
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofStickyException(Status.DEADLINE_EXCEEDED.asRuntimeException()));

    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      try (ResultSet resultSet = client.singleUse().executeQuery(SELECT1_STATEMENT)) {
        SpannerException exception = assertThrows(SpannerException.class, resultSet::next);
        assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());
      }
      int numChannels = spanner.getOptions().getNumChannels();
      assertEquals(numChannels, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
      List<ExecuteSqlRequest> requests = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class);
      // The requests use the same multiplexed session.
      String session = requests.get(0).getSession();
      for (ExecuteSqlRequest request : requests) {
        assertEquals(session, request.getSession());
      }
      // The requests use all gRPC channels.
      assertEquals(
          numChannels,
          SERVER_ADDRESSES
              .getOrDefault("google.spanner.v1.Spanner/ExecuteStreamingSql", ImmutableSet.of())
              .size());
    }
  }

  @Test
  public void testReadWriteTransaction_withGrpcContextDeadline_doesNotRetry() {
    SpannerOptions.Builder builder = createSpannerOptionsBuilder();
    builder.setSessionPoolOption(
        SessionPoolOptions.newBuilder().setWaitForMinSessions(Duration.ofSeconds(5L)).build());
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(500, 500));

    try (Spanner spanner = builder.build().getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
      Context context =
          Context.current().withDeadline(Deadline.after(50L, TimeUnit.MILLISECONDS), service);
      SpannerException exception =
          assertThrows(
              SpannerException.class,
              () ->
                  context.run(
                      () ->
                          client
                              .readWriteTransaction()
                              .run(
                                  transaction -> {
                                    transaction.buffer(
                                        Mutation.newInsertBuilder("foo").set("id").to(1L).build());
                                    return null;
                                  })));
      assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());
    }
    // A gRPC context deadline will still cause the underlying error handler to try to retry the
    // transaction on a new channel, but as the deadline has been exceeded even before those RPCs
    // are being executed, the RPC invocation will be skipped, and the error will eventually bubble
    // up.
    assertEquals(1, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
  }
}
