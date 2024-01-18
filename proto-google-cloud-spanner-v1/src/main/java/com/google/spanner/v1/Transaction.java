/*
 * Copyright 2023 Google LLC
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
// source: google/spanner/v1/transaction.proto

package com.google.spanner.v1;

/**
 *
 *
 * <pre>
 * A transaction.
 * </pre>
 *
 * Protobuf type {@code google.spanner.v1.Transaction}
 */
public final class Transaction extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.v1.Transaction)
    TransactionOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use Transaction.newBuilder() to construct.
  private Transaction(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private Transaction() {
    id_ = com.google.protobuf.ByteString.EMPTY;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new Transaction();
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.v1.TransactionProto
        .internal_static_google_spanner_v1_Transaction_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.v1.TransactionProto
        .internal_static_google_spanner_v1_Transaction_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.v1.Transaction.class,
            com.google.spanner.v1.Transaction.Builder.class);
  }

  public static final int ID_FIELD_NUMBER = 1;
  private com.google.protobuf.ByteString id_ = com.google.protobuf.ByteString.EMPTY;
  /**
   *
   *
   * <pre>
   * `id` may be used to identify the transaction in subsequent
   * [Read][google.spanner.v1.Spanner.Read],
   * [ExecuteSql][google.spanner.v1.Spanner.ExecuteSql],
   * [Commit][google.spanner.v1.Spanner.Commit], or
   * [Rollback][google.spanner.v1.Spanner.Rollback] calls.
   *
   * Single-use read-only transactions do not have IDs, because
   * single-use transactions do not support multiple requests.
   * </pre>
   *
   * <code>bytes id = 1;</code>
   *
   * @return The id.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getId() {
    return id_;
  }

  public static final int READ_TIMESTAMP_FIELD_NUMBER = 2;
  private com.google.protobuf.Timestamp readTimestamp_;
  /**
   *
   *
   * <pre>
   * For snapshot read-only transactions, the read timestamp chosen
   * for the transaction. Not returned by default: see
   * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
   *
   * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
   * Example: `"2014-10-02T15:01:23.045123456Z"`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
   *
   * @return Whether the readTimestamp field is set.
   */
  @java.lang.Override
  public boolean hasReadTimestamp() {
    return readTimestamp_ != null;
  }
  /**
   *
   *
   * <pre>
   * For snapshot read-only transactions, the read timestamp chosen
   * for the transaction. Not returned by default: see
   * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
   *
   * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
   * Example: `"2014-10-02T15:01:23.045123456Z"`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
   *
   * @return The readTimestamp.
   */
  @java.lang.Override
  public com.google.protobuf.Timestamp getReadTimestamp() {
    return readTimestamp_ == null
        ? com.google.protobuf.Timestamp.getDefaultInstance()
        : readTimestamp_;
  }
  /**
   *
   *
   * <pre>
   * For snapshot read-only transactions, the read timestamp chosen
   * for the transaction. Not returned by default: see
   * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
   *
   * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
   * Example: `"2014-10-02T15:01:23.045123456Z"`.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
   */
  @java.lang.Override
  public com.google.protobuf.TimestampOrBuilder getReadTimestampOrBuilder() {
    return readTimestamp_ == null
        ? com.google.protobuf.Timestamp.getDefaultInstance()
        : readTimestamp_;
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
    if (!id_.isEmpty()) {
      output.writeBytes(1, id_);
    }
    if (readTimestamp_ != null) {
      output.writeMessage(2, getReadTimestamp());
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!id_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(1, id_);
    }
    if (readTimestamp_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getReadTimestamp());
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
    if (!(obj instanceof com.google.spanner.v1.Transaction)) {
      return super.equals(obj);
    }
    com.google.spanner.v1.Transaction other = (com.google.spanner.v1.Transaction) obj;

    if (!getId().equals(other.getId())) return false;
    if (hasReadTimestamp() != other.hasReadTimestamp()) return false;
    if (hasReadTimestamp()) {
      if (!getReadTimestamp().equals(other.getReadTimestamp())) return false;
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
    hash = (37 * hash) + ID_FIELD_NUMBER;
    hash = (53 * hash) + getId().hashCode();
    if (hasReadTimestamp()) {
      hash = (37 * hash) + READ_TIMESTAMP_FIELD_NUMBER;
      hash = (53 * hash) + getReadTimestamp().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.v1.Transaction parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.Transaction parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.Transaction parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.Transaction parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.Transaction parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.Transaction parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.Transaction parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.Transaction parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.Transaction parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.Transaction parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.Transaction parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.Transaction parseFrom(
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

  public static Builder newBuilder(com.google.spanner.v1.Transaction prototype) {
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
   * A transaction.
   * </pre>
   *
   * Protobuf type {@code google.spanner.v1.Transaction}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.v1.Transaction)
      com.google.spanner.v1.TransactionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.v1.TransactionProto
          .internal_static_google_spanner_v1_Transaction_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.v1.TransactionProto
          .internal_static_google_spanner_v1_Transaction_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.v1.Transaction.class,
              com.google.spanner.v1.Transaction.Builder.class);
    }

    // Construct using com.google.spanner.v1.Transaction.newBuilder()
    private Builder() {}

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      id_ = com.google.protobuf.ByteString.EMPTY;
      readTimestamp_ = null;
      if (readTimestampBuilder_ != null) {
        readTimestampBuilder_.dispose();
        readTimestampBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.v1.TransactionProto
          .internal_static_google_spanner_v1_Transaction_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.v1.Transaction getDefaultInstanceForType() {
      return com.google.spanner.v1.Transaction.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.v1.Transaction build() {
      com.google.spanner.v1.Transaction result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.v1.Transaction buildPartial() {
      com.google.spanner.v1.Transaction result = new com.google.spanner.v1.Transaction(this);
      if (bitField0_ != 0) {
        buildPartial0(result);
      }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.google.spanner.v1.Transaction result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.id_ = id_;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.readTimestamp_ =
            readTimestampBuilder_ == null ? readTimestamp_ : readTimestampBuilder_.build();
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
      if (other instanceof com.google.spanner.v1.Transaction) {
        return mergeFrom((com.google.spanner.v1.Transaction) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.v1.Transaction other) {
      if (other == com.google.spanner.v1.Transaction.getDefaultInstance()) return this;
      if (other.getId() != com.google.protobuf.ByteString.EMPTY) {
        setId(other.getId());
      }
      if (other.hasReadTimestamp()) {
        mergeReadTimestamp(other.getReadTimestamp());
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
            case 10:
              {
                id_ = input.readBytes();
                bitField0_ |= 0x00000001;
                break;
              } // case 10
            case 18:
              {
                input.readMessage(getReadTimestampFieldBuilder().getBuilder(), extensionRegistry);
                bitField0_ |= 0x00000002;
                break;
              } // case 18
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

    private com.google.protobuf.ByteString id_ = com.google.protobuf.ByteString.EMPTY;
    /**
     *
     *
     * <pre>
     * `id` may be used to identify the transaction in subsequent
     * [Read][google.spanner.v1.Spanner.Read],
     * [ExecuteSql][google.spanner.v1.Spanner.ExecuteSql],
     * [Commit][google.spanner.v1.Spanner.Commit], or
     * [Rollback][google.spanner.v1.Spanner.Rollback] calls.
     *
     * Single-use read-only transactions do not have IDs, because
     * single-use transactions do not support multiple requests.
     * </pre>
     *
     * <code>bytes id = 1;</code>
     *
     * @return The id.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getId() {
      return id_;
    }
    /**
     *
     *
     * <pre>
     * `id` may be used to identify the transaction in subsequent
     * [Read][google.spanner.v1.Spanner.Read],
     * [ExecuteSql][google.spanner.v1.Spanner.ExecuteSql],
     * [Commit][google.spanner.v1.Spanner.Commit], or
     * [Rollback][google.spanner.v1.Spanner.Rollback] calls.
     *
     * Single-use read-only transactions do not have IDs, because
     * single-use transactions do not support multiple requests.
     * </pre>
     *
     * <code>bytes id = 1;</code>
     *
     * @param value The id to set.
     * @return This builder for chaining.
     */
    public Builder setId(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      id_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * `id` may be used to identify the transaction in subsequent
     * [Read][google.spanner.v1.Spanner.Read],
     * [ExecuteSql][google.spanner.v1.Spanner.ExecuteSql],
     * [Commit][google.spanner.v1.Spanner.Commit], or
     * [Rollback][google.spanner.v1.Spanner.Rollback] calls.
     *
     * Single-use read-only transactions do not have IDs, because
     * single-use transactions do not support multiple requests.
     * </pre>
     *
     * <code>bytes id = 1;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearId() {
      bitField0_ = (bitField0_ & ~0x00000001);
      id_ = getDefaultInstance().getId();
      onChanged();
      return this;
    }

    private com.google.protobuf.Timestamp readTimestamp_;
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp,
            com.google.protobuf.Timestamp.Builder,
            com.google.protobuf.TimestampOrBuilder>
        readTimestampBuilder_;
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     *
     * @return Whether the readTimestamp field is set.
     */
    public boolean hasReadTimestamp() {
      return ((bitField0_ & 0x00000002) != 0);
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     *
     * @return The readTimestamp.
     */
    public com.google.protobuf.Timestamp getReadTimestamp() {
      if (readTimestampBuilder_ == null) {
        return readTimestamp_ == null
            ? com.google.protobuf.Timestamp.getDefaultInstance()
            : readTimestamp_;
      } else {
        return readTimestampBuilder_.getMessage();
      }
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     */
    public Builder setReadTimestamp(com.google.protobuf.Timestamp value) {
      if (readTimestampBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        readTimestamp_ = value;
      } else {
        readTimestampBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     */
    public Builder setReadTimestamp(com.google.protobuf.Timestamp.Builder builderForValue) {
      if (readTimestampBuilder_ == null) {
        readTimestamp_ = builderForValue.build();
      } else {
        readTimestampBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     */
    public Builder mergeReadTimestamp(com.google.protobuf.Timestamp value) {
      if (readTimestampBuilder_ == null) {
        if (((bitField0_ & 0x00000002) != 0)
            && readTimestamp_ != null
            && readTimestamp_ != com.google.protobuf.Timestamp.getDefaultInstance()) {
          getReadTimestampBuilder().mergeFrom(value);
        } else {
          readTimestamp_ = value;
        }
      } else {
        readTimestampBuilder_.mergeFrom(value);
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     */
    public Builder clearReadTimestamp() {
      bitField0_ = (bitField0_ & ~0x00000002);
      readTimestamp_ = null;
      if (readTimestampBuilder_ != null) {
        readTimestampBuilder_.dispose();
        readTimestampBuilder_ = null;
      }
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     */
    public com.google.protobuf.Timestamp.Builder getReadTimestampBuilder() {
      bitField0_ |= 0x00000002;
      onChanged();
      return getReadTimestampFieldBuilder().getBuilder();
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     */
    public com.google.protobuf.TimestampOrBuilder getReadTimestampOrBuilder() {
      if (readTimestampBuilder_ != null) {
        return readTimestampBuilder_.getMessageOrBuilder();
      } else {
        return readTimestamp_ == null
            ? com.google.protobuf.Timestamp.getDefaultInstance()
            : readTimestamp_;
      }
    }
    /**
     *
     *
     * <pre>
     * For snapshot read-only transactions, the read timestamp chosen
     * for the transaction. Not returned by default: see
     * [TransactionOptions.ReadOnly.return_read_timestamp][google.spanner.v1.TransactionOptions.ReadOnly.return_read_timestamp].
     *
     * A timestamp in RFC3339 UTC &#92;"Zulu&#92;" format, accurate to nanoseconds.
     * Example: `"2014-10-02T15:01:23.045123456Z"`.
     * </pre>
     *
     * <code>.google.protobuf.Timestamp read_timestamp = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp,
            com.google.protobuf.Timestamp.Builder,
            com.google.protobuf.TimestampOrBuilder>
        getReadTimestampFieldBuilder() {
      if (readTimestampBuilder_ == null) {
        readTimestampBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<
                com.google.protobuf.Timestamp,
                com.google.protobuf.Timestamp.Builder,
                com.google.protobuf.TimestampOrBuilder>(
                getReadTimestamp(), getParentForChildren(), isClean());
        readTimestamp_ = null;
      }
      return readTimestampBuilder_;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.v1.Transaction)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.v1.Transaction)
  private static final com.google.spanner.v1.Transaction DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.v1.Transaction();
  }

  public static com.google.spanner.v1.Transaction getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Transaction> PARSER =
      new com.google.protobuf.AbstractParser<Transaction>() {
        @java.lang.Override
        public Transaction parsePartialFrom(
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

  public static com.google.protobuf.Parser<Transaction> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Transaction> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.v1.Transaction getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
