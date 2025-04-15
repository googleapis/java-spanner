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
    stopWatch.stop();
  }

  public void reset() {
    stopWatch.reset();
  }

  public long elapsed(TimeUnit unit) {
    return stopWatch.elapsed(unit);
  }
}
