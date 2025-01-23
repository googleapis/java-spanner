package com.google.cloud.spanner.benchmark;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.SessionPoolOptionsHelper;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DataGenerator {

  private static final String CREATE_TABLE_QUERY =
      "CREATE TABLE Employees (ID INT64 NOT NULL, NAME STRING(30)) PRIMARY KEY(ID)";

  public static void main(String[] args)
      throws ExecutionException, InterruptedException, TimeoutException {
    String project = System.getenv("SPANNER_CLIENT_BENCHMARK_GOOGLE_CLOUD_PROJECT");
    String instance = System.getenv("SPANNER_CLIENT_BENCHMARK_SPANNER_INSTANCE");
    String database = System.getenv("SPANNER_CLIENT_BENCHMARK_SPANNER_DATABASE");

    String fullyQualifiedDatabase = null;
    if (project != null && instance != null && database != null) {
      fullyQualifiedDatabase =
          String.format("projects/%s/instances/%s/databases/%s", project, instance, database);
    } else {
      throw new IllegalArgumentException(
          "You must either set all the environment variables SPANNER_CLIENT_BENCHMARK_GOOGLE_CLOUD_PROJECT, SPANNER_CLIENT_BENCHMARK_SPANNER_INSTANCE and SPANNER_CLIENT_BENCHMARK_SPANNER_DATABASE, or specify a value for the command line argument --database");
    }

    DatabaseId databaseId = DatabaseId.of(fullyQualifiedDatabase);
    try (Spanner spanner = getSpannerOptions(databaseId).getService()) {
      // Create database and tables
      DatabaseAdminClient adminClient = spanner.getDatabaseAdminClient();
      createDatabaseAndTables(adminClient, databaseId);
      System.out.println("Database and Tables are created");

      // Insert Rows into the table
      System.out.println("Inserting rows");
      DatabaseClient databaseClient = spanner.getDatabaseClient(databaseId);
      System.out.println(databaseId);
      for (int i = 0; i < 100; i++) {
        List<Mutation> mutationList = new ArrayList<>();
        for (int j = 0; j < 1000; j++) {
          int row = i * 1000 + j;
          Mutation mutation =
              Mutation.newInsertBuilder("Employees")
                  .set("ID")
                  .to(row)
                  .set("NAME")
                  .to("RANDOM")
                  .build();
          mutationList.add(mutation);
        }
        databaseClient.write(mutationList);
      }
      System.out.println("Inserting rows completed");
    }
  }

  private static void createDatabaseAndTables(
      DatabaseAdminClient adminClient, DatabaseId databaseId)
      throws ExecutionException, InterruptedException, TimeoutException {
    adminClient
        .createDatabase(
            adminClient.newDatabaseBuilder(databaseId).build(),
            Collections.singleton(CREATE_TABLE_QUERY))
        .get(60, TimeUnit.SECONDS);
  }

  private static SpannerOptions getSpannerOptions(DatabaseId databaseId) {
    SessionPoolOptions sessionPoolOptions =
        SessionPoolOptionsHelper.setUseMultiplexedSession(SessionPoolOptions.newBuilder(), true)
            .build();

    System.out.println(databaseId.toString());

    return SpannerOptions.newBuilder()
        .setProjectId(databaseId.getInstanceId().getProject())
        .setSessionPoolOption(sessionPoolOptions)
        .setHost("https://spanner.googleapis.com")
        .build();
  }
}
