/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XGoogSpannerRequestIdTest {
  private static final Pattern REGEX_RAND_PROCESS_ID =
      Pattern.compile("1.([0-9a-z]{16})(\\.\\d+){3}\\.(\\d+)$");

  @Test
  public void testEquals() {
    XGoogSpannerRequestId reqID1 = XGoogSpannerRequestId.of(1, 1, 1, 1);
    XGoogSpannerRequestId reqID2 = XGoogSpannerRequestId.of(1, 1, 1, 1);
    assertEquals(reqID1, reqID2);
    assertEquals(reqID1, reqID1);
    assertEquals(reqID2, reqID2);

    XGoogSpannerRequestId reqID3 = XGoogSpannerRequestId.of(1, 1, 1, 2);
    assertNotEquals(reqID1, reqID3);
    assertNotEquals(reqID3, reqID1);
    assertEquals(reqID3, reqID3);
  }

  @Test
  public void testEnsureHexadecimalFormatForRandProcessID() {
    String str = XGoogSpannerRequestId.of(1, 2, 3, 4).toString();
    Matcher m = XGoogSpannerRequestIdTest.REGEX_RAND_PROCESS_ID.matcher(str);
    assertTrue(m.matches());
  }

  public static class ServerHeaderEnforcer implements ServerInterceptor {
    private List<String> gotValues;

    ServerHeaderEnforcer() {
      this.gotValues = new ArrayList<String>();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        final Metadata requestHeaders,
        ServerCallHandler<ReqT, RespT> next) {
      // Firstly assert and validate that at least we've got a requestId.
      String gotReqId = requestHeaders.get(XGoogSpannerRequestId.REQUEST_HEADER_KEY);
      assertNotNull(gotReqId);
      Matcher m = XGoogSpannerRequestIdTest.REGEX_RAND_PROCESS_ID.matcher(gotReqId);
      String message =
          String.format(
              "%s lacks %s in %s",
              call.getMethodDescriptor().getFullMethodName(),
              XGoogSpannerRequestId.REQUEST_HEADER_KEY.name(),
              gotReqId);
      System.out.println("\033[32mMessage: " + message + "\033[00m");
      assertTrue(m.matches());

      this.gotValues.add(gotReqId);

      // Finally proceed with the call.
      return next.startCall(call, requestHeaders);
    }

    public String[] accumulatedValues() {
      return this.gotValues.toArray(new String[0]);
    }
  }
}
