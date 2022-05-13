/*
 * Copyright 2022 Google LLC
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

import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.ExplainCommandConverter;
import org.junit.Test;

public class ExplainCommandConverterTest {
  @Test
  public void testConvert() {
    ExplainCommandConverter explainCommandConverter = new ExplainCommandConverter();
    assertEquals(
        "select * from table1", explainCommandConverter.convert("explain select * from table1"));
    assertEquals(
        "select    *   \t from table1",
        explainCommandConverter.convert("explain \tselect    *   \t from table1"));
    assertEquals(
        "select    *   \t from table1",
        explainCommandConverter.convert("EXPLAIN \tselect    *   \t from table1"));
    assertEquals(
        "select    *   \t from table1",
        explainCommandConverter.convert("ExplAIn \tselect    *   \t from table1"));
    assertEquals(
        "select    *   \t from table1",
        explainCommandConverter.convert("explain \n select    *   \t from table1"));
    assertEquals(
        "select    *   \t from table1",
        explainCommandConverter.convert("explain \n \t select    *   \t from table1"));
    assertEquals("foo", explainCommandConverter.convert("explain   foo"));
    assertEquals(null, explainCommandConverter.convert("explain"));

    assertEquals(
        "analyse select * from table1",
        explainCommandConverter.convert("explain analyse select * from table1"));
    assertEquals(
        "analyze \tselect    *   \t from table1",
        explainCommandConverter.convert("explain \t analyze \tselect    *   \t from table1"));
    assertEquals(
        "analyse \n select    *   \t from table1",
        explainCommandConverter.convert("explain \n analyse \n select    *   \t from table1"));
    assertEquals(
        "ANALYZE \n select    *   \t from table1",
        explainCommandConverter.convert("EXPLAIN \n ANALYZE \n select    *   \t from table1"));
    assertEquals(
        "aNALyzE \n select    *   \t from table1",
        explainCommandConverter.convert("ExPLaiN \n aNALyzE \n select    *   \t from table1"));
    assertEquals(
        "analyse \t select    *   \t from table1",
        explainCommandConverter.convert("explain \n analyse \t select    *   \t from table1"));
    assertEquals("analyse foo", explainCommandConverter.convert("explain  analyse foo"));
    assertEquals("analyse", explainCommandConverter.convert("explain analyse"));
  }
}
