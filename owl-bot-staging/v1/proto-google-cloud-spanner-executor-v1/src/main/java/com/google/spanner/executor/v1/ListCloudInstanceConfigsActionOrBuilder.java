// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface ListCloudInstanceConfigsActionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.ListCloudInstanceConfigsAction)
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
   * Number of instance configs to be returned in the response. If 0 or
   * less, defaults to the server's maximum allowed page size.
   * </pre>
   *
   * <code>optional int32 page_size = 2;</code>
   * @return Whether the pageSize field is set.
   */
  boolean hasPageSize();
  /**
   * <pre>
   * Number of instance configs to be returned in the response. If 0 or
   * less, defaults to the server's maximum allowed page size.
   * </pre>
   *
   * <code>optional int32 page_size = 2;</code>
   * @return The pageSize.
   */
  int getPageSize();

  /**
   * <pre>
   * If non-empty, "page_token" should contain a next_page_token
   * from a previous ListInstanceConfigsResponse to the same "parent".
   * </pre>
   *
   * <code>optional string page_token = 3;</code>
   * @return Whether the pageToken field is set.
   */
  boolean hasPageToken();
  /**
   * <pre>
   * If non-empty, "page_token" should contain a next_page_token
   * from a previous ListInstanceConfigsResponse to the same "parent".
   * </pre>
   *
   * <code>optional string page_token = 3;</code>
   * @return The pageToken.
   */
  java.lang.String getPageToken();
  /**
   * <pre>
   * If non-empty, "page_token" should contain a next_page_token
   * from a previous ListInstanceConfigsResponse to the same "parent".
   * </pre>
   *
   * <code>optional string page_token = 3;</code>
   * @return The bytes for pageToken.
   */
  com.google.protobuf.ByteString
      getPageTokenBytes();
}
