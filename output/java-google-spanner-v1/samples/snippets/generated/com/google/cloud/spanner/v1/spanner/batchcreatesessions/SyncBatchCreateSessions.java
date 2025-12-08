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

package com.google.cloud.spanner.v1.samples;

// [START spanner_v1_generated_Spanner_BatchCreateSessions_sync]
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BatchCreateSessionsResponse;
import com.google.spanner.v1.DatabaseName;
import com.google.spanner.v1.Session;

public class SyncBatchCreateSessions {

  public static void main(String[] args) throws Exception {
    syncBatchCreateSessions();
  }

  public static void syncBatchCreateSessions() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    try (SpannerClient spannerClient = SpannerClient.create()) {
      BatchCreateSessionsRequest request =
          BatchCreateSessionsRequest.newBuilder()
              .setDatabase(DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]").toString())
              .setSessionTemplate(Session.newBuilder().build())
              .setSessionCount(185691686)
              .build();
      BatchCreateSessionsResponse response = spannerClient.batchCreateSessions(request);
    }
  }
}
// [END spanner_v1_generated_Spanner_BatchCreateSessions_sync]
