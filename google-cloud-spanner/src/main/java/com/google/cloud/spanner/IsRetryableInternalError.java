package com.google.cloud.spanner;

import com.google.api.gax.rpc.InternalException;
import com.google.common.base.Predicate;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class IsRetryableInternalError implements Predicate<Throwable> {

  private static final String HTTP2_ERROR_MESSAGE = "HTTP/2 error code: INTERNAL_ERROR";
  private static final String CONNECTION_CLOSED_ERROR_MESSAGE = "Connection closed with unknown cause";
  private static final String EOS_ERROR_MESSAGE = "Received unexpected EOS on DATA frame from server";

  @Override
  public boolean apply(@NullableDecl Throwable cause) {
    if (isInternalError(cause)) {
      if (cause.getMessage().contains(HTTP2_ERROR_MESSAGE)) {
        // See b/25451313.
        return true;
      } else if (cause.getMessage().contains(CONNECTION_CLOSED_ERROR_MESSAGE)) {
        // See b/27794742.
        return true;
      } else if (cause.getMessage().contains(EOS_ERROR_MESSAGE)) {
        return true;
      }
    }
    return false;
  }

  private boolean isInternalError(Throwable cause) {
    return (cause instanceof InternalException)
        || (cause instanceof StatusRuntimeException
        && ((StatusRuntimeException) cause).getStatus().getCode()
        == Status.Code.INTERNAL);
  }
}
