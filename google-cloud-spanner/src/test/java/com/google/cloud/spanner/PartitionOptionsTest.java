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

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.cloud.spanner.PartitionOptions}. */
@RunWith(JUnit4.class)
public class PartitionOptionsTest {

  @Test
  public void builder() {
    long desiredBytesPerBatch = 1 << 30;
    long maxPartitionCount = 100;
    PartitionOptions parameters =
        PartitionOptions.newBuilder()
            .setPartitionSizeBytes(desiredBytesPerBatch)
            .setMaxPartitions(maxPartitionCount)
            .build();
    assertThat(parameters.getPartitionSizeBytes()).isEqualTo(desiredBytesPerBatch);
    assertThat(parameters.getMaxPartitions()).isEqualTo(maxPartitionCount);

    // Test serialization.
    reserializeAndAssert(parameters);
  }

  @Test
  public void emptyBuilder() {
    PartitionOptions parameters = PartitionOptions.newBuilder().build();
    assertThat(parameters.getPartitionSizeBytes()).isEqualTo(0);
    assertThat(parameters.getMaxPartitions()).isEqualTo(0);

    // Test serialization.
    reserializeAndAssert(parameters);
  }

  @Test
  public void defaultBuilder() {
    PartitionOptions parameters = PartitionOptions.getDefaultInstance();
    assertThat(parameters.getPartitionSizeBytes()).isEqualTo(0);
    assertThat(parameters.getMaxPartitions()).isEqualTo(0);

    // Test serialization.
    reserializeAndAssert(parameters);
  }

  @Test
  public void equalAndHashCode() {
    new EqualsTester()
        .addEqualityGroup(
            PartitionOptions.newBuilder()
                .setPartitionSizeBytes(1 << 30)
                .setMaxPartitions(100)
                .build(),
            PartitionOptions.newBuilder()
                .setPartitionSizeBytes(1 << 30)
                .setMaxPartitions(100)
                .build())
        .addEqualityGroup(
            PartitionOptions.newBuilder().build(), PartitionOptions.getDefaultInstance())
        .testEquals();
  }

  @Test
  public void invalidDesiredBytesPerBatch() {
    try {
      PartitionOptions.newBuilder().setPartitionSizeBytes(-1).build();
      fail("Expected exception");
    } catch (IllegalArgumentException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void invalidMaxPartitionCount() {
    try {
      PartitionOptions.newBuilder().setMaxPartitions(-1).build();
      fail("Expected exception");
    } catch (IllegalArgumentException ex) {
      assertNotNull(ex.getMessage());
    }
  }
}
