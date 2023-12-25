/*
 * Copyright 2022 Google LLC
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

// [START spanner_create_instance_config]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.instance.v1.CreateInstanceConfigMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.ReplicaInfo;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class CreateInstanceConfigSample {
  static void createInstanceConfig() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String baseInstanceConfig = "my-base-instance-config";
    String instanceConfigId = "custom-instance-config4";
    createInstanceConfig(projectId, baseInstanceConfig, instanceConfigId);
  }

  static void createInstanceConfig(
      String projectId, String baseInstanceConfig, String instanceConfigName) throws IOException {
    InstanceAdminSettings instanceAdminSettings =
        InstanceAdminSettings.newBuilder().setQuotaProjectId(projectId).build();
    InstanceAdminClient instanceAdminClient = InstanceAdminClient.create(instanceAdminSettings);
    final InstanceConfig baseConfig = instanceAdminClient.getInstanceConfig(baseInstanceConfig);
    final List<ReplicaInfo> readOnlyReplicas = ImmutableList.of(baseConfig.getOptionalReplicas(0));
    final InstanceConfig instanceConfig =
        InstanceConfig.newBuilder().setName(instanceConfigName).setBaseConfig(baseInstanceConfig)
            .setDisplayName(instanceConfigName).addAllReplicas(readOnlyReplicas).build();
    final CreateInstanceConfigRequest createInstanceConfigRequest =
        CreateInstanceConfigRequest.newBuilder().setInstanceConfig(instanceConfig).build();
    final OperationFuture<InstanceConfig, CreateInstanceConfigMetadata> operation =
        instanceAdminClient.createInstanceConfigAsync(createInstanceConfigRequest);
    try {
      System.out.printf("Waiting for create operation for %s to complete...\n", instanceConfigName);
      InstanceConfig instanceConfigResult = operation.get(5, TimeUnit.MINUTES);
      System.out.printf("Created instance configuration %s\n", instanceConfigResult.getName());
    } catch (ExecutionException | TimeoutException e) {
      System.out.printf(
          "Error: Creating instance configuration %s failed with error message %s\n",
          instanceConfig.getName(), e.getMessage());
    } catch (InterruptedException e) {
      System.out.println(
          "Error: Waiting for createInstanceConfig operation to finish was interrupted");
    }
  }
}
// [END spanner_create_instance_config]
