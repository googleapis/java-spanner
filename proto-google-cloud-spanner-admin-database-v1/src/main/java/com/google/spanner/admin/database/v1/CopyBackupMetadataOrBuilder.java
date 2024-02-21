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
// source: google/spanner/admin/database/v1/backup.proto

// Protobuf Java Version: 3.25.2
package com.google.spanner.admin.database.v1;

public interface CopyBackupMetadataOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.CopyBackupMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The name of the backup being created through the copy operation.
   * Values are of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The name.
   */
  java.lang.String getName();
  /**
   *
   *
   * <pre>
   * The name of the backup being created through the copy operation.
   * Values are of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   *
   *
   * <pre>
   * The name of the source backup that is being copied.
   * Values are of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * </pre>
   *
   * <code>string source_backup = 2 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The sourceBackup.
   */
  java.lang.String getSourceBackup();
  /**
   *
   *
   * <pre>
   * The name of the source backup that is being copied.
   * Values are of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * </pre>
   *
   * <code>string source_backup = 2 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The bytes for sourceBackup.
   */
  com.google.protobuf.ByteString getSourceBackupBytes();

  /**
   *
   *
   * <pre>
   * The progress of the
   * [CopyBackup][google.spanner.admin.database.v1.DatabaseAdmin.CopyBackup] operation.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 3;</code>
   *
   * @return Whether the progress field is set.
   */
  boolean hasProgress();
  /**
   *
   *
   * <pre>
   * The progress of the
   * [CopyBackup][google.spanner.admin.database.v1.DatabaseAdmin.CopyBackup] operation.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 3;</code>
   *
   * @return The progress.
   */
  com.google.spanner.admin.database.v1.OperationProgress getProgress();
  /**
   *
   *
   * <pre>
   * The progress of the
   * [CopyBackup][google.spanner.admin.database.v1.DatabaseAdmin.CopyBackup] operation.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 3;</code>
   */
  com.google.spanner.admin.database.v1.OperationProgressOrBuilder getProgressOrBuilder();

  /**
   *
   *
   * <pre>
   * The time at which cancellation of CopyBackup operation was received.
   * [Operations.CancelOperation][google.longrunning.Operations.CancelOperation]
   * starts asynchronous cancellation on a long-running operation. The server
   * makes a best effort to cancel the operation, but success is not guaranteed.
   * Clients can use
   * [Operations.GetOperation][google.longrunning.Operations.GetOperation] or
   * other methods to check whether the cancellation succeeded or whether the
   * operation completed despite cancellation. On successful cancellation,
   * the operation is not deleted; instead, it becomes an operation with
   * an [Operation.error][google.longrunning.Operation.error] value with a
   * [google.rpc.Status.code][google.rpc.Status.code] of 1,
   * corresponding to `Code.CANCELLED`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 4;</code>
   *
   * @return Whether the cancelTime field is set.
   */
  boolean hasCancelTime();
  /**
   *
   *
   * <pre>
   * The time at which cancellation of CopyBackup operation was received.
   * [Operations.CancelOperation][google.longrunning.Operations.CancelOperation]
   * starts asynchronous cancellation on a long-running operation. The server
   * makes a best effort to cancel the operation, but success is not guaranteed.
   * Clients can use
   * [Operations.GetOperation][google.longrunning.Operations.GetOperation] or
   * other methods to check whether the cancellation succeeded or whether the
   * operation completed despite cancellation. On successful cancellation,
   * the operation is not deleted; instead, it becomes an operation with
   * an [Operation.error][google.longrunning.Operation.error] value with a
   * [google.rpc.Status.code][google.rpc.Status.code] of 1,
   * corresponding to `Code.CANCELLED`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 4;</code>
   *
   * @return The cancelTime.
   */
  com.google.protobuf.Timestamp getCancelTime();
  /**
   *
   *
   * <pre>
   * The time at which cancellation of CopyBackup operation was received.
   * [Operations.CancelOperation][google.longrunning.Operations.CancelOperation]
   * starts asynchronous cancellation on a long-running operation. The server
   * makes a best effort to cancel the operation, but success is not guaranteed.
   * Clients can use
   * [Operations.GetOperation][google.longrunning.Operations.GetOperation] or
   * other methods to check whether the cancellation succeeded or whether the
   * operation completed despite cancellation. On successful cancellation,
   * the operation is not deleted; instead, it becomes an operation with
   * an [Operation.error][google.longrunning.Operation.error] value with a
   * [google.rpc.Status.code][google.rpc.Status.code] of 1,
   * corresponding to `Code.CANCELLED`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 4;</code>
   */
  com.google.protobuf.TimestampOrBuilder getCancelTimeOrBuilder();
}
