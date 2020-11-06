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

package com.google.cloud.spanner.connection.it;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITCommitResponseTest extends ITAbstractSpannerTest {
  @Override
  public void appendConnectionUri(StringBuilder uri) {
    uri.append(";autocommit=false");
  }

  @Override
  public boolean doCreateDefaultTestTable() {
    return true;
  }

  @Before
  public void clearTestData() {
    try (ITConnection connection = createConnection()) {
      connection.bufferedWrite(Mutation.delete("TEST", KeySet.all()));
      connection.commit();
    }
  }

  @Test
  public void testDefaultNoCommitStats() {
    try (ITConnection connection = createConnection()) {
      connection.bufferedWrite(
          Mutation.newInsertBuilder("TEST").set("ID").to(1L).set("NAME").to("TEST").build());
      connection.commit();
      assertThat(connection.getCommitResponse()).isNotNull();
      assertThat(connection.getCommitResponse().getCommitTimestamp()).isNotNull();
      assertThat(connection.getCommitResponse().hasCommitStats()).isFalse();
    }
  }

  @Test
  public void testReturnCommitStats() {
    try (ITConnection connection = createConnection()) {
      connection.setReturnCommitStats(true);
      connection.bufferedWrite(
          Mutation.newInsertBuilder("TEST").set("ID").to(1L).set("NAME").to("TEST").build());
      connection.commit();
      assertThat(connection.getCommitResponse()).isNotNull();
      assertThat(connection.getCommitResponse().getCommitTimestamp()).isNotNull();
      assertThat(connection.getCommitResponse().hasCommitStats()).isTrue();
      assertThat(connection.getCommitResponse().getCommitStats().getMutationCount()).isEqualTo(2L);
    }
  }

  @Test
  public void testReturnCommitStatsUsingSql() {
    try (ITConnection connection = createConnection()) {
      connection.execute(Statement.of("SET RETURN_COMMIT_STATS=TRUE"));
      connection.bufferedWrite(
          Mutation.newInsertBuilder("TEST").set("ID").to(1L).set("NAME").to("TEST").build());
      connection.commit();
      assertThat(connection.getCommitResponse()).isNotNull();
      assertThat(connection.getCommitResponse().getCommitTimestamp()).isNotNull();
      assertThat(connection.getCommitResponse().hasCommitStats()).isTrue();
      assertThat(connection.getCommitResponse().getCommitStats().getMutationCount()).isEqualTo(2L);
      try (ResultSet rs =
          connection.execute(Statement.of("SHOW VARIABLE COMMIT_RESPONSE")).getResultSet()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getTimestamp("COMMIT_TIMESTAMP")).isNotNull();
        assertThat(rs.getLong("MUTATION_COUNT")).isEqualTo(2L);
        assertThat(rs.getString("OVERLOAD_DELAY")).isNotNull();
        assertThat(rs.next()).isFalse();
      }
    }
  }

  @Test
  public void testAutocommitDefaultNoCommitStats() {
    try (ITConnection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.write(
          Mutation.newInsertBuilder("TEST").set("ID").to(1L).set("NAME").to("TEST").build());
      assertThat(connection.getCommitResponse()).isNotNull();
      assertThat(connection.getCommitResponse().getCommitTimestamp()).isNotNull();
      assertThat(connection.getCommitResponse().hasCommitStats()).isFalse();
    }
  }

  @Test
  public void testAutocommitReturnCommitStats() {
    try (ITConnection connection = createConnection()) {
      connection.setAutocommit(true);
      connection.setReturnCommitStats(true);
      connection.write(
          Mutation.newInsertBuilder("TEST").set("ID").to(1L).set("NAME").to("TEST").build());
      assertThat(connection.getCommitResponse()).isNotNull();
      assertThat(connection.getCommitResponse().getCommitTimestamp()).isNotNull();
      assertThat(connection.getCommitResponse().hasCommitStats()).isTrue();
      assertThat(connection.getCommitResponse().getCommitStats().getMutationCount()).isEqualTo(2L);
    }
  }

  @Test
  public void testAutocommitReturnCommitStatsUsingSql() {
    try (ITConnection connection = createConnection()) {
      connection.execute(Statement.of("SET AUTOCOMMIT=TRUE"));
      connection.execute(Statement.of("SET RETURN_COMMIT_STATS=TRUE"));
      connection.write(
          Mutation.newInsertBuilder("TEST").set("ID").to(1L).set("NAME").to("TEST").build());
      assertThat(connection.getCommitResponse()).isNotNull();
      assertThat(connection.getCommitResponse().getCommitTimestamp()).isNotNull();
      assertThat(connection.getCommitResponse().hasCommitStats()).isTrue();
      assertThat(connection.getCommitResponse().getCommitStats().getMutationCount()).isEqualTo(2L);
      try (ResultSet rs =
          connection.execute(Statement.of("SHOW VARIABLE COMMIT_RESPONSE")).getResultSet()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getTimestamp("COMMIT_TIMESTAMP")).isNotNull();
        assertThat(rs.getLong("MUTATION_COUNT")).isEqualTo(2L);
        assertThat(rs.getString("OVERLOAD_DELAY")).isNotNull();
        assertThat(rs.next()).isFalse();
      }
    }
  }
}
