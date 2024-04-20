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

public interface GenerateDbPartitionsForReadActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.GenerateDbPartitionsForReadAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Read to generate partitions for.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ReadAction read = 1;</code>
   *
   * @return Whether the read field is set.
   */
  boolean hasRead();
  /**
   *
   *
   * <pre>
   * Read to generate partitions for.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ReadAction read = 1;</code>
   *
   * @return The read.
   */
  com.google.spanner.executor.v1.ReadAction getRead();
  /**
   *
   *
   * <pre>
   * Read to generate partitions for.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ReadAction read = 1;</code>
   */
  com.google.spanner.executor.v1.ReadActionOrBuilder getReadOrBuilder();

  /**
   *
   *
   * <pre>
   * Metadata related to the tables involved in the read.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.TableMetadata table = 2;</code>
   */
  java.util.List<com.google.spanner.executor.v1.TableMetadata> getTableList();
  /**
   *
   *
   * <pre>
   * Metadata related to the tables involved in the read.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.TableMetadata table = 2;</code>
   */
  com.google.spanner.executor.v1.TableMetadata getTable(int index);
  /**
   *
   *
   * <pre>
   * Metadata related to the tables involved in the read.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.TableMetadata table = 2;</code>
   */
  int getTableCount();
  /**
   *
   *
   * <pre>
   * Metadata related to the tables involved in the read.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.TableMetadata table = 2;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.TableMetadataOrBuilder>
      getTableOrBuilderList();
  /**
   *
   *
   * <pre>
   * Metadata related to the tables involved in the read.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.TableMetadata table = 2;</code>
   */
  com.google.spanner.executor.v1.TableMetadataOrBuilder getTableOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * Desired size of data in each partition. Spanner doesn't guarantee to
   * respect this value.
   * </pre>
   *
   * <code>optional int64 desired_bytes_per_partition = 3;</code>
   *
   * @return Whether the desiredBytesPerPartition field is set.
   */
  boolean hasDesiredBytesPerPartition();
  /**
   *
   *
   * <pre>
   * Desired size of data in each partition. Spanner doesn't guarantee to
   * respect this value.
   * </pre>
   *
   * <code>optional int64 desired_bytes_per_partition = 3;</code>
   *
   * @return The desiredBytesPerPartition.
   */
  long getDesiredBytesPerPartition();

  /**
   *
   *
   * <pre>
   * If set, the desired max number of partitions. Spanner doesn't guarantee to
   * respect this value.
   * </pre>
   *
   * <code>optional int64 max_partition_count = 4;</code>
   *
   * @return Whether the maxPartitionCount field is set.
   */
  boolean hasMaxPartitionCount();
  /**
   *
   *
   * <pre>
   * If set, the desired max number of partitions. Spanner doesn't guarantee to
   * respect this value.
   * </pre>
   *
   * <code>optional int64 max_partition_count = 4;</code>
   *
   * @return The maxPartitionCount.
   */
  long getMaxPartitionCount();
}
