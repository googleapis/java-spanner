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

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class UpdateDatabaseDropProtectionSampleIT extends SampleTestBase {

  @Test
  public void testUpdateDatabaseDropProtection() throws Exception {
    // Create database
    final String databaseId = idGenerator.generateDatabaseId();
    databaseAdminClient.createDatabase(instanceId, databaseId, Collections.emptyList())
        .get(5, TimeUnit.MINUTES);

    // Runs sample
    final String out = SampleRunner.runSample(() -> UpdateDatabaseDropProtectionSample
        .updateDatabaseDropProtection(projectId, instanceId, databaseId)
    );

    assertTrue(
        "Expected that drop protection would have been enabled. Output received was " + out,
        out.contains("updated to true")
    );
    assertTrue(
        "Expected that drop protection would have been disabled. Output received was " + out,
        out.contains("updated to false")
    );
  }
}