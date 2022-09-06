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
