/*
 * Copyright 2017 Google LLC
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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.api.gax.grpc.GrpcCallContext;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ServerStreamingCallSettings;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.TransportOptions;
import com.google.cloud.spanner.SpannerOptions.FixedCloseableExecutorProvider;
import com.google.cloud.spanner.SpannerOptions.SpannerCallContextTimeoutConfigurator;
import com.google.cloud.spanner.admin.database.v1.stub.DatabaseAdminStubSettings;
import com.google.cloud.spanner.admin.instance.v1.stub.InstanceAdminStubSettings;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.base.Strings;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CreateSessionRequest;
import com.google.spanner.v1.DeleteSessionRequest;
import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.DirectedReadOptions.IncludeReplicas;
import com.google.spanner.v1.DirectedReadOptions.ReplicaSelection;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.GetSessionRequest;
import com.google.spanner.v1.ListSessionsRequest;
import com.google.spanner.v1.PartitionQueryRequest;
import com.google.spanner.v1.PartitionReadRequest;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.SpannerGrpc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.threeten.bp.Duration;

/** Unit tests for {@link com.google.cloud.spanner.SpannerOptions}. */
@RunWith(JUnit4.class)
public class SpannerOptionsTest {
  private static Level originalLogLevel;

  @BeforeClass
  public static void disableLogging() {
    Logger logger = Logger.getLogger("");
    originalLogLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
  }

  @AfterClass
  public static void resetLogging() {
    Logger logger = Logger.getLogger("");
    logger.setLevel(originalLogLevel);
  }

  @Test
  public void defaultBuilder() {
    // We need to set the project id since in test environment we cannot obtain a default project
    // id.
    SpannerOptions options = SpannerOptions.newBuilder().setProjectId("test-project").build();
    if (Strings.isNullOrEmpty(System.getenv("SPANNER_EMULATOR_HOST"))) {
      assertThat(options.getHost()).isEqualTo("https://spanner.googleapis.com");
    } else {
      assertThat(options.getHost()).isEqualTo("http://" + System.getenv("SPANNER_EMULATOR_HOST"));
    }
    assertThat(options.getPrefetchChunks()).isEqualTo(4);
    assertNull(options.getSessionLabels());
  }

  @Test
  public void builder() {
    String host = "http://localhost:8000/";
    String projectId = "test-project";
    Map<String, String> labels = new HashMap<>();
    labels.put("env", "dev");
    InMemoryMetricReader inMemoryMetricReader = InMemoryMetricReader.create();
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder().registerMetricReader(inMemoryMetricReader).build();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setHost(host)
            .setProjectId(projectId)
            .setPrefetchChunks(2)
            .setSessionLabels(labels)
            .setOpenTelemetry(openTelemetry)
            .build();
    assertThat(options.getHost()).isEqualTo(host);
    assertThat(options.getProjectId()).isEqualTo(projectId);
    assertThat(options.getPrefetchChunks()).isEqualTo(2);
    assertThat(options.getSessionLabels()).containsExactlyEntriesIn(labels);
    assertThat(options.getOpenTelemetry()).isEqualTo(openTelemetry);
  }

  @Test
  public void testSpannerDefaultRetrySettings() {
    RetrySettings witRetryPolicy1 =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(250L))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(32000L))
            .setInitialRpcTimeout(Duration.ofMillis(3600000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(3600000L))
            .setTotalTimeout(Duration.ofMillis(3600000L))
            .build();
    RetrySettings witRetryPolicy2 =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(250L))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(32000L))
            .setInitialRpcTimeout(Duration.ofMillis(60000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(60000L))
            .setTotalTimeout(Duration.ofMillis(60000L))
            .build();
    RetrySettings witRetryPolicy3 =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(250L))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(32000L))
            .setInitialRpcTimeout(Duration.ofMillis(30000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(30000L))
            .setTotalTimeout(Duration.ofMillis(30000L))
            .build();
    RetrySettings noRetry1 =
        RetrySettings.newBuilder()
            .setInitialRpcTimeout(Duration.ofMillis(3600000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(3600000L))
            .setTotalTimeout(Duration.ofMillis(3600000L))
            .build();
    SpannerOptions options = SpannerOptions.newBuilder().setProjectId("test-project").build();
    SpannerStubSettings stubSettings = options.getSpannerStubSettings();
    List<? extends UnaryCallSettings<?, ?>> callsWithRetry1 =
        Arrays.asList(stubSettings.listSessionsSettings(), stubSettings.commitSettings());
    List<? extends UnaryCallSettings<?, ?>> callsWithRetry2 =
        Collections.singletonList(stubSettings.batchCreateSessionsSettings());
    List<? extends UnaryCallSettings<?, ?>> callsWithRetry3 =
        Arrays.asList(
            stubSettings.createSessionSettings(),
            stubSettings.getSessionSettings(),
            stubSettings.deleteSessionSettings(),
            stubSettings.executeSqlSettings(),
            stubSettings.executeBatchDmlSettings(),
            stubSettings.readSettings(),
            stubSettings.beginTransactionSettings(),
            stubSettings.rollbackSettings(),
            stubSettings.partitionQuerySettings(),
            stubSettings.partitionReadSettings());
    List<? extends ServerStreamingCallSettings<?, ?>> callsWithNoRetry1 =
        Arrays.asList(
            stubSettings.executeStreamingSqlSettings(), stubSettings.streamingReadSettings());

    for (UnaryCallSettings<?, ?> callSettings : callsWithRetry1) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(witRetryPolicy1);
    }
    for (UnaryCallSettings<?, ?> callSettings : callsWithRetry2) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(witRetryPolicy2);
    }
    for (UnaryCallSettings<?, ?> callSettings : callsWithRetry3) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(witRetryPolicy3);
    }
    for (ServerStreamingCallSettings<?, ?> callSettings : callsWithNoRetry1) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(noRetry1);
    }
  }

  @Test
  public void testSpannerCustomRetrySettings() {
    RetrySettings retrySettings =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofSeconds(9999L))
            .setRetryDelayMultiplier(9999.99D)
            .setMaxRetryDelay(Duration.ofSeconds(9999L))
            .setInitialRpcTimeout(Duration.ofSeconds(9999L))
            .setRpcTimeoutMultiplier(9999.99D)
            .setMaxRpcTimeout(Duration.ofSeconds(9999L))
            .setTotalTimeout(Duration.ofSeconds(9999L))
            .build();
    SpannerOptions.Builder builder = SpannerOptions.newBuilder().setProjectId("test-project");
    SpannerStubSettings.Builder stubSettingsBuilder = builder.getSpannerStubSettingsBuilder();
    List<? extends UnaryCallSettings.Builder<?, ?>> unaryCallSettingsBuilders =
        Arrays.asList(
            stubSettingsBuilder.beginTransactionSettings(),
            stubSettingsBuilder.createSessionSettings(),
            stubSettingsBuilder.deleteSessionSettings(),
            stubSettingsBuilder.executeBatchDmlSettings(),
            stubSettingsBuilder.executeSqlSettings(),
            stubSettingsBuilder.getSessionSettings(),
            stubSettingsBuilder.listSessionsSettings(),
            stubSettingsBuilder.partitionQuerySettings(),
            stubSettingsBuilder.partitionReadSettings(),
            stubSettingsBuilder.readSettings(),
            stubSettingsBuilder.rollbackSettings(),
            stubSettingsBuilder.commitSettings());
    for (UnaryCallSettings.Builder<?, ?> callSettingsBuilder : unaryCallSettingsBuilders) {
      callSettingsBuilder.setRetrySettings(retrySettings);
    }
    List<? extends ServerStreamingCallSettings.Builder<?, ?>> streamingCallSettingsBuilders =
        Arrays.asList(
            stubSettingsBuilder.executeStreamingSqlSettings(),
            stubSettingsBuilder.streamingReadSettings());
    for (ServerStreamingCallSettings.Builder<?, ?> callSettingsBuilder :
        streamingCallSettingsBuilders) {
      callSettingsBuilder.setRetrySettings(retrySettings);
    }

    SpannerOptions options = builder.build();
    SpannerStubSettings stubSettings = options.getSpannerStubSettings();
    List<? extends UnaryCallSettings<?, ?>> callsWithDefaultSettings =
        Arrays.asList(
            stubSettings.beginTransactionSettings(),
            stubSettings.createSessionSettings(),
            stubSettings.deleteSessionSettings(),
            stubSettings.executeBatchDmlSettings(),
            stubSettings.executeSqlSettings(),
            stubSettings.getSessionSettings(),
            stubSettings.listSessionsSettings(),
            stubSettings.partitionQuerySettings(),
            stubSettings.partitionReadSettings(),
            stubSettings.readSettings(),
            stubSettings.rollbackSettings(),
            stubSettings.commitSettings());
    List<? extends ServerStreamingCallSettings<?, ?>> callsWithStreamingSettings =
        Arrays.asList(
            stubSettings.executeStreamingSqlSettings(), stubSettings.streamingReadSettings());

    for (UnaryCallSettings<?, ?> callSettings : callsWithDefaultSettings) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(retrySettings);
    }
    for (ServerStreamingCallSettings<?, ?> callSettings : callsWithStreamingSettings) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(retrySettings);
    }
  }

  @Test
  public void testDatabaseAdminDefaultRetrySettings() {
    RetrySettings withRetryPolicy1 =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(1000L))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(32000L))
            .setInitialRpcTimeout(Duration.ofMillis(3600000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(3600000L))
            .setTotalTimeout(Duration.ofMillis(3600000L))
            .build();
    RetrySettings withRetryPolicy2 =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(1000L))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(32000L))
            .setInitialRpcTimeout(Duration.ofMillis(30000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(30000L))
            .setTotalTimeout(Duration.ofMillis(30000L))
            .build();
    RetrySettings noRetryPolicy2 =
        RetrySettings.newBuilder()
            .setInitialRpcTimeout(Duration.ofMillis(30000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(30000L))
            .setTotalTimeout(Duration.ofMillis(30000L))
            .build();
    SpannerOptions options = SpannerOptions.newBuilder().setProjectId("test-project").build();
    DatabaseAdminStubSettings stubSettings = options.getDatabaseAdminStubSettings();
    List<? extends UnaryCallSettings<?, ?>> callsWithRetryPolicy1 =
        Arrays.asList(
            stubSettings.dropDatabaseSettings(),
            stubSettings.getDatabaseSettings(),
            stubSettings.getDatabaseDdlSettings());
    List<? extends UnaryCallSettings<?, ?>> callsWithRetryPolicy2 =
        Collections.singletonList(stubSettings.getIamPolicySettings());
    List<? extends UnaryCallSettings<?, ?>> callsWithNoRetry2 =
        Arrays.asList(
            stubSettings.setIamPolicySettings(), stubSettings.testIamPermissionsSettings());

    for (UnaryCallSettings<?, ?> callSettings : callsWithRetryPolicy1) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(withRetryPolicy1);
    }
    for (UnaryCallSettings<?, ?> callSettings : callsWithRetryPolicy2) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(withRetryPolicy2);
    }
    for (UnaryCallSettings<?, ?> callSettings : callsWithNoRetry2) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(noRetryPolicy2);
    }
  }

  @Test
  public void testDatabaseAdminCustomRetrySettings() {
    RetrySettings retrySettings =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofSeconds(9999L))
            .setRetryDelayMultiplier(9999.99D)
            .setMaxRetryDelay(Duration.ofSeconds(9999L))
            .setInitialRpcTimeout(Duration.ofSeconds(9999L))
            .setRpcTimeoutMultiplier(9999.99D)
            .setMaxRpcTimeout(Duration.ofSeconds(9999L))
            .setTotalTimeout(Duration.ofSeconds(9999L))
            .build();
    SpannerOptions.Builder builder = SpannerOptions.newBuilder().setProjectId("test-project");
    DatabaseAdminStubSettings.Builder stubSettingsBuilder =
        builder.getDatabaseAdminStubSettingsBuilder();
    List<? extends UnaryCallSettings.Builder<?, ?>> unaryCallSettingsBuilders =
        Arrays.asList(
            stubSettingsBuilder.dropDatabaseSettings(),
            stubSettingsBuilder.getDatabaseDdlSettings(),
            stubSettingsBuilder.getDatabaseSettings());
    for (UnaryCallSettings.Builder<?, ?> callSettingsBuilder : unaryCallSettingsBuilders) {
      callSettingsBuilder.setRetrySettings(retrySettings);
    }

    SpannerOptions options = builder.build();
    DatabaseAdminStubSettings stubSettings = options.getDatabaseAdminStubSettings();
    List<? extends UnaryCallSettings<?, ?>> callsWithDefaultSettings =
        Arrays.asList(
            stubSettings.dropDatabaseSettings(),
            stubSettings.getDatabaseDdlSettings(),
            stubSettings.getDatabaseSettings());

    for (UnaryCallSettings<?, ?> callSettings : callsWithDefaultSettings) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(retrySettings);
    }
  }

  @Test
  public void testInstanceAdminDefaultRetrySettings() {
    RetrySettings withRetryPolicy1 =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(1000L))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(32000L))
            .setInitialRpcTimeout(Duration.ofMillis(3600000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(3600000L))
            .setTotalTimeout(Duration.ofMillis(3600000L))
            .build();
    RetrySettings withRetryPolicy2 =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(1000L))
            .setRetryDelayMultiplier(1.3)
            .setMaxRetryDelay(Duration.ofMillis(32000L))
            .setInitialRpcTimeout(Duration.ofMillis(30000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(30000L))
            .setTotalTimeout(Duration.ofMillis(30000L))
            .build();
    RetrySettings noRetryPolicy1 =
        RetrySettings.newBuilder()
            .setInitialRpcTimeout(Duration.ofMillis(3600000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(3600000L))
            .setTotalTimeout(Duration.ofMillis(3600000L))
            .build();
    RetrySettings noRetryPolicy2 =
        RetrySettings.newBuilder()
            .setInitialRpcTimeout(Duration.ofMillis(30000L))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofMillis(30000L))
            .setTotalTimeout(Duration.ofMillis(30000L))
            .build();
    SpannerOptions options = SpannerOptions.newBuilder().setProjectId("test-project").build();
    InstanceAdminStubSettings stubSettings = options.getInstanceAdminStubSettings();
    List<? extends UnaryCallSettings<?, ?>> callsWithRetryPolicy1 =
        Arrays.asList(
            stubSettings.getInstanceConfigSettings(),
            stubSettings.listInstanceConfigsSettings(),
            stubSettings.deleteInstanceSettings(),
            stubSettings.getInstanceSettings(),
            stubSettings.listInstancesSettings());
    List<? extends UnaryCallSettings<?, ?>> callsWithRetryPolicy2 =
        Collections.singletonList(stubSettings.getIamPolicySettings());
    List<? extends UnaryCallSettings<?, ?>> callsWithNoRetryPolicy1 =
        Arrays.asList(stubSettings.createInstanceSettings(), stubSettings.updateInstanceSettings());
    List<? extends UnaryCallSettings<?, ?>> callsWithNoRetryPolicy2 =
        Arrays.asList(
            stubSettings.setIamPolicySettings(), stubSettings.testIamPermissionsSettings());

    for (UnaryCallSettings<?, ?> callSettings : callsWithRetryPolicy1) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(withRetryPolicy1);
    }
    for (UnaryCallSettings<?, ?> callSettings : callsWithRetryPolicy2) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(withRetryPolicy2);
    }
    for (UnaryCallSettings<?, ?> callSettings : callsWithNoRetryPolicy1) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(noRetryPolicy1);
    }
    for (UnaryCallSettings<?, ?> callSettings : callsWithNoRetryPolicy2) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(noRetryPolicy2);
    }
  }

  @Test
  public void testInstanceAdminCustomRetrySettings() {
    RetrySettings retrySettings =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofSeconds(9999L))
            .setRetryDelayMultiplier(9999.99D)
            .setMaxRetryDelay(Duration.ofSeconds(9999L))
            .setInitialRpcTimeout(Duration.ofSeconds(9999L))
            .setRpcTimeoutMultiplier(9999.99D)
            .setMaxRpcTimeout(Duration.ofSeconds(9999L))
            .setTotalTimeout(Duration.ofSeconds(9999L))
            .build();
    SpannerOptions.Builder builder = SpannerOptions.newBuilder().setProjectId("test-project");
    InstanceAdminStubSettings.Builder stubSettingsBuilder =
        builder.getInstanceAdminStubSettingsBuilder();
    List<? extends UnaryCallSettings.Builder<?, ?>> unaryCallSettingsBuilders =
        Arrays.asList(
            stubSettingsBuilder.deleteInstanceSettings(),
            stubSettingsBuilder.getInstanceConfigSettings(),
            stubSettingsBuilder.getInstanceSettings(),
            stubSettingsBuilder.listInstanceConfigsSettings(),
            stubSettingsBuilder.listInstancesSettings());
    for (UnaryCallSettings.Builder<?, ?> callSettingsBuilder : unaryCallSettingsBuilders) {
      callSettingsBuilder.setRetrySettings(retrySettings);
    }

    SpannerOptions options = builder.build();
    InstanceAdminStubSettings stubSettings = options.getInstanceAdminStubSettings();
    List<? extends UnaryCallSettings<?, ?>> callsWithDefaultSettings =
        Arrays.asList(
            stubSettings.getInstanceConfigSettings(),
            stubSettings.listInstanceConfigsSettings(),
            stubSettings.deleteInstanceSettings(),
            stubSettings.getInstanceSettings(),
            stubSettings.listInstancesSettings());

    for (UnaryCallSettings<?, ?> callSettings : callsWithDefaultSettings) {
      assertThat(callSettings.getRetrySettings()).isEqualTo(retrySettings);
    }
  }

  @Test
  public void testInvalidTransport() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                SpannerOptions.newBuilder()
                    .setTransportOptions(Mockito.mock(TransportOptions.class)));
    assertNotNull(e.getMessage());
  }

  @Test
  public void testInvalidSessionLabels() {
    Map<String, String> labels = new HashMap<>();
    labels.put("env", null);
    NullPointerException e =
        assertThrows(
            NullPointerException.class, () -> SpannerOptions.newBuilder().setSessionLabels(labels));
    assertNotNull(e.getMessage());
  }

  @Test
  public void testNullSessionLabels() {
    NullPointerException e =
        assertThrows(
            NullPointerException.class, () -> SpannerOptions.newBuilder().setSessionLabels(null));
    assertNotNull(e.getMessage());
  }

  @Test
  public void testDoNotCacheClosedSpannerInstance() {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setCredentials(NoCredentials.getInstance())
            .build();
    // Getting a service twice should give the same instance.
    Spanner service1 = options.getService();
    Spanner service2 = options.getService();
    assertThat(service1 == service2, is(true));
    assertThat(service1.isClosed()).isFalse();
    // Closing a service instance should cause the SpannerOptions to create a new service.
    service1.close();
    Spanner service3 = options.getService();
    assertThat(service3 == service1, is(false));
    assertThat(service1.isClosed()).isTrue();
    assertThat(service3.isClosed()).isFalse();

    // Getting another service from the SpannerOptions should return the new cached instance.
    Spanner service4 = options.getService();
    assertThat(service3 == service4, is(true));
    assertThat(service3.isClosed()).isFalse();
    service3.close();
  }

  @Test
  public void testSetClientLibToken() {
    final String jdbcToken = "sp-jdbc";
    final String hibernateToken = "sp-hib";
    final String pgAdapterToken = "pg-adapter";
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(NoCredentials.getInstance())
            .setClientLibToken(jdbcToken)
            .build();
    // Verify that the client lib token that will actually be used contains both the JDBC token and
    // the standard Java client library token ('gccl').
    assertEquals("sp-jdbc gccl", options.getClientLibToken());

    options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(NoCredentials.getInstance())
            .setClientLibToken(hibernateToken)
            .build();
    assertEquals("sp-hib gccl", options.getClientLibToken());

    options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(NoCredentials.getInstance())
            .setClientLibToken(pgAdapterToken)
            .build();
    assertEquals("pg-adapter gccl", options.getClientLibToken());

    options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(NoCredentials.getInstance())
            .build();
    assertEquals(options.getClientLibToken(), ServiceOptions.getGoogApiClientLibName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetInvalidClientLibToken() {
    SpannerOptions.newBuilder()
        .setProjectId("some-project")
        .setCredentials(NoCredentials.getInstance())
        .setClientLibToken("foo");
  }

  @Test
  public void testSetEmulatorHostWithoutProtocol() {
    // If the host doesn't have a protocol as a prefix, it will automatically be prefixed with
    // "http://".
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setEmulatorHost("localhost:1234")
            .build();
    assertThat(options.getHost()).isEqualTo("http://localhost:1234");
    assertThat(options.getEndpoint()).isEqualTo("localhost:1234");
  }

  @Test
  public void testSetEmulatorHostWithProtocol() {
    // If the host has a protocol, it should not be modified.
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setEmulatorHost("http://localhost:1234")
            .build();
    assertThat(options.getHost()).isEqualTo("http://localhost:1234");
    assertThat(options.getEndpoint()).isEqualTo("localhost:1234");
  }

  @Test
  public void testDefaultQueryOptions() {
    SpannerOptions.useEnvironment(
        new SpannerOptions.SpannerEnvironment() {
          @Override
          public String getOptimizerVersion() {
            return "";
          }

          @Nonnull
          @Override
          public String getOptimizerStatisticsPackage() {
            return "";
          }
        });
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setDefaultQueryOptions(
                DatabaseId.of("p", "i", "d"),
                QueryOptions.newBuilder()
                    .setOptimizerVersion("1")
                    .setOptimizerStatisticsPackage("custom-package")
                    .build())
            .setProjectId("p")
            .setCredentials(NoCredentials.getInstance())
            .build();
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "d")))
        .isEqualTo(
            QueryOptions.newBuilder()
                .setOptimizerVersion("1")
                .setOptimizerStatisticsPackage("custom-package")
                .build());
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "o")))
        .isEqualTo(QueryOptions.getDefaultInstance());

    // Now simulate that the user has set an environment variable for the query optimizer version
    // and statistics package.
    SpannerOptions.useEnvironment(
        new SpannerOptions.SpannerEnvironment() {
          @Override
          public String getOptimizerVersion() {
            return "2";
          }

          @Nonnull
          @Override
          public String getOptimizerStatisticsPackage() {
            return "env-package";
          }
        });
    // Create options with '1' as the default query optimizer version and 'custom-package' as the
    // default query optimizer statistics package. These values should be overridden by
    // the environment variable.
    options =
        SpannerOptions.newBuilder()
            .setDefaultQueryOptions(
                DatabaseId.of("p", "i", "d"),
                QueryOptions.newBuilder()
                    .setOptimizerVersion("1")
                    .setOptimizerStatisticsPackage("custom-package")
                    .build())
            .setProjectId("p")
            .setCredentials(NoCredentials.getInstance())
            .build();
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "d")))
        .isEqualTo(
            QueryOptions.newBuilder()
                .setOptimizerVersion("2")
                .setOptimizerStatisticsPackage("env-package")
                .build());
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "o")))
        .isEqualTo(
            QueryOptions.newBuilder()
                .setOptimizerVersion("2")
                .setOptimizerStatisticsPackage("env-package")
                .build());
  }

  @Test
  public void testCompressorName() {
    assertThat(
            SpannerOptions.newBuilder()
                .setProjectId("p")
                .setCompressorName("gzip")
                .build()
                .getCompressorName())
        .isEqualTo("gzip");
    assertThat(
            SpannerOptions.newBuilder()
                .setProjectId("p")
                .setCompressorName("identity")
                .build()
                .getCompressorName())
        .isEqualTo("identity");
    assertNull(
        SpannerOptions.newBuilder()
            .setProjectId("p")
            .setCompressorName(null)
            .build()
            .getCompressorName());
    assertThrows(
        IllegalArgumentException.class, () -> SpannerOptions.newBuilder().setCompressorName("foo"));
  }

  @Test
  public void testLeaderAwareRoutingEnablement() {
    assertTrue(SpannerOptions.newBuilder().setProjectId("p").build().isLeaderAwareRoutingEnabled());
    assertTrue(
        SpannerOptions.newBuilder()
            .setProjectId("p")
            .enableLeaderAwareRouting()
            .build()
            .isLeaderAwareRoutingEnabled());
    assertFalse(
        SpannerOptions.newBuilder()
            .setProjectId("p")
            .disableLeaderAwareRouting()
            .build()
            .isLeaderAwareRoutingEnabled());
  }

  @Test
  public void testSetDirectedReadOptions() {
    final DirectedReadOptions directedReadOptions =
        DirectedReadOptions.newBuilder()
            .setIncludeReplicas(
                IncludeReplicas.newBuilder()
                    .addReplicaSelections(
                        ReplicaSelection.newBuilder().setLocation("us-west1").build())
                    .build())
            .build();
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("[PROJECT]")
            .setDirectedReadOptions(directedReadOptions)
            .build();
    assertEquals(options.getDirectedReadOptions(), directedReadOptions);
    assertThrows(
        NullPointerException.class,
        () -> SpannerOptions.newBuilder().setDirectedReadOptions(null).build());
  }

  @Test
  public void testSpannerCallContextTimeoutConfigurator_NullValues() {
    SpannerCallContextTimeoutConfigurator configurator =
        SpannerCallContextTimeoutConfigurator.create();
    ApiCallContext inputCallContext = GrpcCallContext.createDefault();

    assertNull(
        configurator.configure(
            inputCallContext,
            BatchCreateSessionsRequest.getDefaultInstance(),
            SpannerGrpc.getBatchCreateSessionsMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            CreateSessionRequest.getDefaultInstance(),
            SpannerGrpc.getCreateSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            DeleteSessionRequest.getDefaultInstance(),
            SpannerGrpc.getDeleteSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            GetSessionRequest.getDefaultInstance(),
            SpannerGrpc.getGetSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            DeleteSessionRequest.getDefaultInstance(),
            SpannerGrpc.getDeleteSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            ListSessionsRequest.getDefaultInstance(),
            SpannerGrpc.getListSessionsMethod()));

    assertNull(
        configurator.configure(
            inputCallContext,
            BeginTransactionRequest.getDefaultInstance(),
            SpannerGrpc.getBeginTransactionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext, CommitRequest.getDefaultInstance(), SpannerGrpc.getCommitMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            RollbackRequest.getDefaultInstance(),
            SpannerGrpc.getRollbackMethod()));

    assertNull(
        configurator.configure(
            inputCallContext,
            ExecuteSqlRequest.getDefaultInstance(),
            SpannerGrpc.getExecuteSqlMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            ExecuteSqlRequest.getDefaultInstance(),
            SpannerGrpc.getExecuteStreamingSqlMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            ExecuteBatchDmlRequest.getDefaultInstance(),
            SpannerGrpc.getExecuteBatchDmlMethod()));
    assertNull(
        configurator.configure(
            inputCallContext, ReadRequest.getDefaultInstance(), SpannerGrpc.getReadMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            ReadRequest.getDefaultInstance(),
            SpannerGrpc.getStreamingReadMethod()));

    assertNull(
        configurator.configure(
            inputCallContext,
            PartitionQueryRequest.getDefaultInstance(),
            SpannerGrpc.getPartitionQueryMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            PartitionReadRequest.getDefaultInstance(),
            SpannerGrpc.getPartitionReadMethod()));
  }

  @Test
  public void testSpannerCallContextTimeoutConfigurator_WithTimeouts() {
    SpannerCallContextTimeoutConfigurator configurator =
        SpannerCallContextTimeoutConfigurator.create();
    configurator.withBatchUpdateTimeout(Duration.ofSeconds(1L));
    configurator.withCommitTimeout(Duration.ofSeconds(2L));
    configurator.withExecuteQueryTimeout(Duration.ofSeconds(3L));
    configurator.withExecuteUpdateTimeout(Duration.ofSeconds(4L));
    configurator.withPartitionQueryTimeout(Duration.ofSeconds(5L));
    configurator.withPartitionReadTimeout(Duration.ofSeconds(6L));
    configurator.withReadTimeout(Duration.ofSeconds(7L));
    configurator.withRollbackTimeout(Duration.ofSeconds(8L));

    ApiCallContext inputCallContext = GrpcCallContext.createDefault();

    assertNull(
        configurator.configure(
            inputCallContext,
            BatchCreateSessionsRequest.getDefaultInstance(),
            SpannerGrpc.getBatchCreateSessionsMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            CreateSessionRequest.getDefaultInstance(),
            SpannerGrpc.getCreateSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            DeleteSessionRequest.getDefaultInstance(),
            SpannerGrpc.getDeleteSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            GetSessionRequest.getDefaultInstance(),
            SpannerGrpc.getGetSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            DeleteSessionRequest.getDefaultInstance(),
            SpannerGrpc.getDeleteSessionMethod()));
    assertNull(
        configurator.configure(
            inputCallContext,
            ListSessionsRequest.getDefaultInstance(),
            SpannerGrpc.getListSessionsMethod()));

    assertNull(
        configurator.configure(
            inputCallContext,
            BeginTransactionRequest.getDefaultInstance(),
            SpannerGrpc.getBeginTransactionMethod()));
    assertThat(
            configurator
                .configure(
                    inputCallContext,
                    CommitRequest.getDefaultInstance(),
                    SpannerGrpc.getCommitMethod())
                .getTimeout())
        .isEqualTo(Duration.ofSeconds(2L));
    assertThat(
            configurator
                .configure(
                    inputCallContext,
                    RollbackRequest.getDefaultInstance(),
                    SpannerGrpc.getRollbackMethod())
                .getTimeout())
        .isEqualTo(Duration.ofSeconds(8L));

    assertNull(
        configurator.configure(
            inputCallContext,
            ExecuteSqlRequest.getDefaultInstance(),
            SpannerGrpc.getExecuteSqlMethod()));
    assertThat(
            configurator
                .configure(
                    inputCallContext,
                    ExecuteSqlRequest.getDefaultInstance(),
                    SpannerGrpc.getExecuteStreamingSqlMethod())
                .getTimeout())
        .isEqualTo(Duration.ofSeconds(3L));
    assertThat(
            configurator
                .configure(
                    inputCallContext,
                    ExecuteBatchDmlRequest.getDefaultInstance(),
                    SpannerGrpc.getExecuteBatchDmlMethod())
                .getTimeout())
        .isEqualTo(Duration.ofSeconds(1L));
    assertNull(
        configurator.configure(
            inputCallContext, ReadRequest.getDefaultInstance(), SpannerGrpc.getReadMethod()));
    assertThat(
            configurator
                .configure(
                    inputCallContext,
                    ReadRequest.getDefaultInstance(),
                    SpannerGrpc.getStreamingReadMethod())
                .getTimeout())
        .isEqualTo(Duration.ofSeconds(7L));

    assertThat(
            configurator
                .configure(
                    inputCallContext,
                    PartitionQueryRequest.getDefaultInstance(),
                    SpannerGrpc.getPartitionQueryMethod())
                .getTimeout())
        .isEqualTo(Duration.ofSeconds(5L));
    assertThat(
            configurator
                .configure(
                    inputCallContext,
                    PartitionReadRequest.getDefaultInstance(),
                    SpannerGrpc.getPartitionReadMethod())
                .getTimeout())
        .isEqualTo(Duration.ofSeconds(6L));
  }

  @Test
  public void testCustomAsyncExecutorProvider() {
    ScheduledExecutorService service = mock(ScheduledExecutorService.class);
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .setAsyncExecutorProvider(FixedCloseableExecutorProvider.create(service))
            .build();
    assertSame(service, options.getAsyncExecutorProvider().getExecutor());
  }

  @Test
  public void testAsyncExecutorProviderCoreThreadCount() throws Exception {
    assertEquals(8, SpannerOptions.getDefaultAsyncExecutorProviderCoreThreadCount());
    String propertyName = "com.google.cloud.spanner.async_num_core_threads";
    assertEquals(
        Integer.valueOf(8),
        runWithSystemProperty(
            propertyName, null, SpannerOptions::getDefaultAsyncExecutorProviderCoreThreadCount));
    assertEquals(
        Integer.valueOf(16),
        runWithSystemProperty(
            propertyName, "16", SpannerOptions::getDefaultAsyncExecutorProviderCoreThreadCount));
    assertEquals(
        Integer.valueOf(1),
        runWithSystemProperty(
            propertyName, "1", SpannerOptions::getDefaultAsyncExecutorProviderCoreThreadCount));
    assertThrows(
        SpannerException.class,
        () ->
            runWithSystemProperty(
                propertyName,
                "foo",
                SpannerOptions::getDefaultAsyncExecutorProviderCoreThreadCount));
    assertThrows(
        SpannerException.class,
        () ->
            runWithSystemProperty(
                propertyName,
                "-1",
                SpannerOptions::getDefaultAsyncExecutorProviderCoreThreadCount));
    assertThrows(
        SpannerException.class,
        () ->
            runWithSystemProperty(
                propertyName, "", SpannerOptions::getDefaultAsyncExecutorProviderCoreThreadCount));
  }

  static <V> V runWithSystemProperty(
      String propertyName, String propertyValue, Callable<V> callable) throws Exception {
    String currentValue = System.getProperty(propertyName);
    if (propertyValue == null) {
      System.clearProperty(propertyName);
    } else {
      System.setProperty(propertyName, propertyValue);
    }
    try {
      return callable.call();
    } finally {
      if (currentValue == null) {
        System.clearProperty(propertyName);
      } else {
        System.setProperty(propertyName, currentValue);
      }
    }
  }

  @Test
  public void testDefaultNumChannelsWithGrpcGcpExtensionEnabled() {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .enableGrpcGcpExtension()
            .build();

    assertEquals(SpannerOptions.GRPC_GCP_ENABLED_DEFAULT_CHANNELS, options.getNumChannels());
  }

  @Test
  public void testDefaultNumChannelsWithGrpcGcpExtensionDisabled() {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .build();

    assertEquals(SpannerOptions.DEFAULT_CHANNELS, options.getNumChannels());
  }

  @Test
  public void testNumChannelsWithGrpcGcpExtensionEnabled() {
    // Set number of channels explicitly, before enabling gRPC-GCP channel pool in SpannerOptions
    // builder.
    int numChannels = 5;
    SpannerOptions options1 =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .setNumChannels(numChannels)
            .enableGrpcGcpExtension()
            .build();

    assertEquals(numChannels, options1.getNumChannels());

    // Set number of channels explicitly, after enabling gRPC-GCP channel pool in SpannerOptions
    // builder.
    SpannerOptions options2 =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .enableGrpcGcpExtension()
            .setNumChannels(numChannels)
            .build();

    assertEquals(numChannels, options2.getNumChannels());
  }

  @Test
  public void checkCreatedInstanceWhenGrpcGcpExtensionDisabled() {
    SpannerOptions options = SpannerOptions.newBuilder().setProjectId("test-project").build();
    SpannerOptions options1 = options.toBuilder().build();
    assertEquals(false, options.isGrpcGcpExtensionEnabled());
    assertEquals(options.isGrpcGcpExtensionEnabled(), options1.isGrpcGcpExtensionEnabled());

    Spanner spanner1 = options.getService();
    Spanner spanner2 = options1.getService();

    assertNotSame(spanner1, spanner2);

    spanner1.close();
    spanner2.close();
  }

  @Test
  public void checkCreatedInstanceWhenGrpcGcpExtensionEnabled() {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .enableGrpcGcpExtension()
            .build();
    SpannerOptions options1 = options.toBuilder().build();
    assertEquals(true, options.isGrpcGcpExtensionEnabled());
    assertEquals(options.isGrpcGcpExtensionEnabled(), options1.isGrpcGcpExtensionEnabled());

    Spanner spanner1 = options.getService();
    Spanner spanner2 = options1.getService();

    assertNotSame(spanner1, spanner2);

    spanner1.close();
    spanner2.close();
  }

  @Test
  public void checkGlobalOpenTelemetryWhenNotInjected() {
    GlobalOpenTelemetry.resetForTest();
    InMemoryMetricReader inMemoryMetricReader = InMemoryMetricReader.create();
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder().registerMetricReader(inMemoryMetricReader).build();
    OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).buildAndRegisterGlobal();
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .build();
    assertEquals(GlobalOpenTelemetry.get(), options.getOpenTelemetry());
  }
}
