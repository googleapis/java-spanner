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
// source: google/spanner/v1/result_set.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.v1;

public final class ResultSetProto {
  private ResultSetProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_ResultSet_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_ResultSet_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_PartialResultSet_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_PartialResultSet_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_ResultSetMetadata_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_ResultSetMetadata_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_v1_ResultSetStats_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_v1_ResultSetStats_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n\"google/spanner/v1/result_set.proto\022\021go"
          + "ogle.spanner.v1\032\037google/api/field_behavi"
          + "or.proto\032\034google/protobuf/struct.proto\032\""
          + "google/spanner/v1/query_plan.proto\032#goog"
          + "le/spanner/v1/transaction.proto\032\034google/"
          + "spanner/v1/type.proto\"\362\001\n\tResultSet\0226\n\010m"
          + "etadata\030\001 \001(\0132$.google.spanner.v1.Result"
          + "SetMetadata\022(\n\004rows\030\002 \003(\0132\032.google.proto"
          + "buf.ListValue\0220\n\005stats\030\003 \001(\0132!.google.sp"
          + "anner.v1.ResultSetStats\022Q\n\017precommit_tok"
          + "en\030\005 \001(\01323.google.spanner.v1.Multiplexed"
          + "SessionPrecommitTokenB\003\340A\001\"\267\002\n\020PartialRe"
          + "sultSet\0226\n\010metadata\030\001 \001(\0132$.google.spann"
          + "er.v1.ResultSetMetadata\022&\n\006values\030\002 \003(\0132"
          + "\026.google.protobuf.Value\022\025\n\rchunked_value"
          + "\030\003 \001(\010\022\024\n\014resume_token\030\004 \001(\014\0220\n\005stats\030\005 "
          + "\001(\0132!.google.spanner.v1.ResultSetStats\022Q"
          + "\n\017precommit_token\030\010 \001(\01323.google.spanner"
          + ".v1.MultiplexedSessionPrecommitTokenB\003\340A"
          + "\001\022\021\n\004last\030\t \001(\010B\003\340A\001\"\267\001\n\021ResultSetMetada"
          + "ta\022/\n\010row_type\030\001 \001(\0132\035.google.spanner.v1"
          + ".StructType\0223\n\013transaction\030\002 \001(\0132\036.googl"
          + "e.spanner.v1.Transaction\022<\n\025undeclared_p"
          + "arameters\030\003 \001(\0132\035.google.spanner.v1.Stru"
          + "ctType\"\271\001\n\016ResultSetStats\0220\n\nquery_plan\030"
          + "\001 \001(\0132\034.google.spanner.v1.QueryPlan\022,\n\013q"
          + "uery_stats\030\002 \001(\0132\027.google.protobuf.Struc"
          + "t\022\031\n\017row_count_exact\030\003 \001(\003H\000\022\037\n\025row_coun"
          + "t_lower_bound\030\004 \001(\003H\000B\013\n\trow_countB\261\001\n\025c"
          + "om.google.spanner.v1B\016ResultSetProtoP\001Z5"
          + "cloud.google.com/go/spanner/apiv1/spanne"
          + "rpb;spannerpb\252\002\027Google.Cloud.Spanner.V1\312"
          + "\002\027Google\\Cloud\\Spanner\\V1\352\002\032Google::Clou"
          + "d::Spanner::V1b\006proto3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
              com.google.api.FieldBehaviorProto.getDescriptor(),
              com.google.protobuf.StructProto.getDescriptor(),
              com.google.spanner.v1.QueryPlanProto.getDescriptor(),
              com.google.spanner.v1.TransactionProto.getDescriptor(),
              com.google.spanner.v1.TypeProto.getDescriptor(),
            });
    internal_static_google_spanner_v1_ResultSet_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_google_spanner_v1_ResultSet_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_ResultSet_descriptor,
            new java.lang.String[] {
              "Metadata", "Rows", "Stats", "PrecommitToken",
            });
    internal_static_google_spanner_v1_PartialResultSet_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_google_spanner_v1_PartialResultSet_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_PartialResultSet_descriptor,
            new java.lang.String[] {
              "Metadata",
              "Values",
              "ChunkedValue",
              "ResumeToken",
              "Stats",
              "PrecommitToken",
              "Last",
            });
    internal_static_google_spanner_v1_ResultSetMetadata_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_google_spanner_v1_ResultSetMetadata_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_ResultSetMetadata_descriptor,
            new java.lang.String[] {
              "RowType", "Transaction", "UndeclaredParameters",
            });
    internal_static_google_spanner_v1_ResultSetStats_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_google_spanner_v1_ResultSetStats_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_v1_ResultSetStats_descriptor,
            new java.lang.String[] {
              "QueryPlan", "QueryStats", "RowCountExact", "RowCountLowerBound", "RowCount",
            });
    com.google.protobuf.ExtensionRegistry registry =
        com.google.protobuf.ExtensionRegistry.newInstance();
    registry.add(com.google.api.FieldBehaviorProto.fieldBehavior);
    com.google.protobuf.Descriptors.FileDescriptor.internalUpdateFileDescriptor(
        descriptor, registry);
    com.google.api.FieldBehaviorProto.getDescriptor();
    com.google.protobuf.StructProto.getDescriptor();
    com.google.spanner.v1.QueryPlanProto.getDescriptor();
    com.google.spanner.v1.TransactionProto.getDescriptor();
    com.google.spanner.v1.TypeProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
