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
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

package com.google.spanner.admin.instance.v1;

public interface CreateInstanceRequestOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.CreateInstanceRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The name of the project in which to create the instance. Values
   * are of the form `projects/&lt;project&gt;`.
   * </pre>
   *
   * <code>
   * string parent = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The parent.
   */
  java.lang.String getParent();
  /**
   *
   *
   * <pre>
   * Required. The name of the project in which to create the instance. Values
   * are of the form `projects/&lt;project&gt;`.
   * </pre>
   *
   * <code>
   * string parent = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The bytes for parent.
   */
  com.google.protobuf.ByteString getParentBytes();

  /**
   *
   *
   * <pre>
   * Required. The ID of the instance to create.  Valid identifiers are of the
   * form `[a-z][-a-z0-9]*[a-z0-9]` and must be between 2 and 64 characters in
   * length.
   * </pre>
   *
   * <code>string instance_id = 2 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The instanceId.
   */
  java.lang.String getInstanceId();
  /**
   *
   *
   * <pre>
   * Required. The ID of the instance to create.  Valid identifiers are of the
   * form `[a-z][-a-z0-9]*[a-z0-9]` and must be between 2 and 64 characters in
   * length.
   * </pre>
   *
   * <code>string instance_id = 2 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The bytes for instanceId.
   */
  com.google.protobuf.ByteString getInstanceIdBytes();

  /**
   *
   *
   * <pre>
   * Required. The instance to create.  The name may be omitted, but if
   * specified must be `&lt;parent&gt;/instances/&lt;instance_id&gt;`.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.Instance instance = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return Whether the instance field is set.
   */
  boolean hasInstance();
  /**
   *
   *
   * <pre>
   * Required. The instance to create.  The name may be omitted, but if
   * specified must be `&lt;parent&gt;/instances/&lt;instance_id&gt;`.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.Instance instance = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return The instance.
   */
  com.google.spanner.admin.instance.v1.Instance getInstance();
  /**
   *
   *
   * <pre>
   * Required. The instance to create.  The name may be omitted, but if
   * specified must be `&lt;parent&gt;/instances/&lt;instance_id&gt;`.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.Instance instance = 3 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   */
  com.google.spanner.admin.instance.v1.InstanceOrBuilder getInstanceOrBuilder();
}
