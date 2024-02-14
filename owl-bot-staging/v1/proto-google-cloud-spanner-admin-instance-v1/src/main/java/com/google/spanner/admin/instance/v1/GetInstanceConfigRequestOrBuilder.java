// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

// Protobuf Java Version: 3.25.2
package com.google.spanner.admin.instance.v1;

public interface GetInstanceConfigRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.GetInstanceConfigRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Required. The name of the requested instance configuration. Values are of
   * the form `projects/&lt;project&gt;/instanceConfigs/&lt;config&gt;`.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }</code>
   * @return The name.
   */
  java.lang.String getName();
  /**
   * <pre>
   * Required. The name of the requested instance configuration. Values are of
   * the form `projects/&lt;project&gt;/instanceConfigs/&lt;config&gt;`.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();
}
