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

package com.google.cloud.spanner;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.BatchClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

public class Main {

  private static final String PROJECT = "appdev-soda-spanner-staging";
  private static final String INSTANCE = "thiagotnunes-test-instance";
  private static final String DATABASE = "example-db";

  public static void main(String[] args) {
    final SpannerOptions.Builder optionsBuilder = SpannerOptions.newBuilder();
    final SpannerOptions options = optionsBuilder.build();
    final Spanner spanner = options.getService();
    final DatabaseId id = DatabaseId.of(PROJECT, INSTANCE, DATABASE);
    final DatabaseClient databaseClient = spanner.getDatabaseClient(id);
    final DatabaseAdminClient databaseAdminClient = spanner.getDatabaseAdminClient();

    final ResultSet rs = databaseClient.singleUse().executeQuery(Statement.of("SELECT 1"));
    rs.next();
    System.out.println(rs.getLong(0));
  }
}

