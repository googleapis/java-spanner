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
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.ListOption;
import com.google.longrunning.Operation;
import com.google.spanner.admin.database.v1.CopyBackupMetadata;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
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
   * Creates a database in a Cloud Spanner instance. Any configuration options in the {@link
   * Database} instance will be included in the {@link CreateDatabaseRequest}.
   *
   * <p>Example to create an encrypted database.
   *
   * <pre>{@code
   * Database dbInfo =
   *     dbClient
   *         .newDatabaseBuilder(DatabaseId.of("my-project", "my-instance", "my-database"))
   *         .setEncryptionConfig(
   *             EncryptionConfig.ofKey(
   *                 "projects/my-project/locations/some-location/keyRings/my-keyring/cryptoKeys/my-key"))
   *         .build();
   * Operation<Database, CreateDatabaseMetadata> op = dbAdminClient
   *     .createDatabase(
   *         dbInfo,
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
   * @see #createDatabase(String, String, Iterable)
   */
  OperationFuture<Database, CreateDatabaseMetadata> createDatabase(
      Database database, Iterable<String> statements) throws SpannerException;

  /** Returns a builder for a {@code Database} object with the given id. */
  Database.Builder newDatabaseBuilder(DatabaseId id);

  /** Returns a builder for a {@code Backup} object with the given id. */
  Backup.Builder newBackupBuilder(BackupId id);

  /** Returns a builder for a {@link Restore} object with the given source and destination */
  Restore.Builder newRestoreBuilder(BackupId source, DatabaseId destination);

  /**
   * Creates a new backup from a database in a Cloud Spanner instance.
   *
   * <p>Example to create a backup.
   *
   * <pre>{@code
   * String instance       = my_instance_id;
   * String backupId       = my_backup_id;
   * String databaseId     = my_database_id;
   * Timestamp expireTime  = Timestamp.ofTimeMicroseconds(micros);
   * OperationFuture<Backup, CreateBackupMetadata> op = dbAdminClient
   *     .createBackup(
   *         instanceId,
   *         backupId,
   *         databaseId,
   *         expireTime);
   * Backup backup = op.get();
   * }</pre>
   *
   * @param sourceInstanceId the id of the instance where the database to backup is located and
   *     where the backup will be created.
   * @param backupId the id of the backup which will be created. It must conform to the regular
   *     expression [a-z][a-z0-9_\-]*[a-z0-9] and be between 2 and 60 characters in length.
   * @param databaseId the id of the database to backup.
   * @param expireTime the time that the backup will automatically expire.
   */
  OperationFuture<Backup, CreateBackupMetadata> createBackup(
      String sourceInstanceId, String backupId, String databaseId, Timestamp expireTime)
      throws SpannerException;

  /**
   * Creates a new backup from a database in a Cloud Spanner. Any configuration options in the
   * {@link Backup} instance will be included in the {@link
   * com.google.spanner.admin.database.v1.CreateBackupRequest}.
   *
   * <p>Example to create an encrypted backup.
   *
   * <pre>{@code
   * BackupId backupId = BackupId.of("project", "instance", "backup-id");
   * DatabaseId databaseId = DatabaseId.of("project", "instance", "database-id");
   * Timestamp expireTime = Timestamp.ofTimeMicroseconds(expireTimeMicros);
   * Timestamp versionTime = Timestamp.ofTimeMicroseconds(versionTimeMicros);
   * EncryptionConfig encryptionConfig =
   *         EncryptionConfig.ofKey(
   *             "projects/my-project/locations/some-location/keyRings/my-keyring/cryptoKeys/my-key"));
   *
   * Backup backupToCreate = dbAdminClient
   *     .newBackupBuilder(backupId)
   *     .setDatabase(databaseId)
   *     .setExpireTime(expireTime)
   *     .setVersionTime(versionTime)
   *     .setEncryptionConfig(encryptionConfig)
   *     .build();
   *
   * OperationFuture<Backup, CreateBackupMetadata> op = dbAdminClient.createBackup(backupToCreate);
   * Backup createdBackup = op.get();
   * }</pre>
   *
   * @param backup the backup to be created
   */
  OperationFuture<Backup, CreateBackupMetadata> createBackup(Backup backup) throws SpannerException;

  /**
   * Creates a copy of backup from an existing backup in a Cloud Spanner instance.
   *
   * <p>Example to copy a backup.
   *
   * <pre>{@code
   * String instanceId                  ="my_instance_id";
   * String sourceBackupId              ="source_backup_id";
   * String destinationBackupId         ="destination_backup_id";
   * Timestamp expireTime               =Timestamp.ofTimeMicroseconds(micros);
   * OperationFuture<Backup, CopyBackupMetadata> op = dbAdminClient
   *     .copyBackup(
   *         instanceId,
   *         sourceBackupId,
   *         destinationBackupId,
   *         expireTime);
   * Backup backup = op.get();
   * }</pre>
   *
   * @param instanceId the id of the instance where the source backup is located and where the new
   *     backup will be created.
   * @param sourceBackupId the source backup id.
   * @param destinationBackupId the id of the backup which will be created. It must conform to the
   *     regular expression [a-z][a-z0-9_\-]*[a-z0-9] and be between 2 and 60 characters in length.
   * @param expireTime the time that the new backup will automatically expire.
   */
  default OperationFuture<Backup, CopyBackupMetadata> copyBackup(
      String instanceId, String sourceBackupId, String destinationBackupId, Timestamp expireTime) {
    throw new UnsupportedOperationException("Unimplemented");
  }

  /**
   * Creates a copy of backup from an existing backup in Cloud Spanner in the same instance. Any
   * configuration options in the {@link Backup} instance will be included in the {@link
   * com.google.spanner.admin.database.v1.CopyBackupRequest}.
   *
   * <p>The expire time of the new backup must be set and be at least 6 hours and at most 366 days
   * after the creation time of the existing backup that is being copied.
   *
   * <p>Example to create a copy of a backup.
   *
   * <pre>{@code
   * BackupId sourceBackupId = BackupId.of("source-project", "source-instance", "source-backup-id");
   * BackupId destinationBackupId = BackupId.of("destination-project", "destination-instance", "new-backup-id");
   * Timestamp expireTime = Timestamp.ofTimeMicroseconds(expireTimeMicros);
   * EncryptionConfig encryptionConfig =
   *         EncryptionConfig.ofKey(
   *             "projects/my-project/locations/some-location/keyRings/my-keyring/cryptoKeys/my-key"));
   *
   * Backup destinationBackup = dbAdminClient
   *     .newBackupBuilder(destinationBackupId)
   *     .setExpireTime(expireTime)
   *     .setEncryptionConfig(encryptionConfig)
   *     .build();
   *
   * OperationFuture<Backup, CopyBackupMetadata> op = dbAdminClient.copyBackup(sourceBackupId, destinationBackup);
   * Backup copiedBackup = op.get();
   * }</pre>
   *
   * @param sourceBackupId the backup to be copied
   * @param destinationBackup the new backup to create
   */
  default OperationFuture<Backup, CopyBackupMetadata> copyBackup(
      BackupId sourceBackupId, Backup destinationBackup) {
    throw new UnsupportedOperationException("Unimplemented");
  }

  /**
   * Restore a database from a backup. The database that is restored will be created and may not
   * already exist.
   *
   * <p>Example to restore a database.
   *
   * <pre>{@code
   * String backupInstanceId   = my_instance_id;
   * String backupId           = my_backup_id;
   * String restoreInstanceId  = my_db_instance_id;
   * String restoreDatabaseId  = my_database_id;
   * OperationFuture<Database, RestoreDatabaseMetadata> op = dbAdminClient
   *     .restoreDatabase(
   *         backupInstanceId,
   *         backupId,
   *         restoreInstanceId,
   *         restoreDatabaseId);
   * Database database = op.get();
   * }</pre>
   *
   * @param backupInstanceId the id of the instance where the backup is located.
   * @param backupId the id of the backup to restore.
   * @param restoreInstanceId the id of the instance where the database should be created. This may
   *     be a different instance than where the backup is stored.
   * @param restoreDatabaseId the id of the database to restore to.
   */
  OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabase(
      String backupInstanceId, String backupId, String restoreInstanceId, String restoreDatabaseId)
      throws SpannerException;

  /**
   * Restore a database from a backup. The database that is restored will be created and may not
   * already exist.
   *
   * <p>Example to restore an encrypted database.
   *
   * <pre>{@code
   * final Restore restore = dbAdminClient
   *     .newRestoreBuilder(
   *         BackupId.of("my-project", "my-instance", "my-backup"),
   *         DatabaseId.of("my-project", "my-instance", "my-database")
   *     )
   *     .setEncryptionConfig(EncryptionConfig.ofKey(
   *         "projects/my-project/locations/some-location/keyRings/my-keyring/cryptoKeys/my-key"))
   *     .build();
   *
   * final OperationFuture<Database, RestoreDatabaseMetadata> op = dbAdminClient
   *     .restoreDatabase(restore);
   *
   * Database database = op.get();
   * }</pre>
   *
   * @param restore a {@link Restore} instance with the backup source and destination database
   */
  OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabase(Restore restore)
      throws SpannerException;

  /** Lists long-running database operations on the specified instance. */
  Page<Operation> listDatabaseOperations(String instanceId, ListOption... options);

  /** Lists database roles on the specified database. */
  Page<DatabaseRole> listDatabaseRoles(String instanceId, String databaseId, ListOption... options);

  /** Lists long-running backup operations on the specified instance. */
  Page<Operation> listBackupOperations(String instanceId, ListOption... options);

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
   * Gets the current state of a Cloud Spanner database backup.
   *
   * <p>Example to get a backup.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * String backupId   = my_backup_id;
   * Backup backup = dbAdminClient.getBackup(instanceId, backupId);
   * }</pre>
   */
  Backup getBackup(String instanceId, String backupId) throws SpannerException;

  /**
   * Enqueues the given DDL statements to be applied, in order but not necessarily all at once, to
   * the database schema at some point (or points) in the future. The server checks that the
   * statements are executable (syntactically valid, name tables that exist, etc.) before enqueueing
   * them, but they may still fail upon later execution (e.g., if a statement from another batch of
   * statements is applied first and it conflicts in some way, or if there is some data-related
   * problem like a `NULL` value in a column to which `NOT NULL` would be added). If a statement
   * fails, all subsequent statements in the batch are automatically cancelled.
   *
   * <p>If an operation already exists with the given operation id, the operation will be resumed
   * and the returned future will complete when the original operation finishes. See more
   * information in {@link com.google.cloud.spanner.spi.v1.GapicSpannerRpc#updateDatabaseDdl(String,
   * Iterable, String)}
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

  /**
   * Returns the list of Cloud Spanner backups in the given instance.
   *
   * <p>Example to get the list of Cloud Spanner backups in the given instance.
   *
   * <pre>{@code
   * String instanceId = my_instance_id;
   * Page<Backup> page = dbAdminClient.listBackups(instanceId, Options.pageSize(1));
   * List<Backup> backups = new ArrayList<>();
   * while (page != null) {
   *   Backup backup = Iterables.getOnlyElement(page.getValues());
   *   dbs.add(backup);
   *   page = page.getNextPage();
   * }
   * }</pre>
   */
  Page<Backup> listBackups(String instanceId, ListOption... options);

  /**
   * Updates the expire time of a backup.
   *
   * @param instanceId Required. The instance of the backup to update.
   * @param backupId Required. The backup id of the backup to update.
   * @param expireTime Required. The new expire time of the backup to set to.
   * @return the updated Backup object.
   */
  Backup updateBackup(String instanceId, String backupId, Timestamp expireTime);

  /**
   * Deletes a pending or completed backup.
   *
   * @param instanceId Required. The instance where the backup exists.
   * @param backupId Required. The id of the backup to delete.
   */
  void deleteBackup(String instanceId, String backupId);

  /** Cancels the specified long-running operation. */
  void cancelOperation(String name);

  /** Gets the specified long-running operation. */
  Operation getOperation(String name);

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

  /** Returns the IAM policy for the given backup. */
  Policy getBackupIAMPolicy(String instanceId, String backupId);

  /**
   * Updates the IAM policy for the given backup and returns the resulting policy. It is highly
   * recommended to first get the current policy and base the updated policy on the returned policy.
   * See {@link Policy.Builder#setEtag(String)} for information on the recommended read-modify-write
   * cycle.
   */
  Policy setBackupIAMPolicy(String instanceId, String backupId, Policy policy);

  /**
   * Tests for the given permissions on the specified backup for the caller.
   *
   * @param instanceId the id of the instance where the backup to test is located.
   * @param backupId the id of the backup to test.
   * @param permissions the permissions to test for. Permissions with wildcards (such as '*',
   *     'spanner.*', 'spanner.instances.*') are not allowed.
   * @return the subset of the tested permissions that the caller is allowed.
   */
  Iterable<String> testBackupIAMPermissions(
      String instanceId, String backupId, Iterable<String> permissions);
}
