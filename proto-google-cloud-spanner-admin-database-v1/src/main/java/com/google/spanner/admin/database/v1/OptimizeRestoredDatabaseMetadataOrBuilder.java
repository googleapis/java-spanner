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
// source: google/spanner/admin/database/v1/spanner_database_admin.proto

// Protobuf Java Version: 3.25.2
package com.google.spanner.admin.database.v1;

public interface OptimizeRestoredDatabaseMetadataOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.OptimizeRestoredDatabaseMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Name of the restored database being optimized.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The name.
   */
  java.lang.String getName();
  /**
   *
   *
   * <pre>
   * Name of the restored database being optimized.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   *
   *
   * <pre>
   * The progress of the post-restore optimizations.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 2;</code>
   *
   * @return Whether the progress field is set.
   */
  boolean hasProgress();
  /**
   *
   *
   * <pre>
   * The progress of the post-restore optimizations.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 2;</code>
   *
   * @return The progress.
   */
  com.google.spanner.admin.database.v1.OperationProgress getProgress();
  /**
   *
   *
   * <pre>
   * The progress of the post-restore optimizations.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 2;</code>
   */
  com.google.spanner.admin.database.v1.OperationProgressOrBuilder getProgressOrBuilder();
}
