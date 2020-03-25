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

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.Policy;
import com.google.cloud.spanner.Options.ListOption;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.List;
import javax.annotation.Nullable;

/** Client to do admin operations on a Cloud Spanner Database. */
public interface DatabaseAdminClient {
  /**
   * Creates a new database in a Cloud Spanner instance.
   *
   * <p>Example to create database.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * String databaseId = my_database_id;
   * Operation<Database, CreateDatabaseMetadata> op = dbAdminClient
   *     .createDatabase(
   *         instanceId,
   *         databaseId,
   *         Arrays.asList(
   *             "CREATE TABLE Singers (\n"
   *                 + "  SingerId   INT64 NOT NULL,\n"
   *                 + "  FirstName  STRING(1024),\n"
   *                 + "  LastName   STRING(1024),\n"
   *                 + "  SingerInfo BYTES(MAX)\n"
   *                 + ") PRIMARY KEY (SingerId)",
   *             "CREATE TABLE Albums (\n"
   *                 + "  SingerId     INT64 NOT NULL,\n"
   *                 + "  AlbumId      INT64 NOT NULL,\n"
   *                 + "  AlbumTitle   STRING(MAX)\n"
   *                 + ") PRIMARY KEY (SingerId, AlbumId),\n"
   *                 + "  INTERLEAVE IN PARENT Singers ON DELETE CASCADE"));
   * Database db = op.waitFor().getResult();
   * }</pre>
   *
   * @param instanceId the id of the instance in which to create the database.
   * @param databaseId the id of the database which will be created. It must conform to the regular
   *     expression [a-z][a-z0-9_\-]*[a-z0-9] and be between 2 and 30 characters in length
   * @param statements DDL statements to run while creating the database, for example {@code CREATE
   *     TABLE MyTable ( ... )}. This should not include {@code CREATE DATABASE} statement.
   */
  OperationFuture<Database, CreateDatabaseMetadata> createDatabase(
      String instanceId, String databaseId, Iterable<String> statements) throws SpannerException;

  /**
   * Gets the current state of a Cloud Spanner database.
   *
   * <p>Example to getDatabase.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * String databaseId = my_database_id;
   * Database db = dbAdminClient.getDatabase(instanceId, databaseId);
   * }</pre>
   */
  Database getDatabase(String instanceId, String databaseId) throws SpannerException;

  /**
   * Enqueues the given DDL statements to be applied, in order but not necessarily all at once, to
   * the database schema at some point (or points) in the future. The server checks that the
   * statements are executable (syntactically valid, name tables that exist, etc.) before enqueueing
   * them, but they may still fail upon later execution (e.g., if a statement from another batch of
   * statements is applied first and it conflicts in some way, or if there is some data-related
   * problem like a `NULL` value in a column to which `NOT NULL` would be added). If a statement
   * fails, all subsequent statements in the batch are automatically cancelled.
   *
   * <p>Example to update the database DDL.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * String databaseId = my_database_id;
   * dbAdminClient.updateDatabaseDdl(instanceId,
   *     databaseId,
   *     Arrays.asList("ALTER TABLE Albums ADD COLUMN MarketingBudget INT64"),
   *     null).waitFor();
   * }</pre>
   *
   * @param operationId Operation id assigned to this operation. If null, system will autogenerate
   *     one. This must be unique within a database abd must be a valid identifier
   *     [a-zA-Z][a-zA-Z0-9_]*.
   */
  OperationFuture<Void, UpdateDatabaseDdlMetadata> updateDatabaseDdl(
      String instanceId,
      String databaseId,
      Iterable<String> statements,
      @Nullable String operationId)
      throws SpannerException;

  /**
   * Drops a Cloud Spanner database.
   *
   * <p>Example to drop a Cloud Spanner database.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * String databaseId = my_database_id;
   * dbAdminClient.dropDatabase(instanceId, databaseId);
   * }</pre>
   */
  void dropDatabase(String instanceId, String databaseId) throws SpannerException;

  /**
   * Returns the schema of a Cloud Spanner database as a list of formatted DDL statements. This
   * method does not show pending schema updates.
   *
   * <p>Example to get the schema of a Cloud Spanner database.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * String databaseId = my_database_id;
   * List<String> statementsInDb = dbAdminClient.getDatabaseDdl(instanceId, databaseId);
   * }</pre>
   */
  List<String> getDatabaseDdl(String instanceId, String databaseId);

  /**
   * Returns the list of Cloud Spanner database in the given instance.
   *
   * <p>Example to get the list of Cloud Spanner database in the given instance.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * Page<Database> page = dbAdminClient.listDatabases(instanceId, Options.pageSize(1));
   * List<Database> dbs = new ArrayList<>();
   * while (page != null) {
   *   Database db = Iterables.getOnlyElement(page.getValues());
   *   dbs.add(db);
   *   page = page.getNextPage();
   * }
   * }</pre>
   */
  Page<Database> listDatabases(String instanceId, ListOption... options);

  /** Returns the IAM policy for the given database. */
  Policy getDatabaseIAMPolicy(String instanceId, String databaseId);

  /**
   * Updates the IAM policy for the given database and returns the resulting policy. It is highly
   * recommended to first get the current policy and base the updated policy on the returned policy.
   * See {@link Policy.Builder#setEtag(String)} for information on the recommended read-modify-write
   * cycle.
   */
  Policy setDatabaseIAMPolicy(String instanceId, String databaseId, Policy policy);

  /**
   * Tests for the given permissions on the specified database for the caller.
   *
   * @param instanceId the id of the instance where the database to test is located.
   * @param databaseId the id of the database to test.
   * @param permissions the permissions to test for. Permissions with wildcards (such as '*',
   *     'spanner.*', 'spanner.instances.*') are not allowed.
   * @return the subset of the tested permissions that the caller is allowed.
   */
  Iterable<String> testDatabaseIAMPermissions(
      String instanceId, String databaseId, Iterable<String> permissions);
}
