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

// [START spanner_v1_generated_Spanner_ExecuteSql_async]
import com.google.api.core.ApiFuture;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.RequestOptions;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.RoutingHint;
import com.google.spanner.v1.SessionName;
import com.google.spanner.v1.TransactionSelector;
import com.google.spanner.v1.Type;
import java.util.HashMap;

public class AsyncExecuteSql {

  public static void main(String[] args) throws Exception {
    asyncExecuteSql();
  }

  public static void asyncExecuteSql() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    try (SpannerClient spannerClient = SpannerClient.create()) {
      ExecuteSqlRequest request =
          ExecuteSqlRequest.newBuilder()
              .setSession(
                  SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]").toString())
              .setTransaction(TransactionSelector.newBuilder().build())
              .setSql("sql114126")
              .setParams(Struct.newBuilder().build())
              .putAllParamTypes(new HashMap<String, Type>())
              .setResumeToken(ByteString.EMPTY)
              .setPartitionToken(ByteString.EMPTY)
              .setSeqno(109325920)
              .setQueryOptions(ExecuteSqlRequest.QueryOptions.newBuilder().build())
              .setRequestOptions(RequestOptions.newBuilder().build())
              .setDirectedReadOptions(DirectedReadOptions.newBuilder().build())
              .setDataBoostEnabled(true)
              .setLastStatement(true)
              .setRoutingHint(RoutingHint.newBuilder().build())
              .build();
      ApiFuture<ResultSet> future = spannerClient.executeSqlCallable().futureCall(request);
      // Do something.
      ResultSet response = future.get();
    }
  }
}
// [END spanner_v1_generated_Spanner_ExecuteSql_async]
