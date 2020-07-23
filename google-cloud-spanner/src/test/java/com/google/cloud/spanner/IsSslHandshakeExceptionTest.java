package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.InternalException;
import com.google.common.base.Predicate;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class IsSslHandshakeExceptionTest {

  private Predicate<Throwable> predicate;

  @Before
  public void setUp() {
    predicate = new IsSslHandshakeException();
  }

  @Test
  public void sslHandshakeExceptionIsTrue() {
    assertThat(predicate.apply(new SSLHandshakeException("test"))).isTrue();
  }

  @Test
  public void genericExceptionIsNotSslHandshakeException() {
    assertThat(predicate.apply(new Exception("test"))).isFalse();
  }

  @Test
  public void nullIsNotSslHandshakeException() {
    assertThat(predicate.apply(null)).isFalse();
  }
}
