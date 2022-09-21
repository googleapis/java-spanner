/*
 * Copyright 2020 Google LLC
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

package com.google.spanner.v1;

/**
 *
 *
 * <pre>
 * The response for [ListSessions][google.spanner.v1.Spanner.ListSessions].
 * </pre>
 *
 * Protobuf type {@code google.spanner.v1.ListSessionsResponse}
 */
public final class ListSessionsResponse extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.v1.ListSessionsResponse)
    ListSessionsResponseOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use ListSessionsResponse.newBuilder() to construct.
  private ListSessionsResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ListSessionsResponse() {
    sessions_ = java.util.Collections.emptyList();
    nextPageToken_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new ListSessionsResponse();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private ListSessionsResponse(
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
          case 10:
            {
              if (!((mutable_bitField0_ & 0x00000001) != 0)) {
                sessions_ = new java.util.ArrayList<com.google.spanner.v1.Session>();
                mutable_bitField0_ |= 0x00000001;
              }
              sessions_.add(
                  input.readMessage(com.google.spanner.v1.Session.parser(), extensionRegistry));
              break;
            }
          case 18:
            {
              java.lang.String s = input.readStringRequireUtf8();

              nextPageToken_ = s;
              break;
            }
          default:
            {
              if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        sessions_ = java.util.Collections.unmodifiableList(sessions_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_ListSessionsResponse_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.v1.SpannerProto
        .internal_static_google_spanner_v1_ListSessionsResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.v1.ListSessionsResponse.class,
            com.google.spanner.v1.ListSessionsResponse.Builder.class);
  }

  public static final int SESSIONS_FIELD_NUMBER = 1;
  private java.util.List<com.google.spanner.v1.Session> sessions_;
  /**
   *
   *
   * <pre>
   * The list of requested sessions.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
   */
  @java.lang.Override
  public java.util.List<com.google.spanner.v1.Session> getSessionsList() {
    return sessions_;
  }
  /**
   *
   *
   * <pre>
   * The list of requested sessions.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
   */
  @java.lang.Override
  public java.util.List<? extends com.google.spanner.v1.SessionOrBuilder>
      getSessionsOrBuilderList() {
    return sessions_;
  }
  /**
   *
   *
   * <pre>
   * The list of requested sessions.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
   */
  @java.lang.Override
  public int getSessionsCount() {
    return sessions_.size();
  }
  /**
   *
   *
   * <pre>
   * The list of requested sessions.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
   */
  @java.lang.Override
  public com.google.spanner.v1.Session getSessions(int index) {
    return sessions_.get(index);
  }
  /**
   *
   *
   * <pre>
   * The list of requested sessions.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
   */
  @java.lang.Override
  public com.google.spanner.v1.SessionOrBuilder getSessionsOrBuilder(int index) {
    return sessions_.get(index);
  }

  public static final int NEXT_PAGE_TOKEN_FIELD_NUMBER = 2;
  private volatile java.lang.Object nextPageToken_;
  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListSessions][google.spanner.v1.Spanner.ListSessions] call to fetch more of the matching
   * sessions.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The nextPageToken.
   */
  @java.lang.Override
  public java.lang.String getNextPageToken() {
    java.lang.Object ref = nextPageToken_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      nextPageToken_ = s;
      return s;
    }
  }
  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListSessions][google.spanner.v1.Spanner.ListSessions] call to fetch more of the matching
   * sessions.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The bytes for nextPageToken.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getNextPageTokenBytes() {
    java.lang.Object ref = nextPageToken_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      nextPageToken_ = b;
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
  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    for (int i = 0; i < sessions_.size(); i++) {
      output.writeMessage(1, sessions_.get(i));
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(nextPageToken_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, nextPageToken_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    for (int i = 0; i < sessions_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, sessions_.get(i));
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(nextPageToken_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, nextPageToken_);
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
    if (!(obj instanceof com.google.spanner.v1.ListSessionsResponse)) {
      return super.equals(obj);
    }
    com.google.spanner.v1.ListSessionsResponse other =
        (com.google.spanner.v1.ListSessionsResponse) obj;

    if (!getSessionsList().equals(other.getSessionsList())) return false;
    if (!getNextPageToken().equals(other.getNextPageToken())) return false;
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
    if (getSessionsCount() > 0) {
      hash = (37 * hash) + SESSIONS_FIELD_NUMBER;
      hash = (53 * hash) + getSessionsList().hashCode();
    }
    hash = (37 * hash) + NEXT_PAGE_TOKEN_FIELD_NUMBER;
    hash = (53 * hash) + getNextPageToken().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.ListSessionsResponse parseFrom(
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

  public static Builder newBuilder(com.google.spanner.v1.ListSessionsResponse prototype) {
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
   * The response for [ListSessions][google.spanner.v1.Spanner.ListSessions].
   * </pre>
   *
   * Protobuf type {@code google.spanner.v1.ListSessionsResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.v1.ListSessionsResponse)
      com.google.spanner.v1.ListSessionsResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_ListSessionsResponse_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_ListSessionsResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.v1.ListSessionsResponse.class,
              com.google.spanner.v1.ListSessionsResponse.Builder.class);
    }

    // Construct using com.google.spanner.v1.ListSessionsResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getSessionsFieldBuilder();
      }
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (sessionsBuilder_ == null) {
        sessions_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
      } else {
        sessionsBuilder_.clear();
      }
      nextPageToken_ = "";

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.v1.SpannerProto
          .internal_static_google_spanner_v1_ListSessionsResponse_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.v1.ListSessionsResponse getDefaultInstanceForType() {
      return com.google.spanner.v1.ListSessionsResponse.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.v1.ListSessionsResponse build() {
      com.google.spanner.v1.ListSessionsResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.v1.ListSessionsResponse buildPartial() {
      com.google.spanner.v1.ListSessionsResponse result =
          new com.google.spanner.v1.ListSessionsResponse(this);
      int from_bitField0_ = bitField0_;
      if (sessionsBuilder_ == null) {
        if (((bitField0_ & 0x00000001) != 0)) {
          sessions_ = java.util.Collections.unmodifiableList(sessions_);
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.sessions_ = sessions_;
      } else {
        result.sessions_ = sessionsBuilder_.build();
      }
      result.nextPageToken_ = nextPageToken_;
      onBuilt();
      return result;
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
      if (other instanceof com.google.spanner.v1.ListSessionsResponse) {
        return mergeFrom((com.google.spanner.v1.ListSessionsResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.v1.ListSessionsResponse other) {
      if (other == com.google.spanner.v1.ListSessionsResponse.getDefaultInstance()) return this;
      if (sessionsBuilder_ == null) {
        if (!other.sessions_.isEmpty()) {
          if (sessions_.isEmpty()) {
            sessions_ = other.sessions_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureSessionsIsMutable();
            sessions_.addAll(other.sessions_);
          }
          onChanged();
        }
      } else {
        if (!other.sessions_.isEmpty()) {
          if (sessionsBuilder_.isEmpty()) {
            sessionsBuilder_.dispose();
            sessionsBuilder_ = null;
            sessions_ = other.sessions_;
            bitField0_ = (bitField0_ & ~0x00000001);
            sessionsBuilder_ =
                com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                    ? getSessionsFieldBuilder()
                    : null;
          } else {
            sessionsBuilder_.addAllMessages(other.sessions_);
          }
        }
      }
      if (!other.getNextPageToken().isEmpty()) {
        nextPageToken_ = other.nextPageToken_;
        onChanged();
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
      com.google.spanner.v1.ListSessionsResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.google.spanner.v1.ListSessionsResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int bitField0_;

    private java.util.List<com.google.spanner.v1.Session> sessions_ =
        java.util.Collections.emptyList();

    private void ensureSessionsIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        sessions_ = new java.util.ArrayList<com.google.spanner.v1.Session>(sessions_);
        bitField0_ |= 0x00000001;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
            com.google.spanner.v1.Session,
            com.google.spanner.v1.Session.Builder,
            com.google.spanner.v1.SessionOrBuilder>
        sessionsBuilder_;

    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public java.util.List<com.google.spanner.v1.Session> getSessionsList() {
      if (sessionsBuilder_ == null) {
        return java.util.Collections.unmodifiableList(sessions_);
      } else {
        return sessionsBuilder_.getMessageList();
      }
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public int getSessionsCount() {
      if (sessionsBuilder_ == null) {
        return sessions_.size();
      } else {
        return sessionsBuilder_.getCount();
      }
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public com.google.spanner.v1.Session getSessions(int index) {
      if (sessionsBuilder_ == null) {
        return sessions_.get(index);
      } else {
        return sessionsBuilder_.getMessage(index);
      }
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder setSessions(int index, com.google.spanner.v1.Session value) {
      if (sessionsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureSessionsIsMutable();
        sessions_.set(index, value);
        onChanged();
      } else {
        sessionsBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder setSessions(int index, com.google.spanner.v1.Session.Builder builderForValue) {
      if (sessionsBuilder_ == null) {
        ensureSessionsIsMutable();
        sessions_.set(index, builderForValue.build());
        onChanged();
      } else {
        sessionsBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder addSessions(com.google.spanner.v1.Session value) {
      if (sessionsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureSessionsIsMutable();
        sessions_.add(value);
        onChanged();
      } else {
        sessionsBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder addSessions(int index, com.google.spanner.v1.Session value) {
      if (sessionsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureSessionsIsMutable();
        sessions_.add(index, value);
        onChanged();
      } else {
        sessionsBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder addSessions(com.google.spanner.v1.Session.Builder builderForValue) {
      if (sessionsBuilder_ == null) {
        ensureSessionsIsMutable();
        sessions_.add(builderForValue.build());
        onChanged();
      } else {
        sessionsBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder addSessions(int index, com.google.spanner.v1.Session.Builder builderForValue) {
      if (sessionsBuilder_ == null) {
        ensureSessionsIsMutable();
        sessions_.add(index, builderForValue.build());
        onChanged();
      } else {
        sessionsBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder addAllSessions(
        java.lang.Iterable<? extends com.google.spanner.v1.Session> values) {
      if (sessionsBuilder_ == null) {
        ensureSessionsIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, sessions_);
        onChanged();
      } else {
        sessionsBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder clearSessions() {
      if (sessionsBuilder_ == null) {
        sessions_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
      } else {
        sessionsBuilder_.clear();
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public Builder removeSessions(int index) {
      if (sessionsBuilder_ == null) {
        ensureSessionsIsMutable();
        sessions_.remove(index);
        onChanged();
      } else {
        sessionsBuilder_.remove(index);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public com.google.spanner.v1.Session.Builder getSessionsBuilder(int index) {
      return getSessionsFieldBuilder().getBuilder(index);
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public com.google.spanner.v1.SessionOrBuilder getSessionsOrBuilder(int index) {
      if (sessionsBuilder_ == null) {
        return sessions_.get(index);
      } else {
        return sessionsBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public java.util.List<? extends com.google.spanner.v1.SessionOrBuilder>
        getSessionsOrBuilderList() {
      if (sessionsBuilder_ != null) {
        return sessionsBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(sessions_);
      }
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public com.google.spanner.v1.Session.Builder addSessionsBuilder() {
      return getSessionsFieldBuilder()
          .addBuilder(com.google.spanner.v1.Session.getDefaultInstance());
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public com.google.spanner.v1.Session.Builder addSessionsBuilder(int index) {
      return getSessionsFieldBuilder()
          .addBuilder(index, com.google.spanner.v1.Session.getDefaultInstance());
    }
    /**
     *
     *
     * <pre>
     * The list of requested sessions.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.Session sessions = 1;</code>
     */
    public java.util.List<com.google.spanner.v1.Session.Builder> getSessionsBuilderList() {
      return getSessionsFieldBuilder().getBuilderList();
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
            com.google.spanner.v1.Session,
            com.google.spanner.v1.Session.Builder,
            com.google.spanner.v1.SessionOrBuilder>
        getSessionsFieldBuilder() {
      if (sessionsBuilder_ == null) {
        sessionsBuilder_ =
            new com.google.protobuf.RepeatedFieldBuilderV3<
                com.google.spanner.v1.Session,
                com.google.spanner.v1.Session.Builder,
                com.google.spanner.v1.SessionOrBuilder>(
                sessions_, ((bitField0_ & 0x00000001) != 0), getParentForChildren(), isClean());
        sessions_ = null;
      }
      return sessionsBuilder_;
    }

    private java.lang.Object nextPageToken_ = "";
    /**
     *
     *
     * <pre>
     * `next_page_token` can be sent in a subsequent
     * [ListSessions][google.spanner.v1.Spanner.ListSessions] call to fetch more of the matching
     * sessions.
     * </pre>
     *
     * <code>string next_page_token = 2;</code>
     *
     * @return The nextPageToken.
     */
    public java.lang.String getNextPageToken() {
      java.lang.Object ref = nextPageToken_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        nextPageToken_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * `next_page_token` can be sent in a subsequent
     * [ListSessions][google.spanner.v1.Spanner.ListSessions] call to fetch more of the matching
     * sessions.
     * </pre>
     *
     * <code>string next_page_token = 2;</code>
     *
     * @return The bytes for nextPageToken.
     */
    public com.google.protobuf.ByteString getNextPageTokenBytes() {
      java.lang.Object ref = nextPageToken_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        nextPageToken_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * `next_page_token` can be sent in a subsequent
     * [ListSessions][google.spanner.v1.Spanner.ListSessions] call to fetch more of the matching
     * sessions.
     * </pre>
     *
     * <code>string next_page_token = 2;</code>
     *
     * @param value The nextPageToken to set.
     * @return This builder for chaining.
     */
    public Builder setNextPageToken(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      nextPageToken_ = value;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * `next_page_token` can be sent in a subsequent
     * [ListSessions][google.spanner.v1.Spanner.ListSessions] call to fetch more of the matching
     * sessions.
     * </pre>
     *
     * <code>string next_page_token = 2;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearNextPageToken() {

      nextPageToken_ = getDefaultInstance().getNextPageToken();
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * `next_page_token` can be sent in a subsequent
     * [ListSessions][google.spanner.v1.Spanner.ListSessions] call to fetch more of the matching
     * sessions.
     * </pre>
     *
     * <code>string next_page_token = 2;</code>
     *
     * @param value The bytes for nextPageToken to set.
     * @return This builder for chaining.
     */
    public Builder setNextPageTokenBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      nextPageToken_ = value;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.v1.ListSessionsResponse)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.v1.ListSessionsResponse)
  private static final com.google.spanner.v1.ListSessionsResponse DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.v1.ListSessionsResponse();
  }

  public static com.google.spanner.v1.ListSessionsResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ListSessionsResponse> PARSER =
      new com.google.protobuf.AbstractParser<ListSessionsResponse>() {
        @java.lang.Override
        public ListSessionsResponse parsePartialFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ListSessionsResponse(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ListSessionsResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ListSessionsResponse> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.v1.ListSessionsResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
