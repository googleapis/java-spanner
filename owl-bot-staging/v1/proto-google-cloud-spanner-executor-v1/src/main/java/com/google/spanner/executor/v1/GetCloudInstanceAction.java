// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

/**
 * <pre>
 * Action that retrieves a Cloud Spanner instance.
 * </pre>
 *
 * Protobuf type {@code google.spanner.executor.v1.GetCloudInstanceAction}
 */
public final class GetCloudInstanceAction extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:google.spanner.executor.v1.GetCloudInstanceAction)
    GetCloudInstanceActionOrBuilder {
private static final long serialVersionUID = 0L;
  // Use GetCloudInstanceAction.newBuilder() to construct.
  private GetCloudInstanceAction(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private GetCloudInstanceAction() {
    projectId_ = "";
    instanceId_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new GetCloudInstanceAction();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_GetCloudInstanceAction_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_GetCloudInstanceAction_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.executor.v1.GetCloudInstanceAction.class, com.google.spanner.executor.v1.GetCloudInstanceAction.Builder.class);
  }

  public static final int PROJECT_ID_FIELD_NUMBER = 1;
  @SuppressWarnings("serial")
  private volatile java.lang.Object projectId_ = "";
  /**
   * <pre>
   * Cloud project ID, e.g. "spanner-cloud-systest".
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
   * Cloud project ID, e.g. "spanner-cloud-systest".
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
  @SuppressWarnings("serial")
  private volatile java.lang.Object instanceId_ = "";
  /**
   * <pre>
   * Cloud instance ID (not path) to retrieve the instance from,
   * e.g. "test-instance".
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
   * Cloud instance ID (not path) to retrieve the instance from,
   * e.g. "test-instance".
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
    getUnknownFields().writeTo(output);
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
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.google.spanner.executor.v1.GetCloudInstanceAction)) {
      return super.equals(obj);
    }
    com.google.spanner.executor.v1.GetCloudInstanceAction other = (com.google.spanner.executor.v1.GetCloudInstanceAction) obj;

    if (!getProjectId()
        .equals(other.getProjectId())) return false;
    if (!getInstanceId()
        .equals(other.getInstanceId())) return false;
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
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
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.GetCloudInstanceAction parseFrom(
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
  public static Builder newBuilder(com.google.spanner.executor.v1.GetCloudInstanceAction prototype) {
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
   * Action that retrieves a Cloud Spanner instance.
   * </pre>
   *
   * Protobuf type {@code google.spanner.executor.v1.GetCloudInstanceAction}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:google.spanner.executor.v1.GetCloudInstanceAction)
      com.google.spanner.executor.v1.GetCloudInstanceActionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_GetCloudInstanceAction_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_GetCloudInstanceAction_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.executor.v1.GetCloudInstanceAction.class, com.google.spanner.executor.v1.GetCloudInstanceAction.Builder.class);
    }

    // Construct using com.google.spanner.executor.v1.GetCloudInstanceAction.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      projectId_ = "";
      instanceId_ = "";
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_GetCloudInstanceAction_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.GetCloudInstanceAction getDefaultInstanceForType() {
      return com.google.spanner.executor.v1.GetCloudInstanceAction.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.GetCloudInstanceAction build() {
      com.google.spanner.executor.v1.GetCloudInstanceAction result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.GetCloudInstanceAction buildPartial() {
      com.google.spanner.executor.v1.GetCloudInstanceAction result = new com.google.spanner.executor.v1.GetCloudInstanceAction(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.google.spanner.executor.v1.GetCloudInstanceAction result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.projectId_ = projectId_;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.instanceId_ = instanceId_;
      }
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
      if (other instanceof com.google.spanner.executor.v1.GetCloudInstanceAction) {
        return mergeFrom((com.google.spanner.executor.v1.GetCloudInstanceAction)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.executor.v1.GetCloudInstanceAction other) {
      if (other == com.google.spanner.executor.v1.GetCloudInstanceAction.getDefaultInstance()) return this;
      if (!other.getProjectId().isEmpty()) {
        projectId_ = other.projectId_;
        bitField0_ |= 0x00000001;
        onChanged();
      }
      if (!other.getInstanceId().isEmpty()) {
        instanceId_ = other.instanceId_;
        bitField0_ |= 0x00000002;
        onChanged();
      }
      this.mergeUnknownFields(other.getUnknownFields());
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
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              projectId_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000001;
              break;
            } // case 10
            case 18: {
              instanceId_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000002;
              break;
            } // case 18
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private java.lang.Object projectId_ = "";
    /**
     * <pre>
     * Cloud project ID, e.g. "spanner-cloud-systest".
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
     * Cloud project ID, e.g. "spanner-cloud-systest".
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
     * Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @param value The projectId to set.
     * @return This builder for chaining.
     */
    public Builder setProjectId(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      projectId_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearProjectId() {
      projectId_ = getDefaultInstance().getProjectId();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Cloud project ID, e.g. "spanner-cloud-systest".
     * </pre>
     *
     * <code>string project_id = 1;</code>
     * @param value The bytes for projectId to set.
     * @return This builder for chaining.
     */
    public Builder setProjectIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      projectId_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }

    private java.lang.Object instanceId_ = "";
    /**
     * <pre>
     * Cloud instance ID (not path) to retrieve the instance from,
     * e.g. "test-instance".
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
     * Cloud instance ID (not path) to retrieve the instance from,
     * e.g. "test-instance".
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
     * Cloud instance ID (not path) to retrieve the instance from,
     * e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @param value The instanceId to set.
     * @return This builder for chaining.
     */
    public Builder setInstanceId(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      instanceId_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Cloud instance ID (not path) to retrieve the instance from,
     * e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearInstanceId() {
      instanceId_ = getDefaultInstance().getInstanceId();
      bitField0_ = (bitField0_ & ~0x00000002);
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Cloud instance ID (not path) to retrieve the instance from,
     * e.g. "test-instance".
     * </pre>
     *
     * <code>string instance_id = 2;</code>
     * @param value The bytes for instanceId to set.
     * @return This builder for chaining.
     */
    public Builder setInstanceIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      instanceId_ = value;
      bitField0_ |= 0x00000002;
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


    // @@protoc_insertion_point(builder_scope:google.spanner.executor.v1.GetCloudInstanceAction)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.executor.v1.GetCloudInstanceAction)
  private static final com.google.spanner.executor.v1.GetCloudInstanceAction DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.google.spanner.executor.v1.GetCloudInstanceAction();
  }

  public static com.google.spanner.executor.v1.GetCloudInstanceAction getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<GetCloudInstanceAction>
      PARSER = new com.google.protobuf.AbstractParser<GetCloudInstanceAction>() {
    @java.lang.Override
    public GetCloudInstanceAction parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<GetCloudInstanceAction> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<GetCloudInstanceAction> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.executor.v1.GetCloudInstanceAction getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

