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
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/admin/database/v1/spanner_database_admin.proto

package com.google.spanner.admin.database.v1;

public interface RestoreInfoOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.RestoreInfo)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The type of the restore source.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.RestoreSourceType source_type = 1;</code>
   *
   * @return The enum numeric value on the wire for sourceType.
   */
  int getSourceTypeValue();
  /**
   *
   *
   * <pre>
   * The type of the restore source.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.RestoreSourceType source_type = 1;</code>
   *
   * @return The sourceType.
   */
  com.google.spanner.admin.database.v1.RestoreSourceType getSourceType();

  /**
   *
   *
   * <pre>
   * Information about the backup used to restore the database. The backup
   * may no longer exist.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.BackupInfo backup_info = 2;</code>
   *
   * @return Whether the backupInfo field is set.
   */
  boolean hasBackupInfo();
  /**
   *
   *
   * <pre>
   * Information about the backup used to restore the database. The backup
   * may no longer exist.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.BackupInfo backup_info = 2;</code>
   *
   * @return The backupInfo.
   */
  com.google.spanner.admin.database.v1.BackupInfo getBackupInfo();
  /**
   *
   *
   * <pre>
   * Information about the backup used to restore the database. The backup
   * may no longer exist.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.BackupInfo backup_info = 2;</code>
   */
  com.google.spanner.admin.database.v1.BackupInfoOrBuilder getBackupInfoOrBuilder();

  com.google.spanner.admin.database.v1.RestoreInfo.SourceInfoCase getSourceInfoCase();
}
