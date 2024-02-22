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

// [START spanner_drop_foreign_key_constraint_delete_cascade]

import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.spanner.admin.database.v1.DatabaseName;
import java.io.IOException;

class DropForeignKeyConstraintDeleteCascadeSample {

  static void deleteForeignKeyDeleteCascadeConstraint() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project";
    String instanceId = "my-instance";
    String databaseId = "my-database";

    deleteForeignKeyDeleteCascadeConstraint(projectId, instanceId, databaseId);
  }

  static void deleteForeignKeyDeleteCascadeConstraint(
      String projectId, String instanceId, String databaseId) throws IOException {
    DatabaseAdminClient databaseAdminClient = DatabaseAdminClient.create();
    databaseAdminClient.updateDatabaseDdlAsync(
        DatabaseName.of(projectId, instanceId, databaseId),
        ImmutableList.of(
            "ALTER TABLE ShoppingCarts\n"
                + "              DROP CONSTRAINT FKShoppingCartsCustomerName\n"));

    System.out.printf(
        String.format(
            "Altered ShoppingCarts table to drop FKShoppingCartsCustomerName\n"
                + "foreign key constraint on database %s on instance %s\n",
            databaseId, instanceId));
  }
}
// [END spanner_drop_foreign_key_constraint_delete_cascade]
