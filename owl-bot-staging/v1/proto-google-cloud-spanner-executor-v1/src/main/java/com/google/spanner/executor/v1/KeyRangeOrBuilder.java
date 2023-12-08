// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface KeyRangeOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.KeyRange)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * "start" and "limit" must have the same number of key parts,
   * though they may name only a prefix of the table or index key.
   * The start key of this KeyRange.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ValueList start = 1;</code>
   * @return Whether the start field is set.
   */
  boolean hasStart();
  /**
   * <pre>
   * "start" and "limit" must have the same number of key parts,
   * though they may name only a prefix of the table or index key.
   * The start key of this KeyRange.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ValueList start = 1;</code>
   * @return The start.
   */
  com.google.spanner.executor.v1.ValueList getStart();
  /**
   * <pre>
   * "start" and "limit" must have the same number of key parts,
   * though they may name only a prefix of the table or index key.
   * The start key of this KeyRange.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ValueList start = 1;</code>
   */
  com.google.spanner.executor.v1.ValueListOrBuilder getStartOrBuilder();

  /**
   * <pre>
   * The end key of this KeyRange.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ValueList limit = 2;</code>
   * @return Whether the limit field is set.
   */
  boolean hasLimit();
  /**
   * <pre>
   * The end key of this KeyRange.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ValueList limit = 2;</code>
   * @return The limit.
   */
  com.google.spanner.executor.v1.ValueList getLimit();
  /**
   * <pre>
   * The end key of this KeyRange.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.ValueList limit = 2;</code>
   */
  com.google.spanner.executor.v1.ValueListOrBuilder getLimitOrBuilder();

  /**
   * <pre>
   * "start" and "limit" type for this KeyRange.
   * </pre>
   *
   * <code>optional .google.spanner.executor.v1.KeyRange.Type type = 3;</code>
   * @return Whether the type field is set.
   */
  boolean hasType();
  /**
   * <pre>
   * "start" and "limit" type for this KeyRange.
   * </pre>
   *
   * <code>optional .google.spanner.executor.v1.KeyRange.Type type = 3;</code>
   * @return The enum numeric value on the wire for type.
   */
  int getTypeValue();
  /**
   * <pre>
   * "start" and "limit" type for this KeyRange.
   * </pre>
   *
   * <code>optional .google.spanner.executor.v1.KeyRange.Type type = 3;</code>
   * @return The type.
   */
  com.google.spanner.executor.v1.KeyRange.Type getType();
}
