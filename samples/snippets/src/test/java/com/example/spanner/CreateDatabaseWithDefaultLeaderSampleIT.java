package com.example.spanner;

import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.InstanceConfig;
import org.junit.Test;

public class CreateDatabaseWithDefaultLeaderSampleIT extends SampleTestBase {

  @Test
  public void testCreateDatabaseWithDefaultLeader() throws Exception {
    final String databaseId = idGenerator.generateDatabaseId();

    // Finds possible default leader
    final InstanceConfig config = instanceAdminClient.getInstanceConfig(instanceConfigName);
    assertTrue(
        "Expected instance config " + instanceConfigName + " to have at least one leader option",
        config.getLeaderOptions().size() > 0
    );
    final String defaultLeader = config.getLeaderOptions().get(0);

    // Runs sample
    final String out = SampleRunner.runSample(() -> CreateDatabaseWithDefaultLeaderSample
        .createDatabaseWithDefaultLeader(projectId, instanceId, databaseId, defaultLeader)
    );

    assertTrue(
        "Expected created database to have default leader " + defaultLeader + "."
            + " Output received was " + out,
        out.contains("Default leader: " + defaultLeader)
    );
  }
}
