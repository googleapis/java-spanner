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

/**
 *
 *
 * <pre>
 * Encryption configuration for the restored database.
 * </pre>
 *
 * Protobuf type {@code google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig}
 */
public final class RestoreDatabaseEncryptionConfig extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig)
    RestoreDatabaseEncryptionConfigOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use RestoreDatabaseEncryptionConfig.newBuilder() to construct.
  private RestoreDatabaseEncryptionConfig(
      com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private RestoreDatabaseEncryptionConfig() {
    encryptionType_ = 0;
    kmsKeyName_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new RestoreDatabaseEncryptionConfig();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private RestoreDatabaseEncryptionConfig(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 8:
            {
              int rawValue = input.readEnum();

              encryptionType_ = rawValue;
              break;
            }
          case 18:
            {
              java.lang.String s = input.readStringRequireUtf8();

              kmsKeyName_ = s;
              break;
            }
          default:
            {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.admin.database.v1.SpannerDatabaseAdminProto
        .internal_static_google_spanner_admin_database_v1_RestoreDatabaseEncryptionConfig_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.admin.database.v1.SpannerDatabaseAdminProto
        .internal_static_google_spanner_admin_database_v1_RestoreDatabaseEncryptionConfig_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.class,
            com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.Builder.class);
  }

  /**
   *
   *
   * <pre>
   * Encryption types for the database to be restored.
   * </pre>
   *
   * Protobuf enum {@code
   * google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType}
   */
  public enum EncryptionType implements com.google.protobuf.ProtocolMessageEnum {
    /**
     *
     *
     * <pre>
     * Unspecified. Do not use.
     * </pre>
     *
     * <code>ENCRYPTION_TYPE_UNSPECIFIED = 0;</code>
     */
    ENCRYPTION_TYPE_UNSPECIFIED(0),
    /**
     *
     *
     * <pre>
     * This is the default option when
     * [encryption_config][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig] is not specified.
     * </pre>
     *
     * <code>USE_CONFIG_DEFAULT_OR_BACKUP_ENCRYPTION = 1;</code>
     */
    USE_CONFIG_DEFAULT_OR_BACKUP_ENCRYPTION(1),
    /**
     *
     *
     * <pre>
     * Use Google default encryption.
     * </pre>
     *
     * <code>GOOGLE_DEFAULT_ENCRYPTION = 2;</code>
     */
    GOOGLE_DEFAULT_ENCRYPTION(2),
    /**
     *
     *
     * <pre>
     * Use customer managed encryption. If specified, `kms_key_name` must
     * must contain a valid Cloud KMS key.
     * </pre>
     *
     * <code>CUSTOMER_MANAGED_ENCRYPTION = 3;</code>
     */
    CUSTOMER_MANAGED_ENCRYPTION(3),
    UNRECOGNIZED(-1),
    ;

    /**
     *
     *
     * <pre>
     * Unspecified. Do not use.
     * </pre>
     *
     * <code>ENCRYPTION_TYPE_UNSPECIFIED = 0;</code>
     */
    public static final int ENCRYPTION_TYPE_UNSPECIFIED_VALUE = 0;
    /**
     *
     *
     * <pre>
     * This is the default option when
     * [encryption_config][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig] is not specified.
     * </pre>
     *
     * <code>USE_CONFIG_DEFAULT_OR_BACKUP_ENCRYPTION = 1;</code>
     */
    public static final int USE_CONFIG_DEFAULT_OR_BACKUP_ENCRYPTION_VALUE = 1;
    /**
     *
     *
     * <pre>
     * Use Google default encryption.
     * </pre>
     *
     * <code>GOOGLE_DEFAULT_ENCRYPTION = 2;</code>
     */
    public static final int GOOGLE_DEFAULT_ENCRYPTION_VALUE = 2;
    /**
     *
     *
     * <pre>
     * Use customer managed encryption. If specified, `kms_key_name` must
     * must contain a valid Cloud KMS key.
     * </pre>
     *
     * <code>CUSTOMER_MANAGED_ENCRYPTION = 3;</code>
     */
    public static final int CUSTOMER_MANAGED_ENCRYPTION_VALUE = 3;

    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static EncryptionType valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static EncryptionType forNumber(int value) {
      switch (value) {
        case 0:
          return ENCRYPTION_TYPE_UNSPECIFIED;
        case 1:
          return USE_CONFIG_DEFAULT_OR_BACKUP_ENCRYPTION;
        case 2:
          return GOOGLE_DEFAULT_ENCRYPTION;
        case 3:
          return CUSTOMER_MANAGED_ENCRYPTION;
        default:
          return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<EncryptionType> internalGetValueMap() {
      return internalValueMap;
    }

    private static final com.google.protobuf.Internal.EnumLiteMap<EncryptionType> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<EncryptionType>() {
          public EncryptionType findValueByNumber(int number) {
            return EncryptionType.forNumber(number);
          }
        };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
      }
      return getDescriptor().getValues().get(ordinal());
    }

    public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
      return getDescriptor();
    }

    public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
      return com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.getDescriptor()
          .getEnumTypes()
          .get(0);
    }

    private static final EncryptionType[] VALUES = values();

    public static EncryptionType valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private EncryptionType(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType)
  }

  public static final int ENCRYPTION_TYPE_FIELD_NUMBER = 1;
  private int encryptionType_;
  /**
   *
   *
   * <pre>
   * Required. The encryption type of the restored database.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return The enum numeric value on the wire for encryptionType.
   */
  @java.lang.Override
  public int getEncryptionTypeValue() {
    return encryptionType_;
  }
  /**
   *
   *
   * <pre>
   * Required. The encryption type of the restored database.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];
   * </code>
   *
   * @return The encryptionType.
   */
  @java.lang.Override
  public com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType
      getEncryptionType() {
    @SuppressWarnings("deprecation")
    com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType result =
        com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType.valueOf(
            encryptionType_);
    return result == null
        ? com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType
            .UNRECOGNIZED
        : result;
  }

  public static final int KMS_KEY_NAME_FIELD_NUMBER = 2;
  private volatile java.lang.Object kmsKeyName_;
  /**
   *
   *
   * <pre>
   * Optional. The Cloud KMS key that will be used to encrypt/decrypt the restored
   * database. This field should be set only when
   * [encryption_type][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.encryption_type] is
   * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
   * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
   * </pre>
   *
   * <code>
   * string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The kmsKeyName.
   */
  @java.lang.Override
  public java.lang.String getKmsKeyName() {
    java.lang.Object ref = kmsKeyName_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      kmsKeyName_ = s;
      return s;
    }
  }
  /**
   *
   *
   * <pre>
   * Optional. The Cloud KMS key that will be used to encrypt/decrypt the restored
   * database. This field should be set only when
   * [encryption_type][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.encryption_type] is
   * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
   * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
   * </pre>
   *
   * <code>
   * string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The bytes for kmsKeyName.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getKmsKeyNameBytes() {
    java.lang.Object ref = kmsKeyName_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      kmsKeyName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;

  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (encryptionType_
        != com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType
            .ENCRYPTION_TYPE_UNSPECIFIED
            .getNumber()) {
      output.writeEnum(1, encryptionType_);
    }
    if (!getKmsKeyNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, kmsKeyName_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (encryptionType_
        != com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType
            .ENCRYPTION_TYPE_UNSPECIFIED
            .getNumber()) {
      size += com.google.protobuf.CodedOutputStream.computeEnumSize(1, encryptionType_);
    }
    if (!getKmsKeyNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, kmsKeyName_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig)) {
      return super.equals(obj);
    }
    com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig other =
        (com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig) obj;

    if (encryptionType_ != other.encryptionType_) return false;
    if (!getKmsKeyName().equals(other.getKmsKeyName())) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + ENCRYPTION_TYPE_FIELD_NUMBER;
    hash = (53 * hash) + encryptionType_;
    hash = (37 * hash) + KMS_KEY_NAME_FIELD_NUMBER;
    hash = (53 * hash) + getKmsKeyName().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
      parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
      parseDelimitedFrom(
          java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() {
    return newBuilder();
  }

  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }

  public static Builder newBuilder(
      com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }

  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   *
   *
   * <pre>
   * Encryption configuration for the restored database.
   * </pre>
   *
   * Protobuf type {@code google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig)
      com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfigOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.admin.database.v1.SpannerDatabaseAdminProto
          .internal_static_google_spanner_admin_database_v1_RestoreDatabaseEncryptionConfig_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.admin.database.v1.SpannerDatabaseAdminProto
          .internal_static_google_spanner_admin_database_v1_RestoreDatabaseEncryptionConfig_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.class,
              com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.Builder.class);
    }

    // Construct using
    // com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {}
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      encryptionType_ = 0;

      kmsKeyName_ = "";

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.admin.database.v1.SpannerDatabaseAdminProto
          .internal_static_google_spanner_admin_database_v1_RestoreDatabaseEncryptionConfig_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
        getDefaultInstanceForType() {
      return com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
          .getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig build() {
      com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig buildPartial() {
      com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig result =
          new com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig(this);
      result.encryptionType_ = encryptionType_;
      result.kmsKeyName_ = kmsKeyName_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }

    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.setField(field, value);
    }

    @java.lang.Override
    public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }

    @java.lang.Override
    public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }

    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field, int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }

    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig) {
        return mergeFrom(
            (com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(
        com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig other) {
      if (other
          == com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
              .getDefaultInstance()) return this;
      if (other.encryptionType_ != 0) {
        setEncryptionTypeValue(other.getEncryptionTypeValue());
      }
      if (!other.getKmsKeyName().isEmpty()) {
        kmsKeyName_ = other.kmsKeyName_;
        onChanged();
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage =
            (com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig)
                e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int encryptionType_ = 0;
    /**
     *
     *
     * <pre>
     * Required. The encryption type of the restored database.
     * </pre>
     *
     * <code>
     * .google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];
     * </code>
     *
     * @return The enum numeric value on the wire for encryptionType.
     */
    @java.lang.Override
    public int getEncryptionTypeValue() {
      return encryptionType_;
    }
    /**
     *
     *
     * <pre>
     * Required. The encryption type of the restored database.
     * </pre>
     *
     * <code>
     * .google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];
     * </code>
     *
     * @param value The enum numeric value on the wire for encryptionType to set.
     * @return This builder for chaining.
     */
    public Builder setEncryptionTypeValue(int value) {

      encryptionType_ = value;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Required. The encryption type of the restored database.
     * </pre>
     *
     * <code>
     * .google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];
     * </code>
     *
     * @return The encryptionType.
     */
    @java.lang.Override
    public com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType
        getEncryptionType() {
      @SuppressWarnings("deprecation")
      com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType result =
          com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType
              .valueOf(encryptionType_);
      return result == null
          ? com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType
              .UNRECOGNIZED
          : result;
    }
    /**
     *
     *
     * <pre>
     * Required. The encryption type of the restored database.
     * </pre>
     *
     * <code>
     * .google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];
     * </code>
     *
     * @param value The encryptionType to set.
     * @return This builder for chaining.
     */
    public Builder setEncryptionType(
        com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType value) {
      if (value == null) {
        throw new NullPointerException();
      }

      encryptionType_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Required. The encryption type of the restored database.
     * </pre>
     *
     * <code>
     * .google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.EncryptionType encryption_type = 1 [(.google.api.field_behavior) = REQUIRED];
     * </code>
     *
     * @return This builder for chaining.
     */
    public Builder clearEncryptionType() {

      encryptionType_ = 0;
      onChanged();
      return this;
    }

    private java.lang.Object kmsKeyName_ = "";
    /**
     *
     *
     * <pre>
     * Optional. The Cloud KMS key that will be used to encrypt/decrypt the restored
     * database. This field should be set only when
     * [encryption_type][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.encryption_type] is
     * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
     * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
     * </pre>
     *
     * <code>
     * string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @return The kmsKeyName.
     */
    public java.lang.String getKmsKeyName() {
      java.lang.Object ref = kmsKeyName_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        kmsKeyName_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * Optional. The Cloud KMS key that will be used to encrypt/decrypt the restored
     * database. This field should be set only when
     * [encryption_type][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.encryption_type] is
     * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
     * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
     * </pre>
     *
     * <code>
     * string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @return The bytes for kmsKeyName.
     */
    public com.google.protobuf.ByteString getKmsKeyNameBytes() {
      java.lang.Object ref = kmsKeyName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        kmsKeyName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * Optional. The Cloud KMS key that will be used to encrypt/decrypt the restored
     * database. This field should be set only when
     * [encryption_type][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.encryption_type] is
     * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
     * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
     * </pre>
     *
     * <code>
     * string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @param value The kmsKeyName to set.
     * @return This builder for chaining.
     */
    public Builder setKmsKeyName(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      kmsKeyName_ = value;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Optional. The Cloud KMS key that will be used to encrypt/decrypt the restored
     * database. This field should be set only when
     * [encryption_type][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.encryption_type] is
     * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
     * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
     * </pre>
     *
     * <code>
     * string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @return This builder for chaining.
     */
    public Builder clearKmsKeyName() {

      kmsKeyName_ = getDefaultInstance().getKmsKeyName();
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Optional. The Cloud KMS key that will be used to encrypt/decrypt the restored
     * database. This field should be set only when
     * [encryption_type][google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig.encryption_type] is
     * `CUSTOMER_MANAGED_ENCRYPTION`. Values are of the form
     * `projects/&lt;project&gt;/locations/&lt;location&gt;/keyRings/&lt;key_ring&gt;/cryptoKeys/&lt;kms_key_name&gt;`.
     * </pre>
     *
     * <code>
     * string kms_key_name = 2 [(.google.api.field_behavior) = OPTIONAL, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @param value The bytes for kmsKeyName to set.
     * @return This builder for chaining.
     */
    public Builder setKmsKeyNameBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      kmsKeyName_ = value;
      onChanged();
      return this;
    }

    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig)
  private static final com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
      DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig();
  }

  public static com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
      getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<RestoreDatabaseEncryptionConfig> PARSER =
      new com.google.protobuf.AbstractParser<RestoreDatabaseEncryptionConfig>() {
        @java.lang.Override
        public RestoreDatabaseEncryptionConfig parsePartialFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new RestoreDatabaseEncryptionConfig(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<RestoreDatabaseEncryptionConfig> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<RestoreDatabaseEncryptionConfig> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.admin.database.v1.RestoreDatabaseEncryptionConfig
      getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
