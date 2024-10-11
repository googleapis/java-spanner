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

package com.google.cloud.spanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CachedMinMaxThreadExecutorTest {

  @Test
  public void testMinMaxExecutor() throws Exception {
    String format = "test-thread-pool-%d";
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat(format).build();
    CachedMinMaxThreadsExecutor executor =
        (CachedMinMaxThreadsExecutor)
            CachedMinMaxThreadsExecutor.newCachedMinMaxThreadPoolExecutor(
                1, 2, 1L, TimeUnit.NANOSECONDS, threadFactory);
    CountDownLatch startLatch = new CountDownLatch(2);
    CountDownLatch continueLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(3);
    Callable<Void> callable =
        () -> {
          startLatch.countDown();
          continueLatch.await();
          finishLatch.countDown();
          return null;
        };
    executor.submit(callable);
    executor.submit(callable);
    executor.submit(callable);

    // Wait until 2 of the tasks have started.
    assertTrue(startLatch.await(1L, TimeUnit.SECONDS));
    // Verify that we have 2 concurrent threads.
    assertEquals(2, executor.getActiveCount());
    assertEquals(2, executor.getPoolSize());
    // Allow the tasks to continue.
    continueLatch.countDown();
    // Verify that all three tasks finish.
    assertTrue(finishLatch.await(1L, TimeUnit.SECONDS));
    // Verify that the max pool size was 2.
    assertEquals(2, executor.getMaximumPoolSize());
    // Verify that the pool scales back down to 1 core thread.
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (stopwatch.elapsed(TimeUnit.MILLISECONDS) < 1000 && executor.getPoolSize() > 1) {
      Thread.yield();
    }
    assertEquals(1, executor.getPoolSize());

    executor.shutdown();
  }
}
