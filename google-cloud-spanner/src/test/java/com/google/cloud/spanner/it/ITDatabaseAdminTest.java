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

package com.google.cloud.spanner.it;

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import com.google.api.gax.grpc.GrpcInterceptorProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Backup;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for {@link com.google.cloud.spanner.DatabaseAdminClient}. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITDatabaseAdminTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private DatabaseAdminClient dbAdminClient;
  private RemoteSpannerHelper testHelper;
  private List<Database> dbs = new ArrayList<>();

  @Before
  public void setUp() {
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
  }

  @After
  public void tearDown() {
    for (Database db : dbs) {
      db.drop();
    }
    dbs.clear();
  }

  @Test
  public void databaseOperations() throws Exception {
    String dbId = testHelper.getUniqueDatabaseId();
    String instanceId = testHelper.getInstanceId().getInstance();
    String statement1 = "CREATE TABLE T (\n" + "  K STRING(MAX),\n" + ") PRIMARY KEY(K)";
    OperationFuture<Database, CreateDatabaseMetadata> op =
        dbAdminClient.createDatabase(instanceId, dbId, ImmutableList.of(statement1));
    Database db = op.get();
    dbs.add(db);
    assertThat(db.getId().getDatabase()).isEqualTo(dbId);

    db = dbAdminClient.getDatabase(instanceId, dbId);
    assertThat(db.getId().getDatabase()).isEqualTo(dbId);

    boolean foundDb = false;
    for (Database dbInList :
        Iterators.toArray(
            dbAdminClient.listDatabases(instanceId).iterateAll().iterator(), Database.class)) {
      if (dbInList.getId().getDatabase().equals(dbId)) {
        foundDb = true;
        break;
      }
    }
    assertThat(foundDb).isTrue();

    String statement2 = "CREATE TABLE T2 (\n" + "  K2 STRING(MAX),\n" + ") PRIMARY KEY(K2)";
    OperationFuture<?, ?> op2 =
        dbAdminClient.updateDatabaseDdl(instanceId, dbId, ImmutableList.of(statement2), null);
    op2.get();
    List<String> statementsInDb = dbAdminClient.getDatabaseDdl(instanceId, dbId);
    assertThat(statementsInDb).containsExactly(statement1, statement2);

    dbAdminClient.dropDatabase(instanceId, dbId);
    dbs.clear();
    try {
      db = dbAdminClient.getDatabase(testHelper.getInstanceId().getInstance(), dbId);
      fail("Expected exception");
    } catch (SpannerException ex) {
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }
  }

  @Test
  public void updateDdlRetry() throws Exception {
    String dbId = testHelper.getUniqueDatabaseId();
    String instanceId = testHelper.getInstanceId().getInstance();
    String statement1 = "CREATE TABLE T (\n" + "  K STRING(MAX),\n" + ") PRIMARY KEY(K)";
    OperationFuture<Database, CreateDatabaseMetadata> op =
        dbAdminClient.createDatabase(instanceId, dbId, ImmutableList.of(statement1));
    Database db = op.get();
    dbs.add(db);
    String statement2 = "CREATE TABLE T2 (\n" + "  K2 STRING(MAX),\n" + ") PRIMARY KEY(K2)";
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op1 =
        dbAdminClient.updateDatabaseDdl(instanceId, dbId, ImmutableList.of(statement2), "myop");
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op2 =
        dbAdminClient.updateDatabaseDdl(instanceId, dbId, ImmutableList.of(statement2), "myop");
    op1.get();
    op2.get();
    assertThat(op1.getMetadata().get()).isEqualTo(op2.getMetadata().get());
  }

  @Test
  public void databaseOperationsViaEntity() throws Exception {
    String dbId = testHelper.getUniqueDatabaseId();
    String instanceId = testHelper.getInstanceId().getInstance();
    String statement1 = "CREATE TABLE T (\n" + "  K STRING(MAX),\n" + ") PRIMARY KEY(K)";
    OperationFuture<Database, CreateDatabaseMetadata> op =
        dbAdminClient.createDatabase(instanceId, dbId, ImmutableList.of(statement1));
    Database db = op.get();
    dbs.add(db);
    assertThat(db.getId().getDatabase()).isEqualTo(dbId);

    db = db.reload();
    assertThat(db.getId().getDatabase()).isEqualTo(dbId);

    String statement2 = "CREATE TABLE T2 (\n" + "  K2 STRING(MAX),\n" + ") PRIMARY KEY(K2)";
    OperationFuture<?, ?> op2 = db.updateDdl(ImmutableList.of(statement2), null);
    op2.get();
    Iterable<String> statementsInDb = db.getDdl();
    assertThat(statementsInDb).containsExactly(statement1, statement2);
    db.drop();
    dbs.clear();
    try {
      db.reload();
      fail("Expected exception");
    } catch (SpannerException ex) {
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }
  }

  @Test
  public void listPagination() throws Exception {
    List<String> dbIds =
        ImmutableList.of(
            testHelper.getUniqueDatabaseId(),
            testHelper.getUniqueDatabaseId(),
            testHelper.getUniqueDatabaseId());

    String instanceId = testHelper.getInstanceId().getInstance();
    for (String dbId : dbIds) {
      dbs.add(dbAdminClient.createDatabase(instanceId, dbId, ImmutableList.<String>of()).get());
    }
    Page<Database> page = dbAdminClient.listDatabases(instanceId, Options.pageSize(1));
    List<String> dbIdsGot = new ArrayList<>();
    // A valid page will contain 0 or 1 elements.
    while (page != null && page.getValues().iterator().hasNext()) {
      Database db = Iterables.getOnlyElement(page.getValues());
      dbIdsGot.add(db.getId().getDatabase());
      // page.getNextPage() will return null if the previous call did not return a 'nextPageToken'.
      // That is an indication that the server knows that there are no more results. The method may
      // however also return a page with zero results. That happens if there was another result on
      // the server when the previous call was executed (and returned a nextPageToken), but that
      // result has been deleted in the meantime.
      page = page.getNextPage();
    }
    assertThat(dbIdsGot).containsAtLeastElementsIn(dbIds);
  }

  private static final class InjectErrorInterceptorProvider implements GrpcInterceptorProvider {
    final AtomicBoolean injectError = new AtomicBoolean(true);
    final AtomicInteger getOperationCount = new AtomicInteger();
    final AtomicInteger methodCount = new AtomicInteger();
    final String methodName;

    private InjectErrorInterceptorProvider(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public List<ClientInterceptor> getInterceptors() {
      ClientInterceptor interceptor =
          new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
              if (method.getFullMethodName().contains("GetOperation")) {
                getOperationCount.incrementAndGet();
              }
              if (!method.getFullMethodName().contains(methodName)) {
                return next.newCall(method, callOptions);
              }

              methodCount.incrementAndGet();
              final AtomicBoolean errorInjected = new AtomicBoolean();
              final ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);

              return new SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                  super.start(
                      new SimpleForwardingClientCallListener<RespT>(responseListener) {
                        @Override
                        public void onMessage(RespT message) {
                          if (injectError.getAndSet(false)) {
                            errorInjected.set(true);
                            clientCall.cancel("Cancelling call for injected error", null);
                          } else {
                            super.onMessage(message);
                          }
                        }

                        @Override
                        public void onClose(Status status, Metadata metadata) {
                          if (errorInjected.get()) {
                            status = Status.UNAVAILABLE.augmentDescription("INJECTED BY TEST");
                          }
                          super.onClose(status, metadata);
                        }
                      },
                      headers);
                }
              };
            }
          };
      return Collections.singletonList(interceptor);
    }
  }

  @Test
  public void testRetryNonIdempotentRpcsReturningLongRunningOperations() throws Exception {
    assumeFalse(
        "Querying long-running operations is not supported on the emulator", isUsingEmulator());

    // RPCs that return a long-running operation such as CreateDatabase, CreateBackup and
    // RestoreDatabase are non-idempotent and can normally not be automatically retried in case of a
    // transient failure. The client library will however automatically query the backend to check
    // whether the corresponding operation was started or not, and if it was, it will pick up the
    // existing operation. If no operation is found, a new RPC call will be executed to start the
    // operation.

    List<Database> databases = new ArrayList<>();
    List<Backup> backups = new ArrayList<>();
    String initialDatabaseId;
    Timestamp initialDbCreateTime;

    try {
      // CreateDatabase
      InjectErrorInterceptorProvider createDbInterceptor =
          new InjectErrorInterceptorProvider("CreateDatabase");
      SpannerOptions options =
          testHelper.getOptions().toBuilder().setInterceptorProvider(createDbInterceptor).build();
      try (Spanner spanner = options.getService()) {
        initialDatabaseId = testHelper.getUniqueDatabaseId();
        DatabaseAdminClient client = spanner.getDatabaseAdminClient();
        OperationFuture<Database, CreateDatabaseMetadata> op =
            client.createDatabase(
                testHelper.getInstanceId().getInstance(),
                initialDatabaseId,
                Collections.<String>emptyList());
        databases.add(op.get());
        // Keep track of the original create time of this database, as we will drop this database
        // later and create another one with the exact same name. That means that the ListOperations
        // call will return at least two CreateDatabase operations. The retry logic should always
        // pick the last one.
        initialDbCreateTime = op.get().getCreateTime();
        // Assert that the CreateDatabase RPC was called only once, and that the operation tracking
        // was resumed through a GetOperation call.
        assertThat(createDbInterceptor.methodCount.get()).isEqualTo(1);
        assertThat(createDbInterceptor.getOperationCount.get()).isAtLeast(1);
      }

      // CreateBackup
      InjectErrorInterceptorProvider createBackupInterceptor =
          new InjectErrorInterceptorProvider("CreateBackup");
      options =
          testHelper
              .getOptions()
              .toBuilder()
              .setInterceptorProvider(createBackupInterceptor)
              .build();
      try (Spanner spanner = options.getService()) {
        String databaseId = databases.get(0).getId().getDatabase();
        String backupId = String.format("test-bck-%08d", new Random().nextInt(100000000));
        DatabaseAdminClient client = spanner.getDatabaseAdminClient();
        OperationFuture<Backup, CreateBackupMetadata> op =
            client.createBackup(
                testHelper.getInstanceId().getInstance(),
                backupId,
                databaseId,
                Timestamp.ofTimeSecondsAndNanos(
                    Timestamp.now().getSeconds() + TimeUnit.SECONDS.convert(7L, TimeUnit.DAYS), 0));
        backups.add(op.get());
        // Assert that the CreateBackup RPC was called only once, and that the operation tracking
        // was resumed through a GetOperation call.
        assertThat(createDbInterceptor.methodCount.get()).isEqualTo(1);
        assertThat(createDbInterceptor.getOperationCount.get()).isAtLeast(1);
      }

      // RestoreBackup
      int attempts = 0;
      while (true) {
        InjectErrorInterceptorProvider restoreBackupInterceptor =
            new InjectErrorInterceptorProvider("RestoreBackup");
        options =
            testHelper
                .getOptions()
                .toBuilder()
                .setInterceptorProvider(restoreBackupInterceptor)
                .build();
        try (Spanner spanner = options.getService()) {
          String backupId = backups.get(0).getId().getBackup();
          String restoredDbId = testHelper.getUniqueDatabaseId();
          DatabaseAdminClient client = spanner.getDatabaseAdminClient();
          OperationFuture<Database, RestoreDatabaseMetadata> op =
              client.restoreDatabase(
                  testHelper.getInstanceId().getInstance(),
                  backupId,
                  testHelper.getInstanceId().getInstance(),
                  restoredDbId);
          databases.add(op.get());
          // Assert that the RestoreDatabase RPC was called only once, and that the operation
          // tracking was resumed through a GetOperation call.
          assertThat(createDbInterceptor.methodCount.get()).isEqualTo(1);
          assertThat(createDbInterceptor.getOperationCount.get()).isAtLeast(1);
          break;
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SpannerException
              && ((SpannerException) e.getCause()).getErrorCode() == ErrorCode.FAILED_PRECONDITION
              && e.getCause()
                  .getMessage()
                  .contains("Please retry the operation once the pending restores complete")) {
            attempts++;
            if (attempts == 10) {
              // Still same error after 10 attempts. Ignore.
              break;
            }
            // wait and then retry.
            Thread.sleep(60_000L);
          } else {
            throw e;
          }
        }
      }

      // Create another database with the exact same name as the first database.
      createDbInterceptor = new InjectErrorInterceptorProvider("CreateDatabase");
      options =
          testHelper.getOptions().toBuilder().setInterceptorProvider(createDbInterceptor).build();
      try (Spanner spanner = options.getService()) {
        DatabaseAdminClient client = spanner.getDatabaseAdminClient();
        // First drop the initial database.
        client.dropDatabase(testHelper.getInstanceId().getInstance(), initialDatabaseId);
        // Now re-create a database with the exact same name.
        OperationFuture<Database, CreateDatabaseMetadata> op =
            client.createDatabase(
                testHelper.getInstanceId().getInstance(),
                initialDatabaseId,
                Collections.<String>emptyList());
        // Check that the second database was created and has a greater creation time than the
        // first.
        Timestamp secondCreationTime = op.get().getCreateTime();
        // TODO: Change this to greaterThan when the create time of a database is reported back by
        // the server.
        assertThat(secondCreationTime).isAtLeast(initialDbCreateTime);
        // Assert that the CreateDatabase RPC was called only once, and that the operation tracking
        // was resumed through a GetOperation call.
        assertThat(createDbInterceptor.methodCount.get()).isEqualTo(1);
        assertThat(createDbInterceptor.getOperationCount.get()).isAtLeast(1);
      }
    } finally {
      DatabaseAdminClient client = testHelper.getClient().getDatabaseAdminClient();
      for (Database database : databases) {
        client.dropDatabase(
            database.getId().getInstanceId().getInstance(), database.getId().getDatabase());
      }
      for (Backup backup : backups) {
        client.deleteBackup(backup.getInstanceId().getInstance(), backup.getId().getBackup());
      }
    }
  }
}
