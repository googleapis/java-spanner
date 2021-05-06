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

package com.google.cloud.spanner.connection;

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.AbortInterceptor;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.ITConnection;
import com.google.cloud.spanner.connection.it.ITTransactionRetryTest.CountTransactionRetryListener;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AbortedTest extends AbstractMockServerTest {

  @Test
  public void testCommitAborted() {
    // Do two iterations to ensure that each iteration gets its own transaction, and that each
    // transaction is the most recent transaction of that session.
    for (int i = 0; i < 2; i++) {
      mockSpanner.putStatementResult(
          StatementResult.query(SELECT_COUNT_STATEMENT, SELECT_COUNT_RESULTSET_BEFORE_INSERT));
      mockSpanner.putStatementResult(StatementResult.update(INSERT_STATEMENT, UPDATE_COUNT));
      AbortInterceptor interceptor = new AbortInterceptor(0);
      try (ITConnection connection =
          createConnection(interceptor, new CountTransactionRetryListener())) {
        // verify that the there is no test record
        try (ResultSet rs =
            connection.executeQuery(Statement.of("SELECT COUNT(*) AS C FROM TEST WHERE ID=1"))) {
          assertThat(rs.next(), is(true));
          assertThat(rs.getLong("C"), is(equalTo(0L)));
          assertThat(rs.next(), is(false));
        }
        // do an insert
        connection.executeUpdate(
            Statement.of("INSERT INTO TEST (ID, NAME) VALUES (1, 'test aborted')"));
        // indicate that the next statement should abort
        interceptor.setProbability(1.0);
        interceptor.setOnlyInjectOnce(true);
        // do a commit that will first abort, and then on retry will succeed
        connection.commit();
        mockSpanner.putStatementResult(
            StatementResult.query(SELECT_COUNT_STATEMENT, SELECT_COUNT_RESULTSET_AFTER_INSERT));
        // verify that the insert succeeded
        try (ResultSet rs =
            connection.executeQuery(Statement.of("SELECT COUNT(*) AS C FROM TEST WHERE ID=1"))) {
          assertThat(rs.next(), is(true));
          assertThat(rs.getLong("C"), is(equalTo(1L)));
          assertThat(rs.next(), is(false));
        }
      }
    }
  }

  @Test
  public void testAbortedDuringRetryOfFailedQuery() {
    final Statement invalidStatement = Statement.of("SELECT * FROM FOO");
    StatusRuntimeException notFound =
        Status.NOT_FOUND.withDescription("Table not found").asRuntimeException();
    mockSpanner.putStatementResult(StatementResult.exception(invalidStatement, notFound));
    try (ITConnection connection =
        createConnection(createAbortFirstRetryListener(invalidStatement, notFound))) {
      connection.execute(INSERT_STATEMENT);
      try (ResultSet rs = connection.executeQuery(invalidStatement)) {
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      }
      // Force an abort and retry.
      mockSpanner.abortNextStatement();
      connection.commit();
    }
    assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(2);
    // The transaction will be executed 3 times, which means that there will be 6
    // ExecuteSqlRequests:
    // 1. The initial attempt.
    // 2. The first retry attempt. This will fail on the invalid statement as it is aborted.
    // 3. the second retry attempt. This will succeed.
    assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(6);
  }

  @Test
  public void testAbortedDuringRetryOfFailedUpdate() {
    final Statement invalidStatement = Statement.of("INSERT INTO FOO");
    StatusRuntimeException notFound =
        Status.NOT_FOUND.withDescription("Table not found").asRuntimeException();
    mockSpanner.putStatementResult(StatementResult.exception(invalidStatement, notFound));
    try (ITConnection connection =
        createConnection(createAbortFirstRetryListener(invalidStatement, notFound))) {
      connection.execute(INSERT_STATEMENT);
      try {
        connection.execute(invalidStatement);
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      }
      // Force an abort and retry.
      mockSpanner.abortNextStatement();
      connection.commit();
    }
    assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(2);
    // The transaction will be executed 3 times, which means that there will be 6
    // ExecuteSqlRequests:
    // 1. The initial attempt.
    // 2. The first retry attempt. This will fail on the invalid statement as it is aborted.
    // 3. the second retry attempt. This will succeed.
    assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(6);
  }

  @Test
  public void testAbortedDuringRetryOfFailedBatchUpdate() {
    final Statement invalidStatement = Statement.of("INSERT INTO FOO");
    StatusRuntimeException notFound =
        Status.NOT_FOUND.withDescription("Table not found").asRuntimeException();
    mockSpanner.putStatementResult(StatementResult.exception(invalidStatement, notFound));
    try (ITConnection connection =
        createConnection(createAbortFirstRetryListener(invalidStatement, notFound))) {
      connection.execute(INSERT_STATEMENT);
      try {
        connection.executeBatchUpdate(Collections.singletonList(invalidStatement));
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      }
      // Force an abort and retry.
      mockSpanner.abortNextStatement();
      connection.commit();
    }
    assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(2);
    assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(3);
  }

  @Test
  public void testAbortedDuringRetryOfFailedQueryAsFirstStatement() {
    final Statement invalidStatement = Statement.of("SELECT * FROM FOO");
    StatusRuntimeException notFound =
        Status.NOT_FOUND.withDescription("Table not found").asRuntimeException();
    mockSpanner.putStatementResult(StatementResult.exception(invalidStatement, notFound));
    // Abort the invalid statement on the third retry (listener counts from 0). The first retry will
    // be triggered by the client library because the first statement of the transaction failed.
    // That means that it also failed to return a transaction, and the first retry is only executed
    // in order to execute an explicit BeginTransaction RPC:

    // 1: First statement fails => Retry because no transaction was returned
    // 2: BeginTransaction + Invalid statement + Insert + Commit (aborted) => Retry
    // 3: First statement fails => Retry because no transaction was returned
    // 4: BeginTransaction + Invalid statement (aborted) => Retry
    // 5: First statement fails => Retry because no transaction was returned
    // 6: BeginTransaction + Invalid statement + Insert + Commit => Success

    try (ITConnection connection =
        createConnection(createAbortRetryListener(2, invalidStatement, notFound))) {
      try (ResultSet rs = connection.executeQuery(invalidStatement)) {
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      }
      connection.executeUpdate(INSERT_STATEMENT);
      // Force an abort and retry.
      mockSpanner.abortNextStatement();
      connection.commit();
    }
    assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(2);
    // 6 times invalid query + 2 times INSERT.
    assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(8);
  }

  @Test
  public void testAbortedDuringRetryOfFailedUpdateAsFirstStatement() {
    final Statement invalidStatement = Statement.of("INSERT INTO FOO");
    StatusRuntimeException notFound =
        Status.NOT_FOUND.withDescription("Table not found").asRuntimeException();
    mockSpanner.putStatementResult(StatementResult.exception(invalidStatement, notFound));
    try (ITConnection connection =
        createConnection(createAbortRetryListener(2, invalidStatement, notFound))) {
      try {
        connection.execute(invalidStatement);
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      }
      connection.execute(INSERT_STATEMENT);
      // Force an abort and retry.
      mockSpanner.abortNextStatement();
      connection.commit();
    }
    assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(2);
    assertThat(mockSpanner.countRequestsOfType(ExecuteSqlRequest.class)).isEqualTo(8);
  }

  @Test
  public void testAbortedDuringRetryOfFailedBatchUpdateAsFirstStatement() {
    final Statement invalidStatement = Statement.of("INSERT INTO FOO");
    StatusRuntimeException notFound =
        Status.NOT_FOUND.withDescription("Table not found").asRuntimeException();
    mockSpanner.putStatementResult(StatementResult.exception(invalidStatement, notFound));
    try (ITConnection connection =
        createConnection(createAbortFirstRetryListener(invalidStatement, notFound))) {
      try {
        connection.executeBatchUpdate(Collections.singletonList(invalidStatement));
        fail("missing expected exception");
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
      }
      connection.execute(INSERT_STATEMENT);
      // Force an abort and retry.
      mockSpanner.abortNextStatement();
      connection.commit();
    }
    assertThat(mockSpanner.countRequestsOfType(CommitRequest.class)).isEqualTo(2);
    assertThat(mockSpanner.countRequestsOfType(ExecuteBatchDmlRequest.class)).isEqualTo(6);
  }

  ITConnection createConnection(TransactionRetryListener listener) {
    ITConnection connection =
        super.createConnection(ImmutableList.of(), ImmutableList.of(listener));
    connection.setAutocommit(false);
    return connection;
  }

  /** Creates a retry listener that will abort the first retry as well. */
  TransactionRetryListener createAbortFirstRetryListener(
      final Statement invalidStatement, final StatusRuntimeException statementException) {
    return createAbortRetryListener(0, invalidStatement, statementException);
  }

  /** Creates a retry listener that will abort the n'th retry. */
  TransactionRetryListener createAbortRetryListener(
      final int onAttempt,
      final Statement invalidStatement,
      final StatusRuntimeException statementException) {
    return new TransactionRetryListener() {
      @Override
      public void retryStarting(
          Timestamp transactionStarted, long transactionId, int retryAttempt) {
        if (retryAttempt == onAttempt) {
          mockSpanner.putStatementResult(
              StatementResult.exception(
                  invalidStatement,
                  mockSpanner.createAbortedException(ByteString.copyFromUtf8("some-transaction"))));
        } else {
          mockSpanner.putStatementResult(
              StatementResult.exception(invalidStatement, statementException));
        }
      }

      @Override
      public void retryFinished(
          Timestamp transactionStarted, long transactionId, int retryAttempt, RetryResult result) {}
    };
  }
}
