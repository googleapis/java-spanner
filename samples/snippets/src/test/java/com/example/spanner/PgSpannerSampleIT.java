package com.example.spanner;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class PgSpannerSampleIT {
    private static final int DBID_LENGTH = 20;
    // The instance needs to exist for tests to pass.
    private static final String instanceId = System.getProperty("spanner.test.instance");
    private static final String baseDbId = System.getProperty("spanner.sample.database");
    private static final String databaseId = formatForTest(baseDbId);
    private static final String encryptedDatabaseId = formatForTest(baseDbId);
    private static final String encryptedBackupId = formatForTest(baseDbId);
    private static final String encryptedRestoreId = formatForTest(baseDbId);
    static Spanner spanner;
    static DatabaseId dbId;
    static DatabaseAdminClient dbClient;

    @BeforeClass
    public static void setUp() {
        SpannerOptions options =
                SpannerOptions.newBuilder().setAutoThrottleAdministrativeRequests().build();
        spanner = options.getService();
        dbClient = spanner.getDatabaseAdminClient();
        dbId = DatabaseId.of(options.getProjectId(), instanceId, databaseId);
        // Delete stale test databases that have been created earlier by this test, but not deleted.
        deleteStaleTestDatabases();
    }

    static void deleteStaleTestDatabases() {
        Timestamp now = Timestamp.now();
        Pattern samplePattern = getTestDbIdPattern(PgSpannerSampleIT.baseDbId);
        Pattern restoredPattern = getTestDbIdPattern("restored");
        for (Database db : dbClient.listDatabases(PgSpannerSampleIT.instanceId).iterateAll()) {
            if (TimeUnit.HOURS.convert(now.getSeconds() - db.getCreateTime().getSeconds(),
                    TimeUnit.SECONDS) > 24) {
                if (db.getId().getDatabase().length() >= DBID_LENGTH) {
                    if (samplePattern.matcher(toComparableId(PgSpannerSampleIT.baseDbId, db.getId().getDatabase())).matches()) {
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
    public static void tearDown() {
        dbClient.dropDatabase(dbId.getInstanceId().getInstance(), dbId.getDatabase());
        dbClient.dropDatabase(
                dbId.getInstanceId().getInstance(), SpannerSample.createRestoredSampleDbId(dbId));
        dbClient.dropDatabase(instanceId, encryptedDatabaseId);
        dbClient.dropDatabase(instanceId, encryptedRestoreId);
        dbClient.deleteBackup(instanceId, encryptedBackupId);
        spanner.close();
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

    private String runSample(String command) {
        PrintStream stdOut = System.out;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bout);
        System.setOut(out);
        System.out.println(instanceId + ":" + databaseId);
        PgSpannerSample.main(new String[]{command, instanceId, databaseId});
        System.setOut(stdOut);
        return bout.toString();
    }

    @Test
    public void testSample() throws Exception {
        assertThat(instanceId).isNotNull();
        assertThat(databaseId).isNotNull();

        System.out.println("Create Database ...");
        String out = runSample("createdatabase");
        assertThat(out).contains("Created database");
        assertThat(out).contains(dbId.getName());

        System.out.println("Create sample tables Singers and Albums ...");
        runSample("createtableusingddl");

        System.out.println("Write data to sample tables ...");
        runSample("write");

        System.out.println("Read data from sample tables ...");
        out = runSample("read");
        assertThat(out).contains("1 1 Total Junk");

        System.out.println("Write data using DML to sample table ...");
        runSample("writeusingdml");
        System.out.println("Query Singers table ...");
        out = runSample("querysingerstable");
        assertThat(out).contains("Melissa Garcia");
        out = runSample("query");
        assertThat(out).contains("1 1 Total Junk");
        out = runSample("querywithparameter");
        assertThat(out).contains("12 Melissa Garcia");

        System.out.println("Add column marketing budget ...");
        runSample("addmarketingbudget");

        // wait for 15 seconds to elapse and then run an update, and query for stale data
        long lastUpdateDataTimeInMillis = System.currentTimeMillis();
        while (System.currentTimeMillis() < lastUpdateDataTimeInMillis + 16000) {
            Thread.sleep(1000);
        }
        System.out.println("Write data to marketing budget ...");
        runSample("update");

        System.out.println("Query marketing budget ...");
        out = runSample("querymarketingbudget");
        assertThat(out).contains("1 1 100000");
        assertThat(out).contains("2 2 500000");

        System.out.println("Write with transaction using dml...");
        runSample("writewithtransactionusingdml");
        out = runSample("querymarketingbudget");
        assertThat(out).contains("1 1 300000");
        assertThat(out).contains("1 1 300000");

        System.out.println("Add index ...");
        runSample("addindex");

        System.out.println("Read index ...");
        out = runSample("readindex");
        assertThat(out).contains("Go, Go, Go");
        assertThat(out).contains("Forever Hold Your Peace");
        assertThat(out).contains("Green");

        System.out.println("Add Storing index ...");
        runSample("addstoringindex");

        System.out.println("Read storing index ...");
        out = runSample("readstoringindex");
        assertThat(out).contains("300000");

        System.out.println("Read only transaction ...");
        out = runSample("readonlytransaction");
        assertThat(out.replaceAll("[\r\n]+", " ")).containsMatch("(Total Junk.*){2}");
    }
}
