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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.Identity;
import com.google.cloud.Role;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.spi.v1.SpannerRpc.Paginated;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.FieldMask;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import com.google.spanner.admin.instance.v1.InstanceConfig;
import com.google.spanner.admin.instance.v1.UpdateInstanceMetadata;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link com.google.cloud.spanner.SpannerImpl.InstanceAdminClientImpl}. */
@RunWith(JUnit4.class)
public class InstanceAdminClientImplTest {
  private static final String PROJECT_ID = "my-project";
  private static final String INSTANCE_ID = "my-instance";
  private static final String INSTANCE_NAME = "projects/my-project/instances/my-instance";
  private static final String INSTANCE_NAME2 = "projects/my-project/instances/my-instance2";
  private static final String CONFIG_ID = "my-config";
  private static final String CONFIG_NAME = "projects/my-project/instanceConfigs/my-config";
  private static final String CONFIG_NAME2 = "projects/my-project/instanceConfigs/my-config2";

  @Mock SpannerRpc rpc;
  @Mock DatabaseAdminClient dbClient;
  InstanceAdminClientImpl client;

  @Before
  public void setUp() {
    initMocks(this);
    client = new InstanceAdminClientImpl(PROJECT_ID, rpc, dbClient);
  }

  @Test
  public void getInstanceConfig() {
    when(rpc.getInstanceConfig(CONFIG_NAME))
        .thenReturn(InstanceConfig.newBuilder().setName(CONFIG_NAME).build());
    assertThat(client.getInstanceConfig(CONFIG_ID).getId().getName()).isEqualTo(CONFIG_NAME);
  }

  @Test
  public void listInstanceConfigs() {
    String nextToken = "token";
    when(rpc.listInstanceConfigs(1, null))
        .thenReturn(
            new Paginated<>(
                ImmutableList.of(InstanceConfig.newBuilder().setName(CONFIG_NAME).build()),
                nextToken));
    when(rpc.listInstanceConfigs(1, nextToken))
        .thenReturn(
            new Paginated<>(
                ImmutableList.of(InstanceConfig.newBuilder().setName(CONFIG_NAME2).build()), ""));
    List<com.google.cloud.spanner.InstanceConfig> configs =
        Lists.newArrayList(client.listInstanceConfigs(Options.pageSize(1)).iterateAll());
    assertThat(configs.get(0).getId().getName()).isEqualTo(CONFIG_NAME);
    assertThat(configs.get(1).getId().getName()).isEqualTo(CONFIG_NAME2);
    assertThat(configs.size()).isEqualTo(2);
  }

  private com.google.spanner.admin.instance.v1.Instance getInstanceProto() {
    return com.google.spanner.admin.instance.v1.Instance.newBuilder()
        .setConfig(CONFIG_NAME)
        .setName(INSTANCE_NAME)
        .setNodeCount(1)
        .setProcessingUnits(1000)
        .build();
  }

  private com.google.spanner.admin.instance.v1.Instance getInstanceProtoWithProcessingUnits() {
    return com.google.spanner.admin.instance.v1.Instance.newBuilder()
        .setConfig(CONFIG_NAME)
        .setName(INSTANCE_NAME)
        .setProcessingUnits(10)
        .build();
  }

  private com.google.spanner.admin.instance.v1.Instance getAnotherInstanceProto() {
    return com.google.spanner.admin.instance.v1.Instance.newBuilder()
        .setConfig(CONFIG_NAME)
        .setName(INSTANCE_NAME2)
        .setNodeCount(2)
        .setProcessingUnits(2000)
        .build();
  }

  @Test
  public void createInstance() throws Exception {
    OperationFuture<com.google.spanner.admin.instance.v1.Instance, CreateInstanceMetadata>
        rawOperationFuture =
            OperationFutureUtil.immediateOperationFuture(
                "createInstance", getInstanceProto(), CreateInstanceMetadata.getDefaultInstance());
    when(rpc.createInstance(
            "projects/" + PROJECT_ID,
            INSTANCE_ID,
            getInstanceProto().toBuilder().setProcessingUnits(0).build()))
        .thenReturn(rawOperationFuture);
    OperationFuture<Instance, CreateInstanceMetadata> op =
        client.createInstance(
            InstanceInfo.newBuilder(InstanceId.of(PROJECT_ID, INSTANCE_ID))
                .setInstanceConfigId(InstanceConfigId.of(PROJECT_ID, CONFIG_ID))
                .setNodeCount(1)
                .build());
    assertThat(op.isDone()).isTrue();
    assertThat(op.get().getId().getName()).isEqualTo(INSTANCE_NAME);
  }

  @Test
  public void testCreateInstanceWithProcessingUnits() throws Exception {
    OperationFuture<com.google.spanner.admin.instance.v1.Instance, CreateInstanceMetadata>
        rawOperationFuture =
            OperationFutureUtil.immediateOperationFuture(
                "createInstance",
                getInstanceProtoWithProcessingUnits(),
                CreateInstanceMetadata.getDefaultInstance());
    when(rpc.createInstance(
            "projects/" + PROJECT_ID, INSTANCE_ID, getInstanceProtoWithProcessingUnits()))
        .thenReturn(rawOperationFuture);
    OperationFuture<Instance, CreateInstanceMetadata> operation =
        client.createInstance(
            InstanceInfo.newBuilder(InstanceId.of(PROJECT_ID, INSTANCE_ID))
                .setInstanceConfigId(InstanceConfigId.of(PROJECT_ID, CONFIG_ID))
                .setProcessingUnits(10)
                .build());
    assertTrue(operation.isDone());
    assertEquals(INSTANCE_NAME, operation.get().getId().getName());
  }

  @Test
  public void testCreateInstanceWithBothNodeCountAndProcessingUnits() throws Exception {
    try {
      client.createInstance(
          InstanceInfo.newBuilder(InstanceId.of(PROJECT_ID, INSTANCE_ID))
              .setInstanceConfigId(InstanceConfigId.of(PROJECT_ID, CONFIG_ID))
              .setNodeCount(1)
              .setProcessingUnits(100)
              .build());
      fail("missing expected exception");
    } catch (IllegalArgumentException e) {
      assertTrue(
          e.getMessage()
              .contains(
                  "Only one of nodeCount and processingUnits can be set when creating a new instance"));
    }
  }

  @Test
  public void testGetInstance() {
    when(rpc.getInstance(INSTANCE_NAME)).thenReturn(getInstanceProto());
    Instance instance = client.getInstance(INSTANCE_ID);
    assertEquals(INSTANCE_NAME, instance.getId().getName());
    assertEquals(1000, instance.getProcessingUnits());
  }

  @Test
  public void dropInstance() {
    client.deleteInstance(INSTANCE_ID);
    verify(rpc).deleteInstance(INSTANCE_NAME);
  }

  @Test
  public void updateInstanceMetadata() throws Exception {
    com.google.spanner.admin.instance.v1.Instance instance =
        com.google.spanner.admin.instance.v1.Instance.newBuilder()
            .setName(INSTANCE_NAME)
            .setConfig(CONFIG_NAME)
            .setNodeCount(2)
            .build();
    OperationFuture<com.google.spanner.admin.instance.v1.Instance, UpdateInstanceMetadata>
        rawOperationFuture =
            OperationFutureUtil.immediateOperationFuture(
                "updateInstance", getInstanceProto(), UpdateInstanceMetadata.getDefaultInstance());
    when(rpc.updateInstance(instance, FieldMask.newBuilder().addPaths("node_count").build()))
        .thenReturn(rawOperationFuture);
    InstanceInfo instanceInfo =
        InstanceInfo.newBuilder(InstanceId.of(INSTANCE_NAME))
            .setInstanceConfigId(InstanceConfigId.of(CONFIG_NAME))
            .setNodeCount(2)
            .build();
    OperationFuture<Instance, UpdateInstanceMetadata> op =
        client.updateInstance(instanceInfo, InstanceInfo.InstanceField.NODE_COUNT);
    assertThat(op.isDone()).isTrue();
    assertThat(op.get().getId().getName()).isEqualTo(INSTANCE_NAME);
  }

  @Test
  public void testUpdateInstanceProcessingUnits() throws Exception {
    com.google.spanner.admin.instance.v1.Instance instance =
        com.google.spanner.admin.instance.v1.Instance.newBuilder()
            .setName(INSTANCE_NAME)
            .setConfig(CONFIG_NAME)
            .setProcessingUnits(10)
            .build();
    OperationFuture<com.google.spanner.admin.instance.v1.Instance, UpdateInstanceMetadata>
        rawOperationFuture =
            OperationFutureUtil.immediateOperationFuture(
                "updateInstance",
                getInstanceProtoWithProcessingUnits(),
                UpdateInstanceMetadata.getDefaultInstance());
    when(rpc.updateInstance(instance, FieldMask.newBuilder().addPaths("processing_units").build()))
        .thenReturn(rawOperationFuture);
    InstanceInfo instanceInfo =
        InstanceInfo.newBuilder(InstanceId.of(INSTANCE_NAME))
            .setInstanceConfigId(InstanceConfigId.of(CONFIG_NAME))
            .setProcessingUnits(10)
            .build();
    OperationFuture<Instance, UpdateInstanceMetadata> operationWithFieldMask =
        client.updateInstance(instanceInfo, InstanceInfo.InstanceField.PROCESSING_UNITS);
    assertTrue(operationWithFieldMask.isDone());
    assertEquals(INSTANCE_NAME, operationWithFieldMask.get().getId().getName());

    when(rpc.updateInstance(
            instance,
            FieldMask.newBuilder()
                .addAllPaths(Arrays.asList("display_name", "processing_units", "labels"))
                .build()))
        .thenReturn(rawOperationFuture);
    OperationFuture<Instance, UpdateInstanceMetadata> operation =
        client.updateInstance(instanceInfo);
    assertTrue(operation.isDone());
    assertEquals(INSTANCE_NAME, operation.get().getId().getName());
  }

  @Test
  public void testUpdateInstanceWithNodeCountAndProcessingUnits() throws Exception {
    com.google.spanner.admin.instance.v1.Instance instance =
        com.google.spanner.admin.instance.v1.Instance.newBuilder()
            .setName(INSTANCE_NAME)
            .setConfig(CONFIG_NAME)
            .setNodeCount(3)
            .setProcessingUnits(3000)
            .build();
    OperationFuture<com.google.spanner.admin.instance.v1.Instance, UpdateInstanceMetadata>
        rawOperationFuture =
            OperationFutureUtil.immediateOperationFuture(
                "updateInstance",
                getInstanceProtoWithProcessingUnits(),
                UpdateInstanceMetadata.getDefaultInstance());
    // node_count should take precedence over processing_units when node_count>0 and no specific
    // field mask is set by the caller.
    when(rpc.updateInstance(
            instance,
            FieldMask.newBuilder()
                .addAllPaths(Arrays.asList("display_name", "node_count", "labels"))
                .build()))
        .thenReturn(rawOperationFuture);
    InstanceInfo instanceInfo =
        InstanceInfo.newBuilder(InstanceId.of(INSTANCE_NAME))
            .setInstanceConfigId(InstanceConfigId.of(CONFIG_NAME))
            .setNodeCount(3)
            .setProcessingUnits(3000)
            .build();
    OperationFuture<Instance, UpdateInstanceMetadata> operationWithFieldMask =
        client.updateInstance(instanceInfo);
    assertTrue(operationWithFieldMask.isDone());
    assertEquals(INSTANCE_NAME, operationWithFieldMask.get().getId().getName());
  }

  @Test
  public void testListInstances() {
    String nextToken = "token";
    String filter = "env:dev";
    when(rpc.listInstances(1, null, filter))
        .thenReturn(new Paginated<>(ImmutableList.of(getInstanceProto()), nextToken));
    when(rpc.listInstances(1, nextToken, filter))
        .thenReturn(new Paginated<>(ImmutableList.of(getAnotherInstanceProto()), ""));
    List<Instance> instances =
        Lists.newArrayList(
            client.listInstances(Options.pageSize(1), Options.filter(filter)).iterateAll());
    assertEquals(INSTANCE_NAME, instances.get(0).getId().getName());
    assertEquals(1000, instances.get(0).getProcessingUnits());
    assertEquals(INSTANCE_NAME2, instances.get(1).getId().getName());
    assertEquals(2000, instances.get(1).getProcessingUnits());
    assertEquals(2, instances.size());
  }

  @Test
  public void getInstanceIAMPolicy() {
    when(rpc.getInstanceAdminIAMPolicy(INSTANCE_NAME))
        .thenReturn(
            Policy.newBuilder()
                .addBindings(
                    Binding.newBuilder()
                        .addMembers("user:joe@example.com")
                        .setRole("roles/viewer")
                        .build())
                .build());
    com.google.cloud.Policy policy = client.getInstanceIAMPolicy(INSTANCE_ID);
    assertThat(policy.getBindings())
        .containsExactly(Role.viewer(), Sets.newHashSet(Identity.user("joe@example.com")));

    when(rpc.getInstanceAdminIAMPolicy(INSTANCE_NAME))
        .thenReturn(
            Policy.newBuilder()
                .addBindings(
                    Binding.newBuilder()
                        .addAllMembers(Arrays.asList("allAuthenticatedUsers", "domain:google.com"))
                        .setRole("roles/viewer")
                        .build())
                .build());
    policy = client.getInstanceIAMPolicy(INSTANCE_ID);
    assertThat(policy.getBindings())
        .containsExactly(
            Role.viewer(),
            Sets.newHashSet(Identity.allAuthenticatedUsers(), Identity.domain("google.com")));
  }

  @Test
  public void setInstanceIAMPolicy() {
    ByteString etag = ByteString.copyFrom(BaseEncoding.base64().decode("v1"));
    String etagEncoded = BaseEncoding.base64().encode(etag.toByteArray());
    Policy proto =
        Policy.newBuilder()
            .addBindings(
                Binding.newBuilder()
                    .setRole("roles/viewer")
                    .addMembers("user:joe@example.com")
                    .build())
            .setEtag(etag)
            .build();
    when(rpc.setInstanceAdminIAMPolicy(INSTANCE_NAME, proto)).thenReturn(proto);
    com.google.cloud.Policy policy =
        com.google.cloud.Policy.newBuilder()
            .addIdentity(Role.viewer(), Identity.user("joe@example.com"))
            .setEtag(etagEncoded)
            .build();
    com.google.cloud.Policy updated = client.setInstanceIAMPolicy(INSTANCE_ID, policy);
    assertThat(updated).isEqualTo(policy);
  }

  @Test
  public void testInstanceIAMPermissions() {
    Iterable<String> permissions =
        Arrays.asList("spanner.instances.list", "spanner.instances.create");
    when(rpc.testInstanceAdminIAMPermissions(INSTANCE_NAME, permissions))
        .thenReturn(
            TestIamPermissionsResponse.newBuilder()
                .addPermissions("spanner.instances.list")
                .build());
    Iterable<String> allowed = client.testInstanceIAMPermissions(INSTANCE_ID, permissions);
    assertThat(allowed).containsExactly("spanner.instances.list");
  }
}
