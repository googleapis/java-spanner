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

public interface BeginTransactionRequestOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.BeginTransactionRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The session in which the transaction runs.
   * </pre>
   *
   * <code>
   * string session = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The session.
   */
  String getSession();
  /**
   *
   *
   * <pre>
   * Required. The session in which the transaction runs.
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
   * Required. Options for the new transaction.
   * </pre>
   *
   * <code>
   * .google.spanner.v1.TransactionOptions options = 2 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return Whether the options field is set.
   */
  boolean hasOptions();
  /**
   *
   *
   * <pre>
   * Required. Options for the new transaction.
   * </pre>
   *
   * <code>
   * .google.spanner.v1.TransactionOptions options = 2 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return The options.
   */
  TransactionOptions getOptions();
  /**
   *
   *
   * <pre>
   * Required. Options for the new transaction.
   * </pre>
   *
   * <code>
   * .google.spanner.v1.TransactionOptions options = 2 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  com.google.spanner.v1.TransactionOptionsOrBuilder getOptionsOrBuilder();

  /**
   *
   *
   * <pre>
   * Common options for this request.
   * Priority is ignored for this request. Setting the priority in this
   * request_options struct will not do anything. To set the priority for a
   * transaction, set it on the reads and writes that are part of this
   * transaction instead.
   * </pre>
   *
   * <code>.google.spanner.v1.RequestOptions request_options = 3;</code>
   *
   * @return Whether the requestOptions field is set.
   */
  boolean hasRequestOptions();
  /**
   *
   *
   * <pre>
   * Common options for this request.
   * Priority is ignored for this request. Setting the priority in this
   * request_options struct will not do anything. To set the priority for a
   * transaction, set it on the reads and writes that are part of this
   * transaction instead.
   * </pre>
   *
   * <code>.google.spanner.v1.RequestOptions request_options = 3;</code>
   *
   * @return The requestOptions.
   */
  com.google.spanner.v1.RequestOptions getRequestOptions();
  /**
   *
   *
   * <pre>
   * Common options for this request.
   * Priority is ignored for this request. Setting the priority in this
   * request_options struct will not do anything. To set the priority for a
   * transaction, set it on the reads and writes that are part of this
   * transaction instead.
   * </pre>
   *
   * <code>.google.spanner.v1.RequestOptions request_options = 3;</code>
   */
  com.google.spanner.v1.RequestOptionsOrBuilder getRequestOptionsOrBuilder();
}
