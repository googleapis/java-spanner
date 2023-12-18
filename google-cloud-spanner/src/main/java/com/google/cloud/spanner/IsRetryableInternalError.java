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

import com.google.api.gax.rpc.InternalException;
import com.google.common.base.Predicate;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class IsRetryableInternalError implements Predicate<Throwable> {

  private static final String HTTP2_ERROR_MESSAGE = "HTTP/2 error code: INTERNAL_ERROR";
  private static final String CONNECTION_CLOSED_ERROR_MESSAGE =
      "Connection closed with unknown cause";
  private static final String EOS_ERROR_MESSAGE =
      "Received unexpected EOS on DATA frame from server";

  private static final String RST_STREAM_ERROR_MESSAGE = "stream terminated by RST_STREAM";

  @Override
  public boolean apply(Throwable cause) {
    if (isInternalError(cause)) {
      if (cause.getMessage().contains(HTTP2_ERROR_MESSAGE)) {
        return true;
      } else if (cause.getMessage().contains(CONNECTION_CLOSED_ERROR_MESSAGE)) {
        return true;
      } else if (cause.getMessage().contains(EOS_ERROR_MESSAGE)) {
        return true;
      } else if (cause.getMessage().contains(RST_STREAM_ERROR_MESSAGE)) {
        return true;
      }
    }
    return false;
  }

  private boolean isInternalError(Throwable cause) {
    return (cause instanceof InternalException)
        || (cause instanceof StatusRuntimeException
            && ((StatusRuntimeException) cause).getStatus().getCode() == Status.Code.INTERNAL);
  }
}
