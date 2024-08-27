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

public interface BatchPartitionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.BatchPartition)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Serialized Partition instance.
   * </pre>
   *
   * <code>bytes partition = 1;</code>
   *
   * @return The partition.
   */
  com.google.protobuf.ByteString getPartition();

  /**
   *
   *
   * <pre>
   * The partition token decrypted from partition.
   * </pre>
   *
   * <code>bytes partition_token = 2;</code>
   *
   * @return The partitionToken.
   */
  com.google.protobuf.ByteString getPartitionToken();

  /**
   *
   *
   * <pre>
   * Table name is set iff the partition was generated for a read (as opposed to
   * a query).
   * </pre>
   *
   * <code>optional string table = 3;</code>
   *
   * @return Whether the table field is set.
   */
  boolean hasTable();
  /**
   *
   *
   * <pre>
   * Table name is set iff the partition was generated for a read (as opposed to
   * a query).
   * </pre>
   *
   * <code>optional string table = 3;</code>
   *
   * @return The table.
   */
  java.lang.String getTable();
  /**
   *
   *
   * <pre>
   * Table name is set iff the partition was generated for a read (as opposed to
   * a query).
   * </pre>
   *
   * <code>optional string table = 3;</code>
   *
   * @return The bytes for table.
   */
  com.google.protobuf.ByteString getTableBytes();

  /**
   *
   *
   * <pre>
   * Index name if the partition was generated for an index read.
   * </pre>
   *
   * <code>optional string index = 4;</code>
   *
   * @return Whether the index field is set.
   */
  boolean hasIndex();
  /**
   *
   *
   * <pre>
   * Index name if the partition was generated for an index read.
   * </pre>
   *
   * <code>optional string index = 4;</code>
   *
   * @return The index.
   */
  java.lang.String getIndex();
  /**
   *
   *
   * <pre>
   * Index name if the partition was generated for an index read.
   * </pre>
   *
   * <code>optional string index = 4;</code>
   *
   * @return The bytes for index.
   */
  com.google.protobuf.ByteString getIndexBytes();
}
