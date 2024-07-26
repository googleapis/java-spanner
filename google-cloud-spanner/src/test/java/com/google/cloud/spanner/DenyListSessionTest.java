/*
 * Copyright 2024 Google LLC
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
import static org.junit.Assert.assertThrows;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.spanner.v1.BeginTransactionRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class DenyListSessionTest extends AbstractMockServerTest {
  private Spanner spanner;

  @Before
  public void createSpanner() {
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId("my-project")
            .setHost(String.format("http://localhost:%d", getPort()))
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .setCredentials(NoCredentials.getInstance());
    builder
        .getSpannerStubSettingsBuilder()
        .beginTransactionSettings()
        .setSimpleTimeoutNoRetries(Duration.ofMillis(100));
    this.spanner = builder.build().getService();
  }

  @After
  public void closeSpanner() {
    this.spanner.close();
  }

  @Test
  public void testDeadlineExceededForBeginTransaction() {
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofMinimumAndRandomTime(1000, 0));
    DatabaseClient client = this.spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));

    SpannerException exception =
        assertThrows(
            SpannerException.class,
            () ->
                client
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          transaction.buffer(
                              Mutation.newInsertBuilder("foo").set("id").to(1L).build());
                          return null;
                        }));
    assertEquals(ErrorCode.DEADLINE_EXCEEDED, exception.getErrorCode());

    mockSpanner.removeAllExecutionTimes();

    client
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.buffer(Mutation.newInsertBuilder("foo").set("id").to(1L).build());
              return null;
            });
    assertEquals(2, mockSpanner.countRequestsOfType(BeginTransactionRequest.class));
    List<BeginTransactionRequest> requests =
        mockSpanner.getRequestsOfType(BeginTransactionRequest.class);
    assertEquals(requests.get(0).getSession(), requests.get(1).getSession());
  }
}
