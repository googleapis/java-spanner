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

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.Dialect;
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
            .skipCommentsAndLiterals(sql, currentIndex, null);
    return sql.substring(currentIndex, position);
  }

  @Test
  public void testSkipCommentsAndLiterals() {
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
}
