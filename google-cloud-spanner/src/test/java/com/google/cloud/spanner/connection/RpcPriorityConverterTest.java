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

import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.RpcPriorityConverter;
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
    assertEquals(converter.convert("high"), RpcPriority.HIGH);
    assertEquals(converter.convert("HIGH"), RpcPriority.HIGH);
    assertEquals(converter.convert("High"), RpcPriority.HIGH);

    assertEquals(converter.convert("medium"), RpcPriority.MEDIUM);
    assertEquals(converter.convert("Low"), RpcPriority.LOW);
    assertEquals(converter.convert("Medium"), RpcPriority.MEDIUM);

    assertNull(converter.convert(""));
    assertNull(converter.convert(" "));
    assertNull(converter.convert("random string"));
    assertEquals(converter.convert("NULL"), RpcPriority.UNSPECIFIED);
  }
}
