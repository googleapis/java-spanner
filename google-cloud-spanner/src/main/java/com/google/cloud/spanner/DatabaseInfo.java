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

import com.google.cloud.Timestamp;
import com.google.common.base.Preconditions;
import java.util.Objects;
import javax.annotation.Nullable;

/** Represents a Cloud Spanner database. */
public class DatabaseInfo {
  public abstract static class Builder {
    abstract Builder setState(State state);

    abstract Builder setCreateTime(Timestamp createTime);

    abstract Builder setRestoreInfo(RestoreInfo restoreInfo);

    abstract Builder setVersionRetentionPeriod(String versionRetentionPeriod);

    abstract Builder setEarliestVersionTime(Timestamp earliestVersionTime);

    abstract Builder setProto(com.google.spanner.admin.database.v1.Database proto);

    /** Builds the database from this builder. */
    public abstract Database build();
  }

  abstract static class BuilderImpl extends Builder {
    protected final DatabaseId id;
    private State state = State.UNSPECIFIED;
    private Timestamp createTime;
    private RestoreInfo restoreInfo;
    private String versionRetentionPeriod;
    private Timestamp earliestVersionTime;
    private com.google.spanner.admin.database.v1.Database proto;

    BuilderImpl(DatabaseId id) {
      this.id = Preconditions.checkNotNull(id);
    }

    BuilderImpl(DatabaseInfo other) {
      this.id = other.id;
      this.state = other.state;
      this.createTime = other.createTime;
      this.restoreInfo = other.restoreInfo;
      this.versionRetentionPeriod = other.versionRetentionPeriod;
      this.earliestVersionTime = other.earliestVersionTime;
      this.proto = other.proto;
    }

    @Override
    Builder setState(State state) {
      this.state = Preconditions.checkNotNull(state);
      return this;
    }

    @Override
    Builder setCreateTime(Timestamp createTime) {
      this.createTime = Preconditions.checkNotNull(createTime);
      return this;
    }

    @Override
    Builder setRestoreInfo(@Nullable RestoreInfo restoreInfo) {
      this.restoreInfo = restoreInfo;
      return this;
    }

    @Override
    Builder setVersionRetentionPeriod(String versionRetentionPeriod) {
      this.versionRetentionPeriod = versionRetentionPeriod;
      return this;
    }

    @Override
    Builder setEarliestVersionTime(Timestamp earliestVersionTime) {
      this.earliestVersionTime = earliestVersionTime;
      return this;
    }

    @Override
    Builder setProto(@Nullable com.google.spanner.admin.database.v1.Database proto) {
      this.proto = proto;
      return this;
    }
  }

  /** State of the database. */
  public enum State {
    // Not specified.
    UNSPECIFIED,
    // The database is still being created and is not ready to use.
    CREATING,
    // The database is fully created and ready to use.
    READY,
    // The database has restored and is being optimized for use.
    READY_OPTIMIZING
  }

  private final DatabaseId id;
  private final State state;
  private final Timestamp createTime;
  private final RestoreInfo restoreInfo;
  private final String versionRetentionPeriod;
  private final Timestamp earliestVersionTime;
  private final com.google.spanner.admin.database.v1.Database proto;

  public DatabaseInfo(DatabaseId id, State state) {
    this.id = id;
    this.state = state;
    this.createTime = null;
    this.restoreInfo = null;
    this.versionRetentionPeriod = null;
    this.earliestVersionTime = null;
    this.proto = null;
  }

  DatabaseInfo(BuilderImpl builder) {
    this.id = builder.id;
    this.state = builder.state;
    this.createTime = builder.createTime;
    this.restoreInfo = builder.restoreInfo;
    this.versionRetentionPeriod = builder.versionRetentionPeriod;
    this.earliestVersionTime = builder.earliestVersionTime;
    this.proto = builder.proto;
  }

  /** Returns the database id. */
  public DatabaseId getId() {
    return id;
  }

  /** Returns the state of the database. */
  public State getState() {
    return state;
  }

  /** Returns the creation time of the database. */
  public Timestamp getCreateTime() {
    return createTime;
  }

  /** Returns the version retention period of the database. */
  public String getVersionRetentionPeriod() {
    return versionRetentionPeriod;
  }

  /** Returns the earliest version time of the database. */
  public Timestamp getEarliestVersionTime() {
    return earliestVersionTime;
  }

  /**
   * Returns the {@link RestoreInfo} of the database if any is available, or <code>null</code> if no
   * {@link RestoreInfo} is available for this database.
   */
  public @Nullable RestoreInfo getRestoreInfo() {
    return restoreInfo;
  }

  /** Returns the raw proto instance that was used to construct this {@link Database}. */
  public @Nullable com.google.spanner.admin.database.v1.Database getProto() {
    return proto;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DatabaseInfo that = (DatabaseInfo) o;
    return id.equals(that.id)
        && state == that.state
        && Objects.equals(createTime, that.createTime)
        && Objects.equals(restoreInfo, that.restoreInfo)
        && Objects.equals(versionRetentionPeriod, that.versionRetentionPeriod)
        && Objects.equals(earliestVersionTime, that.earliestVersionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        state,
        createTime,
        restoreInfo,
        versionRetentionPeriod,
        earliestVersionTime);
  }

  @Override
  public String toString() {
    return String.format(
        "Database[%s, %s, %s, %s, %s, %s]",
        id.getName(),
        state,
        createTime,
        restoreInfo,
        versionRetentionPeriod,
        earliestVersionTime);
  }
}
