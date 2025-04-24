/*
 * Copyright 2024 Google LLC
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

import static com.google.cloud.spanner.connection.StatementParserTest.assertUnclosedLiteral;
import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.connection.StatementParserTest.CommentInjector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpannerStatementParserTest {

  static String skip(String sql) {
    return skip(sql, 0);
  }

  static String skip(String sql, int currentIndex) {
    int position =
        AbstractStatementParser.getInstance(Dialect.GOOGLE_STANDARD_SQL)
            .skip(sql, currentIndex, null);
    return sql.substring(currentIndex, position);
  }

  @Test
  public void testSkip() {
    assertEquals("", skip(""));
    assertEquals("1", skip("1 "));
    assertEquals("1", skip("12 "));
    assertEquals("2", skip("12 ", 1));
    assertEquals("", skip("12", 2));

    assertEquals("'foo'", skip("'foo'  ", 0));
    assertEquals("'foo'", skip("'foo''bar'  ", 0));
    assertEquals("'foo'", skip("'foo'  'bar'  ", 0));
    assertEquals("'bar'", skip("'foo''bar'  ", 5));
    assertEquals("'foo\"bar\"'", skip("'foo\"bar\"'  ", 0));
    assertEquals("\"foo'bar'\"", skip("\"foo'bar'\"  ", 0));
    assertEquals("`foo'bar'`", skip("`foo'bar'`  ", 0));

    assertEquals("'''foo'bar'''", skip("'''foo'bar'''  ", 0));
    assertEquals("'''foo\\'bar'''", skip("'''foo\\'bar'''  ", 0));
    assertEquals("'''foo\\'\\'bar'''", skip("'''foo\\'\\'bar'''  ", 0));
    assertEquals("'''foo\\'\\'\\'bar'''", skip("'''foo\\'\\'\\'bar'''  ", 0));
    assertEquals("\"\"\"foo'bar\"\"\"", skip("\"\"\"foo'bar\"\"\"", 0));
    assertEquals("```foo'bar```", skip("```foo'bar```", 0));

    assertEquals("-- comment\n", skip("-- comment\nselect * from foo", 0));
    assertEquals("# comment\n", skip("# comment\nselect * from foo", 0));
    assertEquals("/* comment */", skip("/* comment */ select * from foo", 0));
    assertEquals(
        "/* comment /* GoogleSQL does not support nested comments */",
        skip("/* comment /* GoogleSQL does not support nested comments */ select * from foo", 0));
    // GoogleSQL does not support dollar-quoted strings.
    assertEquals("$", skip("$tag$not a string$tag$ select * from foo", 0));

    assertEquals("/* 'test' */", skip("/* 'test' */ foo"));
    assertEquals("-- 'test' \n", skip("-- 'test' \n foo"));
    assertEquals("'/* test */'", skip("'/* test */' foo"));

    // Raw strings do not consider '\' as something that starts an escape sequence, but any
    // quote character following it is still preserved within the string, as the definition of a
    // raw string says that 'both characters are preserved'.
    assertEquals("'foo\\''", skip("'foo\\''  ", 0));
    assertEquals("'foo\\''", skip("r'foo\\''  ", 1));
    assertEquals("'''foo\\'\\'\\'bar'''", skip("'''foo\\'\\'\\'bar'''  ", 0));
  }

  @Test
  public void testConvertPositionalParametersToNamedParameters() {
    AbstractStatementParser parser =
        AbstractStatementParser.getInstance(Dialect.GOOGLE_STANDARD_SQL);

    for (String comment :
        new String[] {
          "-- test comment\n",
          "/* another test comment */",
          "/* comment\nwith\nmultiple\nlines\n */",
          "/* comment /* with nested */ comment */"
        }) {
      for (CommentInjector injector : CommentInjector.values()) {
        assertEquals(
            injector.inject("select * %sfrom foo where name=@p1", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("select * %sfrom foo where name=?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1%s'?test?\"?test?\"?'@p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?%s'?test?\"?test?\"?'?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1'?it\\'?s'%s@p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?'?it\\'?s'%s?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1'?it\\\"?s'%s@p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?'?it\\\"?s'%s?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1\"?it\\\"?s\"%s@p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?\"?it\\\"?s\"%s?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1%s'''?it\\''?s'''@p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?%s'''?it\\''?s'''?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1\"\"\"?it\\\"\"?s\"\"\"%s@p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?\"\"\"?it\\\"\"?s\"\"\"%s?", comment))
                .sqlWithNamedParameters);

        // GoogleSQL does not support dollar-quoted strings, so these are all ignored.
        assertEquals(
            injector.inject("@p1$$@p2it$@p3s$$%s@p4", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?$$?it$?s$$%s?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1$tag$@p2it$$@p3s$tag$%s@p4", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?$tag$?it$$?s$tag$%s?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1%s$$@p2it\\'?s \t ?it\\'?s'$$@p3", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?%s$$?it\\'?s \t ?it\\'?s'$$?", comment))
                .sqlWithNamedParameters);

        // Note: GoogleSQL does not allowa a single-quoted string literal to contain line feeds.
        assertUnclosedLiteral(parser, injector.inject("?'?it\\''?s \n ?it\\''?s'%s?", comment));
        assertEquals(
            "@p1'?it\\''@p2s \n @p3it\\''@p4s@p5",
            parser.convertPositionalParametersToNamedParameters('?', "?'?it\\''?s \n ?it\\''?s?")
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("@p1%s'''?it\\''?s \n ?it\\''?s'''@p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?', injector.inject("?%s'''?it\\''?s \n ?it\\''?s'''?", comment))
                .sqlWithNamedParameters);

        assertEquals(
            injector.inject(
                "select 1, @p1, 'test?test', \"test?test\", %sfoo.* from `foo` where col1=@p2 and"
                    + " col2='test' and col3=@p3 and col4='?' and col5=\"?\" and col6='?''?''?'",
                comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?',
                    injector.inject(
                        "select 1, ?, 'test?test', \"test?test\", %sfoo.* from `foo` where col1=?"
                            + " and col2='test' and col3=? and col4='?' and col5=\"?\" and"
                            + " col6='?''?''?'",
                        comment))
                .sqlWithNamedParameters);

        assertEquals(
            injector.inject(
                "select * "
                    + "%sfrom foo "
                    + "where name=@p1 "
                    + "and col2 like @p2 "
                    + "and col3 > @p3",
                comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?',
                    injector.inject(
                        "select * "
                            + "%sfrom foo "
                            + "where name=? "
                            + "and col2 like ? "
                            + "and col3 > ?",
                        comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("select * " + "from foo " + "where id between @p1%s and @p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?',
                    injector.inject(
                        "select * " + "from foo " + "where id between ?%s and ?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject("select * " + "from foo " + "limit @p1 %s offset @p2", comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?',
                    injector.inject("select * " + "from foo " + "limit ? %s offset ?", comment))
                .sqlWithNamedParameters);
        assertEquals(
            injector.inject(
                "select * "
                    + "from foo "
                    + "where col1=@p1 "
                    + "and col2 like @p2 "
                    + " %s "
                    + "and col3 > @p3 "
                    + "and col4 < @p4 "
                    + "and col5 != @p5 "
                    + "and col6 not in (@p6, @p7, @p8) "
                    + "and col7 in (@p9, @p10, @p11) "
                    + "and col8 between @p12 and @p13",
                comment),
            parser.convertPositionalParametersToNamedParameters(
                    '?',
                    injector.inject(
                        "select * "
                            + "from foo "
                            + "where col1=? "
                            + "and col2 like ? "
                            + " %s "
                            + "and col3 > ? "
                            + "and col4 < ? "
                            + "and col5 != ? "
                            + "and col6 not in (?, ?, ?) "
                            + "and col7 in (?, ?, ?) "
                            + "and col8 between ? and ?",
                        comment))
                .sqlWithNamedParameters);
      }
    }
  }
}
