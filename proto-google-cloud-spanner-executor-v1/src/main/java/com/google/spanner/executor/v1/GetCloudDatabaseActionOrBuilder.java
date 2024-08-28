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

// Protobuf Java Version: 3.25.4
package com.google.spanner.executor.v1;

public interface GetCloudDatabaseActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.GetCloudDatabaseAction)
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
   * Cloud instance ID (not path), e.g. "test-instance".
   * </pre>
   *
   * <code>string instance_id = 2;</code>
   *
   * @return The instanceId.
   */
  java.lang.String getInstanceId();
  /**
   *
   *
   * <pre>
   * Cloud instance ID (not path), e.g. "test-instance".
   * </pre>
   *
   * <code>string instance_id = 2;</code>
   *
   * @return The bytes for instanceId.
   */
  com.google.protobuf.ByteString getInstanceIdBytes();

  /**
   *
   *
   * <pre>
   * The id of the database to get, e.g. "db0".
   * </pre>
   *
   * <code>string database_id = 3;</code>
   *
   * @return The databaseId.
   */
  java.lang.String getDatabaseId();
  /**
   *
   *
   * <pre>
   * The id of the database to get, e.g. "db0".
   * </pre>
   *
   * <code>string database_id = 3;</code>
   *
   * @return The bytes for databaseId.
   */
  com.google.protobuf.ByteString getDatabaseIdBytes();
}
