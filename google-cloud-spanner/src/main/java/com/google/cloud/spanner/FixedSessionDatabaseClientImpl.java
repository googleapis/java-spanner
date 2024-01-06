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

import com.google.cloud.spanner.SessionPool.PooledSessionFuture;
import com.google.cloud.spanner.SessionPool.PooledSessionFuture.ActionOnClose;

class FixedSessionDatabaseClientImpl extends DatabaseClientImpl implements AutoCloseable {
  private volatile PooledSessionFuture session;

  FixedSessionDatabaseClientImpl(String clientId, SessionPool pool) {
    super(clientId, pool);
  }

  @Override
  public void close() {
    synchronized (this) {
      if (this.session != null) {
        this.session.setActionOnClose(ActionOnClose.RELEASE);
        this.session.close();
        this.session = null;
      }
    }
  }

  @Override
  PooledSessionFuture getSession() {
    if (session == null) {
      synchronized (this) {
        if (this.session == null) {
          this.session = pool.getSession();
          this.session.setActionOnClose(ActionOnClose.NONE);
        }
      }
    }
    return session;
  }

  @Override
  PooledSessionFuture replaceSession(
      SessionNotFoundException sessionNotFoundException, PooledSessionFuture session) {
    synchronized (this) {
      this.session = super.replaceSession(sessionNotFoundException, session);
      return this.session;
    }
  }
}
