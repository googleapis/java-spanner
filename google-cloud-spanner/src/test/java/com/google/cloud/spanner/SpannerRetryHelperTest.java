/*
 * Copyright 2019 Google LLC
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

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Duration;
import com.google.rpc.RetryInfo;
import io.grpc.Context;
import io.grpc.Context.CancellableContext;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpannerRetryHelperTest {

  @Test
  public void testCancelledContext() {
    final CancellableContext withCancellation = Context.current().withCancellation();
    final CountDownLatch latch = new CountDownLatch(1);
    final Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            latch.countDown();
            throw SpannerExceptionFactory.newSpannerException(ErrorCode.ABORTED, "test");
          }
        };
    ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    service.submit(
        new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            latch.await();
            withCancellation.cancel(new InterruptedException());
            return null;
          }
        });
    try {
      withCancellation.run(
          new Runnable() {
            @Override
            public void run() {
              SpannerRetryHelper.runTxWithRetriesOnAborted(callable);
            }
          });
      fail("missing expected exception");
    } catch (SpannerException e) {
      if (e.getErrorCode() != ErrorCode.CANCELLED) {
        fail(
            String.format(
                "unexpected error %s, expected %s",
                e.getErrorCode().name(), ErrorCode.CANCELLED.name()));
      }
    }
  }

  @Test
  public void testTimedoutContext() {
    ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    final Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            throw SpannerExceptionFactory.newSpannerException(ErrorCode.ABORTED, "test");
          }
        };
    try {
      final CancellableContext withDeadline =
          Context.current().withDeadline(Deadline.after(1L, TimeUnit.MILLISECONDS), service);
      withDeadline.run(
          new Runnable() {
            @Override
            public void run() {
              SpannerRetryHelper.runTxWithRetriesOnAborted(callable);
            }
          });
      fail("missing expected exception");
    } catch (SpannerException e) {
      if (e.getErrorCode() != ErrorCode.DEADLINE_EXCEEDED) {
        fail(
            String.format(
                "unexpected error %s, expected %s",
                e.getErrorCode().name(), ErrorCode.DEADLINE_EXCEEDED.name()));
      }
    }
  }

  @Test
  public void noException() {
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            return 1 + 1;
          }
        };
    assertThat(SpannerRetryHelper.runTxWithRetriesOnAborted(callable)).isEqualTo(2);
  }

  @Test(expected = IllegalStateException.class)
  public void propagateUncheckedException() {
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            throw new IllegalStateException("test");
          }
        };
    SpannerRetryHelper.runTxWithRetriesOnAborted(callable);
  }

  @Test
  public void retryOnAborted() {
    final AtomicInteger attempts = new AtomicInteger();
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            if (attempts.getAndIncrement() == 0) {
              throw abortedWithRetryInfo((int) TimeUnit.MILLISECONDS.toNanos(1L));
            }
            return 1 + 1;
          }
        };
    assertThat(SpannerRetryHelper.runTxWithRetriesOnAborted(callable)).isEqualTo(2);
  }

  @Test
  public void retryMultipleTimesOnAborted() {
    final AtomicInteger attempts = new AtomicInteger();
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            if (attempts.getAndIncrement() < 2) {
              throw abortedWithRetryInfo((int) TimeUnit.MILLISECONDS.toNanos(1));
            }
            return 1 + 1;
          }
        };
    assertThat(SpannerRetryHelper.runTxWithRetriesOnAborted(callable)).isEqualTo(2);
  }

  @Test(expected = IllegalStateException.class)
  public void retryOnAbortedAndThenPropagateUnchecked() {
    final AtomicInteger attempts = new AtomicInteger();
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            if (attempts.getAndIncrement() == 0) {
              throw abortedWithRetryInfo((int) TimeUnit.MILLISECONDS.toNanos(1L));
            }
            throw new IllegalStateException("test");
          }
        };
    SpannerRetryHelper.runTxWithRetriesOnAborted(callable);
  }

  @Test
  public void testExceptionWithRetryInfo() {
    // Workaround from https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6435126.
    // See also https://stackoverflow.com/questions/824110/accurate-sleep-for-java-on-windows
    // Note that this is a daemon thread, so it will not prevent the JVM from shutting down.
    new ThreadFactoryBuilder()
        .setDaemon(true)
        .build()
        .newThread(
            new Runnable() {
              @Override
              public void run() {
                while (true) {
                  try {
                    Thread.sleep(Long.MAX_VALUE);
                  } catch (InterruptedException e) {
                  }
                }
              }
            });
    final int RETRY_DELAY_MILLIS = 100;
    Metadata.Key<RetryInfo> key = ProtoUtils.keyForProto(RetryInfo.getDefaultInstance());
    Status status = Status.fromCodeValue(Status.Code.ABORTED.value());
    Metadata trailers = new Metadata();
    RetryInfo retryInfo =
        RetryInfo.newBuilder()
            .setRetryDelay(
                Duration.newBuilder()
                    .setNanos(
                        (int)
                            TimeUnit.NANOSECONDS.convert(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS))
                    .build())
            .build();
    trailers.put(key, retryInfo);
    final SpannerException e =
        SpannerExceptionFactory.newSpannerException(new StatusRuntimeException(status, trailers));
    final AtomicInteger attempts = new AtomicInteger();
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            if (attempts.getAndIncrement() == 0) {
              throw e;
            }
            return 1 + 1;
          }
        };
    // The following call should take at least 100ms, as that is the retry delay specified in the
    // retry info of the exception.
    Stopwatch watch = Stopwatch.createStarted();
    assertThat(SpannerRetryHelper.runTxWithRetriesOnAborted(callable)).isEqualTo(2);
    long elapsed = watch.elapsed(TimeUnit.MILLISECONDS);
    // Allow 1ms difference as that should be the accuracy of the sleep method.
    assertThat(elapsed).isAtLeast(RETRY_DELAY_MILLIS - 1);
  }

  private SpannerException abortedWithRetryInfo(int nanos) {
    Metadata.Key<RetryInfo> key = ProtoUtils.keyForProto(RetryInfo.getDefaultInstance());
    Status status = Status.fromCodeValue(Status.Code.ABORTED.value());
    Metadata trailers = new Metadata();
    RetryInfo retryInfo =
        RetryInfo.newBuilder()
            .setRetryDelay(Duration.newBuilder().setNanos(nanos).setSeconds(0L))
            .build();
    trailers.put(key, retryInfo);
    return SpannerExceptionFactory.newSpannerException(
        ErrorCode.ABORTED, "test", new StatusRuntimeException(status, trailers));
  }
}
