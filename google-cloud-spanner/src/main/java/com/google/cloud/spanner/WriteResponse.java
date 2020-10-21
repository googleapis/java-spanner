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

import com.google.cloud.Timestamp;
import java.util.Objects;

/** Represents a response from a write / writeAtLeast once operation. */
public class WriteResponse {

  private final Timestamp commitTimestamp;

  public WriteResponse(Timestamp commitTimestamp) {
    this.commitTimestamp = commitTimestamp;
  }

  /** Returns a {@link Timestamp} representing the commit time of the write operation. */
  public Timestamp getCommitTimestamp() {
    return commitTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WriteResponse that = (WriteResponse) o;
    return Objects.equals(commitTimestamp, that.commitTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commitTimestamp);
  }
}
