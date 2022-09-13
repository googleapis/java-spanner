// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

package com.google.spanner.admin.instance.v1;

public interface UpdateInstanceConfigMetadataOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The desired instance config after updating.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.InstanceConfig instance_config = 1;</code>
   * @return Whether the instanceConfig field is set.
   */
  boolean hasInstanceConfig();
  /**
   * <pre>
   * The desired instance config after updating.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.InstanceConfig instance_config = 1;</code>
   * @return The instanceConfig.
   */
  com.google.spanner.admin.instance.v1.InstanceConfig getInstanceConfig();
  /**
   * <pre>
   * The desired instance config after updating.
   * </pre>
   *
   * <code>.google.spanner.admin.instance.v1.InstanceConfig instance_config = 1;</code>
   */
  com.google.spanner.admin.instance.v1.InstanceConfigOrBuilder getInstanceConfigOrBuilder();

  /**
   * <pre>
   * The progress of the
   * [UpdateInstanceConfig][google.spanner.admin.instance.v1.InstanceAdmin.UpdateInstanceConfig]
   * operation.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 2;</code>
   * @return Whether the progress field is set.
   */
  boolean hasProgress();
  /**
   * <pre>
   * The progress of the
   * [UpdateInstanceConfig][google.spanner.admin.instance.v1.InstanceAdmin.UpdateInstanceConfig]
   * operation.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 2;</code>
   * @return The progress.
   */
  com.google.spanner.admin.database.v1.OperationProgress getProgress();
  /**
   * <pre>
   * The progress of the
   * [UpdateInstanceConfig][google.spanner.admin.instance.v1.InstanceAdmin.UpdateInstanceConfig]
   * operation.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.OperationProgress progress = 2;</code>
   */
  com.google.spanner.admin.database.v1.OperationProgressOrBuilder getProgressOrBuilder();

  /**
   * <pre>
   * The time at which this operation was cancelled.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 3;</code>
   * @return Whether the cancelTime field is set.
   */
  boolean hasCancelTime();
  /**
   * <pre>
   * The time at which this operation was cancelled.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 3;</code>
   * @return The cancelTime.
   */
  com.google.protobuf.Timestamp getCancelTime();
  /**
   * <pre>
   * The time at which this operation was cancelled.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp cancel_time = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getCancelTimeOrBuilder();
}
