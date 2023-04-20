/*
 * Copyright 2023 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.DirectedReadOptions.ExcludeReplicas;
import com.google.spanner.v1.DirectedReadOptions.IncludeReplicas;
import com.google.spanner.v1.DirectedReadOptions.ReplicaSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.cloud.spanner.SpannerUtil}. */
@RunWith(JUnit4.class)
public class SpannerUtilTest {
  private DirectedReadOptions
      getDirectedReadOptions_IncludeReplica_ReplicaSelectionCountGreaterThanMax() {
    List<ReplicaSelection> replicaSelectionList =
        new ArrayList<>(
            Collections.nCopies(
                SpannerUtil.MAX_REPLICA_SELECTIONS_COUNT + 1,
                ReplicaSelection.newBuilder().setLocation("us-west1").build()));
    return DirectedReadOptions.newBuilder()
        .setIncludeReplicas(
            IncludeReplicas.newBuilder().addAllReplicaSelections(replicaSelectionList))
        .build();
  }

  private DirectedReadOptions
      getDirectedReadOptions_ExcludeReplica_ReplicaSelectionCountGreaterThanMax() {
    List<ReplicaSelection> replicaSelectionList =
        new ArrayList<>(
            Collections.nCopies(
                SpannerUtil.MAX_REPLICA_SELECTIONS_COUNT + 1,
                ReplicaSelection.newBuilder().setLocation("us-east1").build()));
    return DirectedReadOptions.newBuilder()
        .setExcludeReplicas(
            ExcludeReplicas.newBuilder().addAllReplicaSelections(replicaSelectionList))
        .build();
  }

  @Test
  public void testDirectedReadOptions_IncludeReplica_ReplicaSelectionCountGreaterThanMax() {
    DirectedReadOptions directedReadOptions =
        getDirectedReadOptions_IncludeReplica_ReplicaSelectionCountGreaterThanMax();
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> SpannerUtil.verifyDirectedReadOptions(directedReadOptions));
    assertEquals(e.getErrorCode(), ErrorCode.INVALID_ARGUMENT);
  }

  @Test
  public void testDirectedReadOptions_ExcludeReplica_ReplicaSelectionCountGreaterThanMax() {
    DirectedReadOptions directedReadOptions =
        getDirectedReadOptions_ExcludeReplica_ReplicaSelectionCountGreaterThanMax();
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> SpannerUtil.verifyDirectedReadOptions(directedReadOptions));
    assertEquals(e.getErrorCode(), ErrorCode.INVALID_ARGUMENT);
  }
}
