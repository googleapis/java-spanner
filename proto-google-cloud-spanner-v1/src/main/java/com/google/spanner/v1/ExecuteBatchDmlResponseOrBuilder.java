/*
 * Copyright 2023 Google LLC
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

public interface ExecuteBatchDmlResponseOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.ExecuteBatchDmlResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * One [ResultSet][google.spanner.v1.ResultSet] for each statement in the request that ran successfully,
   * in the same order as the statements in the request. Each [ResultSet][google.spanner.v1.ResultSet] does
   * not contain any rows. The [ResultSetStats][google.spanner.v1.ResultSetStats] in each [ResultSet][google.spanner.v1.ResultSet] contain
   * the number of rows modified by the statement.
   *
   * Only the first [ResultSet][google.spanner.v1.ResultSet] in the response contains valid
   * [ResultSetMetadata][google.spanner.v1.ResultSetMetadata].
   * </pre>
   *
   * <code>repeated .google.spanner.v1.ResultSet result_sets = 1;</code>
   */
  java.util.List<com.google.spanner.v1.ResultSet> getResultSetsList();
  /**
   *
   *
   * <pre>
   * One [ResultSet][google.spanner.v1.ResultSet] for each statement in the request that ran successfully,
   * in the same order as the statements in the request. Each [ResultSet][google.spanner.v1.ResultSet] does
   * not contain any rows. The [ResultSetStats][google.spanner.v1.ResultSetStats] in each [ResultSet][google.spanner.v1.ResultSet] contain
   * the number of rows modified by the statement.
   *
   * Only the first [ResultSet][google.spanner.v1.ResultSet] in the response contains valid
   * [ResultSetMetadata][google.spanner.v1.ResultSetMetadata].
   * </pre>
   *
   * <code>repeated .google.spanner.v1.ResultSet result_sets = 1;</code>
   */
  com.google.spanner.v1.ResultSet getResultSets(int index);
  /**
   *
   *
   * <pre>
   * One [ResultSet][google.spanner.v1.ResultSet] for each statement in the request that ran successfully,
   * in the same order as the statements in the request. Each [ResultSet][google.spanner.v1.ResultSet] does
   * not contain any rows. The [ResultSetStats][google.spanner.v1.ResultSetStats] in each [ResultSet][google.spanner.v1.ResultSet] contain
   * the number of rows modified by the statement.
   *
   * Only the first [ResultSet][google.spanner.v1.ResultSet] in the response contains valid
   * [ResultSetMetadata][google.spanner.v1.ResultSetMetadata].
   * </pre>
   *
   * <code>repeated .google.spanner.v1.ResultSet result_sets = 1;</code>
   */
  int getResultSetsCount();
  /**
   *
   *
   * <pre>
   * One [ResultSet][google.spanner.v1.ResultSet] for each statement in the request that ran successfully,
   * in the same order as the statements in the request. Each [ResultSet][google.spanner.v1.ResultSet] does
   * not contain any rows. The [ResultSetStats][google.spanner.v1.ResultSetStats] in each [ResultSet][google.spanner.v1.ResultSet] contain
   * the number of rows modified by the statement.
   *
   * Only the first [ResultSet][google.spanner.v1.ResultSet] in the response contains valid
   * [ResultSetMetadata][google.spanner.v1.ResultSetMetadata].
   * </pre>
   *
   * <code>repeated .google.spanner.v1.ResultSet result_sets = 1;</code>
   */
  java.util.List<? extends com.google.spanner.v1.ResultSetOrBuilder> getResultSetsOrBuilderList();
  /**
   *
   *
   * <pre>
   * One [ResultSet][google.spanner.v1.ResultSet] for each statement in the request that ran successfully,
   * in the same order as the statements in the request. Each [ResultSet][google.spanner.v1.ResultSet] does
   * not contain any rows. The [ResultSetStats][google.spanner.v1.ResultSetStats] in each [ResultSet][google.spanner.v1.ResultSet] contain
   * the number of rows modified by the statement.
   *
   * Only the first [ResultSet][google.spanner.v1.ResultSet] in the response contains valid
   * [ResultSetMetadata][google.spanner.v1.ResultSetMetadata].
   * </pre>
   *
   * <code>repeated .google.spanner.v1.ResultSet result_sets = 1;</code>
   */
  com.google.spanner.v1.ResultSetOrBuilder getResultSetsOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * If all DML statements are executed successfully, the status is `OK`.
   * Otherwise, the error status of the first failed statement.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   *
   * @return Whether the status field is set.
   */
  boolean hasStatus();
  /**
   *
   *
   * <pre>
   * If all DML statements are executed successfully, the status is `OK`.
   * Otherwise, the error status of the first failed statement.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   *
   * @return The status.
   */
  com.google.rpc.Status getStatus();
  /**
   *
   *
   * <pre>
   * If all DML statements are executed successfully, the status is `OK`.
   * Otherwise, the error status of the first failed statement.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   */
  com.google.rpc.StatusOrBuilder getStatusOrBuilder();
}
