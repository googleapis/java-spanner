/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfig;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for quickstart sample. */
@RunWith(JUnit4.class)
public class QuickstartSampleIT {
  private boolean ownedInstance = false;
  private String instanceId = System.getProperty("spanner.test.instance");
  private String dbId =
      System.getProperty("spanner.quickstart.database", SpannerSampleIT.formatForTest("sample"));
  private ByteArrayOutputStream bout;
  private PrintStream stdOut = System.out;
  private PrintStream out;

  @Before
  public void setUp() throws SpannerException, InterruptedException, ExecutionException {
    if (instanceId == null) {
      instanceId = SpannerSampleIT.formatForTest("quick");
      SpannerOptions options = SpannerOptions.newBuilder().build();
      try (Spanner spanner = options.getService()) {
        InstanceAdminClient instanceAdmin = spanner.getInstanceAdminClient();
        // Get first available instance config and create an instance.
        InstanceConfig config = instanceAdmin.listInstanceConfigs().iterateAll().iterator().next();
        instanceAdmin
            .createInstance(
                InstanceInfo.newBuilder(InstanceId.of(options.getProjectId(), instanceId))
                    .setDisplayName("samples-test")
                    .setInstanceConfigId(config.getId())
                    .setNodeCount(1)
                    .build())
            .get();
        ownedInstance = true;
        // Create a test database.
        spanner
            .getDatabaseAdminClient()
            .createDatabase(instanceId, dbId, ImmutableList.<String>of())
            .get();
      }
    }
    bout = new ByteArrayOutputStream();
    out = new PrintStream(bout);
    System.setOut(out);
  }

  @After
  public void tearDown() {
    System.setOut(stdOut);
    if (ownedInstance) {
      SpannerOptions options = SpannerOptions.newBuilder().build();
      try (Spanner spanner = options.getService()) {
        spanner.getInstanceAdminClient().deleteInstance(instanceId);
      }
    }
  }

  @Test
  public void testQuickstart() throws Exception {
    assertThat(instanceId).isNotNull();
    assertThat(dbId).isNotNull();
    QuickstartSample.main(instanceId, dbId);
    String got = bout.toString();
    assertThat(got).contains("1");
  }
}
