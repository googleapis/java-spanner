// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface SpannerAsyncActionRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.SpannerAsyncActionRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Action id to uniquely identify this action request.
   * </pre>
   *
   * <code>int32 action_id = 1;</code>
   * @return The actionId.
   */
  int getActionId();

  /**
   * <pre>
   * The actual SpannerAction to perform.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.SpannerAction action = 2;</code>
   * @return Whether the action field is set.
   */
  boolean hasAction();
  /**
   * <pre>
   * The actual SpannerAction to perform.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.SpannerAction action = 2;</code>
   * @return The action.
   */
  com.google.spanner.executor.v1.SpannerAction getAction();
  /**
   * <pre>
   * The actual SpannerAction to perform.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.SpannerAction action = 2;</code>
   */
  com.google.spanner.executor.v1.SpannerActionOrBuilder getActionOrBuilder();
}
