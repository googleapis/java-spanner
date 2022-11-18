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
// source: google/spanner/executor/v1/executor_service.proto

package com.google.spanner.executor.v1;

/**
 * <pre>
 * Response from executor service.
 * </pre>
 *
 * Protobuf type {@code google.spanner.executor.v1.SpannerAsyncActionResponse}
 */
public final class SpannerAsyncActionResponse extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:google.spanner.executor.v1.SpannerAsyncActionResponse)
    SpannerAsyncActionResponseOrBuilder {
private static final long serialVersionUID = 0L;
  // Use SpannerAsyncActionResponse.newBuilder() to construct.
  private SpannerAsyncActionResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private SpannerAsyncActionResponse() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new SpannerAsyncActionResponse();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private SpannerAsyncActionResponse(
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

            actionId_ = input.readInt32();
            break;
          }
          case 18: {
            com.google.spanner.executor.v1.SpannerActionOutcome.Builder subBuilder = null;
            if (outcome_ != null) {
              subBuilder = outcome_.toBuilder();
            }
            outcome_ = input.readMessage(com.google.spanner.executor.v1.SpannerActionOutcome.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(outcome_);
              outcome_ = subBuilder.buildPartial();
            }

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
    return com.google.spanner.executor.v1.ExecutorServiceProto.internal_static_google_spanner_executor_v1_SpannerAsyncActionResponse_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.executor.v1.ExecutorServiceProto.internal_static_google_spanner_executor_v1_SpannerAsyncActionResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.executor.v1.SpannerAsyncActionResponse.class, com.google.spanner.executor.v1.SpannerAsyncActionResponse.Builder.class);
  }

  public static final int ACTION_ID_FIELD_NUMBER = 1;
  private int actionId_;
  /**
   * <pre>
   * Action id corresponds to the request.
   * </pre>
   *
   * <code>int32 action_id = 1;</code>
   * @return The actionId.
   */
  @java.lang.Override
  public int getActionId() {
    return actionId_;
  }

  public static final int OUTCOME_FIELD_NUMBER = 2;
  private com.google.spanner.executor.v1.SpannerActionOutcome outcome_;
  /**
   * <pre>
   * If action results are split into multiple responses, only the last response
   * can and should contain status.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
   * @return Whether the outcome field is set.
   */
  @java.lang.Override
  public boolean hasOutcome() {
    return outcome_ != null;
  }
  /**
   * <pre>
   * If action results are split into multiple responses, only the last response
   * can and should contain status.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
   * @return The outcome.
   */
  @java.lang.Override
  public com.google.spanner.executor.v1.SpannerActionOutcome getOutcome() {
    return outcome_ == null ? com.google.spanner.executor.v1.SpannerActionOutcome.getDefaultInstance() : outcome_;
  }
  /**
   * <pre>
   * If action results are split into multiple responses, only the last response
   * can and should contain status.
   * </pre>
   *
   * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
   */
  @java.lang.Override
  public com.google.spanner.executor.v1.SpannerActionOutcomeOrBuilder getOutcomeOrBuilder() {
    return getOutcome();
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
    if (actionId_ != 0) {
      output.writeInt32(1, actionId_);
    }
    if (outcome_ != null) {
      output.writeMessage(2, getOutcome());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (actionId_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(1, actionId_);
    }
    if (outcome_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, getOutcome());
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
    if (!(obj instanceof com.google.spanner.executor.v1.SpannerAsyncActionResponse)) {
      return super.equals(obj);
    }
    com.google.spanner.executor.v1.SpannerAsyncActionResponse other = (com.google.spanner.executor.v1.SpannerAsyncActionResponse) obj;

    if (getActionId()
        != other.getActionId()) return false;
    if (hasOutcome() != other.hasOutcome()) return false;
    if (hasOutcome()) {
      if (!getOutcome()
          .equals(other.getOutcome())) return false;
    }
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
    hash = (37 * hash) + ACTION_ID_FIELD_NUMBER;
    hash = (53 * hash) + getActionId();
    if (hasOutcome()) {
      hash = (37 * hash) + OUTCOME_FIELD_NUMBER;
      hash = (53 * hash) + getOutcome().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse parseFrom(
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
  public static Builder newBuilder(com.google.spanner.executor.v1.SpannerAsyncActionResponse prototype) {
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
   * Response from executor service.
   * </pre>
   *
   * Protobuf type {@code google.spanner.executor.v1.SpannerAsyncActionResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:google.spanner.executor.v1.SpannerAsyncActionResponse)
      com.google.spanner.executor.v1.SpannerAsyncActionResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.google.spanner.executor.v1.ExecutorServiceProto.internal_static_google_spanner_executor_v1_SpannerAsyncActionResponse_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.executor.v1.ExecutorServiceProto.internal_static_google_spanner_executor_v1_SpannerAsyncActionResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.executor.v1.SpannerAsyncActionResponse.class, com.google.spanner.executor.v1.SpannerAsyncActionResponse.Builder.class);
    }

    // Construct using com.google.spanner.executor.v1.SpannerAsyncActionResponse.newBuilder()
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
      actionId_ = 0;

      if (outcomeBuilder_ == null) {
        outcome_ = null;
      } else {
        outcome_ = null;
        outcomeBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.google.spanner.executor.v1.ExecutorServiceProto.internal_static_google_spanner_executor_v1_SpannerAsyncActionResponse_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.SpannerAsyncActionResponse getDefaultInstanceForType() {
      return com.google.spanner.executor.v1.SpannerAsyncActionResponse.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.SpannerAsyncActionResponse build() {
      com.google.spanner.executor.v1.SpannerAsyncActionResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.SpannerAsyncActionResponse buildPartial() {
      com.google.spanner.executor.v1.SpannerAsyncActionResponse result = new com.google.spanner.executor.v1.SpannerAsyncActionResponse(this);
      result.actionId_ = actionId_;
      if (outcomeBuilder_ == null) {
        result.outcome_ = outcome_;
      } else {
        result.outcome_ = outcomeBuilder_.build();
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
      if (other instanceof com.google.spanner.executor.v1.SpannerAsyncActionResponse) {
        return mergeFrom((com.google.spanner.executor.v1.SpannerAsyncActionResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.executor.v1.SpannerAsyncActionResponse other) {
      if (other == com.google.spanner.executor.v1.SpannerAsyncActionResponse.getDefaultInstance()) return this;
      if (other.getActionId() != 0) {
        setActionId(other.getActionId());
      }
      if (other.hasOutcome()) {
        mergeOutcome(other.getOutcome());
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
      com.google.spanner.executor.v1.SpannerAsyncActionResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.google.spanner.executor.v1.SpannerAsyncActionResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int actionId_ ;
    /**
     * <pre>
     * Action id corresponds to the request.
     * </pre>
     *
     * <code>int32 action_id = 1;</code>
     * @return The actionId.
     */
    @java.lang.Override
    public int getActionId() {
      return actionId_;
    }
    /**
     * <pre>
     * Action id corresponds to the request.
     * </pre>
     *
     * <code>int32 action_id = 1;</code>
     * @param value The actionId to set.
     * @return This builder for chaining.
     */
    public Builder setActionId(int value) {
      
      actionId_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Action id corresponds to the request.
     * </pre>
     *
     * <code>int32 action_id = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearActionId() {
      
      actionId_ = 0;
      onChanged();
      return this;
    }

    private com.google.spanner.executor.v1.SpannerActionOutcome outcome_;
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.spanner.executor.v1.SpannerActionOutcome, com.google.spanner.executor.v1.SpannerActionOutcome.Builder, com.google.spanner.executor.v1.SpannerActionOutcomeOrBuilder> outcomeBuilder_;
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     * @return Whether the outcome field is set.
     */
    public boolean hasOutcome() {
      return outcomeBuilder_ != null || outcome_ != null;
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     * @return The outcome.
     */
    public com.google.spanner.executor.v1.SpannerActionOutcome getOutcome() {
      if (outcomeBuilder_ == null) {
        return outcome_ == null ? com.google.spanner.executor.v1.SpannerActionOutcome.getDefaultInstance() : outcome_;
      } else {
        return outcomeBuilder_.getMessage();
      }
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     */
    public Builder setOutcome(com.google.spanner.executor.v1.SpannerActionOutcome value) {
      if (outcomeBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        outcome_ = value;
        onChanged();
      } else {
        outcomeBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     */
    public Builder setOutcome(
        com.google.spanner.executor.v1.SpannerActionOutcome.Builder builderForValue) {
      if (outcomeBuilder_ == null) {
        outcome_ = builderForValue.build();
        onChanged();
      } else {
        outcomeBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     */
    public Builder mergeOutcome(com.google.spanner.executor.v1.SpannerActionOutcome value) {
      if (outcomeBuilder_ == null) {
        if (outcome_ != null) {
          outcome_ =
            com.google.spanner.executor.v1.SpannerActionOutcome.newBuilder(outcome_).mergeFrom(value).buildPartial();
        } else {
          outcome_ = value;
        }
        onChanged();
      } else {
        outcomeBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     */
    public Builder clearOutcome() {
      if (outcomeBuilder_ == null) {
        outcome_ = null;
        onChanged();
      } else {
        outcome_ = null;
        outcomeBuilder_ = null;
      }

      return this;
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     */
    public com.google.spanner.executor.v1.SpannerActionOutcome.Builder getOutcomeBuilder() {
      
      onChanged();
      return getOutcomeFieldBuilder().getBuilder();
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     */
    public com.google.spanner.executor.v1.SpannerActionOutcomeOrBuilder getOutcomeOrBuilder() {
      if (outcomeBuilder_ != null) {
        return outcomeBuilder_.getMessageOrBuilder();
      } else {
        return outcome_ == null ?
            com.google.spanner.executor.v1.SpannerActionOutcome.getDefaultInstance() : outcome_;
      }
    }
    /**
     * <pre>
     * If action results are split into multiple responses, only the last response
     * can and should contain status.
     * </pre>
     *
     * <code>.google.spanner.executor.v1.SpannerActionOutcome outcome = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.spanner.executor.v1.SpannerActionOutcome, com.google.spanner.executor.v1.SpannerActionOutcome.Builder, com.google.spanner.executor.v1.SpannerActionOutcomeOrBuilder> 
        getOutcomeFieldBuilder() {
      if (outcomeBuilder_ == null) {
        outcomeBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.google.spanner.executor.v1.SpannerActionOutcome, com.google.spanner.executor.v1.SpannerActionOutcome.Builder, com.google.spanner.executor.v1.SpannerActionOutcomeOrBuilder>(
                getOutcome(),
                getParentForChildren(),
                isClean());
        outcome_ = null;
      }
      return outcomeBuilder_;
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


    // @@protoc_insertion_point(builder_scope:google.spanner.executor.v1.SpannerAsyncActionResponse)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.executor.v1.SpannerAsyncActionResponse)
  private static final com.google.spanner.executor.v1.SpannerAsyncActionResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.google.spanner.executor.v1.SpannerAsyncActionResponse();
  }

  public static com.google.spanner.executor.v1.SpannerAsyncActionResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SpannerAsyncActionResponse>
      PARSER = new com.google.protobuf.AbstractParser<SpannerAsyncActionResponse>() {
    @java.lang.Override
    public SpannerAsyncActionResponse parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new SpannerAsyncActionResponse(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<SpannerAsyncActionResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<SpannerAsyncActionResponse> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.executor.v1.SpannerAsyncActionResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

