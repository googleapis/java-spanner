/*
 * Copyright 2021 Google LLC
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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import com.google.protobuf.ByteString;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AsyncTransactionManagerImplTest {

  @Mock private SessionImpl session;
  @Mock TransactionRunnerImpl.TransactionContextImpl transaction;

  @Test
  public void testCommitReturnsCommitStats() {
    Span oTspan = mock(Span.class);
    ISpan span = new OpenTelemetrySpan(oTspan);
    when(oTspan.makeCurrent()).thenReturn(mock(Scope.class));
    try (AsyncTransactionManagerImpl manager =
        new AsyncTransactionManagerImpl(session, span, Options.commitStats())) {
      when(session.newTransaction(eq(Options.fromTransactionOptions(Options.commitStats())), any()))
          .thenReturn(transaction);
      when(transaction.ensureTxnAsync()).thenReturn(ApiFutures.immediateFuture(null));
      Timestamp commitTimestamp = Timestamp.ofTimeMicroseconds(1);
      CommitResponse response = mock(CommitResponse.class);
      when(response.getCommitTimestamp()).thenReturn(commitTimestamp);
      when(transaction.commitAsync()).thenReturn(ApiFutures.immediateFuture(response));
      manager.beginAsync();
      manager.commitAsync();
      verify(transaction).commitAsync();
    }
  }

  @Test
  public void testRetryUsesPreviousTransactionIdOnMultiplexedSession() {
    // Set up mock transaction IDs
    final ByteString mockTransactionId = ByteString.copyFromUtf8("mockTransactionId");
    final ByteString mockPreviousTransactionId =
        ByteString.copyFromUtf8("mockPreviousTransactionId");

    Span oTspan = mock(Span.class);
    ISpan span = new OpenTelemetrySpan(oTspan);
    when(oTspan.makeCurrent()).thenReturn(mock(Scope.class));
    // Mark the session as multiplexed.
    when(session.getIsMultiplexed()).thenReturn(true);

    // Initialize a mock transaction with transactionId = null, previousTransactionId = null.
    transaction = mock(TransactionRunnerImpl.TransactionContextImpl.class);
    when(transaction.ensureTxnAsync()).thenReturn(ApiFutures.immediateFuture(null));
    when(session.newTransaction(eq(Options.fromTransactionOptions(Options.commitStats())), any()))
        .thenReturn(transaction);

    // Simulate an ABORTED error being thrown when `commitAsync()` is called.
    doThrow(SpannerExceptionFactory.newSpannerException(ErrorCode.ABORTED, ""))
        .when(transaction)
        .commitAsync();

    try (AsyncTransactionManagerImpl manager =
        new AsyncTransactionManagerImpl(session, span, Options.commitStats())) {
      manager.beginAsync();

      // Verify that for the first transaction attempt, the `previousTransactionId` is
      // ByteString.EMPTY.
      // This is because no transaction has been previously aborted at this point.
      verify(session)
          .newTransaction(Options.fromTransactionOptions(Options.commitStats()), ByteString.EMPTY);
      assertThrows(AbortedException.class, manager::commitAsync);
      clearInvocations(session);

      // Mock the transaction object to contain transactionID=null and
      // previousTransactionId=mockPreviousTransactionId
      when(transaction.getPreviousTransactionId()).thenReturn(mockPreviousTransactionId);
      manager.resetForRetryAsync();
      // Verify that in the first retry attempt, the `previousTransactionId`
      // (mockPreviousTransactionId) is passed to the new transaction.
      // This allows Spanner to retry the transaction using the ID of the aborted transaction.
      verify(session)
          .newTransaction(
              Options.fromTransactionOptions(Options.commitStats()), mockPreviousTransactionId);
      assertThrows(AbortedException.class, manager::commitAsync);
      clearInvocations(session);

      // Mock the transaction object to contain transactionID=mockTransactionId and
      // previousTransactionId=mockPreviousTransactionId and transactionID = null
      transaction.transactionId = mockTransactionId;
      manager.resetForRetryAsync();
      // Verify that the latest `transactionId` (mockTransactionId) is used in the retry.
      // This ensures the retry logic is working as expected with the latest transaction ID.
      verify(session)
          .newTransaction(Options.fromTransactionOptions(Options.commitStats()), mockTransactionId);

      when(transaction.rollbackAsync()).thenReturn(ApiFutures.immediateFuture(null));
      manager.closeAsync();
    }
  }
}
