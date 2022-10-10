/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/executor_service.proto

package com.google.spanner.executor.v1;

public interface SpannerAsyncActionRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.SpannerAsyncActionRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int32 action_id = 1;</code>
   * @return The actionId.
   */
  int getActionId();

  /**
   * <code>.google.spanner.executor.v1.SpannerAction action = 2;</code>
   * @return Whether the action field is set.
   */
  boolean hasAction();
  /**
   * <code>.google.spanner.executor.v1.SpannerAction action = 2;</code>
   * @return The action.
   */
  com.google.spanner.executor.v1.SpannerAction getAction();
  /**
   * <code>.google.spanner.executor.v1.SpannerAction action = 2;</code>
   */
  com.google.spanner.executor.v1.SpannerActionOrBuilder getActionOrBuilder();

  /**
   * <code>bool cancel = 3;</code>
   * @return Whether the cancel field is set.
   */
  boolean hasCancel();
  /**
   * <code>bool cancel = 3;</code>
   * @return The cancel.
   */
  boolean getCancel();

  public com.google.spanner.executor.v1.SpannerAsyncActionRequest.KindCase getKindCase();
}
