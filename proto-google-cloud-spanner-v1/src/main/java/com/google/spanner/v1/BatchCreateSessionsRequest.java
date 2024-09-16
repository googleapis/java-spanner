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
 * The request for
 * [BatchCreateSessions][google.spanner.v1.Spanner.BatchCreateSessions].
 * </pre>
 *
 * Protobuf type {@code google.spanner.v1.BatchCreateSessionsRequest}
 */
public final class BatchCreateSessionsRequest extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.v1.BatchCreateSessionsRequest)
    BatchCreateSessionsRequestOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use BatchCreateSessionsRequest.newBuilder() to construct.
  private BatchCreateSessionsRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private BatchCreateSessionsRequest() {
    database_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new BatchCreateSessionsRequest();
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_BatchCreateSessionsRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_BatchCreateSessionsRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.v1.BatchCreateSessionsRequest.class,
            com.google.spanner.v1.BatchCreateSessionsRequest.Builder.class);
  }

  private int bitField0_;
  public static final int DATABASE_FIELD_NUMBER = 1;

  @SuppressWarnings("serial")
  private volatile java.lang.Object database_ = "";
  /**
   *
   *
   * <pre>
   * Required. The database in which the new sessions are created.
   * </pre>
   *
   * <code>
   * string database = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The database.
   */
  @java.lang.Override
  public java.lang.String getDatabase() {
    java.lang.Object ref = database_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      database_ = s;
      return s;
    }
  }
  /**
   *
   *
   * <pre>
   * Required. The database in which the new sessions are created.
   * </pre>
   *
   * <code>
   * string database = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The bytes for database.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getDatabaseBytes() {
    java.lang.Object ref = database_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      database_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int SESSION_TEMPLATE_FIELD_NUMBER = 2;
  private com.google.spanner.v1.Session sessionTemplate_;
  /**
   *
   *
   * <pre>
   * Parameters to be applied to each created session.
   * </pre>
   *
   * <code>.google.spanner.v1.Session session_template = 2;</code>
   *
   * @return Whether the sessionTemplate field is set.
   */
  @java.lang.Override
  public boolean hasSessionTemplate() {
    return ((bitField0_ & 0x00000001) != 0);
  }
  /**
   *
   *
   * <pre>
   * Parameters to be applied to each created session.
   * </pre>
   *
   * <code>.google.spanner.v1.Session session_template = 2;</code>
   *
   * @return The sessionTemplate.
   */
  @java.lang.Override
  public com.google.spanner.v1.Session getSessionTemplate() {
    return sessionTemplate_ == null
        ? com.google.spanner.v1.Session.getDefaultInstance()
        : sessionTemplate_;
  }
  /**
   *
   *
   * <pre>
   * Parameters to be applied to each created session.
   * </pre>
   *
   * <code>.google.spanner.v1.Session session_template = 2;</code>
   */
  @java.lang.Override
  public com.google.spanner.v1.SessionOrBuilder getSessionTemplateOrBuilder() {
    return sessionTemplate_ == null
        ? com.google.spanner.v1.Session.getDefaultInstance()
        : sessionTemplate_;
  }

  public static final int SESSION_COUNT_FIELD_NUMBER = 3;
  private int sessionCount_ = 0;
  /**
   *
   *
   * <pre>
   * Required. The number of sessions to be created in this batch call.
   * The API may return fewer than the requested number of sessions. If a
   * specific number of sessions are desired, the client can make additional
   * calls to BatchCreateSessions (adjusting
   * [session_count][google.spanner.v1.BatchCreateSessionsRequest.session_count]
   * as necessary).
   * </pre>
   *
   * <code>int32 session_count = 3 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The sessionCount.
   */
  @java.lang.Override
  public int getSessionCount() {
    return sessionCount_;
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
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(database_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, database_);
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      output.writeMessage(2, getSessionTemplate());
    }
    if (sessionCount_ != 0) {
      output.writeInt32(3, sessionCount_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(database_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, database_);
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getSessionTemplate());
    }
    if (sessionCount_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeInt32Size(3, sessionCount_);
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
    if (!(obj instanceof com.google.spanner.v1.BatchCreateSessionsRequest)) {
      return super.equals(obj);
    }
    com.google.spanner.v1.BatchCreateSessionsRequest other =
        (com.google.spanner.v1.BatchCreateSessionsRequest) obj;

    if (!getDatabase().equals(other.getDatabase())) return false;
    if (hasSessionTemplate() != other.hasSessionTemplate()) return false;
    if (hasSessionTemplate()) {
      if (!getSessionTemplate().equals(other.getSessionTemplate())) return false;
    }
    if (getSessionCount() != other.getSessionCount()) return false;
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
    hash = (37 * hash) + DATABASE_FIELD_NUMBER;
    hash = (53 * hash) + getDatabase().hashCode();
    if (hasSessionTemplate()) {
      hash = (37 * hash) + SESSION_TEMPLATE_FIELD_NUMBER;
      hash = (53 * hash) + getSessionTemplate().hashCode();
    }
    hash = (37 * hash) + SESSION_COUNT_FIELD_NUMBER;
    hash = (53 * hash) + getSessionCount();
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest parseFrom(
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

  public static Builder newBuilder(com.google.spanner.v1.BatchCreateSessionsRequest prototype) {
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
   * The request for
   * [BatchCreateSessions][google.spanner.v1.Spanner.BatchCreateSessions].
   * </pre>
   *
   * Protobuf type {@code google.spanner.v1.BatchCreateSessionsRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.v1.BatchCreateSessionsRequest)
      com.google.spanner.v1.BatchCreateSessionsRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_BatchCreateSessionsRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_BatchCreateSessionsRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.v1.BatchCreateSessionsRequest.class,
              com.google.spanner.v1.BatchCreateSessionsRequest.Builder.class);
    }

    // Construct using com.google.spanner.v1.BatchCreateSessionsRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getSessionTemplateFieldBuilder();
      }
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      database_ = "";
      sessionTemplate_ = null;
      if (sessionTemplateBuilder_ != null) {
        sessionTemplateBuilder_.dispose();
        sessionTemplateBuilder_ = null;
      }
      sessionCount_ = 0;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_BatchCreateSessionsRequest_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.v1.BatchCreateSessionsRequest getDefaultInstanceForType() {
      return com.google.spanner.v1.BatchCreateSessionsRequest.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.v1.BatchCreateSessionsRequest build() {
      com.google.spanner.v1.BatchCreateSessionsRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.v1.BatchCreateSessionsRequest buildPartial() {
      com.google.spanner.v1.BatchCreateSessionsRequest result =
          new com.google.spanner.v1.BatchCreateSessionsRequest(this);
      if (bitField0_ != 0) {
        buildPartial0(result);
      }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.google.spanner.v1.BatchCreateSessionsRequest result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.database_ = database_;
      }
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.sessionTemplate_ =
            sessionTemplateBuilder_ == null ? sessionTemplate_ : sessionTemplateBuilder_.build();
        to_bitField0_ |= 0x00000001;
      }
      if (((from_bitField0_ & 0x00000004) != 0)) {
        result.sessionCount_ = sessionCount_;
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
      if (other instanceof com.google.spanner.v1.BatchCreateSessionsRequest) {
        return mergeFrom((com.google.spanner.v1.BatchCreateSessionsRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.v1.BatchCreateSessionsRequest other) {
      if (other == com.google.spanner.v1.BatchCreateSessionsRequest.getDefaultInstance())
        return this;
      if (!other.getDatabase().isEmpty()) {
        database_ = other.database_;
        bitField0_ |= 0x00000001;
        onChanged();
      }
      if (other.hasSessionTemplate()) {
        mergeSessionTemplate(other.getSessionTemplate());
      }
      if (other.getSessionCount() != 0) {
        setSessionCount(other.getSessionCount());
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
                database_ = input.readStringRequireUtf8();
                bitField0_ |= 0x00000001;
                break;
              } // case 10
            case 18:
              {
                input.readMessage(getSessionTemplateFieldBuilder().getBuilder(), extensionRegistry);
                bitField0_ |= 0x00000002;
                break;
              } // case 18
            case 24:
              {
                sessionCount_ = input.readInt32();
                bitField0_ |= 0x00000004;
                break;
              } // case 24
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

    private java.lang.Object database_ = "";
    /**
     *
     *
     * <pre>
     * Required. The database in which the new sessions are created.
     * </pre>
     *
     * <code>
     * string database = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @return The database.
     */
    public java.lang.String getDatabase() {
      java.lang.Object ref = database_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        database_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * Required. The database in which the new sessions are created.
     * </pre>
     *
     * <code>
     * string database = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @return The bytes for database.
     */
    public com.google.protobuf.ByteString getDatabaseBytes() {
      java.lang.Object ref = database_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        database_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * Required. The database in which the new sessions are created.
     * </pre>
     *
     * <code>
     * string database = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @param value The database to set.
     * @return This builder for chaining.
     */
    public Builder setDatabase(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }
      database_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Required. The database in which the new sessions are created.
     * </pre>
     *
     * <code>
     * string database = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @return This builder for chaining.
     */
    public Builder clearDatabase() {
      database_ = getDefaultInstance().getDatabase();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Required. The database in which the new sessions are created.
     * </pre>
     *
     * <code>
     * string database = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
     * </code>
     *
     * @param value The bytes for database to set.
     * @return This builder for chaining.
     */
    public Builder setDatabaseBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);
      database_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }

    private com.google.spanner.v1.Session sessionTemplate_;
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.spanner.v1.Session,
            com.google.spanner.v1.Session.Builder,
            com.google.spanner.v1.SessionOrBuilder>
        sessionTemplateBuilder_;
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     *
     * @return Whether the sessionTemplate field is set.
     */
    public boolean hasSessionTemplate() {
      return ((bitField0_ & 0x00000002) != 0);
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     *
     * @return The sessionTemplate.
     */
    public com.google.spanner.v1.Session getSessionTemplate() {
      if (sessionTemplateBuilder_ == null) {
        return sessionTemplate_ == null
            ? com.google.spanner.v1.Session.getDefaultInstance()
            : sessionTemplate_;
      } else {
        return sessionTemplateBuilder_.getMessage();
      }
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     */
    public Builder setSessionTemplate(com.google.spanner.v1.Session value) {
      if (sessionTemplateBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        sessionTemplate_ = value;
      } else {
        sessionTemplateBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     */
    public Builder setSessionTemplate(com.google.spanner.v1.Session.Builder builderForValue) {
      if (sessionTemplateBuilder_ == null) {
        sessionTemplate_ = builderForValue.build();
      } else {
        sessionTemplateBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     */
    public Builder mergeSessionTemplate(com.google.spanner.v1.Session value) {
      if (sessionTemplateBuilder_ == null) {
        if (((bitField0_ & 0x00000002) != 0)
            && sessionTemplate_ != null
            && sessionTemplate_ != com.google.spanner.v1.Session.getDefaultInstance()) {
          getSessionTemplateBuilder().mergeFrom(value);
        } else {
          sessionTemplate_ = value;
        }
      } else {
        sessionTemplateBuilder_.mergeFrom(value);
      }
      if (sessionTemplate_ != null) {
        bitField0_ |= 0x00000002;
        onChanged();
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     */
    public Builder clearSessionTemplate() {
      bitField0_ = (bitField0_ & ~0x00000002);
      sessionTemplate_ = null;
      if (sessionTemplateBuilder_ != null) {
        sessionTemplateBuilder_.dispose();
        sessionTemplateBuilder_ = null;
      }
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     */
    public com.google.spanner.v1.Session.Builder getSessionTemplateBuilder() {
      bitField0_ |= 0x00000002;
      onChanged();
      return getSessionTemplateFieldBuilder().getBuilder();
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     */
    public com.google.spanner.v1.SessionOrBuilder getSessionTemplateOrBuilder() {
      if (sessionTemplateBuilder_ != null) {
        return sessionTemplateBuilder_.getMessageOrBuilder();
      } else {
        return sessionTemplate_ == null
            ? com.google.spanner.v1.Session.getDefaultInstance()
            : sessionTemplate_;
      }
    }
    /**
     *
     *
     * <pre>
     * Parameters to be applied to each created session.
     * </pre>
     *
     * <code>.google.spanner.v1.Session session_template = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
            com.google.spanner.v1.Session,
            com.google.spanner.v1.Session.Builder,
            com.google.spanner.v1.SessionOrBuilder>
        getSessionTemplateFieldBuilder() {
      if (sessionTemplateBuilder_ == null) {
        sessionTemplateBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<
                com.google.spanner.v1.Session,
                com.google.spanner.v1.Session.Builder,
                com.google.spanner.v1.SessionOrBuilder>(
                getSessionTemplate(), getParentForChildren(), isClean());
        sessionTemplate_ = null;
      }
      return sessionTemplateBuilder_;
    }

    private int sessionCount_;
    /**
     *
     *
     * <pre>
     * Required. The number of sessions to be created in this batch call.
     * The API may return fewer than the requested number of sessions. If a
     * specific number of sessions are desired, the client can make additional
     * calls to BatchCreateSessions (adjusting
     * [session_count][google.spanner.v1.BatchCreateSessionsRequest.session_count]
     * as necessary).
     * </pre>
     *
     * <code>int32 session_count = 3 [(.google.api.field_behavior) = REQUIRED];</code>
     *
     * @return The sessionCount.
     */
    @java.lang.Override
    public int getSessionCount() {
      return sessionCount_;
    }
    /**
     *
     *
     * <pre>
     * Required. The number of sessions to be created in this batch call.
     * The API may return fewer than the requested number of sessions. If a
     * specific number of sessions are desired, the client can make additional
     * calls to BatchCreateSessions (adjusting
     * [session_count][google.spanner.v1.BatchCreateSessionsRequest.session_count]
     * as necessary).
     * </pre>
     *
     * <code>int32 session_count = 3 [(.google.api.field_behavior) = REQUIRED];</code>
     *
     * @param value The sessionCount to set.
     * @return This builder for chaining.
     */
    public Builder setSessionCount(int value) {

      sessionCount_ = value;
      bitField0_ |= 0x00000004;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * Required. The number of sessions to be created in this batch call.
     * The API may return fewer than the requested number of sessions. If a
     * specific number of sessions are desired, the client can make additional
     * calls to BatchCreateSessions (adjusting
     * [session_count][google.spanner.v1.BatchCreateSessionsRequest.session_count]
     * as necessary).
     * </pre>
     *
     * <code>int32 session_count = 3 [(.google.api.field_behavior) = REQUIRED];</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearSessionCount() {
      bitField0_ = (bitField0_ & ~0x00000004);
      sessionCount_ = 0;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.v1.BatchCreateSessionsRequest)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.v1.BatchCreateSessionsRequest)
  private static final com.google.spanner.v1.BatchCreateSessionsRequest DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.v1.BatchCreateSessionsRequest();
  }

  public static com.google.spanner.v1.BatchCreateSessionsRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<BatchCreateSessionsRequest> PARSER =
      new com.google.protobuf.AbstractParser<BatchCreateSessionsRequest>() {
        @java.lang.Override
        public BatchCreateSessionsRequest parsePartialFrom(
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

  public static com.google.protobuf.Parser<BatchCreateSessionsRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<BatchCreateSessionsRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.v1.BatchCreateSessionsRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
