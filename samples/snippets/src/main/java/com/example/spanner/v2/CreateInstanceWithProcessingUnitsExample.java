/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner.v2;

//[START spanner_create_instance_with_processing_units]

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceRequest;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.InstanceConfigName;
import com.google.spanner.admin.instance.v1.ProjectName;
import java.io.IOException;

class CreateInstanceWithProcessingUnitsExample {

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
    String configId = "regional-us-central1";
    // This will create an instance with the processing power of 0.2 nodes.
    int processingUnits = 500;
    String displayName = "Descriptive name";

    try {
      // Creates a new instance
      System.out.printf("Creating instance %s.%n", instanceId);
      Instance instance =
          Instance.newBuilder()
              .setDisplayName(displayName)
              .setProcessingUnits(processingUnits)
              .setConfig(
                  InstanceConfigName.of(projectId, configId).toString())
              .build();
      OperationFuture<Instance, CreateInstanceMetadata> operation =
          instanceAdminClient.createInstanceAsync(
              CreateInstanceRequest.newBuilder()
                  .setParent(ProjectName.of(projectId).toString())
                  .setInstanceId(instanceId)
                  .setInstance(instance)
                  .build());

      // Wait for the createInstance operation to finish.
      System.out.printf("Waiting for operation on %s to complete...%n", instanceId);
      Instance createdInstance = operation.get();

      System.out.printf("Created instance %s.%n", createdInstance.getName());

      Instance instanceResult = instanceAdminClient.getInstance(instanceId);
      System.out.printf("Instance %s has %d processing units.%n", instanceResult.getName(),
          instanceResult.getProcessingUnits());
    } catch (Exception e) {
      System.out.printf("Error: %s.%n", e.getMessage());
    }
  }
}
//[END spanner_create_instance_with_processing_units]
