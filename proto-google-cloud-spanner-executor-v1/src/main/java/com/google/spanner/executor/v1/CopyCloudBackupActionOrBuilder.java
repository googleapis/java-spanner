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

public interface CopyCloudBackupActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.CopyCloudBackupAction)
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
   * The id of the backup to be created, e.g. "test-backup".
   * </pre>
   *
   * <code>string backup_id = 3;</code>
   *
   * @return The backupId.
   */
  java.lang.String getBackupId();
  /**
   *
   *
   * <pre>
   * The id of the backup to be created, e.g. "test-backup".
   * </pre>
   *
   * <code>string backup_id = 3;</code>
   *
   * @return The bytes for backupId.
   */
  com.google.protobuf.ByteString getBackupIdBytes();

  /**
   *
   *
   * <pre>
   * The fully qualified uri of the source backup from which this
   * backup was copied. eg.
   * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
   * </pre>
   *
   * <code>string source_backup = 4;</code>
   *
   * @return The sourceBackup.
   */
  java.lang.String getSourceBackup();
  /**
   *
   *
   * <pre>
   * The fully qualified uri of the source backup from which this
   * backup was copied. eg.
   * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
   * </pre>
   *
   * <code>string source_backup = 4;</code>
   *
   * @return The bytes for sourceBackup.
   */
  com.google.protobuf.ByteString getSourceBackupBytes();

  /**
   *
   *
   * <pre>
   * Output only. The expiration time of the backup, which must be at least 6
   * hours and at most 366 days from the time the request is received.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp expire_time = 5 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return Whether the expireTime field is set.
   */
  boolean hasExpireTime();
  /**
   *
   *
   * <pre>
   * Output only. The expiration time of the backup, which must be at least 6
   * hours and at most 366 days from the time the request is received.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp expire_time = 5 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The expireTime.
   */
  com.google.protobuf.Timestamp getExpireTime();
  /**
   *
   *
   * <pre>
   * Output only. The expiration time of the backup, which must be at least 6
   * hours and at most 366 days from the time the request is received.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp expire_time = 5 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  com.google.protobuf.TimestampOrBuilder getExpireTimeOrBuilder();
}
