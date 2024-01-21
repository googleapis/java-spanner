/*
 * Copyright 2024 Google LLC
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

import static org.junit.Assert.assertEquals;

import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CheckSumResultSetBenchmarkTest extends AbstractMockServerTest {

  @Test
  public void testBenchmarkChecksumResultSet() {
    int iterations = 1000;
    int rows = 100;
    RandomResultSetGenerator generator = new RandomResultSetGenerator(rows);
    Statement statement = Statement.of("select * from random");
    mockSpanner.putStatementResult(
        MockSpannerServiceImpl.StatementResult.query(statement, generator.generate()));

    try (Connection connection = createConnection()) {
      connection.setAutocommit(false);
      Stopwatch watch = Stopwatch.createStarted();
      for (int iteration = 0; iteration < iterations; iteration++) {
        try (ResultSet resultSet = connection.executeQuery(statement)) {
          int foundRows = 0;
          while (resultSet.next()) {
            foundRows++;
          }
          assertEquals(rows, foundRows);
        }
        connection.commit();
      }
      System.out.println(watch.elapsed(TimeUnit.MILLISECONDS) + "ms");

      // JVM  : 16612ms, 18125ms, 19090ms
      // Guava: 18797ms, 17131ms, 18022ms
    }
  }
}
