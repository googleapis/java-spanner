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

import com.google.api.client.util.Preconditions;
import java.util.List;

/** Encryption configuration factory. */
public class EncryptionConfigs {

  /** Returns a customer managed encryption configuration for the given key. */
  public static CustomerManagedEncryption customerManagedEncryption(String kmsKeyName) {
    Preconditions.checkArgument(
        kmsKeyName != null, "Customer managed encryption key name must not be null");
    return new CustomerManagedEncryption(kmsKeyName);
  }

  public static CustomerManagedEncryption customerManagedEncryptionMultiRegion(List<String> kmsKeyNames) {
    Preconditions.checkArgument(
        kmsKeyNames != null, "Customer managed encryption key names must not be null");
    Preconditions.checkArgument(
        kmsKeyNames.isEmpty() != true, "Customer managed encryption key names must not be empty");
    return new CustomerManagedEncryption(kmsKeyNames);
  }

  /** Returns google default encryption configuration. */
  public static GoogleDefaultEncryption googleDefaultEncryption() {
    return GoogleDefaultEncryption.INSTANCE;
  }

  /** Returns use database encryption configuration. */
  public static UseDatabaseEncryption useDatabaseEncryption() {
    return UseDatabaseEncryption.INSTANCE;
  }

  /** Returns use backup encryption configuration. */
  public static UseBackupEncryption useBackupEncryption() {
    return UseBackupEncryption.INSTANCE;
  }
}
