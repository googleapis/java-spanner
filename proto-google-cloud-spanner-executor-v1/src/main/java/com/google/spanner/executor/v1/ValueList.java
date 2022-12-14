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
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

/**
 * <pre>
 * List of values.
 * </pre>
 *
 * Protobuf type {@code google.spanner.executor.v1.ValueList}
 */
public final class ValueList extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:google.spanner.executor.v1.ValueList)
    ValueListOrBuilder {
private static final long serialVersionUID = 0L;
  // Use ValueList.newBuilder() to construct.
  private ValueList(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private ValueList() {
    value_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new ValueList();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private ValueList(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
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
            if (!((mutable_bitField0_ & 0x00000001) != 0)) {
              value_ = new java.util.ArrayList<com.google.spanner.executor.v1.Value>();
              mutable_bitField0_ |= 0x00000001;
            }
            value_.add(
                input.readMessage(com.google.spanner.executor.v1.Value.parser(), extensionRegistry));
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
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        value_ = java.util.Collections.unmodifiableList(value_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_ValueList_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_ValueList_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.executor.v1.ValueList.class, com.google.spanner.executor.v1.ValueList.Builder.class);
  }

  public static final int VALUE_FIELD_NUMBER = 1;
  private java.util.List<com.google.spanner.executor.v1.Value> value_;
  /**
   * <pre>
   * Values contained in this ValueList.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
   */
  @java.lang.Override
  public java.util.List<com.google.spanner.executor.v1.Value> getValueList() {
    return value_;
  }
  /**
   * <pre>
   * Values contained in this ValueList.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
   */
  @java.lang.Override
  public java.util.List<? extends com.google.spanner.executor.v1.ValueOrBuilder> 
      getValueOrBuilderList() {
    return value_;
  }
  /**
   * <pre>
   * Values contained in this ValueList.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
   */
  @java.lang.Override
  public int getValueCount() {
    return value_.size();
  }
  /**
   * <pre>
   * Values contained in this ValueList.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
   */
  @java.lang.Override
  public com.google.spanner.executor.v1.Value getValue(int index) {
    return value_.get(index);
  }
  /**
   * <pre>
   * Values contained in this ValueList.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
   */
  @java.lang.Override
  public com.google.spanner.executor.v1.ValueOrBuilder getValueOrBuilder(
      int index) {
    return value_.get(index);
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
    for (int i = 0; i < value_.size(); i++) {
      output.writeMessage(1, value_.get(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    for (int i = 0; i < value_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, value_.get(i));
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
    if (!(obj instanceof com.google.spanner.executor.v1.ValueList)) {
      return super.equals(obj);
    }
    com.google.spanner.executor.v1.ValueList other = (com.google.spanner.executor.v1.ValueList) obj;

    if (!getValueList()
        .equals(other.getValueList())) return false;
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
    if (getValueCount() > 0) {
      hash = (37 * hash) + VALUE_FIELD_NUMBER;
      hash = (53 * hash) + getValueList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.executor.v1.ValueList parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.ValueList parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.ValueList parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.ValueList parseFrom(
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
  public static Builder newBuilder(com.google.spanner.executor.v1.ValueList prototype) {
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
   * List of values.
   * </pre>
   *
   * Protobuf type {@code google.spanner.executor.v1.ValueList}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:google.spanner.executor.v1.ValueList)
      com.google.spanner.executor.v1.ValueListOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_ValueList_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_ValueList_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.executor.v1.ValueList.class, com.google.spanner.executor.v1.ValueList.Builder.class);
    }

    // Construct using com.google.spanner.executor.v1.ValueList.newBuilder()
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
        getValueFieldBuilder();
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (valueBuilder_ == null) {
        value_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
      } else {
        valueBuilder_.clear();
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_ValueList_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.ValueList getDefaultInstanceForType() {
      return com.google.spanner.executor.v1.ValueList.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.ValueList build() {
      com.google.spanner.executor.v1.ValueList result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.ValueList buildPartial() {
      com.google.spanner.executor.v1.ValueList result = new com.google.spanner.executor.v1.ValueList(this);
      int from_bitField0_ = bitField0_;
      if (valueBuilder_ == null) {
        if (((bitField0_ & 0x00000001) != 0)) {
          value_ = java.util.Collections.unmodifiableList(value_);
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.value_ = value_;
      } else {
        result.value_ = valueBuilder_.build();
      }
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
      if (other instanceof com.google.spanner.executor.v1.ValueList) {
        return mergeFrom((com.google.spanner.executor.v1.ValueList)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.executor.v1.ValueList other) {
      if (other == com.google.spanner.executor.v1.ValueList.getDefaultInstance()) return this;
      if (valueBuilder_ == null) {
        if (!other.value_.isEmpty()) {
          if (value_.isEmpty()) {
            value_ = other.value_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureValueIsMutable();
            value_.addAll(other.value_);
          }
          onChanged();
        }
      } else {
        if (!other.value_.isEmpty()) {
          if (valueBuilder_.isEmpty()) {
            valueBuilder_.dispose();
            valueBuilder_ = null;
            value_ = other.value_;
            bitField0_ = (bitField0_ & ~0x00000001);
            valueBuilder_ = 
              com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ?
                 getValueFieldBuilder() : null;
          } else {
            valueBuilder_.addAllMessages(other.value_);
          }
        }
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
      com.google.spanner.executor.v1.ValueList parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.google.spanner.executor.v1.ValueList) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.util.List<com.google.spanner.executor.v1.Value> value_ =
      java.util.Collections.emptyList();
    private void ensureValueIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        value_ = new java.util.ArrayList<com.google.spanner.executor.v1.Value>(value_);
        bitField0_ |= 0x00000001;
       }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
        com.google.spanner.executor.v1.Value, com.google.spanner.executor.v1.Value.Builder, com.google.spanner.executor.v1.ValueOrBuilder> valueBuilder_;

    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public java.util.List<com.google.spanner.executor.v1.Value> getValueList() {
      if (valueBuilder_ == null) {
        return java.util.Collections.unmodifiableList(value_);
      } else {
        return valueBuilder_.getMessageList();
      }
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public int getValueCount() {
      if (valueBuilder_ == null) {
        return value_.size();
      } else {
        return valueBuilder_.getCount();
      }
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public com.google.spanner.executor.v1.Value getValue(int index) {
      if (valueBuilder_ == null) {
        return value_.get(index);
      } else {
        return valueBuilder_.getMessage(index);
      }
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder setValue(
        int index, com.google.spanner.executor.v1.Value value) {
      if (valueBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureValueIsMutable();
        value_.set(index, value);
        onChanged();
      } else {
        valueBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder setValue(
        int index, com.google.spanner.executor.v1.Value.Builder builderForValue) {
      if (valueBuilder_ == null) {
        ensureValueIsMutable();
        value_.set(index, builderForValue.build());
        onChanged();
      } else {
        valueBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder addValue(com.google.spanner.executor.v1.Value value) {
      if (valueBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureValueIsMutable();
        value_.add(value);
        onChanged();
      } else {
        valueBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder addValue(
        int index, com.google.spanner.executor.v1.Value value) {
      if (valueBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureValueIsMutable();
        value_.add(index, value);
        onChanged();
      } else {
        valueBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder addValue(
        com.google.spanner.executor.v1.Value.Builder builderForValue) {
      if (valueBuilder_ == null) {
        ensureValueIsMutable();
        value_.add(builderForValue.build());
        onChanged();
      } else {
        valueBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder addValue(
        int index, com.google.spanner.executor.v1.Value.Builder builderForValue) {
      if (valueBuilder_ == null) {
        ensureValueIsMutable();
        value_.add(index, builderForValue.build());
        onChanged();
      } else {
        valueBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder addAllValue(
        java.lang.Iterable<? extends com.google.spanner.executor.v1.Value> values) {
      if (valueBuilder_ == null) {
        ensureValueIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, value_);
        onChanged();
      } else {
        valueBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder clearValue() {
      if (valueBuilder_ == null) {
        value_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
      } else {
        valueBuilder_.clear();
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public Builder removeValue(int index) {
      if (valueBuilder_ == null) {
        ensureValueIsMutable();
        value_.remove(index);
        onChanged();
      } else {
        valueBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public com.google.spanner.executor.v1.Value.Builder getValueBuilder(
        int index) {
      return getValueFieldBuilder().getBuilder(index);
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public com.google.spanner.executor.v1.ValueOrBuilder getValueOrBuilder(
        int index) {
      if (valueBuilder_ == null) {
        return value_.get(index);  } else {
        return valueBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public java.util.List<? extends com.google.spanner.executor.v1.ValueOrBuilder> 
         getValueOrBuilderList() {
      if (valueBuilder_ != null) {
        return valueBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(value_);
      }
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public com.google.spanner.executor.v1.Value.Builder addValueBuilder() {
      return getValueFieldBuilder().addBuilder(
          com.google.spanner.executor.v1.Value.getDefaultInstance());
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public com.google.spanner.executor.v1.Value.Builder addValueBuilder(
        int index) {
      return getValueFieldBuilder().addBuilder(
          index, com.google.spanner.executor.v1.Value.getDefaultInstance());
    }
    /**
     * <pre>
     * Values contained in this ValueList.
     * </pre>
     *
     * <code>repeated .google.spanner.executor.v1.Value value = 1;</code>
     */
    public java.util.List<com.google.spanner.executor.v1.Value.Builder> 
         getValueBuilderList() {
      return getValueFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilderV3<
        com.google.spanner.executor.v1.Value, com.google.spanner.executor.v1.Value.Builder, com.google.spanner.executor.v1.ValueOrBuilder> 
        getValueFieldBuilder() {
      if (valueBuilder_ == null) {
        valueBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<
            com.google.spanner.executor.v1.Value, com.google.spanner.executor.v1.Value.Builder, com.google.spanner.executor.v1.ValueOrBuilder>(
                value_,
                ((bitField0_ & 0x00000001) != 0),
                getParentForChildren(),
                isClean());
        value_ = null;
      }
      return valueBuilder_;
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


    // @@protoc_insertion_point(builder_scope:google.spanner.executor.v1.ValueList)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.executor.v1.ValueList)
  private static final com.google.spanner.executor.v1.ValueList DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.google.spanner.executor.v1.ValueList();
  }

  public static com.google.spanner.executor.v1.ValueList getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ValueList>
      PARSER = new com.google.protobuf.AbstractParser<ValueList>() {
    @java.lang.Override
    public ValueList parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new ValueList(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<ValueList> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ValueList> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.executor.v1.ValueList getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

