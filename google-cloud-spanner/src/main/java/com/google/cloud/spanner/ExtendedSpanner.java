package com.google.cloud.spanner;

public interface ExtendedSpanner extends Spanner {
  /**
   * Returns a {@code DatabaseClient} for the given database and given client id. It uses a pool of
   * sessions to talk to the database.
   * <!--SNIPPET get_db_client-->
   *
   * <pre>{@code
   * SpannerOptions options = SpannerOptions.newBuilder().build();
   * Spanner spanner = options.getService();
   * final String project = "test-project";
   * final String instance = "test-instance";
   * final String database = "example-db";
   * final String client_id = "client_id"
   * DatabaseId db =
   *     DatabaseId.of(project, instance, database);
   *
   * DatabaseClient dbClient = spanner.getDatabaseClient(db, client_id);
   * }</pre>
   *
   * <!--SNIPPET get_db_client-->
   */
  default DatabaseClient getDatabaseClient(DatabaseId db, String clientId) {
    throw new UnsupportedOperationException(
        "getDatabaseClient with clientId is not supported by this default implementation.");
  }
}
