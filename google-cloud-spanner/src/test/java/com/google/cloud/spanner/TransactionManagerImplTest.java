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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CommitResponse;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.Transaction;
import io.opencensus.trace.Span;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

  @Before
  public void setUp() {
    initMocks(this);
    manager = new TransactionManagerImpl(session, mock(Span.class));
  }

  @Test
  public void beginCalledTwiceFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    assertThat(manager.begin()).isEqualTo(txn);
    assertThat(manager.getState()).isEqualTo(TransactionState.STARTED);
    try {
      manager.begin();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void commitBeforeBeginFails() {
    try {
      manager.commit();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void rollbackBeforeBeginFails() {
    try {
      manager.rollback();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void resetBeforeBeginFails() {
    try {
      manager.resetForRetry();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
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
    when(txn.commitTimestamp()).thenReturn(commitTimestamp);
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
    try {
      manager.resetForRetry();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void resetAfterAbortSucceeds() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    doThrow(SpannerExceptionFactory.newSpannerException(ErrorCode.ABORTED, "")).when(txn).commit();
    try {
      manager.commit();
      fail("Expected AbortedException");
    } catch (AbortedException e) {
      assertThat(manager.getState()).isEqualTo(TransactionState.ABORTED);
    }
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
    try {
      manager.commit();
      fail("Expected AbortedException");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNKNOWN);
    }
    try {
      manager.resetForRetry();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void rollbackAfterCommitFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    manager.commit();
    try {
      manager.rollback();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void commitAfterRollbackFails() {
    when(session.newTransaction(Options.fromTransactionOptions())).thenReturn(txn);
    manager.begin();
    manager.rollback();
    try {
      manager.commit();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertNotNull(ex.getMessage());
    }
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
    when(options.getSessionLabels()).thenReturn(Collections.<String, String>emptyMap());
    SpannerRpc rpc = mock(SpannerRpc.class);
    when(rpc.asyncDeleteSession(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
    when(rpc.batchCreateSessions(
            Mockito.anyString(), Mockito.eq(1), Mockito.anyMap(), Mockito.anyMap()))
        .thenAnswer(
            new Answer<List<com.google.spanner.v1.Session>>() {
              @Override
              public List<com.google.spanner.v1.Session> answer(InvocationOnMock invocation) {
                return Arrays.asList(
                    com.google.spanner.v1.Session.newBuilder()
                        .setName((String) invocation.getArguments()[0] + "/sessions/1")
                        .setCreateTime(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build());
              }
            });
    when(rpc.beginTransactionAsync(Mockito.any(BeginTransactionRequest.class), Mockito.anyMap()))
        .thenAnswer(
            new Answer<ApiFuture<Transaction>>() {
              @Override
              public ApiFuture<Transaction> answer(InvocationOnMock invocation) {
                return ApiFutures.immediateFuture(
                    Transaction.newBuilder()
                        .setId(ByteString.copyFromUtf8(UUID.randomUUID().toString()))
                        .build());
              }
            });
    when(rpc.commitAsync(Mockito.any(CommitRequest.class), Mockito.anyMap()))
        .thenAnswer(
            new Answer<ApiFuture<CommitResponse>>() {
              @Override
              public ApiFuture<CommitResponse> answer(InvocationOnMock invocation)
                  throws Throwable {
                return ApiFutures.immediateFuture(
                    CommitResponse.newBuilder()
                        .setCommitTimestamp(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build());
              }
            });
    DatabaseId db = DatabaseId.of("test", "test", "test");
    try (SpannerImpl spanner = new SpannerImpl(rpc, options)) {
      DatabaseClient client = spanner.getDatabaseClient(db);
      try (TransactionManager mgr = client.transactionManager()) {
        mgr.begin();
        mgr.commit();
      }
      verify(rpc, times(1))
          .beginTransactionAsync(Mockito.any(BeginTransactionRequest.class), Mockito.anyMap());
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
    when(options.getSessionLabels()).thenReturn(Collections.<String, String>emptyMap());
    when(options.getDefaultQueryOptions(Mockito.any(DatabaseId.class)))
        .thenReturn(QueryOptions.getDefaultInstance());
    SpannerRpc rpc = mock(SpannerRpc.class);
    when(rpc.asyncDeleteSession(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
    when(rpc.batchCreateSessions(
            Mockito.anyString(), Mockito.eq(1), Mockito.anyMap(), Mockito.anyMap()))
        .thenAnswer(
            new Answer<List<com.google.spanner.v1.Session>>() {
              @Override
              public List<com.google.spanner.v1.Session> answer(InvocationOnMock invocation)
                  throws Throwable {
                return Arrays.asList(
                    com.google.spanner.v1.Session.newBuilder()
                        .setName((String) invocation.getArguments()[0] + "/sessions/1")
                        .setCreateTime(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build());
              }
            });
    when(rpc.beginTransactionAsync(Mockito.any(BeginTransactionRequest.class), Mockito.anyMap()))
        .thenAnswer(
            new Answer<ApiFuture<Transaction>>() {
              @Override
              public ApiFuture<Transaction> answer(InvocationOnMock invocation) throws Throwable {
                return ApiFutures.immediateFuture(
                    Transaction.newBuilder()
                        .setId(ByteString.copyFromUtf8(UUID.randomUUID().toString()))
                        .build());
              }
            });
    final AtomicInteger transactionsStarted = new AtomicInteger();
    when(rpc.executeQuery(Mockito.any(ExecuteSqlRequest.class), Mockito.anyMap()))
        .thenAnswer(
            new Answer<com.google.spanner.v1.ResultSet>() {
              @Override
              public com.google.spanner.v1.ResultSet answer(InvocationOnMock invocation)
                  throws Throwable {
                com.google.spanner.v1.ResultSet.Builder builder =
                    com.google.spanner.v1.ResultSet.newBuilder()
                        .setStats(ResultSetStats.newBuilder().setRowCountExact(1L).build());
                ExecuteSqlRequest request = invocation.getArgumentAt(0, ExecuteSqlRequest.class);
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
              }
            });
    when(rpc.commitAsync(Mockito.any(CommitRequest.class), Mockito.anyMap()))
        .thenAnswer(
            new Answer<ApiFuture<CommitResponse>>() {
              @Override
              public ApiFuture<CommitResponse> answer(InvocationOnMock invocation)
                  throws Throwable {
                return ApiFutures.immediateFuture(
                    CommitResponse.newBuilder()
                        .setCommitTimestamp(
                            com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() * 1000))
                        .build());
              }
            });
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
          .beginTransaction(Mockito.any(BeginTransactionRequest.class), Mockito.anyMap());
      // We should have 2 ExecuteSql requests.
      verify(rpc, times(2)).executeQuery(Mockito.any(ExecuteSqlRequest.class), Mockito.anyMap());
      // But only 1 with a BeginTransaction.
      assertThat(transactionsStarted.get()).isEqualTo(1);
    }
  }
}
