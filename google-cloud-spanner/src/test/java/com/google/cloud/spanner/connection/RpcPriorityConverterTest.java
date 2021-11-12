/*
 * Copyright 2021 Google LLC
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
import static org.junit.Assert.assertNull;

import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.RpcPriorityConverter;
import com.google.spanner.v1.RequestOptions.Priority;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RpcPriorityConverterTest {

  @Test
  public void testConvert() throws CompileException {
    String allowedValues = "'(HIGH|MEDIUM|LOW|NULL)'";
    RpcPriorityConverter converter =
        new ClientSideStatementValueConverters.RpcPriorityConverter(allowedValues);
    assertEquals(converter.convert("high"), Priority.PRIORITY_HIGH);
    assertEquals(converter.convert("HIGH"), Priority.PRIORITY_HIGH);
    assertEquals(converter.convert("High"), Priority.PRIORITY_HIGH);

    assertEquals(converter.convert("medium"), Priority.PRIORITY_MEDIUM);
    assertEquals(converter.convert("Low"), Priority.PRIORITY_LOW);
    assertEquals(converter.convert("Medium"), Priority.PRIORITY_MEDIUM);

    assertNull(converter.convert(""));
    assertNull(converter.convert(" "));
    assertNull(converter.convert("random string"));
    assertEquals(converter.convert("NULL"), Priority.PRIORITY_UNSPECIFIED);
  }
}
