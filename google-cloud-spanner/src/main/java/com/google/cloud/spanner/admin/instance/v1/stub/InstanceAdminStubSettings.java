/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.admin.instance.v1.stub;

import static com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient.ListInstanceConfigOperationsPagedResponse;
import static com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient.ListInstanceConfigsPagedResponse;
import static com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient.ListInstancesPagedResponse;

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
import com.google.api.gax.httpjson.GaxHttpJsonProperties;
import com.google.api.gax.httpjson.HttpJsonTransportChannel;
import com.google.api.gax.httpjson.InstantiatingHttpJsonChannelProvider;
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
import com.google.spanner.admin.instance.v1.CreateInstanceConfigMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceConfigRequest;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceRequest;
import com.google.spanner.admin.instance.v1.DeleteInstanceConfigRequest;
import com.google.spanner.admin.instance.v1.DeleteInstanceRequest;
import com.google.spanner.admin.instance.v1.GetInstanceConfigRequest;
import com.google.spanner.admin.instance.v1.GetInstanceRequest;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest;
import com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse;
import com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest;
import com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse;
import com.google.spanner.admin.instance.v1.ListInstancesRequest;
import com.google.spanner.admin.instance.v1.ListInstancesResponse;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest;
import com.google.spanner.admin.instance.v1.UpdateInstanceMetadata;
import com.google.spanner.admin.instance.v1.UpdateInstanceRequest;
import java.io.IOException;
import java.util.List;
import javax.annotation.Generated;
import org.threeten.bp.Duration;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * Settings class to configure an instance of {@link InstanceAdminStub}.
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
 * <p>For example, to set the total timeout of getInstanceConfig to 30 seconds:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * InstanceAdminStubSettings.Builder instanceAdminSettingsBuilder =
 *     InstanceAdminStubSettings.newBuilder();
 * instanceAdminSettingsBuilder
 *     .getInstanceConfigSettings()
 *     .setRetrySettings(
 *         instanceAdminSettingsBuilder
 *             .getInstanceConfigSettings()
 *             .getRetrySettings()
 *             .toBuilder()
 *             .setTotalTimeout(Duration.ofSeconds(30))
 *             .build());
 * InstanceAdminStubSettings instanceAdminSettings = instanceAdminSettingsBuilder.build();
 * }</pre>
 */
@Generated("by gapic-generator-java")
public class InstanceAdminStubSettings extends StubSettings<InstanceAdminStubSettings> {
  /** The default scopes of the service. */
  private static final ImmutableList<String> DEFAULT_SERVICE_SCOPES =
      ImmutableList.<String>builder()
          .add("https://www.googleapis.com/auth/cloud-platform")
          .add("https://www.googleapis.com/auth/spanner.admin")
          .build();

  private final PagedCallSettings<
          ListInstanceConfigsRequest, ListInstanceConfigsResponse, ListInstanceConfigsPagedResponse>
      listInstanceConfigsSettings;
  private final UnaryCallSettings<GetInstanceConfigRequest, InstanceConfig>
      getInstanceConfigSettings;
  private final UnaryCallSettings<CreateInstanceConfigRequest, Operation>
      createInstanceConfigSettings;
  private final OperationCallSettings<
          CreateInstanceConfigRequest, InstanceConfig, CreateInstanceConfigMetadata>
      createInstanceConfigOperationSettings;
  private final UnaryCallSettings<UpdateInstanceConfigRequest, Operation>
      updateInstanceConfigSettings;
  private final OperationCallSettings<
          UpdateInstanceConfigRequest, InstanceConfig, UpdateInstanceConfigMetadata>
      updateInstanceConfigOperationSettings;
  private final UnaryCallSettings<DeleteInstanceConfigRequest, Empty> deleteInstanceConfigSettings;
  private final PagedCallSettings<
          ListInstanceConfigOperationsRequest,
          ListInstanceConfigOperationsResponse,
          ListInstanceConfigOperationsPagedResponse>
      listInstanceConfigOperationsSettings;
  private final PagedCallSettings<
          ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>
      listInstancesSettings;
  private final UnaryCallSettings<GetInstanceRequest, Instance> getInstanceSettings;
  private final UnaryCallSettings<CreateInstanceRequest, Operation> createInstanceSettings;
  private final OperationCallSettings<CreateInstanceRequest, Instance, CreateInstanceMetadata>
      createInstanceOperationSettings;
  private final UnaryCallSettings<UpdateInstanceRequest, Operation> updateInstanceSettings;
  private final OperationCallSettings<UpdateInstanceRequest, Instance, UpdateInstanceMetadata>
      updateInstanceOperationSettings;
  private final UnaryCallSettings<DeleteInstanceRequest, Empty> deleteInstanceSettings;
  private final UnaryCallSettings<SetIamPolicyRequest, Policy> setIamPolicySettings;
  private final UnaryCallSettings<GetIamPolicyRequest, Policy> getIamPolicySettings;
  private final UnaryCallSettings<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsSettings;

  private static final PagedListDescriptor<
          ListInstanceConfigsRequest, ListInstanceConfigsResponse, InstanceConfig>
      LIST_INSTANCE_CONFIGS_PAGE_STR_DESC =
          new PagedListDescriptor<
              ListInstanceConfigsRequest, ListInstanceConfigsResponse, InstanceConfig>() {
            @Override
            public String emptyToken() {
              return "";
            }

            @Override
            public ListInstanceConfigsRequest injectToken(
                ListInstanceConfigsRequest payload, String token) {
              return ListInstanceConfigsRequest.newBuilder(payload).setPageToken(token).build();
            }

            @Override
            public ListInstanceConfigsRequest injectPageSize(
                ListInstanceConfigsRequest payload, int pageSize) {
              return ListInstanceConfigsRequest.newBuilder(payload).setPageSize(pageSize).build();
            }

            @Override
            public Integer extractPageSize(ListInstanceConfigsRequest payload) {
              return payload.getPageSize();
            }

            @Override
            public String extractNextToken(ListInstanceConfigsResponse payload) {
              return payload.getNextPageToken();
            }

            @Override
            public Iterable<InstanceConfig> extractResources(ListInstanceConfigsResponse payload) {
              return payload.getInstanceConfigsList() == null
                  ? ImmutableList.<InstanceConfig>of()
                  : payload.getInstanceConfigsList();
            }
          };

  private static final PagedListDescriptor<
          ListInstanceConfigOperationsRequest, ListInstanceConfigOperationsResponse, Operation>
      LIST_INSTANCE_CONFIG_OPERATIONS_PAGE_STR_DESC =
          new PagedListDescriptor<
              ListInstanceConfigOperationsRequest,
              ListInstanceConfigOperationsResponse,
              Operation>() {
            @Override
            public String emptyToken() {
              return "";
            }

            @Override
            public ListInstanceConfigOperationsRequest injectToken(
                ListInstanceConfigOperationsRequest payload, String token) {
              return ListInstanceConfigOperationsRequest.newBuilder(payload)
                  .setPageToken(token)
                  .build();
            }

            @Override
            public ListInstanceConfigOperationsRequest injectPageSize(
                ListInstanceConfigOperationsRequest payload, int pageSize) {
              return ListInstanceConfigOperationsRequest.newBuilder(payload)
                  .setPageSize(pageSize)
                  .build();
            }

            @Override
            public Integer extractPageSize(ListInstanceConfigOperationsRequest payload) {
              return payload.getPageSize();
            }

            @Override
            public String extractNextToken(ListInstanceConfigOperationsResponse payload) {
              return payload.getNextPageToken();
            }

            @Override
            public Iterable<Operation> extractResources(
                ListInstanceConfigOperationsResponse payload) {
              return payload.getOperationsList() == null
                  ? ImmutableList.<Operation>of()
                  : payload.getOperationsList();
            }
          };

  private static final PagedListDescriptor<ListInstancesRequest, ListInstancesResponse, Instance>
      LIST_INSTANCES_PAGE_STR_DESC =
          new PagedListDescriptor<ListInstancesRequest, ListInstancesResponse, Instance>() {
            @Override
            public String emptyToken() {
              return "";
            }

            @Override
            public ListInstancesRequest injectToken(ListInstancesRequest payload, String token) {
              return ListInstancesRequest.newBuilder(payload).setPageToken(token).build();
            }

            @Override
            public ListInstancesRequest injectPageSize(ListInstancesRequest payload, int pageSize) {
              return ListInstancesRequest.newBuilder(payload).setPageSize(pageSize).build();
            }

            @Override
            public Integer extractPageSize(ListInstancesRequest payload) {
              return payload.getPageSize();
            }

            @Override
            public String extractNextToken(ListInstancesResponse payload) {
              return payload.getNextPageToken();
            }

            @Override
            public Iterable<Instance> extractResources(ListInstancesResponse payload) {
              return payload.getInstancesList() == null
                  ? ImmutableList.<Instance>of()
                  : payload.getInstancesList();
            }
          };

  private static final PagedListResponseFactory<
          ListInstanceConfigsRequest, ListInstanceConfigsResponse, ListInstanceConfigsPagedResponse>
      LIST_INSTANCE_CONFIGS_PAGE_STR_FACT =
          new PagedListResponseFactory<
              ListInstanceConfigsRequest,
              ListInstanceConfigsResponse,
              ListInstanceConfigsPagedResponse>() {
            @Override
            public ApiFuture<ListInstanceConfigsPagedResponse> getFuturePagedResponse(
                UnaryCallable<ListInstanceConfigsRequest, ListInstanceConfigsResponse> callable,
                ListInstanceConfigsRequest request,
                ApiCallContext context,
                ApiFuture<ListInstanceConfigsResponse> futureResponse) {
              PageContext<ListInstanceConfigsRequest, ListInstanceConfigsResponse, InstanceConfig>
                  pageContext =
                      PageContext.create(
                          callable, LIST_INSTANCE_CONFIGS_PAGE_STR_DESC, request, context);
              return ListInstanceConfigsPagedResponse.createAsync(pageContext, futureResponse);
            }
          };

  private static final PagedListResponseFactory<
          ListInstanceConfigOperationsRequest,
          ListInstanceConfigOperationsResponse,
          ListInstanceConfigOperationsPagedResponse>
      LIST_INSTANCE_CONFIG_OPERATIONS_PAGE_STR_FACT =
          new PagedListResponseFactory<
              ListInstanceConfigOperationsRequest,
              ListInstanceConfigOperationsResponse,
              ListInstanceConfigOperationsPagedResponse>() {
            @Override
            public ApiFuture<ListInstanceConfigOperationsPagedResponse> getFuturePagedResponse(
                UnaryCallable<
                        ListInstanceConfigOperationsRequest, ListInstanceConfigOperationsResponse>
                    callable,
                ListInstanceConfigOperationsRequest request,
                ApiCallContext context,
                ApiFuture<ListInstanceConfigOperationsResponse> futureResponse) {
              PageContext<
                      ListInstanceConfigOperationsRequest,
                      ListInstanceConfigOperationsResponse,
                      Operation>
                  pageContext =
                      PageContext.create(
                          callable,
                          LIST_INSTANCE_CONFIG_OPERATIONS_PAGE_STR_DESC,
                          request,
                          context);
              return ListInstanceConfigOperationsPagedResponse.createAsync(
                  pageContext, futureResponse);
            }
          };

  private static final PagedListResponseFactory<
          ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>
      LIST_INSTANCES_PAGE_STR_FACT =
          new PagedListResponseFactory<
              ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>() {
            @Override
            public ApiFuture<ListInstancesPagedResponse> getFuturePagedResponse(
                UnaryCallable<ListInstancesRequest, ListInstancesResponse> callable,
                ListInstancesRequest request,
                ApiCallContext context,
                ApiFuture<ListInstancesResponse> futureResponse) {
              PageContext<ListInstancesRequest, ListInstancesResponse, Instance> pageContext =
                  PageContext.create(callable, LIST_INSTANCES_PAGE_STR_DESC, request, context);
              return ListInstancesPagedResponse.createAsync(pageContext, futureResponse);
            }
          };

  /** Returns the object with the settings used for calls to listInstanceConfigs. */
  public PagedCallSettings<
          ListInstanceConfigsRequest, ListInstanceConfigsResponse, ListInstanceConfigsPagedResponse>
      listInstanceConfigsSettings() {
    return listInstanceConfigsSettings;
  }

  /** Returns the object with the settings used for calls to getInstanceConfig. */
  public UnaryCallSettings<GetInstanceConfigRequest, InstanceConfig> getInstanceConfigSettings() {
    return getInstanceConfigSettings;
  }

  /** Returns the object with the settings used for calls to createInstanceConfig. */
  public UnaryCallSettings<CreateInstanceConfigRequest, Operation> createInstanceConfigSettings() {
    return createInstanceConfigSettings;
  }

  /** Returns the object with the settings used for calls to createInstanceConfig. */
  public OperationCallSettings<
          CreateInstanceConfigRequest, InstanceConfig, CreateInstanceConfigMetadata>
      createInstanceConfigOperationSettings() {
    return createInstanceConfigOperationSettings;
  }

  /** Returns the object with the settings used for calls to updateInstanceConfig. */
  public UnaryCallSettings<UpdateInstanceConfigRequest, Operation> updateInstanceConfigSettings() {
    return updateInstanceConfigSettings;
  }

  /** Returns the object with the settings used for calls to updateInstanceConfig. */
  public OperationCallSettings<
          UpdateInstanceConfigRequest, InstanceConfig, UpdateInstanceConfigMetadata>
      updateInstanceConfigOperationSettings() {
    return updateInstanceConfigOperationSettings;
  }

  /** Returns the object with the settings used for calls to deleteInstanceConfig. */
  public UnaryCallSettings<DeleteInstanceConfigRequest, Empty> deleteInstanceConfigSettings() {
    return deleteInstanceConfigSettings;
  }

  /** Returns the object with the settings used for calls to listInstanceConfigOperations. */
  public PagedCallSettings<
          ListInstanceConfigOperationsRequest,
          ListInstanceConfigOperationsResponse,
          ListInstanceConfigOperationsPagedResponse>
      listInstanceConfigOperationsSettings() {
    return listInstanceConfigOperationsSettings;
  }

  /** Returns the object with the settings used for calls to listInstances. */
  public PagedCallSettings<ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>
      listInstancesSettings() {
    return listInstancesSettings;
  }

  /** Returns the object with the settings used for calls to getInstance. */
  public UnaryCallSettings<GetInstanceRequest, Instance> getInstanceSettings() {
    return getInstanceSettings;
  }

  /** Returns the object with the settings used for calls to createInstance. */
  public UnaryCallSettings<CreateInstanceRequest, Operation> createInstanceSettings() {
    return createInstanceSettings;
  }

  /** Returns the object with the settings used for calls to createInstance. */
  public OperationCallSettings<CreateInstanceRequest, Instance, CreateInstanceMetadata>
      createInstanceOperationSettings() {
    return createInstanceOperationSettings;
  }

  /** Returns the object with the settings used for calls to updateInstance. */
  public UnaryCallSettings<UpdateInstanceRequest, Operation> updateInstanceSettings() {
    return updateInstanceSettings;
  }

  /** Returns the object with the settings used for calls to updateInstance. */
  public OperationCallSettings<UpdateInstanceRequest, Instance, UpdateInstanceMetadata>
      updateInstanceOperationSettings() {
    return updateInstanceOperationSettings;
  }

  /** Returns the object with the settings used for calls to deleteInstance. */
  public UnaryCallSettings<DeleteInstanceRequest, Empty> deleteInstanceSettings() {
    return deleteInstanceSettings;
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

  public InstanceAdminStub createStub() throws IOException {
    if (getTransportChannelProvider()
        .getTransportName()
        .equals(GrpcTransportChannel.getGrpcTransportName())) {
      return GrpcInstanceAdminStub.create(this);
    }
    if (getTransportChannelProvider()
        .getTransportName()
        .equals(HttpJsonTransportChannel.getHttpJsonTransportName())) {
      return HttpJsonInstanceAdminStub.create(this);
    }
    throw new UnsupportedOperationException(
        String.format(
            "Transport not supported: %s", getTransportChannelProvider().getTransportName()));
  }

  /** Returns the endpoint set by the user or the the service's default endpoint. */
  @Override
  public String getEndpoint() {
    if (super.getEndpoint() != null) {
      return super.getEndpoint();
    }
    return getDefaultEndpoint();
  }

  /** Returns the default service name. */
  @Override
  public String getServiceName() {
    return "spanner";
  }

  /** Returns a builder for the default ExecutorProvider for this service. */
  public static InstantiatingExecutorProvider.Builder defaultExecutorProviderBuilder() {
    return InstantiatingExecutorProvider.newBuilder();
  }

  /** Returns the default service endpoint. */
  public static String getDefaultEndpoint() {
    return "spanner.googleapis.com:443";
  }

  /** Returns the default mTLS service endpoint. */
  public static String getDefaultMtlsEndpoint() {
    return "spanner.mtls.googleapis.com:443";
  }

  /** Returns the default service scopes. */
  public static List<String> getDefaultServiceScopes() {
    return DEFAULT_SERVICE_SCOPES;
  }

  /** Returns a builder for the default credentials for this service. */
  public static GoogleCredentialsProvider.Builder defaultCredentialsProviderBuilder() {
    return GoogleCredentialsProvider.newBuilder()
        .setScopesToApply(DEFAULT_SERVICE_SCOPES)
        .setUseJwtAccessWithScope(true);
  }

  /** Returns a builder for the default gRPC ChannelProvider for this service. */
  public static InstantiatingGrpcChannelProvider.Builder defaultGrpcTransportProviderBuilder() {
    return InstantiatingGrpcChannelProvider.newBuilder()
        .setMaxInboundMessageSize(Integer.MAX_VALUE);
  }

  /** Returns a builder for the default REST ChannelProvider for this service. */
  @BetaApi
  public static InstantiatingHttpJsonChannelProvider.Builder
      defaultHttpJsonTransportProviderBuilder() {
    return InstantiatingHttpJsonChannelProvider.newBuilder();
  }

  public static TransportChannelProvider defaultTransportChannelProvider() {
    return defaultGrpcTransportProviderBuilder().build();
  }

  @BetaApi("The surface for customizing headers is not stable yet and may change in the future.")
  public static ApiClientHeaderProvider.Builder defaultGrpcApiClientHeaderProviderBuilder() {
    return ApiClientHeaderProvider.newBuilder()
        .setGeneratedLibToken(
            "gapic", GaxProperties.getLibraryVersion(InstanceAdminStubSettings.class))
        .setTransportToken(
            GaxGrpcProperties.getGrpcTokenName(), GaxGrpcProperties.getGrpcVersion());
  }

  @BetaApi("The surface for customizing headers is not stable yet and may change in the future.")
  public static ApiClientHeaderProvider.Builder defaultHttpJsonApiClientHeaderProviderBuilder() {
    return ApiClientHeaderProvider.newBuilder()
        .setGeneratedLibToken(
            "gapic", GaxProperties.getLibraryVersion(InstanceAdminStubSettings.class))
        .setTransportToken(
            GaxHttpJsonProperties.getHttpJsonTokenName(),
            GaxHttpJsonProperties.getHttpJsonVersion());
  }

  public static ApiClientHeaderProvider.Builder defaultApiClientHeaderProviderBuilder() {
    return InstanceAdminStubSettings.defaultGrpcApiClientHeaderProviderBuilder();
  }

  /** Returns a new gRPC builder for this class. */
  public static Builder newBuilder() {
    return Builder.createDefault();
  }

  /** Returns a new REST builder for this class. */
  public static Builder newHttpJsonBuilder() {
    return Builder.createHttpJsonDefault();
  }

  /** Returns a new builder for this class. */
  public static Builder newBuilder(ClientContext clientContext) {
    return new Builder(clientContext);
  }

  /** Returns a builder containing all the values of this settings class. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected InstanceAdminStubSettings(Builder settingsBuilder) throws IOException {
    super(settingsBuilder);

    listInstanceConfigsSettings = settingsBuilder.listInstanceConfigsSettings().build();
    getInstanceConfigSettings = settingsBuilder.getInstanceConfigSettings().build();
    createInstanceConfigSettings = settingsBuilder.createInstanceConfigSettings().build();
    createInstanceConfigOperationSettings =
        settingsBuilder.createInstanceConfigOperationSettings().build();
    updateInstanceConfigSettings = settingsBuilder.updateInstanceConfigSettings().build();
    updateInstanceConfigOperationSettings =
        settingsBuilder.updateInstanceConfigOperationSettings().build();
    deleteInstanceConfigSettings = settingsBuilder.deleteInstanceConfigSettings().build();
    listInstanceConfigOperationsSettings =
        settingsBuilder.listInstanceConfigOperationsSettings().build();
    listInstancesSettings = settingsBuilder.listInstancesSettings().build();
    getInstanceSettings = settingsBuilder.getInstanceSettings().build();
    createInstanceSettings = settingsBuilder.createInstanceSettings().build();
    createInstanceOperationSettings = settingsBuilder.createInstanceOperationSettings().build();
    updateInstanceSettings = settingsBuilder.updateInstanceSettings().build();
    updateInstanceOperationSettings = settingsBuilder.updateInstanceOperationSettings().build();
    deleteInstanceSettings = settingsBuilder.deleteInstanceSettings().build();
    setIamPolicySettings = settingsBuilder.setIamPolicySettings().build();
    getIamPolicySettings = settingsBuilder.getIamPolicySettings().build();
    testIamPermissionsSettings = settingsBuilder.testIamPermissionsSettings().build();
  }

  /** Builder for InstanceAdminStubSettings. */
  public static class Builder extends StubSettings.Builder<InstanceAdminStubSettings, Builder> {
    private final ImmutableList<UnaryCallSettings.Builder<?, ?>> unaryMethodSettingsBuilders;
    private final PagedCallSettings.Builder<
            ListInstanceConfigsRequest,
            ListInstanceConfigsResponse,
            ListInstanceConfigsPagedResponse>
        listInstanceConfigsSettings;
    private final UnaryCallSettings.Builder<GetInstanceConfigRequest, InstanceConfig>
        getInstanceConfigSettings;
    private final UnaryCallSettings.Builder<CreateInstanceConfigRequest, Operation>
        createInstanceConfigSettings;
    private final OperationCallSettings.Builder<
            CreateInstanceConfigRequest, InstanceConfig, CreateInstanceConfigMetadata>
        createInstanceConfigOperationSettings;
    private final UnaryCallSettings.Builder<UpdateInstanceConfigRequest, Operation>
        updateInstanceConfigSettings;
    private final OperationCallSettings.Builder<
            UpdateInstanceConfigRequest, InstanceConfig, UpdateInstanceConfigMetadata>
        updateInstanceConfigOperationSettings;
    private final UnaryCallSettings.Builder<DeleteInstanceConfigRequest, Empty>
        deleteInstanceConfigSettings;
    private final PagedCallSettings.Builder<
            ListInstanceConfigOperationsRequest,
            ListInstanceConfigOperationsResponse,
            ListInstanceConfigOperationsPagedResponse>
        listInstanceConfigOperationsSettings;
    private final PagedCallSettings.Builder<
            ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>
        listInstancesSettings;
    private final UnaryCallSettings.Builder<GetInstanceRequest, Instance> getInstanceSettings;
    private final UnaryCallSettings.Builder<CreateInstanceRequest, Operation>
        createInstanceSettings;
    private final OperationCallSettings.Builder<
            CreateInstanceRequest, Instance, CreateInstanceMetadata>
        createInstanceOperationSettings;
    private final UnaryCallSettings.Builder<UpdateInstanceRequest, Operation>
        updateInstanceSettings;
    private final OperationCallSettings.Builder<
            UpdateInstanceRequest, Instance, UpdateInstanceMetadata>
        updateInstanceOperationSettings;
    private final UnaryCallSettings.Builder<DeleteInstanceRequest, Empty> deleteInstanceSettings;
    private final UnaryCallSettings.Builder<SetIamPolicyRequest, Policy> setIamPolicySettings;
    private final UnaryCallSettings.Builder<GetIamPolicyRequest, Policy> getIamPolicySettings;
    private final UnaryCallSettings.Builder<TestIamPermissionsRequest, TestIamPermissionsResponse>
        testIamPermissionsSettings;
    private static final ImmutableMap<String, ImmutableSet<StatusCode.Code>>
        RETRYABLE_CODE_DEFINITIONS;

    static {
      ImmutableMap.Builder<String, ImmutableSet<StatusCode.Code>> definitions =
          ImmutableMap.builder();
      definitions.put(
          "retry_policy_0_codes",
          ImmutableSet.copyOf(
              Lists.<StatusCode.Code>newArrayList(
                  StatusCode.Code.UNAVAILABLE, StatusCode.Code.DEADLINE_EXCEEDED)));
      definitions.put("no_retry_codes", ImmutableSet.copyOf(Lists.<StatusCode.Code>newArrayList()));
      definitions.put(
          "no_retry_2_codes", ImmutableSet.copyOf(Lists.<StatusCode.Code>newArrayList()));
      definitions.put(
          "no_retry_3_codes", ImmutableSet.copyOf(Lists.<StatusCode.Code>newArrayList()));
      definitions.put(
          "retry_policy_1_codes",
          ImmutableSet.copyOf(
              Lists.<StatusCode.Code>newArrayList(
                  StatusCode.Code.UNAVAILABLE, StatusCode.Code.DEADLINE_EXCEEDED)));
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
              .setInitialRpcTimeout(Duration.ofMillis(3600000L))
              .setRpcTimeoutMultiplier(1.0)
              .setMaxRpcTimeout(Duration.ofMillis(3600000L))
              .setTotalTimeout(Duration.ofMillis(3600000L))
              .build();
      definitions.put("retry_policy_0_params", settings);
      settings = RetrySettings.newBuilder().setRpcTimeoutMultiplier(1.0).build();
      definitions.put("no_retry_params", settings);
      settings =
          RetrySettings.newBuilder()
              .setInitialRpcTimeout(Duration.ofMillis(3600000L))
              .setRpcTimeoutMultiplier(1.0)
              .setMaxRpcTimeout(Duration.ofMillis(3600000L))
              .setTotalTimeout(Duration.ofMillis(3600000L))
              .build();
      definitions.put("no_retry_2_params", settings);
      settings =
          RetrySettings.newBuilder()
              .setInitialRpcTimeout(Duration.ofMillis(30000L))
              .setRpcTimeoutMultiplier(1.0)
              .setMaxRpcTimeout(Duration.ofMillis(30000L))
              .setTotalTimeout(Duration.ofMillis(30000L))
              .build();
      definitions.put("no_retry_3_params", settings);
      settings =
          RetrySettings.newBuilder()
              .setInitialRetryDelay(Duration.ofMillis(1000L))
              .setRetryDelayMultiplier(1.3)
              .setMaxRetryDelay(Duration.ofMillis(32000L))
              .setInitialRpcTimeout(Duration.ofMillis(30000L))
              .setRpcTimeoutMultiplier(1.0)
              .setMaxRpcTimeout(Duration.ofMillis(30000L))
              .setTotalTimeout(Duration.ofMillis(30000L))
              .build();
      definitions.put("retry_policy_1_params", settings);
      RETRY_PARAM_DEFINITIONS = definitions.build();
    }

    protected Builder() {
      this(((ClientContext) null));
    }

    protected Builder(ClientContext clientContext) {
      super(clientContext);

      listInstanceConfigsSettings =
          PagedCallSettings.newBuilder(LIST_INSTANCE_CONFIGS_PAGE_STR_FACT);
      getInstanceConfigSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      createInstanceConfigSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      createInstanceConfigOperationSettings = OperationCallSettings.newBuilder();
      updateInstanceConfigSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      updateInstanceConfigOperationSettings = OperationCallSettings.newBuilder();
      deleteInstanceConfigSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      listInstanceConfigOperationsSettings =
          PagedCallSettings.newBuilder(LIST_INSTANCE_CONFIG_OPERATIONS_PAGE_STR_FACT);
      listInstancesSettings = PagedCallSettings.newBuilder(LIST_INSTANCES_PAGE_STR_FACT);
      getInstanceSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      createInstanceSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      createInstanceOperationSettings = OperationCallSettings.newBuilder();
      updateInstanceSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      updateInstanceOperationSettings = OperationCallSettings.newBuilder();
      deleteInstanceSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      setIamPolicySettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      getIamPolicySettings = UnaryCallSettings.newUnaryCallSettingsBuilder();
      testIamPermissionsSettings = UnaryCallSettings.newUnaryCallSettingsBuilder();

      unaryMethodSettingsBuilders =
          ImmutableList.<UnaryCallSettings.Builder<?, ?>>of(
              listInstanceConfigsSettings,
              getInstanceConfigSettings,
              createInstanceConfigSettings,
              updateInstanceConfigSettings,
              deleteInstanceConfigSettings,
              listInstanceConfigOperationsSettings,
              listInstancesSettings,
              getInstanceSettings,
              createInstanceSettings,
              updateInstanceSettings,
              deleteInstanceSettings,
              setIamPolicySettings,
              getIamPolicySettings,
              testIamPermissionsSettings);
      initDefaults(this);
    }

    protected Builder(InstanceAdminStubSettings settings) {
      super(settings);

      listInstanceConfigsSettings = settings.listInstanceConfigsSettings.toBuilder();
      getInstanceConfigSettings = settings.getInstanceConfigSettings.toBuilder();
      createInstanceConfigSettings = settings.createInstanceConfigSettings.toBuilder();
      createInstanceConfigOperationSettings =
          settings.createInstanceConfigOperationSettings.toBuilder();
      updateInstanceConfigSettings = settings.updateInstanceConfigSettings.toBuilder();
      updateInstanceConfigOperationSettings =
          settings.updateInstanceConfigOperationSettings.toBuilder();
      deleteInstanceConfigSettings = settings.deleteInstanceConfigSettings.toBuilder();
      listInstanceConfigOperationsSettings =
          settings.listInstanceConfigOperationsSettings.toBuilder();
      listInstancesSettings = settings.listInstancesSettings.toBuilder();
      getInstanceSettings = settings.getInstanceSettings.toBuilder();
      createInstanceSettings = settings.createInstanceSettings.toBuilder();
      createInstanceOperationSettings = settings.createInstanceOperationSettings.toBuilder();
      updateInstanceSettings = settings.updateInstanceSettings.toBuilder();
      updateInstanceOperationSettings = settings.updateInstanceOperationSettings.toBuilder();
      deleteInstanceSettings = settings.deleteInstanceSettings.toBuilder();
      setIamPolicySettings = settings.setIamPolicySettings.toBuilder();
      getIamPolicySettings = settings.getIamPolicySettings.toBuilder();
      testIamPermissionsSettings = settings.testIamPermissionsSettings.toBuilder();

      unaryMethodSettingsBuilders =
          ImmutableList.<UnaryCallSettings.Builder<?, ?>>of(
              listInstanceConfigsSettings,
              getInstanceConfigSettings,
              createInstanceConfigSettings,
              updateInstanceConfigSettings,
              deleteInstanceConfigSettings,
              listInstanceConfigOperationsSettings,
              listInstancesSettings,
              getInstanceSettings,
              createInstanceSettings,
              updateInstanceSettings,
              deleteInstanceSettings,
              setIamPolicySettings,
              getIamPolicySettings,
              testIamPermissionsSettings);
    }

    private static Builder createDefault() {
      Builder builder = new Builder(((ClientContext) null));

      builder.setTransportChannelProvider(defaultTransportChannelProvider());
      builder.setCredentialsProvider(defaultCredentialsProviderBuilder().build());
      builder.setInternalHeaderProvider(defaultApiClientHeaderProviderBuilder().build());
      builder.setMtlsEndpoint(getDefaultMtlsEndpoint());
      builder.setSwitchToMtlsEndpointAllowed(true);

      return initDefaults(builder);
    }

    private static Builder createHttpJsonDefault() {
      Builder builder = new Builder(((ClientContext) null));

      builder.setTransportChannelProvider(defaultHttpJsonTransportProviderBuilder().build());
      builder.setCredentialsProvider(defaultCredentialsProviderBuilder().build());
      builder.setInternalHeaderProvider(defaultHttpJsonApiClientHeaderProviderBuilder().build());
      builder.setMtlsEndpoint(getDefaultMtlsEndpoint());
      builder.setSwitchToMtlsEndpointAllowed(true);

      return initDefaults(builder);
    }

    private static Builder initDefaults(Builder builder) {
      builder
          .listInstanceConfigsSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("retry_policy_0_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("retry_policy_0_params"));

      builder
          .getInstanceConfigSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("retry_policy_0_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("retry_policy_0_params"));

      builder
          .createInstanceConfigSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_params"));

      builder
          .updateInstanceConfigSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_params"));

      builder
          .deleteInstanceConfigSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_params"));

      builder
          .listInstanceConfigOperationsSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_params"));

      builder
          .listInstancesSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("retry_policy_0_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("retry_policy_0_params"));

      builder
          .getInstanceSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("retry_policy_0_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("retry_policy_0_params"));

      builder
          .createInstanceSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_2_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_2_params"));

      builder
          .updateInstanceSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_2_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_2_params"));

      builder
          .deleteInstanceSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("retry_policy_0_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("retry_policy_0_params"));

      builder
          .setIamPolicySettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_3_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_3_params"));

      builder
          .getIamPolicySettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("retry_policy_1_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("retry_policy_1_params"));

      builder
          .testIamPermissionsSettings()
          .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_3_codes"))
          .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_3_params"));

      builder
          .createInstanceConfigOperationSettings()
          .setInitialCallSettings(
              UnaryCallSettings
                  .<CreateInstanceConfigRequest, OperationSnapshot>newUnaryCallSettingsBuilder()
                  .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_codes"))
                  .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_params"))
                  .build())
          .setResponseTransformer(
              ProtoOperationTransformers.ResponseTransformer.create(InstanceConfig.class))
          .setMetadataTransformer(
              ProtoOperationTransformers.MetadataTransformer.create(
                  CreateInstanceConfigMetadata.class))
          .setPollingAlgorithm(
              OperationTimedPollAlgorithm.create(
                  RetrySettings.newBuilder()
                      .setInitialRetryDelay(Duration.ofMillis(5000L))
                      .setRetryDelayMultiplier(1.5)
                      .setMaxRetryDelay(Duration.ofMillis(45000L))
                      .setInitialRpcTimeout(Duration.ZERO)
                      .setRpcTimeoutMultiplier(1.0)
                      .setMaxRpcTimeout(Duration.ZERO)
                      .setTotalTimeout(Duration.ofMillis(300000L))
                      .build()));

      builder
          .updateInstanceConfigOperationSettings()
          .setInitialCallSettings(
              UnaryCallSettings
                  .<UpdateInstanceConfigRequest, OperationSnapshot>newUnaryCallSettingsBuilder()
                  .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_codes"))
                  .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_params"))
                  .build())
          .setResponseTransformer(
              ProtoOperationTransformers.ResponseTransformer.create(InstanceConfig.class))
          .setMetadataTransformer(
              ProtoOperationTransformers.MetadataTransformer.create(
                  UpdateInstanceConfigMetadata.class))
          .setPollingAlgorithm(
              OperationTimedPollAlgorithm.create(
                  RetrySettings.newBuilder()
                      .setInitialRetryDelay(Duration.ofMillis(5000L))
                      .setRetryDelayMultiplier(1.5)
                      .setMaxRetryDelay(Duration.ofMillis(45000L))
                      .setInitialRpcTimeout(Duration.ZERO)
                      .setRpcTimeoutMultiplier(1.0)
                      .setMaxRpcTimeout(Duration.ZERO)
                      .setTotalTimeout(Duration.ofMillis(300000L))
                      .build()));

      builder
          .createInstanceOperationSettings()
          .setInitialCallSettings(
              UnaryCallSettings
                  .<CreateInstanceRequest, OperationSnapshot>newUnaryCallSettingsBuilder()
                  .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_2_codes"))
                  .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_2_params"))
                  .build())
          .setResponseTransformer(
              ProtoOperationTransformers.ResponseTransformer.create(Instance.class))
          .setMetadataTransformer(
              ProtoOperationTransformers.MetadataTransformer.create(CreateInstanceMetadata.class))
          .setPollingAlgorithm(
              OperationTimedPollAlgorithm.create(
                  RetrySettings.newBuilder()
                      .setInitialRetryDelay(Duration.ofMillis(20000L))
                      .setRetryDelayMultiplier(1.5)
                      .setMaxRetryDelay(Duration.ofMillis(45000L))
                      .setInitialRpcTimeout(Duration.ZERO)
                      .setRpcTimeoutMultiplier(1.0)
                      .setMaxRpcTimeout(Duration.ZERO)
                      .setTotalTimeout(Duration.ofMillis(86400000L))
                      .build()));

      builder
          .updateInstanceOperationSettings()
          .setInitialCallSettings(
              UnaryCallSettings
                  .<UpdateInstanceRequest, OperationSnapshot>newUnaryCallSettingsBuilder()
                  .setRetryableCodes(RETRYABLE_CODE_DEFINITIONS.get("no_retry_2_codes"))
                  .setRetrySettings(RETRY_PARAM_DEFINITIONS.get("no_retry_2_params"))
                  .build())
          .setResponseTransformer(
              ProtoOperationTransformers.ResponseTransformer.create(Instance.class))
          .setMetadataTransformer(
              ProtoOperationTransformers.MetadataTransformer.create(UpdateInstanceMetadata.class))
          .setPollingAlgorithm(
              OperationTimedPollAlgorithm.create(
                  RetrySettings.newBuilder()
                      .setInitialRetryDelay(Duration.ofMillis(20000L))
                      .setRetryDelayMultiplier(1.5)
                      .setMaxRetryDelay(Duration.ofMillis(45000L))
                      .setInitialRpcTimeout(Duration.ZERO)
                      .setRpcTimeoutMultiplier(1.0)
                      .setMaxRpcTimeout(Duration.ZERO)
                      .setTotalTimeout(Duration.ofMillis(86400000L))
                      .build()));

      return builder;
    }

    /**
     * Applies the given settings updater function to all of the unary API methods in this service.
     *
     * <p>Note: This method does not support applying settings to streaming methods.
     */
    public Builder applyToAllUnaryMethods(
        ApiFunction<UnaryCallSettings.Builder<?, ?>, Void> settingsUpdater) {
      super.applyToAllUnaryMethods(unaryMethodSettingsBuilders, settingsUpdater);
      return this;
    }

    public ImmutableList<UnaryCallSettings.Builder<?, ?>> unaryMethodSettingsBuilders() {
      return unaryMethodSettingsBuilders;
    }

    /** Returns the builder for the settings used for calls to listInstanceConfigs. */
    public PagedCallSettings.Builder<
            ListInstanceConfigsRequest,
            ListInstanceConfigsResponse,
            ListInstanceConfigsPagedResponse>
        listInstanceConfigsSettings() {
      return listInstanceConfigsSettings;
    }

    /** Returns the builder for the settings used for calls to getInstanceConfig. */
    public UnaryCallSettings.Builder<GetInstanceConfigRequest, InstanceConfig>
        getInstanceConfigSettings() {
      return getInstanceConfigSettings;
    }

    /** Returns the builder for the settings used for calls to createInstanceConfig. */
    public UnaryCallSettings.Builder<CreateInstanceConfigRequest, Operation>
        createInstanceConfigSettings() {
      return createInstanceConfigSettings;
    }

    /** Returns the builder for the settings used for calls to createInstanceConfig. */
    @BetaApi(
        "The surface for use by generated code is not stable yet and may change in the future.")
    public OperationCallSettings.Builder<
            CreateInstanceConfigRequest, InstanceConfig, CreateInstanceConfigMetadata>
        createInstanceConfigOperationSettings() {
      return createInstanceConfigOperationSettings;
    }

    /** Returns the builder for the settings used for calls to updateInstanceConfig. */
    public UnaryCallSettings.Builder<UpdateInstanceConfigRequest, Operation>
        updateInstanceConfigSettings() {
      return updateInstanceConfigSettings;
    }

    /** Returns the builder for the settings used for calls to updateInstanceConfig. */
    @BetaApi(
        "The surface for use by generated code is not stable yet and may change in the future.")
    public OperationCallSettings.Builder<
            UpdateInstanceConfigRequest, InstanceConfig, UpdateInstanceConfigMetadata>
        updateInstanceConfigOperationSettings() {
      return updateInstanceConfigOperationSettings;
    }

    /** Returns the builder for the settings used for calls to deleteInstanceConfig. */
    public UnaryCallSettings.Builder<DeleteInstanceConfigRequest, Empty>
        deleteInstanceConfigSettings() {
      return deleteInstanceConfigSettings;
    }

    /** Returns the builder for the settings used for calls to listInstanceConfigOperations. */
    public PagedCallSettings.Builder<
            ListInstanceConfigOperationsRequest,
            ListInstanceConfigOperationsResponse,
            ListInstanceConfigOperationsPagedResponse>
        listInstanceConfigOperationsSettings() {
      return listInstanceConfigOperationsSettings;
    }

    /** Returns the builder for the settings used for calls to listInstances. */
    public PagedCallSettings.Builder<
            ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>
        listInstancesSettings() {
      return listInstancesSettings;
    }

    /** Returns the builder for the settings used for calls to getInstance. */
    public UnaryCallSettings.Builder<GetInstanceRequest, Instance> getInstanceSettings() {
      return getInstanceSettings;
    }

    /** Returns the builder for the settings used for calls to createInstance. */
    public UnaryCallSettings.Builder<CreateInstanceRequest, Operation> createInstanceSettings() {
      return createInstanceSettings;
    }

    /** Returns the builder for the settings used for calls to createInstance. */
    @BetaApi(
        "The surface for use by generated code is not stable yet and may change in the future.")
    public OperationCallSettings.Builder<CreateInstanceRequest, Instance, CreateInstanceMetadata>
        createInstanceOperationSettings() {
      return createInstanceOperationSettings;
    }

    /** Returns the builder for the settings used for calls to updateInstance. */
    public UnaryCallSettings.Builder<UpdateInstanceRequest, Operation> updateInstanceSettings() {
      return updateInstanceSettings;
    }

    /** Returns the builder for the settings used for calls to updateInstance. */
    @BetaApi(
        "The surface for use by generated code is not stable yet and may change in the future.")
    public OperationCallSettings.Builder<UpdateInstanceRequest, Instance, UpdateInstanceMetadata>
        updateInstanceOperationSettings() {
      return updateInstanceOperationSettings;
    }

    /** Returns the builder for the settings used for calls to deleteInstance. */
    public UnaryCallSettings.Builder<DeleteInstanceRequest, Empty> deleteInstanceSettings() {
      return deleteInstanceSettings;
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

    /** Returns the endpoint set by the user or the the service's default endpoint. */
    @Override
    public String getEndpoint() {
      if (super.getEndpoint() != null) {
        return super.getEndpoint();
      }
      return getDefaultEndpoint();
    }

    @Override
    public InstanceAdminStubSettings build() throws IOException {
      return new InstanceAdminStubSettings(this);
    }
  }
}
