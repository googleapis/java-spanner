package com.example.spanner;

// [START spanner_list_database_roles]
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.DatabaseRole;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.concurrent.ExecutionException;

public class ListDatabaseRoles {

  static void listDatabaseRoles() throws InterruptedException, ExecutionException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseAdminClient adminClient = spanner.getDatabaseAdminClient();
      listDatabaseRoles(adminClient, projectId, instanceId, databaseId);
    }
  }

  static void listDatabaseRoles(
      DatabaseAdminClient adminClient, String projectId, String instanceId, String databaseId)
      throws InterruptedException, ExecutionException {
    String databasePath = DatabaseId.of(projectId, instanceId, databaseId).getName();
    System.out.println("List of Database roles");
    for (DatabaseRole role : adminClient.listDatabaseRoles(instanceId, databaseId).iterateAll()) {
      if (!role.getName().startsWith(databasePath + "/databaseRoles/")) {
        throw new RuntimeException(
            "Role +"
                + role.getName()
                + "does not have prefix ["
                + databasePath
                + "/databaseRoles/"
                + "]");
      }
      System.out.printf("%s%n", role.getName());
    }
  }
}
// [END spanner_list_database_roles]
