/*
 * Copyright 2020 Google LLC
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

package com.google.cloud.spanner.it;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.Spanner;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITClientIdTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  @Test
  public void testClientId() throws Exception {
    Spanner spanner = env.getTestHelper().getClient();
    InstanceId instanceId = env.getTestHelper().getInstanceId();
    DatabaseId databaseId = DatabaseId.of(instanceId, "my-database");

    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      try {
        DatabaseClient client = spanner.getDatabaseClient(databaseId);
        ids.add(client.getClientId());
        client.singleUse().readRow("MyTable", Key.of(0), Arrays.asList("MyColumn"));
      } catch (Exception e) {
        // ignore
      }
    }
    // All the database clients have used the same client id.
    assertThat(ids).hasSize(1);
    assertThat(ids.iterator().next()).isEqualTo("client-1");
  }
}
