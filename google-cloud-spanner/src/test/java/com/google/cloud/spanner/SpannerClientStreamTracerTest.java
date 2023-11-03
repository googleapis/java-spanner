/*
 * Copyright 2023 Google LLC
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

import static org.junit.Assert.assertEquals;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.connection.RandomResultSetGenerator;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.net.InetSocketAddress;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpannerClientStreamTracerTest {
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static Spanner spanner;
  private static com.google.spanner.v1.ResultSet RANDOM_RESULT_SET;
  private static Statement STATEMENT = Statement.of("select * from random");

  @BeforeClass
  public static void setupServer() throws Exception {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D);
    // Use a real TCP connection for the server.
    InetSocketAddress address = new InetSocketAddress("localhost", 0);
    server = NettyServerBuilder.forAddress(address).addService(mockSpanner).build().start();
    spanner = createSpanner();
    RandomResultSetGenerator generator = new RandomResultSetGenerator(10);
    RANDOM_RESULT_SET = generator.generate();
    mockSpanner.putStatementResult(StatementResult.query(STATEMENT, RANDOM_RESULT_SET));
  }

  public static void stopServer() throws Exception {
    spanner.close();
    server.shutdown();
    server.awaitTermination();
  }

  private static Spanner createSpanner() {
    return SpannerOptions.newBuilder()
        .setProjectId("test-project")
        .setHost(String.format("http://localhost:%d", server.getPort()))
        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
        .setCredentials(NoCredentials.getInstance())
        .build()
        .getService();
  }

  @Test
  public void testSingleUseQuery() {
    mockSpanner.setExecuteStreamingSqlExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.UNAVAILABLE.withDescription("test error").asRuntimeException()));
    DatabaseClient client =
        spanner.getDatabaseClient(DatabaseId.of("test-project", "test-instance", "test-database"));
    try (ResultSet resultSet = client.singleUse().executeQuery(STATEMENT)) {
      while (resultSet.next()) {
        // ignore and just consume the results
      }
    }
    assertEquals(2, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
  }
}
