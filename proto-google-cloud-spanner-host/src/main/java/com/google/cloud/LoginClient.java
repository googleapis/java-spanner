/*
 * Copyright 2017 Google LLC
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

package com.google.cloud;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import spanner.experimental.AccessToken;

public class LoginClient {

  private final String username;
  private final String password;
  private final String endpoint;
  private volatile AccessToken accessToken;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final ReentrantLock refreshLock = new ReentrantLock();
  private ScheduledFuture<?> scheduledTask; // Holds the scheduled task
  private String clientCertificate = null;
  private String clientCertificateKey = null;

  private static final long TOKEN_REFRESH_THRESHOLD_SECONDS = 300; // Refresh 5 minutes before expiry

  public LoginClient(String username, String password, String endpoint) throws Exception {
    this(username, password, endpoint, null, null);
  }

  public LoginClient(
      String username,
      String password,
      String endpoint,
      String clientCertificate,
      String clientCertificateKey) {
    this.username = username;
    this.password = password;
    this.endpoint = endpoint;
    if (clientCertificate != null && clientCertificateKey != null) {
      this.clientCertificate = clientCertificate;
      this.clientCertificateKey = clientCertificateKey;
    }
    login();
    scheduleNextTokenRefresh();
  }

  private void login() {
    try {
      Scram scram =
          new Scram(
              this.username,
              this.password,
              new GrpcClient(this.endpoint, this.clientCertificate, this.clientCertificateKey));
      this.accessToken = scram.login();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public AccessToken getAccessToken() {
    return accessToken;
  }

  private void scheduleNextTokenRefresh() {
    if (accessToken == null) return;
    long delay =
        (accessToken.getExpirationTime().getSeconds() - System.currentTimeMillis() / 1000)
            - TOKEN_REFRESH_THRESHOLD_SECONDS;

    if (delay <= 0) {
      refreshToken();
      return;
    }
    if (scheduledTask != null) {
      scheduledTask.cancel(false);
    }
    // Schedule a new token refresh exactly when needed
    scheduledTask = scheduler.schedule(this::refreshToken, delay, TimeUnit.SECONDS);
    System.out.println("Next token refresh scheduled in " + delay + " seconds.");
  }

  private void refreshToken() {
    if (!refreshLock.tryLock()) return; // Prevent multiple simultaneous refreshes

    try {
      System.out.println("Refreshing access token...");
      login();
      System.out.println("New token acquired.\n" + getAccessToken());
      scheduleNextTokenRefresh();
    } catch (Exception e) {
      System.err.println("Token refresh failed: " + e.getMessage());
    } finally {
      refreshLock.unlock();
    }
  }

  public void shutdown() {
    System.out.println("Shutting down LoginClient...");
    if (scheduledTask != null) {
      scheduledTask.cancel(false); // Cancel any pending token refresh task
    }
    scheduler.shutdown();
  }
}
