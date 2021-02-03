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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.spanner.AsyncRunner.AsyncWork;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AsyncRunnerImplTest {
  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  @AfterClass
  public static void teardown() {
    executor.shutdown();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAsyncRunReturnsResultAndCommitResponse() {
    final Object expectedResult = new Object();
    final CommitResponse expectedCommitResponse = mock(CommitResponse.class);

    TransactionRunnerImpl delegate = mock(TransactionRunnerImpl.class);
    when(delegate.run(any(TransactionCallable.class))).thenReturn(expectedResult);
    when(delegate.getCommitResponse()).thenReturn(expectedCommitResponse);

    AsyncRunnerImpl runner = new AsyncRunnerImpl(delegate);
    ApiFuture<Object> result =
        runner.runAsync(
            new AsyncWork<Object>() {
              @Override
              public ApiFuture<Object> doWorkAsync(TransactionContext txn) {
                return ApiFutures.immediateFuture(expectedResult);
              }
            },
            executor);

    assertThat(SpannerApiFutures.get(result)).isSameInstanceAs(expectedResult);
    assertThat(SpannerApiFutures.get(runner.getCommitResponse()))
        .isSameInstanceAs(expectedCommitResponse);
  }

  @Test
  public void testGetCommitTimestampReturnsErrorBeforeRun() {
    TransactionRunnerImpl delegate = mock(TransactionRunnerImpl.class);
    AsyncRunnerImpl runner = new AsyncRunnerImpl(delegate);
    try {
      runner.getCommitTimestamp();
      fail("missing expected exception");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("runAsync() has not yet been called");
    }
  }

  @Test
  public void testGetCommitResponseReturnsErrorBeforeRun() {
    TransactionRunnerImpl delegate = mock(TransactionRunnerImpl.class);
    AsyncRunnerImpl runner = new AsyncRunnerImpl(delegate);
    try {
      runner.getCommitResponse();
      fail("missing expected exception");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("runAsync() has not yet been called");
    }
  }

  @Test
  public void testGetCommitResponseReturnsErrorIfRunFails() {
    final SpannerException expectedException =
        SpannerExceptionFactory.newSpannerException(ErrorCode.ALREADY_EXISTS, "Row already exists");

    TransactionRunnerImpl delegate = mock(TransactionRunnerImpl.class);
    when(delegate.getCommitResponse()).thenThrow(expectedException);

    AsyncRunnerImpl runner = new AsyncRunnerImpl(delegate);
    runner.runAsync(
        new AsyncWork<Void>() {
          @Override
          public ApiFuture<Void> doWorkAsync(TransactionContext txn) {
            return ApiFutures.immediateFailedFuture(expectedException);
          }
        },
        executor);

    try {
      SpannerApiFutures.get(runner.getCommitResponse());
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e).isSameInstanceAs(expectedException);
    }
  }

  @Test
  public void testRunAyncFailsIfCalledMultipleTimes() {
    final Object result = new Object();
    TransactionRunnerImpl delegate = mock(TransactionRunnerImpl.class);
    when(delegate.run(any(TransactionCallable.class))).thenReturn(result);

    AsyncRunnerImpl runner = new AsyncRunnerImpl(delegate);
    runner.runAsync(
        new AsyncWork<Object>() {
          @Override
          public ApiFuture<Object> doWorkAsync(TransactionContext txn) {
            return ApiFutures.immediateFuture(result);
          }
        },
        executor);

    try {
      runner.runAsync(
          new AsyncWork<Object>() {
            @Override
            public ApiFuture<Object> doWorkAsync(TransactionContext txn) {
              return ApiFutures.immediateFuture(null);
            }
          },
          executor);
      fail("missing expected exception");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("runAsync() can only be called once");
    }
  }
}
