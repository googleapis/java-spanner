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
package com.google.spanner.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/** */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: google/spanner/v1/spanner.proto")
public final class SpannerGrpc {

  private SpannerGrpc() {}

  public static final String SERVICE_NAME = "google.spanner.v1.Spanner";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.CreateSessionRequest, com.google.spanner.v1.Session>
      getCreateSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateSession",
      requestType = com.google.spanner.v1.CreateSessionRequest.class,
      responseType = com.google.spanner.v1.Session.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.CreateSessionRequest, com.google.spanner.v1.Session>
      getCreateSessionMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.CreateSessionRequest, com.google.spanner.v1.Session>
        getCreateSessionMethod;
    if ((getCreateSessionMethod = SpannerGrpc.getCreateSessionMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getCreateSessionMethod = SpannerGrpc.getCreateSessionMethod) == null) {
          SpannerGrpc.getCreateSessionMethod =
              getCreateSessionMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.CreateSessionRequest, com.google.spanner.v1.Session>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateSession"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.CreateSessionRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.Session.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("CreateSession"))
                      .build();
        }
      }
    }
    return getCreateSessionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.BatchCreateSessionsRequest,
          com.google.spanner.v1.BatchCreateSessionsResponse>
      getBatchCreateSessionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BatchCreateSessions",
      requestType = com.google.spanner.v1.BatchCreateSessionsRequest.class,
      responseType = com.google.spanner.v1.BatchCreateSessionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.BatchCreateSessionsRequest,
          com.google.spanner.v1.BatchCreateSessionsResponse>
      getBatchCreateSessionsMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.BatchCreateSessionsRequest,
            com.google.spanner.v1.BatchCreateSessionsResponse>
        getBatchCreateSessionsMethod;
    if ((getBatchCreateSessionsMethod = SpannerGrpc.getBatchCreateSessionsMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getBatchCreateSessionsMethod = SpannerGrpc.getBatchCreateSessionsMethod) == null) {
          SpannerGrpc.getBatchCreateSessionsMethod =
              getBatchCreateSessionsMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.BatchCreateSessionsRequest,
                          com.google.spanner.v1.BatchCreateSessionsResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(
                          generateFullMethodName(SERVICE_NAME, "BatchCreateSessions"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.BatchCreateSessionsRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.BatchCreateSessionsResponse
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new SpannerMethodDescriptorSupplier("BatchCreateSessions"))
                      .build();
        }
      }
    }
    return getBatchCreateSessionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.GetSessionRequest, com.google.spanner.v1.Session>
      getGetSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSession",
      requestType = com.google.spanner.v1.GetSessionRequest.class,
      responseType = com.google.spanner.v1.Session.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.GetSessionRequest, com.google.spanner.v1.Session>
      getGetSessionMethod() {
    io.grpc.MethodDescriptor<com.google.spanner.v1.GetSessionRequest, com.google.spanner.v1.Session>
        getGetSessionMethod;
    if ((getGetSessionMethod = SpannerGrpc.getGetSessionMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getGetSessionMethod = SpannerGrpc.getGetSessionMethod) == null) {
          SpannerGrpc.getGetSessionMethod =
              getGetSessionMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.GetSessionRequest, com.google.spanner.v1.Session>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSession"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.GetSessionRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.Session.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("GetSession"))
                      .build();
        }
      }
    }
    return getGetSessionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.ListSessionsRequest, com.google.spanner.v1.ListSessionsResponse>
      getListSessionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListSessions",
      requestType = com.google.spanner.v1.ListSessionsRequest.class,
      responseType = com.google.spanner.v1.ListSessionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.ListSessionsRequest, com.google.spanner.v1.ListSessionsResponse>
      getListSessionsMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.ListSessionsRequest, com.google.spanner.v1.ListSessionsResponse>
        getListSessionsMethod;
    if ((getListSessionsMethod = SpannerGrpc.getListSessionsMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getListSessionsMethod = SpannerGrpc.getListSessionsMethod) == null) {
          SpannerGrpc.getListSessionsMethod =
              getListSessionsMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.ListSessionsRequest,
                          com.google.spanner.v1.ListSessionsResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListSessions"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ListSessionsRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ListSessionsResponse.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("ListSessions"))
                      .build();
        }
      }
    }
    return getListSessionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.DeleteSessionRequest, com.google.protobuf.Empty>
      getDeleteSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteSession",
      requestType = com.google.spanner.v1.DeleteSessionRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.DeleteSessionRequest, com.google.protobuf.Empty>
      getDeleteSessionMethod() {
    io.grpc.MethodDescriptor<com.google.spanner.v1.DeleteSessionRequest, com.google.protobuf.Empty>
        getDeleteSessionMethod;
    if ((getDeleteSessionMethod = SpannerGrpc.getDeleteSessionMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getDeleteSessionMethod = SpannerGrpc.getDeleteSessionMethod) == null) {
          SpannerGrpc.getDeleteSessionMethod =
              getDeleteSessionMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.DeleteSessionRequest, com.google.protobuf.Empty>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteSession"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.DeleteSessionRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.protobuf.Empty.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("DeleteSession"))
                      .build();
        }
      }
    }
    return getDeleteSessionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.ResultSet>
      getExecuteSqlMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteSql",
      requestType = com.google.spanner.v1.ExecuteSqlRequest.class,
      responseType = com.google.spanner.v1.ResultSet.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.ResultSet>
      getExecuteSqlMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.ResultSet>
        getExecuteSqlMethod;
    if ((getExecuteSqlMethod = SpannerGrpc.getExecuteSqlMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getExecuteSqlMethod = SpannerGrpc.getExecuteSqlMethod) == null) {
          SpannerGrpc.getExecuteSqlMethod =
              getExecuteSqlMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.ResultSet>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteSql"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ExecuteSqlRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ResultSet.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("ExecuteSql"))
                      .build();
        }
      }
    }
    return getExecuteSqlMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.PartialResultSet>
      getExecuteStreamingSqlMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteStreamingSql",
      requestType = com.google.spanner.v1.ExecuteSqlRequest.class,
      responseType = com.google.spanner.v1.PartialResultSet.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.PartialResultSet>
      getExecuteStreamingSqlMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.PartialResultSet>
        getExecuteStreamingSqlMethod;
    if ((getExecuteStreamingSqlMethod = SpannerGrpc.getExecuteStreamingSqlMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getExecuteStreamingSqlMethod = SpannerGrpc.getExecuteStreamingSqlMethod) == null) {
          SpannerGrpc.getExecuteStreamingSqlMethod =
              getExecuteStreamingSqlMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.ExecuteSqlRequest,
                          com.google.spanner.v1.PartialResultSet>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
                      .setFullMethodName(
                          generateFullMethodName(SERVICE_NAME, "ExecuteStreamingSql"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ExecuteSqlRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.PartialResultSet.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new SpannerMethodDescriptorSupplier("ExecuteStreamingSql"))
                      .build();
        }
      }
    }
    return getExecuteStreamingSqlMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.ExecuteBatchDmlRequest,
          com.google.spanner.v1.ExecuteBatchDmlResponse>
      getExecuteBatchDmlMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteBatchDml",
      requestType = com.google.spanner.v1.ExecuteBatchDmlRequest.class,
      responseType = com.google.spanner.v1.ExecuteBatchDmlResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.ExecuteBatchDmlRequest,
          com.google.spanner.v1.ExecuteBatchDmlResponse>
      getExecuteBatchDmlMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.ExecuteBatchDmlRequest,
            com.google.spanner.v1.ExecuteBatchDmlResponse>
        getExecuteBatchDmlMethod;
    if ((getExecuteBatchDmlMethod = SpannerGrpc.getExecuteBatchDmlMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getExecuteBatchDmlMethod = SpannerGrpc.getExecuteBatchDmlMethod) == null) {
          SpannerGrpc.getExecuteBatchDmlMethod =
              getExecuteBatchDmlMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.ExecuteBatchDmlRequest,
                          com.google.spanner.v1.ExecuteBatchDmlResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteBatchDml"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ExecuteBatchDmlRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ExecuteBatchDmlResponse.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("ExecuteBatchDml"))
                      .build();
        }
      }
    }
    return getExecuteBatchDmlMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.ReadRequest, com.google.spanner.v1.ResultSet>
      getReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Read",
      requestType = com.google.spanner.v1.ReadRequest.class,
      responseType = com.google.spanner.v1.ResultSet.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.ReadRequest, com.google.spanner.v1.ResultSet>
      getReadMethod() {
    io.grpc.MethodDescriptor<com.google.spanner.v1.ReadRequest, com.google.spanner.v1.ResultSet>
        getReadMethod;
    if ((getReadMethod = SpannerGrpc.getReadMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getReadMethod = SpannerGrpc.getReadMethod) == null) {
          SpannerGrpc.getReadMethod =
              getReadMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.ReadRequest, com.google.spanner.v1.ResultSet>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Read"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ReadRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ResultSet.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("Read"))
                      .build();
        }
      }
    }
    return getReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.ReadRequest, com.google.spanner.v1.PartialResultSet>
      getStreamingReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamingRead",
      requestType = com.google.spanner.v1.ReadRequest.class,
      responseType = com.google.spanner.v1.PartialResultSet.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.ReadRequest, com.google.spanner.v1.PartialResultSet>
      getStreamingReadMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.ReadRequest, com.google.spanner.v1.PartialResultSet>
        getStreamingReadMethod;
    if ((getStreamingReadMethod = SpannerGrpc.getStreamingReadMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getStreamingReadMethod = SpannerGrpc.getStreamingReadMethod) == null) {
          SpannerGrpc.getStreamingReadMethod =
              getStreamingReadMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.ReadRequest, com.google.spanner.v1.PartialResultSet>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamingRead"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.ReadRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.PartialResultSet.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("StreamingRead"))
                      .build();
        }
      }
    }
    return getStreamingReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.BeginTransactionRequest, com.google.spanner.v1.Transaction>
      getBeginTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BeginTransaction",
      requestType = com.google.spanner.v1.BeginTransactionRequest.class,
      responseType = com.google.spanner.v1.Transaction.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.BeginTransactionRequest, com.google.spanner.v1.Transaction>
      getBeginTransactionMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.BeginTransactionRequest, com.google.spanner.v1.Transaction>
        getBeginTransactionMethod;
    if ((getBeginTransactionMethod = SpannerGrpc.getBeginTransactionMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getBeginTransactionMethod = SpannerGrpc.getBeginTransactionMethod) == null) {
          SpannerGrpc.getBeginTransactionMethod =
              getBeginTransactionMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.BeginTransactionRequest,
                          com.google.spanner.v1.Transaction>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BeginTransaction"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.BeginTransactionRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.Transaction.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("BeginTransaction"))
                      .build();
        }
      }
    }
    return getBeginTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.CommitRequest, com.google.spanner.v1.CommitResponse>
      getCommitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Commit",
      requestType = com.google.spanner.v1.CommitRequest.class,
      responseType = com.google.spanner.v1.CommitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.CommitRequest, com.google.spanner.v1.CommitResponse>
      getCommitMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.CommitRequest, com.google.spanner.v1.CommitResponse>
        getCommitMethod;
    if ((getCommitMethod = SpannerGrpc.getCommitMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getCommitMethod = SpannerGrpc.getCommitMethod) == null) {
          SpannerGrpc.getCommitMethod =
              getCommitMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.CommitRequest, com.google.spanner.v1.CommitResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Commit"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.CommitRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.CommitResponse.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("Commit"))
                      .build();
        }
      }
    }
    return getCommitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.RollbackRequest, com.google.protobuf.Empty>
      getRollbackMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Rollback",
      requestType = com.google.spanner.v1.RollbackRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.RollbackRequest, com.google.protobuf.Empty>
      getRollbackMethod() {
    io.grpc.MethodDescriptor<com.google.spanner.v1.RollbackRequest, com.google.protobuf.Empty>
        getRollbackMethod;
    if ((getRollbackMethod = SpannerGrpc.getRollbackMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getRollbackMethod = SpannerGrpc.getRollbackMethod) == null) {
          SpannerGrpc.getRollbackMethod =
              getRollbackMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.RollbackRequest, com.google.protobuf.Empty>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Rollback"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.RollbackRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.protobuf.Empty.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("Rollback"))
                      .build();
        }
      }
    }
    return getRollbackMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.PartitionQueryRequest, com.google.spanner.v1.PartitionResponse>
      getPartitionQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PartitionQuery",
      requestType = com.google.spanner.v1.PartitionQueryRequest.class,
      responseType = com.google.spanner.v1.PartitionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.PartitionQueryRequest, com.google.spanner.v1.PartitionResponse>
      getPartitionQueryMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.PartitionQueryRequest, com.google.spanner.v1.PartitionResponse>
        getPartitionQueryMethod;
    if ((getPartitionQueryMethod = SpannerGrpc.getPartitionQueryMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getPartitionQueryMethod = SpannerGrpc.getPartitionQueryMethod) == null) {
          SpannerGrpc.getPartitionQueryMethod =
              getPartitionQueryMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.PartitionQueryRequest,
                          com.google.spanner.v1.PartitionResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PartitionQuery"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.PartitionQueryRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.PartitionResponse.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("PartitionQuery"))
                      .build();
        }
      }
    }
    return getPartitionQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.spanner.v1.PartitionReadRequest, com.google.spanner.v1.PartitionResponse>
      getPartitionReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PartitionRead",
      requestType = com.google.spanner.v1.PartitionReadRequest.class,
      responseType = com.google.spanner.v1.PartitionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.spanner.v1.PartitionReadRequest, com.google.spanner.v1.PartitionResponse>
      getPartitionReadMethod() {
    io.grpc.MethodDescriptor<
            com.google.spanner.v1.PartitionReadRequest, com.google.spanner.v1.PartitionResponse>
        getPartitionReadMethod;
    if ((getPartitionReadMethod = SpannerGrpc.getPartitionReadMethod) == null) {
      synchronized (SpannerGrpc.class) {
        if ((getPartitionReadMethod = SpannerGrpc.getPartitionReadMethod) == null) {
          SpannerGrpc.getPartitionReadMethod =
              getPartitionReadMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.spanner.v1.PartitionReadRequest,
                          com.google.spanner.v1.PartitionResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PartitionRead"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.PartitionReadRequest.getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.spanner.v1.PartitionResponse.getDefaultInstance()))
                      .setSchemaDescriptor(new SpannerMethodDescriptorSupplier("PartitionRead"))
                      .build();
        }
      }
    }
    return getPartitionReadMethod;
  }

  /** Creates a new async stub that supports all call types for the service */
  public static SpannerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SpannerStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<SpannerStub>() {
          @java.lang.Override
          public SpannerStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new SpannerStub(channel, callOptions);
          }
        };
    return SpannerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SpannerBlockingStub newBlockingStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SpannerBlockingStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<SpannerBlockingStub>() {
          @java.lang.Override
          public SpannerBlockingStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new SpannerBlockingStub(channel, callOptions);
          }
        };
    return SpannerBlockingStub.newStub(factory, channel);
  }

  /** Creates a new ListenableFuture-style stub that supports unary calls on the service */
  public static SpannerFutureStub newFutureStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SpannerFutureStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<SpannerFutureStub>() {
          @java.lang.Override
          public SpannerFutureStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new SpannerFutureStub(channel, callOptions);
          }
        };
    return SpannerFutureStub.newStub(factory, channel);
  }

  /** */
  public abstract static class SpannerImplBase implements io.grpc.BindableService {

    /** */
    public void createSession(
        com.google.spanner.v1.CreateSessionRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.Session> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateSessionMethod(), responseObserver);
    }

    /** */
    public void batchCreateSessions(
        com.google.spanner.v1.BatchCreateSessionsRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.BatchCreateSessionsResponse>
            responseObserver) {
      asyncUnimplementedUnaryCall(getBatchCreateSessionsMethod(), responseObserver);
    }

    /** */
    public void getSession(
        com.google.spanner.v1.GetSessionRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.Session> responseObserver) {
      asyncUnimplementedUnaryCall(getGetSessionMethod(), responseObserver);
    }

    /** */
    public void listSessions(
        com.google.spanner.v1.ListSessionsRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ListSessionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListSessionsMethod(), responseObserver);
    }

    /** */
    public void deleteSession(
        com.google.spanner.v1.DeleteSessionRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteSessionMethod(), responseObserver);
    }

    /** */
    public void executeSql(
        com.google.spanner.v1.ExecuteSqlRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ResultSet> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteSqlMethod(), responseObserver);
    }

    /** */
    public void executeStreamingSql(
        com.google.spanner.v1.ExecuteSqlRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartialResultSet> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteStreamingSqlMethod(), responseObserver);
    }

    /** */
    public void executeBatchDml(
        com.google.spanner.v1.ExecuteBatchDmlRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ExecuteBatchDmlResponse>
            responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteBatchDmlMethod(), responseObserver);
    }

    /** */
    public void read(
        com.google.spanner.v1.ReadRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ResultSet> responseObserver) {
      asyncUnimplementedUnaryCall(getReadMethod(), responseObserver);
    }

    /** */
    public void streamingRead(
        com.google.spanner.v1.ReadRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartialResultSet> responseObserver) {
      asyncUnimplementedUnaryCall(getStreamingReadMethod(), responseObserver);
    }

    /** */
    public void beginTransaction(
        com.google.spanner.v1.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getBeginTransactionMethod(), responseObserver);
    }

    /** */
    public void commit(
        com.google.spanner.v1.CommitRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.CommitResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCommitMethod(), responseObserver);
    }

    /** */
    public void rollback(
        com.google.spanner.v1.RollbackRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getRollbackMethod(), responseObserver);
    }

    /** */
    public void partitionQuery(
        com.google.spanner.v1.PartitionQueryRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartitionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPartitionQueryMethod(), responseObserver);
    }

    /** */
    public void partitionRead(
        com.google.spanner.v1.PartitionReadRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartitionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPartitionReadMethod(), responseObserver);
    }

    @java.lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
              getCreateSessionMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.CreateSessionRequest, com.google.spanner.v1.Session>(
                      this, METHODID_CREATE_SESSION)))
          .addMethod(
              getBatchCreateSessionsMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.BatchCreateSessionsRequest,
                      com.google.spanner.v1.BatchCreateSessionsResponse>(
                      this, METHODID_BATCH_CREATE_SESSIONS)))
          .addMethod(
              getGetSessionMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.GetSessionRequest, com.google.spanner.v1.Session>(
                      this, METHODID_GET_SESSION)))
          .addMethod(
              getListSessionsMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.ListSessionsRequest,
                      com.google.spanner.v1.ListSessionsResponse>(this, METHODID_LIST_SESSIONS)))
          .addMethod(
              getDeleteSessionMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.DeleteSessionRequest, com.google.protobuf.Empty>(
                      this, METHODID_DELETE_SESSION)))
          .addMethod(
              getExecuteSqlMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.ExecuteSqlRequest, com.google.spanner.v1.ResultSet>(
                      this, METHODID_EXECUTE_SQL)))
          .addMethod(
              getExecuteStreamingSqlMethod(),
              asyncServerStreamingCall(
                  new MethodHandlers<
                      com.google.spanner.v1.ExecuteSqlRequest,
                      com.google.spanner.v1.PartialResultSet>(
                      this, METHODID_EXECUTE_STREAMING_SQL)))
          .addMethod(
              getExecuteBatchDmlMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.ExecuteBatchDmlRequest,
                      com.google.spanner.v1.ExecuteBatchDmlResponse>(
                      this, METHODID_EXECUTE_BATCH_DML)))
          .addMethod(
              getReadMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.ReadRequest, com.google.spanner.v1.ResultSet>(
                      this, METHODID_READ)))
          .addMethod(
              getStreamingReadMethod(),
              asyncServerStreamingCall(
                  new MethodHandlers<
                      com.google.spanner.v1.ReadRequest, com.google.spanner.v1.PartialResultSet>(
                      this, METHODID_STREAMING_READ)))
          .addMethod(
              getBeginTransactionMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.BeginTransactionRequest,
                      com.google.spanner.v1.Transaction>(this, METHODID_BEGIN_TRANSACTION)))
          .addMethod(
              getCommitMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.CommitRequest, com.google.spanner.v1.CommitResponse>(
                      this, METHODID_COMMIT)))
          .addMethod(
              getRollbackMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.RollbackRequest, com.google.protobuf.Empty>(
                      this, METHODID_ROLLBACK)))
          .addMethod(
              getPartitionQueryMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.PartitionQueryRequest,
                      com.google.spanner.v1.PartitionResponse>(this, METHODID_PARTITION_QUERY)))
          .addMethod(
              getPartitionReadMethod(),
              asyncUnaryCall(
                  new MethodHandlers<
                      com.google.spanner.v1.PartitionReadRequest,
                      com.google.spanner.v1.PartitionResponse>(this, METHODID_PARTITION_READ)))
          .build();
    }
  }

  /** */
  public static final class SpannerStub extends io.grpc.stub.AbstractAsyncStub<SpannerStub> {
    private SpannerStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SpannerStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SpannerStub(channel, callOptions);
    }

    /** */
    public void createSession(
        com.google.spanner.v1.CreateSessionRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.Session> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateSessionMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void batchCreateSessions(
        com.google.spanner.v1.BatchCreateSessionsRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.BatchCreateSessionsResponse>
            responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBatchCreateSessionsMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void getSession(
        com.google.spanner.v1.GetSessionRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.Session> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetSessionMethod(), getCallOptions()), request, responseObserver);
    }

    /** */
    public void listSessions(
        com.google.spanner.v1.ListSessionsRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ListSessionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListSessionsMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void deleteSession(
        com.google.spanner.v1.DeleteSessionRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteSessionMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void executeSql(
        com.google.spanner.v1.ExecuteSqlRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ResultSet> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteSqlMethod(), getCallOptions()), request, responseObserver);
    }

    /** */
    public void executeStreamingSql(
        com.google.spanner.v1.ExecuteSqlRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartialResultSet> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getExecuteStreamingSqlMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void executeBatchDml(
        com.google.spanner.v1.ExecuteBatchDmlRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ExecuteBatchDmlResponse>
            responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteBatchDmlMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void read(
        com.google.spanner.v1.ReadRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.ResultSet> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadMethod(), getCallOptions()), request, responseObserver);
    }

    /** */
    public void streamingRead(
        com.google.spanner.v1.ReadRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartialResultSet> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getStreamingReadMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void beginTransaction(
        com.google.spanner.v1.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBeginTransactionMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void commit(
        com.google.spanner.v1.CommitRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.CommitResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCommitMethod(), getCallOptions()), request, responseObserver);
    }

    /** */
    public void rollback(
        com.google.spanner.v1.RollbackRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRollbackMethod(), getCallOptions()), request, responseObserver);
    }

    /** */
    public void partitionQuery(
        com.google.spanner.v1.PartitionQueryRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartitionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPartitionQueryMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /** */
    public void partitionRead(
        com.google.spanner.v1.PartitionReadRequest request,
        io.grpc.stub.StreamObserver<com.google.spanner.v1.PartitionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPartitionReadMethod(), getCallOptions()),
          request,
          responseObserver);
    }
  }

  /** */
  public static final class SpannerBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SpannerBlockingStub> {
    private SpannerBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SpannerBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SpannerBlockingStub(channel, callOptions);
    }

    /** */
    public com.google.spanner.v1.Session createSession(
        com.google.spanner.v1.CreateSessionRequest request) {
      return blockingUnaryCall(getChannel(), getCreateSessionMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.BatchCreateSessionsResponse batchCreateSessions(
        com.google.spanner.v1.BatchCreateSessionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getBatchCreateSessionsMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.Session getSession(
        com.google.spanner.v1.GetSessionRequest request) {
      return blockingUnaryCall(getChannel(), getGetSessionMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.ListSessionsResponse listSessions(
        com.google.spanner.v1.ListSessionsRequest request) {
      return blockingUnaryCall(getChannel(), getListSessionsMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.protobuf.Empty deleteSession(
        com.google.spanner.v1.DeleteSessionRequest request) {
      return blockingUnaryCall(getChannel(), getDeleteSessionMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.ResultSet executeSql(
        com.google.spanner.v1.ExecuteSqlRequest request) {
      return blockingUnaryCall(getChannel(), getExecuteSqlMethod(), getCallOptions(), request);
    }

    /** */
    public java.util.Iterator<com.google.spanner.v1.PartialResultSet> executeStreamingSql(
        com.google.spanner.v1.ExecuteSqlRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getExecuteStreamingSqlMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.ExecuteBatchDmlResponse executeBatchDml(
        com.google.spanner.v1.ExecuteBatchDmlRequest request) {
      return blockingUnaryCall(getChannel(), getExecuteBatchDmlMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.ResultSet read(com.google.spanner.v1.ReadRequest request) {
      return blockingUnaryCall(getChannel(), getReadMethod(), getCallOptions(), request);
    }

    /** */
    public java.util.Iterator<com.google.spanner.v1.PartialResultSet> streamingRead(
        com.google.spanner.v1.ReadRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getStreamingReadMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.Transaction beginTransaction(
        com.google.spanner.v1.BeginTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getBeginTransactionMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.CommitResponse commit(
        com.google.spanner.v1.CommitRequest request) {
      return blockingUnaryCall(getChannel(), getCommitMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.protobuf.Empty rollback(com.google.spanner.v1.RollbackRequest request) {
      return blockingUnaryCall(getChannel(), getRollbackMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.PartitionResponse partitionQuery(
        com.google.spanner.v1.PartitionQueryRequest request) {
      return blockingUnaryCall(getChannel(), getPartitionQueryMethod(), getCallOptions(), request);
    }

    /** */
    public com.google.spanner.v1.PartitionResponse partitionRead(
        com.google.spanner.v1.PartitionReadRequest request) {
      return blockingUnaryCall(getChannel(), getPartitionReadMethod(), getCallOptions(), request);
    }
  }

  /** */
  public static final class SpannerFutureStub
      extends io.grpc.stub.AbstractFutureStub<SpannerFutureStub> {
    private SpannerFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SpannerFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SpannerFutureStub(channel, callOptions);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.spanner.v1.Session>
        createSession(com.google.spanner.v1.CreateSessionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateSessionMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.v1.BatchCreateSessionsResponse>
        batchCreateSessions(com.google.spanner.v1.BatchCreateSessionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getBatchCreateSessionsMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.spanner.v1.Session>
        getSession(com.google.spanner.v1.GetSessionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetSessionMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.v1.ListSessionsResponse>
        listSessions(com.google.spanner.v1.ListSessionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListSessionsMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty>
        deleteSession(com.google.spanner.v1.DeleteSessionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteSessionMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.spanner.v1.ResultSet>
        executeSql(com.google.spanner.v1.ExecuteSqlRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteSqlMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.v1.ExecuteBatchDmlResponse>
        executeBatchDml(com.google.spanner.v1.ExecuteBatchDmlRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteBatchDmlMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.spanner.v1.ResultSet> read(
        com.google.spanner.v1.ReadRequest request) {
      return futureUnaryCall(getChannel().newCall(getReadMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.spanner.v1.Transaction>
        beginTransaction(com.google.spanner.v1.BeginTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getBeginTransactionMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.spanner.v1.CommitResponse>
        commit(com.google.spanner.v1.CommitRequest request) {
      return futureUnaryCall(getChannel().newCall(getCommitMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> rollback(
        com.google.spanner.v1.RollbackRequest request) {
      return futureUnaryCall(getChannel().newCall(getRollbackMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.v1.PartitionResponse>
        partitionQuery(com.google.spanner.v1.PartitionQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPartitionQueryMethod(), getCallOptions()), request);
    }

    /** */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.spanner.v1.PartitionResponse>
        partitionRead(com.google.spanner.v1.PartitionReadRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPartitionReadMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_SESSION = 0;
  private static final int METHODID_BATCH_CREATE_SESSIONS = 1;
  private static final int METHODID_GET_SESSION = 2;
  private static final int METHODID_LIST_SESSIONS = 3;
  private static final int METHODID_DELETE_SESSION = 4;
  private static final int METHODID_EXECUTE_SQL = 5;
  private static final int METHODID_EXECUTE_STREAMING_SQL = 6;
  private static final int METHODID_EXECUTE_BATCH_DML = 7;
  private static final int METHODID_READ = 8;
  private static final int METHODID_STREAMING_READ = 9;
  private static final int METHODID_BEGIN_TRANSACTION = 10;
  private static final int METHODID_COMMIT = 11;
  private static final int METHODID_ROLLBACK = 12;
  private static final int METHODID_PARTITION_QUERY = 13;
  private static final int METHODID_PARTITION_READ = 14;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SpannerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SpannerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_SESSION:
          serviceImpl.createSession(
              (com.google.spanner.v1.CreateSessionRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.Session>) responseObserver);
          break;
        case METHODID_BATCH_CREATE_SESSIONS:
          serviceImpl.batchCreateSessions(
              (com.google.spanner.v1.BatchCreateSessionsRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.BatchCreateSessionsResponse>)
                  responseObserver);
          break;
        case METHODID_GET_SESSION:
          serviceImpl.getSession(
              (com.google.spanner.v1.GetSessionRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.Session>) responseObserver);
          break;
        case METHODID_LIST_SESSIONS:
          serviceImpl.listSessions(
              (com.google.spanner.v1.ListSessionsRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.ListSessionsResponse>)
                  responseObserver);
          break;
        case METHODID_DELETE_SESSION:
          serviceImpl.deleteSession(
              (com.google.spanner.v1.DeleteSessionRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_EXECUTE_SQL:
          serviceImpl.executeSql(
              (com.google.spanner.v1.ExecuteSqlRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.ResultSet>) responseObserver);
          break;
        case METHODID_EXECUTE_STREAMING_SQL:
          serviceImpl.executeStreamingSql(
              (com.google.spanner.v1.ExecuteSqlRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.PartialResultSet>)
                  responseObserver);
          break;
        case METHODID_EXECUTE_BATCH_DML:
          serviceImpl.executeBatchDml(
              (com.google.spanner.v1.ExecuteBatchDmlRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.ExecuteBatchDmlResponse>)
                  responseObserver);
          break;
        case METHODID_READ:
          serviceImpl.read(
              (com.google.spanner.v1.ReadRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.ResultSet>) responseObserver);
          break;
        case METHODID_STREAMING_READ:
          serviceImpl.streamingRead(
              (com.google.spanner.v1.ReadRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.PartialResultSet>)
                  responseObserver);
          break;
        case METHODID_BEGIN_TRANSACTION:
          serviceImpl.beginTransaction(
              (com.google.spanner.v1.BeginTransactionRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.Transaction>) responseObserver);
          break;
        case METHODID_COMMIT:
          serviceImpl.commit(
              (com.google.spanner.v1.CommitRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.CommitResponse>) responseObserver);
          break;
        case METHODID_ROLLBACK:
          serviceImpl.rollback(
              (com.google.spanner.v1.RollbackRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_PARTITION_QUERY:
          serviceImpl.partitionQuery(
              (com.google.spanner.v1.PartitionQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.PartitionResponse>)
                  responseObserver);
          break;
        case METHODID_PARTITION_READ:
          serviceImpl.partitionRead(
              (com.google.spanner.v1.PartitionReadRequest) request,
              (io.grpc.stub.StreamObserver<com.google.spanner.v1.PartitionResponse>)
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

  private abstract static class SpannerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier,
          io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SpannerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.google.spanner.v1.SpannerProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Spanner");
    }
  }

  private static final class SpannerFileDescriptorSupplier extends SpannerBaseDescriptorSupplier {
    SpannerFileDescriptorSupplier() {}
  }

  private static final class SpannerMethodDescriptorSupplier extends SpannerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SpannerMethodDescriptorSupplier(String methodName) {
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
      synchronized (SpannerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor =
              result =
                  io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                      .setSchemaDescriptor(new SpannerFileDescriptorSupplier())
                      .addMethod(getCreateSessionMethod())
                      .addMethod(getBatchCreateSessionsMethod())
                      .addMethod(getGetSessionMethod())
                      .addMethod(getListSessionsMethod())
                      .addMethod(getDeleteSessionMethod())
                      .addMethod(getExecuteSqlMethod())
                      .addMethod(getExecuteStreamingSqlMethod())
                      .addMethod(getExecuteBatchDmlMethod())
                      .addMethod(getReadMethod())
                      .addMethod(getStreamingReadMethod())
                      .addMethod(getBeginTransactionMethod())
                      .addMethod(getCommitMethod())
                      .addMethod(getRollbackMethod())
                      .addMethod(getPartitionQueryMethod())
                      .addMethod(getPartitionReadMethod())
                      .build();
        }
      }
    }
    return result;
  }
}
