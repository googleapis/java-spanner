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
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XGoogSpannerRequestIdTest {

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
    Matcher m = XGoogSpannerRequestId.REGEX.matcher(str);
    assertTrue(m.matches());
  }

  public static class ServerHeaderEnforcer implements ServerInterceptor {
    private Map<String, CopyOnWriteArrayList<XGoogSpannerRequestId>> unaryResults;
    private Map<String, CopyOnWriteArrayList<XGoogSpannerRequestId>> streamingResults;
    private List<String> gotValues;
    private Set<String> checkMethods;

    ServerHeaderEnforcer(Set<String> checkMethods) {
      this.gotValues = new CopyOnWriteArrayList<String>();
      this.unaryResults =
          new ConcurrentHashMap<String, CopyOnWriteArrayList<XGoogSpannerRequestId>>();
      this.streamingResults =
          new ConcurrentHashMap<String, CopyOnWriteArrayList<XGoogSpannerRequestId>>();
      this.checkMethods = checkMethods;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        final Metadata requestHeaders,
        ServerCallHandler<ReqT, RespT> next) {
      boolean isUnary = call.getMethodDescriptor().getType() == MethodType.UNARY;
      String methodName = call.getMethodDescriptor().getFullMethodName();
      String gotReqIdStr = requestHeaders.get(XGoogSpannerRequestId.REQUEST_HEADER_KEY);
      if (!this.checkMethods.contains(methodName)) {
        // System.out.println(
        //     "\033[35mBypassing " + methodName + " but has " + gotReqIdStr + "\033[00m");
        return next.startCall(call, requestHeaders);
      }

      Map<String, CopyOnWriteArrayList<XGoogSpannerRequestId>> saver = this.streamingResults;
      if (isUnary) {
        saver = this.unaryResults;
      }

      // Firstly assert and validate that at least we've got a requestId.
      Matcher m = XGoogSpannerRequestId.REGEX.matcher(gotReqIdStr);
      assertNotNull(gotReqIdStr);
      assertTrue(m.matches());

      XGoogSpannerRequestId reqId = XGoogSpannerRequestId.of(gotReqIdStr);
      if (!saver.containsKey(methodName)) {
        saver.put(methodName, new CopyOnWriteArrayList<XGoogSpannerRequestId>());
      }

      saver.get(methodName).add(reqId);

      // Finally proceed with the call.
      return next.startCall(call, requestHeaders);
    }

    public String[] accumulatedValues() {
      return this.gotValues.toArray(new String[0]);
    }

    public void assertIntegrity() {
      this.unaryResults.forEach(
          (String method, CopyOnWriteArrayList<XGoogSpannerRequestId> values) -> {
            // System.out.println("\033[36munary.method: " + method + "\033[00m");
            XGoogSpannerRequestId.assertMonotonicityOfIds(method, values);
          });
      this.streamingResults.forEach(
          (String method, CopyOnWriteArrayList<XGoogSpannerRequestId> values) -> {
            // System.out.println("\033[36mstreaming.method: " + method + "\033[00m");
            XGoogSpannerRequestId.assertMonotonicityOfIds(method, values);
          });
    }

    public void reset() {
      this.gotValues.clear();
      this.unaryResults.clear();
      this.streamingResults.clear();
    }
  }
}
