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

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.ExplainCommandConverter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExplainCommandConverterTest {
  @Parameter public Dialect dialect;

  @Parameters(name = "dialect = {0}")
  public static Object[] data() {
    return Dialect.values();
  }

  @Test
  public void testConvert() throws CompileException{
    ExplainCommandConverter explainCommandConverter = new ExplainCommandConverter();
    Assert.assertEquals("select * from table1",explainCommandConverter.convert("explain select * from table1"));
    Assert.assertEquals("select    *   \t from table1",explainCommandConverter.convert("explain \tselect    *   \t from table1"));
    Assert.assertEquals("select    *   \t from table1",explainCommandConverter.convert("explain \n select    *   \t from table1"));
    Assert.assertEquals("select    *   \t from table1",explainCommandConverter.convert("explain \n \t select    *   \t from table1"));
    Assert.assertEquals("foo",explainCommandConverter.convert("explain   foo"));
    Assert.assertEquals(null, explainCommandConverter.convert("explain"));

    Assert.assertEquals("analyse select * from table1",explainCommandConverter.convert("explain analyse select * from table1"));
    Assert.assertEquals("analyse \tselect    *   \t from table1",explainCommandConverter.convert("explain \t analyse \tselect    *   \t from table1"));
    Assert.assertEquals("analyse \n select    *   \t from table1",explainCommandConverter.convert("explain \n analyse \n select    *   \t from table1"));
    Assert.assertEquals("analyse \t select    *   \t from table1",explainCommandConverter.convert("explain \n analyse \t select    *   \t from table1"));
    Assert.assertEquals("analyse foo",explainCommandConverter.convert("explain  analyse foo"));
    Assert.assertEquals("analyse", explainCommandConverter.convert("explain analyse"));

  }


}
