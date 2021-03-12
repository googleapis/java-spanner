/*
 * Copyright 2020 Google LLC
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

import java.util.Objects;

/** Represents the encryption configuration for of a Cloud Spanner resource. */
public class EncryptionConfig {
  private final String kmsKeyName;

  public static EncryptionConfig ofKey(String kmsKeyName) {
    return new EncryptionConfig(kmsKeyName);
  }

  private EncryptionConfig(String kmsKeyName) {
    this.kmsKeyName = kmsKeyName;
  }

  private EncryptionConfig(com.google.spanner.admin.database.v1.EncryptionConfig proto) {
    this.kmsKeyName = proto.getKmsKeyName();
  }

  public String getKmsKeyName() {
    return kmsKeyName;
  }

  /**
   * Returns a {@link EncryptionConfig} instance from the given proto, or <code>null</code> if the
   * given proto is the default proto instance (i.e. there is no encryption config info).
   */
  static EncryptionConfig fromProtoOrNullIfDefaultInstance(
      com.google.spanner.admin.database.v1.EncryptionConfig proto) {
    return proto.equals(com.google.spanner.admin.database.v1.EncryptionConfig.getDefaultInstance())
        ? null
        : new EncryptionConfig(proto);
  }

  public com.google.spanner.admin.database.v1.EncryptionConfig toProto() {
    return com.google.spanner.admin.database.v1.EncryptionConfig.newBuilder()
        .setKmsKeyName(kmsKeyName)
        .build();
  }

  public int hashCode() {
    return Objects.hash(kmsKeyName);
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof EncryptionConfig)) {
      return false;
    }
    EncryptionConfig other = (EncryptionConfig) o;
    return Objects.equals(kmsKeyName, other.kmsKeyName);
  }

  public String toString() {
    return String.format("EncryptionConfig[kmsKeyName=%s]", kmsKeyName);
  }
}
