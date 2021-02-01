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

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.AbstractMessage;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ConnectionTest {

  public static class DefaultConnectionOptionsTest extends AbstractMockServerTest {
    @Test
    public void testDefaultOptimizerVersion() {
      try (Connection connection = createConnection()) {
        try (ResultSet rs =
            connection.executeQuery(Statement.of("SHOW VARIABLE OPTIMIZER_VERSION"))) {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getString("OPTIMIZER_VERSION")).isEqualTo("");
          assertThat(rs.next()).isFalse();
        }
      }
    }

    @Test
    public void testUseOptimizerVersionFromEnvironment() {
      try {
        SpannerOptions.useEnvironment(
            new SpannerOptions.SpannerEnvironment() {
              @Override
              public String getOptimizerVersion() {
                return "20";
              }
            });
        try (Connection connection = createConnection()) {
          // Do a query and verify that the version from the environment is used.
          try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(COUNT_BEFORE_INSERT);
            assertThat(rs.next()).isFalse();
            // Verify query options from the environment.
            ExecuteSqlRequest request = getLastExecuteSqlRequest();
            assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("20");
          }
          // Now set one of the query options on the connection. That option should be used in
          // combination with the other option from the environment.
          connection.execute(Statement.of("SET OPTIMIZER_VERSION='30'"));
          try (ResultSet rs = connection.executeQuery(SELECT_COUNT_STATEMENT)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(COUNT_BEFORE_INSERT);
            assertThat(rs.next()).isFalse();

            ExecuteSqlRequest request = getLastExecuteSqlRequest();
            // Optimizer version should come from the connection.
            assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("30");
          }
          // Now specify options directly for the query. These should override both the environment
          // and what is set on the connection.
          try (ResultSet rs =
              connection.executeQuery(
                  Statement.newBuilder(SELECT_COUNT_STATEMENT.getSql())
                      .withQueryOptions(
                          QueryOptions.newBuilder()
                              .setOptimizerVersion("user-defined-version")
                              .build())
                      .build())) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(0)).isEqualTo(COUNT_BEFORE_INSERT);
            assertThat(rs.next()).isFalse();

            ExecuteSqlRequest request = getLastExecuteSqlRequest();
            // Optimizer version should come from the query.
            assertThat(request.getQueryOptions().getOptimizerVersion())
                .isEqualTo("user-defined-version");
          }
        }
      } finally {
        SpannerOptions.useDefaultEnvironment();
      }
    }

    @Test
    public void testExecuteInvalidBatchUpdate() {
      try (Connection connection = createConnection()) {
        try {
          connection.executeBatchUpdate(
              ImmutableList.of(INSERT_STATEMENT, SELECT_RANDOM_STATEMENT));
          fail("Missing expected exception");
        } catch (SpannerException e) {
          assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
        }
      }
    }

    @Test
    public void testQueryAborted() {
      try (Connection connection = createConnection()) {
        connection.setRetryAbortsInternally(false);
        for (boolean abort : new Boolean[] {true, false}) {
          try {
            if (abort) {
              mockSpanner.abortNextStatement();
            }
            connection.executeQuery(SELECT_RANDOM_STATEMENT);
            assertThat(abort).isFalse();
            connection.commit();
          } catch (AbortedException e) {
            assertThat(abort).isTrue();
            connection.rollback();
          }
        }
      }
    }

    @Test
    public void testUpdateAborted() {
      try (Connection connection = createConnection()) {
        connection.setRetryAbortsInternally(false);
        for (boolean abort : new Boolean[] {true, false}) {
          try {
            if (abort) {
              mockSpanner.abortNextStatement();
            }
            connection.executeUpdate(INSERT_STATEMENT);
            assertThat(abort).isFalse();
            connection.commit();
          } catch (AbortedException e) {
            assertThat(abort).isTrue();
            connection.rollback();
          }
        }
      }
    }

    @Test
    public void testBatchUpdateAborted() {
      try (Connection connection = createConnection()) {
        connection.setRetryAbortsInternally(false);
        for (boolean abort : new Boolean[] {true, false}) {
          try {
            if (abort) {
              mockSpanner.abortNextStatement();
            }
            connection.executeBatchUpdate(ImmutableList.of(INSERT_STATEMENT, INSERT_STATEMENT));
            assertThat(abort).isFalse();
            connection.commit();
          } catch (AbortedException e) {
            assertThat(abort).isTrue();
            connection.rollback();
          }
        }
      }
    }
  }

  public static class ConnectionMinSessionsTest extends AbstractMockServerTest {

    @AfterClass
    public static void reset() {
      mockSpanner.reset();
    }

    protected String getBaseUrl() {
      return super.getBaseUrl() + ";minSessions=1";
    }

    @Test
    public void testMinSessions() throws InterruptedException, TimeoutException {
      try (Connection connection = createConnection()) {
        mockSpanner.waitForRequestsToContain(
            new Predicate<AbstractMessage>() {
              @Override
              public boolean apply(AbstractMessage input) {
                return input instanceof BatchCreateSessionsRequest
                    && ((BatchCreateSessionsRequest) input).getSessionCount() == 1;
              }
            },
            5000L);
      }
    }
  }

  public static class ConnectionMaxSessionsTest extends AbstractMockServerTest {

    @AfterClass
    public static void reset() {
      mockSpanner.reset();
    }

    protected String getBaseUrl() {
      return super.getBaseUrl() + ";maxSessions=1";
    }

    @Test
    public void testMaxSessions()
        throws InterruptedException, TimeoutException, ExecutionException {
      try (Connection connection1 = createConnection();
          Connection connection2 = createConnection()) {
        connection1.beginTransactionAsync();
        connection2.beginTransactionAsync();

        ApiFuture<Long> count1 = connection1.executeUpdateAsync(INSERT_STATEMENT);
        ApiFuture<Long> count2 = connection2.executeUpdateAsync(INSERT_STATEMENT);

        // Commit the transactions. Both should be able to finish, but both used the same session.
        ApiFuture<Void> commit1 = connection1.commitAsync();
        ApiFuture<Void> commit2 = connection2.commitAsync();

        // At least one transaction must wait until the other has finished before it can get a
        // session.
        assertThat(count1.isDone() && count2.isDone()).isFalse();
        assertThat(commit1.isDone() && commit2.isDone()).isFalse();

        // Wait until both finishes.
        ApiFutures.allAsList(Arrays.asList(commit1, commit2)).get(5L, TimeUnit.SECONDS);

        assertThat(count1.isDone()).isTrue();
        assertThat(count2.isDone()).isTrue();
      }
      assertThat(mockSpanner.numSessionsCreated()).isEqualTo(1);
    }
  }
}
