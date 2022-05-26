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
// source: google/spanner/v1/commit_response.proto

package com.google.spanner.v1;

public final class CommitResponseProto {
  private CommitResponseProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_CommitResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_CommitResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_CommitResponse_CommitStats_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_CommitResponse_CommitStats_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n\'google/spanner/v1/commit_response.prot"
          + "o\022\021google.spanner.v1\032\037google/protobuf/ti"
          + "mestamp.proto\"\262\001\n\016CommitResponse\0224\n\020comm"
          + "it_timestamp\030\001 \001(\0132\032.google.protobuf.Tim"
          + "estamp\022C\n\014commit_stats\030\002 \001(\0132-.google.sp"
          + "anner.v1.CommitResponse.CommitStats\032%\n\013C"
          + "ommitStats\022\026\n\016mutation_count\030\001 \001(\003B\271\001\n\025c"
          + "om.google.spanner.v1B\023CommitResponseProt"
          + "oP\001Z8google.golang.org/genproto/googleap"
          + "is/spanner/v1;spanner\252\002\027Google.Cloud.Spa"
          + "nner.V1\312\002\027Google\\Cloud\\Spanner\\V1\352\002\032Goog"
          + "le::Cloud::Spanner::V1b\006proto3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
              com.google.protobuf.TimestampProto.getDescriptor(),
            });
    internal_static_google_spanner_v1_CommitResponse_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_google_spanner_v1_CommitResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_CommitResponse_descriptor,
            new java.lang.String[] {
              "CommitTimestamp", "CommitStats",
            });
    internal_static_google_spanner_v1_CommitResponse_CommitStats_descriptor =
        internal_static_google_spanner_v1_CommitResponse_descriptor.getNestedTypes().get(0);
    internal_static_google_spanner_v1_CommitResponse_CommitStats_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_CommitResponse_CommitStats_descriptor,
            new java.lang.String[] {
              "MutationCount",
            });
    com.google.protobuf.TimestampProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
