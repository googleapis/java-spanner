/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

public interface ExtendedSpanner extends Spanner {
  /**
   * Returns a {@code DatabaseClient} for the given database and given client id. It uses a pool of
   * sessions to talk to the database.
   * <!--SNIPPET get_db_client-->
   *
   * <pre>{@code
   * SpannerOptions options = SpannerOptions.newBuilder().build();
   * Spanner spanner = options.getService();
   * final String project = "test-project";
   * final String instance = "test-instance";
   * final String database = "example-db";
   * final String client_id = "client_id"
   * DatabaseId db =
   *     DatabaseId.of(project, instance, database);
   *
   * DatabaseClient dbClient = spanner.getDatabaseClient(db, client_id);
   * }</pre>
   *
   * <!--SNIPPET get_db_client-->
   */
  default DatabaseClient getDatabaseClient(DatabaseId db, String clientId) {
    throw new UnsupportedOperationException(
        "getDatabaseClient with clientId is not supported by this default implementation.");
  }
}
