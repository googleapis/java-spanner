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

import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerException;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Value;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@VisibleForTesting
class GrpcResultSet extends AbstractResultSet<List<Object>> implements ProtobufResultSet {
  private final GrpcValueIterator iterator;
  private final Listener listener;
  private final DecodeMode decodeMode;
  private ResultSetMetadata metadata;
  private GrpcStruct currRow;
  private SpannerException error;
  private ResultSetStats statistics;
  private boolean closed;

  GrpcResultSet(CloseableIterator<PartialResultSet> iterator, Listener listener) {
    this(iterator, listener, DecodeMode.DIRECT);
  }

  GrpcResultSet(
      CloseableIterator<PartialResultSet> iterator, Listener listener, DecodeMode decodeMode) {
    this.iterator = new GrpcValueIterator(iterator);
    this.listener = listener;
    this.decodeMode = decodeMode;
  }

  @Override
  public boolean canGetProtobufValue(int columnIndex) {
    return !closed && currRow != null && currRow.canGetProtoValue(columnIndex);
  }

  @Override
  public Value getProtobufValue(int columnIndex) {
    checkState(!closed, "ResultSet is closed");
    checkState(currRow != null, "next() call required");
    return currRow.getProtoValueInternal(columnIndex);
  }

  @Override
  protected GrpcStruct currRow() {
    checkState(!closed, "ResultSet is closed");
    checkState(currRow != null, "next() call required");
    return currRow;
  }

  @Override
  public boolean next() throws SpannerException {
    if (error != null) {
      throw newSpannerException(error);
    }
    try {
      if (currRow == null) {
        metadata = iterator.getMetadata();
        if (metadata.hasTransaction()) {
          listener.onTransactionMetadata(
              metadata.getTransaction(), iterator.isWithBeginTransaction());
        } else if (iterator.isWithBeginTransaction()) {
          // The query should have returned a transaction.
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.FAILED_PRECONDITION, AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
        }
        currRow = new GrpcStruct(iterator.type(), new ArrayList<>(), decodeMode);
      }
      boolean hasNext = currRow.consumeRow(iterator);
      if (!hasNext) {
        statistics = iterator.getStats();
      }
      return hasNext;
    } catch (Throwable t) {
      throw yieldError(
          SpannerExceptionFactory.asSpannerException(t),
          iterator.isWithBeginTransaction() && currRow == null);
    }
  }

  @Override
  @Nullable
  public ResultSetStats getStats() {
    return statistics;
  }

  @Override
  public ResultSetMetadata getMetadata() {
    checkState(metadata != null, "next() call required");
    return metadata;
  }

  @Override
  public void close() {
    listener.onDone(iterator.isWithBeginTransaction());
    iterator.close("ResultSet closed");
    closed = true;
  }

  @Override
  public Type getType() {
    checkState(currRow != null, "next() call required");
    return currRow.getType();
  }

  private SpannerException yieldError(SpannerException e, boolean beginTransaction) {
    SpannerException toThrow = listener.onError(e, beginTransaction);
    close();
    throw toThrow;
  }
}
