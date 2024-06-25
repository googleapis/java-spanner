// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/admin/database/v1/common.proto

// Protobuf Java Version: 3.25.3
package com.google.spanner.admin.database.v1;

public interface EncryptionInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.EncryptionInfo)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Output only. The type of encryption.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.EncryptionInfo.Type encryption_type = 3 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   * @return The enum numeric value on the wire for encryptionType.
   */
  int getEncryptionTypeValue();
  /**
   * <pre>
   * Output only. The type of encryption.
   * </pre>
   *
   * <code>.google.spanner.admin.database.v1.EncryptionInfo.Type encryption_type = 3 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   * @return The encryptionType.
   */
  com.google.spanner.admin.database.v1.EncryptionInfo.Type getEncryptionType();

  /**
   * <pre>
   * Output only. If present, the status of a recent encrypt/decrypt call on
   * underlying data for this database or backup. Regardless of status, data is
   * always encrypted at rest.
   * </pre>
   *
   * <code>.google.rpc.Status encryption_status = 4 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   * @return Whether the encryptionStatus field is set.
   */
  boolean hasEncryptionStatus();
  /**
   * <pre>
   * Output only. If present, the status of a recent encrypt/decrypt call on
   * underlying data for this database or backup. Regardless of status, data is
   * always encrypted at rest.
   * </pre>
   *
   * <code>.google.rpc.Status encryption_status = 4 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   * @return The encryptionStatus.
   */
  com.google.rpc.Status getEncryptionStatus();
  /**
   * <pre>
   * Output only. If present, the status of a recent encrypt/decrypt call on
   * underlying data for this database or backup. Regardless of status, data is
   * always encrypted at rest.
   * </pre>
   *
   * <code>.google.rpc.Status encryption_status = 4 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   */
  com.google.rpc.StatusOrBuilder getEncryptionStatusOrBuilder();

  /**
   * <pre>
   * Output only. A Cloud KMS key version that is being used to protect the
   * database or backup.
   * </pre>
   *
   * <code>string kms_key_version = 2 [(.google.api.field_behavior) = OUTPUT_ONLY, (.google.api.resource_reference) = { ... }</code>
   * @return The kmsKeyVersion.
   */
  java.lang.String getKmsKeyVersion();
  /**
   * <pre>
   * Output only. A Cloud KMS key version that is being used to protect the
   * database or backup.
   * </pre>
   *
   * <code>string kms_key_version = 2 [(.google.api.field_behavior) = OUTPUT_ONLY, (.google.api.resource_reference) = { ... }</code>
   * @return The bytes for kmsKeyVersion.
   */
  com.google.protobuf.ByteString
      getKmsKeyVersionBytes();
}
