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
// source: google/spanner/admin/instance/v1/common.proto

package com.google.spanner.admin.instance.v1;

public final class CommonProto {
  private CommonProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_instance_v1_OperationProgress_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_instance_v1_OperationProgress_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n-google/spanner/admin/instance/v1/commo"
          + "n.proto\022 google.spanner.admin.instance.v"
          + "1\032\037google/protobuf/timestamp.proto\"\213\001\n\021O"
          + "perationProgress\022\030\n\020progress_percent\030\001 \001"
          + "(\005\022.\n\nstart_time\030\002 \001(\0132\032.google.protobuf"
          + ".Timestamp\022,\n\010end_time\030\003 \001(\0132\032.google.pr"
          + "otobuf.TimestampB\377\001\n$com.google.spanner."
          + "admin.instance.v1B\013CommonProtoP\001ZHgoogle"
          + ".golang.org/genproto/googleapis/spanner/"
          + "admin/instance/v1;instance\252\002&Google.Clou"
          + "d.Spanner.Admin.Instance.V1\312\002&Google\\Clo"
          + "ud\\Spanner\\Admin\\Instance\\V1\352\002+Google::C"
          + "loud::Spanner::Admin::Instance::V1b\006prot"
          + "o3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
              com.google.protobuf.TimestampProto.getDescriptor(),
            });
    internal_static_google_spanner_admin_instance_v1_OperationProgress_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_google_spanner_admin_instance_v1_OperationProgress_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_instance_v1_OperationProgress_descriptor,
            new java.lang.String[] {
              "ProgressPercent", "StartTime", "EndTime",
            });
    com.google.protobuf.TimestampProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
