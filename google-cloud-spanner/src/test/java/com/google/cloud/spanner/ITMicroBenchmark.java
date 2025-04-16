/*
 * Copyright 2025 Google LLC
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

package com.google.cloud.spanner;

import static org.junit.Assert.assertFalse;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ITMicroBenchmark extends AbstractMockServerTest {

  private DatabaseClient client;

  private static final ResultSetMetadata SELECT1_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
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
          .setMetadata(SELECT1_METADATA)
          .build();

  @Override
  public void createSpannerInstance() {
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder()
                    .setWaitForMinSessionsDuration(Duration.ofSeconds(5L))
                    .setFailOnSessionLeak()
                    .build())
            .setEnableApiTracing(true)
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
  }

  @Test
  public void testSingleUseQuery() {
    final String SELECT_QUERY = "SELECT * FROM random";

    System.out.println("Running warmup");
    for (int i = 0; i < 200000; i++) {
      try (ReadContext readContext = client.singleUse()) {
        try (ResultSet resultSet = readContext.executeQuery(Statement.of(SELECT_QUERY))) {
          while (resultSet.next()) {}
        }
      }
    }
    System.out.println("Warmup completed");

    mockSpanner.putStatementResult(
        StatementResult.query(Statement.of(SELECT_QUERY), SELECT1_RESULTSET));

    List<Long> beforeGrpcs = new ArrayList<>();
    List<Long> afterGrpcs = new ArrayList<>();
    for (int i = 0; i < 100000; i++) {
      PerformanceClock.BEFORE_GRPC_INSTANCE.reset();
      PerformanceClock.AFTER_GRPC_INSTANCE.reset();
      PerformanceClock.BEFORE_GRPC_INSTANCE.start();
      try (ReadContext readContext = client.singleUse()) {
        try (ResultSet resultSet = readContext.executeQuery(Statement.of(SELECT_QUERY))) {
          while (resultSet.next()) {}
          PerformanceClock.AFTER_GRPC_INSTANCE.stop();
          beforeGrpcs.add(PerformanceClock.BEFORE_GRPC_INSTANCE.elapsed(TimeUnit.MICROSECONDS));
          afterGrpcs.add(PerformanceClock.AFTER_GRPC_INSTANCE.elapsed(TimeUnit.MICROSECONDS));
          assertFalse(resultSet.next());
        }
      }
    }
    System.out.println(
        "Total time spent in the client library before requesting data from grpc "
            + percentile(beforeGrpcs, 0.5));
    System.out.println(
        "Total time spent in the client library after receiving PartialResultSet from grpc "
            + percentile(afterGrpcs, 0.5));
  }

  public static long percentile(List<Long> latencies, double percentile) {
    int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
    return latencies.get(index - 1);
  }
}
