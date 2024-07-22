/*
 * Copyright 2024 Google Inc.
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
import com.google.api.gax.longrunning.OperationSnapshot;
import com.google.api.gax.retrying.RetryingFuture;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.StatusCode.Code;
import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeyRange;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Value;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.longrunning.Operation;
import com.google.protobuf.FieldMask;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.database.v1.InstanceName;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Example code for using the Cloud Spanner API. This example demonstrates all the common operations
 * that can be done on Cloud Spanner. These are:
 *
 * <p>
 *
 * <ul>
 *   <li>Creating a Cloud Spanner database with a property graph.
 *   <li>Writing, reading and executing graph queries.
 *   <li>Using Google API Extensions for Java to make thread-safe requests via long-running
 *       operations. http://googleapis.github.io/gax-java/
 * </ul>
 */
public class SpannerGraphSample {

  // [START spanner_insert_graph_data]
  /**
   * Class to contain sample Person data.
   */
  static class Person {

    final long id;
    final String name;
    final String gender;
    final Timestamp birthday;
    final String country;
    final String city; 

    Person(long id, String name, String gender,
           Timestamp birthday, String country, String city) {
      this.id = id;
      this.name = name;
      this.gender = gender;
      this.birthday = birthday;
      this.country = country;
      this.city = city;
    }
  }

  /**
   * Class to contain sample Account data.
   */
  static class Account {

    final long id;
    final Timestamp create_time;
    final boolean is_blocked;
    final String nick_name; 

    Account(long id, Timestamp create_time, boolean is_blocked, String nick_name) {
      this.id = id;
      this.create_time = create_time;
      this.is_blocked = is_blocked;
      this.nick_name = nick_name;
    }
  }

  /**
   * Class to contain sample Transfer data.
   */
  static class Transfer {

    final long id;
    final long to_id;
    final double amount;
    final Timestamp create_time;
    final String order_number;

    Transfer(long id, long to_id, double amount, Timestamp create_time, String order_number) {
      this.id = id;
      this.to_id = to_id;
      this.amount = amount;
      this.create_time = create_time;
      this.order_number = order_number;
    }
  }

  /**
   * Class to contain sample Ownership data.
   */
  static class Own {

    final long id;
    final long account_id;
    final Timestamp create_time;

    Own(long id, long account_id, Timestamp create_time) {
      this.id = id;
      this.account_id = account_id;
      this.create_time = create_time;
    }
  }
  // [END spanner_insert_graph_data]

  // [START spanner_create_database_with_property_graph]
  static void createDatabaseWithPropertyGraph(DatabaseAdminClient dbAdminClient,
      InstanceName instanceName, String databaseId) {
    CreateDatabaseRequest createDatabaseRequest =
        CreateDatabaseRequest.newBuilder()
            .setCreateStatement("CREATE DATABASE `" + databaseId + "`")
            .setParent(instanceName.toString())
            .addAllExtraStatements(Arrays.asList(
                "CREATE TABLE Person ("
                    + "  id               INT64 NOT NULL,"
                    + "  name             STRING(MAX),"
                    + "  gender           STRING(40),"
                    + "  birthday         TIMESTAMP,"
                    + "  country          STRING(MAX),"
                    + "  city             STRING(MAX),"
                    + ") PRIMARY KEY (id)",
                "CREATE TABLE Account ("
                    + "  id               INT64 NOT NULL,"
                    + "  create_time      TIMESTAMP,"
                    + "  is_blocked       BOOL,"
                    + "  nick_name        STRING(MAX),"
                    + ") PRIMARY KEY (id)",
                "CREATE TABLE PersonOwnAccount ("
                    + "  id               INT64 NOT NULL,"
                    + "  account_id       INT64 NOT NULL,"
                    + "  create_time      TIMESTAMP,"
                    + "  FOREIGN KEY (account_id)"
                    + "  REFERENCES Account (id)"
                    + ") PRIMARY KEY (id, account_id),"
                    + "INTERLEAVE IN PARENT Person ON DELETE CASCADE",
                "CREATE TABLE AccountTransferAccount ("
                    + "  id               INT64 NOT NULL,"
                    + "  to_id            INT64 NOT NULL,"
                    + "  amount           FLOAT64,"
                    + "  create_time      TIMESTAMP NOT NULL OPTIONS"
                    + "    (allow_commit_timestamp=true),"
                    + "  order_number     STRING(MAX),"
                    + "  FOREIGN KEY (to_id) REFERENCES Account (id)"
                    + ") PRIMARY KEY (id, to_id, create_time),"
                    + "INTERLEAVE IN PARENT Account ON DELETE CASCADE",
                "CREATE OR REPLACE PROPERTY GRAPH FinGraph "
                    + "NODE TABLES (Account, Person)"
                    + "EDGE TABLES ("
                    + "  PersonOwnAccount"
                    + "    SOURCE KEY(id) REFERENCES Person(id)"
                    + "    DESTINATION KEY(account_id) REFERENCES Account(id)"
                    + "    LABEL Owns,"
                    + "  AccountTransferAccount"
                    + "    SOURCE KEY(id) REFERENCES Account(id)"
                    + "    DESTINATION KEY(to_id) REFERENCES Account(id)"
                    + "    LABEL Transfers)")).build();
    try {
      // Initiate the request which returns an OperationFuture.
      com.google.spanner.admin.database.v1.Database db =
          dbAdminClient.createDatabaseAsync(createDatabaseRequest).get();
      System.out.println("Created database [" + db.getName() + "]");
    } catch (ExecutionException e) {
      // If the operation failed during execution, expose the cause.
      System.out.println("Encountered exception" + e.getCause());
      throw (SpannerException) e.getCause();
    } catch (InterruptedException e) {
      // Throw when a thread is waiting, sleeping, or otherwise occupied,
      // and the thread is interrupted, either before or during the activity.
      throw SpannerExceptionFactory.propagateInterrupt(e);
    }
  }
  // [END spanner_create_database_with_property_graph]

  // [START spanner_insert_graph_data]
  static final List<Account> ACCOUNTS =
      Arrays.asList(
          new Account(7, Timestamp.parseTimestamp("2020-01-10T06:22:20.12Z"), false, "Vacation Fund"),
          new Account(16, Timestamp.parseTimestamp("2020-01-27T17:55:09.12Z"), true, "Vacation Fund"),
          new Account(20, Timestamp.parseTimestamp("2020-02-18T05:44:20.12Z"), false, "Rainy Day Fund"));

  static final List<Person> PERSONS =
      Arrays.asList(
          new Person(1, "Alex", "male", Timestamp.parseTimestamp("1991-12-21T00:00:00.12Z"), "Australia"," Adelaide"),
          new Person(2, "Dana", "female", Timestamp.parseTimestamp("1980-10-31T00:00:00.12Z"),"Czech_Republic", "Moravia"),
          new Person(3, "Lee", "male", Timestamp.parseTimestamp("1986-12-07T00:00:00.12Z"), "India", "Kollam"));

  static final List<Transfer> TRANSFERS =
      Arrays.asList(
        new Transfer(7, 16, 300.0, Timestamp.parseTimestamp("2020-08-29T15:28:58.12Z"), "304330008004315"),
        new Transfer(7, 16, 100.0, Timestamp.parseTimestamp("2020-10-04T16:55:05.12Z"), "304120005529714"),
        new Transfer(16, 20, 300.0, Timestamp.parseTimestamp("2020-09-25T02:36:14.12Z"), "103650009791820"),
        new Transfer(20, 7, 500.0, Timestamp.parseTimestamp("2020-10-04T16:55:05.12Z"), "304120005529714"),
        new Transfer(20, 16, 200.0, Timestamp.parseTimestamp("2020-10-17T03:59:40.12Z"), "302290001255747"));

  static final List<Own> OWNERSHIPS =
      Arrays.asList(
        new Own(1, 7, Timestamp.parseTimestamp("2020-01-10T06:22:20.12Z")),
        new Own(2, 20, Timestamp.parseTimestamp("2020-01-27T17:55:09.12Z")),
        new Own(3, 16, Timestamp.parseTimestamp("2020-02-18T05:44:20.12Z")));

  // [END spanner_insert_graph_data]

  // [START spanner_insert_graph_data]
  static void insertData(DatabaseClient dbClient) {
    List<Mutation> mutations = new ArrayList<>();
    for (Account account: ACCOUNTS) {
      mutations.add(
          Mutation.newInsertBuilder("Account")
              .set("id")
              .to(account.id)
              .set("create_time")
              .to(account.create_time)
              .set("is_blocked")
              .to(account.is_blocked)
              .set("nick_name")
              .to(account.nick_name)
              .build());
    }
    for (Person person : PERSONS) {
      mutations.add(
          Mutation.newInsertBuilder("Person")
              .set("id")
              .to(person.id)
              .set("name")
              .to(person.name)
              .set("gender")
              .to(person.gender)
              .set("birthday")
              .to(person.birthday)
              .set("country")
              .to(person.country)
              .set("city")
              .to(person.city)
              .build());
    }
    for (Transfer transfer: TRANSFERS) {
      mutations.add(
          Mutation.newInsertBuilder("AccountTransferAccount")
              .set("id")
              .to(transfer.id)
              .set("to_id")
              .to(transfer.to_id)
              .set("amount")
              .to(transfer.amount)
              .set("create_time")
              .to(transfer.create_time)
              .set("order_number")
              .to(transfer.order_number)
              .build());
    }
    for (Own own: OWNERSHIPS) {
      mutations.add(
          Mutation.newInsertBuilder("PersonOwnAccount")
              .set("id")
              .to(own.id)
              .set("account_id")
              .to(own.account_id)
              .set("create_time")
              .to(own.create_time)
              .build());
    }

    dbClient.write(mutations);
  }
  // [END spanner_insert_graph_data]

  // [START spanner_insert_graph_data_with_dml]
  static void insertUsingDml(DatabaseClient dbClient) {
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql =
              "INSERT INTO Account (id, create_time, is_blocked) "
              + "  VALUES"
              + "    (1, CAST('2000-08-10 08:18:48.463959-07:52' AS TIMESTAMP), false),"
              + "    (2, CAST('2000-08-12 08:18:48.463959-07:52' AS TIMESTAMP), true)";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d record inserted into Account.\n", rowCount);
          return null;
        });

    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql =
              "INSERT INTO AccountTransferAccount (id, to_id, create_time, amount) "
              +  "  VALUES"
              +  "    (1, 2, PENDING_COMMIT_TIMESTAMP(), 100),"
              +  "    (1, 1, PENDING_COMMIT_TIMESTAMP(), 200) ";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d record inserted into AccountTransferAccount.\n", rowCount);
          return null;
        });
  }
  // [END spanner_insert_graph_data_with_dml]

    // [START spanner_update_graph_data_with_dml]
  static void updateUsingDml(DatabaseClient dbClient) {
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql =
              "UPDATE Account SET is_blocked = false WHERE id = 2";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d Account record(s) updated.\n", rowCount);
          return null;
        });

    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql =
              "UPDATE AccountTransferAccount SET amount = 300 WHERE id = 1 AND to_id = 2";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d AccountTransferAccount record(s) updated.\n", rowCount);
          return null;
        });
  }
  // [END spanner_update_graph_data_with_dml]

    // [START spanner_update_graph_data_with_graph_query_in_dml]
  static void updateUsingGraphQueryInDml(DatabaseClient dbClient) {
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql =
              "UPDATE Account SET is_blocked = true "
              +  "WHERE id IN ("
              +  "  GRAPH FinGraph"
              +  "  MATCH (a:Account WHERE a.id = 1)-[:TRANSFERS]->{1,2}(b:Account)"
              +  "  RETURN b.id)";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d record updated.\n", rowCount);
          return null;
        });
  }
  // [END spanner_update_graph_data_with_graph_query_in_dml]

  // [START spanner_query_graph_data]
  static void query(DatabaseClient dbClient) {
    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single query against Cloud Spanner.
            .executeQuery(Statement.of(
                "Graph FinGraph "
                + "MATCH (a:Person)-[o:Owns]->()-[t:Transfers]->()<-[p:Owns]-(b:Person)"
                + "RETURN a.name AS sender, b.name AS receiver, t.amount, t.create_time AS transfer_at"
    ))) {
      while (resultSet.next()) {
        System.out.printf(
            "%s %s %f %s\n",
            resultSet.getString(0),
            resultSet.getString(1),
            resultSet.getDouble(2),
            resultSet.getTimestamp(3));
      }
    }
  }
  // [END spanner_query_graph_data]

  // [START spanner_query_graph_data_with_parameter]
  static void queryWithParameter(DatabaseClient dbClient) {
    Statement statement =
        Statement.newBuilder(
            "Graph FinGraph "
            + "MATCH (a:Person)-[o:Owns]->()-[t:Transfers]->()<-[p:Owns]-(b:Person) "
            + "WHERE t.amount >= @min " 
            + "RETURN a.name AS sender, b.name AS receiver, t.amount, t.create_time AS transfer_at")
            .bind("min")
            .to(500)
            .build();
    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        System.out.printf(
            "%s %s %f %s\n",
            resultSet.getString("sender"),
            resultSet.getString("receiver"),
            resultSet.getDouble("amount"),
            resultSet.getTimestamp("transfer_at"));
      }
    }
  }
  // [END spanner_query_graph_data_with_parameter]

  // [START spanner_delete_graph_data_with_dml]
  static void deleteUsingDml(DatabaseClient dbClient) {
    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql = "DELETE FROM AccountTransferAccount WHERE id = 1 AND to_id = 2";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d AccountTransferAccount record(s) deleted.\n", rowCount);
          return null;
        });

    dbClient
        .readWriteTransaction()
        .run(transaction -> {
          String sql = "DELETE FROM Account WHERE id = 2";
          long rowCount = transaction.executeUpdate(Statement.of(sql));
          System.out.printf("%d Account record(s) deleted.\n", rowCount);
          return null;
        });
  }
  // [END spanner_delete_graph_data_with_dml]

  // [START spanner_delete_data]
  static void deleteData(DatabaseClient dbClient) {
    List<Mutation> mutations = new ArrayList<>();

    // KeySet.Builder can be used to delete a specific set of rows.
    // Delete the PersonOwnAccount rows with the key values (1,7) and (2,20).
    mutations.add(
        Mutation.delete(
            "PersonOwnAccount",
            KeySet.newBuilder()
                .addKey(Key.of(1, 7))
                .addKey(Key.of(2, 20))
                .build()));

    // KeyRange can be used to delete rows with a key in a specific range.
    // Delete a range of rows where the key prefix is >=1 and <8
    mutations.add(
        Mutation.delete(
            "AccountTransferAccount",
            KeySet.range(KeyRange.closedOpen(Key.of(1), Key.of(8)))));

    // KeySet.all() can be used to delete all the rows in a table.
    // Delete all Account rows, which will also delete the remaining
    // AccountTransferAccount rows since it was defined with ON DELETE CASCADE.
    mutations.add(Mutation.delete("Account", KeySet.all()));

    // KeySet.all() can be used to delete all the rows in a table.
    // Delete all Person rows, which will also delete the remaining
    // PersonOwnAccount rows since it was defined with ON DELETE CASCADE.
    mutations.add(Mutation.delete("Person", KeySet.all()));

    dbClient.write(mutations);
    System.out.printf("Records deleted.\n");
  }
  // [END spanner_delete_data]

  static void run(
      DatabaseClient dbClient,
      DatabaseAdminClient dbAdminClient,
      String command,
      DatabaseId database) {
    switch (command) {
      case "createdatabase":
        createDatabaseWithPropertyGraph(dbAdminClient, InstanceName.of(database.getInstanceId().getProject(),
            database.getInstanceId().getInstance()), database.getDatabase());
        break;
      case "insert":
        insertData(dbClient);
        break;
      case "insertusingdml":
        insertUsingDml(dbClient);
        break;
      case "updateusingdml":
        updateUsingDml(dbClient);
        break;
      case "updateusinggraphqueryindml":
        updateUsingGraphQueryInDml(dbClient);
        break;
      case "query":
        query(dbClient);
        break;
      case "querywithparameter":
        queryWithParameter(dbClient);
        break;
      case "deleteusingdml":
        deleteUsingDml(dbClient);
        break;
      case "delete":
        deleteData(dbClient);
        break;
      default:
        printUsageAndExit();
    }
  }

  static void printUsageAndExit() {
    System.err.println("Usage:");
    System.err.println("    SpannerGraphExample <command> <instance_id> <database_id>");
    System.err.println("");
    System.err.println("Examples:");
    System.err.println("    SpannerGraphExample createdatabase my-instance example-db");
    System.err.println("    SpannerGraphExample insert my-instance example-db");
    System.err.println("    SpannerGraphExample insertusingdml my-instance example-db");
    System.err.println("    SpannerGraphExample updateusingdml my-instance example-db");
    System.err.println("    SpannerGraphExample updateusinggraphqueryindml my-instance example-db");
    System.err.println("    SpannerGraphExample query my-instance example-db");
    System.err.println("    SpannerGraphExample querywithparameter my-instance example-db");
    System.err.println("    SpannerGraphExample deleteusingdml my-instance example-db");
    System.err.println("    SpannerGraphExample delete my-instance example-db");
    System.exit(1);
  }

  public static void main(String[] args) {
    if (args.length != 3 && args.length != 4) {
      printUsageAndExit();
    }
    // [START init_client]
    SpannerOptions options = SpannerOptions.newBuilder().build();
    Spanner spanner = options.getService();
    DatabaseAdminClient dbAdminClient = null;
    try {
      final String command = args[0];
      DatabaseId db = DatabaseId.of(options.getProjectId(), args[1], args[2]);
      // [END init_client]
      // This will return the default project id based on the environment.
      String clientProject = spanner.getOptions().getProjectId();
      if (!db.getInstanceId().getProject().equals(clientProject)) {
        System.err.println(
            "Invalid project specified. Project in the database id should match the"
                + "project name set in the environment variable GOOGLE_CLOUD_PROJECT. Expected: "
                + clientProject);
        printUsageAndExit();
      }

      // [START init_client]
      DatabaseClient dbClient = spanner.getDatabaseClient(db);
      dbAdminClient = spanner.createDatabaseAdminClient();

      // Use client here...
      // [END init_client]

      run(dbClient, dbAdminClient, command, db);
      // [START init_client]
    } finally {
      if (dbAdminClient != null) {
        if (!dbAdminClient.isShutdown() || !dbAdminClient.isTerminated()) {
          dbAdminClient.close();
        }
      }
      spanner.close();
    }
    // [END init_client]
    System.out.println("Closed client");
  }
}
