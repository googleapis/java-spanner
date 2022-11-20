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
// source: google/spanner/admin/database/v1/backup.proto

package com.google.spanner.admin.database.v1;

public final class BackupProto {
  private BackupProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_Backup_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_Backup_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CreateBackupRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CreateBackupRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CreateBackupMetadata_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CreateBackupMetadata_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CopyBackupRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CopyBackupRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CopyBackupMetadata_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CopyBackupMetadata_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_UpdateBackupRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_UpdateBackupRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_GetBackupRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_GetBackupRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_DeleteBackupRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_DeleteBackupRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListBackupsRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListBackupsRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListBackupsResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListBackupsResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListBackupOperationsRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListBackupOperationsRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_ListBackupOperationsResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_ListBackupOperationsResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_BackupInfo_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_BackupInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CreateBackupEncryptionConfig_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CreateBackupEncryptionConfig_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_google_spanner_admin_database_v1_CopyBackupEncryptionConfig_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_spanner_admin_database_v1_CopyBackupEncryptionConfig_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n-google/spanner/admin/database/v1/backu"
          + "p.proto\022 google.spanner.admin.database.v"
          + "1\032\037google/api/field_behavior.proto\032\031goog"
          + "le/api/resource.proto\032#google/longrunnin"
          + "g/operations.proto\032 google/protobuf/fiel"
          + "d_mask.proto\032\037google/protobuf/timestamp."
          + "proto\032-google/spanner/admin/database/v1/"
          + "common.proto\"\232\007\n\006Backup\0226\n\010database\030\002 \001("
          + "\tB$\372A!\n\037spanner.googleapis.com/Database\022"
          + "0\n\014version_time\030\t \001(\0132\032.google.protobuf."
          + "Timestamp\022/\n\013expire_time\030\003 \001(\0132\032.google."
          + "protobuf.Timestamp\022\014\n\004name\030\001 \001(\t\0224\n\013crea"
          + "te_time\030\004 \001(\0132\032.google.protobuf.Timestam"
          + "pB\003\340A\003\022\027\n\nsize_bytes\030\005 \001(\003B\003\340A\003\022B\n\005state"
          + "\030\006 \001(\0162..google.spanner.admin.database.v"
          + "1.Backup.StateB\003\340A\003\022F\n\025referencing_datab"
          + "ases\030\007 \003(\tB\'\340A\003\372A!\n\037spanner.googleapis.c"
          + "om/Database\022N\n\017encryption_info\030\010 \001(\01320.g"
          + "oogle.spanner.admin.database.v1.Encrypti"
          + "onInfoB\003\340A\003\022U\n\026encryption_information\030\r "
          + "\003(\01320.google.spanner.admin.database.v1.E"
          + "ncryptionInfoB\003\340A\003\022P\n\020database_dialect\030\n"
          + " \001(\01621.google.spanner.admin.database.v1."
          + "DatabaseDialectB\003\340A\003\022B\n\023referencing_back"
          + "ups\030\013 \003(\tB%\340A\003\372A\037\n\035spanner.googleapis.co"
          + "m/Backup\0228\n\017max_expire_time\030\014 \001(\0132\032.goog"
          + "le.protobuf.TimestampB\003\340A\003\"7\n\005State\022\025\n\021S"
          + "TATE_UNSPECIFIED\020\000\022\014\n\010CREATING\020\001\022\t\n\005READ"
          + "Y\020\002:\\\352AY\n\035spanner.googleapis.com/Backup\022"
          + "8projects/{project}/instances/{instance}"
          + "/backups/{backup}\"\205\002\n\023CreateBackupReques"
          + "t\0227\n\006parent\030\001 \001(\tB\'\340A\002\372A!\n\037spanner.googl"
          + "eapis.com/Instance\022\026\n\tbackup_id\030\002 \001(\tB\003\340"
          + "A\002\022=\n\006backup\030\003 \001(\0132(.google.spanner.admi"
          + "n.database.v1.BackupB\003\340A\002\022^\n\021encryption_"
          + "config\030\004 \001(\0132>.google.spanner.admin.data"
          + "base.v1.CreateBackupEncryptionConfigB\003\340A"
          + "\001\"\370\001\n\024CreateBackupMetadata\0220\n\004name\030\001 \001(\t"
          + "B\"\372A\037\n\035spanner.googleapis.com/Backup\0226\n\010"
          + "database\030\002 \001(\tB$\372A!\n\037spanner.googleapis."
          + "com/Database\022E\n\010progress\030\003 \001(\01323.google."
          + "spanner.admin.database.v1.OperationProgr"
          + "ess\022/\n\013cancel_time\030\004 \001(\0132\032.google.protob"
          + "uf.Timestamp\"\266\002\n\021CopyBackupRequest\0227\n\006pa"
          + "rent\030\001 \001(\tB\'\340A\002\372A!\n\037spanner.googleapis.c"
          + "om/Instance\022\026\n\tbackup_id\030\002 \001(\tB\003\340A\002\022<\n\rs"
          + "ource_backup\030\003 \001(\tB%\340A\002\372A\037\n\035spanner.goog"
          + "leapis.com/Backup\0224\n\013expire_time\030\004 \001(\0132\032"
          + ".google.protobuf.TimestampB\003\340A\002\022\\\n\021encry"
          + "ption_config\030\005 \001(\0132<.google.spanner.admi"
          + "n.database.v1.CopyBackupEncryptionConfig"
          + "B\003\340A\001\"\371\001\n\022CopyBackupMetadata\0220\n\004name\030\001 \001"
          + "(\tB\"\372A\037\n\035spanner.googleapis.com/Backup\0229"
          + "\n\rsource_backup\030\002 \001(\tB\"\372A\037\n\035spanner.goog"
          + "leapis.com/Backup\022E\n\010progress\030\003 \001(\01323.go"
          + "ogle.spanner.admin.database.v1.Operation"
          + "Progress\022/\n\013cancel_time\030\004 \001(\0132\032.google.p"
          + "rotobuf.Timestamp\"\212\001\n\023UpdateBackupReques"
          + "t\022=\n\006backup\030\001 \001(\0132(.google.spanner.admin"
          + ".database.v1.BackupB\003\340A\002\0224\n\013update_mask\030"
          + "\002 \001(\0132\032.google.protobuf.FieldMaskB\003\340A\002\"G"
          + "\n\020GetBackupRequest\0223\n\004name\030\001 \001(\tB%\340A\002\372A\037"
          + "\n\035spanner.googleapis.com/Backup\"J\n\023Delet"
          + "eBackupRequest\0223\n\004name\030\001 \001(\tB%\340A\002\372A\037\n\035sp"
          + "anner.googleapis.com/Backup\"\204\001\n\022ListBack"
          + "upsRequest\0227\n\006parent\030\001 \001(\tB\'\340A\002\372A!\n\037span"
          + "ner.googleapis.com/Instance\022\016\n\006filter\030\002 "
          + "\001(\t\022\021\n\tpage_size\030\003 \001(\005\022\022\n\npage_token\030\004 \001"
          + "(\t\"i\n\023ListBackupsResponse\0229\n\007backups\030\001 \003"
          + "(\0132(.google.spanner.admin.database.v1.Ba"
          + "ckup\022\027\n\017next_page_token\030\002 \001(\t\"\215\001\n\033ListBa"
          + "ckupOperationsRequest\0227\n\006parent\030\001 \001(\tB\'\340"
          + "A\002\372A!\n\037spanner.googleapis.com/Instance\022\016"
          + "\n\006filter\030\002 \001(\t\022\021\n\tpage_size\030\003 \001(\005\022\022\n\npag"
          + "e_token\030\004 \001(\t\"j\n\034ListBackupOperationsRes"
          + "ponse\0221\n\noperations\030\001 \003(\0132\035.google.longr"
          + "unning.Operation\022\027\n\017next_page_token\030\002 \001("
          + "\t\"\342\001\n\nBackupInfo\0222\n\006backup\030\001 \001(\tB\"\372A\037\n\035s"
          + "panner.googleapis.com/Backup\0220\n\014version_"
          + "time\030\004 \001(\0132\032.google.protobuf.Timestamp\022/"
          + "\n\013create_time\030\002 \001(\0132\032.google.protobuf.Ti"
          + "mestamp\022=\n\017source_database\030\003 \001(\tB$\372A!\n\037s"
          + "panner.googleapis.com/Database\"\237\003\n\034Creat"
          + "eBackupEncryptionConfig\022k\n\017encryption_ty"
          + "pe\030\001 \001(\0162M.google.spanner.admin.database"
          + ".v1.CreateBackupEncryptionConfig.Encrypt"
          + "ionTypeB\003\340A\002\022?\n\014kms_key_name\030\002 \001(\tB)\340A\001\372"
          + "A#\n!cloudkms.googleapis.com/CryptoKey\022@\n"
          + "\rkms_key_names\030\003 \003(\tB)\340A\001\372A#\n!cloudkms.g"
          + "oogleapis.com/CryptoKey\"\216\001\n\016EncryptionTy"
          + "pe\022\037\n\033ENCRYPTION_TYPE_UNSPECIFIED\020\000\022\033\n\027U"
          + "SE_DATABASE_ENCRYPTION\020\001\022\035\n\031GOOGLE_DEFAU"
          + "LT_ENCRYPTION\020\002\022\037\n\033CUSTOMER_MANAGED_ENCR"
          + "YPTION\020\003\"\253\003\n\032CopyBackupEncryptionConfig\022"
          + "i\n\017encryption_type\030\001 \001(\0162K.google.spanne"
          + "r.admin.database.v1.CopyBackupEncryption"
          + "Config.EncryptionTypeB\003\340A\002\022?\n\014kms_key_na"
          + "me\030\002 \001(\tB)\340A\001\372A#\n!cloudkms.googleapis.co"
          + "m/CryptoKey\022@\n\rkms_key_names\030\003 \003(\tB)\340A\001\372"
          + "A#\n!cloudkms.googleapis.com/CryptoKey\"\236\001"
          + "\n\016EncryptionType\022\037\n\033ENCRYPTION_TYPE_UNSP"
          + "ECIFIED\020\000\022+\n\'USE_CONFIG_DEFAULT_OR_BACKU"
          + "P_ENCRYPTION\020\001\022\035\n\031GOOGLE_DEFAULT_ENCRYPT"
          + "ION\020\002\022\037\n\033CUSTOMER_MANAGED_ENCRYPTION\020\003B\377"
          + "\001\n$com.google.spanner.admin.database.v1B"
          + "\013BackupProtoP\001ZHgoogle.golang.org/genpro"
          + "to/googleapis/spanner/admin/database/v1;"
          + "database\252\002&Google.Cloud.Spanner.Admin.Da"
          + "tabase.V1\312\002&Google\\Cloud\\Spanner\\Admin\\D"
          + "atabase\\V1\352\002+Google::Cloud::Spanner::Adm"
          + "in::Database::V1b\006proto3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
              com.google.api.FieldBehaviorProto.getDescriptor(),
              com.google.api.ResourceProto.getDescriptor(),
              com.google.longrunning.OperationsProto.getDescriptor(),
              com.google.protobuf.FieldMaskProto.getDescriptor(),
              com.google.protobuf.TimestampProto.getDescriptor(),
              com.google.spanner.admin.database.v1.CommonProto.getDescriptor(),
            });
    internal_static_google_spanner_admin_database_v1_Backup_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_google_spanner_admin_database_v1_Backup_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_Backup_descriptor,
            new java.lang.String[] {
              "Database",
              "VersionTime",
              "ExpireTime",
              "Name",
              "CreateTime",
              "SizeBytes",
              "State",
              "ReferencingDatabases",
              "EncryptionInfo",
              "EncryptionInformation",
              "DatabaseDialect",
              "ReferencingBackups",
              "MaxExpireTime",
            });
    internal_static_google_spanner_admin_database_v1_CreateBackupRequest_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_google_spanner_admin_database_v1_CreateBackupRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CreateBackupRequest_descriptor,
            new java.lang.String[] {
              "Parent", "BackupId", "Backup", "EncryptionConfig",
            });
    internal_static_google_spanner_admin_database_v1_CreateBackupMetadata_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_google_spanner_admin_database_v1_CreateBackupMetadata_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CreateBackupMetadata_descriptor,
            new java.lang.String[] {
              "Name", "Database", "Progress", "CancelTime",
            });
    internal_static_google_spanner_admin_database_v1_CopyBackupRequest_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_google_spanner_admin_database_v1_CopyBackupRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CopyBackupRequest_descriptor,
            new java.lang.String[] {
              "Parent", "BackupId", "SourceBackup", "ExpireTime", "EncryptionConfig",
            });
    internal_static_google_spanner_admin_database_v1_CopyBackupMetadata_descriptor =
        getDescriptor().getMessageTypes().get(4);
    internal_static_google_spanner_admin_database_v1_CopyBackupMetadata_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CopyBackupMetadata_descriptor,
            new java.lang.String[] {
              "Name", "SourceBackup", "Progress", "CancelTime",
            });
    internal_static_google_spanner_admin_database_v1_UpdateBackupRequest_descriptor =
        getDescriptor().getMessageTypes().get(5);
    internal_static_google_spanner_admin_database_v1_UpdateBackupRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_UpdateBackupRequest_descriptor,
            new java.lang.String[] {
              "Backup", "UpdateMask",
            });
    internal_static_google_spanner_admin_database_v1_GetBackupRequest_descriptor =
        getDescriptor().getMessageTypes().get(6);
    internal_static_google_spanner_admin_database_v1_GetBackupRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_GetBackupRequest_descriptor,
            new java.lang.String[] {
              "Name",
            });
    internal_static_google_spanner_admin_database_v1_DeleteBackupRequest_descriptor =
        getDescriptor().getMessageTypes().get(7);
    internal_static_google_spanner_admin_database_v1_DeleteBackupRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_DeleteBackupRequest_descriptor,
            new java.lang.String[] {
              "Name",
            });
    internal_static_google_spanner_admin_database_v1_ListBackupsRequest_descriptor =
        getDescriptor().getMessageTypes().get(8);
    internal_static_google_spanner_admin_database_v1_ListBackupsRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListBackupsRequest_descriptor,
            new java.lang.String[] {
              "Parent", "Filter", "PageSize", "PageToken",
            });
    internal_static_google_spanner_admin_database_v1_ListBackupsResponse_descriptor =
        getDescriptor().getMessageTypes().get(9);
    internal_static_google_spanner_admin_database_v1_ListBackupsResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListBackupsResponse_descriptor,
            new java.lang.String[] {
              "Backups", "NextPageToken",
            });
    internal_static_google_spanner_admin_database_v1_ListBackupOperationsRequest_descriptor =
        getDescriptor().getMessageTypes().get(10);
    internal_static_google_spanner_admin_database_v1_ListBackupOperationsRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListBackupOperationsRequest_descriptor,
            new java.lang.String[] {
              "Parent", "Filter", "PageSize", "PageToken",
            });
    internal_static_google_spanner_admin_database_v1_ListBackupOperationsResponse_descriptor =
        getDescriptor().getMessageTypes().get(11);
    internal_static_google_spanner_admin_database_v1_ListBackupOperationsResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_ListBackupOperationsResponse_descriptor,
            new java.lang.String[] {
              "Operations", "NextPageToken",
            });
    internal_static_google_spanner_admin_database_v1_BackupInfo_descriptor =
        getDescriptor().getMessageTypes().get(12);
    internal_static_google_spanner_admin_database_v1_BackupInfo_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_BackupInfo_descriptor,
            new java.lang.String[] {
              "Backup", "VersionTime", "CreateTime", "SourceDatabase",
            });
    internal_static_google_spanner_admin_database_v1_CreateBackupEncryptionConfig_descriptor =
        getDescriptor().getMessageTypes().get(13);
    internal_static_google_spanner_admin_database_v1_CreateBackupEncryptionConfig_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CreateBackupEncryptionConfig_descriptor,
            new java.lang.String[] {
              "EncryptionType", "KmsKeyName", "KmsKeyNames",
            });
    internal_static_google_spanner_admin_database_v1_CopyBackupEncryptionConfig_descriptor =
        getDescriptor().getMessageTypes().get(14);
    internal_static_google_spanner_admin_database_v1_CopyBackupEncryptionConfig_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_google_spanner_admin_database_v1_CopyBackupEncryptionConfig_descriptor,
            new java.lang.String[] {
              "EncryptionType", "KmsKeyName", "KmsKeyNames",
            });
    com.google.protobuf.ExtensionRegistry registry =
        com.google.protobuf.ExtensionRegistry.newInstance();
    registry.add(com.google.api.FieldBehaviorProto.fieldBehavior);
    registry.add(com.google.api.ResourceProto.resource);
    registry.add(com.google.api.ResourceProto.resourceReference);
    com.google.protobuf.Descriptors.FileDescriptor.internalUpdateFileDescriptor(
        descriptor, registry);
    com.google.api.FieldBehaviorProto.getDescriptor();
    com.google.api.ResourceProto.getDescriptor();
    com.google.longrunning.OperationsProto.getDescriptor();
    com.google.protobuf.FieldMaskProto.getDescriptor();
    com.google.protobuf.TimestampProto.getDescriptor();
    com.google.spanner.admin.database.v1.CommonProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
