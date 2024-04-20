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

package com.google.cloud.spanner.it;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.IntegrationTestWithClosedSessionsEnv;
import com.google.cloud.spanner.IntegrationTestWithClosedSessionsEnv.DatabaseClientWithClosedSessionImpl;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SessionNotFoundException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;
import com.google.cloud.spanner.TransactionRunner;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test the automatic re-creation of sessions that have been invalidated by the server. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITClosedSessionTest {
  // Run each test case twice to ensure that a retried session does not affect subsequent
  // transactions.
  private static final int RUNS_PER_TEST_CASE = 2;

  @ClassRule
  public static IntegrationTestWithClosedSessionsEnv env =
      new IntegrationTestWithClosedSessionsEnv();

  private static Database db;
  private static DatabaseClientWithClosedSessionImpl client;

  @BeforeClass
  public static void setUpDatabase() {
    // For multiplexed sessions, it will never be invalidated by the server and hence the client
    // will never receive an exception with code NOT_FOUND and the text 'Session not found'.
    assumeFalse(
        env.getTestHelper().getOptions().getSessionPoolOptions().getUseMultiplexedSession());

    // Empty database.
    db = env.getTestHelper().createTestDatabase();
    client = (DatabaseClientWithClosedSessionImpl) env.getTestHelper().getDatabaseClient(db);
  }

  @Before
  public void setup() {
    client.setAllowSessionReplacing(true);
  }

  @Test
  public void testSingleUse() {
    // This should trigger an exception with code NOT_FOUND and the text 'Session not found'.
    client.invalidateNextSession();
    for (int run = 0; run < RUNS_PER_TEST_CASE; run++) {
      try (ResultSet rs = Statement.of("SELECT 1").executeQuery(client.singleUse())) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(0)).isEqualTo(1L);
        assertThat(rs.next()).isFalse();
      }
    }
  }

  @Test
  public void testSingleUseNoRecreation() {
    // This should trigger an exception with code NOT_FOUND and the text 'Session not found'.
    client.setAllowSessionReplacing(false);
    client.invalidateNextSession();
    try (ResultSet rs = Statement.of("SELECT 1").executeQuery(client.singleUse())) {
      rs.next();
      fail("Expected exception");
    } catch (SessionNotFoundException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testSingleUseBound() {
    // This should trigger an exception with code NOT_FOUND and the text 'Session not found'.
    client.invalidateNextSession();
    for (int run = 0; run < RUNS_PER_TEST_CASE; run++) {
      try (ResultSet rs =
          Statement.of("SELECT 1")
              .executeQuery(
                  client.singleUse(TimestampBound.ofExactStaleness(10L, TimeUnit.SECONDS)))) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(0)).isEqualTo(1L);
        assertThat(rs.next()).isFalse();
      }
    }
  }

  @Test
  public void testSingleUseReadOnlyTransaction() {
    client.invalidateNextSession();
    for (int run = 0; run < RUNS_PER_TEST_CASE; run++) {
      try (ReadOnlyTransaction txn = client.singleUseReadOnlyTransaction()) {
        try (ResultSet rs = txn.executeQuery(Statement.of("SELECT 1"))) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getLong(0)).isEqualTo(1L);
          assertThat(rs.next()).isFalse();
        }
        assertThat(txn.getReadTimestamp()).isNotNull();
      }
    }
  }

  @Test
  public void testSingleUseReadOnlyTransactionBound() {
    client.invalidateNextSession();
    for (int run = 0; run < RUNS_PER_TEST_CASE; run++) {
      try (ReadOnlyTransaction txn =
          client.singleUseReadOnlyTransaction(
              TimestampBound.ofMaxStaleness(10L, TimeUnit.SECONDS))) {
        try (ResultSet rs = txn.executeQuery(Statement.of("SELECT 1"))) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getLong(0)).isEqualTo(1L);
          assertThat(rs.next()).isFalse();
        }
        assertThat(txn.getReadTimestamp()).isNotNull();
      }
    }
  }

  @Test
  public void testReadOnlyTransaction() {
    client.invalidateNextSession();
    for (int run = 0; run < RUNS_PER_TEST_CASE; run++) {
      try (ReadOnlyTransaction txn = client.readOnlyTransaction()) {
        for (int i = 0; i < 2; i++) {
          try (ResultSet rs = txn.executeQuery(Statement.of("SELECT 1"))) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(1L);
            assertThat(rs.next()).isFalse();
          }
        }
        assertThat(txn.getReadTimestamp()).isNotNull();
      }
    }
  }

  @Test
  public void testReadOnlyTransactionNoRecreation() {
    client.setAllowSessionReplacing(false);
    client.invalidateNextSession();
    try (ReadOnlyTransaction txn = client.readOnlyTransaction()) {
      try (ResultSet rs = txn.executeQuery(Statement.of("SELECT 1"))) {
        rs.next();
        fail("Expected exception");
      }
      fail("Expected exception");
    } catch (SessionNotFoundException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testReadOnlyTransactionBound() {
    client.invalidateNextSession();
    for (int run = 0; run < RUNS_PER_TEST_CASE; run++) {
      try (ReadOnlyTransaction txn =
          client.readOnlyTransaction(TimestampBound.ofExactStaleness(10L, TimeUnit.SECONDS))) {
        for (int i = 0; i < 2; i++) {
          try (ResultSet rs = txn.executeQuery(Statement.of("SELECT 1"))) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(1L);
            assertThat(rs.next()).isFalse();
          }
        }
        assertThat(txn.getReadTimestamp()).isNotNull();
      }
    }
  }

  @Test
  public void testReadWriteTransaction() {
    client.invalidateNextSession();
    for (int run = 0; run < RUNS_PER_TEST_CASE; run++) {
      TransactionRunner txn = client.readWriteTransaction();
      txn.run(
          transaction -> {
            for (int i = 0; i < 2; i++) {
              try (ResultSet rs = transaction.executeQuery(Statement.of("SELECT 1"))) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(0)).isEqualTo(1L);
                assertThat(rs.next()).isFalse();
              }
            }
            return null;
          });
    }
  }

  @Test
  public void testReadWriteTransactionNoRecreation() {
    client.setAllowSessionReplacing(false);
    client.invalidateNextSession();
    try {
      TransactionRunner txn = client.readWriteTransaction();
      txn.run(
          transaction -> {
            try (ResultSet rs = transaction.executeQuery(Statement.of("SELECT 1"))) {
              rs.next();
              fail("Expected exception");
            }
            return null;
          });
      fail("Expected exception");
    } catch (SessionNotFoundException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testTransactionManager() throws InterruptedException {
    client.invalidateNextSession();
    for (int run = 0; run < 2; run++) {
      try (TransactionManager manager = client.transactionManager()) {
        TransactionContext txn = manager.begin();
        try {
          while (true) {
            for (int i = 0; i < 2; i++) {
              try (ResultSet rs = txn.executeQuery(Statement.of("SELECT 1"))) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(0)).isEqualTo(1L);
                assertThat(rs.next()).isFalse();
              }
            }
            manager.commit();
            break;
          }
        } catch (AbortedException e) {
          long retryDelayInMillis = e.getRetryDelayInMillis();
          if (retryDelayInMillis > 0) {
            Thread.sleep(retryDelayInMillis);
          }
          txn = manager.resetForRetry();
        }
      }
    }
  }

  @Test
  public void testTransactionManagerNoRecreation() {
    client.setAllowSessionReplacing(false);
    client.invalidateNextSession();
    try (TransactionManager manager = client.transactionManager()) {
      TransactionContext txn = manager.begin();
      while (true) {
        try (ResultSet rs = txn.executeQuery(Statement.of("SELECT 1"))) {
          rs.next();
          fail("Expected exception");
        }
      }
    } catch (SessionNotFoundException ex) {
      assertNotNull(ex.getMessage());
    }
  }
}
