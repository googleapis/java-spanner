/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.spanner.it;

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Category(ParallelIntegrationTest.class)
@RunWith(Parameterized.class)
public class ITForeignKeyDeleteCascadeTest {

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  @Parameterized.Parameters(name = "Dialect = {0}")
  public static List<DialectTestParameter> data() {
    List<DialectTestParameter> params = new ArrayList<>();
    params.add(new DialectTestParameter(Dialect.GOOGLE_STANDARD_SQL));
    params.add(new DialectTestParameter(Dialect.POSTGRESQL));
    return params;
  }

  private static final String TABLE_NAME_SINGER = "Singer";
  private static final String TABLE_NAME_CONCERTS = "Concerts";
  private static final String DELETE_RULE_CASCADE = "CASCADE";
  private static final String DELETE_RULE_DEFAULT = "NO ACTION";
  private static final String DELETE_RULE_COLUMN_NAME = "DELETE_RULE";
  private static final String CREATE_TABLE_SINGER =
      "CREATE TABLE Singer (\n"
          + "  SingerId   INT64 NOT NULL,\n"
          + "  FirstName  STRING(1024),\n"
          + ") PRIMARY KEY(SingerId)\n";

  private static final String POSTGRES_CREATE_TABLE_SINGER =
      "CREATE TABLE Singer (\n"
          + "  singer_id   BIGINT PRIMARY KEY,\n"
          + "  first_name  VARCHAR\n"
          + ")";

  private static final String CREATE_TABLE_CONCERT_WITH_FOREIGN_KEY =
      "CREATE TABLE Concerts (\n"
          + "  VenueId      INT64 NOT NULL,\n"
          + "  SingerId     INT64 NOT NULL,\n"
          + "  CONSTRAINT Fk_Concerts_Singer FOREIGN KEY (SingerId) REFERENCES Singer (SingerId) ON DELETE CASCADE"
          + ") PRIMARY KEY(VenueId, SingerId)";

  private static final String POSTGRES_CREATE_TABLE_CONCERT_WITH_FOREIGN_KEY =
      "CREATE TABLE Concerts (\n"
          + "      venue_id      BIGINT NOT NULL,\n"
          + "      singer_id     BIGINT NOT NULL,\n"
          + "      PRIMARY KEY (venue_id, singer_id),\n"
          + "      CONSTRAINT Fk_Concerts_Singer FOREIGN KEY (singer_id) REFERENCES Singer (singer_id) ON DELETE CASCADE\n"
          + "      )";

  private static final String CREATE_TABLE_CONCERT_V2_WITHOUT_FOREIGN_KEY =
      "CREATE TABLE ConcertsV2 (\n"
          + "  VenueId      INT64 NOT NULL,\n"
          + "  SingerId     INT64 NOT NULL,\n"
          + ") PRIMARY KEY(VenueId, SingerId)";

  private static final String POSTGRES_CREATE_TABLE_CONCERT_V2_WITHOUT_FOREIGN_KEY =
      "CREATE TABLE ConcertsV2 (\n"
          + "      venue_id      BIGINT NOT NULL,\n"
          + "      singer_id     BIGINT NOT NULL,\n"
          + "      PRIMARY KEY (venue_id, singer_id)\n"
          + "      )";

  private static final String ALTER_TABLE_CONCERT_V2_WITH_FOREIGN_KEY =
      "ALTER TABLE ConcertsV2 "
          + "ADD CONSTRAINT Fk_Concerts_Singer_V2 FOREIGN KEY(SingerId) REFERENCES Singer(SingerId) "
          + "ON DELETE CASCADE";

  private static final String POSTGRES_ALTER_TABLE_CONCERT_V2_WITH_FOREIGN_KEY =
      "ALTER TABLE ConcertsV2 "
          + "ADD CONSTRAINT Fk_Concerts_Singer_V2 FOREIGN KEY(singer_id) REFERENCES Singer(singer_id) "
          + "ON DELETE CASCADE";

  private static final String ALTER_TABLE_CONCERT_V2_UPDATE_FOREIGN_KEY_WITHOUT_DELETE_CASCADE =
      "ALTER TABLE ConcertsV2 "
          + "ADD CONSTRAINT Fk_Concerts_Singer_V2 FOREIGN KEY(SingerId) REFERENCES Singer(SingerId) ";

  private static final String
      POSTGRES_ALTER_TABLE_CONCERT_V2_UPDATE_FOREIGN_KEY_WITHOUT_DELETE_CASCADE =
          "ALTER TABLE ConcertsV2 "
              + "ADD CONSTRAINT Fk_Concerts_Singer_V2 FOREIGN KEY(singer_id) REFERENCES Singer(singer_id) ";

  private static final String ALTER_TABLE_CONCERT_V2_DROP_FOREIGN_KEY_CONSTRAINT =
      "ALTER TABLE ConcertsV2\n" + "DROP CONSTRAINT Fk_Concerts_Singer_V2";

  private static Database GOOGLE_STANDARD_SQL_DATABASE;
  private static Database POSTGRESQL_DATABASE;
  private static List<Database> dbs = new ArrayList<>();

  @Parameterized.Parameter(0)
  public DialectTestParameter dialect;

  @BeforeClass
  public static void setUpDatabase() {
    GOOGLE_STANDARD_SQL_DATABASE =
        env.getTestHelper()
            .createTestDatabase(
                ImmutableList.of(CREATE_TABLE_SINGER, CREATE_TABLE_CONCERT_WITH_FOREIGN_KEY));
    if (!isUsingEmulator()) {
      POSTGRESQL_DATABASE =
          env.getTestHelper()
              .createTestDatabase(
                  Dialect.POSTGRESQL,
                  ImmutableList.of(
                      POSTGRES_CREATE_TABLE_SINGER,
                      POSTGRES_CREATE_TABLE_CONCERT_WITH_FOREIGN_KEY));
    }
    dbs.add(GOOGLE_STANDARD_SQL_DATABASE);
    dbs.add(POSTGRESQL_DATABASE);
  }

  @AfterClass
  public static void tearDown() {
    for (Database db : dbs) {
      db.drop();
    }
    dbs.clear();
  }

  @Test
  public void testForeignKeyDeleteCascadeConstraints_withCreateDDLStatements() {
    final DatabaseClient databaseClient = getCreatedDatabaseClient();
    final String referentialConstraintQuery = getReferentialConstraintsQueryStatement();
    try (final ResultSet rs =
        databaseClient.singleUse().executeQuery(Statement.of(referentialConstraintQuery))) {
      while (rs.next()) {
        assertThat(rs.getString(DELETE_RULE_COLUMN_NAME)).isEqualTo(DELETE_RULE_CASCADE);
      }
    }
  }

  @Test
  public void testForeignKeyDeleteCascadeConstraints_withAlterDDLStatements() throws Exception {
    // Creating new tables within this test to ensure we don't pollute tables used by other tests in
    // this class.
    final List<String> createStatements = getCreateAndAlterTableStatementsWithForeignKey();
    final Database createdDatabase =
        env.getTestHelper().createTestDatabase(dialect.dialect, createStatements);
    dbs.add(createdDatabase);

    final DatabaseClient databaseClient = getCreatedDatabaseClient();

    final String referentialConstraintQuery = getReferentialConstraintsQueryStatement();
    try (final ResultSet rs =
        databaseClient.singleUse().executeQuery(Statement.of(referentialConstraintQuery))) {
      while (rs.next()) {
        assertThat(rs.getString(DELETE_RULE_COLUMN_NAME)).isEqualTo(DELETE_RULE_CASCADE);
      }
    }

    // remove the foreign key delete cascade constraint
    final List<String> alterDropStatements = getAlterDropForeignKeyDeleteCascadeStatements();
    getDatabaseAdminClient()
        .updateDatabaseDdl(
            env.getTestHelper().getInstanceId().getInstance(),
            createdDatabase.getId().getDatabase(),
            alterDropStatements,
            null)
        .get();

    try (final ResultSet rs =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of(getReferentialConstraintsQueryStatementV2()))) {
      while (rs.next()) {
        assertThat(rs.getString(DELETE_RULE_COLUMN_NAME)).isEqualTo(DELETE_RULE_DEFAULT);
      }
    }
  }

  @Test
  public void testForeignKeyDeleteCascadeConstraints_verifyValidInsertions() {

    final DatabaseClient databaseClient = getCreatedDatabaseClient();
    final String singerInsertStatement = getInsertStatementForSingerTable();
    final Statement singerInsertStatementWithValues =
        Statement.newBuilder(singerInsertStatement)
            // Use 'p1' to bind to the parameter with index 1 etc.
            .bind("p1")
            .to(1L)
            .bind("p2")
            .to("singerName")
            .build();

    final String concertInsertStatement = getInsertStatementForConcertsTable();
    final Statement concertInsertStatementWithValues =
        Statement.newBuilder(concertInsertStatement)
            // Use 'p1' to bind to the parameter with index 1 etc.
            .bind("p1")
            .to(1L)
            .bind("p2")
            .to(1L)
            .build();

    // successful inserts into referenced and referencing tables
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.batchUpdate(
                  ImmutableList.of(
                      singerInsertStatementWithValues, concertInsertStatementWithValues));
              return null;
            });

    final String singerIdColumnName = getSingerIdColumnName();
    final String singerFirstNameColumnName = getSingerFirstNameColumnName();
    final String concertVenueIdColumnName = getConcertVenueIdColumnName();

    try (ResultSet resultSet =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of("SELECT * FROM " + TABLE_NAME_SINGER))) {

      resultSet.next();
      assertEquals(1, resultSet.getLong(singerIdColumnName));
      assertEquals("singerName", resultSet.getString(singerFirstNameColumnName));

      assertThat(resultSet.next()).isFalse();
    }

    try (ResultSet resultSet =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of("SELECT * FROM " + TABLE_NAME_CONCERTS))) {

      resultSet.next();
      assertEquals(1, resultSet.getLong(singerIdColumnName));
      assertEquals(1, resultSet.getLong(concertVenueIdColumnName));

      assertThat(resultSet.next()).isFalse();
    }
  }

  @Test
  public void testForeignKeyDeleteCascadeConstraints_verifyInvalidInsertions() {
    final DatabaseClient databaseClient = getCreatedDatabaseClient();

    // unsuccessful inserts into referencing tables when foreign key is not inserted into referenced
    // table
    final String concertInsertStatement = getInsertStatementForConcertsTable();
    final Statement concertInsertStatementWithInvalidValues =
        Statement.newBuilder(concertInsertStatement)
            // Use 'p1' to bind to the parameter with index 1 etc.
            .bind("p1")
            .to(2L)
            .bind("p2")
            .to(2L)
            .build();

    SpannerException ex =
        assertThrows(
            SpannerException.class,
            () ->
                databaseClient
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          transaction.executeUpdate(concertInsertStatementWithInvalidValues);
                          return null;
                        }));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FAILED_PRECONDITION);
    assertThat(ex.getMessage()).contains("Cannot find referenced values");
  }

  @Test
  public void testForeignKeyDeleteCascadeConstraints_forDeletions() {

    final DatabaseClient databaseClient = getCreatedDatabaseClient();

    final String singerInsertStatement = getInsertStatementForSingerTable();
    final Statement singerInsertStatementWithValues =
        Statement.newBuilder(singerInsertStatement)
            // Use 'p1' to bind to the parameter with index 1 etc.
            .bind("p1")
            .to(3L)
            .bind("p2")
            .to("singerName")
            .build();

    final String concertInsertStatement = getInsertStatementForConcertsTable();
    final Statement concertInsertStatementWithValues =
        Statement.newBuilder(concertInsertStatement)
            // Use 'p1' to bind to the parameter with index 1 etc.
            .bind("p1")
            .to(3L)
            .bind("p2")
            .to(3L)
            .build();

    // successful inserts into referenced and referencing tables
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.batchUpdate(
                  ImmutableList.of(
                      singerInsertStatementWithValues, concertInsertStatementWithValues));
              return null;
            });

    // execute delete
    final String singerDeleteStatement = getDeleteStatementForSingerTable();
    final Statement singerDeleteStatementWithValues =
        Statement.newBuilder(singerDeleteStatement)
            // Use 'p1' to bind to the parameter with index 1 etc.
            .bind("p1")
            .to(3L)
            .build();
    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(singerDeleteStatementWithValues);
              return null;
            });

    try (ResultSet resultSet =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of("SELECT * FROM " + TABLE_NAME_SINGER))) {
      assertThat(resultSet.next()).isFalse();
    }

    try (ResultSet resultSet =
        databaseClient
            .singleUse()
            .executeQuery(Statement.of("SELECT * FROM " + TABLE_NAME_CONCERTS))) {
      assertThat(resultSet.next()).isFalse();
    }
  }

  @Test
  public void testForeignKeyDeleteCascadeConstraints_forMutations_onConflictDueToParentTable() {
    final DatabaseClient databaseClient = getCreatedDatabaseClient();

    // inserting and deleting the referenced key within the same mutation are considered
    // conflicting operations, thus this results in an exception.
    SpannerException ex =
        assertThrows(
            SpannerException.class,
            () ->
                databaseClient
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          transaction.buffer(
                              Arrays.asList(
                                  Mutation.newInsertBuilder("Singer")
                                      .set(getSingerIdColumnName())
                                      .to(4L)
                                      .set(getSingerFirstNameColumnName())
                                      .to("singerName")
                                      .build(),
                                  Mutation.delete("Singer", Key.of(4L))));
                          return null;
                        }));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FAILED_PRECONDITION);
  }

  @Test
  public void testForeignKeyDeleteCascadeConstraints_forMutations_onConflictsDueToChildTable() {
    final DatabaseClient databaseClient = getCreatedDatabaseClient();

    // referencing a foreign key in child table and deleting the referenced key in parent table
    // within the same mutations are considered conflicting operations.
    final String singerInsertStatement = getInsertStatementForSingerTable();
    final Statement singerInsertStatementWithValues =
        Statement.newBuilder(singerInsertStatement)
            // Use 'p1' to bind to the parameter with index 1 etc.
            .bind("p1")
            .to(5L)
            .bind("p2")
            .to("singerName")
            .build();

    databaseClient
        .readWriteTransaction()
        .run(
            transaction -> {
              transaction.executeUpdate(singerInsertStatementWithValues);
              return null;
            });
    SpannerException ex =
        assertThrows(
            SpannerException.class,
            () ->
                databaseClient
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          transaction.buffer(
                              Arrays.asList(
                                  Mutation.newInsertBuilder("Concerts")
                                      .set(getConcertVenueIdColumnName())
                                      .to(5L)
                                      .set(getSingerIdColumnName())
                                      .to(5L)
                                      .build(),
                                  Mutation.delete("Singer", Key.of(5L))));
                          return null;
                        }));
  }

  private String getReferentialConstraintsQueryStatement() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "SELECT DELETE_RULE\n"
          + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS\n"
          + "WHERE CONSTRAINT_NAME ='Fk_Concerts_Singer'";
    } else {
      return "SELECT DELETE_RULE\n"
          + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS\n"
          + "WHERE CONSTRAINT_NAME =\"Fk_Concerts_Singer\"";
    }
  }

  private String getReferentialConstraintsQueryStatementV2() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "SELECT DELETE_RULE\n"
          + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS\n"
          + "WHERE CONSTRAINT_NAME ='Fk_Concerts_Singer_V2'";
    } else {
      return "SELECT DELETE_RULE\n"
          + "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS\n"
          + "WHERE CONSTRAINT_NAME =\"Fk_Concerts_Singer_V2\"";
    }
  }

  private List<String> getCreateAndAlterTableStatementsWithForeignKey() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return ImmutableList.of(
          POSTGRES_CREATE_TABLE_SINGER,
          POSTGRES_CREATE_TABLE_CONCERT_V2_WITHOUT_FOREIGN_KEY,
          POSTGRES_ALTER_TABLE_CONCERT_V2_WITH_FOREIGN_KEY);
    } else {
      return ImmutableList.of(
          CREATE_TABLE_SINGER,
          CREATE_TABLE_CONCERT_V2_WITHOUT_FOREIGN_KEY,
          ALTER_TABLE_CONCERT_V2_WITH_FOREIGN_KEY);
    }
  }

  private List<String> getAlterDropForeignKeyDeleteCascadeStatements() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return ImmutableList.of(
          ALTER_TABLE_CONCERT_V2_DROP_FOREIGN_KEY_CONSTRAINT,
          POSTGRES_ALTER_TABLE_CONCERT_V2_UPDATE_FOREIGN_KEY_WITHOUT_DELETE_CASCADE);
    } else {
      return ImmutableList.of(
          ALTER_TABLE_CONCERT_V2_DROP_FOREIGN_KEY_CONSTRAINT,
          ALTER_TABLE_CONCERT_V2_UPDATE_FOREIGN_KEY_WITHOUT_DELETE_CASCADE);
    }
  }

  private DatabaseAdminClient getDatabaseAdminClient() {
    return env.getTestHelper().getClient().getDatabaseAdminClient();
  }

  private DatabaseClient getCreatedDatabaseClient() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return env.getTestHelper().getDatabaseClient(this.POSTGRESQL_DATABASE);
    }
    return env.getTestHelper().getDatabaseClient(this.GOOGLE_STANDARD_SQL_DATABASE);
  }

  private String getInsertStatementForSingerTable() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "INSERT INTO Singer (singer_id, first_name) VALUES ($1, $2)";
    } else {
      return "INSERT INTO Singer (SingerId, FirstName) VALUES (@p1, @p2)";
    }
  }

  private String getInsertStatementForConcertsTable() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "INSERT INTO Concerts (venue_id, singer_id) VALUES ($1, $2)";
    } else {
      return "INSERT INTO Concerts (VenueId, SingerId) VALUES (@p1, @p2)";
    }
  }

  private String getDeleteStatementForSingerTable() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "DELETE FROM Singer WHERE singer_id = $1";
    } else {
      return "DELETE FROM Singer WHERE SingerId = @p1";
    }
  }

  private String getConcertVenueIdColumnName() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "venue_id";
    } else {
      return "VenueId";
    }
  }

  private String getSingerFirstNameColumnName() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "first_name";
    } else {
      return "FirstName";
    }
  }

  private String getSingerIdColumnName() {
    if (dialect.dialect == Dialect.POSTGRESQL) {
      return "singer_id";
    } else {
      return "SingerId";
    }
  }
}
