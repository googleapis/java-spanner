/*
 * Copyright 2025 Google LLC
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

package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITTransactionRetryTest {

  @ClassRule
  public static IntegrationTestEnv env = new IntegrationTestEnv();

  @Test
  public void TestRetryInfo() {
    // Creating a database with the table which contains INT64 columns
    Database db = env.getTestHelper().createTestDatabase("CREATE TABLE Test(ID INT64, "
        + "EMPID INT64) PRIMARY KEY (ID)");
    DatabaseClient databaseClient = env.getTestHelper().getClient().getDatabaseClient(db.getId());

    // Inserting one row
    databaseClient.readWriteTransaction().run(transaction -> {
      transaction.buffer(Mutation.newInsertBuilder("Test")
              .set("ID")
              .to(1)
              .set("EMPID")
              .to(1)
          .build());
      return null;
    });

    try(TransactionManager transactionManager1 = databaseClient.transactionManager()) {
      try(TransactionManager transactionManager2 = databaseClient.transactionManager()) {
        TransactionContext transaction1 = transactionManager1.begin();
        TransactionContext transaction2 = transactionManager2.begin();
        transaction1.executeUpdate(Statement.of("UPDATE Test SET EMPID = EMPID + 1 WHERE ID = 1"));
        transaction2.executeUpdate(Statement.of("UPDATE Test SET EMPID = EMPID + 1 WHERE ID = 1"));
        transactionManager1.commit();
        AbortedException abortedException = Assert.assertThrows(AbortedException.class,
            transactionManager2::commit);
        assertThat(abortedException.getErrorCode()).isEqualTo(ErrorCode.ABORTED);
        assertTrue(abortedException.getRetryDelayInMillis() > 0);
      }
    }
  }
}
