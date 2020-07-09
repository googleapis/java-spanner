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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.Value;
import com.google.rpc.RetryInfo;
import com.google.spanner.v1.PartialResultSet;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import io.opencensus.trace.EndSpanOptions;
import io.opencensus.trace.Span;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

/** Unit tests for {@link AbstractResultSet.ResumableStreamIterator}. */
@RunWith(JUnit4.class)
public class ResumableStreamIteratorTest {
  interface Starter {
    AbstractResultSet.CloseableIterator<PartialResultSet> startStream(
        @Nullable ByteString resumeToken);
  }

  interface ResultSetStream {
    PartialResultSet next();

    void close();
  }

  private static StatusRuntimeException statusWithRetryInfo(ErrorCode code) {
    Metadata.Key<RetryInfo> key = ProtoUtils.keyForProto(RetryInfo.getDefaultInstance());
    Metadata trailers = new Metadata();
    RetryInfo retryInfo =
        RetryInfo.newBuilder()
            .setRetryDelay(
                Duration.newBuilder()
                    .setNanos((int) TimeUnit.MILLISECONDS.toNanos(1L))
                    .setSeconds(0L))
            .build();
    trailers.put(key, retryInfo);
    return code.getGrpcStatus().asRuntimeException(trailers);
  }

  static class RetryableException extends SpannerException {
    RetryableException(ErrorCode code, @Nullable String message) {
      // OK to instantiate SpannerException directly for this unit test.
      super(DoNotConstructDirectly.ALLOWED, code, true, message, statusWithRetryInfo(code));
    }
  }

  static class NonRetryableException extends SpannerException {
    NonRetryableException(ErrorCode code, @Nullable String message) {
      super(DoNotConstructDirectly.ALLOWED, code, false, message, null);
    }
  }

  static class ResultSetIterator extends AbstractIterator<PartialResultSet>
      implements AbstractResultSet.CloseableIterator<PartialResultSet> {
    final ResultSetStream stream;

    ResultSetIterator(ResultSetStream stream) {
      this.stream = stream;
    }

    @Override
    protected PartialResultSet computeNext() {
      PartialResultSet next = stream.next();
      if (next == null) {
        endOfData();
      }
      return next;
    }

    @Override
    public void close(@Nullable String message) {
      stream.close();
    }
  }

  Starter starter = Mockito.mock(Starter.class);
  AbstractResultSet.ResumableStreamIterator resumableStreamIterator;

  @Before
  public void setUp() {
    initWithLimit(Integer.MAX_VALUE);
  }

  private void initWithLimit(int maxBufferSize) {
    resumableStreamIterator =
        new AbstractResultSet.ResumableStreamIterator(maxBufferSize, "", null) {
          @Override
          AbstractResultSet.CloseableIterator<PartialResultSet> startStream(
              @Nullable ByteString resumeToken) {
            return starter.startStream(resumeToken);
          }
        };
  }

  @Test
  public void simple() {
    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(null, "a"))
        .thenReturn(resultSet(null, "b"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b").inOrder();
  }

  @Test
  public void closedSpan() {
    Span span = mock(Span.class);
    Whitebox.setInternalState(this.resumableStreamIterator, "span", span);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r2"), "b"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b").inOrder();

    resumableStreamIterator.close("closed");
    verify(span).end(EndSpanOptions.builder().setSampleToLocalSpanStore(true).build());
  }

  @Test
  public void restart() {
    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r2"), "b"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    ResultSetStream s2 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(ByteString.copyFromUtf8("r2")))
        .thenReturn(new ResultSetIterator(s2));
    Mockito.when(s2.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r3"), "c"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r4"), "d"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b", "c", "d").inOrder();
  }

  @Test
  public void restartWithHoldBack() {
    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r2"), "b"))
        .thenReturn(resultSet(null, "X"))
        .thenReturn(resultSet(null, "X"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    ResultSetStream s2 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(ByteString.copyFromUtf8("r2")))
        .thenReturn(new ResultSetIterator(s2));
    Mockito.when(s2.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r3"), "c"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r4"), "d"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b", "c", "d").inOrder();
  }

  @Test
  public void restartWithHoldBackMidStream() {
    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(null, "b"))
        .thenReturn(resultSet(null, "c"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r2"), "d"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    ResultSetStream s2 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(ByteString.copyFromUtf8("r2")))
        .thenReturn(new ResultSetIterator(s2));
    Mockito.when(s2.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r3"), "e"))
        .thenReturn(resultSet(null, "f"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator))
        .containsExactly("a", "b", "c", "d", "e", "f")
        .inOrder();
  }

  @Test
  public void nonRetryableError() {
    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r2"), "b"))
        .thenReturn(resultSet(null, "X"))
        .thenReturn(resultSet(null, "X"))
        .thenThrow(new NonRetryableException(ErrorCode.FAILED_PRECONDITION, "failed by test"));
    Iterator<String> strings = stringIterator(resumableStreamIterator);
    assertThat(strings.next()).isEqualTo("a");
    assertThat(strings.next()).isEqualTo("b");
    try {
      assertThat(strings.next()).isNotEqualTo("X");
      fail("Expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAILED_PRECONDITION);
    }
  }

  @Test
  public void bufferLimitSimple() {
    initWithLimit(1);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(null, "a"))
        .thenReturn(resultSet(null, "b"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b").inOrder();
  }

  @Test
  public void bufferLimitSimpleWithRestartTokens() {
    initWithLimit(1);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r2"), "b"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b").inOrder();
  }

  @Test
  public void bufferLimitRestart() {
    initWithLimit(1);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r2"), "b"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    ResultSetStream s2 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(ByteString.copyFromUtf8("r2")))
        .thenReturn(new ResultSetIterator(s2));
    Mockito.when(s2.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r3"), "c"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r4"), "d"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b", "c", "d").inOrder();
  }

  @Test
  public void bufferLimitRestartWithinLimitAtStartOfResults() {
    initWithLimit(1);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(null, "XXXXXX"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    ResultSetStream s2 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s2));
    Mockito.when(s2.next())
        .thenReturn(resultSet(null, "a"))
        .thenReturn(resultSet(null, "b"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b").inOrder();
  }

  @Test
  public void bufferLimitRestartWithinLimitMidResults() {
    initWithLimit(1);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(null, "XXXXXX"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    ResultSetStream s2 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(ByteString.copyFromUtf8("r1")))
        .thenReturn(new ResultSetIterator(s2));
    Mockito.when(s2.next())
        .thenReturn(resultSet(null, "b"))
        .thenReturn(resultSet(null, "c"))
        .thenReturn(null);
    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b", "c").inOrder();
  }

  @Test
  public void bufferLimitMissingTokensUnsafeToRetry() {
    initWithLimit(1);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(null, "b"))
        .thenReturn(resultSet(null, "c"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    assertThat(consumeAtMost(3, resumableStreamIterator)).containsExactly("a", "b", "c").inOrder();
    try {
      resumableStreamIterator.next();
      fail("Expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAVAILABLE);
    }
  }

  @Test
  public void bufferLimitMissingTokensSafeToRetry() {
    initWithLimit(1);

    ResultSetStream s1 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(null)).thenReturn(new ResultSetIterator(s1));
    Mockito.when(s1.next())
        .thenReturn(resultSet(ByteString.copyFromUtf8("r1"), "a"))
        .thenReturn(resultSet(null, "b"))
        .thenReturn(resultSet(ByteString.copyFromUtf8("r3"), "c"))
        .thenThrow(new RetryableException(ErrorCode.UNAVAILABLE, "failed by test"));

    ResultSetStream s2 = Mockito.mock(ResultSetStream.class);
    Mockito.when(starter.startStream(ByteString.copyFromUtf8("r3")))
        .thenReturn(new ResultSetIterator(s2));
    Mockito.when(s2.next()).thenReturn(resultSet(null, "d")).thenReturn(null);

    assertThat(consume(resumableStreamIterator)).containsExactly("a", "b", "c", "d").inOrder();
  }

  static PartialResultSet resultSet(@Nullable ByteString resumeToken, String... data) {
    PartialResultSet.Builder builder = PartialResultSet.newBuilder();
    if (resumeToken != null) {
      builder.setResumeToken(resumeToken);
    }
    for (String s : data) {
      builder.addValuesBuilder().setStringValue(s);
    }
    return builder.build();
  }

  static Iterator<String> stringIterator(final Iterator<PartialResultSet> iterator) {
    return new AbstractIterator<String>() {
      private final LinkedList<String> buffer = new LinkedList<>();

      @Override
      protected String computeNext() {
        while (true) {
          if (!buffer.isEmpty()) {
            return buffer.pop();
          }
          if (!iterator.hasNext()) {
            endOfData();
            return null;
          }
          for (Value value : iterator.next().getValuesList()) {
            buffer.add(value.getStringValue());
          }
        }
      }
    };
  }

  static List<String> consume(Iterator<PartialResultSet> iterator) {
    return Lists.newArrayList(stringIterator(iterator));
  }

  static List<String> consumeAtMost(int n, Iterator<PartialResultSet> iterator) {
    Iterator<String> stringIterator = stringIterator(iterator);
    List<String> r = new ArrayList<>(n);
    for (int i = 0; i < n; ++i) {
      if (stringIterator.hasNext()) {
        r.add(stringIterator.next());
      }
    }
    return r;
  }
}
