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
package com.google.cloud.spanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Unit tests for {@link EncryptionConfig} */
public class EncryptionConfigTest {

  private static final String KMS_KEY_NAME = "kms-key-name";

  @Test
  public void testEncryptionConfigOfKey() {
    final EncryptionConfig encryptionConfig = EncryptionConfig.ofKey(KMS_KEY_NAME);

    assertEquals(KMS_KEY_NAME, encryptionConfig.getKmsKeyName());
  }

  @Test
  public void testEncryptionConfigFromProtoDefaultInstance() {
    final EncryptionConfig encryptionConfig =
        EncryptionConfig.fromProtoOrNullIfDefaultInstance(
            com.google.spanner.admin.database.v1.EncryptionConfig.getDefaultInstance());

    assertNull(encryptionConfig);
  }

  @Test
  public void testEncryptionConfigFromProto() {
    final EncryptionConfig actualEncryptionConfig =
        EncryptionConfig.fromProtoOrNullIfDefaultInstance(
            com.google.spanner.admin.database.v1.EncryptionConfig.newBuilder()
                .setKmsKeyName(KMS_KEY_NAME)
                .build());
    final EncryptionConfig expectedEncryptionConfig = EncryptionConfig.ofKey(KMS_KEY_NAME);

    assertEquals(expectedEncryptionConfig, actualEncryptionConfig);
  }

  @Test
  public void testEncryptionConfigToProto() {
    final com.google.spanner.admin.database.v1.EncryptionConfig actualEncryptionConfig =
        EncryptionConfig.ofKey(KMS_KEY_NAME).toProto();
    final com.google.spanner.admin.database.v1.EncryptionConfig expectedEncryptionConfig =
        com.google.spanner.admin.database.v1.EncryptionConfig.newBuilder()
            .setKmsKeyName(KMS_KEY_NAME)
            .build();

    assertEquals(expectedEncryptionConfig, actualEncryptionConfig);
  }

  @Test
  public void testEqualsAndHashCode() {
    final EncryptionConfig encryptionConfig1 = EncryptionConfig.ofKey(KMS_KEY_NAME);
    final EncryptionConfig encryptionConfig2 = EncryptionConfig.ofKey(KMS_KEY_NAME);

    assertEquals(encryptionConfig1, encryptionConfig2);
    assertEquals(encryptionConfig1.hashCode(), encryptionConfig2.hashCode());
  }
}
