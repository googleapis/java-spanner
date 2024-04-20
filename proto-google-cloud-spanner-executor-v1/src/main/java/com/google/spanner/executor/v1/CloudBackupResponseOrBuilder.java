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

// Protobuf Java Version: 3.25.3
package com.google.spanner.executor.v1;

public interface CloudBackupResponseOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.CloudBackupResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * List of backups returned by ListCloudBackupsAction.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup listed_backups = 1;</code>
   */
  java.util.List<com.google.spanner.admin.database.v1.Backup> getListedBackupsList();
  /**
   *
   *
   * <pre>
   * List of backups returned by ListCloudBackupsAction.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup listed_backups = 1;</code>
   */
  com.google.spanner.admin.database.v1.Backup getListedBackups(int index);
  /**
   *
   *
   * <pre>
   * List of backups returned by ListCloudBackupsAction.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup listed_backups = 1;</code>
   */
  int getListedBackupsCount();
  /**
   *
   *
   * <pre>
   * List of backups returned by ListCloudBackupsAction.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup listed_backups = 1;</code>
   */
  java.util.List<? extends com.google.spanner.admin.database.v1.BackupOrBuilder>
      getListedBackupsOrBuilderList();
  /**
   *
   *
   * <pre>
   * List of backups returned by ListCloudBackupsAction.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup listed_backups = 1;</code>
   */
  com.google.spanner.admin.database.v1.BackupOrBuilder getListedBackupsOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * List of operations returned by ListCloudBackupOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_backup_operations = 2;</code>
   */
  java.util.List<com.google.longrunning.Operation> getListedBackupOperationsList();
  /**
   *
   *
   * <pre>
   * List of operations returned by ListCloudBackupOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_backup_operations = 2;</code>
   */
  com.google.longrunning.Operation getListedBackupOperations(int index);
  /**
   *
   *
   * <pre>
   * List of operations returned by ListCloudBackupOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_backup_operations = 2;</code>
   */
  int getListedBackupOperationsCount();
  /**
   *
   *
   * <pre>
   * List of operations returned by ListCloudBackupOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_backup_operations = 2;</code>
   */
  java.util.List<? extends com.google.longrunning.OperationOrBuilder>
      getListedBackupOperationsOrBuilderList();
  /**
   *
   *
   * <pre>
   * List of operations returned by ListCloudBackupOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_backup_operations = 2;</code>
   */
  com.google.longrunning.OperationOrBuilder getListedBackupOperationsOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * "next_page_token" can be sent in a subsequent list action
   * to fetch more of the matching data.
   * </pre>
   *
   * <code>string next_page_token = 3;</code>
   *
   * @return The nextPageToken.
   */
  java.lang.String getNextPageToken();
  /**
   *
   *
   * <pre>
   * "next_page_token" can be sent in a subsequent list action
   * to fetch more of the matching data.
   * </pre>
   *
   * <code>string next_page_token = 3;</code>
   *
   * @return The bytes for nextPageToken.
   */
  com.google.protobuf.ByteString getNextPageTokenBytes();

  /**
   *
   *
   * <pre>
   * Backup returned by GetCloudBackupAction/UpdateCloudBackupAction.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.Backup backup = 4;</code>
   *
   * @return Whether the backup field is set.
   */
  boolean hasBackup();
  /**
   *
   *
   * <pre>
   * Backup returned by GetCloudBackupAction/UpdateCloudBackupAction.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.Backup backup = 4;</code>
   *
   * @return The backup.
   */
  com.google.spanner.admin.database.v1.Backup getBackup();
  /**
   *
   *
   * <pre>
   * Backup returned by GetCloudBackupAction/UpdateCloudBackupAction.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.Backup backup = 4;</code>
   */
  com.google.spanner.admin.database.v1.BackupOrBuilder getBackupOrBuilder();
}
