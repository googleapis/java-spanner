/*
 * Copyright 2021 Google LLC
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.Timestamp;
import com.google.spanner.v1.CommitResponse.CommitStats;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CommitResponseTest {

  @Test
  public void testFromProto() {
    long mutationCount = 5L;
    com.google.protobuf.Timestamp timestamp =
        com.google.protobuf.Timestamp.newBuilder().setSeconds(123L).setNanos(456).build();
    com.google.spanner.v1.CommitResponse proto =
        com.google.spanner.v1.CommitResponse.newBuilder()
            .setCommitStats(
                com.google.spanner.v1.CommitResponse.CommitStats.newBuilder()
                    .setMutationCount(mutationCount)
                    .build())
            .setCommitTimestamp(timestamp)
            .build();

    CommitResponse response = new CommitResponse(proto);

    assertThat(response.getCommitTimestamp()).isEqualTo(Timestamp.ofTimeSecondsAndNanos(123L, 456));
    assertThat(response.getCommitStats().getMutationCount()).isEqualTo(mutationCount);
  }

  @Test
  public void testEqualsAndHashCode() {
    com.google.spanner.v1.CommitResponse proto1 =
        com.google.spanner.v1.CommitResponse.newBuilder()
            .setCommitTimestamp(com.google.protobuf.Timestamp.newBuilder().setSeconds(1L).build())
            .build();
    com.google.spanner.v1.CommitResponse proto2 =
        com.google.spanner.v1.CommitResponse.newBuilder()
            .setCommitTimestamp(com.google.protobuf.Timestamp.newBuilder().setSeconds(2L).build())
            .build();
    com.google.spanner.v1.CommitResponse proto3 =
        com.google.spanner.v1.CommitResponse.newBuilder()
            .setCommitTimestamp(com.google.protobuf.Timestamp.newBuilder().setSeconds(1L).build())
            .build();

    CommitResponse response1 = new CommitResponse(proto1);
    CommitResponse response2 = new CommitResponse(proto2);
    CommitResponse response3 = new CommitResponse(proto3);

    assertThat(response1).isEqualTo(response3);
    assertThat(response1).isNotEqualTo(response2);
    assertThat(response2).isNotEqualTo(response3);
    assertThat(response1.equals(null)).isFalse();
    assertThat(response1.equals(new Object())).isFalse();

    assertThat(response1.hashCode()).isEqualTo(response3.hashCode());
    assertThat(response1.hashCode()).isNotEqualTo(response2.hashCode());
    assertThat(response2.hashCode()).isNotEqualTo(response3.hashCode());
  }

  @Test
  public void testHasCommitStats() {
    com.google.spanner.v1.CommitResponse protoWithoutCommitStats =
        com.google.spanner.v1.CommitResponse.getDefaultInstance();
    CommitResponse responseWithoutCommitStats = new CommitResponse(protoWithoutCommitStats);
    assertFalse(responseWithoutCommitStats.hasCommitStats());

    com.google.spanner.v1.CommitResponse protoWithCommitStats =
        com.google.spanner.v1.CommitResponse.newBuilder()
            .setCommitStats(CommitStats.getDefaultInstance())
            .build();
    CommitResponse responseWithCommitStats = new CommitResponse(protoWithCommitStats);
    assertTrue(responseWithCommitStats.hasCommitStats());
  }
}
