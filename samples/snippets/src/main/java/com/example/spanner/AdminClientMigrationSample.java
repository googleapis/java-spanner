package com.example.spanner;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.spanner.admin.instance.v1.CreateInstanceRequest;
import com.google.spanner.admin.instance.v1.InstanceConfigName;
import com.google.spanner.admin.instance.v1.ProjectName;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Samples which show how the legacy clients {@link DatabaseAdminClient} and
 * {@link InstanceAdminClient} can be used in combination with the newer clients
 * {@link com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient}
 * and {@link com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient}
 * within the same code snippet.
 */
public class AdminClientMigrationSample {

  /**
   * The sample first shows how an instance and a database can be created using the legacy clients.
   *
   * Post that we show how a customer can migrate to new
   * {@link com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient} interface and
   * use the same to create an instance. Customer may choose to stay on the legacy
   * {@link com.google.cloud.spanner.DatabaseAdminClient} for creating a database or can migrate to
   * the new interface {@link com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient}.
   *
   * @param projectId
   * @param instanceId1
   * @param databaseId1
   * @param instanceId2
   * @param databaseId2
   */
  static void migrateInstanceAdminClient(String projectId, String instanceId1, String databaseId1,
      String instanceId2, String databaseId2) {
    Spanner spanner = SpannerOptions.newBuilder().setProjectId(projectId).build().getService();

    // Below are the legacy interfaces for instance/database admin operations on spanner
    InstanceAdminClient legacyInstanceAdminClient = spanner.getInstanceAdminClient();
    DatabaseAdminClient legacyDatabaseAdminClient = spanner.getDatabaseAdminClient();

    /**
     * Here we use the new client object
     * {@link com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient}
     * for performing instance admin operations.
     */
    try(com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient instanceAdminClient =
        spanner.createInstanceAdminClient()) {

      // Set Instance configuration.
      final String configId = "regional-us-central1";
      final int nodeCount = 2;
      final String displayName = "Descriptive name";

      // Create an InstanceInfo object that will be used to create the instance.
      final InstanceInfo instance1 =
          InstanceInfo.newBuilder(InstanceId.of(projectId, instanceId1))
              .setInstanceConfigId(InstanceConfigId.of(projectId, configId))
              .setNodeCount(nodeCount)
              .setDisplayName(displayName)
              .build();
      // Create Instance using legacy client
      final Instance createdInstance1 = legacyInstanceAdminClient.createInstance(instance1).get();
      System.out.printf("Instance %s was successfully created%n", createdInstance1.getId());

      // Create Database
      final Database database1 =
          legacyDatabaseAdminClient.createDatabase(instanceId1, databaseId1,
              Arrays.asList(
                  "CREATE TABLE Singers ("
                      + "  SingerId   INT64 NOT NULL,"
                      + "  FirstName  STRING(1024),"
                      + "  LastName   STRING(1024),"
                      + "  SingerInfo BYTES(MAX),"
                      + "  FullName STRING(2048) AS "
                      + "  (ARRAY_TO_STRING([FirstName, LastName], \" \")) STORED"
                      + ") PRIMARY KEY (SingerId)",
                  "CREATE TABLE Albums ("
                      + "  SingerId     INT64 NOT NULL,"
                      + "  AlbumId      INT64 NOT NULL,"
                      + "  AlbumTitle   STRING(MAX)"
                      + ") PRIMARY KEY (SingerId, AlbumId),"
                      + "  INTERLEAVE IN PARENT Singers ON DELETE CASCADE")).get();
      System.out.printf("Database %s was successfully created%n", database1.getId());


      // Create an Instance object that will be used to create the instance.
      com.google.spanner.admin.instance.v1.Instance instance2 =
          com.google.spanner.admin.instance.v1.Instance.newBuilder()
              .setDisplayName(displayName)
              .setNodeCount(nodeCount)
              .setConfig(
                  InstanceConfigName.of(projectId, configId).toString())
              .build();

        // Wait for the createInstance operation to finish.
        com.google.spanner.admin.instance.v1.Instance createdInstance2 =
            instanceAdminClient.createInstanceAsync(
            CreateInstanceRequest.newBuilder()
                .setParent(ProjectName.of(projectId).toString())
                .setInstanceId(instanceId2)
                .setInstance(instance2).build()).get();
        System.out.printf("Instance %s was successfully created%n", createdInstance2.getName());

      // Create Database
      final Database database2 =
          legacyDatabaseAdminClient.createDatabase(instanceId2, databaseId2,
              Arrays.asList(
                  "CREATE TABLE Singers ("
                      + "  SingerId   INT64 NOT NULL,"
                      + "  FirstName  STRING(1024),"
                      + "  LastName   STRING(1024),"
                      + "  SingerInfo BYTES(MAX),"
                      + "  FullName STRING(2048) AS "
                      + "  (ARRAY_TO_STRING([FirstName, LastName], \" \")) STORED"
                      + ") PRIMARY KEY (SingerId)",
                  "CREATE TABLE Albums ("
                      + "  SingerId     INT64 NOT NULL,"
                      + "  AlbumId      INT64 NOT NULL,"
                      + "  AlbumTitle   STRING(MAX)"
                      + ") PRIMARY KEY (SingerId, AlbumId),"
                      + "  INTERLEAVE IN PARENT Singers ON DELETE CASCADE")).get();
      System.out.printf("Database %s was successfully created%n", database2.getId());
    } catch (ExecutionException e) {
      System.out.printf(
          "Error: Creating instance/database failed with error message %s%n", e.getMessage());
    } catch (InterruptedException e) {
      System.out.println("Error: Waiting for createInstance/createDatabase "
          + "operation to finish was interrupted");
    } finally {
      /**
       * Calling this will free up the resources consumed by the legacy admin clients
       * {@link DatabaseAdminClient} and {@link InstanceAdminClient}.
       *
       * For {@link com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient} we don't require
       * to invoke close() since it implements {@link AutoCloseable} and hence will automatically
       * free up resources when placed within a try-with-resources block.
       */
      spanner.close();
    }
  }
}
