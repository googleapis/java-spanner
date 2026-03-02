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

package com.google.cloud.spanner.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SerialIntegrationTest;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(SerialIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITMutableCredentialsTest extends ITAbstractSpannerTest {
  private static final String INVALID_KEY_FILE =
      ITMutableCredentialsTest.class.getResource("test-key.json").getPath();

  @Test
  public void testMutableCredentialsUpdateAuthorizationForRunningClient() throws IOException {
    assumeTrue("This test requires a service account key file", hasValidKeyFile());

    GoogleCredentials credentialsFromFile;
    try (InputStream stream = Files.newInputStream(Paths.get(getKeyFile()))) {
      credentialsFromFile = GoogleCredentials.fromStream(stream);
    }
    assumeTrue(
        "This test requires service account credentials",
        credentialsFromFile instanceof ServiceAccountCredentials);

    ServiceAccountCredentials validCredentials = (ServiceAccountCredentials) credentialsFromFile;
    ServiceAccountCredentials invalidCredentials;
    try (InputStream stream = Files.newInputStream(Paths.get(INVALID_KEY_FILE))) {
      invalidCredentials = ServiceAccountCredentials.fromStream(stream);
    }

    List<String> scopes = new ArrayList<>(getTestEnv().getTestHelper().getOptions().getScopes());
    MutableCredentials mutableCredentials = new MutableCredentials(validCredentials, scopes);

    StringBuilder uri =
        extractConnectionUrl(getTestEnv().getTestHelper().getOptions(), getDatabase());
    ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(uri.toString())
            .setCredentials(mutableCredentials)
            .build();

    try (Connection connection = options.getConnection()) {
      try (ResultSet rs = connection.executeQuery(Statement.of("SELECT 1"))) {
        assertTrue(rs.next());
      }

      mutableCredentials.updateCredentials(invalidCredentials);

      try (ResultSet rs = connection.executeQuery(Statement.of("SELECT 2"))) {
        rs.next();
        fail("Expected UNAUTHENTICATED after switching to invalid credentials");
      } catch (SpannerException e) {
        assertEquals(ErrorCode.UNAUTHENTICATED, e.getErrorCode());
      }
    } finally {
      closeSpanner();
    }
  }
}

