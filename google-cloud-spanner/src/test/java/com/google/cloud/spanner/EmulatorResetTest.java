/*
 * Copyright 2023 Google LLC
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

package com.google.cloud.spanner;

import static com.google.cloud.spanner.EmulatorUtil.resetAllEmulatorTransactions;
import static org.junit.Assert.assertThrows;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.AbortedException;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminSettings;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.cloud.spanner.v1.SpannerClient;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.instance.v1.CreateInstanceRequest;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CreateSessionRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import com.google.spanner.v1.TransactionOptions.ReadWrite;
import com.google.spanner.v1.TransactionSelector;
import io.grpc.ManagedChannelBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Ignore
@RunWith(JUnit4.class)
public class EmulatorResetTest {

  @Test
  public void testResetEmulator_removesAllTransactions() throws Exception {
    String project = "emulator-project";
    String instance = "test-instance";
    String database = "test-database";

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
                    .build());
        SpannerClient spannerClient =
            SpannerClient.create(
                SpannerSettings.newBuilder()
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .setTransportChannelProvider(
                        InstantiatingGrpcChannelProvider.newBuilder()
                            .setEndpoint("localhost:9010")
                            .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
                            .build())
                    .build())) {
      instanceAdminClient
          .createInstanceAsync(
              CreateInstanceRequest.newBuilder()
                  .setParent("projects/" + project)
                  .setInstanceId(instance)
                  .setInstance(
                      Instance.newBuilder()
                          .setConfig("projects/" + project + "/instanceConfigs/emulator-config")
                          .setDisplayName("Test Instance")
                          .setNodeCount(1)
                          .build())
                  .build())
          .get();
      databaseAdminClient
          .createDatabaseAsync(
              CreateDatabaseRequest.newBuilder()
                  .setParent("projects/" + project + "/instances/" + instance)
                  .setCreateStatement("create database `" + database + "`")
                  .addAllExtraStatements(
                      ImmutableList.of(
                          "create table test (id int64, value string(max)) primary key (id)"))
                  .build())
          .get();

      Session session =
          spannerClient.createSession(
              CreateSessionRequest.newBuilder()
                  .setDatabase(
                      "projects/" + project + "/instances/" + instance + "/databases/" + database)
                  .build());
      Session session2 =
          spannerClient.createSession(
              CreateSessionRequest.newBuilder()
                  .setDatabase(
                      "projects/" + project + "/instances/" + instance + "/databases/" + database)
                  .build());

      Transaction transaction =
          spannerClient.beginTransaction(
              BeginTransactionRequest.newBuilder()
                  .setSession(session.getName())
                  .setOptions(
                      TransactionOptions.newBuilder()
                          .setReadWrite(ReadWrite.newBuilder().build())
                          .build())
                  .build());
      spannerClient.executeSql(
          ExecuteSqlRequest.newBuilder()
              .setTransaction(TransactionSelector.newBuilder().setId(transaction.getId()).build())
              .setSql("select * from test")
              .setSession(session.getName())
              .build());

      Transaction transaction2 =
          spannerClient.beginTransaction(
              BeginTransactionRequest.newBuilder()
                  .setSession(session2.getName())
                  .setOptions(
                      TransactionOptions.newBuilder()
                          .setReadWrite(ReadWrite.newBuilder().build())
                          .build())
                  .build());
      assertThrows(
          AbortedException.class,
          () ->
              spannerClient.executeSql(
                  ExecuteSqlRequest.newBuilder()
                      .setTransaction(
                          TransactionSelector.newBuilder().setId(transaction2.getId()).build())
                      .setSql("select * from test")
                      .setSession(session2.getName())
                      .build()));

      resetAllEmulatorTransactions(project);

      Session session3 =
          spannerClient.createSession(
              CreateSessionRequest.newBuilder()
                  .setDatabase(
                      "projects/" + project + "/instances/" + instance + "/databases/" + database)
                  .build());
      Transaction transaction3 =
          spannerClient.beginTransaction(
              BeginTransactionRequest.newBuilder()
                  .setSession(session3.getName())
                  .setOptions(
                      TransactionOptions.newBuilder()
                          .setReadWrite(ReadWrite.newBuilder().build())
                          .build())
                  .build());
      spannerClient.executeSql(
          ExecuteSqlRequest.newBuilder()
              .setTransaction(TransactionSelector.newBuilder().setId(transaction3.getId()).build())
              .setSql("select * from test")
              .setSession(session3.getName())
              .build());

      instanceAdminClient.deleteInstance("projects/" + project + "/instances/" + instance);
    }
  }
}
