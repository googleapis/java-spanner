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

  public DirectedReadOptions getDirectedReadOptions_BothIncludeExcludeReplicasSet() {
    return DirectedReadOptions.newBuilder()
        .setExcludeReplicas(
            ExcludeReplicas.newBuilder()
                .addReplicaSelections(ReplicaSelection.newBuilder().setLocation("us-east1").build())
                .build())
        .setIncludeReplicas(
            IncludeReplicas.newBuilder()
                .addReplicaSelections(ReplicaSelection.newBuilder().setLocation("us-west1").build())
                .build())
        .build();
  }

  @Test
  public void testDirectedReadOptions_BothIncludeExcludeReplicasSet() {
    // This test can be skipped because using the proto object directly will handle this case
    // automatically and will set only IncludeReplicas, if both IncludeReplicas and ExcludeReplicas
    // are passed in.
    DirectedReadOptions directedReadOptions =
        getDirectedReadOptions_BothIncludeExcludeReplicasSet();
    SpannerException e =
        assertThrows(
            SpannerException.class,
            () -> SpannerUtil.verifyDirectedReadOptions(directedReadOptions));
    assertEquals(e.getErrorCode(), ErrorCode.INVALID_ARGUMENT);
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
