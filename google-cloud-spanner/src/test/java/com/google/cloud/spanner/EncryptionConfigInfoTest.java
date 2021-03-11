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

import com.google.spanner.admin.database.v1.EncryptionConfig;
import org.junit.Test;

/** Unit tests for {@link com.google.cloud.spanner.EncryptionConfigInfo} */
public class EncryptionConfigInfoTest {

  private static final String KMS_KEY_NAME = "kms-key-name";

  @Test
  public void testEncryptionConfigInfoOfKey() {
    final EncryptionConfigInfo encryptionConfigInfo = EncryptionConfigInfo.ofKey(KMS_KEY_NAME);

    assertEquals(KMS_KEY_NAME, encryptionConfigInfo.getKmsKeyName());
  }

  @Test
  public void testEncryptionConfigInfoFromProtoDefaultInstance() {
    final EncryptionConfigInfo encryptionConfigInfo =
        EncryptionConfigInfo.fromProtoOrNullIfDefaultInstance(
            EncryptionConfig.getDefaultInstance());

    assertNull(encryptionConfigInfo);
  }

  @Test
  public void testEncryptionConfigInfoFromProto() {
    final EncryptionConfigInfo actualEncryptionConfigInfo =
        EncryptionConfigInfo.fromProtoOrNullIfDefaultInstance(
            EncryptionConfig.newBuilder().setKmsKeyName(KMS_KEY_NAME).build());
    final EncryptionConfigInfo expectedEncryptionConfigInfo =
        EncryptionConfigInfo.ofKey(KMS_KEY_NAME);

    assertEquals(expectedEncryptionConfigInfo, actualEncryptionConfigInfo);
  }

  @Test
  public void testEncryptionConfigInfoToProto() {
    final EncryptionConfig actualEncryptionConfig =
        EncryptionConfigInfo.ofKey(KMS_KEY_NAME).toProto();
    final EncryptionConfig expectedEncryptionConfig =
        EncryptionConfig.newBuilder().setKmsKeyName(KMS_KEY_NAME).build();

    assertEquals(expectedEncryptionConfig, actualEncryptionConfig);
  }

  @Test
  public void testEqualsAndHashCode() {
    final EncryptionConfigInfo encryptionConfigInfo1 = EncryptionConfigInfo.ofKey(KMS_KEY_NAME);
    final EncryptionConfigInfo encryptionConfigInfo2 = EncryptionConfigInfo.ofKey(KMS_KEY_NAME);

    assertEquals(encryptionConfigInfo1, encryptionConfigInfo2);
    assertEquals(encryptionConfigInfo1.hashCode(), encryptionConfigInfo2.hashCode());
  }
}
