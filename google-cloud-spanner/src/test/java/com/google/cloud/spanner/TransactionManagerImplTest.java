/*
 * Copyright 2017 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.Transaction;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class TransactionManagerImplTest {
  private static final class TestExecutorFactory
      implements ExecutorFactory<ScheduledExecutorService> {
    @Override
    public ScheduledExecutorService get() {
      return Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void release(ScheduledExecutorService exec) {
      exec.shutdown();
    }
  }

  @Mock private SessionImpl session;
  @Mock TransactionRunnerImpl.TransactionContextImpl txn;
  private TransactionManagerImpl manager;

  @BeforeClass
  public static void setupOpenTelemetry() {
    SpannerOptions.resetActiveTracingFramework();
    SpannerOptions.enableOpenTelemetryTraces();
  }

  @Before
  public void setUp() {
    initMocks(this);
    manager =
        new TransactionManagerImpl(
            session,
            new OpenTelemetrySpan(mock(io.opentelemetry.api.trace.Span.class)),
            mock(TraceWrapper.class));
  }

  @Test
  public void beginCalledTwiceFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    assertThat(manager.begin()).isEqualTo(txn);
    assertThat(manager.getState()).isEqualTo(TransactionState.STARTED);
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> manager.begin());
    assertNotNull(e.getMessage());
  }

  @Test
  public void commitBeforeBeginFails() {
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> manager.commit());
    assertNotNull(e.getMessage());
  }

  @Test
  public void rollbackBeforeBeginFails() {
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> manager.rollback());
    assertNotNull(e.getMessage());
  }

  @Test
  public void resetBeforeBeginFails() {
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> manager.resetForRetry());
    assertNotNull(e.getMessage());
  }

  @Test
  public void transactionRolledBackOnClose() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    when(txn.isAborted()).thenReturn(false);
    manager.begin();
    manager.close();
    verify(txn).rollback();
  }

  @Test
  public void commitSucceeds() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    Timestamp commitTimestamp = Timestamp.ofTimeMicroseconds(1);
    CommitResponse response = new CommitResponse(commitTimestamp);
    when(txn.getCommitResponse()).thenReturn(response);
    manager.begin();
    manager.commit();
    assertThat(manager.getState()).isEqualTo(TransactionState.COMMITTED);
    assertThat(manager.getCommitTimestamp()).isEqualTo(commitTimestamp);
  }

  @Test
  public void resetAfterSuccessfulCommitFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    manager.commit();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> manager.resetForRetry());
    assertNotNull(e.getMessage());
  }

  @Test
  public void resetAfterAbortSucceeds() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    doThrow(SpannerExceptionFactory.newSpannerException(ErrorCode.ABORTED, "")).when(txn).commit();
    assertThrows(AbortedException.class, () -> manager.commit());
    assertEquals(TransactionState.ABORTED, manager.getState());

    txn = Mockito.mock(TransactionRunnerImpl.TransactionContextImpl.class);
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    assertThat(manager.resetForRetry()).isEqualTo(txn);
    assertThat(manager.getState()).isEqualTo(TransactionState.STARTED);
  }

  @Test
  public void resetAfterErrorFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    doThrow(SpannerExceptionFactory.newSpannerException(ErrorCode.UNKNOWN, "")).when(txn).commit();
    SpannerException e = assertThrows(SpannerException.class, () -> manager.commit());
    assertEquals(ErrorCode.UNKNOWN, e.getErrorCode());

    IllegalStateException illegal =
        assertThrows(IllegalStateException.class, () -> manager.resetForRetry());
    assertNotNull(illegal.getMessage());
  }

  @Test
  public void rollbackAfterCommitFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    manager.commit();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> manager.rollback());
    assertNotNull(e.getMessage());
  }

  @Test
  public void commitAfterRollbackFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    manager.rollback();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> manager.commit());
    assertNotNull(e.getMessage());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void usesPreparedTransaction() {
    SpannerOptions options = mock(SpannerOptions.class);
    when(options.getNumChannels()).thenReturn(4);
    GrpcTransportOptions transportOptions = mock(GrpcTransportOptions.class);
    when(transportOptions.getExecutorFactory()).thenReturn(new TestExecutorFactory());
    when(options.getTransportOptions()).thenReturn(transportOptions);
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder().setMinSessions(0).setIncStep(1).build();
    when(options.getSessionPoolOptions()).thenReturn(sessionPoolOptions);
    when(options.getSessionLabels()).thenReturn(Collections.emptyMap());
    when(options.getDatabaseRole()).thenReturn("role");
    when(options.getOpenTelemetry()).thenReturn(OpenTelemetry.noop());
    SpannerRpc rpc = mock(SpannerRpc.class);
    when(rpc.asyncDeleteSession(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
    when(rpc.batchCreateSessions(
            Mockito.anyString(),
            Mockito.eq(1),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyMap()))
        .thenAnswer(
            invocation ->
                Collections.singletonList(
                    Session.newBuilder()
                        .setName(invocation.getArguments()[0] + "/sessions/1")
                        .setCreateTime(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build()));
    when(rpc.beginTransactionAsync(
            Mockito.any(BeginTransactionRequest.class), Mockito.anyMap(), eq(true)))
        .thenAnswer(
            invocation ->
                ApiFutures.immediateFuture(
                    Transaction.newBuilder()
                        .setId(ByteString.copyFromUtf8(UUID.randomUUID().toString()))
                        .build()));
    when(rpc.commitAsync(Mockito.any(CommitRequest.class), Mockito.anyMap()))
        .thenAnswer(
            invocation ->
                ApiFutures.immediateFuture(
                    com.google.spanner.v1.CommitResponse.newBuilder()
                        .setCommitTimestamp(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build()));
    when(rpc.getCommitRetrySettings())
        .thenReturn(SpannerStubSettings.newBuilder().commitSettings().getRetrySettings());
    DatabaseId db = DatabaseId.of("test", "test", "test");
    try (SpannerImpl spanner = new SpannerImpl(rpc, options)) {
      DatabaseClient client = spanner.getDatabaseClient(db);
      try (TransactionManager mgr = client.transactionManager()) {
        mgr.begin();
        mgr.commit();
      }
      verify(rpc, times(1))
          .beginTransactionAsync(
              Mockito.any(BeginTransactionRequest.class), Mockito.anyMap(), eq(true));
    }
  }

  @SuppressWarnings({"unchecked", "resource"})
  @Test
  public void inlineBegin() {
    SpannerOptions options = mock(SpannerOptions.class);
    when(options.getNumChannels()).thenReturn(4);
    GrpcTransportOptions transportOptions = mock(GrpcTransportOptions.class);
    when(transportOptions.getExecutorFactory()).thenReturn(new TestExecutorFactory());
    when(options.getTransportOptions()).thenReturn(transportOptions);
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder().setMinSessions(0).setIncStep(1).build();
    when(options.getSessionPoolOptions()).thenReturn(sessionPoolOptions);
    when(options.getSessionLabels()).thenReturn(Collections.emptyMap());
    when(options.getDefaultQueryOptions(Mockito.any(DatabaseId.class)))
        .thenReturn(QueryOptions.getDefaultInstance());
    when(options.getOpenTelemetry()).thenReturn(OpenTelemetry.noop());
    SpannerRpc rpc = mock(SpannerRpc.class);
    when(rpc.asyncDeleteSession(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
    when(options.getDatabaseRole()).thenReturn("role");
    when(rpc.batchCreateSessions(
            Mockito.anyString(),
            Mockito.eq(1),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyMap()))
        .thenAnswer(
            invocation ->
                Collections.singletonList(
                    Session.newBuilder()
                        .setName(invocation.getArguments()[0] + "/sessions/1")
                        .setCreateTime(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build()));
    when(rpc.beginTransactionAsync(
            Mockito.any(BeginTransactionRequest.class), Mockito.anyMap(), eq(true)))
        .thenAnswer(
            invocation ->
                ApiFutures.immediateFuture(
                    Transaction.newBuilder()
                        .setId(ByteString.copyFromUtf8(UUID.randomUUID().toString()))
                        .build()));
    final AtomicInteger transactionsStarted = new AtomicInteger();
    when(rpc.executeQuery(Mockito.any(ExecuteSqlRequest.class), Mockito.anyMap(), eq(true)))
        .thenAnswer(
            invocation -> {
              ResultSet.Builder builder =
                  ResultSet.newBuilder()
                      .setStats(ResultSetStats.newBuilder().setRowCountExact(1L).build());
              ExecuteSqlRequest request = invocation.getArgument(0, ExecuteSqlRequest.class);
              if (request.getTransaction() != null && request.getTransaction().hasBegin()) {
                transactionsStarted.incrementAndGet();
                builder.setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setTransaction(
                            Transaction.newBuilder()
                                .setId(ByteString.copyFromUtf8("test-tx"))
                                .build())
                        .build());
              }
              return builder.build();
            });
    when(rpc.commitAsync(Mockito.any(CommitRequest.class), Mockito.anyMap()))
        .thenAnswer(
            invocation ->
                ApiFutures.immediateFuture(
                    com.google.spanner.v1.CommitResponse.newBuilder()
                        .setCommitTimestamp(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build()));
    when(rpc.getCommitRetrySettings())
        .thenReturn(SpannerStubSettings.newBuilder().commitSettings().getRetrySettings());
    DatabaseId db = DatabaseId.of("test", "test", "test");
    try (SpannerImpl spanner = new SpannerImpl(rpc, options)) {
      DatabaseClient client = spanner.getDatabaseClient(db);
      try (TransactionManager mgr = client.transactionManager()) {
        TransactionContext tx = mgr.begin();
        while (true) {
          try {
            tx.executeUpdate(Statement.of("UPDATE FOO SET BAR=1"));
            tx.executeUpdate(Statement.of("UPDATE FOO SET BAZ=2"));
            mgr.commit();
            break;
          } catch (AbortedException e) {
            tx = mgr.resetForRetry();
          }
        }
      }
      // BeginTransaction should not be called, as we are inlining it with the ExecuteSql request.
      verify(rpc, Mockito.never())
          .beginTransaction(Mockito.any(BeginTransactionRequest.class), Mockito.anyMap(), eq(true));
      // We should have 2 ExecuteSql requests.
      verify(rpc, times(2))
          .executeQuery(Mockito.any(ExecuteSqlRequest.class), Mockito.anyMap(), eq(true));
      // But only 1 with a BeginTransaction.
      assertThat(transactionsStarted.get()).isEqualTo(1);
    }
  }
}
