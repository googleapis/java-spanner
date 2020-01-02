/*
 * Copyright 2019 Google LLC
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

public interface ListInstanceConfigsResponseOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.ListInstanceConfigsResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * The list of requested instance configurations.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.InstanceConfig instance_configs = 1;</code>
   */
  java.util.List<com.google.spanner.admin.instance.v1.InstanceConfig> getInstanceConfigsList();
  /**
   *
   *
   * <pre>
   * The list of requested instance configurations.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.InstanceConfig instance_configs = 1;</code>
   */
  com.google.spanner.admin.instance.v1.InstanceConfig getInstanceConfigs(int index);
  /**
   *
   *
   * <pre>
   * The list of requested instance configurations.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.InstanceConfig instance_configs = 1;</code>
   */
  int getInstanceConfigsCount();
  /**
   *
   *
   * <pre>
   * The list of requested instance configurations.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.InstanceConfig instance_configs = 1;</code>
   */
  java.util.List<? extends com.google.spanner.admin.instance.v1.InstanceConfigOrBuilder>
      getInstanceConfigsOrBuilderList();
  /**
   *
   *
   * <pre>
   * The list of requested instance configurations.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.InstanceConfig instance_configs = 1;</code>
   */
  com.google.spanner.admin.instance.v1.InstanceConfigOrBuilder getInstanceConfigsOrBuilder(
      int index);

  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListInstanceConfigs][google.spanner.admin.instance.v1.InstanceAdmin.ListInstanceConfigs] call to
   * fetch more of the matching instance configurations.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The nextPageToken.
   */
  java.lang.String getNextPageToken();
  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListInstanceConfigs][google.spanner.admin.instance.v1.InstanceAdmin.ListInstanceConfigs] call to
   * fetch more of the matching instance configurations.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The bytes for nextPageToken.
   */
  com.google.protobuf.ByteString getNextPageTokenBytes();
}
