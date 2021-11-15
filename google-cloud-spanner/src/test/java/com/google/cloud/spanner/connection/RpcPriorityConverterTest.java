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
    assertEquals(Priority.PRIORITY_HIGH, converter.convert("high"));
    assertEquals(Priority.PRIORITY_HIGH, converter.convert("HIGH"));
    assertEquals(Priority.PRIORITY_HIGH, converter.convert("High"));

    assertEquals(Priority.PRIORITY_MEDIUM, converter.convert("medium"));
    assertEquals(Priority.PRIORITY_MEDIUM, converter.convert("Medium"));

    assertEquals(Priority.PRIORITY_LOW, converter.convert("Low"));

    assertNull(converter.convert(""));
    assertNull(converter.convert(" "));
    assertNull(converter.convert("random string"));
    assertEquals(Priority.PRIORITY_UNSPECIFIED, converter.convert("NULL"));
  }
}
