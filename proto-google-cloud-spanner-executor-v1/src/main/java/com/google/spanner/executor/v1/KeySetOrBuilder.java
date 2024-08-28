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

public interface KeySetOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.KeySet)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * A list of specific keys. Entries in "keys" should have exactly as
   * many elements as there are columns in the primary or index key
   * with which this "KeySet" is used.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList point = 1;</code>
   */
  java.util.List<com.google.spanner.executor.v1.ValueList> getPointList();
  /**
   *
   *
   * <pre>
   * A list of specific keys. Entries in "keys" should have exactly as
   * many elements as there are columns in the primary or index key
   * with which this "KeySet" is used.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList point = 1;</code>
   */
  com.google.spanner.executor.v1.ValueList getPoint(int index);
  /**
   *
   *
   * <pre>
   * A list of specific keys. Entries in "keys" should have exactly as
   * many elements as there are columns in the primary or index key
   * with which this "KeySet" is used.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList point = 1;</code>
   */
  int getPointCount();
  /**
   *
   *
   * <pre>
   * A list of specific keys. Entries in "keys" should have exactly as
   * many elements as there are columns in the primary or index key
   * with which this "KeySet" is used.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList point = 1;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.ValueListOrBuilder>
      getPointOrBuilderList();
  /**
   *
   *
   * <pre>
   * A list of specific keys. Entries in "keys" should have exactly as
   * many elements as there are columns in the primary or index key
   * with which this "KeySet" is used.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList point = 1;</code>
   */
  com.google.spanner.executor.v1.ValueListOrBuilder getPointOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * A list of key ranges.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.KeyRange range = 2;</code>
   */
  java.util.List<com.google.spanner.executor.v1.KeyRange> getRangeList();
  /**
   *
   *
   * <pre>
   * A list of key ranges.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.KeyRange range = 2;</code>
   */
  com.google.spanner.executor.v1.KeyRange getRange(int index);
  /**
   *
   *
   * <pre>
   * A list of key ranges.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.KeyRange range = 2;</code>
   */
  int getRangeCount();
  /**
   *
   *
   * <pre>
   * A list of key ranges.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.KeyRange range = 2;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.KeyRangeOrBuilder>
      getRangeOrBuilderList();
  /**
   *
   *
   * <pre>
   * A list of key ranges.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.KeyRange range = 2;</code>
   */
  com.google.spanner.executor.v1.KeyRangeOrBuilder getRangeOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * For convenience "all" can be set to "true" to indicate that this
   * "KeySet" matches all keys in the table or index. Note that any keys
   * specified in "keys" or "ranges" are only yielded once.
   * </pre>
   *
   * <code>bool all = 3;</code>
   *
   * @return The all.
   */
  boolean getAll();
}
