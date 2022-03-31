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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.AbstractStatementParser.ParsedStatement;
import com.google.cloud.spanner.connection.AbstractStatementParser.StatementType;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StatementParserTest {
  public static final String COPYRIGHT_PATTERN =
      "\\/\\*\n"
          + " \\* Copyright \\d{4} Google LLC\n"
          + " \\*\n"
          + " \\* Licensed under the Apache License, Version 2.0 \\(the \"License\"\\);\n"
          + " \\* you may not use this file except in compliance with the License.\n"
          + " \\* You may obtain a copy of the License at\n"
          + " \\*\n"
          + " \\*       http://www.apache.org/licenses/LICENSE-2.0\n"
          + " \\*\n"
          + " \\* Unless required by applicable law or agreed to in writing, software\n"
          + " \\* distributed under the License is distributed on an \"AS IS\" BASIS,\n"
          + " \\* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
          + " \\* See the License for the specific language governing permissions and\n"
          + " \\* limitations under the License.\n"
          + " \\*\\/\n";
  private static final Pattern EXPECT_PATTERN = Pattern.compile("(?is)\\s*(?:@EXPECT)\\s+'(.*)'");

  @Parameter public Dialect dialect;

  @Parameters(name = "dialect = {0}")
  public static Object[] data() {
    return Dialect.values();
  }

  private AbstractStatementParser parser;

  @Before
  public void setupParser() {
    parser = AbstractStatementParser.getInstance(dialect);
  }

  private static final ImmutableMap<Dialect, String> COMMENTS_SCRIPTS =
      ImmutableMap.of(
          Dialect.GOOGLE_STANDARD_SQL,
          "CommentsTest.sql",
          Dialect.POSTGRESQL,
          "postgresql/CommentsTest.sql");

  @Test
  public void testRemoveCommentsInScript() {
    List<String> statements = readStatementsFromFile(COMMENTS_SCRIPTS.get(dialect));
    String currentlyExpected = "";
    for (String statement : statements) {
      String sql = statement.trim();
      if (sql.startsWith("@EXPECT")) {
        Matcher matcher = EXPECT_PATTERN.matcher(sql);
        if (matcher.matches()) {
          currentlyExpected = matcher.group(1);
        } else {
          throw new IllegalArgumentException("Unknown @EXPECT statement: " + sql);
        }
      } else {
        assertThat(parser.removeCommentsAndTrim(statement)).isEqualTo(currentlyExpected);
      }
    }
  }

  @Test
  public void testRemoveComments() {
    assertThat(parser.removeCommentsAndTrim("")).isEqualTo("");
    assertThat(parser.removeCommentsAndTrim("SELECT * FROM FOO")).isEqualTo("SELECT * FROM FOO");
    assertThat(parser.removeCommentsAndTrim("-- This is a one line comment\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/* This is a simple multi line comment */\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim("/* This is a \nmulti line comment */\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/* This\nis\na\nmulti\nline\ncomment */\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");

    assertEquals(
        "SELECT \"FOO\" FROM \"BAR\" WHERE name='test'",
        parser.removeCommentsAndTrim(
            "-- Single line comment\nSELECT \"FOO\" FROM \"BAR\" WHERE name='test'"));
    assertEquals(
        "SELECT \"FOO\" FROM \"BAR\" WHERE name='test' and id=1",
        parser.removeCommentsAndTrim(
            "/* Multi\n"
                + "line\n"
                + "comment\n"
                + "*/SELECT \"FOO\" FROM \"BAR\" WHERE name='test' and id=1"));

    if (dialect == Dialect.POSTGRESQL) {
      // PostgreSQL allows string literals and quoted identifiers to contain newline characters.
      assertEquals(
          "SELECT \"FOO\nBAR\" FROM \"BAR\" WHERE name='test\ntest'",
          parser.removeCommentsAndTrim(
              "-- Single line comment\nSELECT \"FOO\nBAR\" FROM \"BAR\" WHERE name='test\ntest'"));
      assertEquals(
          "SELECT \"FOO\nBAR\" FROM \"BAR\" WHERE name='test\ntest' and id=1",
          parser.removeCommentsAndTrim(
              "/* Multi\n"
                  + "line\n"
                  + "comment\n"
                  + "*/SELECT \"FOO\nBAR\" FROM \"BAR\" WHERE name='test\ntest' and id=1"));
    }
  }

  @Test
  public void testGoogleStandardSQLRemoveCommentsGsql() {
    assumeTrue(dialect == Dialect.GOOGLE_STANDARD_SQL);

    assertThat(parser.removeCommentsAndTrim("/*GSQL*/")).isEqualTo("");
    assertThat(parser.removeCommentsAndTrim("/*GSQL*/SELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*/-- This is a one line comment\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*//* This is a simple multi line comment */\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*//* This is a \nmulti line comment */\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*//* This\nis\na\nmulti\nline\ncomment */\nSELECT * FROM FOO"))
        .isEqualTo("SELECT * FROM FOO");
  }

  @Test
  public void testPostgreSQLDialectRemoveCommentsGsql() {
    assumeTrue(dialect == Dialect.POSTGRESQL);

    assertThat(parser.removeCommentsAndTrim("/*GSQL*/")).isEqualTo("/*GSQL*/");
    assertThat(parser.removeCommentsAndTrim("/*GSQL*/SELECT * FROM FOO"))
        .isEqualTo("/*GSQL*/SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*/-- This is a one line comment\nSELECT * FROM FOO"))
        .isEqualTo("/*GSQL*/SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*//* This is a simple multi line comment */\nSELECT * FROM FOO"))
        .isEqualTo("/*GSQL*/SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*//* This is a \nmulti line comment */\nSELECT * FROM FOO"))
        .isEqualTo("/*GSQL*/SELECT * FROM FOO");
    assertThat(
            parser.removeCommentsAndTrim(
                "/*GSQL*//* This\nis\na\nmulti\nline\ncomment */\nSELECT * FROM FOO"))
        .isEqualTo("/*GSQL*/SELECT * FROM FOO");
  }

  @Test
  public void testStatementWithCommentContainingSlash() {
    String sql =
        "/*\n"
            + " * Script for testing invalid/unrecognized statements\n"
            + " */\n"
            + "\n"
            + "-- MERGE into test comment MERGE -- \n"
            + "@EXPECT EXCEPTION INVALID_ARGUMENT 'INVALID_ARGUMENT: Unknown statement'\n"
            + "MERGE INTO Singers s\n"
            + "/*** test ****/"
            + "USING (VALUES (1, 'John', 'Doe')) v\n"
            + "ON v.column1 = s.SingerId\n"
            + "WHEN NOT MATCHED \n"
            + "  INSERT VALUES (v.column1, v.column2, v.column3)\n"
            + "WHEN MATCHED\n"
            + "  UPDATE SET FirstName = v.column2,\n"
            + "             LastName = v.column3;";
    String sqlWithoutComments =
        "@EXPECT EXCEPTION INVALID_ARGUMENT 'INVALID_ARGUMENT: Unknown statement'\n"
            + "MERGE INTO Singers s\n"
            + "USING (VALUES (1, 'John', 'Doe')) v\n"
            + "ON v.column1 = s.SingerId\n"
            + "WHEN NOT MATCHED \n"
            + "  INSERT VALUES (v.column1, v.column2, v.column3)\n"
            + "WHEN MATCHED\n"
            + "  UPDATE SET FirstName = v.column2,\n"
            + "             LastName = v.column3";
    ParsedStatement statement = parser.parse(Statement.of(sql));
    assertThat(statement.getSqlWithoutComments()).isEqualTo(sqlWithoutComments);
  }

  @Test
  public void testStatementWithCommentContainingSlashAndNoAsteriskOnNewLine() {
    String sql =
        "/*\n"
            + " * Script for testing invalid/unrecognized statements\n"
            + " foo bar baz"
            + " */\n"
            + "\n"
            + "-- MERGE INTO test comment MERGE\n"
            + "@EXPECT EXCEPTION INVALID_ARGUMENT 'INVALID_ARGUMENT: Unknown statement'\n"
            + "MERGE INTO Singers s\n"
            + "USING (VALUES (1, 'John', 'Doe')) v\n"
            + "ON v.column1 = s.SingerId\n"
            + "-- test again --\n"
            + "WHEN NOT MATCHED \n"
            + "  INSERT VALUES (v.column1, v.column2, v.column3)\n"
            + "WHEN MATCHED\n"
            + "  UPDATE SET FirstName = v.column2,\n"
            + "             LastName = v.column3;";
    String sqlWithoutComments =
        "@EXPECT EXCEPTION INVALID_ARGUMENT 'INVALID_ARGUMENT: Unknown statement'\n"
            + "MERGE INTO Singers s\n"
            + "USING (VALUES (1, 'John', 'Doe')) v\n"
            + "ON v.column1 = s.SingerId\n"
            + "\nWHEN NOT MATCHED \n"
            + "  INSERT VALUES (v.column1, v.column2, v.column3)\n"
            + "WHEN MATCHED\n"
            + "  UPDATE SET FirstName = v.column2,\n"
            + "             LastName = v.column3";
    ParsedStatement statement = parser.parse(Statement.of(sql));
    assertThat(statement.getSqlWithoutComments()).isEqualTo(sqlWithoutComments);
  }

  @Test
  public void testPostgresSQLDialectDollarQuoted() {
    assumeTrue(dialect == Dialect.POSTGRESQL);

    assertThat(parser.removeCommentsAndTrim("$$foo$$")).isEqualTo("$$foo$$");
    assertThat(parser.removeCommentsAndTrim("$$--foo$$")).isEqualTo("$$--foo$$");
    assertThat(parser.removeCommentsAndTrim("$$\nline 1\n--line2$$"))
        .isEqualTo("$$\nline 1\n--line2$$");
    assertThat(parser.removeCommentsAndTrim("$bar$--foo$bar$")).isEqualTo("$bar$--foo$bar$");
    assertThat(
            parser.removeCommentsAndTrim(
                "$bar$\nThis is a valid string\n -- That could contain special characters$bar$"))
        .isEqualTo("$bar$\nThis is a valid string\n -- That could contain special characters$bar$");

    assertThat(parser.removeCommentsAndTrim("SELECT FOO$BAR FROM SOME_TABLE"))
        .isEqualTo("SELECT FOO$BAR FROM SOME_TABLE");
    assertThat(parser.removeCommentsAndTrim("SELECT FOO$BAR -- This is a comment\nFROM SOME_TABLE"))
        .isEqualTo("SELECT FOO$BAR \nFROM SOME_TABLE");
    assertThat(
            parser.removeCommentsAndTrim("SELECT FOO, $BAR -- This is a comment\nFROM SOME_TABLE"))
        .isEqualTo("SELECT FOO, $BAR \nFROM SOME_TABLE");
  }

  @Test
  public void testPostgreSQLDialectSupportsEmbeddedComments() {
    assumeTrue(dialect == Dialect.POSTGRESQL);

    final String sql =
        "/* This is a comment /* This is an embedded comment */ This is after the embedded comment */ SELECT 1";
    assertEquals("SELECT 1", parser.removeCommentsAndTrim(sql));
  }

  @Test
  public void testGoogleStandardSQLDialectDoesNotSupportEmbeddedComments() {
    assumeTrue(dialect == Dialect.GOOGLE_STANDARD_SQL);

    final String sql =
        "/* This is a comment /* This is an embedded comment */ This is after the embedded comment */ SELECT 1";
    assertEquals(
        "This is after the embedded comment */ SELECT 1", parser.removeCommentsAndTrim(sql));
  }

  @Test
  public void testPostgreSQLDialectUnterminatedComment() {
    assumeTrue(dialect == Dialect.POSTGRESQL);

    final String sql =
        "/* This is a comment /* This is still a comment */ this is unterminated SELECT 1";
    try {
      // Cloud Spanner would see this as a valid comment, while PostgreSQL
      // requires 'embedded' comments to be properly terminated.
      parser.removeCommentsAndTrim(sql);
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
      assertTrue(
          "Message should contain 'unterminated block comment'",
          e.getMessage().contains("unterminated block comment"));
    }
  }

  @Test
  public void testGoogleStandardSqlDialectDialectUnterminatedComment() {
    assumeTrue(dialect == Dialect.GOOGLE_STANDARD_SQL);

    final String sql =
        "/* This is a comment /* This is still a comment */ this is unterminated SELECT 1";
    assertEquals("this is unterminated SELECT 1", parser.removeCommentsAndTrim(sql));
  }

  @Test
  public void testShowStatements() {
    AbstractStatementParser parser = AbstractStatementParser.getInstance(dialect);

    assertThat(parser.parse(Statement.of("show variable autocommit bar")).getType())
        .isEqualTo(StatementType.QUERY);
    assertThat(parser.parse(Statement.of("show variable autocommit")).getType())
        .isEqualTo(StatementType.CLIENT_SIDE);
    if (dialect == Dialect.POSTGRESQL) {
      assertThat(parser.parse(Statement.of("show autocommit")).getType())
          .isEqualTo(StatementType.CLIENT_SIDE);
      assertThat(
              parser.parse(Statement.of("show variable spanner.retry_aborts_internally")).getType())
          .isEqualTo(StatementType.CLIENT_SIDE);
    } else {
      assertThat(parser.parse(Statement.of("show autocommit")).getType())
          .isEqualTo(StatementType.QUERY);
      assertThat(parser.parse(Statement.of("show variable retry_aborts_internally")).getType())
          .isEqualTo(StatementType.CLIENT_SIDE);
    }

    assertThat(parser.parse(Statement.of("show variable retry_aborts_internally bar")).getType())
        .isEqualTo(StatementType.QUERY);
  }

  @Test
  public void testGoogleStandardSQLDialectStatementWithHashTagSingleLineComment() {
    assumeTrue(dialect == Dialect.GOOGLE_STANDARD_SQL);

    // Supports # based comments
    assertThat(
            parser
                .parse(Statement.of("# this is a comment\nselect * from foo"))
                .getSqlWithoutComments())
        .isEqualTo("select * from foo");
    assertThat(
            parser
                .parse(Statement.of("select * from foo\n#this is a comment"))
                .getSqlWithoutComments())
        .isEqualTo("select * from foo");
    assertThat(
            parser
                .parse(Statement.of("select *\nfrom foo # this is a comment\nwhere bar=1"))
                .getSqlWithoutComments())
        .isEqualTo("select *\nfrom foo \nwhere bar=1");
  }

  @Test
  public void testPostgreSQLDialectStatementWithHashTagSingleLineComment() {
    assumeTrue(dialect == Dialect.POSTGRESQL);

    // Does not support # based comments
    assertThat(
            parser
                .parse(Statement.of("# this is a comment\nselect * from foo"))
                .getSqlWithoutComments())
        .isEqualTo("# this is a comment\nselect * from foo");
    assertThat(
            parser
                .parse(Statement.of("select * from foo\n#this is a comment"))
                .getSqlWithoutComments())
        .isEqualTo("select * from foo\n#this is a comment");
    assertThat(
            parser
                .parse(Statement.of("select *\nfrom foo # this is a comment\nwhere bar=1"))
                .getSqlWithoutComments())
        .isEqualTo("select *\nfrom foo # this is a comment\nwhere bar=1");
  }

  @Test
  public void testIsDdlStatement() {
    assertThat(parser.isDdlStatement("")).isFalse();
    assertThat(parser.isDdlStatement("random text")).isFalse();
    assertThat(parser.isDdlStatement("CREATETABLE")).isFalse();
    assertThat(parser.isDdlStatement("CCREATE TABLE")).isFalse();
    assertThat(parser.isDdlStatement("SELECT 1")).isFalse();
    assertThat(parser.isDdlStatement("SELECT FOO FROM BAR")).isFalse();
    assertThat(parser.isDdlStatement("INSERT INTO FOO (ID, NAME) VALUES (1, 'NAME')")).isFalse();
    assertThat(parser.isDdlStatement("UPDATE FOO SET NAME='NAME' WHERE ID=1")).isFalse();
    assertThat(parser.isDdlStatement("DELETE FROM FOO")).isFalse();

    assertThat(
            parser.isDdlStatement("CREATE TABLE FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
        .isTrue();
    assertThat(parser.isDdlStatement("alter table foo add Description string(100)")).isTrue();
    assertThat(parser.isDdlStatement("drop table foo")).isTrue();
    assertThat(parser.isDdlStatement("Create index BAR on foo (name)")).isTrue();

    assertThat(
            parser
                .parse(
                    Statement.of(
                        "\t\tCREATE\n\t   TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "\n\n\nCREATE TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "-- this is a comment\nCREATE TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/* multi line comment\n* with more information on the next line\n*/\nCREATE TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/** java doc comment\n* with more information on the next line\n*/\nCREATE TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "-- SELECT in a single line comment \nCREATE TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/* SELECT in a multi line comment\n* with more information on the next line\n*/\nCREATE TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/** SELECT in a java doc comment\n* with more information on the next line\n*/\nCREATE TABLE   FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
                .isDdl())
        .isTrue();

    assertTrue(
        parser
            .parse(
                Statement.of(
                    "CREATE VIEW SingerNames\n"
                        + "SQL SECURITY INVOKER\n"
                        + "AS SELECT SingerId as SingerId,\n"
                        + "          CONCAT(Singers.FirstName, Singers.LastName) as Name\n"
                        + "   FROM Singers"))
            .isDdl());
    assertTrue(
        parser
            .parse(Statement.of("create view SingerNames as select FullName from Singers"))
            .isDdl());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/* this is a comment */ create view SingerNames as select FullName from Singers"))
            .isDdl());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "create /* this is a comment */ view SingerNames as select FullName from Singers"))
            .isDdl());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "create \n -- This is a comment \n view SingerNames as select FullName from Singers"))
            .isDdl());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    " \t \n create   \n \t  view \n  \t SingerNames as select FullName from Singers"))
            .isDdl());
    assertTrue(parser.parse(Statement.of("DROP VIEW SingerNames")).isDdl());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "ALTER VIEW SingerNames\n"
                        + "AS SELECT SingerId as SingerId,\n"
                        + "          CONCAT(Singers.FirstName, Singers.LastName) as Name\n"
                        + "   FROM Singers"))
            .isDdl());
  }

  @Test
  public void testIsQuery() {
    assertThat(parser.isQuery("")).isFalse();
    assertThat(parser.isQuery("random text")).isFalse();
    assertThat(parser.isQuery("SELECT1")).isFalse();
    assertThat(parser.isQuery("SSELECT 1")).isFalse();

    assertThat(parser.isQuery("SELECT 1")).isTrue();
    assertThat(parser.isQuery("select 1")).isTrue();
    assertThat(parser.isQuery("SELECT foo FROM bar WHERE id=@id")).isTrue();

    assertThat(parser.isQuery("INSERT INTO FOO (ID, NAME) VALUES (1, 'NAME')")).isFalse();
    assertThat(parser.isQuery("UPDATE FOO SET NAME='NAME' WHERE ID=1")).isFalse();
    assertThat(parser.isQuery("DELETE FROM FOO")).isFalse();
    assertThat(parser.isQuery("CREATE TABLE FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"))
        .isFalse();
    assertThat(parser.isQuery("alter table foo add Description string(100)")).isFalse();
    assertThat(parser.isQuery("drop table foo")).isFalse();
    assertThat(parser.isQuery("Create index BAR on foo (name)")).isFalse();

    assertThat(parser.isQuery("select * from foo")).isTrue();

    assertThat(parser.isQuery("INSERT INTO FOO (ID, NAME) SELECT ID+1, NAME FROM FOO")).isFalse();

    assertThat(
            parser.isQuery(
                "WITH subQ1 AS (SELECT SchoolID FROM Roster),\n"
                    + "     subQ2 AS (SELECT OpponentID FROM PlayerStats)\n"
                    + "SELECT * FROM subQ1\n"
                    + "UNION ALL\n"
                    + "SELECT * FROM subQ2"))
        .isTrue();
    assertThat(
            parser.isQuery(
                "with subQ1 AS (SELECT SchoolID FROM Roster),\n"
                    + "     subQ2 AS (SELECT OpponentID FROM PlayerStats)\n"
                    + "select * FROM subQ1\n"
                    + "UNION ALL\n"
                    + "SELECT * FROM subQ2"))
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "-- this is a comment\nwith foo as (select * from bar)\nselect * from foo"))
                .isQuery())
        .isTrue();

    assertThat(parser.parse(Statement.of("-- this is a comment\nselect * from foo")).isQuery())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/* multi line comment\n* with more information on the next line\n*/\nSELECT ID, NAME\nFROM\tTEST\n\tWHERE ID=1"))
                .isQuery())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/** java doc comment\n* with more information on the next line\n*/\nselect max(id) from test"))
                .isQuery())
        .isTrue();
    assertThat(
            parser
                .parse(Statement.of("-- INSERT in a single line comment \n    select 1"))
                .isQuery())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/* UPDATE in a multi line comment\n* with more information on the next line\n*/\nSELECT 1"))
                .isQuery())
        .isTrue();
    assertThat(
            parser
                .parse(
                    Statement.of(
                        "/** DELETE in a java doc comment\n* with more information on the next line\n*/\n\n\n\n -- UPDATE test\nSELECT 1"))
                .isQuery())
        .isTrue();
  }

  @Test
  public void testGoogleStandardSQLDialectIsQuery_QueryHints() {
    assumeTrue(dialect == Dialect.GOOGLE_STANDARD_SQL);

    // Supports query hints, PostgreSQL dialect does NOT
    // Valid query hints.
    assertTrue(parser.isQuery("@{JOIN_METHOD=HASH_JOIN} SELECT * FROM PersonsTable"));
    assertTrue(parser.isQuery("@ {JOIN_METHOD=HASH_JOIN} SELECT * FROM PersonsTable"));
    assertTrue(parser.isQuery("@{ JOIN_METHOD=HASH_JOIN} SELECT * FROM PersonsTable"));
    assertTrue(parser.isQuery("@{JOIN_METHOD=HASH_JOIN } SELECT * FROM PersonsTable"));
    assertTrue(parser.isQuery("@{JOIN_METHOD=HASH_JOIN}\nSELECT * FROM PersonsTable"));
    assertTrue(parser.isQuery("@{\nJOIN_METHOD =  HASH_JOIN   \t}\n\t SELECT * FROM PersonsTable"));
    assertTrue(
        parser.isQuery(
            "@{JOIN_METHOD=HASH_JOIN}\n -- Single line comment\nSELECT * FROM PersonsTable"));
    assertTrue(
        parser.isQuery(
            "@{JOIN_METHOD=HASH_JOIN}\n /* Multi line comment\n with more comments\n */SELECT * FROM PersonsTable"));
    assertTrue(
        parser.isQuery(
            "@{JOIN_METHOD=HASH_JOIN} WITH subQ1 AS (SELECT SchoolID FROM Roster),\n"
                + "     subQ2 AS (SELECT OpponentID FROM PlayerStats)\n"
                + "SELECT * FROM subQ1\n"
                + "UNION ALL\n"
                + "SELECT * FROM subQ2"));

    // Multiple query hints.
    assertTrue(
        parser.isQuery("@{FORCE_INDEX=index_name} @{JOIN_METHOD=HASH_JOIN} SELECT * FROM tbl"));
    assertTrue(
        parser.isQuery("@{FORCE_INDEX=index_name} @{JOIN_METHOD=HASH_JOIN} Select * FROM tbl"));
    assertTrue(
        parser.isQuery(
            "@{FORCE_INDEX=index_name}\n@{JOIN_METHOD=HASH_JOIN}\nWITH subQ1 AS (SELECT SchoolID FROM Roster),\n"
                + "     subQ2 AS (SELECT OpponentID FROM PlayerStats)\n"
                + "SELECT * FROM subQ1\n"
                + "UNION ALL\n"
                + "SELECT * FROM subQ2"));

    // Invalid query hints.
    assertFalse(parser.isQuery("@{JOIN_METHOD=HASH_JOIN SELECT * FROM PersonsTable"));
    assertFalse(parser.isQuery("@JOIN_METHOD=HASH_JOIN} SELECT * FROM PersonsTable"));
    assertFalse(parser.isQuery("@JOIN_METHOD=HASH_JOIN SELECT * FROM PersonsTable"));
    assertFalse(
        parser.isQuery(
            "@{FORCE_INDEX=index_name} @{JOIN_METHOD=HASH_JOIN} UPDATE tbl set FOO=1 WHERE ID=2"));
  }

  @Test
  public void testIsUpdate_QueryHints() {
    assumeTrue(dialect == Dialect.GOOGLE_STANDARD_SQL);

    // Supports query hints, PostgreSQL dialect does NOT
    // Valid query hints.
    assertTrue(
        parser.isUpdateStatement(
            "@{LOCK_SCANNED_RANGES=exclusive} UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(
        parser.isUpdateStatement(
            "@ {LOCK_SCANNED_RANGES=exclusive} UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(
        parser.isUpdateStatement(
            "@{ LOCK_SCANNED_RANGES=exclusive} UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(
        parser.isUpdateStatement(
            "@{LOCK_SCANNED_RANGES=exclusive } UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(
        parser.isUpdateStatement(
            "@{LOCK_SCANNED_RANGES=exclusive}\nUPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(
        parser.isUpdateStatement(
            "@{\nLOCK_SCANNED_RANGES =  exclusive   \t}\n\t UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(
        parser.isUpdateStatement(
            "@{LOCK_SCANNED_RANGES=exclusive}\n -- Single line comment\nUPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(
        parser.isUpdateStatement(
            "@{LOCK_SCANNED_RANGES=exclusive}\n /* Multi line comment\n with more comments\n */UPDATE FOO SET NAME='foo' WHERE ID=1"));

    // Multiple query hints.
    assertTrue(
        parser.isUpdateStatement(
            "@{LOCK_SCANNED_RANGES=exclusive} @{USE_ADDITIONAL_PARALLELISM=TRUE} UPDATE FOO SET NAME='foo' WHERE ID=1"));

    // Invalid query hints.
    assertFalse(
        parser.isUpdateStatement(
            "@{LOCK_SCANNED_RANGES=exclusive UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertFalse(
        parser.isUpdateStatement(
            "@LOCK_SCANNED_RANGES=exclusive} UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertFalse(
        parser.isUpdateStatement(
            "@LOCK_SCANNED_RANGES=exclusive UPDATE FOO SET NAME='foo' WHERE ID=1"));
  }

  @Test
  public void testIsUpdate_InsertStatements() {
    assertFalse(parser.isUpdateStatement(""));
    assertFalse(parser.isUpdateStatement("random text"));
    assertFalse(parser.isUpdateStatement("INSERTINTO FOO (ID) VALUES (1)"));
    assertFalse(parser.isUpdateStatement("IINSERT INTO FOO (ID) VALUES (1)"));
    assertTrue(parser.isUpdateStatement("INSERT INTO FOO (ID) VALUES (1)"));
    assertTrue(parser.isUpdateStatement("insert into foo (id) values (1)"));
    assertTrue(parser.isUpdateStatement("INSERT into Foo (id)\nSELECT id FROM bar WHERE id=@id"));
    assertFalse(parser.isUpdateStatement("SELECT 1"));
    assertFalse(parser.isUpdateStatement("SELECT NAME FROM FOO WHERE ID=1"));
    assertFalse(
        parser.isUpdateStatement("CREATE TABLE FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"));
    assertFalse(parser.isUpdateStatement("alter table foo add Description string(100)"));
    assertFalse(parser.isUpdateStatement("drop table foo"));
    assertFalse(parser.isUpdateStatement("Create index BAR on foo (name)"));
    assertFalse(parser.isUpdateStatement("select * from foo"));
    assertTrue(parser.isUpdateStatement("INSERT INTO FOO (ID, NAME) SELECT ID+1, NAME FROM FOO"));
    assertTrue(
        parser
            .parse(Statement.of("-- this is a comment\ninsert into foo (id) values (1)"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/* multi line comment\n* with more information on the next line\n*/\nINSERT INTO FOO\n(ID)\tVALUES\n\t(1)"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/** java doc comment\n* with more information on the next line\n*/\nInsert intO foo (id) select 1"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "-- SELECT in a single line comment \n    insert into foo (id) values (1)"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/* CREATE in a multi line comment\n* with more information on the next line\n*/\nINSERT INTO FOO (ID) VALUES (1)"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/** DROP in a java doc comment\n* with more information on the next line\n*/\n\n\n\n -- SELECT test\ninsert into foo (id) values (1)"))
            .isUpdate());
  }

  @Test
  public void testIsUpdate_UpdateStatements() {
    assertFalse(parser.isUpdateStatement(""));
    assertFalse(parser.isUpdateStatement("random text"));
    assertFalse(parser.isUpdateStatement("UPDATEFOO SET NAME='foo' WHERE ID=1"));
    assertFalse(parser.isUpdateStatement("UUPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(parser.isUpdateStatement("UPDATE FOO SET NAME='foo' WHERE ID=1"));
    assertTrue(parser.isUpdateStatement("update foo set name='foo' where id=1"));
    assertTrue(
        parser.isUpdateStatement("update foo set name=\n(SELECT name FROM bar WHERE id=@id)"));
    assertFalse(parser.isUpdateStatement("SELECT 1"));
    assertFalse(parser.isUpdateStatement("SELECT NAME FROM FOO WHERE ID=1"));
    assertFalse(
        parser.isUpdateStatement("CREATE TABLE FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"));
    assertFalse(parser.isUpdateStatement("alter table foo add Description string(100)"));
    assertFalse(parser.isUpdateStatement("drop table foo"));
    assertFalse(parser.isUpdateStatement("Create index BAR on foo (name)"));
    assertFalse(parser.isUpdateStatement("select * from foo"));
    assertTrue(
        parser.isUpdateStatement(
            "UPDATE FOO SET NAME=(SELECT NAME FROM FOO) WHERE ID=(SELECT ID+1 FROM FOO)"));

    assertTrue(
        parser
            .parse(Statement.of("-- this is a comment\nupdate foo set name='foo' where id=@id"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/* multi line comment\n* with more information on the next line\n*/\nUPDATE FOO\nSET NAME=\t'foo'\n\tWHERE ID=1"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/** java doc comment\n* with more information on the next line\n*/\nUPDATE FOO SET NAME=(select 'bar')"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of("-- SELECT in a single line comment \n    update foo set name='bar'"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/* CREATE in a multi line comment\n* with more information on the next line\n*/\nUPDATE FOO SET NAME='BAR'"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/** DROP in a java doc comment\n* with more information on the next line\n*/\n\n\n\n -- SELECT test\nupdate foo set bar='foo'"))
            .isUpdate());
  }

  @Test
  public void testIsUpdate_DeleteStatements() {
    assertFalse(parser.isUpdateStatement(""));
    assertFalse(parser.isUpdateStatement("random text"));
    assertFalse(parser.isUpdateStatement("DELETEFROM FOO WHERE ID=1"));
    assertFalse(parser.isUpdateStatement("DDELETE FROM FOO WHERE ID=1"));
    assertTrue(parser.isUpdateStatement("DELETE FROM FOO WHERE ID=1"));
    assertTrue(parser.isUpdateStatement("delete from foo where id=1"));
    assertTrue(
        parser.isUpdateStatement(
            "delete from foo where name=\n(SELECT name FROM bar WHERE id=@id)"));
    assertFalse(parser.isUpdateStatement("SELECT 1"));
    assertFalse(parser.isUpdateStatement("SELECT NAME FROM FOO WHERE ID=1"));
    assertFalse(
        parser.isUpdateStatement("CREATE TABLE FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"));
    assertFalse(parser.isUpdateStatement("alter table foo add Description string(100)"));
    assertFalse(parser.isUpdateStatement("drop table foo"));
    assertFalse(parser.isUpdateStatement("Create index BAR on foo (name)"));
    assertFalse(parser.isUpdateStatement("select * from foo"));
    assertTrue(
        parser.isUpdateStatement(
            "UPDATE FOO SET NAME=(SELECT NAME FROM FOO) WHERE ID=(SELECT ID+1 FROM FOO)"));

    assertTrue(
        parser
            .parse(Statement.of("-- this is a comment\ndelete from foo  where id=@id"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/* multi line comment\n* with more information on the next line\n*/\nDELETE FROM FOO\n\n\tWHERE ID=1"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/** java doc comment\n* with more information on the next line\n*/\nDELETE FROM FOO WHERE NAME=(select 'bar')"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "-- SELECT in a single line comment \n    delete from foo where name='bar'"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/* CREATE in a multi line comment\n* with more information on the next line\n*/\nDELETE FROM FOO WHERE NAME='BAR'"))
            .isUpdate());
    assertTrue(
        parser
            .parse(
                Statement.of(
                    "/** DROP in a java doc comment\n* with more information on the next line\n*/\n\n\n\n -- SELECT test\ndelete from foo where bar='foo'"))
            .isUpdate());
  }

  @Test
  public void testParseStatementsWithNoParameters() throws CompileException {
    for (ClientSideStatementImpl statement : getAllStatements()) {
      if (statement.getSetStatement() == null) {
        for (String testStatement : statement.getExampleStatements()) {
          testParseStatement(testStatement, statement.getClass());
        }
      }
    }
  }

  @Test
  public void testParseStatementsWithOneParameterAtTheEnd() throws CompileException {
    for (ClientSideStatementImpl statement : getAllStatements()) {
      if (statement.getSetStatement() != null) {
        for (String testStatement : statement.getExampleStatements()) {
          testParseStatementWithOneParameterAtTheEnd(testStatement, statement.getClass());
        }
      }
    }
  }

  private Set<ClientSideStatementImpl> getAllStatements() throws CompileException {
    return ClientSideStatements.getInstance(dialect).getCompiledStatements();
  }

  private <T extends ClientSideStatementImpl> void assertParsing(
      String value, Class<T> statementClass) {
    assertThat(this.<T>parse(value)).isEqualTo(statementClass);
  }

  private <T extends ClientSideStatementImpl> void testParseStatement(
      String statement, Class<T> statementClass) {
    Truth.assertWithMessage("\"" + statement + "\" should be " + statementClass.getName())
        .that(this.<T>parse(statement))
        .isEqualTo(statementClass);
    assertParsing(upper(statement), statementClass);
    assertParsing(lower(statement), statementClass);
    assertParsing(withSpaces(statement), statementClass);
    assertParsing(withTabs(statement), statementClass);
    assertParsing(withLinefeeds(statement), statementClass);
    assertParsing(withLeadingSpaces(statement), statementClass);
    assertParsing(withLeadingTabs(statement), statementClass);
    assertParsing(withLeadingLinefeeds(statement), statementClass);
    assertParsing(withTrailingSpaces(statement), statementClass);
    assertParsing(withTrailingTabs(statement), statementClass);
    assertParsing(withTrailingLinefeeds(statement), statementClass);

    assertThat(parse(withInvalidPrefix(statement))).isNull();
    assertThat(parse(withInvalidSuffix(statement))).isNull();

    assertThat(parse(withPrefix("%", statement))).isNull();
    assertThat(parse(withPrefix("_", statement))).isNull();
    assertThat(parse(withPrefix("&", statement))).isNull();
    assertThat(parse(withPrefix("$", statement))).isNull();
    assertThat(parse(withPrefix("@", statement))).isNull();
    assertThat(parse(withPrefix("!", statement))).isNull();
    assertThat(parse(withPrefix("*", statement))).isNull();
    assertThat(parse(withPrefix("(", statement))).isNull();
    assertThat(parse(withPrefix(")", statement))).isNull();

    Truth.assertWithMessage(withSuffix("%", statement) + " is not a valid statement")
        .that(parse(withSuffix("%", statement)))
        .isNull();
    assertThat(parse(withSuffix("_", statement))).isNull();
    assertThat(parse(withSuffix("&", statement))).isNull();
    assertThat(parse(withSuffix("$", statement))).isNull();
    assertThat(parse(withSuffix("@", statement))).isNull();
    assertThat(parse(withSuffix("!", statement))).isNull();
    assertThat(parse(withSuffix("*", statement))).isNull();
    assertThat(parse(withSuffix("(", statement))).isNull();
    assertThat(parse(withSuffix(")", statement))).isNull();
  }

  private <T extends ClientSideStatementImpl> void testParseStatementWithOneParameterAtTheEnd(
      String statement, Class<T> statementClass) {
    Truth.assertWithMessage("\"" + statement + "\" should be " + statementClass.getName())
        .that(this.<T>parse(statement))
        .isEqualTo(statementClass);
    assertParsing(upper(statement), statementClass);
    assertParsing(lower(statement), statementClass);
    assertParsing(withSpaces(statement), statementClass);
    assertParsing(withTabs(statement), statementClass);
    assertParsing(withLinefeeds(statement), statementClass);
    assertParsing(withLeadingSpaces(statement), statementClass);
    assertParsing(withLeadingTabs(statement), statementClass);
    assertParsing(withLeadingLinefeeds(statement), statementClass);
    assertParsing(withTrailingSpaces(statement), statementClass);
    assertParsing(withTrailingTabs(statement), statementClass);
    assertParsing(withTrailingLinefeeds(statement), statementClass);

    assertThat(parse(withInvalidPrefix(statement))).isNull();
    assertParsing(withInvalidSuffix(statement), statementClass);

    assertThat(parse(withPrefix("%", statement))).isNull();
    assertThat(parse(withPrefix("_", statement))).isNull();
    assertThat(parse(withPrefix("&", statement))).isNull();
    assertThat(parse(withPrefix("$", statement))).isNull();
    assertThat(parse(withPrefix("@", statement))).isNull();
    assertThat(parse(withPrefix("!", statement))).isNull();
    assertThat(parse(withPrefix("*", statement))).isNull();
    assertThat(parse(withPrefix("(", statement))).isNull();
    assertThat(parse(withPrefix(")", statement))).isNull();

    assertParsing(withSuffix("%", statement), statementClass);
    assertParsing(withSuffix("_", statement), statementClass);
    assertParsing(withSuffix("&", statement), statementClass);
    assertParsing(withSuffix("$", statement), statementClass);
    assertParsing(withSuffix("@", statement), statementClass);
    assertParsing(withSuffix("!", statement), statementClass);
    assertParsing(withSuffix("*", statement), statementClass);
    assertParsing(withSuffix("(", statement), statementClass);
    assertParsing(withSuffix(")", statement), statementClass);
  }

  @Test
  public void testConvertPositionalParametersToNamedParametersWithGsqlException() {
    assertThat(
            parser.convertPositionalParametersToNamedParameters(
                    '?', "/*GSQL*/select * from foo where name=?")
                .sqlWithNamedParameters)
        .isEqualTo("/*GSQL*/select * from foo where name=@p1");
    assertThat(
            parser.convertPositionalParametersToNamedParameters(
                    '?', "/*GSQL*/?'?test?\"?test?\"?'?")
                .sqlWithNamedParameters)
        .isEqualTo("/*GSQL*/@p1'?test?\"?test?\"?'@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "/*GSQL*/?'?it\\'?s'?")
                .sqlWithNamedParameters)
        .isEqualTo("/*GSQL*/@p1'?it\\'?s'@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "/*GSQL*/?'?it\\\"?s'?")
                .sqlWithNamedParameters)
        .isEqualTo("/*GSQL*/@p1'?it\\\"?s'@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "/*GSQL*/?\"?it\\\"?s\"?")
                .sqlWithNamedParameters)
        .isEqualTo("/*GSQL*/@p1\"?it\\\"?s\"@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "/*GSQL*/?'''?it\\'?s'''?")
                .sqlWithNamedParameters)
        .isEqualTo("/*GSQL*/@p1'''?it\\'?s'''@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters(
                    '?', "/*GSQL*/?\"\"\"?it\\\"?s\"\"\"?")
                .sqlWithNamedParameters)
        .isEqualTo("/*GSQL*/@p1\"\"\"?it\\\"?s\"\"\"@p2");

    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "/*GSQL*/select 1, ?, 'test?test', \"test?test\", foo.* from `foo` where col1=? and col2='test' and col3=? and col4='?' and col5=\"?\" and col6='?''?''?'")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "/*GSQL*/select 1, @p1, 'test?test', \"test?test\", foo.* from `foo` where col1=@p2 and col2='test' and col3=@p3 and col4='?' and col5=\"?\" and col6='?''?''?'")));

    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "/*GSQL*/select * "
                    + "from foo "
                    + "where name=? "
                    + "and col2 like ? "
                    + "and col3 > ?")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "/*GSQL*/select * "
                    + "from foo "
                    + "where name=@p1 "
                    + "and col2 like @p2 "
                    + "and col3 > @p3")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?', "/*GSQL*/select * " + "from foo " + "where id between ? and ?")
            .sqlWithNamedParameters,
        is(equalTo("/*GSQL*/select * " + "from foo " + "where id between @p1 and @p2")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?', "/*GSQL*/select * " + "from foo " + "limit ? offset ?")
            .sqlWithNamedParameters,
        is(equalTo("/*GSQL*/select * " + "from foo " + "limit @p1 offset @p2")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "/*GSQL*/select * "
                    + "from foo "
                    + "where col1=? "
                    + "and col2 like ? "
                    + "and col3 > ? "
                    + "and col4 < ? "
                    + "and col5 != ? "
                    + "and col6 not in (?, ?, ?) "
                    + "and col7 in (?, ?, ?) "
                    + "and col8 between ? and ?")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "/*GSQL*/select * "
                    + "from foo "
                    + "where col1=@p1 "
                    + "and col2 like @p2 "
                    + "and col3 > @p3 "
                    + "and col4 < @p4 "
                    + "and col5 != @p5 "
                    + "and col6 not in (@p6, @p7, @p8) "
                    + "and col7 in (@p9, @p10, @p11) "
                    + "and col8 between @p12 and @p13")));
  }

  @Test
  public void testGoogleStandardSQLDialectConvertPositionalParametersToNamedParameters() {
    assumeTrue(dialect == Dialect.GOOGLE_STANDARD_SQL);

    assertThat(
            parser.convertPositionalParametersToNamedParameters(
                    '?', "select * from foo where name=?")
                .sqlWithNamedParameters)
        .isEqualTo("select * from foo where name=@p1");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'?test?\"?test?\"?'?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1'?test?\"?test?\"?'@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'?it\\'?s'?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1'?it\\'?s'@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'?it\\\"?s'?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1'?it\\\"?s'@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?\"?it\\\"?s\"?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1\"?it\\\"?s\"@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'''?it\\'?s'''?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1'''?it\\'?s'''@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?\"\"\"?it\\\"?s\"\"\"?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1\"\"\"?it\\\"?s\"\"\"@p2");

    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?`?it\\`?s`?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1`?it\\`?s`@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?```?it\\`?s```?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1```?it\\`?s```@p2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'''?it\\'?s \n ?it\\'?s'''?")
                .sqlWithNamedParameters)
        .isEqualTo("@p1'''?it\\'?s \n ?it\\'?s'''@p2");

    assertUnclosedLiteral("?'?it\\'?s \n ?it\\'?s'?");
    assertUnclosedLiteral("?'?it\\'?s \n ?it\\'?s?");
    assertUnclosedLiteral("?'''?it\\'?s \n ?it\\'?s'?");

    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "select 1, ?, 'test?test', \"test?test\", foo.* from `foo` where col1=? and col2='test' and col3=? and col4='?' and col5=\"?\" and col6='?''?''?'")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "select 1, @p1, 'test?test', \"test?test\", foo.* from `foo` where col1=@p2 and col2='test' and col3=@p3 and col4='?' and col5=\"?\" and col6='?''?''?'")));

    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "select * " + "from foo " + "where name=? " + "and col2 like ? " + "and col3 > ?")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "select * "
                    + "from foo "
                    + "where name=@p1 "
                    + "and col2 like @p2 "
                    + "and col3 > @p3")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?', "select * " + "from foo " + "where id between ? and ?")
            .sqlWithNamedParameters,
        is(equalTo("select * " + "from foo " + "where id between @p1 and @p2")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?', "select * " + "from foo " + "limit ? offset ?")
            .sqlWithNamedParameters,
        is(equalTo("select * " + "from foo " + "limit @p1 offset @p2")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "select * "
                    + "from foo "
                    + "where col1=? "
                    + "and col2 like ? "
                    + "and col3 > ? "
                    + "and col4 < ? "
                    + "and col5 != ? "
                    + "and col6 not in (?, ?, ?) "
                    + "and col7 in (?, ?, ?) "
                    + "and col8 between ? and ?")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "select * "
                    + "from foo "
                    + "where col1=@p1 "
                    + "and col2 like @p2 "
                    + "and col3 > @p3 "
                    + "and col4 < @p4 "
                    + "and col5 != @p5 "
                    + "and col6 not in (@p6, @p7, @p8) "
                    + "and col7 in (@p9, @p10, @p11) "
                    + "and col8 between @p12 and @p13")));
  }

  @Test
  public void testPostgreSQLDialectDialectConvertPositionalParametersToNamedParameters() {
    assumeTrue(dialect == Dialect.POSTGRESQL);

    assertThat(
            parser.convertPositionalParametersToNamedParameters(
                    '?', "select * from foo where name=?")
                .sqlWithNamedParameters)
        .isEqualTo("select * from foo where name=$1");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'?test?\"?test?\"?'?")
                .sqlWithNamedParameters)
        .isEqualTo("$1'?test?\"?test?\"?'$2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'?it\\'?s'?")
                .sqlWithNamedParameters)
        .isEqualTo("$1'?it\\'?s'$2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'?it\\\"?s'?")
                .sqlWithNamedParameters)
        .isEqualTo("$1'?it\\\"?s'$2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?\"?it\\\"?s\"?")
                .sqlWithNamedParameters)
        .isEqualTo("$1\"?it\\\"?s\"$2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?'''?it\\'?s'''?")
                .sqlWithNamedParameters)
        .isEqualTo("$1'''?it\\'?s'''$2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?\"\"\"?it\\\"?s\"\"\"?")
                .sqlWithNamedParameters)
        .isEqualTo("$1\"\"\"?it\\\"?s\"\"\"$2");

    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?$$?it$?s$$?")
                .sqlWithNamedParameters)
        .isEqualTo("$1$$?it$?s$$$2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?$tag$?it$$?s$tag$?")
                .sqlWithNamedParameters)
        .isEqualTo("$1$tag$?it$$?s$tag$$2");
    assertThat(
            parser.convertPositionalParametersToNamedParameters('?', "?$$?it\\'?s \n ?it\\'?s$$?")
                .sqlWithNamedParameters)
        .isEqualTo("$1$$?it\\'?s \n ?it\\'?s$$$2");

    // Note: PostgreSQL allows a single-quoted string literal to contain line feeds.
    assertEquals(
        "$1'?it\\'?s \n ?it\\'?s'$2",
        parser.convertPositionalParametersToNamedParameters('?', "?'?it\\'?s \n ?it\\'?s'?")
            .sqlWithNamedParameters);
    assertUnclosedLiteral("?'?it\\'?s \n ?it\\'?s?");
    assertEquals(
        "$1'''?it\\'?s \n ?it\\'?s'$2",
        parser.convertPositionalParametersToNamedParameters('?', "?'''?it\\'?s \n ?it\\'?s'?")
            .sqlWithNamedParameters);

    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "select 1, ?, 'test?test', \"test?test\", foo.* from `foo` where col1=? and col2='test' and col3=? and col4='?' and col5=\"?\" and col6='?''?''?'")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "select 1, $1, 'test?test', \"test?test\", foo.* from `foo` where col1=$2 and col2='test' and col3=$3 and col4='?' and col5=\"?\" and col6='?''?''?'")));

    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "select * " + "from foo " + "where name=? " + "and col2 like ? " + "and col3 > ?")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "select * "
                    + "from foo "
                    + "where name=$1 "
                    + "and col2 like $2 "
                    + "and col3 > $3")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?', "select * " + "from foo " + "where id between ? and ?")
            .sqlWithNamedParameters,
        is(equalTo("select * " + "from foo " + "where id between $1 and $2")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?', "select * " + "from foo " + "limit ? offset ?")
            .sqlWithNamedParameters,
        is(equalTo("select * " + "from foo " + "limit $1 offset $2")));
    assertThat(
        parser.convertPositionalParametersToNamedParameters(
                '?',
                "select * "
                    + "from foo "
                    + "where col1=? "
                    + "and col2 like ? "
                    + "and col3 > ? "
                    + "and col4 < ? "
                    + "and col5 != ? "
                    + "and col6 not in (?, ?, ?) "
                    + "and col7 in (?, ?, ?) "
                    + "and col8 between ? and ?")
            .sqlWithNamedParameters,
        is(
            equalTo(
                "select * "
                    + "from foo "
                    + "where col1=$1 "
                    + "and col2 like $2 "
                    + "and col3 > $3 "
                    + "and col4 < $4 "
                    + "and col5 != $5 "
                    + "and col6 not in ($6, $7, $8) "
                    + "and col7 in ($9, $10, $11) "
                    + "and col8 between $12 and $13")));
  }

  @Test
  public void testPostgreSQLGetQueryParameters() {
    assumeTrue(dialect == Dialect.POSTGRESQL);

    PostgreSQLStatementParser parser = (PostgreSQLStatementParser) this.parser;
    assertEquals(ImmutableSet.of(), parser.getQueryParameters("select * from foo"));
    assertEquals(
        ImmutableSet.of("$1"), parser.getQueryParameters("select * from foo where bar=$1"));
    assertEquals(
        ImmutableSet.of("$1", "$2", "$3"),
        parser.getQueryParameters("select $2 from foo where bar=$1 and baz=$3"));
    assertEquals(
        ImmutableSet.of("$1", "$3"),
        parser.getQueryParameters("select '$2' from foo where bar=$1 and baz in ($1, $3)"));
    assertEquals(
        ImmutableSet.of("$1"),
        parser.getQueryParameters("select '$2' from foo where bar=$1 and baz=$foo"));
  }

  private void assertUnclosedLiteral(String sql) {
    try {
      parser.convertPositionalParametersToNamedParameters('?', sql);
      fail("missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isSameInstanceAs(ErrorCode.INVALID_ARGUMENT);
      assertThat(e.getMessage())
          .startsWith(
              ErrorCode.INVALID_ARGUMENT.name()
                  + ": SQL statement contains an unclosed literal: "
                  + sql);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends ClientSideStatementImpl> Class<T> parse(String statement) {
    ClientSideStatementImpl optional = parser.parseClientSideStatement(statement);
    return optional != null ? (Class<T>) optional.getClass() : null;
  }

  private String upper(String statement) {
    return statement.toUpperCase();
  }

  private String lower(String statement) {
    return statement.toLowerCase();
  }

  private String withLeadingSpaces(String statement) {
    return "   " + statement;
  }

  private String withLeadingTabs(String statement) {
    return "\t\t\t" + statement;
  }

  private String withLeadingLinefeeds(String statement) {
    return "\n\n\n" + statement;
  }

  private String withTrailingSpaces(String statement) {
    return statement + "  ";
  }

  private String withTrailingTabs(String statement) {
    return statement + "\t\t";
  }

  private String withTrailingLinefeeds(String statement) {
    return statement + "\n\n";
  }

  private String withSpaces(String statement) {
    return statement.replaceAll(" ", "   ");
  }

  private String withTabs(String statement) {
    return statement.replaceAll(" ", "\t");
  }

  private String withLinefeeds(String statement) {
    return statement.replaceAll(" ", "\n");
  }

  private String withInvalidPrefix(String statement) {
    return "foo " + statement;
  }

  private String withInvalidSuffix(String statement) {
    return statement + " bar";
  }

  private String withPrefix(String prefix, String statement) {
    return prefix + statement;
  }

  private String withSuffix(String suffix, String statement) {
    return statement + suffix;
  }

  private List<String> readStatementsFromFile(String filename) {
    File file = new File(getClass().getResource(filename).getFile());
    StringBuilder builder = new StringBuilder();
    try (Scanner scanner = new Scanner(file)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        builder.append(line).append("\n");
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    String script = builder.toString().replaceAll(COPYRIGHT_PATTERN, "");
    String[] array = script.split(";");
    List<String> res = new ArrayList<>(array.length);
    for (String statement : array) {
      if (statement != null && statement.trim().length() > 0) {
        res.add(statement);
      }
    }
    return res;
  }
}
