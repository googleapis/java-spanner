// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface ReadResultOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.ReadResult)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Table name.
   * </pre>
   *
   * <code>string table = 1;</code>
   * @return The table.
   */
  java.lang.String getTable();
  /**
   * <pre>
   * Table name.
   * </pre>
   *
   * <code>string table = 1;</code>
   * @return The bytes for table.
   */
  com.google.protobuf.ByteString
      getTableBytes();

  /**
   * <pre>
   * Index name, if read from an index.
   * </pre>
   *
   * <code>optional string index = 2;</code>
   * @return Whether the index field is set.
   */
  boolean hasIndex();
  /**
   * <pre>
   * Index name, if read from an index.
   * </pre>
   *
   * <code>optional string index = 2;</code>
   * @return The index.
   */
  java.lang.String getIndex();
  /**
   * <pre>
   * Index name, if read from an index.
   * </pre>
   *
   * <code>optional string index = 2;</code>
   * @return The bytes for index.
   */
  com.google.protobuf.ByteString
      getIndexBytes();

  /**
   * <pre>
   * Request index (multiread only).
   * </pre>
   *
   * <code>optional int32 request_index = 3;</code>
   * @return Whether the requestIndex field is set.
   */
  boolean hasRequestIndex();
  /**
   * <pre>
   * Request index (multiread only).
   * </pre>
   *
   * <code>optional int32 request_index = 3;</code>
   * @return The requestIndex.
   */
  int getRequestIndex();

  /**
   * <pre>
   * Rows read. Each row is a struct with multiple fields, one for each column
   * in read result. All rows have the same type.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList row = 4;</code>
   */
  java.util.List<com.google.spanner.executor.v1.ValueList> 
      getRowList();
  /**
   * <pre>
   * Rows read. Each row is a struct with multiple fields, one for each column
   * in read result. All rows have the same type.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList row = 4;</code>
   */
  com.google.spanner.executor.v1.ValueList getRow(int index);
  /**
   * <pre>
   * Rows read. Each row is a struct with multiple fields, one for each column
   * in read result. All rows have the same type.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList row = 4;</code>
   */
  int getRowCount();
  /**
   * <pre>
   * Rows read. Each row is a struct with multiple fields, one for each column
   * in read result. All rows have the same type.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList row = 4;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.ValueListOrBuilder> 
      getRowOrBuilderList();
  /**
   * <pre>
   * Rows read. Each row is a struct with multiple fields, one for each column
   * in read result. All rows have the same type.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ValueList row = 4;</code>
   */
  com.google.spanner.executor.v1.ValueListOrBuilder getRowOrBuilder(
      int index);

  /**
   * <pre>
   * The type of rows read. It must be set if at least one row was read.
   * </pre>
   *
   * <code>optional .google.spanner.v1.StructType row_type = 5;</code>
   * @return Whether the rowType field is set.
   */
  boolean hasRowType();
  /**
   * <pre>
   * The type of rows read. It must be set if at least one row was read.
   * </pre>
   *
   * <code>optional .google.spanner.v1.StructType row_type = 5;</code>
   * @return The rowType.
   */
  com.google.spanner.v1.StructType getRowType();
  /**
   * <pre>
   * The type of rows read. It must be set if at least one row was read.
   * </pre>
   *
   * <code>optional .google.spanner.v1.StructType row_type = 5;</code>
   */
  com.google.spanner.v1.StructTypeOrBuilder getRowTypeOrBuilder();
}
