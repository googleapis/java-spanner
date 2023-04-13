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

package com.google.cloud.spanner.connection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MemoryTest extends AbstractMockServerTest {
  private static final int NUM_QUERIES = 5000;
  private static final int NUM_RESULTS = 100;

  private static final int NUM_CLIENTS = 50;

  private static final int CONCURRENCY = 400;

  private static final String SQL = "select * from my_table where id=@id";

  @BeforeClass
  public static void setupQueryResults() {
    Random random = new Random();
    byte[] bytes = new byte[256];
    for (long id = 0L; id < NUM_RESULTS; id++) {
      random.nextBytes(bytes);
      mockSpanner.putStatementResult(
          MockSpannerServiceImpl.StatementResult.query(
              Statement.newBuilder(SQL).bind("id").to(id).build(),
              com.google.spanner.v1.ResultSet.newBuilder()
                  .setMetadata(
                      ResultSetMetadata.newBuilder()
                          .setRowType(
                              StructType.newBuilder()
                                  .addFields(
                                      Field.newBuilder()
                                          .setType(
                                              Type.newBuilder().setCode(TypeCode.INT64).build())
                                          .setName("id")
                                          .build())
                                  .addFields(
                                      Field.newBuilder()
                                          .setType(
                                              Type.newBuilder().setCode(TypeCode.BYTES).build())
                                          .setName("value")
                                          .build())
                                  .build())
                          .build())
                  .addRows(
                      ListValue.newBuilder()
                          .addValues(Value.newBuilder().setStringValue(String.valueOf(id)).build())
                          .addValues(
                              Value.newBuilder()
                                  .setStringValue(Base64.getEncoder().encodeToString(bytes))
                                  .build())
                          .build())
                  .build()));
    }
  }

  // Results from running the test multiple times for a single level of concurrency.
  // The from and to values are the lowest and highest seen for that level of concurrency.

  // Running with -Xms256m -Xmx2048m on a Java 11 JDK.
  // CONCURRENCY =  25, Max mem usage: 146MB to 182MB
  // CONCURRENCY =  50, Max mem usage: 154MB to 176MB
  // CONCURRENCY =  75, Max mem usage: 140MB to 173MB
  // CONCURRENCY = 100, Max mem usage: 140MB to 192MB
  // CONCURRENCY = 125, Max mem usage: 131MB to 166MB
  // CONCURRENCY = 150, Max mem usage: 143MB to 196MB
  // CONCURRENCY = 200, Max mem usage: 149MB to 167MB
  // CONCURRENCY = 250, Max mem usage: 164MB to 193MB
  // CONCURRENCY = 300, Max mem usage: 153MB to 179MB
  // CONCURRENCY = 350, Max mem usage: 169MB to 190MB
  // CONCURRENCY = 400, Max mem usage: 168MB to 192MB

  @Test
  public void testMemoryUsage() throws Exception {
    List<Spanner> spanners = new ArrayList<>(NUM_CLIENTS);
    for (int i = 0; i < NUM_CLIENTS; i++) {
      spanners.add(
          SpannerOptions.newBuilder()
              .setProjectId("my-project")
              .setHost("http://localhost:" + getPort())
              .setCredentials(NoCredentials.getInstance())
              .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
              .build()
              .getService());
    }

    ConcurrentLinkedQueue<Integer> memoryUsage = new ConcurrentLinkedQueue<>();
    Random random = new Random();
    ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
    for (int i = 0; i < NUM_QUERIES; i++) {
      executor.submit(
          () -> {
            Spanner spanner = spanners.get(random.nextInt(NUM_CLIENTS));
            DatabaseClient client =
                spanner.getDatabaseClient(
                    DatabaseId.of("my-project", "my-instance", "my-database"));
            try (ResultSet resultSet =
                client
                    .singleUse()
                    .executeQuery(
                        Statement.newBuilder(SQL)
                            .bind("id")
                            .to(random.nextInt(NUM_RESULTS))
                            .build())) {
              while (resultSet.next()) {
                assertNotNull(resultSet.getValue(0).getAsString());
                memoryUsage.add(
                    Long.valueOf(
                            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                                / 1024
                                / 1024)
                        .intValue());
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }
    executor.shutdown();
    assertTrue(executor.awaitTermination(10L, TimeUnit.MINUTES));
    System.out.printf(
        "Max memory usage: %d\n", memoryUsage.stream().mapToInt(v -> v).max().orElse(0));
  }
}
