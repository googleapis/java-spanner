/*
 * Copyright 2020 Google LLC
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

import com.google.api.client.util.Preconditions;
import com.google.cloud.Timestamp;
import java.util.Objects;
import javax.annotation.Nullable;

/** Represents a Cloud Spanner database backup. */
public class BackupInfo {
  public abstract static class Builder {
    abstract Builder setState(State state);

    abstract Builder setSize(long size);

    abstract Builder setProto(com.google.spanner.admin.database.v1.Backup proto);

    /**
     * Required for creating a new backup.
     *
     * <p>Sets the expiration time of the backup. The expiration time of the backup, with
     * microseconds granularity that must be at least 6 hours and at most 366 days from the time the
     * request is received. Once the expireTime has passed, Cloud Spanner will delete the backup and
     * free the resources used by the backup.
     */
    public abstract Builder setExpireTime(Timestamp expireTime);

    /**
     * Required for creating a new backup.
     *
     * <p>Sets the source database to use for creating the backup.
     */
    public abstract Builder setDatabase(DatabaseId database);

    /** Builds the backup from this builder. */
    public abstract Backup build();
  }

  abstract static class BuilderImpl extends Builder {
    protected final BackupId id;
    private State state = State.UNSPECIFIED;
    private Timestamp expireTime;
    private DatabaseId database;
    private long size;
    private com.google.spanner.admin.database.v1.Backup proto;

    BuilderImpl(BackupId id) {
      this.id = Preconditions.checkNotNull(id);
    }

    BuilderImpl(BackupInfo other) {
      this.id = other.id;
      this.state = other.state;
      this.expireTime = other.expireTime;
      this.database = other.database;
      this.size = other.size;
      this.proto = other.proto;
    }

    @Override
    Builder setState(State state) {
      this.state = Preconditions.checkNotNull(state);
      return this;
    }

    @Override
    public Builder setExpireTime(Timestamp expireTime) {
      this.expireTime = Preconditions.checkNotNull(expireTime);
      return this;
    }

    @Override
    public Builder setDatabase(DatabaseId database) {
      Preconditions.checkArgument(
          database.getInstanceId().equals(id.getInstanceId()),
          "The instance of the source database must be equal to the instance of the backup.");
      this.database = Preconditions.checkNotNull(database);
      return this;
    }

    @Override
    Builder setSize(long size) {
      this.size = size;
      return this;
    }

    @Override
    Builder setProto(@Nullable com.google.spanner.admin.database.v1.Backup proto) {
      this.proto = proto;
      return this;
    }
  }

  /** State of the backup. */
  public enum State {
    // Not specified.
    UNSPECIFIED,
    // The backup is still being created and is not ready to use.
    CREATING,
    // The backup is fully created and ready to use.
    READY,
  }

  private final BackupId id;
  private final State state;
  private final Timestamp expireTime;
  private final DatabaseId database;
  private final long size;
  private final com.google.spanner.admin.database.v1.Backup proto;

  BackupInfo(BuilderImpl builder) {
    this.id = builder.id;
    this.state = builder.state;
    this.size = builder.size;
    this.expireTime = builder.expireTime;
    this.database = builder.database;
    this.proto = builder.proto;
  }

  /** Returns the backup id. */
  public BackupId getId() {
    return id;
  }

  /** Returns the id of the instance that the backup belongs to. */
  public InstanceId getInstanceId() {
    return id.getInstanceId();
  }

  /** Returns the state of the backup. */
  public State getState() {
    return state;
  }

  /** Returns the size of the backup in bytes. */
  public long getSize() {
    return size;
  }

  /** Returns the expire time of the backup. */
  public Timestamp getExpireTime() {
    return expireTime;
  }

  /** Returns the id of the database that was used to create the backup. */
  public DatabaseId getDatabase() {
    return database;
  }

  /** Returns the raw proto instance that was used to construct this {@link Backup}. */
  public @Nullable com.google.spanner.admin.database.v1.Backup getProto() {
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
    BackupInfo that = (BackupInfo) o;
    return id.equals(that.id)
        && state == that.state
        && size == that.size
        && Objects.equals(expireTime, that.expireTime)
        && Objects.equals(database, that.database);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, state, size, expireTime, database);
  }

  @Override
  public String toString() {
    return String.format(
        "Backup[%s, %s, %d, %s, %s]", id.getName(), state, size, expireTime, database);
  }
}
