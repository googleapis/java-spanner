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
// source: google/spanner/admin/database/v1/spanner_database_admin.proto

package com.google.spanner.admin.database.v1;

public final class SpannerDatabaseAdminProto {
  private SpannerDatabaseAdminProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_RestoreInfo_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_RestoreInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_Database_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_Database_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListDatabasesRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListDatabasesRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListDatabasesResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListDatabasesResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CreateDatabaseRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CreateDatabaseRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CreateDatabaseMetadata_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CreateDatabaseMetadata_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_GetDatabaseRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_GetDatabaseRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlMetadata_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlMetadata_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_DropDatabaseRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_DropDatabaseRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_GetDatabaseDdlRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_GetDatabaseDdlRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_GetDatabaseDdlResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_GetDatabaseDdlResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_RestoreDatabaseRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_RestoreDatabaseRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_RestoreDatabaseMetadata_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_RestoreDatabaseMetadata_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_OptimizeRestoredDatabaseMetadata_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_OptimizeRestoredDatabaseMetadata_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n=google/spanner/admin/database/v1/spann"
          + "er_database_admin.proto\022 google.spanner."
          + "admin.database.v1\032\034google/api/annotation"
          + "s.proto\032\027google/api/client.proto\032\037google"
          + "/api/field_behavior.proto\032\031google/api/re"
          + "source.proto\032\036google/iam/v1/iam_policy.p"
          + "roto\032\032google/iam/v1/policy.proto\032#google"
          + "/longrunning/operations.proto\032\033google/pr"
          + "otobuf/empty.proto\032\037google/protobuf/time"
          + "stamp.proto\032-google/spanner/admin/databa"
          + "se/v1/backup.proto\032-google/spanner/admin"
          + "/database/v1/common.proto\"\253\001\n\013RestoreInf"
          + "o\022H\n\013source_type\030\001 \001(\01623.google.spanner."
          + "admin.database.v1.RestoreSourceType\022C\n\013b"
          + "ackup_info\030\002 \001(\0132,.google.spanner.admin."
          + "database.v1.BackupInfoH\000B\r\n\013source_info\""
          + "\226\003\n\010Database\022\021\n\004name\030\001 \001(\tB\003\340A\002\022D\n\005state"
          + "\030\002 \001(\01620.google.spanner.admin.database.v"
          + "1.Database.StateB\003\340A\003\0224\n\013create_time\030\003 \001"
          + "(\0132\032.google.protobuf.TimestampB\003\340A\003\022H\n\014r"
          + "estore_info\030\004 \001(\0132-.google.spanner.admin"
          + ".database.v1.RestoreInfoB\003\340A\003\"M\n\005State\022\025"
          + "\n\021STATE_UNSPECIFIED\020\000\022\014\n\010CREATING\020\001\022\t\n\005R"
          + "EADY\020\002\022\024\n\020READY_OPTIMIZING\020\003:b\352A_\n\037spann"
          + "er.googleapis.com/Database\022<projects/{pr"
          + "oject}/instances/{instance}/databases/{d"
          + "atabase}\"v\n\024ListDatabasesRequest\0227\n\006pare"
          + "nt\030\001 \001(\tB\'\340A\002\372A!\n\037spanner.googleapis.com"
          + "/Instance\022\021\n\tpage_size\030\003 \001(\005\022\022\n\npage_tok"
          + "en\030\004 \001(\t\"o\n\025ListDatabasesResponse\022=\n\tdat"
          + "abases\030\001 \003(\0132*.google.spanner.admin.data"
          + "base.v1.Database\022\027\n\017next_page_token\030\002 \001("
          + "\t\"\216\001\n\025CreateDatabaseRequest\0227\n\006parent\030\001 "
          + "\001(\tB\'\340A\002\372A!\n\037spanner.googleapis.com/Inst"
          + "ance\022\035\n\020create_statement\030\002 \001(\tB\003\340A\002\022\035\n\020e"
          + "xtra_statements\030\003 \003(\tB\003\340A\001\"P\n\026CreateData"
          + "baseMetadata\0226\n\010database\030\001 \001(\tB$\372A!\n\037spa"
          + "nner.googleapis.com/Database\"K\n\022GetDatab"
          + "aseRequest\0225\n\004name\030\001 \001(\tB\'\340A\002\372A!\n\037spanne"
          + "r.googleapis.com/Database\"\204\001\n\030UpdateData"
          + "baseDdlRequest\0229\n\010database\030\001 \001(\tB\'\340A\002\372A!"
          + "\n\037spanner.googleapis.com/Database\022\027\n\nsta"
          + "tements\030\002 \003(\tB\003\340A\002\022\024\n\014operation_id\030\003 \001(\t"
          + "\"\236\001\n\031UpdateDatabaseDdlMetadata\0226\n\010databa"
          + "se\030\001 \001(\tB$\372A!\n\037spanner.googleapis.com/Da"
          + "tabase\022\022\n\nstatements\030\002 \003(\t\0225\n\021commit_tim"
          + "estamps\030\003 \003(\0132\032.google.protobuf.Timestam"
          + "p\"P\n\023DropDatabaseRequest\0229\n\010database\030\001 \001"
          + "(\tB\'\340A\002\372A!\n\037spanner.googleapis.com/Datab"
          + "ase\"R\n\025GetDatabaseDdlRequest\0229\n\010database"
          + "\030\001 \001(\tB\'\340A\002\372A!\n\037spanner.googleapis.com/D"
          + "atabase\",\n\026GetDatabaseDdlResponse\022\022\n\nsta"
          + "tements\030\001 \003(\t\"\217\001\n\035ListDatabaseOperations"
          + "Request\0227\n\006parent\030\001 \001(\tB\'\340A\002\372A!\n\037spanner"
          + ".googleapis.com/Instance\022\016\n\006filter\030\002 \001(\t"
          + "\022\021\n\tpage_size\030\003 \001(\005\022\022\n\npage_token\030\004 \001(\t\""
          + "l\n\036ListDatabaseOperationsResponse\0221\n\nope"
          + "rations\030\001 \003(\0132\035.google.longrunning.Opera"
          + "tion\022\027\n\017next_page_token\030\002 \001(\t\"\253\001\n\026Restor"
          + "eDatabaseRequest\0227\n\006parent\030\001 \001(\tB\'\340A\002\372A!"
          + "\n\037spanner.googleapis.com/Instance\022\030\n\013dat"
          + "abase_id\030\002 \001(\tB\003\340A\002\0224\n\006backup\030\003 \001(\tB\"\372A\037"
          + "\n\035spanner.googleapis.com/BackupH\000B\010\n\006sou"
          + "rce\"\347\002\n\027RestoreDatabaseMetadata\022\014\n\004name\030"
          + "\001 \001(\t\022H\n\013source_type\030\002 \001(\01623.google.span"
          + "ner.admin.database.v1.RestoreSourceType\022"
          + "C\n\013backup_info\030\003 \001(\0132,.google.spanner.ad"
          + "min.database.v1.BackupInfoH\000\022E\n\010progress"
          + "\030\004 \001(\01323.google.spanner.admin.database.v"
          + "1.OperationProgress\022/\n\013cancel_time\030\005 \001(\013"
          + "2\032.google.protobuf.Timestamp\022(\n optimize"
          + "_database_operation_name\030\006 \001(\tB\r\n\013source"
          + "_info\"w\n OptimizeRestoredDatabaseMetadat"
          + "a\022\014\n\004name\030\001 \001(\t\022E\n\010progress\030\002 \001(\01323.goog"
          + "le.spanner.admin.database.v1.OperationPr"
          + "ogress*5\n\021RestoreSourceType\022\024\n\020TYPE_UNSP"
          + "ECIFIED\020\000\022\n\n\006BACKUP\020\0012\223\037\n\rDatabaseAdmin\022"
          + "\300\001\n\rListDatabases\0226.google.spanner.admin"
          + ".database.v1.ListDatabasesRequest\0327.goog"
          + "le.spanner.admin.database.v1.ListDatabas"
          + "esResponse\">\202\323\344\223\002/\022-/v1/{parent=projects"
          + "/*/instances/*}/databases\332A\006parent\022\244\002\n\016C"
          + "reateDatabase\0227.google.spanner.admin.dat"
          + "abase.v1.CreateDatabaseRequest\032\035.google."
          + "longrunning.Operation\"\271\001\202\323\344\223\0022\"-/v1/{par"
          + "ent=projects/*/instances/*}/databases:\001*"
          + "\332A\027parent,create_statement\312Ad\n)google.sp"
          + "anner.admin.database.v1.Database\0227google"
          + ".spanner.admin.database.v1.CreateDatabas"
          + "eMetadata\022\255\001\n\013GetDatabase\0224.google.spann"
          + "er.admin.database.v1.GetDatabaseRequest\032"
          + "*.google.spanner.admin.database.v1.Datab"
          + "ase\"<\202\323\344\223\002/\022-/v1/{name=projects/*/instan"
          + "ces/*/databases/*}\332A\004name\022\235\002\n\021UpdateData"
          + "baseDdl\022:.google.spanner.admin.database."
          + "v1.UpdateDatabaseDdlRequest\032\035.google.lon"
          + "grunning.Operation\"\254\001\202\323\344\223\002:25/v1/{databa"
          + "se=projects/*/instances/*/databases/*}/d"
          + "dl:\001*\332A\023database,statements\312AS\n\025google.p"
          + "rotobuf.Empty\022:google.spanner.admin.data"
          + "base.v1.UpdateDatabaseDdlMetadata\022\243\001\n\014Dr"
          + "opDatabase\0225.google.spanner.admin.databa"
          + "se.v1.DropDatabaseRequest\032\026.google.proto"
          + "buf.Empty\"D\202\323\344\223\0023*1/v1/{database=project"
          + "s/*/instances/*/databases/*}\332A\010database\022"
          + "\315\001\n\016GetDatabaseDdl\0227.google.spanner.admi"
          + "n.database.v1.GetDatabaseDdlRequest\0328.go"
          + "ogle.spanner.admin.database.v1.GetDataba"
          + "seDdlResponse\"H\202\323\344\223\0027\0225/v1/{database=pro"
          + "jects/*/instances/*/databases/*}/ddl\332A\010d"
          + "atabase\022\353\001\n\014SetIamPolicy\022\".google.iam.v1"
          + ".SetIamPolicyRequest\032\025.google.iam.v1.Pol"
          + "icy\"\237\001\202\323\344\223\002\206\001\">/v1/{resource=projects/*/"
          + "instances/*/databases/*}:setIamPolicy:\001*"
          + "ZA\"</v1/{resource=projects/*/instances/*"
          + "/backups/*}:setIamPolicy:\001*\332A\017resource,p"
          + "olicy\022\344\001\n\014GetIamPolicy\022\".google.iam.v1.G"
          + "etIamPolicyRequest\032\025.google.iam.v1.Polic"
          + "y\"\230\001\202\323\344\223\002\206\001\">/v1/{resource=projects/*/in"
          + "stances/*/databases/*}:getIamPolicy:\001*ZA"
          + "\"</v1/{resource=projects/*/instances/*/b"
          + "ackups/*}:getIamPolicy:\001*\332A\010resource\022\234\002\n"
          + "\022TestIamPermissions\022(.google.iam.v1.Test"
          + "IamPermissionsRequest\032).google.iam.v1.Te"
          + "stIamPermissionsResponse\"\260\001\202\323\344\223\002\222\001\"D/v1/"
          + "{resource=projects/*/instances/*/databas"
          + "es/*}:testIamPermissions:\001*ZG\"B/v1/{reso"
          + "urce=projects/*/instances/*/backups/*}:t"
          + "estIamPermissions:\001*\332A\024resource,permissi"
          + "ons\022\237\002\n\014CreateBackup\0225.google.spanner.ad"
          + "min.database.v1.CreateBackupRequest\032\035.go"
          + "ogle.longrunning.Operation\"\270\001\202\323\344\223\0025\"+/v1"
          + "/{parent=projects/*/instances/*}/backups"
          + ":\006backup\332A\027parent,backup,backup_id\312A`\n\'g"
          + "oogle.spanner.admin.database.v1.Backup\0225"
          + "google.spanner.admin.database.v1.CreateB"
          + "ackupMetadata\022\245\001\n\tGetBackup\0222.google.spa"
          + "nner.admin.database.v1.GetBackupRequest\032"
          + "(.google.spanner.admin.database.v1.Backu"
          + "p\":\202\323\344\223\002-\022+/v1/{name=projects/*/instance"
          + "s/*/backups/*}\332A\004name\022\310\001\n\014UpdateBackup\0225"
          + ".google.spanner.admin.database.v1.Update"
          + "BackupRequest\032(.google.spanner.admin.dat"
          + "abase.v1.Backup\"W\202\323\344\223\002<22/v1/{backup.nam"
          + "e=projects/*/instances/*/backups/*}:\006bac"
          + "kup\332A\022backup,update_mask\022\231\001\n\014DeleteBacku"
          + "p\0225.google.spanner.admin.database.v1.Del"
          + "eteBackupRequest\032\026.google.protobuf.Empty"
          + "\":\202\323\344\223\002-*+/v1/{name=projects/*/instances"
          + "/*/backups/*}\332A\004name\022\270\001\n\013ListBackups\0224.g"
          + "oogle.spanner.admin.database.v1.ListBack"
          + "upsRequest\0325.google.spanner.admin.databa"
          + "se.v1.ListBackupsResponse\"<\202\323\344\223\002-\022+/v1/{"
          + "parent=projects/*/instances/*}/backups\332A"
          + "\006parent\022\261\002\n\017RestoreDatabase\0228.google.spa"
          + "nner.admin.database.v1.RestoreDatabaseRe"
          + "quest\032\035.google.longrunning.Operation\"\304\001\202"
          + "\323\344\223\002:\"5/v1/{parent=projects/*/instances/"
          + "*}/databases:restore:\001*\332A\031parent,databas"
          + "e_id,backup\312Ae\n)google.spanner.admin.dat"
          + "abase.v1.Database\0228google.spanner.admin."
          + "database.v1.RestoreDatabaseMetadata\022\344\001\n\026"
          + "ListDatabaseOperations\022?.google.spanner."
          + "admin.database.v1.ListDatabaseOperations"
          + "Request\032@.google.spanner.admin.database."
          + "v1.ListDatabaseOperationsResponse\"G\202\323\344\223\002"
          + "8\0226/v1/{parent=projects/*/instances/*}/d"
          + "atabaseOperations\332A\006parent\022\334\001\n\024ListBacku"
          + "pOperations\022=.google.spanner.admin.datab"
          + "ase.v1.ListBackupOperationsRequest\032>.goo"
          + "gle.spanner.admin.database.v1.ListBackup"
          + "OperationsResponse\"E\202\323\344\223\0026\0224/v1/{parent="
          + "projects/*/instances/*}/backupOperations"
          + "\332A\006parent\032x\312A\026spanner.googleapis.com\322A\\h"
          + "ttps://www.googleapis.com/auth/cloud-pla"
          + "tform,https://www.googleapis.com/auth/sp"
          + "anner.adminB\332\002\n$com.google.spanner.admin"
          + ".database.v1B\031SpannerDatabaseAdminProtoP"
          + "\001ZHgoogle.golang.org/genproto/googleapis"
          + "/spanner/admin/database/v1;database\252\002&Go"
          + "ogle.Cloud.Spanner.Admin.Database.V1\312\002&G"
          + "oogle\\Cloud\\Spanner\\Admin\\Database\\V1\352\002+"
          + "Google::Cloud::Spanner::Admin::Database:"
          + ":V1\352AJ\n\037spanner.googleapis.com/Instance\022"
          + "\'projects/{project}/instances/{instance}"
          + "b\006proto3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
              com.google.api.AnnotationsProto.getDescriptor(),
              com.google.api.ClientProto.getDescriptor(),
              com.google.api.FieldBehaviorProto.getDescriptor(),
              com.google.api.ResourceProto.getDescriptor(),
              com.google.iam.v1.IamPolicyProto.getDescriptor(),
              com.google.iam.v1.PolicyProto.getDescriptor(),
              com.google.longrunning.OperationsProto.getDescriptor(),
              com.google.protobuf.EmptyProto.getDescriptor(),
              com.google.protobuf.TimestampProto.getDescriptor(),
              com.google.spanner.admin.database.v1.BackupProto.getDescriptor(),
              com.google.spanner.admin.database.v1.CommonProto.getDescriptor(),
            });
    internal_static_google_spanner_admin_database_v1_RestoreInfo_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_google_spanner_admin_database_v1_RestoreInfo_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_RestoreInfo_descriptor,
            new java.lang.String[] {
              "SourceType", "BackupInfo", "SourceInfo",
            });
    internal_static_google_spanner_admin_database_v1_Database_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_google_spanner_admin_database_v1_Database_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_Database_descriptor,
            new java.lang.String[] {
              "Name", "State", "CreateTime", "RestoreInfo",
            });
    internal_static_google_spanner_admin_database_v1_ListDatabasesRequest_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_google_spanner_admin_database_v1_ListDatabasesRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListDatabasesRequest_descriptor,
            new java.lang.String[] {
              "Parent", "PageSize", "PageToken",
            });
    internal_static_google_spanner_admin_database_v1_ListDatabasesResponse_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_google_spanner_admin_database_v1_ListDatabasesResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListDatabasesResponse_descriptor,
            new java.lang.String[] {
              "Databases", "NextPageToken",
            });
    internal_static_google_spanner_admin_database_v1_CreateDatabaseRequest_descriptor =
        getDescriptor().getMessageTypes().get(4);
    internal_static_google_spanner_admin_database_v1_CreateDatabaseRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CreateDatabaseRequest_descriptor,
            new java.lang.String[] {
              "Parent", "CreateStatement", "ExtraStatements",
            });
    internal_static_google_spanner_admin_database_v1_CreateDatabaseMetadata_descriptor =
        getDescriptor().getMessageTypes().get(5);
    internal_static_google_spanner_admin_database_v1_CreateDatabaseMetadata_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CreateDatabaseMetadata_descriptor,
            new java.lang.String[] {
              "Database",
            });
    internal_static_google_spanner_admin_database_v1_GetDatabaseRequest_descriptor =
        getDescriptor().getMessageTypes().get(6);
    internal_static_google_spanner_admin_database_v1_GetDatabaseRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_GetDatabaseRequest_descriptor,
            new java.lang.String[] {
              "Name",
            });
    internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlRequest_descriptor =
        getDescriptor().getMessageTypes().get(7);
    internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlRequest_descriptor,
            new java.lang.String[] {
              "Database", "Statements", "OperationId",
            });
    internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlMetadata_descriptor =
        getDescriptor().getMessageTypes().get(8);
    internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlMetadata_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_UpdateDatabaseDdlMetadata_descriptor,
            new java.lang.String[] {
              "Database", "Statements", "CommitTimestamps",
            });
    internal_static_google_spanner_admin_database_v1_DropDatabaseRequest_descriptor =
        getDescriptor().getMessageTypes().get(9);
    internal_static_google_spanner_admin_database_v1_DropDatabaseRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_DropDatabaseRequest_descriptor,
            new java.lang.String[] {
              "Database",
            });
    internal_static_google_spanner_admin_database_v1_GetDatabaseDdlRequest_descriptor =
        getDescriptor().getMessageTypes().get(10);
    internal_static_google_spanner_admin_database_v1_GetDatabaseDdlRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_GetDatabaseDdlRequest_descriptor,
            new java.lang.String[] {
              "Database",
            });
    internal_static_google_spanner_admin_database_v1_GetDatabaseDdlResponse_descriptor =
        getDescriptor().getMessageTypes().get(11);
    internal_static_google_spanner_admin_database_v1_GetDatabaseDdlResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_GetDatabaseDdlResponse_descriptor,
            new java.lang.String[] {
              "Statements",
            });
    internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsRequest_descriptor =
        getDescriptor().getMessageTypes().get(12);
    internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsRequest_descriptor,
            new java.lang.String[] {
              "Parent", "Filter", "PageSize", "PageToken",
            });
    internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsResponse_descriptor =
        getDescriptor().getMessageTypes().get(13);
    internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListDatabaseOperationsResponse_descriptor,
            new java.lang.String[] {
              "Operations", "NextPageToken",
            });
    internal_static_google_spanner_admin_database_v1_RestoreDatabaseRequest_descriptor =
        getDescriptor().getMessageTypes().get(14);
    internal_static_google_spanner_admin_database_v1_RestoreDatabaseRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_RestoreDatabaseRequest_descriptor,
            new java.lang.String[] {
              "Parent", "DatabaseId", "Backup", "Source",
            });
    internal_static_google_spanner_admin_database_v1_RestoreDatabaseMetadata_descriptor =
        getDescriptor().getMessageTypes().get(15);
    internal_static_google_spanner_admin_database_v1_RestoreDatabaseMetadata_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_RestoreDatabaseMetadata_descriptor,
            new java.lang.String[] {
              "Name",
              "SourceType",
              "BackupInfo",
              "Progress",
              "CancelTime",
              "OptimizeDatabaseOperationName",
              "SourceInfo",
            });
    internal_static_google_spanner_admin_database_v1_OptimizeRestoredDatabaseMetadata_descriptor =
        getDescriptor().getMessageTypes().get(16);
    internal_static_google_spanner_admin_database_v1_OptimizeRestoredDatabaseMetadata_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_OptimizeRestoredDatabaseMetadata_descriptor,
            new java.lang.String[] {
              "Name", "Progress",
            });
    com.google.protobuf.ExtensionRegistry registry =
        com.google.protobuf.ExtensionRegistry.newInstance();
    registry.add(com.google.api.ClientProto.defaultHost);
    registry.add(com.google.api.FieldBehaviorProto.fieldBehavior);
    registry.add(com.google.api.AnnotationsProto.http);
    registry.add(com.google.api.ClientProto.methodSignature);
    registry.add(com.google.api.ClientProto.oauthScopes);
    registry.add(com.google.api.ResourceProto.resource);
    registry.add(com.google.api.ResourceProto.resourceDefinition);
    registry.add(com.google.api.ResourceProto.resourceReference);
    registry.add(com.google.longrunning.OperationsProto.operationInfo);
    com.google.protobuf.Descriptors.FileDescriptor.internalUpdateFileDescriptor(
        descriptor, registry);
    com.google.api.AnnotationsProto.getDescriptor();
    com.google.api.ClientProto.getDescriptor();
    com.google.api.FieldBehaviorProto.getDescriptor();
    com.google.api.ResourceProto.getDescriptor();
    com.google.iam.v1.IamPolicyProto.getDescriptor();
    com.google.iam.v1.PolicyProto.getDescriptor();
    com.google.longrunning.OperationsProto.getDescriptor();
    com.google.protobuf.EmptyProto.getDescriptor();
    com.google.protobuf.TimestampProto.getDescriptor();
    com.google.spanner.admin.database.v1.BackupProto.getDescriptor();
    com.google.spanner.admin.database.v1.CommonProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
