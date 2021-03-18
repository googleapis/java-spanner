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

package com.google.cloud.spanner.spi.v1;

import com.google.api.core.ApiFuture;
import com.google.api.core.InternalApi;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.ServiceRpc;
import com.google.cloud.spanner.Restore;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.admin.database.v1.stub.DatabaseAdminStub;
import com.google.cloud.spanner.admin.instance.v1.stub.InstanceAdminStub;
import com.google.common.collect.ImmutableList;
import com.google.iam.v1.Policy;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.UpdateInstanceMetadata;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CommitResponse;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteBatchDmlResponse;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.PartitionQueryRequest;
import com.google.spanner.v1.PartitionReadRequest;
import com.google.spanner.v1.PartitionResponse;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.Transaction;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

/**
 * Abstracts remote calls to the Cloud Spanner service. Typically end-consumer code will never use
 * this interface; it's main purpose is to abstract the implementation of the public Cloud Spanner
 * API from the underlying transport mechanism.
 *
 * <p>Each {@code SpannerRPC} instance is bound to a particular project and set of authorization
 * credentials.
 *
 * <p>The interface is currently defined in terms of the generated HTTP client model classes. This
 * is purely for expedience; a future version of this interface is likely to be independent of
 * transport to allow switching between gRPC and HTTP.
 */
@InternalApi
public interface SpannerRpc extends ServiceRpc {
  /** Options passed in {@link SpannerRpc} methods to control how an RPC is issued. */
  enum Option {
    CHANNEL_HINT("Channel Hint");

    private final String value;

    Option(String value) {
      this.value = value;
    }

    @SuppressWarnings("unchecked")
    <T> T get(@Nullable Map<Option, ?> options) {
      if (options == null) {
        return null;
      }
      return (T) options.get(this);
    }

    Long getLong(@Nullable Map<Option, ?> options) {
      return get(options);
    }

    @Override
    public String toString() {
      return value;
    }
  }

  /**
   * Represents results from paginated RPCs, i.e., those where up to a maximum number of items is
   * returned from each call and a followup call must be made to fetch more.
   *
   * @param <T> the type of result
   */
  public static final class Paginated<T> {
    private final Iterable<T> results;
    private final String nextPageToken;

    /**
     * Creates a new page of results.
     *
     * @param results the result, or null for no results.
     * @param nextPageToken the token for the next page of results, or null if no more pages exist
     */
    public Paginated(@Nullable Iterable<T> results, @Nullable String nextPageToken) {
      // The generated HTTP client has null members when no results are present, rather than an
      // empty list.  Implicitly convert to an empty list to minimize the risk of NPEs.
      this.results = (results == null) ? ImmutableList.<T>of() : results;
      this.nextPageToken =
          (nextPageToken == null || nextPageToken.isEmpty()) ? null : nextPageToken;
    }

    /**
     * Returns the current page of results. Always returns non-null; if a null "results" was passed
     * to the constructor, a default empty {@code Iterable} will be returned.
     */
    public Iterable<T> getResults() {
      return results;
    }

    /**
     * Returns the token to use in the request for the next page, or null if this is the last page.
     */
    @Nullable
    public String getNextPageToken() {
      return nextPageToken;
    }
  }

  /** Consumer for the results produced by a streaming read or query call. */
  interface ResultStreamConsumer {
    void onPartialResultSet(PartialResultSet results);

    void onCompleted();

    void onError(SpannerException e);
  }

  /** Handle for cancellation of a streaming read or query call. */
  interface StreamingCall {

    /**
     * Requests more messages from the stream. We disable the auto flow control mechanisam in grpc,
     * so we need to request messages ourself. This gives us more control over how much buffer we
     * maintain in the client. Grpc will request 1 initial message automatically so we don't need to
     * call this at the beginning. After that it should be called whenever there is a flow control
     * window available based on the flow control setting configured by the client. Currently we do
     * not have any flow control so this is called automatically when a message is received.
     */
    void request(int numMessages);

    /**
     * Cancels the call.
     *
     * @param message a message to use in the final status of any underlying RPC
     */
    void cancel(@Nullable String message);
  }

  // Instance admin APIs.
  Paginated<InstanceConfig> listInstanceConfigs(int pageSize, @Nullable String pageToken)
      throws SpannerException;

  InstanceConfig getInstanceConfig(String instanceConfigName) throws SpannerException;

  Paginated<Instance> listInstances(
      int pageSize, @Nullable String pageToken, @Nullable String filter) throws SpannerException;

  OperationFuture<Instance, CreateInstanceMetadata> createInstance(
      String parent, String instanceId, Instance instance) throws SpannerException;

  OperationFuture<Instance, UpdateInstanceMetadata> updateInstance(
      Instance instance, FieldMask fieldMask) throws SpannerException;

  Instance getInstance(String instanceName) throws SpannerException;

  void deleteInstance(String instanceName) throws SpannerException;

  // Database admin APIs.
  Paginated<Database> listDatabases(String instanceName, int pageSize, @Nullable String pageToken)
      throws SpannerException;

  OperationFuture<Database, CreateDatabaseMetadata> createDatabase(
      String instanceName,
      String createDatabaseStatement,
      Iterable<String> additionalStatements,
      com.google.cloud.spanner.Database database)
      throws SpannerException;

  OperationFuture<Empty, UpdateDatabaseDdlMetadata> updateDatabaseDdl(
      String databaseName, Iterable<String> updateDatabaseStatements, @Nullable String updateId)
      throws SpannerException;

  void dropDatabase(String databaseName) throws SpannerException;

  Database getDatabase(String databaseName) throws SpannerException;

  List<String> getDatabaseDdl(String databaseName) throws SpannerException;
  /** Lists the backups in the specified instance. */
  Paginated<Backup> listBackups(
      String instanceName, int pageSize, @Nullable String filter, @Nullable String pageToken)
      throws SpannerException;

  /**
   * Creates a new backup from the source database specified in the {@link
   * com.google.cloud.spanner.Backup} instance.
   *
   * @param backupInfo the backup to create. The instance, database and expireTime fields of the
   *     backup must be filled.
   * @return the operation that monitors the backup creation.
   */
  OperationFuture<Backup, CreateBackupMetadata> createBackup(
      com.google.cloud.spanner.Backup backupInfo) throws SpannerException;

  /**
   * Restore a backup into the given database.
   *
   * @param restore a {@link Restore} instance with the backup source and destination database
   */
  OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabase(Restore restore);

  /** Gets the backup with the specified name. */
  Backup getBackup(String backupName) throws SpannerException;

  /** Updates the specified backup. The only supported field for updates is expireTime. */
  Backup updateBackup(Backup backup, FieldMask updateMask);

  /** List all long-running backup operations on the given instance. */
  Paginated<Operation> listBackupOperations(
      String instanceName, int pageSize, @Nullable String filter, @Nullable String pageToken);

  /**
   * Deletes a pending or completed backup.
   *
   * @param backupName Required. The fully qualified name of the backup to delete.
   */
  void deleteBackup(String backupName);

  Paginated<Operation> listDatabaseOperations(
      String instanceName, int pageSize, @Nullable String filter, @Nullable String pageToken);

  /** Retrieves a long running operation. */
  Operation getOperation(String name) throws SpannerException;

  /** Cancels the specified long-running operation. */
  void cancelOperation(String name) throws SpannerException;

  List<Session> batchCreateSessions(
      String databaseName,
      int sessionCount,
      @Nullable Map<String, String> labels,
      @Nullable Map<Option, ?> options)
      throws SpannerException;

  Session createSession(
      String databaseName, @Nullable Map<String, String> labels, @Nullable Map<Option, ?> options)
      throws SpannerException;

  void deleteSession(String sessionName, @Nullable Map<Option, ?> options) throws SpannerException;

  ApiFuture<Empty> asyncDeleteSession(String sessionName, @Nullable Map<Option, ?> options)
      throws SpannerException;

  StreamingCall read(
      ReadRequest request, ResultStreamConsumer consumer, @Nullable Map<Option, ?> options);

  ResultSet executeQuery(ExecuteSqlRequest request, @Nullable Map<Option, ?> options);

  ApiFuture<ResultSet> executeQueryAsync(
      ExecuteSqlRequest request, @Nullable Map<Option, ?> options);

  ResultSet executePartitionedDml(ExecuteSqlRequest request, @Nullable Map<Option, ?> options);

  RetrySettings getPartitionedDmlRetrySettings();

  ServerStream<PartialResultSet> executeStreamingPartitionedDml(
      ExecuteSqlRequest request, @Nullable Map<Option, ?> options, Duration timeout);

  StreamingCall executeQuery(
      ExecuteSqlRequest request, ResultStreamConsumer consumer, @Nullable Map<Option, ?> options);

  ExecuteBatchDmlResponse executeBatchDml(ExecuteBatchDmlRequest build, Map<Option, ?> options);

  ApiFuture<ExecuteBatchDmlResponse> executeBatchDmlAsync(
      ExecuteBatchDmlRequest build, Map<Option, ?> options);

  Transaction beginTransaction(BeginTransactionRequest request, @Nullable Map<Option, ?> options)
      throws SpannerException;

  ApiFuture<Transaction> beginTransactionAsync(
      BeginTransactionRequest request, @Nullable Map<Option, ?> options);

  CommitResponse commit(CommitRequest commitRequest, @Nullable Map<Option, ?> options)
      throws SpannerException;

  ApiFuture<CommitResponse> commitAsync(
      CommitRequest commitRequest, @Nullable Map<Option, ?> options);

  void rollback(RollbackRequest request, @Nullable Map<Option, ?> options) throws SpannerException;

  ApiFuture<Empty> rollbackAsync(RollbackRequest request, @Nullable Map<Option, ?> options);

  PartitionResponse partitionQuery(PartitionQueryRequest request, @Nullable Map<Option, ?> options)
      throws SpannerException;

  PartitionResponse partitionRead(PartitionReadRequest request, @Nullable Map<Option, ?> options)
      throws SpannerException;

  /** Gets the IAM policy for the given resource using the {@link DatabaseAdminStub}. */
  Policy getDatabaseAdminIAMPolicy(String resource);

  /**
   * Updates the IAM policy for the given resource using the {@link DatabaseAdminStub}. It is highly
   * recommended to first get the current policy and base the updated policy on the returned policy.
   * See {@link Policy.Builder#setEtag(com.google.protobuf.ByteString)} for information on the
   * recommended read-modify-write cycle.
   */
  Policy setDatabaseAdminIAMPolicy(String resource, Policy policy);

  /** Tests the IAM permissions for the given resource using the {@link DatabaseAdminStub}. */
  TestIamPermissionsResponse testDatabaseAdminIAMPermissions(
      String resource, Iterable<String> permissions);

  /** Gets the IAM policy for the given resource using the {@link InstanceAdminStub}. */
  Policy getInstanceAdminIAMPolicy(String resource);

  /**
   * Updates the IAM policy for the given resource using the {@link InstanceAdminStub}. It is highly
   * recommended to first get the current policy and base the updated policy on the returned policy.
   * See {@link Policy.Builder#setEtag(com.google.protobuf.ByteString)} for information on the
   * recommended read-modify-write cycle.
   */
  Policy setInstanceAdminIAMPolicy(String resource, Policy policy);

  /** Tests the IAM permissions for the given resource using the {@link InstanceAdminStub}. */
  TestIamPermissionsResponse testInstanceAdminIAMPermissions(
      String resource, Iterable<String> permissions);

  public void shutdown();

  boolean isClosed();
}
