/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.connection;

import com.google.api.core.InternalApi;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import io.opentelemetry.api.OpenTelemetry;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Internal connection API for Google Cloud Spanner. This class may introduce breaking changes
 * without prior notice.
 *
 * <p>Options for creating a {@link Connection} to a Google Cloud Spanner database.
 *
 * <p>Usage:
 *
 * <pre>
 * <!--SNIPPET {@link ConnectionOptions} usage-->
 * {@code
 * ConnectionOptions options = ConnectionOptions.newBuilder()
 *       .setUri("cloudspanner:/projects/my_project_id/instances/my_instance_id/databases/my_database_name?autocommit=false")
 *       .setCredentialsUrl("/home/cloudspanner-keys/my-key.json")
 *       .build();
 * try(Connection connection = options.getConnection()) {
 *   try(ResultSet rs = connection.executeQuery(Statement.of("SELECT SingerId, AlbumId, MarketingBudget FROM Albums"))) {
 *     while(rs.next()) {
 *       // do something
 *     }
 *   }
 * }
 * }
 * <!--SNIPPET {@link ConnectionOptions} usage-->
 * </pre>
 */
@InternalApi
public class ConnectionOptions {
  /** Supported connection properties that can be included in the connection URI. */
  public static class ConnectionProperty {
    private static final String[] BOOLEAN_VALUES = new String[] {"true", "false"};
    private final String name;
    private final String description;
    private final String defaultValue;
    private final String[] validValues;
    private final int hashCode;

    private static ConnectionProperty createStringProperty(String name, String description) {
      return new ConnectionProperty(name, description, "", null);
    }

    private static ConnectionProperty createBooleanProperty(
        String name, String description, Boolean defaultValue) {
      return new ConnectionProperty(
          name,
          description,
          defaultValue == null ? "" : String.valueOf(defaultValue),
          BOOLEAN_VALUES);
    }

    private static ConnectionProperty createIntProperty(
        String name, String description, int defaultValue) {
      return new ConnectionProperty(name, description, String.valueOf(defaultValue), null);
    }

    private static ConnectionProperty createEmptyProperty(String name) {
      return new ConnectionProperty(name, "", "", null);
    }

    private ConnectionProperty(
        String name, String description, String defaultValue, String[] validValues) {
      Preconditions.checkNotNull(name);
      Preconditions.checkNotNull(description);
      Preconditions.checkNotNull(defaultValue);
      this.name = name;
      this.description = description;
      this.defaultValue = defaultValue;
      this.validValues = validValues;
      this.hashCode = name.toLowerCase().hashCode();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ConnectionProperty)) {
        return false;
      }
      return ((ConnectionProperty) o).name.equalsIgnoreCase(this.name);
    }

    /** @return the name of this connection property. */
    public String getName() {
      return name;
    }

    /** @return the description of this connection property. */
    public String getDescription() {
      return description;
    }

    /** @return the default value of this connection property. */
    public String getDefaultValue() {
      return defaultValue;
    }

    /**
     * @return the valid values for this connection property. <code>null</code> indicates no
     *     restriction.
     */
    public String[] getValidValues() {
      return validValues;
    }
  }

  private static final LocalConnectionChecker LOCAL_CONNECTION_CHECKER =
      new LocalConnectionChecker();
  private static final boolean DEFAULT_USE_PLAIN_TEXT = false;
  static final boolean DEFAULT_AUTOCOMMIT = true;
  static final boolean DEFAULT_READONLY = false;
  static final boolean DEFAULT_RETRY_ABORTS_INTERNALLY = true;
  static final boolean DEFAULT_USE_VIRTUAL_THREADS = false;
  static final boolean DEFAULT_USE_VIRTUAL_GRPC_TRANSPORT_THREADS = false;
  private static final String DEFAULT_CREDENTIALS = null;
  private static final String DEFAULT_OAUTH_TOKEN = null;
  private static final String DEFAULT_MIN_SESSIONS = null;
  private static final String DEFAULT_MAX_SESSIONS = null;
  private static final String DEFAULT_NUM_CHANNELS = null;
  static final String DEFAULT_ENDPOINT = null;
  private static final String DEFAULT_CHANNEL_PROVIDER = null;
  private static final String DEFAULT_DATABASE_ROLE = null;
  private static final String DEFAULT_USER_AGENT = null;
  private static final String DEFAULT_OPTIMIZER_VERSION = "";
  private static final String DEFAULT_OPTIMIZER_STATISTICS_PACKAGE = "";
  private static final RpcPriority DEFAULT_RPC_PRIORITY = null;
  private static final DdlInTransactionMode DEFAULT_DDL_IN_TRANSACTION_MODE =
      DdlInTransactionMode.ALLOW_IN_EMPTY_TRANSACTION;
  private static final boolean DEFAULT_RETURN_COMMIT_STATS = false;
  private static final boolean DEFAULT_LENIENT = false;
  private static final boolean DEFAULT_ROUTE_TO_LEADER = true;
  private static final boolean DEFAULT_DELAY_TRANSACTION_START_UNTIL_FIRST_WRITE = false;
  private static final boolean DEFAULT_TRACK_SESSION_LEAKS = true;
  private static final boolean DEFAULT_TRACK_CONNECTION_LEAKS = true;
  private static final boolean DEFAULT_DATA_BOOST_ENABLED = false;
  private static final boolean DEFAULT_AUTO_PARTITION_MODE = false;
  private static final int DEFAULT_MAX_PARTITIONS = 0;
  private static final int DEFAULT_MAX_PARTITIONED_PARALLELISM = 1;
  private static final Boolean DEFAULT_ENABLE_EXTENDED_TRACING = null;
  private static final Boolean DEFAULT_ENABLE_API_TRACING = null;

  private static final String PLAIN_TEXT_PROTOCOL = "http:";
  private static final String HOST_PROTOCOL = "https:";
  private static final String DEFAULT_HOST = "https://spanner.googleapis.com";
  private static final String SPANNER_EMULATOR_HOST_ENV_VAR = "SPANNER_EMULATOR_HOST";
  private static final String DEFAULT_EMULATOR_HOST = "http://localhost:9010";
  /** Use plain text is only for local testing purposes. */
  private static final String USE_PLAIN_TEXT_PROPERTY_NAME = "usePlainText";
  /** Name of the 'autocommit' connection property. */
  public static final String AUTOCOMMIT_PROPERTY_NAME = "autocommit";
  /** Name of the 'readonly' connection property. */
  public static final String READONLY_PROPERTY_NAME = "readonly";
  /** Name of the 'routeToLeader' connection property. */
  public static final String ROUTE_TO_LEADER_PROPERTY_NAME = "routeToLeader";
  /** Name of the 'retry aborts internally' connection property. */
  public static final String RETRY_ABORTS_INTERNALLY_PROPERTY_NAME = "retryAbortsInternally";
  /** Name of the property to enable/disable virtual threads for the statement executor. */
  public static final String USE_VIRTUAL_THREADS_PROPERTY_NAME = "useVirtualThreads";
  /** Name of the property to enable/disable virtual threads for gRPC transport. */
  public static final String USE_VIRTUAL_GRPC_TRANSPORT_THREADS_PROPERTY_NAME =
      "useVirtualGrpcTransportThreads";
  /** Name of the 'credentials' connection property. */
  public static final String CREDENTIALS_PROPERTY_NAME = "credentials";
  /** Name of the 'encodedCredentials' connection property. */
  public static final String ENCODED_CREDENTIALS_PROPERTY_NAME = "encodedCredentials";

  public static final String ENABLE_ENCODED_CREDENTIALS_SYSTEM_PROPERTY =
      "ENABLE_ENCODED_CREDENTIALS";
  /** Name of the 'credentialsProvider' connection property. */
  public static final String CREDENTIALS_PROVIDER_PROPERTY_NAME = "credentialsProvider";

  public static final String ENABLE_CREDENTIALS_PROVIDER_SYSTEM_PROPERTY =
      "ENABLE_CREDENTIALS_PROVIDER";
  /**
   * OAuth token to use for authentication. Cannot be used in combination with a credentials file.
   */
  public static final String OAUTH_TOKEN_PROPERTY_NAME = "oauthToken";
  /** Name of the 'minSessions' connection property. */
  public static final String MIN_SESSIONS_PROPERTY_NAME = "minSessions";
  /** Name of the 'maxSessions' connection property. */
  public static final String MAX_SESSIONS_PROPERTY_NAME = "maxSessions";
  /** Name of the 'numChannels' connection property. */
  public static final String NUM_CHANNELS_PROPERTY_NAME = "numChannels";
  /** Name of the 'endpoint' connection property. */
  public static final String ENDPOINT_PROPERTY_NAME = "endpoint";
  /** Name of the 'channelProvider' connection property. */
  public static final String CHANNEL_PROVIDER_PROPERTY_NAME = "channelProvider";

  public static final String ENABLE_CHANNEL_PROVIDER_SYSTEM_PROPERTY = "ENABLE_CHANNEL_PROVIDER";
  /** Custom user agent string is only for other Google libraries. */
  private static final String USER_AGENT_PROPERTY_NAME = "userAgent";
  /** Query optimizer version to use for a connection. */
  private static final String OPTIMIZER_VERSION_PROPERTY_NAME = "optimizerVersion";
  /** Query optimizer statistics package to use for a connection. */
  private static final String OPTIMIZER_STATISTICS_PACKAGE_PROPERTY_NAME =
      "optimizerStatisticsPackage";
  /** Name of the 'lenientMode' connection property. */
  public static final String LENIENT_PROPERTY_NAME = "lenient";
  /** Name of the 'rpcPriority' connection property. */
  public static final String RPC_PRIORITY_NAME = "rpcPriority";

  public static final String DDL_IN_TRANSACTION_MODE_PROPERTY_NAME = "ddlInTransactionMode";
  /** Dialect to use for a connection. */
  private static final String DIALECT_PROPERTY_NAME = "dialect";
  /** Name of the 'databaseRole' connection property. */
  public static final String DATABASE_ROLE_PROPERTY_NAME = "databaseRole";
  /** Name of the 'delay transaction start until first write' property. */
  public static final String DELAY_TRANSACTION_START_UNTIL_FIRST_WRITE_NAME =
      "delayTransactionStartUntilFirstWrite";
  /** Name of the 'trackStackTraceOfSessionCheckout' connection property. */
  public static final String TRACK_SESSION_LEAKS_PROPERTY_NAME = "trackSessionLeaks";
  /** Name of the 'trackStackTraceOfConnectionCreation' connection property. */
  public static final String TRACK_CONNECTION_LEAKS_PROPERTY_NAME = "trackConnectionLeaks";

  public static final String DATA_BOOST_ENABLED_PROPERTY_NAME = "dataBoostEnabled";
  public static final String AUTO_PARTITION_MODE_PROPERTY_NAME = "autoPartitionMode";
  public static final String MAX_PARTITIONS_PROPERTY_NAME = "maxPartitions";
  public static final String MAX_PARTITIONED_PARALLELISM_PROPERTY_NAME =
      "maxPartitionedParallelism";

  public static final String ENABLE_EXTENDED_TRACING_PROPERTY_NAME = "enableExtendedTracing";
  public static final String ENABLE_API_TRACING_PROPERTY_NAME = "enableApiTracing";

  private static final String GUARDED_CONNECTION_PROPERTY_ERROR_MESSAGE =
      "%s can only be used if the system property %s has been set to true. "
          + "Start the application with the JVM command line option -D%s=true";

  private static String generateGuardedConnectionPropertyError(
      String systemPropertyName, String connectionPropertyName) {
    return String.format(
        GUARDED_CONNECTION_PROPERTY_ERROR_MESSAGE,
        connectionPropertyName,
        systemPropertyName,
        systemPropertyName);
  }

  /** All valid connection properties. */
  public static final Set<ConnectionProperty> VALID_PROPERTIES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  ConnectionProperty.createBooleanProperty(
                      AUTOCOMMIT_PROPERTY_NAME,
                      "Should the connection start in autocommit (true/false)",
                      DEFAULT_AUTOCOMMIT),
                  ConnectionProperty.createBooleanProperty(
                      READONLY_PROPERTY_NAME,
                      "Should the connection start in read-only mode (true/false)",
                      DEFAULT_READONLY),
                  ConnectionProperty.createBooleanProperty(
                      ROUTE_TO_LEADER_PROPERTY_NAME,
                      "Should read/write transactions and partitioned DML be routed to leader region (true/false)",
                      DEFAULT_ROUTE_TO_LEADER),
                  ConnectionProperty.createBooleanProperty(
                      RETRY_ABORTS_INTERNALLY_PROPERTY_NAME,
                      "Should the connection automatically retry Aborted errors (true/false)",
                      DEFAULT_RETRY_ABORTS_INTERNALLY),
                  ConnectionProperty.createBooleanProperty(
                      USE_VIRTUAL_THREADS_PROPERTY_NAME,
                      "Use a virtual thread instead of a platform thread for each connection (true/false). "
                          + "This option only has any effect if the application is running on Java 21 or higher. In all other cases, the option is ignored.",
                      DEFAULT_USE_VIRTUAL_THREADS),
                  ConnectionProperty.createBooleanProperty(
                      USE_VIRTUAL_GRPC_TRANSPORT_THREADS_PROPERTY_NAME,
                      "Use a virtual thread instead of a platform thread for the gRPC executor (true/false). "
                          + "This option only has any effect if the application is running on Java 21 or higher. In all other cases, the option is ignored.",
                      DEFAULT_USE_VIRTUAL_GRPC_TRANSPORT_THREADS),
                  ConnectionProperty.createStringProperty(
                      CREDENTIALS_PROPERTY_NAME,
                      "The location of the credentials file to use for this connection. If neither this property or encoded credentials are set, the connection will use the default Google Cloud credentials for the runtime environment."),
                  ConnectionProperty.createStringProperty(
                      ENCODED_CREDENTIALS_PROPERTY_NAME,
                      "Base64-encoded credentials to use for this connection. If neither this property or a credentials location are set, the connection will use the default Google Cloud credentials for the runtime environment."),
                  ConnectionProperty.createStringProperty(
                      CREDENTIALS_PROVIDER_PROPERTY_NAME,
                      "The class name of the com.google.api.gax.core.CredentialsProvider implementation that should be used to obtain credentials for connections."),
                  ConnectionProperty.createStringProperty(
                      OAUTH_TOKEN_PROPERTY_NAME,
                      "A valid pre-existing OAuth token to use for authentication for this connection. Setting this property will take precedence over any value set for a credentials file."),
                  ConnectionProperty.createStringProperty(
                      MIN_SESSIONS_PROPERTY_NAME,
                      "The minimum number of sessions in the backing session pool. The default is 100."),
                  ConnectionProperty.createStringProperty(
                      MAX_SESSIONS_PROPERTY_NAME,
                      "The maximum number of sessions in the backing session pool. The default is 400."),
                  ConnectionProperty.createStringProperty(
                      NUM_CHANNELS_PROPERTY_NAME,
                      "The number of gRPC channels to use to communicate with Cloud Spanner. The default is 4."),
                  ConnectionProperty.createStringProperty(
                      ENDPOINT_PROPERTY_NAME,
                      "The endpoint that the JDBC driver should connect to. "
                          + "The default is the default Spanner production endpoint when autoConfigEmulator=false, "
                          + "and the default Spanner emulator endpoint (localhost:9010) when autoConfigEmulator=true. "
                          + "This property takes precedence over any host name at the start of the connection URL."),
                  ConnectionProperty.createStringProperty(
                      CHANNEL_PROVIDER_PROPERTY_NAME,
                      "The name of the channel provider class. The name must reference an implementation of ExternalChannelProvider. If this property is not set, the connection will use the default grpc channel provider."),
                  ConnectionProperty.createBooleanProperty(
                      USE_PLAIN_TEXT_PROPERTY_NAME,
                      "Use a plain text communication channel (i.e. non-TLS) for communicating with the server (true/false). Set this value to true for communication with the Cloud Spanner emulator.",
                      DEFAULT_USE_PLAIN_TEXT),
                  ConnectionProperty.createStringProperty(
                      USER_AGENT_PROPERTY_NAME,
                      "The custom user-agent property name to use when communicating with Cloud Spanner. This property is intended for internal library usage, and should not be set by applications."),
                  ConnectionProperty.createStringProperty(
                      OPTIMIZER_VERSION_PROPERTY_NAME,
                      "Sets the default query optimizer version to use for this connection."),
                  ConnectionProperty.createStringProperty(
                      OPTIMIZER_STATISTICS_PACKAGE_PROPERTY_NAME, ""),
                  ConnectionProperty.createBooleanProperty("returnCommitStats", "", false),
                  ConnectionProperty.createStringProperty(
                      "maxCommitDelay",
                      "The maximum commit delay in milliseconds that should be applied to commit requests from this connection."),
                  ConnectionProperty.createBooleanProperty(
                      "autoConfigEmulator",
                      "Automatically configure the connection to try to connect to the Cloud Spanner emulator (true/false). "
                          + "The instance and database in the connection string will automatically be created if these do not yet exist on the emulator. "
                          + "Add dialect=postgresql to the connection string to make sure that the database that is created uses the PostgreSQL dialect.",
                      false),
                  ConnectionProperty.createBooleanProperty(
                      LENIENT_PROPERTY_NAME,
                      "Silently ignore unknown properties in the connection string/properties (true/false)",
                      DEFAULT_LENIENT),
                  ConnectionProperty.createStringProperty(
                      RPC_PRIORITY_NAME,
                      "Sets the priority for all RPC invocations from this connection (HIGH/MEDIUM/LOW). The default is HIGH."),
                  ConnectionProperty.createStringProperty(
                      DDL_IN_TRANSACTION_MODE_PROPERTY_NAME,
                      "Sets the behavior of a connection when a DDL statement is executed in a read/write transaction. The default is "
                          + DEFAULT_DDL_IN_TRANSACTION_MODE
                          + "."),
                  ConnectionProperty.createStringProperty(
                      DIALECT_PROPERTY_NAME,
                      "Sets the dialect to use for new databases that are created by this connection."),
                  ConnectionProperty.createStringProperty(
                      DATABASE_ROLE_PROPERTY_NAME,
                      "Sets the database role to use for this connection. The default is privileges assigned to IAM role"),
                  ConnectionProperty.createBooleanProperty(
                      DELAY_TRANSACTION_START_UNTIL_FIRST_WRITE_NAME,
                      "Enabling this option will delay the actual start of a read/write transaction until the first write operation is seen in that transaction. "
                          + "All reads that happen before the first write in a transaction will instead be executed as if the connection was in auto-commit mode. "
                          + "Enabling this option will make read/write transactions lose their SERIALIZABLE isolation level. Read operations that are executed after "
                          + "the first write operation in a read/write transaction will be executed using the read/write transaction. Enabling this mode can reduce locking "
                          + "and improve performance for applications that can handle the lower transaction isolation semantics.",
                      DEFAULT_DELAY_TRANSACTION_START_UNTIL_FIRST_WRITE),
                  ConnectionProperty.createBooleanProperty(
                      TRACK_SESSION_LEAKS_PROPERTY_NAME,
                      "Capture the call stack of the thread that checked out a session of the session pool. This will "
                          + "pre-create a LeakedSessionException already when a session is checked out. This can be disabled, "
                          + "for example if a monitoring system logs the pre-created exception. "
                          + "If disabled, the LeakedSessionException will only be created when an "
                          + "actual session leak is detected. The stack trace of the exception will "
                          + "in that case not contain the call stack of when the session was checked out.",
                      DEFAULT_TRACK_SESSION_LEAKS),
                  ConnectionProperty.createBooleanProperty(
                      TRACK_CONNECTION_LEAKS_PROPERTY_NAME,
                      "Capture the call stack of the thread that created a connection. This will "
                          + "pre-create a LeakedConnectionException already when a connection is created. "
                          + "This can be disabled, for example if a monitoring system logs the pre-created exception. "
                          + "If disabled, the LeakedConnectionException will only be created when an "
                          + "actual connection leak is detected. The stack trace of the exception will "
                          + "in that case not contain the call stack of when the connection was created.",
                      DEFAULT_TRACK_CONNECTION_LEAKS),
                  ConnectionProperty.createBooleanProperty(
                      DATA_BOOST_ENABLED_PROPERTY_NAME,
                      "Enable data boost for all partitioned queries that are executed by this connection. "
                          + "This setting is only used for partitioned queries and is ignored by all other statements.",
                      DEFAULT_DATA_BOOST_ENABLED),
                  ConnectionProperty.createBooleanProperty(
                      AUTO_PARTITION_MODE_PROPERTY_NAME,
                      "Execute all queries on this connection as partitioned queries. "
                          + "Executing a query that cannot be partitioned will fail. "
                          + "Executing a query in a read/write transaction will also fail.",
                      DEFAULT_AUTO_PARTITION_MODE),
                  ConnectionProperty.createIntProperty(
                      MAX_PARTITIONS_PROPERTY_NAME,
                      "The max partitions hint value to use for partitioned queries. "
                          + "Use 0 if you do not want to specify a hint.",
                      DEFAULT_MAX_PARTITIONS),
                  ConnectionProperty.createIntProperty(
                      MAX_PARTITIONED_PARALLELISM_PROPERTY_NAME,
                      "The maximum number of partitions that will be executed in parallel "
                          + "for partitioned queries on this connection. Set this value to 0 to "
                          + "dynamically use the number of processors available in the runtime.",
                      DEFAULT_MAX_PARTITIONED_PARALLELISM),
                  ConnectionProperty.createBooleanProperty(
                      ENABLE_EXTENDED_TRACING_PROPERTY_NAME,
                      "Include the SQL string in the OpenTelemetry traces that are generated "
                          + "by this connection. The SQL string is added as the standard OpenTelemetry "
                          + "attribute 'db.statement'.",
                      DEFAULT_ENABLE_EXTENDED_TRACING),
                  ConnectionProperty.createBooleanProperty(
                      ENABLE_API_TRACING_PROPERTY_NAME,
                      "Add OpenTelemetry traces for each individual RPC call. Enable this "
                          + "to get a detailed view of each RPC that is being executed by your application, "
                          + "or if you want to debug potential latency problems caused by RPCs that are "
                          + "being retried.",
                      DEFAULT_ENABLE_API_TRACING))));

  private static final Set<ConnectionProperty> INTERNAL_PROPERTIES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Collections.singletonList(
                  ConnectionProperty.createStringProperty(USER_AGENT_PROPERTY_NAME, ""))));
  private static final Set<ConnectionProperty> INTERNAL_VALID_PROPERTIES =
      Sets.union(VALID_PROPERTIES, INTERNAL_PROPERTIES);

  /**
   * Gets the default project-id for the current environment as defined by {@link
   * ServiceOptions#getDefaultProjectId()}, and if none could be found, the project-id of the given
   * credentials if it contains any.
   *
   * @param credentials The credentials to use to get the default project-id if none could be found
   *     in the environment.
   * @return the default project-id.
   */
  public static String getDefaultProjectId(Credentials credentials) {
    String projectId = SpannerOptions.getDefaultProjectId();
    if (projectId == null
        && credentials != null
        && credentials instanceof ServiceAccountCredentials) {
      projectId = ((ServiceAccountCredentials) credentials).getProjectId();
    }
    return projectId;
  }

  /**
   * Closes <strong>all</strong> {@link Spanner} instances that have been opened by connections
   * during the lifetime of this JVM. Call this method at the end of your application to free up
   * resources. You must close all {@link Connection}s that have been opened by your application
   * before calling this method. Failing to do so, will cause this method to throw a {@link
   * SpannerException}.
   *
   * <p>This method is also automatically called by a shutdown hook (see {@link
   * Runtime#addShutdownHook(Thread)}) when the JVM is shutdown gracefully.
   */
  public static void closeSpanner() {
    SpannerPool.INSTANCE.checkAndCloseSpanners();
  }

  /**
   * {@link SpannerOptionsConfigurator} can be used to add additional configuration for a {@link
   * Spanner} instance. Intended for tests.
   */
  @VisibleForTesting
  interface SpannerOptionsConfigurator {
    void configure(SpannerOptions.Builder options);
  }

  /**
   * {@link ExternalChannelProvider} can be used for to specify an external channel provider. This
   * is needed if you require different certificates than those provided by the standard grpc
   * channel provider.
   */
  public interface ExternalChannelProvider {
    TransportChannelProvider getChannelProvider(String host, int port);
  }

  /** Builder for {@link ConnectionOptions} instances. */
  public static class Builder {
    private String uri;
    private String credentialsUrl;
    private String oauthToken;
    private Credentials credentials;
    private SessionPoolOptions sessionPoolOptions;
    private List<StatementExecutionInterceptor> statementExecutionInterceptors =
        Collections.emptyList();
    private SpannerOptionsConfigurator configurator;
    private OpenTelemetry openTelemetry;
    private String tracingPrefix;

    private Builder() {}

    /** Spanner {@link ConnectionOptions} URI format. */
    public static final String SPANNER_URI_FORMAT =
        "(?:cloudspanner:)(?<HOSTGROUP>//[\\w.-]+(?:\\.[\\w\\.-]+)*[\\w\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=.]+)?/projects/(?<PROJECTGROUP>(([a-z]|[-.:]|[0-9])+|(DEFAULT_PROJECT_ID)))(/instances/(?<INSTANCEGROUP>([a-z]|[-]|[0-9])+)(/databases/(?<DATABASEGROUP>([a-z]|[-]|[_]|[0-9])+))?)?(?:[?|;].*)?";

    private static final String SPANNER_URI_REGEX = "(?is)^" + SPANNER_URI_FORMAT + "$";

    @VisibleForTesting
    static final Pattern SPANNER_URI_PATTERN = Pattern.compile(SPANNER_URI_REGEX);

    private static final String HOST_GROUP = "HOSTGROUP";
    private static final String PROJECT_GROUP = "PROJECTGROUP";
    private static final String INSTANCE_GROUP = "INSTANCEGROUP";
    private static final String DATABASE_GROUP = "DATABASEGROUP";
    private static final String DEFAULT_PROJECT_ID_PLACEHOLDER = "DEFAULT_PROJECT_ID";

    private boolean isValidUri(String uri) {
      return SPANNER_URI_PATTERN.matcher(uri).matches();
    }

    /**
     * Sets the URI of the Cloud Spanner database to connect to. A connection URI must be specified
     * in this format:
     *
     * <pre>
     * cloudspanner:[//host[:port]]/projects/project-id[/instances/instance-id[/databases/database-name]][\?property-name=property-value[;property-name=property-value]*]?
     * </pre>
     *
     * The property-value strings should be url-encoded.
     *
     * <p>The project-id part of the URI may be filled with the placeholder DEFAULT_PROJECT_ID. This
     * placeholder will be replaced by the default project id of the environment that is requesting
     * a connection.
     *
     * <p>The supported properties are:
     *
     * <ul>
     *   <li>credentials (String): URL for the credentials file to use for the connection. This
     *       property is only used if no credentials have been specified using the {@link
     *       ConnectionOptions.Builder#setCredentialsUrl(String)} method. If you do not specify any
     *       credentials at all, the default credentials of the environment as returned by {@link
     *       GoogleCredentials#getApplicationDefault()} will be used.
     *   <li>encodedCredentials (String): A Base64 encoded string containing the Google credentials
     *       to use. You should only set either this property or the `credentials` (file location)
     *       property, but not both at the same time.
     *   <li>credentialsProvider (String): Class name of the {@link
     *       com.google.api.gax.core.CredentialsProvider} that should be used to get credentials for
     *       a connection that is created by this {@link ConnectionOptions}. The credentials will be
     *       retrieved from the {@link com.google.api.gax.core.CredentialsProvider} when a new
     *       connection is created. A connection will use the credentials that were obtained at
     *       creation during its lifetime.
     *   <li>autocommit (boolean): Sets the initial autocommit mode for the connection. Default is
     *       true.
     *   <li>readonly (boolean): Sets the initial readonly mode for the connection. Default is
     *       false.
     *   <li>minSessions (int): Sets the minimum number of sessions in the backing session pool.
     *   <li>maxSessions (int): Sets the maximum number of sessions in the backing session pool.
     *   <li>numChannels (int): Sets the number of gRPC channels to use for the connection.
     *   <li>retryAbortsInternally (boolean): Sets the initial retryAbortsInternally mode for the
     *       connection. Default is true.
     *   <li>optimizerVersion (string): Sets the query optimizer version to use for the connection.
     *   <li>autoConfigEmulator (boolean): Automatically configures the connection to connect to the
     *       Cloud Spanner emulator. If no host and port is specified in the connection string, the
     *       connection will automatically use the default emulator host/port combination
     *       (localhost:9010). Plain text communication will be enabled and authentication will be
     *       disabled. The instance and database in the connection string will automatically be
     *       created on the emulator if any of them do not yet exist. Any existing instance or
     *       database on the emulator will remain untouched. No other configuration is needed in
     *       order to connect to the emulator than setting this property.
     *   <li>routeToLeader (boolean): Sets the routeToLeader flag to route requests to leader (true)
     *       or any region (false) in read/write transactions and Partitioned DML. Default is true.
     * </ul>
     *
     * @param uri The URI of the Spanner database to connect to.
     * @return this builder
     */
    public Builder setUri(String uri) {
      Preconditions.checkArgument(
          isValidUri(uri),
          "The specified URI is not a valid Cloud Spanner connection URI. Please specify a URI in the format \"cloudspanner:[//host[:port]]/projects/project-id[/instances/instance-id[/databases/database-name]][\\?property-name=property-value[;property-name=property-value]*]?\"");
      checkValidProperties(uri);
      this.uri = uri;
      return this;
    }

    /** Sets the {@link SessionPoolOptions} to use for the connection. */
    public Builder setSessionPoolOptions(SessionPoolOptions sessionPoolOptions) {
      Preconditions.checkNotNull(sessionPoolOptions);
      this.sessionPoolOptions = sessionPoolOptions;
      return this;
    }

    /**
     * Sets the URL of the credentials file to use for this connection. The URL may be a reference
     * to a file on the local file system, or to a file on Google Cloud Storage. References to
     * Google Cloud Storage files are only allowed when the application is running on Google Cloud
     * and the environment has access to the specified storage location. It also requires that the
     * Google Cloud Storage client library is present on the class path. The Google Cloud Storage
     * library is not automatically added as a dependency by the JDBC driver.
     *
     * <p>If you do not specify a credentialsUrl (either by using this setter, or by specifying on
     * the connection URI), the credentials returned by {@link
     * GoogleCredentials#getApplicationDefault()} will be used for the connection.
     *
     * @param credentialsUrl A valid file or Google Cloud Storage URL for the credentials file to be
     *     used.
     * @return this builder
     */
    public Builder setCredentialsUrl(String credentialsUrl) {
      this.credentialsUrl = credentialsUrl;
      return this;
    }

    /**
     * Sets the OAuth token to use with this connection. The token must be a valid token with access
     * to the resources (project/instance/database) that the connection will be accessing. This
     * authentication method cannot be used in combination with a credentials file. If both an OAuth
     * token and a credentials file is specified, the {@link #build()} method will throw an
     * exception.
     *
     * @param oauthToken A valid OAuth token for the Google Cloud project that is used by this
     *     connection.
     * @return this builder
     */
    public Builder setOAuthToken(String oauthToken) {
      this.oauthToken = oauthToken;
      return this;
    }

    @VisibleForTesting
    Builder setStatementExecutionInterceptors(List<StatementExecutionInterceptor> interceptors) {
      this.statementExecutionInterceptors = interceptors;
      return this;
    }

    @VisibleForTesting
    Builder setConfigurator(SpannerOptionsConfigurator configurator) {
      this.configurator = Preconditions.checkNotNull(configurator);
      return this;
    }

    @VisibleForTesting
    Builder setCredentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public Builder setOpenTelemetry(OpenTelemetry openTelemetry) {
      this.openTelemetry = openTelemetry;
      return this;
    }

    public Builder setTracingPrefix(String tracingPrefix) {
      this.tracingPrefix = tracingPrefix;
      return this;
    }

    /** @return the {@link ConnectionOptions} */
    public ConnectionOptions build() {
      Preconditions.checkState(this.uri != null, "Connection URI is required");
      return new ConnectionOptions(this);
    }
  }

  /**
   * Create a {@link Builder} for {@link ConnectionOptions}. Use this method to create {@link
   * ConnectionOptions} that can be used to obtain a {@link Connection}.
   *
   * @return a new {@link Builder}
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  private final String uri;
  private final String warnings;
  private final String credentialsUrl;
  private final String encodedCredentials;
  private final CredentialsProvider credentialsProvider;
  private final String oauthToken;
  private final Credentials fixedCredentials;

  private final boolean usePlainText;
  private final String host;
  private final String projectId;
  private final String instanceId;
  private final String databaseName;
  private final Credentials credentials;
  private final SessionPoolOptions sessionPoolOptions;
  private final Integer numChannels;
  private final String channelProvider;
  private final Integer minSessions;
  private final Integer maxSessions;
  private final String databaseRole;
  private final String userAgent;
  private final QueryOptions queryOptions;
  private final boolean returnCommitStats;
  private final Long maxCommitDelay;
  private final boolean autoConfigEmulator;
  private final Dialect dialect;
  private final RpcPriority rpcPriority;
  private final DdlInTransactionMode ddlInTransactionMode;
  private final boolean delayTransactionStartUntilFirstWrite;
  private final boolean trackSessionLeaks;
  private final boolean trackConnectionLeaks;

  private final boolean dataBoostEnabled;
  private final boolean autoPartitionMode;
  private final int maxPartitions;
  private final int maxPartitionedParallelism;

  private final boolean autocommit;
  private final boolean readOnly;
  private final boolean routeToLeader;
  private final boolean retryAbortsInternally;
  private final boolean useVirtualThreads;
  private final boolean useVirtualGrpcTransportThreads;
  private final OpenTelemetry openTelemetry;
  private final String tracingPrefix;
  private final Boolean enableExtendedTracing;
  private final Boolean enableApiTracing;
  private final List<StatementExecutionInterceptor> statementExecutionInterceptors;
  private final SpannerOptionsConfigurator configurator;

  private ConnectionOptions(Builder builder) {
    Matcher matcher = Builder.SPANNER_URI_PATTERN.matcher(builder.uri);
    Preconditions.checkArgument(
        matcher.find(), String.format("Invalid connection URI specified: %s", builder.uri));
    this.warnings = checkValidProperties(builder.uri);

    this.uri = builder.uri;
    this.credentialsUrl =
        builder.credentialsUrl != null ? builder.credentialsUrl : parseCredentials(builder.uri);
    this.encodedCredentials = parseEncodedCredentials(builder.uri);
    this.credentialsProvider = parseCredentialsProvider(builder.uri);
    this.oauthToken =
        builder.oauthToken != null ? builder.oauthToken : parseOAuthToken(builder.uri);
    // Check that at most one of credentials location, encoded credentials, credentials provider and
    // OUAuth token has been specified in the connection URI.
    Preconditions.checkArgument(
        Stream.of(
                    this.credentialsUrl,
                    this.encodedCredentials,
                    this.credentialsProvider,
                    this.oauthToken)
                .filter(Objects::nonNull)
                .count()
            <= 1,
        "Specify only one of credentialsUrl, encodedCredentials, credentialsProvider and OAuth token");
    this.fixedCredentials = builder.credentials;

    this.userAgent = parseUserAgent(this.uri);
    QueryOptions.Builder queryOptionsBuilder = QueryOptions.newBuilder();
    queryOptionsBuilder.setOptimizerVersion(parseOptimizerVersion(this.uri));
    queryOptionsBuilder.setOptimizerStatisticsPackage(parseOptimizerStatisticsPackage(this.uri));
    this.queryOptions = queryOptionsBuilder.build();
    this.returnCommitStats = parseReturnCommitStats(this.uri);
    this.maxCommitDelay = parseMaxCommitDelay(this.uri);
    this.autoConfigEmulator = parseAutoConfigEmulator(this.uri);
    this.dialect = parseDialect(this.uri);
    this.usePlainText = this.autoConfigEmulator || parseUsePlainText(this.uri);
    this.host =
        determineHost(
            matcher, parseEndpoint(this.uri), autoConfigEmulator, usePlainText, System.getenv());
    this.rpcPriority = parseRPCPriority(this.uri);
    this.ddlInTransactionMode = parseDdlInTransactionMode(this.uri);
    this.delayTransactionStartUntilFirstWrite = parseDelayTransactionStartUntilFirstWrite(this.uri);
    this.trackSessionLeaks = parseTrackSessionLeaks(this.uri);
    this.trackConnectionLeaks = parseTrackConnectionLeaks(this.uri);

    this.dataBoostEnabled = parseDataBoostEnabled(this.uri);
    this.autoPartitionMode = parseAutoPartitionMode(this.uri);
    this.maxPartitions = parseMaxPartitions(this.uri);
    this.maxPartitionedParallelism = parseMaxPartitionedParallelism(this.uri);

    this.instanceId = matcher.group(Builder.INSTANCE_GROUP);
    this.databaseName = matcher.group(Builder.DATABASE_GROUP);
    // Using credentials on a plain text connection is not allowed, so if the user has not specified
    // any credentials and is using a plain text connection, we should not try to get the
    // credentials from the environment, but default to NoCredentials.
    if (this.fixedCredentials == null
        && this.credentialsUrl == null
        && this.encodedCredentials == null
        && this.credentialsProvider == null
        && this.oauthToken == null
        && this.usePlainText) {
      this.credentials = NoCredentials.getInstance();
    } else if (this.oauthToken != null) {
      this.credentials = new GoogleCredentials(new AccessToken(oauthToken, null));
    } else if (this.credentialsProvider != null) {
      try {
        this.credentials = this.credentialsProvider.getCredentials();
      } catch (IOException exception) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            "Failed to get credentials from CredentialsProvider: " + exception.getMessage(),
            exception);
      }
    } else if (this.fixedCredentials != null) {
      this.credentials = fixedCredentials;
    } else if (this.encodedCredentials != null) {
      this.credentials = getCredentialsService().decodeCredentials(this.encodedCredentials);
    } else {
      this.credentials = getCredentialsService().createCredentials(this.credentialsUrl);
    }
    this.minSessions =
        parseIntegerProperty(MIN_SESSIONS_PROPERTY_NAME, parseMinSessions(builder.uri));
    this.maxSessions =
        parseIntegerProperty(MAX_SESSIONS_PROPERTY_NAME, parseMaxSessions(builder.uri));
    this.numChannels =
        parseIntegerProperty(NUM_CHANNELS_PROPERTY_NAME, parseNumChannels(builder.uri));
    this.channelProvider = parseChannelProvider(builder.uri);
    this.databaseRole = parseDatabaseRole(this.uri);

    String projectId = matcher.group(Builder.PROJECT_GROUP);
    if (Builder.DEFAULT_PROJECT_ID_PLACEHOLDER.equalsIgnoreCase(projectId)) {
      projectId = getDefaultProjectId(this.credentials);
    }
    this.projectId = projectId;

    this.autocommit = parseAutocommit(this.uri);
    this.readOnly = parseReadOnly(this.uri);
    this.routeToLeader = parseRouteToLeader(this.uri);
    this.retryAbortsInternally = parseRetryAbortsInternally(this.uri);
    this.useVirtualThreads = parseUseVirtualThreads(this.uri);
    this.useVirtualGrpcTransportThreads = parseUseVirtualGrpcTransportThreads(this.uri);
    this.openTelemetry = builder.openTelemetry;
    this.tracingPrefix = builder.tracingPrefix;
    this.enableExtendedTracing = parseEnableExtendedTracing(this.uri);
    this.enableApiTracing = parseEnableApiTracing(this.uri);
    this.statementExecutionInterceptors =
        Collections.unmodifiableList(builder.statementExecutionInterceptors);
    this.configurator = builder.configurator;

    if (this.minSessions != null || this.maxSessions != null || !this.trackSessionLeaks) {
      SessionPoolOptions.Builder sessionPoolOptionsBuilder =
          builder.sessionPoolOptions == null
              ? SessionPoolOptions.newBuilder()
              : builder.sessionPoolOptions.toBuilder();
      sessionPoolOptionsBuilder.setTrackStackTraceOfSessionCheckout(this.trackSessionLeaks);
      sessionPoolOptionsBuilder.setAutoDetectDialect(true);
      if (this.minSessions != null) {
        sessionPoolOptionsBuilder.setMinSessions(this.minSessions);
      }
      if (this.maxSessions != null) {
        sessionPoolOptionsBuilder.setMaxSessions(this.maxSessions);
      }
      this.sessionPoolOptions = sessionPoolOptionsBuilder.build();
    } else if (builder.sessionPoolOptions != null) {
      this.sessionPoolOptions = builder.sessionPoolOptions;
    } else {
      this.sessionPoolOptions = SessionPoolOptions.newBuilder().setAutoDetectDialect(true).build();
    }
  }

  @VisibleForTesting
  static String determineHost(
      Matcher matcher,
      String endpoint,
      boolean autoConfigEmulator,
      boolean usePlainText,
      Map<String, String> environment) {
    String host;
    if (Objects.equals(endpoint, DEFAULT_ENDPOINT) && matcher.group(Builder.HOST_GROUP) == null) {
      if (autoConfigEmulator) {
        if (Strings.isNullOrEmpty(environment.get(SPANNER_EMULATOR_HOST_ENV_VAR))) {
          return DEFAULT_EMULATOR_HOST;
        } else {
          return PLAIN_TEXT_PROTOCOL + "//" + environment.get(SPANNER_EMULATOR_HOST_ENV_VAR);
        }
      } else {
        return DEFAULT_HOST;
      }
    } else if (!Objects.equals(endpoint, DEFAULT_ENDPOINT)) {
      // Add '//' at the start of the endpoint to conform to the standard URL specification.
      host = "//" + endpoint;
    } else {
      // The leading '//' is already included in the regex for the connection URL, so we don't need
      // to add the leading '//' to the host name here.
      host = matcher.group(Builder.HOST_GROUP);
    }
    if (usePlainText) {
      return PLAIN_TEXT_PROTOCOL + host;
    }
    return HOST_PROTOCOL + host;
  }

  private static Integer parseIntegerProperty(String propertyName, String value) {
    if (value != null) {
      try {
        return Integer.valueOf(value);
      } catch (NumberFormatException e) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            String.format("Invalid %s value specified: %s", propertyName, value),
            e);
      }
    }
    return null;
  }

  /**
   * @return an instance of OpenTelemetry. If OpenTelemetry object is not set then <code>null</code>
   *     will be returned.
   */
  OpenTelemetry getOpenTelemetry() {
    return this.openTelemetry;
  }

  /**
   * @return The prefix that will be added to all traces that are started by the Connection API.
   *     This property is used by for example the JDBC driver to make sure all traces start with
   *     CloudSpannerJdbc.
   */
  String getTracingPrefix() {
    return this.tracingPrefix;
  }

  SpannerOptionsConfigurator getConfigurator() {
    return configurator;
  }

  @VisibleForTesting
  CredentialsService getCredentialsService() {
    return CredentialsService.INSTANCE;
  }

  @VisibleForTesting
  static boolean parseUsePlainText(String uri) {
    String value = parseUriProperty(uri, USE_PLAIN_TEXT_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_USE_PLAIN_TEXT;
  }

  @VisibleForTesting
  static boolean parseAutocommit(String uri) {
    String value = parseUriProperty(uri, AUTOCOMMIT_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_AUTOCOMMIT;
  }

  @VisibleForTesting
  static boolean parseReadOnly(String uri) {
    String value = parseUriProperty(uri, READONLY_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_READONLY;
  }

  static boolean parseRouteToLeader(String uri) {
    String value = parseUriProperty(uri, ROUTE_TO_LEADER_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_ROUTE_TO_LEADER;
  }

  @VisibleForTesting
  static boolean parseRetryAbortsInternally(String uri) {
    String value = parseUriProperty(uri, RETRY_ABORTS_INTERNALLY_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_RETRY_ABORTS_INTERNALLY;
  }

  @VisibleForTesting
  static boolean parseUseVirtualThreads(String uri) {
    String value = parseUriProperty(uri, USE_VIRTUAL_THREADS_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_USE_VIRTUAL_THREADS;
  }

  @VisibleForTesting
  static boolean parseUseVirtualGrpcTransportThreads(String uri) {
    String value = parseUriProperty(uri, USE_VIRTUAL_GRPC_TRANSPORT_THREADS_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_USE_VIRTUAL_GRPC_TRANSPORT_THREADS;
  }

  @VisibleForTesting
  static @Nullable String parseCredentials(String uri) {
    String value = parseUriProperty(uri, CREDENTIALS_PROPERTY_NAME);
    return value != null ? value : DEFAULT_CREDENTIALS;
  }

  @VisibleForTesting
  static @Nullable String parseEncodedCredentials(String uri) {
    String encodedCredentials = parseUriProperty(uri, ENCODED_CREDENTIALS_PROPERTY_NAME);
    checkGuardedProperty(
        encodedCredentials,
        ENABLE_ENCODED_CREDENTIALS_SYSTEM_PROPERTY,
        ENCODED_CREDENTIALS_PROPERTY_NAME);
    return encodedCredentials;
  }

  @VisibleForTesting
  static @Nullable CredentialsProvider parseCredentialsProvider(String uri) {
    String credentialsProviderName = parseUriProperty(uri, CREDENTIALS_PROVIDER_PROPERTY_NAME);
    checkGuardedProperty(
        credentialsProviderName,
        ENABLE_CREDENTIALS_PROVIDER_SYSTEM_PROPERTY,
        CREDENTIALS_PROVIDER_PROPERTY_NAME);
    if (!Strings.isNullOrEmpty(credentialsProviderName)) {
      try {
        Class<? extends CredentialsProvider> clazz =
            (Class<? extends CredentialsProvider>) Class.forName(credentialsProviderName);
        Constructor<? extends CredentialsProvider> constructor = clazz.getDeclaredConstructor();
        return constructor.newInstance();
      } catch (ClassNotFoundException classNotFoundException) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            "Unknown or invalid CredentialsProvider class name: " + credentialsProviderName,
            classNotFoundException);
      } catch (NoSuchMethodException noSuchMethodException) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            "Credentials provider "
                + credentialsProviderName
                + " does not have a public no-arg constructor.",
            noSuchMethodException);
      } catch (InvocationTargetException
          | InstantiationException
          | IllegalAccessException exception) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            "Failed to create an instance of "
                + credentialsProviderName
                + ": "
                + exception.getMessage(),
            exception);
      }
    }
    return null;
  }

  private static void checkGuardedProperty(
      String value, String systemPropertyName, String connectionPropertyName) {
    if (!Strings.isNullOrEmpty(value)
        && !Boolean.parseBoolean(System.getProperty(systemPropertyName))) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.FAILED_PRECONDITION,
          generateGuardedConnectionPropertyError(systemPropertyName, connectionPropertyName));
    }
  }

  @VisibleForTesting
  static @Nullable String parseOAuthToken(String uri) {
    String value = parseUriProperty(uri, OAUTH_TOKEN_PROPERTY_NAME);
    return value != null ? value : DEFAULT_OAUTH_TOKEN;
  }

  @VisibleForTesting
  static String parseMinSessions(String uri) {
    String value = parseUriProperty(uri, MIN_SESSIONS_PROPERTY_NAME);
    return value != null ? value : DEFAULT_MIN_SESSIONS;
  }

  @VisibleForTesting
  static String parseMaxSessions(String uri) {
    String value = parseUriProperty(uri, MAX_SESSIONS_PROPERTY_NAME);
    return value != null ? value : DEFAULT_MAX_SESSIONS;
  }

  @VisibleForTesting
  static String parseNumChannels(String uri) {
    String value = parseUriProperty(uri, NUM_CHANNELS_PROPERTY_NAME);
    return value != null ? value : DEFAULT_NUM_CHANNELS;
  }

  private static String parseEndpoint(String uri) {
    String value = parseUriProperty(uri, ENDPOINT_PROPERTY_NAME);
    return value != null ? value : DEFAULT_ENDPOINT;
  }

  @VisibleForTesting
  static String parseChannelProvider(String uri) {
    String value = parseUriProperty(uri, CHANNEL_PROVIDER_PROPERTY_NAME);
    checkGuardedProperty(
        value, ENABLE_CHANNEL_PROVIDER_SYSTEM_PROPERTY, CHANNEL_PROVIDER_PROPERTY_NAME);
    return value != null ? value : DEFAULT_CHANNEL_PROVIDER;
  }

  @VisibleForTesting
  static String parseDatabaseRole(String uri) {
    String value = parseUriProperty(uri, DATABASE_ROLE_PROPERTY_NAME);
    return value != null ? value : DEFAULT_DATABASE_ROLE;
  }

  @VisibleForTesting
  static String parseUserAgent(String uri) {
    String value = parseUriProperty(uri, USER_AGENT_PROPERTY_NAME);
    return value != null ? value : DEFAULT_USER_AGENT;
  }

  @VisibleForTesting
  static String parseOptimizerVersion(String uri) {
    String value = parseUriProperty(uri, OPTIMIZER_VERSION_PROPERTY_NAME);
    return value != null ? value : DEFAULT_OPTIMIZER_VERSION;
  }

  @VisibleForTesting
  static String parseOptimizerStatisticsPackage(String uri) {
    String value = parseUriProperty(uri, OPTIMIZER_STATISTICS_PACKAGE_PROPERTY_NAME);
    return value != null ? value : DEFAULT_OPTIMIZER_STATISTICS_PACKAGE;
  }

  @VisibleForTesting
  static boolean parseReturnCommitStats(String uri) {
    String value = parseUriProperty(uri, "returnCommitStats");
    return Boolean.parseBoolean(value);
  }

  @VisibleForTesting
  static Long parseMaxCommitDelay(String uri) {
    String value = parseUriProperty(uri, "maxCommitDelay");
    try {
      Long millis = value == null ? null : Long.valueOf(value);
      if (millis != null && millis < 0L) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT, "maxCommitDelay must be >=0");
      }
      return millis;
    } catch (NumberFormatException numberFormatException) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          "Invalid value for maxCommitDelay: "
              + value
              + "\n"
              + "The value must be a positive integer indicating the number of "
              + "milliseconds to use as the max delay.");
    }
  }

  static boolean parseAutoConfigEmulator(String uri) {
    String value = parseUriProperty(uri, "autoConfigEmulator");
    return Boolean.parseBoolean(value);
  }

  @VisibleForTesting
  static Dialect parseDialect(String uri) {
    String value = parseUriProperty(uri, DIALECT_PROPERTY_NAME);
    return value != null ? Dialect.valueOf(value.toUpperCase()) : Dialect.GOOGLE_STANDARD_SQL;
  }

  @VisibleForTesting
  static boolean parseLenient(String uri) {
    String value = parseUriProperty(uri, LENIENT_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_LENIENT;
  }

  @VisibleForTesting
  static boolean parseDelayTransactionStartUntilFirstWrite(String uri) {
    String value = parseUriProperty(uri, DELAY_TRANSACTION_START_UNTIL_FIRST_WRITE_NAME);
    return value != null
        ? Boolean.parseBoolean(value)
        : DEFAULT_DELAY_TRANSACTION_START_UNTIL_FIRST_WRITE;
  }

  @VisibleForTesting
  static boolean parseTrackSessionLeaks(String uri) {
    String value = parseUriProperty(uri, TRACK_SESSION_LEAKS_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_TRACK_SESSION_LEAKS;
  }

  @VisibleForTesting
  static boolean parseTrackConnectionLeaks(String uri) {
    String value = parseUriProperty(uri, TRACK_CONNECTION_LEAKS_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_TRACK_CONNECTION_LEAKS;
  }

  @VisibleForTesting
  static boolean parseDataBoostEnabled(String uri) {
    String value = parseUriProperty(uri, DATA_BOOST_ENABLED_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_DATA_BOOST_ENABLED;
  }

  @VisibleForTesting
  static boolean parseAutoPartitionMode(String uri) {
    String value = parseUriProperty(uri, AUTO_PARTITION_MODE_PROPERTY_NAME);
    return value != null ? Boolean.parseBoolean(value) : DEFAULT_AUTO_PARTITION_MODE;
  }

  @VisibleForTesting
  static int parseMaxPartitions(String uri) {
    String stringValue = parseUriProperty(uri, MAX_PARTITIONS_PROPERTY_NAME);
    if (stringValue == null) {
      return DEFAULT_MAX_PARTITIONS;
    }
    try {
      int value = Integer.parseInt(stringValue);
      if (value < 0) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT, "maxPartitions must be >=0");
      }
      return value;
    } catch (NumberFormatException numberFormatException) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, "Invalid value for maxPartitions: " + stringValue);
    }
  }

  @VisibleForTesting
  static int parseMaxPartitionedParallelism(String uri) {
    String stringValue = parseUriProperty(uri, MAX_PARTITIONED_PARALLELISM_PROPERTY_NAME);
    if (stringValue == null) {
      return DEFAULT_MAX_PARTITIONED_PARALLELISM;
    }
    try {
      int value = Integer.parseInt(stringValue);
      if (value < 0) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT, "maxPartitionedParallelism must be >=0");
      }
      return value;
    } catch (NumberFormatException numberFormatException) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          "Invalid value for maxPartitionedParallelism: " + stringValue);
    }
  }

  @VisibleForTesting
  static RpcPriority parseRPCPriority(String uri) {
    String value = parseUriProperty(uri, RPC_PRIORITY_NAME);
    return value != null ? RpcPriority.valueOf(value) : DEFAULT_RPC_PRIORITY;
  }

  @VisibleForTesting
  static DdlInTransactionMode parseDdlInTransactionMode(String uri) {
    String value = parseUriProperty(uri, DDL_IN_TRANSACTION_MODE_PROPERTY_NAME);
    return value != null
        ? DdlInTransactionMode.valueOf(value.toUpperCase())
        : DEFAULT_DDL_IN_TRANSACTION_MODE;
  }

  @VisibleForTesting
  static Boolean parseEnableExtendedTracing(String uri) {
    String value = parseUriProperty(uri, ENABLE_EXTENDED_TRACING_PROPERTY_NAME);
    return value != null ? Boolean.valueOf(value) : DEFAULT_ENABLE_EXTENDED_TRACING;
  }

  @VisibleForTesting
  static Boolean parseEnableApiTracing(String uri) {
    String value = parseUriProperty(uri, ENABLE_API_TRACING_PROPERTY_NAME);
    return value != null ? Boolean.valueOf(value) : DEFAULT_ENABLE_API_TRACING;
  }

  @VisibleForTesting
  static String parseUriProperty(String uri, String property) {
    Pattern pattern = Pattern.compile(String.format("(?is)(?:;|\\?)%s=(.*?)(?:;|$)", property));
    Matcher matcher = pattern.matcher(uri);
    if (matcher.find() && matcher.groupCount() == 1) {
      return matcher.group(1);
    }
    return null;
  }

  /** Check that only valid properties have been specified. */
  @VisibleForTesting
  static String checkValidProperties(String uri) {
    String invalidProperties = "";
    List<String> properties = parseProperties(uri);
    boolean lenient = parseLenient(uri);
    for (String property : properties) {
      if (!INTERNAL_VALID_PROPERTIES.contains(ConnectionProperty.createEmptyProperty(property))) {
        if (invalidProperties.length() > 0) {
          invalidProperties = invalidProperties + ", ";
        }
        invalidProperties = invalidProperties + property;
      }
    }
    if (lenient) {
      return String.format("Invalid properties found in connection URI: %s", invalidProperties);
    } else {
      Preconditions.checkArgument(
          invalidProperties.isEmpty(),
          String.format(
              "Invalid properties found in connection URI. Add lenient=true to the connection string to ignore unknown properties. Invalid properties: %s",
              invalidProperties));
      return null;
    }
  }

  @VisibleForTesting
  static List<String> parseProperties(String uri) {
    Pattern pattern = Pattern.compile("(?is)(?:\\?|;)(?<PROPERTY>.*?)=(?:.*?)");
    Matcher matcher = pattern.matcher(uri);
    List<String> res = new ArrayList<>();
    while (matcher.find() && matcher.group("PROPERTY") != null) {
      res.add(matcher.group("PROPERTY"));
    }
    return res;
  }

  /**
   * Create a new {@link Connection} from this {@link ConnectionOptions}. Calling this method
   * multiple times for the same {@link ConnectionOptions} will return multiple instances of {@link
   * Connection}s to the same database.
   *
   * @return a new {@link Connection} to the database referenced by this {@link ConnectionOptions}
   */
  public Connection getConnection() {
    LOCAL_CONNECTION_CHECKER.checkLocalConnection(this);
    return new ConnectionImpl(this);
  }

  /** The URI of this {@link ConnectionOptions} */
  public String getUri() {
    return uri;
  }

  /** The credentials URL of this {@link ConnectionOptions} */
  public String getCredentialsUrl() {
    return credentialsUrl;
  }

  String getOAuthToken() {
    return this.oauthToken;
  }

  Credentials getFixedCredentials() {
    return this.fixedCredentials;
  }

  CredentialsProvider getCredentialsProvider() {
    return this.credentialsProvider;
  }

  /** The {@link SessionPoolOptions} of this {@link ConnectionOptions}. */
  public SessionPoolOptions getSessionPoolOptions() {
    return sessionPoolOptions;
  }

  /**
   * The minimum number of sessions in the backing session pool of this connection. The session pool
   * is shared between all connections in the same JVM that connect to the same Cloud Spanner
   * database using the same connection settings.
   */
  public Integer getMinSessions() {
    return minSessions;
  }

  /**
   * The maximum number of sessions in the backing session pool of this connection. The session pool
   * is shared between all connections in the same JVM that connect to the same Cloud Spanner
   * database using the same connection settings.
   */
  public Integer getMaxSessions() {
    return maxSessions;
  }

  /** The number of channels to use for the connection. */
  public Integer getNumChannels() {
    return numChannels;
  }

  /** Calls the getChannelProvider() method from the supplied class. */
  public TransportChannelProvider getChannelProvider() {
    if (channelProvider == null) {
      return null;
    }
    try {
      URL url = new URL(host);
      ExternalChannelProvider provider =
          ExternalChannelProvider.class.cast(Class.forName(channelProvider).newInstance());
      return provider.getChannelProvider(url.getHost(), url.getPort());
    } catch (Exception e) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          String.format(
              "%s : Failed to create channel with external provider: %s",
              e.toString(), channelProvider));
    }
  }

  /**
   * The database role that is used for this connection. Assigning a role to a connection can be
   * used to for example restrict the access of a connection to a specific set of tables.
   */
  public String getDatabaseRole() {
    return databaseRole;
  }

  /** The host and port number that this {@link ConnectionOptions} will connect to */
  public String getHost() {
    return host;
  }

  /** The Google Project ID that this {@link ConnectionOptions} will connect to */
  public String getProjectId() {
    return projectId;
  }

  /** The Spanner Instance ID that this {@link ConnectionOptions} will connect to */
  public String getInstanceId() {
    return instanceId;
  }

  /** The Spanner database name that this {@link ConnectionOptions} will connect to */
  public String getDatabaseName() {
    return databaseName;
  }

  /** The Spanner {@link DatabaseId} that this {@link ConnectionOptions} will connect to */
  public DatabaseId getDatabaseId() {
    Preconditions.checkState(projectId != null, "Project ID is not specified");
    Preconditions.checkState(instanceId != null, "Instance ID is not specified");
    Preconditions.checkState(databaseName != null, "Database name is not specified");
    return DatabaseId.of(projectId, instanceId, databaseName);
  }

  /**
   * The {@link Credentials} of this {@link ConnectionOptions}. This is either the credentials
   * specified in the credentialsUrl or the default Google application credentials
   */
  public Credentials getCredentials() {
    return credentials;
  }

  /** The initial autocommit value for connections created by this {@link ConnectionOptions} */
  public boolean isAutocommit() {
    return autocommit;
  }

  /** The initial readonly value for connections created by this {@link ConnectionOptions} */
  public boolean isReadOnly() {
    return readOnly;
  }

  /**
   * Whether read/write transactions and partitioned DML are preferred to be routed to the leader
   * region.
   */
  public boolean isRouteToLeader() {
    return routeToLeader;
  }

  /**
   * The initial retryAbortsInternally value for connections created by this {@link
   * ConnectionOptions}
   */
  public boolean isRetryAbortsInternally() {
    return retryAbortsInternally;
  }

  /** Whether connections should use virtual threads for connection executors. */
  public boolean isUseVirtualThreads() {
    return useVirtualThreads;
  }

  /** Whether virtual threads should be used for gRPC transport. */
  public boolean isUseVirtualGrpcTransportThreads() {
    return useVirtualGrpcTransportThreads;
  }

  /** Any warnings that were generated while creating the {@link ConnectionOptions} instance. */
  @Nullable
  public String getWarnings() {
    return warnings;
  }

  /** Use http instead of https. Only valid for (local) test servers. */
  boolean isUsePlainText() {
    return usePlainText;
  }

  /**
   * The (custom) user agent string to use for this connection. If <code>null</code>, then the
   * default JDBC user agent string will be used.
   */
  String getUserAgent() {
    return userAgent;
  }

  /** The {@link QueryOptions} to use for the connection. */
  QueryOptions getQueryOptions() {
    return queryOptions;
  }

  /** Whether connections created by this {@link ConnectionOptions} return commit stats. */
  public boolean isReturnCommitStats() {
    return returnCommitStats;
  }

  /** The max_commit_delay that should be applied to commit operations on this connection. */
  public Duration getMaxCommitDelay() {
    return maxCommitDelay == null ? null : Duration.ofMillis(maxCommitDelay);
  }

  /**
   * Whether connections created by this {@link ConnectionOptions} will automatically try to connect
   * to the emulator using the default host/port of the emulator, and automatically create the
   * instance and database that is specified in the connection string if these do not exist on the
   * emulator instance.
   */
  public boolean isAutoConfigEmulator() {
    return autoConfigEmulator;
  }

  /**
   * Returns true if a connection should generate auto-savepoints for retrying transactions on the
   * emulator. This allows some more concurrent transactions on the emulator.
   */
  boolean useAutoSavepointsForEmulator() {
    // For now, this option is directly linked to the option autoConfigEmulator=true, which is the
    // recommended way to configure the emulator for the Connection API.
    return autoConfigEmulator;
  }

  public Dialect getDialect() {
    return dialect;
  }

  /** The {@link RpcPriority} to use for the connection. */
  RpcPriority getRPCPriority() {
    return rpcPriority;
  }

  DdlInTransactionMode getDdlInTransactionMode() {
    return this.ddlInTransactionMode;
  }

  /**
   * Whether connections created by this {@link ConnectionOptions} should delay the actual start of
   * a read/write transaction until the first write operation.
   */
  boolean isDelayTransactionStartUntilFirstWrite() {
    return delayTransactionStartUntilFirstWrite;
  }

  boolean isTrackConnectionLeaks() {
    return this.trackConnectionLeaks;
  }

  boolean isDataBoostEnabled() {
    return this.dataBoostEnabled;
  }

  boolean isAutoPartitionMode() {
    return this.autoPartitionMode;
  }

  int getMaxPartitions() {
    return this.maxPartitions;
  }

  int getMaxPartitionedParallelism() {
    return this.maxPartitionedParallelism;
  }

  Boolean isEnableExtendedTracing() {
    return this.enableExtendedTracing;
  }

  Boolean isEnableApiTracing() {
    return this.enableApiTracing;
  }

  /** Interceptors that should be executed after each statement */
  List<StatementExecutionInterceptor> getStatementExecutionInterceptors() {
    return statementExecutionInterceptors;
  }

  @Override
  public String toString() {
    return getUri();
  }
}
