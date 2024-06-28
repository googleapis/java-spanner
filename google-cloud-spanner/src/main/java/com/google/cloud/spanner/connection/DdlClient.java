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
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.common.base.Preconditions;
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
  private final DatabaseId databaseId;

  static class Builder {
    private DatabaseAdminClient dbAdminClient;

    private DatabaseId databaseId;

    private Builder() {}

    Builder setDatabaseAdminClient(DatabaseAdminClient client) {
      Preconditions.checkNotNull(client);
      this.dbAdminClient = client;
      return this;
    }

    Builder setDatabaseId(DatabaseId databaseId) {
      Preconditions.checkNotNull(databaseId);
      this.databaseId = databaseId;
      return this;
    }

    DdlClient build() {
      Preconditions.checkState(dbAdminClient != null, "No DatabaseAdminClient specified");
      Preconditions.checkState(databaseId != null, "No DatabaseId specified");
      return new DdlClient(this);
    }
  }

  static Builder newBuilder() {
    return new Builder();
  }

  private DdlClient(Builder builder) {
    this.dbAdminClient = builder.dbAdminClient;
    this.databaseId = builder.databaseId;
  }

  OperationFuture<Database, CreateDatabaseMetadata> executeCreateDatabase(
      String createStatement, Dialect dialect) {
    Preconditions.checkArgument(isCreateDatabaseStatement(createStatement));
    return dbAdminClient.createDatabase(
        databaseId.getInstanceId().getInstance(),
        createStatement,
        dialect,
        Collections.emptyList());
  }

  /** Execute a single DDL statement. */
  OperationFuture<Void, UpdateDatabaseDdlMetadata> executeDdl(String ddl, byte[] protoDescriptors) {
    return executeDdl(Collections.singletonList(ddl), protoDescriptors);
  }

  /** Execute a list of DDL statements as one operation. */
  OperationFuture<Void, UpdateDatabaseDdlMetadata> executeDdl(
      List<String> statements, byte[] protoDescriptors) {
    if (statements.stream().anyMatch(DdlClient::isCreateDatabaseStatement)) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, "CREATE DATABASE is not supported in a DDL batch");
    }
    Database.Builder dbBuilder = dbAdminClient.newDatabaseBuilder(databaseId);
    if (protoDescriptors != null) {
      dbBuilder.setProtoDescriptors(protoDescriptors);
    }
    Database db = dbBuilder.build();
    return dbAdminClient.updateDatabaseDdl(db, statements, null);
  }

  /** Returns true if the statement is a `CREATE DATABASE ...` statement. */
  static boolean isCreateDatabaseStatement(String statement) {
    String[] tokens = statement.split("\\s+", 3);
    return tokens.length >= 2
        && tokens[0].equalsIgnoreCase("CREATE")
        && tokens[1].equalsIgnoreCase("DATABASE");
  }
}
