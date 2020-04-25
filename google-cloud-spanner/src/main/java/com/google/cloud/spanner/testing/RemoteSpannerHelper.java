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

package com.google.cloud.spanner.testing;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.spanner.BatchClient;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerClient.ListSessionsPagedResponse;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utility that provides access to a Cloud Spanner instance to use for tests, and allows uniquely
 * named test databases to be created within that instance.
 */
public class RemoteSpannerHelper {
  private static final Logger logger = Logger.getLogger(RemoteSpannerHelper.class.getName());

  private final SpannerOptions options;
  private final Spanner client;
  private final InstanceId instanceId;
  private static int dbSeq;
  private static int dbPrefix = new Random().nextInt(Integer.MAX_VALUE);
  private final List<Database> dbs = new ArrayList<>();

  protected RemoteSpannerHelper(SpannerOptions options, InstanceId instanceId, Spanner client) {
    this.options = options;
    this.instanceId = instanceId;
    this.client = client;
  }

  public SpannerOptions getOptions() {
    return options;
  }

  public Spanner getClient() {
    return client;
  }

  public DatabaseClient getDatabaseClient(Database db) {
    return getClient().getDatabaseClient(db.getId());
  }

  public BatchClient getBatchClient(Database db) {
    return getClient().getBatchClient(db.getId());
  }

  public InstanceId getInstanceId() {
    return instanceId;
  }

  /**
   * Creates a test database defined by {@code statements}. A {@code CREATE DATABASE ...} statement
   * should not be included; an appropriate name will be chosen and the statement generated
   * accordingly.
   */
  public Database createTestDatabase(String... statements) throws SpannerException {
    return createTestDatabase(Arrays.asList(statements));
  }

  /**
   * Returns a database id which is guaranteed to be unique within the context of this environment.
   */
  public String getUniqueDatabaseId() {
    return String.format("testdb_%d_%04d", dbPrefix, dbSeq++);
  }

  /**
   * Creates a test database defined by {@code statements} in the test instance. A {@code CREATE
   * DATABASE ...} statement should not be included; an appropriate name will be chosen and the
   * statement generated accordingly.
   */
  public Database createTestDatabase(Iterable<String> statements) throws SpannerException {
    String dbId = getUniqueDatabaseId();
    try {
      OperationFuture<Database, CreateDatabaseMetadata> op =
          client
              .getDatabaseAdminClient()
              .createDatabase(instanceId.getInstance(), dbId, statements);
      Database db = op.get();
      logger.log(Level.FINE, "Created test database {0}", db.getId());
      dbs.add(db);
      return db;
    } catch (Exception e) {
      throw SpannerExceptionFactory.newSpannerException(e);
    }
  }

  /** Deletes all the databases created via {@code createTestDatabase}. Shuts down the client. */
  public void cleanUp(boolean dropOrphanedTestDbs) {
    try {
      // Drop all the databases we created explicitly.
      int numDropped = 0;
      for (Database db : dbs) {
        try {
          logger.log(Level.FINE, "Dropping test database {0}", db.getId());
          db.drop();
          ++numDropped;
        } catch (SpannerException e) {
          logger.log(Level.SEVERE, "Failed to drop test database " + db.getId(), e);
        }
      }
      logger.log(Level.INFO, "Dropped {0} test database(s)", numDropped);

      if (dropOrphanedTestDbs) {
        try {
          // Also drop all old test databases from other test runs that failed to be cleaned up.
          int numOrphanedDropped = 0;
          DatabaseAdminClient dbAdminClient = client.getDatabaseAdminClient();
          SpannerClient spannerClient =
              SpannerClient.create(
                  SpannerSettings.newBuilder()
                      .setCredentialsProvider(
                          FixedCredentialsProvider.create(
                              GoogleCredentials.getApplicationDefault()))
                      .build());
          Pattern pattern = Pattern.compile("(.*)/databases/testdb_(?:\\d+)_(?:\\d+)(?:_db\\d)?");
          long minSeconds =
              System.currentTimeMillis() / 1000L - TimeUnit.SECONDS.convert(24L, TimeUnit.HOURS);
          for (Database db :
              dbAdminClient.listDatabases(getInstanceId().getInstance()).iterateAll()) {
            // Only delete test databases generated by the integration test environment.
            if (pattern.matcher(db.getId().getName()).matches()) {
              if (db.getCreateTime().getSeconds() < minSeconds) {
                ListSessionsPagedResponse sessions =
                    spannerClient.listSessions(db.getId().getName());
                if (sessions.getPage().getPageElementCount() == 0) {
                  try {
                    logger.log(Level.FINE, "Dropping orphaned test database {0}", db.getId());
                    db.drop();
                    ++numOrphanedDropped;
                  } catch (SpannerException e) {
                    logger.log(
                        Level.SEVERE, "Failed to drop orphaned test database " + db.getId(), e);
                  }
                }
              }
            }
          }
          logger.log(Level.INFO, "Dropped {0} orphaned test database(s)", numOrphanedDropped);
        } catch (Throwable t) {
          logger.log(
              Level.SEVERE,
              "Failed to drop orphaned test databases from existing test instance",
              t);
        }
      }
    } finally {
      client.close();
    }
  }

  /**
   * Creates a {@code RemoteSpannerHelper} bound to the given instance ID. All databases created
   * using this will be created in the given instance.
   */
  public static RemoteSpannerHelper create(InstanceId instanceId) throws Throwable {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId(instanceId.getProject())
            .setAutoThrottleAdministrativeRequests()
            .build();
    Spanner client = options.getService();
    return new RemoteSpannerHelper(options, instanceId, client);
  }

  /**
   * Creates a {@code RemoteSpannerHelper} for the given option and bound to the given instance ID.
   * All databases created using this will be created in the given instance.
   */
  public static RemoteSpannerHelper create(SpannerOptions options, InstanceId instanceId)
      throws Throwable {
    Spanner client = options.getService();
    return new RemoteSpannerHelper(options, instanceId, client);
  }
}
