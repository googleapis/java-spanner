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
