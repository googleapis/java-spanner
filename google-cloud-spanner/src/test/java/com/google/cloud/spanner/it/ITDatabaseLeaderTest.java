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

package com.google.cloud.spanner.it;

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import com.google.api.gax.paging.Page;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfig;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITDatabaseLeaderTest {

  public static final Duration WAIT_TIME = Duration.ofMinutes(5);
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  private InstanceAdminClient instanceAdminClient;
  private DatabaseAdminClient databaseAdminClient;
  private String instanceId;
  private String databaseId;

  @BeforeClass
  public static void beforeClass() {
    assumeFalse("The emulator does not support leader options", isUsingEmulator());
  }

  @Before
  public void setUp() {
    instanceId = env.getTestHelper().getInstanceId().getInstance();
    databaseId = env.getTestHelper().getUniqueDatabaseId();
    instanceAdminClient = env.getTestHelper().getClient().getInstanceAdminClient();
    databaseAdminClient = env.getTestHelper().getClient().getDatabaseAdminClient();
  }

  @After
  public void tearDown() {
    databaseAdminClient.dropDatabase(instanceId, databaseId);
  }

  @Test
  public void testCreateDatabaseWithDefaultLeader()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Page<InstanceConfig> instanceConfigs = instanceAdminClient.listInstanceConfigs();
    final InstanceConfig instanceConfig = instanceConfigs.iterateAll().iterator().next();
    final List<String> leaderOptions = instanceConfig.getLeaderOptions();
    final String leader = leaderOptions.get(0);

    final Database createdDatabase =
        databaseAdminClient
            .createDatabase(
                instanceId,
                databaseId,
                Collections.singletonList(
                    "ALTER DATABASE `"
                        + databaseId
                        + "` SET OPTIONS ( default_leader = '"
                        + leader
                        + "' )"))
            .get(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS);
    assertEquals(leader, createdDatabase.getDefaultLeader());

    final Database retrievedDatabase = databaseAdminClient.getDatabase(instanceId, databaseId);
    assertEquals(leader, retrievedDatabase.getDefaultLeader());

    final Database listedDatabase = listDatabasesWithId(databaseId).get(0);
    assertEquals(leader, listedDatabase.getDefaultLeader());
  }

  @Test
  public void testCreateDatabaseWithoutDefaultLeader()
      throws InterruptedException, ExecutionException, TimeoutException {
    final Database createdDatabase =
        databaseAdminClient
            .createDatabase(instanceId, databaseId, Collections.emptyList())
            .get(WAIT_TIME.toMillis(), TimeUnit.MILLISECONDS);
    assertEquals("", createdDatabase.getDefaultLeader());

    final Database retrievedDatabase = databaseAdminClient.getDatabase(instanceId, databaseId);
    assertEquals("", retrievedDatabase.getDefaultLeader());

    final Database listedDatabase = listDatabasesWithId(databaseId).get(0);
    assertEquals("", listedDatabase.getDefaultLeader());
  }

  private List<Database> listDatabasesWithId(String databaseId) {
    Page<Database> page = databaseAdminClient.listDatabases(instanceId);
    Stream<Database> stream = StreamSupport.stream(page.getValues().spliterator(), false);
    while (page.hasNextPage()) {
      page = page.getNextPage();
      stream = Stream.concat(stream, StreamSupport.stream(page.getValues().spliterator(), false));
    }

    return stream
        .filter(database -> database.getId().getDatabase().equals(databaseId))
        .collect(Collectors.toList());
  }
}
