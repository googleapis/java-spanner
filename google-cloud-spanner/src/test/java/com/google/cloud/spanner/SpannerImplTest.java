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
import static org.mockito.Mockito.when;

import com.google.api.core.NanoClock;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceRpc;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.spanner.SpannerException.DoNotConstructDirectly;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** Unit tests for {@link SpannerImpl}. */
@RunWith(JUnit4.class)
public class SpannerImplTest {
  @Mock private SpannerRpc rpc;
  @Mock private SpannerOptions spannerOptions;
  private SpannerImpl impl;

  @Captor ArgumentCaptor<Map<SpannerRpc.Option, Object>> options;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(spannerOptions.getNumChannels()).thenReturn(4);
    when(spannerOptions.getPrefetchChunks()).thenReturn(1);
    when(spannerOptions.getRetrySettings()).thenReturn(RetrySettings.newBuilder().build());
    when(spannerOptions.getClock()).thenReturn(NanoClock.getDefaultClock());
    when(spannerOptions.getSessionLabels()).thenReturn(Collections.<String, String>emptyMap());
    impl = new SpannerImpl(rpc, spannerOptions);
  }

  @After
  public void teardown() {
    impl.close();
  }

  @Test
  public void getDbclientAgainGivesSame() {
    Map<String, String> labels = new HashMap<>();
    labels.put("env", "dev");
    Mockito.when(spannerOptions.getSessionLabels()).thenReturn(labels);
    String dbName = "projects/p1/instances/i1/databases/d1";
    DatabaseId db = DatabaseId.of(dbName);

    Mockito.when(spannerOptions.getTransportOptions())
        .thenReturn(GrpcTransportOptions.newBuilder().build());
    Mockito.when(spannerOptions.getSessionPoolOptions())
        .thenReturn(SessionPoolOptions.newBuilder().setMinSessions(0).build());

    DatabaseClient databaseClient = impl.getDatabaseClient(db);

    // Get db client again
    DatabaseClient databaseClient1 = impl.getDatabaseClient(db);

    assertThat(databaseClient1).isSameInstanceAs(databaseClient);
  }

  @Test
  public void queryOptions() {
    QueryOptions queryOptions = QueryOptions.newBuilder().setOptimizerVersion("2").build();
    QueryOptions defaultOptions = QueryOptions.getDefaultInstance();
    DatabaseId db = DatabaseId.of("p", "i", "d");
    DatabaseId otherDb = DatabaseId.of("p", "i", "other");

    // Create a SpannerOptions with and without default query options.
    SpannerOptions optionsWithQueryOptions =
        new SpannerOptions.Builder(
            SpannerOptions.newBuilder()
                .setProjectId("some-project")
                .setCredentials(NoCredentials.getInstance())
                .build()) {
          @Override
          QueryOptions getEnvironmentQueryOptions() {
            // Override and return default instance to prevent environment variables from
            // interfering with the test case.
            return QueryOptions.getDefaultInstance();
          }
        }.setDefaultQueryOptions(db, queryOptions).build();
    SpannerOptions optionsWithoutQueryOptions =
        new SpannerOptions.Builder(
            SpannerOptions.newBuilder()
                .setProjectId("some-project")
                .setCredentials(NoCredentials.getInstance())
                .build()) {
          @Override
          QueryOptions getEnvironmentQueryOptions() {
            // Override and return default instance to prevent environment variables from
            // interfering with the test case.
            return QueryOptions.getDefaultInstance();
          }
        }.build();

    try (SpannerImpl implWithQueryOptions = new SpannerImpl(rpc, optionsWithQueryOptions);
        SpannerImpl implWithoutQueryOptions = new SpannerImpl(rpc, optionsWithoutQueryOptions)) {

      // Default query options are on a per-database basis, so we should only get the custom options
      // for 'db' and not for 'otherDb'.
      assertThat(implWithQueryOptions.getDefaultQueryOptions(db)).isEqualTo(queryOptions);
      assertThat(implWithQueryOptions.getDefaultQueryOptions(otherDb)).isEqualTo(defaultOptions);

      // The other Spanner instance should return default options for both databases.
      assertThat(implWithoutQueryOptions.getDefaultQueryOptions(db)).isEqualTo(defaultOptions);
      assertThat(implWithoutQueryOptions.getDefaultQueryOptions(otherDb)).isEqualTo(defaultOptions);
    }
  }

  @Test
  public void getDbclientAfterCloseThrows() {
    SpannerImpl imp = new SpannerImpl(rpc, spannerOptions);
    Map<String, String> labels = new HashMap<>();
    labels.put("env", "dev");
    Mockito.when(spannerOptions.getSessionLabels()).thenReturn(labels);
    String dbName = "projects/p1/instances/i1/databases/d1";
    DatabaseId db = DatabaseId.of(dbName);

    Mockito.when(spannerOptions.getTransportOptions())
        .thenReturn(GrpcTransportOptions.newBuilder().build());
    Mockito.when(spannerOptions.getSessionPoolOptions())
        .thenReturn(SessionPoolOptions.newBuilder().build());

    imp.close();

    try {
      imp.getDatabaseClient(db);
      fail("Expected exception");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("Cloud Spanner client has been closed");
    }
  }

  @Test
  public void testSpannerClosed() {
    SpannerOptions options = createSpannerOptions();
    Spanner spanner1 = options.getService();
    Spanner spanner2 = options.getService();
    ServiceRpc rpc1 = options.getRpc();
    ServiceRpc rpc2 = options.getRpc();
    // The SpannerOptions object should return the same instance.
    assertThat(spanner1 == spanner2, is(true));
    assertThat(rpc1 == rpc2, is(true));
    spanner1.close();
    // A new instance should be returned as the Spanner instance has been closed.
    Spanner spanner3 = options.getService();
    assertThat(spanner1 == spanner3, is(false));
    // A new instance should be returned as the Spanner instance has been closed.
    ServiceRpc rpc3 = options.getRpc();
    assertThat(rpc1 == rpc3, is(false));
    // Creating a copy of the SpannerOptions should result in new instances.
    options = options.toBuilder().build();
    Spanner spanner4 = options.getService();
    ServiceRpc rpc4 = options.getRpc();
    assertThat(spanner4 == spanner3, is(false));
    assertThat(rpc4 == rpc3, is(false));
    Spanner spanner5 = options.getService();
    ServiceRpc rpc5 = options.getRpc();
    assertThat(spanner4 == spanner5, is(true));
    assertThat(rpc4 == rpc5, is(true));
    spanner3.close();
    spanner4.close();
  }

  @Test
  public void testClientId() {
    // Create a unique database id to be sure it has not yet been used in the lifetime of this JVM.
    String dbName =
        String.format("projects/p1/instances/i1/databases/%s", UUID.randomUUID().toString());
    DatabaseId db = DatabaseId.of(dbName);

    Mockito.when(spannerOptions.getTransportOptions())
        .thenReturn(GrpcTransportOptions.newBuilder().build());
    Mockito.when(spannerOptions.getSessionPoolOptions())
        .thenReturn(SessionPoolOptions.newBuilder().setMinSessions(0).build());

    DatabaseClientImpl databaseClient = (DatabaseClientImpl) impl.getDatabaseClient(db);
    assertThat(databaseClient.clientId).isEqualTo("client-1");

    // Get same db client again.
    DatabaseClientImpl databaseClient1 = (DatabaseClientImpl) impl.getDatabaseClient(db);
    assertThat(databaseClient1.clientId).isEqualTo(databaseClient.clientId);

    // Get a db client for a different database.
    String dbName2 =
        String.format("projects/p1/instances/i1/databases/%s", UUID.randomUUID().toString());
    DatabaseId db2 = DatabaseId.of(dbName2);
    DatabaseClientImpl databaseClient2 = (DatabaseClientImpl) impl.getDatabaseClient(db2);
    assertThat(databaseClient2.clientId).isEqualTo("client-1");

    // Getting a new database client for an invalidated database should use the same client id.
    databaseClient.pool.setResourceNotFoundException(
        new DatabaseNotFoundException(DoNotConstructDirectly.ALLOWED, "not found", null, null));
    DatabaseClientImpl revalidated = (DatabaseClientImpl) impl.getDatabaseClient(db);
    assertThat(revalidated).isNotSameInstanceAs(databaseClient);
    assertThat(revalidated.clientId).isEqualTo(databaseClient.clientId);

    // Now invalidate the second client and request a new one.
    revalidated.pool.setResourceNotFoundException(
        new DatabaseNotFoundException(DoNotConstructDirectly.ALLOWED, "not found", null, null));
    DatabaseClientImpl revalidated2 = (DatabaseClientImpl) impl.getDatabaseClient(db);
    assertThat(revalidated2).isNotSameInstanceAs(revalidated);
    assertThat(revalidated2.clientId).isEqualTo(revalidated.clientId);

    // Create a new Spanner instance. This will generate new database clients with new ids.
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId("p1")
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()) {

      // Get a database client for the same database as the first database. As this goes through a
      // different Spanner instance with potentially different options, it will get a different
      // client id.
      DatabaseClientImpl databaseClient3 = (DatabaseClientImpl) spanner.getDatabaseClient(db);
      assertThat(databaseClient3.clientId).isEqualTo("client-2");
    }
  }

  @Test
  public void testClosedException() {
    Spanner spanner = new SpannerImpl(rpc, spannerOptions);
    assertThat(spanner.isClosed()).isFalse();
    // Close the Spanner instance in a different method so we can actually verify that the entire
    // stacktrace of the method that closed the instance is included in the exception that will be
    // thrown by the instance after it has been closed.
    closeSpannerAndIncludeStacktrace(spanner);
    assertThat(spanner.isClosed()).isTrue();
    try {
      spanner.getDatabaseClient(DatabaseId.of("p", "i", "d"));
      fail("missing expected exception");
    } catch (IllegalStateException e) {
      assertThat(e.getCause()).isInstanceOf(ClosedException.class);
      StringWriter sw = new StringWriter();
      e.getCause().printStackTrace(new PrintWriter(sw));
      assertThat(sw.toString()).contains("closeSpannerAndIncludeStacktrace");
    }
  }

  private void closeSpannerAndIncludeStacktrace(Spanner spanner) {
    spanner.close();
  }

  private SpannerOptions createSpannerOptions() {
    return SpannerOptions.newBuilder()
        .setProjectId("[PROJECT]")
        .setCredentials(NoCredentials.getInstance())
        .build();
  }
}
