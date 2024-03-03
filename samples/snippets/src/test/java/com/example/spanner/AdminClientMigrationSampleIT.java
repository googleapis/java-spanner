package com.example.spanner;

import static com.google.common.truth.Truth.assertThat;

import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.InstanceName;
import org.junit.Test;

public class AdminClientMigrationSampleIT extends SampleTestBase {

  @Test
  public void testCreateInstanceWithAutoscalingConfig() throws Exception {
    String instanceId1 = idGenerator.generateInstanceId();
    String instanceId2 = idGenerator.generateInstanceId();
    String databaseId1 = idGenerator.generateDatabaseId();
    String databaseId2 = idGenerator.generateDatabaseId();
    String out =
        SampleRunner.runSample(
            () -> AdminClientMigrationSample.migrateInstanceAdminClient(
                projectId, instanceId1, databaseId1, instanceId2, databaseId2));
    assertThat(out)
        .contains(String.format("Instance %s was successfully created",
                InstanceName.of(projectId, instanceId1)));
    assertThat(out)
        .contains(String.format("Instance %s was successfully created",
            InstanceName.of(projectId, instanceId2)));
    assertThat(out).contains(String.format("Database %s was successfully created%n",
        DatabaseName.of(projectId, instanceId1, databaseId1)));
    assertThat(out).contains(String.format("Database %s was successfully created%n",
        DatabaseName.of(projectId, instanceId2, databaseId2)));
  }
}
