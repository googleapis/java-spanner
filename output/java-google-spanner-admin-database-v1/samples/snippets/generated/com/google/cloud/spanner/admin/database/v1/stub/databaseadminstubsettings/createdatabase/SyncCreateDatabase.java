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

package com.google.cloud.spanner.admin.database.v1.stub.samples;

// [START spanner_v1_generated_DatabaseAdminStubSettings_CreateDatabase_sync]
import com.google.api.gax.longrunning.OperationalTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.TimedRetryAlgorithm;
import com.google.cloud.spanner.admin.database.v1.stub.DatabaseAdminStubSettings;
import java.time.Duration;

public class SyncCreateDatabase {

  public static void main(String[] args) throws Exception {
    syncCreateDatabase();
  }

  public static void syncCreateDatabase() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    DatabaseAdminStubSettings.Builder databaseAdminSettingsBuilder =
        DatabaseAdminStubSettings.newBuilder();
    TimedRetryAlgorithm timedRetryAlgorithm =
        OperationalTimedPollAlgorithm.create(
            RetrySettings.newBuilder()
                .setInitialRetryDelayDuration(Duration.ofMillis(500))
                .setRetryDelayMultiplier(1.5)
                .setMaxRetryDelayDuration(Duration.ofMillis(5000))
                .setTotalTimeoutDuration(Duration.ofHours(24))
                .build());
    databaseAdminSettingsBuilder
        .createClusterOperationSettings()
        .setPollingAlgorithm(timedRetryAlgorithm)
        .build();
  }
}
// [END spanner_v1_generated_DatabaseAdminStubSettings_CreateDatabase_sync]
