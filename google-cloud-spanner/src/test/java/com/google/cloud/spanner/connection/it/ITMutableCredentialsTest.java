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
import static org.junit.Assume.assumeTrue;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.spanner.*;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.cloud.spanner.connection.ITAbstractSpannerTest;
import com.google.cloud.spanner.connection.MutableCredentials;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DatabaseName;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.spanner.admin.database.v1.InstanceName;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(SerialIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITMutableCredentialsTest extends ITAbstractSpannerTest {
  private static final String VALID_KEY_RESOURCE =
      "/com/google/cloud/spanner/connection/test-key-cloud-storage.json";

  private static final String INVALID_KEY_RESOURCE =
      "/com/google/cloud/spanner/connection/test-key.json";

  @Test
  public void testMutableCredentialsUpdateAuthorizationForRunningClient() throws IOException {

    GoogleCredentials credentialsFromFile;
    try (InputStream stream =
        ITMutableCredentialsTest.class.getResourceAsStream(VALID_KEY_RESOURCE)) {
      assertNotNull("Missing test resource: " + VALID_KEY_RESOURCE, stream);
      credentialsFromFile = GoogleCredentials.fromStream(stream);
    }
    assumeTrue(
        "This test requires service account credentials",
        credentialsFromFile instanceof ServiceAccountCredentials);

    ServiceAccountCredentials validCredentials = (ServiceAccountCredentials) credentialsFromFile;
    ServiceAccountCredentials invalidCredentials;
    try (InputStream stream =
        ITMutableCredentialsTest.class.getResourceAsStream(INVALID_KEY_RESOURCE)) {
      assertNotNull("Missing test resource: " + INVALID_KEY_RESOURCE, stream);
      invalidCredentials = ServiceAccountCredentials.fromStream(stream);
    }

    List<String> scopes = new ArrayList<>(getTestEnv().getTestHelper().getOptions().getScopes());
    MutableCredentials mutableCredentials = new MutableCredentials(validCredentials, scopes);

    SpannerOptions options = SpannerOptions.newBuilder().setCredentials(mutableCredentials).build();

    try (Spanner spanner = options.getService();
        DatabaseAdminClient databaseAdminClient = spanner.createDatabaseAdminClient()) {
     /* String dbName =
          DatabaseName.of(
                  getTestEnv().getTestHelper().getInstanceId().getProject(),
                  getTestEnv().getTestHelper().getInstanceId().getInstance(),
                  "TEST")
              .toString();
      Database database = databaseAdminClient.getDatabase(dbName);*/
      InstanceName instanceName = InstanceName.of(getTestEnv().getTestHelper().getInstanceId().getProject(), getTestEnv().getTestHelper().getInstanceId().getProject());
      DatabaseAdminClient.ListDatabasesPagedResponse response =
              databaseAdminClient.listDatabases(instanceName);

      boolean databaseFound = false;
      for (DatabaseAdminClient.ListDatabasesPage page : response.iteratePages()) {
        for (Database database : page.iterateAll()) {
             System.out.println("\t" + database.getName());
             databaseFound = true;
        }
      }
      assertTrue(databaseFound);
      try {
        mutableCredentials.updateCredentials(invalidCredentials);
        databaseAdminClient.listDatabases(instanceName);
        fail("Expected UNAUTHENTICATED after switching to invalid credentials");
      } catch (SpannerException e) {
        assertEquals(ErrorCode.UNAUTHENTICATED, e.getErrorCode());
      }
    } finally {
      closeSpanner();
    }
  }
}
