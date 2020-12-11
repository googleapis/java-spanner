/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.BackupId;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoField;

/** Unit tests for {@code SpannerSample} */
@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class SpannerSampleIT {
  private static final int DBID_LENGTH = 20;
  // The instance needs to exist for tests to pass.
  private static final String instanceId = System.getProperty("spanner.test.instance");
  private static final String baseDbId = System.getProperty("spanner.sample.database");
  private static final String databaseId = formatForTest(baseDbId);
  static Spanner spanner;
  static DatabaseId dbId;
  static DatabaseAdminClient dbClient;
  private long lastUpdateDataTimeInMillis;

  private String runSample(String command) throws Exception {
    PrintStream stdOut = System.out;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    System.setOut(out);
    SpannerSample.main(new String[] {command, instanceId, databaseId});
    System.setOut(stdOut);
    return bout.toString();
  }

  @BeforeClass
  public static void setUp() throws Exception {
    SpannerOptions options =
        SpannerOptions.newBuilder().setAutoThrottleAdministrativeRequests().build();
    spanner = options.getService();
    dbClient = spanner.getDatabaseAdminClient();
    dbId = DatabaseId.of(options.getProjectId(), instanceId, databaseId);
    // Delete stale test databases that have been created earlier by this test, but not deleted.
    deleteStaleTestDatabases(instanceId, baseDbId);
  }
  
  static void deleteStaleTestDatabases(String instanceId, String baseDbId) {
    Timestamp now = Timestamp.now();
    Pattern samplePattern = getTestDbIdPattern(baseDbId);
    Pattern restoredPattern = getTestDbIdPattern("restored");
    for (Database db : dbClient.listDatabases(instanceId).iterateAll()) {
      if (TimeUnit.HOURS.convert(now.getSeconds() - db.getCreateTime().getSeconds(),
          TimeUnit.SECONDS) > 24) {
        if (db.getId().getDatabase().length() >= DBID_LENGTH) {
          if (samplePattern.matcher(toComparableId(baseDbId, db.getId().getDatabase())).matches()) {
            db.drop();
          }
          if (restoredPattern.matcher(toComparableId("restored", db.getId().getDatabase()))
              .matches()) {
            db.drop();
          }
        }
      }
    }
  }

  @AfterClass
  public static void tearDown() throws Exception {
    dbClient.dropDatabase(dbId.getInstanceId().getInstance(), dbId.getDatabase());
    dbClient.dropDatabase(
        dbId.getInstanceId().getInstance(), SpannerSample.createRestoredSampleDbId(dbId));
    spanner.close();
  }

  @Test
  public void testSample() throws Exception {
    assertThat(instanceId).isNotNull();
    assertThat(databaseId).isNotNull();
    String out = runSample("createdatabase");
    assertThat(out).contains("Created database");
    assertThat(out).contains(dbId.getName());

    runSample("write");

    out = runSample("delete");
    assertThat(out).contains("Records deleted.");

    runSample("write");

    out = runSample("read");
    assertThat(out).contains("1 1 Total Junk");

    out = runSample("query");
    assertThat(out).contains("1 1 Total Junk");
    runSample("addmarketingbudget");

    // wait for 15 seconds to elapse and then run an update, and query for stale data
    lastUpdateDataTimeInMillis = System.currentTimeMillis();
    while (System.currentTimeMillis() < lastUpdateDataTimeInMillis + 16000) {
      Thread.sleep(1000);
    }
    runSample("update");
    out = runSample("readstaledata");
    assertThat(out).contains("1 1 NULL");
    runSample("writetransaction");
    out = runSample("querymarketingbudget");
    assertThat(out).contains("1 1 300000");
    assertThat(out).contains("2 2 300000");

    runSample("addindex");
    out = runSample("queryindex");
    assertThat(out).contains("Go, Go, Go");
    assertThat(out).contains("Forever Hold Your Peace");
    assertThat(out).doesNotContain("Green");

    out = runSample("readindex");
    assertThat(out).contains("Go, Go, Go");
    assertThat(out).contains("Forever Hold Your Peace");
    assertThat(out).contains("Green");

    runSample("addstoringindex");
    out = runSample("readstoringindex");
    assertThat(out).contains("300000");

    out = runSample("readonlytransaction");
    assertThat(out.replaceAll("[\r\n]+", " ")).containsMatch("(Total Junk.*){2}");

    out = runSample("addcommittimestamp");
    assertThat(out).contains("Added LastUpdateTime as a commit timestamp column");

    runSample("updatewithtimestamp");
    out = runSample("querywithtimestamp");
    assertThat(out).contains("1 1 1000000");
    assertThat(out).contains("2 2 750000");

    out = runSample("createtablewithtimestamp");
    assertThat(out).contains("Created Performances table in database");

    runSample("writewithtimestamp");
    out = runSample("queryperformancestable");
    assertThat(out).contains("1 4 2017-10-05 11000");
    assertThat(out).contains("1 19 2017-11-02 15000");
    assertThat(out).contains("2 42 2017-12-23 7000");

    runSample("writestructdata");
    out = runSample("querywithstruct");
    assertThat(out).startsWith("6\n");

    out = runSample("querywitharrayofstruct");
    assertThat(out).startsWith("8\n7\n6");

    out = runSample("querystructfield");
    assertThat(out).startsWith("6\n");

    out = runSample("querynestedstructfield");
    assertThat(out).contains("6 Imagination\n");
    assertThat(out).contains("9 Imagination\n");

    runSample("insertusingdml");
    out = runSample("querysingerstable");
    assertThat(out).contains("Virginia Watson");

    runSample("updateusingdml");
    out = runSample("querymarketingbudget");
    assertThat(out).contains("1 1 2000000");

    runSample("deleteusingdml");
    out = runSample("querysingerstable");
    assertThat(out).doesNotContain("Alice Trentor");

    out = runSample("updateusingdmlwithtimestamp");
    assertThat(out).contains("2 records updated");

    out = runSample("writeandreadusingdml");
    assertThat(out).contains("Timothy Campbell");

    runSample("updateusingdmlwithstruct");
    out = runSample("querysingerstable");
    assertThat(out).contains("Timothy Grant");

    runSample("writeusingdml");
    out = runSample("querysingerstable");
    assertThat(out).contains("Melissa Garcia");
    assertThat(out).contains("Russell Morales");
    assertThat(out).contains("Jacqueline Long");
    assertThat(out).contains("Dylan Shaw");
    out = runSample("querywithparameter");
    assertThat(out).contains("12 Melissa Garcia");

    runSample("writewithtransactionusingdml");
    out = runSample("querymarketingbudget");
    assertThat(out).contains("1 1 2200000");
    assertThat(out).contains("2 2 550000");

    runSample("updateusingpartitioneddml");
    out = runSample("querymarketingbudget");
    assertThat(out).contains("1 1 2200000");
    assertThat(out).contains("2 2 100000");

    runSample("deleteusingpartitioneddml");
    out = runSample("querysingerstable");
    assertThat(out).doesNotContain("Timothy Grant");
    assertThat(out).doesNotContain("Melissa Garcia");
    assertThat(out).doesNotContain("Russell Morales");
    assertThat(out).doesNotContain("Jacqueline Long");
    assertThat(out).doesNotContain("Dylan Shaw");

    out = runSample("updateusingbatchdml");
    assertThat(out).contains("1 record updated by stmt 0");
    assertThat(out).contains("1 record updated by stmt 1");

    out = runSample("createtablewithdatatypes");
    assertThat(out).contains("Created Venues table in database");

    runSample("writedatatypesdata");
    out = runSample("querywitharray");
    assertThat(out).contains("19 Venue 19 2020-11-01");
    assertThat(out).contains("42 Venue 42 2020-10-01");

    out = runSample("querywithbool");
    assertThat(out).contains("19 Venue 19 true");

    out = runSample("querywithbytes");
    assertThat(out).contains("4 Venue 4");

    out = runSample("querywithdate");
    assertThat(out).contains("4 Venue 4 2018-09-02");
    assertThat(out).contains("42 Venue 42 2018-10-01");

    out = runSample("querywithfloat");
    assertThat(out).contains("4 Venue 4 0.8");
    assertThat(out).contains("19 Venue 19 0.9");

    out = runSample("querywithint");
    assertThat(out).contains("19 Venue 19 6300");
    assertThat(out).contains("42 Venue 42 3000");

    out = runSample("querywithstring");
    assertThat(out).contains("42 Venue 42");

    out = runSample("querywithtimestampparameter");
    assertThat(out).contains("4 Venue 4");
    assertThat(out).contains("19 Venue 19");
    assertThat(out).contains("42 Venue 42");

    out = runSample("querywithnumeric");
    assertThat(out).contains("19 Venue 19 1200100");
    assertThat(out).contains("42 Venue 42 390650.99");

    out = runSample("clientwithqueryoptions");
    assertThat(out).contains("1 1 Total Junk");
    out = runSample("querywithqueryoptions");
    assertThat(out).contains("1 1 Total Junk");

    String backupName =
        String.format(
            "%s_%02d",
            dbId.getDatabase(), LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR));
    BackupId backupId = BackupId.of(dbId.getInstanceId(), backupName);

    out = runSample("createbackup");
    assertThat(out).contains("Created backup [" + backupId + "]");

    out = runSample("cancelcreatebackup");
    assertThat(out).contains(
        "Backup operation for [" + backupId + "_cancel] successfully cancelled");

    // TODO: remove try-catch when filtering on metadata fields works.
    try {
      out = runSample("listbackupoperations");
      assertThat(out).contains(
          String.format(
              "Backup %s on database %s pending:",
              backupId.getName(),
              dbId.getName()));
    } catch (SpannerException e) {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
      assertThat(e.getMessage()).contains("Cannot evaluate filter expression");
    }

    out = runSample("listbackups");
    assertThat(out).contains("All backups:");
    assertThat(out).contains(
        String.format("All backups with backup name containing \"%s\":", backupId.getBackup()));
    assertThat(out).contains(String.format(
        "All backups for databases with a name containing \"%s\":",
        dbId.getDatabase()));
    assertThat(out).contains(
        String.format("All backups that expire before"));
    assertThat(out).contains("All backups with size greater than 100 bytes:");
    assertThat(out).containsMatch(
        Pattern.compile("All databases created after (.+) and that are ready:"));
    assertThat(out).contains("All backups, listed using pagination:");
    // All the above tests should include the created backup exactly once, i.e. exactly 7 times.
    assertThat(countOccurrences(out, backupId.getName())).isEqualTo(7);

    // Try the restore operation in a retry loop, as there is a limit on the number of restore
    // operations that is allowed to execute simultaneously, and we should retry if we hit this
    // limit.
    boolean restored = false;
    int restoreAttempts = 0;
    while (true) {
      try {
        out = runSample("restorebackup");
        assertThat(out).contains(
            "Restored database ["
                + dbId.getName()
                + "] from ["
                + backupId.getName()
                + "]");
        restored = true;
        break;
      } catch (SpannerException e) {
        if (e.getErrorCode() == ErrorCode.FAILED_PRECONDITION
            && e.getMessage()
                .contains("Please retry the operation once the pending restores complete")) {
          restoreAttempts++;
          if (restoreAttempts == 10) {
            System.out.println(
                "Restore operation failed 10 times because of other pending restores. "
                + "Giving up restore.");
            break;
          }
          Uninterruptibles.sleepUninterruptibly(60L, TimeUnit.SECONDS);
        } else {
          throw e;
        }
      }
    }

    if (restored) {
      out = runSample("listdatabaseoperations");
      assertThat(out).contains(
          String.format(
              "Database %s restored from backup",
              DatabaseId.of(
                  dbId.getInstanceId(),
                  SpannerSample.createRestoredSampleDbId(dbId))
              .getName()));
    }

    out = runSample("updatebackup");
    assertThat(out).contains(
        String.format("Updated backup [" + backupId + "]"));

    // Drop the restored database before we try to delete the backup.
    // Otherwise the delete backup operation might fail as the backup is still in use by
    // the OptimizeRestoredDatabase operation.
    dbClient.dropDatabase(
        dbId.getInstanceId().getInstance(), SpannerSample.createRestoredSampleDbId(dbId));

    out = runSample("deletebackup");
    assertThat(out).contains("Deleted backup [" + backupId + "]");
  }

  private String runSampleRunnable(Runnable sample) {
    PrintStream stdOut = System.out;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    System.setOut(out);
    sample.run();
    System.setOut(stdOut);
    return bout.toString();
  }

  @Test
  public void testCreateInstanceSample() {
    String instanceId = formatForTest("sample-inst");
    String out =
        runSampleRunnable(
            new Runnable() {
              @Override
              public void run() {
                try {
                  CreateInstanceExample.createInstance(
                      dbId.getInstanceId().getProject(), instanceId);
                } finally {
                  spanner.getInstanceAdminClient().deleteInstance(instanceId);
                }
              }
            });
    assertThat(out)
        .contains(
            String.format(
                "Instance %s was successfully created",
                InstanceId.of(dbId.getInstanceId().getProject(), instanceId)));
  }

  private static int countOccurrences(String input, String search) {
    return input.split(search).length - 1;
  }
  
  private static String toComparableId(String baseId, String existingId) {
    String zeroUuid = "00000000-0000-0000-0000-0000-00000000";
    int shouldBeLength = (baseId + "-" + zeroUuid).length();
    int missingLength = shouldBeLength - existingId.length();
    return existingId + zeroUuid.substring(zeroUuid.length() - missingLength);
  }

  private static Pattern getTestDbIdPattern(String baseDbId) {
    return Pattern.compile(
        baseDbId + "-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{8}",
        Pattern.CASE_INSENSITIVE);
  }
  
  static String formatForTest(String name) {
    return name + "-" + UUID.randomUUID().toString().substring(0, DBID_LENGTH);
  }
}