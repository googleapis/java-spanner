package com.example.spanner.v2;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceRequest;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.InstanceConfigName;
import com.google.spanner.admin.instance.v1.ProjectName;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

class CreateInstanceSample {

  static void createInstance() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    createInstance(projectId, instanceId);
  }

  static void createInstance(String projectId, String instanceId) throws IOException {
    InstanceAdminSettings instanceAdminSettings =
        InstanceAdminSettings.newBuilder().setQuotaProjectId(projectId).build();
    InstanceAdminClient instanceAdminClient = InstanceAdminClient.create(instanceAdminSettings);


    // Set Instance configuration.
    int nodeCount = 2;
    String displayName = "Descriptive name";

    // Create an Instance object that will be used to create the instance.
    Instance instance =
        Instance.newBuilder()
            .setDisplayName(displayName)
            .setNodeCount(nodeCount)
            .setConfig(
                InstanceConfigName.of(projectId, "regional-us-central1").toString())
            .build();
    OperationFuture<Instance, CreateInstanceMetadata> operation =
        instanceAdminClient.createInstanceAsync(
            CreateInstanceRequest.newBuilder()
                .setParent(ProjectName.of(projectId).toString())
                .setInstanceId(instanceId)
                .setInstance(instance)
                .build());

    try {
      // Wait for the createInstance operation to finish.
      Instance createdInstance = operation.get();
      System.out.printf("Instance %s was successfully created%n", createdInstance.getName());
    } catch (ExecutionException e) {
      System.out.printf(
          "Error: Creating instance %s failed with error message %s%n",
          instance.getName(), e.getMessage());
      throw SpannerExceptionFactory.asSpannerException(e);
    } catch (InterruptedException e) {
      System.out.println("Error: Waiting for createInstance operation to finish was interrupted");
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
}