/*
 * Copyright 2024 Google LLC
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

/**
 * A client to Cloud Spanner Instance Admin API
 *
 * <p>The interfaces provided are listed below, along with usage samples.
 *
 * <p>======================= InstanceAdminClient =======================
 *
 * <p>Service Description: Cloud Spanner Instance Admin API
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
 * <p>Sample for InstanceAdminClient:
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
 */
@Generated("by gapic-generator-java")
package com.google.cloud.spanner.admin.instance.v1;

import javax.annotation.Generated;
