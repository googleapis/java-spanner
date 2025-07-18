/*
 * Copyright 2025 Google LLC
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

// Protobuf Java Version: 3.25.8
package com.google.spanner.executor.v1;

public interface CreateUserInstanceConfigActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.CreateUserInstanceConfigAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * User instance config ID (not path), e.g. "custom-config".
   * </pre>
   *
   * <code>string user_config_id = 1;</code>
   *
   * @return The userConfigId.
   */
  java.lang.String getUserConfigId();

  /**
   *
   *
   * <pre>
   * User instance config ID (not path), e.g. "custom-config".
   * </pre>
   *
   * <code>string user_config_id = 1;</code>
   *
   * @return The bytes for userConfigId.
   */
  com.google.protobuf.ByteString getUserConfigIdBytes();

  /**
   *
   *
   * <pre>
   * Cloud project ID, e.g. "spanner-cloud-systest".
   * </pre>
   *
   * <code>string project_id = 2;</code>
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
   * <code>string project_id = 2;</code>
   *
   * @return The bytes for projectId.
   */
  com.google.protobuf.ByteString getProjectIdBytes();

  /**
   *
   *
   * <pre>
   * Base config ID, e.g. "test-config".
   * </pre>
   *
   * <code>string base_config_id = 3;</code>
   *
   * @return The baseConfigId.
   */
  java.lang.String getBaseConfigId();

  /**
   *
   *
   * <pre>
   * Base config ID, e.g. "test-config".
   * </pre>
   *
   * <code>string base_config_id = 3;</code>
   *
   * @return The bytes for baseConfigId.
   */
  com.google.protobuf.ByteString getBaseConfigIdBytes();

  /**
   *
   *
   * <pre>
   * Replicas that should be included in the user config.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 4;</code>
   */
  java.util.List<com.google.spanner.admin.instance.v1.ReplicaInfo> getReplicasList();

  /**
   *
   *
   * <pre>
   * Replicas that should be included in the user config.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 4;</code>
   */
  com.google.spanner.admin.instance.v1.ReplicaInfo getReplicas(int index);

  /**
   *
   *
   * <pre>
   * Replicas that should be included in the user config.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 4;</code>
   */
  int getReplicasCount();

  /**
   *
   *
   * <pre>
   * Replicas that should be included in the user config.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 4;</code>
   */
  java.util.List<? extends com.google.spanner.admin.instance.v1.ReplicaInfoOrBuilder>
      getReplicasOrBuilderList();

  /**
   *
   *
   * <pre>
   * Replicas that should be included in the user config.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 4;</code>
   */
  com.google.spanner.admin.instance.v1.ReplicaInfoOrBuilder getReplicasOrBuilder(int index);
}
