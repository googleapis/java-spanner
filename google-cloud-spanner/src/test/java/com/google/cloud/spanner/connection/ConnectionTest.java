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

import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import org.junit.Test;

public class ConnectionTest extends AbstractMockServerTest {
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
}
