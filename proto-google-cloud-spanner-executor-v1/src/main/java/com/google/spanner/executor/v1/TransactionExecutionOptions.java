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
// source: google/spanner/executor/v1/cloud_executor.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.executor.v1;

/**
 *
 *
 * <pre>
 * Options for executing the transaction.
 * </pre>
 *
 * Protobuf type {@code google.spanner.executor.v1.TransactionExecutionOptions}
 */
public final class TransactionExecutionOptions extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.executor.v1.TransactionExecutionOptions)
    TransactionExecutionOptionsOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use TransactionExecutionOptions.newBuilder() to construct.
  private TransactionExecutionOptions(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private TransactionExecutionOptions() {}

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new TransactionExecutionOptions();
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.executor.v1.CloudExecutorProto
        .internal_static_google_spanner_executor_v1_TransactionExecutionOptions_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.executor.v1.CloudExecutorProto
        .internal_static_google_spanner_executor_v1_TransactionExecutionOptions_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.executor.v1.TransactionExecutionOptions.class,
            com.google.spanner.executor.v1.TransactionExecutionOptions.Builder.class);
  }

  public static final int OPTIMISTIC_FIELD_NUMBER = 1;
  private boolean optimistic_ = false;
  /**
   *
   *
   * <pre>
   * Whether optimistic concurrency should be used to execute this transaction.
   * </pre>
   *
   * <code>bool optimistic = 1;</code>
   *
   * @return The optimistic.
   */
  @java.lang.Override
  public boolean getOptimistic() {
    return optimistic_;
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
    if (optimistic_ != false) {
      output.writeBool(1, optimistic_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (optimistic_ != false) {
      size += com.google.protobuf.CodedOutputStream.computeBoolSize(1, optimistic_);
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
    if (!(obj instanceof com.google.spanner.executor.v1.TransactionExecutionOptions)) {
      return super.equals(obj);
    }
    com.google.spanner.executor.v1.TransactionExecutionOptions other =
        (com.google.spanner.executor.v1.TransactionExecutionOptions) obj;

    if (getOptimistic() != other.getOptimistic()) return false;
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
    hash = (37 * hash) + OPTIMISTIC_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getOptimistic());
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions parseFrom(
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
      com.google.spanner.executor.v1.TransactionExecutionOptions prototype) {
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
   * Options for executing the transaction.
   * </pre>
   *
   * Protobuf type {@code google.spanner.executor.v1.TransactionExecutionOptions}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.executor.v1.TransactionExecutionOptions)
      com.google.spanner.executor.v1.TransactionExecutionOptionsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.executor.v1.CloudExecutorProto
          .internal_static_google_spanner_executor_v1_TransactionExecutionOptions_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.executor.v1.CloudExecutorProto
          .internal_static_google_spanner_executor_v1_TransactionExecutionOptions_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.executor.v1.TransactionExecutionOptions.class,
              com.google.spanner.executor.v1.TransactionExecutionOptions.Builder.class);
    }

    // Construct using com.google.spanner.executor.v1.TransactionExecutionOptions.newBuilder()
    private Builder() {}

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      optimistic_ = false;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.executor.v1.CloudExecutorProto
          .internal_static_google_spanner_executor_v1_TransactionExecutionOptions_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.TransactionExecutionOptions getDefaultInstanceForType() {
      return com.google.spanner.executor.v1.TransactionExecutionOptions.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.TransactionExecutionOptions build() {
      com.google.spanner.executor.v1.TransactionExecutionOptions result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.TransactionExecutionOptions buildPartial() {
      com.google.spanner.executor.v1.TransactionExecutionOptions result =
          new com.google.spanner.executor.v1.TransactionExecutionOptions(this);
      if (bitField0_ != 0) {
        buildPartial0(result);
      }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.google.spanner.executor.v1.TransactionExecutionOptions result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.optimistic_ = optimistic_;
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
      if (other instanceof com.google.spanner.executor.v1.TransactionExecutionOptions) {
        return mergeFrom((com.google.spanner.executor.v1.TransactionExecutionOptions) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.executor.v1.TransactionExecutionOptions other) {
      if (other == com.google.spanner.executor.v1.TransactionExecutionOptions.getDefaultInstance())
        return this;
      if (other.getOptimistic() != false) {
        setOptimistic(other.getOptimistic());
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
                optimistic_ = input.readBool();
                bitField0_ |= 0x00000001;
                break;
              } // case 8
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

    private boolean optimistic_;
    /**
     *
     *
     * <pre>
     * Whether optimistic concurrency should be used to execute this transaction.
     * </pre>
     *
     * <code>bool optimistic = 1;</code>
     *
     * @return The optimistic.
     */
    @java.lang.Override
    public boolean getOptimistic() {
      return optimistic_;
    }
    /**
     *
     *
     * <pre>
     * Whether optimistic concurrency should be used to execute this transaction.
     * </pre>
     *
     * <code>bool optimistic = 1;</code>
     *
     * @param value The optimistic to set.
     * @return This builder for chaining.
     */
    public Builder setOptimistic(boolean value) {

      optimistic_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Whether optimistic concurrency should be used to execute this transaction.
     * </pre>
     *
     * <code>bool optimistic = 1;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearOptimistic() {
      bitField0_ = (bitField0_ & ~0x00000001);
      optimistic_ = false;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.executor.v1.TransactionExecutionOptions)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.executor.v1.TransactionExecutionOptions)
  private static final com.google.spanner.executor.v1.TransactionExecutionOptions DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.executor.v1.TransactionExecutionOptions();
  }

  public static com.google.spanner.executor.v1.TransactionExecutionOptions getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<TransactionExecutionOptions> PARSER =
      new com.google.protobuf.AbstractParser<TransactionExecutionOptions>() {
        @java.lang.Override
        public TransactionExecutionOptions parsePartialFrom(
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

  public static com.google.protobuf.Parser<TransactionExecutionOptions> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<TransactionExecutionOptions> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.executor.v1.TransactionExecutionOptions getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
