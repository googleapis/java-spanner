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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.AbortInterceptor;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest.ITConnection;
import com.google.cloud.spanner.connection.it.ITTransactionRetryTest.CountTransactionRetryListener;
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
}
