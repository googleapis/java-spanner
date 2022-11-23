package com.example.spanner;

// [START spanner_add_and_drop_database_role]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.concurrent.ExecutionException;

public class AddAndDropDatabaseRole {

  static void addAndDropDatabaseRole() throws InterruptedException, ExecutionException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";
    String parentRole = "new-parent";
    String childRole = "new-child";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseAdminClient adminClient = spanner.getDatabaseAdminClient();
      addAndDropDatabaseRole(adminClient, instanceId, databaseId, parentRole, childRole);
    }
  }

  static void addAndDropDatabaseRole(
      DatabaseAdminClient adminClient,
      String instanceId,
      String databaseId,
      String parentRole,
      String childRole)
      throws InterruptedException, ExecutionException {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> operation =
        adminClient.updateDatabaseDdl(
            instanceId,
            databaseId,
            ImmutableList.of(
                "CREATE ROLE " + parentRole,
                "GRANT SELECT ON TABLE Albums TO ROLE " + parentRole,
                "CREATE ROLE " + childRole,
                "GRANT ROLE " + parentRole + " TO ROLE " + childRole),
            null);

    // Wait for the operation to finish.
    // This will throw an ExecutionException if the operation fails.
    operation.get();
    System.out.printf("Created roles %s and %s and granted privileges%n", parentRole, childRole);

    // Delete role and membership.
    operation =
        adminClient.updateDatabaseDdl(
            instanceId,
            databaseId,
            ImmutableList.of(
                "REVOKE ROLE " + parentRole + " FROM ROLE " + childRole, "DROP ROLE " + childRole),
            null);
    // Wait for the operation to finish.
    // This will throw an ExecutionException if the operation fails.
    operation.get();
    System.out.printf("Revoked privileges and dropped role %s%n", childRole);
  }
}
// [END spanner_add_and_drop_database_role]
