package com.example.spanner;

import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.InstanceConfig;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class QueryInformationSchemaDatabaseOptionsSampleIT extends SampleTestBase {

  @Test
  public void testQueryInformationSchemaDatabaseOptions() throws Exception {
    // Finds a possible new leader option
    final InstanceConfig config = instanceAdminClient.getInstanceConfig(instanceConfigName);
    assertTrue(
        "Expected instance config " + instanceConfigName + " to have at least one leader option",
        config.getLeaderOptions().size() > 0
    );
    final String defaultLeader = config.getLeaderOptions().get(0);

    // Creates database
    final String databaseId = idGenerator.generateDatabaseId();
    databaseAdminClient.createDatabase(
        instanceId,
        databaseId,
        Arrays.asList(
            "CREATE TABLE Singers (Id INT64 NOT NULL) PRIMARY KEY (Id)",
            "ALTER DATABASE `"
                + databaseId
                + "` SET OPTIONS ( default_leader = '"
                + defaultLeader
                + "')"
        )
    ).get(5, TimeUnit.MINUTES);

    // Runs sample
    final String out = SampleRunner.runSample(() -> QueryInformationSchemaDatabaseOptionsSample
        .queryInformationSchemaDatabaseOptions(projectId, instanceId, databaseId)
    );

    assertTrue(
        "Expected to have retrieved default_leader for " + databaseId + " as " + defaultLeader + "."
            + " Output received was " + out,
        out.contains(
            "The default_leader for projects/"
                + projectId + "/instances/" + instanceId + "/databases/" + databaseId
                + " is " + defaultLeader
        )
    );
  }
}
