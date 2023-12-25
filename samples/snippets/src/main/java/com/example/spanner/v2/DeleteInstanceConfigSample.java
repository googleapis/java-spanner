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

// [START spanner_delete_instance_config]
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest;
import java.io.IOException;

class DeleteInstanceConfigSample {
  static void deleteInstanceConfig() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceConfigId = "projects/my-project/instanceConfigs/custom-user-config";
    deleteInstanceConfig(projectId, instanceConfigId);
  }

  static void deleteInstanceConfig(String projectId, String instanceConfigId) throws IOException {
    final InstanceAdminSettings instanceAdminSettings =
        InstanceAdminSettings.newBuilder().setQuotaProjectId(projectId).build();
    final InstanceAdminClient instanceAdminClient = InstanceAdminClient.create(instanceAdminSettings);
    final DeleteInstanceConfigRequest request =
        DeleteInstanceConfigRequest.newBuilder().setName(instanceConfigId).build();

    try {
      System.out.printf("Deleting %s...\n", instanceConfigId);
      instanceAdminClient.deleteInstanceConfig(request);
      System.out.printf("Deleted instance configuration %s\n", instanceConfigId);
    } catch (SpannerException e) {
      System.out.printf(
          "Error: Deleting instance configuration %s failed with error message: %s\n",
          instanceConfigId, e.getMessage());
    }
  }
}
// [END spanner_delete_instance_config]
