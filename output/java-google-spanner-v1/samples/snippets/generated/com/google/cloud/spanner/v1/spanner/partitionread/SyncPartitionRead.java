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

// [START spanner_v1_generated_Spanner_PartitionRead_sync]
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.spanner.v1.KeySet;
import com.google.spanner.v1.PartitionOptions;
import com.google.spanner.v1.PartitionReadRequest;
import com.google.spanner.v1.PartitionResponse;
import com.google.spanner.v1.SessionName;
import com.google.spanner.v1.TransactionSelector;
import java.util.ArrayList;

public class SyncPartitionRead {

  public static void main(String[] args) throws Exception {
    syncPartitionRead();
  }

  public static void syncPartitionRead() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    try (SpannerClient spannerClient = SpannerClient.create()) {
      PartitionReadRequest request =
          PartitionReadRequest.newBuilder()
              .setSession(
                  SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]").toString())
              .setTransaction(TransactionSelector.newBuilder().build())
              .setTable("table110115790")
              .setIndex("index100346066")
              .addAllColumns(new ArrayList<String>())
              .setKeySet(KeySet.newBuilder().build())
              .setPartitionOptions(PartitionOptions.newBuilder().build())
              .build();
      PartitionResponse response = spannerClient.partitionRead(request);
    }
  }
}
// [END spanner_v1_generated_Spanner_PartitionRead_sync]
