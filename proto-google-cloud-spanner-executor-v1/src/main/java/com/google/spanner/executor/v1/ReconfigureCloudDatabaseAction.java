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

// Protobuf Java Version: 3.25.2
package com.google.spanner.executor.v1;

/**
 *
 *
 * <pre>
 * Action that reconfigures a Cloud Spanner database.
 * </pre>
 *
 * Protobuf type {@code google.spanner.executor.v1.ReconfigureCloudDatabaseAction}
 */
public final class ReconfigureCloudDatabaseAction extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.executor.v1.ReconfigureCloudDatabaseAction)
    ReconfigureCloudDatabaseActionOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use ReconfigureCloudDatabaseAction.newBuilder() to construct.
  private ReconfigureCloudDatabaseAction(
      com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ReconfigureCloudDatabaseAction() {
    databaseUri_ = "";
    servingLocations_ = com.google.protobuf.LazyStringArrayList.emptyList();
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new ReconfigureCloudDatabaseAction();
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.executor.v1.CloudExecutorProto
        .internal_static_google_spanner_executor_v1_ReconfigureCloudDatabaseAction_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.executor.v1.CloudExecutorProto
        .internal_static_google_spanner_executor_v1_ReconfigureCloudDatabaseAction_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction.class,
            com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction.Builder.class);
  }

  private int bitField0_;
  public static final int DATABASE_URI_FIELD_NUMBER = 1;

  @SuppressWarnings("serial")
  private volatile java.lang.Object databaseUri_ = "";
  /**
   *
   *
   * <pre>
   * The fully qualified uri of the database to be reconfigured.
   * </pre>
   *
   * <code>optional string database_uri = 1;</code>
   *
   * @return Whether the databaseUri field is set.
   */
  @java.lang.Override
  public boolean hasDatabaseUri() {
    return ((bitField0_ & 0x00000001) != 0);
  }
  /**
   *
   *
   * <pre>
   * The fully qualified uri of the database to be reconfigured.
   * </pre>
   *
   * <code>optional string database_uri = 1;</code>
   *
   * @return The databaseUri.
   */
  @java.lang.Override
  public java.lang.String getDatabaseUri() {
    java.lang.Object ref = databaseUri_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      databaseUri_ = s;
      return s;
    }
  }
  /**
   *
   *
   * <pre>
   * The fully qualified uri of the database to be reconfigured.
   * </pre>
   *
   * <code>optional string database_uri = 1;</code>
   *
   * @return The bytes for databaseUri.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getDatabaseUriBytes() {
    java.lang.Object ref = databaseUri_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      databaseUri_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int SERVING_LOCATIONS_FIELD_NUMBER = 2;

  @SuppressWarnings("serial")
  private com.google.protobuf.LazyStringArrayList servingLocations_ =
      com.google.protobuf.LazyStringArrayList.emptyList();
  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @return A list containing the servingLocations.
   */
  public com.google.protobuf.ProtocolStringList getServingLocationsList() {
    return servingLocations_;
  }
  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @return The count of servingLocations.
   */
  public int getServingLocationsCount() {
    return servingLocations_.size();
  }
  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @param index The index of the element to return.
   * @return The servingLocations at the given index.
   */
  public java.lang.String getServingLocations(int index) {
    return servingLocations_.get(index);
  }
  /**
   *
   *
   * <pre>
   * The locations of the serving regions, e.g. "asia-south1".
   * </pre>
   *
   * <code>repeated string serving_locations = 2;</code>
   *
   * @param index The index of the value to return.
   * @return The bytes of the servingLocations at the given index.
   */
  public com.google.protobuf.ByteString getServingLocationsBytes(int index) {
    return servingLocations_.getByteString(index);
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
    if (((bitField0_ & 0x00000001) != 0)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, databaseUri_);
    }
    for (int i = 0; i < servingLocations_.size(); i++) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, servingLocations_.getRaw(i));
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (((bitField0_ & 0x00000001) != 0)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, databaseUri_);
    }
    {
      int dataSize = 0;
      for (int i = 0; i < servingLocations_.size(); i++) {
        dataSize += computeStringSizeNoTag(servingLocations_.getRaw(i));
      }
      size += dataSize;
      size += 1 * getServingLocationsList().size();
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
    if (!(obj instanceof com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction)) {
      return super.equals(obj);
    }
    com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction other =
        (com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction) obj;

    if (hasDatabaseUri() != other.hasDatabaseUri()) return false;
    if (hasDatabaseUri()) {
      if (!getDatabaseUri().equals(other.getDatabaseUri())) return false;
    }
    if (!getServingLocationsList().equals(other.getServingLocationsList())) return false;
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
    if (hasDatabaseUri()) {
      hash = (37 * hash) + DATABASE_URI_FIELD_NUMBER;
      hash = (53 * hash) + getDatabaseUri().hashCode();
    }
    if (getServingLocationsCount() > 0) {
      hash = (37 * hash) + SERVING_LOCATIONS_FIELD_NUMBER;
      hash = (53 * hash) + getServingLocationsList().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction parseFrom(
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
      com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction prototype) {
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
   * Action that reconfigures a Cloud Spanner database.
   * </pre>
   *
   * Protobuf type {@code google.spanner.executor.v1.ReconfigureCloudDatabaseAction}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.executor.v1.ReconfigureCloudDatabaseAction)
      com.google.spanner.executor.v1.ReconfigureCloudDatabaseActionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.executor.v1.CloudExecutorProto
          .internal_static_google_spanner_executor_v1_ReconfigureCloudDatabaseAction_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.executor.v1.CloudExecutorProto
          .internal_static_google_spanner_executor_v1_ReconfigureCloudDatabaseAction_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction.class,
              com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction.Builder.class);
    }

    // Construct using com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction.newBuilder()
    private Builder() {}

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      databaseUri_ = "";
      servingLocations_ = com.google.protobuf.LazyStringArrayList.emptyList();
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.executor.v1.CloudExecutorProto
          .internal_static_google_spanner_executor_v1_ReconfigureCloudDatabaseAction_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction
        getDefaultInstanceForType() {
      return com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction build() {
      com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction buildPartial() {
      com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction result =
          new com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction(this);
      if (bitField0_ != 0) {
        buildPartial0(result);
      }
      onBuilt();
      return result;
    }

    private void buildPartial0(
        com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction result) {
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.databaseUri_ = databaseUri_;
        to_bitField0_ |= 0x00000001;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        servingLocations_.makeImmutable();
        result.servingLocations_ = servingLocations_;
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
      if (other instanceof com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction) {
        return mergeFrom((com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction other) {
      if (other
          == com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction.getDefaultInstance())
        return this;
      if (other.hasDatabaseUri()) {
        databaseUri_ = other.databaseUri_;
        bitField0_ |= 0x00000001;
        onChanged();
      }
      if (!other.servingLocations_.isEmpty()) {
        if (servingLocations_.isEmpty()) {
          servingLocations_ = other.servingLocations_;
          bitField0_ |= 0x00000002;
        } else {
          ensureServingLocationsIsMutable();
          servingLocations_.addAll(other.servingLocations_);
        }
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
            case 10:
              {
                databaseUri_ = input.readStringRequireUtf8();
                bitField0_ |= 0x00000001;
                break;
              } // case 10
            case 18:
              {
                java.lang.String s = input.readStringRequireUtf8();
                ensureServingLocationsIsMutable();
                servingLocations_.add(s);
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

    private java.lang.Object databaseUri_ = "";
    /**
     *
     *
     * <pre>
     * The fully qualified uri of the database to be reconfigured.
     * </pre>
     *
     * <code>optional string database_uri = 1;</code>
     *
     * @return Whether the databaseUri field is set.
     */
    public boolean hasDatabaseUri() {
      return ((bitField0_ & 0x00000001) != 0);
    }
    /**
     *
     *
     * <pre>
     * The fully qualified uri of the database to be reconfigured.
     * </pre>
     *
     * <code>optional string database_uri = 1;</code>
     *
     * @return The databaseUri.
     */
    public java.lang.String getDatabaseUri() {
      java.lang.Object ref = databaseUri_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        databaseUri_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * The fully qualified uri of the database to be reconfigured.
     * </pre>
     *
     * <code>optional string database_uri = 1;</code>
     *
     * @return The bytes for databaseUri.
     */
    public com.google.protobuf.ByteString getDatabaseUriBytes() {
      java.lang.Object ref = databaseUri_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        databaseUri_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     *
     *
     * <pre>
     * The fully qualified uri of the database to be reconfigured.
     * </pre>
     *
     * <code>optional string database_uri = 1;</code>
     *
     * @param value The databaseUri to set.
     * @return This builder for chaining.
     */
    public Builder setDatabaseUri(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }
      databaseUri_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The fully qualified uri of the database to be reconfigured.
     * </pre>
     *
     * <code>optional string database_uri = 1;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearDatabaseUri() {
      databaseUri_ = getDefaultInstance().getDatabaseUri();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The fully qualified uri of the database to be reconfigured.
     * </pre>
     *
     * <code>optional string database_uri = 1;</code>
     *
     * @param value The bytes for databaseUri to set.
     * @return This builder for chaining.
     */
    public Builder setDatabaseUriBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);
      databaseUri_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }

    private com.google.protobuf.LazyStringArrayList servingLocations_ =
        com.google.protobuf.LazyStringArrayList.emptyList();

    private void ensureServingLocationsIsMutable() {
      if (!servingLocations_.isModifiable()) {
        servingLocations_ = new com.google.protobuf.LazyStringArrayList(servingLocations_);
      }
      bitField0_ |= 0x00000002;
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @return A list containing the servingLocations.
     */
    public com.google.protobuf.ProtocolStringList getServingLocationsList() {
      servingLocations_.makeImmutable();
      return servingLocations_;
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @return The count of servingLocations.
     */
    public int getServingLocationsCount() {
      return servingLocations_.size();
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @param index The index of the element to return.
     * @return The servingLocations at the given index.
     */
    public java.lang.String getServingLocations(int index) {
      return servingLocations_.get(index);
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @param index The index of the value to return.
     * @return The bytes of the servingLocations at the given index.
     */
    public com.google.protobuf.ByteString getServingLocationsBytes(int index) {
      return servingLocations_.getByteString(index);
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @param index The index to set the value at.
     * @param value The servingLocations to set.
     * @return This builder for chaining.
     */
    public Builder setServingLocations(int index, java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureServingLocationsIsMutable();
      servingLocations_.set(index, value);
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @param value The servingLocations to add.
     * @return This builder for chaining.
     */
    public Builder addServingLocations(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureServingLocationsIsMutable();
      servingLocations_.add(value);
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @param values The servingLocations to add.
     * @return This builder for chaining.
     */
    public Builder addAllServingLocations(java.lang.Iterable<java.lang.String> values) {
      ensureServingLocationsIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(values, servingLocations_);
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @return This builder for chaining.
     */
    public Builder clearServingLocations() {
      servingLocations_ = com.google.protobuf.LazyStringArrayList.emptyList();
      bitField0_ = (bitField0_ & ~0x00000002);
      ;
      onChanged();
      return this;
    }
    /**
     *
     *
     * <pre>
     * The locations of the serving regions, e.g. "asia-south1".
     * </pre>
     *
     * <code>repeated string serving_locations = 2;</code>
     *
     * @param value The bytes of the servingLocations to add.
     * @return This builder for chaining.
     */
    public Builder addServingLocationsBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);
      ensureServingLocationsIsMutable();
      servingLocations_.add(value);
      bitField0_ |= 0x00000002;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.executor.v1.ReconfigureCloudDatabaseAction)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.executor.v1.ReconfigureCloudDatabaseAction)
  private static final com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction
      DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction();
  }

  public static com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ReconfigureCloudDatabaseAction> PARSER =
      new com.google.protobuf.AbstractParser<ReconfigureCloudDatabaseAction>() {
        @java.lang.Override
        public ReconfigureCloudDatabaseAction parsePartialFrom(
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

  public static com.google.protobuf.Parser<ReconfigureCloudDatabaseAction> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ReconfigureCloudDatabaseAction> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.executor.v1.ReconfigureCloudDatabaseAction getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
