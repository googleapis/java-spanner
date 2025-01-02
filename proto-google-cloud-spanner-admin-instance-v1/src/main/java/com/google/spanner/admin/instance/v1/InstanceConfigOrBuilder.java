/*
 * Copyright 2025 Google LLC
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

public interface InstanceConfigOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.instance.v1.InstanceConfig)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * A unique identifier for the instance configuration.  Values
   * are of the form
   * `projects/&lt;project&gt;/instanceConfigs/[a-z][-a-z0-9]*`.
   *
   * User instance configuration must start with `custom-`.
   * </pre>
   *
   * <code>string name = 1;</code>
   *
   * @return The name.
   */
  java.lang.String getName();
  /**
   *
   *
   * <pre>
   * A unique identifier for the instance configuration.  Values
   * are of the form
   * `projects/&lt;project&gt;/instanceConfigs/[a-z][-a-z0-9]*`.
   *
   * User instance configuration must start with `custom-`.
   * </pre>
   *
   * <code>string name = 1;</code>
   *
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   *
   *
   * <pre>
   * The name of this instance configuration as it appears in UIs.
   * </pre>
   *
   * <code>string display_name = 2;</code>
   *
   * @return The displayName.
   */
  java.lang.String getDisplayName();
  /**
   *
   *
   * <pre>
   * The name of this instance configuration as it appears in UIs.
   * </pre>
   *
   * <code>string display_name = 2;</code>
   *
   * @return The bytes for displayName.
   */
  com.google.protobuf.ByteString getDisplayNameBytes();

  /**
   *
   *
   * <pre>
   * Output only. Whether this instance configuration is a Google-managed or
   * user-managed configuration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.Type config_type = 5 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The enum numeric value on the wire for configType.
   */
  int getConfigTypeValue();
  /**
   *
   *
   * <pre>
   * Output only. Whether this instance configuration is a Google-managed or
   * user-managed configuration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.Type config_type = 5 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The configType.
   */
  com.google.spanner.admin.instance.v1.InstanceConfig.Type getConfigType();

  /**
   *
   *
   * <pre>
   * The geographic placement of nodes in this instance configuration and their
   * replication properties.
   *
   * To create user-managed configurations, input
   * `replicas` must include all replicas in `replicas` of the `base_config`
   * and include one or more replicas in the `optional_replicas` of the
   * `base_config`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 3;</code>
   */
  java.util.List<com.google.spanner.admin.instance.v1.ReplicaInfo> getReplicasList();
  /**
   *
   *
   * <pre>
   * The geographic placement of nodes in this instance configuration and their
   * replication properties.
   *
   * To create user-managed configurations, input
   * `replicas` must include all replicas in `replicas` of the `base_config`
   * and include one or more replicas in the `optional_replicas` of the
   * `base_config`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 3;</code>
   */
  com.google.spanner.admin.instance.v1.ReplicaInfo getReplicas(int index);
  /**
   *
   *
   * <pre>
   * The geographic placement of nodes in this instance configuration and their
   * replication properties.
   *
   * To create user-managed configurations, input
   * `replicas` must include all replicas in `replicas` of the `base_config`
   * and include one or more replicas in the `optional_replicas` of the
   * `base_config`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 3;</code>
   */
  int getReplicasCount();
  /**
   *
   *
   * <pre>
   * The geographic placement of nodes in this instance configuration and their
   * replication properties.
   *
   * To create user-managed configurations, input
   * `replicas` must include all replicas in `replicas` of the `base_config`
   * and include one or more replicas in the `optional_replicas` of the
   * `base_config`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 3;</code>
   */
  java.util.List<? extends com.google.spanner.admin.instance.v1.ReplicaInfoOrBuilder>
      getReplicasOrBuilderList();
  /**
   *
   *
   * <pre>
   * The geographic placement of nodes in this instance configuration and their
   * replication properties.
   *
   * To create user-managed configurations, input
   * `replicas` must include all replicas in `replicas` of the `base_config`
   * and include one or more replicas in the `optional_replicas` of the
   * `base_config`.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.instance.v1.ReplicaInfo replicas = 3;</code>
   */
  com.google.spanner.admin.instance.v1.ReplicaInfoOrBuilder getReplicasOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * Output only. The available optional replicas to choose from for
   * user-managed configurations. Populated for Google-managed configurations.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.admin.instance.v1.ReplicaInfo optional_replicas = 6 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  java.util.List<com.google.spanner.admin.instance.v1.ReplicaInfo> getOptionalReplicasList();
  /**
   *
   *
   * <pre>
   * Output only. The available optional replicas to choose from for
   * user-managed configurations. Populated for Google-managed configurations.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.admin.instance.v1.ReplicaInfo optional_replicas = 6 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  com.google.spanner.admin.instance.v1.ReplicaInfo getOptionalReplicas(int index);
  /**
   *
   *
   * <pre>
   * Output only. The available optional replicas to choose from for
   * user-managed configurations. Populated for Google-managed configurations.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.admin.instance.v1.ReplicaInfo optional_replicas = 6 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  int getOptionalReplicasCount();
  /**
   *
   *
   * <pre>
   * Output only. The available optional replicas to choose from for
   * user-managed configurations. Populated for Google-managed configurations.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.admin.instance.v1.ReplicaInfo optional_replicas = 6 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  java.util.List<? extends com.google.spanner.admin.instance.v1.ReplicaInfoOrBuilder>
      getOptionalReplicasOrBuilderList();
  /**
   *
   *
   * <pre>
   * Output only. The available optional replicas to choose from for
   * user-managed configurations. Populated for Google-managed configurations.
   * </pre>
   *
   * <code>
   * repeated .google.spanner.admin.instance.v1.ReplicaInfo optional_replicas = 6 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   */
  com.google.spanner.admin.instance.v1.ReplicaInfoOrBuilder getOptionalReplicasOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * Base configuration name, e.g. projects/&lt;project_name&gt;/instanceConfigs/nam3,
   * based on which this configuration is created. Only set for user-managed
   * configurations. `base_config` must refer to a configuration of type
   * `GOOGLE_MANAGED` in the same project as this configuration.
   * </pre>
   *
   * <code>string base_config = 7 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The baseConfig.
   */
  java.lang.String getBaseConfig();
  /**
   *
   *
   * <pre>
   * Base configuration name, e.g. projects/&lt;project_name&gt;/instanceConfigs/nam3,
   * based on which this configuration is created. Only set for user-managed
   * configurations. `base_config` must refer to a configuration of type
   * `GOOGLE_MANAGED` in the same project as this configuration.
   * </pre>
   *
   * <code>string base_config = 7 [(.google.api.resource_reference) = { ... }</code>
   *
   * @return The bytes for baseConfig.
   */
  com.google.protobuf.ByteString getBaseConfigBytes();

  /**
   *
   *
   * <pre>
   * Cloud Labels are a flexible and lightweight mechanism for organizing cloud
   * resources into groups that reflect a customer's organizational needs and
   * deployment strategies. Cloud Labels can be used to filter collections of
   * resources. They can be used to control how resource metrics are aggregated.
   * And they can be used as arguments to policy management rules (e.g. route,
   * firewall, load balancing, etc.).
   *
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z][a-z0-9_-]{0,62}`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `[a-z0-9_-]{0,63}`.
   *  * No more than 64 labels can be associated with a given resource.
   *
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   *
   * If you plan to use labels in your own code, please note that additional
   * characters may be allowed in the future. Therefore, you are advised to use
   * an internal label representation, such as JSON, which doesn't rely upon
   * specific characters being disallowed.  For example, representing labels
   * as the string:  name + "_" + value  would prove problematic if we were to
   * allow "_" in a future release.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 8;</code>
   */
  int getLabelsCount();
  /**
   *
   *
   * <pre>
   * Cloud Labels are a flexible and lightweight mechanism for organizing cloud
   * resources into groups that reflect a customer's organizational needs and
   * deployment strategies. Cloud Labels can be used to filter collections of
   * resources. They can be used to control how resource metrics are aggregated.
   * And they can be used as arguments to policy management rules (e.g. route,
   * firewall, load balancing, etc.).
   *
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z][a-z0-9_-]{0,62}`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `[a-z0-9_-]{0,63}`.
   *  * No more than 64 labels can be associated with a given resource.
   *
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   *
   * If you plan to use labels in your own code, please note that additional
   * characters may be allowed in the future. Therefore, you are advised to use
   * an internal label representation, such as JSON, which doesn't rely upon
   * specific characters being disallowed.  For example, representing labels
   * as the string:  name + "_" + value  would prove problematic if we were to
   * allow "_" in a future release.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 8;</code>
   */
  boolean containsLabels(java.lang.String key);
  /** Use {@link #getLabelsMap()} instead. */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.String> getLabels();
  /**
   *
   *
   * <pre>
   * Cloud Labels are a flexible and lightweight mechanism for organizing cloud
   * resources into groups that reflect a customer's organizational needs and
   * deployment strategies. Cloud Labels can be used to filter collections of
   * resources. They can be used to control how resource metrics are aggregated.
   * And they can be used as arguments to policy management rules (e.g. route,
   * firewall, load balancing, etc.).
   *
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z][a-z0-9_-]{0,62}`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `[a-z0-9_-]{0,63}`.
   *  * No more than 64 labels can be associated with a given resource.
   *
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   *
   * If you plan to use labels in your own code, please note that additional
   * characters may be allowed in the future. Therefore, you are advised to use
   * an internal label representation, such as JSON, which doesn't rely upon
   * specific characters being disallowed.  For example, representing labels
   * as the string:  name + "_" + value  would prove problematic if we were to
   * allow "_" in a future release.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 8;</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getLabelsMap();
  /**
   *
   *
   * <pre>
   * Cloud Labels are a flexible and lightweight mechanism for organizing cloud
   * resources into groups that reflect a customer's organizational needs and
   * deployment strategies. Cloud Labels can be used to filter collections of
   * resources. They can be used to control how resource metrics are aggregated.
   * And they can be used as arguments to policy management rules (e.g. route,
   * firewall, load balancing, etc.).
   *
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z][a-z0-9_-]{0,62}`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `[a-z0-9_-]{0,63}`.
   *  * No more than 64 labels can be associated with a given resource.
   *
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   *
   * If you plan to use labels in your own code, please note that additional
   * characters may be allowed in the future. Therefore, you are advised to use
   * an internal label representation, such as JSON, which doesn't rely upon
   * specific characters being disallowed.  For example, representing labels
   * as the string:  name + "_" + value  would prove problematic if we were to
   * allow "_" in a future release.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 8;</code>
   */
  /* nullable */
  java.lang.String getLabelsOrDefault(
      java.lang.String key,
      /* nullable */
      java.lang.String defaultValue);
  /**
   *
   *
   * <pre>
   * Cloud Labels are a flexible and lightweight mechanism for organizing cloud
   * resources into groups that reflect a customer's organizational needs and
   * deployment strategies. Cloud Labels can be used to filter collections of
   * resources. They can be used to control how resource metrics are aggregated.
   * And they can be used as arguments to policy management rules (e.g. route,
   * firewall, load balancing, etc.).
   *
   *  * Label keys must be between 1 and 63 characters long and must conform to
   *    the following regular expression: `[a-z][a-z0-9_-]{0,62}`.
   *  * Label values must be between 0 and 63 characters long and must conform
   *    to the regular expression `[a-z0-9_-]{0,63}`.
   *  * No more than 64 labels can be associated with a given resource.
   *
   * See https://goo.gl/xmQnxf for more information on and examples of labels.
   *
   * If you plan to use labels in your own code, please note that additional
   * characters may be allowed in the future. Therefore, you are advised to use
   * an internal label representation, such as JSON, which doesn't rely upon
   * specific characters being disallowed.  For example, representing labels
   * as the string:  name + "_" + value  would prove problematic if we were to
   * allow "_" in a future release.
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 8;</code>
   */
  java.lang.String getLabelsOrThrow(java.lang.String key);

  /**
   *
   *
   * <pre>
   * etag is used for optimistic concurrency control as a way
   * to help prevent simultaneous updates of a instance configuration from
   * overwriting each other. It is strongly suggested that systems make use of
   * the etag in the read-modify-write cycle to perform instance configuration
   * updates in order to avoid race conditions: An etag is returned in the
   * response which contains instance configurations, and systems are expected
   * to put that etag in the request to update instance configuration to ensure
   * that their change is applied to the same version of the instance
   * configuration. If no etag is provided in the call to update the instance
   * configuration, then the existing instance configuration is overwritten
   * blindly.
   * </pre>
   *
   * <code>string etag = 9;</code>
   *
   * @return The etag.
   */
  java.lang.String getEtag();
  /**
   *
   *
   * <pre>
   * etag is used for optimistic concurrency control as a way
   * to help prevent simultaneous updates of a instance configuration from
   * overwriting each other. It is strongly suggested that systems make use of
   * the etag in the read-modify-write cycle to perform instance configuration
   * updates in order to avoid race conditions: An etag is returned in the
   * response which contains instance configurations, and systems are expected
   * to put that etag in the request to update instance configuration to ensure
   * that their change is applied to the same version of the instance
   * configuration. If no etag is provided in the call to update the instance
   * configuration, then the existing instance configuration is overwritten
   * blindly.
   * </pre>
   *
   * <code>string etag = 9;</code>
   *
   * @return The bytes for etag.
   */
  com.google.protobuf.ByteString getEtagBytes();

  /**
   *
   *
   * <pre>
   * Allowed values of the "default_leader" schema option for databases in
   * instances that use this instance configuration.
   * </pre>
   *
   * <code>repeated string leader_options = 4;</code>
   *
   * @return A list containing the leaderOptions.
   */
  java.util.List<java.lang.String> getLeaderOptionsList();
  /**
   *
   *
   * <pre>
   * Allowed values of the "default_leader" schema option for databases in
   * instances that use this instance configuration.
   * </pre>
   *
   * <code>repeated string leader_options = 4;</code>
   *
   * @return The count of leaderOptions.
   */
  int getLeaderOptionsCount();
  /**
   *
   *
   * <pre>
   * Allowed values of the "default_leader" schema option for databases in
   * instances that use this instance configuration.
   * </pre>
   *
   * <code>repeated string leader_options = 4;</code>
   *
   * @param index The index of the element to return.
   * @return The leaderOptions at the given index.
   */
  java.lang.String getLeaderOptions(int index);
  /**
   *
   *
   * <pre>
   * Allowed values of the "default_leader" schema option for databases in
   * instances that use this instance configuration.
   * </pre>
   *
   * <code>repeated string leader_options = 4;</code>
   *
   * @param index The index of the value to return.
   * @return The bytes of the leaderOptions at the given index.
   */
  com.google.protobuf.ByteString getLeaderOptionsBytes(int index);

  /**
   *
   *
   * <pre>
   * Output only. If true, the instance configuration is being created or
   * updated. If false, there are no ongoing operations for the instance
   * configuration.
   * </pre>
   *
   * <code>bool reconciling = 10 [(.google.api.field_behavior) = OUTPUT_ONLY];</code>
   *
   * @return The reconciling.
   */
  boolean getReconciling();

  /**
   *
   *
   * <pre>
   * Output only. The current instance configuration state. Applicable only for
   * `USER_MANAGED` configurations.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.State state = 11 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The enum numeric value on the wire for state.
   */
  int getStateValue();
  /**
   *
   *
   * <pre>
   * Output only. The current instance configuration state. Applicable only for
   * `USER_MANAGED` configurations.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.State state = 11 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The state.
   */
  com.google.spanner.admin.instance.v1.InstanceConfig.State getState();

  /**
   *
   *
   * <pre>
   * Output only. Describes whether free instances are available to be created
   * in this instance configuration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.FreeInstanceAvailability free_instance_availability = 12 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The enum numeric value on the wire for freeInstanceAvailability.
   */
  int getFreeInstanceAvailabilityValue();
  /**
   *
   *
   * <pre>
   * Output only. Describes whether free instances are available to be created
   * in this instance configuration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.FreeInstanceAvailability free_instance_availability = 12 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The freeInstanceAvailability.
   */
  com.google.spanner.admin.instance.v1.InstanceConfig.FreeInstanceAvailability
      getFreeInstanceAvailability();

  /**
   *
   *
   * <pre>
   * Output only. The `QuorumType` of the instance configuration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.QuorumType quorum_type = 18 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The enum numeric value on the wire for quorumType.
   */
  int getQuorumTypeValue();
  /**
   *
   *
   * <pre>
   * Output only. The `QuorumType` of the instance configuration.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.instance.v1.InstanceConfig.QuorumType quorum_type = 18 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The quorumType.
   */
  com.google.spanner.admin.instance.v1.InstanceConfig.QuorumType getQuorumType();

  /**
   *
   *
   * <pre>
   * Output only. The storage limit in bytes per processing unit.
   * </pre>
   *
   * <code>
   * int64 storage_limit_per_processing_unit = 19 [(.google.api.field_behavior) = OUTPUT_ONLY];
   * </code>
   *
   * @return The storageLimitPerProcessingUnit.
   */
  long getStorageLimitPerProcessingUnit();
}
