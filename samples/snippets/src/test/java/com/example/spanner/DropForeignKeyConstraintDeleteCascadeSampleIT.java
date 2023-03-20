package com.example.spanner;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class DropForeignKeyConstraintDeleteCascadeSampleIT extends SampleTestBase {

  @Test
  public void testDropForeignKeyConstraintDeleteCascade() throws Exception {

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
            + "              CONSTRAINT FKShoppingCartsCustomerName FOREIGN KEY (CustomerName)\n"
            + "              REFERENCES Customers (CustomerName) ON DELETE CASCADE\n"
            + "              ) PRIMARY KEY (CartId)\n")
    ).get(5, TimeUnit.MINUTES);

    // Runs sample
    final String out = SampleRunner.runSample(() -> DropForeignKeyConstraintDeleteCascadeSample
        .deleteForeignKeyDeleteCascadeConstraint(databaseAdminClient, instanceId, databaseId)
    );

    assertTrue(
        "Expected to have dropped foreign-key constraints from tables in created database "
            + databaseId , out.contains("Altered ShoppingCarts table to drop FKShoppingCartsCustomerName"
        )
    );
  }
}