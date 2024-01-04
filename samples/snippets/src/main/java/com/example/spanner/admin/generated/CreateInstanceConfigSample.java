/*
 * Copyright 2023 Google LLC
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

package com.example.spanner.admin.generated;

// [START spanner_create_instance_config]
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.ReplicaInfo;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CreateInstanceConfigSample {
  static void createInstanceConfig() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "projects/my-project";
    String baseInstanceConfig = "nam11";
    String instanceConfigId = "custom-instance-config4";
    String instanceConfigName = "projects/my-project/instanceConfigs/custom-instance-config4";
    createInstanceConfig(projectId, baseInstanceConfig, instanceConfigId, instanceConfigName);
  }

  static void createInstanceConfig(
      String projectId, String baseInstanceConfig, String instanceConfigId, String instanceConfigName) throws IOException {
    InstanceAdminClient instanceAdminClient = InstanceAdminClient.create();
    final InstanceConfig baseConfig = instanceAdminClient.getInstanceConfig(baseInstanceConfig);

    /**
     * The replicas for the custom instance configuration must include all the replicas of the base
     * configuration, in addition to at least one from the list of optional replicas of the base
     * configuration.
     */
    final List<ReplicaInfo> replicas =
        Stream.concat(baseConfig.getReplicasList().stream(),
            baseConfig.getOptionalReplicasList().stream().limit(1)).collect(Collectors.toList());
    final InstanceConfig instanceConfig =
        InstanceConfig.newBuilder().setName(instanceConfigName).setBaseConfig(baseInstanceConfig)
            .setDisplayName(instanceConfigId).addAllReplicas(replicas).build();
    final CreateInstanceConfigRequest createInstanceConfigRequest =
        CreateInstanceConfigRequest.newBuilder().setParent(projectId)
            .setInstanceConfigId(instanceConfigId).setInstanceConfig(instanceConfig).build();
    try {
      System.out.printf("Waiting for create operation for %s to complete...\n", instanceConfigName);
      InstanceConfig instanceConfigResult =
          instanceAdminClient.createInstanceConfigAsync(
              createInstanceConfigRequest).get(5, TimeUnit.MINUTES);
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
