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
// source: google/spanner/admin/instance/v1/spanner_instance_admin.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.admin.instance.v1;

public interface FreeInstanceMetadataOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.FreeInstanceMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Output only. Timestamp after which the instance will either be upgraded or
   * scheduled for deletion after a grace period. ExpireBehavior is used to
   * choose between upgrading or scheduling the free instance for deletion. This
   * timestamp is set during the creation of a free instance.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp expire_time = 1 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return Whether the expireTime field is set.
   */
  boolean hasExpireTime();
  /**
   *
   *
   * <pre>
   * Output only. Timestamp after which the instance will either be upgraded or
   * scheduled for deletion after a grace period. ExpireBehavior is used to
   * choose between upgrading or scheduling the free instance for deletion. This
   * timestamp is set during the creation of a free instance.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp expire_time = 1 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The expireTime.
   */
  com.google.protobuf.Timestamp getExpireTime();
  /**
   *
   *
   * <pre>
   * Output only. Timestamp after which the instance will either be upgraded or
   * scheduled for deletion after a grace period. ExpireBehavior is used to
   * choose between upgrading or scheduling the free instance for deletion. This
   * timestamp is set during the creation of a free instance.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp expire_time = 1 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  com.google.protobuf.TimestampOrBuilder getExpireTimeOrBuilder();

  /**
   *
   *
   * <pre>
   * Output only. If present, the timestamp at which the free instance was
   * upgraded to a provisioned instance.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp upgrade_time = 2 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return Whether the upgradeTime field is set.
   */
  boolean hasUpgradeTime();
  /**
   *
   *
   * <pre>
   * Output only. If present, the timestamp at which the free instance was
   * upgraded to a provisioned instance.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp upgrade_time = 2 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The upgradeTime.
   */
  com.google.protobuf.Timestamp getUpgradeTime();
  /**
   *
   *
   * <pre>
   * Output only. If present, the timestamp at which the free instance was
   * upgraded to a provisioned instance.
   * </pre>
   *
   * <code>.google.protobuf.Timestamp upgrade_time = 2 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  com.google.protobuf.TimestampOrBuilder getUpgradeTimeOrBuilder();

  /**
   *
   *
   * <pre>
   * Specifies the expiration behavior of a free instance. The default of
   * ExpireBehavior is `REMOVE_AFTER_GRACE_PERIOD`. This can be modified during
   * or after creation, and before expiration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.FreeInstanceMetadata.ExpireBehavior expire_behavior = 3;
   * </code>
   *
   * @return The enum numeric value on the wire for expireBehavior.
   */
  int getExpireBehaviorValue();
  /**
   *
   *
   * <pre>
   * Specifies the expiration behavior of a free instance. The default of
   * ExpireBehavior is `REMOVE_AFTER_GRACE_PERIOD`. This can be modified during
   * or after creation, and before expiration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.FreeInstanceMetadata.ExpireBehavior expire_behavior = 3;
   * </code>
   *
   * @return The expireBehavior.
   */
  com.google.spanner.admin.instance.v1.FreeInstanceMetadata.ExpireBehavior getExpireBehavior();
}
