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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ITMicroBenchmarkRead extends AbstractMockServerTest {

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
  public void testSingleUseQuery() throws InterruptedException {
    final String SELECT_QUERY = "SELECT * FROM random WHERE 1=1";

    mockSpanner.putStatementResult(
        StatementResult.query(Statement.of(SELECT_QUERY), SELECT1_RESULTSET));

    Instant warmUpEndTime = Instant.now().plus(5, ChronoUnit.MINUTES);
    int waitTimeMilli = 5;

    System.out.println("Running warmup for 5 minutes, Started at " + currentTimeInIST());
    while (warmUpEndTime.isAfter(Instant.now())) {
      try (ReadContext readContext = client.singleUse()) {
        try (ResultSet resultSet =
            readContext.read("random", KeySet.all(), Collections.singletonList("*"))) {
          while (resultSet.next()) {}
        }
      }
      randomWait(waitTimeMilli);
    }
    System.out.println("Warmup completed");

    List<Long> beforeGrpcs = new ArrayList<>();
    List<Long> afterGrpcs = new ArrayList<>();
    Instant perfEndTime = Instant.now().plus(30, ChronoUnit.MINUTES);

    System.out.println("Running benchmarking for 30 minutes, Started at " + currentTimeInIST());
    while (perfEndTime.isAfter(Instant.now())) {
      try (ReadContext readContext = client.singleUse()) {
        try (ResultSet resultSet =
            readContext.read("random", KeySet.all(), Collections.singletonList("*"))) {
          while (resultSet.next()) {}
          assertFalse(resultSet.next());
        }
      }
      randomWait(waitTimeMilli);
    }

    Collections.sort(beforeGrpcs);
    Collections.sort(afterGrpcs);
    System.out.println(
        "Total time spent in the client library before requesting data from grpc "
            + percentile(beforeGrpcs, 0.5));
    System.out.println(
        "Total time spent in the client library after receiving PartialResultSet from grpc "
            + percentile(afterGrpcs, 0.5));
  }

  private String currentTimeInIST() {
    return ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toString();
  }

  public void randomWait(int waitMillis) throws InterruptedException {
    if (waitMillis <= 0) {
      return;
    }
    int randomMillis = ThreadLocalRandom.current().nextInt(waitMillis * 2);
    Thread.sleep(randomMillis);
  }

  public static long percentile(List<Long> latencies, double percentile) {
    int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
    return latencies.get(index - 1);
  }
}
