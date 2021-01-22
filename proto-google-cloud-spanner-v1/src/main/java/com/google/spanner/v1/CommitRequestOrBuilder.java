/*
 * Copyright 2020 Google LLC
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
// source: google/spanner/v1/spanner.proto

package com.google.spanner.v1;

public interface CommitRequestOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.CommitRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The session in which the transaction to be committed is running.
   * </pre>
   *
   * <code>
   * string session = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The session.
   */
  java.lang.String getSession();
  /**
   *
   *
   * <pre>
   * Required. The session in which the transaction to be committed is running.
   * </pre>
   *
   * <code>
   * string session = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The bytes for session.
   */
  com.google.protobuf.ByteString getSessionBytes();

  /**
   *
   *
   * <pre>
   * Commit a previously-started transaction.
   * </pre>
   *
   * <code>bytes transaction_id = 2;</code>
   *
   * @return The transactionId.
   */
  com.google.protobuf.ByteString getTransactionId();

  /**
   *
   *
   * <pre>
   * Execute mutations in a temporary transaction. Note that unlike
   * commit of a previously-started transaction, commit with a
   * temporary transaction is non-idempotent. That is, if the
   * `CommitRequest` is sent to Cloud Spanner more than once (for
   * instance, due to retries in the application, or in the
   * transport library), it is possible that the mutations are
   * executed more than once. If this is undesirable, use
   * [BeginTransaction][google.spanner.v1.Spanner.BeginTransaction] and
   * [Commit][google.spanner.v1.Spanner.Commit] instead.
   * </pre>
   *
   * <code>.google.spanner.v1.TransactionOptions single_use_transaction = 3;</code>
   *
   * @return Whether the singleUseTransaction field is set.
   */
  boolean hasSingleUseTransaction();
  /**
   *
   *
   * <pre>
   * Execute mutations in a temporary transaction. Note that unlike
   * commit of a previously-started transaction, commit with a
   * temporary transaction is non-idempotent. That is, if the
   * `CommitRequest` is sent to Cloud Spanner more than once (for
   * instance, due to retries in the application, or in the
   * transport library), it is possible that the mutations are
   * executed more than once. If this is undesirable, use
   * [BeginTransaction][google.spanner.v1.Spanner.BeginTransaction] and
   * [Commit][google.spanner.v1.Spanner.Commit] instead.
   * </pre>
   *
   * <code>.google.spanner.v1.TransactionOptions single_use_transaction = 3;</code>
   *
   * @return The singleUseTransaction.
   */
  com.google.spanner.v1.TransactionOptions getSingleUseTransaction();
  /**
   *
   *
   * <pre>
   * Execute mutations in a temporary transaction. Note that unlike
   * commit of a previously-started transaction, commit with a
   * temporary transaction is non-idempotent. That is, if the
   * `CommitRequest` is sent to Cloud Spanner more than once (for
   * instance, due to retries in the application, or in the
   * transport library), it is possible that the mutations are
   * executed more than once. If this is undesirable, use
   * [BeginTransaction][google.spanner.v1.Spanner.BeginTransaction] and
   * [Commit][google.spanner.v1.Spanner.Commit] instead.
   * </pre>
   *
   * <code>.google.spanner.v1.TransactionOptions single_use_transaction = 3;</code>
   */
  com.google.spanner.v1.TransactionOptionsOrBuilder getSingleUseTransactionOrBuilder();

  /**
   *
   *
   * <pre>
   * The mutations to be executed when this transaction commits. All
   * mutations are applied atomically, in the order they appear in
   * this list.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Mutation mutations = 4;</code>
   */
  java.util.List<com.google.spanner.v1.Mutation> getMutationsList();
  /**
   *
   *
   * <pre>
   * The mutations to be executed when this transaction commits. All
   * mutations are applied atomically, in the order they appear in
   * this list.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Mutation mutations = 4;</code>
   */
  com.google.spanner.v1.Mutation getMutations(int index);
  /**
   *
   *
   * <pre>
   * The mutations to be executed when this transaction commits. All
   * mutations are applied atomically, in the order they appear in
   * this list.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Mutation mutations = 4;</code>
   */
  int getMutationsCount();
  /**
   *
   *
   * <pre>
   * The mutations to be executed when this transaction commits. All
   * mutations are applied atomically, in the order they appear in
   * this list.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Mutation mutations = 4;</code>
   */
  java.util.List<? extends com.google.spanner.v1.MutationOrBuilder> getMutationsOrBuilderList();
  /**
   *
   *
   * <pre>
   * The mutations to be executed when this transaction commits. All
   * mutations are applied atomically, in the order they appear in
   * this list.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Mutation mutations = 4;</code>
   */
  com.google.spanner.v1.MutationOrBuilder getMutationsOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * If `true`, then statistics related to the transaction will be included in
   * the [CommitResponse][google.spanner.v1.CommitResponse.commit_stats]. Default value is
   * `false`.
   * </pre>
   *
   * <code>bool return_commit_stats = 5;</code>
   *
   * @return The returnCommitStats.
   */
  boolean getReturnCommitStats();

  public com.google.spanner.v1.CommitRequest.TransactionCase getTransactionCase();
}
