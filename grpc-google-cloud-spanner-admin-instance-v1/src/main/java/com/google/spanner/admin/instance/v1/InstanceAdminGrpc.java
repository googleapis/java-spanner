/*
 * Copyright 2023 Google LLC
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
package com.google.spanner.admin.instance.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 *
 *
 * <pre>
 * Cloud Spanner Instance Admin API
 * The Cloud Spanner Instance Admin API can be used to create, delete,
 * modify and list instances. Instances are dedicated Cloud Spanner serving
 * and storage resources to be used by Cloud Spanner databases.
 * Each instance has a "configuration", which dictates where the
 * serving resources for the Cloud Spanner instance are located (e.g.,
 * US-central, Europe). Configurations are created by Google based on
 * resource availability.
 * Cloud Spanner billing is based on the instances that exist and their
 * sizes. After an instance exists, there are no additional
 * per-database or per-operation charges for use of the instance
 * (though there may be additional network bandwidth charges).
 * Instances offer isolation: problems with databases in one instance
 * will not affect other instances. However, within an instance
 * databases can affect each other. For example, if one database in an
 * instance receives a lot of requests and consumes most of the
 * instance resources, fewer resources are available for other
 * databases in that instance, and their performance may suffer.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: google/spanner/admin/instance/v1/spanner_instance_admin.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class InstanceAdminGrpc {

  private InstanceAdminGrpc() {}

  public static final java.lang.String SERVICE_NAME =
      "google.spanner.admin.instance.v1.InstanceAdmin";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest,
          com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
      getListInstanceConfigsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListInstanceConfigs",
      requestType = com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest.class,
      responseType = com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest,
          com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
      getListInstanceConfigsMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest,
            com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
        getListInstanceConfigsMethod;
    if ((getListInstanceConfigsMethod = InstanceAdminGrpc.getListInstanceConfigsMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getListInstanceConfigsMethod = InstanceAdminGrpc.getListInstanceConfigsMethod)
            == null) {
          InstanceAdminGrpc.getListInstanceConfigsMethod =
              getListInstanceConfigsMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest,
                          com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(
                          generateFullMethodName(SERVICE_NAME, "ListInstanceConfigs"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("ListInstanceConfigs"))
                      .build();
        }
      }
    }
    return getListInstanceConfigsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.GetInstanceConfigRequest,
          com.google.spanner.admin.instance.v1.InstanceConfig>
      getGetInstanceConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetInstanceConfig",
      requestType = com.google.spanner.admin.instance.v1.GetInstanceConfigRequest.class,
      responseType = com.google.spanner.admin.instance.v1.InstanceConfig.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.GetInstanceConfigRequest,
          com.google.spanner.admin.instance.v1.InstanceConfig>
      getGetInstanceConfigMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.GetInstanceConfigRequest,
            com.google.spanner.admin.instance.v1.InstanceConfig>
        getGetInstanceConfigMethod;
    if ((getGetInstanceConfigMethod = InstanceAdminGrpc.getGetInstanceConfigMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getGetInstanceConfigMethod = InstanceAdminGrpc.getGetInstanceConfigMethod) == null) {
          InstanceAdminGrpc.getGetInstanceConfigMethod =
              getGetInstanceConfigMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.GetInstanceConfigRequest,
                          com.google.spanner.admin.instance.v1.InstanceConfig>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetInstanceConfig"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.GetInstanceConfigRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.InstanceConfig
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("GetInstanceConfig"))
                      .build();
        }
      }
    }
    return getGetInstanceConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest,
          com.google.longrunning.Operation>
      getCreateInstanceConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateInstanceConfig",
      requestType = com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest,
          com.google.longrunning.Operation>
      getCreateInstanceConfigMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest,
            com.google.longrunning.Operation>
        getCreateInstanceConfigMethod;
    if ((getCreateInstanceConfigMethod = InstanceAdminGrpc.getCreateInstanceConfigMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getCreateInstanceConfigMethod = InstanceAdminGrpc.getCreateInstanceConfigMethod)
            == null) {
          InstanceAdminGrpc.getCreateInstanceConfigMethod =
              getCreateInstanceConfigMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(
                          generateFullMethodName(SERVICE_NAME, "CreateInstanceConfig"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("CreateInstanceConfig"))
                      .build();
        }
      }
    }
    return getCreateInstanceConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest,
          com.google.longrunning.Operation>
      getUpdateInstanceConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateInstanceConfig",
      requestType = com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest,
          com.google.longrunning.Operation>
      getUpdateInstanceConfigMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest,
            com.google.longrunning.Operation>
        getUpdateInstanceConfigMethod;
    if ((getUpdateInstanceConfigMethod = InstanceAdminGrpc.getUpdateInstanceConfigMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getUpdateInstanceConfigMethod = InstanceAdminGrpc.getUpdateInstanceConfigMethod)
            == null) {
          InstanceAdminGrpc.getUpdateInstanceConfigMethod =
              getUpdateInstanceConfigMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(
                          generateFullMethodName(SERVICE_NAME, "UpdateInstanceConfig"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("UpdateInstanceConfig"))
                      .build();
        }
      }
    }
    return getUpdateInstanceConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest,
          com.google.protobuf.Empty>
      getDeleteInstanceConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteInstanceConfig",
      requestType = com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest,
          com.google.protobuf.Empty>
      getDeleteInstanceConfigMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest,
            com.google.protobuf.Empty>
        getDeleteInstanceConfigMethod;
    if ((getDeleteInstanceConfigMethod = InstanceAdminGrpc.getDeleteInstanceConfigMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getDeleteInstanceConfigMethod = InstanceAdminGrpc.getDeleteInstanceConfigMethod)
            == null) {
          InstanceAdminGrpc.getDeleteInstanceConfigMethod =
              getDeleteInstanceConfigMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest,
                          com.google.protobuf.Empty>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(
                          generateFullMethodName(SERVICE_NAME, "DeleteInstanceConfig"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.protobuf.Empty.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("DeleteInstanceConfig"))
                      .build();
        }
      }
    }
    return getDeleteInstanceConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest,
          com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>
      getListInstanceConfigOperationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListInstanceConfigOperations",
      requestType = com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest.class,
      responseType =
          com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest,
          com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>
      getListInstanceConfigOperationsMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest,
            com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>
        getListInstanceConfigOperationsMethod;
    if ((getListInstanceConfigOperationsMethod =
            InstanceAdminGrpc.getListInstanceConfigOperationsMethod)
        == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getListInstanceConfigOperationsMethod =
                InstanceAdminGrpc.getListInstanceConfigOperationsMethod)
            == null) {
          InstanceAdminGrpc.getListInstanceConfigOperationsMethod =
              getListInstanceConfigOperationsMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest,
                          com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(
                          generateFullMethodName(SERVICE_NAME, "ListInstanceConfigOperations"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1
                                  .ListInstanceConfigOperationsRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1
                                  .ListInstanceConfigOperationsResponse.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("ListInstanceConfigOperations"))
                      .build();
        }
      }
    }
    return getListInstanceConfigOperationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.ListInstancesRequest,
          com.google.spanner.admin.instance.v1.ListInstancesResponse>
      getListInstancesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListInstances",
      requestType = com.google.spanner.admin.instance.v1.ListInstancesRequest.class,
      responseType = com.google.spanner.admin.instance.v1.ListInstancesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.ListInstancesRequest,
          com.google.spanner.admin.instance.v1.ListInstancesResponse>
      getListInstancesMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.ListInstancesRequest,
            com.google.spanner.admin.instance.v1.ListInstancesResponse>
        getListInstancesMethod;
    if ((getListInstancesMethod = InstanceAdminGrpc.getListInstancesMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getListInstancesMethod = InstanceAdminGrpc.getListInstancesMethod) == null) {
          InstanceAdminGrpc.getListInstancesMethod =
              getListInstancesMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.ListInstancesRequest,
                          com.google.spanner.admin.instance.v1.ListInstancesResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListInstances"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.ListInstancesRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.ListInstancesResponse
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("ListInstances"))
                      .build();
        }
      }
    }
    return getListInstancesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.GetInstanceRequest,
          com.google.spanner.admin.instance.v1.Instance>
      getGetInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetInstance",
      requestType = com.google.spanner.admin.instance.v1.GetInstanceRequest.class,
      responseType = com.google.spanner.admin.instance.v1.Instance.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.GetInstanceRequest,
          com.google.spanner.admin.instance.v1.Instance>
      getGetInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.GetInstanceRequest,
            com.google.spanner.admin.instance.v1.Instance>
        getGetInstanceMethod;
    if ((getGetInstanceMethod = InstanceAdminGrpc.getGetInstanceMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getGetInstanceMethod = InstanceAdminGrpc.getGetInstanceMethod) == null) {
          InstanceAdminGrpc.getGetInstanceMethod =
              getGetInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.GetInstanceRequest,
                          com.google.spanner.admin.instance.v1.Instance>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.GetInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.Instance.getDefaultInstance()))
                      .setSchemaDescriptor(new InstanceAdminMethodDescriptorSupplier("GetInstance"))
                      .build();
        }
      }
    }
    return getGetInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.CreateInstanceRequest,
          com.google.longrunning.Operation>
      getCreateInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateInstance",
      requestType = com.google.spanner.admin.instance.v1.CreateInstanceRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.CreateInstanceRequest,
          com.google.longrunning.Operation>
      getCreateInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.CreateInstanceRequest,
            com.google.longrunning.Operation>
        getCreateInstanceMethod;
    if ((getCreateInstanceMethod = InstanceAdminGrpc.getCreateInstanceMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getCreateInstanceMethod = InstanceAdminGrpc.getCreateInstanceMethod) == null) {
          InstanceAdminGrpc.getCreateInstanceMethod =
              getCreateInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.CreateInstanceRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.CreateInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("CreateInstance"))
                      .build();
        }
      }
    }
    return getCreateInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.UpdateInstanceRequest,
          com.google.longrunning.Operation>
      getUpdateInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateInstance",
      requestType = com.google.spanner.admin.instance.v1.UpdateInstanceRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.UpdateInstanceRequest,
          com.google.longrunning.Operation>
      getUpdateInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.UpdateInstanceRequest,
            com.google.longrunning.Operation>
        getUpdateInstanceMethod;
    if ((getUpdateInstanceMethod = InstanceAdminGrpc.getUpdateInstanceMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getUpdateInstanceMethod = InstanceAdminGrpc.getUpdateInstanceMethod) == null) {
          InstanceAdminGrpc.getUpdateInstanceMethod =
              getUpdateInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.UpdateInstanceRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.UpdateInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("UpdateInstance"))
                      .build();
        }
      }
    }
    return getUpdateInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.DeleteInstanceRequest, com.google.protobuf.Empty>
      getDeleteInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteInstance",
      requestType = com.google.spanner.admin.instance.v1.DeleteInstanceRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.admin.instance.v1.DeleteInstanceRequest, com.google.protobuf.Empty>
      getDeleteInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.admin.instance.v1.DeleteInstanceRequest, com.google.protobuf.Empty>
        getDeleteInstanceMethod;
    if ((getDeleteInstanceMethod = InstanceAdminGrpc.getDeleteInstanceMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getDeleteInstanceMethod = InstanceAdminGrpc.getDeleteInstanceMethod) == null) {
          InstanceAdminGrpc.getDeleteInstanceMethod =
              getDeleteInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.admin.instance.v1.DeleteInstanceRequest,
                          com.google.protobuf.Empty>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.admin.instance.v1.DeleteInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.protobuf.Empty.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("DeleteInstance"))
                      .build();
        }
      }
    }
    return getDeleteInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.iam.v1.SetIamPolicyRequest, com.google.iam.v1.Policy>
      getSetIamPolicyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SetIamPolicy",
      requestType = com.google.iam.v1.SetIamPolicyRequest.class,
      responseType = com.google.iam.v1.Policy.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.iam.v1.SetIamPolicyRequest, com.google.iam.v1.Policy>
      getSetIamPolicyMethod() {
    io.grpc.MethodDescriptor<com.google.iam.v1.SetIamPolicyRequest, com.google.iam.v1.Policy>
        getSetIamPolicyMethod;
    if ((getSetIamPolicyMethod = InstanceAdminGrpc.getSetIamPolicyMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getSetIamPolicyMethod = InstanceAdminGrpc.getSetIamPolicyMethod) == null) {
          InstanceAdminGrpc.getSetIamPolicyMethod =
              getSetIamPolicyMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.iam.v1.SetIamPolicyRequest, com.google.iam.v1.Policy>newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SetIamPolicy"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.iam.v1.SetIamPolicyRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.iam.v1.Policy.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("SetIamPolicy"))
                      .build();
        }
      }
    }
    return getSetIamPolicyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.iam.v1.GetIamPolicyRequest, com.google.iam.v1.Policy>
      getGetIamPolicyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetIamPolicy",
      requestType = com.google.iam.v1.GetIamPolicyRequest.class,
      responseType = com.google.iam.v1.Policy.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.iam.v1.GetIamPolicyRequest, com.google.iam.v1.Policy>
      getGetIamPolicyMethod() {
    io.grpc.MethodDescriptor<com.google.iam.v1.GetIamPolicyRequest, com.google.iam.v1.Policy>
        getGetIamPolicyMethod;
    if ((getGetIamPolicyMethod = InstanceAdminGrpc.getGetIamPolicyMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getGetIamPolicyMethod = InstanceAdminGrpc.getGetIamPolicyMethod) == null) {
          InstanceAdminGrpc.getGetIamPolicyMethod =
              getGetIamPolicyMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.iam.v1.GetIamPolicyRequest, com.google.iam.v1.Policy>newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetIamPolicy"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.iam.v1.GetIamPolicyRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.iam.v1.Policy.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("GetIamPolicy"))
                      .build();
        }
      }
    }
    return getGetIamPolicyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.iam.v1.TestIamPermissionsRequest, com.google.iam.v1.TestIamPermissionsResponse>
      getTestIamPermissionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "TestIamPermissions",
      requestType = com.google.iam.v1.TestIamPermissionsRequest.class,
      responseType = com.google.iam.v1.TestIamPermissionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.iam.v1.TestIamPermissionsRequest, com.google.iam.v1.TestIamPermissionsResponse>
      getTestIamPermissionsMethod() {
    io.grpc.MethodDescriptor<
            com.google.iam.v1.TestIamPermissionsRequest,
            com.google.iam.v1.TestIamPermissionsResponse>
        getTestIamPermissionsMethod;
    if ((getTestIamPermissionsMethod = InstanceAdminGrpc.getTestIamPermissionsMethod) == null) {
      synchronized (InstanceAdminGrpc.class) {
        if ((getTestIamPermissionsMethod = InstanceAdminGrpc.getTestIamPermissionsMethod) == null) {
          InstanceAdminGrpc.getTestIamPermissionsMethod =
              getTestIamPermissionsMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.iam.v1.TestIamPermissionsRequest,
                          com.google.iam.v1.TestIamPermissionsResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TestIamPermissions"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.iam.v1.TestIamPermissionsRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.iam.v1.TestIamPermissionsResponse.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new InstanceAdminMethodDescriptorSupplier("TestIamPermissions"))
                      .build();
        }
      }
    }
    return getTestIamPermissionsMethod;
  }

  /** Creates a new async stub that supports all call types for the service */
  public static InstanceAdminStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<InstanceAdminStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<InstanceAdminStub>() {
          @java.lang.Override
          public InstanceAdminStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new InstanceAdminStub(channel, callOptions);
          }
        };
    return InstanceAdminStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static InstanceAdminBlockingStub newBlockingStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<InstanceAdminBlockingStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<InstanceAdminBlockingStub>() {
          @java.lang.Override
          public InstanceAdminBlockingStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new InstanceAdminBlockingStub(channel, callOptions);
          }
        };
    return InstanceAdminBlockingStub.newStub(factory, channel);
  }

  /** Creates a new ListenableFuture-style stub that supports unary calls on the service */
  public static InstanceAdminFutureStub newFutureStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<InstanceAdminFutureStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<InstanceAdminFutureStub>() {
          @java.lang.Override
          public InstanceAdminFutureStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new InstanceAdminFutureStub(channel, callOptions);
          }
        };
    return InstanceAdminFutureStub.newStub(factory, channel);
  }

  /**
   *
   *
   * <pre>
   * Cloud Spanner Instance Admin API
   * The Cloud Spanner Instance Admin API can be used to create, delete,
   * modify and list instances. Instances are dedicated Cloud Spanner serving
   * and storage resources to be used by Cloud Spanner databases.
   * Each instance has a "configuration", which dictates where the
   * serving resources for the Cloud Spanner instance are located (e.g.,
   * US-central, Europe). Configurations are created by Google based on
   * resource availability.
   * Cloud Spanner billing is based on the instances that exist and their
   * sizes. After an instance exists, there are no additional
   * per-database or per-operation charges for use of the instance
   * (though there may be additional network bandwidth charges).
   * Instances offer isolation: problems with databases in one instance
   * will not affect other instances. However, within an instance
   * databases can affect each other. For example, if one database in an
   * instance receives a lot of requests and consumes most of the
   * instance resources, fewer resources are available for other
   * databases in that instance, and their performance may suffer.
   * </pre>
   */
  public interface AsyncService {

    /**
     *
     *
     * <pre>
     * Lists the supported instance configurations for a given project.
     * </pre>
     */
    default void listInstanceConfigs(
        com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request,
        io.grpc.stub.StreamObserver<
                com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getListInstanceConfigsMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance configuration.
     * </pre>
     */
    default void getInstanceConfig(
        com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.InstanceConfig>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getGetInstanceConfigMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance config and begins preparing it to be used. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance config. The instance config name is assigned by the caller. If the
     * named instance config already exists, `CreateInstanceConfig` returns
     * `ALREADY_EXISTS`.
     * Immediately after the request returns:
     *   * The instance config is readable via the API, with all requested
     *     attributes. The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true. Its state is `CREATING`.
     * While the operation is pending:
     *   * Cancelling the operation renders the instance config immediately
     *     unreadable via the API.
     *   * Except for deleting the creating resource, all other attempts to modify
     *     the instance config are rejected.
     * Upon completion of the returned operation:
     *   * Instances can be created using the instance configuration.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false. Its state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * creation of the instance config. The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.create` permission on
     * the resource
     * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
     * </pre>
     */
    default void createInstanceConfig(
        com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getCreateInstanceConfigMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance config. The returned
     * [long-running operation][google.longrunning.Operation] can be used to track
     * the progress of updating the instance. If the named instance config does
     * not exist, returns `NOT_FOUND`.
     * Only user managed configurations can be updated.
     * Immediately after the request returns:
     *   * The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true.
     * While the operation is pending:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time].
     *     The operation is guaranteed to succeed at undoing all changes, after
     *     which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance config are rejected.
     *   * Reading the instance config via the API continues to give the
     *     pre-request values.
     * Upon completion of the returned operation:
     *   * Creating instances using the instance configuration uses the new
     *     values.
     *   * The instance config's new values are readable via the API.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * the instance config modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    default void updateInstanceConfig(
        com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getUpdateInstanceConfigMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes the instance config. Deletion is only allowed when no
     * instances are using the configuration. If any instances are using
     * the config, returns `FAILED_PRECONDITION`.
     * Only user managed configurations can be deleted.
     * Authorization requires `spanner.instanceConfigs.delete` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    default void deleteInstanceConfig(
        com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getDeleteInstanceConfigMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists the user-managed instance config [long-running
     * operations][google.longrunning.Operation] in the given project. An instance
     * config operation has a name of the form
     * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
     * The long-running operation
     * [metadata][google.longrunning.Operation.metadata] field type
     * `metadata.type_url` describes the type of the metadata. Operations returned
     * include those that have completed/failed/canceled within the last 7 days,
     * and pending operations. Operations returned are ordered by
     * `operation.metadata.value.start_time` in descending order starting
     * from the most recently started operation.
     * </pre>
     */
    default void listInstanceConfigOperations(
        com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest request,
        io.grpc.stub.StreamObserver<
                com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getListInstanceConfigOperationsMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all instances in the given project.
     * </pre>
     */
    default void listInstances(
        com.google.spanner.admin.instance.v1.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.ListInstancesResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getListInstancesMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance.
     * </pre>
     */
    default void getInstance(
        com.google.spanner.admin.instance.v1.GetInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.Instance>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getGetInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance and begins preparing it to begin serving. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance. The instance name is assigned by the caller. If the
     * named instance already exists, `CreateInstance` returns
     * `ALREADY_EXISTS`.
     * Immediately upon completion of this request:
     *   * The instance is readable via the API, with all requested attributes
     *     but no allocated resources. Its state is `CREATING`.
     * Until completion of the returned operation:
     *   * Cancelling the operation renders the instance immediately unreadable
     *     via the API.
     *   * The instance can be deleted.
     *   * All other attempts to modify the instance are rejected.
     * Upon completion of the returned operation:
     *   * Billing for all successfully-allocated resources begins (some types
     *     may have lower than the requested levels).
     *   * Databases can be created in the instance.
     *   * The instance's allocated resource levels are readable via the API.
     *   * The instance's state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track creation of the instance.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * </pre>
     */
    default void createInstance(
        com.google.spanner.admin.instance.v1.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getCreateInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance, and begins allocating or releasing resources
     * as requested. The returned [long-running
     * operation][google.longrunning.Operation] can be used to track the
     * progress of updating the instance. If the named instance does not
     * exist, returns `NOT_FOUND`.
     * Immediately upon completion of this request:
     *   * For resource types for which a decrease in the instance's allocation
     *     has been requested, billing is based on the newly-requested level.
     * Until completion of the returned operation:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time],
     *     and begins restoring resources to their pre-request values. The
     *     operation is guaranteed to succeed at undoing all resource changes,
     *     after which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance are rejected.
     *   * Reading the instance via the API continues to give the pre-request
     *     resource levels.
     * Upon completion of the returned operation:
     *   * Billing begins for all successfully-allocated resources (some types
     *     may have lower than the requested levels).
     *   * All newly-reserved resources are available for serving the instance's
     *     tables.
     *   * The instance's new resource levels are readable via the API.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track the instance modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * Authorization requires `spanner.instances.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.Instance.name].
     * </pre>
     */
    default void updateInstance(
        com.google.spanner.admin.instance.v1.UpdateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getUpdateInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * Immediately upon completion of the request:
     *   * Billing ceases for all of the instance's reserved resources.
     * Soon afterward:
     *   * The instance and *all of its databases* immediately and
     *     irrevocably disappear from the API. All data in the databases
     *     is permanently deleted.
     * </pre>
     */
    default void deleteInstance(
        com.google.spanner.admin.instance.v1.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getDeleteInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Sets the access control policy on an instance resource. Replaces any
     * existing policy.
     * Authorization requires `spanner.instances.setIamPolicy` on
     * [resource][google.iam.v1.SetIamPolicyRequest.resource].
     * </pre>
     */
    default void setIamPolicy(
        com.google.iam.v1.SetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getSetIamPolicyMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the access control policy for an instance resource. Returns an empty
     * policy if an instance exists but does not have a policy set.
     * Authorization requires `spanner.instances.getIamPolicy` on
     * [resource][google.iam.v1.GetIamPolicyRequest.resource].
     * </pre>
     */
    default void getIamPolicy(
        com.google.iam.v1.GetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getGetIamPolicyMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Returns permissions that the caller has on the specified instance resource.
     * Attempting this RPC on a non-existent Cloud Spanner instance resource will
     * result in a NOT_FOUND error if the user has `spanner.instances.list`
     * permission on the containing Google Cloud Project. Otherwise returns an
     * empty set of permissions.
     * </pre>
     */
    default void testIamPermissions(
        com.google.iam.v1.TestIamPermissionsRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.TestIamPermissionsResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getTestIamPermissionsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service InstanceAdmin.
   *
   * <pre>
   * Cloud Spanner Instance Admin API
   * The Cloud Spanner Instance Admin API can be used to create, delete,
   * modify and list instances. Instances are dedicated Cloud Spanner serving
   * and storage resources to be used by Cloud Spanner databases.
   * Each instance has a "configuration", which dictates where the
   * serving resources for the Cloud Spanner instance are located (e.g.,
   * US-central, Europe). Configurations are created by Google based on
   * resource availability.
   * Cloud Spanner billing is based on the instances that exist and their
   * sizes. After an instance exists, there are no additional
   * per-database or per-operation charges for use of the instance
   * (though there may be additional network bandwidth charges).
   * Instances offer isolation: problems with databases in one instance
   * will not affect other instances. However, within an instance
   * databases can affect each other. For example, if one database in an
   * instance receives a lot of requests and consumes most of the
   * instance resources, fewer resources are available for other
   * databases in that instance, and their performance may suffer.
   * </pre>
   */
  public abstract static class InstanceAdminImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return InstanceAdminGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service InstanceAdmin.
   *
   * <pre>
   * Cloud Spanner Instance Admin API
   * The Cloud Spanner Instance Admin API can be used to create, delete,
   * modify and list instances. Instances are dedicated Cloud Spanner serving
   * and storage resources to be used by Cloud Spanner databases.
   * Each instance has a "configuration", which dictates where the
   * serving resources for the Cloud Spanner instance are located (e.g.,
   * US-central, Europe). Configurations are created by Google based on
   * resource availability.
   * Cloud Spanner billing is based on the instances that exist and their
   * sizes. After an instance exists, there are no additional
   * per-database or per-operation charges for use of the instance
   * (though there may be additional network bandwidth charges).
   * Instances offer isolation: problems with databases in one instance
   * will not affect other instances. However, within an instance
   * databases can affect each other. For example, if one database in an
   * instance receives a lot of requests and consumes most of the
   * instance resources, fewer resources are available for other
   * databases in that instance, and their performance may suffer.
   * </pre>
   */
  public static final class InstanceAdminStub
      extends io.grpc.stub.AbstractAsyncStub<InstanceAdminStub> {
    private InstanceAdminStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InstanceAdminStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new InstanceAdminStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Lists the supported instance configurations for a given project.
     * </pre>
     */
    public void listInstanceConfigs(
        com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request,
        io.grpc.stub.StreamObserver<
                com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListInstanceConfigsMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance configuration.
     * </pre>
     */
    public void getInstanceConfig(
        com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.InstanceConfig>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetInstanceConfigMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance config and begins preparing it to be used. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance config. The instance config name is assigned by the caller. If the
     * named instance config already exists, `CreateInstanceConfig` returns
     * `ALREADY_EXISTS`.
     * Immediately after the request returns:
     *   * The instance config is readable via the API, with all requested
     *     attributes. The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true. Its state is `CREATING`.
     * While the operation is pending:
     *   * Cancelling the operation renders the instance config immediately
     *     unreadable via the API.
     *   * Except for deleting the creating resource, all other attempts to modify
     *     the instance config are rejected.
     * Upon completion of the returned operation:
     *   * Instances can be created using the instance configuration.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false. Its state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * creation of the instance config. The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.create` permission on
     * the resource
     * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
     * </pre>
     */
    public void createInstanceConfig(
        com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateInstanceConfigMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance config. The returned
     * [long-running operation][google.longrunning.Operation] can be used to track
     * the progress of updating the instance. If the named instance config does
     * not exist, returns `NOT_FOUND`.
     * Only user managed configurations can be updated.
     * Immediately after the request returns:
     *   * The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true.
     * While the operation is pending:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time].
     *     The operation is guaranteed to succeed at undoing all changes, after
     *     which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance config are rejected.
     *   * Reading the instance config via the API continues to give the
     *     pre-request values.
     * Upon completion of the returned operation:
     *   * Creating instances using the instance configuration uses the new
     *     values.
     *   * The instance config's new values are readable via the API.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * the instance config modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    public void updateInstanceConfig(
        com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateInstanceConfigMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes the instance config. Deletion is only allowed when no
     * instances are using the configuration. If any instances are using
     * the config, returns `FAILED_PRECONDITION`.
     * Only user managed configurations can be deleted.
     * Authorization requires `spanner.instanceConfigs.delete` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    public void deleteInstanceConfig(
        com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteInstanceConfigMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists the user-managed instance config [long-running
     * operations][google.longrunning.Operation] in the given project. An instance
     * config operation has a name of the form
     * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
     * The long-running operation
     * [metadata][google.longrunning.Operation.metadata] field type
     * `metadata.type_url` describes the type of the metadata. Operations returned
     * include those that have completed/failed/canceled within the last 7 days,
     * and pending operations. Operations returned are ordered by
     * `operation.metadata.value.start_time` in descending order starting
     * from the most recently started operation.
     * </pre>
     */
    public void listInstanceConfigOperations(
        com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest request,
        io.grpc.stub.StreamObserver<
                com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListInstanceConfigOperationsMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all instances in the given project.
     * </pre>
     */
    public void listInstances(
        com.google.spanner.admin.instance.v1.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.ListInstancesResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance.
     * </pre>
     */
    public void getInstance(
        com.google.spanner.admin.instance.v1.GetInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.Instance>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance and begins preparing it to begin serving. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance. The instance name is assigned by the caller. If the
     * named instance already exists, `CreateInstance` returns
     * `ALREADY_EXISTS`.
     * Immediately upon completion of this request:
     *   * The instance is readable via the API, with all requested attributes
     *     but no allocated resources. Its state is `CREATING`.
     * Until completion of the returned operation:
     *   * Cancelling the operation renders the instance immediately unreadable
     *     via the API.
     *   * The instance can be deleted.
     *   * All other attempts to modify the instance are rejected.
     * Upon completion of the returned operation:
     *   * Billing for all successfully-allocated resources begins (some types
     *     may have lower than the requested levels).
     *   * Databases can be created in the instance.
     *   * The instance's allocated resource levels are readable via the API.
     *   * The instance's state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track creation of the instance.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * </pre>
     */
    public void createInstance(
        com.google.spanner.admin.instance.v1.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance, and begins allocating or releasing resources
     * as requested. The returned [long-running
     * operation][google.longrunning.Operation] can be used to track the
     * progress of updating the instance. If the named instance does not
     * exist, returns `NOT_FOUND`.
     * Immediately upon completion of this request:
     *   * For resource types for which a decrease in the instance's allocation
     *     has been requested, billing is based on the newly-requested level.
     * Until completion of the returned operation:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time],
     *     and begins restoring resources to their pre-request values. The
     *     operation is guaranteed to succeed at undoing all resource changes,
     *     after which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance are rejected.
     *   * Reading the instance via the API continues to give the pre-request
     *     resource levels.
     * Upon completion of the returned operation:
     *   * Billing begins for all successfully-allocated resources (some types
     *     may have lower than the requested levels).
     *   * All newly-reserved resources are available for serving the instance's
     *     tables.
     *   * The instance's new resource levels are readable via the API.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track the instance modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * Authorization requires `spanner.instances.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.Instance.name].
     * </pre>
     */
    public void updateInstance(
        com.google.spanner.admin.instance.v1.UpdateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * Immediately upon completion of the request:
     *   * Billing ceases for all of the instance's reserved resources.
     * Soon afterward:
     *   * The instance and *all of its databases* immediately and
     *     irrevocably disappear from the API. All data in the databases
     *     is permanently deleted.
     * </pre>
     */
    public void deleteInstance(
        com.google.spanner.admin.instance.v1.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Sets the access control policy on an instance resource. Replaces any
     * existing policy.
     * Authorization requires `spanner.instances.setIamPolicy` on
     * [resource][google.iam.v1.SetIamPolicyRequest.resource].
     * </pre>
     */
    public void setIamPolicy(
        com.google.iam.v1.SetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetIamPolicyMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the access control policy for an instance resource. Returns an empty
     * policy if an instance exists but does not have a policy set.
     * Authorization requires `spanner.instances.getIamPolicy` on
     * [resource][google.iam.v1.GetIamPolicyRequest.resource].
     * </pre>
     */
    public void getIamPolicy(
        com.google.iam.v1.GetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetIamPolicyMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Returns permissions that the caller has on the specified instance resource.
     * Attempting this RPC on a non-existent Cloud Spanner instance resource will
     * result in a NOT_FOUND error if the user has `spanner.instances.list`
     * permission on the containing Google Cloud Project. Otherwise returns an
     * empty set of permissions.
     * </pre>
     */
    public void testIamPermissions(
        com.google.iam.v1.TestIamPermissionsRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.TestIamPermissionsResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getTestIamPermissionsMethod(), getCallOptions()),
          request,
          responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service InstanceAdmin.
   *
   * <pre>
   * Cloud Spanner Instance Admin API
   * The Cloud Spanner Instance Admin API can be used to create, delete,
   * modify and list instances. Instances are dedicated Cloud Spanner serving
   * and storage resources to be used by Cloud Spanner databases.
   * Each instance has a "configuration", which dictates where the
   * serving resources for the Cloud Spanner instance are located (e.g.,
   * US-central, Europe). Configurations are created by Google based on
   * resource availability.
   * Cloud Spanner billing is based on the instances that exist and their
   * sizes. After an instance exists, there are no additional
   * per-database or per-operation charges for use of the instance
   * (though there may be additional network bandwidth charges).
   * Instances offer isolation: problems with databases in one instance
   * will not affect other instances. However, within an instance
   * databases can affect each other. For example, if one database in an
   * instance receives a lot of requests and consumes most of the
   * instance resources, fewer resources are available for other
   * databases in that instance, and their performance may suffer.
   * </pre>
   */
  public static final class InstanceAdminBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<InstanceAdminBlockingStub> {
    private InstanceAdminBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InstanceAdminBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new InstanceAdminBlockingStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Lists the supported instance configurations for a given project.
     * </pre>
     */
    public com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse listInstanceConfigs(
        com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListInstanceConfigsMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance configuration.
     * </pre>
     */
    public com.google.spanner.admin.instance.v1.InstanceConfig getInstanceConfig(
        com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetInstanceConfigMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance config and begins preparing it to be used. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance config. The instance config name is assigned by the caller. If the
     * named instance config already exists, `CreateInstanceConfig` returns
     * `ALREADY_EXISTS`.
     * Immediately after the request returns:
     *   * The instance config is readable via the API, with all requested
     *     attributes. The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true. Its state is `CREATING`.
     * While the operation is pending:
     *   * Cancelling the operation renders the instance config immediately
     *     unreadable via the API.
     *   * Except for deleting the creating resource, all other attempts to modify
     *     the instance config are rejected.
     * Upon completion of the returned operation:
     *   * Instances can be created using the instance configuration.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false. Its state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * creation of the instance config. The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.create` permission on
     * the resource
     * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
     * </pre>
     */
    public com.google.longrunning.Operation createInstanceConfig(
        com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateInstanceConfigMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance config. The returned
     * [long-running operation][google.longrunning.Operation] can be used to track
     * the progress of updating the instance. If the named instance config does
     * not exist, returns `NOT_FOUND`.
     * Only user managed configurations can be updated.
     * Immediately after the request returns:
     *   * The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true.
     * While the operation is pending:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time].
     *     The operation is guaranteed to succeed at undoing all changes, after
     *     which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance config are rejected.
     *   * Reading the instance config via the API continues to give the
     *     pre-request values.
     * Upon completion of the returned operation:
     *   * Creating instances using the instance configuration uses the new
     *     values.
     *   * The instance config's new values are readable via the API.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * the instance config modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    public com.google.longrunning.Operation updateInstanceConfig(
        com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateInstanceConfigMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes the instance config. Deletion is only allowed when no
     * instances are using the configuration. If any instances are using
     * the config, returns `FAILED_PRECONDITION`.
     * Only user managed configurations can be deleted.
     * Authorization requires `spanner.instanceConfigs.delete` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    public com.google.protobuf.Empty deleteInstanceConfig(
        com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteInstanceConfigMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Lists the user-managed instance config [long-running
     * operations][google.longrunning.Operation] in the given project. An instance
     * config operation has a name of the form
     * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
     * The long-running operation
     * [metadata][google.longrunning.Operation.metadata] field type
     * `metadata.type_url` describes the type of the metadata. Operations returned
     * include those that have completed/failed/canceled within the last 7 days,
     * and pending operations. Operations returned are ordered by
     * `operation.metadata.value.start_time` in descending order starting
     * from the most recently started operation.
     * </pre>
     */
    public com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse
        listInstanceConfigOperations(
            com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListInstanceConfigOperationsMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all instances in the given project.
     * </pre>
     */
    public com.google.spanner.admin.instance.v1.ListInstancesResponse listInstances(
        com.google.spanner.admin.instance.v1.ListInstancesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListInstancesMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance.
     * </pre>
     */
    public com.google.spanner.admin.instance.v1.Instance getInstance(
        com.google.spanner.admin.instance.v1.GetInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance and begins preparing it to begin serving. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance. The instance name is assigned by the caller. If the
     * named instance already exists, `CreateInstance` returns
     * `ALREADY_EXISTS`.
     * Immediately upon completion of this request:
     *   * The instance is readable via the API, with all requested attributes
     *     but no allocated resources. Its state is `CREATING`.
     * Until completion of the returned operation:
     *   * Cancelling the operation renders the instance immediately unreadable
     *     via the API.
     *   * The instance can be deleted.
     *   * All other attempts to modify the instance are rejected.
     * Upon completion of the returned operation:
     *   * Billing for all successfully-allocated resources begins (some types
     *     may have lower than the requested levels).
     *   * Databases can be created in the instance.
     *   * The instance's allocated resource levels are readable via the API.
     *   * The instance's state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track creation of the instance.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * </pre>
     */
    public com.google.longrunning.Operation createInstance(
        com.google.spanner.admin.instance.v1.CreateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance, and begins allocating or releasing resources
     * as requested. The returned [long-running
     * operation][google.longrunning.Operation] can be used to track the
     * progress of updating the instance. If the named instance does not
     * exist, returns `NOT_FOUND`.
     * Immediately upon completion of this request:
     *   * For resource types for which a decrease in the instance's allocation
     *     has been requested, billing is based on the newly-requested level.
     * Until completion of the returned operation:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time],
     *     and begins restoring resources to their pre-request values. The
     *     operation is guaranteed to succeed at undoing all resource changes,
     *     after which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance are rejected.
     *   * Reading the instance via the API continues to give the pre-request
     *     resource levels.
     * Upon completion of the returned operation:
     *   * Billing begins for all successfully-allocated resources (some types
     *     may have lower than the requested levels).
     *   * All newly-reserved resources are available for serving the instance's
     *     tables.
     *   * The instance's new resource levels are readable via the API.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track the instance modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * Authorization requires `spanner.instances.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.Instance.name].
     * </pre>
     */
    public com.google.longrunning.Operation updateInstance(
        com.google.spanner.admin.instance.v1.UpdateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * Immediately upon completion of the request:
     *   * Billing ceases for all of the instance's reserved resources.
     * Soon afterward:
     *   * The instance and *all of its databases* immediately and
     *     irrevocably disappear from the API. All data in the databases
     *     is permanently deleted.
     * </pre>
     */
    public com.google.protobuf.Empty deleteInstance(
        com.google.spanner.admin.instance.v1.DeleteInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Sets the access control policy on an instance resource. Replaces any
     * existing policy.
     * Authorization requires `spanner.instances.setIamPolicy` on
     * [resource][google.iam.v1.SetIamPolicyRequest.resource].
     * </pre>
     */
    public com.google.iam.v1.Policy setIamPolicy(com.google.iam.v1.SetIamPolicyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetIamPolicyMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the access control policy for an instance resource. Returns an empty
     * policy if an instance exists but does not have a policy set.
     * Authorization requires `spanner.instances.getIamPolicy` on
     * [resource][google.iam.v1.GetIamPolicyRequest.resource].
     * </pre>
     */
    public com.google.iam.v1.Policy getIamPolicy(com.google.iam.v1.GetIamPolicyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetIamPolicyMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Returns permissions that the caller has on the specified instance resource.
     * Attempting this RPC on a non-existent Cloud Spanner instance resource will
     * result in a NOT_FOUND error if the user has `spanner.instances.list`
     * permission on the containing Google Cloud Project. Otherwise returns an
     * empty set of permissions.
     * </pre>
     */
    public com.google.iam.v1.TestIamPermissionsResponse testIamPermissions(
        com.google.iam.v1.TestIamPermissionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getTestIamPermissionsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service InstanceAdmin.
   *
   * <pre>
   * Cloud Spanner Instance Admin API
   * The Cloud Spanner Instance Admin API can be used to create, delete,
   * modify and list instances. Instances are dedicated Cloud Spanner serving
   * and storage resources to be used by Cloud Spanner databases.
   * Each instance has a "configuration", which dictates where the
   * serving resources for the Cloud Spanner instance are located (e.g.,
   * US-central, Europe). Configurations are created by Google based on
   * resource availability.
   * Cloud Spanner billing is based on the instances that exist and their
   * sizes. After an instance exists, there are no additional
   * per-database or per-operation charges for use of the instance
   * (though there may be additional network bandwidth charges).
   * Instances offer isolation: problems with databases in one instance
   * will not affect other instances. However, within an instance
   * databases can affect each other. For example, if one database in an
   * instance receives a lot of requests and consumes most of the
   * instance resources, fewer resources are available for other
   * databases in that instance, and their performance may suffer.
   * </pre>
   */
  public static final class InstanceAdminFutureStub
      extends io.grpc.stub.AbstractFutureStub<InstanceAdminFutureStub> {
    private InstanceAdminFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InstanceAdminFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new InstanceAdminFutureStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Lists the supported instance configurations for a given project.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
        listInstanceConfigs(
            com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListInstanceConfigsMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance configuration.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.InstanceConfig>
        getInstanceConfig(com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetInstanceConfigMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance config and begins preparing it to be used. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance config. The instance config name is assigned by the caller. If the
     * named instance config already exists, `CreateInstanceConfig` returns
     * `ALREADY_EXISTS`.
     * Immediately after the request returns:
     *   * The instance config is readable via the API, with all requested
     *     attributes. The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true. Its state is `CREATING`.
     * While the operation is pending:
     *   * Cancelling the operation renders the instance config immediately
     *     unreadable via the API.
     *   * Except for deleting the creating resource, all other attempts to modify
     *     the instance config are rejected.
     * Upon completion of the returned operation:
     *   * Instances can be created using the instance configuration.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false. Its state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * creation of the instance config. The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.create` permission on
     * the resource
     * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        createInstanceConfig(
            com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateInstanceConfigMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance config. The returned
     * [long-running operation][google.longrunning.Operation] can be used to track
     * the progress of updating the instance. If the named instance config does
     * not exist, returns `NOT_FOUND`.
     * Only user managed configurations can be updated.
     * Immediately after the request returns:
     *   * The instance config's
     *     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *     field is set to true.
     * While the operation is pending:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time].
     *     The operation is guaranteed to succeed at undoing all changes, after
     *     which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance config are rejected.
     *   * Reading the instance config via the API continues to give the
     *     pre-request values.
     * Upon completion of the returned operation:
     *   * Creating instances using the instance configuration uses the new
     *     values.
     *   * The instance config's new values are readable via the API.
     *   * The instance config's
     *   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
     *   field becomes false.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format
     * `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
     * the instance config modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if
     * successful.
     * Authorization requires `spanner.instanceConfigs.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        updateInstanceConfig(
            com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateInstanceConfigMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes the instance config. Deletion is only allowed when no
     * instances are using the configuration. If any instances are using
     * the config, returns `FAILED_PRECONDITION`.
     * Only user managed configurations can be deleted.
     * Authorization requires `spanner.instanceConfigs.delete` permission on
     * the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty>
        deleteInstanceConfig(
            com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteInstanceConfigMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Lists the user-managed instance config [long-running
     * operations][google.longrunning.Operation] in the given project. An instance
     * config operation has a name of the form
     * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
     * The long-running operation
     * [metadata][google.longrunning.Operation.metadata] field type
     * `metadata.type_url` describes the type of the metadata. Operations returned
     * include those that have completed/failed/canceled within the last 7 days,
     * and pending operations. Operations returned are ordered by
     * `operation.metadata.value.start_time` in descending order starting
     * from the most recently started operation.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>
        listInstanceConfigOperations(
            com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListInstanceConfigOperationsMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all instances in the given project.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.ListInstancesResponse>
        listInstances(com.google.spanner.admin.instance.v1.ListInstancesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Gets information about a particular instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.Instance>
        getInstance(com.google.spanner.admin.instance.v1.GetInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance and begins preparing it to begin serving. The
     * returned [long-running operation][google.longrunning.Operation]
     * can be used to track the progress of preparing the new
     * instance. The instance name is assigned by the caller. If the
     * named instance already exists, `CreateInstance` returns
     * `ALREADY_EXISTS`.
     * Immediately upon completion of this request:
     *   * The instance is readable via the API, with all requested attributes
     *     but no allocated resources. Its state is `CREATING`.
     * Until completion of the returned operation:
     *   * Cancelling the operation renders the instance immediately unreadable
     *     via the API.
     *   * The instance can be deleted.
     *   * All other attempts to modify the instance are rejected.
     * Upon completion of the returned operation:
     *   * Billing for all successfully-allocated resources begins (some types
     *     may have lower than the requested levels).
     *   * Databases can be created in the instance.
     *   * The instance's allocated resource levels are readable via the API.
     *   * The instance's state becomes `READY`.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track creation of the instance.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        createInstance(com.google.spanner.admin.instance.v1.CreateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Updates an instance, and begins allocating or releasing resources
     * as requested. The returned [long-running
     * operation][google.longrunning.Operation] can be used to track the
     * progress of updating the instance. If the named instance does not
     * exist, returns `NOT_FOUND`.
     * Immediately upon completion of this request:
     *   * For resource types for which a decrease in the instance's allocation
     *     has been requested, billing is based on the newly-requested level.
     * Until completion of the returned operation:
     *   * Cancelling the operation sets its metadata's
     *     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time],
     *     and begins restoring resources to their pre-request values. The
     *     operation is guaranteed to succeed at undoing all resource changes,
     *     after which point it terminates with a `CANCELLED` status.
     *   * All other attempts to modify the instance are rejected.
     *   * Reading the instance via the API continues to give the pre-request
     *     resource levels.
     * Upon completion of the returned operation:
     *   * Billing begins for all successfully-allocated resources (some types
     *     may have lower than the requested levels).
     *   * All newly-reserved resources are available for serving the instance's
     *     tables.
     *   * The instance's new resource levels are readable via the API.
     * The returned [long-running operation][google.longrunning.Operation] will
     * have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and
     * can be used to track the instance modification.  The
     * [metadata][google.longrunning.Operation.metadata] field type is
     * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata].
     * The [response][google.longrunning.Operation.response] field type is
     * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
     * Authorization requires `spanner.instances.update` permission on
     * the resource [name][google.spanner.admin.instance.v1.Instance.name].
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        updateInstance(com.google.spanner.admin.instance.v1.UpdateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * Immediately upon completion of the request:
     *   * Billing ceases for all of the instance's reserved resources.
     * Soon afterward:
     *   * The instance and *all of its databases* immediately and
     *     irrevocably disappear from the API. All data in the databases
     *     is permanently deleted.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty>
        deleteInstance(com.google.spanner.admin.instance.v1.DeleteInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Sets the access control policy on an instance resource. Replaces any
     * existing policy.
     * Authorization requires `spanner.instances.setIamPolicy` on
     * [resource][google.iam.v1.SetIamPolicyRequest.resource].
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.iam.v1.Policy>
        setIamPolicy(com.google.iam.v1.SetIamPolicyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetIamPolicyMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the access control policy for an instance resource. Returns an empty
     * policy if an instance exists but does not have a policy set.
     * Authorization requires `spanner.instances.getIamPolicy` on
     * [resource][google.iam.v1.GetIamPolicyRequest.resource].
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.iam.v1.Policy>
        getIamPolicy(com.google.iam.v1.GetIamPolicyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetIamPolicyMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Returns permissions that the caller has on the specified instance resource.
     * Attempting this RPC on a non-existent Cloud Spanner instance resource will
     * result in a NOT_FOUND error if the user has `spanner.instances.list`
     * permission on the containing Google Cloud Project. Otherwise returns an
     * empty set of permissions.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.iam.v1.TestIamPermissionsResponse>
        testIamPermissions(com.google.iam.v1.TestIamPermissionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getTestIamPermissionsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_INSTANCE_CONFIGS = 0;
  private static final int METHODID_GET_INSTANCE_CONFIG = 1;
  private static final int METHODID_CREATE_INSTANCE_CONFIG = 2;
  private static final int METHODID_UPDATE_INSTANCE_CONFIG = 3;
  private static final int METHODID_DELETE_INSTANCE_CONFIG = 4;
  private static final int METHODID_LIST_INSTANCE_CONFIG_OPERATIONS = 5;
  private static final int METHODID_LIST_INSTANCES = 6;
  private static final int METHODID_GET_INSTANCE = 7;
  private static final int METHODID_CREATE_INSTANCE = 8;
  private static final int METHODID_UPDATE_INSTANCE = 9;
  private static final int METHODID_DELETE_INSTANCE = 10;
  private static final int METHODID_SET_IAM_POLICY = 11;
  private static final int METHODID_GET_IAM_POLICY = 12;
  private static final int METHODID_TEST_IAM_PERMISSIONS = 13;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LIST_INSTANCE_CONFIGS:
          serviceImpl.listInstanceConfigs(
              (com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest) request,
              (io.grpc.stub.StreamObserver<
                      com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>)
                  responseObserver);
          break;
        case METHODID_GET_INSTANCE_CONFIG:
          serviceImpl.getInstanceConfig(
              (com.google.spanner.admin.instance.v1.GetInstanceConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.InstanceConfig>)
                  responseObserver);
          break;
        case METHODID_CREATE_INSTANCE_CONFIG:
          serviceImpl.createInstanceConfig(
              (com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_UPDATE_INSTANCE_CONFIG:
          serviceImpl.updateInstanceConfig(
              (com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_DELETE_INSTANCE_CONFIG:
          serviceImpl.deleteInstanceConfig(
              (com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_LIST_INSTANCE_CONFIG_OPERATIONS:
          serviceImpl.listInstanceConfigOperations(
              (com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest) request,
              (io.grpc.stub.StreamObserver<
                      com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>)
                  responseObserver);
          break;
        case METHODID_LIST_INSTANCES:
          serviceImpl.listInstances(
              (com.google.spanner.admin.instance.v1.ListInstancesRequest) request,
              (io.grpc.stub.StreamObserver<
                      com.google.spanner.admin.instance.v1.ListInstancesResponse>)
                  responseObserver);
          break;
        case METHODID_GET_INSTANCE:
          serviceImpl.getInstance(
              (com.google.spanner.admin.instance.v1.GetInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.Instance>)
                  responseObserver);
          break;
        case METHODID_CREATE_INSTANCE:
          serviceImpl.createInstance(
              (com.google.spanner.admin.instance.v1.CreateInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_UPDATE_INSTANCE:
          serviceImpl.updateInstance(
              (com.google.spanner.admin.instance.v1.UpdateInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_DELETE_INSTANCE:
          serviceImpl.deleteInstance(
              (com.google.spanner.admin.instance.v1.DeleteInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SET_IAM_POLICY:
          serviceImpl.setIamPolicy(
              (com.google.iam.v1.SetIamPolicyRequest) request,
              (io.grpc.stub.StreamObserver<com.google.iam.v1.Policy>) responseObserver);
          break;
        case METHODID_GET_IAM_POLICY:
          serviceImpl.getIamPolicy(
              (com.google.iam.v1.GetIamPolicyRequest) request,
              (io.grpc.stub.StreamObserver<com.google.iam.v1.Policy>) responseObserver);
          break;
        case METHODID_TEST_IAM_PERMISSIONS:
          serviceImpl.testIamPermissions(
              (com.google.iam.v1.TestIamPermissionsRequest) request,
              (io.grpc.stub.StreamObserver<com.google.iam.v1.TestIamPermissionsResponse>)
                  responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
            getListInstanceConfigsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest,
                    com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>(
                    service, METHODID_LIST_INSTANCE_CONFIGS)))
        .addMethod(
            getGetInstanceConfigMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.GetInstanceConfigRequest,
                    com.google.spanner.admin.instance.v1.InstanceConfig>(
                    service, METHODID_GET_INSTANCE_CONFIG)))
        .addMethod(
            getCreateInstanceConfigMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest,
                    com.google.longrunning.Operation>(service, METHODID_CREATE_INSTANCE_CONFIG)))
        .addMethod(
            getUpdateInstanceConfigMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest,
                    com.google.longrunning.Operation>(service, METHODID_UPDATE_INSTANCE_CONFIG)))
        .addMethod(
            getDeleteInstanceConfigMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest,
                    com.google.protobuf.Empty>(service, METHODID_DELETE_INSTANCE_CONFIG)))
        .addMethod(
            getListInstanceConfigOperationsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest,
                    com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse>(
                    service, METHODID_LIST_INSTANCE_CONFIG_OPERATIONS)))
        .addMethod(
            getListInstancesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.ListInstancesRequest,
                    com.google.spanner.admin.instance.v1.ListInstancesResponse>(
                    service, METHODID_LIST_INSTANCES)))
        .addMethod(
            getGetInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.GetInstanceRequest,
                    com.google.spanner.admin.instance.v1.Instance>(service, METHODID_GET_INSTANCE)))
        .addMethod(
            getCreateInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.CreateInstanceRequest,
                    com.google.longrunning.Operation>(service, METHODID_CREATE_INSTANCE)))
        .addMethod(
            getUpdateInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.UpdateInstanceRequest,
                    com.google.longrunning.Operation>(service, METHODID_UPDATE_INSTANCE)))
        .addMethod(
            getDeleteInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.spanner.admin.instance.v1.DeleteInstanceRequest,
                    com.google.protobuf.Empty>(service, METHODID_DELETE_INSTANCE)))
        .addMethod(
            getSetIamPolicyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<com.google.iam.v1.SetIamPolicyRequest, com.google.iam.v1.Policy>(
                    service, METHODID_SET_IAM_POLICY)))
        .addMethod(
            getGetIamPolicyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<com.google.iam.v1.GetIamPolicyRequest, com.google.iam.v1.Policy>(
                    service, METHODID_GET_IAM_POLICY)))
        .addMethod(
            getTestIamPermissionsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
                new MethodHandlers<
                    com.google.iam.v1.TestIamPermissionsRequest,
                    com.google.iam.v1.TestIamPermissionsResponse>(
                    service, METHODID_TEST_IAM_PERMISSIONS)))
        .build();
  }

  private abstract static class InstanceAdminBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier,
          io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    InstanceAdminBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.google.spanner.admin.instance.v1.SpannerInstanceAdminProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("InstanceAdmin");
    }
  }

  private static final class InstanceAdminFileDescriptorSupplier
      extends InstanceAdminBaseDescriptorSupplier {
    InstanceAdminFileDescriptorSupplier() {}
  }

  private static final class InstanceAdminMethodDescriptorSupplier
      extends InstanceAdminBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    InstanceAdminMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (InstanceAdminGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor =
              result =
                  io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                      .setSchemaDescriptor(new InstanceAdminFileDescriptorSupplier())
                      .addMethod(getListInstanceConfigsMethod())
                      .addMethod(getGetInstanceConfigMethod())
                      .addMethod(getCreateInstanceConfigMethod())
                      .addMethod(getUpdateInstanceConfigMethod())
                      .addMethod(getDeleteInstanceConfigMethod())
                      .addMethod(getListInstanceConfigOperationsMethod())
                      .addMethod(getListInstancesMethod())
                      .addMethod(getGetInstanceMethod())
                      .addMethod(getCreateInstanceMethod())
                      .addMethod(getUpdateInstanceMethod())
                      .addMethod(getDeleteInstanceMethod())
                      .addMethod(getSetIamPolicyMethod())
                      .addMethod(getGetIamPolicyMethod())
                      .addMethod(getTestIamPermissionsMethod())
                      .build();
        }
      }
    }
    return result;
  }
}
