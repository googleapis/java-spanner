/*
 * Copyright 2021 Google LLC
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

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.TypeCode;
import io.grpc.ForwardingServerCall;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.opencensus.stats.AggregationData;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewData;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GfeLatencyTest {

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

  @BeforeClass
  public static void startServer() throws IOException {
    SpannerRpcViews.registerGfeLatencyAndHeaderMissingCountViews();

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
    spanner = createSpannerOptions(address, server).getService();
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
    spannerNoHeader = createSpannerOptions(addressNoHeader, serverNoHeader).getService();
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
  public void testGfeLatencyExecuteStreamingSql() throws InterruptedException {
    try (ResultSet rs = databaseClient.singleUse().executeQuery(SELECT1AND2)) {
      rs.next();
    }

    long latency =
        getMetric(
            SpannerRpcViews.SPANNER_GFE_LATENCY_VIEW,
            projectId,
            instanceId,
            databaseId,
            "google.spanner.v1.Spanner/ExecuteStreamingSql",
            false);
    assertEquals(fakeServerTiming.get(), latency);
  }

  @Test
  public void testGfeLatencyExecuteSql() throws InterruptedException {
    databaseClient
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(UPDATE_FOO_STATEMENT));

    long latency =
        getMetric(
            SpannerRpcViews.SPANNER_GFE_LATENCY_VIEW,
            projectId,
            instanceId,
            databaseId,
            "google.spanner.v1.Spanner/ExecuteSql",
            false);
    assertEquals(fakeServerTiming.get(), latency);
  }

  @Test
  public void testGfeMissingHeaderCountExecuteStreamingSql() throws InterruptedException {
    try (ResultSet rs = databaseClient.singleUse().executeQuery(SELECT1AND2)) {
      rs.next();
    }
    long count =
        getMetric(
            SpannerRpcViews.SPANNER_GFE_HEADER_MISSING_COUNT_VIEW,
            projectId,
            instanceId,
            databaseId,
            "google.spanner.v1.Spanner/ExecuteStreamingSql",
            false);
    assertEquals(0, count);

    try (ResultSet rs = databaseClientNoHeader.singleUse().executeQuery(SELECT1AND2)) {
      rs.next();
    }
    long count1 =
        getMetric(
            SpannerRpcViews.SPANNER_GFE_HEADER_MISSING_COUNT_VIEW,
            projectId,
            instanceId,
            databaseId,
            "google.spanner.v1.Spanner/ExecuteStreamingSql",
            true);
    assertEquals(1, count1);
  }

  @Test
  public void testGfeMissingHeaderExecuteSql() throws InterruptedException {
    databaseClient
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(UPDATE_FOO_STATEMENT));
    long count =
        getMetric(
            SpannerRpcViews.SPANNER_GFE_HEADER_MISSING_COUNT_VIEW,
            projectId,
            instanceId,
            databaseId,
            "google.spanner.v1.Spanner/ExecuteSql",
            false);
    assertEquals(0, count);

    databaseClientNoHeader
        .readWriteTransaction()
        .run(transaction -> transaction.executeUpdate(UPDATE_FOO_STATEMENT));
    long count1 =
        getMetric(
            SpannerRpcViews.SPANNER_GFE_HEADER_MISSING_COUNT_VIEW,
            projectId,
            instanceId,
            databaseId,
            "google.spanner.v1.Spanner/ExecuteSql",
            true);
    assertEquals(1, count1);
  }

  private static SpannerOptions createSpannerOptions(InetSocketAddress address, Server server) {
    String endpoint = address.getHostString() + ":" + server.getPort();
    return SpannerOptions.newBuilder()
        .setProjectId("[PROJECT]")
        // Set a custom channel configurator to allow http instead of https.
        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
        .disableDirectPath()
        .setHost("http://" + endpoint)
        // Set static credentials that will return the static OAuth test token.
        .setCredentials(STATIC_CREDENTIALS)
        // Also set a CallCredentialsProvider. These credentials should take precedence above
        // the static credentials.
        .setCallCredentialsProvider(() -> MoreCallCredentials.from(VARIABLE_CREDENTIALS))
        .build();
  }

  private long getAggregationValueAsLong(AggregationData aggregationData) {
    return aggregationData.match(
        new io.opencensus.common.Function<AggregationData.SumDataDouble, Long>() {
          @Override
          public Long apply(AggregationData.SumDataDouble arg) {
            return (long) arg.getSum();
          }
        },
        new io.opencensus.common.Function<AggregationData.SumDataLong, Long>() {
          @Override
          public Long apply(AggregationData.SumDataLong arg) {
            return arg.getSum();
          }
        },
        new io.opencensus.common.Function<AggregationData.CountData, Long>() {
          @Override
          public Long apply(AggregationData.CountData arg) {
            return arg.getCount();
          }
        },
        new io.opencensus.common.Function<AggregationData.DistributionData, Long>() {
          @Override
          public Long apply(AggregationData.DistributionData arg) {
            return (long) arg.getMean();
          }
        },
        new io.opencensus.common.Function<AggregationData.LastValueDataDouble, Long>() {
          @Override
          public Long apply(AggregationData.LastValueDataDouble arg) {
            return (long) arg.getLastValue();
          }
        },
        new io.opencensus.common.Function<AggregationData.LastValueDataLong, Long>() {
          @Override
          public Long apply(AggregationData.LastValueDataLong arg) {
            return arg.getLastValue();
          }
        },
        new io.opencensus.common.Function<AggregationData, Long>() {
          @Override
          public Long apply(AggregationData arg) {
            throw new UnsupportedOperationException();
          }
        });
  }

  private long getMetric(
      View view,
      String projectId,
      String instanceId,
      String databaseId,
      String method,
      boolean withOverride)
      throws InterruptedException {
    List<TagValue> tagValues = new java.util.ArrayList<>();
    for (TagKey column : view.getColumns()) {
      if (column == SpannerRpcViews.INSTANCE_ID) {
        tagValues.add(TagValue.create(instanceId));
      } else if (column == SpannerRpcViews.DATABASE_ID) {
        tagValues.add(TagValue.create(databaseId));
      } else if (column == SpannerRpcViews.METHOD) {
        tagValues.add(TagValue.create(method));
      } else if (column == SpannerRpcViews.PROJECT_ID) {
        tagValues.add(TagValue.create(projectId));
      }
    }
    for (int i = 0; i < MAXIMUM_RETRIES; i++) {
      Thread.sleep(WAIT_FOR_METRICS_TIME_MS);
      ViewData viewData = SpannerRpcViews.viewManager.getView(view.getName());
      if (viewData.getAggregationMap() != null) {
        Map<List<TagValue>, AggregationData> aggregationMap = viewData.getAggregationMap();
        AggregationData aggregationData = aggregationMap.get(tagValues);
        if (withOverride && getAggregationValueAsLong(aggregationData) == 0) {
          continue;
        }
        return getAggregationValueAsLong(aggregationData);
      }
    }
    return -1;
  }
}
