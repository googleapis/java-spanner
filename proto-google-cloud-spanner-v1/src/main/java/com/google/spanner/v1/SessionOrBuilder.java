/*
 * Copyright 2020 Google LLC
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
// source: google/spanner/v1/spanner.proto

package com.google.spanner.v1;

public interface SessionOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.Session)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Output only. The name of the session. This is always system-assigned.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   *
   * @return The name.
   */
  java.lang.String getName();
  /**
   *
   *
   * <pre>
   * Output only. The name of the session. This is always system-assigned.
   * </pre>
   *
   * <code>string name = 1 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   *
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   *
   *
   * <pre>
   * The labels for the session.
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z]([-a-z0-9]*[a-z0-9])?`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `([a-z]([-a-z0-9]*[a-z0-9])?)?`.
   *  * No more than 64 labels can be associated with a given session.
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 2;</code>
   */
  int getLabelsCount();
  /**
   *
   *
   * <pre>
   * The labels for the session.
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z]([-a-z0-9]*[a-z0-9])?`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `([a-z]([-a-z0-9]*[a-z0-9])?)?`.
   *  * No more than 64 labels can be associated with a given session.
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 2;</code>
   */
  boolean containsLabels(java.lang.String key);
  /** Use {@link #getLabelsMap()} instead. */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.String> getLabels();
  /**
   *
   *
   * <pre>
   * The labels for the session.
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z]([-a-z0-9]*[a-z0-9])?`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `([a-z]([-a-z0-9]*[a-z0-9])?)?`.
   *  * No more than 64 labels can be associated with a given session.
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 2;</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getLabelsMap();
  /**
   *
   *
   * <pre>
   * The labels for the session.
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z]([-a-z0-9]*[a-z0-9])?`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `([a-z]([-a-z0-9]*[a-z0-9])?)?`.
   *  * No more than 64 labels can be associated with a given session.
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 2;</code>
   */

  /* nullable */
  java.lang.String getLabelsOrDefault(
      java.lang.String key,
      /* nullable */
      java.lang.String defaultValue);
  /**
   *
   *
   * <pre>
   * The labels for the session.
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z]([-a-z0-9]*[a-z0-9])?`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `([a-z]([-a-z0-9]*[a-z0-9])?)?`.
   *  * No more than 64 labels can be associated with a given session.
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 2;</code>
   */
  java.lang.String getLabelsOrThrow(java.lang.String key);

  /**
   *
   *
   * <pre>
   * Output only. The timestamp when the session is created.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp create_time = 3 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return Whether the createTime field is set.
   */
  boolean hasCreateTime();
  /**
   *
   *
   * <pre>
   * Output only. The timestamp when the session is created.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp create_time = 3 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The createTime.
   */
  com.google.protobuf.Timestamp getCreateTime();
  /**
   *
   *
   * <pre>
   * Output only. The timestamp when the session is created.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp create_time = 3 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  com.google.protobuf.TimestampOrBuilder getCreateTimeOrBuilder();

  /**
   *
   *
   * <pre>
   * Output only. The approximate timestamp when the session is last used. It is
   * typically earlier than the actual last use time.
   * </pre>
   *
   * <code>
   * .google.protobuf.Timestamp approximate_last_use_time = 4 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return Whether the approximateLastUseTime field is set.
   */
  boolean hasApproximateLastUseTime();
  /**
   *
   *
   * <pre>
   * Output only. The approximate timestamp when the session is last used. It is
   * typically earlier than the actual last use time.
   * </pre>
   *
   * <code>
   * .google.protobuf.Timestamp approximate_last_use_time = 4 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The approximateLastUseTime.
   */
  com.google.protobuf.Timestamp getApproximateLastUseTime();
  /**
   *
   *
   * <pre>
   * Output only. The approximate timestamp when the session is last used. It is
   * typically earlier than the actual last use time.
   * </pre>
   *
   * <code>
   * .google.protobuf.Timestamp approximate_last_use_time = 4 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  com.google.protobuf.TimestampOrBuilder getApproximateLastUseTimeOrBuilder();
}
