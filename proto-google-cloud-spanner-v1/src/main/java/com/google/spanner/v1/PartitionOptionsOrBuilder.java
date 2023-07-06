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
// source: google/spanner/v1/spanner.proto

package com.google.spanner.v1;

public interface PartitionOptionsOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.PartitionOptions)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * **Note:** This hint is currently ignored by PartitionQuery and
   * PartitionRead requests.
   * The desired data size for each partition generated.  The default for this
   * option is currently 1 GiB.  This is only a hint. The actual size of each
   * partition may be smaller or larger than this size request.
   * </pre>
   *
   * <code>int64 partition_size_bytes = 1;</code>
   *
   * @return The partitionSizeBytes.
   */
  long getPartitionSizeBytes();

  /**
   *
   *
   * <pre>
   * **Note:** This hint is currently ignored by PartitionQuery and
   * PartitionRead requests.
   * The desired maximum number of partitions to return.  For example, this may
   * be set to the number of workers available.  The default for this option
   * is currently 10,000. The maximum value is currently 200,000.  This is only
   * a hint.  The actual number of partitions returned may be smaller or larger
   * than this maximum count request.
   * </pre>
   *
   * <code>int64 max_partitions = 2;</code>
   *
   * @return The maxPartitions.
   */
  long getMaxPartitions();
}
