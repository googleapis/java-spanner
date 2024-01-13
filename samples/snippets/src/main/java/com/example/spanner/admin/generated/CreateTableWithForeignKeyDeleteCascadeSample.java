/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spanner.admin.generated;

// [START spanner_create_table_with_foreign_key_delete_cascade]

import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.CreateDatabaseRequest;
import com.google.spanner.admin.database.v1.DatabaseName;
import java.io.IOException;

class CreateTableWithForeignKeyDeleteCascadeSample {

  static void createForeignKeyDeleteCascadeConstraint() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";

    createForeignKeyDeleteCascadeConstraint(projectId, instanceId, databaseId);
  }

  static void createForeignKeyDeleteCascadeConstraint(
      String projectId, String instanceId, String databaseId) throws IOException {
    DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create();
    databaseAdminClient.updateDatabaseDdlAsync(
        DatabaseName.of(projectId, instanceId, databaseId),
        ImmutableList.of(
            "CREATE TABLE Customers (\n"
                + "              CustomerId INT64 NOT NULL,\n"
                + "              CustomerName STRING(62) NOT NULL,\n"
                + "              ) PRIMARY KEY (CustomerId)",
            "CREATE TABLE ShoppingCarts (\n"
                + "              CartId INT64 NOT NULL,\n"
                + "              CustomerId INT64 NOT NULL,\n"
                + "              CustomerName STRING(62) NOT NULL,\n"
                + "              CONSTRAINT FKShoppingCartsCustomerId FOREIGN KEY (CustomerId)\n"
                + "              REFERENCES Customers (CustomerId) ON DELETE CASCADE\n"
                + "              ) PRIMARY KEY (CartId)\n"));

    System.out.printf(
        String.format(
            "Created Customers and ShoppingCarts table with FKShoppingCartsCustomerId\n"
                + "foreign key constraint on database %s on instance %s\n",
            databaseId, instanceId));
  }
}
// [END spanner_create_table_with_foreign_key_delete_cascade]
