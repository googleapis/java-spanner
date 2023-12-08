// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface GetCloudInstanceActionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.GetCloudInstanceAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Cloud project ID, e.g. "spanner-cloud-systest".
   * </pre>
   *
   * <code>string project_id = 1;</code>
   * @return The projectId.
   */
  java.lang.String getProjectId();
  /**
   * <pre>
   * Cloud project ID, e.g. "spanner-cloud-systest".
   * </pre>
   *
   * <code>string project_id = 1;</code>
   * @return The bytes for projectId.
   */
  com.google.protobuf.ByteString
      getProjectIdBytes();

  /**
   * <pre>
   * Cloud instance ID (not path) to retrieve the instance from,
   * e.g. "test-instance".
   * </pre>
   *
   * <code>string instance_id = 2;</code>
   * @return The instanceId.
   */
  java.lang.String getInstanceId();
  /**
   * <pre>
   * Cloud instance ID (not path) to retrieve the instance from,
   * e.g. "test-instance".
   * </pre>
   *
   * <code>string instance_id = 2;</code>
   * @return The bytes for instanceId.
   */
  com.google.protobuf.ByteString
      getInstanceIdBytes();
}
