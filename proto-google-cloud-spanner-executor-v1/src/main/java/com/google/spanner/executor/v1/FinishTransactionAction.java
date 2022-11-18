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
 * FinishTransactionAction defines an action of finishing a transaction.
 * </pre>
 *
 * Protobuf type {@code google.spanner.executor.v1.FinishTransactionAction}
 */
public final class FinishTransactionAction extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:google.spanner.executor.v1.FinishTransactionAction)
    FinishTransactionActionOrBuilder {
private static final long serialVersionUID = 0L;
  // Use FinishTransactionAction.newBuilder() to construct.
  private FinishTransactionAction(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private FinishTransactionAction() {
    mode_ = 0;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new FinishTransactionAction();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private FinishTransactionAction(
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
          case 8: {
            int rawValue = input.readEnum();

            mode_ = rawValue;
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
    return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_FinishTransactionAction_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_FinishTransactionAction_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.executor.v1.FinishTransactionAction.class, com.google.spanner.executor.v1.FinishTransactionAction.Builder.class);
  }

  /**
   * <pre>
   * Mode indicates how the transaction should be finished.
   * </pre>
   *
   * Protobuf enum {@code google.spanner.executor.v1.FinishTransactionAction.Mode}
   */
  public enum Mode
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <pre>
     * "MODE_UNSPECIFIED" is equivalent to "COMMIT".
     * </pre>
     *
     * <code>MODE_UNSPECIFIED = 0;</code>
     */
    MODE_UNSPECIFIED(0),
    /**
     * <pre>
     * Commit the transaction.
     * </pre>
     *
     * <code>COMMIT = 1;</code>
     */
    COMMIT(1),
    /**
     * <pre>
     * Drop the transaction without committing it.
     * </pre>
     *
     * <code>ABANDON = 2;</code>
     */
    ABANDON(2),
    UNRECOGNIZED(-1),
    ;

    /**
     * <pre>
     * "MODE_UNSPECIFIED" is equivalent to "COMMIT".
     * </pre>
     *
     * <code>MODE_UNSPECIFIED = 0;</code>
     */
    public static final int MODE_UNSPECIFIED_VALUE = 0;
    /**
     * <pre>
     * Commit the transaction.
     * </pre>
     *
     * <code>COMMIT = 1;</code>
     */
    public static final int COMMIT_VALUE = 1;
    /**
     * <pre>
     * Drop the transaction without committing it.
     * </pre>
     *
     * <code>ABANDON = 2;</code>
     */
    public static final int ABANDON_VALUE = 2;


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
    public static Mode valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static Mode forNumber(int value) {
      switch (value) {
        case 0: return MODE_UNSPECIFIED;
        case 1: return COMMIT;
        case 2: return ABANDON;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<Mode>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        Mode> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<Mode>() {
            public Mode findValueByNumber(int number) {
              return Mode.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
      }
      return getDescriptor().getValues().get(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return com.google.spanner.executor.v1.FinishTransactionAction.getDescriptor().getEnumTypes().get(0);
    }

    private static final Mode[] VALUES = values();

    public static Mode valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private Mode(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:google.spanner.executor.v1.FinishTransactionAction.Mode)
  }

  public static final int MODE_FIELD_NUMBER = 1;
  private int mode_;
  /**
   * <pre>
   * Defines how exactly the transaction should be completed, e.g. with
   * commit or abortion.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
   * @return The enum numeric value on the wire for mode.
   */
  @java.lang.Override public int getModeValue() {
    return mode_;
  }
  /**
   * <pre>
   * Defines how exactly the transaction should be completed, e.g. with
   * commit or abortion.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
   * @return The mode.
   */
  @java.lang.Override public com.google.spanner.executor.v1.FinishTransactionAction.Mode getMode() {
    @SuppressWarnings("deprecation")
    com.google.spanner.executor.v1.FinishTransactionAction.Mode result = com.google.spanner.executor.v1.FinishTransactionAction.Mode.valueOf(mode_);
    return result == null ? com.google.spanner.executor.v1.FinishTransactionAction.Mode.UNRECOGNIZED : result;
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
    if (mode_ != com.google.spanner.executor.v1.FinishTransactionAction.Mode.MODE_UNSPECIFIED.getNumber()) {
      output.writeEnum(1, mode_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (mode_ != com.google.spanner.executor.v1.FinishTransactionAction.Mode.MODE_UNSPECIFIED.getNumber()) {
      size += com.google.protobuf.CodedOutputStream
        .computeEnumSize(1, mode_);
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
    if (!(obj instanceof com.google.spanner.executor.v1.FinishTransactionAction)) {
      return super.equals(obj);
    }
    com.google.spanner.executor.v1.FinishTransactionAction other = (com.google.spanner.executor.v1.FinishTransactionAction) obj;

    if (mode_ != other.mode_) return false;
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
    hash = (37 * hash) + MODE_FIELD_NUMBER;
    hash = (53 * hash) + mode_;
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.FinishTransactionAction parseFrom(
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
  public static Builder newBuilder(com.google.spanner.executor.v1.FinishTransactionAction prototype) {
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
   * FinishTransactionAction defines an action of finishing a transaction.
   * </pre>
   *
   * Protobuf type {@code google.spanner.executor.v1.FinishTransactionAction}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:google.spanner.executor.v1.FinishTransactionAction)
      com.google.spanner.executor.v1.FinishTransactionActionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_FinishTransactionAction_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_FinishTransactionAction_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.executor.v1.FinishTransactionAction.class, com.google.spanner.executor.v1.FinishTransactionAction.Builder.class);
    }

    // Construct using com.google.spanner.executor.v1.FinishTransactionAction.newBuilder()
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
      mode_ = 0;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.google.spanner.executor.v1.CloudExecutorProto.internal_static_google_spanner_executor_v1_FinishTransactionAction_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.FinishTransactionAction getDefaultInstanceForType() {
      return com.google.spanner.executor.v1.FinishTransactionAction.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.FinishTransactionAction build() {
      com.google.spanner.executor.v1.FinishTransactionAction result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.FinishTransactionAction buildPartial() {
      com.google.spanner.executor.v1.FinishTransactionAction result = new com.google.spanner.executor.v1.FinishTransactionAction(this);
      result.mode_ = mode_;
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
      if (other instanceof com.google.spanner.executor.v1.FinishTransactionAction) {
        return mergeFrom((com.google.spanner.executor.v1.FinishTransactionAction)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.executor.v1.FinishTransactionAction other) {
      if (other == com.google.spanner.executor.v1.FinishTransactionAction.getDefaultInstance()) return this;
      if (other.mode_ != 0) {
        setModeValue(other.getModeValue());
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
      com.google.spanner.executor.v1.FinishTransactionAction parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.google.spanner.executor.v1.FinishTransactionAction) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int mode_ = 0;
    /**
     * <pre>
     * Defines how exactly the transaction should be completed, e.g. with
     * commit or abortion.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
     * @return The enum numeric value on the wire for mode.
     */
    @java.lang.Override public int getModeValue() {
      return mode_;
    }
    /**
     * <pre>
     * Defines how exactly the transaction should be completed, e.g. with
     * commit or abortion.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
     * @param value The enum numeric value on the wire for mode to set.
     * @return This builder for chaining.
     */
    public Builder setModeValue(int value) {
      
      mode_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Defines how exactly the transaction should be completed, e.g. with
     * commit or abortion.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
     * @return The mode.
     */
    @java.lang.Override
    public com.google.spanner.executor.v1.FinishTransactionAction.Mode getMode() {
      @SuppressWarnings("deprecation")
      com.google.spanner.executor.v1.FinishTransactionAction.Mode result = com.google.spanner.executor.v1.FinishTransactionAction.Mode.valueOf(mode_);
      return result == null ? com.google.spanner.executor.v1.FinishTransactionAction.Mode.UNRECOGNIZED : result;
    }
    /**
     * <pre>
     * Defines how exactly the transaction should be completed, e.g. with
     * commit or abortion.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
     * @param value The mode to set.
     * @return This builder for chaining.
     */
    public Builder setMode(com.google.spanner.executor.v1.FinishTransactionAction.Mode value) {
      if (value == null) {
        throw new NullPointerException();
      }
      
      mode_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Defines how exactly the transaction should be completed, e.g. with
     * commit or abortion.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.FinishTransactionAction.Mode mode = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearMode() {
      
      mode_ = 0;
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


    // @@protoc_insertion_point(builder_scope:google.spanner.executor.v1.FinishTransactionAction)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.executor.v1.FinishTransactionAction)
  private static final com.google.spanner.executor.v1.FinishTransactionAction DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.google.spanner.executor.v1.FinishTransactionAction();
  }

  public static com.google.spanner.executor.v1.FinishTransactionAction getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<FinishTransactionAction>
      PARSER = new com.google.protobuf.AbstractParser<FinishTransactionAction>() {
    @java.lang.Override
    public FinishTransactionAction parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new FinishTransactionAction(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<FinishTransactionAction> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<FinishTransactionAction> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.executor.v1.FinishTransactionAction getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

