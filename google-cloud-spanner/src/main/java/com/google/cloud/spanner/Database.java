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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.Policy;
import com.google.cloud.Timestamp;
import com.google.common.base.Preconditions;
import com.google.longrunning.Operation;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;

/**
 * Represents a Cloud Spanner database. {@code Database} adds a layer of service related
 * functionality over {@code DatabaseInfo}.
 */
public class Database extends DatabaseInfo {
  public static class Builder extends DatabaseInfo.BuilderImpl {
    private final DatabaseAdminClient dbClient;

    Builder(DatabaseAdminClient dbClient, DatabaseId databaseId) {
      super(databaseId);
      this.dbClient = Preconditions.checkNotNull(dbClient);
    }

    private Builder(Database database) {
      super(database);
      this.dbClient = database.dbClient;
    }

    @Override
    public Database build() {
      return new Database(this);
    }
  }

  private static final String FILTER_DB_OPERATIONS_TEMPLATE = "name:databases/%s";
  private final DatabaseAdminClient dbClient;

  public Database(DatabaseId id, State state, DatabaseAdminClient dbClient) {
    super(id, state);
    this.dbClient = dbClient;
  }

  Database(Builder builder) {
    super(builder);
    this.dbClient = Preconditions.checkNotNull(builder.dbClient);
  }

  /** Fetches the database's current information. */
  public Database reload() throws SpannerException {
    return dbClient.getDatabase(instance(), database());
  }

  /**
   * Enqueues the given DDL statements to be applied, in order but not necessarily all at once, to
   * the database schema at some point (or points) in the future. The server checks that the
   * statements are executable (syntactically valid, name tables that exist, etc.) before enqueueing
   * them, but they may still fail upon later execution (e.g., if a statement from another batch of
   * statements is applied first and it conflicts in some way, or if there is some data-related
   * problem like a `NULL` value in a column to which `NOT NULL` would be added). If a statement
   * fails, all subsequent statements in the batch are automatically cancelled.
   *
   * @param operationId Operation id assigned to this operation. If null, system will autogenerate
   *     one. This must be unique within a database abd must be a valid identifier
   *     [a-zA-Z][a-zA-Z0-9_]*.
   */
  public OperationFuture<Void, UpdateDatabaseDdlMetadata> updateDdl(
      Iterable<String> statements, String operationId) throws SpannerException {
    return dbClient.updateDatabaseDdl(instance(), database(), statements, operationId);
  }

  /** Drops this database. */
  public void drop() throws SpannerException {
    dbClient.dropDatabase(instance(), database());
  }

  /**
   * Returns <code>true</code> if a database with the id of this {@link Database} exists on Cloud
   * Spanner.
   */
  public boolean exists() {
    try {
      dbClient.getDatabase(instance(), database());
    } catch (SpannerException e) {
      if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
        return false;
      }
      throw e;
    }
    return true;
  }

  /**
   * Backs up this database to the location specified by the {@link Backup}. The given {@link
   * Backup} must have an expire time. The backup must belong to the same instance as this database.
   */
  public OperationFuture<Backup, CreateBackupMetadata> backup(Backup backup) {
    Preconditions.checkArgument(
        backup.getExpireTime() != null, "The backup does not have an expire time.");
    Preconditions.checkArgument(
        backup.getInstanceId().equals(getId().getInstanceId()),
        "The instance of the backup must be equal to the instance of this database.");
    return dbClient.createBackup(
        instance(), backup.getId().getBackup(), database(), backup.getExpireTime());
  }

  /**
   * Returns the schema of a Cloud Spanner database as a list of formatted DDL statements. This
   * method does not show pending schema updates.
   */
  public Iterable<String> getDdl() throws SpannerException {
    return dbClient.getDatabaseDdl(instance(), database());
  }

  /** Returns the long-running operations for this database. */
  public Page<Operation> listDatabaseOperations() {
    return dbClient.listDatabaseOperations(
        instance(), Options.filter(String.format(FILTER_DB_OPERATIONS_TEMPLATE, database())));
  }

  /** Returns the IAM {@link Policy} for this database. */
  public Policy getIAMPolicy() {
    return dbClient.getDatabaseIAMPolicy(instance(), database());
  }

  /**
   * Updates the IAM policy for this database and returns the resulting policy. It is highly
   * recommended to first get the current policy and base the updated policy on the returned policy.
   * See {@link Policy.Builder#setEtag(String)} for information on the recommended read-modify-write
   * cycle.
   */
  public Policy setIAMPolicy(Policy policy) {
    return dbClient.setDatabaseIAMPolicy(instance(), database(), policy);
  }

  /**
   * Tests for the given permissions on this database for the caller.
   *
   * @param permissions the permissions to test for. Permissions with wildcards (such as '*',
   *     'spanner.*', 'spanner.instances.*') are not allowed.
   * @return the subset of the tested permissions that the caller is allowed.
   */
  public Iterable<String> testIAMPermissions(Iterable<String> permissions) {
    return dbClient.testDatabaseIAMPermissions(instance(), database(), permissions);
  }

  private String instance() {
    return getId().getInstanceId().getInstance();
  }

  private String database() {
    return getId().getDatabase();
  }

  static Database fromProto(
      com.google.spanner.admin.database.v1.Database proto, DatabaseAdminClient client) {
    checkArgument(!proto.getName().isEmpty(), "Missing expected 'name' field");
    return new Database.Builder(client, DatabaseId.of(proto.getName()))
        .setState(fromProtoState(proto.getState()))
        .setCreateTime(Timestamp.fromProto(proto.getCreateTime()))
        .setRestoreInfo(RestoreInfo.fromProtoOrNullIfDefaultInstance(proto.getRestoreInfo()))
        .setEncryptionConfigInfo(
            EncryptionConfigInfo.fromProtoOrNullIfDefaultInstance(proto.getEncryptionConfig()))
        .setProto(proto)
        .build();
  }

  static DatabaseInfo.State fromProtoState(
      com.google.spanner.admin.database.v1.Database.State protoState) {
    switch (protoState) {
      case STATE_UNSPECIFIED:
        return DatabaseInfo.State.UNSPECIFIED;
      case CREATING:
        return DatabaseInfo.State.CREATING;
      case READY:
        return DatabaseInfo.State.READY;
      case READY_OPTIMIZING:
        return DatabaseInfo.State.READY_OPTIMIZING;
      default:
        throw new IllegalArgumentException("Unrecognized state " + protoState);
    }
  }
}
