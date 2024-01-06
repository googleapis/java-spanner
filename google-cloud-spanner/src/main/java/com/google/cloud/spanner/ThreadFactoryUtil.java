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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadFactory;

/** Utility class for creating a thread factory for daemon or virtual threads. */
public class ThreadFactoryUtil {

  /**
   * Tries to create a thread factory for virtual threads, and otherwise falls back to creating a
   * platform thread factory that creates daemon threads. Virtual threads are supported from JDK21.
   *
   * @param baseNameFormat the base name format for the threads, '-%d' will be appended to the
   *     actual thread name format
   * @return a {@link ThreadFactory} that produces virtual threads (Java 21 or higher) or daemon
   *     threads
   */
  public static ThreadFactory createVirtualOrDaemonThreadFactory(String baseNameFormat) {
    ThreadFactory virtualThreadFactory = tryCreateVirtualThreadFactory(baseNameFormat);
    if (virtualThreadFactory != null) {
      return virtualThreadFactory;
    }

    return new ThreadFactoryBuilder().setDaemon(true).setNameFormat(baseNameFormat + "-%d").build();
  }

  /**
   * Tries to create a {@link ThreadFactory} that creates virtual threads. Returns null if virtual
   * threads are not supported on this JVM.
   */
  public static ThreadFactory tryCreateVirtualThreadFactory(String baseNameFormat) {
    try {
      Class<?> threadBuilderClass = Class.forName("java.lang.Thread$Builder");
      Method ofVirtualMethod = Thread.class.getDeclaredMethod("ofVirtual");
      Object virtualBuilder = ofVirtualMethod.invoke(null);
      Method nameMethod = threadBuilderClass.getDeclaredMethod("name", String.class, long.class);
      virtualBuilder = nameMethod.invoke(virtualBuilder, baseNameFormat + "-", 0);
      Method factoryMethod = threadBuilderClass.getDeclaredMethod("factory");
      return (ThreadFactory) factoryMethod.invoke(virtualBuilder);
    } catch (ClassNotFoundException | NoSuchMethodException ignore) {
      return null;
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
