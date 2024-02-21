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

// Protobuf Java Version: 3.25.2
package com.google.spanner.executor.v1;

public interface ConcurrencyOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.Concurrency)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Indicates a read at a consistent timestamp that is specified relative to
   * now. That is, if the caller has specified an exact staleness of s
   * seconds, we will read at now - s.
   * </pre>
   *
   * <code>double staleness_seconds = 1;</code>
   *
   * @return Whether the stalenessSeconds field is set.
   */
  boolean hasStalenessSeconds();
  /**
   *
   *
   * <pre>
   * Indicates a read at a consistent timestamp that is specified relative to
   * now. That is, if the caller has specified an exact staleness of s
   * seconds, we will read at now - s.
   * </pre>
   *
   * <code>double staleness_seconds = 1;</code>
   *
   * @return The stalenessSeconds.
   */
  double getStalenessSeconds();

  /**
   *
   *
   * <pre>
   * Indicates a boundedly stale read that reads at a timestamp &gt;= T.
   * </pre>
   *
   * <code>int64 min_read_timestamp_micros = 2;</code>
   *
   * @return Whether the minReadTimestampMicros field is set.
   */
  boolean hasMinReadTimestampMicros();
  /**
   *
   *
   * <pre>
   * Indicates a boundedly stale read that reads at a timestamp &gt;= T.
   * </pre>
   *
   * <code>int64 min_read_timestamp_micros = 2;</code>
   *
   * @return The minReadTimestampMicros.
   */
  long getMinReadTimestampMicros();

  /**
   *
   *
   * <pre>
   * Indicates a boundedly stale read that is at most N seconds stale.
   * </pre>
   *
   * <code>double max_staleness_seconds = 3;</code>
   *
   * @return Whether the maxStalenessSeconds field is set.
   */
  boolean hasMaxStalenessSeconds();
  /**
   *
   *
   * <pre>
   * Indicates a boundedly stale read that is at most N seconds stale.
   * </pre>
   *
   * <code>double max_staleness_seconds = 3;</code>
   *
   * @return The maxStalenessSeconds.
   */
  double getMaxStalenessSeconds();

  /**
   *
   *
   * <pre>
   * Indicates a read at a consistent timestamp.
   * </pre>
   *
   * <code>int64 exact_timestamp_micros = 4;</code>
   *
   * @return Whether the exactTimestampMicros field is set.
   */
  boolean hasExactTimestampMicros();
  /**
   *
   *
   * <pre>
   * Indicates a read at a consistent timestamp.
   * </pre>
   *
   * <code>int64 exact_timestamp_micros = 4;</code>
   *
   * @return The exactTimestampMicros.
   */
  long getExactTimestampMicros();

  /**
   *
   *
   * <pre>
   * Indicates a strong read, must only be set to true, or unset.
   * </pre>
   *
   * <code>bool strong = 5;</code>
   *
   * @return Whether the strong field is set.
   */
  boolean hasStrong();
  /**
   *
   *
   * <pre>
   * Indicates a strong read, must only be set to true, or unset.
   * </pre>
   *
   * <code>bool strong = 5;</code>
   *
   * @return The strong.
   */
  boolean getStrong();

  /**
   *
   *
   * <pre>
   * Indicates a batch read, must only be set to true, or unset.
   * </pre>
   *
   * <code>bool batch = 6;</code>
   *
   * @return Whether the batch field is set.
   */
  boolean hasBatch();
  /**
   *
   *
   * <pre>
   * Indicates a batch read, must only be set to true, or unset.
   * </pre>
   *
   * <code>bool batch = 6;</code>
   *
   * @return The batch.
   */
  boolean getBatch();

  /**
   *
   *
   * <pre>
   * True if exact_timestamp_micros is set, and the chosen timestamp is that of
   * a snapshot epoch.
   * </pre>
   *
   * <code>bool snapshot_epoch_read = 7;</code>
   *
   * @return The snapshotEpochRead.
   */
  boolean getSnapshotEpochRead();

  /**
   *
   *
   * <pre>
   * If set, this is a snapshot epoch read constrained to read only the
   * specified log scope root table, and its children. Will not be set for full
   * database epochs.
   * </pre>
   *
   * <code>string snapshot_epoch_root_table = 8;</code>
   *
   * @return The snapshotEpochRootTable.
   */
  java.lang.String getSnapshotEpochRootTable();
  /**
   *
   *
   * <pre>
   * If set, this is a snapshot epoch read constrained to read only the
   * specified log scope root table, and its children. Will not be set for full
   * database epochs.
   * </pre>
   *
   * <code>string snapshot_epoch_root_table = 8;</code>
   *
   * @return The bytes for snapshotEpochRootTable.
   */
  com.google.protobuf.ByteString getSnapshotEpochRootTableBytes();

  /**
   *
   *
   * <pre>
   * Set only when batch is true.
   * </pre>
   *
   * <code>int64 batch_read_timestamp_micros = 9;</code>
   *
   * @return The batchReadTimestampMicros.
   */
  long getBatchReadTimestampMicros();

  com.google.spanner.executor.v1.Concurrency.ConcurrencyModeCase getConcurrencyModeCase();
}
