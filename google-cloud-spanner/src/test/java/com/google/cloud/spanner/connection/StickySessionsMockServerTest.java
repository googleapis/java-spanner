/*
 * Copyright 2024 Google LLC
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

package com.google.cloud.spanner.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.ResultSet;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StickySessionsMockServerTest extends AbstractMockServerTest {

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
  }

  @Test
  public void testQuery() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        for (int i = 0; i < 2; i++) {
          try (ResultSet resultSet = connection.executeQuery(SELECT_COUNT_STATEMENT)) {
            assertTrue(resultSet.next());
            assertEquals(0L, resultSet.getLong(0));
            assertFalse(resultSet.next());
          }
          if (!autocommit) {
            connection.commit();
          }
        }
      }
    }
  }

  @Test
  public void testUpdate() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        for (int i = 0; i < 2; i++) {
          assertEquals(1L, connection.executeUpdate(INSERT_STATEMENT));
          if (!autocommit) {
            connection.commit();
          }
        }
      }
    }
  }

  @Test
  public void testTransaction() {
    try (Connection connection = createConnection()) {
      for (int i = 0; i < 2; i++) {
        try (ResultSet resultSet = connection.executeQuery(SELECT_COUNT_STATEMENT)) {
          assertTrue(resultSet.next());
          assertEquals(0L, resultSet.getLong(0));
          assertFalse(resultSet.next());
        }
        assertEquals(1L, connection.executeUpdate(INSERT_STATEMENT));
        connection.commit();
      }
    }
  }
}
