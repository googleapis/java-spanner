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

// Protobuf Java Version: 3.25.5
package com.google.spanner.executor.v1;

public interface ChangeQuorumCloudDatabaseActionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.ChangeQuorumCloudDatabaseAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The fully qualified uri of the database whose quorum has to be changed.
   * </pre>
   *
   * <code>optional string database_uri = 1;</code>
   *
   * @return Whether the databaseUri field is set.
   */
  boolean hasDatabaseUri();
  /**
   *
   *
   * <pre>
   * The fully qualified uri of the database whose quorum has to be changed.
   * </pre>
   *
   * <code>optional string database_uri = 1;</code>
   *
   * @return The databaseUri.
   */
  java.lang.String getDatabaseUri();
  /**
   *
   *
   * <pre>
   * The fully qualified uri of the database whose quorum has to be changed.
   * </pre>
   *
   * <code>optional string database_uri = 1;</code>
   *
   * @return The bytes for databaseUri.
   */
  com.google.protobuf.ByteString getDatabaseUriBytes();

  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @return A list containing the servingLocations.
   */
  java.util.List<java.lang.String> getServingLocationsList();
  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @return The count of servingLocations.
   */
  int getServingLocationsCount();
  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @param index The index of the element to return.
   * @return The servingLocations at the given index.
   */
  java.lang.String getServingLocations(int index);
  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @param index The index of the value to return.
   * @return The bytes of the servingLocations at the given index.
   */
  com.google.protobuf.ByteString getServingLocationsBytes(int index);
}
