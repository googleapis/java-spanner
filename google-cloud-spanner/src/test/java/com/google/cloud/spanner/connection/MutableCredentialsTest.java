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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MutableCredentialsTest {
  ServiceAccountCredentials initialCredentials = mock(ServiceAccountCredentials.class);
  ServiceAccountCredentials initialScopedCredentials = mock(ServiceAccountCredentials.class);
  ServiceAccountCredentials updatedCredentials = mock(ServiceAccountCredentials.class);
  ServiceAccountCredentials updatedScopedCredentials = mock(ServiceAccountCredentials.class);
  List<String> scopes = Arrays.asList("scope-a", "scope-b");
  Map<String, List<String>> initialMetadata =
          Collections.singletonMap("Authorization", Collections.singletonList("v1"));
  Map<String, List<String>> updatedMetadata =
          Collections.singletonMap("Authorization", Collections.singletonList("v2"));
  String initialAuthType = "auth-1";
  String updatedAuthType = "auth-2";

  @Test
  public void testCreateMutableCredentialsAndUpdate() throws IOException {
    setupInitialCredentials();
    setupUpdatedCredentials();

    MutableCredentials credentials = new MutableCredentials(initialCredentials, scopes);

    assertEquals(initialAuthType, credentials.getAuthenticationType());
    assertTrue(credentials.hasRequestMetadata());
    assertTrue(credentials.hasRequestMetadataOnly());
    assertEquals(initialMetadata, credentials.getRequestMetadata(URI.create("https://spanner.googleapis.com")));

    credentials.refresh();

    verify(initialScopedCredentials, times(1)).refresh();

    credentials.updateCredentials(updatedCredentials);

    assertEquals(updatedAuthType, credentials.getAuthenticationType());
    assertFalse(credentials.hasRequestMetadata());
    assertFalse(credentials.hasRequestMetadataOnly());
    assertSame(updatedMetadata, credentials.getRequestMetadata(URI.create("https://example.com")));

    credentials.refresh();

    verify(updatedScopedCredentials, times(1)).refresh();
  }

  private void setupInitialCredentials() throws IOException {
    when(initialCredentials.createScoped(scopes)).thenReturn(initialScopedCredentials);
    when(initialScopedCredentials.getAuthenticationType()).thenReturn(initialAuthType);
    when(initialScopedCredentials.getRequestMetadata(any(URI.class)))
            .thenReturn(initialMetadata);
    when(initialScopedCredentials.hasRequestMetadata()).thenReturn(true);
    when(initialScopedCredentials.hasRequestMetadataOnly()).thenReturn(true);
  }

  private void setupUpdatedCredentials() throws IOException {
    when(updatedCredentials.createScoped(scopes)).thenReturn(updatedScopedCredentials);
    when(updatedScopedCredentials.getAuthenticationType()).thenReturn(updatedAuthType);
    when(updatedScopedCredentials.getRequestMetadata(any(URI.class))).thenReturn(updatedMetadata);
    when(updatedScopedCredentials.hasRequestMetadata()).thenReturn(false);
    when(updatedScopedCredentials.hasRequestMetadataOnly()).thenReturn(false);
  }
}