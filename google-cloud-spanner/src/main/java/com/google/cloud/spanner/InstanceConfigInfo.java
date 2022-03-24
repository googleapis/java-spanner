/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.cloud.FieldSelector;
import com.google.cloud.spanner.InstanceInfo.BuilderImpl;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.FieldMask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Represents a Cloud Spanner instance config resource. */
public class InstanceConfigInfo {

  /** Represent an updatable field in Cloud Spanner InstanceConfig. */
  public enum InstanceConfigField implements FieldSelector {
    DISPLAY_NAME("display_name"),
    LABELS("labels");

    private final String selector;

    InstanceConfigField(String selector) {
      this.selector = selector;
    }

    @Override
    public String getSelector() {
      return selector;
    }

    static FieldMask toFieldMask(InstanceConfigField... fields) {
      FieldMask.Builder builder = FieldMask.newBuilder();
      for (InstanceConfigField field : fields) {
        builder.addPaths(field.getSelector());
      }
      return builder.build();
    }
  }

  /** Type of the Instance config. */
  public enum Type {
    TYPE_UNSPECIFIED,
    GOOGLE_MANAGED,
    USER_MANAGED
  }

  /** Type of the Instance config. */
  public enum State {
    STATE_UNSPECIFIED,
    CREATING,
    READY
  }

  private final InstanceConfigId id;
  private final String displayName;
  private final List<ReplicaInfo> replicas;
  private final List<String> leaderOptions;
  private final List<ReplicaInfo> optionalReplicas;
  private final String baseConfig;
  private final Type configType;
  private final String etag;
  private final boolean reconciling;
  private final State state;
  private final Map<String, String> labels;

  /** Returns the id of this instance config. */
  public InstanceConfigId getId() {
    return id;
  }

  /** Returns the display name of this instance config. */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * The geographic placement of nodes in this instance configuration and their replication
   * properties.
   */
  public List<ReplicaInfo> getReplicas() {
    return replicas;
  }

  /**
   * Allowed values of the default leader schema option for databases in instances that use this
   * instance configuration.
   */
  public List<String> getLeaderOptions() {
    return leaderOptions;
  }

  /**
   * The available optional replicas to choose from for user managed configurations. Populated for
   * Google managed configurations.
   */
  public List<ReplicaInfo> getOptionalReplicas() {
    return optionalReplicas;
  }

  /**
   * Base configuration name, e.g. projects/<project_name>/instanceConfigs/nam3, based on which this
   * configuration is created. Only set for user managed configurations. base_config must refer to a
   * configuration of type GOOGLE_MANAGED.
   */
  public String getBaseConfig() {
    return baseConfig;
  }

  /**
   * Config type, indicates whether this instance config is a Google or User Managed Configuration.
   */
  public Type getConfigType() {
    return configType;
  }

  /**
   * etag, which is used for optimistic concurrency control as a way to help prevent simultaneous
   * updates of an instance config from overwriting each other.
   */
  public String getEtag() {
    return etag;
  }

  /**
   * If true, the instance config is being created or updated. If false, there are no ongoing
   * operations for the instance config.
   */
  public boolean isReconciling() {
    return reconciling;
  }

  /** The current instance config state. */
  public State getState() {
    return state;
  }

  /**
   * Cloud Labels, which can be used to filter collections of resources. They can be used to control
   * how resource metrics are aggregated.
   */
  public Map<String, String> getLabels() {
    return labels;
  }

  /** Builder for {@code InstanceConfigInfo}. */
  public abstract static class Builder {
    public abstract Builder setInstanceConfigId(InstanceConfigId configId);

    public abstract Builder setDisplayName(String displayName);

    public abstract Builder addAllReplicas(List<ReplicaInfo> replicas);

    public abstract Builder addAllOptionalReplicas(List<ReplicaInfo> optionalReplicas);

    public abstract Builder setBaseConfig(String baseConfig);

    public abstract Builder addAllLeaderOptions(List<String> leaderOptions);

    public abstract Builder setConfigType(Type configType);

    public abstract Builder setState(State state);

    public abstract Builder setEtag(String etag);

    public abstract Builder setReconciling(boolean reconciling);

    public abstract Builder addLabel(String key, String value);

    public abstract Builder putAllLabels(Map<String, String> labels);

    public abstract InstanceConfigInfo build();
  }

  static class BuilderImpl extends Builder {
    private InstanceConfigId id;
    private String displayName;
    private List<ReplicaInfo> replicas;
    private List<String> leaderOptions;
    private List<ReplicaInfo> optionalReplicas;
    private String baseConfig;
    private Type configType;
    private String etag;
    private boolean reconciling;
    private State state;
    private Map<String, String> labels;

    BuilderImpl(InstanceConfigId id) {
      this.id = id;
      this.labels = new HashMap<>();
      this.replicas = new ArrayList<>();
      this.leaderOptions = new ArrayList<>();
      this.optionalReplicas = new ArrayList<>();
      this.state = State.STATE_UNSPECIFIED;
      this.reconciling = false;
      this.configType = Type.TYPE_UNSPECIFIED;
      this.displayName = "";
      this.baseConfig = "";
      this.etag = "";
    }

    BuilderImpl(InstanceConfigInfo instanceConfigInfo) {
      this.id = instanceConfigInfo.id;
      this.displayName = instanceConfigInfo.displayName;
      this.replicas = instanceConfigInfo.replicas;
      this.leaderOptions = instanceConfigInfo.leaderOptions;
      this.optionalReplicas = instanceConfigInfo.optionalReplicas;
      this.baseConfig = instanceConfigInfo.baseConfig;
      this.configType = instanceConfigInfo.configType;
      this.etag = instanceConfigInfo.etag;
      this.reconciling = instanceConfigInfo.reconciling;
      this.state = instanceConfigInfo.state;
      this.labels = new HashMap<>(instanceConfigInfo.labels);
    }

    @Override
    public BuilderImpl setInstanceConfigId(InstanceConfigId id) {
      this.id = id;
      return this;
    }

    @Override
    public BuilderImpl setDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    @Override
    public BuilderImpl addAllReplicas(List<ReplicaInfo> replicas) {
      this.replicas = replicas;
      return this;
    }

    @Override
    public BuilderImpl addAllLeaderOptions(List<String> leaderOptions) {
      this.leaderOptions = leaderOptions;
      return this;
    }

    @Override
    public BuilderImpl addAllOptionalReplicas(List<ReplicaInfo> optionalReplicas) {
      this.optionalReplicas = optionalReplicas;
      return this;
    }

    @Override
    public BuilderImpl setBaseConfig(String baseConfig) {
      this.baseConfig = baseConfig;
      return this;
    }

    @Override
    public BuilderImpl setConfigType(Type configType) {
      this.configType = configType;
      return this;
    }

    @Override
    public BuilderImpl setState(State state) {
      this.state = state;
      return this;
    }

    @Override
    public BuilderImpl setEtag(String etag) {
      this.etag = etag;
      return this;
    }

    @Override
    public BuilderImpl setReconciling(boolean reconciling) {
      this.reconciling = reconciling;
      return this;
    }

    @Override
    public BuilderImpl addLabel(String key, String value) {
      this.labels.put(key, value);
      return this;
    }

    @Override
    public BuilderImpl putAllLabels(Map<String, String> labels) {
      this.labels.putAll(labels);
      return this;
    }

    @Override
    public InstanceConfigInfo build() {
      return new InstanceConfigInfo(this);
    }
  }

  public static Builder newBuilder(InstanceConfigId id) {
    return new BuilderImpl(checkNotNull(id));
  }

  public InstanceConfigInfo(InstanceConfigId id, String displayName) {
    this((BuilderImpl) newBuilder(id).setDisplayName(displayName));
  }

  public InstanceConfigInfo(
      InstanceConfigId id,
      String displayName,
      List<ReplicaInfo> replicas,
      List<String> leaderOptions) {
    this(
        (BuilderImpl)
            newBuilder(id)
                .setDisplayName(displayName)
                .addAllReplicas(replicas)
                .addAllLeaderOptions(leaderOptions));
  }

  InstanceConfigInfo(BuilderImpl builder) {
    this.id = builder.id;
    this.displayName = builder.displayName;
    this.replicas = builder.replicas;
    this.leaderOptions = builder.leaderOptions;
    this.baseConfig = builder.baseConfig;
    this.optionalReplicas = builder.optionalReplicas;
    this.configType = builder.configType;
    this.etag = builder.etag;
    this.reconciling = builder.reconciling;
    this.state = builder.state;
    this.labels = ImmutableMap.copyOf(builder.labels);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InstanceConfigInfo)) {
      return false;
    }
    InstanceConfigInfo that = (InstanceConfigInfo) o;
    return Objects.equals(id, that.id)
        && Objects.equals(displayName, that.displayName)
        && Objects.equals(replicas, that.replicas)
        && Objects.equals(leaderOptions, that.leaderOptions)
        && Objects.equals(optionalReplicas, that.optionalReplicas)
        && Objects.equals(baseConfig, that.baseConfig)
        && Objects.equals(configType, that.configType)
        && Objects.equals(etag, that.etag)
        && Objects.equals(reconciling, that.reconciling)
        && Objects.equals(state, that.state)
        && Objects.equals(labels, that.labels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        displayName,
        replicas,
        leaderOptions,
        optionalReplicas,
        baseConfig,
        configType,
        etag,
        reconciling,
        state,
        labels);
  }

  public Builder toBuilder() {
    return new BuilderImpl(this);
  }

  @Override
  public String toString() {
    return String.format(
        "Instance Config[%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s]",
        id,
        displayName,
        replicas,
        leaderOptions,
        optionalReplicas,
        baseConfig,
        configType,
        etag,
        reconciling,
        state,
        labels);
  }

  com.google.spanner.admin.instance.v1.InstanceConfig toProto() {
    // TODO: add other fields here.
    com.google.spanner.admin.instance.v1.InstanceConfig.Builder builder =
        com.google.spanner.admin.instance.v1.InstanceConfig.newBuilder().setName(getId().getName());
    if (getDisplayName() != null) {
      builder.setDisplayName(getDisplayName());
    }
    if (getBaseConfig() != null && !getBaseConfig().isEmpty()) {
      builder.setBaseConfig(getBaseConfig());
    }
    if (getReplicas() != null) {
      builder.addAllReplicas(
          getReplicas().stream().map(ReplicaInfo::getProto).collect(Collectors.toList()));
    }
    return builder.build();
  }
}
