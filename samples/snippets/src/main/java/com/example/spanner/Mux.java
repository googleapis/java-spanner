package com.example.spanner;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

class Mux {
  public static void main(String[] args){
    Logger.getLogger("com.google.cloud.grpc.GcpManagedChannel").setLevel(Level.FINEST);
    queryWithNumericParameter();
  }

  static void queryWithNumericParameter() {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "span-cloud-testing";
    String instanceId = "harsha-test-gcloud";
    String databaseId = "database1";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).enableGrpcGcpExtension().build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      queryWithNumericParameter(client);
    }
  }

  static void queryWithNumericParameter(DatabaseClient client) {
    Statement statement =
        Statement.newBuilder(
                "SELECT SingerId, FirstName, LastName FROM Singers")
            .build();
    try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        System.out.printf(
            "%d %s %s %n", resultSet.getLong("SingerId"), resultSet.getString("FirstName"), resultSet.getString("LastName"));
      }
    }
  }
}
