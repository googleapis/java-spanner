package com.google.cloud.spanner;

import com.google.common.base.Predicate;
import javax.net.ssl.SSLHandshakeException;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class IsSslHandshakeException implements Predicate<Throwable> {

  @Override
  public boolean apply(@NullableDecl Throwable input) {
    return input instanceof SSLHandshakeException;
  }
}
