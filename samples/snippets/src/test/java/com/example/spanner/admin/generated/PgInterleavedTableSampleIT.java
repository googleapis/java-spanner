/*
 * Copyright 2022 Google LLC
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

package com.example.spanner.admin.generated;

import static org.junit.Assert.assertTrue;

import com.example.spanner.SampleRunner;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.DatabaseDialect;
import com.google.spanner.admin.database.v1.InstanceName;
import org.junit.Test;

public class PgInterleavedTableSampleIT extends SampleTestBaseV2 {

  @Test
  public void testPgInterleavedTableSample() throws Exception {
    final String databaseId = idGenerator.generateDatabaseId();
    final CreateDatabaseRequest request =
        CreateDatabaseRequest.newBuilder()
            .setCreateStatement(getCreateDatabaseStatement(databaseId, DatabaseDialect.POSTGRESQL))
            .setParent(InstanceName.of(projectId, instanceId).toString())
            .setDatabaseDialect(DatabaseDialect.POSTGRESQL).build();
    databaseAdminClient.createDatabaseAsync(request).get();

    final String out =
        SampleRunner.runSample(
            () -> PgInterleavedTableSample.pgInterleavedTable(projectId, instanceId, databaseId));
    assertTrue(out.contains("Created interleaved table hierarchy using PostgreSQL dialect"));
  }
}