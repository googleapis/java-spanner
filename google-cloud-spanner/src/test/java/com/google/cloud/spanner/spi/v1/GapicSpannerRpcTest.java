/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.spi.v1;

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.google.api.core.ApiFunction;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.MockSpannerServiceImpl.SimulatedExecutionTime;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.SpannerOptions.CallContextConfigurator;
import com.google.cloud.spanner.SpannerOptions.CallCredentialsProvider;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.admin.database.v1.MockDatabaseAdminImpl;
import com.google.cloud.spanner.admin.instance.v1.MockInstanceAdminImpl;
import com.google.cloud.spanner.spi.v1.GapicSpannerRpc.AdminRequestsLimitExceededRetryAlgorithm;
import com.google.cloud.spanner.spi.v1.SpannerRpc.Option;
import com.google.common.base.Stopwatch;
import com.google.protobuf.ListValue;
import com.google.rpc.ErrorInfo;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DatabaseName;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.InstanceConfigName;
import com.google.spanner.admin.instance.v1.InstanceName;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.GetSessionRequest;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.SpannerGrpc;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.TypeCode;
import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.threeten.bp.Duration;

/** Tests that opening and closing multiple Spanner instances does not leak any threads. */
@RunWith(JUnit4.class)
public class GapicSpannerRpcTest {

  private static final Statement SELECT1AND2 =
      Statement.of("SELECT 1 AS COL1 UNION ALL SELECT 2 AS COL1");
  private static final ResultSetMetadata SELECT1AND2_METADATA =
      ResultSetMetadata.newBuilder()
          .setRowType(
              StructType.newBuilder()
                  .addFields(
                      Field.newBuilder()
                          .setName("COL1")
                          .setType(
                              com.google.spanner.v1.Type.newBuilder()
                                  .setCode(TypeCode.INT64)
                                  .build())
                          .build())
                  .build())
          .build();
  private static final com.google.spanner.v1.ResultSet SELECT1_RESULTSET =
      com.google.spanner.v1.ResultSet.newBuilder()
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
                  .build())
          .addRows(
              ListValue.newBuilder()
                  .addValues(com.google.protobuf.Value.newBuilder().setStringValue("2").build())
                  .build())
          .setMetadata(SELECT1AND2_METADATA)
          .build();
  private static final Statement UPDATE_FOO_STATEMENT =
      Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");

  private static final String STATIC_OAUTH_TOKEN = "STATIC_TEST_OAUTH_TOKEN";
  private static final String VARIABLE_OAUTH_TOKEN = "VARIABLE_TEST_OAUTH_TOKEN";
  private static final OAuth2Credentials STATIC_CREDENTIALS =
      OAuth2Credentials.create(
          new AccessToken(
              STATIC_OAUTH_TOKEN,
              new java.util.Date(
                  System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS))));
  private static final OAuth2Credentials VARIABLE_CREDENTIALS =
      OAuth2Credentials.create(
          new AccessToken(
              VARIABLE_OAUTH_TOKEN,
              new java.util.Date(
                  System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(1L, TimeUnit.DAYS))));

  private MockSpannerServiceImpl mockSpanner;
  private MockInstanceAdminImpl mockInstanceAdmin;
  private MockDatabaseAdminImpl mockDatabaseAdmin;
  private Server server;
  private InetSocketAddress address;
  private final Map<SpannerRpc.Option, Object> optionsMap = new HashMap<>();

  @BeforeClass
  public static void checkNotEmulator() {
    assumeTrue(
        "Skip tests when emulator is enabled as this test interferes with the check whether the emulator is running",
        System.getenv("SPANNER_EMULATOR_HOST") == null);
  }

  @Before
  public void startServer() throws IOException {
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D); // We don't want any unpredictable aborted transactions.
    mockSpanner.putStatementResult(StatementResult.query(SELECT1AND2, SELECT1_RESULTSET));
    mockSpanner.putStatementResult(StatementResult.update(UPDATE_FOO_STATEMENT, 1L));

    mockInstanceAdmin = new MockInstanceAdminImpl();
    mockDatabaseAdmin = new MockDatabaseAdminImpl();
    address = new InetSocketAddress("localhost", 0);
    server =
        NettyServerBuilder.forAddress(address)
            .addService(mockSpanner)
            .addService(mockInstanceAdmin)
            .addService(mockDatabaseAdmin)
            // Add a server interceptor that will check that we receive the variable OAuth token
            // from the CallCredentials, and not the one set as static credentials.
            .intercept(
                new ServerInterceptor() {
                  @Override
                  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                      ServerCall<ReqT, RespT> call,
                      Metadata headers,
                      ServerCallHandler<ReqT, RespT> next) {
                    String auth =
                        headers.get(Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
                    assertThat(auth).isEqualTo("Bearer " + VARIABLE_OAUTH_TOKEN);
                    return Contexts.interceptCall(Context.current(), call, headers, next);
                  }
                })
            .build()
            .start();
    optionsMap.put(Option.CHANNEL_HINT, Long.valueOf(1L));
  }

  @After
  public void stopServer() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
  }

  private static final int NUMBER_OF_TEST_RUNS = 2;
  private static final int NUM_THREADS_PER_CHANNEL = 4;
  private static final String SPANNER_THREAD_NAME = "Cloud-Spanner-TransportChannel";
  private static final String THREAD_PATTERN = "%s-[0-9]+";

  @Test
  public void testCloseAllThreadsWhenClosingSpanner() throws InterruptedException {
    for (int i = 0; i < NUMBER_OF_TEST_RUNS; i++) {
      assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME, true), is(equalTo(0)));
      // Create Spanner instance.
      SpannerOptions options = createSpannerOptions();
      Spanner spanner = options.getService();
      // Get a database client and do a query. This should initiate threads for the Spanner service.
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      List<ResultSet> resultSets = new ArrayList<>();
      // SpannerStub affiliates a channel with a session, so we need to use multiple sessions
      // to ensure we also hit multiple channels.
      for (int i2 = 0; i2 < options.getSessionPoolOptions().getMaxSessions(); i2++) {
        ResultSet rs = client.singleUse().executeQuery(SELECT1AND2);
        // Execute ResultSet#next() to send the query to Spanner.
        rs.next();
        // Delay closing the result set in order to force the use of multiple sessions.
        // As each session is linked to one transport channel, using multiple different
        // sessions should initialize multiple transport channels.
        resultSets.add(rs);
        // Check whether the number of expected threads has been reached.
        if (getNumberOfThreadsWithName(SPANNER_THREAD_NAME, false)
            == options.getNumChannels() * NUM_THREADS_PER_CHANNEL) {
          break;
        }
      }
      for (ResultSet rs : resultSets) {
        rs.close();
      }
      // Then do a request to the InstanceAdmin service and check the number of threads.
      // Doing a request should initialize a thread pool for the underlying InstanceAdminClient.
      for (int i2 = 0; i2 < options.getNumChannels() * 2; i2++) {
        mockGetInstanceResponse();
        InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
        instanceAdminClient.getInstance("projects/[PROJECT]/instances/[INSTANCE]");
      }
      // Then do a request to the DatabaseAdmin service and check the number of threads.
      // Doing a request should initialize a thread pool for the underlying DatabaseAdminClient.
      for (int i2 = 0; i2 < options.getNumChannels() * 2; i2++) {
        mockGetDatabaseResponse();
        DatabaseAdminClient databaseAdminClient = spanner.getDatabaseAdminClient();
        databaseAdminClient.getDatabase("projects/[PROJECT]/instances/[INSTANCE]", "[DATABASE]");
      }
      // Now close the Spanner instance and check whether the threads are shutdown or not.
      spanner.close();
      // Wait for up to two seconds to allow the threads to actually shutdown.
      Stopwatch watch = Stopwatch.createStarted();
      while (getNumberOfThreadsWithName(SPANNER_THREAD_NAME, false) > 0
          && watch.elapsed(TimeUnit.SECONDS) < 2) {
        Thread.sleep(10L);
      }
      assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME, true), is(equalTo(0)));
    }
  }

  /**
   * Tests that multiple open {@link Spanner} objects at the same time does not share any executors
   * or worker threads, and that all of them are shutdown when the {@link Spanner} object is closed.
   */
  @Test
  public void testMultipleOpenSpanners() throws InterruptedException {
    List<Spanner> spanners = new ArrayList<>();
    assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME, true), is(equalTo(0)));
    for (int openSpanners = 1; openSpanners <= 3; openSpanners++) {
      // Create Spanner instance.
      SpannerOptions options = createSpannerOptions();
      Spanner spanner = options.getService();
      spanners.add(spanner);
      // Get a database client and do a query. This should initiate threads for the Spanner service.
      DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      List<ResultSet> resultSets = new ArrayList<>();
      // SpannerStub affiliates a channel with a session, so we need to use multiple sessions
      // to ensure we also hit multiple channels.
      for (int sessionCount = 0;
          sessionCount < options.getSessionPoolOptions().getMaxSessions()
              && getNumberOfThreadsWithName(SPANNER_THREAD_NAME, false)
                  < options.getNumChannels() * NUM_THREADS_PER_CHANNEL * openSpanners;
          sessionCount++) {
        ResultSet rs = client.singleUse().executeQuery(SELECT1AND2);
        // Execute ResultSet#next() to send the query to Spanner.
        rs.next();
        // Delay closing the result set in order to force the use of multiple sessions.
        // As each session is linked to one transport channel, using multiple different
        // sessions should initialize multiple transport channels.
        resultSets.add(rs);
      }
      for (ResultSet rs : resultSets) {
        rs.close();
      }
    }
    for (Spanner spanner : spanners) {
      spanner.close();
    }
    // Wait a little to allow the threads to actually shutdown.
    Stopwatch watch = Stopwatch.createStarted();
    while (getNumberOfThreadsWithName(SPANNER_THREAD_NAME, false) > 0
        && watch.elapsed(TimeUnit.SECONDS) < 2) {
      Thread.sleep(10L);
    }
    assertThat(getNumberOfThreadsWithName(SPANNER_THREAD_NAME, true), is(equalTo(0)));
  }

  @Test
  public void testCallCredentialsProviderPreferenceAboveCredentials() {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(STATIC_CREDENTIALS)
            .setCallCredentialsProvider(
                new CallCredentialsProvider() {
                  @Override
                  public CallCredentials getCallCredentials() {
                    return MoreCallCredentials.from(VARIABLE_CREDENTIALS);
                  }
                })
            .build();
    GapicSpannerRpc rpc = new GapicSpannerRpc(options);
    // GoogleAuthLibraryCallCredentials doesn't implement equals, so we can only check for the
    // existence.
    assertThat(
            rpc.newCallContext(
                    optionsMap,
                    "/some/resource",
                    GetSessionRequest.getDefaultInstance(),
                    SpannerGrpc.getGetSessionMethod())
                .getCallOptions()
                .getCredentials())
        .isNotNull();
    rpc.shutdown();
  }

  @Test
  public void testCallCredentialsProviderReturnsNull() {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(STATIC_CREDENTIALS)
            .setCallCredentialsProvider(
                new CallCredentialsProvider() {
                  @Override
                  public CallCredentials getCallCredentials() {
                    return null;
                  }
                })
            .build();
    GapicSpannerRpc rpc = new GapicSpannerRpc(options);
    assertThat(
            rpc.newCallContext(
                    optionsMap,
                    "/some/resource",
                    GetSessionRequest.getDefaultInstance(),
                    SpannerGrpc.getGetSessionMethod())
                .getCallOptions()
                .getCredentials())
        .isNull();
    rpc.shutdown();
  }

  @Test
  public void testNoCallCredentials() {
    SpannerOptions options =
        SpannerOptions.newBuilder()
            .setProjectId("some-project")
            .setCredentials(STATIC_CREDENTIALS)
            .build();
    GapicSpannerRpc rpc = new GapicSpannerRpc(options);
    assertThat(
            rpc.newCallContext(
                    optionsMap,
                    "/some/resource",
                    GetSessionRequest.getDefaultInstance(),
                    SpannerGrpc.getGetSessionMethod())
                .getCallOptions()
                .getCredentials())
        .isNull();
    rpc.shutdown();
  }

  private static final class TimeoutHolder {

    private Duration timeout;
  }

  @Test
  public void testCallContextTimeout() {
    // Create a CallContextConfigurator that uses a variable timeout value.
    final TimeoutHolder timeoutHolder = new TimeoutHolder();
    CallContextConfigurator configurator =
        new CallContextConfigurator() {
          @Override
          public <ReqT, RespT> ApiCallContext configure(
              ApiCallContext context, ReqT request, MethodDescriptor<ReqT, RespT> method) {
            // Only configure a timeout for the ExecuteSql method as this method is used for
            // executing DML statements.
            if (request instanceof ExecuteSqlRequest
                && method.equals(SpannerGrpc.getExecuteSqlMethod())) {
              ExecuteSqlRequest sqlRequest = (ExecuteSqlRequest) request;
              // Sequence numbers are only assigned for DML statements, which means that
              // this is an update statement.
              if (sqlRequest.getSeqno() > 0L) {
                return context.withTimeout(timeoutHolder.timeout);
              }
            }
            return null;
          }
        };

    mockSpanner.setExecuteSqlExecutionTime(SimulatedExecutionTime.ofMinimumAndRandomTime(10, 0));
    SpannerOptions options = createSpannerOptions();
    try (Spanner spanner = options.getService()) {
      final DatabaseClient client =
          spanner.getDatabaseClient(DatabaseId.of("[PROJECT]", "[INSTANCE]", "[DATABASE]"));
      Context context =
          Context.current().withValue(SpannerOptions.CALL_CONTEXT_CONFIGURATOR_KEY, configurator);
      context.run(
          new Runnable() {
            @Override
            public void run() {
              try {
                // First try with a 1ns timeout. This should always cause a DEADLINE_EXCEEDED
                // exception.
                timeoutHolder.timeout = Duration.ofNanos(1L);
                client
                    .readWriteTransaction()
                    .run(
                        new TransactionCallable<Long>() {
                          @Override
                          public Long run(TransactionContext transaction) throws Exception {
                            return transaction.executeUpdate(UPDATE_FOO_STATEMENT);
                          }
                        });
                fail("missing expected timeout exception");
              } catch (SpannerException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEADLINE_EXCEEDED);
              }

              // Then try with a longer timeout. This should now succeed.
              timeoutHolder.timeout = Duration.ofMinutes(1L);
              Long updateCount =
                  client
                      .readWriteTransaction()
                      .run(
                          new TransactionCallable<Long>() {
                            @Override
                            public Long run(TransactionContext transaction) throws Exception {
                              return transaction.executeUpdate(UPDATE_FOO_STATEMENT);
                            }
                          });
              assertThat(updateCount).isEqualTo(1L);
            }
          });
    }
  }

  @Test
  public void testNewCallContextWithNullRequestAndNullMethod() {
    SpannerOptions options = SpannerOptions.newBuilder().setProjectId("some-project").build();
    GapicSpannerRpc rpc = new GapicSpannerRpc(options);
    assertThat(rpc.newCallContext(optionsMap, "/some/resource", null, null)).isNotNull();
    rpc.shutdown();
  }

  @Test
  public void testAdminRequestsLimitExceededRetryAlgorithm() {
    AdminRequestsLimitExceededRetryAlgorithm<Long> alg =
        new AdminRequestsLimitExceededRetryAlgorithm<>();

    assertThat(alg.shouldRetry(null, 1L)).isFalse();

    ErrorInfo info =
        ErrorInfo.newBuilder()
            .putMetadata("quota_limit", "AdminMethodQuotaPerMinutePerProject")
            .build();
    Metadata.Key<ErrorInfo> key =
        Metadata.Key.of(
            info.getDescriptorForType().getFullName() + Metadata.BINARY_HEADER_SUFFIX,
            ProtoLiteUtils.metadataMarshaller(info));
    Metadata trailers = new Metadata();
    trailers.put(key, info);

    SpannerException adminRateExceeded =
        SpannerExceptionFactory.newSpannerException(
            Status.RESOURCE_EXHAUSTED.withDescription("foo").asRuntimeException(trailers));
    assertThat(alg.shouldRetry(adminRateExceeded, null)).isTrue();

    SpannerException numDatabasesExceeded =
        SpannerExceptionFactory.newSpannerException(
            Status.RESOURCE_EXHAUSTED
                .withDescription("Too many databases on instance")
                .asRuntimeException());
    assertThat(alg.shouldRetry(numDatabasesExceeded, null)).isFalse();

    assertThat(alg.shouldRetry(new Exception("random exception"), null)).isFalse();
  }

  @SuppressWarnings("rawtypes")
  private SpannerOptions createSpannerOptions() {
    String endpoint = address.getHostString() + ":" + server.getPort();
    return SpannerOptions.newBuilder()
        .setProjectId("[PROJECT]")
        // Set a custom channel configurator to allow http instead of https.
        .setChannelConfigurator(
            new ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder>() {
              @Override
              public ManagedChannelBuilder apply(ManagedChannelBuilder input) {
                input.usePlaintext();
                return input;
              }
            })
        .setHost("http://" + endpoint)
        // Set static credentials that will return the static OAuth test token.
        .setCredentials(STATIC_CREDENTIALS)
        // Also set a CallCredentialsProvider. These credentials should take precedence above
        // the static credentials.
        .setCallCredentialsProvider(
            new CallCredentialsProvider() {
              @Override
              public CallCredentials getCallCredentials() {
                return MoreCallCredentials.from(VARIABLE_CREDENTIALS);
              }
            })
        .build();
  }

  private int getNumberOfThreadsWithName(String serviceName, boolean dumpStack) {
    Pattern pattern = Pattern.compile(String.format(THREAD_PATTERN, serviceName));
    ThreadGroup group = Thread.currentThread().getThreadGroup();
    while (group.getParent() != null) {
      group = group.getParent();
    }
    Thread[] threads = new Thread[100 * NUMBER_OF_TEST_RUNS];
    int numberOfThreads = group.enumerate(threads);
    int res = 0;
    for (int i = 0; i < numberOfThreads; i++) {
      if (pattern.matcher(threads[i].getName()).matches()) {
        if (dumpStack) {
          dumpThread(threads[i]);
        }
        res++;
      }
    }
    return res;
  }

  private void dumpThread(Thread thread) {
    StringBuilder dump = new StringBuilder();
    dump.append('"');
    dump.append(thread.getName());
    dump.append("\" ");
    final Thread.State state = thread.getState();
    dump.append("\n   java.lang.Thread.State: ");
    dump.append(state);
    final StackTraceElement[] stackTraceElements = thread.getStackTrace();
    for (final StackTraceElement stackTraceElement : stackTraceElements) {
      dump.append("\n        at ");
      dump.append(stackTraceElement);
    }
    dump.append("\n\n");
    System.out.print(dump.toString());
  }

  private void mockGetInstanceResponse() {
    InstanceName name2 = InstanceName.of("[PROJECT]", "[INSTANCE]");
    InstanceConfigName config = InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]");
    String displayName = "displayName1615086568";
    int nodeCount = 1539922066;
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(name2.toString())
            .setConfig(config.toString())
            .setDisplayName(displayName)
            .setNodeCount(nodeCount)
            .build();
    mockInstanceAdmin.addResponse(expectedResponse);
  }

  private void mockGetDatabaseResponse() {
    DatabaseName name2 = DatabaseName.of("[PROJECT]", "[INSTANCE]", "[DATABASE]");
    Database expectedResponse = Database.newBuilder().setName(name2.toString()).build();
    mockDatabaseAdmin.addResponse(expectedResponse);
  }
}
