/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner.v2;

//[START spanner_get_instance_config]

import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import java.io.IOException;

public class GetInstanceConfigSample {

  static void getInstanceConfig() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    final String instanceConfigName = "nam6";
    getInstanceConfig(instanceConfigName);
  }

  static void getInstanceConfig(String instanceConfigName) throws IOException {
    final InstanceAdminClient instanceAdminClient = InstanceAdminClient.create();

    final InstanceConfig instanceConfig = instanceAdminClient.getInstanceConfig(instanceConfigName);

    System.out.printf(
        "Available leader options for instance config %s: %s%n",
        instanceConfig.getName(),
        instanceConfig.getLeaderOptionsList()
    );
  }
}
//[END spanner_get_instance_config]
