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
package com.google.spanner.admin.instance.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/** */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: google/spanner/admin/instance/v1/spanner_instance_admin.proto")
public final class InstanceAdminGrpc {

  private InstanceAdminGrpc() {}

  public static final String SERVICE_NAME = "google.spanner.admin.instance.v1.InstanceAdmin";

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

  /** */
  public abstract static class InstanceAdminImplBase implements io.grpc.BindableService {

    /** */
    public void listInstanceConfigs(
        com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request,
        io.grpc.stub.StreamObserver<
                com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
            responseObserver) {
      asyncUnimplementedUnaryCall(getListInstanceConfigsMethod(), responseObserver);
    }

    /** */
    public void getInstanceConfig(
        com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.InstanceConfig>
            responseObserver) {
      asyncUnimplementedUnaryCall(getGetInstanceConfigMethod(), responseObserver);
    }

    /** */
    public void listInstances(
        com.google.spanner.admin.instance.v1.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.ListInstancesResponse>
            responseObserver) {
      asyncUnimplementedUnaryCall(getListInstancesMethod(), responseObserver);
    }

    /** */
    public void getInstance(
        com.google.spanner.admin.instance.v1.GetInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.Instance>
            responseObserver) {
      asyncUnimplementedUnaryCall(getGetInstanceMethod(), responseObserver);
    }

    /** */
    public void createInstance(
        com.google.spanner.admin.instance.v1.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateInstanceMethod(), responseObserver);
    }

    /** */
    public void updateInstance(
        com.google.spanner.admin.instance.v1.UpdateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateInstanceMethod(), responseObserver);
    }

    /** */
    public void deleteInstance(
        com.google.spanner.admin.instance.v1.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteInstanceMethod(), responseObserver);
    }

    /** */
    public void setIamPolicy(
        com.google.iam.v1.SetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      asyncUnimplementedUnaryCall(getSetIamPolicyMethod(), responseObserver);
    }

    /** */
    public void getIamPolicy(
        com.google.iam.v1.GetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      asyncUnimplementedUnaryCall(getGetIamPolicyMethod(), responseObserver);
    }

    /** */
    public void testIamPermissions(
        com.google.iam.v1.TestIamPermissionsRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.TestIamPermissionsResponse>
            responseObserver) {
      asyncUnimplementedUnaryCall(getTestIamPermissionsMethod(), responseObserver);
    }

    @java.lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
              getListInstanceConfigsMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest,
                      com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>(
                      this, METHODID_LIST_INSTANCE_CONFIGS)))
          .addMethod(
              getGetInstanceConfigMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.admin.instance.v1.GetInstanceConfigRequest,
                      com.google.spanner.admin.instance.v1.InstanceConfig>(
                      this, METHODID_GET_INSTANCE_CONFIG)))
          .addMethod(
              getListInstancesMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.admin.instance.v1.ListInstancesRequest,
                      com.google.spanner.admin.instance.v1.ListInstancesResponse>(
                      this, METHODID_LIST_INSTANCES)))
          .addMethod(
              getGetInstanceMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.admin.instance.v1.GetInstanceRequest,
                      com.google.spanner.admin.instance.v1.Instance>(this, METHODID_GET_INSTANCE)))
          .addMethod(
              getCreateInstanceMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.admin.instance.v1.CreateInstanceRequest,
                      com.google.longrunning.Operation>(this, METHODID_CREATE_INSTANCE)))
          .addMethod(
              getUpdateInstanceMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.admin.instance.v1.UpdateInstanceRequest,
                      com.google.longrunning.Operation>(this, METHODID_UPDATE_INSTANCE)))
          .addMethod(
              getDeleteInstanceMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.admin.instance.v1.DeleteInstanceRequest,
                      com.google.protobuf.Empty>(this, METHODID_DELETE_INSTANCE)))
          .addMethod(
              getSetIamPolicyMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.iam.v1.SetIamPolicyRequest, com.google.iam.v1.Policy>(
                      this, METHODID_SET_IAM_POLICY)))
          .addMethod(
              getGetIamPolicyMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.iam.v1.GetIamPolicyRequest, com.google.iam.v1.Policy>(
                      this, METHODID_GET_IAM_POLICY)))
          .addMethod(
              getTestIamPermissionsMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.iam.v1.TestIamPermissionsRequest,
                      com.google.iam.v1.TestIamPermissionsResponse>(
                      this, METHODID_TEST_IAM_PERMISSIONS)))
          .build();
    }
  }

  /** */
  public static final class InstanceAdminStub
      extends io.grpc.stub.AbstractAsyncStub<InstanceAdminStub> {
    private InstanceAdminStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InstanceAdminStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new InstanceAdminStub(channel, callOptions);
    }

    /** */
    public void listInstanceConfigs(
        com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request,
        io.grpc.stub.StreamObserver<
                com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
            responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListInstanceConfigsMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void getInstanceConfig(
        com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.InstanceConfig>
            responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetInstanceConfigMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void listInstances(
        com.google.spanner.admin.instance.v1.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.ListInstancesResponse>
            responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void getInstance(
        com.google.spanner.admin.instance.v1.GetInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.admin.instance.v1.Instance>
            responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void createInstance(
        com.google.spanner.admin.instance.v1.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void updateInstance(
        com.google.spanner.admin.instance.v1.UpdateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void deleteInstance(
        com.google.spanner.admin.instance.v1.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void setIamPolicy(
        com.google.iam.v1.SetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetIamPolicyMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void getIamPolicy(
        com.google.iam.v1.GetIamPolicyRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.Policy> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetIamPolicyMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void testIamPermissions(
        com.google.iam.v1.TestIamPermissionsRequest request,
        io.grpc.stub.StreamObserver<com.google.iam.v1.TestIamPermissionsResponse>
            responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTestIamPermissionsMethod(), getCallOptions()),
          request,
          responseObserver);
    }
  }

  /** */
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

    /** */
    public com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse listInstanceConfigs(
        com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListInstanceConfigsMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.admin.instance.v1.InstanceConfig getInstanceConfig(
        com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetInstanceConfigMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.admin.instance.v1.ListInstancesResponse listInstances(
        com.google.spanner.admin.instance.v1.ListInstancesRequest request) {
      return blockingUnaryCall(getChannel(), getListInstancesMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.admin.instance.v1.Instance getInstance(
        com.google.spanner.admin.instance.v1.GetInstanceRequest request) {
      return blockingUnaryCall(getChannel(), getGetInstanceMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.longrunning.Operation createInstance(
        com.google.spanner.admin.instance.v1.CreateInstanceRequest request) {
      return blockingUnaryCall(getChannel(), getCreateInstanceMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.longrunning.Operation updateInstance(
        com.google.spanner.admin.instance.v1.UpdateInstanceRequest request) {
      return blockingUnaryCall(getChannel(), getUpdateInstanceMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.protobuf.Empty deleteInstance(
        com.google.spanner.admin.instance.v1.DeleteInstanceRequest request) {
      return blockingUnaryCall(getChannel(), getDeleteInstanceMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.iam.v1.Policy setIamPolicy(com.google.iam.v1.SetIamPolicyRequest request) {
      return blockingUnaryCall(getChannel(), getSetIamPolicyMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.iam.v1.Policy getIamPolicy(com.google.iam.v1.GetIamPolicyRequest request) {
      return blockingUnaryCall(getChannel(), getGetIamPolicyMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.iam.v1.TestIamPermissionsResponse testIamPermissions(
        com.google.iam.v1.TestIamPermissionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getTestIamPermissionsMethod(), getCallOptions(), request);
    }
  }

  /** */
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

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse>
        listInstanceConfigs(
            com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListInstanceConfigsMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.InstanceConfig>
        getInstanceConfig(com.google.spanner.admin.instance.v1.GetInstanceConfigRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetInstanceConfigMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.ListInstancesResponse>
        listInstances(com.google.spanner.admin.instance.v1.ListInstancesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.admin.instance.v1.Instance>
        getInstance(com.google.spanner.admin.instance.v1.GetInstanceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetInstanceMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        createInstance(com.google.spanner.admin.instance.v1.CreateInstanceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        updateInstance(com.google.spanner.admin.instance.v1.UpdateInstanceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateInstanceMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty>
        deleteInstance(com.google.spanner.admin.instance.v1.DeleteInstanceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.iam.v1.Policy>
        setIamPolicy(com.google.iam.v1.SetIamPolicyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSetIamPolicyMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.iam.v1.Policy>
        getIamPolicy(com.google.iam.v1.GetIamPolicyRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetIamPolicyMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.iam.v1.TestIamPermissionsResponse>
        testIamPermissions(com.google.iam.v1.TestIamPermissionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTestIamPermissionsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_INSTANCE_CONFIGS = 0;
  private static final int METHODID_GET_INSTANCE_CONFIG = 1;
  private static final int METHODID_LIST_INSTANCES = 2;
  private static final int METHODID_GET_INSTANCE = 3;
  private static final int METHODID_CREATE_INSTANCE = 4;
  private static final int METHODID_UPDATE_INSTANCE = 5;
  private static final int METHODID_DELETE_INSTANCE = 6;
  private static final int METHODID_SET_IAM_POLICY = 7;
  private static final int METHODID_GET_IAM_POLICY = 8;
  private static final int METHODID_TEST_IAM_PERMISSIONS = 9;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final InstanceAdminImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(InstanceAdminImplBase serviceImpl, int methodId) {
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
    private final String methodName;

    InstanceAdminMethodDescriptorSupplier(String methodName) {
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
