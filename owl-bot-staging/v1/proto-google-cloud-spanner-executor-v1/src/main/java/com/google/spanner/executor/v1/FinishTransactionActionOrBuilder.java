// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface FinishTransactionActionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.FinishTransactionAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Defines how exactly the transaction should be completed, e.g. with
   * commit or abortion.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
   * @return The enum numeric value on the wire for mode.
   */
  int getModeValue();
  /**
   * <pre>
   * Defines how exactly the transaction should be completed, e.g. with
   * commit or abortion.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
   * @return The mode.
   */
  com.google.spanner.executor.v1.FinishTransactionAction.Mode getMode();
}
