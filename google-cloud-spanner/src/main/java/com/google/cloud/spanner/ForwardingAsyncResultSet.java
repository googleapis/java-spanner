/*
 * Copyright 2020 Google LLC
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

import com.google.api.core.ApiFuture;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.Executor;

/** Forwarding implementation of {@link AsyncResultSet} that forwards all calls to a delegate. */
public class ForwardingAsyncResultSet extends ForwardingResultSet implements AsyncResultSet {
  final AsyncResultSet delegate;

  public ForwardingAsyncResultSet(AsyncResultSet delegate) {
    super(Preconditions.checkNotNull(delegate));
    this.delegate = delegate;
  }

  @Override
  public CursorState tryNext() throws SpannerException {
    return delegate.tryNext();
  }

  @Override
  public void setCallback(Executor exec, ReadyCallback cb) {
    delegate.setCallback(exec, cb);
    ;
  }

  @Override
  public void cancel() {
    delegate.cancel();
  }

  @Override
  public void resume() {
    delegate.resume();
  }

  @Override
  public <T> ApiFuture<ImmutableList<T>> toListAsync(
      Function<StructReader, T> transformer, Executor executor) {
    return delegate.toListAsync(transformer, executor);
  }

  @Override
  public <T> ImmutableList<T> toList(Function<StructReader, T> transformer)
      throws SpannerException {
    return delegate.toList(transformer);
  }
}
