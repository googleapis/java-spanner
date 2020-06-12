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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InstanceConfigId}. */
@RunWith(JUnit4.class)
public class InstanceConfigIdTest {

  @Test
  public void basic() {
    String name = "projects/test-project/instanceConfigs/test-instance-config";
    InstanceConfigId config = InstanceConfigId.of(name);
    assertThat(config.getProject()).isEqualTo("test-project");
    assertThat(config.getInstanceConfig()).isEqualTo("test-instance-config");
    assertThat(config.getName()).isEqualTo(name);
    assertThat(InstanceConfigId.of(name)).isEqualTo(config);
    assertThat(InstanceConfigId.of("test-project", "test-instance-config")).isEqualTo(config);
    assertThat(InstanceConfigId.of(name).hashCode()).isEqualTo(config.hashCode());
  }

  @Test
  public void badName() {
    try {
      InstanceConfigId.of("bad name");
      fail("Expected exception");
    } catch (IllegalArgumentException e) {
      assertNotNull(e.getMessage());
    }
  }
}
