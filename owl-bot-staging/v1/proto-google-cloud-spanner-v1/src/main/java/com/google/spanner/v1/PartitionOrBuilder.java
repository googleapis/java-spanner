// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/v1/spanner.proto

package com.google.spanner.v1;

public interface PartitionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.Partition)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * This token can be passed to Read, StreamingRead, ExecuteSql, or
   * ExecuteStreamingSql requests to restrict the results to those identified by
   * this partition token.
   * </pre>
   *
   * <code>bytes partition_token = 1;</code>
   * @return The partitionToken.
   */
  com.google.protobuf.ByteString getPartitionToken();
}
