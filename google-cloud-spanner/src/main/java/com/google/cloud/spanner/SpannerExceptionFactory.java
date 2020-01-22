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

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.spanner.SpannerException.DoNotConstructDirectly;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.net.ssl.SSLHandshakeException;

/**
 * A factory for creating instances of {@link SpannerException} and its subtypes. All creation of
 * these exceptions is directed through the factory. This ensures that particular types of errors
 * are always expressed as the same concrete exception type. For example, exceptions of type {@link
 * ErrorCode#ABORTED} are always represented by {@link AbortedException}.
 */
public final class SpannerExceptionFactory {
  public static SpannerException newSpannerException(ErrorCode code, @Nullable String message) {
    return newSpannerException(code, message, null);
  }

  public static SpannerException newSpannerException(
      ErrorCode code, @Nullable String message, @Nullable Throwable cause) {
    return newSpannerExceptionPreformatted(code, formatMessage(code, message), cause);
  }

  public static SpannerException propagateInterrupt(InterruptedException e) {
    Thread.currentThread().interrupt();
    return SpannerExceptionFactory.newSpannerException(ErrorCode.CANCELLED, "Interrupted", e);
  }

  /**
   * Transforms a {@code TimeoutException} to a {@code SpannerException}.
   *
   * <pre>
   * <code>
   * try {
   *   Spanner spanner = SpannerOptions.getDefaultInstance();
   *   spanner
   *       .getDatabaseAdminClient()
   *       .createDatabase("[INSTANCE_ID]", "[DATABASE_ID]", [STATEMENTS])
   *       .get();
   * } catch (TimeoutException e) {
   *   propagateTimeout(e);
   * }
   * </code>
   * </pre>
   */
  public static SpannerException propagateTimeout(TimeoutException e) {
    return SpannerExceptionFactory.newSpannerException(
        ErrorCode.DEADLINE_EXCEEDED, "Operation did not complete in the given time", e);
  }

  /**
   * Creates a new exception based on {@code cause}.
   *
   * <p>Intended for internal library use; user code should use {@link
   * #newSpannerException(ErrorCode, String)} instead of this method.
   */
  public static SpannerException newSpannerException(Throwable cause) {
    return newSpannerException(null, cause);
  }

  public static SpannerBatchUpdateException newSpannerBatchUpdateException(
      ErrorCode code, String message, long[] updateCounts) {
    DoNotConstructDirectly token = DoNotConstructDirectly.ALLOWED;
    return new SpannerBatchUpdateException(token, code, message, updateCounts);
  }

  /**
   * Constructs a specific aborted exception that should only be thrown by a connection after an
   * internal retry aborted due to concurrent modifications.
   */
  public static AbortedDueToConcurrentModificationException
      newAbortedDueToConcurrentModificationException(AbortedException cause) {
    return new AbortedDueToConcurrentModificationException(
        DoNotConstructDirectly.ALLOWED,
        "The transaction was aborted and could not be retried due to a concurrent modification",
        cause);
  }

  /**
   * Constructs a specific aborted exception that should only be thrown by a connection after an
   * internal retry aborted because a database call caused an exception that did not happen during
   * the original attempt.
   */
  public static AbortedDueToConcurrentModificationException
      newAbortedDueToConcurrentModificationException(
          AbortedException cause, SpannerException databaseError) {
    return new AbortedDueToConcurrentModificationException(
        DoNotConstructDirectly.ALLOWED,
        "The transaction was aborted and could not be retried due to a database error during the retry",
        cause,
        databaseError);
  }

  /**
   * Creates a new exception based on {@code cause}. If {@code cause} indicates cancellation, {@code
   * context} will be inspected to establish the type of cancellation.
   *
   * <p>Intended for internal library use; user code should use {@link
   * #newSpannerException(ErrorCode, String)} instead of this method.
   */
  public static SpannerException newSpannerException(@Nullable Context context, Throwable cause) {
    if (cause instanceof SpannerException) {
      SpannerException e = (SpannerException) cause;
      return newSpannerExceptionPreformatted(e.getErrorCode(), e.getMessage(), e);
    } else if (cause instanceof CancellationException) {
      return newSpannerExceptionForCancellation(context, cause);
    } else if (cause instanceof ApiException) {
      return fromApiException((ApiException) cause);
    }
    // Extract gRPC status.  This will produce "UNKNOWN" for non-gRPC exceptions.
    Status status = Status.fromThrowable(cause);
    if (status.getCode() == Status.Code.CANCELLED) {
      return newSpannerExceptionForCancellation(context, cause);
    }
    return newSpannerException(ErrorCode.fromGrpcStatus(status), cause.getMessage(), cause);
  }

  static SpannerException newSpannerExceptionForCancellation(
      @Nullable Context context, @Nullable Throwable cause) {
    if (context != null && context.isCancelled()) {
      Throwable cancellationCause = context.cancellationCause();
      Throwable throwable =
          cause == null && cancellationCause == null
              ? null
              : MoreObjects.firstNonNull(cause, cancellationCause);
      if (cancellationCause instanceof TimeoutException) {
        return newSpannerException(
            ErrorCode.DEADLINE_EXCEEDED, "Current context exceeded deadline", throwable);
      } else {
        return newSpannerException(ErrorCode.CANCELLED, "Current context was cancelled", throwable);
      }
    }
    return newSpannerException(
        ErrorCode.CANCELLED, cause == null ? "Cancelled" : cause.getMessage(), cause);
  }

  private static String formatMessage(ErrorCode code, @Nullable String message) {
    if (message == null) {
      return code.toString();
    }
    // gRPC exceptions already start with the code, which happens to be the same prefix we use.
    return message.startsWith(code.toString()) ? message : code + ": " + message;
  }

  private static SpannerException newSpannerExceptionPreformatted(
      ErrorCode code, @Nullable String message, @Nullable Throwable cause) {
    // This is the one place in the codebase that is allowed to call constructors directly.
    DoNotConstructDirectly token = DoNotConstructDirectly.ALLOWED;
    switch (code) {
      case ABORTED:
        return new AbortedException(token, message, cause);
      case NOT_FOUND:
        if (message != null && message.contains("Session not found")) {
          return new SessionNotFoundException(token, message, cause);
        } else if (message != null && message.contains("Database not found")) {
          return new DatabaseNotFoundException(token, message, cause);
        }
        // Fall through to the default.
      default:
        return new SpannerException(token, code, isRetryable(code, cause), message, cause);
    }
  }

  private static SpannerException fromApiException(ApiException exception) {
    Status.Code code = ((GrpcStatusCode) exception.getStatusCode()).getTransportCode();
    ErrorCode errorCode = ErrorCode.fromGrpcStatus(Status.fromCode(code));
    if (exception.getCause() != null) {
      return SpannerExceptionFactory.newSpannerException(
          errorCode, exception.getMessage(), exception.getCause());
    } else {
      return SpannerExceptionFactory.newSpannerException(errorCode, exception.getMessage());
    }
  }

  private static boolean isRetryable(ErrorCode code, @Nullable Throwable cause) {
    switch (code) {
      case INTERNAL:
        return hasCauseMatching(cause, Matchers.isRetryableInternalError);
      case UNAVAILABLE:
        // SSLHandshakeException is (probably) not retryable, as it is an indication that the server
        // certificate was not accepted by the client.
        return !hasCauseMatching(cause, Matchers.isSSLHandshakeException);
      case RESOURCE_EXHAUSTED:
        return SpannerException.extractRetryDelay(cause) > 0;
      default:
        return false;
    }
  }

  private static boolean hasCauseMatching(
      @Nullable Throwable cause, Predicate<? super Throwable> matcher) {
    while (cause != null) {
      if (matcher.apply(cause)) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private static class Matchers {
    static final Predicate<Throwable> isRetryableInternalError =
        new Predicate<Throwable>() {
          @Override
          public boolean apply(Throwable cause) {
            if (cause instanceof StatusRuntimeException
                && ((StatusRuntimeException) cause).getStatus().getCode() == Status.Code.INTERNAL) {
              if (cause.getMessage().contains("HTTP/2 error code: INTERNAL_ERROR")) {
                // See b/25451313.
                return true;
              }
              if (cause.getMessage().contains("Connection closed with unknown cause")) {
                // See b/27794742.
                return true;
              }
              if (cause
                  .getMessage()
                  .contains("Received unexpected EOS on DATA frame from server")) {
                return true;
              }
            }
            return false;
          }
        };
    static final Predicate<Throwable> isSSLHandshakeException =
        new Predicate<Throwable>() {
          @Override
          public boolean apply(Throwable input) {
            return input instanceof SSLHandshakeException;
          }
        };
  }
}
