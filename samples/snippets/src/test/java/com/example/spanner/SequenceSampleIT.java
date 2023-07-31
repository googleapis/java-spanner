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

package com.example.spanner;

import static com.example.spanner.SampleRunner.runSample;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.DatabaseId;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for DML Returning samples for GoogleStandardSql dialect. */
@RunWith(JUnit4.class)
public class SequenceSampleIT extends SampleTestBase {

  private static DatabaseId databaseId;

  @BeforeClass
  public static void createTestDatabase() throws Exception {
    final String database = idGenerator.generateDatabaseId();
    databaseAdminClient
        .createDatabase(instanceId, database, Collections.emptyList())
        .get(10, TimeUnit.MINUTES);
    databaseId = DatabaseId.of(projectId, instanceId, database);
  }

  @Test
  public void createSequence() throws Exception {
    final String out =
        runSample(
            () ->
                CreateSequenceSample.createSequence(
                    projectId, instanceId, databaseId.getDatabase()));
    assertTrue(
        out.contains(
            "Created Seq sequence and Customers table, where the key column CustomerId uses the sequence as a default value"));
    assertEquals(out.split("Inserted customer record with CustomerId", -1).length - 1, 3);
    assertTrue(out.contains("Number of customer records inserted is: 3"));
  }

  @Test
  public void alterSequence() throws Exception {
    final String out =
        runSample(
            () ->
                AlterSequenceSample.alterSequence(projectId, instanceId, databaseId.getDatabase()));
    assertTrue(
        out.contains("Altered Seq sequence to skip an inclusive range between 1000 and 5000000"));
    assertEquals(out.split("Inserted customer record with CustomerId", -1).length - 1, 3);
    assertTrue(out.contains("Number of customer records inserted is: 3"));
  }

  @Test
  public void dropSequence() throws Exception {
    final String out =
        runSample(
            () -> DropSequenceSample.dropSequence(projectId, instanceId, databaseId.getDatabase()));
    assertTrue(
        out.contains(
            "Altered Customers table to drop DEFAULT from CustomerId column and dropped the Seq sequence"));
  }
}
