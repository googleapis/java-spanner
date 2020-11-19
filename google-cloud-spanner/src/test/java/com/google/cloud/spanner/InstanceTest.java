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
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.common.testing.EqualsTester;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link Instance}. */
@RunWith(JUnit4.class)
public class InstanceTest {

  @Mock InstanceAdminClient instanceClient;
  @Mock DatabaseAdminClient dbClient;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void buildInstance() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    InstanceConfigId configId = new InstanceConfigId("test-project", "test-instance-config");
    Instance instance =
        new Instance.Builder(instanceClient, dbClient, id)
            .setInstanceConfigId(configId)
            .setDisplayName("test instance")
            .setNodeCount(1)
            .setProcessingUnits(2000)
            .setState(InstanceInfo.State.READY)
            .addLabel("env", "prod")
            .addLabel("region", "us")
            .build();
    assertThat(instance.getId()).isEqualTo(id);
    assertThat(instance.getInstanceConfigId()).isEqualTo(configId);
    assertThat(instance.getDisplayName()).isEqualTo("test instance");
    assertThat(instance.getNodeCount()).isEqualTo(1);
    assertThat(instance.getProcessingUnits()).isEqualTo(2000);
    assertThat(instance.getState()).isEqualTo(InstanceInfo.State.READY);
    assertThat(instance.getLabels()).containsExactly("env", "prod", "region", "us");

    instance = instance.toBuilder().setDisplayName("new test instance").build();
    assertThat(instance.getId()).isEqualTo(id);
    assertThat(instance.getInstanceConfigId()).isEqualTo(configId);
    assertThat(instance.getDisplayName()).isEqualTo("new test instance");
    assertThat(instance.getNodeCount()).isEqualTo(1);
    assertThat(instance.getProcessingUnits()).isEqualTo(2000);
    assertThat(instance.getState()).isEqualTo(InstanceInfo.State.READY);
    assertThat(instance.getLabels()).containsExactly("env", "prod", "region", "us");
  }

  @Test
  public void equality() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    InstanceConfigId configId = new InstanceConfigId("test-project", "test-instance-config");

    Instance instance =
        new Instance.Builder(instanceClient, dbClient, id)
            .setInstanceConfigId(configId)
            .setDisplayName("test instance")
            .setNodeCount(1)
            .setProcessingUnits(2000)
            .setState(InstanceInfo.State.READY)
            .addLabel("env", "prod")
            .addLabel("region", "us")
            .build();
    Instance instance2 =
        new Instance.Builder(instanceClient, dbClient, id)
            .setInstanceConfigId(configId)
            .setDisplayName("test instance")
            .setNodeCount(1)
            .setProcessingUnits(2000)
            .setState(InstanceInfo.State.READY)
            .addLabel("region", "us")
            .addLabel("env", "prod")
            .build();
    Instance instance3 =
        new Instance.Builder(instanceClient, dbClient, id)
            .setInstanceConfigId(configId)
            .setDisplayName("test instance")
            .setNodeCount(1)
            .setProcessingUnits(2000)
            .setState(InstanceInfo.State.READY)
            .addLabel("env", "prod")
            .build();
    EqualsTester tester = new EqualsTester();
    tester.addEqualityGroup(instance, instance2);
    tester.addEqualityGroup(instance3);
    tester.testEquals();
  }

  @Test
  public void listDatabases() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    Instance instance = new Instance.Builder(instanceClient, dbClient, id).build();
    instance.listDatabases();
    verify(dbClient).listDatabases("test-instance");
  }

  @Test
  public void listBackups() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    Instance instance = new Instance.Builder(instanceClient, dbClient, id).build();
    instance.listBackups();
    verify(dbClient).listBackups("test-instance");
  }

  @Test
  public void listDatabaseOperations() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    Instance instance = new Instance.Builder(instanceClient, dbClient, id).build();
    instance.listDatabaseOperations();
    verify(dbClient).listDatabaseOperations("test-instance");
  }

  @Test
  public void listBackupOperations() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    Instance instance = new Instance.Builder(instanceClient, dbClient, id).build();
    instance.listBackupOperations();
    verify(dbClient).listBackupOperations("test-instance");
  }

  @Test
  public void getIAMPolicy() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    Instance instance = new Instance.Builder(instanceClient, dbClient, id).build();
    instance.getIAMPolicy();
    verify(instanceClient).getInstanceIAMPolicy("test-instance");
  }

  @Test
  public void setIAMPolicy() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    Instance instance = new Instance.Builder(instanceClient, dbClient, id).build();
    Policy policy =
        Policy.newBuilder().addIdentity(Role.viewer(), Identity.user("joe@example.com")).build();
    instance.setIAMPolicy(policy);
    verify(instanceClient).setInstanceIAMPolicy("test-instance", policy);
  }

  @Test
  public void testIAMPermissions() {
    InstanceId id = new InstanceId("test-project", "test-instance");
    Instance instance = new Instance.Builder(instanceClient, dbClient, id).build();
    Iterable<String> permissions = Arrays.asList("read");
    instance.testIAMPermissions(permissions);
    verify(instanceClient).testInstanceIAMPermissions("test-instance", permissions);
  }
}
