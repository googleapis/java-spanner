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

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Example code for using the Cloud Spanner API. This example demonstrates all the common operations
 * that can be done on Cloud Spanner. These are:
 *
 * <p>
 *
 * <ul>
 *   <li>Creating a Cloud Spanner database.
 *   <li>Writing, reading and executing SQL queries.
 *   <li>Writing data using a read-write transaction.
 *   <li>Using an index to read and execute SQL queries over data.
 *   <li>Using commit timestamp for tracking when a record was last updated.
 *   <li>Using Google API Extensions for Java to make thread-safe requests via long-running
 *       operations. http://googleapis.github.io/gax-java/
 * </ul>
 */
public class PgSpannerSample {
  static final List<Singer> SINGERS =
      Arrays.asList(
          new Singer(1, "Marc", "Richards"),
          new Singer(2, "Catalina", "Smith"),
          new Singer(3, "Alice", "Trentor"),
          new Singer(4, "Lea", "Martin"),
          new Singer(5, "David", "Lomond"));
  static final List<Album> ALBUMS =
      Arrays.asList(
          new Album(1, 1, "Total Junk"),
          new Album(1, 2, "Go, Go, Go"),
          new Album(2, 1, "Green"),
          new Album(2, 2, "Forever Hold Your Peace"),
          new Album(2, 3, "Terrified"));

  // [START spanner_create_database]
  static void createDatabase(DatabaseAdminClient dbAdminClient, DatabaseId id) {
    OperationFuture<Database, CreateDatabaseMetadata> op = dbAdminClient.createDatabase(
        dbAdminClient.newDatabaseBuilder(id).setDialect(Dialect.POSTGRESQL).build(),
        Collections.emptyList());
    try {
      // Initiate the request which returns an OperationFuture.
      Database db = op.get();
      System.out.println("Created database [" + db.getId() + "]");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
  // [END spanner_create_database]

  // [START spanner_insert_data]
  static void writeExampleData(DatabaseClient dbClient) {
    List<Mutation> mutations = new ArrayList<>();
    for (Singer singer : SINGERS) {
      mutations.add(
          Mutation.newInsertBuilder("Singers")
              .set("SingerId")
              .to(singer.singerId)
              .set("FirstName")
              .to(singer.firstName)
              .set("LastName")
              .to(singer.lastName)
              .build());
    }
    for (Album album : ALBUMS) {
      mutations.add(
          Mutation.newInsertBuilder("Albums")
              .set("SingerId")
              .to(album.singerId)
              .set("AlbumId")
              .to(album.albumId)
              .set("AlbumTitle")
              .to(album.albumTitle)
              .build());
    }
    dbClient.write(mutations);
  }
  // [END spanner_insert_data]

  // [START spanner_query_data]
  static void query(DatabaseClient dbClient) {
    try (ResultSet resultSet =
             dbClient
                 .singleUse() // Execute a single read or query against Cloud Spanner.
                 .executeQuery(Statement.of("SELECT SingerId, AlbumId, AlbumTitle FROM Albums"))) {
      while (resultSet.next()) {
        System.out.printf(
            "%d %d %s\n", resultSet.getLong(0), resultSet.getLong(1),
            resultSet.getString(2));
      }
    }
  }
  // [END spanner_query_data]

  // [START spanner_read_data]
  static void read(DatabaseClient dbClient) {
    try (ResultSet resultSet =
             dbClient
                 .singleUse()
                 .read(
                     "Albums",
                     KeySet.all(), // Read all rows in a table.
                     Arrays.asList("SingerId", "AlbumId", "AlbumTitle"))) {
      while (resultSet.next()) {
        System.out.printf(
            "%d %d %s\n", resultSet.getLong(0), resultSet.getLong(1),
            resultSet.getString(2));
      }
    }
  }
  // [END spanner_read_data]

  // [START spanner_add_column]
  static void addMarketingBudget(DatabaseAdminClient adminClient, DatabaseId dbId) {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op = adminClient.updateDatabaseDdl(
        dbId.getInstanceId().getInstance(),
        dbId.getDatabase(),
        Arrays.asList("ALTER TABLE Albums ADD COLUMN MarketingBudget bigint"),
        null);
    try {
      // Initiate the request which returns an OperationFuture.
      op.get();
      System.out.println("Added MarketingBudget column");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
  // [END spanner_add_column]

  // Before executing this method, a new column MarketingBudget has to be added to the Albums
  // table by applying the DDL statement "ALTER TABLE Albums ADD COLUMN MarketingBudget INT64".
  // [START spanner_update_data]
  static void update(DatabaseClient dbClient) {
    // Mutation can be used to update/insert/delete a single row in a table. Here we use
    // newUpdateBuilder to create update mutations.
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.newUpdateBuilder("Albums")
                .set("SingerId")
                .to(1)
                .set("AlbumId")
                .to(1)
                .set("MarketingBudget")
                .to(100000)
                .build(),
            Mutation.newUpdateBuilder("Albums")
                .set("SingerId")
                .to(2)
                .set("AlbumId")
                .to(2)
                .set("MarketingBudget")
                .to(500000)
                .build());
    // This writes all the mutations to Cloud Spanner atomically.
    dbClient.write(mutations);
  }
  // [END spanner_update_data]

  // [START spanner_read_write_transaction]
  static void writeWithTransaction(DatabaseClient dbClient) {
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          // Transfer marketing budget from one album to another. We do it in a transaction to
          // ensure that the transfer is atomic.
          Struct row =
              transaction.readRow("Albums", Key.of(2, 2), Arrays.asList("MarketingBudget"));
          long album2Budget = row.getLong(0);
          // Transaction will only be committed if this condition still holds at the time of
          // commit. Otherwise it will be aborted and the callable will be rerun by the
          // client library.
          long transfer = 200000;
          if (album2Budget >= transfer) {
            long album1Budget =
                transaction
                    .readRow("Albums", Key.of(1, 1), Arrays.asList("MarketingBudget"))
                    .getLong(0);
            album1Budget += transfer;
            album2Budget -= transfer;
            transaction.buffer(
                Mutation.newUpdateBuilder("Albums")
                    .set("SingerId")
                    .to(1)
                    .set("AlbumId")
                    .to(1)
                    .set("MarketingBudget")
                    .to(album1Budget)
                    .build());
            transaction.buffer(
                Mutation.newUpdateBuilder("Albums")
                    .set("SingerId")
                    .to(2)
                    .set("AlbumId")
                    .to(2)
                    .set("MarketingBudget")
                    .to(album2Budget)
                    .build());
          }
          return null;
        });
  }
  // [END spanner_read_write_transaction]

  // [START spanner_query_data_with_new_column]
  static void queryMarketingBudget(DatabaseClient dbClient) {
    // Rows without an explicit value for MarketingBudget will have a MarketingBudget equal to
    // null. A try-with-resource block is used to automatically release resources held by
    // ResultSet.
    try (ResultSet resultSet =
             dbClient
                 .singleUse()
                 .executeQuery(Statement.of("SELECT singerid as \"SingerId\", "
                     + "albumid as \"AlbumId\", marketingbudget as \"MarketingBudget\" "
                     + "FROM Albums"))) {
      while (resultSet.next()) {
        System.out.printf(
            "%d %d %s\n",
            resultSet.getLong("SingerId"),
            resultSet.getLong("AlbumId"),
            // We check that the value is non null. ResultSet getters can only be used to retrieve
            // non null values.
            resultSet.isNull("MarketingBudget") ? "NULL" :
                resultSet.getLong("MarketingBudget"));
      }
    }
  }
  // [END spanner_query_data_with_new_column]

  // [START spanner_create_index]
  static void addIndex(DatabaseAdminClient adminClient, DatabaseId dbId) {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op =
        adminClient.updateDatabaseDdl(
            dbId.getInstanceId().getInstance(),
            dbId.getDatabase(),
            Arrays.asList("CREATE INDEX AlbumsByAlbumTitle ON Albums(AlbumTitle)"),
            null);
    try {
      // Initiate the request which returns an OperationFuture.
      op.get();
      System.out.println("Added AlbumsByAlbumTitle index");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
  // [END spanner_create_index]

  // [START spanner_read_data_with_index]
  static void readUsingIndex(DatabaseClient dbClient) {
    try (ResultSet resultSet =
             dbClient
                 .singleUse()
                 .readUsingIndex(
                     "Albums",
                     "AlbumsByAlbumTitle",
                     KeySet.all(),
                     Arrays.asList("AlbumId", "AlbumTitle"))) {
      while (resultSet.next()) {
        System.out.printf("%d %s\n", resultSet.getLong(0), resultSet.getString(1));
      }
    }
  }
  // [END spanner_read_data_with_index]

  // [START spanner_create_storing_index]
  static void addStoringIndex(DatabaseAdminClient adminClient, DatabaseId dbId) {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op = adminClient.updateDatabaseDdl(
        dbId.getInstanceId().getInstance(),
        dbId.getDatabase(),
        Arrays.asList(
            "CREATE INDEX AlbumsByAlbumTitle2 ON Albums(AlbumTitle) "
                + "INCLUDE (MarketingBudget)"),
        null);
    try {
      // Initiate the request which returns an OperationFuture.
      op.get();
      System.out.println("Added AlbumsByAlbumTitle2 index");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
  // [END spanner_create_storing_index]

  // Before running this example, create a storing index AlbumsByAlbumTitle2 by applying the DDL
  // statement "CREATE INDEX AlbumsByAlbumTitle2 ON Albums(AlbumTitle) STORING (MarketingBudget)".
  // [START spanner_read_data_with_storing_index]
  static void readStoringIndex(DatabaseClient dbClient) {
    // We can read MarketingBudget also from the index since it stores a copy of MarketingBudget.
    try (ResultSet resultSet =
             dbClient
                 .singleUse()
                 .readUsingIndex(
                     "Albums",
                     "AlbumsByAlbumTitle2",
                     KeySet.all(),
                     Arrays.asList("AlbumId", "AlbumTitle", "MarketingBudget"))) {
      while (resultSet.next()) {
        System.out.printf(
            "%d %s %s\n",
            resultSet.getLong(0),
            resultSet.getString(1),
            // TODO: MarketingBudget uppercase not supported, keeping lowercase for now
            resultSet.isNull("marketingbudget") ? "NULL" : resultSet.getLong(2));
      }
    }
  }
  // [END spanner_read_data_with_storing_index]

  // [START spanner_read_only_transaction]
  static void readOnlyTransaction(DatabaseClient dbClient) {
    // ReadOnlyTransaction must be closed by calling close() on it to release resources held by it.
    // We use a try-with-resource block to automatically do so.
    try (ReadOnlyTransaction transaction = dbClient.readOnlyTransaction()) {
      ResultSet queryResultSet =
          transaction.executeQuery(
              Statement.of("SELECT SingerId, AlbumId, AlbumTitle FROM Albums"));
      while (queryResultSet.next()) {
        System.out.printf(
            "%d %d %s\n",
            queryResultSet.getLong(0), queryResultSet.getLong(1),
            queryResultSet.getString(2));
      }
      try (ResultSet readResultSet =
               transaction.read(
                   "Albums", KeySet.all(), Arrays.asList("SingerId", "AlbumId", "AlbumTitle"))) {
        while (readResultSet.next()) {
          System.out.printf(
              "%d %d %s\n",
              readResultSet.getLong(0), readResultSet.getLong(1),
              readResultSet.getString(2));
        }
      }
    }
  }
  // [END spanner_read_only_transaction]

  // [START spanner_query_singers_table]
  static void querySingersTable(DatabaseClient dbClient) {
    try (ResultSet resultSet =
             dbClient
                 .singleUse()
                 .executeQuery(Statement.of("SELECT singerid as \"SingerId\", "
                     + "firstname as \"FirstName\", lastname as \"LastName\" FROM Singers"))) {
      while (resultSet.next()) {
        System.out.printf(
            "%s %s %s\n",
            resultSet.getLong("SingerId"),
            resultSet.getString("FirstName"),
            resultSet.getString("LastName"));
      }
    }
  }
  // [END spanner_query_singers_table]


  // [START spanner_dml_getting_started_insert]
  static void writeUsingDml(DatabaseClient dbClient) {
    // Insert 4 singer records
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql =
              "INSERT INTO Singers (SingerId, FirstName, LastName) VALUES "
                  + "(12, 'Melissa', 'Garcia'), "
                  + "(13, 'Russell', 'Morales'), "
                  + "(14, 'Jacqueline', 'Long'), "
                  + "(15, 'Dylan', 'Shaw')";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d records inserted.\n", rowCount);
          return null;
        });
  }
  // [END spanner_dml_getting_started_insert]

  // [START spanner_query_with_parameter]
  static void queryWithParameter(DatabaseClient dbClient) {
    Statement statement =
        Statement.newBuilder(
                "SELECT singerid AS \"SingerId\", "
                    + "firstname as \"FirstName\", lastname as \"LastName\" "
                    + "FROM Singers "
                    + "WHERE LastName = $1")
            .bind("p1")
            .to("Garcia")
            .build();
    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        System.out.printf(
            "%d %s %s\n",
            resultSet.getLong("SingerId"),
            resultSet.getString("FirstName"),
            resultSet.getString("LastName"));
      }
    }
  }
  // [END spanner_query_with_parameter]

  // [START spanner_dml_getting_started_update]
  static void writeWithTransactionUsingDml(DatabaseClient dbClient) {
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          // Transfer marketing budget from one album to another. We do it in a transaction to
          // ensure that the transfer is atomic.
          String sql1 =
              "SELECT marketingbudget as \"MarketingBudget\" from Albums WHERE "
                  + "SingerId = 2 and AlbumId = 2";
          ResultSet resultSet = transaction.executeQuery(Statement.of(sql1));
          long album2Budget = 0;
          while (resultSet.next()) {
            album2Budget = resultSet.getLong("MarketingBudget");
          }
          // Transaction will only be committed if this condition still holds at the time of
          // commit. Otherwise it will be aborted and the callable will be rerun by the
          // client library.
          long transfer = 200000;
          if (album2Budget >= transfer) {
            String sql2 =
                "SELECT marketingbudget as \"MarketingBudget\" from Albums WHERE "
                    + "SingerId = 1 and AlbumId = 1";
            ResultSet resultSet2 = transaction.executeQuery(Statement.of(sql2));
            long album1Budget = 0;
            while (resultSet2.next()) {
              album1Budget = resultSet2.getLong("MarketingBudget");
            }
            album1Budget += transfer;
            album2Budget -= transfer;
            Statement updateStatement =
                Statement.newBuilder(
                        "UPDATE Albums "
                            + "SET MarketingBudget = $1"
                            + "WHERE SingerId = 1 and AlbumId = 1")
                    .bind("p1")
                    .to(album1Budget)
                    .build();
            transaction.executeUpdate(updateStatement);
            Statement updateStatement2 =
                Statement.newBuilder(
                        "UPDATE Albums "
                            + "SET MarketingBudget = $1"
                            + "WHERE SingerId = 2 and AlbumId = 2")
                    .bind("p1")
                    .to(album2Budget)
                    .build();
            transaction.executeUpdate(updateStatement2);
          }
          return null;
        });
  }
  // [END spanner_dml_getting_started_update]

  // [START spanner_create_table_using_ddl]
  static void createTableUsingDdl(DatabaseAdminClient dbAdminClient, DatabaseId id) {
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op =
        dbAdminClient.updateDatabaseDdl(
            id.getInstanceId().getInstance(),
            id.getDatabase(),
            Arrays.asList(
                "CREATE TABLE Singers ("
                    + "  SingerId   bigint NOT NULL,"
                    + "  FirstName  character varying(1024),"
                    + "  LastName   character varying(1024),"
                    + "  SingerInfo bytea,"
                    + "  PRIMARY KEY (SingerId)"
                    + ")",
                "CREATE TABLE Albums ("
                    + "  SingerId     bigint NOT NULL,"
                    + "  AlbumId      bigint NOT NULL,"
                    + "  AlbumTitle   character varying(1024),"
                    + "  PRIMARY KEY (SingerId, AlbumId)"
                    + ") INTERLEAVE IN PARENT Singers ON DELETE CASCADE"),
            null);
    try {
      // Initiate the request which returns an OperationFuture.
      op.get();
      System.out.println("Created Singers & Albums tables in database: [" + id + "]");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
  // [END spanner_create_table_using_ddl]

  static void run(
      DatabaseClient dbClient,
      DatabaseAdminClient dbAdminClient,
      String command,
      DatabaseId database) {
    switch (command) {
      case "createdatabase":
        createDatabase(dbAdminClient, database);
        break;
      case "write":
        writeExampleData(dbClient);
        break;
      case "query":
        query(dbClient);
        break;
      case "read":
        read(dbClient);
        break;
      case "addmarketingbudget":
        addMarketingBudget(dbAdminClient, database);
        break;
      case "update":
        update(dbClient);
        break;
      case "writetransaction":
        writeWithTransaction(dbClient);
        break;
      case "querymarketingbudget":
        queryMarketingBudget(dbClient);
        break;
      case "addindex":
        addIndex(dbAdminClient, database);
        break;
      case "readindex":
        readUsingIndex(dbClient);
        break;
      case "addstoringindex":
        addStoringIndex(dbAdminClient, database);
        break;
      case "readstoringindex":
        readStoringIndex(dbClient);
        break;
      case "readonlytransaction":
        readOnlyTransaction(dbClient);
        break;
      case "querysingerstable":
        querySingersTable(dbClient);
        break;
      case "writeusingdml":
        writeUsingDml(dbClient);
        break;
      case "querywithparameter":
        queryWithParameter(dbClient);
        break;
      case "writewithtransactionusingdml":
        writeWithTransactionUsingDml(dbClient);
        break;
      case "createtableusingddl":
        createTableUsingDdl(dbAdminClient, database);
        break;
      default:
        printUsageAndExit();
    }
  }

  static void printUsageAndExit() {
    System.err.println("Usage:");
    System.err.println("    PgSpannerExample <command> <instance_id> <database_id>");
    System.err.println();
    System.err.println("Examples:");
    System.err.println("    PgSpannerExample createdatabase my-instance example-db");
    System.err.println("    PgSpannerExample write my-instance example-db");
    System.err.println("    PgSpannerExample delete my-instance example-db");
    System.err.println("    PgSpannerExample query my-instance example-db");
    System.err.println("    PgSpannerExample read my-instance example-db");
    System.err.println("    PgSpannerExample addmarketingbudget my-instance example-db");
    System.err.println("    PgSpannerExample update my-instance example-db");
    System.err.println("    PgSpannerExample writetransaction my-instance example-db");
    System.err.println("    PgSpannerExample querymarketingbudget my-instance example-db");
    System.err.println("    PgSpannerExample addindex my-instance example-db");
    System.err.println("    PgSpannerExample readindex my-instance example-db");
    System.err.println("    PgSpannerExample addstoringindex my-instance example-db");
    System.err.println("    PgSpannerExample readstoringindex my-instance example-db");
    System.err.println("    PgSpannerExample readonlytransaction my-instance example-db");
    System.err.println("    PgSpannerExample querysingerstable my-instance example-db");
    System.err.println("    PgSpannerExample writeusingdml my-instance example-db");
    System.err.println("    PgSpannerExample querywithparameter my-instance example-db");
    System.err.println("    PgSpannerExample writewithtransactionusingdml my-instance example-db");
    System.err.println("    PgSpannerExample createtableforsamples my-instance example-db");
    System.exit(1);
  }

  public static void main(String[] args) {
    if (args.length != 3) {
      printUsageAndExit();
    }
    // [START spanner_init_client]
    SpannerOptions options = SpannerOptions.newBuilder().build();
    Spanner spanner = options.getService();
    try {
      // [END spanner_init_client]
      String command = args[0];
      DatabaseId db = DatabaseId.of(options.getProjectId(), args[1], args[2]);

      // This will return the default project id based on the environment.
      String clientProject = spanner.getOptions().getProjectId();
      if (!db.getInstanceId().getProject().equals(clientProject)) {
        System.err.println(
            "Invalid project specified. Project in the database id should match the"
                + "project name set in the environment variable GOOGLE_CLOUD_PROJECT. Expected: "
                + clientProject);
        printUsageAndExit();
      }
      // [START spanner_init_client]
      DatabaseClient dbClient = spanner.getDatabaseClient(db);
      DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();
      // [END spanner_init_client]

      // Use client here...
      run(dbClient, dbAdminClient, command, db);
      // [START spanner_init_client]
    } finally {
      spanner.close();
    }
    // [END spanner_init_client]
    System.out.println("Closed client");
  }

  /** Class to contain singer sample data. */
  static class Singer {

    final long singerId;
    final String firstName;
    final String lastName;

    Singer(long singerId, String firstName, String lastName) {
      this.singerId = singerId;
      this.firstName = firstName;
      this.lastName = lastName;
    }
  }

  /** Class to contain album sample data. */
  static class Album {

    final long singerId;
    final long albumId;
    final String albumTitle;

    Album(long singerId, long albumId, String albumTitle) {
      this.singerId = singerId;
      this.albumId = albumId;
      this.albumTitle = albumTitle;
    }
  }
}
