/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface ReadActionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.ReadAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The table to read at.
   * </pre>
   *
   * <code>string table = 1;</code>
   * @return The table.
   */
  java.lang.String getTable();
  /**
   * <pre>
   * The table to read at.
   * </pre>
   *
   * <code>string table = 1;</code>
   * @return The bytes for table.
   */
  com.google.protobuf.ByteString
      getTableBytes();

  /**
   * <pre>
   * The index to read at if it's an index read.
   * </pre>
   *
   * <code>optional string index = 2;</code>
   * @return Whether the index field is set.
   */
  boolean hasIndex();
  /**
   * <pre>
   * The index to read at if it's an index read.
   * </pre>
   *
   * <code>optional string index = 2;</code>
   * @return The index.
   */
  java.lang.String getIndex();
  /**
   * <pre>
   * The index to read at if it's an index read.
   * </pre>
   *
   * <code>optional string index = 2;</code>
   * @return The bytes for index.
   */
  com.google.protobuf.ByteString
      getIndexBytes();

  /**
   * <pre>
   * List of columns must begin with the key columns used for the read.
   * </pre>
   *
   * <code>repeated string column = 3;</code>
   * @return A list containing the column.
   */
  java.util.List<java.lang.String>
      getColumnList();
  /**
   * <pre>
   * List of columns must begin with the key columns used for the read.
   * </pre>
   *
   * <code>repeated string column = 3;</code>
   * @return The count of column.
   */
  int getColumnCount();
  /**
   * <pre>
   * List of columns must begin with the key columns used for the read.
   * </pre>
   *
   * <code>repeated string column = 3;</code>
   * @param index The index of the element to return.
   * @return The column at the given index.
   */
  java.lang.String getColumn(int index);
  /**
   * <pre>
   * List of columns must begin with the key columns used for the read.
   * </pre>
   *
   * <code>repeated string column = 3;</code>
   * @param index The index of the value to return.
   * @return The bytes of the column at the given index.
   */
  com.google.protobuf.ByteString
      getColumnBytes(int index);

  /**
   * <pre>
   * Keys for performing this read.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.KeySet keys = 4;</code>
   * @return Whether the keys field is set.
   */
  boolean hasKeys();
  /**
   * <pre>
   * Keys for performing this read.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.KeySet keys = 4;</code>
   * @return The keys.
   */
  com.google.spanner.executor.v1.KeySet getKeys();
  /**
   * <pre>
   * Keys for performing this read.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.KeySet keys = 4;</code>
   */
  com.google.spanner.executor.v1.KeySetOrBuilder getKeysOrBuilder();

  /**
   * <pre>
   * Limit on number of rows to read. If set, must be positive.
   * </pre>
   *
   * <code>int32 limit = 5;</code>
   * @return The limit.
   */
  int getLimit();
}
