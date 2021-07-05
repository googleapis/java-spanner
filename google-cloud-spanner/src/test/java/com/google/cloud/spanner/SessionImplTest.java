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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.api.core.NanoClock;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.Timestamp;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.ListValue;
import com.google.protobuf.util.Timestamps;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CommitResponse;
import com.google.spanner.v1.Mutation.Write;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.Transaction;
import io.opencensus.trace.Span;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** Unit tests for {@link com.google.cloud.spanner.SessionImpl}. */
@RunWith(JUnit4.class)
public class SessionImplTest {
  @Mock private SpannerRpc rpc;
  @Mock private SpannerOptions spannerOptions;
  private com.google.cloud.spanner.Session session;
  @Captor private ArgumentCaptor<Map<SpannerRpc.Option, Object>> optionsCaptor;
  private Map<SpannerRpc.Option, Object> options;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    when(spannerOptions.getPrefetchChunks()).thenReturn(1);
    when(spannerOptions.getRetrySettings()).thenReturn(RetrySettings.newBuilder().build());
    when(spannerOptions.getClock()).thenReturn(NanoClock.getDefaultClock());
    when(spannerOptions.getSessionLabels()).thenReturn(Collections.emptyMap());
    GrpcTransportOptions transportOptions = mock(GrpcTransportOptions.class);
    when(transportOptions.getExecutorFactory()).thenReturn(mock(ExecutorFactory.class));
    when(spannerOptions.getTransportOptions()).thenReturn(transportOptions);
    when(spannerOptions.getSessionPoolOptions()).thenReturn(mock(SessionPoolOptions.class));
    @SuppressWarnings("resource")
    SpannerImpl spanner = new SpannerImpl(rpc, spannerOptions);
    String dbName = "projects/p1/instances/i1/databases/d1";
    String sessionName = dbName + "/sessions/s1";
    DatabaseId db = DatabaseId.of(dbName);

    Session sessionProto = Session.newBuilder().setName(sessionName).build();
    Mockito.when(
            rpc.createSession(
                Mockito.eq(dbName),
                Mockito.anyMapOf(String.class, String.class),
                optionsCaptor.capture()))
        .thenReturn(sessionProto);
    Transaction txn = Transaction.newBuilder().setId(ByteString.copyFromUtf8("TEST")).build();
    Mockito.when(
            rpc.beginTransactionAsync(
                Mockito.any(BeginTransactionRequest.class), Mockito.any(Map.class)))
        .thenReturn(ApiFutures.immediateFuture(txn));
    CommitResponse commitResponse =
        CommitResponse.newBuilder()
            .setCommitTimestamp(com.google.protobuf.Timestamp.getDefaultInstance())
            .build();
    Mockito.when(rpc.commitAsync(Mockito.any(CommitRequest.class), Mockito.any(Map.class)))
        .thenReturn(ApiFutures.immediateFuture(commitResponse));
    Mockito.when(rpc.rollbackAsync(Mockito.any(RollbackRequest.class), Mockito.anyMap()))
        .thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
    session = spanner.getSessionClient(db).createSession();
    ((SessionImpl) session).setCurrentSpan(mock(Span.class));
    // We expect the same options, "options", on all calls on "session".
    options = optionsCaptor.getValue();
  }

  private void doNestedRwTransaction() {
    session
        .readWriteTransaction()
        .run(
            transaction -> {
              session.readWriteTransaction().run(transaction1 -> null);

              return null;
            });
  }

  @Test
  public void nestedReadWriteTxnThrows() {
    SpannerException e = assertThrows(SpannerException.class, () -> doNestedRwTransaction());
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
    assertThat(e.getMessage()).contains("not supported");
  }

  @Test
  public void nestedReadOnlyTxnThrows() {
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () ->
                session
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          session.readOnlyTransaction().getReadTimestamp();
                          return null;
                        }));
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
    assertThat(e.getMessage()).contains("not supported");
  }

  @Test
  public void nestedSingleUseReadTxnThrows() {
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () ->
                session
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          session.singleUseReadOnlyTransaction();
                          return null;
                        }));
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
    assertThat(e.getMessage()).contains("not supported");
  }

  @Test
  public void nestedTxnSucceedsWhenAllowed() {
    session
        .readWriteTransaction()
        .allowNestedTransaction()
        .run(
            transaction -> {
              session.singleUseReadOnlyTransaction();
              return null;
            });
  }

  @Test
  public void writeAtLeastOnce() throws ParseException {
    String timestampString = "2015-10-01T10:54:20.021Z";
    ArgumentCaptor<CommitRequest> commit = ArgumentCaptor.forClass(CommitRequest.class);
    CommitResponse response =
        CommitResponse.newBuilder().setCommitTimestamp(Timestamps.parse(timestampString)).build();
    Mockito.when(rpc.commit(commit.capture(), Mockito.eq(options))).thenReturn(response);

    Timestamp timestamp =
        session.writeAtLeastOnce(
            Collections.singletonList(Mutation.newInsertBuilder("T").set("C").to("x").build()));
    assertThat(timestamp.getSeconds())
        .isEqualTo(utcTimeSeconds(2015, Calendar.OCTOBER, 1, 10, 54, 20));
    assertThat(timestamp.getNanos()).isEqualTo(TimeUnit.MILLISECONDS.toNanos(21));

    CommitRequest request = commit.getValue();
    assertThat(request.getSingleUseTransaction()).isNotNull();
    assertThat(request.getSingleUseTransaction().getReadWrite()).isNotNull();
    com.google.spanner.v1.Mutation mutation =
        com.google.spanner.v1.Mutation.newBuilder()
            .setInsert(
                Write.newBuilder()
                    .setTable("T")
                    .addColumns("C")
                    .addValues(
                        ListValue.newBuilder()
                            .addValues(com.google.protobuf.Value.newBuilder().setStringValue("x"))))
            .build();
    assertThat(request.getMutationsList()).containsExactly(mutation);
  }

  @Test
  public void writeAtLeastOnceWithOptions() throws ParseException {
    String tag = "app=spanner,env=test";
    String timestampString = "2015-10-01T10:54:20.021Z";
    ArgumentCaptor<CommitRequest> commit = ArgumentCaptor.forClass(CommitRequest.class);
    CommitResponse response =
        CommitResponse.newBuilder().setCommitTimestamp(Timestamps.parse(timestampString)).build();
    Mockito.when(rpc.commit(commit.capture(), Mockito.eq(options))).thenReturn(response);
    session.writeAtLeastOnceWithOptions(
        Collections.singletonList(Mutation.newInsertBuilder("T").set("C").to("x").build()),
        Options.tag(tag));

    CommitRequest request = commit.getValue();
    assertThat(request.getRequestOptions().getTransactionTag()).isEqualTo(tag);
    com.google.spanner.v1.Mutation mutation =
        com.google.spanner.v1.Mutation.newBuilder()
            .setInsert(
                Write.newBuilder()
                    .setTable("T")
                    .addColumns("C")
                    .addValues(
                        ListValue.newBuilder()
                            .addValues(com.google.protobuf.Value.newBuilder().setStringValue("x"))))
            .build();
    assertThat(request.getMutationsList()).containsExactly(mutation);
  }

  private static long utcTimeSeconds(int year, int month, int day, int hour, int min, int secs) {
    GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    calendar.set(year, month, day, hour, min, secs);
    return calendar.getTimeInMillis() / 1000;
  }

  @Test
  public void newSingleUseContextClosesOldSingleUseContext() {
    ReadContext ctx = session.singleUse(TimestampBound.strong());
    session.singleUse(TimestampBound.strong());
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void newSingleUseContextClosesOldSingleUseReadOnlyTransactionContext() {
    ReadContext ctx = session.singleUseReadOnlyTransaction(TimestampBound.strong());
    session.singleUse(TimestampBound.strong());
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void newSingleUseContextClosesOldMultiUseReadOnlyTransactionContext() {
    ReadContext ctx = session.singleUseReadOnlyTransaction(TimestampBound.strong());
    session.singleUse(TimestampBound.strong());
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void newSingleUseReadOnlyTransactionContextClosesOldSingleUseContext() {
    ReadContext ctx = session.singleUse(TimestampBound.strong());
    session.singleUseReadOnlyTransaction(TimestampBound.strong());
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void newMultiUseReadOnlyTransactionContextClosesOldSingleUseContext() {
    ReadContext ctx = session.singleUse(TimestampBound.strong());
    session.readOnlyTransaction(TimestampBound.strong());
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void writeClosesOldSingleUseContext() throws ParseException {
    ReadContext ctx = session.singleUse(TimestampBound.strong());

    Mockito.when(rpc.commit(Mockito.any(), Mockito.eq(options)))
        .thenReturn(
            CommitResponse.newBuilder()
                .setCommitTimestamp(Timestamps.parse("2015-10-01T10:54:20.021Z"))
                .build());
    session.writeAtLeastOnce(Collections.emptyList());
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void transactionClosesOldSingleUseContext() {
    ReadContext ctx = session.singleUse(TimestampBound.strong());

    // Note that we don't even run the transaction - just preparing the runner is sufficient.
    session.readWriteTransaction();
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void singleUseContextClosesTransaction() {
    TransactionRunner runner = session.readWriteTransaction();

    session.singleUse(TimestampBound.strong());
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () ->
                runner.run(
                    transaction -> {
                      fail("Unexpected call to transaction body");
                      return null;
                    }));
    assertThat(e.getMessage()).contains("invalidated");
  }

  @Test
  public void prepareClosesOldSingleUseContext() {
    ReadContext ctx = session.singleUse(TimestampBound.strong());

    Mockito.when(rpc.beginTransaction(Mockito.any(), Mockito.eq(options)))
        .thenReturn(Transaction.newBuilder().setId(ByteString.copyFromUtf8("t1")).build());
    session.prepareReadWriteTransaction();
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> ctx.read("Dummy", KeySet.all(), Collections.singletonList("C")));
    assertThat(e.getMessage()).contains("invalidated");
  }

  private static ResultSetMetadata newMetadata(Type type) {
    return ResultSetMetadata.newBuilder().setRowType(type.toProto().getStructType()).build();
  }

  @Test
  public void singleUseReadOnlyTransactionDoesntReturnTransactionMetadata() {
    PartialResultSet resultSet =
        PartialResultSet.newBuilder()
            .setMetadata(newMetadata(Type.struct(Type.StructField.of("C", Type.string()))))
            .build();
    mockRead(resultSet);

    ReadOnlyTransaction txn = session.singleUseReadOnlyTransaction(TimestampBound.strong());
    assertThat(txn.readRow("Dummy", Key.of(), Collections.singletonList("C"))).isNull();

    // For now, getReadTimestamp() will raise an ISE because it hasn't seen a timestamp.  It would
    // be better for the read to fail with an INTERNAL error, but we can't do that until txn
    // metadata is returned for failed reads (e.g., table-not-found) as well as successful ones.
    // TODO(user): Fix this.
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> txn.getReadTimestamp());
    assertNotNull(e.getMessage());
  }

  @Test
  public void singleUseReadOnlyTransactionReturnsEmptyTransactionMetadata() {
    PartialResultSet resultSet =
        PartialResultSet.newBuilder()
            .setMetadata(
                newMetadata(Type.struct(Type.StructField.of("C", Type.string())))
                    .toBuilder()
                    .setTransaction(Transaction.getDefaultInstance()))
            .build();
    mockRead(resultSet);

    ReadOnlyTransaction txn = session.singleUseReadOnlyTransaction(TimestampBound.strong());
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> txn.readRow("Dummy", Key.of(), Collections.singletonList("C")));
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
  }

  private static class NoOpStreamingCall implements SpannerRpc.StreamingCall {
    @Override
    public void cancel(@Nullable String message) {}

    @Override
    public void request(int numMessages) {}
  }

  private void mockRead(final PartialResultSet myResultSet) {
    final ArgumentCaptor<SpannerRpc.ResultStreamConsumer> consumer =
        ArgumentCaptor.forClass(SpannerRpc.ResultStreamConsumer.class);
    Mockito.when(rpc.read(Mockito.any(), consumer.capture(), Mockito.eq(options)))
        .then(
            invocation -> {
              consumer.getValue().onPartialResultSet(myResultSet);
              consumer.getValue().onCompleted();
              return new NoOpStreamingCall();
            });
  }

  @Test
  public void multiUseReadOnlyTransactionReturnsEmptyTransactionMetadata() {
    Transaction txnMetadata = Transaction.newBuilder().setId(ByteString.copyFromUtf8("x")).build();
    PartialResultSet resultSet =
        PartialResultSet.newBuilder()
            .setMetadata(newMetadata(Type.struct(Type.StructField.of("C", Type.string()))))
            .build();
    Mockito.when(rpc.beginTransaction(Mockito.any(), Mockito.eq(options))).thenReturn(txnMetadata);
    mockRead(resultSet);

    ReadOnlyTransaction txn = session.readOnlyTransaction(TimestampBound.strong());
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> txn.readRow("Dummy", Key.of(), Collections.singletonList("C")));
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
  }

  @Test
  public void multiUseReadOnlyTransactionReturnsMissingTimestamp() {
    Transaction txnMetadata = Transaction.newBuilder().setId(ByteString.copyFromUtf8("x")).build();
    PartialResultSet resultSet =
        PartialResultSet.newBuilder()
            .setMetadata(newMetadata(Type.struct(Type.StructField.of("C", Type.string()))))
            .build();
    Mockito.when(rpc.beginTransaction(Mockito.any(), Mockito.eq(options))).thenReturn(txnMetadata);
    mockRead(resultSet);

    ReadOnlyTransaction txn = session.readOnlyTransaction(TimestampBound.strong());
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> txn.readRow("Dummy", Key.of(), Collections.singletonList("C")));
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
  }

  @Test
  public void multiUseReadOnlyTransactionReturnsMissingTransactionId() throws ParseException {
    com.google.protobuf.Timestamp t = Timestamps.parse("2015-10-01T10:54:20.021Z");
    Transaction txnMetadata = Transaction.newBuilder().setReadTimestamp(t).build();
    PartialResultSet resultSet =
        PartialResultSet.newBuilder()
            .setMetadata(newMetadata(Type.struct(Type.StructField.of("C", Type.string()))))
            .build();
    Mockito.when(rpc.beginTransaction(Mockito.any(), Mockito.eq(options))).thenReturn(txnMetadata);
    mockRead(resultSet);

    ReadOnlyTransaction txn = session.readOnlyTransaction(TimestampBound.strong());
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> txn.readRow("Dummy", Key.of(), Collections.singletonList("C")));
    assertEquals(ErrorCode.INTERNAL, e.getErrorCode());
  }
}
