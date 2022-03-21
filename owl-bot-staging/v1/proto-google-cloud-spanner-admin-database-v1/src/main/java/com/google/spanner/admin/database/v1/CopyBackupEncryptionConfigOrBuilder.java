// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/admin/database/v1/backup.proto

package com.google.spanner.admin.database.v1;

public interface CopyBackupEncryptionConfigOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.CopyBackupEncryptionConfig)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Required. The encryption type of the backup.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.CopyBackupEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];</code>
   * @return The enum numeric value on the wire for encryptionType.
   */
  int getEncryptionTypeValue();
  /**
   * <pre>
   * Required. The encryption type of the backup.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.CopyBackupEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];</code>
   * @return The encryptionType.
   */
  com.google.spanner.admin.database.v1.CopyBackupEncryptionConfig.EncryptionType getEncryptionType();

  /**
   * <pre>
   * Optional. The Cloud KMS key that will be used to protect the backup.
   * This field should be set only when
   * [encryption_type][google.spanner.admin.database.v1.CopyBackupEncryptionConfig.encryption_type] is
   * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
   * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
   * </pre>
   *
   * <code>string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }</code>
   * @return The kmsKeyName.
   */
  java.lang.String getKmsKeyName();
  /**
   * <pre>
   * Optional. The Cloud KMS key that will be used to protect the backup.
   * This field should be set only when
   * [encryption_type][google.spanner.admin.database.v1.CopyBackupEncryptionConfig.encryption_type] is
   * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
   * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
   * </pre>
   *
   * <code>string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }</code>
   * @return The bytes for kmsKeyName.
   */
  com.google.protobuf.ByteString
      getKmsKeyNameBytes();
}
