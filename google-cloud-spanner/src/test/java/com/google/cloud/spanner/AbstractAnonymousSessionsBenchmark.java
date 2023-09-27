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

import java.util.Random;

public class AbstractAnonymousSessionsBenchmark extends AbstractLatencyBenchmark {
  static final String SELECT_QUERY = "SELECT ID FROM FOO WHERE ID = @id";
  static final String UPDATE_QUERY = "UPDATE FOO SET BAR=1 WHERE ID = @id";
  static final int TOTAL_READS = 50000;
  static final int TOTAL_WRITES = 50000;
  static final int PARALLEL_THREADS = 25;

  static final int RANDOM_SEARCH_SPACE = 99999;
  static final Random RANDOM = new Random();
  static Statement getRandomisedReadStatement() {
    int randomKey = RANDOM.nextInt(RANDOM_SEARCH_SPACE);
    return Statement.newBuilder(SELECT_QUERY).bind("id").to(randomKey).build();
  }

  static Statement getRandomisedUpdateStatement() {
    int randomKey = RANDOM.nextInt(RANDOM_SEARCH_SPACE);
    return Statement.newBuilder(UPDATE_QUERY).bind("id").to(randomKey).build();
  }
}
