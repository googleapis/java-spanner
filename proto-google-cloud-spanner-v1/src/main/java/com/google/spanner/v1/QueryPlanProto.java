/*
 * Copyright 2025 Google LLC
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
// source: google/spanner/v1/query_plan.proto

// Protobuf Java Version: 3.25.8
package com.google.spanner.v1;

public final class QueryPlanProto {
  private QueryPlanProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_PlanNode_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_PlanNode_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_PlanNode_ChildLink_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_PlanNode_ChildLink_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_PlanNode_ShortRepresentation_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_PlanNode_ShortRepresentation_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_PlanNode_ShortRepresentation_SubqueriesEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_PlanNode_ShortRepresentation_SubqueriesEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_QueryPlan_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_QueryPlan_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n"
          + "\"google/spanner/v1/query_plan.proto\022\021go"
          + "ogle.spanner.v1\032\034google/protobuf/struct.proto\"\370\004\n"
          + "\010PlanNode\022\r\n"
          + "\005index\030\001 \001(\005\022.\n"
          + "\004kind\030\002 \001(\0162 .google.spanner.v1.PlanNode.Kind\022\024\n"
          + "\014display_name\030\003 \001(\t\022:\n"
          + "\013child_links\030\004 \003(\0132%.google.spanner.v1.PlanNode.ChildLink\022M\n"
          + "\024short_representation\030\005 \001(\0132/.goog"
          + "le.spanner.v1.PlanNode.ShortRepresentation\022)\n"
          + "\010metadata\030\006 \001(\0132\027.google.protobuf.Struct\0220\n"
          + "\017execution_stats\030\007 \001(\0132\027.google.protobuf.Struct\032@\n"
          + "\tChildLink\022\023\n"
          + "\013child_index\030\001 \001(\005\022\014\n"
          + "\004type\030\002 \001(\t\022\020\n"
          + "\010variable\030\003 \001(\t\032\262\001\n"
          + "\023ShortRepresentation\022\023\n"
          + "\013description\030\001 \001(\t\022S\n\n"
          + "subqueries\030\002 \003(\0132?.google.span"
          + "ner.v1.PlanNode.ShortRepresentation.SubqueriesEntry\0321\n"
          + "\017SubqueriesEntry\022\013\n"
          + "\003key\030\001 \001(\t\022\r\n"
          + "\005value\030\002 \001(\005:\0028\001\"8\n"
          + "\004Kind\022\024\n"
          + "\020KIND_UNSPECIFIED\020\000\022\016\n\n"
          + "RELATIONAL\020\001\022\n\n"
          + "\006SCALAR\020\002\"<\n"
          + "\tQueryPlan\022/\n\n"
          + "plan_nodes\030\001 \003(\0132\033.google.spanner.v1.PlanNodeB\261\001\n"
          + "\025com.google.spanner.v1B\016QueryPlanProtoP\001Z5cloud.google"
          + ".com/go/spanner/apiv1/spannerpb;spannerp"
          + "b\252\002\027Google.Cloud.Spanner.V1\312\002\027Google\\Clo"
          + "ud\\Spanner\\V1\352\002\032Google::Cloud::Spanner::V1b\006proto3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
              com.google.protobuf.StructProto.getDescriptor(),
            });
    internal_static_google_spanner_v1_PlanNode_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_google_spanner_v1_PlanNode_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_PlanNode_descriptor,
            new java.lang.String[] {
              "Index",
              "Kind",
              "DisplayName",
              "ChildLinks",
              "ShortRepresentation",
              "Metadata",
              "ExecutionStats",
            });
    internal_static_google_spanner_v1_PlanNode_ChildLink_descriptor =
        internal_static_google_spanner_v1_PlanNode_descriptor.getNestedTypes().get(0);
    internal_static_google_spanner_v1_PlanNode_ChildLink_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_PlanNode_ChildLink_descriptor,
            new java.lang.String[] {
              "ChildIndex", "Type", "Variable",
            });
    internal_static_google_spanner_v1_PlanNode_ShortRepresentation_descriptor =
        internal_static_google_spanner_v1_PlanNode_descriptor.getNestedTypes().get(1);
    internal_static_google_spanner_v1_PlanNode_ShortRepresentation_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_PlanNode_ShortRepresentation_descriptor,
            new java.lang.String[] {
              "Description", "Subqueries",
            });
    internal_static_google_spanner_v1_PlanNode_ShortRepresentation_SubqueriesEntry_descriptor =
        internal_static_google_spanner_v1_PlanNode_ShortRepresentation_descriptor
            .getNestedTypes()
            .get(0);
    internal_static_google_spanner_v1_PlanNode_ShortRepresentation_SubqueriesEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_PlanNode_ShortRepresentation_SubqueriesEntry_descriptor,
            new java.lang.String[] {
              "Key", "Value",
            });
    internal_static_google_spanner_v1_QueryPlan_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_google_spanner_v1_QueryPlan_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_QueryPlan_descriptor,
            new java.lang.String[] {
              "PlanNodes",
            });
    com.google.protobuf.StructProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
