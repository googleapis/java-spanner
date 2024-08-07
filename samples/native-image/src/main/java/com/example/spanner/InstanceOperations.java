/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner;

import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;

/** Helper methods to manage Spanner instances. */
public class InstanceOperations {

  static void createTestInstance(
      InstanceAdminClient instanceAdminClient, String projectId, String instanceId)
      throws Exception {

    if (instanceExists(instanceAdminClient, instanceId)) {
      instanceAdminClient.deleteInstance(instanceId);
    }

    InstanceInfo instanceInfo =
        InstanceInfo.newBuilder(InstanceId.of(projectId, instanceId))
            .setInstanceConfigId(InstanceConfigId.of(projectId, "regional-us-east4"))
            .setNodeCount(1)
            .setDisplayName(instanceId)
            .build();
    try {
      instanceAdminClient.createInstance(instanceInfo).get();
    } catch (Exception e) {
      throw new Exception("Failed to create Spanner instance.", e);
    }
  }

  static boolean instanceExists(InstanceAdminClient instanceAdminClient, String instanceName) {
    for (Instance instance : instanceAdminClient.listInstances().iterateAll()) {
      if (instanceName.equals(instance.getId().getInstance())) {
        return true;
      }
    }
    return false;
  }
}
