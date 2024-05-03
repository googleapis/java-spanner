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

import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.TransactionOption;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.spanner.v1.BatchWriteResponse;

/**
 * Base class for the Multiplexed Session {@link DatabaseClient} implementation. Throws {@link
 * UnsupportedOperationException} for all methods that are currently not supported for multiplexed
 * sessions. The concrete implementation implements the methods that are supported with multiplexed
 * sessions.
 */
abstract class AbstractMultiplexedSessionDatabaseClient implements DatabaseClient {

  @Override
  public Dialect getDialect() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDatabaseRole() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timestamp write(Iterable<Mutation> mutations) throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommitResponse writeWithOptions(Iterable<Mutation> mutations, TransactionOption... options)
      throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timestamp writeAtLeastOnce(Iterable<Mutation> mutations) throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommitResponse writeAtLeastOnceWithOptions(
      Iterable<Mutation> mutations, TransactionOption... options) throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServerStream<BatchWriteResponse> batchWriteAtLeastOnce(
      Iterable<MutationGroup> mutationGroups, TransactionOption... options)
      throws SpannerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public TransactionRunner readWriteTransaction(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TransactionManager transactionManager(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AsyncRunner runAsync(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AsyncTransactionManager transactionManagerAsync(TransactionOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long executePartitionedUpdate(Statement stmt, UpdateOption... options) {
    throw new UnsupportedOperationException();
  }
}
