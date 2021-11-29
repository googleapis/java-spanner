/*
 * Copyright 2021 Google Inc.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for ChangeStreamSample.
 */
@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class ChangeStreamSampleIT {
  private static String projectId = System.getProperty("spanner.test.project");
  private static String instanceId = System.getProperty("spanner.test.instance");

  private ByteArrayOutputStream bout;
  private PrintStream stdOut = System.out;
  private PrintStream out;

  @Before
  public void setUp() {
    bout = new ByteArrayOutputStream();
    out = new PrintStream(bout);
    System.setOut(out);
  }

  @After
  public void tearDown() {
    System.setOut(stdOut);
  }

  @Test
  public void testQuickstart() {
    assertNotNull(projectId);
    assertNotNull(instanceId);
    ChangeStreamSample.main(projectId, instanceId);
    String got = bout.toString();
    Assert.assertThat(got, containsString("Received a ChildPartitionsRecord"));
    Assert.assertThat(got, containsString("Received a HeartbeatRecord"));
    Assert.assertThat(got, containsString("Received a DataChangeRecord"));
    Assert.assertThat(got, containsString("mods=[Mod{keysJson={\"SingerId\":\"1\"}, "
        + "oldValuesJson='', "
        + "newValuesJson='{\"FirstName\":\"Melissa\",\"LastName\":\"Garcia\"}'}, "
        + "Mod{keysJson={\"SingerId\":\"2\"}, "
        + "oldValuesJson='', "
        + "newValuesJson='{\"FirstName\":\"Dylan\",\"LastName\":\"Shaw\"}'}]"));
  }
}
