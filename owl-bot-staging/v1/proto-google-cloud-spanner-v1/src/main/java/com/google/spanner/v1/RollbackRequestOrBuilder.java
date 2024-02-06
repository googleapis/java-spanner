// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/v1/spanner.proto

// Protobuf Java Version: 3.25.2
package com.google.spanner.v1;

public interface RollbackRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.RollbackRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Required. The session in which the transaction to roll back is running.
   * </pre>
   *
   * <code>string session = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }</code>
   * @return The session.
   */
  java.lang.String getSession();
  /**
   * <pre>
   * Required. The session in which the transaction to roll back is running.
   * </pre>
   *
   * <code>string session = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }</code>
   * @return The bytes for session.
   */
  com.google.protobuf.ByteString
      getSessionBytes();

  /**
   * <pre>
   * Required. The transaction to roll back.
   * </pre>
   *
   * <code>bytes transaction_id = 2 [(.google.api.field_behavior) = REQUIRED];</code>
   * @return The transactionId.
   */
  com.google.protobuf.ByteString getTransactionId();
}
