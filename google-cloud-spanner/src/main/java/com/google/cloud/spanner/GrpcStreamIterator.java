/*
 * Copyright 2024 Google LLC
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

import com.google.api.gax.rpc.ApiCallContext;
import com.google.cloud.spanner.AbstractResultSet.CloseableIterator;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.AbstractIterator;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.spanner.v1.PartialResultSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

/** Adapts a streaming read/query call into an iterator over partial result sets. */
@VisibleForTesting
class GrpcStreamIterator extends AbstractIterator<PartialResultSet>
    implements CloseableIterator<PartialResultSet> {
  private static final Logger logger = Logger.getLogger(GrpcStreamIterator.class.getName());
  private static final PartialResultSet END_OF_STREAM = PartialResultSet.newBuilder().build();

  private final ConsumerImpl consumer = new ConsumerImpl();
  private final BlockingQueue<PartialResultSet> stream;
  private final Statement statement;

  private SpannerRpc.StreamingCall call;
  private volatile boolean withBeginTransaction;
  private TimeUnit streamWaitTimeoutUnit;
  private long streamWaitTimeoutValue;
  private SpannerException error;

  @VisibleForTesting
  GrpcStreamIterator(int prefetchChunks) {
    this(null, prefetchChunks);
  }

  @VisibleForTesting
  GrpcStreamIterator(Statement statement, int prefetchChunks) {
    this.statement = statement;
    // One extra to allow for END_OF_STREAM message.
    this.stream = new LinkedBlockingQueue<>(prefetchChunks + 1);
  }

  protected final SpannerRpc.ResultStreamConsumer consumer() {
    return consumer;
  }

  public void setCall(SpannerRpc.StreamingCall call, boolean withBeginTransaction) {
    this.call = call;
    this.withBeginTransaction = withBeginTransaction;
    ApiCallContext callContext = call.getCallContext();
    Duration streamWaitTimeout = callContext == null ? null : callContext.getStreamWaitTimeout();
    if (streamWaitTimeout != null) {
      // Determine the timeout unit to use. This reduces the precision to seconds if the timeout
      // value is more than 1 second, which is lower than the precision that would normally be
      // used by the stream watchdog (which uses a precision of 10 seconds by default).
      if (streamWaitTimeout.getSeconds() > 0L) {
        streamWaitTimeoutValue = streamWaitTimeout.getSeconds();
        streamWaitTimeoutUnit = TimeUnit.SECONDS;
      } else if (streamWaitTimeout.getNano() > 0) {
        streamWaitTimeoutValue = streamWaitTimeout.getNano();
        streamWaitTimeoutUnit = TimeUnit.NANOSECONDS;
      }
      // Note that if the stream-wait-timeout is zero, we won't set a timeout at all.
      // That is consistent with ApiCallContext#withStreamWaitTimeout(Duration.ZERO).
    }
  }

  @Override
  public void close(@Nullable String message) {
    if (call != null) {
      call.cancel(message);
    }
  }

  @Override
  public boolean isWithBeginTransaction() {
    return withBeginTransaction;
  }

  @Override
  protected final PartialResultSet computeNext() {
    PartialResultSet next;
    try {
      if (streamWaitTimeoutUnit != null) {
        next = stream.poll(streamWaitTimeoutValue, streamWaitTimeoutUnit);
        if (next == null) {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.DEADLINE_EXCEEDED, "stream wait timeout");
        }
      } else {
        next = stream.take();
      }
    } catch (InterruptedException e) {
      // Treat interrupt as a request to cancel the read.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
    if (next != END_OF_STREAM) {
      call.request(1);
      return next;
    }

    // All done - close() no longer needs to cancel the call.
    call = null;

    if (error != null) {
      throw SpannerExceptionFactory.newSpannerException(error);
    }

    endOfData();
    return null;
  }

  private void addToStream(PartialResultSet results) {
    // We assume that nothing from the user will interrupt gRPC event threads.
    Uninterruptibles.putUninterruptibly(stream, results);
  }

  private class ConsumerImpl implements SpannerRpc.ResultStreamConsumer {
    @Override
    public void onPartialResultSet(PartialResultSet results) {
      addToStream(results);
    }

    @Override
    public void onCompleted() {
      addToStream(END_OF_STREAM);
    }

    @Override
    public void onError(SpannerException e) {
      if (statement != null) {
        if (logger.isLoggable(Level.FINEST)) {
          // Include parameter values if logging level is set to FINEST or higher.
          e =
              SpannerExceptionFactory.newSpannerExceptionPreformatted(
                  e.getErrorCode(),
                  String.format("%s - Statement: '%s'", e.getMessage(), statement.toString()),
                  e);
          logger.log(Level.FINEST, "Error executing statement", e);
        } else {
          e =
              SpannerExceptionFactory.newSpannerExceptionPreformatted(
                  e.getErrorCode(),
                  String.format("%s - Statement: '%s'", e.getMessage(), statement.getSql()),
                  e);
        }
      }
      error = e;
      addToStream(END_OF_STREAM);
    }
  }
}
