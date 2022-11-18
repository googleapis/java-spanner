/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

public interface OperationResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.OperationResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * List of operations returned by ListOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_operations = 1;</code>
   */
  java.util.List<com.google.longrunning.Operation> 
      getListedOperationsList();
  /**
   * <pre>
   * List of operations returned by ListOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_operations = 1;</code>
   */
  com.google.longrunning.Operation getListedOperations(int index);
  /**
   * <pre>
   * List of operations returned by ListOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_operations = 1;</code>
   */
  int getListedOperationsCount();
  /**
   * <pre>
   * List of operations returned by ListOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_operations = 1;</code>
   */
  java.util.List<? extends com.google.longrunning.OperationOrBuilder> 
      getListedOperationsOrBuilderList();
  /**
   * <pre>
   * List of operations returned by ListOperationsAction.
   * </pre>
   *
   * <code>repeated .google.longrunning.Operation listed_operations = 1;</code>
   */
  com.google.longrunning.OperationOrBuilder getListedOperationsOrBuilder(
      int index);

  /**
   * <pre>
   * "next_page_token" can be sent in a subsequent list action
   * to fetch more of the matching data.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   * @return The nextPageToken.
   */
  java.lang.String getNextPageToken();
  /**
   * <pre>
   * "next_page_token" can be sent in a subsequent list action
   * to fetch more of the matching data.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   * @return The bytes for nextPageToken.
   */
  com.google.protobuf.ByteString
      getNextPageTokenBytes();

  /**
   * <pre>
   * Operation returned by GetOperationAction.
   * </pre>
   *
   * <code>.google.longrunning.Operation operation = 3;</code>
   * @return Whether the operation field is set.
   */
  boolean hasOperation();
  /**
   * <pre>
   * Operation returned by GetOperationAction.
   * </pre>
   *
   * <code>.google.longrunning.Operation operation = 3;</code>
   * @return The operation.
   */
  com.google.longrunning.Operation getOperation();
  /**
   * <pre>
   * Operation returned by GetOperationAction.
   * </pre>
   *
   * <code>.google.longrunning.Operation operation = 3;</code>
   */
  com.google.longrunning.OperationOrBuilder getOperationOrBuilder();
}
