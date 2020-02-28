/*
 * Copyright 2020 Google LLC
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

import static com.google.cloud.spanner.MockSpannerTestUtil.*;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.common.base.Function;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.SettableFuture;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReadAsyncTest {
  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;

  private static ExecutorService executor;
  private Spanner spanner;
  private DatabaseClient client;

  @BeforeClass
  public static void setup() throws Exception {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.putStatementResult(
        StatementResult.query(READ_ONE_KEY_VALUE_STATEMENT, READ_ONE_KEY_VALUE_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.query(READ_ONE_EMPTY_KEY_VALUE_STATEMENT, EMPTY_KEY_VALUE_RESULTSET));
    mockSpanner.putStatementResult(
        StatementResult.query(
            READ_MULTIPLE_KEY_VALUE_STATEMENT, READ_MULTIPLE_KEY_VALUE_RESULTSET));

    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
    executor = Executors.newScheduledThreadPool(8);
  }

  @AfterClass
  public static void teardown() throws Exception {
    executor.shutdown();
    server.shutdown();
    server.awaitTermination();
  }

  @Before
  public void before() {
    spanner =
        SpannerOptions.newBuilder()
            .setProjectId(TEST_PROJECT)
            .setChannelProvider(channelProvider)
            .setCredentials(NoCredentials.getInstance())
            .setSessionPoolOption(
                SessionPoolOptions.newBuilder().setFailOnSessionLeak().setMinSessions(0).build())
            .build()
            .getService();
    client = spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, TEST_DATABASE));
  }

  @After
  public void after() {
    spanner.close();
    mockSpanner.removeAllExecutionTimes();
  }

  @Test
  public void emptyReadAsync() throws Exception {
    final SettableFuture<Boolean> result = SettableFuture.create();
    AsyncResultSet resultSet =
        client
            .singleUse(TimestampBound.strong())
            .readAsync(EMPTY_READ_TABLE_NAME, KeySet.singleKey(Key.of("k99")), READ_COLUMN_NAMES);
    resultSet.setCallback(
        executor,
        new ReadyCallback() {
          @Override
          public CallbackResponse cursorReady(AsyncResultSet resultSet) {
            try {
              while (true) {
                switch (resultSet.tryNext()) {
                  case OK:
                    fail("received unexpected data");
                  case NOT_READY:
                    return CallbackResponse.CONTINUE;
                  case DONE:
                    assertThat(resultSet.getType()).isEqualTo(READ_TABLE_TYPE);
                    result.set(true);
                    return CallbackResponse.DONE;
                }
              }
            } catch (Throwable t) {
              result.setException(t);
              return CallbackResponse.DONE;
            }
          }
        });
    assertThat(result.get()).isTrue();
  }

  @Test
  public void pointReadAsync() throws Exception {
    ApiFuture<Struct> row =
        client
            .singleUse(TimestampBound.strong())
            .readRowAsync(READ_TABLE_NAME, Key.of("k1"), READ_COLUMN_NAMES);
    assertThat(row.get()).isNotNull();
    assertThat(row.get().getString(0)).isEqualTo("k1");
    assertThat(row.get().getString(1)).isEqualTo("v1");
  }

  @Test
  public void pointReadNotFound() throws Exception {
    ApiFuture<Struct> row =
        client
            .singleUse(TimestampBound.strong())
            .readRowAsync(EMPTY_READ_TABLE_NAME, Key.of("k999"), READ_COLUMN_NAMES);
    assertThat(row.get()).isNull();
  }

  @Test
  public void invalidDatabase() throws Exception {
    mockSpanner.setBatchCreateSessionsExecutionTime(
        SimulatedExecutionTime.stickyDatabaseNotFoundException("invalid-database"));
    DatabaseClient invalidClient =
        spanner.getDatabaseClient(DatabaseId.of(TEST_PROJECT, TEST_INSTANCE, "invalid-database"));
    ApiFuture<Struct> row =
        invalidClient
            .singleUse(TimestampBound.strong())
            .readRowAsync(READ_TABLE_NAME, Key.of("k99"), READ_COLUMN_NAMES);
    try {
      row.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(DatabaseNotFoundException.class);
    }
  }

  @Test
  public void tableNotFound() throws Exception {
    mockSpanner.setStreamingReadExecutionTime(
        SimulatedExecutionTime.ofException(
            Status.NOT_FOUND
                .withDescription("Table not found: BadTableName")
                .asRuntimeException()));
    ApiFuture<Struct> row =
        client
            .singleUse(TimestampBound.strong())
            .readRowAsync("BadTableName", Key.of("k1"), READ_COLUMN_NAMES);
    try {
      row.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      assertThat(se.getMessage()).contains("BadTableName");
    }
  }

  /**
   * Ending a read-only transaction before an asynchronous query that was executed on that
   * transaction has finished fetching all rows should keep the session checked out of the pool
   * until all the rows have been returned. The session is then automatically returned to the
   * session.
   */
  @Test
  public void closeTransactionBeforeEndOfAsyncQuery() throws Exception {
    final BlockingQueue<String> results = new SynchronousQueue<>();
    final SettableApiFuture<Boolean> finished = SettableApiFuture.create();
    DatabaseClientImpl clientImpl = (DatabaseClientImpl) client;

    // There should currently not be any sessions checked out of the pool.
    assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(0);

    final CountDownLatch dataReceived = new CountDownLatch(1);
    try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
      try (AsyncResultSet rs =
          tx.readAsync(READ_TABLE_NAME, KeySet.all(), READ_COLUMN_NAMES, Options.bufferRows(1))) {
        rs.setCallback(
            executor,
            new ReadyCallback() {
              @Override
              public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                try {
                  while (true) {
                    switch (resultSet.tryNext()) {
                      case DONE:
                        finished.set(true);
                        return CallbackResponse.DONE;
                      case NOT_READY:
                        return CallbackResponse.CONTINUE;
                      case OK:
                        dataReceived.countDown();
                        results.put(resultSet.getString(0));
                    }
                  }
                } catch (Throwable t) {
                  finished.setException(t);
                  return CallbackResponse.DONE;
                }
              }
            });
      }
      // Wait until at least one row has been fetched. At that moment there should be one session
      // checked out.
      dataReceived.await();
      assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(1);
    }
    // The read-only transaction is now closed, but the ready callback will continue to receive
    // data. As it tries to put the data into a synchronous queue and the underlying buffer can also
    // only hold 1 row, the async result set has not yet finished. The read-only transaction will
    // release the session back into the pool when all async statements have finished. The number of
    // sessions in use is therefore still 1.
    assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(1);
    List<String> resultList = new ArrayList<>();
    do {
      results.drainTo(resultList);
    } while (!finished.isDone() || results.size() > 0);
    assertThat(finished.get()).isTrue();
    assertThat(resultList).containsExactly("k1", "k2", "k3");
    // The session will be released back into the pool by the asynchronous result set when it has
    // returned all rows. As this is done in the background, it could take a couple of milliseconds.
    Thread.sleep(10L);
    assertThat(clientImpl.pool.getNumberOfSessionsInUse()).isEqualTo(0);
  }

  @Test
  public void readOnlyTransaction() throws Exception {
    Statement statement1 =
        Statement.of("SELECT * FROM TestTable WHERE Key IN ('k10', 'k11', 'k12')");
    Statement statement2 = Statement.of("SELECT * FROM TestTable WHERE Key IN ('k1', 'k2', 'k3");
    mockSpanner.putStatementResult(
        StatementResult.query(statement1, generateKeyValueResultSet(ContiguousSet.closed(10, 12))));
    mockSpanner.putStatementResult(
        StatementResult.query(statement2, generateKeyValueResultSet(ContiguousSet.closed(1, 3))));

    ApiFuture<ImmutableList<String>> values1;
    ApiFuture<ImmutableList<String>> values2;
    try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
      try (AsyncResultSet rs = tx.executeQueryAsync(statement1)) {
        values1 =
            rs.toListAsync(
                new Function<StructReader, String>() {
                  @Override
                  public String apply(StructReader input) {
                    return input.getString("Value");
                  }
                },
                executor);
      }
      try (AsyncResultSet rs = tx.executeQueryAsync(statement2)) {
        values2 =
            rs.toListAsync(
                new Function<StructReader, String>() {
                  @Override
                  public String apply(StructReader input) {
                    return input.getString("Value");
                  }
                },
                executor);
      }
    }
    ApiFuture<Iterable<String>> allValues =
        ApiFutures.transform(
            ApiFutures.allAsList(Arrays.asList(values1, values2)),
            new ApiFunction<List<ImmutableList<String>>, Iterable<String>>() {
              @Override
              public Iterable<String> apply(List<ImmutableList<String>> input) {
                return Iterables.mergeSorted(
                    input,
                    new Comparator<String>() {
                      @Override
                      public int compare(String o1, String o2) {
                        // Return in numerical order (i.e. without the preceding 'v').
                        return Integer.valueOf(o1.substring(1))
                            .compareTo(Integer.valueOf(o2.substring(1)));
                      }
                    });
              }
            },
            executor);
    assertThat(allValues.get()).containsExactly("v1", "v2", "v3", "v10", "v11", "v12");
  }

  @Test
  public void pauseResume() throws Exception {
    Statement unevenStatement =
        Statement.of("SELECT * FROM TestTable WHERE MOD(CAST(SUBSTR(Key, 2) AS INT64), 2) = 1");
    Statement evenStatement =
        Statement.of("SELECT * FROM TestTable WHERE MOD(CAST(SUBSTR(Key, 2) AS INT64), 2) = 0");
    mockSpanner.putStatementResult(
        StatementResult.query(
            unevenStatement, generateKeyValueResultSet(ImmutableSet.of(1, 3, 5, 7, 9))));
    mockSpanner.putStatementResult(
        StatementResult.query(
            evenStatement, generateKeyValueResultSet(ImmutableSet.of(2, 4, 6, 8, 10))));

    final Object lock = new Object();
    final SettableApiFuture<Boolean> evenFinished = SettableApiFuture.create();
    final SettableApiFuture<Boolean> unevenFinished = SettableApiFuture.create();
    final CountDownLatch unevenReturnedFirstRow = new CountDownLatch(1);
    final Deque<String> allValues = new ConcurrentLinkedDeque<>();
    try (ReadOnlyTransaction tx = client.readOnlyTransaction()) {
      try (AsyncResultSet evenRs = tx.executeQueryAsync(evenStatement);
          AsyncResultSet unevenRs = tx.executeQueryAsync(unevenStatement)) {
        unevenRs.setCallback(
            executor,
            new ReadyCallback() {
              @Override
              public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                try {
                  while (true) {
                    switch (resultSet.tryNext()) {
                      case DONE:
                        unevenFinished.set(true);
                        return CallbackResponse.DONE;
                      case NOT_READY:
                        return CallbackResponse.CONTINUE;
                      case OK:
                        synchronized (lock) {
                          allValues.add(resultSet.getString("Value"));
                        }
                        unevenReturnedFirstRow.countDown();
                        return CallbackResponse.PAUSE;
                    }
                  }
                } catch (Throwable t) {
                  unevenFinished.setException(t);
                  return CallbackResponse.DONE;
                }
              }
            });
        evenRs.setCallback(
            executor,
            new ReadyCallback() {
              @Override
              public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                try {
                  // Make sure the uneven result set has returned the first before we start the even
                  // results.
                  unevenReturnedFirstRow.await();
                  while (true) {
                    switch (resultSet.tryNext()) {
                      case DONE:
                        evenFinished.set(true);
                        return CallbackResponse.DONE;
                      case NOT_READY:
                        return CallbackResponse.CONTINUE;
                      case OK:
                        synchronized (lock) {
                          allValues.add(resultSet.getString("Value"));
                        }
                        return CallbackResponse.PAUSE;
                    }
                  }
                } catch (Throwable t) {
                  evenFinished.setException(t);
                  return CallbackResponse.DONE;
                }
              }
            });
        while (!(evenFinished.isDone() && unevenFinished.isDone())) {
          synchronized (lock) {
            if (allValues.peekLast() != null) {
              if (Integer.valueOf(allValues.peekLast().substring(1)) % 2 == 1) {
                evenRs.resume();
              } else {
                unevenRs.resume();
              }
            }
            if (allValues.size() == 10) {
              unevenRs.resume();
              evenRs.resume();
            }
          }
        }
      }
    }
    assertThat(ApiFutures.allAsList(Arrays.asList(evenFinished, unevenFinished)).get())
        .containsExactly(Boolean.TRUE, Boolean.TRUE);
    assertThat(allValues)
        .containsExactly("v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10");
  }

  @Test
  public void cancel() throws Exception {
    final List<String> values = new LinkedList<>();
    final SettableApiFuture<Boolean> finished = SettableApiFuture.create();
    final CountDownLatch receivedFirstRow = new CountDownLatch(1);
    final CountDownLatch cancelled = new CountDownLatch(1);
    try (AsyncResultSet rs =
        client.singleUse().readAsync(READ_TABLE_NAME, KeySet.all(), READ_COLUMN_NAMES)) {
      rs.setCallback(
          executor,
          new ReadyCallback() {
            @Override
            public CallbackResponse cursorReady(AsyncResultSet resultSet) {
              try {
                while (true) {
                  switch (resultSet.tryNext()) {
                    case DONE:
                      finished.set(true);
                      return CallbackResponse.DONE;
                    case NOT_READY:
                      return CallbackResponse.CONTINUE;
                    case OK:
                      values.add(resultSet.getString("Value"));
                      receivedFirstRow.countDown();
                      cancelled.await();
                      break;
                  }
                }
              } catch (Throwable t) {
                finished.setException(t);
                return CallbackResponse.DONE;
              }
            }
          });
      receivedFirstRow.await();
      rs.cancel();
    }
    cancelled.countDown();
    try {
      finished.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) e.getCause();
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.CANCELLED);
      assertThat(values).containsExactly("v1");
    }
  }
}
