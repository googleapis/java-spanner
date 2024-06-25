/*
 * Copyright 2017 Google LLC
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

import com.google.api.core.ApiFunction;
import com.google.api.core.BetaApi;
import com.google.api.core.InternalApi;
import com.google.api.core.ObsoleteApi;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.GaxProperties;
import com.google.api.gax.grpc.GrpcCallContext;
import com.google.api.gax.grpc.GrpcInterceptorProvider;
import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.api.gax.tracing.ApiTracerFactory;
import com.google.api.gax.tracing.BaseApiTracerFactory;
import com.google.api.gax.tracing.OpencensusTracerFactory;
import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceDefaults;
import com.google.cloud.ServiceOptions;
import com.google.cloud.ServiceRpc;
import com.google.cloud.TransportOptions;
import com.google.cloud.grpc.GcpManagedChannelOptions;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.spanner.Options.DirectedReadOption;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminSettings;
import com.google.cloud.spanner.admin.database.v1.stub.DatabaseAdminStubSettings;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminSettings;
import com.google.cloud.spanner.admin.instance.v1.stub.InstanceAdminStubSettings;
import com.google.cloud.spanner.spi.SpannerRpcFactory;
import com.google.cloud.spanner.spi.v1.GapicSpannerRpc;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.v1.SpannerSettings;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.SpannerGrpc;
import io.grpc.CallCredentials;
import io.grpc.CompressorRegistry;
import io.grpc.Context;
import io.grpc.ExperimentalApi;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.threeten.bp.Duration;

/** Options for the Cloud Spanner service. */
public class SpannerOptions extends ServiceOptions<Spanner, SpannerOptions> {
  private static final long serialVersionUID = 2789571558532701170L;
  private static SpannerEnvironment environment = SpannerEnvironmentImpl.INSTANCE;
  private static boolean enableOpenCensusMetrics = true;
  private static boolean enableOpenTelemetryMetrics = false;

  private static final String JDBC_API_CLIENT_LIB_TOKEN = "sp-jdbc";
  private static final String HIBERNATE_API_CLIENT_LIB_TOKEN = "sp-hib";
  private static final String LIQUIBASE_API_CLIENT_LIB_TOKEN = "sp-liq";
  private static final String PG_ADAPTER_CLIENT_LIB_TOKEN = "pg-adapter";

  private static final String API_SHORT_NAME = "Spanner";
  private static final String DEFAULT_HOST = "https://spanner.googleapis.com";
  private static final ImmutableSet<String> SCOPES =
      ImmutableSet.of(
          "https://www.googleapis.com/auth/spanner.admin",
          "https://www.googleapis.com/auth/spanner.data");
  static final int MAX_CHANNELS = 256;
  @VisibleForTesting static final int DEFAULT_CHANNELS = 4;
  // Set the default number of channels to GRPC_GCP_ENABLED_DEFAULT_CHANNELS when gRPC-GCP extension
  // is enabled, to make sure there are sufficient channels available to move the sessions to a
  // different channel if a network connection in a particular channel fails.
  @VisibleForTesting static final int GRPC_GCP_ENABLED_DEFAULT_CHANNELS = 8;
  private final TransportChannelProvider channelProvider;

  @SuppressWarnings("rawtypes")
  private final ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> channelConfigurator;

  private final GrpcInterceptorProvider interceptorProvider;
  private final SessionPoolOptions sessionPoolOptions;
  private final int prefetchChunks;
  private final DecodeMode decodeMode;
  private final int numChannels;
  private final String transportChannelExecutorThreadNameFormat;
  private final String databaseRole;
  private final ImmutableMap<String, String> sessionLabels;
  private final SpannerStubSettings spannerStubSettings;
  private final InstanceAdminStubSettings instanceAdminStubSettings;
  private final DatabaseAdminStubSettings databaseAdminStubSettings;
  private final Duration partitionedDmlTimeout;
  private final boolean grpcGcpExtensionEnabled;
  private final GcpManagedChannelOptions grpcGcpOptions;
  private final boolean autoThrottleAdministrativeRequests;
  private final RetrySettings retryAdministrativeRequestsSettings;
  private final boolean trackTransactionStarter;
  /**
   * These are the default {@link QueryOptions} defined by the user on this {@link SpannerOptions}.
   */
  private final Map<DatabaseId, QueryOptions> defaultQueryOptions;
  /** These are the default {@link QueryOptions} defined in environment variables on this system. */
  private final QueryOptions envQueryOptions;
  /**
   * These are the merged query options of the {@link QueryOptions} set on this {@link
   * SpannerOptions} and the {@link QueryOptions} in the environment variables. Options specified in
   * environment variables take precedence above options specified in the {@link SpannerOptions}
   * instance.
   */
  private final Map<DatabaseId, QueryOptions> mergedQueryOptions;

  private final CallCredentialsProvider callCredentialsProvider;
  private final CloseableExecutorProvider asyncExecutorProvider;
  private final String compressorName;
  private final boolean leaderAwareRoutingEnabled;
  private final boolean attemptDirectPath;
  private final DirectedReadOptions directedReadOptions;
  private final boolean useVirtualThreads;
  private final OpenTelemetry openTelemetry;
  private final boolean enableApiTracing;
  private final boolean enableExtendedTracing;
  private final boolean enableEndToEndTracing;

  enum TracingFramework {
    OPEN_CENSUS,
    OPEN_TELEMETRY
  }

  private static final Object lock = new Object();

  @GuardedBy("lock")
  private static TracingFramework activeTracingFramework;

  /** Interface that can be used to provide {@link CallCredentials} to {@link SpannerOptions}. */
  public interface CallCredentialsProvider {
    /** Return the {@link CallCredentials} to use for a gRPC call. */
    CallCredentials getCallCredentials();
  }

  /** Context key for the {@link CallContextConfigurator} to use. */
  public static final Context.Key<CallContextConfigurator> CALL_CONTEXT_CONFIGURATOR_KEY =
      Context.key("call-context-configurator");

  /**
   * {@link CallContextConfigurator} can be used to modify the {@link ApiCallContext} for one or
   * more specific RPCs. This can be used to set specific timeout value for RPCs or use specific
   * {@link CallCredentials} for an RPC. The {@link CallContextConfigurator} must be set as a value
   * on the {@link Context} using the {@link SpannerOptions#CALL_CONTEXT_CONFIGURATOR_KEY} key.
   *
   * <p>This API is meant for advanced users. Most users should instead use the {@link
   * SpannerCallContextTimeoutConfigurator} for setting timeouts per RPC.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * CallContextConfigurator configurator =
   *     new CallContextConfigurator() {
   *       public <ReqT, RespT> ApiCallContext configure(
   *           ApiCallContext context, ReqT request, MethodDescriptor<ReqT, RespT> method) {
   *         if (method == SpannerGrpc.getExecuteBatchDmlMethod()) {
   *           return GrpcCallContext.createDefault()
   *               .withCallOptions(CallOptions.DEFAULT.withDeadlineAfter(60L, TimeUnit.SECONDS));
   *         }
   *         return null;
   *       }
   *     };
   * Context context =
   *     Context.current().withValue(SpannerOptions.CALL_CONTEXT_CONFIGURATOR_KEY, configurator);
   * context.run(
   *     () -> {
   *       try {
   *         client
   *             .readWriteTransaction()
   *             .run(
   *                 new TransactionCallable<long[]>() {
   *                   public long[] run(TransactionContext transaction) throws Exception {
   *                     return transaction.batchUpdate(
   *                         ImmutableList.of(statement1, statement2));
   *                   }
   *                 });
   *       } catch (SpannerException e) {
   *         if (e.getErrorCode() == ErrorCode.DEADLINE_EXCEEDED) {
   *           // handle timeout exception.
   *         }
   *       }
   *     }
   * }</pre>
   */
  public interface CallContextConfigurator {
    /**
     * Configure a {@link ApiCallContext} for a specific RPC call.
     *
     * @param context The default context. This can be used to inspect the current values.
     * @param request The request that will be sent.
     * @param method The method that is being called.
     * @return An {@link ApiCallContext} that will be merged with the default {@link
     *     ApiCallContext}. If <code>null</code> is returned, no changes to the default {@link
     *     ApiCallContext} will be made.
     */
    @Nullable
    <ReqT, RespT> ApiCallContext configure(
        ApiCallContext context, ReqT request, MethodDescriptor<ReqT, RespT> method);
  }

  private enum SpannerMethod {
    COMMIT {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        return method == SpannerGrpc.getCommitMethod();
      }
    },
    ROLLBACK {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        return method == SpannerGrpc.getRollbackMethod();
      }
    },

    EXECUTE_QUERY {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        // This also matches with Partitioned DML calls, but that call will override any timeout
        // settings anyway.
        return method == SpannerGrpc.getExecuteStreamingSqlMethod();
      }
    },
    READ {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        return method == SpannerGrpc.getStreamingReadMethod();
      }
    },
    EXECUTE_UPDATE {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        if (method == SpannerGrpc.getExecuteSqlMethod()) {
          ExecuteSqlRequest sqlRequest = (ExecuteSqlRequest) request;
          return sqlRequest.getSeqno() != 0L;
        }
        return false;
      }
    },
    BATCH_UPDATE {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        return method == SpannerGrpc.getExecuteBatchDmlMethod();
      }
    },

    PARTITION_QUERY {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        return method == SpannerGrpc.getPartitionQueryMethod();
      }
    },
    PARTITION_READ {
      @Override
      <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method) {
        return method == SpannerGrpc.getPartitionReadMethod();
      }
    };

    abstract <ReqT, RespT> boolean isMethod(ReqT request, MethodDescriptor<ReqT, RespT> method);

    static <ReqT, RespT> SpannerMethod valueOf(ReqT request, MethodDescriptor<ReqT, RespT> method) {
      for (SpannerMethod m : SpannerMethod.values()) {
        if (m.isMethod(request, method)) {
          return m;
        }
      }
      return null;
    }
  }

  /**
   * Helper class to configure timeouts for specific Spanner RPCs. The {@link
   * SpannerCallContextTimeoutConfigurator} must be set as a value on the {@link Context} using the
   * {@link SpannerOptions#CALL_CONTEXT_CONFIGURATOR_KEY} key.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * // Create a context with a ExecuteQuery timeout of 10 seconds.
   * Context context =
   *     Context.current()
   *         .withValue(
   *             SpannerOptions.CALL_CONTEXT_CONFIGURATOR_KEY,
   *             SpannerCallContextTimeoutConfigurator.create()
   *                 .withExecuteQueryTimeout(Duration.ofSeconds(10L)));
   * context.run(
   *     () -> {
   *       try (ResultSet rs =
   *           client
   *               .singleUse()
   *               .executeQuery(
   *                   Statement.of(
   *                       "SELECT SingerId, FirstName, LastName FROM Singers ORDER BY LastName"))) {
   *         while (rs.next()) {
   *           System.out.printf("%d %s %s%n", rs.getLong(0), rs.getString(1), rs.getString(2));
   *         }
   *       } catch (SpannerException e) {
   *         if (e.getErrorCode() == ErrorCode.DEADLINE_EXCEEDED) {
   *           // Handle timeout.
   *         }
   *       }
   *     }
   * }</pre>
   */
  public static class SpannerCallContextTimeoutConfigurator implements CallContextConfigurator {
    private Duration commitTimeout;
    private Duration rollbackTimeout;

    private Duration executeQueryTimeout;
    private Duration executeUpdateTimeout;
    private Duration batchUpdateTimeout;
    private Duration readTimeout;

    private Duration partitionQueryTimeout;
    private Duration partitionReadTimeout;

    public static SpannerCallContextTimeoutConfigurator create() {
      return new SpannerCallContextTimeoutConfigurator();
    }

    private SpannerCallContextTimeoutConfigurator() {}

    @Override
    public <ReqT, RespT> ApiCallContext configure(
        ApiCallContext context, ReqT request, MethodDescriptor<ReqT, RespT> method) {
      SpannerMethod spannerMethod = SpannerMethod.valueOf(request, method);
      if (spannerMethod == null) {
        return null;
      }
      switch (SpannerMethod.valueOf(request, method)) {
        case BATCH_UPDATE:
          return batchUpdateTimeout == null
              ? null
              : GrpcCallContext.createDefault().withTimeout(batchUpdateTimeout);
        case COMMIT:
          return commitTimeout == null
              ? null
              : GrpcCallContext.createDefault().withTimeout(commitTimeout);
        case EXECUTE_QUERY:
          return executeQueryTimeout == null
              ? null
              : GrpcCallContext.createDefault()
                  .withTimeout(executeQueryTimeout)
                  .withStreamWaitTimeout(executeQueryTimeout);
        case EXECUTE_UPDATE:
          return executeUpdateTimeout == null
              ? null
              : GrpcCallContext.createDefault().withTimeout(executeUpdateTimeout);
        case PARTITION_QUERY:
          return partitionQueryTimeout == null
              ? null
              : GrpcCallContext.createDefault().withTimeout(partitionQueryTimeout);
        case PARTITION_READ:
          return partitionReadTimeout == null
              ? null
              : GrpcCallContext.createDefault().withTimeout(partitionReadTimeout);
        case READ:
          return readTimeout == null
              ? null
              : GrpcCallContext.createDefault()
                  .withTimeout(readTimeout)
                  .withStreamWaitTimeout(readTimeout);
        case ROLLBACK:
          return rollbackTimeout == null
              ? null
              : GrpcCallContext.createDefault().withTimeout(rollbackTimeout);
        default:
      }
      return null;
    }

    public Duration getCommitTimeout() {
      return commitTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withCommitTimeout(Duration commitTimeout) {
      this.commitTimeout = commitTimeout;
      return this;
    }

    public Duration getRollbackTimeout() {
      return rollbackTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withRollbackTimeout(Duration rollbackTimeout) {
      this.rollbackTimeout = rollbackTimeout;
      return this;
    }

    public Duration getExecuteQueryTimeout() {
      return executeQueryTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withExecuteQueryTimeout(
        Duration executeQueryTimeout) {
      this.executeQueryTimeout = executeQueryTimeout;
      return this;
    }

    public Duration getExecuteUpdateTimeout() {
      return executeUpdateTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withExecuteUpdateTimeout(
        Duration executeUpdateTimeout) {
      this.executeUpdateTimeout = executeUpdateTimeout;
      return this;
    }

    public Duration getBatchUpdateTimeout() {
      return batchUpdateTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withBatchUpdateTimeout(
        Duration batchUpdateTimeout) {
      this.batchUpdateTimeout = batchUpdateTimeout;
      return this;
    }

    public Duration getReadTimeout() {
      return readTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withReadTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    public Duration getPartitionQueryTimeout() {
      return partitionQueryTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withPartitionQueryTimeout(
        Duration partitionQueryTimeout) {
      this.partitionQueryTimeout = partitionQueryTimeout;
      return this;
    }

    public Duration getPartitionReadTimeout() {
      return partitionReadTimeout;
    }

    public SpannerCallContextTimeoutConfigurator withPartitionReadTimeout(
        Duration partitionReadTimeout) {
      this.partitionReadTimeout = partitionReadTimeout;
      return this;
    }
  }

  /** Default implementation of {@code SpannerFactory}. */
  private static class DefaultSpannerFactory implements SpannerFactory {
    private static final DefaultSpannerFactory INSTANCE = new DefaultSpannerFactory();

    @Override
    public Spanner create(SpannerOptions serviceOptions) {
      return new SpannerImpl(serviceOptions);
    }
  }

  /** Default implementation of {@code SpannerRpcFactory}. */
  private static class DefaultSpannerRpcFactory implements SpannerRpcFactory {
    private static final DefaultSpannerRpcFactory INSTANCE = new DefaultSpannerRpcFactory();

    @Override
    public ServiceRpc create(SpannerOptions options) {
      return new GapicSpannerRpc(options);
    }
  }

  private static final AtomicInteger DEFAULT_POOL_COUNT = new AtomicInteger();

  /** {@link ExecutorProvider} that is used for {@link AsyncResultSet}. */
  public interface CloseableExecutorProvider extends ExecutorProvider, AutoCloseable {
    /** Overridden to suppress the throws declaration of the super interface. */
    @Override
    void close();
  }

  /**
   * Implementation of {@link CloseableExecutorProvider} that uses a fixed single {@link
   * ScheduledExecutorService}.
   */
  public static class FixedCloseableExecutorProvider implements CloseableExecutorProvider {
    private final ScheduledExecutorService executor;

    private FixedCloseableExecutorProvider(ScheduledExecutorService executor) {
      this.executor = Preconditions.checkNotNull(executor);
    }

    @Override
    public void close() {
      executor.shutdown();
    }

    @Override
    public ScheduledExecutorService getExecutor() {
      return executor;
    }

    @Override
    public boolean shouldAutoClose() {
      return false;
    }

    /** Creates a FixedCloseableExecutorProvider. */
    public static FixedCloseableExecutorProvider create(ScheduledExecutorService executor) {
      return new FixedCloseableExecutorProvider(executor);
    }
  }

  /**
   * Default {@link ExecutorProvider} for high-level async calls that need an executor. The default
   * uses a cached thread pool containing a max of 8 threads. The pool is lazily initialized and
   * will not create any threads if the user application does not use any async methods. It will
   * also scale down the thread usage if the async load allows for that.
   */
  @VisibleForTesting
  static CloseableExecutorProvider createDefaultAsyncExecutorProvider() {
    return createAsyncExecutorProvider(
        getDefaultAsyncExecutorProviderCoreThreadCount(), 60L, TimeUnit.SECONDS);
  }

  @VisibleForTesting
  static int getDefaultAsyncExecutorProviderCoreThreadCount() {
    String propertyName = "com.google.cloud.spanner.async_num_core_threads";
    String propertyValue = System.getProperty(propertyName, "8");
    try {
      int corePoolSize = Integer.parseInt(propertyValue);
      if (corePoolSize < 0) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            String.format(
                "The value for %s must be >=0. Invalid value: %s", propertyName, propertyValue));
      }
      return corePoolSize;
    } catch (NumberFormatException exception) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          String.format(
              "The %s system property must be a valid integer. The value %s could not be parsed as an integer.",
              propertyName, propertyValue));
    }
  }

  /**
   * Creates a {@link CloseableExecutorProvider} that can be used as an {@link ExecutorProvider} for
   * the async API. The {@link ExecutorProvider} will lazily create up to poolSize threads. The
   * backing threads will automatically be shutdown if they have not been used during the keep-alive
   * time. The backing threads are created as daemon threads.
   *
   * @param poolSize the maximum number of threads to create in the pool
   * @param keepAliveTime the time that an unused thread in the pool should be kept alive
   * @param unit the time unit used for the keepAliveTime
   * @return a {@link CloseableExecutorProvider} that can be used for {@link
   *     SpannerOptions.Builder#setAsyncExecutorProvider(CloseableExecutorProvider)}
   */
  public static CloseableExecutorProvider createAsyncExecutorProvider(
      int poolSize, long keepAliveTime, TimeUnit unit) {
    String format =
        String.format("spanner-async-pool-%d-thread-%%d", DEFAULT_POOL_COUNT.incrementAndGet());
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat(format).build();
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(poolSize, threadFactory);
    executor.setKeepAliveTime(keepAliveTime, unit);
    executor.allowCoreThreadTimeOut(true);
    return FixedCloseableExecutorProvider.create(executor);
  }

  protected SpannerOptions(Builder builder) {
    super(SpannerFactory.class, SpannerRpcFactory.class, builder, new SpannerDefaults());
    numChannels = builder.numChannels == null ? DEFAULT_CHANNELS : builder.numChannels;
    Preconditions.checkArgument(
        numChannels >= 1 && numChannels <= MAX_CHANNELS,
        "Number of channels must fall in the range [1, %s], found: %s",
        MAX_CHANNELS,
        numChannels);

    transportChannelExecutorThreadNameFormat = builder.transportChannelExecutorThreadNameFormat;
    channelProvider = builder.channelProvider;
    channelConfigurator = builder.channelConfigurator;
    interceptorProvider = builder.interceptorProvider;
    sessionPoolOptions =
        builder.sessionPoolOptions != null
            ? builder.sessionPoolOptions
            : SessionPoolOptions.newBuilder().build();
    prefetchChunks = builder.prefetchChunks;
    decodeMode = builder.decodeMode;
    databaseRole = builder.databaseRole;
    sessionLabels = builder.sessionLabels;
    try {
      spannerStubSettings = builder.spannerStubSettingsBuilder.build();
      instanceAdminStubSettings = builder.instanceAdminStubSettingsBuilder.build();
      databaseAdminStubSettings = builder.databaseAdminStubSettingsBuilder.build();
    } catch (IOException e) {
      throw SpannerExceptionFactory.newSpannerException(e);
    }
    partitionedDmlTimeout = builder.partitionedDmlTimeout;
    grpcGcpExtensionEnabled = builder.grpcGcpExtensionEnabled;
    grpcGcpOptions = builder.grpcGcpOptions;
    autoThrottleAdministrativeRequests = builder.autoThrottleAdministrativeRequests;
    retryAdministrativeRequestsSettings = builder.retryAdministrativeRequestsSettings;
    trackTransactionStarter = builder.trackTransactionStarter;
    defaultQueryOptions = builder.defaultQueryOptions;
    envQueryOptions = builder.getEnvironmentQueryOptions();
    if (envQueryOptions.equals(QueryOptions.getDefaultInstance())) {
      this.mergedQueryOptions = ImmutableMap.copyOf(builder.defaultQueryOptions);
    } else {
      // Merge all specific database options with the environment options.
      Map<DatabaseId, QueryOptions> merged = new HashMap<>(builder.defaultQueryOptions);
      for (Entry<DatabaseId, QueryOptions> entry : builder.defaultQueryOptions.entrySet()) {
        merged.put(entry.getKey(), entry.getValue().toBuilder().mergeFrom(envQueryOptions).build());
      }
      this.mergedQueryOptions = ImmutableMap.copyOf(merged);
    }
    callCredentialsProvider = builder.callCredentialsProvider;
    asyncExecutorProvider = builder.asyncExecutorProvider;
    compressorName = builder.compressorName;
    leaderAwareRoutingEnabled = builder.leaderAwareRoutingEnabled;
    attemptDirectPath = builder.attemptDirectPath;
    directedReadOptions = builder.directedReadOptions;
    useVirtualThreads = builder.useVirtualThreads;
    openTelemetry = builder.openTelemetry;
    enableApiTracing = builder.enableApiTracing;
    enableExtendedTracing = builder.enableExtendedTracing;
    enableEndToEndTracing = builder.enableEndToEndTracing;
  }

  /**
   * The environment to read configuration values from. The default implementation uses environment
   * variables.
   */
  public interface SpannerEnvironment {
    /**
     * The optimizer version to use. Must return an empty string to indicate that no value has been
     * set.
     */
    @Nonnull
    default String getOptimizerVersion() {
      return "";
    }

    /**
     * The optimizer statistics package to use. Must return an empty string to indicate that no
     * value has been set.
     */
    @Nonnull
    default String getOptimizerStatisticsPackage() {
      return "";
    }

    default boolean isEnableExtendedTracing() {
      return false;
    }

    default boolean isEnableApiTracing() {
      return false;
    }
  }

  /**
   * Default implementation of {@link SpannerEnvironment}. Reads all configuration from environment
   * variables.
   */
  private static class SpannerEnvironmentImpl implements SpannerEnvironment {
    private static final SpannerEnvironmentImpl INSTANCE = new SpannerEnvironmentImpl();
    private static final String SPANNER_OPTIMIZER_VERSION_ENV_VAR = "SPANNER_OPTIMIZER_VERSION";
    private static final String SPANNER_OPTIMIZER_STATISTICS_PACKAGE_ENV_VAR =
        "SPANNER_OPTIMIZER_STATISTICS_PACKAGE";
    private static final String SPANNER_ENABLE_EXTENDED_TRACING = "SPANNER_ENABLE_EXTENDED_TRACING";
    private static final String SPANNER_ENABLE_API_TRACING = "SPANNER_ENABLE_API_TRACING";

    private SpannerEnvironmentImpl() {}

    @Nonnull
    @Override
    public String getOptimizerVersion() {
      return MoreObjects.firstNonNull(System.getenv(SPANNER_OPTIMIZER_VERSION_ENV_VAR), "");
    }

    @Nonnull
    @Override
    public String getOptimizerStatisticsPackage() {
      return MoreObjects.firstNonNull(
          System.getenv(SPANNER_OPTIMIZER_STATISTICS_PACKAGE_ENV_VAR), "");
    }

    @Override
    public boolean isEnableExtendedTracing() {
      return Boolean.parseBoolean(System.getenv(SPANNER_ENABLE_EXTENDED_TRACING));
    }

    @Override
    public boolean isEnableApiTracing() {
      return Boolean.parseBoolean(System.getenv(SPANNER_ENABLE_API_TRACING));
    }
  }

  /** Builder for {@link SpannerOptions} instances. */
  public static class Builder
      extends ServiceOptions.Builder<Spanner, SpannerOptions, SpannerOptions.Builder> {
    static final int DEFAULT_PREFETCH_CHUNKS = 4;
    static final QueryOptions DEFAULT_QUERY_OPTIONS = QueryOptions.getDefaultInstance();
    // TODO: Set the default to DecodeMode.DIRECT before merging to keep the current default.
    //       It is currently set to LAZY_PER_COL so it is used in all tests.
    static final DecodeMode DEFAULT_DECODE_MODE = DecodeMode.LAZY_PER_COL;
    static final RetrySettings DEFAULT_ADMIN_REQUESTS_LIMIT_EXCEEDED_RETRY_SETTINGS =
        RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofSeconds(5L))
            .setRetryDelayMultiplier(2.0)
            .setMaxRetryDelay(Duration.ofSeconds(60L))
            .setMaxAttempts(10)
            .build();
    private final ImmutableSet<String> allowedClientLibTokens =
        ImmutableSet.of(
            ServiceOptions.getGoogApiClientLibName(),
            createCustomClientLibToken(JDBC_API_CLIENT_LIB_TOKEN),
            createCustomClientLibToken(HIBERNATE_API_CLIENT_LIB_TOKEN),
            createCustomClientLibToken(LIQUIBASE_API_CLIENT_LIB_TOKEN),
            createCustomClientLibToken(PG_ADAPTER_CLIENT_LIB_TOKEN));
    private TransportChannelProvider channelProvider;

    @SuppressWarnings("rawtypes")
    private ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> channelConfigurator;

    private GrpcInterceptorProvider interceptorProvider;

    private Integer numChannels;

    private String transportChannelExecutorThreadNameFormat = "Cloud-Spanner-TransportChannel-%d";

    private int prefetchChunks = DEFAULT_PREFETCH_CHUNKS;
    private DecodeMode decodeMode = DEFAULT_DECODE_MODE;
    private SessionPoolOptions sessionPoolOptions;
    private String databaseRole;
    private ImmutableMap<String, String> sessionLabels;
    private SpannerStubSettings.Builder spannerStubSettingsBuilder =
        SpannerStubSettings.newBuilder();
    private InstanceAdminStubSettings.Builder instanceAdminStubSettingsBuilder =
        InstanceAdminStubSettings.newBuilder();
    private DatabaseAdminStubSettings.Builder databaseAdminStubSettingsBuilder =
        DatabaseAdminStubSettings.newBuilder();
    private Duration partitionedDmlTimeout = Duration.ofHours(2L);
    private boolean grpcGcpExtensionEnabled = false;
    private GcpManagedChannelOptions grpcGcpOptions;
    private RetrySettings retryAdministrativeRequestsSettings =
        DEFAULT_ADMIN_REQUESTS_LIMIT_EXCEEDED_RETRY_SETTINGS;
    private boolean autoThrottleAdministrativeRequests = false;
    private boolean trackTransactionStarter = false;
    private Map<DatabaseId, QueryOptions> defaultQueryOptions = new HashMap<>();
    private CallCredentialsProvider callCredentialsProvider;
    private CloseableExecutorProvider asyncExecutorProvider;
    private String compressorName;
    private String emulatorHost = System.getenv("SPANNER_EMULATOR_HOST");
    private boolean leaderAwareRoutingEnabled = true;
    private boolean attemptDirectPath = true;
    private DirectedReadOptions directedReadOptions;
    private boolean useVirtualThreads = false;
    private OpenTelemetry openTelemetry;
    private boolean enableApiTracing = SpannerOptions.environment.isEnableApiTracing();
    private boolean enableExtendedTracing = SpannerOptions.environment.isEnableExtendedTracing();
    private boolean enableEndToEndTracing = false;

    private static String createCustomClientLibToken(String token) {
      return token + " " + ServiceOptions.getGoogApiClientLibName();
    }

    protected Builder() {
      // Manually set retry and polling settings that work.
      OperationTimedPollAlgorithm longRunningPollingAlgorithm =
          OperationTimedPollAlgorithm.create(
              RetrySettings.newBuilder()
                  .setInitialRpcTimeout(Duration.ofSeconds(60L))
                  .setMaxRpcTimeout(Duration.ofSeconds(600L))
                  .setInitialRetryDelay(Duration.ofSeconds(20L))
                  .setMaxRetryDelay(Duration.ofSeconds(45L))
                  .setRetryDelayMultiplier(1.5)
                  .setRpcTimeoutMultiplier(1.5)
                  .setTotalTimeout(Duration.ofHours(48L))
                  .build());
      databaseAdminStubSettingsBuilder
          .createDatabaseOperationSettings()
          .setPollingAlgorithm(longRunningPollingAlgorithm);
      databaseAdminStubSettingsBuilder
          .createBackupOperationSettings()
          .setPollingAlgorithm(longRunningPollingAlgorithm);
      databaseAdminStubSettingsBuilder
          .restoreDatabaseOperationSettings()
          .setPollingAlgorithm(longRunningPollingAlgorithm);
    }

    Builder(SpannerOptions options) {
      super(options);
      if (options.getHost() != null
          && this.emulatorHost != null
          && !options.getHost().equals(this.emulatorHost)) {
        this.emulatorHost = null;
      }
      this.numChannels = options.numChannels;
      this.transportChannelExecutorThreadNameFormat =
          options.transportChannelExecutorThreadNameFormat;
      this.sessionPoolOptions = options.sessionPoolOptions;
      this.prefetchChunks = options.prefetchChunks;
      this.decodeMode = options.decodeMode;
      this.databaseRole = options.databaseRole;
      this.sessionLabels = options.sessionLabels;
      this.spannerStubSettingsBuilder = options.spannerStubSettings.toBuilder();
      this.instanceAdminStubSettingsBuilder = options.instanceAdminStubSettings.toBuilder();
      this.databaseAdminStubSettingsBuilder = options.databaseAdminStubSettings.toBuilder();
      this.partitionedDmlTimeout = options.partitionedDmlTimeout;
      this.grpcGcpExtensionEnabled = options.grpcGcpExtensionEnabled;
      this.grpcGcpOptions = options.grpcGcpOptions;
      this.autoThrottleAdministrativeRequests = options.autoThrottleAdministrativeRequests;
      this.retryAdministrativeRequestsSettings = options.retryAdministrativeRequestsSettings;
      this.trackTransactionStarter = options.trackTransactionStarter;
      this.defaultQueryOptions = options.defaultQueryOptions;
      this.callCredentialsProvider = options.callCredentialsProvider;
      this.asyncExecutorProvider = options.asyncExecutorProvider;
      this.compressorName = options.compressorName;
      this.channelProvider = options.channelProvider;
      this.channelConfigurator = options.channelConfigurator;
      this.interceptorProvider = options.interceptorProvider;
      this.attemptDirectPath = options.attemptDirectPath;
      this.directedReadOptions = options.directedReadOptions;
      this.useVirtualThreads = options.useVirtualThreads;
      this.enableApiTracing = options.enableApiTracing;
      this.enableExtendedTracing = options.enableExtendedTracing;
      this.enableEndToEndTracing = options.enableEndToEndTracing;
    }

    @Override
    public Builder setTransportOptions(TransportOptions transportOptions) {
      if (!(transportOptions instanceof GrpcTransportOptions)) {
        throw new IllegalArgumentException(
            "Only grpc transport is allowed for " + API_SHORT_NAME + ".");
      }
      return super.setTransportOptions(transportOptions);
    }

    @Override
    protected Set<String> getAllowedClientLibTokens() {
      return allowedClientLibTokens;
    }

    @InternalApi
    @Override
    public SpannerOptions.Builder setClientLibToken(String clientLibToken) {
      return super.setClientLibToken(
          clientLibToken + " " + ServiceOptions.getGoogApiClientLibName());
    }

    /**
     * Sets the {@code ChannelProvider}. {@link GapicSpannerRpc} would create a default one if none
     * is provided.
     *
     * <p>Setting a custom {@link TransportChannelProvider} also overrides any other settings that
     * affect the default channel provider. These must be set manually on the custom {@link
     * TransportChannelProvider} instead of on {@link SpannerOptions}. The settings of {@link
     * SpannerOptions} that have no effect if you set a custom {@link TransportChannelProvider} are:
     *
     * <ol>
     *   <li>{@link #setChannelConfigurator(ApiFunction)}
     *   <li>{@link #setHost(String)}
     *   <li>{@link #setNumChannels(int)}
     *   <li>{@link #setInterceptorProvider(GrpcInterceptorProvider)}
     *   <li>{@link #setHeaderProvider(com.google.api.gax.rpc.HeaderProvider)}
     * </ol>
     */
    public Builder setChannelProvider(TransportChannelProvider channelProvider) {
      this.channelProvider = channelProvider;
      return this;
    }

    /**
     * Sets an {@link ApiFunction} that will be used to configure the transport channel. This will
     * only be used if no custom {@link TransportChannelProvider} has been set.
     */
    public Builder setChannelConfigurator(
        @SuppressWarnings("rawtypes")
            ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> channelConfigurator) {
      this.channelConfigurator = channelConfigurator;
      return this;
    }

    /**
     * Sets the {@code GrpcInterceptorProvider}. {@link GapicSpannerRpc} would create a default one
     * if none is provided.
     */
    public Builder setInterceptorProvider(GrpcInterceptorProvider interceptorProvider) {
      this.interceptorProvider = interceptorProvider;
      return this;
    }

    /**
     * Sets the number of gRPC channels to use. By default 4 channels are created per {@link
     * SpannerOptions}.
     */
    public Builder setNumChannels(int numChannels) {
      this.numChannels = numChannels;
      return this;
    }

    /** Sets the name format for transport channel threads that should be used by this instance. */
    Builder setTransportChannelExecutorThreadNameFormat(
        String transportChannelExecutorThreadNameFormat) {
      this.transportChannelExecutorThreadNameFormat = transportChannelExecutorThreadNameFormat;
      return this;
    }

    /**
     * Sets the options for managing the session pool. If not specified then the default {@code
     * SessionPoolOptions} is used.
     */
    public Builder setSessionPoolOption(SessionPoolOptions sessionPoolOptions) {
      this.sessionPoolOptions = sessionPoolOptions;
      return this;
    }

    /**
     * Sets the database role that should be used for connections that are created by this instance.
     * The database role that is used determines the access permissions that a connection has. This
     * can for example be used to create connections that are only permitted to access certain
     * tables.
     */
    public Builder setDatabaseRole(String databaseRole) {
      this.databaseRole = databaseRole;
      return this;
    }

    /**
     * Sets the labels to add to all Sessions created in this client.
     *
     * @param sessionLabels Map from label key to label value. Label key and value cannot be null.
     *     For more information on valid syntax see <a
     *     href="https://cloud.google.com/spanner/docs/reference/rpc/google.spanner.v1#google.spanner.v1.Session">
     *     api docs </a>.
     */
    public Builder setSessionLabels(Map<String, String> sessionLabels) {
      Preconditions.checkNotNull(sessionLabels, "Session labels map cannot be null");
      for (String value : sessionLabels.values()) {
        Preconditions.checkNotNull(value, "Null values are not allowed in the labels map.");
      }
      this.sessionLabels = ImmutableMap.copyOf(sessionLabels);
      return this;
    }

    /**
     * {@link SpannerOptions.Builder} does not support global retry settings, as it creates three
     * different gRPC clients: {@link Spanner}, {@link DatabaseAdminClient} and {@link
     * InstanceAdminClient}. Instead of calling this method, you should set specific {@link
     * RetrySettings} for each of the underlying gRPC clients by calling respectively {@link
     * #getSpannerStubSettingsBuilder()}, {@link #getDatabaseAdminStubSettingsBuilder()} or {@link
     * #getInstanceAdminStubSettingsBuilder()}.
     */
    @Override
    public Builder setRetrySettings(RetrySettings retrySettings) {
      throw new UnsupportedOperationException(
          "SpannerOptions does not support setting global retry settings. "
              + "Call spannerStubSettingsBuilder().<method-name>Settings().setRetrySettings(RetrySettings) instead.");
    }

    /**
     * Returns the {@link SpannerStubSettings.Builder} that will be used to build the {@link
     * SpannerRpc}. Use this to set custom {@link RetrySettings} for individual gRPC methods.
     *
     * <p>The library will automatically use the defaults defined in {@link SpannerStubSettings} if
     * no custom settings are set. The defaults are the same as the defaults that are used by {@link
     * SpannerSettings}, and are generated from the file <a
     * href="https://github.com/googleapis/googleapis/blob/master/google/spanner/v1/spanner_gapic.yaml">spanner_gapic.yaml</a>.
     * Retries are configured for idempotent methods but not for non-idempotent methods.
     *
     * <p>You can set the same {@link RetrySettings} for all unary methods by calling this:
     *
     * <pre><code>
     * builder
     *     .getSpannerStubSettingsBuilder()
     *     .applyToAllUnaryMethods(
     *         new ApiFunction&lt;UnaryCallSettings.Builder&lt;?, ?&gt;, Void&gt;() {
     *           public Void apply(Builder&lt;?, ?&gt; input) {
     *             input.setRetrySettings(retrySettings);
     *             return null;
     *           }
     *         });
     * </code></pre>
     */
    public SpannerStubSettings.Builder getSpannerStubSettingsBuilder() {
      return spannerStubSettingsBuilder;
    }

    /**
     * Returns the {@link InstanceAdminStubSettings.Builder} that will be used to build the {@link
     * SpannerRpc}. Use this to set custom {@link RetrySettings} for individual gRPC methods.
     *
     * <p>The library will automatically use the defaults defined in {@link
     * InstanceAdminStubSettings} if no custom settings are set. The defaults are the same as the
     * defaults that are used by {@link InstanceAdminSettings}, and are generated from the file <a
     * href="https://github.com/googleapis/googleapis/blob/master/google/spanner/admin/instance/v1/spanner_admin_instance_gapic.yaml">spanner_admin_instance_gapic.yaml</a>.
     * Retries are configured for idempotent methods but not for non-idempotent methods.
     *
     * <p>You can set the same {@link RetrySettings} for all unary methods by calling this:
     *
     * <pre><code>
     * builder
     *     .getInstanceAdminStubSettingsBuilder()
     *     .applyToAllUnaryMethods(
     *         new ApiFunction&lt;UnaryCallSettings.Builder&lt;?, ?&gt;, Void&gt;() {
     *           public Void apply(Builder&lt;?, ?&gt; input) {
     *             input.setRetrySettings(retrySettings);
     *             return null;
     *           }
     *         });
     * </code></pre>
     */
    public InstanceAdminStubSettings.Builder getInstanceAdminStubSettingsBuilder() {
      return instanceAdminStubSettingsBuilder;
    }

    /**
     * Returns the {@link DatabaseAdminStubSettings.Builder} that will be used to build the {@link
     * SpannerRpc}. Use this to set custom {@link RetrySettings} for individual gRPC methods.
     *
     * <p>The library will automatically use the defaults defined in {@link
     * DatabaseAdminStubSettings} if no custom settings are set. The defaults are the same as the
     * defaults that are used by {@link DatabaseAdminSettings}, and are generated from the file <a
     * href="https://github.com/googleapis/googleapis/blob/master/google/spanner/admin/database/v1/spanner_admin_database_gapic.yaml">spanner_admin_database_gapic.yaml</a>.
     * Retries are configured for idempotent methods but not for non-idempotent methods.
     *
     * <p>You can set the same {@link RetrySettings} for all unary methods by calling this:
     *
     * <pre><code>
     * builder
     *     .getDatabaseAdminStubSettingsBuilder()
     *     .applyToAllUnaryMethods(
     *         new ApiFunction&lt;UnaryCallSettings.Builder&lt;?, ?&gt;, Void&gt;() {
     *           public Void apply(Builder&lt;?, ?&gt; input) {
     *             input.setRetrySettings(retrySettings);
     *             return null;
     *           }
     *         });
     * </code></pre>
     */
    public DatabaseAdminStubSettings.Builder getDatabaseAdminStubSettingsBuilder() {
      return databaseAdminStubSettingsBuilder;
    }

    /**
     * Sets a timeout specifically for Partitioned DML statements executed through {@link
     * DatabaseClient#executePartitionedUpdate(Statement, UpdateOption...)}. The default is 2 hours.
     */
    public Builder setPartitionedDmlTimeout(Duration timeout) {
      this.partitionedDmlTimeout = timeout;
      return this;
    }

    /**
     * Instructs the client library to automatically throttle the number of administrative requests
     * if the rate of administrative requests generated by this {@link Spanner} instance will exceed
     * the administrative limits Cloud Spanner. The default behavior is to not throttle any
     * requests. If the limit is exceeded, Cloud Spanner will return a RESOURCE_EXHAUSTED error.
     * More information on the administrative limits can be found here:
     * https://cloud.google.com/spanner/quotas#administrative_limits. Setting this option is not a
     * guarantee that the rate will never be exceeded, as this option will only throttle requests
     * coming from this client. Additional requests from other clients could still cause the limit
     * to be exceeded.
     */
    public Builder setAutoThrottleAdministrativeRequests() {
      this.autoThrottleAdministrativeRequests = true;
      return this;
    }

    /**
     * Disables automatic retries of administrative requests that fail if the <a
     * href="https://cloud.google.com/spanner/quotas#administrative_limits">https://cloud.google.com/spanner/quotas#administrative_limits</a>
     * have been exceeded. You should disable these retries if you intend to handle these errors in
     * your application.
     */
    public Builder disableAdministrativeRequestRetries() {
      this.retryAdministrativeRequestsSettings =
          this.retryAdministrativeRequestsSettings.toBuilder().setMaxAttempts(1).build();
      return this;
    }

    /**
     * Sets the retry settings for retrying administrative requests when the quote of administrative
     * requests per minute has been exceeded.
     */
    Builder setRetryAdministrativeRequestsSettings(
        RetrySettings retryAdministrativeRequestsSettings) {
      this.retryAdministrativeRequestsSettings =
          Preconditions.checkNotNull(retryAdministrativeRequestsSettings);
      return this;
    }

    /**
     * Instructs the client library to track the first request of each read/write transaction. This
     * statement will include a BeginTransaction option and will return a transaction id as part of
     * its result. All other statements in the same transaction must wait for this first statement
     * to finish before they can proceed. By setting this option the client library will throw a
     * {@link SpannerException} with {@link ErrorCode#DEADLINE_EXCEEDED} for any subsequent
     * statement that has waited for at least 60 seconds for the first statement to return a
     * transaction id, including the stacktrace of the initial statement that should have returned a
     * transaction id.
     */
    public Builder setTrackTransactionStarter() {
      this.trackTransactionStarter = true;
      return this;
    }

    /**
     * Sets the default {@link QueryOptions} that will be used for all queries on the specified
     * database. Query options can also be specified on a per-query basis and as environment
     * variables. The precedence of these settings are:
     *
     * <ol>
     *   <li>Query options for a specific query
     *   <li>Environment variables
     *   <li>These default query options
     * </ol>
     *
     * Each {@link QueryOption} value that is used for a query is determined individually based on
     * the above precedence. If for example a value for {@link QueryOptions#getOptimizerVersion()}
     * is specified in an environment variable and a value for {@link
     * QueryOptions#getOptimizerStatisticsPackage()} is specified for a specific query, both values
     * will be used for the specific query. Environment variables are only read during the
     * initialization of a {@link SpannerOptions} instance. Changing an environment variable after
     * initializing a {@link SpannerOptions} instance will not have any effect on that instance.
     */
    public Builder setDefaultQueryOptions(DatabaseId database, QueryOptions defaultQueryOptions) {
      this.defaultQueryOptions.put(database, defaultQueryOptions);
      return this;
    }

    /** Gets the {@link QueryOptions} specified in the {@link SpannerEnvironment}. */
    QueryOptions getEnvironmentQueryOptions() {
      return QueryOptions.newBuilder()
          .setOptimizerVersion(environment.getOptimizerVersion())
          .setOptimizerStatisticsPackage(environment.getOptimizerStatisticsPackage())
          .build();
    }

    /**
     * Sets a {@link CallCredentialsProvider} that can deliver {@link CallCredentials} to use on a
     * per-gRPC basis.
     */
    public Builder setCallCredentialsProvider(CallCredentialsProvider callCredentialsProvider) {
      this.callCredentialsProvider = callCredentialsProvider;
      return this;
    }

    /**
     * Sets the compression to use for all gRPC calls. The compressor must be a valid name known in
     * the {@link CompressorRegistry}. This will enable compression both from the client to the
     * server and from the server to the client.
     *
     * <p>Supported values are:
     *
     * <ul>
     *   <li>gzip: Enable gzip compression
     *   <li>identity: Disable compression
     *   <li><code>null</code>: Use default compression
     * </ul>
     */
    @ExperimentalApi("https://github.com/grpc/grpc-java/issues/1704")
    public Builder setCompressorName(@Nullable String compressorName) {
      Preconditions.checkArgument(
          compressorName == null
              || CompressorRegistry.getDefaultInstance().lookupCompressor(compressorName) != null,
          String.format("%s is not a known compressor", compressorName));
      this.compressorName = compressorName;
      return this;
    }

    /**
     * Sets the {@link ExecutorProvider} to use for high-level async calls that need an executor,
     * such as fetching results for an {@link AsyncResultSet}.
     *
     * <p>Async methods will use a sensible default if no custom {@link ExecutorProvider} has been
     * set. The default {@link ExecutorProvider} uses a cached thread pool containing a maximum of 8
     * threads. The pool is lazily initialized and will not create any threads if the user
     * application does not use any async methods. It will also scale down the thread usage if the
     * async load allows for that.
     *
     * <p>Call {@link SpannerOptions#createAsyncExecutorProvider(int, long, TimeUnit)} to create a
     * provider with a custom pool size or call {@link
     * FixedCloseableExecutorProvider#create(ScheduledExecutorService)} to create a {@link
     * CloseableExecutorProvider} from a standard Java {@link ScheduledExecutorService}.
     */
    public Builder setAsyncExecutorProvider(CloseableExecutorProvider provider) {
      this.asyncExecutorProvider = provider;
      return this;
    }

    /**
     * Sets the {@link DirectedReadOption} that specify which replicas or regions should be used for
     * non-transactional reads or queries.
     *
     * <p>DirectedReadOptions set at the request level will take precedence over the options set
     * using this method.
     *
     * <p>An example below of how {@link DirectedReadOptions} can be constructed by including a
     * replica.
     *
     * <pre><code>
     * DirectedReadOptions.newBuilder()
     *           .setIncludeReplicas(
     *               IncludeReplicas.newBuilder()
     *                   .addReplicaSelections(
     *                       ReplicaSelection.newBuilder().setLocation("us-east1").build()))
     *           .build();
     *           }
     * </code></pre>
     */
    public Builder setDirectedReadOptions(DirectedReadOptions directedReadOptions) {
      this.directedReadOptions =
          Preconditions.checkNotNull(directedReadOptions, "DirectedReadOptions cannot be null");
      return this;
    }

    /**
     * Specifying this will allow the client to prefetch up to {@code prefetchChunks} {@code
     * PartialResultSet} chunks for each read and query. The data size of each chunk depends on the
     * server implementation but a good rule of thumb is that each chunk will be up to 1 MiB. Larger
     * values reduce the likelihood of blocking while consuming results at the cost of greater
     * memory consumption. {@code prefetchChunks} should be greater than 0. To get good performance
     * choose a value that is large enough to allow buffering of chunks for an entire row. Apart
     * from the buffered chunks, there can be at most one more row buffered in the client. This can
     * be overridden on a per read/query basis by {@link Options#prefetchChunks()}. If unspecified,
     * we will use a default value (currently 4).
     */
    public Builder setPrefetchChunks(int prefetchChunks) {
      this.prefetchChunks = prefetchChunks;
      return this;
    }

    /**
     * Specifies how values that are returned from a query should be decoded and converted from
     * protobuf values into plain Java objects.
     */
    public Builder setDecodeMode(DecodeMode decodeMode) {
      this.decodeMode = decodeMode;
      return this;
    }

    @Override
    public Builder setHost(String host) {
      super.setHost(host);
      // Setting a host should override any SPANNER_EMULATOR_HOST setting.
      setEmulatorHost(null);
      return this;
    }

    /** Enables gRPC-GCP extension with the default settings. */
    public Builder enableGrpcGcpExtension() {
      return this.enableGrpcGcpExtension(null);
    }

    /**
     * Enables gRPC-GCP extension and uses provided options for configuration. The metric registry
     * and default Spanner metric labels will be added automatically.
     */
    public Builder enableGrpcGcpExtension(GcpManagedChannelOptions options) {
      this.grpcGcpExtensionEnabled = true;
      this.grpcGcpOptions = options;
      return this;
    }

    /** Disables gRPC-GCP extension. */
    public Builder disableGrpcGcpExtension() {
      this.grpcGcpExtensionEnabled = false;
      return this;
    }

    /**
     * Sets the host of an emulator to use. By default the value is read from an environment
     * variable. If the environment variable is not set, this will be <code>null</code>.
     */
    public Builder setEmulatorHost(String emulatorHost) {
      this.emulatorHost = emulatorHost;
      return this;
    }

    /**
     * Sets OpenTelemetry object to be used for Spanner Metrics and Traces. GlobalOpenTelemetry will
     * be used as fallback if this options is not set.
     */
    public Builder setOpenTelemetry(OpenTelemetry openTelemetry) {
      this.openTelemetry = openTelemetry;
      return this;
    }

    /**
     * Enable leader aware routing. Leader aware routing would route all requests in RW/PDML
     * transactions to the leader region.
     */
    public Builder enableLeaderAwareRouting() {
      this.leaderAwareRoutingEnabled = true;
      return this;
    }

    /**
     * Disable leader aware routing. Disabling leader aware routing would route all requests in
     * RW/PDML transactions to any region.
     */
    public Builder disableLeaderAwareRouting() {
      this.leaderAwareRoutingEnabled = false;
      return this;
    }

    @BetaApi
    public Builder disableDirectPath() {
      this.attemptDirectPath = false;
      return this;
    }

    /**
     * Enables/disables the use of virtual threads for the gRPC executor. Setting this option only
     * has any effect on Java 21 and higher. In all other cases, the option will be ignored.
     */
    @BetaApi
    protected Builder setUseVirtualThreads(boolean useVirtualThreads) {
      this.useVirtualThreads = useVirtualThreads;
      return this;
    }

    /**
     * Creates and sets an {@link com.google.api.gax.tracing.ApiTracer} for the RPCs that are
     * executed by this client. Enabling this creates traces for each individual RPC execution,
     * including events/annotations when an RPC is retried or fails. The traces are only exported if
     * an OpenTelemetry or OpenCensus trace exporter has been configured for the client.
     */
    public Builder setEnableApiTracing(boolean enableApiTracing) {
      this.enableApiTracing = enableApiTracing;
      return this;
    }

    /**
     * Sets whether to enable extended OpenTelemetry tracing. Enabling this option will add the
     * following additional attributes to the traces that are generated by the client:
     *
     * <ul>
     *   <li>db.statement: Contains the SQL statement that is being executed.
     * </ul>
     */
    public Builder setEnableExtendedTracing(boolean enableExtendedTracing) {
      this.enableExtendedTracing = enableExtendedTracing;
      return this;
    }

    /**
     * Sets whether to enable end to end tracing. Enabling this option will create the
     * trace spans at the Spanner layer.
     */
    public Builder setEnableEndToEndTracing(boolean enableEndToEndTracing) {
      this.enableEndToEndTracing = enableEndToEndTracing;
      return this;
    }
 
    @SuppressWarnings("rawtypes")
    @Override
    public SpannerOptions build() {
      // Set the host of emulator has been set.
      if (emulatorHost != null) {
        if (!emulatorHost.startsWith("http")) {
          emulatorHost = "http://" + emulatorHost;
        }
        this.setHost(emulatorHost);
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        this.setChannelConfigurator(ManagedChannelBuilder::usePlaintext);
        // As we are using plain text, we should never send any credentials.
        this.setCredentials(NoCredentials.getInstance());
      }
      if (this.numChannels == null) {
        this.numChannels =
            this.grpcGcpExtensionEnabled ? GRPC_GCP_ENABLED_DEFAULT_CHANNELS : DEFAULT_CHANNELS;
      }

      synchronized (lock) {
        if (activeTracingFramework == null) {
          activeTracingFramework = TracingFramework.OPEN_CENSUS;
        }
      }
      return new SpannerOptions(this);
    }
  }

  /** Returns default instance of {@code SpannerOptions}. */
  public static SpannerOptions getDefaultInstance() {
    return newBuilder().build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Sets the environment to use to read configuration. The default will read configuration from
   * environment variables.
   */
  public static void useEnvironment(SpannerEnvironment environment) {
    SpannerOptions.environment = environment;
  }

  /**
   * Sets the environment to use to read configuration to the default environment. This will read
   * configuration from environment variables.
   */
  public static void useDefaultEnvironment() {
    SpannerOptions.environment = SpannerEnvironmentImpl.INSTANCE;
  }

  /**
   * Enables OpenTelemetry traces. Enabling OpenTelemetry traces will disable OpenCensus traces. By
   * default, OpenCensus traces are enabled.
   */
  public static void enableOpenTelemetryTraces() {
    synchronized (lock) {
      if (activeTracingFramework != null
          && activeTracingFramework != TracingFramework.OPEN_TELEMETRY) {
        throw new IllegalStateException(
            "ActiveTracingFramework is set to OpenCensus and cannot be reset after SpannerOptions object is created.");
      }
      activeTracingFramework = TracingFramework.OPEN_TELEMETRY;
    }
  }

  /** Enables OpenCensus traces. Enabling OpenCensus traces will disable OpenTelemetry traces. */
  @ObsoleteApi(
      "The OpenCensus project is deprecated. Use enableOpenTelemetryTraces to switch to OpenTelemetry traces")
  public static void enableOpenCensusTraces() {
    synchronized (lock) {
      if (activeTracingFramework != null
          && activeTracingFramework != TracingFramework.OPEN_CENSUS) {
        throw new IllegalStateException(
            "ActiveTracingFramework is set to OpenTelemetry and cannot be reset after SpannerOptions object is created.");
      }
      activeTracingFramework = TracingFramework.OPEN_CENSUS;
    }
  }

  /**
   * Always resets the activeTracingFramework. This variable is used for internal testing, and is
   * not a valid production scenario
   */
  @ObsoleteApi(
      "The OpenCensus project is deprecated. Use enableOpenTelemetryTraces to switch to OpenTelemetry traces")
  static void resetActiveTracingFramework() {
    activeTracingFramework = null;
  }

  public static TracingFramework getActiveTracingFramework() {
    synchronized (lock) {
      if (activeTracingFramework == null) {
        return TracingFramework.OPEN_CENSUS;
      }
      return activeTracingFramework;
    }
  }

  /** Disables OpenCensus metrics. Disable OpenCensus metrics before creating Spanner client. */
  public static void disableOpenCensusMetrics() {
    SpannerOptions.enableOpenCensusMetrics = false;
  }

  @VisibleForTesting
  static void enableOpenCensusMetrics() {
    SpannerOptions.enableOpenCensusMetrics = true;
  }

  public static boolean isEnabledOpenCensusMetrics() {
    return SpannerOptions.enableOpenCensusMetrics;
  }

  /** Enables OpenTelemetry metrics. Enable OpenTelemetry metrics before creating Spanner client. */
  public static void enableOpenTelemetryMetrics() {
    SpannerOptions.enableOpenTelemetryMetrics = true;
  }

  public static boolean isEnabledOpenTelemetryMetrics() {
    return SpannerOptions.enableOpenTelemetryMetrics;
  }

  @Override
  protected String getDefaultProject() {
    String projectId = getDefaultProjectId();
    // The project id does not matter if we are using the emulator.
    if (projectId == null && System.getenv("SPANNER_EMULATOR_HOST") != null) {
      return "emulator-project";
    }
    return projectId;
  }

  public TransportChannelProvider getChannelProvider() {
    return channelProvider;
  }

  @SuppressWarnings("rawtypes")
  public ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> getChannelConfigurator() {
    return channelConfigurator;
  }

  public GrpcInterceptorProvider getInterceptorProvider() {
    return interceptorProvider;
  }

  public int getNumChannels() {
    return numChannels;
  }

  public String getTransportChannelExecutorThreadNameFormat() {
    return transportChannelExecutorThreadNameFormat;
  }

  public SessionPoolOptions getSessionPoolOptions() {
    return sessionPoolOptions;
  }

  public String getDatabaseRole() {
    return databaseRole;
  }

  public Map<String, String> getSessionLabels() {
    return sessionLabels;
  }

  public SpannerStubSettings getSpannerStubSettings() {
    return spannerStubSettings;
  }

  public InstanceAdminStubSettings getInstanceAdminStubSettings() {
    return instanceAdminStubSettings;
  }

  public DatabaseAdminStubSettings getDatabaseAdminStubSettings() {
    return databaseAdminStubSettings;
  }

  public Duration getPartitionedDmlTimeout() {
    return partitionedDmlTimeout;
  }

  public boolean isGrpcGcpExtensionEnabled() {
    return grpcGcpExtensionEnabled;
  }

  public GcpManagedChannelOptions getGrpcGcpOptions() {
    return grpcGcpOptions;
  }

  public boolean isAutoThrottleAdministrativeRequests() {
    return autoThrottleAdministrativeRequests;
  }

  public RetrySettings getRetryAdministrativeRequestsSettings() {
    return retryAdministrativeRequestsSettings;
  }

  public boolean isTrackTransactionStarter() {
    return trackTransactionStarter;
  }

  public CallCredentialsProvider getCallCredentialsProvider() {
    return callCredentialsProvider;
  }

  public String getCompressorName() {
    return compressorName;
  }

  public boolean isLeaderAwareRoutingEnabled() {
    return leaderAwareRoutingEnabled;
  }

  public DirectedReadOptions getDirectedReadOptions() {
    return directedReadOptions;
  }

  @BetaApi
  public boolean isAttemptDirectPath() {
    return attemptDirectPath;
  }

  /**
   * Returns an instance of OpenTelemetry. If OpenTelemetry object is not set via SpannerOptions
   * then GlobalOpenTelemetry will be used as fallback.
   */
  public OpenTelemetry getOpenTelemetry() {
    if (this.openTelemetry != null) {
      return this.openTelemetry;
    } else {
      return GlobalOpenTelemetry.get();
    }
  }

  @Override
  public ApiTracerFactory getApiTracerFactory() {
    List<ApiTracerFactory> apiTracerFactories = new ArrayList();
    // Prefer any direct ApiTracerFactory that might have been set on the builder.
    apiTracerFactories.add(
        MoreObjects.firstNonNull(super.getApiTracerFactory(), getDefaultApiTracerFactory()));

    return new CompositeTracerFactory(apiTracerFactories);
  }

  private ApiTracerFactory getDefaultApiTracerFactory() {
    if (isEnableApiTracing()) {
      if (activeTracingFramework == TracingFramework.OPEN_TELEMETRY) {
        return new OpenTelemetryApiTracerFactory(
            getOpenTelemetry()
                .getTracer(
                    MetricRegistryConstants.INSTRUMENTATION_SCOPE,
                    GaxProperties.getLibraryVersion(getClass())),
            Attributes.empty());
      } else if (activeTracingFramework == TracingFramework.OPEN_CENSUS) {
        return new OpencensusTracerFactory();
      }
    }
    return BaseApiTracerFactory.getInstance();
  }

  /**
   * Returns true if an {@link com.google.api.gax.tracing.ApiTracer} should be created and set on
   * the Spanner client. Enabling this only has effect if an OpenTelemetry or OpenCensus trace
   * exporter has been configured.
   */
  public boolean isEnableApiTracing() {
    return enableApiTracing;
  }

  @BetaApi
  public boolean isUseVirtualThreads() {
    return useVirtualThreads;
  }

  /**
   * Returns whether extended OpenTelemetry tracing is enabled. Enabling this option will add the
   * following additional attributes to the traces that are generated by the client:
   *
   * <ul>
   *   <li>db.statement: Contains the SQL statement that is being executed.
   * </ul>
   */
  public boolean isEnableExtendedTracing() {
    return enableExtendedTracing;
  }

  /**
   * Returns whether end to end tracing is enabled. If this option is enabled then trace spans
   * will be created at the Spanner layer.
   */
  public boolean isEndToEndTracingEnabled() {
    return enableEndToEndTracing;
  }

  /** Returns the default query options to use for the specific database. */
  public QueryOptions getDefaultQueryOptions(DatabaseId databaseId) {
    // Use the specific query options for the database if any have been specified. These have
    // already been merged with the query options specified in the environment variables.
    QueryOptions options = this.mergedQueryOptions.get(databaseId);
    if (options == null) {
      // Use the generic environment query options. These are initialized as a default instance of
      // query options and appended with any options specified in the environment variables.
      options = this.envQueryOptions;
    }
    return options;
  }

  public CloseableExecutorProvider getAsyncExecutorProvider() {
    return asyncExecutorProvider;
  }

  public int getPrefetchChunks() {
    return prefetchChunks;
  }

  public DecodeMode getDecodeMode() {
    return decodeMode;
  }

  public static GrpcTransportOptions getDefaultGrpcTransportOptions() {
    return GrpcTransportOptions.newBuilder().build();
  }

  @Override
  protected String getDefaultHost() {
    return DEFAULT_HOST;
  }

  private static class SpannerDefaults implements ServiceDefaults<Spanner, SpannerOptions> {

    @Override
    public SpannerFactory getDefaultServiceFactory() {
      return DefaultSpannerFactory.INSTANCE;
    }

    @Override
    public SpannerRpcFactory getDefaultRpcFactory() {
      return DefaultSpannerRpcFactory.INSTANCE;
    }

    @Override
    public TransportOptions getDefaultTransportOptions() {
      return getDefaultGrpcTransportOptions();
    }
  }

  @Override
  public Set<String> getScopes() {
    return SCOPES;
  }

  protected SpannerRpc getSpannerRpcV1() {
    return (SpannerRpc) getRpc();
  }

  /**
   * @return <code>true</code> if the cached Spanner service instance is <code>null</code> or
   *     closed. This will cause the method {@link #getService()} to create a new {@link SpannerRpc}
   *     instance when one is requested.
   */
  @Override
  protected boolean shouldRefreshService(Spanner cachedService) {
    return cachedService == null || cachedService.isClosed();
  }

  /**
   * @return <code>true</code> if the cached {@link ServiceRpc} instance is <code>null</code> or
   *     closed. This will cause the method {@link #getRpc()} to create a new {@link Spanner}
   *     instance when one is requested.
   */
  @Override
  protected boolean shouldRefreshRpc(ServiceRpc cachedRpc) {
    return cachedRpc == null || ((SpannerRpc) cachedRpc).isClosed();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  public String getEndpoint() {
    URL url;
    try {
      url = new URL(getHost());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid host: " + getHost(), e);
    }
    return String.format(
        "%s:%s", url.getHost(), url.getPort() < 0 ? url.getDefaultPort() : url.getPort());
  }
}
