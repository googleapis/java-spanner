package com.example.spanner;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class CreateTableWithForeignKeyDeleteCascadeSampleIT extends SampleTestBase {

  @Test
  public void testCreateTableWithForeignKeyDeleteCascade() throws Exception {

    // Creates database
    final String databaseId = idGenerator.generateDatabaseId();
    databaseAdminClient.createDatabase(instanceId, databaseId, Arrays.asList()
    ).get(5, TimeUnit.MINUTES);

    // Runs sample
    final String out = SampleRunner.runSample(() -> CreateTableWithForeignKeyDeleteCascadeSample
        .createForeignKeyDeleteCascadeConstraint(databaseAdminClient, instanceId, databaseId)
    );

    assertTrue(
        "Expected to have created database " + databaseId + " with tables containing "
            + "foreign key constraints.", out.contains("Created Customers and ShoppingCarts table "
            + "with FKShoppingCartsCustomerId"
        )
    );
  }
}