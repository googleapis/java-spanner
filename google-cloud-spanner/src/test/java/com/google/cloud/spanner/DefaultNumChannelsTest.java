/*
 * Copyright 2022 Google LLC
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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefaultNumChannelsTest {

  // With GRPC GCP channel pool enabled
  @Test
  public void testDefaultNumChannelsWithGrpcGcpEnabled() {
    SpannerOptions.Builder builder = SpannerOptions.newBuilder()
        .setProjectId("test-project")
        .enableGrpcGcpExtension();

    SpannerOptions options = builder.build();
    assertThat(options.getNumChannels()).isEqualTo(8);
  }

  @Test
  public void testNumChannelsWithGrpcGcpEnabledLater() {
    SpannerOptions.Builder builder = SpannerOptions.newBuilder()
        .setProjectId("test-project")
        .setNumChannels(5)
        .enableGrpcGcpExtension();

    SpannerOptions options = builder.build();
    assertThat(options.getNumChannels()).isEqualTo(5);
  }

  @Test
  public void testNumChannelsWithGrpcGcpEnabled() {
    SpannerOptions.Builder builder = SpannerOptions.newBuilder()
        .setProjectId("test-project")
        .enableGrpcGcpExtension()
        .setNumChannels(5);

    SpannerOptions options = builder.build();
    assertThat(options.getNumChannels()).isEqualTo(5);
  }

  // Without enabling GRPC GCP channel pool
  @Test
  public void testDefaultNumChannelsWithOutGrpcGcpEnabled() {
    SpannerOptions.Builder builder = SpannerOptions.newBuilder()
        .setProjectId("test-project");

    SpannerOptions options = builder.build();
    assertThat(options.getNumChannels()).isEqualTo(4);
  }

  @Test
  public void testNumChannelsWithOutGrpcGcpEnabled() {
    SpannerOptions.Builder builder = SpannerOptions.newBuilder()
        .setProjectId("test-project")
        .setNumChannels(7);

    SpannerOptions options = builder.build();
    assertThat(options.getNumChannels()).isEqualTo(7);
  }
}
