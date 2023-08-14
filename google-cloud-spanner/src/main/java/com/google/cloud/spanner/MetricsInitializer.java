package com.google.cloud.spanner;

import com.google.cloud.spanner.spi.v1.OpenTelemetryOptions;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;

public class MetricsInitializer {

  private static final String MILLISECOND = "ms";
  private static final String COUNT = "1";

  private static Meter meter;

  private static OpenTelemetry openTelemetry;

  // Metrics
  LongHistogram gfeLatencies = null;

  private static MetricsInitializer single_instance;

  private MetricsInitializer() {}

  public static MetricsInitializer getInstance() {
    if(single_instance == null) {
      single_instance = new MetricsInitializer();
      // createOpenTelemetryObject();
    }
    return single_instance;
  }

  public static void createOpenTelemetryObject() {
    openTelemetry =
        OpenTelemetrySdk.builder()
            .setMeterProvider(OpenTelemetryOptions.getSdkMeterProviderBuilder().build())
            .setTracerProvider(OpenTelemetryOptions.getSdkTracerProviderBuilder().build())
            .build();

    meter = openTelemetry.meterBuilder(MetricRegistryConstants.Scope)
        .setInstrumentationVersion("1.0.0")
        .build();

  }

  public OpenTelemetry getOpenTelemetryObject() {
    return openTelemetry;
  }

  void gfeLatencyInitializer() {
    gfeLatencies = meter
        .histogramBuilder(RPCViewAttributes.SPANNER_GFE_LATENCY_NAME)
        .ofLongs()
        .setDescription("Latency between Google's network receiving an RPC and reading back the first byte of the response")
        .setUnit(MILLISECOND)
        .build();
  }

  // void gfeLatencyInitializer() {
  //   gfeLatencies = meter
  //       .histogramBuilder("gfe_latency_ot")
  //       .ofLongs()
  //       .setDescription("Latency between Google's network receiving an RPC and reading back the first byte of the response")
  //       .setUnit(MILLISECOND)
  //       .build();
  // }

  public void gfeLatencyRecorder(long value, Attributes attributes) {
    if(gfeLatencies != null)
      gfeLatencies.record(value, attributes);
  }


}
