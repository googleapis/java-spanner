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

public interface DirectedReadOptionsOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.DirectedReadOptions)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Include_replicas indicates the order of replicas (as they appear in
   * this list) to process the request. If auto_failover_disabled is set to
   * true and all replicas are exhausted without finding a healthy replica,
   * Spanner will wait for a replica in the list to become available, requests
   * may fail due to `DEADLINE_EXCEEDED` errors.
   * </pre>
   *
   * <code>.google.spanner.v1.DirectedReadOptions.IncludeReplicas include_replicas = 1;</code>
   *
   * @return Whether the includeReplicas field is set.
   */
  boolean hasIncludeReplicas();
  /**
   *
   *
   * <pre>
   * Include_replicas indicates the order of replicas (as they appear in
   * this list) to process the request. If auto_failover_disabled is set to
   * true and all replicas are exhausted without finding a healthy replica,
   * Spanner will wait for a replica in the list to become available, requests
   * may fail due to `DEADLINE_EXCEEDED` errors.
   * </pre>
   *
   * <code>.google.spanner.v1.DirectedReadOptions.IncludeReplicas include_replicas = 1;</code>
   *
   * @return The includeReplicas.
   */
  com.google.spanner.v1.DirectedReadOptions.IncludeReplicas getIncludeReplicas();
  /**
   *
   *
   * <pre>
   * Include_replicas indicates the order of replicas (as they appear in
   * this list) to process the request. If auto_failover_disabled is set to
   * true and all replicas are exhausted without finding a healthy replica,
   * Spanner will wait for a replica in the list to become available, requests
   * may fail due to `DEADLINE_EXCEEDED` errors.
   * </pre>
   *
   * <code>.google.spanner.v1.DirectedReadOptions.IncludeReplicas include_replicas = 1;</code>
   */
  com.google.spanner.v1.DirectedReadOptions.IncludeReplicasOrBuilder getIncludeReplicasOrBuilder();

  /**
   *
   *
   * <pre>
   * Exclude_replicas indicates that specified replicas should be excluded
   * from serving requests. Spanner will not route requests to the replicas
   * in this list.
   * </pre>
   *
   * <code>.google.spanner.v1.DirectedReadOptions.ExcludeReplicas exclude_replicas = 2;</code>
   *
   * @return Whether the excludeReplicas field is set.
   */
  boolean hasExcludeReplicas();
  /**
   *
   *
   * <pre>
   * Exclude_replicas indicates that specified replicas should be excluded
   * from serving requests. Spanner will not route requests to the replicas
   * in this list.
   * </pre>
   *
   * <code>.google.spanner.v1.DirectedReadOptions.ExcludeReplicas exclude_replicas = 2;</code>
   *
   * @return The excludeReplicas.
   */
  com.google.spanner.v1.DirectedReadOptions.ExcludeReplicas getExcludeReplicas();
  /**
   *
   *
   * <pre>
   * Exclude_replicas indicates that specified replicas should be excluded
   * from serving requests. Spanner will not route requests to the replicas
   * in this list.
   * </pre>
   *
   * <code>.google.spanner.v1.DirectedReadOptions.ExcludeReplicas exclude_replicas = 2;</code>
   */
  com.google.spanner.v1.DirectedReadOptions.ExcludeReplicasOrBuilder getExcludeReplicasOrBuilder();

  com.google.spanner.v1.DirectedReadOptions.ReplicasCase getReplicasCase();
}
