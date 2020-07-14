/*
 * Copyright 2020 Google LLC
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
package com.google.cloud.spanner.admin.database.v1;

import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListBackupOperationsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListBackupsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabaseOperationsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabasesPagedResponse;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GaxGrpcProperties;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.grpc.testing.MockGrpcService;
import com.google.api.gax.grpc.testing.MockServiceHelper;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.resourcenames.ResourceName;
import com.google.common.collect.Lists;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.BackupName;
import com.google.spanner.admin.database.v1.CreateBackupRequest;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.DeleteBackupRequest;
import com.google.spanner.admin.database.v1.DropDatabaseRequest;
import com.google.spanner.admin.database.v1.GetBackupRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlResponse;
import com.google.spanner.admin.database.v1.GetDatabaseRequest;
import com.google.spanner.admin.database.v1.InstanceName;
import com.google.spanner.admin.database.v1.ListBackupOperationsRequest;
import com.google.spanner.admin.database.v1.ListBackupOperationsResponse;
import com.google.spanner.admin.database.v1.ListBackupsRequest;
import com.google.spanner.admin.database.v1.ListBackupsResponse;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsRequest;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsResponse;
import com.google.spanner.admin.database.v1.ListDatabasesRequest;
import com.google.spanner.admin.database.v1.ListDatabasesResponse;
import com.google.spanner.admin.database.v1.RestoreDatabaseRequest;
import com.google.spanner.admin.database.v1.UpdateBackupRequest;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@javax.annotation.Generated("by GAPIC")
public class DatabaseAdminClientTest {
  private static MockDatabaseAdmin mockDatabaseAdmin;
  private static MockServiceHelper serviceHelper;
  private DatabaseAdminClient client;
  private LocalChannelProvider channelProvider;

  @BeforeClass
  public static void startStaticServer() {
    mockDatabaseAdmin = new MockDatabaseAdmin();
    serviceHelper =
        new MockServiceHelper(
            UUID.randomUUID().toString(), Arrays.<MockGrpcService>asList(mockDatabaseAdmin));
    serviceHelper.start();
  }

  @AfterClass
  public static void stopServer() {
    serviceHelper.stop();
  }

  @Before
  public void setUp() throws IOException {
    serviceHelper.reset();
    channelProvider = serviceHelper.createChannelProvider();
    DatabaseAdminSettings settings =
        DatabaseAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    client = DatabaseAdminClient.create(settings);
  }

  @After
  public void tearDown() throws Exception {
    client.close();
  }

  @Test
  @SuppressWarnings("all")
  public void createDatabaseTest() throws Exception {
    DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    Database expectedResponse = Database.newBuilder().setName(name.toString()).build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
    String createStatement = "createStatement552974828";

    Database actualResponse = client.createDatabaseAsync(parent, createStatement).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateDatabaseRequest actualRequest = (CreateDatabaseRequest) actualRequests.get(0);

    Assert.assertEquals(parent, InstanceName.parse(actualRequest.getParent()));
    Assert.assertEquals(createStatement, actualRequest.getCreateStatement());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void createDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      String createStatement = "createStatement552974828";

      client.createDatabaseAsync(parent, createStatement).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = (InvalidArgumentException) e.getCause();
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  @SuppressWarnings("all")
  public void updateDatabaseDdlTest() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("updateDatabaseDdlTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    List<String> statements = new ArrayList<>();

    Empty actualResponse = client.updateDatabaseDdlAsync(database, statements).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateDatabaseDdlRequest actualRequest = (UpdateDatabaseDdlRequest) actualRequests.get(0);

    Assert.assertEquals(database, DatabaseName.parse(actualRequest.getDatabase()));
    Assert.assertEquals(statements, actualRequest.getStatementsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void updateDatabaseDdlExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
      List<String> statements = new ArrayList<>();

      client.updateDatabaseDdlAsync(database, statements).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = (InvalidArgumentException) e.getCause();
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  @SuppressWarnings("all")
  public void createBackupTest() throws Exception {
    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
    long sizeBytes = 1796325715L;
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(database.toString())
            .setName(name.toString())
            .setSizeBytes(sizeBytes)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createBackupTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
    Backup backup = Backup.newBuilder().build();
    String backupId = "backupId1355353272";

    Backup actualResponse = client.createBackupAsync(parent, backup, backupId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateBackupRequest actualRequest = (CreateBackupRequest) actualRequests.get(0);

    Assert.assertEquals(parent, InstanceName.parse(actualRequest.getParent()));
    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(backupId, actualRequest.getBackupId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void createBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      Backup backup = Backup.newBuilder().build();
      String backupId = "backupId1355353272";

      client.createBackupAsync(parent, backup, backupId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = (InvalidArgumentException) e.getCause();
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  @SuppressWarnings("all")
  public void restoreDatabaseTest() throws Exception {
    DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    Database expectedResponse = Database.newBuilder().setName(name.toString()).build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("restoreDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
    String databaseId = "databaseId816491103";
    BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    Database actualResponse = client.restoreDatabaseAsync(parent, databaseId, backup).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RestoreDatabaseRequest actualRequest = (RestoreDatabaseRequest) actualRequests.get(0);

    Assert.assertEquals(parent, InstanceName.parse(actualRequest.getParent()));
    Assert.assertEquals(databaseId, actualRequest.getDatabaseId());
    Assert.assertEquals(backup, BackupName.parse(actualRequest.getBackup()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void restoreDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      String databaseId = "databaseId816491103";
      BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

      client.restoreDatabaseAsync(parent, databaseId, backup).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = (InvalidArgumentException) e.getCause();
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  @SuppressWarnings("all")
  public void listDatabasesTest() {
    String nextPageToken = "";
    Database databasesElement = Database.newBuilder().build();
    List<Database> databases = Arrays.asList(databasesElement);
    ListDatabasesResponse expectedResponse =
        ListDatabasesResponse.newBuilder()
            .setNextPageToken(nextPageToken)
            .addAllDatabases(databases)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

    ListDatabasesPagedResponse pagedListResponse = client.listDatabases(parent);

    List<Database> resources = Lists.newArrayList(pagedListResponse.iterateAll());
    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getDatabasesList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListDatabasesRequest actualRequest = (ListDatabasesRequest) actualRequests.get(0);

    Assert.assertEquals(parent, InstanceName.parse(actualRequest.getParent()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void listDatabasesExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

      client.listDatabases(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void getDatabaseTest() {
    DatabaseName name2 = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    Database expectedResponse = Database.newBuilder().setName(name2.toString()).build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    Database actualResponse = client.getDatabase(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetDatabaseRequest actualRequest = (GetDatabaseRequest) actualRequests.get(0);

    Assert.assertEquals(name, DatabaseName.parse(actualRequest.getName()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void getDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

      client.getDatabase(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void dropDatabaseTest() {
    Empty expectedResponse = Empty.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    client.dropDatabase(database);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DropDatabaseRequest actualRequest = (DropDatabaseRequest) actualRequests.get(0);

    Assert.assertEquals(database, DatabaseName.parse(actualRequest.getDatabase()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void dropDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

      client.dropDatabase(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void getDatabaseDdlTest() {
    GetDatabaseDdlResponse expectedResponse = GetDatabaseDdlResponse.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    GetDatabaseDdlResponse actualResponse = client.getDatabaseDdl(database);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetDatabaseDdlRequest actualRequest = (GetDatabaseDdlRequest) actualRequests.get(0);

    Assert.assertEquals(database, DatabaseName.parse(actualRequest.getDatabase()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void getDatabaseDdlExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

      client.getDatabaseDdl(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void setIamPolicyTest() {
    int version = 351608024;
    ByteString etag = ByteString.copyFromUtf8("21");
    Policy expectedResponse = Policy.newBuilder().setVersion(version).setEtag(etag).build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    ResourceName resource = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
    Policy policy = Policy.newBuilder().build();

    Policy actualResponse = client.setIamPolicy(resource, policy);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    SetIamPolicyRequest actualRequest = (SetIamPolicyRequest) actualRequests.get(0);

    Assert.assertEquals(Objects.toString(resource), Objects.toString(actualRequest.getResource()));
    Assert.assertEquals(policy, actualRequest.getPolicy());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void setIamPolicyExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      ResourceName resource = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
      Policy policy = Policy.newBuilder().build();

      client.setIamPolicy(resource, policy);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void getIamPolicyTest() {
    int version = 351608024;
    ByteString etag = ByteString.copyFromUtf8("21");
    Policy expectedResponse = Policy.newBuilder().setVersion(version).setEtag(etag).build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    ResourceName resource = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    Policy actualResponse = client.getIamPolicy(resource);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetIamPolicyRequest actualRequest = (GetIamPolicyRequest) actualRequests.get(0);

    Assert.assertEquals(Objects.toString(resource), Objects.toString(actualRequest.getResource()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void getIamPolicyExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      ResourceName resource = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

      client.getIamPolicy(resource);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void testIamPermissionsTest() {
    TestIamPermissionsResponse expectedResponse = TestIamPermissionsResponse.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    ResourceName resource = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
    List<String> permissions = new ArrayList<>();

    TestIamPermissionsResponse actualResponse = client.testIamPermissions(resource, permissions);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    TestIamPermissionsRequest actualRequest = (TestIamPermissionsRequest) actualRequests.get(0);

    Assert.assertEquals(Objects.toString(resource), Objects.toString(actualRequest.getResource()));
    Assert.assertEquals(permissions, actualRequest.getPermissionsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void testIamPermissionsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      ResourceName resource = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
      List<String> permissions = new ArrayList<>();

      client.testIamPermissions(resource, permissions);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void getBackupTest() {
    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    BackupName name2 = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
    long sizeBytes = 1796325715L;
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(database.toString())
            .setName(name2.toString())
            .setSizeBytes(sizeBytes)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    Backup actualResponse = client.getBackup(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetBackupRequest actualRequest = (GetBackupRequest) actualRequests.get(0);

    Assert.assertEquals(name, BackupName.parse(actualRequest.getName()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void getBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

      client.getBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void updateBackupTest() {
    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
    long sizeBytes = 1796325715L;
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(database.toString())
            .setName(name.toString())
            .setSizeBytes(sizeBytes)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    Backup backup = Backup.newBuilder().build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Backup actualResponse = client.updateBackup(backup, updateMask);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateBackupRequest actualRequest = (UpdateBackupRequest) actualRequests.get(0);

    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(updateMask, actualRequest.getUpdateMask());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void updateBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      Backup backup = Backup.newBuilder().build();
      FieldMask updateMask = FieldMask.newBuilder().build();

      client.updateBackup(backup, updateMask);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void deleteBackupTest() {
    Empty expectedResponse = Empty.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    client.deleteBackup(name);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteBackupRequest actualRequest = (DeleteBackupRequest) actualRequests.get(0);

    Assert.assertEquals(name, BackupName.parse(actualRequest.getName()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void deleteBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

      client.deleteBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void listBackupsTest() {
    String nextPageToken = "";
    Backup backupsElement = Backup.newBuilder().build();
    List<Backup> backups = Arrays.asList(backupsElement);
    ListBackupsResponse expectedResponse =
        ListBackupsResponse.newBuilder()
            .setNextPageToken(nextPageToken)
            .addAllBackups(backups)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

    ListBackupsPagedResponse pagedListResponse = client.listBackups(parent);

    List<Backup> resources = Lists.newArrayList(pagedListResponse.iterateAll());
    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getBackupsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupsRequest actualRequest = (ListBackupsRequest) actualRequests.get(0);

    Assert.assertEquals(parent, InstanceName.parse(actualRequest.getParent()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void listBackupsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

      client.listBackups(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void listDatabaseOperationsTest() {
    String nextPageToken = "";
    Operation operationsElement = Operation.newBuilder().build();
    List<Operation> operations = Arrays.asList(operationsElement);
    ListDatabaseOperationsResponse expectedResponse =
        ListDatabaseOperationsResponse.newBuilder()
            .setNextPageToken(nextPageToken)
            .addAllOperations(operations)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

    ListDatabaseOperationsPagedResponse pagedListResponse = client.listDatabaseOperations(parent);

    List<Operation> resources = Lists.newArrayList(pagedListResponse.iterateAll());
    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getOperationsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListDatabaseOperationsRequest actualRequest =
        (ListDatabaseOperationsRequest) actualRequests.get(0);

    Assert.assertEquals(parent, InstanceName.parse(actualRequest.getParent()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void listDatabaseOperationsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

      client.listDatabaseOperations(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void listBackupOperationsTest() {
    String nextPageToken = "";
    Operation operationsElement = Operation.newBuilder().build();
    List<Operation> operations = Arrays.asList(operationsElement);
    ListBackupOperationsResponse expectedResponse =
        ListBackupOperationsResponse.newBuilder()
            .setNextPageToken(nextPageToken)
            .addAllOperations(operations)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

    ListBackupOperationsPagedResponse pagedListResponse = client.listBackupOperations(parent);

    List<Operation> resources = Lists.newArrayList(pagedListResponse.iterateAll());
    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getOperationsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupOperationsRequest actualRequest = (ListBackupOperationsRequest) actualRequests.get(0);

    Assert.assertEquals(parent, InstanceName.parse(actualRequest.getParent()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void listBackupOperationsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

      client.listBackupOperations(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }
}
