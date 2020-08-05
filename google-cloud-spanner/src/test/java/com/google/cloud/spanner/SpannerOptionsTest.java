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
import static org.junit.Assert.fail;

import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ServerStreamingCallSettings;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.TransportOptions;
import com.google.cloud.spanner.admin.database.v1.stub.DatabaseAdminStubSettings;
import com.google.cloud.spanner.admin.instance.v1.stub.InstanceAdminStubSettings;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.base.Strings;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.threeten.bp.Duration;

/** Unit tests for {@link com.google.cloud.spanner.SpannerOptions}. */
@RunWith(JUnit4.class)
public class SpannerOptionsTest {

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
    assertThat(options.getSessionLabels()).isNull();
  }

  @Test
  public void builder() {
    String host = "http://localhost:8000/";
    String projectId = "test-project";
    Map<String, String> labels = new HashMap<>();
    labels.put("env", "dev");
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setHost(host)
            .setProjectId(projectId)
            .setPrefetchChunks(2)
            .setSessionLabels(labels)
            .build();
    assertThat(options.getHost()).isEqualTo(host);
    assertThat(options.getProjectId()).isEqualTo(projectId);
    assertThat(options.getPrefetchChunks()).isEqualTo(2);
    assertThat(options.getSessionLabels()).containsExactlyEntriesIn(labels);
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
        Arrays.asList(stubSettings.batchCreateSessionsSettings());
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
        Arrays.asList(stubSettings.getIamPolicySettings());
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
        Arrays.asList(stubSettings.getIamPolicySettings());
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
    try {
      SpannerOptions.newBuilder().setTransportOptions(Mockito.mock(TransportOptions.class));
      fail("Expected exception");
    } catch (IllegalArgumentException ex) {
      assertThat(ex.getMessage()).isNotNull();
    }
  }

  @Test
  public void testInvalidSessionLabels() {
    Map<String, String> labels = new HashMap<>();
    labels.put("env", null);
    try {
      SpannerOptions.newBuilder().setSessionLabels(labels);
      fail("Expected exception");
    } catch (NullPointerException ex) {
      assertThat(ex.getMessage()).isNotNull();
    }
  }

  @Test
  public void testNullSessionLabels() {
    try {
      SpannerOptions.newBuilder().setSessionLabels(null);
      fail("Expected exception");
    } catch (NullPointerException ex) {
      assertThat(ex.getMessage()).isNotNull();
    }
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
    ;
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
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(NoCredentials.getInstance())
            .setClientLibToken(jdbcToken)
            .build();
    assertThat(options.getClientLibToken()).isEqualTo(jdbcToken);

    options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(NoCredentials.getInstance())
            .setClientLibToken(hibernateToken)
            .build();
    assertThat(options.getClientLibToken()).isEqualTo(hibernateToken);

    options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(NoCredentials.getInstance())
            .build();
    assertThat(options.getClientLibToken()).isEqualTo(ServiceOptions.getGoogApiClientLibName());
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
        });
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setDefaultQueryOptions(
                DatabaseId.of("p", "i", "d"),
                QueryOptions.newBuilder().setOptimizerVersion("1").build())
            .setProjectId("p")
            .setCredentials(NoCredentials.getInstance())
            .build();
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "d")))
        .isEqualTo(QueryOptions.newBuilder().setOptimizerVersion("1").build());
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "o")))
        .isEqualTo(QueryOptions.getDefaultInstance());

    // Now simulate that the user has set an environment variable for the query optimizer version.
    SpannerOptions.useEnvironment(
        new SpannerOptions.SpannerEnvironment() {
          @Override
          public String getOptimizerVersion() {
            return "2";
          }
        });
    // Create options with '1' as the default query optimizer version. This should be overridden by
    // the environment variable.
    options =
        SpannerOptions.newBuilder()
            .setDefaultQueryOptions(
                DatabaseId.of("p", "i", "d"),
                QueryOptions.newBuilder().setOptimizerVersion("1").build())
            .setProjectId("p")
            .setCredentials(NoCredentials.getInstance())
            .build();
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "d")))
        .isEqualTo(QueryOptions.newBuilder().setOptimizerVersion("2").build());
    assertThat(options.getDefaultQueryOptions(DatabaseId.of("p", "i", "o")))
        .isEqualTo(QueryOptions.newBuilder().setOptimizerVersion("2").build());
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
    assertThat(
            SpannerOptions.newBuilder()
                .setProjectId("p")
                .setCompressorName(null)
                .build()
                .getCompressorName())
        .isNull();
    try {
      SpannerOptions.newBuilder().setCompressorName("foo");
      fail("missing expected exception");
    } catch (IllegalArgumentException e) {
      // ignore, this is the expected exception.
    }
  }
}
