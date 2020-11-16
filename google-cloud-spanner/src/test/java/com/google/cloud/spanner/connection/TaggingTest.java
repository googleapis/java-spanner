/*
 * Copyright 2020 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import java.util.Arrays;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TaggingTest extends AbstractMockServerTest {

  @After
  public void clearRequests() {
    mockSpanner.clearRequests();
  }

  @Test
  public void testStatementTagNotAllowedForCommit() {
    try (Connection connection = createConnection()) {
      connection.setStatementTag("tag-1");
      try {
        connection.commit();
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAILED_PRECONDITION);
      }
    }
  }

  @Test
  public void testStatementTagNotAllowedForRollback() {
    try (Connection connection = createConnection()) {
      connection.setStatementTag("tag-1");
      try {
        connection.rollback();
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAILED_PRECONDITION);
      }
    }
  }

  @Test
  public void testStatementTagNotAllowedInsideBatch() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        connection.startBatchDml();
        try {
          connection.setStatementTag("tag-1");
          fail("missing expected exception");
        } catch (SpannerException e) {
          assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAILED_PRECONDITION);
        }
        connection.abortBatch();
      }
    }
  }

  @Test
  public void testQuery_NoTags() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {}
        assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  public void testUpdate_NoTags() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        connection.executeUpdate(INSERT_STATEMENT);

        assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  public void testPartitionedUpdate_NoTags() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setAutocommitDmlMode(AutocommitDmlMode.PARTITIONED_NON_ATOMIC);
      connection.executeUpdate(INSERT_STATEMENT);

      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testBatchUpdate_NoTags() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        connection.executeBatchUpdate(Arrays.asList(INSERT_STATEMENT));

        assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  public void testQuery_StatementTag() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        connection.setStatementTag("tag-1");
        try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {}
        assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("tag-1");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();

        // The tag should automatically be cleared after a statement.
        try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {}
        assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  public void testUpdate_StatementTag() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        connection.setStatementTag("tag-2");
        connection.executeUpdate(INSERT_STATEMENT);

        assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("tag-2");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();

        connection.executeUpdate(INSERT_STATEMENT);

        assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteSqlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  public void testPartitionedUpdate_StatementTag() {
    try (Connection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setAutocommitDmlMode(AutocommitDmlMode.PARTITIONED_NON_ATOMIC);
      connection.setStatementTag("tag-4");
      connection.executeUpdate(INSERT_STATEMENT);

      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("tag-4");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");

      mockSpanner.clearRequests();

      connection.executeUpdate(INSERT_STATEMENT);

      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testBatchUpdate_StatementTag() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);
        connection.setStatementTag("tag-3");
        connection.executeBatchUpdate(Arrays.asList(INSERT_STATEMENT));

        assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("tag-3");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();

        connection.executeBatchUpdate(Arrays.asList(INSERT_STATEMENT));

        assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  @Ignore(
      "currently skipped as transaction tags for read operations are not yet implemented in the client library")
  public void testQuery_TransactionTag() {
    try (Connection connection = createConnection()) {
      connection.setTransactionTag("tag-1");
      try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {}
      connection.commit();

      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("tag-1");
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("tag-1");

      mockSpanner.clearRequests();

      // The tag should automatically be cleared after a statement.
      try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {}
      connection.commit();

      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testUpdate_TransactionTag() {
    try (Connection connection = createConnection()) {
      connection.setTransactionTag("tag-2");
      connection.executeUpdate(INSERT_STATEMENT);
      connection.commit();

      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("tag-2");
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("tag-2");

      mockSpanner.clearRequests();

      connection.executeUpdate(INSERT_STATEMENT);
      connection.commit();

      assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteSqlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testBatchUpdate_TransactionTag() {
    try (Connection connection = createConnection()) {
      connection.setTransactionTag("tag-3");
      connection.executeBatchUpdate(Arrays.asList(INSERT_STATEMENT));
      connection.commit();

      assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteBatchDmlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteBatchDmlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("tag-3");
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("tag-3");

      mockSpanner.clearRequests();

      connection.executeBatchUpdate(Arrays.asList(INSERT_STATEMENT));
      connection.commit();

      assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteBatchDmlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteBatchDmlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("");

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testDmlBatch_StatementTag() {
    try (Connection connection = createConnection()) {
      for (boolean autocommit : new boolean[] {true, false}) {
        connection.setAutocommit(autocommit);

        connection.setStatementTag("batch-tag");
        connection.startBatchDml();
        connection.execute(INSERT_STATEMENT);
        connection.execute(INSERT_STATEMENT);
        connection.runBatch();

        assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(1);
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getRequestTag())
            .isEqualTo("batch-tag");
        assertThat(
                mockSpanner
                    .getRequestsOfType(ExecuteBatchDmlRequest.class)
                    .get(0)
                    .getRequestOptions()
                    .getTransactionTag())
            .isEqualTo("");

        mockSpanner.clearRequests();
      }
    }
  }

  @Test
  public void testRunBatch_TransactionTag() {
    try (Connection connection = createConnection()) {
      connection.setTransactionTag("batch-tag");
      connection.startBatchDml();
      connection.execute(INSERT_STATEMENT);
      connection.execute(INSERT_STATEMENT);
      connection.runBatch();
      connection.commit();

      assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteBatchDmlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(ExecuteBatchDmlRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("batch-tag");
      assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(1);
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getRequestTag())
          .isEqualTo("");
      assertThat(
              mockSpanner
                  .getRequestsOfType(CommitRequest.class)
                  .get(0)
                  .getRequestOptions()
                  .getTransactionTag())
          .isEqualTo("batch-tag");

      mockSpanner.clearRequests();
    }
  }

  @Test
  public void testShowSetTags() {
    try (Connection connection = createConnection()) {
      connection.execute(Statement.of("SET STATEMENT_TAG='tag1'"));
      try (ResultSet rs =
          connection.execute(Statement.of("SHOW VARIABLE STATEMENT_TAG")).getResultSet()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("STATEMENT_TAG")).isEqualTo("tag1");
        assertThat(rs.next()).isFalse();
      }
      connection.execute(Statement.of("SET STATEMENT_TAG=''"));
      try (ResultSet rs =
          connection.execute(Statement.of("SHOW VARIABLE STATEMENT_TAG")).getResultSet()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("STATEMENT_TAG")).isEmpty();
        assertThat(rs.next()).isFalse();
      }
      connection.execute(Statement.of("SET TRANSACTION_TAG='tag2'"));
      try (ResultSet rs =
          connection.execute(Statement.of("SHOW VARIABLE TRANSACTION_TAG")).getResultSet()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TRANSACTION_TAG")).isEqualTo("tag2");
        assertThat(rs.next()).isFalse();
      }
      connection.execute(Statement.of("SET TRANSACTION_TAG=''"));
      try (ResultSet rs =
          connection.execute(Statement.of("SHOW VARIABLE TRANSACTION_TAG")).getResultSet()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("TRANSACTION_TAG")).isEmpty();
        assertThat(rs.next()).isFalse();
      }
    }
  }
}
