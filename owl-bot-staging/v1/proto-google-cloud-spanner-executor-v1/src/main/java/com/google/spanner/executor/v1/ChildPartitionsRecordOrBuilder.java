// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface ChildPartitionsRecordOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.ChildPartitionsRecord)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Data change records returned from child partitions in this child partitions
   * record will have a commit timestamp greater than or equal to start_time.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp start_time = 1;</code>
   * @return Whether the startTime field is set.
   */
  boolean hasStartTime();
  /**
   * <pre>
   * Data change records returned from child partitions in this child partitions
   * record will have a commit timestamp greater than or equal to start_time.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp start_time = 1;</code>
   * @return The startTime.
   */
  com.google.protobuf.Timestamp getStartTime();
  /**
   * <pre>
   * Data change records returned from child partitions in this child partitions
   * record will have a commit timestamp greater than or equal to start_time.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp start_time = 1;</code>
   */
  com.google.protobuf.TimestampOrBuilder getStartTimeOrBuilder();

  /**
   * <pre>
   * A monotonically increasing sequence number that can be used to define the
   * ordering of the child partitions record when there are multiple child
   * partitions records returned with the same start_time in a particular
   * partition.
   * </pre>
   *
   * <code>string record_sequence = 2;</code>
   * @return The recordSequence.
   */
  java.lang.String getRecordSequence();
  /**
   * <pre>
   * A monotonically increasing sequence number that can be used to define the
   * ordering of the child partitions record when there are multiple child
   * partitions records returned with the same start_time in a particular
   * partition.
   * </pre>
   *
   * <code>string record_sequence = 2;</code>
   * @return The bytes for recordSequence.
   */
  com.google.protobuf.ByteString
      getRecordSequenceBytes();

  /**
   * <pre>
   * A set of child partitions and their associated information.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ChildPartitionsRecord.ChildPartition child_partitions = 3;</code>
   */
  java.util.List<com.google.spanner.executor.v1.ChildPartitionsRecord.ChildPartition> 
      getChildPartitionsList();
  /**
   * <pre>
   * A set of child partitions and their associated information.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ChildPartitionsRecord.ChildPartition child_partitions = 3;</code>
   */
  com.google.spanner.executor.v1.ChildPartitionsRecord.ChildPartition getChildPartitions(int index);
  /**
   * <pre>
   * A set of child partitions and their associated information.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ChildPartitionsRecord.ChildPartition child_partitions = 3;</code>
   */
  int getChildPartitionsCount();
  /**
   * <pre>
   * A set of child partitions and their associated information.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ChildPartitionsRecord.ChildPartition child_partitions = 3;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.ChildPartitionsRecord.ChildPartitionOrBuilder> 
      getChildPartitionsOrBuilderList();
  /**
   * <pre>
   * A set of child partitions and their associated information.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.ChildPartitionsRecord.ChildPartition child_partitions = 3;</code>
   */
  com.google.spanner.executor.v1.ChildPartitionsRecord.ChildPartitionOrBuilder getChildPartitionsOrBuilder(
      int index);
}
