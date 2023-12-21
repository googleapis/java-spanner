package com.example.spanner.v2;

import static org.junit.Assert.assertTrue;

import com.example.spanner.SampleRunner;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.InstanceName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CreateDatabaseWithDefaultLeaderSampleIT extends SampleTestBaseV2 {

  @Test
  public void testCreateDatabaseWithDefaultLeader() throws Exception {
    final String databaseId = idGenerator.generateDatabaseId();

    // Finds possible default leader

    final String instanceConfigId = instanceAdminClient.getInstance(
        InstanceName.of(projectId, multiRegionalInstanceId)).getConfig();
    final InstanceConfig config = instanceAdminClient.getInstanceConfig(instanceConfigId);
    assertTrue(
        "Expected instance config " + instanceConfigId + " to have at least one leader option",
        config.getLeaderOptionsCount() > 0
    );
    final String defaultLeader = config.getLeaderOptions(0);

    // Runs sample
    final String out = SampleRunner.runSample(() ->
        CreateDatabaseWithDefaultLeaderSample.createDatabaseWithDefaultLeader(
            projectId,
            multiRegionalInstanceId,
            databaseId,
            defaultLeader
        )
    );

    assertTrue(
        "Expected created database to have default leader " + defaultLeader + "."
            + " Output received was " + out,
        out.contains("Default leader: " + defaultLeader)
    );
  }
}
