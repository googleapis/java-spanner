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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DdlClientTests {
  private final String projectId = "test-project";
  private final String instanceId = "test-instance";
  private final String databaseId = "test-database";

  private DdlClient createSubject(DatabaseAdminClient client) {
    return DdlClient.newBuilder()
        .setProjectId(projectId)
        .setInstanceId(instanceId)
        .setDatabaseName(databaseId)
        .setDatabaseAdminClient(client)
        .build();
  }

  @Test
  public void testIsCreateDatabase() {
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE DATABASE foo"));
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE DATABASE \"foo\""));
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE DATABASE `foo`"));
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE DATABASE\tfoo"));
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE DATABASE\n foo"));
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE DATABASE\t\n foo"));
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE DATABASE"));
    assertTrue(DdlClient.isCreateDatabaseStatement("CREATE\t \n DATABASE  foo"));
    assertTrue(DdlClient.isCreateDatabaseStatement("create\t \n DATABASE  foo"));
    assertTrue(DdlClient.isCreateDatabaseStatement("create database foo"));

    assertFalse(DdlClient.isCreateDatabaseStatement("CREATE VIEW foo"));
    assertFalse(DdlClient.isCreateDatabaseStatement("CREATE DATABAS foo"));
    assertFalse(DdlClient.isCreateDatabaseStatement("CREATE DATABASEfoo"));
    assertFalse(DdlClient.isCreateDatabaseStatement("CREATE foo"));
  }
}
