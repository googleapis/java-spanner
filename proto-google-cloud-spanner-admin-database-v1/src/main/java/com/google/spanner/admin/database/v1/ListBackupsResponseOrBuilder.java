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
// source: google/spanner/admin/database/v1/backup.proto

package com.google.spanner.admin.database.v1;

public interface ListBackupsResponseOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.ListBackupsResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The list of matching backups. Backups returned are ordered by `create_time`
   * in descending order, starting from the most recent `create_time`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup backups = 1;</code>
   */
  java.util.List<com.google.spanner.admin.database.v1.Backup> getBackupsList();
  /**
   *
   *
   * <pre>
   * The list of matching backups. Backups returned are ordered by `create_time`
   * in descending order, starting from the most recent `create_time`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup backups = 1;</code>
   */
  com.google.spanner.admin.database.v1.Backup getBackups(int index);
  /**
   *
   *
   * <pre>
   * The list of matching backups. Backups returned are ordered by `create_time`
   * in descending order, starting from the most recent `create_time`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup backups = 1;</code>
   */
  int getBackupsCount();
  /**
   *
   *
   * <pre>
   * The list of matching backups. Backups returned are ordered by `create_time`
   * in descending order, starting from the most recent `create_time`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup backups = 1;</code>
   */
  java.util.List<? extends com.google.spanner.admin.database.v1.BackupOrBuilder>
      getBackupsOrBuilderList();
  /**
   *
   *
   * <pre>
   * The list of matching backups. Backups returned are ordered by `create_time`
   * in descending order, starting from the most recent `create_time`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Backup backups = 1;</code>
   */
  com.google.spanner.admin.database.v1.BackupOrBuilder getBackupsOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListBackups][google.spanner.admin.database.v1.DatabaseAdmin.ListBackups] call to fetch more
   * of the matching backups.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The nextPageToken.
   */
  java.lang.String getNextPageToken();
  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListBackups][google.spanner.admin.database.v1.DatabaseAdmin.ListBackups] call to fetch more
   * of the matching backups.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The bytes for nextPageToken.
   */
  com.google.protobuf.ByteString getNextPageTokenBytes();
}
