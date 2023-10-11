package com.google.cloud.spanner;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminSettings;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import com.google.spanner.v1.TransactionOptions.ReadWrite;
import io.grpc.ManagedChannelBuilder;

public class EmulatorUtil {

  /** Removes all read/write transactions for all databases on the emulator for the given project. */
  public static void resetAllEmulatorTransactions(String project) throws Exception {
    try (InstanceAdminClient instanceAdminClient =
            InstanceAdminClient.create(
                InstanceAdminSettings.newBuilder()
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .setTransportChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder()
                            .setEndpoint("localhost:9010")
                            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
                            .build())
                    .build());
        DatabaseAdminClient databaseAdminClient =
            DatabaseAdminClient.create(
                DatabaseAdminSettings.newBuilder()
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .setTransportChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder()
                            .setEndpoint("localhost:9010")
                            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
                            .build())
                    .build())) {
      for (Instance instance :
          instanceAdminClient.listInstances(String.format("projects/%s", project)).iterateAll()) {
        for (Database database :
            databaseAdminClient.listDatabases(instance.getName()).iterateAll()) {
          resetEmulatorTransactions(database);
        }
      }
    }
  }

  /** Removes all read/write transactions for the given database on the emulator. */
  public static void resetEmulatorTransactions(Database database) throws Exception {
    try (SpannerClient spannerClient =
        SpannerClient.create(
            SpannerSettings.newBuilder()
                .setCredentialsProvider(NoCredentialsProvider.create())
                .setTransportChannelProvider(
                    InstantiatingGrpcChannelProvider.newBuilder()
                        .setEndpoint("localhost:9010")
                        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
                        .build())
                .build())) {
      for (Session session : spannerClient.listSessions(database.getName()).iterateAll()) {
        Transaction transaction =
            spannerClient.beginTransaction(
                BeginTransactionRequest.newBuilder()
                    .setSession(session.getName())
                    .setOptions(
                        TransactionOptions.newBuilder()
                            .setReadWrite(ReadWrite.newBuilder().build())
                            .build())
                    .build());
        spannerClient.rollback(
            RollbackRequest.newBuilder()
                .setSession(session.getName())
                .setTransactionId(transaction.getId())
                .build());
      }
    }
  }
}
