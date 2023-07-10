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
package com.google.cloud.spanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.spanner.SessionPoolOptions.InactiveTransactionRemovalOptions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.threeten.bp.Duration;

/** Unit tests for {@link com.google.cloud.spanner.SessionPoolOptions} */
@RunWith(Parameterized.class)
public class SessionPoolOptionsTest {
  @Parameter public int minSessions;

  @Parameter(1)
  public int maxSessions;

  @Parameters(name = "min sessions = {0}, max sessions = {1}")
  public static Collection<Object[]> data() {
    List<Object[]> params = new ArrayList<>();
    params.add(new Object[] {1, 1});
    params.add(new Object[] {500, 600});
    params.add(new Object[] {600, 500});

    return params;
  }

  @Test
  public void setMinMaxSessions() {
    try {
      SessionPoolOptions options =
          SessionPoolOptions.newBuilder()
              .setMinSessions(minSessions)
              .setMaxSessions(maxSessions)
              .build();
      if (minSessions > maxSessions) {
        fail("Expected exception");
      }
      assertThat(minSessions).isEqualTo(options.getMinSessions());
      assertThat(maxSessions).isEqualTo(options.getMaxSessions());
    } catch (IllegalArgumentException ex) {
      if (minSessions <= maxSessions) {
        throw ex;
      }
      assertNotNull(ex.getMessage());
    }
  }

  /**
   * Setting MaxSessions to a value lower than the default MinSessions should be allowed, and should
   * cause the MinSessions to be set to the same value as the MaxSessions.
   */
  @Test
  public void setOnlyMaxSessions() {
    final int defaultMinSessions = 100;
    // Set max sessions > DEFAULT_MIN_SESSIONS.
    SessionPoolOptions options =
        SessionPoolOptions.newBuilder().setMaxSessions(defaultMinSessions + 1).build();
    assertThat(options.getMaxSessions()).isEqualTo(defaultMinSessions + 1);
    assertThat(options.getMinSessions()).isEqualTo(defaultMinSessions);
    // Set max sessions < DEFAULT_MIN_SESSIONS.
    options = SessionPoolOptions.newBuilder().setMaxSessions(defaultMinSessions - 1).build();
    assertThat(options.getMaxSessions()).isEqualTo(defaultMinSessions - 1);
    assertThat(options.getMinSessions()).isEqualTo(defaultMinSessions - 1);
  }

  @Test
  public void setValidMinSessions() {
    SessionPoolOptions.newBuilder().setMinSessions(0).build();
    SessionPoolOptions.newBuilder().setMinSessions(1).build();
    SessionPoolOptions.newBuilder().setMinSessions(400).build();
    SessionPoolOptions.newBuilder()
        .setMaxSessions(Integer.MAX_VALUE)
        .setMinSessions(Integer.MAX_VALUE)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void setNegativeMinSessions() {
    SessionPoolOptions.newBuilder().setMinSessions(-1);
  }

  @Test
  public void setValidMaxSessions() {
    SessionPoolOptions.newBuilder().setMaxSessions(1).build();
    SessionPoolOptions.newBuilder().setMaxSessions(400).build();
    SessionPoolOptions.newBuilder().setMaxSessions(Integer.MAX_VALUE).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void setZeroMaxSessions() {
    SessionPoolOptions.newBuilder().setMaxSessions(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setNegativeMaxSessions() {
    SessionPoolOptions.newBuilder().setMaxSessions(-1);
  }

  @Test
  public void verifyDefaultInactiveTransactionRemovalOptions() {
    SessionPoolOptions sessionPoolOptions = SessionPoolOptions.newBuilder().build();
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        sessionPoolOptions.getInactiveTransactionRemovalOptions();

    assertTrue(sessionPoolOptions.warnInactiveTransactions());
    assertFalse(sessionPoolOptions.warnAndCloseInactiveTransactions());
    assertFalse(sessionPoolOptions.closeInactiveTransactions());
    assertEquals(0.95, inactiveTransactionRemovalOptions.getUsedSessionsRatioThreshold(), 0.0);
    assertEquals(Duration.ofMinutes(2), inactiveTransactionRemovalOptions.getExecutionFrequency());
    assertEquals(Duration.ofMinutes(60), inactiveTransactionRemovalOptions.getIdleTimeThreshold());
  }

  @Test
  public void setWarnIfInactiveTransactions() {
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder().setWarnIfInactiveTransactions().build();

    assertTrue(sessionPoolOptions.warnInactiveTransactions());
    assertFalse(sessionPoolOptions.warnAndCloseInactiveTransactions());
    assertFalse(sessionPoolOptions.closeInactiveTransactions());
  }

  @Test
  public void setWarnAndCloseIfInactiveTransactions() {
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder().setWarnAndCloseIfInactiveTransactions().build();

    assertFalse(sessionPoolOptions.warnInactiveTransactions());
    assertTrue(sessionPoolOptions.warnAndCloseInactiveTransactions());
    assertFalse(sessionPoolOptions.closeInactiveTransactions());
  }

  @Test
  public void setCloseIfInactiveTransactions() {
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptions.newBuilder().setCloseIfInactiveTransactions().build();

    assertFalse(sessionPoolOptions.warnInactiveTransactions());
    assertFalse(sessionPoolOptions.warnAndCloseInactiveTransactions());
    assertTrue(sessionPoolOptions.closeInactiveTransactions());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setNegativeExecutionFrequency() {
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setExecutionFrequency(Duration.ofMillis(-1))
            .build();
    SessionPoolOptions.newBuilder()
        .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setNegativeIdleTimeThreshold() {
    InactiveTransactionRemovalOptions inactiveTransactionRemovalOptions =
        InactiveTransactionRemovalOptions.newBuilder()
            .setIdleTimeThreshold(Duration.ofMillis(-1))
            .build();
    SessionPoolOptions.newBuilder()
        .setInactiveTransactionRemovalOptions(inactiveTransactionRemovalOptions);
  }
}
