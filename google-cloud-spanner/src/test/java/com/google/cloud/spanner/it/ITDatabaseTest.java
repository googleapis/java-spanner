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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import com.google.api.client.util.ExponentialBackOff;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.DatabaseNotFoundException;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceNotFoundException;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerImpl;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for database admin functionality: DDL etc. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITDatabaseTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  /** Executor for repeatedly reconnecting to Cloud Spanner. */
  private static final ScheduledExecutorService reconnectExecutor =
      Executors.newSingleThreadScheduledExecutor();

  /** Counter for the number of times that we forced a reconnect to Spanner. */
  private static final AtomicInteger RECONNECT_COUNT = new AtomicInteger();

  @BeforeClass
  public static void setupReconnectExecutor() {
    // Schedule a reconnect every 500 to 1500 milliseconds.
    int initialDelay = ThreadLocalRandom.current().nextInt(1000) + 500;
    int period = ThreadLocalRandom.current().nextInt(1000) + 500;
    reconnectExecutor.scheduleAtFixedRate(
        ITDatabaseTest::reconnectSpanner, initialDelay, period, TimeUnit.MILLISECONDS);
  }

  @AfterClass
  public static void shutdownReconnectExecutor() {
    reconnectExecutor.shutdown();
    assertTrue("Spanner should have reconnected at least once", RECONNECT_COUNT.get() > 0);
  }

  static void reconnectSpanner() {
    ((SpannerImpl) env.getTestHelper().getClient()).getRpc().createStubs(false);
    RECONNECT_COUNT.incrementAndGet();
  }

  @Test
  public void badDdl() {
    try {
      env.getTestHelper().createTestDatabase("CREATE TABLE T ( Illegal Way To Define A Table )");
      fail("Expected exception");
    } catch (SpannerException ex) {
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(ex.getMessage()).contains("Syntax error on line 1");
    }
  }

  @Test
  public void databaseDeletedTest() throws Exception {
    // Create a test db, do a query, then delete it and verify that it returns
    // DatabaseNotFoundExceptions.
    Database db = env.getTestHelper().createTestDatabase();
    DatabaseClient client = env.getTestHelper().getClient().getDatabaseClient(db.getId());
    try (ResultSet rs = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }

    // Delete the database.
    db.drop();
    // We need to wait a little before Spanner actually starts sending DatabaseNotFound errors.
    ExponentialBackOff backoff =
        new ExponentialBackOff.Builder()
            .setInitialIntervalMillis(1000)
            .setMaxElapsedTimeMillis(65000)
            .setMaxIntervalMillis(5000)
            .build();
    DatabaseNotFoundException notFoundException = null;
    long millis = 0L;
    while ((millis = backoff.nextBackOffMillis()) != ExponentialBackOff.STOP) {
      Thread.sleep(millis);
      // Queries to this database should eventually return DatabaseNotFoundExceptions.
      try (ResultSet rs = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
        rs.next();
      } catch (DatabaseNotFoundException e) {
        // This is what we expect.
        notFoundException = e;
        break;
      }
    }
    assertThat(notFoundException).isNotNull();

    // Now re-create a database with the same name.
    OperationFuture<Database, CreateDatabaseMetadata> op =
        env.getTestHelper()
            .getClient()
            .getDatabaseAdminClient()
            .createDatabase(
                db.getId().getInstanceId().getInstance(),
                db.getId().getDatabase(),
                Collections.emptyList());
    Database newDb = op.get();

    // Queries using the same DatabaseClient should still return DatabaseNotFoundExceptions.
    try (ResultSet rs = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
      rs.next();
      fail("Missing expected DatabaseNotFoundException");
    } catch (DatabaseNotFoundException e) {
      // This is what we expect.
    }

    // Now get a new DatabaseClient for the database. This should now result in a valid
    // DatabaseClient.
    DatabaseClient newClient = env.getTestHelper().getClient().getDatabaseClient(newDb.getId());
    try (ResultSet rs = newClient.singleUse().executeQuery(Statement.of("SELECT 1"))) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getLong(0)).isEqualTo(1L);
      assertThat(rs.next()).isFalse();
    }
  }

  @Test
  public void instanceNotFound() {
    InstanceId testId = env.getTestHelper().getInstanceId();
    InstanceId nonExistingInstanceId =
        InstanceId.of(testId.getProject(), testId.getInstance() + "-na");
    DatabaseClient client =
        env.getTestHelper()
            .getClient()
            .getDatabaseClient(DatabaseId.of(nonExistingInstanceId, "some-db"));
    try (ResultSet rs = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
      rs.next();
      fail("missing expected exception");
    } catch (InstanceNotFoundException e) {
      assertThat(e.getResourceName()).isEqualTo(nonExistingInstanceId.getName());
    }
  }

  @Test
  public void testNumericPrimaryKey()
      throws InterruptedException, ExecutionException, TimeoutException {
    assumeFalse("Emulator does not support numeric primary keys", isUsingEmulator());

    final String projectId = env.getTestHelper().getInstanceId().getProject();
    final String instanceId = env.getTestHelper().getInstanceId().getInstance();
    final String databaseId = env.getTestHelper().getUniqueDatabaseId();
    final String table = "NumericTable";
    final DatabaseId id = DatabaseId.of(projectId, instanceId, databaseId);
    final DatabaseAdminClient databaseAdminClient =
        env.getTestHelper().getClient().getDatabaseAdminClient();

    try {
      // Creates table with numeric primary key
      final OperationFuture<Database, CreateDatabaseMetadata> operation =
          databaseAdminClient.createDatabase(
              instanceId,
              databaseId,
              Collections.singletonList(
                  "CREATE TABLE " + table + " (" + "Id NUMERIC NOT NULL" + ") PRIMARY KEY (Id)"));
      final Database database = operation.get(10, TimeUnit.MINUTES);
      assertNotNull(database);

      // Writes data into the table
      final DatabaseClient databaseClient = env.getTestHelper().getClient().getDatabaseClient(id);
      final ArrayList<Mutation> mutations = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        mutations.add(
            Mutation.newInsertBuilder(table).set("Id").to(new BigDecimal(i + "")).build());
      }
      databaseClient.write(mutations);

      // Reads the data to verify the writes
      try (final ResultSet resultSet =
          databaseClient.singleUse().read(table, KeySet.all(), Collections.singletonList("Id"))) {
        for (int i = 0; resultSet.next(); i++) {
          assertEquals(new BigDecimal(i + ""), resultSet.getBigDecimal("Id"));
        }
      }

      // Deletes data from the table, leaving only the Id = 0 row
      databaseClient
          .readWriteTransaction()
          .run(
              new TransactionCallable<Object>() {
                @Nullable
                @Override
                public Object run(TransactionContext transaction) throws Exception {
                  transaction.executeUpdate(Statement.of("DELETE FROM " + table + " WHERE Id > 0"));
                  return null;
                }
              });

      // Reads the data to verify the deletes only left a single row left
      try (final ResultSet resultSet =
          databaseClient
              .singleUse()
              .executeQuery(Statement.of("SELECT COUNT(1) as cnt FROM " + table))) {
        resultSet.next();
        assertEquals(1L, resultSet.getLong("cnt"));
      }
    } finally {
      databaseAdminClient.dropDatabase(instanceId, databaseId);
    }
  }
}
