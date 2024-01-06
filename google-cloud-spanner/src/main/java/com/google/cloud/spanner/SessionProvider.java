/*
 * Copyright 2017 Google LLC
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

import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SpannerImpl.ClosedException;
import com.google.common.util.concurrent.ListenableFuture;

interface SessionProvider {

  /** Returns the dialect that is used by the underlying database. */
  Dialect getDialect();

  /** Returns the database role that has been configured. */
  String getDatabaseRole();

  boolean isValid();

  /** Returns a session to use for a transaction. */
  PooledSessionFuture getSession();

  /**
   * Replaces the given session with a new one, as the original session has been garbage collected
   * by the backend.
   */
  PooledSessionFuture replaceSession(
      SessionNotFoundException sessionNotFoundException, PooledSessionFuture pooledSessionFuture);

  /**
   * Closes the {@link SessionProvider}. The implementation can choose to actively delete all
   * sessions that are managed by it, or to let the sessions be garbage collected by the backend.
   * The {@link SessionProvider} is no longer usable after call ing this method.
   */
  ListenableFuture<Void> closeAsync(ClosedException closedException);
}
