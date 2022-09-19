package com.example.spanner.protobuf;

import com.example.spanner.protobuf.book.Book;
import com.example.spanner.protobuf.book.Genre;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Value;
import com.google.common.collect.ImmutableList;

public class UpdateProtoColumnSample {
  static void updateProtoData() {
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";

    try (Spanner spanner =
        SpannerOptions.newBuilder().setProjectId(projectId).build().getService()) {
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
      updateProtoData(client);
    }
  }

  /*
   * Assuming a table Library with two columns bookId & bookProto
   * */
  static void updateProtoData(DatabaseClient client) {
    Book bookProto =
        Book.newBuilder()
            .setAuthor("J.K. Rowling")
            .setGenre(Genre.ROCK)
            .setTitle("Harry Potter")
            .build();
    client.write(
        ImmutableList.of(
            Mutation.newInsertOrUpdateBuilder("Library")
                .set("bookId")
                .to(4L)
                .set("bookProto")
                .to(Value.protoMessage(bookProto))
                .set("genre")
                .to(Value.protoEnum(Genre.ROCK))
                .build()));
  }
}
