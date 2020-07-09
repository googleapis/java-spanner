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
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.core.NanoClock;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.Timestamp;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.Transaction;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

/** Unit tests for {@link com.google.cloud.spanner.BatchClientImpl}. */
@RunWith(JUnit4.class)
public final class BatchClientImplTest {

  private static final String DB_NAME = "projects/my-project/instances/my-instance/databases/my-db";
  private static final String SESSION_NAME = DB_NAME + "/sessions/s1";
  private static final ByteString TXN_ID = ByteString.copyFromUtf8("my-txn");
  private static final String TIMESTAMP = "2017-11-15T10:54:20Z";

  @Mock private SpannerRpc gapicRpc;
  @Mock private SpannerOptions spannerOptions;
  @Captor private ArgumentCaptor<Map<SpannerRpc.Option, Object>> optionsCaptor;
  @Mock private BatchTransactionId txnID;

  private BatchClient client;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    initMocks(this);
    DatabaseId db = DatabaseId.of(DB_NAME);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    when(spannerOptions.getPrefetchChunks()).thenReturn(1);
    when(spannerOptions.getRetrySettings()).thenReturn(RetrySettings.newBuilder().build());
    when(spannerOptions.getClock()).thenReturn(NanoClock.getDefaultClock());
    when(spannerOptions.getSpannerRpcV1()).thenReturn(gapicRpc);
    when(spannerOptions.getSessionLabels()).thenReturn(Collections.<String, String>emptyMap());
    GrpcTransportOptions transportOptions = mock(GrpcTransportOptions.class);
    when(transportOptions.getExecutorFactory()).thenReturn(mock(ExecutorFactory.class));
    when(spannerOptions.getTransportOptions()).thenReturn(transportOptions);
    @SuppressWarnings("resource")
    SpannerImpl spanner = new SpannerImpl(gapicRpc, spannerOptions);
    client = new BatchClientImpl(spanner.getSessionClient(db));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBatchReadOnlyTxnWithBound() throws Exception {
    Session sessionProto = Session.newBuilder().setName(SESSION_NAME).build();
    when(gapicRpc.createSession(eq(DB_NAME), anyMap(), optionsCaptor.capture()))
        .thenReturn(sessionProto);
    com.google.protobuf.Timestamp timestamp = Timestamps.parse(TIMESTAMP);
    Transaction txnMetadata =
        Transaction.newBuilder().setId(TXN_ID).setReadTimestamp(timestamp).build();
    when(gapicRpc.beginTransaction(Mockito.<BeginTransactionRequest>any(), optionsCaptor.capture()))
        .thenReturn(txnMetadata);

    BatchReadOnlyTransaction batchTxn = client.batchReadOnlyTransaction(TimestampBound.strong());
    assertThat(batchTxn.getBatchTransactionId().getSessionId()).isEqualTo(SESSION_NAME);
    assertThat(batchTxn.getBatchTransactionId().getTransactionId()).isEqualTo(TXN_ID);
    Timestamp t = Timestamp.parseTimestamp(TIMESTAMP);
    assertThat(batchTxn.getReadTimestamp()).isEqualTo(t);
    assertThat(batchTxn.getReadTimestamp())
        .isEqualTo(batchTxn.getBatchTransactionId().getTimestamp());
  }

  @Test
  public void testBatchReadOnlyTxnWithTxnId() {
    when(txnID.getSessionId()).thenReturn(SESSION_NAME);
    when(txnID.getTransactionId()).thenReturn(TXN_ID);
    Timestamp t = Timestamp.parseTimestamp(TIMESTAMP);
    when(txnID.getTimestamp()).thenReturn(t);

    BatchReadOnlyTransaction batchTxn = client.batchReadOnlyTransaction(txnID);
    assertThat(batchTxn.getBatchTransactionId().getSessionId()).isEqualTo(SESSION_NAME);
    assertThat(batchTxn.getBatchTransactionId().getTransactionId()).isEqualTo(TXN_ID);
    assertThat(batchTxn.getReadTimestamp()).isEqualTo(t);
    assertThat(batchTxn.getReadTimestamp())
        .isEqualTo(batchTxn.getBatchTransactionId().getTimestamp());
  }
}
