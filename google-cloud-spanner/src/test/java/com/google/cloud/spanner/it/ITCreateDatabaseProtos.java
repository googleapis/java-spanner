/*
 * Copyright 2020 Google LLC
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

package com.google.cloud.spanner.it;

import static com.google.cloud.spanner.testing.EmulatorSpannerHelper.isUsingEmulator;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeFalse;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.GetDatabaseDdlResponse;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.crypto.Data;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITCreateDatabaseProtos {

  private static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(2);
  private static final String VERSION_RETENTION_PERIOD = "7d";

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private RemoteSpannerHelper testHelper;
  private DatabaseAdminClient dbAdminClient;
  private List<Database> databasesToDrop;

  @BeforeClass
  public static void doNotRunOnEmulator() {
    assumeFalse("PITR-lite features are not supported by the emulator", isUsingEmulator());
  }

  @Before
  public void setUp() {
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
    databasesToDrop = new ArrayList<>();
  }

  @After
  public void tearDown() {
    for (Database database : databasesToDrop) {
      final DatabaseId id = database.getId();
      dbAdminClient.dropDatabase(id.getInstanceId().getInstance(), id.getDatabase());
    }
  }

  /*@Test
  public void returnsTheVersionRetentionPeriodSetThroughCreateDatabase() throws Exception {
    final String instanceId = testHelper.getInstanceId().getInstance();
    final String databaseId = testHelper.getUniqueDatabaseId();
    final String extraStatement =
        "ALTER DATABASE "
            + databaseId
            + " SET OPTIONS (version_retention_period = '"
            + VERSION_RETENTION_PERIOD
            + "')";

    final Database database = createDatabase(instanceId, databaseId, extraStatement);

    assertThat(database.getVersionRetentionPeriod()).isEqualTo(VERSION_RETENTION_PERIOD);
    assertThat(database.getEarliestVersionTime()).isNotNull();
  }*/

  @Test
  public void returnsTheVersionRetentionPeriodSetThroughGetDatabase() throws Exception {
    final String projectId = "span-cloud-testing";
    final String instanceId = "harsha-test-gcloud";
    final String databaseId = "singer_test";
    /*final String extraStatement =
    "ALTER DATABASE "
        + databaseId
        + " SET OPTIONS (version_retention_period = '"
        + VERSION_RETENTION_PERIOD
        + "')";*/

    createDatabase(projectId, instanceId, databaseId);
    final Database database = dbAdminClient.getDatabase(instanceId, databaseId);

    assertThat(database.getVersionRetentionPeriod()).isEqualTo(VERSION_RETENTION_PERIOD);
    assertThat(database.getEarliestVersionTime()).isNotNull();


    final GetDatabaseDdlResponse response = dbAdminClient.getDatabaseDdlWithProtoDescriptors("integration-test-proto-column", "int_test_proto_column_db");
    System.out.println(response.getProtoDescriptors().toByteArray());
  }

  /*@Test(expected = DatabaseNotFoundException.class)
  public void returnsAnErrorWhenAnInvalidVersionRetentionPeriodIsGiven() {
    final String instanceId = testHelper.getInstanceId().getInstance();
    final String databaseId = testHelper.getUniqueDatabaseId();
    final String extraStatement =
        "ALTER DATABASE " + databaseId + " SET OPTIONS (version_retention_period = '0d')";

    try {
      createDatabase(instanceId, databaseId, extraStatement);
      fail("Expected invalid argument error when setting invalid version retention period");
    } catch (Exception e) {
      SpannerException spannerException = (SpannerException) e.getCause();
      assertThat(spannerException.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }

    // Expects a database not found exception
    dbAdminClient.getDatabase(instanceId, databaseId);
  }*/

  private Database createDatabase(
      final String projectId, final String instanceId, final String databaseId) throws IOException {

    //String filePath = "/usr/local/google/home/sriharshach/github/Go/golang-samples-proto-support-v2/spanner/spanner_snippets/spanner/testdata/protos/descriptor.pb";
    // file to byte[], Path
    //byte[] bytes = Files.readAllBytes(Paths.get(filePath));

    InputStream inputStream = new FileInputStream("/usr/local/google/home/sriharshach/github/Go/golang-samples-proto-support-v2/spanner/spanner_snippets/spanner/testdata/protos/descriptors.pb");
    byte[] byteArray = null;

    // Try block to check for exceptions
    /*try {
      byteArray = inputStream.readAllBytes();
    }catch (IOException e) {
      System.out.println(e);
    }*/

    try {
      byteArray = ByteStreams.toByteArray(inputStream);
    }
    catch (IOException e) {
      System.out.println(e);
    }

    final Database databaseToCreate =
        dbAdminClient.newDatabaseBuilder(DatabaseId.of(projectId, instanceId, databaseId))
            .setProtoDescriptors(byteArray)
            .build();

    final OperationFuture<Database, CreateDatabaseMetadata> operation =
        dbAdminClient.createDatabase(
            databaseToCreate,
            Arrays.asList(
                "CREATE PROTO BUNDLE ("
                    + "spanner.examples.music.SingerInfo,"
                    + "spanner.examples.music.Genre,"
                    + ")",
                "CREATE TABLE Singers ("
                    + "  SingerId   INT64 NOT NULL,"
                    + "  FirstName  STRING(1024),"
                    + "  LastName   STRING(1024),"
                    + "  SingerInfo spanner.examples.music.SingerInfo,"
                    + "  SingerGenre spanner.examples.music.Genre,"
                    + "  ) PRIMARY KEY (SingerGenre)"));

    try {
      System.out.println("Waiting for operation to complete...");
      Database createdDatabase = operation.get(120, TimeUnit.SECONDS);

      System.out.printf(
          "Database %s created with encryption key %s%n",
          createdDatabase.getId(), createdDatabase.getEncryptionConfig().getKmsKeyName());

      return createdDatabase;
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw SpannerExceptionFactory.asSpannerException(e.getCause());
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    } catch (TimeoutException e) {
      // If the operation timed out propagates the timeout
      throw SpannerExceptionFactory.propagateTimeout(e);
    }
  }

  private Database createDatabaseWithProtos(
      final String projectId, final String instanceId, final String databaseId, InputStream protoDescriptorFile) {

    byte[] protoDescriptorByteArray = null;
    try {
      protoDescriptorByteArray = ByteStreams.toByteArray(protoDescriptorFile);
    }
    catch (IOException e) {
      System.out.println(e);
    }

    final Database databaseToCreate =
        dbAdminClient.newDatabaseBuilder(DatabaseId.of(projectId, instanceId, databaseId))
            .setProtoDescriptors(protoDescriptorByteArray)
            .build();

    final OperationFuture<Database, CreateDatabaseMetadata> operation =
        dbAdminClient.createDatabase(
            databaseToCreate,
            Arrays.asList(
                "CREATE PROTO BUNDLE ("
                    + "spanner.examples.music.SingerInfo,"
                    + "spanner.examples.music.Genre,"
                    + ")",
                "CREATE TABLE Singers ("
                    + "  SingerId   INT64 NOT NULL,"
                    + "  FirstName  STRING(1024),"
                    + "  LastName   STRING(1024),"
                    + "  SingerInfo spanner.examples.music.SingerInfo,"
                    + "  SingerGenre spanner.examples.music.Genre,"
                    + "  ) PRIMARY KEY (SingerGenre)"));

    try {
      System.out.println("Waiting for operation to complete...");
      Database createdDatabase = operation.get(120, TimeUnit.SECONDS);
      return createdDatabase;
    } catch (Exception e) {
      // If the operation failed during execution, expose the cause.
      throw SpannerExceptionFactory.asSpannerException(e.getCause());
    }
  }

  private void updateDatabaseWithProtos(
      final String instanceId, final String databaseId, InputStream protoDescriptorFile) {

    byte[] protoDescriptorByteArray = null;
    try {
      protoDescriptorByteArray = ByteStreams.toByteArray(protoDescriptorFile);
    }
    catch (IOException e) {
      System.out.println(e);
    }

    try {
      final OperationFuture<Void, UpdateDatabaseDdlMetadata> updateOperation =
          dbAdminClient.updateDatabaseDdl(
              instanceId,
              databaseId,
              Arrays.asList(
                  "CREATE PROTO BUNDLE ("
                      + "spanner.examples.music.SingerInfo,"
                      + "spanner.examples.music.Genre,"
                      + ")",
                  "CREATE TABLE Singers ("
                      + "  SingerId   INT64 NOT NULL,"
                      + "  FirstName  STRING(1024),"
                      + "  LastName   STRING(1024),"
                      + "  SingerInfo spanner.examples.music.SingerInfo,"
                      + "  SingerGenre spanner.examples.music.Genre,"
                      + "  ) PRIMARY KEY (SingerGenre)"),
              null, protoDescriptorByteArray);
      updateOperation.get();
      System.out.println("Updated Database");
    }  catch (Exception e) {
      // If the operation failed during execution, expose the cause.
      throw SpannerExceptionFactory.asSpannerException(e.getCause());
    }
  }

  private void getDatabaseDdl(
      String instanceId, String databaseId) {
    try {
      final GetDatabaseDdlResponse response = dbAdminClient.getDatabaseDdlWithProtoDescriptors(instanceId, databaseId);
      System.out.println("Retrieved GetDatabaseDdlResponse for " + databaseId);
      for (String ddl : response.getStatementsList()) {
        System.out.println(ddl);
      }
      System.out.println(response.getProtoDescriptors());
    } catch (Exception e) {
    }
  }
}
