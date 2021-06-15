/*
 * Copyright 2019 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConnectionOptionsTest {
  private static final String FILE_TEST_PATH =
      ConnectionOptionsTest.class.getResource("test-key.json").getFile();
  private static final String DEFAULT_HOST = "https://spanner.googleapis.com";

  @Test
  public void testBuildWithURIWithDots() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner:/projects/some-company.com:test-project-123/instances/test-instance-123/databases/test-database-123");
    builder.setCredentialsUrl(FILE_TEST_PATH);
    ConnectionOptions options = builder.build();
    assertThat(options.getHost()).isEqualTo(DEFAULT_HOST);
    assertThat(options.getProjectId()).isEqualTo("some-company.com:test-project-123");
    assertThat(options.getInstanceId()).isEqualTo("test-instance-123");
    assertThat(options.getDatabaseName()).isEqualTo("test-database-123");
    assertThat(options.getCredentials())
        .isEqualTo(new CredentialsService().createCredentials(FILE_TEST_PATH));
    assertThat(options.isAutocommit()).isEqualTo(ConnectionOptions.DEFAULT_AUTOCOMMIT);
    assertThat(options.isReadOnly()).isEqualTo(ConnectionOptions.DEFAULT_READONLY);
  }

  @Test
  public void testBuildWithValidURIAndCredentialsFileURL() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance-123/databases/test-database-123");
    builder.setCredentialsUrl(FILE_TEST_PATH);
    ConnectionOptions options = builder.build();
    assertThat(options.getHost()).isEqualTo(DEFAULT_HOST);
    assertThat(options.getProjectId()).isEqualTo("test-project-123");
    assertThat(options.getInstanceId()).isEqualTo("test-instance-123");
    assertThat(options.getDatabaseName()).isEqualTo("test-database-123");
    assertThat(options.getCredentials())
        .isEqualTo(new CredentialsService().createCredentials(FILE_TEST_PATH));
    assertThat(options.isAutocommit()).isEqualTo(ConnectionOptions.DEFAULT_AUTOCOMMIT);
    assertThat(options.isReadOnly()).isEqualTo(ConnectionOptions.DEFAULT_READONLY);
  }

  @Test
  public void testBuildWithValidURIAndProperties() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance-123/databases/test-database-123?autocommit=false;readonly=true");
    builder.setCredentialsUrl(FILE_TEST_PATH);
    ConnectionOptions options = builder.build();
    assertThat(options.getHost()).isEqualTo(DEFAULT_HOST);
    assertThat(options.getProjectId()).isEqualTo("test-project-123");
    assertThat(options.getInstanceId()).isEqualTo("test-instance-123");
    assertThat(options.getDatabaseName()).isEqualTo("test-database-123");
    assertThat(options.getCredentials())
        .isEqualTo(new CredentialsService().createCredentials(FILE_TEST_PATH));
    assertThat(options.isAutocommit()).isEqualTo(false);
    assertThat(options.isReadOnly()).isEqualTo(true);
  }

  @Test
  public void testBuildWithHostAndValidURI() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner://test-spanner.googleapis.com/projects/test-project-123/instances/test-instance-123/databases/test-database-123");
    builder.setCredentialsUrl(FILE_TEST_PATH);
    ConnectionOptions options = builder.build();
    assertThat(options.getHost()).isEqualTo("https://test-spanner.googleapis.com");
    assertThat(options.getProjectId()).isEqualTo("test-project-123");
    assertThat(options.getInstanceId()).isEqualTo("test-instance-123");
    assertThat(options.getDatabaseName()).isEqualTo("test-database-123");
    assertThat(options.getCredentials())
        .isEqualTo(new CredentialsService().createCredentials(FILE_TEST_PATH));
    assertThat(options.isAutocommit()).isEqualTo(ConnectionOptions.DEFAULT_AUTOCOMMIT);
    assertThat(options.isReadOnly()).isEqualTo(ConnectionOptions.DEFAULT_READONLY);
  }

  @Test
  public void testBuildWithLocalhostPortAndValidURI() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner://localhost:8443/projects/test-project-123/instances/test-instance-123/databases/test-database-123");
    builder.setCredentialsUrl(FILE_TEST_PATH);
    ConnectionOptions options = builder.build();
    assertThat(options.getHost()).isEqualTo("https://localhost:8443");
    assertThat(options.getProjectId()).isEqualTo("test-project-123");
    assertThat(options.getInstanceId()).isEqualTo("test-instance-123");
    assertThat(options.getDatabaseName()).isEqualTo("test-database-123");
    assertThat(options.getCredentials())
        .isEqualTo(new CredentialsService().createCredentials(FILE_TEST_PATH));
    assertThat(options.isAutocommit()).isEqualTo(ConnectionOptions.DEFAULT_AUTOCOMMIT);
    assertThat(options.isReadOnly()).isEqualTo(ConnectionOptions.DEFAULT_READONLY);
  }

  @Test
  public void testBuildWithAutoConfigEmulator() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance-123/databases/test-database-123?autoConfigEmulator=true");
    ConnectionOptions options = builder.build();
    assertEquals("http://localhost:9010", options.getHost());
    assertEquals("test-project-123", options.getProjectId());
    assertEquals("test-instance-123", options.getInstanceId());
    assertEquals("test-database-123", options.getDatabaseName());
    assertEquals(NoCredentials.getInstance(), options.getCredentials());
    assertTrue(options.isUsePlainText());
  }

  @Test
  public void testBuildWithAutoConfigEmulatorAndHost() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner://central-emulator.local:8080/projects/test-project-123/instances/test-instance-123/databases/test-database-123?autoConfigEmulator=true");
    ConnectionOptions options = builder.build();
    assertEquals("http://central-emulator.local:8080", options.getHost());
    assertEquals("test-project-123", options.getProjectId());
    assertEquals("test-instance-123", options.getInstanceId());
    assertEquals("test-database-123", options.getDatabaseName());
    assertEquals(NoCredentials.getInstance(), options.getCredentials());
    assertTrue(options.isUsePlainText());
  }

  @Test
  public void testBuildWithDefaultProjectPlaceholder() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();
    builder.setUri(
        "cloudspanner:/projects/default_project_id/instances/test-instance-123/databases/test-database-123");
    builder.setCredentialsUrl(FILE_TEST_PATH);
    ConnectionOptions options = builder.build();
    assertThat(options.getHost()).isEqualTo(DEFAULT_HOST);
    String projectId = SpannerOptions.getDefaultProjectId();
    if (projectId == null) {
      projectId =
          ((ServiceAccountCredentials) new CredentialsService().createCredentials(FILE_TEST_PATH))
              .getProjectId();
    }
    assertThat(options.getProjectId()).isEqualTo(projectId);
    assertThat(options.getInstanceId()).isEqualTo("test-instance-123");
    assertThat(options.getDatabaseName()).isEqualTo("test-database-123");
    assertThat(options.getCredentials())
        .isEqualTo(new CredentialsService().createCredentials(FILE_TEST_PATH));
    assertThat(options.isAutocommit()).isEqualTo(ConnectionOptions.DEFAULT_AUTOCOMMIT);
    assertThat(options.isReadOnly()).isEqualTo(ConnectionOptions.DEFAULT_READONLY);
  }

  @Test
  public void testBuilderSetUri() {
    ConnectionOptions.Builder builder = ConnectionOptions.newBuilder();

    // set valid uri's
    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database");
    builder.setUri("cloudspanner:/projects/test-project-123/instances/test-instance");
    builder.setUri("cloudspanner:/projects/test-project-123");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123/instances/test-instance/databases/test-database");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123/instances/test-instance");
    builder.setUri("cloudspanner://spanner.googleapis.com/projects/test-project-123");

    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?autocommit=true");
    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance?autocommit=true");
    builder.setUri("cloudspanner:/projects/test-project-123?autocommit=true");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123/instances/test-instance/databases/test-database?autocommit=true");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123/instances/test-instance?autocommit=true");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123?autocommit=true");

    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?autocommit=true;readonly=false");
    builder.setUri(
        "cloudspanner:/projects/test-project-123/instances/test-instance?autocommit=true;readonly=false");
    builder.setUri("cloudspanner:/projects/test-project-123?autocommit=true;readonly=false");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123/instances/test-instance/databases/test-database?autocommit=true;readonly=false");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123/instances/test-instance?autocommit=true;readonly=false");
    builder.setUri(
        "cloudspanner://spanner.googleapis.com/projects/test-project-123?autocommit=true;readonly=false");

    // set invalid uri's
    setInvalidUri(
        builder, "/projects/test-project-123/instances/test-instance/databases/test-database");
    setInvalidUri(builder, "cloudspanner:/test-project-123/test-instance/test-database");
    setInvalidUri(
        builder,
        "cloudspanner:spanner.googleapis.com/projects/test-project-123/instances/test-instance/databases/test-database");
    setInvalidUri(
        builder,
        "cloudspanner://spanner.googleapis.com/projects/test-project-$$$/instances/test-instance/databases/test-database");
    setInvalidUri(
        builder,
        "cloudspanner://spanner.googleapis.com/projects/test-project-123/databases/test-database");
    setInvalidUri(
        builder,
        "cloudspanner:/projects/test_project_123/instances/test-instance/databases/test-database");

    // Set URI's that are valid, but that contain unknown properties.
    setInvalidProperty(
        builder,
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?read=false",
        "read");
    setInvalidProperty(
        builder,
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?read=false;autocommit=true",
        "read");
    setInvalidProperty(
        builder,
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?read=false;auto=true",
        "read, auto");
  }

  private void setInvalidUri(ConnectionOptions.Builder builder, String uri) {
    try {
      builder.setUri(uri);
      fail(uri + " should be considered an invalid uri");
    } catch (IllegalArgumentException e) {
      // Expected exception
    }
  }

  private void setInvalidProperty(
      ConnectionOptions.Builder builder, String uri, String expectedInvalidProperties) {
    try {
      builder.setUri(uri);
      fail("missing expected exception");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(expectedInvalidProperties);
    }
  }

  @Test
  public void testParseUriProperty() {
    final String baseUri =
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database";

    assertThat(ConnectionOptions.parseUriProperty(baseUri, "autocommit")).isNull();
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "?autocommit=true", "autocommit"))
        .isEqualTo("true");
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "?autocommit=false", "autocommit"))
        .isEqualTo("false");
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "?autocommit=true;", "autocommit"))
        .isEqualTo("true");
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "?autocommit=false;", "autocommit"))
        .isEqualTo("false");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?autocommit=true;readOnly=false", "autocommit"))
        .isEqualTo("true");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?autocommit=false;readOnly=false", "autocommit"))
        .isEqualTo("false");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?readOnly=false;autocommit=true", "autocommit"))
        .isEqualTo("true");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?readOnly=false;autocommit=false", "autocommit"))
        .isEqualTo("false");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?readOnly=false;autocommit=true;foo=bar", "autocommit"))
        .isEqualTo("true");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?readOnly=false;autocommit=false;foo=bar", "autocommit"))
        .isEqualTo("false");

    // case insensitive
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "?AutoCommit=true", "autocommit"))
        .isEqualTo("true");
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "?AutoCommit=false", "autocommit"))
        .isEqualTo("false");

    // ; instead of ? before the properties is ok
    assertThat(ConnectionOptions.parseUriProperty(baseUri + ";autocommit=true", "autocommit"))
        .isEqualTo("true");

    // forgot the ? or ; before the properties
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "autocommit=true", "autocommit"))
        .isNull();
    // substring is not ok
    assertThat(ConnectionOptions.parseUriProperty(baseUri + "?isautocommit=true", "autocommit"))
        .isNull();
  }

  @Test
  public void testParseProperties() {
    final String baseUri =
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database";
    assertThat(ConnectionOptions.parseProperties(baseUri + "?autocommit=true"))
        .isEqualTo(Collections.singletonList("autocommit"));
    assertThat(ConnectionOptions.parseProperties(baseUri + "?autocommit=true;readonly=false"))
        .isEqualTo(Arrays.asList("autocommit", "readonly"));
    assertThat(ConnectionOptions.parseProperties(baseUri + "?autocommit=true;READONLY=false"))
        .isEqualTo(Arrays.asList("autocommit", "READONLY"));
    assertThat(ConnectionOptions.parseProperties(baseUri + ";autocommit=true;readonly=false"))
        .isEqualTo(Arrays.asList("autocommit", "readonly"));
    assertThat(ConnectionOptions.parseProperties(baseUri + ";autocommit=true;readonly=false;"))
        .isEqualTo(Arrays.asList("autocommit", "readonly"));
  }

  @Test
  public void testParsePropertiesSpecifiedMultipleTimes() {
    final String baseUri =
        "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database";
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?autocommit=true;autocommit=false", "autocommit"))
        .isEqualTo("true");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + "?autocommit=false;autocommit=true", "autocommit"))
        .isEqualTo("false");
    assertThat(
            ConnectionOptions.parseUriProperty(
                baseUri + ";autocommit=false;readonly=false;autocommit=true", "autocommit"))
        .isEqualTo("false");
    ConnectionOptions.newBuilder()
        .setUri(
            "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database"
                + ";autocommit=false;readonly=false;autocommit=true");
  }

  @Test
  public void testParseOAuthToken() {
    assertThat(
            ConnectionOptions.parseUriProperty(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database"
                    + "?oauthtoken=RsT5OjbzRn430zqMLgV3Ia",
                "OAuthToken"))
        .isEqualTo("RsT5OjbzRn430zqMLgV3Ia");
    // Try to use both credentials and an OAuth token. That should fail.
    ConnectionOptions.Builder builder =
        ConnectionOptions.newBuilder()
            .setUri(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database"
                    + "?OAuthToken=RsT5OjbzRn430zqMLgV3Ia;credentials=/path/to/credentials.json");
    try {
      builder.build();
      fail("missing expected exception");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Cannot specify both credentials and an OAuth token");
    }

    // Now try to use only an OAuth token.
    builder =
        ConnectionOptions.newBuilder()
            .setUri(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database"
                    + "?OAuthToken=RsT5OjbzRn430zqMLgV3Ia");
    ConnectionOptions options = builder.build();
    assertThat(options.getCredentials()).isInstanceOf(GoogleCredentials.class);
    GoogleCredentials credentials = (GoogleCredentials) options.getCredentials();
    assertThat(credentials.getAccessToken().getTokenValue()).isEqualTo("RsT5OjbzRn430zqMLgV3Ia");
  }

  @Test
  public void testSetOAuthToken() {
    ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database")
            .setOAuthToken("RsT5OjbzRn430zqMLgV3Ia")
            .build();
    assertThat(options.getCredentials()).isInstanceOf(GoogleCredentials.class);
    GoogleCredentials credentials = (GoogleCredentials) options.getCredentials();
    assertThat(credentials.getAccessToken()).isNotNull();
    assertThat(credentials.getAccessToken().getTokenValue()).isEqualTo("RsT5OjbzRn430zqMLgV3Ia");
  }

  @Test
  public void testSetOAuthTokenAndCredentials() {
    try {
      ConnectionOptions.newBuilder()
          .setUri(
              "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database")
          .setOAuthToken("RsT5OjbzRn430zqMLgV3Ia")
          .setCredentialsUrl(FILE_TEST_PATH)
          .build();
      fail("missing expected exception");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("Cannot specify both credentials and an OAuth token");
    }
  }

  @Test
  public void testLenient() {
    ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?lenient=true;foo=bar")
            .setCredentialsUrl(FILE_TEST_PATH)
            .build();
    assertThat(options.getWarnings()).isNotNull();
    assertThat(options.getWarnings()).contains("foo");
    assertThat(options.getWarnings()).doesNotContain("lenient");

    options =
        ConnectionOptions.newBuilder()
            .setUri(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?bar=foo;lenient=true")
            .setCredentialsUrl(FILE_TEST_PATH)
            .build();
    assertThat(options.getWarnings()).isNotNull();
    assertThat(options.getWarnings()).contains("bar");
    assertThat(options.getWarnings()).doesNotContain("lenient");

    try {
      ConnectionOptions.newBuilder()
          .setUri(
              "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?bar=foo")
          .setCredentialsUrl(FILE_TEST_PATH)
          .build();
      fail("missing expected exception");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("bar");
    }
  }

  @Test
  public void testMinSessions() {
    ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?minSessions=400")
            .setCredentialsUrl(FILE_TEST_PATH)
            .build();
    assertThat(options.getMinSessions()).isEqualTo(400);
    assertThat(options.getSessionPoolOptions().getMinSessions()).isEqualTo(400);
  }

  @Test
  public void testMaxSessions() {
    ConnectionOptions options =
        ConnectionOptions.newBuilder()
            .setUri(
                "cloudspanner:/projects/test-project-123/instances/test-instance/databases/test-database?maxSessions=4000")
            .setCredentialsUrl(FILE_TEST_PATH)
            .build();
    assertThat(options.getMaxSessions()).isEqualTo(4000);
    assertThat(options.getSessionPoolOptions().getMaxSessions()).isEqualTo(4000);
  }

  @Test
  public void testLocalConnectionError() {
    String uri =
        "cloudspanner://localhost:1/projects/test-project/instances/test-instance/databases/test-database?usePlainText=true";
    ConnectionOptions options = ConnectionOptions.newBuilder().setUri(uri).build();
    try (Connection connection = options.getConnection()) {
      fail("Missing expected exception");
    } catch (SpannerException e) {
      assertEquals(ErrorCode.UNAVAILABLE, e.getErrorCode());
      assertThat(e.getMessage())
          .contains(
              String.format(
                  "The connection string '%s' contains host 'localhost:1', but no running", uri));
    }
  }

  @Test
  public void testInvalidCredentials() {
    String uri =
        "cloudspanner:/projects/test-project/instances/test-instance/databases/test-database?credentials=/some/non/existing/path";
    try {
      ConnectionOptions.newBuilder().setUri(uri).build();
      fail("Missing expected exception");
    } catch (SpannerException e) {
      assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
      assertThat(e.getMessage())
          .contains("Invalid credentials path specified: /some/non/existing/path");
    }
  }

  @Test
  public void testNonBase64EncodedCredentials() {
    String uri =
        "cloudspanner:/projects/test-project/instances/test-instance/databases/test-database?encodedCredentials=not-a-base64-string/";
    SpannerException e =
        assertThrows(
            SpannerException.class, () -> ConnectionOptions.newBuilder().setUri(uri).build());
    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
    assertThat(e.getMessage())
        .contains("The encoded credentials could not be decoded as a base64 string.");
  }

  @Test
  public void testInvalidEncodedCredentials() throws UnsupportedEncodingException {
    String uri =
        String.format(
            "cloudspanner:/projects/test-project/instances/test-instance/databases/test-database?encodedCredentials=%s",
            BaseEncoding.base64Url().encode("not-a-credentials-JSON-string".getBytes("UTF-8")));
    SpannerException e =
        assertThrows(
            SpannerException.class, () -> ConnectionOptions.newBuilder().setUri(uri).build());
    assertEquals(ErrorCode.INVALID_ARGUMENT, e.getErrorCode());
    assertThat(e.getMessage())
        .contains(
            "The encoded credentials do not contain a valid Google Cloud credentials JSON string.");
  }

  @Test
  public void testValidEncodedCredentials() throws Exception {
    String encoded =
        BaseEncoding.base64Url().encode(Files.asByteSource(new File(FILE_TEST_PATH)).read());
    String uri =
        String.format(
            "cloudspanner:/projects/test-project/instances/test-instance/databases/test-database?encodedCredentials=%s",
            encoded);

    ConnectionOptions options = ConnectionOptions.newBuilder().setUri(uri).build();
    assertEquals(
        new CredentialsService().createCredentials(FILE_TEST_PATH), options.getCredentials());
  }

  @Test
  public void testSetCredentialsAndEncodedCredentials() throws Exception {
    String encoded =
        BaseEncoding.base64Url().encode(Files.asByteSource(new File(FILE_TEST_PATH)).read());
    String uri =
        String.format(
            "cloudspanner:/projects/test-project/instances/test-instance/databases/test-database?credentials=%s;encodedCredentials=%s",
            FILE_TEST_PATH, encoded);

    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> ConnectionOptions.newBuilder().setUri(uri).build());
    assertThat(e.getMessage())
        .contains(
            "Cannot specify both a credentials URL and encoded credentials. Only set one of the properties.");
  }
}
