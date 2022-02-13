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

package com.google.cloud.spanner.spi.v1;

import static org.junit.Assert.assertEquals;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StuckRequestTest {

  private static final String STATIC_OAUTH_TOKEN = "STATIC_TEST_OAUTH_TOKEN";
  private static final OAuth2Credentials STATIC_CREDENTIALS = OAuth2Credentials.create(
      new AccessToken(
          STATIC_OAUTH_TOKEN,
          new java.util.Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS))
      )
  );
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(ListValue
              .newBuilder()
              .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1")))
          .setMetadata(ResultSetMetadata
              .newBuilder()
              .setRowType(StructType
                  .newBuilder()
                  .addFields(Field
                      .newBuilder()
                      .setName("COL1")
                      .setType(com.google.spanner.v1.Type.newBuilder().setCode(TypeCode.INT64)))))
          .build();
  private MockSpannerServiceImpl mockSpanner;
  private InetSocketAddress address;
  private Server server;
  private DatabaseClient databaseClient;

  @Before
  public void setUp() throws Exception {
    mockSpanner = new MockSpannerServiceImpl();
    address = new InetSocketAddress("localhost", 0);
    server = NettyServerBuilder.forAddress(address)
        .addService(mockSpanner)
        .build();
    server.start();

    final SpannerOptions spannerOptions = createSpannerOptions();
    databaseClient = spannerOptions.getService()
        .getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
    server.awaitTermination();
  }

  @Test
  public void test() {
    // Used to initialize the session pool
    mockSpanner.putStatementResult(StatementResult.query(Statement.of("SELECT 1"), SELECT1_RESULTSET));
    try (ResultSet resultSet = databaseClient.singleUse().executeQuery(Statement.of("SELECT 1"))) {
      resultSet.next();
    }

    // Verifies no sessions are used before
    assertEquals(0L, databaseClient.getSessionsInUse());
    try {
      mockSpanner.addException(
          SpannerExceptionFactory.newSpannerException(ErrorCode.INTERNAL, "http2 exception"));
      databaseClient.writeAtLeastOnce(Collections.emptyList());
    } catch (Exception e) {
      // Ignores expected exception
    }
    // Verifies no sessions are used afterwards
    assertEquals(0L, databaseClient.getSessionsInUse());
  }

  private SpannerOptions createSpannerOptions() {
    return SpannerOptions.newBuilder()
        .setProjectId("[PROJECT]")
        // Set a custom channel configurator to allow http instead of https.
        .setChannelConfigurator(input -> {
          input.usePlaintext();
          return input;
        })
        .setHost("http://" + address.getHostString() + ":" + server.getPort())
        // Set static credentials that will return the static OAuth test token.
        .setCredentials(STATIC_CREDENTIALS)
        .build();
  }
}
