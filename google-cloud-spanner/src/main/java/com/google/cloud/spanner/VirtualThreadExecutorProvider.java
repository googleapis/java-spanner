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

import static com.google.cloud.spanner.ThreadFactoryUtil.tryCreateVirtualThreadFactory;

import com.google.cloud.grpc.GrpcTransportOptions.ExecutorFactory;
import io.grpc.internal.SharedResourceHolder;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class VirtualThreadExecutorProvider {
  private static final ThreadFactory VIRTUAL_THREAD_FACTORY =
      tryCreateVirtualThreadFactory("grpc-virtual-transport");

  private static final SharedResourceHolder.Resource<ScheduledExecutorService> EXECUTOR =
      VIRTUAL_THREAD_FACTORY == null
          ? null
          : new SharedResourceHolder.Resource<ScheduledExecutorService>() {
            @Override
            public ScheduledExecutorService create() {
              ScheduledThreadPoolExecutor service =
                  new ScheduledThreadPoolExecutor(8, VIRTUAL_THREAD_FACTORY);
              service.setKeepAliveTime(5, TimeUnit.SECONDS);
              service.allowCoreThreadTimeOut(true);
              service.setRemoveOnCancelPolicy(true);
              return service;
            }

            @Override
            public void close(ScheduledExecutorService instance) {
              instance.shutdown();
            }
          };

  static boolean supportsVirtualThreads() {
    return EXECUTOR != null;
  }

  static class VirtualExecutorFactory implements ExecutorFactory<ScheduledExecutorService> {

    static final VirtualExecutorFactory INSTANCE = new VirtualExecutorFactory();

    @Override
    public ScheduledExecutorService get() {
      return SharedResourceHolder.get(EXECUTOR);
    }

    @Override
    public synchronized void release(ScheduledExecutorService executor) {
      SharedResourceHolder.release(EXECUTOR, executor);
    }
  }
}
