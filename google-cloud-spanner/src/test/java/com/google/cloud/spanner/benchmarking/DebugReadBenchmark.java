package com.google.cloud.spanner.benchmarking;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class DebugReadBenchmark {

  @Test
  public void debugMonitoring() {
    Spanner spanner;
    DatabaseClient databaseClient;

    spanner = SpannerOptions.newBuilder()
        .setProjectId("span-cloud-testing")
        .build().getService();
    databaseClient = spanner.getDatabaseClient(
        DatabaseId.of("span-cloud-testing", "sakthi-spanner-testing", "benchmarking"));

    List<String> columns = new ArrayList<>();
    columns.add("id");
    columns.add("name");
    try (ReadContext readContext = databaseClient.singleUse()) {
      try (ResultSet resultSet = readContext.read("Employees",
          KeySet.singleKey(Key.of("2")), columns)) {
        while (resultSet.next()) {
          System.out.println(resultSet.getLong("id"));
        }
      }
    }
  }
}
