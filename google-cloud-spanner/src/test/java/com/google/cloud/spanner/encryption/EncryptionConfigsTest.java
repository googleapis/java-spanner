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
package com.google.cloud.spanner.encryption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/** Unit tests for {@link EncryptionConfigs} */
public class EncryptionConfigsTest {

  @Test
  public void testCustomerManagedEncryption() {
    final CustomerManagedEncryption expected = new CustomerManagedEncryption("kms-key-name");

    final CustomerManagedEncryption actual =
        EncryptionConfigs.customerManagedEncryption("kms-key-name");

    assertEquals(expected, actual);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCustomerManagedEncryptionNullKeyName() {
    EncryptionConfigs.customerManagedEncryption(null);
  }

  @Test
  public void testGoogleDefaultEncryption() {
    assertSame(EncryptionConfigs.googleDefaultEncryption(), GoogleDefaultEncryption.INSTANCE);
  }

  @Test
  public void testUseDatabaseEncryption() {
    assertSame(EncryptionConfigs.useDatabaseEncryption(), UseDatabaseEncryption.INSTANCE);
  }

  @Test
  public void testUseBackupEncryption() {
    assertSame(EncryptionConfigs.useBackupEncryption(), UseBackupEncryption.INSTANCE);
  }
}
