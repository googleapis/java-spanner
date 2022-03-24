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

package com.example.spanner;

// [START spanner_create_instance_config]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfig;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceConfigInfo;
import com.google.cloud.spanner.ReplicaInfo;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.spanner.admin.instance.v1.CreateInstanceConfigMetadata;
import java.util.List;
import java.util.concurrent.ExecutionException;

class CreateInstanceConfigSample {
  static void createInstanceConfig() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String baseInstanceConfig = "my-base-config";
    String instanceConfigId = "custom-user-config";
    createInstanceConfig(projectId, baseInstanceConfig, instanceConfigId);
  }

  static void createInstanceConfig(
      String projectId, String baseInstanceConfig, String instanceConfigId) {
    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      final InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();

      final InstanceConfig baseConfig = instanceAdminClient.getInstanceConfig(baseInstanceConfig);

      // Custom configurations contain all the replicas of the base config and atleast one optional
      // replica.
      List<ReplicaInfo> replicas = baseConfig.getReplicas();
      replicas.add(baseConfig.getOptionalReplicas().get(0));

      InstanceConfigInfo instanceConfigInfo =
          InstanceConfig.newBuilder(InstanceConfigId.of(projectId, instanceConfigId))
              .setDisplayName(instanceConfigId)
              .setBaseConfig(baseConfig.getId().getName())
              .addAllReplicas(replicas)
              .build();

      final OperationFuture<InstanceConfig, CreateInstanceConfigMetadata> operation =
          instanceAdminClient.createInstanceConfig(instanceConfigInfo);

      try {
        InstanceConfig instanceConfig = operation.get();
        System.out.printf("Instance config %s was successfully created%n", instanceConfig.getId());
      } catch (ExecutionException e) {
        System.out.printf(
            "Error: Creating instance config %s failed with error message %s%n",
            instanceConfigInfo.getId(), e.getMessage());
      } catch (InterruptedException e) {
        System.out.println(
            "Error: Waiting for createInstanceConfig operation to finish was interrupted");
      }
    }
  }
}
// [END spanner_create_instance_config]
