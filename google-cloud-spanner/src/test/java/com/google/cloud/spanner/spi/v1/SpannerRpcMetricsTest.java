/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.spi.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.testing.EmulatorSpannerHelper;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.TypeCode;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpannerRpcMetricsTest {

  private static final String STATIC_OAUTH_TOKEN = "STATIC_TEST_OAUTH_TOKEN";
  private static final String VARIABLE_OAUTH_TOKEN = "VARIABLE_TEST_OAUTH_TOKEN";
  private static final OAuth2Credentials STATIC_CREDENTIALS =
      OAuth2Credentials.create(
          new AccessToken(
              STATIC_OAUTH_TOKEN,
              new java.util.Date(
                  System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS))));
  private static final OAuth2Credentials VARIABLE_CREDENTIALS =
      OAuth2Credentials.create(
          new AccessToken(
              VARIABLE_OAUTH_TOKEN,
              new java.util.Date(
                  System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS))));

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static InetSocketAddress address;
  private static Spanner spanner;
  private static DatabaseClient databaseClient;

  private static final Map<SpannerRpc.Option, Object> optionsMap = new HashMap<>();

  private static MockSpannerServiceImpl mockSpannerNoHeader;
  private static Server serverNoHeader;
  private static InetSocketAddress addressNoHeader;
  private static Spanner spannerNoHeader;
  private static DatabaseClient databaseClientNoHeader;

  private static String instanceId = "fake-instance";
  private static String databaseId = "fake-database";
  private static String projectId = "fake-project";

  private static final long WAIT_FOR_METRICS_TIME_MS = 1_000;
  private static final int MAXIMUM_RETRIES = 5;

  private static AtomicInteger fakeServerTiming = new AtomicInteger(new Random().nextInt(1000) + 1);

  private static final Statement SELECT1AND2 =
      Statement.of("SELECT 1 AS COL1 UNION ALL SELECT 2 AS COL1");

  private static final ResultSetMetadata SELECT1AND2_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      StructType.Field.newBuilder()
                          .setName("COL1")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.INT64)
                                  .build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                  .build())
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(SELECT1AND2_METADATA)
          .build();
  private static final Statement UPDATE_FOO_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");

  private static InMemoryMetricReader inMemoryMetricReader;

  private static InMemoryMetricReader inMemoryMetricReaderNoHeader;

  @BeforeClass
  public static void startServer() throws IOException {
    assumeFalse(EmulatorSpannerHelper.isUsingEmulator());

    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(
        MockSpannerServiceImpl.StatementResult.query(SELECT1AND2, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(
        MockSpannerServiceImpl.StatementResult.update(UPDATE_FOO_STATEMENT, 1L));
    address = new InetSocketAddress("localhost", 0);
    server =
        NettyServerBuilder.forAddress(address)
            .addService(mockSpanner)
            .intercept(
                new ServerInterceptor() {
                  @Override
                  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                      ServerCall<ReqT, RespT> serverCall,
                      Metadata headers,
                      ServerCallHandler<ReqT, RespT> serverCallHandler) {
                    return serverCallHandler.startCall(
                        new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(
                            serverCall) {
                          @Override
                          public void sendHeaders(Metadata headers) {
                            headers.put(
                                Metadata.Key.of("server-timing", Metadata.ASCII_STRING_MARSHALLER),
                                String.format("gfet4t7; dur=%d", fakeServerTiming.get()));
                            super.sendHeaders(headers);
                          }
                        },
                        headers);
                  }
                })
            .build()
            .start();
    optionsMap.put(SpannerRpc.Option.CHANNEL_HINT, 1L);
    inMemoryMetricReader = InMemoryMetricReader.create();
    spanner = createSpannerOptions(address, server, inMemoryMetricReader).getService();
    databaseClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

    mockSpannerNoHeader = new MockSpannerServiceImpl();
    mockSpannerNoHeader.setAbortProbability(0.0D);
    mockSpannerNoHeader.putStatementResult(
        MockSpannerServiceImpl.StatementResult.query(SELECT1AND2, SELECT1_RESULTSET));
    mockSpannerNoHeader.putStatementResult(
        MockSpannerServiceImpl.StatementResult.update(UPDATE_FOO_STATEMENT, 1L));
    addressNoHeader = new InetSocketAddress("localhost", 0);
    serverNoHeader =
        NettyServerBuilder.forAddress(addressNoHeader)
            .addService(mockSpannerNoHeader)
            .build()
            .start();
    spannerNoHeader =
        createSpannerOptions(addressNoHeader, serverNoHeader, inMemoryMetricReader).getService();
    databaseClientNoHeader =
        spannerNoHeader.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    if (spanner != null) {
      spanner.close();
      server.shutdown();
      server.awaitTermination();
    }

    if (spannerNoHeader != null) {
      spannerNoHeader.close();
      serverNoHeader.shutdown();
      serverNoHeader.awaitTermination();
    }
  }

  @After
  public void reset() {
    mockSpanner.reset();
    mockSpannerNoHeader.reset();
  }

  @Test
  public void testGfeLatencyExecuteSql() throws InterruptedException {
    databaseClient
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(UPDATE_FOO_STATEMENT));

    ;
    double latency =
        getGfeLatencyMetric(getMetricData("gfe_latency"), "google.spanner.v1.Spanner/ExecuteSql");
    assertEquals(fakeServerTiming.get(), latency, 0);
  }

  @Test
  public void testGfeMissingHeaderExecuteSql() throws InterruptedException {
    databaseClient
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(UPDATE_FOO_STATEMENT));
    long count =
        getHeaderLatencyMetric(
            getMetricData("gfe_header_missing_count"), "google.spanner.v1.Spanner/ExecuteSql");
    assertEquals(0, count);

    databaseClientNoHeader
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(UPDATE_FOO_STATEMENT));
    long count1 =
        getHeaderLatencyMetric(
            getMetricData("gfe_header_missing_count"), "google.spanner.v1.Spanner/ExecuteSql");
    assertEquals(1, count1);
  }

  private static SpannerOptions createSpannerOptions(
      InetSocketAddress address, Server server, InMemoryMetricReader inMemoryMetricReader) {
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder().registerMetricReader(inMemoryMetricReader).build();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
    String endpoint = address.getHostString() + ":" + server.getPort();
    return SpannerOptions.newBuilder()
        .setProjectId("[PROJECT]")
        // Set a custom channel configurator to allow http instead of https.
        .setChannelConfigurator(
            input -> {
              input.usePlaintext();
              return input;
            })
        .setHost("http://" + endpoint)
        // Set static credentials that will return the static OAuth test token.
        .setCredentials(STATIC_CREDENTIALS)
        // Also set a CallCredentialsProvider. These credentials should take precedence above
        // the static credentials.
        .setCallCredentialsProvider(() -> MoreCallCredentials.from(VARIABLE_CREDENTIALS))
        .setOpenTelemetry(openTelemetry)
        .build();
  }

  private long getHeaderLatencyMetric(MetricData metricData, String methodName) {
    // Attributes attributes =
    return metricData.getLongSumData().getPoints().stream()
        .filter(x -> x.getAttributes().asMap().containsValue(methodName))
        .findFirst()
        .get()
        .getValue();
  }

  private double getGfeLatencyMetric(MetricData metricData, String methodName) {
    // Attributes attributes =
    return metricData.getHistogramData().getPoints().stream()
        .filter(x -> x.getAttributes().asMap().containsValue(methodName))
        .findFirst()
        .get()
        .getMax();
  }

  private MetricData getMetricData(String metricName) {
    Collection<MetricData> metricDataCollection = inMemoryMetricReader.collectAllMetrics();
    Collection<MetricData> metricDataFiltered =
        metricDataCollection.stream()
            .filter(x -> x.getName().equals(metricName))
            .collect(Collectors.toList());
    return metricDataFiltered.stream().findFirst().get();
  }
}
