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

// Protobuf Java Version: 3.25.8
package com.google.spanner.executor.v1;

public interface HeartbeatRecordOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.HeartbeatRecord)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Timestamp for this heartbeat check.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp heartbeat_time = 1;</code>
   *
   * @return Whether the heartbeatTime field is set.
   */
  boolean hasHeartbeatTime();

  /**
   *
   *
   * <pre>
   * Timestamp for this heartbeat check.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp heartbeat_time = 1;</code>
   *
   * @return The heartbeatTime.
   */
  com.google.protobuf.Timestamp getHeartbeatTime();

  /**
   *
   *
   * <pre>
   * Timestamp for this heartbeat check.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp heartbeat_time = 1;</code>
   */
  com.google.protobuf.TimestampOrBuilder getHeartbeatTimeOrBuilder();
}
