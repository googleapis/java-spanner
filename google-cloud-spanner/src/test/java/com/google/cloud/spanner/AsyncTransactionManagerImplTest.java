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
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.core.ApiFutures;
import com.google.cloud.Timestamp;
import io.opencensus.trace.Span;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

@RunWith(JUnit4.class)
public class AsyncTransactionManagerImplTest {

  @Mock private SessionImpl session;
  @Mock TransactionRunnerImpl.TransactionContextImpl txn;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testCommitReturnsCommitStats() {
    try (AsyncTransactionManagerImpl manager =
        new AsyncTransactionManagerImpl(session, mock(Span.class), Options.commitStats())) {
      when(session.newTransaction(Options.fromTransactionOptions(Options.commitStats())))
          .thenReturn(txn);
      when(txn.ensureTxnAsync()).thenReturn(ApiFutures.<Void>immediateFuture(null));
      Timestamp commitTimestamp = Timestamp.ofTimeMicroseconds(1);
      CommitResponse response = mock(CommitResponse.class);
      when(response.getCommitTimestamp()).thenReturn(commitTimestamp);
      when(txn.commitAsync()).thenReturn(ApiFutures.immediateFuture(response));
      manager.beginAsync();
      manager.commitAsync();
      verify(txn).commitAsync();
    }
  }
}
