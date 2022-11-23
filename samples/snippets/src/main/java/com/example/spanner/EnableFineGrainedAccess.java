package com.example.spanner;

// [START spanner_enable_fine_grained_access]
import com.google.cloud.Binding;
import com.google.cloud.Condition;
import com.google.cloud.Policy;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.ArrayList;
import java.util.List;

public class EnableFineGrainedAccess {

  static void enableFineGrainedAccess() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";
    String iamMember = "user:alice@example.com";
    String role = "new-parent";
    String title = "my condition title";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseAdminClient adminClient = spanner.getDatabaseAdminClient();
      enableFineGrainedAccess(adminClient, instanceId, databaseId, iamMember, title, role);
    }
  }

  static void enableFineGrainedAccess(
      DatabaseAdminClient adminClient,
      String instanceId,
      String databaseId,
      String iamMember,
      String title,
      String role) {
    Policy policy = adminClient.getDatabaseIAMPolicy(instanceId, databaseId, 3);
    int policyVersion = policy.getVersion();
    if (policy.getVersion() < 3) {
      policyVersion = 3;
    }
    List<String> members = new ArrayList<>();
    members.add(iamMember);
    List<Binding> bindings = new ArrayList<>(policy.getBindingsList());

    bindings.add(
        Binding.newBuilder()
            .setRole("roles/spanner.fineGrainedAccessUser")
            .setMembers(members)
            .build());

    bindings.add(
        Binding.newBuilder()
            .setRole("roles/spanner.databaseRoleUser")
            .setCondition(
                Condition.newBuilder()
                    .setDescription(title)
                    .setExpression(
                        String.format("resource.name.endsWith(\"/databaseRoles/%s\")", role))
                    .setTitle(title)
                    .build())
            .setMembers(members)
            .build());

    Policy policyWithConditions =
        Policy.newBuilder()
            .setVersion(policyVersion)
            .setEtag(policy.getEtag())
            .setBindings(bindings)
            .build();
    Policy response =
        adminClient.setDatabaseIAMPolicy(instanceId, databaseId, policyWithConditions);
    System.out.printf(
        "Enabled fine-grained access in IAM with version %d%n", response.getVersion());
  }
}
// [END spanner_enable_fine_grained_access]
