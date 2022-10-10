/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/executor.proto

package com.google.spanner.executor.v1;

/**
 * <pre>
 * Action that copies a Cloud Spanner backup.
 * </pre>
 *
 * Protobuf type {@code google.spanner.executor.v1.CopyCloudBackupAction}
 */
public final class CopyCloudBackupAction extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:google.spanner.executor.v1.CopyCloudBackupAction)
    CopyCloudBackupActionOrBuilder {
private static final long serialVersionUID = 0L;
  // Use CopyCloudBackupAction.newBuilder() to construct.
  private CopyCloudBackupAction(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private CopyCloudBackupAction() {
    projectId_ = "";
    instanceId_ = "";
    backupId_ = "";
    sourceBackup_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new CopyCloudBackupAction();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private CopyCloudBackupAction(
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
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            projectId_ = s;
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();

            instanceId_ = s;
            break;
          }
          case 26: {
            java.lang.String s = input.readStringRequireUtf8();

            backupId_ = s;
            break;
          }
          case 34: {
            java.lang.String s = input.readStringRequireUtf8();

            sourceBackup_ = s;
            break;
          }
          case 40: {

            expireTime_ = input.readInt64();
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (com.google.protobuf.UninitializedMessageException e) {
      throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.google.spanner.executor.v1.ExecutorProto.internal_static_google_spanner_executor_v1_CopyCloudBackupAction_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.executor.v1.ExecutorProto.internal_static_google_spanner_executor_v1_CopyCloudBackupAction_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.executor.v1.CopyCloudBackupAction.class, com.google.spanner.executor.v1.CopyCloudBackupAction.Builder.class);
  }

  public static final int PROJECT_ID_FIELD_NUMBER = 1;
  private volatile java.lang.Object projectId_;
  /**
   * <pre>
   * Required. Cloud project ID, e.g. "spanner-cloud-systest".
   * </pre>
   *
   * <code>string project_id = 1;</code>
   * @return The projectId.
   */
  @java.lang.Override
  public java.lang.String getProjectId() {
    java.lang.Object ref = projectId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      projectId_ = s;
      return s;
    }
  }
  /**
   * <pre>
   * Required. Cloud project ID, e.g. "spanner-cloud-systest".
   * </pre>
   *
   * <code>string project_id = 1;</code>
   * @return The bytes for projectId.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getProjectIdBytes() {
    java.lang.Object ref = projectId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      projectId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int INSTANCE_ID_FIELD_NUMBER = 2;
  private volatile java.lang.Object instanceId_;
  /**
   * <pre>
   * Required. Cloud instance ID (not path), e.g. "test-instance".
   * </pre>
   *
   * <code>string instance_id = 2;</code>
   * @return The instanceId.
   */
  @java.lang.Override
  public java.lang.String getInstanceId() {
    java.lang.Object ref = instanceId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      instanceId_ = s;
      return s;
    }
  }
  /**
   * <pre>
   * Required. Cloud instance ID (not path), e.g. "test-instance".
   * </pre>
   *
   * <code>string instance_id = 2;</code>
   * @return The bytes for instanceId.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getInstanceIdBytes() {
    java.lang.Object ref = instanceId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      instanceId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int BACKUP_ID_FIELD_NUMBER = 3;
  private volatile java.lang.Object backupId_;
  /**
   * <pre>
   * Required. The id of the backup to be created, e.g. "test-backup".
   * </pre>
   *
   * <code>string backup_id = 3;</code>
   * @return The backupId.
   */
  @java.lang.Override
  public java.lang.String getBackupId() {
    java.lang.Object ref = backupId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      backupId_ = s;
      return s;
    }
  }
  /**
   * <pre>
   * Required. The id of the backup to be created, e.g. "test-backup".
   * </pre>
   *
   * <code>string backup_id = 3;</code>
   * @return The bytes for backupId.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getBackupIdBytes() {
    java.lang.Object ref = backupId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      backupId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int SOURCE_BACKUP_FIELD_NUMBER = 4;
  private volatile java.lang.Object sourceBackup_;
  /**
   * <pre>
   * Required. The fully qualified uri of the source backup from which this
   * backup was copied. eg.
   * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
   * </pre>
   *
   * <code>string source_backup = 4;</code>
   * @return The sourceBackup.
   */
  @java.lang.Override
  public java.lang.String getSourceBackup() {
    java.lang.Object ref = sourceBackup_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      sourceBackup_ = s;
      return s;
    }
  }
  /**
   * <pre>
   * Required. The fully qualified uri of the source backup from which this
   * backup was copied. eg.
   * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
   * </pre>
   *
   * <code>string source_backup = 4;</code>
   * @return The bytes for sourceBackup.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getSourceBackupBytes() {
    java.lang.Object ref = sourceBackup_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      sourceBackup_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int EXPIRE_TIME_FIELD_NUMBER = 5;
  private long expireTime_;
  /**
   * <pre>
   * Required. The expiration time of the backup, with microseconds granularity
   * that must be at least 6 hours and at most 366 days
   * from the time the request is received.
   * </pre>
   *
   * <code>int64 expire_time = 5;</code>
   * @return The expireTime.
   */
  @java.lang.Override
  public long getExpireTime() {
    return expireTime_;
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
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(projectId_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, projectId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(instanceId_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, instanceId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(backupId_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 3, backupId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(sourceBackup_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, sourceBackup_);
    }
    if (expireTime_ != 0L) {
      output.writeInt64(5, expireTime_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(projectId_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, projectId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(instanceId_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, instanceId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(backupId_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, backupId_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(sourceBackup_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, sourceBackup_);
    }
    if (expireTime_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt64Size(5, expireTime_);
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
    if (!(obj instanceof com.google.spanner.executor.v1.CopyCloudBackupAction)) {
      return super.equals(obj);
    }
    com.google.spanner.executor.v1.CopyCloudBackupAction other = (com.google.spanner.executor.v1.CopyCloudBackupAction) obj;

    if (!getProjectId()
        .equals(other.getProjectId())) return false;
    if (!getInstanceId()
        .equals(other.getInstanceId())) return false;
    if (!getBackupId()
        .equals(other.getBackupId())) return false;
    if (!getSourceBackup()
        .equals(other.getSourceBackup())) return false;
    if (getExpireTime()
        != other.getExpireTime()) return false;
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
    hash = (37 * hash) + PROJECT_ID_FIELD_NUMBER;
    hash = (53 * hash) + getProjectId().hashCode();
    hash = (37 * hash) + INSTANCE_ID_FIELD_NUMBER;
    hash = (53 * hash) + getInstanceId().hashCode();
    hash = (37 * hash) + BACKUP_ID_FIELD_NUMBER;
    hash = (53 * hash) + getBackupId().hashCode();
    hash = (37 * hash) + SOURCE_BACKUP_FIELD_NUMBER;
    hash = (53 * hash) + getSourceBackup().hashCode();
    hash = (37 * hash) + EXPIRE_TIME_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getExpireTime());
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.CopyCloudBackupAction parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.google.spanner.executor.v1.CopyCloudBackupAction prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * Action that copies a Cloud Spanner backup.
   * </pre>
   *
   * Protobuf type {@code google.spanner.executor.v1.CopyCloudBackupAction}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:google.spanner.executor.v1.CopyCloudBackupAction)
      com.google.spanner.executor.v1.CopyCloudBackupActionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.google.spanner.executor.v1.ExecutorProto.internal_static_google_spanner_executor_v1_CopyCloudBackupAction_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.executor.v1.ExecutorProto.internal_static_google_spanner_executor_v1_CopyCloudBackupAction_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.executor.v1.CopyCloudBackupAction.class, com.google.spanner.executor.v1.CopyCloudBackupAction.Builder.class);
    }

    // Construct using com.google.spanner.executor.v1.CopyCloudBackupAction.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      projectId_ = "";

      instanceId_ = "";

      backupId_ = "";

      sourceBackup_ = "";

      expireTime_ = 0L;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.google.spanner.executor.v1.ExecutorProto.internal_static_google_spanner_executor_v1_CopyCloudBackupAction_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.CopyCloudBackupAction getDefaultInstanceForType() {
      return com.google.spanner.executor.v1.CopyCloudBackupAction.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.CopyCloudBackupAction build() {
      com.google.spanner.executor.v1.CopyCloudBackupAction result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.CopyCloudBackupAction buildPartial() {
      com.google.spanner.executor.v1.CopyCloudBackupAction result = new com.google.spanner.executor.v1.CopyCloudBackupAction(this);
      result.projectId_ = projectId_;
      result.instanceId_ = instanceId_;
      result.backupId_ = backupId_;
      result.sourceBackup_ = sourceBackup_;
      result.expireTime_ = expireTime_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.google.spanner.executor.v1.CopyCloudBackupAction) {
        return mergeFrom((com.google.spanner.executor.v1.CopyCloudBackupAction)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.executor.v1.CopyCloudBackupAction other) {
      if (other == com.google.spanner.executor.v1.CopyCloudBackupAction.getDefaultInstance()) return this;
      if (!other.getProjectId().isEmpty()) {
        projectId_ = other.projectId_;
        onChanged();
      }
      if (!other.getInstanceId().isEmpty()) {
        instanceId_ = other.instanceId_;
        onChanged();
      }
      if (!other.getBackupId().isEmpty()) {
        backupId_ = other.backupId_;
        onChanged();
      }
      if (!other.getSourceBackup().isEmpty()) {
        sourceBackup_ = other.sourceBackup_;
        onChanged();
      }
      if (other.getExpireTime() != 0L) {
        setExpireTime(other.getExpireTime());
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
      com.google.spanner.executor.v1.CopyCloudBackupAction parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.google.spanner.executor.v1.CopyCloudBackupAction) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object projectId_ = "";
    /**
     * <pre>
     * Required. Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @return The projectId.
     */
    public java.lang.String getProjectId() {
      java.lang.Object ref = projectId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        projectId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <pre>
     * Required. Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @return The bytes for projectId.
     */
    public com.google.protobuf.ByteString
        getProjectIdBytes() {
      java.lang.Object ref = projectId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        projectId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <pre>
     * Required. Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @param value The projectId to set.
     * @return This builder for chaining.
     */
    public Builder setProjectId(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      projectId_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearProjectId() {
      
      projectId_ = getDefaultInstance().getProjectId();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @param value The bytes for projectId to set.
     * @return This builder for chaining.
     */
    public Builder setProjectIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      projectId_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object instanceId_ = "";
    /**
     * <pre>
     * Required. Cloud instance ID (not path), e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @return The instanceId.
     */
    public java.lang.String getInstanceId() {
      java.lang.Object ref = instanceId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        instanceId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <pre>
     * Required. Cloud instance ID (not path), e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @return The bytes for instanceId.
     */
    public com.google.protobuf.ByteString
        getInstanceIdBytes() {
      java.lang.Object ref = instanceId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        instanceId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <pre>
     * Required. Cloud instance ID (not path), e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @param value The instanceId to set.
     * @return This builder for chaining.
     */
    public Builder setInstanceId(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      instanceId_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. Cloud instance ID (not path), e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearInstanceId() {
      
      instanceId_ = getDefaultInstance().getInstanceId();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. Cloud instance ID (not path), e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @param value The bytes for instanceId to set.
     * @return This builder for chaining.
     */
    public Builder setInstanceIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      instanceId_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object backupId_ = "";
    /**
     * <pre>
     * Required. The id of the backup to be created, e.g. "test-backup".
     * </pre>
     *
     * <code>string backup_id = 3;</code>
     * @return The backupId.
     */
    public java.lang.String getBackupId() {
      java.lang.Object ref = backupId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        backupId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <pre>
     * Required. The id of the backup to be created, e.g. "test-backup".
     * </pre>
     *
     * <code>string backup_id = 3;</code>
     * @return The bytes for backupId.
     */
    public com.google.protobuf.ByteString
        getBackupIdBytes() {
      java.lang.Object ref = backupId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        backupId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <pre>
     * Required. The id of the backup to be created, e.g. "test-backup".
     * </pre>
     *
     * <code>string backup_id = 3;</code>
     * @param value The backupId to set.
     * @return This builder for chaining.
     */
    public Builder setBackupId(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      backupId_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. The id of the backup to be created, e.g. "test-backup".
     * </pre>
     *
     * <code>string backup_id = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearBackupId() {
      
      backupId_ = getDefaultInstance().getBackupId();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. The id of the backup to be created, e.g. "test-backup".
     * </pre>
     *
     * <code>string backup_id = 3;</code>
     * @param value The bytes for backupId to set.
     * @return This builder for chaining.
     */
    public Builder setBackupIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      backupId_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object sourceBackup_ = "";
    /**
     * <pre>
     * Required. The fully qualified uri of the source backup from which this
     * backup was copied. eg.
     * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
     * </pre>
     *
     * <code>string source_backup = 4;</code>
     * @return The sourceBackup.
     */
    public java.lang.String getSourceBackup() {
      java.lang.Object ref = sourceBackup_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        sourceBackup_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <pre>
     * Required. The fully qualified uri of the source backup from which this
     * backup was copied. eg.
     * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
     * </pre>
     *
     * <code>string source_backup = 4;</code>
     * @return The bytes for sourceBackup.
     */
    public com.google.protobuf.ByteString
        getSourceBackupBytes() {
      java.lang.Object ref = sourceBackup_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        sourceBackup_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <pre>
     * Required. The fully qualified uri of the source backup from which this
     * backup was copied. eg.
     * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
     * </pre>
     *
     * <code>string source_backup = 4;</code>
     * @param value The sourceBackup to set.
     * @return This builder for chaining.
     */
    public Builder setSourceBackup(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      sourceBackup_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. The fully qualified uri of the source backup from which this
     * backup was copied. eg.
     * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
     * </pre>
     *
     * <code>string source_backup = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearSourceBackup() {
      
      sourceBackup_ = getDefaultInstance().getSourceBackup();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. The fully qualified uri of the source backup from which this
     * backup was copied. eg.
     * "projects/&lt;project_id&gt;/instances/&lt;instance_id&gt;/backups/&lt;backup_id&gt;".
     * </pre>
     *
     * <code>string source_backup = 4;</code>
     * @param value The bytes for sourceBackup to set.
     * @return This builder for chaining.
     */
    public Builder setSourceBackupBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      sourceBackup_ = value;
      onChanged();
      return this;
    }

    private long expireTime_ ;
    /**
     * <pre>
     * Required. The expiration time of the backup, with microseconds granularity
     * that must be at least 6 hours and at most 366 days
     * from the time the request is received.
     * </pre>
     *
     * <code>int64 expire_time = 5;</code>
     * @return The expireTime.
     */
    @java.lang.Override
    public long getExpireTime() {
      return expireTime_;
    }
    /**
     * <pre>
     * Required. The expiration time of the backup, with microseconds granularity
     * that must be at least 6 hours and at most 366 days
     * from the time the request is received.
     * </pre>
     *
     * <code>int64 expire_time = 5;</code>
     * @param value The expireTime to set.
     * @return This builder for chaining.
     */
    public Builder setExpireTime(long value) {
      
      expireTime_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Required. The expiration time of the backup, with microseconds granularity
     * that must be at least 6 hours and at most 366 days
     * from the time the request is received.
     * </pre>
     *
     * <code>int64 expire_time = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearExpireTime() {
      
      expireTime_ = 0L;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:google.spanner.executor.v1.CopyCloudBackupAction)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.executor.v1.CopyCloudBackupAction)
  private static final com.google.spanner.executor.v1.CopyCloudBackupAction DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.google.spanner.executor.v1.CopyCloudBackupAction();
  }

  public static com.google.spanner.executor.v1.CopyCloudBackupAction getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<CopyCloudBackupAction>
      PARSER = new com.google.protobuf.AbstractParser<CopyCloudBackupAction>() {
    @java.lang.Override
    public CopyCloudBackupAction parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new CopyCloudBackupAction(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<CopyCloudBackupAction> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<CopyCloudBackupAction> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.executor.v1.CopyCloudBackupAction getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

