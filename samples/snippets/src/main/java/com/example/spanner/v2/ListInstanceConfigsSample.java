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

//[START spanner_list_instance_configs]

import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfig;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

public class ListInstanceConfigsSample {

  static void listInstanceConfigs() {
    // TODO(developer): Replace these variables before running the sample.
    final String projectId = "my-project";
    listInstanceConfigs(projectId);
  }

  static void listInstanceConfigs(String projectId) {
    try (Spanner spanner = SpannerOptions
        .newBuilder()
        .setProjectId(projectId)
        .build()
        .getService()) {
      final InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();

      for (InstanceConfig instanceConfig : instanceAdminClient.listInstanceConfigs().iterateAll()) {
        System.out.printf(
            "Available leader options for instance config %s: %s%n",
            instanceConfig.getId(),
            instanceConfig.getLeaderOptions()
        );
      }
    }
  }
}
//[END spanner_list_instance_configs]
