/*
 * Copyright 2025 Google LLC
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

package com.google.cloud.spanner.it;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.SpannerOptionsHelper;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.StructField;
import com.google.cloud.spanner.connection.ConnectionOptions;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.v1.TraceServiceSettings;
import com.google.devtools.cloudtrace.v1.Trace;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for End to End Tracing. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITEndToEndTracingTest {

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static DatabaseClient googleStandardSQLClient;

  static {
    SpannerOptionsHelper.resetActiveTracingFramework();
    SpannerOptions.enableOpenTelemetryMetrics();
    SpannerOptions.enableOpenTelemetryTraces();
  }

  private String selectValueQuery;

  @BeforeClass
  public static void setUp() {
    setUpDatabase();
  }

  public static void setUpDatabase() {
    // Empty database.
    Database googleStandardSQLDatabase = env.getTestHelper().createTestDatabase();
    googleStandardSQLClient = env.getTestHelper().getDatabaseClient(googleStandardSQLDatabase);
  }

  @AfterClass
  public static void teardown() {
    ConnectionOptions.closeSpanner();
  }

  @Before
  public void initSelectValueQuery() {
    selectValueQuery = "SELECT @p1 + @p1 ";
  }

  private void assertTrace(String traceId) throws IOException, InterruptedException {
    TraceServiceSettings settings =
        env.getTestHelper().getOptions().getCredentials() == null
            ? TraceServiceSettings.newBuilder().build()
            : TraceServiceSettings.newBuilder()
                .setCredentialsProvider(
                    FixedCredentialsProvider.create(
                        env.getTestHelper().getOptions().getCredentials()))
                .build();
    try (TraceServiceClient client = TraceServiceClient.create(settings)) {
      // It can take a few seconds before the trace is visible.
      Thread.sleep(5000L);
      boolean foundTrace = false;
      for (int attempts = 0; attempts < 2; attempts++) {
        try {
          Trace clientTrace =
              client.getTrace(env.getTestHelper().getInstanceId().getProject(), traceId);
          // Assert Spanner Frontend Trace is present
          assertTrue(
              clientTrace.getSpansList().stream()
                  .anyMatch(
                      span ->
                          "CloudSpannerOperation.ExecuteStreamingQuery".equals(span.getName())));
          foundTrace = true;
          break;
        } catch (ApiException apiException) {
          assertThat(apiException.getStatusCode().getCode()).isEqualTo(StatusCode.Code.NOT_FOUND);
          Thread.sleep(5000L);
        }
      }
      assertTrue(foundTrace);
    } catch (ResourceExhaustedException resourceExhaustedException) {
      if (resourceExhaustedException
          .getMessage()
          .contains("Quota exceeded for quota metric 'Read requests (free)'")) {
        // Ignore and allow the test to succeed.
        System.out.println("RESOURCE_EXHAUSTED error ignored");
      } else {
        throw resourceExhaustedException;
      }
    }
  }

  private Struct executeWithRowResultType(Statement statement, Type expectedRowType) {
    ResultSet resultSet = statement.executeQuery(googleStandardSQLClient.singleUse());
    assertThat(resultSet.next()).isTrue();
    assertThat(resultSet.getType()).isEqualTo(expectedRowType);
    Struct row = resultSet.getCurrentRowAsStruct();
    assertThat(resultSet.next()).isFalse();
    return row;
  }

  @Test
  public void simpleSelect() throws IOException, InterruptedException {
    Tracer tracer = GlobalOpenTelemetry.getTracer(ITEndToEndTracingTest.class.getName());
    Span span = tracer.spanBuilder("simpleSelect").startSpan();
    Scope scope = span.makeCurrent();
    Type rowType = Type.struct(StructField.of("", Type.int64()));
    Struct row =
        executeWithRowResultType(
            Statement.newBuilder(selectValueQuery).bind("p1").to(1234).build(), rowType);
    assertThat(row.isNull(0)).isFalse();
    assertThat(row.getLong(0)).isEqualTo(2468);
    scope.close();
    span.end();
    assertTrace(span.getSpanContext().getTraceId());
  }
}
