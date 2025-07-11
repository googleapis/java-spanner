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
// source: google/spanner/v1/keys.proto

// Protobuf Java Version: 3.25.8
package com.google.spanner.v1;

public interface KeyRangeOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.KeyRange)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * If the start is closed, then the range includes all rows whose
   * first `len(start_closed)` key columns exactly match `start_closed`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue start_closed = 1;</code>
   *
   * @return Whether the startClosed field is set.
   */
  boolean hasStartClosed();

  /**
   *
   *
   * <pre>
   * If the start is closed, then the range includes all rows whose
   * first `len(start_closed)` key columns exactly match `start_closed`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue start_closed = 1;</code>
   *
   * @return The startClosed.
   */
  com.google.protobuf.ListValue getStartClosed();

  /**
   *
   *
   * <pre>
   * If the start is closed, then the range includes all rows whose
   * first `len(start_closed)` key columns exactly match `start_closed`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue start_closed = 1;</code>
   */
  com.google.protobuf.ListValueOrBuilder getStartClosedOrBuilder();

  /**
   *
   *
   * <pre>
   * If the start is open, then the range excludes rows whose first
   * `len(start_open)` key columns exactly match `start_open`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue start_open = 2;</code>
   *
   * @return Whether the startOpen field is set.
   */
  boolean hasStartOpen();

  /**
   *
   *
   * <pre>
   * If the start is open, then the range excludes rows whose first
   * `len(start_open)` key columns exactly match `start_open`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue start_open = 2;</code>
   *
   * @return The startOpen.
   */
  com.google.protobuf.ListValue getStartOpen();

  /**
   *
   *
   * <pre>
   * If the start is open, then the range excludes rows whose first
   * `len(start_open)` key columns exactly match `start_open`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue start_open = 2;</code>
   */
  com.google.protobuf.ListValueOrBuilder getStartOpenOrBuilder();

  /**
   *
   *
   * <pre>
   * If the end is closed, then the range includes all rows whose
   * first `len(end_closed)` key columns exactly match `end_closed`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue end_closed = 3;</code>
   *
   * @return Whether the endClosed field is set.
   */
  boolean hasEndClosed();

  /**
   *
   *
   * <pre>
   * If the end is closed, then the range includes all rows whose
   * first `len(end_closed)` key columns exactly match `end_closed`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue end_closed = 3;</code>
   *
   * @return The endClosed.
   */
  com.google.protobuf.ListValue getEndClosed();

  /**
   *
   *
   * <pre>
   * If the end is closed, then the range includes all rows whose
   * first `len(end_closed)` key columns exactly match `end_closed`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue end_closed = 3;</code>
   */
  com.google.protobuf.ListValueOrBuilder getEndClosedOrBuilder();

  /**
   *
   *
   * <pre>
   * If the end is open, then the range excludes rows whose first
   * `len(end_open)` key columns exactly match `end_open`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue end_open = 4;</code>
   *
   * @return Whether the endOpen field is set.
   */
  boolean hasEndOpen();

  /**
   *
   *
   * <pre>
   * If the end is open, then the range excludes rows whose first
   * `len(end_open)` key columns exactly match `end_open`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue end_open = 4;</code>
   *
   * @return The endOpen.
   */
  com.google.protobuf.ListValue getEndOpen();

  /**
   *
   *
   * <pre>
   * If the end is open, then the range excludes rows whose first
   * `len(end_open)` key columns exactly match `end_open`.
   * </pre>
   *
   * <code>.google.protobuf.ListValue end_open = 4;</code>
   */
  com.google.protobuf.ListValueOrBuilder getEndOpenOrBuilder();

  com.google.spanner.v1.KeyRange.StartKeyTypeCase getStartKeyTypeCase();

  com.google.spanner.v1.KeyRange.EndKeyTypeCase getEndKeyTypeCase();
}
