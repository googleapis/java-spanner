/*
 * Copyright 2021 Google LLC
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

package com.google.cloud.spanner.it;

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Category(ParallelIntegrationTest.class)
@RunWith(JUnit4.class)
public class ITJsonWriteReadTest {

  private final String RESOURCES_FOLDER = "com/google/cloud/spanner/it";
  private static final String TABLE_NAME = "TestTable";

  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();

  private static DatabaseClient databaseClient;

  @BeforeClass
  public static void beforeClass() {
    final RemoteSpannerHelper testHelper = env.getTestHelper();
    final Database database =
        testHelper.createTestDatabase(
            "CREATE TABLE "
                + TABLE_NAME
                + "("
                + "Id INT64 NOT NULL,"
                + "json JSON"
                // + "jsonArray ARRAY<JSON>"
                + ") PRIMARY KEY (Id)");

    databaseClient = testHelper.getDatabaseClient(database);
  }

  @Test
  public void testWriteAndReadJsonValues() throws IOException {
    List<String> resources = getPathsFromResource(RESOURCES_FOLDER + "/accept");
    long id = 1L;
    for (String resource : resources) {
      System.out.println("Writing file: " + resource);
      String jsonStr = Resources.toString(
          Resources.getResource(this.getClass(), "accept/" + resource), StandardCharsets.UTF_8);
      try {
        databaseClient.write(
            Collections.singletonList(
                Mutation.newInsertBuilder(TABLE_NAME)
                    .set("Id")
                    .to(id)
                    .set("json")
                    .to(Value.json(jsonStr))
                    // .set("jsonArray")
                    // .toJsonArray(Collections.emptyList())
                    .build()));
        try (ResultSet resultSet =
            databaseClient
                .singleUse()
                .executeQuery(Statement.of("SELECT * FROM " + TABLE_NAME + " WHERE Id = " + id))) {
          resultSet.next();

          assertEquals(jsonStr, resultSet.getJson("json"));
        }
        id++;
      } catch (Exception e) {
        System.out.println("Failed to insert JSON: " + jsonStr + ". From: " + resource);
        e.printStackTrace();
      }
    }
  }

  private List<String> getPathsFromResource(String folder) {
    String fixturesRoot = Resources.getResource(folder).getPath();
    final Path fixturesRootPath = Paths.get(fixturesRoot);
    try {
      return Files.walk(fixturesRootPath)
          .filter(Files::isRegularFile)
          .map(path -> fixturesRootPath.relativize(path).toString())
          .filter(x -> x.endsWith(".json"))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
