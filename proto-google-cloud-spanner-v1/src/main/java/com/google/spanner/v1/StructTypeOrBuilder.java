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
// source: google/spanner/v1/type.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.v1;

public interface StructTypeOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.StructType)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The list of fields that make up this struct. Order is
   * significant, because values of this struct type are represented as
   * lists, where the order of field values matches the order of
   * fields in the [StructType][google.spanner.v1.StructType]. In turn, the order of fields
   * matches the order of columns in a read request, or the order of
   * fields in the `SELECT` clause of a query.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.StructType.Field fields = 1;</code>
   */
  java.util.List<com.google.spanner.v1.StructType.Field> getFieldsList();
  /**
   *
   *
   * <pre>
   * The list of fields that make up this struct. Order is
   * significant, because values of this struct type are represented as
   * lists, where the order of field values matches the order of
   * fields in the [StructType][google.spanner.v1.StructType]. In turn, the order of fields
   * matches the order of columns in a read request, or the order of
   * fields in the `SELECT` clause of a query.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.StructType.Field fields = 1;</code>
   */
  com.google.spanner.v1.StructType.Field getFields(int index);
  /**
   *
   *
   * <pre>
   * The list of fields that make up this struct. Order is
   * significant, because values of this struct type are represented as
   * lists, where the order of field values matches the order of
   * fields in the [StructType][google.spanner.v1.StructType]. In turn, the order of fields
   * matches the order of columns in a read request, or the order of
   * fields in the `SELECT` clause of a query.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.StructType.Field fields = 1;</code>
   */
  int getFieldsCount();
  /**
   *
   *
   * <pre>
   * The list of fields that make up this struct. Order is
   * significant, because values of this struct type are represented as
   * lists, where the order of field values matches the order of
   * fields in the [StructType][google.spanner.v1.StructType]. In turn, the order of fields
   * matches the order of columns in a read request, or the order of
   * fields in the `SELECT` clause of a query.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.StructType.Field fields = 1;</code>
   */
  java.util.List<? extends com.google.spanner.v1.StructType.FieldOrBuilder>
      getFieldsOrBuilderList();
  /**
   *
   *
   * <pre>
   * The list of fields that make up this struct. Order is
   * significant, because values of this struct type are represented as
   * lists, where the order of field values matches the order of
   * fields in the [StructType][google.spanner.v1.StructType]. In turn, the order of fields
   * matches the order of columns in a read request, or the order of
   * fields in the `SELECT` clause of a query.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.StructType.Field fields = 1;</code>
   */
  com.google.spanner.v1.StructType.FieldOrBuilder getFieldsOrBuilder(int index);
}
