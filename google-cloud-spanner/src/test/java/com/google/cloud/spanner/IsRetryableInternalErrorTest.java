package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.InternalException;
import com.google.common.base.Predicate;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class IsRetryableInternalErrorTest {

  private Predicate<Throwable> predicate;

  @Before
  public void setUp() {
    predicate = new IsRetryableInternalError();
  }

  @Test
  public void http2ErrorStatusRuntimeExceptionIsRetryable() {
    final StatusRuntimeException e = new StatusRuntimeException(
        Status
            .fromCode(Code.INTERNAL)
            .withDescription("INTERNAL: HTTP/2 error code: INTERNAL_ERROR.")
    );

    assertThat(predicate.apply(e)).isTrue();
  }

  @Test
  public void http2ErrorInternalExceptionIsRetryable() {
    final InternalException e = new InternalException(
        "INTERNAL: HTTP/2 error code: INTERNAL_ERROR.",
        null,
        GrpcStatusCode.of(Code.INTERNAL),
        false
    );

    assertThat(predicate.apply(e)).isTrue();
  }

  @Test
  public void connectionClosedStatusRuntimeExceptionIsRetryable() {
    final StatusRuntimeException e = new StatusRuntimeException(
        Status
            .fromCode(Code.INTERNAL)
            .withDescription("INTERNAL: Connection closed with unknown cause.")
    );

    assertThat(predicate.apply(e)).isTrue();
  }

  @Test
  public void connectionClosedInternalExceptionIsRetryable() {
    final InternalException e = new InternalException(
        "INTERNAL: Connection closed with unknown cause.",
        null,
        GrpcStatusCode.of(Code.INTERNAL),
        false
    );

    assertThat(predicate.apply(e)).isTrue();
  }

  @Test
  public void eosStatusRuntimeExceptionIsRetryable() {
    final StatusRuntimeException e = new StatusRuntimeException(
        Status
            .fromCode(Code.INTERNAL)
            .withDescription("INTERNAL: Received unexpected EOS on DATA frame from server.")
    );

    assertThat(predicate.apply(e)).isTrue();
  }

  @Test
  public void eosInternalExceptionIsRetryable() {
    final InternalException e = new InternalException(
        "INTERNAL: Received unexpected EOS on DATA frame from server.",
        null,
        GrpcStatusCode.of(Code.INTERNAL),
        false
    );

    assertThat(predicate.apply(e)).isTrue();
  }

  @Test
  public void genericInternalStatusRuntimeExceptionIsRetryable() {
    final StatusRuntimeException e = new StatusRuntimeException(
        Status
            .fromCode(Code.INTERNAL)
            .withDescription("INTERNAL: Generic.")
    );

    assertThat(predicate.apply(e)).isFalse();
  }

  @Test
  public void genericInternalExceptionIsNotRetryable() {
    final InternalException e = new InternalException(
        "INTERNAL: Generic.",
        null,
        GrpcStatusCode.of(Code.INTERNAL),
        false
    );

    assertThat(predicate.apply(e)).isFalse();
  }

  @Test
  public void nullIsNotRetryable() {
    assertThat(predicate.apply(null)).isFalse();
  }

  @Test
  public void genericExceptionIsNotRetryable() {
    assertThat(predicate.apply(new Exception())).isFalse();
  }
}
