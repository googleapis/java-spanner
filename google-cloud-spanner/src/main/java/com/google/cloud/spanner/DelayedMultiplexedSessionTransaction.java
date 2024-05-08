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

import static com.google.cloud.spanner.SessionImpl.NO_CHANNEL_HINT;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.spanner.DelayedReadContext.DelayedReadOnlyTransaction;
import com.google.cloud.spanner.MultiplexedSessionDatabaseClient.MultiplexedSessionTransaction;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Represents a delayed execution of a transaction on a multiplexed session. The execution is
 * delayed because the multiplexed session is not yet ready. This class is only used during client
 * creation before the multiplexed session has been created. The use of this class while the
 * multiplexed session is still being created ensures that the creation of a {@link DatabaseClient}
 * is non-blocking.
 */
class DelayedMultiplexedSessionTransaction extends AbstractMultiplexedSessionDatabaseClient {

  private final MultiplexedSessionDatabaseClient client;

  private final ISpan span;

  private final ApiFuture<SessionReference> sessionFuture;

  DelayedMultiplexedSessionTransaction(
      MultiplexedSessionDatabaseClient client,
      ISpan span,
      ApiFuture<SessionReference> sessionFuture) {
    this.client = client;
    this.span = span;
    this.sessionFuture = sessionFuture;
  }

  @Override
  public ReadContext singleUse() {
    return new DelayedReadContext<>(
        ApiFutures.transform(
            this.sessionFuture,
            sessionReference ->
                new MultiplexedSessionTransaction(
                        client, span, sessionReference, NO_CHANNEL_HINT, true)
                    .singleUse(),
            MoreExecutors.directExecutor()));
  }

  @Override
  public ReadContext singleUse(TimestampBound bound) {
    return new DelayedReadContext<>(
        ApiFutures.transform(
            this.sessionFuture,
            sessionReference ->
                new MultiplexedSessionTransaction(
                        client, span, sessionReference, NO_CHANNEL_HINT, true)
                    .singleUse(bound),
            MoreExecutors.directExecutor()));
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction() {
    return new DelayedReadOnlyTransaction(
        ApiFutures.transform(
            this.sessionFuture,
            sessionReference ->
                new MultiplexedSessionTransaction(
                        client, span, sessionReference, NO_CHANNEL_HINT, true)
                    .singleUseReadOnlyTransaction(),
            MoreExecutors.directExecutor()));
  }

  @Override
  public ReadOnlyTransaction singleUseReadOnlyTransaction(TimestampBound bound) {
    return new DelayedReadOnlyTransaction(
        ApiFutures.transform(
            this.sessionFuture,
            sessionReference ->
                new MultiplexedSessionTransaction(
                        client, span, sessionReference, NO_CHANNEL_HINT, true)
                    .singleUseReadOnlyTransaction(bound),
            MoreExecutors.directExecutor()));
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction() {
    return new DelayedReadOnlyTransaction(
        ApiFutures.transform(
            this.sessionFuture,
            sessionReference ->
                new MultiplexedSessionTransaction(
                        client, span, sessionReference, NO_CHANNEL_HINT, false)
                    .readOnlyTransaction(),
            MoreExecutors.directExecutor()));
  }

  @Override
  public ReadOnlyTransaction readOnlyTransaction(TimestampBound bound) {
    return new DelayedReadOnlyTransaction(
        ApiFutures.transform(
            this.sessionFuture,
            sessionReference ->
                new MultiplexedSessionTransaction(
                        client, span, sessionReference, NO_CHANNEL_HINT, false)
                    .readOnlyTransaction(bound),
            MoreExecutors.directExecutor()));
  }
}
