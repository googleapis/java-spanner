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

import com.google.api.client.util.ExponentialBackOff;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.DatabaseNotFoundException;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceNotFoundException;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import java.util.Collections;
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
    // TODO: Remove the following line once the emulator returns ResourceInfo for Database not found
    // errors.
    assumeFalse(
        "Emulator does not return ResourceInfo for Database not found errors", isUsingEmulator());

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
            .setMaxElapsedTimeMillis(35000)
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
    // TODO: Remove the following line when the emulator returns ResourceInfo for Instance not found
    // errors.
    assumeFalse(
        "Emulator does not return ResourceInfo for Instance not found errors", isUsingEmulator());

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
}
