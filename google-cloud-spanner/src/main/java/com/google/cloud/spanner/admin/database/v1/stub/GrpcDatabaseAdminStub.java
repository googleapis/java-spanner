/*
 * Copyright 2020 Google LLC
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

package com.google.cloud.spanner.admin.database.v1.stub;

import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListBackupOperationsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListBackupsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabaseOperationsPagedResponse;
import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabasesPagedResponse;

import com.google.api.gax.core.BackgroundResource;
import com.google.api.gax.core.BackgroundResourceAggregation;
import com.google.api.gax.grpc.GrpcCallSettings;
import com.google.api.gax.grpc.GrpcStubCallableFactory;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.gax.rpc.OperationCallable;
import com.google.api.gax.rpc.RequestParamsExtractor;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.common.collect.ImmutableMap;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.longrunning.stub.GrpcOperationsStub;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateBackupRequest;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DeleteBackupRequest;
import com.google.spanner.admin.database.v1.DropDatabaseRequest;
import com.google.spanner.admin.database.v1.GetBackupRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlResponse;
import com.google.spanner.admin.database.v1.GetDatabaseRequest;
import com.google.spanner.admin.database.v1.ListBackupOperationsRequest;
import com.google.spanner.admin.database.v1.ListBackupOperationsResponse;
import com.google.spanner.admin.database.v1.ListBackupsRequest;
import com.google.spanner.admin.database.v1.ListBackupsResponse;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsRequest;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsResponse;
import com.google.spanner.admin.database.v1.ListDatabasesRequest;
import com.google.spanner.admin.database.v1.ListDatabasesResponse;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseRequest;
import com.google.spanner.admin.database.v1.UpdateBackupRequest;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Generated;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * gRPC stub implementation for the DatabaseAdmin service API.
 *
 * <p>This class is for advanced usage and reflects the underlying API directly.
 */
@Generated("by gapic-generator-java")
public class GrpcDatabaseAdminStub extends DatabaseAdminStub {
  private static final MethodDescriptor<ListDatabasesRequest, ListDatabasesResponse>
      listDatabasesMethodDescriptor =
          MethodDescriptor.<ListDatabasesRequest, ListDatabasesResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/ListDatabases")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(ListDatabasesRequest.getDefaultInstance()))
              .setResponseMarshaller(
                  ProtoUtils.marshaller(ListDatabasesResponse.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<CreateDatabaseRequest, Operation>
      createDatabaseMethodDescriptor =
          MethodDescriptor.<CreateDatabaseRequest, Operation>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/CreateDatabase")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(CreateDatabaseRequest.getDefaultInstance()))
              .setResponseMarshaller(ProtoUtils.marshaller(Operation.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<GetDatabaseRequest, Database> getDatabaseMethodDescriptor =
      MethodDescriptor.<GetDatabaseRequest, Database>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/GetDatabase")
          .setRequestMarshaller(ProtoUtils.marshaller(GetDatabaseRequest.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Database.getDefaultInstance()))
          .build();

  private static final MethodDescriptor<UpdateDatabaseDdlRequest, Operation>
      updateDatabaseDdlMethodDescriptor =
          MethodDescriptor.<UpdateDatabaseDdlRequest, Operation>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/UpdateDatabaseDdl")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(UpdateDatabaseDdlRequest.getDefaultInstance()))
              .setResponseMarshaller(ProtoUtils.marshaller(Operation.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<DropDatabaseRequest, Empty> dropDatabaseMethodDescriptor =
      MethodDescriptor.<DropDatabaseRequest, Empty>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/DropDatabase")
          .setRequestMarshaller(ProtoUtils.marshaller(DropDatabaseRequest.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
          .build();

  private static final MethodDescriptor<GetDatabaseDdlRequest, GetDatabaseDdlResponse>
      getDatabaseDdlMethodDescriptor =
          MethodDescriptor.<GetDatabaseDdlRequest, GetDatabaseDdlResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/GetDatabaseDdl")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(GetDatabaseDdlRequest.getDefaultInstance()))
              .setResponseMarshaller(
                  ProtoUtils.marshaller(GetDatabaseDdlResponse.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<SetIamPolicyRequest, Policy> setIamPolicyMethodDescriptor =
      MethodDescriptor.<SetIamPolicyRequest, Policy>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/SetIamPolicy")
          .setRequestMarshaller(ProtoUtils.marshaller(SetIamPolicyRequest.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Policy.getDefaultInstance()))
          .build();

  private static final MethodDescriptor<GetIamPolicyRequest, Policy> getIamPolicyMethodDescriptor =
      MethodDescriptor.<GetIamPolicyRequest, Policy>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/GetIamPolicy")
          .setRequestMarshaller(ProtoUtils.marshaller(GetIamPolicyRequest.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Policy.getDefaultInstance()))
          .build();

  private static final MethodDescriptor<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsMethodDescriptor =
          MethodDescriptor.<TestIamPermissionsRequest, TestIamPermissionsResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(
                  "google.spanner.admin.database.v1.DatabaseAdmin/TestIamPermissions")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(TestIamPermissionsRequest.getDefaultInstance()))
              .setResponseMarshaller(
                  ProtoUtils.marshaller(TestIamPermissionsResponse.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<CreateBackupRequest, Operation>
      createBackupMethodDescriptor =
          MethodDescriptor.<CreateBackupRequest, Operation>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/CreateBackup")
              .setRequestMarshaller(ProtoUtils.marshaller(CreateBackupRequest.getDefaultInstance()))
              .setResponseMarshaller(ProtoUtils.marshaller(Operation.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<GetBackupRequest, Backup> getBackupMethodDescriptor =
      MethodDescriptor.<GetBackupRequest, Backup>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/GetBackup")
          .setRequestMarshaller(ProtoUtils.marshaller(GetBackupRequest.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Backup.getDefaultInstance()))
          .build();

  private static final MethodDescriptor<UpdateBackupRequest, Backup> updateBackupMethodDescriptor =
      MethodDescriptor.<UpdateBackupRequest, Backup>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/UpdateBackup")
          .setRequestMarshaller(ProtoUtils.marshaller(UpdateBackupRequest.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Backup.getDefaultInstance()))
          .build();

  private static final MethodDescriptor<DeleteBackupRequest, Empty> deleteBackupMethodDescriptor =
      MethodDescriptor.<DeleteBackupRequest, Empty>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/DeleteBackup")
          .setRequestMarshaller(ProtoUtils.marshaller(DeleteBackupRequest.getDefaultInstance()))
          .setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
          .build();

  private static final MethodDescriptor<ListBackupsRequest, ListBackupsResponse>
      listBackupsMethodDescriptor =
          MethodDescriptor.<ListBackupsRequest, ListBackupsResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/ListBackups")
              .setRequestMarshaller(ProtoUtils.marshaller(ListBackupsRequest.getDefaultInstance()))
              .setResponseMarshaller(
                  ProtoUtils.marshaller(ListBackupsResponse.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<RestoreDatabaseRequest, Operation>
      restoreDatabaseMethodDescriptor =
          MethodDescriptor.<RestoreDatabaseRequest, Operation>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName("google.spanner.admin.database.v1.DatabaseAdmin/RestoreDatabase")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(RestoreDatabaseRequest.getDefaultInstance()))
              .setResponseMarshaller(ProtoUtils.marshaller(Operation.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<
          ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>
      listDatabaseOperationsMethodDescriptor =
          MethodDescriptor
              .<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(
                  "google.spanner.admin.database.v1.DatabaseAdmin/ListDatabaseOperations")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(ListDatabaseOperationsRequest.getDefaultInstance()))
              .setResponseMarshaller(
                  ProtoUtils.marshaller(ListDatabaseOperationsResponse.getDefaultInstance()))
              .build();

  private static final MethodDescriptor<ListBackupOperationsRequest, ListBackupOperationsResponse>
      listBackupOperationsMethodDescriptor =
          MethodDescriptor.<ListBackupOperationsRequest, ListBackupOperationsResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(
                  "google.spanner.admin.database.v1.DatabaseAdmin/ListBackupOperations")
              .setRequestMarshaller(
                  ProtoUtils.marshaller(ListBackupOperationsRequest.getDefaultInstance()))
              .setResponseMarshaller(
                  ProtoUtils.marshaller(ListBackupOperationsResponse.getDefaultInstance()))
              .build();

  private final UnaryCallable<ListDatabasesRequest, ListDatabasesResponse> listDatabasesCallable;
  private final UnaryCallable<ListDatabasesRequest, ListDatabasesPagedResponse>
      listDatabasesPagedCallable;
  private final UnaryCallable<CreateDatabaseRequest, Operation> createDatabaseCallable;
  private final OperationCallable<CreateDatabaseRequest, Database, CreateDatabaseMetadata>
      createDatabaseOperationCallable;
  private final UnaryCallable<GetDatabaseRequest, Database> getDatabaseCallable;
  private final UnaryCallable<UpdateDatabaseDdlRequest, Operation> updateDatabaseDdlCallable;
  private final OperationCallable<UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
      updateDatabaseDdlOperationCallable;
  private final UnaryCallable<DropDatabaseRequest, Empty> dropDatabaseCallable;
  private final UnaryCallable<GetDatabaseDdlRequest, GetDatabaseDdlResponse> getDatabaseDdlCallable;
  private final UnaryCallable<SetIamPolicyRequest, Policy> setIamPolicyCallable;
  private final UnaryCallable<GetIamPolicyRequest, Policy> getIamPolicyCallable;
  private final UnaryCallable<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsCallable;
  private final UnaryCallable<CreateBackupRequest, Operation> createBackupCallable;
  private final OperationCallable<CreateBackupRequest, Backup, CreateBackupMetadata>
      createBackupOperationCallable;
  private final UnaryCallable<GetBackupRequest, Backup> getBackupCallable;
  private final UnaryCallable<UpdateBackupRequest, Backup> updateBackupCallable;
  private final UnaryCallable<DeleteBackupRequest, Empty> deleteBackupCallable;
  private final UnaryCallable<ListBackupsRequest, ListBackupsResponse> listBackupsCallable;
  private final UnaryCallable<ListBackupsRequest, ListBackupsPagedResponse>
      listBackupsPagedCallable;
  private final UnaryCallable<RestoreDatabaseRequest, Operation> restoreDatabaseCallable;
  private final OperationCallable<RestoreDatabaseRequest, Database, RestoreDatabaseMetadata>
      restoreDatabaseOperationCallable;
  private final UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>
      listDatabaseOperationsCallable;
  private final UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsPagedResponse>
      listDatabaseOperationsPagedCallable;
  private final UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsResponse>
      listBackupOperationsCallable;
  private final UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsPagedResponse>
      listBackupOperationsPagedCallable;

  private final BackgroundResource backgroundResources;
  private final GrpcOperationsStub operationsStub;
  private final GrpcStubCallableFactory callableFactory;

  public static final GrpcDatabaseAdminStub create(DatabaseAdminStubSettings settings)
      throws IOException {
    return new GrpcDatabaseAdminStub(settings, ClientContext.create(settings));
  }

  public static final GrpcDatabaseAdminStub create(ClientContext clientContext) throws IOException {
    return new GrpcDatabaseAdminStub(DatabaseAdminStubSettings.newBuilder().build(), clientContext);
  }

  public static final GrpcDatabaseAdminStub create(
      ClientContext clientContext, GrpcStubCallableFactory callableFactory) throws IOException {
    return new GrpcDatabaseAdminStub(
        DatabaseAdminStubSettings.newBuilder().build(), clientContext, callableFactory);
  }

  /**
   * Constructs an instance of GrpcDatabaseAdminStub, using the given settings. This is protected so
   * that it is easy to make a subclass, but otherwise, the static factory methods should be
   * preferred.
   */
  protected GrpcDatabaseAdminStub(DatabaseAdminStubSettings settings, ClientContext clientContext)
      throws IOException {
    this(settings, clientContext, new GrpcDatabaseAdminCallableFactory());
  }

  /**
   * Constructs an instance of GrpcDatabaseAdminStub, using the given settings. This is protected so
   * that it is easy to make a subclass, but otherwise, the static factory methods should be
   * preferred.
   */
  protected GrpcDatabaseAdminStub(
      DatabaseAdminStubSettings settings,
      ClientContext clientContext,
      GrpcStubCallableFactory callableFactory)
      throws IOException {
    this.callableFactory = callableFactory;
    this.operationsStub = GrpcOperationsStub.create(clientContext, callableFactory);

    GrpcCallSettings<ListDatabasesRequest, ListDatabasesResponse> listDatabasesTransportSettings =
        GrpcCallSettings.<ListDatabasesRequest, ListDatabasesResponse>newBuilder()
            .setMethodDescriptor(listDatabasesMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<ListDatabasesRequest>() {
                  @Override
                  public Map<String, String> extract(ListDatabasesRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("parent", String.valueOf(request.getParent()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<CreateDatabaseRequest, Operation> createDatabaseTransportSettings =
        GrpcCallSettings.<CreateDatabaseRequest, Operation>newBuilder()
            .setMethodDescriptor(createDatabaseMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<CreateDatabaseRequest>() {
                  @Override
                  public Map<String, String> extract(CreateDatabaseRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("parent", String.valueOf(request.getParent()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<GetDatabaseRequest, Database> getDatabaseTransportSettings =
        GrpcCallSettings.<GetDatabaseRequest, Database>newBuilder()
            .setMethodDescriptor(getDatabaseMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<GetDatabaseRequest>() {
                  @Override
                  public Map<String, String> extract(GetDatabaseRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("name", String.valueOf(request.getName()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<UpdateDatabaseDdlRequest, Operation> updateDatabaseDdlTransportSettings =
        GrpcCallSettings.<UpdateDatabaseDdlRequest, Operation>newBuilder()
            .setMethodDescriptor(updateDatabaseDdlMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<UpdateDatabaseDdlRequest>() {
                  @Override
                  public Map<String, String> extract(UpdateDatabaseDdlRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("database", String.valueOf(request.getDatabase()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<DropDatabaseRequest, Empty> dropDatabaseTransportSettings =
        GrpcCallSettings.<DropDatabaseRequest, Empty>newBuilder()
            .setMethodDescriptor(dropDatabaseMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<DropDatabaseRequest>() {
                  @Override
                  public Map<String, String> extract(DropDatabaseRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("database", String.valueOf(request.getDatabase()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<GetDatabaseDdlRequest, GetDatabaseDdlResponse>
        getDatabaseDdlTransportSettings =
            GrpcCallSettings.<GetDatabaseDdlRequest, GetDatabaseDdlResponse>newBuilder()
                .setMethodDescriptor(getDatabaseDdlMethodDescriptor)
                .setParamsExtractor(
                    new RequestParamsExtractor<GetDatabaseDdlRequest>() {
                      @Override
                      public Map<String, String> extract(GetDatabaseDdlRequest request) {
                        ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                        params.put("database", String.valueOf(request.getDatabase()));
                        return params.build();
                      }
                    })
                .build();
    GrpcCallSettings<SetIamPolicyRequest, Policy> setIamPolicyTransportSettings =
        GrpcCallSettings.<SetIamPolicyRequest, Policy>newBuilder()
            .setMethodDescriptor(setIamPolicyMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<SetIamPolicyRequest>() {
                  @Override
                  public Map<String, String> extract(SetIamPolicyRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("resource", String.valueOf(request.getResource()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<GetIamPolicyRequest, Policy> getIamPolicyTransportSettings =
        GrpcCallSettings.<GetIamPolicyRequest, Policy>newBuilder()
            .setMethodDescriptor(getIamPolicyMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<GetIamPolicyRequest>() {
                  @Override
                  public Map<String, String> extract(GetIamPolicyRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("resource", String.valueOf(request.getResource()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<TestIamPermissionsRequest, TestIamPermissionsResponse>
        testIamPermissionsTransportSettings =
            GrpcCallSettings.<TestIamPermissionsRequest, TestIamPermissionsResponse>newBuilder()
                .setMethodDescriptor(testIamPermissionsMethodDescriptor)
                .setParamsExtractor(
                    new RequestParamsExtractor<TestIamPermissionsRequest>() {
                      @Override
                      public Map<String, String> extract(TestIamPermissionsRequest request) {
                        ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                        params.put("resource", String.valueOf(request.getResource()));
                        return params.build();
                      }
                    })
                .build();
    GrpcCallSettings<CreateBackupRequest, Operation> createBackupTransportSettings =
        GrpcCallSettings.<CreateBackupRequest, Operation>newBuilder()
            .setMethodDescriptor(createBackupMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<CreateBackupRequest>() {
                  @Override
                  public Map<String, String> extract(CreateBackupRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("parent", String.valueOf(request.getParent()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<GetBackupRequest, Backup> getBackupTransportSettings =
        GrpcCallSettings.<GetBackupRequest, Backup>newBuilder()
            .setMethodDescriptor(getBackupMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<GetBackupRequest>() {
                  @Override
                  public Map<String, String> extract(GetBackupRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("name", String.valueOf(request.getName()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<UpdateBackupRequest, Backup> updateBackupTransportSettings =
        GrpcCallSettings.<UpdateBackupRequest, Backup>newBuilder()
            .setMethodDescriptor(updateBackupMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<UpdateBackupRequest>() {
                  @Override
                  public Map<String, String> extract(UpdateBackupRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("backup.name", String.valueOf(request.getBackup().getName()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<DeleteBackupRequest, Empty> deleteBackupTransportSettings =
        GrpcCallSettings.<DeleteBackupRequest, Empty>newBuilder()
            .setMethodDescriptor(deleteBackupMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<DeleteBackupRequest>() {
                  @Override
                  public Map<String, String> extract(DeleteBackupRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("name", String.valueOf(request.getName()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<ListBackupsRequest, ListBackupsResponse> listBackupsTransportSettings =
        GrpcCallSettings.<ListBackupsRequest, ListBackupsResponse>newBuilder()
            .setMethodDescriptor(listBackupsMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<ListBackupsRequest>() {
                  @Override
                  public Map<String, String> extract(ListBackupsRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("parent", String.valueOf(request.getParent()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<RestoreDatabaseRequest, Operation> restoreDatabaseTransportSettings =
        GrpcCallSettings.<RestoreDatabaseRequest, Operation>newBuilder()
            .setMethodDescriptor(restoreDatabaseMethodDescriptor)
            .setParamsExtractor(
                new RequestParamsExtractor<RestoreDatabaseRequest>() {
                  @Override
                  public Map<String, String> extract(RestoreDatabaseRequest request) {
                    ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                    params.put("parent", String.valueOf(request.getParent()));
                    return params.build();
                  }
                })
            .build();
    GrpcCallSettings<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>
        listDatabaseOperationsTransportSettings =
            GrpcCallSettings
                .<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>newBuilder()
                .setMethodDescriptor(listDatabaseOperationsMethodDescriptor)
                .setParamsExtractor(
                    new RequestParamsExtractor<ListDatabaseOperationsRequest>() {
                      @Override
                      public Map<String, String> extract(ListDatabaseOperationsRequest request) {
                        ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                        params.put("parent", String.valueOf(request.getParent()));
                        return params.build();
                      }
                    })
                .build();
    GrpcCallSettings<ListBackupOperationsRequest, ListBackupOperationsResponse>
        listBackupOperationsTransportSettings =
            GrpcCallSettings.<ListBackupOperationsRequest, ListBackupOperationsResponse>newBuilder()
                .setMethodDescriptor(listBackupOperationsMethodDescriptor)
                .setParamsExtractor(
                    new RequestParamsExtractor<ListBackupOperationsRequest>() {
                      @Override
                      public Map<String, String> extract(ListBackupOperationsRequest request) {
                        ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
                        params.put("parent", String.valueOf(request.getParent()));
                        return params.build();
                      }
                    })
                .build();

    this.listDatabasesCallable =
        callableFactory.createUnaryCallable(
            listDatabasesTransportSettings, settings.listDatabasesSettings(), clientContext);
    this.listDatabasesPagedCallable =
        callableFactory.createPagedCallable(
            listDatabasesTransportSettings, settings.listDatabasesSettings(), clientContext);
    this.createDatabaseCallable =
        callableFactory.createUnaryCallable(
            createDatabaseTransportSettings, settings.createDatabaseSettings(), clientContext);
    this.createDatabaseOperationCallable =
        callableFactory.createOperationCallable(
            createDatabaseTransportSettings,
            settings.createDatabaseOperationSettings(),
            clientContext,
            operationsStub);
    this.getDatabaseCallable =
        callableFactory.createUnaryCallable(
            getDatabaseTransportSettings, settings.getDatabaseSettings(), clientContext);
    this.updateDatabaseDdlCallable =
        callableFactory.createUnaryCallable(
            updateDatabaseDdlTransportSettings,
            settings.updateDatabaseDdlSettings(),
            clientContext);
    this.updateDatabaseDdlOperationCallable =
        callableFactory.createOperationCallable(
            updateDatabaseDdlTransportSettings,
            settings.updateDatabaseDdlOperationSettings(),
            clientContext,
            operationsStub);
    this.dropDatabaseCallable =
        callableFactory.createUnaryCallable(
            dropDatabaseTransportSettings, settings.dropDatabaseSettings(), clientContext);
    this.getDatabaseDdlCallable =
        callableFactory.createUnaryCallable(
            getDatabaseDdlTransportSettings, settings.getDatabaseDdlSettings(), clientContext);
    this.setIamPolicyCallable =
        callableFactory.createUnaryCallable(
            setIamPolicyTransportSettings, settings.setIamPolicySettings(), clientContext);
    this.getIamPolicyCallable =
        callableFactory.createUnaryCallable(
            getIamPolicyTransportSettings, settings.getIamPolicySettings(), clientContext);
    this.testIamPermissionsCallable =
        callableFactory.createUnaryCallable(
            testIamPermissionsTransportSettings,
            settings.testIamPermissionsSettings(),
            clientContext);
    this.createBackupCallable =
        callableFactory.createUnaryCallable(
            createBackupTransportSettings, settings.createBackupSettings(), clientContext);
    this.createBackupOperationCallable =
        callableFactory.createOperationCallable(
            createBackupTransportSettings,
            settings.createBackupOperationSettings(),
            clientContext,
            operationsStub);
    this.getBackupCallable =
        callableFactory.createUnaryCallable(
            getBackupTransportSettings, settings.getBackupSettings(), clientContext);
    this.updateBackupCallable =
        callableFactory.createUnaryCallable(
            updateBackupTransportSettings, settings.updateBackupSettings(), clientContext);
    this.deleteBackupCallable =
        callableFactory.createUnaryCallable(
            deleteBackupTransportSettings, settings.deleteBackupSettings(), clientContext);
    this.listBackupsCallable =
        callableFactory.createUnaryCallable(
            listBackupsTransportSettings, settings.listBackupsSettings(), clientContext);
    this.listBackupsPagedCallable =
        callableFactory.createPagedCallable(
            listBackupsTransportSettings, settings.listBackupsSettings(), clientContext);
    this.restoreDatabaseCallable =
        callableFactory.createUnaryCallable(
            restoreDatabaseTransportSettings, settings.restoreDatabaseSettings(), clientContext);
    this.restoreDatabaseOperationCallable =
        callableFactory.createOperationCallable(
            restoreDatabaseTransportSettings,
            settings.restoreDatabaseOperationSettings(),
            clientContext,
            operationsStub);
    this.listDatabaseOperationsCallable =
        callableFactory.createUnaryCallable(
            listDatabaseOperationsTransportSettings,
            settings.listDatabaseOperationsSettings(),
            clientContext);
    this.listDatabaseOperationsPagedCallable =
        callableFactory.createPagedCallable(
            listDatabaseOperationsTransportSettings,
            settings.listDatabaseOperationsSettings(),
            clientContext);
    this.listBackupOperationsCallable =
        callableFactory.createUnaryCallable(
            listBackupOperationsTransportSettings,
            settings.listBackupOperationsSettings(),
            clientContext);
    this.listBackupOperationsPagedCallable =
        callableFactory.createPagedCallable(
            listBackupOperationsTransportSettings,
            settings.listBackupOperationsSettings(),
            clientContext);

    this.backgroundResources =
        new BackgroundResourceAggregation(clientContext.getBackgroundResources());
  }

  public GrpcOperationsStub getOperationsStub() {
    return operationsStub;
  }

  public UnaryCallable<ListDatabasesRequest, ListDatabasesResponse> listDatabasesCallable() {
    return listDatabasesCallable;
  }

  public UnaryCallable<ListDatabasesRequest, ListDatabasesPagedResponse>
      listDatabasesPagedCallable() {
    return listDatabasesPagedCallable;
  }

  public UnaryCallable<CreateDatabaseRequest, Operation> createDatabaseCallable() {
    return createDatabaseCallable;
  }

  public OperationCallable<CreateDatabaseRequest, Database, CreateDatabaseMetadata>
      createDatabaseOperationCallable() {
    return createDatabaseOperationCallable;
  }

  public UnaryCallable<GetDatabaseRequest, Database> getDatabaseCallable() {
    return getDatabaseCallable;
  }

  public UnaryCallable<UpdateDatabaseDdlRequest, Operation> updateDatabaseDdlCallable() {
    return updateDatabaseDdlCallable;
  }

  public OperationCallable<UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
      updateDatabaseDdlOperationCallable() {
    return updateDatabaseDdlOperationCallable;
  }

  public UnaryCallable<DropDatabaseRequest, Empty> dropDatabaseCallable() {
    return dropDatabaseCallable;
  }

  public UnaryCallable<GetDatabaseDdlRequest, GetDatabaseDdlResponse> getDatabaseDdlCallable() {
    return getDatabaseDdlCallable;
  }

  public UnaryCallable<SetIamPolicyRequest, Policy> setIamPolicyCallable() {
    return setIamPolicyCallable;
  }

  public UnaryCallable<GetIamPolicyRequest, Policy> getIamPolicyCallable() {
    return getIamPolicyCallable;
  }

  public UnaryCallable<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsCallable() {
    return testIamPermissionsCallable;
  }

  public UnaryCallable<CreateBackupRequest, Operation> createBackupCallable() {
    return createBackupCallable;
  }

  public OperationCallable<CreateBackupRequest, Backup, CreateBackupMetadata>
      createBackupOperationCallable() {
    return createBackupOperationCallable;
  }

  public UnaryCallable<GetBackupRequest, Backup> getBackupCallable() {
    return getBackupCallable;
  }

  public UnaryCallable<UpdateBackupRequest, Backup> updateBackupCallable() {
    return updateBackupCallable;
  }

  public UnaryCallable<DeleteBackupRequest, Empty> deleteBackupCallable() {
    return deleteBackupCallable;
  }

  public UnaryCallable<ListBackupsRequest, ListBackupsResponse> listBackupsCallable() {
    return listBackupsCallable;
  }

  public UnaryCallable<ListBackupsRequest, ListBackupsPagedResponse> listBackupsPagedCallable() {
    return listBackupsPagedCallable;
  }

  public UnaryCallable<RestoreDatabaseRequest, Operation> restoreDatabaseCallable() {
    return restoreDatabaseCallable;
  }

  public OperationCallable<RestoreDatabaseRequest, Database, RestoreDatabaseMetadata>
      restoreDatabaseOperationCallable() {
    return restoreDatabaseOperationCallable;
  }

  public UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>
      listDatabaseOperationsCallable() {
    return listDatabaseOperationsCallable;
  }

  public UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsPagedResponse>
      listDatabaseOperationsPagedCallable() {
    return listDatabaseOperationsPagedCallable;
  }

  public UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsResponse>
      listBackupOperationsCallable() {
    return listBackupOperationsCallable;
  }

  public UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsPagedResponse>
      listBackupOperationsPagedCallable() {
    return listBackupOperationsPagedCallable;
  }

  @Override
  public final void close() {
    shutdown();
  }

  @Override
  public void shutdown() {
    backgroundResources.shutdown();
  }

  @Override
  public boolean isShutdown() {
    return backgroundResources.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return backgroundResources.isTerminated();
  }

  @Override
  public void shutdownNow() {
    backgroundResources.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long duration, TimeUnit unit) throws InterruptedException {
    return backgroundResources.awaitTermination(duration, unit);
  }
}
