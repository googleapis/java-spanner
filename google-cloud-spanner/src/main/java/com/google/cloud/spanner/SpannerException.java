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

import com.google.cloud.grpc.BaseGrpcServiceException;
import com.google.common.base.Preconditions;
import com.google.protobuf.util.Durations;
import com.google.rpc.ResourceInfo;
import com.google.rpc.RetryInfo;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import javax.annotation.Nullable;

/** Base exception type for all exceptions produced by the Cloud Spanner service. */
public class SpannerException extends BaseGrpcServiceException {
  /** Base exception type for NOT_FOUND exceptions for known resource types. */
  public abstract static class ResourceNotFoundException extends SpannerException {
    private final ResourceInfo resourceInfo;

    ResourceNotFoundException(
        DoNotConstructDirectly token,
        @Nullable String message,
        ResourceInfo resourceInfo,
        @Nullable Throwable cause) {
      super(token, ErrorCode.NOT_FOUND, /* retryable */ false, message, cause);
      this.resourceInfo = resourceInfo;
    }

    public String getResourceName() {
      return resourceInfo.getResourceName();
    }
  }

  private static final long serialVersionUID = 20150916L;
  private static final Metadata.Key<RetryInfo> KEY_RETRY_INFO =
      ProtoUtils.keyForProto(RetryInfo.getDefaultInstance());

  private final ErrorCode code;

  /** Private constructor. Use {@link SpannerExceptionFactory} to create instances. */
  SpannerException(
      DoNotConstructDirectly token,
      ErrorCode code,
      boolean retryable,
      @Nullable String message,
      @Nullable Throwable cause) {
    super(message, cause, code.getCode(), retryable);
    if (token != DoNotConstructDirectly.ALLOWED) {
      throw new AssertionError("Do not construct directly: use SpannerExceptionFactory");
    }
    this.code = Preconditions.checkNotNull(code);
  }

  /** Returns the error code associated with this exception. */
  public ErrorCode getErrorCode() {
    return code;
  }

  enum DoNotConstructDirectly {
    ALLOWED
  }

  /**
   * Return the retry delay for operation in milliseconds. Return -1 if this does not specify any
   * retry delay.
   */
  public long getRetryDelayInMillis() {
    return extractRetryDelay(this.getCause());
  }

  static long extractRetryDelay(Throwable cause) {
    if (cause != null) {
      Metadata trailers = Status.trailersFromThrowable(cause);
      if (trailers != null && trailers.containsKey(KEY_RETRY_INFO)) {
        RetryInfo retryInfo = trailers.get(KEY_RETRY_INFO);
        if (retryInfo.hasRetryDelay()) {
          return Durations.toMillis(retryInfo.getRetryDelay());
        }
      }
    }
    return -1L;
  }
}
