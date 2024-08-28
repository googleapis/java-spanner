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
// source: google/spanner/v1/query_plan.proto

// Protobuf Java Version: 3.25.4
package com.google.spanner.v1;

public interface QueryPlanOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.QueryPlan)
    com.google.protobuf.MessageOrBuilder {

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
  java.util.List<com.google.spanner.v1.PlanNode> getPlanNodesList();
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
  com.google.spanner.v1.PlanNode getPlanNodes(int index);
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
  int getPlanNodesCount();
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
  java.util.List<? extends com.google.spanner.v1.PlanNodeOrBuilder> getPlanNodesOrBuilderList();
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
  com.google.spanner.v1.PlanNodeOrBuilder getPlanNodesOrBuilder(int index);
}
