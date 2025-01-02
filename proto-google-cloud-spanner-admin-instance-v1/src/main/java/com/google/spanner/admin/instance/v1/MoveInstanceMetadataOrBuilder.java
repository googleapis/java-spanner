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
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.admin.instance.v1;

public interface MoveInstanceMetadataOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.MoveInstanceMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The target instance configuration where to move the instance.
   * Values are of the form `projects/&lt;project&gt;/instanceConfigs/&lt;config&gt;`.
   * </pre>
   *
   * <code>string target_config = 1;</code>
   *
   * @return The targetConfig.
   */
  java.lang.String getTargetConfig();
  /**
   *
   *
   * <pre>
   * The target instance configuration where to move the instance.
   * Values are of the form `projects/&lt;project&gt;/instanceConfigs/&lt;config&gt;`.
   * </pre>
   *
   * <code>string target_config = 1;</code>
   *
   * @return The bytes for targetConfig.
   */
  com.google.protobuf.ByteString getTargetConfigBytes();

  /**
   *
   *
   * <pre>
   * The progress of the
   * [MoveInstance][google.spanner.admin.instance.v1.InstanceAdmin.MoveInstance]
   * operation.
   * [progress_percent][google.spanner.admin.instance.v1.OperationProgress.progress_percent]
   * is reset when cancellation is requested.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.OperationProgress progress = 2;</code>
   *
   * @return Whether the progress field is set.
   */
  boolean hasProgress();
  /**
   *
   *
   * <pre>
   * The progress of the
   * [MoveInstance][google.spanner.admin.instance.v1.InstanceAdmin.MoveInstance]
   * operation.
   * [progress_percent][google.spanner.admin.instance.v1.OperationProgress.progress_percent]
   * is reset when cancellation is requested.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.OperationProgress progress = 2;</code>
   *
   * @return The progress.
   */
  com.google.spanner.admin.instance.v1.OperationProgress getProgress();
  /**
   *
   *
   * <pre>
   * The progress of the
   * [MoveInstance][google.spanner.admin.instance.v1.InstanceAdmin.MoveInstance]
   * operation.
   * [progress_percent][google.spanner.admin.instance.v1.OperationProgress.progress_percent]
   * is reset when cancellation is requested.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.OperationProgress progress = 2;</code>
   */
  com.google.spanner.admin.instance.v1.OperationProgressOrBuilder getProgressOrBuilder();

  /**
   *
   *
   * <pre>
   * The time at which this operation was cancelled.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 3;</code>
   *
   * @return Whether the cancelTime field is set.
   */
  boolean hasCancelTime();
  /**
   *
   *
   * <pre>
   * The time at which this operation was cancelled.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 3;</code>
   *
   * @return The cancelTime.
   */
  com.google.protobuf.Timestamp getCancelTime();
  /**
   *
   *
   * <pre>
   * The time at which this operation was cancelled.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getCancelTimeOrBuilder();
}
