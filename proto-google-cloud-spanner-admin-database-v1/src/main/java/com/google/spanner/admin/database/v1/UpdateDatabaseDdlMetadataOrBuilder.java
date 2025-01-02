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
// source: google/spanner/admin/database/v1/spanner_database_admin.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.admin.database.v1;

public interface UpdateDatabaseDdlMetadataOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The database being modified.
   * </pre>
   *
   * <code>string database = 1 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The database.
   */
  java.lang.String getDatabase();
  /**
   *
   *
   * <pre>
   * The database being modified.
   * </pre>
   *
   * <code>string database = 1 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The bytes for database.
   */
  com.google.protobuf.ByteString getDatabaseBytes();

  /**
   *
   *
   * <pre>
   * For an update this list contains all the statements. For an
   * individual statement, this list contains only that statement.
   * </pre>
   *
   * <code>repeated string statements = 2;</code>
   *
   * @return A list containing the statements.
   */
  java.util.List<java.lang.String> getStatementsList();
  /**
   *
   *
   * <pre>
   * For an update this list contains all the statements. For an
   * individual statement, this list contains only that statement.
   * </pre>
   *
   * <code>repeated string statements = 2;</code>
   *
   * @return The count of statements.
   */
  int getStatementsCount();
  /**
   *
   *
   * <pre>
   * For an update this list contains all the statements. For an
   * individual statement, this list contains only that statement.
   * </pre>
   *
   * <code>repeated string statements = 2;</code>
   *
   * @param index The index of the element to return.
   * @return The statements at the given index.
   */
  java.lang.String getStatements(int index);
  /**
   *
   *
   * <pre>
   * For an update this list contains all the statements. For an
   * individual statement, this list contains only that statement.
   * </pre>
   *
   * <code>repeated string statements = 2;</code>
   *
   * @param index The index of the value to return.
   * @return The bytes of the statements at the given index.
   */
  com.google.protobuf.ByteString getStatementsBytes(int index);

  /**
   *
   *
   * <pre>
   * Reports the commit timestamps of all statements that have
   * succeeded so far, where `commit_timestamps[i]` is the commit
   * timestamp for the statement `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.protobuf.Timestamp commit_timestamps = 3;</code>
   */
  java.util.List<com.google.protobuf.Timestamp> getCommitTimestampsList();
  /**
   *
   *
   * <pre>
   * Reports the commit timestamps of all statements that have
   * succeeded so far, where `commit_timestamps[i]` is the commit
   * timestamp for the statement `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.protobuf.Timestamp commit_timestamps = 3;</code>
   */
  com.google.protobuf.Timestamp getCommitTimestamps(int index);
  /**
   *
   *
   * <pre>
   * Reports the commit timestamps of all statements that have
   * succeeded so far, where `commit_timestamps[i]` is the commit
   * timestamp for the statement `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.protobuf.Timestamp commit_timestamps = 3;</code>
   */
  int getCommitTimestampsCount();
  /**
   *
   *
   * <pre>
   * Reports the commit timestamps of all statements that have
   * succeeded so far, where `commit_timestamps[i]` is the commit
   * timestamp for the statement `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.protobuf.Timestamp commit_timestamps = 3;</code>
   */
  java.util.List<? extends com.google.protobuf.TimestampOrBuilder>
      getCommitTimestampsOrBuilderList();
  /**
   *
   *
   * <pre>
   * Reports the commit timestamps of all statements that have
   * succeeded so far, where `commit_timestamps[i]` is the commit
   * timestamp for the statement `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.protobuf.Timestamp commit_timestamps = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getCommitTimestampsOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * Output only. When true, indicates that the operation is throttled e.g.
   * due to resource constraints. When resources become available the operation
   * will resume and this field will be false again.
   * </pre>
   *
   * <code>bool throttled = 4 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   *
   * @return The throttled.
   */
  boolean getThrottled();

  /**
   *
   *
   * <pre>
   * The progress of the
   * [UpdateDatabaseDdl][google.spanner.admin.database.v1.DatabaseAdmin.UpdateDatabaseDdl]
   * operations. All DDL statements will have continuously updating progress,
   * and `progress[i]` is the operation progress for `statements[i]`. Also,
   * `progress[i]` will have start time and end time populated with commit
   * timestamp of operation, as well as a progress of 100% once the operation
   * has completed.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.OperationProgress progress = 5;</code>
   */
  java.util.List<com.google.spanner.admin.database.v1.OperationProgress> getProgressList();
  /**
   *
   *
   * <pre>
   * The progress of the
   * [UpdateDatabaseDdl][google.spanner.admin.database.v1.DatabaseAdmin.UpdateDatabaseDdl]
   * operations. All DDL statements will have continuously updating progress,
   * and `progress[i]` is the operation progress for `statements[i]`. Also,
   * `progress[i]` will have start time and end time populated with commit
   * timestamp of operation, as well as a progress of 100% once the operation
   * has completed.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.OperationProgress progress = 5;</code>
   */
  com.google.spanner.admin.database.v1.OperationProgress getProgress(int index);
  /**
   *
   *
   * <pre>
   * The progress of the
   * [UpdateDatabaseDdl][google.spanner.admin.database.v1.DatabaseAdmin.UpdateDatabaseDdl]
   * operations. All DDL statements will have continuously updating progress,
   * and `progress[i]` is the operation progress for `statements[i]`. Also,
   * `progress[i]` will have start time and end time populated with commit
   * timestamp of operation, as well as a progress of 100% once the operation
   * has completed.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.OperationProgress progress = 5;</code>
   */
  int getProgressCount();
  /**
   *
   *
   * <pre>
   * The progress of the
   * [UpdateDatabaseDdl][google.spanner.admin.database.v1.DatabaseAdmin.UpdateDatabaseDdl]
   * operations. All DDL statements will have continuously updating progress,
   * and `progress[i]` is the operation progress for `statements[i]`. Also,
   * `progress[i]` will have start time and end time populated with commit
   * timestamp of operation, as well as a progress of 100% once the operation
   * has completed.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.OperationProgress progress = 5;</code>
   */
  java.util.List<? extends com.google.spanner.admin.database.v1.OperationProgressOrBuilder>
      getProgressOrBuilderList();
  /**
   *
   *
   * <pre>
   * The progress of the
   * [UpdateDatabaseDdl][google.spanner.admin.database.v1.DatabaseAdmin.UpdateDatabaseDdl]
   * operations. All DDL statements will have continuously updating progress,
   * and `progress[i]` is the operation progress for `statements[i]`. Also,
   * `progress[i]` will have start time and end time populated with commit
   * timestamp of operation, as well as a progress of 100% once the operation
   * has completed.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.OperationProgress progress = 5;</code>
   */
  com.google.spanner.admin.database.v1.OperationProgressOrBuilder getProgressOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * The brief action info for the DDL statements.
   * `actions[i]` is the brief info for `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.DdlStatementActionInfo actions = 6;</code>
   */
  java.util.List<com.google.spanner.admin.database.v1.DdlStatementActionInfo> getActionsList();
  /**
   *
   *
   * <pre>
   * The brief action info for the DDL statements.
   * `actions[i]` is the brief info for `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.DdlStatementActionInfo actions = 6;</code>
   */
  com.google.spanner.admin.database.v1.DdlStatementActionInfo getActions(int index);
  /**
   *
   *
   * <pre>
   * The brief action info for the DDL statements.
   * `actions[i]` is the brief info for `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.DdlStatementActionInfo actions = 6;</code>
   */
  int getActionsCount();
  /**
   *
   *
   * <pre>
   * The brief action info for the DDL statements.
   * `actions[i]` is the brief info for `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.DdlStatementActionInfo actions = 6;</code>
   */
  java.util.List<? extends com.google.spanner.admin.database.v1.DdlStatementActionInfoOrBuilder>
      getActionsOrBuilderList();
  /**
   *
   *
   * <pre>
   * The brief action info for the DDL statements.
   * `actions[i]` is the brief info for `statements[i]`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.DdlStatementActionInfo actions = 6;</code>
   */
  com.google.spanner.admin.database.v1.DdlStatementActionInfoOrBuilder getActionsOrBuilder(
      int index);
}
