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

import com.google.spanner.admin.database.v1.EncryptionConfig;
import java.util.Objects;

/** Represents the encryption information of a Cloud Spanner database. */
public class EncryptionConfigInfo {
  private final String kmsKeyName;

  public static EncryptionConfigInfo ofKey(String kmsKeyName) {
    return new EncryptionConfigInfo(kmsKeyName);
  }

  private EncryptionConfigInfo(String kmsKeyName) {
    this.kmsKeyName = kmsKeyName;
  }

  private EncryptionConfigInfo(EncryptionConfig proto) {
    this.kmsKeyName = proto.getKmsKeyName();
  }

  public String getKmsKeyName() {
    return kmsKeyName;
  }

  /**
   * Returns a {@link EncryptionConfigInfo} instance from the given proto, or <code>null</code> if
   * the given proto is the default proto instance (i.e. there is no encryption config info).
   */
  static EncryptionConfigInfo fromProtoOrNullIfDefaultInstance(
      com.google.spanner.admin.database.v1.EncryptionConfig proto) {
    return proto.equals(com.google.spanner.admin.database.v1.EncryptionConfig.getDefaultInstance())
        ? null
        : new EncryptionConfigInfo(proto);
  }

  public EncryptionConfig toProto() {
    return EncryptionConfig.newBuilder().setKmsKeyName(kmsKeyName).build();
  }

  public int hashCode() {
    return Objects.hash(kmsKeyName);
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof EncryptionConfigInfo)) {
      return false;
    }
    EncryptionConfigInfo other = (EncryptionConfigInfo) o;
    return Objects.equals(kmsKeyName, other.kmsKeyName);
  }

  public String toString() {
    return String.format("EncryptionConfigInfo[kmsKeyName=%s]", kmsKeyName);
  }
}
