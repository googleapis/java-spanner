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
// source: google/spanner/executor/v1/cloud_executor.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.executor.v1;

public interface BatchDmlActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.BatchDmlAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * DML statements.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction updates = 1;</code>
   */
  java.util.List<com.google.spanner.executor.v1.QueryAction> getUpdatesList();
  /**
   *
   *
   * <pre>
   * DML statements.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction updates = 1;</code>
   */
  com.google.spanner.executor.v1.QueryAction getUpdates(int index);
  /**
   *
   *
   * <pre>
   * DML statements.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction updates = 1;</code>
   */
  int getUpdatesCount();
  /**
   *
   *
   * <pre>
   * DML statements.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction updates = 1;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.QueryActionOrBuilder>
      getUpdatesOrBuilderList();
  /**
   *
   *
   * <pre>
   * DML statements.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction updates = 1;</code>
   */
  com.google.spanner.executor.v1.QueryActionOrBuilder getUpdatesOrBuilder(int index);
}
