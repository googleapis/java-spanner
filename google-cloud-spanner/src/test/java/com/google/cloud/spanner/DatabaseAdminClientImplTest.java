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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.Identity;
import com.google.cloud.Role;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.spi.v1.SpannerRpc.Paginated;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.CreateBackupMetadata;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.database.v1.RestoreDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link com.google.cloud.spanner.SpannerImpl.DatabaseAdminClientImpl}. */
@RunWith(JUnit4.class)
public class DatabaseAdminClientImplTest {
  private static final String PROJECT_ID = "my-project";
  private static final String INSTANCE_ID = "my-instance";
  private static final String INSTANCE_NAME = "projects/my-project/instances/my-instance";
  private static final String DB_ID = "my-db";
  private static final String DB_NAME = "projects/my-project/instances/my-instance/databases/my-db";
  private static final String DB_NAME2 =
      "projects/my-project/instances/my-instance/databases/my-db2";
  private static final String BK_ID = "my-bk";
  private static final String BK_NAME = "projects/my-project/instances/my-instance/backups/my-bk";
  private static final String BK_NAME2 = "projects/my-project/instances/my-instance/backups/my-bk2";

  @Mock SpannerRpc rpc;
  DatabaseAdminClientImpl client;

  @Before
  public void setUp() {
    initMocks(this);
    client = new DatabaseAdminClientImpl(PROJECT_ID, rpc);
  }

  private Database getDatabaseProto() {
    return Database.newBuilder().setName(DB_NAME).setState(Database.State.READY).build();
  }

  private Database getAnotherDatabaseProto() {
    return Database.newBuilder().setName(DB_NAME2).setState(Database.State.READY).build();
  }

  static Any toAny(Message message) {
    return Any.newBuilder()
        .setTypeUrl("type.googleapis.com/" + message.getDescriptorForType().getFullName())
        .setValue(message.toByteString())
        .build();
  }

  private Backup getBackupProto() {
    return Backup.newBuilder()
        .setName(BK_NAME)
        .setDatabase(DB_NAME)
        .setState(Backup.State.READY)
        .build();
  }

  private Backup getAnotherBackupProto() {
    return Backup.newBuilder()
        .setName(BK_NAME2)
        .setDatabase(DB_NAME2)
        .setState(Backup.State.READY)
        .build();
  }

  @Test
  public void getDatabase() {
    when(rpc.getDatabase(DB_NAME)).thenReturn(getDatabaseProto());
    com.google.cloud.spanner.Database db = client.getDatabase(INSTANCE_ID, DB_ID);
    assertThat(db.getId().getName()).isEqualTo(DB_NAME);
    assertThat(db.getState()).isEqualTo(DatabaseInfo.State.READY);
  }

  @Test
  public void createDatabase() throws Exception {
    OperationFuture<Database, CreateDatabaseMetadata> rawOperationFuture =
        OperationFutureUtil.immediateOperationFuture(
            "createDatabase", getDatabaseProto(), CreateDatabaseMetadata.getDefaultInstance());
    when(rpc.createDatabase(
            INSTANCE_NAME, "CREATE DATABASE `" + DB_ID + "`", Collections.<String>emptyList()))
        .thenReturn(rawOperationFuture);
    OperationFuture<com.google.cloud.spanner.Database, CreateDatabaseMetadata> op =
        client.createDatabase(INSTANCE_ID, DB_ID, Collections.<String>emptyList());
    assertThat(op.isDone()).isTrue();
    assertThat(op.get().getId().getName()).isEqualTo(DB_NAME);
  }

  @Test
  public void updateDatabaseDdl() throws Exception {
    String opName = DB_NAME + "/operations/myop";
    String opId = "myop";
    List<String> ddl = ImmutableList.of();
    OperationFuture<Empty, UpdateDatabaseDdlMetadata> rawOperationFuture =
        OperationFutureUtil.immediateOperationFuture(
            opName, Empty.getDefaultInstance(), UpdateDatabaseDdlMetadata.getDefaultInstance());
    when(rpc.updateDatabaseDdl(DB_NAME, ddl, opId)).thenReturn(rawOperationFuture);
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op =
        client.updateDatabaseDdl(INSTANCE_ID, DB_ID, ddl, opId);
    assertThat(op.isDone()).isTrue();
    assertThat(op.getName()).isEqualTo(opName);
  }

  @Test
  public void updateDatabaseDdlOpAlreadyExists() throws Exception {
    String originalOpName = DB_NAME + "/operations/originalop";
    List<String> ddl = ImmutableList.of();
    OperationFuture<Empty, UpdateDatabaseDdlMetadata> originalOp =
        OperationFutureUtil.immediateOperationFuture(
            originalOpName,
            Empty.getDefaultInstance(),
            UpdateDatabaseDdlMetadata.getDefaultInstance());

    String newOpId = "newop";
    when(rpc.updateDatabaseDdl(DB_NAME, ddl, newOpId)).thenReturn(originalOp);
    OperationFuture<Void, UpdateDatabaseDdlMetadata> op =
        client.updateDatabaseDdl(INSTANCE_ID, DB_ID, ddl, newOpId);
    assertThat(op.getName()).isEqualTo(originalOpName);
  }

  @Test
  public void dropDatabase() {
    client.dropDatabase(INSTANCE_ID, DB_ID);
    verify(rpc).dropDatabase(DB_NAME);
  }

  @Test
  public void getDatabaseDdl() {
    List<String> ddl = ImmutableList.of("CREATE TABLE mytable()");
    when(rpc.getDatabaseDdl(DB_NAME)).thenReturn(ddl);
    assertThat(client.getDatabaseDdl(INSTANCE_ID, DB_ID)).isEqualTo(ddl);
  }

  @Test
  public void listDatabases() {
    String pageToken = "token";
    when(rpc.listDatabases(INSTANCE_NAME, 1, null))
        .thenReturn(new Paginated<>(ImmutableList.<Database>of(getDatabaseProto()), pageToken));
    when(rpc.listDatabases(INSTANCE_NAME, 1, pageToken))
        .thenReturn(new Paginated<>(ImmutableList.<Database>of(getAnotherDatabaseProto()), ""));
    List<com.google.cloud.spanner.Database> dbs =
        Lists.newArrayList(client.listDatabases(INSTANCE_ID, Options.pageSize(1)).iterateAll());
    assertThat(dbs.get(0).getId().getName()).isEqualTo(DB_NAME);
    assertThat(dbs.get(1).getId().getName()).isEqualTo(DB_NAME2);
    assertThat(dbs.size()).isEqualTo(2);
  }

  @Test
  public void listDatabasesError() {
    when(rpc.listDatabases(INSTANCE_NAME, 1, null))
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(ErrorCode.INVALID_ARGUMENT, "Test error"));
    try {
      client.listDatabases(INSTANCE_ID, Options.pageSize(1));
      Assert.fail("Missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getMessage()).contains(INSTANCE_NAME);
      // Assert that the call was done without a page token.
      assertThat(e.getMessage()).contains("with pageToken <null>");
    }
  }

  @Test
  public void listDatabaseErrorWithToken() {
    String pageToken = "token";
    when(rpc.listDatabases(INSTANCE_NAME, 1, null))
        .thenReturn(new Paginated<>(ImmutableList.<Database>of(getDatabaseProto()), pageToken));
    when(rpc.listDatabases(INSTANCE_NAME, 1, pageToken))
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(ErrorCode.INVALID_ARGUMENT, "Test error"));
    try {
      Lists.newArrayList(client.listDatabases(INSTANCE_ID, Options.pageSize(1)).iterateAll());
      Assert.fail("Missing expected exception");
    } catch (SpannerException e) {
      assertThat(e.getMessage()).contains(INSTANCE_NAME);
      // Assert that the call was done without a page token.
      assertThat(e.getMessage()).contains(String.format("with pageToken %s", pageToken));
    }
  }

  @Test
  public void getDatabaseIAMPolicy() {
    when(rpc.getDatabaseAdminIAMPolicy(DB_NAME))
        .thenReturn(
            Policy.newBuilder()
                .addBindings(
                    Binding.newBuilder()
                        .addMembers("user:joe@example.com")
                        .setRole("roles/viewer")
                        .build())
                .build());
    com.google.cloud.Policy policy = client.getDatabaseIAMPolicy(INSTANCE_ID, DB_ID);
    assertThat(policy.getBindings())
        .containsExactly(Role.viewer(), Sets.newHashSet(Identity.user("joe@example.com")));

    when(rpc.getDatabaseAdminIAMPolicy(DB_NAME))
        .thenReturn(
            Policy.newBuilder()
                .addBindings(
                    Binding.newBuilder()
                        .addAllMembers(Arrays.asList("allAuthenticatedUsers", "domain:google.com"))
                        .setRole("roles/viewer")
                        .build())
                .build());
    policy = client.getDatabaseIAMPolicy(INSTANCE_ID, DB_ID);
    assertThat(policy.getBindings())
        .containsExactly(
            Role.viewer(),
            Sets.newHashSet(Identity.allAuthenticatedUsers(), Identity.domain("google.com")));
  }

  @Test
  public void setDatabaseIAMPolicy() {
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
    when(rpc.setDatabaseAdminIAMPolicy(DB_NAME, proto)).thenReturn(proto);
    com.google.cloud.Policy policy =
        com.google.cloud.Policy.newBuilder()
            .addIdentity(Role.viewer(), Identity.user("joe@example.com"))
            .setEtag(etagEncoded)
            .build();
    com.google.cloud.Policy updated = client.setDatabaseIAMPolicy(INSTANCE_ID, DB_ID, policy);
    assertThat(updated).isEqualTo(policy);
  }

  @Test
  public void testDatabaseIAMPermissions() {
    Iterable<String> permissions =
        Arrays.asList("spanner.databases.select", "spanner.databases.write");
    when(rpc.testDatabaseAdminIAMPermissions(DB_NAME, permissions))
        .thenReturn(
            TestIamPermissionsResponse.newBuilder()
                .addPermissions("spanner.databases.select")
                .build());
    Iterable<String> allowed = client.testDatabaseIAMPermissions(INSTANCE_ID, DB_ID, permissions);
    assertThat(allowed).containsExactly("spanner.databases.select");
  }

  @Test
  public void createBackup() throws Exception {
    OperationFuture<Backup, CreateBackupMetadata> rawOperationFuture =
        OperationFutureUtil.immediateOperationFuture(
            "createBackup", getBackupProto(), CreateBackupMetadata.getDefaultInstance());
    Timestamp t =
        Timestamp.ofTimeMicroseconds(
            TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis())
                + TimeUnit.HOURS.toMicros(28));
    Backup backup = Backup.newBuilder().setDatabase(DB_NAME).setExpireTime(t.toProto()).build();
    when(rpc.createBackup(INSTANCE_NAME, BK_ID, backup)).thenReturn(rawOperationFuture);
    OperationFuture<com.google.cloud.spanner.Backup, CreateBackupMetadata> op =
        client.createBackup(INSTANCE_ID, BK_ID, DB_ID, t);
    assertThat(op.isDone()).isTrue();
    assertThat(op.get().getId().getName()).isEqualTo(BK_NAME);
  }

  @Test
  public void deleteBackup() {
    client.deleteBackup(INSTANCE_ID, BK_ID);
    verify(rpc).deleteBackup(BK_NAME);
  }

  @Test
  public void getBackup() {
    when(rpc.getBackup(BK_NAME)).thenReturn(getBackupProto());
    com.google.cloud.spanner.Backup bk = client.getBackup(INSTANCE_ID, BK_ID);
    BackupId bid = BackupId.of(bk.getId().getName());
    assertThat(bid.getName()).isEqualTo(BK_NAME);
    assertThat(bk.getState()).isEqualTo(com.google.cloud.spanner.Backup.State.READY);
  }

  @Test
  public void listBackups() {
    String pageToken = "token";
    when(rpc.listBackups(INSTANCE_NAME, 1, null, null))
        .thenReturn(new Paginated<>(ImmutableList.<Backup>of(getBackupProto()), pageToken));
    when(rpc.listBackups(INSTANCE_NAME, 1, null, pageToken))
        .thenReturn(new Paginated<>(ImmutableList.<Backup>of(getAnotherBackupProto()), ""));
    List<com.google.cloud.spanner.Backup> backups =
        Lists.newArrayList(client.listBackups(INSTANCE_ID, Options.pageSize(1)).iterateAll());
    assertThat(backups.get(0).getId().getName()).isEqualTo(BK_NAME);
    assertThat(backups.get(1).getId().getName()).isEqualTo(BK_NAME2);
    assertThat(backups.size()).isEqualTo(2);
  }

  @Test
  public void updateBackup() {
    Timestamp t =
        Timestamp.ofTimeMicroseconds(
            TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis())
                + TimeUnit.HOURS.toMicros(28));
    Backup backup = Backup.newBuilder().setName(BK_NAME).setExpireTime(t.toProto()).build();
    when(rpc.updateBackup(backup, FieldMask.newBuilder().addPaths("expire_time").build()))
        .thenReturn(
            Backup.newBuilder()
                .setName(BK_NAME)
                .setDatabase(DB_NAME)
                .setExpireTime(t.toProto())
                .build());
    com.google.cloud.spanner.Backup updatedBackup = client.updateBackup(INSTANCE_ID, BK_ID, t);
    assertThat(updatedBackup.getExpireTime()).isEqualTo(t);
  }

  @Test
  public void restoreDatabase() throws Exception {
    OperationFuture<Database, RestoreDatabaseMetadata> rawOperationFuture =
        OperationFutureUtil.immediateOperationFuture(
            "restoreDatabase", getDatabaseProto(), RestoreDatabaseMetadata.getDefaultInstance());
    when(rpc.restoreDatabase(INSTANCE_NAME, DB_ID, BK_NAME)).thenReturn(rawOperationFuture);
    OperationFuture<com.google.cloud.spanner.Database, RestoreDatabaseMetadata> op =
        client.restoreDatabase(INSTANCE_ID, BK_ID, INSTANCE_ID, DB_ID);
    assertThat(op.isDone()).isTrue();
    assertThat(op.get().getId().getName()).isEqualTo(DB_NAME);
  }
}
