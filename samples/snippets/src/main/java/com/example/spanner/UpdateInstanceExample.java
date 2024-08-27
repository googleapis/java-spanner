package com.example.spanner;

// [START spanner_update_instance]

import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.common.collect.Lists;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.InstanceConfigName;
import com.google.spanner.admin.instance.v1.InstanceName;
import com.google.spanner.admin.instance.v1.UpdateInstanceRequest;
import java.util.concurrent.ExecutionException;

public class UpdateInstanceExample {

  static void updateInstance() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    updateInstance(projectId, instanceId);
  }

  static void updateInstance(String projectId, String instanceId) {
    // Set Instance configuration.
    int nodeCount = 2;
    String displayName = "Updated name";

    // Create an Instance object that will be used to create the instance.
    Instance instance =
        Instance.newBuilder()
            .setName(InstanceName.of(projectId, instanceId).toString())
            .setDisplayName(displayName)
            .setNodeCount(nodeCount)
            .setEdition(Instance.Edition.ENTERPRISE)
            .setConfig(InstanceConfigName.of(projectId, "regional-us-east4").toString())
            .build();

    try (Spanner spanner =
            SpannerOptions.newBuilder().setProjectId(projectId).build().getService();
        InstanceAdminClient instanceAdminClient = spanner.createInstanceAdminClient()) {

      // Wait for the createInstance operation to finish.
      Instance createdInstance =
          instanceAdminClient
              .updateInstanceAsync(
                  UpdateInstanceRequest.newBuilder()
                      .setFieldMask(
                          FieldMask.newBuilder().addAllPaths(Lists.newArrayList("edition")))
                      .setInstance(instance)
                      .build())
              .get();
      System.out.printf("Instance %s was successfully updated%n", createdInstance.getName());
    } catch (ExecutionException e) {
      System.out.printf(
          "Error: Updating instance %s failed with error message %s%n",
          instance.getName(), e.getMessage());
    } catch (InterruptedException e) {
      System.out.println("Error: Waiting for updateInstance operation to finish was interrupted");
    }
  }
}

// [START spanner_update_instance]
