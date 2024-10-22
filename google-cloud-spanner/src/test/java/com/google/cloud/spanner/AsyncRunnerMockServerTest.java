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
import static org.junit.Assert.assertTrue;

import com.google.api.core.ApiFuture;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.connection.AbstractMockServerTest;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Context;
import io.grpc.Context.CancellableContext;
import io.grpc.Deadline;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@RunWith(JUnit4.class)
public class AsyncRunnerMockServerTest extends AbstractMockServerTest {
  private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(8);

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

  @Test
  public void testMutations() throws Exception {
    // Simulate that a Commit RPC on Spanner takes 500 milliseconds.
    mockSpanner.setCommitExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(500, 0));

    try (Spanner spanner = createSpanner()) {
      DatabaseClient client = spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      ApiFuture<Void> result = insertRowWithGrpcContext(client);
      ExecutionException executionException = assertThrows(ExecutionException.class, result::get);
      assertTrue(executionException.getCause() instanceof SpannerException);
      SpannerException spannerException = (SpannerException) executionException.getCause();
      assertEquals(ErrorCode.CANCELLED, spannerException.getErrorCode());
      assertEquals("CANCELLED: Current context was cancelled", spannerException.getMessage());
    }
  }

  private ApiFuture<Void> insertRowWithGrpcContext(DatabaseClient client) throws Exception {
    // Create a gRPC context with a timeout of 1000 milliseconds.
    try (CancellableContext context =
        Context.current().withDeadline(Deadline.after(1000, TimeUnit.MILLISECONDS), EXECUTOR)) {
      return context.call(
          () -> {
            // Schedule the context to be cancelled in 100 milliseconds from now.
            EXECUTOR.schedule(
                () -> context.cancel(new RuntimeException("test")), 100, TimeUnit.MILLISECONDS);
            // Asynchronously insert a row while the cancellable context is the current context.
            // This will cancel the insert when the context is cancelled.
            // The insert will take 500 milliseconds on Spanner due to the simulated 500-millisecond
            // delay introduced above. That means that the context will be cancelled before the
            // insert actually finishes.
            return insertRowAsync(
                client, Mutation.newInsertBuilder("foo").set("id").to(1L).build());
          });
    }
  }

  private ApiFuture<Void> insertRowAsync(DatabaseClient client, Mutation mutation) {
    return client
        .runAsync()
        .runAsync(txn -> txn.bufferAsync(mutation), MoreExecutors.directExecutor());
  }
}
