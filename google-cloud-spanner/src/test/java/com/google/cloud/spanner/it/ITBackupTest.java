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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.api.gax.rpc.FailedPreconditionException;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Backup;
import com.google.cloud.spanner.BackupId;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.longrunning.Operation;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import com.google.spanner.admin.database.v1.RestoreSourceType;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests creating, reading, updating and deleting backups. This test class combines
 * several tests into one long test to reduce the total execution time.
 */
@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITBackupTest {
  private static final Logger logger = Logger.getLogger(ITBackupTest.class.getName());
  private static final String EXPECTED_OP_NAME_FORMAT = "%s/backups/%s/operations/";
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  private DatabaseAdminClient dbAdminClient;
  private InstanceAdminClient instanceAdminClient;
  private Instance instance;
  private RemoteSpannerHelper testHelper;
  private final AtomicInteger backupSeq = new AtomicInteger();
  private List<String> databases = new ArrayList<>();
  private List<String> backups = new ArrayList<>();
  private final Random random = new Random();

  @BeforeClass
  public static void doNotRunOnEmulator() {
    assumeFalse("backups are not supported on the emulator", env.getTestHelper().isEmulator());
  }

  @Before
  public void setUp() throws Exception {
    logger.info("Setting up tests");
    testHelper = env.getTestHelper();
    dbAdminClient = testHelper.getClient().getDatabaseAdminClient();
    instanceAdminClient = testHelper.getClient().getInstanceAdminClient();
    instance = instanceAdminClient.getInstance(testHelper.getInstanceId().getInstance());
    logger.info("Finished setup");

    // Cancel any backup operation that has been started by this integration test if it has been
    // running for at least 6 hours.
    logger.info("Cancelling long-running test backup operations");
    Pattern pattern = Pattern.compile(".*/backups/testbck_\\d{6}_\\d{4}_bck\\d/operations/.*");
    try {
      for (Operation operation :
          dbAdminClient.listBackupOperations(instance.getId().getInstance()).iterateAll()) {
        Matcher matcher = pattern.matcher(operation.getName());
        if (matcher.matches()) {
          if (!operation.getDone()) {
            Timestamp currentTime = Timestamp.now();
            Timestamp startTime =
                Timestamp.fromProto(
                    operation
                        .getMetadata()
                        .unpack(CreateBackupMetadata.class)
                        .getProgress()
                        .getStartTime());
            long diffSeconds = currentTime.getSeconds() - startTime.getSeconds();
            if (TimeUnit.HOURS.convert(diffSeconds, TimeUnit.SECONDS) >= 6L) {
              logger.warning(
                  String.format(
                      "Cancelling test backup operation %s that was started at %s",
                      operation.getName(), startTime.toString()));
              dbAdminClient.cancelOperation(operation.getName());
            }
          }
        }
      }
    } catch (InvalidProtocolBufferException e) {
      logger.log(Level.WARNING, "Could not list all existing backup operations.", e);
    }
    logger.info("Finished checking existing test backup operations");
  }

  @After
  public void tearDown() throws Exception {
    for (String backup : backups) {
      waitForDbOperations(backup);
      dbAdminClient.deleteBackup(testHelper.getInstanceId().getInstance(), backup);
    }
    backups.clear();
    for (String db : databases) {
      dbAdminClient.dropDatabase(testHelper.getInstanceId().getInstance(), db);
    }
  }

  private void waitForDbOperations(String backupId) throws InterruptedException {
    try {
      Backup backupMetadata =
          dbAdminClient.getBackup(testHelper.getInstanceId().getInstance(), backupId);
      boolean allDbOpsDone = false;
      while (!allDbOpsDone) {
        allDbOpsDone = true;
        for (String referencingDb : backupMetadata.getProto().getReferencingDatabasesList()) {
          String filter =
              String.format(
                  "name:%s/operations/ AND "
                      + "(metadata.@type:type.googleapis.com/"
                      + "google.spanner.admin.database.v1.OptimizeRestoredDatabaseMetadata)",
                  referencingDb);
          for (Operation op :
              dbAdminClient
                  .listDatabaseOperations(
                      testHelper.getInstanceId().getInstance(), Options.filter(filter))
                  .iterateAll()) {
            if (!op.getDone()) {
              Thread.sleep(5000L);
              allDbOpsDone = false;
              break;
            }
          }
        }
      }
    } catch (SpannerException e) {
      if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
        return;
      }
      throw e;
    }
  }

  private String getUniqueBackupId() {
    return String.format("testbck_%06d_%04d", random.nextInt(1000000), backupSeq.incrementAndGet());
  }

  private static Timestamp after7Days() {
    return Timestamp.ofTimeMicroseconds(
        TimeUnit.MICROSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            + TimeUnit.MICROSECONDS.convert(7L, TimeUnit.DAYS));
  }

  private Timestamp after5Minutes() {
    return Timestamp.ofTimeMicroseconds(
        TimeUnit.MICROSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            + TimeUnit.MICROSECONDS.convert(5L, TimeUnit.MINUTES));
  }

  private Timestamp tomorrow() {
    return Timestamp.ofTimeMicroseconds(
        TimeUnit.MICROSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            + TimeUnit.MICROSECONDS.convert(1L, TimeUnit.DAYS));
  }

  private Timestamp yesterday() {
    return Timestamp.ofTimeMicroseconds(
        TimeUnit.MICROSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            - TimeUnit.MICROSECONDS.convert(1L, TimeUnit.DAYS));
  }

  @Test
  public void testBackups() throws InterruptedException, ExecutionException {
    // Create two test databases in parallel.
    String db1Id = testHelper.getUniqueDatabaseId() + "_db1";
    logger.info(String.format("Creating test database %s", db1Id));
    OperationFuture<Database, CreateDatabaseMetadata> dbOp1 =
        dbAdminClient.createDatabase(
            testHelper.getInstanceId().getInstance(),
            db1Id,
            Arrays.asList("CREATE TABLE FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"));
    String db2Id = testHelper.getUniqueDatabaseId() + "_db2";
    logger.info(String.format("Creating test database %s", db2Id));
    OperationFuture<Database, CreateDatabaseMetadata> dbOp2 =
        dbAdminClient.createDatabase(
            testHelper.getInstanceId().getInstance(),
            testHelper.getUniqueDatabaseId() + "_db2",
            Arrays.asList("CREATE TABLE BAR (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)"));
    // Make sure all databases are created before we try to create any backups.
    Database db1 = dbOp1.get();
    Database db2 = dbOp2.get();
    databases.add(db1.getId().getDatabase());
    databases.add(db2.getId().getDatabase());
    // Insert some data into db2 to make sure the backup will have a size>0.
    DatabaseClient client = testHelper.getDatabaseClient(db2);
    client.writeAtLeastOnce(
        Arrays.asList(
            Mutation.newInsertOrUpdateBuilder("BAR")
                .set("ID")
                .to(1L)
                .set("NAME")
                .to("TEST")
                .build()));

    // Create two backups in parallel.
    String backupId1 = getUniqueBackupId() + "_bck1";
    String backupId2 = getUniqueBackupId() + "_bck2";
    Timestamp expireTime = after7Days();
    logger.info(String.format("Creating backups %s and %s in parallel", backupId1, backupId2));
    OperationFuture<Backup, CreateBackupMetadata> op1 =
        dbAdminClient.createBackup(
            testHelper.getInstanceId().getInstance(),
            backupId1,
            db1.getId().getDatabase(),
            expireTime);
    OperationFuture<Backup, CreateBackupMetadata> op2 =
        dbAdminClient.createBackup(
            testHelper.getInstanceId().getInstance(),
            backupId2,
            db2.getId().getDatabase(),
            expireTime);
    backups.add(backupId1);
    backups.add(backupId2);

    // Execute metadata tests as part of this integration test to reduce total execution time.
    testMetadata(op1, op2, backupId1, backupId2, db1, db2);

    // Ensure both backups have been created before we proceed.
    logger.info("Waiting for backup operations to finish");
    Backup backup1 = null;
    Backup backup2 = null;
    Stopwatch watch = Stopwatch.createStarted();
    try {
      backup1 = op1.get(6L, TimeUnit.MINUTES);
      backup2 = op2.get(Math.max(1L, 6L - watch.elapsed(TimeUnit.MINUTES)), TimeUnit.MINUTES);
    } catch (TimeoutException e) {
      logger.warning(
          "Waiting for backup operations to finish timed out. Getting long-running operations.");
      while (watch.elapsed(TimeUnit.MINUTES) < 12L
          && (!dbAdminClient.getOperation(op1.getName()).getDone()
              || !dbAdminClient.getOperation(op2.getName()).getDone())) {
        Thread.sleep(10_000L);
      }
      if (!dbAdminClient.getOperation(op1.getName()).getDone()) {
        logger.warning(String.format("Operation %s still not finished", op1.getName()));
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.DEADLINE_EXCEEDED,
            "Backup1 still not finished. Test is giving up waiting for it.");
      }
      if (!dbAdminClient.getOperation(op2.getName()).getDone()) {
        logger.warning(String.format("Operation %s still not finished", op2.getName()));
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.DEADLINE_EXCEEDED,
            "Backup2 still not finished. Test is giving up waiting for it.");
      }
      logger.info("Long-running operations finished. Getting backups by id.");
      backup1 = dbAdminClient.getBackup(instance.getId().getInstance(), backupId1);
      backup2 = dbAdminClient.getBackup(instance.getId().getInstance(), backupId2);
    }
    // Insert some more data into db2 to get a timestamp from the server.
    Timestamp commitTs =
        client.writeAtLeastOnce(
            Arrays.asList(
                Mutation.newInsertOrUpdateBuilder("BAR")
                    .set("ID")
                    .to(2L)
                    .set("NAME")
                    .to("TEST2")
                    .build()));

    // Test listing operations.
    // List all backups.
    logger.info("Listing all backups");
    assertThat(instance.listBackups().iterateAll()).containsAtLeast(backup1, backup2);
    // List all backups whose names contain 'bck1'.
    logger.info("Listing backups with name bck1");
    assertThat(
            dbAdminClient
                .listBackups(
                    testHelper.getInstanceId().getInstance(),
                    Options.filter(String.format("name:%s", backup1.getId().getName())))
                .iterateAll())
        .containsExactly(backup1);
    logger.info("Listing ready backups");
    Iterable<Backup> readyBackups =
        dbAdminClient
            .listBackups(testHelper.getInstanceId().getInstance(), Options.filter("state:READY"))
            .iterateAll();
    assertThat(readyBackups).containsAtLeast(backup1, backup2);
    // List all backups for databases whose names contain 'db1'.
    logger.info("Listing backups for database db1");
    assertThat(
            dbAdminClient
                .listBackups(
                    testHelper.getInstanceId().getInstance(),
                    Options.filter(String.format("database:%s", db1.getId().getName())))
                .iterateAll())
        .containsExactly(backup1);
    // List all backups that were created before a certain time.
    Timestamp ts = Timestamp.ofTimeSecondsAndNanos(commitTs.getSeconds(), 0);
    logger.info(String.format("Listing backups created before %s", ts));
    assertThat(
            dbAdminClient
                .listBackups(
                    testHelper.getInstanceId().getInstance(),
                    Options.filter(String.format("create_time<\"%s\"", ts)))
                .iterateAll())
        .containsAtLeast(backup1, backup2);
    // List all backups with a size > 0.
    logger.info("Listing backups with size>0");
    assertThat(
            dbAdminClient
                .listBackups(
                    testHelper.getInstanceId().getInstance(), Options.filter("size_bytes>0"))
                .iterateAll())
        .contains(backup2);
    assertThat(
            dbAdminClient
                .listBackups(
                    testHelper.getInstanceId().getInstance(), Options.filter("size_bytes>0"))
                .iterateAll())
        .doesNotContain(backup1);

    // Test pagination.
    testPagination(2);
    logger.info("Finished listBackup tests");

    // Execute other tests as part of this integration test to reduce total execution time.
    testGetBackup(db2, backupId2, expireTime);
    testUpdateBackup(backup1);
    testCreateInvalidExpirationDate(db1);
    testRestore(backup1, op1);

    testDelete(backupId2);
    testCancelBackupOperation(db1);
    // Finished all tests.
    logger.info("Finished all backup tests");
  }

  private void testMetadata(
      OperationFuture<Backup, CreateBackupMetadata> op1,
      OperationFuture<Backup, CreateBackupMetadata> op2,
      String backupId1,
      String backupId2,
      Database db1,
      Database db2)
      throws InterruptedException, ExecutionException {

    logger.info("Getting operation metadata 1");
    CreateBackupMetadata metadata1 = op1.getMetadata().get();
    logger.info("Getting operation metadata 2");
    CreateBackupMetadata metadata2 = op2.getMetadata().get();
    String expectedOperationName1 =
        String.format(EXPECTED_OP_NAME_FORMAT, testHelper.getInstanceId().getName(), backupId1);
    String expectedOperationName2 =
        String.format(EXPECTED_OP_NAME_FORMAT, testHelper.getInstanceId().getName(), backupId2);
    assertThat(op1.getName()).startsWith(expectedOperationName1);
    assertThat(op2.getName()).startsWith(expectedOperationName2);
    assertThat(metadata1.getDatabase()).isEqualTo(db1.getId().getName());
    assertThat(metadata2.getDatabase()).isEqualTo(db2.getId().getName());
    assertThat(metadata1.getName())
        .isEqualTo(BackupId.of(testHelper.getInstanceId(), backupId1).getName());
    assertThat(metadata2.getName())
        .isEqualTo(BackupId.of(testHelper.getInstanceId(), backupId2).getName());
    logger.info("Finished metadata tests");
  }

  private void testCreateInvalidExpirationDate(Database db) throws InterruptedException {
    // This is not allowed, the expiration date must be at least 6 hours in the future.
    Timestamp expireTime = yesterday();
    String backupId = getUniqueBackupId();
    logger.info(String.format("Creating backup %s with invalid expiration date", backupId));
    OperationFuture<Backup, CreateBackupMetadata> op =
        dbAdminClient.createBackup(
            testHelper.getInstanceId().getInstance(),
            backupId,
            db.getId().getDatabase(),
            expireTime);
    backups.add(backupId);
    try {
      op.get();
      fail("missing expected exception");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      assertThat(cause).isInstanceOf(SpannerException.class);
      SpannerException se = (SpannerException) cause;
      assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
  }

  private void testCancelBackupOperation(Database db)
      throws InterruptedException, ExecutionException {
    Timestamp expireTime = after7Days();
    String backupId = getUniqueBackupId();
    logger.info(String.format("Starting to create backup %s", backupId));
    OperationFuture<Backup, CreateBackupMetadata> op =
        dbAdminClient.createBackup(
            testHelper.getInstanceId().getInstance(),
            backupId,
            db.getId().getDatabase(),
            expireTime);
    backups.add(backupId);
    // Cancel the backup operation.
    logger.info(String.format("Cancelling the creation of backup %s", backupId));
    dbAdminClient.cancelOperation(op.getName());
    logger.info("Fetching backup operations");
    boolean operationFound = false;
    for (Operation operation :
        dbAdminClient
            .listBackupOperations(
                testHelper.getInstanceId().getInstance(),
                Options.filter(String.format("name:%s", op.getName())))
            .iterateAll()) {
      assertThat(operation.getError().getCode()).isEqualTo(Status.Code.CANCELLED.value());
      operationFound = true;
    }
    assertThat(operationFound).isTrue();
    logger.info("Finished cancel test");
  }

  private void testGetBackup(Database db, String backupId, Timestamp expireTime) {
    // Get the most recent version of the backup.
    logger.info(String.format("Getting backup %s", backupId));
    Backup backup = instance.getBackup(backupId);
    assertThat(backup.getState()).isEqualTo(Backup.State.READY);
    assertThat(backup.getSize()).isGreaterThan(0L);
    assertThat(backup.getExpireTime()).isEqualTo(expireTime);
    assertThat(backup.getDatabase()).isEqualTo(db.getId());
  }

  private void testUpdateBackup(Backup backup) throws InterruptedException, ExecutionException {
    // Update the expire time.
    Timestamp tomorrow = tomorrow();
    backup = backup.toBuilder().setExpireTime(tomorrow).build();
    logger.info(
        String.format("Updating expire time of backup %s to 1 week", backup.getId().getBackup()));
    backup.updateExpireTime();
    // Re-get the backup and ensure the expire time was updated.
    logger.info(String.format("Reloading backup %s", backup.getId().getBackup()));
    backup = backup.reload();
    assertThat(backup.getExpireTime()).isEqualTo(tomorrow);

    // Try to set the expire time to 5 minutes in the future.
    Timestamp in5Minutes = after5Minutes();
    backup = backup.toBuilder().setExpireTime(in5Minutes).build();
    try {
      logger.info(
          String.format(
              "Updating expire time of backup %s to 5 minutes", backup.getId().getBackup()));
      backup.updateExpireTime();
      fail("Missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
    // Re-get the backup and ensure the expire time is still in one week.
    backup = backup.reload();
    assertThat(backup.getExpireTime()).isEqualTo(tomorrow);
  }

  private void testPagination(int expectedMinimumTotalBackups) {
    logger.info("Listing backups using pagination");
    int numBackups = 0;
    logger.info("Fetching first page");
    Page<Backup> page =
        dbAdminClient.listBackups(testHelper.getInstanceId().getInstance(), Options.pageSize(1));
    assertThat(page.getValues()).hasSize(1);
    numBackups++;
    assertThat(page.hasNextPage()).isTrue();
    while (page.hasNextPage()) {
      logger.info(String.format("Fetching page %d", numBackups + 1));
      page =
          dbAdminClient.listBackups(
              testHelper.getInstanceId().getInstance(),
              Options.pageToken(page.getNextPageToken()),
              Options.pageSize(1));
      assertThat(page.getValues()).hasSize(1);
      numBackups++;
    }
    assertThat(numBackups).isAtLeast(expectedMinimumTotalBackups);
  }

  private void testDelete(String backupId) throws InterruptedException, ExecutionException {
    waitForDbOperations(backupId);
    // Get the backup.
    logger.info(String.format("Fetching backup %s", backupId));
    Backup backup = instance.getBackup(backupId);
    // Delete it.
    logger.info(String.format("Deleting backup %s", backupId));
    backup.delete();
    // Try to get it again. This should cause a NOT_FOUND error.
    try {
      logger.info(String.format("Fetching non-existent backup %s", backupId));
      instance.getBackup(backupId);
      fail("Missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }
    // Try to delete the non-existent backup. This should be a no-op.
    logger.info(String.format("Deleting non-existent backup %s", backupId));
    backup.delete();
    logger.info("Finished delete tests");
  }

  private void testRestore(Backup backup, OperationFuture<Backup, CreateBackupMetadata> backupOp)
      throws InterruptedException, ExecutionException {
    // Restore the backup to a new database.
    String restoredDb = testHelper.getUniqueDatabaseId();
    String restoreOperationName;
    OperationFuture<Database, RestoreDatabaseMetadata> restoreOp;
    int attempts = 0;
    while (true) {
      try {
        logger.info(
            String.format(
                "Restoring backup %s to database %s", backup.getId().getBackup(), restoredDb));
        restoreOp = backup.restore(DatabaseId.of(testHelper.getInstanceId(), restoredDb));
        restoreOperationName = restoreOp.getName();
        break;
      } catch (ExecutionException e) {
        if (e.getCause() instanceof FailedPreconditionException
            && e.getCause()
                .getMessage()
                .contains("Please retry the operation once the pending restores complete")) {
          attempts++;
          if (attempts == 10) {
            logger.info(
                "Restore operation failed 10 times because of other pending restores. Skipping restore test.");
            return;
          }
          // wait and then retry.
          logger.info(
              String.format(
                  "Restoring backup %s to database %s must wait because of other pending restore operation",
                  backup.getId().getBackup(), restoredDb));
          Thread.sleep(60_000L);
        } else {
          throw e;
        }
      }
    }
    databases.add(restoredDb);
    logger.info(String.format("Restore operation %s running", restoreOperationName));
    RestoreDatabaseMetadata metadata = restoreOp.getMetadata().get();
    assertThat(metadata.getBackupInfo().getBackup()).isEqualTo(backup.getId().getName());
    assertThat(metadata.getSourceType()).isEqualTo(RestoreSourceType.BACKUP);
    assertThat(metadata.getName())
        .isEqualTo(DatabaseId.of(testHelper.getInstanceId(), restoredDb).getName());

    // Ensure the operations show up in the right collections.
    // TODO: Re-enable when it is clear why this fails on the CI environment.
    //    verifyRestoreOperations(backupOp.getName(), restoreOperationName);

    // Wait until the restore operation has finished successfully.
    Database database = restoreOp.get();
    assertThat(database.getId().getDatabase()).isEqualTo(restoredDb);
    // Restoring the backup to an existing database should fail.
    try {
      logger.info(
          String.format(
              "Restoring backup %s to existing database %s",
              backup.getId().getBackup(), restoredDb));
      backup.restore(DatabaseId.of(testHelper.getInstanceId(), restoredDb)).get();
      fail("Missing expected exception");
    } catch (ExecutionException ee) {
      assertThat(ee.getCause()).isInstanceOf(SpannerException.class);
      SpannerException e = (SpannerException) ee.getCause();
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_EXISTS);
    }
  }

  // TODO: Remove when this verification can be re-enabled.
  @SuppressWarnings("unused")
  private void verifyRestoreOperations(
      final String backupOperationName, final String restoreOperationName) {
    assertThat(
            Iterables.any(
                instance.listBackupOperations().iterateAll(),
                new Predicate<Operation>() {
                  @Override
                  public boolean apply(Operation input) {
                    return input.getName().equals(backupOperationName);
                  }
                }))
        .isTrue();
    assertThat(
            Iterables.any(
                instance.listBackupOperations().iterateAll(),
                new Predicate<Operation>() {
                  @Override
                  public boolean apply(Operation input) {
                    return input.getName().equals(restoreOperationName);
                  }
                }))
        .isFalse();
    assertThat(
            Iterables.any(
                instance.listDatabaseOperations().iterateAll(),
                new Predicate<Operation>() {
                  @Override
                  public boolean apply(Operation input) {
                    return input.getName().equals(backupOperationName);
                  }
                }))
        .isFalse();
    assertThat(
            Iterables.any(
                instance.listDatabaseOperations().iterateAll(),
                new Predicate<Operation>() {
                  @Override
                  public boolean apply(Operation input) {
                    return input.getName().equals(restoreOperationName);
                  }
                }))
        .isTrue();
  }
}
