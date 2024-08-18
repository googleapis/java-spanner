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
 * Options for a PartitionQueryRequest and
 * PartitionReadRequest.
 * </pre>
 *
 * Protobuf type {@code google.spanner.v1.PartitionOptions}
 */
public final class PartitionOptions extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.v1.PartitionOptions)
    PartitionOptionsOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use PartitionOptions.newBuilder() to construct.
  private PartitionOptions(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private PartitionOptions() {}

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new PartitionOptions();
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_PartitionOptions_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_PartitionOptions_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.v1.PartitionOptions.class,
            com.google.spanner.v1.PartitionOptions.Builder.class);
  }

  public static final int PARTITION_SIZE_BYTES_FIELD_NUMBER = 1;
  private long partitionSizeBytes_ = 0L;
  /**
   *
   *
   * <pre>
   * **Note:** This hint is currently ignored by PartitionQuery and
   * PartitionRead requests.
   *
   * The desired data size for each partition generated.  The default for this
   * option is currently 1 GiB.  This is only a hint. The actual size of each
   * partition may be smaller or larger than this size request.
   * </pre>
   *
   * <code>int64 partition_size_bytes = 1;</code>
   *
   * @return The partitionSizeBytes.
   */
  @java.lang.Override
  public long getPartitionSizeBytes() {
    return partitionSizeBytes_;
  }

  public static final int MAX_PARTITIONS_FIELD_NUMBER = 2;
  private long maxPartitions_ = 0L;
  /**
   *
   *
   * <pre>
   * **Note:** This hint is currently ignored by PartitionQuery and
   * PartitionRead requests.
   *
   * The desired maximum number of partitions to return.  For example, this may
   * be set to the number of workers available.  The default for this option
   * is currently 10,000. The maximum value is currently 200,000.  This is only
   * a hint.  The actual number of partitions returned may be smaller or larger
   * than this maximum count request.
   * </pre>
   *
   * <code>int64 max_partitions = 2;</code>
   *
   * @return The maxPartitions.
   */
  @java.lang.Override
  public long getMaxPartitions() {
    return maxPartitions_;
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
    if (partitionSizeBytes_ != 0L) {
      output.writeInt64(1, partitionSizeBytes_);
    }
    if (maxPartitions_ != 0L) {
      output.writeInt64(2, maxPartitions_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (partitionSizeBytes_ != 0L) {
      size += com.google.protobuf.CodedOutputStream.computeInt64Size(1, partitionSizeBytes_);
    }
    if (maxPartitions_ != 0L) {
      size += com.google.protobuf.CodedOutputStream.computeInt64Size(2, maxPartitions_);
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
    if (!(obj instanceof com.google.spanner.v1.PartitionOptions)) {
      return super.equals(obj);
    }
    com.google.spanner.v1.PartitionOptions other = (com.google.spanner.v1.PartitionOptions) obj;

    if (getPartitionSizeBytes() != other.getPartitionSizeBytes()) return false;
    if (getMaxPartitions() != other.getMaxPartitions()) return false;
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
    hash = (37 * hash) + PARTITION_SIZE_BYTES_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getPartitionSizeBytes());
    hash = (37 * hash) + MAX_PARTITIONS_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getMaxPartitions());
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.PartitionOptions parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.PartitionOptions parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.PartitionOptions parseFrom(
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

  public static Builder newBuilder(com.google.spanner.v1.PartitionOptions prototype) {
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
   * Options for a PartitionQueryRequest and
   * PartitionReadRequest.
   * </pre>
   *
   * Protobuf type {@code google.spanner.v1.PartitionOptions}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.v1.PartitionOptions)
      com.google.spanner.v1.PartitionOptionsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_PartitionOptions_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_PartitionOptions_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.v1.PartitionOptions.class,
              com.google.spanner.v1.PartitionOptions.Builder.class);
    }

    // Construct using com.google.spanner.v1.PartitionOptions.newBuilder()
    private Builder() {}

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      partitionSizeBytes_ = 0L;
      maxPartitions_ = 0L;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_PartitionOptions_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.v1.PartitionOptions getDefaultInstanceForType() {
      return com.google.spanner.v1.PartitionOptions.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.v1.PartitionOptions build() {
      com.google.spanner.v1.PartitionOptions result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.v1.PartitionOptions buildPartial() {
      com.google.spanner.v1.PartitionOptions result =
          new com.google.spanner.v1.PartitionOptions(this);
      if (bitField0_ != 0) {
        buildPartial0(result);
      }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.google.spanner.v1.PartitionOptions result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.partitionSizeBytes_ = partitionSizeBytes_;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.maxPartitions_ = maxPartitions_;
      }
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
      if (other instanceof com.google.spanner.v1.PartitionOptions) {
        return mergeFrom((com.google.spanner.v1.PartitionOptions) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.v1.PartitionOptions other) {
      if (other == com.google.spanner.v1.PartitionOptions.getDefaultInstance()) return this;
      if (other.getPartitionSizeBytes() != 0L) {
        setPartitionSizeBytes(other.getPartitionSizeBytes());
      }
      if (other.getMaxPartitions() != 0L) {
        setMaxPartitions(other.getMaxPartitions());
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
                partitionSizeBytes_ = input.readInt64();
                bitField0_ |= 0x00000001;
                break;
              } // case 8
            case 16:
              {
                maxPartitions_ = input.readInt64();
                bitField0_ |= 0x00000002;
                break;
              } // case 16
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

    private long partitionSizeBytes_;
    /**
     *
     *
     * <pre>
     * **Note:** This hint is currently ignored by PartitionQuery and
     * PartitionRead requests.
     *
     * The desired data size for each partition generated.  The default for this
     * option is currently 1 GiB.  This is only a hint. The actual size of each
     * partition may be smaller or larger than this size request.
     * </pre>
     *
     * <code>int64 partition_size_bytes = 1;</code>
     *
     * @return The partitionSizeBytes.
     */
    @java.lang.Override
    public long getPartitionSizeBytes() {
      return partitionSizeBytes_;
    }
    /**
     *
     *
     * <pre>
     * **Note:** This hint is currently ignored by PartitionQuery and
     * PartitionRead requests.
     *
     * The desired data size for each partition generated.  The default for this
     * option is currently 1 GiB.  This is only a hint. The actual size of each
     * partition may be smaller or larger than this size request.
     * </pre>
     *
     * <code>int64 partition_size_bytes = 1;</code>
     *
     * @param value The partitionSizeBytes to set.
     * @return This builder for chaining.
     */
    public Builder setPartitionSizeBytes(long value) {

      partitionSizeBytes_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * **Note:** This hint is currently ignored by PartitionQuery and
     * PartitionRead requests.
     *
     * The desired data size for each partition generated.  The default for this
     * option is currently 1 GiB.  This is only a hint. The actual size of each
     * partition may be smaller or larger than this size request.
     * </pre>
     *
     * <code>int64 partition_size_bytes = 1;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearPartitionSizeBytes() {
      bitField0_ = (bitField0_ & ~0x00000001);
      partitionSizeBytes_ = 0L;
      onChanged();
      return this;
    }

    private long maxPartitions_;
    /**
     *
     *
     * <pre>
     * **Note:** This hint is currently ignored by PartitionQuery and
     * PartitionRead requests.
     *
     * The desired maximum number of partitions to return.  For example, this may
     * be set to the number of workers available.  The default for this option
     * is currently 10,000. The maximum value is currently 200,000.  This is only
     * a hint.  The actual number of partitions returned may be smaller or larger
     * than this maximum count request.
     * </pre>
     *
     * <code>int64 max_partitions = 2;</code>
     *
     * @return The maxPartitions.
     */
    @java.lang.Override
    public long getMaxPartitions() {
      return maxPartitions_;
    }
    /**
     *
     *
     * <pre>
     * **Note:** This hint is currently ignored by PartitionQuery and
     * PartitionRead requests.
     *
     * The desired maximum number of partitions to return.  For example, this may
     * be set to the number of workers available.  The default for this option
     * is currently 10,000. The maximum value is currently 200,000.  This is only
     * a hint.  The actual number of partitions returned may be smaller or larger
     * than this maximum count request.
     * </pre>
     *
     * <code>int64 max_partitions = 2;</code>
     *
     * @param value The maxPartitions to set.
     * @return This builder for chaining.
     */
    public Builder setMaxPartitions(long value) {

      maxPartitions_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * **Note:** This hint is currently ignored by PartitionQuery and
     * PartitionRead requests.
     *
     * The desired maximum number of partitions to return.  For example, this may
     * be set to the number of workers available.  The default for this option
     * is currently 10,000. The maximum value is currently 200,000.  This is only
     * a hint.  The actual number of partitions returned may be smaller or larger
     * than this maximum count request.
     * </pre>
     *
     * <code>int64 max_partitions = 2;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearMaxPartitions() {
      bitField0_ = (bitField0_ & ~0x00000002);
      maxPartitions_ = 0L;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.v1.PartitionOptions)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.v1.PartitionOptions)
  private static final com.google.spanner.v1.PartitionOptions DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.v1.PartitionOptions();
  }

  public static com.google.spanner.v1.PartitionOptions getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PartitionOptions> PARSER =
      new com.google.protobuf.AbstractParser<PartitionOptions>() {
        @java.lang.Override
        public PartitionOptions parsePartialFrom(
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

  public static com.google.protobuf.Parser<PartitionOptions> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PartitionOptions> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.v1.PartitionOptions getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
