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

package com.google.spanner.executor.v1;

public interface StartBatchTransactionActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.StartBatchTransactionAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The exact timestamp to start the batch transaction.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp batch_txn_time = 1;</code>
   *
   * @return Whether the batchTxnTime field is set.
   */
  boolean hasBatchTxnTime();
  /**
   *
   *
   * <pre>
   * The exact timestamp to start the batch transaction.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp batch_txn_time = 1;</code>
   *
   * @return The batchTxnTime.
   */
  com.google.protobuf.Timestamp getBatchTxnTime();
  /**
   *
   *
   * <pre>
   * The exact timestamp to start the batch transaction.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp batch_txn_time = 1;</code>
   */
  com.google.protobuf.TimestampOrBuilder getBatchTxnTimeOrBuilder();

  /**
   *
   *
   * <pre>
   * ID of a batch read-only transaction. It can be used to start the same
   * batch transaction on multiple executors and parallelize partition
   * processing.
   * </pre>
   *
   * <code>bytes tid = 2;</code>
   *
   * @return Whether the tid field is set.
   */
  boolean hasTid();
  /**
   *
   *
   * <pre>
   * ID of a batch read-only transaction. It can be used to start the same
   * batch transaction on multiple executors and parallelize partition
   * processing.
   * </pre>
   *
   * <code>bytes tid = 2;</code>
   *
   * @return The tid.
   */
  com.google.protobuf.ByteString getTid();

  /**
   *
   *
   * <pre>
   * Database role to assume while performing this action. Setting the
   * database_role will enforce additional role-based access checks on this
   * action.
   * </pre>
   *
   * <code>string cloud_database_role = 3;</code>
   *
   * @return The cloudDatabaseRole.
   */
  java.lang.String getCloudDatabaseRole();
  /**
   *
   *
   * <pre>
   * Database role to assume while performing this action. Setting the
   * database_role will enforce additional role-based access checks on this
   * action.
   * </pre>
   *
   * <code>string cloud_database_role = 3;</code>
   *
   * @return The bytes for cloudDatabaseRole.
   */
  com.google.protobuf.ByteString getCloudDatabaseRoleBytes();

  com.google.spanner.executor.v1.StartBatchTransactionAction.ParamCase getParamCase();
}
