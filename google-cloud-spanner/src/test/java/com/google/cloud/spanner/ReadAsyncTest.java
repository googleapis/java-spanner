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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.core.ApiFuture;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.Type.StructField;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReadAsyncTest {
  private static final String EMPTY_TABLE_NAME = "EmptyTestTable";
  private static final String TABLE_NAME = "TestTable";
  private static final List<String> ALL_COLUMNS = Arrays.asList("Key", "StringValue");
  private static final Type TABLE_TYPE =
      Type.struct(
          StructField.of("Key", Type.string()), StructField.of("StringValue", Type.string()));
  private static final String TEST_PROJECT = "my-project";
  private static final String TEST_INSTANCE = "my-instance";
  private static final String TEST_DATABASE = "my-database";

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static LocalChannelProvider channelProvider;
  private static final Statement EMPTY_READ_STATEMENT =
      Statement.of("SELECT Key, StringValue FROM EmptyTestTable WHERE ID=1");
  private static final Statement READ1_STATEMENT =
      Statement.of("SELECT Key, StringValue FROM TestTable WHERE ID=1");
  private static final ResultSetMetadata READ1_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("Key")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.STRING)
                                  .build())
                          .build())
                  .addFields(
                      Field.newBuilder()
                          .setName("StringValue")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.STRING)
                                  .build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet EMPTY_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(ListValue.newBuilder().build())
          .setMetadata(READ1_METADATA)
          .build();
  private static final com.google.spanner.v1.ResultSet READ1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("k1").build())
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("v1").build())
                  .build())
          .setMetadata(READ1_METADATA)
          .build();

  private static ExecutorService executor;
  private Spanner spanner;
  private DatabaseClient client;

  @BeforeClass
  public static void setup() throws Exception {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.putStatementResult(StatementResult.query(READ1_STATEMENT, READ1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.query(EMPTY_READ_STATEMENT, EMPTY_RESULTSET));

    String uniqueName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(uniqueName)
            .scheduledExecutorService(new ScheduledThreadPoolExecutor(1))
            .addService(mockSpanner)
            .build()
            .start();
    channelProvider = LocalChannelProvider.create(uniqueName);
    executor = Executors.newSingleThreadExecutor();
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
            .setSessionPoolOption(SessionPoolOptions.newBuilder().setFailOnSessionLeak().build())
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
            .readAsync(EMPTY_TABLE_NAME, KeySet.singleKey(Key.of("k99")), ALL_COLUMNS);
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
                    assertThat(resultSet.getType()).isEqualTo(TABLE_TYPE);
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
            .readRowAsync(TABLE_NAME, Key.of("k1"), ALL_COLUMNS);
    assertThat(row.get()).isNotNull();
    assertThat(row.get().getString(0)).isEqualTo("k1");
    assertThat(row.get().getString(1)).isEqualTo("v1");
    assertThat(row.get())
        .isEqualTo(Struct.newBuilder().set("Key").to("k1").set("StringValue").to("v1").build());
  }

  @Test
  public void pointReadNotFound() throws Exception {
    ApiFuture<Struct> row =
        client
            .singleUse(TimestampBound.strong())
            .readRowAsync(EMPTY_TABLE_NAME, Key.of("k999"), ALL_COLUMNS);
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
            .readRowAsync(TABLE_NAME, Key.of("k99"), ALL_COLUMNS);
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
            .readRowAsync("BadTableName", Key.of("k1"), ALL_COLUMNS);
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
}
