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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.PgDurationConverter;
import com.google.protobuf.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PgDurationConverterTest {
  @Test
  public void testConvert() throws CompileException {
    String allowedValues =
        ReadOnlyStalenessConverterTest.getAllowedValues(
            PgDurationConverter.class, Dialect.POSTGRESQL);
    assertNotNull(allowedValues);
    PgDurationConverter converter = new PgDurationConverter(allowedValues);

    assertEquals(Duration.newBuilder().setNanos(1000000).build(), converter.convert("1"));
    assertEquals(Duration.newBuilder().setSeconds(1L).build(), converter.convert("1000"));
    assertEquals(
        Duration.newBuilder().setSeconds(1L).setNanos(1000000).build(), converter.convert("1001"));

    assertEquals(
        Duration.newBuilder().setNanos((int) TimeUnit.MILLISECONDS.toNanos(100L)).build(),
        converter.convert("'100ms'"));
    assertNull(converter.convert("'0ms'"));
    assertNull(converter.convert("'-100ms'"));
    assertEquals(
        Duration.newBuilder().setSeconds(315576000000L).build(),
        converter.convert("'315576000000000ms'"));
    assertEquals(Duration.newBuilder().setSeconds(1L).build(), converter.convert("'1s'"));
    assertEquals(
        Duration.newBuilder()
            .setSeconds(1L)
            .setNanos((int) TimeUnit.MILLISECONDS.toNanos(1L))
            .build(),
        converter.convert("'1001ms'"));

    assertEquals(Duration.newBuilder().setNanos(1).build(), converter.convert("'1ns'"));
    assertEquals(Duration.newBuilder().setNanos(1000).build(), converter.convert("'1us'"));
    assertEquals(Duration.newBuilder().setNanos(1000000).build(), converter.convert("'1ms'"));
    assertEquals(
        Duration.newBuilder().setNanos(999999999).build(), converter.convert("'999999999ns'"));
    assertEquals(Duration.newBuilder().setSeconds(1L).build(), converter.convert("'1s'"));

    assertNull(converter.convert("''"));
    assertNull(converter.convert("' '"));
    assertNull(converter.convert("'random string'"));

    assertEquals(Duration.getDefaultInstance(), converter.convert("default"));
    assertEquals(Duration.getDefaultInstance(), converter.convert("DEFAULT"));
    assertEquals(Duration.getDefaultInstance(), converter.convert("Default"));
    assertNull(converter.convert("'default'"));
    assertNull(converter.convert("'DEFAULT'"));
    assertNull(converter.convert("'Default'"));
  }
}
