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
import static org.junit.Assert.assertTrue;

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
}
