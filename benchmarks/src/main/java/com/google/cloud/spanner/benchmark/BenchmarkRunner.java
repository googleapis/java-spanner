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

package com.google.cloud.spanner.benchmark;

public interface BenchmarkRunner {
  enum TransactionType {
    READ_ONLY_SINGLE_USE,
    READ_ONLY_MULTI_USE,
    READ_WRITE
  }

  void execute(
      TransactionType transactionType,
      int numClients,
      int numOperations,
      int waitMillis,
      boolean useMultiplexedSession,
      int warmUpMinutes);
}
