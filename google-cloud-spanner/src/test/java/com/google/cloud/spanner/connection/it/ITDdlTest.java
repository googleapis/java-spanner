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

package com.google.cloud.spanner.connection.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.api.core.ApiFuture;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseNotFoundException;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.Connection;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest;
import com.google.cloud.spanner.connection.SqlScriptVerifier;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Execute DDL statements using the generic connection API. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITDdlTest extends ITAbstractSpannerTest {

  @Test
  public void testSqlScript() throws Exception {
    SqlScriptVerifier verifier = new SqlScriptVerifier(new ITConnectionProvider());
    verifier.verifyStatementsInFile("ITDdlTest.sql", SqlScriptVerifier.class, false);
  }

  @Test
  public void testCreateDatabase() {
    DatabaseAdminClient client = getTestEnv().getTestHelper().getClient().getDatabaseAdminClient();
    String instance = getTestEnv().getTestHelper().getInstanceId().getInstance();
    String name = getTestEnv().getTestHelper().getUniqueDatabaseId();

    assertThrows(DatabaseNotFoundException.class, () -> client.getDatabase(instance, name));

    try (Connection connection = createConnection()) {
      connection.execute(Statement.of(String.format("CREATE DATABASE `%s`", name)));
      assertNotNull(client.getDatabase(instance, name));
    } finally {
      client.dropDatabase(instance, name);
    }
  }

  @Test
  public void testCreateDatabaseExecuteUpdate() {
    DatabaseAdminClient client = getTestEnv().getTestHelper().getClient().getDatabaseAdminClient();
    String instance = getTestEnv().getTestHelper().getInstanceId().getInstance();
    String name = getTestEnv().getTestHelper().getUniqueDatabaseId();

    try (Connection connection = createConnection()) {
      long updateCount =
          connection.executeUpdate(Statement.of(String.format("CREATE DATABASE `%s`", name)));
      assertEquals(updateCount, 0);
    } finally {
      client.dropDatabase(instance, name);
    }
  }

  @Test
  public void testCreateDatabaseExecuteUpdateAsync() {
    DatabaseAdminClient client = getTestEnv().getTestHelper().getClient().getDatabaseAdminClient();
    String instance = getTestEnv().getTestHelper().getInstanceId().getInstance();
    String name = getTestEnv().getTestHelper().getUniqueDatabaseId();

    try (Connection connection = createConnection()) {
      ApiFuture<Long> updateCountFuture =
          connection.executeUpdateAsync(Statement.of(String.format("CREATE DATABASE `%s`", name)));
      long updateCount = updateCountFuture.get();
      assertEquals(updateCount, 0L);
    } catch (ExecutionException | InterruptedException e) {
      // pass
    } finally {
      client.dropDatabase(instance, name);
    }
  }
}
