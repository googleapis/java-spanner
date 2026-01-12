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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.XGoogSpannerRequestId.NoopRequestIdCreator;
import com.google.cloud.spanner.spi.v1.SpannerRpc.Option;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

abstract class BaseSessionPoolTest {
  ScheduledExecutorService mockExecutor;
  int sessionIndex;
  AtomicLong channelHint = new AtomicLong(0L);

  final class TestExecutorFactory implements ExecutorFactory<ScheduledExecutorService> {

    @Override
    public ScheduledExecutorService get() {
      ScheduledExecutorService realExecutor = new ScheduledThreadPoolExecutor(2);
      mockExecutor = spy(realExecutor);
      @SuppressWarnings("rawtypes")
      ScheduledFuture mockFuture = mock(ScheduledFuture.class);
      // To prevent maintenance loop from running.
      doReturn(mockFuture)
          .when(mockExecutor)
          .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
      return mockExecutor;
    }

    @Override
    public void release(ScheduledExecutorService executor) {
      try {
        executor.shutdown();
      } catch (Throwable ignore) {
      }
    }
  }

  @SuppressWarnings("unchecked")
  SessionImpl mockSession() {
    final SessionImpl session = mock(SessionImpl.class);
    Map options = new HashMap<>();
    options.put(Option.CHANNEL_HINT, channelHint.getAndIncrement());
    when(session.getRequestIdCreator()).thenReturn(NoopRequestIdCreator.INSTANCE);
    when(session.getOptions()).thenReturn(options);
    when(session.getName())
        .thenReturn(
            "projects/dummy/instances/dummy/database/dummy/sessions/session" + sessionIndex);
    when(session.asyncClose()).thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
    when(session.writeWithOptions(any(Iterable.class)))
        .thenReturn(new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance()));
    when(session.writeAtLeastOnceWithOptions(any(Iterable.class)))
        .thenReturn(new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance()));
    sessionIndex++;
    return session;
  }

  SessionImpl mockMultiplexedSession() {
    final SessionImpl session = mock(SessionImpl.class);
    Map options = new HashMap<>();
    when(session.getIsMultiplexed()).thenReturn(true);
    when(session.getOptions()).thenReturn(options);
    when(session.getName())
        .thenReturn(
            "projects/dummy/instances/dummy/database/dummy/sessions/session" + sessionIndex);
    when(session.asyncClose()).thenReturn(ApiFutures.immediateFuture(Empty.getDefaultInstance()));
    when(session.writeWithOptions(any(Iterable.class)))
        .thenReturn(new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance()));
    when(session.writeAtLeastOnceWithOptions(any(Iterable.class)))
        .thenReturn(new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance()));
    sessionIndex++;
    return session;
  }

  SessionImpl buildMockSession(SpannerImpl spanner, ReadContext context) {
    Map options = new HashMap<>();
    options.put(Option.CHANNEL_HINT, channelHint.getAndIncrement());
    final SessionImpl session =
        new SessionImpl(
            spanner,
            new SessionReference(
                "projects/dummy/instances/dummy/databases/dummy/sessions/session" + sessionIndex,
                options)) {
          @Override
          public ReadContext singleUse(TimestampBound bound) {
            // The below stubs are added so that we can mock keep-alive.
            return context;
          }

          @Override
          public ApiFuture<Empty> asyncClose() {
            return ApiFutures.immediateFuture(Empty.getDefaultInstance());
          }

          @Override
          public CommitResponse writeAtLeastOnceWithOptions(
              Iterable<Mutation> mutations, TransactionOption... transactionOptions)
              throws SpannerException {
            return new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance());
          }

          @Override
          public CommitResponse writeWithOptions(
              Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
            return new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance());
          }
        };
    sessionIndex++;
    return session;
  }

  SessionImpl buildMockMultiplexedSession(
      SpannerImpl spanner, ReadContext context, Timestamp creationTime) {
    Map options = new HashMap<>();
    final SessionImpl session =
        new SessionImpl(
            spanner,
            new SessionReference(
                "projects/dummy/instances/dummy/databases/dummy/sessions/session" + sessionIndex,
                creationTime,
                true,
                options)) {
          @Override
          public ReadContext singleUse(TimestampBound bound) {
            // The below stubs are added so that we can mock keep-alive.
            return context;
          }

          @Override
          public ApiFuture<Empty> asyncClose() {
            return ApiFutures.immediateFuture(Empty.getDefaultInstance());
          }

          @Override
          public CommitResponse writeAtLeastOnceWithOptions(
              Iterable<Mutation> mutations, TransactionOption... transactionOptions)
              throws SpannerException {
            return new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance());
          }

          @Override
          public CommitResponse writeWithOptions(
              Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
            return new CommitResponse(com.google.spanner.v1.CommitResponse.getDefaultInstance());
          }
        };
    sessionIndex++;
    return session;
  }

  void runMaintenanceLoop(FakeClock clock, SessionPool pool, long numCycles) {
    for (int i = 0; i < numCycles; i++) {
      pool.poolMaintainer.maintainPool();
      clock.currentTimeMillis.addAndGet(pool.poolMaintainer.loopFrequency);
    }
  }
}
