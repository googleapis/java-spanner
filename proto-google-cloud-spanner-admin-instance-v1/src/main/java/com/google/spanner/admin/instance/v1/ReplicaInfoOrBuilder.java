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
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.admin.instance.v1;

public interface ReplicaInfoOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.ReplicaInfo)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The location of the serving resources, e.g., "us-central1".
   * </pre>
   *
   * <code>string location = 1;</code>
   *
   * @return The location.
   */
  java.lang.String getLocation();
  /**
   *
   *
   * <pre>
   * The location of the serving resources, e.g., "us-central1".
   * </pre>
   *
   * <code>string location = 1;</code>
   *
   * @return The bytes for location.
   */
  com.google.protobuf.ByteString getLocationBytes();

  /**
   *
   *
   * <pre>
   * The type of replica.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.ReplicaInfo.ReplicaType type = 2;</code>
   *
   * @return The enum numeric value on the wire for type.
   */
  int getTypeValue();
  /**
   *
   *
   * <pre>
   * The type of replica.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.ReplicaInfo.ReplicaType type = 2;</code>
   *
   * @return The type.
   */
  com.google.spanner.admin.instance.v1.ReplicaInfo.ReplicaType getType();

  /**
   *
   *
   * <pre>
   * If true, this location is designated as the default leader location where
   * leader replicas are placed. See the [region types
   * documentation](https://cloud.google.com/spanner/docs/instances#region_types)
   * for more details.
   * </pre>
   *
   * <code>bool default_leader_location = 3;</code>
   *
   * @return The defaultLeaderLocation.
   */
  boolean getDefaultLeaderLocation();
}
