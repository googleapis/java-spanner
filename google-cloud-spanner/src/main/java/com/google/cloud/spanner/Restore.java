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

import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;

public class Restore {

  public static class Builder {

    private final BackupId source;
    private final DatabaseId destination;
    private EncryptionConfig encryptionConfig;

    public Builder(BackupId source, DatabaseId destination) {
      this.source = source;
      this.destination = destination;
    }

    public Builder setEncryptionConfig(EncryptionConfig encryptionConfig) {
      this.encryptionConfig = encryptionConfig;
      return this;
    }

    public Restore build() {
      return new Restore(this);
    }
  }

  private final BackupId source;
  private final DatabaseId destination;
  private final EncryptionConfig encryptionConfig;

  Restore(Builder builder) {
    this(builder.source, builder.destination, builder.encryptionConfig);
  }

  @VisibleForTesting
  Restore(BackupId source, DatabaseId destination, EncryptionConfig encryptionConfig) {
    this.source = source;
    this.destination = destination;
    this.encryptionConfig = encryptionConfig;
  }

  public BackupId getSource() {
    return source;
  }

  public DatabaseId getDestination() {
    return destination;
  }

  public EncryptionConfig getEncryptionConfig() {
    return encryptionConfig;
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
        && Objects.equals(encryptionConfig, restore.encryptionConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination, encryptionConfig);
  }

  @Override
  public String toString() {
    return String.format(
        "Restore[%s, %s, %s]", source.getName(), destination.getName(), encryptionConfig);
  }
}
