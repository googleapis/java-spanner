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
import java.util.stream.Collectors;

/**
 * Represents a Cloud Spanner instance config.{@code InstanceConfig} adds a layer of service related
 * functionality over {@code InstanceConfigInfo}.
 */
public class InstanceConfig extends InstanceConfigInfo {

  private final InstanceAdminClient client;

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

  /** Gets the current state of this instance config. */
  public InstanceConfig reload() {
    return client.getInstanceConfig(getId().getInstanceConfig());
  }

  static InstanceConfig fromProto(
      com.google.spanner.admin.instance.v1.InstanceConfig proto, InstanceAdminClient client) {
    return new InstanceConfig(
        InstanceConfigId.of(proto.getName()),
        proto.getDisplayName(),
        proto.getReplicasList().stream().map(ReplicaInfo::fromProto).collect(Collectors.toList()),
        proto.getLeaderOptionsList(),
        client);
  }
}
