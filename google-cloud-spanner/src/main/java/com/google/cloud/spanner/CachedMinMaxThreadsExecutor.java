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

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

/**
 * {@link ThreadPoolExecutor} that uses a cached thread pool with a min and a max number of threads.
 */
public class CachedMinMaxThreadsExecutor extends ThreadPoolExecutor {

  /**
   * Creates a new {@link ExecutorService} that uses a cached thread pool with a min and a max
   * number of threads for the underlying thread pool.
   */
  public static ExecutorService newCachedMinMaxThreadPoolExecutor(
      int numCoreThreads,
      int maxNumThreads,
      long keepAliveTime,
      TimeUnit timeUnit,
      ThreadFactory threadFactory) {
    WorkQueue queue = new WorkQueue();
    ThreadPoolExecutor executor =
        new CachedMinMaxThreadsExecutor(
            numCoreThreads, maxNumThreads, keepAliveTime, timeUnit, queue, threadFactory);
    executor.setRejectedExecutionHandler(ForceQueuePolicy.INSTANCE);
    queue.setThreadPoolExecutor(executor);

    return executor;
  }

  /** Work queue for {@link CachedMinMaxThreadsExecutor}. */
  private static class WorkQueue extends LinkedBlockingQueue<Runnable> {
    private ThreadPoolExecutor executor;

    void setThreadPoolExecutor(ThreadPoolExecutor executor) {
      this.executor = executor;
    }

    @Override
    public boolean offer(@Nonnull Runnable work) {
      // Calculate the number of running tasks + queued tasks.
      int currentTaskCount = executor.getActiveCount() + size();
      // Accept the task if there are more threads in the pool than active tasks.
      // Reject it if the current task count occupies all threads in the pool.
      // This will trigger the RejectedExecutionHandler to be triggered, which again
      // will add the work to the queue. That again will trigger the creation of more
      // threads, as long as the thread count won't exceed the maximum thread count.
      return currentTaskCount < executor.getPoolSize() && super.offer(work);
    }
  }

  private static class ForceQueuePolicy implements RejectedExecutionHandler {
    private static final ForceQueuePolicy INSTANCE = new ForceQueuePolicy();

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
      Uninterruptibles.putUninterruptibly(executor.getQueue(), runnable);
    }
  }

  private final AtomicInteger activeCount = new AtomicInteger();

  private CachedMinMaxThreadsExecutor(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
  }

  @Override
  public int getActiveCount() {
    return activeCount.get();
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    activeCount.incrementAndGet();
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    activeCount.decrementAndGet();
  }
}
