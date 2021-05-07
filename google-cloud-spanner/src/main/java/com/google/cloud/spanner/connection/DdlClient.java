/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.connection;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseNotFoundException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.Collections;
import java.util.List;

/**
 * Convenience class for executing Data Definition Language statements on transactions that support
 * DDL statements, i.e. DdlBatchTransaction and SingleUseTransaction.
 */
class DdlClient {
  private final DatabaseAdminClient dbAdminClient;
  private final String instanceId;
  private String databaseId;

  static class Builder {
    private DatabaseAdminClient dbAdminClient;
    private String instanceId;
    private String databaseId;

    private Builder() {}

    Builder setDatabaseAdminClient(DatabaseAdminClient client) {
      Preconditions.checkNotNull(client);
      this.dbAdminClient = client;
      return this;
    }

    Builder setInstanceId(String instanceId) {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(instanceId), "Empty instanceId is not allowed");
      this.instanceId = instanceId;
      return this;
    }

    Builder setDatabaseId(String databaseId) {
      this.databaseId = databaseId;
      return this;
    }

    DdlClient build() {
      Preconditions.checkState(dbAdminClient != null, "No DatabaseAdminClient specified");
      Preconditions.checkState(!Strings.isNullOrEmpty(instanceId), "No InstanceId specified");
      return new DdlClient(this);
    }
  }

  static Builder newBuilder() {
    return new Builder();
  }

  private DdlClient(Builder builder) {
    this.dbAdminClient = builder.dbAdminClient;
    this.instanceId = builder.instanceId;
    this.databaseId = builder.databaseId;
  }

  void setDefaultDatabaseId(String databaseId) {
    this.databaseId = databaseId;
  }

  /**
   * Gets the metadata of the given database on the instance that the DdlClient is connected to.
   *
   * @throws DatabaseNotFoundException if the database could not be found
   */
  Database getDatabase(String databaseId) {
    return dbAdminClient.getDatabase(instanceId, databaseId);
  }

  /** Gets the list of databases on the instance that the DdlClient is connected to. */
  Iterable<Database> listDatabases() {
    return dbAdminClient.listDatabases(instanceId).iterateAll();
  }

  /** Execute a single DDL statement. */
  OperationFuture<Void, UpdateDatabaseDdlMetadata> executeDdl(String ddl) {
    return executeDdl(Collections.singletonList(ddl));
  }

  /** Execute a list of DDL statements as one operation. */
  OperationFuture<Void, UpdateDatabaseDdlMetadata> executeDdl(List<String> statements) {
    ConnectionPreconditions.checkState(
        databaseId != null, "This connection is not connected to a database.");
    return dbAdminClient.updateDatabaseDdl(instanceId, databaseId, statements, null);
  }

  /** Execute a list of DDL statements as one operation on a specific database. */
  OperationFuture<Void, UpdateDatabaseDdlMetadata> executeDdl(
      String databaseId, List<String> statements) {
    return dbAdminClient.updateDatabaseDdl(instanceId, databaseId, statements, null);
  }

  /** Creates a new database. */
  OperationFuture<Database, CreateDatabaseMetadata> createDatabase(
      String databaseId, List<String> additionalStatements) {
    return dbAdminClient.createDatabase(instanceId, databaseId, additionalStatements);
  }

  /** Drops an existing database. */
  void dropDatabase(String databaseId) {
    dbAdminClient.dropDatabase(instanceId, databaseId);
  }
}
