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

public interface ExecuteBatchDmlRequestOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.ExecuteBatchDmlRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The session in which the DML statements should be performed.
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
   * Required. The session in which the DML statements should be performed.
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
   * Required. The transaction to use. Must be a read-write transaction.
   * To protect against replays, single-use transactions are not supported. The
   * caller must either supply an existing transaction ID or begin a new
   * transaction.
   * </pre>
   *
   * <code>
   * .google.spanner.v1.TransactionSelector transaction = 2 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return Whether the transaction field is set.
   */
  boolean hasTransaction();
  /**
   *
   *
   * <pre>
   * Required. The transaction to use. Must be a read-write transaction.
   * To protect against replays, single-use transactions are not supported. The
   * caller must either supply an existing transaction ID or begin a new
   * transaction.
   * </pre>
   *
   * <code>
   * .google.spanner.v1.TransactionSelector transaction = 2 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return The transaction.
   */
  TransactionSelector getTransaction();
  /**
   *
   *
   * <pre>
   * Required. The transaction to use. Must be a read-write transaction.
   * To protect against replays, single-use transactions are not supported. The
   * caller must either supply an existing transaction ID or begin a new
   * transaction.
   * </pre>
   *
   * <code>
   * .google.spanner.v1.TransactionSelector transaction = 2 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  TransactionSelectorOrBuilder getTransactionOrBuilder();

  /**
   *
   *
   * <pre>
   * Required. The list of statements to execute in this batch. Statements are executed
   * serially, such that the effects of statement `i` are visible to statement
   * `i+1`. Each statement must be a DML statement. Execution stops at the
   * first failed statement; the remaining statements are not executed.
   * Callers must provide at least one statement.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.v1.ExecuteBatchDmlRequest.Statement statements = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  java.util.List<ExecuteBatchDmlRequest.Statement> getStatementsList();
  /**
   *
   *
   * <pre>
   * Required. The list of statements to execute in this batch. Statements are executed
   * serially, such that the effects of statement `i` are visible to statement
   * `i+1`. Each statement must be a DML statement. Execution stops at the
   * first failed statement; the remaining statements are not executed.
   * Callers must provide at least one statement.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.v1.ExecuteBatchDmlRequest.Statement statements = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  ExecuteBatchDmlRequest.Statement getStatements(int index);
  /**
   *
   *
   * <pre>
   * Required. The list of statements to execute in this batch. Statements are executed
   * serially, such that the effects of statement `i` are visible to statement
   * `i+1`. Each statement must be a DML statement. Execution stops at the
   * first failed statement; the remaining statements are not executed.
   * Callers must provide at least one statement.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.v1.ExecuteBatchDmlRequest.Statement statements = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  int getStatementsCount();
  /**
   *
   *
   * <pre>
   * Required. The list of statements to execute in this batch. Statements are executed
   * serially, such that the effects of statement `i` are visible to statement
   * `i+1`. Each statement must be a DML statement. Execution stops at the
   * first failed statement; the remaining statements are not executed.
   * Callers must provide at least one statement.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.v1.ExecuteBatchDmlRequest.Statement statements = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  java.util.List<? extends ExecuteBatchDmlRequest.StatementOrBuilder> getStatementsOrBuilderList();
  /**
   *
   *
   * <pre>
   * Required. The list of statements to execute in this batch. Statements are executed
   * serially, such that the effects of statement `i` are visible to statement
   * `i+1`. Each statement must be a DML statement. Execution stops at the
   * first failed statement; the remaining statements are not executed.
   * Callers must provide at least one statement.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.v1.ExecuteBatchDmlRequest.Statement statements = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  ExecuteBatchDmlRequest.StatementOrBuilder getStatementsOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * Required. A per-transaction sequence number used to identify this request. This field
   * makes each request idempotent such that if the request is received multiple
   * times, at most one will succeed.
   * The sequence number must be monotonically increasing within the
   * transaction. If a request arrives for the first time with an out-of-order
   * sequence number, the transaction may be aborted. Replays of previously
   * handled requests will yield the same response as the first execution.
   * </pre>
   *
   * <code>int64 seqno = 4 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The seqno.
   */
  long getSeqno();

  /**
   *
   *
   * <pre>
   * Common options for this request.
   * </pre>
   *
   * <code>.google.spanner.v1.RequestOptions request_options = 5;</code>
   *
   * @return Whether the requestOptions field is set.
   */
  boolean hasRequestOptions();
  /**
   *
   *
   * <pre>
   * Common options for this request.
   * </pre>
   *
   * <code>.google.spanner.v1.RequestOptions request_options = 5;</code>
   *
   * @return The requestOptions.
   */
  com.google.spanner.v1.RequestOptions getRequestOptions();
  /**
   *
   *
   * <pre>
   * Common options for this request.
   * </pre>
   *
   * <code>.google.spanner.v1.RequestOptions request_options = 5;</code>
   */
  com.google.spanner.v1.RequestOptionsOrBuilder getRequestOptionsOrBuilder();
}
