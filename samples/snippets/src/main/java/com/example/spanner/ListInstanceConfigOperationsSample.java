package com.example.spanner;

// [START spanner_list_instance_config_operations]
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.longrunning.Operation;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.spanner.admin.instance.v1.CreateInstanceConfigMetadata;

public class ListInstanceConfigOperationsSample {
  static void listInstanceConfigOperations() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    listInstanceConfigOperations(projectId);
  }

  static void listInstanceConfigOperations(String projectId) {
    try (Spanner spanner =
        SpannerOptions.newBuilder()
            .setProjectId(projectId)
            .build()
            .getService()) {
      final InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();

      try {
        System.out.printf(
            "Waiting for listing instance config operations on project %s to complete...\n",
            projectId);
        final Iterable<Operation> instanceConfigOperations =
            instanceAdminClient.listInstanceConfigOperations(
                Options.filter(
                    "(metadata.@type=type.googleapis.com/google.spanner.admin.instance.v1.CreateInstanceConfigMetadata)"))
                .iterateAll();
        for (Operation operation : instanceConfigOperations) {
          CreateInstanceConfigMetadata metadata =
              operation.getMetadata().unpack(CreateInstanceConfigMetadata.class);
          System.out.printf(
              "List instance config operation on %s is %d%% completed.\n",
              metadata.getInstanceConfig().getName(),
              metadata.getProgress().getProgressPercent());
        }
      } catch (InvalidProtocolBufferException e) {
        System.out.printf(
            "Error: Listing instance config operations failed with error message %s\n",
            e.getMessage());
        e.printStackTrace();
      }
    }
  }
}
// [END spanner_list_instance_config_operations]
