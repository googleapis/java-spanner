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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.AbortedException;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.gax.rpc.UnavailableException;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionSelector;
import io.grpc.Status.Code;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.threeten.bp.Duration;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class PartitionedDmlTransactionTest {

  @Mock private SpannerRpc rpc;

  @Mock private SessionImpl session;

  @Mock private Ticker ticker;

  private PartitionedDmlTransaction tx;

  private final String sessionId = "projects/p/instances/i/databases/d/sessions/s";
  private final ByteString txId = ByteString.copyFromUtf8("tx");
  private final ByteString resumeToken = ByteString.copyFromUtf8("resume");
  private final String sql = "UPDATE FOO SET BAR=1 WHERE TRUE";
  private final ExecuteSqlRequest executeRequestWithoutResumeToken =
      ExecuteSqlRequest.newBuilder()
          .setQueryMode(QueryMode.NORMAL)
          .setSession(sessionId)
          .setSql(sql)
          .setTransaction(TransactionSelector.newBuilder().setId(txId))
          .build();
  private final ExecuteSqlRequest executeRequestWithResumeToken =
      executeRequestWithoutResumeToken.toBuilder().setResumeToken(resumeToken).build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(session.getName()).thenReturn(sessionId);
    when(session.getOptions()).thenReturn(Collections.EMPTY_MAP);
    when(rpc.beginTransaction(any(BeginTransactionRequest.class), anyMap()))
        .thenReturn(Transaction.newBuilder().setId(txId).build());

    tx = new PartitionedDmlTransaction(session, rpc, ticker);
  }

  @Test
  public void testExecuteStreamingPartitionedUpdate() {
    ResultSetStats stats = ResultSetStats.newBuilder().setRowCountLowerBound(1000L).build();
    PartialResultSet p1 = PartialResultSet.newBuilder().setResumeToken(resumeToken).build();
    PartialResultSet p2 = PartialResultSet.newBuilder().setStats(stats).build();
    ServerStream<PartialResultSet> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(ImmutableList.of(p1, p2).iterator());
    when(rpc.executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class)))
        .thenReturn(stream);

    long count = tx.executeStreamingPartitionedUpdate(Statement.of(sql), Duration.ofMinutes(10));

    assertThat(count).isEqualTo(1000L);
    verify(rpc).beginTransaction(any(BeginTransactionRequest.class), anyMap());
    verify(rpc)
        .executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class));
  }

  @Test
  public void testExecuteStreamingPartitionedUpdateAborted() {
    ResultSetStats stats = ResultSetStats.newBuilder().setRowCountLowerBound(1000L).build();
    PartialResultSet p1 = PartialResultSet.newBuilder().setResumeToken(resumeToken).build();
    PartialResultSet p2 = PartialResultSet.newBuilder().setStats(stats).build();
    ServerStream<PartialResultSet> stream1 = mock(ServerStream.class);
    Iterator<PartialResultSet> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true, true, false);
    when(iterator.next())
        .thenReturn(p1)
        .thenThrow(
            new AbortedException(
                "transaction aborted", null, GrpcStatusCode.of(Code.ABORTED), true));
    when(stream1.iterator()).thenReturn(iterator);
    ServerStream<PartialResultSet> stream2 = mock(ServerStream.class);
    when(stream2.iterator()).thenReturn(ImmutableList.of(p1, p2).iterator());
    when(rpc.executeStreamingPartitionedDml(
            any(ExecuteSqlRequest.class), anyMap(), any(Duration.class)))
        .thenReturn(stream1, stream2);

    long count = tx.executeStreamingPartitionedUpdate(Statement.of(sql), Duration.ofMinutes(10));

    assertThat(count).isEqualTo(1000L);
    verify(rpc, times(2)).beginTransaction(any(BeginTransactionRequest.class), anyMap());
    verify(rpc, times(2))
        .executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class));
  }

  @Test
  public void testExecuteStreamingPartitionedUpdateUnavailable() {
    ResultSetStats stats = ResultSetStats.newBuilder().setRowCountLowerBound(1000L).build();
    PartialResultSet p1 = PartialResultSet.newBuilder().setResumeToken(resumeToken).build();
    PartialResultSet p2 = PartialResultSet.newBuilder().setStats(stats).build();
    ServerStream<PartialResultSet> stream1 = mock(ServerStream.class);
    Iterator<PartialResultSet> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true, true, false);
    when(iterator.next())
        .thenReturn(p1)
        .thenThrow(
            new UnavailableException(
                "temporary unavailable", null, GrpcStatusCode.of(Code.UNAVAILABLE), true));
    when(stream1.iterator()).thenReturn(iterator);
    ServerStream<PartialResultSet> stream2 = mock(ServerStream.class);
    when(stream2.iterator()).thenReturn(ImmutableList.of(p1, p2).iterator());
    when(rpc.executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class)))
        .thenReturn(stream1);
    when(rpc.executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithResumeToken), anyMap(), any(Duration.class)))
        .thenReturn(stream2);

    long count = tx.executeStreamingPartitionedUpdate(Statement.of(sql), Duration.ofMinutes(10));

    assertThat(count).isEqualTo(1000L);
    verify(rpc).beginTransaction(any(BeginTransactionRequest.class), anyMap());
    verify(rpc)
        .executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class));
    verify(rpc)
        .executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithResumeToken), anyMap(), any(Duration.class));
  }

  @Test
  public void testExecuteStreamingPartitionedUpdateUnavailableAndThenDeadlineExceeded() {
    PartialResultSet p1 = PartialResultSet.newBuilder().setResumeToken(resumeToken).build();
    ServerStream<PartialResultSet> stream1 = mock(ServerStream.class);
    Iterator<PartialResultSet> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true, true, false);
    when(iterator.next())
        .thenReturn(p1)
        .thenThrow(
            new UnavailableException(
                "temporary unavailable", null, GrpcStatusCode.of(Code.UNAVAILABLE), true));
    when(stream1.iterator()).thenReturn(iterator);
    when(rpc.executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class)))
        .thenReturn(stream1);
    when(ticker.read()).thenReturn(0L, 1L, TimeUnit.NANOSECONDS.convert(10L, TimeUnit.MINUTES));

    try {
      tx.executeStreamingPartitionedUpdate(Statement.of(sql), Duration.ofMinutes(10));
      fail("missing expected DEADLINE_EXCEEDED exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
      verify(rpc).beginTransaction(any(BeginTransactionRequest.class), anyMap());
      verify(rpc)
          .executeStreamingPartitionedDml(
              Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class));
    }
  }

  @Test
  public void testExecuteStreamingPartitionedUpdateAbortedAndThenDeadlineExceeded() {
    PartialResultSet p1 = PartialResultSet.newBuilder().setResumeToken(resumeToken).build();
    ServerStream<PartialResultSet> stream1 = mock(ServerStream.class);
    Iterator<PartialResultSet> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true, true, false);
    when(iterator.next())
        .thenReturn(p1)
        .thenThrow(
            new AbortedException(
                "transaction aborted", null, GrpcStatusCode.of(Code.ABORTED), true));
    when(stream1.iterator()).thenReturn(iterator);
    when(rpc.executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class)))
        .thenReturn(stream1);
    when(ticker.read()).thenReturn(0L, 1L, TimeUnit.NANOSECONDS.convert(10L, TimeUnit.MINUTES));

    try {
      tx.executeStreamingPartitionedUpdate(Statement.of(sql), Duration.ofMinutes(10));
      fail("missing expected DEADLINE_EXCEEDED exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
      verify(rpc, times(2)).beginTransaction(any(BeginTransactionRequest.class), anyMap());
      verify(rpc)
          .executeStreamingPartitionedDml(
              Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class));
    }
  }

  @Test
  public void testExecuteStreamingPartitionedUpdateMultipleAbortsUntilDeadlineExceeded() {
    PartialResultSet p1 = PartialResultSet.newBuilder().setResumeToken(resumeToken).build();
    ServerStream<PartialResultSet> stream1 = mock(ServerStream.class);
    Iterator<PartialResultSet> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next())
        .thenReturn(p1)
        .thenThrow(
            new AbortedException(
                "transaction aborted", null, GrpcStatusCode.of(Code.ABORTED), true));
    when(stream1.iterator()).thenReturn(iterator);
    when(rpc.executeStreamingPartitionedDml(
            Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class)))
        .thenReturn(stream1);
    when(ticker.read())
        .thenAnswer(
            new Answer<Long>() {
              long ticks = 0L;

              @Override
              public Long answer(InvocationOnMock invocation) throws Throwable {
                return TimeUnit.NANOSECONDS.convert(++ticks, TimeUnit.MINUTES);
              }
            });

    try {
      tx.executeStreamingPartitionedUpdate(Statement.of(sql), Duration.ofMinutes(10));
      fail("missing expected DEADLINE_EXCEEDED exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
      // It should start a transaction exactly 10 times (10 ticks == 10 minutes).
      verify(rpc, times(10)).beginTransaction(any(BeginTransactionRequest.class), anyMap());
      // The last transaction should timeout before it starts the actual statement execution, which
      // means that the execute method is only executed 9 times.
      verify(rpc, times(9))
          .executeStreamingPartitionedDml(
              Mockito.eq(executeRequestWithoutResumeToken), anyMap(), any(Duration.class));
    }
  }
}
