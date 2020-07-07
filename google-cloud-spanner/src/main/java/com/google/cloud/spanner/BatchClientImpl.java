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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.cloud.spanner.AbstractReadContext.MultiUseReadOnlyTransaction;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Struct;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.PartitionQueryRequest;
import com.google.spanner.v1.PartitionReadRequest;
import com.google.spanner.v1.PartitionResponse;
import com.google.spanner.v1.TransactionSelector;
import io.opencensus.trace.Tracing;
import java.util.List;
import java.util.Map;

/** Default implementation for Batch Client interface. */
public class BatchClientImpl implements BatchClient {
  private final SessionClient sessionClient;

  BatchClientImpl(SessionClient sessionClient) {
    this.sessionClient = checkNotNull(sessionClient);
  }

  @Override
  public BatchReadOnlyTransaction batchReadOnlyTransaction(TimestampBound bound) {
    SessionImpl session = sessionClient.createSession();
    return new BatchReadOnlyTransactionImpl(
        MultiUseReadOnlyTransaction.newBuilder()
            .setSession(session)
            .setRpc(sessionClient.getSpanner().getRpc())
            .setTimestampBound(bound)
            .setDefaultQueryOptions(
                sessionClient.getSpanner().getDefaultQueryOptions(sessionClient.getDatabaseId()))
            .setExecutorProvider(sessionClient.getSpanner().getAsyncExecutorProvider())
            .setDefaultPrefetchChunks(sessionClient.getSpanner().getDefaultPrefetchChunks()),
        checkNotNull(bound));
  }

  @Override
  public BatchReadOnlyTransaction batchReadOnlyTransaction(BatchTransactionId batchTransactionId) {
    SessionImpl session =
        sessionClient.sessionWithId(checkNotNull(batchTransactionId).getSessionId());
    return new BatchReadOnlyTransactionImpl(
        MultiUseReadOnlyTransaction.newBuilder()
            .setSession(session)
            .setRpc(sessionClient.getSpanner().getRpc())
            .setTransactionId(batchTransactionId.getTransactionId())
            .setTimestamp(batchTransactionId.getTimestamp())
            .setDefaultQueryOptions(
                sessionClient.getSpanner().getDefaultQueryOptions(sessionClient.getDatabaseId()))
            .setExecutorProvider(sessionClient.getSpanner().getAsyncExecutorProvider())
            .setDefaultPrefetchChunks(sessionClient.getSpanner().getDefaultPrefetchChunks()),
        batchTransactionId);
  }

  private static class BatchReadOnlyTransactionImpl extends MultiUseReadOnlyTransaction
      implements BatchReadOnlyTransaction {
    private final String sessionName;
    private final Map<SpannerRpc.Option, ?> options;

    BatchReadOnlyTransactionImpl(
        MultiUseReadOnlyTransaction.Builder builder, TimestampBound bound) {
      super(builder.setTimestampBound(bound));
      this.sessionName = session.getName();
      this.options = session.getOptions();
      setSpan(Tracing.getTracer().getCurrentSpan());
      initTransaction();
    }

    BatchReadOnlyTransactionImpl(
        MultiUseReadOnlyTransaction.Builder builder, BatchTransactionId batchTransactionId) {
      super(builder.setTransactionId(batchTransactionId.getTransactionId()));
      this.sessionName = session.getName();
      this.options = session.getOptions();
      setSpan(Tracing.getTracer().getCurrentSpan());
    }

    @Override
    public BatchTransactionId getBatchTransactionId() {
      return new BatchTransactionId(sessionName, getTransactionId(), getReadTimestamp());
    }

    @Override
    public List<Partition> partitionRead(
        PartitionOptions partitionOptions,
        String table,
        KeySet keys,
        Iterable<String> columns,
        ReadOption... options)
        throws SpannerException {
      return partitionReadUsingIndex(
          partitionOptions, table, null /*index*/, keys, columns, options);
    }

    @Override
    public List<Partition> partitionReadUsingIndex(
        PartitionOptions partitionOptions,
        String table,
        String index,
        KeySet keys,
        Iterable<String> columns,
        ReadOption... option)
        throws SpannerException {
      Options readOptions = Options.fromReadOptions(option);
      Preconditions.checkArgument(
          !readOptions.hasLimit(),
          "Limit option not supported by partitionRead|partitionReadUsingIndex");
      final PartitionReadRequest.Builder builder =
          PartitionReadRequest.newBuilder()
              .setSession(sessionName)
              .setTable(checkNotNull(table))
              .addAllColumns(columns);
      keys.appendToProto(builder.getKeySetBuilder());
      if (index != null) {
        builder.setIndex(index);
      }
      TransactionSelector selector = getTransactionSelector();
      if (selector != null) {
        builder.setTransaction(selector);
      }
      com.google.spanner.v1.PartitionOptions.Builder pbuilder =
          com.google.spanner.v1.PartitionOptions.newBuilder();
      if (partitionOptions != null) {
        partitionOptions.appendToProto(pbuilder);
      }
      builder.setPartitionOptions(pbuilder.build());

      final PartitionReadRequest request = builder.build();
      PartitionResponse response = rpc.partitionRead(request, options);
      ImmutableList.Builder<Partition> partitions = ImmutableList.builder();
      for (com.google.spanner.v1.Partition p : response.getPartitionsList()) {
        Partition partition =
            Partition.createReadPartition(
                p.getPartitionToken(), partitionOptions, table, index, keys, columns, readOptions);
        partitions.add(partition);
      }
      return partitions.build();
    }

    @Override
    public List<Partition> partitionQuery(
        PartitionOptions partitionOptions, Statement statement, QueryOption... option)
        throws SpannerException {
      Options queryOptions = Options.fromQueryOptions(option);
      final PartitionQueryRequest.Builder builder =
          PartitionQueryRequest.newBuilder().setSession(sessionName).setSql(statement.getSql());
      Map<String, Value> stmtParameters = statement.getParameters();
      if (!stmtParameters.isEmpty()) {
        Struct.Builder paramsBuilder = builder.getParamsBuilder();
        for (Map.Entry<String, Value> param : stmtParameters.entrySet()) {
          paramsBuilder.putFields(param.getKey(), param.getValue().toProto());
          builder.putParamTypes(param.getKey(), param.getValue().getType().toProto());
        }
      }
      TransactionSelector selector = getTransactionSelector();
      if (selector != null) {
        builder.setTransaction(selector);
      }
      com.google.spanner.v1.PartitionOptions.Builder pbuilder =
          com.google.spanner.v1.PartitionOptions.newBuilder();
      if (partitionOptions != null) {
        partitionOptions.appendToProto(pbuilder);
      }
      builder.setPartitionOptions(pbuilder.build());

      final PartitionQueryRequest request = builder.build();
      PartitionResponse response = rpc.partitionQuery(request, options);
      ImmutableList.Builder<Partition> partitions = ImmutableList.builder();
      for (com.google.spanner.v1.Partition p : response.getPartitionsList()) {
        Partition partition =
            Partition.createQueryPartition(
                p.getPartitionToken(), partitionOptions, statement, queryOptions);
        partitions.add(partition);
      }
      return partitions.build();
    }

    @Override
    public ResultSet execute(Partition partition) throws SpannerException {
      if (partition.getStatement() != null) {
        return executeQueryInternalWithOptions(
            partition.getStatement(),
            QueryMode.NORMAL,
            partition.getQueryOptions(),
            partition.getPartitionToken());
      }
      return readInternalWithOptions(
          partition.getTable(),
          partition.getIndex(),
          partition.getKeys(),
          partition.getColumns(),
          partition.getReadOptions(),
          partition.getPartitionToken());
    }

    @Override
    public void close() {
      super.close();
      session.close();
    }
  }
}
