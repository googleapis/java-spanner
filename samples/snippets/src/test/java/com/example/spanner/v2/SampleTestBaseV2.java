package com.example.spanner.v2;

import com.example.spanner.SampleIdGenerator;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base class for sample integration intests using auto-generated admin clients.
 */
public class SampleTestBaseV2 {

  private static final String BASE_DATABASE_ID =
      System.getProperty("spanner.sample.database", "sampledb");
  private static final String BASE_BACKUP_ID = "samplebk";
  private static final String BASE_INSTANCE_CONFIG_ID = "sampleconfig";

  protected static String projectId;
  protected static final String instanceId = System.getProperty("spanner.test.instance");
  protected static DatabaseAdminClient databaseAdminClient;
  protected static InstanceAdminClient instanceAdminClient;

  protected static final String multiRegionalInstanceId =
      System.getProperty("spanner.test.instance.mr");
  protected static final String instanceConfigName = System
      .getProperty("spanner.test.instance.config");
  protected static SampleIdGenerator idGenerator;

  @BeforeClass
  public static void beforeClass() throws IOException {
    final String serverUrl = "";
    final SpannerOptions.Builder optionsBuilder = SpannerOptions
        .newBuilder()
        .setAutoThrottleAdministrativeRequests();
    if (!serverUrl.isEmpty()) {
      optionsBuilder.setHost(serverUrl);
    }
    final SpannerOptions options = optionsBuilder.build();
    projectId = options.getProjectId();
    databaseAdminClient = DatabaseAdminClient.create();
    instanceAdminClient = InstanceAdminClient.create();
    idGenerator = new SampleIdGenerator(BASE_DATABASE_ID, BASE_BACKUP_ID, BASE_INSTANCE_CONFIG_ID);
  }

  @AfterClass
  public static void afterClass() {
    for (String databaseId : idGenerator.getDatabaseIds()) {
      System.out.println("Trying to drop " + databaseId);
      try {
        // If the database is not found, it is ignored (no exception is thrown)
        databaseAdminClient.dropDatabase(
            getDatabaseName(projectId, instanceId, databaseId));
        databaseAdminClient.dropDatabase(
            getDatabaseName(projectId, multiRegionalInstanceId, databaseId));
      } catch (Exception e) {
        System.out.println(
            "Failed to drop database "
                + databaseId
                + " due to "
                + e.getMessage()
                + ", skipping...");
      }
    }
    for (String backupId : idGenerator.getBackupIds()) {
      try {
        // If the backup is not found, it is ignored (no exception is thrown)
        databaseAdminClient.deleteBackup(
            getBackupName(projectId, instanceId, backupId));
        databaseAdminClient.deleteBackup(
            getBackupName(projectId, multiRegionalInstanceId, backupId));
      } catch (Exception e) {
        System.out.println(
            "Failed to delete backup " + backupId + " due to " + e.getMessage() + ", skipping...");
      }
    }
    for (String configId : idGenerator.getInstanceConfigIds()) {
      try {
        // If the config is not found, it is ignored (no exception is thrown)
        instanceAdminClient.deleteInstanceConfig(configId);
      } catch (Exception e) {
        System.out.println(
            "Failed to delete instance config "
                + configId
                + " due to "
                + e.getMessage()
                + ", skipping...");
      }
    }
  }

  private static String getDatabaseName(final String projectId,
      final String instanceId, final String databaseId) {
    return String.format(
        "projects/%s/instances/%s/databases/%s", projectId, instanceId, databaseId);
  }

  private static String getBackupName(final String projectId,
      final String instanceId, final String backupId) {
    return String.format(
        "projects/%s/instances/%s/backups/%s", projectId, instanceId, backupId);
  }
}
