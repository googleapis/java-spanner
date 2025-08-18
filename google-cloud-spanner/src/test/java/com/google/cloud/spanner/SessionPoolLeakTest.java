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
import static org.junit.Assume.assumeFalse;

import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.auth.mtls.DefaultMtlsProviderFactory;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.SessionPool.LeakedSessionException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
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

  private static boolean originalSkipMtls;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    originalSkipMtls = DefaultMtlsProviderFactory.SKIP_MTLS.get();
    DefaultMtlsProviderFactory.SKIP_MTLS.set(true);
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
    DefaultMtlsProviderFactory.SKIP_MTLS.set(originalSkipMtls);
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
  public void testIgnoreLeakedSession() {
    for (boolean trackStackTraceofSessionCheckout : new boolean[] {true, false}) {
      SessionPoolOptions sessionPoolOptions =
          SessionPoolOptions.newBuilder()
              .setMinSessions(0)
              .setMaxSessions(2)
              .setIncStep(1)
              .setFailOnSessionLeak()
              .setTrackStackTraceOfSessionCheckout(trackStackTraceofSessionCheckout)
              .build();
      assumeFalse(
          "Session Leaks do not occur with Multiplexed Sessions",
          sessionPoolOptions.getUseMultiplexedSession());
      SpannerOptions.Builder builder =
          SpannerOptions.newBuilder()
              .setProjectId("[PROJECT]")
              .setChannelProvider(channelProvider)
              .setCredentials(NoCredentials.getInstance());
      builder.setSessionPoolOption(sessionPoolOptions);
      Spanner spanner = builder.build().getService();
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      mockSpanner.putStatementResult(
          StatementResult.query(
              Statement.of("SELECT 1"),
              com.google.spanner.v1.ResultSet.newBuilder()
                  .setMetadata(
                      ResultSetMetadata.newBuilder()
                          .setRowType(
                              StructType.newBuilder()
                                  .addFields(
                                      Field.newBuilder()
                                          .setName("c")
                                          .setType(
                                              Type.newBuilder().setCode(TypeCode.INT64).build())
                                          .build())
                                  .build())
                          .build())
                  .addRows(
                      ListValue.newBuilder()
                          .addValues(Value.newBuilder().setStringValue("1").build())
                          .build())
                  .build()));

      // Start a read-only transaction without closing it before closing the Spanner instance.
      // This will cause a session leak.
      ReadOnlyTransaction transaction = client.readOnlyTransaction();
      try (ResultSet resultSet = transaction.executeQuery(Statement.of("SELECT 1"))) {
        //noinspection StatementWithEmptyBody
        while (resultSet.next()) {
          // ignore
        }
      }
      LeakedSessionException exception = assertThrows(LeakedSessionException.class, spanner::close);
      // The top of the stack trace will be "markCheckedOut" if we keep track of the point where the
      // session was checked out, while it will be "closeAsync" if we don't. In the latter case, we
      // get the stack trace of the method that tries to close the Spanner instance, while in the
      // former the stack trace will contain the method that checked out the session.
      assertEquals(
          trackStackTraceofSessionCheckout ? "markCheckedOut" : "closeAsync",
          exception.getStackTrace()[0].getMethodName());
    }
  }

  @Test
  public void testReadWriteTransactionExceptionOnCreateSession() {
    assumeFalse(
        "Session Leaks do not occur with Multiplexed Sessions",
        isMultiplexedSessionsEnabledForRW());
    readWriteTransactionTest(
        () ->
            mockSpanner.setBatchCreateSessionsExecutionTime(
                SimulatedExecutionTime.ofException(FAILED_PRECONDITION)),
        0);
  }

  @Test
  public void testReadWriteTransactionExceptionOnBegin() {
    assumeFalse(
        "Session Leaks do not occur with Multiplexed Sessions",
        isMultiplexedSessionsEnabledForRW());
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
    assumeFalse(
        "Session Leaks do not occur with Multiplexed Sessions",
        isMultiplexedSessionsEnabledForRW());
    transactionManagerTest(
        () ->
            mockSpanner.setBatchCreateSessionsExecutionTime(
                SimulatedExecutionTime.ofException(FAILED_PRECONDITION)),
        0);
  }

  @Test
  public void testTransactionManagerExceptionOnBegin() {
    assumeFalse(
        "Session Leaks do not occur with Multiplexed Sessions",
        isMultiplexedSessionsEnabledForRW());
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
      SpannerException e = assertThrows(SpannerException.class, txManager::begin);
      assertEquals(ErrorCode.FAILED_PRECONDITION, e.getErrorCode());
    }
    assertEquals(expectedNumberOfSessionsAfterExecution, pool.getNumberOfSessionsInPool());
  }

  private boolean isMultiplexedSessionsEnabledForRW() {
    if (spanner.getOptions() == null || spanner.getOptions().getSessionPoolOptions() == null) {
      return false;
    }
    return spanner.getOptions().getSessionPoolOptions().getUseMultiplexedSessionForRW();
  }
}
