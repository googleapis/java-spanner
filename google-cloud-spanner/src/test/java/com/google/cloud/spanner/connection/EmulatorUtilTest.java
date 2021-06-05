/*
 * Copyright 2021 Google LLC
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

package com.google.cloud.spanner.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Matchers;

@RunWith(JUnit4.class)
public class EmulatorUtilTest {

  @Test
  public void testCreateInstanceAndDatabase_bothSucceed()
      throws InterruptedException, ExecutionException {
    Spanner spanner = mock(Spanner.class);
    SpannerOptions options = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(options);
    when(options.getCredentials()).thenReturn(NoCredentials.getInstance());

    InstanceAdminClient instanceClient = mock(InstanceAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Instance, CreateInstanceMetadata> instanceOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getInstanceAdminClient()).thenReturn(instanceClient);
    when(instanceClient.createInstance(any(InstanceInfo.class)))
        .thenReturn(instanceOperationFuture);
    when(instanceOperationFuture.get()).thenReturn(mock(Instance.class));

    DatabaseAdminClient databaseClient = mock(DatabaseAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Database, CreateDatabaseMetadata> databaseOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getDatabaseAdminClient()).thenReturn(databaseClient);
    when(databaseClient.createDatabase(
            Matchers.eq("test-instance"),
            Matchers.eq("test-database"),
            Matchers.eq(ImmutableList.of())))
        .thenReturn(databaseOperationFuture);
    when(databaseOperationFuture.get()).thenReturn(mock(Database.class));

    EmulatorUtil.maybeCreateInstanceAndDatabase(
        spanner, DatabaseId.of("test-project", "test-instance", "test-database"));

    // Verify that both the instance and the database was created.
    verify(instanceClient)
        .createInstance(
            InstanceInfo.newBuilder(InstanceId.of("test-project", "test-instance"))
                .setDisplayName("Automatically Generated Test Instance")
                .setInstanceConfigId(InstanceConfigId.of("test-project", "emulator-config"))
                .setNodeCount(1)
                .build());
    verify(databaseClient).createDatabase("test-instance", "test-database", ImmutableList.of());
  }

  @Test
  public void testCreateInstanceAndDatabase_bothFailWithAlreadyExists()
      throws InterruptedException, ExecutionException {
    Spanner spanner = mock(Spanner.class);
    SpannerOptions options = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(options);
    when(options.getCredentials()).thenReturn(NoCredentials.getInstance());

    InstanceAdminClient instanceClient = mock(InstanceAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Instance, CreateInstanceMetadata> instanceOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getInstanceAdminClient()).thenReturn(instanceClient);
    when(instanceClient.createInstance(any(InstanceInfo.class)))
        .thenReturn(instanceOperationFuture);
    when(instanceOperationFuture.get())
        .thenThrow(
            new ExecutionException(
                SpannerExceptionFactory.newSpannerException(
                    ErrorCode.ALREADY_EXISTS, "Instance already exists")));

    DatabaseAdminClient databaseClient = mock(DatabaseAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Database, CreateDatabaseMetadata> databaseOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getDatabaseAdminClient()).thenReturn(databaseClient);
    when(databaseClient.createDatabase(
            Matchers.eq("test-instance"),
            Matchers.eq("test-database"),
            Matchers.eq(ImmutableList.of())))
        .thenReturn(databaseOperationFuture);
    when(databaseOperationFuture.get())
        .thenThrow(
            new ExecutionException(
                SpannerExceptionFactory.newSpannerException(
                    ErrorCode.ALREADY_EXISTS, "Database already exists")));

    EmulatorUtil.maybeCreateInstanceAndDatabase(
        spanner, DatabaseId.of("test-project", "test-instance", "test-database"));

    // Verify that both the instance and the database was created.
    verify(instanceClient)
        .createInstance(
            InstanceInfo.newBuilder(InstanceId.of("test-project", "test-instance"))
                .setDisplayName("Automatically Generated Test Instance")
                .setInstanceConfigId(InstanceConfigId.of("test-project", "emulator-config"))
                .setNodeCount(1)
                .build());
    verify(databaseClient).createDatabase("test-instance", "test-database", ImmutableList.of());
  }

  @Test
  public void testCreateInstanceAndDatabase_propagatesOtherErrorsOnInstanceCreation()
      throws InterruptedException, ExecutionException {
    Spanner spanner = mock(Spanner.class);
    SpannerOptions options = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(options);
    when(options.getCredentials()).thenReturn(NoCredentials.getInstance());

    InstanceAdminClient instanceClient = mock(InstanceAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Instance, CreateInstanceMetadata> instanceOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getInstanceAdminClient()).thenReturn(instanceClient);
    when(instanceClient.createInstance(any(InstanceInfo.class)))
        .thenReturn(instanceOperationFuture);
    when(instanceOperationFuture.get())
        .thenThrow(
            new ExecutionException(
                SpannerExceptionFactory.newSpannerException(
                    ErrorCode.INVALID_ARGUMENT, "Invalid instance options")));

    try {
      EmulatorUtil.maybeCreateInstanceAndDatabase(
          spanner, DatabaseId.of("test-project", "test-instance", "test-database"));
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
    }
  }

  @Test
  public void testCreateInstanceAndDatabase_propagatesInterruptsOnInstanceCreation()
      throws InterruptedException, ExecutionException {
    Spanner spanner = mock(Spanner.class);
    SpannerOptions options = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(options);
    when(options.getCredentials()).thenReturn(NoCredentials.getInstance());

    InstanceAdminClient instanceClient = mock(InstanceAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Instance, CreateInstanceMetadata> instanceOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getInstanceAdminClient()).thenReturn(instanceClient);
    when(instanceClient.createInstance(any(InstanceInfo.class)))
        .thenReturn(instanceOperationFuture);
    when(instanceOperationFuture.get()).thenThrow(new InterruptedException());

    try {
      EmulatorUtil.maybeCreateInstanceAndDatabase(
          spanner, DatabaseId.of("test-project", "test-instance", "test-database"));
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertEquals(ErrorCode.CANCELLED, e.getErrorCode());
    }
  }

  @Test
  public void testCreateInstanceAndDatabase_propagatesOtherErrorsOnDatabaseCreation()
      throws InterruptedException, ExecutionException {
    Spanner spanner = mock(Spanner.class);
    SpannerOptions options = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(options);
    when(options.getCredentials()).thenReturn(NoCredentials.getInstance());

    InstanceAdminClient instanceClient = mock(InstanceAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Instance, CreateInstanceMetadata> instanceOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getInstanceAdminClient()).thenReturn(instanceClient);
    when(instanceClient.createInstance(any(InstanceInfo.class)))
        .thenReturn(instanceOperationFuture);
    when(instanceOperationFuture.get()).thenReturn(mock(Instance.class));

    DatabaseAdminClient databaseClient = mock(DatabaseAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Database, CreateDatabaseMetadata> databaseOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getDatabaseAdminClient()).thenReturn(databaseClient);
    when(databaseClient.createDatabase(
            Matchers.eq("test-instance"),
            Matchers.eq("test-database"),
            Matchers.eq(ImmutableList.of())))
        .thenReturn(databaseOperationFuture);
    when(databaseOperationFuture.get())
        .thenThrow(
            new ExecutionException(
                SpannerExceptionFactory.newSpannerException(
                    ErrorCode.INVALID_ARGUMENT, "Invalid database options")));

    try {
      EmulatorUtil.maybeCreateInstanceAndDatabase(
          spanner, DatabaseId.of("test-project", "test-instance", "test-database"));
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
    }
  }

  @Test
  public void testCreateInstanceAndDatabase_propagatesInterruptsOnDatabaseCreation()
      throws InterruptedException, ExecutionException {
    Spanner spanner = mock(Spanner.class);
    SpannerOptions options = mock(SpannerOptions.class);
    when(spanner.getOptions()).thenReturn(options);
    when(options.getCredentials()).thenReturn(NoCredentials.getInstance());

    InstanceAdminClient instanceClient = mock(InstanceAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Instance, CreateInstanceMetadata> instanceOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getInstanceAdminClient()).thenReturn(instanceClient);
    when(instanceClient.createInstance(any(InstanceInfo.class)))
        .thenReturn(instanceOperationFuture);
    when(instanceOperationFuture.get()).thenReturn(mock(Instance.class));

    DatabaseAdminClient databaseClient = mock(DatabaseAdminClient.class);
    @SuppressWarnings("unchecked")
    OperationFuture<Database, CreateDatabaseMetadata> databaseOperationFuture =
        mock(OperationFuture.class);

    when(spanner.getDatabaseAdminClient()).thenReturn(databaseClient);
    when(databaseClient.createDatabase(
            Matchers.eq("test-instance"),
            Matchers.eq("test-database"),
            Matchers.eq(ImmutableList.of())))
        .thenReturn(databaseOperationFuture);
    when(databaseOperationFuture.get()).thenThrow(new InterruptedException());

    try {
      EmulatorUtil.maybeCreateInstanceAndDatabase(
          spanner, DatabaseId.of("test-project", "test-instance", "test-database"));
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertEquals(ErrorCode.CANCELLED, e.getErrorCode());
    }
  }
}
