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
// source: google/spanner/executor/v1/executor.proto

package com.google.spanner.executor.v1;

public interface TableMetadataOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.TableMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Table name.
   * </pre>
   *
   * <code>string name = 1;</code>
   * @return The name.
   */
  java.lang.String getName();
  /**
   * <pre>
   * Table name.
   * </pre>
   *
   * <code>string name = 1;</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();

  /**
   * <pre>
   * Columns, in the same order as in the schema.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata column = 2;</code>
   */
  java.util.List<com.google.spanner.executor.v1.ColumnMetadata> 
      getColumnList();
  /**
   * <pre>
   * Columns, in the same order as in the schema.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata column = 2;</code>
   */
  com.google.spanner.executor.v1.ColumnMetadata getColumn(int index);
  /**
   * <pre>
   * Columns, in the same order as in the schema.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata column = 2;</code>
   */
  int getColumnCount();
  /**
   * <pre>
   * Columns, in the same order as in the schema.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata column = 2;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.ColumnMetadataOrBuilder> 
      getColumnOrBuilderList();
  /**
   * <pre>
   * Columns, in the same order as in the schema.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata column = 2;</code>
   */
  com.google.spanner.executor.v1.ColumnMetadataOrBuilder getColumnOrBuilder(
      int index);

  /**
   * <pre>
   * Keys, in order. Column name is currently not populated.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata key_column = 3;</code>
   */
  java.util.List<com.google.spanner.executor.v1.ColumnMetadata> 
      getKeyColumnList();
  /**
   * <pre>
   * Keys, in order. Column name is currently not populated.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata key_column = 3;</code>
   */
  com.google.spanner.executor.v1.ColumnMetadata getKeyColumn(int index);
  /**
   * <pre>
   * Keys, in order. Column name is currently not populated.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata key_column = 3;</code>
   */
  int getKeyColumnCount();
  /**
   * <pre>
   * Keys, in order. Column name is currently not populated.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata key_column = 3;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.ColumnMetadataOrBuilder> 
      getKeyColumnOrBuilderList();
  /**
   * <pre>
   * Keys, in order. Column name is currently not populated.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ColumnMetadata key_column = 3;</code>
   */
  com.google.spanner.executor.v1.ColumnMetadataOrBuilder getKeyColumnOrBuilder(
      int index);
}
