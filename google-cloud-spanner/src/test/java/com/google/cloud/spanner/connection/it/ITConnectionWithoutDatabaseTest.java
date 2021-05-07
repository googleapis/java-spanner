/*
 * Copyright 2021 Google LLC
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.Connection;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITConnectionWithoutDatabaseTest extends ITAbstractSpannerTest {

  /**
   * This shadows the same method in the super class to prevent it from creating a test database.
   */
  @BeforeClass
  public static void setup() {}

  @Test
  public void testCreateUseAlterDropDatabase() {
    try (ITConnection connection = createConnectionWithoutDb()) {
      // This connection does not have a database, so queries are not allowed.
      assertThrows(SpannerException.class, () -> connection.execute(Statement.of("SELECT 1")));

      String databaseId = "testdb_" + new Random().nextInt(100000000);
      // Ensure that the database does not exist.
      assertFalse(databaseExists(connection, databaseId));

      // Create a database and alter the options of it.
      connection.execute(Statement.of(String.format("CREATE DATABASE `%s`", databaseId)));
      connection.execute(
          Statement.of(
              String.format("ALTER DATABASE `%s` SET OPTIONS (optimizer_version=1)", databaseId)));

      // Instruct the connection to use the newly created database.
      connection.execute(Statement.of(String.format("USE DATABASE `%s`", databaseId)));
      // This should now work as the connection is using the database that was created.
      try (ResultSet resultSet = connection.executeQuery(Statement.of("SELECT 1"))) {
        assertTrue(resultSet.next());
        assertEquals(1L, resultSet.getLong(0));
        assertFalse(resultSet.next());
      }
      // The options that we set above should also be visible now that we are using this database.
      try (ResultSet resultSet =
          connection.executeQuery(
              Statement.of("SELECT * FROM INFORMATION_SCHEMA.DATABASE_OPTIONS"))) {
        assertTrue(resultSet.next());
        assertEquals("optimizer_version", resultSet.getString("OPTION_NAME"));
        assertEquals("1", resultSet.getString("OPTION_VALUE"));
        assertFalse(resultSet.next());
      }

      // The database should show up in the list of all databases.
      assertTrue(databaseExists(connection, databaseId));
      // Drop the database that we are currently using.
      connection.execute(Statement.of(String.format("DROP DATABASE `%s`", databaseId)));
      // The database should no longer show up in the list of all databases.
      assertFalse(databaseExists(connection, databaseId));
    }
  }

  private boolean databaseExists(Connection connection, String id) {
    try (ResultSet resultSet =
        connection.execute(Statement.of("SHOW VARIABLE DATABASES")).getResultSet()) {
      while (resultSet.next()) {
        if (resultSet.getString("NAME").equals(id)) {
          return true;
        }
      }
    }
    return false;
  }
}
