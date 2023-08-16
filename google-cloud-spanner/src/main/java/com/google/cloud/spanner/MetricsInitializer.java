package com.google.cloud.spanner;

import com.google.cloud.spanner.spi.v1.OpenTelemetryOptions;
import com.google.cloud.spanner.spi.v1.RPCView;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.util.List;
import java.util.Optional;

public class MetricsInitializer {

  private static Meter meter;

  private static OpenTelemetry openTelemetry;

  // Metrics
  private static LongHistogram gfeLatencies = null;

  private static LongCounter gfeHeaderMissingCount = null;

  private MetricsInitializer() {}

  public static void initializeOpenTelemetry() {

    if(openTelemetry == null) {

      SdkMeterProviderBuilder sdkMeterProviderBuilder = SdkMeterProvider.builder();
      List<RPCView> registeredViews = OpenTelemetryOptions.getRegisteredViews();

      registeredViews.forEach(
          view -> {
            Optional<RPCViewImpl> viewImpl = RPCViewImpl.getRPCViewByName(view.name());
            viewImpl.ifPresent(
                rpcView ->
                {
                  sdkMeterProviderBuilder.registerView(
                      rpcView.getInstrumentSelector(), rpcView.getView());
                });
          });

      sdkMeterProviderBuilder.registerMetricReader(
          PeriodicMetricReader.builder(OpenTelemetryOptions.getMetricsExporter())
              .setInterval(java.time.Duration.ofSeconds(5)).build());

      openTelemetry =
          OpenTelemetrySdk.builder()
              .setMeterProvider(sdkMeterProviderBuilder.build())
              // .setTracerProvider(OpenTelemetryOptions.getSdkTracerProviderBuilder().build())
              .build();

      meter = openTelemetry.meterBuilder(MetricRegistryConstants.Scope)
          .setInstrumentationVersion("1.0.0")
          .build();

      registeredViews.forEach(view -> initializeRPCViewMetrics(view, meter));
    }

  }

  private static void initializeRPCViewMetrics(RPCView rpcView, Meter meter) {
    switch (rpcView) {
      case SPANNER_GFE_LATENCY_VIEW:
        gfeLatencies = meter
            .histogramBuilder(RPCViewAttributes.SPANNER_GFE_LATENCY_NAME)
            .ofLongs()
            .setDescription(RPCViewAttributes.SPANNER_GFE_LATENCY_DESCRIPTION)
            .setUnit(RPCViewAttributes.MILLISECOND)
            .build();
        break;
      case SPANNER_GFE_HEADER_MISSING_COUNT_VIEW:
        gfeHeaderMissingCount = meter
            .counterBuilder(RPCViewAttributes.SPANNER_GFE_HEADER_MISSING_COUNT_NAME)
            .setDescription(RPCViewAttributes.SPANNER_GFE_HEADER_MISSING_COUNT_DESCRIPTION)
            .setUnit(RPCViewAttributes.COUNT)
            .build();
        break;
    }

  }
  public static OpenTelemetry getOpenTelemetryObject() {
    return openTelemetry;
  }

  public static void gfeLatencyRecorder(long value, Attributes attributes) {
    if(gfeLatencies != null)
      gfeLatencies.record(value, attributes);
  }

  public static void gfeHeaderMissingCountRecorder(long value, Attributes attributes) {
    if(gfeHeaderMissingCount != null)
      gfeHeaderMissingCount.add(value, attributes);
  }



}
