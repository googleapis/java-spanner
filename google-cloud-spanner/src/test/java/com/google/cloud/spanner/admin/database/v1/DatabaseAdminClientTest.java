/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import com.google.iam.v1.Binding;
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
import com.google.protobuf.Timestamp;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.BackupName;
import com.google.spanner.admin.database.v1.CreateBackupRequest;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.CryptoKeyVersionName;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.DeleteBackupRequest;
import com.google.spanner.admin.database.v1.DropDatabaseRequest;
import com.google.spanner.admin.database.v1.EncryptionConfig;
import com.google.spanner.admin.database.v1.EncryptionInfo;
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
import com.google.spanner.admin.database.v1.RestoreInfo;
import com.google.spanner.admin.database.v1.UpdateBackupRequest;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.annotation.Generated;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Generated("by gapic-generator-java")
public class DatabaseAdminClientTest {
  private static MockServiceHelper mockServiceHelper;
  private static MockDatabaseAdmin mockDatabaseAdmin;
  private DatabaseAdminClient client;
  private LocalChannelProvider channelProvider;

  @BeforeClass
  public static void startStaticServer() {
    mockDatabaseAdmin = new MockDatabaseAdmin();
    mockServiceHelper =
        new MockServiceHelper(
            UUID.randomUUID().toString(), Arrays.<MockGrpcService>asList(mockDatabaseAdmin));
    mockServiceHelper.start();
  }

  @AfterClass
  public static void stopServer() {
    mockServiceHelper.stop();
  }

  @Before
  public void setUp() throws IOException {
    mockServiceHelper.reset();
    channelProvider = mockServiceHelper.createChannelProvider();
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
  public void listDatabasesTest() throws Exception {
    Database responsesElement = Database.newBuilder().build();
    ListDatabasesResponse expectedResponse =
        ListDatabasesResponse.newBuilder()
            .setNextPageToken("")
            .addAllDatabases(Arrays.asList(responsesElement))
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

    ListDatabasesPagedResponse pagedListResponse = client.listDatabases(parent);

    List<Database> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getDatabasesList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListDatabasesRequest actualRequest = ((ListDatabasesRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listDatabasesExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      client.listDatabases(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listDatabasesTest2() throws Exception {
    Database responsesElement = Database.newBuilder().build();
    ListDatabasesResponse expectedResponse =
        ListDatabasesResponse.newBuilder()
            .setNextPageToken("")
            .addAllDatabases(Arrays.asList(responsesElement))
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListDatabasesPagedResponse pagedListResponse = client.listDatabases(parent);

    List<Database> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getDatabasesList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListDatabasesRequest actualRequest = ((ListDatabasesRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listDatabasesExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listDatabases(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void createDatabaseTest() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
    String createStatement = "createStatement744686547";

    Database actualResponse = client.createDatabaseAsync(parent, createStatement).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateDatabaseRequest actualRequest = ((CreateDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(createStatement, actualRequest.getCreateStatement());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      String createStatement = "createStatement744686547";
      client.createDatabaseAsync(parent, createStatement).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void createDatabaseTest2() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    String parent = "parent-995424086";
    String createStatement = "createStatement744686547";

    Database actualResponse = client.createDatabaseAsync(parent, createStatement).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateDatabaseRequest actualRequest = ((CreateDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(createStatement, actualRequest.getCreateStatement());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createDatabaseExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      String createStatement = "createStatement744686547";
      client.createDatabaseAsync(parent, createStatement).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void getDatabaseTest() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    Database actualResponse = client.getDatabase(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetDatabaseRequest actualRequest = ((GetDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
      client.getDatabase(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getDatabaseTest2() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String name = "name3373707";

    Database actualResponse = client.getDatabase(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetDatabaseRequest actualRequest = ((GetDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getDatabaseExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String name = "name3373707";
      client.getDatabase(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
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

    client.updateDatabaseDdlAsync(database, statements).get();

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateDatabaseDdlRequest actualRequest = ((UpdateDatabaseDdlRequest) actualRequests.get(0));

    Assert.assertEquals(database.toString(), actualRequest.getDatabase());
    Assert.assertEquals(statements, actualRequest.getStatementsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void updateDatabaseDdlExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
      List<String> statements = new ArrayList<>();
      client.updateDatabaseDdlAsync(database, statements).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void updateDatabaseDdlTest2() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("updateDatabaseDdlTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    String database = "database1789464955";
    List<String> statements = new ArrayList<>();

    client.updateDatabaseDdlAsync(database, statements).get();

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateDatabaseDdlRequest actualRequest = ((UpdateDatabaseDdlRequest) actualRequests.get(0));

    Assert.assertEquals(database, actualRequest.getDatabase());
    Assert.assertEquals(statements, actualRequest.getStatementsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void updateDatabaseDdlExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String database = "database1789464955";
      List<String> statements = new ArrayList<>();
      client.updateDatabaseDdlAsync(database, statements).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void dropDatabaseTest() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    client.dropDatabase(database);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DropDatabaseRequest actualRequest = ((DropDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(database.toString(), actualRequest.getDatabase());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void dropDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
      client.dropDatabase(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void dropDatabaseTest2() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String database = "database1789464955";

    client.dropDatabase(database);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DropDatabaseRequest actualRequest = ((DropDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(database, actualRequest.getDatabase());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void dropDatabaseExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String database = "database1789464955";
      client.dropDatabase(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getDatabaseDdlTest() throws Exception {
    GetDatabaseDdlResponse expectedResponse =
        GetDatabaseDdlResponse.newBuilder().addAllStatements(new ArrayList<String>()).build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    GetDatabaseDdlResponse actualResponse = client.getDatabaseDdl(database);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetDatabaseDdlRequest actualRequest = ((GetDatabaseDdlRequest) actualRequests.get(0));

    Assert.assertEquals(database.toString(), actualRequest.getDatabase());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getDatabaseDdlExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
      client.getDatabaseDdl(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getDatabaseDdlTest2() throws Exception {
    GetDatabaseDdlResponse expectedResponse =
        GetDatabaseDdlResponse.newBuilder().addAllStatements(new ArrayList<String>()).build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String database = "database1789464955";

    GetDatabaseDdlResponse actualResponse = client.getDatabaseDdl(database);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetDatabaseDdlRequest actualRequest = ((GetDatabaseDdlRequest) actualRequests.get(0));

    Assert.assertEquals(database, actualRequest.getDatabase());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getDatabaseDdlExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String database = "database1789464955";
      client.getDatabaseDdl(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void setIamPolicyTest() throws Exception {
    Policy expectedResponse =
        Policy.newBuilder()
            .setVersion(351608024)
            .addAllBindings(new ArrayList<Binding>())
            .setEtag(ByteString.EMPTY)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    ResourceName resource =
        CryptoKeyVersionName.of(
            "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
    Policy policy = Policy.newBuilder().build();

    Policy actualResponse = client.setIamPolicy(resource, policy);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    SetIamPolicyRequest actualRequest = ((SetIamPolicyRequest) actualRequests.get(0));

    Assert.assertEquals(resource.toString(), actualRequest.getResource());
    Assert.assertEquals(policy, actualRequest.getPolicy());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void setIamPolicyExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      ResourceName resource =
          CryptoKeyVersionName.of(
              "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
      Policy policy = Policy.newBuilder().build();
      client.setIamPolicy(resource, policy);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void setIamPolicyTest2() throws Exception {
    Policy expectedResponse =
        Policy.newBuilder()
            .setVersion(351608024)
            .addAllBindings(new ArrayList<Binding>())
            .setEtag(ByteString.EMPTY)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String resource = "resource-341064690";
    Policy policy = Policy.newBuilder().build();

    Policy actualResponse = client.setIamPolicy(resource, policy);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    SetIamPolicyRequest actualRequest = ((SetIamPolicyRequest) actualRequests.get(0));

    Assert.assertEquals(resource, actualRequest.getResource());
    Assert.assertEquals(policy, actualRequest.getPolicy());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void setIamPolicyExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String resource = "resource-341064690";
      Policy policy = Policy.newBuilder().build();
      client.setIamPolicy(resource, policy);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getIamPolicyTest() throws Exception {
    Policy expectedResponse =
        Policy.newBuilder()
            .setVersion(351608024)
            .addAllBindings(new ArrayList<Binding>())
            .setEtag(ByteString.EMPTY)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    ResourceName resource =
        CryptoKeyVersionName.of(
            "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");

    Policy actualResponse = client.getIamPolicy(resource);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetIamPolicyRequest actualRequest = ((GetIamPolicyRequest) actualRequests.get(0));

    Assert.assertEquals(resource.toString(), actualRequest.getResource());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getIamPolicyExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      ResourceName resource =
          CryptoKeyVersionName.of(
              "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
      client.getIamPolicy(resource);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getIamPolicyTest2() throws Exception {
    Policy expectedResponse =
        Policy.newBuilder()
            .setVersion(351608024)
            .addAllBindings(new ArrayList<Binding>())
            .setEtag(ByteString.EMPTY)
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String resource = "resource-341064690";

    Policy actualResponse = client.getIamPolicy(resource);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetIamPolicyRequest actualRequest = ((GetIamPolicyRequest) actualRequests.get(0));

    Assert.assertEquals(resource, actualRequest.getResource());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getIamPolicyExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String resource = "resource-341064690";
      client.getIamPolicy(resource);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void testIamPermissionsTest() throws Exception {
    TestIamPermissionsResponse expectedResponse =
        TestIamPermissionsResponse.newBuilder().addAllPermissions(new ArrayList<String>()).build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    ResourceName resource =
        CryptoKeyVersionName.of(
            "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
    List<String> permissions = new ArrayList<>();

    TestIamPermissionsResponse actualResponse = client.testIamPermissions(resource, permissions);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    TestIamPermissionsRequest actualRequest = ((TestIamPermissionsRequest) actualRequests.get(0));

    Assert.assertEquals(resource.toString(), actualRequest.getResource());
    Assert.assertEquals(permissions, actualRequest.getPermissionsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void testIamPermissionsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      ResourceName resource =
          CryptoKeyVersionName.of(
              "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
      List<String> permissions = new ArrayList<>();
      client.testIamPermissions(resource, permissions);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void testIamPermissionsTest2() throws Exception {
    TestIamPermissionsResponse expectedResponse =
        TestIamPermissionsResponse.newBuilder().addAllPermissions(new ArrayList<String>()).build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String resource = "resource-341064690";
    List<String> permissions = new ArrayList<>();

    TestIamPermissionsResponse actualResponse = client.testIamPermissions(resource, permissions);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    TestIamPermissionsRequest actualRequest = ((TestIamPermissionsRequest) actualRequests.get(0));

    Assert.assertEquals(resource, actualRequest.getResource());
    Assert.assertEquals(permissions, actualRequest.getPermissionsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void testIamPermissionsExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String resource = "resource-341064690";
      List<String> permissions = new ArrayList<>();
      client.testIamPermissions(resource, permissions);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void createBackupTest() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setVersionTime(Timestamp.newBuilder().build())
            .setExpireTime(Timestamp.newBuilder().build())
            .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setSizeBytes(-1796325715)
            .addAllReferencingDatabases(new ArrayList<String>())
            .setEncryptionInfo(EncryptionInfo.newBuilder().build())
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
    String backupId = "backupId2121930365";

    Backup actualResponse = client.createBackupAsync(parent, backup, backupId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateBackupRequest actualRequest = ((CreateBackupRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(backupId, actualRequest.getBackupId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      Backup backup = Backup.newBuilder().build();
      String backupId = "backupId2121930365";
      client.createBackupAsync(parent, backup, backupId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void createBackupTest2() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setVersionTime(Timestamp.newBuilder().build())
            .setExpireTime(Timestamp.newBuilder().build())
            .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setSizeBytes(-1796325715)
            .addAllReferencingDatabases(new ArrayList<String>())
            .setEncryptionInfo(EncryptionInfo.newBuilder().build())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createBackupTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    String parent = "parent-995424086";
    Backup backup = Backup.newBuilder().build();
    String backupId = "backupId2121930365";

    Backup actualResponse = client.createBackupAsync(parent, backup, backupId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateBackupRequest actualRequest = ((CreateBackupRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(backupId, actualRequest.getBackupId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createBackupExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      Backup backup = Backup.newBuilder().build();
      String backupId = "backupId2121930365";
      client.createBackupAsync(parent, backup, backupId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void getBackupTest() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setVersionTime(Timestamp.newBuilder().build())
            .setExpireTime(Timestamp.newBuilder().build())
            .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setSizeBytes(-1796325715)
            .addAllReferencingDatabases(new ArrayList<String>())
            .setEncryptionInfo(EncryptionInfo.newBuilder().build())
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    Backup actualResponse = client.getBackup(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetBackupRequest actualRequest = ((GetBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
      client.getBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getBackupTest2() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setVersionTime(Timestamp.newBuilder().build())
            .setExpireTime(Timestamp.newBuilder().build())
            .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setSizeBytes(-1796325715)
            .addAllReferencingDatabases(new ArrayList<String>())
            .setEncryptionInfo(EncryptionInfo.newBuilder().build())
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String name = "name3373707";

    Backup actualResponse = client.getBackup(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetBackupRequest actualRequest = ((GetBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getBackupExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String name = "name3373707";
      client.getBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void updateBackupTest() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setVersionTime(Timestamp.newBuilder().build())
            .setExpireTime(Timestamp.newBuilder().build())
            .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setSizeBytes(-1796325715)
            .addAllReferencingDatabases(new ArrayList<String>())
            .setEncryptionInfo(EncryptionInfo.newBuilder().build())
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    Backup backup = Backup.newBuilder().build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Backup actualResponse = client.updateBackup(backup, updateMask);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateBackupRequest actualRequest = ((UpdateBackupRequest) actualRequests.get(0));

    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(updateMask, actualRequest.getUpdateMask());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void updateBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      Backup backup = Backup.newBuilder().build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateBackup(backup, updateMask);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void deleteBackupTest() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    client.deleteBackup(name);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteBackupRequest actualRequest = ((DeleteBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
      client.deleteBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void deleteBackupTest2() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String name = "name3373707";

    client.deleteBackup(name);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteBackupRequest actualRequest = ((DeleteBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteBackupExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String name = "name3373707";
      client.deleteBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listBackupsTest() throws Exception {
    Backup responsesElement = Backup.newBuilder().build();
    ListBackupsResponse expectedResponse =
        ListBackupsResponse.newBuilder()
            .setNextPageToken("")
            .addAllBackups(Arrays.asList(responsesElement))
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

    ListBackupsPagedResponse pagedListResponse = client.listBackups(parent);

    List<Backup> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getBackupsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupsRequest actualRequest = ((ListBackupsRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listBackupsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      client.listBackups(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listBackupsTest2() throws Exception {
    Backup responsesElement = Backup.newBuilder().build();
    ListBackupsResponse expectedResponse =
        ListBackupsResponse.newBuilder()
            .setNextPageToken("")
            .addAllBackups(Arrays.asList(responsesElement))
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListBackupsPagedResponse pagedListResponse = client.listBackups(parent);

    List<Backup> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getBackupsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupsRequest actualRequest = ((ListBackupsRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listBackupsExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listBackups(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void restoreDatabaseTest() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("restoreDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
    String databaseId = "databaseId1688905718";
    BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    Database actualResponse = client.restoreDatabaseAsync(parent, databaseId, backup).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RestoreDatabaseRequest actualRequest = ((RestoreDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(databaseId, actualRequest.getDatabaseId());
    Assert.assertEquals(backup.toString(), actualRequest.getBackup());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void restoreDatabaseExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      String databaseId = "databaseId1688905718";
      BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
      client.restoreDatabaseAsync(parent, databaseId, backup).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void restoreDatabaseTest2() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("restoreDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
    String databaseId = "databaseId1688905718";
    String backup = "backup-1396673086";

    Database actualResponse = client.restoreDatabaseAsync(parent, databaseId, backup).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RestoreDatabaseRequest actualRequest = ((RestoreDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(databaseId, actualRequest.getDatabaseId());
    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void restoreDatabaseExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      String databaseId = "databaseId1688905718";
      String backup = "backup-1396673086";
      client.restoreDatabaseAsync(parent, databaseId, backup).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void restoreDatabaseTest3() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("restoreDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    String parent = "parent-995424086";
    String databaseId = "databaseId1688905718";
    BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");

    Database actualResponse = client.restoreDatabaseAsync(parent, databaseId, backup).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RestoreDatabaseRequest actualRequest = ((RestoreDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(databaseId, actualRequest.getDatabaseId());
    Assert.assertEquals(backup.toString(), actualRequest.getBackup());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void restoreDatabaseExceptionTest3() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      String databaseId = "databaseId1688905718";
      BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
      client.restoreDatabaseAsync(parent, databaseId, backup).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void restoreDatabaseTest4() throws Exception {
    Database expectedResponse =
        Database.newBuilder()
            .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
            .setCreateTime(Timestamp.newBuilder().build())
            .setRestoreInfo(RestoreInfo.newBuilder().build())
            .setEncryptionConfig(EncryptionConfig.newBuilder().build())
            .addAllEncryptionInfo(new ArrayList<EncryptionInfo>())
            .setVersionRetentionPeriod("versionRetentionPeriod-629783929")
            .setEarliestVersionTime(Timestamp.newBuilder().build())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("restoreDatabaseTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockDatabaseAdmin.addResponse(resultOperation);

    String parent = "parent-995424086";
    String databaseId = "databaseId1688905718";
    String backup = "backup-1396673086";

    Database actualResponse = client.restoreDatabaseAsync(parent, databaseId, backup).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RestoreDatabaseRequest actualRequest = ((RestoreDatabaseRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(databaseId, actualRequest.getDatabaseId());
    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void restoreDatabaseExceptionTest4() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      String databaseId = "databaseId1688905718";
      String backup = "backup-1396673086";
      client.restoreDatabaseAsync(parent, databaseId, backup).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void listDatabaseOperationsTest() throws Exception {
    Operation responsesElement = Operation.newBuilder().build();
    ListDatabaseOperationsResponse expectedResponse =
        ListDatabaseOperationsResponse.newBuilder()
            .setNextPageToken("")
            .addAllOperations(Arrays.asList(responsesElement))
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
        ((ListDatabaseOperationsRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listDatabaseOperationsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      client.listDatabaseOperations(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listDatabaseOperationsTest2() throws Exception {
    Operation responsesElement = Operation.newBuilder().build();
    ListDatabaseOperationsResponse expectedResponse =
        ListDatabaseOperationsResponse.newBuilder()
            .setNextPageToken("")
            .addAllOperations(Arrays.asList(responsesElement))
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListDatabaseOperationsPagedResponse pagedListResponse = client.listDatabaseOperations(parent);

    List<Operation> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getOperationsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListDatabaseOperationsRequest actualRequest =
        ((ListDatabaseOperationsRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listDatabaseOperationsExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listDatabaseOperations(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listBackupOperationsTest() throws Exception {
    Operation responsesElement = Operation.newBuilder().build();
    ListBackupOperationsResponse expectedResponse =
        ListBackupOperationsResponse.newBuilder()
            .setNextPageToken("")
            .addAllOperations(Arrays.asList(responsesElement))
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");

    ListBackupOperationsPagedResponse pagedListResponse = client.listBackupOperations(parent);

    List<Operation> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getOperationsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupOperationsRequest actualRequest =
        ((ListBackupOperationsRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listBackupOperationsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
      client.listBackupOperations(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listBackupOperationsTest2() throws Exception {
    Operation responsesElement = Operation.newBuilder().build();
    ListBackupOperationsResponse expectedResponse =
        ListBackupOperationsResponse.newBuilder()
            .setNextPageToken("")
            .addAllOperations(Arrays.asList(responsesElement))
            .build();
    mockDatabaseAdmin.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListBackupOperationsPagedResponse pagedListResponse = client.listBackupOperations(parent);

    List<Operation> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getOperationsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockDatabaseAdmin.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupOperationsRequest actualRequest =
        ((ListBackupOperationsRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listBackupOperationsExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockDatabaseAdmin.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listBackupOperations(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }
}
