/*
 * Copyright 2024 Google LLC
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
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.executor.v1;

public interface ListCloudInstancesActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.ListCloudInstancesAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Cloud project ID, e.g. "spanner-cloud-systest".
   * </pre>
   *
   * <code>string project_id = 1;</code>
   *
   * @return The projectId.
   */
  java.lang.String getProjectId();
  /**
   *
   *
   * <pre>
   * Cloud project ID, e.g. "spanner-cloud-systest".
   * </pre>
   *
   * <code>string project_id = 1;</code>
   *
   * @return The bytes for projectId.
   */
  com.google.protobuf.ByteString getProjectIdBytes();

  /**
   *
   *
   * <pre>
   * A filter expression that filters what operations are returned in the
   * response.
   * The expression must specify the field name, a comparison operator,
   * and the value that you want to use for filtering.
   * Refer spanner_instance_admin.proto.ListInstancesRequest for
   * detail.
   * </pre>
   *
   * <code>optional string filter = 2;</code>
   *
   * @return Whether the filter field is set.
   */
  boolean hasFilter();
  /**
   *
   *
   * <pre>
   * A filter expression that filters what operations are returned in the
   * response.
   * The expression must specify the field name, a comparison operator,
   * and the value that you want to use for filtering.
   * Refer spanner_instance_admin.proto.ListInstancesRequest for
   * detail.
   * </pre>
   *
   * <code>optional string filter = 2;</code>
   *
   * @return The filter.
   */
  java.lang.String getFilter();
  /**
   *
   *
   * <pre>
   * A filter expression that filters what operations are returned in the
   * response.
   * The expression must specify the field name, a comparison operator,
   * and the value that you want to use for filtering.
   * Refer spanner_instance_admin.proto.ListInstancesRequest for
   * detail.
   * </pre>
   *
   * <code>optional string filter = 2;</code>
   *
   * @return The bytes for filter.
   */
  com.google.protobuf.ByteString getFilterBytes();

  /**
   *
   *
   * <pre>
   * Number of instances to be returned in the response. If 0 or
   * less, defaults to the server's maximum allowed page size.
   * </pre>
   *
   * <code>optional int32 page_size = 3;</code>
   *
   * @return Whether the pageSize field is set.
   */
  boolean hasPageSize();
  /**
   *
   *
   * <pre>
   * Number of instances to be returned in the response. If 0 or
   * less, defaults to the server's maximum allowed page size.
   * </pre>
   *
   * <code>optional int32 page_size = 3;</code>
   *
   * @return The pageSize.
   */
  int getPageSize();

  /**
   *
   *
   * <pre>
   * If non-empty, "page_token" should contain a next_page_token
   * from a previous ListInstancesResponse to the same "parent"
   * and with the same "filter".
   * </pre>
   *
   * <code>optional string page_token = 4;</code>
   *
   * @return Whether the pageToken field is set.
   */
  boolean hasPageToken();
  /**
   *
   *
   * <pre>
   * If non-empty, "page_token" should contain a next_page_token
   * from a previous ListInstancesResponse to the same "parent"
   * and with the same "filter".
   * </pre>
   *
   * <code>optional string page_token = 4;</code>
   *
   * @return The pageToken.
   */
  java.lang.String getPageToken();
  /**
   *
   *
   * <pre>
   * If non-empty, "page_token" should contain a next_page_token
   * from a previous ListInstancesResponse to the same "parent"
   * and with the same "filter".
   * </pre>
   *
   * <code>optional string page_token = 4;</code>
   *
   * @return The bytes for pageToken.
   */
  com.google.protobuf.ByteString getPageTokenBytes();
}
