/*
 * Copyright 2017 Google LLC
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

import com.google.common.base.Preconditions;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.Serializable;
import java.util.Objects;

public class Json implements Comparable<Json>, Serializable {
  public String value;

  /**
   * Representation of a JSON object.
   *
   * @param value JSON text
   * @throws NullPointerException if {@code value} is null
   * @throws JsonParseException if {@code value} is not valid JSON
   */
  public Json(String value) {
    Preconditions.checkNotNull(value, "JSON cannot contain NULL value.");
    // Attempts to parse the JSON String, throws JsonParseException if it is not valid JSON
    JsonParser.parseString(value);
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Json)) {
      return false;
    }
    Json that = (Json) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public int compareTo(Json other) {
    if (value == null) {
      if (other.value == null) {
        return 0;
      }
    }
    return value.compareTo(other.value);
  }
}
