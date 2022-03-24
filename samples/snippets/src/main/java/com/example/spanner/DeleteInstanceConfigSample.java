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

// [START spanner_delete_instance_config]
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;

class DeleteInstanceConfigSample {
  static void deleteInstanceConfig() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceConfigId = "custom-user-config";
    deleteInstanceConfig(projectId, instanceConfigId);
  }

  static void deleteInstanceConfig(String projectId, String instanceConfigId) {
    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      final InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();

      try {
        instanceAdminClient.deleteInstanceConfig(instanceConfigId);
        System.out.printf("Deleted user instance config with id %s\n", instanceConfigId);
      } catch (SpannerException e) {
        System.out.printf(
            "Could not delete user instance config %s: %s\n", instanceConfigId, e.getMessage());
      }
    }
  }
}
// [END spanner_delete_instance_config]
