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

public interface PartitionQueryRequestOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.PartitionQueryRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The session used to create the partitions.
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
   * Required. The session used to create the partitions.
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
   * Read only snapshot transactions are supported, read/write and single use
   * transactions are not.
   * </pre>
   *
   * <code>.google.spanner.v1.TransactionSelector transaction = 2;</code>
   *
   * @return Whether the transaction field is set.
   */
  boolean hasTransaction();
  /**
   *
   *
   * <pre>
   * Read only snapshot transactions are supported, read/write and single use
   * transactions are not.
   * </pre>
   *
   * <code>.google.spanner.v1.TransactionSelector transaction = 2;</code>
   *
   * @return The transaction.
   */
  com.google.spanner.v1.TransactionSelector getTransaction();
  /**
   *
   *
   * <pre>
   * Read only snapshot transactions are supported, read/write and single use
   * transactions are not.
   * </pre>
   *
   * <code>.google.spanner.v1.TransactionSelector transaction = 2;</code>
   */
  com.google.spanner.v1.TransactionSelectorOrBuilder getTransactionOrBuilder();

  /**
   *
   *
   * <pre>
   * Required. The query request to generate partitions for. The request will fail if
   * the query is not root partitionable. The query plan of a root
   * partitionable query has a single distributed union operator. A distributed
   * union operator conceptually divides one or more tables into multiple
   * splits, remotely evaluates a subquery independently on each split, and
   * then unions all results.
   *
   * This must not contain DML commands, such as INSERT, UPDATE, or
   * DELETE. Use [ExecuteStreamingSql][google.spanner.v1.Spanner.ExecuteStreamingSql] with a
   * PartitionedDml transaction for large, partition-friendly DML operations.
   * </pre>
   *
   * <code>string sql = 3 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The sql.
   */
  java.lang.String getSql();
  /**
   *
   *
   * <pre>
   * Required. The query request to generate partitions for. The request will fail if
   * the query is not root partitionable. The query plan of a root
   * partitionable query has a single distributed union operator. A distributed
   * union operator conceptually divides one or more tables into multiple
   * splits, remotely evaluates a subquery independently on each split, and
   * then unions all results.
   *
   * This must not contain DML commands, such as INSERT, UPDATE, or
   * DELETE. Use [ExecuteStreamingSql][google.spanner.v1.Spanner.ExecuteStreamingSql] with a
   * PartitionedDml transaction for large, partition-friendly DML operations.
   * </pre>
   *
   * <code>string sql = 3 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The bytes for sql.
   */
  com.google.protobuf.ByteString getSqlBytes();

  /**
   *
   *
   * <pre>
   * Parameter names and values that bind to placeholders in the SQL string.
   *
   * A parameter placeholder consists of the `&#64;` character followed by the
   * parameter name (for example, `&#64;firstName`). Parameter names can contain
   * letters, numbers, and underscores.
   *
   * Parameters can appear anywhere that a literal value is expected.  The same
   * parameter name can be used more than once, for example:
   *
   * `"WHERE id &gt; &#64;msg_id AND id &lt; &#64;msg_id + 100"`
   *
   * It is an error to execute a SQL statement with unbound parameters.
   * </pre>
   *
   * <code>.google.protobuf.Struct params = 4;</code>
   *
   * @return Whether the params field is set.
   */
  boolean hasParams();
  /**
   *
   *
   * <pre>
   * Parameter names and values that bind to placeholders in the SQL string.
   *
   * A parameter placeholder consists of the `&#64;` character followed by the
   * parameter name (for example, `&#64;firstName`). Parameter names can contain
   * letters, numbers, and underscores.
   *
   * Parameters can appear anywhere that a literal value is expected.  The same
   * parameter name can be used more than once, for example:
   *
   * `"WHERE id &gt; &#64;msg_id AND id &lt; &#64;msg_id + 100"`
   *
   * It is an error to execute a SQL statement with unbound parameters.
   * </pre>
   *
   * <code>.google.protobuf.Struct params = 4;</code>
   *
   * @return The params.
   */
  com.google.protobuf.Struct getParams();
  /**
   *
   *
   * <pre>
   * Parameter names and values that bind to placeholders in the SQL string.
   *
   * A parameter placeholder consists of the `&#64;` character followed by the
   * parameter name (for example, `&#64;firstName`). Parameter names can contain
   * letters, numbers, and underscores.
   *
   * Parameters can appear anywhere that a literal value is expected.  The same
   * parameter name can be used more than once, for example:
   *
   * `"WHERE id &gt; &#64;msg_id AND id &lt; &#64;msg_id + 100"`
   *
   * It is an error to execute a SQL statement with unbound parameters.
   * </pre>
   *
   * <code>.google.protobuf.Struct params = 4;</code>
   */
  com.google.protobuf.StructOrBuilder getParamsOrBuilder();

  /**
   *
   *
   * <pre>
   * It is not always possible for Cloud Spanner to infer the right SQL type
   * from a JSON value.  For example, values of type `BYTES` and values
   * of type `STRING` both appear in [params][google.spanner.v1.PartitionQueryRequest.params] as JSON strings.
   *
   * In these cases, `param_types` can be used to specify the exact
   * SQL type for some or all of the SQL query parameters. See the
   * definition of [Type][google.spanner.v1.Type] for more information
   * about SQL types.
   * </pre>
   *
   * <code>map&lt;string, .google.spanner.v1.Type&gt; param_types = 5;</code>
   */
  int getParamTypesCount();
  /**
   *
   *
   * <pre>
   * It is not always possible for Cloud Spanner to infer the right SQL type
   * from a JSON value.  For example, values of type `BYTES` and values
   * of type `STRING` both appear in [params][google.spanner.v1.PartitionQueryRequest.params] as JSON strings.
   *
   * In these cases, `param_types` can be used to specify the exact
   * SQL type for some or all of the SQL query parameters. See the
   * definition of [Type][google.spanner.v1.Type] for more information
   * about SQL types.
   * </pre>
   *
   * <code>map&lt;string, .google.spanner.v1.Type&gt; param_types = 5;</code>
   */
  boolean containsParamTypes(java.lang.String key);
  /** Use {@link #getParamTypesMap()} instead. */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, com.google.spanner.v1.Type> getParamTypes();
  /**
   *
   *
   * <pre>
   * It is not always possible for Cloud Spanner to infer the right SQL type
   * from a JSON value.  For example, values of type `BYTES` and values
   * of type `STRING` both appear in [params][google.spanner.v1.PartitionQueryRequest.params] as JSON strings.
   *
   * In these cases, `param_types` can be used to specify the exact
   * SQL type for some or all of the SQL query parameters. See the
   * definition of [Type][google.spanner.v1.Type] for more information
   * about SQL types.
   * </pre>
   *
   * <code>map&lt;string, .google.spanner.v1.Type&gt; param_types = 5;</code>
   */
  java.util.Map<java.lang.String, com.google.spanner.v1.Type> getParamTypesMap();
  /**
   *
   *
   * <pre>
   * It is not always possible for Cloud Spanner to infer the right SQL type
   * from a JSON value.  For example, values of type `BYTES` and values
   * of type `STRING` both appear in [params][google.spanner.v1.PartitionQueryRequest.params] as JSON strings.
   *
   * In these cases, `param_types` can be used to specify the exact
   * SQL type for some or all of the SQL query parameters. See the
   * definition of [Type][google.spanner.v1.Type] for more information
   * about SQL types.
   * </pre>
   *
   * <code>map&lt;string, .google.spanner.v1.Type&gt; param_types = 5;</code>
   */
  /* nullable */
  com.google.spanner.v1.Type getParamTypesOrDefault(
      java.lang.String key,
      /* nullable */
      com.google.spanner.v1.Type defaultValue);
  /**
   *
   *
   * <pre>
   * It is not always possible for Cloud Spanner to infer the right SQL type
   * from a JSON value.  For example, values of type `BYTES` and values
   * of type `STRING` both appear in [params][google.spanner.v1.PartitionQueryRequest.params] as JSON strings.
   *
   * In these cases, `param_types` can be used to specify the exact
   * SQL type for some or all of the SQL query parameters. See the
   * definition of [Type][google.spanner.v1.Type] for more information
   * about SQL types.
   * </pre>
   *
   * <code>map&lt;string, .google.spanner.v1.Type&gt; param_types = 5;</code>
   */
  com.google.spanner.v1.Type getParamTypesOrThrow(java.lang.String key);

  /**
   *
   *
   * <pre>
   * Additional options that affect how many partitions are created.
   * </pre>
   *
   * <code>.google.spanner.v1.PartitionOptions partition_options = 6;</code>
   *
   * @return Whether the partitionOptions field is set.
   */
  boolean hasPartitionOptions();
  /**
   *
   *
   * <pre>
   * Additional options that affect how many partitions are created.
   * </pre>
   *
   * <code>.google.spanner.v1.PartitionOptions partition_options = 6;</code>
   *
   * @return The partitionOptions.
   */
  com.google.spanner.v1.PartitionOptions getPartitionOptions();
  /**
   *
   *
   * <pre>
   * Additional options that affect how many partitions are created.
   * </pre>
   *
   * <code>.google.spanner.v1.PartitionOptions partition_options = 6;</code>
   */
  com.google.spanner.v1.PartitionOptionsOrBuilder getPartitionOptionsOrBuilder();
}
