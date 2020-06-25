/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.spanner.v1;

import static com.google.cloud.spanner.v1.SpannerClient.ListSessionsPagedResponse;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GaxGrpcProperties;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.grpc.testing.MockGrpcService;
import com.google.api.gax.grpc.testing.MockServiceHelper;
import com.google.api.gax.grpc.testing.MockStreamObserver;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.ServerStreamingCallable;
import com.google.api.gax.rpc.StatusCode;
import com.google.common.collect.Lists;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BatchCreateSessionsResponse;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CommitResponse;
import com.google.spanner.v1.CreateSessionRequest;
import com.google.spanner.v1.DatabaseName;
import com.google.spanner.v1.DeleteSessionRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteBatchDmlResponse;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.GetSessionRequest;
import com.google.spanner.v1.KeySet;
import com.google.spanner.v1.ListSessionsRequest;
import com.google.spanner.v1.ListSessionsResponse;
import com.google.spanner.v1.Mutation;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.PartitionQueryRequest;
import com.google.spanner.v1.PartitionReadRequest;
import com.google.spanner.v1.PartitionResponse;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.SessionName;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import com.google.spanner.v1.TransactionSelector;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@javax.annotation.Generated("by GAPIC")
public class SpannerClientTest {
  private static MockSpanner mockSpanner;
  private static MockServiceHelper serviceHelper;
  private SpannerClient client;
  private LocalChannelProvider channelProvider;

  @BeforeClass
  public static void startStaticServer() {
    mockSpanner = new MockSpanner();
    serviceHelper =
        new MockServiceHelper(
            UUID.randomUUID().toString(), Arrays.<MockGrpcService>asList(mockSpanner));
    serviceHelper.start();
  }

  @AfterClass
  public static void stopServer() {
    serviceHelper.stop();
  }

  @Before
  public void setUp() throws IOException {
    serviceHelper.reset();
    channelProvider = serviceHelper.createChannelProvider();
    SpannerSettings settings =
        SpannerSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    client = SpannerClient.create(settings);
  }

  @After
  public void tearDown() {
    client.close();
  }

  @Test
  @SuppressWarnings("all")
  public void createSessionTest() {
    SessionName name = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    Session expectedResponse = Session.newBuilder().setName(name.toString()).build();
    mockSpanner.addResponse(expectedResponse);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    Session actualResponse = client.createSession(database);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateSessionRequest actualRequest = (CreateSessionRequest) actualRequests.get(0);

    Assert.assertEquals(database, DatabaseName.parse(actualRequest.getDatabase()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void createSessionExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

      client.createSession(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void batchCreateSessionsTest() {
    BatchCreateSessionsResponse expectedResponse = BatchCreateSessionsResponse.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    int sessionCount = 185691686;

    BatchCreateSessionsResponse actualResponse = client.batchCreateSessions(database, sessionCount);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    BatchCreateSessionsRequest actualRequest = (BatchCreateSessionsRequest) actualRequests.get(0);

    Assert.assertEquals(database, DatabaseName.parse(actualRequest.getDatabase()));
    Assert.assertEquals(sessionCount, actualRequest.getSessionCount());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void batchCreateSessionsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
      int sessionCount = 185691686;

      client.batchCreateSessions(database, sessionCount);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void getSessionTest() {
    SessionName name2 = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    Session expectedResponse = Session.newBuilder().setName(name2.toString()).build();
    mockSpanner.addResponse(expectedResponse);

    SessionName name = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");

    Session actualResponse = client.getSession(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetSessionRequest actualRequest = (GetSessionRequest) actualRequests.get(0);

    Assert.assertEquals(name, SessionName.parse(actualRequest.getName()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void getSessionExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName name = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");

      client.getSession(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void listSessionsTest() {
    String nextPageToken = "";
    Session sessionsElement = Session.newBuilder().build();
    List<Session> sessions = Arrays.asList(sessionsElement);
    ListSessionsResponse expectedResponse =
        ListSessionsResponse.newBuilder()
            .setNextPageToken(nextPageToken)
            .addAllSessions(sessions)
            .build();
    mockSpanner.addResponse(expectedResponse);

    DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

    ListSessionsPagedResponse pagedListResponse = client.listSessions(database);

    List<Session> resources = Lists.newArrayList(pagedListResponse.iterateAll());
    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSessionsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListSessionsRequest actualRequest = (ListSessionsRequest) actualRequests.get(0);

    Assert.assertEquals(database, DatabaseName.parse(actualRequest.getDatabase()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void listSessionsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      DatabaseName database = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");

      client.listSessions(database);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void deleteSessionTest() {
    Empty expectedResponse = Empty.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName name = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");

    client.deleteSession(name);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteSessionRequest actualRequest = (DeleteSessionRequest) actualRequests.get(0);

    Assert.assertEquals(name, SessionName.parse(actualRequest.getName()));
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void deleteSessionExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName name = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");

      client.deleteSession(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void executeSqlTest() {
    ResultSet expectedResponse = ResultSet.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String sql = "sql114126";
    ExecuteSqlRequest request =
        ExecuteSqlRequest.newBuilder().setSession(session.toString()).setSql(sql).build();

    ResultSet actualResponse = client.executeSql(request);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ExecuteSqlRequest actualRequest = (ExecuteSqlRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(sql, actualRequest.getSql());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void executeSqlExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      String sql = "sql114126";
      ExecuteSqlRequest request =
          ExecuteSqlRequest.newBuilder().setSession(session.toString()).setSql(sql).build();

      client.executeSql(request);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void executeStreamingSqlTest() throws Exception {
    boolean chunkedValue = true;
    ByteString resumeToken = ByteString.copyFromUtf8("103");
    PartialResultSet expectedResponse =
        PartialResultSet.newBuilder()
            .setChunkedValue(chunkedValue)
            .setResumeToken(resumeToken)
            .build();
    mockSpanner.addResponse(expectedResponse);
    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String sql = "sql114126";
    ExecuteSqlRequest request =
        ExecuteSqlRequest.newBuilder().setSession(session.toString()).setSql(sql).build();

    MockStreamObserver<PartialResultSet> responseObserver = new MockStreamObserver<>();

    ServerStreamingCallable<ExecuteSqlRequest, PartialResultSet> callable =
        client.executeStreamingSqlCallable();
    callable.serverStreamingCall(request, responseObserver);

    List<PartialResultSet> actualResponses = responseObserver.future().get();
    Assert.assertEquals(1, actualResponses.size());
    Assert.assertEquals(expectedResponse, actualResponses.get(0));
  }

  @Test
  @SuppressWarnings("all")
  public void executeStreamingSqlExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);
    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String sql = "sql114126";
    ExecuteSqlRequest request =
        ExecuteSqlRequest.newBuilder().setSession(session.toString()).setSql(sql).build();

    MockStreamObserver<PartialResultSet> responseObserver = new MockStreamObserver<>();

    ServerStreamingCallable<ExecuteSqlRequest, PartialResultSet> callable =
        client.executeStreamingSqlCallable();
    callable.serverStreamingCall(request, responseObserver);

    try {
      List<PartialResultSet> actualResponses = responseObserver.future().get();
      Assert.fail("No exception thrown");
    } catch (ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof InvalidArgumentException);
      InvalidArgumentException apiException = (InvalidArgumentException) e.getCause();
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  @SuppressWarnings("all")
  public void executeBatchDmlTest() {
    ExecuteBatchDmlResponse expectedResponse = ExecuteBatchDmlResponse.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    TransactionSelector transaction = TransactionSelector.newBuilder().build();
    List<ExecuteBatchDmlRequest.Statement> statements = new ArrayList<>();
    long seqno = 109325920L;
    ExecuteBatchDmlRequest request =
        ExecuteBatchDmlRequest.newBuilder()
            .setSession(session.toString())
            .setTransaction(transaction)
            .addAllStatements(statements)
            .setSeqno(seqno)
            .build();

    ExecuteBatchDmlResponse actualResponse = client.executeBatchDml(request);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ExecuteBatchDmlRequest actualRequest = (ExecuteBatchDmlRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(transaction, actualRequest.getTransaction());
    Assert.assertEquals(statements, actualRequest.getStatementsList());
    Assert.assertEquals(seqno, actualRequest.getSeqno());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void executeBatchDmlExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      TransactionSelector transaction = TransactionSelector.newBuilder().build();
      List<ExecuteBatchDmlRequest.Statement> statements = new ArrayList<>();
      long seqno = 109325920L;
      ExecuteBatchDmlRequest request =
          ExecuteBatchDmlRequest.newBuilder()
              .setSession(session.toString())
              .setTransaction(transaction)
              .addAllStatements(statements)
              .setSeqno(seqno)
              .build();

      client.executeBatchDml(request);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void readTest() {
    ResultSet expectedResponse = ResultSet.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String table = "table110115790";
    List<String> columns = new ArrayList<>();
    KeySet keySet = KeySet.newBuilder().build();
    ReadRequest request =
        ReadRequest.newBuilder()
            .setSession(session.toString())
            .setTable(table)
            .addAllColumns(columns)
            .setKeySet(keySet)
            .build();

    ResultSet actualResponse = client.read(request);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ReadRequest actualRequest = (ReadRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(table, actualRequest.getTable());
    Assert.assertEquals(columns, actualRequest.getColumnsList());
    Assert.assertEquals(keySet, actualRequest.getKeySet());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void readExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      String table = "table110115790";
      List<String> columns = new ArrayList<>();
      KeySet keySet = KeySet.newBuilder().build();
      ReadRequest request =
          ReadRequest.newBuilder()
              .setSession(session.toString())
              .setTable(table)
              .addAllColumns(columns)
              .setKeySet(keySet)
              .build();

      client.read(request);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void streamingReadTest() throws Exception {
    boolean chunkedValue = true;
    ByteString resumeToken = ByteString.copyFromUtf8("103");
    PartialResultSet expectedResponse =
        PartialResultSet.newBuilder()
            .setChunkedValue(chunkedValue)
            .setResumeToken(resumeToken)
            .build();
    mockSpanner.addResponse(expectedResponse);
    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String table = "table110115790";
    List<String> columns = new ArrayList<>();
    KeySet keySet = KeySet.newBuilder().build();
    ReadRequest request =
        ReadRequest.newBuilder()
            .setSession(session.toString())
            .setTable(table)
            .addAllColumns(columns)
            .setKeySet(keySet)
            .build();

    MockStreamObserver<PartialResultSet> responseObserver = new MockStreamObserver<>();

    ServerStreamingCallable<ReadRequest, PartialResultSet> callable =
        client.streamingReadCallable();
    callable.serverStreamingCall(request, responseObserver);

    List<PartialResultSet> actualResponses = responseObserver.future().get();
    Assert.assertEquals(1, actualResponses.size());
    Assert.assertEquals(expectedResponse, actualResponses.get(0));
  }

  @Test
  @SuppressWarnings("all")
  public void streamingReadExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);
    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String table = "table110115790";
    List<String> columns = new ArrayList<>();
    KeySet keySet = KeySet.newBuilder().build();
    ReadRequest request =
        ReadRequest.newBuilder()
            .setSession(session.toString())
            .setTable(table)
            .addAllColumns(columns)
            .setKeySet(keySet)
            .build();

    MockStreamObserver<PartialResultSet> responseObserver = new MockStreamObserver<>();

    ServerStreamingCallable<ReadRequest, PartialResultSet> callable =
        client.streamingReadCallable();
    callable.serverStreamingCall(request, responseObserver);

    try {
      List<PartialResultSet> actualResponses = responseObserver.future().get();
      Assert.fail("No exception thrown");
    } catch (ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof InvalidArgumentException);
      InvalidArgumentException apiException = (InvalidArgumentException) e.getCause();
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  @SuppressWarnings("all")
  public void beginTransactionTest() {
    ByteString id = ByteString.copyFromUtf8("27");
    Transaction expectedResponse = Transaction.newBuilder().setId(id).build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    TransactionOptions options = TransactionOptions.newBuilder().build();

    Transaction actualResponse = client.beginTransaction(session, options);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    BeginTransactionRequest actualRequest = (BeginTransactionRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(options, actualRequest.getOptions());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void beginTransactionExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      TransactionOptions options = TransactionOptions.newBuilder().build();

      client.beginTransaction(session, options);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void commitTest() {
    CommitResponse expectedResponse = CommitResponse.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    ByteString transactionId = ByteString.copyFromUtf8("28");
    List<Mutation> mutations = new ArrayList<>();

    CommitResponse actualResponse = client.commit(session, transactionId, mutations);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CommitRequest actualRequest = (CommitRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(transactionId, actualRequest.getTransactionId());
    Assert.assertEquals(mutations, actualRequest.getMutationsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void commitExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      ByteString transactionId = ByteString.copyFromUtf8("28");
      List<Mutation> mutations = new ArrayList<>();

      client.commit(session, transactionId, mutations);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void commitTest2() {
    CommitResponse expectedResponse = CommitResponse.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    TransactionOptions singleUseTransaction = TransactionOptions.newBuilder().build();
    List<Mutation> mutations = new ArrayList<>();

    CommitResponse actualResponse = client.commit(session, singleUseTransaction, mutations);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CommitRequest actualRequest = (CommitRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(singleUseTransaction, actualRequest.getSingleUseTransaction());
    Assert.assertEquals(mutations, actualRequest.getMutationsList());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void commitExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      TransactionOptions singleUseTransaction = TransactionOptions.newBuilder().build();
      List<Mutation> mutations = new ArrayList<>();

      client.commit(session, singleUseTransaction, mutations);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void rollbackTest() {
    Empty expectedResponse = Empty.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    ByteString transactionId = ByteString.copyFromUtf8("28");

    client.rollback(session, transactionId);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RollbackRequest actualRequest = (RollbackRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(transactionId, actualRequest.getTransactionId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void rollbackExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      ByteString transactionId = ByteString.copyFromUtf8("28");

      client.rollback(session, transactionId);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void partitionQueryTest() {
    PartitionResponse expectedResponse = PartitionResponse.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String sql = "sql114126";
    PartitionQueryRequest request =
        PartitionQueryRequest.newBuilder().setSession(session.toString()).setSql(sql).build();

    PartitionResponse actualResponse = client.partitionQuery(request);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    PartitionQueryRequest actualRequest = (PartitionQueryRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(sql, actualRequest.getSql());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void partitionQueryExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      String sql = "sql114126";
      PartitionQueryRequest request =
          PartitionQueryRequest.newBuilder().setSession(session.toString()).setSql(sql).build();

      client.partitionQuery(request);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }

  @Test
  @SuppressWarnings("all")
  public void partitionReadTest() {
    PartitionResponse expectedResponse = PartitionResponse.newBuilder().build();
    mockSpanner.addResponse(expectedResponse);

    SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
    String table = "table110115790";
    KeySet keySet = KeySet.newBuilder().build();
    PartitionReadRequest request =
        PartitionReadRequest.newBuilder()
            .setSession(session.toString())
            .setTable(table)
            .setKeySet(keySet)
            .build();

    PartitionResponse actualResponse = client.partitionRead(request);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockSpanner.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    PartitionReadRequest actualRequest = (PartitionReadRequest) actualRequests.get(0);

    Assert.assertEquals(session, SessionName.parse(actualRequest.getSession()));
    Assert.assertEquals(table, actualRequest.getTable());
    Assert.assertEquals(keySet, actualRequest.getKeySet());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  @SuppressWarnings("all")
  public void partitionReadExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);
    mockSpanner.addException(exception);

    try {
      SessionName session = SessionName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]", "[SESSION]");
      String table = "table110115790";
      KeySet keySet = KeySet.newBuilder().build();
      PartitionReadRequest request =
          PartitionReadRequest.newBuilder()
              .setSession(session.toString())
              .setTable(table)
              .setKeySet(keySet)
              .build();

      client.partitionRead(request);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception
    }
  }
}
