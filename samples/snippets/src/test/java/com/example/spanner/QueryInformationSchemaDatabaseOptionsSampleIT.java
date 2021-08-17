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

import com.google.cloud.spanner.InstanceConfig;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;

public class QueryInformationSchemaDatabaseOptionsSampleIT extends SampleTestBase {

  @Ignore("Skipping until we have a MR instance to run this on")
  @Test
  public void testQueryInformationSchemaDatabaseOptions() throws Exception {
    // Finds a possible new leader option
    final InstanceConfig config = instanceAdminClient.getInstanceConfig(instanceConfigName);
    assertTrue(
        "Expected instance config " + instanceConfigName + " to have at least one leader option",
        config.getLeaderOptions().size() > 0);
    final String defaultLeader = config.getLeaderOptions().get(0);

    // Creates database
    final String databaseId = idGenerator.generateDatabaseId();
    databaseAdminClient
        .createDatabase(
            instanceId,
            databaseId,
            Arrays.asList(
                "CREATE TABLE Singers (Id INT64 NOT NULL) PRIMARY KEY (Id)",
                "ALTER DATABASE `"
                    + databaseId
                    + "` SET OPTIONS ( default_leader = '"
                    + defaultLeader
                    + "')"))
        .get(5, TimeUnit.MINUTES);

    // Runs sample
    final String out =
        SampleRunner.runSample(
            () ->
                QueryInformationSchemaDatabaseOptionsSample.queryInformationSchemaDatabaseOptions(
                    projectId, instanceId, databaseId));

    assertTrue(
        "Expected to have retrieved default_leader for "
            + databaseId
            + " as "
            + defaultLeader
            + "."
            + " Output received was "
            + out,
        out.contains(
            "The default_leader for projects/"
                + projectId
                + "/instances/"
                + instanceId
                + "/databases/"
                + databaseId
                + " is "
                + defaultLeader));
  }
}
