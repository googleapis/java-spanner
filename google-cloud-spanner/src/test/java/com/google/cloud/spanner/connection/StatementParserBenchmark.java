/*
 * Copyright 2025 Google LLC
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

import com.google.cloud.spanner.Dialect;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

@Fork(value = 1, warmups = 0)
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 5, time = 5)
public class StatementParserBenchmark {
  private static final Dialect dialect = Dialect.POSTGRESQL;
  private static final AbstractStatementParser parser =
      AbstractStatementParser.getInstance(dialect);

  private static String longQueryText = generateLongQuery(100 * 1024); // 100kb

  /** Generates a long SQL-looking string. */
  private static String generateLongQuery(int length) {
    StringBuilder sb = new StringBuilder(length + 50);
    sb.append("SELECT * FROM foo WHERE 1");
    while (sb.length() < length) {
      sb.append(" OR abcdefghijklmnopqrstuvwxyz");
    }
    return sb.toString();
  }

  @Benchmark
  public boolean isQueryTest() {
    return parser.isQuery("CREATE TABLE FOO (ID INT64, NAME STRING(100)) PRIMARY KEY (ID)");
  }

  @Benchmark
  public boolean longQueryTest() {
    return parser.isQuery(longQueryText);
  }
}
