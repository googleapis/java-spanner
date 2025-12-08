/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.admin.instance.v1.samples;

// [START spanner_v1_generated_InstanceAdmin_UpdateInstanceConfig_LRO_async]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest;

public class AsyncUpdateInstanceConfigLRO {

  public static void main(String[] args) throws Exception {
    asyncUpdateInstanceConfigLRO();
  }

  public static void asyncUpdateInstanceConfigLRO() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
      UpdateInstanceConfigRequest request =
          UpdateInstanceConfigRequest.newBuilder()
              .setInstanceConfig(InstanceConfig.newBuilder().build())
              .setUpdateMask(FieldMask.newBuilder().build())
              .setValidateOnly(true)
              .build();
      OperationFuture<InstanceConfig, UpdateInstanceConfigMetadata> future =
          instanceAdminClient.updateInstanceConfigOperationCallable().futureCall(request);
      // Do something.
      InstanceConfig response = future.get();
    }
  }
}
// [END spanner_v1_generated_InstanceAdmin_UpdateInstanceConfig_LRO_async]
