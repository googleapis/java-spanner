package com.google.cloud.spanner;

import com.google.common.base.Preconditions;
import com.google.spanner.v1.BatchWriteRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MutationGroup {
  private final List<Mutation> mutations;

  private MutationGroup(List<Mutation> mutations) {
    this.mutations = mutations;
  }

  public static MutationGroup of(Mutation... mutations) {
    Preconditions.checkArgument(mutations.length > 0, "Should pass in at least one mutation.");
    return new MutationGroup(Arrays.asList(mutations));
  }

  public List<Mutation> getMutations() {
    return mutations;
  }

  static BatchWriteRequest.MutationGroup toProto(final MutationGroup mutationGroup) {
    List<com.google.spanner.v1.Mutation> mutationsProto = new ArrayList<>();
    Mutation.toProto(mutationGroup.getMutations(), mutationsProto);
    return BatchWriteRequest.MutationGroup.newBuilder().addAllMutations(mutationsProto).build();
  }

  static List<BatchWriteRequest.MutationGroup> toListProto(
      final Iterable<MutationGroup> mutationGroups) {
    List<BatchWriteRequest.MutationGroup> mutationGroupsProto = new ArrayList<>();
    for (MutationGroup group : mutationGroups) {
      mutationGroupsProto.add(toProto(group));
    }
    return mutationGroupsProto;
  }
}
