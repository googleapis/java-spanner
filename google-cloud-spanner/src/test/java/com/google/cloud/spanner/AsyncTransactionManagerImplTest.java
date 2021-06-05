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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import io.opencensus.trace.Span;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AsyncTransactionManagerImplTest {

  @Mock private SessionImpl session;
  @Mock TransactionRunnerImpl.TransactionContextImpl transaction;

  @Test
  public void testCommitReturnsCommitStats() {
    try (AsyncTransactionManagerImpl manager =
        new AsyncTransactionManagerImpl(session, mock(Span.class), Options.commitStats())) {
      when(session.newTransaction(Options.fromTransactionOptions(Options.commitStats())))
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
}
