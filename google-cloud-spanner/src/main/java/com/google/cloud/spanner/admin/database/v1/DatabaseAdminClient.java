/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.admin.database.v1;

import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.BetaApi;
import com.google.api.gax.core.BackgroundResource;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.AbstractFixedSizeCollection;
import com.google.api.gax.paging.AbstractPage;
import com.google.api.gax.paging.AbstractPagedListResponse;
import com.google.api.gax.rpc.OperationCallable;
import com.google.api.gax.rpc.PageContext;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.api.resourcenames.ResourceName;
import com.google.cloud.spanner.admin.database.v1.stub.DatabaseAdminStub;
import com.google.cloud.spanner.admin.database.v1.stub.DatabaseAdminStubSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.longrunning.OperationsClient;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.BackupName;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateBackupRequest;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.DeleteBackupRequest;
import com.google.spanner.admin.database.v1.DropDatabaseRequest;
import com.google.spanner.admin.database.v1.GetBackupRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlResponse;
import com.google.spanner.admin.database.v1.GetDatabaseRequest;
import com.google.spanner.admin.database.v1.InstanceName;
import com.google.spanner.admin.database.v1.ListBackupOperationsRequest;
import com.google.spanner.admin.database.v1.ListBackupOperationsResponse;
import com.google.spanner.admin.database.v1.ListBackupsRequest;
import com.google.spanner.admin.database.v1.ListBackupsResponse;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsRequest;
import com.google.spanner.admin.database.v1.ListDatabaseOperationsResponse;
import com.google.spanner.admin.database.v1.ListDatabasesRequest;
import com.google.spanner.admin.database.v1.ListDatabasesResponse;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseRequest;
import com.google.spanner.admin.database.v1.UpdateBackupRequest;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Generated;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * Service Description: Cloud Spanner Database Admin API
 *
 * <p>The Cloud Spanner Database Admin API can be used to create, drop, and list databases. It also
 * enables updating the schema of pre-existing databases. It can be also used to create, delete and
 * list backups for a database and to restore from an existing backup.
 *
 * <p>This class provides the ability to make remote calls to the backing service through method
 * calls that map to API methods. Sample code to get started:
 *
 * <pre>{@code
 * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
 *   DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
 *   Database response = databaseAdminClient.getDatabase(name);
 * }
 * }</pre>
 *
 * <p>Note: close() needs to be called on the DatabaseAdminClient object to clean up resources such
 * as threads. In the example above, try-with-resources is used, which automatically calls close().
 *
 * <p>The surface of this class includes several types of Java methods for each of the API's
 * methods:
 *
 * <ol>
 *   <li>A "flattened" method. With this type of method, the fields of the request type have been
 *       converted into function parameters. It may be the case that not all fields are available as
 *       parameters, and not every API method will have a flattened method entry point.
 *   <li>A "request object" method. This type of method only takes one parameter, a request object,
 *       which must be constructed before the call. Not every API method will have a request object
 *       method.
 *   <li>A "callable" method. This type of method takes no parameters and returns an immutable API
 *       callable object, which can be used to initiate calls to the service.
 * </ol>
 *
 * <p>See the individual methods for example code.
 *
 * <p>Many parameters require resource names to be formatted in a particular way. To assist with
 * these names, this class includes a format method for each type of name, and additionally a parse
 * method to extract the individual identifiers contained within names that are returned.
 *
 * <p>This class can be customized by passing in a custom instance of DatabaseAdminSettings to
 * create(). For example:
 *
 * <p>To customize credentials:
 *
 * <pre>{@code
 * DatabaseAdminSettings databaseAdminSettings =
 *     DatabaseAdminSettings.newBuilder()
 *         .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
 *         .build();
 * DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create(databaseAdminSettings);
 * }</pre>
 *
 * <p>To customize the endpoint:
 *
 * <pre>{@code
 * DatabaseAdminSettings databaseAdminSettings =
 *     DatabaseAdminSettings.newBuilder().setEndpoint(myEndpoint).build();
 * DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create(databaseAdminSettings);
 * }</pre>
 *
 * <p>Please refer to the GitHub repository's samples for more quickstart code snippets.
 */
@Generated("by gapic-generator-java")
public class DatabaseAdminClient implements BackgroundResource {
  private final DatabaseAdminSettings settings;
  private final DatabaseAdminStub stub;
  private final OperationsClient operationsClient;

  /** Constructs an instance of DatabaseAdminClient with default settings. */
  public static final DatabaseAdminClient create() throws IOException {
    return create(DatabaseAdminSettings.newBuilder().build());
  }

  /**
   * Constructs an instance of DatabaseAdminClient, using the given settings. The channels are
   * created based on the settings passed in, or defaults for any settings that are not set.
   */
  public static final DatabaseAdminClient create(DatabaseAdminSettings settings)
      throws IOException {
    return new DatabaseAdminClient(settings);
  }

  /**
   * Constructs an instance of DatabaseAdminClient, using the given stub for making calls. This is
   * for advanced usage - prefer using create(DatabaseAdminSettings).
   */
  @BetaApi("A restructuring of stub classes is planned, so this may break in the future")
  public static final DatabaseAdminClient create(DatabaseAdminStub stub) {
    return new DatabaseAdminClient(stub);
  }

  /**
   * Constructs an instance of DatabaseAdminClient, using the given settings. This is protected so
   * that it is easy to make a subclass, but otherwise, the static factory methods should be
   * preferred.
   */
  protected DatabaseAdminClient(DatabaseAdminSettings settings) throws IOException {
    this.settings = settings;
    this.stub = ((DatabaseAdminStubSettings) settings.getStubSettings()).createStub();
    this.operationsClient = OperationsClient.create(this.stub.getOperationsStub());
  }

  @BetaApi("A restructuring of stub classes is planned, so this may break in the future")
  protected DatabaseAdminClient(DatabaseAdminStub stub) {
    this.settings = null;
    this.stub = stub;
    this.operationsClient = OperationsClient.create(this.stub.getOperationsStub());
  }

  public final DatabaseAdminSettings getSettings() {
    return settings;
  }

  @BetaApi("A restructuring of stub classes is planned, so this may break in the future")
  public DatabaseAdminStub getStub() {
    return stub;
  }

  /**
   * Returns the OperationsClient that can be used to query the status of a long-running operation
   * returned by another API method call.
   */
  public final OperationsClient getOperationsClient() {
    return operationsClient;
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists Cloud Spanner databases.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   for (Database element : databaseAdminClient.listDatabases(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance whose databases should be listed. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListDatabasesPagedResponse listDatabases(InstanceName parent) {
    ListDatabasesRequest request =
        ListDatabasesRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listDatabases(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists Cloud Spanner databases.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   for (Database element : databaseAdminClient.listDatabases(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance whose databases should be listed. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListDatabasesPagedResponse listDatabases(String parent) {
    ListDatabasesRequest request = ListDatabasesRequest.newBuilder().setParent(parent).build();
    return listDatabases(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists Cloud Spanner databases.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListDatabasesRequest request =
   *       ListDatabasesRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   for (Database element : databaseAdminClient.listDatabases(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListDatabasesPagedResponse listDatabases(ListDatabasesRequest request) {
    return listDatabasesPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists Cloud Spanner databases.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListDatabasesRequest request =
   *       ListDatabasesRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   ApiFuture<Database> future =
   *       databaseAdminClient.listDatabasesPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Database element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListDatabasesRequest, ListDatabasesPagedResponse>
      listDatabasesPagedCallable() {
    return stub.listDatabasesPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists Cloud Spanner databases.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListDatabasesRequest request =
   *       ListDatabasesRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   while (true) {
   *     ListDatabasesResponse response = databaseAdminClient.listDatabasesCallable().call(request);
   *     for (Database element : response.getResponsesList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListDatabasesRequest, ListDatabasesResponse> listDatabasesCallable() {
    return stub.listDatabasesCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a new Cloud Spanner database and starts to prepare it for serving. The returned
   * [long-running operation][google.longrunning.Operation] will have a name of the format
   * `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to track preparation of
   * the database. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateDatabaseMetadata][google.spanner.admin.database.v1.CreateDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Database][google.spanner.admin.database.v1.Database], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   String createStatement = "createStatement744686547";
   *   Database response = databaseAdminClient.createDatabaseAsync(parent, createStatement).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance that will serve the new database. Values are
   *     of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param createStatement Required. A `CREATE DATABASE` statement, which specifies the ID of the
   *     new database. The database ID must conform to the regular expression
   *     `[a-z][a-z0-9_\\-]&#42;[a-z0-9]` and be between 2 and 30 characters in length. If the
   *     database ID is a reserved word or if it contains a hyphen, the database ID must be enclosed
   *     in backticks (`` ` ``).
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, CreateDatabaseMetadata> createDatabaseAsync(
      InstanceName parent, String createStatement) {
    CreateDatabaseRequest request =
        CreateDatabaseRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setCreateStatement(createStatement)
            .build();
    return createDatabaseAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a new Cloud Spanner database and starts to prepare it for serving. The returned
   * [long-running operation][google.longrunning.Operation] will have a name of the format
   * `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to track preparation of
   * the database. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateDatabaseMetadata][google.spanner.admin.database.v1.CreateDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Database][google.spanner.admin.database.v1.Database], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   String createStatement = "createStatement744686547";
   *   Database response = databaseAdminClient.createDatabaseAsync(parent, createStatement).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance that will serve the new database. Values are
   *     of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param createStatement Required. A `CREATE DATABASE` statement, which specifies the ID of the
   *     new database. The database ID must conform to the regular expression
   *     `[a-z][a-z0-9_\\-]&#42;[a-z0-9]` and be between 2 and 30 characters in length. If the
   *     database ID is a reserved word or if it contains a hyphen, the database ID must be enclosed
   *     in backticks (`` ` ``).
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, CreateDatabaseMetadata> createDatabaseAsync(
      String parent, String createStatement) {
    CreateDatabaseRequest request =
        CreateDatabaseRequest.newBuilder()
            .setParent(parent)
            .setCreateStatement(createStatement)
            .build();
    return createDatabaseAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a new Cloud Spanner database and starts to prepare it for serving. The returned
   * [long-running operation][google.longrunning.Operation] will have a name of the format
   * `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to track preparation of
   * the database. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateDatabaseMetadata][google.spanner.admin.database.v1.CreateDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Database][google.spanner.admin.database.v1.Database], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   CreateDatabaseRequest request =
   *       CreateDatabaseRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setCreateStatement("createStatement744686547")
   *           .addAllExtraStatements(new ArrayList<String>())
   *           .setEncryptionConfig(EncryptionConfig.newBuilder().build())
   *           .build();
   *   Database response = databaseAdminClient.createDatabaseAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, CreateDatabaseMetadata> createDatabaseAsync(
      CreateDatabaseRequest request) {
    return createDatabaseOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a new Cloud Spanner database and starts to prepare it for serving. The returned
   * [long-running operation][google.longrunning.Operation] will have a name of the format
   * `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to track preparation of
   * the database. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateDatabaseMetadata][google.spanner.admin.database.v1.CreateDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Database][google.spanner.admin.database.v1.Database], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   CreateDatabaseRequest request =
   *       CreateDatabaseRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setCreateStatement("createStatement744686547")
   *           .addAllExtraStatements(new ArrayList<String>())
   *           .setEncryptionConfig(EncryptionConfig.newBuilder().build())
   *           .build();
   *   OperationFuture<Database, CreateDatabaseMetadata> future =
   *       databaseAdminClient.createDatabaseOperationCallable().futureCall(request);
   *   // Do something.
   *   Database response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<CreateDatabaseRequest, Database, CreateDatabaseMetadata>
      createDatabaseOperationCallable() {
    return stub.createDatabaseOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a new Cloud Spanner database and starts to prepare it for serving. The returned
   * [long-running operation][google.longrunning.Operation] will have a name of the format
   * `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to track preparation of
   * the database. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateDatabaseMetadata][google.spanner.admin.database.v1.CreateDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Database][google.spanner.admin.database.v1.Database], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   CreateDatabaseRequest request =
   *       CreateDatabaseRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setCreateStatement("createStatement744686547")
   *           .addAllExtraStatements(new ArrayList<String>())
   *           .setEncryptionConfig(EncryptionConfig.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       databaseAdminClient.createDatabaseCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateDatabaseRequest, Operation> createDatabaseCallable() {
    return stub.createDatabaseCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the state of a Cloud Spanner database.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DatabaseName name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
   *   Database response = databaseAdminClient.getDatabase(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the requested database. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Database getDatabase(DatabaseName name) {
    GetDatabaseRequest request =
        GetDatabaseRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return getDatabase(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the state of a Cloud Spanner database.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String name = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString();
   *   Database response = databaseAdminClient.getDatabase(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the requested database. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Database getDatabase(String name) {
    GetDatabaseRequest request = GetDatabaseRequest.newBuilder().setName(name).build();
    return getDatabase(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the state of a Cloud Spanner database.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetDatabaseRequest request =
   *       GetDatabaseRequest.newBuilder()
   *           .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .build();
   *   Database response = databaseAdminClient.getDatabase(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Database getDatabase(GetDatabaseRequest request) {
    return getDatabaseCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the state of a Cloud Spanner database.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetDatabaseRequest request =
   *       GetDatabaseRequest.newBuilder()
   *           .setName(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .build();
   *   ApiFuture<Database> future = databaseAdminClient.getDatabaseCallable().futureCall(request);
   *   // Do something.
   *   Database response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetDatabaseRequest, Database> getDatabaseCallable() {
    return stub.getDatabaseCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the schema of a Cloud Spanner database by creating/altering/dropping tables, columns,
   * indexes, etc. The returned [long-running operation][google.longrunning.Operation] will have a
   * name of the format `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to
   * track execution of the schema change(s). The [metadata][google.longrunning.Operation.metadata]
   * field type is
   * [UpdateDatabaseDdlMetadata][google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata]. The
   * operation has no response.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
   *   List<String> statements = new ArrayList<>();
   *   databaseAdminClient.updateDatabaseDdlAsync(database, statements).get();
   * }
   * }</pre>
   *
   * @param database Required. The database to update.
   * @param statements Required. DDL statements to be applied to the database.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, UpdateDatabaseDdlMetadata> updateDatabaseDdlAsync(
      DatabaseName database, List<String> statements) {
    UpdateDatabaseDdlRequest request =
        UpdateDatabaseDdlRequest.newBuilder()
            .setDatabase(database == null ? null : database.toString())
            .addAllStatements(statements)
            .build();
    return updateDatabaseDdlAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the schema of a Cloud Spanner database by creating/altering/dropping tables, columns,
   * indexes, etc. The returned [long-running operation][google.longrunning.Operation] will have a
   * name of the format `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to
   * track execution of the schema change(s). The [metadata][google.longrunning.Operation.metadata]
   * field type is
   * [UpdateDatabaseDdlMetadata][google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata]. The
   * operation has no response.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString();
   *   List<String> statements = new ArrayList<>();
   *   databaseAdminClient.updateDatabaseDdlAsync(database, statements).get();
   * }
   * }</pre>
   *
   * @param database Required. The database to update.
   * @param statements Required. DDL statements to be applied to the database.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, UpdateDatabaseDdlMetadata> updateDatabaseDdlAsync(
      String database, List<String> statements) {
    UpdateDatabaseDdlRequest request =
        UpdateDatabaseDdlRequest.newBuilder()
            .setDatabase(database)
            .addAllStatements(statements)
            .build();
    return updateDatabaseDdlAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the schema of a Cloud Spanner database by creating/altering/dropping tables, columns,
   * indexes, etc. The returned [long-running operation][google.longrunning.Operation] will have a
   * name of the format `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to
   * track execution of the schema change(s). The [metadata][google.longrunning.Operation.metadata]
   * field type is
   * [UpdateDatabaseDdlMetadata][google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata]. The
   * operation has no response.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   UpdateDatabaseDdlRequest request =
   *       UpdateDatabaseDdlRequest.newBuilder()
   *           .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .addAllStatements(new ArrayList<String>())
   *           .setOperationId("operationId129704162")
   *           .build();
   *   databaseAdminClient.updateDatabaseDdlAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, UpdateDatabaseDdlMetadata> updateDatabaseDdlAsync(
      UpdateDatabaseDdlRequest request) {
    return updateDatabaseDdlOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the schema of a Cloud Spanner database by creating/altering/dropping tables, columns,
   * indexes, etc. The returned [long-running operation][google.longrunning.Operation] will have a
   * name of the format `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to
   * track execution of the schema change(s). The [metadata][google.longrunning.Operation.metadata]
   * field type is
   * [UpdateDatabaseDdlMetadata][google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata]. The
   * operation has no response.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   UpdateDatabaseDdlRequest request =
   *       UpdateDatabaseDdlRequest.newBuilder()
   *           .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .addAllStatements(new ArrayList<String>())
   *           .setOperationId("operationId129704162")
   *           .build();
   *   OperationFuture<Empty, UpdateDatabaseDdlMetadata> future =
   *       databaseAdminClient.updateDatabaseDdlOperationCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
      updateDatabaseDdlOperationCallable() {
    return stub.updateDatabaseDdlOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the schema of a Cloud Spanner database by creating/altering/dropping tables, columns,
   * indexes, etc. The returned [long-running operation][google.longrunning.Operation] will have a
   * name of the format `&lt;database_name&gt;/operations/&lt;operation_id&gt;` and can be used to
   * track execution of the schema change(s). The [metadata][google.longrunning.Operation.metadata]
   * field type is
   * [UpdateDatabaseDdlMetadata][google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata]. The
   * operation has no response.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   UpdateDatabaseDdlRequest request =
   *       UpdateDatabaseDdlRequest.newBuilder()
   *           .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .addAllStatements(new ArrayList<String>())
   *           .setOperationId("operationId129704162")
   *           .build();
   *   ApiFuture<Operation> future =
   *       databaseAdminClient.updateDatabaseDdlCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateDatabaseDdlRequest, Operation> updateDatabaseDdlCallable() {
    return stub.updateDatabaseDdlCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Drops (aka deletes) a Cloud Spanner database. Completed backups for the database will be
   * retained according to their `expire_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
   *   databaseAdminClient.dropDatabase(database);
   * }
   * }</pre>
   *
   * @param database Required. The database to be dropped.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void dropDatabase(DatabaseName database) {
    DropDatabaseRequest request =
        DropDatabaseRequest.newBuilder()
            .setDatabase(database == null ? null : database.toString())
            .build();
    dropDatabase(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Drops (aka deletes) a Cloud Spanner database. Completed backups for the database will be
   * retained according to their `expire_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString();
   *   databaseAdminClient.dropDatabase(database);
   * }
   * }</pre>
   *
   * @param database Required. The database to be dropped.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void dropDatabase(String database) {
    DropDatabaseRequest request = DropDatabaseRequest.newBuilder().setDatabase(database).build();
    dropDatabase(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Drops (aka deletes) a Cloud Spanner database. Completed backups for the database will be
   * retained according to their `expire_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DropDatabaseRequest request =
   *       DropDatabaseRequest.newBuilder()
   *           .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .build();
   *   databaseAdminClient.dropDatabase(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void dropDatabase(DropDatabaseRequest request) {
    dropDatabaseCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Drops (aka deletes) a Cloud Spanner database. Completed backups for the database will be
   * retained according to their `expire_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DropDatabaseRequest request =
   *       DropDatabaseRequest.newBuilder()
   *           .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .build();
   *   ApiFuture<Empty> future = databaseAdminClient.dropDatabaseCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DropDatabaseRequest, Empty> dropDatabaseCallable() {
    return stub.dropDatabaseCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns the schema of a Cloud Spanner database as a list of formatted DDL statements. This
   * method does not show pending schema updates, those may be queried using the
   * [Operations][google.longrunning.Operations] API.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
   *   GetDatabaseDdlResponse response = databaseAdminClient.getDatabaseDdl(database);
   * }
   * }</pre>
   *
   * @param database Required. The database whose schema we wish to get. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final GetDatabaseDdlResponse getDatabaseDdl(DatabaseName database) {
    GetDatabaseDdlRequest request =
        GetDatabaseDdlRequest.newBuilder()
            .setDatabase(database == null ? null : database.toString())
            .build();
    return getDatabaseDdl(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns the schema of a Cloud Spanner database as a list of formatted DDL statements. This
   * method does not show pending schema updates, those may be queried using the
   * [Operations][google.longrunning.Operations] API.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString();
   *   GetDatabaseDdlResponse response = databaseAdminClient.getDatabaseDdl(database);
   * }
   * }</pre>
   *
   * @param database Required. The database whose schema we wish to get. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final GetDatabaseDdlResponse getDatabaseDdl(String database) {
    GetDatabaseDdlRequest request =
        GetDatabaseDdlRequest.newBuilder().setDatabase(database).build();
    return getDatabaseDdl(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns the schema of a Cloud Spanner database as a list of formatted DDL statements. This
   * method does not show pending schema updates, those may be queried using the
   * [Operations][google.longrunning.Operations] API.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetDatabaseDdlRequest request =
   *       GetDatabaseDdlRequest.newBuilder()
   *           .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .build();
   *   GetDatabaseDdlResponse response = databaseAdminClient.getDatabaseDdl(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final GetDatabaseDdlResponse getDatabaseDdl(GetDatabaseDdlRequest request) {
    return getDatabaseDdlCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns the schema of a Cloud Spanner database as a list of formatted DDL statements. This
   * method does not show pending schema updates, those may be queried using the
   * [Operations][google.longrunning.Operations] API.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetDatabaseDdlRequest request =
   *       GetDatabaseDdlRequest.newBuilder()
   *           .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
   *           .build();
   *   ApiFuture<GetDatabaseDdlResponse> future =
   *       databaseAdminClient.getDatabaseDdlCallable().futureCall(request);
   *   // Do something.
   *   GetDatabaseDdlResponse response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetDatabaseDdlRequest, GetDatabaseDdlResponse>
      getDatabaseDdlCallable() {
    return stub.getDatabaseDdlCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on a database or backup resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.databases.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ResourceName resource =
   *       CryptoKeyVersionName.of(
   *           "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
   *   Policy policy = Policy.newBuilder().build();
   *   Policy response = databaseAdminClient.setIamPolicy(resource, policy);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being specified. See the
   *     operation documentation for the appropriate value for this field.
   * @param policy REQUIRED: The complete policy to be applied to the `resource`. The size of the
   *     policy is limited to a few 10s of KB. An empty policy is a valid policy but certain Cloud
   *     Platform services (such as Projects) might reject them.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy setIamPolicy(ResourceName resource, Policy policy) {
    SetIamPolicyRequest request =
        SetIamPolicyRequest.newBuilder()
            .setResource(resource == null ? null : resource.toString())
            .setPolicy(policy)
            .build();
    return setIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on a database or backup resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.databases.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String resource =
   *       CryptoKeyVersionName.of(
   *               "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]")
   *           .toString();
   *   Policy policy = Policy.newBuilder().build();
   *   Policy response = databaseAdminClient.setIamPolicy(resource, policy);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being specified. See the
   *     operation documentation for the appropriate value for this field.
   * @param policy REQUIRED: The complete policy to be applied to the `resource`. The size of the
   *     policy is limited to a few 10s of KB. An empty policy is a valid policy but certain Cloud
   *     Platform services (such as Projects) might reject them.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy setIamPolicy(String resource, Policy policy) {
    SetIamPolicyRequest request =
        SetIamPolicyRequest.newBuilder().setResource(resource).setPolicy(policy).build();
    return setIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on a database or backup resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.databases.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   SetIamPolicyRequest request =
   *       SetIamPolicyRequest.newBuilder()
   *           .setResource(
   *               CryptoKeyVersionName.of(
   *                       "[PROJECT]",
   *                       "[LOCATION]",
   *                       "[KEY_RING]",
   *                       "[CRYPTO_KEY]",
   *                       "[CRYPTO_KEY_VERSION]")
   *                   .toString())
   *           .setPolicy(Policy.newBuilder().build())
   *           .build();
   *   Policy response = databaseAdminClient.setIamPolicy(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy setIamPolicy(SetIamPolicyRequest request) {
    return setIamPolicyCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on a database or backup resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.databases.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.setIamPolicy` permission on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   SetIamPolicyRequest request =
   *       SetIamPolicyRequest.newBuilder()
   *           .setResource(
   *               CryptoKeyVersionName.of(
   *                       "[PROJECT]",
   *                       "[LOCATION]",
   *                       "[KEY_RING]",
   *                       "[CRYPTO_KEY]",
   *                       "[CRYPTO_KEY_VERSION]")
   *                   .toString())
   *           .setPolicy(Policy.newBuilder().build())
   *           .build();
   *   ApiFuture<Policy> future = databaseAdminClient.setIamPolicyCallable().futureCall(request);
   *   // Do something.
   *   Policy response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<SetIamPolicyRequest, Policy> setIamPolicyCallable() {
    return stub.setIamPolicyCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for a database or backup resource. Returns an empty policy if a
   * database or backup exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.databases.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ResourceName resource =
   *       CryptoKeyVersionName.of(
   *           "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
   *   Policy response = databaseAdminClient.getIamPolicy(resource);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy getIamPolicy(ResourceName resource) {
    GetIamPolicyRequest request =
        GetIamPolicyRequest.newBuilder()
            .setResource(resource == null ? null : resource.toString())
            .build();
    return getIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for a database or backup resource. Returns an empty policy if a
   * database or backup exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.databases.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String resource =
   *       CryptoKeyVersionName.of(
   *               "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]")
   *           .toString();
   *   Policy response = databaseAdminClient.getIamPolicy(resource);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy getIamPolicy(String resource) {
    GetIamPolicyRequest request = GetIamPolicyRequest.newBuilder().setResource(resource).build();
    return getIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for a database or backup resource. Returns an empty policy if a
   * database or backup exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.databases.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetIamPolicyRequest request =
   *       GetIamPolicyRequest.newBuilder()
   *           .setResource(
   *               CryptoKeyVersionName.of(
   *                       "[PROJECT]",
   *                       "[LOCATION]",
   *                       "[KEY_RING]",
   *                       "[CRYPTO_KEY]",
   *                       "[CRYPTO_KEY_VERSION]")
   *                   .toString())
   *           .setOptions(GetPolicyOptions.newBuilder().build())
   *           .build();
   *   Policy response = databaseAdminClient.getIamPolicy(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy getIamPolicy(GetIamPolicyRequest request) {
    return getIamPolicyCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for a database or backup resource. Returns an empty policy if a
   * database or backup exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.databases.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource]. For backups, authorization requires
   * `spanner.backups.getIamPolicy` permission on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetIamPolicyRequest request =
   *       GetIamPolicyRequest.newBuilder()
   *           .setResource(
   *               CryptoKeyVersionName.of(
   *                       "[PROJECT]",
   *                       "[LOCATION]",
   *                       "[KEY_RING]",
   *                       "[CRYPTO_KEY]",
   *                       "[CRYPTO_KEY_VERSION]")
   *                   .toString())
   *           .setOptions(GetPolicyOptions.newBuilder().build())
   *           .build();
   *   ApiFuture<Policy> future = databaseAdminClient.getIamPolicyCallable().futureCall(request);
   *   // Do something.
   *   Policy response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetIamPolicyRequest, Policy> getIamPolicyCallable() {
    return stub.getIamPolicyCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified database or backup resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner database will result in a NOT_FOUND
   * error if the user has `spanner.databases.list` permission on the containing Cloud Spanner
   * instance. Otherwise returns an empty set of permissions. Calling this method on a backup that
   * does not exist will result in a NOT_FOUND error if the user has `spanner.backups.list`
   * permission on the containing instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ResourceName resource =
   *       CryptoKeyVersionName.of(
   *           "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]");
   *   List<String> permissions = new ArrayList<>();
   *   TestIamPermissionsResponse response =
   *       databaseAdminClient.testIamPermissions(resource, permissions);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy detail is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @param permissions The set of permissions to check for the `resource`. Permissions with
   *     wildcards (such as '&#42;' or 'storage.&#42;') are not allowed. For more information see
   *     [IAM Overview](https://cloud.google.com/iam/docs/overview#permissions).
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final TestIamPermissionsResponse testIamPermissions(
      ResourceName resource, List<String> permissions) {
    TestIamPermissionsRequest request =
        TestIamPermissionsRequest.newBuilder()
            .setResource(resource == null ? null : resource.toString())
            .addAllPermissions(permissions)
            .build();
    return testIamPermissions(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified database or backup resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner database will result in a NOT_FOUND
   * error if the user has `spanner.databases.list` permission on the containing Cloud Spanner
   * instance. Otherwise returns an empty set of permissions. Calling this method on a backup that
   * does not exist will result in a NOT_FOUND error if the user has `spanner.backups.list`
   * permission on the containing instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String resource =
   *       CryptoKeyVersionName.of(
   *               "[PROJECT]", "[LOCATION]", "[KEY_RING]", "[CRYPTO_KEY]", "[CRYPTO_KEY_VERSION]")
   *           .toString();
   *   List<String> permissions = new ArrayList<>();
   *   TestIamPermissionsResponse response =
   *       databaseAdminClient.testIamPermissions(resource, permissions);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy detail is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @param permissions The set of permissions to check for the `resource`. Permissions with
   *     wildcards (such as '&#42;' or 'storage.&#42;') are not allowed. For more information see
   *     [IAM Overview](https://cloud.google.com/iam/docs/overview#permissions).
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final TestIamPermissionsResponse testIamPermissions(
      String resource, List<String> permissions) {
    TestIamPermissionsRequest request =
        TestIamPermissionsRequest.newBuilder()
            .setResource(resource)
            .addAllPermissions(permissions)
            .build();
    return testIamPermissions(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified database or backup resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner database will result in a NOT_FOUND
   * error if the user has `spanner.databases.list` permission on the containing Cloud Spanner
   * instance. Otherwise returns an empty set of permissions. Calling this method on a backup that
   * does not exist will result in a NOT_FOUND error if the user has `spanner.backups.list`
   * permission on the containing instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   TestIamPermissionsRequest request =
   *       TestIamPermissionsRequest.newBuilder()
   *           .setResource(
   *               CryptoKeyVersionName.of(
   *                       "[PROJECT]",
   *                       "[LOCATION]",
   *                       "[KEY_RING]",
   *                       "[CRYPTO_KEY]",
   *                       "[CRYPTO_KEY_VERSION]")
   *                   .toString())
   *           .addAllPermissions(new ArrayList<String>())
   *           .build();
   *   TestIamPermissionsResponse response = databaseAdminClient.testIamPermissions(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final TestIamPermissionsResponse testIamPermissions(TestIamPermissionsRequest request) {
    return testIamPermissionsCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified database or backup resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner database will result in a NOT_FOUND
   * error if the user has `spanner.databases.list` permission on the containing Cloud Spanner
   * instance. Otherwise returns an empty set of permissions. Calling this method on a backup that
   * does not exist will result in a NOT_FOUND error if the user has `spanner.backups.list`
   * permission on the containing instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   TestIamPermissionsRequest request =
   *       TestIamPermissionsRequest.newBuilder()
   *           .setResource(
   *               CryptoKeyVersionName.of(
   *                       "[PROJECT]",
   *                       "[LOCATION]",
   *                       "[KEY_RING]",
   *                       "[CRYPTO_KEY]",
   *                       "[CRYPTO_KEY_VERSION]")
   *                   .toString())
   *           .addAllPermissions(new ArrayList<String>())
   *           .build();
   *   ApiFuture<TestIamPermissionsResponse> future =
   *       databaseAdminClient.testIamPermissionsCallable().futureCall(request);
   *   // Do something.
   *   TestIamPermissionsResponse response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsCallable() {
    return stub.testIamPermissionsCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Starts creating a new Cloud Spanner Backup. The returned backup [long-running
   * operation][google.longrunning.Operation] will have a name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation_id&gt;`
   * and can be used to track creation of the backup. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateBackupMetadata][google.spanner.admin.database.v1.CreateBackupMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Backup][google.spanner.admin.database.v1.Backup], if successful. Cancelling the returned
   * operation will stop the creation and delete the backup. There can be only one pending backup
   * creation per database. Backup creation of different databases can run concurrently.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   Backup backup = Backup.newBuilder().build();
   *   String backupId = "backupId2121930365";
   *   Backup response = databaseAdminClient.createBackupAsync(parent, backup, backupId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance in which the backup will be created. This must
   *     be the same instance that contains the database the backup will be created from. The backup
   *     will be stored in the location(s) specified in the instance configuration of this instance.
   *     Values are of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param backup Required. The backup to create.
   * @param backupId Required. The id of the backup to be created. The `backup_id` appended to
   *     `parent` forms the full backup name of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup_id&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, CreateBackupMetadata> createBackupAsync(
      InstanceName parent, Backup backup, String backupId) {
    CreateBackupRequest request =
        CreateBackupRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setBackup(backup)
            .setBackupId(backupId)
            .build();
    return createBackupAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Starts creating a new Cloud Spanner Backup. The returned backup [long-running
   * operation][google.longrunning.Operation] will have a name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation_id&gt;`
   * and can be used to track creation of the backup. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateBackupMetadata][google.spanner.admin.database.v1.CreateBackupMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Backup][google.spanner.admin.database.v1.Backup], if successful. Cancelling the returned
   * operation will stop the creation and delete the backup. There can be only one pending backup
   * creation per database. Backup creation of different databases can run concurrently.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   Backup backup = Backup.newBuilder().build();
   *   String backupId = "backupId2121930365";
   *   Backup response = databaseAdminClient.createBackupAsync(parent, backup, backupId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance in which the backup will be created. This must
   *     be the same instance that contains the database the backup will be created from. The backup
   *     will be stored in the location(s) specified in the instance configuration of this instance.
   *     Values are of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param backup Required. The backup to create.
   * @param backupId Required. The id of the backup to be created. The `backup_id` appended to
   *     `parent` forms the full backup name of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup_id&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, CreateBackupMetadata> createBackupAsync(
      String parent, Backup backup, String backupId) {
    CreateBackupRequest request =
        CreateBackupRequest.newBuilder()
            .setParent(parent)
            .setBackup(backup)
            .setBackupId(backupId)
            .build();
    return createBackupAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Starts creating a new Cloud Spanner Backup. The returned backup [long-running
   * operation][google.longrunning.Operation] will have a name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation_id&gt;`
   * and can be used to track creation of the backup. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateBackupMetadata][google.spanner.admin.database.v1.CreateBackupMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Backup][google.spanner.admin.database.v1.Backup], if successful. Cancelling the returned
   * operation will stop the creation and delete the backup. There can be only one pending backup
   * creation per database. Backup creation of different databases can run concurrently.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   CreateBackupRequest request =
   *       CreateBackupRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setBackupId("backupId2121930365")
   *           .setBackup(Backup.newBuilder().build())
   *           .setEncryptionConfig(CreateBackupEncryptionConfig.newBuilder().build())
   *           .build();
   *   Backup response = databaseAdminClient.createBackupAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, CreateBackupMetadata> createBackupAsync(
      CreateBackupRequest request) {
    return createBackupOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Starts creating a new Cloud Spanner Backup. The returned backup [long-running
   * operation][google.longrunning.Operation] will have a name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation_id&gt;`
   * and can be used to track creation of the backup. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateBackupMetadata][google.spanner.admin.database.v1.CreateBackupMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Backup][google.spanner.admin.database.v1.Backup], if successful. Cancelling the returned
   * operation will stop the creation and delete the backup. There can be only one pending backup
   * creation per database. Backup creation of different databases can run concurrently.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   CreateBackupRequest request =
   *       CreateBackupRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setBackupId("backupId2121930365")
   *           .setBackup(Backup.newBuilder().build())
   *           .setEncryptionConfig(CreateBackupEncryptionConfig.newBuilder().build())
   *           .build();
   *   OperationFuture<Backup, CreateBackupMetadata> future =
   *       databaseAdminClient.createBackupOperationCallable().futureCall(request);
   *   // Do something.
   *   Backup response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<CreateBackupRequest, Backup, CreateBackupMetadata>
      createBackupOperationCallable() {
    return stub.createBackupOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Starts creating a new Cloud Spanner Backup. The returned backup [long-running
   * operation][google.longrunning.Operation] will have a name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation_id&gt;`
   * and can be used to track creation of the backup. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateBackupMetadata][google.spanner.admin.database.v1.CreateBackupMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Backup][google.spanner.admin.database.v1.Backup], if successful. Cancelling the returned
   * operation will stop the creation and delete the backup. There can be only one pending backup
   * creation per database. Backup creation of different databases can run concurrently.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   CreateBackupRequest request =
   *       CreateBackupRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setBackupId("backupId2121930365")
   *           .setBackup(Backup.newBuilder().build())
   *           .setEncryptionConfig(CreateBackupEncryptionConfig.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future = databaseAdminClient.createBackupCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateBackupRequest, Operation> createBackupCallable() {
    return stub.createBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets metadata on a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
   *   Backup response = databaseAdminClient.getBackup(name);
   * }
   * }</pre>
   *
   * @param name Required. Name of the backup. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup getBackup(BackupName name) {
    GetBackupRequest request =
        GetBackupRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return getBackup(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets metadata on a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString();
   *   Backup response = databaseAdminClient.getBackup(name);
   * }
   * }</pre>
   *
   * @param name Required. Name of the backup. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup getBackup(String name) {
    GetBackupRequest request = GetBackupRequest.newBuilder().setName(name).build();
    return getBackup(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets metadata on a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetBackupRequest request =
   *       GetBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
   *           .build();
   *   Backup response = databaseAdminClient.getBackup(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup getBackup(GetBackupRequest request) {
    return getBackupCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets metadata on a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   GetBackupRequest request =
   *       GetBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
   *           .build();
   *   ApiFuture<Backup> future = databaseAdminClient.getBackupCallable().futureCall(request);
   *   // Do something.
   *   Backup response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetBackupRequest, Backup> getBackupCallable() {
    return stub.getBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   Backup backup = Backup.newBuilder().build();
   *   FieldMask updateMask = FieldMask.newBuilder().build();
   *   Backup response = databaseAdminClient.updateBackup(backup, updateMask);
   * }
   * }</pre>
   *
   * @param backup Required. The backup to update. `backup.name`, and the fields to be updated as
   *     specified by `update_mask` are required. Other fields are ignored. Update is only supported
   *     for the following fields: &#42; `backup.expire_time`.
   * @param updateMask Required. A mask specifying which fields (e.g. `expire_time`) in the Backup
   *     resource should be updated. This mask is relative to the Backup resource, not to the
   *     request message. The field mask must always be specified; this prevents any future fields
   *     from being erased accidentally by clients that do not know about them.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup updateBackup(Backup backup, FieldMask updateMask) {
    UpdateBackupRequest request =
        UpdateBackupRequest.newBuilder().setBackup(backup).setUpdateMask(updateMask).build();
    return updateBackup(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   UpdateBackupRequest request =
   *       UpdateBackupRequest.newBuilder()
   *           .setBackup(Backup.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   Backup response = databaseAdminClient.updateBackup(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup updateBackup(UpdateBackupRequest request) {
    return updateBackupCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   UpdateBackupRequest request =
   *       UpdateBackupRequest.newBuilder()
   *           .setBackup(Backup.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   ApiFuture<Backup> future = databaseAdminClient.updateBackupCallable().futureCall(request);
   *   // Do something.
   *   Backup response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateBackupRequest, Backup> updateBackupCallable() {
    return stub.updateBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   BackupName name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
   *   databaseAdminClient.deleteBackup(name);
   * }
   * }</pre>
   *
   * @param name Required. Name of the backup to delete. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteBackup(BackupName name) {
    DeleteBackupRequest request =
        DeleteBackupRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    deleteBackup(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String name = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString();
   *   databaseAdminClient.deleteBackup(name);
   * }
   * }</pre>
   *
   * @param name Required. Name of the backup to delete. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteBackup(String name) {
    DeleteBackupRequest request = DeleteBackupRequest.newBuilder().setName(name).build();
    deleteBackup(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DeleteBackupRequest request =
   *       DeleteBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
   *           .build();
   *   databaseAdminClient.deleteBackup(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteBackup(DeleteBackupRequest request) {
    deleteBackupCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a pending or completed [Backup][google.spanner.admin.database.v1.Backup].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   DeleteBackupRequest request =
   *       DeleteBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString())
   *           .build();
   *   ApiFuture<Empty> future = databaseAdminClient.deleteBackupCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DeleteBackupRequest, Empty> deleteBackupCallable() {
    return stub.deleteBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists completed and pending backups. Backups returned are ordered by `create_time` in
   * descending order, starting from the most recent `create_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   for (Backup element : databaseAdminClient.listBackups(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance to list backups from. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupsPagedResponse listBackups(InstanceName parent) {
    ListBackupsRequest request =
        ListBackupsRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listBackups(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists completed and pending backups. Backups returned are ordered by `create_time` in
   * descending order, starting from the most recent `create_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   for (Backup element : databaseAdminClient.listBackups(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance to list backups from. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupsPagedResponse listBackups(String parent) {
    ListBackupsRequest request = ListBackupsRequest.newBuilder().setParent(parent).build();
    return listBackups(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists completed and pending backups. Backups returned are ordered by `create_time` in
   * descending order, starting from the most recent `create_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListBackupsRequest request =
   *       ListBackupsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   for (Backup element : databaseAdminClient.listBackups(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupsPagedResponse listBackups(ListBackupsRequest request) {
    return listBackupsPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists completed and pending backups. Backups returned are ordered by `create_time` in
   * descending order, starting from the most recent `create_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListBackupsRequest request =
   *       ListBackupsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   ApiFuture<Backup> future = databaseAdminClient.listBackupsPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Backup element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListBackupsRequest, ListBackupsPagedResponse>
      listBackupsPagedCallable() {
    return stub.listBackupsPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists completed and pending backups. Backups returned are ordered by `create_time` in
   * descending order, starting from the most recent `create_time`.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListBackupsRequest request =
   *       ListBackupsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   while (true) {
   *     ListBackupsResponse response = databaseAdminClient.listBackupsCallable().call(request);
   *     for (Backup element : response.getResponsesList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListBackupsRequest, ListBackupsResponse> listBackupsCallable() {
    return stub.listBackupsCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Create a new database by restoring from a completed backup. The new database must be in the
   * same project and in an instance with the same instance configuration as the instance containing
   * the backup. The returned database [long-running operation][google.longrunning.Operation] has a
   * name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation_id&gt;`,
   * and can be used to track the progress of the operation, and to cancel it. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [RestoreDatabaseMetadata][google.spanner.admin.database.v1.RestoreDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] type is
   * [Database][google.spanner.admin.database.v1.Database], if successful. Cancelling the returned
   * operation will stop the restore and delete the database. There can be only one database being
   * restored into an instance at a time. Once the restore operation completes, a new restore
   * operation can be initiated, without waiting for the optimize operation associated with the
   * first restore to complete.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   String databaseId = "databaseId1688905718";
   *   BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
   *   Database response =
   *       databaseAdminClient.restoreDatabaseAsync(parent, databaseId, backup).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance in which to create the restored database. This
   *     instance must be in the same project and have the same instance configuration as the
   *     instance containing the source backup. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param databaseId Required. The id of the database to create and restore to. This database must
   *     not already exist. The `database_id` appended to `parent` forms the full database name of
   *     the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database_id&gt;`.
   * @param backup Name of the backup from which to restore. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabaseAsync(
      InstanceName parent, String databaseId, BackupName backup) {
    RestoreDatabaseRequest request =
        RestoreDatabaseRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setDatabaseId(databaseId)
            .setBackup(backup == null ? null : backup.toString())
            .build();
    return restoreDatabaseAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Create a new database by restoring from a completed backup. The new database must be in the
   * same project and in an instance with the same instance configuration as the instance containing
   * the backup. The returned database [long-running operation][google.longrunning.Operation] has a
   * name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation_id&gt;`,
   * and can be used to track the progress of the operation, and to cancel it. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [RestoreDatabaseMetadata][google.spanner.admin.database.v1.RestoreDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] type is
   * [Database][google.spanner.admin.database.v1.Database], if successful. Cancelling the returned
   * operation will stop the restore and delete the database. There can be only one database being
   * restored into an instance at a time. Once the restore operation completes, a new restore
   * operation can be initiated, without waiting for the optimize operation associated with the
   * first restore to complete.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   String databaseId = "databaseId1688905718";
   *   String backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString();
   *   Database response =
   *       databaseAdminClient.restoreDatabaseAsync(parent, databaseId, backup).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance in which to create the restored database. This
   *     instance must be in the same project and have the same instance configuration as the
   *     instance containing the source backup. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param databaseId Required. The id of the database to create and restore to. This database must
   *     not already exist. The `database_id` appended to `parent` forms the full database name of
   *     the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database_id&gt;`.
   * @param backup Name of the backup from which to restore. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabaseAsync(
      InstanceName parent, String databaseId, String backup) {
    RestoreDatabaseRequest request =
        RestoreDatabaseRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setDatabaseId(databaseId)
            .setBackup(backup)
            .build();
    return restoreDatabaseAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Create a new database by restoring from a completed backup. The new database must be in the
   * same project and in an instance with the same instance configuration as the instance containing
   * the backup. The returned database [long-running operation][google.longrunning.Operation] has a
   * name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation_id&gt;`,
   * and can be used to track the progress of the operation, and to cancel it. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [RestoreDatabaseMetadata][google.spanner.admin.database.v1.RestoreDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] type is
   * [Database][google.spanner.admin.database.v1.Database], if successful. Cancelling the returned
   * operation will stop the restore and delete the database. There can be only one database being
   * restored into an instance at a time. Once the restore operation completes, a new restore
   * operation can be initiated, without waiting for the optimize operation associated with the
   * first restore to complete.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   String databaseId = "databaseId1688905718";
   *   BackupName backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]");
   *   Database response =
   *       databaseAdminClient.restoreDatabaseAsync(parent, databaseId, backup).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance in which to create the restored database. This
   *     instance must be in the same project and have the same instance configuration as the
   *     instance containing the source backup. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param databaseId Required. The id of the database to create and restore to. This database must
   *     not already exist. The `database_id` appended to `parent` forms the full database name of
   *     the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database_id&gt;`.
   * @param backup Name of the backup from which to restore. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabaseAsync(
      String parent, String databaseId, BackupName backup) {
    RestoreDatabaseRequest request =
        RestoreDatabaseRequest.newBuilder()
            .setParent(parent)
            .setDatabaseId(databaseId)
            .setBackup(backup == null ? null : backup.toString())
            .build();
    return restoreDatabaseAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Create a new database by restoring from a completed backup. The new database must be in the
   * same project and in an instance with the same instance configuration as the instance containing
   * the backup. The returned database [long-running operation][google.longrunning.Operation] has a
   * name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation_id&gt;`,
   * and can be used to track the progress of the operation, and to cancel it. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [RestoreDatabaseMetadata][google.spanner.admin.database.v1.RestoreDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] type is
   * [Database][google.spanner.admin.database.v1.Database], if successful. Cancelling the returned
   * operation will stop the restore and delete the database. There can be only one database being
   * restored into an instance at a time. Once the restore operation completes, a new restore
   * operation can be initiated, without waiting for the optimize operation associated with the
   * first restore to complete.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   String databaseId = "databaseId1688905718";
   *   String backup = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString();
   *   Database response =
   *       databaseAdminClient.restoreDatabaseAsync(parent, databaseId, backup).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the instance in which to create the restored database. This
   *     instance must be in the same project and have the same instance configuration as the
   *     instance containing the source backup. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @param databaseId Required. The id of the database to create and restore to. This database must
   *     not already exist. The `database_id` appended to `parent` forms the full database name of
   *     the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database_id&gt;`.
   * @param backup Name of the backup from which to restore. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabaseAsync(
      String parent, String databaseId, String backup) {
    RestoreDatabaseRequest request =
        RestoreDatabaseRequest.newBuilder()
            .setParent(parent)
            .setDatabaseId(databaseId)
            .setBackup(backup)
            .build();
    return restoreDatabaseAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Create a new database by restoring from a completed backup. The new database must be in the
   * same project and in an instance with the same instance configuration as the instance containing
   * the backup. The returned database [long-running operation][google.longrunning.Operation] has a
   * name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation_id&gt;`,
   * and can be used to track the progress of the operation, and to cancel it. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [RestoreDatabaseMetadata][google.spanner.admin.database.v1.RestoreDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] type is
   * [Database][google.spanner.admin.database.v1.Database], if successful. Cancelling the returned
   * operation will stop the restore and delete the database. There can be only one database being
   * restored into an instance at a time. Once the restore operation completes, a new restore
   * operation can be initiated, without waiting for the optimize operation associated with the
   * first restore to complete.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   RestoreDatabaseRequest request =
   *       RestoreDatabaseRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setDatabaseId("databaseId1688905718")
   *           .setEncryptionConfig(RestoreDatabaseEncryptionConfig.newBuilder().build())
   *           .build();
   *   Database response = databaseAdminClient.restoreDatabaseAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Database, RestoreDatabaseMetadata> restoreDatabaseAsync(
      RestoreDatabaseRequest request) {
    return restoreDatabaseOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Create a new database by restoring from a completed backup. The new database must be in the
   * same project and in an instance with the same instance configuration as the instance containing
   * the backup. The returned database [long-running operation][google.longrunning.Operation] has a
   * name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation_id&gt;`,
   * and can be used to track the progress of the operation, and to cancel it. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [RestoreDatabaseMetadata][google.spanner.admin.database.v1.RestoreDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] type is
   * [Database][google.spanner.admin.database.v1.Database], if successful. Cancelling the returned
   * operation will stop the restore and delete the database. There can be only one database being
   * restored into an instance at a time. Once the restore operation completes, a new restore
   * operation can be initiated, without waiting for the optimize operation associated with the
   * first restore to complete.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   RestoreDatabaseRequest request =
   *       RestoreDatabaseRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setDatabaseId("databaseId1688905718")
   *           .setEncryptionConfig(RestoreDatabaseEncryptionConfig.newBuilder().build())
   *           .build();
   *   OperationFuture<Database, RestoreDatabaseMetadata> future =
   *       databaseAdminClient.restoreDatabaseOperationCallable().futureCall(request);
   *   // Do something.
   *   Database response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<RestoreDatabaseRequest, Database, RestoreDatabaseMetadata>
      restoreDatabaseOperationCallable() {
    return stub.restoreDatabaseOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Create a new database by restoring from a completed backup. The new database must be in the
   * same project and in an instance with the same instance configuration as the instance containing
   * the backup. The returned database [long-running operation][google.longrunning.Operation] has a
   * name of the format
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation_id&gt;`,
   * and can be used to track the progress of the operation, and to cancel it. The
   * [metadata][google.longrunning.Operation.metadata] field type is
   * [RestoreDatabaseMetadata][google.spanner.admin.database.v1.RestoreDatabaseMetadata]. The
   * [response][google.longrunning.Operation.response] type is
   * [Database][google.spanner.admin.database.v1.Database], if successful. Cancelling the returned
   * operation will stop the restore and delete the database. There can be only one database being
   * restored into an instance at a time. Once the restore operation completes, a new restore
   * operation can be initiated, without waiting for the optimize operation associated with the
   * first restore to complete.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   RestoreDatabaseRequest request =
   *       RestoreDatabaseRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setDatabaseId("databaseId1688905718")
   *           .setEncryptionConfig(RestoreDatabaseEncryptionConfig.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       databaseAdminClient.restoreDatabaseCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<RestoreDatabaseRequest, Operation> restoreDatabaseCallable() {
    return stub.restoreDatabaseCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists database [longrunning-operations][google.longrunning.Operation]. A database operation has
   * a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   for (Operation element : databaseAdminClient.listDatabaseOperations(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance of the database operations. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListDatabaseOperationsPagedResponse listDatabaseOperations(InstanceName parent) {
    ListDatabaseOperationsRequest request =
        ListDatabaseOperationsRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listDatabaseOperations(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists database [longrunning-operations][google.longrunning.Operation]. A database operation has
   * a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   for (Operation element : databaseAdminClient.listDatabaseOperations(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance of the database operations. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListDatabaseOperationsPagedResponse listDatabaseOperations(String parent) {
    ListDatabaseOperationsRequest request =
        ListDatabaseOperationsRequest.newBuilder().setParent(parent).build();
    return listDatabaseOperations(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists database [longrunning-operations][google.longrunning.Operation]. A database operation has
   * a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListDatabaseOperationsRequest request =
   *       ListDatabaseOperationsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   for (Operation element : databaseAdminClient.listDatabaseOperations(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListDatabaseOperationsPagedResponse listDatabaseOperations(
      ListDatabaseOperationsRequest request) {
    return listDatabaseOperationsPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists database [longrunning-operations][google.longrunning.Operation]. A database operation has
   * a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListDatabaseOperationsRequest request =
   *       ListDatabaseOperationsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   ApiFuture<Operation> future =
   *       databaseAdminClient.listDatabaseOperationsPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Operation element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsPagedResponse>
      listDatabaseOperationsPagedCallable() {
    return stub.listDatabaseOperationsPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists database [longrunning-operations][google.longrunning.Operation]. A database operation has
   * a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/databases/&lt;database&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListDatabaseOperationsRequest request =
   *       ListDatabaseOperationsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   while (true) {
   *     ListDatabaseOperationsResponse response =
   *         databaseAdminClient.listDatabaseOperationsCallable().call(request);
   *     for (Operation element : response.getResponsesList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse>
      listDatabaseOperationsCallable() {
    return stub.listDatabaseOperationsCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the backup [long-running operations][google.longrunning.Operation] in the given instance.
   * A backup operation has a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.progress.start_time` in descending order
   * starting from the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   for (Operation element : databaseAdminClient.listBackupOperations(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance of the backup operations. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupOperationsPagedResponse listBackupOperations(InstanceName parent) {
    ListBackupOperationsRequest request =
        ListBackupOperationsRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listBackupOperations(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the backup [long-running operations][google.longrunning.Operation] in the given instance.
   * A backup operation has a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.progress.start_time` in descending order
   * starting from the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   for (Operation element : databaseAdminClient.listBackupOperations(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance of the backup operations. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupOperationsPagedResponse listBackupOperations(String parent) {
    ListBackupOperationsRequest request =
        ListBackupOperationsRequest.newBuilder().setParent(parent).build();
    return listBackupOperations(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the backup [long-running operations][google.longrunning.Operation] in the given instance.
   * A backup operation has a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.progress.start_time` in descending order
   * starting from the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListBackupOperationsRequest request =
   *       ListBackupOperationsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   for (Operation element : databaseAdminClient.listBackupOperations(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupOperationsPagedResponse listBackupOperations(
      ListBackupOperationsRequest request) {
    return listBackupOperationsPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the backup [long-running operations][google.longrunning.Operation] in the given instance.
   * A backup operation has a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.progress.start_time` in descending order
   * starting from the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListBackupOperationsRequest request =
   *       ListBackupOperationsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   ApiFuture<Operation> future =
   *       databaseAdminClient.listBackupOperationsPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Operation element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsPagedResponse>
      listBackupOperationsPagedCallable() {
    return stub.listBackupOperationsPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the backup [long-running operations][google.longrunning.Operation] in the given instance.
   * A backup operation has a name of the form
   * `projects/&lt;project&gt;/instances/&lt;instance&gt;/backups/&lt;backup&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.progress.start_time` in descending order
   * starting from the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
   *   ListBackupOperationsRequest request =
   *       ListBackupOperationsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   while (true) {
   *     ListBackupOperationsResponse response =
   *         databaseAdminClient.listBackupOperationsCallable().call(request);
   *     for (Operation element : response.getResponsesList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListBackupOperationsRequest, ListBackupOperationsResponse>
      listBackupOperationsCallable() {
    return stub.listBackupOperationsCallable();
  }

  @Override
  public final void close() {
    stub.close();
  }

  @Override
  public void shutdown() {
    stub.shutdown();
  }

  @Override
  public boolean isShutdown() {
    return stub.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return stub.isTerminated();
  }

  @Override
  public void shutdownNow() {
    stub.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long duration, TimeUnit unit) throws InterruptedException {
    return stub.awaitTermination(duration, unit);
  }

  public static class ListDatabasesPagedResponse
      extends AbstractPagedListResponse<
          ListDatabasesRequest,
          ListDatabasesResponse,
          Database,
          ListDatabasesPage,
          ListDatabasesFixedSizeCollection> {

    public static ApiFuture<ListDatabasesPagedResponse> createAsync(
        PageContext<ListDatabasesRequest, ListDatabasesResponse, Database> context,
        ApiFuture<ListDatabasesResponse> futureResponse) {
      ApiFuture<ListDatabasesPage> futurePage =
          ListDatabasesPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          new ApiFunction<ListDatabasesPage, ListDatabasesPagedResponse>() {
            @Override
            public ListDatabasesPagedResponse apply(ListDatabasesPage input) {
              return new ListDatabasesPagedResponse(input);
            }
          },
          MoreExecutors.directExecutor());
    }

    private ListDatabasesPagedResponse(ListDatabasesPage page) {
      super(page, ListDatabasesFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListDatabasesPage
      extends AbstractPage<
          ListDatabasesRequest, ListDatabasesResponse, Database, ListDatabasesPage> {

    private ListDatabasesPage(
        PageContext<ListDatabasesRequest, ListDatabasesResponse, Database> context,
        ListDatabasesResponse response) {
      super(context, response);
    }

    private static ListDatabasesPage createEmptyPage() {
      return new ListDatabasesPage(null, null);
    }

    @Override
    protected ListDatabasesPage createPage(
        PageContext<ListDatabasesRequest, ListDatabasesResponse, Database> context,
        ListDatabasesResponse response) {
      return new ListDatabasesPage(context, response);
    }

    @Override
    public ApiFuture<ListDatabasesPage> createPageAsync(
        PageContext<ListDatabasesRequest, ListDatabasesResponse, Database> context,
        ApiFuture<ListDatabasesResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListDatabasesFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListDatabasesRequest,
          ListDatabasesResponse,
          Database,
          ListDatabasesPage,
          ListDatabasesFixedSizeCollection> {

    private ListDatabasesFixedSizeCollection(List<ListDatabasesPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListDatabasesFixedSizeCollection createEmptyCollection() {
      return new ListDatabasesFixedSizeCollection(null, 0);
    }

    @Override
    protected ListDatabasesFixedSizeCollection createCollection(
        List<ListDatabasesPage> pages, int collectionSize) {
      return new ListDatabasesFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListBackupsPagedResponse
      extends AbstractPagedListResponse<
          ListBackupsRequest,
          ListBackupsResponse,
          Backup,
          ListBackupsPage,
          ListBackupsFixedSizeCollection> {

    public static ApiFuture<ListBackupsPagedResponse> createAsync(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ApiFuture<ListBackupsResponse> futureResponse) {
      ApiFuture<ListBackupsPage> futurePage =
          ListBackupsPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          new ApiFunction<ListBackupsPage, ListBackupsPagedResponse>() {
            @Override
            public ListBackupsPagedResponse apply(ListBackupsPage input) {
              return new ListBackupsPagedResponse(input);
            }
          },
          MoreExecutors.directExecutor());
    }

    private ListBackupsPagedResponse(ListBackupsPage page) {
      super(page, ListBackupsFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListBackupsPage
      extends AbstractPage<ListBackupsRequest, ListBackupsResponse, Backup, ListBackupsPage> {

    private ListBackupsPage(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ListBackupsResponse response) {
      super(context, response);
    }

    private static ListBackupsPage createEmptyPage() {
      return new ListBackupsPage(null, null);
    }

    @Override
    protected ListBackupsPage createPage(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ListBackupsResponse response) {
      return new ListBackupsPage(context, response);
    }

    @Override
    public ApiFuture<ListBackupsPage> createPageAsync(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ApiFuture<ListBackupsResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListBackupsFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListBackupsRequest,
          ListBackupsResponse,
          Backup,
          ListBackupsPage,
          ListBackupsFixedSizeCollection> {

    private ListBackupsFixedSizeCollection(List<ListBackupsPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListBackupsFixedSizeCollection createEmptyCollection() {
      return new ListBackupsFixedSizeCollection(null, 0);
    }

    @Override
    protected ListBackupsFixedSizeCollection createCollection(
        List<ListBackupsPage> pages, int collectionSize) {
      return new ListBackupsFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListDatabaseOperationsPagedResponse
      extends AbstractPagedListResponse<
          ListDatabaseOperationsRequest,
          ListDatabaseOperationsResponse,
          Operation,
          ListDatabaseOperationsPage,
          ListDatabaseOperationsFixedSizeCollection> {

    public static ApiFuture<ListDatabaseOperationsPagedResponse> createAsync(
        PageContext<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse, Operation>
            context,
        ApiFuture<ListDatabaseOperationsResponse> futureResponse) {
      ApiFuture<ListDatabaseOperationsPage> futurePage =
          ListDatabaseOperationsPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          new ApiFunction<ListDatabaseOperationsPage, ListDatabaseOperationsPagedResponse>() {
            @Override
            public ListDatabaseOperationsPagedResponse apply(ListDatabaseOperationsPage input) {
              return new ListDatabaseOperationsPagedResponse(input);
            }
          },
          MoreExecutors.directExecutor());
    }

    private ListDatabaseOperationsPagedResponse(ListDatabaseOperationsPage page) {
      super(page, ListDatabaseOperationsFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListDatabaseOperationsPage
      extends AbstractPage<
          ListDatabaseOperationsRequest,
          ListDatabaseOperationsResponse,
          Operation,
          ListDatabaseOperationsPage> {

    private ListDatabaseOperationsPage(
        PageContext<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse, Operation>
            context,
        ListDatabaseOperationsResponse response) {
      super(context, response);
    }

    private static ListDatabaseOperationsPage createEmptyPage() {
      return new ListDatabaseOperationsPage(null, null);
    }

    @Override
    protected ListDatabaseOperationsPage createPage(
        PageContext<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse, Operation>
            context,
        ListDatabaseOperationsResponse response) {
      return new ListDatabaseOperationsPage(context, response);
    }

    @Override
    public ApiFuture<ListDatabaseOperationsPage> createPageAsync(
        PageContext<ListDatabaseOperationsRequest, ListDatabaseOperationsResponse, Operation>
            context,
        ApiFuture<ListDatabaseOperationsResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListDatabaseOperationsFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListDatabaseOperationsRequest,
          ListDatabaseOperationsResponse,
          Operation,
          ListDatabaseOperationsPage,
          ListDatabaseOperationsFixedSizeCollection> {

    private ListDatabaseOperationsFixedSizeCollection(
        List<ListDatabaseOperationsPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListDatabaseOperationsFixedSizeCollection createEmptyCollection() {
      return new ListDatabaseOperationsFixedSizeCollection(null, 0);
    }

    @Override
    protected ListDatabaseOperationsFixedSizeCollection createCollection(
        List<ListDatabaseOperationsPage> pages, int collectionSize) {
      return new ListDatabaseOperationsFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListBackupOperationsPagedResponse
      extends AbstractPagedListResponse<
          ListBackupOperationsRequest,
          ListBackupOperationsResponse,
          Operation,
          ListBackupOperationsPage,
          ListBackupOperationsFixedSizeCollection> {

    public static ApiFuture<ListBackupOperationsPagedResponse> createAsync(
        PageContext<ListBackupOperationsRequest, ListBackupOperationsResponse, Operation> context,
        ApiFuture<ListBackupOperationsResponse> futureResponse) {
      ApiFuture<ListBackupOperationsPage> futurePage =
          ListBackupOperationsPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          new ApiFunction<ListBackupOperationsPage, ListBackupOperationsPagedResponse>() {
            @Override
            public ListBackupOperationsPagedResponse apply(ListBackupOperationsPage input) {
              return new ListBackupOperationsPagedResponse(input);
            }
          },
          MoreExecutors.directExecutor());
    }

    private ListBackupOperationsPagedResponse(ListBackupOperationsPage page) {
      super(page, ListBackupOperationsFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListBackupOperationsPage
      extends AbstractPage<
          ListBackupOperationsRequest,
          ListBackupOperationsResponse,
          Operation,
          ListBackupOperationsPage> {

    private ListBackupOperationsPage(
        PageContext<ListBackupOperationsRequest, ListBackupOperationsResponse, Operation> context,
        ListBackupOperationsResponse response) {
      super(context, response);
    }

    private static ListBackupOperationsPage createEmptyPage() {
      return new ListBackupOperationsPage(null, null);
    }

    @Override
    protected ListBackupOperationsPage createPage(
        PageContext<ListBackupOperationsRequest, ListBackupOperationsResponse, Operation> context,
        ListBackupOperationsResponse response) {
      return new ListBackupOperationsPage(context, response);
    }

    @Override
    public ApiFuture<ListBackupOperationsPage> createPageAsync(
        PageContext<ListBackupOperationsRequest, ListBackupOperationsResponse, Operation> context,
        ApiFuture<ListBackupOperationsResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListBackupOperationsFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListBackupOperationsRequest,
          ListBackupOperationsResponse,
          Operation,
          ListBackupOperationsPage,
          ListBackupOperationsFixedSizeCollection> {

    private ListBackupOperationsFixedSizeCollection(
        List<ListBackupOperationsPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListBackupOperationsFixedSizeCollection createEmptyCollection() {
      return new ListBackupOperationsFixedSizeCollection(null, 0);
    }

    @Override
    protected ListBackupOperationsFixedSizeCollection createCollection(
        List<ListBackupOperationsPage> pages, int collectionSize) {
      return new ListBackupOperationsFixedSizeCollection(pages, collectionSize);
    }
  }
}
