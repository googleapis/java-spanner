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

import com.google.spanner.admin.database.v1.EncryptionConfig;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.units.qual.C;

/**
 * The data is encrypted with a key provided by the customer.
 */
public class CustomerManagedEncryption implements BackupEncryptionConfig, RestoreEncryptionConfig {

  private final String kmsKeyName;
  private final List<String> kmsKeyNames;

  CustomerManagedEncryption(String kmsKeyName) {
    this.kmsKeyName = kmsKeyName;
    this.kmsKeyNames = null;
  }

  CustomerManagedEncryption(List<String> kmsKeyNames) {
    this.kmsKeyNames = kmsKeyNames;
    this.kmsKeyName = null;
  }

  public String getKmsKeyName() {
    return kmsKeyName;
  }

  public List<String> getKmsKeyNames() {
    return kmsKeyNames;
  }

  /**
   * Returns a {@link CustomerManagedEncryption} instance from the given proto, or <code>null</code>
   * if the given proto is the default proto instance (i.e. there is no encryption config).
   */
  public static CustomerManagedEncryption fromProtoOrNull(EncryptionConfig proto) {
    return proto.equals(EncryptionConfig.getDefaultInstance())
        ? null
        : !proto.getKmsKeyName().isEmpty() ? new CustomerManagedEncryption(proto.getKmsKeyName())
            : new CustomerManagedEncryption(proto.getKmsKeyNamesList());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CustomerManagedEncryption that = (CustomerManagedEncryption) o;
    return Objects.equals(kmsKeyName, that.kmsKeyName) && Objects.equals(kmsKeyNames, that.kmsKeyNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kmsKeyName);
  }

  @Override
  public String toString() {
    return "CustomerManagedEncryption{" + "kmsKeyName='" + kmsKeyName + '\'' + '}';
  }
}
