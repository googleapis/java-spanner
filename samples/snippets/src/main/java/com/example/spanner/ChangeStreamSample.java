/*
 * Copyright 2021 Google Inc.
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

// [START spanner_change_stream]
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Sample code for Cloud Spanner Change Stream. It demonstrates how to:
 * 1. Set up the Cloud Spanner client.
 * 2. Create a database with a table that's watched by a change stream.
 * 3. Insert rows into the table.
 * 4. Execute change stream initial query to get change stream partition tokens.
 * 5. Execute a change stream partition query to get data change records of the inserted rows.
 */
public class ChangeStreamSample {
  public static void main(String... args) {
    if (args.length != 2 && args.length != 3) {
      System.err.println("Usage: ChangeStreamSample "
          + "<project_id> "
          + "<instance_id> "
          + "<database_id> (optional)");
      return;
    }

    final String projectId = args[0];
    final String instanceId = args[1];
    String databaseId = "";
    boolean createTmpDatabase = args.length == 2;
    if (createTmpDatabase) {
      // Generates a random database ID if not provided.
      databaseId = "sample-db-" + UUID.randomUUID().toString().substring(0, 10);
    } else {
      databaseId = args[2];
    }

    Spanner spanner = SpannerOptions.newBuilder().build().getService();

    try {
      // Creates a database with a table Singers and a change stream ChangeStreamAll.
      System.out.println("Creating database " + databaseId
          + " with table Singers and change stream ChangeStreamAll");
      DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();
      if (createTmpDatabase) {
        createDatabase(dbAdminClient, projectId, instanceId, databaseId);
      }

      // Insert rows into Singers table.
      System.out.println(
          "Inserting rows (1, Melissa, Garcia) and (2, Dylan, Shaw) into Singers table.");
      DatabaseClient dbClient = spanner.getDatabaseClient(
          DatabaseId.of(projectId, instanceId, databaseId));
      insertRows(dbClient);

      final Timestamp startTimestamp = Timestamp.now();
      // end = start + 30 seconds.
      final Timestamp endTimestamp = Timestamp.ofTimeSecondsAndNanos(
          startTimestamp.getSeconds() + 30, startTimestamp.getNanos());
      final String startTimestampStr = startTimestamp.toString();
      final String endTimestampStr = endTimestamp.toString();

      // Executes an initial query to get partition tokens.
      System.out.println("Executing change stream initial query.");
      List<ChangeStreamRecord> initialQueryRecords = executeChangeStreamQueryAndPrint(
          dbClient, startTimestampStr, endTimestampStr, null);

      System.out.println("Executing change stream partition queries.");
      for (ChangeStreamRecord record : initialQueryRecords) {
        // Executes a partition query to print data records that we just inserted.
        if (record instanceof ChildPartitionsRecord) {
          for (ChildPartition childPartition :
              ((ChildPartitionsRecord) record).getChildPartitions()) {
            executeChangeStreamQueryAndPrint(
                dbClient, startTimestampStr, endTimestampStr,
                childPartition.getToken());
          }
        }
      }

      if (createTmpDatabase) {
        System.out.println("Dropping database " + databaseId);
        dbAdminClient.dropDatabase(instanceId, databaseId);
      }
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      // Closes the client which will free up the resources used.
      spanner.close();
    }
  }

  static void createDatabase(
      DatabaseAdminClient client, String projectId, String instanceId, String databaseId)
      throws InterruptedException {
    Database dbInfo = client
        .newDatabaseBuilder(DatabaseId.of(projectId, instanceId, databaseId))
        .build();
    OperationFuture<Database, CreateDatabaseMetadata> op = client
        .createDatabase(
        dbInfo,
        Arrays.asList(
          "CREATE TABLE Singers ("
            + "  SingerId   INT64 NOT NULL,"
            + "  FirstName  STRING(1024),"
            + "  LastName   STRING(1024)"
            + ") PRIMARY KEY (SingerId)",
          "CREATE CHANGE STREAM ChangeStreamAll FOR ALL"
        )
      );

    // Wait for the database to be created.
    Stopwatch watch = Stopwatch.createStarted();
    while (watch.elapsed(TimeUnit.MINUTES) < 5L && !op.isDone()) {
      Thread.sleep(1000L);
    }

    if (!op.isDone()) {
      throw SpannerExceptionFactory.newSpannerException(
        ErrorCode.DEADLINE_EXCEEDED,
        "Timeout while creating database " + databaseId);
    }
  }

  // Insert two rows into Singers table.
  static void insertRows(DatabaseClient client) {
    client.write(
        ImmutableList.of(
        Mutation.newInsertOrUpdateBuilder("Singers")
          .set("SingerId").to(1)
          .set("FirstName").to("Melissa")
          .set("LastName").to("Garcia")
          .build(),
        Mutation.newInsertOrUpdateBuilder("Singers")
          .set("SingerId").to(2)
          .set("FirstName").to("Dylan")
          .set("LastName").to("Shaw")
          .build()
      )
    );
  }

  // Execute a change stream query, return and print out the result records.
  static List<ChangeStreamRecord> executeChangeStreamQueryAndPrint(
      DatabaseClient dbClient, String startTimestamp, String endTimestamp, String partitionToken) {
    System.out.println("Executing a change stream query with: "
        + "start_timestamp => " + startTimestamp
        + ", end_timestamp => " + endTimestamp
        + ", partition_token => " + partitionToken
        + ", heartbeat_milliseconds => 5000"
        + ", read_options => null");

    final String query =
        "SELECT * FROM READ_ChangeStreamAll ("
        + "start_timestamp => @startTimestamp,"
        + "end_timestamp => @endTimestamp,"
        + "partition_token => @partitionToken,"
        + "read_options => null,"
        + "heartbeat_milliseconds => @heartbeatMillis"
        + ")";

    final ResultSet resultSet =
        dbClient
        .singleUse()
        .executeQuery(
          Statement.newBuilder(query)
            .bind("startTimestamp").to(Timestamp.parseTimestamp(startTimestamp))
            .bind("endTimestamp").to(Timestamp.parseTimestamp(endTimestamp))
            .bind("partitionToken").to(partitionToken)
            .bind("heartbeatMillis").to(5000)
            .build());

    ChangeStreamRecordMapper changeStreamRecordMapper = new ChangeStreamRecordMapper();
    List<ChangeStreamRecord> res = new LinkedList<>();
    while (resultSet.next()) {
      // Parses result set into change stream result format.
      final List<ChangeStreamRecord> records =
          changeStreamRecordMapper.toChangeStreamRecords(resultSet.getCurrentRowAsStruct());

      // Prints out all the query results.
      for (final ChangeStreamRecord record : records) {
        if (record instanceof DataChangeRecord) {
          System.out.println("Received a DataChangeRecord: " + record);
        } else if (record instanceof HeartbeatRecord) {
          System.out.println("Received a HeartbeatRecord: " + record);
        } else if (record instanceof ChildPartitionsRecord) {
          System.out.println("Received a ChildPartitionsRecord: " + record);
        } else {
          throw new IllegalArgumentException("Unknown record type " + record.getClass());
        }
      }
      res.addAll(records);
    }

    return res;
  }

  // Models of change stream result format.
  interface ChangeStreamRecord extends Serializable {}

  static class DataChangeRecord implements ChangeStreamRecord {

    private Timestamp commitTimestamp;
    private String serverTransactionId;
    private boolean isLastRecordInTransactionInPartition;
    private String recordSequence;
    private String tableName;
    private List<ColumnType> rowType;
    private List<Mod> mods;
    private ModType modType;
    private ValueCaptureType valueCaptureType;
    private long numberOfRecordsInTransaction;
    private long numberOfPartitionsInTransaction;

    /**
     * Constructs a data change record for a given partition, at a given timestamp, for a given
     * transaction. The data change record needs to be given information about the table modified,
     * the type of primary keys and modified columns, the modifications themselves and other
     * metadata.
     *
     * @param commitTimestamp the timestamp at which the modifications within were committed in
     *        Cloud Spanner
     * @param serverTransactionId the unique transaction id in which the modifications occurred
     * @param isLastRecordInTransactionInPartition indicates whether this record is the last emitted
     *        for the given transaction in the given partition
     * @param recordSequence indicates the order in which this record was put into the change stream
     *        in the scope of a partition, commit timestamp and transaction tuple
     * @param tableName the name of the table in which the modifications occurred
     * @param rowType the type of the primary keys and modified columns
     * @param mods the modifications occurred
     * @param modType the operation that caused the modification to occur
     * @param valueCaptureType the capture type of the change stream
     * @param numberOfRecordsInTransaction the total number of records for the given transaction
     * @param numberOfPartitionsInTransaction the total number of partitions within the given
     *        transaction
     */
    public DataChangeRecord(
        Timestamp commitTimestamp,
        String serverTransactionId,
        boolean isLastRecordInTransactionInPartition,
        String recordSequence,
        String tableName,
        List<ColumnType> rowType,
        List<Mod> mods,
        ModType modType,
        ValueCaptureType valueCaptureType,
        long numberOfRecordsInTransaction,
        long numberOfPartitionsInTransaction) {
      this.commitTimestamp = commitTimestamp;
      this.serverTransactionId = serverTransactionId;
      this.isLastRecordInTransactionInPartition = isLastRecordInTransactionInPartition;
      this.recordSequence = recordSequence;
      this.tableName = tableName;
      this.rowType = rowType;
      this.mods = mods;
      this.modType = modType;
      this.valueCaptureType = valueCaptureType;
      this.numberOfRecordsInTransaction = numberOfRecordsInTransaction;
      this.numberOfPartitionsInTransaction = numberOfPartitionsInTransaction;
    }

    @Override
    public String toString() {
      return "DataChangeRecord{"
        + "commitTimestamp="
        + commitTimestamp
        + ", serverTransactionId='"
        + serverTransactionId
        + '\''
        + ", isLastRecordInTransactionInPartition="
        + isLastRecordInTransactionInPartition
        + ", recordSequence='"
        + recordSequence
        + '\''
        + ", tableName='"
        + tableName
        + '\''
        + ", rowType="
        + rowType
        + ", mods="
        + mods
        + ", modType="
        + modType
        + ", valueCaptureType="
        + valueCaptureType
        + ", numberOfRecordsInTransaction="
        + numberOfRecordsInTransaction
        + ", numberOfPartitionsInTransaction="
        + numberOfPartitionsInTransaction
        + '}';
    }
  }

  static class Mod implements Serializable {

    private String keysJson;
    @Nullable private String oldValuesJson;
    @Nullable private String newValuesJson;

    /**
     * Constructs a mod from the primary key values, the old state of the row and the new state of
     * the row.
     *
     * @param keysJson JSON object as String, where the keys are the primary key column names and
     *        the values are the primary key column values
     * @param oldValuesJson JSON object as String, displaying the old state of the columns modified.
     *        This JSON object can be null in the case of an INSERT
     * @param newValuesJson JSON object as String, displaying the new state of the columns modified.
     *        This JSON object can be null in the case of a DELETE
     */
    public Mod(String keysJson, String oldValuesJson, String newValuesJson) {
      this.keysJson = keysJson;
      this.oldValuesJson = oldValuesJson;
      this.newValuesJson = newValuesJson;
    }

    @Override
    public String toString() {
      return "Mod{"
        + "keysJson="
        + keysJson
        + ", oldValuesJson='"
        + oldValuesJson
        + '\''
        + ", newValuesJson='"
        + newValuesJson
        + '\''
        + '}';
    }
  }

  static class ColumnType implements Serializable {

    private String name;
    private TypeCode type;
    private boolean isPrimaryKey;
    private long ordinalPosition;

    public ColumnType(String name, TypeCode type, boolean isPrimaryKey, long ordinalPosition) {
      this.name = name;
      this.type = type;
      this.isPrimaryKey = isPrimaryKey;
      this.ordinalPosition = ordinalPosition;
    }

    @Override
    public String toString() {
      return "ColumnType{"
        + "name='"
        + name
        + '\''
        + ", type="
        + type
        + ", isPrimaryKey="
        + isPrimaryKey
        + ", ordinalPosition="
        + ordinalPosition
        + '}';
    }
  }

  static class TypeCode implements Serializable {

    private String code;

    /**
     * Constructs a type code from the given String code.
     *
     * @param code the code of the column type
     */
    public TypeCode(String code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return "TypeCode{" + "code='" + code + '\'' + '}';
    }
  }

  enum ModType {
    INSERT,
    UPDATE,
    INSERT_OR_UPDATE,
    DELETE
  }

  enum ValueCaptureType {
    OLD_AND_NEW_VALUES,
  }

  static class HeartbeatRecord implements ChangeStreamRecord {

    private Timestamp timestamp;

    /**
     * Constructs the heartbeat record with the given timestamp and metadata.
     *
     * @param timestamp the timestamp for which all changes in the partition have occurred
     */
    public HeartbeatRecord(Timestamp timestamp) {
      this.timestamp = timestamp;
    }

    @Override
    public String toString() {
      return "HeartbeatRecord{" + "timestamp=" + timestamp + '}';
    }
  }

  static class ChildPartitionsRecord implements ChangeStreamRecord {

    private Timestamp startTimestamp;
    private String recordSequence;
    private List<ChildPartition> childPartitions;

    /**
     * Constructs a child partitions record containing one or more child partitions.
     *
     * @param startTimestamp the timestamp which this partition started being valid in Cloud Spanner
     * @param recordSequence the order within a partition and a transaction in which the record was
     *        put to the stream
     * @param childPartitions child partition tokens emitted within this record
     */
    public ChildPartitionsRecord(
        Timestamp startTimestamp,
        String recordSequence,
        List<ChildPartition> childPartitions) {
      this.startTimestamp = startTimestamp;
      this.recordSequence = recordSequence;
      this.childPartitions = childPartitions;
    }

    public List<ChildPartition> getChildPartitions() {
      return this.childPartitions;
    }

    @Override
    public String toString() {
      return "ChildPartitionsRecord{"
        + "startTimestamp="
        + startTimestamp
        + ", recordSequence='"
        + recordSequence
        + '\''
        + ", childPartitions="
        + childPartitions
        + '}';
    }
  }

  static class ChildPartition implements Serializable {

    private String token;
    private HashSet<String> parentTokens;

    /**
     * Constructs a child partition, which will have its own token and the parents that it
     * originated from. A child partition will have a single parent if it is originated from a
     * partition move or split. A child partition will have multiple parents if it is originated
     * from a partition merge.
     *
     * @param token the child partition token
     * @param parentTokens the partition tokens of the parent(s) that originated the child partition
     */
    public ChildPartition(String token, HashSet<String> parentTokens) {
      this.token = token;
      this.parentTokens = parentTokens;
    }

    public String getToken() {
      return this.token;
    }

    @Override
    public String toString() {
      return "ChildPartition{"
        + "childToken='"
        + token
        + '\''
        + ", parentTokens="
        + parentTokens
        + '}';
    }
  }

  static class ChangeStreamRecordMapper {

    ChangeStreamRecordMapper() {}

    public List<ChangeStreamRecord> toChangeStreamRecords(Struct row) {
      return row.getStructList(0).stream()
        .flatMap(struct -> toChangeStreamRecord(struct))
        .collect(Collectors.toList());
    }

    private Stream<ChangeStreamRecord> toChangeStreamRecord(Struct row) {
      final Stream<DataChangeRecord> dataChangeRecords =
          row.getStructList("data_change_record").stream()
            .filter(this::isNonNullDataChangeRecord)
            .map(struct -> toDataChangeRecord(struct));

      final Stream<HeartbeatRecord> heartbeatRecords =
          row.getStructList("heartbeat_record").stream()
          .filter(this::isNonNullHeartbeatRecord)
          .map(struct -> toHeartbeatRecord(struct));

      final Stream<ChildPartitionsRecord> childPartitionsRecords =
          row.getStructList("child_partitions_record").stream()
          .filter(this::isNonNullChildPartitionsRecord)
          .map(struct -> toChildPartitionsRecord(struct));

      return Stream.concat(
        Stream.concat(dataChangeRecords, heartbeatRecords), childPartitionsRecords);
    }

    private boolean isNonNullDataChangeRecord(Struct row) {
      return !row.isNull("commit_timestamp");
    }

    private boolean isNonNullHeartbeatRecord(Struct row) {
      return !row.isNull("timestamp");
    }

    private boolean isNonNullChildPartitionsRecord(Struct row) {
      return !row.isNull("start_timestamp");
    }

    private DataChangeRecord toDataChangeRecord(Struct row) {
      final Timestamp commitTimestamp = row.getTimestamp("commit_timestamp");
      return new DataChangeRecord(
        commitTimestamp,
        row.getString("server_transaction_id"),
        row.getBoolean("is_last_record_in_transaction_in_partition"),
        row.getString("record_sequence"),
        row.getString("table_name"),
        row.getStructList("column_types").stream()
          .map(this::columnTypeFrom)
          .collect(Collectors.toList()),
        row.getStructList("mods").stream().map(this::modFrom).collect(Collectors.toList()),
        ModType.valueOf(row.getString("mod_type")),
        ValueCaptureType.valueOf(row.getString("value_capture_type")),
        row.getLong("number_of_records_in_transaction"),
        row.getLong("number_of_partitions_in_transaction"));
    }

    private HeartbeatRecord toHeartbeatRecord(Struct row) {
      final Timestamp timestamp = row.getTimestamp("timestamp");
      return new HeartbeatRecord(timestamp);
    }

    private ChildPartitionsRecord toChildPartitionsRecord(Struct row) {
      final Timestamp startTimestamp = row.getTimestamp("start_timestamp");
      return new ChildPartitionsRecord(
        startTimestamp,
        row.getString("record_sequence"),
        row.getStructList("child_partitions").stream()
          .map(struct -> childPartitionFrom(struct))
          .collect(Collectors.toList()));
    }

    private ColumnType columnTypeFrom(Struct struct) {
      return new ColumnType(
        struct.getString("name"),
          new TypeCode(struct.getString("type")),
        struct.getBoolean("is_primary_key"),
        struct.getLong("ordinal_position"));
    }

    private Mod modFrom(Struct struct) {
      final String keysJson = struct.getString("keys");
      final String oldValuesJson =
          struct.isNull("old_values") ? null : struct.getString("old_values");
      final String newValuesJson =
          struct.isNull("new_values") ? null : struct.getString("new_values");
      return new Mod(keysJson, oldValuesJson, newValuesJson);
    }

    private ChildPartition childPartitionFrom(Struct struct) {
      final HashSet<String> parentTokens =
          Sets.newHashSet(struct.getStringList("parent_partition_tokens"));
      return new ChildPartition(struct.getString("token"), parentTokens);
    }
  }
}
// [END spanner_change_stream]
