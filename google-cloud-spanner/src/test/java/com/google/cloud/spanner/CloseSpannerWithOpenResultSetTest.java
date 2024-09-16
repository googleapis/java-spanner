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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.cloud.spanner.spi.v1.GapicSpannerRpc;
import com.google.spanner.v1.DeleteSessionRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class CloseSpannerWithOpenResultSetTest extends AbstractMockServerTest {

  Spanner createSpanner() {
    return SpannerOptions.newBuilder()
        .setProjectId("p")
        .setHost(String.format("http://localhost:%d", getPort()))
        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
        .setCredentials(NoCredentials.getInstance())
        .setSessionPoolOption(
            SessionPoolOptions.newBuilder().setWaitForMinSessions(Duration.ofSeconds(5L)).build())
        .build()
        .getService();
  }

  @After
  public void cleanup() {
    mockSpanner.unfreeze();
    mockSpanner.clearRequests();
  }

  @Test
  public void testBatchClient_closedSpannerWithOpenResultSet_streamsAreCancelled() {
    Spanner spanner = createSpanner();
    assumeFalse(spanner.getOptions().getSessionPoolOptions().getUseMultiplexedSession());

    BatchClient client = spanner.getBatchClient(DatabaseId.of("p", "i", "d"));
    try (BatchReadOnlyTransaction transaction =
            client.batchReadOnlyTransaction(TimestampBound.strong());
        ResultSet resultSet = transaction.executeQuery(SELECT_RANDOM_STATEMENT)) {
      mockSpanner.freezeAfterReturningNumRows(1);
      assertTrue(resultSet.next());
      ((SpannerImpl) spanner).close(1, TimeUnit.MILLISECONDS);
      // This should return an error as the stream is cancelled.
      SpannerException exception = assertThrows(SpannerException.class, resultSet::next);
      assertEquals(ErrorCode.CANCELLED, exception.getErrorCode());
    }
  }

  @Test
  public void testNormalDatabaseClient_closedSpannerWithOpenResultSet_sessionsAreDeleted()
      throws Exception {
    Spanner spanner = createSpanner();
    assumeFalse(spanner.getOptions().getSessionPoolOptions().getUseMultiplexedSession());

    DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
    try (ReadOnlyTransaction transaction = client.readOnlyTransaction(TimestampBound.strong());
        ResultSet resultSet = transaction.executeQuery(SELECT_RANDOM_STATEMENT)) {
      mockSpanner.freezeAfterReturningNumRows(1);
      assertTrue(resultSet.next());
      List<ExecuteSqlRequest> executeSqlRequests =
          mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).stream()
              .filter(request -> request.getSql().equals(SELECT_RANDOM_STATEMENT.getSql()))
              .collect(Collectors.toList());
      assertEquals(1, executeSqlRequests.size());
      ExecutorService service = Executors.newSingleThreadExecutor();
      service.submit(spanner::close);
      // Verify that the session that is used by this transaction is deleted.
      // That will automatically cancel the query.
      mockSpanner.waitForRequestsToContain(
          request ->
              request instanceof DeleteSessionRequest
                  && ((DeleteSessionRequest) request)
                      .getName()
                      .equals(executeSqlRequests.get(0).getSession()),
          /*timeoutMillis=*/ 1000L);
      service.shutdownNow();
    }
  }

  @Test
  public void testStreamsAreCleanedUp() throws Exception {
    String invalidSql = "select * from foo";
    Statement invalidStatement = Statement.of(invalidSql);
    mockSpanner.putStatementResult(
        StatementResult.exception(
            invalidStatement,
            Status.NOT_FOUND.withDescription("Table not found: foo").asRuntimeException()));
    int numThreads = 16;
    int numQueries = 32;
    try (Spanner spanner = createSpanner()) {
      BatchClient client = spanner.getBatchClient(DatabaseId.of("p", "i", "d"));
      ExecutorService service = Executors.newFixedThreadPool(numThreads);
      List<Future<?>> futures = new ArrayList<>(numQueries);
      for (int n = 0; n < numQueries; n++) {
        futures.add(
            service.submit(
                () -> {
                  try (BatchReadOnlyTransaction transaction =
                      client.batchReadOnlyTransaction(TimestampBound.strong())) {
                    if (ThreadLocalRandom.current().nextInt(10) < 2) {
                      try (ResultSet resultSet = transaction.executeQuery(invalidStatement)) {
                        SpannerException exception =
                            assertThrows(SpannerException.class, resultSet::next);
                        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
                      }
                    } else {
                      try (ResultSet resultSet =
                          transaction.executeQuery(SELECT_RANDOM_STATEMENT)) {
                        while (resultSet.next()) {
                          assertNotNull(resultSet.getCurrentRowAsStruct());
                        }
                      }
                    }
                  }
                }));
      }
      service.shutdown();
      for (Future<?> fut : futures) {
        fut.get();
      }
      assertTrue(service.awaitTermination(1L, TimeUnit.MINUTES));
      // Verify that all response observers have been unregistered.
      assertEquals(
          0, ((GapicSpannerRpc) ((SpannerImpl) spanner).getRpc()).getNumActiveResponseObservers());
    }
  }
}
