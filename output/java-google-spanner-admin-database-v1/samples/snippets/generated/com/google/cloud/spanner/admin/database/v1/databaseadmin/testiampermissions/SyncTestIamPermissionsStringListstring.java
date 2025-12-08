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

package com.google.cloud.spanner.admin.database.v1.samples;

// [START spanner_v1_generated_DatabaseAdmin_TestIamPermissions_StringListstring_sync]
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.spanner.admin.database.v1.BackupName;
import java.util.ArrayList;
import java.util.List;

public class SyncTestIamPermissionsStringListstring {

  public static void main(String[] args) throws Exception {
    syncTestIamPermissionsStringListstring();
  }

  public static void syncTestIamPermissionsStringListstring() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    try (DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create()) {
      String resource = BackupName.of("[PROJECT]", "[INSTANCE]", "[BACKUP]").toString();
      List<String> permissions = new ArrayList<>();
      TestIamPermissionsResponse response =
          databaseAdminClient.testIamPermissions(resource, permissions);
    }
  }
}
// [END spanner_v1_generated_DatabaseAdmin_TestIamPermissions_StringListstring_sync]
