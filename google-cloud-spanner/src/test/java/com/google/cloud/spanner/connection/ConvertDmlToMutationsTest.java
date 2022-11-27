/*
 * Copyright 2022 Google LLC
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

import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConvertDmlToMutationsTest extends AbstractMockServerTest {

  @Test
  public void testConvertInsert() {
    try (Connection connection = createConnection()) {
      try (ResultSet resultSet = connection.executeQuery(Statement.of("SELECT 1"))) {
        while (resultSet.next()) {
          System.out.println(resultSet.getCurrentRowAsStruct());
        }
      }
    }
  }
}
