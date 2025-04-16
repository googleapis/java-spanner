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

package com.google.cloud.spanner;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;

public class PerformanceClock {

  public static final PerformanceClock BEFORE_GRPC_INSTANCE = new PerformanceClock();
  public static final PerformanceClock AFTER_GRPC_INSTANCE = new PerformanceClock();

  private final Stopwatch stopWatch;

  PerformanceClock() {
    this.stopWatch = Stopwatch.createUnstarted();
  }

  public void start() {
    stopWatch.start();
  }

  public void stop() {
    if (stopWatch.isRunning()) {
      stopWatch.stop();
    }
  }

  public void reset() {
    stopWatch.reset();
  }

  public long elapsed(TimeUnit unit) {
    return stopWatch.elapsed(unit);
  }
}
