/*
 * Copyright 2025 Google LLC
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

import static com.google.cloud.spanner.connection.ConnectionProperties.DEFAULT_ISOLATION_LEVEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.ITConnection;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.TransactionOptions.IsolationLevel;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TransactionMockServerTest extends AbstractMockServerTest {

  @Parameter public IsolationLevel isolationLevel;

  @Parameters(name = "isolationLevel = {0}")
  public static Object[] data() {
    return DEFAULT_ISOLATION_LEVEL.getValidValues();
  }

  @Override
  protected ITConnection createConnection() {
    return createConnection(
        Collections.emptyList(),
        Collections.emptyList(),
        String.format(";default_isolation_level=%s", isolationLevel));
  }

  @Test
  public void testQuery() {
    try (Connection connection = createConnection()) {
      //noinspection EmptyTryBlock
      try (ResultSet ignore = connection.executeQuery(SELECT1_STATEMENT)) {}
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertFalse(request.getLastStatement());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testDml() {
    try (Connection connection = createConnection()) {
      connection.executeUpdate(INSERT_STATEMENT);
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertFalse(request.getLastStatement());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testDmlReturning() {
    try (Connection connection = createConnection()) {
      //noinspection EmptyTryBlock
      try (ResultSet ignore = connection.executeQuery(INSERT_RETURNING_STATEMENT)) {}
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
    ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertFalse(request.getLastStatement());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testBatchDml() {
    try (Connection connection = createConnection()) {
      connection.startBatchDml();
      connection.executeUpdate(INSERT_STATEMENT);
      connection.executeUpdate(INSERT_STATEMENT);
      connection.runBatch();
      connection.commit();
    }
    assertEquals(1, mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class));
    ExecuteBatchDmlRequest request =
        mockSpanner.getRequestsOfType(ExecuteBatchDmlRequest.class).get(0);
    assertTrue(request.getTransaction().hasBegin());
    assertTrue(request.getTransaction().getBegin().hasReadWrite());
    assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
    assertFalse(request.getLastStatements());
    assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));
  }

  @Test
  public void testTransactionIsolationLevel() {
    try (Connection connection = createConnection()) {
      for (IsolationLevel isolationLevel :
          new IsolationLevel[] {IsolationLevel.REPEATABLE_READ, IsolationLevel.SERIALIZABLE}) {
        for (boolean useSql : new boolean[] {true, false}) {
          if (useSql) {
            connection.execute(
                Statement.of(
                    "begin transaction isolation level "
                        + isolationLevel.name().replace("_", " ")));
          } else {
            connection.beginTransaction(isolationLevel);
          }
          connection.executeUpdate(INSERT_STATEMENT);
          connection.commit();

          assertEquals(1, mockSpanner.countRequestsOfType(ExecuteSqlRequest.class));
          ExecuteSqlRequest request = mockSpanner.getRequestsOfType(ExecuteSqlRequest.class).get(0);
          assertTrue(request.getTransaction().hasBegin());
          assertTrue(request.getTransaction().getBegin().hasReadWrite());
          assertEquals(isolationLevel, request.getTransaction().getBegin().getIsolationLevel());
          assertFalse(request.getLastStatement());
          assertEquals(1, mockSpanner.countRequestsOfType(CommitRequest.class));

          mockSpanner.clearRequests();
        }
      }
    }
  }
}
