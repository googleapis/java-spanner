/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SessionPoolLeakTest {
  private static final StatusRuntimeException FAILED_PRECONDITION =
      io.grpc.Status.FAILED_PRECONDITION
          .withDescription("Non-retryable test exception.")
          .asRuntimeException();
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private Spanner spanner;
  private DatabaseClient client;
  private SessionPool pool;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
  }

  @Before
  public void setUp() {
    mockSpanner.reset();
    mockSpanner.removeAllExecutionTimes();
    SpannerOptions.Builder builder =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance());
    // Make sure the session pool is empty by default, does not contain any sessions,
    // contains at most 2 sessions, and creates sessions in steps of 1.
    builder.setSessionPoolOption(
        SessionPoolOptions.newBuilder().setMinSessions(0).setMaxSessions(2).setIncStep(1).build());
    spanner = builder.build().getService();
    client = spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
    pool = ((DatabaseClientImpl) client).pool;
  }

  @After
  public void tearDown() {
    spanner.close();
  }

  @Test
  public void testReadWriteTransactionExceptionOnCreateSession() {
    readWriteTransactionTest(
        () ->
            mockSpanner.setBatchCreateSessionsExecutionTime(
                SimulatedExecutionTime.ofException(FAILED_PRECONDITION)),
        0);
  }

  @Test
  public void testReadWriteTransactionExceptionOnBegin() {
    readWriteTransactionTest(
        () ->
            mockSpanner.setBeginTransactionExecutionTime(
                SimulatedExecutionTime.ofException(FAILED_PRECONDITION)),
        1);
  }

  private void readWriteTransactionTest(
      Runnable setup, int expectedNumberOfSessionsAfterExecution) {
    assertEquals(0, pool.getNumberOfSessionsInPool());
    setup.run();
    SpannerException e =
        assertThrows(
            SpannerException.class, () -> client.readWriteTransaction().run(transaction -> null));
    assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
    assertEquals(expectedNumberOfSessionsAfterExecution, pool.getNumberOfSessionsInPool());
  }

  @Test
  public void testTransactionManagerExceptionOnCreateSession() {
    transactionManagerTest(
        () ->
            mockSpanner.setBatchCreateSessionsExecutionTime(
                SimulatedExecutionTime.ofException(FAILED_PRECONDITION)),
        0);
  }

  @Test
  public void testTransactionManagerExceptionOnBegin() {
    assertThat(pool.getNumberOfSessionsInPool(), is(equalTo(0)));
    mockSpanner.setBeginTransactionExecutionTime(
        SimulatedExecutionTime.ofException(FAILED_PRECONDITION));
    try (TransactionManager txManager = client.transactionManager()) {
      // This should not cause an error, as the actual BeginTransaction will be included with the
      // first statement of the transaction.
      txManager.begin();
    }
    assertThat(pool.getNumberOfSessionsInPool(), is(equalTo(1)));
  }

  private void transactionManagerTest(Runnable setup, int expectedNumberOfSessionsAfterExecution) {
    assertEquals(0, pool.getNumberOfSessionsInPool());
    setup.run();
    try (TransactionManager txManager = client.transactionManager()) {
      SpannerException e = assertThrows(SpannerException.class, () -> txManager.begin());
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
    }
    assertEquals(expectedNumberOfSessionsAfterExecution, pool.getNumberOfSessionsInPool());
  }
}
