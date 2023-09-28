/*
 * Copyright 2023 Google LLC
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

package com.google.cloud.spanner;

import java.util.concurrent.ThreadLocalRandom;

public class AbstractAnonymousSessionsBenchmark extends AbstractLatencyBenchmark {
  static final String SELECT_QUERY = "SELECT ID FROM FOO WHERE ID = @id";
  static final String UPDATE_QUERY = "UPDATE FOO SET BAR=1 WHERE ID = @id";
  private static final int TOTAL_READS = 300000;
  private static final int TOTAL_WRITES = 100000;
  static final int PARALLEL_THREADS = 25;
  static final int TOTAL_READS_PER_THREAD = TOTAL_READS/PARALLEL_THREADS;
  static final int TOTAL_WRITES_PER_THREAD = TOTAL_WRITES/PARALLEL_THREADS;

  static final int WARMUP_TRANSACTIONS = PARALLEL_THREADS * 3;
  static final int RANDOM_SEARCH_SPACE = 99999;
  static Statement getRandomisedReadStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(RANDOM_SEARCH_SPACE);
    return Statement.newBuilder(SELECT_QUERY).bind("id").to(randomKey).build();
  }

  static Statement getRandomisedUpdateStatement() {
    int randomKey = ThreadLocalRandom.current().nextInt(RANDOM_SEARCH_SPACE);
    return Statement.newBuilder(UPDATE_QUERY).bind("id").to(randomKey).build();
  }
}
