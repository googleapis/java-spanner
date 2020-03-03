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

package com.google.cloud.spanner.it;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTest;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(IntegrationTest.class)
@RunWith(JUnit4.class)
public class ITQueryOptionsTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static Database db;
  @Rule public ExpectedException expectedException = ExpectedException.none();
  private static DatabaseClient client;

  @BeforeClass
  public static void setUpDatabase() {
    // Empty database.
    db = env.getTestHelper().createTestDatabase();
    client = env.getTestHelper().getDatabaseClient(db);
  }

  @Test
  public void executeQuery() {
    // Version '1' should work.
    try (ResultSet rs =
        client
            .singleUse()
            .executeQuery(
                Statement.of("SELECT 1"),
                Options.queryOptions(QueryOptions.newBuilder().setOptimizerVersion("1").build()))) {
      while (rs.next()) {
        assertThat(rs.getLong(0)).isEqualTo(1L);
      }
    }
    // Version 'latest' should also work.
    try (ResultSet rs =
        client
            .singleUse()
            .executeQuery(
                Statement.of("SELECT 1"),
                Options.queryOptions(
                    QueryOptions.newBuilder().setOptimizerVersion("latest").build()))) {
      while (rs.next()) {
        assertThat(rs.getLong(0)).isEqualTo(1L);
      }
    }
    // Version '100000' should not work.
    try (ResultSet rs =
        client
            .singleUse()
            .executeQuery(
                Statement.of("SELECT 1"),
                Options.queryOptions(
                    QueryOptions.newBuilder().setOptimizerVersion("100000").build()))) {
      while (rs.next()) {
        fail("should not get any results");
      }
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(e.getMessage()).contains("Query optimizer version: 100000 is not supported");
    }
  }

  @Test
  public void spannerOptions() {
    // Version '1' should work.
    try (Spanner spanner =
        env.getTestHelper()
            .getOptions()
            .toBuilder()
            .setDefaultQueryOptions(
                db.getId(), QueryOptions.newBuilder().setOptimizerVersion("1").build())
            .build()
            .getService()) {
      DatabaseClient client = spanner.getDatabaseClient(db.getId());
      try (ResultSet rs = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
        while (rs.next()) {
          assertThat(rs.getLong(0)).isEqualTo(1L);
        }
      }
    }
    // Version 'latest' should also work.
    try (Spanner spanner =
        env.getTestHelper()
            .getOptions()
            .toBuilder()
            .setDefaultQueryOptions(
                db.getId(), QueryOptions.newBuilder().setOptimizerVersion("latest").build())
            .build()
            .getService()) {
      DatabaseClient client = spanner.getDatabaseClient(db.getId());
      try (ResultSet rs = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
        while (rs.next()) {
          assertThat(rs.getLong(0)).isEqualTo(1L);
        }
      }
    }
    // Version '100000' should not work.
    try (Spanner spanner =
        env.getTestHelper()
            .getOptions()
            .toBuilder()
            .setDefaultQueryOptions(
                db.getId(), QueryOptions.newBuilder().setOptimizerVersion("100000").build())
            .build()
            .getService()) {
      DatabaseClient client = spanner.getDatabaseClient(db.getId());
      try (ResultSet rs = client.singleUse().executeQuery(Statement.of("SELECT 1"))) {
        while (rs.next()) {
          fail("should not get any results");
        }
      } catch (SpannerException e) {
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
        assertThat(e.getMessage()).contains("Query optimizer version: 100000 is not supported");
      }
    }
  }
}
