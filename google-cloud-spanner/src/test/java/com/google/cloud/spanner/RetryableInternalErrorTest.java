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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RetryableInternalErrorTest extends AbstractMockServerTest {
  @Test
  public void testTranslateInternalException() {
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("my-project")
            .setHost(String.format("http://localhost:%d", getPort()))
            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setMinSessions(1).setMaxSessions(1).build())
            .build()
            .getService()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      mockSpanner.setBatchCreateSessionsExecutionTime(
          SimulatedExecutionTime.ofException(
              Status.INTERNAL
                  .withDescription("Authentication backend internal server error. Please retry.")
                  .asRuntimeException()));
      mockSpanner.setExecuteStreamingSqlExecutionTime(
          SimulatedExecutionTime.ofException(
              Status.INTERNAL
                  .withDescription("Authentication backend internal server error. Please retry.")
                  .asRuntimeException()));
      mockSpanner.setExecuteSqlExecutionTime(
          SimulatedExecutionTime.ofException(
              Status.INTERNAL
                  .withDescription("Authentication backend internal server error. Please retry.")
                  .asRuntimeException()));

      try (ResultSet resultSet = client.singleUse().executeQuery(SELECT1_STATEMENT)) {
        assertTrue(resultSet.next());
        assertFalse(resultSet.next());
      }
      assertEquals(
          Long.valueOf(1L),
          client
              .readWriteTransaction()
              .run(transaction -> transaction.executeUpdate(INSERT_STATEMENT)));

      // Verify that all the RPCs were retried.
      assertEquals(2, mockSpanner.countRequestsOfType(BatchCreateSessionsRequest.class));
      assertEquals(4, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    }
  }
}
