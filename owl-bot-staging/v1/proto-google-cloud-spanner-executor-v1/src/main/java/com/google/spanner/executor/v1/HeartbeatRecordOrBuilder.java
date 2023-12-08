// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface HeartbeatRecordOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.HeartbeatRecord)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Timestamp for this heartbeat check.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp heartbeat_time = 1;</code>
   * @return Whether the heartbeatTime field is set.
   */
  boolean hasHeartbeatTime();
  /**
   * <pre>
   * Timestamp for this heartbeat check.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp heartbeat_time = 1;</code>
   * @return The heartbeatTime.
   */
  com.google.protobuf.Timestamp getHeartbeatTime();
  /**
   * <pre>
   * Timestamp for this heartbeat check.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp heartbeat_time = 1;</code>
   */
  com.google.protobuf.TimestampOrBuilder getHeartbeatTimeOrBuilder();
}
