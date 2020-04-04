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
import static org.junit.Assert.fail;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.spanner.SpannerOptions;
import java.util.Arrays;
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
        .isEqualTo(Arrays.asList("autocommit"));
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
}
