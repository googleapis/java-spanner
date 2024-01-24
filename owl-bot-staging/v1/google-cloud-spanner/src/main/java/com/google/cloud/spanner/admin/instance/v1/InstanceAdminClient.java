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

package com.google.cloud.spanner.admin.instance.v1;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.BetaApi;
import com.google.api.gax.core.BackgroundResource;
import com.google.api.gax.httpjson.longrunning.OperationsClient;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.AbstractFixedSizeCollection;
import com.google.api.gax.paging.AbstractPage;
import com.google.api.gax.paging.AbstractPagedListResponse;
import com.google.api.gax.rpc.OperationCallable;
import com.google.api.gax.rpc.PageContext;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.api.resourcenames.ResourceName;
import com.google.cloud.spanner.admin.instance.v1.stub.InstanceAdminStub;
import com.google.cloud.spanner.admin.instance.v1.stub.InstanceAdminStubSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.longrunning.Operation;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
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
import com.google.spanner.admin.instance.v1.InstanceConfigName;
import com.google.spanner.admin.instance.v1.InstanceName;
import com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsRequest;
import com.google.spanner.admin.instance.v1.ListInstanceConfigOperationsResponse;
import com.google.spanner.admin.instance.v1.ListInstanceConfigsRequest;
import com.google.spanner.admin.instance.v1.ListInstanceConfigsResponse;
import com.google.spanner.admin.instance.v1.ListInstancesRequest;
import com.google.spanner.admin.instance.v1.ListInstancesResponse;
import com.google.spanner.admin.instance.v1.ProjectName;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata;
import com.google.spanner.admin.instance.v1.UpdateInstanceConfigRequest;
import com.google.spanner.admin.instance.v1.UpdateInstanceMetadata;
import com.google.spanner.admin.instance.v1.UpdateInstanceRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Generated;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * Service Description: Cloud Spanner Instance Admin API
 *
 * <p>The Cloud Spanner Instance Admin API can be used to create, delete, modify and list instances.
 * Instances are dedicated Cloud Spanner serving and storage resources to be used by Cloud Spanner
 * databases.
 *
 * <p>Each instance has a "configuration", which dictates where the serving resources for the Cloud
 * Spanner instance are located (e.g., US-central, Europe). Configurations are created by Google
 * based on resource availability.
 *
 * <p>Cloud Spanner billing is based on the instances that exist and their sizes. After an instance
 * exists, there are no additional per-database or per-operation charges for use of the instance
 * (though there may be additional network bandwidth charges). Instances offer isolation: problems
 * with databases in one instance will not affect other instances. However, within an instance
 * databases can affect each other. For example, if one database in an instance receives a lot of
 * requests and consumes most of the instance resources, fewer resources are available for other
 * databases in that instance, and their performance may suffer.
 *
 * <p>This class provides the ability to make remote calls to the backing service through method
 * calls that map to API methods. Sample code to get started:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
 *   InstanceConfigName name = InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]");
 *   InstanceConfig response = instanceAdminClient.getInstanceConfig(name);
 * }
 * }</pre>
 *
 * <p>Note: close() needs to be called on the InstanceAdminClient object to clean up resources such
 * as threads. In the example above, try-with-resources is used, which automatically calls close().
 *
 * <table>
 *    <caption>Methods</caption>
 *    <tr>
 *      <th>Method</th>
 *      <th>Description</th>
 *      <th>Method Variants</th>
 *    </tr>
 *    <tr>
 *      <td><p> ListInstanceConfigs</td>
 *      <td><p> Lists the supported instance configurations for a given project.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> listInstanceConfigs(ListInstanceConfigsRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> listInstanceConfigs(ProjectName parent)
 *           <li><p> listInstanceConfigs(String parent)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> listInstanceConfigsPagedCallable()
 *           <li><p> listInstanceConfigsCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> GetInstanceConfig</td>
 *      <td><p> Gets information about a particular instance configuration.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> getInstanceConfig(GetInstanceConfigRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> getInstanceConfig(InstanceConfigName name)
 *           <li><p> getInstanceConfig(String name)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> getInstanceConfigCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> CreateInstanceConfig</td>
 *      <td><p> Creates an instance config and begins preparing it to be used. The returned [long-running operation][google.longrunning.Operation] can be used to track the progress of preparing the new instance config. The instance config name is assigned by the caller. If the named instance config already exists, `CreateInstanceConfig` returns `ALREADY_EXISTS`.
 * <p>  Immediately after the request returns:
 * <p>    &#42; The instance config is readable via the API, with all requested     attributes. The instance config's     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]     field is set to true. Its state is `CREATING`.
 * <p>  While the operation is pending:
 * <p>    &#42; Cancelling the operation renders the instance config immediately     unreadable via the API.   &#42; Except for deleting the creating resource, all other attempts to modify     the instance config are rejected.
 * <p>  Upon completion of the returned operation:
 * <p>    &#42; Instances can be created using the instance configuration.   &#42; The instance config's   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]   field becomes false. Its state becomes `READY`.
 * <p>  The returned [long-running operation][google.longrunning.Operation] will have a name of the format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track creation of the instance config. The [metadata][google.longrunning.Operation.metadata] field type is [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata]. The [response][google.longrunning.Operation.response] field type is [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
 * <p>  Authorization requires `spanner.instanceConfigs.create` permission on the resource [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> createInstanceConfigAsync(CreateInstanceConfigRequest request)
 *      </ul>
 *      <p>Methods that return long-running operations have "Async" method variants that return `OperationFuture`, which is used to track polling of the service.</p>
 *      <ul>
 *           <li><p> createInstanceConfigAsync(ProjectName parent, InstanceConfig instanceConfig, String instanceConfigId)
 *           <li><p> createInstanceConfigAsync(String parent, InstanceConfig instanceConfig, String instanceConfigId)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> createInstanceConfigOperationCallable()
 *           <li><p> createInstanceConfigCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> UpdateInstanceConfig</td>
 *      <td><p> Updates an instance config. The returned [long-running operation][google.longrunning.Operation] can be used to track the progress of updating the instance. If the named instance config does not exist, returns `NOT_FOUND`.
 * <p>  Only user managed configurations can be updated.
 * <p>  Immediately after the request returns:
 * <p>    &#42; The instance config's     [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]     field is set to true.
 * <p>  While the operation is pending:
 * <p>    &#42; Cancelling the operation sets its metadata's     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time].     The operation is guaranteed to succeed at undoing all changes, after     which point it terminates with a `CANCELLED` status.   &#42; All other attempts to modify the instance config are rejected.   &#42; Reading the instance config via the API continues to give the     pre-request values.
 * <p>  Upon completion of the returned operation:
 * <p>    &#42; Creating instances using the instance configuration uses the new     values.   &#42; The instance config's new values are readable via the API.   &#42; The instance config's   [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]   field becomes false.
 * <p>  The returned [long-running operation][google.longrunning.Operation] will have a name of the format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track the instance config modification.  The [metadata][google.longrunning.Operation.metadata] field type is [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata]. The [response][google.longrunning.Operation.response] field type is [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
 * <p>  Authorization requires `spanner.instanceConfigs.update` permission on the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> updateInstanceConfigAsync(UpdateInstanceConfigRequest request)
 *      </ul>
 *      <p>Methods that return long-running operations have "Async" method variants that return `OperationFuture`, which is used to track polling of the service.</p>
 *      <ul>
 *           <li><p> updateInstanceConfigAsync(InstanceConfig instanceConfig, FieldMask updateMask)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> updateInstanceConfigOperationCallable()
 *           <li><p> updateInstanceConfigCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> DeleteInstanceConfig</td>
 *      <td><p> Deletes the instance config. Deletion is only allowed when no instances are using the configuration. If any instances are using the config, returns `FAILED_PRECONDITION`.
 * <p>  Only user managed configurations can be deleted.
 * <p>  Authorization requires `spanner.instanceConfigs.delete` permission on the resource [name][google.spanner.admin.instance.v1.InstanceConfig.name].</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> deleteInstanceConfig(DeleteInstanceConfigRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> deleteInstanceConfig(InstanceConfigName name)
 *           <li><p> deleteInstanceConfig(String name)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> deleteInstanceConfigCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> ListInstanceConfigOperations</td>
 *      <td><p> Lists the user-managed instance config [long-running operations][google.longrunning.Operation] in the given project. An instance config operation has a name of the form `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`. The long-running operation [metadata][google.longrunning.Operation.metadata] field type `metadata.type_url` describes the type of the metadata. Operations returned include those that have completed/failed/canceled within the last 7 days, and pending operations. Operations returned are ordered by `operation.metadata.value.start_time` in descending order starting from the most recently started operation.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> listInstanceConfigOperations(ListInstanceConfigOperationsRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> listInstanceConfigOperations(ProjectName parent)
 *           <li><p> listInstanceConfigOperations(String parent)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> listInstanceConfigOperationsPagedCallable()
 *           <li><p> listInstanceConfigOperationsCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> ListInstances</td>
 *      <td><p> Lists all instances in the given project.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> listInstances(ListInstancesRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> listInstances(ProjectName parent)
 *           <li><p> listInstances(String parent)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> listInstancesPagedCallable()
 *           <li><p> listInstancesCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> GetInstance</td>
 *      <td><p> Gets information about a particular instance.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> getInstance(GetInstanceRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> getInstance(InstanceName name)
 *           <li><p> getInstance(String name)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> getInstanceCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> CreateInstance</td>
 *      <td><p> Creates an instance and begins preparing it to begin serving. The returned [long-running operation][google.longrunning.Operation] can be used to track the progress of preparing the new instance. The instance name is assigned by the caller. If the named instance already exists, `CreateInstance` returns `ALREADY_EXISTS`.
 * <p>  Immediately upon completion of this request:
 * <p>    &#42; The instance is readable via the API, with all requested attributes     but no allocated resources. Its state is `CREATING`.
 * <p>  Until completion of the returned operation:
 * <p>    &#42; Cancelling the operation renders the instance immediately unreadable     via the API.   &#42; The instance can be deleted.   &#42; All other attempts to modify the instance are rejected.
 * <p>  Upon completion of the returned operation:
 * <p>    &#42; Billing for all successfully-allocated resources begins (some types     may have lower than the requested levels).   &#42; Databases can be created in the instance.   &#42; The instance's allocated resource levels are readable via the API.   &#42; The instance's state becomes `READY`.
 * <p>  The returned [long-running operation][google.longrunning.Operation] will have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track creation of the instance.  The [metadata][google.longrunning.Operation.metadata] field type is [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata]. The [response][google.longrunning.Operation.response] field type is [Instance][google.spanner.admin.instance.v1.Instance], if successful.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> createInstanceAsync(CreateInstanceRequest request)
 *      </ul>
 *      <p>Methods that return long-running operations have "Async" method variants that return `OperationFuture`, which is used to track polling of the service.</p>
 *      <ul>
 *           <li><p> createInstanceAsync(ProjectName parent, String instanceId, Instance instance)
 *           <li><p> createInstanceAsync(String parent, String instanceId, Instance instance)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> createInstanceOperationCallable()
 *           <li><p> createInstanceCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> UpdateInstance</td>
 *      <td><p> Updates an instance, and begins allocating or releasing resources as requested. The returned [long-running operation][google.longrunning.Operation] can be used to track the progress of updating the instance. If the named instance does not exist, returns `NOT_FOUND`.
 * <p>  Immediately upon completion of this request:
 * <p>    &#42; For resource types for which a decrease in the instance's allocation     has been requested, billing is based on the newly-requested level.
 * <p>  Until completion of the returned operation:
 * <p>    &#42; Cancelling the operation sets its metadata's     [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time],     and begins restoring resources to their pre-request values. The     operation is guaranteed to succeed at undoing all resource changes,     after which point it terminates with a `CANCELLED` status.   &#42; All other attempts to modify the instance are rejected.   &#42; Reading the instance via the API continues to give the pre-request     resource levels.
 * <p>  Upon completion of the returned operation:
 * <p>    &#42; Billing begins for all successfully-allocated resources (some types     may have lower than the requested levels).   &#42; All newly-reserved resources are available for serving the instance's     tables.   &#42; The instance's new resource levels are readable via the API.
 * <p>  The returned [long-running operation][google.longrunning.Operation] will have a name of the format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track the instance modification.  The [metadata][google.longrunning.Operation.metadata] field type is [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata]. The [response][google.longrunning.Operation.response] field type is [Instance][google.spanner.admin.instance.v1.Instance], if successful.
 * <p>  Authorization requires `spanner.instances.update` permission on the resource [name][google.spanner.admin.instance.v1.Instance.name].</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> updateInstanceAsync(UpdateInstanceRequest request)
 *      </ul>
 *      <p>Methods that return long-running operations have "Async" method variants that return `OperationFuture`, which is used to track polling of the service.</p>
 *      <ul>
 *           <li><p> updateInstanceAsync(Instance instance, FieldMask fieldMask)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> updateInstanceOperationCallable()
 *           <li><p> updateInstanceCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> DeleteInstance</td>
 *      <td><p> Deletes an instance.
 * <p>  Immediately upon completion of the request:
 * <p>    &#42; Billing ceases for all of the instance's reserved resources.
 * <p>  Soon afterward:
 * <p>    &#42; The instance and &#42;all of its databases&#42; immediately and     irrevocably disappear from the API. All data in the databases     is permanently deleted.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> deleteInstance(DeleteInstanceRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> deleteInstance(InstanceName name)
 *           <li><p> deleteInstance(String name)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> deleteInstanceCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> SetIamPolicy</td>
 *      <td><p> Sets the access control policy on an instance resource. Replaces any existing policy.
 * <p>  Authorization requires `spanner.instances.setIamPolicy` on [resource][google.iam.v1.SetIamPolicyRequest.resource].</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> setIamPolicy(SetIamPolicyRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> setIamPolicy(ResourceName resource, Policy policy)
 *           <li><p> setIamPolicy(String resource, Policy policy)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> setIamPolicyCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> GetIamPolicy</td>
 *      <td><p> Gets the access control policy for an instance resource. Returns an empty policy if an instance exists but does not have a policy set.
 * <p>  Authorization requires `spanner.instances.getIamPolicy` on [resource][google.iam.v1.GetIamPolicyRequest.resource].</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> getIamPolicy(GetIamPolicyRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> getIamPolicy(ResourceName resource)
 *           <li><p> getIamPolicy(String resource)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> getIamPolicyCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *    <tr>
 *      <td><p> TestIamPermissions</td>
 *      <td><p> Returns permissions that the caller has on the specified instance resource.
 * <p>  Attempting this RPC on a non-existent Cloud Spanner instance resource will result in a NOT_FOUND error if the user has `spanner.instances.list` permission on the containing Google Cloud Project. Otherwise returns an empty set of permissions.</td>
 *      <td>
 *      <p>Request object method variants only take one parameter, a request object, which must be constructed before the call.</p>
 *      <ul>
 *           <li><p> testIamPermissions(TestIamPermissionsRequest request)
 *      </ul>
 *      <p>"Flattened" method variants have converted the fields of the request object into function parameters to enable multiple ways to call the same method.</p>
 *      <ul>
 *           <li><p> testIamPermissions(ResourceName resource, List&lt;String&gt; permissions)
 *           <li><p> testIamPermissions(String resource, List&lt;String&gt; permissions)
 *      </ul>
 *      <p>Callable method variants take no parameters and return an immutable API callable object, which can be used to initiate calls to the service.</p>
 *      <ul>
 *           <li><p> testIamPermissionsCallable()
 *      </ul>
 *       </td>
 *    </tr>
 *  </table>
 *
 * <p>See the individual methods for example code.
 *
 * <p>Many parameters require resource names to be formatted in a particular way. To assist with
 * these names, this class includes a format method for each type of name, and additionally a parse
 * method to extract the individual identifiers contained within names that are returned.
 *
 * <p>This class can be customized by passing in a custom instance of InstanceAdminSettings to
 * create(). For example:
 *
 * <p>To customize credentials:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * InstanceAdminSettings instanceAdminSettings =
 *     InstanceAdminSettings.newBuilder()
 *         .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
 *         .build();
 * InstanceAdminClient instanceAdminClient = InstanceAdminClient.create(instanceAdminSettings);
 * }</pre>
 *
 * <p>To customize the endpoint:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * InstanceAdminSettings instanceAdminSettings =
 *     InstanceAdminSettings.newBuilder().setEndpoint(myEndpoint).build();
 * InstanceAdminClient instanceAdminClient = InstanceAdminClient.create(instanceAdminSettings);
 * }</pre>
 *
 * <p>To use REST (HTTP1.1/JSON) transport (instead of gRPC) for sending and receiving requests over
 * the wire:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * InstanceAdminSettings instanceAdminSettings =
 *     InstanceAdminSettings.newHttpJsonBuilder().build();
 * InstanceAdminClient instanceAdminClient = InstanceAdminClient.create(instanceAdminSettings);
 * }</pre>
 *
 * <p>Please refer to the GitHub repository's samples for more quickstart code snippets.
 */
@Generated("by gapic-generator-java")
public class InstanceAdminClient implements BackgroundResource {
  private final InstanceAdminSettings settings;
  private final InstanceAdminStub stub;
  private final OperationsClient httpJsonOperationsClient;
  private final com.google.longrunning.OperationsClient operationsClient;

  /** Constructs an instance of InstanceAdminClient with default settings. */
  public static final InstanceAdminClient create() throws IOException {
    return create(InstanceAdminSettings.newBuilder().build());
  }

  /**
   * Constructs an instance of InstanceAdminClient, using the given settings. The channels are
   * created based on the settings passed in, or defaults for any settings that are not set.
   */
  public static final InstanceAdminClient create(InstanceAdminSettings settings)
      throws IOException {
    return new InstanceAdminClient(settings);
  }

  /**
   * Constructs an instance of InstanceAdminClient, using the given stub for making calls. This is
   * for advanced usage - prefer using create(InstanceAdminSettings).
   */
  public static final InstanceAdminClient create(InstanceAdminStub stub) {
    return new InstanceAdminClient(stub);
  }

  /**
   * Constructs an instance of InstanceAdminClient, using the given settings. This is protected so
   * that it is easy to make a subclass, but otherwise, the static factory methods should be
   * preferred.
   */
  protected InstanceAdminClient(InstanceAdminSettings settings) throws IOException {
    this.settings = settings;
    this.stub = ((InstanceAdminStubSettings) settings.getStubSettings()).createStub();
    this.operationsClient =
        com.google.longrunning.OperationsClient.create(this.stub.getOperationsStub());
    this.httpJsonOperationsClient = OperationsClient.create(this.stub.getHttpJsonOperationsStub());
  }

  protected InstanceAdminClient(InstanceAdminStub stub) {
    this.settings = null;
    this.stub = stub;
    this.operationsClient =
        com.google.longrunning.OperationsClient.create(this.stub.getOperationsStub());
    this.httpJsonOperationsClient = OperationsClient.create(this.stub.getHttpJsonOperationsStub());
  }

  public final InstanceAdminSettings getSettings() {
    return settings;
  }

  public InstanceAdminStub getStub() {
    return stub;
  }

  /**
   * Returns the OperationsClient that can be used to query the status of a long-running operation
   * returned by another API method call.
   */
  public final com.google.longrunning.OperationsClient getOperationsClient() {
    return operationsClient;
  }

  /**
   * Returns the OperationsClient that can be used to query the status of a long-running operation
   * returned by another API method call.
   */
  @BetaApi
  public final OperationsClient getHttpJsonOperationsClient() {
    return httpJsonOperationsClient;
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the supported instance configurations for a given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ProjectName parent = ProjectName.of("[PROJECT]");
   *   for (InstanceConfig element : instanceAdminClient.listInstanceConfigs(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project for which a list of supported instance
   *     configurations is requested. Values are of the form `projects/&lt;project&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstanceConfigsPagedResponse listInstanceConfigs(ProjectName parent) {
    ListInstanceConfigsRequest request =
        ListInstanceConfigsRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listInstanceConfigs(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the supported instance configurations for a given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String parent = ProjectName.of("[PROJECT]").toString();
   *   for (InstanceConfig element : instanceAdminClient.listInstanceConfigs(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project for which a list of supported instance
   *     configurations is requested. Values are of the form `projects/&lt;project&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstanceConfigsPagedResponse listInstanceConfigs(String parent) {
    ListInstanceConfigsRequest request =
        ListInstanceConfigsRequest.newBuilder().setParent(parent).build();
    return listInstanceConfigs(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the supported instance configurations for a given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstanceConfigsRequest request =
   *       ListInstanceConfigsRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   for (InstanceConfig element : instanceAdminClient.listInstanceConfigs(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstanceConfigsPagedResponse listInstanceConfigs(
      ListInstanceConfigsRequest request) {
    return listInstanceConfigsPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the supported instance configurations for a given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstanceConfigsRequest request =
   *       ListInstanceConfigsRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   ApiFuture<InstanceConfig> future =
   *       instanceAdminClient.listInstanceConfigsPagedCallable().futureCall(request);
   *   // Do something.
   *   for (InstanceConfig element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListInstanceConfigsRequest, ListInstanceConfigsPagedResponse>
      listInstanceConfigsPagedCallable() {
    return stub.listInstanceConfigsPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the supported instance configurations for a given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstanceConfigsRequest request =
   *       ListInstanceConfigsRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   while (true) {
   *     ListInstanceConfigsResponse response =
   *         instanceAdminClient.listInstanceConfigsCallable().call(request);
   *     for (InstanceConfig element : response.getInstanceConfigsList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListInstanceConfigsRequest, ListInstanceConfigsResponse>
      listInstanceConfigsCallable() {
    return stub.listInstanceConfigsCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance configuration.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   InstanceConfigName name = InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]");
   *   InstanceConfig response = instanceAdminClient.getInstanceConfig(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the requested instance configuration. Values are of the form
   *     `projects/&lt;project&gt;/instanceConfigs/&lt;config&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final InstanceConfig getInstanceConfig(InstanceConfigName name) {
    GetInstanceConfigRequest request =
        GetInstanceConfigRequest.newBuilder()
            .setName(name == null ? null : name.toString())
            .build();
    return getInstanceConfig(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance configuration.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String name = InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]").toString();
   *   InstanceConfig response = instanceAdminClient.getInstanceConfig(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the requested instance configuration. Values are of the form
   *     `projects/&lt;project&gt;/instanceConfigs/&lt;config&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final InstanceConfig getInstanceConfig(String name) {
    GetInstanceConfigRequest request = GetInstanceConfigRequest.newBuilder().setName(name).build();
    return getInstanceConfig(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance configuration.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   GetInstanceConfigRequest request =
   *       GetInstanceConfigRequest.newBuilder()
   *           .setName(InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]").toString())
   *           .build();
   *   InstanceConfig response = instanceAdminClient.getInstanceConfig(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final InstanceConfig getInstanceConfig(GetInstanceConfigRequest request) {
    return getInstanceConfigCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance configuration.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   GetInstanceConfigRequest request =
   *       GetInstanceConfigRequest.newBuilder()
   *           .setName(InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]").toString())
   *           .build();
   *   ApiFuture<InstanceConfig> future =
   *       instanceAdminClient.getInstanceConfigCallable().futureCall(request);
   *   // Do something.
   *   InstanceConfig response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetInstanceConfigRequest, InstanceConfig> getInstanceConfigCallable() {
    return stub.getInstanceConfigCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance config and begins preparing it to be used. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance config. The instance config name is assigned by the caller. If the named instance
   * config already exists, `CreateInstanceConfig` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config is readable via the API, with all requested attributes. The
   * instance config's [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
   * field is set to true. Its state is `CREATING`.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation renders the instance config immediately unreadable via the
   * API. &#42; Except for deleting the creating resource, all other attempts to modify the instance
   * config are rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Instances can be created using the instance configuration. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   * Its state becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance config. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.create` permission on the resource
   * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ProjectName parent = ProjectName.of("[PROJECT]");
   *   InstanceConfig instanceConfig = InstanceConfig.newBuilder().build();
   *   String instanceConfigId = "instanceConfigId1750947762";
   *   InstanceConfig response =
   *       instanceAdminClient
   *           .createInstanceConfigAsync(parent, instanceConfig, instanceConfigId)
   *           .get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project in which to create the instance config. Values
   *     are of the form `projects/&lt;project&gt;`.
   * @param instanceConfig Required. The InstanceConfig proto of the configuration to create.
   *     instance_config.name must be `&lt;parent&gt;/instanceConfigs/&lt;instance_config_id&gt;`.
   *     instance_config.base_config must be a Google managed configuration name, e.g.
   *     &lt;parent&gt;/instanceConfigs/us-east1, &lt;parent&gt;/instanceConfigs/nam3.
   * @param instanceConfigId Required. The ID of the instance config to create. Valid identifiers
   *     are of the form `custom-[-a-z0-9]&#42;[a-z0-9]` and must be between 2 and 64 characters in
   *     length. The `custom-` prefix is required to avoid name conflicts with Google managed
   *     configurations.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<InstanceConfig, CreateInstanceConfigMetadata>
      createInstanceConfigAsync(
          ProjectName parent, InstanceConfig instanceConfig, String instanceConfigId) {
    CreateInstanceConfigRequest request =
        CreateInstanceConfigRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setInstanceConfig(instanceConfig)
            .setInstanceConfigId(instanceConfigId)
            .build();
    return createInstanceConfigAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance config and begins preparing it to be used. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance config. The instance config name is assigned by the caller. If the named instance
   * config already exists, `CreateInstanceConfig` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config is readable via the API, with all requested attributes. The
   * instance config's [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
   * field is set to true. Its state is `CREATING`.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation renders the instance config immediately unreadable via the
   * API. &#42; Except for deleting the creating resource, all other attempts to modify the instance
   * config are rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Instances can be created using the instance configuration. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   * Its state becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance config. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.create` permission on the resource
   * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String parent = ProjectName.of("[PROJECT]").toString();
   *   InstanceConfig instanceConfig = InstanceConfig.newBuilder().build();
   *   String instanceConfigId = "instanceConfigId1750947762";
   *   InstanceConfig response =
   *       instanceAdminClient
   *           .createInstanceConfigAsync(parent, instanceConfig, instanceConfigId)
   *           .get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project in which to create the instance config. Values
   *     are of the form `projects/&lt;project&gt;`.
   * @param instanceConfig Required. The InstanceConfig proto of the configuration to create.
   *     instance_config.name must be `&lt;parent&gt;/instanceConfigs/&lt;instance_config_id&gt;`.
   *     instance_config.base_config must be a Google managed configuration name, e.g.
   *     &lt;parent&gt;/instanceConfigs/us-east1, &lt;parent&gt;/instanceConfigs/nam3.
   * @param instanceConfigId Required. The ID of the instance config to create. Valid identifiers
   *     are of the form `custom-[-a-z0-9]&#42;[a-z0-9]` and must be between 2 and 64 characters in
   *     length. The `custom-` prefix is required to avoid name conflicts with Google managed
   *     configurations.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<InstanceConfig, CreateInstanceConfigMetadata>
      createInstanceConfigAsync(
          String parent, InstanceConfig instanceConfig, String instanceConfigId) {
    CreateInstanceConfigRequest request =
        CreateInstanceConfigRequest.newBuilder()
            .setParent(parent)
            .setInstanceConfig(instanceConfig)
            .setInstanceConfigId(instanceConfigId)
            .build();
    return createInstanceConfigAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance config and begins preparing it to be used. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance config. The instance config name is assigned by the caller. If the named instance
   * config already exists, `CreateInstanceConfig` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config is readable via the API, with all requested attributes. The
   * instance config's [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
   * field is set to true. Its state is `CREATING`.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation renders the instance config immediately unreadable via the
   * API. &#42; Except for deleting the creating resource, all other attempts to modify the instance
   * config are rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Instances can be created using the instance configuration. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   * Its state becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance config. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.create` permission on the resource
   * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   CreateInstanceConfigRequest request =
   *       CreateInstanceConfigRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setInstanceConfigId("instanceConfigId1750947762")
   *           .setInstanceConfig(InstanceConfig.newBuilder().build())
   *           .setValidateOnly(true)
   *           .build();
   *   InstanceConfig response = instanceAdminClient.createInstanceConfigAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<InstanceConfig, CreateInstanceConfigMetadata>
      createInstanceConfigAsync(CreateInstanceConfigRequest request) {
    return createInstanceConfigOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance config and begins preparing it to be used. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance config. The instance config name is assigned by the caller. If the named instance
   * config already exists, `CreateInstanceConfig` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config is readable via the API, with all requested attributes. The
   * instance config's [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
   * field is set to true. Its state is `CREATING`.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation renders the instance config immediately unreadable via the
   * API. &#42; Except for deleting the creating resource, all other attempts to modify the instance
   * config are rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Instances can be created using the instance configuration. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   * Its state becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance config. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.create` permission on the resource
   * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   CreateInstanceConfigRequest request =
   *       CreateInstanceConfigRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setInstanceConfigId("instanceConfigId1750947762")
   *           .setInstanceConfig(InstanceConfig.newBuilder().build())
   *           .setValidateOnly(true)
   *           .build();
   *   OperationFuture<InstanceConfig, CreateInstanceConfigMetadata> future =
   *       instanceAdminClient.createInstanceConfigOperationCallable().futureCall(request);
   *   // Do something.
   *   InstanceConfig response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<
          CreateInstanceConfigRequest, InstanceConfig, CreateInstanceConfigMetadata>
      createInstanceConfigOperationCallable() {
    return stub.createInstanceConfigOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance config and begins preparing it to be used. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance config. The instance config name is assigned by the caller. If the named instance
   * config already exists, `CreateInstanceConfig` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config is readable via the API, with all requested attributes. The
   * instance config's [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling]
   * field is set to true. Its state is `CREATING`.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation renders the instance config immediately unreadable via the
   * API. &#42; Except for deleting the creating resource, all other attempts to modify the instance
   * config are rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Instances can be created using the instance configuration. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   * Its state becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance config. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [CreateInstanceConfigMetadata][google.spanner.admin.instance.v1.CreateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.create` permission on the resource
   * [parent][google.spanner.admin.instance.v1.CreateInstanceConfigRequest.parent].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   CreateInstanceConfigRequest request =
   *       CreateInstanceConfigRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setInstanceConfigId("instanceConfigId1750947762")
   *           .setInstanceConfig(InstanceConfig.newBuilder().build())
   *           .setValidateOnly(true)
   *           .build();
   *   ApiFuture<Operation> future =
   *       instanceAdminClient.createInstanceConfigCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateInstanceConfigRequest, Operation>
      createInstanceConfigCallable() {
    return stub.createInstanceConfigCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance config. The returned [long-running operation][google.longrunning.Operation]
   * can be used to track the progress of updating the instance. If the named instance config does
   * not exist, returns `NOT_FOUND`.
   *
   * <p>Only user managed configurations can be updated.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field is set to
   * true.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time]. The
   * operation is guaranteed to succeed at undoing all changes, after which point it terminates with
   * a `CANCELLED` status. &#42; All other attempts to modify the instance config are rejected.
   * &#42; Reading the instance config via the API continues to give the pre-request values.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Creating instances using the instance configuration uses the new values. &#42; The
   * instance config's new values are readable via the API. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * the instance config modification. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   InstanceConfig instanceConfig = InstanceConfig.newBuilder().build();
   *   FieldMask updateMask = FieldMask.newBuilder().build();
   *   InstanceConfig response =
   *       instanceAdminClient.updateInstanceConfigAsync(instanceConfig, updateMask).get();
   * }
   * }</pre>
   *
   * @param instanceConfig Required. The user instance config to update, which must always include
   *     the instance config name. Otherwise, only fields mentioned in
   *     [update_mask][google.spanner.admin.instance.v1.UpdateInstanceConfigRequest.update_mask]
   *     need be included. To prevent conflicts of concurrent updates,
   *     [etag][google.spanner.admin.instance.v1.InstanceConfig.reconciling] can be used.
   * @param updateMask Required. A mask specifying which fields in
   *     [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig] should be updated. The
   *     field mask must always be specified; this prevents any future fields in
   *     [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig] from being erased
   *     accidentally by clients that do not know about them. Only display_name and labels can be
   *     updated.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<InstanceConfig, UpdateInstanceConfigMetadata>
      updateInstanceConfigAsync(InstanceConfig instanceConfig, FieldMask updateMask) {
    UpdateInstanceConfigRequest request =
        UpdateInstanceConfigRequest.newBuilder()
            .setInstanceConfig(instanceConfig)
            .setUpdateMask(updateMask)
            .build();
    return updateInstanceConfigAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance config. The returned [long-running operation][google.longrunning.Operation]
   * can be used to track the progress of updating the instance. If the named instance config does
   * not exist, returns `NOT_FOUND`.
   *
   * <p>Only user managed configurations can be updated.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field is set to
   * true.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time]. The
   * operation is guaranteed to succeed at undoing all changes, after which point it terminates with
   * a `CANCELLED` status. &#42; All other attempts to modify the instance config are rejected.
   * &#42; Reading the instance config via the API continues to give the pre-request values.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Creating instances using the instance configuration uses the new values. &#42; The
   * instance config's new values are readable via the API. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * the instance config modification. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   UpdateInstanceConfigRequest request =
   *       UpdateInstanceConfigRequest.newBuilder()
   *           .setInstanceConfig(InstanceConfig.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setValidateOnly(true)
   *           .build();
   *   InstanceConfig response = instanceAdminClient.updateInstanceConfigAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<InstanceConfig, UpdateInstanceConfigMetadata>
      updateInstanceConfigAsync(UpdateInstanceConfigRequest request) {
    return updateInstanceConfigOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance config. The returned [long-running operation][google.longrunning.Operation]
   * can be used to track the progress of updating the instance. If the named instance config does
   * not exist, returns `NOT_FOUND`.
   *
   * <p>Only user managed configurations can be updated.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field is set to
   * true.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time]. The
   * operation is guaranteed to succeed at undoing all changes, after which point it terminates with
   * a `CANCELLED` status. &#42; All other attempts to modify the instance config are rejected.
   * &#42; Reading the instance config via the API continues to give the pre-request values.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Creating instances using the instance configuration uses the new values. &#42; The
   * instance config's new values are readable via the API. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * the instance config modification. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   UpdateInstanceConfigRequest request =
   *       UpdateInstanceConfigRequest.newBuilder()
   *           .setInstanceConfig(InstanceConfig.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setValidateOnly(true)
   *           .build();
   *   OperationFuture<InstanceConfig, UpdateInstanceConfigMetadata> future =
   *       instanceAdminClient.updateInstanceConfigOperationCallable().futureCall(request);
   *   // Do something.
   *   InstanceConfig response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<
          UpdateInstanceConfigRequest, InstanceConfig, UpdateInstanceConfigMetadata>
      updateInstanceConfigOperationCallable() {
    return stub.updateInstanceConfigOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance config. The returned [long-running operation][google.longrunning.Operation]
   * can be used to track the progress of updating the instance. If the named instance config does
   * not exist, returns `NOT_FOUND`.
   *
   * <p>Only user managed configurations can be updated.
   *
   * <p>Immediately after the request returns:
   *
   * <p>&#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field is set to
   * true.
   *
   * <p>While the operation is pending:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata.cancel_time]. The
   * operation is guaranteed to succeed at undoing all changes, after which point it terminates with
   * a `CANCELLED` status. &#42; All other attempts to modify the instance config are rejected.
   * &#42; Reading the instance config via the API continues to give the pre-request values.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Creating instances using the instance configuration uses the new values. &#42; The
   * instance config's new values are readable via the API. &#42; The instance config's
   * [reconciling][google.spanner.admin.instance.v1.InstanceConfig.reconciling] field becomes false.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_config_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * the instance config modification. The [metadata][google.longrunning.Operation.metadata] field
   * type is
   * [UpdateInstanceConfigMetadata][google.spanner.admin.instance.v1.UpdateInstanceConfigMetadata].
   * The [response][google.longrunning.Operation.response] field type is
   * [InstanceConfig][google.spanner.admin.instance.v1.InstanceConfig], if successful.
   *
   * <p>Authorization requires `spanner.instanceConfigs.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   UpdateInstanceConfigRequest request =
   *       UpdateInstanceConfigRequest.newBuilder()
   *           .setInstanceConfig(InstanceConfig.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setValidateOnly(true)
   *           .build();
   *   ApiFuture<Operation> future =
   *       instanceAdminClient.updateInstanceConfigCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateInstanceConfigRequest, Operation>
      updateInstanceConfigCallable() {
    return stub.updateInstanceConfigCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes the instance config. Deletion is only allowed when no instances are using the
   * configuration. If any instances are using the config, returns `FAILED_PRECONDITION`.
   *
   * <p>Only user managed configurations can be deleted.
   *
   * <p>Authorization requires `spanner.instanceConfigs.delete` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   InstanceConfigName name = InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]");
   *   instanceAdminClient.deleteInstanceConfig(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the instance configuration to be deleted. Values are of the
   *     form `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteInstanceConfig(InstanceConfigName name) {
    DeleteInstanceConfigRequest request =
        DeleteInstanceConfigRequest.newBuilder()
            .setName(name == null ? null : name.toString())
            .build();
    deleteInstanceConfig(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes the instance config. Deletion is only allowed when no instances are using the
   * configuration. If any instances are using the config, returns `FAILED_PRECONDITION`.
   *
   * <p>Only user managed configurations can be deleted.
   *
   * <p>Authorization requires `spanner.instanceConfigs.delete` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String name = InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]").toString();
   *   instanceAdminClient.deleteInstanceConfig(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the instance configuration to be deleted. Values are of the
   *     form `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteInstanceConfig(String name) {
    DeleteInstanceConfigRequest request =
        DeleteInstanceConfigRequest.newBuilder().setName(name).build();
    deleteInstanceConfig(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes the instance config. Deletion is only allowed when no instances are using the
   * configuration. If any instances are using the config, returns `FAILED_PRECONDITION`.
   *
   * <p>Only user managed configurations can be deleted.
   *
   * <p>Authorization requires `spanner.instanceConfigs.delete` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   DeleteInstanceConfigRequest request =
   *       DeleteInstanceConfigRequest.newBuilder()
   *           .setName(InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]").toString())
   *           .setEtag("etag3123477")
   *           .setValidateOnly(true)
   *           .build();
   *   instanceAdminClient.deleteInstanceConfig(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteInstanceConfig(DeleteInstanceConfigRequest request) {
    deleteInstanceConfigCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes the instance config. Deletion is only allowed when no instances are using the
   * configuration. If any instances are using the config, returns `FAILED_PRECONDITION`.
   *
   * <p>Only user managed configurations can be deleted.
   *
   * <p>Authorization requires `spanner.instanceConfigs.delete` permission on the resource
   * [name][google.spanner.admin.instance.v1.InstanceConfig.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   DeleteInstanceConfigRequest request =
   *       DeleteInstanceConfigRequest.newBuilder()
   *           .setName(InstanceConfigName.of("[PROJECT]", "[INSTANCE_CONFIG]").toString())
   *           .setEtag("etag3123477")
   *           .setValidateOnly(true)
   *           .build();
   *   ApiFuture<Empty> future =
   *       instanceAdminClient.deleteInstanceConfigCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DeleteInstanceConfigRequest, Empty> deleteInstanceConfigCallable() {
    return stub.deleteInstanceConfigCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the user-managed instance config [long-running operations][google.longrunning.Operation]
   * in the given project. An instance config operation has a name of the form
   * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.start_time` in descending order starting from
   * the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ProjectName parent = ProjectName.of("[PROJECT]");
   *   for (Operation element :
   *       instanceAdminClient.listInstanceConfigOperations(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The project of the instance config operations. Values are of the form
   *     `projects/&lt;project&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstanceConfigOperationsPagedResponse listInstanceConfigOperations(
      ProjectName parent) {
    ListInstanceConfigOperationsRequest request =
        ListInstanceConfigOperationsRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listInstanceConfigOperations(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the user-managed instance config [long-running operations][google.longrunning.Operation]
   * in the given project. An instance config operation has a name of the form
   * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.start_time` in descending order starting from
   * the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String parent = ProjectName.of("[PROJECT]").toString();
   *   for (Operation element :
   *       instanceAdminClient.listInstanceConfigOperations(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The project of the instance config operations. Values are of the form
   *     `projects/&lt;project&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstanceConfigOperationsPagedResponse listInstanceConfigOperations(
      String parent) {
    ListInstanceConfigOperationsRequest request =
        ListInstanceConfigOperationsRequest.newBuilder().setParent(parent).build();
    return listInstanceConfigOperations(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the user-managed instance config [long-running operations][google.longrunning.Operation]
   * in the given project. An instance config operation has a name of the form
   * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.start_time` in descending order starting from
   * the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstanceConfigOperationsRequest request =
   *       ListInstanceConfigOperationsRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   for (Operation element :
   *       instanceAdminClient.listInstanceConfigOperations(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstanceConfigOperationsPagedResponse listInstanceConfigOperations(
      ListInstanceConfigOperationsRequest request) {
    return listInstanceConfigOperationsPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the user-managed instance config [long-running operations][google.longrunning.Operation]
   * in the given project. An instance config operation has a name of the form
   * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.start_time` in descending order starting from
   * the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstanceConfigOperationsRequest request =
   *       ListInstanceConfigOperationsRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   ApiFuture<Operation> future =
   *       instanceAdminClient.listInstanceConfigOperationsPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Operation element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<
          ListInstanceConfigOperationsRequest, ListInstanceConfigOperationsPagedResponse>
      listInstanceConfigOperationsPagedCallable() {
    return stub.listInstanceConfigOperationsPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists the user-managed instance config [long-running operations][google.longrunning.Operation]
   * in the given project. An instance config operation has a name of the form
   * `projects/&lt;project&gt;/instanceConfigs/&lt;instance_config&gt;/operations/&lt;operation&gt;`.
   * The long-running operation [metadata][google.longrunning.Operation.metadata] field type
   * `metadata.type_url` describes the type of the metadata. Operations returned include those that
   * have completed/failed/canceled within the last 7 days, and pending operations. Operations
   * returned are ordered by `operation.metadata.value.start_time` in descending order starting from
   * the most recently started operation.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstanceConfigOperationsRequest request =
   *       ListInstanceConfigOperationsRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setFilter("filter-1274492040")
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .build();
   *   while (true) {
   *     ListInstanceConfigOperationsResponse response =
   *         instanceAdminClient.listInstanceConfigOperationsCallable().call(request);
   *     for (Operation element : response.getOperationsList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<
          ListInstanceConfigOperationsRequest, ListInstanceConfigOperationsResponse>
      listInstanceConfigOperationsCallable() {
    return stub.listInstanceConfigOperationsCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in the given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ProjectName parent = ProjectName.of("[PROJECT]");
   *   for (Instance element : instanceAdminClient.listInstances(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project for which a list of instances is requested.
   *     Values are of the form `projects/&lt;project&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstancesPagedResponse listInstances(ProjectName parent) {
    ListInstancesRequest request =
        ListInstancesRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listInstances(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in the given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String parent = ProjectName.of("[PROJECT]").toString();
   *   for (Instance element : instanceAdminClient.listInstances(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project for which a list of instances is requested.
   *     Values are of the form `projects/&lt;project&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstancesPagedResponse listInstances(String parent) {
    ListInstancesRequest request = ListInstancesRequest.newBuilder().setParent(parent).build();
    return listInstances(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in the given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstancesRequest request =
   *       ListInstancesRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   for (Instance element : instanceAdminClient.listInstances(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstancesPagedResponse listInstances(ListInstancesRequest request) {
    return listInstancesPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in the given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstancesRequest request =
   *       ListInstancesRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   ApiFuture<Instance> future =
   *       instanceAdminClient.listInstancesPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Instance element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListInstancesRequest, ListInstancesPagedResponse>
      listInstancesPagedCallable() {
    return stub.listInstancesPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in the given project.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ListInstancesRequest request =
   *       ListInstancesRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   while (true) {
   *     ListInstancesResponse response = instanceAdminClient.listInstancesCallable().call(request);
   *     for (Instance element : response.getInstancesList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListInstancesRequest, ListInstancesResponse> listInstancesCallable() {
    return stub.listInstancesCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   InstanceName name = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   Instance response = instanceAdminClient.getInstance(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the requested instance. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Instance getInstance(InstanceName name) {
    GetInstanceRequest request =
        GetInstanceRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return getInstance(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String name = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   Instance response = instanceAdminClient.getInstance(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the requested instance. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Instance getInstance(String name) {
    GetInstanceRequest request = GetInstanceRequest.newBuilder().setName(name).build();
    return getInstance(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   GetInstanceRequest request =
   *       GetInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFieldMask(FieldMask.newBuilder().build())
   *           .build();
   *   Instance response = instanceAdminClient.getInstance(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Instance getInstance(GetInstanceRequest request) {
    return getInstanceCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets information about a particular instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   GetInstanceRequest request =
   *       GetInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setFieldMask(FieldMask.newBuilder().build())
   *           .build();
   *   ApiFuture<Instance> future = instanceAdminClient.getInstanceCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetInstanceRequest, Instance> getInstanceCallable() {
    return stub.getInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance and begins preparing it to begin serving. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance. The instance name is assigned by the caller. If the named instance already exists,
   * `CreateInstance` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; The instance is readable via the API, with all requested attributes but no allocated
   * resources. Its state is `CREATING`.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation renders the instance immediately unreadable via the API.
   * &#42; The instance can be deleted. &#42; All other attempts to modify the instance are
   * rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing for all successfully-allocated resources begins (some types may have lower
   * than the requested levels). &#42; Databases can be created in the instance. &#42; The
   * instance's allocated resource levels are readable via the API. &#42; The instance's state
   * becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ProjectName parent = ProjectName.of("[PROJECT]");
   *   String instanceId = "instanceId902024336";
   *   Instance instance = Instance.newBuilder().build();
   *   Instance response =
   *       instanceAdminClient.createInstanceAsync(parent, instanceId, instance).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project in which to create the instance. Values are of
   *     the form `projects/&lt;project&gt;`.
   * @param instanceId Required. The ID of the instance to create. Valid identifiers are of the form
   *     `[a-z][-a-z0-9]&#42;[a-z0-9]` and must be between 2 and 64 characters in length.
   * @param instance Required. The instance to create. The name may be omitted, but if specified
   *     must be `&lt;parent&gt;/instances/&lt;instance_id&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, CreateInstanceMetadata> createInstanceAsync(
      ProjectName parent, String instanceId, Instance instance) {
    CreateInstanceRequest request =
        CreateInstanceRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setInstanceId(instanceId)
            .setInstance(instance)
            .build();
    return createInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance and begins preparing it to begin serving. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance. The instance name is assigned by the caller. If the named instance already exists,
   * `CreateInstance` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; The instance is readable via the API, with all requested attributes but no allocated
   * resources. Its state is `CREATING`.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation renders the instance immediately unreadable via the API.
   * &#42; The instance can be deleted. &#42; All other attempts to modify the instance are
   * rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing for all successfully-allocated resources begins (some types may have lower
   * than the requested levels). &#42; Databases can be created in the instance. &#42; The
   * instance's allocated resource levels are readable via the API. &#42; The instance's state
   * becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String parent = ProjectName.of("[PROJECT]").toString();
   *   String instanceId = "instanceId902024336";
   *   Instance instance = Instance.newBuilder().build();
   *   Instance response =
   *       instanceAdminClient.createInstanceAsync(parent, instanceId, instance).get();
   * }
   * }</pre>
   *
   * @param parent Required. The name of the project in which to create the instance. Values are of
   *     the form `projects/&lt;project&gt;`.
   * @param instanceId Required. The ID of the instance to create. Valid identifiers are of the form
   *     `[a-z][-a-z0-9]&#42;[a-z0-9]` and must be between 2 and 64 characters in length.
   * @param instance Required. The instance to create. The name may be omitted, but if specified
   *     must be `&lt;parent&gt;/instances/&lt;instance_id&gt;`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, CreateInstanceMetadata> createInstanceAsync(
      String parent, String instanceId, Instance instance) {
    CreateInstanceRequest request =
        CreateInstanceRequest.newBuilder()
            .setParent(parent)
            .setInstanceId(instanceId)
            .setInstance(instance)
            .build();
    return createInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance and begins preparing it to begin serving. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance. The instance name is assigned by the caller. If the named instance already exists,
   * `CreateInstance` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; The instance is readable via the API, with all requested attributes but no allocated
   * resources. Its state is `CREATING`.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation renders the instance immediately unreadable via the API.
   * &#42; The instance can be deleted. &#42; All other attempts to modify the instance are
   * rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing for all successfully-allocated resources begins (some types may have lower
   * than the requested levels). &#42; Databases can be created in the instance. &#42; The
   * instance's allocated resource levels are readable via the API. &#42; The instance's state
   * becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   CreateInstanceRequest request =
   *       CreateInstanceRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setInstanceId("instanceId902024336")
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   Instance response = instanceAdminClient.createInstanceAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, CreateInstanceMetadata> createInstanceAsync(
      CreateInstanceRequest request) {
    return createInstanceOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance and begins preparing it to begin serving. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance. The instance name is assigned by the caller. If the named instance already exists,
   * `CreateInstance` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; The instance is readable via the API, with all requested attributes but no allocated
   * resources. Its state is `CREATING`.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation renders the instance immediately unreadable via the API.
   * &#42; The instance can be deleted. &#42; All other attempts to modify the instance are
   * rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing for all successfully-allocated resources begins (some types may have lower
   * than the requested levels). &#42; Databases can be created in the instance. &#42; The
   * instance's allocated resource levels are readable via the API. &#42; The instance's state
   * becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   CreateInstanceRequest request =
   *       CreateInstanceRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setInstanceId("instanceId902024336")
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   OperationFuture<Instance, CreateInstanceMetadata> future =
   *       instanceAdminClient.createInstanceOperationCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<CreateInstanceRequest, Instance, CreateInstanceMetadata>
      createInstanceOperationCallable() {
    return stub.createInstanceOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance and begins preparing it to begin serving. The returned [long-running
   * operation][google.longrunning.Operation] can be used to track the progress of preparing the new
   * instance. The instance name is assigned by the caller. If the named instance already exists,
   * `CreateInstance` returns `ALREADY_EXISTS`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; The instance is readable via the API, with all requested attributes but no allocated
   * resources. Its state is `CREATING`.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation renders the instance immediately unreadable via the API.
   * &#42; The instance can be deleted. &#42; All other attempts to modify the instance are
   * rejected.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing for all successfully-allocated resources begins (some types may have lower
   * than the requested levels). &#42; Databases can be created in the instance. &#42; The
   * instance's allocated resource levels are readable via the API. &#42; The instance's state
   * becomes `READY`.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track
   * creation of the instance. The [metadata][google.longrunning.Operation.metadata] field type is
   * [CreateInstanceMetadata][google.spanner.admin.instance.v1.CreateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   CreateInstanceRequest request =
   *       CreateInstanceRequest.newBuilder()
   *           .setParent(ProjectName.of("[PROJECT]").toString())
   *           .setInstanceId("instanceId902024336")
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       instanceAdminClient.createInstanceCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateInstanceRequest, Operation> createInstanceCallable() {
    return stub.createInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance, and begins allocating or releasing resources as requested. The returned
   * [long-running operation][google.longrunning.Operation] can be used to track the progress of
   * updating the instance. If the named instance does not exist, returns `NOT_FOUND`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; For resource types for which a decrease in the instance's allocation has been
   * requested, billing is based on the newly-requested level.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time], and begins
   * restoring resources to their pre-request values. The operation is guaranteed to succeed at
   * undoing all resource changes, after which point it terminates with a `CANCELLED` status. &#42;
   * All other attempts to modify the instance are rejected. &#42; Reading the instance via the API
   * continues to give the pre-request resource levels.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing begins for all successfully-allocated resources (some types may have lower
   * than the requested levels). &#42; All newly-reserved resources are available for serving the
   * instance's tables. &#42; The instance's new resource levels are readable via the API.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track the
   * instance modification. The [metadata][google.longrunning.Operation.metadata] field type is
   * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Authorization requires `spanner.instances.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.Instance.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   Instance instance = Instance.newBuilder().build();
   *   FieldMask fieldMask = FieldMask.newBuilder().build();
   *   Instance response = instanceAdminClient.updateInstanceAsync(instance, fieldMask).get();
   * }
   * }</pre>
   *
   * @param instance Required. The instance to update, which must always include the instance name.
   *     Otherwise, only fields mentioned in
   *     [field_mask][google.spanner.admin.instance.v1.UpdateInstanceRequest.field_mask] need be
   *     included.
   * @param fieldMask Required. A mask specifying which fields in
   *     [Instance][google.spanner.admin.instance.v1.Instance] should be updated. The field mask
   *     must always be specified; this prevents any future fields in
   *     [Instance][google.spanner.admin.instance.v1.Instance] from being erased accidentally by
   *     clients that do not know about them.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, UpdateInstanceMetadata> updateInstanceAsync(
      Instance instance, FieldMask fieldMask) {
    UpdateInstanceRequest request =
        UpdateInstanceRequest.newBuilder().setInstance(instance).setFieldMask(fieldMask).build();
    return updateInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance, and begins allocating or releasing resources as requested. The returned
   * [long-running operation][google.longrunning.Operation] can be used to track the progress of
   * updating the instance. If the named instance does not exist, returns `NOT_FOUND`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; For resource types for which a decrease in the instance's allocation has been
   * requested, billing is based on the newly-requested level.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time], and begins
   * restoring resources to their pre-request values. The operation is guaranteed to succeed at
   * undoing all resource changes, after which point it terminates with a `CANCELLED` status. &#42;
   * All other attempts to modify the instance are rejected. &#42; Reading the instance via the API
   * continues to give the pre-request resource levels.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing begins for all successfully-allocated resources (some types may have lower
   * than the requested levels). &#42; All newly-reserved resources are available for serving the
   * instance's tables. &#42; The instance's new resource levels are readable via the API.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track the
   * instance modification. The [metadata][google.longrunning.Operation.metadata] field type is
   * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Authorization requires `spanner.instances.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.Instance.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   UpdateInstanceRequest request =
   *       UpdateInstanceRequest.newBuilder()
   *           .setInstance(Instance.newBuilder().build())
   *           .setFieldMask(FieldMask.newBuilder().build())
   *           .build();
   *   Instance response = instanceAdminClient.updateInstanceAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, UpdateInstanceMetadata> updateInstanceAsync(
      UpdateInstanceRequest request) {
    return updateInstanceOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance, and begins allocating or releasing resources as requested. The returned
   * [long-running operation][google.longrunning.Operation] can be used to track the progress of
   * updating the instance. If the named instance does not exist, returns `NOT_FOUND`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; For resource types for which a decrease in the instance's allocation has been
   * requested, billing is based on the newly-requested level.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time], and begins
   * restoring resources to their pre-request values. The operation is guaranteed to succeed at
   * undoing all resource changes, after which point it terminates with a `CANCELLED` status. &#42;
   * All other attempts to modify the instance are rejected. &#42; Reading the instance via the API
   * continues to give the pre-request resource levels.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing begins for all successfully-allocated resources (some types may have lower
   * than the requested levels). &#42; All newly-reserved resources are available for serving the
   * instance's tables. &#42; The instance's new resource levels are readable via the API.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track the
   * instance modification. The [metadata][google.longrunning.Operation.metadata] field type is
   * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Authorization requires `spanner.instances.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.Instance.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   UpdateInstanceRequest request =
   *       UpdateInstanceRequest.newBuilder()
   *           .setInstance(Instance.newBuilder().build())
   *           .setFieldMask(FieldMask.newBuilder().build())
   *           .build();
   *   OperationFuture<Instance, UpdateInstanceMetadata> future =
   *       instanceAdminClient.updateInstanceOperationCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<UpdateInstanceRequest, Instance, UpdateInstanceMetadata>
      updateInstanceOperationCallable() {
    return stub.updateInstanceOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates an instance, and begins allocating or releasing resources as requested. The returned
   * [long-running operation][google.longrunning.Operation] can be used to track the progress of
   * updating the instance. If the named instance does not exist, returns `NOT_FOUND`.
   *
   * <p>Immediately upon completion of this request:
   *
   * <p>&#42; For resource types for which a decrease in the instance's allocation has been
   * requested, billing is based on the newly-requested level.
   *
   * <p>Until completion of the returned operation:
   *
   * <p>&#42; Cancelling the operation sets its metadata's
   * [cancel_time][google.spanner.admin.instance.v1.UpdateInstanceMetadata.cancel_time], and begins
   * restoring resources to their pre-request values. The operation is guaranteed to succeed at
   * undoing all resource changes, after which point it terminates with a `CANCELLED` status. &#42;
   * All other attempts to modify the instance are rejected. &#42; Reading the instance via the API
   * continues to give the pre-request resource levels.
   *
   * <p>Upon completion of the returned operation:
   *
   * <p>&#42; Billing begins for all successfully-allocated resources (some types may have lower
   * than the requested levels). &#42; All newly-reserved resources are available for serving the
   * instance's tables. &#42; The instance's new resource levels are readable via the API.
   *
   * <p>The returned [long-running operation][google.longrunning.Operation] will have a name of the
   * format `&lt;instance_name&gt;/operations/&lt;operation_id&gt;` and can be used to track the
   * instance modification. The [metadata][google.longrunning.Operation.metadata] field type is
   * [UpdateInstanceMetadata][google.spanner.admin.instance.v1.UpdateInstanceMetadata]. The
   * [response][google.longrunning.Operation.response] field type is
   * [Instance][google.spanner.admin.instance.v1.Instance], if successful.
   *
   * <p>Authorization requires `spanner.instances.update` permission on the resource
   * [name][google.spanner.admin.instance.v1.Instance.name].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   UpdateInstanceRequest request =
   *       UpdateInstanceRequest.newBuilder()
   *           .setInstance(Instance.newBuilder().build())
   *           .setFieldMask(FieldMask.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       instanceAdminClient.updateInstanceCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateInstanceRequest, Operation> updateInstanceCallable() {
    return stub.updateInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Immediately upon completion of the request:
   *
   * <p>&#42; Billing ceases for all of the instance's reserved resources.
   *
   * <p>Soon afterward:
   *
   * <p>&#42; The instance and &#42;all of its databases&#42; immediately and irrevocably disappear
   * from the API. All data in the databases is permanently deleted.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   InstanceName name = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   instanceAdminClient.deleteInstance(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the instance to be deleted. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteInstance(InstanceName name) {
    DeleteInstanceRequest request =
        DeleteInstanceRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    deleteInstance(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Immediately upon completion of the request:
   *
   * <p>&#42; Billing ceases for all of the instance's reserved resources.
   *
   * <p>Soon afterward:
   *
   * <p>&#42; The instance and &#42;all of its databases&#42; immediately and irrevocably disappear
   * from the API. All data in the databases is permanently deleted.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String name = InstanceName.of("[PROJECT]", "[INSTANCE]").toString();
   *   instanceAdminClient.deleteInstance(name);
   * }
   * }</pre>
   *
   * @param name Required. The name of the instance to be deleted. Values are of the form
   *     `projects/&lt;project&gt;/instances/&lt;instance&gt;`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteInstance(String name) {
    DeleteInstanceRequest request = DeleteInstanceRequest.newBuilder().setName(name).build();
    deleteInstance(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Immediately upon completion of the request:
   *
   * <p>&#42; Billing ceases for all of the instance's reserved resources.
   *
   * <p>Soon afterward:
   *
   * <p>&#42; The instance and &#42;all of its databases&#42; immediately and irrevocably disappear
   * from the API. All data in the databases is permanently deleted.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   DeleteInstanceRequest request =
   *       DeleteInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .build();
   *   instanceAdminClient.deleteInstance(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final void deleteInstance(DeleteInstanceRequest request) {
    deleteInstanceCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Immediately upon completion of the request:
   *
   * <p>&#42; Billing ceases for all of the instance's reserved resources.
   *
   * <p>Soon afterward:
   *
   * <p>&#42; The instance and &#42;all of its databases&#42; immediately and irrevocably disappear
   * from the API. All data in the databases is permanently deleted.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   DeleteInstanceRequest request =
   *       DeleteInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .build();
   *   ApiFuture<Empty> future = instanceAdminClient.deleteInstanceCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DeleteInstanceRequest, Empty> deleteInstanceCallable() {
    return stub.deleteInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on an instance resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.instances.setIamPolicy` on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ResourceName resource = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   Policy policy = Policy.newBuilder().build();
   *   Policy response = instanceAdminClient.setIamPolicy(resource, policy);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being specified. See the
   *     operation documentation for the appropriate value for this field.
   * @param policy REQUIRED: The complete policy to be applied to the `resource`. The size of the
   *     policy is limited to a few 10s of KB. An empty policy is a valid policy but certain Cloud
   *     Platform services (such as Projects) might reject them.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy setIamPolicy(ResourceName resource, Policy policy) {
    SetIamPolicyRequest request =
        SetIamPolicyRequest.newBuilder()
            .setResource(resource == null ? null : resource.toString())
            .setPolicy(policy)
            .build();
    return setIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on an instance resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.instances.setIamPolicy` on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String resource = ProjectName.of("[PROJECT]").toString();
   *   Policy policy = Policy.newBuilder().build();
   *   Policy response = instanceAdminClient.setIamPolicy(resource, policy);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being specified. See the
   *     operation documentation for the appropriate value for this field.
   * @param policy REQUIRED: The complete policy to be applied to the `resource`. The size of the
   *     policy is limited to a few 10s of KB. An empty policy is a valid policy but certain Cloud
   *     Platform services (such as Projects) might reject them.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy setIamPolicy(String resource, Policy policy) {
    SetIamPolicyRequest request =
        SetIamPolicyRequest.newBuilder().setResource(resource).setPolicy(policy).build();
    return setIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on an instance resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.instances.setIamPolicy` on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   SetIamPolicyRequest request =
   *       SetIamPolicyRequest.newBuilder()
   *           .setResource(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setPolicy(Policy.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   Policy response = instanceAdminClient.setIamPolicy(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy setIamPolicy(SetIamPolicyRequest request) {
    return setIamPolicyCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Sets the access control policy on an instance resource. Replaces any existing policy.
   *
   * <p>Authorization requires `spanner.instances.setIamPolicy` on
   * [resource][google.iam.v1.SetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   SetIamPolicyRequest request =
   *       SetIamPolicyRequest.newBuilder()
   *           .setResource(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setPolicy(Policy.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   ApiFuture<Policy> future = instanceAdminClient.setIamPolicyCallable().futureCall(request);
   *   // Do something.
   *   Policy response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<SetIamPolicyRequest, Policy> setIamPolicyCallable() {
    return stub.setIamPolicyCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for an instance resource. Returns an empty policy if an instance
   * exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.instances.getIamPolicy` on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ResourceName resource = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   Policy response = instanceAdminClient.getIamPolicy(resource);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy getIamPolicy(ResourceName resource) {
    GetIamPolicyRequest request =
        GetIamPolicyRequest.newBuilder()
            .setResource(resource == null ? null : resource.toString())
            .build();
    return getIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for an instance resource. Returns an empty policy if an instance
   * exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.instances.getIamPolicy` on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String resource = ProjectName.of("[PROJECT]").toString();
   *   Policy response = instanceAdminClient.getIamPolicy(resource);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy getIamPolicy(String resource) {
    GetIamPolicyRequest request = GetIamPolicyRequest.newBuilder().setResource(resource).build();
    return getIamPolicy(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for an instance resource. Returns an empty policy if an instance
   * exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.instances.getIamPolicy` on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   GetIamPolicyRequest request =
   *       GetIamPolicyRequest.newBuilder()
   *           .setResource(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setOptions(GetPolicyOptions.newBuilder().build())
   *           .build();
   *   Policy response = instanceAdminClient.getIamPolicy(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Policy getIamPolicy(GetIamPolicyRequest request) {
    return getIamPolicyCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the access control policy for an instance resource. Returns an empty policy if an instance
   * exists but does not have a policy set.
   *
   * <p>Authorization requires `spanner.instances.getIamPolicy` on
   * [resource][google.iam.v1.GetIamPolicyRequest.resource].
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   GetIamPolicyRequest request =
   *       GetIamPolicyRequest.newBuilder()
   *           .setResource(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .setOptions(GetPolicyOptions.newBuilder().build())
   *           .build();
   *   ApiFuture<Policy> future = instanceAdminClient.getIamPolicyCallable().futureCall(request);
   *   // Do something.
   *   Policy response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetIamPolicyRequest, Policy> getIamPolicyCallable() {
    return stub.getIamPolicyCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified instance resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner instance resource will result in a
   * NOT_FOUND error if the user has `spanner.instances.list` permission on the containing Google
   * Cloud Project. Otherwise returns an empty set of permissions.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   ResourceName resource = InstanceName.of("[PROJECT]", "[INSTANCE]");
   *   List<String> permissions = new ArrayList<>();
   *   TestIamPermissionsResponse response =
   *       instanceAdminClient.testIamPermissions(resource, permissions);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy detail is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @param permissions The set of permissions to check for the `resource`. Permissions with
   *     wildcards (such as '&#42;' or 'storage.&#42;') are not allowed. For more information see
   *     [IAM Overview](https://cloud.google.com/iam/docs/overview#permissions).
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final TestIamPermissionsResponse testIamPermissions(
      ResourceName resource, List<String> permissions) {
    TestIamPermissionsRequest request =
        TestIamPermissionsRequest.newBuilder()
            .setResource(resource == null ? null : resource.toString())
            .addAllPermissions(permissions)
            .build();
    return testIamPermissions(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified instance resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner instance resource will result in a
   * NOT_FOUND error if the user has `spanner.instances.list` permission on the containing Google
   * Cloud Project. Otherwise returns an empty set of permissions.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   String resource = ProjectName.of("[PROJECT]").toString();
   *   List<String> permissions = new ArrayList<>();
   *   TestIamPermissionsResponse response =
   *       instanceAdminClient.testIamPermissions(resource, permissions);
   * }
   * }</pre>
   *
   * @param resource REQUIRED: The resource for which the policy detail is being requested. See the
   *     operation documentation for the appropriate value for this field.
   * @param permissions The set of permissions to check for the `resource`. Permissions with
   *     wildcards (such as '&#42;' or 'storage.&#42;') are not allowed. For more information see
   *     [IAM Overview](https://cloud.google.com/iam/docs/overview#permissions).
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final TestIamPermissionsResponse testIamPermissions(
      String resource, List<String> permissions) {
    TestIamPermissionsRequest request =
        TestIamPermissionsRequest.newBuilder()
            .setResource(resource)
            .addAllPermissions(permissions)
            .build();
    return testIamPermissions(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified instance resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner instance resource will result in a
   * NOT_FOUND error if the user has `spanner.instances.list` permission on the containing Google
   * Cloud Project. Otherwise returns an empty set of permissions.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   TestIamPermissionsRequest request =
   *       TestIamPermissionsRequest.newBuilder()
   *           .setResource(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .addAllPermissions(new ArrayList<String>())
   *           .build();
   *   TestIamPermissionsResponse response = instanceAdminClient.testIamPermissions(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final TestIamPermissionsResponse testIamPermissions(TestIamPermissionsRequest request) {
    return testIamPermissionsCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Returns permissions that the caller has on the specified instance resource.
   *
   * <p>Attempting this RPC on a non-existent Cloud Spanner instance resource will result in a
   * NOT_FOUND error if the user has `spanner.instances.list` permission on the containing Google
   * Cloud Project. Otherwise returns an empty set of permissions.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (InstanceAdminClient instanceAdminClient = InstanceAdminClient.create()) {
   *   TestIamPermissionsRequest request =
   *       TestIamPermissionsRequest.newBuilder()
   *           .setResource(InstanceName.of("[PROJECT]", "[INSTANCE]").toString())
   *           .addAllPermissions(new ArrayList<String>())
   *           .build();
   *   ApiFuture<TestIamPermissionsResponse> future =
   *       instanceAdminClient.testIamPermissionsCallable().futureCall(request);
   *   // Do something.
   *   TestIamPermissionsResponse response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<TestIamPermissionsRequest, TestIamPermissionsResponse>
      testIamPermissionsCallable() {
    return stub.testIamPermissionsCallable();
  }

  @Override
  public final void close() {
    stub.close();
  }

  @Override
  public void shutdown() {
    stub.shutdown();
  }

  @Override
  public boolean isShutdown() {
    return stub.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return stub.isTerminated();
  }

  @Override
  public void shutdownNow() {
    stub.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long duration, TimeUnit unit) throws InterruptedException {
    return stub.awaitTermination(duration, unit);
  }

  public static class ListInstanceConfigsPagedResponse
      extends AbstractPagedListResponse<
          ListInstanceConfigsRequest,
          ListInstanceConfigsResponse,
          InstanceConfig,
          ListInstanceConfigsPage,
          ListInstanceConfigsFixedSizeCollection> {

    public static ApiFuture<ListInstanceConfigsPagedResponse> createAsync(
        PageContext<ListInstanceConfigsRequest, ListInstanceConfigsResponse, InstanceConfig>
            context,
        ApiFuture<ListInstanceConfigsResponse> futureResponse) {
      ApiFuture<ListInstanceConfigsPage> futurePage =
          ListInstanceConfigsPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          input -> new ListInstanceConfigsPagedResponse(input),
          MoreExecutors.directExecutor());
    }

    private ListInstanceConfigsPagedResponse(ListInstanceConfigsPage page) {
      super(page, ListInstanceConfigsFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListInstanceConfigsPage
      extends AbstractPage<
          ListInstanceConfigsRequest,
          ListInstanceConfigsResponse,
          InstanceConfig,
          ListInstanceConfigsPage> {

    private ListInstanceConfigsPage(
        PageContext<ListInstanceConfigsRequest, ListInstanceConfigsResponse, InstanceConfig>
            context,
        ListInstanceConfigsResponse response) {
      super(context, response);
    }

    private static ListInstanceConfigsPage createEmptyPage() {
      return new ListInstanceConfigsPage(null, null);
    }

    @Override
    protected ListInstanceConfigsPage createPage(
        PageContext<ListInstanceConfigsRequest, ListInstanceConfigsResponse, InstanceConfig>
            context,
        ListInstanceConfigsResponse response) {
      return new ListInstanceConfigsPage(context, response);
    }

    @Override
    public ApiFuture<ListInstanceConfigsPage> createPageAsync(
        PageContext<ListInstanceConfigsRequest, ListInstanceConfigsResponse, InstanceConfig>
            context,
        ApiFuture<ListInstanceConfigsResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListInstanceConfigsFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListInstanceConfigsRequest,
          ListInstanceConfigsResponse,
          InstanceConfig,
          ListInstanceConfigsPage,
          ListInstanceConfigsFixedSizeCollection> {

    private ListInstanceConfigsFixedSizeCollection(
        List<ListInstanceConfigsPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListInstanceConfigsFixedSizeCollection createEmptyCollection() {
      return new ListInstanceConfigsFixedSizeCollection(null, 0);
    }

    @Override
    protected ListInstanceConfigsFixedSizeCollection createCollection(
        List<ListInstanceConfigsPage> pages, int collectionSize) {
      return new ListInstanceConfigsFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListInstanceConfigOperationsPagedResponse
      extends AbstractPagedListResponse<
          ListInstanceConfigOperationsRequest,
          ListInstanceConfigOperationsResponse,
          Operation,
          ListInstanceConfigOperationsPage,
          ListInstanceConfigOperationsFixedSizeCollection> {

    public static ApiFuture<ListInstanceConfigOperationsPagedResponse> createAsync(
        PageContext<
                ListInstanceConfigOperationsRequest,
                ListInstanceConfigOperationsResponse,
                Operation>
            context,
        ApiFuture<ListInstanceConfigOperationsResponse> futureResponse) {
      ApiFuture<ListInstanceConfigOperationsPage> futurePage =
          ListInstanceConfigOperationsPage.createEmptyPage()
              .createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          input -> new ListInstanceConfigOperationsPagedResponse(input),
          MoreExecutors.directExecutor());
    }

    private ListInstanceConfigOperationsPagedResponse(ListInstanceConfigOperationsPage page) {
      super(page, ListInstanceConfigOperationsFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListInstanceConfigOperationsPage
      extends AbstractPage<
          ListInstanceConfigOperationsRequest,
          ListInstanceConfigOperationsResponse,
          Operation,
          ListInstanceConfigOperationsPage> {

    private ListInstanceConfigOperationsPage(
        PageContext<
                ListInstanceConfigOperationsRequest,
                ListInstanceConfigOperationsResponse,
                Operation>
            context,
        ListInstanceConfigOperationsResponse response) {
      super(context, response);
    }

    private static ListInstanceConfigOperationsPage createEmptyPage() {
      return new ListInstanceConfigOperationsPage(null, null);
    }

    @Override
    protected ListInstanceConfigOperationsPage createPage(
        PageContext<
                ListInstanceConfigOperationsRequest,
                ListInstanceConfigOperationsResponse,
                Operation>
            context,
        ListInstanceConfigOperationsResponse response) {
      return new ListInstanceConfigOperationsPage(context, response);
    }

    @Override
    public ApiFuture<ListInstanceConfigOperationsPage> createPageAsync(
        PageContext<
                ListInstanceConfigOperationsRequest,
                ListInstanceConfigOperationsResponse,
                Operation>
            context,
        ApiFuture<ListInstanceConfigOperationsResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListInstanceConfigOperationsFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListInstanceConfigOperationsRequest,
          ListInstanceConfigOperationsResponse,
          Operation,
          ListInstanceConfigOperationsPage,
          ListInstanceConfigOperationsFixedSizeCollection> {

    private ListInstanceConfigOperationsFixedSizeCollection(
        List<ListInstanceConfigOperationsPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListInstanceConfigOperationsFixedSizeCollection createEmptyCollection() {
      return new ListInstanceConfigOperationsFixedSizeCollection(null, 0);
    }

    @Override
    protected ListInstanceConfigOperationsFixedSizeCollection createCollection(
        List<ListInstanceConfigOperationsPage> pages, int collectionSize) {
      return new ListInstanceConfigOperationsFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListInstancesPagedResponse
      extends AbstractPagedListResponse<
          ListInstancesRequest,
          ListInstancesResponse,
          Instance,
          ListInstancesPage,
          ListInstancesFixedSizeCollection> {

    public static ApiFuture<ListInstancesPagedResponse> createAsync(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ApiFuture<ListInstancesResponse> futureResponse) {
      ApiFuture<ListInstancesPage> futurePage =
          ListInstancesPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          input -> new ListInstancesPagedResponse(input),
          MoreExecutors.directExecutor());
    }

    private ListInstancesPagedResponse(ListInstancesPage page) {
      super(page, ListInstancesFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListInstancesPage
      extends AbstractPage<
          ListInstancesRequest, ListInstancesResponse, Instance, ListInstancesPage> {

    private ListInstancesPage(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ListInstancesResponse response) {
      super(context, response);
    }

    private static ListInstancesPage createEmptyPage() {
      return new ListInstancesPage(null, null);
    }

    @Override
    protected ListInstancesPage createPage(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ListInstancesResponse response) {
      return new ListInstancesPage(context, response);
    }

    @Override
    public ApiFuture<ListInstancesPage> createPageAsync(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ApiFuture<ListInstancesResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListInstancesFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListInstancesRequest,
          ListInstancesResponse,
          Instance,
          ListInstancesPage,
          ListInstancesFixedSizeCollection> {

    private ListInstancesFixedSizeCollection(List<ListInstancesPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListInstancesFixedSizeCollection createEmptyCollection() {
      return new ListInstancesFixedSizeCollection(null, 0);
    }

    @Override
    protected ListInstancesFixedSizeCollection createCollection(
        List<ListInstancesPage> pages, int collectionSize) {
      return new ListInstancesFixedSizeCollection(pages, collectionSize);
    }
  }
}
