/*
 * Copyright 2024 Google LLC
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
// source: google/spanner/v1/spanner.proto

// Protobuf Java Version: 3.25.4
package com.google.spanner.v1;

/**
 *
 *
 * <pre>
 * The result of applying a batch of mutations.
 * </pre>
 *
 * Protobuf type {@code google.spanner.v1.BatchWriteResponse}
 */
public final class BatchWriteResponse extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.v1.BatchWriteResponse)
    BatchWriteResponseOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use BatchWriteResponse.newBuilder() to construct.
  private BatchWriteResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private BatchWriteResponse() {
    indexes_ = emptyIntList();
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new BatchWriteResponse();
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_BatchWriteResponse_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_BatchWriteResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.v1.BatchWriteResponse.class,
            com.google.spanner.v1.BatchWriteResponse.Builder.class);
  }

  private int bitField0_;
  public static final int INDEXES_FIELD_NUMBER = 1;

  @SuppressWarnings("serial")
  private com.google.protobuf.Internal.IntList indexes_ = emptyIntList();
  /**
   *
   *
   * <pre>
   * The mutation groups applied in this batch. The values index into the
   * `mutation_groups` field in the corresponding `BatchWriteRequest`.
   * </pre>
   *
   * <code>repeated int32 indexes = 1;</code>
   *
   * @return A list containing the indexes.
   */
  @java.lang.Override
  public java.util.List<java.lang.Integer> getIndexesList() {
    return indexes_;
  }
  /**
   *
   *
   * <pre>
   * The mutation groups applied in this batch. The values index into the
   * `mutation_groups` field in the corresponding `BatchWriteRequest`.
   * </pre>
   *
   * <code>repeated int32 indexes = 1;</code>
   *
   * @return The count of indexes.
   */
  public int getIndexesCount() {
    return indexes_.size();
  }
  /**
   *
   *
   * <pre>
   * The mutation groups applied in this batch. The values index into the
   * `mutation_groups` field in the corresponding `BatchWriteRequest`.
   * </pre>
   *
   * <code>repeated int32 indexes = 1;</code>
   *
   * @param index The index of the element to return.
   * @return The indexes at the given index.
   */
  public int getIndexes(int index) {
    return indexes_.getInt(index);
  }

  private int indexesMemoizedSerializedSize = -1;

  public static final int STATUS_FIELD_NUMBER = 2;
  private com.google.rpc.Status status_;
  /**
   *
   *
   * <pre>
   * An `OK` status indicates success. Any other status indicates a failure.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   *
   * @return Whether the status field is set.
   */
  @java.lang.Override
  public boolean hasStatus() {
    return ((bitField0_ & 0x00000001) != 0);
  }
  /**
   *
   *
   * <pre>
   * An `OK` status indicates success. Any other status indicates a failure.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   *
   * @return The status.
   */
  @java.lang.Override
  public com.google.rpc.Status getStatus() {
    return status_ == null ? com.google.rpc.Status.getDefaultInstance() : status_;
  }
  /**
   *
   *
   * <pre>
   * An `OK` status indicates success. Any other status indicates a failure.
   * </pre>
   *
   * <code>.google.rpc.Status status = 2;</code>
   */
  @java.lang.Override
  public com.google.rpc.StatusOrBuilder getStatusOrBuilder() {
    return status_ == null ? com.google.rpc.Status.getDefaultInstance() : status_;
  }

  public static final int COMMIT_TIMESTAMP_FIELD_NUMBER = 3;
  private com.google.protobuf.Timestamp commitTimestamp_;
  /**
   *
   *
   * <pre>
   * The commit timestamp of the transaction that applied this batch.
   * Present if `status` is `OK`, absent otherwise.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
   *
   * @return Whether the commitTimestamp field is set.
   */
  @java.lang.Override
  public boolean hasCommitTimestamp() {
    return ((bitField0_ & 0x00000002) != 0);
  }
  /**
   *
   *
   * <pre>
   * The commit timestamp of the transaction that applied this batch.
   * Present if `status` is `OK`, absent otherwise.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
   *
   * @return The commitTimestamp.
   */
  @java.lang.Override
  public com.google.protobuf.Timestamp getCommitTimestamp() {
    return commitTimestamp_ == null
        ? com.google.protobuf.Timestamp.getDefaultInstance()
        : commitTimestamp_;
  }
  /**
   *
   *
   * <pre>
   * The commit timestamp of the transaction that applied this batch.
   * Present if `status` is `OK`, absent otherwise.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
   */
  @java.lang.Override
  public com.google.protobuf.TimestampOrBuilder getCommitTimestampOrBuilder() {
    return commitTimestamp_ == null
        ? com.google.protobuf.Timestamp.getDefaultInstance()
        : commitTimestamp_;
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
    getSerializedSize();
    if (getIndexesList().size() > 0) {
      output.writeUInt32NoTag(10);
      output.writeUInt32NoTag(indexesMemoizedSerializedSize);
    }
    for (int i = 0; i < indexes_.size(); i++) {
      output.writeInt32NoTag(indexes_.getInt(i));
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      output.writeMessage(2, getStatus());
    }
    if (((bitField0_ & 0x00000002) != 0)) {
      output.writeMessage(3, getCommitTimestamp());
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    {
      int dataSize = 0;
      for (int i = 0; i < indexes_.size(); i++) {
        dataSize += com.google.protobuf.CodedOutputStream.computeInt32SizeNoTag(indexes_.getInt(i));
      }
      size += dataSize;
      if (!getIndexesList().isEmpty()) {
        size += 1;
        size += com.google.protobuf.CodedOutputStream.computeInt32SizeNoTag(dataSize);
      }
      indexesMemoizedSerializedSize = dataSize;
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getStatus());
    }
    if (((bitField0_ & 0x00000002) != 0)) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(3, getCommitTimestamp());
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
    if (!(obj instanceof com.google.spanner.v1.BatchWriteResponse)) {
      return super.equals(obj);
    }
    com.google.spanner.v1.BatchWriteResponse other = (com.google.spanner.v1.BatchWriteResponse) obj;

    if (!getIndexesList().equals(other.getIndexesList())) return false;
    if (hasStatus() != other.hasStatus()) return false;
    if (hasStatus()) {
      if (!getStatus().equals(other.getStatus())) return false;
    }
    if (hasCommitTimestamp() != other.hasCommitTimestamp()) return false;
    if (hasCommitTimestamp()) {
      if (!getCommitTimestamp().equals(other.getCommitTimestamp())) return false;
    }
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
    if (getIndexesCount() > 0) {
      hash = (37 * hash) + INDEXES_FIELD_NUMBER;
      hash = (53 * hash) + getIndexesList().hashCode();
    }
    if (hasStatus()) {
      hash = (37 * hash) + STATUS_FIELD_NUMBER;
      hash = (53 * hash) + getStatus().hashCode();
    }
    if (hasCommitTimestamp()) {
      hash = (37 * hash) + COMMIT_TIMESTAMP_FIELD_NUMBER;
      hash = (53 * hash) + getCommitTimestamp().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.BatchWriteResponse parseFrom(
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

  public static Builder newBuilder(com.google.spanner.v1.BatchWriteResponse prototype) {
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
   * The result of applying a batch of mutations.
   * </pre>
   *
   * Protobuf type {@code google.spanner.v1.BatchWriteResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.v1.BatchWriteResponse)
      com.google.spanner.v1.BatchWriteResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_BatchWriteResponse_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_BatchWriteResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.v1.BatchWriteResponse.class,
              com.google.spanner.v1.BatchWriteResponse.Builder.class);
    }

    // Construct using com.google.spanner.v1.BatchWriteResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getStatusFieldBuilder();
        getCommitTimestampFieldBuilder();
      }
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      indexes_ = emptyIntList();
      status_ = null;
      if (statusBuilder_ != null) {
        statusBuilder_.dispose();
        statusBuilder_ = null;
      }
      commitTimestamp_ = null;
      if (commitTimestampBuilder_ != null) {
        commitTimestampBuilder_.dispose();
        commitTimestampBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_BatchWriteResponse_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.v1.BatchWriteResponse getDefaultInstanceForType() {
      return com.google.spanner.v1.BatchWriteResponse.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.v1.BatchWriteResponse build() {
      com.google.spanner.v1.BatchWriteResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.v1.BatchWriteResponse buildPartial() {
      com.google.spanner.v1.BatchWriteResponse result =
          new com.google.spanner.v1.BatchWriteResponse(this);
      if (bitField0_ != 0) {
        buildPartial0(result);
      }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.google.spanner.v1.BatchWriteResponse result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        indexes_.makeImmutable();
        result.indexes_ = indexes_;
      }
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.status_ = statusBuilder_ == null ? status_ : statusBuilder_.build();
        to_bitField0_ |= 0x00000001;
      }
      if (((from_bitField0_ & 0x00000004) != 0)) {
        result.commitTimestamp_ =
            commitTimestampBuilder_ == null ? commitTimestamp_ : commitTimestampBuilder_.build();
        to_bitField0_ |= 0x00000002;
      }
      result.bitField0_ |= to_bitField0_;
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
      if (other instanceof com.google.spanner.v1.BatchWriteResponse) {
        return mergeFrom((com.google.spanner.v1.BatchWriteResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.v1.BatchWriteResponse other) {
      if (other == com.google.spanner.v1.BatchWriteResponse.getDefaultInstance()) return this;
      if (!other.indexes_.isEmpty()) {
        if (indexes_.isEmpty()) {
          indexes_ = other.indexes_;
          indexes_.makeImmutable();
          bitField0_ |= 0x00000001;
        } else {
          ensureIndexesIsMutable();
          indexes_.addAll(other.indexes_);
        }
        onChanged();
      }
      if (other.hasStatus()) {
        mergeStatus(other.getStatus());
      }
      if (other.hasCommitTimestamp()) {
        mergeCommitTimestamp(other.getCommitTimestamp());
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
            case 8:
              {
                int v = input.readInt32();
                ensureIndexesIsMutable();
                indexes_.addInt(v);
                break;
              } // case 8
            case 10:
              {
                int length = input.readRawVarint32();
                int limit = input.pushLimit(length);
                ensureIndexesIsMutable();
                while (input.getBytesUntilLimit() > 0) {
                  indexes_.addInt(input.readInt32());
                }
                input.popLimit(limit);
                break;
              } // case 10
            case 18:
              {
                input.readMessage(getStatusFieldBuilder().getBuilder(), extensionRegistry);
                bitField0_ |= 0x00000002;
                break;
              } // case 18
            case 26:
              {
                input.readMessage(getCommitTimestampFieldBuilder().getBuilder(), extensionRegistry);
                bitField0_ |= 0x00000004;
                break;
              } // case 26
            default:
              {
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

    private com.google.protobuf.Internal.IntList indexes_ = emptyIntList();

    private void ensureIndexesIsMutable() {
      if (!indexes_.isModifiable()) {
        indexes_ = makeMutableCopy(indexes_);
      }
      bitField0_ |= 0x00000001;
    }
    /**
     *
     *
     * <pre>
     * The mutation groups applied in this batch. The values index into the
     * `mutation_groups` field in the corresponding `BatchWriteRequest`.
     * </pre>
     *
     * <code>repeated int32 indexes = 1;</code>
     *
     * @return A list containing the indexes.
     */
    public java.util.List<java.lang.Integer> getIndexesList() {
      indexes_.makeImmutable();
      return indexes_;
    }
    /**
     *
     *
     * <pre>
     * The mutation groups applied in this batch. The values index into the
     * `mutation_groups` field in the corresponding `BatchWriteRequest`.
     * </pre>
     *
     * <code>repeated int32 indexes = 1;</code>
     *
     * @return The count of indexes.
     */
    public int getIndexesCount() {
      return indexes_.size();
    }
    /**
     *
     *
     * <pre>
     * The mutation groups applied in this batch. The values index into the
     * `mutation_groups` field in the corresponding `BatchWriteRequest`.
     * </pre>
     *
     * <code>repeated int32 indexes = 1;</code>
     *
     * @param index The index of the element to return.
     * @return The indexes at the given index.
     */
    public int getIndexes(int index) {
      return indexes_.getInt(index);
    }
    /**
     *
     *
     * <pre>
     * The mutation groups applied in this batch. The values index into the
     * `mutation_groups` field in the corresponding `BatchWriteRequest`.
     * </pre>
     *
     * <code>repeated int32 indexes = 1;</code>
     *
     * @param index The index to set the value at.
     * @param value The indexes to set.
     * @return This builder for chaining.
     */
    public Builder setIndexes(int index, int value) {

      ensureIndexesIsMutable();
      indexes_.setInt(index, value);
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The mutation groups applied in this batch. The values index into the
     * `mutation_groups` field in the corresponding `BatchWriteRequest`.
     * </pre>
     *
     * <code>repeated int32 indexes = 1;</code>
     *
     * @param value The indexes to add.
     * @return This builder for chaining.
     */
    public Builder addIndexes(int value) {

      ensureIndexesIsMutable();
      indexes_.addInt(value);
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The mutation groups applied in this batch. The values index into the
     * `mutation_groups` field in the corresponding `BatchWriteRequest`.
     * </pre>
     *
     * <code>repeated int32 indexes = 1;</code>
     *
     * @param values The indexes to add.
     * @return This builder for chaining.
     */
    public Builder addAllIndexes(java.lang.Iterable<? extends java.lang.Integer> values) {
      ensureIndexesIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(values, indexes_);
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The mutation groups applied in this batch. The values index into the
     * `mutation_groups` field in the corresponding `BatchWriteRequest`.
     * </pre>
     *
     * <code>repeated int32 indexes = 1;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearIndexes() {
      indexes_ = emptyIntList();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }

    private com.google.rpc.Status status_;
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.rpc.Status, com.google.rpc.Status.Builder, com.google.rpc.StatusOrBuilder>
        statusBuilder_;
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     *
     * @return Whether the status field is set.
     */
    public boolean hasStatus() {
      return ((bitField0_ & 0x00000002) != 0);
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     *
     * @return The status.
     */
    public com.google.rpc.Status getStatus() {
      if (statusBuilder_ == null) {
        return status_ == null ? com.google.rpc.Status.getDefaultInstance() : status_;
      } else {
        return statusBuilder_.getMessage();
      }
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     */
    public Builder setStatus(com.google.rpc.Status value) {
      if (statusBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        status_ = value;
      } else {
        statusBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     */
    public Builder setStatus(com.google.rpc.Status.Builder builderForValue) {
      if (statusBuilder_ == null) {
        status_ = builderForValue.build();
      } else {
        statusBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     */
    public Builder mergeStatus(com.google.rpc.Status value) {
      if (statusBuilder_ == null) {
        if (((bitField0_ & 0x00000002) != 0)
            && status_ != null
            && status_ != com.google.rpc.Status.getDefaultInstance()) {
          getStatusBuilder().mergeFrom(value);
        } else {
          status_ = value;
        }
      } else {
        statusBuilder_.mergeFrom(value);
      }
      if (status_ != null) {
        bitField0_ |= 0x00000002;
        onChanged();
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     */
    public Builder clearStatus() {
      bitField0_ = (bitField0_ & ~0x00000002);
      status_ = null;
      if (statusBuilder_ != null) {
        statusBuilder_.dispose();
        statusBuilder_ = null;
      }
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     */
    public com.google.rpc.Status.Builder getStatusBuilder() {
      bitField0_ |= 0x00000002;
      onChanged();
      return getStatusFieldBuilder().getBuilder();
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     */
    public com.google.rpc.StatusOrBuilder getStatusOrBuilder() {
      if (statusBuilder_ != null) {
        return statusBuilder_.getMessageOrBuilder();
      } else {
        return status_ == null ? com.google.rpc.Status.getDefaultInstance() : status_;
      }
    }
    /**
     *
     *
     * <pre>
     * An `OK` status indicates success. Any other status indicates a failure.
     * </pre>
     *
     * <code>.google.rpc.Status status = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.rpc.Status, com.google.rpc.Status.Builder, com.google.rpc.StatusOrBuilder>
        getStatusFieldBuilder() {
      if (statusBuilder_ == null) {
        statusBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<
                com.google.rpc.Status,
                com.google.rpc.Status.Builder,
                com.google.rpc.StatusOrBuilder>(getStatus(), getParentForChildren(), isClean());
        status_ = null;
      }
      return statusBuilder_;
    }

    private com.google.protobuf.Timestamp commitTimestamp_;
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp,
            com.google.protobuf.Timestamp.Builder,
            com.google.protobuf.TimestampOrBuilder>
        commitTimestampBuilder_;
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     *
     * @return Whether the commitTimestamp field is set.
     */
    public boolean hasCommitTimestamp() {
      return ((bitField0_ & 0x00000004) != 0);
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     *
     * @return The commitTimestamp.
     */
    public com.google.protobuf.Timestamp getCommitTimestamp() {
      if (commitTimestampBuilder_ == null) {
        return commitTimestamp_ == null
            ? com.google.protobuf.Timestamp.getDefaultInstance()
            : commitTimestamp_;
      } else {
        return commitTimestampBuilder_.getMessage();
      }
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     */
    public Builder setCommitTimestamp(com.google.protobuf.Timestamp value) {
      if (commitTimestampBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        commitTimestamp_ = value;
      } else {
        commitTimestampBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000004;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     */
    public Builder setCommitTimestamp(com.google.protobuf.Timestamp.Builder builderForValue) {
      if (commitTimestampBuilder_ == null) {
        commitTimestamp_ = builderForValue.build();
      } else {
        commitTimestampBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000004;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     */
    public Builder mergeCommitTimestamp(com.google.protobuf.Timestamp value) {
      if (commitTimestampBuilder_ == null) {
        if (((bitField0_ & 0x00000004) != 0)
            && commitTimestamp_ != null
            && commitTimestamp_ != com.google.protobuf.Timestamp.getDefaultInstance()) {
          getCommitTimestampBuilder().mergeFrom(value);
        } else {
          commitTimestamp_ = value;
        }
      } else {
        commitTimestampBuilder_.mergeFrom(value);
      }
      if (commitTimestamp_ != null) {
        bitField0_ |= 0x00000004;
        onChanged();
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     */
    public Builder clearCommitTimestamp() {
      bitField0_ = (bitField0_ & ~0x00000004);
      commitTimestamp_ = null;
      if (commitTimestampBuilder_ != null) {
        commitTimestampBuilder_.dispose();
        commitTimestampBuilder_ = null;
      }
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     */
    public com.google.protobuf.Timestamp.Builder getCommitTimestampBuilder() {
      bitField0_ |= 0x00000004;
      onChanged();
      return getCommitTimestampFieldBuilder().getBuilder();
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     */
    public com.google.protobuf.TimestampOrBuilder getCommitTimestampOrBuilder() {
      if (commitTimestampBuilder_ != null) {
        return commitTimestampBuilder_.getMessageOrBuilder();
      } else {
        return commitTimestamp_ == null
            ? com.google.protobuf.Timestamp.getDefaultInstance()
            : commitTimestamp_;
      }
    }
    /**
     *
     *
     * <pre>
     * The commit timestamp of the transaction that applied this batch.
     * Present if `status` is `OK`, absent otherwise.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp commit_timestamp = 3;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp,
            com.google.protobuf.Timestamp.Builder,
            com.google.protobuf.TimestampOrBuilder>
        getCommitTimestampFieldBuilder() {
      if (commitTimestampBuilder_ == null) {
        commitTimestampBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<
                com.google.protobuf.Timestamp,
                com.google.protobuf.Timestamp.Builder,
                com.google.protobuf.TimestampOrBuilder>(
                getCommitTimestamp(), getParentForChildren(), isClean());
        commitTimestamp_ = null;
      }
      return commitTimestampBuilder_;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.v1.BatchWriteResponse)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.v1.BatchWriteResponse)
  private static final com.google.spanner.v1.BatchWriteResponse DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.v1.BatchWriteResponse();
  }

  public static com.google.spanner.v1.BatchWriteResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<BatchWriteResponse> PARSER =
      new com.google.protobuf.AbstractParser<BatchWriteResponse>() {
        @java.lang.Override
        public BatchWriteResponse parsePartialFrom(
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

  public static com.google.protobuf.Parser<BatchWriteResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<BatchWriteResponse> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.v1.BatchWriteResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
