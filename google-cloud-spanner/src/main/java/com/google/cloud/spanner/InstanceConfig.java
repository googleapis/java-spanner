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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a Cloud Spanner instance config.{@code InstanceConfig} adds a layer of service related
 * functionality over {@code InstanceConfigInfo}.
 */
public class InstanceConfig extends InstanceConfigInfo {

  private final InstanceAdminClient client;

  /** Builder of {@code InstanceConfig}. */
  public static class Builder extends InstanceConfigInfo.Builder {
    private final InstanceAdminClient client;
    private final InstanceConfigInfo.BuilderImpl infoBuilder;

    Builder(InstanceConfig instanceConfig) {
      this.client = instanceConfig.client;
      this.infoBuilder = new InstanceConfigInfo.BuilderImpl(instanceConfig);
    }

    Builder(InstanceAdminClient client, InstanceConfigId id) {
      this.client = client;
      this.infoBuilder = new InstanceConfigInfo.BuilderImpl(id);
    }

    @Override
    public Builder setInstanceConfigId(InstanceConfigId id) {
      infoBuilder.setInstanceConfigId(id);
      return this;
    }

    @Override
    public Builder setDisplayName(String displayName) {
      infoBuilder.setDisplayName(displayName);
      return this;
    }

    @Override
    public Builder addAllReplicas(List<ReplicaInfo> replicas) {
      infoBuilder.addAllReplicas(replicas);
      return this;
    }

    @Override
    public Builder addAllLeaderOptions(List<String> leaderOptions) {
      infoBuilder.addAllLeaderOptions(leaderOptions);
      return this;
    }

    @Override
    public Builder addAllOptionalReplicas(List<ReplicaInfo> optionalReplicas) {
      infoBuilder.addAllOptionalReplicas(optionalReplicas);
      return this;
    }

    @Override
    public Builder setBaseConfig(String baseConfig) {
      infoBuilder.setBaseConfig(baseConfig);
      return this;
    }

    @Override
    public Builder setConfigType(Type configType) {
      infoBuilder.setConfigType(configType);
      return this;
    }

    @Override
    protected Builder setState(State state) {
      infoBuilder.setState(state);
      return this;
    }

    @Override
    public Builder setEtag(String etag) {
      infoBuilder.setEtag(etag);
      return this;
    }

    @Override
    public Builder setReconciling(boolean reconciling) {
      infoBuilder.setReconciling(reconciling);
      return this;
    }

    @Override
    public Builder addLabel(String key, String value) {
      infoBuilder.addLabel(key, value);
      return this;
    }

    @Override
    public Builder putAllLabels(Map<String, String> labels) {
      infoBuilder.putAllLabels(labels);
      return this;
    }

    @Override
    public InstanceConfig build() {
      return new InstanceConfig(this);
    }
  }

  public InstanceConfig(InstanceConfigId id, String displayName, InstanceAdminClient client) {
    this(id, displayName, Collections.emptyList(), Collections.emptyList(), client);
  }

  public InstanceConfig(
      InstanceConfigId id,
      String displayName,
      List<ReplicaInfo> replicas,
      List<String> leaderOptions,
      InstanceAdminClient client) {
    super(id, displayName, replicas, leaderOptions);
    this.client = client;
  }

  InstanceConfig(Builder builder) {
    super(builder.infoBuilder);
    this.client = builder.client;
  }

  /** Gets the current state of this instance config. */
  public InstanceConfig reload() {
    return client.getInstanceConfig(getId().getInstanceConfig());
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  static InstanceConfig fromProto(
      com.google.spanner.admin.instance.v1.InstanceConfig proto, InstanceAdminClient client) {
    Builder builder =
        new Builder(client, InstanceConfigId.of(proto.getName()))
            .setBaseConfig(proto.getBaseConfig())
            .setReconciling(proto.getReconciling())
            .addAllReplicas(
                proto.getReplicasList().stream()
                    .map(ReplicaInfo::fromProto)
                    .collect(Collectors.toList()))
            .setDisplayName(proto.getDisplayName())
            .putAllLabels(proto.getLabelsMap())
            .setEtag(proto.getEtag())
            .addAllLeaderOptions(proto.getLeaderOptionsList())
            .addAllOptionalReplicas(
                proto.getOptionalReplicasList().stream()
                    .map(ReplicaInfo::fromProto)
                    .collect(Collectors.toList()));
    State state;
    switch (proto.getState()) {
      case STATE_UNSPECIFIED:
        state = State.STATE_UNSPECIFIED;
        break;
      case CREATING:
        state = State.CREATING;
        break;
      case READY:
        state = State.READY;
        break;
      default:
        throw new IllegalArgumentException("Unknown state:" + proto.getState());
    }
    builder.setState(state);
    Type type;
    switch (proto.getConfigType()) {
      case TYPE_UNSPECIFIED:
        type = Type.TYPE_UNSPECIFIED;
        break;
      case GOOGLE_MANAGED:
        type = Type.GOOGLE_MANAGED;
        break;
      case USER_MANAGED:
        type = Type.USER_MANAGED;
        break;
      default:
        throw new IllegalArgumentException("Unknown config type:" + proto.getConfigType());
    }
    builder.setConfigType(type);
    return builder.build();
  }
}
