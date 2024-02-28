// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

// Protobuf Java Version: 3.25.2
package com.google.spanner.admin.instance.v1;

public interface ListInstancePartitionOperationsRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.ListInstancePartitionOperationsRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Required. The parent instance of the instance partition operations.
   * Values are of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * </pre>
   *
   * <code>string parent = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }</code>
   * @return The parent.
   */
  java.lang.String getParent();
  /**
   * <pre>
   * Required. The parent instance of the instance partition operations.
   * Values are of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * </pre>
   *
   * <code>string parent = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }</code>
   * @return The bytes for parent.
   */
  com.google.protobuf.ByteString
      getParentBytes();

  /**
   * <pre>
   * Optional. An expression that filters the list of returned operations.
   *
   * A filter expression consists of a field name, a
   * comparison operator, and a value for filtering.
   * The value must be a string, a number, or a boolean. The comparison operator
   * must be one of: `&lt;`, `&gt;`, `&lt;=`, `&gt;=`, `!=`, `=`, or `:`.
   * Colon `:` is the contains operator. Filter rules are not case sensitive.
   *
   * The following fields in the [Operation][google.longrunning.Operation]
   * are eligible for filtering:
   *
   *   * `name` - The name of the long-running operation
   *   * `done` - False if the operation is in progress, else true.
   *   * `metadata.&#64;type` - the type of metadata. For example, the type string
   *      for
   *      [CreateInstancePartitionMetadata][google.spanner.admin.instance.v1.CreateInstancePartitionMetadata]
   *      is
   *      `type.googleapis.com/google.spanner.admin.instance.v1.CreateInstancePartitionMetadata`.
   *   * `metadata.&lt;field_name&gt;` - any field in metadata.value.
   *      `metadata.&#64;type` must be specified first, if filtering on metadata
   *      fields.
   *   * `error` - Error associated with the long-running operation.
   *   * `response.&#64;type` - the type of response.
   *   * `response.&lt;field_name&gt;` - any field in response.value.
   *
   * You can combine multiple expressions by enclosing each expression in
   * parentheses. By default, expressions are combined with AND logic. However,
   * you can specify AND, OR, and NOT logic explicitly.
   *
   * Here are a few examples:
   *
   *   * `done:true` - The operation is complete.
   *   * `(metadata.&#64;type=` &#92;
   *     `type.googleapis.com/google.spanner.admin.instance.v1.CreateInstancePartitionMetadata)
   *     AND` &#92;
   *     `(metadata.instance_partition.name:custom-instance-partition) AND` &#92;
   *     `(metadata.start_time &lt; &#92;"2021-03-28T14:50:00Z&#92;") AND` &#92;
   *     `(error:*)` - Return operations where:
   *     * The operation's metadata type is
   *     [CreateInstancePartitionMetadata][google.spanner.admin.instance.v1.CreateInstancePartitionMetadata].
   *     * The instance partition name contains "custom-instance-partition".
   *     * The operation started before 2021-03-28T14:50:00Z.
   *     * The operation resulted in an error.
   * </pre>
   *
   * <code>string filter = 2 [(.google.api.field_behavior) = OPTIONAL];</code>
   * @return The filter.
   */
  java.lang.String getFilter();
  /**
   * <pre>
   * Optional. An expression that filters the list of returned operations.
   *
   * A filter expression consists of a field name, a
   * comparison operator, and a value for filtering.
   * The value must be a string, a number, or a boolean. The comparison operator
   * must be one of: `&lt;`, `&gt;`, `&lt;=`, `&gt;=`, `!=`, `=`, or `:`.
   * Colon `:` is the contains operator. Filter rules are not case sensitive.
   *
   * The following fields in the [Operation][google.longrunning.Operation]
   * are eligible for filtering:
   *
   *   * `name` - The name of the long-running operation
   *   * `done` - False if the operation is in progress, else true.
   *   * `metadata.&#64;type` - the type of metadata. For example, the type string
   *      for
   *      [CreateInstancePartitionMetadata][google.spanner.admin.instance.v1.CreateInstancePartitionMetadata]
   *      is
   *      `type.googleapis.com/google.spanner.admin.instance.v1.CreateInstancePartitionMetadata`.
   *   * `metadata.&lt;field_name&gt;` - any field in metadata.value.
   *      `metadata.&#64;type` must be specified first, if filtering on metadata
   *      fields.
   *   * `error` - Error associated with the long-running operation.
   *   * `response.&#64;type` - the type of response.
   *   * `response.&lt;field_name&gt;` - any field in response.value.
   *
   * You can combine multiple expressions by enclosing each expression in
   * parentheses. By default, expressions are combined with AND logic. However,
   * you can specify AND, OR, and NOT logic explicitly.
   *
   * Here are a few examples:
   *
   *   * `done:true` - The operation is complete.
   *   * `(metadata.&#64;type=` &#92;
   *     `type.googleapis.com/google.spanner.admin.instance.v1.CreateInstancePartitionMetadata)
   *     AND` &#92;
   *     `(metadata.instance_partition.name:custom-instance-partition) AND` &#92;
   *     `(metadata.start_time &lt; &#92;"2021-03-28T14:50:00Z&#92;") AND` &#92;
   *     `(error:*)` - Return operations where:
   *     * The operation's metadata type is
   *     [CreateInstancePartitionMetadata][google.spanner.admin.instance.v1.CreateInstancePartitionMetadata].
   *     * The instance partition name contains "custom-instance-partition".
   *     * The operation started before 2021-03-28T14:50:00Z.
   *     * The operation resulted in an error.
   * </pre>
   *
   * <code>string filter = 2 [(.google.api.field_behavior) = OPTIONAL];</code>
   * @return The bytes for filter.
   */
  com.google.protobuf.ByteString
      getFilterBytes();

  /**
   * <pre>
   * Optional. Number of operations to be returned in the response. If 0 or
   * less, defaults to the server's maximum allowed page size.
   * </pre>
   *
   * <code>int32 page_size = 3 [(.google.api.field_behavior) = OPTIONAL];</code>
   * @return The pageSize.
   */
  int getPageSize();

  /**
   * <pre>
   * Optional. If non-empty, `page_token` should contain a
   * [next_page_token][google.spanner.admin.instance.v1.ListInstancePartitionOperationsResponse.next_page_token]
   * from a previous
   * [ListInstancePartitionOperationsResponse][google.spanner.admin.instance.v1.ListInstancePartitionOperationsResponse]
   * to the same `parent` and with the same `filter`.
   * </pre>
   *
   * <code>string page_token = 4 [(.google.api.field_behavior) = OPTIONAL];</code>
   * @return The pageToken.
   */
  java.lang.String getPageToken();
  /**
   * <pre>
   * Optional. If non-empty, `page_token` should contain a
   * [next_page_token][google.spanner.admin.instance.v1.ListInstancePartitionOperationsResponse.next_page_token]
   * from a previous
   * [ListInstancePartitionOperationsResponse][google.spanner.admin.instance.v1.ListInstancePartitionOperationsResponse]
   * to the same `parent` and with the same `filter`.
   * </pre>
   *
   * <code>string page_token = 4 [(.google.api.field_behavior) = OPTIONAL];</code>
   * @return The bytes for pageToken.
   */
  com.google.protobuf.ByteString
      getPageTokenBytes();

  /**
   * <pre>
   * Optional. Deadline used while retrieving metadata for instance partition
   * operations. Instance partitions whose operation metadata cannot be
   * retrieved within this deadline will be added to
   * [unreachable][ListInstancePartitionOperationsResponse.unreachable] in
   * [ListInstancePartitionOperationsResponse][google.spanner.admin.instance.v1.ListInstancePartitionOperationsResponse].
   * </pre>
   *
   * <code>.google.protobuf.Timestamp instance_partition_deadline = 5 [(.google.api.field_behavior) = OPTIONAL];</code>
   * @return Whether the instancePartitionDeadline field is set.
   */
  boolean hasInstancePartitionDeadline();
  /**
   * <pre>
   * Optional. Deadline used while retrieving metadata for instance partition
   * operations. Instance partitions whose operation metadata cannot be
   * retrieved within this deadline will be added to
   * [unreachable][ListInstancePartitionOperationsResponse.unreachable] in
   * [ListInstancePartitionOperationsResponse][google.spanner.admin.instance.v1.ListInstancePartitionOperationsResponse].
   * </pre>
   *
   * <code>.google.protobuf.Timestamp instance_partition_deadline = 5 [(.google.api.field_behavior) = OPTIONAL];</code>
   * @return The instancePartitionDeadline.
   */
  com.google.protobuf.Timestamp getInstancePartitionDeadline();
  /**
   * <pre>
   * Optional. Deadline used while retrieving metadata for instance partition
   * operations. Instance partitions whose operation metadata cannot be
   * retrieved within this deadline will be added to
   * [unreachable][ListInstancePartitionOperationsResponse.unreachable] in
   * [ListInstancePartitionOperationsResponse][google.spanner.admin.instance.v1.ListInstancePartitionOperationsResponse].
   * </pre>
   *
   * <code>.google.protobuf.Timestamp instance_partition_deadline = 5 [(.google.api.field_behavior) = OPTIONAL];</code>
   */
  com.google.protobuf.TimestampOrBuilder getInstancePartitionDeadlineOrBuilder();
}
