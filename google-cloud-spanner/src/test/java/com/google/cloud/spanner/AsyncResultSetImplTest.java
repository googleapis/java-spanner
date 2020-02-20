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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.CursorState;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AsyncResultSetImplTest {
  private ExecutorFactory<ScheduledExecutorService> mockedFactory;
  private ExecutorFactory<ScheduledExecutorService> simpleFactory;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() {
    mockedFactory = mock(ExecutorFactory.class);
    when(mockedFactory.get()).thenReturn(mock(ScheduledExecutorService.class));
    simpleFactory =
        new GrpcTransportOptions.ExecutorFactory<ScheduledExecutorService>() {
          @Override
          public ScheduledExecutorService get() {
            return Executors.newScheduledThreadPool(1);
          }

          @Override
          public void release(ScheduledExecutorService executor) {
            executor.shutdown();
          }
        };
  }

  @SuppressWarnings("unchecked")
  @Test
  public void close() {
    AsyncResultSetImpl rs = new AsyncResultSetImpl(mockedFactory, mock(ResultSet.class));
    rs.close();
    // Closing a second time should be a no-op.
    rs.close();

    // The following methods are not allowed to call after closing the result set.
    try {
      rs.setCallback(mock(Executor.class), mock(ReadyCallback.class));
      fail("missing expected exception");
    } catch (IllegalStateException e) {
    }
    try {
      rs.toList(mock(Function.class));
      fail("missing expected exception");
    } catch (IllegalStateException e) {
    }
    try {
      rs.toListAsync(mock(Function.class), mock(Executor.class));
      fail("missing expected exception");
    } catch (IllegalStateException e) {
    }

    // The following methods are allowed on a closed result set.
    AsyncResultSetImpl rs2 = new AsyncResultSetImpl(mockedFactory, mock(ResultSet.class));
    rs2.setCallback(mock(Executor.class), mock(ReadyCallback.class));
    rs2.close();
    rs2.cancel();
    rs2.resume();
  }

  @Test
  public void tryNextNotAllowed() {
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(mockedFactory, mock(ResultSet.class))) {
      rs.setCallback(mock(Executor.class), mock(ReadyCallback.class));
      try {
        rs.tryNext();
        fail("missing expected exception");
      } catch (IllegalStateException e) {
        assertThat(e.getMessage())
            .contains("tryNext may only be called from a DataReady callback.");
      }
    }
  }

  @Test
  public void toList() {
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next()).thenReturn(true, true, true, false);
    when(delegate.getCurrentRowAsStruct()).thenReturn(mock(Struct.class));
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      ImmutableList<Object> list =
          rs.toList(
              new Function<StructReader, Object>() {
                @Override
                public Object apply(StructReader input) {
                  return new Object();
                }
              });
      assertThat(list).hasSize(3);
    }
  }

  @Test
  public void toListPropagatesError() {
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next())
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.INVALID_ARGUMENT, "invalid query"));
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.toList(
          new Function<StructReader, Object>() {
            @Override
            public Object apply(StructReader input) {
              return new Object();
            }
          });
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(e.getMessage()).contains("invalid query");
    }
  }

  @Test
  public void toListAsync() throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newFixedThreadPool(1);
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next()).thenReturn(true, true, true, false);
    when(delegate.getCurrentRowAsStruct()).thenReturn(mock(Struct.class));
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      ApiFuture<ImmutableList<Object>> future =
          rs.toListAsync(
              new Function<StructReader, Object>() {
                @Override
                public Object apply(StructReader input) {
                  return new Object();
                }
              },
              executor);
      assertThat(future.get()).hasSize(3);
    }
    executor.shutdown();
  }

  @Test
  public void toListAsyncPropagatesError() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(1);
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next())
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.INVALID_ARGUMENT, "invalid query"));
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.toListAsync(
              new Function<StructReader, Object>() {
                @Override
                public Object apply(StructReader input) {
                  return new Object();
                }
              },
              executor)
          .get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(se.getMessage()).contains("invalid query");
    }
    executor.shutdown();
  }

  @Test
  public void withCallback() throws InterruptedException {
    Executor executor = Executors.newSingleThreadExecutor();
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next()).thenReturn(true, true, true, false);
    when(delegate.getCurrentRowAsStruct()).thenReturn(mock(Struct.class));
    final AtomicInteger callbackCounter = new AtomicInteger();
    final AtomicInteger rowCounter = new AtomicInteger();
    final CountDownLatch finishedLatch = new CountDownLatch(1);
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              callbackCounter.incrementAndGet();
              CursorState state;
              while ((state = resultSet.tryNext()) == CursorState.OK) {
                rowCounter.incrementAndGet();
              }
              if (state == CursorState.DONE) {
                finishedLatch.countDown();
              }
              return CallbackResponse.CONTINUE;
            }
          });
    }
    finishedLatch.await();
    // There should be between 1 and 4 callbacks, depending on the timing of the threads.
    // Normally, there should be just 1 callback.
    assertThat(callbackCounter.get()).isIn(Range.closed(1, 4));
    assertThat(rowCounter.get()).isEqualTo(3);
  }

  @Test
  public void callbackReceivesError() throws InterruptedException {
    Executor executor = Executors.newSingleThreadExecutor();
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next())
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.INVALID_ARGUMENT, "invalid query"));
    final BlockingDeque<Exception> receivedErr = new LinkedBlockingDeque<>(1);
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              try {
                resultSet.tryNext();
                receivedErr.push(new Exception("missing expected exception"));
              } catch (SpannerException e) {
                receivedErr.push(e);
              }
              return CallbackResponse.DONE;
            }
          });
    }
    Exception e = receivedErr.take();
    assertThat(e).isInstanceOf(SpannerException.class);
    SpannerException se = (SpannerException) e;
    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    assertThat(se.getMessage()).contains("invalid query");
  }

  @Test
  public void callbackReceivesErrorHalfwayThrough() throws InterruptedException {
    Executor executor = Executors.newSingleThreadExecutor();
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next())
        .thenReturn(true)
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.INVALID_ARGUMENT, "invalid query"));
    when(delegate.getCurrentRowAsStruct()).thenReturn(mock(Struct.class));
    final AtomicInteger rowCount = new AtomicInteger();
    final BlockingDeque<Exception> receivedErr = new LinkedBlockingDeque<>(1);
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              try {
                if (resultSet.tryNext() != CursorState.DONE) {
                  rowCount.incrementAndGet();
                  return CallbackResponse.CONTINUE;
                }
              } catch (SpannerException e) {
                receivedErr.push(e);
              }
              return CallbackResponse.DONE;
            }
          });
    }
    Exception e = receivedErr.take();
    assertThat(e).isInstanceOf(SpannerException.class);
    SpannerException se = (SpannerException) e;
    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    assertThat(se.getMessage()).contains("invalid query");
    assertThat(rowCount.get()).isEqualTo(1);
  }

  @Test
  public void pauseResume() throws InterruptedException {
    Executor executor = Executors.newSingleThreadExecutor();
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next()).thenReturn(true, true, true, false);
    when(delegate.getCurrentRowAsStruct()).thenReturn(mock(Struct.class));
    final AtomicInteger callbackCounter = new AtomicInteger();
    final BlockingDeque<Object> queue = new LinkedBlockingDeque<>(1);
    final AtomicBoolean finished = new AtomicBoolean(false);
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              callbackCounter.incrementAndGet();
              CursorState state = resultSet.tryNext();
              if (state == CursorState.OK) {
                try {
                  queue.put(new Object());
                } catch (InterruptedException e) {
                  // Finish early if an error occurs.
                  return CallbackResponse.DONE;
                }
                return CallbackResponse.PAUSE;
              }
              finished.set(true);
              return CallbackResponse.DONE;
            }
          });
      int rowCounter = 0;
      while (!finished.get()) {
        Object o = queue.poll(1L, TimeUnit.MILLISECONDS);
        if (o != null) {
          rowCounter++;
        }
        rs.resume();
      }
      // There should be exactly 4 callbacks as we only consume one row per callback.
      assertThat(callbackCounter.get()).isEqualTo(4);
      assertThat(rowCounter).isEqualTo(3);
    }
  }

  @Test
  public void cancel() throws InterruptedException {
    Executor executor = Executors.newSingleThreadExecutor();
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next()).thenReturn(true, true, true, false);
    when(delegate.getCurrentRowAsStruct()).thenReturn(mock(Struct.class));
    final AtomicInteger callbackCounter = new AtomicInteger();
    final BlockingDeque<Object> queue = new LinkedBlockingDeque<>(1);
    final AtomicBoolean finished = new AtomicBoolean(false);
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              callbackCounter.incrementAndGet();
              try {
                CursorState state = resultSet.tryNext();
                if (state == CursorState.OK) {
                  try {
                    queue.put(new Object());
                  } catch (InterruptedException e) {
                    // Finish early if an error occurs.
                    return CallbackResponse.DONE;
                  }
                }
                // Pause after 2 rows to make sure that no more data is consumed until the cancel
                // call has been received.
                return callbackCounter.get() == 2
                    ? CallbackResponse.PAUSE
                    : CallbackResponse.CONTINUE;
              } catch (SpannerException e) {
                if (e.getErrorCode() == ErrorCode.CANCELLED) {
                  finished.set(true);
                }
              }
              return CallbackResponse.DONE;
            }
          });
      int rowCounter = 0;
      while (!finished.get()) {
        Object o = queue.poll(1L, TimeUnit.MILLISECONDS);
        if (o != null) {
          rowCounter++;
        }
        if (rowCounter == 2) {
          // Cancel the result set and then resume it to get the cancelled error.
          rs.cancel();
          rs.resume();
        }
      }
      assertThat(callbackCounter.get()).isIn(Range.closed(2, 4));
      assertThat(rowCounter).isIn(Range.closed(2, 3));
    }
  }

  @Test
  public void callbackReturnsError() throws InterruptedException {
    Executor executor = Executors.newSingleThreadExecutor();
    ResultSet delegate = mock(ResultSet.class);
    when(delegate.next()).thenReturn(true, true, true, false);
    when(delegate.getCurrentRowAsStruct()).thenReturn(mock(Struct.class));
    final AtomicInteger callbackCounter = new AtomicInteger();
    try (AsyncResultSetImpl rs = new AsyncResultSetImpl(simpleFactory, delegate)) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              callbackCounter.incrementAndGet();
              throw new RuntimeException("async test");
            }
          });
      rs.getResult().get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.UNKNOWN);
      assertThat(se.getMessage()).contains("async test");
      assertThat(callbackCounter.get()).isEqualTo(1);
    }
  }
}
