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

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseInfo.State;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Unit tests for {@link com.google.cloud.spanner.Database}. */
@RunWith(JUnit4.class)
public class DatabaseTest {
  private static final String NAME =
      "projects/test-project/instances/test-instance/databases/database-1";

  private static final Timestamp EARLIEST_VERSION_TIME = Timestamp.now();
  private static final String VERSION_RETENTION_PERIOD = "7d";

  @Mock DatabaseAdminClient dbClient;

  @Before
  public void setUp() {
    initMocks(this);
    when(dbClient.newBackupBuilder(Mockito.any(BackupId.class)))
        .thenAnswer(
            new Answer<Backup.Builder>() {
              @Override
              public Backup.Builder answer(InvocationOnMock invocation) {
                return new Backup.Builder(dbClient, (BackupId) invocation.getArguments()[0]);
              }
            });
  }

  @Test
  public void backup() {
    Timestamp expireTime = Timestamp.now();
    Database db = createDatabase();
    db.backup(
        dbClient
            .newBackupBuilder(BackupId.of("test-project", "test-instance", "test-backup"))
            .setExpireTime(expireTime)
            .build());
    verify(dbClient).createBackup("test-instance", "test-backup", "database-1", expireTime);
  }

  @Test
  public void listDatabaseOperations() {
    Database db = createDatabase();
    db.listDatabaseOperations();
    verify(dbClient)
        .listDatabaseOperations("test-instance", Options.filter("name:databases/database-1"));
  }

  @Test
  public void fromProto() {
    Database db = createDatabase();
    assertThat(db.getId().getName()).isEqualTo(NAME);
    assertThat(db.getState()).isEqualTo(DatabaseInfo.State.CREATING);
    assertThat(db.getVersionRetentionPeriod()).isEqualTo(VERSION_RETENTION_PERIOD);
    assertThat(db.getEarliestVersionTime()).isEqualTo(EARLIEST_VERSION_TIME);
  }

  @Test
  public void getIAMPolicy() {
    Database database =
        new Database(
            DatabaseId.of("test-project", "test-instance", "test-database"), State.READY, dbClient);
    database.getIAMPolicy();
    verify(dbClient).getDatabaseIAMPolicy("test-instance", "test-database");
  }

  @Test
  public void setIAMPolicy() {
    Database database =
        new Database(
            DatabaseId.of("test-project", "test-instance", "test-database"), State.READY, dbClient);
    Policy policy =
        Policy.newBuilder().addIdentity(Role.editor(), Identity.user("joe@example.com")).build();
    database.setIAMPolicy(policy);
    verify(dbClient).setDatabaseIAMPolicy("test-instance", "test-database", policy);
  }

  @Test
  public void testIAMPermissions() {
    Database database =
        new Database(
            DatabaseId.of("test-project", "test-instance", "test-database"), State.READY, dbClient);
    Iterable<String> permissions = Arrays.asList("read");
    database.testIAMPermissions(permissions);
    verify(dbClient).testDatabaseIAMPermissions("test-instance", "test-database", permissions);
  }

  private Database createDatabase() {
    com.google.spanner.admin.database.v1.Database proto =
        com.google.spanner.admin.database.v1.Database.newBuilder()
            .setName(NAME)
            .setState(com.google.spanner.admin.database.v1.Database.State.CREATING)
            .setEarliestVersionTime(EARLIEST_VERSION_TIME.toProto())
            .setVersionRetentionPeriod(VERSION_RETENTION_PERIOD)
            .build();
    return Database.fromProto(proto, dbClient);
  }
}
