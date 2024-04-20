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
// source: google/spanner/v1/spanner.proto

// Protobuf Java Version: 3.25.3
package com.google.spanner.v1;

public interface BatchWriteResponseOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.BatchWriteResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The mutation groups applied in this batch. The values index into the
   * `mutation_groups` field in the corresponding `BatchWriteRequest`.
   * </pre>
   *
   * <code>repeated int32 indexes = 1;</code>
   *
   * @return A list containing the indexes.
   */
  java.util.List<java.lang.Integer> getIndexesList();
  /**
   *
   *
   * <pre>
   * The mutation groups applied in this batch. The values index into the
   * `mutation_groups` field in the corresponding `BatchWriteRequest`.
   * </pre>
   *
   * <code>repeated int32 indexes = 1;</code>
   *
   * @return The count of indexes.
   */
  int getIndexesCount();
  /**
   *
   *
   * <pre>
   * The mutation groups applied in this batch. The values index into the
   * `mutation_groups` field in the corresponding `BatchWriteRequest`.
   * </pre>
   *
   * <code>repeated int32 indexes = 1;</code>
   *
   * @param index The index of the element to return.
   * @return The indexes at the given index.
   */
  int getIndexes(int index);

  /**
   *
   *
   * <pre>
   * An `OK` status indicates success. Any other status indicates a failure.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   *
   * @return Whether the status field is set.
   */
  boolean hasStatus();
  /**
   *
   *
   * <pre>
   * An `OK` status indicates success. Any other status indicates a failure.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   *
   * @return The status.
   */
  com.google.rpc.Status getStatus();
  /**
   *
   *
   * <pre>
   * An `OK` status indicates success. Any other status indicates a failure.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   */
  com.google.rpc.StatusOrBuilder getStatusOrBuilder();

  /**
   *
   *
   * <pre>
   * The commit timestamp of the transaction that applied this batch.
   * Present if `status` is `OK`, absent otherwise.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
   *
   * @return Whether the commitTimestamp field is set.
   */
  boolean hasCommitTimestamp();
  /**
   *
   *
   * <pre>
   * The commit timestamp of the transaction that applied this batch.
   * Present if `status` is `OK`, absent otherwise.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
   *
   * @return The commitTimestamp.
   */
  com.google.protobuf.Timestamp getCommitTimestamp();
  /**
   *
   *
   * <pre>
   * The commit timestamp of the transaction that applied this batch.
   * Present if `status` is `OK`, absent otherwise.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getCommitTimestampOrBuilder();
}
