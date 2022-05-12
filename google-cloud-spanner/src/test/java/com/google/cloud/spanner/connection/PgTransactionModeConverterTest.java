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
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.PgTransactionModeConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PgTransactionModeConverterTest {
  @Test
  public void testConvert() throws CompileException {
    String allowedValues =
        ReadOnlyStalenessConverterTest.getAllowedValues(
            PgTransactionModeConverter.class, Dialect.POSTGRESQL);

    assertNotNull(allowedValues);
    PgTransactionModeConverter converter = new PgTransactionModeConverter(allowedValues);

    assertEquals(PgTransactionMode.READ_WRITE_TRANSACTION, converter.convert("read write"));
    assertEquals(PgTransactionMode.READ_WRITE_TRANSACTION, converter.convert("READ WRITE"));
    assertEquals(PgTransactionMode.READ_WRITE_TRANSACTION, converter.convert("Read Write"));
    assertEquals(PgTransactionMode.READ_WRITE_TRANSACTION, converter.convert("read   write"));
    assertEquals(PgTransactionMode.READ_WRITE_TRANSACTION, converter.convert("READ\nWRITE"));
    assertEquals(PgTransactionMode.READ_WRITE_TRANSACTION, converter.convert("Read\tWrite"));

    assertEquals(PgTransactionMode.READ_ONLY_TRANSACTION, converter.convert("read only"));
    assertEquals(PgTransactionMode.READ_ONLY_TRANSACTION, converter.convert("READ ONLY"));
    assertEquals(PgTransactionMode.READ_ONLY_TRANSACTION, converter.convert("Read Only"));
    assertEquals(PgTransactionMode.READ_ONLY_TRANSACTION, converter.convert("read   only"));
    assertEquals(PgTransactionMode.READ_ONLY_TRANSACTION, converter.convert("READ\nONLY"));
    assertEquals(PgTransactionMode.READ_ONLY_TRANSACTION, converter.convert("Read\tOnly"));

    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_DEFAULT, converter.convert("isolation level default"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_DEFAULT, converter.convert("ISOLATION LEVEL DEFAULT"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_DEFAULT, converter.convert("Isolation Level Default"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_DEFAULT,
        converter.convert("isolation    level  default"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_DEFAULT, converter.convert("ISOLATION\nLEVEL\nDEFAULT"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_DEFAULT, converter.convert("Isolation\tLevel\tDefault"));

    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_SERIALIZABLE,
        converter.convert("isolation level serializable"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_SERIALIZABLE,
        converter.convert("ISOLATION LEVEL SERIALIZABLE"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_SERIALIZABLE,
        converter.convert("Isolation Level Serializable"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_SERIALIZABLE,
        converter.convert("isolation    level  serializable"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_SERIALIZABLE,
        converter.convert("ISOLATION\nLEVEL\nSERIALIZABLE"));
    assertEquals(
        PgTransactionMode.ISOLATION_LEVEL_SERIALIZABLE,
        converter.convert("Isolation\tLevel\tSerializable"));

    assertNull(converter.convert(""));
    assertNull(converter.convert(" "));
    assertNull(converter.convert("random string"));
    assertNull(converter.convert("read_write"));
    assertNull(converter.convert("READ_WRITE"));
    assertNull(converter.convert("read_only"));
    assertNull(converter.convert("Read_Only"));
    assertNull(converter.convert("READ_ONLY"));

    assertNull(converter.convert("isolation_level default"));
    assertNull(converter.convert("isolationlevel default"));
    assertNull(converter.convert("isolation level read committed"));
    assertNull(converter.convert("isolation level "));
    assertNull(converter.convert("isolation level_default"));
  }
}
