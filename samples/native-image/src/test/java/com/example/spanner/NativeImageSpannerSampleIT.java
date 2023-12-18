/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.testing.junit4.StdOutCaptureRule;
import org.junit.Rule;
import org.junit.Test;

public class NativeImageSpannerSampleIT {

  @Rule public StdOutCaptureRule stdOut = new StdOutCaptureRule();

  @Test
  public void testStoreAndRead() throws Exception {
    NativeImageSpannerSample.main(new String[] {});
    assertThat(stdOut.getCapturedOutputAsUtf8String()).contains("Singers Registered in Spanner:");
    assertThat(stdOut.getCapturedOutputAsUtf8String()).contains("Virginia Watson");
    assertThat(stdOut.getCapturedOutputAsUtf8String()).contains("Bob Loblaw");
  }
}
