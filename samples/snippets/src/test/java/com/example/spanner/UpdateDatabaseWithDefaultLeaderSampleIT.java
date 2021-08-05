package com.example.spanner;

import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.InstanceConfig;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class UpdateDatabaseWithDefaultLeaderSampleIT extends SampleTestBase {

  @Test
  public void testUpdateDatabaseWithDefaultLeader() throws Exception {
    // Create database
    final String databaseId = idGenerator.generateDatabaseId();
    final Database createdDatabase = databaseAdminClient
        .createDatabase(instanceId, databaseId, Collections.emptyList())
        .get(5, TimeUnit.MINUTES);
    final String defaultLeader = createdDatabase.getDefaultLeader();

    // Finds a possible new leader option
    final InstanceConfig instanceConfig = instanceAdminClient.getInstanceConfig(instanceConfigName);
    final String newLeader = instanceConfig
        .getLeaderOptions()
        .stream()
        .filter(leader -> !leader.equals(defaultLeader))
        .findFirst()
        .orElseThrow(() ->
            new RuntimeException("Expected to find a leader option different than " + defaultLeader)
        );

    // Runs sample
    final String out = SampleRunner.runSample(() -> UpdateDatabaseWithDefaultLeaderSample
        .updateDatabaseWithDefaultLeader(projectId, instanceId, databaseId, newLeader)
    );

    assertTrue(
        "Expected that database new leader would had been updated to " + newLeader + "."
            + " Output received was " + out,
        out.contains("Updated default leader to " + newLeader)
    );
  }
}
