/*
 * Copyright 2022 Google LLC
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for Role Permissions using {@link com.google.cloud.spanner.DatabaseRole}. */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITDatabaseRolePermissionTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private DatabaseAdminClient dbAdminClient;
  private RemoteSpannerHelper testHelper;

  @Before
  public void setUp() {
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
  }

  @Test
  public void grantAndRevokeDatabaseRolePermissions() throws Exception {
    // Create database with table and role permission.
    final String dbRoleParent = "parent";
    final String databaseId = testHelper.getUniqueDatabaseId();
    final String instanceId = testHelper.getInstanceId().getInstance();

    final String createTableT = "CREATE TABLE T (\n" + "  K STRING(MAX),\n" + ") PRIMARY KEY(K)";
    final String createRoleParent = String.format("CREATE ROLE %s", dbRoleParent);
    final String grantSelectOnTableToParent =
        String.format("GRANT SELECT ON TABLE T TO ROLE %s", dbRoleParent);

    final Database createdDatabase =
        dbAdminClient
            .createDatabase(
                instanceId,
                databaseId,
                ImmutableList.of(createTableT, createRoleParent, grantSelectOnTableToParent))
            .get(5, TimeUnit.MINUTES);

    // Connect to db with dbRoleParent.
    SpannerOptions options = SpannerOptions.newBuilder().setCreatorRole(dbRoleParent).build();

    Spanner spanner = options.getService();
    DatabaseClient dbClient = spanner.getDatabaseClient(createdDatabase.getId());
    DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();

    // Test SELECT permissions to role dbRoleParent on table T.
    // Query using dbRoleParent should return result.
    try (ResultSet rs =
        dbClient.singleUse().executeQuery(Statement.of("SELECT COUNT(*) as cnt FROM T"))) {
      assertTrue(rs.next());
    } catch (PermissionDeniedException e) {
      // This is not expected
      fail("Got PermissionDeniedException when it should not have occurred.");
    }

    // Revoke select Permission for dbRoleParent.
    final String revokeSelectOnTableFromParent =
        String.format("REVOKE SELECT ON TABLE T FROM ROLE %s", dbRoleParent);

    dbAdminClient
        .updateDatabaseDdl(
            instanceId, databaseId, Arrays.asList(revokeSelectOnTableFromParent), null)
        .get();

    // Test SELECT permissions to role dbRoleParent on table T.
    // Query using dbRoleParent should return PermissionDeniedException.
    try (ResultSet rs =
        dbClient.singleUse().executeQuery(Statement.of("SELECT COUNT(*) as cnt FROM T"))) {
      rs.next();
      fail("Missing expected permission denied to dbRoleParent Exception");
    } catch (SpannerException e) {
      // This is what we expect.
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
      assertThat(e.getMessage()).contains(dbRoleParent);
    }
    // Drop role and table.
    final String dropTableT = "DROP TABLE T";
    final String dropRoleParent = String.format("DROP ROLE %s", dbRoleParent);
    dbAdminClient
        .updateDatabaseDdl(instanceId, databaseId, Arrays.asList(dropTableT, dropRoleParent), null)
        .get();
  }

  @Test
  public void orphanRolePermissions() throws Exception {
    final String dbRoleOrphan = testHelper.getUniqueDatabaseRole();
    final String databaseId = testHelper.getUniqueDatabaseId();
    final String instanceId = testHelper.getInstanceId().getInstance();

    final String createTableT = "CREATE TABLE T (\n" + "  K STRING(MAX),\n" + ") PRIMARY KEY(K)";
    final String createRoleOrphan = String.format("CREATE ROLE %s", dbRoleOrphan);

    final Database createdDatabase =
        dbAdminClient
            .createDatabase(
                instanceId, databaseId, ImmutableList.of(createTableT, createRoleOrphan))
            .get(5, TimeUnit.MINUTES);

    // Connect to db with dbRoleParent.
    SpannerOptions options = SpannerOptions.newBuilder().setCreatorRole(dbRoleOrphan).build();

    Spanner spanner = options.getService();
    DatabaseClient dbClient = spanner.getDatabaseClient(createdDatabase.getId());
    DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();

    // Test SELECT permissions to role dbRoleOrphan on table T.
    // Query using dbRoleOrphan should return PermissionDeniedException.
    try (ResultSet rs =
        dbClient.singleUse().executeQuery(Statement.of("SELECT COUNT(*) as cnt FROM T"))) {
      rs.next();
      fail("Missing expected permission denied to dbRoleOrphan Exception");
    } catch (SpannerException e) {
      // This is what we expect.
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
      assertThat(e.getMessage()).contains(dbRoleOrphan);
    }

    // Drop role and table.
    final String dropTableT = "DROP TABLE T";
    final String dropRoleParent = String.format("DROP ROLE %s", dbRoleOrphan);
    dbAdminClient
        .updateDatabaseDdl(instanceId, databaseId, Arrays.asList(dropTableT, dropRoleParent), null)
        .get();
  }
}
