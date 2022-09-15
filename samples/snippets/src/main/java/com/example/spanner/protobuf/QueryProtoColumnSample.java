package com.example.spanner.protobuf;

import com.example.spanner.protobuf.book.Book;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import com.google.protobuf.InvalidProtocolBufferException;

public class QueryProtoColumnSample {
  static void queryProtoColumn() {
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      queryProtoColumn(client);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  static void queryProtoColumn(DatabaseClient client) throws InvalidProtocolBufferException {
    Statement statement =
        Statement.newBuilder("SELECT bookId,  bookProto\n" + "FROM Library").build();

    try (ResultSet resultSet = client.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        System.out.printf(
            "bookId: %s, bookProto: %s%n",
            resultSet.getLong("bookId"),
            resultSet.getProtoMessage("bookProto", Book.getDefaultInstance()));
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }
}
