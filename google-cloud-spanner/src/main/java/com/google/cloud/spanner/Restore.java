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

public class Restore {

  public static class Builder {

    private final BackupId source;
    private final DatabaseId destination;
    private EncryptionConfigInfo encryptionConfigInfo;

    public Builder(BackupId source, DatabaseId destination) {
      this.source = source;
      this.destination = destination;
    }

    public Builder setEncryptionConfigInfo(EncryptionConfigInfo encryptionConfigInfo) {
      this.encryptionConfigInfo = encryptionConfigInfo;
      return this;
    }

    public Restore build() {
      return new Restore(this);
    }
  }

  private final BackupId source;
  private final DatabaseId destination;
  private final EncryptionConfigInfo encryptionConfigInfo;

  Restore(Builder builder) {
    this.source = builder.source;
    this.destination = builder.destination;
    this.encryptionConfigInfo = builder.encryptionConfigInfo;
  }

  public BackupId getSource() {
    return source;
  }

  public DatabaseId getDestination() {
    return destination;
  }

  public EncryptionConfigInfo getEncryptionConfigInfo() {
    return encryptionConfigInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Restore restore = (Restore) o;
    return Objects.equals(source, restore.source)
        && Objects.equals(destination, restore.destination)
        && Objects.equals(encryptionConfigInfo, restore.encryptionConfigInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination, encryptionConfigInfo);
  }

  @Override
  public String toString() {
    return String.format(
        "Restore[%s, %s, %s]", source.getName(), destination.getName(), encryptionConfigInfo);
  }
}
