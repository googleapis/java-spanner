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
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

// Protobuf Java Version: 3.25.4
package com.google.spanner.admin.instance.v1;

public interface GetInstanceRequestOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.GetInstanceRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The name of the requested instance. Values are of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * </pre>
   *
   * <code>
   * string name = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The name.
   */
  java.lang.String getName();
  /**
   *
   *
   * <pre>
   * Required. The name of the requested instance. Values are of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * </pre>
   *
   * <code>
   * string name = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   *
   *
   * <pre>
   * If field_mask is present, specifies the subset of
   * [Instance][google.spanner.admin.instance.v1.Instance] fields that should be
   * returned. If absent, all
   * [Instance][google.spanner.admin.instance.v1.Instance] fields are returned.
   * </pre>
   *
   * <code>.google.protobuf.FieldMask field_mask = 2;</code>
   *
   * @return Whether the fieldMask field is set.
   */
  boolean hasFieldMask();
  /**
   *
   *
   * <pre>
   * If field_mask is present, specifies the subset of
   * [Instance][google.spanner.admin.instance.v1.Instance] fields that should be
   * returned. If absent, all
   * [Instance][google.spanner.admin.instance.v1.Instance] fields are returned.
   * </pre>
   *
   * <code>.google.protobuf.FieldMask field_mask = 2;</code>
   *
   * @return The fieldMask.
   */
  com.google.protobuf.FieldMask getFieldMask();
  /**
   *
   *
   * <pre>
   * If field_mask is present, specifies the subset of
   * [Instance][google.spanner.admin.instance.v1.Instance] fields that should be
   * returned. If absent, all
   * [Instance][google.spanner.admin.instance.v1.Instance] fields are returned.
   * </pre>
   *
   * <code>.google.protobuf.FieldMask field_mask = 2;</code>
   */
  com.google.protobuf.FieldMaskOrBuilder getFieldMaskOrBuilder();
}
