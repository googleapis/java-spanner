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
// source: google/spanner/v1/query_plan.proto

package com.google.spanner.v1;

/**
 *
 *
 * <pre>
 * Contains an ordered list of nodes appearing in the query plan.
 * </pre>
 *
 * Protobuf type {@code google.spanner.v1.QueryPlan}
 */
public final class QueryPlan extends com.google.protobuf.GeneratedMessageV3
    implements
    // @@protoc_insertion_point(message_implements:google.spanner.v1.QueryPlan)
    QueryPlanOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use QueryPlan.newBuilder() to construct.
  private QueryPlan(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private QueryPlan() {
    planNodes_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new QueryPlan();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private QueryPlan(
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
                planNodes_ = new java.util.ArrayList<com.google.spanner.v1.PlanNode>();
                mutable_bitField0_ |= 0x00000001;
              }
              planNodes_.add(
                  input.readMessage(com.google.spanner.v1.PlanNode.parser(), extensionRegistry));
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
    } catch (com.google.protobuf.UninitializedMessageException e) {
      throw e.asInvalidProtocolBufferException().setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        planNodes_ = java.util.Collections.unmodifiableList(planNodes_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return com.google.spanner.v1.QueryPlanProto
        .internal_static_google_spanner_v1_QueryPlan_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.google.spanner.v1.QueryPlanProto
        .internal_static_google_spanner_v1_QueryPlan_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.google.spanner.v1.QueryPlan.class, com.google.spanner.v1.QueryPlan.Builder.class);
  }

  public static final int PLAN_NODES_FIELD_NUMBER = 1;
  private java.util.List<com.google.spanner.v1.PlanNode> planNodes_;
  /**
   *
   *
   * <pre>
   * The nodes in the query plan. Plan nodes are returned in pre-order starting
   * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
   * `plan_nodes`.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
   */
  @java.lang.Override
  public java.util.List<com.google.spanner.v1.PlanNode> getPlanNodesList() {
    return planNodes_;
  }
  /**
   *
   *
   * <pre>
   * The nodes in the query plan. Plan nodes are returned in pre-order starting
   * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
   * `plan_nodes`.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
   */
  @java.lang.Override
  public java.util.List<? extends com.google.spanner.v1.PlanNodeOrBuilder>
      getPlanNodesOrBuilderList() {
    return planNodes_;
  }
  /**
   *
   *
   * <pre>
   * The nodes in the query plan. Plan nodes are returned in pre-order starting
   * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
   * `plan_nodes`.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
   */
  @java.lang.Override
  public int getPlanNodesCount() {
    return planNodes_.size();
  }
  /**
   *
   *
   * <pre>
   * The nodes in the query plan. Plan nodes are returned in pre-order starting
   * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
   * `plan_nodes`.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
   */
  @java.lang.Override
  public com.google.spanner.v1.PlanNode getPlanNodes(int index) {
    return planNodes_.get(index);
  }
  /**
   *
   *
   * <pre>
   * The nodes in the query plan. Plan nodes are returned in pre-order starting
   * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
   * `plan_nodes`.
   * </pre>
   *
   * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
   */
  @java.lang.Override
  public com.google.spanner.v1.PlanNodeOrBuilder getPlanNodesOrBuilder(int index) {
    return planNodes_.get(index);
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
    for (int i = 0; i < planNodes_.size(); i++) {
      output.writeMessage(1, planNodes_.get(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    for (int i = 0; i < planNodes_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, planNodes_.get(i));
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
    if (!(obj instanceof com.google.spanner.v1.QueryPlan)) {
      return super.equals(obj);
    }
    com.google.spanner.v1.QueryPlan other = (com.google.spanner.v1.QueryPlan) obj;

    if (!getPlanNodesList().equals(other.getPlanNodesList())) return false;
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
    if (getPlanNodesCount() > 0) {
      hash = (37 * hash) + PLAN_NODES_FIELD_NUMBER;
      hash = (53 * hash) + getPlanNodesList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.QueryPlan parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.QueryPlan parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(
        PARSER, input, extensionRegistry);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static com.google.spanner.v1.QueryPlan parseFrom(
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

  public static Builder newBuilder(com.google.spanner.v1.QueryPlan prototype) {
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
   * Contains an ordered list of nodes appearing in the query plan.
   * </pre>
   *
   * Protobuf type {@code google.spanner.v1.QueryPlan}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:google.spanner.v1.QueryPlan)
      com.google.spanner.v1.QueryPlanOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return com.google.spanner.v1.QueryPlanProto
          .internal_static_google_spanner_v1_QueryPlan_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.google.spanner.v1.QueryPlanProto
          .internal_static_google_spanner_v1_QueryPlan_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.google.spanner.v1.QueryPlan.class, com.google.spanner.v1.QueryPlan.Builder.class);
    }

    // Construct using com.google.spanner.v1.QueryPlan.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getPlanNodesFieldBuilder();
      }
    }

    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (planNodesBuilder_ == null) {
        planNodes_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
      } else {
        planNodesBuilder_.clear();
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return com.google.spanner.v1.QueryPlanProto
          .internal_static_google_spanner_v1_QueryPlan_descriptor;
    }

    @java.lang.Override
    public com.google.spanner.v1.QueryPlan getDefaultInstanceForType() {
      return com.google.spanner.v1.QueryPlan.getDefaultInstance();
    }

    @java.lang.Override
    public com.google.spanner.v1.QueryPlan build() {
      com.google.spanner.v1.QueryPlan result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.google.spanner.v1.QueryPlan buildPartial() {
      com.google.spanner.v1.QueryPlan result = new com.google.spanner.v1.QueryPlan(this);
      int from_bitField0_ = bitField0_;
      if (planNodesBuilder_ == null) {
        if (((bitField0_ & 0x00000001) != 0)) {
          planNodes_ = java.util.Collections.unmodifiableList(planNodes_);
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.planNodes_ = planNodes_;
      } else {
        result.planNodes_ = planNodesBuilder_.build();
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
      if (other instanceof com.google.spanner.v1.QueryPlan) {
        return mergeFrom((com.google.spanner.v1.QueryPlan) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.google.spanner.v1.QueryPlan other) {
      if (other == com.google.spanner.v1.QueryPlan.getDefaultInstance()) return this;
      if (planNodesBuilder_ == null) {
        if (!other.planNodes_.isEmpty()) {
          if (planNodes_.isEmpty()) {
            planNodes_ = other.planNodes_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensurePlanNodesIsMutable();
            planNodes_.addAll(other.planNodes_);
          }
          onChanged();
        }
      } else {
        if (!other.planNodes_.isEmpty()) {
          if (planNodesBuilder_.isEmpty()) {
            planNodesBuilder_.dispose();
            planNodesBuilder_ = null;
            planNodes_ = other.planNodes_;
            bitField0_ = (bitField0_ & ~0x00000001);
            planNodesBuilder_ =
                com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                    ? getPlanNodesFieldBuilder()
                    : null;
          } else {
            planNodesBuilder_.addAllMessages(other.planNodes_);
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
      com.google.spanner.v1.QueryPlan parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.google.spanner.v1.QueryPlan) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int bitField0_;

    private java.util.List<com.google.spanner.v1.PlanNode> planNodes_ =
        java.util.Collections.emptyList();

    private void ensurePlanNodesIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        planNodes_ = new java.util.ArrayList<com.google.spanner.v1.PlanNode>(planNodes_);
        bitField0_ |= 0x00000001;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
            com.google.spanner.v1.PlanNode,
            com.google.spanner.v1.PlanNode.Builder,
            com.google.spanner.v1.PlanNodeOrBuilder>
        planNodesBuilder_;

    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public java.util.List<com.google.spanner.v1.PlanNode> getPlanNodesList() {
      if (planNodesBuilder_ == null) {
        return java.util.Collections.unmodifiableList(planNodes_);
      } else {
        return planNodesBuilder_.getMessageList();
      }
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public int getPlanNodesCount() {
      if (planNodesBuilder_ == null) {
        return planNodes_.size();
      } else {
        return planNodesBuilder_.getCount();
      }
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public com.google.spanner.v1.PlanNode getPlanNodes(int index) {
      if (planNodesBuilder_ == null) {
        return planNodes_.get(index);
      } else {
        return planNodesBuilder_.getMessage(index);
      }
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder setPlanNodes(int index, com.google.spanner.v1.PlanNode value) {
      if (planNodesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePlanNodesIsMutable();
        planNodes_.set(index, value);
        onChanged();
      } else {
        planNodesBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder setPlanNodes(int index, com.google.spanner.v1.PlanNode.Builder builderForValue) {
      if (planNodesBuilder_ == null) {
        ensurePlanNodesIsMutable();
        planNodes_.set(index, builderForValue.build());
        onChanged();
      } else {
        planNodesBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder addPlanNodes(com.google.spanner.v1.PlanNode value) {
      if (planNodesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePlanNodesIsMutable();
        planNodes_.add(value);
        onChanged();
      } else {
        planNodesBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder addPlanNodes(int index, com.google.spanner.v1.PlanNode value) {
      if (planNodesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePlanNodesIsMutable();
        planNodes_.add(index, value);
        onChanged();
      } else {
        planNodesBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder addPlanNodes(com.google.spanner.v1.PlanNode.Builder builderForValue) {
      if (planNodesBuilder_ == null) {
        ensurePlanNodesIsMutable();
        planNodes_.add(builderForValue.build());
        onChanged();
      } else {
        planNodesBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder addPlanNodes(int index, com.google.spanner.v1.PlanNode.Builder builderForValue) {
      if (planNodesBuilder_ == null) {
        ensurePlanNodesIsMutable();
        planNodes_.add(index, builderForValue.build());
        onChanged();
      } else {
        planNodesBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder addAllPlanNodes(
        java.lang.Iterable<? extends com.google.spanner.v1.PlanNode> values) {
      if (planNodesBuilder_ == null) {
        ensurePlanNodesIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, planNodes_);
        onChanged();
      } else {
        planNodesBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder clearPlanNodes() {
      if (planNodesBuilder_ == null) {
        planNodes_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
      } else {
        planNodesBuilder_.clear();
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public Builder removePlanNodes(int index) {
      if (planNodesBuilder_ == null) {
        ensurePlanNodesIsMutable();
        planNodes_.remove(index);
        onChanged();
      } else {
        planNodesBuilder_.remove(index);
      }
      return this;
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public com.google.spanner.v1.PlanNode.Builder getPlanNodesBuilder(int index) {
      return getPlanNodesFieldBuilder().getBuilder(index);
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public com.google.spanner.v1.PlanNodeOrBuilder getPlanNodesOrBuilder(int index) {
      if (planNodesBuilder_ == null) {
        return planNodes_.get(index);
      } else {
        return planNodesBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public java.util.List<? extends com.google.spanner.v1.PlanNodeOrBuilder>
        getPlanNodesOrBuilderList() {
      if (planNodesBuilder_ != null) {
        return planNodesBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(planNodes_);
      }
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public com.google.spanner.v1.PlanNode.Builder addPlanNodesBuilder() {
      return getPlanNodesFieldBuilder()
          .addBuilder(com.google.spanner.v1.PlanNode.getDefaultInstance());
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public com.google.spanner.v1.PlanNode.Builder addPlanNodesBuilder(int index) {
      return getPlanNodesFieldBuilder()
          .addBuilder(index, com.google.spanner.v1.PlanNode.getDefaultInstance());
    }
    /**
     *
     *
     * <pre>
     * The nodes in the query plan. Plan nodes are returned in pre-order starting
     * with the plan root. Each [PlanNode][google.spanner.v1.PlanNode]'s `id` corresponds to its index in
     * `plan_nodes`.
     * </pre>
     *
     * <code>repeated .google.spanner.v1.PlanNode plan_nodes = 1;</code>
     */
    public java.util.List<com.google.spanner.v1.PlanNode.Builder> getPlanNodesBuilderList() {
      return getPlanNodesFieldBuilder().getBuilderList();
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
            com.google.spanner.v1.PlanNode,
            com.google.spanner.v1.PlanNode.Builder,
            com.google.spanner.v1.PlanNodeOrBuilder>
        getPlanNodesFieldBuilder() {
      if (planNodesBuilder_ == null) {
        planNodesBuilder_ =
            new com.google.protobuf.RepeatedFieldBuilderV3<
                com.google.spanner.v1.PlanNode,
                com.google.spanner.v1.PlanNode.Builder,
                com.google.spanner.v1.PlanNodeOrBuilder>(
                planNodes_, ((bitField0_ & 0x00000001) != 0), getParentForChildren(), isClean());
        planNodes_ = null;
      }
      return planNodesBuilder_;
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

    // @@protoc_insertion_point(builder_scope:google.spanner.v1.QueryPlan)
  }

  // @@protoc_insertion_point(class_scope:google.spanner.v1.QueryPlan)
  private static final com.google.spanner.v1.QueryPlan DEFAULT_INSTANCE;

  static {
    DEFAULT_INSTANCE = new com.google.spanner.v1.QueryPlan();
  }

  public static com.google.spanner.v1.QueryPlan getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<QueryPlan> PARSER =
      new com.google.protobuf.AbstractParser<QueryPlan>() {
        @java.lang.Override
        public QueryPlan parsePartialFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new QueryPlan(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<QueryPlan> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<QueryPlan> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.spanner.v1.QueryPlan getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
