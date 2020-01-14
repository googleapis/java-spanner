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
package com.google.cloud.spanner.admin.database.v1.stub;

import static com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient.ListDatabasesPagedResponse;

import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.BetaApi;
import com.google.api.gax.core.GaxProperties;
import com.google.api.gax.core.GoogleCredentialsProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.api.gax.grpc.GaxGrpcProperties;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.grpc.ProtoOperationTransformers;
import com.google.api.gax.longrunning.OperationSnapshot;
import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.gax.rpc.OperationCallSettings;
import com.google.api.gax.rpc.PageContext;
import com.google.api.gax.rpc.PagedCallSettings;
import com.google.api.gax.rpc.PagedListDescriptor;
import com.google.api.gax.rpc.PagedListResponseFactory;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.StubSettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.protobuf.Empty;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.DropDatabaseRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlRequest;
import com.google.spanner.admin.database.v1.GetDatabaseDdlResponse;
import com.google.spanner.admin.database.v1.GetDatabaseRequest;
import com.google.spanner.admin.database.v1.ListDatabasesRequest;
import com.google.spanner.admin.database.v1.ListDatabasesResponse;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlRequest;
import java.io.IOException;
import java.util.List;
import javax.annotation.Generated;
import org.threeten.bp.Duration;

// AUTO-GENERATED DOCUMENTATION AND CLASS
/**
 * Settings class to configure an instance of {@link DatabaseAdminStub}.
 *
 * <p>The default instance has everything set to sensible defaults:
 *
 * <ul>
 *   <li>The default service address (spanner.googleapis.com) and default port (443) are used.
 *   <li>Credentials are acquired automatically through Application Default Credentials.
 *   <li>Retries are configured for idempotent methods but not for non-idempotent methods.
 * </ul>
 *
 * <p>The builder of this class is recursive, so contained classes are themselves builders. When
 * build() is called, the tree of builders is called to create the complete settings object.
 *
 * <p>For example, to set the total timeout of getDatabase to 30 seconds:
 *
 * <pre>
 * <code>
 * DatabaseAdminStubSettings.Builder databaseAdminSettingsBuilder =
 *     DatabaseAdminStubSettings.newBuilder();
 * databaseAdminSettingsBuilder.getDatabaseSettings().getRetrySettings().toBuilder()
 *     .setTotalTimeout(Duration.ofSeconds(30));
 * DatabaseAdminStubSettings databaseAdminSettings = databaseAdminSettingsBuilder.build();
 * </code>
 * </pre>
 */
@Generated("by gapic-generator")
@BetaApi
public class DatabaseAdminStubSettings extends StubSettings<DatabaseAdminStubSettings> {
  /** The default scopes of the service. */
  private static final ImmutableList<String> DEFAULT_SERVICE_SCOPES =
      ImmutableList.<String>builder()
          .add("https://www.googleapis.com/auth/cloud-platform")
          .add("https://www.googleapis.com/auth/spanner.admin")
          .build();

  private final UnaryCallSettings<CreateDatabaseRequest, Operation> createDatabaseSettings;
  private final OperationCallSettings<CreateDatabaseRequest, Database, CreateDatabaseMetadata>
      createDatabaseOperationSettings;
  private final UnaryCallSettings<GetDatabaseRequest, Database> getDatabaseSettings;
  private final UnaryCallSettings<UpdateDatabaseDdlRequest, Operation> updateDatabaseDdlSettings;
  private final OperationCallSettings<UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
      updateDatabaseDdlOperationSettings;
  private final UnaryCallSettings<DropDatabaseRequest, Empty> dropDatabaseSettings;
  private final UnaryCallSettings<GetDatabaseDdlRequest, GetDatabaseDdlResponse>
      getDatabaseDdlSettings;
  private final UnaryCallSettings<SetIamPolicyRequest, Policy> setIamPolicySettings;
  private final UnaryCallSettings<GetIamPolicyRequest, Policy> getIamPolicySettings;
  private final UnaryCallSettings<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsSettings;
  private final PagedCallSettings<
          ListDatabasesRequest, ListDatabasesResponse, ListDatabasesPagedResponse>
      listDatabasesSettings;

  /** Returns the object with the settings used for calls to createDatabase. */
  public UnaryCallSettings<CreateDatabaseRequest, Operation> createDatabaseSettings() {
    return createDatabaseSettings;
  }

  /** Returns the object with the settings used for calls to createDatabase. */
  @BetaApi("The surface for use by generated code is not stable yet and may change in the future.")
  public OperationCallSettings<CreateDatabaseRequest, Database, CreateDatabaseMetadata>
      createDatabaseOperationSettings() {
    return createDatabaseOperationSettings;
  }

  /** Returns the object with the settings used for calls to getDatabase. */
  public UnaryCallSettings<GetDatabaseRequest, Database> getDatabaseSettings() {
    return getDatabaseSettings;
  }

  /** Returns the object with the settings used for calls to updateDatabaseDdl. */
  public UnaryCallSettings<UpdateDatabaseDdlRequest, Operation> updateDatabaseDdlSettings() {
    return updateDatabaseDdlSettings;
  }

  /** Returns the object with the settings used for calls to updateDatabaseDdl. */
  @BetaApi("The surface for use by generated code is not stable yet and may change in the future.")
  public OperationCallSettings<UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
      updateDatabaseDdlOperationSettings() {
    return updateDatabaseDdlOperationSettings;
  }

  /** Returns the object with the settings used for calls to dropDatabase. */
  public UnaryCallSettings<DropDatabaseRequest, Empty> dropDatabaseSettings() {
    return dropDatabaseSettings;
  }

  /** Returns the object with the settings used for calls to getDatabaseDdl. */
  public UnaryCallSettings<GetDatabaseDdlRequest, GetDatabaseDdlResponse> getDatabaseDdlSettings() {
    return getDatabaseDdlSettings;
  }

  /** Returns the object with the settings used for calls to setIamPolicy. */
  public UnaryCallSettings<SetIamPolicyRequest, Policy> setIamPolicySettings() {
    return setIamPolicySettings;
  }

  /** Returns the object with the settings used for calls to getIamPolicy. */
  public UnaryCallSettings<GetIamPolicyRequest, Policy> getIamPolicySettings() {
    return getIamPolicySettings;
  }

  /** Returns the object with the settings used for calls to testIamPermissions. */
  public UnaryCallSettings<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsSettings() {
    return testIamPermissionsSettings;
  }

  /** Returns the object with the settings used for calls to listDatabases. */
  public PagedCallSettings<ListDatabasesRequest, ListDatabasesResponse, ListDatabasesPagedResponse>
      listDatabasesSettings() {
    return listDatabasesSettings;
  }

  @BetaApi("A restructuring of stub classes is planned, so this may break in the future")
  public DatabaseAdminStub createStub() throws IOException {
    if (getTransportChannelProvider()
        .getTransportName()
        .equals(GrpcTransportChannel.getGrpcTransportName())) {
      return GrpcDatabaseAdminStub.create(this);
    } else {
      throw new UnsupportedOperationException(
          "Transport not supported: " + getTransportChannelProvider().getTransportName());
    }
  }

  /** Returns a builder for the default ExecutorProvider for this service. */
  public static InstantiatingExecutorProvider.Builder defaultExecutorProviderBuilder() {
    return InstantiatingExecutorProvider.newBuilder();
  }

  /** Returns the default service endpoint. */
  public static String getDefaultEndpoint() {
    return "spanner.googleapis.com:443";
  }

  /** Returns the default service scopes. */
  public static List<String> getDefaultServiceScopes() {
    return DEFAULT_SERVICE_SCOPES;
  }

  /** Returns a builder for the default credentials for this service. */
  public static GoogleCredentialsProvider.Builder defaultCredentialsProviderBuilder() {
    return GoogleCredentialsProvider.newBuilder().setScopesToApply(DEFAULT_SERVICE_SCOPES);
  }

  /** Returns a builder for the default ChannelProvider for this service. */
  public static InstantiatingGrpcChannelProvider.Builder defaultGrpcTransportProviderBuilder() {
    return InstantiatingGrpcChannelProvider.newBuilder()
        .setMaxInboundMessageSize(Integer.MAX_VALUE);
  }

  public static TransportChannelProvider defaultTransportChannelProvider() {
    return defaultGrpcTransportProviderBuilder().build();
  }

  @BetaApi("The surface for customizing headers is not stable yet and may change in the future.")
  public static ApiClientHeaderProvider.Builder defaultApiClientHeaderProviderBuilder() {
    return ApiClientHeaderProvider.newBuilder()
        .setGeneratedLibToken(
            "gapic", GaxProperties.getLibraryVersion(DatabaseAdminStubSettings.class))
        .setTransportToken(
            GaxGrpcProperties.getGrpcTokenName(), GaxGrpcProperties.getGrpcVersion());
  }

  /** Returns a new builder for this class. */
  public static Builder newBuilder() {
    return Builder.createDefault();
  }

  /** Returns a new builder for this class. */
  public static Builder newBuilder(ClientContext clientContext) {
    return new Builder(clientContext);
  }

  /** Returns a builder containing all the values of this settings class. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected DatabaseAdminStubSettings(Builder settingsBuilder) throws IOException {
    super(settingsBuilder);

    createDatabaseSettings = settingsBuilder.createDatabaseSettings().build();
    createDatabaseOperationSettings = settingsBuilder.createDatabaseOperationSettings().build();
    getDatabaseSettings = settingsBuilder.getDatabaseSettings().build();
    updateDatabaseDdlSettings = settingsBuilder.updateDatabaseDdlSettings().build();
    updateDatabaseDdlOperationSettings =
        settingsBuilder.updateDatabaseDdlOperationSettings().build();
    dropDatabaseSettings = settingsBuilder.dropDatabaseSettings().build();
    getDatabaseDdlSettings = settingsBuilder.getDatabaseDdlSettings().build();
    setIamPolicySettings = settingsBuilder.setIamPolicySettings().build();
    getIamPolicySettings = settingsBuilder.getIamPolicySettings().build();
    testIamPermissionsSettings = settingsBuilder.testIamPermissionsSettings().build();
    listDatabasesSettings = settingsBuilder.listDatabasesSettings().build();
  }

  private static final PagedListDescriptor<ListDatabasesRequest, ListDatabasesResponse, Database>
      LIST_DATABASES_PAGE_STR_DESC =
          new PagedListDescriptor<ListDatabasesRequest, ListDatabasesResponse, Database>() {
            @Override
            public String emptyToken() {
              return "";
            }

            @Override
            public ListDatabasesRequest injectToken(ListDatabasesRequest payload, String token) {
              return ListDatabasesRequest.newBuilder(payload).setPageToken(token).build();
            }

            @Override
            public ListDatabasesRequest injectPageSize(ListDatabasesRequest payload, int pageSize) {
              return ListDatabasesRequest.newBuilder(payload).setPageSize(pageSize).build();
            }

            @Override
            public Integer extractPageSize(ListDatabasesRequest payload) {
              return payload.getPageSize();
            }

            @Override
            public String extractNextToken(ListDatabasesResponse payload) {
              return payload.getNextPageToken();
            }

            @Override
            public Iterable<Database> extractResources(ListDatabasesResponse payload) {
              return payload.getDatabasesList() != null
                  ? payload.getDatabasesList()
                  : ImmutableList.<Database>of();
            }
          };

  private static final PagedListResponseFactory<
          ListDatabasesRequest, ListDatabasesResponse, ListDatabasesPagedResponse>
      LIST_DATABASES_PAGE_STR_FACT =
          new PagedListResponseFactory<
              ListDatabasesRequest, ListDatabasesResponse, ListDatabasesPagedResponse>() {
            @Override
            public ApiFuture<ListDatabasesPagedResponse> getFuturePagedResponse(
                UnaryCallable<ListDatabasesRequest, ListDatabasesResponse> callable,
                ListDatabasesRequest request,
                ApiCallContext context,
                ApiFuture<ListDatabasesResponse> futureResponse) {
              PageContext<ListDatabasesRequest, ListDatabasesResponse, Database> pageContext =
                  PageContext.create(callable, LIST_DATABASES_PAGE_STR_DESC, request, context);
              return ListDatabasesPagedResponse.createAsync(pageContext, futureResponse);
            }
          };

  /** Builder for DatabaseAdminStubSettings. */
  public static class Builder extends StubSettings.Builder<DatabaseAdminStubSettings, Builder> {
    private final ImmutableList<UnaryCallSettings.Builder<?, ?>> unaryMethodSettingsBuilders;

    private final UnaryCallSettings.Builder<CreateDatabaseRequest, Operation>
        createDatabaseSettings;
    private final OperationCallSettings.Builder<
            CreateDatabaseRequest, Database, CreateDatabaseMetadata>
        createDatabaseOperationSettings;
    private final UnaryCallSettings.Builder<GetDatabaseRequest, Database> getDatabaseSettings;
    private final UnaryCallSettings.Builder<UpdateDatabaseDdlRequest, Operation>
        updateDatabaseDdlSettings;
    private final OperationCallSettings.Builder<
            UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
        updateDatabaseDdlOperationSettings;
    private final UnaryCallSettings.Builder<DropDatabaseRequest, Empty> dropDatabaseSettings;
    private final UnaryCallSettings.Builder<GetDatabaseDdlRequest, GetDatabaseDdlResponse>
        getDatabaseDdlSettings;
    private final UnaryCallSettings.Builder<SetIamPolicyRequest, Policy> setIamPolicySettings;
    private final UnaryCallSettings.Builder<GetIamPolicyRequest, Policy> getIamPolicySettings;
    private final UnaryCallSettings.Builder<TestIamPermissionsRequest, TestIamPermissionsResponse>
        testIamPermissionsSettings;
    private final PagedCallSettings.Builder<
            ListDatabasesRequest, ListDatabasesResponse, ListDatabasesPagedResponse>
        listDatabasesSettings;

    private static final ImmutableMap<String, ImmutableSet<StatusCode.Code>>
        RETRYABLE_CODE_DEFINITIONS;

    static {
      ImmutableMap.Builder<String, ImmutableSet<StatusCode.Code>> definitions =
          ImmutableMap.builder();
      definitions.put(
          "idempotent",
          ImmutableSet.copyOf(
              Lists.<StatusCode.Code>newArrayList(
                  StatusCode.Code.DEADLINE_EXCEEDED, StatusCode.Code.UNAVAILABLE)));
      definitions.put("non_idempotent", ImmutableSet.copyOf(Lists.<StatusCode.Code>newArrayList()));
      RETRYABLE_CODE_DEFINITIONS = definitions.build();
    }

    private static final ImmutableMap<String, RetrySettings> RETRY_PARAM_DEFINITIONS;

    static {
      ImmutableMap.Builder<String, RetrySettings> definitions = ImmutableMap.builder();
      RetrySettings settings = null;
      settings =
          RetrySettings.newBuilder()
              .setInitialRetryDelay(Duration.ofMillis(1000L))
              .setRetryDelayMultiplier(1.3)
              .setMaxRetryDelay(Duration.ofMillis(32000L))
              .setInitialRpcTimeout(Duration.ofMillis(60000L))
              .setRpcTimeoutMultiplier(1.0)
              .setMaxRpcTimeout(Duration.ofMillis(60000L))
              .setTotalTimeout(Duration.ofMillis(600000L))
              .build();
      definitions.put("default", settings);
      RETRY_PARAM_DEFINITIONS = definitions.build();
    }

    protected Builder() {
      this((ClientContext) null);
    }

    protected Builder(ClientContext clientContext) {
      super(clientContext);

      createDatabaseSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      createDatabaseOperationSettings = OperationCallSettings.newBuilder();

      getDatabaseSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      updateDatabaseDdlSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      updateDatabaseDdlOperationSettings = OperationCallSettings.newBuilder();

      dropDatabaseSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      getDatabaseDdlSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      setIamPolicySettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      getIamPolicySettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      testIamPermissionsSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      listDatabasesSettings = PagedCallSettings.newBuilder(LIST_DATABASES_PAGE_STR_FACT);

      unaryMethodSettingsBuilders =
          ImmutableList.<UnaryCallSettings.Builder<?, ?>>of(
              createDatabaseSettings,
              getDatabaseSettings,
              updateDatabaseDdlSettings,
              dropDatabaseSettings,
              getDatabaseDdlSettings,
              setIamPolicySettings,
              getIamPolicySettings,
              testIamPermissionsSettings,
              listDatabasesSettings);

      initDefaults(this);
    }

    private static Builder createDefault() {
      Builder builder = new Builder((ClientContext) null);
      builder.setTransportChannelProvider(defaultTransportChannelProvider());
      builder.setCredentialsProvider(defaultCredentialsProviderBuilder().build());
      builder.setInternalHeaderProvider(defaultApiClientHeaderProviderBuilder().build());
      builder.setEndpoint(getDefaultEndpoint());
      return initDefaults(builder);
    }

    private static Builder initDefaults(Builder builder) {

      builder
          .createDatabaseSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("non_idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .getDatabaseSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .updateDatabaseDdlSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .dropDatabaseSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .getDatabaseDdlSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .setIamPolicySettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("non_idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .getIamPolicySettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .testIamPermissionsSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("non_idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));

      builder
          .listDatabasesSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("idempotent"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"));
      builder
          .createDatabaseOperationSettings()
          .setInitialCallSettings(
              UnaryCallSettings
                  .<CreateDatabaseRequest, OperationSnapshot>newUnaryCallSettingsBuilder()
                  .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("non_idempotent"))
                  .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"))
                  .build())
          .setResponseTransformer(
              ProtoOperationTransformers.ResponseTransformer.create(Database.class))
          .setMetadataTransformer(
              ProtoOperationTransformers.MetadataTransformer.create(CreateDatabaseMetadata.class))
          .setPollingAlgorithm(
              OperationTimedPollAlgorithm.create(
                  RetrySettings.newBuilder()
                      .setInitialRetryDelay(Duration.ofMillis(20000L))
                      .setRetryDelayMultiplier(1.5)
                      .setMaxRetryDelay(Duration.ofMillis(45000L))
                      .setInitialRpcTimeout(Duration.ZERO) // ignored
                      .setRpcTimeoutMultiplier(1.0) // ignored
                      .setMaxRpcTimeout(Duration.ZERO) // ignored
                      .setTotalTimeout(Duration.ofMillis(86400000L))
                      .build()));
      builder
          .updateDatabaseDdlOperationSettings()
          .setInitialCallSettings(
              UnaryCallSettings
                  .<UpdateDatabaseDdlRequest, OperationSnapshot>newUnaryCallSettingsBuilder()
                  .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("idempotent"))
                  .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("default"))
                  .build())
          .setResponseTransformer(
              ProtoOperationTransformers.ResponseTransformer.create(Empty.class))
          .setMetadataTransformer(
              ProtoOperationTransformers.MetadataTransformer.create(
                  UpdateDatabaseDdlMetadata.class))
          .setPollingAlgorithm(
              OperationTimedPollAlgorithm.create(
                  RetrySettings.newBuilder()
                      .setInitialRetryDelay(Duration.ofMillis(20000L))
                      .setRetryDelayMultiplier(1.5)
                      .setMaxRetryDelay(Duration.ofMillis(45000L))
                      .setInitialRpcTimeout(Duration.ZERO) // ignored
                      .setRpcTimeoutMultiplier(1.0) // ignored
                      .setMaxRpcTimeout(Duration.ZERO) // ignored
                      .setTotalTimeout(Duration.ofMillis(86400000L))
                      .build()));

      return builder;
    }

    protected Builder(DatabaseAdminStubSettings settings) {
      super(settings);

      createDatabaseSettings = settings.createDatabaseSettings.toBuilder();
      createDatabaseOperationSettings = settings.createDatabaseOperationSettings.toBuilder();
      getDatabaseSettings = settings.getDatabaseSettings.toBuilder();
      updateDatabaseDdlSettings = settings.updateDatabaseDdlSettings.toBuilder();
      updateDatabaseDdlOperationSettings = settings.updateDatabaseDdlOperationSettings.toBuilder();
      dropDatabaseSettings = settings.dropDatabaseSettings.toBuilder();
      getDatabaseDdlSettings = settings.getDatabaseDdlSettings.toBuilder();
      setIamPolicySettings = settings.setIamPolicySettings.toBuilder();
      getIamPolicySettings = settings.getIamPolicySettings.toBuilder();
      testIamPermissionsSettings = settings.testIamPermissionsSettings.toBuilder();
      listDatabasesSettings = settings.listDatabasesSettings.toBuilder();

      unaryMethodSettingsBuilders =
          ImmutableList.<UnaryCallSettings.Builder<?, ?>>of(
              createDatabaseSettings,
              getDatabaseSettings,
              updateDatabaseDdlSettings,
              dropDatabaseSettings,
              getDatabaseDdlSettings,
              setIamPolicySettings,
              getIamPolicySettings,
              testIamPermissionsSettings,
              listDatabasesSettings);
    }

    // NEXT_MAJOR_VER: remove 'throws Exception'
    /**
     * Applies the given settings updater function to all of the unary API methods in this service.
     *
     * <p>Note: This method does not support applying settings to streaming methods.
     */
    public Builder applyToAllUnaryMethods(
        ApiFunction<UnaryCallSettings.Builder<?, ?>, Void> settingsUpdater) throws Exception {
      super.applyToAllUnaryMethods(unaryMethodSettingsBuilders, settingsUpdater);
      return this;
    }

    public ImmutableList<UnaryCallSettings.Builder<?, ?>> unaryMethodSettingsBuilders() {
      return unaryMethodSettingsBuilders;
    }

    /** Returns the builder for the settings used for calls to createDatabase. */
    public UnaryCallSettings.Builder<CreateDatabaseRequest, Operation> createDatabaseSettings() {
      return createDatabaseSettings;
    }

    /** Returns the builder for the settings used for calls to createDatabase. */
    @BetaApi(
        "The surface for use by generated code is not stable yet and may change in the future.")
    public OperationCallSettings.Builder<CreateDatabaseRequest, Database, CreateDatabaseMetadata>
        createDatabaseOperationSettings() {
      return createDatabaseOperationSettings;
    }

    /** Returns the builder for the settings used for calls to getDatabase. */
    public UnaryCallSettings.Builder<GetDatabaseRequest, Database> getDatabaseSettings() {
      return getDatabaseSettings;
    }

    /** Returns the builder for the settings used for calls to updateDatabaseDdl. */
    public UnaryCallSettings.Builder<UpdateDatabaseDdlRequest, Operation>
        updateDatabaseDdlSettings() {
      return updateDatabaseDdlSettings;
    }

    /** Returns the builder for the settings used for calls to updateDatabaseDdl. */
    @BetaApi(
        "The surface for use by generated code is not stable yet and may change in the future.")
    public OperationCallSettings.Builder<UpdateDatabaseDdlRequest, Empty, UpdateDatabaseDdlMetadata>
        updateDatabaseDdlOperationSettings() {
      return updateDatabaseDdlOperationSettings;
    }

    /** Returns the builder for the settings used for calls to dropDatabase. */
    public UnaryCallSettings.Builder<DropDatabaseRequest, Empty> dropDatabaseSettings() {
      return dropDatabaseSettings;
    }

    /** Returns the builder for the settings used for calls to getDatabaseDdl. */
    public UnaryCallSettings.Builder<GetDatabaseDdlRequest, GetDatabaseDdlResponse>
        getDatabaseDdlSettings() {
      return getDatabaseDdlSettings;
    }

    /** Returns the builder for the settings used for calls to setIamPolicy. */
    public UnaryCallSettings.Builder<SetIamPolicyRequest, Policy> setIamPolicySettings() {
      return setIamPolicySettings;
    }

    /** Returns the builder for the settings used for calls to getIamPolicy. */
    public UnaryCallSettings.Builder<GetIamPolicyRequest, Policy> getIamPolicySettings() {
      return getIamPolicySettings;
    }

    /** Returns the builder for the settings used for calls to testIamPermissions. */
    public UnaryCallSettings.Builder<TestIamPermissionsRequest, TestIamPermissionsResponse>
        testIamPermissionsSettings() {
      return testIamPermissionsSettings;
    }

    /** Returns the builder for the settings used for calls to listDatabases. */
    public PagedCallSettings.Builder<
            ListDatabasesRequest, ListDatabasesResponse, ListDatabasesPagedResponse>
        listDatabasesSettings() {
      return listDatabasesSettings;
    }

    @Override
    public DatabaseAdminStubSettings build() throws IOException {
      return new DatabaseAdminStubSettings(this);
    }
  }
}
