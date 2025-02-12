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

import static com.google.common.truth.Truth.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XGoogSpannerRequestIdTest {
  private static String RAND_PROCESS_ID = XGoogSpannerRequestId.RAND_PROCESS_ID;
  private static Pattern REGEX_RAND_PROCESS_ID =
      Pattern.compile("1.([0-9a-z]{8})(\\.\\d+){3}\\.(\\d+)$");

  @Test
  public void testEquals() {
    XGoogSpannerRequestId reqID1 = XGoogSpannerRequestId.of(1, 1, 1, 1);
    XGoogSpannerRequestId reqID2 = XGoogSpannerRequestId.of(1, 1, 1, 1);
    assertThat(reqID1).isEqualTo(reqID2);
    assertThat(reqID1).isEqualTo(reqID1);
    assertThat(reqID2).isEqualTo(reqID2);

    XGoogSpannerRequestId reqID3 = XGoogSpannerRequestId.of(1, 1, 1, 2);
    assertThat(reqID1).isNotEqualTo(reqID3);
    assertThat(reqID3).isNotEqualTo(reqID1);
    assertThat(reqID3).isEqualTo(reqID3);
  }

  @Test
  public void testEnsureHexadecimalFormatForRandProcessID() {
    String str = XGoogSpannerRequestId.of(1, 2, 3, 4).toString();
    Matcher m = REGEX_RAND_PROCESS_ID.matcher(str);
    assertThat(m.matches()).isEqualTo(true);
  }
}
