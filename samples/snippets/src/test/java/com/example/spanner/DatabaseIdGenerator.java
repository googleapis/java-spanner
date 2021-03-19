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

package com.example.spanner;

import java.util.UUID;

public class DatabaseIdGenerator {

  private static final int DATABASE_NAME_MAX_SIZE = 30;
  private static final String BASE_DATABASE_ID = System.getProperty(
      "spanner.sample.database",
      "sampletest"
  );

  static String generateDatabaseId() {
    return (
        BASE_DATABASE_ID
            + "-"
            + UUID.randomUUID().toString().replaceAll("-", "")
    ).substring(0, DATABASE_NAME_MAX_SIZE);
  }
}
