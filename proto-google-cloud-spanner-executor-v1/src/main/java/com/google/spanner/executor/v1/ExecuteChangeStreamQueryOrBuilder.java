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

// Protobuf Java Version: 3.25.5
package com.google.spanner.executor.v1;

public interface ExecuteChangeStreamQueryOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.ExecuteChangeStreamQuery)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Name for this change stream.
   * </pre>
   *
   * <code>string name = 1;</code>
   *
   * @return The name.
   */
  java.lang.String getName();
  /**
   *
   *
   * <pre>
   * Name for this change stream.
   * </pre>
   *
   * <code>string name = 1;</code>
   *
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   *
   *
   * <pre>
   * Specifies that records with commit_timestamp greater than or equal to
   * start_time should be returned.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp start_time = 2;</code>
   *
   * @return Whether the startTime field is set.
   */
  boolean hasStartTime();
  /**
   *
   *
   * <pre>
   * Specifies that records with commit_timestamp greater than or equal to
   * start_time should be returned.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp start_time = 2;</code>
   *
   * @return The startTime.
   */
  com.google.protobuf.Timestamp getStartTime();
  /**
   *
   *
   * <pre>
   * Specifies that records with commit_timestamp greater than or equal to
   * start_time should be returned.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp start_time = 2;</code>
   */
  com.google.protobuf.TimestampOrBuilder getStartTimeOrBuilder();

  /**
   *
   *
   * <pre>
   * Specifies that records with commit_timestamp less than or equal to
   * end_time should be returned.
   * </pre>
   *
   * <code>optional .google.protobuf.Timestamp end_time = 3;</code>
   *
   * @return Whether the endTime field is set.
   */
  boolean hasEndTime();
  /**
   *
   *
   * <pre>
   * Specifies that records with commit_timestamp less than or equal to
   * end_time should be returned.
   * </pre>
   *
   * <code>optional .google.protobuf.Timestamp end_time = 3;</code>
   *
   * @return The endTime.
   */
  com.google.protobuf.Timestamp getEndTime();
  /**
   *
   *
   * <pre>
   * Specifies that records with commit_timestamp less than or equal to
   * end_time should be returned.
   * </pre>
   *
   * <code>optional .google.protobuf.Timestamp end_time = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getEndTimeOrBuilder();

  /**
   *
   *
   * <pre>
   * Specifies which change stream partition to query, based on the content of
   * child partitions records.
   * </pre>
   *
   * <code>optional string partition_token = 4;</code>
   *
   * @return Whether the partitionToken field is set.
   */
  boolean hasPartitionToken();
  /**
   *
   *
   * <pre>
   * Specifies which change stream partition to query, based on the content of
   * child partitions records.
   * </pre>
   *
   * <code>optional string partition_token = 4;</code>
   *
   * @return The partitionToken.
   */
  java.lang.String getPartitionToken();
  /**
   *
   *
   * <pre>
   * Specifies which change stream partition to query, based on the content of
   * child partitions records.
   * </pre>
   *
   * <code>optional string partition_token = 4;</code>
   *
   * @return The bytes for partitionToken.
   */
  com.google.protobuf.ByteString getPartitionTokenBytes();

  /**
   *
   *
   * <pre>
   * Read options for this change stream query.
   * </pre>
   *
   * <code>repeated string read_options = 5;</code>
   *
   * @return A list containing the readOptions.
   */
  java.util.List<java.lang.String> getReadOptionsList();
  /**
   *
   *
   * <pre>
   * Read options for this change stream query.
   * </pre>
   *
   * <code>repeated string read_options = 5;</code>
   *
   * @return The count of readOptions.
   */
  int getReadOptionsCount();
  /**
   *
   *
   * <pre>
   * Read options for this change stream query.
   * </pre>
   *
   * <code>repeated string read_options = 5;</code>
   *
   * @param index The index of the element to return.
   * @return The readOptions at the given index.
   */
  java.lang.String getReadOptions(int index);
  /**
   *
   *
   * <pre>
   * Read options for this change stream query.
   * </pre>
   *
   * <code>repeated string read_options = 5;</code>
   *
   * @param index The index of the value to return.
   * @return The bytes of the readOptions at the given index.
   */
  com.google.protobuf.ByteString getReadOptionsBytes(int index);

  /**
   *
   *
   * <pre>
   * Determines how frequently a heartbeat ChangeRecord will be returned in case
   * there are no transactions committed in this partition, in milliseconds.
   * </pre>
   *
   * <code>optional int32 heartbeat_milliseconds = 6;</code>
   *
   * @return Whether the heartbeatMilliseconds field is set.
   */
  boolean hasHeartbeatMilliseconds();
  /**
   *
   *
   * <pre>
   * Determines how frequently a heartbeat ChangeRecord will be returned in case
   * there are no transactions committed in this partition, in milliseconds.
   * </pre>
   *
   * <code>optional int32 heartbeat_milliseconds = 6;</code>
   *
   * @return The heartbeatMilliseconds.
   */
  int getHeartbeatMilliseconds();

  /**
   *
   *
   * <pre>
   * Deadline for this change stream query, in seconds.
   * </pre>
   *
   * <code>optional int64 deadline_seconds = 7;</code>
   *
   * @return Whether the deadlineSeconds field is set.
   */
  boolean hasDeadlineSeconds();
  /**
   *
   *
   * <pre>
   * Deadline for this change stream query, in seconds.
   * </pre>
   *
   * <code>optional int64 deadline_seconds = 7;</code>
   *
   * @return The deadlineSeconds.
   */
  long getDeadlineSeconds();

  /**
   *
   *
   * <pre>
   * Database role to assume while performing this action. This should only be
   * set for cloud requests. Setting the database role will enforce additional
   * role-based access checks on this action.
   * </pre>
   *
   * <code>optional string cloud_database_role = 8;</code>
   *
   * @return Whether the cloudDatabaseRole field is set.
   */
  boolean hasCloudDatabaseRole();
  /**
   *
   *
   * <pre>
   * Database role to assume while performing this action. This should only be
   * set for cloud requests. Setting the database role will enforce additional
   * role-based access checks on this action.
   * </pre>
   *
   * <code>optional string cloud_database_role = 8;</code>
   *
   * @return The cloudDatabaseRole.
   */
  java.lang.String getCloudDatabaseRole();
  /**
   *
   *
   * <pre>
   * Database role to assume while performing this action. This should only be
   * set for cloud requests. Setting the database role will enforce additional
   * role-based access checks on this action.
   * </pre>
   *
   * <code>optional string cloud_database_role = 8;</code>
   *
   * @return The bytes for cloudDatabaseRole.
   */
  com.google.protobuf.ByteString getCloudDatabaseRoleBytes();
}
