/*
 * Copyright 2025 Google LLC
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
// source: google/spanner/v1/transaction.proto

// Protobuf Java Version: 3.25.8
package com.google.spanner.v1;

public interface TransactionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.Transaction)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * `id` may be used to identify the transaction in subsequent
   * [Read][google.spanner.v1.Spanner.Read],
   * [ExecuteSql][google.spanner.v1.Spanner.ExecuteSql],
   * [Commit][google.spanner.v1.Spanner.Commit], or
   * [Rollback][google.spanner.v1.Spanner.Rollback] calls.
   *
   * Single-use read-only transactions do not have IDs, because
   * single-use transactions do not support multiple requests.
   * </pre>
   *
   * <code>bytes id = 1;</code>
   *
   * @return The id.
   */
  com.google.protobuf.ByteString getId();

  /**
   *
   *
   * <pre>
   * For snapshot read-only transactions, the read timestamp chosen
   * for the transaction. Not returned by default: see
   * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
   *
   * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
   * Example: `"2014-10-02T15:01:23.045123456Z"`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
   *
   * @return Whether the readTimestamp field is set.
   */
  boolean hasReadTimestamp();

  /**
   *
   *
   * <pre>
   * For snapshot read-only transactions, the read timestamp chosen
   * for the transaction. Not returned by default: see
   * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
   *
   * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
   * Example: `"2014-10-02T15:01:23.045123456Z"`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
   *
   * @return The readTimestamp.
   */
  com.google.protobuf.Timestamp getReadTimestamp();

  /**
   *
   *
   * <pre>
   * For snapshot read-only transactions, the read timestamp chosen
   * for the transaction. Not returned by default: see
   * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
   *
   * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
   * Example: `"2014-10-02T15:01:23.045123456Z"`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
   */
  com.google.protobuf.TimestampOrBuilder getReadTimestampOrBuilder();

  /**
   *
   *
   * <pre>
   * A precommit token is included in the response of a BeginTransaction
   * request if the read-write transaction is on a multiplexed session and
   * a mutation_key was specified in the
   * [BeginTransaction][google.spanner.v1.BeginTransactionRequest].
   * The precommit token with the highest sequence number from this transaction
   * attempt should be passed to the [Commit][google.spanner.v1.Spanner.Commit]
   * request for this transaction.
   * </pre>
   *
   * <code>.google.spanner.v1.MultiplexedSessionPrecommitToken precommit_token = 3;</code>
   *
   * @return Whether the precommitToken field is set.
   */
  boolean hasPrecommitToken();

  /**
   *
   *
   * <pre>
   * A precommit token is included in the response of a BeginTransaction
   * request if the read-write transaction is on a multiplexed session and
   * a mutation_key was specified in the
   * [BeginTransaction][google.spanner.v1.BeginTransactionRequest].
   * The precommit token with the highest sequence number from this transaction
   * attempt should be passed to the [Commit][google.spanner.v1.Spanner.Commit]
   * request for this transaction.
   * </pre>
   *
   * <code>.google.spanner.v1.MultiplexedSessionPrecommitToken precommit_token = 3;</code>
   *
   * @return The precommitToken.
   */
  com.google.spanner.v1.MultiplexedSessionPrecommitToken getPrecommitToken();

  /**
   *
   *
   * <pre>
   * A precommit token is included in the response of a BeginTransaction
   * request if the read-write transaction is on a multiplexed session and
   * a mutation_key was specified in the
   * [BeginTransaction][google.spanner.v1.BeginTransactionRequest].
   * The precommit token with the highest sequence number from this transaction
   * attempt should be passed to the [Commit][google.spanner.v1.Spanner.Commit]
   * request for this transaction.
   * </pre>
   *
   * <code>.google.spanner.v1.MultiplexedSessionPrecommitToken precommit_token = 3;</code>
   */
  com.google.spanner.v1.MultiplexedSessionPrecommitTokenOrBuilder getPrecommitTokenOrBuilder();
}
