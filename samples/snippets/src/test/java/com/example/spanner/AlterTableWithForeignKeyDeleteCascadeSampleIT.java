package com.example.spanner;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class AlterTableWithForeignKeyDeleteCascadeSampleIT extends SampleTestBase {

  @Test
  public void testAlterTableWithForeignKeyDeleteCascade() throws Exception {

    // Creates database
    final String databaseId = idGenerator.generateDatabaseId();
    databaseAdminClient.createDatabase(instanceId, databaseId, Arrays.asList("CREATE TABLE Customers (\n"
            + "              CustomerId INT64 NOT NULL,\n"
            + "              CustomerName STRING(62) NOT NULL,\n"
            + "              ) PRIMARY KEY (CustomerId)",
        "CREATE TABLE ShoppingCarts (\n"
            + "              CartId INT64 NOT NULL,\n"
            + "              CustomerId INT64 NOT NULL,\n"
            + "              CustomerName STRING(62) NOT NULL,\n"
            + "              CONSTRAINT FKShoppingCartsCustomerId FOREIGN KEY (CustomerId)\n"
            + "              REFERENCES Customers (CustomerId) ON DELETE CASCADE\n"
            + "              ) PRIMARY KEY (CartId)\n")
    ).get(5, TimeUnit.MINUTES);

    // Runs sample
    final String out = SampleRunner.runSample(() -> AlterTableWithForeignKeyDeleteCascadeSample
        .alterForeignKeyDeleteCascadeConstraint(databaseAdminClient, instanceId, databaseId)
    );

    assertTrue(
        "Expected to have created database " + databaseId + " with tables containing "
            + "foreign key constraints.", out.contains("Altered ShoppingCarts table "
            + "with FKShoppingCartsCustomerName"
        )
    );
  }
}