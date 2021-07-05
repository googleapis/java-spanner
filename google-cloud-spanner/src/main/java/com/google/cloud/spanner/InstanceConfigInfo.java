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
import java.util.Objects;

/** Represents a Cloud Spanner instance config resource. */
public class InstanceConfigInfo {

  private final InstanceConfigId id;
  private final String displayName;
  private final List<String> leaderOptions;

  public InstanceConfigInfo(InstanceConfigId id, String displayName) {
    this(id, displayName, Collections.emptyList());
  }

  public InstanceConfigInfo(InstanceConfigId id, String displayName, List<String> leaderOptions) {
    this.id = id;
    this.displayName = displayName;
    this.leaderOptions = leaderOptions;
  }

  /*
   * Returns the id of this instance config.
   */
  public InstanceConfigId getId() {
    return id;
  }

  /** Returns the display name of this instance config. */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Allowed values of the default leader schema option for databases in instances that use this
   * instance configuration.
   */
  public List<String> getLeaderOptions() {
    return leaderOptions;
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
        && Objects.equals(leaderOptions, that.leaderOptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, displayName, leaderOptions);
  }

  @Override
  public String toString() {
    return String.format("Instance Config[%s, %s, %s]", id, displayName, leaderOptions);
  }
}
