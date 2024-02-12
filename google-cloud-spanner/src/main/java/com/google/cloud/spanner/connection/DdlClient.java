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
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DatabaseDialect;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.InstanceName;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.Collections;
import java.util.List;

/**
 * Convenience class for executing Data Definition Language statements on transactions that support
 * DDL statements, i.e. DdlBatchTransaction and SingleUseTransaction.
 */
class DdlClient {
  private final DatabaseAdminClient dbAdminClient;
  private final String projectId;
  private final String instanceId;
  private final String databaseName;

  static class Builder {
    private DatabaseAdminClient dbAdminClient;
    private String projectId;
    private String instanceId;
    private String databaseName;

    private Builder() {}

    Builder setDatabaseAdminClient(DatabaseAdminClient client) {
      Preconditions.checkNotNull(client);
      this.dbAdminClient = client;
      return this;
    }

    Builder setProjectId(String projectId) {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(projectId), "Empty projectId is not allowed");
      this.projectId = projectId;
      return this;
    }

    Builder setInstanceId(String instanceId) {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(instanceId), "Empty instanceId is not allowed");
      this.instanceId = instanceId;
      return this;
    }

    Builder setDatabaseName(String name) {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(name), "Empty database name is not allowed");
      this.databaseName = name;
      return this;
    }

    DdlClient build() {
      Preconditions.checkState(dbAdminClient != null, "No DatabaseAdminClient specified");
      Preconditions.checkState(!Strings.isNullOrEmpty(projectId), "No ProjectId specified");
      Preconditions.checkState(!Strings.isNullOrEmpty(instanceId), "No InstanceId specified");
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(databaseName), "No database name specified");
      return new DdlClient(this);
    }
  }

  static Builder newBuilder() {
    return new Builder();
  }

  private DdlClient(Builder builder) {
    this.dbAdminClient = builder.dbAdminClient;
    this.projectId = builder.projectId;
    this.instanceId = builder.instanceId;
    this.databaseName = builder.databaseName;
  }

  OperationFuture<Database, CreateDatabaseMetadata> executeCreateDatabase(
      String createStatement, DatabaseDialect databaseDialect) {
    Preconditions.checkArgument(isCreateDatabaseStatement(createStatement));
    CreateDatabaseRequest createDatabaseRequest =
        CreateDatabaseRequest.newBuilder()
            .setParent(InstanceName.of(projectId, instanceId).toString())
            .setCreateStatement(createStatement)
            .setDatabaseDialect(databaseDialect)
            .build();
    return dbAdminClient.createDatabaseAsync(createDatabaseRequest);
  }

  /** Execute a single DDL statement. */
  OperationFuture<Empty, UpdateDatabaseDdlMetadata> executeDdl(String ddl) {
    return executeDdl(Collections.singletonList(ddl));
  }

  /** Execute a list of DDL statements as one operation. */
  OperationFuture<Empty, UpdateDatabaseDdlMetadata> executeDdl(List<String> statements) {
    if (statements.stream().anyMatch(DdlClient::isCreateDatabaseStatement)) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, "CREATE DATABASE is not supported in a DDL batch");
    }
    return dbAdminClient.updateDatabaseDdlAsync(
        DatabaseName.of(projectId, instanceId, databaseName), statements);
  }

  /** Returns true if the statement is a `CREATE DATABASE ...` statement. */
  static boolean isCreateDatabaseStatement(String statement) {
    String[] tokens = statement.split("\\s+", 3);
    return tokens.length >= 2
        && tokens[0].equalsIgnoreCase("CREATE")
        && tokens[1].equalsIgnoreCase("DATABASE");
  }
}
