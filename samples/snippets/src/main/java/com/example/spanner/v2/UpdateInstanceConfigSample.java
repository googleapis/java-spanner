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

// [START spanner_update_instance_config]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class UpdateInstanceConfigSample {
  static void updateInstanceConfig() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String instanceConfigName = "projects/my-project/instanceConfigs/custom-instance-config";
    updateInstanceConfig(instanceConfigName);
  }

  static void updateInstanceConfig(String instanceConfigName) throws IOException {
    final com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient instanceAdminClient =
        com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient.create();
    final InstanceConfig instanceConfig =
        InstanceConfig.newBuilder()
            .setName(instanceConfigName)
            .setDisplayName("updated custom instance config")
            .putLabels("updated", "true").build();
    final UpdateInstanceConfigRequest updateInstanceConfigRequest =
        UpdateInstanceConfigRequest.newBuilder().setInstanceConfig(instanceConfig).setUpdateMask(
            FieldMask.newBuilder().addAllPaths(ImmutableList.of("display_name", "labels")).build()).build();
    final OperationFuture<InstanceConfig, UpdateInstanceConfigMetadata> operation =
        instanceAdminClient.updateInstanceConfigAsync(updateInstanceConfigRequest);
    try {
      System.out.printf("Waiting for update operation on %s to complete...\n", instanceConfigName);
      InstanceConfig instanceConfigResult = operation.get(5, TimeUnit.MINUTES);
      System.out.printf(
          "Updated instance configuration %s with new display name %s\n",
          instanceConfigResult.getName(), instanceConfig.getDisplayName());
    } catch (ExecutionException | TimeoutException e) {
      System.out.printf(
          "Error: Updating instance config %s failed with error message %s\n",
          instanceConfig.getName(), e.getMessage());
      e.printStackTrace();
    } catch (InterruptedException e) {
      System.out.println(
          "Error: Waiting for updateInstanceConfig operation to finish was interrupted");
    }
  }
}
// [END spanner_update_instance_config]
