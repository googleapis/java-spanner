/*
 * Copyright 2026 Google LLC
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

package com.google.cloud.spanner.connection.it;

import static org.junit.Assert.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.spanner.*;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.cloud.spanner.connection.MutableCredentials;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.InstanceName;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(SerialIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITMutableCredentialsTest {
  private static final String MISSING_PERM_KEY =
      "/com/google/cloud/spanner/connection/test-key-missing-permissions.json";

  private static final String INVALID_KEY = "/com/google/cloud/spanner/connection/test-key.json";

  @Test
  public void testMutableCredentialsUpdateAuthorizationForRunningClient() throws IOException {

    GoogleCredentials missingPermissionCredentials;
    try (InputStream stream =
        ITMutableCredentialsTest.class.getResourceAsStream(MISSING_PERM_KEY)) {
      missingPermissionCredentials = GoogleCredentials.fromStream(stream);
    }
    ServiceAccountCredentials invalidCredentials;
    try (InputStream stream = ITMutableCredentialsTest.class.getResourceAsStream(INVALID_KEY)) {
      invalidCredentials = ServiceAccountCredentials.fromStream(stream);
    }
    List<String> scopes =
        Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    // create MutableCredentials first with missing permissions
    MutableCredentials mutableCredentials =
        new MutableCredentials((ServiceAccountCredentials) missingPermissionCredentials, scopes);

    SpannerOptions options = SpannerOptions.newBuilder().setCredentials(mutableCredentials).build();
    try (Spanner spanner = options.getService();
        DatabaseAdminClient databaseAdminClient = spanner.createDatabaseAdminClient()) {
      String project = "gcloud-devel";
      String instance = "java-client-integration-tests";
      try {
        listDatabases(databaseAdminClient, project, instance);
        fail("Expected PERMISSION_DENIED");
      } catch (Exception e) {
        // specifically validate the permission denied error message
        System.out.println("exception " + e.getMessage());
        assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
        assertFalse(e.getMessage().contains("UNAUTHENTICATED"));
      }

      // update mutableCredentials now to use an invalid credential
      mutableCredentials.updateCredentials(invalidCredentials);
      try {
        listDatabases(databaseAdminClient, project, instance);
        fail("Expected UNAUTHENTICATED after switching to invalid credentials");
      } catch (Exception e) {
        assertTrue(e.getMessage().contains("UNAUTHENTICATED"));
        assertFalse(e.getMessage().contains("PERMISSION_DENIED"));
      }
    }
  }

  private static void listDatabases(
      DatabaseAdminClient databaseAdminClient, String projectId, String instanceId) {
    DatabaseAdminClient.ListDatabasesPagedResponse response =
        databaseAdminClient.listDatabases(InstanceName.of(projectId, instanceId));

    for (DatabaseAdminClient.ListDatabasesPage page : response.iteratePages()) {
      for (Database database : page.iterateAll()) {
        // no-op
      }
    }
  }
}
